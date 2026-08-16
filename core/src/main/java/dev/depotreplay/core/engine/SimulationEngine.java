package dev.depotreplay.core.engine;

import dev.depotreplay.core.model.DeliveryTaskDefinition;
import dev.depotreplay.core.model.DispatchCommand;
import dev.depotreplay.core.model.Position;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ServiceAction;
import dev.depotreplay.core.model.SimulationSnapshot;
import dev.depotreplay.core.model.TaskSnapshot;
import dev.depotreplay.core.model.TaskStatus;
import dev.depotreplay.core.model.VehicleDefinition;
import dev.depotreplay.core.model.VehicleSnapshot;
import dev.depotreplay.core.path.GridPathfinder;
import dev.depotreplay.core.scoring.Scorer;
import dev.depotreplay.core.validation.ScenarioValidator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Pure Java fixed-tick simulation with no rendering or wall-clock dependencies. */
public final class SimulationEngine {
    private static final Comparator<DispatchCommand> COMMAND_ORDER = Comparator
            .comparing(DispatchCommand::vehicleId)
            .thenComparing(DispatchCommand::taskId)
            .thenComparing(DispatchCommand::action);

    private final Scenario scenario;
    private final GridPathfinder pathfinder;
    private final Map<String, VehicleRuntime> vehicles = new LinkedHashMap<>();
    private final Map<String, TaskRuntime> tasks = new LinkedHashMap<>();
    private final Map<Integer, List<DispatchCommand>> scheduledCommands = new TreeMap<>();
    private final List<DispatchCommand> commandLog = new ArrayList<>();
    private int tick;

    public SimulationEngine(Scenario scenario) {
        ScenarioValidator.validate(scenario);
        this.scenario = scenario;
        this.pathfinder = new GridPathfinder(scenario.grid());
        scenario.vehicles().stream()
                .sorted(Comparator.comparing(VehicleDefinition::id))
                .forEach(definition -> vehicles.put(definition.id(), new VehicleRuntime(definition)));
        scenario.tasks().stream()
                .sorted(Comparator.comparing(DeliveryTaskDefinition::id))
                .forEach(definition -> tasks.put(definition.id(), new TaskRuntime(definition)));
    }

    public Scenario scenario() {
        return scenario;
    }

    public int currentTick() {
        return tick;
    }

    public boolean isTerminal() {
        return tick >= scenario.maxTicks() || tasks.values().stream()
                .allMatch(task -> task.status == TaskStatus.DELIVERED);
    }

    public void submitCommand(DispatchCommand command) {
        Objects.requireNonNull(command, "command");
        if (isTerminal()) {
            throw new CommandRejectedException("The simulation is already terminal");
        }
        if (command.tick() < tick || command.tick() >= scenario.maxTicks()) {
            throw new CommandRejectedException("Command tick must be between currentTick and maxTicks - 1");
        }
        if (!vehicles.containsKey(command.vehicleId())) {
            throw new CommandRejectedException("Unknown vehicle: " + command.vehicleId());
        }
        if (!tasks.containsKey(command.taskId())) {
            throw new CommandRejectedException("Unknown task: " + command.taskId());
        }
        if (command.action() == null) {
            throw new CommandRejectedException("Command action is required");
        }
        List<DispatchCommand> commandsAtTick = scheduledCommands.computeIfAbsent(
                command.tick(), ignored -> new ArrayList<>()
        );
        if (commandsAtTick.stream().anyMatch(existing -> existing.vehicleId().equals(command.vehicleId()))) {
            throw new CommandRejectedException(
                    "Only one command per vehicle is allowed at tick " + command.tick()
            );
        }
        commandsAtTick.add(command);
    }

    /** Applies commands, advances each vehicle by at most one cell, and increments logical time once. */
    public SimulationSnapshot advanceOneTick() {
        if (isTerminal()) {
            throw new IllegalStateException("Cannot advance a terminal simulation");
        }
        List<DispatchCommand> commands = scheduledCommands.remove(tick);
        if (commands != null) {
            List<DispatchCommand> ordered = commands.stream().sorted(COMMAND_ORDER).toList();
            validateCommandBatch(ordered);
            ordered.forEach(this::applyCommand);
        }
        for (VehicleRuntime vehicle : vehicles.values()) {
            advanceVehicle(vehicle);
        }
        tick++;
        return snapshot();
    }

    public SimulationSnapshot runUntilTerminal() {
        while (!isTerminal()) {
            advanceOneTick();
        }
        return snapshot();
    }

    public SimulationSnapshot snapshot() {
        List<VehicleSnapshot> vehicleSnapshots = vehicles.values().stream()
                .map(VehicleRuntime::snapshot)
                .toList();
        List<TaskSnapshot> taskSnapshots = tasks.values().stream()
                .map(TaskRuntime::snapshot)
                .toList();
        return new SimulationSnapshot(
                1,
                scenario.name(),
                scenario.seed(),
                tick,
                scenario.maxTicks(),
                scenario.pathAlgorithm(),
                scenario.grid(),
                vehicleSnapshots,
                taskSnapshots,
                Scorer.calculate(vehicleSnapshots, taskSnapshots, scenario.scoreWeights()),
                tasks.values().stream().allMatch(task -> task.status == TaskStatus.DELIVERED),
                List.copyOf(commandLog)
        );
    }

    public boolean isVehicleIdle(String vehicleId) {
        VehicleRuntime vehicle = requireVehicle(vehicleId);
        return vehicle.activeOrder == null;
    }

    private void applyCommand(DispatchCommand command) {
        VehicleRuntime vehicle = requireVehicle(command.vehicleId());
        TaskRuntime task = requireTask(command.taskId());
        if (vehicle.activeOrder != null && vehicle.activeOrder.action() == ServiceAction.PICKUP) {
            TaskRuntime abandoned = requireTask(vehicle.activeOrder.taskId());
            abandoned.reservedBy = null;
        }
        Position target;
        if (command.action() == ServiceAction.PICKUP) {
            if (task.status != TaskStatus.PENDING) {
                throw rejected(command, "task is not pending");
            }
            if (task.reservedBy != null && !task.reservedBy.equals(vehicle.id)) {
                throw rejected(command, "task is reserved by " + task.reservedBy);
            }
            if (vehicle.load + task.definition.demand() > vehicle.capacity) {
                throw rejected(command, "vehicle capacity would be exceeded");
            }
            task.reservedBy = vehicle.id;
            target = task.definition.pickup();
        } else {
            if (task.status != TaskStatus.IN_TRANSIT || !vehicle.id.equals(task.vehicleId)) {
                throw rejected(command, "task is not carried by this vehicle");
            }
            target = task.definition.delivery();
        }
        vehicle.route.clear();
        vehicle.route.addAll(pathfinder.findPath(vehicle.position, target, scenario.pathAlgorithm()));
        vehicle.activeOrder = new ActiveOrder(task.definition.id(), command.action());
        commandLog.add(command);
    }

    private void validateCommandBatch(List<DispatchCommand> commands) {
        Map<String, String> reservations = new HashMap<>();
        tasks.values().stream()
                .filter(task -> task.reservedBy != null)
                .forEach(task -> reservations.put(task.definition.id(), task.reservedBy));
        for (DispatchCommand command : commands) {
            VehicleRuntime vehicle = requireVehicle(command.vehicleId());
            if (vehicle.activeOrder != null && vehicle.activeOrder.action() == ServiceAction.PICKUP) {
                reservations.remove(vehicle.activeOrder.taskId(), vehicle.id);
            }
        }
        for (DispatchCommand command : commands) {
            VehicleRuntime vehicle = requireVehicle(command.vehicleId());
            TaskRuntime task = requireTask(command.taskId());
            if (command.action() == ServiceAction.PICKUP) {
                if (task.status != TaskStatus.PENDING) {
                    throw rejected(command, "task is not pending");
                }
                String owner = reservations.putIfAbsent(task.definition.id(), vehicle.id);
                if (owner != null && !owner.equals(vehicle.id)) {
                    throw rejected(command, "task is reserved by " + owner);
                }
                if (vehicle.load + task.definition.demand() > vehicle.capacity) {
                    throw rejected(command, "vehicle capacity would be exceeded");
                }
            } else if (task.status != TaskStatus.IN_TRANSIT || !vehicle.id.equals(task.vehicleId)) {
                throw rejected(command, "task is not carried by this vehicle");
            }
        }
    }

    private void advanceVehicle(VehicleRuntime vehicle) {
        if (!vehicle.route.isEmpty()) {
            vehicle.position = vehicle.route.removeFirst();
            vehicle.distanceTraveled++;
            vehicle.routeTrace.add(vehicle.position);
        }
        if (vehicle.activeOrder != null && vehicle.route.isEmpty()) {
            completeService(vehicle, vehicle.activeOrder, tick + 1);
            vehicle.activeOrder = null;
        }
    }

    private void completeService(VehicleRuntime vehicle, ActiveOrder order, int completionTick) {
        TaskRuntime task = requireTask(order.taskId());
        if (order.action() == ServiceAction.PICKUP) {
            if (task.status != TaskStatus.PENDING || !vehicle.id.equals(task.reservedBy)) {
                throw new IllegalStateException("Reserved pickup became invalid: " + task.definition.id());
            }
            task.status = TaskStatus.IN_TRANSIT;
            task.vehicleId = vehicle.id;
            task.pickupTick = completionTick;
            task.reservedBy = null;
            vehicle.cargo.add(task.definition.id());
            vehicle.cargo.sort(String::compareTo);
            vehicle.load += task.definition.demand();
        } else {
            if (task.status != TaskStatus.IN_TRANSIT || !vehicle.id.equals(task.vehicleId)) {
                throw new IllegalStateException("Delivery became invalid: " + task.definition.id());
            }
            task.status = TaskStatus.DELIVERED;
            task.deliveryTick = completionTick;
            vehicle.cargo.remove(task.definition.id());
            vehicle.load -= task.definition.demand();
        }
    }

    private VehicleRuntime requireVehicle(String id) {
        VehicleRuntime vehicle = vehicles.get(id);
        if (vehicle == null) {
            throw new CommandRejectedException("Unknown vehicle: " + id);
        }
        return vehicle;
    }

    private TaskRuntime requireTask(String id) {
        TaskRuntime task = tasks.get(id);
        if (task == null) {
            throw new CommandRejectedException("Unknown task: " + id);
        }
        return task;
    }

    private static CommandRejectedException rejected(DispatchCommand command, String reason) {
        return new CommandRejectedException("Rejected " + command + ": " + reason);
    }

    private static final class VehicleRuntime {
        private final String id;
        private final int capacity;
        private Position position;
        private int load;
        private int distanceTraveled;
        private final List<String> cargo = new ArrayList<>();
        private final Deque<Position> route = new ArrayDeque<>();
        private final List<Position> routeTrace = new ArrayList<>();
        private ActiveOrder activeOrder;

        private VehicleRuntime(VehicleDefinition definition) {
            id = definition.id();
            capacity = definition.capacity();
            position = definition.start();
            routeTrace.add(position);
        }

        private VehicleSnapshot snapshot() {
            return new VehicleSnapshot(
                    id,
                    position,
                    capacity,
                    load,
                    cargo,
                    distanceTraveled,
                    List.copyOf(route),
                    activeOrder == null ? null : activeOrder.taskId(),
                    activeOrder == null ? null : activeOrder.action(),
                    routeTrace
            );
        }
    }

    private static final class TaskRuntime {
        private final DeliveryTaskDefinition definition;
        private TaskStatus status = TaskStatus.PENDING;
        private String reservedBy;
        private String vehicleId;
        private Integer pickupTick;
        private Integer deliveryTick;

        private TaskRuntime(DeliveryTaskDefinition definition) {
            this.definition = definition;
        }

        private TaskSnapshot snapshot() {
            return new TaskSnapshot(
                    definition.id(),
                    definition.pickup(),
                    definition.delivery(),
                    definition.demand(),
                    definition.deadline(),
                    status,
                    vehicleId,
                    pickupTick,
                    deliveryTick
            );
        }
    }

    private record ActiveOrder(String taskId, ServiceAction action) { }
}

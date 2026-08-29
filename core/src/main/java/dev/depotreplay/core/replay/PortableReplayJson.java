package dev.depotreplay.core.replay;

import dev.depotreplay.core.model.DeliveryTaskDefinition;
import dev.depotreplay.core.model.DispatchCommand;
import dev.depotreplay.core.model.GridDefinition;
import dev.depotreplay.core.model.Position;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ScoreBreakdown;
import dev.depotreplay.core.model.ScoreWeights;
import dev.depotreplay.core.model.SimulationSnapshot;
import dev.depotreplay.core.model.TaskSnapshot;
import dev.depotreplay.core.model.VehicleDefinition;
import dev.depotreplay.core.model.VehicleSnapshot;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/** Reflection-free canonical JSON used by verified replay features on every platform. */
public final class PortableReplayJson {
    private PortableReplayJson() { }

    public static String encode(ReplayFile replay) {
        JsonWriter writer = new JsonWriter();
        writer.beginObject();
        writer.field("commands", value -> commands(value, replay.commands()));
        writer.field("engineVersion", value -> value.string(replay.engineVersion()));
        writer.field("finalStateHash", value -> value.string(replay.finalStateHash()));
        writer.field("scenario", value -> scenario(value, replay.scenario()));
        writer.field("scenarioHash", value -> value.string(replay.scenarioHash()));
        writer.field("schemaVersion", value -> value.number(replay.schemaVersion()));
        writer.field("tickHashes", value -> tickHashes(value, replay.tickHashes()));
        writer.endObject();
        return writer.toString();
    }

    public static String scenarioHash(Scenario scenario) {
        JsonWriter writer = new JsonWriter();
        scenario(writer, scenario);
        return PortableSha256.digestUtf8(writer.toString());
    }

    public static String stateHash(SimulationSnapshot state) {
        JsonWriter writer = new JsonWriter();
        snapshot(writer, state);
        return PortableSha256.digestUtf8(writer.toString());
    }

    static String encodeScenario(Scenario scenario) {
        JsonWriter writer = new JsonWriter();
        scenario(writer, scenario);
        return writer.toString();
    }

    static String encodeState(SimulationSnapshot state) {
        JsonWriter writer = new JsonWriter();
        snapshot(writer, state);
        return writer.toString();
    }

    private static void scenario(JsonWriter writer, Scenario scenario) {
        writer.beginObject();
        writer.field("grid", value -> grid(value, scenario.grid()));
        writer.field("maxTicks", value -> value.number(scenario.maxTicks()));
        writer.field("name", value -> value.string(scenario.name()));
        writer.field("pathAlgorithm", value -> value.string(scenario.pathAlgorithm().name()));
        writer.field("schemaVersion", value -> value.number(scenario.schemaVersion()));
        writer.field("scoreWeights", value -> scoreWeights(value, scenario.scoreWeights()));
        writer.field("seed", value -> value.number(scenario.seed()));
        writer.field("tasks", value -> tasks(value, scenario.tasks()));
        writer.field("vehicles", value -> vehicleDefinitions(value, scenario.vehicles()));
        writer.endObject();
    }

    private static void snapshot(JsonWriter writer, SimulationSnapshot state) {
        writer.beginObject();
        writer.field("commandLog", value -> commands(value, state.commandLog()));
        writer.field("complete", value -> value.bool(state.complete()));
        writer.field("grid", value -> grid(value, state.grid()));
        writer.field("maxTicks", value -> value.number(state.maxTicks()));
        writer.field("pathAlgorithm", value -> value.string(state.pathAlgorithm().name()));
        writer.field("scenarioName", value -> value.string(state.scenarioName()));
        writer.field("schemaVersion", value -> value.number(state.schemaVersion()));
        writer.field("score", value -> score(value, state.score()));
        writer.field("seed", value -> value.number(state.seed()));
        writer.field("tasks", value -> taskSnapshots(value, state.tasks()));
        writer.field("tick", value -> value.number(state.tick()));
        writer.field("vehicles", value -> vehicleSnapshots(value, state.vehicles()));
        writer.endObject();
    }

    private static void grid(JsonWriter writer, GridDefinition grid) {
        writer.beginObject();
        writer.field("height", value -> value.number(grid.height()));
        writer.field("obstacles", value -> positions(value, grid.obstacles()));
        writer.field("width", value -> value.number(grid.width()));
        writer.endObject();
    }

    private static void positions(JsonWriter writer, List<Position> positions) {
        writer.array(positions, PortableReplayJson::position);
    }

    private static void position(JsonWriter writer, Position position) {
        writer.beginObject();
        writer.field("x", value -> value.number(position.x()));
        writer.field("y", value -> value.number(position.y()));
        writer.endObject();
    }

    private static void scoreWeights(JsonWriter writer, ScoreWeights weights) {
        writer.beginObject();
        writer.field("distance", value -> value.number(weights.distance()));
        writer.field("lateness", value -> value.number(weights.lateness()));
        writer.field("unserved", value -> value.number(weights.unserved()));
        writer.endObject();
    }

    private static void tasks(JsonWriter writer, List<DeliveryTaskDefinition> tasks) {
        writer.array(tasks, PortableReplayJson::taskDefinition);
    }

    private static void taskDefinition(JsonWriter writer, DeliveryTaskDefinition task) {
        writer.beginObject();
        writer.field("deadline", value -> value.number(task.deadline()));
        writer.field("delivery", value -> position(value, task.delivery()));
        writer.field("demand", value -> value.number(task.demand()));
        writer.field("id", value -> value.string(task.id()));
        writer.field("pickup", value -> position(value, task.pickup()));
        writer.endObject();
    }

    private static void vehicleDefinitions(JsonWriter writer, List<VehicleDefinition> vehicles) {
        writer.array(vehicles, PortableReplayJson::vehicleDefinition);
    }

    private static void vehicleDefinition(JsonWriter writer, VehicleDefinition vehicle) {
        writer.beginObject();
        writer.field("capacity", value -> value.number(vehicle.capacity()));
        writer.field("id", value -> value.string(vehicle.id()));
        writer.field("start", value -> position(value, vehicle.start()));
        writer.endObject();
    }

    private static void commands(JsonWriter writer, List<DispatchCommand> commands) {
        writer.array(commands, PortableReplayJson::command);
    }

    private static void command(JsonWriter writer, DispatchCommand command) {
        writer.beginObject();
        writer.field("action", value -> value.string(command.action().name()));
        writer.field("taskId", value -> value.string(command.taskId()));
        writer.field("tick", value -> value.number(command.tick()));
        writer.field("vehicleId", value -> value.string(command.vehicleId()));
        writer.endObject();
    }

    private static void tickHashes(JsonWriter writer, List<TickHash> hashes) {
        writer.array(hashes, PortableReplayJson::tickHash);
    }

    private static void tickHash(JsonWriter writer, TickHash hash) {
        writer.beginObject();
        writer.field("stateHash", value -> value.string(hash.stateHash()));
        writer.field("tick", value -> value.number(hash.tick()));
        writer.endObject();
    }

    private static void score(JsonWriter writer, ScoreBreakdown score) {
        writer.beginObject();
        writer.field("distance", value -> value.number(score.distance()));
        writer.field("distanceCost", value -> value.number(score.distanceCost()));
        writer.field("lateness", value -> value.number(score.lateness()));
        writer.field("latenessCost", value -> value.number(score.latenessCost()));
        writer.field("total", value -> value.number(score.total()));
        writer.field("unservedCost", value -> value.number(score.unservedCost()));
        writer.field("unservedTasks", value -> value.number(score.unservedTasks()));
        writer.endObject();
    }

    private static void taskSnapshots(JsonWriter writer, List<TaskSnapshot> tasks) {
        writer.array(tasks, PortableReplayJson::taskSnapshot);
    }

    private static void taskSnapshot(JsonWriter writer, TaskSnapshot task) {
        writer.beginObject();
        writer.field("deadline", value -> value.number(task.deadline()));
        writer.field("delivery", value -> position(value, task.delivery()));
        writer.field("deliveryTick", value -> value.nullableNumber(task.deliveryTick()));
        writer.field("demand", value -> value.number(task.demand()));
        writer.field("id", value -> value.string(task.id()));
        writer.field("pickup", value -> position(value, task.pickup()));
        writer.field("pickupTick", value -> value.nullableNumber(task.pickupTick()));
        writer.field("status", value -> value.string(task.status().name()));
        writer.field("vehicleId", value -> value.string(task.vehicleId()));
        writer.endObject();
    }

    private static void vehicleSnapshots(JsonWriter writer, List<VehicleSnapshot> vehicles) {
        writer.array(vehicles, PortableReplayJson::vehicleSnapshot);
    }

    private static void vehicleSnapshot(JsonWriter writer, VehicleSnapshot vehicle) {
        writer.beginObject();
        writer.field("activeAction", value -> value.string(
                vehicle.activeAction() == null ? null : vehicle.activeAction().name()
        ));
        writer.field("activeTaskId", value -> value.string(vehicle.activeTaskId()));
        writer.field("capacity", value -> value.number(vehicle.capacity()));
        writer.field("cargo", value -> value.strings(vehicle.cargo()));
        writer.field("distanceTraveled", value -> value.number(vehicle.distanceTraveled()));
        writer.field("id", value -> value.string(vehicle.id()));
        writer.field("load", value -> value.number(vehicle.load()));
        writer.field("position", value -> position(value, vehicle.position()));
        writer.field("remainingRoute", value -> positions(value, vehicle.remainingRoute()));
        writer.field("routeTrace", value -> positions(value, vehicle.routeTrace()));
        writer.endObject();
    }

    private static final class JsonWriter {
        private static final char[] HEX = "0123456789ABCDEF".toCharArray();
        private final StringBuilder output = new StringBuilder();
        private final Deque<Boolean> firstFields = new ArrayDeque<>();

        void beginObject() {
            output.append('{');
            firstFields.push(true);
        }

        void endObject() {
            firstFields.pop();
            output.append('}');
        }

        void field(String name, Consumer<JsonWriter> value) {
            boolean firstField = firstFields.pop();
            if (!firstField) {
                output.append(',');
            }
            firstFields.push(false);
            string(name);
            output.append(':');
            value.accept(this);
        }

        <T> void array(List<T> values, ElementWriter<T> elementWriter) {
            output.append('[');
            for (int index = 0; index < values.size(); index++) {
                if (index > 0) {
                    output.append(',');
                }
                elementWriter.write(this, values.get(index));
            }
            output.append(']');
        }

        void strings(List<String> values) {
            array(values, JsonWriter::string);
        }

        void string(String value) {
            if (value == null) {
                output.append("null");
                return;
            }
            output.append('"');
            for (int index = 0; index < value.length(); index++) {
                char character = value.charAt(index);
                switch (character) {
                    case '"' -> output.append("\\\"");
                    case '\\' -> output.append("\\\\");
                    case '\b' -> output.append("\\b");
                    case '\f' -> output.append("\\f");
                    case '\n' -> output.append("\\n");
                    case '\r' -> output.append("\\r");
                    case '\t' -> output.append("\\t");
                    default -> appendUnescaped(character);
                }
            }
            output.append('"');
        }

        void number(long value) {
            output.append(value);
        }

        void nullableNumber(Integer value) {
            if (value == null) {
                output.append("null");
            } else {
                number(value);
            }
        }

        void bool(boolean value) {
            output.append(value);
        }

        private void appendUnescaped(char character) {
            if (character < 0x20) {
                output.append("\\u");
                output.append(HEX[character >>> 12 & 0x0f]);
                output.append(HEX[character >>> 8 & 0x0f]);
                output.append(HEX[character >>> 4 & 0x0f]);
                output.append(HEX[character & 0x0f]);
            } else {
                output.append(character);
            }
        }

        @Override
        public String toString() {
            return output.toString();
        }
    }

    @FunctionalInterface
    private interface ElementWriter<T> {
        void write(JsonWriter writer, T value);
    }
}

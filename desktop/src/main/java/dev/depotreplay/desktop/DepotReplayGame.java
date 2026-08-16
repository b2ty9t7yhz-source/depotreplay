package dev.depotreplay.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import dev.depotreplay.core.compare.ComparisonReport;
import dev.depotreplay.core.compare.ComparisonService;
import dev.depotreplay.core.compare.RunComparison;
import dev.depotreplay.core.engine.CommandRejectedException;
import dev.depotreplay.core.engine.SimulationEngine;
import dev.depotreplay.core.io.CanonicalJson;
import dev.depotreplay.core.io.ScenarioIo;
import dev.depotreplay.core.model.DispatchCommand;
import dev.depotreplay.core.model.Position;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ServiceAction;
import dev.depotreplay.core.model.SimulationSnapshot;
import dev.depotreplay.core.model.TaskSnapshot;
import dev.depotreplay.core.model.TaskStatus;
import dev.depotreplay.core.model.VehicleSnapshot;
import dev.depotreplay.core.replay.ReplayFile;
import dev.depotreplay.core.replay.ReplayService;
import dev.depotreplay.core.save.LoadedSave;
import dev.depotreplay.core.save.SaveService;
import dev.depotreplay.core.strategy.NearestTaskStrategy;
import dev.depotreplay.core.strategy.StrategyDecision;
import dev.depotreplay.core.strategy.StrategyRunner;
import dev.depotreplay.core.path.GridPathfinder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Thin libGDX adapter; all rules and persistence live in the headless core module. */
public final class DepotReplayGame extends ApplicationAdapter {
    private static final float WORLD_WIDTH = 1280f;
    private static final float WORLD_HEIGHT = 800f;
    private static final float GRID_LEFT = 54f;
    private static final float GRID_BOTTOM = 138f;
    private static final float GRID_SIZE = 590f;
    private static final Color BACKGROUND = Color.valueOf("0D1321");
    private static final Color PANEL = Color.valueOf("151E31");
    private static final Color GRID_CELL = Color.valueOf("1C2942");
    private static final Color GRID_LINE = Color.valueOf("30415F");
    private static final Color PRIMARY = Color.valueOf("61DAFB");
    private static final Color SECONDARY = Color.valueOf("FFB454");
    private static final Color SUCCESS = Color.valueOf("75E6A4");
    private static final Color DANGER = Color.valueOf("FF6B81");
    private static final Path SAVE_PATH = Path.of("build/player.save.json");
    private static final Path REPLAY_PATH = Path.of("build/player.replay.json");

    private final Path scenarioPath;
    private final Path screenshotPath;
    private final Viewport viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
    private Scenario scenario;
    private SimulationEngine engine;
    private ComparisonReport baselineReport;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;
    private BitmapFont smallFont;
    private int selectedVehicle;
    private int selectedTask;
    private String status = "Select a vehicle and task, then dispatch a pickup.";
    private int renderedFrames;
    private boolean screenshotWritten;
    private String modeLabel = "PLAYER DISPATCH";

    public DepotReplayGame(Path scenarioPath, Path screenshotPath) {
        this.scenarioPath = scenarioPath;
        this.screenshotPath = screenshotPath;
    }

    @Override
    public void create() {
        scenario = ScenarioIo.load(scenarioPath);
        engine = new SimulationEngine(scenario);
        baselineReport = new ComparisonService().compare(scenario, null);
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.28f);
        smallFont = new BitmapFont();
        smallFont.getData().setScale(0.88f);
        Gdx.input.setInputProcessor(new Controls());
        if (screenshotPath != null) {
            loadShowcaseRun();
        }
    }

    @Override
    public void render() {
        ScreenUtils.clear(BACKGROUND);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        batch.setProjectionMatrix(viewport.getCamera().combined);
        SimulationSnapshot state = engine.snapshot();
        drawPanels();
        drawGrid(state);
        drawRoutes(state);
        drawTasks(state);
        drawVehicles(state);
        drawText(state);
        writeScreenshotWhenReady();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
        smallFont.dispose();
    }

    private void drawPanels() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(PANEL);
        shapes.rect(30, 30, 650, 730);
        shapes.rect(700, 30, 550, 730);
        shapes.setColor(Color.valueOf("0A1020"));
        shapes.rect(720, 480, 510, 250);
        shapes.rect(720, 205, 510, 250);
        shapes.end();
    }

    private void drawGrid(SimulationSnapshot state) {
        float cell = cellSize(state);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < state.grid().width(); x++) {
            for (int y = 0; y < state.grid().height(); y++) {
                Position position = new Position(x, y);
                shapes.setColor(state.grid().obstacles().contains(position) ? Color.valueOf("070B12") : GRID_CELL);
                shapes.rect(GRID_LEFT + x * cell, GRID_BOTTOM + y * cell, cell - 2, cell - 2);
            }
        }
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(GRID_LINE);
        for (int x = 0; x <= state.grid().width(); x++) {
            shapes.line(GRID_LEFT + x * cell, GRID_BOTTOM, GRID_LEFT + x * cell,
                    GRID_BOTTOM + state.grid().height() * cell);
        }
        for (int y = 0; y <= state.grid().height(); y++) {
            shapes.line(GRID_LEFT, GRID_BOTTOM + y * cell,
                    GRID_LEFT + state.grid().width() * cell, GRID_BOTTOM + y * cell);
        }
        shapes.end();
    }

    private void drawRoutes(SimulationSnapshot state) {
        float cell = cellSize(state);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(3f);
        for (int index = 0; index < state.vehicles().size(); index++) {
            VehicleSnapshot vehicle = state.vehicles().get(index);
            Color color = index == 0 ? PRIMARY : SECONDARY;
            shapes.setColor(color);
            drawPolyline(vehicle.routeTrace(), cell);
            if (!vehicle.remainingRoute().isEmpty()) {
                List<Position> planned = new java.util.ArrayList<>();
                planned.add(vehicle.position());
                planned.addAll(vehicle.remainingRoute());
                color.a = 0.45f;
                shapes.setColor(color);
                drawPolyline(planned, cell);
                color.a = 1f;
            }
        }
        Gdx.gl.glLineWidth(1f);
        shapes.end();
    }

    private void drawPolyline(List<Position> positions, float cell) {
        for (int index = 1; index < positions.size(); index++) {
            Position from = positions.get(index - 1);
            Position to = positions.get(index);
            shapes.line(centerX(from, cell), centerY(from, cell), centerX(to, cell), centerY(to, cell));
        }
    }

    private void drawTasks(SimulationSnapshot state) {
        float cell = cellSize(state);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int index = 0; index < state.tasks().size(); index++) {
            TaskSnapshot task = state.tasks().get(index);
            float marker = Math.max(7f, cell * 0.16f);
            shapes.setColor(task.status() == TaskStatus.PENDING ? SUCCESS : Color.valueOf("52637E"));
            shapes.circle(centerX(task.pickup(), cell), centerY(task.pickup(), cell), marker, 24);
            shapes.setColor(task.status() == TaskStatus.DELIVERED ? SUCCESS : DANGER);
            shapes.rect(centerX(task.delivery(), cell) - marker,
                    centerY(task.delivery(), cell) - marker, marker * 2, marker * 2);
            if (index == selectedTask && screenshotPath == null) {
                shapes.setColor(Color.WHITE);
                shapes.circle(centerX(task.pickup(), cell), centerY(task.pickup(), cell), marker * 0.35f, 18);
            }
        }
        shapes.end();
    }

    private void drawVehicles(SimulationSnapshot state) {
        float cell = cellSize(state);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int index = 0; index < state.vehicles().size(); index++) {
            VehicleSnapshot vehicle = state.vehicles().get(index);
            float radius = Math.max(11f, cell * 0.24f);
            shapes.setColor(index == 0 ? PRIMARY : SECONDARY);
            shapes.circle(centerX(vehicle.position(), cell), centerY(vehicle.position(), cell), radius, 28);
            if (index == selectedVehicle && screenshotPath == null) {
                shapes.setColor(Color.WHITE);
                shapes.circle(centerX(vehicle.position(), cell), centerY(vehicle.position(), cell), radius * 0.34f, 20);
            }
        }
        shapes.end();
    }

    private void drawText(SimulationSnapshot state) {
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "DEPOTREPLAY", 54, 726);
        smallFont.setColor(PRIMARY);
        smallFont.draw(batch, modeLabel + "  /  DETERMINISTIC TICK ENGINE", 54, 696);
        smallFont.setColor(Color.LIGHT_GRAY);
        smallFont.draw(batch, scenario.name() + "  /  seed " + scenario.seed()
                + "  /  " + scenario.pathAlgorithm(), 54, 672);

        font.setColor(Color.WHITE);
        font.draw(batch, "LIVE STATE", 740, 704);
        smallFont.setColor(Color.LIGHT_GRAY);
        smallFont.draw(batch, "Tick", 740, 674);
        font.setColor(PRIMARY);
        font.draw(batch, state.tick() + " / " + state.maxTicks(), 790, 676);
        drawScore(state, 740, 640);
        drawVehicleState(state, 740, 540);

        font.setColor(Color.WHITE);
        font.draw(batch, "COUNTERFACTUAL BASELINES", 740, 430);
        float y = 392;
        for (RunComparison run : baselineReport.runs()) {
            smallFont.setColor(Color.LIGHT_GRAY);
            smallFont.draw(batch, run.label(), 740, y);
            font.setColor(run.score().unservedTasks() == 0 ? SUCCESS : DANGER);
            font.draw(batch, String.valueOf(run.score().total()), 985, y + 2);
            smallFont.setColor(Color.valueOf("91A1BB"));
            smallFont.draw(batch, "dist " + run.score().distance()
                    + "  late " + run.score().lateness()
                    + "  unserved " + run.score().unservedTasks(), 1040, y);
            y -= 56;
        }

        font.setColor(Color.WHITE);
        font.draw(batch, "TASK TIMELINE", 740, 178);
        y = 146;
        for (TaskSnapshot task : state.tasks()) {
            smallFont.setColor(task.status() == TaskStatus.DELIVERED ? SUCCESS : Color.LIGHT_GRAY);
            smallFont.draw(batch, task.id() + "  " + task.status()
                    + "  due " + task.deadline()
                    + "  done " + value(task.deliveryTick()), 740, y);
            y -= 24;
        }

        smallFont.setColor(Color.valueOf("91A1BB"));
        smallFont.draw(batch, "1/2 vehicle  UP/DOWN task  P pickup  D deliver  SPACE tick", 54, 94);
        smallFont.draw(batch, "A auto-step  R reset  S save  L load  E replay", 54, 70);
        smallFont.setColor(status.startsWith("ERROR") ? DANGER : SUCCESS);
        smallFont.draw(batch, status, 54, 46);
        batch.end();
    }

    private void drawScore(SimulationSnapshot state, float x, float y) {
        smallFont.setColor(Color.LIGHT_GRAY);
        smallFont.draw(batch, "TOTAL COST", x, y);
        font.setColor(state.score().unservedTasks() == 0 ? SUCCESS : SECONDARY);
        font.draw(batch, String.valueOf(state.score().total()), x + 125, y + 2);
        smallFont.setColor(Color.valueOf("91A1BB"));
        smallFont.draw(batch, "distance  " + state.score().distance() + " * " + scenario.scoreWeights().distance()
                + " = " + state.score().distanceCost(), x, y - 30);
        smallFont.draw(batch, "lateness  " + state.score().lateness() + " * " + scenario.scoreWeights().lateness()
                + " = " + state.score().latenessCost(), x, y - 53);
        smallFont.draw(batch, "unserved  " + state.score().unservedTasks() + " * " + scenario.scoreWeights().unserved()
                + " = " + state.score().unservedCost(), x, y - 76);
    }

    private void drawVehicleState(SimulationSnapshot state, float x, float y) {
        for (int index = 0; index < state.vehicles().size(); index++) {
            VehicleSnapshot vehicle = state.vehicles().get(index);
            smallFont.setColor(index == 0 ? PRIMARY : SECONDARY);
            smallFont.draw(batch, vehicle.id() + "  @ " + coordinate(vehicle.position())
                    + "  load " + vehicle.load() + "/" + vehicle.capacity()
                    + "  distance " + vehicle.distanceTraveled(), x, y - index * 30);
        }
    }

    private float cellSize(SimulationSnapshot state) {
        return Math.min(GRID_SIZE / state.grid().width(), GRID_SIZE / state.grid().height());
    }

    private static String coordinate(Position position) {
        return "(" + position.x() + "," + position.y() + ")";
    }

    private static String value(Integer value) {
        return value == null ? "-" : value.toString();
    }

    private static float centerX(Position position, float cell) {
        return GRID_LEFT + (position.x() + 0.5f) * cell;
    }

    private static float centerY(Position position, float cell) {
        return GRID_BOTTOM + (position.y() + 0.5f) * cell;
    }

    private void dispatch(ServiceAction action) {
        if (engine.isTerminal()) {
            status = "ERROR: The run is terminal. Press R to reset.";
            return;
        }
        SimulationSnapshot state = engine.snapshot();
        VehicleSnapshot vehicle = state.vehicles().get(selectedVehicle);
        TaskSnapshot task = state.tasks().get(selectedTask);
        try {
            engine.submitCommand(new DispatchCommand(
                    state.tick(), vehicle.id(), task.id(), action
            ));
            status = "Queued " + action + " for " + vehicle.id() + " -> " + task.id() + ".";
        } catch (CommandRejectedException error) {
            status = "ERROR: " + error.getMessage();
        }
    }

    private void advance() {
        try {
            engine.advanceOneTick();
            status = "Advanced to deterministic tick " + engine.currentTick() + ".";
        } catch (RuntimeException error) {
            status = "ERROR: " + error.getMessage();
        }
    }

    private void autoStep() {
        if (engine.isTerminal()) {
            status = "ERROR: The run is terminal. Press R to reset.";
            return;
        }
        SimulationSnapshot state = engine.snapshot();
        GridPathfinder pathfinder = new GridPathfinder(scenario.grid());
        Set<String> reserved = new HashSet<>();
        state.vehicles().stream()
                .filter(vehicle -> vehicle.activeAction() == ServiceAction.PICKUP)
                .map(VehicleSnapshot::activeTaskId)
                .forEach(reserved::add);
        for (VehicleSnapshot vehicle : state.vehicles()) {
            if (engine.isVehicleIdle(vehicle.id())) {
                Optional<StrategyDecision> decision = new NearestTaskStrategy().chooseNext(
                        state, vehicle, reserved, pathfinder
                );
                decision.ifPresent(selected -> {
                    engine.submitCommand(new DispatchCommand(
                            state.tick(), vehicle.id(), selected.taskId(), selected.action()
                    ));
                    if (selected.action() == ServiceAction.PICKUP) {
                        reserved.add(selected.taskId());
                    }
                });
            }
        }
        advance();
        status = "Auto-stepped with nearest-task at tick " + state.tick() + ".";
    }

    private void reset() {
        engine = new SimulationEngine(scenario);
        selectedVehicle = 0;
        selectedTask = 0;
        modeLabel = "PLAYER DISPATCH";
        status = "Reset to canonical initial state.";
    }

    private void save() {
        try {
            SaveService service = new SaveService();
            service.save(SAVE_PATH, service.create(engine));
            status = "Saved verified checkpoint to " + SAVE_PATH + ".";
        } catch (RuntimeException error) {
            status = "ERROR: " + error.getMessage();
        }
    }

    private void load() {
        try {
            LoadedSave loaded = new SaveService().restore(new SaveService().load(SAVE_PATH));
            if (!CanonicalJson.sha256(scenario).equals(CanonicalJson.sha256(loaded.engine().scenario()))) {
                throw new IllegalArgumentException("Save belongs to a different scenario");
            }
            engine = loaded.engine();
            status = "Loaded checkpoint at tick " + loaded.state().tick() + "; hash verified.";
        } catch (RuntimeException error) {
            status = "ERROR: " + error.getMessage();
        }
    }

    private void exportReplay() {
        try {
            ReplayService service = new ReplayService();
            ReplayFile replay = service.create(scenario, engine.snapshot().commandLog());
            service.save(REPLAY_PATH, replay);
            status = "Exported and verified replay: " + replay.finalStateHash().substring(0, 12) + "...";
        } catch (RuntimeException error) {
            status = "ERROR: " + error.getMessage();
        }
    }

    private void loadShowcaseRun() {
        SimulationSnapshot showcase = new StrategyRunner()
                .run(scenario, new NearestTaskStrategy())
                .finalState();
        engine = new SimulationEngine(scenario);
        showcase.commandLog().forEach(engine::submitCommand);
        engine.runUntilTerminal();
        modeLabel = "VERIFIED STRATEGY REVIEW";
        status = "Replay-ready result / " + CanonicalJson.sha256(engine.snapshot()).substring(0, 16) + "...";
    }

    private void writeScreenshotWhenReady() {
        if (screenshotPath == null || screenshotWritten || renderedFrames++ < 3) {
            return;
        }
        try {
            Path absolute = screenshotPath.toAbsolutePath().normalize();
            Path parent = absolute.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            int width = Gdx.graphics.getBackBufferWidth();
            int height = Gdx.graphics.getBackBufferHeight();
            byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, width, height, true);
            Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
            BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
            try {
                PixmapIO.writePNG(new FileHandle(absolute.toFile()), pixmap);
            } finally {
                pixmap.dispose();
            }
            screenshotWritten = true;
            Gdx.app.log("DepotReplay", "Screenshot written to " + absolute);
            Gdx.app.exit();
        } catch (RuntimeException | java.io.IOException error) {
            Gdx.app.error("DepotReplay", "Could not write screenshot", error);
            Gdx.app.exit();
        }
    }

    private final class Controls extends InputAdapter {
        @Override
        public boolean keyDown(int keycode) {
            switch (keycode) {
                case Input.Keys.NUM_1 -> selectedVehicle = 0;
                case Input.Keys.NUM_2 -> selectedVehicle = 1;
                case Input.Keys.UP -> selectedTask = Math.max(0, selectedTask - 1);
                case Input.Keys.DOWN -> selectedTask = Math.min(scenario.tasks().size() - 1, selectedTask + 1);
                case Input.Keys.P -> dispatch(ServiceAction.PICKUP);
                case Input.Keys.D -> dispatch(ServiceAction.DELIVER);
                case Input.Keys.SPACE -> advance();
                case Input.Keys.A -> autoStep();
                case Input.Keys.R -> reset();
                case Input.Keys.S -> save();
                case Input.Keys.L -> load();
                case Input.Keys.E -> exportReplay();
                case Input.Keys.ESCAPE -> Gdx.app.exit();
                default -> { return false; }
            }
            return true;
        }
    }
}

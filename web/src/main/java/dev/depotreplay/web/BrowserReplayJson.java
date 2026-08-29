package dev.depotreplay.web;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import dev.depotreplay.core.io.CorruptedReplayException;
import dev.depotreplay.core.model.DeliveryTaskDefinition;
import dev.depotreplay.core.model.DispatchCommand;
import dev.depotreplay.core.model.GridDefinition;
import dev.depotreplay.core.model.PathAlgorithm;
import dev.depotreplay.core.model.Position;
import dev.depotreplay.core.model.Scenario;
import dev.depotreplay.core.model.ScoreWeights;
import dev.depotreplay.core.model.ServiceAction;
import dev.depotreplay.core.model.VehicleDefinition;
import dev.depotreplay.core.replay.PortableReplayJson;
import dev.depotreplay.core.replay.ReplayFile;
import dev.depotreplay.core.replay.TickHash;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Strict, reflection-free replay codec for TeaVM and headless browser-format tests. */
final class BrowserReplayJson {
    private BrowserReplayJson() { }

    static String encode(ReplayFile replay) {
        return PortableReplayJson.encode(replay);
    }

    static ReplayFile decode(String json) {
        if (json == null || json.isBlank()) {
            throw new CorruptedReplayException("Replay JSON must not be blank");
        }
        try {
            JsonValue root = object(
                    new JsonReader().parse(json),
                    "$",
                    Set.of(
                            "commands", "engineVersion", "finalStateHash", "scenario",
                            "scenarioHash", "schemaVersion", "tickHashes"
                    )
            );
            return new ReplayFile(
                    integer(root, "schemaVersion", "$"),
                    string(root, "engineVersion", "$"),
                    scenario(required(root, "scenario", "$"), "$.scenario"),
                    string(root, "scenarioHash", "$"),
                    commands(required(root, "commands", "$"), "$.commands"),
                    tickHashes(required(root, "tickHashes", "$"), "$.tickHashes"),
                    string(root, "finalStateHash", "$")
            );
        } catch (CorruptedReplayException error) {
            throw error;
        } catch (RuntimeException error) {
            throw new CorruptedReplayException("Could not parse browser replay JSON", error);
        }
    }

    private static Scenario scenario(JsonValue value, String path) {
        JsonValue object = object(value, path, Set.of(
                "grid", "maxTicks", "name", "pathAlgorithm", "schemaVersion",
                "scoreWeights", "seed", "tasks", "vehicles"
        ));
        return new Scenario(
                integer(object, "schemaVersion", path),
                string(object, "name", path),
                longNumber(object, "seed", path),
                grid(required(object, "grid", path), path + ".grid"),
                vehicles(required(object, "vehicles", path), path + ".vehicles"),
                tasks(required(object, "tasks", path), path + ".tasks"),
                scoreWeights(required(object, "scoreWeights", path), path + ".scoreWeights"),
                integer(object, "maxTicks", path),
                enumValue(PathAlgorithm.class, string(object, "pathAlgorithm", path), path + ".pathAlgorithm")
        );
    }

    private static GridDefinition grid(JsonValue value, String path) {
        JsonValue object = object(value, path, Set.of("height", "obstacles", "width"));
        return new GridDefinition(
                integer(object, "width", path),
                integer(object, "height", path),
                positions(required(object, "obstacles", path), path + ".obstacles")
        );
    }

    private static ScoreWeights scoreWeights(JsonValue value, String path) {
        JsonValue object = object(value, path, Set.of("distance", "lateness", "unserved"));
        return new ScoreWeights(
                integer(object, "distance", path),
                integer(object, "lateness", path),
                integer(object, "unserved", path)
        );
    }

    private static List<VehicleDefinition> vehicles(JsonValue value, String path) {
        JsonValue array = array(value, path);
        List<VehicleDefinition> result = new ArrayList<>(array.size);
        int index = 0;
        for (JsonValue item : array) {
            String itemPath = path + "[" + index + "]";
            JsonValue object = object(item, itemPath, Set.of("capacity", "id", "start"));
            result.add(new VehicleDefinition(
                    string(object, "id", itemPath),
                    position(required(object, "start", itemPath), itemPath + ".start"),
                    integer(object, "capacity", itemPath)
            ));
            index++;
        }
        return result;
    }

    private static List<DeliveryTaskDefinition> tasks(JsonValue value, String path) {
        JsonValue array = array(value, path);
        List<DeliveryTaskDefinition> result = new ArrayList<>(array.size);
        int index = 0;
        for (JsonValue item : array) {
            String itemPath = path + "[" + index + "]";
            JsonValue object = object(item, itemPath, Set.of(
                    "deadline", "delivery", "demand", "id", "pickup"
            ));
            result.add(new DeliveryTaskDefinition(
                    string(object, "id", itemPath),
                    position(required(object, "pickup", itemPath), itemPath + ".pickup"),
                    position(required(object, "delivery", itemPath), itemPath + ".delivery"),
                    integer(object, "demand", itemPath),
                    integer(object, "deadline", itemPath)
            ));
            index++;
        }
        return result;
    }

    private static List<Position> positions(JsonValue value, String path) {
        JsonValue array = array(value, path);
        List<Position> result = new ArrayList<>(array.size);
        int index = 0;
        for (JsonValue item : array) {
            result.add(position(item, path + "[" + index + "]"));
            index++;
        }
        return result;
    }

    private static Position position(JsonValue value, String path) {
        JsonValue object = object(value, path, Set.of("x", "y"));
        return new Position(integer(object, "x", path), integer(object, "y", path));
    }

    private static List<DispatchCommand> commands(JsonValue value, String path) {
        JsonValue array = array(value, path);
        List<DispatchCommand> result = new ArrayList<>(array.size);
        int index = 0;
        for (JsonValue item : array) {
            String itemPath = path + "[" + index + "]";
            JsonValue object = object(item, itemPath, Set.of("action", "taskId", "tick", "vehicleId"));
            result.add(new DispatchCommand(
                    integer(object, "tick", itemPath),
                    string(object, "vehicleId", itemPath),
                    string(object, "taskId", itemPath),
                    enumValue(ServiceAction.class, string(object, "action", itemPath), itemPath + ".action")
            ));
            index++;
        }
        return result;
    }

    private static List<TickHash> tickHashes(JsonValue value, String path) {
        JsonValue array = array(value, path);
        List<TickHash> result = new ArrayList<>(array.size);
        int index = 0;
        for (JsonValue item : array) {
            String itemPath = path + "[" + index + "]";
            JsonValue object = object(item, itemPath, Set.of("stateHash", "tick"));
            result.add(new TickHash(
                    integer(object, "tick", itemPath),
                    string(object, "stateHash", itemPath)
            ));
            index++;
        }
        return result;
    }

    private static JsonValue object(JsonValue value, String path, Set<String> expectedFields) {
        if (value == null || !value.isObject()) {
            throw invalid(path, "must be an object");
        }
        Set<String> seenFields = new HashSet<>();
        for (JsonValue child : value) {
            if (!expectedFields.contains(child.name)) {
                throw invalid(path + "." + child.name, "is not a supported field");
            }
            if (!seenFields.add(child.name)) {
                throw invalid(path + "." + child.name, "must not be duplicated");
            }
        }
        for (String field : expectedFields) {
            if (!value.has(field)) {
                throw invalid(path + "." + field, "is required");
            }
        }
        return value;
    }

    private static JsonValue array(JsonValue value, String path) {
        if (value == null || !value.isArray()) {
            throw invalid(path, "must be an array");
        }
        return value;
    }

    private static JsonValue required(JsonValue object, String field, String path) {
        JsonValue value = object.get(field);
        if (value == null || value.isNull()) {
            throw invalid(path + "." + field, "must not be null");
        }
        return value;
    }

    private static String string(JsonValue object, String field, String path) {
        JsonValue value = required(object, field, path);
        if (!value.isString()) {
            throw invalid(path + "." + field, "must be a string");
        }
        return value.asString();
    }

    private static int integer(JsonValue object, String field, String path) {
        long value = longNumber(object, field, path);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw invalid(path + "." + field, "is outside the 32-bit integer range");
        }
        return (int) value;
    }

    private static long longNumber(JsonValue object, String field, String path) {
        JsonValue value = required(object, field, path);
        if (!value.isLong()) {
            throw invalid(path + "." + field, "must be an integer");
        }
        return value.asLong();
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String path) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException error) {
            throw invalid(path, "contains unsupported value " + value);
        }
    }

    private static CorruptedReplayException invalid(String path, String problem) {
        return new CorruptedReplayException("Invalid replay JSON: " + path + " " + problem);
    }
}

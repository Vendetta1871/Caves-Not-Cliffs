package net.celestiald.cavesnotcliffs.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.BufferedReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Verifies the 1.12 resource-location rules across the complete mod asset graph. */
public class ResourceReferenceIntegrityTest {
    @Test
    public void everyLocalModelTextureAndSoundReferenceResolves() throws Exception {
        URL resource = getClass().getClassLoader().getResource("assets/cavesnotcliffs");
        assertNotNull(resource);
        Path root = Paths.get(resource.toURI());
        List<String> errors = new ArrayList<>();

        for (Path state : jsonFiles(root.resolve("blockstates"))) {
            inspectBlockstateModels(root, state, json(state), errors);
        }
        for (Path model : jsonFiles(root.resolve("models"))) {
            inspectModel(root, model, json(model).getAsJsonObject(), errors);
        }
        inspectSounds(root, json(root.resolve("sounds.json")).getAsJsonObject(), errors);

        assertTrue(String.join("\n", errors), errors.isEmpty());
    }

    private static void inspectBlockstateModels(Path root, Path source,
            JsonElement element, List<String> errors) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                inspectBlockstateModels(root, source, child, errors);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        JsonElement model = object.get("model");
        if (model != null && model.isJsonPrimitive()) {
            String reference = model.getAsString();
            String path = localPath(reference);
            if (path != null) {
                // Variant.Deserializer#getResourceLocationBlock adds this prefix in 1.12.
                require(root, source, reference,
                        root.resolve("models/block").resolve(path + ".json"), errors);
            }
        }
        for (JsonElement child : object.entrySet().stream()
                .map(java.util.Map.Entry::getValue).collect(Collectors.toList())) {
            inspectBlockstateModels(root, source, child, errors);
        }
    }

    private static void inspectModel(Path root, Path source, JsonObject model,
            List<String> errors) {
        JsonElement parent = model.get("parent");
        if (parent != null) {
            String reference = parent.getAsString();
            String path = localPath(reference);
            if (path != null) {
                require(root, source, reference,
                        root.resolve("models").resolve(path + ".json"), errors);
            }
        }
        JsonObject textures = model.getAsJsonObject("textures");
        if (textures == null) {
            return;
        }
        for (java.util.Map.Entry<String, JsonElement> entry : textures.entrySet()) {
            String reference = entry.getValue().getAsString();
            if (reference.startsWith("#")) {
                continue;
            }
            String path = localPath(reference);
            if (path != null) {
                require(root, source, reference,
                        root.resolve("textures").resolve(path + ".png"), errors);
            }
        }
    }

    private static void inspectSounds(Path root, JsonObject sounds,
            List<String> errors) {
        Path source = root.resolve("sounds.json");
        for (java.util.Map.Entry<String, JsonElement> event : sounds.entrySet()) {
            JsonArray entries = event.getValue().getAsJsonObject().getAsJsonArray("sounds");
            if (entries == null) {
                continue;
            }
            for (JsonElement entry : entries) {
                String reference;
                boolean eventReference = false;
                if (entry.isJsonObject()) {
                    JsonObject object = entry.getAsJsonObject();
                    reference = object.get("name").getAsString();
                    eventReference = object.has("type")
                            && "event".equals(object.get("type").getAsString());
                } else {
                    reference = entry.getAsString();
                }
                String path = localPath(reference);
                if (path != null && !eventReference) {
                    require(root, source, event.getKey() + " -> " + reference,
                            root.resolve("sounds").resolve(path + ".ogg"), errors);
                }
            }
        }
    }

    private static List<Path> jsonFiles(Path directory) throws Exception {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted().collect(Collectors.toList());
        }
    }

    private static JsonElement json(Path path) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader);
        }
    }

    private static String localPath(String reference) {
        int separator = reference.indexOf(':');
        if (separator < 0 || !"cavesnotcliffs".equals(reference.substring(0, separator))) {
            return null;
        }
        return reference.substring(separator + 1);
    }

    private static void require(Path root, Path source, String reference,
            Path target, List<String> errors) {
        if (!Files.isRegularFile(target)) {
            errors.add(root.relativize(source) + " references " + reference
                    + " but " + root.relativize(target) + " does not exist");
        }
    }
}

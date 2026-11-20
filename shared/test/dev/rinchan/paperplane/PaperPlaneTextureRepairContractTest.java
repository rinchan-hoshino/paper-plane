package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

final class PaperPlaneTextureRepairContractTest {
    private static final Set<String> PLANE_ITEMS = Set.of(
        "paper_plane",
        "ender_paper_plane",
        "back_paper_plane",
        "ender_back_paper_plane"
    );

    private static Path resources() {
        for (String candidate : new String[]{"../shared/resources", "../../shared/resources"}) {
            Path path = Path.of(candidate);
            if (Files.isDirectory(path)) return path.normalize();
        }
        Path direct = Path.of("common/src/main/resources");
        return Files.isDirectory(direct) ? direct : Path.of("../common/src/main/resources").normalize();
    }

    private static Path localResources() {
        Path direct = Path.of("common/src/main/resources");
        return Files.isDirectory(direct) ? direct : Path.of("../common/src/main/resources").normalize();
    }

    @Test
    void itemDefinitionsResolveEveryRegisteredPlaneModel() throws Exception {
        Path localAssets = localResources().resolve("assets/paper_plane");
        if (!Files.isDirectory(localAssets.resolve("items"))) {
            return;
        }
        Path sharedAssets = resources().resolve("assets/paper_plane");
        for (String item : PLANE_ITEMS) {
            Path definition = localAssets.resolve("items/" + item + ".json");
            assertTrue(Files.isRegularFile(definition), definition + " must exist for the modern item model loader");
            assertTrue(
                Files.readString(definition).contains("\"model\": \"paper_plane:item/" + item + "\""),
                definition + " must resolve its matching model"
            );
            assertTrue(Files.isRegularFile(sharedAssets.resolve("models/item/" + item + ".json")));
        }
        assertFalse(Files.exists(localAssets.resolve("items/soggy_paper_plane.json")));
    }

    @Test
    void itemModelsUseStandardGeneratedParentAndTextures() throws Exception {
        Path modelsDir = resources().resolve("assets/paper_plane/models/item");
        for (String item : PLANE_ITEMS) {
            Path modelPath = modelsDir.resolve(item + ".json");
            assertTrue(Files.isRegularFile(modelPath), modelPath + " must exist");
            String model = Files.readString(modelPath);
            assertTrue(model.contains("\"parent\": \"minecraft:item/generated\""), modelPath + " must use item/generated");
            assertTrue(model.contains("\"layer0\": \"paper_plane:item/" + item + "\""), modelPath + " must reference layer0 texture");
            assertFalse(model.contains("\"elements\""), modelPath + " must not contain legacy voxel elements");
            assertFalse(model.contains("_edge"), modelPath + " must not reference deprecated edge texture");
        }
        assertFalse(Files.exists(modelsDir.resolve("paper_plane.obj")));
        assertFalse(Files.exists(modelsDir.resolve("paper_plane.mtl")));
        assertFalse(Files.exists(modelsDir.resolve("soggy_paper_plane.json")));
    }

    @Test
    void eachItemTextureIsValidNonEmpty16x16SpriteAndDeprecatedAssetsAreRemoved() throws Exception {
        Path texturesDir = resources().resolve("assets/paper_plane/textures/item");
        for (String name : PLANE_ITEMS) {
            Path texturePath = texturesDir.resolve(name + ".png");
            assertTrue(Files.isRegularFile(texturePath), texturePath + " must exist");
            BufferedImage image = ImageIO.read(texturePath.toFile());
            assertNotNull(image, texturePath + " must be a readable PNG");
            assertEquals(16, image.getWidth());
            assertEquals(16, image.getHeight());
            int nonEmpty = 0;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    if ((image.getRGB(x, y) >>> 24) != 0) {
                        nonEmpty++;
                    }
                }
            }
            assertTrue(nonEmpty > 0 && nonEmpty < 256, texturePath + " must be a non-empty 16x16 sprite with transparency");
            assertFalse(Files.exists(texturesDir.resolve(name + "_edge.png")));
        }
        assertFalse(Files.exists(texturesDir.resolve("transparent.png")));
        assertFalse(Files.exists(texturesDir.resolve("soggy_paper_plane.png")));
        assertFalse(Files.exists(texturesDir.resolve("soggy_paper_plane_edge.png")));
    }
}

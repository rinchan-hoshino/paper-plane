package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

final class PaperPlaneTextureRepairContractTest {
    private static Path resources() {
        Path direct = Path.of("common/src/main/resources");
        return Files.isDirectory(direct) ? direct : Path.of("../common/src/main/resources").normalize();
    }

    @Test
    void modelUsesMinecraftScaleUnitVoxelsForTheWingAndTKeel() throws Exception {
        Path modelPath = resources().resolve("assets/paper_plane/models/item/paper_plane.json");
        String model = Files.readString(modelPath);
        assertEquals(121, occurrences(model, "\"name\":"));
        assertEquals(92, occurrences(model, "\"name\": \"wing_"));
        assertEquals(29, occurrences(model, "\"name\": \"keel_"));
        assertFalse(model.contains("transparent"));
        assertTrue(model.contains("\"name\": \"wing_8_0\""));
        assertTrue(model.contains("\"name\": \"keel_4_4\""));
        assertFalse(model.contains("\"name\": \"keel_1_"));
        assertFalse(model.contains("\"name\": \"keel_2_"));
        assertFalse(model.contains("\"name\": \"keel_3_"));
        assertTrue(model.contains("\"translation\": [\n        -0.5,\n        0,\n        0\n      ]"));
        assertFalse(Files.exists(modelPath.resolveSibling("paper_plane.obj")));
        assertFalse(Files.exists(modelPath.resolveSibling("paper_plane.mtl")));

        for (String variant : new String[] {"soggy", "ender"}) {
            String child = Files.readString(modelPath.resolveSibling(variant + "_paper_plane.json"));
            assertTrue(child.contains("\"edge\": \"paper_plane:item/" + variant + "_paper_plane_edge\""));
        }
    }

    @Test
    void eachTopTextureHasOnePointedConnectedIntegerAxisSymmetricSilhouette() throws Exception {
        Set<Integer> referenceMask = null;
        for (String name : new String[] {"paper_plane", "soggy_paper_plane", "ender_paper_plane"}) {
            BufferedImage image = image(name + ".png");
            assertEquals(16, image.getWidth());
            assertEquals(16, image.getHeight());
            Set<Integer> mask = mask(image);
            assertEquals(92, mask.size());
            assertEquals(1, componentCount(mask));
            assertNotEquals(0, image.getRGB(8, 0) >>> 24);
            assertEquals(0, image.getRGB(7, 0) >>> 24);
            assertEquals(0, image.getRGB(9, 0) >>> 24);
            for (int y = 0; y < 16; y++) for (int x = 1; x < 8; x++)
                assertEquals(image.getRGB(x, y), image.getRGB(16 - x, y));
            if (referenceMask == null) referenceMask = mask;
            else assertEquals(referenceMask, mask);
        }
    }

    @Test
    void edgeTexturesAreOpaqueSingleColourPixelsWithoutSpacerFallback() throws Exception {
        for (String name : new String[] {"paper_plane_edge", "soggy_paper_plane_edge", "ender_paper_plane_edge"}) {
            BufferedImage image = image(name + ".png");
            assertEquals(16, image.getWidth());
            assertEquals(16, image.getHeight());
            int expected = image.getRGB(0, 0);
            assertNotEquals(0, expected >>> 24);
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) assertEquals(expected, image.getRGB(x, y));
        }
        assertFalse(Files.exists(resources().resolve("assets/paper_plane/textures/item/transparent.png")));
    }

    private static BufferedImage image(String name) throws Exception {
        return ImageIO.read(resources().resolve("assets/paper_plane/textures/item/" + name).toFile());
    }

    private static int occurrences(String source, String needle) {
        int count = 0, offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private static Set<Integer> mask(BufferedImage image) {
        Set<Integer> result = new HashSet<>();
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++)
            if ((image.getRGB(x, y) >>> 24) != 0) result.add(y * 16 + x);
        return result;
    }

    private static int componentCount(Set<Integer> source) {
        Set<Integer> remaining = new HashSet<>(source);
        int components = 0;
        while (!remaining.isEmpty()) {
            components++;
            ArrayDeque<Integer> open = new ArrayDeque<>();
            int first = remaining.iterator().next();
            remaining.remove(first);
            open.add(first);
            while (!open.isEmpty()) {
                int value = open.removeFirst();
                int x = value % 16, y = value / 16;
                for (int next : new int[] {value - 16, value + 16, value - 1, value + 1}) {
                    int nx = next % 16, ny = next / 16;
                    if (nx >= 0 && nx < 16 && ny >= 0 && ny < 16 && Math.abs(nx - x) + Math.abs(ny - y) == 1 && remaining.remove(next)) open.add(next);
                }
            }
        }
        return components;
    }
}

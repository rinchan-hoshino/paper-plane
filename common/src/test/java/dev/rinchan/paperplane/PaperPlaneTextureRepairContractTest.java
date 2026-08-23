package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
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
    void keepsTheAcceptedModelByteForByte() throws Exception {
        byte[] model = Files.readAllBytes(resources().resolve("assets/paper_plane/models/item/paper_plane.json"));
        assertEquals("a08d66e171863882c8bf153fc46e266ba90c1c32b7981320fec6230fa2f17db6", hex(MessageDigest.getInstance("SHA-256").digest(model)));
    }

    @Test
    void eachVariantRemovesOnlyTheDetachedTailPixel() throws Exception {
        Set<Integer> referenceMask = null;
        for (String name : new String[] {"paper_plane", "soggy_paper_plane", "ender_paper_plane"}) {
            BufferedImage image = ImageIO.read(resources().resolve("assets/paper_plane/textures/item/" + name + ".png").toFile());
            assertEquals(16, image.getWidth());
            assertEquals(16, image.getHeight());
            Set<Integer> mask = mask(image);
            assertEquals(77, mask.size());
            assertEquals(1, componentCount(mask));
            assertTrue(mask.contains(8));
            assertFalse(mask.contains(7));
            assertFalse(mask.contains(15 * 16 + 13));
            assertFalse(mask.contains(15 * 16 + 14));
            if (referenceMask == null) referenceMask = mask;
            else assertEquals(referenceMask, mask);
        }
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

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }
}

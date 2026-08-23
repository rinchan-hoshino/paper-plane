package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

final class PaperPlaneModelContractTest {
    private static final Path ASSETS = locateAssets();

    @Test
    void baseItemUsesOneCleanTriangularObjModel() throws Exception {
        String model = Files.readString(ASSETS.resolve("models/item/paper_plane.json"));
        assertTrue(model.contains("\"loader\": \"neoforge:obj\""));
        assertTrue(model.contains("paper_plane:models/item/paper_plane.obj"));
        assertFalse(model.contains("\"elements\""));

        List<double[]> vertices = new ArrayList<>();
        List<String> faces = new ArrayList<>();
        for (String line : Files.readAllLines(ASSETS.resolve("models/item/paper_plane.obj"))) {
            if (line.startsWith("v ")) {
                String[] values = line.substring(2).trim().split("\\s+");
                vertices.add(new double[] {
                        Double.parseDouble(values[0]),
                        Double.parseDouble(values[1]),
                        Double.parseDouble(values[2])
                });
            } else if (line.startsWith("f ")) {
                faces.add(line);
                assertEquals(3, line.substring(2).trim().split("\\s+").length, line);
            }
        }
        assertTrue(faces.size() >= 10, "A standard folded plane needs explicit triangular panels");
        assertTrue(vertices.size() >= 8);

        Set<String> vertexSet = new HashSet<>();
        for (double[] vertex : vertices) {
            assertTrue(vertex[0] >= 0.0 && vertex[0] <= 1.0);
            assertTrue(vertex[1] >= 0.0 && vertex[1] <= 1.0);
            assertTrue(vertex[2] >= 0.0 && vertex[2] <= 1.0);
            vertexSet.add(key(vertex[0], vertex[1], vertex[2]));
        }
        for (double[] vertex : vertices) {
            assertTrue(vertexSet.contains(key(1.0 - vertex[0], vertex[1], vertex[2])),
                    "Every vertex must have a mirrored partner across the center fold");
        }
    }

    @Test
    void everyVariantUsesAFourColorOpaquePaperAtlas() throws Exception {
        Set<Integer> signatures = new HashSet<>();
        for (String name : List.of("paper_plane", "soggy_paper_plane", "ender_paper_plane")) {
            BufferedImage image = ImageIO.read(ASSETS.resolve("textures/item/" + name + ".png").toFile());
            assertEquals(16, image.getWidth());
            assertEquals(16, image.getHeight());
            Set<Integer> colors = new HashSet<>();
            int signature = 1;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int argb = image.getRGB(x, y);
                    assertEquals(255, argb >>> 24, name + " has a stray transparent pixel");
                    colors.add(argb);
                    signature = 31 * signature + argb;
                }
            }
            assertEquals(4, colors.size(), name + " must be a clean four-tone material atlas");
            signatures.add(signature);
        }
        assertEquals(3, signatures.size());
    }

    @Test
    void variantsReuseTheSameGeometryAndOnlyReplacePaperColor() throws Exception {
        String soggy = Files.readString(ASSETS.resolve("models/item/soggy_paper_plane.json"));
        String ender = Files.readString(ASSETS.resolve("models/item/ender_paper_plane.json"));
        for (String model : List.of(soggy, ender)) {
            assertTrue(model.contains("\"parent\": \"paper_plane:item/paper_plane\""));
            assertTrue(model.contains("\"paper\""));
            assertFalse(model.contains("\"elements\""));
        }
        String materials = Files.readString(ASSETS.resolve("models/item/paper_plane.mtl"));
        assertEquals(4, materials.lines().filter(line -> line.startsWith("newmtl ")).count());
        assertEquals(4, materials.lines().filter(line -> line.equals("map_Kd #paper")).count());
        assertNotEquals(0, materials.length());
    }

    private static String key(double x, double y, double z) {
        return String.format("%.5f/%.5f/%.5f", x, y, z);
    }

    private static Path locateAssets() {
        for (Path candidate : List.of(
                Path.of("common/src/main/resources/assets/paper_plane"),
                Path.of("../common/src/main/resources/assets/paper_plane"))) {
            if (Files.isDirectory(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException("Paper Plane asset root is missing");
    }
}

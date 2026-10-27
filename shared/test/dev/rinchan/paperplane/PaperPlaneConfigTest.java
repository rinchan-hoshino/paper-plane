package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperPlaneConfigTest {
    @TempDir Path directory;

    @Test void createsAnEditableTomlWithBothAbilitiesEnabled() throws Exception {
        assertEquals(new PaperPlaneConfig(true, true), PaperPlaneConfig.load(directory));
        assertTrue(Files.isRegularFile(directory.resolve("paper_plane.toml")));
        assertEquals(new PaperPlaneConfig(true, true), PaperPlaneConfig.load(directory));
    }

    @Test void independentFeatureChoicesSurviveReloadAndMissingDefaults() throws Exception {
        Files.writeString(directory.resolve("paper_plane.toml"), "enableTpa = false\n");
        assertEquals(new PaperPlaneConfig(false, true), PaperPlaneConfig.load(directory));
        Files.writeString(directory.resolve("paper_plane.toml"), "enableTpa = true\nenableBack = false\n");
        assertEquals(new PaperPlaneConfig(true, false), PaperPlaneConfig.load(directory));
        assertEquals(new PaperPlaneConfig(true, false), PaperPlaneConfig.load(directory));
    }

    @Test void rejectsWrongTypesInsteadOfSilentlyEnablingDisabledFeatures() throws Exception {
        Files.writeString(directory.resolve("paper_plane.toml"), "enableTpa = \"false\"\n");
        assertThrows(IllegalArgumentException.class, () -> PaperPlaneConfig.load(directory));
    }
}

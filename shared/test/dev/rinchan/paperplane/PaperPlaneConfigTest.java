package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperPlaneConfigTest {
    @TempDir Path directory;

    @Test void createsACommentedTomlWithBothAbilitiesEnabled() throws Exception {
        assertEquals(new PaperPlaneConfig(true, true), PaperPlaneConfig.load(directory));
        Path file = directory.resolve("paper_plane.toml");
        assertTrue(Files.isRegularFile(file));
        String content = Files.readString(file);
        assertTrue(content.contains("# Paper Plane configuration"));
        assertTrue(content.contains("enableTpa = true"));
        assertTrue(content.contains("enableBack = true"));
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

    @Test void holderHotReloadsDiskEditsAndAppliesScreenSavesImmediately() throws Exception {
        PaperPlaneConfig.Holder holder = new PaperPlaneConfig.Holder();
        assertEquals(new PaperPlaneConfig(true, true), holder.configure(directory));

        Path file = directory.resolve("paper_plane.toml");
        Files.writeString(file, "enableTpa = false\nenableBack = true\n");
        Files.setLastModifiedTime(file, FileTime.from(Instant.now().plusSeconds(5)));

        assertEquals(new PaperPlaneConfig(false, true), holder.get());

        holder.saveAndApply(new PaperPlaneConfig(true, false));
        assertEquals(new PaperPlaneConfig(true, false), holder.get());
        assertEquals(new PaperPlaneConfig(true, false), PaperPlaneConfig.load(directory));
    }
}

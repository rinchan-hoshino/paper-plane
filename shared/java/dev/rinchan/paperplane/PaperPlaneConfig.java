package dev.rinchan.paperplane;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record PaperPlaneConfig(boolean enableTpa, boolean enableBack) {
    public static PaperPlaneConfig load(Path directory) throws IOException {
        Files.createDirectories(directory);
        try (var file = CommentedFileConfig.builder(directory.resolve("paper_plane.toml")).sync().build()) {
            file.load();
            boolean changed = false;
            for (String key : new String[]{"enableTpa", "enableBack"}) {
                if (!file.contains(key)) {
                    file.set(key, true);
                    changed = true;
                }
                if (!(file.get(key) instanceof Boolean)) {
                    throw new IllegalArgumentException("paper_plane.toml: " + key + " must be true or false");
                }
            }
            if (changed) file.save();
            return new PaperPlaneConfig(file.get("enableTpa"), file.get("enableBack"));
        }
    }
}

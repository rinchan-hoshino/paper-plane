package dev.rinchan.paperplane;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Zero-dependency commented TOML configuration for Paper Plane with hot-reload support.
 */
public record PaperPlaneConfig(boolean enableTpa, boolean enableBack) {
    public static final String FILE_NAME = "paper_plane.toml";
    public static final PaperPlaneConfig DEFAULT = new PaperPlaneConfig(true, true);

    public static Path resolveFile(Path directory) {
        return directory.resolve(FILE_NAME);
    }

    public static PaperPlaneConfig load(Path directory) throws IOException {
        Files.createDirectories(directory);
        Path file = resolveFile(directory);
        if (!Files.exists(file)) {
            save(directory, DEFAULT);
            return DEFAULT;
        }

        String content = Files.readString(file, StandardCharsets.UTF_8);
        Map<String, Boolean> parsed = parseToml(content);
        boolean hasTpa = parsed.containsKey("enableTpa");
        boolean hasBack = parsed.containsKey("enableBack");
        PaperPlaneConfig config = new PaperPlaneConfig(
            parsed.getOrDefault("enableTpa", true),
            parsed.getOrDefault("enableBack", true)
        );
        if (!hasTpa || !hasBack) {
            save(directory, config);
        }
        return config;
    }

    public static void save(Path directory, PaperPlaneConfig config) throws IOException {
        Files.createDirectories(directory);
        Path file = resolveFile(directory);
        Files.writeString(file, formatToml(config), StandardCharsets.UTF_8);
    }

    public static String formatToml(PaperPlaneConfig config) {
        return """
            # Paper Plane configuration
            # Changes saved in the in-game config screen apply immediately.
            # Manual edits to this file are hot-reloaded automatically (no restart required).
            # Cooldowns, warmups, and teleport history are configured in config/ftbessentials.snbt.

            # Enable TPA Paper Plane and Ender Paper Plane (player-to-player teleport requests).
            enableTpa = %s

            # Enable Back Paper Plane and Ender Back Paper Plane (return to previous location).
            enableBack = %s
            """.formatted(config.enableTpa(), config.enableBack());
    }

    private static Map<String, Boolean> parseToml(String content) {
        Map<String, Boolean> values = new HashMap<>();
        String[] lines = content.split("\\R");
        for (String rawLine : lines) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty() || line.startsWith("[")) {
                continue;
            }
            int equals = line.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String key = line.substring(0, equals).trim();
            String value = line.substring(equals + 1).trim();
            if ("enableTpa".equals(key) || "enableBack".equals(key)) {
                if ("true".equals(value)) {
                    values.put(key, Boolean.TRUE);
                } else if ("false".equals(value)) {
                    values.put(key, Boolean.FALSE);
                } else {
                    throw new IllegalArgumentException(FILE_NAME + ": " + key + " must be true or false");
                }
            }
        }
        return values;
    }

    private static String stripComment(String line) {
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (c == '#' && !inQuotes) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    /**
     * Thread-safe live config holder that hot-reloads whenever {@code paper_plane.toml}
     * changes on disk and immediately reflects saves from the native config screen.
     */
    public static final class Holder {
        private volatile Path directory;
        private volatile PaperPlaneConfig value = DEFAULT;
        private volatile FileTime lastModified;

        public synchronized PaperPlaneConfig configure(Path directory) throws IOException {
            this.directory = directory;
            PaperPlaneConfig loaded = PaperPlaneConfig.load(directory);
            this.value = loaded;
            this.lastModified = readLastModified(resolveFile(directory));
            return loaded;
        }

        public PaperPlaneConfig get() {
            Path dir = this.directory;
            if (dir == null) {
                return this.value;
            }
            Path file = resolveFile(dir);
            FileTime currentTime = readLastModified(file);
            if (currentTime != null && !currentTime.equals(this.lastModified)) {
                synchronized (this) {
                    FileTime recheck = readLastModified(file);
                    if (recheck != null && !recheck.equals(this.lastModified)) {
                        try {
                            this.value = PaperPlaneConfig.load(dir);
                            this.lastModified = readLastModified(file);
                        } catch (IOException | IllegalArgumentException ignored) {
                            // Keep last known valid configuration if the file is mid-edit or invalid.
                            this.lastModified = recheck;
                        }
                    }
                }
            }
            return this.value;
        }

        public synchronized PaperPlaneConfig saveAndApply(PaperPlaneConfig updated) throws IOException {
            Path dir = this.directory;
            if (dir != null) {
                PaperPlaneConfig.save(dir, updated);
                this.lastModified = readLastModified(resolveFile(dir));
            }
            this.value = updated;
            return updated;
        }

        public Path directory() {
            return this.directory;
        }

        private static FileTime readLastModified(Path file) {
            try {
                return Files.exists(file) ? Files.getLastModifiedTime(file) : null;
            } catch (IOException ignored) {
                return null;
            }
        }
    }
}

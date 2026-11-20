package dev.rinchan.paperplane;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/** Case-insensitive player search filter for the teleport player picker screen. */
public final class PlayerFilter {
    private PlayerFilter() {
    }

    public static <T> List<T> filter(List<T> items, Function<T, String> nameExtractor, String query) {
        if (items.isEmpty()) {
            return List.of();
        }
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return List.copyOf(items);
        }
        return items.stream()
            .filter(item -> {
                String name = nameExtractor.apply(item);
                return name != null && name.toLowerCase(Locale.ROOT).contains(normalized);
            })
            .toList();
    }

    public static <T> Optional<T> uniqueMatch(List<T> items, Function<T, String> nameExtractor, String query) {
        List<T> matches = filter(items, nameExtractor, query);
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    private static String normalize(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }
}

package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Fixed-seed randomized property tests (>= 1,000 seeds per property) covering:
 * <ul>
 *   <li>SelectionSessions multi-player concurrent lifecycle, expiration, and anti-hijack invariants</li>
 *   <li>PlayerFilter random search string filtering, order, idempotency, and uniqueMatch invariants</li>
 *   <li>TeleportResponseCommand random command parsing invariants</li>
 * </ul>
 */
final class PaperPlanePropertyTest {
    private static final int SESSION_SEEDS = 1_200;
    private static final int FILTER_SEEDS = 1_500;
    private static final int COMMAND_SEEDS = 1_000;
    private static final long SESSION_LIFETIME_TICKS = 600L;

    private static final char[] ALPHABET = (
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_- .*+?[]()^$\\/中文日本語"
    ).toCharArray();

    @Test
    void selectionSessionsPreserveIsolationOneUseAndExpirationAcross1200Seeds() {
        List<UUID> players = List.of(
            new UUID(1L, 1L),
            new UUID(1L, 2L),
            new UUID(1L, 3L),
            new UUID(1L, 4L),
            new UUID(1L, 5L)
        );
        PlaneKind[] kinds = PlaneKind.values();

        for (long seed = 1L; seed <= SESSION_SEEDS; seed++) {
            Random rng = new Random(seed * 0x9E3779B97F4A7C15L);
            SelectionSessions sessions = new SelectionSessions();
            Map<UUID, ExpectedSession> model = new HashMap<>();
            List<UUID> seenTokens = new ArrayList<>();
            Set<UUID> consumedTokens = new HashSet<>();
            long now = rng.nextInt(10_000);

            for (int step = 0; step < 60; step++) {
                String ctx = "seed=" + seed + ", step=" + step + ", now=" + now;
                int op = rng.nextInt(5);
                UUID player = players.get(rng.nextInt(players.size()));

                switch (op) {
                    case 0 -> {
                        // Open session
                        PlaneKind kind = kinds[rng.nextInt(kinds.length)];
                        ExpectedSession prev = model.get(player);
                        UUID token = sessions.open(player, kind, now);
                        if (prev != null && prev.kind == kind && now < prev.expiresAt) {
                            assertEquals(prev.token, token, ctx + ": active same-kind session must be reused");
                        } else {
                            if (prev != null) {
                                assertNotEquals(prev.token, token, ctx + ": new session must issue fresh token");
                            }
                            model.put(player, new ExpectedSession(token, kind, now + SESSION_LIFETIME_TICKS));
                            seenTokens.add(token);
                        }
                    }
                    case 1 -> {
                        // Consume with own current token (may be expired)
                        ExpectedSession expected = model.get(player);
                        if (expected == null) {
                            UUID unknown = seenTokens.isEmpty() ? UUID.randomUUID() : seenTokens.get(rng.nextInt(seenTokens.size()));
                            assertEquals(Optional.empty(), sessions.consume(player, unknown, now), ctx + ": no session to consume");
                        } else {
                            Optional<PlaneKind> actual = sessions.consume(player, expected.token, now);
                            model.remove(player);
                            if (now < expected.expiresAt) {
                                assertEquals(Optional.of(expected.kind), actual, ctx + ": valid token must consume exact kind");
                                assertTrue(consumedTokens.add(expected.token), ctx + ": token must never be consumed twice");
                            } else {
                                assertEquals(Optional.empty(), actual, ctx + ": expired token must be rejected");
                            }
                            // Immediate replay MUST fail (no double use)
                            assertEquals(Optional.empty(), sessions.consume(player, expected.token, now), ctx + ": replay must fail");
                        }
                    }
                    case 2 -> {
                        // Cross-player hijack attempt (无串单)
                        UUID victim = players.get(rng.nextInt(players.size()));
                        if (!victim.equals(player)) {
                            ExpectedSession victimSession = model.get(victim);
                            if (victimSession != null) {
                                assertEquals(
                                    Optional.empty(),
                                    sessions.consume(player, victimSession.token, now),
                                    ctx + ": another player must never consume victim's session token"
                                );
                            }
                        }
                    }
                    case 3 -> {
                        // Remove (logout)
                        sessions.remove(player);
                        model.remove(player);
                    }
                    case 4 -> {
                        // Advance time (including exact boundary tests)
                        int mode = rng.nextInt(4);
                        if (mode == 0) {
                            now += 1;
                        } else if (mode == 1) {
                            now += SESSION_LIFETIME_TICKS - 1;
                        } else if (mode == 2) {
                            now += SESSION_LIFETIME_TICKS;
                        } else {
                            now += rng.nextInt(1_200);
                        }
                    }
                    default -> throw new AssertionError();
                }
            }
        }
    }

    @Test
    void playerFilterSatisfiesInvariantsAcross1500RandomSeeds() {
        record Named(int index, String name) {}

        for (long seed = 1L; seed <= FILTER_SEEDS; seed++) {
            Random rng = new Random(seed * 0xBF58476D1CE4E5B9L);
            int rosterSize = rng.nextInt(25);
            List<Named> roster = new ArrayList<>(rosterSize);
            for (int i = 0; i < rosterSize; i++) {
                String name = rng.nextInt(15) == 0 ? null : randomString(rng, 0, 18);
                roster.add(new Named(i, name));
            }

            // Test multiple query shapes per seed: null, blank, random, substring of an existing player
            List<String> queries = new ArrayList<>();
            queries.add(null);
            queries.add("");
            queries.add("   \t ");
            queries.add(randomString(rng, 1, 8));
            if (!roster.isEmpty()) {
                Named sample = roster.get(rng.nextInt(roster.size()));
                if (sample.name != null && !sample.name.isEmpty()) {
                    int start = rng.nextInt(sample.name.length());
                    int end = start + 1 + rng.nextInt(sample.name.length() - start);
                    queries.add(sample.name.substring(start, end));
                }
            }

            for (String query : queries) {
                String ctx = "seed=" + seed + ", query=" + query;
                List<Named> result = PlayerFilter.filter(roster, Named::name, query);
                String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

                // 1. Exact specification match + relative order preservation
                List<Named> expected = new ArrayList<>();
                if (normalized.isEmpty()) {
                    expected.addAll(roster);
                } else {
                    for (Named n : roster) {
                        if (n.name != null && n.name.toLowerCase(Locale.ROOT).contains(normalized)) {
                            expected.add(n);
                        }
                    }
                }
                assertEquals(expected, result, ctx + ": filter output mismatch");

                // 2. Strict index monotonicity (order preserved)
                for (int i = 1; i < result.size(); i++) {
                    assertTrue(result.get(i - 1).index < result.get(i).index, ctx + ": order not preserved");
                }

                // 3. Idempotency
                assertEquals(result, PlayerFilter.filter(result, Named::name, query), ctx + ": filter must be idempotent");

                // 4. Case & outer-whitespace invariance (for ASCII-safe case folding)
                if (query != null && query.chars().allMatch(c -> c < 128)) {
                    String paddedUpper = "  " + query.toUpperCase(Locale.ROOT) + "\t ";
                    String paddedLower = "\t" + query.toLowerCase(Locale.ROOT) + "  ";
                    assertEquals(result, PlayerFilter.filter(roster, Named::name, paddedUpper), ctx + ": uppercase+pad invariant");
                    assertEquals(result, PlayerFilter.filter(roster, Named::name, paddedLower), ctx + ": lowercase+pad invariant");
                }

                // 5. uniqueMatch iff size == 1
                Optional<Named> unique = PlayerFilter.uniqueMatch(roster, Named::name, query);
                if (result.size() == 1) {
                    assertEquals(Optional.of(result.get(0)), unique, ctx + ": uniqueMatch must return sole match");
                } else {
                    assertEquals(Optional.empty(), unique, ctx + ": uniqueMatch must be empty when count != 1");
                }
            }
        }
    }

    @Test
    void teleportResponseCommandParsingInvariantsAcross1000Seeds() {
        for (long seed = 1L; seed <= COMMAND_SEEDS; seed++) {
            Random rng = new Random(seed * 0x94D049BB133111EBL);
            UUID id = new UUID(rng.nextLong(), rng.nextLong());

            Optional<TeleportResponseCommand> accept = TeleportResponseCommand.parse("tpaccept " + id);
            assertEquals(Optional.of(new TeleportResponseCommand(id, true)), accept);

            Optional<TeleportResponseCommand> deny = TeleportResponseCommand.parse("tpdeny " + id);
            assertEquals(Optional.of(new TeleportResponseCommand(id, false)), deny);

            // Leading slash or trailing garbage or unrelated commands must never parse
            assertFalse(TeleportResponseCommand.parse("/tpaccept " + id).isPresent());
            assertFalse(TeleportResponseCommand.parse("tpaccept " + id + " extra").isPresent());
            assertFalse(TeleportResponseCommand.parse("tpdeny " + randomString(rng, 0, 12)).isPresent());
            assertFalse(TeleportResponseCommand.parse(randomString(rng, 0, 20)).isPresent());
        }
    }

    private static String randomString(Random rng, int minLen, int maxLen) {
        int len = minLen + rng.nextInt(maxLen - minLen + 1);
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(ALPHABET[rng.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

    private record ExpectedSession(UUID token, PlaneKind kind, long expiresAt) {}
}

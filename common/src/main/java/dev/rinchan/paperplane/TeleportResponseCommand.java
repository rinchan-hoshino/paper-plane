package dev.rinchan.paperplane;

import java.util.Optional;
import java.util.UUID;

public record TeleportResponseCommand(UUID requestId, boolean accept) {
    private static final String ACCEPT_PREFIX = "tpaccept ";
    private static final String DENY_PREFIX = "tpdeny ";

    public static Optional<TeleportResponseCommand> parse(String command) {
        boolean accept;
        String requestId;
        if (command.startsWith(ACCEPT_PREFIX)) {
            accept = true;
            requestId = command.substring(ACCEPT_PREFIX.length());
        } else if (command.startsWith(DENY_PREFIX)) {
            accept = false;
            requestId = command.substring(DENY_PREFIX.length());
        } else {
            return Optional.empty();
        }

        try {
            return Optional.of(new TeleportResponseCommand(UUID.fromString(requestId), accept));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public String command() {
        return (accept ? ACCEPT_PREFIX : DENY_PREFIX) + requestId;
    }
}

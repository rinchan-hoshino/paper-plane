package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class TeleportResponseCommandTest {
    private static final UUID REQUEST_ID = UUID.fromString("d0c45834-82c8-4419-a8a5-192132858e40");

    @Test
    void parsesFtbAcceptAndDenyButtons() {
        TeleportResponseCommand accept = TeleportResponseCommand.parse("tpaccept " + REQUEST_ID).orElseThrow();
        TeleportResponseCommand deny = TeleportResponseCommand.parse("tpdeny " + REQUEST_ID).orElseThrow();
        assertTrue(accept.accept());
        assertFalse(deny.accept());
        assertEquals(REQUEST_ID, accept.requestId());
        assertEquals(REQUEST_ID, deny.requestId());
    }

    @Test
    void fallbackCommandRoundTripsExactlyOnceThroughTheSameParser() {
        TeleportResponseCommand response = new TeleportResponseCommand(REQUEST_ID, true);
        assertEquals(response, TeleportResponseCommand.parse(response.command()).orElseThrow());
    }

    @Test
    void rejectsOtherCommandsAndMalformedRequestIds() {
        assertTrue(TeleportResponseCommand.parse("home").isEmpty());
        assertTrue(TeleportResponseCommand.parse("tpaccept not-a-uuid").isEmpty());
        assertTrue(TeleportResponseCommand.parse("tpaccept " + REQUEST_ID + " extra").isEmpty());
    }
}

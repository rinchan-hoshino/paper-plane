package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PaperPlaneFlightModelTest {
    @Test
    void chargeRequiresAQuarterSecondAndCapsAtTwoSeconds() {
        assertFalse(PaperPlaneFlightModel.canLaunch(4));
        assertTrue(PaperPlaneFlightModel.canLaunch(5));
        assertEquals(0.65D, PaperPlaneFlightModel.launchSpeed(5), 1.0E-9D);
        assertEquals(1.65D, PaperPlaneFlightModel.launchSpeed(40), 1.0E-9D);
        assertEquals(1.65D, PaperPlaneFlightModel.launchSpeed(200), 1.0E-9D);
    }

    @Test
    void flatGlideRangeScalesFromAWeakPuffToAboutFortyFiveBlocks() {
        double weak = PaperPlaneFlightModel.simulateFlatRange(PaperPlaneFlightModel.launchSpeed(5), 1.5D);
        double medium = PaperPlaneFlightModel.simulateFlatRange(PaperPlaneFlightModel.launchSpeed(20), 1.5D);
        double full = PaperPlaneFlightModel.simulateFlatRange(PaperPlaneFlightModel.launchSpeed(40), 1.5D);
        assertTrue(weak >= 5.0D && weak <= 8.0D, () -> "weak=" + weak);
        assertTrue(medium > weak);
        assertTrue(full >= 40.0D && full <= 50.0D, () -> "full=" + full);
    }

    @Test
    void slowPlanesStallAndLoseAltitude() {
        PaperPlaneFlightModel.Velocity result = PaperPlaneFlightModel.step(new PaperPlaneFlightModel.Velocity(0.2D, 0.0D, 0.0D));
        assertTrue(result.stalled());
        assertTrue(result.y() < 0.0D);
        assertTrue(result.horizontalSpeed() < 0.2D);
    }
}

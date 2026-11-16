package dev.rinchan.paperplane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PaperPlaneFlightModelTest {


    @Test
    void slowPlanesStallAndLoseAltitude() {
        PaperPlaneFlightModel.Velocity result = PaperPlaneFlightModel.step(new PaperPlaneFlightModel.Velocity(0.2D, 0.0D, 0.0D));
        assertTrue(result.stalled());
        assertTrue(result.y() < 0.0D);
        assertTrue(result.horizontalSpeed() < 0.2D);
    }
}

package dev.rinchan.paperplane;

/** Deterministic low-speed glider model expressed in blocks per tick. */
public final class PaperPlaneFlightModel {
    private static final double STALL_SPEED = 0.32D;
    private static final double BASE_DRAG = 0.009D;
    private static final double SPEED_DRAG = 0.003D;
    private static final double LIFT_COEFFICIENT = 0.024D;
    private static final double GRAVITY = 0.05D;
    private static final double VERTICAL_DAMPING = 0.985D;

    private PaperPlaneFlightModel() {
    }



    public static Velocity step(Velocity velocity) {
        double horizontalSpeed = velocity.horizontalSpeed();
        double speed = Math.sqrt(horizontalSpeed * horizontalSpeed + velocity.y() * velocity.y());
        boolean stalled = horizontalSpeed < STALL_SPEED;
        double drag = BASE_DRAG + SPEED_DRAG * speed + (stalled ? 0.025D : 0.0D);
        double horizontalFactor = Math.max(0.0D, 1.0D - drag);
        double lift = horizontalSpeed * horizontalSpeed * LIFT_COEFFICIENT * (stalled ? 0.3D : 1.0D);
        double gravity = GRAVITY + (stalled ? 0.01D : 0.0D);
        return new Velocity(
            velocity.x() * horizontalFactor,
            (velocity.y() + lift - gravity) * VERTICAL_DAMPING,
            velocity.z() * horizontalFactor
        );
    }


    public record Velocity(double x, double y, double z) {
        public double horizontalSpeed() {
            return Math.sqrt(x * x + z * z);
        }

        public boolean stalled() {
            return horizontalSpeed() < STALL_SPEED;
        }
    }
}

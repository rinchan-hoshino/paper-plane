package dev.rinchan.paperplane;

/** Deterministic low-speed glider model expressed in blocks per tick. */
public final class PaperPlaneFlightModel {
    public static final int MIN_CHARGE_TICKS = 5;
    public static final int MAX_CHARGE_TICKS = 40;
    private static final double MIN_LAUNCH_SPEED = 0.65D;
    private static final double MAX_LAUNCH_SPEED = 1.65D;
    private static final double STALL_SPEED = 0.32D;
    private static final double BASE_DRAG = 0.009D;
    private static final double SPEED_DRAG = 0.003D;
    private static final double LIFT_COEFFICIENT = 0.024D;
    private static final double GRAVITY = 0.05D;
    private static final double VERTICAL_DAMPING = 0.985D;

    private PaperPlaneFlightModel() {
    }

    public static boolean canLaunch(int chargeTicks) {
        return chargeTicks >= MIN_CHARGE_TICKS;
    }

    public static double launchSpeed(int chargeTicks) {
        if (!canLaunch(chargeTicks)) {
            return 0.0D;
        }
        double fraction = Math.min(1.0D, (chargeTicks - MIN_CHARGE_TICKS) / (double) (MAX_CHARGE_TICKS - MIN_CHARGE_TICKS));
        double eased = fraction * fraction * (3.0D - 2.0D * fraction);
        return MIN_LAUNCH_SPEED + (MAX_LAUNCH_SPEED - MIN_LAUNCH_SPEED) * eased;
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

    public static double simulateFlatRange(double launchSpeed, double startHeight) {
        Velocity velocity = new Velocity(launchSpeed, 0.0D, 0.0D);
        double x = 0.0D;
        double y = startHeight;
        for (int tick = 0; tick < 600 && y > 0.0D; tick++) {
            velocity = step(velocity);
            x += velocity.x();
            y += velocity.y();
        }
        return x;
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

package dev.rinchan.paperplane;

public enum PlaneKind {
    NORMAL(true, true),
    SOGGY(true, false),
    ENDER(false, true);

    private final boolean consumable;
    private final boolean throwable;

    PlaneKind(boolean consumable, boolean throwable) {
        this.consumable = consumable;
        this.throwable = throwable;
    }

    public boolean consumable() {
        return consumable;
    }

    public boolean throwable() {
        return throwable;
    }
}

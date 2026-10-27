package dev.rinchan.paperplane;

public enum PlaneKind {
    NORMAL(true),
    SOGGY(true),
    ENDER(false);

    private final boolean consumable;

    PlaneKind(boolean consumable) {
        this.consumable = consumable;
    }

    public boolean consumable() {
        return consumable;
    }

}

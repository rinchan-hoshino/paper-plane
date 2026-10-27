package dev.rinchan.paperplane;

public enum PlaneKind {
    NORMAL(true),
    SOGGY(true),
    ENDER(false),
    BACK(true),
    ENDER_BACK(false);

    private final boolean consumable;

    PlaneKind(boolean consumable) {
        this.consumable = consumable;
    }

    public boolean consumable() {
        return consumable;
    }

    public boolean returning() {
        return this == BACK || this == ENDER_BACK;
    }

}

package dev.rinchan.paperplane;

public enum PlaneKind {
    NORMAL(true),
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

    public boolean ender() {
        return this == ENDER || this == ENDER_BACK;
    }

    public String tooltipKey() {
        return switch (this) {
            case NORMAL -> "tooltip.paper_plane.paper_plane";
            case ENDER -> "tooltip.paper_plane.ender_paper_plane";
            case BACK -> "tooltip.paper_plane.back_paper_plane";
            case ENDER_BACK -> "tooltip.paper_plane.ender_back_paper_plane";
        };
    }
}

package ca.wildcraft.anticheat.model;

public enum CaseStatus {
    OPEN, WATCHING, CLEARED, CONFIRMED;

    public String display() {
        return switch (this) {
            case OPEN -> "Open";
            case WATCHING -> "Watching";
            case CLEARED -> "Cleared";
            case CONFIRMED -> "Confirmed";
        };
    }
}

package example.phaseteleporters;

/** Status sent to the framed teleporter screens. */
public final class PortalStatus {
    public static final int NO_FRAME = 0;
    public static final int NO_ENERGY = 1;
    public static final int REDSTONE = 2;
    public static final int NO_FREQUENCY = 3;
    public static final int NO_DESTINATION = 4;
    public static final int ACTIVE = 5;

    private PortalStatus() {}

    public static int of(boolean frame, long energy, boolean canWork,
            boolean hasFrequency, boolean linked) {
        if (!frame) return NO_FRAME;
        if (energy < 500) return NO_ENERGY;
        if (!canWork) return REDSTONE;
        if (!hasFrequency) return NO_FREQUENCY;
        return linked ? ACTIVE : NO_DESTINATION;
    }

    public static String key(int status) {
        return switch (status) {
            case NO_FRAME -> "no_frame";
            case NO_ENERGY -> "no_energy";
            case REDSTONE -> "redstone";
            case NO_FREQUENCY -> "no_frequency";
            case ACTIVE -> "active";
            default -> "no_destination";
        };
    }
}

package example.phaseteleporters.energy;

public enum PERedstoneMode {
    IGNORED,
    WITHOUT_SIGNAL,
    WITH_SIGNAL;

    public static PERedstoneMode byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : IGNORED;
    }

    public boolean allowsWork(boolean powered) {
        return switch (this) {
            case IGNORED -> true;
            case WITHOUT_SIGNAL -> !powered;
            case WITH_SIGNAL -> powered;
        };
    }
}

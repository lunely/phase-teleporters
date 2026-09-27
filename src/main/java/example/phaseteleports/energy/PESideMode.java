package example.phaseteleports.energy;

public enum PESideMode {
    INPUT(true, false, 0xFFB83B46),
    OUTPUT(false, true, 0xFF376DB7),
    INPUT_OUTPUT(true, true, 0xFF8051A8),
    DISABLED(false, false, 0xFF777777);

    private final boolean input;
    private final boolean output;
    private final int color;

    PESideMode(boolean input, boolean output, int color) {
        this.input = input;
        this.output = output;
        this.color = color;
    }

    public boolean allowsInput() { return input; }
    public boolean allowsOutput() { return output; }
    public int color() { return color; }
    public static PESideMode byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : DISABLED;
    }
}

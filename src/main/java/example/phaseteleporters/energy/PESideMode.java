package example.phaseteleporters.energy;

public enum PESideMode {
    INPUT(true, false, 0xFF4B9B58),
    OUTPUT(false, true, 0xFF4B9B58),
    INPUT_OUTPUT(true, true, 0xFF4B9B58),
    DISABLED(false, false, 0xFF777777),
    ITEM_INPUT(false, false, 0xFFD58A3C),
    ITEM_OUTPUT(false, false, 0xFFD58A3C),
    ENERGY_ITEM_INPUT(true, false, 0xFF935CC4),
    ENERGY_ITEM_OUTPUT(false, true, 0xFF935CC4);

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
    public boolean allowsItemInput() {
        return this == ITEM_INPUT || this == ENERGY_ITEM_INPUT;
    }
    public boolean allowsItemOutput() {
        return this == ITEM_OUTPUT || this == ENERGY_ITEM_OUTPUT;
    }
    public int color() { return color; }
    public static PESideMode byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : DISABLED;
    }
}

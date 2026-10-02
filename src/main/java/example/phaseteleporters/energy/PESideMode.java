package example.phaseteleporters.energy;

public enum PESideMode {
    INPUT(true, false, 0xFF4B9B58),
    OUTPUT(false, true, 0xFF4B9B58),
    INPUT_OUTPUT(true, true, 0xFF4B9B58),
    DISABLED(false, false, 0xFF777777),
    ITEM_INPUT(false, false, 0xFFD58A3C),
    ITEM_OUTPUT(false, false, 0xFFD58A3C),
    ENERGY_ITEM_INPUT(true, false, 0xFF935CC4),
    ENERGY_ITEM_OUTPUT(false, true, 0xFF935CC4),
    ALL_INPUT(true, false, 0xFF935CC4),
    ALL_OUTPUT(false, true, 0xFF935CC4),
    // Append modes to preserve existing saved side IDs.
    FLUID_INPUT(false, false, 0xFF355FE8),
    FLUID_OUTPUT(false, false, 0xFF355FE8);

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
        return this == ITEM_INPUT || this == ENERGY_ITEM_INPUT || this == ALL_INPUT;
    }
    public boolean allowsItemOutput() {
        return this == ITEM_OUTPUT || this == ENERGY_ITEM_OUTPUT || this == ALL_OUTPUT;
    }
    public boolean allowsFluidInput() {
        return this == FLUID_INPUT || this == ALL_INPUT;
    }
    public boolean allowsFluidOutput() {
        return this == FLUID_OUTPUT || this == ALL_OUTPUT;
    }
    public int color() { return color; }
    public String translationKey() {
        return "gui.phaseteleporters.energy_mode." + name().toLowerCase(java.util.Locale.ROOT);
    }
    public static PESideMode byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : DISABLED;
    }
}

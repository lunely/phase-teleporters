package example.phaseteleporters;

/** Shared, fixed palette for local frequencies and portal plane tinting. */
public final class PortalColors {
    public static final int DEFAULT = 0;
    private static final int[] RGB = {
            0x59B4FF, 0x24EEE0, 0x35E257, 0xFFEA4A,
            0xFF9D36, 0xFF4856, 0xE44CF2, 0x9365EE,
            0xFFFFFF, 0xD4D8DD, 0x8F959D, 0x17191D,
            0x9C6040, 0xFFB6D5, 0xB0F541, 0x306DF8,
            0xAB1730, 0x079DAD, 0xFFC69B, 0xB7A5F5
    };
    // Keep persisted color IDs stable; presentation follows the requested palette order.
    private static final int[] DISPLAY_ORDER = {
            13, 11, 15, 2, 1, 16, 7, 4, 9, 10, 0, 14, 17, 5, 6, 18, 3, 8, 12, 19
    };
    private static final String[] NAMES = {
            "light_blue", "turquoise", "green", "yellow", "orange", "red",
            "magenta", "purple", "white", "light_gray", "gray", "black",
            "brown", "pink", "lime", "blue", "dark_red", "sea", "peach", "lavender"
    };

    private PortalColors() {}

    public static int count() { return RGB.length; }
    public static boolean isValid(int color) { return color >= 0 && color < RGB.length; }
    public static int rgb(int color) { return RGB[isValid(color) ? color : DEFAULT]; }
    public static int displayColor(int position) { return DISPLAY_ORDER[position]; }
    public static String nameKey(int color) {
        return "gui.phaseteleporters.color." + NAMES[isValid(color) ? color : DEFAULT];
    }
}

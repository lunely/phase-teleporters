package example.phaseteleports;

/** Shared, fixed palette for local frequencies and portal plane tinting. */
public final class PortalColors {
    public static final int DEFAULT = 0;
    private static final int[] RGB = {
            0x4B8FE8, 0x38C7D8, 0x54C96A, 0xE2CC4E,
            0xEC893D, 0xDA5555, 0xCE64B7, 0x8355CC,
            0xEEEFEF, 0xA9B3C1, 0x596575, 0x222735,
            0x8C6547, 0xE9A5BD, 0x8FC44B, 0x5261AD
    };

    private PortalColors() {}

    public static int count() { return RGB.length; }
    public static boolean isValid(int color) { return color >= 0 && color < RGB.length; }
    public static int rgb(int color) { return RGB[isValid(color) ? color : DEFAULT]; }
}

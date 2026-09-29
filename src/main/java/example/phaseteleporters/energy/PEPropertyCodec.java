package example.phaseteleporters.energy;

import net.minecraft.screen.PropertyDelegate;

/** Screen handler properties are 16-bit on the wire; split each PE value in two. */
public final class PEPropertyCodec {
    public static final int PROPERTY_COUNT = 4;

    private PEPropertyCodec() {}

    public static int part(PEStorage storage, int index) {
        long value = index < 2 ? storage.getStored() : storage.getCapacity();
        return (int) (value >>> (16 * (index & 1)) & 0xFFFF);
    }

    public static long stored(PropertyDelegate properties, int offset) {
        return combine(properties.get(offset), properties.get(offset + 1));
    }

    public static long capacity(PropertyDelegate properties, int offset) {
        return combine(properties.get(offset + 2), properties.get(offset + 3));
    }

    private static long combine(int low, int high) {
        return (low & 0xFFFFL) | ((high & 0xFFFFL) << 16);
    }
}

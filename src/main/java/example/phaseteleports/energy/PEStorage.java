package example.phaseteleports.energy;

/** Amounts are PE; insert and extract return the amount actually transferred. */
public interface PEStorage {
    long getStored();
    long getCapacity();
    long insert(long amount, boolean simulate);
    long extract(long amount, boolean simulate);

    default boolean isInfinite() { return false; }
}

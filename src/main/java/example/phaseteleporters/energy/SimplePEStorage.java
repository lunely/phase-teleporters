package example.phaseteleporters.energy;

/** Reusable finite PE buffer for future machines and energy blocks. */
public final class SimplePEStorage implements PEStorage {
    private final long capacity;
    private long stored;

    public SimplePEStorage(long capacity) {
        if (capacity < 0) throw new IllegalArgumentException("PE capacity must be nonnegative");
        this.capacity = capacity;
    }

    @Override public long getStored() { return stored; }
    @Override public long getCapacity() { return capacity; }

    @Override public Runnable createEnergySnapshot() {
        long saved = stored;
        return () -> stored = saved;
    }

    public void setStored(long amount) {
        stored = Math.clamp(amount, 0L, capacity);
    }

    @Override public long insert(long amount, boolean simulate) {
        if (amount <= 0) return 0;
        long accepted = Math.min(amount, capacity - stored);
        if (!simulate) stored += accepted;
        return accepted;
    }

    @Override public long extract(long amount, boolean simulate) {
        if (amount <= 0) return 0;
        long extracted = Math.min(amount, stored);
        if (!simulate) stored -= extracted;
        return extracted;
    }
}

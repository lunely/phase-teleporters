package example.phaseteleporters.energy;

/** Amounts are PE; insert and extract return the amount actually transferred. */
public interface PEStorage {
    long getStored();
    long getCapacity();
    long insert(long amount, boolean simulate);
    long extract(long amount, boolean simulate);

    /** Restore local energy and transfer quotas when a transaction is aborted. */
    Runnable createEnergySnapshot();

    /** Shared buffers and cable networks enlist every affected storage once. */
    default Iterable<? extends PEStorage> transactionParticipants() { return java.util.List.of(this); }

    default boolean isInfinite() { return false; }

    /** Transfer only what the receiver can accept, refunding any unexpected shortfall. */
    default long transferTo(PEStorage receiver, long limit) {
        if (receiver == this || limit <= 0) return 0;
        long free = Math.max(0L, receiver.getCapacity() - receiver.getStored());
        long offered = Math.min(limit, Math.min(getStored(), free));
        if (offered <= 0) return 0;
        long accepted = receiver.insert(extract(offered, true), true);
        if (accepted <= 0) return 0;
        long sent = extract(accepted, false);
        long received = receiver.insert(sent, false);
        if (received < sent) insert(sent - received, false);
        return received;
    }
}

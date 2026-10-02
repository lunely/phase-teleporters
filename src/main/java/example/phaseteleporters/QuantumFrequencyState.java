package example.phaseteleporters;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import example.phaseteleporters.energy.PEStorage;
import example.phaseteleporters.energy.SimplePEStorage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

/** Shared across dimensions; private entries are addressed by creator UUID. */
public final class QuantumFrequencyState extends PersistentState {
    public static final int MAX_NAME_LENGTH = 32;
    public static final int DEFAULT_COLOR = 7;
    private static final int MAX_FREQUENCIES = 256;
    private static final Type<QuantumFrequencyState> TYPE =
            new Type<>(QuantumFrequencyState::new, QuantumFrequencyState::fromNbt, null);
    private final List<Frequency> frequencies = new ArrayList<>();
    // Runtime endpoints only: never saved and never used to load remote chunks.
    private final Set<QuantumTeleportBlockEntity> loadedTeleporters = new LinkedHashSet<>();

    public void register(QuantumTeleportBlockEntity teleport) { loadedTeleporters.add(teleport); }
    public void unregister(QuantumTeleportBlockEntity teleport) { loadedTeleporters.remove(teleport); }
    public List<QuantumTeleportBlockEntity> loadedTeleporters() { return List.copyOf(loadedTeleporters); }

    public record Frequency(String name, UUID owner, int color, UUID creator, String creatorName,
            PEStorage energy) {
        public boolean isPrivate() { return owner != null; }
    }

    /** The frequency owns this buffer; block entities only expose access to it. */
    private final class FrequencyEnergy implements PEStorage {
        private final SimplePEStorage buffer = new SimplePEStorage(QuantumTeleportBlockEntity.CAPACITY);

        FrequencyEnergy(long stored) { buffer.setStored(stored); }
        @Override public long getStored() { return buffer.getStored(); }
        @Override public long getCapacity() { return buffer.getCapacity(); }
        @Override public Runnable createEnergySnapshot() {
            Runnable saved = buffer.createEnergySnapshot();
            return () -> {
                saved.run();
                QuantumFrequencyState.this.markDirty();
            };
        }
        @Override public long insert(long amount, boolean simulate) {
            long accepted = buffer.insert(amount, simulate);
            if (accepted > 0 && !simulate) QuantumFrequencyState.this.markDirty();
            return accepted;
        }
        @Override public long extract(long amount, boolean simulate) {
            long extracted = buffer.extract(amount, simulate);
            if (extracted > 0 && !simulate) QuantumFrequencyState.this.markDirty();
            return extracted;
        }
    }

    public static QuantumFrequencyState get(ServerWorld world) {
        return world.getServer().getWorld(World.OVERWORLD).getPersistentStateManager()
                .getOrCreate(TYPE, "phaseteleporters_quantum_frequencies");
    }

    private static QuantumFrequencyState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        QuantumFrequencyState state = new QuantumFrequencyState();
        NbtList entries = nbt.getList("QuantumFrequencyEntries", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < entries.size(); i++) {
            NbtCompound entry = entries.getCompound(i);
            UUID owner = null;
            if (entry.getBoolean("Private")) {
                try { owner = UUID.fromString(entry.getString("Owner")); }
                catch (IllegalArgumentException ignored) { continue; }
            }
            UUID creator = owner;
            if (entry.contains("Creator", NbtElement.STRING_TYPE)) {
                try { creator = UUID.fromString(entry.getString("Creator")); }
                catch (IllegalArgumentException ignored) { creator = owner; }
            }
            state.addLoaded(normalize(entry.getString("Name")), owner, entry.getInt("Color"),
                    creator, entry.getString("CreatorName"), entry.getLong("PE"));
        }
        return state;
    }

    private void addLoaded(String name, UUID owner, int color, UUID creator, String creatorName, long energy) {
        if (!name.isEmpty() && frequencies.size() < MAX_FREQUENCIES
                && !contains(name, owner != null, owner)) {
            frequencies.add(new Frequency(name, owner, PortalColors.isValid(color) ? color : QuantumFrequencyState.DEFAULT_COLOR,
                    creator, creatorName, new FrequencyEnergy(energy)));
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtList entries = new NbtList();
        for (Frequency frequency : frequencies) {
            NbtCompound entry = new NbtCompound();
            entry.putString("Name", frequency.name());
            entry.putBoolean("Private", frequency.isPrivate());
            if (frequency.isPrivate()) entry.putString("Owner", frequency.owner().toString());
            if (frequency.creator() != null) entry.putString("Creator", frequency.creator().toString());
            entry.putString("CreatorName", frequency.creatorName());
            entry.putInt("Color", frequency.color());
            entry.putLong("PE", frequency.energy().getStored());
            entries.add(entry);
        }
        nbt.put("QuantumFrequencyEntries", entries);
        return nbt;
    }

    public List<Frequency> visibleTo(UUID player, boolean privateTab) {
        return frequencies.stream().filter(f -> f.isPrivate() == privateTab
                && (!privateTab || f.owner().equals(player))).toList();
    }

    private int indexOf(String name, boolean privateFrequency, UUID owner) {
        for (int i = 0; i < frequencies.size(); i++) {
            Frequency f = frequencies.get(i);
            if (f.name().equals(name) && f.isPrivate() == privateFrequency
                    && (!privateFrequency || f.owner().equals(owner))) return i;
        }
        return -1;
    }

    public boolean contains(String name, boolean privateFrequency, UUID owner) {
        return indexOf(name, privateFrequency, owner) >= 0;
    }

    public PEStorage energy(String name, boolean privateFrequency, UUID owner) {
        int index = indexOf(name, privateFrequency, owner);
        return index < 0 ? null : frequencies.get(index).energy();
    }

    public int color(String name, boolean privateFrequency, UUID owner) {
        int index = indexOf(name, privateFrequency, owner);
        return index < 0 ? QuantumFrequencyState.DEFAULT_COLOR : frequencies.get(index).color();
    }

    public boolean create(String name, boolean privateFrequency, UUID owner, String creatorName) {
        if (name.isEmpty() || (privateFrequency && owner == null) || frequencies.size() >= MAX_FREQUENCIES
                || contains(name, privateFrequency, owner)) return false;
        frequencies.add(new Frequency(name, privateFrequency ? owner : null, QuantumFrequencyState.DEFAULT_COLOR,
                owner, creatorName, new FrequencyEnergy(0)));
        markDirty();
        return true;
    }

    public boolean setColor(String name, boolean privateFrequency, UUID owner, int color) {
        int index = indexOf(name, privateFrequency, owner);
        if (index < 0 || !PortalColors.isValid(color)) return false;
        Frequency old = frequencies.get(index);
        if (old.color() != color) {
            frequencies.set(index, new Frequency(name, old.owner(), color, old.creator(), old.creatorName(),
                    old.energy()));
            markDirty();
        }
        return true;
    }

    public boolean delete(String name, boolean privateFrequency, UUID owner) {
        int index = indexOf(name, privateFrequency, owner);
        if (index < 0) return false;
        frequencies.remove(index);
        markDirty();
        return true;
    }

    public static String normalize(String name) {
        String trimmed = name.trim();
        return trimmed.length() <= MAX_NAME_LENGTH ? trimmed : "";
    }
}

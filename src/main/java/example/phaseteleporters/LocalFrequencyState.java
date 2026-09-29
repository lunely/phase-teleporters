package example.phaseteleporters;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

/** Frequencies belong to one dimension; private entries are addressed by creator UUID. */
public final class LocalFrequencyState extends PersistentState {
    public static final int MAX_NAME_LENGTH = 32;
    private static final int MAX_FREQUENCIES = 256;
    private static final Type<LocalFrequencyState> TYPE =
            new Type<>(LocalFrequencyState::new, LocalFrequencyState::fromNbt, null);
    private final List<Frequency> frequencies = new ArrayList<>();

    public record Frequency(String name, UUID owner, int color, UUID creator, String creatorName) {
        public boolean isPrivate() { return owner != null; }
    }

    public static LocalFrequencyState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, "phaseteleporters_local_frequencies");
    }

    private static LocalFrequencyState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        LocalFrequencyState state = new LocalFrequencyState();
        if (!nbt.contains("LocalFrequencyEntries", NbtElement.LIST_TYPE)) {
            // Migrate worlds created before private frequencies existed.
            NbtList oldNames = nbt.getList("Frequencies", NbtElement.STRING_TYPE);
            NbtList oldColors = nbt.getList("FrequencyColors", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < oldNames.size(); i++) {
                String name = normalize(oldNames.getString(i));
                int color = PortalColors.DEFAULT;
                for (int j = 0; j < oldColors.size(); j++) {
                    NbtCompound entry = oldColors.getCompound(j);
                    if (name.equals(entry.getString("Name")) && PortalColors.isValid(entry.getInt("Color"))) {
                        color = entry.getInt("Color");
                        break;
                    }
                }
                state.addLoaded(name, null, color, null, "");
            }
            if (!oldNames.isEmpty()) state.markDirty();
        }
        NbtList entries = nbt.getList("LocalFrequencyEntries", NbtElement.COMPOUND_TYPE);
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
                    creator, entry.getString("CreatorName"));
        }
        return state;
    }

    private void addLoaded(String name, UUID owner, int color, UUID creator, String creatorName) {
        if (!name.isEmpty() && frequencies.size() < MAX_FREQUENCIES
                && !contains(name, owner != null, owner)) {
            frequencies.add(new Frequency(name, owner, PortalColors.isValid(color) ? color : PortalColors.DEFAULT,
                    creator, creatorName));
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.remove("Frequencies");
        nbt.remove("FrequencyColors");
        NbtList entries = new NbtList();
        for (Frequency frequency : frequencies) {
            NbtCompound entry = new NbtCompound();
            entry.putString("Name", frequency.name());
            entry.putBoolean("Private", frequency.isPrivate());
            if (frequency.isPrivate()) entry.putString("Owner", frequency.owner().toString());
            if (frequency.creator() != null) entry.putString("Creator", frequency.creator().toString());
            entry.putString("CreatorName", frequency.creatorName());
            entry.putInt("Color", frequency.color());
            entries.add(entry);
        }
        nbt.put("LocalFrequencyEntries", entries);
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

    public int color(String name, boolean privateFrequency, UUID owner) {
        int index = indexOf(name, privateFrequency, owner);
        return index < 0 ? PortalColors.DEFAULT : frequencies.get(index).color();
    }

    public boolean create(String name, boolean privateFrequency, UUID owner, String creatorName) {
        if (name.isEmpty() || (privateFrequency && owner == null) || frequencies.size() >= MAX_FREQUENCIES
                || contains(name, privateFrequency, owner)) return false;
        frequencies.add(new Frequency(name, privateFrequency ? owner : null, PortalColors.DEFAULT,
                owner, creatorName));
        markDirty();
        return true;
    }

    public boolean setColor(String name, boolean privateFrequency, UUID owner, int color) {
        int index = indexOf(name, privateFrequency, owner);
        if (index < 0 || !PortalColors.isValid(color)) return false;
        Frequency old = frequencies.get(index);
        if (old.color() != color) {
            frequencies.set(index, new Frequency(name, old.owner(), color, old.creator(), old.creatorName()));
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

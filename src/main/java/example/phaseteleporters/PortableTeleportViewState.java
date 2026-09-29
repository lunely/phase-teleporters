package example.phaseteleporters;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

/** The player's last opened portable tabs, stored without changing the held item. */
public final class PortableTeleportViewState extends PersistentState {
    private static final Type<PortableTeleportViewState> TYPE =
            new Type<>(PortableTeleportViewState::new, PortableTeleportViewState::fromNbt, null);
    private final Map<UUID, PortableTeleportItem.View> views = new HashMap<>();

    public static PortableTeleportViewState get(ServerWorld world) {
        return world.getServer().getWorld(World.OVERWORLD).getPersistentStateManager()
                .getOrCreate(TYPE, "phaseteleporters_portable_views");
    }

    public PortableTeleportItem.View viewFor(UUID player, PortableTeleportItem.View fallback) {
        return views.getOrDefault(player, fallback);
    }

    public void setView(UUID player, PortableTeleportItem.View view) {
        if (view.equals(views.put(player, view))) return;
        markDirty();
    }

    private static PortableTeleportViewState fromNbt(NbtCompound nbt,
            RegistryWrapper.WrapperLookup lookup) {
        PortableTeleportViewState state = new PortableTeleportViewState();
        NbtList entries = nbt.getList("Views", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < entries.size(); i++) {
            NbtCompound entry = entries.getCompound(i);
            try {
                state.views.put(UUID.fromString(entry.getString("Player")),
                        new PortableTeleportItem.View(entry.getBoolean("Private"),
                                entry.getBoolean("Interdimensional")));
            } catch (IllegalArgumentException ignored) {}
        }
        return state;
    }

    @Override public NbtCompound writeNbt(NbtCompound nbt,
            RegistryWrapper.WrapperLookup lookup) {
        NbtList entries = new NbtList();
        for (var saved : views.entrySet()) {
            NbtCompound entry = new NbtCompound();
            entry.putString("Player", saved.getKey().toString());
            entry.putBoolean("Private", saved.getValue().privateFrequency());
            entry.putBoolean("Interdimensional", saved.getValue().interdimensional());
            entries.add(entry);
        }
        nbt.put("Views", entries);
        return nbt;
    }
}

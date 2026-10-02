package example.phaseteleporters;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

/** Saved per player, independent of portable frequency selection and held-item animations. */
public final class EmergencyTeleportState extends PersistentState {
    private static final Type<EmergencyTeleportState> TYPE =
            new Type<>(EmergencyTeleportState::new, EmergencyTeleportState::fromNbt, null);
    private final Map<UUID, Profile> profiles = new HashMap<>();

    public static final class Profile {
        public RegistryKey<World> dimension;
        public BlockPos pos;
        public UUID platform;
        public boolean enabled = true;
        public boolean rescueFalls = true;
        public int threshold = 4;
        public boolean skipTotem = true;
        public boolean skipWaterBucket;

        public boolean isBound() { return dimension != null && pos != null && platform != null; }
        public boolean matches(EmergencyTeleportBlockEntity pad) {
            return isBound() && platform.equals(pad.platformId()) && pos.equals(pad.getPos())
                    && dimension.equals(pad.getWorld().getRegistryKey());
        }
        public void bind(EmergencyTeleportBlockEntity pad) {
            dimension = pad.getWorld().getRegistryKey();
            pos = pad.getPos().toImmutable();
            platform = pad.platformId();
        }
        public void unbind() { dimension = null; pos = null; platform = null; }
    }

    public static EmergencyTeleportState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "phaseteleporters_emergency");
    }
    public Profile profile(UUID player) { return profiles.computeIfAbsent(player, unused -> new Profile()); }
    public Profile boundProfile(UUID player) {
        Profile found = profiles.get(player);
        return found != null && found.isBound() ? found : null;
    }

    private static EmergencyTeleportState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        EmergencyTeleportState saved = new EmergencyTeleportState();
        NbtList players = nbt.getList("Players", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < players.size(); i++) {
            NbtCompound entry = players.getCompound(i);
            if (!entry.containsUuid("Player")) continue;
            Profile profile = new Profile();
            profile.enabled = !entry.contains("Enabled") || entry.getBoolean("Enabled");
            profile.rescueFalls = !entry.contains("RescueFalls") || entry.getBoolean("RescueFalls");
            if (entry.contains("Threshold")) profile.threshold = Math.clamp(entry.getInt("Threshold"), 1, 20);
            profile.skipTotem = !entry.contains("SkipTotem") || entry.getBoolean("SkipTotem");
            profile.skipWaterBucket = entry.getBoolean("SkipWaterBucket");
            Identifier dimension = Identifier.tryParse(entry.getString("Dimension"));
            if (dimension != null && entry.containsUuid("Platform") && entry.contains("Position")) {
                profile.dimension = RegistryKey.of(RegistryKeys.WORLD, dimension);
                profile.pos = BlockPos.fromLong(entry.getLong("Position"));
                profile.platform = entry.getUuid("Platform");
            }
            saved.profiles.put(entry.getUuid("Player"), profile);
        }
        return saved;
    }

    @Override public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtList players = new NbtList();
        profiles.forEach((uuid, profile) -> {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("Player", uuid);
            entry.putBoolean("Enabled", profile.enabled);
            entry.putBoolean("RescueFalls", profile.rescueFalls);
            entry.putInt("Threshold", profile.threshold);
            entry.putBoolean("SkipTotem", profile.skipTotem);
            entry.putBoolean("SkipWaterBucket", profile.skipWaterBucket);
            if (profile.isBound()) {
                entry.putString("Dimension", profile.dimension.getValue().toString());
                entry.putLong("Position", profile.pos.asLong());
                entry.putUuid("Platform", profile.platform);
            }
            players.add(entry);
        });
        nbt.put("Players", players);
        return nbt;
    }
}

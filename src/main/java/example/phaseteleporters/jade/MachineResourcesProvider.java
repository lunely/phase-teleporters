package example.phaseteleporters.jade;

import example.phaseteleporters.InfusionResource;
import example.phaseteleporters.InfusionStationBlockEntity;
import example.phaseteleporters.PhaseTeleportersMod;
import example.phaseteleporters.energy.PEStorage;
import example.phaseteleporters.energy.StoredPEItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum MachineResourcesProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String DATA_KEY = "phaseteleporters:machine_resources";
    private static final Identifier UID = Identifier.of(PhaseTeleportersMod.MOD_ID, "machine_resources");

    @Override public Identifier getUid() { return UID; }

    @Override public boolean shouldRequestData(BlockAccessor accessor) {
        return accessor.getPlayer() != null && accessor.getPlayer().isCreative()
                && accessor.getBlockEntity() instanceof PEStorage;
    }

    @Override public void appendServerData(NbtCompound data, BlockAccessor accessor) {
        if (!shouldRequestData(accessor)) return;
        PEStorage storage = (PEStorage) accessor.getBlockEntity();
        NbtCompound resources = new NbtCompound();
        resources.putLong("Energy", storage.getStored());
        resources.putLong("Capacity", storage.getCapacity());
        resources.putBoolean("Infinite", storage.isInfinite());
        if (accessor.getBlockEntity() instanceof InfusionStationBlockEntity station) {
            resources.putBoolean("InfusionStation", true);
            station.writeStoredInfusion(resources);
        }
        data.put(DATA_KEY, resources);
    }

    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getPlayer() == null || !accessor.getPlayer().isCreative()
                || !accessor.getServerData().contains(DATA_KEY)) return;
        NbtCompound resources = accessor.getServerData().getCompound(DATA_KEY);
        tooltip.add(resources.getBoolean("Infinite")
                ? Text.translatable("gui.phaseteleporters.inspection.infinite").formatted(Formatting.GREEN)
                : StoredPEItem.energyTooltip(resources.getLong("Energy"), resources.getLong("Capacity")));
        if (resources.getBoolean("InfusionStation")) {
            InfusionResource resource = InfusionResource.byId(resources.getInt("InfusionResource"));
            Text name = resource == InfusionResource.NONE
                    ? Text.translatable("gui.phaseteleporters.inspection.empty") : resource.item().getName();
            tooltip.add(Text.translatable("gui.phaseteleporters.inspection.infusion", name,
                    resources.getInt("InfusionAmount"), InfusionStationBlockEntity.MAX_INFUSION)
                    .formatted(Formatting.GRAY));
        }
    }
}

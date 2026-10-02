package example.phaseteleporters;

import example.phaseteleporters.energy.PEStorage;
import example.phaseteleporters.energy.PEBlockEntity;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Carries stored resources through the normal block loot and placement paths. */
public abstract class StoredEnergyBlock extends BlockWithEntity {
    protected StoredEnergyBlock(Settings settings) { super(settings); }

    @Override
    protected List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        List<ItemStack> drops = super.getDroppedStacks(state, builder);
        BlockEntity entity = builder.getOptional(LootContextParameters.BLOCK_ENTITY);
        NbtCompound stored = new NbtCompound();
        if (entity instanceof PEStorage storage && !storage.isInfinite()
                && !(entity instanceof QuantumTeleportBlockEntity)) {
            long energy = storage.getStored();
            if (energy > 0) stored.putLong("PE", energy);
        }
        if (entity instanceof InfusionStationBlockEntity station) station.writeStoredInfusion(stored);
        if (entity instanceof AnchoredTeleportBlockEntity teleport) teleport.writeStoredAnchorUpgrade(stored);
        if (entity instanceof QuantumTeleportBlockEntity teleport) teleport.writeStoredFrequency(stored);
        if (!stored.isEmpty()) {
            for (ItemStack drop : drops) {
                if (!drop.isOf(asItem())) continue;
                NbtComponent component = drop.get(DataComponentTypes.CUSTOM_DATA);
                NbtCompound data = component == null ? new NbtCompound() : component.copyNbt();
                data.copyFrom(stored);
                NbtComponent.set(DataComponentTypes.CUSTOM_DATA, drop, data);
            }
        }
        return drops;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (world.isClient) return;
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (component == null) return;
        NbtCompound data = component.copyNbt();
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof PEBlockEntity machine)
            machine.restoreStoredEnergy(Math.max(0, data.getLong("PE")));
        else if (entity instanceof PEStorage storage && !storage.isInfinite())
            storage.insert(Math.max(0, data.getLong("PE")), false);
        if (entity instanceof InfusionStationBlockEntity station) station.readStoredInfusion(data);
        if (entity instanceof AnchoredTeleportBlockEntity teleport) teleport.readStoredAnchorUpgrade(data);
        if (entity instanceof QuantumTeleportBlockEntity teleport) teleport.readStoredFrequency(data);
    }
}

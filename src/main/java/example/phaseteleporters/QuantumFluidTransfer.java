package example.phaseteleporters;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Pulls directly from adjacent fluid stores into remote outputs without a local buffer. */
public final class QuantumFluidTransfer {
    private QuantumFluidTransfer() {}

    public static void tick(QuantumTeleportBlockEntity input) {
        if (!(input.getWorld() instanceof ServerWorld world) || !input.canWork()) return;
        QuantumFrequencyState frequencies = QuantumFrequencyState.get(world);
        if (!frequencies.contains(input.getFrequency(), input.isPrivateFrequency(), input.getFrequencyOwner())) return;

        for (Direction side : Direction.values()) {
            if (!input.getSideMode(side).allowsFluidInput()) continue;
            BlockPos sourcePos = input.getPos().offset(side);
            if (!world.isChunkLoaded(sourcePos.getX() >> 4, sourcePos.getZ() >> 4)
                    || world.getBlockEntity(sourcePos) instanceof AnchoredTeleportBlockEntity) continue;
            Storage<FluidVariant> source = AdAstraFluidPipeStorage.find(world, sourcePos, side.getOpposite());
            Storage<FluidVariant> target = FluidStorage.SIDED.find(world, input.getPos(), side);
            if (source == null || target == null || source instanceof QuantumFluidStorage
                    || !source.supportsExtraction() || !target.supportsInsertion()) continue;
            // One bucket per input face per tick; both operations commit or roll back together.
            StorageUtil.move(source, target, variant -> true, FluidConstants.BUCKET, null);
        }
    }
}

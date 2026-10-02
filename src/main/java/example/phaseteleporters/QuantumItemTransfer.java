package example.phaseteleporters;

import example.phaseteleporters.energy.PEBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Chest to remote inventory, in one transaction; nothing is held inside a teleporter. */
public final class QuantumItemTransfer {
    private QuantumItemTransfer() {}

    public static void tick(QuantumTeleportBlockEntity input) {
        if (!(input.getWorld() instanceof ServerWorld world) || !input.canWork()) return;
        QuantumFrequencyState frequencies = QuantumFrequencyState.get(world);
        if (!frequencies.contains(input.getFrequency(), input.isPrivateFrequency(), input.getFrequencyOwner())) return;

        for (Direction inputSide : Direction.values()) {
            if (!input.getSideMode(inputSide).allowsItemInput()) continue;
            BlockPos chestPos = input.getPos().offset(inputSide);
            if (!loaded(world, chestPos) || !(world.getBlockState(chestPos).getBlock() instanceof ChestBlock)) continue;
            Storage<ItemVariant> source = ItemStorage.SIDED.find(world, chestPos, inputSide.getOpposite());
            if (source == null || !source.supportsExtraction()) continue;

            for (QuantumTeleportBlockEntity output : frequencies.loadedTeleporters()) {
                if (output == input || output.isRemoved() || !output.canWork()
                        || !output.matchesFrequency(input.getFrequency(), input.isPrivateFrequency(), input.getFrequencyOwner())
                        || !(output.getWorld() instanceof ServerWorld targetWorld)
                        || !loaded(targetWorld, output.getPos())) continue;
                boolean moved = false;
                for (Direction outputSide : Direction.values()) {
                    if (!output.getSideMode(outputSide).allowsItemOutput()) continue;
                    BlockPos targetPos = output.getPos().offset(outputSide);
                    // Do not move within the same chest or insert into configuration slots.
                    if (!loaded(targetWorld, targetPos)
                            || (targetWorld == world && sameChest(world, chestPos, targetPos))) continue;
                    var targetBlock = targetWorld.getBlockEntity(targetPos);
                    if (targetBlock instanceof AnchoredTeleportBlockEntity) continue;
                    Direction face = outputSide.getOpposite();
                    if (targetBlock instanceof PEBlockEntity machine
                            && (!machine.canWork() || !machine.getSideMode(face).allowsItemInput())) continue;
                    Storage<ItemVariant> target = ItemStorage.SIDED.find(targetWorld, targetPos, face);
                    if (target == null || !target.supportsInsertion() || target == source) continue;
                    if (move(source, target) > 0) { moved = true; break; }
                }
                // One stack per input face per tick; try other outputs when this one cannot accept it.
                if (moved) break;
            }
        }
    }

    static <T> long move(Storage<T> source, Storage<T> target) {
        return StorageUtil.move(source, target, variant -> true, 64, null);
    }

    private static boolean loaded(ServerWorld world, BlockPos pos) {
        return world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static boolean sameChest(ServerWorld world, BlockPos source, BlockPos target) {
        if (source.equals(target)) return true;
        var state = world.getBlockState(source);
        ChestType type = state.get(ChestBlock.CHEST_TYPE);
        if (type == ChestType.SINGLE) return false;
        Direction facing = state.get(ChestBlock.FACING);
        Direction otherHalf = type == ChestType.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise();
        return source.offset(otherHalf).equals(target);
    }
}

package example.phaseteleporters;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class PortalPlaneBlockEntity extends BlockEntity {
    private BlockPos controller;

    public PortalPlaneBlockEntity(BlockPos pos, BlockState state) {
        super(PhaseTeleportersMod.PORTAL_PLANE_BLOCK_ENTITY, pos, state);
    }

    public void setController(BlockPos controller) {
        this.controller = controller.toImmutable();
        markDirty();
    }

    public boolean belongsTo(BlockPos pos) { return pos.equals(controller); }

    public TeleportBlockEntity sourceTeleport(ServerWorld world) {
        return controller != null && world.getBlockEntity(controller) instanceof TeleportBlockEntity teleport
                ? teleport : null;
    }

    public TeleportBlockEntity findDestination(ServerWorld world) {
        TeleportBlockEntity teleport = sourceTeleport(world);
        if (teleport == null) return null;
        return teleport.findDestinationFor(pos);
    }

    public static void tick(World world, BlockPos pos, BlockState state, PortalPlaneBlockEntity plane) {
        if (world.getTime() % 20 != 0) return;
        if (plane.controller == null || !(world.getBlockEntity(plane.controller) instanceof TeleportBlockEntity teleport)
                || !teleport.isActiveAt(pos)) {
            world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        controller = nbt.contains("ControllerX")
                ? new BlockPos(nbt.getInt("ControllerX"), nbt.getInt("ControllerY"), nbt.getInt("ControllerZ"))
                : null;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        if (controller != null) {
            nbt.putInt("ControllerX", controller.getX());
            nbt.putInt("ControllerY", controller.getY());
            nbt.putInt("ControllerZ", controller.getZ());
        }
    }
}

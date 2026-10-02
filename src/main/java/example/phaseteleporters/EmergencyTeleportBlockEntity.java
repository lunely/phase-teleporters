package example.phaseteleporters;

import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** The pad supplies power; rescue preferences belong to each linked player. */
public final class EmergencyTeleportBlockEntity extends AnchoredTeleportBlockEntity implements NamedScreenHandlerFactory {
    public static final long CAPACITY = 1_000_000;
    public static final long TELEPORT_COST = 100_000;
    public static final double HEIGHT = 1.0 / 16.0;
    private UUID platformId = UUID.randomUUID();

    public EmergencyTeleportBlockEntity(BlockPos pos, BlockState state) {
        super(PhaseTeleportersMod.EMERGENCY_TELEPORT_BLOCK_ENTITY, pos, state, CAPACITY);
    }

    public UUID platformId() { return platformId; }
    public static void tick(World world, BlockPos pos, BlockState state, EmergencyTeleportBlockEntity pad) {
        pad.dischargeEnergyItem();
        if (world.getTime() % 20 == 0) pad.syncAnchor();
    }
    @Override protected long getMaxInputPerTick() { return 5_000; }
    @Override public Text getDisplayName() { return Text.translatable("block.phaseteleporters.emergency_teleporter"); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
        return new EmergencyTeleportScreenHandler(syncId, inventory, this);
    }

    @Override protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        if (nbt.containsUuid("EmergencyId")) platformId = nbt.getUuid("EmergencyId");
    }
    @Override protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putUuid("EmergencyId", platformId);
    }
}

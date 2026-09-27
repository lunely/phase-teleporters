package example.phaseteleports;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.state.property.Properties;
import net.minecraft.world.World;

public final class TeleportBlockEntity extends AnchoredTeleportBlockEntity implements NamedScreenHandlerFactory {
    private static final long ACTIVATION_ENERGY = 500;
    private String frequency = "";
    private boolean privateFrequency;
    private UUID frequencyOwner;
    private TeleportStructure.Bounds structureBounds;
    private TeleportStructure.Bounds activeBounds;
    private BlockPos linkedPos;
    private boolean poweredLastTick;

    public TeleportBlockEntity(BlockPos pos, BlockState state) {
        super(PhaseTeleportsMod.TELEPORT_BLOCK_ENTITY, pos, state);
    }

    public String getFrequency() { return frequency; }
    public boolean isPrivateFrequency() { return privateFrequency; }
    public UUID getFrequencyOwner() { return frequencyOwner; }
    public boolean matchesFrequency(String name, boolean privateType, UUID owner) {
        return frequency.equals(name) && privateFrequency == privateType
                && (!privateType || Objects.equals(frequencyOwner, owner));
    }

    public boolean isStructureValid() {
        return getStructureBounds().isPresent();
    }

    public Optional<TeleportStructure.Bounds> getStructureBounds() {
        if (world == null) return Optional.empty();
        TeleportStructure.Bounds found = TeleportStructure.find(world, pos).orElse(null);
        if (!Objects.equals(structureBounds, found)) {
            structureBounds = found;
            if (!world.isClient) markDirty();
        }
        return Optional.ofNullable(found);
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        if (world instanceof ServerWorld serverWorld) LocalTeleportIndex.get(serverWorld).add(pos);
    }

    public static void tick(World world, BlockPos pos, BlockState state, TeleportBlockEntity teleport) {
        if (world.getTime() % 20 == 0) teleport.syncAnchor();
        teleport.sendConfiguredOutput();
        boolean powered = teleport.getStored() >= ACTIVATION_ENERGY;
        if (powered != teleport.poweredLastTick || world.getTime() % 20 == 0
                || teleport.activeBounds != null && !teleport.linkedPartnerPowered())
            teleport.refreshPortal();
        teleport.poweredLastTick = powered;
    }

    private boolean linkedPartnerPowered() {
        return world instanceof ServerWorld serverWorld && linkedPos != null
                && serverWorld.getBlockEntity(linkedPos) instanceof TeleportBlockEntity candidate
                && candidate.getStored() >= ACTIVATION_ENERGY;
    }

    public void refreshPortal() {
        if (!(world instanceof ServerWorld serverWorld)) return;
        TeleportStructure.Bounds found = getStructureBounds().orElse(null);
        TeleportBlockEntity destination = found == null || frequency.isEmpty()
                || getStored() < ACTIVATION_ENERGY ? null : findPartner(serverWorld);
        if (destination == null) {
            clearPortal();
            return;
        }
        if (activeBounds != null && !activeBounds.equals(found)) clearPortal();
        activeBounds = found;
        linkedPos = destination.getPos().toImmutable();
        fillPortal(serverWorld, found);
    }

    public boolean isActiveAt(BlockPos planePos) {
        return getStored() >= ACTIVATION_ENERGY && activeBounds != null && linkedPos != null
                && activeBounds.containsInterior(planePos);
    }

    public TeleportBlockEntity findDestinationFor(BlockPos planePos) {
        if (!(world instanceof ServerWorld serverWorld) || !isActiveAt(planePos)
                || getStructureBounds().filter(bounds -> bounds.containsInterior(planePos)).isEmpty()) return null;
        TeleportBlockEntity destination = findPartner(serverWorld);
        return destination != null && destination.getPos().equals(linkedPos) ? destination : null;
    }

    private TeleportBlockEntity findPartner(ServerWorld serverWorld) {
        if (frequency.isEmpty() || world != serverWorld
                || !world.getRegistryKey().equals(serverWorld.getRegistryKey())) return null;
        TeleportBlockEntity best = null;
        long bestDistance = Long.MAX_VALUE;
        for (long packed : LocalTeleportIndex.get(serverWorld).positions()) {
            BlockPos candidatePos = BlockPos.fromLong(packed);
            if (candidatePos.equals(pos)) continue;
            serverWorld.getChunk(candidatePos.getX() >> 4, candidatePos.getZ() >> 4);
            if (!(serverWorld.getBlockEntity(candidatePos) instanceof TeleportBlockEntity candidate)) {
                LocalTeleportIndex.get(serverWorld).remove(candidatePos);
                continue;
            }
            if (candidate.getWorld() == null
                    || !candidate.getWorld().getRegistryKey().equals(serverWorld.getRegistryKey())
                    || !candidate.matchesFrequency(frequency, privateFrequency, frequencyOwner)
                    || candidate.getStructureBounds().isEmpty()
                    || candidate.getStored() < ACTIVATION_ENERGY) continue;
            long dx = (long) candidatePos.getX() - pos.getX();
            long dy = (long) candidatePos.getY() - pos.getY();
            long dz = (long) candidatePos.getZ() - pos.getZ();
            long distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void fillPortal(ServerWorld serverWorld, TeleportStructure.Bounds bounds) {
        int color = LocalFrequencyState.get(serverWorld).color(frequency, privateFrequency, frequencyOwner);
        BlockPos low = bounds.interiorMin();
        BlockPos high = bounds.interiorMax();
        for (int y = low.getY(); y <= high.getY(); y++) {
            for (int x = low.getX(); x <= high.getX(); x++) {
                for (int z = low.getZ(); z <= high.getZ(); z++) {
                    BlockPos planePos = new BlockPos(x, y, z);
                    BlockState current = serverWorld.getBlockState(planePos);
                    boolean ownPlane = current.isOf(PhaseTeleportsMod.PORTAL_PLANE)
                            && serverWorld.getBlockEntity(planePos) instanceof PortalPlaneBlockEntity plane
                            && plane.belongsTo(pos);
                    if (current.isAir() || (ownPlane && (current.get(Properties.HORIZONTAL_AXIS) != bounds.axis()
                            || current.get(PortalPlaneBlock.COLOR) != color))) {
                        serverWorld.setBlockState(planePos, PhaseTeleportsMod.PORTAL_PLANE.getDefaultState()
                                .with(Properties.HORIZONTAL_AXIS, bounds.axis()).with(PortalPlaneBlock.COLOR, color), 3);
                        if (serverWorld.getBlockEntity(planePos) instanceof PortalPlaneBlockEntity plane) {
                            plane.setController(pos);
                        }
                    }
                }
            }
        }
    }

    public void clearPortal() {
        if (world instanceof ServerWorld serverWorld && activeBounds != null) {
            BlockPos low = activeBounds.interiorMin();
            BlockPos high = activeBounds.interiorMax();
            for (int y = low.getY(); y <= high.getY(); y++) {
                for (int x = low.getX(); x <= high.getX(); x++) {
                    for (int z = low.getZ(); z <= high.getZ(); z++) {
                        BlockPos planePos = new BlockPos(x, y, z);
                        if (serverWorld.getBlockState(planePos).isOf(PhaseTeleportsMod.PORTAL_PLANE)
                                && serverWorld.getBlockEntity(planePos) instanceof PortalPlaneBlockEntity plane
                                && plane.belongsTo(pos)) {
                            serverWorld.setBlockState(planePos, Blocks.AIR.getDefaultState(), 3);
                        }
                    }
                }
            }
        }
        activeBounds = null;
        linkedPos = null;
    }

    public void setFrequency(String frequency, boolean privateFrequency, UUID owner) {
        String oldFrequency = this.frequency;
        boolean oldPrivateFrequency = this.privateFrequency;
        UUID oldOwner = this.frequencyOwner;
        this.frequency = frequency;
        this.privateFrequency = privateFrequency;
        this.frequencyOwner = privateFrequency ? owner : null;
        markDirty();
        refreshPortal();
        if (world instanceof ServerWorld serverWorld
                && !matchesFrequency(oldFrequency, oldPrivateFrequency, oldOwner))
            UnusedFrequencyCleanup.local(serverWorld, oldFrequency, oldPrivateFrequency, oldOwner);
    }

    public void removeFrequency() {
        String oldFrequency = frequency;
        boolean oldPrivateFrequency = privateFrequency;
        UUID oldOwner = frequencyOwner;
        clearPortal();
        frequency = "";
        privateFrequency = false;
        frequencyOwner = null;
        markDirty();
        if (world instanceof ServerWorld serverWorld)
            UnusedFrequencyCleanup.local(serverWorld, oldFrequency, oldPrivateFrequency, oldOwner);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        frequency = LocalFrequencyState.normalize(nbt.getString("LocalFrequency"));
        privateFrequency = nbt.getBoolean("LocalFrequencyPrivate");
        frequencyOwner = null;
        if (privateFrequency) {
            try { frequencyOwner = UUID.fromString(nbt.getString("LocalFrequencyOwner")); }
            catch (IllegalArgumentException ignored) { frequency = ""; privateFrequency = false; }
        }
        structureBounds = null;
        activeBounds = null;
        linkedPos = null;
        int width = nbt.getInt("PortalWidth");
        int height = nbt.getInt("PortalHeight");
        int leftSpan = nbt.getInt("PortalLeftSpan");
        int rightSpan = nbt.getInt("PortalRightSpan");
        String axisName = nbt.getString("PortalAxis");
        if (width >= 3 && width == leftSpan + rightSpan + 1
                && leftSpan >= 1 && leftSpan == rightSpan && height >= 4
                && (axisName.equals("x") || axisName.equals("z"))) {
            structureBounds = TeleportStructure.bounds(pos,
                    axisName.equals("x") ? Direction.Axis.X : Direction.Axis.Z,
                    leftSpan, rightSpan, height);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putString("LocalFrequency", frequency);
        nbt.putBoolean("LocalFrequencyPrivate", privateFrequency);
        if (privateFrequency && frequencyOwner != null) nbt.putString("LocalFrequencyOwner", frequencyOwner.toString());
        if (structureBounds != null) {
            nbt.putString("PortalAxis", structureBounds.axis() == Direction.Axis.X ? "x" : "z");
            nbt.putInt("PortalWidth", structureBounds.width());
            nbt.putInt("PortalHeight", structureBounds.height());
            nbt.putInt("PortalLeftSpan", structureBounds.axis() == Direction.Axis.X
                    ? pos.getX() - structureBounds.min().getX() : pos.getZ() - structureBounds.min().getZ());
            nbt.putInt("PortalRightSpan", structureBounds.axis() == Direction.Axis.X
                    ? structureBounds.max().getX() - pos.getX() : structureBounds.max().getZ() - pos.getZ());
        }
    }

    @Override public Text getDisplayName() { return Text.translatable("block.phaseteleports.teleport"); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
        return new TeleportScreenHandler(syncId, inventory, this);
    }
}

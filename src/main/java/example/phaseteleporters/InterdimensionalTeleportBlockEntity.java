package example.phaseteleporters;

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
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.state.property.Properties;
import net.minecraft.world.World;

public final class InterdimensionalTeleportBlockEntity extends AnchoredTeleportBlockEntity implements NamedScreenHandlerFactory {
    private static final long ACTIVATION_ENERGY = 500;
    private String frequency = "";
    private boolean privateFrequency;
    private UUID frequencyOwner;
    private InterdimensionalTeleportStructure.Bounds structureBounds;
    private InterdimensionalTeleportStructure.Bounds activeBounds;
    private BlockPos linkedPos;
    private RegistryKey<World> linkedDimension;
    private boolean poweredLastTick;

    public InterdimensionalTeleportBlockEntity(BlockPos pos, BlockState state) {
        super(PhaseTeleportersMod.INTERDIMENSIONAL_TELEPORT_BLOCK_ENTITY, pos, state, 2_000_000);
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

    public int getPortalStatus() {
        return PortalStatus.of(isStructureValid(), getStored(), canWork(),
                !frequency.isEmpty(), activeBounds != null && linkedPos != null && linkedDimension != null);
    }

    public Optional<InterdimensionalTeleportStructure.Bounds> getStructureBounds() {
        if (world == null) return Optional.empty();
        InterdimensionalTeleportStructure.Bounds found = InterdimensionalTeleportStructure.find(world, pos).orElse(null);
        if (!Objects.equals(structureBounds, found)) {
            structureBounds = found;
            if (!world.isClient) markDirty();
        }
        return Optional.ofNullable(found);
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        if (world instanceof ServerWorld serverWorld) InterdimensionalTeleportIndex.get(serverWorld).add(pos);
    }

    public static void tick(World world, BlockPos pos, BlockState state, InterdimensionalTeleportBlockEntity teleport) {
        if (world.getTime() % 20 == 0) teleport.syncAnchor();
        teleport.dischargeEnergyItem();
        teleport.sendConfiguredOutput();
        boolean powered = teleport.canWork() && teleport.getStored() >= ACTIVATION_ENERGY;
        if (powered != teleport.poweredLastTick || world.getTime() % 20 == 0
                || teleport.activeBounds != null && !teleport.linkedPartnerPowered())
            teleport.refreshPortal();
        teleport.poweredLastTick = powered;
    }

    private boolean linkedPartnerPowered() {
        if (!(world instanceof ServerWorld serverWorld) || linkedPos == null || linkedDimension == null) return false;
        ServerWorld destinationWorld = serverWorld.getServer().getWorld(linkedDimension);
        return destinationWorld != null
                && destinationWorld.getBlockEntity(linkedPos) instanceof InterdimensionalTeleportBlockEntity candidate
                && candidate.canWork() && candidate.getStored() >= ACTIVATION_ENERGY;
    }

    public void refreshPortal() {
        if (!(world instanceof ServerWorld serverWorld)) return;
        InterdimensionalTeleportStructure.Bounds found = getStructureBounds().orElse(null);
        InterdimensionalTeleportBlockEntity destination = found == null || frequency.isEmpty()
                || !canWork() || getStored() < ACTIVATION_ENERGY ? null : findPartner(serverWorld);
        if (destination == null) {
            clearPortal();
            return;
        }
        if (activeBounds != null && !activeBounds.equals(found)) clearPortal();
        activeBounds = found;
        linkedPos = destination.getPos().toImmutable();
        linkedDimension = destination.getWorld().getRegistryKey();
        fillPortal(serverWorld, found);
    }

    public boolean isActiveAt(BlockPos planePos) {
        return canWork() && getStored() >= ACTIVATION_ENERGY && activeBounds != null && linkedPos != null && linkedDimension != null
                && activeBounds.containsInterior(planePos);
    }

    public InterdimensionalTeleportBlockEntity findDestinationFor(BlockPos planePos) {
        if (!(world instanceof ServerWorld serverWorld) || !isActiveAt(planePos)
                || getStructureBounds().filter(bounds -> bounds.containsInterior(planePos)).isEmpty()) return null;
        InterdimensionalTeleportBlockEntity destination = findPartner(serverWorld);
        return destination != null && destination.getPos().equals(linkedPos)
                && destination.getWorld().getRegistryKey().equals(linkedDimension) ? destination : null;
    }

    private InterdimensionalTeleportBlockEntity findPartner(ServerWorld serverWorld) {
        if (frequency.isEmpty()) return null;
        InterdimensionalTeleportBlockEntity best = null;
        long bestDistance = Long.MAX_VALUE;
        for (ServerWorld candidateWorld : serverWorld.getServer().getWorlds()) {
            for (long packed : InterdimensionalTeleportIndex.get(candidateWorld).positions()) {
                BlockPos candidatePos = BlockPos.fromLong(packed);
                if (candidateWorld == serverWorld && candidatePos.equals(pos)) continue;
                candidateWorld.getChunk(candidatePos.getX() >> 4, candidatePos.getZ() >> 4);
                if (!(candidateWorld.getBlockEntity(candidatePos) instanceof InterdimensionalTeleportBlockEntity candidate)) {
                    InterdimensionalTeleportIndex.get(candidateWorld).remove(candidatePos);
                    continue;
                }
                if (!candidate.matchesFrequency(frequency, privateFrequency, frequencyOwner)
                        || candidate.getStructureBounds().isEmpty()
                        || !candidate.canWork() || candidate.getStored() < ACTIVATION_ENERGY) continue;
                long dx = (long) candidatePos.getX() - pos.getX();
                long dy = (long) candidatePos.getY() - pos.getY();
                long dz = (long) candidatePos.getZ() - pos.getZ();
                long distance = dx * dx + dy * dy + dz * dz;
                if (best == null || (candidateWorld != serverWorld && best.getWorld() == serverWorld)
                        || ((candidateWorld == serverWorld) == (best.getWorld() == serverWorld)
                        && distance < bestDistance)) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private void fillPortal(ServerWorld serverWorld, InterdimensionalTeleportStructure.Bounds bounds) {
        int color = InterdimensionalFrequencyState.get(serverWorld).color(frequency, privateFrequency, frequencyOwner);
        BlockPos low = bounds.interiorMin();
        BlockPos high = bounds.interiorMax();
        for (int y = low.getY(); y <= high.getY(); y++) {
            for (int x = low.getX(); x <= high.getX(); x++) {
                for (int z = low.getZ(); z <= high.getZ(); z++) {
                    BlockPos planePos = new BlockPos(x, y, z);
                    BlockState current = serverWorld.getBlockState(planePos);
                    boolean ownPlane = current.isOf(PhaseTeleportersMod.INTERDIMENSIONAL_PORTAL_PLANE)
                            && serverWorld.getBlockEntity(planePos) instanceof InterdimensionalPortalPlaneBlockEntity plane
                            && plane.belongsTo(pos);
                    if (current.isAir() || (ownPlane && (current.get(Properties.HORIZONTAL_AXIS) != bounds.axis()
                            || current.get(InterdimensionalPortalPlaneBlock.COLOR) != color))) {
                        serverWorld.setBlockState(planePos, PhaseTeleportersMod.INTERDIMENSIONAL_PORTAL_PLANE.getDefaultState()
                                .with(Properties.HORIZONTAL_AXIS, bounds.axis()).with(InterdimensionalPortalPlaneBlock.COLOR, color), 3);
                        if (serverWorld.getBlockEntity(planePos) instanceof InterdimensionalPortalPlaneBlockEntity plane) {
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
                        if (serverWorld.getBlockState(planePos).isOf(PhaseTeleportersMod.INTERDIMENSIONAL_PORTAL_PLANE)
                                && serverWorld.getBlockEntity(planePos) instanceof InterdimensionalPortalPlaneBlockEntity plane
                                && plane.belongsTo(pos)) {
                            serverWorld.setBlockState(planePos, Blocks.AIR.getDefaultState(), 3);
                        }
                    }
                }
            }
        }
        activeBounds = null;
        linkedPos = null;
        linkedDimension = null;
    }

    public void setFrequency(String frequency, boolean privateFrequency, UUID owner) {
        this.frequency = frequency;
        this.privateFrequency = privateFrequency;
        this.frequencyOwner = privateFrequency ? owner : null;
        markDirty();
        refreshPortal();
    }

    public void removeFrequency() {
        clearPortal();
        frequency = "";
        privateFrequency = false;
        frequencyOwner = null;
        markDirty();
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        frequency = InterdimensionalFrequencyState.normalize(nbt.getString("InterdimensionalFrequency"));
        privateFrequency = nbt.getBoolean("InterdimensionalFrequencyPrivate");
        frequencyOwner = null;
        if (privateFrequency) {
            try { frequencyOwner = UUID.fromString(nbt.getString("InterdimensionalFrequencyOwner")); }
            catch (IllegalArgumentException ignored) { frequency = ""; privateFrequency = false; }
        }
        structureBounds = null;
        activeBounds = null;
        linkedPos = null;
        linkedDimension = null;
        int width = nbt.getInt("PortalWidth");
        int height = nbt.getInt("PortalHeight");
        int leftSpan = nbt.getInt("PortalLeftSpan");
        int rightSpan = nbt.getInt("PortalRightSpan");
        String axisName = nbt.getString("PortalAxis");
        if (width >= 3 && width == leftSpan + rightSpan + 1
                && leftSpan >= 1 && leftSpan == rightSpan && height >= 4
                && (axisName.equals("x") || axisName.equals("z"))) {
            structureBounds = InterdimensionalTeleportStructure.bounds(pos,
                    axisName.equals("x") ? Direction.Axis.X : Direction.Axis.Z,
                    leftSpan, rightSpan, height);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putString("InterdimensionalFrequency", frequency);
        nbt.putBoolean("InterdimensionalFrequencyPrivate", privateFrequency);
        if (privateFrequency && frequencyOwner != null) nbt.putString("InterdimensionalFrequencyOwner", frequencyOwner.toString());
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

    @Override public Text getDisplayName() { return Text.translatable("block.phaseteleporters.interdimensional_teleporter"); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
        return new InterdimensionalTeleportScreenHandler(syncId, inventory, this);
    }
}

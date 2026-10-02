package example.phaseteleporters;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import example.phaseteleporters.energy.PEChargeableItem;
import example.phaseteleporters.energy.PEPropertyCodec;
import example.phaseteleporters.energy.PESideMode;
import example.phaseteleporters.energy.PERedstoneMode;
import net.minecraft.util.math.Direction;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

public class EmergencyTeleportScreenHandler extends ScreenHandler implements EnergySideScreenHandler {
    public static final int LESS = 0, MORE = 1, ENABLED = 2, TOTEM = 3, WATER = 4, BIND = 5, FALLS = 7;
    private final EmergencyTeleportBlockEntity pad;
    private final PropertyDelegate properties;
    private final PropertyDelegate energyProperties, sideProperties;
    private final Inventory padInventory;
    private final boolean remote;
    private final Hand usedHand;

    public EmergencyTeleportScreenHandler(int syncId, PlayerInventory inventory) { this(syncId, inventory, null); }
    public EmergencyTeleportScreenHandler(int syncId, PlayerInventory inventory, EmergencyTeleportBlockEntity pad) {
        this(PhaseTeleportersMod.EMERGENCY_TELEPORT_SCREEN_HANDLER, syncId, inventory, pad, false, null);
    }
    protected EmergencyTeleportScreenHandler(ScreenHandlerType<?> type, int syncId, PlayerInventory inventory,
            EmergencyTeleportBlockEntity pad, boolean remote, Hand usedHand) {
        super(type, syncId);
        this.pad = pad;
        this.remote = remote;
        this.usedHand = usedHand;
        properties = !(inventory.player instanceof ServerPlayerEntity) ? new ArrayPropertyDelegate(6) : new PropertyDelegate() {
            @Override public int get(int index) {
                var profile = EmergencyTeleportState.get(inventory.player.getServer()).profile(inventory.player.getUuid());
                return switch (index) {
                    case 0 -> profile.threshold;
                    case 1 -> profile.enabled ? 1 : 0;
                    case 2 -> profile.skipTotem ? 1 : 0;
                    case 3 -> profile.skipWaterBucket ? 1 : 0;
                    case 4 -> (remote ? profile.isBound() : pad != null && profile.matches(pad)) ? 1 : 0;
                    case 5 -> profile.rescueFalls ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int size() { return 6; }
        };
        if (!(inventory.player instanceof ServerPlayerEntity)) { properties.set(0, 4); properties.set(1, 1); properties.set(2, 1); properties.set(5, 1); }
        padInventory = pad == null ? new SimpleInventory(2) {
            @Override public boolean isValid(int slot, ItemStack stack) {
                return slot == 0 && stack.isOf(PhaseTeleportersMod.ANCHOR_UPGRADE)
                        || slot == 1 && stack.getItem() instanceof PEChargeableItem;
            }
        } : pad;
        if (!remote) {
            padInventory.onOpen(inventory.player);
            addSlot(new Slot(padInventory, 0, 7, 130) {
                @Override public boolean canInsert(ItemStack stack) { return padInventory.isValid(0, stack); }
                @Override public int getMaxItemCount() { return 1; }
            });
            addSlot(new Slot(padInventory, 1, 151, 77) {
                @Override public boolean canInsert(ItemStack stack) { return padInventory.isValid(1, stack); }
                @Override public int getMaxItemCount() { return 1; }
            });
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, 9 + row * 9 + col, 7 + col * 18, 153 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 7 + col * 18, 211));
            }
        addProperties(properties);
        energyProperties = pad == null ? new ArrayPropertyDelegate(PEPropertyCodec.PROPERTY_COUNT) : new PropertyDelegate() {
            @Override public int get(int index) { return PEPropertyCodec.part(pad, index); }
            @Override public void set(int index, int value) {}
            @Override public int size() { return PEPropertyCodec.PROPERTY_COUNT; }
        };
        sideProperties = EnergySideConfiguration.properties(pad, inventory.player);
        if (!remote) { addProperties(energyProperties); addProperties(sideProperties); }
    }
    public boolean remote() { return remote; }
    public int threshold() { return properties.get(0); }
    public boolean enabled() { return properties.get(1) != 0; }
    public boolean skipTotem() { return properties.get(2) != 0; }
    public boolean skipWaterBucket() { return properties.get(3) != 0; }
    public boolean bound() { return properties.get(4) != 0; }
    public boolean rescueFalls() { return properties.get(5) != 0; }
    public long energy() { return PEPropertyCodec.stored(energyProperties, 0); }
    public long capacity() { return PEPropertyCodec.capacity(energyProperties, 0); }
    @Override public PESideMode getSideMode(Direction side) { return EnergySideConfiguration.mode(sideProperties, side); }
    @Override public Direction getSideFacing() { return EnergySideConfiguration.facing(sideProperties); }
    @Override public PERedstoneMode getRedstoneMode() { return EnergySideConfiguration.redstoneMode(sideProperties); }
    @Override public boolean hasRedstoneSignal() { return EnergySideConfiguration.hasRedstoneSignal(sideProperties); }
    @Override public boolean isPublicAccess() { return EnergySideConfiguration.isPublicAccess(sideProperties); }
    @Override public boolean isSecurityOwner() { return EnergySideConfiguration.isSecurityOwner(sideProperties); }

    @Override public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(player instanceof ServerPlayerEntity serverPlayer) || !canUse(player) || !remote && pad == null) return false;
        if (id >= EnergySideConfiguration.FIRST_BUTTON && id < EnergySideConfiguration.LAST_BUTTON_EXCLUSIVE)
            return !remote && EnergySideConfiguration.click(player, id, pad);
        var saved = EmergencyTeleportState.get(serverPlayer.getServer());
        var profile = saved.profile(player.getUuid());
        switch (id) {
            case LESS -> profile.threshold = Math.max(1, profile.threshold - 1);
            case MORE -> profile.threshold = Math.min(20, profile.threshold + 1);
            case ENABLED -> profile.enabled = !profile.enabled;
            case TOTEM -> profile.skipTotem = !profile.skipTotem;
            case WATER -> profile.skipWaterBucket = !profile.skipWaterBucket;
            case FALLS -> profile.rescueFalls = !profile.rescueFalls;
            case BIND -> {
                if (remote) profile.unbind();
                else if (profile.matches(pad)) profile.unbind();
                else if (!EmergencyTeleportRescue.bind(serverPlayer, pad)) return false;
            }
            default -> { return false; }
        }
        saved.markDirty();
        sendContentUpdates();
        return true;
    }
    @Override public boolean canUse(PlayerEntity player) {
        if (remote) return usedHand == null ? hasHeldPortable(player)
                : player.getStackInHand(usedHand).isOf(PhaseTeleportersMod.PORTABLE_TELEPORT);
        return pad == null || player.getWorld().getBlockEntity(pad.getPos()) == pad
                && player.squaredDistanceTo(pad.getPos().toCenterPos()) <= 64 && pad.canPlayerUse(player);
    }
    private static boolean hasHeldPortable(PlayerEntity player) {
        return player.getMainHandStack().isOf(PhaseTeleportersMod.PORTABLE_TELEPORT)
                || player.getOffHandStack().isOf(PhaseTeleportersMod.PORTABLE_TELEPORT);
    }
    @Override public ItemStack quickMove(PlayerEntity player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack(), original = stack.copy();
        if (index < 2) {
            if (!insertItem(stack, 2, 38, true)) return ItemStack.EMPTY;
        } else if (stack.isOf(PhaseTeleportersMod.ANCHOR_UPGRADE) && insertItem(stack, 0, 1, false)) {
            // Installed upgrade.
        } else if (stack.getItem() instanceof PEChargeableItem && insertItem(stack, 1, 2, false)) {
            // Installed battery.
        } else if (!insertItem(stack, index < 29 ? 29 : 2, index < 29 ? 38 : 29, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY); else slot.markDirty();
        return original;
    }
    @Override public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (!remote) padInventory.onClose(player);
    }
}

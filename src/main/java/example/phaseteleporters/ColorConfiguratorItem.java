package example.phaseteleporters;

import example.phaseteleporters.energy.PEEnergyFormat;
import example.phaseteleporters.energy.StoredPEItem;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Formatting;
import java.util.List;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

/** Cycles a frame or controller palette using the item's stored energy. */
public final class ColorConfiguratorItem extends StoredPEItem {
    public static final long CAPACITY = 200_000;
    public static final long ENERGY_PER_CHANGE = 100;
    public enum Mode {
        FRAME, LIGHTS, TEXT;
        public Mode step(int direction) {
            return values()[Math.floorMod(ordinal() + direction, values().length)];
        }
    }

    public static Mode mode(ItemStack stack) {
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        int value = data == null ? 0 : data.copyNbt().getInt("ColorMode");
        return value >= 0 && value < Mode.values().length ? Mode.values()[value] : Mode.FRAME;
    }
    public static void setMode(ItemStack stack, Mode mode) {
        var current = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound data = current == null ? new NbtCompound() : current.copyNbt();
        data.putInt("ColorMode", mode.ordinal());
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, data);
    }
    private static String modeKey(Mode mode) {
        return "tooltip.phaseteleporters.color_configurator.mode." + mode.name().toLowerCase(java.util.Locale.ROOT);
    }
    public static Text modeText(Mode mode) {
        return Text.translatable("tooltip.phaseteleporters.color_configurator.mode",
                Text.translatable(modeKey(mode)));
    }
    @Override public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(modeText(mode(stack)).copy().formatted(Formatting.GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }

    public ColorConfiguratorItem(Settings settings) {
        super(settings, CAPACITY);
    }

    @Override public ActionResult useOnBlock(ItemUsageContext context) {
        var world = context.getWorld();
        var pos = context.getBlockPos();
        var state = world.getBlockState(pos);
        boolean monitorTarget = state.getBlock() instanceof TeleportationFrameMonitorBlock;
        boolean teleportTarget = state.getBlock() instanceof TeleportBlock
                || state.getBlock() instanceof InterdimensionalTeleportBlock
                || state.getBlock() instanceof EmergencyTeleportBlock;
        if (!(state.getBlock() instanceof TeleportationFrameBlock)
                && !teleportTarget && !monitorTarget) return ActionResult.PASS;
        Mode mode = mode(context.getStack());
        if (mode != Mode.FRAME && !monitorTarget) return ActionResult.FAIL;
        var monitor = world.getBlockEntity(pos) instanceof TeleportationFrameMonitorBlockEntity value ? value : null;
        if (mode == Mode.TEXT && monitor == null) return ActionResult.FAIL;
        PlayerEntity player = context.getPlayer();
        if (teleportTarget && (player == null || !player.isSneaking())) return ActionResult.PASS;
        if (player != null && !world.canPlayerModifyAt(player, pos)) return ActionResult.FAIL;
        if (player != null && world.getBlockEntity(pos) instanceof TeleportBlockEntity teleport
                && !PESecurity.canOpen(player, teleport)) return ActionResult.FAIL;
        if (player != null && world.getBlockEntity(pos) instanceof InterdimensionalTeleportBlockEntity teleport
                && !PESecurity.canOpen(player, teleport)) return ActionResult.FAIL;
        if (player != null && world.getBlockEntity(pos) instanceof EmergencyTeleportBlockEntity teleport
                && !PESecurity.canOpen(player, teleport)) return ActionResult.FAIL;
        var stack = context.getStack();
        if (getStoredPE(stack) < ENERGY_PER_CHANGE) {
            if (!world.isClient && player != null) {
                player.sendMessage(Text.translatable("message.phaseteleporters.color_configurator.no_energy",
                        PEEnergyFormat.format(ENERGY_PER_CHANGE)), true);
            }
            return ActionResult.FAIL;
        }
        if (world.isClient) return ActionResult.SUCCESS;

        int next = switch (mode) {
            case FRAME -> PortalColors.nextDisplayColor(state.get(TeleportationFrameBlock.COLOR));
            case LIGHTS -> TeleportationFrameMonitorBlock.nextMonitorColor(state.get(TeleportationFrameMonitorBlock.MONITOR_COLOR));
            case TEXT -> PortalColors.nextDisplayColor(monitor.textColor());
        };
        if (!spendPE(stack, ENERGY_PER_CHANGE)) return ActionResult.FAIL;
        boolean changed;
        if (mode == Mode.TEXT) {
            monitor.setTextColor(next);
            changed = true;
        } else {
            var property = mode == Mode.LIGHTS ? TeleportationFrameMonitorBlock.MONITOR_COLOR : TeleportationFrameBlock.COLOR;
            changed = world.setBlockState(pos, state.with(property, next), Block.NOTIFY_ALL);
        }
        if (!changed) {
            insertPE(stack, ENERGY_PER_CHANGE, false);
            return ActionResult.FAIL;
        }
        if (player != null) {
            player.getInventory().markDirty();
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.playerScreenHandler.sendContentUpdates();
            }
            String message = mode == Mode.LIGHTS ? "message.phaseteleporters.color_configurator.lights_changed"
                    : mode == Mode.TEXT ? "message.phaseteleporters.color_configurator.text_changed"
                    : teleportTarget ? "message.phaseteleporters.color_configurator.teleporter_changed"
                    : "message.phaseteleporters.color_configurator.changed";
            Text selected = next == TeleportationFrameMonitorBlock.LIGHTS_OFF
                    ? Text.translatable("tooltip.phaseteleporters.color_configurator.off")
                    : Text.translatable(PortalColors.nameKey(next)).styled(style -> style.withColor(PortalColors.rgb(next)));
            player.sendMessage(Text.translatable(message, selected), true);
        }
        return ActionResult.CONSUME;
    }
}

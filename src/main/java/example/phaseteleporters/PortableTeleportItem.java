package example.phaseteleporters;

import example.phaseteleporters.energy.StoredPEItem;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class PortableTeleportItem extends StoredPEItem {
    public static final long CAPACITY = 400_000;
    private static final String FREQUENCY_KEY = "PortableFrequency";
    private static final String PRIVATE_KEY = "PortablePrivateFrequency";
    private static final String INTERDIMENSIONAL_KEY = "PortableInterdimensionalFrequency";
    private static final String VIEW_PRIVATE_KEY = "PortableViewPrivate";
    private static final String VIEW_INTERDIMENSIONAL_KEY = "PortableViewInterdimensional";
    private static final String QUICK_MODE_KEY = "PortableQuickMode";
    private static final String MODE_KEY = "PortableMode";

    public enum Mode {
        GUI("gui"), QUICK("quick"), EMERGENCY("emergency");
        private final String key;
        Mode(String key) { this.key = key; }
        public String messageKey() { return "message.phaseteleporters.portable.mode_" + key; }
        public Mode step(int direction) { return values()[Math.floorMod(ordinal() + direction, values().length)]; }
        public static Mode byId(int id) { return id >= 0 && id < values().length ? values()[id] : GUI; }
    }

    public record Selection(String name, boolean privateFrequency, boolean interdimensional) {}
    public record View(boolean privateFrequency, boolean interdimensional) {}

    public PortableTeleportItem(Settings settings) {
        super(settings, CAPACITY);
    }

    public boolean isQuickMode(ItemStack stack) {
        return getMode(stack) == Mode.QUICK;
    }

    public Mode getMode(ItemStack stack) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound data = component == null ? new NbtCompound() : component.copyNbt();
        return data.contains(MODE_KEY) ? Mode.byId(data.getInt(MODE_KEY))
                : data.getBoolean(QUICK_MODE_KEY) ? Mode.QUICK : Mode.GUI;
    }

    public void setQuickMode(ItemStack stack, boolean quick) {
        setMode(stack, quick ? Mode.QUICK : Mode.GUI);
    }
    public void setMode(ItemStack stack, Mode mode) {
        if (getMode(stack) == mode) return;
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound data = component == null ? new NbtCompound() : component.copyNbt();
        data.putInt(MODE_KEY, mode.ordinal());
        data.putBoolean(QUICK_MODE_KEY, mode == Mode.QUICK);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, data);
    }

    public void openGui(ServerPlayerEntity player, Hand hand) {
        var screen = new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, unused) -> new PortableTeleportScreenHandler(syncId, inventory, hand),
                Text.translatable("item.phaseteleporters.portable_teleporter"));
        player.openHandledScreen(screen);
    }

    private void openEmergencyGui(ServerPlayerEntity player, Hand hand) {
        if (EmergencyTeleportState.get(player.getServer()).boundProfile(player.getUuid()) == null) {
            player.sendMessage(Text.translatable("message.phaseteleporters.emergency.no_platform"), true);
            return;
        }
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, unused) -> new PortableEmergencyScreenHandler(syncId, inventory, hand),
                Text.translatable("gui.phaseteleporters.emergency.remote_title")));
    }

    public Selection getSelection(ItemStack stack) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound data = component == null ? new NbtCompound() : component.copyNbt();
        return new Selection(LocalFrequencyState.normalize(data.getString(FREQUENCY_KEY)),
                data.getBoolean(PRIVATE_KEY), data.getBoolean(INTERDIMENSIONAL_KEY));
    }

    public void setSelection(ItemStack stack, Selection selection) {
        if (getSelection(stack).equals(selection)) return;
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound data = component == null ? new NbtCompound() : component.copyNbt();
        data.putString(FREQUENCY_KEY, LocalFrequencyState.normalize(selection.name()));
        data.putBoolean(PRIVATE_KEY, selection.privateFrequency());
        data.putBoolean(INTERDIMENSIONAL_KEY, selection.interdimensional());
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, data);
    }

    public View getView(ItemStack stack) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound data = component == null ? new NbtCompound() : component.copyNbt();
        Selection selection = getSelection(stack);
        return new View(data.contains(VIEW_PRIVATE_KEY) ? data.getBoolean(VIEW_PRIVATE_KEY)
                        : selection.privateFrequency(),
                data.contains(VIEW_INTERDIMENSIONAL_KEY) ? data.getBoolean(VIEW_INTERDIMENSIONAL_KEY)
                        : selection.interdimensional());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable(getMode(stack).messageKey()).formatted(Formatting.GRAY));
        Selection selection = getSelection(stack);
        Text frequency = selection.name().isEmpty()
                ? Text.translatable("gui.phaseteleporters.no_frequency")
                : Text.literal(selection.name());
        tooltip.add(Text.translatable("tooltip.phaseteleporters.portable.frequency", frequency)
                .formatted(Formatting.GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            if (getMode(stack) == Mode.EMERGENCY) {
                openEmergencyGui(serverPlayer, hand);
                return TypedActionResult.success(stack, false);
            }
            if (isQuickMode(stack)) {
                String failure = PortableTeleportNetworking.quickTeleport(serverPlayer, stack, this);
                if (failure == null) return TypedActionResult.success(stack, false);
                setQuickMode(stack, false);
                serverPlayer.getInventory().markDirty();
                serverPlayer.playerScreenHandler.sendContentUpdates();
                serverPlayer.sendMessage(Text.translatable(failure), true);
            }
            openGui(serverPlayer, hand);
        }
        return TypedActionResult.success(stack, world.isClient());
    }
}

package example.phaseteleports;

import example.phaseteleports.energy.PEItemTransfer;
import example.phaseteleports.energy.StoredPEItem;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class BatteryItem extends StoredPEItem {
    public static final long CAPACITY = 20_000;

    public BatteryItem(Settings settings) {
        super(settings, CAPACITY);
    }

    private boolean isInventoryCharging(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null && data.copyNbt().getBoolean("InventoryCharging");
    }

    private void setInventoryCharging(ItemStack stack, boolean enabled) {
        NbtComponent current = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound data = current == null ? new NbtCompound() : current.copyNbt();
        data.putBoolean("InventoryCharging", enabled);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, data);
    }

    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!world.isClient) {
            boolean enabled = !isInventoryCharging(stack);
            setInventoryCharging(stack, enabled);
            player.getInventory().markDirty();
            player.sendMessage(Text.translatable(enabled
                    ? "message.phaseteleports.battery_mode_charging"
                    : "message.phaseteleports.battery_mode_idle"), true);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override public void inventoryTick(ItemStack stack, World world, Entity entity,
            int slot, boolean selected) {
        if (world.isClient || !(entity instanceof ServerPlayerEntity player)
                || !isInventoryCharging(stack) || getStoredPE(stack) <= 0) return;
        long moved = 0;
        for (int index = 0; index < player.getInventory().size() && getStoredPE(stack) > 0; index++) {
            moved += PEItemTransfer.transfer(stack, player.getInventory().getStack(index), getStoredPE(stack));
        }
        if (moved > 0) {
            player.getInventory().markDirty();
            player.playerScreenHandler.sendContentUpdates();
        }
    }

    @Override public void appendTooltip(ItemStack stack, TooltipContext context,
            List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("tooltip.phaseteleports.battery_mode",
                Text.translatable(isInventoryCharging(stack)
                        ? "tooltip.phaseteleports.battery_mode_charging"
                        : "tooltip.phaseteleports.battery_mode_idle")));
    }
}

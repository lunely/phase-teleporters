package example.phaseteleporters;

import example.phaseteleporters.energy.PEItemTransfer;
import example.phaseteleporters.energy.StoredPEItem;
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
import net.minecraft.util.Formatting;
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

    @Override public boolean hasGlint(ItemStack stack) {
        return isInventoryCharging(stack);
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
                    ? "message.phaseteleporters.battery_mode_charging"
                    : "message.phaseteleporters.battery_mode_idle"), true);
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
        tooltip.add(Text.translatable("tooltip.phaseteleporters.battery_mode",
                Text.translatable(isInventoryCharging(stack)
                        ? "tooltip.phaseteleporters.battery_mode_charging"
                        : "tooltip.phaseteleporters.battery_mode_idle")).formatted(Formatting.GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}

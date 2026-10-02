package example.phaseteleporters.energy;

import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** PE stored in the ItemStack, with the shared charge bar and tooltip. */
public class StoredPEItem extends Item implements PEChargeableItem {
    private final long capacity;

    public StoredPEItem(Settings settings, long capacity) {
        super(settings);
        this.capacity = capacity;
    }

    @Override public long getStoredPE(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data == null ? 0 : Math.clamp(data.copyNbt().getLong("PE"), 0L, capacity);
    }

    @Override public long getCapacityPE(ItemStack stack) { return capacity; }

    @Override public long insertPE(ItemStack stack, long amount, boolean simulate) {
        long accepted = Math.min(Math.max(0, amount), capacity - getStoredPE(stack));
        if (accepted > 0 && !simulate) setStoredPE(stack, getStoredPE(stack) + accepted);
        return accepted;
    }

    @Override public long extractPE(ItemStack stack, long amount, boolean simulate) {
        long extracted = Math.min(Math.max(0, amount), getStoredPE(stack));
        if (extracted > 0 && !simulate) setStoredPE(stack, getStoredPE(stack) - extracted);
        return extracted;
    }

    public boolean spendPE(ItemStack stack, long amount) {
        if (amount <= 0 || getStoredPE(stack) < amount) return false;
        return extractPE(stack, amount, false) == amount;
    }

    private void setStoredPE(ItemStack stack, long amount) {
        NbtComponent current = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound data = current == null ? new NbtCompound() : current.copyNbt();
        data.putLong("PE", Math.clamp(amount, 0L, capacity));
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, data);
    }

    @Override public boolean isItemBarVisible(ItemStack stack) { return true; }
    @Override public int getItemBarStep(ItemStack stack) {
        return itemBarStep(getStoredPE(stack), capacity);
    }
    @Override public int getItemBarColor(ItemStack stack) { return itemBarColor(); }

    @Override public void appendTooltip(ItemStack stack, TooltipContext context,
            List<Text> tooltip, TooltipType type) {
        tooltip.add(energyTooltip(getStoredPE(stack), capacity));
    }

    public static int itemBarStep(long stored, long capacity) {
        return capacity <= 0 ? 0 : Math.round(13.0f * stored / capacity);
    }

    public static int itemBarColor() { return 0x57C96B; }

    public static Text energyTooltip(long stored, long capacity) {
        return Text.translatable("tooltip.phaseteleporters.energy",
                Text.translatable("tooltip.phaseteleporters.energy_label").formatted(Formatting.GREEN),
                PEEnergyFormat.format(stored), PEEnergyFormat.format(capacity)).formatted(Formatting.GRAY);
    }
}

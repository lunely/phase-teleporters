package example.phaseteleporters;

import example.phaseteleporters.energy.StoredPEItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class PortableTeleportItem extends StoredPEItem {
    public static final long CAPACITY = 400_000;
    private static final String FREQUENCY_KEY = "PortableFrequency";
    private static final String PRIVATE_KEY = "PortablePrivateFrequency";
    private static final String INTERDIMENSIONAL_KEY = "PortableInterdimensionalFrequency";
    private static final String VIEW_PRIVATE_KEY = "PortableViewPrivate";
    private static final String VIEW_INTERDIMENSIONAL_KEY = "PortableViewInterdimensional";

    public record Selection(String name, boolean privateFrequency, boolean interdimensional) {}
    public record View(boolean privateFrequency, boolean interdimensional) {}

    public PortableTeleportItem(Settings settings) {
        super(settings, CAPACITY);
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
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            var screen = new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, unused) -> new PortableTeleportScreenHandler(syncId, inventory, hand),
                    Text.translatable("item.phaseteleporters.portable_teleporter"));
            serverPlayer.openHandledScreen(screen);
        }
        return TypedActionResult.success(stack, world.isClient());
    }
}

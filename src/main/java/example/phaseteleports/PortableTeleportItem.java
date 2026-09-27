package example.phaseteleports;

import example.phaseteleports.energy.StoredPEItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class PortableTeleportItem extends StoredPEItem {
    public static final long CAPACITY = 400_000;

    public PortableTeleportItem(Settings settings) {
        super(settings, CAPACITY);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            var screen = new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, unused) -> new PortableTeleportScreenHandler(syncId, inventory, hand),
                    Text.translatable("item.phaseteleports.portable_teleport"));
            if (serverPlayer.openHandledScreen(screen).isPresent()) {
                PortableTeleportNetworking.sendSnapshot(serverPlayer);
            }
        }
        return TypedActionResult.success(stack, world.isClient());
    }
}

package example.phaseteleporters;

import example.phaseteleporters.energy.PEBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Ownership is assigned once, when a player places a machine. */
public final class PESecurity {
    private PESecurity() {}

    public static void onPlaced(World world, BlockPos pos, LivingEntity placer) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof PEBlockEntity block)
            block.assignOwner(placer);
    }

    public static boolean canOpen(PlayerEntity player, PEBlockEntity block) {
        // Older machines and machines placed without a player are claimed on first use.
        block.claimUnowned(player);
        if (block.canPlayerUse(player)) return true;
        player.sendMessage(Text.translatable("message.phaseteleporters.security.denied"), true);
        return false;
    }

    public static boolean canUseInventory(PlayerEntity player, Inventory inventory) {
        return inventory.canPlayerUse(player)
                && (!(inventory instanceof PEBlockEntity block) || block.canPlayerUse(player));
    }
}

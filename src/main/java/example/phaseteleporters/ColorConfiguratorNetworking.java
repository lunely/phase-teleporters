package example.phaseteleporters;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ColorConfiguratorNetworking {
    private ColorConfiguratorNetworking() {}

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(ColorConfiguratorModePayload.ID, ColorConfiguratorModePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ColorConfiguratorModePayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    var player = context.player();
                    if (player.currentScreenHandler != player.playerScreenHandler
                            || player.getInventory().selectedSlot != payload.slot()
                            || payload.mode() < 0 || payload.mode() >= ColorConfiguratorItem.Mode.values().length) return;
                    // Match the portable teleporter: vanilla sneak input is checked by the
                    // client before sending, since its movement packet can arrive later.
                    var stack = player.getMainHandStack();
                    if (!(stack.getItem() instanceof ColorConfiguratorItem)) return;
                    var mode = ColorConfiguratorItem.Mode.values()[payload.mode()];
                    ColorConfiguratorItem.setMode(stack, mode);
                    player.getInventory().markDirty();
                    player.playerScreenHandler.sendContentUpdates();
                    player.sendMessage(ColorConfiguratorItem.modeText(mode), true);
                }));
    }
}

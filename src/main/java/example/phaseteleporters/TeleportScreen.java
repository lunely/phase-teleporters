package example.phaseteleporters;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class TeleportScreen extends FramedTeleportScreenBase<TeleportScreenHandler> {
    public TeleportScreen(TeleportScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected Text guiTitle() {
        return Text.translatable("gui.phaseteleporters.local_title");
    }

    @Override
    protected int getPortalStatus() {
        return handler.getPortalStatus();
    }

    @Override protected long getEnergy() { return handler.getEnergy(); }
    @Override protected long getMaxEnergy() { return handler.getMaxEnergy(); }

    public void applySnapshot(TeleportFrequencySnapshotPayload snapshot) {
        if (snapshot.syncId() == handler.syncId)
            applyFrequencySnapshot(snapshot.assigned(), snapshot.assignedPrivate(), snapshot.currentColor(),
                    snapshot.publicNames(), snapshot.publicColors(), snapshot.publicCreators(),
                    snapshot.privateNames(), snapshot.privateColors(), snapshot.privateCreators());
    }

    @Override
    protected void sendColor(String name, boolean isPrivate, int color) {
        ClientPlayNetworking.send(new TeleportFrequencyActionPayload(
                handler.syncId, TeleportFrequencyActionPayload.COLOR, name, color, isPrivate));
    }

    @Override
    protected void sendFrequencyAction(int action, String name, int color, boolean isPrivate) {
        ClientPlayNetworking.send(new TeleportFrequencyActionPayload(
                handler.syncId, action, name, color, isPrivate));
    }
}

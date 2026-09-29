package example.phaseteleporters;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class InterdimensionalTeleportScreen extends FramedTeleportScreenBase<InterdimensionalTeleportScreenHandler> {
    public InterdimensionalTeleportScreen(InterdimensionalTeleportScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected Text guiTitle() {
        return Text.translatable("gui.phaseteleporters.interdimensional_title");
    }

    @Override
    protected int getPortalStatus() {
        return handler.getPortalStatus();
    }

    @Override protected long getEnergy() { return handler.getEnergy(); }
    @Override protected long getMaxEnergy() { return handler.getMaxEnergy(); }

    public void applySnapshot(InterdimensionalTeleportFrequencySnapshotPayload snapshot) {
        if (snapshot.syncId() == handler.syncId)
            applyFrequencySnapshot(snapshot.assigned(), snapshot.assignedPrivate(), snapshot.currentColor(),
                    snapshot.publicNames(), snapshot.publicColors(), snapshot.publicCreators(),
                    snapshot.privateNames(), snapshot.privateColors(), snapshot.privateCreators());
    }

    @Override
    protected void sendColor(String name, boolean isPrivate, int color) {
        ClientPlayNetworking.send(new InterdimensionalTeleportFrequencyActionPayload(
                handler.syncId, InterdimensionalTeleportFrequencyActionPayload.COLOR, name, color, isPrivate));
    }

    @Override
    protected void sendFrequencyAction(int action, String name, int color, boolean isPrivate) {
        ClientPlayNetworking.send(new InterdimensionalTeleportFrequencyActionPayload(
                handler.syncId, action, name, color, isPrivate));
    }
}

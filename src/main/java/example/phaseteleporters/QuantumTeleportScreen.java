package example.phaseteleporters;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class QuantumTeleportScreen extends FramedTeleportScreenBase<QuantumTeleportScreenHandler> {
    public QuantumTeleportScreen(QuantumTeleportScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected Text guiTitle() {
        return Text.translatable("gui.phaseteleporters.quantum_title");
    }

    @Override
    protected int getPortalStatus() {
        return handler.getPortalStatus();
    }

    @Override protected long getEnergy() { return handler.getEnergy(); }
    @Override protected boolean showsPortalStatus() { return false; }
    @Override protected boolean showsColorConfiguration() { return false; }
    @Override protected long getMaxEnergy() { return handler.getMaxEnergy(); }

    public void applySnapshot(QuantumTeleportFrequencySnapshotPayload snapshot) {
        if (snapshot.syncId() == handler.syncId)
            applyFrequencySnapshot(snapshot.assigned(), snapshot.assignedPrivate(), snapshot.currentColor(),
                    snapshot.publicNames(), snapshot.publicColors(), snapshot.publicCreators(),
                    snapshot.privateNames(), snapshot.privateColors(), snapshot.privateCreators());
    }

    @Override
    protected void sendColor(String name, boolean isPrivate, int color) {
        ClientPlayNetworking.send(new QuantumTeleportFrequencyActionPayload(
                handler.syncId, QuantumTeleportFrequencyActionPayload.COLOR, name, color, isPrivate));
    }

    @Override
    protected void sendFrequencyAction(int action, String name, int color, boolean isPrivate) {
        ClientPlayNetworking.send(new QuantumTeleportFrequencyActionPayload(
                handler.syncId, action, name, color, isPrivate));
    }
}

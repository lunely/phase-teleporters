package example.phaseteleporters;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Hand;

public final class PortableEmergencyScreenHandler extends EmergencyTeleportScreenHandler {
    public PortableEmergencyScreenHandler(int syncId, PlayerInventory inventory) { this(syncId, inventory, null); }
    public PortableEmergencyScreenHandler(int syncId, PlayerInventory inventory, Hand hand) {
        super(PhaseTeleportersMod.PORTABLE_EMERGENCY_SCREEN_HANDLER, syncId, inventory, null, true, hand);
    }
}

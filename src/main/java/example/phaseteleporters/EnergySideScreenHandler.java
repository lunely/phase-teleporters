package example.phaseteleporters;

import example.phaseteleporters.energy.PESideMode;
import example.phaseteleporters.energy.PERedstoneMode;
import net.minecraft.util.math.Direction;

public interface EnergySideScreenHandler {
    PESideMode getSideMode(Direction side);
    Direction getSideFacing();
    PERedstoneMode getRedstoneMode();
    boolean hasRedstoneSignal();
    boolean isPublicAccess();
    boolean isSecurityOwner();
}

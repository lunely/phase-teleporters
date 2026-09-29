package example.phaseteleporters;

import example.phaseteleporters.energy.PEEnergyFormat;

final class PEGuiText {
    private PEGuiText() {}

    static String energy(long stored, long capacity) {
        return PEEnergyFormat.format(stored) + " / " + PEEnergyFormat.format(capacity);
    }

    static String teleporterEnergy(long stored, long capacity) {
        return energy(stored, capacity);
    }
}

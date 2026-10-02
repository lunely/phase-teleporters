package example.phaseteleporters.energy;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Keeps optional API classes out of the standalone mod's class loading path. */
public final class EnergyApiCompat {
    private static final boolean AVAILABLE = FabricLoader.getInstance().isModLoaded("team_reborn_energy");

    private EnergyApiCompat() {}

    public static void register() {
        if (AVAILABLE) TeamRebornEnergyCompat.register();
    }

    public static boolean connects(World world, BlockPos pos, Direction side) {
        return AVAILABLE && TeamRebornEnergyCompat.connects(world, pos, side);
    }

    public static boolean accepts(World world, BlockPos pos, Direction side) {
        return AVAILABLE && TeamRebornEnergyCompat.accepts(world, pos, side);
    }

    public static long push(World world, BlockPos pos, Direction side, PEStorage source, long limitPE) {
        return AVAILABLE ? TeamRebornEnergyCompat.push(world, pos, side, source, limitPE) : 0;
    }
}


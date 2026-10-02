package example.phaseteleporters.jade;

import example.phaseteleporters.BasicEnergyCableBlockEntity;
import example.phaseteleporters.energy.PEBlockEntity;
import net.minecraft.block.BlockWithEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Loaded by Jade's optional Fabric entrypoint, not by the mod initializer. */
@WailaPlugin
public final class PhaseTeleportersJadePlugin implements IWailaPlugin {
    @Override public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(MachineResourcesProvider.INSTANCE, PEBlockEntity.class);
        registration.registerBlockDataProvider(MachineResourcesProvider.INSTANCE, BasicEnergyCableBlockEntity.class);
    }

    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(MachineResourcesProvider.INSTANCE, BlockWithEntity.class);
    }
}

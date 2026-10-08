package example.phaseteleporters;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ColorConfiguratorModePayload(int slot, int mode) implements CustomPayload {
    public static final Id<ColorConfiguratorModePayload> ID =
            new Id<>(Identifier.of(PhaseTeleportersMod.MOD_ID, "color_configurator_mode"));
    public static final PacketCodec<RegistryByteBuf, ColorConfiguratorModePayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeVarInt(payload.slot);
                buf.writeVarInt(payload.mode);
            }, buf -> new ColorConfiguratorModePayload(buf.readVarInt(), buf.readVarInt()));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

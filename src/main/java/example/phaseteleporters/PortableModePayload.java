package example.phaseteleporters;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PortableModePayload(int slot, int mode) implements CustomPayload {
    public static final Id<PortableModePayload> ID =
            new Id<>(Identifier.of(PhaseTeleportersMod.MOD_ID, "portable_mode"));
    public static final PacketCodec<RegistryByteBuf, PortableModePayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeVarInt(payload.slot);
                buf.writeVarInt(payload.mode);
            }, buf -> new PortableModePayload(buf.readVarInt(), buf.readVarInt()));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

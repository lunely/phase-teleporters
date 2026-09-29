package example.phaseteleporters;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PortableSelectionPayload(int syncId, boolean interdimensional,
        boolean privateFrequency) implements CustomPayload {
    public static final Id<PortableSelectionPayload> ID =
            new Id<>(Identifier.of(PhaseTeleportersMod.MOD_ID, "portable_selection"));
    public static final PacketCodec<RegistryByteBuf, PortableSelectionPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeVarInt(payload.syncId);
                buf.writeBoolean(payload.interdimensional);
                buf.writeBoolean(payload.privateFrequency);
            },
            buf -> new PortableSelectionPayload(buf.readVarInt(), buf.readBoolean(),
                    buf.readBoolean()));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

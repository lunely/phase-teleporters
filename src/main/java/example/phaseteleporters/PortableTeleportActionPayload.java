package example.phaseteleporters;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PortableTeleportActionPayload(int syncId, boolean interdimensional,
        boolean privateFrequency, String name) implements CustomPayload {
    public static final Id<PortableTeleportActionPayload> ID =
            new Id<>(Identifier.of(PhaseTeleportersMod.MOD_ID, "portable_teleport_action"));
    public static final PacketCodec<RegistryByteBuf, PortableTeleportActionPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeVarInt(payload.syncId);
                buf.writeBoolean(payload.interdimensional);
                buf.writeBoolean(payload.privateFrequency);
                buf.writeString(payload.name, LocalFrequencyState.MAX_NAME_LENGTH);
            },
            buf -> new PortableTeleportActionPayload(buf.readVarInt(), buf.readBoolean(),
                    buf.readBoolean(), buf.readString(LocalFrequencyState.MAX_NAME_LENGTH)));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

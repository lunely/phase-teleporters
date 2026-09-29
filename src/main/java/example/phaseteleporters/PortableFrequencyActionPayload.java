package example.phaseteleporters;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PortableFrequencyActionPayload(int syncId, int action, boolean interdimensional,
        boolean privateFrequency, String name) implements CustomPayload {
    public static final int CREATE = 0;
    public static final int DELETE = 1;
    public static final Id<PortableFrequencyActionPayload> ID =
            new Id<>(Identifier.of(PhaseTeleportersMod.MOD_ID, "portable_frequency_action"));
    public static final PacketCodec<RegistryByteBuf, PortableFrequencyActionPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeVarInt(payload.syncId);
                buf.writeByte(payload.action);
                buf.writeBoolean(payload.interdimensional);
                buf.writeBoolean(payload.privateFrequency);
                buf.writeString(payload.name, LocalFrequencyState.MAX_NAME_LENGTH);
            },
            buf -> new PortableFrequencyActionPayload(buf.readVarInt(), buf.readByte(),
                    buf.readBoolean(), buf.readBoolean(),
                    buf.readString(LocalFrequencyState.MAX_NAME_LENGTH)));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

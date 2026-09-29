package example.phaseteleporters;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record InterdimensionalTeleportFrequencyActionPayload(int syncId, int action, String name, int color,
                                             boolean privateFrequency) implements CustomPayload {
    public static final int CREATE = 0;
    public static final int SET = 1;
    public static final int COLOR = 2;
    public static final int DELETE = 3;
    public static final Id<InterdimensionalTeleportFrequencyActionPayload> ID =
            new Id<>(Identifier.of(PhaseTeleportersMod.MOD_ID, "interdimensional_frequency_action"));
    public static final PacketCodec<RegistryByteBuf, InterdimensionalTeleportFrequencyActionPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeVarInt(payload.syncId);
                buf.writeByte(payload.action);
                buf.writeString(payload.name, InterdimensionalFrequencyState.MAX_NAME_LENGTH);
                buf.writeVarInt(payload.color);
                buf.writeBoolean(payload.privateFrequency);
            },
            buf -> new InterdimensionalTeleportFrequencyActionPayload(buf.readVarInt(), buf.readByte(),
                    buf.readString(InterdimensionalFrequencyState.MAX_NAME_LENGTH), buf.readVarInt(), buf.readBoolean()));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

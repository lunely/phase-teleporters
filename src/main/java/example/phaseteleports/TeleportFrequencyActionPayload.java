package example.phaseteleports;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TeleportFrequencyActionPayload(int syncId, int action, String name, int color,
                                             boolean privateFrequency) implements CustomPayload {
    public static final int CREATE = 0;
    public static final int SET = 1;
    public static final int COLOR = 2;
    public static final int DELETE = 3;
    public static final Id<TeleportFrequencyActionPayload> ID =
            new Id<>(Identifier.of(PhaseTeleportsMod.MOD_ID, "local_frequency_action"));
    public static final PacketCodec<RegistryByteBuf, TeleportFrequencyActionPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeVarInt(payload.syncId);
                buf.writeByte(payload.action);
                buf.writeString(payload.name, LocalFrequencyState.MAX_NAME_LENGTH);
                buf.writeVarInt(payload.color);
                buf.writeBoolean(payload.privateFrequency);
            },
            buf -> new TeleportFrequencyActionPayload(buf.readVarInt(), buf.readByte(),
                    buf.readString(LocalFrequencyState.MAX_NAME_LENGTH), buf.readVarInt(), buf.readBoolean()));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

package example.phaseteleporters;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record FrameMonitorTextPayload(BlockPos pos, String text) implements CustomPayload {
    public static final Id<FrameMonitorTextPayload> ID =
            new Id<>(Identifier.of(PhaseTeleportersMod.MOD_ID, "frame_monitor_text"));
    public static final PacketCodec<RegistryByteBuf, FrameMonitorTextPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeBlockPos(payload.pos);
                buf.writeString(payload.text, TeleportationFrameMonitorBlockEntity.MAX_TEXT_LENGTH);
            },
            buf -> new FrameMonitorTextPayload(buf.readBlockPos(),
                    buf.readString(TeleportationFrameMonitorBlockEntity.MAX_TEXT_LENGTH)));
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

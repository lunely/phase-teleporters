package example.phaseteleporters;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PortableSnapshotRequestPayload(int syncId) implements CustomPayload {
    public static final Id<PortableSnapshotRequestPayload> ID =
            new Id<>(Identifier.of(PhaseTeleportersMod.MOD_ID, "portable_snapshot_request"));
    public static final PacketCodec<RegistryByteBuf, PortableSnapshotRequestPayload> CODEC = PacketCodec.of(
            (payload, buf) -> buf.writeVarInt(payload.syncId),
            buf -> new PortableSnapshotRequestPayload(buf.readVarInt()));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

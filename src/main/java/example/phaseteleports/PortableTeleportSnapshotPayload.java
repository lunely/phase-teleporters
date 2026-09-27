package example.phaseteleports;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PortableTeleportSnapshotPayload(int syncId, List<Entry> localPublic,
        List<Entry> localPrivate, List<Entry> interdimensionalPublic,
        List<Entry> interdimensionalPrivate) implements CustomPayload {
    public record Entry(String name, int color, String creator) {}

    public static final Id<PortableTeleportSnapshotPayload> ID =
            new Id<>(Identifier.of(PhaseTeleportsMod.MOD_ID, "portable_teleport_snapshot"));
    public static final PacketCodec<RegistryByteBuf, PortableTeleportSnapshotPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeVarInt(payload.syncId);
                writeList(buf, payload.localPublic);
                writeList(buf, payload.localPrivate);
                writeList(buf, payload.interdimensionalPublic);
                writeList(buf, payload.interdimensionalPrivate);
            },
            buf -> new PortableTeleportSnapshotPayload(buf.readVarInt(), readList(buf), readList(buf),
                    readList(buf), readList(buf)));

    private static void writeList(RegistryByteBuf buf, List<Entry> entries) {
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeString(entry.name(), LocalFrequencyState.MAX_NAME_LENGTH);
            buf.writeVarInt(entry.color());
            buf.writeString(entry.creator(), 64);
        }
    }

    private static List<Entry> readList(RegistryByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 256) throw new IllegalArgumentException("Invalid frequency count");
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buf.readString(LocalFrequencyState.MAX_NAME_LENGTH),
                    buf.readVarInt(), buf.readString(64)));
        }
        return entries;
    }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

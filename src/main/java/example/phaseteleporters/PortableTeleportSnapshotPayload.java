package example.phaseteleporters;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PortableTeleportSnapshotPayload(int syncId, List<Entry> localPublic,
        List<Entry> localPrivate, List<Entry> interdimensionalPublic,
        List<Entry> interdimensionalPrivate, String selectedName,
        boolean selectedPrivate, boolean selectedInterdimensional,
        boolean viewPrivate, boolean viewInterdimensional) implements CustomPayload {
    public record Entry(String name, String creator) {}

    public static final Id<PortableTeleportSnapshotPayload> ID =
            new Id<>(Identifier.of(PhaseTeleportersMod.MOD_ID, "portable_teleport_snapshot"));
    public static final PacketCodec<RegistryByteBuf, PortableTeleportSnapshotPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeVarInt(payload.syncId);
                writeList(buf, payload.localPublic);
                writeList(buf, payload.localPrivate);
                writeList(buf, payload.interdimensionalPublic);
                writeList(buf, payload.interdimensionalPrivate);
                buf.writeString(payload.selectedName, LocalFrequencyState.MAX_NAME_LENGTH);
                buf.writeBoolean(payload.selectedPrivate);
                buf.writeBoolean(payload.selectedInterdimensional);
                buf.writeBoolean(payload.viewPrivate);
                buf.writeBoolean(payload.viewInterdimensional);
            },
            buf -> new PortableTeleportSnapshotPayload(buf.readVarInt(), readList(buf), readList(buf),
                    readList(buf), readList(buf),
                    buf.readString(LocalFrequencyState.MAX_NAME_LENGTH),
                    buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));

    private static void writeList(RegistryByteBuf buf, List<Entry> entries) {
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeString(entry.name(), LocalFrequencyState.MAX_NAME_LENGTH);
            buf.writeString(entry.creator(), 64);
        }
    }

    private static List<Entry> readList(RegistryByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > 256) throw new IllegalArgumentException("Invalid frequency count");
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buf.readString(LocalFrequencyState.MAX_NAME_LENGTH),
                    buf.readString(64)));
        }
        return entries;
    }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

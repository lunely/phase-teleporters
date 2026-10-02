package example.phaseteleporters;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record QuantumTeleportFrequencySnapshotPayload(int syncId, String assigned, boolean assignedPrivate,
        boolean assignedHidden, int currentColor, List<String> publicNames, List<Integer> publicColors,
        List<String> publicCreators, List<String> privateNames, List<Integer> privateColors,
        List<String> privateCreators) implements CustomPayload {
    public static final Id<QuantumTeleportFrequencySnapshotPayload> ID =
            new Id<>(Identifier.of(PhaseTeleportersMod.MOD_ID, "quantum_frequency_snapshot"));
    public static final PacketCodec<RegistryByteBuf, QuantumTeleportFrequencySnapshotPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeVarInt(payload.syncId);
                buf.writeString(payload.assigned, QuantumFrequencyState.MAX_NAME_LENGTH);
                buf.writeBoolean(payload.assignedPrivate);
                buf.writeBoolean(payload.assignedHidden);
                buf.writeVarInt(payload.currentColor);
                writeList(buf, payload.publicNames, payload.publicColors, payload.publicCreators);
                writeList(buf, payload.privateNames, payload.privateColors, payload.privateCreators);
            },
            buf -> {
                int syncId = buf.readVarInt();
                String assigned = buf.readString(QuantumFrequencyState.MAX_NAME_LENGTH);
                boolean assignedPrivate = buf.readBoolean();
                boolean assignedHidden = buf.readBoolean();
                int currentColor = buf.readVarInt();
                List<String> publicNames = new ArrayList<>();
                List<Integer> publicColors = new ArrayList<>();
                List<String> publicCreators = new ArrayList<>();
                List<String> privateNames = new ArrayList<>();
                List<Integer> privateColors = new ArrayList<>();
                List<String> privateCreators = new ArrayList<>();
                readList(buf, publicNames, publicColors, publicCreators);
                readList(buf, privateNames, privateColors, privateCreators);
                return new QuantumTeleportFrequencySnapshotPayload(syncId, assigned, assignedPrivate, assignedHidden,
                        currentColor, publicNames, publicColors, publicCreators,
                        privateNames, privateColors, privateCreators);
            });

    private static void writeList(RegistryByteBuf buf, List<String> names, List<Integer> colors,
            List<String> creators) {
        buf.writeVarInt(names.size());
        for (int i = 0; i < names.size(); i++) {
            buf.writeString(names.get(i), QuantumFrequencyState.MAX_NAME_LENGTH);
            buf.writeVarInt(colors.get(i));
            buf.writeString(creators.get(i), 64);
        }
    }

    private static void readList(RegistryByteBuf buf, List<String> names, List<Integer> colors,
            List<String> creators) {
        int count = buf.readVarInt();
        if (count < 0 || count > 256) throw new IllegalArgumentException("Invalid frequency count");
        for (int i = 0; i < count; i++) {
            names.add(buf.readString(QuantumFrequencyState.MAX_NAME_LENGTH));
            colors.add(buf.readVarInt());
            creators.add(buf.readString(64));
        }
    }

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

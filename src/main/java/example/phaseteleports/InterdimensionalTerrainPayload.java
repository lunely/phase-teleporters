package example.phaseteleports;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record InterdimensionalTerrainPayload(boolean active) implements CustomPayload {
    public static final Id<InterdimensionalTerrainPayload> ID =
            new Id<>(Identifier.of(PhaseTeleportsMod.MOD_ID, "interdimensional_terrain"));
    public static final PacketCodec<RegistryByteBuf, InterdimensionalTerrainPayload> CODEC = PacketCodec.of(
            (payload, buf) -> buf.writeBoolean(payload.active),
            buf -> new InterdimensionalTerrainPayload(buf.readBoolean()));

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}

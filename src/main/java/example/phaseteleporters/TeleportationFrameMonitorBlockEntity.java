package example.phaseteleporters;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;

public final class TeleportationFrameMonitorBlockEntity extends BlockEntity {
    public static final int MAX_LINE_LENGTH = 90;
    public static final int MAX_TEXT_LENGTH = MAX_LINE_LENGTH * 4 + 3;
    private String text = "";
    private int textColor = FrameColors.DEFAULT;
    private UUID editor;
    private long editUntil;

    public TeleportationFrameMonitorBlockEntity(BlockPos pos, BlockState state) {
        super(PhaseTeleportersMod.TELEPORTATION_FRAME_MONITOR_BLOCK_ENTITY, pos, state);
    }
    public String text() { return text; }
    public int textColor() { return textColor; }
    public void setTextColor(int color) {
        textColor = PortalColors.isValid(color) ? color : FrameColors.DEFAULT;
        sync();
    }
    public boolean beginEditing(UUID player) {
        if (editor != null && world != null) {
            var previous = world.getPlayerByUuid(editor);
            if (previous == null || previous.squaredDistanceTo(pos.toCenterPos()) > 64) editor = null;
        }
        if (world == null || (editor != null && !editor.equals(player)
                && world.getTime() < editUntil)) return false;
        editor = player;
        editUntil = world.getTime() + 1200;
        return true;
    }
    public boolean finishEditing(UUID player, String value) {
        if (!player.equals(editor)) return false;
        editor = null;
        setText(value);
        return true;
    }
    public void setText(String value) {
        String[] lines = (value == null ? "" : value.replace('\r', ' ')).split("\n", -1);
        String[] normalized = new String[4];
        for (int i = 0; i < 4; i++) {
            String line = i < lines.length ? lines[i] : "";
            normalized[i] = line.substring(0, Math.min(line.length(), MAX_LINE_LENGTH));
        }
        text = String.join("\n", normalized);
        sync();
    }
    private void sync() {
        markDirty();
        if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
    }
    @Override protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putString("MonitorText", text);
        nbt.putInt("MonitorTextColor", textColor);
    }
    @Override protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        text = nbt.getString("MonitorText");
        int savedColor = nbt.contains("MonitorTextColor") ? nbt.getInt("MonitorTextColor") : FrameColors.DEFAULT;
        textColor = PortalColors.isValid(savedColor) ? savedColor : FrameColors.DEFAULT;
        if (text.length() > MAX_TEXT_LENGTH) text = text.substring(0, MAX_TEXT_LENGTH);
    }
    @Override public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
    @Override public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup lookup) {
        return createNbt(lookup);
    }
}

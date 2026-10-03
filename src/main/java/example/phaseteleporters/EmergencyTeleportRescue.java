package example.phaseteleporters;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import example.phaseteleporters.mixin.EmergencyFallDamageAccess;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.block.BedBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class EmergencyTeleportRescue {
    private static final Map<UUID, Prepared> PENDING = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    private record Prepared(ServerPlayerEntity player, EmergencyTeleportBlockEntity pad, Vec3d arrival) {}
    private EmergencyTeleportRescue() {}

    public static void register() {
        // Absorption-only hits do not call setHealth; check their remaining yellow hearts too.
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, damage, blocked) -> {
            if (!blocked && damage > 0 && entity instanceof ServerPlayerEntity player)
                beforeHealthDamage(player, source, player.getHealth());
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!player.getStackInHand(hand).isOf(PhaseTeleportersMod.PORTABLE_TELEPORT)
                    || !world.getBlockState(hit.getBlockPos()).isOf(PhaseTeleportersMod.EMERGENCY_TELEPORT))
                return ActionResult.PASS;
            if (player instanceof ServerPlayerEntity serverPlayer
                    && world.getBlockEntity(hit.getBlockPos()) instanceof EmergencyTeleportBlockEntity pad)
                bind(serverPlayer, pad);
            return ActionResult.SUCCESS;
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient && world.getBlockEntity(pos) instanceof EmergencyTeleportBlockEntity pad
                    && !PESecurity.canOpen(player, pad)) return ActionResult.FAIL;
            return ActionResult.PASS;
        });
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) ->
                !(entity instanceof EmergencyTeleportBlockEntity pad) || PESecurity.canOpen(player, pad));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (Prepared rescue : java.util.List.copyOf(PENDING.values())) complete(rescue);
            PENDING.clear();
            long tick = server.getTicks();
            COOLDOWNS.values().removeIf(until -> until <= tick);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { PENDING.clear(); COOLDOWNS.clear(); });
    }

    public static boolean hasPortable(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++)
            if (player.getInventory().getStack(slot).isOf(PhaseTeleportersMod.PORTABLE_TELEPORT)) return true;
        return false;
    }

    public static boolean bind(ServerPlayerEntity player, EmergencyTeleportBlockEntity pad) {
        if (pad.getWorld() != player.getServerWorld()) return false;
        if (!PESecurity.canOpen(player, pad)) return false;
        if (!hasPortable(player)) {
            player.sendMessage(Text.translatable("message.phaseteleporters.emergency.need_portable"), true);
            return false;
        }
        var state = EmergencyTeleportState.get(player.getServer());
        state.profile(player.getUuid()).bind(pad);
        state.markDirty();
        player.sendMessage(Text.translatable("message.phaseteleporters.emergency.bound"), true);
        return true;
    }

    private static EmergencyTeleportState.Profile eligible(ServerPlayerEntity player, boolean falling) {
        if (!player.isAlive() || player.isSpectator() || player.getAbilities().invulnerable) return null;
        if (COOLDOWNS.getOrDefault(player.getUuid(), 0L) > player.getServer().getTicks()) return null;
        var profile = EmergencyTeleportState.get(player.getServer()).boundProfile(player.getUuid());
        if (profile == null || !profile.enabled
                || !profile.dimension.equals(player.getServerWorld().getRegistryKey())) return null;
        if (falling && !profile.rescueFalls) return null;
        if (profile.skipTotem && (player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)
                || player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING))) return null;
        if (falling && profile.skipWaterBucket) {
            for (int slot = 0; slot < 9; slot++)
                if (player.getInventory().getStack(slot).isOf(Items.WATER_BUCKET)) return null;
        }
        return hasPortable(player) ? profile : null;
    }

    /** Called after vanilla mitigation and absorption, immediately before health is changed. */
    public static float beforeHealthDamage(ServerPlayerEntity player, DamageSource source, float nextHealth) {
        // Administrative kills must remain effective.
        if (source.isOf(net.minecraft.entity.damage.DamageTypes.GENERIC_KILL)) return nextHealth;
        Prepared pending = PENDING.get(player.getUuid());
        if (pending != null) return pending.pad().getWorld() != player.getServerWorld()
                ? nextHealth : survivingHealth(player, nextHealth);
        boolean falling = source.isOf(net.minecraft.entity.damage.DamageTypes.FALL);
        var profile = eligible(player, falling);
        if (profile == null || nextHealth + player.getAbsorptionAmount() > profile.threshold) return nextHealth;
        return prepare(player, profile) ? survivingHealth(player, nextHealth) : nextHealth;
    }

    private static float survivingHealth(ServerPlayerEntity player, float nextHealth) {
        return nextHealth > 0 ? nextHealth : Math.min(1, player.getHealth());
    }

    /** Run only for downward movement of a linked player, just before an imminent landing. */
    public static boolean beforeFallMovement(ServerPlayerEntity player, Vec3d movement) {
        Prepared pending = PENDING.get(player.getUuid());
        if (pending != null) return pending.pad().getWorld() == player.getServerWorld();
        if (movement.y >= 0 || player.fallDistance <= 3 || player.isFallFlying()
                || player.isClimbing() || player.hasVehicle()) return false;
        var profile = eligible(player, true);
        if (profile == null) return false;
        Vec3d start = player.getPos().add(0, 0.01, 0);
        // A short probe avoids guessing the landing surface of a long fall.
        Vec3d end = start.add(movement.x, Math.min(-0.5, movement.y - 0.5), movement.z);
        var hit = player.getWorld().raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, player));
        if (hit.getType() != HitResult.Type.BLOCK) return false;
        BlockPos landing = hit.getBlockPos();
        if (!player.getWorld().getFluidState(landing).isEmpty()) return false;
        var block = player.getWorld().getBlockState(landing);
        if (block.isOf(Blocks.SLIME_BLOCK) && !player.isSneaking()
                || block.isOf(Blocks.POWDER_SNOW) || block.isOf(Blocks.COBWEB)) return false;
        float multiplier = block.isOf(Blocks.HAY_BLOCK) ? 0.2f
                : block.getBlock() instanceof BedBlock ? 0.5f : block.isOf(Blocks.HONEY_BLOCK) ? 0.2f : 1;
        float distance = player.fallDistance + (float) Math.max(0, start.y - hit.getPos().y);
        float damage = ((EmergencyFallDamageAccess) player).phaseteleporters$fallDamage(distance, multiplier);
        var resistance = player.getStatusEffect(StatusEffects.RESISTANCE);
        if (resistance != null) damage *= Math.max(0, 1 - (resistance.getAmplifier() + 1) * 0.2f);
        damage = DamageUtil.getInflictedDamage(damage,
                EnchantmentHelper.getProtectionAmount(player.getServerWorld(), player, player.getDamageSources().fall()));
        if (damage <= 0 || player.getHealth() + player.getAbsorptionAmount() - damage > profile.threshold)
            return false;
        return prepare(player, profile);
    }

    private static boolean prepare(ServerPlayerEntity player, EmergencyTeleportState.Profile profile) {
        if (!profile.dimension.equals(player.getServerWorld().getRegistryKey())) return false;
        Prepared prepared = destination(player, profile);
        if (prepared == null || prepared.pad().extract(EmergencyTeleportBlockEntity.TELEPORT_COST, false)
                != EmergencyTeleportBlockEntity.TELEPORT_COST) return false;
        PENDING.put(player.getUuid(), prepared);
        return true;
    }

    private static Prepared destination(ServerPlayerEntity player, EmergencyTeleportState.Profile profile) {
        ServerWorld world = player.getServer().getWorld(profile.dimension);
        if (world == null || !world.getWorldBorder().contains(profile.pos)) return null;
        // Load the destination on demand; no permanent chunk loader is installed.
        world.getChunk(profile.pos.getX() >> 4, profile.pos.getZ() >> 4);
        if (!(world.getBlockEntity(profile.pos) instanceof EmergencyTeleportBlockEntity pad)
                || !profile.matches(pad) || !pad.canPlayerTeleport(player) || !pad.canWork()
                || pad.getStored() < EmergencyTeleportBlockEntity.TELEPORT_COST) return null;
        Vec3d arrival = new Vec3d(profile.pos.getX() + 0.5,
                profile.pos.getY() + EmergencyTeleportBlockEntity.HEIGHT, profile.pos.getZ() + 0.5);
        var space = player.getBoundingBox().offset(arrival.subtract(player.getPos()));
        if (!world.isSpaceEmpty(player, space) || world.containsFluid(space)) return null;
        return new Prepared(player, pad, arrival);
    }

    private static void complete(Prepared transfer) {
        var player = transfer.player();
        var pad = transfer.pad();
        ServerWorld target = (ServerWorld) pad.getWorld();
        ServerWorld source = player.getServerWorld();
        Vec3d departure = player.getPos();
        Vec3d arrival = transfer.arrival();
        var profile = EmergencyTeleportState.get(player.getServer()).boundProfile(player.getUuid());
        boolean valid = player.isAlive() && !player.isDisconnected() && !pad.isRemoved()
                && source == target
                && profile != null && profile.enabled && profile.matches(pad) && hasPortable(player)
                && pad.canPlayerTeleport(player) && pad.canWork() && target.getBlockEntity(pad.getPos()) == pad;
        var space = player.getBoundingBox().offset(arrival.subtract(departure));
        valid = valid && target.isSpaceEmpty(player, space) && !target.containsFluid(space);
        boolean success = valid && player.teleport(target, arrival.x, arrival.y, arrival.z,
                Set.of(), player.getYaw(), player.getPitch());
        if (success) {
            player.setVelocity(Vec3d.ZERO);
            player.fallDistance = 0;
            player.closeHandledScreen();
            COOLDOWNS.put(player.getUuid(), (long) player.getServer().getTicks() + 100);
            PortalPlaneBlock.playPortableTeleportEffects(source, target,
                    departure.x, departure.y, departure.z, arrival.x, arrival.y, arrival.z);
            player.sendMessage(Text.translatable("message.phaseteleporters.emergency.rescued"), true);
        } else {
            pad.restoreStoredEnergy(EmergencyTeleportBlockEntity.TELEPORT_COST);
            pad.markDirty();
        }
    }
}

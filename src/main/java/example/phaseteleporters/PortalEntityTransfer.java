package example.phaseteleporters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

/** Moves a vehicle and all of its riders together, including across dimensions. */
final class PortalEntityTransfer {
    private PortalEntityTransfer() {}

    static List<Entity> group(Entity entity) {
        return entity.getRootVehicle().streamSelfAndPassengers().toList();
    }

    static double arrivalOffset(List<Entity> group, int interiorWidth, int interiorHeight) {
        double widest = group.stream().mapToDouble(Entity::getWidth).max().orElse(0);
        double tallest = group.stream().mapToDouble(Entity::getHeight).max().orElse(0);
        // Large entities arrive in front of the frame instead of inside its narrow opening.
        return widest > interiorWidth || tallest > interiorHeight
                ? widest / 2.0 + 0.625 : 0.18;
    }

    static boolean teleport(List<Entity> group, ServerWorld destination,
            double x, double y, double z) {
        List<Rider> riders = new ArrayList<>();
        Map<UUID, Origin> origins = new HashMap<>();
        Map<UUID, Entity> arrivals = new HashMap<>();
        for (Entity member : group) {
            if (member.isRemoved() || !(member.getWorld() instanceof ServerWorld source)) return false;
            origins.put(member.getUuid(), new Origin(source, member.getX(), member.getY(), member.getZ(),
                    member.getYaw(), member.getPitch()));
            arrivals.put(member.getUuid(), member);
            if (member.hasVehicle()) riders.add(new Rider(member.getUuid(), member.getVehicle().getUuid()));
        }
        for (Entity member : group) member.stopRiding();

        for (Entity member : group) {
            Entity arrived = move(member, destination, x, y, z, member.getYaw(), member.getPitch());
            if (arrived == null) {
                rollback(origins, arrivals, riders);
                return false;
            }
            arrivals.put(member.getUuid(), arrived);
        }
        for (Rider rider : riders) {
            Entity passenger = arrivals.get(rider.passenger());
            Entity vehicle = arrivals.get(rider.vehicle());
            if (passenger != null && vehicle != null) passenger.startRiding(vehicle, true);
        }
        return true;
    }

    private static void rollback(Map<UUID, Origin> origins, Map<UUID, Entity> entities, List<Rider> riders) {
        for (var entry : origins.entrySet()) {
            Entity current = entities.get(entry.getKey());
            Origin origin = entry.getValue();
            if (current.isRemoved()) continue;
            Entity restored = move(current, origin.world(), origin.x(), origin.y(), origin.z(),
                    origin.yaw(), origin.pitch());
            if (restored != null) entities.put(entry.getKey(), restored);
        }
        for (Rider rider : riders) {
            Entity passenger = entities.get(rider.passenger());
            Entity vehicle = entities.get(rider.vehicle());
            if (passenger != null && vehicle != null && !passenger.isRemoved() && !vehicle.isRemoved()
                    && passenger.getWorld() == vehicle.getWorld()) passenger.startRiding(vehicle, true);
        }
    }

    private static Entity move(Entity entity, ServerWorld world, double x, double y, double z,
            float yaw, float pitch) {
        if (entity.getWorld() == world)
            return entity.teleport(world, x, y, z, Set.of(), yaw, pitch) ? entity : null;
        // The UUID lookup may not expose an entity until the arrival chunk becomes active.
        // Keep the actual replacement instance returned by Minecraft instead.
        return entity.teleportTo(new TeleportTarget(world, new Vec3d(x, y, z), entity.getVelocity(),
                yaw, pitch, TeleportTarget.ADD_PORTAL_CHUNK_TICKET));
    }

    private record Origin(ServerWorld world, double x, double y, double z, float yaw, float pitch) {}
    private record Rider(UUID passenger, UUID vehicle) {}
}

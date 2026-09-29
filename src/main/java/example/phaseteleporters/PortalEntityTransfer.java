package example.phaseteleporters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;

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
        for (Entity member : group) {
            if (member.hasVehicle()) riders.add(new Rider(member.getUuid(), member.getVehicle().getUuid()));
        }
        for (Entity member : group) member.stopRiding();

        Map<UUID, Entity> arrivals = new HashMap<>();
        for (Entity member : group) {
            if (!member.teleport(destination, x, y, z, Set.of(), member.getYaw(), member.getPitch()))
                return false;
            Entity arrived = destination.getEntity(member.getUuid());
            if (arrived == null) return false;
            arrivals.put(member.getUuid(), arrived);
        }
        for (Rider rider : riders) {
            Entity passenger = arrivals.get(rider.passenger());
            Entity vehicle = arrivals.get(rider.vehicle());
            if (passenger != null && vehicle != null) passenger.startRiding(vehicle, true);
        }
        return true;
    }

    private record Rider(UUID passenger, UUID vehicle) {}
}

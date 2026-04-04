package et.xiaomu.essentialsd.utils;

import et.xiaomu.essentialsd.EssentialsD;
import cn.lunadeer.utils.XLogger;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public final class SeatManager {
    public static final String SEAT_TAG = "essd_seat";

    private SeatManager() {
    }

    public static boolean sit(Player player, Location seatLocation) {
        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            if (isSeat(vehicle)) {
                vehicle.remove();
            } else {
                return false;
            }
        }

        clearSeat(seatLocation);
        ArmorStand seat = spawnSeat(seatLocation);
        if (!seat.addPassenger(player)) {
            seat.remove();
            XLogger.debug("Failed to seat player " + player.getName() + " at " + seatLocation);
            return false;
        }

        XLogger.debug("Player " + player.getName() + " is sitting at " + seatLocation);
        return true;
    }

    public static boolean isSeat(Entity entity) {
        return entity instanceof ArmorStand && entity.getScoreboardTags().contains(SEAT_TAG);
    }

    public static void clearSeat(Location location) {
        for (Entity entity : location.getWorld().getNearbyEntities(location, 0.4, 0.4, 0.4)) {
            if (isSeat(entity)) {
                entity.remove();
            }
        }
    }

    private static ArmorStand spawnSeat(Location location) {
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.addScoreboardTag(SEAT_TAG);
        if (!EssentialsD.config.isDebug()) {
            armorStand.setVisible(false);
        }
        armorStand.setGravity(false);
        armorStand.setInvulnerable(true);
        armorStand.setSmall(true);
        return armorStand;
    }
}

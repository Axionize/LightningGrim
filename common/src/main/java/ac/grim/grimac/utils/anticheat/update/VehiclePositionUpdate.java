package ac.grim.grimac.utils.anticheat.update;

public record VehiclePositionUpdate(
        double fromX,
        double fromY,
        double fromZ,
        double toX,
        double toY,
        double toZ,
        float yaw,
        float pitch,
        boolean onGround,
        boolean isTeleport
) {}

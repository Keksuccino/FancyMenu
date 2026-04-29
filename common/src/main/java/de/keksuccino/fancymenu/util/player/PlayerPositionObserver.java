package de.keksuccino.fancymenu.util.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class PlayerPositionObserver {

    private static double currentPositionDeltaX;
    private static double currentPositionDeltaY;
    private static double currentPositionDeltaZ;
    private static double lastPositionX;
    private static double lastPositionY;
    private static double lastPositionZ;

    private PlayerPositionObserver() {
    }

    public static void tick() {
        Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity == null) {
            reset();
            return;
        }

        double currentX = cameraEntity.getX();
        double currentY = cameraEntity.getY();
        double currentZ = cameraEntity.getZ();

        currentPositionDeltaX = currentX - lastPositionX;
        currentPositionDeltaY = currentY - lastPositionY;
        currentPositionDeltaZ = currentZ - lastPositionZ;

        lastPositionX = currentX;
        lastPositionY = currentY;
        lastPositionZ = currentZ;
    }

    public static double getCurrentPositionDeltaX() {
        return currentPositionDeltaX;
    }

    public static double getCurrentPositionDeltaY() {
        return currentPositionDeltaY;
    }

    public static double getCurrentPositionDeltaZ() {
        return currentPositionDeltaZ;
    }

    private static void reset() {
        currentPositionDeltaX = 0.0D;
        currentPositionDeltaY = 0.0D;
        currentPositionDeltaZ = 0.0D;
        lastPositionX = 0.0D;
        lastPositionY = 0.0D;
        lastPositionZ = 0.0D;
    }

}

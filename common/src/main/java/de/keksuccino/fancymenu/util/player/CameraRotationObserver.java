package de.keksuccino.fancymenu.util.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class CameraRotationObserver {

    private static float currentRotationDeltaX;
    private static float currentRotationDeltaY;
    private static float lastRotationX;
    private static float lastRotationY;

    private CameraRotationObserver() {
    }

    public static void tick() {
        Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity == null) {
            reset();
            return;
        }

        float currentRotationX = cameraEntity.getXRot();
        float currentRotationY = cameraEntity.getYRot();

        currentRotationDeltaX = currentRotationX - lastRotationX;
        currentRotationDeltaY = currentRotationY - lastRotationY;

        lastRotationX = currentRotationX;
        lastRotationY = currentRotationY;
    }

    public static float getCurrentRotationDeltaX() {
        return currentRotationDeltaX;
    }

    public static float getCurrentRotationDeltaY() {
        return currentRotationDeltaY;
    }

    private static void reset() {
        currentRotationDeltaX = 0.0F;
        currentRotationDeltaY = 0.0F;
        lastRotationX = 0.0F;
        lastRotationY = 0.0F;
    }

}

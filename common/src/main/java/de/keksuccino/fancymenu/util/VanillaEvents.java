package de.keksuccino.fancymenu.util;

import de.keksuccino.fancymenu.util.input.MouseUtils;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

public class VanillaEvents {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final int GLFW_NO_MODIFIERS = 0;

    @Nullable
    private static MouseButtonInfo latestVanillaMouseButtonInfo = null;
    private static long latestVanillaMouseButtonInfoCachingTime = -1L;

    @NotNull
    public static MouseButtonInfo getLatestVanillaMouseButtonInfoOrDummy() {
        long now = System.currentTimeMillis();
        if ((latestVanillaMouseButtonInfoCachingTime != -1L) && ((latestVanillaMouseButtonInfoCachingTime + 2000) < now)) {
            LOGGER.warn("[FANCYMENU] Getting an possibly outdated latestVanillaMouseButtonInfo! Last caching time is more than 2 seconds in the past! This should be avoided.", new Exception("Outdated MouseButtonInfo in VanillaEvents!"));
        }
        if (latestVanillaMouseButtonInfo != null) return latestVanillaMouseButtonInfo;
        LOGGER.warn("[FANCYMENU] Getting latestVanillaMouseButtonInfo before caching it! This should be avoided.", new Exception("No MouseButtonInfo cached in VanillaEvents!"));
        return new MouseButtonInfo(0, -1);
    }

    public static void updateLatestVanillaMouseButtonInfo(@NotNull MouseButtonInfo info) {
        latestVanillaMouseButtonInfoCachingTime = System.currentTimeMillis();
        latestVanillaMouseButtonInfo = Objects.requireNonNull(info);
    }

    /**
     * Will create a new {@link MouseButtonEvent}.
     */
    @NotNull
    public static MouseButtonEvent mouseButtonEvent(double mouseX, double mouseY, @NotNull MouseButtonInfo info) {
        return new MouseButtonEvent(mouseX, mouseY, info);
    }

    /**
     * Will create a new {@link MouseButtonEvent} with a new {@link MouseButtonInfo}, using the provided {@code button} and {@code modifiers} parameters.
     */
    @NotNull
    public static MouseButtonEvent mouseButtonEvent(double mouseX, double mouseY, int button, int modifiers) {
        return mouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, modifiers));
    }

    /**
     * Will create a new {@link MouseButtonEvent} with a new {@link MouseButtonInfo}, using the provided {@code button} parameter and {@link MouseButtonInfo#modifiers()} from the {@link VanillaEvents#latestVanillaMouseButtonInfo}.
     */
    @NotNull
    public static MouseButtonEvent mouseButtonEvent(double mouseX, double mouseY, int button) {
        MouseButtonInfo cached = getLatestVanillaMouseButtonInfoOrDummy();
        return mouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, cached.modifiers()));
    }

    /**
     * Will create a new {@link MouseButtonEvent}, automatically resolve the {@code mouseX} and {@code mouseY} coordinates and create a new {@link MouseButtonInfo}, using the provided {@code button} parameter and {@link MouseButtonInfo#modifiers()} from the {@link VanillaEvents#latestVanillaMouseButtonInfo}.
     */
    @NotNull
    public static MouseButtonEvent mouseButtonEvent(int button) {
        MouseButtonInfo cached = getLatestVanillaMouseButtonInfoOrDummy();
        return mouseButtonEvent(MouseUtils.getScaledMouseX(), MouseUtils.getScaledMouseY(), new MouseButtonInfo(button, cached.modifiers()));
    }

    /**
     * Will create a new {@link MouseButtonEvent}, automatically resolve the {@code mouseX} and {@code mouseY} coordinates and create a new {@link MouseButtonInfo}, using the provided {@code button} and {@code modifiers} parameters.
     */
    @NotNull
    public static MouseButtonEvent mouseButtonEvent(int button, int modifiers) {
        return mouseButtonEvent(MouseUtils.getScaledMouseX(), MouseUtils.getScaledMouseY(), new MouseButtonInfo(button, modifiers));
    }

    /**
     * Will create a new {@link MouseButtonEvent} and use the cached {@link VanillaEvents#latestVanillaMouseButtonInfo} as {@link MouseButtonInfo}.
     */
    @NotNull
    public static MouseButtonEvent mouseButtonEvent(double mouseX, double mouseY) {
        return mouseButtonEvent(mouseX, mouseY, getLatestVanillaMouseButtonInfoOrDummy());
    }

    /**
     * Will create a new {@link MouseButtonEvent}, automatically resolve the {@code mouseX} and {@code mouseY} coordinates and use the cached {@link VanillaEvents#latestVanillaMouseButtonInfo} as {@link MouseButtonInfo}.
     */
    @NotNull
    public static MouseButtonEvent mouseButtonEvent() {
        return mouseButtonEvent(MouseUtils.getScaledMouseX(), MouseUtils.getScaledMouseY(), getLatestVanillaMouseButtonInfoOrDummy());
    }

}

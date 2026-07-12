package de.keksuccino.fancymenu.util.auth;

import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.mod.UniversalModContainer;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;

public class ModValidator {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final DrawableColor INVALID_COLOR = DrawableColor.of(Color.RED);
    private static final String FANCYMENU_MOD_ID = "fancymenu";
    private static final String FANCYMENU_DISPLAY_NAME = "FancyMenu";
    private static final String FANCYMENU_DESCRIPTION = "Customize Minecraft's menus with ease!";
    private static final String FANCYMENU_LICENSE = "DSMSLv3 (DON'T SNATCH MA STUFF LICENSE v3)";

    public static boolean isFancyMenuLoaded() {
        return Services.PLATFORM.isModLoaded(FANCYMENU_MOD_ID);
    }

    public static boolean isFancyMenuDisplayName() {
        return hasFancyMenuDisplayName(Services.PLATFORM.getLoadedMod(FANCYMENU_MOD_ID));
    }

    public static boolean isFancyMenuDescription() {
        return hasFancyMenuDescription(Services.PLATFORM.getLoadedMod(FANCYMENU_MOD_ID));
    }

    public static boolean isFancyMenuLicense() {
        return hasFancyMenuLicense(Services.PLATFORM.getLoadedMod(FANCYMENU_MOD_ID));
    }

    public static boolean isFancyMenuMetadataValid() {
        if (!isFancyMenuLoaded()) return false;
        UniversalModContainer mod = Services.PLATFORM.getLoadedMod(FANCYMENU_MOD_ID);
        return isFancyMenuMetadataValid(mod);
    }

    public static void printInfo() {

        LOGGER.warn("");
        LOGGER.warn("================ FANCYMENU ================");
        LOGGER.warn("");

        UniversalModContainer mod = Services.PLATFORM.getLoadedMod(FANCYMENU_MOD_ID);
        LOGGER.warn("FM found in loaded mods: " + isFancyMenuLoaded());
        LOGGER.warn("FM metadata has correct display name: " + hasFancyMenuDisplayName(mod));
        LOGGER.warn("FM metadata has correct description: " + hasFancyMenuDescription(mod));
        LOGGER.warn("FM metadata has correct license: " + hasFancyMenuLicense(mod));

        LOGGER.warn("");
        LOGGER.warn("===========================================");
        LOGGER.warn("");

    }

    public static void renderInvalidError(GuiGraphics graphics) {

        Screen current = Minecraft.getInstance().screen;
        if (current == null) return;

        //Do not render the error in non-Minecraft screens
        if (!isValidMinecraftScreen(current)) return;

        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);

        graphics.fill(0, 0, current.width, current.height, DrawableColor.BLACK.getColorInt());

        graphics.drawCenteredString(Minecraft.getInstance().font, "MODIFIED FANCYMENU JAR FOUND!", current.width / 2, (current.height / 2) - 10, INVALID_COLOR.getColorInt());
        graphics.drawCenteredString(Minecraft.getInstance().font, "PLEASE DOWNLOAD A VALID BUILD FROM CURSEFORGE OR MODRINTH!", current.width / 2, (current.height / 2) + 5, INVALID_COLOR.getColorInt());

        graphics.pose().popPose();

    }

    private static boolean isValidMinecraftScreen(@NotNull Screen screen) {
        if (screen instanceof TitleScreen) return true;
        if (screen instanceof PauseScreen) return true;
        if (screen instanceof SelectWorldScreen) return true;
        if (screen instanceof OptionsScreen) return true;
        if (screen instanceof OptionsSubScreen) return true;
        if (screen instanceof JoinMultiplayerScreen) return true;
        if (screen instanceof DeathScreen) return true;
        if (screen instanceof InventoryScreen) return true;
        if (screen instanceof AbstractContainerScreen<?>) return true;
        if (screen instanceof CreativeModeInventoryScreen) return true;
        if (screen instanceof ChatScreen) return true;
        return false;
    }

    static boolean isFancyMenuMetadataValid(@Nullable UniversalModContainer mod) {
        return hasFancyMenuDisplayName(mod) && hasFancyMenuDescription(mod) && hasFancyMenuLicense(mod);
    }

    private static boolean hasFancyMenuDisplayName(@Nullable UniversalModContainer mod) {
        return mod != null && FANCYMENU_DISPLAY_NAME.equals(mod.name());
    }

    private static boolean hasFancyMenuDescription(@Nullable UniversalModContainer mod) {
        return mod != null && FANCYMENU_DESCRIPTION.equals(mod.description());
    }

    private static boolean hasFancyMenuLicense(@Nullable UniversalModContainer mod) {
        return mod != null && FANCYMENU_LICENSE.equals(mod.license());
    }

}

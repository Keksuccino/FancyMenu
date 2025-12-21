package de.keksuccino.fancymenu.customization.screenoverlay;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractOverlay implements Renderable {

    @NotNull
    private String instanceIdentifier = ScreenCustomization.generateUniqueIdentifier();
    @ApiStatus.Internal
    public boolean showOverlay = false;

    @NotNull
    public String getInstanceIdentifier() {
        return this.instanceIdentifier;
    }

    public void setInstanceIdentifier(@NotNull String instanceIdentifier) {
        this.instanceIdentifier = instanceIdentifier;
    }

    @Override
    public abstract void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);

    /**
     * Gets called after the current {@link Screen} got initialized or resized.<br>
     * At this point all its widgets should be available.
     */
    public void onScreenInitializedOrResized(@NotNull Screen screen) {
    }

    @Nullable
    protected static Screen getScreen() {
        return Minecraft.getInstance().screen;
    }

    protected static int getScreenWidth() {
        var s = getScreen();
        return (s != null) ? s.width : 1;
    }

    protected static int getScreenHeight() {
        var s = getScreen();
        return (s != null) ? s.height : 1;
    }

}

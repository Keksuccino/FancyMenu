package de.keksuccino.fancymenu.customization.layout.editor.buddy;

import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper to integrate the TamagotchiBuddy with any Minecraft screen
 */
public class TamagotchiEasterEgg extends AbstractContainerEventHandler implements Renderable, NarratableEntry, FancyMenuUiComponent {

    private static final Logger LOGGER = LogManager.getLogger();

    private final TamagotchiBuddy buddy;
    private final List<GuiEventListener> unusedDummyChildren = new ArrayList<>(); // don't use this and handle event method calls manually instead
    private int screenWidth;
    private int screenHeight;

    public TamagotchiEasterEgg(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.buddy = new TamagotchiBuddy(screenWidth, screenHeight);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render the buddy (including radial menu when active)
        buddy.render(graphics, mouseX, mouseY, partialTick);
    }

    public void tick() {
        buddy.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.buddy.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.buddy.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.buddy.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return unusedDummyChildren;
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(0, 0, screenWidth, screenHeight);
    }

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        buddy.setScreenSize(width, height);
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }
}
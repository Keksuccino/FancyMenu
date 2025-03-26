package de.keksuccino.fancymenu.customization.layout.editor.buddy;

import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
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

    private static final ResourceLocation WORK_BUTTON = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/work_button.png");
    private static final int BUTTON_SIZE = 24;

    private final TamagotchiBuddy buddy;
    private final List<GuiEventListener> unusedDummyChildren = new ArrayList<>(); // don't use this and handle event method calls manually instead
    private int screenWidth;
    private int screenHeight;
    private int workButtonX;
    private int workButtonY;

    public TamagotchiEasterEgg(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.buddy = new TamagotchiBuddy(screenWidth, screenHeight);

        // Position work button in bottom corner
        this.workButtonX = screenWidth - BUTTON_SIZE - 10;
        this.workButtonY = screenHeight - BUTTON_SIZE - 10;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render the buddy
        buddy.render(graphics, mouseX, mouseY, partialTick);

        // Render work button if buddy needs to work
        if (buddy.needsWork()) {
            boolean hovered = isMouseOverWorkButton(mouseX, mouseY);
            int yOffset = hovered ? BUTTON_SIZE : 0;

            graphics.blit(
                    RenderType::guiTextured,
                    WORK_BUTTON,
                    workButtonX, workButtonY,
                    0, yOffset,
                    BUTTON_SIZE, BUTTON_SIZE,
                    BUTTON_SIZE, BUTTON_SIZE * 2
            );
        }
    }

    public void tick() {
        buddy.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        // Check work button first
        if (buddy.needsWork() && isMouseOverWorkButton(mouseX, mouseY) && button == 0) {
            buddy.startWorking();
            return true;
        }

        // Then let buddy handle its clicks
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

    private boolean isMouseOverWorkButton(double mouseX, double mouseY) {
        return buddy.needsWork() &&
                mouseX >= workButtonX && mouseX < workButtonX + BUTTON_SIZE &&
                mouseY >= workButtonY && mouseY < workButtonY + BUTTON_SIZE;
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

        // Update work button position
        this.workButtonX = width - BUTTON_SIZE - 10;
        this.workButtonY = height - BUTTON_SIZE - 10;
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

}
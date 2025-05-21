package de.keksuccino.fancymenu.customization.layout.editor.buddy.gui;

import de.keksuccino.fancymenu.customization.layout.editor.buddy.TamagotchiBuddy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * Represents a button in the buddy GUI
 */
public class BuddyGuiButton {

    public static final int BUTTON_WIDTH = 80;
    public static final int BUTTON_HEIGHT = 20;

    @NotNull
    private final ButtonNameSupplier nameSupplier;
    @NotNull
    private final Runnable action;
    @Nullable
    private final BooleanSupplier activeCondition;
    @NotNull
    private final TamagotchiBuddy buddy;

    private int x, y;
    private boolean active = true;

    public BuddyGuiButton(@NotNull TamagotchiBuddy buddy, @NotNull ButtonNameSupplier nameSupplier, @NotNull Runnable action, @Nullable BooleanSupplier activeCondition) {
        this.nameSupplier = nameSupplier;
        this.action = action;
        this.activeCondition = activeCondition;
        this.buddy = buddy;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + BUTTON_WIDTH &&
                mouseY >= y && mouseY < y + BUTTON_HEIGHT;
    }

    public void onClick() {
        if (active) {
            action.run();
        }
    }

    public boolean isActive() {
        return active;
    }

    public void updateActiveState() {
        this.active = activeCondition == null || activeCondition.getAsBoolean();
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean hovered = isMouseOver(mouseX, mouseY) && active;
        int backgroundColor = active ? (hovered ? 0xFF909090 : 0xFF606060) : 0xFF404040;
        int textColor = active ? 0xFFFFFFFF : 0xFFAAAAAA;
        Font font = Minecraft.getInstance().font;

        // Draw button background
        graphics.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, backgroundColor);
        graphics.renderOutline(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, 0xFF000000);

        // Draw button text
        String name = nameSupplier.name(this.buddy);
        int textX = x + (BUTTON_WIDTH - font.width(name)) / 2;
        int textY = y + (BUTTON_HEIGHT - 8) / 2;
        graphics.drawString(font, name, textX, textY, textColor);
    }

    @FunctionalInterface
    public interface ButtonNameSupplier {
        String name(@NotNull TamagotchiBuddy buddy);
    }

}
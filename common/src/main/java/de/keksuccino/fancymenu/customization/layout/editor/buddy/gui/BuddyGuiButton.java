package de.keksuccino.fancymenu.customization.layout.editor.buddy.gui;

import de.keksuccino.fancymenu.customization.layout.editor.buddy.Buddy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.BooleanSupplier;

/**
 * Represents a button in the buddy GUI
 */
public class BuddyGuiButton implements Renderable {

    public static final ResourceLocation DEFAULT_BUTTON_NORMAL = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/gui/default_button_normal.png");
    public static final ResourceLocation DEFAULT_BUTTON_HOVER = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/gui/default_button_hover.png");
    public static final ResourceLocation DEFAULT_BUTTON_INACTIVE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/gui/default_button_inactive.png");
    public static final ResourceLocation BUTTON_CLOSE_NORMAL = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/gui/button_close_normal.png");
    public static final ResourceLocation BUTTON_CLOSE_HOVER = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/buddy/gui/button_close_hover.png");

    private static final int DEFAULT_BUTTON_WIDTH = 80;
    private static final int DEFAULT_BUTTON_HEIGHT = 20;

    @NotNull
    protected final ButtonNameSupplier nameSupplier;
    @NotNull
    protected final Runnable action;
    @Nullable
    protected final BooleanSupplier activeCondition;
    @NotNull
    protected final Buddy buddy;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected boolean active = true;
    @Nullable
    protected ResourceLocation normalTexture = null;
    @Nullable
    protected ResourceLocation hoverTexture = null;
    @Nullable
    protected ResourceLocation inactiveTexture = null;

    public BuddyGuiButton(@NotNull Buddy buddy, int x, int y, int width, int height, @NotNull ButtonNameSupplier nameSupplier, @NotNull Runnable action, @Nullable BooleanSupplier activeCondition) {
        this.nameSupplier = nameSupplier;
        this.action = action;
        this.activeCondition = activeCondition;
        this.buddy = buddy;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.setDefaultButtonTextures();
    }

    public BuddyGuiButton(@NotNull Buddy buddy, int x, int y, @NotNull ButtonNameSupplier nameSupplier, @NotNull Runnable action, @Nullable BooleanSupplier activeCondition) {
        this(buddy, x, y, DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT, nameSupplier, action, activeCondition);
    }

    public BuddyGuiButton(@NotNull Buddy buddy, @NotNull ButtonNameSupplier nameSupplier, @NotNull Runnable action, @Nullable BooleanSupplier activeCondition) {
        this(buddy, 0, 0, DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT, nameSupplier, action, activeCondition);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        boolean hovered = isMouseOver(mouseX, mouseY) && active;
        ResourceLocation backgroundTexture = active ? (hovered ? this.hoverTexture : this.normalTexture) : this.inactiveTexture;
        int backgroundColor = active ? (hovered ? 0xFF909090 : 0xFF606060) : 0xFF404040;
        int textColor = active ? 0xFFFFFFFF : 0xFFAAAAAA;
        Font font = Minecraft.getInstance().font;

        // Draw button background
        if (backgroundTexture != null) {
            graphics.blit(RenderType::guiTextured, backgroundTexture, this.x, this.y, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
        } else {
            graphics.fill(x, y, x + this.width, y + this.height, backgroundColor);
            graphics.renderOutline(x, y, this.width, this.height, 0xFF000000);
        }

        // Draw button text
        String name = nameSupplier.name(this.buddy);
        int textX = x + (this.width - font.width(name)) / 2;
        int textY = y + (this.height - 8) / 2;
        graphics.drawString(font, name, textX, textY, textColor);
    }

    public void onClick() {
        if (active) {
            action.run();
        }
    }

    public BuddyGuiButton setTextures(@Nullable ResourceLocation normal, @Nullable ResourceLocation hover, @Nullable ResourceLocation inactive) {
        this.normalTexture = normal;
        this.hoverTexture = hover;
        this.inactiveTexture = inactive;
        return this;
    }

    public BuddyGuiButton setDefaultButtonTextures() {
        return this.setTextures(DEFAULT_BUTTON_NORMAL, DEFAULT_BUTTON_HOVER, DEFAULT_BUTTON_INACTIVE);
    }

    public BuddyGuiButton setCloseButtonTextures() {
        return this.setTextures(BUTTON_CLOSE_NORMAL, BUTTON_CLOSE_HOVER, BUTTON_CLOSE_NORMAL);
    }

    public BuddyGuiButton setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public BuddyGuiButton setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public int getX() {
        return x;
    }

    public BuddyGuiButton setX(int x) {
        this.x = x;
        return this;
    }

    public int getY() {
        return y;
    }

    public BuddyGuiButton setY(int y) {
        this.y = y;
        return this;
    }

    public int getWidth() {
        return width;
    }

    public BuddyGuiButton setWidth(int width) {
        this.width = width;
        return this;
    }

    public int getHeight() {
        return height;
    }

    public BuddyGuiButton setHeight(int height) {
        this.height = height;
        return this;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + this.width && mouseY >= y && mouseY < y + this.height;
    }

    public boolean isActive() {
        return active;
    }

    public void updateActiveState() {
        this.active = activeCondition == null || activeCondition.getAsBoolean();
    }

    @FunctionalInterface
    public interface ButtonNameSupplier {
        String name(@NotNull Buddy buddy);
    }

}
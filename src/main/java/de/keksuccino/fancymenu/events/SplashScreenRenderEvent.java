package de.keksuccino.fancymenu.events;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.screens.LoadingOverlay;

public class SplashScreenRenderEvent extends EventBase {

    protected LoadingOverlay screen;
    protected PoseStack matrix;
    protected int mouseX;
    protected int mouseY;
    protected float partialTicks;
    protected int screenWidth;
    protected int screenHeight;

    protected boolean renderLogo = true;
    protected boolean renderBar = true;
    protected int backgroundColor = -1;

    public SplashScreenRenderEvent(LoadingOverlay screen, PoseStack matrix, int mouseX, int mouseY, float partialTicks, int screenWidth, int screenHeight) {
        this.screen = screen;
        this.matrix = matrix;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.partialTicks = partialTicks;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    public PoseStack getMatrixStack() {
        return this.matrix;
    }

    public LoadingOverlay getGui() {
        return this.screen;
    }

    public int getMouseX() {
        return this.mouseX;
    }

    public int getMouseY() {
        return this.mouseY;
    }

    public float getRenderPartialTicks() {
        return this.partialTicks;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setRenderBar(boolean renderBar) {
        this.renderBar = renderBar;
    }

    public boolean isRenderBar() {
        return renderBar;
    }

    public void setRenderLogo(boolean renderLogo) {
        this.renderLogo = renderLogo;
    }

    public boolean isRenderLogo() {
        return renderLogo;
    }

    public static class Pre extends SplashScreenRenderEvent {

        public Pre(LoadingOverlay screen, PoseStack matrix, int mouseX, int mouseY, float partialTicks, int screenWidth, int screenHeight) {
            super(screen, matrix, mouseX, mouseY, partialTicks, screenWidth, screenHeight);
        }

        @Override
        public boolean isCancelable() {
            return true;
        }

    }

    public static class Post extends SplashScreenRenderEvent {

        public Post(LoadingOverlay screen, PoseStack matrix, int mouseX, int mouseY, float partialTicks, int screenWidth, int screenHeight) {
            super(screen, matrix, mouseX, mouseY, partialTicks, screenWidth, screenHeight);
        }

    }

}

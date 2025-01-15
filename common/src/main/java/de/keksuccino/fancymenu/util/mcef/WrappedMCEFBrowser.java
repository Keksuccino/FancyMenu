package de.keksuccino.fancymenu.util.mcef;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class WrappedMCEFBrowser extends AbstractWidget implements Closeable, FancyMenuUiComponent {

    //TODO interactable überall implementieren + eigenes focus handling !!!!!!!!!!!
    //TODO interactable überall implementieren + eigenes focus handling !!!!!!!!!!!
    //TODO interactable überall implementieren + eigenes focus handling !!!!!!!!!!!
    //TODO interactable überall implementieren + eigenes focus handling !!!!!!!!!!!
    //TODO interactable überall implementieren + eigenes focus handling !!!!!!!!!!!
    //TODO interactable überall implementieren + eigenes focus handling !!!!!!!!!!!
    //TODO interactable überall implementieren + eigenes focus handling !!!!!!!!!!!

    protected static final Logger LOGGER = LogManager.getLogger();

    protected final MCEFBrowser browser;
    protected final Minecraft minecraft = Minecraft.getInstance();
    protected boolean browserFocused = false;
    protected boolean interactable = true;
    protected float opacity = 1.0F;
    protected boolean autoHandle = true;
    protected UUID genericIdentifier = UUID.randomUUID();

    @NotNull
    public static WrappedMCEFBrowser build(@NotNull String url, boolean transparent, boolean autoHandle) {
        WrappedMCEFBrowser b = new WrappedMCEFBrowser(url, transparent);
        b.autoHandle = autoHandle;
        return b;
    }

    @NotNull
    public static WrappedMCEFBrowser build(@NotNull String url, boolean transparent, boolean autoHandle, int x, int y, int width, int height) {
        WrappedMCEFBrowser b = build(url, transparent, autoHandle);
        b.setSize(width, height);
        b.setPosition(x, y);
        return b;
    }

    protected WrappedMCEFBrowser(@NotNull String url, boolean transparent) {
        super(0, 0, 0, 0, Component.empty());
        this.browser = MCEF.createBrowser(url, transparent);
        this.setSize(200, 200);
        this.setPosition(0, 0);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        try {

            if (this.autoHandle) BrowserHandler.notifyHandler(this.genericIdentifier.toString(), this);

            RenderSystem.disableDepthTest();
            RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, this.browser.getRenderer().getTextureID());
            Tesselator t = Tesselator.getInstance();
            BufferBuilder buffer = t.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            int alpha = (int)(this.opacity * 255.0F);

            // Bottom left vertex
            buffer.addVertex(this.getX(), this.getY() + this.getHeight(), 0.0F)
                    .setUv(0.0F, 1.0F)
                    .setColor(255, 255, 255, alpha);

            // Bottom right vertex
            buffer.addVertex(this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0.0F)
                    .setUv(1.0F, 1.0F)
                    .setColor(255, 255, 255, alpha);

            // Top right vertex
            buffer.addVertex(this.getX() + this.getWidth(), this.getY(), 0.0F)
                    .setUv(1.0F, 0.0F)
                    .setColor(255, 255, 255, alpha);

            // Top left vertex
            buffer.addVertex(this.getX(), this.getY(), 0.0F)
                    .setUv(0.0F, 0.0F)
                    .setColor(255, 255, 255, alpha);

            BufferUploader.drawWithShader(Objects.requireNonNull(buffer.build()));
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.enableDepthTest();
            graphics.flush();

        } catch (Exception ex) {

            LOGGER.error("[FANCYMENU] Failed to render MCEFBrowser!", ex);

        }

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOver(mouseX, mouseY) && this.interactable) {
            this.browser.sendMousePress(this.convertMouseX(mouseX), this.convertMouseY(mouseY), button);
            this.browser.setFocus(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.browser.sendMouseRelease(this.convertMouseX(mouseX), this.convertMouseY(mouseY), button);
        this.browser.setFocus(true);
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.browser.sendMouseMove(this.convertMouseX(mouseX), this.convertMouseY(mouseY));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isMouseOver(mouseX, mouseY) && this.interactable) {
            this.browser.sendMouseWheel(this.convertMouseX(mouseX), this.convertMouseY(mouseY), scrollY, 0);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.interactable) {
            this.browser.sendKeyPress(keyCode, scanCode, modifiers);
            this.browser.setFocus(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (this.interactable) {
            this.browser.sendKeyRelease(keyCode, scanCode, modifiers);
            this.browser.setFocus(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.interactable) {
            if (codePoint == (char) 0) return true;
            this.browser.sendKeyTyped(codePoint, modifiers);
            this.browser.setFocus(true);
            return true;
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        this.browser.resize(this.convertWidth(width), this.convertHeight(height));
    }

    protected int convertMouseX(double mouseX) {
        return (int)((mouseX - (double)this.getX()) * this.minecraft.getWindow().getGuiScale());
    }

    protected int convertMouseY(double mouseY) {
        return (int)((mouseY - (double)this.getY()) * this.minecraft.getWindow().getGuiScale());
    }

    protected int convertWidth(double width) {
        return (int) (width * this.minecraft.getWindow().getGuiScale());
    }

    protected int convertHeight(double height) {
        return (int) (height * this.minecraft.getWindow().getGuiScale());
    }

//    protected void setBrowserFocus(boolean focused) {
//        if (!this.interactable) return;
//        this.browserFocused = focused;
//        this.browser.setFocus(focused);
//    }

    public void setInteractable(boolean interactable) {
        this.interactable = interactable;
        if (!this.interactable) {
            this.browserFocused = false;
            this.browser.setFocus(false);
        }
    }

    public boolean isInteractable() {
        return this.interactable;
    }

    public void setAutoHandle(boolean autoHandle) {
        this.autoHandle = autoHandle;
    }

    public boolean isAutoHandle() {
        return this.autoHandle;
    }

    public void goBack() {
        if (this.browser.canGoBack()) this.browser.goBack();
    }

    public void goForward() {
        if (this.browser.canGoForward()) this.browser.goForward();
    }

    public String getUrl() {
        return this.browser.getURL();
    }

    public void setUrl(@NotNull String url) {
        this.browser.loadURL(url);
    }

    public void reload() {
        this.browser.reload();
    }

    /**
     * @param opacity Alpha value between 0.0F and 1.0F
     */
    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    @NotNull
    public MCEFBrowser getBrowser() {
        return this.browser;
    }

    @Override
    public void close() throws IOException {
        this.browser.close(true);
    }

}

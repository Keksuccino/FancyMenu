package de.keksuccino.fancymenu;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

public class McefExampleScreen extends Screen {

    private static final int BROWSER_DRAW_OFFSET = 20;
    private MCEFBrowser browser;

    public McefExampleScreen(Component component) {
        super(component);
    }

    protected void init() {
        super.init();
        if (this.browser == null) {
            String url = "https://www.google.com";
            boolean transparent = true;
            this.browser = MCEF.createBrowser(url, transparent);
            this.resizeBrowser();
        }
    }

    private int mouseX(double x) {
        return (int)((x - (double)20.0F) * this.minecraft.getWindow().getGuiScale());
    }

    private int mouseY(double y) {
        return (int)((y - (double)20.0F) * this.minecraft.getWindow().getGuiScale());
    }

    private int scaleX(double x) {
        return (int)((x - (double)40.0F) * this.minecraft.getWindow().getGuiScale());
    }

    private int scaleY(double y) {
        return (int)((y - (double)40.0F) * this.minecraft.getWindow().getGuiScale());
    }

    private void resizeBrowser() {
        if (this.width > 100 && this.height > 100) {
            this.browser.resize(this.scaleX((double)this.width), this.scaleY((double)this.height));
        }

    }

    public void resize(Minecraft minecraft, int i, int j) {
        super.resize(minecraft, i, j);
        this.resizeBrowser();
    }

    public void onClose() {
        this.browser.close();
        super.onClose();
    }

    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, this.browser.getRenderer().getTextureID());
        Tesselator t = Tesselator.getInstance();
        BufferBuilder buffer = t.getBuilder();
        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.vertex((double)20.0F, (double)(this.height - 20), (double)0.0F).uv(0.0F, 1.0F).color(255, 255, 255, 255).endVertex();
        buffer.vertex((double)(this.width - 20), (double)(this.height - 20), (double)0.0F).uv(1.0F, 1.0F).color(255, 255, 255, 255).endVertex();
        buffer.vertex((double)(this.width - 20), (double)20.0F, (double)0.0F).uv(1.0F, 0.0F).color(255, 255, 255, 255).endVertex();
        buffer.vertex((double)20.0F, (double)20.0F, (double)0.0F).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
        t.end();
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableDepthTest();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.browser.sendMousePress(this.mouseX(mouseX), this.mouseY(mouseY), button);
        this.browser.setFocus(true);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.browser.sendMouseRelease(this.mouseX(mouseX), this.mouseY(mouseY), button);
        this.browser.setFocus(true);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public void mouseMoved(double mouseX, double mouseY) {
        this.browser.sendMouseMove(this.mouseX(mouseX), this.mouseY(mouseY));
        super.mouseMoved(mouseX, mouseY);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.browser.sendMouseWheel(this.mouseX(mouseX), this.mouseY(mouseY), delta, 0);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        this.browser.sendKeyPress(keyCode, (long)scanCode, modifiers);
        this.browser.setFocus(true);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        this.browser.sendKeyRelease(keyCode, (long)scanCode, modifiers);
        this.browser.setFocus(true);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (codePoint == 0) {
            return false;
        } else {
            this.browser.sendKeyTyped(codePoint, modifiers);
            this.browser.setFocus(true);
            return super.charTyped(codePoint, modifiers);
        }
    }

}

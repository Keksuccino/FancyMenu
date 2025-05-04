package de.keksuccino.fancymenu.util.mcef;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings("unused")
public class WrappedMCEFBrowser extends AbstractWidget implements Closeable, FancyMenuUiComponent, NavigatableWidget {

    protected static final Logger LOGGER = LogManager.getLogger();

    protected final MCEFBrowser browser;
    protected final Minecraft minecraft = Minecraft.getInstance();
    protected boolean browserFocused = false;
    protected boolean interactable = true;
    protected float opacity = 1.0F;
    protected boolean autoHandle = true;
    protected volatile float volume = 1.0F;
    protected volatile boolean fullscreenAllVideos = false;
    protected volatile boolean autoPlayAllVideosOnLoad = true;
    protected volatile boolean muteAllMediaOnLoad = false;
    protected volatile boolean loopAllVideos = false;
    protected volatile boolean hideVideoControls = false;
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

        this.setVolume(this.volume);
        this.setSize(200, 200);
        this.setPosition(0, 0);

        MCEF.getClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                //Update video/audio stuff after loading completed
                WrappedMCEFBrowser.this.setVolume(WrappedMCEFBrowser.this.volume);
                WrappedMCEFBrowser.this.setFullscreenAllVideos(WrappedMCEFBrowser.this.fullscreenAllVideos);
                WrappedMCEFBrowser.this.setAutoPlayAllVideosOnLoad(WrappedMCEFBrowser.this.autoPlayAllVideosOnLoad);
                WrappedMCEFBrowser.this.setMuteAllMediaOnLoad(WrappedMCEFBrowser.this.muteAllMediaOnLoad);
                WrappedMCEFBrowser.this.setLoopAllVideos(WrappedMCEFBrowser.this.loopAllVideos);
                WrappedMCEFBrowser.this.setHideVideoControls(WrappedMCEFBrowser.this.hideVideoControls);
                super.onLoadEnd(browser, frame, httpStatusCode);
            }
        });

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

    public void onVolumeUpdated(@NotNull SoundSource soundSource, float newVolume) {
        this.setVolume(this.volume);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOver(mouseX, mouseY) && this.interactable) {
            this.browserFocused = true;
            this.browser.sendMousePress(this.convertMouseX(mouseX), this.convertMouseY(mouseY), button);
            this.browser.setFocus(true);
            return true;
        } else {
            this.browserFocused = false;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.interactable) {
            this.browser.sendMouseRelease(this.convertMouseX(mouseX), this.convertMouseY(mouseY), button);
            this.browser.setFocus(true);
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!this.interactable) return;
        this.browser.sendMouseMove(this.convertMouseX(mouseX), this.convertMouseY(mouseY));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isMouseOver(mouseX, mouseY) && this.interactable) {
            this.browser.sendMouseWheel(this.convertMouseX(mouseX), this.convertMouseY(mouseY), scrollY, 0);
            this.browser.setFocus(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.interactable && this.browserFocused) {
            this.browser.sendKeyPress(keyCode, scanCode, modifiers);
            this.browser.setFocus(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (this.interactable && this.browserFocused) {
            this.browser.sendKeyRelease(keyCode, scanCode, modifiers);
            this.browser.setFocus(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.interactable && this.browserFocused) {
            if (codePoint == (char) 0) return true;
            this.browser.sendKeyTyped(codePoint, modifiers);
            this.browser.setFocus(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return UIBase.isXYInArea(mouseX, mouseY, this.getX(), this.getY(), this.getWidth(), this.getHeight());
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

    /**
     * @param volume Value between 0.0 and 1.0
     */
    public void setVolume(float volume) {
        this.volume = volume;
        String code = "document.querySelectorAll('audio, video').forEach(el => el.volume = " + this.getActualVolume() + ");";
        this.browser.executeJavaScript(code, this.browser.getURL(), 0);
    }

    public float getVolume() {
        return this.volume;
    }

    public float getActualVolume() {
        float actualVolume = this.volume;
        float soundSourceVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
        actualVolume *= soundSourceVolume;
        return actualVolume;
    }

    public void setInteractable(boolean interactable) {
        this.interactable = interactable;
        if (!this.interactable) {
            this.browser.setFocus(false);
            this.browserFocused = false;
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

    public void setFullscreenAllVideos(boolean fullscreenAllVideos) {
        this.fullscreenAllVideos = fullscreenAllVideos;
        String code = """
                document.querySelectorAll('video').forEach(video => {
                    if (video.requestFullscreen) {
                        video.requestFullscreen().catch(err => console.error('Fullscreen error:', err));
                    } else if (video.webkitRequestFullscreen) { // Safari compatibility
                        video.webkitRequestFullscreen().catch(err => console.error('Fullscreen error (webkit):', err));
                    } else if (video.msRequestFullscreen) { // IE/Edge compatibility
                        video.msRequestFullscreen().catch(err => console.error('Fullscreen error (ms):', err));
                    }
                });
                """;
        if (this.fullscreenAllVideos) this.browser.executeJavaScript(code, this.browser.getURL(), 0);
    }

    public boolean isFullscreenAllVideos() {
        return fullscreenAllVideos;
    }

    public void setAutoPlayAllVideosOnLoad(boolean autoPlayAllVideosOnLoad) {
        this.autoPlayAllVideosOnLoad = autoPlayAllVideosOnLoad;
        String code = """
                document.querySelectorAll('video').forEach(video => {
                    video.play(); // Start playing the video
                });
                """;
        if (this.autoPlayAllVideosOnLoad) this.browser.executeJavaScript(code, this.browser.getURL(), 0);
    }

    public boolean isAutoPlayAllVideosOnLoad() {
        return autoPlayAllVideosOnLoad;
    }

    public void setMuteAllMediaOnLoad(boolean muteAllMediaOnLoad) {
        this.muteAllMediaOnLoad = muteAllMediaOnLoad;
        String code = """
                document.querySelectorAll('audio, video').forEach(media => {
                    media.muted = %muted%; // Mute media
                });
                """.replace("%muted%", "" + this.muteAllMediaOnLoad);
        this.browser.executeJavaScript(code, this.browser.getURL(), 0);
    }

    public boolean isMuteAllMediaOnLoad() {
        return muteAllMediaOnLoad;
    }

    public void setLoopAllVideos(boolean loopAllVideos) {
        this.loopAllVideos = loopAllVideos;
        String code = """
                document.querySelectorAll('video').forEach(video => {
                    video.loop = %loop%; // Set video to loop
                });
                """.replace("%loop%", "" + this.loopAllVideos);
        this.browser.executeJavaScript(code, this.browser.getURL(), 0);
    }

    public boolean isLoopAllVideos() {
        return loopAllVideos;
    }

    public void setHideVideoControls(boolean hideVideoControls) {
        this.hideVideoControls = hideVideoControls;
        String codeRemove = """
                document.querySelectorAll('video').forEach(video => {
                    video.removeAttribute('controls'); // Hide video controls
                });
                """;
        String codeAdd = """
                document.querySelectorAll('video').forEach(video => {
                    if (!video.hasAttribute('controls')) {
                        video.setAttribute('controls', 'controls'); // Add controls
                    }
                });
                """;
        this.browser.executeJavaScript(this.hideVideoControls ? codeRemove : codeAdd, this.browser.getURL(), 0);
    }

    public boolean isHideVideoControls() {
        return hideVideoControls;
    }

    public void goBack() {
        if (this.browser.canGoBack()) this.browser.goBack();
        this.setVolume(this.volume);
    }

    public void goForward() {
        if (this.browser.canGoForward()) this.browser.goForward();
        this.setVolume(this.volume);
    }

    public String getUrl() {
        return this.browser.getURL();
    }

    public void setUrl(@NotNull String url) {
        this.browser.loadURL(url);
    }

    public void reload() {
        this.browser.reload();
        this.setVolume(this.volume);
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
    public boolean isFocusable() {
        return false;
    }

    @Override
    public void setFocusable(boolean focusable) {
    }

    @Override
    public boolean isNavigatable() {
        return false;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
    }

    @Override
    public void close() throws IOException {
        this.browser.close(true);
    }

}

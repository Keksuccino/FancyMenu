package de.keksuccino.fancymenu.util.mcef;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class WrappedMCEFBrowser extends AbstractWidget implements Closeable, NavigatableWidget {

    protected static final Logger LOGGER = LogManager.getLogger();
    protected static final ScheduledExecutorService EXECUTOR = Executors. newSingleThreadScheduledExecutor();

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
    protected final UUID genericIdentifier = UUID.randomUUID();
    protected final ResourceLocation frameLocation = ResourceLocation.fromNamespaceAndPath("fancymenu", "mcef_browser_frame_texture_" + this.genericIdentifier.toString().toLowerCase().replace("-", ""));
    protected final BrowserFrameTexture frameTexture = new BrowserFrameTexture(-1, this.frameLocation.toString());

    // Track if initialization is complete for this browser
    private volatile boolean initialized = false;

    @NotNull
    public static WrappedMCEFBrowser build(@NotNull String url, boolean transparent, boolean autoHandle, @Nullable Consumer<Boolean> loadListener) {
        WrappedMCEFBrowser b = new WrappedMCEFBrowser(url, transparent, loadListener);
        b.autoHandle = autoHandle;
        return b;
    }

    @NotNull
    public static WrappedMCEFBrowser build(@NotNull String url, boolean transparent, boolean autoHandle, int x, int y, int width, int height, @Nullable Consumer<Boolean> loadListener) {
        WrappedMCEFBrowser b = build(url, transparent, autoHandle, loadListener);
        b.setSize(width, height);
        b.setPosition(x, y);
        return b;
    }

    protected WrappedMCEFBrowser(@NotNull String url, boolean transparent, @Nullable Consumer<Boolean> loadListener) {

        super(0, 0, 0, 0, Component.empty());

        // Register the custom load listener handler to later register multiple load listeners.
        // Calling this method multiple times is fine, because there can only be one default listener active.
        MCEF.getClient().addLoadHandler(BrowserLoadEventListenerManager.getInstance().getGlobalHandler());

        this.browser = MCEF.createBrowser(url, transparent);

        String browserId = this.getIdentifier();

        BrowserLoadEventListenerManager.getInstance().registerListenerForBrowser(this, success -> {
            if (success) {
                initialized = true;
                // Apply settings once the page is loaded
                applyInitialSettings();
            } else {
                LOGGER.error("[FANCYMENU] WrappedMCEFBrowser browser page failed to load (ID: {})", browserId, new Exception());
                initialized = false;
            }
        });

        if (loadListener != null) {
            BrowserLoadEventListenerManager.getInstance().registerListenerForBrowser(this, loadListener);
        }

        this.setVolume(this.volume);
        this.setSize(200, 200);
        this.setPosition(0, 0);

        this.updateFrameTexture();

        Minecraft.getInstance().getTextureManager().register(this.frameLocation, this.frameTexture);

    }

    /**
     * Apply all initial settings once the page is loaded
     */
    protected void applyInitialSettings() {
        this.setVolume(this.volume);
        this.setLoopAllVideos(this.loopAllVideos);
        this.setHideVideoControls(this.hideVideoControls);
        this.setAutoPlayAllVideosOnLoad(this.autoPlayAllVideosOnLoad);
        this.setMuteAllMediaOnLoad(this.muteAllMediaOnLoad);
    }

    protected void updateFrameTexture() {
        this.frameTexture.setId(this.browser.getRenderer().getTextureID());
        this.frameTexture.setWidth(this.getWidth());
        this.frameTexture.setHeight(this.getHeight());
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        try {

            this.updateFrameTexture();

            if (this.autoHandle) BrowserHandler.notifyHandler(this.genericIdentifier.toString(), this);

            graphics.blit(RenderPipelines.GUI_TEXTURED, this.frameLocation, this.getX(), this.getY(), 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight(), ARGB.white(this.opacity));

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

    @Override
    public void setWidth(int width) {
        this.width = width;
        this.setSize(this.width, this.height);
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
        this.setSize(this.width, this.height);
    }

    protected int convertMouseX(double mouseX) {
        return (int)((mouseX - (double)this.getX()) * WindowHandler.getGuiScale());
    }

    protected int convertMouseY(double mouseY) {
        return (int)((mouseY - (double)this.getY()) * WindowHandler.getGuiScale());
    }

    protected int convertWidth(double width) {
        return (int) (width * WindowHandler.getGuiScale());
    }

    protected int convertHeight(double height) {
        return (int) (height * WindowHandler.getGuiScale());
    }

    /**
     * @param volume Value between 0.0 and 1.0
     */
    public void setVolume(float volume) {
        this.volume = volume;
        if (initialized) {
            String code = "document.querySelectorAll('audio, video').forEach(el => el.volume = " + this.getActualVolume() + ");";
            this.browser.executeJavaScript(code, this.browser.getURL(), 0);
        }
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
        if (initialized) {
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
    }

    public boolean isFullscreenAllVideos() {
        return fullscreenAllVideos;
    }

    public void setAutoPlayAllVideosOnLoad(boolean autoPlayAllVideosOnLoad) {
        this.autoPlayAllVideosOnLoad = autoPlayAllVideosOnLoad;
        if (initialized) {
            String code = """
                    document.querySelectorAll('video').forEach(video => {
                        video.play(); // Start playing the video
                    });
                    """;
            if (this.autoPlayAllVideosOnLoad) this.browser.executeJavaScript(code, this.browser.getURL(), 0);
        }
    }

    public boolean isAutoPlayAllVideosOnLoad() {
        return autoPlayAllVideosOnLoad;
    }

    public void setMuteAllMediaOnLoad(boolean muteAllMediaOnLoad) {
        this.muteAllMediaOnLoad = muteAllMediaOnLoad;
        if (initialized) {
            String code = """
                    document.querySelectorAll('audio, video').forEach(media => {
                        media.muted = %muted%; // Mute media
                    });
                    """.replace("%muted%", "" + this.muteAllMediaOnLoad);
            this.browser.executeJavaScript(code, this.browser.getURL(), 0);
        }
    }

    public boolean isMuteAllMediaOnLoad() {
        return muteAllMediaOnLoad;
    }

    public void setLoopAllVideos(boolean loopAllVideos) {
        this.loopAllVideos = loopAllVideos;
        if (initialized) {
            String code = """
                    document.querySelectorAll('video').forEach(video => {
                        video.loop = %loop%; // Set video to loop
                    });
                    """.replace("%loop%", "" + this.loopAllVideos);
            this.browser.executeJavaScript(code, this.browser.getURL(), 0);
        }
    }

    public boolean isLoopAllVideos() {
        return loopAllVideos;
    }

    public void setHideVideoControls(boolean hideVideoControls) {
        this.hideVideoControls = hideVideoControls;
        if (initialized) {
            // More aggressive approach to hiding controls
            String codeRemove = """
                    document.querySelectorAll('video').forEach(video => {
                        // Multiple methods to ensure controls are hidden
                        video.removeAttribute('controls');
                        video.setAttribute('nocontrols', '');
                        video.setAttribute('controlslist', 'nodownload nofullscreen noremoteplayback');
                        video.controls = false;
                        
                        // Add style to hide controls
                        const style = document.createElement('style');
                        style.textContent = `
                            video::-webkit-media-controls,
                            video::-webkit-media-controls-enclosure,
                            video::-webkit-media-controls-panel,
                            video::-webkit-media-controls-panel-container,
                            video::-webkit-media-controls-play-button,
                            video::-webkit-media-controls-overlay-play-button {
                                display: none !important;
                                opacity: 0 !important;
                                pointer-events: none !important;
                            }
                        `;
                        if (!document.head.querySelector('style#hide-video-controls')) {
                            style.id = 'hide-video-controls';
                            document.head.appendChild(style);
                        }
                    });
                    """;
            String codeAdd = """
                    document.querySelectorAll('video').forEach(video => {
                        if (!video.hasAttribute('controls')) {
                            video.setAttribute('controls', 'controls'); // Add controls
                        }
                        video.removeAttribute('nocontrols');
                        // Remove style if it exists
                        const style = document.head.querySelector('style#hide-video-controls');
                        if (style) {
                            document.head.removeChild(style);
                        }
                    });
                    """;
            this.browser.executeJavaScript(this.hideVideoControls ? codeRemove : codeAdd, this.browser.getURL(), 0);
        }
    }

    public boolean isHideVideoControls() {
        return hideVideoControls;
    }

    public void goBack() {
        if (this.browser.canGoBack()) this.browser.goBack();
        if (initialized) {
            this.setVolume(this.volume);
        }
    }

    public void goForward() {
        if (this.browser.canGoForward()) this.browser.goForward();
        if (initialized) {
            this.setVolume(this.volume);
        }
    }

    public String getUrl() {
        return this.browser.getURL();
    }

    public void setUrl(@NotNull String url) {
        this.browser.loadURL(url);
    }

    public void reload() {
        this.browser.reload();
        if (initialized) {
            this.setVolume(this.volume);
        }
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

    /**
     * Get the browser identifier
     *
     * @return The browser identifier
     */
    public String getIdentifier() {
        return this.genericIdentifier.toString();
    }

    @NotNull
    public ResourceLocation getFrameLocation() {
        this.updateFrameTexture();
        if (this.autoHandle) BrowserHandler.notifyHandler(this.genericIdentifier.toString(), this);
        return this.frameLocation;
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
        // Unregister from the global handler manager
        if (this.browser != null) {
            BrowserLoadEventListenerManager.getInstance().unregisterAllListenersForBrowser(this.getIdentifier());
            this.browser.close(true);
        }
        Minecraft.getInstance().getTextureManager().release(this.frameLocation);
    }

}
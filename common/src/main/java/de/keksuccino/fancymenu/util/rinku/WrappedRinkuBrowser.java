package de.keksuccino.fancymenu.util.rinku;

import de.keksuccino.rinku.Rinku;
import de.keksuccino.rinku.RinkuBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import de.keksuccino.fancymenu.util.rendering.ui.MouseButtonCaptureOwner;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.threading.FancyMenuExecutors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class WrappedRinkuBrowser extends AbstractWidget implements Closeable, NavigatableWidget, FancyMenuUiComponent, MouseButtonCaptureOwner {

    protected static final Logger LOGGER = LogManager.getLogger();
    protected static final ScheduledExecutorService EXECUTOR = FancyMenuExecutors.newSingleThreadScheduledExecutor("FancyMenu-WrappedRinkuBrowser");

    protected final RinkuBrowser browser;
    private final BrowserAudioMuteController audioMuteController;
    protected final Minecraft minecraft = Minecraft.getInstance();
    protected final AtomicLong mainFrameNavigationGeneration = new AtomicLong();
    protected final BrowserInputState inputState = new BrowserInputState();
    @Nullable protected volatile String expectedMainFrameUrl;
    protected boolean interactable = true;
    protected float opacity = 1.0F;
    protected boolean autoHandle = true;
    protected volatile float volume = 1.0F;
    protected volatile boolean fullscreenAllVideos = false;
    protected volatile boolean autoPlayAllVideosOnLoad = true;
    protected volatile boolean loopAllVideos = false;
    protected volatile boolean hideVideoControls = false;
    protected final UUID genericIdentifier = UUID.randomUUID();
    protected volatile boolean closed = false;
    
    // Track if initialization is complete for this browser
    private volatile boolean initialized = false;

    @NotNull
    public static WrappedRinkuBrowser build(@NotNull String url, boolean transparent, boolean autoHandle, @Nullable Consumer<Boolean> loadListener) {
        return build(url, transparent, autoHandle, false, loadListener);
    }

    @NotNull
    public static WrappedRinkuBrowser build(@NotNull String url, boolean transparent, boolean autoHandle, boolean muted, @Nullable Consumer<Boolean> loadListener) {
        WrappedRinkuBrowser b = new WrappedRinkuBrowser(url, transparent, muted, loadListener);
        b.autoHandle = autoHandle;
        return b;
    }

    @NotNull
    public static WrappedRinkuBrowser build(@NotNull String url, boolean transparent, boolean autoHandle, int x, int y, int width, int height, @Nullable Consumer<Boolean> loadListener) {
        WrappedRinkuBrowser b = build(url, transparent, autoHandle, loadListener);
        b.setSize(width, height);
        b.setPosition(x, y);
        return b;
    }

    protected WrappedRinkuBrowser(@NotNull String url, boolean transparent, boolean muted, @Nullable Consumer<Boolean> loadListener) {

        super(0, 0, 0, 0, Component.empty());

        this.expectedMainFrameUrl = url;

        // Initialize the global message router if not already done
        ActionBridge.initialize();
        
        // Register the custom load listener handler to later register multiple load listeners.
        // Calling this method multiple times is fine, because there can only be one default listener active.
        BrowserLoadEventListenerManager.getInstance().initialize();

        this.browser = Rinku.createBrowser(url, transparent);
        this.audioMuteController = new BrowserAudioMuteController(this.browser::setAudioMuted, muted);

        String browserId = this.getIdentifier();

        BrowserLoadEventListenerManager.getInstance().registerPersistentListenerForBrowser(this, success -> {
            if (success) {
                initialized = true;
                // Apply settings once the page is loaded
                applyInitialSettings();
                // Inject the FancyMenu JavaScript API
                injectJavaScriptAPI();
            } else {
                LOGGER.error("[FANCYMENU] WrappedRinkuBrowser browser page failed to load (ID: {})", browserId, new Exception());
                initialized = false;
            }
        });

        if (loadListener != null) {
            BrowserLoadEventListenerManager.getInstance().registerListenerForBrowser(this, loadListener);
        }

        this.setVolume(this.volume);
        this.setSize(200, 200);
        this.setPosition(0, 0);

    }

    protected WrappedRinkuBrowser(@NotNull String url, boolean transparent, @Nullable Consumer<Boolean> loadListener) {
        this(url, transparent, false, loadListener);
    }
    
    /**
     * Apply all initial settings once the page is loaded
     */
    protected void applyInitialSettings() {
        this.setVolume(this.volume);
        this.setLoopAllVideos(this.loopAllVideos);
        this.setHideVideoControls(this.hideVideoControls);
        this.setAutoPlayAllVideosOnLoad(this.autoPlayAllVideosOnLoad);
        this.audioMuteController.reapply();
    }
    
    /**
     * Injects the FancyMenu JavaScript API into the browser
     */
    protected void injectJavaScriptAPI() {
        try {
            LOGGER.info("[FANCYMENU] Injecting FancyMenu JavaScript API into browser (ID: {})", this.getIdentifier());
            long navigationGeneration = this.mainFrameNavigationGeneration.get();
            // Execute the JavaScript injection with a delay to ensure the page and message router are ready
            EXECUTOR.schedule(() -> {
                if (!shouldInjectJavaScript(this.closed, this.mainFrameNavigationGeneration.get(), navigationGeneration)) return;
                try {
                    this.browser.executeJavaScript(ActionBridge.JAVASCRIPT_API, this.browser.getURL(), 0);
                    LOGGER.info("[FANCYMENU] JavaScript API injection completed for browser (ID: {})", this.getIdentifier());
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to inject JavaScript API into browser", ex);
                }
            }, 500, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to schedule JavaScript API injection", ex);
        }
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.closed) {
            return;
        }

        try {
            if (this.autoHandle) BrowserHandler.notifyHandler(this.genericIdentifier.toString(), this);

            ResourceLocation frameLocation = this.browser.getTextureIdentifier();
            if (frameLocation == null) return;

            RenderSystem.enableBlend();

            graphics.setColor(1.0F, 1.0F, 1.0F, this.opacity);

            graphics.blit(frameLocation, this.getX(), this.getY(), 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());

            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to render RinkuBrowser!", ex);
        }

    }

    public void onVolumeUpdated(@NotNull SoundSource soundSource, float newVolume) {
        this.setVolume(this.volume);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = this.inputState.forwardMousePress(this.interactable, this.isMouseOver(mouseX, mouseY), button, () -> this.browser.sendMousePress(this.convertMouseX(mouseX), this.convertMouseY(mouseY), button));
        this.setFocused(handled);
        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.inputState.forwardMouseRelease(button, () -> {
            this.browser.sendMouseRelease(this.convertMouseX(mouseX), this.convertMouseY(mouseY), button);
            this.browser.setFocus(this.inputState.isFocused());
        });
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!this.interactable) return;
        this.browser.sendMouseMove(this.convertMouseX(mouseX), this.convertMouseY(mouseY));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return this.inputState.forwardMouseScroll(this.interactable, this.isMouseOver(mouseX, mouseY), () -> this.browser.sendMouseWheel(this.convertMouseX(mouseX), this.convertMouseY(mouseY), scrollY, 0));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.inputState.forwardKeyboardInput(this.interactable, () -> {
            this.browser.sendKeyPress(keyCode, scanCode, modifiers);
            this.browser.setFocus(true);
        });
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return this.inputState.forwardKeyboardInput(this.interactable, () -> {
            this.browser.sendKeyRelease(keyCode, scanCode, modifiers);
            this.browser.setFocus(true);
        });
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.inputState.forwardCharacterInput(this.interactable, codePoint, () -> {
            this.browser.sendKeyTyped(codePoint, modifiers);
            this.browser.setFocus(true);
        });
    }

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return this.interactable && UIBase.isXYInArea(mouseX, mouseY, this.getX(), this.getY(), this.getWidth(), this.getHeight());
	}

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public void setPosition(int x, int y) {
        super.setPosition(x, y);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        this.browser.resize(this.convertWidth(width), this.convertHeight(height));
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
        this.setSize(this.width, this.height);
    }

    public void setHeight(int height) {
        this.height = height;
        this.setSize(this.width, this.height);
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
        if (this.interactable == interactable) return;
        this.interactable = interactable;
        if (!this.interactable) {
            // Keep captured buttons until their matching releases, otherwise Chromium can retain a stuck pressed button.
            this.setFocused(false);
        }
    }

    public boolean isInteractable() {
        return this.interactable;
    }

    public void setBrowserFocused(boolean browserFocused) {
        this.setFocused(browserFocused);
    }

    public boolean isBrowserFocused() {
        return this.inputState.isFocused();
    }

    @Override
    public void setFocused(boolean focused) {
        boolean acceptedFocus = focused && this.interactable;
        super.setFocused(acceptedFocus);
        this.inputState.setFocused(acceptedFocus);
        if (!this.closed && (this.browser != null)) this.browser.setFocus(acceptedFocus);
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

    public void setMuted(boolean muted) {
        this.audioMuteController.setMuted(muted);
    }

    public boolean isMuted() {
        return this.audioMuteController.isMuted();
    }

    /**
     * @deprecated Use {@link #setMuted(boolean)}. The setting now mutes the complete Chromium browser instead of individual media elements.
     */
    @Deprecated(forRemoval = false)
    public void setMuteAllMediaOnLoad(boolean muted) {
        this.setMuted(muted);
    }

    /**
     * @deprecated Use {@link #isMuted()}.
     */
    @Deprecated(forRemoval = false)
    public boolean isMuteAllMediaOnLoad() {
        return this.isMuted();
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
        if (this.browser.canGoBack()) {
            this.mainFrameNavigationGeneration.incrementAndGet();
            this.browser.goBack();
        }
        if (initialized) {
            this.setVolume(this.volume);
        }
    }

    public void goForward() {
        if (this.browser.canGoForward()) {
            this.mainFrameNavigationGeneration.incrementAndGet();
            this.browser.goForward();
        }
        if (initialized) {
            this.setVolume(this.volume);
        }
    }

    public String getUrl() {
        return this.browser.getURL();
    }

    public void setUrl(@NotNull String url) {
        this.expectedMainFrameUrl = url;
        this.mainFrameNavigationGeneration.incrementAndGet();
        this.browser.loadURL(url);
    }

    void onMainFrameLoadStartedForTracking(@Nullable String url) {
        this.expectedMainFrameUrl = url;
        this.mainFrameNavigationGeneration.incrementAndGet();
        // CEF owns browser audio independently of the page DOM, so reapply here while the native browser is guaranteed to exist.
        this.audioMuteController.reapply();
    }

    static boolean shouldInjectJavaScript(boolean closed, long currentNavigationGeneration, long scheduledNavigationGeneration) {
        return !closed && currentNavigationGeneration == scheduledNavigationGeneration;
    }

    @Nullable
    String getExpectedMainFrameUrlForTracking() {
        return this.expectedMainFrameUrl;
    }

    public void reload() {
        this.expectedMainFrameUrl = this.browser.getURL();
        this.mainFrameNavigationGeneration.incrementAndGet();
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
    public RinkuBrowser getBrowser() {
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

    public boolean isClosed() {
        return this.closed;
    }

    @Nullable
    public ResourceLocation getFrameLocation() {
        if (this.autoHandle) BrowserHandler.notifyHandler(this.genericIdentifier.toString(), this);
        return this.browser.getTextureIdentifier();
    }

    @Override
    public boolean isFocusable() {
        // Pointer focus is required for Screen to route browser input, but isNavigatable() remains false to keep it out of tab/arrow navigation.
        return true;
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
        if (this.closed) return;
        this.setFocused(false);
        this.closed = true;
        this.inputState.reset();
        this.mainFrameNavigationGeneration.incrementAndGet();
        // Unregister from the global handler manager
        if (this.browser != null) {
            BrowserLoadEventListenerManager.getInstance().unregisterAllListenersForBrowser(this.getIdentifier());
            this.browser.close();
        }
    }

    @Override
    public boolean hasMouseButtonCapture(int button) {
        return this.inputState.hasMouseButtonCapture(button);
    }

}

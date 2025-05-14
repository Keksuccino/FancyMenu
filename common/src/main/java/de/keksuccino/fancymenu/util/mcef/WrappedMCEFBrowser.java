package de.keksuccino.fancymenu.util.mcef;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.rendering.video.VideoManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    protected UUID genericIdentifier = UUID.randomUUID();
    
    // Track if initialization is complete for this browser
    private volatile boolean initialized = false;
    private final CompletableFuture<Boolean> initFuture = new CompletableFuture<>();

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

        // Register with the global handler manager instead of creating our own
        if (this.browser != null && this.browser.getClient() != null) {
            final int browserId = this.browser.getIdentifier();
            
            LOGGER.debug("[FANCYMENU] WrappedMCEFBrowser browser ID: {}", browserId);
            
            // Check if the browser ID is valid (should be > 0)
            if (browserId <= 0) {
                LOGGER.error("[FANCYMENU] WrappedMCEFBrowser has invalid browser ID: {}. Falling back to manual initialization.", browserId);
                
                // We can't use the global handler, so we'll set up a direct initialization after a delay
                EXECUTOR.schedule(() -> {
                    if (!initFuture.isDone()) {
                        LOGGER.info("[FANCYMENU] WrappedMCEFBrowser manual initialization fallback completed");
                        initialized = true;
                        initFuture.complete(true);
                        applyInitialSettings();
                    }
                }, 2000, TimeUnit.MILLISECONDS);
                
                return; // Exit early since we can't register properly
            }
            
            // Register this browser with the global handler manager
            if (GlobalLoadHandlerManager.getInstance().registerBrowser(browserId, initFuture)) {
                LOGGER.info("[FANCYMENU] WrappedMCEFBrowser registered with global handler, browser ID: {}", browserId);
                
                // Make sure the global handler is registered with the CefClient
                // This only needs to happen once, but it's safe to call multiple times
                // as the CefClient will only set it if there's no handler yet
                this.browser.getClient().addLoadHandler(
                    GlobalLoadHandlerManager.getInstance().getGlobalHandler());
                
                // Listen for completion of the future
                initFuture.thenAccept(success -> {
                    if (success) {
                        LOGGER.debug("[FANCYMENU] WrappedMCEFBrowser browser page loaded successfully (ID: {})", browserId);
                        initialized = true;
                        
                        // Apply settings once the page is loaded
                        applyInitialSettings();
                    } else {
                        LOGGER.error("[FANCYMENU] WrappedMCEFBrowser browser page failed to load (ID: {})", browserId);
                        initialized = false;
                    }
                });
            } else {
                LOGGER.warn("[FANCYMENU] WrappedMCEFBrowser failed to register with global handler (ID: {}). Falling back to manual initialization.", browserId);
                
                // Fallback initialization after a delay if registration failed
                EXECUTOR.schedule(() -> {
                    if (!initFuture.isDone()) {
                        LOGGER.info("[FANCYMENU] WrappedMCEFBrowser manual initialization fallback completed");
                        initialized = true;
                        initFuture.complete(true);
                        applyInitialSettings();
                    }
                }, 2000, TimeUnit.MILLISECONDS);
            }
        } else {
            LOGGER.warn("[FANCYMENU] Could not attach to global load handler for WrappedMCEFBrowser. Settings on page load may be unreliable.");
            
            // Fallback for browser without client
            EXECUTOR.schedule(() -> {
                if (!initFuture.isDone()) {
                    LOGGER.info("[FANCYMENU] WrappedMCEFBrowser manual initialization fallback completed (no client)");
                    initialized = true;
                    initFuture.complete(true);
                    applyInitialSettings();
                }
            }, 2000, TimeUnit.MILLISECONDS);
        }
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

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        try {
            if (this.autoHandle) BrowserHandler.notifyHandler(this.genericIdentifier.toString(), this);

            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
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
    public int getIdentifier() {
        return this.browser.getIdentifier();
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
            int browserId = this.browser.getIdentifier();
            
            // Only unregister if the browser ID is valid
            if (browserId > 0) {
                GlobalLoadHandlerManager.getInstance().unregisterBrowser(browserId);
            }
            
            this.browser.close(true);
        }
    }
}
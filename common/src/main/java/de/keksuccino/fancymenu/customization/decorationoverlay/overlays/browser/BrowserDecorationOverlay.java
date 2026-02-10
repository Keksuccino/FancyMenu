package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.browser;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlay;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.mcef.BrowserHandler;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import de.keksuccino.fancymenu.util.mcef.WrappedMCEFBrowser;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

public class BrowserDecorationOverlay extends AbstractDecorationOverlay<BrowserDecorationOverlay> {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final DrawableColor ERROR_BACKGROUND_COLOR = DrawableColor.of(Color.RED);
    private static final DrawableColor EDITOR_PREVIEW_BACKGROUND_COLOR = DrawableColor.of(new Color(20, 25, 35));
    private static final DrawableColor EDITOR_PREVIEW_STRIPE_COLOR = DrawableColor.of(new Color(140, 170, 210));
    private static final String FALLBACK_URL = "about:blank";

    public final Property.StringProperty url = putProperty(Property.stringProperty("url", "https://docs.fancymenu.net", false, true, "fancymenu.decoration_overlays.browser.url"));
    public final Property<Boolean> consumeMouseClicks = putProperty(Property.booleanProperty("consume_mouse_clicks", true, "fancymenu.decoration_overlays.browser.consume_mouse_clicks"));
    public final Property<Boolean> consumeMouseScrolls = putProperty(Property.booleanProperty("consume_mouse_scrolls", true, "fancymenu.decoration_overlays.browser.consume_mouse_scrolls"));
    public final Property<Boolean> consumeKeyboardPresses = putProperty(Property.booleanProperty("consume_keyboard_presses", true, "fancymenu.decoration_overlays.browser.consume_keyboard_presses"));
    public final Property<Boolean> processMouseClicks = putProperty(Property.booleanProperty("process_mouse_clicks", true, "fancymenu.decoration_overlays.browser.process_mouse_clicks"));
    public final Property<Boolean> processMouseScrolls = putProperty(Property.booleanProperty("process_mouse_scrolls", true, "fancymenu.decoration_overlays.browser.process_mouse_scrolls"));
    public final Property<Boolean> processKeyboardPresses = putProperty(Property.booleanProperty("process_keyboard_presses", true, "fancymenu.decoration_overlays.browser.process_keyboard_presses"));
    public final Property<Boolean> hideVideoControls = putProperty(Property.booleanProperty("hide_video_controls", false, "fancymenu.decoration_overlays.browser.hide_video_controls"));
    public final Property<Boolean> loopVideos = putProperty(Property.booleanProperty("loop_videos", false, "fancymenu.decoration_overlays.browser.loop_videos"));
    public final Property<Boolean> muteMedia = putProperty(Property.booleanProperty("mute_media", false, "fancymenu.decoration_overlays.browser.mute_media"));
    public final Property.FloatProperty mediaVolume = putProperty(Property.floatProperty("media_volume", 1.0F, "fancymenu.decoration_overlays.browser.media_volume"));

    @Nullable
    private WrappedMCEFBrowser browser = null;
    @Nullable
    private Screen attachedScreen = null;
    @Nullable
    private String lastTickUrl = null;
    private int lastTickWidth = -1;
    private int lastTickHeight = -1;
    @Nullable
    private String registeredInputConsumerId = null;
    private boolean autoFocusPending = true;

    private final InputCaptureOverlay inputCaptureOverlay = new InputCaptureOverlay();

    public BrowserDecorationOverlay() {
        this.showOverlay.addValueSetListener((oldValue, newValue) -> {
            boolean nowEnabled = Boolean.TRUE.equals(newValue);
            this.autoFocusPending = nowEnabled;
            if (nowEnabled) {
                if (this.attachedScreen != null) {
                    if (this.isEditorScreenActive()) {
                        this.unregisterInputCaptureOverlay();
                        this.destroyBrowser();
                    } else {
                        this.ensureInputCaptureOverlayRegistered();
                        this.ensureBrowserCreated();
                        this.focusSelfAndBrowser();
                    }
                }
            } else {
                this.unregisterInputCaptureOverlay();
                if (this.browser != null) {
                    this.browser.setBrowserFocused(false);
                }
            }
        });
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.url.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.OPEN_IN_BROWSER)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.browser.url.desc")));

        menu.addSeparatorEntry("separator_before_input_consumption");

        this.consumeMouseClicks.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.MOUSE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.browser.consume_mouse_clicks.desc")));

        this.consumeMouseScrolls.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.SWIPE_VERTICAL)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.browser.consume_mouse_scrolls.desc")));

        this.consumeKeyboardPresses.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.KEYBOARD)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.browser.consume_keyboard_presses.desc")));

        menu.addSeparatorEntry("separator_between_input_consume_and_process");

        this.processMouseClicks.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.MOUSE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.browser.process_mouse_clicks.desc")));

        this.processMouseScrolls.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.SWIPE_VERTICAL)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.browser.process_mouse_scrolls.desc")));

        this.processKeyboardPresses.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.KEYBOARD)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.browser.process_keyboard_presses.desc")));

        menu.addSeparatorEntry("separator_after_input_consumption");

        this.hideVideoControls.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.VIDEO_SETTINGS)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.browser.hide_video_controls.desc")));

        this.loopVideos.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.REPEAT)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.browser.loop_videos.desc")));

        this.muteMedia.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.VOLUME_OFF)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.browser.mute_media.desc")));

        this.mediaVolume.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.VOLUME_UP)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.browser.media_volume.desc")));

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        int width = getScreenWidth();
        int height = getScreenHeight();
        if (this.isEditorScreenActive()) {
            this.unregisterInputCaptureOverlay();
            this.destroyBrowser();
            this.renderEditorPreview(graphics, width, height);
            return;
        }
        this.ensureBrowserCreated();
        this.ensureInputCaptureOverlayRegistered();
        if (this.autoFocusPending) {
            this.focusSelfAndBrowser();
            this.autoFocusPending = false;
        }
        if (this.browser != null) {
            RenderSystem.disableDepthTest();
            RenderingUtils.setDepthTestLocked(true);
            try {
                BrowserHandler.notifyHandler(this.getInstanceIdentifier(), this.browser);
                this.syncBrowserSettings(width, height);
                RenderSystem.enableBlend();
                this.browser.render(graphics, mouseX, mouseY, partial);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to render browser instance of BrowserDecorationOverlay!", ex);
            }
            RenderingUtils.setDepthTestLocked(false);
            return;
        }
        RenderSystem.enableBlend();
        graphics.fill(RenderType.guiOverlay(), 0, 0, width, height, ERROR_BACKGROUND_COLOR.getColorInt());
        graphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("fancymenu.decoration_overlays.browser.mcef_not_loaded.line_1").setStyle(Style.EMPTY.withBold(true)), width / 2, (height / 2) - Minecraft.getInstance().font.lineHeight - 2, -1);
        graphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("fancymenu.decoration_overlays.browser.mcef_not_loaded.line_2").setStyle(Style.EMPTY.withBold(true)), width / 2, (height / 2) + 2, -1);
    }

    @Override
    public void onScreenInitializedOrResized(@NotNull Screen screen, @NotNull List<AbstractElement> elements) {
        this.attachedScreen = screen;
        this.autoFocusPending = true;
        if (this.isEditorScreenActive()) {
            this.unregisterInputCaptureOverlay();
            this.destroyBrowser();
            return;
        }
        if (this.showOverlay.tryGetNonNullElse(false)) {
            this.ensureInputCaptureOverlayRegistered();
            this.ensureBrowserCreated();
        }
        if (this.browser != null && this.showOverlay.tryGetNonNullElse(false)) {
            this.browser.setPosition(0, 0);
            this.browser.setSize(screen.width, screen.height);
            this.lastTickWidth = screen.width;
            this.lastTickHeight = screen.height;
        }
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        this.attachedScreen = null;
        this.unregisterInputCaptureOverlay();
        this.destroyBrowser();
        CursorHandler.setClientTickCursor(CursorHandler.CURSOR_NORMAL);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.consumeMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.consumeMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.consumeMouseDragged(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        return this.consumeMouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.consumeKeyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return this.consumeKeyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.consumeCharTyped(codePoint, modifiers);
    }

    private void ensureBrowserCreated() {
        String instanceIdentifier = this.getInstanceIdentifier();
        WrappedMCEFBrowser wrappedBrowser = this.browser;
        if (wrappedBrowser != null && wrappedBrowser.isClosed()) {
            this.browser = null;
            wrappedBrowser = null;
        }
        if (wrappedBrowser != null) {
            BrowserHandler.notifyHandler(instanceIdentifier, wrappedBrowser);
            return;
        }
        if (!MCEFUtil.isMCEFLoaded() || !MCEFUtil.MCEF_initialized) {
            return;
        }
        wrappedBrowser = BrowserHandler.get(instanceIdentifier);
        if (wrappedBrowser != null && wrappedBrowser.isClosed()) {
            wrappedBrowser = null;
        }
        if (wrappedBrowser == null) {
            String resolvedUrl = this.url.getString();
            if (resolvedUrl == null || resolvedUrl.isBlank()) {
                resolvedUrl = FALLBACK_URL;
            }
            wrappedBrowser = WrappedMCEFBrowser.build(resolvedUrl, true, false, null);
        }
        this.browser = wrappedBrowser;
        BrowserHandler.notifyHandler(instanceIdentifier, wrappedBrowser);
        this.lastTickUrl = null;
        this.lastTickWidth = -1;
        this.lastTickHeight = -1;
        this.autoFocusPending = true;
    }

    private void destroyBrowser() {
        if (this.browser == null) {
            return;
        }
        BrowserHandler.remove(this.getInstanceIdentifier(), true);
        this.browser = null;
        this.lastTickUrl = null;
        this.lastTickWidth = -1;
        this.lastTickHeight = -1;
    }

    private void syncBrowserSettings(int width, int height) {
        WrappedMCEFBrowser wrappedBrowser = this.browser;
        if (wrappedBrowser == null) {
            return;
        }
        if (wrappedBrowser.isClosed()) {
            this.browser = null;
            return;
        }

        BrowserHandler.notifyHandler(this.getInstanceIdentifier(), wrappedBrowser);

        wrappedBrowser.setOpacity(1.0F);
        wrappedBrowser.setPosition(0, 0);

        if (this.lastTickWidth != width || this.lastTickHeight != height) {
            wrappedBrowser.setSize(width, height);
        }
        this.lastTickWidth = width;
        this.lastTickHeight = height;

        String finalUrl = this.url.getString();
        if (finalUrl == null || finalUrl.isBlank()) {
            finalUrl = FALLBACK_URL;
        }
        if (!Objects.equals(finalUrl, this.lastTickUrl)) {
            wrappedBrowser.setUrl(finalUrl);
            this.lastTickUrl = finalUrl;
        }

        boolean hideControls = this.hideVideoControls.tryGetNonNullElse(false);
        if (wrappedBrowser.isHideVideoControls() != hideControls) {
            wrappedBrowser.setHideVideoControls(hideControls);
        }

        boolean loopAllVideos = this.loopVideos.tryGetNonNullElse(false);
        if (wrappedBrowser.isLoopAllVideos() != loopAllVideos) {
            wrappedBrowser.setLoopAllVideos(loopAllVideos);
        }

        boolean muteOnLoad = this.muteMedia.tryGetNonNullElse(false);
        if (wrappedBrowser.isMuteAllMediaOnLoad() != muteOnLoad) {
            wrappedBrowser.setMuteAllMediaOnLoad(muteOnLoad);
        }

        float resolvedVolume = this.mediaVolume.getFloat();
        if (resolvedVolume > 1.0F) {
            resolvedVolume = 1.0F;
        } else if (resolvedVolume < 0.0F) {
            resolvedVolume = 0.0F;
        }
        if (wrappedBrowser.getVolume() != resolvedVolume) {
            wrappedBrowser.setVolume(resolvedVolume);
        }

        wrappedBrowser.setInteractable(true);
    }

    private boolean isInputCaptureActive() {
        if (!this.showOverlay.tryGetNonNullElse(false)) {
            return false;
        }
        if (this.attachedScreen == null) {
            return false;
        }
        if (this.isEditorScreenActive()) {
            return false;
        }
        return Minecraft.getInstance().screen == this.attachedScreen;
    }

    private void ensureInputCaptureOverlayRegistered() {
        if (this.attachedScreen == null) {
            return;
        }
        if (this.isEditorScreenActive()) {
            return;
        }
        String desiredId = this.getInstanceIdentifier();
        if (Objects.equals(this.registeredInputConsumerId, desiredId)) {
            return;
        }
        if (this.registeredInputConsumerId != null) {
            BrowserOverlayInputConsumers.unregister(this.registeredInputConsumerId);
        }
        BrowserOverlayInputConsumers.register(desiredId, this.inputCaptureOverlay);
        this.registeredInputConsumerId = desiredId;
    }

    private boolean isEditorScreenActive() {
        return this.attachedScreen instanceof LayoutEditorScreen;
    }

    private void renderEditorPreview(@NotNull GuiGraphics graphics, int width, int height) {
        RenderSystem.enableBlend();
        graphics.fill(RenderType.guiOverlay(), 0, 0, width, height, EDITOR_PREVIEW_BACKGROUND_COLOR.getColorIntWithAlpha(0.35F));

        graphics.pose().pushPose();
        graphics.pose().translate(width / 2.0F, height / 2.0F, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(-28.0F));
        graphics.pose().translate(-width / 2.0F, -height / 2.0F, 0.0F);

        int stripeWidth = 20;
        int stripeGap = 16;
        int startX = -height;
        int endX = width + height;
        int stripeColor = EDITOR_PREVIEW_STRIPE_COLOR.getColorIntWithAlpha(0.35F);
        for (int x = startX; x < endX; x += stripeWidth + stripeGap) {
            graphics.fill(x, -height, x + stripeWidth, height * 2, stripeColor);
        }

        graphics.pose().popPose();
    }

    private void unregisterInputCaptureOverlay() {
        if (this.registeredInputConsumerId == null) {
            return;
        }
        BrowserOverlayInputConsumers.unregister(this.registeredInputConsumerId);
        this.registeredInputConsumerId = null;
    }

    private void focusSelfAndBrowser() {
        if (!this.isInputCaptureActive()) {
            return;
        }
        Screen screen = this.attachedScreen;
        if (screen != null && screen.getFocused() != this) {
            screen.setFocused(this);
        }
        if (this.browser != null) {
            this.browser.setBrowserFocused(true);
        }
    }

    private boolean consumeMouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isInputCaptureActive()) {
            return false;
        }
        if (this.processMouseClicks.tryGetNonNullElse(true)) {
            this.focusSelfAndBrowser();
            WrappedMCEFBrowser wrappedBrowser = this.browser;
            if (wrappedBrowser != null) {
                wrappedBrowser.mouseClicked(mouseX, mouseY, button);
            }
        }
        return this.consumeMouseClicks.tryGetNonNullElse(true);
    }

    private boolean consumeMouseReleased(double mouseX, double mouseY, int button) {
        if (!this.isInputCaptureActive()) {
            return false;
        }
        if (this.processMouseClicks.tryGetNonNullElse(true)) {
            WrappedMCEFBrowser wrappedBrowser = this.browser;
            if (wrappedBrowser != null) {
                wrappedBrowser.mouseReleased(mouseX, mouseY, button);
            }
        }
        return this.consumeMouseClicks.tryGetNonNullElse(true);
    }

    private boolean consumeMouseDragged(double mouseX, double mouseY, int button) {
        if (!this.isInputCaptureActive()) {
            return false;
        }
        if (this.processMouseClicks.tryGetNonNullElse(true)) {
            WrappedMCEFBrowser wrappedBrowser = this.browser;
            if (wrappedBrowser != null) {
                wrappedBrowser.mouseMoved(mouseX, mouseY);
            }
        }
        return this.consumeMouseClicks.tryGetNonNullElse(true);
    }

    private boolean consumeMouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (!this.isInputCaptureActive()) {
            return false;
        }
        if (this.processMouseScrolls.tryGetNonNullElse(true)) {
            WrappedMCEFBrowser wrappedBrowser = this.browser;
            if (wrappedBrowser != null) {
                wrappedBrowser.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
            }
        }
        return this.consumeMouseScrolls.tryGetNonNullElse(true);
    }

    private boolean consumeKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isInputCaptureActive()) {
            return false;
        }
        if (this.processKeyboardPresses.tryGetNonNullElse(true)) {
            WrappedMCEFBrowser wrappedBrowser = this.browser;
            if (wrappedBrowser != null) {
                wrappedBrowser.setBrowserFocused(true);
                wrappedBrowser.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return this.consumeKeyboardPresses.tryGetNonNullElse(true);
    }

    private boolean consumeKeyReleased(int keyCode, int scanCode, int modifiers) {
        if (!this.isInputCaptureActive()) {
            return false;
        }
        if (this.processKeyboardPresses.tryGetNonNullElse(true)) {
            WrappedMCEFBrowser wrappedBrowser = this.browser;
            if (wrappedBrowser != null) {
                wrappedBrowser.setBrowserFocused(true);
                wrappedBrowser.keyReleased(keyCode, scanCode, modifiers);
            }
        }
        return this.consumeKeyboardPresses.tryGetNonNullElse(true);
    }

    private boolean consumeCharTyped(char codePoint, int modifiers) {
        if (!this.isInputCaptureActive()) {
            return false;
        }
        if (this.processKeyboardPresses.tryGetNonNullElse(true)) {
            WrappedMCEFBrowser wrappedBrowser = this.browser;
            if (wrappedBrowser != null) {
                wrappedBrowser.setBrowserFocused(true);
                wrappedBrowser.charTyped(codePoint, modifiers);
            }
        }
        return this.consumeKeyboardPresses.tryGetNonNullElse(true);
    }

    private class InputCaptureOverlay implements Renderable, GuiEventListener {

        private boolean focused = false;

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            // No visual output, this only captures input before vanilla screen handling.
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return consumeMouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return consumeMouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return consumeMouseDragged(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
            return consumeMouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return consumeKeyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
            return consumeKeyReleased(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            return consumeCharTyped(codePoint, modifiers);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return true;
        }

        @Override
        public void setFocused(boolean focused) {
            this.focused = focused;
        }

        @Override
        public boolean isFocused() {
            return this.focused;
        }
    }
}

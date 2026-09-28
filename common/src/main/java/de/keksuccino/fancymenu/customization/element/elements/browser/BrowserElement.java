package de.keksuccino.fancymenu.customization.element.elements.browser;

import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rinku.BrowserHandler;
import de.keksuccino.fancymenu.util.rinku.RinkuUtil;
import de.keksuccino.fancymenu.util.rinku.WrappedRinkuBrowser;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CustomizableScreen;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.List;

public class BrowserElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final DrawableColor ERROR_BACKGROUND_COLOR = DrawableColor.of(Color.RED);

    @NotNull
    public String url = "https://www.curseforge.com/minecraft";
    public boolean interactable = true;
    public boolean hideVideoControls = false;
    public boolean loopVideos = false;
    // Keep this public legacy name for add-on and serialized-layout compatibility; it now controls all browser audio.
    public boolean muteMedia = false;
    public final Property.FloatProperty mediaVolume = putProperty(Property.floatProperty("media_volume", 1.0F, "fancymenu.elements.browser.media_volume"));
    @Nullable
    public WrappedRinkuBrowser browser = null;
    public int lastTickWidth = -1;
    public int lastTickHeight = -1;
    public long lastLeftClickTime = -1;
    private final BrowserWidgetSlot<WrappedRinkuBrowser> browserWidget = new BrowserWidgetSlot<>(() -> RinkuUtil.isRinkuLoaded() && RinkuUtil.rinku_initialized, this::createBrowser);

    public BrowserElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.allowDepthTestManipulation = true;
    }

    @Override
    public void afterConstruction() {
        this.ensureBrowserCreated();
    }

    @NotNull
    private WrappedRinkuBrowser createBrowser() {
        WrappedRinkuBrowser wrappedBrowser = BrowserHandler.get(this.getInstanceIdentifier());
        if (wrappedBrowser == null || wrappedBrowser.isClosed()) {
            wrappedBrowser = WrappedRinkuBrowser.build(PlaceholderParser.replacePlaceholders(this.url), true, false, this.muteMedia, null);
        } else if (wrappedBrowser.isMuted() != this.muteMedia) {
            wrappedBrowser.setMuted(this.muteMedia);
        }
        // Creation can precede the first visible frame; never accept input until visibility has been resolved.
        wrappedBrowser.setInteractable(false);
        BrowserHandler.notifyHandler(this.getInstanceIdentifier(), wrappedBrowser);
        return wrappedBrowser;
    }

    private void ensureBrowserCreated() {
        try {
            this.browser = this.browserWidget.resolve();
        } catch (RuntimeException ex) {
            LOGGER.error("[FANCYMENU] Failed to create browser element {}!", this.getInstanceIdentifier(), ex);
        }
    }

    @Override
    public void renderTick_Inner_Stage_2() {
        super.renderTick_Inner_Stage_2();
        // Rinku can finish initializing after the title screen. Retry on the render thread without rebuilding the
        // screen or registering callbacks that could outlive this element after a resize/close.
        if (this.shouldRender()) this.ensureBrowserCreated();
        if (this.browser != null) {
            if (!this.browserWidget.isAttached() && !isEditor()) {
                Screen screen = getScreen();
                if (screen != null) this.browserWidget.attach(((IMixinScreen)screen).getChildrenFancyMenu(), screen instanceof CustomizableScreen customizable ? customizable.removeOnInitChildrenFancyMenu() : null);
            }
            this.browser.setInteractable(isBrowserInputEnabled(this.shouldRender(), this.interactable, isEditor()));
        }
    }

    @Override
    public void onDestroyElement() {
        this.browserWidget.retire();
        if (this.browser != null) this.browser.setInteractable(false);
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        if ((closedScreen != null) && (newScreen != null)) {
            boolean bothCustomGuis = (closedScreen instanceof CustomGuiBaseScreen) && (newScreen instanceof CustomGuiBaseScreen);
            if ((closedScreen instanceof CustomGuiBaseScreen c1) && (newScreen instanceof CustomGuiBaseScreen c2) && c1.getIdentifier().equals(c2.getIdentifier())) return;
            if (!bothCustomGuis && (closedScreen.getClass() == newScreen.getClass())) return;
        }
        this.browserWidget.retire();
        if (this.browser != null) BrowserHandler.remove(this.getInstanceIdentifier(), true);
        this.browser = null;
        // Reset cursor in case the browser changed it
        CursorHandler.setClientTickCursor(CursorHandler.CURSOR_NORMAL);
    }

    @Override
    public @NotNull List<GuiEventListener> getWidgetsToRegister() {
        return List.of(this.browserWidget.getWidgetToRegister());
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();
            int w = this.getAbsoluteWidth();
            int h = this.getAbsoluteHeight();

            if (this.browser != null) {

                BrowserHandler.notifyHandler(this.getInstanceIdentifier(), this.browser);

                boolean mouseInside = UIBase.isXYInArea(mouseX, mouseY, x, y, w, h);

                if (!this.browser.isHideVideoControls() && this.hideVideoControls) this.browser.setHideVideoControls(true);
                if (this.browser.isHideVideoControls() && !this.hideVideoControls) this.browser.setHideVideoControls(false);

                if (!this.browser.isLoopAllVideos() && this.loopVideos) this.browser.setLoopAllVideos(true);
                if (this.browser.isLoopAllVideos() && !this.loopVideos) this.browser.setLoopAllVideos(false);

                if (this.browser.isMuted() != this.muteMedia) this.browser.setMuted(this.muteMedia);

                float resolvedVolume = this.mediaVolume.getFloat();
                if (resolvedVolume > 1.0F) resolvedVolume = 1.0F;
                if (resolvedVolume < 0.0F) resolvedVolume = 0.0F;
                if (this.browser.getVolume() != resolvedVolume) this.browser.setVolume(resolvedVolume);

                this.browser.setOpacity(this.opacity);

                this.browser.setPosition(x, y);

                if ((this.lastTickWidth != w) || (this.lastTickHeight != h)) {
                    this.browser.setSize(w, h);
                }
                this.lastTickWidth = w;
                this.lastTickHeight = h;

                String finalUrl = PlaceholderParser.replacePlaceholders(this.url);
                if (!finalUrl.equals(this.getLastTickUrl())) {
                    this.browser.setUrl(finalUrl);
                    this.setLastTickUrl(finalUrl);
                }

                this.browser.extractRenderState(graphics, mouseX, mouseY, partial);

                //Render warning when trying to click browser in editor
                if (isEditor()) {
                    if (MouseInput.isLeftMouseDown() && mouseInside) {
                        this.lastLeftClickTime = System.currentTimeMillis();
                    }
                    if ((this.lastLeftClickTime + 5000) > System.currentTimeMillis()) {
                        graphics.fill(x, y, x + w, y + h, ERROR_BACKGROUND_COLOR.getColorIntWithAlpha(0.4F));
                        graphics.centeredText(Minecraft.getInstance().font, Component.translatable("fancymenu.elements.browser.disabled_in_editor").setStyle(Style.EMPTY.withBold(true)), x + (w / 2), y + (h / 2) - (Minecraft.getInstance().font.lineHeight / 2), -1);
                    }
                }

            } else {

                boolean rinkuLoaded = RinkuUtil.isRinkuLoaded();
                boolean failed = RinkuUtil.rinku_critical_failure || this.browserWidget.hasFailed();
                String statusKey = getBrowserStatusKey(rinkuLoaded, failed);
                int statusColor = rinkuLoaded && !failed ? DrawableColor.BLACK.getColorInt() : ERROR_BACKGROUND_COLOR.getColorInt();

                graphics.fill(x, y, x + w, y + h, statusColor);
                graphics.centeredText(Minecraft.getInstance().font, Component.translatable(statusKey + ".line_1").setStyle(Style.EMPTY.withBold(true)), x + (w / 2), y + (h / 2) - Minecraft.getInstance().font.lineHeight - 2, -1);
                graphics.centeredText(Minecraft.getInstance().font, Component.translatable(statusKey + ".line_2").setStyle(Style.EMPTY.withBold(true)), x + (w / 2), y + (h / 2) + 2, -1);

            }

        }

    }

    @Nullable
    public String getLastTickUrl() {
        return this.getMemory().getStringProperty("last_tick_url");
    }

    public void setLastTickUrl(@Nullable String url) {
        this.getMemory().putProperty("last_tick_url", url);
    }

    static String getBrowserStatusKey(boolean rinkuLoaded, boolean failed) {
        if (failed) return "fancymenu.elements.browser.rinku_failed";
        return rinkuLoaded ? "fancymenu.elements.browser.rinku_initializing" : "fancymenu.elements.browser.rinku_not_loaded";
    }

    static boolean isBrowserInputEnabled(boolean rendered, boolean interactable, boolean editor) {
        return rendered && interactable && !editor;
    }

}

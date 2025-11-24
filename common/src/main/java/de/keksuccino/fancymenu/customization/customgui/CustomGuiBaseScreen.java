package de.keksuccino.fancymenu.customization.customgui;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomGuiBaseScreen extends Screen {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final DrawableColor DARK_BACKGROUND = DrawableColor.BLACK;
    private static final float BACKGROUND_ALPHA = 0.4F;

    protected final CustomGui gui;
    protected final Screen overrideScreen;
    protected final Screen parentScreen;

    public CustomGuiBaseScreen(@NotNull CustomGui customGui, @Nullable Screen parentScreen, @Nullable Screen overrideScreen) {
        super(Component.empty());
        this.gui = customGui;
        this.overrideScreen = overrideScreen;
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        if (this.gui.popupMode) {
            this.resizePopupMenuBackgroundScreen(Minecraft.getInstance(), this.width, this.height);
        }
    }

    protected void resizePopupMenuBackgroundScreen(Minecraft minecraft, int width, int height) {
        try {
            if (this.parentScreen != null) {
                Screen current = Minecraft.getInstance().screen;
                Minecraft.getInstance().screen = this.parentScreen;
                this.parentScreen.resize(minecraft, width, height);
                Minecraft.getInstance().screen = current;
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to resize popup menu background screen of Custom GUI!", ex);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parentScreen);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.gui.allowEsc;
    }

    @Override
    public boolean isPauseScreen() {
        return this.gui.pauseGame;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this._renderBackground(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

        String title = PlaceholderParser.replacePlaceholders(this.getTitleString());
        Component titleComp = LocalizationUtils.isLocalizationKey(title) ? Component.translatable(title) : Component.literal(title);
        graphics.drawCenteredString(this.font, titleComp, this.width / 2, 8, -1);

    }

    protected void _renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        boolean popup = this.gui.popupMode && (this.parentScreen != null);
        boolean popupOverlay = popup && this.gui.popupModeBackgroundOverlay;
        if (popup) {
            this.renderPopupMenuBackgroundScreen(graphics, mouseX, mouseY, partial);
            this._renderBackgroundEvent(graphics, mouseX, mouseY, partial, false);
        } else {
            if ((Minecraft.getInstance().level == null) || !this.gui.worldBackground) {
                this.renderDirtBackground(graphics);
                this._renderBackgroundEvent(graphics, mouseX, mouseY, partial, true);
                return;
            }
        }
        if (popupOverlay) {
            this.renderDarkBackgroundOverlay(graphics);
        }
        if (!popup) {
            this.renderBackground(graphics);
            this._renderBackgroundEvent(graphics, mouseX, mouseY, partial, true);
        }
    }

    protected void _renderBackgroundEvent(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial, boolean blackBackground) {
        ScreenCustomizationLayer l = ScreenCustomizationLayerHandler.getLayerOfScreen(this);
        if (l != null) {
            if (!l.layoutBase.menuBackgrounds.isEmpty() && blackBackground) {
                RenderSystem.enableBlend();
                //Render a black background before the custom background gets rendered
                graphics.fill(0, 0, this.width, this.height, 0);
                RenderingUtils.resetShaderColor(graphics);
            }
        }
        EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(this, graphics, mouseX, mouseY, partial));
    }

    protected void renderDarkBackgroundOverlay(@NotNull GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, DARK_BACKGROUND.getColorIntWithAlpha(BACKGROUND_ALPHA));
    }

    protected void renderPopupMenuBackgroundScreen(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        RenderingUtils.setTooltipRenderingBlocked(true);
        Screen current = Minecraft.getInstance().screen;
        CustomGui.isCurrentlyRenderingPopupBackgroundScreen = true;
        try {
            Minecraft.getInstance().screen = this.parentScreen;
            // FancyMenu's render events get fired in renderWithTooltip, so they should fire here automatically
            this.parentScreen.renderWithTooltip(graphics, -500, -500, partial);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to render popup menu background screen of Custom GUI!", ex);
        }
        CustomGui.isCurrentlyRenderingPopupBackgroundScreen = false;
        Minecraft.getInstance().screen = current;
        RenderingUtils.setTooltipRenderingBlocked(false);
    }

    @NotNull
    public String getTitleString() {
        return this.gui.title;
    }

    @NotNull
    public String getIdentifier() {
        return this.gui.identifier;
    }

    @Nullable
    public Screen getOverriddenScreen() {
        return this.overrideScreen;
    }

    @NotNull
    public CustomGui getGuiMetadata() {
        return gui;
    }

    @Nullable
    public Screen getParentScreen() {
        return this.parentScreen;
    }

}
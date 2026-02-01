package de.keksuccino.fancymenu.customization.customgui;

import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenStartingEvent;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.GuiBlurRenderer;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer.ScrollScreenNormalizer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomGuiBaseScreen extends Screen {

    private static final Logger LOGGER = LogManager.getLogger();

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
                this.withPopupMenuBackgroundScreen(this.parentScreen, () -> {
                    boolean initialized = ((IMixinScreen)this.parentScreen).get_initialized_FancyMenu();
                    InitOrResizeScreenEvent.InitializationPhase phase = initialized ? InitOrResizeScreenEvent.InitializationPhase.RESIZE : InitOrResizeScreenEvent.InitializationPhase.INIT;
                    EventHandler.INSTANCE.postEvent(new InitOrResizeScreenStartingEvent(this.parentScreen, phase));
                    EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Pre(this.parentScreen, phase));
                    if (initialized) {
                        this.parentScreen.resize(minecraft, width, height);
                    } else {
                        this.parentScreen.init(minecraft, width, height);
                    }
                    ScrollScreenNormalizer.normalizeScrollableScreen(this.parentScreen);
                    EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Post(this.parentScreen, phase));
                    EventHandler.INSTANCE.postEvent(new InitOrResizeScreenCompletedEvent(this.parentScreen, phase));
                });
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

		super.render(graphics, mouseX, mouseY, partial);

		String title = PlaceholderParser.replacePlaceholders(this.getTitleString());
		Component titleComp = LocalizationUtils.isLocalizationKey(title) ? Component.translatable(title) : Component.literal(title);
		graphics.drawCenteredString(this.font, titleComp, this.width / 2, 8, -1);

	}

	@Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        boolean popup = this.gui.popupMode && (this.parentScreen != null);
        boolean popupOverlay = popup && this.gui.popupModeBackgroundOverlay;
        boolean renderWorldBackground = this.gui.worldBackground;
        boolean renderBackgroundOverlay = !renderWorldBackground || this.gui.worldBackgroundOverlay;
        if (popup) {
            this.renderPopupMenuBackgroundScreen(graphics, mouseX, mouseY, partial);
        } else {
            if ((Minecraft.getInstance().level == null) || !renderWorldBackground) {
                this.renderPanorama(graphics, partial);
            }
        }
        if (popup) {
            RenderingUtils.setOverrideBackgroundBlurRadius(7);
        }
        try {
            if ((!popup && renderBackgroundOverlay) || (popup && popupOverlay)) {
                this.renderGuiBlurBackground(graphics, partial);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Error while rendering background blur in Custom GUI!", ex);
        }
        RenderingUtils.resetOverrideBackgroundBlurRadius();
        if (!popup && renderBackgroundOverlay) {
            this.renderMenuBackground(graphics);
        }
    }

    protected void renderPopupMenuBackgroundScreen(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.parentScreen == null) return;
        if (this.gui.popupModeBackgroundOverlay) RenderingUtils.setVanillaMenuBlurringBlocked(true);
        RenderingUtils.setTooltipRenderingBlocked(true);
        CustomGui.isCurrentlyRenderingPopupBackgroundScreen = true;
        try {
            this.withPopupMenuBackgroundScreen(this.parentScreen, () -> {
                // FancyMenu's render events get fired in renderWithTooltip, so they should fire here automatically
                this.parentScreen.renderWithTooltip(graphics, -500, -500, partial);
            });
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to render popup menu background screen of Custom GUI!", ex);
        } finally {
            CustomGui.isCurrentlyRenderingPopupBackgroundScreen = false;
            RenderingUtils.setTooltipRenderingBlocked(false);
            RenderingUtils.setVanillaMenuBlurringBlocked(false);
        }
    }

    protected void renderGuiBlurBackground(@NotNull GuiGraphics graphics, float partial) {
        Minecraft minecraft = Minecraft.getInstance();
        int vanillaRadius = minecraft.options.getMenuBackgroundBlurriness();
        if (vanillaRadius < 1) {
            return;
        }
        double guiScale = minecraft.getWindow().getGuiScale();
        float blurRadius = guiScale > 0.0D ? (float) vanillaRadius / (float) guiScale : (float) vanillaRadius;
        GuiBlurRenderer.renderBlurArea(graphics, 0.0F, 0.0F, this.width, this.height, blurRadius, 0.0F, DrawableColor.FULLY_TRANSPARENT, partial);
    }

    protected void withPopupMenuBackgroundScreen(@NotNull Screen backgroundScreen, @NotNull Runnable action) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen current = minecraft.screen;
        minecraft.screen = backgroundScreen;
        try {
            action.run();
        } finally {
            minecraft.screen = current;
        }
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

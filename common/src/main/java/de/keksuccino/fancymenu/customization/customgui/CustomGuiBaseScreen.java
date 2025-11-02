package de.keksuccino.fancymenu.customization.customgui;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
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
                Screen current = Minecraft.getInstance().screen;
                Minecraft.getInstance().screen = this.parentScreen;
//                EventHandler.INSTANCE.postEvent(new InitOrResizeScreenStartingEvent(this.parentScreen, InitOrResizeScreenEvent.InitializationPhase.RESIZE));
//                EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Pre(this.parentScreen, InitOrResizeScreenEvent.InitializationPhase.RESIZE));
                this.parentScreen.resize(minecraft, width, height);
//                ScrollScreenNormalizer.normalizeScrollableScreen(this.parentScreen);
//                EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Post(this.parentScreen, InitOrResizeScreenEvent.InitializationPhase.RESIZE));
//                EventHandler.INSTANCE.postEvent(new InitOrResizeScreenCompletedEvent(this.parentScreen, InitOrResizeScreenEvent.InitializationPhase.RESIZE));
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

		if (this.gui.worldBackground) {
			this.renderBackground(graphics);
		} else {
			this.renderDirtBackground(graphics);
		}

		String title = PlaceholderParser.replacePlaceholders(this.getTitleString());
		Component titleComp = LocalizationUtils.isLocalizationKey(title) ? Component.translatable(title) : Component.literal(title);
		graphics.drawCenteredString(this.font, titleComp, this.width / 2, 8, -1);

		super.render(graphics, mouseX, mouseY, partial);

	}

	@Override
	public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        boolean popup = this.gui.popupMode && (this.parentScreen != null);
        boolean popupOverlay = popup && this.gui.popupModeBackgroundOverlay;
		if (popup) {
            this.renderPopupMenuBackgroundScreen(graphics, mouseX, mouseY, partial);
        } else {
            if ((Minecraft.getInstance().level == null) || !this.gui.worldBackground) {
                this.renderPanorama(graphics, partial);
            }
        }
        if (popup) {
            RenderingUtils.setOverrideBackgroundBlurRadius(7);
        }
		try {
            if (!popup || popupOverlay) {
                this.renderBlurredBackground(partial);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Error while rendering background blur in Custom GUI!", ex);
        }
        RenderingUtils.resetOverrideBackgroundBlurRadius();
		if (!popup) {
            this.renderMenuBackground(graphics);
        }
	}

    protected void renderPopupMenuBackgroundScreen(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.gui.popupModeBackgroundOverlay) RenderingUtils.setMenuBlurringBlocked(true);
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
        RenderingUtils.setMenuBlurringBlocked(false);
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

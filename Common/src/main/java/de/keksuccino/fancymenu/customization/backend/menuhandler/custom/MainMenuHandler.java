package de.keksuccino.fancymenu.customization.backend.menuhandler.custom;

import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationElement;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationItem;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationLayerRegistry;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.layers.titlescreen.splash.TitleScreenSplashElement;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.layers.titlescreen.splash.TitleScreenSplashItem;
import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.event.events.ButtonCacheUpdatedEvent;
import de.keksuccino.fancymenu.event.events.screen.RenderScreenBackgroundEvent;
import de.keksuccino.fancymenu.event.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.customization.MenuCustomization;
import de.keksuccino.fancymenu.event.events.MenuReloadEvent;
import de.keksuccino.fancymenu.customization.backend.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationLayer;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinScreen;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinTitleScreen;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainMenuHandler extends MenuHandlerBase {

	private static final Logger LOGGER = LogManager.getLogger();
	
	private static final CubeMap PANORAMA_CUBE_MAP = new CubeMap(new ResourceLocation("textures/gui/title/background/panorama"));
	private static final ResourceLocation PANORAMA_OVERLAY = new ResourceLocation("textures/gui/title/background/panorama_overlay.png");
	private static final ResourceLocation MINECRAFT_TITLE_TEXTURE = new ResourceLocation("textures/gui/title/minecraft.png");
	private static final ResourceLocation EDITION_TITLE_TEXTURE = new ResourceLocation("textures/gui/title/edition.png");
	private static final Random RANDOM = new Random();

	private final PanoramaRenderer panorama = new PanoramaRenderer(PANORAMA_CUBE_MAP);

	protected boolean showLogo = true;
	protected boolean showBranding = true;
	protected boolean showForgeNotificationCopyright = true;
	protected boolean showForgeNotificationTop = true;
	protected boolean showRealmsNotification = true;
	protected TitleScreenSplashItem splashItem = null;

	public MainMenuHandler() {
		super(TitleScreen.class.getName());
	}

	@Override
	public void onMenuReloaded(MenuReloadEvent e) {
		super.onMenuReloaded(e);

		TitleScreenSplashItem.cachedSplashText = null;
	}

	@Override
	public void onInitPre(InitOrResizeScreenEvent.Pre e) {
		if (this.shouldCustomize(e.getScreen())) {
			if (MenuCustomization.isMenuCustomizable(e.getScreen())) {
				if (e.getScreen() instanceof TitleScreen) {
					setShowFadeInAnimation(false, (TitleScreen) e.getScreen());
				}
			}
		}
		super.onInitPre(e);
	}

	@Override
	public void onButtonsCached(ButtonCacheUpdatedEvent e) {
		if (this.shouldCustomize(e.getScreen())) {
			if (MenuCustomization.isMenuCustomizable(e.getScreen())) {
				showLogo = true;
				showBranding = true;
				showForgeNotificationCopyright = true;
				showForgeNotificationTop = true;
				showRealmsNotification = true;
				DeepCustomizationLayer layer = DeepCustomizationLayerRegistry.getLayerByMenuIdentifier(this.getMenuIdentifier());
				if (layer != null) {
					TitleScreenSplashElement element = (TitleScreenSplashElement) layer.getElementByIdentifier("title_screen_splash");
					if (element != null) {
						splashItem = (TitleScreenSplashItem) element.constructDefaultItemInstance();
					}
				}
				super.onButtonsCached(e);
			}
		}
	}

	@Override
	protected void applyLayout(PropertiesSection sec, String renderOrder, ButtonCacheUpdatedEvent e) {

		super.applyLayout(sec, renderOrder, e);

		DeepCustomizationLayer layer = DeepCustomizationLayerRegistry.getLayerByMenuIdentifier(this.getMenuIdentifier());
		if (layer != null) {

			String action = sec.getEntryValue("action");
			if (action != null) {

				if (action.startsWith("deep_customization_element:")) {
					String elementId = action.split("[:]", 2)[1];
					DeepCustomizationElement element = layer.getElementByIdentifier(elementId);
					if (element != null) {
						DeepCustomizationItem i = element.constructCustomizedItemInstance(sec);
						if (i != null) {
							if (elementId.equals("title_screen_branding")) {
								if (this.showBranding) {
									this.showBranding = !(i.hidden);
								}
							}
							if (elementId.equals("title_screen_logo")) {
								if (this.showLogo) {
									this.showLogo = !(i.hidden);
								}
							}
							if (elementId.equals("title_screen_splash")) {
								if ((this.splashItem == null) || !this.splashItem.hidden) {
									this.splashItem = (TitleScreenSplashItem) i;
								}
							}
							if (elementId.equals("title_screen_realms_notification")) {
								if (this.showRealmsNotification) {
									this.showRealmsNotification = !(i.hidden);
								}
							}
							//Forge -------------->
							if (elementId.equals("title_screen_forge_copyright")) {
								if (this.showForgeNotificationCopyright) {
									this.showForgeNotificationCopyright = !(i.hidden);
								}
							}
							if (elementId.equals("title_screen_forge_top")) {
								if (this.showForgeNotificationTop) {
									this.showForgeNotificationTop = !(i.hidden);
								}
							}
						}
					}
				}

			}

		}

	}

	@EventListener
	public void onRender(RenderScreenEvent.Pre e) {
		if (this.shouldCustomize(e.getScreen())) {
			if (MenuCustomization.isMenuCustomizable(e.getScreen())) {
				e.setCanceled(true);
				e.getScreen().renderBackground(e.getPoseStack());
			}
		}
	}

	/**
	 * Mimic the original main menu to be able to customize it easier
	 */
	@Override
	public void drawToBackground(RenderScreenBackgroundEvent.Post e) {
		if (this.shouldCustomize(e.getScreen())) {
			Font font = Minecraft.getInstance().font;
			int width = e.getScreen().width;
			int j = width / 2 - 137;
			float minecraftLogoSpelling = RANDOM.nextFloat();
			int mouseX = MouseInput.getMouseX();
			int mouseY = MouseInput.getMouseY();
			PoseStack matrix = e.getPoseStack();

			RenderSystem.enableBlend();

			//Draw the panorama skybox and a semi-transparent overlay over it
			if (!this.canRenderBackground()) {
				this.panorama.render(Minecraft.getInstance().getDeltaFrameTime(), 1.0F);
				RenderUtils.bindTexture(PANORAMA_OVERLAY);
				RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				blit(matrix, 0, 0, e.getScreen().width, e.getScreen().height, 0.0F, 0.0F, 16, 128, 16, 128);
			}

			super.drawToBackground(e);

			if (this.showLogo) {
				RenderUtils.bindTexture(MINECRAFT_TITLE_TEXTURE);
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				if ((double) minecraftLogoSpelling < 1.0E-4D) {
					blit(matrix, j, 30, 0, 0, 99, 44);
					blit(matrix, j + 99, 30, 129, 0, 27, 44);
					blit(matrix, j + 99 + 26, 30, 126, 0, 3, 44);
					blit(matrix, j + 99 + 26 + 3, 30, 99, 0, 26, 44);
					blit(matrix, j + 155, 30, 0, 45, 155, 44);
				} else {
					blit(matrix, j, 30, 0, 0, 155, 44);
					blit(matrix, j + 155, 30, 0, 45, 155, 44);
				}

				RenderSystem.setShaderTexture(0, EDITION_TITLE_TEXTURE);
				blit(matrix, j + 88, 67, 0.0F, 0.0F, 98, 14, 128, 16);
			}

			Services.COMPAT.renderTitleScreenOverlay(matrix, font, e.getScreen(), this.showBranding, this.showForgeNotificationTop, this.showForgeNotificationCopyright);

			if (!PopupHandler.isPopupActive()) {
				this.renderButtons(e, mouseX, mouseY);
			}

			if (this.showRealmsNotification) {
				this.drawRealmsNotification(matrix, e.getScreen());
			}

			this.renderSplash(matrix, e.getScreen());

		}
	}

	protected void renderSplash(PoseStack matrix, Screen s) {

		try {
			if (this.splashItem != null) {
				this.splashItem.render(matrix, s);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void renderButtons(RenderScreenBackgroundEvent.Post e, int mouseX, int mouseY) {
		List<Renderable> buttons = ((IMixinScreen)e.getScreen()).getRenderablesFancyMenu();
		float partial = Minecraft.getInstance().getFrameTime();
		if (buttons != null) {
			for (Renderable button : buttons) {
				button.render(e.getPoseStack(), mouseX, mouseY, partial);
			}
		}
	}

	private void drawRealmsNotification(PoseStack matrix, Screen gui) {
		if (Minecraft.getInstance().options.realmsNotifications().get()) {
			Screen realms = ((IMixinTitleScreen)gui).getRealmsNotificationsScreenFancyMenu();
			if (realms != null) {
				realms.render(matrix, (int)Minecraft.getInstance().mouseHandler.xpos(), (int)Minecraft.getInstance().mouseHandler.ypos(), Minecraft.getInstance().getFrameTime());
			}
		}
	}

	protected static void setShowFadeInAnimation(boolean showFadeIn, TitleScreen s) {
		s.fading = showFadeIn;
	}

}

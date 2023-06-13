package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.compatibility.MinecraftCompatibilityUtils;
import de.keksuccino.fancymenu.events.*;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayer;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayerRegistry;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.splash.TitleScreenSplashElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.splash.TitleScreenSplashItem;
import de.keksuccino.fancymenu.mixin.client.IMixinScreen;
import de.keksuccino.konkrete.events.EventPriority;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.BackgroundDrawnEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.DrawScreenEvent.Post;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.CurrentScreenHandler;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("resource")
public class MainMenuHandler extends MenuHandlerBase {
	
	private static final CubeMap PANORAMA_CUBE_MAP = new CubeMap(new ResourceLocation("textures/gui/title/background/panorama"));
	private static final ResourceLocation PANORAMA_OVERLAY = new ResourceLocation("textures/gui/title/background/panorama_overlay.png");
//	private static final ResourceLocation MINECRAFT_TITLE_TEXTURE = new ResourceLocation("textures/gui/title/minecraft.png");
//	private static final ResourceLocation EDITION_TITLE_TEXTURE = new ResourceLocation("textures/gui/title/edition.png");
	private static final LogoRenderer LOGO_RENDERER = new LogoRenderer(false);
	private static final Random RANDOM = new Random();
	
	private PanoramaRenderer panorama = new PanoramaRenderer(PANORAMA_CUBE_MAP);

	protected boolean showLogo = true;
	protected boolean showBranding = true;
	protected boolean showRealmsNotification = true;
	protected TitleScreenSplashItem splashItem = null;
	
	public MainMenuHandler() {
		super(TitleScreen.class.getName());
	}

	@SubscribeEvent
	@Override
	public void onSoftReload(SoftMenuReloadEvent e) {
		super.onSoftReload(e);
	}
	
	@SubscribeEvent
	@Override
	public void onMenuReloaded(MenuReloadedEvent e) {
		super.onMenuReloaded(e);

		TitleScreenSplashItem.cachedSplashText = null;
	}

	@SubscribeEvent
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
	
	@SubscribeEvent
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui())) {
			if (MenuCustomization.isMenuCustomizable(e.getGui())) {

				showLogo = true;
				showBranding = true;
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
	protected void applyLayout(PropertiesSection sec, String renderOrder, ButtonCachedEvent e) {

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
						}
					}
				}

			}

		}

	}
	
	@SubscribeEvent
	@Override
	public void onRenderPost(Post e) {
		super.onRenderPost(e);
	}
	
	@SubscribeEvent
	public void onRender(GuiScreenEvent.DrawScreenEvent.Pre e) {
		if (this.shouldCustomize(e.getGui())) {
			if (MenuCustomization.isMenuCustomizable(e.getGui())) {
				e.setCanceled(true);
				e.getGui().renderBackground(e.getGuiGraphics());
			}
		}
	}
	
	/**
	 * Mimic the original main menu to be able to customize it easier
	 */
	@SubscribeEvent
	@Override
	public void drawToBackground(BackgroundDrawnEvent e) {
		
		if (this.shouldCustomize(e.getGui())) {
			
			Font font = Minecraft.getInstance().font;
			int width = e.getGui().width;
			int height = e.getGui().height;
			int j = width / 2 - 137;
			float minecraftLogoSpelling = RANDOM.nextFloat();
			int mouseX = MouseInput.getMouseX();
			int mouseY = MouseInput.getMouseY();
//			GuiGraphics graphics = CurrentScreenHandler.getMatrixStack();

			RenderSystem.enableBlend();

			//Draw the panorama skybox and a semi-transparent overlay over it
			if (!this.canRenderBackground()) {
				this.panorama.render(Minecraft.getInstance().getDeltaFrameTime(), 1.0F);
//				RenderUtils.bindTexture(PANORAMA_OVERLAY);
				RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				e.getGuiGraphics().blit(PANORAMA_OVERLAY, 0, 0, e.getGui().width, e.getGui().height, 0.0F, 0.0F, 16, 128, 16, 128);
			}
			
			super.drawToBackground(e);
			
			//Draw minecraft logo and edition textures if not disabled in the config
			if (this.showLogo) {
				LOGO_RENDERER.renderLogo(e.getGuiGraphics(), e.getScreen().width, 1.0F);
			}

			//Draw branding strings to the main menu if not disabled in the config
			if (this.showBranding) {
				String string = "Minecraft " + SharedConstants.getCurrentVersion().getName();
				if (Minecraft.getInstance().isDemo()) {
					string = string + " Demo";
				} else {
					string = string + ("release".equalsIgnoreCase(Minecraft.getInstance().getVersionType()) ? "" : "/" + Minecraft.getInstance().getVersionType());
				}

				if (Minecraft.checkModStatus().shouldReportAsModified()) {
					string = string + I18n.get("menu.modded");
				}

				e.getGuiGraphics().drawString(font, string, 2, e.getGui().height - 10, -1);
			}

			if (!PopupHandler.isPopupActive()) {
				this.renderButtons(e, mouseX, mouseY);
			}

			//Draw notification indicators to the "Realms" button if not disabled in the config
			if (this.showRealmsNotification) {
				this.drawRealmsNotification(e.getGuiGraphics(), e.getGui());
			}

			this.renderSplash(e.getGuiGraphics(), e.getGui());

		}
	}
	
	@SubscribeEvent
	@Override
	public void onButtonClickSound(PlayWidgetClickSoundEvent.Pre e) {
		super.onButtonClickSound(e);
	}
	
	@SubscribeEvent
	@Override
	public void onButtonRenderBackground(RenderWidgetBackgroundEvent.Pre e) {
		super.onButtonRenderBackground(e);
	}
	
	@SubscribeEvent
	@Override
	public void onRenderListBackground(RenderGuiListBackgroundEvent.Post e) {
		super.onRenderListBackground(e);
	}

	protected void renderSplash(GuiGraphics graphics, Screen s) {

		try {
			if (this.splashItem != null) {
				this.splashItem.render(graphics, s);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	private void renderButtons(GuiScreenEvent.BackgroundDrawnEvent e, int mouseX, int mouseY) {
		List<Renderable> buttons = this.getButtonList(e.getGui());
		float partial = Minecraft.getInstance().getFrameTime();
		
		if (buttons != null) {
			for(int i = 0; i < buttons.size(); ++i) {
				buttons.get(i).render(e.getGuiGraphics(), mouseX, mouseY, partial);
			}
		}
	}
	
	private void drawRealmsNotification(GuiGraphics graphics, Screen gui) {
		try {
			if (Minecraft.getInstance().options.realmsNotifications().get()) {
				Screen realms = MinecraftCompatibilityUtils.getTitleScreenRealmsNotificationsScreen((TitleScreen) gui);
				if (realms != null) {
					MouseHandler mh = Minecraft.getInstance().mouseHandler;
					realms.render(graphics, (int)mh.xpos(), (int)mh.ypos(), Minecraft.getInstance().getFrameTime());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private List<Renderable> getButtonList(Screen gui) {
		return ((IMixinScreen)gui).getRenderablesFancyMenu();
	}

	protected static void setShowFadeInAnimation(boolean showFadeIn, TitleScreen s) {
		s.fading = showFadeIn;
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onRenderPre(GuiScreenEvent.DrawScreenEvent.Pre e) {
		super.onRenderPre(e);
	}

}

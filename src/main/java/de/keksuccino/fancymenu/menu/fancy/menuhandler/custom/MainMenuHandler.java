package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.compatibility.MinecraftCompatibilityUtils;
import de.keksuccino.fancymenu.events.SoftMenuReloadEvent;
import de.keksuccino.fancymenu.events.PlayWidgetClickSoundEvent;
import de.keksuccino.fancymenu.events.RenderGuiListBackgroundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
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
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import de.keksuccino.konkrete.rendering.CurrentScreenHandler;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Widget;
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
	private static final ResourceLocation MINECRAFT_TITLE_TEXTURE = new ResourceLocation("textures/gui/title/minecraft.png");
	private static final ResourceLocation EDITION_TITLE_TEXTURE = new ResourceLocation("textures/gui/title/edition.png");
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
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if (this.shouldCustomize(e.getGui())) {
			if (MenuCustomization.isMenuCustomizable(e.getGui())) {
				if (e.getGui() instanceof TitleScreen) {
					setShowFadeInAnimation(false, (TitleScreen) e.getGui());
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
								this.showBranding = !(i.hidden);
							}
							if (elementId.equals("title_screen_logo")) {
								this.showLogo = !(i.hidden);
							}
							if (elementId.equals("title_screen_splash")) {
								this.splashItem = (TitleScreenSplashItem) i;
							}
							if (elementId.equals("title_screen_realms_notification")) {
								this.showRealmsNotification = !(i.hidden);
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
				e.getGui().renderBackground(e.getMatrixStack());
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
			PoseStack matrix = CurrentScreenHandler.getMatrixStack();
			
			RenderSystem.enableBlend();
			
			//Draw the panorama skybox and a semi-transparent overlay over it
			if (!this.canRenderBackground()) {
				this.panorama.render(Minecraft.getInstance().getDeltaFrameTime(), 1.0F);
				RenderUtils.bindTexture(PANORAMA_OVERLAY);
				RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				blit(matrix, 0, 0, e.getGui().width, e.getGui().height, 0.0F, 0.0F, 16, 128, 16, 128);
			}
			
			super.drawToBackground(e);
			
			//Draw minecraft logo and edition textures if not disabled in the config
			if (this.showLogo) {
				RenderUtils.bindTexture(MINECRAFT_TITLE_TEXTURE);
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				if ((double) minecraftLogoSpelling < 1.0E-4D) {
					e.getGui().blit(matrix, j + 0, 30, 0, 0, 99, 44);
					e.getGui().blit(matrix, j + 99, 30, 129, 0, 27, 44);
					e.getGui().blit(matrix, j + 99 + 26, 30, 126, 0, 3, 44);
					e.getGui().blit(matrix, j + 99 + 26 + 3, 30, 99, 0, 26, 44);
					e.getGui().blit(matrix, j + 155, 30, 0, 45, 155, 44);
				} else {
					e.getGui().blit(matrix, j + 0, 30, 0, 0, 155, 44);
					e.getGui().blit(matrix, j + 155, 30, 0, 45, 155, 44);
				}

				RenderSystem.setShaderTexture(0, EDITION_TITLE_TEXTURE);
				blit(matrix, j + 88, 67, 0.0F, 0.0F, 98, 14, 128, 16);
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
				
				drawString(e.getMatrixStack(), font, string, 2, e.getGui().height - 10, -1);
			}

			if (!PopupHandler.isPopupActive()) {
				this.renderButtons(e, mouseX, mouseY);
			}
			
			//Draw notification indicators to the "Realms" button if not disabled in the config
			if (this.showRealmsNotification) {
				this.drawRealmsNotification(matrix, e.getGui());
			}

			this.renderSplash(matrix, e.getGui());

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

	protected void renderSplash(PoseStack matrix, Screen s) {

		try {
			if (this.splashItem != null) {
				this.splashItem.render(matrix, s);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	private void renderButtons(GuiScreenEvent.BackgroundDrawnEvent e, int mouseX, int mouseY) {
		List<Widget> buttons = this.getButtonList(e.getGui());
		float partial = Minecraft.getInstance().getFrameTime();
		
		if (buttons != null) {
			for(int i = 0; i < buttons.size(); ++i) {
				buttons.get(i).render(CurrentScreenHandler.getMatrixStack(), mouseX, mouseY, partial);
			}
		}
	}
	
	private void drawRealmsNotification(PoseStack matrix, Screen gui) {
		try {
			if (Minecraft.getInstance().options.realmsNotifications().get()) {
				Screen realms = MinecraftCompatibilityUtils.getTitleScreenRealmsNotificationsScreen((TitleScreen) gui);
				if (realms != null) {
					MouseHandler mh = Minecraft.getInstance().mouseHandler;
					realms.render(matrix, (int)mh.xpos(), (int)mh.ypos(), Minecraft.getInstance().getFrameTime());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private List<Widget> getButtonList(Screen gui) {
		return ((IMixinScreen)gui).getRenderablesFancyMenu();
	}

	protected static void setShowFadeInAnimation(boolean showFadeIn, TitleScreen s) {
		try {
			Field f = ReflectionHelper.findField(TitleScreen.class, "fading", "field_18222");
			f.set(s, showFadeIn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onRenderPre(GuiScreenEvent.DrawScreenEvent.Pre e) {
		super.onRenderPre(e);
	}

}

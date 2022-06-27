package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import de.keksuccino.fancymenu.FancyMenu;
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
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import de.keksuccino.konkrete.rendering.CurrentScreenHandler;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ScreenEvent.BackgroundDrawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.internal.BrandingControl;

public class MainMenuHandler extends MenuHandlerBase {
	
	private static final CubeMap PANORAMA_CUBE_MAP = new CubeMap(new ResourceLocation("textures/gui/title/background/panorama"));
	private static final ResourceLocation PANORAMA_OVERLAY = new ResourceLocation("textures/gui/title/background/panorama_overlay.png");
	private static final ResourceLocation MINECRAFT_TITLE_TEXTURE = new ResourceLocation("textures/gui/title/minecraft.png");
	private static final ResourceLocation EDITION_TITLE_TEXTURE = new ResourceLocation("textures/gui/title/edition.png");
	private static final Random RANDOM = new Random();

	private PanoramaRenderer panorama = new PanoramaRenderer(PANORAMA_CUBE_MAP);

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
	public void onMenuReloaded(MenuReloadedEvent e) {
		super.onMenuReloaded(e);

		TitleScreenSplashItem.cachedSplashText = null;
	}

	@Override
	public void onInitPre(ScreenEvent.InitScreenEvent.Pre e) {
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
	public void onButtonsCached(ButtonCachedEvent e) {
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

							//Forge -------------->
							if (elementId.equals("title_screen_forge_copyright")) {
								this.showForgeNotificationCopyright = !(i.hidden);
							}
							if (elementId.equals("title_screen_forge_top")) {
								this.showForgeNotificationTop = !(i.hidden);
							}

						}
					}
				}

			}

		}

	}

	@SubscribeEvent
	public void onRender(ScreenEvent.DrawScreenEvent.Pre e) {
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
	public void drawToBackground(BackgroundDrawnEvent e) {
		if (this.shouldCustomize(e.getScreen())) {
			Font font = Minecraft.getInstance().font;
			int width = e.getScreen().width;
			int height = e.getScreen().height;
			int j = width / 2 - 137;
			float minecraftLogoSpelling = RANDOM.nextFloat();
			int mouseX = MouseInput.getMouseX();
			int mouseY = MouseInput.getMouseY();
			PoseStack matrix = CurrentScreenHandler.getPoseStack();

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

			//Draw minecraft logo and edition textures if not disabled in the config
			if (this.showLogo) {
				RenderUtils.bindTexture(MINECRAFT_TITLE_TEXTURE);
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				if ((double) minecraftLogoSpelling < 1.0E-4D) {
					blit(matrix, j + 0, 30, 0, 0, 99, 44);
					blit(matrix, j + 99, 30, 129, 0, 27, 44);
					blit(matrix, j + 99 + 26, 30, 126, 0, 3, 44);
					blit(matrix, j + 99 + 26 + 3, 30, 99, 0, 26, 44);
					blit(matrix, j + 155, 30, 0, 45, 155, 44);
				} else {
					blit(matrix, j + 0, 30, 0, 0, 155, 44);
					blit(matrix, j + 155, 30, 0, 45, 155, 44);
				}

				RenderSystem.setShaderTexture(0, EDITION_TITLE_TEXTURE);
				blit(matrix, j + 88, 67, 0.0F, 0.0F, 98, 14, 128, 16);
			}

			//Draw branding strings to the main menu if not disabled in the config
			if (this.showBranding) {
				BrandingControl.forEachLine(true, true, (brdline, brd) ->
						GuiComponent.drawString(matrix, font, brd, 2, e.getScreen().height - ( 10 + brdline * (font.lineHeight + 1)), 16777215)
				);
			}

			if (this.showForgeNotificationTop) {
				ForgeHooksClient.renderMainMenu((TitleScreen) e.getScreen(), matrix, Minecraft.getInstance().font, e.getScreen().width, e.getScreen().height, 255);
			}
			if (this.showForgeNotificationCopyright) {
				BrandingControl.forEachAboveCopyrightLine((brdline, brd) ->
						GuiComponent.drawString(matrix, font, brd, e.getScreen().width - font.width(brd) - 1, e.getScreen().height - (11 + (brdline + 1) * ( font.lineHeight + 1)), 16777215)
				);
			}

			if (!PopupHandler.isPopupActive()) {
				this.renderButtons(e, mouseX, mouseY);
			}

			//Draw notification indicators to the "Realms" button if not disabled in the config
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

	private void renderButtons(ScreenEvent.BackgroundDrawnEvent e, int mouseX, int mouseY) {
		List<Widget> buttons = this.getButtonList(e.getScreen());
		float partial = Minecraft.getInstance().getFrameTime();

		if (buttons != null) {
			for(int i = 0; i < buttons.size(); ++i) {
				buttons.get(i).render(CurrentScreenHandler.getPoseStack(), mouseX, mouseY, partial);
			}
		}
	}

	private void drawRealmsNotification(PoseStack matrix, Screen gui) {
		if (Minecraft.getInstance().options.realmsNotifications) {
			Field f = ReflectionHelper.findField(TitleScreen.class, "f_96726_"); //realmsNotificationScreen
			Screen realms = null;
			try {
				realms = (Screen) f.get(gui);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (realms != null) {
				//render
				realms.render(matrix, (int)Minecraft.getInstance().mouseHandler.xpos(), (int)Minecraft.getInstance().mouseHandler.ypos(), Minecraft.getInstance().getFrameTime());
			}
		}
	}

	private List<Widget> getButtonList(Screen gui) {
		List<Widget> buttons = new ArrayList<Widget>();
		try {
			Field f = ReflectionHelper.findField(Screen.class, "f_169369_"); //renderables
			try {
				buttons = (List<Widget>) f.get(gui);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buttons;
	}

	protected static void setShowFadeInAnimation(boolean showFadeIn, TitleScreen s) {
		try {
			Field f = ReflectionHelper.findField(TitleScreen.class, "f_96714_"); //fading
			f.setBoolean(s, showFadeIn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

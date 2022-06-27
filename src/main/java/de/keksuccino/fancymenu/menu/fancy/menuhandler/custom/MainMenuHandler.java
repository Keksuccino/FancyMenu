package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.util.concurrent.Runnables;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

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
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.CurrentScreenHandler;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WinGameScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.RenderSkybox;
import net.minecraft.client.renderer.RenderSkyboxCube;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.BackgroundDrawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.BrandingControl;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class MainMenuHandler extends MenuHandlerBase {
	
	private final RenderSkybox panorama = new RenderSkybox(new RenderSkyboxCube(new ResourceLocation("textures/gui/title/background/panorama")));
	private static final ResourceLocation MINECRAFT_TITLE_TEXTURES = new ResourceLocation("textures/gui/title/minecraft.png");
	private static final ResourceLocation MINECRAFT_TITLE_EDITION = new ResourceLocation("textures/gui/title/edition.png");
	private static final Random RANDOM = new Random();

	protected boolean showLogo = true;
	protected boolean showBranding = true;
	protected boolean showForgeNotificationCopyright = true;
	protected boolean showForgeNotificationTop = true;
	protected boolean showRealmsNotification = true;
	protected TitleScreenSplashItem splashItem = null;

	public MainMenuHandler() {
		super(MainMenuScreen.class.getName());
	}

	@Override
	public void onMenuReloaded(MenuReloadedEvent e) {
		super.onMenuReloaded(e);

		TitleScreenSplashItem.cachedSplashText = null;
	}

	@Override
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		if (this.shouldCustomize(e.getGui())) {
			if (MenuCustomization.isMenuCustomizable(e.getGui())) {
				if (e.getGui() instanceof MainMenuScreen) {
					setShowFadeInAnimation(false, (MainMenuScreen) e.getGui());
				}
			}
		}
		super.onInitPre(e);
	}

	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui())) {
			if (MenuCustomization.isMenuCustomizable(e.getGui())) {

				this.setWidthCopyrightRest(Integer.MAX_VALUE);

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
	@Override
	public void drawToBackground(BackgroundDrawnEvent e) {
		if (this.shouldCustomize(e.getGui())) {
			FontRenderer font = Minecraft.getInstance().fontRenderer;
			int width = e.getGui().width;
			int height = e.getGui().height;
			int j = width / 2 - 137;
			float minecraftLogoSpelling = RANDOM.nextFloat();
			int mouseX = MouseInput.getMouseX();
			int mouseY = MouseInput.getMouseY();
			MatrixStack matrix = CurrentScreenHandler.getMatrixStack();

			RenderSystem.enableBlend();

			//Draw the panorama skybox and a semi-transparent overlay over it
			if (!this.canRenderBackground()) {
				this.panorama.render(Minecraft.getInstance().getTickLength(), 1.0F);
				Minecraft.getInstance().getTextureManager().bindTexture(new ResourceLocation("textures/gui/title/background/panorama_overlay.png"));
				RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
				IngameGui.blit(matrix, 0, 0, width, height, 0.0F, 0.0F, 16, 128, 16, 128);
			}

			super.drawToBackground(e);

			//Draw minecraft logo and edition textures if not disabled in the config
			if (this.showLogo) {
				Minecraft.getInstance().getTextureManager().bindTexture(MINECRAFT_TITLE_TEXTURES);
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
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

				Minecraft.getInstance().getTextureManager().bindTexture(MINECRAFT_TITLE_EDITION);
				IngameGui.blit(matrix, j + 88, 67, 0.0F, 0.0F, 98, 14, 128, 16);
			}

			//Draw branding strings to the main menu if not disabled in the config
			if (this.showBranding) {
				BrandingControl.forEachLine(true, true, (brdline, brd) -> {
					AbstractGui.drawString(matrix, font, brd, 2, e.getGui().height - (10 + brdline * (font.FONT_HEIGHT + 1)), 16777215);
				});
			}

			if (this.showForgeNotificationTop) {
				ForgeHooksClient.renderMainMenu((MainMenuScreen) e.getGui(), matrix, font, width, height);
			}
			if (this.showForgeNotificationCopyright) {
				BrandingControl.forEachAboveCopyrightLine((brdline, brd) -> {
					AbstractGui.drawString(matrix, font, brd, e.getGui().width - font.getStringWidth(brd) - 1, e.getGui().height - (11 + (brdline + 1) * (font.FONT_HEIGHT + 1)), 16777215);
				});
			}

			//Draw and handle copyright
			String c = "Copyright Mojang AB. Do not distribute!";
			String cPos = FancyMenu.config.getOrDefault("copyrightposition", "bottom-right");
			int cX = 0;
			int cY = 0;
			int cW = Minecraft.getInstance().fontRenderer.getStringWidth(c);
			int cH = 10;

			if (cPos.equalsIgnoreCase("top-left")) {
				cX = 2;
				cY = 2;
			} else if (cPos.equalsIgnoreCase("top-centered")) {
				cX = (width / 2) - (cW / 2);
				cY = 2;
			} else if (cPos.equalsIgnoreCase("top-right")) {
				cX = width - cW - 2;
				cY = 2;
			} else if (cPos.equalsIgnoreCase("bottom-left")) {
				cX = 2;
				cY = height - cH - 2;
			} else if (cPos.equalsIgnoreCase("bottom-centered")) {
				cX = (width / 2) - (cW / 2);
				cY = height - cH - 2;
			} else {
				cX = width - cW - 2;
				cY = height - cH - 2;
			}

			Color copyrightcolor = RenderUtils.getColorFromHexString(FancyMenu.config.getOrDefault("copyrightcolor", "#ffffff"));
			if (copyrightcolor == null) {
				copyrightcolor = new Color(255, 255, 255);
			}

			AbstractGui.drawString(matrix, font, c, cX, cY, copyrightcolor.getRGB() | 255 << 24);

			if ((mouseX >= cX) && (mouseX <= cX + cW) && (mouseY >= cY) && (mouseY <= cY + cH)) {
				IngameGui.fill(matrix, cX, cY + cH - 1, cX + cW, cY + cH, -1);

				if (MouseInput.isLeftMouseDown()) {
					Minecraft.getInstance().displayGuiScreen(new WinGameScreen(false, Runnables.doNothing()));
				}
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

	protected void renderSplash(MatrixStack matrix, Screen s) {

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
		float partial = Minecraft.getInstance().getRenderPartialTicks();

		if (buttons != null) {
			for(int i = 0; i < buttons.size(); ++i) {
				buttons.get(i).render(CurrentScreenHandler.getMatrixStack(), mouseX, mouseY, partial);
			}
		}
	}

	private void drawRealmsNotification(MatrixStack matrix, Screen gui) {
		if (Minecraft.getInstance().gameSettings.realmsNotifications) {
			Field f = ObfuscationReflectionHelper.findField(MainMenuScreen.class, "field_183503_M"); //"realmsNotification" field from GuiMainMenu
			Screen realms = null;
			try {
				realms = (Screen) f.get(gui);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (realms != null) {
				//render
				realms.render(matrix, (int)Minecraft.getInstance().mouseHelper.getMouseX(), (int)Minecraft.getInstance().mouseHelper.getMouseY(), Minecraft.getInstance().getRenderPartialTicks());
			}
		}
	}

	private List<Widget> getButtonList(Screen gui) {
		Field f = ObfuscationReflectionHelper.findField(Screen.class, "field_230710_m_");
		List<Widget> buttons = new ArrayList<Widget>();
		try {
			buttons = (List<Widget>) f.get(gui);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buttons;
	}

	private void setWidthCopyrightRest(int i) {
		try {
			if (Minecraft.getInstance().currentScreen instanceof MainMenuScreen) {
				Field f = ObfuscationReflectionHelper.findField(MainMenuScreen.class, "field_193979_N");
				f.set(Minecraft.getInstance().currentScreen, i);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void setShowFadeInAnimation(boolean showFadeIn, MainMenuScreen s) {
		try {
			Field f = ObfuscationReflectionHelper.findField(MainMenuScreen.class, "field_213102_y");
			f.setBoolean(s, showFadeIn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

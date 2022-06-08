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
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
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
import net.minecraft.network.chat.Component;
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

	private String splash;

	public MainMenuHandler() {
		super(TitleScreen.class.getName());
	}

	@Override
	public void onMenuReloaded(MenuReloadedEvent e) {
		super.onMenuReloaded(e);

		this.splash = getRandomSplashText();
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

				if (this.splash == null) {
					this.splash = getRandomSplashText();
				}

				if (FancyMenu.getMinecraftVersion().equals("1.18") || FancyMenu.getMinecraftVersion().equals("1.18.1")) {
					this.setWidthCopyrightRest(Integer.MAX_VALUE);
				}

				super.onButtonsCached(e);
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
			if (!FancyMenu.config.getOrDefault("hidelogo", true)) {
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
			if (!FancyMenu.config.getOrDefault("hidebranding", true)) {
				BrandingControl.forEachLine(true, true, (brdline, brd) ->
						GuiComponent.drawString(matrix, font, brd, 2, e.getScreen().height - ( 10 + brdline * (font.lineHeight + 1)), 16777215)
				);
			}

			if (!FancyMenu.config.getOrDefault("hideforgenotifications", false)) {
				ForgeHooksClient.renderMainMenu((TitleScreen) e.getScreen(), matrix, Minecraft.getInstance().font, e.getScreen().width, e.getScreen().height, 255);
				BrandingControl.forEachAboveCopyrightLine((brdline, brd) ->
						GuiComponent.drawString(matrix, font, brd, e.getScreen().width - font.width(brd) - 1, e.getScreen().height - (11 + (brdline + 1) * ( font.lineHeight + 1)), 16777215)
				);
			}

			if (FancyMenu.getMinecraftVersion().equals("1.18") || FancyMenu.getMinecraftVersion().equals("1.18.1")) {

				//Draw and handle copyright
				String c = "Copyright Mojang AB. Do not distribute!";
				String cPos = FancyMenu.config.getOrDefault("copyrightposition", "bottom-right");
				int cX = 0;
				int cY = 0;
				int cW = Minecraft.getInstance().font.width(c);
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

				GuiComponent.drawString(matrix, font, c, cX, cY, copyrightcolor.getRGB() | 255 << 24);

				if ((mouseX >= cX) && (mouseX <= cX + cW) && (mouseY >= cY) && (mouseY <= cY + cH)) {
					GuiComponent.fill(matrix, cX, cY + cH - 1, cX + cW, cY + cH, -1);

					if (MouseInput.isLeftMouseDown()) {
						Minecraft.getInstance().setScreen(new WinScreen(false, Runnables.doNothing()));
					}
				}

			}

			if (!PopupHandler.isPopupActive()) {
				this.renderButtons(e, mouseX, mouseY);
			}

			//Draw notification indicators to the "Realms" button if not disabled in the config
			if (!FancyMenu.config.getOrDefault("hiderealmsnotifications", false)) {
				this.drawRealmsNotification(matrix, e.getScreen());
			}

			this.renderSplash(matrix, font, e.getScreen());

		}
	}

	protected void renderSplash(PoseStack matrix, Font font, Screen s) {

		if (!FancyMenu.config.getOrDefault("hidesplashtext", true)) {

			float finalPosX = (s.width / 2 + 90);
			float finalPosY = 70.0F;

			int rotation = FancyMenu.config.getOrDefault("splashrotation", -20);
			int posX = FancyMenu.config.getOrDefault("splashx", 0);
			int posY = FancyMenu.config.getOrDefault("splashy", 0);
			String orientation = FancyMenu.config.getOrDefault("splashorientation", "original");

			int originX = 0;
			int originY = 0;

			boolean setpos = true;

			if (orientation.equalsIgnoreCase("original")) {
				originX = (int) finalPosX;
				originY = (int) finalPosY;
			} else if (orientation.equalsIgnoreCase("top-left")) {
				; //do nuffin
			} else if (orientation.equalsIgnoreCase("mid-left")) {
				originY = s.height / 2;
			} else if (orientation.equalsIgnoreCase("bottom-left")) {
				originY = s.height;
			} else if (orientation.equalsIgnoreCase("top-centered")) {
				originX = s.width / 2;
			} else if (orientation.equalsIgnoreCase("mid-centered")) {
				originX = s.width / 2;
				originY = s.height / 2;
			} else if (orientation.equalsIgnoreCase("bottom-centered")) {
				originX = s.width / 2;
				originY = s.height;
			} else if (orientation.equalsIgnoreCase("top-right")) {
				originX = s.width;
			} else if (orientation.equalsIgnoreCase("mid-right")) {
				originX = s.width;
				originY = s.height / 2;
			} else if (orientation.equalsIgnoreCase("bottom-right")) {
				originX = s.width;
				originY = s.height;
			} else {
				setpos = false;
			}

			//I'm doing this to signalize when an invalid orientation was used
			if (setpos) {
				finalPosX = originX + posX;
				finalPosY = originY + posY;
			}

			if (this.splash == null) {
				this.splash = "";
			}

			matrix.pushPose();
			matrix.translate(finalPosX, finalPosY, 0.0F);
			matrix.mulPose(Vector3f.ZP.rotationDegrees(rotation));
			float f = 1.8F - Mth.abs(Mth.sin((float) (System.currentTimeMillis() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
			f = f * 100.0F / (float) (font.width(this.splash) + 32);
			matrix.scale(f, f, f);

			Color c = RenderUtils.getColorFromHexString(FancyMenu.config.getOrDefault("splashcolor", "#ffff00"));
			if (c == null) {
				c = new Color(255, 255, 0);
			}

			drawCenteredString(matrix, font, Component.literal(this.splash), 0, -8, c.getRGB());

			matrix.popPose();

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
		if (Minecraft.getInstance().options.realmsNotifications().get()) {
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

	private void setWidthCopyrightRest(int i) {
		try {
			if (Minecraft.getInstance().screen instanceof TitleScreen) {
				Field f = ReflectionHelper.findField(TitleScreen.class, "f_96728_"); //copyrightX
				f.set(Minecraft.getInstance().screen, i);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void setShowFadeInAnimation(boolean showFadeIn, TitleScreen s) {
		try {
			Field f = ReflectionHelper.findField(TitleScreen.class, "f_96714_"); //fading
			f.setBoolean(s, showFadeIn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static String getRandomSplashText() {
		String customSplashPath = FancyMenu.config.getOrDefault("splashtextfile", "");
		if ((customSplashPath != null) && !customSplashPath.equals("")) {
			File f = new File(customSplashPath);
			if (f.exists() && f.isFile() && f.getPath().toLowerCase().endsWith(".txt")) {
				List<String> l = FileUtils.getFileLines(f);
				if ((l != null) && !l.isEmpty()) {
					int random = MathUtils.getRandomNumberInRange(0, l.size()-1);
					return l.get(random);
				}
			}
		}

		return Minecraft.getInstance().getSplashManager().getSplash();
	}

}

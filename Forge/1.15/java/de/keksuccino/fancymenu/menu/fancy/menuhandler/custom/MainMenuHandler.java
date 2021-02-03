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

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WinGameScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.RenderSkybox;
import net.minecraft.client.renderer.RenderSkyboxCube;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.BackgroundDrawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.BrandingControl;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class MainMenuHandler extends MenuHandlerBase {

	private static final ResourceLocation FACEBOOK = new ResourceLocation("keksuccino", "socialmedia/fb.png");
	private static final ResourceLocation TWITTER = new ResourceLocation("keksuccino", "socialmedia/twitter.png");
	private static final ResourceLocation INSTAGRAM = new ResourceLocation("keksuccino", "socialmedia/instagram.png");
	private int tickFooter;
	private float fadeFooter;
	
	private final RenderSkybox panorama = new RenderSkybox(new RenderSkyboxCube(new ResourceLocation("textures/gui/title/background/panorama")));
	private static final ResourceLocation MINECRAFT_TITLE_TEXTURES = new ResourceLocation("textures/gui/title/minecraft.png");
	private static final ResourceLocation MINECRAFT_TITLE_EDITION = new ResourceLocation("textures/gui/title/edition.png");
	private static final Random RANDOM = new Random();
	
	private String splash;
	
	public MainMenuHandler() {
		super(MainMenuScreen.class.getName());
	}
	
	@Override
	public void onMenuReloaded(MenuReloadedEvent e) {
		super.onMenuReloaded(e);
		
		this.splash = getRandomSplashText();
	}
	
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui())) {
			if (MenuCustomization.isMenuCustomizable(e.getGui())) {
				// Resetting values to defaults
				fadeFooter = 0.1F;
				tickFooter = 0;

				if (this.splash == null) {
					this.splash = getRandomSplashText();
				}
				
				this.setWidthCopyrightRest(Integer.MAX_VALUE);
				
				super.onButtonsCached(e);
			}
		}
	}
	
	@SubscribeEvent
	public void onRender(GuiScreenEvent.DrawScreenEvent.Pre e) {
		if (this.shouldCustomize(e.getGui())) {
			if (MenuCustomization.isMenuCustomizable(e.getGui())) {
				e.setCanceled(true);
				e.getGui().renderBackground();

				this.renderFooter(e);
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
			
			RenderSystem.enableBlend();
			
			//Draw the panorama skybox and a semi-transparent overlay over it
			if (!this.canRenderBackground()) {
				this.panorama.render(Minecraft.getInstance().getRenderPartialTicks(), 1.0F);
				Minecraft.getInstance().getTextureManager().bindTexture(new ResourceLocation("textures/gui/title/background/panorama_overlay.png"));
				RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
				Screen.blit(0, 0, width, height, 0.0F, 0.0F, 16, 128, 16, 128);
			}
			
			super.drawToBackground(e);
			
			//Draw minecraft logo and edition textures if not disabled in the config
			if (!FancyMenu.config.getOrDefault("hidelogo", true)) {
				Minecraft.getInstance().getTextureManager().bindTexture(MINECRAFT_TITLE_TEXTURES);
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
				if ((double) minecraftLogoSpelling < 1.0E-4D) {
					e.getGui().blit(j + 0, 30, 0, 0, 99, 44);
					e.getGui().blit(j + 99, 30, 129, 0, 27, 44);
					e.getGui().blit(j + 99 + 26, 30, 126, 0, 3, 44);
					e.getGui().blit(j + 99 + 26 + 3, 30, 99, 0, 26, 44);
					e.getGui().blit(j + 155, 30, 0, 45, 155, 44);
				} else {
					e.getGui().blit(j + 0, 30, 0, 0, 155, 44);
					e.getGui().blit(j + 155, 30, 0, 45, 155, 44);
				}

				Minecraft.getInstance().getTextureManager().bindTexture(MINECRAFT_TITLE_EDITION);
				Screen.blit(j + 88, 67, 0.0F, 0.0F, 98, 14, 128, 16);
			}

			//Draw branding strings to the main menu if not disabled in the config
			if (!FancyMenu.config.getOrDefault("hidebranding", true)) {
				BrandingControl.forEachLine(true, true, (brdline, brd) -> e.getGui().drawString(font, brd, 2, height - (10 + brdline * (font.FONT_HEIGHT + 1)), 16777215));
			}
			
			if (!FancyMenu.config.getOrDefault("hideforgenotifications", false)) {
				ForgeHooksClient.renderMainMenu((MainMenuScreen) e.getGui(), font, width, height);
				
				BrandingControl.forEachAboveCopyrightLine((brdline, brd) ->
				   e.getGui().drawString(font, brd, e.getGui().width - font.getStringWidth(brd) - 1, e.getGui().height - (11 + (brdline + 1) * ( font.FONT_HEIGHT + 1)), 16777215)
				);
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
			
			e.getGui().drawString(font, c, cX, cY, -1);
			
			if ((mouseX >= cX) && (mouseX <= cX + cW) && (mouseY >= cY) && (mouseY <= cY + cH)) {
				IngameGui.fill(cX, cY + cH - 1, cX + cW, cY + cH, -1);
				
				if (MouseInput.isLeftMouseDown()) {
					Minecraft.getInstance().displayGuiScreen(new WinGameScreen(false, Runnables.doNothing()));
				}
			}

			if (!PopupHandler.isPopupActive()) {
				this.renderButtons(e, mouseX, mouseY);
			}
			
			//Draw notification indicators to the "Realms" button if not disabled in the config
			if (!FancyMenu.config.getOrDefault("hiderealmsnotifications", false)) {
				this.drawRealmsNotification(e.getGui());
			}

			this.renderSplash(font, e.getGui());
			
		}
	}
	
	protected void renderSplash(FontRenderer font, Screen s) {
		
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

			RenderSystem.pushMatrix();
			RenderSystem.translatef(finalPosX, finalPosY, 0.0F);
			RenderSystem.rotatef((float)rotation, 0.0F, 0.0F, 1.0F);
			float f = 1.8F - MathHelper.abs(MathHelper.sin((float) (Util.milliTime() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
			f = f * 100.0F / (float) (font.getStringWidth(this.splash) + 32);
			RenderSystem.scalef(f, f, f);

			Color c = RenderUtils.getColorFromHexString(FancyMenu.config.getOrDefault("splashcolor", "#ffff00"));
			if (c == null) {
				c = new Color(255, 255, 0);
			}
			drawCenteredString(font, this.splash, 0, -8, c.getRGB());
			RenderSystem.popMatrix();
			
		}
		
	}
	
	private void renderButtons(GuiScreenEvent.BackgroundDrawnEvent e, int mouseX, int mouseY) {
		List<Widget> buttons = this.getButtonList(e.getGui());
		float partial = Minecraft.getInstance().getRenderPartialTicks();
		
		if (buttons != null) {
			for(int i = 0; i < buttons.size(); ++i) {
				buttons.get(i).render(mouseX, mouseY, partial);
			}
		}
	}
	
	private void drawRealmsNotification(Screen gui) {
		if (Minecraft.getInstance().gameSettings.realmsNotifications) {
			Field f = ObfuscationReflectionHelper.findField(MainMenuScreen.class, "field_183503_M"); //"realmsNotification" field from GuiMainMenu
			Screen realms = null;
			try {
				realms = (Screen) f.get(gui);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (realms != null) {
				realms.render((int)Minecraft.getInstance().mouseHelper.getMouseX(), (int)Minecraft.getInstance().mouseHelper.getMouseY(), Minecraft.getInstance().getRenderPartialTicks());
			}
		}
	}
	
	private List<Widget> getButtonList(Screen gui) {
		Field f = ObfuscationReflectionHelper.findField(Screen.class, "buttons");
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
	
	private void renderFooter(GuiScreenEvent.DrawScreenEvent e) {
		if (!FancyMenu.config.getOrDefault("showmainmenufooter", true)) {
			return;
		}
		
		if (tickFooter < 30) {
			tickFooter += 1;
		} else if (e.getGui().height >= 280) {
			int i = MathHelper.ceil(fadeFooter * 255.0F) << 24;
			
			RenderUtils.setScale(1.1F);
			e.getGui().drawCenteredString(Minecraft.getInstance().fontRenderer, "§fDISCOVER MORE AT MINECRAFT.NET", (int) (e.getGui().width / 2 / 1.1D), (int) ((e.getGui().height - 50) / 1.1D), i);
			RenderUtils.postScale();
			
			RenderUtils.setScale(0.7F);
			e.getGui().drawString(Minecraft.getInstance().fontRenderer, "§f@MINECRAFT", (int) ((e.getGui().width / 2 - 10) / 0.7D), (int) ((e.getGui().height - 30) / 0.7D), i);
			
			e.getGui().drawString(Minecraft.getInstance().fontRenderer, "§fMINECRAFT", (int) ((e.getGui().width / 2 + 60) / 0.7D), (int) ((e.getGui().height - 30) / 0.7D), i);

			e.getGui().drawString(Minecraft.getInstance().fontRenderer, "§f/MINECRAFT", (int) ((e.getGui().width / 2 - 80) / 0.7D), (int) ((e.getGui().height - 30) / 0.7D), i);
			RenderUtils.postScale();
			
			RenderSystem.enableBlend();
			
			Minecraft.getInstance().getTextureManager().bindTexture(FACEBOOK);
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, MathHelper.clamp(fadeFooter, 0.1F, 1.0F));
			Screen.blit(e.getGui().width / 2 - 100, e.getGui().height - 35, 0.0F, 0.0F, 15, 15, 15, 15);

			Minecraft.getInstance().getTextureManager().bindTexture(TWITTER);
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, MathHelper.clamp(fadeFooter, 0.1F, 1.0F));
			Screen.blit(e.getGui().width / 2 - 30, e.getGui().height - 35, 0.0F, 0.0F, 15, 15, 15, 15);

			Minecraft.getInstance().getTextureManager().bindTexture(INSTAGRAM);
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, MathHelper.clamp(fadeFooter, 0.1F, 1.0F));
			Screen.blit(e.getGui().width / 2 + 40, e.getGui().height - 35, 0.0F, 0.0F, 15, 15, 15, 15);
			
			RenderSystem.disableBlend();

			if (fadeFooter < 1.0F) {
				fadeFooter += 0.005F;
			}
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
		
		return Minecraft.getInstance().getSplashes().getSplashText();
	}
	
}

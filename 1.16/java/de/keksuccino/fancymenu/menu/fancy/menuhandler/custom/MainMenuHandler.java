package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

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
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
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
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
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
	public void onButtonsCached(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui())) {
			// Resetting values to defaults
			fadeFooter = 0.1F;
			tickFooter = 0;

			if (this.splash == null) {
				this.splash = Minecraft.getInstance().getSplashes().getSplashText();
			}
		}
		
		super.onButtonsCached(e);
	}
	
	@SubscribeEvent
	public void onRender(GuiScreenEvent.DrawScreenEvent.Pre e) {
		if (this.shouldCustomize(e.getGui())) {
			e.setCanceled(true);
			e.getGui().renderBackground(e.getMatrixStack());;
			
			this.renderFooter(e);
		}
	}
	
	/**
	 * Mimic the original main menu to be able to customize it easier
	 */
	@SuppressWarnings("deprecation")
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
				this.panorama.render(Minecraft.getInstance().getRenderPartialTicks(), 1.0F);
				Minecraft.getInstance().getTextureManager().bindTexture(new ResourceLocation("textures/gui/title/background/panorama_overlay.png"));
				RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
				IngameGui.blit(matrix, 0, 0, width, height, 0.0F, 0.0F, 16, 128, 16, 128);
			}
			
			super.drawToBackground(e);
			
			//Draw minecraft logo and edition textures if not disabled in the config
			if (!FancyMenu.config.getOrDefault("hidelogo", true)) {
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
			if (!FancyMenu.config.getOrDefault("hidebranding", true)) {
				e.getGui();
				BrandingControl.forEachLine(true, true, (brdline, brd) ->
				   AbstractGui.drawString(matrix, font, brd, 2, e.getGui().height - ( 10 + brdline * (font.FONT_HEIGHT + 1)), 16777215)
				);
			}

			if (!FancyMenu.config.getOrDefault("hideforgenotifications", false)) {
				ForgeHooksClient.renderMainMenu((MainMenuScreen) e.getGui(), matrix, font, width, height);
				
				e.getGui();
				BrandingControl.forEachAboveCopyrightLine((brdline, brd) ->
				   AbstractGui.drawString(matrix, font, brd, e.getGui().width - font.getStringWidth(brd) - 1, e.getGui().height - (11 + (brdline + 1) * ( font.FONT_HEIGHT + 1)), 16777215)
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
			
			e.getGui();
			AbstractGui.drawString(matrix, font, c, cX, cY, -1);
			
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
			if (!FancyMenu.config.getOrDefault("hiderealmsnotifications", false)) {
				this.drawRealmsNotification(matrix, e.getGui());
			}
			
			//Draw splashtext string to the main menu if not disabled in the config
			if (!FancyMenu.config.getOrDefault("hidesplashtext", true)) {
				int offsetx = FancyMenu.config.getOrDefault("splashoffsetx", 0);
				int offsety = FancyMenu.config.getOrDefault("splashoffsety", 0);
				int rotation = FancyMenu.config.getOrDefault("splashrotation", -20);
				RenderSystem.pushMatrix();
				RenderSystem.translatef((float) (width / 2 + 90) + offsetx, 70.0F + offsety, 0.0F);
				RenderSystem.rotatef((float)rotation, 0.0F, 0.0F, 1.0F);
				float f = 1.8F - MathHelper.abs(MathHelper.sin((float) (Util.milliTime() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
				f = f * 100.0F / (float) (font.getStringWidth(this.splash) + 32);
				RenderSystem.scalef(f, f, f);
				e.getGui();
				AbstractGui.drawCenteredString(matrix, font, new StringTextComponent(this.splash), 0, -8, -256);
				RenderSystem.popMatrix();
			}
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
	
	@SuppressWarnings("deprecation")
	private void renderFooter(GuiScreenEvent.DrawScreenEvent e) {
		if (!FancyMenu.config.getOrDefault("showmainmenufooter", true)) {
			return;
		}
		
		if (tickFooter < 30) {
			tickFooter += 1;
		} else if (e.getGui().height >= 280) {
			int i = MathHelper.ceil(fadeFooter * 255.0F) << 24;
			
			RenderUtils.setScale(e.getMatrixStack(), 1.1F);
			e.getGui();
			AbstractGui.drawCenteredString(e.getMatrixStack(), Minecraft.getInstance().fontRenderer, new StringTextComponent("§fDISCOVER MORE AT MINECRAFT.NET"), (int) (e.getGui().width / 2 / 1.1D), (int) ((e.getGui().height - 50) / 1.1D), i);
			RenderUtils.postScale(e.getMatrixStack());
			
			RenderUtils.setScale(e.getMatrixStack(), 0.7F);
			e.getGui();
			AbstractGui.drawString(e.getMatrixStack(), Minecraft.getInstance().fontRenderer, "§f@MINECRAFT", (int) ((e.getGui().width / 2 - 10) / 0.7D), (int) ((e.getGui().height - 30) / 0.7D), i);
			
			e.getGui();
			AbstractGui.drawString(e.getMatrixStack(), Minecraft.getInstance().fontRenderer, "§fMINECRAFT", (int) ((e.getGui().width / 2 + 60) / 0.7D), (int) ((e.getGui().height - 30) / 0.7D), i);

			e.getGui();
			AbstractGui.drawString(e.getMatrixStack(), Minecraft.getInstance().fontRenderer, "§f/MINECRAFT", (int) ((e.getGui().width / 2 - 80) / 0.7D), (int) ((e.getGui().height - 30) / 0.7D), i);
			RenderUtils.postScale(e.getMatrixStack());
			
			RenderSystem.enableBlend();
			
			Minecraft.getInstance().getTextureManager().bindTexture(FACEBOOK);
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, MathHelper.clamp(fadeFooter, 0.1F, 1.0F));
			Screen.blit(e.getMatrixStack(), e.getGui().width / 2 - 100, e.getGui().height - 35, 0.0F, 0.0F, 15, 15, 15, 15);

			Minecraft.getInstance().getTextureManager().bindTexture(TWITTER);
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, MathHelper.clamp(fadeFooter, 0.1F, 1.0F));
			Screen.blit(e.getMatrixStack(), e.getGui().width / 2 - 30, e.getGui().height - 35, 0.0F, 0.0F, 15, 15, 15, 15);

			Minecraft.getInstance().getTextureManager().bindTexture(INSTAGRAM);
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, MathHelper.clamp(fadeFooter, 0.1F, 1.0F));
			Screen.blit(e.getMatrixStack(), e.getGui().width / 2 + 40, e.getGui().height - 35, 0.0F, 0.0F, 15, 15, 15, 15);
			
			RenderSystem.disableBlend();

			if (fadeFooter < 1.0F) {
				fadeFooter += 0.005F;
			}
		}
	}
}

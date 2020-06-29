package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.core.gui.screens.popup.PopupHandler;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.rendering.CurrentScreenHandler;
import de.keksuccino.core.rendering.RenderUtils;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.RenderSkybox;
import net.minecraft.client.renderer.RenderSkyboxCube;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
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
	
	public MainMenuHandler() {
		super(MainMenuScreen.class.getName());
	}
	
	@Override
	public void onInitPost(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui())) {
			// Resetting values to defaults
			fadeFooter = 0.1F;
			tickFooter = 0;
		}
		
		super.onInitPost(e);
	}
	
	@SubscribeEvent
	public void onRender(GuiScreenEvent.DrawScreenEvent.Pre e) {
		if (this.shouldCustomize(e.getGui())) {
			e.setCanceled(true);
			e.getGui().func_230446_a_(e.getMatrixStack());;
			
			this.renderFooter(e);
		}
	}
	
	/**
	 * Mimic the original main menu to be able to customize it easier
	 */
	@Override
	public void drawToBackground(BackgroundDrawnEvent e) {
		if (this.shouldCustomize(e.getGui())) {
			FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
			int width = e.getGui().field_230708_k_;
			int height = e.getGui().field_230709_l_;
			int j = width / 2 - 137;
			float minecraftLogoSpelling = RANDOM.nextFloat();
			int mouseX = MouseInput.getMouseX();
			int mouseY = MouseInput.getMouseY();
			String copyright = "Copyright Mojang AB. Do not distribute!";
			int widthCopyright = Minecraft.getInstance().fontRenderer.getStringWidth(copyright);
			int widthCopyrightRest = width - widthCopyright - 2;
			MatrixStack matrix = CurrentScreenHandler.getMatrixStack();
			
			RenderSystem.enableBlend();
			
			//Draw the panorama skybox and a semi-transparent overlay over it
			if (!this.canRenderBackground()) {
				this.panorama.render(Minecraft.getInstance().getRenderPartialTicks(), 1.0F);
				Minecraft.getInstance().getTextureManager().bindTexture(new ResourceLocation("textures/gui/title/background/panorama_overlay.png"));
				RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
				IngameGui.func_238466_a_(matrix, 0, 0, width, height, 0.0F, 0.0F, 16, 128, 16, 128);
			}
			
			super.drawToBackground(e);
			
			//Draw minecraft logo and edition textures if not disabled in the config
			if (!FancyMenu.config.getOrDefault("hidelogo", true)) {
				Minecraft.getInstance().getTextureManager().bindTexture(MINECRAFT_TITLE_TEXTURES);
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
				if ((double) minecraftLogoSpelling < 1.0E-4D) {
					e.getGui().func_238474_b_(matrix, j + 0, 30, 0, 0, 99, 44);
					e.getGui().func_238474_b_(matrix, j + 99, 30, 129, 0, 27, 44);
					e.getGui().func_238474_b_(matrix, j + 99 + 26, 30, 126, 0, 3, 44);
					e.getGui().func_238474_b_(matrix, j + 99 + 26 + 3, 30, 99, 0, 26, 44);
					e.getGui().func_238474_b_(matrix, j + 155, 30, 0, 45, 155, 44);
				} else {
					e.getGui().func_238474_b_(matrix, j + 0, 30, 0, 0, 155, 44);
					e.getGui().func_238474_b_(matrix, j + 155, 30, 0, 45, 155, 44);
				}

				Minecraft.getInstance().getTextureManager().bindTexture(MINECRAFT_TITLE_EDITION);
				IngameGui.func_238463_a_(matrix, j + 88, 67, 0.0F, 0.0F, 98, 14, 128, 16);
			}

			ForgeHooksClient.renderMainMenu((MainMenuScreen) e.getGui(), matrix, fontRenderer, width, height);

			//Draw branding strings to the main menu if not disabled in the config
			if (!FancyMenu.config.getOrDefault("hidebranding", true)) {
				BrandingControl.forEachLine(true, true, (brdline, brd) -> e.getGui().func_238476_c_(matrix, fontRenderer, brd, 2, height - (10 + brdline * (fontRenderer.FONT_HEIGHT + 1)), 16777215));
			}
			
			e.getGui().func_238476_c_(matrix, fontRenderer, copyright, widthCopyrightRest, height - 10, -1);
			if (mouseX > widthCopyrightRest && mouseX < widthCopyrightRest + widthCopyright && mouseY > height - 10 && mouseY < height) {
				IngameGui.func_238467_a_(matrix, widthCopyrightRest, height - 1, widthCopyrightRest + widthCopyright, height, -1);
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
				matrix.push();
				matrix.translate((float) (width / 2 + 90) + offsetx, 70.0F + offsety, 0.0F);
				matrix.rotate(new Quaternion((float)rotation, 0.0F, 0.0F, 1.0F));
				float f = 1.8F - MathHelper.abs(MathHelper.sin((float) (Util.milliTime() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
				f = f * 100.0F / (float) (fontRenderer.getStringWidth(this.getSplash(e.getGui())) + 32);
				matrix.scale(f, f, f);
				e.getGui().func_238472_a_(matrix, fontRenderer, new StringTextComponent(this.getSplash(e.getGui())), 0, -8, -256);
				matrix.pop();
			}
		}
	}
	
	private void renderButtons(GuiScreenEvent.BackgroundDrawnEvent e, int mouseX, int mouseY) {
		List<Widget> buttons = this.getButtonList(e.getGui());
		float partial = Minecraft.getInstance().getRenderPartialTicks();
		
		if (buttons != null) {
			for(int i = 0; i < buttons.size(); ++i) {
				buttons.get(i).func_230430_a_(CurrentScreenHandler.getMatrixStack(), mouseX, mouseY, partial);
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
				realms.func_230430_a_(matrix, (int)Minecraft.getInstance().mouseHelper.getMouseX(), (int)Minecraft.getInstance().mouseHelper.getMouseY(), Minecraft.getInstance().getRenderPartialTicks());
			}
		}
	}
	
	private List<Widget> getButtonList(Screen gui) {
		Field f = ObfuscationReflectionHelper.findField(Screen.class, "field_230710_m_");
		List<Widget> buttons = null;
		try {
			buttons = (List<Widget>) f.get(gui);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buttons;
	}
	
	private String getSplash(Screen gui) {
		Field f5 = ObfuscationReflectionHelper.findField(MainMenuScreen.class, "field_73975_c");
		try {
			return (String) f5.get(gui);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void renderFooter(GuiScreenEvent.DrawScreenEvent e) {
		if (!FancyMenu.config.getOrDefault("showmainmenufooter", true)) {
			return;
		}
		
		if (tickFooter < 30) {
			tickFooter += 1;
		} else if (e.getGui().field_230709_l_ >= 280) {
			int i = MathHelper.ceil(fadeFooter * 255.0F) << 24;
			
			RenderUtils.setScale(e.getMatrixStack(), 1.1F);
			e.getGui().func_238472_a_(e.getMatrixStack(), Minecraft.getInstance().fontRenderer, new StringTextComponent("§fDISCOVER MORE AT MINECRAFT.NET"), (int) (e.getGui().field_230708_k_ / 2 / 1.1D), (int) ((e.getGui().field_230709_l_ - 50) / 1.1D), i);
			RenderUtils.postScale(e.getMatrixStack());
			
			RenderUtils.setScale(e.getMatrixStack(), 0.7F);
			e.getGui().func_238476_c_(e.getMatrixStack(), Minecraft.getInstance().fontRenderer, "§f@MINECRAFT", (int) ((e.getGui().field_230708_k_ / 2 - 10) / 0.7D), (int) ((e.getGui().field_230709_l_ - 30) / 0.7D), i);
			
			e.getGui().func_238476_c_(e.getMatrixStack(), Minecraft.getInstance().fontRenderer, "§fMINECRAFT", (int) ((e.getGui().field_230708_k_ / 2 + 60) / 0.7D), (int) ((e.getGui().field_230709_l_ - 30) / 0.7D), i);

			e.getGui().func_238476_c_(e.getMatrixStack(), Minecraft.getInstance().fontRenderer, "§f/MINECRAFT", (int) ((e.getGui().field_230708_k_ / 2 - 80) / 0.7D), (int) ((e.getGui().field_230709_l_ - 30) / 0.7D), i);
			RenderUtils.postScale(e.getMatrixStack());
			
			RenderSystem.enableBlend();
			
			Minecraft.getInstance().getTextureManager().bindTexture(FACEBOOK);
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, MathHelper.clamp(fadeFooter, 0.1F, 1.0F));
			Screen.func_238463_a_(e.getMatrixStack(), e.getGui().field_230708_k_ / 2 - 100, e.getGui().field_230709_l_ - 35, 0.0F, 0.0F, 15, 15, 15, 15);

			Minecraft.getInstance().getTextureManager().bindTexture(TWITTER);
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, MathHelper.clamp(fadeFooter, 0.1F, 1.0F));
			Screen.func_238463_a_(e.getMatrixStack(), e.getGui().field_230708_k_ / 2 - 30, e.getGui().field_230709_l_ - 35, 0.0F, 0.0F, 15, 15, 15, 15);

			Minecraft.getInstance().getTextureManager().bindTexture(INSTAGRAM);
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, MathHelper.clamp(fadeFooter, 0.1F, 1.0F));
			Screen.func_238463_a_(e.getMatrixStack(), e.getGui().field_230708_k_ / 2 + 40, e.getGui().field_230709_l_ - 35, 0.0F, 0.0F, 15, 15, 15, 15);
			
			RenderSystem.disableBlend();

			if (fadeFooter < 1.0F) {
				fadeFooter += 0.005F;
			}
		}
	}
}

package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.RenderSkybox;
import net.minecraft.client.renderer.RenderSkyboxCube;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.BrandingControl;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class MainMenuHandler extends MenuHandlerBase {

	private static final ResourceLocation FACEBOOK = new ResourceLocation("keksuccino", "socialmedia/fb.png");
	private static final ResourceLocation TWITTER = new ResourceLocation("keksuccino", "socialmedia/twitter.png");
	private static final ResourceLocation INSTAGRAM = new ResourceLocation("keksuccino", "socialmedia/instagram.png");
	private Map<Integer, List<Widget>> widgetsRaw = new HashMap<Integer, List<Widget>>();
	private List<List<Widget>> widgets = new ArrayList<List<Widget>>();
	private int tick;
	private int cachedTick;
	private int tickFooter;
	private float fadeFooter;
	private int fadeInFrame = -1;
	
	private final RenderSkybox panorama = new RenderSkybox(new RenderSkyboxCube(new ResourceLocation("textures/gui/title/background/panorama")));
	private static final ResourceLocation MINECRAFT_TITLE_TEXTURES = new ResourceLocation("textures/gui/title/minecraft.png");
	private static final ResourceLocation MINECRAFT_TITLE_EDITION = new ResourceLocation("textures/gui/title/edition.png");
	private static final Random RANDOM = new Random();
	
	@Override
	public String getMenuIdentifier() {
		return this.getMenuType().getName();
	}

	@Override
	public Class<?> getMenuType() {
		return MainMenuScreen.class;
	}
	
	//----------------------------------------------------------
	
	@SubscribeEvent
	public void onRender(GuiScreenEvent.DrawScreenEvent.Pre e) {
		if (this.shouldCustomize(e.getGui())) {
			
			this.renderMainMenu(e);
			
			if (this.canRenderBackground() && FancyMenu.config.getOrDefault("buttonfadein", true)) {
				//Setting the animation frame at which the menu buttons should start fading in
				if (fadeInFrame == -1) {
					int i2 = FancyMenu.config.getOrDefault("mainmenufadeinframe", 0);
					if (i2 > this.backgroundAnimation.animationFrames()) {
						fadeInFrame = 0;
					} else {
						fadeInFrame = i2;
					}
				}
				
				if (this.backgroundAnimation.currentFrame() >= fadeInFrame) {
					if (!widgets.isEmpty()) {
						if (tick >= cachedTick + 9) {
							for (Widget w : (List<Widget>) widgets.get(0)) {
								w.visible = true;
							}
							widgets.remove(0);
							cachedTick = tick;
						}
						tick += 1;
					} else {
						renderFooter(e);
						tick = 0;
						cachedTick = 0;
					}
				}
			} else {
				//Rendering footer instant if button-fade-in is disabled
				renderFooter(e);
			}

		}
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onScreenInit(GuiScreenEvent.InitGuiEvent.Post e) {
		if (this.shouldCustomize(e.getGui())) {
			// Resetting values to defaults
			fadeFooter = 0.1F;
			tickFooter = 0;
			tick = 0;
			cachedTick = 0;

			widgetsRaw.clear();
			widgets.clear();

			if (this.canRenderBackground()) {
				if (this.shouldFadeInButtons()) {
					cacheWidgets(e);

					List<Integer> ints = new ArrayList<Integer>();
					ints.addAll(widgetsRaw.keySet());

					// Sorting all buttons by its group (height)
					Collections.sort(ints, new Comparator<Integer>() {
						@Override
						public int compare(Integer o1, Integer o2) {
							if (o1 > o2) {
								return 1;
							}
							if (o1 < o2) {
								return -1;
							}
							return 0;
						}
					});

					for (Integer i : ints) {
						widgets.add(widgetsRaw.get(i));
					}
				}
			}
		}
	}
	
	/**
	 * Mimic the original main menu to be able to customize it easier
	 */
	private void renderMainMenu(GuiScreenEvent.DrawScreenEvent.Pre e) {
		e.setCanceled(true);
		
		FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
		int width = e.getGui().width;
		int height = e.getGui().height;
		int j = width / 2 - 137;
		float minecraftLogoSpelling = RANDOM.nextFloat();
		int mouseX = e.getMouseX();
		int mouseY = e.getMouseY();
		String copyright = "Copyright Mojang AB. Do not distribute!";
		int widthCopyright = Minecraft.getInstance().fontRenderer.getStringWidth(copyright);
		int widthCopyrightRest = width - widthCopyright - 2;
		
		RenderSystem.enableBlend();
		
		//Draw the panorama skybox and a semi-transparent overlay over it
		if (!this.canRenderBackground()) {
			this.panorama.render(Minecraft.getInstance().getRenderPartialTicks(), 1.0F);
			Minecraft.getInstance().getTextureManager().bindTexture(new ResourceLocation("textures/gui/title/background/panorama_overlay.png"));
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			Screen.blit(0, 0, width, height, 0.0F, 0.0F, 16, 128, 16, 128);
		} else {
			e.getGui().renderBackground();
		}
		
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

		RenderSystem.disableBlend();
		
		ForgeHooksClient.renderMainMenu((MainMenuScreen) e.getGui(), fontRenderer, width, height);

		//Draw splashtext string to the main menu if not disabled in the config
		if (!FancyMenu.config.getOrDefault("hidesplashtext", true)) {
			RenderSystem.pushMatrix();
			RenderSystem.translatef((float) (width / 2 + 90), 70.0F, 0.0F);
			RenderSystem.rotatef(-20.0F, 0.0F, 0.0F, 1.0F);
			float f = 1.8F - MathHelper.abs(MathHelper.sin((float) (Util.milliTime() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
			f = f * 100.0F / (float) (fontRenderer.getStringWidth(this.getSplash(e.getGui())) + 32);
			RenderSystem.scalef(f, f, f);
			e.getGui().drawCenteredString(fontRenderer, this.getSplash(e.getGui()), 0, -8, -256);
			RenderSystem.popMatrix();
		}

		//Draw branding strings to the main menu if not disabled in the config
		if (!FancyMenu.config.getOrDefault("hidebranding", true)) {
			BrandingControl.forEachLine(true, true, (brdline, brd) -> e.getGui().drawString(fontRenderer, brd, 2, height - (10 + brdline * (fontRenderer.FONT_HEIGHT + 1)), 16777215));
		}
		
		e.getGui().drawString(fontRenderer, copyright, widthCopyrightRest, height - 10, -1);
		if (mouseX > widthCopyrightRest && mouseX < widthCopyrightRest + widthCopyright && mouseY > height - 10 && mouseY < height) {
			Screen.fill(widthCopyrightRest, height - 1, widthCopyrightRest + widthCopyright, height, -1);
		}

		this.renderButtonsAndLabels(e);
		
		//Draw notification indicators to the "Realms" button if not disabled in the config
		if (!FancyMenu.config.getOrDefault("hiderealmsnotifications", false)) {
			this.drawRealmsNotification(e.getGui());
		}
	}
	
	/**
	 * Only renders buttons in newer MC versions
	 */
	private void renderButtonsAndLabels(GuiScreenEvent.DrawScreenEvent.Pre e) {
		List<Widget> buttons = this.getButtonList(e.getGui());
		int mouseX = e.getMouseX();
		int mouseY = e.getMouseY();
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
	
	@SuppressWarnings("unchecked")
	private List<Widget> getButtonList(Screen gui) {
		Field f = ObfuscationReflectionHelper.findField(Screen.class, "buttons");
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
	
	private boolean fade = false;
	private boolean shouldFadeInButtons() {
		if (FancyMenu.config.getOrDefault("buttonfadein", true) && !fade) {
			if (!this.replayIntro) {
				fade = true;
			}
			return true;
		}
		return false;
	}
	
	private void cacheWidgets(GuiScreenEvent.InitGuiEvent e) {
		//Grouping all menu buttons by its height to fade in buttons at the same height in the same moment
		//Seperated from ButtonCache because we need the (possibly) rearanged buttons in its new order
		for (Widget w : e.getWidgetList()) {
			if (w.visible) {
				if (widgetsRaw.containsKey(w.y)) {
					widgetsRaw.get(w.y).add(w);
				} else {
					widgetsRaw.put(w.y, new ArrayList<Widget>());
					widgetsRaw.get(w.y).add(w);
				}
				w.visible = false;
			}
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
}

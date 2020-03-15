package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.mainmenu.MainMenu;
import de.keksuccino.reflection.ReflectionHelper;
import de.keksuccino.rendering.RenderUtils;
import de.keksuccino.rendering.animation.AnimationRendererSkybox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.RenderSkybox;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.BackgroundDrawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
	
	private RenderSkybox cachedSkybox;
	
	@Override
	public String getMenuIdentifier() {
		return this.getMenuType().getName();
	}

	@Override
	public Class<?> getMenuType() {
		return MainMenuScreen.class;
	}
	
	@Override
	public void drawToBackground(BackgroundDrawnEvent e) {}
	
	//----------------------------------------------------------
	
	@SubscribeEvent
	public void onRender(GuiScreenEvent.DrawScreenEvent.Post e) {
		if (this.shouldCustomize(e.getGui())) {
			if (this.canRenderBackground() && FancyMenu.config.getOrDefault("buttonfadein", true)) {
				//Setting the animation frame at which the menu buttons should start fading in
				if (fadeInFrame == -1) {
					int i = FancyMenu.config.getOrDefault("mainmenufadeinframe", 0);
					if (i > this.backgroundAnimation.animationFrames()) {
						fadeInFrame = 0;
					} else {
						fadeInFrame = i;
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
			//Resetting values to defaults
			fadeFooter = 0.1F;
			tickFooter = 0;
			tick = 0;
			cachedTick = 0;

			if (FancyMenu.config.getOrDefault("hidebranding", true)) {
				MainMenu.clearBranding();
			}
			
			try {
				//Clearing the semi-transparent background overlay and setting the animation render skybox if a background animation was defined
				if (this.canRenderBackground()) {
					Field f = ObfuscationReflectionHelper.findField(MainMenuScreen.class, "field_209101_K");
					if (this.cachedSkybox == null) {
						this.cachedSkybox = (RenderSkybox) f.get(e.getGui());
					}
					ReflectionHelper.setField(f, e.getGui(), new AnimationRendererSkybox(this.backgroundAnimation));
					
					Field f3 = ObfuscationReflectionHelper.findField(MainMenuScreen.class, "field_213099_c");
					ReflectionHelper.setStaticFinalField(f3, MainMenuScreen.class, RenderUtils.getBlankImageResource());
				} else {
					//Resetting the background skybox to the original one if no background animation can be rendered.
					//I would reset the overlay too, but changing this damn value multiple times at runtime causes the overlay texture to
					//get stuck at one state and not changing correcly. I think it happens because of some sort of texture caching made by the render engine
					//or the fact that its, or was, a static final field, but that doesn't really makes much sense.
					if (this.cachedSkybox != null) {
						Field f = ObfuscationReflectionHelper.findField(MainMenuScreen.class, "field_209101_K");
						ReflectionHelper.setField(f, e.getGui(), this.cachedSkybox);
					}
				}

				//Setting blank textures to the title- and edition-textures if "hidelogo" is true 
				if (FancyMenu.config.getOrDefault("hidelogo", true)) {
					Field f = ObfuscationReflectionHelper.findField(MainMenuScreen.class, "field_110352_y");
					Field f2 = ObfuscationReflectionHelper.findField(MainMenuScreen.class, "field_194400_H");
					ReflectionHelper.setStaticFinalField(f, MainMenuScreen.class, RenderUtils.getBlankImageResource());
					ReflectionHelper.setStaticFinalField(f2, MainMenuScreen.class, RenderUtils.getBlankImageResource());
				}
				
				if (FancyMenu.config.getOrDefault("hidesplashtext", true)) {
					Field f5 = ObfuscationReflectionHelper.findField(MainMenuScreen.class, "field_73975_c");
					ReflectionHelper.setField(f5, e.getGui(), " ");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			widgetsRaw.clear();
			widgets.clear();
			
			if (this.canRenderBackground()) {
				if (this.shouldFadeInButtons()) {
					cacheWidgets(e);
					
					List<Integer> ints = new ArrayList<Integer>();
					ints.addAll(widgetsRaw.keySet());

					//Sorting all buttons by its group (height)
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
			IngameGui.blit(e.getGui().width / 2 - 100, e.getGui().height - 35, 0.0F, 0.0F, 15, 15, 15, 15);

			Minecraft.getInstance().getTextureManager().bindTexture(TWITTER);
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, MathHelper.clamp(fadeFooter, 0.1F, 1.0F));
			IngameGui.blit(e.getGui().width / 2 - 30, e.getGui().height - 35, 0.0F, 0.0F, 15, 15, 15, 15);

			Minecraft.getInstance().getTextureManager().bindTexture(INSTAGRAM);
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, MathHelper.clamp(fadeFooter, 0.1F, 1.0F));
			IngameGui.blit(e.getGui().width / 2 + 40, e.getGui().height - 35, 0.0F, 0.0F, 15, 15, 15, 15);
			
			RenderSystem.disableBlend();

			if (fadeFooter < 1.0F) {
				fadeFooter += 0.005F;
			}
		}
	}
}

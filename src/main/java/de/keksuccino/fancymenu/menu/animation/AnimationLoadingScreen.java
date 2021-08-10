package de.keksuccino.fancymenu.menu.animation;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.matrix.MatrixStack;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.gui.screens.SimpleLoadingScreen;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;

public class AnimationLoadingScreen extends SimpleLoadingScreen {

	private Screen fallback;
	private List<IAnimationRenderer> renderers = new ArrayList<IAnimationRenderer>();
	private boolean ready = false;
	private int cachedFPS;
	private boolean cachedLoop;
	private boolean done = false;
	private volatile boolean preparing = false;
	private String specialString;

	public AnimationLoadingScreen(@Nullable Screen fallbackGui, IAnimationRenderer... renderer) {
		super(Minecraft.getInstance());
		this.renderers.addAll(Arrays.asList(renderer));
		this.fallback = fallbackGui;
		
		this.setDarkmode(FancyMenu.config.getOrDefault("loadingscreendarkmode", false));

		String defaultColor = "#ffffffff";
		String hex = FancyMenu.config.getOrDefault("loadinganimationcolor", defaultColor);
		Color c = RenderUtils.getColorFromHexString(hex);
		if (c != null) {
			this.setLoadingAnimationColor(hex);
		} else {
			this.setLoadingAnimationColor(defaultColor);
		}

		this.specialString = getVerySpecialString();
	}

	@Override
	public void render(MatrixStack matrix, int p_render_1_, int p_render_2_, float p_render_3_) {
		IAnimationRenderer current = this.getCurrentRenderer();
		
		if (current == null) {
			this.done = true;
			this.onFinished();
			if (this.fallback != null) {
				Minecraft.getInstance().displayGuiScreen(this.fallback);
				MenuCustomization.reloadCurrentMenu();
			}
		} else {
			if (!this.ready) {
				this.cachedFPS = current.getFPS();
				this.cachedLoop = current.isGettingLooped();
				current.setFPS(-1);
				current.setLooped(false);
				this.ready = true;
			}
			
			if (!current.isReady()) {
				if (!this.preparing) {
					AnimationLoadingScreen loading = this;
					this.preparing = true;
					new Thread(new Runnable() {
						@Override
						public void run() {
							if (FancyMenu.config.getOrDefault("showanimationloadingstatus", true)) {
								loading.setStatusText(Locals.localize("loading.animation.loadingframes", current.getPath().replace("/", ".").replace("\\", ".")));
							}
							current.prepareAnimation();
							System.gc();
							loading.preparing = false;
						}
					}).start();
				}
			} else {
				if (!current.isFinished()) {
					if (FancyMenu.config.getOrDefault("showanimationloadingstatus", true)) {
						this.setStatusText(Locals.localize("loading.animation.prerendering", current.getPath().replace("/", ".").replace("\\", ".")));
					}
					if (current instanceof AdvancedAnimation) {
						((AdvancedAnimation)current).setMuteAudio(true);
					}
					current.render(matrix);
					if (current instanceof AdvancedAnimation) {
						((AdvancedAnimation)current).setMuteAudio(false);
					}
				} else {
					current.setFPS(this.cachedFPS);
					current.setLooped(this.cachedLoop);
					current.resetAnimation();
					this.renderers.remove(0);
					this.ready = false;
				}
			}
		}
		
		super.render(matrix, p_render_1_, p_render_2_, p_render_3_);

		if (this.specialString != null) {
			FontRenderer r = Minecraft.getInstance().fontRenderer;
			int i = r.getStringWidth(this.specialString) / 2;
			r.drawStringWithShadow(matrix, this.specialString, (this.width / 2) - i, 20, Color.WHITE.getRGB());
		}
		
	}

	private IAnimationRenderer getCurrentRenderer() {
		if (!this.renderers.isEmpty()) {
			IAnimationRenderer r = renderers.get(0);
			if (r instanceof ResourcePackAnimationRenderer) {
				this.renderers.remove(0);
				return this.getCurrentRenderer();
			}
			if (r instanceof AdvancedAnimation) {
				IAnimationRenderer main = ((AdvancedAnimation)r).getMainAnimationRenderer();
				if (main == null) {
					return null;
				}
				if (main instanceof ResourcePackAnimationRenderer) {
					this.renderers.remove(0);
					return this.getCurrentRenderer();
				}
			}
			return r;
		}
		return null;
	}
	
	public void onFinished() {
		this.setStatusText(Locals.localize("loading.animation.done"));
	}

	public boolean loadingFinished() {
		return this.done;
	}

	private static String getVerySpecialString() {
		try {
			
			Calendar c = Calendar.getInstance();
			int day = c.get(Calendar.DAY_OF_MONTH);
			int month = c.get(Calendar.MONTH) + 1;
			int hour = c.get(Calendar.HOUR_OF_DAY);
			int minute = c.get(Calendar.MINUTE);
			int second = c.get(Calendar.SECOND);
			
			if ((day == 14) && (month == 2)) {
				return "§dHappy Valentine's Day! ❤";
			}
			if ((day == 24) && (month == 12)) {
				return "§cMerry Christmas!";
			}
			if ((hour == 0) && (minute == 0)) {
				return "§4OoOoOoOoOoOoO Spoooooky!";
			}
			if ((day == 1) && (month == 1)) {
				return "§dHappy New Year!";
			}
			if ((day == 31) && (month == 10)) {
				return "§6Happy Halloweeeeen!";
			}
			if ((day == 7) && (month == 7)) {
				return "§dHappy Tanabata! 幸せ七夕！";
			}
			if ((day == 21) && (month == 2) && (hour == 19)) {
				return "§bAvatar State, Yip Yip!";
			}
			if ((hour == 16) && (minute == 20) && (second >= 0) && (second <= 3)) {
				return "§2420";
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
}

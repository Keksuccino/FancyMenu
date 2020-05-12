package de.keksuccino.fancymenu.menu.fancy.loadingdarkmode;

import java.awt.Color;
import java.lang.reflect.Field;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.LoadingGui;
import net.minecraft.client.gui.ResourceLoadProgressGui;
import net.minecraft.resources.IAsyncReloader;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class DarkResourceLoadingScreen extends ResourceLoadProgressGui {

	private static final ResourceLocation MOJANG_LOGO_TEXTURE = new ResourceLocation("keksuccino", "mojang_dark.png");
	private IAsyncReloader asyncReloader;
	private boolean reloading;
	private Runnable callback;
	private float f1;
	private long l1 = -1L;
	private long l2 = -1L;
	
	public DarkResourceLoadingScreen(Minecraft mc, IAsyncReloader reloader, Runnable callback, boolean reloading) {
		super(mc, reloader, callback, reloading);
		this.asyncReloader = reloader;
		this.reloading = reloading;
		this.callback = callback;
	}

	@Override
	public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
		Minecraft mc = Minecraft.getInstance();
		int i = mc.mainWindow.getScaledWidth();
		int j = mc.mainWindow.getScaledHeight();
		long k = Util.milliTime();
		if (this.reloading && (this.asyncReloader.asyncPartDone() || mc.currentScreen != null) && this.l2 == -1L) {
			this.l2 = k;
		}

		int color = new Color(26, 26, 26).getRGB();
		float f = this.l1 > -1L ? (float)(k - this.l1) / 1000.0F : -1.0F;
		float f1 = this.l2 > -1L ? (float)(k - this.l2) / 500.0F : -1.0F;
		float f2;
		if (f >= 1.0F) {
			if (mc.currentScreen != null) {
				mc.currentScreen.render(0, 0, p_render_3_);
			}

			int l = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
			fill(0, 0, i, j, color | l << 24);
			f2 = 1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F);
		} else if (this.reloading) {
			if (mc.currentScreen != null && f1 < 1.0F) {
				mc.currentScreen.render(p_render_1_, p_render_2_, p_render_3_);
			}

			int j1 = MathHelper.ceil(MathHelper.clamp((double)f1, 0.15D, 1.0D) * 255.0D);
			fill(0, 0, i, j, color | j1 << 24);
			f2 = MathHelper.clamp(f1, 0.0F, 1.0F);
		} else {
			fill(0, 0, i, j, color);
			f2 = 1.0F;
		}

		int k1 = (mc.mainWindow.getScaledWidth() - 256) / 2;
		int i1 = (mc.mainWindow.getScaledHeight() - 256) / 2;
		mc.getTextureManager().bindTexture(MOJANG_LOGO_TEXTURE);
		GlStateManager.enableBlend();
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, f2);
		this.blit(k1, i1, 0, 0, 256, 256);
		float f3 = this.asyncReloader.estimateExecutionSpeed();
		this.f1 = MathHelper.clamp(this.f1 * 0.95F + f3 * 0.050000012F, 0.0F, 1.0F);
		net.minecraftforge.fml.client.ClientModLoader.renderProgressText();
		if (f < 1.0F) {
			this.renderProgressBar(i / 2 - 150, j / 4 * 3, i / 2 + 150, j / 4 * 3 + 10, 1.0F - MathHelper.clamp(f, 0.0F, 1.0F));
		}

		if (f >= 2.0F) {
			mc.setLoadingGui((LoadingGui)null);
		}

		if (this.l1 == -1L && this.asyncReloader.fullyDone() && (!this.reloading || f1 >= 2.0F)) {
	         this.asyncReloader.join();
	         this.l1 = Util.milliTime();
	         this.callback.run();
	         if (mc.currentScreen != null) {
	            mc.currentScreen.init(mc, mc.mainWindow.getScaledWidth(), mc.mainWindow.getScaledHeight());
	         }
	      }

	}
	
	private void renderProgressBar(int p_228181_1_, int p_228181_2_, int p_228181_3_, int p_228181_4_, float p_228181_5_) {
		int i = MathHelper.ceil((float)(p_228181_3_ - p_228181_1_ - 1) * this.f1);
		fill(p_228181_1_ - 1, p_228181_2_ - 1, p_228181_3_ + 1, p_228181_4_ + 1, -16777216 | Math.round((1.0F - p_228181_5_) * 255.0F) << 16 | Math.round((1.0F - p_228181_5_) * 255.0F) << 8 | Math.round((1.0F - p_228181_5_) * 255.0F));
		fill(p_228181_1_, p_228181_2_, p_228181_3_, p_228181_4_, new Color(26, 26, 26).getRGB());
		fill(p_228181_1_ + 1, p_228181_2_ + 1, p_228181_1_ + i, p_228181_4_ - 1, -16777216 | (int)MathHelper.lerp(1.0F - p_228181_5_, 226.0F, 255.0F) << 16 | (int)MathHelper.lerp(1.0F - p_228181_5_, 40.0F, 255.0F) << 8 | (int)MathHelper.lerp(1.0F - p_228181_5_, 55.0F, 255.0F));
	}
	
	protected static Runnable getCallback(ResourceLoadProgressGui screen) {
		try {
			Field f = ObfuscationReflectionHelper.findField(ResourceLoadProgressGui.class, "field_212976_d");
			return (Runnable) f.get(screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected static IAsyncReloader getReloader(ResourceLoadProgressGui screen) {
		try {
			Field f = ObfuscationReflectionHelper.findField(ResourceLoadProgressGui.class, "field_212975_c");
			return (IAsyncReloader) f.get(screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected static boolean getReloading(ResourceLoadProgressGui screen) {
		try {
			Field f = ObfuscationReflectionHelper.findField(ResourceLoadProgressGui.class, "field_212977_e");
			return (boolean) f.get(screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}

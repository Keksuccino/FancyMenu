package de.keksuccino.fancymenu.menu.fancy.loadingdarkmode;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Consumer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.LoadingGui;
import net.minecraft.client.gui.ResourceLoadProgressGui;
import net.minecraft.resources.IAsyncReloader;
import net.minecraft.util.ColorHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class DarkResourceLoadingScreen extends ResourceLoadProgressGui {

	private static final ResourceLocation MOJANG_LOGO_TEXTURE = new ResourceLocation("textures/gui/title/mojangstudios.png");
	private IAsyncReloader asyncReloader;
	private boolean reloading;
	private Consumer<Optional<Throwable>> callback;
	private float progress;
	private long l1 = -1L;
	private long l2 = -1L;
	
	public DarkResourceLoadingScreen(Minecraft mc, IAsyncReloader reloader, Consumer<Optional<Throwable>> callback, boolean reloading) {
		super(mc, reloader, callback, reloading);
		this.asyncReloader = reloader;
		this.reloading = reloading;
		this.callback = callback;
	}

	//render
	@Override
	public void func_230430_a_(MatrixStack matrix, int p_render_1_, int p_render_2_, float p_render_3_) {
		Minecraft mc = Minecraft.getInstance();
		int i = mc.getMainWindow().getScaledWidth();
		int j = mc.getMainWindow().getScaledHeight();
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
				mc.currentScreen.func_230430_a_(matrix, 0, 0, p_render_3_);
			}

			int l = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
			func_238467_a_(matrix, 0, 0, i, j, color | l << 24);
			f2 = 1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F);
		} else if (this.reloading) {
			if (mc.currentScreen != null && f1 < 1.0F) {
				mc.currentScreen.func_230430_a_(matrix, p_render_1_, p_render_2_, p_render_3_);
			}

			int j1 = MathHelper.ceil(MathHelper.clamp((double)f1, 0.15D, 1.0D) * 255.0D);
			func_238467_a_(matrix, 0, 0, i, j, color | j1 << 24);
			f2 = MathHelper.clamp(f1, 0.0F, 1.0F);
		} else {
			func_238467_a_(matrix, 0, 0, i, j, color);
			f2 = 1.0F;
		}

		int j2 = (int)((double)mc.getMainWindow().getScaledWidth() * 0.5D);
		int i1 = (int)((double)mc.getMainWindow().getScaledHeight() * 0.5D);
		double d0 = Math.min((double)mc.getMainWindow().getScaledWidth() * 0.75D, (double)mc.getMainWindow().getScaledHeight()) * 0.25D;
		int j1 = (int)(d0 * 0.5D);
		double d1 = d0 * 4.0D;
		int k1 = (int)(d1 * 0.5D);
		
		mc.getTextureManager().bindTexture(MOJANG_LOGO_TEXTURE);
		RenderSystem.enableBlend();
		RenderSystem.blendEquation(32774);
		RenderSystem.blendFunc(770, 1);
		RenderSystem.alphaFunc(516, 0.0F);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, f2);
		func_238466_a_(matrix, j2 - k1, i1 - j1, k1, (int)d0, -0.0625F, 0.0F, 120, 60, 120, 120);
		func_238466_a_(matrix, j2, i1 - j1, k1, (int)d0, 0.0625F, 60.0F, 120, 60, 120, 120);
		RenderSystem.defaultBlendFunc();
		RenderSystem.defaultAlphaFunc();
		RenderSystem.disableBlend();
		
		float f3 = this.asyncReloader.estimateExecutionSpeed();
		this.progress = MathHelper.clamp(this.progress * 0.95F + f3 * 0.050000012F, 0.0F, 1.0F);
		//TODO progress text an darkmode anpassen
		net.minecraftforge.fml.client.ClientModLoader.renderProgressText();
		int l2 = (int)((double)mc.getMainWindow().getScaledHeight() * 0.8325D);
		if (f < 1.0F) {
			this.renderProgressBar(matrix, i / 2 - k1, l2 - 5, i / 2 + k1, l2 + 5, 1.0F - MathHelper.clamp(f, 0.0F, 1.0F));
		}

		if (f >= 2.0F) {
			mc.setLoadingGui((LoadingGui)null);
		}

		if (this.l1 == -1L && this.asyncReloader.fullyDone() && (!this.reloading || f1 >= 2.0F)) {
			try {
				this.asyncReloader.join();
				this.callback.accept(Optional.empty());
			} catch (Throwable throwable) {
				this.callback.accept(Optional.of(throwable));
			}

			this.l1 = Util.milliTime();
			if (mc.currentScreen != null) {
				//init
				mc.currentScreen.func_231158_b_(mc, mc.getMainWindow().getScaledWidth(), mc.getMainWindow().getScaledHeight());
			}
		}

	}
	
	private void renderProgressBar(MatrixStack p_238629_1_, int p_238629_2_, int p_238629_3_, int p_238629_4_, int p_238629_5_, float p_238629_6_) {
		int i = MathHelper.ceil((float)(p_238629_4_ - p_238629_2_ - 2) * this.progress);
		int j = Math.round(p_238629_6_ * 255.0F);
		int k = ColorHelper.PackedColor.func_233006_a_(j, 255, 255, 255);
		func_238467_a_(p_238629_1_, p_238629_2_ + 1, p_238629_3_, p_238629_4_ - 1, p_238629_3_ + 1, k);
		func_238467_a_(p_238629_1_, p_238629_2_ + 1, p_238629_5_, p_238629_4_ - 1, p_238629_5_ - 1, k);
		func_238467_a_(p_238629_1_, p_238629_2_, p_238629_3_, p_238629_2_ + 1, p_238629_5_, k);
		func_238467_a_(p_238629_1_, p_238629_4_, p_238629_3_, p_238629_4_ - 1, p_238629_5_, k);
		func_238467_a_(p_238629_1_, p_238629_2_ + 2, p_238629_3_ + 2, p_238629_2_ + i, p_238629_5_ - 2, k);
	}
	
	protected static Consumer<Optional<Throwable>> getCallback(ResourceLoadProgressGui screen) {
		try {
			Field f = ObfuscationReflectionHelper.findField(ResourceLoadProgressGui.class, "field_212976_d");
			return (Consumer<Optional<Throwable>>) f.get(screen);
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

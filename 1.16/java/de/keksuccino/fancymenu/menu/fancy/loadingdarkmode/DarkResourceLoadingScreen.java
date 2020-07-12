package de.keksuccino.fancymenu.menu.fancy.loadingdarkmode;

import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.stb.STBEasyFont;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
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
import net.minecraftforge.fml.loading.progress.StartupMessageManager;

public class DarkResourceLoadingScreen extends ResourceLoadProgressGui {

	private static final ResourceLocation MOJANG_LOGO_TEXTURE = new ResourceLocation("textures/gui/title/mojangstudios.png");
	private IAsyncReloader asyncReloader;
	private boolean reloading;
	private Consumer<Optional<Throwable>> callback;
	private float progress;
	private long fadeOutStart = -1L;
	private long fadeInStart = -1L;
	
	public DarkResourceLoadingScreen(Minecraft mc, IAsyncReloader reloader, Consumer<Optional<Throwable>> callback, boolean reloading) {
		super(mc, reloader, callback, reloading);
		this.asyncReloader = reloader;
		this.reloading = reloading;
		this.callback = callback;
	}

	@Override
	public void render(MatrixStack matrix, int p_render_1_, int p_render_2_, float p_render_3_) {
		Minecraft mc = Minecraft.getInstance();
		int i = mc.getMainWindow().getScaledWidth();
		int j = mc.getMainWindow().getScaledHeight();
		long k = Util.milliTime();
		if (this.reloading && (this.asyncReloader.asyncPartDone() || mc.currentScreen != null) && this.fadeInStart == -1L) {
			this.fadeInStart = k;
		}

		int color = new Color(26, 26, 26).getRGB();
		float f = this.fadeOutStart > -1L ? (float)(k - this.fadeOutStart) / 1000.0F : -1.0F;
		float f1 = this.fadeInStart > -1L ? (float)(k - this.fadeInStart) / 500.0F : -1.0F;
		float f2;
		if (f >= 1.0F) {
			if (mc.currentScreen != null) {
				mc.currentScreen.render(matrix, 0, 0, p_render_3_);
			}

			int l = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
			fill(matrix, 0, 0, i, j, color | l << 24);
			f2 = 1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F);
		} else if (this.reloading) {
			if (mc.currentScreen != null && f1 < 1.0F) {
				mc.currentScreen.render(matrix, p_render_1_, p_render_2_, p_render_2_);
			}

			int i2 = MathHelper.ceil(MathHelper.clamp((double)f1, 0.15D, 1.0D) * 255.0D);
			fill(matrix, 0, 0, i, j, color | i2 << 24);
			f2 = MathHelper.clamp(f1, 0.0F, 1.0F);
		} else {
			fill(matrix, 0, 0, i, j, color);
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
		blit(matrix, j2 - k1, i1 - j1, k1, (int)d0, -0.0625F, 0.0F, 120, 60, 120, 120);
		blit(matrix, j2, i1 - j1, k1, (int)d0, 0.0625F, 60.0F, 120, 60, 120, 120);
		RenderSystem.defaultBlendFunc();
		RenderSystem.defaultAlphaFunc();
		RenderSystem.disableBlend();
		int l1 = (int)((double)mc.getMainWindow().getScaledHeight() * 0.8325D);
		float f3 = this.asyncReloader.estimateExecutionSpeed();
		this.progress = MathHelper.clamp(this.progress * 0.95F + f3 * 0.050000012F, 0.0F, 1.0F);

		this.renderForgeStatusMessages();
		
		if (f < 1.0F) {
			this.renderProgressBar(matrix, i / 2 - k1, l1 - 5, i / 2 + k1, l1 + 5, 1.0F - MathHelper.clamp(f, 0.0F, 1.0F));
		}

		if (f >= 2.0F) {
			mc.setLoadingGui((LoadingGui)null);
		}

		if (this.fadeOutStart == -1L && this.asyncReloader.fullyDone() && (!this.reloading || f1 >= 2.0F)) {
			this.fadeOutStart = Util.milliTime();
			try {
				this.asyncReloader.join();
				this.callback.accept(Optional.empty());
			} catch (Throwable throwable) {
				this.callback.accept(Optional.of(throwable));
			}

			if (mc.currentScreen != null) {
				mc.currentScreen.init(mc, mc.getMainWindow().getScaledWidth(), mc.getMainWindow().getScaledHeight());
			}
		}

	}
	
	private void renderProgressBar(MatrixStack matrix, int p_238629_2_, int p_238629_3_, int p_238629_4_, int p_238629_5_, float p_238629_6_) {
		int i = MathHelper.ceil((float)(p_238629_4_ - p_238629_2_ - 2) * this.progress);
		int j = Math.round(p_238629_6_ * 255.0F);
		int k = ColorHelper.PackedColor.packColor(j, 255, 255, 255);
		fill(matrix, p_238629_2_ + 1, p_238629_3_, p_238629_4_ - 1, p_238629_3_ + 1, k);
		fill(matrix, p_238629_2_ + 1, p_238629_5_, p_238629_4_ - 1, p_238629_5_ - 1, k);
		fill(matrix, p_238629_2_, p_238629_3_, p_238629_2_ + 1, p_238629_5_, k);
		fill(matrix, p_238629_4_, p_238629_3_, p_238629_4_ - 1, p_238629_5_, k);
		fill(matrix, p_238629_2_ + 2, p_238629_3_ + 2, p_238629_2_ + i, p_238629_5_ - 2, k);
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
	
	//Forge status render stuff ---->
	private static float[] forgestatuscolor = new float[] {1.0f, 1.0f, 1.0f};
	private void renderForgeStatusMessages() {
		List<Pair<Integer, StartupMessageManager.Message>> messages = StartupMessageManager.getMessages();
        for (int i = 0; i < messages.size(); i++) {
            boolean nofade = i == 0;
            final Pair<Integer, StartupMessageManager.Message> pair = messages.get(i);
            final float fade = MathHelper.clamp((4000.0f - (float) pair.getLeft() - ( i - 4 ) * 1000.0f) / 5000.0f, 0.0f, 1.0f);
            if (fade <0.01f && !nofade) continue;
            StartupMessageManager.Message msg = pair.getRight();
            renderForgeMessage(msg.getText(), forgestatuscolor, ((Minecraft.getInstance().getMainWindow().getScaledHeight() - 15) / 10) - i + 1, nofade ? 1.0f : fade);
        }
        renderForgeMemoryInfo();
    }

	private void renderForgeMessage(final String message, final float[] colour, int line, float alpha) {
		GlStateManager.enableClientState(GL11.GL_VERTEX_ARRAY);
        ByteBuffer charBuffer = MemoryUtil.memAlloc(message.length() * 270);
        int quads = STBEasyFont.stb_easy_font_print(0, 0, message, null, charBuffer);
        GL14.glVertexPointer(2, GL11.GL_FLOAT, 16, charBuffer);

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        // STBEasyFont's quads are in reverse order or what OGGL expects, so it gets culled for facing the wrong way. 
        // So Disable culling https://github.com/MinecraftForge/MinecraftForge/pull/6824
        RenderSystem.disableCull(); 
        GL14.glBlendColor(0,0,0, alpha);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.CONSTANT_ALPHA, GlStateManager.DestFactor.ONE_MINUS_CONSTANT_ALPHA);
        RenderSystem.color3f(colour[0],colour[1],colour[2]);
        RenderSystem.pushMatrix();
        RenderSystem.translatef(10, line * 10, 0);
        RenderSystem.scalef(1, 1, 0);
        RenderSystem.drawArrays(GL11.GL_QUADS, 0, quads * 4);
        RenderSystem.popMatrix();

        RenderSystem.enableCull();
        GlStateManager.disableClientState(GL11.GL_VERTEX_ARRAY);
        MemoryUtil.memFree(charBuffer);
    }
	
	private static float[] memorycolour = new float[] {0.0f, 0.0f, 0.0f};
    private void renderForgeMemoryInfo() {
    	final MemoryUsage heapusage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        final MemoryUsage offheapusage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        final float pctmemory = (float) heapusage.getUsed() / heapusage.getMax();
        String memory = String.format("Memory Heap: %d / %d MB (%.1f%%)  OffHeap: %d MB", heapusage.getUsed() >> 20, heapusage.getMax() >> 20, pctmemory * 100.0, offheapusage.getUsed() >> 20);

        final int i = MathHelper.hsvToRGB((1.0f - (float)Math.pow(pctmemory, 1.5f)) / 3f, 1.0f, 0.5f);
        memorycolour[2] = ((i) & 0xFF) / 255.0f;
        memorycolour[1] = ((i >> 8 ) & 0xFF) / 255.0f;
        memorycolour[0] = ((i >> 16 ) & 0xFF) / 255.0f;
        renderForgeMessage(memory, memorycolour, 1, 1.0f);
    }

}

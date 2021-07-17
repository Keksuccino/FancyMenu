//TODO übernehmen
package de.keksuccino.fancymenu.mixin.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.SplashScreenRenderEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.resources.IAsyncReloader;
import net.minecraft.util.ColorHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.loading.progress.StartupMessageManager;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.stb.STBEasyFont;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.ResourceLoadProgressGui;

import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(value = ResourceLoadProgressGui.class)
public abstract class MixinResourceLoadProgressGui extends AbstractGui {

	private static final ResourceLocation MOJANG_LOGO_TEXTURE = new ResourceLocation("textures/gui/title/mojangstudios.png");
	private static final int BACKGROUND_COLOR = ColorHelper.PackedColor.packColor(255, 239, 50, 61);
	private static final int BACKGROUND_COLOR_2 = BACKGROUND_COLOR & 16777215;

	private static final float[] MEM_COLOR = new float[] { 0.0f, 0.0f, 0.0f};

	protected Minecraft mc = Minecraft.getInstance();
	protected MainWindow window = Minecraft.getInstance().getMainWindow();

	@Shadow @Final private IAsyncReloader asyncReloader;
	@Shadow @Final private Consumer<Optional<Throwable>> completedCallback;
	@Shadow @Final private boolean reloading;
	@Shadow private long fadeOutStart;
	@Shadow private long fadeInStart;
	@Shadow private float progress;

//	protected SplashCustomizationLayerScreen customizationLayer = new SplashCustomizationLayerScreen();
//	protected boolean initDone = false;
//	protected int lastWidth = 0;
//	protected int lastHeight = 0;

	@Inject(at = @At("HEAD"), method = "render", cancellable = true)
	protected void onRender(MatrixStack matrix, int mouseX, int mouseY, float partialTicks, CallbackInfo info) {
		info.cancel();

		int i = this.mc.getMainWindow().getScaledWidth();
		int j = this.mc.getMainWindow().getScaledHeight();
		long k = Util.milliTime();

		SplashScreenRenderEvent.Pre event = new SplashScreenRenderEvent.Pre((ResourceLoadProgressGui) ((Object)this), matrix, mouseX, mouseY, partialTicks, i, j);
		MinecraftForge.EVENT_BUS.post(event);
		if (event.isCanceled()) {
			return;
		}

//		//Handle customization layer (re-)initialization
//		if ((lastWidth != i) || (lastHeight != j)) {
//			initDone = false;
//		}
//		lastWidth = i;
//		lastHeight = j;
//		if (!initDone) {
//			customizationLayer.init(Minecraft.getInstance(), i, j);
//			initDone = true;
//		}

		if (this.reloading && (this.asyncReloader.asyncPartDone() || this.mc.currentScreen != null) && this.fadeInStart == -1L) {
			this.fadeInStart = k;
		}

		//Variables for dark mode and custom background color
		int darkBackground = new Color(26, 26, 26).getRGB();
		boolean isDarkmode = FancyMenu.config.getOrDefault("loadingscreendarkmode", false);
		boolean isCustomBackground = event.getBackgroundColor() != -1;

		float f = this.fadeOutStart > -1L ? (float)(k - this.fadeOutStart) / 1000.0F : -1.0F;
		float f1 = this.fadeInStart > -1L ? (float)(k - this.fadeInStart) / 500.0F : -1.0F;
		float f2;
		if (f >= 1.0F) {
			if (this.mc.currentScreen != null) {
				this.mc.currentScreen.render(matrix, 0, 0, partialTicks);
			}

			int l = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
			//Disabling background transparency when fading to customizable menus to prevent layout bugs
			if (MenuCustomization.isMenuCustomizable(mc.currentScreen)) {
				l = 255;
			}
			//Handle background color
			if (isCustomBackground) {
				fill(matrix, 0, 0, i, j, withAlpha(event.getBackgroundColor(), l));
			} else if (isDarkmode) {
				fill(matrix, 0, 0, i, j, withAlpha(darkBackground, l));
			} else {
				fill(matrix, 0, 0, i, j, withAlpha(BACKGROUND_COLOR_2, l));
			}
			f2 = 1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F);
		} else if (this.reloading) {
			if (this.mc.currentScreen != null && f1 < 1.0F) {
				this.mc.currentScreen.render(matrix, mouseX, mouseY, partialTicks);
			}

			int i2 = MathHelper.ceil(MathHelper.clamp(f1, 0.15D, 1.0D) * 255.0D);
			//Disabling background transparency when fading to customizable menus to prevent layout bugs
			if (MenuCustomization.isMenuCustomizable(mc.currentScreen)) {
				i2 = 255;
			}
			//Handle background color
			if (isCustomBackground) {
				fill(matrix, 0, 0, i, j, withAlpha(event.getBackgroundColor(), i2));
			} else if (isDarkmode) {
				fill(matrix, 0, 0, i, j, withAlpha(darkBackground, i2));
			} else {
				fill(matrix, 0, 0, i, j, withAlpha(BACKGROUND_COLOR_2, i2));
			}
			f2 = MathHelper.clamp(f1, 0.0F, 1.0F);
		} else {
			//Handle background color
			if (isCustomBackground) {
				fill(matrix, 0, 0, i, j, event.getBackgroundColor());
			} else if (isDarkmode) {
				fill(matrix, 0, 0, i, j, darkBackground);
			} else {
				fill(matrix, 0, 0, i, j, BACKGROUND_COLOR);
			}
			f2 = 1.0F;
		}

		int j2 = (int)((double)this.mc.getMainWindow().getScaledWidth() * 0.5D);
		int i1 = (int)((double)this.mc.getMainWindow().getScaledHeight() * 0.5D);
		double d0 = Math.min((double)this.mc.getMainWindow().getScaledWidth() * 0.75D, this.mc.getMainWindow().getScaledHeight()) * 0.25D;
		int j1 = (int)(d0 * 0.5D);
		double d1 = d0 * 4.0D;
		int k1 = (int)(d1 * 0.5D);
		if (event.isRenderLogo()) {
			this.mc.getTextureManager().bindTexture(MOJANG_LOGO_TEXTURE);
			RenderSystem.enableBlend();
			RenderSystem.blendEquation(32774);
			RenderSystem.blendFunc(770, 1);
			RenderSystem.alphaFunc(516, 0.0F);
			//Disabling logo transparency when fading to customizable menus to prevent layout bugs
			if (MenuCustomization.isMenuCustomizable(mc.currentScreen)) {
				f2 = 1.0F;
			}
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, f2);
			blit(matrix, j2 - k1, i1 - j1, k1, (int)d0, -0.0625F, 0.0F, 120, 60, 120, 120);
			blit(matrix, j2, i1 - j1, k1, (int)d0, 0.0625F, 60.0F, 120, 60, 120, 120);
			RenderSystem.defaultBlendFunc();
			RenderSystem.defaultAlphaFunc();
			RenderSystem.disableBlend();
		}
		int l1 = (int)((double)this.mc.getMainWindow().getScaledHeight() * 0.8325D);
		float f3 = this.asyncReloader.estimateExecutionSpeed();
		this.progress = MathHelper.clamp(this.progress * 0.95F + f3 * 0.050000012F, 0.0F, 1.0F);
		if (event.isRenderForgeText()) {
			renderMessages(isDarkmode, MenuCustomization.isMenuCustomizable(mc.currentScreen));
		}
		if (f < 1.0F) {
			if (event.isRenderBar()) {
				float barTransparency = 1.0F - MathHelper.clamp(f, 0.0F, 1.0F);
				//Disabling bar transparency when fading to customizable menus to prevent layout bugs
				if (MenuCustomization.isMenuCustomizable(mc.currentScreen)) {
					barTransparency = 1.0F;
				}
				this.func_238629_a_(matrix, i / 2 - k1, l1 - 5, i / 2 + k1, l1 + 5, barTransparency);
			}
		}

		if (f >= 2.0F) {
			this.mc.setLoadingGui(null);
		}

		if (this.fadeOutStart == -1L && this.asyncReloader.fullyDone() && (!this.reloading || f1 >= 2.0F)) {
			this.fadeOutStart = Util.milliTime();
			try {
				this.asyncReloader.join();
				this.completedCallback.accept(Optional.empty());
			} catch (Throwable throwable) {
				this.completedCallback.accept(Optional.of(throwable));
			}

			if (this.mc.currentScreen != null) {
				this.mc.currentScreen.init(this.mc, this.mc.getMainWindow().getScaledWidth(), this.mc.getMainWindow().getScaledHeight());
			}
		}

		SplashScreenRenderEvent.Post event2 = new SplashScreenRenderEvent.Post((ResourceLoadProgressGui) ((Object)this), matrix, mouseX, mouseY, partialTicks, i, j);
		event2.setRenderBar(event.isRenderBar());
		event2.setRenderLogo(event.isRenderLogo());
		event2.setBackgroundColor(event.getBackgroundColor());
		event2.setRenderForgeText(event.isRenderForgeText());
		MinecraftForge.EVENT_BUS.post(event2);

	}

	private static int withAlpha(int color, int alpha) {
		return color & 16777215 | alpha << 24;
	}

	@Shadow protected abstract void func_238629_a_(MatrixStack p_238629_1_, int p_238629_2_, int p_238629_3_, int p_238629_4_, int p_238629_5_, float p_238629_6_);

	protected void renderMessages(boolean darkMode, boolean isFade) {
		List<Pair<Integer, StartupMessageManager.Message>> messages = StartupMessageManager.getMessages();
		for (int i = 0; i < messages.size(); i++) {
			boolean nofade = i == 0;
			//Disable fading when currentMenu is customizable to prevent layout render bugs
			if (!isFade) {
				nofade = true;
			}
			final Pair<Integer, StartupMessageManager.Message> pair = messages.get(i);
			final float fade = MathHelper.clamp((4000.0f - (float) pair.getLeft() - ( i - 4 ) * 1000.0f) / 5000.0f, 0.0f, 1.0f);
			if (fade <0.01f && !nofade) continue;
			StartupMessageManager.Message msg = pair.getRight();
			//Render messages in white when dark mode is enabled
			if (darkMode) {
				renderMessage(msg.getText(), new float[]{255,255,255}, ((window.getScaledHeight() - 15) / 10) - i + 1, nofade ? 1.0f : fade);
			} else {
				renderMessage(msg.getText(), msg.getTypeColour(), ((window.getScaledHeight() - 15) / 10) - i + 1, nofade ? 1.0f : fade);
			}
		}
		renderMemoryInfo();
	}

	protected void renderMemoryInfo() {
		final MemoryUsage heapusage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
		final MemoryUsage offheapusage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
		final float pctmemory = (float) heapusage.getUsed() / heapusage.getMax();
		String memory = String.format("Memory Heap: %d / %d MB (%.1f%%)  OffHeap: %d MB", heapusage.getUsed() >> 20, heapusage.getMax() >> 20, pctmemory * 100.0, offheapusage.getUsed() >> 20);

		final int i = MathHelper.hsvToRGB((1.0f - (float)Math.pow(pctmemory, 1.5f)) / 3f, 1.0f, 0.5f);
		MEM_COLOR[2] = ((i) & 0xFF) / 255.0f;
		MEM_COLOR[1] = ((i >> 8 ) & 0xFF) / 255.0f;
		MEM_COLOR[0] = ((i >> 16 ) & 0xFF) / 255.0f;
		renderMessage(memory, MEM_COLOR, 1, 1.0f);
	}

	private void renderMessage(final String message, final float[] colour, int line, float alpha) {
		GlStateManager.enableClientState(GL11.GL_VERTEX_ARRAY);
		ByteBuffer charBuffer = MemoryUtil.memAlloc(message.length() * 270);
		int quads = STBEasyFont.stb_easy_font_print(0, 0, message, null, charBuffer);
		GL14.glVertexPointer(2, GL11.GL_FLOAT, 16, charBuffer);

		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
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

}

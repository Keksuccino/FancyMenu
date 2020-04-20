package de.keksuccino.core.rendering;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.ResourceLocation;

public class RenderUtils {
	
	private static ResourceLocation WHITE = null;
	private static ResourceLocation BLANK = null;

	public static ResourceLocation getWhiteImageResource() {
		if (WHITE != null) {
			return WHITE;
		}
		if (Minecraft.getInstance().getTextureManager() == null) {
			return null;
		}
		NativeImage i = new NativeImage(1, 1, true);
		i.setPixelRGBA(0, 0, Color.WHITE.getRGB());
		ResourceLocation r = Minecraft.getInstance().getTextureManager().getDynamicTextureLocation("whiteback", new DynamicTexture(i));
		WHITE = r;
		return r;
	}
	
	public static ResourceLocation getBlankImageResource() {
		if (BLANK != null) {
			return BLANK;
		}
		if (Minecraft.getInstance().getTextureManager() == null) {
			return null;
		}
		NativeImage i = new NativeImage(1, 1, true);
		i.setPixelRGBA(0, 0, new Color(255, 255, 255, 0).getRGB());
		ResourceLocation r = Minecraft.getInstance().getTextureManager().getDynamicTextureLocation("blankback", new DynamicTexture(i));
		BLANK = r;
		return r;
	}
	
	public static void setScale(float scale) {
    	GL11.glPushMatrix();
        GlStateManager.enableBlend();
        GL11.glPushMatrix();
        GL11.glScaled(scale, scale, scale);
    }
	
    public static void postScale() {
    	GL11.glPopMatrix();
        GlStateManager.disableBlend();
        GL11.glPopMatrix();
    }

}

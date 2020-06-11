package de.keksuccino.core.rendering;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public class RenderUtils {
	
	private static ResourceLocation WHITE = null;
	private static ResourceLocation BLANK = null;

	public static ResourceLocation getWhiteImageResource() {
		if (WHITE != null) {
			return WHITE;
		}
		if (Minecraft.getMinecraft().getTextureManager() == null) {
			return null;
		}
		BufferedImage i = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		i.setRGB(0, 0, Color.WHITE.getRGB());
		ResourceLocation r = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("whiteback", new DynamicTexture(i));
		WHITE = r;
		return r;
	}
	
	public static ResourceLocation getBlankImageResource() {
		if (BLANK != null) {
			return BLANK;
		}
		if (Minecraft.getMinecraft().getTextureManager() == null) {
			return null;
		}
		BufferedImage i = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		i.setRGB(0, 0, new Color(255, 255, 255, 0).getRGB());
		ResourceLocation r = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("blankback", new DynamicTexture(i));
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
    
    public static void doubleBlit(double x, double y, float f1, float f2, int w, int h) {
    	innerDoubleBlit(x, x + (double)w, y, y + (double)h, 0, (f1 + 0.0F) / (float)w, (f1 + (float)w) / (float)w, (f2 + 0.0F) / (float)h, (f2 + (float)h) / (float)h);
    }

    public static void innerDoubleBlit(double x, double xEnd, double y, double yEnd, int z, float f1, float f2, float f3, float f4) {
    	Tessellator tesselator = Tessellator.getInstance();
    	BufferBuilder bufferbuilder = tesselator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, yEnd, (double)z).tex(f1, f4).endVertex();
        bufferbuilder.pos(xEnd, yEnd, (double)z).tex(f2, f4).endVertex();
        bufferbuilder.pos(xEnd, y, (double)z).tex(f2, f3).endVertex();
        bufferbuilder.pos(x, y, (double)z).tex(f1, f3).endVertex();
        tesselator.draw();
    }

}

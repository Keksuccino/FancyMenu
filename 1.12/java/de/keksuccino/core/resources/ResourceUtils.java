package de.keksuccino.core.resources;

import java.awt.image.BufferedImage;
import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;

public class ResourceUtils {
	
	public static String[] splitToNamespaceAndPath(String fullPath, char splitter) {
		String[] s = new String[] { "minecraft", fullPath };
		int i = fullPath.indexOf(splitter);
		if (i >= 0) {
			s[1] = fullPath.substring(i + 1, fullPath.length());
			if (i >= 1) {
				s[0] = fullPath.substring(0, i);
			}
		}
		return s;
	}
	
	public static BufferedImage getImageResourceAsStream(ResourceLocation r) {
		try {
			return TextureUtil.readBufferedImage(Minecraft.getMinecraft().getResourceManager().getResource(r).getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}

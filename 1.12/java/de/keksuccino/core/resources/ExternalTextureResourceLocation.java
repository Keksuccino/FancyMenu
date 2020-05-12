package de.keksuccino.core.resources;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.io.IOUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

public class ExternalTextureResourceLocation {
	
	private String path;
	private ResourceLocation location;
	private boolean loaded = false;
	private int width = 0;
	private int height = 0;
	
	public ExternalTextureResourceLocation(String path) {
		this.path = path;
	}

	/**
	 * Loads the external texture to a {@link ResourceLocation}.<br>
	 * The main instance of minecraft's {@link TextureManager} needs to be loaded before calling this method.<br><br>
	 * 
	 * After loading the texture, {@code ExternalResourceLocation.isReady()} will return true.
	 * 
	 * @throws LowMemoryException 
	 */
	public void loadTexture() {
		if (this.loaded) {
			return;
		}
		
		try {
			if (Minecraft.getMinecraft().getTextureManager() == null) {
				System.out.println("################################ WARNING ################################");
				System.out.println("Can't load texture '" + this.path + "'! Minecraft TextureManager instance not ready yet!");
				return;
			}

			File f = new File(this.path);
			ImageInputStream s = ImageIO.createImageInputStream(f);
			BufferedImage i = ImageIO.read(s);
			this.width = i.getWidth();
			this.height = i.getHeight();
			location = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(f.getName(), new SelfcleaningDynamicTexture(i));
			loaded = true;
			
			i.getGraphics().dispose();
			IOUtils.closeQuietly(s);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public ResourceLocation getResourceLocation() {
		return this.location;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public boolean isReady() {
		return this.loaded;
	}
	
	public int getHeight() {
		return this.height;
	}
	
	public int getWidth() {
		return this.width;
	}

}

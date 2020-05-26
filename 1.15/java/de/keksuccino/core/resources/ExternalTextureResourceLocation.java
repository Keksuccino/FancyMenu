package de.keksuccino.core.resources;

import java.io.File;
import java.io.FileInputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

public class ExternalTextureResourceLocation implements ITextureResourceLocation {
	
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
	 */
	public void loadTexture() {
		if (this.loaded) {
			return;
		}

		try {
			if (Minecraft.getInstance().getTextureManager() == null) {
				System.out.println("################################ WARNING ################################");
				System.out.println("Can't load texture '" + this.path + "'! Minecraft TextureManager instance not ready yet!");
				return;
			}
			File f = new File(path);
			FileInputStream s = new FileInputStream(f);
			NativeImage i = NativeImage.read(s);
			this.width = i.getWidth();
			this.height = i.getHeight();
			location = Minecraft.getInstance().getTextureManager().getDynamicTextureLocation(f.getName(), new SelfcleaningDynamicTexture(i));
			s.close();
			loaded = true;
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

package de.keksuccino.core.resources;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import de.keksuccino.core.input.CharacterFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

public class WebTextureResourceLocation implements ITextureResourceLocation {

	private String url;
	private ResourceLocation location;
	private boolean loaded = false;
	private int width = 0;
	private int height = 0;
	
	public WebTextureResourceLocation(String url) {
		this.url = url;
	}
	
	/**
	 * Loads the web texture to a {@link ResourceLocation}.<br>
	 * The main instance of minecraft's {@link TextureManager} needs to be loaded before calling this method.<br><br>
	 * 
	 * After loading the texture, {@code WebTextureResourceLocation.isReady()} will return true.
	 */
	public void loadTexture() {
		if (this.loaded) {
			return;
		}
		
		if (!this.isValidUrl()) {
			System.out.println("################################ WARNING ################################");
			System.out.println("Can't load texture '" + this.url + "'! Invalid URL!");
			return;
		}

		try {
			if (Minecraft.getInstance().getTextureManager() == null) {
				System.out.println("################################ WARNING ################################");
				System.out.println("Can't load texture '" + this.url + "'! Minecraft TextureManager instance not ready yet!");
				return;
			}
			
			URL u = new URL(this.url);
			InputStream s = u.openStream();
			if (s == null) {
				return;
			}
			NativeImage i = NativeImage.read(s);
			this.width = i.getWidth();
			this.height = i.getHeight();
			location = Minecraft.getInstance().getTextureManager().getDynamicTextureLocation(this.filterUrl(url), new SelfcleaningDynamicTexture(i));
			s.close();
			loaded = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public ResourceLocation getResourceLocation() {
		return this.location;
	}
	
	public String getURL() {
		return this.url;
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
	
	public boolean isValidUrl() {
		if ((this.url == null) || (!this.url.startsWith("http://") && !this.url.startsWith("https://"))) {
			return false;
		}
		
		try {
			URL url = new URL(this.url);
			HttpURLConnection c = (HttpURLConnection) url.openConnection();
			
			if (!c.getContentType().equalsIgnoreCase("image/jpeg") && !c.getContentType().equalsIgnoreCase("image/jpg") && !c.getContentType().equalsIgnoreCase("image/png")) {
				return false;
			}

			c.setRequestMethod("HEAD");
			int r = c.getResponseCode();
			
			if (r == 200) {
				return true;
			}
		} catch (Exception e) {
			System.out.println("Trying alternative method to check for existing url..");
			try {
				URL url = new URL(this.url);
				HttpURLConnection c = (HttpURLConnection) url.openConnection();
				
				if (!c.getContentType().equalsIgnoreCase("image/jpeg") && !c.getContentType().equalsIgnoreCase("image/jpg") && !c.getContentType().equalsIgnoreCase("image/png")) {
					return false;
				}
				
				int r = c.getResponseCode();
				
				if (r == 200) {
					return true;
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return false;
	}
	
	private String filterUrl(String url) {
		CharacterFilter c = new CharacterFilter();
		c.addAllowedCharacters("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", 
				"o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".");
		return c.filterForAllowedChars(url.toLowerCase());
	}

}

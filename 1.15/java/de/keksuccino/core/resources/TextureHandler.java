package de.keksuccino.core.resources;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TextureHandler {

	private static Map<String, ITextureResourceLocation> textures = new HashMap<String, ITextureResourceLocation>();
	
	public static ExternalTextureResourceLocation getResource(String path) {
		File f = new File(path);
		if (!textures.containsKey(f.getAbsolutePath())) {
			if (f.exists() && f.isFile()) {
				ExternalTextureResourceLocation r = new ExternalTextureResourceLocation(f.getAbsolutePath());
				r.loadTexture();
				textures.put(f.getAbsolutePath(), r);
				return r;
			}
			return null;
		}
		return (ExternalTextureResourceLocation) textures.get(f.getAbsolutePath());
	}
	
	public static WebTextureResourceLocation getWebResource(String url) {
		if (!textures.containsKey(url)) {
			try {
				WebTextureResourceLocation r = new WebTextureResourceLocation(url);
				r.loadTexture();
				if (!r.isReady()) {
					return null;
				}
				textures.put(url, r);
				return r;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		return (WebTextureResourceLocation) textures.get(url);
	}
	
	public static void removeResource(String path) {
		File f = new File(path);
		if (textures.containsKey(f.getAbsolutePath())) {
			textures.remove(f.getAbsolutePath());
		}
	}

}

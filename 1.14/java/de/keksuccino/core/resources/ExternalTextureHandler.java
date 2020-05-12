package de.keksuccino.core.resources;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ExternalTextureHandler {
	
	private static Map<String, ExternalTextureResourceLocation> textures = new HashMap<String, ExternalTextureResourceLocation>();
	
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
		return textures.get(f.getAbsolutePath());
	}
	
	public static void removeResource(String path) {
		File f = new File(path);
		if (textures.containsKey(f.getAbsolutePath())) {
			textures.remove(f.getAbsolutePath());
		}
	}

}

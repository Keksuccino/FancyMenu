package de.keksuccino.fancymenu.menu.fancy.item.playerentity;

import java.io.*;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

public class PlayerEntityCache {
	
	private static volatile Map<String, ResourceLocation> skins = new HashMap<String, ResourceLocation>();
	private static volatile Map<String, ResourceLocation> capes = new HashMap<String, ResourceLocation>();
	private static volatile Map<String, Boolean> slimskin = new HashMap<String, Boolean>();
	
	public static boolean isSkinCached(String playerName) {
		return skins.containsKey(playerName);
	}

	public static void cacheSkin(String playerName, ResourceLocation skin) {
		skins.put(playerName, skin);
	}
	
	public static ResourceLocation getSkin(String playerName) {
		return skins.get(playerName);
	}
	
	public static boolean isCapeCached(String playerName) {
		return capes.containsKey(playerName);
	}
	
	public static void cacheCape(String playerName, ResourceLocation cape) {
		capes.put(playerName, cape);
	}
	
	public static ResourceLocation getCape(String playerName) {
		return capes.get(playerName);
	}
	
	public static boolean isSlimSkinInfoCached(String playerName) {
		return slimskin.containsKey(playerName);
	}
	
	public static void cacheIsSlimSkin(String playerName, boolean isSlimSkin) {
		slimskin.put(playerName, isSlimSkin);
	}
	
	public static boolean getIsSlimSkin(String playerName) {
		if (slimskin.containsKey(playerName)) {
			return slimskin.get(playerName);
		}
		return false;
	}

	/**
	 * Returns the calculated SHA-1 or <b>null</b> if calculating failed.
	 */
	public static String calculateSHA1(File file) {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			InputStream input = new FileInputStream(file);
			byte[] buffer = new byte[8192];
			int len = input.read(buffer);

			while (len != -1) {
				sha1.update(buffer, 0, len);
				len = input.read(buffer);
			}

			return new HexBinaryAdapter().marshal(sha1.digest());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}

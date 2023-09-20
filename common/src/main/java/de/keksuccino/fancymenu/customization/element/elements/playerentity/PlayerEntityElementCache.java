package de.keksuccino.fancymenu.customization.element.elements.playerentity;

import de.keksuccino.konkrete.web.WebUtils;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class PlayerEntityElementCache {

	private static final Logger LOGGER = LogManager.getLogger();
	
	private static final Map<String, ResourceLocation> SKIN_CACHE = new HashMap<>();
	private static final Map<String, ResourceLocation> CAPE_CACHE = new HashMap<>();
	private static final Map<String, Boolean> IS_SLIM_CACHE = new HashMap<>();
	
	public static boolean isSkinCached(String playerName) {
		return SKIN_CACHE.containsKey(playerName);
	}

	public static void cacheSkin(String playerName, ResourceLocation skin) {
		SKIN_CACHE.put(playerName, skin);
	}
	
	public static ResourceLocation getSkin(String playerName) {
		return SKIN_CACHE.get(playerName);
	}
	
	public static boolean isCapeCached(String playerName) {
		return CAPE_CACHE.containsKey(playerName);
	}
	
	public static void cacheCape(String playerName, ResourceLocation cape) {
		CAPE_CACHE.put(playerName, cape);
	}
	
	public static ResourceLocation getCape(String playerName) {
		return CAPE_CACHE.get(playerName);
	}
	
	public static boolean isSlimSkinInfoCached(String playerName) {
		return IS_SLIM_CACHE.containsKey(playerName);
	}
	
	public static void cacheIsSlimSkin(String playerName, boolean isSlimSkin) {
		IS_SLIM_CACHE.put(playerName, isSlimSkin);
	}
	
	public static boolean getIsSlimSkin(String playerName) {
		if (IS_SLIM_CACHE.containsKey(playerName)) {
			return IS_SLIM_CACHE.get(playerName);
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
			IOUtils.closeQuietly(input);
			return bytesToHexString(sha1.digest());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns the calculated SHA-1 or <b>null</b> if calculating failed.
	 */
	public static String calculateWebSourceSHA1(String url) {
		InputStream input = null;
		try {
			if (WebUtils.isValidUrl(url)) {
				MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
				URL u = new URL(url);
				HttpURLConnection httpcon = (HttpURLConnection)u.openConnection();
				httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");
				input = httpcon.getInputStream();
				if (input != null) {
					byte[] buffer = new byte[8192];
					int len = input.read(buffer);
					while (len != -1) {
						sha1.update(buffer, 0, len);
						len = input.read(buffer);
					}
					IOUtils.closeQuietly(input);
					return bytesToHexString(sha1.digest());
				}
			}
		} catch (Exception e) {
			if (input != null) {
				IOUtils.closeQuietly(input);
			}
			e.printStackTrace();
		}
		return null;
	}

	protected static String bytesToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			int value = b & 0xFF;
			if (value < 16) {
				sb.append("0");
			}
			sb.append(Integer.toHexString(value).toUpperCase());
		}
		return sb.toString();
	}

}

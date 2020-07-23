package de.keksuccino.fancymenu.localization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.common.io.Files;

import de.keksuccino.core.file.FileUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class Locals {
	
	private static Map<String, LocalizationPackage> locals = new HashMap<String, LocalizationPackage>();
	
	public static void init() {
		locals.clear();
		
		File f = new File("config/fancymenu/locals");
		if (!f.exists()) {
			f.mkdirs();
		}
		
		prepareLocalsFiles();
		
		for (File f2 : f.listFiles()) {
			if (f2.isFile() && f2.getName().endsWith(".local")) {
				String language = Files.getNameWithoutExtension(f2.getPath());
				LocalizationPackage p = new LocalizationPackage(language);
				
				for (String s : FileUtils.getFileLines(f2)) {
					if (s.contains("=")) {
						String key = s.split("[=]", 2)[0].replace(" ", "");
						String value = s.split("[=]", 2)[1];
						if (value.startsWith(" ")) {
							value = value.substring(1);
						}
						p.addLocalizedString(key, value);
					}
				}
				
				if (!p.isEmpty()) {
					locals.put(language, p);
				}
			}
		}
	}
	
	private static void prepareLocalsFiles() {
		copyLocalsFile("en_us");
		copyLocalsFile("de_de");
		copyLocalsFile("pl_pl");
	}
	
	private static void copyLocalsFile(String language) {
		File lang = new File("config/fancymenu/locals/" + language + ".local");
		if (lang.exists()) {
			lang.delete();
		}

		BufferedReader br = null;
		BufferedWriter bw = null;
		try {
			try {
				br = new BufferedReader(new InputStreamReader(Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation("keksuccino", "fmlocals/" + language + ".local")).getInputStream(), StandardCharsets.UTF_8));
				bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(lang, false), StandardCharsets.UTF_8));
				
				String full = "";
				
				String line = br.readLine();
				while (line != null) {
					full += line + "\n";
					line = br.readLine();
				}
				
				bw.write(full);
				bw.flush();
			} finally {
				bw.close();
				br.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String localizeTo(String key, String language, String... dynamicValues) {
		LocalizationPackage p = getPackage(language);
		if (p == null) {
			return key;
		}
		String rawLocal = p.getLocalizedString(key);
		if (rawLocal == null) {
			if (!language.equals("en_us")) {
				return localizeTo(key, "en_us", dynamicValues);
			} else {
				return key;
			}
		}
		
		String local = rawLocal;
		for (String s : dynamicValues) {
			if (local.contains("{}")) {
				local = local.replaceFirst("[{][}]", s);
			} else {
				break;
			}
		}
		
		return local;
	}
	
	public static String localize(String key, String... dynamicValues) {
		String playerLang = Minecraft.getInstance().gameSettings.language;
		if (locals.containsKey(playerLang)) {
			return localizeTo(key, playerLang, dynamicValues);
		}
		return localizeTo(key, "en_us", dynamicValues);
	}
	
	public static LocalizationPackage getPackage(String language) {
		if (locals.containsKey(language)) {
			return locals.get(language);
		}
		return null;
	}
	
}

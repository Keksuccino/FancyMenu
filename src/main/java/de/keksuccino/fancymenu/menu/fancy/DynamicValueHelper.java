package de.keksuccino.fancymenu.menu.fancy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.keksuccino.konkrete.input.StringUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;

public class DynamicValueHelper {
	
	public static String convertFromRaw(String in) {
		int width = 0;
		int height = 0;
		String playername = MinecraftClient.getInstance().getSession().getUsername();
		String playeruuid = MinecraftClient.getInstance().getSession().getUuid();
		String mcversion = SharedConstants.getGameVersion().getReleaseTarget();
		if (MinecraftClient.getInstance().currentScreen != null) {
			width = MinecraftClient.getInstance().currentScreen.width;
			height = MinecraftClient.getInstance().currentScreen.height;
		}
		
		//Convert &-formatcodes to real ones
		in = StringUtils.convertFormatCodes(in, "&", "§");
		
		//Replace height and width placeholders
		in = in.replace("%guiwidth%", "" + width);
		in = in.replace("%guiheight%", "" + height);
		
		//Replace player name and uuid placeholders
		in = in.replace("%playername%", playername);
		in = in.replace("%playeruuid%", playeruuid);
		
		//Replace mc version placeholder
		in = in.replace("%mcversion%", mcversion);

		//Replace mod version placeholder
		in = replaceModVersionPlaceolder(in);

		//Replace loaded mods placeholder
		int loaded = getLoadedMods();
		in = in.replace("%loadedmods%", "" + loaded);

		//Replace total mods placeholder
		int total = getTotalMods();
		if (total < loaded) {
			total = loaded;
		}
		in = in.replace("%totalmods%", "" + total);
		
		return in;
	}
	
	public static boolean containsDynamicValues(String in) {
		String s = convertFromRaw(in);
		return !s.equals(in);
	}
	
	private static String replaceModVersionPlaceolder(String in) {
		try {
			if (in.contains("%version:")) {
				List<String> l = new ArrayList<String>();
				int index = -1;
				int i = 0;
				while (i < in.length()) {
					String s = "" + in.charAt(i);
					if (s.equals("%")) {
						if (index == -1) {
							index = i;
						} else {
							String sub = in.substring(index, i+1);
							if (sub.startsWith("%version:") && sub.endsWith("%")) {
								l.add(sub);
							}
							index = -1;
						}
					}
					i++;
				}
				for (String s : l) {
					if (s.contains(":")) {
						String blank = s.substring(1, s.length()-1);
						String mod = blank.split(":", 2)[1];
						if (FabricLoader.getInstance().isModLoaded(mod)) {
							Optional<ModContainer> o = FabricLoader.getInstance().getModContainer(mod);
							if (o.isPresent()) {
								ModContainer c = o.get();
								String version = c.getMetadata().getVersion().getFriendlyString();
								in = in.replace(s, version);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	//Just for forge-fabric compatibility (basically useless in fabric, since fabric doesn't support disabling mods and both values will always be the same)
	private static int getTotalMods() {
		return getLoadedMods();
	}

	private static int getLoadedMods() {
		try {
			return FabricLoader.getInstance().getAllMods().size();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

}

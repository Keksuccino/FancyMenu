package de.keksuccino.fancymenu.menu.fancy;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import de.keksuccino.fancymenu.api.placeholder.PlaceholderTextContainer;
import de.keksuccino.fancymenu.api.placeholder.PlaceholderTextRegistry;
import de.keksuccino.fancymenu.menu.servers.ServerCache;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.versions.mcp.MCPVersion;

public class DynamicValueHelper {

	private static final File MOD_DIRECTORY = new File("mods");
	
	public static String convertFromRaw(String in) {
		int width = 0;
		int height = 0;
		String playername = Minecraft.getInstance().getUser().getName();
		String playeruuid = Minecraft.getInstance().getUser().getUuid();
		String mcversion = MCPVersion.getMCVersion();
		if (Minecraft.getInstance().screen != null) {
			width = Minecraft.getInstance().screen.width;
			height = Minecraft.getInstance().screen.height;
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

		in = replaceLocalsPlaceolder(in);

		in = replaceServerMOTD(in);

		in = replaceServerPing(in);

		in = replaceServerVersion(in);

		in = replaceServerPlayerCount(in);

		in = replaceServerStatus(in);

		if (in.contains("ram%")) {
			long i = Runtime.getRuntime().maxMemory();
			long j = Runtime.getRuntime().totalMemory();
			long k = Runtime.getRuntime().freeMemory();
			long l = j - k;

			in = in.replace("%percentram%", (l * 100L / i) + "%");

			in = in.replace("%usedram%", "" + bytesToMb(l));

			in = in.replace("%maxram%", "" + bytesToMb(i));
		}

		if (in.contains("%realtime")) {

			Calendar c = Calendar.getInstance();

			in = in.replace("%realtimeyear%", "" + c.get(Calendar.YEAR));

			in = in.replace("%realtimemonth%", formatToFancyDateTime(c.get(Calendar.MONTH) + 1));

			in = in.replace("%realtimeday%", formatToFancyDateTime(c.get(Calendar.DAY_OF_MONTH)));

			in = in.replace("%realtimehour%", formatToFancyDateTime(c.get(Calendar.HOUR_OF_DAY)));

			in = in.replace("%realtimeminute%", formatToFancyDateTime(c.get(Calendar.MINUTE)));

			in = in.replace("%realtimesecond%", formatToFancyDateTime(c.get(Calendar.SECOND)));

		}

		//Handle all custom placeholders added via the API
		for (PlaceholderTextContainer p : PlaceholderTextRegistry.getPlaceholders()) {
			in = p.replacePlaceholders(in);
		}
		
		return in;
	}
	
	public static boolean containsDynamicValues(String in) {
		String s = convertFromRaw(in);
		return !s.equals(in);
	}

	private static String replaceLocalsPlaceolder(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%local:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String localizationKey = blank.split(":", 2)[1];
					in = in.replace(s, Locals.localize(localizationKey));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceServerVersion(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%serverversion:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String ip = blank.split(":", 2)[1];
					ServerData sd = ServerCache.getServer(ip);
					if (sd != null) {
						if (sd.version != null) {
							in = in.replace(s, sd.version.getString());
						} else {
							in = in.replace(s, "---");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceServerStatus(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%serverstatus:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String ip = blank.split(":", 2)[1];
					ServerData sd = ServerCache.getServer(ip);
					if (sd != null) {
						if (sd.ping != -1L) {
							in = in.replace(s, "§aOnline");
						} else {
							in = in.replace(s, "§cOffline");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceServerPlayerCount(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%serverplayercount:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String ip = blank.split(":", 2)[1];
					ServerData sd = ServerCache.getServer(ip);
					if (sd != null) {
						if (sd.status != null) {
							in = in.replace(s, "" + sd.status.getString());
						} else {
							in = in.replace(s, "0/0");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceServerPing(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%serverping:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String ip = blank.split(":", 2)[1];
					ServerData sd = ServerCache.getServer(ip);
					if (sd != null) {
						in = in.replace(s, "" + sd.ping);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceServerMOTD(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%servermotd:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String ip = blank.split(":", 2)[1];
					ServerData sd = ServerCache.getServer(ip);
					if (sd != null) {
						if (sd.motd != null) {
							in = in.replace(s, sd.motd.getString());
						} else {
							in = in.replace(s, "---");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceModVersionPlaceolder(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%version:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String mod = blank.split(":", 2)[1];
					if (ModList.get().isLoaded(mod)) {
						Optional<? extends ModContainer> o = ModList.get().getModContainerById(mod);
						if (o.isPresent()) {
							ModContainer c = o.get();
							String version = c.getModInfo().getVersion().toString();
							in = in.replace(s, version);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	protected static List<String> getReplaceablesWithValue(String in, String placeholderBase) {
		List<String> l = new ArrayList<String>();
		try {
			if (in.contains(placeholderBase)) {
				int index = -1;
				int i = 0;
				while (i < in.length()) {
					String s = "" + in.charAt(i);
					if (s.equals("%")) {
						if (index == -1) {
							index = i;
						} else {
							String sub = in.substring(index, i+1);
							if (sub.startsWith(placeholderBase) && sub.endsWith("%")) {
								l.add(sub);
							}
							index = -1;
						}
					}
					i++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return l;
	}

	private static int getTotalMods() {
		if (MOD_DIRECTORY.exists()) {
			int i = 0;
			for (File f : MOD_DIRECTORY.listFiles()) {
				if (f.isFile() && f.getName().toLowerCase().endsWith(".jar")) {
					i++;
				}
			}
			return i+2;
		}
		return -1;
	}

	private static int getLoadedMods() {
		try {
			return ModList.get().getMods().size();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	private static String formatToFancyDateTime(int in) {
		String s = "" + in;
		if (s.length() < 2) {
			s = "0" + s;
		}
		return s;
	}

	private static long bytesToMb(long bytes) {
		return bytes / 1024L / 1024L;
	}

}

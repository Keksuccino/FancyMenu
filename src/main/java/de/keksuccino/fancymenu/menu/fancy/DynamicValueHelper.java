package de.keksuccino.fancymenu.menu.fancy;

import java.io.File;
import java.util.*;

import de.keksuccino.fancymenu.api.placeholder.PlaceholderTextContainer;
import de.keksuccino.fancymenu.api.placeholder.PlaceholderTextRegistry;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.button.ButtonMimeHandler;
import de.keksuccino.fancymenu.menu.servers.ServerCache;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.versions.mcp.MCPVersion;

public class DynamicValueHelper {

	private static final File MOD_DIRECTORY = new File("mods");

	private static int cachedTotalMods = -10;
	public static Map<String, RandomTextPackage> randomTextIntervals = new HashMap<>();

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
		in = replaceServerMotdFirstLine(in);
		in = replaceServerMotdSecondLine(in);

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

		in = replaceVanillaButtonLabelPlaceolder(in);

		in = replaceRandomTextValue(in);

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

	private static String replaceRandomTextValue(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%randomtext:")) { // %randomtext:<filepath>:<change_interval_sec>%
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String value = blank.split(":", 2)[1];
					if (value.contains(":")) {
						String pathString = value.split(":", 2)[0];
						File path = new File(pathString);
						String intervalString = value.split(":", 2)[1];
						if (MathUtils.isLong(intervalString) && path.isFile() && path.getPath().toLowerCase().endsWith(".txt")) {
							long interval = Long.parseLong(intervalString) * 1000;
							if (interval < 0L) {
								interval = 0L;
							}
							long currentTime = System.currentTimeMillis();
							RandomTextPackage p;
							if (randomTextIntervals.containsKey(path.getPath())) {
								p = randomTextIntervals.get(path.getPath());
							} else {
								p = new RandomTextPackage();
								randomTextIntervals.put(path.getPath(), p);
							}
							if ((interval > 0) || (p.currentText == null)) {
								if ((p.lastChange + interval) <= currentTime) {
									p.lastChange = currentTime;
									List<String> txtLines = FileUtils.getFileLines(path);
									if (!txtLines.isEmpty()) {
										p.currentText = txtLines.get(MathUtils.getRandomNumberInRange(0, txtLines.size()-1));
									} else {
										p.currentText = null;
									}
								}
							}
							if (p.currentText != null) {
								in = in.replace(s, p.currentText);
							} else {
								in = in.replace(s, "");
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

	private static String replaceVanillaButtonLabelPlaceolder(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%vanillabuttonlabel:")) {
				String blank = s.substring(1, s.length()-1);
				String buttonLocator = blank.split(":", 2)[1];
				ButtonData d = ButtonMimeHandler.getButton(buttonLocator);
				if (d != null) {
					in = in.replace(s, d.getButton().getMessage().getString());
				} else {
					in = in.replace(s, "§c[unable to get button label]");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceLocalsPlaceolder(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%local:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String localizationKey = blank.split(":", 2)[1];
					String localized = Locals.localize(localizationKey);
					if (localized.equals(localizationKey)) {
						localized = I18n.get(localizationKey);
						if (localized == null) {
							localized = localizationKey;
						}
					}
					in = in.replace(s, localized);
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
							in = in.replace(s, "");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceServerMotdFirstLine(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%servermotd_line1:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String ip = blank.split(":", 2)[1];
					ServerData sd = ServerCache.getServer(ip);
					if (sd != null) {
						if (sd.motd != null) {
							List<String> lines = splitMotdLines(sd.motd.getString());
							if (!lines.isEmpty()) {
								in = in.replace(s, lines.get(0));
							} else {
								in = in.replace(s, "");
							}
						} else {
							in = in.replace(s, "");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	private static String replaceServerMotdSecondLine(String in) {
		try {
			for (String s : getReplaceablesWithValue(in, "%servermotd_line2:")) {
				if (s.contains(":")) {
					String blank = s.substring(1, s.length()-1);
					String ip = blank.split(":", 2)[1];
					ServerData sd = ServerCache.getServer(ip);
					if (sd != null) {
						if (sd.motd != null) {
							List<String> lines = splitMotdLines(sd.motd.getString());
							if (lines.size() >= 2) {
								in = in.replace(s, lines.get(1));
							} else {
								in = in.replace(s, "");
							}
						} else {
							in = in.replace(s, "");
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return in;
	}

	protected static List<String> splitMotdLines(String motd) {
		List<String> l = new ArrayList<>();
		try {
			if (motd.contains("\n")) {
				l.addAll(Arrays.asList(motd.split("\n")));
			} else {
				l.add(motd);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return l;
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
		if (cachedTotalMods == -10) {
			if (MOD_DIRECTORY.exists()) {
				int i = 0;
				for (File f : MOD_DIRECTORY.listFiles()) {
					if (f.isFile() && f.getName().toLowerCase().endsWith(".jar")) {
						i++;
					}
				}
				cachedTotalMods = i+2;
			} else {
				cachedTotalMods = -1;
			}
		}
		return cachedTotalMods;
	}

	private static int getLoadedMods() {
		try {
			int i = 0;
			if (Konkrete.isOptifineLoaded) {
				i++;
			}
			return ModList.get().getMods().size() + i;
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

	public static class RandomTextPackage {
		public String currentText = null;
		public long lastChange = 0L;
	}

}

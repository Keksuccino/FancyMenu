package de.keksuccino.fancymenu.menu.button;

import java.awt.Color;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.keksuccino.fancymenu.menu.world.LastWorldHandler;
import de.keksuccino.fancymenu.mixin.client.IMixinServerList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import com.google.common.io.Files;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.api.buttonaction.ButtonActionContainer;
import de.keksuccino.fancymenu.api.buttonaction.ButtonActionRegistry;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationHelper;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry;
import de.keksuccino.fancymenu.menu.guiconstruction.GuiConstructor;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;

public class ButtonScriptEngine {

	private static Map<String, ButtonScript> scripts = new HashMap<String, ButtonScriptEngine.ButtonScript>();
	private static boolean init = false;
	
	public static void init() {
		if (!init) {
			init = true;
			Konkrete.getEventHandler().registerEventsFrom(new ButtonScriptEngine());
			updateButtonScripts();
		}
	}
	
	public static void updateButtonScripts() {
		scripts.clear();

		if (!FancyMenu.getButtonScriptPath().exists()) {
			FancyMenu.getButtonScriptPath().mkdirs();
		}

		for (File f : FancyMenu.getButtonScriptPath().listFiles()) {
			if (f.isFile() && f.getPath().toLowerCase().endsWith(".txt")) {
				scripts.put(Files.getNameWithoutExtension(f.getPath()), new ButtonScript(f));
			}
		}
	}
	
	public static void runButtonScript(String name) {
		if (scripts.containsKey(name)) {
			scripts.get(name).runScript();
		}
	}
	
	public static Map<String, ButtonScript> getButtonScripts() {
		return scripts;
	}
	
	@SuppressWarnings("resource")
	public static void runButtonAction(String action, String value) {
		try {
			if (action.equalsIgnoreCase("openlink")) {
				openWebLink(value);
			}
			if (action.equalsIgnoreCase("sendmessage")) {
				if (Minecraft.getInstance().level != null) {
					Minecraft.getInstance().player.chat(value);
				}
			}
			if (action.equalsIgnoreCase("quitgame")) {
				Minecraft.getInstance().stop();
			}
			if (action.equalsIgnoreCase("joinserver")) {
				if (value != null) {
					String ip = value.replace(" ", "");
					int port = 25565;
					if (ip.contains(":")) {
						String portString = ip.split("[:]", 2)[1];
						ip = ip.split("[:]", 2)[0];
						if (MathUtils.isInteger(portString)) {
							port = Integer.parseInt(portString);
						}
					}
					Screen s = Minecraft.getInstance().screen;
					if (s == null) {
						s = new TitleScreen();
					}
					ServerData d = null;
					ServerList l = new ServerList(Minecraft.getInstance());
					l.load();
					for (ServerData data : ((IMixinServerList)l).getServerListFancyMenu()) {
						if (data.ip.equals(value.replace(" ", ""))) {
							d = data;
							break;
						}
					}
					if (d == null) {
						d = new ServerData(value.replace(" ", ""), value.replace(" ", ""), false);
						l.add(d);
						l.save();
					}
					ConnectScreen.startConnecting(s, Minecraft.getInstance(), new ServerAddress(ip, port), d);
				}
			}
			if (action.equalsIgnoreCase("loadworld")) {
				if (Minecraft.getInstance().getLevelSource().levelExists(value)) {
					//launchIntegratedServer
					Minecraft.getInstance().loadLevel(value);
				}
			}
			if (action.equalsIgnoreCase("openfile")) { //for files and folders
				File f = new File(value.replace("\\", "/"));
				if (f.exists()) {
					openFile(f);
				}
			}
			if (action.equalsIgnoreCase("prevbackground")) {
				MenuHandlerBase handler = MenuHandlerRegistry.getLastActiveHandler();
				if (handler != null) {
					int cur = handler.getCurrentBackgroundAnimationId();
					if (cur > 0) {
						for (IAnimationRenderer an : handler.backgroundAnimations()) {
							if (an instanceof AdvancedAnimation) {
								((AdvancedAnimation)an).stopAudio();
							}
						}
						handler.setBackgroundAnimation(cur-1);
					}
				}
			}
			if (action.equalsIgnoreCase("nextbackground")) {
				MenuHandlerBase handler = MenuHandlerRegistry.getLastActiveHandler();
				if (handler != null) {
					int cur = handler.getCurrentBackgroundAnimationId();
					if (cur < handler.backgroundAnimations().size()-1) {
						for (IAnimationRenderer an : handler.backgroundAnimations()) {
							if (an instanceof AdvancedAnimation) {
								((AdvancedAnimation)an).stopAudio();
							}
						}
						handler.setBackgroundAnimation(cur+1);
					}
				}
			}
			if (action.equalsIgnoreCase("opencustomgui")) {
				if (CustomGuiLoader.guiExists(value)) {
					Minecraft.getInstance().setScreen(CustomGuiLoader.getGui(value, Minecraft.getInstance().screen, null));
				}
			}
			if (action.equalsIgnoreCase("opengui")) {
				if (value.equals(CreateWorldScreen.class.getName())) {
					Minecraft.getInstance().setScreen(CreateWorldScreen.createFresh(Minecraft.getInstance().screen));
				} else {
					try {
						if (CustomGuiLoader.guiExists(value)) {
							Minecraft.getInstance().setScreen(CustomGuiLoader.getGui(value, Minecraft.getInstance().screen, null));
						} else {
							Screen s = GuiConstructor.tryToConstruct(MenuCustomization.getValidMenuIdentifierFor(value));
							if (s != null) {
								Minecraft.getInstance().setScreen(s);
							} else {
								PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("custombuttons.action.opengui.cannotopengui")));
							}
						}
					} catch (Exception e) {
						PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("custombuttons.action.opengui.cannotopengui")));
						e.printStackTrace();
					}
				}
			}
			if (action.equalsIgnoreCase("movefile")) {
				if (value.contains(";")) {
					String from = cleanPath(value.split("[;]", 2)[0]);
					String to = cleanPath(value.split("[;]", 2)[1]);
					File toFile = new File(to);
					File fromFile = new File(from);
					
					FileUtils.moveFile(fromFile, toFile);
				}
			}
			if (action.equalsIgnoreCase("copyfile")) {
				if (value.contains(";")) {
					String from = cleanPath(value.split("[;]", 2)[0]);
					String to = cleanPath(value.split("[;]", 2)[1]);
					File toFile = new File(to);
					File fromFile = new File(from);
					
					FileUtils.copyFile(fromFile, toFile);
				}
			}
			if (action.equalsIgnoreCase("deletefile")) {
				File f = new File(cleanPath(value));
				if (f.exists()) {
					if (f.delete()) {
						int i = 0;
						while (f.exists() && (i < 10*10)) {
							Thread.sleep(100);
							i++;
						}
					}
				}
			}
			if (action.equalsIgnoreCase("renamefile")) {
				if (value.contains(";")) {
					String path = cleanPath(value.split("[;]", 2)[0]);
					String name = value.split("[;]", 2)[1];
					File f = new File(path);
					if (f.exists()) {
						String parent = f.getParent();
						if (parent != null) {
							f.renameTo(new File(parent + "/" + name));
						} else {
							f.renameTo(new File(name));
						}
					}
				}
			}
			if (action.equalsIgnoreCase("downloadfile")) {
				if (value.contains(";")) {
					String url = cleanPath(value.split("[;]", 2)[0]);
					String path = cleanPath(value.split("[;]", 2)[1]);
					File f = new File(path);
					if (!f.exists()) {
						f.mkdirs();
					}
					InputStream in = new URL(url).openStream();
					java.nio.file.Files.copy(in, Paths.get(new File(path).toURI()), StandardCopyOption.REPLACE_EXISTING);
				}
			}
			if (action.equalsIgnoreCase("unpackzip")) {
				if (value.contains(";")) {
					String zipPath = cleanPath(value.split("[;]", 2)[0]);
					String outputDir = cleanPath(value.split("[;]", 2)[1]);
					FileUtils.unpackZip(zipPath, outputDir);
				}
			}
			if (action.equalsIgnoreCase("reloadmenu")) {
				CustomizationHelper.reloadSystemAndMenu();
			}
			if (action.equalsIgnoreCase("runscript")) {
				if(scripts.containsKey(value)) {
					runButtonScript(value);
				}
			}
			if (action.equalsIgnoreCase("mutebackgroundsounds")) {
				if (value != null) {
					if (value.equalsIgnoreCase("true")) {
						FancyMenu.config.setValue("playbackgroundsounds", false);
						MenuCustomization.stopSounds();
					}
					if (value.equalsIgnoreCase("false")) {
						FancyMenu.config.setValue("playbackgroundsounds", true);
						CustomizationHelper.reloadSystemAndMenu();
					}
				}
			}
			if (action.equalsIgnoreCase("runcmd")) {
				runCMD(value);
			}
			if (action.equalsIgnoreCase("closegui")) {
				Minecraft.getInstance().setScreen(null);
			}
			if (action.equalsIgnoreCase("copytoclipboard")) {
				Minecraft.getInstance().keyboardHandler.setClipboard(value);
			}
			if (action.equalsIgnoreCase("mimicbutton")) {
				if ((value != null) && value.contains(":")) {
					if (!ButtonMimeHandler.executeButtonAction(value)) {
						PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("fancymenu.custombutton.action.mimicbutton.unabletoexecute")));
					}
				} else {
					PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("fancymenu.custombutton.action.mimicbutton.unabletoexecute")));
				}
			}
			if (action.equalsIgnoreCase("join_last_world")) {
				if (!LastWorldHandler.getLastWorld().equals("")) {
					if (!LastWorldHandler.isLastWorldServer()) {
						File f = new File(LastWorldHandler.getLastWorld());
						if (Minecraft.getInstance().getLevelSource().levelExists(f.getName())) {
							Minecraft.getInstance().loadLevel(f.getName());
						}
					} else {
						String ipRaw = LastWorldHandler.getLastWorld().replace(" ", "");
						String ip = ipRaw;
						int port = 25565;
						if (ip.contains(":")) {
							String portString = ip.split("[:]", 2)[1];
							ip = ip.split("[:]", 2)[0];
							if (MathUtils.isInteger(portString)) {
								port = Integer.parseInt(portString);
							}
						}
						Screen s = Minecraft.getInstance().screen;
						if (s == null) {
							s = new TitleScreen();
						}
						ServerData d = null;
						ServerList l = new ServerList(Minecraft.getInstance());
						l.load();
						for (ServerData data : ((IMixinServerList)l).getServerListFancyMenu()) {
							if (data.ip.equals(ipRaw)) {
								d = data;
								break;
							}
						}
						if (d == null) {
							d = new ServerData(ipRaw, ipRaw, false);
							l.add(d);
							l.save();
						}
						ConnectScreen.startConnecting(s, Minecraft.getInstance(), new ServerAddress(ip, port), d);
					}
				}
			}

			/** CUSTOM ACTIONS **/
			ButtonActionContainer c = ButtonActionRegistry.getActionByName(action);
			if (c != null) {
				if (c.hasValue()) {
					c.execute(value);
				} else {
					c.execute(null);
				}
			}
			
		} catch (Exception e) {
			System.out.println("################ ERROR [FANCYMENU] ################");
			System.out.println("An error happened while trying to execute a button action!");
			System.out.println("Action: " + action);
			System.out.println("Value: " + value);
			System.out.println("###################################################");
			e.printStackTrace();
		}
	}

	public static void openWebLink(String url) {
		try {
			String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
			URL u = new URL(url);
			if (!Minecraft.ON_OSX) {
				if (s.contains("win")) {
					Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
				} else {
					if (u.getProtocol().equals("file")) {
						url = url.replace("file:", "file://");
					}
					Runtime.getRuntime().exec(new String[]{"xdg-open", url});
				}
			} else {
				Runtime.getRuntime().exec(new String[]{"open", url});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void openFile(File f) {
		try {
			openWebLink(f.toURI().toURL().toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private  static boolean isMacOS() {
		return Minecraft.ON_OSX;
	}

	private static boolean isWindows() {
		String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		return (s.contains("win"));
	}

	private static void runCMD(String cmd) {
		try {
			if (cmd != null) {
				
				cmd = cmd.replace("];", "%e%;");
				cmd = cmd.replace("[windows:", "%s%windows:");
				cmd = cmd.replace("[macos:", "%s%macos:");
				cmd = cmd.replace("[linux:", "%s%linux:");
				
				if (cmd.contains("%s%") && cmd.contains("%e%;")) {
					String s = null;
					if (isMacOS()) {
						if (cmd.contains("%s%macos:")) {
							s = cmd.split("%s%macos:", 2)[1];
						}
					} else if (isWindows()) {
						if (cmd.contains("%s%windows:")) {
							s = cmd.split("%s%windows:", 2)[1];
						}
					} else {
						if (cmd.contains("%s%linux:")) {
							s = cmd.split("%s%linux:", 2)[1];
						}
					}
					if (s != null) {
						if (s.contains("%e%;")) {
							s = s.split("%e%;", 2)[0];
							Runtime.getRuntime().exec(s);
						}
					} else {
						System.out.println("############## ERROR [FANCYMENU] ##############");
						System.out.println("Invalid value for 'runcmd' button action!");
						System.out.println("Missing OS name in '" + cmd.replace("%s%", "[").replace("%e%", "]") + "'!");
						System.out.println("###############################################");
					}
				} else {
					Runtime.getRuntime().exec(cmd);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Removes all spaces from the beginning of the path and replaces all backslash characters with normal slash characters.
	 */
	private static String cleanPath(String path) {
		int i = 0;
		for (char c : path.toCharArray()) {
			if (c == " ".charAt(0)) {
				i++;
			} else {
				break;
			}
		}
		if (i <= path.length()) {
			return path.substring(i).replace("\\", "/");
		}

		return "";
	}
	
	@SubscribeEvent
	public void onMenuReload(MenuReloadedEvent e) {
		updateButtonScripts();
	}

	public static class ButtonScript {

		public final File script;
		public final List<String> actions = new ArrayList<String>();
		public final List<String> values = new ArrayList<String>();

		public ButtonScript(File script) {
			this.script = script;

			for (String s : FileUtils.getFileLines(script)) {
				String action = "";
				String value = "";
				if (s.contains(":")) {
					action = s.split("[:]", 2)[0].replace(" ", "");
					value = s.split("[:]", 2)[1];
				} else {
					action = s.replace(" ", "");
				}
				actions.add(action);
				values.add(value);
			}
		}

		public void runScript() {
			if (!this.actions.isEmpty()) {
				for (int i = 0; i <= this.actions.size()-1; i++) {
					runButtonAction(this.actions.get(i), this.values.get(i));
				}
			}
		}

	}

}

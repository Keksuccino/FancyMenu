package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.io.Files;

import de.keksuccino.core.file.FileUtils;
import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.content.IMenu;
import de.keksuccino.core.gui.content.PopupMenu;
import de.keksuccino.core.gui.screens.popup.NotificationPopup;
import de.keksuccino.core.gui.screens.popup.PopupHandler;
import de.keksuccino.core.gui.screens.popup.TextInputPopup;
import de.keksuccino.core.gui.screens.popup.YesNoPopup;
import de.keksuccino.core.input.KeyboardData;
import de.keksuccino.core.input.KeyboardHandler;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.input.StringUtils;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.properties.PropertiesSet;
import de.keksuccino.core.rendering.animation.ExternalTextureAnimationRenderer;
import de.keksuccino.core.rendering.animation.IAnimationRenderer;
import de.keksuccino.core.resources.ExternalTextureResourceLocation;
import de.keksuccino.core.sound.SoundHandler;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomizationProperties;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutAnimation;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutObject;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutString;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutTexture;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.LayoutButton;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.LayoutVanillaButton;
import de.keksuccino.fancymenu.menu.fancy.item.AnimationCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.StringCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.TextureCustomizationItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class LayoutCreatorScreen extends GuiScreen {
	
	public static boolean isActive = false;
	
	private static final ResourceLocation EXPAND_INDICATOR = new ResourceLocation("keksuccino", "expand.png");
	private static final ResourceLocation SHRINK_INDICATOR = new ResourceLocation("keksuccino", "shrink.png");
	private boolean expandHovered = false;
	private boolean expanded = false;
	private boolean expandMouseDown = false;
	private Color expandColor = Color.WHITE;
	
	public final GuiScreen screen;
	private List<LayoutObject> content = new ArrayList<LayoutObject>();
	private List<LayoutVanillaButton> hidden = new ArrayList<LayoutVanillaButton>();
	private Map<String, Boolean> audio = new HashMap<String, Boolean>();
	private Map<Integer, String> vanillaButtonNames = new HashMap<Integer, String>();
	private LayoutObject focused = null;
	private int hiddenIndicatorTick = 0;
	private int hiddenIndicatorCount = 0;
	private boolean renderHiddenIndicator = false;
	
	private List<IMenu> menus = new ArrayList<IMenu>();
	
	private AdvancedButton addObjectButton;
	private AdvancedButton hiddenButton;
	private AdvancedButton audioButton;
	private AdvancedButton closeButton;
	private AdvancedButton saveButton;
	
	private PopupMenu backgroundRightclickMenu;
	private PopupMenu chooseBackgroundAnimationMenu;
	private PopupMenu addAnimationMenu;
	private PopupMenu addObjectPopup;
	private PopupMenu hiddenPopup;
	private PopupMenu audioPopup;
	private List<PopupMenu> audioSubPopups = new ArrayList<PopupMenu>();
	private PopupMenu renderorderPopup;
	private PopupMenu mcversionPopup;
	private PopupMenu fmversionPopup;
	
	private AdvancedButton renderorderBackgroundButton;
	private AdvancedButton renderorderForegroundButton;
	
	private IAnimationRenderer backgroundAnimation;
	private ExternalTextureResourceLocation backgroundTexture;
	private String backgroundAnimationName;
	
	private String renderorder = "foreground";
	private String requiredmods;
	private String minimumMC;
	private String maximumMC;
	private String minimumFM;
	private String maximumFM;
	
	public LayoutCreatorScreen(GuiScreen screenToCustomize) {
		this.screen = screenToCustomize;

		KeyboardHandler.addKeyPressedListener(this::updatePositionArrowKeys);
		KeyboardHandler.addKeyPressedListener(this::onDeletePress);
	}
	
	@Override
	public void initGui() {
		this.focused = null;
		this.updateContent();
		
		for (IMenu m : this.menus) {
			m.closeMenu();
		}
		
		this.addObjectButton = new AdvancedButton(17, (this.height / 2) - 104, 40, 40, "Add", true, (onPress) -> {
			if (this.addObjectPopup.isOpen()) {
				this.addObjectPopup.closeMenu();
			} else {
				this.addObjectPopup.openMenuAt(onPress.x + onPress.width + 2, onPress.y);
			}
		});
		LayoutCreatorScreen.colorizeCreatorButton(addObjectButton);
		
		this.hiddenButton = new AdvancedButton(17, (this.height / 2) - 62, 40, 40, "Hidden", true, (onPress) -> {
			if (this.hiddenPopup.isOpen()) {
				this.hiddenPopup.closeMenu();
			} else {
				this.hiddenPopup.openMenuAt(onPress.x + onPress.width + 2, onPress.y);
			}
		});
		LayoutCreatorScreen.colorizeCreatorButton(hiddenButton);
		
		this.audioButton = new AdvancedButton(17, (this.height / 2) - 20, 40, 40, "Audio", true, (onPress) -> {
			if (this.audioPopup.isOpen()) {
				this.audioPopup.closeMenu();
			} else {
				this.audioPopup.openMenuAt(onPress.x + onPress.width + 2, onPress.y);
			}
		});
		LayoutCreatorScreen.colorizeCreatorButton(audioButton);
		
		this.saveButton = new AdvancedButton(17, (this.height / 2) + 22, 40, 40, "Save", true, (onPress) -> {
			if (!this.isLayoutEmpty()) {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new LayoutSavePopup(this::saveCustomizationFileCallback));
			} else {
				this.displayNotification(300, "§c§lLayout is empty!", "", "Your layout has no content.", "", "Do some customization magic first before you save it!", "", "", "");
			}
		});
		LayoutCreatorScreen.colorizeCreatorButton(saveButton);
		
		this.closeButton = new AdvancedButton(17, (this.height / 2) + 64, 40, 40, "Close", true, (onPress) -> {
			PopupHandler.displayPopup(new YesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
				if (call.booleanValue()) {
					isActive = false;
					for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
						if (r instanceof AdvancedAnimation) {
							((AdvancedAnimation)r).stopAudio();
							if (((AdvancedAnimation)r).replayIntro()) {
								((AdvancedAnimation)r).resetAnimation();
							}
						}
					}
					MenuCustomization.stopSounds();
					MenuCustomization.resetSounds();
					MenuCustomizationProperties.loadProperties();
					Minecraft.getMinecraft().displayGuiScreen(this.screen);
				}
			}, "§c§lAre you sure?", "", "Do you really want to close the Layout Creator?", "", "", ""));
		});
		LayoutCreatorScreen.colorizeCreatorButton(closeButton);
		
		if (this.chooseBackgroundAnimationMenu == null) {
			this.chooseBackgroundAnimationMenu = this.generateAnimationMenu(this::setBackgroundAnimation);
		}
		
		if (this.chooseBackgroundAnimationMenu == null) {
			this.chooseBackgroundAnimationMenu = this.generateAnimationMenu(this::setBackgroundAnimation);
		}
		
		if (this.addAnimationMenu == null) {
			this.addAnimationMenu = this.generateAnimationMenu(this::addAnimation);
		}

		if (this.addObjectPopup == null) {
			this.addObjectPopup = new PopupMenu(130, -1);
			this.addMenu(this.addObjectPopup);
			
			AdvancedButton b1 = new AdvancedButton(0, 0, 0, 20, "Image", (press) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), "§lImage Path:", null, 240, this::addTexture));
			});
			LayoutCreatorScreen.colorizeCreatorButton(b1);
			this.addObjectPopup.addContent(b1);
			
			AdvancedButton b2 = new AdvancedButton(0, 0, 0, 20, "Animation", (press) -> {
				if (this.addAnimationMenu.isOpen()) {
					this.addAnimationMenu.closeMenu();
				} else {
					this.addAnimationMenu.openMenuAt(press.x + press.width + 2, press.y);
				}
				this.chooseBackgroundAnimationMenu.closeMenu();
			});
			LayoutCreatorScreen.colorizeCreatorButton(b2);
			this.addObjectPopup.addContent(b2);
			
			AdvancedButton b3 = new AdvancedButton(0, 0, 0, 20, "Text", (press) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), "§lNew Text:", null, 240, this::addText));
			});
			LayoutCreatorScreen.colorizeCreatorButton(b3);
			this.addObjectPopup.addContent(b3);
			
			AdvancedButton b5 = new AdvancedButton(0, 0, 0, 20, "Button", (press) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), "§lButton Label:", null, 240, this::addButton));
			});
			LayoutCreatorScreen.colorizeCreatorButton(b5);
			this.addObjectPopup.addContent(b5);
			
			AdvancedButton b6 = new AdvancedButton(0, 0, 0, 20, "Audio", (press) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), "§lAudio Path:", null, 240, (call) -> {
					if (call != null) {
						File f = new File(call);
						if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
							if (!this.audio.containsKey(call)) {
								this.setMenusUseable(true);
								MenuCustomization.registerSound(call, call);
								SoundHandler.playSound(call);
								this.audio.put(call, false);
								this.updateAudioPopup();
							} else {
								this.displayNotification(300, "§c§lAudio already loaded!", "", "The audio you want to add is already part of this layout!", "", "You can't add the same audio multiple times.", "", "", "", "", "", "");
							}
							
						} else {
							this.displayNotification(300, "§c§lInvalid audio file!", "", "The file you want to load does not exists or has an unsupported file type.", "", "Please check your file path and be sure that your file is a valid WAV audio file!", "", "", "", "", "", "");
						}
					} else {
						this.setMenusUseable(true);
					}
				}));
			});
			LayoutCreatorScreen.colorizeCreatorButton(b6);
			this.addObjectPopup.addContent(b6);
			
			AdvancedButton b4 = new AdvancedButton(0, 0, 0, 20, "Background Animation", (press) -> {
				if (this.chooseBackgroundAnimationMenu.isOpen()) {
					this.chooseBackgroundAnimationMenu.closeMenu();
				} else {
					this.chooseBackgroundAnimationMenu.openMenuAt(press.x + press.width + 2, press.y);
				}
				this.addAnimationMenu.closeMenu();
			});
			LayoutCreatorScreen.colorizeCreatorButton(b4);
			this.addObjectPopup.addContent(b4);
			
			AdvancedButton b7 = new AdvancedButton(0, 0, 0, 20, "Background Image", (press) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), "§lImage Path:", null, 240, (call) -> {
					if (call != null) {
						File f = new File(call);
						if (f.exists() && f.isFile() && (f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".jpeg") || f.getName().toLowerCase().endsWith(".png"))) {
							if ((this.backgroundTexture == null) || !this.backgroundTexture.getPath().equals(call)) {
								this.backgroundTexture = new ExternalTextureResourceLocation(call);
								this.backgroundTexture.loadTexture();
								if (this.backgroundAnimation != null) {
									((AdvancedAnimation)this.backgroundAnimation).stopAudio();
								}
								this.backgroundAnimation = null;
							}
							this.setMenusUseable(true);
						} else {
							this.displayNotification(300, "§c§lInvalid image file!", "", "The image you want to load does not exists or has an unsupported file type!", "", "Supported file types are JPG/JPEG and PNG.", "", "", "", "", "", "");
						}
					} else {
						this.setMenusUseable(true);
					}
				}));
			});
			LayoutCreatorScreen.colorizeCreatorButton(b7);
			this.addObjectPopup.addContent(b7);
		}
		
		this.updateHiddenButtonPopup();
		
		this.updateAudioPopup();
		
		if (PopupHandler.isPopupActive()) {
			this.setMenusUseable(false);
		}
		
		if (this.renderorderPopup == null) {
			this.renderorderPopup = new PopupMenu(100, -1);
			this.addMenu(renderorderPopup);

			this.renderorderBackgroundButton = new AdvancedButton(0, 0, 0, 16, "Background", true, (press) -> {
				press.displayString = "§aBackground";
				this.renderorderForegroundButton.displayString = "Foreground";
				this.renderorder = "background";
			});
			this.renderorderPopup.addContent(renderorderBackgroundButton);
			LayoutCreatorScreen.colorizeCreatorButton(renderorderBackgroundButton);
			
			this.renderorderForegroundButton = new AdvancedButton(0, 0, 0, 16, "§aForeground", true, (press) -> {
				press.displayString = "§aForeground";
				this.renderorderBackgroundButton.displayString = "Background";
				this.renderorder = "foreground";
			});
			this.renderorderPopup.addContent(renderorderForegroundButton);
			LayoutCreatorScreen.colorizeCreatorButton(renderorderForegroundButton);
		}
		
		if (this.mcversionPopup == null) {
			this.mcversionPopup = new PopupMenu(100, -1);
			this.addMenu(mcversionPopup);
			
			AdvancedButton m1 = new AdvancedButton(0, 0, 0, 16, "Set Minimum", true, (press) -> {
				this.setMenusUseable(false);
				TextInputPopup p = new TextInputPopup(new Color(0, 0, 0, 0), "§lMinimum Minecraft Version", null, 240, (call) -> {
					if (call != null) {
						this.minimumMC = call;
					}
					this.setMenusUseable(true);
				});
				if (this.minimumMC != null) {
					p.setText(this.minimumMC);
				}
				PopupHandler.displayPopup(p);
			});
			this.mcversionPopup.addContent(m1);
			LayoutCreatorScreen.colorizeCreatorButton(m1);
			
			AdvancedButton m2 = new AdvancedButton(0, 0, 0, 16, "Set Maximum", true, (press) -> {
				this.setMenusUseable(false);
				TextInputPopup p = new TextInputPopup(new Color(0, 0, 0, 0), "§lMaximum Minecraft Version", null, 240, (call) -> {
					if (call != null) {
						this.maximumMC = call;
					}
					this.setMenusUseable(true);
				});
				if (this.maximumMC != null) {
					p.setText(this.maximumMC);
				}
				PopupHandler.displayPopup(p);
			});
			this.mcversionPopup.addContent(m2);
			LayoutCreatorScreen.colorizeCreatorButton(m2);
		}
		
		if (this.fmversionPopup == null) {
			this.fmversionPopup = new PopupMenu(100, -1);
			this.addMenu(fmversionPopup);
			
			AdvancedButton m1 = new AdvancedButton(0, 0, 0, 16, "Set Minimum", true, (press) -> {
				this.setMenusUseable(false);
				TextInputPopup p = new TextInputPopup(new Color(0, 0, 0, 0), "§lMinimum FancyMenu Version", null, 240, (call) -> {
					if (call != null) {
						this.minimumFM = call;
					}
					this.setMenusUseable(true);
				});
				if (this.minimumFM != null) {
					p.setText(this.minimumFM);
				}
				PopupHandler.displayPopup(p);
			});
			this.fmversionPopup.addContent(m1);
			LayoutCreatorScreen.colorizeCreatorButton(m1);
			
			AdvancedButton m2 = new AdvancedButton(0, 0, 0, 16, "Set Maximum", true, (press) -> {
				this.setMenusUseable(false);
				TextInputPopup p = new TextInputPopup(new Color(0, 0, 0, 0), "§lMaximum FancyMenu Version", null, 240, (call) -> {
					if (call != null) {
						this.maximumFM = call;
					}
					this.setMenusUseable(true);
				});
				if (this.maximumFM != null) {
					p.setText(this.maximumFM);
				}
				PopupHandler.displayPopup(p);
			});
			this.fmversionPopup.addContent(m2);
			LayoutCreatorScreen.colorizeCreatorButton(m2);
		}
		
		if (this.backgroundRightclickMenu == null) {
			this.backgroundRightclickMenu = new PopupMenu(110, -1);
			
			AdvancedButton resetBackB = new AdvancedButton(0, 0, 0, 16, "Reset Background", true, (press) -> {
				if (this.backgroundAnimation != null) {
					((AdvancedAnimation)this.backgroundAnimation).stopAudio();
				}
				this.backgroundAnimation = null;
				this.backgroundTexture = null;
				this.backgroundRightclickMenu.closeMenu();
			});
			this.backgroundRightclickMenu.addContent(resetBackB);
			LayoutCreatorScreen.colorizeCreatorButton(resetBackB);
			
			AdvancedButton b2 = new AdvancedButton(0, 0, 0, 16, "Renderorder", true, (press) -> {
				this.fmversionPopup.closeMenu();
				this.mcversionPopup.closeMenu();
				this.renderorderPopup.openMenuAt(press.x + press.width + 2, press.y);
			});
			this.backgroundRightclickMenu.addContent(b2);
			LayoutCreatorScreen.colorizeCreatorButton(b2);
			
			AdvancedButton b3 = new AdvancedButton(0, 0, 0, 16, "Required Mods", true, (press) -> {
				this.setMenusUseable(false);
				TextInputPopup p = new TextInputPopup(new Color(0, 0, 0, 0), "§lRequired Mods [modid, separated by commas]", null, 240, (call) -> {
					if (call != null) {
						this.requiredmods = call;
					}
					this.setMenusUseable(true);
				});
				if (this.requiredmods != null) {
					p.setText(this.requiredmods);
				}
				PopupHandler.displayPopup(p);
			});
			this.backgroundRightclickMenu.addContent(b3);
			LayoutCreatorScreen.colorizeCreatorButton(b3);
			
			AdvancedButton b4 = new AdvancedButton(0, 0, 0, 16, "Minecraft Version", true, (press) -> {
				this.renderorderPopup.closeMenu();
				this.fmversionPopup.closeMenu();
				this.mcversionPopup.openMenuAt(press.x + press.width + 2, press.y);
			});
			this.backgroundRightclickMenu.addContent(b4);
			LayoutCreatorScreen.colorizeCreatorButton(b4);
			
			AdvancedButton b6 = new AdvancedButton(0, 0, 0, 16, "FancyMenu Version", true, (press) -> {
				this.renderorderPopup.closeMenu();
				this.mcversionPopup.closeMenu();
				this.fmversionPopup.openMenuAt(press.x + press.width + 2, press.y);
			});
			this.backgroundRightclickMenu.addContent(b6);
			LayoutCreatorScreen.colorizeCreatorButton(b6);
			
			this.addMenu(this.backgroundRightclickMenu);
		}
	}
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
	}
	
	private void disableLayouts() {
		File f = new File(FancyMenu.getCustomizationPath().getPath() + "/.disabled");
		if (!f.exists()) {
			f.mkdirs();
		}
		
		MenuCustomizationProperties.loadProperties();
		for (PropertiesSet set : MenuCustomizationProperties.getProperties()) {
			List<PropertiesSection> l = set.getPropertiesOfType("customization-meta");
			if (l.isEmpty()) {
				l = set.getPropertiesOfType("type-meta");
			}
			if (l.isEmpty()) {
				continue;
			}
			PropertiesSection sec = l.get(0);
			if (sec.getEntryValue("identifier").equals(this.screen.getClass().getName())) {
				File cusFile = new File(sec.getEntryValue("path"));
				if (cusFile.exists()) {
					try {
						File moveTo = new File(f.getPath() + "/" + cusFile.getName());
						if (moveTo.exists()) {
							moveTo = new File(f.getPath() + "/" + this.generateCustomizationFileName(f.getPath(), Files.getNameWithoutExtension(cusFile.getPath())));
						}
						Files.move(cusFile, moveTo);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public boolean isLayoutEmpty() {
		return this.getAllProperties().isEmpty();
	}
	
	private List<PropertiesSection> getAllProperties() {
		List<PropertiesSection> l = new ArrayList<PropertiesSection>();
		
		PropertiesSection meta = new PropertiesSection("customization-meta");
		meta.addEntry("identifier", this.screen.getClass().getName());
		meta.addEntry("renderorder", this.renderorder);
		if ((this.requiredmods != null) && !this.requiredmods.replace(" ", "").equals("")) {
			meta.addEntry("requiredmods", this.requiredmods);
		}
		if ((this.minimumMC != null) && !this.minimumMC.replace(" ", "").equals("")) {
			meta.addEntry("minimummcversion", this.minimumMC);
		}
		if ((this.maximumMC != null) && !this.maximumMC.replace(" ", "").equals("")) {
			meta.addEntry("maximummcversion", this.maximumMC);
		}
		if ((this.minimumFM != null) && !this.minimumFM.replace(" ", "").equals("")) {
			meta.addEntry("minimumfmversion", this.minimumFM);
		}
		if ((this.maximumFM != null) && !this.maximumFM.replace(" ", "").equals("")) {
			meta.addEntry("maximumfmversion", this.maximumFM);
		}
		l.add(meta);
		
		if (this.backgroundAnimation != null) {
			PropertiesSection ps = new PropertiesSection("customization");
			ps.addEntry("action", "animatebackground");
			ps.addEntry("name", this.backgroundAnimationName);
			ps.addEntry("replayanimation", "false");
			l.add(ps);
		}
		
		if (this.backgroundTexture != null) {
			PropertiesSection ps = new PropertiesSection("customization");
			ps.addEntry("action", "texturizebackground");
			ps.addEntry("path", this.backgroundTexture.getPath());
			l.add(ps);
		}
		
		for (Map.Entry<String, Boolean> m : this.audio.entrySet()) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "addaudio");
			s.addEntry("path", m.getKey());
			s.addEntry("loop", "" + m.getValue());
			l.add(s);
		}
		
		for (LayoutObject o : this.content) {
			l.addAll(o.getProperties());
		}
		return l;
	}
	
	private void saveToCustomizationFile(String fileName) throws IOException {
		List<PropertiesSection> l = this.getAllProperties();
		
		if (!l.isEmpty() && (l.size() > 1)) {
			File f = new File(FancyMenu.getCustomizationPath().getPath() + "/" + fileName);
			f.createNewFile();
			
			String data = "";
			
			data += "type = menu\n\n";
			
			for (PropertiesSection ps : l) {
				data += ps.getSectionType() + " {\n";
				for (Map.Entry<String, String> e : ps.getEntries().entrySet()) {
					data += "  " + e.getKey() + " = " + e.getValue() + "\n";
				}
				data += "}\n\n";
			}
			
			FileUtils.writeTextToFile(f, data, false);
		}
	}
	
	private String generateCustomizationFileName(String dir, String baseName) {
		File f = new File(dir);
		if (!f.exists() && f.isDirectory()) {
			f.mkdirs();
		}

		File f2 = new File(f.getPath() + "/" + baseName + ".txt");
		int i = 1;
		while (f2.exists()) {
			f2 = new File(f.getPath() + "/" + baseName + "_" + i + ".txt");
			i++;
		}
		
		return f2.getName();
	}
	
	private PopupMenu generateAnimationMenu(Consumer<String> callback) {
		PopupMenu p = new PopupMenu(120, -1);
		
		AdvancedButton inputAniB = new AdvancedButton(0, 0, 0, 20, "Enter Animation Name", true, (press) -> {
			this.setMenusUseable(false);
			PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), "§lAnimation Name:", null, 240, callback));
		});
		p.addContent(inputAniB);
		LayoutCreatorScreen.colorizeCreatorButton(inputAniB);
		
		for (String s : AnimationHandler.getCustomAnimationNames()) {
			String name = s;
			if (Minecraft.getMinecraft().fontRenderer.getStringWidth(name) > 90) {
				name = Minecraft.getMinecraft().fontRenderer.trimStringToWidth(name, 90) + "..";
			}
			
			AdvancedButton aniB = new AdvancedButton(0, 0, 0, 20, name, true, (press) -> {
				callback.accept(s);
				p.closeMenu();
			});
			p.addContent(aniB);
			LayoutCreatorScreen.colorizeCreatorButton(aniB);
		}
		this.addMenu(p);
		
		return p;
	}
	
	/**
	 * Updates the LayoutObjects shown in the CreatorScreen.<br>
	 * The positions of all UNMODIFIED vanilla buttons will be updated to keep them at the correct position when the screen is getting resized.
	 */
	private void updateContent() {
		List<LayoutObject> l = new ArrayList<LayoutObject>();
		for (LayoutObject o : this.content) {
			if (!(o instanceof LayoutVanillaButton)) {
				l.add(o);
			} else {
				if (!o.object.orientation.equals("original") || ((LayoutVanillaButton)o).hidden) {
					l.add(o);
				}
			}
		}
		
		ButtonCache.cacheFrom(this.screen, this.width, this.height);
		
		this.content.clear();
		for (ButtonData b : ButtonCache.getButtons()) {
			if (!this.containsVanillaButton(l, b)) {
				LayoutVanillaButton v = new LayoutVanillaButton(b, this);
				if (this.vanillaButtonNames.containsKey(b.getId())) {
					v.object.value = this.vanillaButtonNames.get(b.getId()); 
				}
				content.add(v);
			}
		}
		this.content.addAll(l);
	}
	
	private boolean containsVanillaButton(List<LayoutObject> l, ButtonData b) {
		for (LayoutObject o : l) {
			if (o instanceof LayoutVanillaButton) {
				if (((LayoutVanillaButton)o).button.getId() == b.getId()) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean isHidden(LayoutObject b) {
		return this.hidden.contains(b);
	}
	
	private void updateHiddenButtonPopup() {
		if ((this.hiddenPopup != null) && this.menus.contains(this.hiddenPopup)) {
			this.menus.remove(this.hiddenPopup);
		}
		this.hiddenPopup = new PopupMenu(100, -1);
		this.addMenu(this.hiddenPopup);
		
		if (this.hidden.isEmpty()) {
			AdvancedButton bt = new AdvancedButton(0, 0, 0, 16, "EMPTY", true, (press) -> {
			});
			this.hiddenPopup.addContent(bt);
			LayoutCreatorScreen.colorizeCreatorButton(bt);
		} else {
			for (LayoutVanillaButton b : this.hidden) {
				String label = b.button.label;
				if (Minecraft.getMinecraft().fontRenderer.getStringWidth(label) > 80) {
					label = Minecraft.getMinecraft().fontRenderer.trimStringToWidth(label, 80) + "..";
				}
				AdvancedButton bt = new AdvancedButton(0, 0, 0, 16, label, true, (press) -> {
					this.showVanillaButton(b);
				});
				this.hiddenPopup.addContent(bt);
				LayoutCreatorScreen.colorizeCreatorButton(bt);
			}
		}
	}
	
	private void updateAudioPopup() {
		if ((this.audioPopup != null) && this.menus.contains(this.audioPopup)) {
			this.menus.remove(this.audioPopup);
		}
		this.audioSubPopups.clear();
		this.audioPopup = new PopupMenu(100, -1);
		this.addMenu(this.audioPopup);
		
		if (this.audio.isEmpty()) {
			AdvancedButton bt = new AdvancedButton(0, 0, 0, 16, "EMPTY", true, (press) -> {
			});
			this.audioPopup.addContent(bt);
			LayoutCreatorScreen.colorizeCreatorButton(bt);
		} else {
			for (Map.Entry<String, Boolean> m : this.audio.entrySet()) {
				String label = new File(m.getKey()).getName();
				if (Minecraft.getMinecraft().fontRenderer.getStringWidth(label) > 80) {
					label = Minecraft.getMinecraft().fontRenderer.trimStringToWidth(label, 80) + "..";
				}
				
				PopupMenu actions = new PopupMenu(100, -1);
				
				AdvancedButton a1 = new AdvancedButton(0, 0, 0, 16, "Delete", true, (press2) -> {
					this.audioPopup.closeMenu();
					this.setMenusUseable(false);
					PopupHandler.displayPopup(new YesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
						if (call.booleanValue()) {
							this.audio.remove(m.getKey());
							SoundHandler.stopSound(m.getKey());
							MenuCustomization.unregisterSound(m.getKey());
							this.updateAudioPopup();
						}
						this.setMenusUseable(true);
					}, "§c§lAre you sure?", "", "", "Do you really want to delete this audio?", "", ""));
				});
				actions.addContent(a1);
				LayoutCreatorScreen.colorizeCreatorButton(a1);
				
				String lab = "Enable Loop";
				if (m.getValue()) {
					lab = "Disable Loop";
				}
				AdvancedButton a2 = new AdvancedButton(0, 0, 0, 16, lab, true, (press2) -> {
					if (press2.displayString.equals("Enable Loop")) {
						SoundHandler.setLooped(m.getKey(), true);
						this.audio.put(m.getKey(), true);
						press2.displayString = "Disable Loop";
					} else {
						SoundHandler.setLooped(m.getKey(), false);
						this.audio.put(m.getKey(), false);
						press2.displayString = "Enable Loop";
					}
				});
				actions.addContent(a2);
				LayoutCreatorScreen.colorizeCreatorButton(a2);
				
				AdvancedButton bt = new AdvancedButton(0, 0, 0, 16, label, true, (press) -> {
					actions.openMenuAt(press.x + press.width + 2, press.y);
				});
				this.audioPopup.addContent(bt);
				LayoutCreatorScreen.colorizeCreatorButton(bt);
				
				this.audioSubPopups.add(actions);
			}
		}
	}
	
	public void setVanillaButtonName(LayoutVanillaButton button, String text) {
		this.vanillaButtonNames.put(button.button.getId(), text);
		button.object.value = text;
	}
	
	public void hideVanillaButton(LayoutVanillaButton b) {
		if (!this.hidden.contains(b) && this.content.contains(b)) {
			this.hidden.add(b);
			b.hidden = true;
			this.setObjectFocused(b, false);
			this.updateHiddenButtonPopup();
			this.renderHiddenIndicator = true;
		}
	}
	
	public void showVanillaButton(LayoutVanillaButton b) {
		if (this.isHidden(b)) {
			this.hidden.remove(b);
			b.hidden = false;
			this.updateHiddenButtonPopup();
			this.updateContent();
		}
	}
	
	public void addMenu(IMenu menu) {
		if (!this.menus.contains(menu)) {
			this.menus.add(menu);
		}
	}
	
	public void setMenusUseable(boolean b) {
		for (IMenu m : this.menus) {
			m.setUseable(b);
		}
	}
	
	public boolean isMenuOpen() {
		for (IMenu m : this.menus) {
			if (m.isOpen()) {
				return true;
			}
		}
		return false;
	}
	
	public void addContent(LayoutObject object) {
		if (!this.content.contains(object)) {
			this.content.add(object);
		}
	}
	
	public void removeContent(LayoutObject object) {
		if (this.content.contains(object)) {
			if ((this.isFocused(object))) {
				this.focused = null;
			}
			this.content.remove(object);
			this.updateContent();
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.renderCreatorBackground();
		
		//Renders all layout objects. The focused object is always rendered on top of all other objects.
		for (LayoutObject l : this.content) {
			if (!this.isHidden(l)) {
				if (this.focused == null) {
					l.render(mouseX, mouseY);
				} else {
					if (l != this.focused) {
						l.render(mouseX, mouseY);
					}
				}
			}
		}
		if (this.isObjectFocused()) {
			this.focused.render(mouseX, mouseY);
		}
		
		this.addObjectPopup.render(mouseX, mouseY);

		this.chooseBackgroundAnimationMenu.render(mouseX, mouseY);
		this.addAnimationMenu.render(mouseX, mouseY);
		
		this.hiddenPopup.render(mouseX, mouseY);
		this.renderHiddenButtonIndicator();
		
		this.audioPopup.render(mouseX, mouseY);
		
		boolean sub = false;
		for (PopupMenu p : this.audioSubPopups) {
			p.render(mouseX, mouseY);
			
			if (!audioPopup.isOpen() && !p.isHovered()) {
				p.closeMenu();
			}
			if (p.isHovered()) {
				sub = true;
			}
		}
		
		if (this.backgroundRightclickMenu != null) {
			if (MouseInput.isRightMouseDown() && !this.isContentHovered() && !this.isObjectFocused()) {
				this.backgroundRightclickMenu.openMenuAt(mouseX, mouseY);
			}
			this.backgroundRightclickMenu.render(mouseX, mouseY);
		}
		
		this.renderorderPopup.render(mouseX, mouseY);
		
		this.mcversionPopup.render(mouseX, mouseY);
		
		this.fmversionPopup.render(mouseX, mouseY);
		
		if (!this.backgroundRightclickMenu.isOpen()) {
			this.renderorderPopup.closeMenu();
			this.mcversionPopup.closeMenu();
			this.fmversionPopup.closeMenu();
		}
		
		if (this.expanded) {
			this.closeButton.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks);
			this.saveButton.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks);
			this.audioButton.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks);
			this.hiddenButton.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks);
			this.addObjectButton.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks);
		}
		
		if (MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown()) {
			if (!this.hiddenButton.isMouseOver() && !this.hiddenPopup.isHovered()) {
				this.hiddenPopup.closeMenu();
			}
			if (!this.audioButton.isMouseOver() && !this.audioPopup.isHovered() && !sub) {
				this.audioPopup.closeMenu();
			}
			if (!this.addObjectButton.isMouseOver() && !this.addObjectPopup.isHovered()) {
				this.addObjectPopup.closeMenu();
			}
			if ((!this.backgroundRightclickMenu.isHovered() && !this.renderorderPopup.isHovered() && !this.mcversionPopup.isHovered() && !this.fmversionPopup.isHovered()) || this.isObjectFocused()) {
				this.backgroundRightclickMenu.closeMenu();
			}
		}
		
		if (!this.addObjectPopup.isOpen()) {
			this.addAnimationMenu.closeMenu();
			this.chooseBackgroundAnimationMenu.closeMenu();
		}
		
		if (PopupHandler.isPopupActive() || this.isObjectFocused()) {
			this.saveButton.setUseable(false);
			this.audioButton.setUseable(false);
			this.hiddenButton.setUseable(false);
			this.closeButton.setUseable(false);
			this.addObjectButton.setUseable(false);
		} else {
			this.saveButton.setUseable(true);
			this.audioButton.setUseable(true);
			this.hiddenButton.setUseable(true);
			this.closeButton.setUseable(true);
			this.addObjectButton.setUseable(true);
		}
		
		this.renderMenuExpandIndicator(mouseX, mouseY);
		
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
	
	private void renderMenuExpandIndicator(int mouseX, int mouseY) {
		GlStateManager.enableBlend();
		int x = 5;
		int y = (this.height / 2) - 10;
		if (this.expanded) {
			Minecraft.getMinecraft().getTextureManager().bindTexture(SHRINK_INDICATOR);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 0.5F);
		} else {
			Minecraft.getMinecraft().getTextureManager().bindTexture(EXPAND_INDICATOR);
			GlStateManager.color(this.expandColor.getRed(), this.expandColor.getGreen(), this.expandColor.getBlue(), 0.5F);
		}
		Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, 20, 20, 20, 20);
		GlStateManager.resetColor();
		GlStateManager.disableBlend();
		
		if ((mouseX >= x) && (mouseX <= x + 7) && (mouseY >= y + 2) && mouseY <= y + 18) {
			this.expandHovered = true;
		} else {
			this.expandHovered = false;
		}
		if (this.expandHovered && MouseInput.isLeftMouseDown() && !this.expandMouseDown) {
			if (this.expanded) {
				this.expanded = false;
			} else {
				this.expanded = true;
			}
			this.expandMouseDown = true;
		}
		if (!MouseInput.isLeftMouseDown()) {
			this.expandMouseDown = false;
		}
	}
	
	private void renderHiddenButtonIndicator() {
		if (this.renderHiddenIndicator) {
			if (this.hiddenIndicatorTick == 0) {
				if ((this.hiddenIndicatorCount == 0) || (this.hiddenIndicatorCount == 2) || (this.hiddenIndicatorCount == 4)) {
					this.hiddenButton.displayString = "§4Hidden";
					this.expandColor = Color.RED;
				}
				if ((this.hiddenIndicatorCount == 1) || (this.hiddenIndicatorCount == 3) || (this.hiddenIndicatorCount == 5)) {
					this.hiddenButton.displayString = "Hidden";
					this.expandColor = Color.WHITE;
					if (this.hiddenIndicatorCount == 5) {
						this.renderHiddenIndicator = false;
					}
				}
				this.hiddenIndicatorCount++;
			}
			this.hiddenIndicatorTick++;
			if (this.hiddenIndicatorTick == 20) {
				this.hiddenIndicatorTick = 0;
			}
		} else {
			this.hiddenIndicatorCount = 0;
			this.hiddenIndicatorTick = 0;
		}
	}
	
	private void renderCreatorBackground() {
		GlStateManager.enableBlend();
		Gui.drawRect(0, 0, this.width, this.height, new Color(38, 38, 38).getRGB());
		GlStateManager.disableBlend();
		
		if (this.backgroundTexture != null) {
			GlStateManager.enableBlend();
			Minecraft.getMinecraft().getTextureManager().bindTexture(this.backgroundTexture.getResourceLocation());
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GuiScreen.drawModalRectWithCustomSizedTexture(0, 0, 1.0F, 1.0F, this.width, this.height, this.width, this.height);
			GlStateManager.disableBlend();
		}
		
		if (this.backgroundAnimation != null) {
			boolean b = this.backgroundAnimation.isStretchedToStreensize();
			this.backgroundAnimation.setStretchImageToScreensize(true);
			this.backgroundAnimation.render();
			this.backgroundAnimation.setStretchImageToScreensize(b);
		}
	}
	
	public boolean isFocused(LayoutObject object) {
		if (PopupHandler.isPopupActive()) {
			return false;
		}
		return (this.focused == object);
	}
	
	public void setObjectFocused(LayoutObject object, boolean b) {
		if (!this.content.contains(object)) {
			return;
		}
		if (this.isHidden(object)) {
			return;
		}
		if (PopupHandler.isPopupActive()) {
			return;
		}
		if (b) {
			if (this.backgroundRightclickMenu.isHovered() || this.renderorderPopup.isHovered()) {
				return;
			}
			if (this.focused == null) {
				this.focused = object;
			}
		} else {
			if ((this.focused != null) && (this.focused == object)) {
				this.focused = null;
			}
		}
	}
	
	public boolean isObjectFocused() {
		return (this.focused != null);
	}
	
	public boolean isContentHovered() {
		for (LayoutObject o : this.content) {
			if (o.isHovered()) {
				return true;
			}
		}
		return false;
	}
	
	public void displayNotification(int width, String... strings) {
		PopupHandler.displayPopup(new NotificationPopup(width, new Color(0, 0, 0, 0), 240, this::notificationClosedCallback, strings));
		this.setMenusUseable(false);
	}
	
	public static void colorizeCreatorButton(AdvancedButton b) {
		b.setBackgroundColor(new Color(102, 102, 153), new Color(133, 133, 173), new Color(163, 163, 194), new Color(163, 163, 194), 1);
	}
	
	private void notificationClosedCallback() {
		this.setMenusUseable(true);
	}

	private void addTexture(String path) {
		if (path == null) {
			this.setMenusUseable(true);
			return;
		}
		System.out.println(path);
		File f = new File(path);
		if (f.exists()) {
			TextureCustomizationItem i = new TextureCustomizationItem(new PropertiesSection(""));
			i.action = "addtexture";
			i.value = path;
			i.height = 100;
			i.width = 100;
			i.renderer = new ExternalTextureAnimationRenderer(1, false, 0, 0, 20, 20, path);
			this.addContent(new LayoutTexture(i, this));
			
			this.setMenusUseable(true);
		} else {
			this.displayNotification(300, "§c§lImage not found!", "", "The image you want do add does not exists", "at the specified location!", "", "", "", "", "", "");
		}
	}
	
	private void addAnimation(String name) {
		this.addAnimationMenu.closeMenu();
		if (name == null) {
			this.setMenusUseable(true);
			return;
		}
		if (AnimationHandler.animationExists(name)) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "addanimation");
			s.addEntry("name", name);
			AnimationCustomizationItem i = new AnimationCustomizationItem(s);
			i.height = 100;
			i.width = 100;
			AnimationHandler.getAnimation(name).resetAnimation();
			this.addContent(new LayoutAnimation(i, this));
			
			this.setMenusUseable(true);
		} else {
			this.displayNotification(300, "§c§lAnimation not found!", "", "The animation you want to add does not exists!", "", "Maybe you forgot to add all mandatory variables to its properties?", "", "Always keep in mind that you have to restart your game", "after you've added a new animation to the animations folder.", "", "", "");
		}
	}
	
	private void addButton(String label) {
		if (label == null) {
			this.setMenusUseable(true);
			return;
		}
		this.addContent(new LayoutButton(100, 20, label, this));
		this.setMenusUseable(true);
	}
	
	private void addText(String text) {
		if (text == null) {
			this.setMenusUseable(true);
			return;
		}
		if (text.length() > 0) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "addtext");
			s.addEntry("value", StringUtils.convertFormatCodes(text, "&", "§"));
			StringCustomizationItem i = new StringCustomizationItem(s);
			this.addContent(new LayoutString(i, this));
			
			this.setMenusUseable(true);
		} else {
			this.displayNotification(300, "§c§lText too short!", "", "Your text needs at least one character to be a text!", "Otherwise..it would be nothing.", "", "Nobody wants to be nothing, so don't do that to your text!", "", "", "", "");
		}
	}
	
	private void setBackgroundAnimation(String name) {
		this.chooseBackgroundAnimationMenu.closeMenu();
		if (name == null) {
			this.setMenusUseable(true);
			return;
		}
		if (AnimationHandler.animationExists(name)) {
			this.backgroundTexture = null;
			this.backgroundAnimation = AnimationHandler.getAnimation(name);
			this.backgroundAnimation.resetAnimation();
			this.backgroundAnimationName = name;
			this.setMenusUseable(true);
		} else {
			this.displayNotification(300, "§c§lAnimation not found!", "", "The animation you want to add does not exists!", "", "Maybe you forgot to add all mandatory variables to its properties?", "", "Always keep in mind that you have to restart your game", "after you've added a new animation to the animations folder.", "", "", "");
		}
	}
	
	private void saveCustomizationFileCallback(Integer i) {
		if (i == 2) {
			this.disableLayouts();
		}
		if ((i == 1) || (i == 2)) {
			try {
				String name = this.screen.getClass().getName();
				if (name.contains(".")) {
					name = new StringBuilder(new StringBuilder(name).reverse().toString().split("[.]", 2)[0]).reverse().toString();
				}
				this.saveToCustomizationFile(this.generateCustomizationFileName(FancyMenu.getCustomizationPath().getPath(), name));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.setMenusUseable(true);
	}
	
	private void updatePositionArrowKeys(KeyboardData d) {
		if ((this == Minecraft.getMinecraft().currentScreen) && (this.focused != null) && !PopupHandler.isPopupActive()) {
			if (!((d.keycode == 203) || (d.keycode == 205) || (d.keycode == 200) || (d.keycode == 208))) {
				return;
			}
			if ((this.focused instanceof LayoutVanillaButton) && this.focused.object.orientation.equals("original")) {
				((LayoutVanillaButton)this.focused).displaySetOrientationNotification();
				return;
			}
			if (d.keycode == 203) {
				this.focused.setX(this.focused.object.posX - 1);
			}
			if (d.keycode == 205) {
				this.focused.setX(this.focused.object.posX + 1);
			}
			if (d.keycode == 200) {
				this.focused.setY(this.focused.object.posY - 1);
			}
			if (d.keycode == 208) {
				this.focused.setY(this.focused.object.posY + 1);
			}
		}
	}
	
	private void onDeletePress(KeyboardData d) {
		if ((this == Minecraft.getMinecraft().currentScreen) && (this.focused != null) && !PopupHandler.isPopupActive()) {
			if (d.keycode == 211) {
				if (this.focused.isDestroyable()) {
					this.focused.destroyObject();
				} else {
					this.displayNotification(300, "§c§lObject can't be deleted!", "", "Sorry, but you cannot delete this object.", "", "You will need to learn how to live with it..", "", "", "");
				}
			}
		}
	}

}

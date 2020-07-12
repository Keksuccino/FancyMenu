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
import de.keksuccino.core.input.CharacterFilter;
import de.keksuccino.core.input.KeyboardData;
import de.keksuccino.core.input.KeyboardHandler;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.input.StringUtils;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.properties.PropertiesSerializer;
import de.keksuccino.core.properties.PropertiesSet;
import de.keksuccino.core.rendering.RenderUtils;
import de.keksuccino.core.rendering.animation.IAnimationRenderer;
import de.keksuccino.core.resources.TextureHandler;
import de.keksuccino.core.resources.ExternalTextureResourceLocation;
import de.keksuccino.core.sound.SoundHandler;
import de.keksuccino.core.web.WebUtils;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.localization.Locals;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomizationProperties;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.BackgroundOptionsPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.ChooseFilePopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutAnimation;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutObject;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutString;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutTexture;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutWebString;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutWebTexture;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.LayoutButton;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.LayoutButtonDummyCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.LayoutVanillaButton;
import de.keksuccino.fancymenu.menu.fancy.item.AnimationCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.StringCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.TextureCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.WebStringCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.WebTextureCustomizationItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class LayoutCreatorScreen extends GuiScreen {
	
	public static boolean isActive = false;
	
	protected static final ResourceLocation EXPAND_INDICATOR = new ResourceLocation("keksuccino", "expand.png");
	protected static final ResourceLocation SHRINK_INDICATOR = new ResourceLocation("keksuccino", "shrink.png");
	protected boolean expandHovered = false;
	protected boolean expanded = false;
	protected boolean expandMouseDown = false;
	protected Color expandColor = Color.WHITE;
	
	protected boolean leftDownAndFocused = false;
	
	public final GuiScreen screen;
	protected List<LayoutObject> content = new ArrayList<LayoutObject>();
	protected List<LayoutVanillaButton> hidden = new ArrayList<LayoutVanillaButton>();
	protected Map<String, Boolean> audio = new HashMap<String, Boolean>();
	protected Map<Integer, String> vanillaButtonNames = new HashMap<Integer, String>();
	protected Map<Integer, List<String>> vanillaButtonTextures = new HashMap<Integer, List<String>>();
	protected Map<Integer, Integer> vanillaButtonClicks = new HashMap<Integer, Integer>();
	protected Map<Integer, String> vanillaHoverLabels = new HashMap<Integer, String>();
	protected Map<Integer, String> vanillaHoverSounds = new HashMap<Integer, String>();
	protected Map<Integer, Double> vanillaHideFor = new HashMap<Integer, Double>();
	protected LayoutObject focused = null;
	protected int hiddenIndicatorTick = 0;
	protected int hiddenIndicatorCount = 0;
	protected boolean renderHiddenIndicator = false;
	
	protected List<IMenu> menus = new ArrayList<IMenu>();
	
	protected AdvancedButton addObjectButton;
	protected AdvancedButton hiddenButton;
	protected AdvancedButton audioButton;
	protected AdvancedButton closeButton;
	protected AdvancedButton saveButton;
	
	protected PopupMenu backgroundRightclickMenu;
	protected PopupMenu addAnimationMenu;
	protected PopupMenu addObjectPopup;
	protected PopupMenu hiddenPopup;
	protected PopupMenu audioPopup;
	protected List<PopupMenu> audioSubPopups = new ArrayList<PopupMenu>();
	protected PopupMenu renderorderPopup;
	protected PopupMenu mcversionPopup;
	protected PopupMenu fmversionPopup;
	
	protected AdvancedButton renderorderBackgroundButton;
	protected AdvancedButton renderorderForegroundButton;
	
	protected IAnimationRenderer backgroundAnimation;
	public ExternalTextureResourceLocation backgroundTexture;
	public String backgroundTexturePath;
	public List<String> backgroundAnimationNames = new ArrayList<String>();
	public boolean randomBackgroundAnimation = false;
	public boolean panorama = false;
	private int panoTick = 0;
	private double panoPos = 0.0;
	private boolean panoMoveBack = false;
	private boolean panoStop = false;
	
	protected String renderorder = "foreground";
	protected String requiredmods;
	protected String minimumMC;
	protected String maximumMC;
	protected String minimumFM;
	protected String maximumFM;
	
	private Map<String, Boolean> focusChangeBlocker = new HashMap<String, Boolean>();
	private LayoutObject topObject;
	
	public LayoutCreatorScreen(GuiScreen screenToCustomize) {
		this.screen = screenToCustomize;

		KeyboardHandler.addKeyPressedListener(this::updatePositionArrowKeys);
		KeyboardHandler.addKeyPressedListener(this::onDeletePress);
	}
	
	@Override
	public void initGui() {
		this.focused = null;
		this.updateContent();
		
		this.focusChangeBlocker.clear();
		
		for (IMenu m : this.menus) {
			m.closeMenu();
		}
		
		this.addObjectButton = new AdvancedButton(17, (this.height / 2) - 104, 40, 40, Locals.localize("helper.creator.menu.add"), true, (onPress) -> {
			if (this.addObjectPopup.isOpen()) {
				this.addObjectPopup.closeMenu();
			} else {
				this.addObjectPopup.openMenuAt(onPress.x + onPress.width + 2, onPress.y);
			}
		});
		LayoutCreatorScreen.colorizeCreatorButton(addObjectButton);
		
		this.hiddenButton = new AdvancedButton(17, (this.height / 2) - 62, 40, 40, Locals.localize("helper.creator.menu.hidden"), true, (onPress) -> {
			if (this.hiddenPopup.isOpen()) {
				this.hiddenPopup.closeMenu();
			} else {
				this.hiddenPopup.openMenuAt(onPress.x + onPress.width + 2, onPress.y);
			}
		});
		LayoutCreatorScreen.colorizeCreatorButton(hiddenButton);
		
		this.audioButton = new AdvancedButton(17, (this.height / 2) - 20, 40, 40, Locals.localize("helper.creator.menu.audio"), true, (onPress) -> {
			if (this.audioPopup.isOpen()) {
				this.audioPopup.closeMenu();
			} else {
				this.audioPopup.openMenuAt(onPress.x + onPress.width + 2, onPress.y);
			}
		});
		LayoutCreatorScreen.colorizeCreatorButton(audioButton);
		
		this.saveButton = new AdvancedButton(17, (this.height / 2) + 22, 40, 40, Locals.localize("helper.creator.menu.save"), true, (onPress) -> {
			if (!this.isLayoutEmpty()) {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new LayoutSavePopup(this::saveCustomizationFileCallback));
			} else {
				this.displayNotification(300, "§c§l" + Locals.localize("helper.creator.layoutempty.title"), "", Locals.localize("helper.creator.layoutempty.desc"), "", "", "");
			}
		});
		LayoutCreatorScreen.colorizeCreatorButton(saveButton);
		
		this.closeButton = new AdvancedButton(17, (this.height / 2) + 64, 40, 40, Locals.localize("helper.creator.menu.close"), true, (onPress) -> {
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
			}, "§c§l" + Locals.localize("helper.creator.messages.sure"), "", Locals.localize("helper.creator.close"), "", "", ""));
		});
		LayoutCreatorScreen.colorizeCreatorButton(closeButton);
		
		if (this.addAnimationMenu == null) {
			this.addAnimationMenu = this.generateAnimationMenu(this::addAnimation);
		}

		if (this.addObjectPopup == null) {
			this.addObjectPopup = new PopupMenu(130, 16, -1);
			this.addMenu(this.addObjectPopup);
			
			AdvancedButton b1 = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.image"), (press) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new ChooseFilePopup(this::addTexture, "jpg", "jpeg", "png"));
			});
			LayoutCreatorScreen.colorizeCreatorButton(b1);
			this.addObjectPopup.addContent(b1);
			
			AdvancedButton b4 = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.webimage"), (press) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.web.enterurl"), null, 240, this::addWebTexture));
			});
			LayoutCreatorScreen.colorizeCreatorButton(b4);
			this.addObjectPopup.addContent(b4);
			
			AdvancedButton b2 = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.animation"), (press) -> {
				if (this.addAnimationMenu.isOpen()) {
					this.addAnimationMenu.closeMenu();
				} else {
					this.addAnimationMenu.openMenuAt(press.x + press.width + 2, press.y);
				}
			});
			LayoutCreatorScreen.colorizeCreatorButton(b2);
			this.addObjectPopup.addContent(b2);
			
			AdvancedButton b3 = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.text"), (press) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.add.text.newtext") + ":", null, 240, this::addText));
			});
			LayoutCreatorScreen.colorizeCreatorButton(b3);
			this.addObjectPopup.addContent(b3);
			
			AdvancedButton b7 = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.webtext"), (press) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.web.enterurl"), null, 240, this::addWebText));
			});
			LayoutCreatorScreen.colorizeCreatorButton(b7);
			this.addObjectPopup.addContent(b7);
			
			AdvancedButton b5 = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.button"), (press) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.add.button.label") + ":", null, 240, this::addButton));
			});
			LayoutCreatorScreen.colorizeCreatorButton(b5);
			this.addObjectPopup.addContent(b5);
			
			AdvancedButton b6 = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.audio"), (press) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new ChooseFilePopup(this::addAudio, "wav"));
			});
			LayoutCreatorScreen.colorizeCreatorButton(b6);
			this.addObjectPopup.addContent(b6);
		}
		
		this.updateHiddenButtonPopup();
		
		this.updateAudioPopup();
		
		if (PopupHandler.isPopupActive()) {
			this.setMenusUseable(false);
		}
		
		if (this.renderorderPopup == null) {
			this.renderorderPopup = new PopupMenu(100, 16, -1);
			this.addMenu(renderorderPopup);

			this.renderorderBackgroundButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.renderorder.background"), true, (press) -> {
				press.displayString = "§a" + Locals.localize("helper.creator.layoutoptions.renderorder.background");
				this.renderorderForegroundButton.displayString = Locals.localize("helper.creator.layoutoptions.renderorder.foreground");
				this.renderorder = "background";
			});
			this.renderorderPopup.addContent(renderorderBackgroundButton);
			LayoutCreatorScreen.colorizeCreatorButton(renderorderBackgroundButton);
			
			this.renderorderForegroundButton = new AdvancedButton(0, 0, 0, 16, "§a" + Locals.localize("helper.creator.layoutoptions.renderorder.foreground"), true, (press) -> {
				press.displayString = "§a" + Locals.localize("helper.creator.layoutoptions.renderorder.foreground");
				this.renderorderBackgroundButton.displayString = Locals.localize("helper.creator.layoutoptions.renderorder.background");
				this.renderorder = "foreground";
			});
			this.renderorderPopup.addContent(renderorderForegroundButton);
			LayoutCreatorScreen.colorizeCreatorButton(renderorderForegroundButton);
		}
		
		if (this.mcversionPopup == null) {
			this.mcversionPopup = new PopupMenu(100, 16, -1);
			this.addMenu(mcversionPopup);
			
			AdvancedButton m1 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.version.minimum"), true, (press) -> {
				this.setMenusUseable(false);
				TextInputPopup p = new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.layoutoptions.version.minimum.mc"), null, 240, (call) -> {
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
			
			AdvancedButton m2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.version.maximum"), true, (press) -> {
				this.setMenusUseable(false);
				TextInputPopup p = new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.layoutoptions.version.maximum.mc"), null, 240, (call) -> {
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
			this.fmversionPopup = new PopupMenu(100, 16, -1);
			this.addMenu(fmversionPopup);
			
			AdvancedButton m1 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.version.minimum"), true, (press) -> {
				this.setMenusUseable(false);
				TextInputPopup p = new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.layoutoptions.version.minimum.fm"), null, 240, (call) -> {
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
			
			AdvancedButton m2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.version.maximum"), true, (press) -> {
				this.setMenusUseable(false);
				TextInputPopup p = new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.layoutoptions.version.maximum.fm"), null, 240, (call) -> {
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
			this.backgroundRightclickMenu = new PopupMenu(110, 16, -1);

			this.backgroundRightclickMenu.addChild(this.renderorderPopup);
			this.backgroundRightclickMenu.addChild(this.mcversionPopup);
			this.backgroundRightclickMenu.addChild(this.fmversionPopup);
			
			AdvancedButton backOptionsB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.backgroundoptions"), true, (press) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new BackgroundOptionsPopup(this));
			});
			this.backgroundRightclickMenu.addContent(backOptionsB);
			LayoutCreatorScreen.colorizeCreatorButton(backOptionsB);
			
			AdvancedButton resetBackB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.resetbackground"), true, (press) -> {
				if (this.backgroundAnimation != null) {
					((AdvancedAnimation)this.backgroundAnimation).stopAudio();
				}
				this.backgroundAnimation = null;
				this.backgroundTexture = null;
				this.backgroundRightclickMenu.closeMenu();
			});
			this.backgroundRightclickMenu.addContent(resetBackB);
			LayoutCreatorScreen.colorizeCreatorButton(resetBackB);
			
			this.backgroundRightclickMenu.addChild(this.renderorderPopup);
			AdvancedButton b2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.renderorder"), true, (press) -> {
				this.fmversionPopup.closeMenu();
				this.mcversionPopup.closeMenu();
				this.renderorderPopup.openMenuAt(0, press.y);
			});
			this.backgroundRightclickMenu.addContent(b2);
			LayoutCreatorScreen.colorizeCreatorButton(b2);
			
			AdvancedButton b3 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.requiredmods"), true, (press) -> {
				this.setMenusUseable(false);
				TextInputPopup p = new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.layoutoptions.requiredmods.desc"), null, 240, (call) -> {
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
			
			AdvancedButton b4 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.version.mc"), true, (press) -> {
				this.renderorderPopup.closeMenu();
				this.fmversionPopup.closeMenu();
				this.mcversionPopup.openMenuAt(press.x + press.width + 2, press.y);
			});
			this.backgroundRightclickMenu.addContent(b4);
			LayoutCreatorScreen.colorizeCreatorButton(b4);
			
			AdvancedButton b6 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.version.fm"), true, (press) -> {
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
	
	protected void disableLayouts() {
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
			if (sec.getEntryValue("identifier").equals(this.getScreenToCustomizeIdentifier())) {
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
		meta.addEntry("identifier", this.getScreenToCustomizeIdentifier());
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
		
		if (!this.backgroundAnimationNames.isEmpty()) {
			String names = this.backgroundAnimationNames.get(0);
			if (this.backgroundAnimationNames.size() > 1) {
				int i = 0;
				for (String s : this.backgroundAnimationNames) {
					if (i > 0) {
						names += ", " + s;
					}
					i++;
				}
			}
			PropertiesSection ps = new PropertiesSection("customization");
			ps.addEntry("action", "animatebackground");
			ps.addEntry("name", names);
			if (this.randomBackgroundAnimation) {
				ps.addEntry("random", "true");
			}
			l.add(ps);
		}
		
		if (this.backgroundTexture != null) {
			PropertiesSection ps = new PropertiesSection("customization");
			ps.addEntry("action", "texturizebackground");
			ps.addEntry("path", this.backgroundTexturePath);
			if (this.panorama) {
				ps.addEntry("panorama", "true");
			}
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
	
	protected void saveToCustomizationFile(String fileName) throws IOException {
		List<PropertiesSection> l = this.getAllProperties();
		PropertiesSet props = new PropertiesSet("menu");
		for (PropertiesSection s : l) {
			props.addProperties(s);
		}
		
		PropertiesSerializer.writeProperties(props, FancyMenu.getCustomizationPath().getPath() + "/" + fileName);
	}
	
	protected String generateCustomizationFileName(String dir, String baseName) {
		return FileUtils.generateAvailableFilename(dir, baseName, "txt");
	}
	
	private PopupMenu generateAnimationMenu(Consumer<String> callback) {
		PopupMenu p = new PopupMenu(120, 16, -1);
		
		AdvancedButton inputAniB = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.animation.entername"), true, (press) -> {
			this.setMenusUseable(false);
			PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.add.animation.entername.title") + ":", null, 240, callback));
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
	protected void updateContent() {
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

		//Sync labels, textures, auto-clicks and other stuff made to vanilla buttons
		for (ButtonData b : ButtonCache.getButtons()) {
			if (!this.containsVanillaButton(l, b)) {
				LayoutVanillaButton v = new LayoutVanillaButton(b, this);
				if (this.vanillaButtonNames.containsKey(b.getId())) {
					v.object.value = this.vanillaButtonNames.get(b.getId()); 
				}
				if (this.vanillaButtonClicks.containsKey(b.getId())) {
					v.clicks = this.vanillaButtonClicks.get(b.getId()); 
				}
				if (this.vanillaHideFor.containsKey(b.getId())) {
					v.hideforsec = this.vanillaHideFor.get(b.getId()); 
				}
				if (this.vanillaHoverLabels.containsKey(b.getId())) {
					v.hoverLabel = this.vanillaHoverLabels.get(b.getId()); 
				}
				if (this.vanillaHoverSounds.containsKey(b.getId())) {
					v.hoverSound = this.vanillaHoverSounds.get(b.getId()); 
				}
				if (this.vanillaButtonTextures.containsKey(b.getId())) {
					List<String> l2 = this.vanillaButtonTextures.get(b.getId());
					v.backNormal = l2.get(0);
					v.backHovered = l2.get(1);
					((LayoutButtonDummyCustomizationItem)v.object).setTexture(TextureHandler.getResource(l2.get(0)).getResourceLocation());
				}
				//---------------
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
	
	public boolean isHidden(LayoutObject b) {
		return this.hidden.contains(b);
	}
	
	private void updateHiddenButtonPopup() {
		if ((this.hiddenPopup != null) && this.menus.contains(this.hiddenPopup)) {
			this.menus.remove(this.hiddenPopup);
		}
		this.hiddenPopup = new PopupMenu(100, 16, -1);
		this.addMenu(this.hiddenPopup);
		
		if (this.hidden.isEmpty()) {
			AdvancedButton bt = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.empty"), true, (press) -> {
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
		this.audioPopup = new PopupMenu(100, 16, -1);
		this.addMenu(this.audioPopup);
		
		if (this.audio.isEmpty()) {
			AdvancedButton bt = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.empty"), true, (press) -> {
			});
			this.audioPopup.addContent(bt);
			LayoutCreatorScreen.colorizeCreatorButton(bt);
		} else {
			for (Map.Entry<String, Boolean> m : this.audio.entrySet()) {
				String label = new File(m.getKey()).getName();
				if (Minecraft.getMinecraft().fontRenderer.getStringWidth(label) > 80) {
					label = Minecraft.getMinecraft().fontRenderer.trimStringToWidth(label, 80) + "..";
				}
				
				PopupMenu actions = new PopupMenu(100, 16, -1);
				
				AdvancedButton a1 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.audio.delete"), true, (press2) -> {
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
					}, "§c§l" + Locals.localize("helper.creator.messages.sure"), "", "", Locals.localize("helper.creator.audio.delete.msg"), "", ""));
				});
				actions.addContent(a1);
				LayoutCreatorScreen.colorizeCreatorButton(a1);
				
				String lab = Locals.localize("helper.creator.audio.enableloop");
				if (m.getValue()) {
					lab = Locals.localize("helper.creator.audio.disableloop");
				}
				AdvancedButton a2 = new AdvancedButton(0, 0, 0, 16, lab, true, (press2) -> {
					if (press2.displayString.equals(Locals.localize("helper.creator.audio.enableloop"))) {
						SoundHandler.setLooped(m.getKey(), true);
						this.audio.put(m.getKey(), true);
						press2.displayString = Locals.localize("helper.creator.audio.disableloop");
					} else {
						SoundHandler.setLooped(m.getKey(), false);
						this.audio.put(m.getKey(), false);
						press2.displayString = Locals.localize("helper.creator.audio.enableloop");
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

	public void setVanillaTexture(LayoutVanillaButton button, String backNormal, String backHover) {
		if ((backNormal != null) && (backHover != null)) {
			List<String> l = new ArrayList<String>();
			l.add(backNormal);
			l.add(backHover);

			this.vanillaButtonTextures.put(button.button.getId(), l);
		} else {
			if (this.vanillaButtonTextures.containsKey(button.button.getId())) {
				this.vanillaButtonTextures.remove(button.button.getId());
			}
		}
	}

	public void setVanillaClicks(LayoutVanillaButton button, int clicks) {
		if (clicks > 0) {
			this.vanillaButtonClicks.put(button.button.getId(), clicks);
		} else {
			if (this.vanillaButtonClicks.containsKey(button.button.getId())) {
				this.vanillaButtonClicks.remove(button.button.getId());
			}
		}
	}
	
	public void setVanillaHideFor(LayoutVanillaButton button, double seconds) {
		this.vanillaHideFor.put(button.button.getId(), seconds);
	}

	public void setVanillaHoverLabel(LayoutVanillaButton button, String label) {
		if (label != null) {
			this.vanillaHoverLabels.put(button.button.getId(), label);
		} else {
			if (this.vanillaHoverLabels.containsKey(button.button.getId())) {
				this.vanillaHoverLabels.remove(button.button.getId());
			}
		}
	}

	public void setVanillaHoverSound(LayoutVanillaButton button, String sound) {
		if (sound != null) {
			this.vanillaHoverSounds.put(button.button.getId(), sound);
		} else {
			if (this.vanillaHoverSounds.containsKey(button.button.getId())) {
				this.vanillaHoverSounds.remove(button.button.getId());
			}
		}
	}
	
	public void hideVanillaButton(LayoutVanillaButton b) {
		if (!this.hidden.contains(b) && this.content.contains(b)) {
			this.hidden.add(b);
			b.hidden = true;
			this.setObjectFocused(b, false, true);
			b.resetObjectStates();
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
	
	public List<LayoutObject> getContent() {
		return this.content;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		
		//Handle object focus and update the top hovered object
		if (!MouseInput.isVanillaInputBlocked()) {
			if ((this.focused != null) && !this.focused.isHovered() && !this.focused.isDragged() && !this.focused.isGrabberPressed() && !this.focused.isGettingResized() && (MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown())) {
				this.setObjectFocused(this.focused, false, false);
			}
			LayoutObject ob = null;
			LayoutObject top = null;
			for (LayoutObject o : this.content) {
				if (o.isHovered()) {
					top = o;
					if (MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown()) {
						ob = o;
					}
				}
			}
			if (!this.isObjectFocused() && (ob != null)) {
				this.setObjectFocused(ob, true, false);
			}
			this.topObject = top;
		} else {
			this.focused = null;
		}
		
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
		}
		
		if (PopupHandler.isPopupActive() || this.leftDownAndFocused) {
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
		
		if (this.isObjectFocused() && MouseInput.isLeftMouseDown()) {
			this.leftDownAndFocused = true;
		} else {
			this.leftDownAndFocused = false;
		}
		
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
		
		if (this.leftDownAndFocused) {
			return;
		}
		
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
					this.hiddenButton.displayString = "§4" + Locals.localize("helper.creator.menu.hidden");
					this.expandColor = Color.RED;
				}
				if ((this.hiddenIndicatorCount == 1) || (this.hiddenIndicatorCount == 3) || (this.hiddenIndicatorCount == 5)) {
					this.hiddenButton.displayString = Locals.localize("helper.creator.menu.hidden");
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
		
		if (this.backgroundTexture != null) {
			Minecraft.getMinecraft().getTextureManager().bindTexture(this.backgroundTexture.getResourceLocation());
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			
			if (!this.panorama) {
				Gui.drawModalRectWithCustomSizedTexture(0, 0, 1.0F, 1.0F, this.width, this.height, this.width, this.height);
			} else {
				int w = this.backgroundTexture.getWidth();
				int h = this.backgroundTexture.getHeight();
				double ratio = (double) w / (double) h;
				int wfinal = (int)(this.height * ratio);

				//Check if the panorama background should move to the left side or to the right side
				if ((panoPos + (wfinal - this.width)) <= 0) {
					panoMoveBack = true;
				}
				if (panoPos >= 0) {
					panoMoveBack = false;
				}

				//Fix pos after resizing
				if (panoPos + (wfinal - this.width) < 0) {
					panoPos = 0 - (wfinal - this.width);
				}
				if (panoPos > 0) {
					panoPos = 0;
				}
				
				if (!panoStop) {
					panoTick = 0;
					if (panoMoveBack) {
						panoPos = panoPos + 0.5;
					} else {
						panoPos = panoPos - 0.5;
					}
					
					if (panoPos + (wfinal - this.width) == 0) {
						panoStop = true;
					}
					if (panoPos == 0) {
						panoStop = true;
					}
				} else {
					if (panoTick >= 150) {
						panoStop = false;
						panoTick = 0;
					} else {
						panoTick++;
					}
				}
				if (wfinal <= this.width) {
					Gui.drawModalRectWithCustomSizedTexture(0, 0, 1.0F, 1.0F, this.width, this.height, this.width, this.height);
				} else {
					RenderUtils.doubleBlit(panoPos, 0, 1.0F, 1.0F, wfinal, this.height);
				}
			}
		}
		GlStateManager.disableBlend();
		
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
	
	public void setObjectFocused(LayoutObject object, boolean b, boolean ignoreBlockedFocusChange) {
		if (this.isFocusChangeBlocked() && !ignoreBlockedFocusChange) {
			return;
		}
		if (!this.content.contains(object)) {
			return;
		}
		if (b) {
			//TODO irgendwie n bisschen sinnlos oder wenigstens teilweise sinnlos.
			if (this.backgroundRightclickMenu.isHovered() || this.renderorderPopup.isHovered()) {
				return;
			}
			this.focused = object;
			//---------
		} else {
			if ((this.focused != null) && (this.focused == object)) {
				this.focused = null;
			}
		}
	}
	
	public LayoutObject getFocusedObject() {
		return this.focused;
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
		File home = new File("");
		if (path == null) {
			this.setMenusUseable(true);
			return;
		}
		if (path.startsWith(home.getAbsolutePath())) {
			path = path.replace(home.getAbsolutePath(), "");
			if (path.startsWith("\\") || path.startsWith("/")) {
				path = path.substring(1);
			}
		}
		File f = new File(path);
		String filename = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
		if (f.exists()) {
			if (filename.equals(f.getName())) {
				PropertiesSection sec = new PropertiesSection("customization");
				sec.addEntry("action", "addtexture");
				sec.addEntry("path", path);
				sec.addEntry("height", "100");
				
				TextureCustomizationItem i = new TextureCustomizationItem(sec);
				this.addContent(new LayoutTexture(i, this));
				
				this.setMenusUseable(true);
			} else {
				this.displayNotification(300, Locals.localize("helper.creator.textures.invalidcharacters"), "", "", "", "", "", "");
			}
		} else {
			this.displayNotification(300, "§c§l" + Locals.localize("helper.creator.invalidimage.title"), "", Locals.localize("helper.creator.invalidimage.desc"), "", "", "", "", "", "");
		}
	}
	
	private void addWebTexture(String url) {
		if (url != null) {
			url = WebUtils.filterURL(url);
		}
		if (WebUtils.isValidUrl(url)) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "addwebtexture");
			s.addEntry("url", url);
			s.addEntry("height", "100");
			this.addContent(new LayoutWebTexture(new WebTextureCustomizationItem(s), this));
			this.setMenusUseable(true);
		} else {
			this.displayNotification(300, Locals.localize("helper.creator.web.invalidurl"), "", "", "", "", "", "");
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
			int w = AnimationHandler.getAnimation(name).getWidth();
			int h = AnimationHandler.getAnimation(name).getHeight();
			double ratio = (double) w / (double) h;
			i.height = 100;
			i.width = (int)(i.height * ratio);
			AnimationHandler.getAnimation(name).resetAnimation();
			this.addContent(new LayoutAnimation(i, this));
			
			this.setMenusUseable(true);
		} else {
			this.displayNotification(300, "§c§l" + Locals.localize("helper.creator.animationnotfound.title"), "", Locals.localize("helper.creator.animationnotfound.desc"), "", "", "");
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
	
	private void addWebText(String url) {
		if (url != null) {
			url = WebUtils.filterURL(url);
		}
		if (WebUtils.isValidUrl(url)) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "addwebtext");
			s.addEntry("url", url);
			this.addContent(new LayoutWebString(new WebStringCustomizationItem(s), this));
			this.setMenusUseable(true);
		} else {
			this.displayNotification(300, Locals.localize("helper.creator.web.invalidurl"), "", "", "", "", "", "");
		}
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
			this.displayNotification(300, "§c§l" + Locals.localize("helper.creator.texttooshort.title"), "", Locals.localize("helper.creator.texttooshort.desc"), "", "", "", "");
		}
	}
	
	protected void addAudio(String path) {
		if (path != null) {
			File home = new File("");
			if (path.startsWith(home.getAbsolutePath())) {
				path = path.replace(home.getAbsolutePath(), "");
				if (path.startsWith("\\") || path.startsWith("/")) {
					path = path.substring(1);
				}
			}
			
			File f = new File(path);
			if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
				if (!this.audio.containsKey(path)) {
					this.setMenusUseable(true);
					MenuCustomization.registerSound(path, path);
					SoundHandler.playSound(path);
					this.audio.put(path, false);
					this.updateAudioPopup();
				} else {
					this.displayNotification(300, "§c§l" + Locals.localize("helper.creator.audioalreadyloaded.title"), "", Locals.localize("helper.creator.audioalreadyloaded.desc"), "", "", "", "", "", "");
				}
				
			} else {
				this.displayNotification(300, "§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "", "");
			}
		} else {
			this.setMenusUseable(true);
		}
	}
	
	public void setBackgroundAnimations(String... names) {
		if ((names != null) && (names.length > 0)) {
			for (String s : names) {
				if (AnimationHandler.animationExists(s) && !this.backgroundAnimationNames.contains(s)) {
					this.backgroundAnimationNames.add(s);
				}
			}
			if (!this.backgroundAnimationNames.isEmpty()) {
				this.backgroundTexture = null;
				if (this.backgroundAnimation != null) {
					((AdvancedAnimation)this.backgroundAnimation).stopAudio();
				}
				this.backgroundAnimation = AnimationHandler.getAnimation(this.backgroundAnimationNames.get(0));
				this.backgroundAnimation.resetAnimation();
			}
		}
		if (names == null) {
			if (this.backgroundAnimation != null) {
				((AdvancedAnimation)this.backgroundAnimation).stopAudio();
			}
			this.backgroundAnimation = null;
		}
		this.setMenusUseable(true);
	}
	
	public void setBackgroundTexture(String path) {
		if (path != null) {
			File home = new File("");
			if (path.startsWith(home.getAbsolutePath())) {
				path = path.replace(home.getAbsolutePath(), "");
				if (path.startsWith("\\") || path.startsWith("/")) {
					path = path.substring(1);
				}
			}
			
			File f = new File(path);
			String filename = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
			if (f.exists() && f.isFile() && (f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".jpeg") || f.getName().toLowerCase().endsWith(".png"))) {
				if (filename.equals(f.getName())) {
					this.backgroundTexture = TextureHandler.getResource(path);
					this.backgroundTexturePath = path;
					if (this.backgroundAnimation != null) {
						((AdvancedAnimation)this.backgroundAnimation).stopAudio();
					}
					this.backgroundAnimation = null;
					this.backgroundAnimationNames.clear();
					
					this.setMenusUseable(true);
				} else {
					this.displayNotification(300, Locals.localize("helper.creator.textures.invalidcharacters"), "", "", "", "", "", "");
				}
			} else {
				this.displayNotification(300, "§c§l" + Locals.localize("helper.creator.invalidimage.title"), "", Locals.localize("helper.creator.invalidimage.desc"), "", "", "", "", "", "");
			}
		} else {
			this.setMenusUseable(true);
		}
	}
	
	private void saveCustomizationFileCallback(Integer i) {
		if (i == 2) {
			this.disableLayouts();
		}
		if ((i == 1) || (i == 2)) {
			try {
				String name = this.getScreenToCustomizeIdentifier();
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
					this.displayNotification(300, "§c§l" + Locals.localize("helper.creator.cannotdelete.title"), "", Locals.localize("helper.creator.cannotdelete.desc"), "", "", "");
				}
			}
		}
	}
	
	public GuiScreen getScreenToCustomize() {
		return this.screen;
	}

	public String getScreenToCustomizeIdentifier() {
		if (!(this.screen instanceof CustomGuiBase)) {
			return this.screen.getClass().getName();
		} else {
			return ((CustomGuiBase)this.screen).getIdentifier();
		}
	}

	public void setFocusChangeBlocked(String id, Boolean b) {
		this.focusChangeBlocker.put(id, b);
	}

	public boolean isFocusChangeBlocked() {
		return this.focusChangeBlocker.containsValue(true);
	}
	
	public LayoutObject getTopHoverObject() {
		return this.topObject;
	}

}

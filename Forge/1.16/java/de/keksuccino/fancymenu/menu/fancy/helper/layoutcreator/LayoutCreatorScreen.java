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
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomizationProperties;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.EditHistory.Snapshot;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.WindowSizePopup.ActionType;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.BackgroundOptionsPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.ChooseFilePopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutAnimation;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutObject;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutPlayerEntity;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutShape;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutSlideshow;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutString;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutTexture;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutWebString;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutWebTexture;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.LayoutButton;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.LayoutButtonDummyCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.LayoutVanillaButton;
import de.keksuccino.fancymenu.menu.fancy.item.AnimationCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.PlayerEntityCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.ShapeCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.SlideshowCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.StringCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.TextureCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.WebStringCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.WebTextureCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.ShapeCustomizationItem.Shape;
import de.keksuccino.fancymenu.menu.panorama.ExternalTexturePanoramaRenderer;
import de.keksuccino.fancymenu.menu.slideshow.ExternalTextureSlideshowRenderer;
import de.keksuccino.fancymenu.menu.slideshow.SlideshowHandler;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedImageButton;
import de.keksuccino.konkrete.gui.content.ContextMenu;
import de.keksuccino.konkrete.gui.content.IMenu;
import de.keksuccino.konkrete.gui.content.PopupMenu;
import de.keksuccino.konkrete.gui.screens.popup.NotificationPopup;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.gui.screens.popup.TextInputPopup;
import de.keksuccino.konkrete.gui.screens.popup.YesNoPopup;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSerializer;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.rendering.CurrentScreenHandler;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.konkrete.resources.TextureHandler;
import de.keksuccino.konkrete.sound.SoundHandler;
import de.keksuccino.konkrete.web.WebUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;

public class LayoutCreatorScreen extends Screen {
	
	public static boolean isActive = false;
	
	protected static final ResourceLocation EXPAND_INDICATOR = new ResourceLocation("keksuccino", "expand.png");
	protected static final ResourceLocation SHRINK_INDICATOR = new ResourceLocation("keksuccino", "shrink.png");
	protected static final ResourceLocation SETTINGS_ICON = new ResourceLocation("keksuccino", "gear.png");

	public EditHistory history = new EditHistory(this);
		
	protected boolean expandHovered = false;
	protected boolean expanded = false;
	protected boolean expandMouseDown = false;
	protected Color expandColor = Color.WHITE;
	
	protected boolean leftDownAndFocused = false;
	
	public final Screen screen;
	protected List<LayoutObject> content = new ArrayList<LayoutObject>();
	protected List<LayoutObject> newContent;
	protected List<LayoutObject> vanillaButtonContent = new ArrayList<LayoutObject>();
	protected List<LayoutVanillaButton> hidden = new ArrayList<LayoutVanillaButton>();
	protected Map<String, Boolean> audio = new HashMap<String, Boolean>();
	public Map<Long, String> vanillaButtonNames = new HashMap<Long, String>();
	public Map<Long, List<String>> vanillaButtonTextures = new HashMap<Long, List<String>>();
	public Map<Long, Integer> vanillaButtonClicks = new HashMap<Long, Integer>();
	public Map<Long, String> vanillaHoverLabels = new HashMap<Long, String>();
	public Map<Long, String> vanillaClickSounds = new HashMap<Long, String>();
	public Map<Long, String> vanillaHoverSounds = new HashMap<Long, String>();
	public Map<Long, Double> vanillaHideFor = new HashMap<Long, Double>();
	public Map<Long, Boolean> vanillaDelayOnlyFirstTime = new HashMap<Long, Boolean>();
	public Map<Long, String> vanillaDescriptions = new HashMap<Long, String>();
	protected List<LayoutObject> focusedObjects = new ArrayList<LayoutObject>();
	protected List<LayoutObject> focusedObjectsCache = new ArrayList<LayoutObject>();
	protected int hiddenIndicatorTick = 0;
	protected int hiddenIndicatorCount = 0;
	protected boolean renderHiddenIndicator = false;
	
	protected List<IMenu> menus = new ArrayList<IMenu>();
	
	protected AdvancedButton addObjectButton;
	protected AdvancedButton hiddenButton;
	protected AdvancedButton audioButton;
	protected AdvancedButton closeButton;
	protected AdvancedButton saveButton;
	protected AdvancedImageButton settingsButton;
	
	protected PopupMenu backgroundRightclickMenu;
	protected PopupMenu addAnimationMenu;
	protected PopupMenu addSlideshowMenu;
	protected PopupMenu addShapesMenu;
	protected PopupMenu addObjectPopup;
	protected PopupMenu hiddenPopup;
	protected PopupMenu audioPopup;
	protected List<PopupMenu> audioSubPopups = new ArrayList<PopupMenu>();
	protected PopupMenu renderorderPopup;
	protected PopupMenu windowsizePopup;
	protected PopupMenu mcversionPopup;
	protected PopupMenu fmversionPopup;
	protected PopupMenu openCloseSoundPopup;
	protected PopupMenu multiselectRightclickPopup;
	
	protected AdvancedButton renderorderBackgroundButton;
	protected AdvancedButton renderorderForegroundButton;
	
	protected IAnimationRenderer backgroundAnimation;
	public ExternalTextureResourceLocation backgroundTexture;
	public String backgroundTexturePath;
	public ExternalTexturePanoramaRenderer backgroundPanorama;
	public ExternalTextureSlideshowRenderer backgroundSlideshow;
	public List<String> backgroundAnimationNames = new ArrayList<String>();
	public boolean randomBackgroundAnimation = false;
	public boolean panorama = false;
	private int panoTick = 0;
	private double panoPos = 0.0;
	private boolean panoMoveBack = false;
	private boolean panoStop = false;

	protected String openAudio;
	protected String closeAudio;
	
	protected String renderorder = "foreground";
	protected String requiredmods;
	protected String minimumMC;
	protected String maximumMC;
	protected String minimumFM;
	protected String maximumFM;

	protected int biggerThanWidth = 0;
	protected int biggerThanHeight = 0;
	protected int smallerThanWidth = 0;
	protected int smallerThanHeight = 0;

	protected int scale = 0;

	protected boolean multiselectStretchedX = false;
	protected boolean multiselectStretchedY = false;
	protected List<PopupMenu> multiselectChilds = new ArrayList<PopupMenu>();
	
	private Map<String, Boolean> focusChangeBlocker = new HashMap<String, Boolean>();
	private LayoutObject topObject;
	
	public LayoutCreatorScreen(Screen screenToCustomize) {
		super(new StringTextComponent(""));
		this.screen = screenToCustomize;
		
		KeyboardHandler.addKeyPressedListener(this::updatePositionArrowKeys);
		KeyboardHandler.addKeyPressedListener(this::onDeletePress);
	}

	@Override
	protected void init() {

		if (this.scale > 0) {
			Minecraft.getInstance().getMainWindow().setGuiScale(this.scale);
			this.height = Minecraft.getInstance().getMainWindow().getScaledHeight();
			this.width = Minecraft.getInstance().getMainWindow().getScaledWidth();
		} else {
			Minecraft.getInstance().getMainWindow().setGuiScale(Minecraft.getInstance().getMainWindow().calcGuiScale(Minecraft.getInstance().gameSettings.guiScale, Minecraft.getInstance().getForceUnicodeFont()));
			this.height = Minecraft.getInstance().getMainWindow().getScaledHeight();
			this.width = Minecraft.getInstance().getMainWindow().getScaledWidth();
		}

		this.focusedObjects.clear();
		this.updateContent();
		
		this.focusChangeBlocker.clear();
		
		for (IMenu m : this.menus) {
			m.closeMenu();
		}
		
		this.addObjectButton = new AdvancedButton(17, (this.height / 2) - 104, 40, 40, Locals.localize("helper.creator.menu.add"), true, (onPress) -> {
			if (this.addObjectPopup.isOpen()) {
				this.addObjectPopup.closeMenu();
			} else {
				this.addObjectPopup.openMenuAt(((AdvancedButton)onPress).getX() + ((AdvancedButton)onPress).getWidth() + 2, ((AdvancedButton)onPress).getY());
			}
		});
		LayoutCreatorScreen.colorizeCreatorButton(addObjectButton);
		
		this.hiddenButton = new AdvancedButton(17, (this.height / 2) - 62, 40, 40, Locals.localize("helper.creator.menu.hidden"), true, (onPress) -> {
			if (this.hiddenPopup.isOpen()) {
				this.hiddenPopup.closeMenu();
			} else {
				this.hiddenPopup.openMenuAt(((AdvancedButton)onPress).getX() + ((AdvancedButton)onPress).getWidth() + 2, ((AdvancedButton)onPress).getY());
			}
		});
		LayoutCreatorScreen.colorizeCreatorButton(hiddenButton);
		
		this.audioButton = new AdvancedButton(17, (this.height / 2) - 20, 40, 40, Locals.localize("helper.creator.menu.audio"), true, (onPress) -> {
			if (this.audioPopup.isOpen()) {
				this.audioPopup.closeMenu();
			} else {
				this.audioPopup.openMenuAt(((AdvancedButton)onPress).getX() + ((AdvancedButton)onPress).getWidth() + 2, ((AdvancedButton)onPress).getY());
			}
		});
		LayoutCreatorScreen.colorizeCreatorButton(audioButton);
		
		this.saveButton = new AdvancedButton(17, (this.height / 2) + 22, 40, 40, Locals.localize("helper.creator.menu.save"), true, (onPress) -> {
			this.setMenusUseable(false);
			PopupHandler.displayPopup(new LayoutSavePopup(this::saveCustomizationFileCallback));
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
					
					Minecraft.getInstance().getMainWindow().setGuiScale(Minecraft.getInstance().getMainWindow().calcGuiScale(Minecraft.getInstance().gameSettings.guiScale, Minecraft.getInstance().getForceUnicodeFont()));
					this.height = Minecraft.getInstance().getMainWindow().getScaledHeight();
					this.width = Minecraft.getInstance().getMainWindow().getScaledWidth();
					
					Minecraft.getInstance().displayGuiScreen(this.screen);
				}
			}, "§c§l" + Locals.localize("helper.creator.messages.sure"), "", Locals.localize("helper.creator.close"), "", "", ""));
		});
		LayoutCreatorScreen.colorizeCreatorButton(closeButton);

		this.settingsButton = new AdvancedImageButton(17, (this.height / 2) + 106, 40, 20, SETTINGS_ICON, true, (press) -> {
			this.setMenusUseable(false);
			PopupHandler.displayPopup(new LayoutCreatorSettingsPopup(this));
		});
		this.settingsButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.sidemenu.settings.btndesc"), "%n%"));
		LayoutCreatorScreen.colorizeCreatorButton(settingsButton);
		
		if (this.addAnimationMenu == null) {
			this.addAnimationMenu = this.generateAnimationMenu(this::addAnimation);
		}

		//Add -> 
		if (this.addObjectPopup == null) {
			this.addObjectPopup = new PopupMenu(130, 16, -1);
			this.addMenu(this.addObjectPopup);
			
			AdvancedButton b1 = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.image"), (press) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new ChooseFilePopup(this::addTexture, "jpg", "jpeg", "png", "gif"));
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
					this.addAnimationMenu.setParentButton((AdvancedButton)press);
					this.addAnimationMenu.setAutoclose(true);
					this.addAnimationMenu.openMenuAt(((AdvancedButton)press).getX() + ((AdvancedButton)press).getWidth() + 2, ((AdvancedButton)press).getY());
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

			AdvancedButton b8 = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.playerentity"), (press) -> {
				PropertiesSection s = new PropertiesSection("customization");
				s.addEntry("action", "addentity");
				LayoutPlayerEntity e = new LayoutPlayerEntity(new PlayerEntityCustomizationItem(s), this);
				e.setX(e.getWidth());
				e.setY(e.getHeight());
				this.addContent(e);
			});
			LayoutCreatorScreen.colorizeCreatorButton(b8);
			this.addObjectPopup.addContent(b8);

			AdvancedButton b9 = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.slideshow"), (press) -> {
				if (this.addSlideshowMenu.isOpen()) {
					this.addSlideshowMenu.closeMenu();
				} else {
					this.addSlideshowMenu.openMenuAt(((AdvancedButton)press).getX() + ((AdvancedButton)press).getWidth() + 2, ((AdvancedButton)press).getY());
				}
			});
			LayoutCreatorScreen.colorizeCreatorButton(b9);
			this.addObjectPopup.addContent(b9);

			this.addSlideshowMenu = new PopupMenu(130, 16, -1);
			this.addSlideshowMenu.setParentButton(b9);
			this.addSlideshowMenu.setAutoclose(true);
			this.addMenu(this.addSlideshowMenu);

			AdvancedButton inputSlideshowButton = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.slideshow.entername"), true, (press) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.add.slideshow.entername.title") + ":", null, 240, this::addSlideshow));
			});
			this.addSlideshowMenu.addContent(inputSlideshowButton);
			LayoutCreatorScreen.colorizeCreatorButton(inputSlideshowButton);
			for (String s : SlideshowHandler.getSlideshowNames()) {
				String name = s;
				if (Minecraft.getInstance().fontRenderer.getStringWidth(name) > 90) {
					name = Minecraft.getInstance().fontRenderer.func_238412_a_(name, 90) + "..";
				}
				
				AdvancedButton slideshowB = new AdvancedButton(0, 0, 0, 20, name, true, (press) -> {
					if (SlideshowHandler.slideshowExists(s)) {
						this.addSlideshow(s);
					}
				});
				this.addSlideshowMenu.addContent(slideshowB);
				LayoutCreatorScreen.colorizeCreatorButton(slideshowB);
			}

			AdvancedButton b10 = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.shapes"), (press) -> {
				if (this.addShapesMenu.isOpen()) {
					this.addShapesMenu.closeMenu();
				} else {
					this.addShapesMenu.openMenuAt(((AdvancedButton)press).getX() + ((AdvancedButton)press).getWidth() + 2, ((AdvancedButton)press).getY());
				}
			});
			LayoutCreatorScreen.colorizeCreatorButton(b10);
			this.addObjectPopup.addContent(b10);

			this.addShapesMenu = new PopupMenu(130, 16, -1);
			this.addShapesMenu.setParentButton(b10);
			this.addShapesMenu.setAutoclose(true);
			this.addMenu(this.addShapesMenu);

			AdvancedButton addRectangleButton = new AdvancedButton(0, 0, 0, 20, Locals.localize("helper.creator.add.shapes.rectangle"), (press) -> {
				PropertiesSection s = new PropertiesSection("customization");
				s.addEntry("action", "addshape");
				s.addEntry("shape", Shape.RECTANGLE.name);
				s.addEntry("width", "100");
				s.addEntry("height", "100");
				this.addContent(new LayoutShape(new ShapeCustomizationItem(s), this));
				
				this.addShapesMenu.closeMenu();
			});
			LayoutCreatorScreen.colorizeCreatorButton(addRectangleButton);
			this.addShapesMenu.addContent(addRectangleButton);
			
		}
		
		this.updateHiddenButtonPopup();
		
		this.updateAudioPopup();
		
		if (PopupHandler.isPopupActive()) {
			this.setMenusUseable(false);
		}
		
		//BackgroundRightClick -> Renderorder ->
		if (this.renderorderPopup == null) {
			this.renderorderPopup = new PopupMenu(100, 16, -1);
			this.addMenu(renderorderPopup);

			this.renderorderBackgroundButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.renderorder.background"), true, (press) -> {
				((AdvancedButton)press).setMessage("§a" + Locals.localize("helper.creator.layoutoptions.renderorder.background"));
				this.renderorderForegroundButton.setMessage(Locals.localize("helper.creator.layoutoptions.renderorder.foreground"));
				if (!this.renderorder.equals("background")) {
					this.history.saveSnapshot(this.history.createSnapshot());
				}
				
				this.renderorder = "background";
			});
			this.renderorderPopup.addContent(renderorderBackgroundButton);
			LayoutCreatorScreen.colorizeCreatorButton(renderorderBackgroundButton);
			
			this.renderorderForegroundButton = new AdvancedButton(0, 0, 0, 16, "§a" + Locals.localize("helper.creator.layoutoptions.renderorder.foreground"), true, (press) -> {
				((AdvancedButton)press).setMessage("§a" + Locals.localize("helper.creator.layoutoptions.renderorder.foreground"));
				this.renderorderBackgroundButton.setMessage(Locals.localize("helper.creator.layoutoptions.renderorder.background"));
				if (!this.renderorder.equals("foreground")) {
					this.history.saveSnapshot(this.history.createSnapshot());
				}
				
				this.renderorder = "foreground";
			});
			this.renderorderPopup.addContent(renderorderForegroundButton);
			LayoutCreatorScreen.colorizeCreatorButton(renderorderForegroundButton);
		}

		//BackgroundRightClick -> Windowsize Restricts ->
		if (this.windowsizePopup == null) {
			this.windowsizePopup = new PopupMenu(100, 16, -1);
			this.addMenu(this.windowsizePopup);

			AdvancedButton wb1 = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.windowsize.biggerthan"), true, (press) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new WindowSizePopup(this, ActionType.BIGGERTHAN));
			});
			this.windowsizePopup.addContent(wb1);
			LayoutCreatorScreen.colorizeCreatorButton(wb1);
			
			AdvancedButton wb2 = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.windowsize.smallerthan"), true, (press) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new WindowSizePopup(this, ActionType.SMALLERTHAN));
			});
			this.windowsizePopup.addContent(wb2);
			LayoutCreatorScreen.colorizeCreatorButton(wb2);
		}
		
		//BackgroundRightClick -> MC Version ->
		if (this.mcversionPopup == null) {
			this.mcversionPopup = new PopupMenu(100, 16, -1);
			this.addMenu(mcversionPopup);
			
			AdvancedButton m1 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.version.minimum"), true, (press) -> {
				this.setMenusUseable(false);
				TextInputPopup p = new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.layoutoptions.version.minimum.mc"), null, 240, (call) -> {
					if (call != null) {
						if (this.minimumMC != call) {
							this.history.saveSnapshot(this.history.createSnapshot());
						}
						
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
						if (this.maximumMC != call) {
							this.history.saveSnapshot(this.history.createSnapshot());
						}
						
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
		
		//BackgroundRightClick -> FM Version ->
		if (this.fmversionPopup == null) {
			this.fmversionPopup = new PopupMenu(100, 16, -1);
			this.addMenu(fmversionPopup);
			
			AdvancedButton m1 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.version.minimum"), true, (press) -> {
				this.setMenusUseable(false);
				TextInputPopup p = new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.layoutoptions.version.minimum.fm"), null, 240, (call) -> {
					if (call != null) {
						if (this.minimumFM != call) {
							this.history.saveSnapshot(this.history.createSnapshot());
						}
						
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
						if (this.maximumFM != call) {
							this.history.saveSnapshot(this.history.createSnapshot());
						}
						
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

		if (this.openCloseSoundPopup == null) {
			this.openCloseSoundPopup = new PopupMenu(100, 16, -1);
			this.addMenu(openCloseSoundPopup);
			
			AdvancedButton openSoundBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.openaudio"), true, (press) -> {
				this.setMenusUseable(false);
				ChooseFilePopup p = new ChooseFilePopup((call) -> {
					if (call != null) {
						if (call.length() < 3) {
							if (this.openAudio != null) {
								this.history.saveSnapshot(this.history.createSnapshot());
							}
							this.openAudio = null;
							this.setMenusUseable(true);
						} else {
							File f = new File(call);
							if (f.exists() && f.isFile() && f.getName().toLowerCase().endsWith(".wav")) {
								if (this.openAudio != call) {
									this.history.saveSnapshot(this.history.createSnapshot());
								}
								this.openAudio = call;
								this.setMenusUseable(true);
							} else {
								this.displayNotification(300, "§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "");
							}
						}
					} else {
						if (this.openAudio != null) {
							this.history.saveSnapshot(this.history.createSnapshot());
						}
						this.openAudio = null;
						this.setMenusUseable(true);
					}
				}, "wav");
				if (this.openAudio != null) {
					p.setText(this.openAudio);
				}
				PopupHandler.displayPopup(p);
			});
			openSoundBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.openaudio.desc"), "%n%"));
			this.openCloseSoundPopup.addContent(openSoundBtn);
			LayoutCreatorScreen.colorizeCreatorButton(openSoundBtn);
			
			AdvancedButton resetOpenBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.openaudio.reset"), true, (press) -> {
				if (this.openAudio != null) {
					this.history.saveSnapshot(this.history.createSnapshot());
				}
				this.openAudio = null;
			});
			resetOpenBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.opencloseaudio.reset.desc"), "%n%"));
			this.openCloseSoundPopup.addContent(resetOpenBtn);
			LayoutCreatorScreen.colorizeCreatorButton(resetOpenBtn);
			
			AdvancedButton closeSoundBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.closeaudio"), true, (press) -> {
				this.setMenusUseable(false);
				ChooseFilePopup p = new ChooseFilePopup((call) -> {
					if (call != null) {
						if (call.length() < 3) {
							if (this.closeAudio != null) {
								this.history.saveSnapshot(this.history.createSnapshot());
							}
							this.closeAudio = null;
							this.setMenusUseable(true);
						} else {
							File f = new File(call);
							if (f.exists() && f.isFile() && f.getName().toLowerCase().endsWith(".wav")) {
								if (this.closeAudio != call) {
									this.history.saveSnapshot(this.history.createSnapshot());
								}
								this.closeAudio = call;
								this.setMenusUseable(true);
							} else {
								this.displayNotification(300, "§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "");
							}
						}
					} else {
						if (this.closeAudio != null) {
							this.history.saveSnapshot(this.history.createSnapshot());
						}
						this.closeAudio = null;
						this.setMenusUseable(true);
					}
				}, "wav");
				if (this.closeAudio != null) {
					p.setText(this.closeAudio);
				}
				PopupHandler.displayPopup(p);
			});
			closeSoundBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.closeaudio.desc"), "%n%"));
			this.openCloseSoundPopup.addContent(closeSoundBtn);
			LayoutCreatorScreen.colorizeCreatorButton(closeSoundBtn);
			
			AdvancedButton resetCloseBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.closeaudio.reset"), true, (press) -> {
				if (this.closeAudio != null) {
					this.history.saveSnapshot(this.history.createSnapshot());
				}
				this.closeAudio = null;
			});
			resetCloseBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.opencloseaudio.reset.desc"), "%n%"));
			this.openCloseSoundPopup.addContent(resetCloseBtn);
			LayoutCreatorScreen.colorizeCreatorButton(resetCloseBtn);
		}
		
		//Background Rightclick Menu
		if (this.backgroundRightclickMenu == null) {
			this.backgroundRightclickMenu = new PopupMenu(110, 16, -1);

			this.backgroundRightclickMenu.addChild(this.renderorderPopup);
			this.backgroundRightclickMenu.addChild(this.mcversionPopup);
			this.backgroundRightclickMenu.addChild(this.fmversionPopup);
			this.backgroundRightclickMenu.addChild(this.windowsizePopup);
			this.backgroundRightclickMenu.addChild(this.openCloseSoundPopup);
			
			AdvancedButton backOptionsB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.backgroundoptions"), true, (press) -> {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new BackgroundOptionsPopup(this));
			});
			this.backgroundRightclickMenu.addContent(backOptionsB);
			LayoutCreatorScreen.colorizeCreatorButton(backOptionsB);
			
			AdvancedButton resetBackB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.resetbackground"), true, (press) -> {
				if ((this.backgroundTexture != null) || (this.backgroundAnimation != null) || (this.backgroundPanorama != null)) {
					this.history.saveSnapshot(this.history.createSnapshot());
				}
				
				if (this.backgroundAnimation != null) {
					((AdvancedAnimation)this.backgroundAnimation).stopAudio();
				}

				this.backgroundAnimationNames = new ArrayList<String>();
				this.backgroundPanorama = null;
				this.backgroundSlideshow = null;
				this.backgroundAnimation = null;
				this.backgroundTexture = null;
				this.backgroundRightclickMenu.closeMenu();
			});
			this.backgroundRightclickMenu.addContent(resetBackB);
			LayoutCreatorScreen.colorizeCreatorButton(resetBackB);

			AdvancedButton b2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.renderorder"), true, (press) -> {
				this.fmversionPopup.closeMenu();
				this.mcversionPopup.closeMenu();
				this.windowsizePopup.closeMenu();
				this.renderorderPopup.openMenuAt(0, ((AdvancedButton)press).getY());
			});
			this.backgroundRightclickMenu.addContent(b2);
			LayoutCreatorScreen.colorizeCreatorButton(b2);

			AdvancedButton b7 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.windowsize"), true, (press) -> {
				this.fmversionPopup.closeMenu();
				this.mcversionPopup.closeMenu();
				this.renderorderPopup.closeMenu();
				this.windowsizePopup.openMenuAt(0, ((AdvancedButton)press).getY());
			});
			this.backgroundRightclickMenu.addContent(b7);
			LayoutCreatorScreen.colorizeCreatorButton(b7);

			AdvancedButton b8 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.rightclick.scale"), true, (press) -> {
				TextInputPopup p = new TextInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.creator.rightclick.scale"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
					if ((call != null) && MathUtils.isInteger(call)) {
						int s = Integer.parseInt(call);
						if (s < 0) {
							this.displayNotification(300, Locals.localize("helper.creator.rightclick.scale.invalid"), "", "", "", "");
						} else {
							this.setMenusUseable(true);
							
							if (this.scale != s) {
								this.history.saveSnapshot(this.history.createSnapshot());
							}
							
							this.scale = s;
							this.init(Minecraft.getInstance(), Minecraft.getInstance().getMainWindow().getScaledWidth(), Minecraft.getInstance().getMainWindow().getScaledHeight());
						}
					} else {
						this.setMenusUseable(true);
					}
				});
				p.setText("" + this.scale);
				this.setMenusUseable(false);
				PopupHandler.displayPopup(p);
			});
			this.backgroundRightclickMenu.addContent(b8);
			LayoutCreatorScreen.colorizeCreatorButton(b8);
			
			AdvancedButton b3 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.requiredmods"), true, (press) -> {
				this.setMenusUseable(false);
				TextInputPopup p = new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.layoutoptions.requiredmods.desc"), null, 240, (call) -> {
					if (call != null) {
						if (this.requiredmods != call) {
							this.history.saveSnapshot(this.history.createSnapshot());
						}
						
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
				this.windowsizePopup.closeMenu();
				this.openCloseSoundPopup.closeMenu();
				this.mcversionPopup.openMenuAt(((AdvancedButton)press).getX() + ((AdvancedButton)press).getWidth() + 2, ((AdvancedButton)press).getY());
			});
			this.backgroundRightclickMenu.addContent(b4);
			LayoutCreatorScreen.colorizeCreatorButton(b4);
			
			AdvancedButton b6 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.layoutoptions.version.fm"), true, (press) -> {
				this.renderorderPopup.closeMenu();
				this.mcversionPopup.closeMenu();
				this.windowsizePopup.closeMenu();
				this.openCloseSoundPopup.closeMenu();
				this.fmversionPopup.openMenuAt(((AdvancedButton)press).getX() + ((AdvancedButton)press).getWidth() + 2, ((AdvancedButton)press).getY());
			});
			this.backgroundRightclickMenu.addContent(b6);
			LayoutCreatorScreen.colorizeCreatorButton(b6);

			AdvancedButton btnOpenCloseSound = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.opencloseaudio"), true, (press) -> {
				this.renderorderPopup.closeMenu();
				this.mcversionPopup.closeMenu();
				this.fmversionPopup.closeMenu();
				this.windowsizePopup.closeMenu();
				this.openCloseSoundPopup.openMenuAt(((AdvancedButton)press).getX() + ((AdvancedButton)press).getWidth() + 2, ((AdvancedButton)press).getY());
			});
			btnOpenCloseSound.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.opencloseaudio.desc"), "%n%"));
			this.backgroundRightclickMenu.addContent(btnOpenCloseSound);
			LayoutCreatorScreen.colorizeCreatorButton(btnOpenCloseSound);
			
			this.addMenu(this.backgroundRightclickMenu);
		}
		if (this.backgroundRightclickMenu != null) {
			this.backgroundRightclickMenu.menuScale = getPopupMenuScale();
		}
	}
	
	//shouldCloseOnEsc
	@Override
	public boolean shouldCloseOnEsc() {
		return false;
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

	protected List<PropertiesSection> getAllProperties() {
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
		if (this.biggerThanWidth != 0) {
			meta.addEntry("biggerthanwidth", "" + this.biggerThanWidth);
		}
		if (this.biggerThanHeight != 0) {
			meta.addEntry("biggerthanheight", "" + this.biggerThanHeight);
		}
		if (this.smallerThanWidth != 0) {
			meta.addEntry("smallerthanwidth", "" + this.smallerThanWidth);
		}
		if (this.smallerThanHeight != 0) {
			meta.addEntry("smallerthanheight", "" + this.smallerThanHeight);
		}
		//----------------
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

		if (this.backgroundPanorama != null) {
			PropertiesSection ps = new PropertiesSection("customization");
			ps.addEntry("action", "setbackgroundpanorama");
			ps.addEntry("name", this.backgroundPanorama.getName());
			l.add(ps);
		}

		if (this.backgroundSlideshow != null) {
			PropertiesSection ps = new PropertiesSection("customization");
			ps.addEntry("action", "setbackgroundslideshow");
			ps.addEntry("name", this.backgroundSlideshow.getName());
			l.add(ps);
		}
		
		if (this.backgroundTexture != null) {
			PropertiesSection ps = new PropertiesSection("customization");
			ps.addEntry("action", "texturizebackground");
			ps.addEntry("path", this.backgroundTexturePath);
			if (this.panorama) {
				ps.addEntry("wideformat", "true");
			}
			l.add(ps);
		}

		if (this.scale > 0) {
			PropertiesSection ps = new PropertiesSection("customization");
			ps.addEntry("action", "setscale");
			ps.addEntry("scale", "" + this.scale);
			l.add(ps);
		}

		if (this.openAudio != null) {
			PropertiesSection ps = new PropertiesSection("customization");
			ps.addEntry("action", "setopenaudio");
			ps.addEntry("path", this.openAudio);
			l.add(ps);
		}

		if (this.closeAudio != null) {
			PropertiesSection ps = new PropertiesSection("customization");
			ps.addEntry("action", "setcloseaudio");
			ps.addEntry("path", this.closeAudio);
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
			if (Minecraft.getInstance().fontRenderer.getStringWidth(name) > 90) {
				name = Minecraft.getInstance().fontRenderer.func_238412_a_(name, 90) + "..";
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
					if (this.vanillaDelayOnlyFirstTime.containsKey(b.getId())) {
						v.delayonlyfirsttime = this.vanillaDelayOnlyFirstTime.get(b.getId());
					}
				}
				if (this.vanillaHoverLabels.containsKey(b.getId())) {
					v.hoverLabel = this.vanillaHoverLabels.get(b.getId()); 
				}
				if (this.vanillaHoverSounds.containsKey(b.getId())) {
					v.hoverSound = this.vanillaHoverSounds.get(b.getId()); 
				}
				if (this.vanillaClickSounds.containsKey(b.getId())) {
					v.clicksound = this.vanillaClickSounds.get(b.getId()); 
				}
				if (this.vanillaButtonTextures.containsKey(b.getId())) {
					List<String> l2 = this.vanillaButtonTextures.get(b.getId());
					v.backNormal = l2.get(0);
					v.backHovered = l2.get(1);
					((LayoutButtonDummyCustomizationItem)v.object).setTexture(TextureHandler.getResource(l2.get(0)).getResourceLocation());
				}
				if (this.vanillaDescriptions.containsKey(b.getId())) {
					v.description = this.vanillaDescriptions.get(b.getId()); 
				}
				content.add(v);
			}
		}
		this.content.addAll(l);

		this.vanillaButtonContent.clear();
		for (LayoutObject o : this.content) {
			if (o instanceof LayoutVanillaButton) {
				this.vanillaButtonContent.add(o);
			}
		}
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
				if (Minecraft.getInstance().fontRenderer.getStringWidth(label) > 80) {
					label = Minecraft.getInstance().fontRenderer.func_238412_a_(label, 80) + "..";
				}
				AdvancedButton bt = new AdvancedButton(0, 0, 0, 16, label, true, (press) -> {
					this.showVanillaButton(b);
				});
				this.hiddenPopup.addContent(bt);
				LayoutCreatorScreen.colorizeCreatorButton(bt);
			}
		}
	}

	protected void updateMultiselectRightclickPopup() {
		if ((this.multiselectRightclickPopup != null) && this.menus.contains(this.multiselectRightclickPopup)) {
			for (PopupMenu m : this.multiselectChilds) {
				if (this.menus.contains(m)) {
					m.closeMenu();
					this.menus.remove(m);
				}
			}
			this.multiselectChilds.clear();
			this.multiselectRightclickPopup.closeMenu();
			this.menus.remove(this.multiselectRightclickPopup);
		}
		
		if (this.isObjectFocused()) {

			this.focusedObjectsCache = this.getFocusedObjects();
			
			PopupMenu multiRightclickMenu = new PopupMenu(110, 16, -1) {
				
				@Override
				public void openMenuAt(int x, int y) {
					LayoutCreatorScreen.this.multiselectStretchedX = false;
					LayoutCreatorScreen.this.multiselectStretchedY = false;
					LayoutCreatorScreen.this.addMenu(this);
					
					super.openMenuAt(x, y);
				}
				
				@Override
				public void addChild(PopupMenu menu) {
					LayoutCreatorScreen.this.multiselectChilds.add(menu);
					LayoutCreatorScreen.this.addMenu(menu);
					super.addChild(menu);
				}
				
				@Override
				public boolean isHovered() {
					if (!this.isOpen()) {
						return false;
					}
					for (PopupMenu m : LayoutCreatorScreen.this.multiselectChilds) {
						if (m.isHovered()) {
							return true;
						}
					}
					return super.isHovered();
				}
				
			};
			
			multiRightclickMenu.menuScale = getPopupMenuScale();
			
			//Delete --------------------
			AdvancedButton deleteBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.multiselect.object.deleteall"), true, (press) -> {
				this.deleteFocusedObjects();
			});
			deleteBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.object.deleteall.btndesc"), "%n%"));
			colorizeCreatorButton(deleteBtn);
			multiRightclickMenu.addContent(deleteBtn);
			//---------------------------
			
			//Stretch -------------------
			PopupMenu stretchMenu = new PopupMenu(110, 16, -1);
			multiRightclickMenu.addChild(stretchMenu);
			
			AdvancedButton stretchBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.multiselect.object.stretchall"), true, (press) -> {
				LayoutCreatorScreen.this.closeMultiselectChildMenus();
				stretchMenu.openMenuAt(0, press.y);
			});
			stretchBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.object.stretchall.btndesc"), "%n%"));
			colorizeCreatorButton(stretchBtn);
			multiRightclickMenu.addContent(stretchBtn);
			
			AdvancedButton stretchXBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.object.stretch.x"), true, (press) -> {
				this.history.saveSnapshot(this.history.createSnapshot());
				
				for (LayoutObject o : this.focusedObjectsCache) {
					if (o.isStretchable()) {
						o.setStretchedX(!this.multiselectStretchedX, false);
					}
				}
				
				this.multiselectStretchedX = !this.multiselectStretchedX;
				
				if (!this.multiselectStretchedX) {
					press.setMessage(new StringTextComponent(Locals.localize("helper.creator.object.stretch.x")));
				} else {
					press.setMessage(new StringTextComponent("§a" + Locals.localize("helper.creator.object.stretch.x")));
				}

			});
			colorizeCreatorButton(stretchXBtn);
			stretchMenu.addContent(stretchXBtn);
			
			AdvancedButton stretchYBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.object.stretch.y"), true, (press) -> {
				this.history.saveSnapshot(this.history.createSnapshot());
				
				for (LayoutObject o : this.focusedObjectsCache) {
					if (o.isStretchable()) {
						o.setStretchedY(!this.multiselectStretchedY, false);
					}
				}
				
				this.multiselectStretchedY = !this.multiselectStretchedY;
				
				if (!this.multiselectStretchedY) {
					press.setMessage(new StringTextComponent(Locals.localize("helper.creator.object.stretch.y")));
				} else {
					press.setMessage(new StringTextComponent("§a" + Locals.localize("helper.creator.object.stretch.y")));
				}
				
			});
			colorizeCreatorButton(stretchYBtn);
			stretchMenu.addContent(stretchYBtn);
			//---------------------------
			
			boolean allVanillaBtns = false;
			boolean allBtns = false;
			for (LayoutObject o : this.focusedObjectsCache) {
				if (o instanceof LayoutVanillaButton) {
					allVanillaBtns = true;
				} else {
					allVanillaBtns = false;
				}
				if ((o instanceof LayoutVanillaButton) || (o instanceof LayoutButton)) {
					allBtns = true;
				} else {
					allBtns = false;
				}
			}
			
			if (allVanillaBtns) {
				
				//Vanilla Buttons: Reset Orientation
				AdvancedButton resetOriBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.multiselect.vanillabutton.resetorientation"), true, (press) -> {
					this.history.saveSnapshot(this.history.createSnapshot());
					
					for (LayoutObject o : this.focusedObjectsCache) {
						if (o instanceof LayoutVanillaButton) {
							LayoutVanillaButton vb = (LayoutVanillaButton) o;
							vb.object.orientation = "original";
							vb.object.posX = vb.button.x;
							vb.object.posY = vb.button.y;
							vb.object.width = vb.button.width;
							vb.object.height = vb.button.height;
						}
					}
					multiRightclickMenu.closeMenu();
					Minecraft.getInstance().displayGuiScreen(this);
				});
				resetOriBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.vanillabutton.resetorientation.btndesc"), "%n%"));
				colorizeCreatorButton(resetOriBtn);
				multiRightclickMenu.addContent(resetOriBtn);
				//-----------------------

				//Vanilla Buttons: Hide All
				AdvancedButton hideAllBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.multiselect.vanillabutton.hideall"), true, (press) -> {
					this.history.saveSnapshot(this.history.createSnapshot());
					this.history.setPreventSnapshotSaving(true);
					
					for (LayoutObject o : this.focusedObjectsCache) {
						if (o instanceof LayoutVanillaButton) {
							LayoutVanillaButton vb = (LayoutVanillaButton) o;
							this.hideVanillaButton(vb);
						}
					}
					
					this.focusedObjects.clear();
					this.focusedObjectsCache.clear();
					this.multiselectRightclickPopup.closeMenu();
					
					this.history.setPreventSnapshotSaving(false);
				});
				hideAllBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.vanillabutton.hideall.btndesc"), "%n%"));
				colorizeCreatorButton(hideAllBtn);
				multiRightclickMenu.addContent(hideAllBtn);
				//-----------------------
				
			}
			
			if (allBtns) {
				
				PopupMenu textureMenu = new PopupMenu(110, 16, -1);
				multiRightclickMenu.addChild(textureMenu);
				
				//All Buttons: Button Texture
				AdvancedButton buttonTextureBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture"), true, (press) -> {
					LayoutCreatorScreen.this.closeMultiselectChildMenus();
					textureMenu.openMenuAt(0, press.y);
				});
				buttonTextureBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.button.buttontexture.btndesc"), "%n%"));
				colorizeCreatorButton(buttonTextureBtn);
				multiRightclickMenu.addContent(buttonTextureBtn);
				//-----------------------
				
				//All Buttons: Button Texture: Normal
				AdvancedButton normalTextureBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture.normal"), true, (press) -> {
					this.setButtonTexturesForFocusedObjects(false);
				});
				colorizeCreatorButton(normalTextureBtn);
				textureMenu.addContent(normalTextureBtn);
				//-----------------------

				//All Buttons: Button Texture: Hover
				AdvancedButton hoverTextureBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture.hovered"), true, (press) -> {
					this.setButtonTexturesForFocusedObjects(true);
				});
				colorizeCreatorButton(hoverTextureBtn);
				textureMenu.addContent(hoverTextureBtn);
				//-----------------------

				//All Buttons: Button Texture: Reset
				AdvancedButton resetTextureBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture.reset"), true, (press) -> {
					this.history.saveSnapshot(this.history.createSnapshot());
					this.history.setPreventSnapshotSaving(true);
					
					for (LayoutObject o : this.focusedObjectsCache) {
						if (o instanceof LayoutVanillaButton) {
							LayoutVanillaButton vb = (LayoutVanillaButton) o;
							vb.backHovered = null;
							vb.backNormal = null;
							((LayoutButtonDummyCustomizationItem)o.object).setTexture(null);
							this.setVanillaTexture(vb, null, null);
						} else if (o instanceof LayoutButton) {
							LayoutButton lb = (LayoutButton) o;
							lb.backHovered = null;
							lb.backNormal = null;
							((LayoutButtonDummyCustomizationItem)o.object).setTexture(null);
						}
					}
					
					this.history.setPreventSnapshotSaving(false);
				});
				resetTextureBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.button.buttontexture.reset.btndesc"), "%n%"));
				colorizeCreatorButton(resetTextureBtn);
				textureMenu.addContent(resetTextureBtn);
				//-----------------------

				//All Buttons: Click Sound
				AdvancedButton clickSoundBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.clicksound"), true, (press) -> {
					
					this.setMenusUseable(false);
					ChooseFilePopup cf = new ChooseFilePopup((call) -> {
						if (call != null) {
							File f = new File(call);
							if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
								this.history.saveSnapshot(this.history.createSnapshot());
								this.history.setPreventSnapshotSaving(true);
								
								for (LayoutObject o : this.focusedObjectsCache) {
									if (o instanceof LayoutVanillaButton) {
										LayoutVanillaButton vb = (LayoutVanillaButton) o;
										vb.clicksound = call;
										this.setVanillaClickSound(vb, call);
									} else if (o instanceof LayoutButton) {
										LayoutButton lb = (LayoutButton) o;
										lb.clicksound = call;
									}
								}
								
								this.history.setPreventSnapshotSaving(false);
								this.setMenusUseable(true);
							} else {
								
								this.displayNotification(300, "§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "", "");
							}
						} else {
							this.setMenusUseable(true);
						}
					}, "wav");

					PopupHandler.displayPopup(cf);
					
				});
				clickSoundBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.button.clicksound.btndesc"), "%n%"));
				colorizeCreatorButton(clickSoundBtn);
				multiRightclickMenu.addContent(clickSoundBtn);
				//-----------------------

				//All Buttons: Reset Clicksound
				AdvancedButton resetClickSoundBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.clicksound.reset"), true, (press) -> {
					
					this.history.saveSnapshot(this.history.createSnapshot());
					this.history.setPreventSnapshotSaving(true);
					
					for (LayoutObject o : this.focusedObjectsCache) {
						if (o instanceof LayoutVanillaButton) {
							LayoutVanillaButton vb = (LayoutVanillaButton) o;
							vb.clicksound = null;
							this.setVanillaClickSound(vb, null);
						} else if (o instanceof LayoutButton) {
							LayoutButton lb = (LayoutButton) o;
							lb.clicksound = null;
						}
					}
					
					this.history.setPreventSnapshotSaving(false);
					this.setMenusUseable(true);
					
				});
				resetClickSoundBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.button.clicksound.reset.btndesc"), "%n%"));
				colorizeCreatorButton(resetClickSoundBtn);
				multiRightclickMenu.addContent(resetClickSoundBtn);
				//-----------------------
				
				//All Buttons: Hoversound
				AdvancedButton hoverSoundBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoversound"), true, (press) -> {
					
					this.setMenusUseable(false);
					ChooseFilePopup cf = new ChooseFilePopup((call) -> {
						if (call != null) {
							File f = new File(call);
							if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
								this.history.saveSnapshot(this.history.createSnapshot());
								this.history.setPreventSnapshotSaving(true);
								
								for (LayoutObject o : this.focusedObjectsCache) {
									if (o instanceof LayoutVanillaButton) {
										LayoutVanillaButton vb = (LayoutVanillaButton) o;
										vb.hoverSound = call;
										this.setVanillaHoverSound(vb, call);
									} else if (o instanceof LayoutButton) {
										LayoutButton lb = (LayoutButton) o;
										lb.hoverSound = call;
									}
								}
								
								this.history.setPreventSnapshotSaving(false);
								this.setMenusUseable(true);
							} else {
								
								this.displayNotification(300, "§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "", "");
							}
						} else {
							this.setMenusUseable(true);
						}
					}, "wav");

					PopupHandler.displayPopup(cf);
					
				});
				hoverSoundBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.button.hoversound.btndesc"), "%n%"));
				colorizeCreatorButton(hoverSoundBtn);
				multiRightclickMenu.addContent(hoverSoundBtn);
				//-----------------------

				//All Buttons: Reset Hoversound
				AdvancedButton resetHoverSoundBtn = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoversound.reset"), true, (press) -> {
					
					this.history.saveSnapshot(this.history.createSnapshot());
					this.history.setPreventSnapshotSaving(true);
					
					for (LayoutObject o : this.focusedObjectsCache) {
						if (o instanceof LayoutVanillaButton) {
							LayoutVanillaButton vb = (LayoutVanillaButton) o;
							vb.hoverSound = null;
							this.setVanillaHoverSound(vb, null);
						} else if (o instanceof LayoutButton) {
							LayoutButton lb = (LayoutButton) o;
							lb.hoverSound = null;
						}
					}
					
					this.history.setPreventSnapshotSaving(false);
					this.setMenusUseable(true);
					
				});
				resetHoverSoundBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.multiselect.button.hoversound.reset.btndesc"), "%n%"));
				colorizeCreatorButton(resetHoverSoundBtn);
				multiRightclickMenu.addContent(resetHoverSoundBtn);
				//-----------------------
			}
			
			this.multiselectRightclickPopup = multiRightclickMenu;
			
		} else {
			this.multiselectRightclickPopup = null;
		}
	}

	protected void closeMultiselectChildMenus() {
		for (PopupMenu m : this.multiselectChilds) {
			m.closeMenu();
		}
	}

	protected void setButtonTexturesForFocusedObjects(boolean hover) {
		this.setMenusUseable(false);
		ChooseFilePopup cf = new ChooseFilePopup((call) -> {
			if (call != null) {
				File home = new File("");
				call = call.replace("\\", "/");
				File f = new File(call);
				String filename = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
				if (f.exists() && f.isFile() && (f.getName().endsWith(".jpg") || f.getName().endsWith(".jpeg") || f.getName().endsWith(".png"))) {
					if (filename.equals(f.getName())) {
						if (call.startsWith(home.getAbsolutePath())) {
							call = call.replace(home.getAbsolutePath(), "");
							if (call.startsWith("\\") || call.startsWith("/")) {
								call = call.substring(1);
							}
						}

						this.history.saveSnapshot(this.history.createSnapshot());
						this.history.setPreventSnapshotSaving(true);
						
						for (LayoutObject o : this.focusedObjectsCache) {
							if (o instanceof LayoutVanillaButton) {
								
								LayoutVanillaButton vb = (LayoutVanillaButton) o;
								
								if (hover) {
									vb.backHovered = call;
									if (vb.backNormal == null) {
										vb.backNormal = call;
									}
								} else {
									vb.backNormal = call;
									if (vb.backHovered == null) {
										vb.backHovered = call;
									}
								}
								
								this.setVanillaTexture(vb, vb.backNormal, vb.backHovered);
								
								((LayoutButtonDummyCustomizationItem)vb.object).setTexture(TextureHandler.getResource(vb.backNormal).getResourceLocation());
								
							} else if (o instanceof LayoutButton) {
								
								LayoutButton lb = (LayoutButton) o;
								
								if (hover) {
									lb.backHovered = call;
									if (lb.backNormal == null) {
										lb.backNormal = call;
									}
								} else {
									lb.backNormal = call;
									if (lb.backHovered == null) {
										lb.backHovered = call;
									}
								}
								
								((LayoutButtonDummyCustomizationItem)lb.object).setTexture(TextureHandler.getResource(lb.backNormal).getResourceLocation());
								
							}
						}
						
						this.history.setPreventSnapshotSaving(false);
						
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
		}, "jpg", "jpeg", "png");
		
		PopupHandler.displayPopup(cf);
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
				if (Minecraft.getInstance().fontRenderer.getStringWidth(label) > 80) {
					label = Minecraft.getInstance().fontRenderer.func_238412_a_(label, 80) + "..";
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
					if (((AdvancedButton)press2).getMessage().getString().equals(Locals.localize("helper.creator.audio.enableloop"))) {
						SoundHandler.setLooped(m.getKey(), true);
						this.audio.put(m.getKey(), true);
						((AdvancedButton)press2).setMessage(Locals.localize("helper.creator.audio.disableloop"));;
					} else {
						SoundHandler.setLooped(m.getKey(), false);
						this.audio.put(m.getKey(), false);
						((AdvancedButton)press2).setMessage(Locals.localize("helper.creator.audio.enableloop"));;
					}
				});
				actions.addContent(a2);
				LayoutCreatorScreen.colorizeCreatorButton(a2);
				
				AdvancedButton bt = new AdvancedButton(0, 0, 0, 16, label, true, (press) -> {
					actions.openMenuAt(((AdvancedButton)press).getX() + ((AdvancedButton)press).getWidth() + 2, ((AdvancedButton)press).getY());
				});
				this.audioPopup.addContent(bt);
				LayoutCreatorScreen.colorizeCreatorButton(bt);
				
				this.audioSubPopups.add(actions);
			}
		}
	}
	
	public void setVanillaButtonName(LayoutVanillaButton button, String text) {
		if ((this.vanillaButtonNames.get(button.button.getId()) == null) || !(this.vanillaButtonNames.get(button.button.getId()).equals(text))) {
			this.history.saveSnapshot(this.history.createSnapshot());
		}
		
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

	public void setVanillaDescription(LayoutVanillaButton button, String desc) {
		if (desc == null) {
			this.vanillaDescriptions.remove(button.button.getId());
		} else {
			this.vanillaDescriptions.put(button.button.getId(), desc);
		}
	}
	
	public void setVanillaDelayOnlyFirstTime(LayoutVanillaButton button, boolean onlyfirsttime) {
		this.vanillaDelayOnlyFirstTime.put(button.button.getId(), onlyfirsttime);
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

	public void setVanillaClickSound(LayoutVanillaButton button, String sound) {
		if (sound != null) {
			this.vanillaClickSounds.put(button.button.getId(), sound);
		} else {
			if (this.vanillaClickSounds.containsKey(button.button.getId())) {
				this.vanillaClickSounds.remove(button.button.getId());
			}
		}
	}
	
	public void hideVanillaButton(LayoutVanillaButton b) {
		if (!this.hidden.contains(b) && this.content.contains(b)) {
			this.history.saveSnapshot(this.history.createSnapshot());
			
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
			this.history.saveSnapshot(this.history.createSnapshot());
			
			this.hidden.remove(b);
			b.hidden = false;
			this.updateHiddenButtonPopup();
			this.updateContent();
		}
	}
	
	public void addMenu(IMenu menu) {
		if (!this.menus.contains(menu)) {
			//TODO übernehmen
			if (menu instanceof ContextMenu) {
				((ContextMenu) menu).setAlwaysOnTop(true);
			}
			this.menus.add(menu);
		}
	}

	public void removeMenu(IMenu menu) {
		if (this.menus.contains(menu)) {
			this.menus.remove(menu);
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
				this.focusedObjects.remove(object);
			}
			this.content.remove(object);
			this.updateContent();
		}
	}
	
	public List<LayoutObject> getContent() {
		return this.content;
	}

	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
		
		//Handle object focus and update the top hovered object
		if (!MouseInput.isVanillaInputBlocked()) {
			if (!KeyboardHandler.isCtrlPressed() && !this.focusedObjects.isEmpty() && !this.isFocusedHovered() && !this.isFocusedDragged() && !this.isFocusedGrabberPressed() && !this.isFocusedGettingResized() && (MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown())) {
				if (((this.multiselectRightclickPopup == null) || !this.multiselectRightclickPopup.isHovered()) && !this.isFocusChangeBlocked()) {
					this.focusedObjects.clear();
				}
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
			if (((KeyboardHandler.isCtrlPressed() && !this.isFocused(ob)) || (!this.isObjectFocused()) && (ob != null))) {
				this.setObjectFocused(ob, true, false);
			}
			this.topObject = top;
		} else {
			this.focusedObjects.clear();
		}

		this.renderCreatorBackground(matrix);

		if (this.renderorder.equalsIgnoreCase("foreground")) {
			this.renderVanillaButtons(matrix, mouseX, mouseY);
		}
		//Renders all layout objects. The focused object is always rendered on top of all other objects.
		for (LayoutObject l : this.content) {
			if (!(l instanceof LayoutVanillaButton)) {
				if (!this.isHidden(l)) {
					if (!this.isFocused(l)) {
						l.render(matrix, mouseX, mouseY);
					}
				}
			}
		}
		if (this.renderorder.equalsIgnoreCase("background")) {
			this.renderVanillaButtons(matrix, mouseX, mouseY);
		}
		
		for (LayoutObject o : this.getFocusedObjects()) {
			o.render(matrix, mouseX, mouseY);
		}
		
		this.addObjectPopup.render(matrix, mouseX, mouseY);

		this.addAnimationMenu.render(matrix, mouseX, mouseY);

		this.addSlideshowMenu.render(matrix, mouseX, mouseY);

		this.addShapesMenu.render(matrix, mouseX, mouseY);
		
		this.hiddenPopup.render(matrix, mouseX, mouseY);
		this.renderHiddenButtonIndicator();
		
		this.audioPopup.render(matrix, mouseX, mouseY);
		
		boolean sub = false;
		for (PopupMenu p : this.audioSubPopups) {
			p.render(matrix, mouseX, mouseY);
			
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
			this.backgroundRightclickMenu.render(matrix, mouseX, mouseY);
		}
		
		if (this.expanded) {
			this.settingsButton.render(matrix, mouseX, mouseY, partialTicks);
			this.closeButton.render(matrix, mouseX, mouseY, partialTicks);
			this.saveButton.render(matrix, mouseX, mouseY, partialTicks);
			this.audioButton.render(matrix, mouseX, mouseY, partialTicks);
			this.hiddenButton.render(matrix, mouseX, mouseY, partialTicks);
			this.addObjectButton.render(matrix, mouseX, mouseY, partialTicks);
		}
		
		if (MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown()) {
			if (!this.hiddenButton.isHovered() && !this.hiddenPopup.isHovered()) {
				this.hiddenPopup.closeMenu();
			}
			if (!this.audioButton.isHovered() && !this.audioPopup.isHovered() && !sub) {
				this.audioPopup.closeMenu();
			}
			if (!this.addObjectButton.isHovered() && !this.addObjectPopup.isHovered()) {
				this.addObjectPopup.closeMenu();
			}
			if ((!this.backgroundRightclickMenu.isHovered() && !this.renderorderPopup.isHovered() && !this.mcversionPopup.isHovered() && !this.fmversionPopup.isHovered() && !this.windowsizePopup.isHovered()) || this.isObjectFocused()) {
				this.backgroundRightclickMenu.closeMenu();
			}
			if ((this.multiselectRightclickPopup != null) && !this.multiselectRightclickPopup.isHovered()) {
				this.multiselectRightclickPopup.closeMenu();
			}
		}

		if (this.multiselectRightclickPopup != null) {
			this.multiselectRightclickPopup.render(matrix, mouseX, mouseY);
		}

		if ((this.focusedObjects.size() > 1) && this.isFocusedHovered()) {
			if (MouseInput.isRightMouseDown()) {
				if (this.multiselectRightclickPopup != null) {
					this.multiselectRightclickPopup.closeMenu();
				}
				
				this.updateMultiselectRightclickPopup();
				
				if (this.multiselectRightclickPopup != null) {
					this.multiselectRightclickPopup.openMenuAt(mouseX, mouseY);
				}
			}
		}
		
		if (!this.addObjectPopup.isOpen()) {
			this.addAnimationMenu.closeMenu();
			this.addSlideshowMenu.closeMenu();
			this.addShapesMenu.closeMenu();
		}

		if (PopupHandler.isPopupActive() || this.leftDownAndFocused) {
			this.settingsButton.setUseable(false);
			this.saveButton.setUseable(false);
			this.audioButton.setUseable(false);
			this.hiddenButton.setUseable(false);
			this.closeButton.setUseable(false);
			this.addObjectButton.setUseable(false);
		} else {
			this.settingsButton.setUseable(true);
			this.saveButton.setUseable(true);
			this.audioButton.setUseable(true);
			this.hiddenButton.setUseable(true);
			this.closeButton.setUseable(true);
			this.addObjectButton.setUseable(true);
		}

		this.renderMenuExpandIndicator(matrix, mouseX, mouseY);

		if (this.isObjectFocused() && MouseInput.isLeftMouseDown()) {
			this.leftDownAndFocused = true;
		} else {
			this.leftDownAndFocused = false;
		}

		this.history.render(matrix);
		
		super.render(matrix, mouseX, mouseY, partialTicks);

		//Needs to be done after other object render stuff to prevent ConcurrentModificationExceptions.
		if (this.newContent != null) {
			this.history.saveSnapshot(this.history.createSnapshot());
			this.content = this.newContent;
			this.newContent = null;
		}
	}

	private void renderVanillaButtons(MatrixStack matrix, int mouseX, int mouseY) {
		for (LayoutObject l : this.vanillaButtonContent) {
			if (!this.isHidden(l)) {
				if (!this.isFocused(l)) {
					l.render(matrix, mouseX, mouseY);
				}
			}
		}
	}
	
	private void renderMenuExpandIndicator(MatrixStack matrix, int mouseX, int mouseY) {
		RenderSystem.enableBlend();
		int x = 5;
		int y = (this.height / 2) - 10;
		if (this.expanded) {
			Minecraft.getInstance().getTextureManager().bindTexture(SHRINK_INDICATOR);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 0.5F);
		} else {
			Minecraft.getInstance().getTextureManager().bindTexture(EXPAND_INDICATOR);
			RenderSystem.color4f(this.expandColor.getRed(), this.expandColor.getGreen(), this.expandColor.getBlue(), 0.5F);
		}
		blit(matrix, x, y, 0.0F, 0.0F, 20, 20, 20, 20);
		RenderSystem.disableBlend();

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
					this.hiddenButton.setMessage("§4" + Locals.localize("helper.creator.menu.hidden"));;
					this.expandColor = Color.RED;
				}
				if ((this.hiddenIndicatorCount == 1) || (this.hiddenIndicatorCount == 3) || (this.hiddenIndicatorCount == 5)) {
					this.hiddenButton.setMessage(Locals.localize("helper.creator.menu.hidden"));;
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

	private void renderCreatorBackground(MatrixStack matrix) {
		RenderSystem.enableBlend();
		fill(matrix, 0, 0, this.width, this.height, new Color(38, 38, 38).getRGB());

		if (this.backgroundTexture != null) {
			Minecraft.getInstance().getTextureManager().bindTexture(this.backgroundTexture.getResourceLocation());
			
			if (!this.panorama) {
				blit(matrix, 0, 0, 1.0F, 1.0F, this.width + 1, this.height + 1, this.width + 1, this.height + 1);
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
					if (panoTick >= 1) {
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
						panoTick++;
					}
				} else {
					if (panoTick >= 300) {
						panoStop = false;
						panoTick = 0;
					} else {
						panoTick++;
					}
				}
				if (wfinal <= this.width) {
					blit(matrix, 0, 0, 1.0F, 1.0F, this.width + 1, this.height + 1, this.width + 1, this.height + 1);
				} else {
					RenderUtils.doubleBlit(panoPos, 0, 1.0F, 1.0F, wfinal, this.height + 1);
				}
			}
		}
		RenderSystem.disableBlend();
		
		if (this.backgroundAnimation != null) {
			boolean b = this.backgroundAnimation.isStretchedToStreensize();
			this.backgroundAnimation.setStretchImageToScreensize(true);
			this.backgroundAnimation.render(matrix);
			this.backgroundAnimation.setStretchImageToScreensize(b);
		}

		if (this.backgroundPanorama != null) {
			this.backgroundPanorama.render();
		}

		if (this.backgroundSlideshow != null) {
			int sw = this.backgroundSlideshow.width;
			int sh = this.backgroundSlideshow.height;
			int sx = this.backgroundSlideshow.x;
			int sy = this.backgroundSlideshow.y;
			
			this.backgroundSlideshow.height = this.height;
			this.backgroundSlideshow.width = this.width;
			this.backgroundSlideshow.x = 0;
			this.backgroundSlideshow.y = 0;
			
			this.backgroundSlideshow.render(CurrentScreenHandler.getMatrixStack());
			
			this.backgroundSlideshow.width = sw;
			this.backgroundSlideshow.height = sh;
			this.backgroundSlideshow.x = sx;
			this.backgroundSlideshow.y = sy;
		}
	}

	public boolean isFocused(LayoutObject object) {
		if (PopupHandler.isPopupActive()) {
			return false;
		}
		return (this.focusedObjects.contains(object));
	}

	public void setObjectFocused(LayoutObject object, boolean focused, boolean ignoreBlockedFocusChange) {
		if (this.isFocusChangeBlocked() && !ignoreBlockedFocusChange) {
			return;
		}
		if (!this.content.contains(object)) {
			return;
		}
		if (focused) {
			if (this.backgroundRightclickMenu.isHovered() || this.renderorderPopup.isHovered()) {
				return;
			}
			if (!this.focusedObjects.contains(object)) {
				this.focusedObjects.add(object);
			}
		} else {
			if (this.focusedObjects.contains(object)) {
				this.focusedObjects.remove(object);
			}
		}
	}

	public boolean isObjectFocused() {
		return (!this.focusedObjects.isEmpty());
	}

	public boolean isFocusedHovered() {
		for (LayoutObject o : this.focusedObjects) {
			if (o.isHovered()) {
				return true;
			}
		}
		return false;
	}

	public boolean isFocusedDragged() {
		for (LayoutObject o : this.focusedObjects) {
			if (o.isDragged()) {
				return true;
			}
		}
		return false;
	}

	public boolean isFocusedGrabberPressed() {
		for (LayoutObject o : this.focusedObjects) {
			if (o.isGrabberPressed()) {
				return true;
			}
		}
		return false;
	}

	public boolean isFocusedGettingResized() {
		for (LayoutObject o : this.focusedObjects) {
			if (o.isGettingResized()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a copy of the focused objects list.
	 */
	public List<LayoutObject> getFocusedObjects() {
		List<LayoutObject> l = new ArrayList<LayoutObject>();
		l.addAll(this.focusedObjects);
		return l;
	}

	public void clearFocusedObjects() {
		if (this.multiselectRightclickPopup != null) {
			this.multiselectRightclickPopup.closeMenu();
		}
		this.focusedObjects.clear();
	}
	
	public boolean isContentHovered() {
		for (LayoutObject o : this.content) {
			if (o.isHovered()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the LayoutObject the given object was moved above.
	 */
	public LayoutObject moveUp(LayoutObject o) {
		LayoutObject movedAbove = null;
		try {
			if (this.content.contains(o)) {
				List<LayoutObject> l = new ArrayList<LayoutObject>();
				int index = this.content.indexOf(o);
				int i = 0;
				if (index < this.content.size() - 1) {
					for (LayoutObject o2 : this.content) {
						if (o2 != o) {
							l.add(o2);
							if (i == index+1) {
								movedAbove = o2;
								l.add(o);
							}
						}
						i++;
					}
					
					this.newContent = l;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return movedAbove;
	}

	/**
	 * Returns the LayoutObject the object was moved behind.<br>
	 * Will <b>NOT</b> move behind {@link LayoutVanillaButton}s, but will return the vanilla button the object would have been moved under.
	 */
	public LayoutObject moveDown(LayoutObject o) {
		LayoutObject movedBehind = null;
		try {
			if (this.content.contains(o)) {
				List<LayoutObject> l = new ArrayList<LayoutObject>();
				int index = this.content.indexOf(o);
				int i = 0;
				if (index > 0) {
					for (LayoutObject o2 : this.content) {
						if (o2 != o) {
							if (i == index-1) {
								l.add(o);
								movedBehind = o2;
							}
							l.add(o2);
						}
						i++;
					}
					
					if (!(movedBehind instanceof LayoutVanillaButton)) {
						this.newContent = l;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return movedBehind;
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
				this.history.saveSnapshot(this.history.createSnapshot());
				
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
			this.history.saveSnapshot(this.history.createSnapshot());
			
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

	private void addSlideshow(String name) {
		this.addSlideshowMenu.closeMenu();
		if (name == null) {
			this.setMenusUseable(true);
			return;
		}
		if (SlideshowHandler.slideshowExists(name)) {
			this.history.saveSnapshot(this.history.createSnapshot());
			
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "addslideshow");
			s.addEntry("name", name);
			SlideshowCustomizationItem i = new SlideshowCustomizationItem(s);
			int w = SlideshowHandler.getSlideshow(name).width;
			int h = SlideshowHandler.getSlideshow(name).height;
			double ratio = (double) w / (double) h;
			i.height = 100;
			i.width = (int)(i.height * ratio);
			
			this.addContent(new LayoutSlideshow(i, this));
			
			this.setMenusUseable(true);
		} else {
			this.displayNotification(300, Locals.localize("helper.creator.slideshownotfound"), "", "", "", "");
		}
	}
	
	private void addAnimation(String name) {
		this.addAnimationMenu.closeMenu();
		if (name == null) {
			this.setMenusUseable(true);
			return;
		}
		if (AnimationHandler.animationExists(name)) {
			this.history.saveSnapshot(this.history.createSnapshot());
			
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
		this.history.saveSnapshot(this.history.createSnapshot());

		int w = 100;
		if (Minecraft.getInstance().fontRenderer.getStringWidth(label) + 10 > w) {
			w = Minecraft.getInstance().fontRenderer.getStringWidth(label) + 10;
		}
		this.addContent(new LayoutButton(w, 20, label, null, this));
		
		this.setMenusUseable(true);
	}
	
	private void addWebText(String url) {
		if (url != null) {
			url = WebUtils.filterURL(url);
		}
		if (WebUtils.isValidUrl(url)) {
			this.history.saveSnapshot(this.history.createSnapshot());
			
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
			this.history.saveSnapshot(this.history.createSnapshot());
			
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
					this.history.saveSnapshot(this.history.createSnapshot());
					
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
				this.history.saveSnapshot(this.history.createSnapshot());

				this.backgroundPanorama = null;
				this.backgroundSlideshow = null;
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
				this.history.saveSnapshot(this.history.createSnapshot());
				
				((AdvancedAnimation)this.backgroundAnimation).stopAudio();
			}
			this.backgroundAnimation = null;
			this.backgroundAnimationNames.clear();
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
					this.history.saveSnapshot(this.history.createSnapshot());
					
					this.backgroundTexture = TextureHandler.getResource(path);
					this.backgroundTexturePath = path;
					if (this.backgroundAnimation != null) {
						((AdvancedAnimation)this.backgroundAnimation).stopAudio();
					}
					this.backgroundAnimation = null;
					this.backgroundAnimationNames.clear();

					this.backgroundPanorama = null;
					this.backgroundSlideshow = null;

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
				String filename = this.generateCustomizationFileName(FancyMenu.getCustomizationPath().getPath(), name);
				
				this.saveToCustomizationFile(filename);

				Snapshot snap = this.history.createSnapshot();

				List<PropertiesSet> l = new ArrayList<PropertiesSet>();
				l.add(snap.snapshot);

				PreloadedLayoutCreatorScreen neweditor = new PreloadedLayoutCreatorScreen(this.screen, l);
				neweditor.history = this.history;
				this.history.editor = neweditor;
				neweditor.expanded = this.expanded;
				neweditor.single = FancyMenu.getCustomizationPath().getPath() + "/" + filename;

				Minecraft.getInstance().displayGuiScreen(neweditor);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.setMenusUseable(true);
	}

	private void updatePositionArrowKeys(KeyboardData d) {
		if ((this == Minecraft.getInstance().currentScreen) && this.isObjectFocused() && !PopupHandler.isPopupActive()) {
			
			if (!((d.keycode == 263) || (d.keycode == 262) || (d.keycode == 265) || (d.keycode == 264))) {
				return;
			}
			
			Snapshot snap = this.history.createSnapshot();
			boolean saveSnap = false;
			
			for (LayoutObject o : this.focusedObjects) {
				if ((o instanceof LayoutVanillaButton) && o.object.orientation.equals("original")) {
					((LayoutVanillaButton)o).displaySetOrientationNotification();
					continue;
				}
				if (d.keycode == 263) {
					saveSnap = true;
					
					o.setX(o.object.posX - 1);
				}
				if (d.keycode == 262) {
					saveSnap = true;
					
					o.setX(o.object.posX + 1);
				}
				if (d.keycode == 265) {
					saveSnap = true;
					
					o.setY(o.object.posY - 1);
				}
				if (d.keycode == 264) {
					saveSnap = true;
					
					o.setY(o.object.posY + 1);
				}
			}
			
			if (saveSnap) {
				this.history.saveSnapshot(snap);
			}
			
		}
	}

	private void onDeletePress(KeyboardData d) {
		if ((this == Minecraft.getInstance().currentScreen) && this.isObjectFocused() && !PopupHandler.isPopupActive()) {
			if (d.keycode == 261) {
				this.deleteFocusedObjects();
			}
		}
	}

	private void deleteFocusedObjects() {
		List<LayoutObject> l = new ArrayList<LayoutObject>();
		l.addAll(this.focusedObjects);
		
		if (!l.isEmpty()) {
			if (l.size() == 1) {
				if (l.get(0).isDestroyable()) {
					l.get(0).destroyObject();
				} else {
					this.displayNotification(300, "§c§l" + Locals.localize("helper.creator.cannotdelete.title"), "", Locals.localize("helper.creator.cannotdelete.desc"), "", "", "");
				}
			}
			if (l.size() > 1) {
				this.setMenusUseable(false);
				PopupHandler.displayPopup(new YesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
					if (call) {
						this.history.saveSnapshot(this.history.createSnapshot());
						
						for (LayoutObject o : l) {
							if (o.isDestroyable()) {
								this.removeContent(o);
							}
						}
					}
					this.setMenusUseable(true);
				}, "§c§l" + Locals.localize("helper.creator.messages.sure"), "", Locals.localize("helper.creator.deleteselectedobjects"), "", "", "", "", ""));
			}
		}
	}
	
	public int getWidth() {
		return this.width;
	}
	
	public int getHeight() {
		return this.height;
	}
	
	public Screen getScreenToCustomize() {
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

	public void updateMenuScale() {
		for (IMenu m : this.menus) {
			if (m instanceof PopupMenu) {
				((PopupMenu) m).menuScale = FancyMenu.config.getOrDefault("popupmenuscale", 1.0F);
			}
		}
	}

	public static float getPopupMenuScale() {
		return FancyMenu.config.getOrDefault("popupmenuscale", 1.0F);
	}

}

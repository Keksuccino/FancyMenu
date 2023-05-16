//package de.keksuccino.fancymenu.customization.layout.editor;
//
//import com.mojang.blaze3d.platform.Window;
//import com.mojang.blaze3d.systems.RenderSystem;
//import com.mojang.blaze3d.vertex.PoseStack;
//import de.keksuccino.fancymenu.FancyMenu;
//import de.keksuccino.fancymenu.customization.background.MenuBackground;
//import de.keksuccino.fancymenu.customization.ScreenCustomization;
//import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
//import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
//import de.keksuccino.fancymenu.customization.button.ButtonCache;
//import de.keksuccino.fancymenu.customization.button.ButtonData;
//import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
//import de.keksuccino.fancymenu.customization.deep.DeepScreenCustomizationLayer;
//import de.keksuccino.fancymenu.customization.deep.DeepScreenCustomizationLayerRegistry;
//import de.keksuccino.fancymenu.customization.deep.AbstractDeepEditorElement;
//import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
//import de.keksuccino.fancymenu.customization.element.v1.*;
//import de.keksuccino.fancymenu.customization.element.v1.ShapeCustomizationItem.Shape;
//import de.keksuccino.fancymenu.customization.guicreator.CustomGuiBase;
//import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
//import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
//import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorUI.LayoutEditorRightClickContextMenu;
//import de.keksuccino.fancymenu.customization.layout.editor.elements.*;
//import de.keksuccino.fancymenu.customization.layout.editor.elements.button.ButtonBackgroundPopup;
//import de.keksuccino.fancymenu.customization.layout.editor.elements.button.LayoutButton;
//import de.keksuccino.fancymenu.customization.layout.editor.elements.button.LayoutVanillaButton;
//import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
//import de.keksuccino.fancymenu.customization.panorama.ExternalTexturePanoramaRenderer;
//import de.keksuccino.fancymenu.customization.slideshow.ExternalTextureSlideshowRenderer;
//import de.keksuccino.fancymenu.customization.slideshow.SlideshowHandler;
//import de.keksuccino.fancymenu.rendering.texture.ExternalTextureHandler;
//import de.keksuccino.fancymenu.rendering.ui.UIBase;
//import de.keksuccino.fancymenu.rendering.ui.contextmenu.ContextMenu;
//import de.keksuccino.fancymenu.rendering.ui.popup.FMNotificationPopup;
//import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
//import de.keksuccino.fancymenu.rendering.ui.popup.FMYesNoPopup;
//import de.keksuccino.fancymenu.utils.ScreenTitleUtils;
//import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
//import de.keksuccino.konkrete.input.*;
//import de.keksuccino.konkrete.localization.Locals;
//import de.keksuccino.fancymenu.properties.PropertyContainer;
//import de.keksuccino.fancymenu.properties.PropertyContainerSet;
//import de.keksuccino.konkrete.rendering.RenderUtils;
//import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
//import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
//import de.keksuccino.konkrete.sound.SoundHandler;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.screens.Screen;
//import net.minecraft.network.chat.Component;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.jetbrains.annotations.Nullable;
//
//import java.awt.*;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class LayoutEditorScreenOLD extends Screen {
//
//	private static final Logger LOGGER = LogManager.getLogger("fancymenu/LayoutEditorScreen");
//
//	public static boolean isActive = false;
//
//	protected static final List<PropertyContainer> COPIED_ELEMENT_CACHE = new ArrayList<PropertyContainer>();
//	protected static boolean initDone = false;
//
//	public LayoutEditorHistory history = new LayoutEditorHistory(this);
//
//	public List<Runnable> postRenderTasks = new ArrayList<>();
//
//	public final Screen screen;
//	protected List<AbstractEditorElement> content = new ArrayList<>();
//	protected List<AbstractEditorElement> newContentMove;
//	protected List<AbstractEditorElement> newContentPaste = new ArrayList<>();
//	public List<AbstractEditorElement> deleteContentQueue = new ArrayList<>();
//	protected List<AbstractEditorElement> vanillaButtonContent = new ArrayList<>();
//	protected Map<String, Boolean> audio = new HashMap<>();
//	public Map<Long, ScreenCustomizationLayer.ButtonCustomizationContainer> vanillaButtonCustomizationContainers = new HashMap<>();
//	public Map<Long, Float> vanillaDelayAppearance = new HashMap<>();
//	public Map<Long, Boolean> vanillaDelayAppearanceFirstTime = new HashMap<>();
//	public Map<Long, Float> vanillaFadeIn = new HashMap<>();
//	protected List<AbstractEditorElement> focusedObjects = new ArrayList<>();
//	protected List<AbstractEditorElement> focusedObjectsCache = new ArrayList<>();
//
//	protected ContextMenu multiselectRightclickMenu;
//	protected LayoutEditorRightClickContextMenu propertiesRightclickMenu;
//
//	protected IAnimationRenderer backgroundAnimation;
//	public ExternalTextureResourceLocation backgroundTexture;
//	public String backgroundTexturePath;
//	public ExternalTexturePanoramaRenderer backgroundPanorama;
//	public ExternalTextureSlideshowRenderer backgroundSlideshow;
//	public MenuBackground customMenuBackground;
//	public String customMenuBackgroundInputString;
//	public List<String> backgroundAnimationNames = new ArrayList<>();
//	public boolean randomBackgroundAnimation = false;
//	public boolean panorama = false;
//	protected int panoTick = 0;
//	protected double panoPos = 0.0;
//	protected boolean panoMoveBack = false;
//	protected boolean panoStop = false;
//	protected boolean keepBackgroundAspectRatio = false;
//	protected boolean restartAnimationBackgroundOnLoad = false;
//
//	protected String openAudio;
//	protected String closeAudio;
//
//	protected String renderorder = "foreground";
//	protected String requiredmods;
//	protected String minimumMC;
//	protected String maximumMC;
//	protected String minimumFM;
//	protected String maximumFM;
//
//	protected int biggerThanWidth = 0;
//	protected int biggerThanHeight = 0;
//	protected int smallerThanWidth = 0;
//	protected int smallerThanHeight = 0;
//
//	protected boolean randomMode = false;
//	protected String randomGroup = "1";
//	protected boolean randomOnlyFirstTime = false;
//
//	protected int autoScalingWidth = 0;
//	protected int autoScalingHeight = 0;
//
//	protected String customMenuTitle = null;
//
//	protected int scale = 0;
//
//	protected boolean multiselectStretchedX = false;
//	protected boolean multiselectStretchedY = false;
//	protected List<de.keksuccino.konkrete.gui.content.ContextMenu> multiselectChilds = new ArrayList<>();
//
//	protected AbstractEditorElement topObject;
//
//	protected List<String> universalLayoutWhitelist = new ArrayList<>();
//	protected List<String> universalLayoutBlacklist = new ArrayList<>();
//
//	protected LoadingRequirementContainer layoutWideLoadingRequirementContainer = new LoadingRequirementContainer();
//
//	protected ContextMenu activeElementContextMenu = null;
//
//	public LayoutEditorUI ui = new LayoutEditorUI(this);
//
//	public LayoutEditorScreenOLD(Screen screenToCustomize) {
//		super(Component.literal(""));
//		this.screen = screenToCustomize;
//		Component cachedOriTitle = ScreenCustomizationLayer.cachedOriginalMenuTitles.get(this.screen.getClass());
//		if (cachedOriTitle != null) {
//			ScreenTitleUtils.setScreenTitle(this.screen, cachedOriTitle);
//		}
//
//		if (!initDone) {
//			KeyboardHandler.addKeyPressedListener(LayoutEditorScreenOLD::onShortcutPressed);
//			KeyboardHandler.addKeyPressedListener(LayoutEditorScreenOLD::onArrowKeysPressed);
//			initDone = true;
//		}
//
//		if (!(this instanceof PreloadedLayoutEditorScreen)) {
//			DeepScreenCustomizationLayer layer = DeepScreenCustomizationLayerRegistry.getLayer(this.screen.getClass().getName());
//			if (layer != null) {
//				for (DeepElementBuilder e : layer.getBuilders()) {
//					this.content.add(e.constructEditorElementInstance(e.constructDefaultItemInstance(), this));
//				}
//			}
//		}
//
//	}
//
//	@Override
//	protected void init() {
//
//		this.ui.updateTopMenuBar();
//
//		if (this.multiselectRightclickMenu != null) {
//			this.multiselectRightclickMenu.closeMenu();
//		}
//		this.multiselectRightclickMenu = new LayoutEditorUI.MultiselectContextMenu(this);
//		this.multiselectRightclickMenu.setAutoclose(false);
//		this.multiselectRightclickMenu.setAlwaysOnTop(true);
//
//		if (this.propertiesRightclickMenu != null) {
//			this.propertiesRightclickMenu.closeMenu();
//		}
//		this.propertiesRightclickMenu = new LayoutEditorRightClickContextMenu(this, true);
//		this.propertiesRightclickMenu.setAutoclose(false);
//		this.propertiesRightclickMenu.setAlwaysOnTop(true);
//
//		if (this.scale > 0) {
//			Minecraft.getInstance().getWindow().setGuiScale(this.scale);
//		} else {
//			Minecraft.getInstance().getWindow().setGuiScale(Minecraft.getInstance().getWindow().calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode()));
//		}
//		this.height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
//		this.width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
//
//		if ((this.autoScalingWidth != 0) && (this.autoScalingHeight != 0)) {
//			Window m = Minecraft.getInstance().getWindow();
//			double guiWidth = this.width * m.getGuiScale();
//			double guiHeight = this.height * m.getGuiScale();
//			double percentX = (guiWidth / (double)this.autoScalingWidth) * 100.0D;
//			double percentY = (guiHeight / (double)this.autoScalingHeight) * 100.0D;
//			double newScaleX = (percentX / 100.0D) * m.getGuiScale();
//			double newScaleY = (percentY / 100.0D) * m.getGuiScale();
//			double newScale = Math.min(newScaleX, newScaleY);
//
//			m.setGuiScale(newScale);
//			this.width = m.getGuiScaledWidth();
//			this.height = m.getGuiScaledHeight();
//		} else if (this.scale <= 0) {
//			Minecraft.getInstance().getWindow().setGuiScale(Minecraft.getInstance().getWindow().calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode()));
//		}
//
//		this.focusedObjects.clear();
//		this.updateContent();
//
//		this.resetActiveElementContextMenu();
//
//	}
//
//	@Override
//	public boolean shouldCloseOnEsc() {
//		return false;
//	}
//
//	protected List<PropertyContainer> getAllProperties() {
//
//		List<PropertyContainer> l = new ArrayList<>();
//
//		PropertyContainer meta = new PropertyContainer("customization-meta");
//		meta.putProperty("identifier", this.getScreenToCustomizeIdentifier());
//		meta.putProperty("renderorder", this.renderorder);
//
//		meta.putProperty("randommode", "" + this.randomMode);
//		meta.putProperty("randomgroup", this.randomGroup);
//		meta.putProperty("randomonlyfirsttime", "" + this.randomOnlyFirstTime);
//
////		if ((this.requiredmods != null) && !this.requiredmods.replace(" ", "").equals("")) {
////			meta.addEntry("requiredmods", this.requiredmods);
////		}
////		if ((this.minimumMC != null) && !this.minimumMC.replace(" ", "").equals("")) {
////			meta.addEntry("minimummcversion", this.minimumMC);
////		}
////		if ((this.maximumMC != null) && !this.maximumMC.replace(" ", "").equals("")) {
////			meta.addEntry("maximummcversion", this.maximumMC);
////		}
////		if ((this.minimumFM != null) && !this.minimumFM.replace(" ", "").equals("")) {
////			meta.addEntry("minimumfmversion", this.minimumFM);
////		}
////		if ((this.maximumFM != null) && !this.maximumFM.replace(" ", "").equals("")) {
////			meta.addEntry("maximumfmversion", this.maximumFM);
////		}
////		if (this.biggerThanWidth != 0) {
////			meta.addEntry("biggerthanwidth", "" + this.biggerThanWidth);
////		}
////		if (this.biggerThanHeight != 0) {
////			meta.addEntry("biggerthanheight", "" + this.biggerThanHeight);
////		}
////		if (this.smallerThanWidth != 0) {
////			meta.addEntry("smallerthanwidth", "" + this.smallerThanWidth);
////		}
////		if (this.smallerThanHeight != 0) {
////			meta.addEntry("smallerthanheight", "" + this.smallerThanHeight);
////		}
//		if (this.isUniversalLayout() && !this.universalLayoutWhitelist.isEmpty()) {
//			String wl = "";
//			for (String s : this.universalLayoutWhitelist) {
//				wl += s + ";";
//			}
//			meta.putProperty("universal_layout_whitelist", wl);
//		}
//		if (this.isUniversalLayout() && !this.universalLayoutBlacklist.isEmpty()) {
//			String bl = "";
//			for (String s : this.universalLayoutBlacklist) {
//				bl += s + ";";
//			}
//			meta.putProperty("universal_layout_blacklist", bl);
//		}
//		if (this.customMenuTitle != null) {
//			meta.putProperty("custom_menu_title", this.customMenuTitle);
//		}
//
//		this.layoutWideLoadingRequirementContainer.serializeContainerToExistingPropertiesSection(meta);
//
//		l.add(meta);
//
//		if (!this.backgroundAnimationNames.isEmpty()) {
//			String names = this.backgroundAnimationNames.get(0);
//			if (this.backgroundAnimationNames.size() > 1) {
//				int i = 0;
//				for (String s : this.backgroundAnimationNames) {
//					if (i > 0) {
//						names += ", " + s;
//					}
//					i++;
//				}
//			}
//			PropertyContainer ps = new PropertyContainer("customization");
//			ps.putProperty("action", "animatebackground");
//			ps.putProperty("name", names);
//			if (this.randomBackgroundAnimation) {
//				ps.putProperty("random", "true");
//			}
//			ps.putProperty("restart_on_load", "" + this.restartAnimationBackgroundOnLoad);
//			l.add(ps);
//		}
//
//		if (this.backgroundPanorama != null) {
//			PropertyContainer ps = new PropertyContainer("customization");
//			ps.putProperty("action", "setbackgroundpanorama");
//			ps.putProperty("name", this.backgroundPanorama.getName());
//			l.add(ps);
//		}
//
//		if (this.backgroundSlideshow != null) {
//			PropertyContainer ps = new PropertyContainer("customization");
//			ps.putProperty("action", "setbackgroundslideshow");
//			ps.putProperty("name", this.backgroundSlideshow.getName());
//			l.add(ps);
//		}
//
//		if (this.backgroundTexture != null) {
//			PropertyContainer ps = new PropertyContainer("customization");
//			ps.putProperty("action", "texturizebackground");
//			ps.putProperty("path", this.backgroundTexturePath);
//			if (this.panorama) {
//				ps.putProperty("wideformat", "true");
//			}
//			l.add(ps);
//		}
//
//		if (this.customMenuBackground != null) {
//			PropertyContainer ps = new PropertyContainer("customization");
//			ps.putProperty("action", "api:custombackground");
//			ps.putProperty("type_identifier", this.customMenuBackground.getType().getIdentifier());
//			if (this.customMenuBackground.getType().needsInputString()) {
//				ps.putProperty("input_string", this.customMenuBackgroundInputString);
//			} else {
//				ps.putProperty("background_identifier", this.customMenuBackground.getIdentifier());
//			}
//			l.add(ps);
//		}
//
//		if (this.scale > 0) {
//			PropertyContainer ps = new PropertyContainer("customization");
//			ps.putProperty("action", "setscale");
//			ps.putProperty("scale", "" + this.scale);
//			l.add(ps);
//		}
//
//		if ((this.autoScalingWidth != 0) && (this.autoScalingHeight != 0)) {
//			PropertyContainer ps = new PropertyContainer("customization");
//			ps.putProperty("action", "autoscale");
//			ps.putProperty("basewidth", "" + this.autoScalingWidth);
//			ps.putProperty("baseheight", "" + this.autoScalingHeight);
//			l.add(ps);
//		}
//
//		if (this.openAudio != null) {
//			PropertyContainer ps = new PropertyContainer("customization");
//			ps.putProperty("action", "setopenaudio");
//			ps.putProperty("path", this.openAudio);
//			l.add(ps);
//		}
//
//		if (this.closeAudio != null) {
//			PropertyContainer ps = new PropertyContainer("customization");
//			ps.putProperty("action", "setcloseaudio");
//			ps.putProperty("path", this.closeAudio);
//			l.add(ps);
//		}
//
//		for (Map.Entry<String, Boolean> m : this.audio.entrySet()) {
//			PropertyContainer s = new PropertyContainer("customization");
//			s.putProperty("action", "addaudio");
//			s.putProperty("path", m.getKey());
//			s.putProperty("loop", "" + m.getValue());
//			l.add(s);
//		}
//
//		//Background Options Section
//		PropertyContainer s = new PropertyContainer("customization");
//		s.putProperty("action", "backgroundoptions");
//		s.putProperty("keepaspectratio", "" + this.keepBackgroundAspectRatio);
//		l.add(s);
//
//		for (AbstractEditorElement o : this.content) {
//			l.addAll(o.getProperties());
//		}
//		return l;
//	}
//
//	/**
//	 * Updates the LayoutObjects shown in the CreatorScreen.<br>
//	 * The positions of all UNMODIFIED vanilla buttons will be updated to keep them at the correct position when the screen is getting resized.
//	 */
//	protected void updateContent() {
//		List<AbstractEditorElement> l = new ArrayList<>();
//		for (AbstractEditorElement o : this.content) {
//			if (!(o instanceof LayoutVanillaButton)) {
//				l.add(o);
//			} else {
//				if (!o.element.anchorPoint.equals("original") || ((LayoutVanillaButton)o).customizationContainer.isButtonHidden) {
//					l.add(o);
//				}
//			}
//		}
//
//		ButtonCache.cacheFrom(this.screen, this.width, this.height);
//
//		this.content.clear();
//
//		//Sync labels, textures, auto clicks and other stuff for vanilla buttons
//		for (ButtonData b : ButtonCache.getButtons()) {
//			if (!this.containsVanillaButton(l, b)) {
//				if (!this.vanillaButtonCustomizationContainers.containsKey(b.getId())) {
//					ScreenCustomizationLayer.ButtonCustomizationContainer cc = new ScreenCustomizationLayer.ButtonCustomizationContainer();
//					PropertyContainer dummySec = new PropertyContainer("customization");
//					cc.loadingRequirementContainer = new LoadingRequirementContainer();
//					this.vanillaButtonCustomizationContainers.put(b.getId(), cc);
//				}
//				LayoutVanillaButton v = new LayoutVanillaButton(this.vanillaButtonCustomizationContainers.get(b.getId()), b, this);
//				if (this.vanillaDelayAppearance.containsKey(b.getId())) {
//					v.element.delayAppearance = true;
//					v.element.appearanceDelayInSeconds = this.vanillaDelayAppearance.get(b.getId());
//					if (this.vanillaDelayAppearanceFirstTime.containsKey(b.getId())) {
//						v.element.delayAppearanceEverytime = !this.vanillaDelayAppearanceFirstTime.get(b.getId());
//					}
//					if (this.vanillaFadeIn.containsKey(b.getId())) {
//						v.element.fadeIn = true;
//						v.element.fadeInSpeed = this.vanillaFadeIn.get(b.getId());
//					}
//				}
//				l.add(v);
//			}
//		}
//		this.content.addAll(l);
//
//		this.vanillaButtonContent.clear();
//
//		for (AbstractEditorElement o : this.content) {
//
//			o.init();
//
//			if (o instanceof LayoutVanillaButton) {
//				this.vanillaButtonContent.add(o);
//			}
//
//		}
//
//		for (AbstractEditorElement e: this.vanillaButtonContent) {
//			for (AbstractEditorElement e2 : this.content) {
//				e2.onUpdateVanillaButton((LayoutVanillaButton) e);
//			}
//		}
//
//	}
//
//	protected boolean containsVanillaButton(List<AbstractEditorElement> l, ButtonData b) {
//		for (AbstractEditorElement o : l) {
//			if (o instanceof LayoutVanillaButton) {
//				if (((LayoutVanillaButton)o).button.getId() == b.getId()) {
//					return true;
//				}
//			}
//		}
//		return false;
//	}
//
//	public boolean isHidden(AbstractEditorElement b) {
//		if (b instanceof LayoutVanillaButton) {
//			return ((LayoutVanillaButton) b).customizationContainer.isButtonHidden;
//		}
//		return false;
//	}
//
//	public List<LayoutVanillaButton> getHiddenButtons() {
//		List<LayoutVanillaButton> l = new ArrayList<>();
//		for (AbstractEditorElement e : this.vanillaButtonContent) {
//			if (e instanceof LayoutVanillaButton) {
//				if (((LayoutVanillaButton) e).customizationContainer.isButtonHidden) {
//					l.add((LayoutVanillaButton) e);
//				}
//			}
//		}
//		return l;
//	}
//
//	public void hideVanillaButton(LayoutVanillaButton b) {
//		if (!b.customizationContainer.isButtonHidden && this.content.contains(b)) {
//			this.history.saveSnapshot(this.history.createSnapshot());
//
//			b.customizationContainer.isButtonHidden = true;
//			this.setObjectFocused(b, false, true);
//			b.resetElementStates();
//		}
//	}
//
//	public void showVanillaButton(LayoutVanillaButton b) {
//		if (b.customizationContainer.isButtonHidden) {
//			this.history.saveSnapshot(this.history.createSnapshot());
//
//			b.customizationContainer.isButtonHidden = false;
//			b.resetElementStates();
//		}
//	}
//
//	public void addContent(AbstractEditorElement object) {
//		if (!this.content.contains(object)) {
//			this.content.add(object);
//		}
//	}
//
//	public void removeContent(AbstractEditorElement object) {
//		if (this.content.contains(object)) {
//			if ((this.isFocused(object))) {
//				this.focusedObjects.remove(object);
//			}
//			if (!(object instanceof AbstractDeepEditorElement)) {
//				this.content.remove(object);
//			}
//			this.updateContent();
//		}
//		this.resetActiveElementContextMenu();
//	}
//
//	public List<AbstractEditorElement> getContent() {
//		return this.content;
//	}
//
//	@Override
//	public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
//
//		//Handle object focus and update the top hovered object
//		if (!MouseInput.isVanillaInputBlocked()) {
//			if (!KeyboardHandler.isCtrlPressed() && !this.focusedObjects.isEmpty() && !this.isFocusedHovered() && !this.isFocusedDragged() && !this.isFocusedGrabberPressed() && !this.isFocusedGettingResized() && (MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown())) {
//				if (((this.multiselectRightclickMenu == null) || !this.multiselectRightclickMenu.isHovered()) && ((this.propertiesRightclickMenu == null) || !this.propertiesRightclickMenu.isHovered()) && !this.isFocusChangeBlocked() && !this.ui.topMenuBar.isHoveredOrFocused() && !this.ui.topMenuBar.isChildOpen()) {
//					this.focusedObjects.clear();
//				}
//			}
//			AbstractEditorElement ob = null;
//			AbstractEditorElement top = null;
//			for (AbstractEditorElement o : this.content) {
//				if (o.isHovered()) {
//					top = o;
//					if (MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown()) {
//						ob = o;
//					}
//				}
//			}
//			if (((KeyboardHandler.isCtrlPressed() && !this.isFocused(ob)) || (!this.isObjectFocused()) && (ob != null))) {
//				this.setObjectFocused(ob, true, false);
//			}
//			this.topObject = top;
//		} else {
//			if (!this.ui.topMenuBar.isHoveredOrFocused() && !this.ui.topMenuBar.isChildOpen()) {
//				this.focusedObjects.clear();
//			}
//		}
//
//		this.renderCreatorBackground(matrix);
//
//		this.drawGrid(matrix);
//
//		if (this.renderorder.equalsIgnoreCase("foreground")) {
//			this.renderVanillaButtons(matrix, mouseX, mouseY);
//			for (AbstractEditorElement l : this.content) {
//				if (l instanceof AbstractDeepEditorElement) {
//					l.render(matrix, mouseX, mouseY);
//				}
//			}
//		}
//		//Renders all layout objects. The focused object is always rendered on top of all other objects.
//		for (AbstractEditorElement l : this.content) {
//			if (!(l instanceof LayoutVanillaButton) && !(l instanceof AbstractDeepEditorElement)) {
//				if (!this.isFocused(l)) {
//					l.render(matrix, mouseX, mouseY);
//				}
//			}
//		}
//		if (this.renderorder.equalsIgnoreCase("background")) {
//			this.renderVanillaButtons(matrix, mouseX, mouseY);
//			for (AbstractEditorElement l : this.content) {
//				if (l instanceof AbstractDeepEditorElement) {
//					l.render(matrix, mouseX, mouseY);
//				}
//			}
//		}
//
//		for (AbstractEditorElement o : this.getFocusedObjects()) {
//			o.render(matrix, mouseX, mouseY);
//		}
//
//		super.render(matrix, mouseX, mouseY, partialTicks);
//
//		//Handle multiselect rightclick menu
//		if (this.multiselectRightclickMenu != null) {
//
//			if ((this.focusedObjects.size() > 1) && this.isFocusedHovered()) {
//				if (MouseInput.isRightMouseDown()) {
//					UIBase.openScaledContextMenuAtMouse(this.multiselectRightclickMenu);
//				}
//			}
//
//			if (!PopupHandler.isPopupActive()) {
//				UIBase.renderScaledContextMenu(matrix, this.multiselectRightclickMenu);
//			} else {
//				this.multiselectRightclickMenu.closeMenu();
//			}
//
//			if (MouseInput.isLeftMouseDown() && !this.multiselectRightclickMenu.isHovered()) {
//				this.multiselectRightclickMenu.closeMenu();
//			}
//
//		}
//
//		//Handle properties context menu
//		if (this.propertiesRightclickMenu != null) {
//
//			if (!this.isContentHovered() && MouseInput.isRightMouseDown()) {
//				UIBase.openScaledContextMenuAtMouse(this.propertiesRightclickMenu);
//			}
//
//			if (!PopupHandler.isPopupActive()) {
//				UIBase.renderScaledContextMenu(matrix, this.propertiesRightclickMenu);
//			} else {
//				this.propertiesRightclickMenu.closeMenu();
//			}
//
//			if (MouseInput.isLeftMouseDown() && !this.propertiesRightclickMenu.isHovered()) {
//				this.propertiesRightclickMenu.closeMenu();
//			}
//
//		}
//
//		this.handleActiveElementContextMenu();
//		if (this.activeElementContextMenu != null) {
//			UIBase.renderScaledContextMenu(matrix, this.activeElementContextMenu);
//		}
//
//		//Render the editor UI
//		this.ui.renderTopMenuBar(matrix, this);
//
//		//Needs to be done after other object render stuff to prevent ConcurrentModificationExceptions.
//		if (this.newContentMove != null) {
//			this.history.saveSnapshot(this.history.createSnapshot());
//			this.content = this.newContentMove;
//			this.newContentMove = null;
//		}
//		if (!this.newContentPaste.isEmpty()) {
//			this.content.addAll(this.newContentPaste);
//			this.newContentPaste.clear();
//		}
//		if (!this.deleteContentQueue.isEmpty()) {
//			this.history.saveSnapshot(this.history.createSnapshot());
//			for (AbstractEditorElement e : this.deleteContentQueue) {
//				if (e.isDestroyable()) {
//					this.removeContent(e);
//				}
//			}
//			this.deleteContentQueue.clear();
//		}
//
//		for (Runnable r : this.postRenderTasks) {
//			r.run();
//		}
//		this.postRenderTasks.clear();
//
//	}
//
//	protected void drawGrid(PoseStack matrix) {
//		if (FancyMenu.getConfig().getOrDefault("showgrid", false)) {
//
//			Color cNormal = new Color(255, 255, 255, 100);
//			Color cCenter = new Color(150, 105, 255, 100);
//			int gridSize = FancyMenu.getConfig().getOrDefault("gridsize", 10);
//			int lineThickness = 1;
//
//			//Draw centered vertical line
//			fill(matrix, (this.width / 2) - 1, 0, (this.width / 2) + 1, this.height, cCenter.getRGB());
//
//			//Draw vertical lines center -> left
//			int linesVerticalToLeftPosX = (this.width / 2) - gridSize - 1;
//			while (linesVerticalToLeftPosX > 0) {
//				int minY = 0;
//				int maxY = this.height;
//				int maxX = linesVerticalToLeftPosX + lineThickness;
//				fill(matrix, linesVerticalToLeftPosX, minY, maxX, maxY, cNormal.getRGB());
//				linesVerticalToLeftPosX -= gridSize;
//			}
//
//			//Draw vertical lines center -> right
//			int linesVerticalToRightPosX = (this.width / 2) + gridSize;
//			while (linesVerticalToRightPosX < this.width) {
//				int minY = 0;
//				int maxY = this.height;
//				int maxX = linesVerticalToRightPosX + lineThickness;
//				fill(matrix, linesVerticalToRightPosX, minY, maxX, maxY, cNormal.getRGB());
//				linesVerticalToRightPosX += gridSize;
//			}
//
//			//Draw centered horizontal line
//			fill(matrix, 0, (this.height / 2) - 1, this.width, (this.height / 2) + 1, cCenter.getRGB());
//
//			//Draw horizontal lines center -> top
//			int linesHorizontalToTopPosY = (this.height / 2) - gridSize - 1;
//			while (linesHorizontalToTopPosY > 0) {
//				int minX = 0;
//				int maxX = this.width;
//				int maxY = linesHorizontalToTopPosY + lineThickness;
//				fill(matrix, minX, linesHorizontalToTopPosY, maxX, maxY, cNormal.getRGB());
//				linesHorizontalToTopPosY -= gridSize;
//			}
//
//			//Draw horizontal lines center -> bottom
//			int linesHorizontalToBottomPosY = (this.height / 2) + gridSize;
//			while (linesHorizontalToBottomPosY < this.height) {
//				int minX = 0;
//				int maxX = this.width;
//				int maxY = linesHorizontalToBottomPosY + lineThickness;
//				fill(matrix, minX, linesHorizontalToBottomPosY, maxX, maxY, cNormal.getRGB());
//				linesHorizontalToBottomPosY += gridSize;
//			}
//
//		}
//	}
//
//	protected void renderVanillaButtons(PoseStack matrix, int mouseX, int mouseY) {
//		for (AbstractEditorElement l : this.vanillaButtonContent) {
//			if (!this.isHidden(l)) {
//				if (!this.isFocused(l)) {
//					l.render(matrix, mouseX, mouseY);
//				}
//			}
//		}
//	}
//
//	protected void renderCreatorBackground(PoseStack matrix) {
//		RenderSystem.enableBlend();
//		fill(matrix, 0, 0, this.width, this.height, new Color(38, 38, 38).getRGB());
//
//		if (this.backgroundTexture != null) {
//			RenderUtils.bindTexture(this.backgroundTexture.getResourceLocation());
//
//			if (!this.panorama) {
//				if (!this.keepBackgroundAspectRatio) {
//					blit(matrix, 0, 0, 1.0F, 1.0F, this.width + 1, this.height + 1, this.width + 1, this.height + 1);
//				} else {
//					int w = this.backgroundTexture.getWidth();
//					int h = this.backgroundTexture.getHeight();
//					double ratio = (double) w / (double) h;
//					int wFinal = (int)(this.height * ratio);
//					int screenCenterX = this.width / 2;
//					if (wFinal < this.width) {
//						blit(matrix, 0, 0, 1.0F, 1.0F, this.width + 1, this.height + 1, this.width + 1, this.height + 1);
//					} else {
//						blit(matrix, screenCenterX - (wFinal / 2), 0, 1.0F, 1.0F, wFinal + 1, this.height + 1, wFinal + 1, this.height + 1);
//					}
//				}
//			} else {
//				int w = this.backgroundTexture.getWidth();
//				int h = this.backgroundTexture.getHeight();
//				double ratio = (double) w / (double) h;
//				int wFinal = (int)(this.height * ratio);
//
//				//Check if the panorama background should move to the left side or to the right side
//				if ((panoPos + (wFinal - this.width)) <= 0) {
//					panoMoveBack = true;
//				}
//				if (panoPos >= 0) {
//					panoMoveBack = false;
//				}
//
//				//Fix pos after resizing
//				if (panoPos + (wFinal - this.width) < 0) {
//					panoPos = -(wFinal - this.width);
//				}
//				if (panoPos > 0) {
//					panoPos = 0;
//				}
//
//				if (!panoStop) {
//					if (panoTick >= 1) {
//						panoTick = 0;
//						if (panoMoveBack) {
//							panoPos = panoPos + 0.5;
//						} else {
//							panoPos = panoPos - 0.5;
//						}
//
//						if (panoPos + (wFinal - this.width) == 0) {
//							panoStop = true;
//						}
//						if (panoPos == 0) {
//							panoStop = true;
//						}
//					} else {
//						panoTick++;
//					}
//				} else {
//					if (panoTick >= 300) {
//						panoStop = false;
//						panoTick = 0;
//					} else {
//						panoTick++;
//					}
//				}
//				if (wFinal <= this.width) {
//					blit(matrix, 0, 0, 1.0F, 1.0F, this.width + 1, this.height + 1, this.width + 1, this.height + 1);
//				} else {
//					RenderUtils.doubleBlit(panoPos, 0, 1.0F, 1.0F, wFinal, this.height + 1);
//				}
//			}
//		}
//		RenderSystem.disableBlend();
//
//		if (this.backgroundAnimation != null) {
//			boolean b = this.backgroundAnimation.isStretchedToStreensize();
//			int wOri = this.backgroundAnimation.getWidth();
//			int hOri = this.backgroundAnimation.getHeight();
//			int xOri = this.backgroundAnimation.getPosX();
//			int yOri = this.backgroundAnimation.getPosY();
//			if (!this.keepBackgroundAspectRatio) {
//				this.backgroundAnimation.setStretchImageToScreensize(true);
//			} else {
//				double ratio = (double) wOri / (double) hOri;
//				int wfinal = (int)(this.height * ratio);
//				int screenCenterX = this.width / 2;
//				if (wfinal < this.width) {
//					this.backgroundAnimation.setStretchImageToScreensize(true);
//				} else {
//					this.backgroundAnimation.setWidth(wfinal + 1);
//					this.backgroundAnimation.setHeight(this.height + 1);
//					this.backgroundAnimation.setPosX(screenCenterX - (wfinal / 2));
//					this.backgroundAnimation.setPosY(0);
//				}
//			}
//			this.backgroundAnimation.render(matrix);
//			this.backgroundAnimation.setWidth(wOri);
//			this.backgroundAnimation.setHeight(hOri);
//			this.backgroundAnimation.setPosX(xOri);
//			this.backgroundAnimation.setPosY(yOri);
//			this.backgroundAnimation.setStretchImageToScreensize(b);
//		}
//
//		if (this.backgroundPanorama != null) {
//			this.backgroundPanorama.render(matrix);
//		}
//
//		if (this.backgroundSlideshow != null) {
//			int sw = this.backgroundSlideshow.width;
//			int sh = this.backgroundSlideshow.height;
//			int sx = this.backgroundSlideshow.x;
//			int sy = this.backgroundSlideshow.y;
//
//			if (!this.keepBackgroundAspectRatio) {
//				this.backgroundSlideshow.width = this.width + 1;
//				this.backgroundSlideshow.height = this.height +1;
//				this.backgroundSlideshow.x = 0;
//			} else {
//				double ratio = (double) sw / (double) sh;
//				int wfinal = (int)(this.height * ratio);
//				int screenCenterX = this.width / 2;
//				if (wfinal < this.width) {
//					this.backgroundSlideshow.width = this.width + 1;
//					this.backgroundSlideshow.height = this.height +1;
//					this.backgroundSlideshow.x = 0;
//				} else {
//					this.backgroundSlideshow.width = wfinal + 1;
//					this.backgroundSlideshow.height = this.height +1;
//					this.backgroundSlideshow.x = screenCenterX - (wfinal / 2);
//				}
//			}
//			this.backgroundSlideshow.y = 0;
//
//			this.backgroundSlideshow.render(matrix);
//
//			this.backgroundSlideshow.width = sw;
//			this.backgroundSlideshow.height = sh;
//			this.backgroundSlideshow.x = sx;
//			this.backgroundSlideshow.y = sy;
//		}
//
//		if (this.customMenuBackground != null) {
//			this.customMenuBackground.render(matrix, this, this.keepBackgroundAspectRatio);
//		}
//
//	}
//
//	public boolean isFocused(AbstractEditorElement object) {
//		if (PopupHandler.isPopupActive()) {
//			return false;
//		}
//		return (this.focusedObjects.contains(object));
//	}
//
//	public void setObjectFocused(AbstractEditorElement object, boolean focused, boolean ignoreBlockedFocusChange) {
//		if (this.isFocusChangeBlocked() && !ignoreBlockedFocusChange) {
//			return;
//		}
//		if (!this.content.contains(object)) {
//			return;
//		}
//		if (focused) {
//			if (!this.focusedObjects.contains(object)) {
//				this.focusedObjects.add(object);
//			}
//		} else {
//			this.focusedObjects.remove(object);
//		}
//	}
//
//	public boolean isObjectFocused() {
//		return (!this.focusedObjects.isEmpty());
//	}
//
//	public boolean isFocusedHovered() {
//		for (AbstractEditorElement o : this.focusedObjects) {
//			if (o.isHovered()) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	public boolean isFocusedDragged() {
//		for (AbstractEditorElement o : this.focusedObjects) {
//			if (o.isDragged()) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	public boolean isFocusedGrabberPressed() {
//		for (AbstractEditorElement o : this.focusedObjects) {
//			if (o.isGrabberPressed()) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	public boolean isFocusedGettingResized() {
//		for (AbstractEditorElement o : this.focusedObjects) {
//			if (o.isGettingResized()) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	/**
//	 * Returns a copy of the focused objects list.
//	 */
//	public List<AbstractEditorElement> getFocusedObjects() {
//		return new ArrayList<>(this.focusedObjects);
//	}
//
//	public void clearFocusedObjects() {
//		if (this.multiselectRightclickMenu != null) {
//			this.multiselectRightclickMenu.closeMenu();
//		}
//		this.focusedObjects.clear();
//	}
//
//	public boolean isContentHovered() {
//		for (AbstractEditorElement o : this.content) {
//			if (o.isHovered()) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	/**
//	 * Returns the LayoutObject the given object was moved above.
//	 */
//	public AbstractEditorElement moveUp(AbstractEditorElement o) {
//		AbstractEditorElement movedAbove = null;
//		try {
//			if (this.content.contains(o)) {
//				List<AbstractEditorElement> l = new ArrayList<AbstractEditorElement>();
//				int index = this.content.indexOf(o);
//				int i = 0;
//				if (index < this.content.size() - 1) {
//					for (AbstractEditorElement o2 : this.content) {
//						if (o2 != o) {
//							l.add(o2);
//							if (i == index+1) {
//								movedAbove = o2;
//								l.add(o);
//							}
//						}
//						i++;
//					}
//
//					this.newContentMove = l;
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return movedAbove;
//	}
//
//	/**
//	 * Returns the LayoutObject the object was moved behind.<br>
//	 * Will <b>NOT</b> move behind {@link LayoutVanillaButton}s, but will return the vanilla button the object would have been moved under.
//	 */
//	public AbstractEditorElement moveDown(AbstractEditorElement o) {
//		AbstractEditorElement movedBehind = null;
//		try {
//			if (this.content.contains(o)) {
//				List<AbstractEditorElement> l = new ArrayList<AbstractEditorElement>();
//				int index = this.content.indexOf(o);
//				int i = 0;
//				if (index > 0) {
//					for (AbstractEditorElement o2 : this.content) {
//						if (o2 != o) {
//							if (i == index-1) {
//								l.add(o);
//								movedBehind = o2;
//							}
//							l.add(o2);
//						}
//						i++;
//					}
//
//					if (!(movedBehind instanceof LayoutVanillaButton)) {
//						this.newContentMove = l;
//					}
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return movedBehind;
//	}
//
//	protected void setButtonTexturesForFocusedObjects() {
//		ScreenCustomizationLayer.ButtonCustomizationContainer cc = new ScreenCustomizationLayer.ButtonCustomizationContainer();
//		ButtonBackgroundPopup pop = new ButtonBackgroundPopup(this, cc, () -> {
//
//			this.history.saveSnapshot(this.history.createSnapshot());
//
//			for (AbstractEditorElement o : this.focusedObjectsCache) {
//				if (o instanceof LayoutVanillaButton) {
//
//					LayoutVanillaButton vb = (LayoutVanillaButton) o;
//					vb.customizationContainer.normalBackground = cc.normalBackground;
//					vb.customizationContainer.hoverBackground = cc.hoverBackground;
//					vb.customizationContainer.loopAnimation = cc.loopAnimation;
//					vb.customizationContainer.restartAnimationOnHover = cc.restartAnimationOnHover;
//
//				} else if (o instanceof LayoutButton) {
//
//					LayoutButton lb = (LayoutButton) o;
//					lb.customizationContainer.normalBackground = cc.normalBackground;
//					lb.customizationContainer.hoverBackground = cc.hoverBackground;
//					lb.customizationContainer.loopAnimation = cc.loopAnimation;
//					lb.customizationContainer.restartAnimationOnHover = cc.restartAnimationOnHover;
//
//				}
//			}
//
//		});
//		pop.saveSnapshots = false;
//		PopupHandler.displayPopup(pop);
//	}
//
//	protected void addTexture(String path) {
//		File home = Minecraft.getInstance().gameDirectory;
//		if (path == null) {
//			return;
//		}
//		if (path.startsWith(home.getAbsolutePath().replace("\\", "/"))) {
//			path = path.replace(home.getAbsolutePath().replace("\\", "/"), "");
//			if (path.startsWith("\\") || path.startsWith("/")) {
//				path = path.substring(1);
//			}
//		}
//		File f = new File(Minecraft.getInstance().gameDirectory, path);
//		String filename = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
//		if (f.exists()) {
//			if (filename.equals(f.getName())) {
//				this.history.saveSnapshot(this.history.createSnapshot());
//
//				PropertyContainer sec = new PropertyContainer("customization");
//				sec.putProperty("action", "addtexture");
//				sec.putProperty("path", path);
//				sec.putProperty("height", "100");
//				sec.putProperty("y", "" + (int)(this.ui.topMenuBar.getHeight() * UIBase.getUIScale()));
//
//				TextureCustomizationItem i = new TextureCustomizationItem(sec);
//				this.addContent(new LayoutTexture(i, this));
//
//			} else {
//				displayNotification(Locals.localize("helper.creator.textures.invalidcharacters"), "", "", "", "", "", "");
//			}
//		} else {
//			displayNotification("§c§l" + Locals.localize("helper.creator.invalidimage.title"), "", Locals.localize("helper.creator.invalidimage.desc"), "", "", "", "", "", "");
//		}
//	}
//
//	protected void addWebTexture(String url) {
//		this.history.saveSnapshot(this.history.createSnapshot());
//		PropertyContainer s = new PropertyContainer("customization");
//		s.putProperty("action", "addwebtexture");
//		s.putProperty("url", url);
//		s.putProperty("height", "100");
//		s.putProperty("width", "100");
//		s.putProperty("y", "" + (int)(this.ui.topMenuBar.getHeight() * UIBase.getUIScale()));
//		this.addContent(new LayoutWebTexture(new WebTextureCustomizationItem(s), this));
//	}
//
//	protected void addSlideshow(String name) {
//		if (name == null) {
//			return;
//		}
//		if (SlideshowHandler.slideshowExists(name)) {
//			this.history.saveSnapshot(this.history.createSnapshot());
//
//			PropertyContainer s = new PropertyContainer("customization");
//			s.putProperty("action", "addslideshow");
//			s.putProperty("name", name);
//			s.putProperty("y", "" + (int)(this.ui.topMenuBar.getHeight() * UIBase.getUIScale()));
//			SlideshowCustomizationItem i = new SlideshowCustomizationItem(s);
//			int w = SlideshowHandler.getSlideshow(name).width;
//			int h = SlideshowHandler.getSlideshow(name).height;
//			double ratio = (double) w / (double) h;
//			i.setHeight(100);
//			i.setWidth((int)(i.getHeight() * ratio));
//
//			this.addContent(new LayoutSlideshow(i, this));
//
//		} else {
//			displayNotification(Locals.localize("helper.creator.slideshownotfound"), "", "", "", "");
//		}
//	}
//
//	protected void addShape(Shape shape) {
//		PropertyContainer s = new PropertyContainer("customization");
//		s.putProperty("action", "addshape");
//		s.putProperty("shape", shape.name);
//		s.putProperty("width", "100");
//		s.putProperty("height", "100");
//		s.putProperty("y", "" + (int)(this.ui.topMenuBar.getHeight() * UIBase.getUIScale()));
//		this.history.saveSnapshot(this.history.createSnapshot());
//		this.addContent(new LayoutShape(new ShapeCustomizationItem(s), this));
//	}
//
//	protected void addMultiSplashText(String path) {
//		File home = Minecraft.getInstance().gameDirectory;
//		if (path == null) {
//			return;
//		}
//		if (path.startsWith(home.getAbsolutePath().replace("\\", "/"))) {
//			path = path.replace(home.getAbsolutePath().replace("\\", "/"), "");
//			if (path.startsWith("\\") || path.startsWith("/")) {
//				path = path.substring(1);
//			}
//		}
//		File f = new File(Minecraft.getInstance().gameDirectory, path);
//		if (f.exists() && f.getPath().toLowerCase().endsWith(".txt")) {
//
//			this.history.saveSnapshot(this.history.createSnapshot());
//
//			PropertyContainer sec = new PropertyContainer("customization");
//			sec.putProperty("action", "addsplash");
//			sec.putProperty("splashfilepath", path);
//			sec.putProperty("y", "" + (int)(this.ui.topMenuBar.getHeight() * UIBase.getUIScale()));
//
//			SplashTextCustomizationItem i = new SplashTextCustomizationItem(sec);
//			this.addContent(new LayoutSplashText(i, this));
//
//		} else {
//			displayNotification(Locals.localize("helper.creator.error.invalidfile"));
//		}
//	}
//
//	protected void addSingleSplashText(String content) {
//		if (content == null) {
//			return;
//		}
//		if (!content.replace(" ", "").equals("")) {
//
//			this.history.saveSnapshot(this.history.createSnapshot());
//
//			PropertyContainer sec = new PropertyContainer("customization");
//			sec.putProperty("action", "addsplash");
//			sec.putProperty("text", content);
//			sec.putProperty("y", "" + (int)(this.ui.topMenuBar.getHeight() * UIBase.getUIScale()));
//
//			SplashTextCustomizationItem i = new SplashTextCustomizationItem(sec);
//			this.addContent(new LayoutSplashText(i, this));
//
//		} else {
//			displayNotification("§c§l" + Locals.localize("helper.creator.texttooshort.title"), "", Locals.localize("helper.creator.texttooshort.desc"), "", "", "", "");
//		}
//	}
//
//	protected void addAnimation(String name) {
//		if (name == null) {
//			return;
//		}
//		if (AnimationHandler.animationExists(name)) {
//			this.history.saveSnapshot(this.history.createSnapshot());
//
//			PropertyContainer s = new PropertyContainer("customization");
//			s.putProperty("action", "addanimation");
//			s.putProperty("name", name);
//			s.putProperty("y", "" + (int)(this.ui.topMenuBar.getHeight() * UIBase.getUIScale()));
//			AnimationCustomizationItem i = new AnimationCustomizationItem(s);
//			int w = AnimationHandler.getAnimation(name).getWidth();
//			int h = AnimationHandler.getAnimation(name).getHeight();
//			double ratio = (double) w / (double) h;
//			i.setHeight(100);
//			i.setWidth((int)(i.getHeight() * ratio));
//			AnimationHandler.getAnimation(name).resetAnimation();
//			this.addContent(new LayoutAnimation(i, this));
//
//		} else {
//			displayNotification("§c§l" + Locals.localize("helper.creator.animationnotfound.title"), "", Locals.localize("helper.creator.animationnotfound.desc"), "", "", "");
//		}
//	}
//
//	protected void addButton(String label) {
//		if (label == null) {
//			return;
//		}
//		this.history.saveSnapshot(this.history.createSnapshot());
//
//		int w = 100;
//		if (Minecraft.getInstance().font.width(label) + 10 > w) {
//			w = Minecraft.getInstance().font.width(label) + 10;
//		}
//		LayoutButton b = new LayoutButton(new ScreenCustomizationLayer.ButtonCustomizationContainer(), w, 20, label, null, this);
//		b.element.baseY = (int)(this.ui.topMenuBar.getHeight() * UIBase.getUIScale());
//		this.addContent(b);
//	}
//
//	protected void addWebText(String url) {
//		this.history.saveSnapshot(this.history.createSnapshot());
//		PropertyContainer s = new PropertyContainer("customization");
//		s.putProperty("action", "addwebtext");
//		s.putProperty("url", url);
//		s.putProperty("y", "" + (int)(this.ui.topMenuBar.getHeight() * UIBase.getUIScale()));
//		this.addContent(new LayoutWebString(new WebStringCustomizationItem(s), this));
//	}
//
//	protected void addText(String text) {
//		if (text == null) {
//			return;
//		}
//		if (text.length() > 0) {
//			this.history.saveSnapshot(this.history.createSnapshot());
//
//			PropertyContainer s = new PropertyContainer("customization");
//			s.putProperty("action", "addtext");
//			s.putProperty("value", StringUtils.convertFormatCodes(text, "&", "§"));
//			s.putProperty("y", "" + (int)(this.ui.topMenuBar.getHeight() * UIBase.getUIScale()));
//			StringCustomizationItem i = new StringCustomizationItem(s);
//			this.addContent(new LayoutString(i, this));
//		} else {
//			displayNotification("§c§l" + Locals.localize("helper.creator.texttooshort.title"), "", Locals.localize("helper.creator.texttooshort.desc"), "", "", "", "");
//		}
//	}
//
//	protected void addAudio(String path) {
//		if (path != null) {
//			File home = Minecraft.getInstance().gameDirectory;
//			if (path.startsWith(home.getAbsolutePath().replace("\\", "/"))) {
//				path = path.replace(home.getAbsolutePath().replace("\\", "/"), "");
//				if (path.startsWith("\\") || path.startsWith("/")) {
//					path = path.substring(1);
//				}
//			}
//			File f = new File(Minecraft.getInstance().gameDirectory, path);
//			if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
//				if (!this.audio.containsKey(path)) {
//					this.history.saveSnapshot(this.history.createSnapshot());
//
//					SoundHandler.registerSound(path, path);
//					SoundHandler.playSound(path);
//					this.audio.put(path, false);
//				} else {
//					displayNotification("§c§l" + Locals.localize("helper.creator.audioalreadyloaded.title"), "", Locals.localize("helper.creator.audioalreadyloaded.desc"), "", "", "", "", "", "");
//				}
//
//			} else {
//				displayNotification("§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "", "");
//			}
//		}
//	}
//
//	public void setBackgroundAnimations(String... names) {
//		if ((names != null) && (names.length > 0)) {
//			for (String s : names) {
//				if (AnimationHandler.animationExists(s) && !this.backgroundAnimationNames.contains(s)) {
//					this.backgroundAnimationNames.add(s);
//				}
//			}
//			if (!this.backgroundAnimationNames.isEmpty()) {
//				this.history.saveSnapshot(this.history.createSnapshot());
//
//				this.backgroundPanorama = null;
//				this.backgroundSlideshow = null;
//				this.backgroundTexture = null;
//				this.customMenuBackground = null;
//				if (this.backgroundAnimation != null) {
//					((AdvancedAnimation)this.backgroundAnimation).stopAudio();
//				}
//				this.backgroundAnimation = AnimationHandler.getAnimation(this.backgroundAnimationNames.get(0));
//				this.backgroundAnimation.resetAnimation();
//			}
//		}
//		if (names == null) {
//			if (this.backgroundAnimation != null) {
//				this.history.saveSnapshot(this.history.createSnapshot());
//
//				((AdvancedAnimation)this.backgroundAnimation).stopAudio();
//			}
//			this.backgroundAnimation = null;
//			this.backgroundAnimationNames.clear();
//		}
//	}
//
//	public void setBackgroundTexture(String path) {
//		if (path != null) {
//			File home = Minecraft.getInstance().gameDirectory;
//			if (path.startsWith(home.getAbsolutePath().replace("\\", "/"))) {
//				path = path.replace(home.getAbsolutePath().replace("\\", "/"), "");
//				if (path.startsWith("\\") || path.startsWith("/")) {
//					path = path.substring(1);
//				}
//			}
//			File f = new File(Minecraft.getInstance().gameDirectory, path);
//			String filename = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
//			if (f.exists() && f.isFile() && (f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".jpeg") || f.getName().toLowerCase().endsWith(".png"))) {
//				if (filename.equals(f.getName())) {
//					this.history.saveSnapshot(this.history.createSnapshot());
//
//					this.backgroundTexture = ExternalTextureHandler.INSTANCE.getTexture(Minecraft.getInstance().gameDirectory.getPath() + "/" + path);
//					this.backgroundTexturePath = path;
//					if (this.backgroundAnimation != null) {
//						((AdvancedAnimation)this.backgroundAnimation).stopAudio();
//					}
//					this.backgroundAnimation = null;
//					this.backgroundAnimationNames.clear();
//
//					this.backgroundPanorama = null;
//					this.backgroundSlideshow = null;
//					this.customMenuBackground = null;
//
//				} else {
//					displayNotification(Locals.localize("helper.creator.textures.invalidcharacters"), "", "", "", "", "", "");
//				}
//			} else {
//				displayNotification("§c§l" + Locals.localize("helper.creator.invalidimage.title"), "", Locals.localize("helper.creator.invalidimage.desc"), "", "", "", "", "", "");
//			}
//		}
//	}
//
//	protected void deleteFocusedObjects() {
//		List<AbstractEditorElement> l = new ArrayList<>();
//		l.addAll(this.focusedObjects);
//
//		if (!l.isEmpty()) {
//			if (l.size() == 1) {
//				if (l.get(0).isDestroyable()) {
//					l.get(0).destroyElement();
//				} else {
//					displayNotification("§c§l" + Locals.localize("helper.creator.cannotdelete.title"), "", Locals.localize("helper.creator.cannotdelete.desc"), "", "", "");
//				}
//			}
//			if (l.size() > 1) {
//				if (FancyMenu.getConfig().getOrDefault("editordeleteconfirmation", true)) {
//					PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
//						if (call) {
//							this.deleteContentQueue.addAll(l);
//						}
//					}, "§c§l" + Locals.localize("helper.creator.messages.sure"), "", Locals.localize("helper.creator.deleteselectedobjects"), "", "", "", "", ""));
//				} else {
//					this.deleteContentQueue.addAll(l);
//				}
//			}
//		}
//	}
//
//	public Screen getScreenToCustomize() {
//		return this.screen;
//	}
//
//	public String getScreenToCustomizeIdentifier() {
//		if (!(this.screen instanceof CustomGuiBase)) {
//			return this.screen.getClass().getName();
//		} else {
//			return ((CustomGuiBase)this.screen).getIdentifier();
//		}
//	}
//
//	public boolean isFocusChangeBlocked() {
//		if (this.activeElementContextMenu != null) {
//			return true;
//		}
//		if ((this.propertiesRightclickMenu != null) && this.propertiesRightclickMenu.isOpen()) {
//			return true;
//		}
//		if ((this.multiselectRightclickMenu != null) && this.multiselectRightclickMenu.isOpen()) {
//			return true;
//		}
//		return false;
//	}
//
//	public AbstractEditorElement getTopHoverObject() {
//		return this.topObject;
//	}
//
//	public AbstractEditorElement getElementByInstanceIdentifier(String instanceIdentifier) {
//		for (AbstractEditorElement e : this.content) {
//			if (e instanceof LayoutVanillaButton) {
//				String id = "vanillabtn:" + ((LayoutVanillaButton)e).button.getId();
//				String compId = null;
//				if (((LayoutVanillaButton)e).button.getCompatibilityId() != null) {
//					compId = "vanillabtn:" + ((LayoutVanillaButton)e).button.getCompatibilityId();
//				}
//				if (id.equals(instanceIdentifier) || ((compId != null) && compId.equals(instanceIdentifier))) {
//					return e;
//				}
//			} else {
//				if (e.element.getInstanceIdentifier().equals(instanceIdentifier)) {
//					return e;
//				}
//			}
//		}
//		return null;
//	}
//
//	public void saveLayout() {
//
//		if ((this instanceof PreloadedLayoutEditorScreen) && (((PreloadedLayoutEditorScreen)this).single != null)) {
//
//			if (!LayoutHandler.saveLayoutToFile(this.getAllProperties(), ((PreloadedLayoutEditorScreen)this).single)) {
//				this.saveLayoutAs();
//			} else {
//				LayoutEditorHistory.Snapshot snap = this.history.createSnapshot();
//
//				List<PropertyContainerSet> l = new ArrayList<PropertyContainerSet>();
//				l.add(snap.snapshot);
//
//				PreloadedLayoutEditorScreen neweditor = new PreloadedLayoutEditorScreen(this.screen, l);
//				neweditor.history = this.history;
//				this.history.editor = neweditor;
//				neweditor.single = ((PreloadedLayoutEditorScreen)this).single;
//
//				Minecraft.getInstance().setScreen(neweditor);
//			}
//
//		} else {
//			this.saveLayoutAs();
//		}
//
//	}
//
//	public void saveLayoutAs() {
//		PopupHandler.displayPopup(new FMTextInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.editor.ui.layout.saveas.entername"), CharacterFilter.getBasicFilenameCharacterFilter(), 240, (call) -> {
//			try {
//
//				if ((call != null) && (call.length() > 0)) {
//
//					String file = FancyMenu.getCustomizationsDirectory().getAbsolutePath().replace("\\", "/")+ "/" + call + ".txt";
//					File f = new File(file);
//					if (!f.exists()) {
//						if (!LayoutHandler.saveLayoutToFile(this.getAllProperties(), file)) {
//							PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.editor.ui.layout.saveas.failed")));
//						} else {
//							LayoutEditorHistory.Snapshot snap = this.history.createSnapshot();
//
//							List<PropertyContainerSet> l = new ArrayList<PropertyContainerSet>();
//							l.add(snap.snapshot);
//
//							PreloadedLayoutEditorScreen neweditor = new PreloadedLayoutEditorScreen(this.screen, l);
//							neweditor.history = this.history;
//							this.history.editor = neweditor;
//							neweditor.single = file;
//
//							Minecraft.getInstance().setScreen(neweditor);
//						}
//					} else {
//						PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.editor.ui.layout.saveas.failed")));
//					}
//				} else {
//					PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.editor.ui.layout.saveas.failed")));
//				}
//
//			} catch (Exception e) {
//				e.printStackTrace();
//				PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.editor.ui.layout.saveas.failed")));
//			}
//		}));
//	}
//
//	public void copySelectedElements() {
//		List<AbstractEditorElement> l = this.getFocusedObjects();
//
//		LayoutEditorScreenOLD.COPIED_ELEMENT_CACHE.clear();
//		for (AbstractEditorElement e : l) {
//			LayoutEditorScreenOLD.COPIED_ELEMENT_CACHE.addAll(e.getProperties());
//		}
//	}
//
//	public void pasteElements() {
//		if (!LayoutEditorScreenOLD.COPIED_ELEMENT_CACHE.isEmpty()) {
//
//			PropertyContainerSet set = new PropertyContainerSet("menu");
//			for (PropertyContainer s : LayoutEditorScreenOLD.COPIED_ELEMENT_CACHE) {
//				set.putContainer(s);
//				if (s.hasProperty("actionid") && s.getType().equalsIgnoreCase("customization")) {
//					s.removeProperty("actionid");
//					s.putProperty("actionid", ScreenCustomization.generateUniqueIdentifier());
//				}
//			}
//
//			List<PropertyContainerSet> l = new ArrayList<PropertyContainerSet>();
//			l.add(set);
//
//			//Init dummy preloaded editor to use its customization action serializer for building the copied elements
//			PreloadedLayoutEditorScreen pe = new PreloadedLayoutEditorScreen(new CustomGuiBase("", "", false, null, null), l);
//			pe.init();
//
//			for (AbstractEditorElement e : pe.content) {
//
//				e.editor = this;
//				//Change the element position a bit to better see that the element was successfully pasted
//				e.element.baseX += 1;
//			}
//			this.history.saveSnapshot(this.history.createSnapshot());
//			this.newContentPaste.addAll(pe.content);
//
//			this.postRenderTasks.add(() -> {
//				LayoutEditorScreenOLD.this.init();
//				LayoutEditorScreenOLD.this.focusedObjects.clear();
//				LayoutEditorScreenOLD.this.focusedObjectsCache.clear();
//				LayoutEditorScreenOLD.this.focusedObjects.addAll(pe.content);
//			});
//
//		}
//	}
//
//	public boolean isUniversalLayout() {
//		if (this.screen instanceof CustomGuiBase) {
//			return (((CustomGuiBase) this.screen).getIdentifier().equals("%fancymenu:universal_layout%"));
//		}
//		return false;
//	}
//
//	protected void handleActiveElementContextMenu() {
//
//		if (this.activeElementContextMenu != null) {
//			//Close active element menu when mouse is clicked while menu is not hovered
//			if ((MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown()) && !this.activeElementContextMenu.isHovered()) {
//				this.activeElementContextMenu.closeMenu();
//				this.activeElementContextMenu = null;
//			}
//			//Force-reset active element menu if it's closed
//			if ((this.activeElementContextMenu != null) && !this.activeElementContextMenu.isOpen()) {
//				this.activeElementContextMenu = null;
//			}
//		}
//
//		if ((this.activeElementContextMenu == null) && MouseInput.isRightMouseDown()) {
//			//Search for potential element menu to open
//			for (AbstractEditorElement e : this.content) {
//				if ((e.rightClickContextMenu != null) && e.isRightClicked() && this.isFocused(e) && (this.getFocusedObjects().size() == 1)) {
//					this.activeElementContextMenu = e.rightClickContextMenu;
//					UIBase.openScaledContextMenuAtMouse(e.rightClickContextMenu);
//					e.hoveredLayers.clear();
//					for (AbstractEditorElement o : this.content) {
//						if (o.isHovered()) {
//							e.hoveredLayers.add(o);
//						}
//					}
//				}
//			}
//		}
//
//	}
//
//	public void resetActiveElementContextMenu() {
//		if (this.activeElementContextMenu != null) {
//			this.activeElementContextMenu.closeMenu();
//			this.activeElementContextMenu = null;
//		}
//	}
//
//	@Nullable
//	public ContextMenu getActiveElementContextMenu() {
//		return this.activeElementContextMenu;
//	}
//
//	protected static void onShortcutPressed(KeyboardData d) {
//		Screen c = Minecraft.getInstance().screen;
//		if (c instanceof LayoutEditorScreenOLD) {
//
//			//CTRL + C
//			if (d.keycode == 67) {
//				if (KeyboardHandler.isCtrlPressed()) {
//					if (!PopupHandler.isPopupActive()) {
//						((LayoutEditorScreenOLD) c).copySelectedElements();
//					}
//				}
//			}
//
//			//CTRL + V
//			if (d.keycode == 86) {
//				if (KeyboardHandler.isCtrlPressed()) {
//					if (!PopupHandler.isPopupActive()) {
//						((LayoutEditorScreenOLD) c).pasteElements();
//					}
//				}
//			}
//
//			//CTRL + S
//			if (d.keycode == 83) {
//				if (KeyboardHandler.isCtrlPressed()) {
//					if (!PopupHandler.isPopupActive()) {
//						((LayoutEditorScreenOLD) c).saveLayout();
//					}
//				}
//			}
//
//			//CTRL + Z
//			if (d.keycode == 89) {
//				if (KeyboardHandler.isCtrlPressed()) {
//					((LayoutEditorScreenOLD) c).history.stepBack();
//				}
//			}
//
//			//CTRL + Y
//			if (d.keycode == 90) {
//				if (KeyboardHandler.isCtrlPressed()) {
//					((LayoutEditorScreenOLD) c).history.stepForward();
//				}
//			}
//
//			//CTRL + G
//			if (d.keycode == 71) {
//				if (KeyboardHandler.isCtrlPressed()) {
//					try {
//						if (FancyMenu.getConfig().getOrDefault("showgrid", false)) {
//							FancyMenu.getConfig().setValue("showgrid", false);
//						} else {
//							FancyMenu.getConfig().setValue("showgrid", true);
//						}
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
//			}
//
//			//DEL
//			if (((LayoutEditorScreenOLD)c).isObjectFocused() && !PopupHandler.isPopupActive()) {
//				if (d.keycode == 261) {
//					((LayoutEditorScreenOLD) c).deleteFocusedObjects();
//				}
//			}
//
//		}
//	}
//
//	protected static void onArrowKeysPressed(KeyboardData d) {
//		Screen c = Minecraft.getInstance().screen;
//		if (c instanceof LayoutEditorScreenOLD) {
//			if (((LayoutEditorScreenOLD) c).isObjectFocused() && !PopupHandler.isPopupActive()) {
//
//				if (!((d.keycode == 263) || (d.keycode == 262) || (d.keycode == 265) || (d.keycode == 264))) {
//					return;
//				}
//
//				LayoutEditorHistory.Snapshot snap = ((LayoutEditorScreenOLD) c).history.createSnapshot();
//				boolean saveSnap = false;
//
//				for (AbstractEditorElement o : ((LayoutEditorScreenOLD) c).focusedObjects) {
//					if ((o instanceof LayoutVanillaButton) && o.element.anchorPoint.equals("original")) {
//						((LayoutVanillaButton)o).displaySetOrientationNotification();
//						continue;
//					}
//					if (d.keycode == 263) {
//						saveSnap = true;
//
//						o.setRawX(o.element.baseX - 1);
//					}
//					if (d.keycode == 262) {
//						saveSnap = true;
//
//						o.setRawX(o.element.baseX + 1);
//					}
//					if (d.keycode == 265) {
//						saveSnap = true;
//
//						o.setRawY(o.element.baseY - 1);
//					}
//					if (d.keycode == 264) {
//						saveSnap = true;
//
//						o.setRawY(o.element.baseY + 1);
//					}
//				}
//
//				if (saveSnap) {
//					((LayoutEditorScreenOLD) c).history.saveSnapshot(snap);
//				}
//
//			}
//		}
//	}
//
//	public static void displayNotification(String... strings) {
//		PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, strings));
//	}
//
//}

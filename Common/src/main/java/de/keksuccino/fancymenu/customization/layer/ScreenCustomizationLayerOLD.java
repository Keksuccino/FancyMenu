//package de.keksuccino.fancymenu.customization.layer;
//
//import com.mojang.blaze3d.platform.Window;
//import com.mojang.blaze3d.systems.RenderSystem;
//import com.mojang.blaze3d.vertex.PoseStack;
//import de.keksuccino.fancymenu.audio.SoundRegistry;
//import de.keksuccino.fancymenu.customization.ScreenCustomization;
//import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
//import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
//import de.keksuccino.fancymenu.customization.background.MenuBackground;
//import de.keksuccino.fancymenu.customization.button.ButtonCache;
//import de.keksuccino.fancymenu.customization.button.ButtonData;
//import de.keksuccino.fancymenu.customization.element.AbstractElement;
//import de.keksuccino.fancymenu.customization.element.ElementBuilder;
//import de.keksuccino.fancymenu.customization.element.ElementRegistry;
//import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
//import de.keksuccino.fancymenu.customization.element.v1.*;
//import de.keksuccino.fancymenu.customization.element.v1.button.ButtonCustomizationItem;
//import de.keksuccino.fancymenu.customization.element.v1.button.VanillaButtonCustomizationItem;
//import de.keksuccino.fancymenu.customization.gameintro.GameIntroHandler;
//import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
//import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
//import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
//import de.keksuccino.fancymenu.customization.panorama.ExternalTexturePanoramaRenderer;
//import de.keksuccino.fancymenu.customization.placeholder.v2.PlaceholderParser;
//import de.keksuccino.fancymenu.customization.slideshow.ExternalTextureSlideshowRenderer;
//import de.keksuccino.fancymenu.event.acara.EventHandler;
//import de.keksuccino.fancymenu.event.acara.EventListener;
//import de.keksuccino.fancymenu.event.acara.EventPriority;
//import de.keksuccino.fancymenu.event.events.ButtonCacheUpdatedEvent;
//import de.keksuccino.fancymenu.event.events.ModReloadEvent;
//import de.keksuccino.fancymenu.event.events.ScreenReloadEvent;
//import de.keksuccino.fancymenu.event.events.screen.CloseScreenEvent;
//import de.keksuccino.fancymenu.event.events.screen.InitOrResizeScreenEvent;
//import de.keksuccino.fancymenu.event.events.screen.RenderScreenEvent;
//import de.keksuccino.fancymenu.event.events.screen.RenderedScreenBackgroundEvent;
//import de.keksuccino.fancymenu.event.events.widget.PlayWidgetClickSoundEvent;
//import de.keksuccino.fancymenu.event.events.widget.RenderGuiListBackgroundEvent;
//import de.keksuccino.fancymenu.event.events.widget.RenderWidgetBackgroundEvent;
//import de.keksuccino.fancymenu.rendering.texture.ExternalTextureHandler;
//import de.keksuccino.fancymenu.utils.ScreenTitleUtils;
//import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
//import de.keksuccino.konkrete.input.MouseInput;
//import de.keksuccino.konkrete.math.MathUtils;
//import de.keksuccino.fancymenu.properties.PropertyContainer;
//import de.keksuccino.fancymenu.properties.PropertyContainerSet;
//import de.keksuccino.konkrete.rendering.RenderUtils;
//import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
//import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
//import de.keksuccino.konkrete.sound.SoundHandler;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.GuiComponent;
//import net.minecraft.client.gui.components.AbstractWidget;
//import net.minecraft.client.gui.components.ImageButton;
//import net.minecraft.client.gui.screens.Screen;
//import net.minecraft.client.resources.language.I18n;
//import net.minecraft.network.chat.Component;
//import net.minecraft.util.Mth;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicBoolean;
//
////TODO komplett rewriten und neues Layout element nutzen + LayoutHandler
//
//public class ScreenCustomizationLayerOLD extends GuiComponent {
//
//	private static final Logger LOGGER = LogManager.getLogger();
//
//	public List<AbstractElement> foregroundElements = new ArrayList<>();
//	public List<AbstractElement> backgroundElements = new ArrayList<>();
//
//	protected Map<String, Boolean> audio = new HashMap<>();
//	protected IAnimationRenderer backgroundAnimation = null;
//	protected IAnimationRenderer lastBackgroundAnimation = null;
//	protected List<IAnimationRenderer> backgroundAnimations = new ArrayList<>();
//	protected int backgroundAnimationId = 0;
//	protected ExternalTextureResourceLocation backgroundTexture = null;
//	protected String identifier;
//	protected boolean backgroundDrawable;
//	protected boolean panoramaback = false;
//	protected int panoTick = 0;
//	protected double panoPos = 0.0;
//	protected boolean panoMoveBack = false;
//	protected boolean panoStop = false;
//	protected boolean keepBackgroundAspectRatio = false;
//	protected String customMenuTitle = null;
//	protected boolean forceDisableCustomMenuTitle = false;
//
//	protected ExternalTexturePanoramaRenderer panoramaCube;
//
//	protected ExternalTextureSlideshowRenderer slideshow;
//
//	protected MenuBackground customMenuBackground = null;
//	public float backgroundOpacity = 1.0F;
//
//	protected List<ButtonData> hidden = new ArrayList<>();
//	protected Map<AbstractWidget, ButtonCustomizationContainer> vanillaButtonCustomizations = new HashMap<>();
//	protected Map<AbstractWidget, LoadingRequirementContainer> vanillaButtonLoadingRequirementContainers = new HashMap<>();
//
//	protected volatile Map<ButtonData, Float> delayAppearanceVanilla = new HashMap<>();
//	protected Map<ButtonData, Float> fadeInVanilla = new HashMap<>();
//	protected List<String> delayAppearanceFirstTime = new ArrayList<>();
//	protected List<Long> delayAppearanceFirstTimeVanilla = new ArrayList<>();
//	protected List<ThreadCaller> delayThreads = new ArrayList<>();
//
//	protected boolean preInitialized = false;
//
//	protected Map<String, RandomLayoutContainer> randomLayoutGroups = new HashMap<>();
//	protected List<PropertyContainerSet> normalLayouts = new ArrayList<>();
//	protected SharedLayoutProperties sharedLayoutProps = new SharedLayoutProperties();
//
//	protected String closeAudio;
//	protected String openAudio;
//
//	protected Map<LoadingRequirementContainer, Boolean> cachedLayoutWideLoadingRequirements = new HashMap<>();
//
//	/**
//	 * @param identifier Has to be the full class path of the menu screen.
//	 */
//	public ScreenCustomizationLayerOLD(@NotNull String identifier) {
//		this.identifier = identifier;
//		EventHandler.INSTANCE.registerListenersOf(this);
//	}
//
//	public String getIdentifier() {
//		return this.identifier;
//	}
//
//	@EventListener
//	public void onSoftReload(ScreenReloadEvent e) {
//		if (this.shouldCustomize(e.getScreen())) {
//			this.delayAppearanceFirstTimeVanilla.clear();
//			this.delayAppearanceFirstTime.clear();
//			this.delayAppearanceVanilla.clear();
//			this.fadeInVanilla.clear();
//			for (RandomLayoutContainer c : this.randomLayoutGroups.values()) {
//				c.lastLayoutPath = null;
//			}
//
//			if (this.lastBackgroundAnimation != null) {
//				this.lastBackgroundAnimation.resetAnimation();
//			}
//		}
//	}
//
//	@EventListener
//	public void onMenuReloaded(ModReloadEvent e) {
//		this.delayAppearanceFirstTimeVanilla.clear();
//		this.delayAppearanceFirstTime.clear();
//		this.delayAppearanceVanilla.clear();
//		this.fadeInVanilla.clear();
//		for (RandomLayoutContainer c : this.randomLayoutGroups.values()) {
//			c.lastLayoutPath = null;
//		}
//
//		if (this.lastBackgroundAnimation != null) {
//			this.lastBackgroundAnimation.resetAnimation();
//		}
//	}
//
//	@EventListener
//	public void onCloseScreen(CloseScreenEvent e) {
//
//		if (!this.shouldCustomize(e.getScreen())) return;
//
//		if (this.closeAudio != null) {
//			SoundHandler.resetSound(this.closeAudio);
//			SoundHandler.playSound(this.closeAudio);
//		}
//
//	}
//
//	@EventListener
//	public void onInitPre(InitOrResizeScreenEvent.Pre e) {
//
//		for (ThreadCaller t : this.delayThreads) {
//			t.running.set(false);
//		}
//		this.delayThreads.clear();
//
//		if (!this.shouldCustomize(e.getScreen())) {
//			return;
//		}
//		if (!AnimationHandler.isReady()) {
//			return;
//		}
//		if (!GameIntroHandler.introDisplayed) {
//			return;
//		}
//		if (LayoutEditorScreen.isActive) {
//			return;
//		}
//		if (ButtonCache.isCaching()) {
//			return;
//		}
//		if (!ScreenCustomization.isCustomizationEnabledForScreen(e.getScreen())) {
//			return;
//		}
//
//		preInitialized = true;
//
//		List<PropertyContainerSet> rawLayouts = LayoutHandler.getEnabledLayoutsForMenuIdentifier(this.getIdentifier());
//		String defaultGroup = "-100397";
//
//		this.normalLayouts.clear();
//
//		for (RandomLayoutContainer c : this.randomLayoutGroups.values()) {
//			c.onlyFirstTime = false;
//			c.clearLayouts();
//		}
//
//		this.sharedLayoutProps = new SharedLayoutProperties();
//
//		this.customMenuTitle = null;
//
//		this.cachedLayoutWideLoadingRequirements.clear();
//
//		for (PropertyContainerSet s : rawLayouts) {
//
//			List<PropertyContainer> metas = s.getSectionsOfType("customization-meta");
//			if (metas.isEmpty()) {
//				metas = s.getSectionsOfType("type-meta");
//			}
//			if (metas.isEmpty()) {
//				continue;
//			}
//
//			LoadingRequirementContainer layoutWideRequirementContainer = LoadingRequirementContainer.deserializeRequirementContainer(metas.get(0));
//			this.cachedLayoutWideLoadingRequirements.put(layoutWideRequirementContainer, layoutWideRequirementContainer.requirementsMet());
//			if (!layoutWideRequirementContainer.requirementsMet()) {
//				continue;
//			}
//
//			if (!this.forceDisableCustomMenuTitle) {
//				String cusMenuTitle = metas.get(0).getValue("custom_menu_title");
//				if (cusMenuTitle != null) {
//					this.customMenuTitle = cusMenuTitle;
//					ScreenTitleUtils.setScreenTitle(e.getScreen(), Component.literal(PlaceholderParser.replacePlaceholders(cusMenuTitle)));
//				}
//			}
//
//			String biggerthanwidth = metas.get(0).getValue("biggerthanwidth");
//			if (biggerthanwidth != null) {
//				biggerthanwidth = biggerthanwidth.replace(" ", "");
//				if (MathUtils.isInteger(biggerthanwidth)) {
//					int i = Integer.parseInt(biggerthanwidth);
//					if (WindowHandler.getWindowGuiWidth() < i) {
//						continue;
//					}
//				}
//			}
//			String biggerthanheight = metas.get(0).getValue("biggerthanheight");
//			if (biggerthanheight != null) {
//				biggerthanheight = biggerthanheight.replace(" ", "");
//				if (MathUtils.isInteger(biggerthanheight)) {
//					int i = Integer.parseInt(biggerthanheight);
//					if (WindowHandler.getWindowGuiHeight() < i) {
//						continue;
//					}
//				}
//			}
//			String smallerthanwidth = metas.get(0).getValue("smallerthanwidth");
//			if (smallerthanwidth != null) {
//				smallerthanwidth = smallerthanwidth.replace(" ", "");
//				if (MathUtils.isInteger(smallerthanwidth)) {
//					int i = Integer.parseInt(smallerthanwidth);
//					if (WindowHandler.getWindowGuiWidth() > i) {
//						continue;
//					}
//				}
//			}
//			String smallerthanheight = metas.get(0).getValue("smallerthanheight");
//			if (smallerthanheight != null) {
//				smallerthanheight = smallerthanheight.replace(" ", "");
//				if (MathUtils.isInteger(smallerthanheight)) {
//					int i = Integer.parseInt(smallerthanheight);
//					if (WindowHandler.getWindowGuiHeight() > i) {
//						continue;
//					}
//				}
//			}
//
//			String randomMode = metas.get(0).getValue("randommode");
//			if ((randomMode != null) && randomMode.equalsIgnoreCase("true")) {
//
//				String group = metas.get(0).getValue("randomgroup");
//				if (group == null) {
//					group = defaultGroup;
//				}
//				if (!this.randomLayoutGroups.containsKey(group)) {
//					this.randomLayoutGroups.put(group, new RandomLayoutContainer(group, this));
//				}
//				RandomLayoutContainer c = this.randomLayoutGroups.get(group);
//				if (c != null) {
//					String randomOnlyFirstTime = metas.get(0).getValue("randomonlyfirsttime");
//					if ((randomOnlyFirstTime != null) && randomOnlyFirstTime.equalsIgnoreCase("true")) {
//						c.setOnlyFirstTime(true);
//					}
//					c.addLayout(s);
//				}
//
//			} else {
//
//				this.normalLayouts.add(s);
//
//			}
//
//		}
//
//		List<String> trashLayoutGroups = new ArrayList<String>();
//		for (Map.Entry<String, RandomLayoutContainer> m : this.randomLayoutGroups.entrySet()) {
//			if (m.getValue().getLayouts().isEmpty()) {
//				trashLayoutGroups.add(m.getKey());
//			}
//		}
//		for (String s : trashLayoutGroups) {
//			this.randomLayoutGroups.remove(s);
//		}
//
//		//Applying customizations which needs to be done before other ones
//		for (PropertyContainerSet s : this.normalLayouts) {
//			for (PropertyContainer sec : s.getSectionsOfType("customization")) {
//				this.applyLayoutPre(sec, e);
//			}
//		}
//		for (RandomLayoutContainer c : this.randomLayoutGroups.values()) {
//			PropertyContainerSet s = c.getRandomLayout();
//			if (s != null) {
//				for (PropertyContainer sec : s.getSectionsOfType("customization")) {
//					this.applyLayoutPre(sec, e);
//				}
//			}
//		}
//
//		//Handle auto scaling
//		if ((this.sharedLayoutProps.autoScaleBaseWidth != 0) && (this.sharedLayoutProps.autoScaleBaseHeight != 0)) {
//			Window m = Minecraft.getInstance().getWindow();
//			double guiWidth = e.getScreen().width * m.getGuiScale();
//			double guiHeight = e.getScreen().height * m.getGuiScale();
//			double percentX = (guiWidth / (double)this.sharedLayoutProps.autoScaleBaseWidth) * 100.0D;
//			double percentY = (guiHeight / (double)this.sharedLayoutProps.autoScaleBaseHeight) * 100.0D;
//			double newScaleX = (percentX / 100.0D) * m.getGuiScale();
//			double newScaleY = (percentY / 100.0D) * m.getGuiScale();
//			double newScale = Math.min(newScaleX, newScaleY);
//
//			m.setGuiScale(newScale);
//			e.getScreen().width = m.getGuiScaledWidth();
//			e.getScreen().height = m.getGuiScaledHeight();
//			this.sharedLayoutProps.scaled = true;
//		}
//
//	}
//
//
//	protected void applyLayoutPre(PropertyContainer sec, InitOrResizeScreenEvent.Pre e) {
//
//		String action = sec.getValue("action");
//		if (action != null) {
//			String identifier = sec.getValue("identifier");
//
//			if (action.equalsIgnoreCase("overridemenu")) {
//				if ((identifier != null) && CustomGuiLoader.guiExists(identifier)) {
//					CustomGuiBase cus = CustomGuiLoader.getGui(identifier, null, e.getScreen(), (onClose) -> {
//						e.getScreen().removed();
//					});
//					Minecraft.getInstance().setScreen(cus);
//				}
//			}
//
//			if (action.contentEquals("setscale")) {
//				String scale = sec.getValue("scale");
//				if ((scale != null) && (MathUtils.isInteger(scale.replace(" ", "")) || MathUtils.isDouble(scale.replace(" ", "")))) {
//					int newscale = (int) Double.parseDouble(scale.replace(" ", ""));
//					if (newscale <= 0) {
//						newscale = 1;
//					}
//					Window m = Minecraft.getInstance().getWindow();
//					m.setGuiScale(newscale);
//					e.getScreen().width = m.getGuiScaledWidth();
//					e.getScreen().height = m.getGuiScaledHeight();
//					this.sharedLayoutProps.scaled = true;
//				}
//			}
//
//			if (action.equalsIgnoreCase("autoscale")) {
//				String baseWidth = sec.getValue("basewidth");
//				if (MathUtils.isInteger(baseWidth)) {
//					this.sharedLayoutProps.autoScaleBaseWidth = Integer.parseInt(baseWidth);
//				}
//				String baseHeight = sec.getValue("baseheight");
//				if (MathUtils.isInteger(baseHeight)) {
//					this.sharedLayoutProps.autoScaleBaseHeight = Integer.parseInt(baseHeight);
//				}
//			}
//		}
//
//	}
//
//	@EventListener
//	public void onButtonsCached(ButtonCacheUpdatedEvent e) {
//
//		if (e.getScreen() != Minecraft.getInstance().screen) {
//			return;
//		}
//		if (!this.shouldCustomize(e.getScreen())) {
//			return;
//		}
//		if (!AnimationHandler.isReady()) {
//			return;
//		}
//		if (!GameIntroHandler.introDisplayed) {
//			return;
//		}
//		if (LayoutEditorScreen.isActive) {
//			return;
//		}
//		if (ButtonCache.isCaching()) {
//			return;
//		}
//		if (!ScreenCustomization.isCustomizationEnabledForScreen(e.getScreen())) {
//			return;
//		}
//
//		if (!this.preInitialized) {
//			System.out.println("################ WARNING [FANCYMENU] ################");
//			System.out.println("MenuHandler pre-init skipped! Trying to re-initialize menu!");
//			System.out.println("Menu Type: " + e.getScreen().getClass().getName());
//			System.out.println("Menu Handler: " + this.getClass().getName());
//			System.out.println("This probably happened because a mod has overridden a menu with this one.");
//			System.out.println("#####################################################");
//			e.getScreen().resize(Minecraft.getInstance(), e.getScreen().width, e.getScreen().height);
//			return;
//		}
//
//		this.hidden.clear();
//		this.delayAppearanceVanilla.clear();
//		this.fadeInVanilla.clear();
//		this.vanillaButtonCustomizations.clear();
//		this.vanillaButtonLoadingRequirementContainers.clear();
//		this.audio.clear();
//		this.foregroundElements.clear();
//		this.backgroundElements.clear();
//		this.panoramaCube = null;
//		this.slideshow = null;
//		this.customMenuBackground = null;
//
//		this.backgroundOpacity = 1.0F;
//		this.backgroundAnimation = null;
//		this.backgroundAnimations.clear();
//		if ((this.backgroundAnimation != null) && (this.backgroundAnimation instanceof AdvancedAnimation)) {
//			((AdvancedAnimation)this.backgroundAnimation).stopAudio();
//		}
//		this.backgroundDrawable = false;
//
//		for (PropertyContainerSet s : this.normalLayouts) {
//			List<PropertyContainer> metas = s.getSectionsOfType("customization-meta");
//			if (metas.isEmpty()) {
//				metas = s.getSectionsOfType("type-meta");
//			}
////			String renderOrder = metas.get(0).getEntryValue("renderorder");
//			for (PropertyContainer sec : s.getSectionsOfType("customization")) {
//				this.applyLayout(sec, renderOrder, e);
//			}
//		}
//		for (RandomLayoutContainer c : this.randomLayoutGroups.values()) {
//			PropertyContainerSet s = c.getRandomLayout();
//			if (s != null) {
//				List<PropertyContainer> metas = s.getSectionsOfType("customization-meta");
//				if (metas.isEmpty()) {
//					metas = s.getSectionsOfType("type-meta");
//				}
////				String renderOrder = metas.get(0).getEntryValue("renderorder");
//				for (PropertyContainer sec : s.getSectionsOfType("customization")) {
//					this.applyLayout(sec, renderOrder, e);
//				}
//			}
//		}
//
//		for (String s : SoundRegistry.getSounds()) {
//			if (!this.audio.containsKey(s) && !s.equals(this.openAudio) && !s.equals(this.closeAudio)) {
//				SoundHandler.stopSound(s);
//				SoundHandler.resetSound(s);
//			}
//		}
//
//		if (!this.sharedLayoutProps.closeAudioSet && (this.closeAudio != null)) {
//			SoundRegistry.unregisterSound(this.closeAudio);
//			this.closeAudio = null;
//		}
//
//		if (!this.sharedLayoutProps.openAudioSet && (this.openAudio != null)) {
//			SoundRegistry.unregisterSound(this.openAudio);
//			this.openAudio = null;
//		}
//
//		for (Map.Entry<String, Boolean> m : this.audio.entrySet()) {
//			SoundHandler.playSound(m.getKey());
//			if (m.getValue()) {
//				SoundHandler.setLooped(m.getKey(), true);
//			}
//		}
//
//		if (!this.sharedLayoutProps.backgroundTextureSet) {
//			this.backgroundTexture = null;
//		}
//
//		for (ButtonData d : this.hidden) {
//			d.getButton().visible = false;
//		}
//
//		for (AbstractElement i : this.foregroundElements) {
//			if (ScreenCustomization.isNewMenu()) {
//				this.handleAppearanceDelayFor(i);
//			}
//		}
//		for (AbstractElement i : this.backgroundElements) {
//			if (ScreenCustomization.isNewMenu()) {
//				this.handleAppearanceDelayFor(i);
//			}
//		}
//
//		//Handle vanilla button visibility requirements
//		for (Map.Entry<AbstractWidget, LoadingRequirementContainer> m : this.vanillaButtonLoadingRequirementContainers.entrySet()) {
//			boolean isBtnHidden = false;
//			for (ButtonData d : this.hidden) {
//				if (d.getButton() == m.getKey()) {
//					isBtnHidden = true;
//					break;
//				}
//			}
//			if (!isBtnHidden) {
//				PropertyContainer dummySec = new PropertyContainer("customization");
//				dummySec.putProperty("action", "vanilla_button_visibility_requirements");
//				ButtonData btn = null;
//				for (ButtonData d : ButtonCache.getButtons()) {
//					if (d.getButton() == m.getKey()) {
//						btn = d;
//						break;
//					}
//				}
//				if (btn != null) {
//					VanillaButtonCustomizationItem i = new VanillaButtonCustomizationItem(dummySec, btn, this);
//					i.loadingRequirements = m.getValue();
//					this.backgroundElements.add(i);
//				}
//			}
//		}
//
//		for (Map.Entry<ButtonData, Float> m : this.delayAppearanceVanilla.entrySet()) {
//			if (!hidden.contains(m.getKey())) {
//				if (this.vanillaButtonLoadingRequirementsMet(m.getKey().getButton())) {
//					this.handleVanillaAppearanceDelayFor(m.getKey());
//				}
//			}
//		}
//
//	}
//
//	protected void applyLayout(PropertyContainer sec, String renderOrder, ButtonCacheUpdatedEvent e) {
//
//		String action = sec.getValue("action");
//		if (action != null) {
//			String identifier = sec.getValue("identifier");
//			AbstractWidget b = null;
//			ButtonData bd = null;
//			if (identifier != null) {
//				bd = getButton(identifier);
//				if (bd != null) {
//					b = bd.getButton();
//				}
//			}
//
//			if (action.equalsIgnoreCase("backgroundoptions")) {
//				String keepAspect = sec.getValue("keepaspectratio");
//				if ((keepAspect != null) && keepAspect.equalsIgnoreCase("true")) {
//					this.keepBackgroundAspectRatio = true;
//				}
//			}
//
//			if (action.equalsIgnoreCase("setbackgroundslideshow")) {
//				String name = sec.getValue("name");
//				if (name != null) {
//					if (SlideshowHandler.slideshowExists(name)) {
//						this.slideshow = SlideshowHandler.getSlideshow(name);
//					}
//				}
//			}
//
//			if (action.equalsIgnoreCase("setbackgroundpanorama")) {
//				String name = sec.getValue("name");
//				if (name != null) {
//					if (PanoramaHandler.panoramaExists(name)) {
//						this.panoramaCube = PanoramaHandler.getPanorama(name);
//					}
//				}
//			}
//
//			if (action.equalsIgnoreCase("texturizebackground")) {
//				String value = AbstractElement.fixBackslashPath(sec.getValue("path"));
//				String pano = sec.getValue("wideformat");
//				if (pano == null) {
//					pano = sec.getValue("panorama");
//				}
//				if (value != null) {
//					File f = new File(value.replace("\\", "/"));
//					if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
//						value = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + value.replace("\\", "/");
//						f = new File(value);
//					}
//					if (f.exists() && f.isFile() && (f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".jpeg") || f.getName().toLowerCase().endsWith(".png"))) {
//						if ((this.backgroundTexture == null) || !this.backgroundTexture.getPath().equals(value)) {
//							this.backgroundTexture = ExternalTextureHandler.INSTANCE.getTexture(value);
//						}
//						this.panoramaback = (pano != null) && pano.equalsIgnoreCase("true");
//						this.sharedLayoutProps.backgroundTextureSet = true;
//					}
//				}
//			}
//
//			if (action.equalsIgnoreCase("animatebackground")) {
//				String value = sec.getValue("name");
//				String random = sec.getValue("random");
//				boolean ran = (random != null) && random.equalsIgnoreCase("true");
//				boolean restartOnLoad = false;
//				String restartOnLoadString = sec.getValue("restart_on_load");
//				if ((restartOnLoadString != null) && restartOnLoadString.equalsIgnoreCase("true")) {
//					restartOnLoad = true;
//				}
//				if (value != null) {
//					if (value.contains(",")) {
//						for (String s2 : value.split(",")) {
//							int i = 0;
//							for (char c : s2.toCharArray()) {
//								if (c != ' ') {
//									break;
//								}
//								i++;
//							}
//							if (i > s2.length()) {
//								continue;
//							}
//							String temp = new StringBuilder(s2.substring(i)).reverse().toString();
//							int i2 = 0;
//							for (char c : temp.toCharArray()) {
//								if (c != ' ') {
//									break;
//								}
//								i2++;
//							}
//							String name = new StringBuilder(temp.substring(i2)).reverse().toString();
//							if (AnimationHandler.animationExists(name)) {
//								this.backgroundAnimations.add(AnimationHandler.getAnimation(name));
//							}
//						}
//					} else {
//						if (AnimationHandler.animationExists(value)) {
//							this.backgroundAnimations.add(AnimationHandler.getAnimation(value));
//						}
//					}
//
//					if (!this.backgroundAnimations.isEmpty()) {
//						if (restartOnLoad && ScreenCustomization.isNewMenu()) {
//							for (IAnimationRenderer r : this.backgroundAnimations) {
//								r.resetAnimation();
//							}
//						}
//						if (ran) {
//							if (ScreenCustomization.isNewMenu()) {
//								this.backgroundAnimationId = MathUtils.getRandomNumberInRange(0, this.backgroundAnimations.size()-1);
//							}
//							this.backgroundAnimation = this.backgroundAnimations.get(this.backgroundAnimationId);
//						} else {
//							if ((this.lastBackgroundAnimation != null) && this.backgroundAnimations.contains(this.lastBackgroundAnimation)) {
//								this.backgroundAnimation = this.lastBackgroundAnimation;
//							} else {
//								this.backgroundAnimationId = 0;
//								this.backgroundAnimation = this.backgroundAnimations.get(0);
//							}
//							this.lastBackgroundAnimation = this.backgroundAnimation;
//						}
//					}
//				}
//			}
//
//			//Custom background handling (API)
//			if (action.equalsIgnoreCase("api:custombackground")) {
//				String typeId = sec.getValue("type_identifier");
//				String backId = sec.getValue("background_identifier");
//				String inputString = sec.getValue("input_string");
//				if (typeId != null) {
//					MenuBackgroundType type = MenuBackgroundTypeRegistry.getBackgroundTypeByIdentifier(typeId);
//					if (type != null) {
//						if (type.needsInputString() && (inputString != null)) {
//							try {
//								this.customMenuBackground = type.createInstanceFromInputString(inputString);
//							} catch (Exception ex) {
//								ex.printStackTrace();
//							}
//							if (this.customMenuBackground != null) {
//								if (ScreenCustomization.isNewMenu()) {
//									this.customMenuBackground.onOpenMenu();
//								}
//							}
//						} else if (backId != null) {
//							this.customMenuBackground = type.getBackgroundByIdentifier(backId);
//							if (this.customMenuBackground != null) {
//								if (ScreenCustomization.isNewMenu()) {
//									this.customMenuBackground.onOpenMenu();
//								}
//							}
//						}
//					}
//				}
//			}
//
//			if (action.equalsIgnoreCase("hidebuttonfor")) {
//				String time = sec.getValue("seconds");
//				String onlyfirsttime = sec.getValue("onlyfirsttime");
//				String fadein = sec.getValue("fadein");
//				String fadeinspeed = sec.getValue("fadeinspeed");
//				if (b != null) {
//					if (ScreenCustomization.isNewMenu()) {
//						boolean ft = (onlyfirsttime != null) && onlyfirsttime.equalsIgnoreCase("true");
//						if ((time != null) && MathUtils.isFloat(time)) {
//							if (!ft || !this.delayAppearanceFirstTimeVanilla.contains(bd.getId())) {
//								this.delayAppearanceVanilla.put(bd, Float.parseFloat(time));
//							}
//						}
//						if (ft) {
//							if (!this.delayAppearanceFirstTimeVanilla.contains(bd.getId())) {
//								this.delayAppearanceFirstTimeVanilla.add(bd.getId());
//							}
//						}
//						if ((fadein != null) && fadein.equalsIgnoreCase("true")) {
//							float speed = 1.0F;
//							if ((fadeinspeed != null) && MathUtils.isFloat(fadeinspeed)) {
//								speed = Float.parseFloat(fadeinspeed);
//							}
//							this.fadeInVanilla.put(bd, speed);
//						}
//					}
//				}
//			}
//
//			if (action.equalsIgnoreCase("hidebutton")) {
//				if (b != null) {
//					this.hidden.add(bd);
//				}
//			}
//
//			if (action.equalsIgnoreCase("renamebutton") || action.equalsIgnoreCase("setbuttonlabel")) {
//				if (b != null) {
//					backgroundElements.add(new VanillaButtonCustomizationItem(sec, bd, this));
//				}
//			}
//
//			if (action.equalsIgnoreCase("resizebutton")) {
//				if (b != null) {
//					backgroundElements.add(new VanillaButtonCustomizationItem(sec, bd, this));
//				}
//			}
//
//			if (action.equalsIgnoreCase("movebutton")) {
//				if (b != null) {
//					backgroundElements.add(new VanillaButtonCustomizationItem(sec, bd, this));
//				}
//			}
//
//			if (action.equalsIgnoreCase("setbuttontexture")) {
//				if (b != null) {
//					String loopBackAnimations = sec.getValue("loopbackgroundanimations");
//					if ((loopBackAnimations != null) && loopBackAnimations.equalsIgnoreCase("false")) {
//						this.getContainerForVanillaButton(b).loopAnimation = false;
//					}
//					String restartBackAnimationsOnHover = sec.getValue("restartbackgroundanimations");
//					if ((restartBackAnimationsOnHover != null) && restartBackAnimationsOnHover.equalsIgnoreCase("false")) {
//						this.getContainerForVanillaButton(b).restartAnimationOnHover = false;
//					}
//					String backNormal = AbstractElement.fixBackslashPath(sec.getValue("backgroundnormal"));
//					String backHover = AbstractElement.fixBackslashPath(sec.getValue("backgroundhovered"));
//					if (backNormal != null) {
//						this.getContainerForVanillaButton(b).normalBackground = backNormal;
//					} else {
//						String backAniNormal = sec.getValue("backgroundanimationnormal");
//						if (backAniNormal != null) {
//							this.getContainerForVanillaButton(b).normalBackground = "animation:" + backAniNormal;
//						}
//					}
//					if (backHover != null) {
//						this.getContainerForVanillaButton(b).hoverBackground = backHover;
//					} else {
//						String backAniHover = sec.getValue("backgroundanimationhovered");
//						if (backAniHover != null) {
//							this.getContainerForVanillaButton(b).hoverBackground = "animation:" + backAniHover;
//						}
//					}
//				}
//			}
//
//			if (action.equalsIgnoreCase("setbuttonclicksound")) {
//				if (b != null) {
//					String path = AbstractElement.fixBackslashPath(sec.getValue("path"));
//					if (path != null) {
//						this.getContainerForVanillaButton(b).clickSound = path;
//					}
//				}
//			}
//
//			if (action.equalsIgnoreCase("vanilla_button_visibility_requirements")) {
//				if (b != null) {
//					this.vanillaButtonLoadingRequirementContainers.put(b, LoadingRequirementContainer.deserializeRequirementContainer(sec));
//				}
//			}
//
//			if (action.equalsIgnoreCase("addhoversound")) {
//				if (b != null) {
//					if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
//						backgroundElements.add(new VanillaButtonCustomizationItem(sec, bd, this));
//					} else {
//						foregroundElements.add(new VanillaButtonCustomizationItem(sec, bd, this));
//					}
//				}
//			}
//
//			if (action.equalsIgnoreCase("sethoverlabel")) {
//				if (b != null) {
//					if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
//						backgroundElements.add(new VanillaButtonCustomizationItem(sec, bd, this));
//					} else {
//						foregroundElements.add(new VanillaButtonCustomizationItem(sec, bd, this));
//					}
//				}
//			}
//
//			if (action.equalsIgnoreCase("clickbutton")) {
//				if (b != null) {
//					String clicks = sec.getValue("clicks");
//					if ((clicks != null) && (MathUtils.isInteger(clicks))) {
//						for (int i = 0; i < Integer.parseInt(clicks); i++) {
//							b.onClick(MouseInput.getMouseX(), MouseInput.getMouseY());
//						}
//					}
//				}
//			}
//
//			// CUSTOM ITEMS
//
//			if (action.equalsIgnoreCase("addtexture")) {
//				if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
//					backgroundElements.add(new TextureCustomizationItem(sec));
//				} else {
//					foregroundElements.add(new TextureCustomizationItem(sec));
//				}
//			}
//
//			if (action.equalsIgnoreCase("addwebtexture")) {
//				if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
//					backgroundElements.add(new WebTextureCustomizationItem(sec));
//				} else {
//					foregroundElements.add(new WebTextureCustomizationItem(sec));
//				}
//			}
//
//			if (action.equalsIgnoreCase("addanimation")) {
//				if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
//					backgroundElements.add(new AnimationCustomizationItem(sec));
//				} else {
//					foregroundElements.add(new AnimationCustomizationItem(sec));
//				}
//			}
//
//			if (action.equalsIgnoreCase("addshape")) {
//				if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
//					backgroundElements.add(new ShapeCustomizationItem(sec));
//				} else {
//					foregroundElements.add(new ShapeCustomizationItem(sec));
//				}
//			}
//
//			if (action.equalsIgnoreCase("addslideshow")) {
//				if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
//					backgroundElements.add(new SlideshowCustomizationItem(sec));
//				} else {
//					foregroundElements.add(new SlideshowCustomizationItem(sec));
//				}
//			}
//
//			if (action.equalsIgnoreCase("addbutton")) {
//				ButtonCustomizationItem i = new ButtonCustomizationItem(sec);
//
//				if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
//					backgroundElements.add(i);
//				} else {
//					foregroundElements.add(i);
//				}
//			}
//
//			if (action.equalsIgnoreCase("setcloseaudio")) {
//				String path = AbstractElement.fixBackslashPath(sec.getValue("path"));
//
//				if (path != null) {
//					File f = new File(path);
//					if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
//						path = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + path;
//						f = new File(path);
//					}
//					if (f.isFile() && f.exists() && f.getName().endsWith(".wav")) {
//						try {
//							String name = "closesound_" + path + Files.size(f.toPath());
//							SoundRegistry.registerSound(name, path);
//							this.closeAudio = name;
//							this.sharedLayoutProps.closeAudioSet = true;
//						} catch (Exception ex) {
//							ex.printStackTrace();
//						}
//					}
//				}
//			}
//
//			if (action.equalsIgnoreCase("setopenaudio")) {
//				if (ScreenCustomization.isNewMenu()) {
//					String path = AbstractElement.fixBackslashPath(sec.getValue("path"));
//					if (path != null) {
//						File f = new File(path);
//						if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
//							path = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + path;
//							f = new File(path);
//						}
//						if (f.isFile() && f.exists() && f.getName().endsWith(".wav")) {
//							try {
//								String name = "opensound_" + path + Files.size(f.toPath());
//								SoundRegistry.registerSound(name, path);
//								SoundHandler.resetSound(name);
//								SoundHandler.playSound(name);
//								this.openAudio = name;
//								this.sharedLayoutProps.openAudioSet = true;
//							} catch (Exception ex) {
//								ex.printStackTrace();
//							}
//						}
//					}
//				}
//			}
//
//			if (action.equalsIgnoreCase("setbuttondescription")) {
//				if (b != null) {
//					backgroundElements.add(new VanillaButtonCustomizationItem(sec, bd, this));
//				}
//			}
//
//			if (action.equalsIgnoreCase("addsplash")) {
//				String file = AbstractElement.fixBackslashPath(sec.getValue("splashfilepath"));
//				String text = sec.getValue("text");
//				if ((file != null) || (text != null)) {
//
//					SplashTextCustomizationItem i = new SplashTextCustomizationItem(sec);
//
//					if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
//						backgroundElements.add(i);
//					} else {
//						foregroundElements.add(i);
//					}
//
//				}
//			}
//
//			// CUSTOM ITEMS (API)
//			if (action.startsWith("custom_layout_element:")) {
//				String cusId = action.split(":", 2)[1];
//				ElementBuilder cusItem = ElementRegistry.getBuilder(cusId);
//				if (cusItem != null) {
//					CustomizationItem cusItemInstance = cusItem.deserializeElement(sec);
//					if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
//						backgroundElements.add(cusItemInstance);
//					} else {
//						foregroundElements.add(cusItemInstance);
//					}
//				}
//			}
//
//		}
//
//	}
//
//	protected void handleAppearanceDelayFor(AbstractElement i) {
//		if (!(i instanceof VanillaButtonCustomizationItem)) {
//			if (i.delayAppearance) {
//				if (i.getInstanceIdentifier() == null) {
//					return;
//				}
//				if (!i.delayAppearanceEverytime && delayAppearanceFirstTime.contains(i.getInstanceIdentifier())) {
//					return;
//				}
//				if (!i.delayAppearanceEverytime) {
//					if (!this.delayAppearanceFirstTime.contains(i.getInstanceIdentifier())) {
//						delayAppearanceFirstTime.add(i.getInstanceIdentifier());
//					}
//				}
//
//				i.visible = false;
//
//				if (i.fadeIn) {
//					i.opacity = 0.1F;
//				}
//
//				ThreadCaller c = new ThreadCaller();
//				this.delayThreads.add(c);
//
//				new Thread(() -> {
//					long start = System.currentTimeMillis();
//					float delay = (float) (1000.0 * i.appearanceDelayInSeconds);
//					boolean fade = false;
//					while (c.running.get()) {
//						try {
//							long now = System.currentTimeMillis();
//							if (!fade) {
//								if (now >= start + (int)delay) {
//									i.visible = true;
//									if (!i.fadeIn) {
//										return;
//									} else {
//										fade = true;
//									}
//								}
//							} else {
//								float o = i.opacity + (0.03F * i.fadeInSpeed);
//								if (o > 1.0F) {
//									o = 1.0F;
//								}
//								if (i.opacity < 1.0F) {
//									i.opacity = o;
//								} else {
//									return;
//								}
//							}
//							Thread.sleep(50);
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//					}
//				}).start();
//
//			}
//		}
//	}
//
//	protected void handleVanillaAppearanceDelayFor(ButtonData d) {
//		if (this.delayAppearanceVanilla.containsKey(d)) {
//
//			boolean fadein = this.fadeInVanilla.containsKey(d);
//			float delaysec = this.delayAppearanceVanilla.get(d);
//
//			LoadingRequirementContainer reqs = this.vanillaButtonLoadingRequirementContainers.get(d.getButton());
//
//			d.getButton().visible = false;
//			if (reqs != null) {
//				reqs.forceRequirementsNotMet = true;
//			}
//
//			if (fadein) {
//				d.getButton().setAlpha(0.1F);
//			}
//
//			ThreadCaller c = new ThreadCaller();
//			this.delayThreads.add(c);
//
//			new Thread(() -> {
//				float fadespeed = 1.0F;
//				if (fadein) {
//					if (fadeInVanilla.containsKey(d)) {
//						fadespeed = fadeInVanilla.get(d);
//					}
//				}
//				float opacity = 0.1F;
//				long start = System.currentTimeMillis();
//				float delay = (float) (1000.0 * delaysec);
//				boolean fade = false;
//				while (c.running.get()) {
//					try {
//						long now = System.currentTimeMillis();
//						if (!fade) {
//							if (now >= start + (int)delay) {
//								d.getButton().visible = true;
//								if (reqs != null) {
//									reqs.forceRequirementsNotMet = false;
//								}
//								if (!fadein) {
//									return;
//								} else {
//									fade = true;
//								}
//							}
//						} else {
//							float o = opacity + (0.03F * fadespeed);
//							if (o > 1.0F) {
//								o = 1.0F;
//							}
//							if (opacity < 1.0F) {
//								opacity = o;
//								d.getButton().setAlpha(opacity);
//							} else {
//								return;
//							}
//						}
//
//						Thread.sleep(50);
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
//			}).start();
//
//		}
//	}
//
//	@EventListener(priority = EventPriority.VERY_HIGH)
//	public void onRenderPre(RenderScreenEvent.Pre e) {
//		if (PopupHandler.isPopupActive()) {
//			return;
//		}
//		if (!this.shouldCustomize(e.getScreen())) {
//			return;
//		}
//		if (!ScreenCustomization.isCustomizationEnabledForScreen(e.getScreen())) {
//			return;
//		}
//
//		//Re-init screen if layout-wide requirements changed
//		for (Map.Entry<LoadingRequirementContainer, Boolean> m : this.cachedLayoutWideLoadingRequirements.entrySet()) {
//
//			if (m.getKey().requirementsMet() != m.getValue()) {
//				e.getScreen().resize(Minecraft.getInstance(), e.getScreen().width, e.getScreen().height);
//				break;
//			}
//		}
//
//	}
//
//	@EventListener
//	public void onRenderPost(RenderScreenEvent.Post e) {
//		if (PopupHandler.isPopupActive()) {
//			return;
//		}
//		if (!this.shouldCustomize(e.getScreen())) {
//			return;
//		}
//		if (!ScreenCustomization.isCustomizationEnabledForScreen(e.getScreen())) {
//			return;
//		}
//
//		if ((this.customMenuTitle != null) && !this.forceDisableCustomMenuTitle) {
//			ScreenTitleUtils.setScreenTitle(e.getScreen(), Component.literal(PlaceholderParser.replacePlaceholders(this.customMenuTitle)));
//		}
//
//		if (!this.backgroundDrawable) {
//			//Rendering all items that SHOULD be rendered in the background IF it's not possible to render them in the background (In this case, they will be forced to render in the foreground)
//			List<AbstractElement> backItems = new ArrayList<>(this.backgroundElements);
//			for (AbstractElement i : backItems) {
//				try {
//					i.render(e.getPoseStack(), e.getScreen());
//				} catch (IOException e1) {
//					e1.printStackTrace();
//				}
//			}
//		}
//
//		//Rendering all items that should be rendered in the foreground
//		List<AbstractElement> frontItems = new ArrayList<>(this.foregroundElements);
//		for (AbstractElement i : frontItems) {
//			try {
//				i.render(e.getPoseStack(), e.getScreen());
//			} catch (IOException e1) {
//				e1.printStackTrace();
//			}
//		}
//	}
//
//	@EventListener
//	public void drawToBackground(RenderedScreenBackgroundEvent e) {
//		if (!ScreenCustomization.isCurrentMenuScrollable()) {
//			this.renderBackground(e.getPoseStack(), e.getScreen());
//		}
//	}
//
//	protected void renderBackground(PoseStack matrix, Screen s) {
//		if (this.shouldCustomize(s)) {
//			if (!ScreenCustomization.isCustomizationEnabledForScreen(s)) {
//				return;
//			}
//
//			//Rendering the background animation to the menu
//			if (this.canRenderBackground()) {
//				if ((this.backgroundAnimation != null) && this.backgroundAnimation.isReady()) {
//					boolean b = this.backgroundAnimation.isStretchedToStreensize();
//					int wOri = this.backgroundAnimation.getWidth();
//					int hOri = this.backgroundAnimation.getHeight();
//					int xOri = this.backgroundAnimation.getPosX();
//					int yOri = this.backgroundAnimation.getPosY();
//					if (!this.keepBackgroundAspectRatio) {
//						this.backgroundAnimation.setStretchImageToScreensize(true);
//					} else {
//						double ratio = (double) wOri / (double) hOri;
//						int wfinal = (int)(s.height * ratio);
//						int screenCenterX = s.width / 2;
//						if (wfinal < s.width) {
//							this.backgroundAnimation.setStretchImageToScreensize(true);
//						} else {
//							this.backgroundAnimation.setWidth(wfinal + 1);
//							this.backgroundAnimation.setHeight(s.height + 1);
//							this.backgroundAnimation.setPosX(screenCenterX - (wfinal / 2));
//							this.backgroundAnimation.setPosY(0);
//						}
//					}
//
//					this.backgroundAnimation.setOpacity(this.backgroundOpacity);
//
//					this.backgroundAnimation.render(matrix);
//					this.backgroundAnimation.setWidth(wOri);
//					this.backgroundAnimation.setHeight(hOri);
//					this.backgroundAnimation.setPosX(xOri);
//					this.backgroundAnimation.setPosY(yOri);
//					this.backgroundAnimation.setStretchImageToScreensize(b);
//
//					this.backgroundAnimation.setOpacity(1.0F);
//				} else if (this.backgroundTexture != null) {
//					RenderSystem.enableBlend();
//					RenderUtils.bindTexture(this.backgroundTexture.getResourceLocation());
//
//					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.backgroundOpacity);
//					if (!this.panoramaback) {
//						if (!this.keepBackgroundAspectRatio) {
//							GuiComponent.blit(matrix, 0, 0, 1.0F, 1.0F, s.width + 1, s.height + 1, s.width + 1, s.height + 1);
//						} else {
//							int w = this.backgroundTexture.getWidth();
//							int h = this.backgroundTexture.getHeight();
//							double ratio = (double) w / (double) h;
//							int wfinal = (int)(s.height * ratio);
//							int screenCenterX = s.width / 2;
//							if (wfinal < s.width) {
//								GuiComponent.blit(matrix, 0, 0, 1.0F, 1.0F, s.width + 1, s.height + 1, s.width + 1, s.height + 1);
//							} else {
//								GuiComponent.blit(matrix, screenCenterX - (wfinal / 2), 0, 1.0F, 1.0F, wfinal + 1, s.height + 1, wfinal + 1, s.height + 1);
//							}
//						}
//					} else {
//						int w = this.backgroundTexture.getWidth();
//						int h = this.backgroundTexture.getHeight();
//						double ratio = (double) w / (double) h;
//						int wfinal = (int)(s.height * ratio);
//
//						//Check if the panorama background should move to the left side or to the ride side
//						if ((panoPos + (wfinal - s.width)) <= 0) {
//							panoMoveBack = true;
//						}
//						if (panoPos >= 0) {
//							panoMoveBack = false;
//						}
//
//						//Fix pos after resizing
//						if (panoPos + (wfinal - s.width) < 0) {
//							panoPos = -(wfinal - s.width);
//						}
//						if (panoPos > 0) {
//							panoPos = 0;
//						}
//
//						if (!panoStop) {
//							if (panoTick >= 1) {
//								panoTick = 0;
//								if (panoMoveBack) {
//									panoPos = panoPos + 0.5;
//								} else {
//									panoPos = panoPos - 0.5;
//								}
//
//								if (panoPos + (wfinal - s.width) == 0) {
//									panoStop = true;
//								}
//								if (panoPos == 0) {
//									panoStop = true;
//								}
//							} else {
//								panoTick++;
//							}
//						} else {
//							if (panoTick >= 300) {
//								panoStop = false;
//								panoTick = 0;
//							} else {
//								panoTick++;
//							}
//						}
//						if (wfinal <= s.width) {
//							GuiComponent.blit(matrix, 0, 0, 1.0F, 1.0F, s.width + 1, s.height + 1, s.width + 1, s.height + 1);
//						} else {
//							RenderUtils.doubleBlit(panoPos, 0, 1.0F, 1.0F, wfinal, s.height + 1);
//						}
//					}
//
//					RenderSystem.disableBlend();
//
//					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//
//				} else if (this.panoramaCube != null) {
//
//
//					float opacity = this.panoramaCube.opacity;
//					this.panoramaCube.opacity = this.backgroundOpacity;
//					this.panoramaCube.render(matrix);
//					this.panoramaCube.opacity = opacity;
//
//
//				} else if (this.slideshow != null) {
//					int sw = this.slideshow.width;
//					int sh = this.slideshow.height;
//					int sx = this.slideshow.x;
//					int sy = this.slideshow.y;
//
//					float opacity = this.slideshow.slideshowOpacity;
//
//					if (!this.keepBackgroundAspectRatio) {
//						this.slideshow.width = s.width + 1;
//						this.slideshow.height = s.height +1;
//						this.slideshow.x = 0;
//					} else {
//						double ratio = (double) sw / (double) sh;
//						int wfinal = (int)(s.height * ratio);
//						int screenCenterX = s.width / 2;
//						if (wfinal < s.width) {
//							this.slideshow.width = s.width + 1;
//							this.slideshow.height = s.height +1;
//							this.slideshow.x = 0;
//						} else {
//							this.slideshow.width = wfinal + 1;
//							this.slideshow.height = s.height +1;
//							this.slideshow.x = screenCenterX - (wfinal / 2);
//						}
//					}
//					this.slideshow.y = 0;
//
//					this.slideshow.slideshowOpacity = this.backgroundOpacity;
//
//					this.slideshow.render(matrix);
//
//					this.slideshow.width = sw;
//					this.slideshow.height = sh;
//					this.slideshow.x = sx;
//					this.slideshow.y = sy;
//
//					this.slideshow.slideshowOpacity = opacity;
//				} else if (this.customMenuBackground != null) {
//
//
//					this.customMenuBackground.opacity = this.backgroundOpacity;
//					this.customMenuBackground.render(matrix, s, this.keepBackgroundAspectRatio);
//					this.customMenuBackground.opacity = 1.0F;
//
//
//				}
//			}
//
//			if (PopupHandler.isPopupActive()) {
//				return;
//			}
//
//			//Rendering all items which should be rendered in the background
//			List<AbstractElement> backItems = new ArrayList<>(this.backgroundElements);
//			for (AbstractElement i : backItems) {
//				try {
//					i.render(matrix, s);
//				} catch (IOException e1) {
//					e1.printStackTrace();
//				}
//			}
//
//			this.backgroundDrawable = true;
//		}
//	}
//
//	@EventListener
//	public void onButtonClickSound(PlayWidgetClickSoundEvent.Pre e) {
//
//		if (this.shouldCustomize(Minecraft.getInstance().screen)) {
//			if (ScreenCustomization.isCustomizationEnabledForScreen(Minecraft.getInstance().screen)) {
//
//				ButtonCustomizationContainer c = this.vanillaButtonCustomizations.get(e.getWidget());
//
//				if (c != null) {
//					if (c.clickSound != null) {
//						File f = new File(c.clickSound);
//						if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
//							c.clickSound = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + c.clickSound;
//							f = new File(c.clickSound);
//						}
//						if (f.exists() && f.isFile() && f.getPath().toLowerCase().endsWith(".wav")) {
//
//							SoundHandler.registerSound(f.getPath(), f.getPath());
//							SoundHandler.resetSound(f.getPath());
//							SoundHandler.playSound(f.getPath());
//
//							e.setCanceled(true);
//
//						}
//					}
//				}
//
//			}
//		}
//
//	}
//
//	@EventListener
//	public void onButtonRenderBackground(RenderWidgetBackgroundEvent.Pre e) {
//		if (this.shouldCustomize(Minecraft.getInstance().screen)) {
//			if (ScreenCustomization.isCustomizationEnabledForScreen(Minecraft.getInstance().screen)) {
//
//				AbstractWidget w = e.getWidget();
//				ButtonCustomizationContainer c = this.vanillaButtonCustomizations.get(w);
//				if (c != null) {
//					String normalBack = c.normalBackground;
//					String hoverBack = c.hoverBackground;
//					boolean hasCustomBackground = false;
//					boolean restart = false;
//					if (c.lastHoverState != w.isHoveredOrFocused()) {
//						if (w.isHoveredOrFocused() && c.restartAnimationOnHover) {
//							restart = true;
//						}
//					}
//					c.lastHoverState = w.isHoveredOrFocused();
//
//					if (!w.isHoveredOrFocused()) {
//						if (normalBack != null) {
//							if (this.renderCustomButtomBackground(e, normalBack, restart)) {
//								hasCustomBackground = true;
//							}
//						}
//					}
//
//					if (w.isHoveredOrFocused()) {
//						if (w.active) {
//							if (hoverBack != null) {
//								if (this.renderCustomButtomBackground(e, hoverBack, restart)) {
//									hasCustomBackground = true;
//								}
//							}
//						} else {
//							if (normalBack != null) {
//								if (this.renderCustomButtomBackground(e, normalBack, restart)) {
//									hasCustomBackground = true;
//								}
//							}
//						}
//					}
//
//					if (hasCustomBackground) {
//						if (w instanceof ImageButton) {
//							Component msg = w.getMessage();
//							if (msg != null) {
//								int j = w.active ? 16777215 : 10526880;
//								GuiComponent.drawCenteredString(e.getPoseStack(), Minecraft.getInstance().font, msg, w.x + w.getWidth() / 2, w.y + (w.getHeight() - 8) / 2, j | Mth.ceil(e.getAlpha() * 255.0F) << 24);
//							}
//						}
//
//						e.setCanceled(true);
//					}
//				}
//
//			}
//		}
//	}
//
//	protected boolean renderCustomButtomBackground(RenderWidgetBackgroundEvent e, String background, boolean restartAnimationBackground) {
//		AbstractWidget w = e.getWidget();
//		PoseStack matrix = e.getPoseStack();
//		ButtonCustomizationContainer c = this.vanillaButtonCustomizations.get(w);
//		if (c != null) {
//			if (w != null) {
//				if (background != null) {
//					if (background.startsWith("animation:")) {
//						String aniName = background.split("[:]", 2)[1];
//						if (AnimationHandler.animationExists(aniName)) {
//							IAnimationRenderer a = AnimationHandler.getAnimation(aniName);
//							if (restartAnimationBackground && (a != null)) {
//								a.resetAnimation();
//							}
//							this.renderBackgroundAnimation(e, a);
//							if (!c.cachedAnimations.contains(a)) {
//								c.cachedAnimations.add(a);
//							}
//							return true;
//						}
//					} else {
//						File f = new File(background);
//						if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
//							background = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + background;
//							f = new File(background);
//						}
//						if (f.isFile()) {
//							if (f.getPath().toLowerCase().endsWith(".gif")) {
//								IAnimationRenderer a =  ExternalTextureHandler.INSTANCE.getGif(f.getPath());
//								if (restartAnimationBackground) {
//									a.resetAnimation();
//								}
//								this.renderBackgroundAnimation(e, a);
//								if (!c.cachedAnimations.contains(a)) {
//									c.cachedAnimations.add(a);
//								}
//								return true;
//							} else if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
//								ExternalTextureResourceLocation back = ExternalTextureHandler.INSTANCE.getTexture(f.getPath());
//								if (back != null) {
//									RenderUtils.bindTexture(back.getResourceLocation());
//									RenderSystem.enableBlend();
//									RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, e.getAlpha());
//									GuiComponent.blit(matrix, w.x, w.y, 0.0F, 0.0F, w.getWidth(), w.getHeight(), w.getWidth(), w.getHeight());
//									return true;
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//		return false;
//	}
//
//	protected void renderBackgroundAnimation(RenderWidgetBackgroundEvent e, IAnimationRenderer ani) {
//		AbstractWidget w = e.getWidget();
//		ButtonCustomizationContainer c = this.vanillaButtonCustomizations.get(w);
//		if (c != null) {
//			if (ani != null) {
//				if (!ani.isReady()) {
//					ani.prepareAnimation();
//				}
//
//				int aniX = ani.getPosX();
//				int aniY = ani.getPosY();
//				int aniWidth = ani.getWidth();
//				int aniHeight = ani.getHeight();
//				boolean aniLoop = ani.isGettingLooped();
//
//				ani.setPosX(w.x);
//				ani.setPosY(w.y);
//				ani.setWidth(w.getWidth());
//				ani.setHeight(w.getHeight());
//				ani.setLooped(c.loopAnimation);
//				ani.setOpacity(e.getAlpha());
//				if (ani instanceof AdvancedAnimation) {
//					((AdvancedAnimation) ani).setMuteAudio(true);
//				}
//
//				ani.render(e.getPoseStack());
//
//				ani.setPosX(aniX);
//				ani.setPosY(aniY);
//				ani.setWidth(aniWidth);
//				ani.setHeight(aniHeight);
//				ani.setLooped(aniLoop);
//				ani.setOpacity(1.0F);
//				if (ani instanceof AdvancedAnimation) {
//					((AdvancedAnimation) ani).setMuteAudio(false);
//				}
//			}
//		}
//	}
//
//	protected ButtonCustomizationContainer getContainerForVanillaButton(AbstractWidget w) {
//		if (!this.vanillaButtonCustomizations.containsKey(w)) {
//			ButtonCustomizationContainer c = new ButtonCustomizationContainer();
//			this.vanillaButtonCustomizations.put(w, c);
//			return c;
//		}
//		return this.vanillaButtonCustomizations.get(w);
//	}
//
//	@Nullable
//	public AbstractElement getElementByInstanceIdentifier(String instanceIdentifier) {
//		for (AbstractElement c : this.backgroundElements) {
//			if (c instanceof VanillaButtonCustomizationItem) {
//				String id = "vanillabtn:" + ((VanillaButtonCustomizationItem)c).getButtonId();
//				if (id.equals(instanceIdentifier)) {
//					return c;
//				}
//			} else {
//				if (c.getInstanceIdentifier().equals(instanceIdentifier)) {
//					return c;
//				}
//			}
//		}
//		for (AbstractElement c : this.foregroundElements) {
//			if (c instanceof VanillaButtonCustomizationItem) {
//				String id = "vanillabtn:" + ((VanillaButtonCustomizationItem)c).getButtonId();
//				if (id.equals(instanceIdentifier)) {
//					return c;
//				}
//			} else {
//				if (c.getInstanceIdentifier().equals(instanceIdentifier)) {
//					return c;
//				}
//			}
//		}
//		if (instanceIdentifier.startsWith("vanillabtn:")) {
//			String idRaw = instanceIdentifier.split(":", 2)[1];
//			ButtonData d;
//			if (MathUtils.isLong(idRaw)) {
//				d = ButtonCache.getButtonForId(Long.parseLong(idRaw));
//			} else {
//				d = ButtonCache.getButtonForCompatibilityId(idRaw);
//			}
//			if ((d != null) && (d.getButton() != null)) {
//				VanillaButtonCustomizationItem vb = new VanillaButtonCustomizationItem(new PropertyContainer("customization"), d, this);
//				vb.anchorPoint = ElementAnchorPoint.TOP_LEFT;
//				vb.baseX = d.getButton().x;
//				vb.baseY = d.getButton().y;
//				vb.width = d.getButton().getWidth();
//				vb.height = d.getButton().getHeight();
//				return vb;
//			}
//		}
//		return null;
//	}
//
//	protected boolean vanillaButtonLoadingRequirementsMet(AbstractWidget b) {
//		LoadingRequirementContainer c = this.vanillaButtonLoadingRequirementContainers.get(b);
//		if (c != null) {
//			return c.requirementsMet();
//		}
//		return true;
//	}
//
//	@EventListener
//	public void onRenderListBackground(RenderGuiListBackgroundEvent.Post e) {
//		Screen s = Minecraft.getInstance().screen;
//		if (this.shouldCustomize(s)) {
//			if (ScreenCustomization.isCustomizationEnabledForScreen(s)) {
//				//Allow background stuff to be rendered in scrollable GUIs
//				if (Minecraft.getInstance().screen != null) {
//					this.renderBackground(e.getPoseStack(), s);
//				}
//			}
//		}
//	}
//
//	private static ButtonData getButton(String identifier) {
//		if (identifier.startsWith("%id=")) {
//			String p = identifier.split("=")[1].replace("%", "");
//			if (MathUtils.isLong(p)) {
//				return ButtonCache.getButtonForId(Long.parseLong(p));
//			} else if (p.startsWith("button_compatibility_id:")) {
//				return ButtonCache.getButtonForCompatibilityId(p);
//			}
//		} else {
//			ButtonData b;
//			if (I18n.exists(identifier)) {
//				b = ButtonCache.getButtonForKey(identifier);
//			} else {
//				b = ButtonCache.getButtonForName(identifier);
//			}
//			return b;
//		}
//		return null;
//	}
//
//	protected boolean shouldCustomize(Screen menu) {
//		if (menu == null) {
//			return false;
//		}
//		if (getIdentifier() != null) {
//			return this.getIdentifier().equals(menu.getClass().getName());
//		}
//		return true;
//	}
//
//	public boolean canRenderBackground() {
//		return ((this.backgroundAnimation != null) || (this.backgroundTexture != null) || (this.panoramaCube != null) || (this.slideshow != null) || (this.customMenuBackground != null));
//	}
//
//	public boolean setBackgroundAnimation(int id) {
//		if (id < this.backgroundAnimations.size()) {
//			this.backgroundAnimationId = id;
//			this.backgroundAnimation = this.backgroundAnimations.get(id);
//			this.lastBackgroundAnimation = this.backgroundAnimation;
//			return true;
//		}
//		return false;
//	}
//
//	public int getCurrentBackgroundAnimationId() {
//		return this.backgroundAnimationId;
//	}
//
//	public List<IAnimationRenderer> backgroundAnimations() {
//		return this.backgroundAnimations;
//	}
//
//	private static class ThreadCaller {
//		AtomicBoolean running = new AtomicBoolean(true);
//	}
//
//	public static class RandomLayoutContainer {
//
//		public final String id;
//		protected List<PropertyContainerSet> layouts = new ArrayList<PropertyContainerSet>();
//		protected boolean onlyFirstTime = false;
//		protected String lastLayoutPath = null;
//
//		public ScreenCustomizationLayerOLD parent;
//
//		public RandomLayoutContainer(String id, ScreenCustomizationLayerOLD parent) {
//			this.id = id;
//			this.parent = parent;
//		}
//
//		public List<PropertyContainerSet> getLayouts() {
//			return this.layouts;
//		}
//
//		public void addLayout(PropertyContainerSet layout) {
//			this.layouts.add(layout);
//		}
//
//		public void addLayouts(List<PropertyContainerSet> layouts) {
//			this.layouts.addAll(layouts);
//		}
//
//		public void clearLayouts() {
//			this.layouts.clear();
//		}
//
//		public void setOnlyFirstTime(boolean b) {
//			this.onlyFirstTime = b;
//		}
//
//		public boolean isOnlyFirstTime() {
//			return this.onlyFirstTime;
//		}
//
//		public void resetLastLayout() {
//			this.lastLayoutPath = null;
//		}
//
//		@Nullable
//		public PropertyContainerSet getRandomLayout() {
//			if (!this.layouts.isEmpty()) {
//				if ((this.onlyFirstTime || !ScreenCustomization.isNewMenu()) && (this.lastLayoutPath != null)) {
//					File f = new File(this.lastLayoutPath);
//					if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
//						this.lastLayoutPath = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + this.lastLayoutPath;
//						f = new File(this.lastLayoutPath);
//					}
//					if (f.exists()) {
//						for (PropertyContainerSet s : this.layouts) {
//							List<PropertyContainer> metas = s.getSectionsOfType("customization-meta");
//							if (metas.isEmpty()) {
//								metas = s.getSectionsOfType("type-meta");
//							}
//							if (metas.isEmpty()) {
//								continue;
//							}
//							String path = metas.get(0).getValue("path");
//							if ((path != null) && path.equals(this.lastLayoutPath)) {
//								return s;
//							}
//						}
//					} else {
//						SoundRegistry.stopSounds();
//						SoundRegistry.resetSounds();
//						AnimationHandler.resetAnimations();
//						AnimationHandler.resetAnimationSounds();
//						AnimationHandler.stopAnimationSounds();
//					}
//				}
//				int i = MathUtils.getRandomNumberInRange(0, this.layouts.size()-1);
//				PropertyContainerSet s = this.layouts.get(i);
//				List<PropertyContainer> metas = s.getSectionsOfType("customization-meta");
//				if (metas.isEmpty()) {
//					metas = s.getSectionsOfType("type-meta");
//				}
//				if (!metas.isEmpty()) {
//					String path = metas.get(0).getValue("path");
//					if ((path != null)) {
//						this.lastLayoutPath = path;
//						return s;
//					}
//				}
//			}
//			return null;
//		}
//
//	}
//
//	public boolean isVanillaButtonDelayed(AbstractWidget w) {
//		for (ButtonData d : this.delayAppearanceVanilla.keySet()) {
//			if (d.getButton() == w) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	public boolean isVanillaButtonHidden(AbstractWidget w) {
//		for (ButtonData d : this.hidden) {
//			if (d.getButton() == w) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	public static class SharedLayoutProperties {
//
//		public boolean scaled = false;
//		public int autoScaleBaseWidth = 0;
//		public int autoScaleBaseHeight = 0;
//		public boolean backgroundTextureSet = false;
//		public boolean openAudioSet = false;
//		public boolean closeAudioSet = false;
//
//	}
//
//	public static class ButtonCustomizationContainer {
//
//		public String normalBackground = null;
//		public String hoverBackground = null;
//		public boolean loopAnimation = true;
//		public boolean restartAnimationOnHover = true;
//		public String clickSound = null;
//		public String hoverSound = null;
//		public String hoverLabel = null;
//		public int autoButtonClicks = 0;
//		public String customButtonLabel = null;
//		public String buttonDescription = null;
//		public boolean isButtonHidden = false;
//		public LoadingRequirementContainer loadingRequirementContainer = null;
//
//		public List<IAnimationRenderer> cachedAnimations = new ArrayList<>();
//		public boolean lastHoverState = false;
//
//	}
//
//}

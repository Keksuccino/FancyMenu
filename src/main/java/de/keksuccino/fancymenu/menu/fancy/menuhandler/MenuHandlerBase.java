package de.keksuccino.fancymenu.menu.fancy.menuhandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.api.background.MenuBackground;
import de.keksuccino.fancymenu.api.background.MenuBackgroundType;
import de.keksuccino.fancymenu.api.background.MenuBackgroundTypeRegistry;
import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.api.item.CustomizationItemRegistry;
import de.keksuccino.fancymenu.events.*;
import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.button.VanillaButtonDescriptionHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomizationProperties;
import de.keksuccino.fancymenu.menu.fancy.gameintro.GameIntroHandler;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.item.AnimationCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.ButtonCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.fancymenu.menu.fancy.item.ShapeCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.SlideshowCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.SplashTextCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.StringCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.TextureCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.VanillaButtonCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.WebStringCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.WebTextureCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.VisibilityRequirementContainer;
import de.keksuccino.fancymenu.menu.panorama.ExternalTexturePanoramaRenderer;
import de.keksuccino.fancymenu.menu.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser;
import de.keksuccino.fancymenu.menu.slideshow.ExternalTextureSlideshowRenderer;
import de.keksuccino.fancymenu.menu.slideshow.SlideshowHandler;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.properties.PropertiesSet;
import de.keksuccino.konkrete.rendering.CurrentScreenHandler;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.konkrete.resources.TextureHandler;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MenuHandlerBase extends GuiComponent {

	private static final Logger LOGGER = LogManager.getLogger("fancymenu/MenuHandlerBase");

	public List<CustomizationItemBase> frontRenderItems = new ArrayList<CustomizationItemBase>();
	public List<CustomizationItemBase> backgroundRenderItems = new ArrayList<CustomizationItemBase>();
	
	protected Map<String, Boolean> audio = new HashMap<String, Boolean>();
	protected IAnimationRenderer backgroundAnimation = null;
	protected IAnimationRenderer lastBackgroundAnimation = null;
	protected List<IAnimationRenderer> backgroundAnimations = new ArrayList<IAnimationRenderer>();
	protected int backgroundAnimationId = 0;
	protected ExternalTextureResourceLocation backgroundTexture = null;
	protected String identifier;
	protected boolean backgroundDrawable;
	protected boolean panoramaback = false;
	protected int panoTick = 0;
	protected double panoPos = 0.0;
	protected boolean panoMoveBack = false;
	protected boolean panoStop = false;
	protected boolean keepBackgroundAspectRatio = false;
	protected String customMenuTitle = null;

	protected ExternalTexturePanoramaRenderer panoramacube;

	protected ExternalTextureSlideshowRenderer slideshow;

	protected MenuBackground customMenuBackground = null;

	protected List<ButtonData> hidden = new ArrayList<ButtonData>();
	protected Map<AbstractWidget, ButtonCustomizationContainer> vanillaButtonCustomizations = new HashMap<AbstractWidget, ButtonCustomizationContainer>();
	protected Map<AbstractWidget, VisibilityRequirementContainer> vanillaButtonVisibilityRequirementContainers = new HashMap<AbstractWidget, VisibilityRequirementContainer>();

	protected volatile Map<ButtonData, Float> delayAppearanceVanilla = new HashMap<ButtonData, Float>();
	protected Map<ButtonData, Float> fadeInVanilla = new HashMap<ButtonData, Float>();
	protected List<String> delayAppearanceFirstTime = new ArrayList<String>();
	protected List<Long> delayAppearanceFirstTimeVanilla = new ArrayList<Long>();
	protected List<ThreadCaller> delayThreads = new ArrayList<ThreadCaller>();

	protected boolean preinit = false;

	protected Map<String, RandomLayoutContainer> randomLayoutGroups = new HashMap<String, RandomLayoutContainer>();
	protected List<PropertiesSet> normalLayouts = new ArrayList<PropertiesSet>();
	protected SharedLayoutProperties sharedLayoutProps = new SharedLayoutProperties();

	protected String closeAudio;
	protected String openAudio;

	protected Map<VisibilityRequirementContainer, Boolean> cachedLayoutWideRequirements = new HashMap<>();

	protected static Screen scaleChangedIn = null;

	public static Map<Class, Component> cachedOriginalMenuTitles = new HashMap<>();

	/**
	 * @param identifier Has to be the valid and full class name of the GUI screen.
	 */
	public MenuHandlerBase(@Nonnull String identifier) {
		this.identifier = identifier;
	}

	public String getMenuIdentifier() {
		return this.identifier;
	}

	@SubscribeEvent
	public void onSoftReload(SoftMenuReloadEvent e) {
		if (this.shouldCustomize(e.screen)) {
			this.delayAppearanceFirstTimeVanilla.clear();
			this.delayAppearanceFirstTime.clear();
			this.delayAppearanceVanilla.clear();
			this.fadeInVanilla.clear();
			for (RandomLayoutContainer c : this.randomLayoutGroups.values()) {
				c.lastLayoutPath = null;
			}

			if (this.lastBackgroundAnimation != null) {
				this.lastBackgroundAnimation.resetAnimation();
			}
		}
	}

	@SubscribeEvent
	public void onMenuReloaded(MenuReloadedEvent e) {
		this.delayAppearanceFirstTimeVanilla.clear();
		this.delayAppearanceFirstTime.clear();
		this.delayAppearanceVanilla.clear();
		this.fadeInVanilla.clear();
		for (RandomLayoutContainer c : this.randomLayoutGroups.values()) {
			c.lastLayoutPath = null;
		}

		if (this.lastBackgroundAnimation != null) {
			this.lastBackgroundAnimation.resetAnimation();
		}
	}

	//TODO übernehmen 1.19.4 (event ändern)
	@SubscribeEvent
	public void onInitPre(InitOrResizeScreenEvent.Pre e) {

		for (ThreadCaller t : this.delayThreads) {
			t.running.set(false);
		}
		this.delayThreads.clear();

		int mcscale = Minecraft.getInstance().getWindow().calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode());

		if (e.getScreen() != Minecraft.getInstance().screen) {
			return;
		}

		//Resetting scale to the normal value if it was changed in another screen
		if ((scaleChangedIn != null) && (scaleChangedIn != e.getScreen())) {
			scaleChangedIn = null;
			Window m = Minecraft.getInstance().getWindow();
			m.setGuiScale((double)mcscale);
			e.getScreen().width = m.getGuiScaledWidth();
			e.getScreen().height = m.getGuiScaledHeight();
		}

		if (!MenuCustomization.isValidScreen(e.getScreen())) {
			return;
		}
		if (!this.shouldCustomize(e.getScreen())) {
			return;
		}
		if (!AnimationHandler.isReady()) {
			return;
		}
		if (!GameIntroHandler.introDisplayed) {
			return;
		}
		if (LayoutEditorScreen.isActive) {
			return;
		}
		if (ButtonCache.isCaching()) {
			return;
		}
		if (!MenuCustomization.isMenuCustomizable(e.getScreen())) {
			return;
		}

		preinit = true;

		List<PropertiesSet> rawLayouts = MenuCustomizationProperties.getPropertiesWithIdentifier(this.getMenuIdentifier());
		String defaultGroup = "-100397";

		this.normalLayouts.clear();

		for (RandomLayoutContainer c : this.randomLayoutGroups.values()) {
			c.onlyFirstTime = false;
			c.clearLayouts();
		}

		this.sharedLayoutProps = new SharedLayoutProperties();

		this.customMenuTitle = null;

		this.cachedLayoutWideRequirements.clear();

		for (PropertiesSet s : rawLayouts) {
			
			List<PropertiesSection> metas = s.getPropertiesOfType("customization-meta");
			if (metas.isEmpty()) {
				metas = s.getPropertiesOfType("type-meta");
			}
			if (metas.isEmpty()) {
				continue;
			}

			VisibilityRequirementContainer globalVisReqContainer = new CustomizationItemBase(metas.get(0)) {
				@Override public void render(PoseStack matrix, Screen menu) throws IOException {}
			}.visibilityRequirementContainer;
			this.cachedLayoutWideRequirements.put(globalVisReqContainer, globalVisReqContainer.isVisible());
			if (!globalVisReqContainer.isVisible()) {
				continue;
			}

			String cusMenuTitle = metas.get(0).getEntryValue("custom_menu_title");
			if (cusMenuTitle != null) {
				this.customMenuTitle = cusMenuTitle;
				e.getScreen().title = Component.literal(PlaceholderParser.replacePlaceholders(cusMenuTitle));
			}
			
			String biggerthanwidth = metas.get(0).getEntryValue("biggerthanwidth");
			if (biggerthanwidth != null) {
				biggerthanwidth = biggerthanwidth.replace(" ", "");
				if (MathUtils.isInteger(biggerthanwidth)) {
					int i = Integer.parseInt(biggerthanwidth);
					if (MainWindowHandler.getWindowGuiWidth() < i) {
						continue;
					}
				}
			}
			String biggerthanheight = metas.get(0).getEntryValue("biggerthanheight");
			if (biggerthanheight != null) {
				biggerthanheight = biggerthanheight.replace(" ", "");
				if (MathUtils.isInteger(biggerthanheight)) {
					int i = Integer.parseInt(biggerthanheight);
					if (MainWindowHandler.getWindowGuiHeight() < i) {
						continue;
					}
				}
			}
			String smallerthanwidth = metas.get(0).getEntryValue("smallerthanwidth");
			if (smallerthanwidth != null) {
				smallerthanwidth = smallerthanwidth.replace(" ", "");
				if (MathUtils.isInteger(smallerthanwidth)) {
					int i = Integer.parseInt(smallerthanwidth);
					if (MainWindowHandler.getWindowGuiWidth() > i) {
						continue;
					}
				}
			}
			String smallerthanheight = metas.get(0).getEntryValue("smallerthanheight");
			if (smallerthanheight != null) {
				smallerthanheight = smallerthanheight.replace(" ", "");
				if (MathUtils.isInteger(smallerthanheight)) {
					int i = Integer.parseInt(smallerthanheight);
					if (MainWindowHandler.getWindowGuiHeight() > i) {
						continue;
					}
				}
			}
			
			String randomMode = metas.get(0).getEntryValue("randommode");
			if ((randomMode != null) && randomMode.equalsIgnoreCase("true")) {
				
				String group = metas.get(0).getEntryValue("randomgroup");
				if (group == null) {
					group = defaultGroup;
				}
				if (!this.randomLayoutGroups.containsKey(group)) {
					this.randomLayoutGroups.put(group, new RandomLayoutContainer(group, this));
				}
				RandomLayoutContainer c = this.randomLayoutGroups.get(group);
				if (c != null) {
					String randomOnlyFirstTime = metas.get(0).getEntryValue("randomonlyfirsttime");
					if ((randomOnlyFirstTime != null) && randomOnlyFirstTime.equalsIgnoreCase("true")) {
						c.setOnlyFirstTime(true);
					}
					c.addLayout(s);
				}
				
			} else {
				
				this.normalLayouts.add(s);
				
			}
			
		}

		List<String> trashLayoutGroups = new ArrayList<String>();
		for (Map.Entry<String, RandomLayoutContainer> m : this.randomLayoutGroups.entrySet()) {
			if (m.getValue().getLayouts().isEmpty()) {
				trashLayoutGroups.add(m.getKey());
			}
		}
		for (String s : trashLayoutGroups) {
			this.randomLayoutGroups.remove(s);
		}

		//Applying customizations which needs to be done before other ones
		for (PropertiesSet s : this.normalLayouts) {
			for (PropertiesSection sec : s.getPropertiesOfType("customization")) {
				this.applyLayoutPre(sec, e);
			}
		}
		for (RandomLayoutContainer c : this.randomLayoutGroups.values()) {
			PropertiesSet s = c.getRandomLayout();
			if (s != null) {
				for (PropertiesSection sec : s.getPropertiesOfType("customization")) {
					this.applyLayoutPre(sec, e);
				}
			}
		}

		//Resetting scale in the same menu when scale customization action was removed
		if (!this.sharedLayoutProps.scaled) {
			if (scaleChangedIn != null) {
				scaleChangedIn = null;
				Window m = Minecraft.getInstance().getWindow();
				m.setGuiScale((double)mcscale);
				e.getScreen().width = m.getGuiScaledWidth();
				e.getScreen().height = m.getGuiScaledHeight();
			}
		}

		//Handle auto scaling
		if ((this.sharedLayoutProps.autoScaleBaseWidth != 0) && (this.sharedLayoutProps.autoScaleBaseHeight != 0)) {
			Window m = Minecraft.getInstance().getWindow();
			double guiWidth = e.getScreen().width * m.getGuiScale();
			double guiHeight = e.getScreen().height * m.getGuiScale();
			double percentX = (guiWidth / (double)this.sharedLayoutProps.autoScaleBaseWidth) * 100.0D;
			double percentY = (guiHeight / (double)this.sharedLayoutProps.autoScaleBaseHeight) * 100.0D;
			double newScaleX = (percentX / 100.0D) * m.getGuiScale();
			double newScaleY = (percentY / 100.0D) * m.getGuiScale();
			double newScale = Math.min(newScaleX, newScaleY);

			m.setGuiScale(newScale);
			e.getScreen().width = m.getGuiScaledWidth();
			e.getScreen().height = m.getGuiScaledHeight();
			this.sharedLayoutProps.scaled = true;
			scaleChangedIn = e.getScreen();
		}

	}

	//TODO übernehmen 1.19.4 (event ändern)
	protected void applyLayoutPre(PropertiesSection sec, InitOrResizeScreenEvent.Pre e) {
		
		String action = sec.getEntryValue("action");
		if (action != null) {
			String identifier = sec.getEntryValue("identifier");

			if (action.equalsIgnoreCase("overridemenu")) {
				if ((identifier != null) && CustomGuiLoader.guiExists(identifier)) {
					CustomGuiBase cus = CustomGuiLoader.getGui(identifier, (Screen)null, e.getScreen(), (onClose) -> {
						e.getScreen().removed();
					});
					Minecraft.getInstance().setScreen(cus);
				}
			}

			if (action.contentEquals("setscale")) {
				String scale = sec.getEntryValue("scale");
				if ((scale != null) && (MathUtils.isInteger(scale.replace(" ", "")) || MathUtils.isDouble(scale.replace(" ", "")))) {
					scaleChangedIn = e.getScreen();
					int newscale = (int) Double.parseDouble(scale.replace(" ", ""));
					if (newscale <= 0) {
						newscale = 1;
					}
					Window m = Minecraft.getInstance().getWindow();
					m.setGuiScale((double)newscale);
					e.getScreen().width = m.getGuiScaledWidth();
					e.getScreen().height = m.getGuiScaledHeight();
					this.sharedLayoutProps.scaled = true;
				}
			}

			if (action.equalsIgnoreCase("autoscale")) {
				String baseWidth = sec.getEntryValue("basewidth");
				if (MathUtils.isInteger(baseWidth)) {
					this.sharedLayoutProps.autoScaleBaseWidth = Integer.parseInt(baseWidth);
				}
				String baseHeight = sec.getEntryValue("baseheight");
				if (MathUtils.isInteger(baseHeight)) {
					this.sharedLayoutProps.autoScaleBaseHeight = Integer.parseInt(baseHeight);
				}
			}
		}
		
	}

	@SubscribeEvent
	public void onButtonsCached(ButtonCachedEvent e) {

		if (e.getScreen() != Minecraft.getInstance().screen) {
			return;
		}
		if (!MenuCustomization.isValidScreen(e.getScreen())) {
			return;
		}
		if (!this.shouldCustomize(e.getScreen())) {
			return;
		}
		if (!AnimationHandler.isReady()) {
			return;
		}
		if (!GameIntroHandler.introDisplayed) {
			return;
		}
		if (LayoutEditorScreen.isActive) {
			return;
		}
		if (ButtonCache.isCaching()) {
			return;
		}
		if (!MenuCustomization.isMenuCustomizable(e.getScreen())) {
			return;
		}

		if (!this.preinit) {
			System.out.println("################ WARNING [FANCYMENU] ################");
			System.out.println("MenuHandler pre-init skipped! Trying to re-initialize menu!");
			System.out.println("Menu Type: " + e.getScreen().getClass().getName());
			System.out.println("Menu Handler: " + this.getClass().getName());
			System.out.println("This probably happened because a mod has overridden a menu with this one.");
			System.out.println("#####################################################");
			e.getScreen().init(Minecraft.getInstance(), e.getScreen().width, e.getScreen().height);
			return;
		}

		this.hidden.clear();
		this.delayAppearanceVanilla.clear();
		this.fadeInVanilla.clear();
		this.vanillaButtonCustomizations.clear();
		this.vanillaButtonVisibilityRequirementContainers.clear();
		this.audio.clear();
		this.frontRenderItems.clear();
		this.backgroundRenderItems.clear();
		this.panoramacube = null;
		this.slideshow = null;
		this.customMenuBackground = null;
		this.backgroundAnimation = null;
		this.backgroundAnimations.clear();
		if ((this.backgroundAnimation != null) && (this.backgroundAnimation instanceof AdvancedAnimation)) {
			((AdvancedAnimation)this.backgroundAnimation).stopAudio();
		}
		this.backgroundDrawable = false;

		for (PropertiesSet s : this.normalLayouts) {
			List<PropertiesSection> metas = s.getPropertiesOfType("customization-meta");
			if (metas.isEmpty()) {
				metas = s.getPropertiesOfType("type-meta");
			}
			String renderOrder = metas.get(0).getEntryValue("renderorder");
			for (PropertiesSection sec : s.getPropertiesOfType("customization")) {
				this.applyLayout(sec, renderOrder, e);
			}
		}
		for (RandomLayoutContainer c : this.randomLayoutGroups.values()) {
			PropertiesSet s = c.getRandomLayout();
			if (s != null) {
				List<PropertiesSection> metas = s.getPropertiesOfType("customization-meta");
				if (metas.isEmpty()) {
					metas = s.getPropertiesOfType("type-meta");
				}
				String renderOrder = metas.get(0).getEntryValue("renderorder");
				for (PropertiesSection sec : s.getPropertiesOfType("customization")) {
					this.applyLayout(sec, renderOrder, e);
				}
			}
		}

		MenuHandlerRegistry.setActiveHandler(this.getMenuIdentifier());



		for (Map.Entry<ButtonData, String> m : this.sharedLayoutProps.descriptions.entrySet()) {
			AbstractWidget w = m.getKey().getButton();
			if (w != null) {
				VanillaButtonDescriptionHandler.setDescriptionFor(w, m.getValue());
			}
		}
		
		for (String s : MenuCustomization.getSounds()) {
			if (!this.audio.containsKey(s) && !s.equals(this.openAudio) && !s.equals(this.closeAudio)) {
				SoundHandler.stopSound(s);
				SoundHandler.resetSound(s);
			}
		}

		if (!this.sharedLayoutProps.closeAudioSet && (this.closeAudio != null)) {
			MenuCustomization.unregisterSound(this.closeAudio);
			this.closeAudio = null;
		}

		if (!this.sharedLayoutProps.openAudioSet && (this.openAudio != null)) {
			MenuCustomization.unregisterSound(this.openAudio);
			this.openAudio = null;
		}

		for (Map.Entry<String, Boolean> m : this.audio.entrySet()) {
			SoundHandler.playSound(m.getKey());
			if (m.getValue()) {
				SoundHandler.setLooped(m.getKey(), true);
			}
		}

		if (!this.sharedLayoutProps.backgroundTextureSet) {
			this.backgroundTexture = null;
		}

		for (ButtonData d : this.hidden) {
			d.getButton().visible = false;
		}

		for (CustomizationItemBase i : this.frontRenderItems) {
			if (MenuCustomization.isNewMenu()) {
				this.handleAppearanceDelayFor(i);
			}
			if (i.orientation.equals("element") && (i.orientationElementIdentifier != null)) {
				i.orientationElement = this.getItemByActionId(i.orientationElementIdentifier);
			}
		}
		for (CustomizationItemBase i : this.backgroundRenderItems) {
			if (MenuCustomization.isNewMenu()) {
				this.handleAppearanceDelayFor(i);
			}
			if (i.orientation.equals("element") && (i.orientationElementIdentifier != null)) {
				i.orientationElement = this.getItemByActionId(i.orientationElementIdentifier);
			}
		}

		//Handle vanilla button visibility requirements
		for (Map.Entry<AbstractWidget, VisibilityRequirementContainer> m : this.vanillaButtonVisibilityRequirementContainers.entrySet()) {
			boolean isBtnHidden = false;
			for (ButtonData d : this.hidden) {
				if (d.getButton() == m.getKey()) {
					isBtnHidden = true;
					break;
				}
			}
			if (!isBtnHidden) {
				PropertiesSection dummySec = new PropertiesSection("customization");
				dummySec.addEntry("action", "vanilla_button_visibility_requirements");
				ButtonData btn = null;
				for (ButtonData d : ButtonCache.getButtons()) {
					if (d.getButton() == m.getKey()) {
						btn = d;
						break;
					}
				}
				if (btn != null) {
					VanillaButtonCustomizationItem i = new VanillaButtonCustomizationItem(dummySec, btn, this);
					i.visibilityRequirements = m.getValue();
					this.backgroundRenderItems.add(i);
				}
			}
		}

		for (Map.Entry<ButtonData, Float> m : this.delayAppearanceVanilla.entrySet()) {
			if (!hidden.contains(m.getKey())) {
				if (this.visibilityRequirementsMet(m.getKey().getButton())) {
					this.handleVanillaAppearanceDelayFor(m.getKey());
				}
			}
		}

		//Cache custom buttons
		ButtonCache.clearCustomButtonCache();
		for (CustomizationItemBase c : this.backgroundRenderItems) {
			if (c instanceof ButtonCustomizationItem) {
				ButtonCache.cacheCustomButton(c.getActionId(), ((ButtonCustomizationItem) c).button);
			}
		}
		for (CustomizationItemBase c : this.frontRenderItems) {
			if (c instanceof ButtonCustomizationItem) {
				ButtonCache.cacheCustomButton(c.getActionId(), ((ButtonCustomizationItem) c).button);
			}
		}

	}

	protected void applyLayout(PropertiesSection sec, String renderOrder, ButtonCachedEvent e) {

		String action = sec.getEntryValue("action");
		if (action != null) {
			String identifier = sec.getEntryValue("identifier");
			AbstractWidget b = null;
			ButtonData bd = null;
			if (identifier != null) {
				bd = getButton(identifier);
				if (bd != null) {
					b = bd.getButton();
				}
			}

			if (action.equalsIgnoreCase("backgroundoptions")) {
				String keepAspect = sec.getEntryValue("keepaspectratio");
				if ((keepAspect != null) && keepAspect.equalsIgnoreCase("true")) {
					this.keepBackgroundAspectRatio = true;
				}
			}

			if (action.equalsIgnoreCase("setbackgroundslideshow")) {
				String name = sec.getEntryValue("name");
				if (name != null) {
					if (SlideshowHandler.slideshowExists(name)) {
						this.slideshow = SlideshowHandler.getSlideshow(name);
					}
				}
			}
			
			if (action.equalsIgnoreCase("setbackgroundpanorama")) {
				String name = sec.getEntryValue("name");
				if (name != null) {
					if (PanoramaHandler.panoramaExists(name)) {
						this.panoramacube = PanoramaHandler.getPanorama(name);
					}
				}
			}
			
			if (action.equalsIgnoreCase("texturizebackground")) {
				String value = CustomizationItemBase.fixBackslashPath(sec.getEntryValue("path"));
				String pano = sec.getEntryValue("wideformat");
				if (pano == null) {
					pano = sec.getEntryValue("panorama");
				}
				if (value != null) {
					File f = new File(value.replace("\\", "/"));
					if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
						value = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + value.replace("\\", "/");
						f = new File(value);
					}
					if (f.exists() && f.isFile() && (f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".jpeg") || f.getName().toLowerCase().endsWith(".png"))) {
						if ((this.backgroundTexture == null) || !this.backgroundTexture.getPath().equals(value)) {
							this.backgroundTexture = TextureHandler.getResource(value);
						}
						if ((pano != null) && pano.equalsIgnoreCase("true")) {
							this.panoramaback = true;
						} else {
							this.panoramaback = false;
						}
						this.sharedLayoutProps.backgroundTextureSet = true;
					}
				}
			}

			if (action.equalsIgnoreCase("animatebackground")) {
				String value = sec.getEntryValue("name");
				String random = sec.getEntryValue("random");
				boolean ran = false;
				if ((random != null) && random.equalsIgnoreCase("true")) {
					ran = true;
				}
				boolean restartOnLoad = false;
				String restartOnLoadString = sec.getEntryValue("restart_on_load");
				if ((restartOnLoadString != null) && restartOnLoadString.equalsIgnoreCase("true")) {
					restartOnLoad = true;
				}
				if (value != null) {
					if (value.contains(",")) {
						for (String s2 : value.split("[,]")) {
							int i = 0;
							for (char c : s2.toCharArray()) {
								if (c != " ".charAt(0)) {
									break;
								}
								i++;
							}
							if (i > s2.length()) {
								continue;
							}
							String temp = new StringBuilder(s2.substring(i)).reverse().toString();
							int i2 = 0;
							for (char c : temp.toCharArray()) {
								if (c != " ".charAt(0)) {
									break;
								}
								i2++;
							}
							String name = new StringBuilder(temp.substring(i2)).reverse().toString();
							if (AnimationHandler.animationExists(name)) {
								this.backgroundAnimations.add(AnimationHandler.getAnimation(name));
							}
						}
					} else {
						if (AnimationHandler.animationExists(value)) {
							this.backgroundAnimations.add(AnimationHandler.getAnimation(value));
						}
					}

					if (!this.backgroundAnimations.isEmpty()) {
						if (restartOnLoad && MenuCustomization.isNewMenu()) {
							for (IAnimationRenderer r : this.backgroundAnimations) {
								r.resetAnimation();
							}
						}
						if (ran) {
							if (MenuCustomization.isNewMenu()) {
								this.backgroundAnimationId = MathUtils.getRandomNumberInRange(0, this.backgroundAnimations.size()-1);
							}
							this.backgroundAnimation = this.backgroundAnimations.get(this.backgroundAnimationId);
						} else {
							if ((this.lastBackgroundAnimation != null) && this.backgroundAnimations.contains(this.lastBackgroundAnimation)) {
								this.backgroundAnimation = this.lastBackgroundAnimation;
							} else {
								this.backgroundAnimationId = 0;
								this.backgroundAnimation = this.backgroundAnimations.get(0);
							}
							this.lastBackgroundAnimation = this.backgroundAnimation;
						}
					}
				}
			}

			//Custom background handling (API)
			if (action.equalsIgnoreCase("api:custombackground")) {
				String typeId = sec.getEntryValue("type_identifier");
				String backId = sec.getEntryValue("background_identifier");
				String inputString = sec.getEntryValue("input_string");
				if (typeId != null) {
					MenuBackgroundType type = MenuBackgroundTypeRegistry.getBackgroundTypeByIdentifier(typeId);
					if (type != null) {
						if (type.needsInputString() && (inputString != null)) {
							try {
								this.customMenuBackground = type.createInstanceFromInputString(inputString);
							} catch (Exception ex) {
								ex.printStackTrace();
							}
							if (this.customMenuBackground != null) {
								if (MenuCustomization.isNewMenu()) {
									this.customMenuBackground.onOpenMenu();
								}
							}
						} else if (backId != null) {
							this.customMenuBackground = type.getBackgroundByIdentifier(backId);
							if (this.customMenuBackground != null) {
								if (MenuCustomization.isNewMenu()) {
									this.customMenuBackground.onOpenMenu();
								}
							}
						}
					}
				}
			}

			if (action.equalsIgnoreCase("hidebuttonfor")) {
				String time = sec.getEntryValue("seconds");
				String onlyfirsttime = sec.getEntryValue("onlyfirsttime");
				String fadein = sec.getEntryValue("fadein");
				String fadeinspeed = sec.getEntryValue("fadeinspeed");
				if (b != null) {
					if (MenuCustomization.isNewMenu()) {
						boolean ft = false;
						if ((onlyfirsttime != null) && onlyfirsttime.equalsIgnoreCase("true")) {
							ft = true;
						}
						if ((time != null) && MathUtils.isFloat(time)) {
							if (!ft || !this.delayAppearanceFirstTimeVanilla.contains(bd.getId())) {
								this.delayAppearanceVanilla.put(bd, Float.parseFloat(time));
							}
						}
						if (ft) {
							if (!this.delayAppearanceFirstTimeVanilla.contains(bd.getId())) {
								this.delayAppearanceFirstTimeVanilla.add(bd.getId());
							}
						}
						if ((fadein != null) && fadein.equalsIgnoreCase("true")) {
							float speed = 1.0F;
							if ((fadeinspeed != null) && MathUtils.isFloat(fadeinspeed)) {
								speed = Float.parseFloat(fadeinspeed);
							}
							this.fadeInVanilla.put(bd, speed);
						}
					}
				}
			}

			if (action.equalsIgnoreCase("hidebutton")) {
				if (b != null) {
					this.hidden.add(bd);
				}
			}

			if (action.equalsIgnoreCase("renamebutton") || action.equalsIgnoreCase("setbuttonlabel")) {
				if (b != null) {
					backgroundRenderItems.add(new VanillaButtonCustomizationItem(sec, bd, this));
				}
			}

			if (action.equalsIgnoreCase("resizebutton")) {
				if (b != null) {
					backgroundRenderItems.add(new VanillaButtonCustomizationItem(sec, bd, this));
				}
			}

			if (action.equalsIgnoreCase("movebutton")) {
				if (b != null) {
					backgroundRenderItems.add(new VanillaButtonCustomizationItem(sec, bd, this));
				}
			}

			if (action.equalsIgnoreCase("setbuttontexture")) {
				if (b != null) {
					String loopBackAnimations = sec.getEntryValue("loopbackgroundanimations");
					if ((loopBackAnimations != null) && loopBackAnimations.equalsIgnoreCase("false")) {
						this.getContainerForVanillaButton(b).loopAnimation = false;
					}
					String restartBackAnimationsOnHover = sec.getEntryValue("restartbackgroundanimations");
					if ((restartBackAnimationsOnHover != null) && restartBackAnimationsOnHover.equalsIgnoreCase("false")) {
						this.getContainerForVanillaButton(b).restartAnimationOnHover = false;
					}
					String backNormal = CustomizationItemBase.fixBackslashPath(sec.getEntryValue("backgroundnormal"));
					String backHover = CustomizationItemBase.fixBackslashPath(sec.getEntryValue("backgroundhovered"));
					if (backNormal != null) {
						this.getContainerForVanillaButton(b).normalBackground = backNormal;
					} else {
						String backAniNormal = sec.getEntryValue("backgroundanimationnormal");
						if (backAniNormal != null) {
							this.getContainerForVanillaButton(b).normalBackground = "animation:" + backAniNormal;
						}
					}
					if (backHover != null) {
						this.getContainerForVanillaButton(b).hoverBackground = backHover;
					} else {
						String backAniHover = sec.getEntryValue("backgroundanimationhovered");
						if (backAniHover != null) {
							this.getContainerForVanillaButton(b).hoverBackground = "animation:" + backAniHover;
						}
					}
				}
			}

			if (action.equalsIgnoreCase("setbuttonclicksound")) {
				if (b != null) {
					String path = CustomizationItemBase.fixBackslashPath(sec.getEntryValue("path"));
					if (path != null) {
						this.getContainerForVanillaButton(b).clickSound = path;
					}
				}
			}

			if (action.equalsIgnoreCase("vanilla_button_visibility_requirements")) {
				if (b != null) {
					this.vanillaButtonVisibilityRequirementContainers.put(b, new VanillaButtonCustomizationItem(sec, bd, this).visibilityRequirementContainer);
				}
			}

			if (action.equalsIgnoreCase("addhoversound")) {
				if (b != null) {
					if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
						backgroundRenderItems.add(new VanillaButtonCustomizationItem(sec, bd, this));
					} else {
						frontRenderItems.add(new VanillaButtonCustomizationItem(sec, bd, this));
					}
				}
			}

			if (action.equalsIgnoreCase("sethoverlabel")) {
				if (b != null) {
					if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
						backgroundRenderItems.add(new VanillaButtonCustomizationItem(sec, bd, this));
					} else {
						frontRenderItems.add(new VanillaButtonCustomizationItem(sec, bd, this));
					}
				}
			}

			if (action.equalsIgnoreCase("clickbutton")) {
				if (b != null) {
					String clicks = sec.getEntryValue("clicks");
					if ((clicks != null) && (MathUtils.isInteger(clicks))) {
						for (int i = 0; i < Integer.parseInt(clicks); i++) {
							b.onClick(MouseInput.getMouseX(), MouseInput.getMouseY());
						}
					}
				}
			}
			
			/** CUSTOM ITEMS **/

			if (action.equalsIgnoreCase("addtext")) {
				if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
					backgroundRenderItems.add(new StringCustomizationItem(sec));
				} else {
					frontRenderItems.add(new StringCustomizationItem(sec));
				}
			}

			if (action.equalsIgnoreCase("addwebtext")) {
				if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
					backgroundRenderItems.add(new WebStringCustomizationItem(sec));
				} else {
					frontRenderItems.add(new WebStringCustomizationItem(sec));
				}
			}

			if (action.equalsIgnoreCase("addtexture")) {
				if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
					backgroundRenderItems.add(new TextureCustomizationItem(sec));
				} else {
					frontRenderItems.add(new TextureCustomizationItem(sec));
				}
			}

			if (action.equalsIgnoreCase("addwebtexture")) {
				if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
					backgroundRenderItems.add(new WebTextureCustomizationItem(sec));
				} else {
					frontRenderItems.add(new WebTextureCustomizationItem(sec));
				}
			}

			if (action.equalsIgnoreCase("addanimation")) {
				if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
					backgroundRenderItems.add(new AnimationCustomizationItem(sec));
				} else {
					frontRenderItems.add(new AnimationCustomizationItem(sec));
				}
			}

			if (action.equalsIgnoreCase("addshape")) {
				if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
					backgroundRenderItems.add(new ShapeCustomizationItem(sec));
				} else {
					frontRenderItems.add(new ShapeCustomizationItem(sec));
				}
			}

			if (action.equalsIgnoreCase("addslideshow")) {
				if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
					backgroundRenderItems.add(new SlideshowCustomizationItem(sec));
				} else {
					frontRenderItems.add(new SlideshowCustomizationItem(sec));
				}
			}

			if (action.equalsIgnoreCase("addbutton")) {
				ButtonCustomizationItem i = new ButtonCustomizationItem(sec);

				if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
					backgroundRenderItems.add(i);
				} else {
					frontRenderItems.add(i);
				}
			}

			if (action.equalsIgnoreCase("addaudio")) {
				if (FancyMenu.config.getOrDefault("playbackgroundsounds", true)) {
					if ((Minecraft.getInstance().level == null) || FancyMenu.config.getOrDefault("playbackgroundsoundsinworld", false)) {
						String path = CustomizationItemBase.fixBackslashPath(sec.getEntryValue("path"));
						String loopString = sec.getEntryValue("loop");

						boolean loop = false; 
						if ((loopString != null) && loopString.equalsIgnoreCase("true")) {
							loop = true;
						}
						if (path != null) {
							File f = new File(path);
							if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
								path = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + path;
								f = new File(path);
							}
							if (f.isFile() && f.exists() && f.getName().endsWith(".wav")) {
								try {
									String name = path + Files.size(f.toPath());
									MenuCustomization.registerSound(name, path);
									this.audio.put(name, loop);
								} catch (Exception ex) {
									ex.printStackTrace();
								}
							}
						}
					}
				}
			}
			
			if (action.equalsIgnoreCase("setcloseaudio")) {
				String path = CustomizationItemBase.fixBackslashPath(sec.getEntryValue("path"));

				if (path != null) {
					File f = new File(path);
					if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
						path = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + path;
						f = new File(path);
					}
					if (f.isFile() && f.exists() && f.getName().endsWith(".wav")) {
						try {
							String name = "closesound_" + path + Files.size(f.toPath());
							MenuCustomization.registerSound(name, path);
							this.closeAudio = name;
							this.sharedLayoutProps.closeAudioSet = true;
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			}

			if (action.equalsIgnoreCase("setopenaudio")) {
				if (MenuCustomization.isNewMenu()) {
					String path = CustomizationItemBase.fixBackslashPath(sec.getEntryValue("path"));
					if (path != null) {
						File f = new File(path);
						if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
							path = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + path;
							f = new File(path);
						}
						if (f.isFile() && f.exists() && f.getName().endsWith(".wav")) {
							try {
								String name = "opensound_" + path + Files.size(f.toPath());
								MenuCustomization.registerSound(name, path);
								SoundHandler.resetSound(name);
								SoundHandler.playSound(name);
								this.openAudio = name;
								this.sharedLayoutProps.openAudioSet = true;
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}
					}
				}
			}

			if (action.equalsIgnoreCase("setbuttondescription")) {
				if (b != null) {
					String desc = sec.getEntryValue("description");
					if (desc != null) {
						this.sharedLayoutProps.descriptions.put(bd, de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser.replacePlaceholders(desc));
					}
				}
			}
			
			if (action.equalsIgnoreCase("addsplash")) {
				String file = CustomizationItemBase.fixBackslashPath(sec.getEntryValue("splashfilepath"));
				String text = sec.getEntryValue("text");
				if ((file != null) || (text != null)) {
					
					SplashTextCustomizationItem i = new SplashTextCustomizationItem(sec);
					
					if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
						backgroundRenderItems.add(i);
					} else {
						frontRenderItems.add(i);
					}
					
				}
			}

			/** CUSTOM ITEMS (API) **/
			if (action.startsWith("custom_layout_element:")) {
				String cusId = action.split("[:]", 2)[1];
				CustomizationItemContainer cusItem = CustomizationItemRegistry.getItem(cusId);
				if (cusItem != null) {
					CustomizationItem cusItemInstance = cusItem.constructCustomizedItemInstance(sec);
					if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
						backgroundRenderItems.add(cusItemInstance);
					} else {
						frontRenderItems.add(cusItemInstance);
					}
				}
			}

		}
		
	}

	protected void handleAppearanceDelayFor(CustomizationItemBase i) {
		if (!(i instanceof VanillaButtonCustomizationItem)) {
			if (i.delayAppearance) {
				if (i.getActionId() == null) {
					return;
				}
				if (!i.delayAppearanceEverytime && delayAppearanceFirstTime.contains(i.getActionId())) {
					return;
				}
				if (!i.delayAppearanceEverytime) {
					if (!this.delayAppearanceFirstTime.contains(i.getActionId())) {
						delayAppearanceFirstTime.add(i.getActionId());
					}
				}
				
				i.visible = false;
				
				if (i.fadeIn) {
					i.opacity = 0.1F;
				}
				
				ThreadCaller c = new ThreadCaller();
				this.delayThreads.add(c);
				
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						long start = System.currentTimeMillis();
						float delay = (float) (1000.0 * i.delayAppearanceSec);
						boolean fade = false;
						while (c.running.get()) {
							try {
								long now = System.currentTimeMillis();
								if (!fade) {
									if (now >= start + (int)delay) {
										i.visible = true;
										if (!i.fadeIn) {
											return;
										} else {
											fade = true;
										}
									}
								} else {
									float o = i.opacity + (0.03F * i.fadeInSpeed);
									if (o > 1.0F) {
										o = 1.0F;
									}
									if (i.opacity < 1.0F) {
										i.opacity = o;
									} else {
										return;
									}
								}
								
								Thread.sleep(50);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				});
				t.start();
				
			}
		}
	}

	protected void handleVanillaAppearanceDelayFor(ButtonData d) {
		if (this.delayAppearanceVanilla.containsKey(d)) {
			
			boolean fadein = this.fadeInVanilla.containsKey(d);
			float delaysec = this.delayAppearanceVanilla.get(d);

			VisibilityRequirementContainer vis = this.vanillaButtonVisibilityRequirementContainers.get(d.getButton());
			
			d.getButton().visible = false;
			if (vis != null) {
				vis.forceHide = true;
			}
			
			if (fadein) {
				d.getButton().setAlpha(0.1F);
			}
			
			ThreadCaller c = new ThreadCaller();
			this.delayThreads.add(c);
			
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					float fadespeed = 1.0F;
					if (fadein) {
						if (fadeInVanilla.containsKey(d)) {
							fadespeed = fadeInVanilla.get(d);
						}
					}
					float opacity = 0.1F;
					long start = System.currentTimeMillis();
					float delay = (float) (1000.0 * delaysec);
					boolean fade = false;
					while (c.running.get()) {
						try {
							long now = System.currentTimeMillis();
							if (!fade) {
								if (now >= start + (int)delay) {
									d.getButton().visible = true;
									if (vis != null) {
										vis.forceHide = false;
									}
									if (!fadein) {
										return;
									} else {
										fade = true;
									}
								}
							} else {
								float o = opacity + (0.03F * fadespeed);
								if (o > 1.0F) {
									o = 1.0F;
								}
								if (opacity < 1.0F) {
									opacity = o;
									d.getButton().setAlpha(opacity);
								} else {
									return;
								}
							}
							
							Thread.sleep(50);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});
			t.start();
			
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onRenderPre(ScreenEvent.Render.Pre e) {

		if (PopupHandler.isPopupActive()) {
			return;
		}
		if (!this.shouldCustomize(e.getScreen())) {
			return;
		}
		if (!MenuCustomization.isMenuCustomizable(e.getScreen())) {
			return;
		}

		//Re-init screen if layout-wide requirements changed
		for (Map.Entry<VisibilityRequirementContainer, Boolean> m : this.cachedLayoutWideRequirements.entrySet()) {
			if (m.getKey().isVisible() != m.getValue()) {
				e.getScreen().init(Minecraft.getInstance(), e.getScreen().width, e.getScreen().height);
				break;
			}
		}

	}

	@SubscribeEvent
	public void onRenderPost(ScreenEvent.Render.Post e) {
		if (PopupHandler.isPopupActive()) {
			return;
		}
		if (!this.shouldCustomize(e.getScreen())) {
			return;
		}
		if (!MenuCustomization.isMenuCustomizable(e.getScreen())) {
			return;
		}

		if (this.customMenuTitle != null) {
			e.getScreen().title = Component.literal(PlaceholderParser.replacePlaceholders(this.customMenuTitle));
		}

		if (!this.backgroundDrawable) {
			//Rendering all items that SHOULD be rendered in the background IF it's not possible to render them in the background (In this case, they will be forced to render in the foreground)
			List<CustomizationItemBase> backItems = new ArrayList<CustomizationItemBase>();
			backItems.addAll(this.backgroundRenderItems);
			for (CustomizationItemBase i : backItems) {
				try {
					i.render(e.getPoseStack(), e.getScreen());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		//Rendering all items that should be rendered in the foreground
		List<CustomizationItemBase> frontItems = new ArrayList<CustomizationItemBase>();
		frontItems.addAll(this.frontRenderItems);
		for (CustomizationItemBase i : frontItems) {
			try {
				i.render(e.getPoseStack(), e.getScreen());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@SubscribeEvent
	public void drawToBackground(ScreenEvent.BackgroundRendered e) {
		if (!MenuCustomization.isCurrentMenuScrollable()) {
			this.renderBackground(e.getPoseStack(), e.getScreen());
		}
	}

	protected void renderBackground(PoseStack matrix, Screen s) {
		if (this.shouldCustomize(s)) {
			if (!MenuCustomization.isMenuCustomizable(s)) {
				return;
			}

			//Rendering the background animation to the menu
			if (this.canRenderBackground()) {
				if ((this.backgroundAnimation != null) && this.backgroundAnimation.isReady()) {
					boolean b = this.backgroundAnimation.isStretchedToStreensize();
					int wOri = this.backgroundAnimation.getWidth();
					int hOri = this.backgroundAnimation.getHeight();
					int xOri = this.backgroundAnimation.getPosX();
					int yOri = this.backgroundAnimation.getPosY();
					if (!this.keepBackgroundAspectRatio) {
						this.backgroundAnimation.setStretchImageToScreensize(true);
					} else {
						double ratio = (double) wOri / (double) hOri;
						int wfinal = (int)(s.height * ratio);
						int screenCenterX = s.width / 2;
						if (wfinal < s.width) {
							this.backgroundAnimation.setStretchImageToScreensize(true);
						} else {
							this.backgroundAnimation.setWidth(wfinal + 1);
							this.backgroundAnimation.setHeight(s.height + 1);
							this.backgroundAnimation.setPosX(screenCenterX - (wfinal / 2));
							this.backgroundAnimation.setPosY(0);
						}
					}
					this.backgroundAnimation.render(CurrentScreenHandler.getPoseStack());
					this.backgroundAnimation.setWidth(wOri);
					this.backgroundAnimation.setHeight(hOri);
					this.backgroundAnimation.setPosX(xOri);
					this.backgroundAnimation.setPosY(yOri);
					this.backgroundAnimation.setStretchImageToScreensize(b);
				} else if (this.backgroundTexture != null) {
					RenderSystem.enableBlend();
					RenderUtils.bindTexture(this.backgroundTexture.getResourceLocation());

					if (!this.panoramaback) {
						if (!this.keepBackgroundAspectRatio) {
							GuiComponent.blit(CurrentScreenHandler.getPoseStack(), 0, 0, 1.0F, 1.0F, s.width + 1, s.height + 1, s.width + 1, s.height + 1);
						} else {
							int w = this.backgroundTexture.getWidth();
							int h = this.backgroundTexture.getHeight();
							double ratio = (double) w / (double) h;
							int wfinal = (int)(s.height * ratio);
							int screenCenterX = s.width / 2;
							if (wfinal < s.width) {
								GuiComponent.blit(CurrentScreenHandler.getPoseStack(), 0, 0, 1.0F, 1.0F, s.width + 1, s.height + 1, s.width + 1, s.height + 1);
							} else {
								GuiComponent.blit(CurrentScreenHandler.getPoseStack(), screenCenterX - (wfinal / 2), 0, 1.0F, 1.0F, wfinal + 1, s.height + 1, wfinal + 1, s.height + 1);
							}
						}
					} else {
						int w = this.backgroundTexture.getWidth();
						int h = this.backgroundTexture.getHeight();
						double ratio = (double) w / (double) h;
						int wfinal = (int)(s.height * ratio);

						//Check if the panorama background should move to the left side or to the ride side
						if ((panoPos + (wfinal - s.width)) <= 0) {
							panoMoveBack = true;
						}
						if (panoPos >= 0) {
							panoMoveBack = false;
						}

						//Fix pos after resizing
						if (panoPos + (wfinal - s.width) < 0) {
							panoPos = 0 - (wfinal - s.width);
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

								if (panoPos + (wfinal - s.width) == 0) {
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
						if (wfinal <= s.width) {
							GuiComponent.blit(CurrentScreenHandler.getPoseStack(), 0, 0, 1.0F, 1.0F, s.width + 1, s.height + 1, s.width + 1, s.height + 1);
						} else {
							RenderUtils.doubleBlit(panoPos, 0, 1.0F, 1.0F, wfinal, s.height + 1);
						}
					}

					RenderSystem.disableBlend();

				} else if (this.panoramacube != null) {
					
					this.panoramacube.render();

				} else if (this.slideshow != null) {
					int sw = this.slideshow.width;
					int sh = this.slideshow.height;
					int sx = this.slideshow.x;
					int sy = this.slideshow.y;

					if (!this.keepBackgroundAspectRatio) {
						this.slideshow.width = s.width + 1;
						this.slideshow.height = s.height +1;
						this.slideshow.x = 0;
					} else {
						double ratio = (double) sw / (double) sh;
						int wfinal = (int)(s.height * ratio);
						int screenCenterX = s.width / 2;
						if (wfinal < s.width) {
							this.slideshow.width = s.width + 1;
							this.slideshow.height = s.height +1;
							this.slideshow.x = 0;
						} else {
							this.slideshow.width = wfinal + 1;
							this.slideshow.height = s.height +1;
							this.slideshow.x = screenCenterX - (wfinal / 2);
						}
					}
					this.slideshow.y = 0;

					this.slideshow.render(matrix);
					
					this.slideshow.width = sw;
					this.slideshow.height = sh;
					this.slideshow.x = sx;
					this.slideshow.y = sy;
				} else if (this.customMenuBackground != null) {

					this.customMenuBackground.render(matrix, s, this.keepBackgroundAspectRatio);

				}
			}

			if (PopupHandler.isPopupActive()) {
				return;
			}

			//Rendering all items which should be rendered in the background
			List<CustomizationItemBase> backItems = new ArrayList<CustomizationItemBase>();
			backItems.addAll(this.backgroundRenderItems);
			for (CustomizationItemBase i : backItems) {
				try {
					i.render(CurrentScreenHandler.getPoseStack(), s);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}

			this.backgroundDrawable = true;
		}
	}

	@SubscribeEvent
	public void onButtonClickSound(PlayWidgetClickSoundEvent.Pre e) {
		
		if (this.shouldCustomize(Minecraft.getInstance().screen)) {
			if (MenuCustomization.isMenuCustomizable(Minecraft.getInstance().screen)) {

				ButtonCustomizationContainer c = this.vanillaButtonCustomizations.get(e.getWidget());

				if (c != null) {
					if (c.clickSound != null) {
						File f = new File(c.clickSound);
						if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
							c.clickSound = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + c.clickSound;
							f = new File(c.clickSound);
						}
						if (f.exists() && f.isFile() && f.getPath().toLowerCase().endsWith(".wav")) {

							SoundHandler.registerSound(f.getPath(), f.getPath());
							SoundHandler.resetSound(f.getPath());
							SoundHandler.playSound(f.getPath());

							e.setCanceled(true);

						}
					}
				}
				
			}
		}

	}

	@SubscribeEvent
	public void onButtonRenderBackground(RenderWidgetBackgroundEvent.Pre e) {
		if (this.shouldCustomize(Minecraft.getInstance().screen)) {
			if (MenuCustomization.isMenuCustomizable(Minecraft.getInstance().screen)) {

				AbstractWidget w = e.getWidget();
				ButtonCustomizationContainer c = this.vanillaButtonCustomizations.get(w);
				if (c != null) {
					String normalBack = c.normalBackground;
					String hoverBack = c.hoverBackground;
					boolean hasCustomBackground = false;
					boolean restart = false;
					if (c.lastHoverState != w.isHoveredOrFocused()) {
						if (w.isHoveredOrFocused() && c.restartAnimationOnHover) {
							restart = true;
						}
					}
					c.lastHoverState = w.isHoveredOrFocused();

					if (!w.isHoveredOrFocused()) {
						if (normalBack != null) {
							if (this.renderCustomButtomBackground(e, normalBack, restart)) {
								hasCustomBackground = true;
							}
						}
					}

					if (w.isHoveredOrFocused()) {
						if (w.active) {
							if (hoverBack != null) {
								if (this.renderCustomButtomBackground(e, hoverBack, restart)) {
									hasCustomBackground = true;
								}
							}
						} else {
							if (normalBack != null) {
								if (this.renderCustomButtomBackground(e, normalBack, restart)) {
									hasCustomBackground = true;
								}
							}
						}
					}

					if (hasCustomBackground) {
						if (w instanceof ImageButton) {
							Component msg = w.getMessage();
							if (msg != null) {
								GuiComponent.drawCenteredString(e.getPoseStack(), Minecraft.getInstance().font, msg, w.x + w.getWidth() / 2, w.y + (w.getHeight() - 8) / 2, w.getFGColor() | Mth.ceil(e.getAlpha() * 255.0F) << 24);
							}
						}

						e.setCanceled(true);
					}
				}

			}
		}
	}

	protected boolean renderCustomButtomBackground(RenderWidgetBackgroundEvent e, String background, boolean restartAnimationBackground) {
		AbstractWidget w = e.getWidget();
		PoseStack matrix = e.getPoseStack();
		ButtonCustomizationContainer c = this.vanillaButtonCustomizations.get(w);
		if (c != null) {
			if (w != null) {
				if (background != null) {
					if (background.startsWith("animation:")) {
						String aniName = background.split("[:]", 2)[1];
						if (AnimationHandler.animationExists(aniName)) {
							IAnimationRenderer a = AnimationHandler.getAnimation(aniName);
							if (restartAnimationBackground) {
								a.resetAnimation();
							}
							this.renderBackgroundAnimation(e, a);
							if (!c.cachedAnimations.contains(a)) {
								c.cachedAnimations.add(a);
							}
							return true;
						}
					} else {
						File f = new File(background);
						if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
							background = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + background;
							f = new File(background);
						}
						if (f.isFile()) {
							if (f.getPath().toLowerCase().endsWith(".gif")) {
								IAnimationRenderer a =  TextureHandler.getGifResource(f.getPath());
								if (restartAnimationBackground) {
									a.resetAnimation();
								}
								this.renderBackgroundAnimation(e, a);
								if (!c.cachedAnimations.contains(a)) {
									c.cachedAnimations.add(a);
								}
								return true;
							} else if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
								ExternalTextureResourceLocation back = TextureHandler.getResource(f.getPath());
								if (back != null) {
									RenderUtils.bindTexture(back.getResourceLocation());
									RenderSystem.enableBlend();
									RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, e.getAlpha());
									GuiComponent.blit(matrix, w.x, w.y, 0.0F, 0.0F, w.getWidth(), w.getHeight(), w.getWidth(), w.getHeight());
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	protected void renderBackgroundAnimation(RenderWidgetBackgroundEvent e, IAnimationRenderer ani) {
		AbstractWidget w = e.getWidget();
		ButtonCustomizationContainer c = this.vanillaButtonCustomizations.get(w);
		if (c != null) {
			if (ani != null) {
				if (!ani.isReady()) {
					ani.prepareAnimation();
				}

				int aniX = ani.getPosX();
				int aniY = ani.getPosY();
				int aniWidth = ani.getWidth();
				int aniHeight = ani.getHeight();
				boolean aniLoop = ani.isGettingLooped();

				ani.setPosX(w.x);
				ani.setPosY(w.y);
				ani.setWidth(w.getWidth());
				ani.setHeight(w.getHeight());
				ani.setLooped(c.loopAnimation);
				ani.setOpacity(e.getAlpha());
				if (ani instanceof AdvancedAnimation) {
					((AdvancedAnimation) ani).setMuteAudio(true);
				}

				ani.render(e.getPoseStack());

				ani.setPosX(aniX);
				ani.setPosY(aniY);
				ani.setWidth(aniWidth);
				ani.setHeight(aniHeight);
				ani.setLooped(aniLoop);
				ani.setOpacity(1.0F);
				if (ani instanceof AdvancedAnimation) {
					((AdvancedAnimation) ani).setMuteAudio(false);
				}
			}
		}
	}

	protected ButtonCustomizationContainer getContainerForVanillaButton(AbstractWidget w) {
		if (!this.vanillaButtonCustomizations.containsKey(w)) {
			ButtonCustomizationContainer c = new ButtonCustomizationContainer();
			this.vanillaButtonCustomizations.put(w, c);
			return c;
		}
		return this.vanillaButtonCustomizations.get(w);
	}

	public CustomizationItemBase getItemByActionId(String actionId) {
		for (CustomizationItemBase c : this.backgroundRenderItems) {
			if (c instanceof VanillaButtonCustomizationItem) {
				String id = "vanillabtn:" + ((VanillaButtonCustomizationItem)c).getButtonId();
				if (id.equals(actionId)) {
					return c;
				}
			} else {
				if (c.getActionId().equals(actionId)) {
					return c;
				}
			}
		}
		for (CustomizationItemBase c : this.frontRenderItems) {
			if (c instanceof VanillaButtonCustomizationItem) {
				String id = "vanillabtn:" + ((VanillaButtonCustomizationItem)c).getButtonId();
				if (id.equals(actionId)) {
					return c;
				}
			} else {
				if (c.getActionId().equals(actionId)) {
					return c;
				}
			}
		}
		if (actionId.startsWith("vanillabtn:")) {
			String idRaw = actionId.split("[:]", 2)[1];
			ButtonData d;
			if (MathUtils.isLong(idRaw)) {
				d = ButtonCache.getButtonForId(Long.parseLong(idRaw));
			} else {
				d = ButtonCache.getButtonForCompatibilityId(idRaw);
			}
			if ((d != null) && (d.getButton() != null)) {
				VanillaButtonCustomizationItem vb = new VanillaButtonCustomizationItem(new PropertiesSection("customization"), d, this);
				vb.orientation = "top-left";
				vb.posX = d.getButton().x;
				vb.posY = d.getButton().y;
				vb.width = d.getButton().getWidth();
				vb.height = d.getButton().getHeight();
				return vb;
			}
		}
		return null;
	}

	protected boolean visibilityRequirementsMet(AbstractWidget b) {
		VisibilityRequirementContainer c = this.vanillaButtonVisibilityRequirementContainers.get(b);
		if (c != null) {
			return c.isVisible();
		}
		return true;
	}

	@SubscribeEvent
	public void onRenderListBackground(RenderGuiListBackgroundEvent.Post e) {

		Screen s = Minecraft.getInstance().screen;
		
		if (this.shouldCustomize(s)) {
			if (MenuCustomization.isMenuCustomizable(s)) {

				//Allow background stuff to be rendered in scrollable GUIs
				if (Minecraft.getInstance().screen != null) {
					
					this.renderBackground(e.getPoseStack(), s);
					
				}

			}
		}

	}

	private static ButtonData getButton(String identifier) {
		if (identifier.startsWith("%id=")) {
			String p = identifier.split("[=]")[1].replace("%", "");
			if (MathUtils.isLong(p)) {
				return ButtonCache.getButtonForId(Long.parseLong(p));
			} else if (p.startsWith("button_compatibility_id:")) {
				return ButtonCache.getButtonForCompatibilityId(p);
			}
		} else {
			ButtonData b;
			if (I18n.exists(identifier)) {
				b = ButtonCache.getButtonForKey(identifier);
			} else {
				b = ButtonCache.getButtonForName(identifier);
			}
			return b;
		}
		return null;
	}

	protected boolean shouldCustomize(Screen menu) {
		if (menu == null) {
			return false;
		}
		if (getMenuIdentifier() != null) {
			if (!this.getMenuIdentifier().equals(menu.getClass().getName())) {
				return false;
			}
		}
		return true;
	}

	public boolean canRenderBackground() {
		return ((this.backgroundAnimation != null) || (this.backgroundTexture != null) || (this.panoramacube != null) || (this.slideshow != null) || (this.customMenuBackground != null));
	}

	public boolean setBackgroundAnimation(int id) {
		if (id < this.backgroundAnimations.size()) {
			this.backgroundAnimationId = id;
			this.backgroundAnimation = this.backgroundAnimations.get(id);
			this.lastBackgroundAnimation = this.backgroundAnimation;
			return true;
		}
		return false;
	}

	public int getCurrentBackgroundAnimationId() {
		return this.backgroundAnimationId;
	}

	public List<IAnimationRenderer> backgroundAnimations() {
		return this.backgroundAnimations;
	}

	private static class ThreadCaller {
		AtomicBoolean running = new AtomicBoolean(true);
	}

	public static class RandomLayoutContainer {
		
		public final String id;
		protected List<PropertiesSet> layouts = new ArrayList<PropertiesSet>();
		protected boolean onlyFirstTime = false;
		protected String lastLayoutPath = null;
		
		public MenuHandlerBase parent;
		
		public RandomLayoutContainer(String id, MenuHandlerBase parent) {
			this.id = id;
			this.parent = parent;
		}
		
		public List<PropertiesSet> getLayouts() {
			return this.layouts;
		}
		
		public void addLayout(PropertiesSet layout) {
			this.layouts.add(layout);
		}
		
		public void addLayouts(List<PropertiesSet> layouts) {
			this.layouts.addAll(layouts);
		}
		
		public void clearLayouts() {
			this.layouts.clear();
		}
		
		public void setOnlyFirstTime(boolean b) {
			this.onlyFirstTime = b;
		}
		
		public boolean isOnlyFirstTime() {
			return this.onlyFirstTime;
		}
		
		public void resetLastLayout() {
			this.lastLayoutPath = null;
		}
		
		@Nullable
		public PropertiesSet getRandomLayout() {
			if (!this.layouts.isEmpty()) {
				if ((this.onlyFirstTime || !MenuCustomization.isNewMenu()) && (this.lastLayoutPath != null)) {
					File f = new File(this.lastLayoutPath);
					if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
						this.lastLayoutPath = Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + this.lastLayoutPath;
						f = new File(this.lastLayoutPath);
					}
					if (f.exists()) {
						for (PropertiesSet s : this.layouts) {
							List<PropertiesSection> metas = s.getPropertiesOfType("customization-meta");
							if (metas.isEmpty()) {
								metas = s.getPropertiesOfType("type-meta");
							}
							if (metas.isEmpty()) {
								continue;
							}
							String path = metas.get(0).getEntryValue("path");
							if ((path != null) && path.equals(this.lastLayoutPath)) {
								return s;
							}
						}
					} else {
						MenuCustomization.stopSounds();
						MenuCustomization.resetSounds();
						AnimationHandler.resetAnimations();
						AnimationHandler.resetAnimationSounds();
						AnimationHandler.stopAnimationSounds();
					}
				}
				int i = MathUtils.getRandomNumberInRange(0, this.layouts.size()-1);
				PropertiesSet s = this.layouts.get(i);
				List<PropertiesSection> metas = s.getPropertiesOfType("customization-meta");
				if (metas.isEmpty()) {
					metas = s.getPropertiesOfType("type-meta");
				}
				if (!metas.isEmpty()) {
					String path = metas.get(0).getEntryValue("path");
					if ((path != null)) {
						this.lastLayoutPath = path;
						return s;
					}
				}
			}
			return null;
		}
		
	}

	public boolean isVanillaButtonDelayed(AbstractWidget w) {
		for (ButtonData d : this.delayAppearanceVanilla.keySet()) {
			if (d.getButton() == w) {
				return true;
			}
		}
		return false;
	}

	public boolean isVanillaButtonHidden(AbstractWidget w) {
		for (ButtonData d : this.hidden) {
			if (d.getButton() == w) {
				return true;
			}
		}
		return false;
	}

	public static class SharedLayoutProperties {
		
		public boolean scaled = false;
		public int autoScaleBaseWidth = 0;
		public int autoScaleBaseHeight = 0;
		public boolean backgroundTextureSet = false;
		public boolean openAudioSet = false;
		public boolean closeAudioSet = false;
		public Map<ButtonData, String> descriptions = new HashMap<ButtonData, String>();
		
	}

	public static class ButtonCustomizationContainer {

		public String normalBackground = null;
		public String hoverBackground = null;
		public boolean loopAnimation = true;
		public boolean restartAnimationOnHover = true;
		public String clickSound = null;
		public String hoverSound = null;
		public String hoverLabel = null;
		public int autoButtonClicks = 0;
		public String customButtonLabel = null;
		public String buttonDescription = null;
		public boolean isButtonHidden = false;
		public VisibilityRequirementContainer visibilityRequirementContainer = null;

		public List<IAnimationRenderer> cachedAnimations = new ArrayList<IAnimationRenderer>();
		public boolean lastHoverState = false;

	}

}

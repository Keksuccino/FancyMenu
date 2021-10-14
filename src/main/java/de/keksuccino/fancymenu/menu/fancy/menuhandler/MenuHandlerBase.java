package de.keksuccino.fancymenu.menu.fancy.menuhandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.PlayWidgetClickSoundEvent;
import de.keksuccino.fancymenu.events.RenderGuiListBackgroundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
import de.keksuccino.fancymenu.events.SoftMenuReloadEvent;
import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.button.VanillaButtonDescriptionHandler;
import de.keksuccino.fancymenu.menu.fancy.DynamicValueHelper;
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
import de.keksuccino.fancymenu.menu.fancy.item.playerentity.PlayerEntityCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.VisibilityRequirementContainer;
import de.keksuccino.fancymenu.menu.panorama.ExternalTexturePanoramaRenderer;
import de.keksuccino.fancymenu.menu.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.menu.slideshow.ExternalTextureSlideshowRenderer;
import de.keksuccino.fancymenu.menu.slideshow.SlideshowHandler;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.gui.content.widget.WidgetUtils;
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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MenuHandlerBase extends DrawableHelper {

	protected List<CustomizationItemBase> frontRenderItems = new ArrayList<CustomizationItemBase>();
	protected List<CustomizationItemBase> backgroundRenderItems = new ArrayList<CustomizationItemBase>();

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

	protected ExternalTexturePanoramaRenderer panoramacube;

	protected ExternalTextureSlideshowRenderer slideshow;

	protected List<ButtonData> hidden = new ArrayList<ButtonData>();
	protected Map<PressableWidget, ButtonCustomizationContainer> vanillaButtonCustomizations = new HashMap<PressableWidget, ButtonCustomizationContainer>();
	protected Map<PressableWidget, VisibilityRequirementContainer> vanillaButtonVisibilityRequirementContainers = new HashMap<PressableWidget, VisibilityRequirementContainer>();

	protected Map<ButtonData, Float> delayAppearanceVanilla = new HashMap<ButtonData, Float>();
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

	protected static Screen scaleChangedIn = null;

	/**
	 * @param identifier Has to be the valid and full class name of the GUI screen.
	 */
	public MenuHandlerBase(@NotNull String identifier) {
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

	@SubscribeEvent
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {

		for (ThreadCaller t : this.delayThreads) {
			t.running.set(false);
		}
		this.delayThreads.clear();

		int mcscale = MinecraftClient.getInstance().getWindow().calculateScaleFactor(MinecraftClient.getInstance().options.guiScale, MinecraftClient.getInstance().forcesUnicodeFont());

		if (e.getGui() != MinecraftClient.getInstance().currentScreen) {
			return;
		}

		//Resetting scale to the normal value if it was changed in another screen
		if ((scaleChangedIn != null) && (scaleChangedIn != e.getGui())) {
			scaleChangedIn = null;
			Window m = MinecraftClient.getInstance().getWindow();
			m.setScaleFactor((double)mcscale);
			e.getGui().width = m.getScaledWidth();
			e.getGui().height = m.getScaledHeight();
		}

		if (!MenuCustomization.isValidScreen(e.getGui())) {
			return;
		}
		if (!this.shouldCustomize(e.getGui())) {
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
		if (!MenuCustomization.isMenuCustomizable(e.getGui())) {
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

		for (PropertiesSet s : rawLayouts) {

			List<PropertiesSection> metas = s.getPropertiesOfType("customization-meta");
			if (metas.isEmpty()) {
				metas = s.getPropertiesOfType("type-meta");
			}
			if (metas.isEmpty()) {
				continue;
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
				Window m = MinecraftClient.getInstance().getWindow();
				m.setScaleFactor((double)mcscale);
				e.getGui().width = m.getScaledWidth();
				e.getGui().height = m.getScaledHeight();
			}
		}

		//Handle auto scaling
		if ((this.sharedLayoutProps.autoScaleBaseWidth != 0) && (this.sharedLayoutProps.autoScaleBaseHeight != 0)) {
			Window m = MinecraftClient.getInstance().getWindow();
			double guiWidth = e.getGui().width * m.getScaleFactor();
			double guiHeight = e.getGui().height * m.getScaleFactor();
			double percentX = (guiWidth / (double)this.sharedLayoutProps.autoScaleBaseWidth) * 100.0D;
			double percentY = (guiHeight / (double)this.sharedLayoutProps.autoScaleBaseHeight) * 100.0D;
			double newScaleX = (percentX / 100.0D) * m.getScaleFactor();
			double newScaleY = (percentY / 100.0D) * m.getScaleFactor();
			double newScale = Math.min(newScaleX, newScaleY);

			m.setScaleFactor(newScale);
			e.getGui().width = m.getScaledWidth();
			e.getGui().height = m.getScaledHeight();
			this.sharedLayoutProps.scaled = true;
			scaleChangedIn = e.getGui();
		}

	}

	protected void applyLayoutPre(PropertiesSection sec, GuiScreenEvent.InitGuiEvent.Pre e) {

		String action = sec.getEntryValue("action");
		if (action != null) {
			String identifier = sec.getEntryValue("identifier");

			if (action.equalsIgnoreCase("overridemenu")) {
				if ((identifier != null) && CustomGuiLoader.guiExists(identifier)) {
					CustomGuiBase cus = CustomGuiLoader.getGui(identifier, (Screen)null, e.getGui(), (onClose) -> {
						e.getGui().removed();
					});
					MinecraftClient.getInstance().setScreen(cus);
				}
			}

			if (action.contentEquals("setscale")) {
				String scale = sec.getEntryValue("scale");
				if ((scale != null) && (MathUtils.isInteger(scale.replace(" ", "")) || MathUtils.isDouble(scale.replace(" ", "")))) {
					scaleChangedIn = e.getGui();
					int newscale = (int) Double.parseDouble(scale.replace(" ", ""));
					if (newscale <= 0) {
						newscale = 1;
					}
					Window m = MinecraftClient.getInstance().getWindow();
					m.setScaleFactor((double)newscale);
					e.getGui().width = m.getScaledWidth();
					e.getGui().height = m.getScaledHeight();
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

		if (e.getGui() != MinecraftClient.getInstance().currentScreen) {
			return;
		}
		if (!MenuCustomization.isValidScreen(e.getGui())) {
			return;
		}
		if (!this.shouldCustomize(e.getGui())) {
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
		if (!MenuCustomization.isMenuCustomizable(e.getGui())) {
			return;
		}

		if (!this.preinit) {
			System.out.println("################ WARNING [FANCYMENU] ################");
			System.out.println("MenuHandler pre-init skipped! Trying to re-initialize menu!");
			System.out.println("Menu Type: " + e.getGui().getClass().getName());
			System.out.println("Menu Handler: " + this.getClass().getName());
			System.out.println("This probably happened because a mod has overridden a menu with this one.");
			System.out.println("#####################################################");
			e.getGui().init(MinecraftClient.getInstance(), e.getGui().width, e.getGui().height);
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
			PressableWidget w = m.getKey().getButton();
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

		if (MenuCustomization.isNewMenu()) {
			for (CustomizationItemBase i : this.frontRenderItems) {
				this.handleAppearanceDelayFor(i);
			}

			for (CustomizationItemBase i : this.backgroundRenderItems) {
				this.handleAppearanceDelayFor(i);
			}
		}

		//Handle vanilla button visibility requirements
		for (Map.Entry<PressableWidget, VisibilityRequirementContainer> m : this.vanillaButtonVisibilityRequirementContainers.entrySet()) {
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
			PressableWidget b = null;
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
				String value = sec.getEntryValue("path");
				String pano = sec.getEntryValue("wideformat");
				if (pano == null) {
					pano = sec.getEntryValue("panorama");
				}
				if (value != null) {
					File f = new File(value.replace("\\", "/"));
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
				String width = sec.getEntryValue("width");
				String height = sec.getEntryValue("height");
				if (width != null) {
					width = DynamicValueHelper.convertFromRaw(width);
				}
				if (height != null) {
					height = DynamicValueHelper.convertFromRaw(height);
				}
				if ((width != null) && (height != null) && (b != null)) {
					if (MathUtils.isInteger(width) && MathUtils.isInteger(height)) {
						b.setWidth(Integer.parseInt(width));
						WidgetUtils.setHeight(b, Integer.parseInt(height));
					}
				}
			}

			if (action.equalsIgnoreCase("movebutton")) {
				String posX = sec.getEntryValue("x");
				String posY = sec.getEntryValue("y");
				if (posX != null) {
					posX = DynamicValueHelper.convertFromRaw(posX);
				}
				if (posY != null) {
					posY = DynamicValueHelper.convertFromRaw(posY);
				}
				String orientation = sec.getEntryValue("orientation");
				if ((orientation != null) && (posX != null) && (posY != null) && (b != null)) {
					if (MathUtils.isInteger(posX) && MathUtils.isInteger(posY)) {
						int x = Integer.parseInt(posX);
						int y = Integer.parseInt(posY);
						int w = e.getGui().width;
						int h = e.getGui().height;

						if (orientation.equalsIgnoreCase("original")) {
							b.x = b.x + x;
							b.y = b.y + y;
						}

						if (orientation.equalsIgnoreCase("top-left")) {
							b.x = x;
							b.y = y;
						}

						if (orientation.equalsIgnoreCase("mid-left")) {
							b.x = x;
							b.y = (h / 2) + y;
						}

						if (orientation.equalsIgnoreCase("bottom-left")) {
							b.x = x;
							b.y = h + y;
						}

						if (orientation.equalsIgnoreCase("top-centered")) {
							b.x = (w / 2) + x;
							b.y = y;
						}

						if (orientation.equalsIgnoreCase("mid-centered")) {
							b.x = (w / 2) + x;
							b.y = (h / 2) + y;
						}

						if (orientation.equalsIgnoreCase("bottom-centered")) {
							b.x = (w / 2) + x;
							b.y = h + y;
						}

						if (orientation.equalsIgnoreCase("top-right")) {
							b.x = w + x;
							b.y = y;
						}

						if (orientation.equalsIgnoreCase("mid-right")) {
							b.x = w + x;
							b.y = (h / 2) + y;
						}

						if (orientation.equalsIgnoreCase("bottom-right")) {
							b.x = w + x;
							b.y = h + y;
						}
					}
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
					String backNormal = sec.getEntryValue("backgroundnormal");
					String backHover = sec.getEntryValue("backgroundhovered");
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
					String path = sec.getEntryValue("path");
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

			if (action.equalsIgnoreCase("addentity")) {
				if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
					backgroundRenderItems.add(new PlayerEntityCustomizationItem(sec));
				} else {
					frontRenderItems.add(new PlayerEntityCustomizationItem(sec));
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
					if ((MinecraftClient.getInstance().world == null) || FancyMenu.config.getOrDefault("playbackgroundsoundsinworld", false)) {
						String path = sec.getEntryValue("path");
						String loopString = sec.getEntryValue("loop");

						boolean loop = false;
						if ((loopString != null) && loopString.equalsIgnoreCase("true")) {
							loop = true;
						}
						if (path != null) {
							File f = new File(path);
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
				String path = sec.getEntryValue("path");

				if (path != null) {
					File f = new File(path);
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
				String path = sec.getEntryValue("path");

				if (path != null) {
					File f = new File(path);
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

			if (action.equalsIgnoreCase("setbuttondescription")) {
				if (b != null) {
					String desc = sec.getEntryValue("description");
					if (desc != null) {
						this.sharedLayoutProps.descriptions.put(bd, DynamicValueHelper.convertFromRaw(desc));
					}
				}
			}

			if (action.equalsIgnoreCase("addsplash")) {
				String file = sec.getEntryValue("splashfilepath");
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

	@SubscribeEvent
	public void onRenderPost(GuiScreenEvent.DrawScreenEvent.Post e) {
		if (PopupHandler.isPopupActive()) {
			return;
		}
		if (!this.shouldCustomize(e.getGui())) {
			return;
		}
		if (!MenuCustomization.isMenuCustomizable(e.getGui())) {
			return;
		}

		if (!this.backgroundDrawable) {
			//Rendering all items that SHOULD be rendered in the background IF it's not possible to render them in the background (In this case, they will be forced to render in the foreground)
			List<CustomizationItemBase> backItems = new ArrayList<CustomizationItemBase>();
			backItems.addAll(this.backgroundRenderItems);
			for (CustomizationItemBase i : backItems) {
				try {
					i.render(e.getMatrixStack(), e.getGui());
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
				i.render(e.getMatrixStack(), e.getGui());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@SubscribeEvent
	public void drawToBackground(GuiScreenEvent.BackgroundDrawnEvent e) {
		if (!MenuCustomization.isCurrentMenuScrollable()) {
			this.renderBackground(e.getMatrixStack(), e.getGui());
		}
	}

	protected void renderBackground(MatrixStack matrix, Screen s) {
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
					this.backgroundAnimation.render(CurrentScreenHandler.getMatrixStack());
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
							drawTexture(CurrentScreenHandler.getMatrixStack(), 0, 0, 1.0F, 1.0F, s.width + 1, s.height + 1, s.width + 1, s.height + 1);
						} else {
							int w = this.backgroundTexture.getWidth();
							int h = this.backgroundTexture.getHeight();
							double ratio = (double) w / (double) h;
							int wfinal = (int)(s.height * ratio);
							int screenCenterX = s.width / 2;
							if (wfinal < s.width) {
								drawTexture(CurrentScreenHandler.getMatrixStack(), 0, 0, 1.0F, 1.0F, s.width + 1, s.height + 1, s.width + 1, s.height + 1);
							} else {
								drawTexture(CurrentScreenHandler.getMatrixStack(), screenCenterX - (wfinal / 2), 0, 1.0F, 1.0F, wfinal + 1, s.height + 1, wfinal + 1, s.height + 1);
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
							drawTexture(CurrentScreenHandler.getMatrixStack(), 0, 0, 1.0F, 1.0F, s.width + 1, s.height + 1, s.width + 1, s.height + 1);
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
					i.render(CurrentScreenHandler.getMatrixStack(), s);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}

			this.backgroundDrawable = true;
		}
	}

	@SubscribeEvent
	public void onButtonClickSound(PlayWidgetClickSoundEvent.Pre e) {

		if (this.shouldCustomize(MinecraftClient.getInstance().currentScreen)) {
			if (MenuCustomization.isMenuCustomizable(MinecraftClient.getInstance().currentScreen)) {

				ButtonCustomizationContainer c = this.vanillaButtonCustomizations.get(e.getWidget());

				if (c != null) {
					if (c.clickSound != null) {
						File f = new File(c.clickSound);
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
		if (this.shouldCustomize(MinecraftClient.getInstance().currentScreen)) {
			if (MenuCustomization.isMenuCustomizable(MinecraftClient.getInstance().currentScreen)) {

				PressableWidget w = e.getWidget();
				ButtonCustomizationContainer c = this.vanillaButtonCustomizations.get(w);
				if (c != null) {
					String normalBack = c.normalBackground;
					String hoverBack = c.hoverBackground;
					boolean hasCustomBackground = false;
					if (c.lastHoverState != w.isHovered()) {
						if (w.isHovered()) {
							if (c.restartAnimationOnHover) {
								for (IAnimationRenderer i : c.cachedAnimations) {
									if (i != null) {
										i.resetAnimation();
									}
								}
							}
						}
					}
					c.lastHoverState = w.isHovered();

					if (!w.isHovered()) {
						if (normalBack != null) {
							if (this.renderCustomButtomBackground(e, normalBack)) {
								hasCustomBackground = true;
							}
						}
					}

					if (w.isHovered()) {
						if (w.active) {
							if (hoverBack != null) {
								if (this.renderCustomButtomBackground(e, hoverBack)) {
									hasCustomBackground = true;
								}
							}
						} else {
							if (normalBack != null) {
								if (this.renderCustomButtomBackground(e, normalBack)) {
									hasCustomBackground = true;
								}
							}
						}
					}

					if (hasCustomBackground) {
						if (w instanceof TexturedButtonWidget) {
							Text msg = w.getMessage();
							if (msg != null) {
								int j = w.active ? 16777215 : 10526880;
								drawCenteredText(e.getMatrixStack(), MinecraftClient.getInstance().textRenderer, msg, w.x + w.getWidth() / 2, w.y + (w.getHeight() - 8) / 2, j | MathHelper.ceil(e.getAlpha() * 255.0F) << 24);
							}
						}

						e.setCanceled(true);
					}
				}

			}
		}
	}

	protected boolean renderCustomButtomBackground(RenderWidgetBackgroundEvent e, String background) {
		PressableWidget w = e.getWidget();
		MatrixStack matrix = e.getMatrixStack();
		ButtonCustomizationContainer c = this.vanillaButtonCustomizations.get(w);
		if (c != null) {
			if (w != null) {
				if (background != null) {
					if (background.startsWith("animation:")) {
						String aniName = background.split("[:]", 2)[1];
						if (AnimationHandler.animationExists(aniName)) {
							IAnimationRenderer a = AnimationHandler.getAnimation(aniName);
							this.renderBackgroundAnimation(e, a);
							if (!c.cachedAnimations.contains(a)) {
								c.cachedAnimations.add(a);
							}
							return true;
						}
					} else {
						File f = new File(background);
						if (f.isFile()) {
							if (f.getPath().toLowerCase().endsWith(".gif")) {
								IAnimationRenderer a =  TextureHandler.getGifResource(f.getPath());
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
									drawTexture(matrix, w.x, w.y, 0.0F, 0.0F, w.getWidth(), w.getHeight(), w.getWidth(), w.getHeight());
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
		PressableWidget w = e.getWidget();
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

				ani.render(e.getMatrixStack());

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

	protected ButtonCustomizationContainer getContainerForVanillaButton(PressableWidget w) {
		if (!this.vanillaButtonCustomizations.containsKey(w)) {
			ButtonCustomizationContainer c = new ButtonCustomizationContainer();
			this.vanillaButtonCustomizations.put(w, c);
			return c;
		}
		return this.vanillaButtonCustomizations.get(w);
	}

	protected boolean visibilityRequirementsMet(PressableWidget b) {
		VisibilityRequirementContainer c = this.vanillaButtonVisibilityRequirementContainers.get(b);
		if (c != null) {
			return c.isVisible();
		}
		return true;
	}

	@SubscribeEvent
	public void onRenderListBackground(RenderGuiListBackgroundEvent.Post e) {

		Screen s = MinecraftClient.getInstance().currentScreen;

		if (this.shouldCustomize(s)) {
			if (MenuCustomization.isMenuCustomizable(s)) {

				//Allow background stuff to be rendered in scrollable GUIs
				if (MinecraftClient.getInstance().currentScreen != null) {

					this.renderBackground(e.getMatrixStack(), s);

				}

			}
		}

	}

	private static ButtonData getButton(String identifier) {
		if (identifier.startsWith("%id=")) { //%id=1%
			String p = identifier.split("[=]")[1].replace("%", "");
			if (!MathUtils.isLong(p)) {
				return null;
			}
			long id = Long.parseLong(p);

			ButtonData b = ButtonCache.getButtonForId(id);
			if (b != null) {
				return b;
			}
		} else {
			ButtonData b = null;
			if (I18n.hasTranslation(identifier)) {
				b = ButtonCache.getButtonForKey(identifier);
			} else {
				b = ButtonCache.getButtonForName(identifier);
			}
			if (b != null) {
				return b;
			}
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
		return ((this.backgroundAnimation != null) || (this.backgroundTexture != null) || (this.panoramacube != null) || (this.slideshow != null));
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

	public boolean isVanillaButtonHidden(PressableWidget w) {
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

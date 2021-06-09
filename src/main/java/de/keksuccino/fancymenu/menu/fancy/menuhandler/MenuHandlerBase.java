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

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.events.PlayWidgetClickSoundEvent;
import de.keksuccino.fancymenu.events.RenderGuiListBackgroundEvent;
import de.keksuccino.fancymenu.events.RenderWidgetBackgroundEvent;
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
import de.keksuccino.fancymenu.menu.panorama.ExternalTexturePanoramaRenderer;
import de.keksuccino.fancymenu.menu.panorama.PanoramaHandler;
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
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MenuHandlerBase {

	//TODO übernehmen (ALLE ZU PROTECTED ÄNDERN)
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

	protected ExternalTexturePanoramaRenderer panoramacube;

	protected ExternalTextureSlideshowRenderer slideshow;

	protected List<ButtonData> hidden = new ArrayList<ButtonData>();
	protected Map<ButtonData, String> vanillaClickSounds = new HashMap<ButtonData, String>();
	protected Map<ButtonData, String> vanillaIdleTextures = new HashMap<ButtonData, String>();
	protected Map<ButtonData, String> vanillaHoverTextures = new HashMap<ButtonData, String>();

	protected Map<ButtonData, Float> delayAppearanceVanilla = new HashMap<ButtonData, Float>();
	protected Map<ButtonData, Float> fadeInVanilla = new HashMap<ButtonData, Float>();
	protected List<String> delayAppearanceFirstTime = new ArrayList<String>();
	protected List<Long> delayAppearanceFirstTimeVanilla = new ArrayList<Long>();
	protected List<ThreadCaller> delayThreads = new ArrayList<ThreadCaller>();

	//TODO übernehmen
//	protected List<PropertiesSet> props;
	protected boolean preinit = false;
	
	//TODO übernehmen
	protected Map<String, RandomLayoutContainer> randomLayoutGroups = new HashMap<String, RandomLayoutContainer>();
	protected List<PropertiesSet> normalLayouts = new ArrayList<PropertiesSet>();
	protected SharedLayoutProperties sharedLayoutProps = new SharedLayoutProperties();
	//----------------

	protected String closeAudio;
	protected String openAudio;
	
	protected static Screen scaleChangedIn = null;

	//TODO übernehmen
//	protected static Screen lastScreen = null;
//	protected boolean isNewMenu = true;

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
	public void onMenuReloaded(MenuReloadedEvent e) {
		this.delayAppearanceFirstTimeVanilla.clear();
		this.delayAppearanceFirstTime.clear();
		this.delayAppearanceVanilla.clear();
		this.fadeInVanilla.clear();
		//TODO übernehmen
//		this.isNewMenu = true;
		//TODO übernehmen
		for (RandomLayoutContainer c : this.randomLayoutGroups.values()) {
			c.lastLayoutPath = null;
		}

		if (this.lastBackgroundAnimation != null) {
			this.lastBackgroundAnimation.resetAnimation();
		}
	}

	@SubscribeEvent
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre e) {
		
		//TODO übernehmen
//		if ((lastScreen == null) || (!lastScreen.getClass().getName().equals(e.getGui().getClass().getName()))) {
//			this.isNewMenu = true;
//		}
//		lastScreen = e.getGui();

		for (ThreadCaller t : this.delayThreads) {
			t.running.set(false);
		}
		this.delayThreads.clear();
		
		//TODO übernehmen
//		boolean scaled = false;
		int mcscale = Minecraft.getInstance().getMainWindow().calcGuiScale(Minecraft.getInstance().gameSettings.guiScale, Minecraft.getInstance().getForceUnicodeFont());

		if (e.getGui() != Minecraft.getInstance().currentScreen) {
			return;
		}

		//Resetting scale to the normal value if it was changed in another screen
		if ((scaleChangedIn != null) && (scaleChangedIn != e.getGui())) {
			scaleChangedIn = null;
			MainWindow m = Minecraft.getInstance().getMainWindow();
			m.setGuiScale((double)mcscale);
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

		//TODO übernehmen
//		this.props = MenuCustomizationProperties.getPropertiesWithIdentifier(this.getMenuIdentifier());
		
		//TODO übernehmen
		List<PropertiesSet> rawLayouts = MenuCustomizationProperties.getPropertiesWithIdentifier(this.getMenuIdentifier());
		//TODO übernehmen
		String defaultGroup = "-100397";
		
		//TODO übernehmen
		this.normalLayouts.clear();
		
		//TODO übernehmen
		for (RandomLayoutContainer c : this.randomLayoutGroups.values()) {
			c.onlyFirstTime = false;
			c.clearLayouts();
		}
		
		//TODO übernehmen
		this.sharedLayoutProps = new SharedLayoutProperties();
		
		//TODO übernehmen
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
		
		//TODO übernehmen
		List<String> trashLayoutGroups = new ArrayList<String>();
		for (Map.Entry<String, RandomLayoutContainer> m : this.randomLayoutGroups.entrySet()) {
			if (m.getValue().getLayouts().isEmpty()) {
				trashLayoutGroups.add(m.getKey());
			}
		}
		for (String s : trashLayoutGroups) {
			this.randomLayoutGroups.remove(s);
		}
		//---------------------

		//Applying customizations which needs to be done before other ones
		//TODO übernehmen
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
		//----------------------

		//Resetting scale in the same menu when scale customization action was removed
		//TODO übernehmen (sharedLayoutProps)
		if (!this.sharedLayoutProps.scaled) {
			if (scaleChangedIn != null) {
				scaleChangedIn = null;
				MainWindow m = Minecraft.getInstance().getMainWindow();
				m.setGuiScale((double)mcscale);
				e.getGui().width = m.getScaledWidth();
				e.getGui().height = m.getScaledHeight();
			}
		}

	}
	
	//TODO übernehmen
	protected void applyLayoutPre(PropertiesSection sec, GuiScreenEvent.InitGuiEvent.Pre e) {
		
		String action = sec.getEntryValue("action");
		if (action != null) {
			String identifier = sec.getEntryValue("identifier");

			if (action.equalsIgnoreCase("overridemenu")) {
				if ((identifier != null) && CustomGuiLoader.guiExists(identifier)) {
					CustomGuiBase cus = CustomGuiLoader.getGui(identifier, (Screen)null, e.getGui(), (onClose) -> {
						e.getGui().onClose();
					});
					Minecraft.getInstance().displayGuiScreen(cus);
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
					MainWindow m = Minecraft.getInstance().getMainWindow();
					m.setGuiScale((double)newscale);
					e.getGui().width = m.getScaledWidth();
					e.getGui().height = m.getScaledHeight();
					this.sharedLayoutProps.scaled = true;
				}
			}
		}
		
	}

	@SubscribeEvent
	public void onButtonsCached(ButtonCachedEvent e) {

		if (e.getGui() != Minecraft.getInstance().currentScreen) {
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
			e.getGui().init(Minecraft.getInstance(), e.getGui().width, e.getGui().height);
			return;
		}

		//TODO übernehmen
//		if (this.props == null) {
//			return;
//		}

		this.hidden.clear();
		this.delayAppearanceVanilla.clear();
		this.fadeInVanilla.clear();
		this.vanillaClickSounds.clear();
		this.vanillaIdleTextures.clear();
		this.vanillaHoverTextures.clear();
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

		//TODO übernehmen (variablen entfernen)
//		boolean backgroundTextureSet = false;
//
//		boolean closeAudioSet = false;
//		boolean openAudioSet = false;
//
//		Map<ButtonData, String> descriptions = new HashMap<ButtonData, String>();

		//TODO übernehmen (dafür altes handling weg und nach unten in extra methode schieben)
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
		//---------------------

		MenuHandlerRegistry.setActiveHandler(this.getMenuIdentifier());

		//TODO übernehmen
		for (Map.Entry<ButtonData, String> m : this.sharedLayoutProps.descriptions.entrySet()) {
			Widget w = m.getKey().getButton();
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

		//TODO übernehmen
		if (!this.sharedLayoutProps.closeAudioSet && (this.closeAudio != null)) {
			MenuCustomization.unregisterSound(this.closeAudio);
			this.closeAudio = null;
		}

		//TODO übernehmen
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

		//TODO übernehmen
		if (!this.sharedLayoutProps.backgroundTextureSet) {
			this.backgroundTexture = null;
		}

		for (ButtonData d : this.hidden) {
			d.getButton().visible = false;
		}
		
		//TODO übernehmen (isNewMenu)
		if (MenuCustomization.isNewMenu()) {
			for (CustomizationItemBase i : this.frontRenderItems) {
				this.handleAppearanceDelayFor(i);
			}

			for (CustomizationItemBase i : this.backgroundRenderItems) {
				this.handleAppearanceDelayFor(i);
			}
		}

		for (Map.Entry<ButtonData, Float> m : this.delayAppearanceVanilla.entrySet()) {
			if (!hidden.contains(m.getKey())) {
				this.handleVanillaAppearanceDelayFor(m.getKey());
			}
		}

		//TODO übernehmen
//		this.isNewMenu = false;
		
	}
	
	//TODO übernehmen
	protected void applyLayout(PropertiesSection sec, String renderOrder, ButtonCachedEvent e) {
		
		String action = sec.getEntryValue("action");
		if (action != null) {
			String identifier = sec.getEntryValue("identifier");
			Widget b = null;
			ButtonData bd = null;
			if (identifier != null) {
				bd = getButton(identifier);
				if (bd != null) {
					b = bd.getButton();
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
							//TODO übernehmen (isNewMenu)
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
					//TODO übernehmen (isNewMenu)
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
				String value = sec.getEntryValue("value");
				if ((value != null) && (b != null)) {
					value = DynamicValueHelper.convertFromRaw(value);
					b.setMessage(new StringTextComponent(value));
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
						b.setHeight(Integer.parseInt(height));
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
					String backNormal = sec.getEntryValue("backgroundnormal");
					String backHover = sec.getEntryValue("backgroundhovered");
					if ((backNormal != null) && (backHover != null)) {
						this.vanillaIdleTextures.put(bd, backNormal);
						this.vanillaHoverTextures.put(bd, backHover);
					}
				}
			}

			if (action.equalsIgnoreCase("setbuttonclicksound")) {
				if (b != null) {
					String path = sec.getEntryValue("path");
					if (path != null) {
						this.vanillaClickSounds.put(bd, path);
					}
				}
			}

			if (action.equalsIgnoreCase("addhoversound")) {
				if (b != null) {
					if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
						backgroundRenderItems.add(new VanillaButtonCustomizationItem(sec, bd));
					} else {
						frontRenderItems.add(new VanillaButtonCustomizationItem(sec, bd));
					}
				}
			}

			if (action.equalsIgnoreCase("sethoverlabel")) {
				if (b != null) {
					if ((renderOrder != null) && renderOrder.equalsIgnoreCase("background")) {
						backgroundRenderItems.add(new VanillaButtonCustomizationItem(sec, bd));
					} else {
						frontRenderItems.add(new VanillaButtonCustomizationItem(sec, bd));
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
					if ((Minecraft.getInstance().world == null) || FancyMenu.config.getOrDefault("playbackgroundsoundsinworld", false)) {
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
									//TODO change to md5 instead of filesize
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
							//TODO change to md5 instead of filesize
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
							//TODO change to md5 instead of filesize
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
			
			d.getButton().visible = false;
			
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
					this.backgroundAnimation.setStretchImageToScreensize(true);
					this.backgroundAnimation.render(CurrentScreenHandler.getMatrixStack());
					this.backgroundAnimation.setStretchImageToScreensize(b);
				} else if (this.backgroundTexture != null) {
					RenderSystem.enableBlend();
					Minecraft.getInstance().getTextureManager().bindTexture(this.backgroundTexture.getResourceLocation());

					if (!this.panoramaback) {
						IngameGui.blit(CurrentScreenHandler.getMatrixStack(), 0, 0, 1.0F, 1.0F, s.width + 1, s.height + 1, s.width + 1, s.height + 1);
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
							IngameGui.blit(CurrentScreenHandler.getMatrixStack(), 0, 0, 1.0F, 1.0F, s.width + 1, s.height + 1, s.width + 1, s.height + 1);
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
					
					this.slideshow.height = s.height;
					this.slideshow.width = s.width;
					this.slideshow.x = 0;
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
		
		if (this.shouldCustomize(Minecraft.getInstance().currentScreen)) {
			if (MenuCustomization.isMenuCustomizable(Minecraft.getInstance().currentScreen)) {

				//Handle vanilla button click sounds
				for (Map.Entry<ButtonData, String> m : this.vanillaClickSounds.entrySet()) {

					if (m.getKey().getButton() == e.getWidget()) {
						File f = new File(m.getValue());

						if (f.exists() && f.isFile() && f.getPath().toLowerCase().endsWith(".wav")) {

							SoundHandler.registerSound(f.getPath(), f.getPath());
							SoundHandler.resetSound(f.getPath());
							SoundHandler.playSound(f.getPath());

						}

						e.setCanceled(true);

						break;
					}

				}
				
			}
		}

	}

	@SubscribeEvent
	public void onButtonRenderBackground(RenderWidgetBackgroundEvent.Pre e) {

		if (this.shouldCustomize(Minecraft.getInstance().currentScreen)) {
			if (MenuCustomization.isMenuCustomizable(Minecraft.getInstance().currentScreen)) {

				//Handle custom vanilla button background textures
				for (Map.Entry<ButtonData, String> m : this.vanillaIdleTextures.entrySet()) {

					Widget w = m.getKey().getButton();

					if (w == e.getWidget()) {
						String idle = m.getValue();
						String hover = this.vanillaHoverTextures.get(m.getKey());

						if ((hover != null) && (idle != null)) {
							ExternalTextureResourceLocation idleR = TextureHandler.getResource(idle);
							ExternalTextureResourceLocation hoverR = TextureHandler.getResource(hover);

							if ((idleR != null) && (hoverR != null)) {

								if (!idleR.isReady()) {
									idleR.loadTexture();
								}
								if (!hoverR.isReady()) {
									hoverR.loadTexture();
								}

								if (w.isHovered()) {
									if (w.active) {
										Minecraft.getInstance().textureManager.bindTexture(hoverR.getResourceLocation());
									} else {
										Minecraft.getInstance().textureManager.bindTexture(idleR.getResourceLocation());
									}
								} else {
									Minecraft.getInstance().textureManager.bindTexture(idleR.getResourceLocation());
								}
								RenderSystem.enableBlend();
								RenderSystem.color4f(1.0F, 1.0F, 1.0F, e.getAlpha());
								AbstractGui.blit(e.getMatrixStack(), w.x, w.y, 0.0F, 0.0F, w.getWidth(), w.getHeight(), w.getWidth(), w.getHeight());

								if (w instanceof ImageButton) {
									ITextComponent msg = w.getMessage();
									if (msg != null) {
										AbstractGui.drawCenteredString(e.getMatrixStack(), Minecraft.getInstance().fontRenderer, msg, w.x + w.getWidth() / 2, w.y + (w.getHeight() - 8) / 2, w.getFGColor() | MathHelper.ceil(e.getAlpha() * 255.0F) << 24);
									}
								}
								
								e.setCanceled(true);

							}

						}

						break;
					}

				}
			}
		}

	}

	@SubscribeEvent
	public void onRenderListBackground(RenderGuiListBackgroundEvent.Post e) {

		Screen s = Minecraft.getInstance().currentScreen;
		
		if (this.shouldCustomize(s)) {
			if (MenuCustomization.isMenuCustomizable(s)) {

				//Allow background stuff to be rendered in scrollable GUIs
				if (Minecraft.getInstance().currentScreen != null) {
					
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
			if (I18n.hasKey(identifier)) {
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
	
	//TODO übernehmen
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
	
	//TODO übernehmen
	public static class SharedLayoutProperties {
		
		public boolean scaled = false;
		public boolean backgroundTextureSet = false;
		public boolean openAudioSet = false;
		public boolean closeAudioSet = false;
		public Map<ButtonData, String> descriptions = new HashMap<ButtonData, String>();
		
	}

}

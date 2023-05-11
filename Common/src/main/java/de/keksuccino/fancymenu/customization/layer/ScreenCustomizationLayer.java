package de.keksuccino.fancymenu.customization.layer;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.blaze3d.platform.Window;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.audio.SoundRegistry;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.vanilla.VanillaButtonElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanilla.VanillaButtonElementBuilder;
import de.keksuccino.fancymenu.customization.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.customization.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutBase;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventPriority;
import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.ScreenReloadEvent;
import de.keksuccino.fancymenu.event.events.screen.*;
import de.keksuccino.fancymenu.event.events.widget.RenderGuiListBackgroundEvent;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.button.ButtonCache;
import de.keksuccino.fancymenu.event.events.ButtonCacheUpdatedEvent;
import de.keksuccino.fancymenu.customization.button.ButtonData;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.event.events.ModReloadEvent;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.v1.button.VanillaButtonCustomizationItem;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.v2.PlaceholderParser;
import de.keksuccino.fancymenu.utils.ScreenTitleUtils;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScreenCustomizationLayer extends GuiComponent {

	private static final Logger LOGGER = LogManager.getLogger();

	protected String identifier;

	protected LayoutBase layoutBase = new LayoutBase();
	protected Layout.OrderedElementCollection elements = new Layout.OrderedElementCollection();
	protected List<VanillaButtonElement> vanillaButtonElements = new ArrayList<>();
	protected Map<String, RandomLayoutContainer> randomLayoutGroups = new HashMap<>();
	protected List<Layout> activeLayouts = new ArrayList<>();

	protected List<String> delayAppearanceFirstTime = new ArrayList<>();
	protected List<ScreenCustomizationLayer.ThreadCaller> delayThreads = new ArrayList<>();

	protected boolean backgroundDrawable;
	protected boolean isNewMenu = true;
	protected boolean forceDisableCustomMenuTitle = false;
	public float backgroundOpacity = 1.0F;
	protected Map<LoadingRequirementContainer, Boolean> cachedLayoutWideLoadingRequirements = new HashMap<>();

	public static Map<Class<?>, Component> cachedOriginalMenuTitles = new HashMap<>();

	/**
	 * @param identifier Has to be the full class path of the menu screen.
	 */
	public ScreenCustomizationLayer(@NotNull String identifier) {
		Objects.requireNonNull(identifier);
		this.identifier = identifier;
		EventHandler.INSTANCE.registerListenersOf(this);
	}

	@NotNull
	public String getIdentifier() {
		return this.identifier;
	}

	@EventListener
	public void onSoftReload(ScreenReloadEvent e) {
		if (this.shouldCustomize(e.getScreen())) {
			this.delayAppearanceFirstTime.clear();
			this.isNewMenu = true;
			for (RandomLayoutContainer c : this.randomLayoutGroups.values()) {
				c.lastLayoutPath = null;
			}
		}
	}

	@EventListener
	public void onMenuReloaded(ModReloadEvent e) {
		this.delayAppearanceFirstTime.clear();
		this.isNewMenu = true;
		for (RandomLayoutContainer c : this.randomLayoutGroups.values()) {
			c.lastLayoutPath = null;
		}
	}

	@EventListener
	public void onOpenScreen(OpenScreenEvent e) {
		//Cache original menu title
		if (!cachedOriginalMenuTitles.containsKey(e.getScreen().getClass())) {
			cachedOriginalMenuTitles.put(e.getScreen().getClass(), e.getScreen().getTitle());
		}
		this.isNewMenu = true;
	}

	@EventListener
	public void onCloseScreen(CloseScreenEvent e) {

		if (!this.shouldCustomize(e.getScreen())) return;

		if (this.layoutBase.closeAudio != null) {
			SoundHandler.resetSound(this.layoutBase.closeAudio);
			SoundHandler.playSound(this.layoutBase.closeAudio);
		}

	}

	@EventListener
	public void onInitPre(InitOrResizeScreenEvent.Pre e) {

		for (ThreadCaller t : this.delayThreads) {
			t.running.set(false);
		}
		this.delayThreads.clear();

		if (!this.shouldCustomize(e.getScreen())) return;

		List<Layout> rawLayouts = LayoutHandler.getEnabledLayoutsForMenuIdentifier(this.getIdentifier());
		List<Layout> normalLayouts = new ArrayList<>();

		this.activeLayouts.clear();
		this.layoutBase = new LayoutBase();
		this.elements = new Layout.OrderedElementCollection();
		this.vanillaButtonElements.clear();
		this.backgroundOpacity = 1.0F;
		this.backgroundDrawable = false;
		this.cachedLayoutWideLoadingRequirements.clear();

		for (RandomLayoutContainer c : this.randomLayoutGroups.values()) {
			c.onlyFirstTime = false;
			c.clearLayouts();
		}

		for (Layout layout : rawLayouts) {

			LoadingRequirementContainer layoutWideRequirementContainer = layout.layoutWideLoadingRequirementContainer;
			this.cachedLayoutWideLoadingRequirements.put(layoutWideRequirementContainer, layoutWideRequirementContainer.requirementsMet());
			if (!layoutWideRequirementContainer.requirementsMet()) {
				continue;
			}

			if (layout.randomMode) {
				String group = layout.randomGroup;
				if (!this.randomLayoutGroups.containsKey(group)) {
					this.randomLayoutGroups.put(group, new RandomLayoutContainer(group, this));
				}
				RandomLayoutContainer randomContainer = this.randomLayoutGroups.get(group);
				if (randomContainer != null) {
					randomContainer.setOnlyFirstTime(layout.randomOnlyFirstTime);
					randomContainer.addLayout(layout);
				}
			} else {
				normalLayouts.add(layout);
			}

		}

		List<String> trashLayoutGroups = new ArrayList<>();
		for (Map.Entry<String, RandomLayoutContainer> m : this.randomLayoutGroups.entrySet()) {
			if (m.getValue().getLayouts().isEmpty()) {
				trashLayoutGroups.add(m.getKey());
			}
		}
		for (String s : trashLayoutGroups) {
			this.randomLayoutGroups.remove(s);
		}

		this.activeLayouts = new ArrayList<>(normalLayouts);
		this.randomLayoutGroups.values().forEach((container) -> this.activeLayouts.add(container.getRandomLayout()));

		//Stack active layouts
		this.layoutBase = LayoutBase.stackLayoutBases(activeLayouts.toArray(new LayoutBase[]{}));

		//Override menu with custom GUI if custom GUI identifier is defined
		if (this.layoutBase.overrideMenuWith != null) {
			if (CustomGuiLoader.guiExists(this.layoutBase.overrideMenuWith)) {
				CustomGuiBase cus = CustomGuiLoader.getGui(this.layoutBase.overrideMenuWith, null, e.getScreen(), (onClose) -> {
					e.getScreen().removed();
				});
				Minecraft.getInstance().setScreen(cus);
			}
		}

		//Handle forced GUI scale
		if (this.layoutBase.forcedScale != -1F) {
			float newscale = this.layoutBase.forcedScale;
			if (newscale <= 0) {
				newscale = 1;
			}
			Window m = Minecraft.getInstance().getWindow();
			m.setGuiScale(newscale);
			e.getScreen().width = m.getGuiScaledWidth();
			e.getScreen().height = m.getGuiScaledHeight();
		}

		//Handle auto-scaling
		if ((this.layoutBase.autoScalingWidth != 0) && (this.layoutBase.autoScalingHeight != 0)) {
			Window m = Minecraft.getInstance().getWindow();
			double guiWidth = e.getScreen().width * m.getGuiScale();
			double guiHeight = e.getScreen().height * m.getGuiScale();
			double percentX = (guiWidth / (double)this.layoutBase.autoScalingWidth) * 100.0D;
			double percentY = (guiHeight / (double)this.layoutBase.autoScalingHeight) * 100.0D;
			double newScaleX = (percentX / 100.0D) * m.getGuiScale();
			double newScaleY = (percentY / 100.0D) * m.getGuiScale();
			double newScale = Math.min(newScaleX, newScaleY);
			m.setGuiScale(newScale);
			e.getScreen().width = m.getGuiScaledWidth();
			e.getScreen().height = m.getGuiScaledHeight();
		}

	}

	@EventListener
	public void onButtonsCached(ButtonCacheUpdatedEvent e) {

		if (!this.shouldCustomize(e.getScreen())) return;

		if (this.isNewMenu && (this.layoutBase.openAudio != null)) {
			SoundHandler.resetSound(this.layoutBase.openAudio);
			SoundHandler.playSound(this.layoutBase.openAudio);
		}

		for (Layout layout : this.activeLayouts) {
			//Construct element instances
			Layout.OrderedElementCollection layoutElements = layout.buildElementInstances();
			this.elements.foregroundElements.addAll(layoutElements.foregroundElements);
			this.elements.backgroundElements.addAll(layoutElements.backgroundElements);
			//Load vanilla button customizations
			for (VanillaButtonElement element : layout.buildVanillaButtonElementInstances()) {
				ButtonData d = ButtonCache.getButtonForCompatibilityId(element.vanillaButtonIdentifier);
				if ((d == null) && MathUtils.isLong(element.vanillaButtonIdentifier)) {
					d = ButtonCache.getButtonForId(Long.parseLong(element.vanillaButtonIdentifier));
				}
				if (d != null) {
					element.buttonData = d;
					element.button = d.getButton();
					this.vanillaButtonElements.add(element);
					this.elements.backgroundElements.add(element);
				}
			}
		}

		//Handle appearance delay
		for (AbstractElement i : this.elements.foregroundElements) {
			if (ScreenCustomization.isNewMenu()) {
				this.handleAppearanceDelayFor(i);
			}
		}
		for (AbstractElement i : this.elements.backgroundElements) {
			if (ScreenCustomization.isNewMenu()) {
				this.handleAppearanceDelayFor(i);
			}
		}

		this.isNewMenu = false;

	}

	protected void handleAppearanceDelayFor(AbstractElement element) {
		if (!(element instanceof VanillaButtonCustomizationItem)) {
			if ((element.appearanceDelay != null) && (element.appearanceDelay != AbstractElement.AppearanceDelay.NO_DELAY)) {
				if (element.getInstanceIdentifier() == null) {
					return;
				}
				if ((element.appearanceDelay == AbstractElement.AppearanceDelay.FIRST_TIME) && delayAppearanceFirstTime.contains(element.getInstanceIdentifier())) {
					return;
				}
				if (element.appearanceDelay == AbstractElement.AppearanceDelay.FIRST_TIME) {
					if (!this.delayAppearanceFirstTime.contains(element.getInstanceIdentifier())) {
						delayAppearanceFirstTime.add(element.getInstanceIdentifier());
					}
				}
				element.visible = false;
				if (element.fadeIn) {
					element.opacity = 0.1F;
				}
				ThreadCaller c = new ThreadCaller();
				this.delayThreads.add(c);
				new Thread(() -> {
					long start = System.currentTimeMillis();
					float delay = (float) (1000.0 * element.appearanceDelayInSeconds);
					boolean fade = false;
					while (c.running.get()) {
						try {
							long now = System.currentTimeMillis();
							if (!fade) {
								if (now >= start + (int)delay) {
									element.visible = true;
									if (!element.fadeIn) {
										return;
									} else {
										fade = true;
									}
								}
							} else {
								float o = element.opacity + (0.03F * element.fadeInSpeed);
								if (o > 1.0F) {
									o = 1.0F;
								}
								if (element.opacity < 1.0F) {
									element.opacity = o;
								} else {
									return;
								}
							}
							Thread.sleep(50);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).start();
			}
		}
	}

	@EventListener(priority = EventPriority.VERY_HIGH)
	public void onRenderPre(RenderScreenEvent.Pre e) {

		if (PopupHandler.isPopupActive()) return;
		if (!this.shouldCustomize(e.getScreen())) return;

		//Re-init screen if layout-wide requirements changed
		for (Map.Entry<LoadingRequirementContainer, Boolean> m : this.cachedLayoutWideLoadingRequirements.entrySet()) {
			if (m.getKey().requirementsMet() != m.getValue()) {
				e.getScreen().resize(Minecraft.getInstance(), e.getScreen().width, e.getScreen().height);
				break;
			}
		}

	}

	@EventListener
	public void onRenderPost(RenderScreenEvent.Post e) {

		if (PopupHandler.isPopupActive()) return;
		if (!this.shouldCustomize(e.getScreen())) return;

		if ((this.layoutBase.customMenuTitle != null) && !this.forceDisableCustomMenuTitle) {
			ScreenTitleUtils.setScreenTitle(e.getScreen(), Component.literal(PlaceholderParser.replacePlaceholders(this.layoutBase.customMenuTitle)));
		}

		//Render background elements in foreground if it wasn't possible to render to the menu background
		if (!this.backgroundDrawable) {
			List<AbstractElement> background = new ArrayList<>(this.elements.backgroundElements);
			for (AbstractElement element : background) {
				element.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartial());
			}
		}

		//Render foreground elements
		List<AbstractElement> foreground = new ArrayList<>(this.elements.foregroundElements);
		for (AbstractElement element : foreground) {
			element.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartial());
		}

	}

	@EventListener
	public void drawToBackground(RenderedScreenBackgroundEvent e) {
		if (!ScreenCustomization.isCurrentMenuScrollable()) {
			this.renderBackground(e.getPoseStack(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getDeltaFrameTime(), e.getScreen());
		}
	}

	@EventListener
	public void onRenderListBackground(RenderGuiListBackgroundEvent.Post e) {
		Screen s = Minecraft.getInstance().screen;
		if (this.shouldCustomize(s)) {
			if (ScreenCustomization.isCustomizationEnabledForScreen(s)) {
				//Allow background stuff to be rendered in scrollable GUIs
				if (Minecraft.getInstance().screen != null) {
					this.renderBackground(e.getPoseStack(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getDeltaFrameTime(), s);
				}
			}
		}
	}

	protected void renderBackground(PoseStack pose, int mouseX, int mouseY, float partial, Screen screen) {

		if (!this.shouldCustomize(screen)) return;

		if (this.layoutBase.menuBackground != null) {
			this.layoutBase.menuBackground.opacity = this.backgroundOpacity;
			this.layoutBase.menuBackground.render(pose, mouseX, mouseY, partial);
			this.layoutBase.menuBackground.opacity = 1.0F;
		}

		if (PopupHandler.isPopupActive()) return;

		//Render background elements
		List<AbstractElement> background = new ArrayList<>(this.elements.backgroundElements);
		for (AbstractElement elements : background) {
			elements.render(pose, mouseX, mouseY, partial);
		}

		this.backgroundDrawable = true;

	}

	@Nullable
	public AbstractElement getElementByInstanceIdentifier(String instanceIdentifier) {
		List<AbstractElement> combined = new ArrayList<>(this.elements.backgroundElements);
		combined.addAll(this.elements.foregroundElements);
		for (AbstractElement element : combined) {
			if (element instanceof VanillaButtonCustomizationItem) {
				String id = "vanillabtn:" + ((VanillaButtonCustomizationItem)element).getButtonId();
				if (id.equals(instanceIdentifier)) {
					return element;
				}
			} else {
				if (element.getInstanceIdentifier().equals(instanceIdentifier)) {
					return element;
				}
			}
		}
		if (instanceIdentifier.startsWith("vanillabtn:")) {
			String idRaw = instanceIdentifier.split(":", 2)[1];
			ButtonData d;
			if (MathUtils.isLong(idRaw)) {
				d = ButtonCache.getButtonForId(Long.parseLong(idRaw));
			} else {
				d = ButtonCache.getButtonForCompatibilityId(idRaw);
			}
			if ((d != null) && (d.getButton() != null)) {
				VanillaButtonCustomizationItem vb = new VanillaButtonCustomizationItem(new PropertiesSection("customization"), d, this);
				vb.anchorPoint = ElementAnchorPoints.TOP_LEFT;
				vb.baseX = d.getButton().x;
				vb.baseY = d.getButton().y;
				vb.width = d.getButton().getWidth();
				vb.height = d.getButton().getHeight();
				return vb;
			}
		}
		return null;
	}

	protected boolean shouldCustomize(Screen screen) {
		if (screen == null) return false;
		if (!this.getIdentifier().equals(screen.getClass().getName())) return false;
		if (ButtonCache.isCaching()) return false;
		if (!ScreenCustomization.isCustomizationEnabledForScreen(screen)) return false;
		if (ButtonCache.isCaching()) return false;
		return true;
	}

	protected static ButtonData getVanillaButtonData(String identifier) {
		if (identifier.startsWith("%id=")) {
			String p = identifier.split("=")[1].replace("%", "");
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

	private static class ThreadCaller {
		AtomicBoolean running = new AtomicBoolean(true);
	}

	public static class RandomLayoutContainer {
		
		public final String id;
		protected List<Layout> layouts = new ArrayList<>();
		protected boolean onlyFirstTime = false;
		protected String lastLayoutPath = null;
		
		public ScreenCustomizationLayer parent;
		
		public RandomLayoutContainer(String id, ScreenCustomizationLayer parent) {
			this.id = id;
			this.parent = parent;
		}
		
		public List<Layout> getLayouts() {
			return this.layouts;
		}
		
		public void addLayout(Layout layout) {
			this.layouts.add(layout);
		}
		
		public void addLayouts(List<Layout> layouts) {
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
		public Layout getRandomLayout() {
			if (!this.layouts.isEmpty()) {
				if ((this.onlyFirstTime || !ScreenCustomization.isNewMenu()) && (this.lastLayoutPath != null)) {
					File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(this.lastLayoutPath));
					if (f.exists()) {
						for (Layout layout : this.layouts) {
							if ((layout.layoutFile != null) && layout.layoutFile.getAbsolutePath().replace("\\", "/").equals(f.getAbsolutePath())) {
								return layout;
							}
						}
					} else {
						SoundRegistry.stopSounds();
						SoundRegistry.resetSounds();
						AnimationHandler.resetAnimations();
						AnimationHandler.resetAnimationSounds();
						AnimationHandler.stopAnimationSounds();
					}
				}
				int i = MathUtils.getRandomNumberInRange(0, this.layouts.size()-1);
				Layout layout = this.layouts.get(i);
				if ((layout.layoutFile != null)) {
					this.lastLayoutPath = layout.layoutFile.getAbsolutePath();
					return layout;
				}
			}
			return null;
		}
	}

}

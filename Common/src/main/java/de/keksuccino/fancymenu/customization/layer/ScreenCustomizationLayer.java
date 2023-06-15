package de.keksuccino.fancymenu.customization.layer;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.blaze3d.platform.Window;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.audio.SoundRegistry;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanilla.VanillaButtonElement;
import de.keksuccino.fancymenu.customization.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.customization.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutBase;
import de.keksuccino.fancymenu.customization.widget.ScreenWidgetDiscoverer;
import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.acara.EventPriority;
import de.keksuccino.fancymenu.event.acara.EventListener;
import de.keksuccino.fancymenu.event.events.ScreenReloadEvent;
import de.keksuccino.fancymenu.event.events.screen.*;
import de.keksuccino.fancymenu.event.events.widget.RenderGuiListBackgroundEvent;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.event.events.ModReloadEvent;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinScreen;
import de.keksuccino.fancymenu.utils.ScreenTitleUtils;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ScreenCustomizationLayer extends GuiComponent implements IElementFactory {

	private static final Logger LOGGER = LogManager.getLogger();

	protected String identifier;

	public LayoutBase layoutBase = new LayoutBase();
	public List<AbstractElement> allElements = new ArrayList<>();
	public Layout.OrderedElementCollection normalElements = new Layout.OrderedElementCollection();
	public List<VanillaButtonElement> vanillaButtonElements = new ArrayList<>();
	public List<AbstractDeepElement> deepElements = new ArrayList<>();
	public Map<String, RandomLayoutContainer> randomLayoutGroups = new HashMap<>();
	public List<Layout> activeLayouts = new ArrayList<>();

	public List<String> delayAppearanceFirstTime = new ArrayList<>();
	public List<ScreenCustomizationLayer.ThreadCaller> delayThreads = new ArrayList<>();

	public boolean backgroundDrawable;
	public boolean isNewMenu = true;
	public boolean forceDisableCustomMenuTitle = false;
	public float backgroundOpacity = 1.0F;
	public Map<LoadingRequirementContainer, Boolean> cachedLayoutWideLoadingRequirements = new HashMap<>();
	protected List<WidgetMeta> cachedScreenWidgetMetas = new ArrayList<>();

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
	public void onScreenReload(ScreenReloadEvent e) {
		if (this.shouldCustomize(e.getScreen())) {
			this.delayAppearanceFirstTime.clear();
			this.isNewMenu = true;
			for (RandomLayoutContainer c : this.randomLayoutGroups.values()) {
				c.lastLayoutPath = null;
			}
		}
	}

	@EventListener
	public void onModReload(ModReloadEvent e) {
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
	public void onInitOrResizeScreenPre(InitOrResizeScreenEvent.Pre e) {

		for (ThreadCaller t : this.delayThreads) {
			t.running.set(false);
		}
		this.delayThreads.clear();

		if (!this.shouldCustomize(e.getScreen())) return;

		List<Layout> rawLayouts = LayoutHandler.getEnabledLayoutsForMenuIdentifier(this.getIdentifier());
		List<Layout> normalLayouts = new ArrayList<>();

		this.activeLayouts.clear();
		this.layoutBase = new LayoutBase();
		this.normalElements = new Layout.OrderedElementCollection();
		this.vanillaButtonElements.clear();
		this.deepElements.clear();
		this.allElements.clear();
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
					//TODO tf is this and why did I add it???
					e.getScreen().removed();
				});
				Minecraft.getInstance().setScreen(cus);
			}
		}

		//Handle forced GUI scale
		if (this.layoutBase.forcedScale != 0) {
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
	public void onInitOrResizeScreenPost(InitOrResizeScreenEvent.Post e) {

		if (!this.shouldCustomize(e.getScreen())) return;

		if (this.isNewMenu && (this.layoutBase.openAudio != null)) {
			SoundHandler.resetSound(this.layoutBase.openAudio);
			SoundHandler.playSound(this.layoutBase.openAudio);
		}

		this.cachedScreenWidgetMetas = ScreenWidgetDiscoverer.getWidgetMetasOfScreen(e.getScreen());

		this.constructElementInstances(this.identifier, this.cachedScreenWidgetMetas, this.activeLayouts, this.normalElements, this.vanillaButtonElements, this.deepElements);
		this.allElements.addAll(this.normalElements.backgroundElements);
		this.allElements.addAll(this.normalElements.foregroundElements);
		this.allElements.addAll(this.deepElements);
		this.allElements.addAll(this.vanillaButtonElements);

		//TODO experimental
//		//Remove vanilla buttons from the renderables list and let the vanilla button elements render them instead
//		if (e.getScreen() != null) {
//			for (WidgetMeta d : widgetMetas) {
//				((IMixinScreen)e.getScreen()).getRenderablesFancyMenu().remove(d.getWidget());
//			}
//		}

		for (AbstractElement i : this.allElements) {
			//Handle appearance delay
			if (this.isNewMenu) {
				this.handleAppearanceDelayFor(i);
			}
			//Add widgets of element to screen
			List<GuiEventListener> widgetsToRegister = i.getWidgetsToRegister();
			if (widgetsToRegister != null) {
				for (GuiEventListener w : widgetsToRegister) {
					if ((w instanceof NarratableEntry) && !((IMixinScreen)e.getScreen()).getChildrenFancyMenu().contains(w)) {
						((IMixinScreen)e.getScreen()).getChildrenFancyMenu().add(w);
					}
				}
			}
		}

		this.isNewMenu = false;

	}

	protected void handleAppearanceDelayFor(AbstractElement element) {
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

	@EventListener(priority = EventPriority.VERY_HIGH)
	public void onRenderPre(RenderScreenEvent.Pre e) {

		if (PopupHandler.isPopupActive()) return;
		if (!this.shouldCustomize(e.getScreen())) return;

		//Remove vanilla widgets from the renderables list before rendering the screen and let the vanilla button elements render them instead
		if (e.getScreen() != null) {
			for (WidgetMeta d : this.cachedScreenWidgetMetas) {
				((IMixinScreen)e.getScreen()).getRenderablesFancyMenu().remove(d.getWidget());
			}
		}

		//Re-init screen if layout-wide loading requirements changed
		for (Map.Entry<LoadingRequirementContainer, Boolean> m : this.cachedLayoutWideLoadingRequirements.entrySet()) {
			if (m.getKey().requirementsMet() != m.getValue()) {
				ScreenCustomization.reInitCurrentScreen();
				break;
			}
		}

		//Set custom menu title
		if ((this.layoutBase.customMenuTitle != null) && !this.forceDisableCustomMenuTitle) {
			ScreenTitleUtils.setScreenTitle(e.getScreen(), Component.literal(PlaceholderParser.replacePlaceholders(this.layoutBase.customMenuTitle)));
		}

	}

	@EventListener
	public void onRenderPost(RenderScreenEvent.Post e) {

		if (PopupHandler.isPopupActive()) return;
		if (!this.shouldCustomize(e.getScreen())) return;

		//Render background elements in foreground if it wasn't possible to render to the menu background
		if (!this.backgroundDrawable) {
			for (AbstractElement element : new ArrayList<>(this.normalElements.backgroundElements)) {
				element.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartial());
			}
		}
		//Render vanilla button elements
		for (AbstractElement element : new ArrayList<>(this.vanillaButtonElements)) {
			element.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartial());
		}
		//Render deep elements
		for (AbstractElement element : new ArrayList<>(this.deepElements)) {
			element.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartial());
		}
		//Render foreground elements
		for (AbstractElement element : new ArrayList<>(this.normalElements.foregroundElements)) {
			element.render(e.getPoseStack(), e.getMouseX(), e.getMouseY(), e.getPartial());
		}

		//Add back vanilla widgets after screen rendering
		//Info: Adding back widgets is important for screens that don't clear their widgets on resize, like the CreateWorldScreen
		if (e.getScreen() != null) {
			for (WidgetMeta d : this.cachedScreenWidgetMetas) {
				((IMixinScreen)e.getScreen()).getRenderablesFancyMenu().add(d.getWidget());
			}
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
		if ((s != null) && this.shouldCustomize(s)) {
			//Allow background rendering in scrollable GUIs
			this.renderBackground(e.getPoseStack(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getDeltaFrameTime(), s);
		}
	}

	protected void renderBackground(PoseStack pose, int mouseX, int mouseY, float partial, Screen screen) {

		if (!this.shouldCustomize(screen)) return;

		if (this.layoutBase.menuBackground != null) {
			this.layoutBase.menuBackground.keepBackgroundAspectRatio = this.layoutBase.keepBackgroundAspectRatio;
			this.layoutBase.menuBackground.opacity = this.backgroundOpacity;
			this.layoutBase.menuBackground.render(pose, mouseX, mouseY, partial);
			this.layoutBase.menuBackground.opacity = 1.0F;
		}

		if (PopupHandler.isPopupActive()) return;

		//Render background elements
		for (AbstractElement elements : new ArrayList<>(this.normalElements.backgroundElements)) {
			elements.render(pose, mouseX, mouseY, partial);
		}

		this.backgroundDrawable = true;

	}

	@Nullable
	public AbstractElement getElementByInstanceIdentifier(String instanceIdentifier) {
		for (AbstractElement element : this.allElements) {
			if (element.getInstanceIdentifier().equals(instanceIdentifier)) {
				return element;
			}
		}
		return null;
	}

	@SuppressWarnings("all")
	protected boolean shouldCustomize(Screen screen) {
		if (screen == null) return false;
		if (!this.getIdentifier().equals(screen.getClass().getName())) return false;
		if (!ScreenCustomization.isCustomizationEnabledForScreen(screen)) return false;
		return true;
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
				if ((this.onlyFirstTime || !this.parent.isNewMenu) && (this.lastLayoutPath != null)) {
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

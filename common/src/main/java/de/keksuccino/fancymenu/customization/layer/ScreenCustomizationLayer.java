package de.keksuccino.fancymenu.customization.layer;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.events.widget.RenderedGuiListHeaderFooterEvent;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutBase;
import de.keksuccino.fancymenu.customization.widget.ScreenWidgetDiscoverer;
import de.keksuccino.fancymenu.events.widget.RenderTabNavigationBarHeaderBackgroundEvent;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.events.screen.*;
import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.events.ModReloadEvent;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.fancymenu.util.ScreenTitleUtils;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CustomizableScreen;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"unused", "deprecation", "removal"})
public class ScreenCustomizationLayer implements ElementFactory {

	private static final Logger LOGGER = LogManager.getLogger();

	public static final ResourceLocation MENU_BACKGROUND = ResourceLocation.parse("textures/gui/menu_background.png");
	public static final ResourceLocation INWORLD_MENU_BACKGROUND = ResourceLocation.parse("textures/gui/inworld_menu_background.png");

	protected String screenIdentifier;
	public LayoutBase layoutBase = new LayoutBase();
	@NotNull
	public List<AbstractElement> allElements = new ArrayList<>();
	public Layout.OrderedElementCollection normalElements = new Layout.OrderedElementCollection();
	public List<VanillaWidgetElement> vanillaWidgetElements = new ArrayList<>();
	public Map<String, RandomLayoutContainer> randomLayoutGroups = new HashMap<>();
	public List<Layout> activeLayouts = new ArrayList<>();
	public List<String> delayAppearanceFirstTime = new ArrayList<>();
	public List<ScreenCustomizationLayer.ThreadCaller> delayThreads = new ArrayList<>();
	public boolean backgroundDrawable;
	public boolean forceDisableCustomMenuTitle = false;
	public float backgroundOpacity = 1.0F;
	public Map<LoadingRequirementContainer, Boolean> cachedLayoutWideLoadingRequirements = new HashMap<>();
	@NotNull
	public List<WidgetMeta> cachedScreenWidgetMetas = new ArrayList<>();
	/** The first {@link TabNavigationBar} of the target {@link Screen}, if it has one. This is NULL if the {@link Screen} has no {@link TabNavigationBar}. **/
	@Nullable
	public TabNavigationBar cachedTabNavigationBar = null;
	public boolean loadEarly = false;

	public static Map<Class<?>, Component> cachedOriginalMenuTitles = new HashMap<>();

	public ScreenCustomizationLayer(@NotNull String screenIdentifier) {
		Objects.requireNonNull(screenIdentifier);
		this.screenIdentifier = screenIdentifier;
		EventHandler.INSTANCE.registerListenersOf(this);
	}

	@NotNull
	public String getScreenIdentifier() {
		return this.screenIdentifier;
	}

	public void resetLayer() {
		this.delayAppearanceFirstTime.clear();
		for (RandomLayoutContainer c : this.randomLayoutGroups.values()) {
			c.lastLayoutPath = null;
		}
	}

	@EventListener
	public void onModReload(ModReloadEvent e) {
		this.resetLayer();
	}

	@EventListener
	public void onOpenScreen(OpenScreenEvent e) {

		if (!this.shouldCustomize(e.getScreen())) return;

		//Cache original menu title
		if (!cachedOriginalMenuTitles.containsKey(e.getScreen().getClass())) {
			cachedOriginalMenuTitles.put(e.getScreen().getClass(), e.getScreen().getTitle());
		}

	}

	@EventListener
	public void onOpenScreenPostInit(OpenScreenPostInitEvent e) {

		if (!this.shouldCustomize(e.getScreen())) return;

		this.allElements.forEach(AbstractElement::_onOpenScreen);

		this.layoutBase.menuBackgrounds.forEach(MenuBackground::onOpenScreen);

		this.layoutBase.openScreenExecutableBlocks.forEach(AbstractExecutableBlock::execute);

	}

	@EventListener
	public void onCloseScreen(CloseScreenEvent e) {

		if (!this.shouldCustomize(e.getScreen())) return;

		this.allElements.forEach(element -> {
			element.onCloseScreen(e.getClosedScreen(), e.getNewScreen());
			element.onCloseScreen();
			element.onDestroyElement();
		});

		this.layoutBase.menuBackgrounds.forEach(MenuBackground::onCloseScreen);

		if (this.layoutBase.closeAudio != null) {
			IAudio audio = this.layoutBase.closeAudio.get();
			if (audio != null) {
				audio.stop();
				audio.play();
			}
		}

		this.layoutBase.closeScreenExecutableBlocks.forEach(AbstractExecutableBlock::execute);

	}

	@EventListener
	public void onInitOrResizeScreenPre(InitOrResizeScreenEvent.Pre e) {

		this.cachedTabNavigationBar = null;

		for (ThreadCaller t : this.delayThreads) {
			t.running.set(false);
		}
		this.delayThreads.clear();

		if (!this.shouldCustomize(e.getScreen())) return;

		this.allElements.forEach(element -> {
			//Call onResizeScreen() for all OLD elements BEFORE resizing the screen
			if (e.getInitializationPhase() == InitOrResizeScreenEvent.InitializationPhase.RESIZE) {
				element.onBeforeResizeScreen();
			}
			//Call onDestroyElement() for all OLD elements before resizing the screen, because they get rebuild on resize
			element.onDestroyElement();
		});

		if (e.getInitializationPhase() == InitOrResizeScreenEvent.InitializationPhase.RESIZE) {
			this.layoutBase.menuBackgrounds.forEach(MenuBackground::onBeforeResizeScreen);
		}

		List<Layout> rawLayouts = LayoutHandler.getEnabledLayoutsForScreenIdentifier(this.getScreenIdentifier(), true);
		List<Layout> normalLayouts = new ArrayList<>();

		this.activeLayouts.clear();
		this.layoutBase = new LayoutBase();
		this.normalElements = new Layout.OrderedElementCollection();
		this.vanillaWidgetElements.clear();
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

		//Sort layouts by its index, so the layout with the smallest index is first in the list
		this.activeLayouts.sort(Comparator.comparingInt(l -> l.layoutIndex));

		//Stack active layouts
		this.layoutBase = LayoutBase.stackLayoutBases(this.activeLayouts.toArray(new LayoutBase[]{}));

		Window window = Minecraft.getInstance().getWindow();

		//Handle forced GUI scale
		if (this.layoutBase.forcedScale != 0) {
			float newscale = this.layoutBase.forcedScale;
			if (newscale <= 0) {
				newscale = 1;
			}
			WindowHandler.setGuiScale(newscale);
			e.getScreen().width = window.getGuiScaledWidth();
			e.getScreen().height = window.getGuiScaledHeight();
		}

		//Handle auto-scaling
		if ((this.layoutBase.autoScalingWidth != 0) && (this.layoutBase.autoScalingHeight != 0) && (this.layoutBase.forcedScale != 0)) {
			double guiWidth = e.getScreen().width * WindowHandler.getGuiScale();
			double guiHeight = e.getScreen().height * WindowHandler.getGuiScale();
			double percentX = (guiWidth / (double)this.layoutBase.autoScalingWidth) * 100.0D;
			double percentY = (guiHeight / (double)this.layoutBase.autoScalingHeight) * 100.0D;
			double newScaleX = (percentX / 100.0D) * WindowHandler.getGuiScale();
			double newScaleY = (percentY / 100.0D) * WindowHandler.getGuiScale();
			double newScale = Math.min(newScaleX, newScaleY);
			WindowHandler.setGuiScale(newScale);
			e.getScreen().width = window.getGuiScaledWidth();
			e.getScreen().height = window.getGuiScaledHeight();
		}

	}

	@EventListener
	public void onInitOrResizeScreenPost(InitOrResizeScreenEvent.Post e) {

		this.cachedTabNavigationBar = null;

		if (!this.shouldCustomize(e.getScreen())) return;

		for (Renderable renderable : e.getRenderables()) {
			if (renderable instanceof TabNavigationBar bar) {
				this.cachedTabNavigationBar = bar;
				break;
			}
		}

		if (ScreenCustomization.isNewMenu() && (this.layoutBase.openAudio != null)) {
			IAudio audio = this.layoutBase.openAudio.get();
			if (audio != null) {
				audio.stop();
				audio.play();
			}
		}

		this.cachedScreenWidgetMetas = ScreenWidgetDiscoverer.getWidgetsOfScreen(e.getScreen());

		this.constructElementInstances(this.getScreenIdentifier(), this.cachedScreenWidgetMetas, this.activeLayouts, this.normalElements, this.vanillaWidgetElements);
		this.allElements.addAll(this.normalElements.backgroundElements);
		this.allElements.addAll(this.normalElements.foregroundElements);
		this.allElements.addAll(this.vanillaWidgetElements);

		for (AbstractElement ae : this.allElements) {
			//Add widgets of element to screen
			List<GuiEventListener> widgetsToRegister = ae.getWidgetsToRegister();
			if (widgetsToRegister != null) {
				//Element children get always added at pos 0, so reverse the list to preserve the natural widget order
				widgetsToRegister = Lists.reverse(widgetsToRegister);
				for (GuiEventListener w : widgetsToRegister) {
					if ((w instanceof NarratableEntry) && !((IMixinScreen)e.getScreen()).getChildrenFancyMenu().contains(w)) {
						((IMixinScreen)e.getScreen()).getChildrenFancyMenu().addFirst(w);
						if (e.getScreen() instanceof CustomizableScreen c) c.removeOnInitChildrenFancyMenu().add(w);
					}
				}
			}
			//Update vanilla widgets before render, so they don't get rendered uncustomized for one tick
			if (ae instanceof VanillaWidgetElement v) {
				v.updateWidget();
			}
		}

		//Add all elements to the screen's widget list
		for (AbstractElement ae : Lists.reverse(this.allElements)) {
			((IMixinScreen)e.getScreen()).getChildrenFancyMenu().addFirst(ae);
			if (e.getScreen() instanceof CustomizableScreen c) c.removeOnInitChildrenFancyMenu().add(ae);
		}

		//Add menu background to screen's widget list
		this.layoutBase.menuBackgrounds.forEach(menuBackground -> {
			((IMixinScreen)e.getScreen()).getChildrenFancyMenu().addFirst(menuBackground);
			if (e.getScreen() instanceof CustomizableScreen c) c.removeOnInitChildrenFancyMenu().add(menuBackground);
		});

		if (e.getInitializationPhase() == InitOrResizeScreenEvent.InitializationPhase.RESIZE) {
			this.layoutBase.menuBackgrounds.forEach(MenuBackground::onAfterResizeScreen);
		}

	}

	@EventListener
	public void onScreenTickPre(ScreenTickEvent.Post e) {

		if (!this.shouldCustomize(e.getScreen())) return;

		this.layoutBase.menuBackgrounds.forEach(MenuBackground::tick);

		for (AbstractElement element : this.allElements) {
			element.tick();
		}

	}

	@EventListener(priority = EventPriority.VERY_HIGH)
	public void onRenderPre(RenderScreenEvent.Pre e) {

		if (!this.shouldCustomize(e.getScreen())) return;

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

		//Render vanilla button elements (render in pre, because Vanilla Widget elements don't actually render the widget, they just manage it, so it's important to call their render logic before everything else)
		for (AbstractElement element : new ArrayList<>(this.vanillaWidgetElements)) {
			element.renderInternal(e.getGraphics(), e.getMouseX(), e.getMouseY(), e.getPartial());
		}

	}

	@EventListener
	public void onRenderPost(RenderScreenEvent.Post e) {

		if (!this.shouldCustomize(e.getScreen())) return;

		//Render background elements in foreground if it wasn't possible to render to the menu background
		if (!this.backgroundDrawable) {
			for (AbstractElement element : new ArrayList<>(this.normalElements.backgroundElements)) {
				element.renderInternal(e.getGraphics(), e.getMouseX(), e.getMouseY(), e.getPartial());
			}
		}
		//Render foreground elements
		for (AbstractElement element : new ArrayList<>(this.normalElements.foregroundElements)) {
			element.renderInternal(e.getGraphics(), e.getMouseX(), e.getMouseY(), e.getPartial());
		}

	}

	@EventListener
	public void drawToBackground(RenderedScreenBackgroundEvent e) {
		this.renderBackground(e.getGraphics(), MouseInput.getMouseX(), MouseInput.getMouseY(), RenderingUtils.getPartialTick(), e.getScreen());
	}

	@EventListener
	public void onRenderListHeaderFooterPre(RenderedGuiListHeaderFooterEvent e) {

		GuiGraphics graphics = e.getGraphics();

		if (this.shouldCustomize(Minecraft.getInstance().screen)) {

			AbstractSelectionList<?> list = e.getList();

			ITexture headerTexture = (this.layoutBase.scrollListHeaderTexture != null) ? this.layoutBase.scrollListHeaderTexture.get() : null;
			ITexture footerTexture = (this.layoutBase.scrollListFooterTexture != null) ? this.layoutBase.scrollListFooterTexture.get() : null;

			boolean canRenderCustom = (headerTexture != null) || (footerTexture != null) || !this.layoutBase.renderScrollListHeaderShadow || !this.layoutBase.renderScrollListFooterShadow;
			if (!canRenderCustom) return;

			if (headerTexture != null) {
				ResourceLocation loc = headerTexture.getResourceLocation();
				if (loc != null) {
					if (this.layoutBase.preserveScrollListHeaderFooterAspectRatio) {
						int[] headerSize = headerTexture.getAspectRatio().getAspectRatioSizeByMinimumSize(list.getWidth(), list.getY());
						int headerWidth = headerSize[0];
						int headerHeight = headerSize[1];
						int headerX = list.getX() + (list.getWidth() / 2) - (headerWidth / 2);
						int headerY = (list.getY() / 2) - (headerHeight / 2);
						graphics.enableScissor(list.getX(), 0, list.getRight(), list.getY());
						graphics.blit(RenderPipelines.GUI_TEXTURED, loc, headerX, headerY, 0.0F, 0.0F, headerWidth, headerHeight, headerWidth, headerHeight);
						graphics.disableScissor();
					} else if (this.layoutBase.repeatScrollListHeaderTexture) {
						RenderingUtils.blitRepeat(graphics, loc, list.getX(), 0, list.getWidth(), list.getY(), headerTexture.getWidth(), headerTexture.getHeight(), -1);
					} else {
						graphics.blit(RenderPipelines.GUI_TEXTURED, loc, list.getX(), 0, 0.0F, 0.0F, list.getWidth(), list.getY(), list.getWidth(), list.getY());
					}
				}
			}

			if (footerTexture != null) {
				ResourceLocation loc = footerTexture.getResourceLocation();
				if (loc != null) {
					if (this.layoutBase.preserveScrollListHeaderFooterAspectRatio) {
						int footerOriginalHeight = ScreenUtils.getScreenHeight() - list.getBottom();
						if (footerOriginalHeight <= 0) footerOriginalHeight = 1;
						int[] footerSize = footerTexture.getAspectRatio().getAspectRatioSizeByMinimumSize(list.getWidth(), footerOriginalHeight);
						int footerWidth = footerSize[0];
						int footerHeight = footerSize[1];
						int footerX = list.getX() + (list.getWidth() / 2) - (footerWidth / 2);
						int footerY = list.getBottom() + (footerOriginalHeight / 2) - (footerHeight / 2);
						graphics.enableScissor(list.getX(), list.getBottom(), list.getRight(), list.getBottom() + footerOriginalHeight);
						graphics.blit(RenderPipelines.GUI_TEXTURED, loc, footerX, footerY, 0.0F, 0.0F, footerWidth, footerHeight, footerWidth, footerHeight);
						graphics.disableScissor();
					} else if (this.layoutBase.repeatScrollListFooterTexture) {
						int footerHeight = ScreenUtils.getScreenHeight() - list.getBottom();
						if (footerHeight <= 0) footerHeight = 1;
						RenderingUtils.blitRepeat(graphics, loc, list.getX(), list.getBottom(), list.getWidth(), footerHeight, footerTexture.getWidth(), footerTexture.getHeight(), -1);
					} else {
						int footerHeight = ScreenUtils.getScreenHeight() - list.getBottom();
						if (footerHeight <= 0) footerHeight = 1;
						graphics.blit(RenderPipelines.GUI_TEXTURED, loc, list.getX(), list.getBottom(), 0.0F, 0.0F, list.getWidth(), footerHeight, list.getWidth(), footerHeight);
					}
				}
			}

		}

	}

	@EventListener
	public void onRenderTabNavigationBarHeaderBackgroundPre(RenderTabNavigationBarHeaderBackgroundEvent.Pre e) {

		GuiGraphics graphics = e.getGraphics();

		if (this.shouldCustomize(Minecraft.getInstance().screen)) {

			ITexture headerTexture = (this.layoutBase.scrollListHeaderTexture != null) ? this.layoutBase.scrollListHeaderTexture.get() : null;

			if (headerTexture != null) {
				ResourceLocation loc = headerTexture.getResourceLocation();
				if (loc != null) {
					e.setCanceled(true);
					if (this.layoutBase.preserveScrollListHeaderFooterAspectRatio) {
						int[] headerSize = headerTexture.getAspectRatio().getAspectRatioSizeByMinimumSize(e.getHeaderWidth(), e.getHeaderHeight());
						int headerWidth = headerSize[0];
						int headerHeight = headerSize[1];
						int headerX = (e.getHeaderWidth() / 2) - (headerWidth / 2);
						int headerY = (e.getHeaderHeight() / 2) - (headerHeight / 2);
						graphics.enableScissor(0, 0, e.getHeaderWidth(), e.getHeaderHeight());
						graphics.blit(RenderPipelines.GUI_TEXTURED, loc, headerX, headerY, 0.0F, 0.0F, headerWidth, headerHeight, headerWidth, headerHeight);
						graphics.disableScissor();
					} else if (this.layoutBase.repeatScrollListHeaderTexture) {
						RenderingUtils.blitRepeat(graphics, loc, 0, 0, e.getHeaderWidth(), e.getHeaderHeight(), headerTexture.getWidth(), headerTexture.getHeight(), -1);
					} else {
						graphics.blit(RenderPipelines.GUI_TEXTURED, loc, 0, 0, 0.0F, 0.0F, e.getHeaderWidth(), e.getHeaderHeight(), e.getHeaderWidth(), e.getHeaderHeight());
					}
				}
			}

		}

	}

	protected void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partial, Screen screen) {

		if (!this.shouldCustomize(screen)) return;

		this.layoutBase.menuBackgrounds.forEach(menuBackground -> {

			menuBackground.keepBackgroundAspectRatio = this.layoutBase.preserveBackgroundAspectRatio;
			menuBackground.opacity = this.backgroundOpacity;
			menuBackground.render(graphics, mouseX, mouseY, partial);
			menuBackground.opacity = 1.0F;

		});

		if (!this.layoutBase.menuBackgrounds.isEmpty()) {

			if (this.layoutBase.applyVanillaBackgroundBlur) {
				Minecraft.getInstance().gameRenderer.processBlurEffect();
			}

			if (this.layoutBase.showScreenBackgroundOverlayOnCustomBackground) {
				int overlayY = 0;
				if (this.cachedTabNavigationBar != null) overlayY = this.cachedTabNavigationBar.getRectangle().bottom();
				this._renderBackgroundOverlay(graphics, 0, overlayY, screen.width, screen.height);
			}

		}

		//Render background elements
		for (AbstractElement elements : new ArrayList<>(this.normalElements.backgroundElements)) {
			elements.renderInternal(graphics, mouseX, mouseY, partial);
		}

		this.backgroundDrawable = true;

	}

	protected void _renderBackgroundOverlay(GuiGraphics graphics, int x, int y, int width, int height) {
		renderBackgroundOverlay(graphics, x, y, width, height);
	}

	public static void renderBackgroundOverlay(GuiGraphics graphics, int x, int y, int width, int height) {
		ResourceLocation location = (Minecraft.getInstance().level == null) ? MENU_BACKGROUND : INWORLD_MENU_BACKGROUND;
		graphics.blit(RenderPipelines.GUI_TEXTURED, location, x, y, 0, 0.0F, 0, width, height, 32, 32);
	}

	@Nullable
	public AbstractElement getElementByInstanceIdentifier(String instanceIdentifier) {
		instanceIdentifier = instanceIdentifier.replace("vanillabtn:", "").replace("button_compatibility_id:", "");
		for (AbstractElement element : this.allElements) {
			if (element.getInstanceIdentifier().equals(instanceIdentifier)) {
				return element;
			}
		}
		return null;
	}

	@SuppressWarnings("all")
	protected boolean shouldCustomize(@Nullable Screen screen) {
		if (screen == null) return false;
		if (ScreenCustomizationLayerHandler.isBeforeFinishInitialMinecraftReload() && !this.loadEarly) return false;
		if (!ScreenIdentifierHandler.isIdentifierOfScreen(this.getScreenIdentifier(), screen)) return false;
		if (!ScreenCustomization.isCustomizationEnabledForScreen(screen)) return false;
		return true;
	}

	public static class ThreadCaller {
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
					File f = new File(GameDirectoryUtils.getAbsoluteGameDirectoryPath(this.lastLayoutPath));
					if (f.exists()) {
						for (Layout layout : this.layouts) {
							if ((layout.layoutFile != null) && layout.layoutFile.getAbsolutePath().replace("\\", "/").equals(f.getAbsolutePath())) {
								return layout;
							}
						}
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

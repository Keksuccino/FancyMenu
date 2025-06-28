package de.keksuccino.fancymenu.customization.layer;

import java.io.File;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutBase;
import de.keksuccino.fancymenu.customization.widget.ScreenWidgetDiscoverer;
import de.keksuccino.fancymenu.events.widget.RenderGuiListHeaderFooterEvent;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractSelectionList;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.TaskExecutor;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.events.screen.*;
import de.keksuccino.fancymenu.events.widget.RenderGuiListBackgroundEvent;
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
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CustomizableScreen;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ScreenCustomizationLayer implements ElementFactory {

	private static final Logger LOGGER = LogManager.getLogger();

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

		this.layoutBase.menuBackgrounds.forEach(menuBackground -> menuBackground.onCloseScreen(e.getClosedScreen(), e.getNewScreen()));
		this.layoutBase.menuBackgrounds.forEach(MenuBackground::onCloseScreen);

		if (this.layoutBase.closeAudio != null) {
			final ResourceSupplier<IAudio> closeAudioSupplier = this.layoutBase.closeAudio;
			IAudio audio = closeAudioSupplier.get();
			if ((audio != null) && audio.isReady()) {
				audio.stop();
				audio.play();
			} else {
				final AtomicBoolean played = new AtomicBoolean(false);
				TaskExecutor.scheduleAtFixedRate((future) -> {
					if (played.get()) return;
					IAudio audio2 = closeAudioSupplier.get();
					if ((audio2 != null) && audio2.isReady()) {
						audio2.stop();
						audio2.play();
						played.set(true);
						future.cancel(true);
					}
				}, 100, 100, TimeUnit.MILLISECONDS, true);
			}
		}

		this.layoutBase.closeScreenExecutableBlocks.forEach(AbstractExecutableBlock::execute);

	}

	@EventListener
	public void onInitOrResizeScreenPre(InitOrResizeScreenEvent.Pre e) {

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

		List<MenuBackground> oldMenuBackgrounds = new ArrayList<>(this.layoutBase.menuBackgrounds);

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
			window.setGuiScale(newscale);
			e.getScreen().width = window.getGuiScaledWidth();
			e.getScreen().height = window.getGuiScaledHeight();
		}

		//Handle auto-scaling
		if ((this.layoutBase.autoScalingWidth != 0) && (this.layoutBase.autoScalingHeight != 0) && (this.layoutBase.forcedScale != 0)) {
			double guiWidth = e.getScreen().width * window.getGuiScale();
			double guiHeight = e.getScreen().height * window.getGuiScale();
			double percentX = (guiWidth / (double)this.layoutBase.autoScalingWidth) * 100.0D;
			double percentY = (guiHeight / (double)this.layoutBase.autoScalingHeight) * 100.0D;
			double newScaleX = (percentX / 100.0D) * window.getGuiScale();
			double newScaleY = (percentY / 100.0D) * window.getGuiScale();
			double newScale = Math.min(newScaleX, newScaleY);
			window.setGuiScale(newScale);
			e.getScreen().width = window.getGuiScaledWidth();
			e.getScreen().height = window.getGuiScaledHeight();
		}

		oldMenuBackgrounds.forEach(menuBackground -> {
			if (!this.layoutBase.menuBackgrounds.contains(menuBackground)) menuBackground.onDisableOrRemove();
		});

		this.layoutBase.menuBackgrounds.forEach(MenuBackground::onAfterEnable);

	}

	@EventListener
	public void onInitOrResizeScreenPost(InitOrResizeScreenEvent.Post e) {

		if (!this.shouldCustomize(e.getScreen())) return;

		if (ScreenCustomization.isNewMenu() && (this.layoutBase.openAudio != null)) {
			final ResourceSupplier<IAudio> openAudioSupplier = this.layoutBase.openAudio;
			IAudio audio = openAudioSupplier.get();
			if ((audio != null) && audio.isReady()) {
				audio.stop();
				audio.play();
			} else {
				final AtomicBoolean played = new AtomicBoolean(false);
				TaskExecutor.scheduleAtFixedRate((future) -> {
					if (played.get()) return;
					IAudio audio2 = openAudioSupplier.get();
					if ((audio2 != null) && audio2.isReady()) {
						audio2.stop();
						audio2.play();
						played.set(true);
						future.cancel(true);
					}
				}, 100, 100, TimeUnit.MILLISECONDS, true);
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
						((IMixinScreen)e.getScreen()).getChildrenFancyMenu().add(0, w);
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
			((IMixinScreen)e.getScreen()).getChildrenFancyMenu().add(0, ae);
			if (e.getScreen() instanceof CustomizableScreen c) c.removeOnInitChildrenFancyMenu().add(ae);
		}

		//Add menu background to screen's widget list
		this.layoutBase.menuBackgrounds.forEach(menuBackground -> {
			((IMixinScreen)e.getScreen()).getChildrenFancyMenu().add(0, menuBackground);
			if (e.getScreen() instanceof CustomizableScreen c) c.removeOnInitChildrenFancyMenu().add(menuBackground);
		});

		if (e.getInitializationPhase() == InitOrResizeScreenEvent.InitializationPhase.RESIZE) {
			this.layoutBase.menuBackgrounds.forEach(MenuBackground::onAfterResizeScreen);
		}

	}

	@EventListener
	public void onScreenTickPre(ScreenTickEvent.Post e) {

		if (PopupHandler.isPopupActive()) return;
		if (!this.shouldCustomize(e.getScreen())) return;

		this.layoutBase.menuBackgrounds.forEach(MenuBackground::tick);

		for (AbstractElement element : this.allElements) {
			element.tick();
		}

	}

	@EventListener(priority = EventPriority.VERY_HIGH)
	public void onRenderPre(RenderScreenEvent.Pre e) {

		if (PopupHandler.isPopupActive()) return;
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
			ScreenTitleUtils.setScreenTitle(e.getScreen(), Components.literal(PlaceholderParser.replacePlaceholders(this.layoutBase.customMenuTitle)));
		}

		//Render vanilla button elements (render in pre, because Vanilla Widget elements don't actually render the widget, they just manage it, so it's important to call their render logic before everything else)
		for (AbstractElement element : new ArrayList<>(this.vanillaWidgetElements)) {
			element.renderInternal(e.getGraphics(), e.getMouseX(), e.getMouseY(), e.getPartial());
		}

	}

	@EventListener
	public void onRenderPost(RenderScreenEvent.Post e) {

		if (PopupHandler.isPopupActive()) return;
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
		if (!ScreenCustomization.isCurrentMenuScrollable()) {
			this.renderBackground(e.getGraphics(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getDeltaFrameTime(), e.getScreen());
		}
	}

	@EventListener
	public void onRenderListBackground(RenderGuiListBackgroundEvent.Post e) {
		Screen s = Minecraft.getInstance().screen;
		if ((s != null) && this.shouldCustomize(s)) {
			//Allow background rendering in scrollable GUIs
			this.renderBackground(e.getGraphics(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getDeltaFrameTime(), s);
		}
	}

	@EventListener
	public void onRenderListHeaderFooterPre(RenderGuiListHeaderFooterEvent.Pre e) {

		GuiGraphics graphics = e.getGraphics();

		if (this.shouldCustomize(Minecraft.getInstance().screen)) {

			e.setCanceled(true);

			IMixinAbstractSelectionList access = e.getAccessor();

			ITexture headerTexture = (this.layoutBase.scrollListHeaderTexture != null) ? this.layoutBase.scrollListHeaderTexture.get() : null;
			ITexture footerTexture = (this.layoutBase.scrollListFooterTexture != null) ? this.layoutBase.scrollListFooterTexture.get() : null;

			if (headerTexture != null) {
				ResourceLocation loc = headerTexture.getResourceLocation();
				if (loc != null) {
					RenderingUtils.resetShaderColor(graphics);
					if (this.layoutBase.preserveScrollListHeaderFooterAspectRatio) {
						int[] headerSize = headerTexture.getAspectRatio().getAspectRatioSizeByMinimumSize(access.getWidthFancyMenu(), access.getY0FancyMenu());
						int headerWidth = headerSize[0];
						int headerHeight = headerSize[1];
						int headerX = access.getX0FancyMenu() + (access.getWidthFancyMenu() / 2) - (headerWidth / 2);
						int headerY = (access.getY0FancyMenu() / 2) - (headerHeight / 2);
						graphics.enableScissor(access.getX0FancyMenu(), 0, access.getX0FancyMenu() + access.getWidthFancyMenu(), access.getY0FancyMenu());
						graphics.blit(loc, headerX, headerY, 0.0F, 0.0F, headerWidth, headerHeight, headerWidth, headerHeight);
						graphics.disableScissor();
					} else if (this.layoutBase.repeatScrollListHeaderTexture) {
						RenderingUtils.blitRepeat(graphics, loc, access.getX0FancyMenu(), 0, access.getWidthFancyMenu(), access.getY0FancyMenu(), headerTexture.getWidth(), headerTexture.getHeight());
					} else {
						graphics.blit(loc, access.getX0FancyMenu(), 0, 0.0F, 0.0F, access.getWidthFancyMenu(), access.getY0FancyMenu(), access.getWidthFancyMenu(), access.getY0FancyMenu());
					}
				}
			} else {
				graphics.setColor(0.25F, 0.25F, 0.25F, 1.0F);
				graphics.blit(Screen.BACKGROUND_LOCATION, access.getX0FancyMenu(), 0, 0.0F, 0.0F, access.getWidthFancyMenu(), access.getY0FancyMenu(), 32, 32);
			}

			if (footerTexture != null) {
				ResourceLocation loc = footerTexture.getResourceLocation();
				if (loc != null) {
					RenderingUtils.resetShaderColor(graphics);
					if (this.layoutBase.preserveScrollListHeaderFooterAspectRatio) {
						int footerOriginalHeight = access.getHeightFancyMenu() - access.getY1FancyMenu();
						int[] footerSize = footerTexture.getAspectRatio().getAspectRatioSizeByMinimumSize(access.getWidthFancyMenu(), footerOriginalHeight);
						int footerWidth = footerSize[0];
						int footerHeight = footerSize[1];
						int footerX = access.getX0FancyMenu() + (access.getWidthFancyMenu() / 2) - (footerWidth / 2);
						int footerY = access.getY1FancyMenu() + (footerOriginalHeight / 2) - (footerHeight / 2);
						graphics.enableScissor(access.getX0FancyMenu(), access.getY1FancyMenu(), access.getX0FancyMenu() + access.getWidthFancyMenu(), access.getY1FancyMenu() + footerOriginalHeight);
						graphics.blit(loc, footerX, footerY, 0.0F, 0.0F, footerWidth, footerHeight, footerWidth, footerHeight);
						graphics.disableScissor();
					} else if (this.layoutBase.repeatScrollListFooterTexture) {
						int footerHeight = access.getHeightFancyMenu() - access.getY1FancyMenu();
						RenderingUtils.blitRepeat(graphics, loc, access.getX0FancyMenu(), access.getY1FancyMenu(), access.getWidthFancyMenu(), footerHeight, footerTexture.getWidth(), footerTexture.getHeight());
					} else {
						int footerHeight = access.getHeightFancyMenu() - access.getY1FancyMenu();
						graphics.blit(loc, access.getX0FancyMenu(), access.getY1FancyMenu(), 0.0F, 0.0F, access.getWidthFancyMenu(), footerHeight, access.getWidthFancyMenu(), footerHeight);
					}
				}
			} else {
				graphics.setColor(0.25F, 0.25F, 0.25F, 1.0F);
				graphics.blit(Screen.BACKGROUND_LOCATION, access.getX0FancyMenu(), access.getY1FancyMenu(), 0.0F, (float)access.getY1FancyMenu(), access.getWidthFancyMenu(), access.getHeightFancyMenu() - access.getY1FancyMenu(), 32, 32);
			}

			RenderingUtils.resetShaderColor(graphics);

			if (this.layoutBase.renderScrollListHeaderShadow) {
				graphics.fillGradient(access.getX0FancyMenu(), access.getY0FancyMenu(), access.getX1FancyMenu(), access.getY0FancyMenu() + 4, -16777216, 0);
			}
			if (this.layoutBase.renderScrollListFooterShadow) {
				graphics.fillGradient(access.getX0FancyMenu(), access.getY1FancyMenu() - 4, access.getX1FancyMenu(), access.getY1FancyMenu(), 0, -16777216);
			}

			RenderingUtils.resetShaderColor(graphics);

		}

	}

	protected void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partial, Screen screen) {

		if (!this.shouldCustomize(screen)) return;

		this.layoutBase.menuBackgrounds.forEach(menuBackground -> {

			RenderSystem.enableBlend();

			menuBackground.keepBackgroundAspectRatio = this.layoutBase.preserveBackgroundAspectRatio;
			menuBackground.opacity = this.backgroundOpacity;
			menuBackground.render(graphics, mouseX, mouseY, partial);
			menuBackground.opacity = 1.0F;

			//Restore render defaults
			RenderSystem.colorMask(true, true, true, true);
			RenderSystem.depthMask(true);
			RenderSystem.enableCull();
			RenderSystem.enableDepthTest();
			RenderSystem.enableBlend();
			graphics.flush();

		});

		if (!this.layoutBase.menuBackgrounds.isEmpty()) {

			if (this.layoutBase.applyVanillaBackgroundBlur) {
//				Minecraft.getInstance().gameRenderer.processBlurEffect(partial);
//				Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
			}

			if (this.layoutBase.showScreenBackgroundOverlayOnCustomBackground) {
//				int overlayY = 0;
//				if (this.cachedTabNavigationBar != null) overlayY = this.cachedTabNavigationBar.getRectangle().bottom();
//				this._renderBackgroundOverlay(graphics, 0, overlayY, screen.width, screen.height);
			}

		}

		if (PopupHandler.isPopupActive()) return;

		//Render background elements
		for (AbstractElement elements : new ArrayList<>(this.normalElements.backgroundElements)) {
			elements.renderInternal(graphics, mouseX, mouseY, partial);
		}

		this.backgroundDrawable = true;

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

	@Nullable
	public MenuBackground getMenuBackgroundByInstanceIdentifier(@NotNull String identifier) {
		for (MenuBackground b : this.layoutBase.menuBackgrounds) {
			if (b.getInstanceIdentifier().equals(identifier)) return b;
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

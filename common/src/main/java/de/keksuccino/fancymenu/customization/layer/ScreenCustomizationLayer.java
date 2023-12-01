package de.keksuccino.fancymenu.customization.layer;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.events.widget.RenderGuiListHeaderFooterEvent;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractSelectionList;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutBase;
import de.keksuccino.fancymenu.customization.widget.ScreenWidgetDiscoverer;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.events.screen.*;
import de.keksuccino.fancymenu.events.widget.RenderGuiListBackgroundEvent;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
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
import de.keksuccino.fancymenu.util.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
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
public class ScreenCustomizationLayer extends GuiComponent implements ElementFactory {

	private static final Logger LOGGER = LogManager.getLogger();

	protected String screenIdentifier;
	public LayoutBase layoutBase = new LayoutBase();
	@NotNull
	public List<AbstractElement> allElements = new ArrayList<>();
	public Layout.OrderedElementCollection normalElements = new Layout.OrderedElementCollection();
	public List<VanillaWidgetElement> vanillaWidgetElements = new ArrayList<>();
	public List<AbstractDeepElement> deepElements = new ArrayList<>();
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

		this.allElements.forEach(AbstractElement::onOpenScreen);

		if (this.layoutBase.menuBackground != null) this.layoutBase.menuBackground.onOpenScreen();

	}

	@EventListener
	public void onCloseScreen(CloseScreenEvent e) {

		if (!this.shouldCustomize(e.getScreen())) return;

		this.allElements.forEach(element -> {
			element.onCloseScreen();
			element.onDestroyElement();
		});

		if (this.layoutBase.menuBackground != null) this.layoutBase.menuBackground.onCloseScreen();

		if (this.layoutBase.closeAudio != null) {
			IAudio audio = this.layoutBase.closeAudio.get();
			if (audio != null) {
				audio.stop();
				audio.play();
			}
		}

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
			if (this.layoutBase.menuBackground != null) this.layoutBase.menuBackground.onBeforeResizeScreen();
		}

		List<Layout> rawLayouts = LayoutHandler.getEnabledLayoutsForScreenIdentifier(this.getScreenIdentifier(), true);
		List<Layout> normalLayouts = new ArrayList<>();

		this.activeLayouts.clear();
		this.layoutBase = new LayoutBase();
		this.normalElements = new Layout.OrderedElementCollection();
		this.vanillaWidgetElements.clear();
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
		if ((this.layoutBase.autoScalingWidth != 0) && (this.layoutBase.autoScalingHeight != 0) && (this.layoutBase.forcedScale != 0)) {
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

		if (ScreenCustomization.isNewMenu() && (this.layoutBase.openAudio != null)) {
			IAudio audio = this.layoutBase.openAudio.get();
			if (audio != null) {
				audio.stop();
				audio.play();
			}
		}

		this.cachedScreenWidgetMetas = ScreenWidgetDiscoverer.getWidgetsOfScreen(e.getScreen(), false, false);

		this.constructElementInstances(this.getScreenIdentifier(), this.cachedScreenWidgetMetas, this.activeLayouts, this.normalElements, this.vanillaWidgetElements, this.deepElements);
		this.allElements.addAll(this.normalElements.backgroundElements);
		this.allElements.addAll(this.normalElements.foregroundElements);
		this.allElements.addAll(this.deepElements);
		this.allElements.addAll(this.vanillaWidgetElements);

		for (AbstractElement ae : this.allElements) {
			//Handle appearance delay
			if (ScreenCustomization.isNewMenu()) {
				this.handleAppearanceDelayFor(ae);
			}
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
		if (this.layoutBase.menuBackground != null) {
			((IMixinScreen)e.getScreen()).getChildrenFancyMenu().add(0, this.layoutBase.menuBackground);
			if (e.getScreen() instanceof CustomizableScreen c) c.removeOnInitChildrenFancyMenu().add(this.layoutBase.menuBackground);
		}

		if (e.getInitializationPhase() == InitOrResizeScreenEvent.InitializationPhase.RESIZE) {
			if (this.layoutBase.menuBackground != null) this.layoutBase.menuBackground.onAfterResizeScreen();
		}

	}

	@SuppressWarnings("all")
	protected void handleAppearanceDelayFor(AbstractElement element) {
		if ((element.appearanceDelay != null) && (element.appearanceDelay != AbstractElement.AppearanceDelay.NO_DELAY)) {
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

	@EventListener
	public void onScreenTickPre(ScreenTickEvent.Post e) {

		if (PopupHandler.isPopupActive()) return;
		if (!this.shouldCustomize(e.getScreen())) return;

		if (this.layoutBase.menuBackground != null) this.layoutBase.menuBackground.tick();

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
		for (AbstractElement element : new ArrayList<>(this.vanillaWidgetElements)) {
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

	@EventListener
	public void onRenderListHeaderFooterPre(RenderGuiListHeaderFooterEvent.Pre e) {

		if (this.shouldCustomize(Minecraft.getInstance().screen)) {

			e.setCanceled(true);

			IMixinAbstractSelectionList access = e.getAccessor();

			ITexture headerTexture = (this.layoutBase.scrollListHeaderTexture != null) ? this.layoutBase.scrollListHeaderTexture.get() : null;
			ITexture footerTexture = (this.layoutBase.scrollListFooterTexture != null) ? this.layoutBase.scrollListFooterTexture.get() : null;

			if (headerTexture != null) {
				ResourceLocation loc = headerTexture.getResourceLocation();
				if (loc != null) {
					RenderingUtils.bindTexture(loc);
					RenderingUtils.resetShaderColor();
					if (this.layoutBase.preserveScrollListHeaderFooterAspectRatio) {
						int[] headerSize = headerTexture.getAspectRatio().getAspectRatioSizeByMinimumSize(access.getWidthFancyMenu(), access.getY0FancyMenu());
						int headerWidth = headerSize[0];
						int headerHeight = headerSize[1];
						int headerX = access.getX0FancyMenu() + (access.getWidthFancyMenu() / 2) - (headerWidth / 2);
						int headerY = (access.getY0FancyMenu() / 2) - (headerHeight / 2);
						enableScissor(access.getX0FancyMenu(), 0, access.getX0FancyMenu() + access.getWidthFancyMenu(), access.getY0FancyMenu());
						blit(e.getPoseStack(), headerX, headerY, 0.0F, 0.0F, headerWidth, headerHeight, headerWidth, headerHeight);
						disableScissor();
					} else {
						blit(e.getPoseStack(), access.getX0FancyMenu(), 0, 0.0F, 0.0F, access.getWidthFancyMenu(), access.getY0FancyMenu(), access.getWidthFancyMenu(), access.getY0FancyMenu());
					}
				}
			} else {
				RenderSystem.setShaderTexture(0, GuiComponent.BACKGROUND_LOCATION);
				RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 1.0F);
				blit(e.getPoseStack(), access.getX0FancyMenu(), 0, 0.0F, 0.0F, access.getWidthFancyMenu(), access.getY0FancyMenu(), 32, 32);
			}

			if (footerTexture != null) {
				ResourceLocation loc = footerTexture.getResourceLocation();
				if (loc != null) {
					RenderingUtils.bindTexture(loc);
					RenderingUtils.resetShaderColor();
					if (this.layoutBase.preserveScrollListHeaderFooterAspectRatio) {
						int footerOriginalHeight = access.getHeightFancyMenu() - access.getY1FancyMenu();
						int[] footerSize = footerTexture.getAspectRatio().getAspectRatioSizeByMinimumSize(access.getWidthFancyMenu(), footerOriginalHeight);
						int footerWidth = footerSize[0];
						int footerHeight = footerSize[1];
						int footerX = access.getX0FancyMenu() + (access.getWidthFancyMenu() / 2) - (footerWidth / 2);
						int footerY = access.getY1FancyMenu() + (footerOriginalHeight / 2) - (footerHeight / 2);
						enableScissor(access.getX0FancyMenu(), access.getY1FancyMenu(), access.getX0FancyMenu() + access.getWidthFancyMenu(), access.getY1FancyMenu() + footerOriginalHeight);
						blit(e.getPoseStack(), footerX, footerY, 0.0F, 0.0F, footerWidth, footerHeight, footerWidth, footerHeight);
						disableScissor();
					} else {
						int footerHeight = access.getHeightFancyMenu() - access.getY1FancyMenu();
						blit(e.getPoseStack(), access.getX0FancyMenu(), access.getY1FancyMenu(), 0.0F, 0.0F, access.getWidthFancyMenu(), footerHeight, access.getWidthFancyMenu(), footerHeight);
					}
				}
			} else {
				RenderSystem.setShaderTexture(0, GuiComponent.BACKGROUND_LOCATION);
				RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 1.0F);
				blit(e.getPoseStack(), access.getX0FancyMenu(), access.getY1FancyMenu(), 0.0F, (float)access.getY1FancyMenu(), access.getWidthFancyMenu(), access.getHeightFancyMenu() - access.getY1FancyMenu(), 32, 32);
			}

			RenderingUtils.resetShaderColor();

			if (this.layoutBase.renderScrollListHeaderShadow) {
				fillGradient(e.getPoseStack(), access.getX0FancyMenu(), access.getY0FancyMenu(), access.getX1FancyMenu(), access.getY0FancyMenu() + 4, -16777216, 0);
			}
			if (this.layoutBase.renderScrollListFooterShadow) {
				fillGradient(e.getPoseStack(), access.getX0FancyMenu(), access.getY1FancyMenu() - 4, access.getX1FancyMenu(), access.getY1FancyMenu(), 0, -16777216);
			}

			RenderingUtils.resetShaderColor();

		}

	}

	protected void renderBackground(PoseStack pose, int mouseX, int mouseY, float partial, Screen screen) {

		if (!this.shouldCustomize(screen)) return;

		if (this.layoutBase.menuBackground != null) {
			this.layoutBase.menuBackground.keepBackgroundAspectRatio = this.layoutBase.preserveBackgroundAspectRatio;
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
	protected boolean shouldCustomize(@Nullable Screen screen) {
		if (screen == null) return false;
		if (ScreenCustomizationLayerHandler.isBeforeFinishInitialMinecraftReload() && !this.loadEarly) return false;
		if (!ScreenIdentifierHandler.isIdentifierOfScreen(this.getScreenIdentifier(), screen)) return false;
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
				if ((this.onlyFirstTime || !ScreenCustomization.isNewMenu()) && (this.lastLayoutPath != null)) {
					File f = new File(GameDirectoryUtils.getAbsoluteGameDirectoryPath(this.lastLayoutPath));
					if (f.exists()) {
						for (Layout layout : this.layouts) {
							if ((layout.layoutFile != null) && layout.layoutFile.getAbsolutePath().replace("\\", "/").equals(f.getAbsolutePath())) {
								return layout;
							}
						}
					} else {
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

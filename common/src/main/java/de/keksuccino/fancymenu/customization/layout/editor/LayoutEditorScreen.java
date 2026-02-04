package de.keksuccino.fancymenu.customization.layout.editor;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.HideableElement;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget.VanillaWidgetElementBuilder;
import de.keksuccino.fancymenu.customization.layer.ElementFactory;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import de.keksuccino.fancymenu.customization.layout.editor.widget.LayoutEditorWidgetRegistry;
import de.keksuccino.fancymenu.customization.overlay.ScreenOverlays;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.customization.widget.ScreenWidgetDiscoverer;
import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.ObjectUtils;
import de.keksuccino.fancymenu.util.ScreenTitleUtils;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuHandler;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.SaveFileWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public class LayoutEditorScreen extends Screen implements ElementFactory {

	private static final Logger LOGGER = LogManager.getLogger();

	protected static final Map<SerializedElement, ElementBuilder<?,?>> COPIED_ELEMENTS_CLIPBOARD = new LinkedHashMap<>();
	public static final int ELEMENT_DRAG_CRUMPLE_ZONE = 5;

	@Nullable
	protected static LayoutEditorScreen currentInstance = null;

	@Nullable
	public Screen layoutTargetScreen;
	@NotNull
	public Layout layout;
	public List<AbstractEditorElement<?, ?>> normalEditorElements = new ArrayList<>();
	public List<VanillaWidgetEditorElement> vanillaWidgetEditorElements = new ArrayList<>();
	public LayoutEditorHistory history = new LayoutEditorHistory(this);
	public AnchorPointOverlay anchorPointOverlay = new AnchorPointOverlay(this);
	public ContextMenu rightClickMenu;
	public ContextMenu activeElementContextMenu = null;
	public List<AbstractLayoutEditorWidget> layoutEditorWidgets = new ArrayList<>();
	protected boolean isMouseSelection = false;
	protected int mouseSelectionStartX = 0;
	protected int mouseSelectionStartY = 0;
	public int leftMouseDownPosX = 0;
	public int leftMouseDownPosY = 0;
	protected boolean elementMovingStarted = false;
	protected boolean elementResizingStarted = false;
	protected boolean mouseDraggingStarted = false;
	protected List<AbstractEditorElement<?, ?>> currentlyDraggedElements = new ArrayList<>();
	protected int rightClickMenuOpenPosX = -1000;
	protected int rightClickMenuOpenPosY = -1000;
	protected LayoutEditorHistory.Snapshot preDragElementSnapshot;
	public final List<WidgetMeta> cachedVanillaWidgetMetas = new ArrayList<>();
	public boolean unsavedChanges = false;
	public boolean justOpened = true;
    protected final LayoutEditorUI layoutEditorUI = new LayoutEditorUI(this);

	public LayoutEditorScreen(@NotNull Layout layout) {
		this(null, layout);
	}

	public LayoutEditorScreen(@Nullable Screen layoutTargetScreen, @NotNull Layout layout) {

		super(Component.literal(""));

		this.layoutTargetScreen = layoutTargetScreen;
		layout.updateLastEditedTime();
		layout.saveToFileIfPossible();
		this.layout = layout.copy();

		if (this.layoutTargetScreen != null) {
			Component cachedOriTitle = ScreenCustomizationLayer.cachedOriginalMenuTitles.get(this.layoutTargetScreen.getClass());
			if (cachedOriTitle != null) {
				ScreenTitleUtils.setScreenTitle(this.layoutTargetScreen, cachedOriTitle);
			}
		}

		//Load all element instances before init, so the layout instance elements don't get wiped when updating it
		this.constructElementInstances();

		this.getAllElements().forEach(element -> {
			element.element._onOpenScreen();
		});

	}

	@Override
	protected void init() {

        ContextMenuHandler.INSTANCE.removeCurrent();

		this.currentlyDraggedElements.clear();

		this.anchorPointOverlay.resetOverlay();

		for (WidgetMeta m : this.cachedVanillaWidgetMetas) {
			if (m.getWidget() instanceof CustomizableWidget w) {
				w.resetWidgetCustomizationsFancyMenu();
			}
		}

		//Build widget instances only once (don't build in constructor to avoid stack overflows in builders)
		if ((this.layoutEditorWidgets == null) || this.layoutEditorWidgets.isEmpty()) {
			this.layoutEditorWidgets = LayoutEditorWidgetRegistry.buildWidgetInstances(this);
		}

		this.closeRightClickMenu();
		this.rightClickMenu = this.layoutEditorUI.buildRightClickContextMenu();
		ScreenOverlayHandler.INSTANCE.addOverlayWithId(ScreenOverlays.LAYOUT_EDITOR_RIGHT_CLICK_CONTEXT_MENU, this.rightClickMenu);

        this.refreshMenuBar();

		for (AbstractLayoutEditorWidget w : Lists.reverse(new ArrayList<>(this.layoutEditorWidgets))) {
			this.addWidget(Objects.requireNonNull(w));
		}

		this.isMouseSelection = false;
		this.preDragElementSnapshot = null;

		this.closeActiveElementMenu(true);

		this.serializeElementInstancesToLayoutInstance();

        Window window = Minecraft.getInstance().getWindow();

		//Handle forced GUI scale
		if (this.layout.forcedScale != 0) {
			float newscale = this.layout.forcedScale;
			if (newscale <= 0) {
				newscale = 1;
			}
			window.setGuiScale(newscale);
			this.width = window.getGuiScaledWidth();
			this.height = window.getGuiScaledHeight();
		}

		//Handle auto-scaling
		if ((this.layout.autoScalingWidth != 0) && (this.layout.autoScalingHeight != 0)) {
			double guiWidth = this.width * window.getGuiScale();
			double guiHeight = this.height * window.getGuiScale();
			double percentX = (guiWidth / (double)this.layout.autoScalingWidth) * 100.0D;
			double percentY = (guiHeight / (double)this.layout.autoScalingHeight) * 100.0D;
			double newScaleX = (percentX / 100.0D) * window.getGuiScale();
			double newScaleY = (percentY / 100.0D) * window.getGuiScale();
			double newScale = Math.min(newScaleX, newScaleY);
			window.setGuiScale(newScale);
			this.width = window.getGuiScaledWidth();
			this.height = window.getGuiScaledHeight();
		}

		this.getAllElements().forEach(element -> {
			if (!this.justOpened) element.element.onBeforeResizeScreen();
			element.element.onDestroyElement();
		});

		if (this.justOpened) this.layout.menuBackgrounds.forEach(MenuBackground::onOpenScreen);

		if (!this.justOpened) this.layout.menuBackgrounds.forEach(MenuBackground::onBeforeResizeScreen);

		this.constructElementInstances();

		if (!this.justOpened) this.layout.menuBackgrounds.forEach(MenuBackground::onAfterResizeScreen);

        this.layout.decorationOverlays.forEach(pair -> {
            pair.getSecond().onScreenInitializedOrResized(this, List.of());
            this.addWidget(pair.getSecond());
        });

		this.layout.menuBackgrounds.forEach(MenuBackground::onAfterEnable);

		for (AbstractLayoutEditorWidget w : this.layoutEditorWidgets) {
			w.refresh();
		}

		this.justOpened = false;

	}

    public void refreshMenuBar() {
        MenuBar oldMenuBar = this.getCurrentMenuBar();
        MenuBar menuBar = this.layoutEditorUI.buildMenuBar((oldMenuBar == null) || oldMenuBar.isExpanded());
        menuBar.addClickListener((button, state) -> {
           if (this.rightClickMenu != null) this.rightClickMenu.closeMenu();
           if (this.activeElementContextMenu != null) this.activeElementContextMenu.closeMenu();
        });
        ScreenOverlayHandler.INSTANCE.addOverlayWithId(ScreenOverlays.LAYOUT_EDITOR_MENU_BAR, menuBar);
        ScreenOverlayHandler.INSTANCE.setVisibilityControllerFor(ScreenOverlays.LAYOUT_EDITOR_MENU_BAR, screen -> {
            return (screen instanceof LayoutEditorScreen);
        });
    }

    @Nullable
    public MenuBar getCurrentMenuBar() {
        Renderable menuBarRaw = ScreenOverlayHandler.INSTANCE.getOverlay(ScreenOverlays.LAYOUT_EDITOR_MENU_BAR);
        return (menuBarRaw instanceof MenuBar b) ? b : null;
    }

    @Override
    public void removed() {
        this.closeActiveElementMenu(true);
        this.closeRightClickMenu(true);
        PiPWindowHandler.INSTANCE.closeAllWindows();
    }

    @Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void tick() {

		for (AbstractLayoutEditorWidget w : this.layoutEditorWidgets) {
			w.tick();
		}

		for (AbstractEditorElement<?, ?> e : this.getAllElements()) {
			e.element.tick();
		}

		this.layout.menuBackgrounds.forEach(MenuBackground::tick);

	}

	@Override
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

		//Clear active element context menu if not open
		if ((this.activeElementContextMenu != null) && !this.activeElementContextMenu.isOpen()) {
			this.activeElementContextMenu = null;
            ScreenOverlayHandler.INSTANCE.removeOverlay(ScreenOverlays.LAYOUT_EDITOR_ELEMENT_CONTEXT_MENU, true, false);
		}

		this.renderBackground(graphics, mouseX, mouseY, partial);

		this.renderElements(graphics, mouseX, mouseY, partial);

        this.layout.decorationOverlays.forEach(pair -> {
            if (pair.getSecond().showOverlay.tryGetNonNullElse(false)) pair.getSecond()._render(graphics, mouseX, mouseY, partial);
        });

		this.renderMouseSelectionRectangle(graphics, mouseX, mouseY);

		this.anchorPointOverlay.render(graphics, mouseX, mouseY, partial);

		this.renderLayoutEditorWidgets(graphics, mouseX, mouseY, partial);

	}

	protected void renderLayoutEditorWidgets(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
		for (AbstractLayoutEditorWidget w : this.layoutEditorWidgets) {
			if (w.isVisible()) w.render(graphics, mouseX, mouseY, partial);
		}
	}

	protected void renderMouseSelectionRectangle(GuiGraphics graphics, int mouseX, int mouseY) {
		if (this.isMouseSelection) {
			int startX = Math.min(this.mouseSelectionStartX, mouseX);
			int startY = Math.min(this.mouseSelectionStartY, mouseY);
			int endX = Math.max(this.mouseSelectionStartX, mouseX);
			int endY = Math.max(this.mouseSelectionStartY, mouseY);
			graphics.fill(startX, startY, endX, endY, RenderingUtils.replaceAlphaInColor(UIBase.getUITheme().layout_editor_mouse_selection_rectangle_color.getColorInt(), 70));
			UIBase.renderBorder(graphics, startX, startY, endX, endY, 1, UIBase.getUITheme().layout_editor_mouse_selection_rectangle_color.getColor(), true, true, true, true);
			graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		}
	}

	protected void renderElements(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

		//Render normal elements behind vanilla if renderBehindVanilla
		if (this.layout.renderElementsBehindVanilla) {
			for (AbstractEditorElement<?, ?> e : new ArrayList<>(this.normalEditorElements)) {
				if (!e.isSelected()) e.render(graphics, mouseX, mouseY, partial);
			}
		}
		//Render vanilla button elements
		for (VanillaWidgetEditorElement e : new ArrayList<>(this.vanillaWidgetEditorElements)) {
			if (!e.isSelected() && !e.isHidden()) e.render(graphics, mouseX, mouseY, partial);
		}
		//Render normal elements before vanilla if NOT renderBehindVanilla
		if (!this.layout.renderElementsBehindVanilla) {
			for (AbstractEditorElement<?, ?> e : new ArrayList<>(this.normalEditorElements)) {
				if (!e.isSelected()) e.render(graphics, mouseX, mouseY, partial);
			}
		}

		//Render selected elements last, so they're always visible
		List<AbstractEditorElement<?, ?>> selected = this.getSelectedElements();
		for (AbstractEditorElement<?, ?> e : selected) {
			e.render(graphics, mouseX, mouseY, partial);
		}

	}

	@Override
	public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

		graphics.fill(0, 0, this.width, this.height, UIBase.getUITheme().ui_interface_background_color.getColorInt());

		this.layout.menuBackgrounds.forEach(background -> {

			if (background.showBackground.tryGetNonNull()) {

                RenderSystem.enableBlend();

                background.keepBackgroundAspectRatio = this.layout.preserveBackgroundAspectRatio;
                background.opacity = 1.0F;
                background._render(graphics, mouseX, mouseY, partial);

                //Restore render defaults
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.depthMask(true);
                RenderSystem.enableCull();
                RenderSystem.enableDepthTest();
                RenderSystem.enableBlend();
                graphics.flush();

            }

		});

		if (!this.layout.menuBackgrounds.isEmpty()) {

			if (this.layout.applyVanillaBackgroundBlur) {
				Minecraft.getInstance().gameRenderer.processBlurEffect(partial);
				Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
			}

			if (this.layout.showScreenBackgroundOverlayOnCustomBackground) {
				ScreenCustomizationLayer.renderBackgroundOverlay(graphics, 0, 0, this.width, this.height);
			}

		}

		RenderingUtils.resetShaderColor(graphics);

		this.renderScrollListHeaderFooterPreview(graphics, mouseX, mouseY, partial);

		renderGrid(graphics, this.width, this.height);

	}

	@SuppressWarnings("unused")
	protected void renderScrollListHeaderFooterPreview(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

		if (this.layout.showScrollListHeaderFooterPreviewInEditor) {

			int x0 = 0;
			int x1 = this.width;
			int y0 = 48;
			int y1 = this.height - 64;

			ITexture headerTexture = (this.layout.scrollListHeaderTexture != null) ? this.layout.scrollListHeaderTexture.get() : null;
			ITexture footerTexture = (this.layout.scrollListFooterTexture != null) ? this.layout.scrollListFooterTexture.get() : null;

			//Header Texture
			if (headerTexture != null) {
				ResourceLocation loc = headerTexture.getResourceLocation();
				if (loc != null) {
					RenderingUtils.resetShaderColor(graphics);
					if (this.layout.preserveScrollListHeaderFooterAspectRatio) {
						int[] headerSize = headerTexture.getAspectRatio().getAspectRatioSizeByMinimumSize(this.width, y0);
						int headerWidth = headerSize[0];
						int headerHeight = headerSize[1];
						int headerX = x0 + (this.width / 2) - (headerWidth / 2);
						int headerY = (y0 / 2) - (headerHeight / 2);
						graphics.enableScissor(x0, 0, x0 + this.width, y0);
						graphics.blit(loc, headerX, headerY, 0.0F, 0.0F, headerWidth, headerHeight, headerWidth, headerHeight);
						graphics.disableScissor();
					} else if (this.layout.repeatScrollListHeaderTexture) {
						RenderingUtils.blitRepeat(graphics, loc, x0, 0, this.width, y0, headerTexture.getWidth(), headerTexture.getHeight());
					} else {
						graphics.blit(loc, x0, 0, 0.0F, 0.0F, this.width, y0, this.width, y0);
					}
				}
			}
			//Footer Texture
			if (footerTexture != null) {
				ResourceLocation loc = footerTexture.getResourceLocation();
				if (loc != null) {
					RenderingUtils.resetShaderColor(graphics);
					if (this.layout.preserveScrollListHeaderFooterAspectRatio) {
						int footerOriginalHeight = this.height - y1;
						int[] footerSize = footerTexture.getAspectRatio().getAspectRatioSizeByMinimumSize(this.width, footerOriginalHeight);
						int footerWidth = footerSize[0];
						int footerHeight = footerSize[1];
						int footerX = x0 + (this.width / 2) - (footerWidth / 2);
						int footerY = y1 + (footerOriginalHeight / 2) - (footerHeight / 2);
						graphics.enableScissor(x0, y1, x0 + this.width, y1 + footerOriginalHeight);
						graphics.blit(loc, footerX, footerY, 0.0F, 0.0F, footerWidth, footerHeight, footerWidth, footerHeight);
						graphics.disableScissor();
					} else if (this.layout.repeatScrollListFooterTexture) {
						int footerHeight = this.height - y1;
						RenderingUtils.blitRepeat(graphics, loc, x0, y1, this.width, footerHeight, footerTexture.getWidth(), footerTexture.getHeight());
					} else {
						int footerHeight = this.height - y1;
						graphics.blit(loc, x0, y1, 0.0F, 0.0F, this.width, footerHeight, this.width, footerHeight);
					}
				}
			}

			RenderingUtils.resetShaderColor(graphics);

			RenderSystem.enableBlend();
			graphics.blit(Screen.HEADER_SEPARATOR, 0, y0 - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
			graphics.blit(Screen.FOOTER_SEPARATOR, 0, y1, 0.0F, 0.0F, this.width, 2, 32, 2);

			RenderingUtils.resetShaderColor(graphics);

		}

	}

	@SuppressWarnings("all")
	public static void renderGrid(@NotNull GuiGraphics graphics, int screenWidth, int screenHeight) {

		if (FancyMenu.getOptions().showLayoutEditorGrid.getValue()) {

			float scale = UIBase.calculateFixedRenderScale(1.0F);
			int scaledWidth = (int)((float)screenWidth / scale);
			int scaledHeight = (int)((float)screenHeight / scale);

			graphics.pose().pushPose();
			graphics.pose().scale(scale, scale, scale);

			int gridSize = FancyMenu.getOptions().layoutEditorGridSize.getValue();
			int lineThickness = 1;

			//Draw centered vertical line
			graphics.fill((scaledWidth / 2) - 1, 0, (scaledWidth / 2) + 1, scaledHeight, UIBase.getUITheme().layout_editor_grid_color_center.getColorInt());

			//Draw vertical lines center -> left
			int linesVerticalToLeftPosX = (scaledWidth / 2) - gridSize - 1;
			while (linesVerticalToLeftPosX > 0) {
				int minY = 0;
				int maxY = scaledHeight;
				int maxX = linesVerticalToLeftPosX + lineThickness;
				graphics.fill(linesVerticalToLeftPosX, minY, maxX, maxY, UIBase.getUITheme().layout_editor_grid_color_normal.getColorInt());
				linesVerticalToLeftPosX -= gridSize;
			}

			//Draw vertical lines center -> right
			int linesVerticalToRightPosX = (scaledWidth / 2) + gridSize;
			while (linesVerticalToRightPosX < scaledWidth) {
				int minY = 0;
				int maxY = scaledHeight;
				int maxX = linesVerticalToRightPosX + lineThickness;
				graphics.fill(linesVerticalToRightPosX, minY, maxX, maxY, UIBase.getUITheme().layout_editor_grid_color_normal.getColorInt());
				linesVerticalToRightPosX += gridSize;
			}

			//Draw centered horizontal line
			graphics.fill(0, (scaledHeight / 2) - 1, scaledWidth, (scaledHeight / 2) + 1, UIBase.getUITheme().layout_editor_grid_color_center.getColorInt());

			//Draw horizontal lines center -> top
			int linesHorizontalToTopPosY = (scaledHeight / 2) - gridSize - 1;
			while (linesHorizontalToTopPosY > 0) {
				int minX = 0;
				int maxX = scaledWidth;
				int maxY = linesHorizontalToTopPosY + lineThickness;
				graphics.fill(minX, linesHorizontalToTopPosY, maxX, maxY, UIBase.getUITheme().layout_editor_grid_color_normal.getColorInt());
				linesHorizontalToTopPosY -= gridSize;
			}

			//Draw horizontal lines center -> bottom
			int linesHorizontalToBottomPosY = (scaledHeight / 2) + gridSize;
			while (linesHorizontalToBottomPosY < scaledHeight) {
				int minX = 0;
				int maxX = scaledWidth;
				int maxY = linesHorizontalToBottomPosY + lineThickness;
				graphics.fill(minX, linesHorizontalToBottomPosY, maxX, maxY, UIBase.getUITheme().layout_editor_grid_color_normal.getColorInt());
				linesHorizontalToBottomPosY += gridSize;
			}

			graphics.pose().popPose();

		}

	}

	protected void constructElementInstances() {

		//Clear element lists
		for (AbstractEditorElement<?, ?> e : this.getAllElements()) {
			e.resetElementStates();
		}
		this.normalEditorElements.clear();
		this.vanillaWidgetEditorElements.clear();

		Layout.OrderedElementCollection normalElements = new Layout.OrderedElementCollection();
		List<VanillaWidgetElement> vanillaWidgetElements = (this.layoutTargetScreen != null) ? new ArrayList<>() : null;

		this.cachedVanillaWidgetMetas.clear();
		if (this.layoutTargetScreen != null) {
			this.cachedVanillaWidgetMetas.addAll(ScreenWidgetDiscoverer.getWidgetsOfScreen(this.layoutTargetScreen, true));
		}
		for (WidgetMeta m : this.cachedVanillaWidgetMetas) {
			if (m.getWidget() instanceof CustomizableWidget w) {
				w.resetWidgetCustomizationsFancyMenu();
			}
		}

		this.constructElementInstances(this.layout.screenIdentifier, this.cachedVanillaWidgetMetas, this.layout, normalElements, vanillaWidgetElements);

		//Wrap normal elements
		for (AbstractElement e : ListUtils.mergeLists(normalElements.backgroundElements, normalElements.foregroundElements)) {
			AbstractEditorElement<?, ?> editorElement = e.getBuilder().wrapIntoEditorElementInternal(e, this);
			if (editorElement != null) {
				this.normalEditorElements.add(editorElement);
			}
		}
		//Wrap vanilla elements
		if (vanillaWidgetElements != null) {
			for (VanillaWidgetElement e : vanillaWidgetElements) {
				VanillaWidgetEditorElement editorElement = (VanillaWidgetEditorElement) VanillaWidgetElementBuilder.INSTANCE.wrapIntoEditorElementInternal(e, this);
				if (editorElement != null) {
					this.vanillaWidgetEditorElements.add(editorElement);
				}
			}
		}

		this.sanitizeLayerGroups();

	}

	protected void serializeElementInstancesToLayoutInstance() {

		this.layout.serializedElements.clear();
		this.layout.serializedVanillaButtonElements.clear();
		this.layout.serializedDeepElements.clear();
		this.updateLayerGroupElementOrder();

		//Serialize normal elements
		for (AbstractEditorElement<?, ?> e : this.normalEditorElements) {
			SerializedElement serialized = e.element.getBuilder().serializeElementInternal(e.element);
			if (serialized != null) {
				this.layout.serializedElements.add(serialized);
			}
		}
		//Serialize vanilla button elements
		for (VanillaWidgetEditorElement e : this.vanillaWidgetEditorElements) {
			SerializedElement serialized = VanillaWidgetElementBuilder.INSTANCE.serializeElementInternal(e.element);
			if (serialized != null) {
				this.layout.serializedVanillaButtonElements.add(serialized);
			}
		}

	}

	@NotNull
	public List<AbstractEditorElement<?, ?>> getAllElements() {
		List<AbstractEditorElement<?, ?>> elements = new ArrayList<>();
		List<AbstractEditorElement<?, ?>> selected = new ArrayList<>();
		List<AbstractEditorElement<?, ?>> elementsFinal = new ArrayList<>();
		if (this.layout.renderElementsBehindVanilla) {
			elements.addAll(this.normalEditorElements);
		}
		elements.addAll(this.vanillaWidgetEditorElements);
		if (!this.layout.renderElementsBehindVanilla) {
			elements.addAll(this.normalEditorElements);
		}
		//Put selected elements at the end, because they are always on top
		for (AbstractEditorElement<?, ?> e : elements) {
			if (!e.isSelected()) {
				elementsFinal.add(e);
			} else {
				selected.add(e);
			}
		}
		elementsFinal.addAll(selected);
		return elementsFinal;
	}

	@NotNull
	public List<AbstractEditorElement<?, ?>> getHoveredElements() {
		List<AbstractEditorElement<?, ?>> elements = new ArrayList<>();
		for (AbstractEditorElement<?, ?> e : this.getAllElements()) {
			if (e.isHovered()) {
				if (e.element.layerHiddenInEditor) continue;
				boolean hidden = (e instanceof HideableElement h) && h.isHidden();
				if (!hidden) elements.add(e);
			}
		}
		return elements;
	}

	@Nullable
	public AbstractEditorElement<?, ?> getTopHoveredElement() {
		List<AbstractEditorElement<?, ?>> hoveredElements = this.getHoveredElements();
		return (!hoveredElements.isEmpty()) ? hoveredElements.get(hoveredElements.size()-1) : null;
	}

	@NotNull
	public List<AbstractEditorElement<?, ?>> getSelectedElements() {
		List<AbstractEditorElement<?, ?>> l = new ArrayList<>();
		this.getAllElements().forEach(element -> {
			if (element.isSelected()) l.add(element);
		});
		return l;
	}

	@SuppressWarnings("all")
	@NotNull
	protected <E extends AbstractEditorElement<?, ?>> List<E> getSelectedElementsOfType(@NotNull Class<E> type) {
		List<E> l = new ArrayList<>();
		for (AbstractEditorElement<?, ?> e : this.getSelectedElements()) {
			if (type.isAssignableFrom(e.getClass())) {
				l.add((E)e);
			}
		}
		return l;
	}

	@Nullable
	public AbstractEditorElement<?, ?> getElementByInstanceIdentifier(@NotNull String instanceIdentifier) {
		instanceIdentifier = instanceIdentifier.replace("vanillabtn:", "").replace("button_compatibility_id:", "");
		for (AbstractEditorElement<?, ?> e : this.getAllElements()) {
			if (e.element.getInstanceIdentifier().equals(instanceIdentifier)) {
				return e;
			}
		}
		return null;
	}

	@Nullable
	public Layout.LayerGroup getLayerGroupForElement(@NotNull AbstractEditorElement<?, ?> element) {
		return this.getLayerGroupForInstanceIdentifier(element.element.getInstanceIdentifier());
	}

	@Nullable
	public Layout.LayerGroup getLayerGroupForInstanceIdentifier(@NotNull String instanceIdentifier) {
		for (Layout.LayerGroup group : this.layout.layerGroups) {
			if (group.elementInstanceIdentifiers.contains(instanceIdentifier)) {
				return group;
			}
		}
		return null;
	}

	@NotNull
	public List<AbstractEditorElement<?, ?>> getElementsInGroup(@NotNull Layout.LayerGroup group) {
		List<AbstractEditorElement<?, ?>> elements = new ArrayList<>();
		for (AbstractEditorElement<?, ?> element : this.normalEditorElements) {
			if (group.elementInstanceIdentifiers.contains(element.element.getInstanceIdentifier())) {
				elements.add(element);
			}
		}
		return elements;
	}

	public void removeElementsFromLayerGroups(@NotNull Collection<String> elementIds) {
		for (Layout.LayerGroup group : this.layout.layerGroups) {
			group.elementInstanceIdentifiers.removeIf(elementIds::contains);
		}
	}

	public void addElementsToLayerGroup(@NotNull Collection<String> elementIds, @NotNull Layout.LayerGroup group) {
		this.removeElementsFromLayerGroups(elementIds);
		for (String id : elementIds) {
			if (!group.elementInstanceIdentifiers.contains(id)) {
				group.elementInstanceIdentifiers.add(id);
			}
		}
	}

	public void sanitizeLayerGroups() {
		Set<String> validIds = new HashSet<>();
		for (AbstractEditorElement<?, ?> element : this.normalEditorElements) {
			validIds.add(element.element.getInstanceIdentifier());
		}
		Set<String> seen = new HashSet<>();
		for (Layout.LayerGroup group : this.layout.layerGroups) {
			List<String> cleaned = new ArrayList<>();
			for (String id : group.elementInstanceIdentifiers) {
				if (validIds.contains(id) && seen.add(id)) {
					cleaned.add(id);
				}
			}
			group.elementInstanceIdentifiers = cleaned;
		}
		this.updateLayerGroupElementOrder();
	}

	public void updateLayerGroupElementOrder() {
		Map<String, Integer> indexMap = new HashMap<>();
		for (int i = 0; i < this.normalEditorElements.size(); i++) {
			indexMap.put(this.normalEditorElements.get(i).element.getInstanceIdentifier(), i);
		}
		List<Layout.LayerGroup> withElements = new ArrayList<>();
		List<Layout.LayerGroup> empty = new ArrayList<>();
		Map<Layout.LayerGroup, Integer> groupIndex = new HashMap<>();
		for (Layout.LayerGroup group : this.layout.layerGroups) {
			group.elementInstanceIdentifiers.sort(Comparator.comparingInt(id -> indexMap.getOrDefault(id, Integer.MAX_VALUE)));
			int minIndex = Integer.MAX_VALUE;
			for (String id : group.elementInstanceIdentifiers) {
				Integer idx = indexMap.get(id);
				if (idx != null) {
					minIndex = Math.min(minIndex, idx);
				}
			}
			if (minIndex == Integer.MAX_VALUE) {
				empty.add(group);
			} else {
				withElements.add(group);
				groupIndex.put(group, minIndex);
			}
		}
		withElements.sort(Comparator.comparingInt(groupIndex::get));
		this.layout.layerGroups.clear();
		this.layout.layerGroups.addAll(withElements);
		this.layout.layerGroups.addAll(empty);
	}

	public void selectAllElements() {
		for (AbstractEditorElement<?, ?> e : this.getAllElements()) {
			if (e.element.layerHiddenInEditor) continue;
			e.setSelected(true);
		}
	}

	public void deselectAllElements() {
		for (AbstractEditorElement<?, ?> e : this.getAllElements()) {
			e.setSelected(false);
		}
	}

	@SuppressWarnings("all")
	public boolean deleteElement(@NotNull AbstractEditorElement<?, ?> element) {
		if (element.settings.isDestroyable()) {
			if (!element.settings.shouldHideInsteadOfDestroy()) {
				this.removeElementsFromLayerGroups(List.of(element.element.getInstanceIdentifier()));
				this.normalEditorElements.remove(element);
				this.vanillaWidgetEditorElements.remove(element);
				this.updateLayerGroupElementOrder();
				for (AbstractLayoutEditorWidget w : this.layoutEditorWidgets) {
					w.editorElementRemovedOrHidden(element);
				}
				return true;
			} else if (element instanceof HideableElement hideable) {
				hideable.setHidden(true);
				return true;
			}
		}
		return false;
	}

	protected boolean isElementOverlappingArea(@NotNull AbstractEditorElement<?, ?> element, int xStart, int yStart, int xEnd, int yEnd) {
		int elementStartX = element.getX();
		int elementStartY = element.getY();
		int elementEndX = element.getX() + element.getWidth();
		int elementEndY = element.getY() + element.getHeight();
		return (xEnd > elementStartX) && (yEnd > elementStartY) && (yStart < elementEndY) && (xStart < elementEndX);
	}

	public boolean allSelectedElementsMovable() {
		for (AbstractEditorElement<?, ?> e : this.getSelectedElements()) {
			if (e.element.layerHiddenInEditor) return false;
			if (!e.settings.isMovable()) return false;
		}
		return true;
	}

	public boolean canMoveLayerUp(AbstractEditorElement<?, ?> element) {
		Layout.LayerGroup group = this.getLayerGroupForElement(element);
		if (group != null) {
			List<AbstractEditorElement<?, ?>> groupElements = this.getElementsInGroup(group);
			int groupIndex = groupElements.indexOf(element);
			return groupIndex != -1 && groupIndex < groupElements.size() - 1;
		}
		int index = this.normalEditorElements.indexOf(element);
		if (index == -1) return false;
		return index < this.normalEditorElements.size()-1;
	}

	public boolean canMoveLayerDown(AbstractEditorElement<?, ?> element) {
		Layout.LayerGroup group = this.getLayerGroupForElement(element);
		if (group != null) {
			List<AbstractEditorElement<?, ?>> groupElements = this.getElementsInGroup(group);
			int groupIndex = groupElements.indexOf(element);
			return groupIndex > 0;
		}
		int index = this.normalEditorElements.indexOf(element);
		return index > 0;
	}

	/**
	 * Returns the element the given one was moved above or NULL if there was no element above the given one.
	 */
	@Nullable
	public AbstractEditorElement<?, ?> moveLayerUp(@NotNull AbstractEditorElement<?, ?> element) {
		AbstractEditorElement<?, ?> movedAbove = null;
		try {
			Layout.LayerGroup group = this.getLayerGroupForElement(element);
			if (group != null) {
				List<AbstractEditorElement<?, ?>> groupElements = this.getElementsInGroup(group);
				int groupIndex = groupElements.indexOf(element);
				if (groupIndex >= 0 && groupIndex < groupElements.size() - 1) {
					AbstractEditorElement<?, ?> above = groupElements.get(groupIndex + 1);
					int targetIndex = this.normalEditorElements.indexOf(above) + 1;
					List<AbstractEditorElement<?, ?>> newNormalEditorElements = new ArrayList<>(this.normalEditorElements);
					int sourceIndex = newNormalEditorElements.indexOf(element);
					if (sourceIndex != -1) {
						newNormalEditorElements.remove(element);
						int adjustedTargetIndex = targetIndex;
						if (sourceIndex < targetIndex) {
							adjustedTargetIndex--;
						}
						if (adjustedTargetIndex < 0) {
							adjustedTargetIndex = 0;
						}
						if (adjustedTargetIndex > newNormalEditorElements.size()) {
							adjustedTargetIndex = newNormalEditorElements.size();
						}
						newNormalEditorElements.add(adjustedTargetIndex, element);
						this.normalEditorElements = newNormalEditorElements;
					}
					movedAbove = above;
				}
			} else if (this.normalEditorElements.contains(element)) {
				List<AbstractEditorElement<?, ?>> newNormalEditorElements = new ArrayList<>();
				int index = this.normalEditorElements.indexOf(element);
				int i = 0;
				if (index < (this.normalEditorElements.size() - 1)) {
					for (AbstractEditorElement<?, ?> e : this.normalEditorElements) {
						if (e != element) {
							newNormalEditorElements.add(e);
							if (i == index+1) {
								movedAbove = e;
								newNormalEditorElements.add(element);
							}
						}
						i++;
					}
					this.normalEditorElements = newNormalEditorElements;
					if (movedAbove != null) {
						Layout.LayerGroup targetGroup = this.getLayerGroupForElement(movedAbove);
						if (targetGroup != null) {
							this.addElementsToLayerGroup(List.of(element.element.getInstanceIdentifier()), targetGroup);
						}
					}
				}
			}
			this.updateLayerGroupElementOrder();
		} catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to move element one layer up in the editor!", ex);
		}
		return movedAbove;
	}

	/**
	 * Returns the element the given one was moved behind or NULL if there was no element behind the given one.
	 */
	@Nullable
	public AbstractEditorElement<?, ?> moveLayerDown(AbstractEditorElement<?, ?> element) {
		AbstractEditorElement<?, ?> movedBehind = null;
		try {
			Layout.LayerGroup group = this.getLayerGroupForElement(element);
			if (group != null) {
				List<AbstractEditorElement<?, ?>> groupElements = this.getElementsInGroup(group);
				int groupIndex = groupElements.indexOf(element);
				if (groupIndex > 0) {
					AbstractEditorElement<?, ?> below = groupElements.get(groupIndex - 1);
					int targetIndex = this.normalEditorElements.indexOf(below);
					List<AbstractEditorElement<?, ?>> newNormalEditorElements = new ArrayList<>(this.normalEditorElements);
					int sourceIndex = newNormalEditorElements.indexOf(element);
					if (sourceIndex != -1) {
						newNormalEditorElements.remove(element);
						int adjustedTargetIndex = targetIndex;
						if (sourceIndex < targetIndex) {
							adjustedTargetIndex--;
						}
						if (adjustedTargetIndex < 0) {
							adjustedTargetIndex = 0;
						}
						if (adjustedTargetIndex > newNormalEditorElements.size()) {
							adjustedTargetIndex = newNormalEditorElements.size();
						}
						newNormalEditorElements.add(adjustedTargetIndex, element);
						this.normalEditorElements = newNormalEditorElements;
					}
					movedBehind = below;
				}
			} else if (this.normalEditorElements.contains(element)) {
				List<AbstractEditorElement<?, ?>> newNormalEditorElements = new ArrayList<>();
				int index = this.normalEditorElements.indexOf(element);
				int i = 0;
				if (index > 0) {
					for (AbstractEditorElement<?, ?> e : this.normalEditorElements) {
						if (e != element) {
							if (i == index-1) {
								newNormalEditorElements.add(element);
								movedBehind = e;
							}
							newNormalEditorElements.add(e);
						}
						i++;
					}
					this.normalEditorElements = newNormalEditorElements;
					if (movedBehind != null) {
						Layout.LayerGroup targetGroup = this.getLayerGroupForElement(movedBehind);
						if (targetGroup != null) {
							this.addElementsToLayerGroup(List.of(element.element.getInstanceIdentifier()), targetGroup);
						}
					}
				}
			}
			this.updateLayerGroupElementOrder();
		} catch (Exception ex) {
			LOGGER.error("[FANCYMENU] Failed to move element one layer down in the editor!", ex);
		}
		return movedBehind;
	}

	/**
	 * Moves a layer element to a specific position in the layer order.
	 *
	 * @param element The element to move
	 * @param targetIndex The index to move the element to (in the normalEditorElements list)
	 * @return true if the move was successful, false otherwise
	 */
	public boolean moveLayerToPosition(AbstractEditorElement<?, ?> element, int targetIndex) {
		try {
			if (this.normalEditorElements.contains(element)) {
				int sourceIndex = this.normalEditorElements.indexOf(element);

				// Skip if element is already at the target position
				if (sourceIndex == targetIndex) {
					return false;
				}

				// Create a new list for the reordered elements
				List<AbstractEditorElement<?, ?>> newNormalEditorElements = new ArrayList<>(this.normalEditorElements);

				// Remove the element from its current position
				newNormalEditorElements.remove(element);

				// Make sure targetIndex is valid after removal
				int adjustedTargetIndex = targetIndex;
				if (sourceIndex < targetIndex) {
					adjustedTargetIndex--;
				}

				// Insert at the target position
				if (adjustedTargetIndex >= newNormalEditorElements.size()) {
					newNormalEditorElements.add(element);
				} else if (adjustedTargetIndex < 0) {
					newNormalEditorElements.add(0, element);
				} else {
					newNormalEditorElements.add(adjustedTargetIndex, element);
				}

				// Update the elements list
				this.normalEditorElements = newNormalEditorElements;
				this.updateLayerGroupElementOrder();

				// Notify widgets about the change
				boolean movedUp = sourceIndex > targetIndex;
				this.layoutEditorWidgets.forEach(widget -> widget.editorElementOrderChanged(element, movedUp));

				// Mark layout as having unsaved changes
				this.unsavedChanges = true;

				return true;
			}
		} catch (Exception ex) {
			LOGGER.error("Failed to move layer to position", ex);
		}
		return false;
	}

	public void copyElementsToClipboard(AbstractEditorElement<?, ?>... elements) {
		if ((elements != null) && (elements.length > 0)) {
			COPIED_ELEMENTS_CLIPBOARD.clear();
			for (AbstractEditorElement<?, ?> e : elements) {
				if (e.element.layerHiddenInEditor) continue;
				if (e.settings.isCopyable()) {
					SerializedElement serialized = e.element.getBuilder().serializeElementInternal(e.element);
					if (serialized != null) {
						serialized.putProperty("instance_identifier", ScreenCustomization.generateUniqueIdentifier());
						COPIED_ELEMENTS_CLIPBOARD.put(serialized, e.element.getBuilder());
					}
				}
			}
		}
	}

	public void pasteElementsFromClipboard() {
		if (!COPIED_ELEMENTS_CLIPBOARD.isEmpty()) {
			this.deselectAllElements();
			for (Map.Entry<SerializedElement, ElementBuilder<?,?>> m : COPIED_ELEMENTS_CLIPBOARD.entrySet()) {
				m.getKey().putProperty("instance_identifier", ScreenCustomization.generateUniqueIdentifier());
				AbstractElement deserialized = m.getValue().deserializeElementInternal(m.getKey());
				if (deserialized != null) {
					AbstractEditorElement<?, ?> deserializedEditorElement = m.getValue().wrapIntoEditorElementInternal(deserialized, this);
					if (deserializedEditorElement != null) {
						this.normalEditorElements.add(deserializedEditorElement);
						this.layoutEditorWidgets.forEach(widget -> widget.editorElementAdded(deserializedEditorElement));
						deserializedEditorElement.element.layerHiddenInEditor = false;
						deserializedEditorElement.setSelected(true);
					}
				}
			}
		}
	}

	public void saveLayout() {
		if (this.layout.layoutFile != null) {
			this.layout.updateLastEditedTime();
			this.serializeElementInstancesToLayoutInstance();
			if (!this.layout.saveToFileIfPossible()) {
				Dialogs.openMessage(Component.translatable("fancymenu.editor.saving_failed.generic"), MessageDialogStyle.ERROR);
			} else {
				this.unsavedChanges = false;
				LayoutHandler.reloadLayouts();
			}
		} else {
			this.saveLayoutAs();
		}
	}

	public void saveLayoutAs() {
		String fileNamePreset = "universal_layout";
		if (this.layoutTargetScreen != null) {
			fileNamePreset = ScreenIdentifierHandler.getIdentifierOfScreen(this.layoutTargetScreen) + "_layout";
		}
		fileNamePreset = fileNamePreset.toLowerCase();
		fileNamePreset = CharacterFilter.buildOnlyLowercaseFileNameFilter().filterForAllowedChars(fileNamePreset);
		fileNamePreset = FileUtils.generateAvailableFilename(LayoutHandler.LAYOUT_DIR.getAbsolutePath(), fileNamePreset, "txt");
		if (this.layout.layoutFile != null) {
			fileNamePreset = this.layout.layoutFile.getName();
		}
		SaveFileWindowBody s = (SaveFileWindowBody) SaveFileWindowBody.build(LayoutHandler.LAYOUT_DIR, fileNamePreset, "txt", (call) -> {
			if (call != null) {
				try {
					this.layout.updateLastEditedTime();
					this.serializeElementInstancesToLayoutInstance();
					this.layout.layoutFile = call.getAbsoluteFile();
					//Unregister old layout if it gets overridden with new one
					if (this.layout.layoutFile.isFile()) {
						Layout old = LayoutHandler.getLayout(this.layout.getLayoutName());
						if (old != null) old.delete(false);
					}
					if (!this.layout.saveToFileIfPossible()) {
						Dialogs.openMessage(Component.translatable("fancymenu.editor.saving_failed.generic"), MessageDialogStyle.ERROR);
					} else {
						this.unsavedChanges = false;
						LayoutHandler.reloadLayouts();
					}
				} catch (Exception ex) {
					LOGGER.error("[FANCYMENU] Error while saving layout in editor!", ex);
					Dialogs.openMessage(Component.translatable("fancymenu.editor.saving_failed.generic"), MessageDialogStyle.ERROR);
				}
			}
		}).setVisibleDirectoryLevelsAboveRoot(2).setShowSubDirectories(true);
		FileTypeGroup<?> fileTypeGroup = FileTypeGroup.of(FileTypes.TXT_TEXT);
		fileTypeGroup.setDisplayName(Component.translatable("fancymenu.file_types.groups.text"));
		s.setFileTypes(fileTypeGroup);
		s.openInWindow(null);
	}

	public void onUpdateSelectedElements() {
		List<AbstractEditorElement<?, ?>> selected = this.getSelectedElements();
		if (selected.size() > 1) {
			for (AbstractEditorElement<?, ?> e : selected) {
				e.setMultiSelected(true);
			}
		} else if (selected.size() == 1) {
			selected.get(0).setMultiSelected(false);
		}
	}

	public void openRightClickMenuAtMouse(int mouseX, int mouseY) {
        this.closeActiveElementMenu();
		if (this.rightClickMenu != null) {
			this.rightClickMenuOpenPosX = mouseX;
			this.rightClickMenuOpenPosY = mouseY;
			this.rightClickMenu.openMenuAtMouse();
		}
	}

	public void closeRightClickMenu(boolean forceClose) {
		if (this.rightClickMenu != null) {
			if (!forceClose && this.rightClickMenu.isUserNavigatingInMenu()) return;
			this.rightClickMenuOpenPosX = -1000;
			this.rightClickMenuOpenPosY = -1000;
			this.rightClickMenu.closeMenu();
		}
	}

    public void closeRightClickMenu() {
        this.closeRightClickMenu(false);
    }

	public void openElementContextMenuAtMouseIfPossible() {
		this.closeActiveElementMenu();
        this.closeRightClickMenu();
		List<AbstractEditorElement<?, ?>> selectedElements = this.getSelectedElements();
		if (selectedElements.size() == 1) {
			this.activeElementContextMenu = selectedElements.get(0).rightClickMenu;
            ScreenOverlayHandler.INSTANCE.addOverlayWithId(ScreenOverlays.LAYOUT_EDITOR_ELEMENT_CONTEXT_MENU, this.activeElementContextMenu);
			this.activeElementContextMenu.openMenuAtMouse();
		} else if (selectedElements.size() > 1) {
			List<ContextMenu> menus = ObjectUtils.getOfAll(ContextMenu.class, selectedElements, consumes -> consumes.rightClickMenu);
			this.activeElementContextMenu = ContextMenu.stackContextMenus(menus);
			ScreenOverlayHandler.INSTANCE.addOverlayWithId(ScreenOverlays.LAYOUT_EDITOR_ELEMENT_CONTEXT_MENU, this.activeElementContextMenu);
			this.activeElementContextMenu.openMenuAtMouse();
		}
	}

	public void closeActiveElementMenu(boolean forceClose) {
		if (this.activeElementContextMenu != null) {
			if (!forceClose && this.activeElementContextMenu.isUserNavigatingInMenu()) return;
			this.activeElementContextMenu.closeMenu();
            ScreenOverlayHandler.INSTANCE.removeOverlay(ScreenOverlays.LAYOUT_EDITOR_ELEMENT_CONTEXT_MENU, true, false);
		}
		this.activeElementContextMenu = null;
	}

	public void closeActiveElementMenu() {
		this.closeActiveElementMenu(false);
	}

	public boolean isUserNavigatingInRightClickMenu() {
		return (this.rightClickMenu != null) && this.rightClickMenu.isUserNavigatingInMenu();
	}

	public boolean isUserNavigatingInElementMenu() {
		return (this.activeElementContextMenu != null) && this.activeElementContextMenu.isUserNavigatingInMenu();
	}

	public void saveWidgetSettings() {
		for (AbstractLayoutEditorWidget w : this.layoutEditorWidgets) {
			w.getBuilder().writeSettingsInternal(w);
		}
	}

	@NotNull
	public List<AbstractEditorElement<?, ?>> getCurrentlyDraggedElements() {
		return this.currentlyDraggedElements;
	}

	/**
	 * Returns NULL if there was an error while trying to get the element chain.
	 */
	@Nullable
	protected List<AbstractEditorElement<?, ?>> getElementChildChainOfExcluding(@NotNull AbstractEditorElement<?, ?> element) {
		Objects.requireNonNull(element);
		List<AbstractEditorElement<?, ?>> chain = new ArrayList<>();
		try {
			AbstractEditorElement<?, ?> e = element;
			while (true) {
				e = this.getChildElementOf(e);
				if (e == null) break;
				if (e == element) throw new IllegalStateException("Child of origin element is its own child. This shouldn't be possible and comes from an invalid ELEMENT anchor point. You need to manually fix this.");
				if (chain.contains(e)) throw new IllegalStateException("Chain already contains element! This shouldn't be possible and probably comes from an invalid ELEMENT anchor who's child is its parent or similar scenarios (sweet home Alabama). You need to manually fix this.");
				chain.add(e);
			}
		} catch (Exception ex) {
			LOGGER.error("[FANCYMENU] There was an error while trying to get the element chain!", ex);
			return null;
		}
		return chain;
	}

	@Nullable
	protected AbstractEditorElement<?, ?> getChildElementOf(@NotNull AbstractEditorElement<?, ?> element) {
		for (AbstractEditorElement<?, ?> e : this.getAllElements()) {
			String parentOfE = e.element.getAnchorPointElementIdentifier();
			if ((parentOfE != null) && parentOfE.equals(element.element.getInstanceIdentifier())) return e;
		}
		return null;
	}

	protected void moveSelectedElementsByXYOffset(int offsetX, int offsetY) {
		List<AbstractEditorElement<?, ?>> selected = this.getSelectedElements();
		if ((!selected.isEmpty()) && this.allSelectedElementsMovable()) {
			this.history.saveSnapshot();
		}
		boolean multiSelect = selected.size() > 1;
		for (AbstractEditorElement<?, ?> e : selected) {
			if (this.allSelectedElementsMovable()) {
				if (!multiSelect || !e.isElementAnchorAndParentIsSelected()) {
					e.element.posOffsetX = e.element.posOffsetX + offsetX;
					e.element.posOffsetY = e.element.posOffsetY + offsetY;
				}
			} else if (!e.settings.isMovable()) {
				e.renderMovingNotAllowedTime = System.currentTimeMillis() + 800;
			}
		}
	}

	//Called before mouseDragged
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {

		this.leftMouseDownPosX = (int) mouseX;
		this.leftMouseDownPosY = (int) mouseY;

        MenuBar menuBar = getCurrentMenuBar();
		boolean menuBarContextOpen = (menuBar != null) && menuBar.isEntryContextMenuOpen();

		if (super.mouseClicked(mouseX, mouseY, button)) {
			this.closeRightClickMenu();
			this.closeActiveElementMenu();
			return true;
		}

		//Skip the first click out of the menu bar context menus
		if (menuBarContextOpen) return true;

		AbstractEditorElement<?, ?> topHoverElement = this.getTopHoveredElement();

		boolean topHoverGotSelected = false;
		if (topHoverElement != null) {
			//Select hovered element on left- and right-click
			if (!this.rightClickMenu.isUserNavigatingInMenu() && ((this.activeElementContextMenu == null) || !this.activeElementContextMenu.isUserNavigatingInMenu())) {
				if (!topHoverElement.isSelected()) {
					topHoverElement.setSelected(true);
					topHoverElement.recentlyLeftClickSelected = true;
					topHoverGotSelected = true;
				}
			}
		}
		boolean canStartMouseSelection = true;
		//Handle mouse click for elements
		for (AbstractEditorElement<?, ?> e : this.getAllElements()) {
			e.mouseClicked(mouseX, mouseY, button);
			if (e.isHovered() || e.isGettingResized() || (e.getHoveredResizeType() != null)) {
				canStartMouseSelection = false;
			}
		}
		//Handle mouse selection
		if ((button == 0) && canStartMouseSelection) {
			this.isMouseSelection = true;
			this.mouseSelectionStartX = (int) mouseX;
			this.mouseSelectionStartY = (int) mouseY;
		}
		//Deselect all elements
		if (!this.rightClickMenu.isUserNavigatingInMenu() && ((this.activeElementContextMenu == null) || !this.activeElementContextMenu.isUserNavigatingInMenu()) && !hasControlDown()) {
			if ((button == 0) || ((button == 1) && ((topHoverElement == null) || topHoverGotSelected))) {
				for (AbstractEditorElement<?, ?> e : this.getAllElements()) {
					if (!e.isGettingResized() && ((topHoverElement == null) || (e != topHoverElement))) e.setSelected(false);
				}
			}
		}
		//Close active element context menu
		this.closeActiveElementMenu();
		//Close background right-click context menu
		if ((button == 0) && !this.rightClickMenu.isUserNavigatingInMenu()) {
			this.closeRightClickMenu();
		}
		//Open background right-click context menu
		if (topHoverElement == null) {
			if (button == 1) {
				this.openRightClickMenuAtMouse((int) mouseX, (int) mouseY);
			}
		} else if (!topHoverElement.element.layerHiddenInEditor) {
			this.closeRightClickMenu();
			//Set and open active element context menu
			if (button == 1) {
				this.openElementContextMenuAtMouseIfPossible();
			}
		}

		return false;

	}

	//Called after mouseDragged
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {

		this.anchorPointOverlay.mouseReleased(mouseX, mouseY, button);

		boolean cachedMovingStarted = this.elementMovingStarted;

		this.elementMovingStarted = false;
		this.elementResizingStarted = false;
		this.currentlyDraggedElements.clear();

		boolean mouseWasInDraggingMode = this.mouseDraggingStarted;
		this.mouseDraggingStarted = false;

		boolean cachedMouseSelection = this.isMouseSelection;
		if (button == 0) {
			this.isMouseSelection = false;
		}

		//Imitate super.mouseReleased in a way that doesn't suck
		this.setDragging(false);
		for(GuiEventListener child : this.children()) {
			if (child.mouseReleased(mouseX, mouseY, button)) return true;
		}

		List<AbstractEditorElement<?, ?>> hoveredElements = this.getHoveredElements();
		AbstractEditorElement<?, ?> topHoverElement = !hoveredElements.isEmpty() ? hoveredElements.get(hoveredElements.size()-1) : null;

		//Deselect hovered element on left-click if CTRL pressed
		if (!mouseWasInDraggingMode && !cachedMouseSelection && (button == 0) && (topHoverElement != null) && topHoverElement.isSelected() && !topHoverElement.recentlyMovedByDragging && !topHoverElement.recentlyLeftClickSelected && hasControlDown()) {
			topHoverElement.setSelected(false);
		}

		//Handle mouse released for all elements
		for (AbstractEditorElement<?, ?> e : this.getAllElements()) {
			e.mouseReleased(mouseX, mouseY, button);
			e.recentlyLeftClickSelected = false;
		}

		//Save snapshot from before started dragging element(s)
		if (cachedMovingStarted && (this.preDragElementSnapshot != null)) {
			this.history.saveSnapshot(this.preDragElementSnapshot);
		}
		this.preDragElementSnapshot = null;

		return false;

	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double $$3, double $$4) {

		if (super.mouseDragged(mouseX, mouseY, button, $$3, $$4)) return true;

		if (this.isMouseSelection) {
			for (AbstractEditorElement<?, ?> e : this.getAllElements()) {
				if (e.element.layerHiddenInEditor) continue;
				boolean b = this.isElementOverlappingArea(e, Math.min(this.mouseSelectionStartX, (int)mouseX), Math.min(this.mouseSelectionStartY, (int)mouseY), Math.max(this.mouseSelectionStartX, (int)mouseX), Math.max(this.mouseSelectionStartY, (int)mouseY));
				if (!b && hasControlDown()) continue; //skip deselect if CTRL pressed
				e.setSelected(b);
			}
		}

		int draggingDiffX = (int) (mouseX - this.leftMouseDownPosX);
		int draggingDiffY = (int) (mouseY - this.leftMouseDownPosY);
		if ((draggingDiffX != 0) || (draggingDiffY != 0)) {
			this.mouseDraggingStarted = true;
		}

		List<AbstractEditorElement<?, ?>> allElements = this.getAllElements();

		if (!this.elementResizingStarted) {
			allElements.forEach(element -> element.updateResizingStartPos((int)mouseX, (int)mouseY));
		}
		this.elementResizingStarted = true;

		boolean movingCrumpleZonePassed = (Math.abs(draggingDiffX) >= ELEMENT_DRAG_CRUMPLE_ZONE) || (Math.abs(draggingDiffY) >= ELEMENT_DRAG_CRUMPLE_ZONE);
		if (movingCrumpleZonePassed) {
			if (!this.elementMovingStarted) {
				if (this.preDragElementSnapshot == null) {
					this.preDragElementSnapshot = this.history.createSnapshot();
				}
				allElements.forEach(element -> {
					element.updateMovingStartPos((int)mouseX, (int)mouseY);
					element.movingCrumpleZonePassed = true;
				});
				if (this.allSelectedElementsMovable()) {
					this.currentlyDraggedElements.addAll(this.getSelectedElements());
				}
			}
			this.elementMovingStarted = true;
		}
		for (AbstractEditorElement<?, ?> e : allElements) {
			if (e.mouseDragged(mouseX, mouseY, button, $$3, $$4)) {
				break;
			}
		}

		return false;

	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
		return super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
	}

	@Override
	public boolean keyPressed(int keycode, int scancode, int modifiers) {

		this.anchorPointOverlay.keyPressed(keycode, scancode, modifiers);

		if (super.keyPressed(keycode, scancode, modifiers)) return true;

		for (AbstractEditorElement<?, ?> e : this.getAllElements()) {
			if (e.element.layerHiddenInEditor) continue;
			if (e.keyPressed(keycode, scancode, modifiers)) return true;
		}

		String key = GLFW.glfwGetKeyName(keycode, scancode);
		if (key == null) key = "";

		//ARROW LEFT
		if (keycode == InputConstants.KEY_LEFT) {
			this.moveSelectedElementsByXYOffset(-1, 0);
			return true;
		}

		//ARROW UP
		if (keycode == InputConstants.KEY_UP) {
			this.moveSelectedElementsByXYOffset(0, -1);
			return true;
		}

		//ARROW RIGHT
		if (keycode == InputConstants.KEY_RIGHT) {
			this.moveSelectedElementsByXYOffset(1, 0);
			return true;
		}

		//ARROW DOWN
		if (keycode == InputConstants.KEY_DOWN) {
			this.moveSelectedElementsByXYOffset(0, 1);
			return true;
		}

		//CTRL + A
		if (key.equals("a") && hasControlDown()) {
			this.selectAllElements();
		}

		//CTRL + C
		if (key.equals("c") && hasControlDown()) {
			this.copyElementsToClipboard(this.getSelectedElements().toArray(new AbstractEditorElement<?, ?>[0]));
			return true;
		}

		//CTRL + V
		if (key.equals("v") && hasControlDown()) {
			this.pasteElementsFromClipboard();
			return true;
		}

		//CTRL + S
		if (key.equals("s") && hasControlDown()) {
			this.saveLayout();
			return true;
		}

		//CTRL + Z
		if (key.equals("z") && hasControlDown()) {
			this.history.stepBack();
            this.resize(Minecraft.getInstance(), this.width, this.height);
			return true;
		}

		//CTRL + Y
		if (key.equals("y") && hasControlDown()) {
			this.history.stepForward();
            this.resize(Minecraft.getInstance(), this.width, this.height);
			return true;
		}

		//CTRL + G
		if (key.equals("g") && hasControlDown()) {
			try {
				FancyMenu.getOptions().showLayoutEditorGrid.setValue(!FancyMenu.getOptions().showLayoutEditorGrid.getValue());
			} catch (Exception ex) {
				LOGGER.error("[FANCYMENU] Failed to toggle layout editor grid!", ex);
			}
			return true;
		}

		//DEL
		if (keycode == InputConstants.KEY_DELETE) {
			this.history.saveSnapshot();
			for (AbstractEditorElement<?, ?> e : this.getSelectedElements()) {
				if (e.element.layerHiddenInEditor) continue;
				e.deleteElement();
			}
			return true;
		}

		return super.keyPressed(keycode, scancode, modifiers);

	}

	@Override
	public boolean keyReleased(int keycode, int scancode, int modifiers) {

		this.anchorPointOverlay.keyReleased(keycode, scancode, modifiers);

		for (AbstractEditorElement<?, ?> e : this.getAllElements()) {
			if (e.keyReleased(keycode, scancode, modifiers)) return true;
		}

		return super.keyReleased(keycode, scancode, modifiers);

	}

    public void openChildScreen(@NotNull Screen screen) {
        this.beforeOpenChildScreen(screen);
        Minecraft.getInstance().setScreen(screen);
    }

    public void beforeOpenChildScreen(@NotNull Screen screen) {
        this.removed();
    }

    @SuppressWarnings("deprecation")
	public void closeEditor() {
        PiPWindowHandler.INSTANCE.closeAllWindows();
        this.closeActiveElementMenu(true);
        this.closeRightClickMenu(true);
		this.saveWidgetSettings();
		this.getAllElements().forEach(element -> {
			element.element.onDestroyElement();
			element.element.onCloseScreen(null, null);
			element.element.onCloseScreen();
		});
		this.layout.menuBackgrounds.forEach(menuBackground -> menuBackground.onCloseScreen(null, null));
		this.layout.menuBackgrounds.forEach(MenuBackground::onCloseScreen);
		this.layout.decorationOverlays.forEach(pair -> pair.getSecond().onCloseScreen(null, null));
		currentInstance = null;
		if (this.layoutTargetScreen != null) {
			if (!((IMixinScreen)this.layoutTargetScreen).get_initialized_FancyMenu()) {
				Minecraft.getInstance().setScreen(this.layoutTargetScreen);
			} else {
				Minecraft.getInstance().setScreen(new GenericMessageScreen(Component.literal("Closing editor..")));
				Minecraft.getInstance().screen = this.layoutTargetScreen;
				ScreenCustomization.reInitCurrentScreen();
			}
		} else {
			Minecraft.getInstance().setScreen(null);
		}
	}

	public LayoutEditorScreen setAsCurrentInstance() {
		currentInstance = this;
		return this;
	}

	/**
	 * The currently active editor instance. This is NULL when not in the editor.
	 */
	@Nullable
	public static LayoutEditorScreen getCurrentInstance() {
		return currentInstance;
	}

}

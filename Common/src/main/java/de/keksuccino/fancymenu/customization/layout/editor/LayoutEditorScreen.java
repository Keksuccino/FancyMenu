package de.keksuccino.fancymenu.customization.layout.editor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBase;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layout.editor.widget.AbstractLayoutEditorWidget;
import de.keksuccino.fancymenu.customization.layout.editor.widget.LayoutEditorWidgetRegistry;
import de.keksuccino.fancymenu.customization.widget.ScreenWidgetDiscoverer;
import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepEditorElement;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.IHideableElement;
import de.keksuccino.fancymenu.customization.element.SerializedElement;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanilla.VanillaButtonEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanilla.VanillaButtonElement;
import de.keksuccino.fancymenu.customization.element.elements.button.vanilla.VanillaButtonElementBuilder;
import de.keksuccino.fancymenu.customization.layer.IElementFactory;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinScreen;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.*;
import de.keksuccino.fancymenu.util.rendering.ui.menubar.v2.MenuBar;
import de.keksuccino.fancymenu.util.rendering.ui.screen.NotificationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.SaveFileScreen;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class LayoutEditorScreen extends Screen implements IElementFactory {

	//TODO grid toggle in Window tab wird nicht aktualisiert, wenn grid per Ctrl + G getoggelt wird

	private static final Logger LOGGER = LogManager.getLogger();

	protected static final Map<SerializedElement, ElementBuilder<?,?>> COPIED_ELEMENTS_CLIPBOARD = new LinkedHashMap<>();

	@Nullable
	public Screen layoutTargetScreen;
	@NotNull
	public Layout layout;
	public List<AbstractEditorElement> normalEditorElements = new ArrayList<>();
	public List<VanillaButtonEditorElement> vanillaButtonEditorElements = new ArrayList<>();
	public List<AbstractDeepEditorElement> deepEditorElements = new ArrayList<>();

	public LayoutEditorHistory history = new LayoutEditorHistory(this);
	public MenuBar menuBar;
	public AnchorPointOverlay anchorPointOverlay = new AnchorPointOverlay(this);
	public ContextMenu rightClickMenu;
	public ContextMenu activeElementContextMenu = null;
	public List<AbstractLayoutEditorWidget> layoutEditorWidgets = new ArrayList<>();

	protected boolean isMouseSelection = false;
	protected int mouseSelectionStartX = 0;
	protected int mouseSelectionStartY = 0;
	protected int rightClickMenuOpenPosX = -1000;
	protected int rightClickMenuOpenPosY = -1000;
	protected LayoutEditorHistory.Snapshot preDragElementSnapshot;

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

	}

	@Override
	protected void init() {

		//Build widget instances only once (don't build in constructor to avoid stack overflows in builders)
		if ((this.layoutEditorWidgets == null) || this.layoutEditorWidgets.isEmpty()) {
			this.layoutEditorWidgets = LayoutEditorWidgetRegistry.buildWidgetInstances(this);
		}

		this.closeRightClickMenu();
		this.rightClickMenu = LayoutEditorUI.buildRightClickContextMenu(this);
		this.addWidget(this.rightClickMenu);

		if (this.menuBar != null) {
			this.menuBar.closeAllContextMenus();
		}
		this.menuBar = LayoutEditorUI.buildMenuBar(this);
		this.addWidget(this.menuBar);

		for (AbstractLayoutEditorWidget w : Lists.reverse(new ArrayList<>(this.layoutEditorWidgets))) {
			this.addWidget(w);
		}

		this.isMouseSelection = false;
		this.preDragElementSnapshot = null;

		this.closeActiveElementMenu();

		this.serializeElementInstancesToLayoutInstance();

		//Handle forced GUI scale
		if (this.layout.forcedScale != 0) {
			float newscale = this.layout.forcedScale;
			if (newscale <= 0) {
				newscale = 1;
			}
			Window m = Minecraft.getInstance().getWindow();
			m.setGuiScale(newscale);
			this.width = m.getGuiScaledWidth();
			this.height = m.getGuiScaledHeight();
		}

		//Handle auto-scaling
		if ((this.layout.autoScalingWidth != 0) && (this.layout.autoScalingHeight != 0)) {
			Window m = Minecraft.getInstance().getWindow();
			double guiWidth = this.width * m.getGuiScale();
			double guiHeight = this.height * m.getGuiScale();
			double percentX = (guiWidth / (double)this.layout.autoScalingWidth) * 100.0D;
			double percentY = (guiHeight / (double)this.layout.autoScalingHeight) * 100.0D;
			double newScaleX = (percentX / 100.0D) * m.getGuiScale();
			double newScaleY = (percentY / 100.0D) * m.getGuiScale();
			double newScale = Math.min(newScaleX, newScaleY);
			m.setGuiScale(newScale);
			this.width = m.getGuiScaledWidth();
			this.height = m.getGuiScaledHeight();
		}

		this.constructElementInstances();

		for (AbstractLayoutEditorWidget w : this.layoutEditorWidgets) {
			w.refresh();
		}

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
	}

	@Override
	public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

		//Clear active element context menu if not open
		if ((this.activeElementContextMenu != null) && !this.activeElementContextMenu.isOpen()) {
			this.activeElementContextMenu = null;
		}

		this.renderBackground(pose, mouseX, mouseY, partial);

		this.renderElements(pose, mouseX, mouseY, partial);

		this.renderMouseSelectionRectangle(pose, mouseX, mouseY);

		this.anchorPointOverlay.render(pose, mouseX, mouseY, partial);

		this.renderLayoutEditorWidgets(pose, mouseX, mouseY, partial);

		this.menuBar.render(pose, mouseX, mouseY, partial);

		this.rightClickMenu.render(pose, mouseX, mouseY, partial);

		//Render active element context menu
		if (this.activeElementContextMenu != null) {
			this.activeElementContextMenu.render(pose, mouseX, mouseY, partial);
		}

	}

	protected void renderLayoutEditorWidgets(PoseStack pose, int mouseX, int mouseY, float partial) {
		for (AbstractLayoutEditorWidget w : this.layoutEditorWidgets) {
			if (w.isVisible()) w.render(pose, mouseX, mouseY, partial);
		}
	}

	protected void renderMouseSelectionRectangle(PoseStack pose, int mouseX, int mouseY) {
		if (this.isMouseSelection) {
			int startX = Math.min(this.mouseSelectionStartX, mouseX);
			int startY = Math.min(this.mouseSelectionStartY, mouseY);
			int endX = Math.max(this.mouseSelectionStartX, mouseX);
			int endY = Math.max(this.mouseSelectionStartY, mouseY);
			fill(pose, startX, startY, endX, endY, RenderingUtils.replaceAlphaInColor(UIBase.getUIColorScheme().layout_editor_mouse_selection_rectangle_color.getColorInt(), 70));
			UIBase.renderBorder(pose, startX, startY, endX, endY, 1, UIBase.getUIColorScheme().layout_editor_mouse_selection_rectangle_color.getColor(), true, true, true, true);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		}
	}

	protected void renderElements(PoseStack pose, int mouseX, int mouseY, float partial) {

		//Render normal elements behind vanilla if renderBehindVanilla
		if (this.layout.renderElementsBehindVanilla) {
			for (AbstractEditorElement e : new ArrayList<>(this.normalEditorElements)) {
				if (!e.isSelected()) e.render(pose, mouseX, mouseY, partial);
			}
		}
		//Render vanilla button elements
		for (VanillaButtonEditorElement e : new ArrayList<>(this.vanillaButtonEditorElements)) {
			if (!e.isSelected() && !e.isHidden()) e.render(pose, mouseX, mouseY, partial);
		}
		//Render deep elements
		for (AbstractDeepEditorElement e : new ArrayList<>(this.deepEditorElements)) {
			if (!e.isSelected() && !e.isHidden()) e.render(pose, mouseX, mouseY, partial);
		}
		//Render normal elements before vanilla if NOT renderBehindVanilla
		if (!this.layout.renderElementsBehindVanilla) {
			for (AbstractEditorElement e : new ArrayList<>(this.normalEditorElements)) {
				if (!e.isSelected()) e.render(pose, mouseX, mouseY, partial);
			}
		}

		//Render selected elements last, so they're always visible
		List<AbstractEditorElement> selected = this.getSelectedElements();
		for (AbstractEditorElement e : selected) {
			e.render(pose, mouseX, mouseY, partial);
		}

	}

	protected void renderBackground(PoseStack pose, int mouseX, int mouseY, float partial) {

		fill(pose, 0, 0, this.width, this.height, UIBase.getUIColorScheme().screen_background_color_darker.getColorInt());

		if (this.layout.menuBackground != null) {
			this.layout.menuBackground.keepBackgroundAspectRatio = this.layout.keepBackgroundAspectRatio;
			this.layout.menuBackground.opacity = 1.0F;
			this.layout.menuBackground.render(pose, mouseX, mouseY, partial);
		}

		this.renderGrid(pose);

	}

	protected void renderGrid(PoseStack pose) {

		if (FancyMenu.getOptions().showLayoutEditorGrid.getValue()) {

			int gridSize = FancyMenu.getOptions().layoutEditorGridSize.getValue();
			int lineThickness = 1;

			//Draw centered vertical line
			fill(pose, (this.width / 2) - 1, 0, (this.width / 2) + 1, this.height, UIBase.getUIColorScheme().layout_editor_grid_color_center.getColorInt());

			//Draw vertical lines center -> left
			int linesVerticalToLeftPosX = (this.width / 2) - gridSize - 1;
			while (linesVerticalToLeftPosX > 0) {
				int minY = 0;
				int maxY = this.height;
				int maxX = linesVerticalToLeftPosX + lineThickness;
				fill(pose, linesVerticalToLeftPosX, minY, maxX, maxY, UIBase.getUIColorScheme().layout_editor_grid_color_normal.getColorInt());
				linesVerticalToLeftPosX -= gridSize;
			}

			//Draw vertical lines center -> right
			int linesVerticalToRightPosX = (this.width / 2) + gridSize;
			while (linesVerticalToRightPosX < this.width) {
				int minY = 0;
				int maxY = this.height;
				int maxX = linesVerticalToRightPosX + lineThickness;
				fill(pose, linesVerticalToRightPosX, minY, maxX, maxY, UIBase.getUIColorScheme().layout_editor_grid_color_normal.getColorInt());
				linesVerticalToRightPosX += gridSize;
			}

			//Draw centered horizontal line
			fill(pose, 0, (this.height / 2) - 1, this.width, (this.height / 2) + 1, UIBase.getUIColorScheme().layout_editor_grid_color_center.getColorInt());

			//Draw horizontal lines center -> top
			int linesHorizontalToTopPosY = (this.height / 2) - gridSize - 1;
			while (linesHorizontalToTopPosY > 0) {
				int minX = 0;
				int maxX = this.width;
				int maxY = linesHorizontalToTopPosY + lineThickness;
				fill(pose, minX, linesHorizontalToTopPosY, maxX, maxY, UIBase.getUIColorScheme().layout_editor_grid_color_normal.getColorInt());
				linesHorizontalToTopPosY -= gridSize;
			}

			//Draw horizontal lines center -> bottom
			int linesHorizontalToBottomPosY = (this.height / 2) + gridSize;
			while (linesHorizontalToBottomPosY < this.height) {
				int minX = 0;
				int maxX = this.width;
				int maxY = linesHorizontalToBottomPosY + lineThickness;
				fill(pose, minX, linesHorizontalToBottomPosY, maxX, maxY, UIBase.getUIColorScheme().layout_editor_grid_color_normal.getColorInt());
				linesHorizontalToBottomPosY += gridSize;
			}

		}

	}

	protected void constructElementInstances() {

		//Clear element lists
		for (AbstractEditorElement e : this.getAllElements()) {
			e.resetElementStates();
		}
		this.normalEditorElements.clear();
		this.vanillaButtonEditorElements.clear();
		this.deepEditorElements.clear();

		Layout.OrderedElementCollection normalElements = new Layout.OrderedElementCollection();
		List<VanillaButtonElement> vanillaButtonElements = (this.layoutTargetScreen != null) ? new ArrayList<>() : null;
		List<AbstractDeepElement> deepElements = (this.layoutTargetScreen != null) ? new ArrayList<>() : null;

		List<WidgetMeta> vanillaWidgetMetaList = (this.layoutTargetScreen != null) ? ScreenWidgetDiscoverer.getWidgetMetasOfScreen(this.layoutTargetScreen, true) : null;

		this.constructElementInstances(this.layout.menuIdentifier, vanillaWidgetMetaList, this.layout, normalElements, vanillaButtonElements, deepElements);

		//Wrap normal elements
		for (AbstractElement e : ListUtils.mergeLists(normalElements.backgroundElements, normalElements.foregroundElements)) {
			AbstractEditorElement editorElement = e.builder.wrapIntoEditorElementInternal(e, this);
			if (editorElement != null) {
				this.normalEditorElements.add(editorElement);
			}
		}
		//Wrap deep elements
		if (deepElements != null) {
			for (AbstractElement e : deepElements) {
				AbstractEditorElement editorElement = e.builder.wrapIntoEditorElementInternal(e, this);
				if (editorElement instanceof AbstractDeepEditorElement d) {
					this.deepEditorElements.add(d);
				}
			}
		}
		//Wrap vanilla elements
		if (vanillaButtonElements != null) {
			for (VanillaButtonElement e : vanillaButtonElements) {
				VanillaButtonEditorElement editorElement = (VanillaButtonEditorElement) VanillaButtonElementBuilder.INSTANCE.wrapIntoEditorElementInternal(e, this);
				if (editorElement != null) {
					this.vanillaButtonEditorElements.add(editorElement);
				}
			}
		}

	}

	protected void serializeElementInstancesToLayoutInstance() {

		this.layout.serializedElements.clear();
		this.layout.serializedVanillaButtonElements.clear();
		this.layout.serializedDeepElements.clear();

		//Serialize normal elements
		for (AbstractEditorElement e : this.normalEditorElements) {
			SerializedElement serialized = e.element.builder.serializeElementInternal(e.element);
			if (serialized != null) {
				this.layout.serializedElements.add(serialized);
			}
		}
		//Serialize deep elements
		for (AbstractEditorElement e : this.deepEditorElements) {
			SerializedElement serialized = e.element.builder.serializeElementInternal(e.element);
			if (serialized != null) {
				this.layout.serializedDeepElements.add(serialized);
			}
		}
		//Serialize vanilla button elements
		for (VanillaButtonEditorElement e : this.vanillaButtonEditorElements) {
			SerializedElement serialized = VanillaButtonElementBuilder.INSTANCE.serializeElementInternal(e.element);
			if (serialized != null) {
				this.layout.serializedVanillaButtonElements.add(serialized);
			}
		}

	}

	@NotNull
	public List<AbstractEditorElement> getAllElements() {
		List<AbstractEditorElement> elements = new ArrayList<>();
		List<AbstractEditorElement> selected = new ArrayList<>();
		List<AbstractEditorElement> elementsFinal = new ArrayList<>();
		if (this.layout.renderElementsBehindVanilla) {
			elements.addAll(this.normalEditorElements);
		}
		elements.addAll(this.vanillaButtonEditorElements);
		elements.addAll(this.deepEditorElements);
		if (!this.layout.renderElementsBehindVanilla) {
			elements.addAll(this.normalEditorElements);
		}
		//Put selected elements at the end, because they are always on top
		for (AbstractEditorElement e : elements) {
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
	public List<AbstractEditorElement> getHoveredElements() {
		List<AbstractEditorElement> elements = new ArrayList<>();
		for (AbstractEditorElement e : this.getAllElements()) {
			if (e.isHovered()) {
				if ((e instanceof IHideableElement h) && h.isHidden()) continue;
				elements.add(e);
			}
		}
		return elements;
	}

	@Nullable
	public AbstractEditorElement getTopHoveredElement() {
		List<AbstractEditorElement> hoveredElements = this.getHoveredElements();
		return (hoveredElements.size() > 0) ? hoveredElements.get(hoveredElements.size()-1) : null;
	}

	@NotNull
	public List<AbstractEditorElement> getSelectedElements() {
		List<AbstractEditorElement> l = new ArrayList<>();
		this.getAllElements().forEach((element) -> {
			if (element.isSelected()) {
				l.add(element);
			}
		});
		return l;
	}

	@SuppressWarnings("all")
	@NotNull
	protected <E extends AbstractEditorElement> List<E> getSelectedElementsOfType(@NotNull Class<E> type) {
		List<E> l = new ArrayList<>();
		for (AbstractEditorElement e : this.getSelectedElements()) {
			if (type.isAssignableFrom(e.getClass())) {
				l.add((E)e);
			}
		}
		return l;
	}

	@Nullable
	public AbstractEditorElement getElementByInstanceIdentifier(@NotNull String instanceIdentifier) {
		for (AbstractEditorElement e : this.getAllElements()) {
			if (e.element.getInstanceIdentifier().equals(instanceIdentifier)) {
				return e;
			}
		}
		return null;
	}

	public void selectAllElements() {
		for (AbstractEditorElement e : this.getAllElements()) {
			e.setSelected(true);
		}
	}

	public void deselectAllElements() {
		for (AbstractEditorElement e : this.getAllElements()) {
			e.setSelected(false);
		}
	}

	@SuppressWarnings("all")
	public boolean deleteElement(@NotNull AbstractEditorElement element) {
		if (element.settings.isDestroyable()) {
			if (!element.settings.shouldHideInsteadOfDestroy()) {
				this.normalEditorElements.remove(element);
				this.vanillaButtonEditorElements.remove(element);
				this.deepEditorElements.remove(element);
				for (AbstractLayoutEditorWidget w : this.layoutEditorWidgets) {
					w.editorElementRemovedOrHidden(element);
				}
				return true;
			} else if (element instanceof IHideableElement hideable) {
				hideable.setHidden(true);
				return true;
			}
		}
		return false;
	}

	protected boolean isElementOverlappingArea(@NotNull AbstractEditorElement element, int xStart, int yStart, int xEnd, int yEnd) {
		int elementStartX = element.getX();
		int elementStartY = element.getY();
		int elementEndX = element.getX() + element.getWidth();
		int elementEndY = element.getY() + element.getHeight();
		return (xEnd > elementStartX) && (yEnd > elementStartY) && (yStart < elementEndY) && (xStart < elementEndX);
	}

	public boolean allSelectedElementsMovable() {
		for (AbstractEditorElement e : this.getSelectedElements()) {
			if (!e.settings.isMovable()) return false;
		}
		return true;
	}

	public boolean canBeMovedUp(AbstractEditorElement element) {
		int index = this.normalEditorElements.indexOf(element);
		if (index == -1) return false;
		return index < this.normalEditorElements.size()-1;
	}

	public boolean canBeMovedDown(AbstractEditorElement element) {
		int index = this.normalEditorElements.indexOf(element);
		return index > 0;
	}

	/**
	 * Returns the element the given one was moved above or NULL if there was no element above the given one.
	 */
	@Nullable
	public AbstractEditorElement moveElementUp(@NotNull AbstractEditorElement element) {
		AbstractEditorElement movedAbove = null;
		try {
			if (this.normalEditorElements.contains(element)) {
				List<AbstractEditorElement> newNormalEditorElements = new ArrayList<>();
				int index = this.normalEditorElements.indexOf(element);
				int i = 0;
				if (index < (this.normalEditorElements.size() - 1)) {
					for (AbstractEditorElement e : this.normalEditorElements) {
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
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return movedAbove;
	}

	/**
	 * Returns the element the given one was moved behind or NULL if there was no element behind the given one.
	 */
	@Nullable
	public AbstractEditorElement moveElementDown(AbstractEditorElement element) {
		AbstractEditorElement movedBehind = null;
		try {
			if (this.normalEditorElements.contains(element)) {
				List<AbstractEditorElement> newNormalEditorElements = new ArrayList<>();
				int index = this.normalEditorElements.indexOf(element);
				int i = 0;
				if (index > 0) {
					for (AbstractEditorElement e : this.normalEditorElements) {
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
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return movedBehind;
	}

	public void copyElementsToClipboard(AbstractEditorElement... elements) {
		if ((elements != null) && (elements.length > 0)) {
			COPIED_ELEMENTS_CLIPBOARD.clear();
			for (AbstractEditorElement e : elements) {
				if (e.settings.isCopyable()) {
					SerializedElement serialized = e.element.builder.serializeElementInternal(e.element);
					if (serialized != null) {
						serialized.putProperty("instance_identifier", ScreenCustomization.generateUniqueIdentifier());
						COPIED_ELEMENTS_CLIPBOARD.put(serialized, e.element.builder);
					}
				}
			}
		}
	}

	public void pasteElementsFromClipboard() {
		if (!COPIED_ELEMENTS_CLIPBOARD.isEmpty()) {
			this.deselectAllElements();
			for (Map.Entry<SerializedElement, ElementBuilder<?,?>> m : COPIED_ELEMENTS_CLIPBOARD.entrySet()) {
				AbstractElement deserialized = m.getValue().deserializeElementInternal(m.getKey());
				if (deserialized != null) {
					AbstractEditorElement deserializedEditorElement = m.getValue().wrapIntoEditorElementInternal(deserialized, this);
					if (deserializedEditorElement != null) {
						this.normalEditorElements.add(deserializedEditorElement);
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
				Minecraft.getInstance().setScreen(NotificationScreen.error((call) -> {
					Minecraft.getInstance().setScreen(this);
				}, LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.saving_failed.generic")));
			} else {
				LayoutHandler.reloadLayouts();
			}
		} else {
			this.saveLayoutAs();
		}
	}

	public void saveLayoutAs() {
		String fileNamePreset = "universal_layout";
		if (this.layoutTargetScreen != null) {
			if (this.layoutTargetScreen instanceof CustomGuiBase c) {
				fileNamePreset = c.getIdentifier() + "_layout";
			} else {
				fileNamePreset = this.layoutTargetScreen.getClass().getSimpleName() + "_layout";
			}
		}
		fileNamePreset = fileNamePreset.toLowerCase();
		fileNamePreset = CharacterFilter.buildBasicFilenameCharacterFilter().filterForAllowedChars(fileNamePreset);
		fileNamePreset = FileUtils.generateAvailableFilename(LayoutHandler.LAYOUT_DIR.getAbsolutePath(), fileNamePreset, "txt");
		if (this.layout.layoutFile != null) {
			fileNamePreset = this.layout.layoutFile.getName();
		}
		Minecraft.getInstance().setScreen(SaveFileScreen.build(LayoutHandler.LAYOUT_DIR, fileNamePreset, "txt", (call) -> {
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
						Minecraft.getInstance().setScreen(NotificationScreen.error((call2) -> {
							Minecraft.getInstance().setScreen(this);
						}, LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.saving_failed.generic")));
					} else {
						LayoutHandler.reloadLayouts();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					Minecraft.getInstance().setScreen(NotificationScreen.error((call2) -> {
						Minecraft.getInstance().setScreen(this);
					}, LocalizationUtils.splitLocalizedStringLines("fancymenu.editor.saving_failed.generic")));
				}
			}
			Minecraft.getInstance().setScreen(this);
		}).setVisibleDirectoryLevelsAboveRoot(2).setShowSubDirectories(false));
	}

	public void onUpdateSelectedElements() {
		List<AbstractEditorElement> selected = this.getSelectedElements();
		if (selected.size() > 1) {
			for (AbstractEditorElement e : selected) {
				e.setMultiSelected(true);
			}
		} else if (selected.size() == 1) {
			selected.get(0).setMultiSelected(false);
		}
	}

	public void openRightClickMenuAtMouse(int mouseX, int mouseY) {
		if (this.rightClickMenu != null) {
			this.rightClickMenuOpenPosX = mouseX;
			this.rightClickMenuOpenPosY = mouseY;
			this.rightClickMenu.openMenuAtMouse();
		}
	}

	public void closeRightClickMenu() {
		if (this.rightClickMenu != null) {
			if (this.rightClickMenu.isUserNavigatingInMenu()) return;
			this.rightClickMenuOpenPosX = -1000;
			this.rightClickMenuOpenPosY = -1000;
			this.rightClickMenu.closeMenu();
		}
	}

	public void openElementContextMenuAtMouseIfPossible() {
		this.closeActiveElementMenu();
		List<AbstractEditorElement> selectedElements = this.getSelectedElements();
		if (selectedElements.size() == 1) {
			this.activeElementContextMenu = selectedElements.get(0).rightClickMenu;
			((IMixinScreen)this).getChildrenFancyMenu().add(0, this.activeElementContextMenu);
			this.activeElementContextMenu.openMenuAtMouse();
		} else if (selectedElements.size() > 1) {
			List<ContextMenu> menus = ObjectUtils.getOfAll(ContextMenu.class, selectedElements, consumes -> consumes.rightClickMenu);
			this.activeElementContextMenu = ContextMenu.stackContextMenus(menus);
			((IMixinScreen)this).getChildrenFancyMenu().add(0, this.activeElementContextMenu);
			this.activeElementContextMenu.openMenuAtMouse();
		}
	}

	public void closeActiveElementMenu() {
		if (this.activeElementContextMenu != null) {
			if (this.activeElementContextMenu.isUserNavigatingInMenu()) return;
			this.activeElementContextMenu.closeMenu();
			this.removeWidget(this.activeElementContextMenu);
		}
		this.activeElementContextMenu = null;
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

	//Called before mouseDragged
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {

		boolean menuBarContextOpen = (this.menuBar != null) && this.menuBar.isEntryContextMenuOpen();

		if (PopupHandler.isPopupActive()) {
			this.closeRightClickMenu();
			this.closeActiveElementMenu();
			return false;
		}

		if (super.mouseClicked(mouseX, mouseY, button)) {
			this.closeRightClickMenu();
			this.closeActiveElementMenu();
			return true;
		}

		//Skip the first click out of the menu bar context menus
		if (menuBarContextOpen) return true;

		AbstractEditorElement topHoverElement = this.getTopHoveredElement();

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
		for (AbstractEditorElement e : this.getAllElements()) {
			e.mouseClicked(mouseX, mouseY, button);
			if (e.isHovered() || e.isGettingResized() || (e.getHoveredResizeGrabber() != null)) {
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
				for (AbstractEditorElement e : this.getAllElements()) {
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
		} else {
			this.closeRightClickMenu();
			//Set and open active element context menu
			if (button == 1) {
				this.openElementContextMenuAtMouseIfPossible();
			}
		}

		this.anchorPointOverlay.mouseClicked(mouseX, mouseY, button);

		return false;

	}

	//Called after mouseDragged
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {

		boolean cachedMouseSelection = this.isMouseSelection;
		if (button == 0) {
			this.isMouseSelection = false;
		}

		if (PopupHandler.isPopupActive()) return false;

		//Imitate super.mouseReleased in a way that doesn't suck
		this.setDragging(false);
		for(GuiEventListener child : this.children()) {
			if (child.mouseReleased(mouseX, mouseY, button)) return true;
		}

		List<AbstractEditorElement> hoveredElements = this.getHoveredElements();
		AbstractEditorElement topHoverElement = (hoveredElements.size() > 0) ? hoveredElements.get(hoveredElements.size()-1) : null;

		//Deselect hovered element on left-click if CTRL pressed
		if (!cachedMouseSelection && (button == 0) && (topHoverElement != null) && topHoverElement.isSelected() && !topHoverElement.recentlyMovedByDragging && !topHoverElement.recentlyLeftClickSelected && hasControlDown()) {
			topHoverElement.setSelected(false);
		}

		boolean elementRecentlyMovedByDragging = false;

		//Handle mouse released for all elements
		for (AbstractEditorElement e : this.getAllElements()) {
			if (e.recentlyMovedByDragging) elementRecentlyMovedByDragging = true;
			e.mouseReleased(mouseX, mouseY, button);
			e.recentlyLeftClickSelected = false;
		}

		//Save snapshot from before started dragging element(s)
		if (elementRecentlyMovedByDragging && (this.preDragElementSnapshot != null)) {
			this.history.saveSnapshot(this.preDragElementSnapshot);
		}
		this.preDragElementSnapshot = null;

		this.anchorPointOverlay.mouseReleased(mouseX, mouseY, button);

		return false;

	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double $$3, double $$4) {

		if (PopupHandler.isPopupActive()) return false;

		if (super.mouseDragged(mouseX, mouseY, button, $$3, $$4)) return true;

		if (this.isMouseSelection) {
			for (AbstractEditorElement e : this.getAllElements()) {
				boolean b = this.isElementOverlappingArea(e, Math.min(this.mouseSelectionStartX, (int)mouseX), Math.min(this.mouseSelectionStartY, (int)mouseY), Math.max(this.mouseSelectionStartX, (int)mouseX), Math.max(this.mouseSelectionStartY, (int)mouseY));
				if (!b && hasControlDown()) continue; //skip deselect if CTRL pressed
				e.setSelected(b);
			}
		}

		if (this.preDragElementSnapshot == null) {
			this.preDragElementSnapshot = this.history.createSnapshot();
		}

		for (AbstractEditorElement e : this.getAllElements()) {
			if (e.mouseDragged(mouseX, mouseY, button, $$3, $$4)) {
				break;
			}
		}

		this.anchorPointOverlay.mouseDragged(mouseX, mouseY, button, $$3, $$4);

		return false;

	}

	@Override
	public boolean keyPressed(int keycode, int scancode, int $$2) {

		if (PopupHandler.isPopupActive()) return false;

		if (super.keyPressed(keycode, scancode, $$2)) return true;

		String key = GLFW.glfwGetKeyName(keycode, scancode);
		if (key == null) key = "";

		//ARROW LEFT
		if (keycode == InputConstants.KEY_LEFT) {
			this.history.saveSnapshot();
			for (AbstractEditorElement e : this.getSelectedElements()) {
				if (this.allSelectedElementsMovable()) {
					e.element.posOffsetX -= 1;
				} else if (!e.settings.isMovable()) {
					e.renderMovingNotAllowedTime = System.currentTimeMillis() + 800;
				}
			}
			return true;
		}

		//ARROW UP
		if (keycode == InputConstants.KEY_UP) {
			this.history.saveSnapshot();
			for (AbstractEditorElement e : this.getSelectedElements()) {
				if (this.allSelectedElementsMovable()) {
					e.element.posOffsetY -= 1;
				} else if (!e.settings.isMovable()) {
					e.renderMovingNotAllowedTime = System.currentTimeMillis() + 800;
				}
			}
			return true;
		}

		//ARROW RIGHT
		if (keycode == InputConstants.KEY_RIGHT) {
			this.history.saveSnapshot();
			for (AbstractEditorElement e : this.getSelectedElements()) {
				if (this.allSelectedElementsMovable()) {
					e.element.posOffsetX += 1;
				} else if (!e.settings.isMovable()) {
					e.renderMovingNotAllowedTime = System.currentTimeMillis() + 800;
				}
			}
			return true;
		}

		//ARROW DOWN
		if (keycode == InputConstants.KEY_DOWN) {
			this.history.saveSnapshot();
			for (AbstractEditorElement e : this.getSelectedElements()) {
				if (this.allSelectedElementsMovable()) {
					e.element.posOffsetY += 1;
				} else if (!e.settings.isMovable()) {
					e.renderMovingNotAllowedTime = System.currentTimeMillis() + 800;
				}
			}
			return true;
		}

		//CTRL + A
		if (key.equals("a") && hasControlDown()) {
			this.selectAllElements();
		}

		//CTRL + C
		if (key.equals("c") && hasControlDown()) {
			this.copyElementsToClipboard(this.getSelectedElements().toArray(new AbstractEditorElement[0]));
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
			return true;
		}

		//CTRL + Y
		if (key.equals("y") && hasControlDown()) {
			this.history.stepForward();
			return true;
		}

		//CTRL + G
		if (key.equals("g") && hasControlDown()) {
			try {
				FancyMenu.getOptions().showLayoutEditorGrid.setValue(!FancyMenu.getOptions().showLayoutEditorGrid.getValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		}

		//DEL
		if (keycode == InputConstants.KEY_DELETE) {
			this.history.saveSnapshot();
			for (AbstractEditorElement e : this.getSelectedElements()) {
				e.deleteElement();
			}
			return true;
		}

		return super.keyPressed(keycode, scancode, $$2);

	}

}

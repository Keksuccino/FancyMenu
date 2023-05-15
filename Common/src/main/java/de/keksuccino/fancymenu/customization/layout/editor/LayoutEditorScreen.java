package de.keksuccino.fancymenu.customization.layout.editor;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.button.ButtonCache;
import de.keksuccino.fancymenu.customization.button.ButtonData;
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
import de.keksuccino.fancymenu.misc.InputConstants;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.AdvancedContextMenu;
import de.keksuccino.fancymenu.utils.ListUtils;
import de.keksuccino.fancymenu.utils.ScreenTitleUtils;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LayoutEditorScreen extends Screen implements IElementFactory {

	//TODO komplett rewriten

	//TODO alte key press handler weg -> stattdessen die von Screen nutzen

	//TODO GUI event listener methoden von Editor Elementen hier in richtigen methoden callen

	//TODO das meiste der gecachten top-level layout properties von hier in eigene Layout Klasse verschieben

	private static final Logger LOGGER = LogManager.getLogger();

	protected static final Map<SerializedElement, ElementBuilder<?,?>> COPIED_ELEMENTS_CLIPBOARD = new LinkedHashMap<>();

	public static final Color GRID_COLOR_NORMAL = new Color(255, 255, 255, 100);
	public static final Color GRID_COLOR_CENTER = new Color(150, 105, 255, 100);

	@Nullable
	public Screen screenToCustomize;
	@NotNull
	public Layout layout;
	public List<AbstractEditorElement> normalEditorElements = new ArrayList<>();
	public List<VanillaButtonEditorElement> vanillaButtonEditorElements = new ArrayList<>();
	public List<AbstractDeepEditorElement> deepEditorElements = new ArrayList<>();

	public LayoutEditorHistory history = new LayoutEditorHistory(this);
	public LayoutEditorUI ui = new LayoutEditorUI(this);
	public AdvancedContextMenu rightClickContextMenu = new AdvancedContextMenu();
	
	public LayoutEditorScreen(@Nullable Screen screenToCustomize, @Nullable Layout layout) {

		super(Component.literal(""));

		this.screenToCustomize = screenToCustomize;
		if (layout != null) {
			this.layout = layout.copy();
		} else {
			this.layout = new Layout();
		}

		Component cachedOriTitle = ScreenCustomizationLayer.cachedOriginalMenuTitles.get(this.screenToCustomize.getClass());
		if (cachedOriTitle != null) {
			ScreenTitleUtils.setScreenTitle(this.screenToCustomize, cachedOriTitle);
		}

		//Load all element instances before init, so the layout instance elements don't get wiped when updating it
		this.constructElementInstances();

	}

	@Override
	protected void init() {

		this.ui.updateUI();

		this.rightClickContextMenu.closeMenu();
		//TODO init context menu

		this.serializeElementInstancesToLayoutInstance();

		//Clear element lists
		for (AbstractEditorElement e : this.getAllElements()) {
			e.resetElementStates();
		}
		this.normalEditorElements.clear();
		this.vanillaButtonEditorElements.clear();
		this.deepEditorElements.clear();

		this.constructElementInstances();

	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

		this.renderBackground(pose, mouseX, mouseY, partial);

		this.renderElements(pose, mouseX, mouseY, partial);

	}

	protected void renderElements(PoseStack pose, int mouseX, int mouseY, float partial) {

		//Render normal elements behind vanilla if renderBehindVanilla
		if (this.layout.renderElementsBehindVanilla) {
			for (AbstractEditorElement e : new ArrayList<>(this.normalEditorElements)) {
				if (!e.isSelected()) e.render(pose, mouseX, mouseY, partial);
			}
		}
		//Render deep elements
		for (AbstractEditorElement e : new ArrayList<>(this.deepEditorElements)) {
			if (!e.isSelected()) e.render(pose, mouseX, mouseY, partial);
		}
		//Render vanilla button elements
		for (AbstractEditorElement e : new ArrayList<>(this.vanillaButtonEditorElements)) {
			if (!e.isSelected()) e.render(pose, mouseX, mouseY, partial);
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

		//Render element context menus
		for (AbstractEditorElement e : selected) {
			e.menu.render(pose, mouseX, mouseY, partial);
		}

	}

	protected void renderBackground(PoseStack pose, int mouseX, int mouseY, float partial) {

		fill(pose, 0, 0, this.width, this.height, UIBase.SCREEN_BACKGROUND_COLOR_DARK.getRGB());

		if (this.layout.menuBackground != null) {
			this.layout.menuBackground.keepBackgroundAspectRatio = this.layout.keepBackgroundAspectRatio;
			this.layout.menuBackground.opacity = 1.0F;
			this.layout.menuBackground.render(pose, mouseX, mouseY, partial);
		}

		this.renderGrid(pose);

	}

	protected void renderGrid(PoseStack pose) {

		if (FancyMenu.getConfig().getOrDefault("showgrid", false)) {

			int gridSize = FancyMenu.getConfig().getOrDefault("gridsize", 10);
			int lineThickness = 1;

			//Draw centered vertical line
			fill(pose, (this.width / 2) - 1, 0, (this.width / 2) + 1, this.height, GRID_COLOR_CENTER.getRGB());

			//Draw vertical lines center -> left
			int linesVerticalToLeftPosX = (this.width / 2) - gridSize - 1;
			while (linesVerticalToLeftPosX > 0) {
				int minY = 0;
				int maxY = this.height;
				int maxX = linesVerticalToLeftPosX + lineThickness;
				fill(pose, linesVerticalToLeftPosX, minY, maxX, maxY, GRID_COLOR_NORMAL.getRGB());
				linesVerticalToLeftPosX -= gridSize;
			}

			//Draw vertical lines center -> right
			int linesVerticalToRightPosX = (this.width / 2) + gridSize;
			while (linesVerticalToRightPosX < this.width) {
				int minY = 0;
				int maxY = this.height;
				int maxX = linesVerticalToRightPosX + lineThickness;
				fill(pose, linesVerticalToRightPosX, minY, maxX, maxY, GRID_COLOR_NORMAL.getRGB());
				linesVerticalToRightPosX += gridSize;
			}

			//Draw centered horizontal line
			fill(pose, 0, (this.height / 2) - 1, this.width, (this.height / 2) + 1, GRID_COLOR_CENTER.getRGB());

			//Draw horizontal lines center -> top
			int linesHorizontalToTopPosY = (this.height / 2) - gridSize - 1;
			while (linesHorizontalToTopPosY > 0) {
				int minX = 0;
				int maxX = this.width;
				int maxY = linesHorizontalToTopPosY + lineThickness;
				fill(pose, minX, linesHorizontalToTopPosY, maxX, maxY, GRID_COLOR_NORMAL.getRGB());
				linesHorizontalToTopPosY -= gridSize;
			}

			//Draw horizontal lines center -> bottom
			int linesHorizontalToBottomPosY = (this.height / 2) + gridSize;
			while (linesHorizontalToBottomPosY < this.height) {
				int minX = 0;
				int maxX = this.width;
				int maxY = linesHorizontalToBottomPosY + lineThickness;
				fill(pose, minX, linesHorizontalToBottomPosY, maxX, maxY, GRID_COLOR_NORMAL.getRGB());
				linesHorizontalToBottomPosY += gridSize;
			}

		}

	}

	protected void constructElementInstances() {

		Layout.OrderedElementCollection normalElements = new Layout.OrderedElementCollection();
		List<VanillaButtonElement> vanillaButtonElements = (this.screenToCustomize != null) ? new ArrayList<>() : null;
		List<AbstractDeepElement> deepElements = (this.screenToCustomize != null) ? new ArrayList<>() : null;

		if (this.screenToCustomize != null) {
			ButtonCache.cacheButtons(this.screenToCustomize, this.width, this.height);
		}

		List<ButtonData> vanillaButtonDataList = (this.screenToCustomize != null) ? ButtonCache.getButtons() : null;

		this.constructElementInstances(this.layout.menuIdentifier, vanillaButtonDataList, this.layout, normalElements, vanillaButtonElements, deepElements);

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
				if ((editorElement instanceof AbstractDeepEditorElement)) {
					this.deepEditorElements.add((AbstractDeepEditorElement) editorElement);
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
		if (this.layout.keepBackgroundAspectRatio) {
			elements.addAll(this.normalEditorElements);
		}
		elements.addAll(this.deepEditorElements);
		elements.addAll(this.vanillaButtonEditorElements);
		if (!this.layout.keepBackgroundAspectRatio) {
			elements.addAll(this.normalEditorElements);
		}
		return elements;
	}

	@NotNull
	public List<AbstractEditorElement> getHoveredElements() {
		List<AbstractEditorElement> elements = new ArrayList<>();
		for (AbstractEditorElement e : this.getAllElements()) {
			if (e.isHovered()) elements.add(e);
		}
		return elements;
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
				return true;
			} else if (element instanceof IHideableElement hideable) {
				hideable.setHidden(true);
				return true;
			}
		}
		return false;
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

	public void onUpdateSelectedElements() {
		List<AbstractEditorElement> selected = this.getSelectedElements();
		if (selected.size() > 1) {
			for (AbstractEditorElement e : selected) {
				e.setMultiSelected(true);
			}
		} else {
			selected.get(0).setMultiSelected(false);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {

		if (PopupHandler.isPopupActive()) return false;

		List<AbstractEditorElement> hoveredElements = this.getHoveredElements();
		AbstractEditorElement topHoverElement = (hoveredElements.size() > 0) ? hoveredElements.get(hoveredElements.size()-1) : null;
		if ((button == 0) && !this.rightClickContextMenu.isUserNavigatingInMenu()) {
			this.rightClickContextMenu.closeMenu();
		}
		if (topHoverElement == null) {
			this.deselectAllElements();
			if (button == 1) {
				this.rightClickContextMenu.openMenuAtMouse();
			}
		}
		for (AbstractEditorElement e : this.getAllElements()) {
			if ((e == topHoverElement) && !e.isSelected() && !hasControlDown()) {
				e.setSelected(true);
				return true;
			} else if ((e == topHoverElement) && hasControlDown()) {
				e.setSelected(!e.isSelected());
				return true;
			}
			if (e.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);

	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {

		if (PopupHandler.isPopupActive()) return false;

		for (AbstractEditorElement e : this.getAllElements()) {
			if (e.mouseReleased(mouseX, mouseY, button)) {
				return true;
			}
		}
		return super.mouseReleased(mouseX, mouseY, button);

	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double $$3, double $$4) {

		if (PopupHandler.isPopupActive()) return false;

		for (AbstractEditorElement e : this.getAllElements()) {
			if (e.mouseDragged(mouseX, mouseY, button, $$3, $$4)) {
				return true;
			}
		}
		return super.mouseDragged(mouseX, mouseY, button, $$3, $$4);

	}

	@Override
	public boolean keyPressed(int keycode, int $$1, int $$2) {

		if (PopupHandler.isPopupActive()) return false;

		//TODO add arrow key move element

		//CTRL + C
		if ((keycode == InputConstants.KEY_C) && hasControlDown()) {
			this.copyElementsToClipboard(this.getSelectedElements().toArray(new AbstractEditorElement[0]));
			return true;
		}

		//CTRL + V
		if ((keycode == InputConstants.KEY_V) && hasControlDown()) {
			this.pasteElementsFromClipboard();
			return true;
		}

		//CTRL + S
		if ((keycode == InputConstants.KEY_S) && hasControlDown()) {
			//TODO save layout / save layout as
			return true;
		}

		//CTRL + Z
		if ((keycode == InputConstants.KEY_Z) && hasControlDown()) {
			this.history.stepBack();
			return true;
		}

		//CTRL + Y
		if ((keycode == InputConstants.KEY_Y) && hasControlDown()) {
			this.history.stepForward();
			return true;
		}

		//CTRL + G
		if ((keycode == InputConstants.KEY_G) && hasControlDown()) {
			try {
				FancyMenu.getConfig().setValue("showgrid", !FancyMenu.getConfig().getOrDefault("showgrid", false));
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		}

		//DEL
		if (keycode == InputConstants.KEY_DELETE) {
			for (AbstractEditorElement e : this.getSelectedElements()) {
				e.deleteElement();
			}
			return true;
		}

		return super.keyPressed(keycode, $$1, $$2);

	}

}

package de.keksuccino.fancymenu.customization.element.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.layout.editor.AnchorPointOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements.ManageRequirementsScreen;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.util.*;
import de.keksuccino.fancymenu.util.cycle.ValueCycle;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroups;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.NotificationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.resource.Resource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@SuppressWarnings("deprecation")
public abstract class AbstractEditorElement implements Renderable, GuiEventListener {

	private static final Logger LOGGER = LogManager.getLogger();

	protected static final ResourceLocation DRAGGING_NOT_ALLOWED_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/not_allowed.png");
	protected static final ResourceLocation DEPRECATED_WARNING_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/warning_20x20.png");
	protected static final ConsumingSupplier<AbstractEditorElement, Integer> BORDER_COLOR = (editorElement) -> {
		if (editorElement.isSelected()) {
			return UIBase.getUIColorTheme().layout_editor_element_border_color_selected.getColorInt();
		}
		return UIBase.getUIColorTheme().layout_editor_element_border_color_normal.getColorInt();
	};

	public AbstractElement element;
	public final EditorElementSettings settings;
	public ContextMenu rightClickMenu;
	public EditorElementBorderDisplay topLeftDisplay = new EditorElementBorderDisplay(this, EditorElementBorderDisplay.DisplayPosition.TOP_LEFT, EditorElementBorderDisplay.DisplayPosition.LEFT_TOP, EditorElementBorderDisplay.DisplayPosition.BOTTOM_LEFT);
	public EditorElementBorderDisplay bottomRightDisplay = new EditorElementBorderDisplay(this, EditorElementBorderDisplay.DisplayPosition.BOTTOM_RIGHT, EditorElementBorderDisplay.DisplayPosition.RIGHT_BOTTOM, EditorElementBorderDisplay.DisplayPosition.TOP_RIGHT);
	public LayoutEditorScreen editor;
	protected boolean selected = false;
	protected boolean multiSelected = false;
	protected boolean hovered = false;
	protected boolean leftMouseDown = false;
	protected double leftMouseDownMouseX = 0;
	protected double leftMouseDownMouseY = 0;
	protected int leftMouseDownBaseX = 0;
	protected int leftMouseDownBaseY = 0;
	protected int leftMouseDownBaseWidth = 0;
	protected int leftMouseDownBaseHeight = 0;
	protected int movingStartPosX = 0;
	protected int movingStartPosY = 0;
	protected int resizingStartPosX = 0;
	protected int resizingStartPosY = 0;
	protected ResizeGrabber[] resizeGrabbers = new ResizeGrabber[]{new ResizeGrabber(ResizeGrabberType.TOP), new ResizeGrabber(ResizeGrabberType.RIGHT), new ResizeGrabber(ResizeGrabberType.BOTTOM), new ResizeGrabber(ResizeGrabberType.LEFT)};
	protected ResizeGrabber activeResizeGrabber = null;
	protected AspectRatio resizeAspectRatio = new AspectRatio(10, 10);
	public long renderMovingNotAllowedTime = -1;
	public boolean recentlyMovedByDragging = false;
	public boolean recentlyLeftClickSelected = false;
	public boolean recentlyResized = false;
	public boolean movingCrumpleZonePassed = false;

	private final List<AbstractEditorElement> cachedHoveredElementsOnRightClickMenuOpen = new ArrayList<>();

	public AbstractEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor, @Nullable EditorElementSettings settings) {
		this.settings = (settings != null) ? settings : new EditorElementSettings();
		this.settings.editorElement = this;
		this.editor = editor;
		this.element = element;
		this.rightClickMenu = new ContextMenu() {
			@Override
			public ContextMenu openMenuAt(float x, float y, @Nullable List<String> entryPath) {
				cachedHoveredElementsOnRightClickMenuOpen.clear();
				cachedHoveredElementsOnRightClickMenuOpen.addAll(editor.getHoveredElements());
				return super.openMenuAt(x, y, entryPath);
			}
		};
		this.init();
	}

	public AbstractEditorElement(@Nonnull AbstractElement element, @Nonnull LayoutEditorScreen editor) {
		this(element, editor, new EditorElementSettings());
	}

	public void init() {

		this.rightClickMenu.closeMenu();
		this.rightClickMenu.clearEntries();
		this.topLeftDisplay.clearLines();
		this.bottomRightDisplay.clearLines();

		this.topLeftDisplay.addLine("anchor_point", () -> Component.translatable("fancymenu.element.border_display.anchor_point", this.element.anchorPoint.getDisplayName()));
		this.topLeftDisplay.addLine("pos_x", () -> Component.translatable("fancymenu.element.border_display.pos_x", "" + this.getX()));
		this.topLeftDisplay.addLine("width", () -> Component.translatable("fancymenu.element.border_display.width", "" + this.getWidth()));
		if (this.element.builder.isDeprecated()) {
			this.topLeftDisplay.addLine("deprecated_warning_line0", Component::empty);
			this.topLeftDisplay.addLine("deprecated_warning_line1", () -> Component.translatable("fancymenu.editor.elements.deprecated_warning.line1").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt())));
			this.topLeftDisplay.addLine("deprecated_warning_line2", () -> Component.translatable("fancymenu.editor.elements.deprecated_warning.line2").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt())));
			this.topLeftDisplay.addLine("deprecated_warning_line3", () -> Component.translatable("fancymenu.editor.elements.deprecated_warning.line3").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt())));
		}

		this.bottomRightDisplay.addLine("pos_y", () -> Component.translatable("fancymenu.element.border_display.pos_y", "" + this.getY()));
		this.bottomRightDisplay.addLine("height", () -> Component.translatable("fancymenu.element.border_display.height", "" + this.getHeight()));

		ContextMenu pickElementMenu = new ContextMenu() {
			@Override
			public @NotNull ContextMenu openMenuAt(float x, float y) {
				this.clearEntries();
				int i = 0;
				for (AbstractEditorElement e : cachedHoveredElementsOnRightClickMenuOpen) {
					this.addClickableEntry("element_" + i, e.element.getDisplayName(), (menu, entry) -> {
						editor.getAllElements().forEach(AbstractEditorElement::resetElementStates);
						e.setSelected(true);
					});
					i++;
				}
				return super.openMenuAt(x, y);
			}
		};
		this.rightClickMenu.addSubMenuEntry("pick_element", Component.translatable("fancymenu.element.general.pick_element"), pickElementMenu)
				.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.general.pick_element.desc")));

		this.rightClickMenu.addSeparatorEntry("separator_1");

		if (this.settings.isIdentifierCopyable()) {

			this.rightClickMenu.addClickableEntry("copy_id", Component.translatable("fancymenu.helper.editor.items.copyid"), (menu, entry) -> {
				Minecraft.getInstance().keyboardHandler.setClipboard(this.element.getInstanceIdentifier());
				menu.closeMenu();
			}).setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.copyid.btn.desc")))
					.setIcon(ContextMenu.IconFactory.getIcon("notes"));

		}

		this.rightClickMenu.addSeparatorEntry("separator_2");

		this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_in_editor_display_name", AbstractEditorElement.class,
						consumes -> consumes.element.customElementLayerName,
						(abstractEditorElement, s) -> abstractEditorElement.element.customElementLayerName = s,
						null, false, false, Component.translatable("fancymenu.elements.in_editor_display_name"), true, null, null, null)
				.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.in_editor_display_name.desc")));

		if (this.settings.isInEditorColorSupported()) {

			this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_in_editor_color", AbstractEditorElement.class,
							consumes -> consumes.element.inEditorColor.getHex(),
							(element, s) -> element.element.inEditorColor = DrawableColor.of(s),
							null, false, false, Component.translatable("fancymenu.editor.elements.in_editor_color"),
							false, null, TextValidators.HEX_COLOR_TEXT_VALIDATOR, null)
					.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.elements.in_editor_color.desc")));

		}

		this.rightClickMenu.addSeparatorEntry("separator_after_set_in_editor_stuff");

		if (this.settings.isAnchorPointChangeable()) {

			ContextMenu anchorPointMenu = new ContextMenu();
			this.rightClickMenu.addSubMenuEntry("anchor_point", Component.translatable("fancymenu.editor.items.setorientation"), anchorPointMenu)
					.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.items.orientation.btndesc")))
					.setStackable(true)
					.setIcon(ContextMenu.IconFactory.getIcon("anchor"));

			if (this.settings.isElementAnchorPointAllowed()) {

				anchorPointMenu.addClickableEntry("anchor_point_element", ElementAnchorPoints.ELEMENT.getDisplayName(),
								(menu, entry) -> {
									if (entry.getStackMeta().isFirstInStack()) {
										TextInputScreen s = new TextInputScreen(Component.translatable("fancymenu.helper.editor.items.orientation.element.setidentifier"), null, call -> {
											if (call != null) {
												AbstractEditorElement editorElement = this.editor.getElementByInstanceIdentifier(call);
												if (editorElement != null) {
													this.editor.history.saveSnapshot();
													for (AbstractEditorElement e : this.editor.getSelectedElements()) {
														if (e.settings.isAnchorPointChangeable() && e.settings.isElementAnchorPointAllowed()) {
															e.element.setAnchorPointElementIdentifier(editorElement.element.getInstanceIdentifier());
															e.element.setElementAnchorPointParent(editorElement.element);
															e.setAnchorPoint(ElementAnchorPoints.ELEMENT, true);
														}
													}
													Minecraft.getInstance().setScreen(this.editor);
												} else {
													Minecraft.getInstance().setScreen(NotificationScreen.error(b -> {
														Minecraft.getInstance().setScreen(this.editor);
													}, LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.orientation.element.setidentifier.identifiernotfound")));
												}
											} else {
												Minecraft.getInstance().setScreen(this.editor);
											}
										});
										if (!entry.getStackMeta().isPartOfStack()) {
											s.setText(this.element.getAnchorPointElementIdentifier());
										}
										Minecraft.getInstance().setScreen(s);
										menu.closeMenu();
									}
								})
						.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.orientation.element.btn.desc")))
						.setStackable(true);

			}

			anchorPointMenu.addSeparatorEntry("separator_1").setStackable(true);

			for (ElementAnchorPoint p : ElementAnchorPoints.getAnchorPoints()) {
				if ((p != ElementAnchorPoints.ELEMENT) && (this.settings.isVanillaAnchorPointAllowed() || (p != ElementAnchorPoints.VANILLA))) {
					anchorPointMenu.addClickableEntry("anchor_point_" + p.getName().replace("-", "_"), p.getDisplayName(), (menu, entry) -> {
						if (entry.getStackMeta().isFirstInStack()) {
							this.editor.history.saveSnapshot();
							for (AbstractEditorElement e : this.editor.getSelectedElements()) {
								if (e.settings.isAnchorPointChangeable()) {
									e.setAnchorPoint(p, true);
								}
							}
							menu.closeMenu();
						}
					}).setStackable(true)
							.setIcon(ContextMenu.IconFactory.getIcon("anchor_" + p.getName().replace("-", "_")));
				}
			}

		}

		if (this.settings.isStayOnScreenAllowed()) {

			this.addToggleContextMenuEntryTo(this.rightClickMenu, "stay_on_screen", AbstractEditorElement.class,
							consumes -> consumes.element.stayOnScreen,
							(element1, aBoolean) -> element1.element.stayOnScreen = aBoolean,
							"fancymenu.elements.element.stay_on_screen")
					.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines(!this.element.stickyAnchor ? "fancymenu.elements.element.stay_on_screen.tooltip" : "fancymenu.elements.element.stay_on_screen.tooltip.disable_sticky")))
					.setIcon(ContextMenu.IconFactory.getIcon("screen"))
					.setStackable(false)
					.addIsActiveSupplier((menu, entry) -> !this.element.stickyAnchor);

		}

		if (this.settings.isAdvancedPositioningSupported()) {

			ContextMenu advancedPositioningMenu = new ContextMenu();
			this.rightClickMenu.addSubMenuEntry("advanced_positioning", Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning"), advancedPositioningMenu)
					.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.features.advanced_positioning.desc")))
					.setStackable(true)
					.setIcon(ContextMenu.IconFactory.getIcon("move"));

			this.addGenericStringInputContextMenuEntryTo(advancedPositioningMenu, "advanced_positioning_x",
							element -> element.settings.isAdvancedPositioningSupported(),
							consumes -> consumes.element.advancedX,
							(element, input) -> element.element.advancedX = input,
							null, false, true, Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning.posx"),
							true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
					.setStackable(true);

			this.addGenericStringInputContextMenuEntryTo(advancedPositioningMenu, "advanced_positioning_y",
							element -> element.settings.isAdvancedPositioningSupported(),
							consumes -> consumes.element.advancedY,
							(element, input) -> element.element.advancedY = input,
							null, false, true, Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning.posy"),
							true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
					.setStackable(true);

		}

		if (this.settings.isAdvancedSizingSupported()) {

			ContextMenu advancedSizingMenu = new ContextMenu();
			this.rightClickMenu.addSubMenuEntry("advanced_sizing", Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing"), advancedSizingMenu)
					.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.features.advanced_sizing.desc")))
					.setStackable(true)
					.setIcon(ContextMenu.IconFactory.getIcon("resize"));

			this.addGenericStringInputContextMenuEntryTo(advancedSizingMenu, "advanced_sizing_width",
							element -> element.settings.isAdvancedSizingSupported(),
							consumes -> consumes.element.advancedWidth,
							(element, input) -> {
								element.element.advancedWidth = input;
								element.element.baseWidth = 50;
							}, null, false, true, Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing.width"),
							true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
					.setStackable(true);

			this.addGenericStringInputContextMenuEntryTo(advancedSizingMenu, "advanced_sizing_height",
							element -> element.settings.isAdvancedSizingSupported(),
							consumes -> consumes.element.advancedHeight, (element, input) -> {
								element.element.advancedHeight = input;
								element.element.baseHeight = 50;
							}, null, false, true, Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing.height"),
							true, null, TextValidators.NO_EMPTY_STRING_TEXT_VALIDATOR, null)
					.setStackable(true);

		}

		this.rightClickMenu.addSeparatorEntry("separator_after_advanced_sizing_positioning").setStackable(true);

		if (this.settings.isStretchable()) {

			this.addToggleContextMenuEntryTo(this.rightClickMenu, "stretch_x",
							AbstractEditorElement.class,
							consumes -> consumes.element.stretchX,
							(element1, aBoolean) -> element1.element.stretchX = aBoolean,
							"fancymenu.editor.elements.stretch.x")
					.setStackable(true)
					.setIsActiveSupplier((menu, entry) -> element.advancedWidth == null)
					.setIcon(ContextMenu.IconFactory.getIcon("arrow_horizontal"));

			this.addToggleContextMenuEntryTo(this.rightClickMenu, "stretch_y",
							AbstractEditorElement.class,
							consumes -> consumes.element.stretchY,
							(element1, aBoolean) -> element1.element.stretchY = aBoolean,
							"fancymenu.editor.elements.stretch.y")
					.setStackable(true)
					.setIsActiveSupplier((menu, entry) -> element.advancedHeight == null)
					.setIcon(ContextMenu.IconFactory.getIcon("arrow_vertical"));

		}

		this.rightClickMenu.addSeparatorEntry("separator_after_stretch_xy").setStackable(true);

		if (this.settings.isLoadingRequirementsEnabled()) {

			this.rightClickMenu.addClickableEntry("loading_requirements", Component.translatable("fancymenu.editor.loading_requirement.elements.loading_requirements"), (menu, entry) ->
					{
						if (!entry.getStackMeta().isPartOfStack()) {
							ManageRequirementsScreen s = new ManageRequirementsScreen(this.element.loadingRequirementContainer.copy(false), (call) -> {
								if (call != null) {
									this.editor.history.saveSnapshot();
									this.element.loadingRequirementContainer = call;
								}
								Minecraft.getInstance().setScreen(this.editor);
							});
							Minecraft.getInstance().setScreen(s);
						} else if (entry.getStackMeta().isFirstInStack()) {
							List<AbstractEditorElement> selectedElements = this.getFilteredSelectedElementList(element -> element.settings.isLoadingRequirementsEnabled());
							List<LoadingRequirementContainer> containers = ObjectUtils.getOfAll(LoadingRequirementContainer.class, selectedElements, consumes -> consumes.element.loadingRequirementContainer);
							LoadingRequirementContainer containerToUseInManager = new LoadingRequirementContainer();
							boolean allEqual = ListUtils.allInListEqual(containers);
							if (allEqual) {
								containerToUseInManager = containers.get(0).copy(true);
							}
							ManageRequirementsScreen s = new ManageRequirementsScreen(containerToUseInManager, (call) -> {
								if (call != null) {
									this.editor.history.saveSnapshot();
									for (AbstractEditorElement e : selectedElements) {
										e.element.loadingRequirementContainer = call.copy(true);
									}
								}
								Minecraft.getInstance().setScreen(this.editor);
							});
							if (allEqual) {
								Minecraft.getInstance().setScreen(s);
							} else {
								Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings((call) -> {
									if (call) {
										Minecraft.getInstance().setScreen(s);
									} else {
										Minecraft.getInstance().setScreen(this.editor);
									}
								}, LocalizationUtils.splitLocalizedStringLines("fancymenu.elements.multiselect.loading_requirements.warning.override")));
							}
						}
					})
					.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.loading_requirement.elements.loading_requirements.desc")))
					.setStackable(true)
					.setIcon(ContextMenu.IconFactory.getIcon("check_list"));

		}

		this.addToggleContextMenuEntryTo(this.rightClickMenu, "load_once_per_session", AbstractEditorElement.class,
						consumes -> consumes.element.loadOncePerSession,
						(element1, aBoolean) -> element1.element.loadOncePerSession = aBoolean,
						"fancymenu.elements.element.load_once_per_session")
				.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.element.load_once_per_session.desc")))
				.setStackable(true);

		this.rightClickMenu.addSeparatorEntry("separator_5");

		if (this.settings.isOrderable()) {

			this.rightClickMenu.addClickableEntry("move_up_element", Component.translatable("fancymenu.editor.object.moveup"),
							(menu, entry) -> {
								this.editor.moveLayerUp(this);
								this.editor.layoutEditorWidgets.forEach(widget -> widget.editorElementOrderChanged(this, true));
							})
					.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.object.moveup.desc")))
					.setIsActiveSupplier((menu, entry) -> this.editor.canMoveLayerUp(this))
					.setIcon(ContextMenu.IconFactory.getIcon("arrow_up"));

			this.rightClickMenu.addClickableEntry("move_down_element", Component.translatable("fancymenu.editor.object.movedown"),
							(menu, entry) -> {
								this.editor.moveLayerDown(this);
								this.editor.layoutEditorWidgets.forEach(widget -> widget.editorElementOrderChanged(this, false));
							})
					.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.object.movedown.desc")))
					.setIsActiveSupplier((menu, entry) -> this.editor.canMoveLayerDown(this))
					.setIcon(ContextMenu.IconFactory.getIcon("arrow_down"));

		}

		this.rightClickMenu.addSeparatorEntry("separator_6").setStackable(true);

		if (this.settings.isCopyable()) {

			this.rightClickMenu.addClickableEntry("copy_element", Component.translatable("fancymenu.editor.edit.copy"), (menu, entry) ->
					{
						if (!entry.getStackMeta().isPartOfStack()) {
							this.editor.copyElementsToClipboard(this);
						} else {
							this.editor.copyElementsToClipboard(this.editor.getSelectedElements().toArray(new AbstractEditorElement[0]));
						}
						menu.closeMenu();
					})
					.setStackable(true)
					.setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.copy"))
					.setIcon(ContextMenu.IconFactory.getIcon("copy"));

		}

		if (this.settings.isDestroyable()) {

			this.rightClickMenu.addClickableEntry("delete_element", Component.translatable("fancymenu.editor.items.delete"), (menu, entry) ->
					{
						this.editor.history.saveSnapshot();
						for (AbstractEditorElement e : this.editor.getSelectedElements()) {
							e.deleteElement();
						}
						menu.closeMenu();
					})
					.setStackable(true)
					.setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.delete"))
					.setIcon(ContextMenu.IconFactory.getIcon("delete"));

		}

		this.rightClickMenu.addSeparatorEntry("separator_7").setStackable(true);

		if (this.settings.isDelayable()) {

			ContextMenu appearanceDelayMenu = new ContextMenu();
			this.rightClickMenu.addSubMenuEntry("appearance_delay", Component.translatable("fancymenu.element.general.appearance_delay"), appearanceDelayMenu)
					.setStackable(true)
					.setIcon(ContextMenu.IconFactory.getIcon("timer"));

			this.addGenericCycleContextMenuEntryTo(appearanceDelayMenu, "appearance_delay_type",
							ListUtils.of(AbstractElement.AppearanceDelay.NO_DELAY, AbstractElement.AppearanceDelay.FIRST_TIME, AbstractElement.AppearanceDelay.EVERY_TIME),
							consumes -> consumes.settings.isDelayable(),
							consumes -> consumes.element.appearanceDelay,
							(element, switcherValue) -> element.element.appearanceDelay = switcherValue,
							(menu, entry, switcherValue) -> {
								return Component.translatable("fancymenu.element.general.appearance_delay." + switcherValue.name);
							})
					.setStackable(true);

			Supplier<Boolean> appearanceDelayIsActive = () -> {
				List<AbstractEditorElement> selected = this.editor.getSelectedElements();
				selected.removeIf(e -> !e.settings.isDelayable());
				if (selected.size() > 1) return true;
				for (AbstractEditorElement e : selected) {
					if (e.element.appearanceDelay == AbstractElement.AppearanceDelay.NO_DELAY) return false;
				}
				return true;
			};

			this.addGenericFloatInputContextMenuEntryTo(appearanceDelayMenu, "appearance_delay_seconds",
							element -> element.settings.isDelayable(),
							element -> element.element.appearanceDelayInSeconds,
							(element, input) -> element.element.appearanceDelayInSeconds = input,
							Component.translatable("fancymenu.element.general.appearance_delay.seconds"),
							true, 1.0F, null, null)
					.setIsActiveSupplier((menu, entry) -> appearanceDelayIsActive.get())
					.setStackable(true);

		}

		if (this.settings.isFadeable()) {

			ContextMenu fadingMenu = new ContextMenu();
			this.rightClickMenu.addSubMenuEntry("fading_in_out", Component.translatable("fancymenu.element.fading"), fadingMenu)
					.setStackable(true);

			this.addGenericCycleContextMenuEntryTo(fadingMenu, "fade_in",
					List.of(AbstractElement.Fading.NO_FADING, AbstractElement.Fading.FIRST_TIME, AbstractElement.Fading.EVERY_TIME),
					consumes -> consumes.settings.isFadeable(),
					consumes -> consumes.element.fadeIn,
					(abstractEditorElement, fading) -> abstractEditorElement.element.fadeIn = fading,
					(menu, entry, switcherValue) -> {
						if (switcherValue == AbstractElement.Fading.FIRST_TIME) {
							return Component.translatable("fancymenu.element.fading.fade_in", Component.translatable("fancymenu.element.fading.values.first_time").setStyle(LocalizedCycleEnum.WARNING_TEXT_STYLE.get()));
						}
						if (switcherValue == AbstractElement.Fading.EVERY_TIME) {
							return Component.translatable("fancymenu.element.fading.fade_in", Component.translatable("fancymenu.element.fading.values.every_time").setStyle(LocalizedCycleEnum.WARNING_TEXT_STYLE.get()));
						}
						return Component.translatable("fancymenu.element.fading.fade_in", Component.translatable("fancymenu.element.fading.values.no_fading").setStyle(LocalizedCycleEnum.WARNING_TEXT_STYLE.get()));
					}
			).setStackable(true);

			this.addGenericFloatInputContextMenuEntryTo(fadingMenu, "fade_in_speed",
					consumes -> consumes.settings.isFadeable(),
					consumes -> consumes.element.fadeInSpeed,
					(abstractEditorElement, aFloat) -> abstractEditorElement.element.fadeInSpeed = aFloat,
					Component.translatable("fancymenu.element.fading.fade_in.speed"), true, 1.0F,
					consumes -> {
						if (de.keksuccino.fancymenu.util.MathUtils.isFloat(consumes)) {
							float f = Float.parseFloat(consumes);
							return (f > 0.0F);
						}
						return false;
					},
					consumes -> {
						if (de.keksuccino.fancymenu.util.MathUtils.isFloat(consumes)) {
							float f = Float.parseFloat(consumes);
							if (f <= 0.0F) return null;
						}
						return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.fading.error.negative_value"));
					}).setStackable(true);

			fadingMenu.addSeparatorEntry("separator_after_fade_in_speed").setStackable(true);

			this.addGenericCycleContextMenuEntryTo(fadingMenu, "fade_out",
							List.of(AbstractElement.Fading.NO_FADING, AbstractElement.Fading.FIRST_TIME, AbstractElement.Fading.EVERY_TIME),
							consumes -> consumes.settings.isFadeable(),
							consumes -> consumes.element.fadeOut,
							(abstractEditorElement, fading) -> abstractEditorElement.element.fadeOut = fading,
							(menu, entry, switcherValue) -> {
								if (switcherValue == AbstractElement.Fading.FIRST_TIME) {
									return Component.translatable("fancymenu.element.fading.fade_out", Component.translatable("fancymenu.element.fading.values.first_time").setStyle(LocalizedCycleEnum.WARNING_TEXT_STYLE.get()));
								}
								if (switcherValue == AbstractElement.Fading.EVERY_TIME) {
									return Component.translatable("fancymenu.element.fading.fade_out", Component.translatable("fancymenu.element.fading.values.every_time").setStyle(LocalizedCycleEnum.WARNING_TEXT_STYLE.get()));
								}
								return Component.translatable("fancymenu.element.fading.fade_out", Component.translatable("fancymenu.element.fading.values.no_fading").setStyle(LocalizedCycleEnum.WARNING_TEXT_STYLE.get()));
							}
					).setStackable(true)
					.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.fading.fade_out.desc")));

			this.addGenericFloatInputContextMenuEntryTo(fadingMenu, "fade_out_speed",
					consumes -> consumes.settings.isFadeable(),
					consumes -> consumes.element.fadeOutSpeed,
					(abstractEditorElement, aFloat) -> abstractEditorElement.element.fadeOutSpeed = aFloat,
					Component.translatable("fancymenu.element.fading.fade_out.speed"), true, 1.0F,
					consumes -> {
						if (de.keksuccino.fancymenu.util.MathUtils.isFloat(consumes)) {
							float f = Float.parseFloat(consumes);
							return (f > 0.0F);
						}
						return false;
					},
					consumes -> {
						if (de.keksuccino.fancymenu.util.MathUtils.isFloat(consumes)) {
							float f = Float.parseFloat(consumes);
							if (f <= 0.0F) return null;
						}
						return Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.fading.error.negative_value"));
					}).setStackable(true);

		}

		if (this.settings.isOpacityChangeable()) {

			this.addGenericStringInputContextMenuEntryTo(this.rightClickMenu, "base_opacity",
							consumes -> consumes.settings.isOpacityChangeable(),
							consumes -> consumes.element.baseOpacity,
							(abstractEditorElement, s) -> abstractEditorElement.element.baseOpacity = s,
							null, false, true, Component.translatable("fancymenu.element.base_opacity"),
							true, "1.0", null, null)
					.setStackable(true)
					.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.base_opacity.desc")));

		}

		if (this.settings.isAutoSizingAllowed()) {

			this.addToggleContextMenuEntryTo(this.rightClickMenu, "auto_sizing", AbstractEditorElement.class,
							consumes -> consumes.element.autoSizing,
							(abstractEditorElement, aBoolean) -> {
								abstractEditorElement.element.setAutoSizingBaseWidthAndHeight();
								abstractEditorElement.element.autoSizing = aBoolean;
								abstractEditorElement.element.updateAutoSizing(true);
							},
							"fancymenu.element.auto_sizing")
					.setStackable(true)
					.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.auto_sizing.desc")))
					.setIcon(ContextMenu.IconFactory.getIcon("measure"));

		}

		if (this.settings.isStickyAnchorAllowed()) {

			this.addToggleContextMenuEntryTo(this.rightClickMenu, "sticky_anchor", AbstractEditorElement.class,
							consumes -> consumes.element.stickyAnchor,
							(abstractEditorElement, aBoolean) -> {

				                int oldPosX = abstractEditorElement.element.getAbsoluteX();
								int oldPosY = abstractEditorElement.element.getAbsoluteY();

								abstractEditorElement.element.stickyAnchor = aBoolean;

								int newPosX = abstractEditorElement.element.getAbsoluteX();
								int newPosY = abstractEditorElement.element.getAbsoluteY();

								abstractEditorElement.element.posOffsetX += oldPosX - newPosX;
								abstractEditorElement.element.posOffsetY += oldPosY - newPosY;

							},
							"fancymenu.element.sticky_anchor")
					.setStackable(false)
					.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines(!this.element.stayOnScreen ? "fancymenu.element.sticky_anchor.desc" : "fancymenu.element.sticky_anchor.desc.disable_stay_on_screen")))
					.setIcon(ContextMenu.IconFactory.getIcon("anchor"))
					.addIsActiveSupplier((menu, entry) -> !this.element.stayOnScreen);

		}

		if (this.settings.isParallaxAllowed()) {

			this.rightClickMenu.addSeparatorEntry("separator_before_parallax").setStackable(true);

			this.addToggleContextMenuEntryTo(this.rightClickMenu, "enable_parallax", AbstractEditorElement.class,
							consumes -> consumes.element.enableParallax,
							(abstractEditorElement, aBoolean) -> abstractEditorElement.element.enableParallax = aBoolean,
							"fancymenu.elements.parallax")
					.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.parallax.desc")));

			this.addStringInputContextMenuEntryTo(this.rightClickMenu, "parallax_intensity", AbstractEditorElement.class,
							consumes -> consumes.element.parallaxIntensityString,
							(element1, s) -> element1.element.parallaxIntensityString = s,
							null, false, true, Component.translatable("fancymenu.elements.parallax.intensity"),
							true, "0.5", null, null)
					.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.parallax.intensity.desc")));

			this.addToggleContextMenuEntryTo(this.rightClickMenu, "invert_parallax", AbstractEditorElement.class,
							consumes -> consumes.element.invertParallax,
							(abstractEditorElement, aBoolean) -> abstractEditorElement.element.invertParallax = aBoolean,
							"fancymenu.elements.parallax.invert")
					.setTooltipSupplier((menu, entry) -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.parallax.invert.desc")));

		}

		this.rightClickMenu.addSeparatorEntry("separator_8").setStackable(true);

	}

	@Override
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

		if (this.element.layerHiddenInEditor) {
			if (this.rightClickMenu.isOpen()) {
				this.rightClickMenu.closeMenu();
			}
			return;
		}

		this.tick();

		this.hovered = this.isMouseOver(mouseX, mouseY);

		this.element.renderInternal(graphics, mouseX, mouseY, partial);

		this.renderDraggingNotAllowedOverlay(graphics);

		this.renderDeprecatedIndicator(graphics);

		//Update cursor
		ResizeGrabber hoveredGrabber = this.getHoveredResizeGrabber();
		if (hoveredGrabber != null) CursorHandler.setClientTickCursor(hoveredGrabber.getCursor());

		this.renderBorder(graphics, mouseX, mouseY, partial);

	}

	protected void tick() {
		if ((this.element.advancedWidth != null) || (this.element.advancedHeight != null) && !this.topLeftDisplay.hasLine("advanced_sizing_enabled")) {
			this.topLeftDisplay.addLine("advanced_sizing_enabled", () -> Component.translatable("fancymenu.elements.advanced_sizing.enabled_notification"));
		}
		if ((this.element.advancedWidth == null) && (this.element.advancedHeight == null) && this.topLeftDisplay.hasLine("advanced_sizing_enabled")) {
			this.topLeftDisplay.removeLine("advanced_sizing_enabled");
		}
		if ((this.element.advancedX != null) || (this.element.advancedY != null) && !this.topLeftDisplay.hasLine("advanced_positioning_enabled")) {
			this.topLeftDisplay.addLine("advanced_positioning_enabled", () -> Component.translatable("fancymenu.elements.advanced_positioning.enabled_notification"));
		}
		if ((this.element.advancedX == null) && (this.element.advancedY == null) && this.topLeftDisplay.hasLine("advanced_positioning_enabled")) {
			this.topLeftDisplay.removeLine("advanced_positioning_enabled");
		}
	}

	protected void renderDraggingNotAllowedOverlay(GuiGraphics graphics) {
		if (this.renderMovingNotAllowedTime >= System.currentTimeMillis()) {
			RenderSystem.enableBlend();
			graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), UIBase.getUIColorTheme().layout_editor_element_dragging_not_allowed_color.getColorInt());
			AspectRatio ratio = new AspectRatio(32, 32);
			int[] size = ratio.getAspectRatioSizeByMaximumSize(this.getWidth(), this.getHeight());
			int texW = size[0];
			int texH = size[1];
			int texX = this.getX() + (this.getWidth() / 2) - (texW / 2);
			int texY = this.getY() + (this.getHeight() / 2) - (texH / 2);
			graphics.blit(DRAGGING_NOT_ALLOWED_TEXTURE, texX, texY, 0.0F, 0.0F, texW, texH, texW, texH);
		}
	}

	protected void renderDeprecatedIndicator(GuiGraphics graphics) {
		if (this.element.builder.isDeprecated()) {
			RenderSystem.enableBlend();
			AspectRatio ratio = new AspectRatio(32, 32);
			int[] size = ratio.getAspectRatioSizeByMaximumSize(this.getWidth() / 3, this.getHeight() / 3);
			int texW = size[0];
			int texH = size[1];
			int texX = this.getX() + this.getWidth() - texW;
			int texY = this.getY();
			UIBase.setShaderColor(graphics, UIBase.getUIColorTheme().warning_text_color);
			graphics.blit(DEPRECATED_WARNING_TEXTURE, texX, texY, 0.0F, 0.0F, texW, texH, texW, texH);
			RenderingUtils.resetShaderColor(graphics);
		}
	}

	protected void renderBorder(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

		if (((this.editor.getTopHoveredElement() == this) && !this.editor.isUserNavigatingInRightClickMenu() && !this.editor.isUserNavigatingInElementMenu()) || this.isSelected() || this.isMultiSelected()) {

			//TOP
			graphics.fill(this.getX() + 1, this.getY(), this.getX() + this.getWidth() - 1, this.getY() + 1, BORDER_COLOR.get(this));
			//BOTTOM
			graphics.fill(this.getX() + 1, this.getY() + this.getHeight() - 1, this.getX() + this.getWidth() - 1, this.getY() + this.getHeight(), BORDER_COLOR.get(this));
			//LEFT
			graphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.getHeight(), BORDER_COLOR.get(this));
			//RIGHT
			graphics.fill(this.getX() + this.getWidth() - 1, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), BORDER_COLOR.get(this));

			for (ResizeGrabber g : this.resizeGrabbers) {
				g.render(graphics, mouseX, mouseY, partial);
			}

		}

		if (this.isSelected()) {
			this.topLeftDisplay.render(graphics, mouseX, mouseY, partial);
			this.bottomRightDisplay.render(graphics, mouseX, mouseY, partial);
		}

	}

	/**
	 * Sets the {@link ElementAnchorPoint} of the element.<br>
	 * It is important to set {@link AbstractElement#setAnchorPointElementIdentifier(String)} first before calling
	 * this method, in case {@code newAnchor} is {@link ElementAnchorPoints#ELEMENT}.
	 */
	public void setAnchorPoint(ElementAnchorPoint newAnchor, boolean resetElementStates) {

		// Capture the element’s actual (absolute) position before changing the anchor.
		int oldAbsX = this.element.getAbsoluteX();
		int oldAbsY = this.element.getAbsoluteY();

		this.setAnchorPoint(newAnchor, oldAbsX, oldAbsY, resetElementStates);

	}

	/**
	 * Sets the {@link ElementAnchorPoint} of the element.<br>
	 * It is important to set {@link AbstractElement#setAnchorPointElementIdentifier(String)} first before calling
	 * this method, in case {@code newAnchor} is {@link ElementAnchorPoints#ELEMENT}.
	 */
	public void setAnchorPoint(ElementAnchorPoint newAnchor, int oldAbsX, int oldAbsY, boolean resetElementStates) {

		if (!this.settings.isAnchorPointChangeable()) return;
		if ((newAnchor == ElementAnchorPoints.ELEMENT) && !this.settings.isElementAnchorPointAllowed()) return;
		if ((newAnchor == ElementAnchorPoints.ELEMENT) && (this.element.getAnchorPointElementIdentifier() == null)) {
			LOGGER.error("[FANCYMENU] Failed to set element's anchor to anchor point type 'ELEMENT'! Identifier was NULL!", new NullPointerException());
			return;
		}

		boolean stayOnScreen = this.element.stayOnScreen;
		this.element.stayOnScreen = false;

		if (resetElementStates) {
			this.resetElementStates();
		}
		if (newAnchor == null) {
			newAnchor = ElementAnchorPoints.MID_CENTERED;
		}

		if (newAnchor == ElementAnchorPoints.ELEMENT) {
			AbstractEditorElement ee = this.editor.getElementByInstanceIdentifier(Objects.requireNonNull(this.element.getAnchorPointElementIdentifier()));
			if (ee != null) {
				this.element.setElementAnchorPointParent(ee.element);
			} else {
				this.element.setElementAnchorPointParent(null);
				LOGGER.error("[FANCYMENU] Failed to get parent element for 'ELEMENT' anchor type! Element was NULL!", new NullPointerException());
			}
		} else {
			this.element.setAnchorPointElementIdentifier(null);
			this.element.setElementAnchorPointParent(null);
		}

		// Update the anchor.
		this.element.anchorPoint = newAnchor;

		// Now get the new absolute position with the new anchor settings.
		int newAbsX = this.element.getAbsoluteX();
		int newAbsY = this.element.getAbsoluteY();

		// Adjust posOffset to counteract any shift—keeping the on-screen (absolute) position unchanged.
		this.element.posOffsetX += (oldAbsX - newAbsX);
		this.element.posOffsetY += (oldAbsY - newAbsY);

		this.element.stayOnScreen = stayOnScreen;

	}

	@ApiStatus.Internal
	public void setAnchorPointViaOverlay(AnchorPointOverlay.AnchorPointArea anchor, int mouseX, int mouseY) {
		if (!this.settings.isAnchorPointChangeable()) return;
		if ((anchor.anchorPoint == ElementAnchorPoints.ELEMENT) && !this.settings.isElementAnchorPointAllowed()) return;
		if (anchor instanceof AnchorPointOverlay.ElementAnchorPointArea ea) {
			this.element.setAnchorPointElementIdentifier(ea.elementIdentifier);
		}
		this.setAnchorPoint(anchor.anchorPoint, false);
		this.updateLeftMouseDownCachedValues(mouseX, mouseY);
		this.updateMovingStartPos(mouseX, mouseY);
	}

	public void resetElementStates() {
		this.selected = false;
		this.multiSelected = false;
		this.leftMouseDown = false;
		this.activeResizeGrabber = null;
		this.rightClickMenu.closeMenu();
	}

	public void onSettingsChanged() {
		this.resetElementStates();
		this.init();
	}

	public void updateLeftMouseDownCachedValues(int mouseX, int mouseY) {
		this.leftMouseDownMouseX = mouseX;
		this.leftMouseDownMouseY = mouseY;
		this.leftMouseDownBaseX = this.element.posOffsetX;
		this.leftMouseDownBaseY = this.element.posOffsetY;
		this.leftMouseDownBaseWidth = this.element.baseWidth;
		this.leftMouseDownBaseHeight = this.element.baseHeight;
	}

	public void updateMovingStartPos(int mouseX, int mouseY) {
		this.movingStartPosX = mouseX;
		this.movingStartPosY = mouseY;
	}

	public void updateResizingStartPos(int mouseX, int mouseY) {
		this.resizingStartPosX = mouseX;
		this.resizingStartPosY = mouseY;
	}

	/**
	 * If the element's anchor point is {@link ElementAnchorPoints#ELEMENT} and its parent is selected.<br>
	 * Returns FALSE if the element has no parent, the parent is not selected or the element's anchor is not {@link ElementAnchorPoints#ELEMENT}, otherwise returns TRUE.
	 */
	public boolean isElementAnchorAndParentIsSelected() {
		if (this.element.anchorPoint != ElementAnchorPoints.ELEMENT) return false;
		if (this.element.getAnchorPointElementIdentifier() == null) return false;
		AbstractEditorElement parent = this.editor.getElementByInstanceIdentifier(this.element.getAnchorPointElementIdentifier());
		if (parent == null) return false;
		return (parent.isSelected() || parent.isMultiSelected());
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {

		if (this.element.layerHiddenInEditor) return false;

		if (!this.isSelected()) {
			return false;
		}
		if (button == 0) {
			if (!this.rightClickMenu.isUserNavigatingInMenu()) {
				this.activeResizeGrabber = !this.isMultiSelected() ? this.getHoveredResizeGrabber() : null;
				if (this.isHovered() || (this.isMultiSelected() && !this.editor.getHoveredElements().isEmpty()) || this.isGettingResized()) {
					this.leftMouseDown = true;
					this.updateLeftMouseDownCachedValues((int) mouseX, (int) mouseY);
					this.resizeAspectRatio = new AspectRatio(this.getWidth(), this.getHeight());
					if (this.element.autoSizingWidth > 0) this.element.baseWidth = this.element.autoSizingWidth;
					if (this.element.autoSizingHeight > 0) this.element.baseHeight = this.element.autoSizingHeight;
					this.element.setAutoSizingBaseWidthAndHeight();
					this.element.updateAutoSizing(true);
					this.element.autoSizingWidth = 0;
					this.element.autoSizingHeight = 0;
				}
			}
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0) {
			this.leftMouseDown = false;
			this.activeResizeGrabber = null;
			this.element.updateAutoSizing(true);
			this.recentlyMovedByDragging = false;
			this.recentlyResized = false;
			this.movingCrumpleZonePassed = false;
		}
		return false;
	}

	/**
	 * @param mouseX The X coordinate of the mouse.
	 * @param mouseY The Y coordinate of the mouse.
	 * @param button The button that is being dragged.
	 * @param dragX The X distance of the drag (mouse move distance per tick; mostly values between 0.3 and 5).
	 * @param dragY The Y distance of the drag (mouse move distance per tick; mostly values between 0.3 and 5).
	 */
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {

		if (this.element.layerHiddenInEditor) return false;

		if (!this.isSelected()) {
			return false;
		}
		if (button == 0) {
			if (this.leftMouseDown && !this.isGettingResized() && this.movingCrumpleZonePassed) { // MOVE ELEMENT
				int diffX = (int)-(this.movingStartPosX - mouseX);
				int diffY = (int)-(this.movingStartPosY - mouseY);
				if (this.editor.allSelectedElementsMovable()) {
					if (!this.isMultiSelected() || !this.isElementAnchorAndParentIsSelected()) {
						// Calculate new positions
						int newOffsetX = this.leftMouseDownBaseX + diffX;
						int newOffsetY = this.leftMouseDownBaseY + diffY;
						
						// Check if stay on screen is enabled
						if (this.element.stayOnScreen && !this.element.stickyAnchor) {
							// Calculate what the absolute positions would be with the new offsets
							int oldPosOffsetX = this.element.posOffsetX;
							int oldPosOffsetY = this.element.posOffsetY;
							
							// Temporarily set new offsets to calculate absolute positions
							this.element.posOffsetX = newOffsetX;
							this.element.posOffsetY = newOffsetY;
							
							// Get the absolute positions (which include stay-on-screen clamping)
							int absoluteX = this.element.getAbsoluteX();
							int absoluteY = this.element.getAbsoluteY();
							
							// Restore old offsets
							this.element.posOffsetX = oldPosOffsetX;
							this.element.posOffsetY = oldPosOffsetY;
							
							// Check if X position would be clamped
							int leftEdge = AbstractElement.STAY_ON_SCREEN_EDGE_ZONE_SIZE;
							int rightEdge = AbstractElement.getScreenWidth() - AbstractElement.STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.element.getAbsoluteWidth();
							
							if (absoluteX <= leftEdge && diffX < 0) {
								// Element is at left edge and trying to move further left
								newOffsetX = this.element.posOffsetX;
							} else if (absoluteX >= rightEdge && diffX > 0) {
								// Element is at right edge and trying to move further right
								newOffsetX = this.element.posOffsetX;
							}
							
							// Check if Y position would be clamped
							int topEdge = AbstractElement.STAY_ON_SCREEN_EDGE_ZONE_SIZE;
							int bottomEdge = AbstractElement.getScreenHeight() - AbstractElement.STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.element.getAbsoluteHeight();
							
							if (absoluteY <= topEdge && diffY < 0) {
								// Element is at top edge and trying to move further up
								newOffsetY = this.element.posOffsetY;
							} else if (absoluteY >= bottomEdge && diffY > 0) {
								// Element is at bottom edge and trying to move further down
								newOffsetY = this.element.posOffsetY;
							}
						}
						
						// Apply the potentially adjusted offsets
						this.element.posOffsetX = newOffsetX;
						this.element.posOffsetY = newOffsetY;

						// Apply grid snapping if enabled
						if (FancyMenu.getOptions().showLayoutEditorGrid.getValue() && FancyMenu.getOptions().layoutEditorGridSnapping.getValue() && !this.isMultiSelected()) {
							// Get element edges
							int leftEdge = this.getX();
							int rightEdge = leftEdge + this.getWidth();
							int topEdge = this.getY();
							int bottomEdge = topEdge + this.getHeight();

							// Get GUI scale to account for the scale difference
							double guiScale = Minecraft.getInstance().getWindow().getGuiScale();

							// Get grid size
							int gridSize = FancyMenu.getOptions().layoutEditorGridSize.getValue();

							// Calculate magnetic snap threshold (smaller = more precise snapping)
							double snapThreshold = Math.max(2 * guiScale, gridSize / 5.0) * FancyMenu.getOptions().layoutEditorGridSnappingStrength.getValue();

							// Get screen centers at grid space
							double centerXInGridSpace = AbstractElement.getScreenWidth() * guiScale / 2.0;
							double centerYInGridSpace = AbstractElement.getScreenHeight() * guiScale / 2.0;

							// Find nearest grid lines for all four edges
							double[] edgesX = new double[] { leftEdge * guiScale, rightEdge * guiScale };
							double[] distancesToGridX = new double[2];
							int[] nearestGridLinesX = new int[2];

							for (int i = 0; i < 2; i++) {
								double edgeInGridSpace = edgesX[i];
								double offsetFromCenter = edgeInGridSpace - centerXInGridSpace;
								double modX = offsetFromCenter % gridSize;
								if (modX < 0) modX += gridSize; // Handle negative numbers

								double nearestLowerGridXInGridSpace = edgeInGridSpace - modX;
								double nearestUpperGridXInGridSpace = nearestLowerGridXInGridSpace + gridSize;

								// Calculate distances to both potential grid lines
								double distToLower = Math.abs(edgeInGridSpace - nearestLowerGridXInGridSpace);
								double distToUpper = Math.abs(nearestUpperGridXInGridSpace - edgeInGridSpace);

								// Choose the closest grid line
								if (distToLower <= distToUpper) {
									distancesToGridX[i] = distToLower;
									nearestGridLinesX[i] = (int)(nearestLowerGridXInGridSpace / guiScale);
								} else {
									distancesToGridX[i] = distToUpper;
									nearestGridLinesX[i] = (int)(nearestUpperGridXInGridSpace / guiScale);
								}
							}

							// Find the edge that's closest to a grid line (either left or right)
							int closestEdgeIndexX = (distancesToGridX[0] <= distancesToGridX[1]) ? 0 : 1;

							// If the closest edge is within threshold, snap to it
							if (distancesToGridX[closestEdgeIndexX] <= snapThreshold) {
								int edgePos = (closestEdgeIndexX == 0) ? leftEdge : rightEdge;
								int gridPos = nearestGridLinesX[closestEdgeIndexX];
								int xAdjustment = gridPos - edgePos;
								this.element.posOffsetX += xAdjustment;
							}

							// Repeat for Y direction with top and bottom edges
							double[] edgesY = new double[] { topEdge * guiScale, bottomEdge * guiScale };
							double[] distancesToGridY = new double[2];
							int[] nearestGridLinesY = new int[2];

							for (int i = 0; i < 2; i++) {
								double edgeInGridSpace = edgesY[i];
								double offsetFromCenter = edgeInGridSpace - centerYInGridSpace;
								double modY = offsetFromCenter % gridSize;
								if (modY < 0) modY += gridSize; // Handle negative numbers

								double nearestLowerGridYInGridSpace = edgeInGridSpace - modY;
								double nearestUpperGridYInGridSpace = nearestLowerGridYInGridSpace + gridSize;

								// Calculate distances to both potential grid lines
								double distToLower = Math.abs(edgeInGridSpace - nearestLowerGridYInGridSpace);
								double distToUpper = Math.abs(nearestUpperGridYInGridSpace - edgeInGridSpace);

								// Choose the closest grid line
								if (distToLower <= distToUpper) {
									distancesToGridY[i] = distToLower;
									nearestGridLinesY[i] = (int)(nearestLowerGridYInGridSpace / guiScale);
								} else {
									distancesToGridY[i] = distToUpper;
									nearestGridLinesY[i] = (int)(nearestUpperGridYInGridSpace / guiScale);
								}
							}

							// Find the edge that's closest to a grid line (either top or bottom)
							int closestEdgeIndexY = (distancesToGridY[0] <= distancesToGridY[1]) ? 0 : 1;

							// If the closest edge is within threshold, snap to it
							if (distancesToGridY[closestEdgeIndexY] <= snapThreshold) {
								int edgePos = (closestEdgeIndexY == 0) ? topEdge : bottomEdge;
								int gridPos = nearestGridLinesY[closestEdgeIndexY];
								int yAdjustment = gridPos - edgePos;
								this.element.posOffsetY += yAdjustment;
							}
						}
					}
					if ((diffX > 0) || (diffY > 0)) {
						this.recentlyMovedByDragging = true;
					}
				} else if (!this.settings.isMovable()) {
					this.renderMovingNotAllowedTime = System.currentTimeMillis() + 800;
				}
			}
			if (this.leftMouseDown && this.isGettingResized()) { // RESIZE ELEMENT
				int diffX = (int)-(this.resizingStartPosX - mouseX);
				int diffY = (int)-(this.resizingStartPosY - mouseY);
				if ((diffX > 0) || (diffY > 0)) this.recentlyResized = true;
				if ((this.activeResizeGrabber.type == ResizeGrabberType.LEFT) || (this.activeResizeGrabber.type == ResizeGrabberType.RIGHT)) {
					int newWidth = (this.activeResizeGrabber.type == ResizeGrabberType.LEFT) ? (this.leftMouseDownBaseWidth - diffX) : (this.leftMouseDownBaseWidth + diffX);
					if (newWidth >= 2) {
						this.element.autoSizingWidth = 0;
						this.element.autoSizingHeight = 0;
						int cachedOldOffsetX = this.element.posOffsetX;
						int cachedOldPosX = this.element.getAbsoluteX();
						int cachedOldWidth = this.element.getAbsoluteWidth();
						this.element.baseWidth = newWidth;
						this.element.posOffsetX = this.leftMouseDownBaseX + this.element.anchorPoint.getResizePositionOffsetX(this.element, diffX, this.activeResizeGrabber.type);
						if (this.element.stickyAnchor) {
							this.element.posOffsetX += this.element.anchorPoint.getStickyResizePositionCorrectionX(this.element, diffX, cachedOldOffsetX, this.element.posOffsetX, cachedOldPosX, this.element.getAbsoluteX(), cachedOldWidth, this.element.getAbsoluteWidth(), this.activeResizeGrabber.type);
						}
						if (Screen.hasShiftDown()) {
							this.element.baseHeight = this.resizeAspectRatio.getAspectRatioHeight(this.element.baseWidth);
						}
					}
				}
				if ((this.activeResizeGrabber.type == ResizeGrabberType.TOP) || (this.activeResizeGrabber.type == ResizeGrabberType.BOTTOM)) {
					int newHeight = (this.activeResizeGrabber.type == ResizeGrabberType.TOP) ? (this.leftMouseDownBaseHeight - diffY) : (this.leftMouseDownBaseHeight + diffY);
					if (newHeight >= 2) {
						this.element.autoSizingWidth = 0;
						this.element.autoSizingHeight = 0;
						int cachedOldOffsetY = this.element.posOffsetY;
						int cachedOldPosY = this.element.getAbsoluteY();
						int cachedOldHeight = this.element.getAbsoluteHeight();
						this.element.baseHeight = newHeight;
						this.element.posOffsetY = this.leftMouseDownBaseY + this.element.anchorPoint.getResizePositionOffsetY(this.element, diffY, this.activeResizeGrabber.type);
						if (this.element.stickyAnchor) {
							this.element.posOffsetY += this.element.anchorPoint.getStickyResizePositionCorrectionY(this.element, diffY, cachedOldOffsetY, this.element.posOffsetY, cachedOldPosY, this.element.getAbsoluteY(), cachedOldHeight, this.element.baseHeight, this.activeResizeGrabber.type);
						}
						if (Screen.hasShiftDown()) {
							this.element.baseWidth = this.resizeAspectRatio.getAspectRatioWidth(this.element.baseHeight);
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		if (this.element.layerHiddenInEditor) return false;
		return UIBase.isXYInArea((int) mouseX, (int) mouseY, this.getX(), this.getY(), this.getWidth(), this.getHeight());
	}

	@Override
	public void setFocused(boolean var1) {}

	@Override
	public boolean isFocused() {
		return false;
	}

	public boolean isSelected() {
		return this.selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
		if (!this.selected) {
			this.resetElementStates();
		}
		this.editor.onUpdateSelectedElements();
	}

	public boolean isMultiSelected() {
		return this.multiSelected;
	}

	public void setMultiSelected(boolean multiSelected) {
		this.multiSelected = multiSelected;
	}

	public boolean isHovered() {
		if (this.element.layerHiddenInEditor) return false;
		return this.hovered || this.rightClickMenu.isUserNavigatingInMenu() || (this.getHoveredResizeGrabber() != null);
	}

	public int getX() {
		return this.element.getAbsoluteX();
	}

	public int getY() {
		return this.element.getAbsoluteY();
	}

	public int getWidth() {
		return this.element.getAbsoluteWidth();
	}

	public int getHeight() {
		return this.element.getAbsoluteHeight();
	}

	public boolean deleteElement() {
		return this.editor.deleteElement(this);
	}

	public boolean isGettingResized() {
		if (!this.settings.isResizeable()) {
			return false;
		}
		return this.activeResizeGrabber != null;
	}

	public boolean isDragged() {
		return this.recentlyMovedByDragging;
	}

	public boolean isPressed() {
		return this.leftMouseDown;
	}

	@Nullable
	public ResizeGrabber getHoveredResizeGrabber() {
		if (!this.settings.isResizeable()) {
			return null;
		}
		if (this.activeResizeGrabber != null) {
			return this.activeResizeGrabber;
		}
		for (ResizeGrabber g : this.resizeGrabbers) {
			if (g.hovered) {
				return g;
			}
		}
		return null;
	}

	public class ResizeGrabber implements Renderable {

		protected int width = 4;
		protected int height = 4;
		protected final ResizeGrabberType type;
		protected boolean hovered = false;

		protected ResizeGrabber(ResizeGrabberType type) {
			this.type = type;
		}

		@Override
		public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
			this.hovered = AbstractEditorElement.this.isSelected() && this.isGrabberEnabled() && this.isMouseOver(mouseX, mouseY);
			if (AbstractEditorElement.this.isSelected() && this.isGrabberEnabled()) {
				graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, BORDER_COLOR.get(AbstractEditorElement.this));
			}
		}

		protected int getX() {
			int x = AbstractEditorElement.this.getX();
			if ((this.type == ResizeGrabberType.TOP) || (this.type == ResizeGrabberType.BOTTOM)) {
				x += (AbstractEditorElement.this.getWidth() / 2) - (this.width / 2);
			}
			if (this.type == ResizeGrabberType.RIGHT) {
				x += AbstractEditorElement.this.getWidth() - (this.width / 2);
			}
			if (this.type == ResizeGrabberType.LEFT) {
				x -= (this.width / 2);
			}
			return x;
		}

		protected int getY() {
			int y = AbstractEditorElement.this.getY();
			if (this.type == ResizeGrabberType.TOP) {
				y -= (this.height / 2);
			}
			if ((this.type == ResizeGrabberType.RIGHT) || (this.type == ResizeGrabberType.LEFT)) {
				y += (AbstractEditorElement.this.getHeight() / 2) - (this.height / 2);
			}
			if (this.type == ResizeGrabberType.BOTTOM) {
				y += AbstractEditorElement.this.getHeight() - (this.height / 2);
			}
			return y;
		}

		protected long getCursor() {
			if ((this.type == ResizeGrabberType.TOP) || (this.type == ResizeGrabberType.BOTTOM)) {
				return CursorHandler.CURSOR_RESIZE_VERTICAL;
			}
			return CursorHandler.CURSOR_RESIZE_HORIZONTAL;
		}

		protected boolean isGrabberEnabled() {
			if (AbstractEditorElement.this.isMultiSelected()) {
				return false;
			}
			if ((this.type == ResizeGrabberType.TOP) || (this.type == ResizeGrabberType.BOTTOM)) {
				if ((this.type == ResizeGrabberType.TOP) && (AbstractEditorElement.this.element.advancedY != null)) return false;
				return AbstractEditorElement.this.settings.isResizeable() && AbstractEditorElement.this.settings.isResizeableY() && (AbstractEditorElement.this.element.advancedHeight == null);
			}
			if ((this.type == ResizeGrabberType.LEFT) || (this.type == ResizeGrabberType.RIGHT)) {
				if ((this.type == ResizeGrabberType.LEFT) && (AbstractEditorElement.this.element.advancedX != null)) return false;
				return AbstractEditorElement.this.settings.isResizeable() && AbstractEditorElement.this.settings.isResizeableX() && (AbstractEditorElement.this.element.advancedWidth == null);
			}
			return false;
		}

		protected boolean isMouseOver(double mouseX, double mouseY) {
			return (mouseX >= this.getX()) && (mouseX <= this.getX() + this.width) && (mouseY >= this.getY()) && mouseY <= this.getY() + this.height;
		}

	}

	@SuppressWarnings("all")
	protected <E extends AbstractEditorElement> ContextMenu.ClickableContextMenuEntry<?> addImageResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<E> elementType, ResourceSupplier<ITexture> defaultValue, @NotNull ConsumingSupplier<E, ResourceSupplier<ITexture>> targetFieldGetter, @NotNull BiConsumer<E, ResourceSupplier<ITexture>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
		ConsumingSupplier<AbstractEditorElement, ResourceSupplier<ITexture>> getter = (ConsumingSupplier<AbstractEditorElement, ResourceSupplier<ITexture>>) targetFieldGetter;
		BiConsumer<AbstractEditorElement, ResourceSupplier<ITexture>> setter = (BiConsumer<AbstractEditorElement, ResourceSupplier<ITexture>>) targetFieldSetter;
		return addGenericResourceChooserContextMenuEntryTo(addTo, entryIdentifier, (consumes) -> elementType.isAssignableFrom(consumes.getClass()), () -> ResourceChooserScreen.image(null, file -> {}), ResourceSupplier::image, defaultValue, getter, setter, label, addResetOption, FileTypeGroups.IMAGE_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
	}

	@SuppressWarnings("all")
	protected <E extends AbstractEditorElement> ContextMenu.ClickableContextMenuEntry<?> addAudioResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<E> elementType, ResourceSupplier<IAudio> defaultValue, @NotNull ConsumingSupplier<E, ResourceSupplier<IAudio>> targetFieldGetter, @NotNull BiConsumer<E, ResourceSupplier<IAudio>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
		ConsumingSupplier<AbstractEditorElement, ResourceSupplier<IAudio>> getter = (ConsumingSupplier<AbstractEditorElement, ResourceSupplier<IAudio>>) targetFieldGetter;
		BiConsumer<AbstractEditorElement, ResourceSupplier<IAudio>> setter = (BiConsumer<AbstractEditorElement, ResourceSupplier<IAudio>>) targetFieldSetter;
		return addGenericResourceChooserContextMenuEntryTo(addTo, entryIdentifier, (consumes) -> elementType.isAssignableFrom(consumes.getClass()), () -> ResourceChooserScreen.audio(null, file -> {}), ResourceSupplier::audio, defaultValue, getter, setter, label, addResetOption, FileTypeGroups.AUDIO_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
	}

	@SuppressWarnings("all")
	protected <E extends AbstractEditorElement> ContextMenu.ClickableContextMenuEntry<?> addVideoResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<E> elementType, ResourceSupplier<IVideo> defaultValue, @NotNull ConsumingSupplier<E, ResourceSupplier<IVideo>> targetFieldGetter, @NotNull BiConsumer<E, ResourceSupplier<IVideo>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
		ConsumingSupplier<AbstractEditorElement, ResourceSupplier<IVideo>> getter = (ConsumingSupplier<AbstractEditorElement, ResourceSupplier<IVideo>>) targetFieldGetter;
		BiConsumer<AbstractEditorElement, ResourceSupplier<IVideo>> setter = (BiConsumer<AbstractEditorElement, ResourceSupplier<IVideo>>) targetFieldSetter;
		return addGenericResourceChooserContextMenuEntryTo(addTo, entryIdentifier, (consumes) -> elementType.isAssignableFrom(consumes.getClass()), () -> ResourceChooserScreen.video(null, file -> {}), ResourceSupplier::video, defaultValue, getter, setter, label, addResetOption, FileTypeGroups.VIDEO_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
	}

	@SuppressWarnings("all")
	protected <E extends AbstractEditorElement> ContextMenu.ClickableContextMenuEntry<?> addTextResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<E> elementType, ResourceSupplier<IText> defaultValue, @NotNull ConsumingSupplier<E, ResourceSupplier<IText>> targetFieldGetter, @NotNull BiConsumer<E, ResourceSupplier<IText>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
		ConsumingSupplier<AbstractEditorElement, ResourceSupplier<IText>> getter = (ConsumingSupplier<AbstractEditorElement, ResourceSupplier<IText>>) targetFieldGetter;
		BiConsumer<AbstractEditorElement, ResourceSupplier<IText>> setter = (BiConsumer<AbstractEditorElement, ResourceSupplier<IText>>) targetFieldSetter;
		return addGenericResourceChooserContextMenuEntryTo(addTo, entryIdentifier, (consumes) -> elementType.isAssignableFrom(consumes.getClass()), () -> ResourceChooserScreen.text(null, file -> {}), ResourceSupplier::text, defaultValue, getter, setter, label, addResetOption, FileTypeGroups.TEXT_TYPES, fileFilter, allowLocation, allowLocal, allowWeb);
	}

	@SuppressWarnings("all")
	protected <R extends Resource, F extends FileType<R>, E extends AbstractEditorElement> ContextMenu.ClickableContextMenuEntry<?> addGenericResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<E> elementType, @NotNull Supplier<ResourceChooserScreen<R,F>> resourceChooserScreenBuilder, @NotNull ConsumingSupplier<String, ResourceSupplier<R>> resourceSupplierBuilder, ResourceSupplier<R> defaultValue, @NotNull ConsumingSupplier<E, ResourceSupplier<R>> targetFieldGetter, @NotNull BiConsumer<E, ResourceSupplier<R>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileTypeGroup<F> fileTypes, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {
		ConsumingSupplier<AbstractEditorElement, ResourceSupplier<R>> getter = (ConsumingSupplier<AbstractEditorElement, ResourceSupplier<R>>) targetFieldGetter;
		BiConsumer<AbstractEditorElement, ResourceSupplier<R>> setter = (BiConsumer<AbstractEditorElement, ResourceSupplier<R>>) targetFieldSetter;
		return addGenericResourceChooserContextMenuEntryTo(addTo, entryIdentifier, (consumes) -> elementType.isAssignableFrom(consumes.getClass()), resourceChooserScreenBuilder, resourceSupplierBuilder, defaultValue, getter, setter, label, addResetOption, fileTypes, fileFilter, allowLocation, allowLocal, allowWeb);
	}

	protected <R extends Resource, F extends FileType<R>> ContextMenu.ClickableContextMenuEntry<?> addGenericResourceChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, @NotNull Supplier<ResourceChooserScreen<R,F>> resourceChooserScreenBuilder, @NotNull ConsumingSupplier<String, ResourceSupplier<R>> resourceSupplierBuilder, ResourceSupplier<R> defaultValue, @NotNull ConsumingSupplier<AbstractEditorElement, ResourceSupplier<R>> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, ResourceSupplier<R>> targetFieldSetter, @NotNull Component label, boolean addResetOption, @Nullable FileTypeGroup<F> fileTypes, @Nullable FileFilter fileFilter, boolean allowLocation, boolean allowLocal, boolean allowWeb) {

		ContextMenu subMenu = new ContextMenu();

		subMenu.addClickableEntry("choose_file", Component.translatable("fancymenu.ui.resources.choose"),
				(menu, entry) -> {
					List<AbstractEditorElement> selectedElements = this.getFilteredSelectedElementList(selectedElementsFilter);
					if (entry.getStackMeta().isFirstInStack() && !selectedElements.isEmpty()) {
						String preSelectedSource = null;
						List<String> allPaths = ObjectUtils.getOfAll(String.class, selectedElements, consumes -> {
							ResourceSupplier<R> supplier = targetFieldGetter.get(consumes);
							if (supplier != null) return supplier.getSourceWithPrefix();
							return null;
						});
						if (!allPaths.isEmpty() && ListUtils.allInListEqual(allPaths)) {
							preSelectedSource = allPaths.get(0);
						}
						ResourceChooserScreen<R,F> chooserScreen = resourceChooserScreenBuilder.get();
						chooserScreen.setFileFilter(fileFilter);
						chooserScreen.setAllowedFileTypes(fileTypes);
						chooserScreen.setSource(preSelectedSource, false);
						chooserScreen.setLocationSourceAllowed(allowLocation);
						chooserScreen.setLocalSourceAllowed(allowLocal);
						chooserScreen.setWebSourceAllowed(allowWeb);
						chooserScreen.setResourceSourceCallback(source -> {
							if (source != null) {
								this.editor.history.saveSnapshot();
								for (AbstractEditorElement e : selectedElements) {
									targetFieldSetter.accept(e, resourceSupplierBuilder.get(source));
								}
							}
							Minecraft.getInstance().setScreen(this.editor);
						});
						Minecraft.getInstance().setScreen(chooserScreen);
					}
				}).setStackable(true);

		if (addResetOption) {
			subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.ui.resources.reset"),
					(menu, entry) -> {
						if (entry.getStackMeta().isFirstInStack()) {
							List<AbstractEditorElement> selectedElements = this.getFilteredSelectedElementList(selectedElementsFilter);
							this.editor.history.saveSnapshot();
							for (AbstractEditorElement e : selectedElements) {
								targetFieldSetter.accept(e, defaultValue);
							}
						}
					}).setStackable(true);
		}

		Supplier<Component> currentValueDisplayLabelSupplier = () -> {
			List<AbstractEditorElement> selectedElements = this.getFilteredSelectedElementList(selectedElementsFilter);
			if (selectedElements.size() == 1) {
				Component valueComponent;
				ResourceSupplier<R> supplier = targetFieldGetter.get(selectedElements.get(0));
				String val = (supplier != null) ? supplier.getSourceWithoutPrefix() : null;
				if (val == null) {
					valueComponent = Component.literal("---").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt()));
				} else {
					val = GameDirectoryUtils.getPathWithoutGameDirectory(val);
					if (Minecraft.getInstance().font.width(val) > 150) {
						val = new StringBuilder(val).reverse().toString();
						val = Minecraft.getInstance().font.plainSubstrByWidth(val, 150);
						val = new StringBuilder(val).reverse().toString();
						val = ".." + val;
					}
					valueComponent = Component.literal(val).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().success_text_color.getColorInt()));
				}
				return Component.translatable("fancymenu.ui.resources.current", valueComponent);
			}
			return Component.empty();
		};
		subMenu.addSeparatorEntry("separator_before_current_value_display")
				.setIsVisibleSupplier((menu, entry) -> this.getFilteredSelectedElementList(selectedElementsFilter).size() == 1);
		subMenu.addClickableEntry("current_value_display", Component.empty(), (menu, entry) -> {})
				.setLabelSupplier((menu, entry) -> currentValueDisplayLabelSupplier.get())
				.setClickSoundEnabled(false)
				.setChangeBackgroundColorOnHover(false)
				.setIsVisibleSupplier((menu, entry) -> this.getFilteredSelectedElementList(selectedElementsFilter).size() == 1)
				.setIcon(ContextMenu.IconFactory.getIcon("info"));

		return addTo.addSubMenuEntry(entryIdentifier, label, subMenu).setStackable(true);

	}

	protected ContextMenu.ClickableContextMenuEntry<?> addInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<AbstractEditorElement, String> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
		ContextMenu subMenu = new ContextMenu();
		ContextMenu.ClickableContextMenuEntry<?> inputEntry = subMenu.addClickableEntry("input_value", Component.translatable("fancymenu.guicomponents.set"), (menu, entry) ->
		{
			if (entry.getStackMeta().isFirstInStack()) {
				List<AbstractEditorElement> selectedElements = this.getFilteredSelectedElementList(selectedElementsFilter);
				String defaultText = null;
				List<String> targetValuesOfSelected = new ArrayList<>();
				for (AbstractEditorElement e : selectedElements) {
					targetValuesOfSelected.add(targetFieldGetter.get(e));
				}
				if (!entry.getStackMeta().isPartOfStack() || ListUtils.allInListEqual(targetValuesOfSelected)) {
					defaultText = targetFieldGetter.get(this);
				}
				Screen inputScreen;
				if (!multiLineInput && !allowPlaceholders) {
					TextInputScreen s = TextInputScreen.build(label, inputCharacterFilter, call -> {
						if (call != null) {
							this.editor.history.saveSnapshot();
							for (AbstractEditorElement e : selectedElements) {
								targetFieldSetter.accept(e, call);
							}
						}
						menu.closeMenu();
						Minecraft.getInstance().setScreen(this.editor);
					});
					if (textValidator != null) {
						s.setTextValidator(consumes -> {
							if (textValidatorUserFeedback != null) consumes.setTextValidatorUserFeedback(textValidatorUserFeedback.get(consumes.getText()));
							return textValidator.get(consumes.getText());
						});
					}
					s.setText(defaultText);
					inputScreen = s;
				} else {
					TextEditorScreen s = new TextEditorScreen(label, (inputCharacterFilter != null) ? inputCharacterFilter.convertToLegacyFilter() : null, (call) -> {
						if (call != null) {
							this.editor.history.saveSnapshot();
							for (AbstractEditorElement e : selectedElements) {
								targetFieldSetter.accept(e, call);
							}
						}
						menu.closeMenu();
						Minecraft.getInstance().setScreen(this.editor);
					});
					if (textValidator != null) {
						s.setTextValidator(consumes -> {
							if (textValidatorUserFeedback != null) consumes.setTextValidatorUserFeedback(textValidatorUserFeedback.get(consumes.getText()));
							return textValidator.get(consumes.getText());
						});
					}
					s.setText(defaultText);
					s.setMultilineMode(multiLineInput);
					s.setPlaceholdersAllowed(allowPlaceholders);
					inputScreen = s;
				}
				Minecraft.getInstance().setScreen(inputScreen);
			}
		}).setStackable(true);

		if (addResetOption) {
			subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.guicomponents.reset"), (menu, entry) -> {
				if (entry.getStackMeta().isFirstInStack()) {
					List<AbstractEditorElement> selectedElements = this.getFilteredSelectedElementList(selectedElementsFilter);
					this.editor.history.saveSnapshot();
					for (AbstractEditorElement e : selectedElements) {
						targetFieldSetter.accept(e, defaultValue);
					}
				}
			}).setStackable(true);
		}

		Supplier<Component> currentValueDisplayLabelSupplier = () -> {
			List<AbstractEditorElement> selectedElements = this.getFilteredSelectedElementList(selectedElementsFilter);
			if (selectedElements.size() == 1) {
				Component valueComponent;
				String val = targetFieldGetter.get(selectedElements.get(0));
				if (val == null) {
					valueComponent = Component.literal("---").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt()));
				} else {
					val = GameDirectoryUtils.getPathWithoutGameDirectory(val);
					if (Minecraft.getInstance().font.width(val) > 150) {
						val = new StringBuilder(val).reverse().toString();
						val = Minecraft.getInstance().font.plainSubstrByWidth(val, 150);
						val = new StringBuilder(val).reverse().toString();
						val = ".." + val;
					}
					valueComponent = Component.literal(val).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().success_text_color.getColorInt()));
				}
				return Component.translatable("fancymenu.context_menu.entries.choose_or_set.current", valueComponent);
			}
			return Component.empty();
		};
		subMenu.addSeparatorEntry("separator_before_current_value_display")
				.setIsVisibleSupplier((menu, entry) -> this.getFilteredSelectedElementList(selectedElementsFilter).size() == 1);
		subMenu.addClickableEntry("current_value_display", Component.empty(), (menu, entry) -> {})
				.setLabelSupplier((menu, entry) -> currentValueDisplayLabelSupplier.get())
				.setClickSoundEnabled(false)
				.setChangeBackgroundColorOnHover(false)
				.setIsVisibleSupplier((menu, entry) -> this.getFilteredSelectedElementList(selectedElementsFilter).size() == 1)
				.setIcon(ContextMenu.IconFactory.getIcon("info"));

		return addTo.addSubMenuEntry(entryIdentifier, label, subMenu).setStackable(true);

	}

	@SuppressWarnings("all")
	protected <E extends AbstractEditorElement> ContextMenu.ClickableContextMenuEntry<?> addInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<E> elementType, @NotNull ConsumingSupplier<E, String> targetFieldGetter, @NotNull BiConsumer<E, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
		ConsumingSupplier<AbstractEditorElement, String> getter = (ConsumingSupplier<AbstractEditorElement, String>) targetFieldGetter;
		BiConsumer<AbstractEditorElement, String> setter = (BiConsumer<AbstractEditorElement, String>) targetFieldSetter;
		return this.addInputContextMenuEntryTo(addTo, entryIdentifier, (consumes) -> elementType.isAssignableFrom(consumes.getClass()), getter, setter, inputCharacterFilter, multiLineInput, allowPlaceholders, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
	}

	protected ContextMenu.ClickableContextMenuEntry<?> addGenericStringInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<AbstractEditorElement, String> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
		return addInputContextMenuEntryTo(addTo, entryIdentifier, selectedElementsFilter, targetFieldGetter, targetFieldSetter, inputCharacterFilter, multiLineInput, allowPlaceholders, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
	}

	@SuppressWarnings("all")
	protected <E extends AbstractEditorElement> ContextMenu.ClickableContextMenuEntry<?> addStringInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<E> elementType, @NotNull ConsumingSupplier<E, String> targetFieldGetter, @NotNull BiConsumer<E, String> targetFieldSetter, @Nullable CharacterFilter inputCharacterFilter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label, boolean addResetOption, String defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
		ConsumingSupplier<AbstractEditorElement, String> getter = (ConsumingSupplier<AbstractEditorElement, String>) targetFieldGetter;
		BiConsumer<AbstractEditorElement, String> setter = (BiConsumer<AbstractEditorElement, String>) targetFieldSetter;
		return this.addGenericStringInputContextMenuEntryTo(addTo, entryIdentifier, consumes -> elementType.isAssignableFrom(consumes.getClass()), getter, setter, inputCharacterFilter, multiLineInput, allowPlaceholders, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
	}

	protected ContextMenu.ClickableContextMenuEntry<?> addGenericIntegerInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<AbstractEditorElement, Integer> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, Integer> targetFieldSetter, @NotNull Component label, boolean addResetOption, int defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
		ConsumingSupplier<String, Boolean> defaultIntegerValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isInteger(consumes);
		return addInputContextMenuEntryTo(addTo, entryIdentifier, selectedElementsFilter,
				consumes -> {
					Integer i = targetFieldGetter.get(consumes);
					if (i == null) i = 0;
					return "" + i;
				},
				(e, s) -> {
					if (MathUtils.isInteger(s)) targetFieldSetter.accept(e, Integer.valueOf(s));
				},
				CharacterFilter.buildIntegerFiler(), false, false, label, addResetOption, "" + defaultValue,
				(textValidator != null) ? textValidator : defaultIntegerValidator, textValidatorUserFeedback);
	}

	@SuppressWarnings("all")
	protected <E extends AbstractEditorElement> ContextMenu.ClickableContextMenuEntry<?> addIntegerInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<E> elementType, @NotNull ConsumingSupplier<E, Integer> targetFieldGetter, @NotNull BiConsumer<E, Integer> targetFieldSetter, @NotNull Component label, boolean addResetOption, int defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
		ConsumingSupplier<AbstractEditorElement, Integer> getter = (ConsumingSupplier<AbstractEditorElement, Integer>) targetFieldGetter;
		BiConsumer<AbstractEditorElement, Integer> setter = (BiConsumer<AbstractEditorElement, Integer>) targetFieldSetter;
		return this.addGenericIntegerInputContextMenuEntryTo(addTo, entryIdentifier, consumes -> elementType.isAssignableFrom(consumes.getClass()), getter, setter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
	}

	protected ContextMenu.ClickableContextMenuEntry<?> addGenericLongInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<AbstractEditorElement, Long> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, Long> targetFieldSetter, @NotNull Component label, boolean addResetOption, long defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
		ConsumingSupplier<String, Boolean> defaultLongValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isLong(consumes);
		return addInputContextMenuEntryTo(addTo, entryIdentifier, selectedElementsFilter,
				consumes -> {
					Long l = targetFieldGetter.get(consumes);
					if (l == null) l = 0L;
					return "" + l;
				},
				(e, s) -> {
					if (MathUtils.isLong(s)) targetFieldSetter.accept(e, Long.valueOf(s));
				},
				CharacterFilter.buildIntegerFiler(), false, false, label, addResetOption, "" + defaultValue,
				(textValidator != null) ? textValidator : defaultLongValidator, textValidatorUserFeedback);
	}

	@SuppressWarnings("all")
	protected <E extends AbstractEditorElement> ContextMenu.ClickableContextMenuEntry<?> addLongInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<E> elementType, @NotNull ConsumingSupplier<E, Long> targetFieldGetter, @NotNull BiConsumer<E, Long> targetFieldSetter, @NotNull Component label, boolean addResetOption, long defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
		ConsumingSupplier<AbstractEditorElement, Long> getter = (ConsumingSupplier<AbstractEditorElement, Long>) targetFieldGetter;
		BiConsumer<AbstractEditorElement, Long> setter = (BiConsumer<AbstractEditorElement, Long>) targetFieldSetter;
		return this.addGenericLongInputContextMenuEntryTo(addTo, entryIdentifier, consumes -> elementType.isAssignableFrom(consumes.getClass()), getter, setter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
	}

	protected ContextMenu.ClickableContextMenuEntry<?> addGenericDoubleInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<AbstractEditorElement, Double> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, Double> targetFieldSetter, @NotNull Component label, boolean addResetOption, double defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
		ConsumingSupplier<String, Boolean> defaultDoubleValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isDouble(consumes);
		return addInputContextMenuEntryTo(addTo, entryIdentifier, selectedElementsFilter,
				consumes -> {
					Double d = targetFieldGetter.get(consumes);
					if (d == null) d = 0D;
					return "" + d;
				},
				(e, s) -> {
					if (MathUtils.isDouble(s)) targetFieldSetter.accept(e, Double.valueOf(s));
				},
				CharacterFilter.buildDecimalFiler(), false, false, label, addResetOption, "" + defaultValue,
				(textValidator != null) ? textValidator : defaultDoubleValidator, textValidatorUserFeedback);
	}

	@SuppressWarnings("all")
	protected <E extends AbstractEditorElement> ContextMenu.ClickableContextMenuEntry<?> addDoubleInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<E> elementType, @NotNull ConsumingSupplier<E, Double> targetFieldGetter, @NotNull BiConsumer<E, Double> targetFieldSetter, @NotNull Component label, boolean addResetOption, double defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
		ConsumingSupplier<AbstractEditorElement, Double> getter = (ConsumingSupplier<AbstractEditorElement, Double>) targetFieldGetter;
		BiConsumer<AbstractEditorElement, Double> setter = (BiConsumer<AbstractEditorElement, Double>) targetFieldSetter;
		return this.addGenericDoubleInputContextMenuEntryTo(addTo, entryIdentifier, consumes -> elementType.isAssignableFrom(consumes.getClass()), getter, setter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
	}

	protected ContextMenu.ClickableContextMenuEntry<?> addGenericFloatInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<AbstractEditorElement, Float> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, Float> targetFieldSetter, @NotNull Component label, boolean addResetOption, float defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
		ConsumingSupplier<String, Boolean> defaultFloatValidator = consumes -> (consumes != null) && !consumes.replace(" ", "").isEmpty() && MathUtils.isFloat(consumes);
		return addInputContextMenuEntryTo(addTo, entryIdentifier, selectedElementsFilter,
				consumes -> {
					Float f = targetFieldGetter.get(consumes);
					if (f == null) f = 0F;
					return "" + f;
				},
				(e, s) -> {
					if (MathUtils.isFloat(s)) targetFieldSetter.accept(e, Float.valueOf(s));
				},
				CharacterFilter.buildDecimalFiler(), false, false, label, addResetOption, "" + defaultValue,
				(textValidator != null) ? textValidator : defaultFloatValidator, textValidatorUserFeedback);
	}

	@SuppressWarnings("all")
	protected <E extends AbstractEditorElement> ContextMenu.ClickableContextMenuEntry<?> addFloatInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<E> elementType, @NotNull ConsumingSupplier<E, Float> targetFieldGetter, @NotNull BiConsumer<E, Float> targetFieldSetter, @NotNull Component label, boolean addResetOption, float defaultValue, @Nullable ConsumingSupplier<String, Boolean> textValidator, @Nullable ConsumingSupplier<String, Tooltip> textValidatorUserFeedback) {
		ConsumingSupplier<AbstractEditorElement, Float> getter = (ConsumingSupplier<AbstractEditorElement, Float>) targetFieldGetter;
		BiConsumer<AbstractEditorElement, Float> setter = (BiConsumer<AbstractEditorElement, Float>) targetFieldSetter;
		return this.addGenericFloatInputContextMenuEntryTo(addTo, entryIdentifier, consumes -> elementType.isAssignableFrom(consumes.getClass()), getter, setter, label, addResetOption, defaultValue, textValidator, textValidatorUserFeedback);
	}

	protected <V> ContextMenu.ClickableContextMenuEntry<?> addGenericCycleContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, List<V> switcherValues, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<AbstractEditorElement, V> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, V> targetFieldSetter, @NotNull AbstractEditorElement.SwitcherContextMenuEntryLabelSupplier<V> labelSupplier) {
		return addTo.addClickableEntry(entryIdentifier, Component.literal(""), (menu, entry) ->
				{
					List<AbstractEditorElement> selectedElements = this.getFilteredSelectedElementList(selectedElementsFilter);
					ValueCycle<V> cycle = this.setupValueCycle("switcher", ValueCycle.fromList(switcherValues), selectedElements, entry.getStackMeta(), targetFieldGetter);
					this.editor.history.saveSnapshot();
					if (!selectedElements.isEmpty() && entry.getStackMeta().isFirstInStack()) {
						V next = cycle.next();
						for (AbstractEditorElement e : selectedElements) {
							targetFieldSetter.accept(e, next);
						}
					}
				})
				.setLabelSupplier((menu, entry) -> {
					List<AbstractEditorElement> selectedElements = new ArrayList<>();
					if (!entry.getStackMeta().getProperties().hasProperty("switcher")) {
						selectedElements = this.getFilteredSelectedElementList(selectedElementsFilter);
					}
					ValueCycle<V> switcher = this.setupValueCycle("switcher", ValueCycle.fromList(switcherValues), selectedElements, entry.getStackMeta(), targetFieldGetter);
					return labelSupplier.get(menu, (ContextMenu.ClickableContextMenuEntry<?>) entry, switcher.current());
				}).setStackable(true);
	}

	@SuppressWarnings("all")
	protected <V, E extends AbstractEditorElement> ContextMenu.ClickableContextMenuEntry<?> addCycleContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, List<V> switcherValues, @NotNull Class<E> elementType, @NotNull ConsumingSupplier<E, V> targetFieldGetter, @NotNull BiConsumer<E, V> targetFieldSetter, @NotNull AbstractEditorElement.SwitcherContextMenuEntryLabelSupplier<V> labelSupplier) {
		ConsumingSupplier<AbstractEditorElement, V> getter = (ConsumingSupplier<AbstractEditorElement, V>) targetFieldGetter;
		BiConsumer<AbstractEditorElement, V> setter = (BiConsumer<AbstractEditorElement, V>) targetFieldSetter;
		return this.addGenericCycleContextMenuEntryTo(addTo, entryIdentifier, switcherValues, consumes -> elementType.isAssignableFrom(consumes.getClass()), getter, setter, labelSupplier);
	}

	@SuppressWarnings("all")
	protected <E extends AbstractEditorElement> ContextMenu.ClickableContextMenuEntry<?> addToggleContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @NotNull Class<E> elementType, @NotNull ConsumingSupplier<E, Boolean> targetFieldGetter, @NotNull BiConsumer<E, Boolean> targetFieldSetter, @NotNull String labelLocalizationKeyBase) {
		ConsumingSupplier<AbstractEditorElement, Boolean> getter = (ConsumingSupplier<AbstractEditorElement, Boolean>) targetFieldGetter;
		BiConsumer<AbstractEditorElement, Boolean> setter = (BiConsumer<AbstractEditorElement, Boolean>) targetFieldSetter;
		return addGenericCycleContextMenuEntryTo(addTo, entryIdentifier, ListUtils.of(false, true), consumes -> elementType.isAssignableFrom(consumes.getClass()), getter, setter, (menu, entry, switcherValue) -> {
			if (switcherValue && entry.isActive()) {
				MutableComponent enabled = Component.translatable("fancymenu.general.cycle.enabled_disabled.enabled").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().success_text_color.getColorInt()));
				return Component.translatable(labelLocalizationKeyBase, enabled);
			}
			MutableComponent disabled = Component.translatable("fancymenu.general.cycle.enabled_disabled.disabled").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt()));
			return Component.translatable(labelLocalizationKeyBase, disabled);
		}).setStackable(true);
	}

	/**
	 * Only supports old (legacy) toggle localization keys (format = localization.key.on / .off)!<br>
	 * For newer localization keys, use <b>AbstractEditorElement#addToggleContextMenuEntryTo(...)</b> instead!
	 */
	@Deprecated
	@Legacy("This is to be able to use old .on/.off localizations. Remove this in the future and update localizations.")
	protected ContextMenu.ClickableContextMenuEntry<?> addGenericBooleanSwitcherContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<AbstractEditorElement, Boolean> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, Boolean> targetFieldSetter, @NotNull String labelLocalizationKeyBase) {
		return addGenericCycleContextMenuEntryTo(addTo, entryIdentifier, ListUtils.of(false, true), selectedElementsFilter, targetFieldGetter, targetFieldSetter, (menu, entry, switcherValue) -> {
			if (switcherValue && entry.isActive()) {
				return Component.translatable(labelLocalizationKeyBase + ".on");
			}
			return Component.translatable(labelLocalizationKeyBase + ".off");
		}).setStackable(true);
	}

	@SuppressWarnings("all")
	protected <T, E extends AbstractEditorElement> ValueCycle<T> setupValueCycle(String toggleIdentifier, ValueCycle<T> cycle, List<E> elements, ContextMenu.ContextMenuStackMeta stackMeta, ConsumingSupplier<E, T> defaultValue) {
		boolean hasProperty = stackMeta.getProperties().hasProperty(toggleIdentifier);
		ValueCycle<T> t = stackMeta.getProperties().putPropertyIfAbsentAndGet(toggleIdentifier, cycle);
		if (!elements.isEmpty()) {
			E firstElement = elements.get(0);
			if (!stackMeta.isPartOfStack()) {
				t.setCurrentValue(defaultValue.get(firstElement));
			} else if (!hasProperty) {
				if (ListUtils.allInListEqual(ObjectUtils.getOfAllUnsafe((List<Object>)((Object)elements), (ConsumingSupplier<Object,Object>)((Object)defaultValue)))) {
					t.setCurrentValue(defaultValue.get(firstElement));
				}
			}
		}
		return t;
	}

	protected List<AbstractEditorElement> getFilteredSelectedElementList(@Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter) {
		return ListUtils.filterList(this.editor.getSelectedElements(), (consumes) -> {
			if (selectedElementsFilter == null) {
				return true;
			}
			return selectedElementsFilter.get(consumes);
		});
	}

	@FunctionalInterface
	protected interface SwitcherContextMenuEntryLabelSupplier<V> {
		Component get(ContextMenu menu, ContextMenu.ClickableContextMenuEntry<?> entry, V switcherValue);
	}

	public enum ResizeGrabberType {
		TOP,
		RIGHT,
		BOTTOM,
		LEFT
	}

}

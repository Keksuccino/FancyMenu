package de.keksuccino.fancymenu.customization.element.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.layout.editor.AnchorPointOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorHistory;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.ui.ManageRequirementsScreen;
import de.keksuccino.fancymenu.util.*;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.SmoothCircleRenderer;
import de.keksuccino.fancymenu.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuBuilder;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class AbstractEditorElement<E extends AbstractEditorElement<?, ?>, N extends AbstractElement> implements Renderable, GuiEventListener, ContextMenuBuilder<E>, PropertyHolder {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final ResourceLocation DRAGGING_NOT_ALLOWED_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/not_allowed.png");
    protected static final ResourceLocation DEPRECATED_WARNING_TEXTURE = ResourceLocation.fromNamespaceAndPath("fancymenu", "textures/warning_20x20.png");
    protected static final ConsumingSupplier<AbstractEditorElement<?, ?>, Integer> BORDER_COLOR = (editorElement) -> {
        if (editorElement.isSelected()) {
            return UIBase.getUITheme().layout_editor_element_border_color_selected.getColorInt();
        }
        return UIBase.getUITheme().layout_editor_element_border_color_normal.getColorInt();
    };
    protected static final ConsumingSupplier<AbstractEditorElement<?, ?>, Float> HORIZONTAL_TILT_CONTROLS_ALPHA = consumes -> {
        if (consumes.horizontalTiltGrabber.hovered || consumes.isGettingHorizontalTilted()) {
            return 1.0F;
        }
        return 0.7F;
    };
    protected static final ConsumingSupplier<AbstractEditorElement<?, ?>, Float> VERTICAL_TILT_CONTROLS_ALPHA = consumes -> {
        if (consumes.verticalTiltGrabber.hovered || consumes.isGettingVerticalTilted()) {
            return 1.0F;
        }
        return 0.7F;
    };
    protected static final ConsumingSupplier<AbstractEditorElement<?, ?>, Float> ROTATION_CONTROLS_ALPHA = consumes -> {
        if (consumes.rotationGrabber.hovered || consumes.isGettingRotated()) {
            return 1.0F;
        }
        return 0.7F;
    };
    protected static final int RESIZE_EDGE_GRAB_ZONE = 4;
    protected static final int RESIZE_INDICATOR_SIZE = 5;
    protected static final float RESIZE_INDICATOR_CORNER_RADIUS = 1.0F;
    protected static final int TILT_CONTROLS_PADDING = 10;
    protected static final int TILT_CONTROLS_LINE_EXTENSION = 20;
    protected static final float TILT_ROTATION_ZERO_SNAP_DEGREES = 2.0F;

    @NotNull
    public N element;
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
    protected @Nullable ResizeGrabberType activeResizeGrabberType = null;
    protected @Nullable ResizeGrabberType hoveredResizeGrabberType = null;
    protected RotationGrabber rotationGrabber = new RotationGrabber();
    protected boolean rotationGrabberActive = false;
    protected float rotationStartAngle = 0.0F;
    protected double rotationStartMouseAngle = 0.0;
    protected LayoutEditorHistory.Snapshot preRotationSnapshot;
    protected VerticalTiltGrabber verticalTiltGrabber = new VerticalTiltGrabber();
    protected boolean verticalTiltGrabberActive = false;
    protected float verticalTiltStartAngle = 0.0F;
    protected double verticalTiltStartMouseY = 0.0;
    protected HorizontalTiltGrabber horizontalTiltGrabber = new HorizontalTiltGrabber();
    protected boolean horizontalTiltGrabberActive = false;
    protected float horizontalTiltStartAngle = 0.0F;
    protected double horizontalTiltStartMouseX = 0.0;
    protected LayoutEditorHistory.Snapshot preTiltSnapshot;
    protected AspectRatio resizeAspectRatio = new AspectRatio(10, 10);
    public long renderMovingNotAllowedTime = -1;
    public boolean recentlyMovedByDragging = false;
    public boolean recentlyLeftClickSelected = false;
    public boolean recentlyResized = false;
    public boolean movingCrumpleZonePassed = false;

    private final List<AbstractEditorElement<?, ?>> cachedHoveredElementsOnRightClickMenuOpen = new ArrayList<>();
    private final List<ContextMenuBuilder.ContextMenuScreenOpenProcessor> contextMenuScreenOpenProcessorList = new ArrayList<>();

    public AbstractEditorElement(@NotNull N element, @NotNull LayoutEditorScreen editor, @Nullable EditorElementSettings settings) {
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
        this.addContextMenuScreenOpenProcessor(screen -> {
            this.editor.beforeOpenChildScreen(screen);
        });
        this.init();
    }

    public AbstractEditorElement(@NotNull N element, @NotNull LayoutEditorScreen editor) {
        this(element, editor, new EditorElementSettings());
    }

    @SuppressWarnings("unchecked")
    public @NotNull E self() {
        return (E) this;
    }

    @SuppressWarnings("unchecked")
    protected @NotNull Class<E> selfClass() {
        return (Class<E>) this.getClass();
    }

    @Override
    public @NotNull Map<String, Property<?>> getPropertyMap() {
        return this.element.getPropertyMap();
    }

    public void init() {

        this.rightClickMenu.closeMenu();
        this.rightClickMenu.clearEntries();
        this.topLeftDisplay.clearLines();
        this.bottomRightDisplay.clearLines();

        this.topLeftDisplay.addLine("anchor_point", () -> Component.translatable("fancymenu.element.border_display.anchor_point", this.element.anchorPoint.getDisplayName()));
        this.topLeftDisplay.addLine("pos_x", () -> Component.translatable("fancymenu.element.border_display.pos_x", "" + this.getX()));
        this.topLeftDisplay.addLine("width", () -> Component.translatable("fancymenu.element.border_display.width", "" + this.getWidth()));
        if (this.element.getBuilder().isDeprecated()) {
            this.topLeftDisplay.addLine("deprecated_warning_line0", Component::empty);
            this.topLeftDisplay.addLine("deprecated_warning_line1", () -> Component.translatable("fancymenu.elements.deprecated_warning.line1").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt())));
            this.topLeftDisplay.addLine("deprecated_warning_line2", () -> Component.translatable("fancymenu.elements.deprecated_warning.line2").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt())));
            this.topLeftDisplay.addLine("deprecated_warning_line3", () -> Component.translatable("fancymenu.elements.deprecated_warning.line3").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt())));
        }

        this.bottomRightDisplay.addLine("pos_y", () -> Component.translatable("fancymenu.element.border_display.pos_y", "" + this.getY()));
        this.bottomRightDisplay.addLine("height", () -> Component.translatable("fancymenu.element.border_display.height", "" + this.getHeight()));

        ContextMenu pickElementMenu = new ContextMenu() {
            @Override
            public @NotNull ContextMenu openMenuAt(float x, float y) {
                this.clearEntries();
                int i = 0;
                for (AbstractEditorElement<?, ?> e : cachedHoveredElementsOnRightClickMenuOpen) {
                    this.addClickableEntry("element_" + i, e.element.getDisplayName(), (menu, entry) -> {
                        editor.getAllElements().forEach(AbstractEditorElement::resetElementStates);
                        e.setSelected(true);
                    }).setIcon(MaterialIcons.MOUSE);
                    i++;
                }
                return super.openMenuAt(x, y);
            }
        };
        this.rightClickMenu.addSubMenuEntry("pick_element", Component.translatable("fancymenu.element.general.pick_element"), pickElementMenu)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.general.pick_element.desc")))
                .setIcon(MaterialIcons.MOUSE);

        this.rightClickMenu.addSeparatorEntry("separator_1");

        if (this.settings.isIdentifierCopyable()) {

            this.rightClickMenu.addClickableEntry("copy_id", Component.translatable("fancymenu.elements.copyid"), (menu, entry) -> {
                        Minecraft.getInstance().keyboardHandler.setClipboard(this.element.getInstanceIdentifier());
                        menu.closeMenu();
                    }).setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.copyid.desc")))
                    .setIcon(MaterialIcons.CONTENT_COPY);

        }

        this.rightClickMenu.addSeparatorEntry("separator_2");

        this.addStringInputContextMenuEntryTo(this.rightClickMenu, "set_in_editor_display_name", this.selfClass(),
                        consumes -> consumes.element.customElementLayerName,
                        (abstractEditorElement, s) -> abstractEditorElement.element.customElementLayerName = s,
                        null, false, false, Component.translatable("fancymenu.elements.in_editor_display_name"), true, null, null, null)
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.in_editor_display_name.desc")))
                .setIcon(MaterialIcons.TEXT_FIELDS);

        if (this.settings.isInEditorColorSupported()) {

            this.element.inEditorColor.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.in_editor_color.desc")))
                    .setIcon(MaterialIcons.PALETTE);

        }

        this.rightClickMenu.addSeparatorEntry("separator_after_set_in_editor_stuff");

        this.element.shouldBeAffectedByDecorationOverlays.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .setIcon(MaterialIcons.LAYERS);

        this.rightClickMenu.addSeparatorEntry("separator_after_should_be_affected_by_decoration_overlays");

        if (this.settings.isAnchorPointChangeable()) {

            ContextMenu anchorPointMenu = new ContextMenu();
            this.rightClickMenu.addSubMenuEntry("anchor_point", Component.translatable("fancymenu.elements.anchor_point"), anchorPointMenu)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.anchor_points.desc")))
                    .setStackable(true)
                    .setIcon(MaterialIcons.ANCHOR);

            if (this.settings.isElementAnchorPointAllowed()) {

                anchorPointMenu.addClickableEntry("anchor_point_element", ElementAnchorPoints.ELEMENT.getDisplayName(),
                                (menu, entry) -> {
                                    if (entry.getStackMeta().isFirstInStack()) {
                                        TextInputWindowBody s = new TextInputWindowBody(null, call -> {
                                            if (call != null) {
                                                AbstractEditorElement<?, ?> editorElement = this.editor.getElementByInstanceIdentifier(call);
                                                if (editorElement != null) {
                                                    this.editor.history.saveSnapshot();
                                                    for (AbstractEditorElement<?, ?> e : this.editor.getSelectedElements()) {
                                                        if (e.settings.isAnchorPointChangeable() && e.settings.isElementAnchorPointAllowed()) {
                                                            e.element.setAnchorPointElementIdentifier(editorElement.element.getInstanceIdentifier());
                                                            e.element.setElementAnchorPointParent(editorElement.element);
                                                            e.setAnchorPoint(ElementAnchorPoints.ELEMENT, true);
                                                        }
                                                    }
                                                } else {
                                                    Dialogs.openMessage(Component.translatable("fancymenu.elements.anchor_points.element.setidentifier.identifiernotfound"), MessageDialogStyle.ERROR);
                                                }
                                            }
                                        });
                                        if (!entry.getStackMeta().isPartOfStack()) {
                                            s.setText(this.element.getAnchorPointElementIdentifier());
                                        }
                                        Dialogs.openGeneric(s,
                                                Component.translatable("fancymenu.elements.anchor_points.element.setidentifier"),
                                                null, TextInputWindowBody.PIP_WINDOW_WIDTH, TextInputWindowBody.PIP_WINDOW_HEIGHT).getSecond().setIcon(MaterialIcons.TEXT_FIELDS);
                                        menu.closeMenuChain();
                                    }
                                })
                        .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.anchor_points.element.desc")))
                        .setStackable(true)
                        .setIcon(MaterialIcons.LINK);

            }

            anchorPointMenu.addSeparatorEntry("separator_1").setStackable(true);

            for (ElementAnchorPoint p : ElementAnchorPoints.getAnchorPoints()) {
                if ((p != ElementAnchorPoints.ELEMENT) && (this.settings.isVanillaAnchorPointAllowed() || (p != ElementAnchorPoints.VANILLA))) {
                    anchorPointMenu.addClickableEntry("anchor_point_" + p.getName().replace("-", "_"), p.getDisplayName(), (menu, entry) -> {
                                if (entry.getStackMeta().isFirstInStack()) {
                                    this.editor.history.saveSnapshot();
                                    for (AbstractEditorElement<?, ?> e : this.editor.getSelectedElements()) {
                                        if (e.settings.isAnchorPointChangeable()) {
                                            e.setAnchorPoint(p, true);
                                        }
                                    }
                                    menu.closeMenu();
                                }
                            }).setStackable(true)
                            .setIcon(getAnchorPointIcon(p));
                }
            }

        }

        if (this.settings.isStayOnScreenAllowed()) {

            this.addToggleContextMenuEntryTo(this.rightClickMenu, "stay_on_screen", this.selfClass(),
                            consumes -> consumes.element.stayOnScreen,
                            (element1, aBoolean) -> element1.element.stayOnScreen = aBoolean,
                            "fancymenu.elements.element.stay_on_screen")
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(!this.element.stickyAnchor ? "fancymenu.elements.element.stay_on_screen.tooltip" : "fancymenu.elements.element.stay_on_screen.tooltip.disable_sticky")))
                    .setIcon(MaterialIcons.FIT_SCREEN)
                    .setStackable(false)
                    .addIsActiveSupplier((menu, entry) -> !this.element.stickyAnchor);

        }

        ContextMenu advancedPositioningMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("advanced_positioning", Component.translatable("fancymenu.elements.features.advanced_positioning"), advancedPositioningMenu)
                .addIsVisibleSupplier((menu, entry) -> this.settings.isAdvancedPositioningSupported())
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.features.advanced_positioning.desc")))
                .setStackable(true)
                .setIcon(MaterialIcons.MOVE);

        this.element.advancedX.buildContextMenuEntryAndAddTo(advancedPositioningMenu, this)
                .addIsVisibleSupplier((menu, entry) -> this.settings.isAdvancedPositioningSupported())
                .setStackable(true)
                .setIcon(MaterialIcons.MOVE);

        this.element.advancedY.buildContextMenuEntryAndAddTo(advancedPositioningMenu, this)
                .addIsVisibleSupplier((menu, entry) -> this.settings.isAdvancedPositioningSupported())
                .setStackable(true)
                .setIcon(MaterialIcons.MOVE);

        ContextMenu advancedSizingMenu = new ContextMenu();
        this.rightClickMenu.addSubMenuEntry("advanced_sizing", Component.translatable("fancymenu.elements.features.advanced_sizing"), advancedSizingMenu)
                .addIsVisibleSupplier((menu, entry) -> this.settings.isAdvancedSizingSupported())
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.features.advanced_sizing.desc")))
                .setStackable(true)
                .setIcon(MaterialIcons.STRAIGHTEN);

        this.element.advancedWidth.buildContextMenuEntryAndAddTo(advancedSizingMenu, this)
                .addIsVisibleSupplier((menu, entry) -> this.settings.isAdvancedSizingSupported())
                .setStackable(true)
                .setIcon(MaterialIcons.STRAIGHTEN);

        this.element.advancedHeight.buildContextMenuEntryAndAddTo(advancedSizingMenu, this)
                .addIsVisibleSupplier((menu, entry) -> this.settings.isAdvancedSizingSupported())
                .setStackable(true)
                .setIcon(MaterialIcons.STRAIGHTEN);

        this.rightClickMenu.addSeparatorEntry("separator_after_advanced_sizing_positioning").setStackable(true);

        this.element.stretchX.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .addIsVisibleSupplier((menu, entry) -> this.settings.isStretchable())
                .setStackable(true)
                .addIsActiveSupplier((menu, entry) -> this.element.advancedWidth.getInteger() == Integer.MIN_VALUE)
                .setIcon(MaterialIcons.SWAP_HORIZ);

        this.element.stretchY.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                .addIsVisibleSupplier((menu, entry) -> this.settings.isStretchable())
                .setStackable(true)
                .addIsActiveSupplier((menu, entry) -> this.element.advancedHeight.getInteger() == Integer.MIN_VALUE)
                .setIcon(MaterialIcons.SWAP_VERT);

        this.rightClickMenu.addSeparatorEntry("separator_after_stretch_xy").setStackable(true);

        if (this.settings.isLoadingRequirementsEnabled()) {

            this.rightClickMenu.addClickableEntry("loading_requirements", Component.translatable("fancymenu.requirements.elements.loading_requirements"), (menu, entry) ->
                    {
                        if (!entry.getStackMeta().isPartOfStack()) {
                            ManageRequirementsScreen s = new ManageRequirementsScreen(this.element.requirementContainer.copy(false), (call) -> {
                                if (call != null) {
                                    this.editor.history.saveSnapshot();
                                    this.element.requirementContainer = call;
                                }
                            });
                            menu.closeMenuChain();
                            ManageRequirementsScreen.openInWindow(s);
                        } else if (entry.getStackMeta().isFirstInStack()) {
                            List<E> selectedElements = this.getFilteredSelectedElementList(element -> element.settings.isLoadingRequirementsEnabled());
                            List<RequirementContainer> containers = ObjectUtils.getOfAll(RequirementContainer.class, selectedElements, consumes -> consumes.element.requirementContainer);
                            RequirementContainer containerToUseInManager = new RequirementContainer();
                            boolean allEqual = ListUtils.allInListEqual(containers);
                            if (allEqual) {
                                containerToUseInManager = containers.get(0).copy(true);
                            }
                            ManageRequirementsScreen s = new ManageRequirementsScreen(containerToUseInManager, (call) -> {
                                if (call != null) {
                                    this.editor.history.saveSnapshot();
                                    for (AbstractEditorElement<?, ?> e : selectedElements) {
                                        e.element.requirementContainer = call.copy(true);
                                    }
                                }
                            });
                            if (allEqual) {
                                menu.closeMenuChain();
                                ManageRequirementsScreen.openInWindow(s);
                            } else {
                                Dialogs.openMessageWithCallback(Component.translatable("fancymenu.elements.multiselect.loading_requirements.warning.override"), MessageDialogStyle.WARNING, call -> {
                                    if (call) {
                                        menu.closeMenuChain();
                                        ManageRequirementsScreen.openInWindow(s);
                                    }
                                });
                            }
                        }
                    })
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.requirements.elements.loading_requirements.desc")))
                    .setStackable(true)
                    .setIcon(MaterialIcons.CHECKLIST);

        }

        this.addToggleContextMenuEntryTo(this.rightClickMenu, "load_once_per_session", this.selfClass(),
                        consumes -> consumes.element.loadOncePerSession,
                        (element1, aBoolean) -> element1.element.loadOncePerSession = aBoolean,
                        "fancymenu.elements.element.load_once_per_session")
                .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.element.load_once_per_session.desc")))
                .setIcon(MaterialIcons.HISTORY)
                .setStackable(true);

        this.rightClickMenu.addSeparatorEntry("separator_5");

        if (this.settings.isOrderable()) {

            this.rightClickMenu.addClickableEntry("move_up_element", Component.translatable("fancymenu.editor.object.moveup"),
                            (menu, entry) -> {
                                this.editor.moveLayerUp(this);
                                this.editor.layoutEditorWidgets.forEach(widget -> widget.editorElementOrderChanged(this, true));
                            })
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.object.moveup.desc")))
                    .addIsActiveSupplier((menu, entry) -> this.editor.canMoveLayerUp(this))
                    .setIcon(MaterialIcons.ARROW_UPWARD);

            this.rightClickMenu.addClickableEntry("move_down_element", Component.translatable("fancymenu.editor.object.movedown"),
                            (menu, entry) -> {
                                this.editor.moveLayerDown(this);
                                this.editor.layoutEditorWidgets.forEach(widget -> widget.editorElementOrderChanged(this, false));
                            })
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.editor.object.movedown.desc")))
                    .addIsActiveSupplier((menu, entry) -> this.editor.canMoveLayerDown(this))
                    .setIcon(MaterialIcons.ARROW_DOWNWARD);

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
                    .setIcon(MaterialIcons.CONTENT_COPY);

        }

        if (this.settings.isDestroyable()) {

            this.rightClickMenu.addClickableEntry("delete_element", Component.translatable("fancymenu.elements.delete"), (menu, entry) ->
                    {
                        this.editor.history.saveSnapshot();
                        for (AbstractEditorElement<?, ?> e : this.editor.getSelectedElements()) {
                            e.deleteElement();
                        }
                        menu.closeMenu();
                    })
                    .setStackable(true)
                    .setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.delete"))
                    .setIcon(MaterialIcons.DELETE);

        }

        this.rightClickMenu.addSeparatorEntry("separator_7").setStackable(true);

        if (this.settings.isDelayable()) {

            ContextMenu appearanceDelayMenu = new ContextMenu();
            this.rightClickMenu.addSubMenuEntry("appearance_delay", Component.translatable("fancymenu.element.general.appearance_delay"), appearanceDelayMenu)
                    .setStackable(true)
                    .setIcon(MaterialIcons.TIMER);

            this.addGenericCycleContextMenuEntryTo(appearanceDelayMenu, "appearance_delay_type",
                            ListUtils.of(AbstractElement.AppearanceDelay.NO_DELAY, AbstractElement.AppearanceDelay.FIRST_TIME, AbstractElement.AppearanceDelay.EVERY_TIME),
                            consumes -> consumes.settings.isDelayable(),
                            consumes -> consumes.element.appearanceDelay,
                            (element, switcherValue) -> element.element.appearanceDelay = switcherValue,
                            (menu, entry, switcherValue) -> {
                                return Component.translatable("fancymenu.element.general.appearance_delay." + switcherValue.name);
                            })
                    .setStackable(true)
                    .setIcon(MaterialIcons.TIMER);

            Supplier<Boolean> appearanceDelayIsActive = () -> {
                List<AbstractEditorElement<?, ?>> selected = this.editor.getSelectedElements();
                selected.removeIf(e -> !e.settings.isDelayable());
                if (selected.size() > 1) return true;
                for (AbstractEditorElement<?, ?> e : selected) {
                    if (e.element.appearanceDelay == AbstractElement.AppearanceDelay.NO_DELAY) return false;
                }
                return true;
            };

            this.element.appearanceDelaySeconds.buildContextMenuEntryAndAddTo(appearanceDelayMenu, this)
                    .addIsActiveSupplier((menu, entry) -> appearanceDelayIsActive.get())
                    .setStackable(true)
                    .setIcon(MaterialIcons.TIMER);

            ContextMenu disappearanceDelayMenu = new ContextMenu();
            this.rightClickMenu.addSubMenuEntry("disappearance_delay", Component.translatable("fancymenu.element.general.disappearance_delay"), disappearanceDelayMenu)
                    .setStackable(true)
                    .setIcon(MaterialIcons.TIMER);

            this.addGenericCycleContextMenuEntryTo(disappearanceDelayMenu, "disappearance_delay_type",
                            ListUtils.of(AbstractElement.DisappearanceDelay.NO_DELAY, AbstractElement.DisappearanceDelay.FIRST_TIME, AbstractElement.DisappearanceDelay.EVERY_TIME),
                            consumes -> consumes.settings.isDelayable(),
                            consumes -> consumes.element.disappearanceDelay,
                            (element, switcherValue) -> element.element.disappearanceDelay = switcherValue,
                            (menu, entry, switcherValue) -> {
                                return Component.translatable("fancymenu.element.general.disappearance_delay." + switcherValue.name);
                            })
                    .setStackable(true)
                    .setIcon(MaterialIcons.TIMER);

            Supplier<Boolean> disappearanceDelayIsActive = () -> {
                List<AbstractEditorElement<?, ?>> selected = this.editor.getSelectedElements();
                selected.removeIf(e -> !e.settings.isDelayable());
                if (selected.size() > 1) return true;
                for (AbstractEditorElement<?, ?> e : selected) {
                    if (e.element.disappearanceDelay == AbstractElement.DisappearanceDelay.NO_DELAY) return false;
                }
                return true;
            };

            this.element.disappearanceDelaySeconds.buildContextMenuEntryAndAddTo(disappearanceDelayMenu, this)
                    .addIsActiveSupplier((menu, entry) -> disappearanceDelayIsActive.get())
                    .setStackable(true)
                    .setIcon(MaterialIcons.TIMER);

        }

        if (this.settings.isFadeable()) {

            ContextMenu fadingMenu = new ContextMenu();
            this.rightClickMenu.addSubMenuEntry("fading_in_out", Component.translatable("fancymenu.element.fading"), fadingMenu)
                    .setStackable(true)
                    .setIcon(MaterialIcons.TRANSITION_FADE);

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
            ).setStackable(true)
                    .setIcon(MaterialIcons.TRANSITION_FADE);

            this.element.fadeInSpeed.buildContextMenuEntryAndAddTo(fadingMenu, this)
                    .setStackable(true)
                    .setIcon(MaterialIcons.SPEED);

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
                    .setIcon(MaterialIcons.TRANSITION_FADE)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.fading.fade_out.desc")));

            this.element.fadeOutSpeed.buildContextMenuEntryAndAddTo(fadingMenu, this)
                    .setStackable(true)
                    .setIcon(MaterialIcons.SPEED);

        }

        if (this.settings.isOpacityChangeable()) {

            this.element.baseOpacity.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                    .setStackable(true)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.base_opacity.desc")))
                    .setIcon(MaterialIcons.OPACITY);

        }

        if (this.settings.isAutoSizingAllowed()) {

            this.addToggleContextMenuEntryTo(this.rightClickMenu, "auto_sizing", this.selfClass(),
                            consumes -> consumes.element.autoSizing,
                            (abstractEditorElement, aBoolean) -> {
                                abstractEditorElement.element.setAutoSizingBaseWidthAndHeight();
                                abstractEditorElement.element.autoSizing = aBoolean;
                                abstractEditorElement.element.updateAutoSizing(true);
                            },
                            "fancymenu.element.auto_sizing")
                    .setStackable(true)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.auto_sizing.desc")))
                    .setIcon(MaterialIcons.STRAIGHTEN);

        }

        if (this.settings.isStickyAnchorAllowed()) {

            this.addToggleContextMenuEntryTo(this.rightClickMenu, "sticky_anchor", this.selfClass(),
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
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines(!this.element.stayOnScreen ? "fancymenu.element.sticky_anchor.desc" : "fancymenu.element.sticky_anchor.desc.disable_stay_on_screen")))
                    .setIcon(MaterialIcons.ANCHOR)
                    .addIsActiveSupplier((menu, entry) -> !this.element.stayOnScreen);

        }

        if (this.element.supportsRotation()) {

            this.rightClickMenu.addSeparatorEntry("separator_before_rotation").setStackable(true);

            this.addToggleContextMenuEntryTo(this.rightClickMenu, "advanced_rotation_mode", this.selfClass(),
                            consumes -> consumes.element.advancedRotationMode,
                    (abstractEditorElement, aBoolean) -> abstractEditorElement.element.advancedRotationMode = aBoolean,
                    "fancymenu.element.rotation.advanced_mode")
                    .setStackable(false)
                    .setIcon(MaterialIcons.ROTATE_RIGHT)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.rotation.advanced_mode.desc")));

            this.element.rotationDegrees.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                    .setStackable(false)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.rotation.degrees.desc")))
                    .setIcon(MaterialIcons.ROTATE_RIGHT)
                    .addIsVisibleSupplier((menu, entry) -> !this.element.advancedRotationMode);

            this.element.advancedRotationDegrees.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                    .setStackable(false)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.rotation.degrees.desc")))
                    .setIcon(MaterialIcons.ROTATE_RIGHT)
                    .addIsVisibleSupplier((menu, entry) -> this.element.advancedRotationMode);

        }

        if (this.element.supportsTilting()) {

            this.rightClickMenu.addSeparatorEntry("separator_before_tilting").setStackable(true);

            this.addToggleContextMenuEntryTo(this.rightClickMenu, "advanced_vertical_tilt_mode", this.selfClass(),
                            consumes -> consumes.element.advancedVerticalTiltMode,
                    (abstractEditorElement, aBoolean) -> abstractEditorElement.element.advancedVerticalTiltMode = aBoolean,
                    "fancymenu.element.tilt.vertical.advanced_mode")
                    .setStackable(false)
                    .setIcon(MaterialIcons.FILTER_TILT_SHIFT)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.tilt.vertical.advanced_mode.desc")));

            this.element.verticalTiltDegrees.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                    .setStackable(false)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.tilt.vertical.degrees.desc")))
                    .setIcon(MaterialIcons.FILTER_TILT_SHIFT)
                    .addIsVisibleSupplier((menu, entry) -> !this.element.advancedVerticalTiltMode);

            this.element.advancedVerticalTiltDegrees.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                    .setStackable(false)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.tilt.vertical.degrees.desc")))
                    .setIcon(MaterialIcons.FILTER_TILT_SHIFT)
                    .addIsVisibleSupplier((menu, entry) -> this.element.advancedVerticalTiltMode);

            this.addToggleContextMenuEntryTo(this.rightClickMenu, "advanced_horizontal_tilt_mode", this.selfClass(),
                            consumes -> consumes.element.advancedHorizontalTiltMode,
                    (abstractEditorElement, aBoolean) -> abstractEditorElement.element.advancedHorizontalTiltMode = aBoolean,
                    "fancymenu.element.tilt.horizontal.advanced_mode")
                    .setStackable(false)
                    .setIcon(MaterialIcons.FILTER_TILT_SHIFT)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.tilt.horizontal.advanced_mode.desc")));

            this.element.horizontalTiltDegrees.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                    .setStackable(false)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.tilt.horizontal.degrees.desc")))
                    .setIcon(MaterialIcons.FILTER_TILT_SHIFT)
                    .addIsVisibleSupplier((menu, entry) -> !this.element.advancedHorizontalTiltMode);

            this.element.advancedHorizontalTiltDegrees.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                    .setStackable(false)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.element.tilt.horizontal.degrees.desc")))
                    .setIcon(MaterialIcons.FILTER_TILT_SHIFT)
                    .addIsVisibleSupplier((menu, entry) -> this.element.advancedHorizontalTiltMode);

        }

        if (this.settings.isParallaxAllowed()) {

            this.rightClickMenu.addSeparatorEntry("separator_before_parallax").setStackable(true);

            this.addToggleContextMenuEntryTo(this.rightClickMenu, "enable_parallax", this.selfClass(),
                            consumes -> consumes.element.enableParallax,
                    (abstractEditorElement, aBoolean) -> abstractEditorElement.element.enableParallax = aBoolean,
                    "fancymenu.elements.parallax")
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.elements.parallax.desc")))
                    .setIcon(MaterialIcons._3D);

            this.element.parallaxIntensityX.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.elements.parallax.intensity_x.desc")))
                    .setIcon(MaterialIcons.SPLITSCREEN_LANDSCAPE);

            this.element.parallaxIntensityY.buildContextMenuEntryAndAddTo(this.rightClickMenu, this)
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.elements.parallax.intensity_y.desc")))
                    .setIcon(MaterialIcons.SPLITSCREEN_PORTRAIT);

            this.addToggleContextMenuEntryTo(this.rightClickMenu, "invert_parallax", this.selfClass(),
                            consumes -> consumes.element.invertParallax,
                    (abstractEditorElement, aBoolean) -> abstractEditorElement.element.invertParallax = aBoolean,
                    "fancymenu.elements.parallax.invert")
                    .setTooltipSupplier((menu, entry) -> UITooltip.of(Component.translatable("fancymenu.elements.parallax.invert.desc")))
                    .setIcon(MaterialIcons.SWAP_HORIZ);

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
        this.updateResizeHoverState(mouseX, mouseY);

        this.element.renderInternal(graphics, mouseX, mouseY, partial);

        this.renderDraggingNotAllowedOverlay(graphics);

        this.renderDeprecatedIndicator(graphics);

        //Update cursor
        ResizeGrabberType hoveredResizeType = this.getHoveredResizeType();
        if (hoveredResizeType != null) CursorHandler.setClientTickCursor(this.getResizeCursor(hoveredResizeType));

        RotationGrabber hoveredRotationGrabber = this.getHoveredRotationGrabber();
        if (hoveredRotationGrabber != null) CursorHandler.setClientTickCursor(CursorHandler.CURSOR_RESIZE_ALL);

        VerticalTiltGrabber hoveredVerticalTiltGrabber = this.getHoveredVerticalTiltGrabber();
        if (hoveredVerticalTiltGrabber != null) CursorHandler.setClientTickCursor(CursorHandler.CURSOR_RESIZE_VERTICAL);

        HorizontalTiltGrabber hoveredHorizontalTiltGrabber = this.getHoveredHorizontalTiltGrabber();
        if (hoveredHorizontalTiltGrabber != null) CursorHandler.setClientTickCursor(CursorHandler.CURSOR_RESIZE_HORIZONTAL);

        this.renderBorder(graphics, mouseX, mouseY, partial);

    }

    protected void tick() {
        if (!this.element.advancedWidth.isDefault() || !this.element.advancedHeight.isDefault() && !this.topLeftDisplay.hasLine("advanced_sizing_enabled")) {
            this.topLeftDisplay.addLine("advanced_sizing_enabled", () -> Component.translatable("fancymenu.elements.advanced_sizing.enabled_notification"));
        }
        if (this.element.advancedWidth.isDefault() && this.element.advancedHeight.isDefault() && this.topLeftDisplay.hasLine("advanced_sizing_enabled")) {
            this.topLeftDisplay.removeLine("advanced_sizing_enabled");
        }
        if (!this.element.advancedX.isDefault() || !this.element.advancedY.isDefault() && !this.topLeftDisplay.hasLine("advanced_positioning_enabled")) {
            this.topLeftDisplay.addLine("advanced_positioning_enabled", () -> Component.translatable("fancymenu.elements.advanced_positioning.enabled_notification"));
        }
        if (this.element.advancedX.isDefault() && this.element.advancedY.isDefault() && this.topLeftDisplay.hasLine("advanced_positioning_enabled")) {
            this.topLeftDisplay.removeLine("advanced_positioning_enabled");
        }
        // Handle rotation display
        boolean shouldShowRotation = this.element.supportsRotation() && this.element.getRotationDegrees() != 0.0F;
        if (shouldShowRotation && !this.topLeftDisplay.hasLine("rotation")) {
            // Insert rotation line after width line
            this.topLeftDisplay.addLine("rotation", () -> Component.translatable("fancymenu.element.border_display.rotation", String.format("%.1f", this.element.getRotationDegrees())));
        } else if (!shouldShowRotation && this.topLeftDisplay.hasLine("rotation")) {
            this.topLeftDisplay.removeLine("rotation");
        }
        // Handle tilt display
        boolean shouldShowVerticalTilt = this.element.supportsTilting() && this.element.getVerticalTiltDegrees() != 0.0F;
        if (shouldShowVerticalTilt && !this.topLeftDisplay.hasLine("vertical_tilt")) {
            this.topLeftDisplay.addLine("vertical_tilt", () -> Component.translatable("fancymenu.element.border_display.vertical_tilt", String.format("%.1f", this.element.getVerticalTiltDegrees())));
        } else if (!shouldShowVerticalTilt && this.topLeftDisplay.hasLine("vertical_tilt")) {
            this.topLeftDisplay.removeLine("vertical_tilt");
        }
        boolean shouldShowHorizontalTilt = this.element.supportsTilting() && this.element.getHorizontalTiltDegrees() != 0.0F;
        if (shouldShowHorizontalTilt && !this.topLeftDisplay.hasLine("horizontal_tilt")) {
            this.topLeftDisplay.addLine("horizontal_tilt", () -> Component.translatable("fancymenu.element.border_display.horizontal_tilt", String.format("%.1f", this.element.getHorizontalTiltDegrees())));
        } else if (!shouldShowHorizontalTilt && this.topLeftDisplay.hasLine("horizontal_tilt")) {
            this.topLeftDisplay.removeLine("horizontal_tilt");
        }
    }

    protected void renderDraggingNotAllowedOverlay(GuiGraphics graphics) {
        if (this.renderMovingNotAllowedTime >= System.currentTimeMillis()) {
            RenderSystem.enableBlend();
            graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), UIBase.getUITheme().layout_editor_element_dragging_not_allowed_color.getColorInt());
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
        if (this.element.getBuilder().isDeprecated()) {
            RenderSystem.enableBlend();
            AspectRatio ratio = new AspectRatio(32, 32);
            int[] size = ratio.getAspectRatioSizeByMaximumSize(this.getWidth() / 3, this.getHeight() / 3);
            int texW = size[0];
            int texH = size[1];
            int texX = this.getX() + this.getWidth() - texW;
            int texY = this.getY();
            UIBase.setShaderColor(graphics, UIBase.getUITheme().warning_color);
            graphics.blit(DEPRECATED_WARNING_TEXTURE, texX, texY, 0.0F, 0.0F, texW, texH, texW, texH);
            RenderingUtils.resetShaderColor(graphics);
        }
    }

    protected void renderBorder(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        RenderSystem.disableDepthTest();
        RenderingUtils.setDepthTestLocked(true);

        if (((this.editor.getTopHoveredElement() == this) && !this.editor.isUserNavigatingInRightClickMenu() && !this.editor.isUserNavigatingInElementMenu()) || this.isSelected() || this.isMultiSelected()) {

            //TOP
            graphics.fill(this.getX() + 1, this.getY(), this.getX() + this.getWidth() - 1, this.getY() + 1, BORDER_COLOR.get(this));
            //BOTTOM
            graphics.fill(this.getX() + 1, this.getY() + this.getHeight() - 1, this.getX() + this.getWidth() - 1, this.getY() + this.getHeight(), BORDER_COLOR.get(this));
            //LEFT
            graphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.getHeight(), BORDER_COLOR.get(this));
            //RIGHT
            graphics.fill(this.getX() + this.getWidth() - 1, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), BORDER_COLOR.get(this));

            this.renderResizeIndicators(graphics);

            // Render rotation circle and grabber
            if (this.isSelected() && this.element.supportsRotation() && !this.element.advancedRotationMode && !this.isMultiSelected()) {
                this.renderRotationControls(graphics, mouseX, mouseY, partial);
            }

            // Render tilt lines and grabbers
            if (this.isSelected() && this.element.supportsTilting() && !this.isMultiSelected()) {
                this.renderTiltControls(graphics, mouseX, mouseY, partial);
            }

        }

        if (this.isSelected()) {
            this.topLeftDisplay.render(graphics, mouseX, mouseY, partial);
            this.bottomRightDisplay.render(graphics, mouseX, mouseY, partial);
        }

        RenderingUtils.setDepthTestLocked(false);
        RenderSystem.enableDepthTest();

    }

    protected void renderRotationControls(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!FancyMenu.getOptions().enableElementRotationControls.getValue()) return;

        float centerX = this.getX() + (this.getWidth() / 2.0F);
        float centerY = this.getY() + (this.getHeight() / 2.0F);

        // Calculate radius - slightly larger than the element's diagonal
        float halfWidth = this.getWidth() / 2.0F;
        float halfHeight = this.getHeight() / 2.0F;
        float radius = (float)Math.sqrt(halfWidth * halfWidth + halfHeight * halfHeight) + 8; // 8 pixels padding

        int circleColor = UIBase.getUITheme().layout_editor_element_border_rotation_controls_color.getColorIntWithAlpha(ROTATION_CONTROLS_ALPHA.get(this));

        float circleDiameter = radius * 2.0F;
        float circleX = centerX - radius;
        float circleY = centerY - radius;
        float rotationAngleRad = (float) -Math.toRadians(this.element.getRotationDegrees() - 90.0F);
        float arcHalfRadians = (float) Math.toRadians(15.0F);
        SmoothCircleRenderer.renderSmoothCircleBorderArc(
                graphics,
                circleX,
                circleY,
                circleDiameter,
                circleDiameter,
                1.0F,
                2.0F,
                rotationAngleRad - arcHalfRadians,
                rotationAngleRad + arcHalfRadians,
                true,
                circleColor,
                partial
        );

        this.rotationGrabber.render(graphics, mouseX, mouseY, partial);

    }

    protected void renderTiltControls(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!FancyMenu.getOptions().enableElementTiltingControls.getValue()) return;

        // Render vertical tilt line
        if (!this.element.advancedVerticalTiltMode) {
            // Vertical line (for vertical tilt) - offset to the right with padding
            int verticalLineX = this.getVerticalTiltLineX();
            int verticalLineTop = this.getVerticalTiltLineTop();
            int verticalLineBottom = this.getVerticalTiltLineBottom();
            graphics.fill(verticalLineX, verticalLineTop, verticalLineX + 1, verticalLineBottom, UIBase.getUITheme().layout_editor_element_border_vertical_tilting_controls_color.getColorIntWithAlpha(VERTICAL_TILT_CONTROLS_ALPHA.get(this)));
        }

        if (!this.element.advancedHorizontalTiltMode) {
            // Horizontal line (for horizontal tilt) - offset below with padding
            int horizontalLineY = this.getHorizontalTiltLineY();
            int horizontalLineLeft = this.getHorizontalTiltLineLeft();
            int horizontalLineRight = this.getHorizontalTiltLineRight();
            graphics.fill(horizontalLineLeft, horizontalLineY, horizontalLineRight, horizontalLineY + 1, UIBase.getUITheme().layout_editor_element_border_horizontal_tilting_controls_color.getColorIntWithAlpha(HORIZONTAL_TILT_CONTROLS_ALPHA.get(this)));
        }

        // Render tilt grabbers
        if (!this.element.advancedVerticalTiltMode) {
            this.verticalTiltGrabber.render(graphics, mouseX, mouseY, partial);
        }
        if (!this.element.advancedVerticalTiltMode) {
            this.horizontalTiltGrabber.render(graphics, mouseX, mouseY, partial);
        }

    }

    protected int getVerticalTiltLineX() {
        return this.getX() + this.getWidth() + TILT_CONTROLS_PADDING;
    }

    protected int getVerticalTiltLineTop() {
        return this.getY() - TILT_CONTROLS_LINE_EXTENSION;
    }

    protected int getVerticalTiltLineBottom() {
        return this.getY() + this.getHeight() + TILT_CONTROLS_LINE_EXTENSION;
    }

    protected int getHorizontalTiltLineY() {
        return this.getY() + this.getHeight() + TILT_CONTROLS_PADDING;
    }

    protected int getHorizontalTiltLineLeft() {
        return this.getX() - TILT_CONTROLS_LINE_EXTENSION;
    }

    protected int getHorizontalTiltLineRight() {
        return this.getX() + this.getWidth() + TILT_CONTROLS_LINE_EXTENSION;
    }

    protected void renderResizeIndicators(@NotNull GuiGraphics graphics) {
        if (!this.isSelected() || this.isMultiSelected() || !this.settings.isResizeable()) {
            return;
        }
        float radius = UIBase.getInterfaceCornerRoundingRadius() > 0.0F ? RESIZE_INDICATOR_CORNER_RADIUS : 0.0F;
        for (ResizeGrabberType type : ResizeGrabberType.values()) {
            if (!this.isResizeIndicatorEnabled(type)) {
                continue;
            }
            int x = this.getResizeIndicatorX(type, RESIZE_INDICATOR_SIZE);
            int y = this.getResizeIndicatorY(type, RESIZE_INDICATOR_SIZE);
            SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                    graphics,
                    x,
                    y,
                    RESIZE_INDICATOR_SIZE,
                    RESIZE_INDICATOR_SIZE,
                    radius,
                    radius,
                    radius,
                    radius,
                    BORDER_COLOR.get(this),
                    1.0F
            );
        }
    }

    protected boolean isResizeIndicatorEnabled(@NotNull ResizeGrabberType type) {
        boolean resizeLeft = (type == ResizeGrabberType.LEFT) || (type == ResizeGrabberType.TOP_LEFT) || (type == ResizeGrabberType.BOTTOM_LEFT);
        boolean resizeRight = (type == ResizeGrabberType.RIGHT) || (type == ResizeGrabberType.TOP_RIGHT) || (type == ResizeGrabberType.BOTTOM_RIGHT);
        boolean resizeTop = (type == ResizeGrabberType.TOP) || (type == ResizeGrabberType.TOP_LEFT) || (type == ResizeGrabberType.TOP_RIGHT);
        boolean resizeBottom = (type == ResizeGrabberType.BOTTOM) || (type == ResizeGrabberType.BOTTOM_LEFT) || (type == ResizeGrabberType.BOTTOM_RIGHT);
        boolean resizeHorizontal = resizeLeft || resizeRight;
        boolean resizeVertical = resizeTop || resizeBottom;
        boolean resizeCorner = resizeHorizontal && resizeVertical;
        boolean canResizeX = this.settings.isResizeableX() && this.element.advancedWidth.isDefault();
        boolean canResizeY = this.settings.isResizeableY() && this.element.advancedHeight.isDefault();
        if (resizeLeft && !this.element.advancedX.isDefault()) return false;
        if (resizeTop && !this.element.advancedY.isDefault()) return false;
        if (resizeCorner) {
            return canResizeX && canResizeY;
        }
        if (resizeVertical) {
            return canResizeY;
        }
        if (resizeHorizontal) {
            return canResizeX;
        }
        return false;
    }

    protected int getResizeIndicatorX(@NotNull ResizeGrabberType type, int size) {
        int x = this.getX();
        boolean resizeLeft = (type == ResizeGrabberType.LEFT) || (type == ResizeGrabberType.TOP_LEFT) || (type == ResizeGrabberType.BOTTOM_LEFT);
        boolean resizeRight = (type == ResizeGrabberType.RIGHT) || (type == ResizeGrabberType.TOP_RIGHT) || (type == ResizeGrabberType.BOTTOM_RIGHT);
        if (resizeLeft) {
            return x - (size / 2);
        }
        if (resizeRight) {
            return x + this.getWidth() - 1 - (size / 2);
        }
        return x + (this.getWidth() / 2) - (size / 2);
    }

    protected int getResizeIndicatorY(@NotNull ResizeGrabberType type, int size) {
        int y = this.getY();
        boolean resizeTop = (type == ResizeGrabberType.TOP) || (type == ResizeGrabberType.TOP_LEFT) || (type == ResizeGrabberType.TOP_RIGHT);
        boolean resizeBottom = (type == ResizeGrabberType.BOTTOM) || (type == ResizeGrabberType.BOTTOM_LEFT) || (type == ResizeGrabberType.BOTTOM_RIGHT);
        if (resizeTop) {
            return y - (size / 2);
        }
        if (resizeBottom) {
            return y + this.getHeight() - 1 - (size / 2);
        }
        return y + (this.getHeight() / 2) - (size / 2);
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
            AbstractEditorElement<?, ?> ee = this.editor.getElementByInstanceIdentifier(Objects.requireNonNull(this.element.getAnchorPointElementIdentifier()));
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
        this.activeResizeGrabberType = null;
        this.hoveredResizeGrabberType = null;
        this.rotationGrabberActive = false;
        this.verticalTiltGrabberActive = false;
        this.horizontalTiltGrabberActive = false;
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
        AbstractEditorElement<?, ?> parent = this.editor.getElementByInstanceIdentifier(this.element.getAnchorPointElementIdentifier());
        if (parent == null) return false;
        return (parent.isSelected() || parent.isMultiSelected());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (this.element.layerHiddenInEditor) return false;
        this.updateResizeHoverState((int) mouseX, (int) mouseY);

        if (!this.isSelected()) {
            return false;
        }
        if (button == 0) {
            if (!this.rightClickMenu.isUserNavigatingInMenu()) {
                this.activeResizeGrabberType = !this.isMultiSelected() ? this.getResizeGrabberTypeAt((int) mouseX, (int) mouseY) : null;
                this.rotationGrabberActive = !this.isMultiSelected() && this.getHoveredRotationGrabber() != null;
                this.verticalTiltGrabberActive = !this.isMultiSelected() && this.getHoveredVerticalTiltGrabber() != null;
                this.horizontalTiltGrabberActive = !this.isMultiSelected() && this.getHoveredHorizontalTiltGrabber() != null;

                if (this.isHovered() || (this.isMultiSelected() && !this.editor.getHoveredElements().isEmpty()) || this.isGettingResized() || this.isGettingRotated() || this.isGettingTilted()) {
                    this.leftMouseDown = true;
                    this.updateLeftMouseDownCachedValues((int) mouseX, (int) mouseY);
                    this.resizeAspectRatio = new AspectRatio(this.getWidth(), this.getHeight());
                    if (this.rotationGrabberActive) {
                        this.preRotationSnapshot = this.editor.history.createSnapshot();
                        this.rotationStartAngle = this.element.rotationDegrees.getFloat();
                        // Calculate initial mouse angle relative to element center
                        float centerX = this.getX() + (this.getWidth() / 2.0F);
                        float centerY = this.getY() + (this.getHeight() / 2.0F);
                        this.rotationStartMouseAngle = Math.toDegrees(Math.atan2(mouseY - centerY, mouseX - centerX));
                    }

                    if (this.verticalTiltGrabberActive) {
                        this.preTiltSnapshot = this.editor.history.createSnapshot();
                        this.verticalTiltStartAngle = this.element.verticalTiltDegrees.getFloat();
                        this.verticalTiltStartMouseY = mouseY;
                    }

                    if (this.horizontalTiltGrabberActive) {
                        this.preTiltSnapshot = this.editor.history.createSnapshot();
                        this.horizontalTiltStartAngle = this.element.horizontalTiltDegrees.getFloat();
                        this.horizontalTiltStartMouseX = mouseX;
                    }

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
            this.activeResizeGrabberType = null;
            if (this.isGettingRotated() && (this.preRotationSnapshot != null)) {
                if (this.rotationStartAngle != this.element.rotationDegrees.getFloat()) {
                    this.editor.history.saveSnapshot(this.preRotationSnapshot);
                }
            }
            if ((this.isGettingVerticalTilted() || this.isGettingHorizontalTilted()) && (this.preTiltSnapshot != null)) {
                if ((this.verticalTiltStartAngle != this.element.verticalTiltDegrees.getFloat()) || (this.horizontalTiltStartAngle != this.element.horizontalTiltDegrees.getFloat())) {
                    this.editor.history.saveSnapshot(this.preTiltSnapshot);
                }
            }
            this.preRotationSnapshot = null;
            this.preTiltSnapshot = null;
            this.rotationGrabberActive = false;
            this.verticalTiltGrabberActive = false;
            this.horizontalTiltGrabberActive = false;
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
            if (this.leftMouseDown && this.isGettingRotated()) { // ROTATE ELEMENT
                // Calculate current mouse angle relative to element center
                float centerX = this.getX() + (this.getWidth() / 2.0F);
                float centerY = this.getY() + (this.getHeight() / 2.0F);
                double currentMouseAngle = Math.toDegrees(Math.atan2(mouseY - centerY, mouseX - centerX));

                // Calculate angle difference and apply to rotation
                double angleDiff = currentMouseAngle - this.rotationStartMouseAngle;
                float newRotation = (float)(this.rotationStartAngle + angleDiff);

                // Snap to 45-degree increments if shift is held
                if (Screen.hasShiftDown()) {
                    newRotation = Math.round(newRotation / 45.0F) * 45.0F;
                }

                // Normalize rotation to 0-360 range
                while (newRotation < 0) newRotation += 360;
                while (newRotation >= 360) newRotation -= 360;

                this.element.rotationDegrees.set(this.snapRotationToZero(newRotation));
            } else if (this.leftMouseDown && this.isGettingVerticalTilted()) { // VERTICAL TILT
                // Calculate tilt based on mouse position along the tilt line
                int lineTop = this.getVerticalTiltLineTop();
                int lineBottom = this.getVerticalTiltLineBottom();
                int lineLength = Math.max(1, lineBottom - lineTop);
                double normalized = (mouseY - lineTop) / (double) lineLength;
                normalized = Math.max(0.0D, Math.min(1.0D, normalized));
                float newTilt = (float) ((normalized * 120.0D) - 60.0D);

                // Snap to 15-degree increments if shift is held
                if (Screen.hasShiftDown()) {
                    newTilt = Math.round(newTilt / 15.0F) * 15.0F;
                }

                this.element.verticalTiltDegrees.set(this.snapTiltToZero(newTilt));
            } else if (this.leftMouseDown && this.isGettingHorizontalTilted()) { // HORIZONTAL TILT
                // Calculate tilt based on mouse position along the tilt line
                int lineLeft = this.getHorizontalTiltLineLeft();
                int lineRight = this.getHorizontalTiltLineRight();
                int lineLength = Math.max(1, lineRight - lineLeft);
                double normalized = (mouseX - lineLeft) / (double) lineLength;
                normalized = Math.max(0.0D, Math.min(1.0D, normalized));
                float newTilt = (float) ((normalized * 120.0D) - 60.0D);

                // Snap to 15-degree increments if shift is held
                if (Screen.hasShiftDown()) {
                    newTilt = Math.round(newTilt / 15.0F) * 15.0F;
                }

                this.element.horizontalTiltDegrees.set(this.snapTiltToZero(newTilt));
            } else if (this.leftMouseDown && !this.isGettingResized() && this.movingCrumpleZonePassed) { // MOVE ELEMENT
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

                ResizeGrabberType resizeType = this.activeResizeGrabberType;
                boolean resizeLeft = (resizeType == ResizeGrabberType.LEFT) || (resizeType == ResizeGrabberType.TOP_LEFT) || (resizeType == ResizeGrabberType.BOTTOM_LEFT);
                boolean resizeRight = (resizeType == ResizeGrabberType.RIGHT) || (resizeType == ResizeGrabberType.TOP_RIGHT) || (resizeType == ResizeGrabberType.BOTTOM_RIGHT);
                boolean resizeTop = (resizeType == ResizeGrabberType.TOP) || (resizeType == ResizeGrabberType.TOP_LEFT) || (resizeType == ResizeGrabberType.TOP_RIGHT);
                boolean resizeBottom = (resizeType == ResizeGrabberType.BOTTOM) || (resizeType == ResizeGrabberType.BOTTOM_LEFT) || (resizeType == ResizeGrabberType.BOTTOM_RIGHT);
                boolean resizeHorizontal = resizeLeft || resizeRight;
                boolean resizeVertical = resizeTop || resizeBottom;
                boolean resizeCorner = resizeHorizontal && resizeVertical;

                int newWidth = this.leftMouseDownBaseWidth;
                int newHeight = this.leftMouseDownBaseHeight;

                if (resizeHorizontal) {
                    newWidth = resizeLeft ? (this.leftMouseDownBaseWidth - diffX) : (this.leftMouseDownBaseWidth + diffX);
                }
                if (resizeVertical) {
                    newHeight = resizeTop ? (this.leftMouseDownBaseHeight - diffY) : (this.leftMouseDownBaseHeight + diffY);
                }

                if (resizeCorner && Screen.hasShiftDown()) {
                    if (Math.abs(diffX) >= Math.abs(diffY)) {
                        newWidth = resizeLeft ? (this.leftMouseDownBaseWidth - diffX) : (this.leftMouseDownBaseWidth + diffX);
                        newHeight = this.resizeAspectRatio.getAspectRatioHeight(newWidth);
                    } else {
                        newHeight = resizeTop ? (this.leftMouseDownBaseHeight - diffY) : (this.leftMouseDownBaseHeight + diffY);
                        newWidth = this.resizeAspectRatio.getAspectRatioWidth(newHeight);
                    }
                }

                if (resizeHorizontal && newWidth >= 2 && (!resizeCorner || !Screen.hasShiftDown() || newHeight >= 2)) {
                    this.element.autoSizingWidth = 0;
                    this.element.autoSizingHeight = 0;
                    int cachedOldOffsetX = this.element.posOffsetX;
                    int cachedOldPosX = this.element.getAbsoluteX();
                    int cachedOldWidth = this.element.getAbsoluteWidth();
                    int usedDiffX = resizeLeft ? (this.leftMouseDownBaseWidth - newWidth) : (newWidth - this.leftMouseDownBaseWidth);
                    this.element.baseWidth = newWidth;
                    this.element.posOffsetX = this.leftMouseDownBaseX + this.element.anchorPoint.getResizePositionOffsetX(this.element, usedDiffX, resizeType);
                    if (this.element.stickyAnchor) {
                        this.element.posOffsetX += this.element.anchorPoint.getStickyResizePositionCorrectionX(this.element, usedDiffX, cachedOldOffsetX, this.element.posOffsetX, cachedOldPosX, this.element.getAbsoluteX(), cachedOldWidth, this.element.getAbsoluteWidth(), resizeType);
                    }
                    if (Screen.hasShiftDown() && !resizeCorner) {
                        this.element.baseHeight = this.resizeAspectRatio.getAspectRatioHeight(this.element.baseWidth);
                    }
                }
                if (resizeVertical && newHeight >= 2 && (!resizeCorner || !Screen.hasShiftDown() || newWidth >= 2)) {
                    this.element.autoSizingWidth = 0;
                    this.element.autoSizingHeight = 0;
                    int cachedOldOffsetY = this.element.posOffsetY;
                    int cachedOldPosY = this.element.getAbsoluteY();
                    int cachedOldHeight = this.element.getAbsoluteHeight();
                    int usedDiffY = resizeTop ? (this.leftMouseDownBaseHeight - newHeight) : (newHeight - this.leftMouseDownBaseHeight);
                    this.element.baseHeight = newHeight;
                    this.element.posOffsetY = this.leftMouseDownBaseY + this.element.anchorPoint.getResizePositionOffsetY(this.element, usedDiffY, resizeType);
                    if (this.element.stickyAnchor) {
                        this.element.posOffsetY += this.element.anchorPoint.getStickyResizePositionCorrectionY(this.element, usedDiffY, cachedOldOffsetY, this.element.posOffsetY, cachedOldPosY, this.element.getAbsoluteY(), cachedOldHeight, this.element.baseHeight, resizeType);
                    }
                    if (Screen.hasShiftDown() && !resizeCorner) {
                        this.element.baseWidth = this.resizeAspectRatio.getAspectRatioWidth(this.element.baseHeight);
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
        return this.hovered || this.rightClickMenu.isUserNavigatingInMenu() || (this.getHoveredResizeType() != null) || (this.getHoveredRotationGrabber() != null) || (this.getHoveredVerticalTiltGrabber() != null) || (this.getHoveredHorizontalTiltGrabber() != null);
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
        return this.activeResizeGrabberType != null;
    }

    public boolean isGettingRotated() {
        if (!this.element.supportsRotation()) {
            return false;
        }
        return this.rotationGrabberActive;
    }

    public boolean isGettingVerticalTilted() {
        if (!this.element.supportsTilting()) {
            return false;
        }
        return this.verticalTiltGrabberActive;
    }

    public boolean isGettingHorizontalTilted() {
        if (!this.element.supportsTilting()) {
            return false;
        }
        return this.horizontalTiltGrabberActive;
    }

    public boolean isGettingTilted() {
        return this.isGettingVerticalTilted() || this.isGettingHorizontalTilted();
    }

    public boolean isDragged() {
        return this.recentlyMovedByDragging;
    }

    public boolean isPressed() {
        return this.leftMouseDown;
    }

    @Nullable
    public ResizeGrabberType getHoveredResizeType() {
        if (!this.settings.isResizeable()) {
            return null;
        }
        if (this.activeResizeGrabberType != null) {
            return this.activeResizeGrabberType;
        }
        return this.hoveredResizeGrabberType;
    }

    protected void updateResizeHoverState(int mouseX, int mouseY) {
        this.hoveredResizeGrabberType = this.getResizeGrabberTypeAt(mouseX, mouseY);
    }

    @Nullable
    protected ResizeGrabberType getResizeGrabberTypeAt(int mouseX, int mouseY) {
        if (!this.settings.isResizeable()) {
            return null;
        }
        if (!this.isSelected()) {
            return null;
        }
        if (this.isMultiSelected()) {
            return null;
        }
        int left = this.getX();
        int right = left + this.getWidth();
        int top = this.getY();
        int bottom = top + this.getHeight();

        int grabZone = RESIZE_EDGE_GRAB_ZONE;
        if ((mouseX < left - grabZone) || (mouseX > right + grabZone) || (mouseY < top - grabZone) || (mouseY > bottom + grabZone)) {
            return null;
        }

        int distLeft = Math.abs(mouseX - left);
        int distRight = Math.abs(mouseX - right);
        int distTop = Math.abs(mouseY - top);
        int distBottom = Math.abs(mouseY - bottom);

        boolean canResizeX = this.settings.isResizeableX() && this.element.advancedWidth.isDefault();
        boolean canResizeY = this.settings.isResizeableY() && this.element.advancedHeight.isDefault();
        boolean canResizeLeft = canResizeX && this.element.advancedX.isDefault();
        boolean canResizeRight = canResizeX;
        boolean canResizeTop = canResizeY && this.element.advancedY.isDefault();
        boolean canResizeBottom = canResizeY;

        boolean nearLeft = distLeft <= grabZone;
        boolean nearRight = distRight <= grabZone;
        boolean nearTop = distTop <= grabZone;
        boolean nearBottom = distBottom <= grabZone;

        boolean useLeft = nearLeft && canResizeLeft;
        boolean useRight = nearRight && canResizeRight;
        boolean useTop = nearTop && canResizeTop;
        boolean useBottom = nearBottom && canResizeBottom;

        if (useLeft && useRight) {
            useLeft = distLeft <= distRight;
            useRight = !useLeft;
        }
        if (useTop && useBottom) {
            useTop = distTop <= distBottom;
            useBottom = !useTop;
        }

        if (useLeft && useTop) {
            return ResizeGrabberType.TOP_LEFT;
        }
        if (useRight && useTop) {
            return ResizeGrabberType.TOP_RIGHT;
        }
        if (useRight && useBottom) {
            return ResizeGrabberType.BOTTOM_RIGHT;
        }
        if (useLeft && useBottom) {
            return ResizeGrabberType.BOTTOM_LEFT;
        }
        if (useTop) {
            return ResizeGrabberType.TOP;
        }
        if (useBottom) {
            return ResizeGrabberType.BOTTOM;
        }
        if (useLeft) {
            return ResizeGrabberType.LEFT;
        }
        if (useRight) {
            return ResizeGrabberType.RIGHT;
        }
        return null;
    }

    protected float snapTiltToZero(float tiltDegrees) {
        return (Math.abs(tiltDegrees) <= TILT_ROTATION_ZERO_SNAP_DEGREES) ? 0.0F : tiltDegrees;
    }

    protected float snapRotationToZero(float rotationDegrees) {
        if (Math.abs(rotationDegrees) <= TILT_ROTATION_ZERO_SNAP_DEGREES) {
            return 0.0F;
        }
        return (Math.abs(360.0F - rotationDegrees) <= TILT_ROTATION_ZERO_SNAP_DEGREES) ? 0.0F : rotationDegrees;
    }

    protected long getResizeCursor(@NotNull ResizeGrabberType type) {
        if ((type == ResizeGrabberType.TOP_LEFT) || (type == ResizeGrabberType.BOTTOM_RIGHT)) {
            return CursorHandler.CURSOR_RESIZE_NWSE;
        }
        if ((type == ResizeGrabberType.TOP_RIGHT) || (type == ResizeGrabberType.BOTTOM_LEFT)) {
            return CursorHandler.CURSOR_RESIZE_NESW;
        }
        if ((type == ResizeGrabberType.TOP) || (type == ResizeGrabberType.BOTTOM)) {
            return CursorHandler.CURSOR_RESIZE_VERTICAL;
        }
        return CursorHandler.CURSOR_RESIZE_HORIZONTAL;
    }

    @Nullable
    public RotationGrabber getHoveredRotationGrabber() {
        if (!FancyMenu.getOptions().enableElementRotationControls.getValue()) {
            return null;
        }
        if (!this.element.supportsRotation()) {
            return null;
        }
        if (this.element.advancedRotationMode) {
            return null;
        }
        if (this.isMultiSelected()) {
            return null;
        }
        if (this.rotationGrabberActive) {
            return this.rotationGrabber;
        }
        if (this.rotationGrabber.hovered) {
            return this.rotationGrabber;
        }
        return null;
    }

    @Nullable
    public VerticalTiltGrabber getHoveredVerticalTiltGrabber() {
        if (!FancyMenu.getOptions().enableElementTiltingControls.getValue()) {
            return null;
        }
        if (!this.element.supportsTilting()) {
            return null;
        }
        if (this.element.advancedVerticalTiltMode) {
            return null;
        }
        if (this.isMultiSelected()) {
            return null;
        }
        if (this.verticalTiltGrabberActive) {
            return this.verticalTiltGrabber;
        }
        if (this.verticalTiltGrabber.hovered) {
            return this.verticalTiltGrabber;
        }
        return null;
    }

    @Nullable
    public HorizontalTiltGrabber getHoveredHorizontalTiltGrabber() {
        if (!FancyMenu.getOptions().enableElementTiltingControls.getValue()) {
            return null;
        }
        if (!this.element.supportsTilting()) {
            return null;
        }
        if (this.element.advancedHorizontalTiltMode) {
            return null;
        }
        if (this.isMultiSelected()) {
            return null;
        }
        if (this.horizontalTiltGrabberActive) {
            return this.horizontalTiltGrabber;
        }
        if (this.horizontalTiltGrabber.hovered) {
            return this.horizontalTiltGrabber;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected List<E> getFilteredSelectedElementList(@Nullable ConsumingSupplier<E, Boolean> selectedElementsFilter) {
        List<E> filtered = new ArrayList<>();
        Class<?> selfClass = this.getClass();
        for (AbstractEditorElement<?, ?> element : this.editor.getSelectedElements()) {
            if (!selfClass.isInstance(element)) {
                continue;
            }
            E casted = (E) element;
            if (selectedElementsFilter != null && !selectedElementsFilter.get(casted)) {
                continue;
            }
            filtered.add(casted);
        }
        return filtered;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull List<E> getFilteredStackableObjectsList(@Nullable ConsumingSupplier<E, Boolean> filter) {
        List<E> filtered = new ArrayList<>();
        ConsumingSupplier<Object, Boolean> rawFilter = (ConsumingSupplier<Object, Boolean>) (Object) filter;
        List<AbstractEditorElement<?, ?>> rawList = (List<AbstractEditorElement<?, ?>>) filtered;
        for (AbstractEditorElement<?, ?> element : this.editor.getSelectedElements()) {
            if (rawFilter != null && !rawFilter.get(element)) {
                continue;
            }
            rawList.add(element);
        }
        return filtered;
    }

    @Override
    public void saveSnapshot() {
        this.editor.history.saveSnapshot();
    }

    @Override
    public void saveSnapshot(@NotNull Object snapshot) {
        Objects.requireNonNull(snapshot);
        this.editor.history.saveSnapshot((LayoutEditorHistory.Snapshot) snapshot);
    }

    @Override
    public @Nullable Object createSnapshot() {
        return this.editor.history.createSnapshot();
    }

    @Override
    public @NotNull List<ContextMenuBuilder.ContextMenuScreenOpenProcessor> getContextMenuScreenOpenProcessorList() {
        return this.contextMenuScreenOpenProcessorList;
    }

    @Override
    public @Nullable Screen getContextMenuCallbackScreen() {
        return this.editor;
    }

    private static @NotNull MaterialIcon getAnchorPointIcon(@NotNull ElementAnchorPoint anchorPoint) {
        return switch (anchorPoint.getName()) {
            case "top-left" -> MaterialIcons.NORTH_WEST;
            case "mid-left" -> MaterialIcons.WEST;
            case "bottom-left" -> MaterialIcons.SOUTH_WEST;
            case "top-centered" -> MaterialIcons.NORTH;
            case "mid-centered" -> MaterialIcons.CENTER_FOCUS_STRONG;
            case "bottom-centered" -> MaterialIcons.SOUTH;
            case "top-right" -> MaterialIcons.NORTH_EAST;
            case "mid-right" -> MaterialIcons.EAST;
            case "bottom-right" -> MaterialIcons.SOUTH_EAST;
            case "vanilla" -> MaterialIcons.DASHBOARD;
            default -> MaterialIcons.ANCHOR;
        };
    }

    public enum ResizeGrabberType {
        TOP_LEFT,
        TOP,
        TOP_RIGHT,
        RIGHT,
        BOTTOM_RIGHT,
        BOTTOM,
        BOTTOM_LEFT,
        LEFT
    }

    public class RotationGrabber implements Renderable {

        protected int size = RESIZE_INDICATOR_SIZE; // Size of the grabber
        protected boolean hovered = false;

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.hovered = AbstractEditorElement.this.isSelected() && this.isGrabberEnabled() && this.isMouseOver(mouseX, mouseY);
            if (AbstractEditorElement.this.isSelected() && this.isGrabberEnabled()) {
                // Draw the grabber as a filled square at the rotation position
                int x = this.getX();
                int y = this.getY();
                float radius = UIBase.getInterfaceCornerRoundingRadius() > 0.0F ? RESIZE_INDICATOR_CORNER_RADIUS : 0.0F;
                int color = UIBase.getUITheme().layout_editor_element_border_rotation_controls_color.getColorIntWithAlpha(ROTATION_CONTROLS_ALPHA.get(AbstractEditorElement.this));

                SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                        graphics,
                        x - size / 2,
                        y - size / 2,
                        size,
                        size,
                        radius,
                        radius,
                        radius,
                        radius,
                        color,
                        1.0F
                );
            }
        }

        protected int getX() {
            float centerX = AbstractEditorElement.this.getX() + (AbstractEditorElement.this.getWidth() / 2.0F);
            float halfWidth = AbstractEditorElement.this.getWidth() / 2.0F;
            float halfHeight = AbstractEditorElement.this.getHeight() / 2.0F;
            float radius = (float) Math.sqrt(halfWidth * halfWidth + halfHeight * halfHeight) + 8; // Same padding as circle

            // Position grabber at the current rotation angle
            // Start at the top (90 degrees offset because 0 degrees is to the right in standard coordinates)
            float angleRad = (float) Math.toRadians(AbstractEditorElement.this.element.getRotationDegrees() - 90);
            return (int) (centerX + radius * Math.cos(angleRad));
        }

        protected int getY() {
            float centerY = AbstractEditorElement.this.getY() + (AbstractEditorElement.this.getHeight() / 2.0F);
            float halfWidth = AbstractEditorElement.this.getWidth() / 2.0F;
            float halfHeight = AbstractEditorElement.this.getHeight() / 2.0F;
            float radius = (float) Math.sqrt(halfWidth * halfWidth + halfHeight * halfHeight) + 8; // Same padding as circle

            // Position grabber at the current rotation angle
            // Start at the top (90 degrees offset because 0 degrees is to the right in standard coordinates)
            float angleRad = (float) Math.toRadians(AbstractEditorElement.this.element.getRotationDegrees() - 90);
            return (int) (centerY + radius * Math.sin(angleRad));
        }

        protected boolean isGrabberEnabled() {
            if (!FancyMenu.getOptions().enableElementRotationControls.getValue()) {
                return false;
            }
            if (!AbstractEditorElement.this.element.supportsRotation()) {
                return false;
            }
            if (AbstractEditorElement.this.element.advancedRotationMode) {
                return false;
            }
            if (AbstractEditorElement.this.isMultiSelected()) {
                return false;
            }
            return true;
        }

        protected boolean isMouseOver(double mouseX, double mouseY) {
            int x = this.getX();
            int y = this.getY();
            return (mouseX >= x - (double)size/2) && (mouseX <= x + (double)size/2) && (mouseY >= y - (double)size/2) && (mouseY <= y + (double)size/2);
        }

    }

    public class VerticalTiltGrabber implements Renderable {

        protected int size = RESIZE_INDICATOR_SIZE; // Size of the grabber
        protected boolean hovered = false;

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.hovered = AbstractEditorElement.this.isSelected() && this.isGrabberEnabled() && this.isMouseOver(mouseX, mouseY);
            if (AbstractEditorElement.this.isSelected() && this.isGrabberEnabled()) {
                // Draw the grabber as a filled square at the tilt position
                int x = this.getX();
                int y = this.getY();
                float radius = UIBase.getInterfaceCornerRoundingRadius() > 0.0F ? RESIZE_INDICATOR_CORNER_RADIUS : 0.0F;
                int color = UIBase.getUITheme().layout_editor_element_border_vertical_tilting_controls_color.getColorIntWithAlpha(VERTICAL_TILT_CONTROLS_ALPHA.get(AbstractEditorElement.this));

                SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                        graphics,
                        x - size / 2,
                        y - size / 2,
                        size,
                        size,
                        radius,
                        radius,
                        radius,
                        radius,
                        color,
                        1.0F
                );
            }
        }

        protected int getX() {
            return AbstractEditorElement.this.getVerticalTiltLineX();
        }

        protected int getY() {
            float lineLength = AbstractEditorElement.this.getHeight() + (TILT_CONTROLS_LINE_EXTENSION * 2);
            float lineTop = AbstractEditorElement.this.getY() - TILT_CONTROLS_LINE_EXTENSION;
            // Map tilt angle (-60 to 60) to position on line
            // 0 degrees = center, -60 = top, 60 = bottom
            float normalizedTilt = (AbstractEditorElement.this.element.getVerticalTiltDegrees() + 60.0F) / 120.0F; // 0 to 1
            return (int)(lineTop + (lineLength * normalizedTilt));
        }

        protected boolean isGrabberEnabled() {
            if (!FancyMenu.getOptions().enableElementTiltingControls.getValue()) {
                return false;
            }
            if (!AbstractEditorElement.this.element.supportsTilting()) {
                return false;
            }
            if (AbstractEditorElement.this.element.advancedVerticalTiltMode) {
                return false;
            }
            if (AbstractEditorElement.this.isMultiSelected()) {
                return false;
            }
            return true;
        }

        protected boolean isMouseOver(double mouseX, double mouseY) {
            int x = this.getX();
            int y = this.getY();
            return (mouseX >= x - (double)size/2) && (mouseX <= x + (double)size/2) && (mouseY >= y - (double)size/2) && (mouseY <= y + (double)size/2);
        }

    }

    public class HorizontalTiltGrabber implements Renderable {

        protected int size = RESIZE_INDICATOR_SIZE; // Size of the grabber
        protected boolean hovered = false;

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.hovered = AbstractEditorElement.this.isSelected() && this.isGrabberEnabled() && this.isMouseOver(mouseX, mouseY);
            if (AbstractEditorElement.this.isSelected() && this.isGrabberEnabled()) {
                // Draw the grabber as a filled square at the tilt position
                int x = this.getX();
                int y = this.getY();
                float radius = UIBase.getInterfaceCornerRoundingRadius() > 0.0F ? RESIZE_INDICATOR_CORNER_RADIUS : 0.0F;
                int color = UIBase.getUITheme().layout_editor_element_border_horizontal_tilting_controls_color.getColorIntWithAlpha(HORIZONTAL_TILT_CONTROLS_ALPHA.get(AbstractEditorElement.this));

                SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                        graphics,
                        x - size / 2,
                        y - size / 2,
                        size,
                        size,
                        radius,
                        radius,
                        radius,
                        radius,
                        color,
                        1.0F
                );
            }
        }

        protected int getX() {
            float lineLength = AbstractEditorElement.this.getWidth() + (TILT_CONTROLS_LINE_EXTENSION * 2);
            float lineLeft = AbstractEditorElement.this.getX() - TILT_CONTROLS_LINE_EXTENSION;
            // Map tilt angle (-60 to 60) to position on line
            // 0 degrees = center, -60 = left, 60 = right
            float normalizedTilt = (AbstractEditorElement.this.element.getHorizontalTiltDegrees() + 60.0F) / 120.0F; // 0 to 1
            return (int)(lineLeft + (lineLength * normalizedTilt));
        }

        protected int getY() {
            return AbstractEditorElement.this.getHorizontalTiltLineY();
        }

        protected boolean isGrabberEnabled() {
            if (!FancyMenu.getOptions().enableElementTiltingControls.getValue()) {
                return false;
            }
            if (!AbstractEditorElement.this.element.supportsTilting()) {
                return false;
            }
            if (AbstractEditorElement.this.element.advancedHorizontalTiltMode) {
                return false;
            }
            if (AbstractEditorElement.this.isMultiSelected()) {
                return false;
            }
            return true;
        }

        protected boolean isMouseOver(double mouseX, double mouseY) {
            int x = this.getX();
            int y = this.getY();
            return (mouseX >= x - (double)size/2) && (mouseX <= x + (double)size/2) && (mouseY >= y - (double)size/2) && (mouseY <= y + (double)size/2);
        }

    }

}

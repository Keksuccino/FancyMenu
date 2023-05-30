package de.keksuccino.fancymenu.customization.element.editor;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements.ManageRequirementsScreen;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.misc.ConsumingSupplier;
import de.keksuccino.fancymenu.misc.ValueSwitcher;
import de.keksuccino.fancymenu.rendering.AspectRatio;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.rendering.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.rendering.ui.screen.filechooser.FileChooserScreen;
import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.utils.ListUtils;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import de.keksuccino.fancymenu.utils.ObjectUtils;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("unused")
public abstract class AbstractEditorElement extends GuiComponent implements Renderable, GuiEventListener {

	private static final Logger LOGGER = LogManager.getLogger();

	protected static final ResourceLocation DRAGGING_NOT_ALLOWED_TEXTURE = new ResourceLocation("fancymenu", "textures/not_allowed.png");
	protected static final Color DRAGGING_NOT_ALLOWED_OVERLAY_COLOR = new Color(232, 54, 9, 200);
	protected static final Color BORDER_COLOR_SELECTED = new Color(3, 219, 252);
	protected static final Color BORDER_COLOR_NORMAL = new Color(3, 148, 252);
	protected static final ConsumingSupplier<AbstractEditorElement, Integer> BORDER_COLOR = (editorElement) -> {
		if (editorElement.isSelected()) {
			return BORDER_COLOR_SELECTED.getRGB();
		}
		return BORDER_COLOR_NORMAL.getRGB();
	};
	protected static final long CURSOR_HORIZONTAL_RESIZE = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR);
	protected static final long CURSOR_VERTICAL_RESIZE = GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR);
	protected static final long CURSOR_NORMAL = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);

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
	protected ResizeGrabber[] resizeGrabbers = new ResizeGrabber[]{new ResizeGrabber(ResizeGrabberType.TOP), new ResizeGrabber(ResizeGrabberType.RIGHT), new ResizeGrabber(ResizeGrabberType.BOTTOM), new ResizeGrabber(ResizeGrabberType.LEFT)};
	protected ResizeGrabber activeResizeGrabber = null;
	public long renderMovingNotAllowedTime = -1;
	public boolean recentlyMovedByDragging = false;
	public boolean recentlyLeftClickSelected = false;

	private final List<AbstractEditorElement> cachedHoveredElementsOnRightClickMenuOpen = new ArrayList<>();

	public AbstractEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor, @Nullable EditorElementSettings settings) {
		this.settings = (settings != null) ? settings : new EditorElementSettings();
		this.settings.editorElement = this;
		this.editor = editor;
		this.element = element;
		this.rightClickMenu = new ContextMenu() {
			@Override
			public @NotNull ContextMenu openMenuAt(int x, int y) {
				cachedHoveredElementsOnRightClickMenuOpen.clear();
				cachedHoveredElementsOnRightClickMenuOpen.addAll(editor.getHoveredElements());
				return super.openMenuAt(x, y);
			}
		};
		this.init();
	}

	public AbstractEditorElement(@Nonnull AbstractElement element, @Nonnull LayoutEditorScreen editor) {
		this(element, editor, new EditorElementSettings());
	}

	@SuppressWarnings("all")
	public void init() {

		this.rightClickMenu.closeMenu();
		this.rightClickMenu.clearEntries();
		this.topLeftDisplay.clearLines();
		this.bottomRightDisplay.clearLines();

		this.topLeftDisplay.addLine("anchor_point", () -> Component.translatable("fancymenu.element.border_display.anchor_point", this.element.anchorPoint.getDisplayName()));
		this.topLeftDisplay.addLine("pos_x", () -> Component.translatable("fancymenu.element.border_display.pos_x", "" + this.getX()));
		this.topLeftDisplay.addLine("width", () -> Component.translatable("fancymenu.element.border_display.width", "" + this.getWidth()));

		this.bottomRightDisplay.addLine("pos_y", () -> Component.translatable("fancymenu.element.border_display.pos_y", "" + this.getY()));
		this.bottomRightDisplay.addLine("height", () -> Component.translatable("fancymenu.element.border_display.height", "" + this.getHeight()));

		ContextMenu pickElementMenu = new ContextMenu() {
			@Override
			public @NotNull ContextMenu openMenuAt(int x, int y) {
				this.clearEntries();
				int i = 0;
				for (AbstractEditorElement e : cachedHoveredElementsOnRightClickMenuOpen) {
					this.addClickableEntry("element_" + i, e.element.builder.getDisplayName(e.element), (menu, entry) -> {
						for (AbstractEditorElement e2 : AbstractEditorElement.this.editor.getAllElements()) {
							e2.resetElementStates();
						}
						e.setSelected(true);
					});
					i++;
				}
				return super.openMenuAt(x, y);
			}
		};
		this.rightClickMenu.addSubMenuEntry("pick_element", Component.translatable("fancymenu.element.general.pick_element"), pickElementMenu)
				.setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.element.general.pick_element.desc")));

		this.rightClickMenu.addSeparatorEntry("separator_1");

		if (this.settings.isIdentifierCopyable()) {

			this.rightClickMenu.addClickableEntry("copy_id", Component.translatable("fancymenu.helper.editor.items.copyid"), (menu, entry) -> {
				Minecraft.getInstance().keyboardHandler.setClipboard(this.element.getInstanceIdentifier());
				menu.closeMenu();
			}).setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.copyid.btn.desc")));

		}

		//TODO add vanilla button locator button in vanilla button editor element HERE

		this.rightClickMenu.addSeparatorEntry("separator_2");

		if (this.settings.isAnchorPointChangeable()) {

			ContextMenu anchorPointMenu = new ContextMenu();
			this.rightClickMenu.addSubMenuEntry("anchor_point", Component.translatable("fancymenu.editor.items.setorientation"), anchorPointMenu)
					.setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.editor.items.orientation.btndesc")))
					.setStackable(true);

			if (this.settings.isElementAnchorPointAllowed()) {

				anchorPointMenu.addClickableEntry("anchor_point_element", ElementAnchorPoints.ELEMENT.getDisplayName(), (menu, entry) ->
						{
							if (entry.getStackMeta().isFirstInStack()) {
								FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), I18n.get("fancymenu.helper.editor.items.orientation.element.setidentifier"), null, 240, (call) -> {
									if (call != null) {
										AbstractEditorElement editorElement = this.editor.getElementByInstanceIdentifier(call);
										if (editorElement != null) {
											this.editor.history.saveSnapshot();
											for (AbstractEditorElement e : this.editor.getSelectedElements()) {
												if (e.settings.isAnchorPointChangeable() && e.settings.isElementAnchorPointAllowed()) {
													e.element.anchorPointElementIdentifier = editorElement.element.getInstanceIdentifier();
													e.setAnchorPoint(ElementAnchorPoints.ELEMENT);
												}
											}
										} else {
											PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, LocalizationUtils.splitLocalizedStringLines("fancymenu.helper.editor.items.orientation.element.setidentifier.identifiernotfound")));
										}
									}
								});
								if (!entry.getStackMeta().isPartOfStack() && (this.element.anchorPointElementIdentifier != null)) {
									p.setText(this.element.anchorPointElementIdentifier);
								}
								PopupHandler.displayPopup(p);
								menu.closeMenu();
							}
						})
						.setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.orientation.element.btn.desc")))
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
									e.setAnchorPoint(p);
								}
							}
							menu.closeMenu();
						}
					}).setStackable(true);
				}
			}

		}

		if (this.settings.isAdvancedPositioningSupported()) {

			ContextMenu advancedPositioningMenu = new ContextMenu();
			this.rightClickMenu.addSubMenuEntry("advanced_positioning", Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning"), advancedPositioningMenu)
					.setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.features.advanced_positioning.desc")))
					.setStackable(true);

			this.addStringInputContextMenuEntryTo(advancedPositioningMenu, "advanced_positioning_x", null,
							element -> element.settings.isAdvancedPositioningSupported(),
							null,
							consumes -> consumes.element.advancedX,
							(element, input) -> element.element.advancedX = input,
							false, true, Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning.posx"))
					.setStackable(true);

			this.addStringInputContextMenuEntryTo(advancedPositioningMenu, "advanced_positioning_y", null,
							element -> element.settings.isAdvancedPositioningSupported(),
							null,
							consumes -> consumes.element.advancedY,
							(element, input) -> element.element.advancedY = input,
							false, true, Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning.posy"))
					.setStackable(true);

		}

		if (this.settings.isAdvancedSizingSupported()) {

			ContextMenu advancedSizingMenu = new ContextMenu();
			this.rightClickMenu.addSubMenuEntry("advanced_sizing", Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing"), advancedSizingMenu)
					.setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.features.advanced_sizing.desc")))
					.setStackable(true);

			this.addStringInputContextMenuEntryTo(advancedSizingMenu, "advanced_sizing_width", null,
							element -> element.settings.isAdvancedSizingSupported(),
							null,
							consumes -> consumes.element.advancedWidth,
							(element, input) -> {
								element.element.advancedWidth = input;
								element.element.width = 50;
							}, false, true, Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing.width"))
					.setStackable(true);

			this.addStringInputContextMenuEntryTo(advancedSizingMenu, "advanced_sizing_height", null,
							element -> element.settings.isAdvancedSizingSupported(),
							null,
							consumes -> consumes.element.advancedHeight, (element, input) -> {
								element.element.advancedHeight = input;
								element.element.height = 50;
							}, false, true, Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing.height"))
					.setStackable(true);

		}

		this.rightClickMenu.addSeparatorEntry("separator_3").setStackable(true);

		if (this.settings.isStretchable()) {

			this.addBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "stretch_x",
							consumes -> consumes.settings.isStretchable(),
							consumes -> consumes.element.stretchX,
							(element, switcherValue) -> element.element.stretchX = switcherValue,
							"fancymenu.editor.object.stretch.x")
					.setStackable(true)
					.setIsActiveSupplier((menu, entry) -> element.advancedWidth == null);

			this.addBooleanSwitcherContextMenuEntryTo(this.rightClickMenu, "stretch_y",
							consumes -> consumes.settings.isStretchable(),
							consumes -> consumes.element.stretchY,
							(element, switcherValue) -> element.element.stretchY = switcherValue,
							"fancymenu.editor.object.stretch.y")
					.setStackable(true)
					.setIsActiveSupplier((menu, entry) -> element.advancedHeight == null);

		}

		this.rightClickMenu.addSeparatorEntry("separator_4").setStackable(true);

		if (this.settings.isLoadingRequirementsEnabled()) {

			this.rightClickMenu.addClickableEntry("loading_requirements", Component.translatable("fancymenu.editor.loading_requirement.elements.loading_requirements"), (menu, entry) ->
					{
						if (!entry.getStackMeta().isPartOfStack()) {
							ManageRequirementsScreen s = new ManageRequirementsScreen(this.editor, this.element.loadingRequirementContainer.copy(true), (call) -> {
								if (call != null) {
									this.editor.history.saveSnapshot();
									this.element.loadingRequirementContainer = call;
								}
							});
							Minecraft.getInstance().setScreen(s);
						} else if (entry.getStackMeta().isFirstInStack()) {
							List<AbstractEditorElement> selectedElements = this.getFilteredSelectedElementList(element -> element.settings.isLoadingRequirementsEnabled());
							List<LoadingRequirementContainer> containers = ObjectUtils.getOfAll(LoadingRequirementContainer.class, selectedElements, consumes -> consumes.element.loadingRequirementContainer);
							LoadingRequirementContainer containerToUseInManager = new LoadingRequirementContainer();
							boolean allEqual = ListUtils.allInListEqual(containers);
							if (allEqual) {
								containerToUseInManager = containers.get(0).copy(false);
							}
							ManageRequirementsScreen s = new ManageRequirementsScreen(this.editor, containerToUseInManager, (call) -> {
								if (call != null) {
									this.editor.history.saveSnapshot();
									for (AbstractEditorElement e : selectedElements) {
										e.element.loadingRequirementContainer = call.copy(false);
									}
								}
							});
							if (allEqual) {
								Minecraft.getInstance().setScreen(s);
							} else {
								Minecraft.getInstance().setScreen(new ConfirmationScreen((call) -> {
									if (call) {
										Minecraft.getInstance().setScreen(s);
									} else {
										Minecraft.getInstance().setScreen(this.editor);
									}
								}, LocalizationUtils.splitLocalizedStringLines("fancymenu.elements.multiselect.loading_requirements.warning.override")));
							}
						}
					})
					.setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.editor.loading_requirement.elements.loading_requirements.desc")))
					.setStackable(true);

		}

		this.rightClickMenu.addSeparatorEntry("separator_5");

		if (this.settings.isOrderable()) {

			this.rightClickMenu.addClickableEntry("move_up_element", Component.translatable("fancymenu.editor.object.moveup"), (menu, entry) -> {
				AbstractEditorElement o = this.editor.moveElementUp(this);
				if (o != null) {
					entry.setTooltipSupplier((menu1, entry1) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.editor.object.moveup.desc", I18n.get("fancymenu.editor.object.moveup.desc.subtext", o.element.builder.getDisplayName(o.element).getString()))));
				}
			}).setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.editor.object.moveup.desc")));

			this.rightClickMenu.addClickableEntry("move_down_element", Component.translatable("fancymenu.editor.object.movedown"), (menu, entry) -> {
				AbstractEditorElement o = this.editor.moveElementDown(this);
				if (o != null) {
					entry.setTooltipSupplier((menu1, entry1) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.editor.object.movedown.desc", I18n.get("fancymenu.editor.object.movedown.desc.subtext", o.element.builder.getDisplayName(o.element).getString()))));
				}
			}).setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.editor.object.movedown.desc")));

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
					.setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.copy"));

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
					.setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.delete"));

		}

		this.rightClickMenu.addSeparatorEntry("separator_7").setStackable(true);

		if (this.settings.isDelayable()) {

			ContextMenu appearanceDelayMenu = new ContextMenu();
			this.rightClickMenu.addSubMenuEntry("appearance_delay", Component.translatable("fancymenu.element.general.appearance_delay"), appearanceDelayMenu)
					.setStackable(true);

			this.addSwitcherContextMenuEntryTo(appearanceDelayMenu, "appearance_delay_type",
							ListUtils.build(AbstractElement.AppearanceDelay.NO_DELAY, AbstractElement.AppearanceDelay.FIRST_TIME, AbstractElement.AppearanceDelay.EVERY_TIME),
							consumes -> consumes.settings.isDelayable(),
							consumes -> consumes.element.appearanceDelay,
							(element, switcherValue) -> element.element.appearanceDelay = switcherValue,
							(menu, entry, switcherValue) -> {
								return Component.translatable("fancymenu.element.general.appearance_delay." + switcherValue.name);
							})
					.setStackable(true);

			this.addFloatInputContextMenuEntryTo(appearanceDelayMenu, "appearance_delay_seconds",
							element -> element.settings.isDelayable(),
							1.0F,
							element -> element.element.appearanceDelayInSeconds,
							(element, input) -> element.element.appearanceDelayInSeconds = input,
							Component.translatable("fancymenu.element.general.appearance_delay.seconds"))
					.setStackable(true);

			appearanceDelayMenu.addSeparatorEntry("separator_1").setStackable(true);

			this.addBooleanSwitcherContextMenuEntryTo(appearanceDelayMenu, "appearance_delay_fade_in",
							consumes -> consumes.settings.isDelayable(),
							consumes -> consumes.element.fadeIn,
							(element, switcherValue) -> element.element.fadeIn = switcherValue,
							"fancymenu.element.general.appearance_delay.fade_in")
					.setStackable(true);

			this.addFloatInputContextMenuEntryTo(appearanceDelayMenu, "appearance_delay_fade_in_speed",
							element -> element.settings.isDelayable(),
							1.0F,
							element -> element.element.fadeInSpeed,
							(element, input) -> element.element.fadeInSpeed = input,
							Component.translatable("fancymenu.element.general.appearance_delay.fade_in.speed"))
					.setStackable(true);

		}

		this.rightClickMenu.addSeparatorEntry("separator_8").setStackable(true);

	}

	@Override
	public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

		this.tick();

		this.hovered = this.isMouseOver(mouseX, mouseY);

		this.element.render(pose, mouseX, mouseY, partial);

		this.renderDraggingNotAllowedOverlay(pose);

		//Update cursor
		ResizeGrabber hoveredGrabber = this.getHoveredResizeGrabber();
		GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), (hoveredGrabber != null) ? hoveredGrabber.getCursor() : CURSOR_NORMAL);

		this.renderBorder(pose, mouseX, mouseY, partial);

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

	protected void renderDraggingNotAllowedOverlay(PoseStack pose) {
		if (this.renderMovingNotAllowedTime >= System.currentTimeMillis()) {
			RenderSystem.enableBlend();
			fill(pose, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), DRAGGING_NOT_ALLOWED_OVERLAY_COLOR.getRGB());
			AspectRatio ratio = new AspectRatio(32, 32);
			int[] size = ratio.getAspectRatioSizeByMaximumSize(this.getWidth(), this.getHeight());
			int texW = size[0];
			int texH = size[1];
			int texX = this.getX() + (this.getWidth() / 2) - (texW / 2);
			int texY = this.getY() + (this.getHeight() / 2) - (texH / 2);
			RenderUtils.bindTexture(DRAGGING_NOT_ALLOWED_TEXTURE);
			blit(pose, texX, texY, 0.0F, 0.0F, texW, texH, texW, texH);
		}
	}

	protected void renderBorder(PoseStack pose, int mouseX, int mouseY, float partial) {

		if (this.isHovered() || this.isSelected() || this.isMultiSelected()) {

			//TOP
			fill(pose, this.getX() + 1, this.getY(), this.getX() + this.getWidth() - 1, this.getY() + 1, BORDER_COLOR.get(this));
			//BOTTOM
			fill(pose, this.getX() + 1, this.getY() + this.getHeight() - 1, this.getX() + this.getWidth() - 1, this.getY() + this.getHeight(), BORDER_COLOR.get(this));
			//LEFT
			fill(pose, this.getX(), this.getY(), this.getX() + 1, this.getY() + this.getHeight(), BORDER_COLOR.get(this));
			//RIGHT
			fill(pose, this.getX() + this.getWidth() - 1, this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), BORDER_COLOR.get(this));

			for (ResizeGrabber g : this.resizeGrabbers) {
				g.render(pose, mouseX, mouseY, partial);
			}

		}

		if (this.isSelected()) {
			this.topLeftDisplay.render(pose, mouseX, mouseY, partial);
			this.bottomRightDisplay.render(pose, mouseX, mouseY, partial);
		}

	}

	public void setAnchorPoint(ElementAnchorPoint p) {
		this.resetElementStates();
		if (p == null) {
			p = ElementAnchorPoints.TOP_LEFT;
		}
		this.element.anchorPoint = p;
		this.element.baseX = p.getDefaultElementBaseX(this.element);
		this.element.baseY = p.getDefaultElementBaseY(this.element);
	}

	public void resetElementStates() {
		this.hovered = false;
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

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!this.isSelected()) {
			return false;
		}
		if (button == 0) {
			if (!this.rightClickMenu.isUserNavigatingInMenu()) {
				this.activeResizeGrabber = !this.isMultiSelected() ? this.getHoveredResizeGrabber() : null;
				if (this.isHovered() || (this.isMultiSelected() && !this.editor.getHoveredElements().isEmpty()) || this.isGettingResized()) {
					this.leftMouseDown = true;
					this.leftMouseDownMouseX = mouseX;
					this.leftMouseDownMouseY = mouseY;
					this.leftMouseDownBaseX = this.element.baseX;
					this.leftMouseDownBaseY = this.element.baseY;
					this.leftMouseDownBaseWidth = this.element.width;
					this.leftMouseDownBaseHeight = this.element.height;
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
			this.recentlyMovedByDragging = false;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double $$3, double $$4) {
		if (!this.isSelected()) {
			return false;
		}
		if (button == 0) {
			int diffX = (int)-(this.leftMouseDownMouseX - mouseX);
			int diffY = (int)-(this.leftMouseDownMouseY - mouseY);
			if (this.leftMouseDown && !this.isGettingResized()) {
				if (this.editor.allSelectedElementsMovable()) {
					this.element.baseX = this.leftMouseDownBaseX + diffX;
					this.element.baseY = this.leftMouseDownBaseY + diffY;
					if ((diffX > 0) || (diffY > 0)) {
						this.recentlyMovedByDragging = true;
					}
				} else if (!this.settings.isMovable()) {
					this.renderMovingNotAllowedTime = System.currentTimeMillis() + 800;
				}
			}
			//TODO add SHIFT-resize (aspect ratio)
			if (this.leftMouseDown && this.isGettingResized()) {
				if ((this.activeResizeGrabber.type == ResizeGrabberType.LEFT) || (this.activeResizeGrabber.type == ResizeGrabberType.RIGHT)) {
					int i = (this.activeResizeGrabber.type == ResizeGrabberType.LEFT) ? (this.leftMouseDownBaseWidth - diffX) : (this.leftMouseDownBaseWidth + diffX);
					if (i >= 2) {
						this.element.width = i;
						this.element.baseX = this.leftMouseDownBaseX + this.element.anchorPoint.getResizePositionOffsetX(this.element, diffX, this.activeResizeGrabber.type);
					}
				}
				if ((this.activeResizeGrabber.type == ResizeGrabberType.TOP) || (this.activeResizeGrabber.type == ResizeGrabberType.BOTTOM)) {
					int i = (this.activeResizeGrabber.type == ResizeGrabberType.TOP) ? (this.leftMouseDownBaseHeight - diffY) : (this.leftMouseDownBaseHeight + diffY);
					if (i >= 2) {
						this.element.height = i;
						this.element.baseY = this.leftMouseDownBaseY + this.element.anchorPoint.getResizePositionOffsetY(this.element, diffY, this.activeResizeGrabber.type);
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return (mouseX >= this.element.getX()) && (mouseX <= this.element.getX() + this.element.getWidth()) && (mouseY >= this.element.getY()) && mouseY <= this.element.getY() + this.element.getHeight();
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
		return this.hovered || this.rightClickMenu.isUserNavigatingInMenu() || (this.getHoveredResizeGrabber() != null);
	}

	public int getX() {
		return this.element.getX();
	}

	public int getY() {
		return this.element.getY();
	}

	public int getWidth() {
		return this.element.getWidth();
	}

	public int getHeight() {
		return this.element.getHeight();
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

	public class ResizeGrabber extends GuiComponent implements Renderable {

		protected int width = 4;
		protected int height = 4;
		protected final ResizeGrabberType type;
		protected boolean hovered = false;

		protected ResizeGrabber(ResizeGrabberType type) {
			this.type = type;
		}

		@Override
		public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
			this.hovered = AbstractEditorElement.this.isSelected() && this.isGrabberEnabled() && this.isMouseOver(mouseX, mouseY);
			if (AbstractEditorElement.this.isSelected() && this.isGrabberEnabled()) {
				fill(pose, this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, BORDER_COLOR.get(AbstractEditorElement.this));
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
				return CURSOR_VERTICAL_RESIZE;
			}
			return CURSOR_HORIZONTAL_RESIZE;
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

	protected ContextMenu.ContextMenuEntry addFileChooserContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, String defaultValue, @NotNull ConsumingSupplier<AbstractEditorElement, String> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, String> targetFieldSetter, @NotNull Component label, boolean addResetEntry, @Nullable FileChooserScreen.FileFilter fileFilter) {
		ContextMenu subMenu = new ContextMenu();
		ContextMenu addToFinal = addResetEntry ? subMenu : addTo;
		ContextMenu.ContextMenuEntry chooseEntry = addToFinal.addClickableEntry(addResetEntry ? "choose_file" : entryIdentifier, addResetEntry ? Component.translatable("fancymenu.ui.filechooser.choose.file") : label, (menu, entry) ->
		{
			List<AbstractEditorElement> selectedElements = this.getFilteredSelectedElementList(selectedElementsFilter);
			if (entry.getStackMeta().isFirstInStack() && !selectedElements.isEmpty()) {
				File startDir = FancyMenu.getGameDirectory();
				List<String> allPaths = ObjectUtils.getOfAll(String.class, selectedElements, targetFieldGetter);
				if (ListUtils.allInListEqual(allPaths)) {
					String path = allPaths.get(0);
					if (path != null) {
						startDir = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(allPaths.get(0))).getParentFile();
						if (startDir == null) startDir = FancyMenu.getGameDirectory();
					}
				}
				FileChooserScreen fileChooser = new FileChooserScreen(FancyMenu.getGameDirectory(), startDir, (call) -> {
					if (call != null) {
						this.editor.history.saveSnapshot();
						for (AbstractEditorElement e : selectedElements) {
							targetFieldSetter.accept(e, ScreenCustomization.getPathWithoutGameDirectory(call.getAbsolutePath()));
						}
					}
					Minecraft.getInstance().setScreen(this.editor);
				});
				fileChooser.setFileFilter(fileFilter);
				Minecraft.getInstance().setScreen(fileChooser);
			}
		}).setStackable(true);
		if (addResetEntry) {
			subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.editor.filechooser.reset"), (menu, entry) ->
			{
				if (entry.getStackMeta().isFirstInStack()) {
					List<AbstractEditorElement> selectedElements = this.getFilteredSelectedElementList(selectedElementsFilter);
					this.editor.history.saveSnapshot();
					for (AbstractEditorElement e : selectedElements) {
						targetFieldSetter.accept(e, defaultValue);
					}
				}
			}).setStackable(true);
			return addTo.addSubMenuEntry(entryIdentifier, label, subMenu);
		}
		return chooseEntry;
	}

	protected ContextMenu.ClickableContextMenuEntry addInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable CharacterFilter inputCharacterFilter, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<AbstractEditorElement, String> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, String> targetFieldSetter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label) {
		return addTo.addClickableEntry(entryIdentifier, label, (menu, entry) ->
				{
					if (entry.getStackMeta().isFirstInStack()) {
						List<AbstractEditorElement> selectedElements = this.getFilteredSelectedElementList(selectedElementsFilter);
						TextEditorScreen s = new TextEditorScreen(label, this.editor, inputCharacterFilter, (call) -> {
							if (call != null) {
								this.editor.history.saveSnapshot();
								for (AbstractEditorElement e : selectedElements) {
									targetFieldSetter.accept(e, call);
								}
							}
							menu.closeMenu();
						});
						s.multilineMode = multiLineInput;
						s.setPlaceholdersAllowed(allowPlaceholders);
						List<String> targetValuesOfSelected = new ArrayList<>();
						for (AbstractEditorElement e : selectedElements) {
							targetValuesOfSelected.add(targetFieldGetter.get(e));
						}
						if (!entry.getStackMeta().isPartOfStack() || ListUtils.allInListEqual(targetValuesOfSelected)) {
							String text = targetFieldGetter.get(this);
							if (text != null) s.setText(text);
						}
						Minecraft.getInstance().setScreen(s);
					}
				});
	}

	protected ContextMenu.ClickableContextMenuEntry addStringInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable CharacterFilter inputCharacterFilter, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, String defaultValue, @NotNull ConsumingSupplier<AbstractEditorElement, String> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, String> targetFieldSetter, boolean multiLineInput, boolean allowPlaceholders, @NotNull Component label) {
		return addInputContextMenuEntryTo(addTo, entryIdentifier, inputCharacterFilter, selectedElementsFilter, targetFieldGetter, (e, s) -> {
			if (s.replace(" ", "").isEmpty()) {
				targetFieldSetter.accept(e, defaultValue);
			} else {
				targetFieldSetter.accept(e, s);
			}
		}, multiLineInput, allowPlaceholders, label);
	}

	protected ContextMenu.ClickableContextMenuEntry addIntegerInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, int defaultValue, @NotNull ConsumingSupplier<AbstractEditorElement, Integer> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, Integer> targetFieldSetter, @NotNull Component label) {
		return addInputContextMenuEntryTo(addTo, entryIdentifier, CharacterFilter.getIntegerCharacterFiler(), selectedElementsFilter, consumes -> {
			Integer i = targetFieldGetter.get(consumes);
			if (i == null) i = 0;
			return "" + i;
		}, (e, s) -> {
			if (s.replace(" ", "").isEmpty()) {
				targetFieldSetter.accept(e, defaultValue);
			} else if (MathUtils.isInteger(s)) {
				targetFieldSetter.accept(e, Integer.parseInt(s));
			}
		}, false, false, label);
	}

	protected ContextMenu.ClickableContextMenuEntry addLongInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, long defaultValue, @NotNull ConsumingSupplier<AbstractEditorElement, Long> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, Long> targetFieldSetter, @NotNull Component label) {
		return addInputContextMenuEntryTo(addTo, entryIdentifier, CharacterFilter.getIntegerCharacterFiler(), selectedElementsFilter, consumes -> {
			Long l = targetFieldGetter.get(consumes);
			if (l == null) l = 0L;
			return "" + l;
		}, (e, s) -> {
			if (s.replace(" ", "").isEmpty()) {
				targetFieldSetter.accept(e, defaultValue);
			} else if (MathUtils.isLong(s)) {
				targetFieldSetter.accept(e, Long.parseLong(s));
			}
		}, false, false, label);
	}

	protected ContextMenu.ClickableContextMenuEntry addDoubleInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, double defaultValue, @NotNull ConsumingSupplier<AbstractEditorElement, Double> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, Double> targetFieldSetter, @NotNull Component label) {
		return addInputContextMenuEntryTo(addTo, entryIdentifier, CharacterFilter.getDoubleCharacterFiler(), selectedElementsFilter, consumes -> {
			Double d = targetFieldGetter.get(consumes);
			if (d == null) d = 0D;
			return "" + d;
		}, (e, s) -> {
			if (s.replace(" ", "").isEmpty()) {
				targetFieldSetter.accept(e, defaultValue);
			} else if (MathUtils.isDouble(s)) {
				targetFieldSetter.accept(e, Double.parseDouble(s));
			}
		}, false, false, label);
	}

	protected ContextMenu.ClickableContextMenuEntry addFloatInputContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, float defaultValue, @NotNull ConsumingSupplier<AbstractEditorElement, Float> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, Float> targetFieldSetter, @NotNull Component label) {
		return addInputContextMenuEntryTo(addTo, entryIdentifier, CharacterFilter.getDoubleCharacterFiler(), selectedElementsFilter, consumes -> {
			Float d = targetFieldGetter.get(consumes);
			if (d == null) d = 0F;
			return "" + d;
		}, (e, s) -> {
			if (s.replace(" ", "").isEmpty()) {
				targetFieldSetter.accept(e, defaultValue);
			} else if (MathUtils.isFloat(s)) {
				targetFieldSetter.accept(e, Float.parseFloat(s));
			}
		}, false, false, label);
	}

	protected <V> ContextMenu.ClickableContextMenuEntry addSwitcherContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, List<V> switcherValues, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<AbstractEditorElement, V> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, V> targetFieldSetter, @NotNull AbstractEditorElement.SwitcherContextMenuEntryLabelSupplier<V> labelSupplier) {
		return addTo.addClickableEntry(entryIdentifier, Component.literal(""), (menu, entry) ->
				{
					List<AbstractEditorElement> selectedElements = this.getFilteredSelectedElementList(selectedElementsFilter);
					ValueSwitcher<V> switcher = this.setupValueToggle("switcher", ValueSwitcher.fromList(switcherValues), selectedElements, entry.getStackMeta(), targetFieldGetter);
					this.editor.history.saveSnapshot();
					if (!selectedElements.isEmpty() && entry.getStackMeta().isFirstInStack()) {
						V next = switcher.next();
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
					ValueSwitcher<V> switcher = this.setupValueToggle("switcher", ValueSwitcher.fromList(switcherValues), selectedElements, entry.getStackMeta(), targetFieldGetter);
					return labelSupplier.get(menu, (ContextMenu.ClickableContextMenuEntry) entry, switcher.current());
				});
	}

	protected ContextMenu.ClickableContextMenuEntry addBooleanSwitcherContextMenuEntryTo(@NotNull ContextMenu addTo, @NotNull String entryIdentifier, @Nullable ConsumingSupplier<AbstractEditorElement, Boolean> selectedElementsFilter, @NotNull ConsumingSupplier<AbstractEditorElement, Boolean> targetFieldGetter, @NotNull BiConsumer<AbstractEditorElement, Boolean> targetFieldSetter, @NotNull String labelLocalizationKeyBase) {
		return addSwitcherContextMenuEntryTo(addTo, entryIdentifier, ListUtils.build(true, false), selectedElementsFilter, targetFieldGetter, targetFieldSetter, (menu, entry, switcherValue) -> {
			if (switcherValue && entry.isActive()) {
				return Component.translatable(labelLocalizationKeyBase + ".on");
			}
			return Component.translatable(labelLocalizationKeyBase + ".off");
		});
	}

	@SuppressWarnings("all")
	protected <T, E extends AbstractEditorElement> ValueSwitcher<T> setupValueToggle(String toggleIdentifier, ValueSwitcher<T> toggle, List<E> elements, ContextMenu.ContextMenuStackMeta stackMeta, ConsumingSupplier<E, T> defaultValue) {
		boolean hasProperty = stackMeta.getProperties().hasProperty(toggleIdentifier);
		ValueSwitcher<T> t = stackMeta.getProperties().putPropertyIfAbsentAndGet(toggleIdentifier, toggle);
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
		Component get(ContextMenu menu, ContextMenu.ClickableContextMenuEntry entry, V switcherValue);
	}

	public enum ResizeGrabberType {
		TOP,
		RIGHT,
		BOTTOM,
		LEFT
	}

}

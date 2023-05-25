package de.keksuccino.fancymenu.customization.element.editor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements.ManageRequirementsScreen;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.misc.ConsumingSupplier;
import de.keksuccino.fancymenu.misc.ValueToggle;
import de.keksuccino.fancymenu.rendering.AspectRatio;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.rendering.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.tooltip.Tooltip;
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
					.setIsActiveSupplier((menu, entry) -> (element.advancedX == null) && (element.advancedY == null))
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
			this.rightClickMenu.addSubMenuEntry("advanced_positioning", Component.literal(""), advancedPositioningMenu)
					.setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.features.advanced_positioning.desc")))
					.setLabelSupplier((menu, entry) -> {
						if (((element.advancedX != null) || (element.advancedY != null)) && !entry.getStackMeta().isPartOfStack()) {
							return Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning.active");
						} else {
							return Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning");
						}
					})
					.setStackable(true);

			advancedPositioningMenu.addClickableEntry("advanced_positioning_x", Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning.posx"), (menu, entry) -> {
				if (entry.getStackMeta().isFirstInStack()) {
					TextEditorScreen s = new TextEditorScreen(Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning.posx"), this.editor, null, (call) -> {
						if (call != null) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							if (call.replace(" ", "").equals("")) {
								this.element.advancedX = null;
							} else {
								this.element.advancedX = call;
							}
							this.element.baseX = 0;
							this.element.baseY = 0;
							this.element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
							entry.getStackMeta().getProperties().putProperty("x", this.element.advancedX);
							//TODO methode auf nicht-notify umschreiben
//							entry.getStackMeta().notifyNextInStack();
						}
					});
					s.multilineMode = false;
					if ((this.element.advancedX != null) && !entry.getStackMeta().isPartOfStack()) {
						s.setText(this.element.advancedX);
					}
					Minecraft.getInstance().setScreen(s);
				} else {
					String call = entry.getStackMeta().getProperties().getProperty("x", String.class);
					if (call != null) {
						this.element.advancedX = call;
						this.element.baseX = 0;
						this.element.baseY = 0;
						this.element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
						//TODO methode auf nicht-notify umschreiben
//						entry.getStackMeta().notifyNextInStack();
					}
				}
			}).setStackable(true);

			advancedPositioningMenu.addClickableEntry("advanced_positioning_y", Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning.posy"), (menu, entry) -> {
				if (entry.getStackMeta().isFirstInStack()) {
					TextEditorScreen s = new TextEditorScreen(Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning.posy"), this.editor, null, (call) -> {
						if (call != null) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							if (call.replace(" ", "").equals("")) {
								this.element.advancedY = null;
							} else {
								this.element.advancedY = call;
							}
							this.element.baseX = 0;
							this.element.baseY = 0;
							this.element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
							entry.getStackMeta().getProperties().putProperty("y", this.element.advancedY);
							//TODO methode auf nicht-notify umschreiben
//							entry.getStackMeta().notifyNextInStack();
						}
					});
					s.multilineMode = false;
					if ((this.element.advancedY != null) && !entry.getStackMeta().isPartOfStack()) {
						s.setText(this.element.advancedY);
					}
					Minecraft.getInstance().setScreen(s);
				} else {
					String call = entry.getStackMeta().getProperties().getProperty("y", String.class);
					if (call != null) {
						this.element.advancedY = call;
						this.element.baseX = 0;
						this.element.baseY = 0;
						this.element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
						//TODO methode auf nicht-notify umschreiben
//						entry.getStackMeta().notifyNextInStack();
					}
				}
			}).setStackable(true);

		}

		if (this.settings.isAdvancedSizingSupported()) {

			ContextMenu advancedSizingMenu = new ContextMenu();
			this.rightClickMenu.addSubMenuEntry("advanced_sizing", Component.literal(""), advancedSizingMenu)
					.setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.features.advanced_sizing.desc")))
					.setLabelSupplier((menu, entry) -> {
						if (((element.advancedX != null) || (element.advancedY != null)) && !entry.getStackMeta().isPartOfStack()) {
							return Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing.active");
						} else {
							return Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing");
						}
					})
					.setStackable(true);

			advancedSizingMenu.addClickableEntry("advanced_sizing_width", Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing.width"), (menu, entry) -> {
				if (entry.getStackMeta().isFirstInStack()) {
					TextEditorScreen s = new TextEditorScreen(Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing.width"), this.editor, null, (call) -> {
						if (call != null) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							if (call.replace(" ", "").equals("")) {
								this.element.width = 50;
								this.element.advancedWidth = null;
							} else {
								this.element.width = 50;
								this.element.advancedWidth = call;
							}
							entry.getStackMeta().getProperties().putProperty("width", this.element.advancedWidth);
							//TODO methode auf nicht-notify umschreiben
//							entry.getStackMeta().notifyNextInStack();
						}
					});
					s.multilineMode = false;
					if ((this.element.advancedWidth != null) && !entry.getStackMeta().isPartOfStack()) {
						s.setText(this.element.advancedWidth);
					}
					Minecraft.getInstance().setScreen(s);
				} else {
					String call = entry.getStackMeta().getProperties().getProperty("width", String.class);
					if (call != null) {
						this.element.width = 50;
						this.element.advancedWidth = call;
						//TODO methode auf nicht-notify umschreiben
//						entry.getStackMeta().notifyNextInStack();
					}
				}
			}).setStackable(true);

			advancedSizingMenu.addClickableEntry("advanced_sizing_height", Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing.height"), (menu, entry) -> {
				if (entry.getStackMeta().isFirstInStack()) {
					TextEditorScreen s = new TextEditorScreen(Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing.height"), this.editor, null, (call) -> {
						if (call != null) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							if (call.replace(" ", "").equals("")) {
								this.element.height = 50;
								this.element.advancedHeight = null;
							} else {
								this.element.height = 50;
								this.element.advancedHeight = call;
							}
							entry.getStackMeta().getProperties().putProperty("height", this.element.advancedHeight);
							//TODO methode auf nicht-notify umschreiben
//							entry.getStackMeta().notifyNextInStack();
						}
					});
					s.multilineMode = false;
					if ((this.element.advancedHeight != null) && !entry.getStackMeta().isPartOfStack()) {
						s.setText(this.element.advancedHeight);
					}
					Minecraft.getInstance().setScreen(s);
				} else {
					String call = entry.getStackMeta().getProperties().getProperty("height", String.class);
					if (call != null) {
						this.element.height = 50;
						this.element.advancedHeight = call;
						//TODO methode auf nicht-notify umschreiben
//						entry.getStackMeta().notifyNextInStack();
					}
				}
			}).setStackable(true);

		}

		this.rightClickMenu.addSeparatorEntry("separator_3").setStackable(true);

		if (this.settings.isStretchable()) {

			this.rightClickMenu.addClickableEntry("stretch_x", Component.literal(""), (menu, entry) ->
					{
						if (entry.getStackMeta().isFirstInStack()) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
						}
						if (!entry.getStackMeta().isPartOfStack()) {
							this.element.stretchX = !this.element.stretchX;
						} else {
							ValueToggle<Boolean> stretch = entry.getStackMeta().getProperties().putPropertyIfAbsentAndGet("stretch", new ValueToggle<Boolean>(0, false, true));
							if (entry.getStackMeta().isFirstInStack()) {
								stretch.next();
							}
							this.element.stretchX = stretch.current();
							//TODO methode auf nicht-notify umschreiben
//							entry.getStackMeta().notifyNextInStack();
						}
					})
					.setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.editor.object.stretch.x.desc")))
					.setLabelSupplier((menu, entry) -> {
						if (!entry.getStackMeta().isPartOfStack()) {
							if (element.stretchX && entry.isActive()) {
								return Component.translatable("fancymenu.editor.object.stretch.x.on");
							} else {
								return Component.translatable("fancymenu.editor.object.stretch.x.off");
							}
						} else {
							ValueToggle<Boolean> stretch = entry.getStackMeta().getProperties().putPropertyIfAbsentAndGet("stretch", new ValueToggle<Boolean>(0, false, true));
							if (stretch.current()) {
								return Component.translatable("fancymenu.editor.object.stretch.x.on");
							} else {
								return Component.translatable("fancymenu.editor.object.stretch.x.off");
							}
						}
					})
					.setIsActiveSupplier((menu, entry) -> element.advancedWidth == null)
					.setStackable(true);

			this.rightClickMenu.addClickableEntry("stretch_y", Component.literal(""), (menu, entry) ->
					{
						if (entry.getStackMeta().isFirstInStack()) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
						}
						if (!entry.getStackMeta().isPartOfStack()) {
							this.element.stretchY = !this.element.stretchY;
						} else {
							ValueToggle<Boolean> stretch = entry.getStackMeta().getProperties().putPropertyIfAbsentAndGet("stretch", new ValueToggle<Boolean>(0, false, true));
							if (entry.getStackMeta().isFirstInStack()) {
								stretch.next();
							}
							this.element.stretchY = stretch.current();
							//TODO methode auf nicht-notify umschreiben
//							entry.getStackMeta().notifyNextInStack();
						}
					})
					.setTooltipSupplier((menu, entry) -> Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.editor.object.stretch.y.desc")))
					.setLabelSupplier((menu, entry) -> {
						if (!entry.getStackMeta().isPartOfStack()) {
							if (element.stretchY && entry.isActive()) {
								return Component.translatable("fancymenu.editor.object.stretch.y.on");
							} else {
								return Component.translatable("fancymenu.editor.object.stretch.y.off");
							}
						} else {
							ValueToggle<Boolean> stretch = entry.getStackMeta().getProperties().putPropertyIfAbsentAndGet("stretch", new ValueToggle<Boolean>(0, false, true));
							if (stretch.current()) {
								return Component.translatable("fancymenu.editor.object.stretch.y.on");
							} else {
								return Component.translatable("fancymenu.editor.object.stretch.y.off");
							}
						}
					})
					.setIsActiveSupplier((menu, entry) -> element.advancedHeight == null)
					.setStackable(true);

		}

		this.rightClickMenu.addSeparatorEntry("separator_4").setStackable(true);

		if (this.settings.isLoadingRequirementsEnabled()) {

			this.rightClickMenu.addClickableEntry("loading_requirements", Component.translatable("fancymenu.editor.loading_requirement.elements.loading_requirements"), (menu, entry) ->
					{
						if (!entry.getStackMeta().isPartOfStack()) {
							ManageRequirementsScreen s = new ManageRequirementsScreen(this.editor, this.element.loadingRequirementContainer.copy(), (call) -> {
								if (call != null) {
									this.editor.history.saveSnapshot();
									this.element.loadingRequirementContainer = call;
								}
							});
							Minecraft.getInstance().setScreen(s);
						} else if (entry.getStackMeta().isFirstInStack()) {
							List<AbstractEditorElement> selected = new ArrayList<>();
							this.editor.getSelectedElements().forEach((element) -> {
								if (element.settings.isLoadingRequirementsEnabled()) selected.add(element);
							});
							List<LoadingRequirementContainer> containers = ObjectUtils.getOfAll(LoadingRequirementContainer.class, selected, consumes -> consumes.element.loadingRequirementContainer);
							LoadingRequirementContainer containerToUseInManager = new LoadingRequirementContainer();
							if ((containers.size() > 1) && ObjectUtils.equalsAll(containers.get(0), containers.subList(1, containers.size()-1).toArray())) {
								containerToUseInManager = containers.get(0).copy();
							} else {
								containerToUseInManager = this.element.loadingRequirementContainer.copy();
							}
							ManageRequirementsScreen s = new ManageRequirementsScreen(this.editor, containerToUseInManager, (call) -> {
								if (call != null) {
									this.editor.history.saveSnapshot();
									for (AbstractEditorElement e : selected) {
										e.element.loadingRequirementContainer = call.copy();
									}
								}
							});
							Minecraft.getInstance().setScreen(s);
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
					})
					.setStackable(true)
					.setShortcutTextSupplier((menu, entry) -> Component.translatable("fancymenu.editor.shortcuts.delete"));

		}

		this.rightClickMenu.addSeparatorEntry("separator_7").setStackable(true);

		if (this.settings.isDelayable()) {

			ContextMenu appearanceDelayMenu = new ContextMenu();
			this.rightClickMenu.addSubMenuEntry("appearance_delay", Component.translatable("fancymenu.element.general.appearance_delay"), appearanceDelayMenu)
					.setStackable(true);

			appearanceDelayMenu.addClickableEntry("appearance_delay_type", Component.translatable("fancymenu.element.general.appearance_delay.no_delay"), (menu, entry) ->
					{
						this.editor.history.saveSnapshot();
						if (!entry.getStackMeta().isPartOfStack()) {
							ValueToggle<AbstractElement.AppearanceDelay> toggle = new ValueToggle<>(this.element.appearanceDelay, AbstractElement.AppearanceDelay.NO_DELAY, AbstractElement.AppearanceDelay.FIRST_TIME, AbstractElement.AppearanceDelay.EVERY_TIME);
							this.element.appearanceDelay = toggle.next();
						} else if (entry.getStackMeta().isFirstInStack()) {
							ValueToggle<AbstractElement.AppearanceDelay> delay = entry.getStackMeta().getProperties().putPropertyIfAbsentAndGet("delay", new ValueToggle<AbstractElement.AppearanceDelay>(0, AbstractElement.AppearanceDelay.NO_DELAY, AbstractElement.AppearanceDelay.FIRST_TIME, AbstractElement.AppearanceDelay.EVERY_TIME));
							AbstractElement.AppearanceDelay d = delay.next();
							for (AbstractEditorElement e : this.editor.getSelectedElements()) {
								if (e.settings.isDelayable()) {
									e.element.appearanceDelay = d;
								}
							}
						}
					})
					.setLabelSupplier((menu, entry) -> {
						if (entry.getStackMeta().isPartOfStack()) {
							ValueToggle<AbstractElement.AppearanceDelay> delay = entry.getStackMeta().getProperties().putPropertyIfAbsentAndGet("delay", new ValueToggle<AbstractElement.AppearanceDelay>(0, AbstractElement.AppearanceDelay.NO_DELAY, AbstractElement.AppearanceDelay.FIRST_TIME, AbstractElement.AppearanceDelay.EVERY_TIME));
							return Component.translatable("fancymenu.element.general.appearance_delay." + delay.current().name);
						} else {
							return Component.translatable("fancymenu.element.general.appearance_delay." + this.element.appearanceDelay.name);
						}
					})
					.setStackable(true);

			appearanceDelayMenu.addClickableEntry("appearance_delay_seconds", Component.translatable("fancymenu.element.general.appearance_delay.seconds"), (menu, entry) -> {
				if (entry.getStackMeta().isFirstInStack()) {
					FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§f" + I18n.get("fancymenu.element.general.appearance_delay.seconds"), CharacterFilter.getDoubleCharacterFiler(), 240, (call) -> {
						if (call != null) {
							this.editor.history.saveSnapshot();
							for (AbstractEditorElement e : this.editor.getSelectedElements()) {
								if (e.settings.isDelayable()) {
									if (call.replace(" ", "").equals("")) {
										e.element.appearanceDelayInSeconds = 1.0F;
									} else if (MathUtils.isFloat(call)) {
										e.element.appearanceDelayInSeconds = Float.parseFloat(call);
									}
								}
							}
						}
					});
					if (!entry.getStackMeta().isPartOfStack()) {
						p.setText("" + this.element.appearanceDelayInSeconds);
					}
					PopupHandler.displayPopup(p);
				}
			}).setStackable(true);

			appearanceDelayMenu.addSeparatorEntry("separator_1").setStackable(true);

			appearanceDelayMenu.addClickableEntry("appearance_delay_fade_in", Component.translatable("fancymenu.element.general.appearance_delay.fade_in.off"), (menu, entry) ->
					{
						this.editor.history.saveSnapshot();
						if (!entry.getStackMeta().isPartOfStack()) {
							this.element.fadeIn = !this.element.fadeIn;
						} else {
							ValueToggle<Boolean> toggle = entry.getStackMeta().getProperties().putPropertyIfAbsentAndGet("fade_in", new ValueToggle<Boolean>(0, false, true));
							boolean b = toggle.next();
							for (AbstractEditorElement e : this.editor.getSelectedElements()) {
								if (e.settings.isDelayable()) {
									e.element.fadeIn = b;
								}
							}
						}
					})
					.setLabelSupplier((menu, entry) -> {
						ValueToggle<Boolean> toggle = entry.getStackMeta().getProperties().putPropertyIfAbsentAndGet("fade_in", new ValueToggle<Boolean>(0, false, true));
						if ((entry.getStackMeta().isPartOfStack()) ? toggle.current() : this.element.fadeIn) {
							return Component.translatable("fancymenu.element.general.appearance_delay.fade_in.on");
						} else {
							return Component.translatable("fancymenu.element.general.appearance_delay.fade_in.off");
						}
					})
					.setStackable(true);

			appearanceDelayMenu.addClickableEntry("appearance_delay_fade_in_speed", Component.translatable("fancymenu.element.general.appearance_delay.fade_in.speed"), (menu, entry) -> {
				if (entry.getStackMeta().isFirstInStack()) {
					FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§f" + I18n.get("fancymenu.element.general.appearance_delay.fade_in.speed"), CharacterFilter.getDoubleCharacterFiler(), 240, (call) -> {
						if (call != null) {
							this.editor.history.saveSnapshot();
							for (AbstractEditorElement e : this.editor.getSelectedElements()) {
								if (e.settings.isDelayable()) {
									if (call.replace(" ", "").equals("")) {
										e.element.fadeInSpeed = 1.0F;
									} else if (MathUtils.isFloat(call)) {
										e.element.fadeInSpeed = Float.parseFloat(call);
									}
								}
							}
						}
					});
					if (!entry.getStackMeta().isPartOfStack()) {
						p.setText("" + this.element.fadeInSpeed);
					}
					PopupHandler.displayPopup(p);
				}
			}).setStackable(true);

		}

		this.rightClickMenu.addSeparatorEntry("separator_8").setStackable(true);

	}

	@Override
	public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

		this.hovered = this.isMouseOver(mouseX, mouseY);

		this.element.render(pose, mouseX, mouseY, partial);

		this.renderDraggingNotAllowedOverlay(pose);

		//Update cursor
		ResizeGrabber hoveredGrabber = this.getHoveredResizeGrabber();
		GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), (hoveredGrabber != null) ? hoveredGrabber.getCursor() : CURSOR_NORMAL);

		this.renderBorder(pose, mouseX, mouseY, partial);

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
				if (this.isHovered() || this.isMultiSelected() || this.isGettingResized()) {
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
				if (this.settings.isMovable()) {
					this.element.baseX = this.leftMouseDownBaseX + diffX;
					this.element.baseY = this.leftMouseDownBaseY + diffY;
				} else {
					this.renderMovingNotAllowedTime = System.currentTimeMillis() + 2000;
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
				return AbstractEditorElement.this.settings.isResizeable() && AbstractEditorElement.this.settings.isResizeableY();
			}
			if ((this.type == ResizeGrabberType.LEFT) || (this.type == ResizeGrabberType.RIGHT)) {
				return AbstractEditorElement.this.settings.isResizeable() && AbstractEditorElement.this.settings.isResizeableX();
			}
			return false;
		}

		protected boolean isMouseOver(double mouseX, double mouseY) {
			return (mouseX >= this.getX()) && (mouseX <= this.getX() + this.width) && (mouseY >= this.getY()) && mouseY <= this.getY() + this.height;
		}

	}

	public enum ResizeGrabberType {
		TOP,
		RIGHT,
		BOTTOM,
		LEFT
	}

}

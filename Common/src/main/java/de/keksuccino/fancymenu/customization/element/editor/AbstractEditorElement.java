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
import de.keksuccino.fancymenu.misc.ConsumingSupplier;
import de.keksuccino.fancymenu.rendering.AspectRatio;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.AdvancedContextMenu;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.rendering.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
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
	public ContextMenu rightClickMenu = new ContextMenu();
	public EditorElementBorderDisplay topLeftDisplay = new EditorElementBorderDisplay(this, EditorElementBorderDisplay.DisplayPosition.TOP_LEFT, EditorElementBorderDisplay.DisplayPosition.LEFT_TOP, EditorElementBorderDisplay.DisplayPosition.BOTTOM_LEFT);
	public EditorElementBorderDisplay bottomRightDisplay = new EditorElementBorderDisplay(this, EditorElementBorderDisplay.DisplayPosition.BOTTOM_RIGHT, EditorElementBorderDisplay.DisplayPosition.RIGHT_BOTTOM, EditorElementBorderDisplay.DisplayPosition.TOP_RIGHT);
	public LayoutEditorScreen editor;
	protected boolean selected = false;
	protected boolean multiSelected = false;
	protected boolean hovered = false;
	protected boolean leftMouseDown = false;
	protected double leftMouseDownX = 0;
	protected double leftMouseDownY = 0;
	protected ResizeGrabber[] resizeGrabbers = new ResizeGrabber[]{new ResizeGrabber(ResizeGrabberType.TOP), new ResizeGrabber(ResizeGrabberType.RIGHT), new ResizeGrabber(ResizeGrabberType.BOTTOM), new ResizeGrabber(ResizeGrabberType.LEFT)};
	protected ResizeGrabber activeResizeGrabber = null;
	protected int resizeStartX = 0;
	protected int resizeStartY = 0;
	protected int resizeStartWidth = 0;
	protected int resizeStartHeight = 0;
	public long renderMovingNotAllowedTime = -1;

	public AbstractEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor, @Nullable EditorElementSettings settings) {
		this.settings = (settings != null) ? settings : new EditorElementSettings();
		this.settings.editorElement = this;
		this.editor = editor;
		this.element = element;
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
				for (AbstractEditorElement e : AbstractEditorElement.this.editor.getAllElements()) {
					if (e.isHovered()) {
						this.addClickableEntry("element_" + i, e.element.builder.getDisplayName(e.element), (menu, entry) -> {
							for (AbstractEditorElement e2 : AbstractEditorElement.this.editor.getAllElements()) {
								e2.resetElementStates();
							}
							e.setSelected(true);
						});
						i++;
					}
				}
				return super.openMenuAt(x, y);
			}
		};
		this.rightClickMenu.addSubMenuEntry("pick_element", Component.translatable("fancymenu.element.general.pick_element"), pickElementMenu)
				.setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.element.general.pick_element.desc")));

		this.rightClickMenu.addSeparatorEntry("separator_1");

		if (this.settings.isIdentifierCopyable()) {

			this.rightClickMenu.addClickableEntry("copy_id", Component.translatable("fancymenu.helper.editor.items.copyid"), (menu, entry) -> {
				Minecraft.getInstance().keyboardHandler.setClipboard(this.element.getInstanceIdentifier());
			}).setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.copyid.btn.desc")));

		}

		//TODO add vanilla button locator button in vanilla button editor element HERE

		this.rightClickMenu.addSeparatorEntry("separator_2");

		if (this.settings.isAnchorPointChangeable()) {

			ContextMenu anchorPointMenu = new ContextMenu();
			this.rightClickMenu.addSubMenuEntry("anchor_point", Component.translatable("fancymenu.editor.items.setorientation"), anchorPointMenu)
					.setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.editor.items.orientation.btndesc")))
					.setTicker((menu, entry, isPostTick) -> {
						entry.setActive((element.advancedX == null) && (element.advancedY == null));
					})
					.setStackable(true);

			if (this.settings.isElementAnchorPointAllowed()) {
				anchorPointMenu.addClickableEntry("anchor_point_element", ElementAnchorPoints.ELEMENT.getDisplayName(), (menu, entry) -> {
					if (entry.getStackMeta().isFirstInStack()) {
						FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), I18n.get("fancymenu.helper.editor.items.orientation.element.setidentifier"), null, 240, (call) -> {
							if (call != null) {
								AbstractEditorElement editorElement = this.editor.getElementByInstanceIdentifier(call);
								if (editorElement != null) {
									this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
									this.element.anchorPointElementIdentifier = editorElement.element.builder.getIdentifier();
									this.element.anchorPointElement = editorElement.element;
									this.setAnchorPoint(ElementAnchorPoints.ELEMENT);
									entry.getStackMeta().getProperties().putProperty("element", editorElement);
									entry.getStackMeta().notifyNextInStack();
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
					} else {
						AbstractEditorElement editorElement = entry.getStackMeta().getProperties().getProperty("element", AbstractEditorElement.class);
						if (editorElement != null) {
							this.element.anchorPointElementIdentifier = editorElement.element.builder.getIdentifier();
							this.element.anchorPointElement = editorElement.element;
							this.setAnchorPoint(ElementAnchorPoints.ELEMENT);
							entry.getStackMeta().notifyNextInStack();
						}
					}
				}).setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.orientation.element.btn.desc")))
						.setStackable(true);
			}

			anchorPointMenu.addSeparatorEntry("separator_1").setStackable(true);

			for (ElementAnchorPoint p : ElementAnchorPoints.getAnchorPoints()) {
				if (p != ElementAnchorPoints.ELEMENT) {
					anchorPointMenu.addClickableEntry("anchor_point_" + p.getName().replace("-", "_"), p.getDisplayName(), (menu, entry) -> {
						if (entry.getStackMeta().isFirstInStack()) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
						}
						anchorPointMenu.closeMenu();
						this.setAnchorPoint(p);
						entry.getStackMeta().notifyNextInStack();
					}).setStackable(true);
				}
			}

		}

		if (this.settings.isAdvancedPositioningSupported()) {

			ContextMenu advancedPositioningMenu = new ContextMenu();
			this.rightClickMenu.addSubMenuEntry("advanced_positioning", Component.literal(""), advancedPositioningMenu)
					.setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.features.advanced_positioning.desc")))
					.setTicker((menu, entry, isPostTick) -> {
						if (entry instanceof ContextMenu.SubMenuContextMenuEntry e) {
							if ((element.advancedX != null) || (element.advancedY != null)) {
								e.setLabel(Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning.active"));
							} else {
								e.setLabel(Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning"));
							}
						}
					});

			advancedPositioningMenu.addClickableEntry("advanced_positioning_x", Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning.posx"), (menu, entry) -> {
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
					}
				});
				s.multilineMode = false;
				if (this.element.advancedX != null) {
					s.setText(this.element.advancedX);
				}
				Minecraft.getInstance().setScreen(s);
			});

			advancedPositioningMenu.addClickableEntry("advanced_positioning_y", Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning.posy"), (menu, entry) -> {
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
					}
				});
				s.multilineMode = false;
				if (this.element.advancedY != null) {
					s.setText(this.element.advancedY);
				}
				Minecraft.getInstance().setScreen(s);
			});

		}

		if (this.settings.isAdvancedSizingSupported()) {

			ContextMenu advancedSizingMenu = new ContextMenu();
			this.rightClickMenu.addSubMenuEntry("advanced_sizing", Component.literal(""), advancedSizingMenu)
					.setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.helper.editor.items.features.advanced_sizing.desc")))
					.setTicker((menu, entry, isPostTick) -> {
						if (entry instanceof ContextMenu.SubMenuContextMenuEntry e) {
							if ((element.advancedX != null) || (element.advancedY != null)) {
								e.setLabel(Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing.active"));
							} else {
								e.setLabel(Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing"));
							}
						}
					});

			advancedSizingMenu.addClickableEntry("advanced_sizing_width", Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing.width"), (menu, entry) -> {
				TextEditorScreen s = new TextEditorScreen(Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing.width"), this.editor, null, (call) -> {
					if (call != null) {
						if (call.replace(" ", "").equals("")) {
							if ((this.element.advancedWidth != null) || (this.element.width != 50)) {
								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							}
							this.element.width = 50;
							this.element.advancedWidth = null;
						} else {
							if (!call.equals(this.element.advancedWidth) || (this.element.width != 50)) {
								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							}
							this.element.width = 50;
							this.element.advancedWidth = call;
						}
					}
				});
				s.multilineMode = false;
				if (this.element.advancedWidth != null) {
					s.setText(this.element.advancedWidth);
				}
				Minecraft.getInstance().setScreen(s);
			});

			advancedSizingMenu.addClickableEntry("advanced_sizing_height", Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing.height"), (menu, entry) -> {
				TextEditorScreen s = new TextEditorScreen(Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing.height"), this.editor, null, (call) -> {
					if (call != null) {
						if (call.replace(" ", "").equals("")) {
							if ((this.element.advancedHeight != null) || (this.element.height != 50)) {
								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							}
							this.element.height = 50;
							this.element.advancedHeight = null;
						} else {
							if (!call.equals(this.element.advancedHeight) || (this.element.height != 50)) {
								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							}
							this.element.height = 50;
							this.element.advancedHeight = call;
						}
					}
				});
				s.multilineMode = false;
				if (this.element.advancedHeight != null) {
					s.setText(this.element.advancedHeight);
				}
				Minecraft.getInstance().setScreen(s);
			});

		}

		this.rightClickMenu.addSeparatorEntry("separator_3").setStackable(true);

		if (this.settings.isStretchable()) {

			//TODO schauen, ob stretch stackable gemacht werden kann
			this.rightClickMenu.addClickableEntry("stretch_x", false, Component.literal(""), null, Boolean.class, (entry, inherited, pass) -> {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				this.element.stretchX = !this.element.stretchX;
			})
			.setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.editor.object.stretch.x.desc")))
			.setTicker((entry) -> {
				AdvancedContextMenu.ClickableMenuEntry<?> e = (AdvancedContextMenu.ClickableMenuEntry<?>) entry;
				e.getButton().active = element.advancedWidth == null;
				if (element.stretchX && e.getButton().active) {
					e.setLabel(Component.translatable("fancymenu.editor.object.stretch.x.on"));
				} else {
					e.setLabel(Component.translatable("fancymenu.editor.object.stretch.x.off"));
				}
			});

			this.rightClickMenu.addClickableEntry("stretch_y", false, Component.literal(""), null, Boolean.class, (entry, inherited, pass) -> {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				this.element.stretchY = !this.element.stretchY;
			})
			.setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.editor.object.stretch.y.desc")))
			.setTicker((entry) -> {
				AdvancedContextMenu.ClickableMenuEntry<?> e = (AdvancedContextMenu.ClickableMenuEntry<?>) entry;
				e.getButton().active = element.advancedHeight == null;
				if (element.stretchY && e.getButton().active) {
					e.setLabel(Component.translatable("fancymenu.editor.object.stretch.y.on"));
				} else {
					e.setLabel(Component.translatable("fancymenu.editor.object.stretch.y.off"));
				}
			});

		}

		this.rightClickMenu.addSeparatorEntry("separator_4", true);

		if (this.settings.isLoadingRequirementsEnabled()) {

			this.rightClickMenu.addClickableEntry("loading_requirements", false, Component.translatable("fancymenu.editor.loading_requirement.elements.loading_requirements"), null, Boolean.class, (entry, inherited, pass) -> {
				ManageRequirementsScreen s = new ManageRequirementsScreen(this.editor, this.element.loadingRequirementContainer, (call) -> {});
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				Minecraft.getInstance().setScreen(s);
			}).setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.editor.loading_requirement.elements.loading_requirements.desc")));

		}

		this.rightClickMenu.addSeparatorEntry("separator_5", true);

		if (this.settings.isOrderable()) {

			this.rightClickMenu.addClickableEntry("move_up_element", false, Component.translatable("fancymenu.editor.object.moveup"), null, Boolean.class, (entry, inherited, pass) -> {
				AbstractEditorElement o = this.editor.moveElementUp(this);
				if (o != null) {
					((AdvancedContextMenu.ClickableMenuEntry<?>)entry).setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.editor.object.moveup.desc", I18n.get("fancymenu.editor.object.moveup.desc.subtext", o.element.builder.getDisplayName(o.element).getString()))));
				}
			}).setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.editor.object.moveup.desc")));

			this.rightClickMenu.addClickableEntry("move_down_element", false, Component.translatable("fancymenu.editor.object.movedown"), null, Boolean.class, (entry, inherited, pass) -> {
				AbstractEditorElement o = this.editor.moveElementDown(this);
				if (o != null) {
					((AdvancedContextMenu.ClickableMenuEntry<?>)entry).setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.editor.object.movedown.desc", I18n.get("fancymenu.editor.object.movedown.desc.subtext", o.element.builder.getDisplayName(o.element).getString()))));
				}
			}).setTooltip(Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.editor.object.movedown.desc")));

		}

		this.rightClickMenu.addSeparatorEntry("separator_6", true);

		if (this.settings.isCopyable()) {

			this.rightClickMenu.addClickableEntry("copy_element", true, Component.translatable("fancymenu.editor.edit.copy"), null, List.class, (entry, inherited, pass) -> {
				List<AbstractEditorElement> elements = (inherited != null) ? inherited : new ArrayList<>();
				elements.add(this);
				if (entry.isLastInStack()) {
					this.editor.copyElementsToClipboard(elements.toArray(new AbstractEditorElement[0]));
				} else {
					pass.accept(elements);
				}
			});

		}

		if (this.settings.isDestroyable()) {

			this.rightClickMenu.addClickableEntry("delete_element", true, Component.translatable("fancymenu.editor.items.delete"), null, Boolean.class, (entry, inherited, pass) -> {
				if (inherited == null) {
					this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
					pass.accept(this.deleteElement());
				} else if (inherited) {
					pass.accept(this.deleteElement());
				}
			});

		}

		this.rightClickMenu.addSeparatorEntry("separator_7", true);

		if (this.settings.isDelayable()) {

			AdvancedContextMenu appearanceDelayMenu = new AdvancedContextMenu();
			this.rightClickMenu.addClickableEntry("appearance_delay", true, Component.translatable("fancymenu.element.general.appearance_delay"), appearanceDelayMenu, Boolean.class, (entry, inherited, pass) -> {
				if (inherited == null) {
					appearanceDelayMenu.getContextMenu().setParentButton(entry.getButton());
					appearanceDelayMenu.openMenu(0, entry.getButton().y);
				}
				pass.accept(true);
			});

			appearanceDelayMenu.addClickableEntry("appearance_delay_type", true, Component.translatable("fancymenu.element.general.appearance_delay.no_delay"), null, Boolean.class, (entry, inherited, pass) -> {
				if (entry.isFirstInStack()) {
					this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				}
				if (!entry.getEntryStackMeta().hasProperty("delay")) {
					entry.getEntryStackMeta().putProperty("delay", entry.isPartOfStack() ? AbstractElement.AppearanceDelay.NO_DELAY : this.element.appearanceDelay);
				}
				AbstractElement.AppearanceDelay delay = entry.getEntryStackMeta().getProperty("delay", AbstractElement.AppearanceDelay.class);
				this.element.appearanceDelay = AbstractElement.AppearanceDelay.next(delay);
				if (entry.isLastInStack()) {
					entry.getEntryStackMeta().putProperty("delay", this.element.appearanceDelay);
				}
				pass.accept(true);
			}).setTicker((entry) -> {
				AdvancedContextMenu.ClickableMenuEntry<?> e = (AdvancedContextMenu.ClickableMenuEntry<?>) entry;
				AbstractElement.AppearanceDelay delay = this.element.appearanceDelay;
				if (e.isPartOfStack()) {
					AbstractElement.AppearanceDelay stackedDelay = e.getEntryStackMeta().getProperty("delay", AbstractElement.AppearanceDelay.class);
					delay = (stackedDelay != null) ? stackedDelay : AbstractElement.AppearanceDelay.NO_DELAY;
				}
				e.setLabel(Component.translatable("fancymenu.element.general.appearance_delay." + delay.name));
			});

			appearanceDelayMenu.addClickableEntry("appearance_delay_seconds", true, Component.translatable("fancymenu.element.general.appearance_delay.seconds"), null, Float.class, (entry, inherited, pass) -> {
				if (entry.isFirstInStack()) {
					this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				}
				if (inherited == null) {
					FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§f" + I18n.get("fancymenu.element.general.appearance_delay.seconds"), CharacterFilter.getDoubleCharacterFiler(), 240, (call) -> {
						if (call != null) {
							if (call.replace(" ", "").equals("")) {
								this.element.appearanceDelayInSeconds = 1.0F;
							} else if (MathUtils.isFloat(call)) {
								this.element.appearanceDelayInSeconds = Float.parseFloat(call);
							}
							pass.accept(this.element.appearanceDelayInSeconds);
						}
					});
					if (!entry.isPartOfStack()) {
						p.setText("" + this.element.appearanceDelayInSeconds);
					}
					PopupHandler.displayPopup(p);
				} else {
					this.element.appearanceDelayInSeconds = inherited;
					pass.accept(inherited);
				}
			});

			appearanceDelayMenu.addSeparatorEntry("separator_1", true);

			appearanceDelayMenu.addClickableEntry("appearance_delay_fade_in", true, Component.translatable("fancymenu.element.general.appearance_delay.fade_in.off"), null, Boolean.class, (entry, inherited, pass) -> {
				if (entry.isFirstInStack()) {
					this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				}
				if (!entry.getEntryStackMeta().hasProperty("fade_in")) {
					entry.getEntryStackMeta().putProperty("fade_in", entry.isPartOfStack() ? false : this.element.fadeIn);
				}
				boolean fade = entry.getEntryStackMeta().getBooleanProperty("fade_in");
				this.element.fadeIn = !fade;
				if (entry.isLastInStack()) {
					entry.getEntryStackMeta().putProperty("fade_in", this.element.fadeIn);
				}
				pass.accept(true);
			}).setTicker((entry) -> {
				AdvancedContextMenu.ClickableMenuEntry<?> e = (AdvancedContextMenu.ClickableMenuEntry<?>) entry;
				boolean fade = this.element.fadeIn;
				if (e.isPartOfStack()) {
					Boolean stackedFade = e.getEntryStackMeta().getProperty("fade_in", Boolean.class);
					fade = (stackedFade != null) ? stackedFade : false;
				}
				if (fade) {
					e.setLabel(Component.translatable("fancymenu.element.general.appearance_delay.fade_in.on"));
				} else {
					e.setLabel(Component.translatable("fancymenu.element.general.appearance_delay.fade_in.off"));
				}
			});

			appearanceDelayMenu.addClickableEntry("appearance_delay_fade_in_speed", true, Component.translatable("fancymenu.element.general.appearance_delay.fade_in.speed"), null, Float.class, (entry, inherited, pass) -> {
				if (entry.isFirstInStack()) {
					this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				}
				if (inherited == null) {
					FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§f" + I18n.get("fancymenu.element.general.appearance_delay.fade_in.speed"), CharacterFilter.getDoubleCharacterFiler(), 240, (call) -> {
						if (call != null) {
							if (call.replace(" ", "").equals("")) {
								this.element.fadeInSpeed = 1.0F;
							} else if (MathUtils.isFloat(call)) {
								this.element.fadeInSpeed = Float.parseFloat(call);
							}
							pass.accept(this.element.fadeInSpeed);
						}
					});
					if (!entry.isPartOfStack()) {
						p.setText("" + this.element.fadeInSpeed);
					}
					PopupHandler.displayPopup(p);
				} else {
					this.element.fadeInSpeed = inherited;
					pass.accept(inherited);
				}
			});

		}

		this.rightClickMenu.addSeparatorEntry("separator_8", true);

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
		this.element.baseX = p.getDefaultElementPositionX(this.element);
		this.element.baseY = p.getDefaultElementPositionY(this.element);
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
					this.leftMouseDownX = mouseX;
					this.leftMouseDownY = mouseY;
					this.resizeStartX = this.element.baseX;
					this.resizeStartY = this.element.baseY;
					this.resizeStartWidth = this.element.width;
					this.resizeStartHeight = this.element.height;
				}
			}
			if (this.rightClickMenu.isOpen() && !this.rightClickMenu.isHovered()) {
				this.rightClickMenu.closeMenu();
			}
		}
		if (button == 1) {
			if (this.isHovered() && !this.isGettingResized()) {
				this.rightClickMenu.openMenuAtMouseScaled();
			}
			if (!this.isHovered()) {
				this.rightClickMenu.closeMenu();
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
			int diffX = (int)-(this.leftMouseDownX - mouseX);
			int diffY = (int)-(this.leftMouseDownY - mouseY);
			if (this.leftMouseDown && !this.isGettingResized()) {
				if (this.settings.isMovable()) {
					this.element.baseX += diffX;
					this.element.baseY += diffY;
				} else {
					this.renderMovingNotAllowedTime = System.currentTimeMillis() + 2000;
				}
			}
			//TODO add SHIFT-resize (aspect ratio)
			if (this.leftMouseDown && this.isGettingResized()) {
				if ((this.activeResizeGrabber.type == ResizeGrabberType.LEFT) || (this.activeResizeGrabber.type == ResizeGrabberType.RIGHT)) {
					int i = this.resizeStartWidth + diffX;
					if (i >= 1) {
						this.element.width = i;
						this.element.baseX = this.resizeStartX + this.element.anchorPoint.getResizePositionOffsetX(this.element, diffX, this.activeResizeGrabber.type);
					}
				}
				if ((this.activeResizeGrabber.type == ResizeGrabberType.TOP) || (this.activeResizeGrabber.type == ResizeGrabberType.BOTTOM)) {
					int i = this.resizeStartHeight + diffY;
					if (i >= 1) {
						this.element.height = i;
						this.element.baseY = this.resizeStartY + this.element.anchorPoint.getResizePositionOffsetY(this.element, diffY, this.activeResizeGrabber.type);
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
		return this.hovered || this.rightClickMenu.isUserNavigatingInMenu();
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
	protected ResizeGrabber getHoveredResizeGrabber() {
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

	protected class ResizeGrabber extends GuiComponent implements Renderable {

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

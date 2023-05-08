package de.keksuccino.fancymenu.customization.element.editor;

import java.awt.Color;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.elements.button.LayoutVanillaButton;
import de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements.ManageRequirementsScreen;
import de.keksuccino.fancymenu.misc.ConsumingSupplier;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.AdvancedContextMenu;
import de.keksuccino.fancymenu.rendering.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public abstract class AbstractEditorElement extends GuiComponent implements Renderable, GuiEventListener {

	private static final Logger LOGGER = LogManager.getLogger();

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
	public AdvancedContextMenu menu = new AdvancedContextMenu();
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
	
	public void init() {

		this.menu.closeMenu();
		this.menu.clearEntries();
		this.topLeftDisplay.clearLines();
		this.bottomRightDisplay.clearLines();

		this.topLeftDisplay.addLine("anchor_point", () -> Component.translatable("fancymenu.element.border_display.anchor_point", this.element.anchorPoint.getDisplayName()));
		this.topLeftDisplay.addLine("pos_x", () -> Component.translatable("fancymenu.element.border_display.pos_x", "" + this.getX()));
		this.topLeftDisplay.addLine("width", () -> Component.translatable("fancymenu.element.border_display.width", "" + this.getWidth()));

		this.bottomRightDisplay.addLine("pos_y", () -> Component.translatable("fancymenu.element.border_display.pos_y", "" + this.getY()));
		this.bottomRightDisplay.addLine("height", () -> Component.translatable("fancymenu.element.border_display.height", "" + this.getHeight()));

		//TODO add support for vanilla/deepcuz elements (vanilla buttons, etc.)
		this.menu.addClickableEntry("copy_id", false, Component.translatable("fancymenu.helper.editor.items.copyid"), null, Boolean.class, (entry, inherited, pass) -> {
			Minecraft.getInstance().keyboardHandler.setClipboard(this.element.getInstanceIdentifier());
		}).setTooltip(TooltipHandler.splitLocalizedTooltipLines("fancymenu.helper.editor.items.copyid.btn.desc"));

		//TODO add vanilla button locator button in vanilla button editor element HERE

		this.menu.addSeparatorEntry("separator_1", true);

		if (this.settings.isAnchorPointChangeable()) {

			AdvancedContextMenu anchorPointMenu = new AdvancedContextMenu();
			this.menu.addClickableEntry("anchor_point", true, Component.translatable("helper.creator.items.setorientation"), anchorPointMenu, Boolean.class, (entry, inherited, pass) -> {
				if (inherited == null) {
					anchorPointMenu.openMenu(0,0);
				}
				pass.accept(true);
			})
			.setTooltip(TooltipHandler.splitLocalizedTooltipLines("helper.creator.items.orientation.btndesc"))
			.setTicker((entry) -> {
				((AdvancedContextMenu.ClickableMenuEntry<?>)entry).getButton().active = (element.advancedX == null) && (element.advancedY == null);
			});

			if (this.settings.isElementAnchorPointAllowed()) {
				anchorPointMenu.addClickableEntry("anchor_point_element", true, ElementAnchorPoints.ELEMENT.getDisplayName(), null, AbstractElement.class, (entry, inherited, pass) -> {
					if (inherited == null) {
						FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), I18n.get("fancymenu.helper.editor.items.orientation.element.setidentifier"), null, 240, (call) -> {
							if (call != null) {
								AbstractEditorElement editorElement = this.editor.getElementByInstanceIdentifier(call);
								if (editorElement != null) {
									this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
									this.element.anchorPointElementIdentifier = editorElement.element.builder.getIdentifier();
									this.element.anchorPointElement = editorElement.element;
									this.setAnchorPoint(ElementAnchorPoints.ELEMENT);
									pass.accept(editorElement.element);
								} else {
									PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, TooltipHandler.splitLocalizedTooltipStringLines("fancymenu.helper.editor.items.orientation.element.setidentifier.identifiernotfound")));
								}
							}
						});
						if (!entry.isPartOfStack() && (this.element.anchorPointElementIdentifier != null)) {
							p.setText(this.element.anchorPointElementIdentifier);
						}
						PopupHandler.displayPopup(p);
						anchorPointMenu.closeMenu();
					} else {
						this.element.anchorPointElementIdentifier = inherited.builder.getIdentifier();
						this.element.anchorPointElement = inherited;
						this.setAnchorPoint(ElementAnchorPoints.ELEMENT);
						pass.accept(inherited);
					}
				}).setTooltip(TooltipHandler.splitLocalizedTooltipLines("fancymenu.helper.editor.items.orientation.element.btn.desc"));
			}

			anchorPointMenu.addSeparatorEntry("separator_1", true);

			for (ElementAnchorPoint p : ElementAnchorPoints.getAnchorPoints()) {
				if (p != ElementAnchorPoints.ELEMENT) {
					anchorPointMenu.addClickableEntry("anchor_point_" + p.getName().replace("-", "_"), true, p.getDisplayName(), null, Boolean.class, (entry, inherited, pass) -> {
						if (inherited == null) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
						}
						anchorPointMenu.closeMenu();
						this.setAnchorPoint(p);
						pass.accept(true);
					});
				}
			}

		}

		if (this.settings.isAdvancedPositioningSupported()) {

			AdvancedContextMenu advancedPositioningMenu = new AdvancedContextMenu();
			this.menu.addClickableEntry("advanced_positioning", false, Component.literal(""), advancedPositioningMenu, Boolean.class, (entry, inherited, pass) -> {
				advancedPositioningMenu.openMenu(0,0);
			})
			.setTooltip(TooltipHandler.splitLocalizedTooltipLines("fancymenu.helper.editor.items.features.advanced_positioning.desc"))
			.setTicker((entry) -> {
				AdvancedContextMenu.ClickableMenuEntry<?> e = (AdvancedContextMenu.ClickableMenuEntry<?>) entry;
				if ((element.advancedX != null) || (element.advancedY != null)) {
					e.setLabel(Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning.active"));
				} else {
					e.setLabel(Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning"));
				}
			});

			advancedPositioningMenu.addClickableEntry("advanced_positioning_x", false, Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning.posx"), null, Boolean.class, (entry, inherited, pass) -> {
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

			advancedPositioningMenu.addClickableEntry("advanced_positioning_y", false, Component.translatable("fancymenu.helper.editor.items.features.advanced_positioning.posy"), null, Boolean.class, (entry, inherited, pass) -> {
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

			AdvancedContextMenu advancedSizingMenu = new AdvancedContextMenu();
			this.menu.addClickableEntry("advanced_sizing", false, Component.literal(""), advancedSizingMenu, Boolean.class, (entry, inherited, pass) -> {
						advancedSizingMenu.openMenu(0,0);
					})
					.setTooltip(TooltipHandler.splitLocalizedTooltipLines("fancymenu.helper.editor.items.features.advanced_sizing.desc"))
					.setTicker((entry) -> {
						AdvancedContextMenu.ClickableMenuEntry<?> e = (AdvancedContextMenu.ClickableMenuEntry<?>) entry;
						if ((element.advancedX != null) || (element.advancedY != null)) {
							e.setLabel(Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing.active"));
						} else {
							e.setLabel(Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing"));
						}
					});

			advancedSizingMenu.addClickableEntry("advanced_sizing_width", false, Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing.width"), null, Boolean.class, (entry, inherited, pass) -> {
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
//							if ((this instanceof LayoutVanillaButton) && (this.element.anchorPoint == ElementAnchorPoints.VANILLA)) {
//								this.element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
//								this.element.baseX = 0;
//								this.element.baseY = 0;
//							}
						}
					}
				});
				s.multilineMode = false;
				if (this.element.advancedWidth != null) {
					s.setText(this.element.advancedWidth);
				}
				Minecraft.getInstance().setScreen(s);
			});

			advancedSizingMenu.addClickableEntry("advanced_sizing_height", false, Component.translatable("fancymenu.helper.editor.items.features.advanced_sizing.height"), null, Boolean.class, (entry, inherited, pass) -> {
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
//							if ((this instanceof LayoutVanillaButton) && (this.element.anchorPoint == ElementAnchorPoints.VANILLA)) {
//								this.element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
//								this.element.baseX = 0;
//								this.element.baseY = 0;
//							}
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

		this.menu.addSeparatorEntry("separator_2", true);

		if (this.settings.isStretchable()) {

			this.menu.addClickableEntry("stretch_x", false, Component.literal(""), null, Boolean.class, (entry, inherited, pass) -> {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				this.element.stretchX = !this.element.stretchX;
			})
			.setTooltip(TooltipHandler.splitLocalizedTooltipLines("helper.creator.object.stretch.x.desc"))
			.setTicker((entry) -> {
				AdvancedContextMenu.ClickableMenuEntry<?> e = (AdvancedContextMenu.ClickableMenuEntry<?>) entry;
				e.getButton().active = element.advancedWidth == null;
				if (element.stretchX && e.getButton().active) {
					e.setLabel(Component.translatable("helper.creator.object.stretch.x.on"));
				} else {
					e.setLabel(Component.translatable("helper.creator.object.stretch.x.off"));
				}
			});

			this.menu.addClickableEntry("stretch_y", false, Component.literal(""), null, Boolean.class, (entry, inherited, pass) -> {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				this.element.stretchY = !this.element.stretchY;
			})
			.setTooltip(TooltipHandler.splitLocalizedTooltipLines("helper.creator.object.stretch.y.desc"))
			.setTicker((entry) -> {
				AdvancedContextMenu.ClickableMenuEntry<?> e = (AdvancedContextMenu.ClickableMenuEntry<?>) entry;
				e.getButton().active = element.advancedHeight == null;
				if (element.stretchY && e.getButton().active) {
					e.setLabel(Component.translatable("helper.creator.object.stretch.y.on"));
				} else {
					e.setLabel(Component.translatable("helper.creator.object.stretch.y.off"));
				}
			});

		}

		this.menu.addSeparatorEntry("separator_3", true);

		//TODO add layers entry HERE

		this.menu.addSeparatorEntry("separator_4", true);

		if (this.settings.isLoadingRequirementsEnabled()) {

			this.menu.addClickableEntry("loading_requirements", false, Component.translatable("fancymenu.editor.loading_requirement.elements.loading_requirements"), null, Boolean.class, (entry, inherited, pass) -> {
				ManageRequirementsScreen s = new ManageRequirementsScreen(this.editor, this.element.loadingRequirementContainer, (call) -> {});
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				Minecraft.getInstance().setScreen(s);
			}).setTooltip(TooltipHandler.splitLocalizedTooltipLines("fancymenu.editor.loading_requirement.elements.loading_requirements.desc"));

		}

		this.menu.addSeparatorEntry("separator_5", true);

		//TODO hier bei orderable entry weiter machen

	}

	@Override
	public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

		this.hovered = this.isMouseOver(mouseX, mouseY);

		this.element.render(pose, mouseX, mouseY, partial);

		//Update cursor
		ResizeGrabber hoveredGrabber = this.getHoveredResizeGrabber();
		GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), (hoveredGrabber != null) ? hoveredGrabber.getCursor() : CURSOR_NORMAL);

		this.renderBorder(pose, mouseX, mouseY, partial);

		//TODO render menu in editor

	}

	protected void renderBorder(PoseStack pose, int mouseX, int mouseY, float partial) {

		if (this.isHovered() || this.isSelected() || this.isMultiSelected()) {

			//TOP
			fill(pose, this.getX() + 1, this.getY(), this.getX() + this.getWidth() - 2, this.getY() + 1, BORDER_COLOR.get(this));
			//BOTTOM
			fill(pose, this.getX() + 1, this.getY() + this.getHeight() - 1, this.getX() + this.getWidth() - 2, this.getY() + this.getHeight(), BORDER_COLOR.get(this));
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
		this.selected = false;
		this.multiSelected = false;
		this.leftMouseDown = false;
		this.activeResizeGrabber = null;
		this.menu.closeMenu();
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
			if (!this.menu.isUserNavigatingInMenu()) {
				this.activeResizeGrabber = this.getHoveredResizeGrabber();
				if (this.isHovered() || this.isGettingResized()) {
					this.leftMouseDown = true;
					this.leftMouseDownX = mouseX;
					this.leftMouseDownY = mouseY;
					this.resizeStartX = this.element.baseX;
					this.resizeStartY = this.element.baseY;
					this.resizeStartWidth = this.element.width;
					this.resizeStartHeight = this.element.height;
				}
			}
			if (this.menu.isOpen() && !this.menu.isHovered()) {
				this.menu.closeMenu();
			}
			return true;
		}
		if (button == 1) {
			if (this.isHovered() && !this.isGettingResized()) {
				this.menu.openMenuAtMouse();
				return true;
			}
			if (!this.isHovered()) {
				this.menu.closeMenu();
			}
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0) {
			this.leftMouseDown = false;
			this.activeResizeGrabber = null;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double $$3, double $$4) {
		if (button == 0) {
			int diffX = (int)-(this.leftMouseDownX - mouseX);
			int diffY = (int)-(this.leftMouseDownY - mouseY);
			if (this.leftMouseDown && !this.isGettingResized()) {
				this.element.baseX += diffX;
				this.element.baseY += diffY;
				return true;
			}
			if (this.leftMouseDown && this.isGettingResized()) {
				if ((this.activeResizeGrabber.type == ResizeGrabberType.LEFT) || (this.activeResizeGrabber.type == ResizeGrabberType.RIGHT)) {
					this.element.width = this.resizeStartWidth + diffX;
					this.element.baseX = this.resizeStartX + this.element.anchorPoint.getResizePositionOffsetX(this.element, diffX, this.activeResizeGrabber.type);
				}
				if ((this.activeResizeGrabber.type == ResizeGrabberType.TOP) || (this.activeResizeGrabber.type == ResizeGrabberType.BOTTOM)) {
					this.element.height = this.resizeStartHeight + diffY;
					this.element.baseY = this.resizeStartY + this.element.anchorPoint.getResizePositionOffsetY(this.element, diffY, this.activeResizeGrabber.type);
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
	}

	public boolean isMultiSelected() {
		return this.multiSelected;
	}

	public void setMultiSelected(boolean multiSelected) {
		this.multiSelected = multiSelected;
	}

	public boolean isHovered() {
		return this.hovered;
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

	public boolean isGettingResized() {
		return this.activeResizeGrabber != null;
	}

	@Nullable
	protected ResizeGrabber getHoveredResizeGrabber() {
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

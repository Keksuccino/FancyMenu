//package de.keksuccino.fancymenu.customization.element.editor;
//
//import com.mojang.blaze3d.vertex.PoseStack;
//import de.keksuccino.fancymenu.FancyMenu;
//import de.keksuccino.fancymenu.customization.element.AbstractElement;
//import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
//import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorHistory;
//import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
//import de.keksuccino.fancymenu.customization.layout.editor.elements.button.LayoutVanillaButton;
//import de.keksuccino.fancymenu.customization.layout.editor.loadingrequirements.ManageRequirementsScreen;
//import de.keksuccino.fancymenu.rendering.ui.contextmenu.ContextMenu;
//import de.keksuccino.fancymenu.rendering.ui.popup.FMNotificationPopup;
//import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
//import de.keksuccino.fancymenu.rendering.ui.popup.FMYesNoPopup;
//import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
//import de.keksuccino.fancymenu.rendering.ui.widget.Button;
//import de.keksuccino.konkrete.gui.content.AdvancedButton;
//import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
//import de.keksuccino.konkrete.input.CharacterFilter;
//import de.keksuccino.konkrete.input.KeyboardHandler;
//import de.keksuccino.konkrete.input.MouseInput;
//import de.keksuccino.konkrete.input.StringUtils;
//import de.keksuccino.konkrete.localization.Locals;
//import de.keksuccino.konkrete.math.MathUtils;
//import de.keksuccino.fancymenu.properties.PropertiesSection;
//import de.keksuccino.konkrete.rendering.RenderUtils;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.GuiComponent;
//import net.minecraft.client.gui.components.Renderable;
//import net.minecraft.client.gui.components.events.GuiEventListener;
//import net.minecraft.network.chat.Component;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import org.lwjgl.glfw.GLFW;
//
//import javax.annotation.Nonnull;
//import java.awt.*;
//import java.util.ArrayList;
//import java.util.List;
//
//public abstract class AbstractEditorElementOld extends GuiComponent implements Renderable, GuiEventListener {
//
//	private static final Logger LOGGER = LogManager.getLogger();
//
//	protected static final Color BORDER_COLOR_FOCUSED = new Color(3, 219, 252);
//	protected static final Color BORDER_COLOR_NORMAL = new Color(3, 148, 252);
//
//	protected static final long CURSOR_HORIZONTAL_RESIZE = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR);
//	protected static final long CURSOR_VERTICAL_RESIZE = GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR);
//	protected static final long CURSOR_NORMAL = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);
//
//	public final EditorElementSettings settings;
//	public AbstractElement element;
//	public LayoutEditorScreen editor;
//	private LayoutEditorHistory.Snapshot cachedSnapshot;
//	protected boolean hovered = false;
//	protected boolean dragging = false;
//	protected boolean resizing = false;
//	protected int activeGrabber = -1;
//	protected int lastGrabber;
//	protected int startDiffX;
//	protected int startDiffY;
//	protected int startX;
//	protected int startY;
//	protected int startWidth;
//	protected int startHeight;
//	private boolean moving = false;
//
//	public List<AbstractEditorElementOld> hoveredLayers = new ArrayList<>();
//
//	public ContextMenu rightClickContextMenu;
//
//	protected static boolean isShiftPressed = false;
//	private static boolean shiftListener = false;
//
//	public AbstractEditorElementOld(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor, @Nullable EditorElementSettings settings) {
//		this.settings = (settings != null) ? settings : new EditorElementSettings();
//		this.settings.editorElement = this;
//		this.editor = editor;
//		this.element = element;
//		if (!shiftListener) {
//			KeyboardHandler.addKeyPressedListener(d -> {
//				if ((d.keycode == 340) || (d.keycode == 344)) {
//					isShiftPressed = true;
//				}
//			});
//			KeyboardHandler.addKeyReleasedListener(d -> {
//				if ((d.keycode == 340) || (d.keycode == 344)) {
//					isShiftPressed = false;
//				}
//			});
//			shiftListener = true;
//		}
//		this.init();
//	}
//
//	public AbstractEditorElementOld(@Nonnull AbstractElement element, @Nonnull LayoutEditorScreen editor) {
//		this(element, editor, new EditorElementSettings());
//	}
//
//	public void init() {
//
//		this.rightClickContextMenu = new ContextMenu();
//		this.rightClickContextMenu.setAlwaysOnTop(true);
//
//		if (this.settings.isElementIdCopyButtonEnabled()) {
//			AdvancedButton copyIdButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.copyid"), true, (press) -> {
//				if (!(this instanceof LayoutVanillaButton)) {
//					Minecraft.getInstance().keyboardHandler.setClipboard(this.element.getInstanceIdentifier());
//				} else {
//					Minecraft.getInstance().keyboardHandler.setClipboard("vanillabtn:" + ((LayoutVanillaButton)this).getButtonId());
//				}
//			});
//			copyIdButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.copyid.btn.desc"), "%n%"));
//			this.rightClickContextMenu.addContent(copyIdButton);
//		}
//
//		if (this instanceof LayoutVanillaButton) {
//			AdvancedButton copyLocatorButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.vanilla_button.copy_locator"), true, (press) -> {
//				String locator = this.editor.getScreenToCustomizeIdentifier() + ":" + ((LayoutVanillaButton)this).getButtonId();
//				Minecraft.getInstance().keyboardHandler.setClipboard(locator);
//			});
//			copyLocatorButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.vanilla_button.copy_locator.desc"), "%n%"));
//			this.rightClickContextMenu.addContent(copyLocatorButton);
//		}
//
//		this.rightClickContextMenu.addSeparator();
//
//		if (this.settings.isAnchorPointChangeable()) {
//
//			ContextMenu anchorPointContext = new ContextMenu();
//			anchorPointContext.setAutoclose(true);
//			this.rightClickContextMenu.addChild(anchorPointContext);
//
//			if (this.settings.isElementAnchorPointAllowed()) {
//				Button anchorElementButton = new Button(0, 0, 0, 16, ElementAnchorPoint.ELEMENT.getDisplayName(), (press) -> {
//					this.editor.setObjectFocused(this, false, true);
//					FMTextInputPopup pop = new FMTextInputPopup(new Color(0, 0, 0, 0), Locals.localize("fancymenu.helper.editor.items.orientation.element.setidentifier"), null, 240, (call) -> {
//						if (call != null) {
//							AbstractEditorElementOld l = this.editor.getElementByInstanceIdentifier(call);
//							if (l != null) {
//								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//								this.element.anchorPointElementIdentifier = call;
//								this.element.anchorPointElement = l.element;
//								this.setAnchorPoint(ElementAnchorPoint.ELEMENT);
//							} else {
//								PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("fancymenu.helper.editor.items.orientation.element.setidentifier.identifiernotfound")));
//							}
//						}
//					});
//					if (this.element.anchorPointElementIdentifier != null) {
//						pop.setText(this.element.anchorPointElementIdentifier);
//					}
//					PopupHandler.displayPopup(pop);
//					anchorPointContext.closeMenu();
//				});
//				anchorElementButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.orientation.element.btn.desc"), "%n%"));
//				anchorPointContext.addContent(anchorElementButton);
//			}
//
//			anchorPointContext.addSeparator();
//
//			for (ElementAnchorPoint p : ElementAnchorPoint.ANCHOR_POINTS) {
//				if (p != ElementAnchorPoint.ELEMENT) {
//					Button b = new Button(0, 0, 0, 0, p.getDisplayName(), true, (button) -> {
//						this.editor.setObjectFocused(this, false, true);
//						this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//						this.setAnchorPoint(p);
//						anchorPointContext.closeMenu();
//					});
//					anchorPointContext.addContent(b);
//				}
//			}
//
//			AdvancedButton anchorPointButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.setorientation"), true, (press) -> {
//				anchorPointContext.setParentButton((AdvancedButton) press);
//				anchorPointContext.openMenuAt(0, press.y);
//			}) {
//				@Override
//				public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//					this.active = (element.advancedX == null) && (element.advancedY == null);
//					super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//				}
//			};
//			anchorPointButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.orientation.btndesc"), "%n%"));
//			this.rightClickContextMenu.addContent(anchorPointButton);
//
//		}
//
//		if (this.settings.isAdvancedPositioningSupported()) {
//			ContextMenu advancedPositioningMenu = new ContextMenu();
//			advancedPositioningMenu.setAutoclose(true);
//			this.rightClickContextMenu.addChild(advancedPositioningMenu);
//
//			AdvancedButton advancedPositioningButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
//				advancedPositioningMenu.setParentButton((AdvancedButton) press);
//				advancedPositioningMenu.openMenuAt(0, press.y);
//			}) {
//				@Override
//				public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//					if ((element.advancedX != null) || (element.advancedY != null)) {
//						this.setMessage(Locals.localize("fancymenu.helper.editor.items.features.advanced_positioning.active"));
//					} else {
//						this.setMessage(Locals.localize("fancymenu.helper.editor.items.features.advanced_positioning"));
//					}
//					super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//				}
//			};
//			advancedPositioningButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.features.advanced_positioning.desc"), "%n%"));
//			this.rightClickContextMenu.addContent(advancedPositioningButton);
//
//			AdvancedButton advancedPosXButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.features.advanced_positioning.posx"), true, (press) -> {
//				TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("fancymenu.helper.editor.items.features.advanced_positioning.posx")), this.editor, null, (call) -> {
//					if (call != null) {
//						this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//						if (call.replace(" ", "").equals("")) {
//							this.element.advancedX = null;
//						} else {
//							this.element.advancedX = call;
//						}
//						this.element.baseX = 0;
//						this.element.baseY = 0;
//						this.element.anchorPoint = ElementAnchorPoint.TOP_LEFT;
//					}
//				});
//				s.multilineMode = false;
//				if (this.element.advancedX != null) {
//					s.setText(this.element.advancedX);
//				}
//				Minecraft.getInstance().setScreen(s);
//			});
//			advancedPositioningMenu.addContent(advancedPosXButton);
//
//			AdvancedButton advancedPosYButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.features.advanced_positioning.posy"), true, (press) -> {
//				TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("fancymenu.helper.editor.items.features.advanced_positioning.posy")), this.editor, null, (call) -> {
//					if (call != null) {
//						this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//						if (call.replace(" ", "").equals("")) {
//							this.element.advancedY = null;
//						} else {
//							this.element.advancedY = call;
//						}
//						this.element.baseX = 0;
//						this.element.baseY = 0;
//						this.element.anchorPoint = ElementAnchorPoint.TOP_LEFT;
//					}
//				});
//				s.multilineMode = false;
//				if (this.element.advancedY != null) {
//					s.setText(this.element.advancedY);
//				}
//				Minecraft.getInstance().setScreen(s);
//			});
//			advancedPositioningMenu.addContent(advancedPosYButton);
//		}
//
//		if (this.settings.isAdvancedSizingSupported()) {
//			ContextMenu advancedSizingMenu = new ContextMenu();
//			advancedSizingMenu.setAutoclose(true);
//			this.rightClickContextMenu.addChild(advancedSizingMenu);
//
//			AdvancedButton advancedSizingButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
//				advancedSizingMenu.setParentButton((AdvancedButton) press);
//				advancedSizingMenu.openMenuAt(0, press.y);
//			}) {
//				@Override
//				public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//					if ((element.advancedWidth != null) || (element.advancedHeight != null)) {
//						this.setMessage(Locals.localize("fancymenu.helper.editor.items.features.advanced_sizing.active"));
//					} else {
//						this.setMessage(Locals.localize("fancymenu.helper.editor.items.features.advanced_sizing"));
//					}
//					super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//				}
//			};
//			advancedSizingButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.features.advanced_sizing.desc"), "%n%"));
//			this.rightClickContextMenu.addContent(advancedSizingButton);
//
//			AdvancedButton advancedWidthButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.features.advanced_sizing.width"), true, (press) -> {
//				TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("fancymenu.helper.editor.items.features.advanced_sizing.width")), this.editor, null, (call) -> {
//					if (call != null) {
//						if (call.replace(" ", "").equals("")) {
//							if ((this.element.advancedWidth != null) || (this.element.width != 50)) {
//								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//							}
//							this.element.width = 50;
//							this.element.advancedWidth = null;
//						} else {
//							if (!call.equals(this.element.advancedWidth) || (this.element.width != 50)) {
//								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//							}
//							this.element.width = 50;
//							this.element.advancedWidth = call;
//							if ((this instanceof LayoutVanillaButton) && (this.element.anchorPoint.equals("original"))) {
//								this.element.anchorPoint = ElementAnchorPoint.TOP_LEFT;
//								this.element.baseX = 0;
//								this.element.baseY = 0;
//							}
//						}
//					}
//				});
//				s.multilineMode = false;
//				if (this.element.advancedWidth != null) {
//					s.setText(this.element.advancedWidth);
//				}
//				Minecraft.getInstance().setScreen(s);
//			});
//			advancedSizingMenu.addContent(advancedWidthButton);
//
//			AdvancedButton advancedHeightButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.features.advanced_sizing.height"), true, (press) -> {
//				TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("fancymenu.helper.editor.items.features.advanced_sizing.height")), this.editor, null, (call) -> {
//					if (call != null) {
//						if (call.replace(" ", "").equals("")) {
//							if ((this.element.advancedHeight != null) || (this.element.height != 50)) {
//								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//							}
//							this.element.height = 50;
//							this.element.advancedHeight = null;
//						} else {
//							if (!call.equals(this.element.advancedHeight) || (this.element.height != 50)) {
//								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//							}
//							this.element.height = 50;
//							this.element.advancedHeight = call;
//							if ((this instanceof LayoutVanillaButton) && (this.element.anchorPoint.equals("original"))) {
//								this.element.anchorPoint = ElementAnchorPoint.TOP_LEFT;
//								this.element.baseX = 0;
//								this.element.baseY = 0;
//							}
//						}
//					}
//				});
//				s.multilineMode = false;
//				if (this.element.advancedHeight != null) {
//					s.setText(this.element.advancedHeight);
//				}
//				Minecraft.getInstance().setScreen(s);
//			});
//			advancedSizingMenu.addContent(advancedHeightButton);
//		}
//
//		ContextMenu layersMenu = new ContextMenu();
//		layersMenu.setAutoclose(true);
//		this.rightClickContextMenu.addChild(layersMenu);
//
//		AdvancedButton layersButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.element.general.pick_element"), true, (press) -> {
//			layersMenu.getContent().clear();
//			for (AbstractEditorElementOld o : this.hoveredLayers) {
//				String label = o.element.builder.getDisplayName().getString();
//				if (Minecraft.getInstance().font.width(label) > 200) {
//					label = Minecraft.getInstance().font.plainSubstrByWidth(label, 200) + "..";
//				}
//				AdvancedButton btn = new AdvancedButton(0, 0, 0, 0, label, (press2) -> {
//					this.editor.clearFocusedObjects();
//					this.editor.setObjectFocused(o, true, true);
//				});
//				layersMenu.addContent(btn);
//			}
//			layersMenu.setParentButton((AdvancedButton) press);
//			layersMenu.openMenuAt(0, press.y);
//		});
//		this.rightClickContextMenu.addContent(layersButton);
//
//		this.rightClickContextMenu.addSeparator();
//
//		if (this.settings.isStretchable()) {
//			AdvancedButton stretchXButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
//				if (isOrientationSupportedByStretchAction(!this.element.stretchX, this.element.stretchY)) {
//					this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//					this.element.stretchX = !this.element.stretchX;
//				}
//			}) {
//				@Override
//				public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//					this.active = element.advancedWidth == null;
//					this.active = isOrientationSupportedByStretchAction(!element.stretchX, element.stretchY);
//					if (element.stretchX && this.active) {
//						this.setMessage(Locals.localize("helper.creator.object.stretch.x.on"));
//					} else {
//						this.setMessage(Locals.localize("helper.creator.object.stretch.x.off"));
//					}
//					if (this.active) {
//						this.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.stretch.x.desc"), "%n%"));
//					} else {
//						this.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.stretch.not_supported"), "%n%"));
//					}
//					super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//				}
//			};
//			this.rightClickContextMenu.addContent(stretchXButton);
//
//			AdvancedButton stretchYButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
//				if (isOrientationSupportedByStretchAction(this.element.stretchX, !this.element.stretchY)) {
//					this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//					this.element.stretchY = !this.element.stretchY;
//				}
//			}) {
//				@Override
//				public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
//					this.active = element.advancedHeight == null;
//					this.active = isOrientationSupportedByStretchAction(element.stretchX, !element.stretchY);
//					if (element.stretchY && this.active) {
//						this.setMessage(Locals.localize("helper.creator.object.stretch.y.on"));
//					} else {
//						this.setMessage(Locals.localize("helper.creator.object.stretch.y.off"));
//					}
//					if (this.active) {
//						this.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.stretch.y.desc"), "%n%"));
//					} else {
//						this.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.stretch.not_supported"), "%n%"));
//					}
//					super.render(p_93657_, p_93658_, p_93659_, p_93660_);
//				}
//			};
//			this.rightClickContextMenu.addContent(stretchYButton);
//		}
//
//		this.rightClickContextMenu.addSeparator();
//
//		if (this.settings.isLoadingRequirementsEnabled()) {
//			AdvancedButton loadingRequirementsButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.editor.loading_requirement.elements.loading_requirements"), (press) -> {
//				ManageRequirementsScreen s = new ManageRequirementsScreen(this.editor, this.element.loadingRequirementContainer, (call) -> {});
//				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//				Minecraft.getInstance().setScreen(s);
//			});
//			loadingRequirementsButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.elements.loading_requirements.desc"), "%n%"));
//			this.rightClickContextMenu.addContent(loadingRequirementsButton);
//		}
//
//		this.rightClickContextMenu.addSeparator();
//
//		if (this.settings.isOrderable()) {
//			AdvancedButton moveUpButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.object.moveup"), (press) -> {
//				AbstractEditorElementOld o = this.editor.moveUp(this);
//				if (o != null) {
//					((AdvancedButton)press).setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.moveup.desc", Locals.localize("helper.creator.object.moveup.desc.subtext", o.element.builder.getDisplayName().getString())), "%n%"));
//				}
//			});
//			moveUpButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.moveup.desc", ""), "%n%"));
//			this.rightClickContextMenu.addContent(moveUpButton);
//
//			AdvancedButton moveDownButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.object.movedown"), (press) -> {
//				AbstractEditorElementOld o = this.editor.moveDown(this);
//				if (o != null) {
//					if (o instanceof LayoutVanillaButton) {
//						((AdvancedButton)press).setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.movedown.desc", Locals.localize("helper.creator.object.movedown.desc.subtext.vanillabutton")), "%n%"));
//					} else {
//						((AdvancedButton)press).setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.movedown.desc", Locals.localize("helper.creator.object.movedown.desc.subtext", o.element.builder.getDisplayName().getString())), "%n%"));
//					}
//				}
//			});
//			moveDownButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.movedown.desc", ""), "%n%"));
//			this.rightClickContextMenu.addContent(moveDownButton);
//		}
//
//		if (this.settings.isCopyable()) {
//			AdvancedButton copyButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.editor.ui.edit.copy"), (press) -> {
//				this.editor.copySelectedElements();
//			});
//			this.rightClickContextMenu.addContent(copyButton);
//		}
//
//		if (this.settings.isDestroyable()) {
//			AdvancedButton destroyButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.delete"), true, (press) -> {
//				this.destroyElement();
//			});
//			this.rightClickContextMenu.addContent(destroyButton);
//		}
//
//		this.rightClickContextMenu.addSeparator();
//
//		ContextMenu delayMenu = new ContextMenu();
//		delayMenu.setAutoclose(true);
//		this.rightClickContextMenu.addChild(delayMenu);
//
//		String tdLabel = Locals.localize("helper.creator.items.delay.off");
//		if (this.element.delayAppearance) {
//			tdLabel = Locals.localize("helper.creator.items.delay.everytime");
//		}
//		if (this.element.delayAppearance && !this.element.delayAppearanceEverytime) {
//			tdLabel = Locals.localize("helper.creator.items.delay.firsttime");
//		}
//		AdvancedButton toggleDelayButton = new AdvancedButton(0, 0, 0, 0, tdLabel, true, (press) -> {
//			this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//			if (!this.element.delayAppearance) {
//				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.delay.firsttime"));
//				this.element.delayAppearance = true;
//				this.element.delayAppearanceEverytime = false;
//			} else if (this.element.delayAppearance && !this.element.delayAppearanceEverytime) {
//				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.delay.everytime"));
//				this.element.delayAppearance = true;
//				this.element.delayAppearanceEverytime = true;
//			} else if (this.element.delayAppearance && this.element.delayAppearanceEverytime) {
//				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.delay.off"));
//				this.element.delayAppearance = false;
//				this.element.delayAppearanceEverytime = false;
//			}
//		});
//		delayMenu.addContent(toggleDelayButton);
//
//		AdvancedButton delaySecondsButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.delay.seconds"), true, (press) -> {
//			FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§f" + Locals.localize("helper.creator.items.delay.seconds"), CharacterFilter.getDoubleCharacterFiler(), 240, (call) -> {
//				if (call != null) {
//					if (!call.equals("" + this.element.appearanceDelayInSeconds)) {
//						this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//					}
//					if (call.replace(" ", "").equals("")) {
//						this.element.appearanceDelayInSeconds = 1.0F;
//					} else if (MathUtils.isFloat(call)) {
//						this.element.appearanceDelayInSeconds = Float.parseFloat(call);
//					}
//				}
//			});
//			p.setText("" + this.element.appearanceDelayInSeconds);
//			PopupHandler.displayPopup(p);
//		}) {
//			@Override
//			public void render(@NotNull PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
//				this.active = AbstractEditorElementOld.this.element.delayAppearance;
//				super.render(matrix, mouseX, mouseY, partialTicks);
//			}
//		};
//		delayMenu.addContent(delaySecondsButton);
//
//		delayMenu.addSeparator();
//
//		String fiLabel = Locals.localize("helper.creator.items.delay.fadein.off");
//		if (this.element.delayAppearance && this.element.fadeIn) {
//			fiLabel = Locals.localize("helper.creator.items.delay.fadein.on");
//		}
//		AdvancedButton toggleFadeButton = new AdvancedButton(0, 0, 0, 0, fiLabel, true, (press) -> {
//			if (!this.element.fadeIn) {
//				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.delay.fadein.on"));
//				this.element.fadeIn = true;
//			} else {
//				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.delay.fadein.off"));
//				this.element.fadeIn = false;
//			}
//		}) {
//			@Override
//			public void render(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
//				this.active = AbstractEditorElementOld.this.element.delayAppearance;
//				super.render(matrixStack, mouseX, mouseY, partialTicks);
//			}
//		};
//		if (this.settings.isFadeable()) {
//			delayMenu.addContent(toggleFadeButton);
//		}
//
//		AdvancedButton fadeSpeedButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.delay.fadein.speed"), true, (press) -> {
//			FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§f" + Locals.localize("helper.creator.items.delay.fadein.speed"), CharacterFilter.getDoubleCharacterFiler(), 240, (call) -> {
//				if (call != null) {
//					if (!call.equals("" + this.element.fadeInSpeed)) {
//						this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//					}
//					if (call.replace(" ", "").equals("")) {
//						this.element.fadeInSpeed = 1.0F;
//					} else if (MathUtils.isFloat(call)) {
//						this.element.fadeInSpeed = Float.parseFloat(call);
//					}
//				}
//			});
//			p.setText("" + this.element.fadeInSpeed);
//			PopupHandler.displayPopup(p);
//		}) {
//			@Override
//			public void render(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
//				this.active = AbstractEditorElementOld.this.element.delayAppearance;
//				super.render(matrixStack, mouseX, mouseY, partialTicks);
//			}
//		};
//		if (this.settings.isFadeable()) {
//			delayMenu.addContent(fadeSpeedButton);
//		}
//
//		AdvancedButton delayButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.delay"), true, (press) -> {
//			delayMenu.setParentButton((AdvancedButton) press);
//			delayMenu.openMenuAt(0, press.y);
//		});
//		if (this.settings.isDelayable()) {
//			this.rightClickContextMenu.addContent(delayButton);
//		}
//
//		this.rightClickContextMenu.addSeparator();
//
//	}
//
//	public void onSettingsChanged() {
//		this.resetElementStates();
//		this.init();
//	}
//
//	protected void setAnchorPoint(@NotNull ElementAnchorPoint anchorPoint) {
//		if (!this.settings.isAnchorPointChangeable()) {
//			return;
//		}
//		this.element.anchorPoint = anchorPoint;
//		this.element.baseX = anchorPoint.getDefaultElementPositionX(this.element);
//		this.element.baseY = anchorPoint.getDefaultElementPositionY(this.element);
//	}
//
////	protected int orientationMouseX(int mouseX) {
////		if (this.element.anchorPoint.endsWith("-centered")) {
////			return mouseX - (this.editor.width / 2);
////		}
////		if (this.element.anchorPoint.endsWith("-right")) {
////			return mouseX - this.editor.width;
////		}
////		return mouseX;
////	}
////
////	protected int orientationMouseY(int mouseY) {
////		if (this.element.anchorPoint.startsWith("mid-")) {
////			return mouseY - (this.editor.height / 2);
////		}
////		if (this.element.anchorPoint.startsWith("bottom-")) {
////			return mouseY - this.editor.height;
////		}
////		return mouseY;
////	}
////
////	private boolean isOrientationSupportedByStretchAction(boolean stX, boolean stY) {
////		try {
////			if (stX && !stY) {
////				if (!this.element.anchorPoint.equals("top-left") && !this.element.anchorPoint.equals("mid-left") && !this.element.anchorPoint.equals("bottom-left")) {
////					return false;
////				}
////			}
////			if (stY && !stX) {
////				if (!this.element.anchorPoint.equals("top-left") && !this.element.anchorPoint.equals("top-centered") && !this.element.anchorPoint.equals("top-right")) {
////					return false;
////				}
////			}
////			if (stX && stY) {
////				return this.element.anchorPoint.equals("top-left");
////			}
////			return true;
////		} catch (Exception e) {
////			e.printStackTrace();
////		}
////		return false;
////	}
////
////	private void handleStretch() {
////		try {
////			if (this.element.stretchX || this.element.stretchY) {
////				this.orientationElementButton.active = false;
////			}
////			if (this.settings.isAnchorPointChangeable()) {
////				if (this.element.stretchX && !this.element.stretchY) {
////					this.orientationTopLeftButton.active = true;
////					this.orientationMidLeftButton.active = true;
////					this.orientationBottomLeftButton.active = true;
////					this.orientationTopCenteredButton.active = false;
////					this.orientationMidCenteredButton.active = false;
////					this.orientationBottomCenteredButton.active = false;
////					this.orientationTopRightButton.active = false;
////					this.orientationMidRightButton.active = false;
////					this.orientationBottomRightButton.active = false;
////				}
////				if (this.element.stretchY && !this.element.stretchX) {
////					this.orientationTopLeftButton.active = true;
////					this.orientationMidLeftButton.active = false;
////					this.orientationBottomLeftButton.active = false;
////					this.orientationTopCenteredButton.active = true;
////					this.orientationMidCenteredButton.active = false;
////					this.orientationBottomCenteredButton.active = false;
////					this.orientationTopRightButton.active = true;
////					this.orientationMidRightButton.active = false;
////					this.orientationBottomRightButton.active = false;
////				}
////				if (this.element.stretchX && this.element.stretchY) {
////					this.orientationTopLeftButton.active = true;
////					this.orientationMidLeftButton.active = false;
////					this.orientationBottomLeftButton.active = false;
////					this.orientationTopCenteredButton.active = false;
////					this.orientationMidCenteredButton.active = false;
////					this.orientationBottomCenteredButton.active = false;
////					this.orientationTopRightButton.active = false;
////					this.orientationMidRightButton.active = false;
////					this.orientationBottomRightButton.active = false;
////				}
////				if (!this.element.stretchX && !this.element.stretchY) {
////					this.orientationTopLeftButton.active = true;
////					this.orientationMidLeftButton.active = true;
////					this.orientationBottomLeftButton.active = true;
////					this.orientationTopCenteredButton.active = true;
////					this.orientationMidCenteredButton.active = true;
////					this.orientationBottomCenteredButton.active = true;
////					this.orientationTopRightButton.active = true;
////					this.orientationMidRightButton.active = true;
////					this.orientationBottomRightButton.active = true;
////					this.orientationElementButton.active = true;
////				}
////			}
////		} catch (Exception e) {
////			e.printStackTrace();
////		}
////	}
//
//	@Override
//	public void render(@NotNull PoseStack matrix, int mouseX, int mouseY, float partial) {
//
//		this.hovered = this.isMouseOver(mouseX, mouseY);
//
//		this.element.render(matrix, mouseX, mouseY, partial);
//
////		this.handleStretch();
//
//		// Renders the border around the object if its focused (starts to render one tick after the object got focused)
//		if (this.editor.isFocused(this)) {
//			this.renderBorder(matrix, mouseX, mouseY);
//		} else {
//			if ((this.editor.getTopHoverObject() == this) && (!this.editor.isObjectFocused() || (!this.editor.isFocusedHovered() && !this.editor.isFocusedDragged() && !this.editor.isFocusedGettingResized() && !this.editor.isFocusedGrabberPressed()))) {
//				this.renderHighlightBorder(matrix);
//			}
//		}
//
//		//Reset cursor to default
//		if ((this.activeGrabber == -1) && (!MouseInput.isLeftMouseDown() || PopupHandler.isPopupActive())) {
//			GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), CURSOR_NORMAL);
//		}
//
//		//Update dragging state
//		if (this.settings.isDragable() && (this.element.advancedX == null) && (this.element.advancedY == null)) {
//			if (this.isLeftClicked() && !(this.resizing || this.isGrabberPressed())) {
//				this.dragging = true;
//			} else {
//				if (!MouseInput.isLeftMouseDown()) {
//					this.dragging = false;
//				}
//			}
//		} else {
//			this.dragging = false;
//		}
//
//		//Handles the resizing process
//		if (this.settings.isResizeable()) {
//			if ((this.isGrabberPressed() || this.resizing) && !this.isDragged() && this.editor.isFocused(this)) {
//				if (this.editor.getFocusedObjects().size() == 1) {
//					if (!this.resizing) {
//						this.cachedSnapshot = this.editor.history.createSnapshot();
//
//						this.lastGrabber = this.getActiveResizeGrabber();
//					}
//					this.resizing = true;
////					this.handleResize(this.orientationMouseX(mouseX), this.orientationMouseY(mouseY));
//					this.handleResize(mouseX, mouseY);
//				}
//			}
//		} else {
//			this.resizing = false;
//		}
//
//		//Moves the object with the mouse motion if dragged
//		if (this.isDragged() && this.editor.isFocused(this)) {
//			if (this.editor.getFocusedObjects().size() == 1) {
//				if (!this.moving) {
//					this.cachedSnapshot = this.editor.history.createSnapshot();
//				}
//				this.moving = true;
//				if ((mouseX >= 5) && (mouseX <= this.editor.width -5)) {
//					if (!this.element.stretchX) {
//						this.element.baseX = this.orientationMouseX(mouseX) - this.startDiffX;
//					}
//				}
//				if ((mouseY >= 5) && (mouseY <= this.editor.height -5)) {
//					if (!this.element.stretchY) {
//						this.element.baseY = this.orientationMouseY(mouseY) - this.startDiffY;
//					}
//				}
//			}
//		}
//		if (!this.isDragged()) {
//			this.startDiffX = this.orientationMouseX(mouseX) - this.element.baseX;
//			this.startDiffY = this.orientationMouseY(mouseY) - this.element.baseY;
//			if (((this.startX != this.element.baseX) || (this.startY != this.element.baseY)) && this.moving) {
//				if (this.cachedSnapshot != null) {
//					this.editor.history.saveSnapshot(this.cachedSnapshot);
//				}
//			}
//			this.moving = false;
//		}
//
//		if (!MouseInput.isLeftMouseDown()) {
//			if (((this.startWidth != this.element.getWidth()) || (this.startHeight != this.element.getHeight())) && this.resizing) {
//				if (this.cachedSnapshot != null) {
//					this.editor.history.saveSnapshot(this.cachedSnapshot);
//				}
//			}
//			this.startX = this.element.baseX;
//			this.startY = this.element.baseY;
//			this.startWidth = this.element.getWidth();
//			this.startHeight = this.element.getHeight();
//			this.resizing = false;
//		}
//
//	}
//
//	protected void renderBorder(PoseStack matrix, int mouseX, int mouseY) {
//
//		//horizontal line top
//		GuiComponent.fill(matrix, this.element.getX(), this.element.getY(), this.element.getX() + this.element.getWidth(), this.element.getY() + 1, BORDER_COLOR_NORMAL.getRGB());
//		//horizontal line bottom
//		GuiComponent.fill(matrix, this.element.getX(), this.element.getY() + this.element.getHeight() - 1, this.element.getX() + this.element.getWidth(), this.element.getY() + this.element.getHeight(), BORDER_COLOR_NORMAL.getRGB());
//		//vertical line left
//		GuiComponent.fill(matrix, this.element.getX(), this.element.getY(), this.element.getX() + 1, this.element.getY() + this.element.getHeight(), BORDER_COLOR_NORMAL.getRGB());
//		//vertical line right
//		GuiComponent.fill(matrix, this.element.getX() + this.element.getWidth() - 1, this.element.getY(), this.element.getX() + this.element.getWidth(), this.element.getY() + this.element.getHeight(), BORDER_COLOR_NORMAL.getRGB());
//
//		int w = 4;
//		int h = 4;
//
//		int yHorizontal = this.element.getY() + (this.element.getHeight() / 2) - (h / 2);
//		int xHorizontalLeft = this.element.getX() - (w / 2);
//		int xHorizontalRight = this.element.getX() + this.element.getWidth() - (w / 2);
//
//		int xVertical = this.element.getX() + (this.element.getWidth() / 2) - (w / 2);
//		int yVerticalTop = this.element.getY() - (h / 2);
//		int yVerticalBottom = this.element.getY() + this.element.getHeight() - (h / 2);
//
//		if (this.settings.isDragable() && this.settings.isResizeable() && (this.element.advancedX == null) && (this.element.advancedY == null) && (this.element.advancedWidth == null) && (this.element.advancedHeight == null)) {
//			if (!this.element.stretchX && this.settings.isResizeableX()) {
//				//grabber left
//				GuiComponent.fill(matrix, xHorizontalLeft, yHorizontal, xHorizontalLeft + w, yHorizontal + h, BORDER_COLOR_NORMAL.getRGB());
//				//grabber right
//				GuiComponent.fill(matrix, xHorizontalRight, yHorizontal, xHorizontalRight + w, yHorizontal + h, BORDER_COLOR_NORMAL.getRGB());
//			}
//			if (!this.element.stretchY && this.settings.isResizeableY()) {
//				//grabber top
//				GuiComponent.fill(matrix, xVertical, yVerticalTop, xVertical + w, yVerticalTop + h, BORDER_COLOR_NORMAL.getRGB());
//				//grabber bottom
//				GuiComponent.fill(matrix, xVertical, yVerticalBottom, xVertical + w, yVerticalBottom + h, BORDER_COLOR_NORMAL.getRGB());
//			}
//		}
//
//		//Update cursor and active grabber when grabber is hovered
//		if (this.settings.isResizeable() && (this.element.advancedX == null) && (this.element.advancedY == null) && (this.element.advancedWidth == null) && (this.element.advancedHeight == null)) {
//			if ((mouseX >= xHorizontalLeft) && (mouseX <= xHorizontalLeft + w) && (mouseY >= yHorizontal) && (mouseY <= yHorizontal + h)) {
//				if (!this.element.stretchX && this.settings.isResizeableX()) {
//					GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), CURSOR_HORIZONTAL_RESIZE);
//					this.activeGrabber = 0;
//				} else {
//					this.activeGrabber = -1;
//				}
//			} else if ((mouseX >= xHorizontalRight) && (mouseX <= xHorizontalRight + w) && (mouseY >= yHorizontal) && (mouseY <= yHorizontal + h)) {
//				if (!this.element.stretchX && this.settings.isResizeableX()) {
//					GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), CURSOR_HORIZONTAL_RESIZE);
//					this.activeGrabber = 1;
//				} else {
//					this.activeGrabber = -1;
//				}
//			} else if ((mouseX >= xVertical) && (mouseX <= xVertical + w) && (mouseY >= yVerticalTop) && (mouseY <= yVerticalTop + h)) {
//				if (!this.element.stretchY && this.settings.isResizeableY()) {
//					GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), CURSOR_VERTICAL_RESIZE);
//					this.activeGrabber = 2;
//				} else {
//					this.activeGrabber = -1;
//				}
//			} else if ((mouseX >= xVertical) && (mouseX <= xVertical + w) && (mouseY >= yVerticalBottom) && (mouseY <= yVerticalBottom + h)) {
//				if (!this.element.stretchY && this.settings.isResizeableY()) {
//					GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), CURSOR_VERTICAL_RESIZE);
//					this.activeGrabber = 3;
//				} else {
//					this.activeGrabber = -1;
//				}
//			} else {
//				this.activeGrabber = -1;
//			}
//		} else {
//			this.activeGrabber = -1;
//		}
//
//		//Render pos and size values
//		RenderUtils.setScale(matrix, 0.5F);
//		drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.orientation") + ": " + this.element.anchorPoint, this.element.getX()*2, (this.element.getY()*2) - 26, -1);
//		drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.posx") + ": " + this.element.getX(), this.element.getX()*2, (this.element.getY()*2) - 17, -1);
//		drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.width") + ": " + this.element.getWidth(), this.element.getX()*2, (this.element.getY()*2) - 8, -1);
//
//		drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.posy") + ": " + this.element.getY(), ((this.element.getX() + this.element.getWidth())*2)+3, ((this.element.getY() + this.element.getHeight())*2) - 14, -1);
//		drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.height") + ": " + this.element.getHeight(), ((this.element.getX() + this.element.getWidth())*2)+3, ((this.element.getY() + this.element.getHeight())*2) - 5, -1);
//		RenderUtils.postScale(matrix);
//	}
//
//	protected void renderHighlightBorder(PoseStack matrix) {
//		Color c = BORDER_COLOR_FOCUSED;
//		//horizontal line top
//		GuiComponent.fill(matrix, this.element.getX(), this.element.getY(), this.element.getX() + this.element.getWidth(), this.element.getY() + 1, c.getRGB());
//		//horizontal line bottom
//		GuiComponent.fill(matrix, this.element.getX(), this.element.getY() + this.element.getHeight() - 1, this.element.getX() + this.element.getWidth(), this.element.getY() + this.element.getHeight(), c.getRGB());
//		//vertical line left
//		GuiComponent.fill(matrix, this.element.getX(), this.element.getY(), this.element.getX() + 1, this.element.getY() + this.element.getHeight(), c.getRGB());
//		//vertical line right
//		GuiComponent.fill(matrix, this.element.getX() + this.element.getWidth() - 1, this.element.getY(), this.element.getX() + this.element.getWidth(), this.element.getY() + this.element.getHeight(), c.getRGB());
//	}
//
//	/**
//	 * <b>Returns:</b><br><br>
//	 *
//	 * -1 if NO grabber is currently pressed<br>
//	 * 0 if the LEFT grabber is pressed<br>
//	 * 1 if the RIGHT grabber is pressed<br>
//	 * 2 if the TOP grabber is pressed<br>
//	 * 3 if the BOTTOM grabber is pressed
//	 *
//	 */
//	public int getActiveResizeGrabber() {
//		return this.activeGrabber;
//	}
//
//	public boolean isGrabberPressed() {
//		return ((this.getActiveResizeGrabber() != -1) && MouseInput.isLeftMouseDown());
//	}
//
//	protected int getAspectWidth(int startW, int startH, int height) {
//		double ratio = (double) startW / (double) startH;
//		return (int)(height * ratio);
//	}
//
//	protected int getAspectHeight(int startW, int startH, int width) {
//		double ratio = (double) startW / (double) startH;
//		return (int)(width / ratio);
//	}
//
//	protected void handleResize(int mouseX, int mouseY) {
//
//		int g = this.lastGrabber;
//		int diffX;
//		int diffY;
//
//		//X difference
//		if (mouseX > this.startX) {
//			diffX = Math.abs(mouseX - this.startX);
//		} else {
//			diffX = Math.negateExact(this.startX - mouseX);
//		}
//		//Y difference
//		if (mouseY > this.startY) {
//			diffY = Math.abs(mouseY - this.startY);
//		} else {
//			diffY = Math.negateExact(this.startY - mouseY);
//		}
//
//		if (!this.element.stretchX) {
//			if (g == 0) { //left
//				int w = this.startWidth + this.getOppositeInt(diffX);
//				if (w >= 5) {
//					this.element.baseX = this.startX + diffX;
//					this.element.setWidth(w);
//					if (isShiftPressed) {
//						int h = this.getAspectHeight(this.startWidth, this.startHeight, w);
//						if (h >= 5) {
//							this.element.setHeight(h);
//						}
//					}
//				}
//			}
//			if (g == 1) { //right
//				int w = this.element.getWidth() + (diffX - this.element.getWidth());
//				if (w >= 5) {
//					this.element.setWidth(w);
//					if (isShiftPressed) {
//						int h = this.getAspectHeight(this.startWidth, this.startHeight, w);
//						if (h >= 5) {
//							this.element.setHeight(h);
//						}
//					}
//				}
//			}
//		}
//
//		if (!this.element.stretchY) {
//			if (g == 2) { //top
//				int h = this.startHeight + this.getOppositeInt(diffY);
//				if (h >= 5) {
//					this.element.baseY = this.startY + diffY;
//					this.element.setHeight(h);
//					if (isShiftPressed) {
//						int w = this.getAspectWidth(this.startWidth, this.startHeight, h);
//						if (w >= 5) {
//							this.element.setWidth(w);
//						}
//					}
//				}
//			}
//			if (g == 3) { //bottom
//				int h = this.element.getHeight() + (diffY - this.element.getHeight());
//				if (h >= 5) {
//					this.element.setHeight(h);
//					if (isShiftPressed) {
//						int w = this.getAspectWidth(this.startWidth, this.startHeight, h);
//						if (w >= 5) {
//							this.element.setWidth(w);
//						}
//					}
//				}
//			}
//		}
//	}
//
//	private int getOppositeInt(int i) {
//		if (Math.abs(i) == i) {
//			return Math.negateExact(i);
//		} else {
//			return Math.abs(i);
//		}
//	}
//
//	public boolean isDragged() {
//		return this.dragging;
//	}
//
//	public boolean isGettingResized() {
//		return this.resizing;
//	}
//
//	public boolean isLeftClicked() {
//		return (this.isHovered() && MouseInput.isLeftMouseDown());
//	}
//
//	public boolean isRightClicked() {
//		return (this.isHovered() && MouseInput.isRightMouseDown());
//	}
//
//	public boolean isHovered() {
//		return this.hovered;
//	}
//
//	/**
//	 * Sets the BASE position of this object (NOT the absolute position!)
//	 */
//	public void setRawX(int x) {
//		this.element.baseX = x;
//	}
//
//	/**
//	 * Sets the BASE position of this object (NOT the absolute position!)
//	 */
//	public void setRawY(int y) {
//		this.element.baseY = y;
//	}
//
//	/**
//	 * Returns the ABSOLUTE position of this object (NOT the base position!)
//	 */
//	public int getX() {
//		return this.element.getX();
//	}
//
//	/**
//	 * Returns the ABSOLUTE position of this object (NOT the base position!)
//	 */
//	public int getY() {
//		return this.element.getY();
//	}
//
//	public void setWidth(int width) {
//		this.element.setWidth(width);
//	}
//
//	public void setHeight(int height) {
//		this.element.setHeight(height);
//	}
//
//	public int getWidth() {
//		return this.element.getWidth();
//	}
//
//	public int getHeight() {
//		return this.element.getHeight();
//	}
//
//	public boolean isDestroyable() {
//		return this.settings.isDestroyable();
//	}
//
//	public boolean isStretchable() {
//		return this.settings.isStretchable();
//	}
//
//	public void destroyElement() {
//		if (!this.settings.isDestroyable()) {
//			return;
//		}
//		if (FancyMenu.getConfig().getOrDefault("editordeleteconfirmation", true)) {
//			PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
//				if (call) {
//					this.editor.deleteContentQueue.add(this);
//				}
//			}, "§c§l" + Locals.localize("helper.creator.messages.sure"), "", Locals.localize("helper.creator.deleteobject"), "", "", "", "", ""));
//		} else {
//			this.editor.deleteContentQueue.add(this);
//		}
//	}
//
//	public void resetElementStates() {
//		hovered = false;
//		dragging = false;
//		resizing = false;
//		activeGrabber = -1;
//		if (this.rightClickContextMenu != null) {
//			this.rightClickContextMenu.closeMenu();
//		}
//		this.editor.setObjectFocused(this, false, true);
//	}
//
//	/** Called when a vanilla button object was updated in the editor. **/
//	public void onUpdateVanillaButton(LayoutVanillaButton btn) {
//		if (this.element.anchorPointElementIdentifier != null) {
//			String id = "vanillabtn:" + btn.getButtonId();
//			if (this.element.anchorPointElementIdentifier.equals(id)) {
//				this.element.anchorPointElement = this.editor.getElementByInstanceIdentifier(id).element;
//			}
//		}
//	}
//
//	@Override
//	public void setFocused(boolean var1) {}
//
//	@Override
//	public boolean isFocused() {
//		return false;
//	}
//
//	@Override
//	public void mouseMoved(double mouseX, double mouseY) {
//
//	}
//
//	@Override
//	public boolean mouseClicked(double mouseX, double mouseY, int button) {
//
//		return false;
//	}
//
//	@Override
//	public boolean mouseReleased(double mouseX, double mouseY, int button) {
//
//		return false;
//	}
//
//	@Override
//	public boolean mouseDragged(double $$0, double $$1, int $$2, double $$3, double $$4) {
//		return GuiEventListener.super.mouseDragged($$0, $$1, $$2, $$3, $$4);
//	}
//
//	@Override
//	public boolean mouseScrolled(double $$0, double $$1, double $$2) {
//		return GuiEventListener.super.mouseScrolled($$0, $$1, $$2);
//	}
//
//	@Override
//	public boolean keyPressed(int $$0, int $$1, int $$2) {
//		return GuiEventListener.super.keyPressed($$0, $$1, $$2);
//	}
//
//	@Override
//	public boolean keyReleased(int $$0, int $$1, int $$2) {
//		return GuiEventListener.super.keyReleased($$0, $$1, $$2);
//	}
//
//	@Override
//	public boolean charTyped(char c, int key) {
//
//		return false;
//	}
//
//	@Override
//	public boolean isMouseOver(double mouseX, double mouseY) {
//		return (mouseX >= this.element.getX()) && (mouseX <= this.element.getX() + this.element.getWidth()) && (mouseY >= this.element.getY()) && mouseY <= this.element.getY() + this.element.getHeight();
//	}
//
//	public abstract List<PropertiesSection> getProperties();
//
//}
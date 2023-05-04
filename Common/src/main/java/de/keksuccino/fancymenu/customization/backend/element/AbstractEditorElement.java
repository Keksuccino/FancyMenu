package de.keksuccino.fancymenu.customization.backend.element;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorHistory;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.elements.button.LayoutVanillaButton;
import de.keksuccino.fancymenu.customization.frontend.layouteditor.loadingrequirements.ManageRequirementsScreen;
import de.keksuccino.fancymenu.rendering.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorScreen;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.rendering.ui.FMContextMenu;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.rendering.ui.popup.FMYesNoPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;

public abstract class AbstractEditorElement extends GuiComponent {
	
	public AbstractElement element;
	public LayoutEditorScreen editor;
	protected boolean hovered = false;
	protected boolean dragging = false;
	protected boolean resizing = false;
	protected int activeGrabber = -1;
	protected int lastGrabber;
	protected int startDiffX;
	protected int startDiffY;
	protected int startX;
	protected int startY;
	protected int startWidth;
	protected int startHeight;
	protected boolean stretchable = false;
	protected boolean orderable = true;
	protected boolean copyable = true;
	protected boolean delayable = true;
	protected boolean fadeable = true;
	protected boolean resizeable = true;
	protected boolean supportsAdvancedPositioning = true;
	protected boolean supportsAdvancedSizing = true;
	protected boolean resizeableX = true;
	protected boolean resizeableY = true;
	protected boolean dragable = true;
	protected boolean orientationCanBeChanged = true;
	protected boolean enableElementIdCopyButton = true;
	protected boolean allowOrientationByElement = true;

	public List<AbstractEditorElement> hoveredLayers = new ArrayList<>();

	public FMContextMenu rightClickContextMenu;

	protected AdvancedButton oElement;
	protected AdvancedButton o1;
	protected AdvancedButton o2;
	protected AdvancedButton o3;
	protected AdvancedButton o4;
	protected AdvancedButton o5;
	protected AdvancedButton o6;
	protected AdvancedButton o7;
	protected AdvancedButton o8;
	protected AdvancedButton o9;

	protected static boolean isShiftPressed = false;
	private static boolean shiftListener = false;
	
	private final boolean destroyable;
	public boolean enableVisibilityRequirements = true;

	private LayoutEditorHistory.Snapshot cachedSnapshot;
	private boolean moving = false;
	
	protected static final long CURSOR_HORIZONTAL_RESIZE = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR);
	protected static final long CURSOR_VERTICAL_RESIZE = GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR);
	protected static final long CURSOR_NORMAL = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);

	public AbstractEditorElement(@NotNull AbstractElement element, boolean destroyable, @NotNull LayoutEditorScreen editor, boolean doInit) {
		this.editor = editor;
		this.element = element;
		this.destroyable = destroyable;

		if (!shiftListener) {
			KeyboardHandler.addKeyPressedListener(d -> {
				if ((d.keycode == 340) || (d.keycode == 344)) {
					isShiftPressed = true;
				}
			});
			KeyboardHandler.addKeyReleasedListener(d -> {
				if ((d.keycode == 340) || (d.keycode == 344)) {
					isShiftPressed = false;
				}
			});
			shiftListener = true;
		}

		if (doInit) {
			this.init();
		}
	}

	public AbstractEditorElement(@Nonnull AbstractElement element, boolean destroyable, @Nonnull LayoutEditorScreen editor) {
		this(element, destroyable, editor, true);
	}
	
	public void init() {
		
		this.rightClickContextMenu = new FMContextMenu();
		this.rightClickContextMenu.setAlwaysOnTop(true);

		if (this.enableElementIdCopyButton) {
			AdvancedButton copyIdButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.copyid"), true, (press) -> {
				if (!(this instanceof LayoutVanillaButton)) {
					Minecraft.getInstance().keyboardHandler.setClipboard(this.element.getInstanceIdentifier());
				} else {
					Minecraft.getInstance().keyboardHandler.setClipboard("vanillabtn:" + ((LayoutVanillaButton)this).getButtonId());
				}
			});
			copyIdButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.copyid.btn.desc"), "%n%"));
			this.rightClickContextMenu.addContent(copyIdButton);
		}

		if (this instanceof LayoutVanillaButton) {
			AdvancedButton copyLocatorButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.vanilla_button.copy_locator"), true, (press) -> {
				String locator = this.editor.getScreenToCustomizeIdentifier() + ":" + ((LayoutVanillaButton)this).getButtonId();
				Minecraft.getInstance().keyboardHandler.setClipboard(locator);
			});
			copyLocatorButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.vanilla_button.copy_locator.desc"), "%n%"));
			this.rightClickContextMenu.addContent(copyLocatorButton);
		}

		this.rightClickContextMenu.addSeparator();

		// ORIENTATION
		if (this.orientationCanBeChanged) {

			FMContextMenu orientationMenu = new FMContextMenu();
			orientationMenu.setAutoclose(true);
			this.rightClickContextMenu.addChild(orientationMenu);

			oElement = new AdvancedButton(0, 0, 0, 16, "element", (press) -> {
				this.editor.setObjectFocused(this, false, true);
				FMTextInputPopup pop = new FMTextInputPopup(new Color(0, 0, 0, 0), Locals.localize("fancymenu.helper.editor.items.orientation.element.setidentifier"), null, 240, (call) -> {
					if (call != null) {
						AbstractEditorElement l = this.editor.getElementByActionId(call);
						if (l != null) {
							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
							this.element.orientationElementIdentifier = call;
							this.element.orientationElement = l.element;
							this.editor.history.setPreventSnapshotSaving(true);
							this.setOrientation("element");
							this.editor.history.setPreventSnapshotSaving(false);
						} else {
							PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("fancymenu.helper.editor.items.orientation.element.setidentifier.identifiernotfound")));
						}
					}
				});
				if (this.element.orientationElementIdentifier != null) {
					pop.setText(this.element.orientationElementIdentifier);
				}
				PopupHandler.displayPopup(pop);
				orientationMenu.closeMenu();
			});
			oElement.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.orientation.element.btn.desc"), "%n%"));
			if (this.allowOrientationByElement) {
				orientationMenu.addContent(oElement);
			}

			orientationMenu.addSeparator();

			o1 = new AdvancedButton(0, 0, 0, 16, "top-left", (press) -> {
				this.editor.setObjectFocused(this, false, true);
				this.setOrientation("top-left");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o1);

			o2 = new AdvancedButton(0, 0, 0, 16, "mid-left", (press) -> {
				this.editor.setObjectFocused(this, false, true);
				this.setOrientation("mid-left");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o2);

			o3 = new AdvancedButton(0, 0, 0, 16, "bottom-left", (press) -> {
				this.editor.setObjectFocused(this, false, true);
				this.setOrientation("bottom-left");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o3);

			o4 = new AdvancedButton(0, 0, 0, 16, "top-centered", (press) -> {
				this.editor.setObjectFocused(this, false, true);
				this.setOrientation("top-centered");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o4);

			o5 = new AdvancedButton(0, 0, 0, 16, "mid-centered", (press) -> {
				this.editor.setObjectFocused(this, false, true);
				this.setOrientation("mid-centered");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o5);

			o6 = new AdvancedButton(0, 0, 0, 16, "bottom-centered", (press) -> {
				this.editor.setObjectFocused(this, false, true);
				this.setOrientation("bottom-centered");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o6);

			o7 = new AdvancedButton(0, 0, 0, 16, "top-right", (press) -> {
				this.editor.setObjectFocused(this, false, true);
				this.setOrientation("top-right");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o7);

			o8 = new AdvancedButton(0, 0, 0, 16, "mid-right", (press) -> {
				this.editor.setObjectFocused(this, false, true);
				this.setOrientation("mid-right");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o8);

			o9 = new AdvancedButton(0, 0, 0, 16, "bottom-right", (press) -> {
				this.editor.setObjectFocused(this, false, true);
				this.setOrientation("bottom-right");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o9);

			AdvancedButton orientationButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.setorientation"), true, (press) -> {
				orientationMenu.setParentButton((AdvancedButton) press);
				orientationMenu.openMenuAt(0, press.y);
			}) {
				@Override
				public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
					this.active = (element.advancedX == null) && (element.advancedY == null);
					super.render(p_93657_, p_93658_, p_93659_, p_93660_);
				}
			};
			orientationButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.orientation.btndesc"), "%n%"));
			this.rightClickContextMenu.addContent(orientationButton);

		}

		// ADVANCED POSITIONING
		FMContextMenu advancedPositioningMenu = new FMContextMenu();
		advancedPositioningMenu.setAutoclose(true);
		this.rightClickContextMenu.addChild(advancedPositioningMenu);

		AdvancedButton advancedPositioningButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
			advancedPositioningMenu.setParentButton((AdvancedButton) press);
			advancedPositioningMenu.openMenuAt(0, press.y);
		}) {
			@Override
			public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
				if ((element.advancedX != null) || (element.advancedY != null)) {
					this.setMessage(Locals.localize("fancymenu.helper.editor.items.features.advanced_positioning.active"));
				} else {
					this.setMessage(Locals.localize("fancymenu.helper.editor.items.features.advanced_positioning"));
				}
				super.render(p_93657_, p_93658_, p_93659_, p_93660_);
			}
		};
		advancedPositioningButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.features.advanced_positioning.desc"), "%n%"));
		if (this.supportsAdvancedPositioning) {
			this.rightClickContextMenu.addContent(advancedPositioningButton);
		}

		AdvancedButton advancedPosXButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.features.advanced_positioning.posx"), true, (press) -> {
			TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("fancymenu.helper.editor.items.features.advanced_positioning.posx")), this.editor, null, (call) -> {
				if (call != null) {
					this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
					if (call.replace(" ", "").equals("")) {
						this.element.advancedX = null;
					} else {
						this.element.advancedX = call;
					}
					this.element.rawX = 0;
					this.element.rawY = 0;
					this.element.orientation = "top-left";
				}
			});
			s.multilineMode = false;
			if (this.element.advancedX != null) {
				s.setText(this.element.advancedX);
			}
			Minecraft.getInstance().setScreen(s);
		});
		advancedPositioningMenu.addContent(advancedPosXButton);

		AdvancedButton advancedPosYButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.features.advanced_positioning.posy"), true, (press) -> {
			TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("fancymenu.helper.editor.items.features.advanced_positioning.posy")), this.editor, null, (call) -> {
				if (call != null) {
					this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
					if (call.replace(" ", "").equals("")) {
						this.element.advancedY = null;
					} else {
						this.element.advancedY = call;
					}
					this.element.rawX = 0;
					this.element.rawY = 0;
					this.element.orientation = "top-left";
				}
			});
			s.multilineMode = false;
			if (this.element.advancedY != null) {
				s.setText(this.element.advancedY);
			}
			Minecraft.getInstance().setScreen(s);
		});
		advancedPositioningMenu.addContent(advancedPosYButton);

		FMContextMenu advancedSizingMenu = new FMContextMenu();
		advancedSizingMenu.setAutoclose(true);
		this.rightClickContextMenu.addChild(advancedSizingMenu);

		AdvancedButton advancedSizingButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
			advancedSizingMenu.setParentButton((AdvancedButton) press);
			advancedSizingMenu.openMenuAt(0, press.y);
		}) {
			@Override
			public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
				if ((element.advancedWidth != null) || (element.advancedHeight != null)) {
					this.setMessage(Locals.localize("fancymenu.helper.editor.items.features.advanced_sizing.active"));
				} else {
					this.setMessage(Locals.localize("fancymenu.helper.editor.items.features.advanced_sizing"));
				}
				super.render(p_93657_, p_93658_, p_93659_, p_93660_);
			}
		};
		advancedSizingButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.features.advanced_sizing.desc"), "%n%"));
		if (this.supportsAdvancedSizing) {
			this.rightClickContextMenu.addContent(advancedSizingButton);
		}

		AdvancedButton advancedWidthButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.features.advanced_sizing.width"), true, (press) -> {
			TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("fancymenu.helper.editor.items.features.advanced_sizing.width")), this.editor, null, (call) -> {
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
						if ((this instanceof LayoutVanillaButton) && (this.element.orientation.equals("original"))) {
							this.element.orientation = "top-left";
							this.element.rawX = 0;
							this.element.rawY = 0;
						}
					}
				}
			});
			s.multilineMode = false;
			if (this.element.advancedWidth != null) {
				s.setText(this.element.advancedWidth);
			}
			Minecraft.getInstance().setScreen(s);
		});
		advancedSizingMenu.addContent(advancedWidthButton);

		AdvancedButton advancedHeightButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.features.advanced_sizing.height"), true, (press) -> {
			TextEditorScreen s = new TextEditorScreen(Component.literal(Locals.localize("fancymenu.helper.editor.items.features.advanced_sizing.height")), this.editor, null, (call) -> {
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
						if ((this instanceof LayoutVanillaButton) && (this.element.orientation.equals("original"))) {
							this.element.orientation = "top-left";
							this.element.rawX = 0;
							this.element.rawY = 0;
						}
					}
				}
			});
			s.multilineMode = false;
			if (this.element.advancedHeight != null) {
				s.setText(this.element.advancedHeight);
			}
			Minecraft.getInstance().setScreen(s);
		});
		advancedSizingMenu.addContent(advancedHeightButton);

		FMContextMenu layersMenu = new FMContextMenu();
		layersMenu.setAutoclose(true);
		this.rightClickContextMenu.addChild(layersMenu);
		
		AdvancedButton layersButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.chooselayer"), true, (press) -> {
			layersMenu.getContent().clear();
			for (AbstractEditorElement o : this.hoveredLayers) {
				String label = o.element.builder.getDisplayName();
				if (Minecraft.getInstance().font.width(label) > 200) {
					label = Minecraft.getInstance().font.plainSubstrByWidth(label, 200) + "..";
				}
				AdvancedButton btn = new AdvancedButton(0, 0, 0, 0, label, (press2) -> {
					this.editor.clearFocusedObjects();
					this.editor.setObjectFocused(o, true, true);
				});
				layersMenu.addContent(btn);
			}
			layersMenu.setParentButton((AdvancedButton) press);
			layersMenu.openMenuAt(0, press.y);
		});
		this.rightClickContextMenu.addContent(layersButton);

		this.rightClickContextMenu.addSeparator();

		//TODO stretch besser handeln

		AdvancedButton stretchXButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
			if (isOrientationSupportedByStretchAction(!this.element.stretchX, this.element.stretchY)) {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				this.element.stretchX = !this.element.stretchX;
			}
		}) {
			@Override
			public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
				this.active = element.advancedWidth == null;
				if (element.stretchX) {
					this.setMessage(Locals.localize("helper.creator.object.stretch.x.on"));
				} else {
					this.setMessage(Locals.localize("helper.creator.object.stretch.x.off"));
				}
				super.render(p_93657_, p_93658_, p_93659_, p_93660_);
			}
		};
		stretchXButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.stretch.x.desc"), "%n%"));
		this.rightClickContextMenu.addContent(stretchXButton);

		AdvancedButton stretchYButton = new AdvancedButton(0, 0, 0, 0, "", true, (press) -> {
			if (isOrientationSupportedByStretchAction(this.element.stretchX, !this.element.stretchY)) {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				this.element.stretchY = !this.element.stretchY;
			}
		}) {
			@Override
			public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
				this.active = element.advancedHeight == null;
				if (element.stretchY) {
					this.setMessage(Locals.localize("helper.creator.object.stretch.y.on"));
				} else {
					this.setMessage(Locals.localize("helper.creator.object.stretch.y.off"));
				}
				super.render(p_93657_, p_93658_, p_93659_, p_93660_);
			}
		};
		stretchYButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.stretch.y.desc"), "%n%"));
		this.rightClickContextMenu.addContent(stretchYButton);

		this.rightClickContextMenu.addSeparator();

		AdvancedButton moveUpButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.object.moveup"), (press) -> {
			AbstractEditorElement o = this.editor.moveUp(this);
			if (o != null) {
				((AdvancedButton)press).setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.moveup.desc", Locals.localize("helper.creator.object.moveup.desc.subtext", o.element.builder.getDisplayName())), "%n%"));
			}
		});
		moveUpButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.moveup.desc", ""), "%n%"));
		if (this.orderable) {
			this.rightClickContextMenu.addContent(moveUpButton);
		}

		AdvancedButton moveDownButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.object.movedown"), (press) -> {
			AbstractEditorElement o = this.editor.moveDown(this);
			if (o != null) {
				if (o instanceof LayoutVanillaButton) {
					((AdvancedButton)press).setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.movedown.desc", Locals.localize("helper.creator.object.movedown.desc.subtext.vanillabutton")), "%n%"));
				} else {
					((AdvancedButton)press).setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.movedown.desc", Locals.localize("helper.creator.object.movedown.desc.subtext", o.element.builder.getDisplayName())), "%n%"));
				}
			}
		});
		moveDownButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.movedown.desc", ""), "%n%"));
		if (this.orderable) {
			this.rightClickContextMenu.addContent(moveDownButton);
		}

		AdvancedButton loadingRequirementsButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.editor.loading_requirement.elements.loading_requirements"), (press) -> {
			ManageRequirementsScreen s = new ManageRequirementsScreen(this.editor, this.element.loadingRequirementContainer, (call) -> {});
			this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
			Minecraft.getInstance().setScreen(s);
		});
		loadingRequirementsButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.editor.loading_requirement.elements.loading_requirements.desc"), "%n%"));
		if (this.enableVisibilityRequirements) {
			this.rightClickContextMenu.addContent(loadingRequirementsButton);
		}

		AdvancedButton copyButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.editor.ui.edit.copy"), (press) -> {
			this.editor.copySelectedElements();
		});
		if (this.copyable) {
			this.rightClickContextMenu.addContent(copyButton);
		}

		AdvancedButton destroyButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.delete"), true, (press) -> {
			this.destroyElement();
		});
		if (this.destroyable) {
			this.rightClickContextMenu.addContent(destroyButton);
		}

		FMContextMenu delayMenu = new FMContextMenu();
		delayMenu.setAutoclose(true);
		this.rightClickContextMenu.addChild(delayMenu);
		
		String tdLabel = Locals.localize("helper.creator.items.delay.off");
		if (this.element.delayAppearance) {
			tdLabel = Locals.localize("helper.creator.items.delay.everytime");
		}
		if (this.element.delayAppearance && !this.element.delayAppearanceEverytime) {
			tdLabel = Locals.localize("helper.creator.items.delay.firsttime");
		}
		AdvancedButton toggleDelayButton = new AdvancedButton(0, 0, 0, 0, tdLabel, true, (press) -> {
			this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
			if (!this.element.delayAppearance) {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.delay.firsttime"));
				this.element.delayAppearance = true;
				this.element.delayAppearanceEverytime = false;
			} else if (this.element.delayAppearance && !this.element.delayAppearanceEverytime) {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.delay.everytime"));
				this.element.delayAppearance = true;
				this.element.delayAppearanceEverytime = true;
			} else if (this.element.delayAppearance && this.element.delayAppearanceEverytime) {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.delay.off"));
				this.element.delayAppearance = false;
				this.element.delayAppearanceEverytime = false;
			}
		});
		delayMenu.addContent(toggleDelayButton);
		
		AdvancedButton delaySecondsButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.delay.seconds"), true, (press) -> {
			FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§f" + Locals.localize("helper.creator.items.delay.seconds"), CharacterFilter.getDoubleCharacterFiler(), 240, (call) -> {
				if (call != null) {
					if (!call.equals("" + this.element.delayAppearanceSec)) {
						this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
					}
					if (call.replace(" ", "").equals("")) {
						this.element.delayAppearanceSec = 1.0F;
					} else if (MathUtils.isFloat(call)) {
						this.element.delayAppearanceSec = Float.parseFloat(call);
					}
				}
			});
			p.setText("" + this.element.delayAppearanceSec);
			PopupHandler.displayPopup(p);
		}) {
			@Override
			public void render(@NotNull PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
				this.active = AbstractEditorElement.this.element.delayAppearance;
				super.render(matrix, mouseX, mouseY, partialTicks);
			}
		};
		delayMenu.addContent(delaySecondsButton);
		
		delayMenu.addSeparator();
		
		String fiLabel = Locals.localize("helper.creator.items.delay.fadein.off");
		if (this.element.delayAppearance && this.element.fadeIn) {
			fiLabel = Locals.localize("helper.creator.items.delay.fadein.on");
		}
		AdvancedButton toggleFadeButton = new AdvancedButton(0, 0, 0, 0, fiLabel, true, (press) -> {
			if (!this.element.fadeIn) {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.delay.fadein.on"));
				this.element.fadeIn = true;
			} else {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.delay.fadein.off"));
				this.element.fadeIn = false;
			}
		}) {
			@Override
			public void render(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
				this.active = AbstractEditorElement.this.element.delayAppearance;
				super.render(matrixStack, mouseX, mouseY, partialTicks);
			}
		};
		if (this.fadeable) {
			delayMenu.addContent(toggleFadeButton);
		}
		
		AdvancedButton fadeSpeedButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.delay.fadein.speed"), true, (press) -> {
			FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§f" + Locals.localize("helper.creator.items.delay.fadein.speed"), CharacterFilter.getDoubleCharacterFiler(), 240, (call) -> {
				if (call != null) {
					if (!call.equals("" + this.element.fadeInSpeed)) {
						this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
					}
					if (call.replace(" ", "").equals("")) {
						this.element.fadeInSpeed = 1.0F;
					} else if (MathUtils.isFloat(call)) {
						this.element.fadeInSpeed = Float.parseFloat(call);
					}
				}
			});
			p.setText("" + this.element.fadeInSpeed);
			PopupHandler.displayPopup(p);
		}) {
			@Override
			public void render(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
				this.active = AbstractEditorElement.this.element.delayAppearance;
				super.render(matrixStack, mouseX, mouseY, partialTicks);
			}
		};
		if (this.fadeable) {
			delayMenu.addContent(fadeSpeedButton);
		}
		
		AdvancedButton delayButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.delay"), true, (press) -> {
			delayMenu.setParentButton((AdvancedButton) press);
			delayMenu.openMenuAt(0, press.y);
		});
		if (this.delayable) {
			this.rightClickContextMenu.addContent(delayButton);
		}
		
		this.rightClickContextMenu.addSeparator();

	}

	protected void setOrientation(String pos) {
		if (!this.orientationCanBeChanged) {
			return;
		}
		this.editor.history.saveSnapshot(this.editor.history.createSnapshot());

		if (pos.equals("mid-left")) {
			this.element.orientation = pos;
			this.element.rawX = 0;
			this.element.rawY = -(this.element.getHeight() / 2);
		} else if (pos.equals("bottom-left")) {
			this.element.orientation = pos;
			this.element.rawX = 0;
			this.element.rawY = -this.element.getHeight();
		} else if (pos.equals("top-centered")) {
			this.element.orientation = pos;
			this.element.rawX = -(this.element.getWidth() / 2);
			this.element.rawY = 0;
		} else if (pos.equals("mid-centered")) {
			this.element.orientation = pos;
			this.element.rawX = -(this.element.getWidth() / 2);
			this.element.rawY = -(this.element.getHeight() / 2);
		} else if (pos.equals("bottom-centered")) {
			this.element.orientation = pos;
			this.element.rawX = -(this.element.getWidth() / 2);
			this.element.rawY = -this.element.getHeight();
		} else if (pos.equals("top-right")) {
			this.element.orientation = pos;
			this.element.rawX = -this.element.getWidth();
			this.element.rawY = 0;
		} else if (pos.equals("mid-right")) {
			this.element.orientation = pos;
			this.element.rawX = -this.element.getWidth();
			this.element.rawY = -(this.element.getHeight() / 2);
		} else if (pos.equals("bottom-right")) {
			this.element.orientation = pos;
			this.element.rawX = -this.element.getWidth();
			this.element.rawY = -this.element.getHeight();
		} else if (pos.equals("element") && (this.element.orientationElement != null)) {
			this.element.orientation = pos;
			this.element.rawX = 10;
			this.element.rawY = 10;
		} else {
			this.element.orientation = pos;
			this.element.rawX = 0;
			this.element.rawY = 0;
		}

	}
	
	protected int orientationMouseX(int mouseX) {
		if (this.element.orientation.endsWith("-centered")) {
			return mouseX - (this.editor.width / 2);
		}
		if (this.element.orientation.endsWith("-right")) {
			return mouseX - this.editor.width;
		}
		return mouseX;
	}
	
	protected int orientationMouseY(int mouseY) {
		if (this.element.orientation.startsWith("mid-")) {
			return mouseY - (this.editor.height / 2);
		}
		if (this.element.orientation.startsWith("bottom-")) {
			return mouseY - this.editor.height;
		}
		return mouseY;
	}

	public void setStretchedX(boolean b, boolean saveSnapshot) {
		if (this.isOrientationSupportedByStretchAction(b, this.stretchY)) {
			if (saveSnapshot) {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
			}
			this.stretchX = b;
			String stretchXLabel = Locals.localize("helper.creator.object.stretch.x");
			if (this.stretchX) {
				stretchXLabel = "§a" + stretchXLabel;
			}
			if (this.stretchXButton != null) {
				this.stretchXButton.setMessage(stretchXLabel);
			}
		}
	}

	public void setStretchedY(boolean b, boolean saveSnapshot) {
		if (this.isOrientationSupportedByStretchAction(this.stretchX, b)) {
			if (saveSnapshot) {
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
			}
			this.stretchY = b;
			String stretchYLabel = Locals.localize("helper.creator.object.stretch.y");
			if (this.stretchY) {
				stretchYLabel = "§a" + stretchYLabel;
			}
			if (this.stretchYButton != null) {
				this.stretchYButton.setMessage(stretchYLabel);
			}
		}
	}

	private boolean isOrientationSupportedByStretchAction(boolean stX, boolean stY) {
		try {
			if (stX && !stY) {
				if (!this.element.orientation.equals("top-left") && !this.element.orientation.equals("mid-left") && !this.element.orientation.equals("bottom-left")) {
					return false;
				}
			}
			if (stY && !stX) {
				if (!this.element.orientation.equals("top-left") && !this.element.orientation.equals("top-centered") && !this.element.orientation.equals("top-right")) {
					return false;
				}
			}
			if (stX && stY) {
				return this.element.orientation.equals("top-left");
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private void handleStretch() {
		try {
			if (this.stretchX) {
				this.element.rawX = 0;
				this.element.setWidth(Minecraft.getInstance().screen.width);
			}
			if (this.stretchY) {
				this.element.rawY = 0;
				this.element.setHeight(Minecraft.getInstance().screen.height);
			}
			if (this.stretchX || this.stretchY) {
				this.oElement.active = false;
			}
			if (this.orientationCanBeChanged) {
				if (this.stretchX && !this.stretchY) {
					this.o1.active = true;
					this.o2.active = true;
					this.o3.active = true;
					this.o4.active = false;
					this.o5.active = false;
					this.o6.active = false;
					this.o7.active = false;
					this.o8.active = false;
					this.o9.active = false;
				}
				if (this.stretchY && !this.stretchX) {
					this.o1.active = true;
					this.o2.active = false;
					this.o3.active = false;
					this.o4.active = true;
					this.o5.active = false;
					this.o6.active = false;
					this.o7.active = true;
					this.o8.active = false;
					this.o9.active = false;
				}
				if (this.stretchX && this.stretchY) {
					this.o1.active = true;
					this.o2.active = false;
					this.o3.active = false;
					this.o4.active = false;
					this.o5.active = false;
					this.o6.active = false;
					this.o7.active = false;
					this.o8.active = false;
					this.o9.active = false;
				}
				if (!this.stretchX && !this.stretchY) {
					this.o1.active = true;
					this.o2.active = true;
					this.o3.active = true;
					this.o4.active = true;
					this.o5.active = true;
					this.o6.active = true;
					this.o7.active = true;
					this.o8.active = true;
					this.o9.active = true;
					this.oElement.active = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void render(PoseStack matrix, int mouseX, int mouseY) {
		this.updateHovered(mouseX, mouseY);

		//Render the customization item
        try {
			this.element.render(matrix, editor);

			this.handleStretch();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
        
		// Renders the border around the object if its focused (starts to render one tick after the object got focused)
		if (this.editor.isFocused(this)) {
			this.renderBorder(matrix, mouseX, mouseY);
		} else {
			if ((this.editor.getTopHoverObject() == this) && (!this.editor.isObjectFocused() || (!this.editor.isFocusedHovered() && !this.editor.isFocusedDragged() && !this.editor.isFocusedGettingResized() && !this.editor.isFocusedGrabberPressed()))) {
				this.renderHighlightBorder(matrix);
			}
		}
		
		//Reset cursor to default
		if ((this.activeGrabber == -1) && (!MouseInput.isLeftMouseDown() || PopupHandler.isPopupActive())) {
			GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), CURSOR_NORMAL);
		}
				
		//Update dragging state
		if (this.dragable && (this.element.advancedX == null) && (this.element.advancedY == null)) {
			if (this.isLeftClicked() && !(this.resizing || this.isGrabberPressed())) {
				this.dragging = true;
			} else {
				if (!MouseInput.isLeftMouseDown()) {
					this.dragging = false;
				}
			}
		} else {
			this.dragging = false;
		}
		
		//Handles the resizing process
		if (this.resizeable) {
			if ((this.isGrabberPressed() || this.resizing) && !this.isDragged() && this.editor.isFocused(this)) {
				if (this.editor.getFocusedObjects().size() == 1) {
					if (!this.resizing) {
						this.cachedSnapshot = this.editor.history.createSnapshot();

						this.lastGrabber = this.getActiveResizeGrabber();
					}
					this.resizing = true;
					this.handleResize(this.orientationMouseX(mouseX), this.orientationMouseY(mouseY));
				}
			}
		} else {
			this.resizing = false;
		}
		
		//Moves the object with the mouse motion if dragged
		if (this.isDragged() && this.editor.isFocused(this)) {
			if (this.editor.getFocusedObjects().size() == 1) {
				if (!this.moving) {
					this.cachedSnapshot = this.editor.history.createSnapshot();
				}

				this.moving = true;
				
				if ((mouseX >= 5) && (mouseX <= this.editor.width -5)) {
					if (!this.stretchX) {
						this.element.rawX = this.orientationMouseX(mouseX) - this.startDiffX;
					}
				}
				if ((mouseY >= 5) && (mouseY <= this.editor.height -5)) {
					if (!this.stretchY) {
						this.element.rawY = this.orientationMouseY(mouseY) - this.startDiffY;
					}
				}
			}
		}
		if (!this.isDragged()) {
			this.startDiffX = this.orientationMouseX(mouseX) - this.element.rawX;
			this.startDiffY = this.orientationMouseY(mouseY) - this.element.rawY;

			if (((this.startX != this.element.rawX) || (this.startY != this.element.rawY)) && this.moving) {
				if (this.cachedSnapshot != null) {
					this.editor.history.saveSnapshot(this.cachedSnapshot);
				}
			}

			this.moving = false;
		}

		if (!MouseInput.isLeftMouseDown()) {
			if (((this.startWidth != this.element.getWidth()) || (this.startHeight != this.element.getHeight())) && this.resizing) {
				if (this.cachedSnapshot != null) {
					this.editor.history.saveSnapshot(this.cachedSnapshot);
				}
			}
			
			this.startX = this.element.rawX;
			this.startY = this.element.rawY;
			this.startWidth = this.element.getWidth();
			this.startHeight = this.element.getHeight();
			this.resizing = false;
		}

	}
	
	protected void renderBorder(PoseStack matrix, int mouseX, int mouseY) {
		//horizontal line top
		GuiComponent.fill(matrix, this.element.getX(editor), this.element.getY(editor), this.element.getX(editor) + this.element.getWidth(), this.element.getY(editor) + 1, Color.BLUE.getRGB());
		//horizontal line bottom
		GuiComponent.fill(matrix, this.element.getX(editor), this.element.getY(editor) + this.element.getHeight() - 1, this.element.getX(editor) + this.element.getWidth(), this.element.getY(editor) + this.element.getHeight(), Color.BLUE.getRGB());
		//vertical line left
		GuiComponent.fill(matrix, this.element.getX(editor), this.element.getY(editor), this.element.getX(editor) + 1, this.element.getY(editor) + this.element.getHeight(), Color.BLUE.getRGB());
		//vertical line right
		GuiComponent.fill(matrix, this.element.getX(editor) + this.element.getWidth() - 1, this.element.getY(editor), this.element.getX(editor) + this.element.getWidth(), this.element.getY(editor) + this.element.getHeight(), Color.BLUE.getRGB());

		int w = 4;
		int h = 4;

		int yHorizontal = this.element.getY(editor) + (this.element.getHeight() / 2) - (h / 2);
		int xHorizontalLeft = this.element.getX(editor) - (w / 2);
		int xHorizontalRight = this.element.getX(editor) + this.element.getWidth() - (w / 2);
		
		int xVertical = this.element.getX(editor) + (this.element.getWidth() / 2) - (w / 2);
		int yVerticalTop = this.element.getY(editor) - (h / 2);
		int yVerticalBottom = this.element.getY(editor) + this.element.getHeight() - (h / 2);

		if (this.dragable && this.resizeable && (this.element.advancedX == null) && (this.element.advancedY == null) && (this.element.advancedWidth == null) && (this.element.advancedHeight == null)) {
			if (!this.stretchX && this.resizeableX) {
				//grabber left
				GuiComponent.fill(matrix, xHorizontalLeft, yHorizontal, xHorizontalLeft + w, yHorizontal + h, Color.BLUE.getRGB());
				//grabber right
				GuiComponent.fill(matrix, xHorizontalRight, yHorizontal, xHorizontalRight + w, yHorizontal + h, Color.BLUE.getRGB());
			}
			if (!this.stretchY && this.resizeableY) {
				//grabber top
				GuiComponent.fill(matrix, xVertical, yVerticalTop, xVertical + w, yVerticalTop + h, Color.BLUE.getRGB());
				//grabber bottom
				GuiComponent.fill(matrix, xVertical, yVerticalBottom, xVertical + w, yVerticalBottom + h, Color.BLUE.getRGB());
			}
		}

		//Update cursor and active grabber when grabber is hovered
		if (this.resizeable && (this.element.advancedX == null) && (this.element.advancedY == null) && (this.element.advancedWidth == null) && (this.element.advancedHeight == null)) {
			if ((mouseX >= xHorizontalLeft) && (mouseX <= xHorizontalLeft + w) && (mouseY >= yHorizontal) && (mouseY <= yHorizontal + h)) {
				if (!this.stretchX && this.resizeableX) {
					GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), CURSOR_HORIZONTAL_RESIZE);
					this.activeGrabber = 0;
				} else {
					this.activeGrabber = -1;
				}
			} else if ((mouseX >= xHorizontalRight) && (mouseX <= xHorizontalRight + w) && (mouseY >= yHorizontal) && (mouseY <= yHorizontal + h)) {
				if (!this.stretchX && this.resizeableX) {
					GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), CURSOR_HORIZONTAL_RESIZE);
					this.activeGrabber = 1;
				} else {
					this.activeGrabber = -1;
				}
			} else if ((mouseX >= xVertical) && (mouseX <= xVertical + w) && (mouseY >= yVerticalTop) && (mouseY <= yVerticalTop + h)) {
				if (!this.stretchY && this.resizeableY) {
					GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), CURSOR_VERTICAL_RESIZE);
					this.activeGrabber = 2;
				} else {
					this.activeGrabber = -1;
				}
			} else if ((mouseX >= xVertical) && (mouseX <= xVertical + w) && (mouseY >= yVerticalBottom) && (mouseY <= yVerticalBottom + h)) {
				if (!this.stretchY && this.resizeableY) {
					GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), CURSOR_VERTICAL_RESIZE);
					this.activeGrabber = 3;
				} else {
					this.activeGrabber = -1;
				}
			} else {
				this.activeGrabber = -1;
			}
		} else {
			this.activeGrabber = -1;
		}

		//Render pos and size values
		RenderUtils.setScale(matrix, 0.5F);
		GuiComponent.drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.orientation") + ": " + this.element.orientation, this.element.getX(editor)*2, (this.element.getY(editor)*2) - 26, Color.WHITE.getRGB());
		GuiComponent.drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.posx") + ": " + this.element.getX(editor), this.element.getX(editor)*2, (this.element.getY(editor)*2) - 17, Color.WHITE.getRGB());
		GuiComponent.drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.width") + ": " + this.element.getWidth(), this.element.getX(editor)*2, (this.element.getY(editor)*2) - 8, Color.WHITE.getRGB());
		
		GuiComponent.drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.posy") + ": " + this.element.getY(editor), ((this.element.getX(editor) + this.element.getWidth())*2)+3, ((this.element.getY(editor) + this.element.getHeight())*2) - 14, Color.WHITE.getRGB());
		GuiComponent.drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.height") + ": " + this.element.getHeight(), ((this.element.getX(editor) + this.element.getWidth())*2)+3, ((this.element.getY(editor) + this.element.getHeight())*2) - 5, Color.WHITE.getRGB());
		RenderUtils.postScale(matrix);
	}

	protected void renderHighlightBorder(PoseStack matrix) {
		Color c = new Color(0, 200, 255, 255);
		//horizontal line top
		GuiComponent.fill(matrix, this.element.getX(editor), this.element.getY(editor), this.element.getX(editor) + this.element.getWidth(), this.element.getY(editor) + 1, c.getRGB());
		//horizontal line bottom
		GuiComponent.fill(matrix, this.element.getX(editor), this.element.getY(editor) + this.element.getHeight() - 1, this.element.getX(editor) + this.element.getWidth(), this.element.getY(editor) + this.element.getHeight(), c.getRGB());
		//vertical line left
		GuiComponent.fill(matrix, this.element.getX(editor), this.element.getY(editor), this.element.getX(editor) + 1, this.element.getY(editor) + this.element.getHeight(), c.getRGB());
		//vertical line right
		GuiComponent.fill(matrix, this.element.getX(editor) + this.element.getWidth() - 1, this.element.getY(editor), this.element.getX(editor) + this.element.getWidth(), this.element.getY(editor) + this.element.getHeight(), c.getRGB());
	}
	
	/**
	 * <b>Returns:</b><br><br>
	 * 
	 * -1 if NO grabber is currently pressed<br>
	 * 0 if the LEFT grabber is pressed<br>
	 * 1 if the RIGHT grabber is pressed<br>
	 * 2 if the TOP grabber is pressed<br>
	 * 3 if the BOTTOM grabber is pressed
	 * 
	 */
	public int getActiveResizeGrabber() {
		return this.activeGrabber;
	}
	
	public boolean isGrabberPressed() {
		return ((this.getActiveResizeGrabber() != -1) && MouseInput.isLeftMouseDown());
	}

	protected int getAspectWidth(int startW, int startH, int height) {
		double ratio = (double) startW / (double) startH;
		return (int)(height * ratio);
	}

	protected int getAspectHeight(int startW, int startH, int width) {
		double ratio = (double) startW / (double) startH;
		return (int)(width / ratio);
	}
	
	protected void handleResize(int mouseX, int mouseY) {

		int g = this.lastGrabber;
		int diffX;
		int diffY;
		
		//X difference
		if (mouseX > this.startX) {
			diffX = Math.abs(mouseX - this.startX);
		} else {
			diffX = Math.negateExact(this.startX - mouseX);
		}
		//Y difference
		if (mouseY > this.startY) {
			diffY = Math.abs(mouseY - this.startY);
		} else {
			diffY = Math.negateExact(this.startY - mouseY);
		}

		if (!this.stretchX) {
			if (g == 0) { //left
				int w = this.startWidth + this.getOppositeInt(diffX);
				if (w >= 5) {
					this.element.rawX = this.startX + diffX;
					this.element.setWidth(w);
					if (isShiftPressed) {
						int h = this.getAspectHeight(this.startWidth, this.startHeight, w);
						if (h >= 5) {
							this.element.setHeight(h);
						}
					}
				}
			}
			if (g == 1) { //right
				int w = this.element.getWidth() + (diffX - this.element.getWidth());
				if (w >= 5) {
					this.element.setWidth(w);
					if (isShiftPressed) {
						int h = this.getAspectHeight(this.startWidth, this.startHeight, w);
						if (h >= 5) {
							this.element.setHeight(h);
						}
					}
				}
			}
		}

		if (!this.stretchY) {
			if (g == 2) { //top
				int h = this.startHeight + this.getOppositeInt(diffY);
				if (h >= 5) {
					this.element.rawY = this.startY + diffY;
					this.element.setHeight(h);
					if (isShiftPressed) {
						int w = this.getAspectWidth(this.startWidth, this.startHeight, h);
						if (w >= 5) {
							this.element.setWidth(w);
						}
					}
				}
			}
			if (g == 3) { //bottom
				int h = this.element.getHeight() + (diffY - this.element.getHeight());
				if (h >= 5) {
					this.element.setHeight(h);
					if (isShiftPressed) {
						int w = this.getAspectWidth(this.startWidth, this.startHeight, h);
						if (w >= 5) {
							this.element.setWidth(w);
						}
					}
				}
			}
		}
	}

	private int getOppositeInt(int i) {
		if (Math.abs(i) == i) {
			return Math.negateExact(i);
		} else {
			return Math.abs(i);
		}
	}

	protected void updateHovered(int mouseX, int mouseY) {
		this.hovered = (mouseX >= this.element.getX(editor)) && (mouseX <= this.element.getX(editor) + this.element.getWidth()) && (mouseY >= this.element.getY(editor)) && mouseY <= this.element.getY(editor) + this.element.getHeight();
	}
	
	public boolean isDragged() {
		return this.dragging;
	}

	public boolean isGettingResized() {
		return this.resizing;
	}
	
	public boolean isLeftClicked() {
		return (this.isHoveredOrFocused() && MouseInput.isLeftMouseDown());
	}
	
	public boolean isRightClicked() {
		return (this.isHoveredOrFocused() && MouseInput.isRightMouseDown());
	}
	
	public boolean isHoveredOrFocused() {
		return this.hovered;
	}
	
	/**
	 * Sets the BASE position of this object (NOT the absolute position!)
	 */
	public void setX(int x) {
		this.element.rawX = x;
	}
	
	/**
	 * Sets the BASE position of this object (NOT the absolute position!)
	 */
	public void setY(int y) {
		this.element.rawY = y;
	}
	
	/**
	 * Returns the ABSOLUTE position of this object (NOT the base position!)
	 */
	public int getX() {
		return this.element.getX(editor);
	}
	
	/**
	 * Returns the ABSOLUTE position of this object (NOT the base position!)
	 */
	public int getY() {
		return this.element.getY(editor);
	}

	public void setWidth(int width) {
		this.element.setWidth(width);
	}

	public void setHeight(int height) {
		this.element.setHeight(height);
	}

	public int getWidth() {
		return this.element.getWidth();
	}

	public int getHeight() {
		return this.element.getHeight();
	}
	
	public boolean isDestroyable() {
		return this.destroyable;
	}

	public boolean isStretchable() {
		return this.stretchable;
	}

	public void destroyElement() {
		if (!this.destroyable) {
			return;
		}
		if (FancyMenu.getConfig().getOrDefault("editordeleteconfirmation", true)) {
			PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
				if (call) {
					this.editor.deleteContentQueue.add(this);
				}
			}, "§c§l" + Locals.localize("helper.creator.messages.sure"), "", Locals.localize("helper.creator.deleteobject"), "", "", "", "", ""));
		} else {
			this.editor.deleteContentQueue.add(this);
		}
	}

	public void resetElementStates() {
		hovered = false;
		dragging = false;
		resizing = false;
		activeGrabber = -1;
		if (this.rightClickContextMenu != null) {
			this.rightClickContextMenu.closeMenu();
		}
		this.editor.setObjectFocused(this, false, true);
	}

	/** Called when a vanilla button object was updated in the editor. **/
	public void onUpdateVanillaButton(LayoutVanillaButton btn) {
		if (this.element.orientationElementIdentifier != null) {
			String id = "vanillabtn:" + btn.getButtonId();
			if (this.element.orientationElementIdentifier.equals(id)) {
				this.element.orientationElement = this.editor.getElementByActionId(id).element;
			}
		}
	}

	public abstract List<PropertiesSection> getProperties();

}

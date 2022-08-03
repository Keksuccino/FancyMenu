package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.visibilityrequirements.VisibilityRequirementsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import de.keksuccino.fancymenu.api.visibilityrequirements.VisibilityRequirement;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMNotificationPopup;
import de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.VisibilityRequirementContainer;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.EditHistory.Snapshot;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button.LayoutVanillaButton;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.FMContextMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMYesNoPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;

public abstract class LayoutElement extends GuiComponent {

	public CustomizationItemBase object;
	public LayoutEditorScreen handler;
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
	protected boolean stretchX = false;
	protected boolean stretchY = false;
	protected boolean orderable = true;
	protected boolean copyable = true;
	protected boolean delayable = true;
	protected boolean fadeable = true;
	protected boolean resizeable = true;
	protected boolean resizeableX = true;
	protected boolean resizeableY = true;
	protected boolean dragable = true;
	protected boolean orientationCanBeChanged = true;
	protected boolean enableElementIdCopyButton = true;
	protected boolean allowOrientationByElement = true;

	protected List<LayoutElement> hoveredLayers = new ArrayList<LayoutElement>();

	public FMContextMenu rightclickMenu;

	protected AdvancedButton stretchXButton;
	protected AdvancedButton stretchYButton;

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

	public final String objectId = UUID.randomUUID().toString();

	private Snapshot cachedSnapshot;
	private boolean moving = false;

	protected static final long hResizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR);
	protected static final long vResizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR);
	protected static final long normalCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);

	public LayoutElement(@NotNull CustomizationItemBase object, boolean destroyable, @NotNull LayoutEditorScreen handler, boolean doInit) {
		this.handler = handler;
		this.object = object;
		this.destroyable = destroyable;

		if (!shiftListener) {
			KeyboardHandler.addKeyPressedListener(new Consumer<KeyboardData>() {
				@Override
				public void accept(KeyboardData t) {
					if ((t.keycode == 340) || (t.keycode == 344)) {
						isShiftPressed = true;
					}
				}
			});
			KeyboardHandler.addKeyReleasedListener(new Consumer<KeyboardData>() {
				@Override
				public void accept(KeyboardData t) {
					if ((t.keycode == 340) || (t.keycode == 344)) {
						isShiftPressed = false;
					}
				}
			});
			shiftListener = true;
		}

		if (doInit) {
			this.init();
		}
	}

	public LayoutElement(@NotNull CustomizationItemBase object, boolean destroyable, @NotNull LayoutEditorScreen handler) {
		this(object, destroyable, handler, true);
	}

	public void init() {

		this.rightclickMenu = new FMContextMenu();
		this.rightclickMenu.setAlwaysOnTop(true);

		/** COPY ELEMENT ID **/
		if (this.enableElementIdCopyButton) {
			AdvancedButton copyIdButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.copyid"), true, (press) -> {
				if (!(this instanceof LayoutVanillaButton)) {
					Minecraft.getInstance().keyboardHandler.setClipboard(this.object.getActionId());
				} else {
					Minecraft.getInstance().keyboardHandler.setClipboard("vanillabtn:" + ((LayoutVanillaButton) this).getButtonId());
				}
			});
			copyIdButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.copyid.btn.desc"), "%n%"));
			this.rightclickMenu.addContent(copyIdButton);
		}

		if (this instanceof LayoutVanillaButton) {

			AdvancedButton copyLocatorButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.vanilla_button.copy_locator"), true, (press) -> {
				String locator = this.handler.getScreenToCustomizeIdentifier() + ":" + ((LayoutVanillaButton)this).getButtonId();
				Minecraft.getInstance().keyboardHandler.setClipboard(locator);
			});
			copyLocatorButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.vanilla_button.copy_locator.desc"), "%n%"));
			this.rightclickMenu.addContent(copyLocatorButton);

		}

		this.rightclickMenu.addSeparator();

		/** ORIENTATION **/
		if (this.orientationCanBeChanged) {
			FMContextMenu orientationMenu = new FMContextMenu();
			orientationMenu.setAutoclose(true);
			this.rightclickMenu.addChild(orientationMenu);

			oElement = new AdvancedButton(0, 0, 0, 16, "element", (press) -> {
				this.handler.setObjectFocused(this, false, true);
				FMTextInputPopup pop = new FMTextInputPopup(new Color(0, 0, 0, 0), Locals.localize("fancymenu.helper.editor.items.orientation.element.setidentifier"), null, 240, (call) -> {
					if (call != null) {
						LayoutElement l = this.handler.getElementByActionId(call);
						if (l != null) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
							this.object.orientationElementIdentifier = call;
							this.object.orientationElement = l.object;
							this.handler.history.setPreventSnapshotSaving(true);
							this.setOrientation("element");
							this.handler.history.setPreventSnapshotSaving(false);
						} else {
							PopupHandler.displayPopup(new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("fancymenu.helper.editor.items.orientation.element.setidentifier.identifiernotfound")));
						}
					}
				});
				if (this.object.orientationElementIdentifier != null) {
					pop.setText(this.object.orientationElementIdentifier);
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
				this.handler.setObjectFocused(this, false, true);
				this.setOrientation("top-left");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o1);

			o2 = new AdvancedButton(0, 0, 0, 16, "mid-left", (press) -> {
				this.handler.setObjectFocused(this, false, true);
				this.setOrientation("mid-left");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o2);

			o3 = new AdvancedButton(0, 0, 0, 16, "bottom-left", (press) -> {
				this.handler.setObjectFocused(this, false, true);
				this.setOrientation("bottom-left");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o3);

			o4 = new AdvancedButton(0, 0, 0, 16, "top-centered", (press) -> {
				this.handler.setObjectFocused(this, false, true);
				this.setOrientation("top-centered");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o4);

			o5 = new AdvancedButton(0, 0, 0, 16, "mid-centered", (press) -> {
				this.handler.setObjectFocused(this, false, true);
				this.setOrientation("mid-centered");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o5);

			o6 = new AdvancedButton(0, 0, 0, 16, "bottom-centered", (press) -> {
				this.handler.setObjectFocused(this, false, true);
				this.setOrientation("bottom-centered");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o6);

			o7 = new AdvancedButton(0, 0, 0, 16, "top-right", (press) -> {
				this.handler.setObjectFocused(this, false, true);
				this.setOrientation("top-right");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o7);

			o8 = new AdvancedButton(0, 0, 0, 16, "mid-right", (press) -> {
				this.handler.setObjectFocused(this, false, true);
				this.setOrientation("mid-right");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o8);

			o9 = new AdvancedButton(0, 0, 0, 16, "bottom-right", (press) -> {
				this.handler.setObjectFocused(this, false, true);
				this.setOrientation("bottom-right");
				orientationMenu.closeMenu();
			});
			orientationMenu.addContent(o9);

			AdvancedButton orientationButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.setorientation"), true, (press) -> {
				orientationMenu.setParentButton((AdvancedButton) press);
				orientationMenu.openMenuAt(0, press.y);
			});
			orientationButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.orientation.btndesc"), "%n%"));
			this.rightclickMenu.addContent(orientationButton);
		}

		/** LAYERS **/
		FMContextMenu layersMenu = new FMContextMenu();
		layersMenu.setAutoclose(true);
		this.rightclickMenu.addChild(layersMenu);

		AdvancedButton layersButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.chooselayer"), true, (press) -> {

			layersMenu.getContent().clear();

			for (LayoutElement o : this.hoveredLayers) {
				String label = o.object.value;
				if (label == null) {
					label = "Object";
				} else {
					if (Minecraft.getInstance().font.width(label) > 200) {
						label = Minecraft.getInstance().font.plainSubstrByWidth(label, 200) + "..";
					}
				}
				AdvancedButton btn = new AdvancedButton(0, 0, 0, 0, label, (press2) -> {
					this.handler.clearFocusedObjects();
					this.handler.setObjectFocused(o, true, true);
				});
				layersMenu.addContent(btn);
			}

			layersMenu.setParentButton((AdvancedButton) press);
			layersMenu.openMenuAt(0, press.y);

		});
		this.rightclickMenu.addContent(layersButton);

		/** STRETCH **/
		FMContextMenu stretchMenu = new FMContextMenu();
		stretchMenu.setAutoclose(true);
		this.rightclickMenu.addChild(stretchMenu);

		stretchXButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.object.stretch.x"), true, (press) -> {
			if (this.stretchX) {
				this.setStretchedX(false, true);
			} else {
				this.setStretchedX(true, true);
			}
		});
		stretchMenu.addContent(stretchXButton);

		stretchYButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.object.stretch.y"), true, (press) -> {
			if (this.stretchY) {
				this.setStretchedY(false, true);
			} else {
				this.setStretchedY(true, true);
			}
		});
		stretchMenu.addContent(stretchYButton);

		AdvancedButton stretchButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.object.stretch"), true, (press) -> {
			stretchMenu.setParentButton((AdvancedButton) press);
			stretchMenu.openMenuAt(0, press.y);
		});
		if (this.stretchable) {
			this.rightclickMenu.addContent(stretchButton);
		}

		/** MOVE UP **/
		AdvancedButton moveUpButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.object.moveup"), (press) -> {
			LayoutElement o = this.handler.moveUp(this);
			if (o != null) {
				((AdvancedButton)press).setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.moveup.desc", Locals.localize("helper.creator.object.moveup.desc.subtext", o.object.value)), "%n%"));
			}
		});
		moveUpButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.moveup.desc", ""), "%n%"));
		if (this.orderable) {
			this.rightclickMenu.addContent(moveUpButton);
		}

		/** MOVE DOWN **/
		AdvancedButton moveDownButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.object.movedown"), (press) -> {
			LayoutElement o = this.handler.moveDown(this);
			if (o != null) {
				if (o instanceof LayoutVanillaButton) {
					((AdvancedButton)press).setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.movedown.desc", Locals.localize("helper.creator.object.movedown.desc.subtext.vanillabutton")), "%n%"));
				} else {
					((AdvancedButton)press).setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.movedown.desc", Locals.localize("helper.creator.object.movedown.desc.subtext", o.object.value)), "%n%"));
				}
			}
		});
		moveDownButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.object.movedown.desc", ""), "%n%"));
		if (this.orderable) {
			this.rightclickMenu.addContent(moveDownButton);
		}

		/** VISIBILITY REQUIREMENTS **/
		AdvancedButton visibilityRequirementsButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("fancymenu.helper.editor.items.visibilityrequirements"), (press) -> {
			Minecraft.getInstance().setScreen(new VisibilityRequirementsScreen(this.handler, this.object));
		});
		visibilityRequirementsButton.setDescription(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.btn.desc", ""), "%n%"));
		if (this.enableVisibilityRequirements) {
			this.rightclickMenu.addContent(visibilityRequirementsButton);
		}

		/** COPY **/
		AdvancedButton copyButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.editor.ui.edit.copy"), (press) -> {
			this.handler.copySelectedElements();
		});
		if (this.copyable) {
			this.rightclickMenu.addContent(copyButton);
		}

		/** DESTROY **/
		AdvancedButton destroyButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.delete"), true, (press) -> {
			this.destroyObject();
		});
		if (this.destroyable) {
			this.rightclickMenu.addContent(destroyButton);
		}

		/** DELAY APPEARANCE **/
		FMContextMenu delayMenu = new FMContextMenu();
		delayMenu.setAutoclose(true);
		this.rightclickMenu.addChild(delayMenu);

		String tdLabel = Locals.localize("helper.creator.items.delay.off");
		if (this.object.delayAppearance) {
			tdLabel = Locals.localize("helper.creator.items.delay.everytime");
		}
		if (this.object.delayAppearance && !this.object.delayAppearanceEverytime) {
			tdLabel = Locals.localize("helper.creator.items.delay.firsttime");
		}
		AdvancedButton toggleDelayButton = new AdvancedButton(0, 0, 0, 0, tdLabel, true, (press) -> {
			this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			if (!this.object.delayAppearance) {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.delay.firsttime"));
				this.object.delayAppearance = true;
				this.object.delayAppearanceEverytime = false;
			} else if (this.object.delayAppearance && !this.object.delayAppearanceEverytime) {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.delay.everytime"));
				this.object.delayAppearance = true;
				this.object.delayAppearanceEverytime = true;
			} else if (this.object.delayAppearance && this.object.delayAppearanceEverytime) {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.delay.off"));
				this.object.delayAppearance = false;
				this.object.delayAppearanceEverytime = false;
			}
		});
		delayMenu.addContent(toggleDelayButton);

		AdvancedButton delaySecondsButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.delay.seconds"), true, (press) -> {
			FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§f" + Locals.localize("helper.creator.items.delay.seconds"), CharacterFilter.getDoubleCharacterFiler(), 240, (call) -> {
				if (call != null) {
					if (!call.equals("" + this.object.delayAppearanceSec)) {
						this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
					}
					if (call.replace(" ", "").equals("")) {
						this.object.delayAppearanceSec = 1.0F;
					} else if (MathUtils.isFloat(call)) {
						this.object.delayAppearanceSec = Float.parseFloat(call);
					}
				}
			});
			p.setText("" + this.object.delayAppearanceSec);
			PopupHandler.displayPopup(p);
		}) {
			@Override
			public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
				if (!LayoutElement.this.object.delayAppearance) {
					this.active = false;
				} else {
					this.active = true;
				}

				super.render(matrixStack, mouseX, mouseY, partialTicks);
			}
		};
		delayMenu.addContent(delaySecondsButton);

		delayMenu.addSeparator();

		String fiLabel = Locals.localize("helper.creator.items.delay.fadein.off");
		if (this.object.delayAppearance && this.object.fadeIn) {
			fiLabel = Locals.localize("helper.creator.items.delay.fadein.on");
		}
		AdvancedButton toggleFadeButton = new AdvancedButton(0, 0, 0, 0, fiLabel, true, (press) -> {
			if (!this.object.fadeIn) {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.delay.fadein.on"));
				this.object.fadeIn = true;
			} else {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.delay.fadein.off"));
				this.object.fadeIn = false;
			}
		}) {
			@Override
			public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
				if (!LayoutElement.this.object.delayAppearance) {
					this.active = false;
				} else {
					this.active = true;
				}

				super.render(matrixStack, mouseX, mouseY, partialTicks);
			}
		};
		if (this.fadeable) {
			delayMenu.addContent(toggleFadeButton);
		}

		AdvancedButton fadeSpeedButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.delay.fadein.speed"), true, (press) -> {
			FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§f" + Locals.localize("helper.creator.items.delay.fadein.speed"), CharacterFilter.getDoubleCharacterFiler(), 240, (call) -> {
				if (call != null) {
					if (!call.equals("" + this.object.fadeInSpeed)) {
						this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
					}
					if (call.replace(" ", "").equals("")) {
						this.object.fadeInSpeed = 1.0F;
					} else if (MathUtils.isFloat(call)) {
						this.object.fadeInSpeed = Float.parseFloat(call);
					}
				}
			});
			p.setText("" + this.object.fadeInSpeed);
			PopupHandler.displayPopup(p);
		}) {
			@Override
			public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
				if (!LayoutElement.this.object.delayAppearance) {
					this.active = false;
				} else {
					this.active = true;
				}

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
			this.rightclickMenu.addContent(delayButton);
		}

		this.rightclickMenu.addSeparator();

	}

	protected void setOrientation(String pos) {
		if (!this.orientationCanBeChanged) {
			return;
		}
		this.handler.history.saveSnapshot(this.handler.history.createSnapshot());

		if (pos.equals("mid-left")) {
			this.object.orientation = pos;
			this.object.posX = 0;
			this.object.posY = -(this.object.getHeight() / 2);
		} else if (pos.equals("bottom-left")) {
			this.object.orientation = pos;
			this.object.posX = 0;
			this.object.posY = -this.object.getHeight();
		} else if (pos.equals("top-centered")) {
			this.object.orientation = pos;
			this.object.posX = -(this.object.getWidth() / 2);
			this.object.posY = 0;
		} else if (pos.equals("mid-centered")) {
			this.object.orientation = pos;
			this.object.posX = -(this.object.getWidth() / 2);
			this.object.posY = -(this.object.getHeight() / 2);
		} else if (pos.equals("bottom-centered")) {
			this.object.orientation = pos;
			this.object.posX = -(this.object.getWidth() / 2);
			this.object.posY = -this.object.getHeight();
		} else if (pos.equals("top-right")) {
			this.object.orientation = pos;
			this.object.posX = -this.object.getWidth();
			this.object.posY = 0;
		} else if (pos.equals("mid-right")) {
			this.object.orientation = pos;
			this.object.posX = -this.object.getWidth();
			this.object.posY = -(this.object.getHeight() / 2);
		} else if (pos.equals("bottom-right")) {
			this.object.orientation = pos;
			this.object.posX = -this.object.getWidth();
			this.object.posY = -this.object.getHeight();
		} else if (pos.equals("element") && (this.object.orientationElement != null)) {
			this.object.orientation = pos;
			this.object.posX = 10;
			this.object.posY = 10;
		} else {
			this.object.orientation = pos;
			this.object.posX = 0;
			this.object.posY = 0;
		}
	}

	protected int orientationMouseX(int mouseX) {
		if (this.object.orientation.endsWith("-centered")) {
			return mouseX - (this.handler.width / 2);
		}
		if (this.object.orientation.endsWith("-right")) {
			return mouseX - this.handler.width;
		}
		return mouseX;
	}

	protected int orientationMouseY(int mouseY) {
		if (this.object.orientation.startsWith("mid-")) {
			return mouseY - (this.handler.height / 2);
		}
		if (this.object.orientation.startsWith("bottom-")) {
			return mouseY - this.handler.height;
		}
		return mouseY;
	}

	public void setStretchedX(boolean b, boolean saveSnapshot) {
		if (this.isOrientationSupportedByStretchAction(b, this.stretchY)) {
			if (saveSnapshot) {
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
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
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
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
				if (!this.object.orientation.equals("top-left") && !this.object.orientation.equals("mid-left") && !this.object.orientation.equals("bottom-left")) {
					LayoutEditorScreen.displayNotification(Locals.localize("helper.creator.object.stretch.unsupportedorientation", "top-left, mid-left, bottom-left"));
					return false;
				}
			}
			if (stY && !stX) {
				if (!this.object.orientation.equals("top-left") && !this.object.orientation.equals("top-centered") && !this.object.orientation.equals("top-right")) {
					LayoutEditorScreen.displayNotification(Locals.localize("helper.creator.object.stretch.unsupportedorientation", "top-left, top-centered, top-right"));
					return false;
				}
			}
			if (stX && stY) {
				if (!this.object.orientation.equals("top-left")) {
					LayoutEditorScreen.displayNotification(Locals.localize("helper.creator.object.stretch.unsupportedorientation", "top-left"));
					return false;
				}
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
				this.object.posX = 0;
				this.object.setWidth(Minecraft.getInstance().screen.width);
			}
			if (this.stretchY) {
				this.object.posY = 0;
				this.object.setHeight(Minecraft.getInstance().screen.height);
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
			this.object.render(matrix, handler);

			this.handleStretch();

		} catch (IOException e) {
			e.printStackTrace();
		}

		// Renders the border around the object if its focused (starts to render one tick after the object got focused)
		if (this.handler.isFocused(this)) {
			this.renderBorder(matrix, mouseX, mouseY);
		} else {
			if ((this.handler.getTopHoverObject() == this) && (!this.handler.isObjectFocused() || (!this.handler.isFocusedHovered() && !this.handler.isFocusedDragged() && !this.handler.isFocusedGettingResized() && !this.handler.isFocusedGrabberPressed()))) {
				this.renderHighlightBorder(matrix);
			}
		}

		//Reset cursor to default
		if ((this.activeGrabber == -1) && (!MouseInput.isLeftMouseDown() || PopupHandler.isPopupActive())) {
			GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), normalCursor);
		}

		//Update dragging state
		if (this.dragable) {
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
			if ((this.isGrabberPressed() || this.resizing) && !this.isDragged() && this.handler.isFocused(this)) {
				if (this.handler.getFocusedObjects().size() == 1) {
					if (!this.resizing) {
						this.cachedSnapshot = this.handler.history.createSnapshot();

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
		if (this.isDragged() && this.handler.isFocused(this)) {
			if (this.handler.getFocusedObjects().size() == 1) {
				if (!this.moving) {
					this.cachedSnapshot = this.handler.history.createSnapshot();
				}

				this.moving = true;

				if ((mouseX >= 5) && (mouseX <= this.handler.width -5)) {
					if (!this.stretchX) {
						this.object.posX = this.orientationMouseX(mouseX) - this.startDiffX;
					}
				}
				if ((mouseY >= 5) && (mouseY <= this.handler.height -5)) {
					if (!this.stretchY) {
						this.object.posY = this.orientationMouseY(mouseY) - this.startDiffY;
					}
				}
			}
		}
		if (!this.isDragged()) {
			this.startDiffX = this.orientationMouseX(mouseX) - this.object.posX;
			this.startDiffY = this.orientationMouseY(mouseY) - this.object.posY;

			if (((this.startX != this.object.posX) || (this.startY != this.object.posY)) && this.moving) {
				if (this.cachedSnapshot != null) {
					this.handler.history.saveSnapshot(this.cachedSnapshot);
				}
			}

			this.moving = false;
		}

		if (!MouseInput.isLeftMouseDown()) {
			if (((this.startWidth != this.object.getWidth()) || (this.startHeight != this.object.getHeight())) && this.resizing) {
				if (this.cachedSnapshot != null) {
					this.handler.history.saveSnapshot(this.cachedSnapshot);
				}
			}

			this.startX = this.object.posX;
			this.startY = this.object.posY;
			this.startWidth = this.object.getWidth();
			this.startHeight = this.object.getHeight();
			this.resizing = false;
		}

		//Handle rightclick context menu
		if (this.rightclickMenu != null) {

			if (this.isRightClicked() && this.handler.isFocused(this)) {
				if (this.handler.getFocusedObjects().size() == 1) {
					UIBase.openScaledContextMenuAtMouse(this.rightclickMenu);
					this.hoveredLayers.clear();
					for (LayoutElement o : this.handler.getContent()) {
						if (o.isHovered()) {
							this.hoveredLayers.add(o);
						}
					}
				}
			}

			if ((MouseInput.isLeftMouseDown() && !this.rightclickMenu.isHovered())) {
				this.rightclickMenu.closeMenu();
			}
			if (MouseInput.isRightMouseDown() && !this.isHovered() && !this.rightclickMenu.isHovered()) {
				this.rightclickMenu.closeMenu();
			}

			if (this.rightclickMenu.isOpen()) {
				this.handler.setFocusChangeBlocked(objectId, true);
			} else {
				this.handler.setFocusChangeBlocked(objectId, false);
			}

		}
	}

	protected void renderBorder(PoseStack matrix, int mouseX, int mouseY) {
		//horizontal line top
		GuiComponent.fill(matrix, this.object.getPosX(handler), this.object.getPosY(handler), this.object.getPosX(handler) + this.object.getWidth(), this.object.getPosY(handler) + 1, Color.BLUE.getRGB());
		//horizontal line bottom
		GuiComponent.fill(matrix, this.object.getPosX(handler), this.object.getPosY(handler) + this.object.getHeight() - 1, this.object.getPosX(handler) + this.object.getWidth(), this.object.getPosY(handler) + this.object.getHeight(), Color.BLUE.getRGB());
		//vertical line left
		GuiComponent.fill(matrix, this.object.getPosX(handler), this.object.getPosY(handler), this.object.getPosX(handler) + 1, this.object.getPosY(handler) + this.object.getHeight(), Color.BLUE.getRGB());
		//vertical line right
		GuiComponent.fill(matrix, this.object.getPosX(handler) + this.object.getWidth() - 1, this.object.getPosY(handler), this.object.getPosX(handler) + this.object.getWidth(), this.object.getPosY(handler) + this.object.getHeight(), Color.BLUE.getRGB());
		//--------------------------------

		int w = 4;
		int h = 4;

		int yHorizontal = this.object.getPosY(handler) + (this.object.getHeight() / 2) - (h / 2);
		int xHorizontalLeft = this.object.getPosX(handler) - (w / 2);
		int xHorizontalRight = this.object.getPosX(handler) + this.object.getWidth() - (w / 2);

		int xVertical = this.object.getPosX(handler) + (this.object.getWidth() / 2) - (w / 2);
		int yVerticalTop = this.object.getPosY(handler) - (h / 2);
		int yVerticalBottom = this.object.getPosY(handler) + this.object.getHeight() - (h / 2);

		if (this.dragable && this.resizeable) {
			//TODO übernehmen (if)
			if (!this.stretchX && this.resizeableX) {
				//grabber left
				GuiComponent.fill(matrix, xHorizontalLeft, yHorizontal, xHorizontalLeft + w, yHorizontal + h, Color.BLUE.getRGB());
				//grabber right
				GuiComponent.fill(matrix, xHorizontalRight, yHorizontal, xHorizontalRight + w, yHorizontal + h, Color.BLUE.getRGB());
			}
			//TODO übernehmen (if)
			if (!this.stretchY && this.resizeableY) {
				//grabber top
				GuiComponent.fill(matrix, xVertical, yVerticalTop, xVertical + w, yVerticalTop + h, Color.BLUE.getRGB());
				//grabber bottom
				GuiComponent.fill(matrix, xVertical, yVerticalBottom, xVertical + w, yVerticalBottom + h, Color.BLUE.getRGB());
			}
		}

		//Update cursor and active grabber when grabber is hovered
		if (this.resizeable) {
			if ((mouseX >= xHorizontalLeft) && (mouseX <= xHorizontalLeft + w) && (mouseY >= yHorizontal) && (mouseY <= yHorizontal + h)) {
				//TODO übernehmen (if)
				if (!this.stretchX && this.resizeableX) {
					GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), hResizeCursor);
					this.activeGrabber = 0;
				} else {
					this.activeGrabber = -1;
				}
			} else if ((mouseX >= xHorizontalRight) && (mouseX <= xHorizontalRight + w) && (mouseY >= yHorizontal) && (mouseY <= yHorizontal + h)) {
				//TODO übernehmen (if)
				if (!this.stretchX && this.resizeableX) {
					GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), hResizeCursor);
					this.activeGrabber = 1;
				} else {
					this.activeGrabber = -1;
				}
			} else if ((mouseX >= xVertical) && (mouseX <= xVertical + w) && (mouseY >= yVerticalTop) && (mouseY <= yVerticalTop + h)) {
				//TODO übernehmen (if)
				if (!this.stretchY && this.resizeableY) {
					GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), vResizeCursor);
					this.activeGrabber = 2;
				} else {
					this.activeGrabber = -1;
				}
			} else if ((mouseX >= xVertical) && (mouseX <= xVertical + w) && (mouseY >= yVerticalBottom) && (mouseY <= yVerticalBottom + h)) {
				//TODO übernehmen (if)
				if (!this.stretchY && this.resizeableY) {
					GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), vResizeCursor);
					this.activeGrabber = 3;
				} else {
					this.activeGrabber = -1;
				}
			} else {
				this.activeGrabber = -1;
			}
		}

		//Render pos and size values
		RenderUtils.setScale(matrix, 0.5F);
		GuiComponent.drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.orientation") + ": " + this.object.orientation, this.object.getPosX(handler)*2, (this.object.getPosY(handler)*2) - 26, Color.WHITE.getRGB());
		GuiComponent.drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.posx") + ": " + this.object.getPosX(handler), this.object.getPosX(handler)*2, (this.object.getPosY(handler)*2) - 17, Color.WHITE.getRGB());
		GuiComponent.drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.width") + ": " + this.object.getWidth(), this.object.getPosX(handler)*2, (this.object.getPosY(handler)*2) - 8, Color.WHITE.getRGB());

		GuiComponent.drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.posy") + ": " + this.object.getPosY(handler), ((this.object.getPosX(handler) + this.object.getWidth())*2)+3, ((this.object.getPosY(handler) + this.object.getHeight())*2) - 14, Color.WHITE.getRGB());
		GuiComponent.drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.height") + ": " + this.object.getHeight(), ((this.object.getPosX(handler) + this.object.getWidth())*2)+3, ((this.object.getPosY(handler) + this.object.getHeight())*2) - 5, Color.WHITE.getRGB());
		RenderUtils.postScale(matrix);
	}

	protected void renderHighlightBorder(PoseStack matrix) {
		Color c = new Color(0, 200, 255, 255);

		//horizontal line top
		GuiComponent.fill(matrix, this.object.getPosX(handler), this.object.getPosY(handler), this.object.getPosX(handler) + this.object.getWidth(), this.object.getPosY(handler) + 1, c.getRGB());
		//horizontal line bottom
		GuiComponent.fill(matrix, this.object.getPosX(handler), this.object.getPosY(handler) + this.object.getHeight() - 1, this.object.getPosX(handler) + this.object.getWidth(), this.object.getPosY(handler) + this.object.getHeight(), c.getRGB());
		//vertical line left
		GuiComponent.fill(matrix, this.object.getPosX(handler), this.object.getPosY(handler), this.object.getPosX(handler) + 1, this.object.getPosY(handler) + this.object.getHeight(), c.getRGB());
		//vertical line right
		GuiComponent.fill(matrix, this.object.getPosX(handler) + this.object.getWidth() - 1, this.object.getPosY(handler), this.object.getPosX(handler) + this.object.getWidth(), this.object.getPosY(handler) + this.object.getHeight(), c.getRGB());
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
					this.object.posX = this.startX + diffX;
					this.object.setWidth(w);
					if (isShiftPressed) {
						int h = this.getAspectHeight(this.startWidth, this.startHeight, w);
						if (h >= 5) {
							this.object.setHeight(h);
						}
					}
				}
			}
			if (g == 1) { //right
				int w = this.object.getWidth() + (diffX - this.object.getWidth());
				if (w >= 5) {
					this.object.setWidth(w);
					if (isShiftPressed) {
						int h = this.getAspectHeight(this.startWidth, this.startHeight, w);
						if (h >= 5) {
							this.object.setHeight(h);
						}
					}
				}
			}
		}

		if (!this.stretchY) {
			if (g == 2) { //top
				int h = this.startHeight + this.getOppositeInt(diffY);
				if (h >= 5) {
					this.object.posY = this.startY + diffY;
					this.object.setHeight(h);
					if (isShiftPressed) {
						int w = this.getAspectWidth(this.startWidth, this.startHeight, h);
						if (w >= 5) {
							this.object.setWidth(w);
						}
					}
				}
			}
			if (g == 3) { //bottom
				int h = this.object.getHeight() + (diffY - this.object.getHeight());
				if (h >= 5) {
					this.object.setHeight(h);
					if (isShiftPressed) {
						int w = this.getAspectWidth(this.startWidth, this.startHeight, h);
						if (w >= 5) {
							this.object.setWidth(w);
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
		if ((mouseX >= this.object.getPosX(handler)) && (mouseX <= this.object.getPosX(handler) + this.object.getWidth()) && (mouseY >= this.object.getPosY(handler)) && mouseY <= this.object.getPosY(handler) + this.object.getHeight()) {
			this.hovered = true;
		} else {
			this.hovered = false;
		}
	}

	public boolean isDragged() {
		return this.dragging;
	}

	public boolean isGettingResized() {
		return this.resizing;
	}

	public boolean isLeftClicked() {
		return (this.isHovered() && MouseInput.isLeftMouseDown());
	}

	public boolean isRightClicked() {
		return (this.isHovered() && MouseInput.isRightMouseDown());
	}

	public boolean isHovered() {
		return this.hovered;
	}

	/**
	 * Sets the BASE position of this object (NOT the absolute position!)
	 */
	public void setX(int x) {
		this.object.posX = x;
	}

	/**
	 * Sets the BASE position of this object (NOT the absolute position!)
	 */
	public void setY(int y) {
		this.object.posY = y;
	}

	/**
	 * Returns the ABSOLUTE position of this object (NOT the base position!)
	 */
	public int getX() {
		return this.object.getPosX(handler);
	}

	/**
	 * Returns the ABSOLUTE position of this object (NOT the base position!)
	 */
	public int getY() {
		return this.object.getPosY(handler);
	}

	public void setWidth(int width) {
		this.object.setWidth(width);
	}

	public void setHeight(int height) {
		this.object.setHeight(height);
	}

	public int getWidth() {
		return this.object.getWidth();
	}

	public int getHeight() {
		return this.object.getHeight();
	}

	public boolean isDestroyable() {
		return this.destroyable;
	}

	public boolean isStretchable() {
		return this.stretchable;
	}

	public void destroyObject() {
		if (!this.destroyable) {
			return;
		}
		if (FancyMenu.config.getOrDefault("editordeleteconfirmation", true)) {
			PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
				if (call) {
					this.handler.deleteContentQueue.add(this);
				}
			}, "§c§l" + Locals.localize("helper.creator.messages.sure"), "", Locals.localize("helper.creator.deleteobject"), "", "", "", "", ""));
		} else {
			this.handler.deleteContentQueue.add(this);
		}
	}

	public void resetObjectStates() {
		hovered = false;
		dragging = false;
		resizing = false;
		activeGrabber = -1;
		if (this.rightclickMenu != null) {
			this.rightclickMenu.closeMenu();
		}
		this.handler.setFocusChangeBlocked(objectId, false);
		this.handler.setObjectFocused(this, false, true);
	}

	/** Called when a vanilla button object was updated in the editor. **/
	public void onUpdateVanillaButton(LayoutVanillaButton btn) {
		if (this.object.orientationElementIdentifier != null) {
			String id = "vanillabtn:" + btn.getButtonId();
			if (this.object.orientationElementIdentifier.equals(id)) {
				this.object.orientationElement = this.handler.getElementByActionId(id).object;
			}
		}
	}

	public abstract List<PropertiesSection> getProperties();

	public void addVisibilityPropertiesTo(PropertiesSection sec) {

		VisibilityRequirementContainer c = this.object.visibilityRequirementContainer;

		if (c.vrCheckForSingleplayer) {
			sec.addEntry("vr:showif:singleplayer", "" + c.vrShowIfSingleplayer);
		}
		if (c.vrCheckForMultiplayer) {
			sec.addEntry("vr:showif:multiplayer", "" + c.vrShowIfMultiplayer);
		}
		if (c.vrCheckForWindowWidth) {
			String val = "";
			for (int i : c.vrWindowWidth) {
				val += i + ",";
			}
			if (val.length() > 0) {
				val = val.substring(0, val.length() -1);
			}
			if (val.length() > 0) {
				sec.addEntry("vr:showif:windowwidth", "" + c.vrShowIfWindowWidth);
				sec.addEntry("vr:value:windowwidth", val);
			}
		}
		if (c.vrCheckForWindowHeight) {
			String val = "";
			for (int i : c.vrWindowHeight) {
				val += i + ",";
			}
			if (val.length() > 0) {
				val = val.substring(0, val.length() -1);
			}
			if (val.length() > 0) {
				sec.addEntry("vr:showif:windowheight", "" + c.vrShowIfWindowHeight);
				sec.addEntry("vr:value:windowheight", val);
			}
		}
		if (c.vrCheckForWindowWidthBiggerThan) {
			sec.addEntry("vr:showif:windowwidthbiggerthan", "" + c.vrShowIfWindowWidthBiggerThan);
			sec.addEntry("vr:value:windowwidthbiggerthan", "" + c.vrWindowWidthBiggerThan);
		}
		if (c.vrCheckForWindowHeightBiggerThan) {
			sec.addEntry("vr:showif:windowheightbiggerthan", "" + c.vrShowIfWindowHeightBiggerThan);
			sec.addEntry("vr:value:windowheightbiggerthan", "" + c.vrWindowHeightBiggerThan);
		}
		if (c.vrCheckForButtonHovered && (c.vrButtonHovered != null)) {
			sec.addEntry("vr:showif:buttonhovered", "" + c.vrShowIfButtonHovered);
			sec.addEntry("vr:value:buttonhovered", "" + c.vrButtonHovered);
		}
		if (c.vrCheckForWorldLoaded) {
			sec.addEntry("vr:showif:worldloaded", "" + c.vrShowIfWorldLoaded);
		}
		if (c.vrCheckForLanguage && (c.vrLanguage != null)) {
			sec.addEntry("vr:showif:language", "" + c.vrShowIfLanguage);
			sec.addEntry("vr:value:language", "" + c.vrLanguage);
		}
		if (c.vrCheckForFullscreen) {
			sec.addEntry("vr:showif:fullscreen", "" + c.vrShowIfFullscreen);
		}
		if (c.vrCheckForOsWindows) {
			sec.addEntry("vr:showif:oswindows", "" + c.vrShowIfOsWindows);
		}
		if (c.vrCheckForOsMac) {
			sec.addEntry("vr:showif:osmac", "" + c.vrShowIfOsMac);
		}
		if (c.vrCheckForOsLinux) {
			sec.addEntry("vr:showif:oslinux", "" + c.vrShowIfOsLinux);
		}
		if (c.vrCheckForModLoaded) {
			String val = "";
			for (String s : c.vrModLoaded) {
				val += s + ",";
			}
			if (val.length() > 0) {
				val = val.substring(0, val.length() -1);
			}
			if (val.length() > 0) {
				sec.addEntry("vr:showif:modloaded", "" + c.vrShowIfModLoaded);
				sec.addEntry("vr:value:modloaded", val);
			}
		}
		if (c.vrCheckForServerOnline && (c.vrServerOnline != null)) {
			sec.addEntry("vr:showif:serveronline", "" + c.vrShowIfServerOnline);
			sec.addEntry("vr:value:serveronline", "" + c.vrServerOnline);
		}
		if (c.vrCheckForGuiScale) {
			String val = "";
			for (String condition : c.vrGuiScale) {
				if (condition.startsWith("double:")) {
					String value = condition.replace("double:", "");
					val += value + ",";
				} else if (condition.startsWith("biggerthan:")) {
					String value = condition.replace("biggerthan:", "");
					val += ">" + value + ",";
				} else if (condition.startsWith("smallerthan:")) {
					String value = condition.replace("smallerthan:", "");
					val += "<" + value + ",";
				}
			}
			if (val.length() > 0) {
				val = val.substring(0, val.length() -1);
			}
			if (val.length() > 0) {
				sec.addEntry("vr:showif:guiscale", "" + c.vrShowIfGuiScale);
				sec.addEntry("vr:value:guiscale", val);
			}
		}

		//CUSTOM VISIBILITY REQUIREMENTS (API)
		for (VisibilityRequirementContainer.RequirementPackage p : c.customRequirements.values()) {

			VisibilityRequirement v = p.requirement;

			if (p.checkFor) {
				sec.addEntry("vr_custom:showif:" + v.getIdentifier(), "" + p.showIf);
				if (v.hasValue() && (p.value != null)) {
					sec.addEntry("vr_custom:value:" + v.getIdentifier(), "" + p.value);
				}
			}

		}

	}

}

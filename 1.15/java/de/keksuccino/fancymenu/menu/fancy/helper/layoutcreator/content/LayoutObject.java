package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.lwjgl.glfw.GLFW;

import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.EditHistory.Snapshot;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.PopupMenu;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.gui.screens.popup.YesNoPopup;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;

public abstract class LayoutObject extends AbstractGui {
	
	public CustomizationItemBase object;
	protected LayoutCreatorScreen handler;
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
	protected int orientationDiffX = 0;
	protected int orientationDiffY = 0;

	protected List<LayoutObject> hoveredLayers = new ArrayList<LayoutObject>();
	
	protected PopupMenu rightclickMenu;
	protected PopupMenu orientationMenu;
	protected AdvancedButton orientationButton;
	protected PopupMenu layersPopup;
	protected AdvancedButton layersButton;

	protected static boolean isShiftPressed = false;
	private static boolean shiftListener = false;
	
	private final boolean destroyable;

	public final String objectId = UUID.randomUUID().toString();

	private Snapshot cachedSnapshot;
	private boolean moving = false;
	
	protected static final long hResizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR);
	protected static final long vResizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR);
	protected static final long normalCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_CURSOR_NORMAL);
	
	public LayoutObject(@Nonnull CustomizationItemBase object, boolean destroyable, @Nonnull LayoutCreatorScreen handler) {
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
		
		this.init();
	}
	
	protected void init() {
		AdvancedButton o1 = new AdvancedButton(0, 0, 0, 16, "top-left", (press) -> {
			this.handler.setObjectFocused(this, false, true);
			this.setOrientation("top-left");
			this.orientationMenu.closeMenu();
		});
		LayoutCreatorScreen.colorizeCreatorButton(o1);
		AdvancedButton o2 = new AdvancedButton(0, 0, 0, 16, "mid-left", (press) -> {
			this.handler.setObjectFocused(this, false, true);
			this.setOrientation("mid-left");
			this.orientationMenu.closeMenu();
		});
		LayoutCreatorScreen.colorizeCreatorButton(o2);
		AdvancedButton o3 = new AdvancedButton(0, 0, 0, 16, "bottom-left", (press) -> {
			this.handler.setObjectFocused(this, false, true);
			this.setOrientation("bottom-left");
			this.orientationMenu.closeMenu();
		});
		LayoutCreatorScreen.colorizeCreatorButton(o3);
		AdvancedButton o4 = new AdvancedButton(0, 0, 0, 16, "top-centered", (press) -> {
			this.handler.setObjectFocused(this, false, true);
			this.setOrientation("top-centered");
			this.orientationMenu.closeMenu();
		});
		LayoutCreatorScreen.colorizeCreatorButton(o4);
		AdvancedButton o5 = new AdvancedButton(0, 0, 0, 16, "mid-centered", (press) -> {
			this.handler.setObjectFocused(this, false, true);
			this.setOrientation("mid-centered");
			this.orientationMenu.closeMenu();
		});
		LayoutCreatorScreen.colorizeCreatorButton(o5);
		AdvancedButton o6 = new AdvancedButton(0, 0, 0, 16, "bottom-centered", (press) -> {
			this.handler.setObjectFocused(this, false, true);
			this.setOrientation("bottom-centered");
			this.orientationMenu.closeMenu();
		});
		LayoutCreatorScreen.colorizeCreatorButton(o6);
		AdvancedButton o7 = new AdvancedButton(0, 0, 0, 16, "top-right", (press) -> {
			this.handler.setObjectFocused(this, false, true);
			this.setOrientation("top-right");
			this.orientationMenu.closeMenu();
		});
		LayoutCreatorScreen.colorizeCreatorButton(o7);
		AdvancedButton o8 = new AdvancedButton(0, 0, 0, 16, "mid-right", (press) -> {
			this.handler.setObjectFocused(this, false, true);
			this.setOrientation("mid-right");
			this.orientationMenu.closeMenu();
		});
		LayoutCreatorScreen.colorizeCreatorButton(o8);
		AdvancedButton o9 = new AdvancedButton(0, 0, 0, 16, "bottom-right", (press) -> {
			this.handler.setObjectFocused(this, false, true);
			this.setOrientation("bottom-right");
			this.orientationMenu.closeMenu();
		});
		LayoutCreatorScreen.colorizeCreatorButton(o9);
		
		this.orientationMenu = new PopupMenu(100, 16, -1);
		this.orientationMenu.addContent(o1);
		this.orientationMenu.addContent(o2);
		this.orientationMenu.addContent(o3);
		this.orientationMenu.addContent(o4);
		this.orientationMenu.addContent(o5);
		this.orientationMenu.addContent(o6);
		this.orientationMenu.addContent(o7);
		this.orientationMenu.addContent(o8);
		this.orientationMenu.addContent(o9);
		
		this.orientationButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.setorientation"), true, (press) -> {
			this.orientationMenu.openMenuAt(press.x + press.getWidth(), press.y);
		});
		LayoutCreatorScreen.colorizeCreatorButton(this.orientationButton);

		this.layersButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.chooselayer"), true, (press) -> {
			if (this.layersPopup != null) {
				this.rightclickMenu.removeChild(layersPopup);
			}
			this.layersPopup = new PopupMenu(100, 16, -1);
			for (LayoutObject o : this.hoveredLayers) {
				String label = o.object.value;
				if (label == null) {
					label = "Object";
				} else {
					if (Minecraft.getInstance().fontRenderer.getStringWidth(label) > 90) {
						label = Minecraft.getInstance().fontRenderer.trimStringToWidth(label, 85) + "..";
					}
				}
				AdvancedButton btn = new AdvancedButton(0, 0, 0, 0, label, (press2) -> {
					this.handler.setObjectFocused(o, true, true);
				});
				LayoutCreatorScreen.colorizeCreatorButton(btn);
				this.layersPopup.addContent(btn);
			}
			this.rightclickMenu.addChild(layersPopup);
			this.layersPopup.openMenuAt(press.x + press.getWidth(), press.y);
		});
		LayoutCreatorScreen.colorizeCreatorButton(this.layersButton);
				
		this.rightclickMenu = new PopupMenu(110, 16, -1);
		this.rightclickMenu.addContent(this.layersButton);
		this.rightclickMenu.addContent(this.orientationButton);
		this.rightclickMenu.addChild(this.orientationMenu);
		
		if (this.destroyable) {
			AdvancedButton destroy = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.delete"), true, (press) -> {
				this.destroyObject();
			});
			LayoutCreatorScreen.colorizeCreatorButton(destroy);
			this.rightclickMenu.addContent(destroy);
		}
		
		this.handler.addMenu(this.orientationMenu);
		this.handler.addMenu(this.rightclickMenu);
	}
	
	protected void setOrientation(String pos) {
		this.handler.history.saveSnapshot(this.handler.history.createSnapshot());

		if (pos.equals("mid-left")) {
			this.object.orientation = pos;
			this.object.posX = 0;
			this.object.posY = -(this.object.height / 2);
		} else if (pos.equals("bottom-left")) {
			this.object.orientation = pos;
			this.object.posX = 0;
			this.object.posY = -this.object.height;
		} else if (pos.equals("top-centered")) {
			this.object.orientation = pos;
			this.object.posX = -(this.object.width / 2);
			this.object.posY = 0;
		} else if (pos.equals("mid-centered")) {
			this.object.orientation = pos;
			this.object.posX = -(this.object.width / 2);
			this.object.posY = -(this.object.height / 2);
		} else if (pos.equals("bottom-centered")) {
			this.object.orientation = pos;
			this.object.posX = -(this.object.width / 2);
			this.object.posY = -this.object.height;
		} else if (pos.equals("top-right")) {
			this.object.orientation = pos;
			this.object.posX = -this.object.width;
			this.object.posY = 0;
		} else if (pos.equals("mid-right")) {
			this.object.orientation = pos;
			this.object.posX = -this.object.width;
			this.object.posY = -(this.object.height / 2);
		} else if (pos.equals("bottom-right")) {
			this.object.orientation = pos;
			this.object.posX = -this.object.width;
			this.object.posY = -this.object.height;
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
	
	public void render(int mouseX, int mouseY) {
		this.updateHovered(mouseX, mouseY);

		//Render the customization item
        try {
			this.object.render(handler);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
		// Renders the border around the object if its focused (starts to render one tick after the object got focused)
		if (this.handler.isFocused(this)) {
			this.renderBorder(mouseX, mouseY);
		} else {
			LayoutObject f = this.handler.getFocusedObject();
			if ((this.handler.getTopHoverObject() == this) && (!this.handler.isObjectFocused() || (!f.isHovered() && !f.isDragged() && !f.isGettingResized() && !f.isGrabberPressed()))) {
				this.renderHighlightBorder();
			}
		}
		
		//Reset cursor to default
		if ((this.activeGrabber == -1) && (!MouseInput.isLeftMouseDown() || PopupHandler.isPopupActive())) {
			GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), normalCursor);
		}
				
		//Update dragging state
		if (this.isLeftClicked() && !(this.resizing || this.isGrabberPressed())) {
			this.dragging = true;
		} else {
			if (!MouseInput.isLeftMouseDown()) {
				this.dragging = false;
			}
		}
		
		//Handles the resizing process
		if ((this.isGrabberPressed() || this.resizing) && !this.isDragged() && this.handler.isFocused(this)) {
			if (!this.resizing) {
				this.cachedSnapshot = this.handler.history.createSnapshot();
				
				this.lastGrabber = this.getActiveResizeGrabber();
			}
			this.resizing = true;
			this.handleResize(this.orientationMouseX(mouseX), this.orientationMouseY(mouseY));
		}
		
		//Moves the object with the mouse motion if dragged
		if (this.isDragged() && this.handler.isFocused(this)) {

			if (!this.moving) {
				this.cachedSnapshot = this.handler.history.createSnapshot();
			}

			this.moving = true;
			
			if ((mouseX >= 5) && (mouseX <= this.handler.width -5)) {
				this.object.posX = this.orientationMouseX(mouseX) - this.startDiffX;
			}
			if ((mouseY >= 5) && (mouseY <= this.handler.height -5)) {
				this.object.posY = this.orientationMouseY(mouseY) - this.startDiffY;
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
			if (((this.startWidth != this.object.width) || (this.startHeight != this.object.height)) && this.resizing) {
				if (this.cachedSnapshot != null) {
					this.handler.history.saveSnapshot(this.cachedSnapshot);
				}
			}
			
			this.startX = this.object.posX;
			this.startY = this.object.posY;
			this.startWidth = this.object.width;
			this.startHeight = this.object.height;
			this.resizing = false;
		}

		//Handle button options menu
        if (this.rightclickMenu != null) {
        	if (this.isRightClicked() && this.handler.isFocused(this)) {
            	this.rightclickMenu.openMenuAt(mouseX, mouseY);
            	this.hoveredLayers.clear();
            	for (LayoutObject o : this.handler.getContent()) {
            		if (o.isHovered()) {
            			this.hoveredLayers.add(o);
            		}
            	}
            }
        	
        	this.rightclickMenu.render(mouseX, mouseY);

            if ((this.isLeftClicked() || ((MouseInput.isRightMouseDown() || MouseInput.isLeftMouseDown()) && !this.isHovered())) && !this.rightclickMenu.isHovered()) {
            	this.rightclickMenu.closeMenu();
            }
        }
        
        //Handle orientation menu
        if (this.orientationMenu != null) {
            if ((this.isLeftClicked() || ((MouseInput.isRightMouseDown() || MouseInput.isLeftMouseDown()) && !this.isHovered())) && !this.orientationMenu.isHovered() && !this.orientationButton.isHovered()) {
            	this.orientationMenu.closeMenu();
            }
        }

        if (this.layersPopup != null) {
            if ((this.isLeftClicked() || ((MouseInput.isRightMouseDown() || MouseInput.isLeftMouseDown()) && !this.isHovered())) && !this.layersPopup.isHovered() && !this.layersButton.isHovered()) {
            	this.layersPopup.closeMenu();
            }
        }

        if (!(this.handler.isFocusChangeBlocked() && (MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown()))) {
        	if (((this.layersPopup != null && this.layersPopup.isOpen())) || ((this.orientationMenu != null) && this.orientationMenu.isOpen()) || this.rightclickMenu.isOpen()) {
            	this.handler.setFocusChangeBlocked(objectId, true);
            } else {
            	this.handler.setFocusChangeBlocked(objectId, false);
            }
        }
	}
	
	protected void renderBorder(int mouseX, int mouseY) {
		//horizontal line top
		AbstractGui.fill(this.object.getPosX(handler), this.object.getPosY(handler), this.object.getPosX(handler) + this.object.width, this.object.getPosY(handler) + 1, Color.BLUE.getRGB());
		//horizontal line bottom
		AbstractGui.fill(this.object.getPosX(handler), this.object.getPosY(handler) + this.object.height - 1, this.object.getPosX(handler) + this.object.width, this.object.getPosY(handler) + this.object.height, Color.BLUE.getRGB());
		//vertical line left
		AbstractGui.fill(this.object.getPosX(handler), this.object.getPosY(handler), this.object.getPosX(handler) + 1, this.object.getPosY(handler) + this.object.height, Color.BLUE.getRGB());
		//vertical line right
		AbstractGui.fill(this.object.getPosX(handler) + this.object.width - 1, this.object.getPosY(handler), this.object.getPosX(handler) + this.object.width, this.object.getPosY(handler) + this.object.height, Color.BLUE.getRGB());
		
		int w = 4;
		int h = 4;
		
		int yHorizontal = this.object.getPosY(handler) + (this.object.height / 2) - (h / 2);
		int xHorizontalLeft = this.object.getPosX(handler) - (w / 2);
		int xHorizontalRight = this.object.getPosX(handler) + this.object.width - (w / 2);
		
		int xVertical = this.object.getPosX(handler) + (this.object.width / 2) - (w / 2);
		int yVerticalTop = this.object.getPosY(handler) - (h / 2);
		int yVerticalBottom = this.object.getPosY(handler) + this.object.height - (h / 2);
		
		//grabber left
		AbstractGui.fill(xHorizontalLeft, yHorizontal, xHorizontalLeft + w, yHorizontal + h, Color.BLUE.getRGB());
		//grabber right
		AbstractGui.fill(xHorizontalRight, yHorizontal, xHorizontalRight + w, yHorizontal + h, Color.BLUE.getRGB());
		//grabber top
		AbstractGui.fill(xVertical, yVerticalTop, xVertical + w, yVerticalTop + h, Color.BLUE.getRGB());
		//grabber bottom
		AbstractGui.fill(xVertical, yVerticalBottom, xVertical + w, yVerticalBottom + h, Color.BLUE.getRGB());
		
		//Update cursor and active grabber when grabber is hovered
		if ((mouseX >= xHorizontalLeft) && (mouseX <= xHorizontalLeft + w) && (mouseY >= yHorizontal) && (mouseY <= yHorizontal + h)) {
			GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), hResizeCursor);
			this.activeGrabber = 0;
		} else if ((mouseX >= xHorizontalRight) && (mouseX <= xHorizontalRight + w) && (mouseY >= yHorizontal) && (mouseY <= yHorizontal + h)) {
			GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), hResizeCursor);
			this.activeGrabber = 1;
		} else if ((mouseX >= xVertical) && (mouseX <= xVertical + w) && (mouseY >= yVerticalTop) && (mouseY <= yVerticalTop + h)) {
			GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), vResizeCursor);
			this.activeGrabber = 2;
		} else if ((mouseX >= xVertical) && (mouseX <= xVertical + w) && (mouseY >= yVerticalBottom) && (mouseY <= yVerticalBottom + h)) {
			GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), vResizeCursor);
			this.activeGrabber = 3;
		} else {
			this.activeGrabber = -1;
		}
		
		//Render pos and size values
		RenderUtils.setScale(0.5F);
		drawString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.creator.items.border.orientation") + ": " + this.object.orientation, this.object.getPosX(handler)*2, (this.object.getPosY(handler)*2) - 26, Color.WHITE.getRGB());
		drawString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.creator.items.border.posx") + ": " + this.object.getPosX(handler), this.object.getPosX(handler)*2, (this.object.getPosY(handler)*2) - 17, Color.WHITE.getRGB());
		drawString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.creator.items.border.width") + ": " + this.object.width, this.object.getPosX(handler)*2, (this.object.getPosY(handler)*2) - 8, Color.WHITE.getRGB());
		
		drawString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.creator.items.border.posy") + ": " + this.object.getPosY(handler), ((this.object.getPosX(handler) + this.object.width)*2)+3, ((this.object.getPosY(handler) + this.object.height)*2) - 14, Color.WHITE.getRGB());
		drawString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.creator.items.border.height") + ": " + this.object.height, ((this.object.getPosX(handler) + this.object.width)*2)+3, ((this.object.getPosY(handler) + this.object.height)*2) - 5, Color.WHITE.getRGB());
		RenderUtils.postScale();
	}
	
	protected void renderHighlightBorder() {
		Color c = new Color(0, 200, 255, 255);
		
		//horizontal line top
		AbstractGui.fill(this.object.getPosX(handler), this.object.getPosY(handler), this.object.getPosX(handler) + this.object.width, this.object.getPosY(handler) + 1, c.getRGB());
		//horizontal line bottom
		AbstractGui.fill(this.object.getPosX(handler), this.object.getPosY(handler) + this.object.height - 1, this.object.getPosX(handler) + this.object.width, this.object.getPosY(handler) + this.object.height, c.getRGB());
		//vertical line left
		AbstractGui.fill(this.object.getPosX(handler), this.object.getPosY(handler), this.object.getPosX(handler) + 1, this.object.getPosY(handler) + this.object.height, c.getRGB());
		//vertical line right
		AbstractGui.fill(this.object.getPosX(handler) + this.object.width - 1, this.object.getPosY(handler), this.object.getPosX(handler) + this.object.width, this.object.getPosY(handler) + this.object.height, c.getRGB());
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

		if (g == 0) { //left
			int w = this.startWidth + this.getOpponentInt(diffX);
			if (w >= 5) {
				this.object.posX = this.startX + diffX;
				this.object.width = w;
				if (isShiftPressed) {
					int h = this.getAspectHeight(this.startWidth, this.startHeight, w);
					if (h >= 5) {
						this.object.height = h;
					}
				}
			}
		}
		if (g == 1) { //right
			int w = this.object.width + (diffX - this.object.width);
			if (w >= 5) {
				this.object.width = w;
				if (isShiftPressed) {
					int h = this.getAspectHeight(this.startWidth, this.startHeight, w);
					if (h >= 5) {
						this.object.height = h;
					}
				}
			}
		}
		if (g == 2) { //top
			int h = this.startHeight + this.getOpponentInt(diffY);
			if (h >= 5) {
				this.object.posY = this.startY + diffY;
				this.object.height = h;
				if (isShiftPressed) {
					int w = this.getAspectWidth(this.startWidth, this.startHeight, h);
					if (w >= 5) {
						this.object.width = w;
					}
				}
			}
		}
		if (g == 3) { //bottom
			int h = this.object.height + (diffY - this.object.height);
			if (h >= 5) {
				this.object.height = h;
				if (isShiftPressed) {
					int w = this.getAspectWidth(this.startWidth, this.startHeight, h);
					if (w >= 5) {
						this.object.width = w;
					}
				}
			}
		}
	}
	
	private int getOpponentInt(int i) {
		if (Math.abs(i) == i) {
			return Math.negateExact(i);
		} else {
			return Math.abs(i);
		}
	}
	
	protected void updateHovered(int mouseX, int mouseY) {
		if ((mouseX >= this.object.getPosX(handler)) && (mouseX <= this.object.getPosX(handler) + this.object.width) && (mouseY >= this.object.getPosY(handler)) && mouseY <= this.object.getPosY(handler) + this.object.height) {
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
		this.object.width = width;
	}
	
	public void setHeight(int height) {
		this.object.height = height;
	}
	
	public int getWidth() {
		return this.object.width;
	}
	
	public int getHeight() {
		return this.object.height;
	}
	
	public boolean isDestroyable() {
		return this.destroyable;
	}
	
	public void destroyObject() {
		if (!this.destroyable) {
			return;
		}
		this.handler.setMenusUseable(false);
		PopupHandler.displayPopup(new YesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
			if (call) {
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
				
				this.handler.removeContent(this);
			}
			this.handler.setMenusUseable(true);
		}, "§c§l" + Locals.localize("helper.creator.messages.sure"), "", Locals.localize("helper.creator.deleteobject"), "", "", "", "", ""));
	}

	public void resetObjectStates() {
		hovered = false;
		dragging = false;
		resizing = false;
		activeGrabber = -1;
		if (this.orientationMenu != null) {
			this.orientationMenu.closeMenu();
		}
		if (this.rightclickMenu != null) {
			this.rightclickMenu.closeMenu();
		}
		if (this.layersPopup != null) {
			this.layersPopup.closeMenu();
		}
		this.handler.setFocusChangeBlocked(objectId, false);
		this.handler.setObjectFocused(this, false, true);
	}

	public abstract List<PropertiesSection> getProperties();

}

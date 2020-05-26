package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import org.lwjgl.glfw.GLFW;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.content.PopupMenu;
import de.keksuccino.core.gui.screens.popup.PopupHandler;
import de.keksuccino.core.gui.screens.popup.YesNoPopup;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.rendering.RenderUtils;
import de.keksuccino.fancymenu.localization.Locals;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.item.CustomizationItemBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;

public abstract class LayoutObject extends AbstractGui {
	
	public CustomizationItemBase object;
	protected LayoutCreatorScreen handler;
	protected boolean hovered = false;
	protected boolean focused = false;
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
	
	protected PopupMenu rightclickMenu;
	protected PopupMenu orientationMenu;
	protected AdvancedButton orientationButton;
	
	private final boolean destroyable;
	
	protected static final long hResizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR);
	protected static final long vResizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR);
	protected static final long normalCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_CURSOR_NORMAL);
	
	public LayoutObject(@Nonnull CustomizationItemBase object, boolean destroyable, @Nonnull LayoutCreatorScreen handler) {
		this.handler = handler;
		this.object = object;
		this.destroyable = destroyable;
		
		this.init();
	}
	
	protected void init() {
		AdvancedButton o1 = new AdvancedButton(0, 0, 0, 16, "top-left", (press) -> {
			this.setOrientation("top-left");
			this.orientationMenu.closeMenu();
		});
		LayoutCreatorScreen.colorizeCreatorButton(o1);
		AdvancedButton o2 = new AdvancedButton(0, 0, 0, 16, "mid-left", (press) -> {
			this.setOrientation("mid-left");
			this.orientationMenu.closeMenu();
		});
		LayoutCreatorScreen.colorizeCreatorButton(o2);
		AdvancedButton o3 = new AdvancedButton(0, 0, 0, 16, "bottom-left", (press) -> {
			this.setOrientation("bottom-left");
			this.orientationMenu.closeMenu();
		});
		LayoutCreatorScreen.colorizeCreatorButton(o3);
		AdvancedButton o4 = new AdvancedButton(0, 0, 0, 16, "top-centered", (press) -> {
			this.setOrientation("top-centered");
			this.orientationMenu.closeMenu();
		});
		LayoutCreatorScreen.colorizeCreatorButton(o4);
		AdvancedButton o5 = new AdvancedButton(0, 0, 0, 16, "mid-centered", (press) -> {
			this.setOrientation("mid-centered");
			this.orientationMenu.closeMenu();
		});
		LayoutCreatorScreen.colorizeCreatorButton(o5);
		AdvancedButton o6 = new AdvancedButton(0, 0, 0, 16, "bottom-centered", (press) -> {
			this.setOrientation("bottom-centered");
			this.orientationMenu.closeMenu();
		});
		LayoutCreatorScreen.colorizeCreatorButton(o6);
		AdvancedButton o7 = new AdvancedButton(0, 0, 0, 16, "top-right", (press) -> {
			this.setOrientation("top-right");
			this.orientationMenu.closeMenu();
		});
		LayoutCreatorScreen.colorizeCreatorButton(o7);
		AdvancedButton o8 = new AdvancedButton(0, 0, 0, 16, "mid-right", (press) -> {
			this.setOrientation("mid-right");
			this.orientationMenu.closeMenu();
		});
		LayoutCreatorScreen.colorizeCreatorButton(o8);
		AdvancedButton o9 = new AdvancedButton(0, 0, 0, 16, "bottom-right", (press) -> {
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

		this.rightclickMenu = new PopupMenu(110, 16, -1);
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
		this.updateFocused();

		//Render the customization item
        try {
			this.object.render(handler);
		} catch (IOException e) {
			e.printStackTrace();
		}
        
		// Renders the border around the object if its focused (starts to render one tick after the object got focused)
		if (this.handler.isFocused(this)) {
			this.renderBorder(mouseX, mouseY);
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
				
		//Tell the handler if this object is currently focused
		if (this.focused || this.isDragged() || this.resizing || this.isGrabberPressed()) {
			this.handler.setObjectFocused(this, true);
		} else {
			this.handler.setObjectFocused(this, false);
		}
		
		//Handles the resizing process
		if ((this.isGrabberPressed() || this.resizing) && !this.isDragged() && this.handler.isFocused(this)) {
			if (!this.resizing) {
				this.lastGrabber = this.getActiveResizeGrabber();
			}
			this.resizing = true;
			this.handleResize(this.orientationMouseX(mouseX), this.orientationMouseY(mouseY));
		}
		if (!MouseInput.isLeftMouseDown()) {
			this.startX = this.object.posX;
			this.startY = this.object.posY;
			this.startWidth = this.object.width;
			this.startHeight = this.object.height;
			this.resizing = false;
		}
		
		//Moves the object with the mouse motion if dragged
		if (this.isDragged() && this.handler.isFocused(this)) {
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
		}

		//Handle button options menu
        if (this.rightclickMenu != null) {
        	if (this.isRightClicked() && this.handler.isFocused(this)) {
            	this.rightclickMenu.openMenuAt(mouseX, mouseY);
            }
        	
        	this.rightclickMenu.render(mouseX, mouseY);
    		
            if (this.rightclickMenu.isOpen()) {
            	this.handler.setObjectFocused(this, true);
            }
            if ((this.isLeftClicked() || ((MouseInput.isRightMouseDown() || MouseInput.isLeftMouseDown()) && !this.isHovered())) && !this.rightclickMenu.isHovered()) {
            	this.rightclickMenu.closeMenu();
            }
        }
        
        //Handle orientation menu
        if (this.orientationMenu != null) {
            if (this.orientationMenu.isOpen()) {
            	this.handler.setObjectFocused(this, true);
            }
            if ((this.isLeftClicked() || ((MouseInput.isRightMouseDown() || MouseInput.isLeftMouseDown()) && !this.isHovered())) && !this.orientationMenu.isHovered() && !this.orientationButton.isHovered()) {
            	this.orientationMenu.closeMenu();
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
		this.drawString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.creator.items.border.orientation") + ": " + this.object.orientation, this.object.getPosX(handler)*2, (this.object.getPosY(handler)*2) - 26, Color.WHITE.getRGB());
		this.drawString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.creator.items.border.posx") + ": " + this.object.getPosX(handler), this.object.getPosX(handler)*2, (this.object.getPosY(handler)*2) - 17, Color.WHITE.getRGB());
		this.drawString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.creator.items.border.width") + ": " + this.object.width, this.object.getPosX(handler)*2, (this.object.getPosY(handler)*2) - 8, Color.WHITE.getRGB());
		
		this.drawString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.creator.items.border.posy") + ": " + this.object.getPosY(handler), ((this.object.getPosX(handler) + this.object.width)*2)+3, ((this.object.getPosY(handler) + this.object.height)*2) - 14, Color.WHITE.getRGB());
		this.drawString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.creator.items.border.height") + ": " + this.object.height, ((this.object.getPosX(handler) + this.object.width)*2)+3, ((this.object.getPosY(handler) + this.object.height)*2) - 5, Color.WHITE.getRGB());
		RenderUtils.postScale();
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
			}
		}
		if (g == 1) { //right
			int w = this.object.width + (diffX - this.object.width);
			if (w >= 5) {
				this.object.width = w;
			}
		}
		if (g == 2) { //top
			int h = this.startHeight + this.getOpponentInt(diffY);
			if (h >= 5) {
				this.object.posY = this.startY + diffY;
				this.object.height = h;
			}
		}
		if (g == 3) { //bottom
			int h = this.object.height + (diffY - this.object.height);
			if (h >= 5) {
				this.object.height = h;
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
	
	protected void updateFocused() {
		if (this.isLeftClicked() || this.isRightClicked()) {
			this.focused = true;
		}
		if (!this.isHovered() && (MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown())) {
			this.focused = false;
		}
	}
	
	public boolean isDragged() {
		return this.dragging;
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
				this.handler.removeContent(this);
			}
			this.handler.setMenusUseable(true);
		}, "§c§l" + Locals.localize("helper.creator.messages.sure"), "", Locals.localize("helper.creator.deleteobject"), "", "", "", "", ""));
	}

	public abstract List<PropertiesSection> getProperties();

}

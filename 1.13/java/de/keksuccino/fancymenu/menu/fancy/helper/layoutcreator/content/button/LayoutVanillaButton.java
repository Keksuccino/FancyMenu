package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.screens.popup.PopupHandler;
import de.keksuccino.core.gui.screens.popup.TextInputPopup;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutObject;
import net.minecraft.client.Minecraft;

public class LayoutVanillaButton extends LayoutObject {
	
	public final ButtonData button;
	public boolean hidden = false;
	
	public LayoutVanillaButton(ButtonData button, LayoutCreatorScreen handler) {
		super(new LayoutButtonDummyCustomizationItem(button.label, button.width, button.height, button.x, button.y), false, handler);
		this.button = button;
		this.object.orientation = "original";
	}
	
	@Override
	public void init() {
		super.init();
		
		AdvancedButton b0 = new AdvancedButton(0, 0, 0, 16, "Reset Orientation", (press) -> {
			this.object.orientation = "original";
			this.object.posX = this.button.x;
			this.object.posY = this.button.y;
			this.object.width = this.button.width;
			this.object.height = this.button.height;
			this.rightclickMenu.closeMenu();
			this.orientationMenu.closeMenu();
		});
		this.rightclickMenu.addContent(b0);
		LayoutCreatorScreen.colorizeCreatorButton(b0);
		
		AdvancedButton b1 = new AdvancedButton(0, 0, 0, 16, "Hide Button", (press) -> {
			this.handler.hideVanillaButton(this);
		});
		this.rightclickMenu.addContent(b1);
		LayoutCreatorScreen.colorizeCreatorButton(b1);
		
		AdvancedButton b2 = new AdvancedButton(0, 0, 0, 16, "Edit Label", (press) -> {
			this.handler.setMenusUseable(false);
			TextInputPopup i = new TextInputPopup(new Color(0, 0, 0, 0), "§lEdit Label:", null, 240, this::editLabelCallback);
			i.setText(this.object.value);
			PopupHandler.displayPopup(i);
		});
		this.rightclickMenu.addContent(b2);
		LayoutCreatorScreen.colorizeCreatorButton(b2);
	}
	
	@Override
	public void render(int mouseX, int mouseY) {
		if (this.hidden) {
			this.rightclickMenu.closeMenu();
			this.orientationMenu.closeMenu();
		}

		if (!this.canBeModified()) {
			//Cancel dragging
			if (this.isDragged() && this.handler.isFocused(this) && ((this.startX != this.object.posX) || (this.startY != this.object.posY))) {
				this.displaySetOrientationNotification();
				this.object.posX = this.button.x;
				this.object.posY = this.button.y;
				GLFW.glfwSetCursor(Minecraft.getInstance().mainWindow.getHandle(), normalCursor);
				return;
			}
			//Cancel resize
			if ((this.isGrabberPressed() || this.resizing) && !this.isDragged() && this.handler.isFocused(this)) {
				this.displaySetOrientationNotification();
				GLFW.glfwSetCursor(Minecraft.getInstance().mainWindow.getHandle(), normalCursor);
				return;
			}
		}
		
        super.render(mouseX, mouseY);
	}
	
	@Override
	public List<PropertiesSection> getProperties() {
		List<PropertiesSection> l = new ArrayList<PropertiesSection>();
		
		//hidebutton
		if (this.hidden) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "hidebutton");
			s.addEntry("identifier", "%id=" + this.button.getId() + "%");
			l.add(s);
			//Return because no more sections needed
			return l;
		}
		//movebutton
		if (this.canBeModified()) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "movebutton");
			s.addEntry("identifier", "%id=" + this.button.getId() + "%");
			s.addEntry("orientation", this.object.orientation);
			s.addEntry("x", "" + this.object.posX);
			s.addEntry("y", "" + this.object.posY);
			l.add(s);
		}
		// resizebutton
		if (this.canBeModified() && ((this.getWidth() != this.button.width) || (this.getHeight() != this.button.height))) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "resizebutton");
			s.addEntry("identifier", "%id=" + this.button.getId() + "%");
			s.addEntry("width", "" + this.object.width);
			s.addEntry("height", "" + this.object.height);
			l.add(s);
		}
		// renamebutton
		if (this.object.value != this.button.label) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "renamebutton");
			s.addEntry("identifier", "%id=" + this.button.getId() + "%");
			s.addEntry("value", this.object.value);
			l.add(s);
		}
		
		return l;
	}
	
	public void displaySetOrientationNotification() {
		this.handler.displayNotification(300, "§c§lSpecial care required!", "", "§oStandard buttons need some head pats before they listen to you.", "", "", "To §lresize or move §rthem, you have to give them an §lorientation §rfirst!", "", "You can do that by §lright-clicking §rthe button.", "", "", "");
	}
	
	private boolean canBeModified() {
		return !this.object.orientation.equals("original");
	}
	
	private void editLabelCallback(String text) {
		if (text == null) {
			this.handler.setMenusUseable(true);
			return;
		} else {
			this.handler.setVanillaButtonName(this, text);
		}
		this.handler.setMenusUseable(true);
	}
	
}

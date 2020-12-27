package de.keksuccino.core.gui.content;

import java.util.ArrayList;
import java.util.List;

import de.keksuccino.core.input.MouseInput;
import net.minecraft.client.Minecraft;

public class DropdownMenu implements IMenu {
	
	private int width;
	private int height;
	private int x;
	private int y;
	private List<AdvancedButton> content = new ArrayList<AdvancedButton>();
	private AdvancedButton dropdown;
	private boolean opened = false;
	private boolean hovered = false;
	private boolean autoclose = false;
	private int space;
	
	public DropdownMenu(String label, int width, int height, int x, int y, int space) {
		this.width = width;
		this.height = height;
		this.x = x;
		this.y = y;
		this.space = space;
		
		this.dropdown = new AdvancedButton(0, 0, 0, 0, label, true, (press) -> {
			this.toggleMenu();
		});
	}
	
	public void render(int mouseX, int mouseY) {
		float ticks = Minecraft.getInstance().getRenderPartialTicks();
		
		this.updateHovered(mouseX, mouseY);
		
		this.dropdown.height = this.height;
		this.dropdown.width = this.width;
		this.dropdown.x = this.x;
		this.dropdown.y = this.y;
		
		this.dropdown.render(mouseX, mouseY, ticks);
		
		int stackedHeight = this.height + this.space;
		if (this.opened) {
			for (AdvancedButton b : this.content) {
				b.setHandleClick(true);
				b.width = this.width;
				b.x = this.x;
				b.y = this.y + stackedHeight;
				b.render(mouseX, mouseY, ticks);
				
				stackedHeight += b.height + this.space;
			}
		}
		
		if (this.autoclose && !this.isHovered() && (MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown())) {
			this.opened = false;
		}
	}
	
	private void updateHovered(int mouseX, int mouseY) {
		if ((mouseX >= this.dropdown.x) && (mouseX <= this.dropdown.x + this.dropdown.width) && (mouseY >= this.dropdown.y) && mouseY <= this.dropdown.y + this.dropdown.height) {
			this.hovered = true;
			return;
		}
		for (AdvancedButton b : this.content) {
			if ((mouseX >= b.x) && (mouseX <= b.x + b.width) && (mouseY >= b.y) && mouseY <= b.y + b.height) {
				this.hovered = true;
				return;
			}
		}
		this.hovered = false;
	}
	
	public boolean isHovered() {
		if (!this.isOpen()) {
			return false;
		}
		return this.hovered;
	}
	
	public void setUseable(boolean b) {
		this.dropdown.setUseable(b);
		for (AdvancedButton bt : this.content) {
			bt.setUseable(b);
		}
		if (!b) {
			this.opened = false;
		}
	}
	
	public boolean isUseable() {
		if (this.dropdown == null) {
			return false;
		}
		return this.dropdown.isUseable();
	}
	
	public void setAutoclose(boolean b) {
		this.autoclose = b;
	}
	
	public boolean isOpen() {
		return this.opened;
	}
	
	public void openMenu() {
		this.opened = true;
	}
	
	public void closeMenu() {
		this.opened = false;
	}
	
	private void toggleMenu() {
		if (this.opened) {
			this.opened = false;
		} else {
			this.opened = true;
		}
	}
	
	public void addContent(AdvancedButton button) {
		this.content.add(button);
	}
	
	public void setLabel(String text) {
		this.dropdown.displayString = text;
	}
	
	public AdvancedButton getDropdownParent() {
		return this.dropdown;
	}

}

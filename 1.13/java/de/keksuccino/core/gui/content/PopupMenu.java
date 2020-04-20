package de.keksuccino.core.gui.content;

import java.util.ArrayList;
import java.util.List;

import de.keksuccino.core.input.MouseInput;
import net.minecraft.client.Minecraft;

public class PopupMenu implements IMenu {
	
	private int width;
	private int x = 0;
	private int y = 0;
	private List<AdvancedButton> content = new ArrayList<AdvancedButton>();
	private boolean opened = false;
	private boolean hovered = false;
	private boolean autoclose = false;
	private int space;
	
	public PopupMenu(int width, int space) {
		this.width = width;
		this.space = space;
	}
	
	public void render(int mouseX, int mouseY) {
		this.updateHovered(mouseX, mouseY);
		
		float ticks = Minecraft.getInstance().getRenderPartialTicks();
		
		int stackedHeight = 0;
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
		for (AdvancedButton b : this.content) {
			if ((mouseX >= b.x) && (mouseX <= b.x + b.width) && (mouseY >= b.y) && mouseY <= b.y + b.height) {
				this.hovered = true;
				return;
			}
		}
		this.hovered = false;
	}
	
	public boolean isLeftClicked() {
		for (AdvancedButton b : this.content) {
			if (b.isMouseOver() && MouseInput.isLeftMouseDown()) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isHovered() {
		if (!this.isOpen()) {
			return false;
		}
		return this.hovered;
	}
	
	public boolean isOpen() {
		return this.opened;
	}
	
	public void setAutoclose(boolean b) {
		this.autoclose = b;
	}
	
	public void setUseable(boolean b) {
		for (AdvancedButton bt : this.content) {
			bt.setUseable(b);
		}
		if (!b) {
			this.opened = false;
		}
	}
	
	public boolean isUseable() {
		if ((this.content == null) || this.content.isEmpty()) {
			return false;
		}
		return this.content.get(0).isUseable();
	}
	
	public void openMenuAt(int x, int y) {
		this.x = x;
		this.y = y;
		this.opened = true;
	}
	
	public void closeMenu() {
		this.opened = false;
	}
	
	public void addContent(AdvancedButton button) {
		this.content.add(button);
	}
	
	public void setWidth(int width) {
		this.width = width;
	}

}

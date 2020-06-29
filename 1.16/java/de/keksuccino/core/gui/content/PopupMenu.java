package de.keksuccino.core.gui.content;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;

import de.keksuccino.core.input.MouseInput;
import net.minecraft.client.Minecraft;

public class PopupMenu implements IMenu {
	
	private int width;
	private int buttonHeight;
	private int x = 0;
	private int y = 0;
	private List<AdvancedButton> content = new ArrayList<AdvancedButton>();
	private List<PopupMenu> children = new ArrayList<PopupMenu>();
	private PopupMenu parent;
	private boolean opened = false;
	private boolean hovered = false;
	private boolean autoclose = false;
	private int space;
	
	private boolean up = false;
	private boolean left = false;
	private int lastHeight = 0;
	
	public PopupMenu(int width, int buttonHeight, int space) {
		this.width = width;
		this.buttonHeight = buttonHeight;
		this.space = space;
	}
	
	public void render(MatrixStack matrix, int mouseX, int mouseY) {
		this.updateHovered(mouseX, mouseY);
		
		float ticks = Minecraft.getInstance().getRenderPartialTicks();
		
		int stackedHeight = 0;
		if (this.opened) {
			for (AdvancedButton b : this.content) {
				b.setHandleClick(true);
				b.setWidth(this.width);
				b.setHeight(this.buttonHeight);
				
				//TODO Logik an subchilds anpassen (falls irgendwann nötig)
				if (this.parent != null) {
					this.buttonHeight = parent.buttonHeight;
					
					if (parent.left) {
						this.left = true;
						this.x = parent.x - parent.width - this.width - 2;
					} else {
						this.x = parent.x + parent.width + 2;
					}
					if ((this.x + this.width) > Minecraft.getInstance().currentScreen.field_230708_k_) {
						this.x = parent.x - this.width - 2;
						this.left = true;
					} else if (!parent.left) {
						this.left = false;
					}
					b.setX(this.x);
					
					if (this.up) {
						b.setY(this.y + stackedHeight - this.lastHeight + this.buttonHeight + this.space);
					} else {
						b.setY(this.y + stackedHeight);
					}
				} else {
					if (this.left) {
						b.setX(this.x - this.width);
					} else {
						b.setX(this.x);
					}
					
					if (this.up) {
						b.setY(this.y + stackedHeight - this.lastHeight);
					} else {
						b.setY(this.y + stackedHeight);
					}
				}

				b.render(matrix, mouseX, mouseY, ticks);
				
				stackedHeight += b.getHeight() + this.space;
			}
			
			for (PopupMenu m : this.children) {
				m.render(matrix, mouseX, mouseY);
			}
		}
		
		if (this.autoclose && !this.isHovered() && (MouseInput.isLeftMouseDown() || MouseInput.isRightMouseDown())) {
			this.opened = false;
		}
	}
	
	private void updateHovered(int mouseX, int mouseY) {
		for (AdvancedButton b : this.content) {
			if ((mouseX >= b.getX()) && (mouseX <= b.getX() + b.getWidth()) && (mouseY >= b.getY()) && mouseY <= b.getY() + b.getHeight()) {
				this.hovered = true;
				return;
			}
		}
		this.hovered = false;
	}
	
	public boolean isLeftClicked() {
		for (AdvancedButton b : this.content) {
			if (b.isHovered() && MouseInput.isLeftMouseDown()) {
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

		this.lastHeight = 0;
		for (AdvancedButton b : this.content) {
			this.lastHeight += b.getHeight() + this.space;
		}
		
		//field_230709_l_ = screen height
		//field_230708_k_ = screen width
		
		if ((this.y + this.lastHeight) > Minecraft.getInstance().currentScreen.field_230709_l_) {
			this.up = true;
		} else {
			this.up = false;
		}
		if ((this.x + this.width) > Minecraft.getInstance().currentScreen.field_230708_k_) {
			this.left = true;
		} else {
			this.left = false;
		}
		
		this.opened = true;
	}
	
	public void closeMenu() {
		this.opened = false;
		for (PopupMenu m : this.children) {
			m.closeMenu();
		}
	}
	
	public void addContent(AdvancedButton button) {
		this.content.add(button);
	}
	
	public void setWidth(int width) {
		this.width = width;
	}
	
	public int getWidth() {
		return this.width;
	}
	
	public int getLastHeight() {
		return this.lastHeight;
	}
	
	public boolean isRenderedLeft() {
		return this.left;
	}
	
	public boolean isRenderedUp() {
		return this.up;
	}
	
	public void addChild(PopupMenu menu) {
		if (!this.children.contains(menu)) {
			this.children.add(menu);
			menu.parent = this;
		}
	}
	
	public void removeChild(PopupMenu menu) {
		if (this.children.contains(menu)) {
			this.children.remove(menu);
			menu.parent = null;
		}
	}

}

package de.keksuccino.core.gui.content;

import java.util.ArrayList;
import java.util.List;

import de.keksuccino.core.input.MouseInput;
import net.minecraft.client.Minecraft;

public class PopupMenu implements IMenu {
	
	private int width;
	private int buttonHeight;
	private int x = 0;
	private int y = 0;
	protected List<AdvancedButton> content = new ArrayList<AdvancedButton>();
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
	
	public void render(int mouseX, int mouseY) {
		this.updateHovered(mouseX, mouseY);
		
		float ticks = Minecraft.getMinecraft().getRenderPartialTicks();
		
		int stackedHeight = 0;
		if (this.opened) {
			for (AdvancedButton b : this.content) {
				b.setHandleClick(true);
				b.width = this.width;
				b.height = this.buttonHeight;
				
				//TODO Logik an subchilds anpassen (falls irgendwann nötig)
				if (this.parent != null) {
					this.buttonHeight = parent.buttonHeight;
					
					if (parent.left) {
						this.left = true;
						this.x = parent.x - parent.width - this.width - 2;
					} else {
						this.x = parent.x + parent.width + 2;
					}
					if ((this.x + this.width) > Minecraft.getMinecraft().currentScreen.width) {
						this.x = parent.x - this.width - 2;
						this.left = true;
					} else if (!parent.left) {
						this.left = false;
					}
					b.x = this.x;
					
					if (this.up) {
						b.y = this.y + stackedHeight - this.lastHeight + this.buttonHeight + this.space;
					} else {
						b.y = this.y + stackedHeight;
					}
				} else {
					if (this.left) {
						b.x = this.x - this.width;
					} else {
						b.x = this.x;
					}
					
					if (this.up) {
						b.y = this.y + stackedHeight - this.lastHeight;
					} else {
						b.y = this.y + stackedHeight;
					}
				}

				b.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, ticks);
				
				stackedHeight += b.height + this.space;
			}
			
			for (PopupMenu m : this.children) {
				m.render(mouseX, mouseY);
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
		
		for (PopupMenu m : this.children) {
			m.closeMenu();
		}
		
		this.x = x;
		this.y = y;

		this.lastHeight = 0;
		for (AdvancedButton b : this.content) {
			this.lastHeight += b.height + this.space;
		}
		if ((this.y + this.lastHeight) > Minecraft.getMinecraft().currentScreen.height) {
			this.up = true;
		} else {
			this.up = false;
		}
		if ((this.x + this.width) > Minecraft.getMinecraft().currentScreen.width) {
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
		button.ignoreBlockedInput = true;
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

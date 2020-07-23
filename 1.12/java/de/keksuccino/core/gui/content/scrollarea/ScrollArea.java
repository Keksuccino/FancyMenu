package de.keksuccino.core.gui.content.scrollarea;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.input.MouseInput.MouseData;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

public class ScrollArea extends Gui {
	
	public Color backgroundColor = new Color(0, 0, 0, 240);
	public int x;
	public int y;
	public int width;
	public int height;
	public int grabberheight = 20;
	public int grabberwidth = 10;
	private List<ScrollAreaEntry> entries = new ArrayList<ScrollAreaEntry>();
	
	private boolean grabberHovered = false;
	private boolean grabberPressed = false;
	
	private int scrollpos = 0;
	private int entryheight = 0;
	
	private int startY = 0;
	private int startPos = 0;
	
	public ScrollArea(int x, int y, int width, int height) {
		this.width = width;
		this.height = height;
		this.x = x;
		this.y = y;
		
		MouseInput.registerMouseListener(this::onMouseScroll);
	}
	
	public void render() {
		
		GlStateManager.enableBlend();

		this.renderBackground();
		
		this.renderScrollbar();
		
		int i = 0;
		for (ScrollAreaEntry e : this.entries) {
			int scroll = this.scrollpos * (this.entryheight / (this.height - this.grabberheight));
			e.x = this.x;
			e.y = this.y + i - scroll;
			e.render();
			
			i += e.getHeight();
		}
		
	}
	
	protected void renderScrollbar() {
		if (this.height < this.entryheight) {
			int mouseX = MouseInput.getMouseX();
			int mouseY = MouseInput.getMouseY();

			//update grabber hover state
			if (((this.x + this.width) <= mouseX) && ((this.x + this.width + grabberwidth) >= mouseX) && ((this.y + this.scrollpos) <= mouseY) && ((this.y + this.scrollpos + grabberheight) >= mouseY)) {
				this.grabberHovered = true;
			} else {
				this.grabberHovered = false;
			}
			
			//Update grabber pressed state
			if (this.isGrabberHovered() && MouseInput.isLeftMouseDown()) {
				this.grabberPressed = true;
			}
			if (!MouseInput.isLeftMouseDown()) {
				this.grabberPressed = false;
			}
					
			//Render scroll grabber
			if (!this.isGrabberHovered()) {
				drawRect(this.x + this.width, this.y + this.scrollpos, this.x + this.width + grabberwidth, this.y + this.scrollpos + grabberheight, Color.GRAY.getRGB());
			} else {
				drawRect(this.x + this.width, this.y + this.scrollpos, this.x + this.width + grabberwidth, this.y + this.scrollpos + grabberheight, Color.LIGHT_GRAY.getRGB());
			}
			
			//Handle scroll
			if (this.isGrabberPressed()) {
				this.handleGrabberScrolling();
			} else {
				this.startY = MouseInput.getMouseY();
				this.startPos = this.scrollpos;
			}
		}
	}
	
	public boolean isAreaHovered() {
		int mouseX = MouseInput.getMouseX();
		int mouseY = MouseInput.getMouseY();
		if ((this.x <= mouseX) && ((this.x + this.width + this.grabberwidth) >= mouseX) && (this.y <= mouseY) && ((this.y + this.height) >= mouseY)) {
			return true;
		}
		return false;
	}
	
	protected void handleGrabberScrolling() {
		int i = this.startY - MouseInput.getMouseY();
		int scroll = this.startPos - i;
		
		if (scroll < 0) {
			this.scrollpos = 0;
		} else if (scroll > this.height - this.grabberheight) {
			this.scrollpos = this.height - this.grabberheight;
		} else {
			this.scrollpos = scroll;
		}
	}
	
	protected void renderBackground() {
		GlStateManager.pushMatrix();
		GlStateManager.disableAlpha();
		drawRect(this.x, this.y, this.x + this.width, this.y + this.height, this.backgroundColor.getRGB());
		GlStateManager.popMatrix();
	}
	
	public void addEntry(ScrollAreaEntry e) {
		this.entries.add(e);
		this.scrollpos = 0;
		this.entryheight += e.getHeight();
	}
	
	public void removeEntry(ScrollAreaEntry e) {
		if (this.entries.contains(e)) {
			this.entries.remove(e);
			this.scrollpos = 0;
			this.entryheight -= e.getHeight();
		}
	}
	
	public List<ScrollAreaEntry> getEntries() {
		return this.entries;
	}
	
	public int getStackedEntryHeight() {
		return this.entryheight;
	}
	
	public boolean isGrabberHovered() {
		return this.grabberHovered;
	}
	
	public boolean isGrabberPressed() {
		return this.grabberPressed;
	}

	public void onMouseScroll(MouseData d) {
		if (this.isAreaHovered()) {
			int i = d.deltaZ / 120;
			int scroll = this.scrollpos - i * 7;
			if (scroll < 0) {
				this.scrollpos = 0;
			} else if (scroll > this.height - this.grabberheight) {
				this.scrollpos = this.height - this.grabberheight;
			} else {
				this.scrollpos = scroll;
			}
		}
	}

}

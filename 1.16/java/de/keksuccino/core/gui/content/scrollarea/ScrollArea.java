package de.keksuccino.core.gui.content.scrollarea;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.core.input.MouseInput;
import net.minecraft.client.gui.AbstractGui;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ScrollArea extends AbstractGui {
	
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
		
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	public void render(MatrixStack matrix) {
		
		RenderSystem.enableBlend();

		this.renderBackground(matrix);
		
		this.renderScrollbar(matrix);
		
		int i = 0;
		for (ScrollAreaEntry e : this.entries) {
			int scroll = this.scrollpos * (this.entryheight / (this.height - this.grabberheight));
			e.x = this.x;
			e.y = this.y + i - scroll;
			e.render(matrix);
			
			i += e.getHeight();
		}
		
	}
	
	protected void renderScrollbar(MatrixStack matrix) {
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
				fill(matrix, this.x + this.width, this.y + this.scrollpos, this.x + this.width + grabberwidth, this.y + this.scrollpos + grabberheight, Color.GRAY.getRGB());
			} else {
				fill(matrix, this.x + this.width, this.y + this.scrollpos, this.x + this.width + grabberwidth, this.y + this.scrollpos + grabberheight, Color.LIGHT_GRAY.getRGB());
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
	
	protected void renderBackground(MatrixStack matrix) {
		matrix.push();
		RenderSystem.disableAlphaTest();
		fill(matrix, this.x, this.y, this.x + this.width, this.y + this.height, this.backgroundColor.getRGB());
		matrix.pop();
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
	
	@SubscribeEvent
	public void onMouseScrollPre(GuiScreenEvent.MouseScrollEvent.Pre e) {
		if (this.isAreaHovered()) {
			int scroll = this.scrollpos - (int) e.getScrollDelta() * 7;
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

package de.keksuccino.fancymenu.menu.fancy.helper.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.ContextMenu;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;

public class FMContextMenu extends ContextMenu {

	protected boolean resetParentButtonOnClose = false;
	protected int cachedScreenWidth = 0;
	protected int cachedScreenHeight = 0;
	
	protected List<Integer> separators = new ArrayList<Integer>();
	
	public FMContextMenu() {
		super(20, 20, 0);
	}
	
	@Override
	public void addContent(AdvancedButton button) {
		super.addContent(button);
		
		Color c = new Color(0, 0, 0, 0);
		button.setBackgroundColor(UIBase.getButtonIdleColor(), UIBase.getButtonHoverColor(), c, c, 0);
	}
	
	public void addSeparator() {
		int i = this.content.size();
		if (!this.separators.contains(i)) {
			this.separators.add(i);
		}
	}
	
	@Override
	public void openMenuAt(int x, int y, int screenWidth, int screenHeight) {
		
		for (AdvancedButton b : this.content) {
			b.setLabelShadow(false);
		}
		
		this.cachedScreenWidth = screenWidth;
		this.cachedScreenHeight = screenHeight;
		
		if (this.parent != null) {
			if (this.parent instanceof FMContextMenu) {
				screenWidth = ((FMContextMenu) this.parent).cachedScreenWidth;
				screenHeight = ((FMContextMenu) this.parent).cachedScreenHeight;
			}
		}
		
		int i = 20;
		for (AdvancedButton b : this.content) {
			int sw = Minecraft.getMinecraft().fontRenderer.getStringWidth(b.displayString) + 12;
			if (b.width > sw) {
				sw = b.width;
			}
			if (sw > i) {
				i = sw;
			}
		}
		this.width = i;
		
		super.openMenuAt(x, y, screenWidth, screenHeight);
		
	}
	
	@Override
	public void render(int mouseX, int mouseY, int screenWidth, int screenHeight) {
		
		if (this.parent != null) {
			if (this.parent instanceof FMContextMenu) {
				screenWidth = ((FMContextMenu) this.parent).cachedScreenWidth;
				screenHeight = ((FMContextMenu) this.parent).cachedScreenHeight;
			}
		}
		
		super.render(mouseX, mouseY, screenWidth, screenHeight);
		
		if (this.opened) {
			
			if (this.alwaysOnTop) {
				RenderUtils.setZLevelPre(400);
				this.renderBorder();
				this.renderSeparators();
				RenderUtils.setZLevelPost();
			} else {
				this.renderBorder();
				this.renderSeparators();
			}
			
			
		} else {
			
			if (this.resetParentButtonOnClose) {
				this.parentButton = null;
			}
			
		}
	}
	
	protected void renderSeparators() {
		if (!this.content.isEmpty()) {
			for (Integer i : this.separators) {
				if (this.content.size() >= i+1) {
					AdvancedButton b = this.content.get(i);
					Color c = new Color(UIBase.getButtonBorderIdleColor().getRed(), UIBase.getButtonBorderIdleColor().getGreen(), UIBase.getButtonBorderIdleColor().getBlue(), 100);
					UIBase.drawRect(b.x, b.y, b.x + this.width, b.y + 1, c.getRGB());
				}
			}
		}
	}
	
	protected void renderBorder() {
		if (!this.content.isEmpty()) {
			AdvancedButton b = this.content.get(0);
			//TOP
			UIBase.drawRect(b.x, b.y, b.x + this.width, b.y + 1, UIBase.getButtonBorderIdleColor().getRGB());
			//LEFT
			UIBase.drawRect( b.x, b.y + 1, b.x + 1, b.y + this.lastHeight, UIBase.getButtonBorderIdleColor().getRGB());
			//BOTTOM
			UIBase.drawRect(b.x + 1, b.y + this.lastHeight - 1, b.x + this.width, b.y + this.lastHeight, UIBase.getButtonBorderIdleColor().getRGB());
			//RIGHT
			UIBase.drawRect(b.x + this.width - 1, b.y + 1, b.x + this.width, b.y + this.lastHeight - 1, UIBase.getButtonBorderIdleColor().getRGB());
		}
	}
	
	public List<AdvancedButton> getContent() {
		return this.content;
	}
	
	public void resetParentButtonOnClose(boolean reset) {
		this.resetParentButtonOnClose = reset;
	}
	
	public int getX() {
		return this.x;
	}
	
	public int getY() {
		return this.y;
	}
	
	@Override
	public boolean isHovered() {
		for (ContextMenu c : this.children) {
			if (c.isHovered()) {
				return true;
			}
		}
		return super.isHovered();
	}
	
}

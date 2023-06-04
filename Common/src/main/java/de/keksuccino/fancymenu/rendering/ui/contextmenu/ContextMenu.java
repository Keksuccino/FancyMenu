package de.keksuccino.fancymenu.rendering.ui.contextmenu;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;

public class ContextMenu extends de.keksuccino.konkrete.gui.content.ContextMenu {

	protected boolean resetParentButtonOnClose = false;
	protected int cachedScreenWidth = 0;
	protected int cachedScreenHeight = 0;
	
	protected List<Integer> separators = new ArrayList<>();
	
	public ContextMenu() {
		super(20, 20, 0);
	}
	
	@Override
	public void addContent(AdvancedButton button) {
		super.addContent(button);
		Color c = new Color(0, 0, 0, 0);
		button.setBackgroundColor(UIBase.getUIColorScheme().elementBackgroundColorNormal.getColor(), UIBase.getUIColorScheme().elementBackgroundColorHover.getColor(), c, c, 0);
		button.ignoreBlockedInput = true;
		button.ignoreLeftMouseDownClickBlock = true;
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
			if (this.parent instanceof ContextMenu) {
				screenWidth = ((ContextMenu) this.parent).cachedScreenWidth;
				screenHeight = ((ContextMenu) this.parent).cachedScreenHeight;
			}
		}
		
		int i = 20;
		for (AdvancedButton b : this.content) {
			int sw = Minecraft.getInstance().font.width(b.getMessageString()) + 12;
			if (b.getWidth() > sw) {
				sw = b.getWidth();
			}
			if (sw > i) {
				i = sw;
			}
		}
		this.width = i;
		
		super.openMenuAt(x, y, screenWidth, screenHeight);
		
	}
	
	@Override
	public void render(PoseStack matrix, int mouseX, int mouseY, int screenWidth, int screenHeight) {
		
		if (this.parent != null) {
			if (this.parent instanceof ContextMenu) {
				screenWidth = ((ContextMenu) this.parent).cachedScreenWidth;
				screenHeight = ((ContextMenu) this.parent).cachedScreenHeight;
			}
		}
		
		super.render(matrix, mouseX, mouseY, screenWidth, screenHeight);
		
		if (this.opened) {
			
			if (this.alwaysOnTop) {
				RenderUtils.setZLevelPre(matrix, 400);
				this.renderBorder(matrix);
				this.renderSeparators(matrix);
				RenderUtils.setZLevelPost(matrix);
			} else {
				this.renderBorder(matrix);
				this.renderSeparators(matrix);
			}
			
			
		} else {
			
			if (this.resetParentButtonOnClose) {
				this.parentButton = null;
			}
			
		}
	}
	
	protected void renderSeparators(PoseStack matrix) {
		if (!this.content.isEmpty()) {
			for (Integer i : this.separators) {
				if (this.content.size() >= i+1) {
					AdvancedButton b = this.content.get(i);
					UIBase.fill(matrix, b.x, b.y, b.x + this.width, b.y + 1, UIBase.getUIColorScheme().elementBackgroundColorNormal.getColorIntWithAlpha(100));
				}
			}
		}
	}
	
	protected void renderBorder(PoseStack matrix) {
		if (!this.content.isEmpty()) {
			AdvancedButton b = this.content.get(0);
			int c = UIBase.getUIColorScheme().elementBorderColorNormal.getColorInt();
			//TOP
			UIBase.fill(matrix, b.x, b.y, b.x + this.width, b.y + 1, c);
			//LEFT
			UIBase.fill(matrix, b.x, b.y + 1, b.x + 1, b.y + this.lastHeight, c);
			//BOTTOM
			UIBase.fill(matrix, b.x + 1, b.y + this.lastHeight - 1, b.x + this.width, b.y + this.lastHeight, c);
			//RIGHT
			UIBase.fill(matrix, b.x + this.width - 1, b.y + 1, b.x + this.width, b.y + this.lastHeight - 1, c);
		}
	}
	
	public List<AdvancedButton> getContent() {
		return this.content;
	}

	public List<de.keksuccino.konkrete.gui.content.ContextMenu> getChildren() {
		return this.children;
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
		for (de.keksuccino.konkrete.gui.content.ContextMenu c : this.children) {
			if (c.isHovered()) {
				return true;
			}
		}
		return super.isHovered();
	}
	
}

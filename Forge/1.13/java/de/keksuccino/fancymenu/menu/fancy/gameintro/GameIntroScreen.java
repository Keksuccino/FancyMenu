package de.keksuccino.fancymenu.menu.fancy.gameintro;

import de.keksuccino.core.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;

public class GameIntroScreen extends GuiScreen {
	
	private IAnimationRenderer renderer;
	private GuiMainMenu main;
	private boolean pre;
	private boolean loop;
	
	public GameIntroScreen(IAnimationRenderer intro, GuiMainMenu main) {
		this.renderer = intro;
		this.main = main;
	}
	
	@Override
	protected void initGui() {
		super.initGui();
		
		this.pre = false;
		this.loop = false;
	}
	
	@Override
	public void render(int mouseX, int mouseY, float partialTicks) {
		super.render(mouseX, mouseY, partialTicks);
		
		if (this.renderer != null) {
			if (!this.renderer.isReady()) {
				this.renderer.prepareAnimation();
			}
			if (!pre) {
				this.renderer.resetAnimation();
				this.loop = this.renderer.isGettingLooped();
				this.renderer.setLooped(false);
				this.pre = true;
			}
			
			if (!this.renderer.isFinished()) {
				boolean stretched = this.renderer.isStretchedToStreensize();
				this.renderer.setStretchImageToScreensize(true);
				this.renderer.render();
				this.renderer.setStretchImageToScreensize(stretched);
			} else {
				this.renderer.setLooped(this.loop);
				this.renderer.resetAnimation();
				Minecraft.getInstance().displayGuiScreen(this.main);
			}
		}
	}

}

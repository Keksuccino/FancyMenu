package de.keksuccino.fancymenu.menu.fancy.gameintro;

import de.keksuccino.core.rendering.animation.IAnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

public class GameIntroScreen extends Screen {

	private IAnimationRenderer renderer;
	private MainMenuScreen main;
	private boolean pre;
	private boolean loop;

	public GameIntroScreen(IAnimationRenderer intro, MainMenuScreen main) {
		super(new StringTextComponent(""));
		this.renderer = intro;
		this.main = main;
	}
	
	@Override
	protected void init() {
		super.init();
		
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

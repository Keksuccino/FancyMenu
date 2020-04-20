package de.keksuccino.core.rendering.animation;

import de.keksuccino.core.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderSkybox;
import net.minecraft.client.renderer.RenderSkyboxCube;

public class AnimationRendererSkybox extends RenderSkybox {
	
	private IAnimationRenderer animation;
	
	public AnimationRendererSkybox(IAnimationRenderer renderer) {
		super(new AnimationSkyboxCube());
		this.animation = renderer;
	}
	
	@Override
	public void render(float deltaT, float alpha) {
		if (this.animation != null) {
			System.out.println("render");
			boolean b = this.animation.isStretchedToStreensize();
			this.animation.setStretchImageToScreensize(true);
			this.animation.render();
			this.animation.setStretchImageToScreensize(b);
		}
	}
	
	private static class AnimationSkyboxCube extends RenderSkyboxCube {
		public AnimationSkyboxCube() {
			super(RenderUtils.getBlankImageResource());
		}
		
		@Override
		public void render(Minecraft mc, float pitch, float yaw, float alpha) {
		}

	}

}

package de.keksuccino.rendering.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderSkybox;
import net.minecraft.client.renderer.RenderSkyboxCube;
import net.minecraft.util.ResourceLocation;

public class AnimationRendererSkybox extends RenderSkybox {
	
	private IAnimationRenderer renderer;
	
	public AnimationRendererSkybox(IAnimationRenderer renderer) {
		super(new AnimationSkyboxCube());
		this.renderer = renderer;
	}
	
	@Override
	public void render(float deltaT, float alpha) {
		if (this.renderer != null) {
			boolean b = this.renderer.isStretchedToStreensize();
			this.renderer.setStretchImageToScreensize(true);
			this.renderer.render();
			this.renderer.setStretchImageToScreensize(b);
		}
	}
	
	private static class AnimationSkyboxCube extends RenderSkyboxCube {
		public AnimationSkyboxCube() {
			super(new ResourceLocation(""));
		}
		
		@Override
		public void render(Minecraft mc, float pitch, float yaw, float alpha) {
		}
	}

}

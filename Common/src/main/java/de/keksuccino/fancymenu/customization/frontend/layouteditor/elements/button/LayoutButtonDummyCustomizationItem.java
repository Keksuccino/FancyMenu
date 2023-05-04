package de.keksuccino.fancymenu.customization.frontend.layouteditor.elements.button;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.backend.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.customization.backend.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.backend.element.AbstractElement;
import de.keksuccino.fancymenu.customization.backend.layer.ScreenCustomizationLayer;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.fancymenu.rendering.texture.ExternalTextureHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Dummy item class to use its orientation handling for LayoutButtons
 */
public class LayoutButtonDummyCustomizationItem  extends AbstractElement {

	protected ScreenCustomizationLayer.ButtonCustomizationContainer button;

	public LayoutButtonDummyCustomizationItem(ScreenCustomizationLayer.ButtonCustomizationContainer button, String label, int width, int height, int x, int y) {
		super(new PropertiesSection("customization"));
		this.value = label;
		this.elementType = "handlelayoutbutton";
		this.setWidth(width);
		this.setHeight(height);
		this.rawX = x;
		this.rawY = y;
		this.button = button;
	}

	@Override
	public void render(PoseStack matrix, Screen menu) throws IOException {
		RenderSystem.enableBlend();

		IAnimationRenderer animation = null;
		ResourceLocation texture = null;
		if (this.button.normalBackground != null) {
			if (this.button.normalBackground.startsWith("animation:")) {
				String aniName = this.button.normalBackground.split("[:]", 2)[1];
				if (AnimationHandler.animationExists(aniName)) {
					animation = AnimationHandler.getAnimation(aniName);
					if (!animation.isReady()) {
						animation.prepareAnimation();
					}
				}
			} else {
				File f = new File(this.button.normalBackground);
				if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
					f = new File(Minecraft.getInstance().gameDirectory, this.button.normalBackground);
				}
				if (f.isFile()) {
					if (f.getPath().toLowerCase().endsWith(".gif")) {
						animation = ExternalTextureHandler.INSTANCE.getGif(f.getPath());
					} else if (f.getPath().toLowerCase().endsWith(".jpg") || f.getPath().toLowerCase().endsWith(".jpeg") || f.getPath().toLowerCase().endsWith(".png")) {
						ExternalTextureResourceLocation exTex = ExternalTextureHandler.INSTANCE.getTexture(f.getPath());
						if (exTex != null) {
							if (!exTex.isReady()) {
								exTex.loadTexture();
							}
							texture = exTex.getResourceLocation();
						}
					}
				}
			}
		}

		if (texture != null) {
			RenderUtils.bindTexture(texture);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			Screen.blit(matrix, this.getX(menu), this.getY(menu), 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
		} else if (animation != null) {
			int aniX = animation.getPosX();
			int aniY = animation.getPosY();
			int aniWidth = animation.getWidth();
			int aniHeight = animation.getHeight();
			boolean aniLoop = animation.isGettingLooped();

			animation.setPosX(this.getX(menu));
			animation.setPosY(this.getY(menu));
			animation.setWidth(this.getWidth());
			animation.setHeight(this.getHeight());
			animation.setLooped(this.button.loopAnimation);
			if (animation instanceof AdvancedAnimation) {
				((AdvancedAnimation) animation).setMuteAudio(true);
			}

			animation.render(matrix);

			animation.setPosX(aniX);
			animation.setPosY(aniY);
			animation.setWidth(aniWidth);
			animation.setHeight(aniHeight);
			animation.setLooped(aniLoop);
			if (animation instanceof AdvancedAnimation) {
				((AdvancedAnimation) animation).setMuteAudio(false);
			}
		} else {
			fill(matrix, this.getX(menu), this.getY(menu), this.getX(menu) + this.getWidth(), this.getY(menu) + this.getHeight(), new Color(138, 138, 138, 255).getRGB());
		}
        drawCenteredString(matrix, Minecraft.getInstance().font, Component.literal(this.value), this.getX(menu) + this.getWidth() / 2, this.getY(menu) + (this.getHeight() - 8) / 2, new Color(255, 255, 255, 255).getRGB());
        RenderSystem.disableBlend();
	}

}

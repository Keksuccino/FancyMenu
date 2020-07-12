package de.keksuccino.core.gui.content;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class AdvancedImageButton extends AdvancedButton {

	private ResourceLocation image;
	
	public AdvancedImageButton(int x, int y, int widthIn, int heightIn, ResourceLocation image, boolean handleClick, IPressable onPress) {
		super(x, y, widthIn, heightIn, "", handleClick, onPress);
		this.image = image;
	}
	
	public AdvancedImageButton(int x, int y, int widthIn, int heightIn, ResourceLocation image, IPressable onPress) {
		super(x, y, widthIn, heightIn, "", onPress);
		this.image = image;
	}
	
	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
		super.render(matrix, mouseX, mouseY, partialTicks);
		
		Minecraft.getInstance().getTextureManager().bindTexture(this.image);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		blit(matrix, this.getX(), this.getY(), 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
	}

}

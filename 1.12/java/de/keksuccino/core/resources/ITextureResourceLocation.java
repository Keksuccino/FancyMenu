package de.keksuccino.core.resources;

import net.minecraft.util.ResourceLocation;

public interface ITextureResourceLocation {
	
	public void loadTexture();
	
	public ResourceLocation getResourceLocation();
	
	public boolean isReady();
	
	public int getHeight();
	
	public int getWidth();

}

package de.keksuccino.resources;

import java.lang.reflect.Field;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class SelfcleaningDynamicTexture extends DynamicTexture {

	public SelfcleaningDynamicTexture(NativeImage nativeImageIn) {
		super(nativeImageIn);
	}
	
	@Override
	public void updateDynamicTexture() {
		super.updateDynamicTexture();
		
		//Clearing all NativeImage data to free memory
		this.getTextureData().close();
		clearTextureData(this);
	}
	
	private static void clearTextureData(DynamicTexture texture) {
		try {
			Field f = ObfuscationReflectionHelper.findField(DynamicTexture.class, "field_110566_b");
			f.set(texture, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

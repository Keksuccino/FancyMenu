package de.keksuccino.resources;

import java.lang.reflect.Field;

import de.keksuccino.reflection.ReflectionHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;

public class SelfcleaningDynamicTexture extends DynamicTexture {

	public SelfcleaningDynamicTexture(NativeImage nativeImageIn) {
		super(nativeImageIn);
	}
	
	@Override
	public void updateDynamicTexture() {
		super.updateDynamicTexture();
		
		//Clearing all NativeImage data to free memory
		clearTextureData(this);
	}
	
	/**
	 * Dummy method to avoid NullPointer's.
	 */
	@Override
	public NativeImage getTextureData() {
		return new NativeImage(0, 0, true);
	}
	
	private static void clearTextureData(DynamicTexture texture) {
		try {
			Field f = ReflectionHelper.findField(DynamicTexture.class, "field_110566_b");
			((NativeImage)f.get(texture)).close();
			f.set(texture, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

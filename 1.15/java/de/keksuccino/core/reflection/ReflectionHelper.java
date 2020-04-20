package de.keksuccino.core.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class ReflectionHelper {
	
	/**
	 * Searches for a field and makes it accessible.
	 * 
	 * @return The field or null if the field couldn't be found.
	 */
	public static Field getDeclaredField(Class<?> c, String fieldname) {
		try {
			Field f = c.getDeclaredField(fieldname);
			f.setAccessible(true);
			return f;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @return True if the field value was set correctly.
	 */
	public static boolean setField(Field f, Object instance, Object value) {
		try {
			f.set(instance, value);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * @return The old value.
	 */
	public static Object setStaticFinalField(Field f, Class<?> c, Object value) {
		Object o = null;
		try {
			f.setAccessible(true);
			Field modifiers = Field.class.getDeclaredField("modifiers");
			modifiers.setAccessible(true);
			modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
			o = f.get(c);
			f.set(null, value);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return o;
	}

	/**
	 * Searches for an obfuscated MC field.
	 * @param srgName The srg name of the field.
	 */
    public static <T> Field findField(Class<? super T> c, String srgName) {
        return ObfuscationReflectionHelper.findField(c, srgName);
    }
    
    public static <T> Method findMethod(Class<? super T> c, String srgName, Class<?>... parameterTypes) {
    	return ObfuscationReflectionHelper.findMethod(c, srgName, parameterTypes);
    }

}

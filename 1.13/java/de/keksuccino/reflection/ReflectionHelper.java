package de.keksuccino.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraftforge.fml.loading.FMLLoader;

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
	 * Method back-ported from Forge for MC 1.15.2
	 * Copyright (c) 2019 Minecraft Forge.
	 */
	@Nonnull
    public static <T> Field findField(@Nonnull final Class<? super T> clazz, @Nonnull final String fieldName) {
        Preconditions.checkNotNull(clazz, "Class to find field on cannot be null.");
        Preconditions.checkNotNull(fieldName, "Name of field to find cannot be null.");
        Preconditions.checkArgument(!fieldName.isEmpty(), "Name of field to find cannot be empty.");

        try {
        	Field f = clazz.getDeclaredField(remapName(INameMappingService.Domain.FIELD, fieldName));
            f.setAccessible(true);
            return f;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
	
	/**
	 * Method back-ported from Forge for MC 1.15.2
	 * Copyright (c) 2019 Minecraft Forge.
	 */
	@Nonnull
    public static String remapName(INameMappingService.Domain domain, String name) {
        return FMLLoader.getNameFunction("srg").map(f->f.apply(domain, name)).orElse(name);
    }

}

package de.keksuccino.fancymenu.customization.screeninstancefactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScreenInstanceFactory {
	
	private static final Map<Class<?>, Object> DEFAULT_PARAMETERS = new HashMap<>();
	private static final Map<String, Supplier<? extends Screen>> SCREEN_INSTANCE_PROVIDERS = new HashMap<>();
	
	public static void init() {
		
		DEFAULT_PARAMETERS.put(Minecraft.class, Minecraft.getInstance());
		DEFAULT_PARAMETERS.put(Screen.class, null);
		DEFAULT_PARAMETERS.put(Options.class, Minecraft.getInstance().options);
		DEFAULT_PARAMETERS.put(LanguageManager.class, Minecraft.getInstance().getLanguageManager());
		DEFAULT_PARAMETERS.put(Player.class, null);
		DEFAULT_PARAMETERS.put(String.class, "");
		DEFAULT_PARAMETERS.put(ClientAdvancements.class, null);
		DEFAULT_PARAMETERS.put(Component.class, Component.empty());
		DEFAULT_PARAMETERS.put(boolean.class, true);
		DEFAULT_PARAMETERS.put(int.class, 0);
		DEFAULT_PARAMETERS.put(long.class, 0L);
		DEFAULT_PARAMETERS.put(double.class, 0D);
		DEFAULT_PARAMETERS.put(float.class, 0F);
		DEFAULT_PARAMETERS.put(Boolean.class, true);
		DEFAULT_PARAMETERS.put(Integer.class, 0);
		DEFAULT_PARAMETERS.put(Long.class, 0L);
		DEFAULT_PARAMETERS.put(Double.class, 0D);
		DEFAULT_PARAMETERS.put(Float.class, 0F);

		registerScreenProvider(PackSelectionScreen.class.getName(), () -> {
			return new PackSelectionScreen(Minecraft.getInstance().getResourcePackRepository(), (repo) -> {
				Minecraft.getInstance().options.updateResourcePacks(repo);
				Minecraft.getInstance().setScreen(Minecraft.getInstance().screen);
			}, Minecraft.getInstance().getResourcePackDirectory(), Component.translatable("resourcePack.title"));
		});
		
	}

	public static void registerScreenProvider(@NotNull String fullScreenClassName, @NotNull Supplier<? extends Screen> provider) {
		SCREEN_INSTANCE_PROVIDERS.put(fullScreenClassName, provider);
	}

	@Nullable
	public static Supplier<? extends Screen> getScreenProvider(@NotNull String fullScreenClassName) {
		return SCREEN_INSTANCE_PROVIDERS.get(fullScreenClassName);
	}

	@Nullable
	public static Screen tryConstruct(@NotNull String fullScreenClassName) {
		try {
			if (ScreenCustomization.isScreenBlacklisted(fullScreenClassName)) {
				return null;
			}
			//Update last screen
			DEFAULT_PARAMETERS.put(Screen.class, Minecraft.getInstance().screen);
			//Update player
			DEFAULT_PARAMETERS.put(Player.class, Minecraft.getInstance().player);
			if (Minecraft.getInstance().player != null) {
				DEFAULT_PARAMETERS.put(ClientAdvancements.class, Minecraft.getInstance().player.connection.getAdvancements());
			}
			//Check if a provider is registered for the screen and return from provider if one was found
			Supplier<? extends Screen> screenProvider = getScreenProvider(fullScreenClassName);
			if (screenProvider != null) return screenProvider.get();
			//Try to construct and instance of the screen
			Class<?> screenClass = Class.forName(fullScreenClassName, false, ScreenInstanceFactory.class.getClassLoader());
			if (Screen.class.isAssignableFrom(screenClass)) {
				Constructor<?>[] constructors = screenClass.getConstructors();
				if (constructors.length > 0) {
					Constructor<?> constructor = null;
					//Try to find constructor without parameters
					for (Constructor<?> c : constructors) {
						if (c.getParameterTypes().length == 0) {
							constructor = c;
							break;
						}
					}
					if (constructor == null) {
						//Try to find constructor with supported parameters
						for (Constructor<?> c : constructors) {
							if (allParametersSupported(c.getParameterTypes())) {
								constructor = c;
								break;
							}
						}
					}
					if (constructor != null) {
						Class<?>[] parameters = constructor.getParameterTypes();
						List<Object> parameterInstances = new ArrayList<>();
						for (Class<?> p : parameters) {
							parameterInstances.add(DEFAULT_PARAMETERS.get(p));
						}
						return createInstance(constructor, parameterInstances);
					}
					return null;
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	private static boolean allParametersSupported(Class<?>[] parameters) {
		for (Class<?> par : parameters) {
			if (!DEFAULT_PARAMETERS.containsKey(par)) {
				return false;
			}
		}
		return true;
	}

	@Nullable
	private static Screen createInstance(@NotNull Constructor<?> constructor, @Nullable List<Object> parameters) {
		try {
			if ((parameters == null) || parameters.isEmpty()) {
				return (Screen) constructor.newInstance();
			} else {
				return (Screen) constructor.newInstance(parameters.toArray(new Object[0]));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}

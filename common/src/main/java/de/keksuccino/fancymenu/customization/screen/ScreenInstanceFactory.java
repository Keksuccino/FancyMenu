package de.keksuccino.fancymenu.customization.screen;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.customization.screen.identifier.UniversalScreenIdentifierRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScreenInstanceFactory {
	
	private static final Map<Class<?>, Object> DEFAULT_PARAMETERS = new HashMap<>();
	private static final Map<String, Function<ScreenConstructionContext, ? extends Screen>> SCREEN_INSTANCE_PROVIDERS = new HashMap<>();
	private static final Map<String, Supplier<? extends Screen>> LEGACY_SCREEN_INSTANCE_PROVIDERS = new HashMap<>();

	static {

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

		ScreenInstanceFactory.registerScreenProvider(PackSelectionScreen.class.getName(),
				() -> new PackSelectionScreen(Minecraft.getInstance().screen, Minecraft.getInstance().getResourcePackRepository(), (repo) -> {
					updateResourcePacks(Minecraft.getInstance().options, repo);
					Minecraft.getInstance().setScreen(Minecraft.getInstance().screen);
				}, Minecraft.getInstance().getResourcePackDirectory(), Component.translatable("resourcePack.title"))
		);

		ScreenInstanceFactory.registerScreenProvider(CreateWorldScreen.class.getName(), () ->
				new ExecuteOnRenderScreen(() -> CreateWorldScreen.openFresh(Minecraft.getInstance(), new TitleScreen()), true));

		ScreenInstanceFactory.registerContextAwareScreenProvider(AccessibilityOptionsScreen.class.getName(), context -> new AccessibilityOptionsScreen(context.parentScreen(), Minecraft.getInstance().options));
		// 1.19.2 determines the world-specific Options screen contents during init(), so its constructor only needs the explicit parent.
		ScreenInstanceFactory.registerContextAwareScreenProvider(OptionsScreen.class.getName(), context -> new OptionsScreen(context.parentScreen(), Minecraft.getInstance().options));

	}

	public static void registerScreenProvider(@NotNull String screenClassPath, @NotNull Supplier<? extends Screen> provider) {
		Objects.requireNonNull(screenClassPath);
		Objects.requireNonNull(provider);
		SCREEN_INSTANCE_PROVIDERS.put(screenClassPath, context -> provider.get());
		LEGACY_SCREEN_INSTANCE_PROVIDERS.put(screenClassPath, provider);
	}

	public static void registerContextAwareScreenProvider(@NotNull String screenClassPath, @NotNull Function<ScreenConstructionContext, ? extends Screen> provider) {
		Objects.requireNonNull(screenClassPath);
		SCREEN_INSTANCE_PROVIDERS.put(screenClassPath, Objects.requireNonNull(provider));
		LEGACY_SCREEN_INSTANCE_PROVIDERS.remove(screenClassPath);
	}

	private static void updateResourcePacks(@NotNull Options options, @NotNull PackRepository repository) {
		options.resourcePacks.clear();
		options.incompatibleResourcePacks.clear();
		for (Pack selected : repository.getSelectedPacks()) {
			if (!selected.isFixedPosition()) {
				options.resourcePacks.add(selected.getId());
				if (!selected.getCompatibility().isCompatible()) {
					options.incompatibleResourcePacks.add(selected.getId());
				}
			}
		}
		options.save();
	}

	@Nullable
	public static Supplier<? extends Screen> getScreenProvider(@NotNull String screenClassPath) {
		Supplier<? extends Screen> legacyProvider = LEGACY_SCREEN_INSTANCE_PROVIDERS.get(screenClassPath);
		if (legacyProvider != null) return legacyProvider;
		Function<ScreenConstructionContext, ? extends Screen> provider = getContextAwareScreenProvider(screenClassPath);
		return provider != null ? () -> provider.apply(ScreenConstructionContext.live()) : null;
	}

	@Nullable
	public static Function<ScreenConstructionContext, ? extends Screen> getContextAwareScreenProvider(@NotNull String screenClassPath) {
		return SCREEN_INSTANCE_PROVIDERS.get(screenClassPath);
	}

	@Nullable
	public static Screen tryConstruct(@NotNull String screenClassPathOrIdentifier) {
		return tryConstruct(screenClassPathOrIdentifier, ScreenConstructionContext.live());
	}

	@Nullable
	public static Screen tryConstruct(@NotNull String screenClassPathOrIdentifier, @NotNull ScreenConstructionContext context) {
		try {
			Objects.requireNonNull(context);
			//Convert universal identifiers to actual class paths
			if (UniversalScreenIdentifierRegistry.universalIdentifierExists(screenClassPathOrIdentifier)) {
				String nonUniversal = UniversalScreenIdentifierRegistry.getScreenForUniversalIdentifier(screenClassPathOrIdentifier);
				if (nonUniversal != null) screenClassPathOrIdentifier = nonUniversal;
			}
			//Fixing potentially invalid class paths
			screenClassPathOrIdentifier = ScreenIdentifierHandler.tryFixInvalidIdentifierWithNonUniversal(screenClassPathOrIdentifier);
			if (ScreenCustomization.isScreenBlacklisted(screenClassPathOrIdentifier)) {
				return null;
			}
			//Update last screen
			DEFAULT_PARAMETERS.put(Screen.class, context.parentScreen());
			//Update player
			DEFAULT_PARAMETERS.put(Player.class, Minecraft.getInstance().player);
			if (Minecraft.getInstance().player != null) {
				DEFAULT_PARAMETERS.put(ClientAdvancements.class, Minecraft.getInstance().player.connection.getAdvancements());
			}
			//Check if a provider is registered for the screen and return from provider if one was found
			Function<ScreenConstructionContext, ? extends Screen> screenProvider = getContextAwareScreenProvider(screenClassPathOrIdentifier);
			if (screenProvider != null) return screenProvider.apply(context);
			//Try to construct and instance of the screen
			Class<?> screenClass = Class.forName(screenClassPathOrIdentifier, false, ScreenInstanceFactory.class.getClassLoader());
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

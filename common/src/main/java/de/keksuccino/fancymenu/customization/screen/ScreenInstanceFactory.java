package de.keksuccino.fancymenu.customization.screen;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Lifecycle;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.customization.screen.identifier.UniversalScreenIdentifierRegistry;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinCreateWorldScreen;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldGenSettingsComponent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScreenInstanceFactory {
	
	private static final Map<Class<?>, Object> DEFAULT_PARAMETERS = new HashMap<>();
	private static final Map<String, Supplier<? extends Screen>> SCREEN_INSTANCE_PROVIDERS = new HashMap<>();

	static {

		DEFAULT_PARAMETERS.put(Minecraft.class, Minecraft.getInstance());
		DEFAULT_PARAMETERS.put(Screen.class, null);
		DEFAULT_PARAMETERS.put(Options.class, Minecraft.getInstance().options);
		DEFAULT_PARAMETERS.put(LanguageManager.class, Minecraft.getInstance().getLanguageManager());
		DEFAULT_PARAMETERS.put(Player.class, null);
		DEFAULT_PARAMETERS.put(String.class, "");
		DEFAULT_PARAMETERS.put(ClientAdvancements.class, null);
		DEFAULT_PARAMETERS.put(Component.class, Components.empty());
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

		ScreenInstanceFactory.registerScreenProvider(CreateWorldScreen.class.getName(),
				() -> {
					Minecraft $$0 = Minecraft.getInstance();
					Screen $$1 = Minecraft.getInstance().screen;
					PackRepository $$2 = new PackRepository(PackType.SERVER_DATA, new RepositorySource[]{new ServerPacksSource()});
					WorldLoader.InitConfig $$3 = IMixinCreateWorldScreen.invokeCreateDefaultLoadConfigFancyMenu($$2, DataPackConfig.DEFAULT);
					CompletableFuture<WorldCreationContext> $$4 = WorldLoader.load($$3, ($$0x, $$1x) -> {
						RegistryAccess.Frozen $$2x = RegistryAccess.builtinCopy().freeze();
						WorldGenSettings $$3x = WorldPresets.createNormalWorldFromPreset($$2x);
						return Pair.of($$3x, $$2x);
					}, ($$0x, $$1x, $$2x, $$3x) -> {
						$$0x.close();
						return new WorldCreationContext($$3x, Lifecycle.stable(), $$2x, $$1x);
					}, Util.backgroundExecutor(), $$0);
					Objects.requireNonNull($$4);
					$$0.managedBlock($$4::isDone);
					return IMixinCreateWorldScreen.invokeConstructFancyMenu($$1, DataPackConfig.DEFAULT, new WorldGenSettingsComponent((WorldCreationContext)$$4.join(), Optional.of(WorldPresets.NORMAL), OptionalLong.empty()));
				});

		ScreenInstanceFactory.registerScreenProvider(PackSelectionScreen.class.getName(),
				() -> new PackSelectionScreen(
						(Minecraft.getInstance().screen != null) ? Minecraft.getInstance().screen : new TitleScreen(),
						Minecraft.getInstance().getResourcePackRepository(),
						repository -> {
							Options options = Minecraft.getInstance().options;
							List<String> $$1 = ImmutableList.copyOf(options.resourcePacks);
							options.resourcePacks.clear();
							options.incompatibleResourcePacks.clear();
							for(Pack $$2 : repository.getSelectedPacks()) {
								if (!$$2.isFixedPosition()) {
									options.resourcePacks.add($$2.getId());
									if (!$$2.getCompatibility().isCompatible()) {
										options.incompatibleResourcePacks.add($$2.getId());
									}
								}
							}
							options.save();
							List<String> $$3 = ImmutableList.copyOf(options.resourcePacks);
							if (!$$3.equals($$1)) {
								Minecraft.getInstance().reloadResourcePacks();
							}
						},
						Minecraft.getInstance().getResourcePackDirectory(),
						Component.translatable("resourcePack.title")
				));

	}

	public static void registerScreenProvider(@NotNull String screenClassPath, @NotNull Supplier<? extends Screen> provider) {
		SCREEN_INSTANCE_PROVIDERS.put(screenClassPath, provider);
	}

	@Nullable
	public static Supplier<? extends Screen> getScreenProvider(@NotNull String screenClassPath) {
		return SCREEN_INSTANCE_PROVIDERS.get(screenClassPath);
	}

	@Nullable
	public static Screen tryConstruct(@NotNull String screenClassPathOrIdentifier) {
		try {
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
			DEFAULT_PARAMETERS.put(Screen.class, Minecraft.getInstance().screen);
			//Update player
			DEFAULT_PARAMETERS.put(Player.class, Minecraft.getInstance().player);
			if (Minecraft.getInstance().player != null) {
				DEFAULT_PARAMETERS.put(ClientAdvancements.class, Minecraft.getInstance().player.connection.getAdvancements());
			}
			//Check if a provider is registered for the screen and return from provider if one was found
			Supplier<? extends Screen> screenProvider = getScreenProvider(screenClassPathOrIdentifier);
			if (screenProvider != null) return screenProvider.get();
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

package de.keksuccino.fancymenu.customization.animation;

import java.io.File;
import java.util.*;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.animation.AnimationData.Type;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * DOES _NOTHING_ ANYMORE! JUST HERE FOR BACKWARDS COMPAT! DON'T USE THIS CLASS ANYMORE!<br>
 * The old animation format does not exist anymore. It got replaced by FMA files.
 */
@Deprecated(forRemoval = true)
public class AnimationHandler {

	private static final Logger LOGGER = LogManager.getLogger();
	@Deprecated
	public static final File ANIMATIONS_DIR = FileUtils.createDirectory(new File(FancyMenu.MOD_DIR, "/animations"));
	private static final Map<String, AnimationData> ANIMATIONS = new HashMap<>();
	private static final List<String> EXTERNAL_ANIMATION_NAMES = new ArrayList<>();
	protected static boolean preloadCompleted = false;
	@Deprecated
	protected static boolean initialized = false;

	/**
	 * DOES _NOTHING_ ANYMORE! JUST HERE FOR BACKWARDS COMPAT! DON'T USE THIS METHOD ANYMORE!<br>
	 * The old animation format does not exist anymore. It got replaced by FMA files.
	 */
	@Deprecated(forRemoval = true)
	public static void init() {}

	/**
	 * DOES _NOTHING_ ANYMORE! JUST HERE FOR BACKWARDS COMPAT! DON'T USE THIS METHOD ANYMORE!<br>
	 * The old animation format does not exist anymore. It got replaced by FMA files.
	 */
	@Deprecated(forRemoval = true)
	public static void register(@NotNull IAnimationRenderer animation, @NotNull String name, @NotNull Type type) {}

	/**
	 * DOES _NOTHING_ ANYMORE! JUST HERE FOR BACKWARDS COMPAT! DON'T USE THIS METHOD ANYMORE!<br>
	 * The old animation format does not exist anymore. It got replaced by FMA files.
	 */
	@Deprecated(forRemoval = true)
	public static void unregister(@NotNull IAnimationRenderer animation) {}

	/**
	 * DOES _NOTHING_ ANYMORE! JUST HERE FOR BACKWARDS COMPAT! DON'T USE THIS METHOD ANYMORE!<br>
	 * The old animation format does not exist anymore. It got replaced by FMA files.
	 */
	@Deprecated(forRemoval = true)
	public static void unregister(@NotNull String name) {}

	/**
	 * DOES _NOTHING_ ANYMORE! JUST HERE FOR BACKWARDS COMPAT! DON'T USE THIS METHOD ANYMORE!<br>
	 * The old animation format does not exist anymore. It got replaced by FMA files.
	 */
	@Deprecated(forRemoval = true)
	public static void discoverAndRegisterExternalAnimations() {}

	/**
	 * DOES _NOTHING_ ANYMORE! JUST HERE FOR BACKWARDS COMPAT! DON'T USE THIS METHOD ANYMORE!<br>
	 * The old animation format does not exist anymore. It got replaced by FMA files.
	 */
	@Deprecated(forRemoval = true)
	@NotNull
	public static List<String> getExternalAnimationNames() {
		return new ArrayList<>();
	}

	private static void clearExternalAnimations() {}

	/**
	 * DOES _NOTHING_ ANYMORE! JUST HERE FOR BACKWARDS COMPAT! DON'T USE THIS METHOD ANYMORE!<br>
	 * The old animation format does not exist anymore. It got replaced by FMA files.
	 */
	@Deprecated(forRemoval = true)
	public static boolean animationExists(@NotNull String name) {
		return false;
	}

	/**
	 * DOES _NOTHING_ ANYMORE! JUST HERE FOR BACKWARDS COMPAT! DON'T USE THIS METHOD ANYMORE!<br>
	 * The old animation format does not exist anymore. It got replaced by FMA files.
	 */
	@Deprecated(forRemoval = true)
	@NotNull
	public static List<IAnimationRenderer> getAnimations() {
		return new ArrayList<>();
	}

	/**
	 * DOES _NOTHING_ ANYMORE! JUST HERE FOR BACKWARDS COMPAT! DON'T USE THIS METHOD ANYMORE!<br>
	 * The old animation format does not exist anymore. It got replaced by FMA files.
	 */
	@Deprecated(forRemoval = true)
	@Nullable
	public static IAnimationRenderer getAnimation(String name) {
		return null;
	}

	/**
	 * DOES _NOTHING_ ANYMORE! JUST HERE FOR BACKWARDS COMPAT! DON'T USE THIS METHOD ANYMORE!<br>
	 * The old animation format does not exist anymore. It got replaced by FMA files.
	 */
	@Deprecated(forRemoval = true)
	public static void resetAnimations() {}

	/**
	 * DOES _NOTHING_ ANYMORE! JUST HERE FOR BACKWARDS COMPAT! DON'T USE THIS METHOD ANYMORE!<br>
	 * The old animation format does not exist anymore. It got replaced by FMA files.
	 */
	@Deprecated(forRemoval = true)
	public static void resetAnimationSounds() {}

	/**
	 * DOES _NOTHING_ ANYMORE! JUST HERE FOR BACKWARDS COMPAT! DON'T USE THIS METHOD ANYMORE!<br>
	 * The old animation format does not exist anymore. It got replaced by FMA files.
	 */
	@Deprecated(forRemoval = true)
	public static void stopAnimationSounds() {}

	/**
	 * DOES _NOTHING_ ANYMORE! JUST HERE FOR BACKWARDS COMPAT! DON'T USE THIS METHOD ANYMORE!<br>
	 * The old animation format does not exist anymore. It got replaced by FMA files.
	 */
	@Deprecated(forRemoval = true)
	public static void updateAnimationSizes() {}

	/**
	 * DOES _NOTHING_ ANYMORE! JUST HERE FOR BACKWARDS COMPAT! DON'T USE THIS METHOD ANYMORE!<br>
	 * The old animation format does not exist anymore. It got replaced by FMA files.
	 */
	@Deprecated(forRemoval = true)
	public static void preloadAnimations(boolean ignoreAlreadyPreloaded) {}

	/**
	 * DOES _NOTHING_ ANYMORE! JUST HERE FOR BACKWARDS COMPAT! DON'T USE THIS METHOD ANYMORE!<br>
	 * The old animation format does not exist anymore. It got replaced by FMA files.
	 */
	@Deprecated(forRemoval = true)
	public static boolean preloadingCompleted() {
		return true;
	}
	
}

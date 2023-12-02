package de.keksuccino.fancymenu.customization.gameintro;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.file.type.types.ImageFileType;
import de.keksuccino.fancymenu.util.file.type.types.VideoFileType;
import de.keksuccino.fancymenu.util.resources.*;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GameIntroHandler {

	public static boolean introPlayed = false;

	public static boolean shouldPlayIntro() {
		return !FancyMenu.getOptions().gameIntroAnimation.getValue().isEmpty() && AnimationHandler.animationExists(FancyMenu.getOptions().gameIntroAnimation.getValue());
	}

	@Nullable
	public static PlayableResource getIntro() {
		String source = FancyMenu.getOptions().gameIntroAnimation.getValue();
		if (source.trim().isEmpty()) return null;
		source = PlaceholderParser.replacePlaceholders(source);
		AdvancedAnimation animation = getGameIntroAnimation(source);
		if (animation != null) return animation;
		ResourceSource resourceSource = ResourceSource.of(source);
		FileType<?> fileType;
		fileType = FileTypes.getType(resourceSource, false);
		if (fileType == null) {
			fileType = FileTypes.getType(resourceSource, true);
		}
		if (fileType != null) {
			if (fileType instanceof ImageFileType) {
				ITexture t = ResourceHandlers.getImageHandler().get(resourceSource);
				if (t instanceof PlayableResource p) return p;
			} else if (fileType instanceof VideoFileType) {
				return ResourceHandlers.getVideoHandler().get(resourceSource);
			}
		}
		return null;
	}

	public static boolean introIsAnimation() {
		String source = FancyMenu.getOptions().gameIntroAnimation.getValue();
		if (source.trim().isEmpty()) return false;
		return getGameIntroAnimation(source) != null;
	}

	@Deprecated
	@Nullable
	private static AdvancedAnimation getGameIntroAnimation(@NotNull String name) {
		if (AnimationHandler.animationExists(name)) {
			if (AnimationHandler.getAnimation(name) instanceof AdvancedAnimation a) return a;
		}
		return null;
	}
	
}

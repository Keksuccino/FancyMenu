package de.keksuccino.fancymenu.customization.gameintro;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.file.type.types.ImageFileType;
import de.keksuccino.fancymenu.util.file.type.types.VideoFileType;
import de.keksuccino.fancymenu.util.resource.*;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class GameIntroHandler {

	private static final Logger LOGGER = LogManager.getLogger();

	public static boolean introPlayed = false;

	public static boolean shouldPlayIntro() {
		if (FancyMenu.getOptions().gameIntroAnimation.getValue().trim().isEmpty()) return false;
		return getIntro() != null;
	}

	@Nullable
	public static PlayableResource getIntro() {
		String source = FancyMenu.getOptions().gameIntroAnimation.getValue();
		if (source.trim().isEmpty()) return null;
		source = PlaceholderParser.replacePlaceholders(source);
		ResourceSource resourceSource = ResourceSource.of(source);
		FileType<?> fileType;
		fileType = FileTypes.getType(resourceSource, false);
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
	
}

package de.keksuccino.fancymenu.customization.gameintro;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.file.type.types.ImageFileType;
import de.keksuccino.fancymenu.util.file.type.types.VideoFileType;
import de.keksuccino.fancymenu.util.resource.*;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
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

	public static float getIntroVolume() {
		return Math.max(0.0F, Math.min(1.0F, FancyMenu.getOptions().gameIntroVolume.getValue()));
	}

	@NotNull
	public static SoundSource getIntroSoundSource() {
		String name = FancyMenu.getOptions().gameIntroSoundChannel.getValue();
		if (name != null) {
			for (SoundSource source : SoundSource.values()) {
				if (source.getName().equals(name)) return source;
			}
		}
		return SoundSource.MASTER;
	}

	@Nullable
	public static PlayableResource getIntro() {
		String source = FancyMenu.getOptions().gameIntroAnimation.getValue();
		if (source.trim().isEmpty()) return null;
		source = PlaceholderParser.replacePlaceholders(source);
		ResourceSource resourceSource = ResourceSource.of(source);
		FileType<?> fileType;
		// Use advanced checks for web sources so MP4 URLs with query parameters are detected reliably.
		fileType = FileTypes.getType(resourceSource, true);
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

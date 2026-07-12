package de.keksuccino.fancymenu.customization.gameintro;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.file.type.types.ImageFileType;
import de.keksuccino.fancymenu.util.file.type.types.VideoFileType;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GameIntroHandler {

	public static boolean introPlayed = false;

    /**
     * Starts an unconsumed intro for a loading overlay. The intro remains unconsumed until its overlay completes, allowing a queued resource reload to displace and retry it.
     */
    public static boolean tryStartIntro(@NotNull Screen fadeTo) {
        Objects.requireNonNull(fadeTo);
        return tryStartIntro(introPlayed, GameIntroHandler::getIntro, intro -> Minecraft.getInstance().setOverlay(new GameIntroOverlay(fadeTo, intro)));
    }

    static <T> boolean tryStartIntro(boolean alreadyPlayed, @NotNull Supplier<T> introSupplier, @NotNull Consumer<T> introStarter) {
        Objects.requireNonNull(introSupplier);
        Objects.requireNonNull(introStarter);
        if (alreadyPlayed) return false;
        T intro = introSupplier.get();
        if (intro == null) return false;
        introStarter.accept(intro);
        return true;
    }

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

package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy.buddy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class BuddySaveFileNames {

    private BuddySaveFileNames() {
    }

    @NotNull
    public static String buildSaveFileName(@NotNull String prefix, @Nullable String instanceIdentifier) {
        String identifier = (instanceIdentifier == null || instanceIdentifier.isBlank()) ? "default" : instanceIdentifier;
        return prefix + encodeIdentifier(identifier) + ".json";
    }

    @NotNull
    private static String encodeIdentifier(@NotNull String identifier) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(identifier.getBytes(StandardCharsets.UTF_8));
    }

}

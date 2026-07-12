package de.keksuccino.fancymenu.mixin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.mixin.support.client.CenteredIconButtonLabelResolver;
import de.keksuccino.fancymenu.mixin.support.client.ContainerWidgetPointerRouter;
import de.keksuccino.fancymenu.mixin.support.client.PauseScreenWidgetIdentifierResolver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MixinPackageIsolationTest {

    @Test
    void runtimeSupportClassesAreOutsideOwnedMixinPackage() throws IOException {
        String mixinPackagePrefix;
        try (InputStream stream = requireNonNull(MixinPackageIsolationTest.class.getResourceAsStream("/fancymenu.mixins.json")); InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject mixinConfig = JsonParser.parseReader(reader).getAsJsonObject();
            mixinPackagePrefix = mixinConfig.get("package").getAsString() + ".";
        }

        assertAll(() -> assertFalse(ContainerWidgetPointerRouter.class.getName().startsWith(mixinPackagePrefix)), () -> assertFalse(CenteredIconButtonLabelResolver.class.getName().startsWith(mixinPackagePrefix)), () -> assertFalse(PauseScreenWidgetIdentifierResolver.class.getName().startsWith(mixinPackagePrefix)));
    }

}

package de.keksuccino.fancymenu.customization.screeninstancefactory.dummyscreens;

import de.keksuccino.fancymenu.mixin.mixins.client.IMixinConnectScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class DummyScreenRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, DummyScreenBuilder> BUILDERS = new LinkedHashMap<>();

    static {

        register(new DummyScreenBuilder(LevelLoadingScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.level_loading_screen"), () -> new LevelLoadingScreen(new StoringChunkProgressListener(0))));
        register(new DummyScreenBuilder(GenericDirtMessageScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.dirt_message_screen"), () -> new GenericDirtMessageScreen(Component.literal("404 - Funny placeholder message not found!"))).setScreenDescription(List.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.tools.dirt_message_screen.desc"))));
        register(new DummyScreenBuilder(ProgressScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.progress_screen"), () -> {
            ProgressScreen s = new ProgressScreen(false);
            s.progressStart(Component.literal("Progress Status Message"));
            s.progressStage(Component.literal("Progress Stage Message"));
            s.progressStagePercentage(24);
            return s;
        }));
        register(new DummyScreenBuilder(ConnectScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.connect_screen"), () -> IMixinConnectScreen.invokeConstructFancyMenu(new TitleScreen())));

    }

    public static void register(@NotNull DummyScreenBuilder builder) {
        Objects.requireNonNull(builder);
        if (BUILDERS.containsKey(builder.screenIdentifier)) {
            LOGGER.warn("[FANCYMENU] DummyScreenBuilder for screen identifier '" + builder.screenIdentifier + "' already exists! Replacing builder..");
        }
        BUILDERS.put(builder.screenIdentifier, builder);
    }

    @Nullable
    public static DummyScreenBuilder getBuilderFor(@NotNull String screenIdentifier) {
        return BUILDERS.get(screenIdentifier);
    }

    @NotNull
    public static List<DummyScreenBuilder> getBuilders() {
        return new ArrayList<>(BUILDERS.values());
    }

}

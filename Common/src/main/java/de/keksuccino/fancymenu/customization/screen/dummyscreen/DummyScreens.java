package de.keksuccino.fancymenu.customization.screen.dummyscreen;

import de.keksuccino.fancymenu.mixin.mixins.client.IMixinConnectScreen;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinReceivingLevelScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import java.util.List;

public class DummyScreens {

    public static final DummyScreenBuilder LEVEL_LOADING_SCREEN_DUMMY = new DummyScreenBuilder(LevelLoadingScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.level_loading_screen"), () -> new LevelLoadingScreen(new StoringChunkProgressListener(0)));
    public static final DummyScreenBuilder GENERIC_DIRT_MESSAGE_SCREEN_DUMMY = new DummyScreenBuilder(GenericDirtMessageScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.dirt_message_screen"), () -> new GenericDirtMessageScreen(Component.literal("404 - Funny placeholder message not found!"))).setScreenDescriptionSupplier(() -> List.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.tools.dirt_message_screen.desc")));
    public static final DummyScreenBuilder PROGRESS_SCREEN_DUMMY = new DummyScreenBuilder(ProgressScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.progress_screen"), () -> {
        ProgressScreen s = new ProgressScreen(false);
        s.progressStart(Component.literal("Progress Status Message"));
        s.progressStage(Component.literal("Progress Stage Message"));
        s.progressStagePercentage(24);
        return s;
    });
    public static final DummyScreenBuilder CONNECT_SCREEN_DUMMY = new DummyScreenBuilder(ConnectScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.connect_screen"), () -> IMixinConnectScreen.invokeConstructFancyMenu(new TitleScreen()));
    public static final DummyScreenBuilder RECEIVING_LEVEL_SCREEN_DUMMY = new DummyScreenBuilder(ReceivingLevelScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.dummy_screen_instances.receiving_level_screen"), () -> {
        ReceivingLevelScreen s = new ReceivingLevelScreen();
        ((IMixinReceivingLevelScreen)s).setCreatedAtFancyMenu(System.currentTimeMillis() + 10000000000000L);
        return s;
    }).setScreenDescriptionSupplier(() -> List.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.tools.dummy_screen_instances.receiving_level_screen.desc")));

    public static void registerAll() {

        DummyScreenRegistry.register(LEVEL_LOADING_SCREEN_DUMMY);
        DummyScreenRegistry.register(GENERIC_DIRT_MESSAGE_SCREEN_DUMMY);
        DummyScreenRegistry.register(PROGRESS_SCREEN_DUMMY);
        DummyScreenRegistry.register(CONNECT_SCREEN_DUMMY);
        DummyScreenRegistry.register(RECEIVING_LEVEL_SCREEN_DUMMY);

    }

}

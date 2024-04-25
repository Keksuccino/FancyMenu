package de.keksuccino.fancymenu.customization.screen.dummyscreen;

import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinConnectScreen;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinReceivingLevelScreen;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;

public class DummyScreens {

    public static final DummyScreenBuilder LEVEL_LOADING_SCREEN_DUMMY = new DummyScreenBuilder(LevelLoadingScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.level_loading_screen"), () -> new LevelLoadingScreen(StoringChunkProgressListener.createCompleted()));
    public static final DummyScreenBuilder GENERIC_DIRT_MESSAGE_SCREEN_DUMMY = new DummyScreenBuilder(GenericMessageScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.dirt_message_screen"), () -> new GenericMessageScreen(Component.literal("404 - Funny placeholder message not found!"))).setScreenDescriptionSupplier(() -> ListUtils.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.tools.dirt_message_screen.desc")));
    public static final DummyScreenBuilder PROGRESS_SCREEN_DUMMY = new DummyScreenBuilder(ProgressScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.progress_screen"), () -> {
        ProgressScreen s = new ProgressScreen(false);
        s.progressStart(Component.literal("Progress Status Message"));
        s.progressStage(Component.literal("Progress Stage Message"));
        s.progressStagePercentage(24);
        return s;
    });
    public static final DummyScreenBuilder CONNECT_SCREEN_DUMMY = new DummyScreenBuilder(ConnectScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.connect_screen"), () -> IMixinConnectScreen.invokeConstructFancyMenu(new TitleScreen(), Component.empty()));
    public static final DummyScreenBuilder RECEIVING_LEVEL_SCREEN_DUMMY = new DummyScreenBuilder(ReceivingLevelScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.dummy_screen_instances.receiving_level_screen"), () -> {
        ReceivingLevelScreen s = new ReceivingLevelScreen(() -> false, ReceivingLevelScreen.Reason.OTHER);
        ((IMixinReceivingLevelScreen)s).setCreatedAtFancyMenu(System.currentTimeMillis() + 10000000000000L);
        return s;
    }).setScreenDescriptionSupplier(() -> ListUtils.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.tools.dummy_screen_instances.receiving_level_screen.desc")));

    public static void registerAll() {

        DummyScreenRegistry.register(LEVEL_LOADING_SCREEN_DUMMY);
        DummyScreenRegistry.register(GENERIC_DIRT_MESSAGE_SCREEN_DUMMY);
        DummyScreenRegistry.register(PROGRESS_SCREEN_DUMMY);
        DummyScreenRegistry.register(CONNECT_SCREEN_DUMMY);
        DummyScreenRegistry.register(RECEIVING_LEVEL_SCREEN_DUMMY);

    }

}

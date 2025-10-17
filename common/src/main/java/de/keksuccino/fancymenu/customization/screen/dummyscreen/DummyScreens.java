package de.keksuccino.fancymenu.customization.screen.dummyscreen;

import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinConnectScreen;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

public class DummyScreens {

    public static final DummyScreenBuilder LEVEL_LOADING_SCREEN_DUMMY = new DummyScreenBuilder(LevelLoadingScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.level_loading_screen"), () -> {
        LevelLoadTracker tracker = new LevelLoadTracker();
        tracker.setServerChunkStatusView(new DummyChunkLoadStatusView());
        // I don't know what the hell I'm doing btw, I just typed some numbers and it started working
        tracker.start(LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS, 5);
        tracker.update(LevelLoadListener.Stage.LOAD_PLAYER_CHUNKS, 5, 5);
        tracker.updateFocus(ClientLevel.OVERWORLD, ChunkPos.ZERO);
        return new LevelLoadingScreen(tracker, LevelLoadingScreen.Reason.OTHER);
    });
    public static final DummyScreenBuilder GENERIC_DIRT_MESSAGE_SCREEN_DUMMY = new DummyScreenBuilder(GenericMessageScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.dirt_message_screen"), () -> new GenericMessageScreen(Component.literal("404 - Funny placeholder message not found!"))).setScreenDescriptionSupplier(() -> ListUtils.of(LocalizationUtils.splitLocalizedLines("fancymenu.overlay.menu_bar.tools.dirt_message_screen.desc")));
    public static final DummyScreenBuilder PROGRESS_SCREEN_DUMMY = new DummyScreenBuilder(ProgressScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.progress_screen"), () -> {
        ProgressScreen s = new ProgressScreen(false);
        s.progressStart(Component.literal("Progress Status Message"));
        s.progressStage(Component.literal("Progress Stage Message"));
        s.progressStagePercentage(24);
        return s;
    });
    public static final DummyScreenBuilder CONNECT_SCREEN_DUMMY = new DummyScreenBuilder(ConnectScreen.class.getName(), Component.translatable("fancymenu.overlay.menu_bar.tools.connect_screen"), () -> IMixinConnectScreen.invokeConstructFancyMenu(new TitleScreen(), Component.empty()));

    public static void registerAll() {

        DummyScreenRegistry.register(LEVEL_LOADING_SCREEN_DUMMY);
        DummyScreenRegistry.register(GENERIC_DIRT_MESSAGE_SCREEN_DUMMY);
        DummyScreenRegistry.register(PROGRESS_SCREEN_DUMMY);
        DummyScreenRegistry.register(CONNECT_SCREEN_DUMMY);

    }

    private static class DummyChunkLoadStatusView implements ChunkLoadStatusView {

        @Override
        public void moveTo(ResourceKey<Level> resourceKey, ChunkPos chunkPos) {
        }

        @Override
        public @Nullable ChunkStatus get(int i, int i1) {
            return ChunkStatus.SPAWN;
        }

        @Override
        public int radius() {
            return 10;
        }

    }

}

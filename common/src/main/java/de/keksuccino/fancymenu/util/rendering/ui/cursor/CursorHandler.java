package de.keksuccino.fancymenu.util.rendering.ui.cursor;

import org.lwjgl.sdl.SDLMouse;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.resource.resources.texture.PngTexture;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLSurface;
import org.lwjgl.sdl.SDLPixels;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

@SuppressWarnings("unused")
public class CursorHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final CursorHandleLifecycle CURSOR_HANDLE_LIFECYCLE = new CursorHandleLifecycle(new MinecraftSdlThreadExecutor(), new SdlNativeOperations(), throwable -> LOGGER.error("[FANCYMENU] Failed to release a SDL cursor!", throwable));
    private static final CursorRegistry<CustomCursor> CUSTOM_CURSORS = new CursorRegistry<>(new CustomCursorRetirement());
    private static final CursorTickSelection<CustomCursor> CLIENT_TICK_CURSOR = new CursorTickSelection<>();

    public static final long CURSOR_RESIZE_HORIZONTAL = createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_EW_RESIZE);
    public static final long CURSOR_RESIZE_VERTICAL = createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_NS_RESIZE);
    public static final long CURSOR_RESIZE_NWSE = createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_NWSE_RESIZE);
    public static final long CURSOR_RESIZE_NESW = createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_NESW_RESIZE);
    public static final long CURSOR_RESIZE_ALL = createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_MOVE);
    public static final long CURSOR_WRITING = createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_TEXT);
    public static final long CURSOR_POINTING_HAND = createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_POINTER);
    public static final long CURSOR_NORMAL = createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_DEFAULT);

    private static volatile boolean initialized = false;

    /** Returns SDL's process-wide active cursor, or {@code -1} before the client window is available. */
    public static long getActiveCursor() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getWindow() == null) return -1L;
            return SDLMouse.SDL_GetCursor();
        } catch (Exception ignored) {
            return -1L;
        }
    }

    /**
     * Returns the standard SDL cursor shape id of the currently active cursor, or {@code -1} if unknown/not a standard cursor.
     */
    public static int getActiveStandardCursorShape() {
        long cursor = getActiveCursor();
        if (cursor == 0L || cursor == SDLMouse.SDL_GetDefaultCursor()) {
            return SDLMouse.SDL_SYSTEM_CURSOR_DEFAULT; // SDL owns the default cursor.
        }
        if (cursor <= 0L) {
            return -1;
        }
        return SdlCursorTracker.getStandardCursorShape(cursor);
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        EventHandler.INSTANCE.registerListenersOf(new CursorHandler());
    }

    public static void registerCustomCursor(@NotNull String uniqueCursorName, @NotNull CustomCursor cursor) {
        if (!initialized) throw new RuntimeException("[FANCYMENU] CursorHandler accessed too early!");
        LOGGER.info("[FANCYMENU] Registering SDL custom cursor: NAME: " + uniqueCursorName + " | TEXTURE CONTEXT: " + cursor.textureName);
        CUSTOM_CURSORS.register(Objects.requireNonNull(uniqueCursorName), Objects.requireNonNull(cursor), CustomCursor::isUsable);
    }

    public static void unregisterCustomCursor(@NotNull String cursorName) {
        if (!initialized) throw new RuntimeException("[FANCYMENU] CursorHandler accessed too early!");
        CUSTOM_CURSORS.unregister(Objects.requireNonNull(cursorName));
    }

    public static void unregisterCustomCursor(@NotNull String cursorName, @NotNull CustomCursor expectedCursor) {
        if (!initialized) throw new RuntimeException("[FANCYMENU] CursorHandler accessed too early!");
        CUSTOM_CURSORS.unregister(Objects.requireNonNull(cursorName), Objects.requireNonNull(expectedCursor));
    }

    @Nullable
    public static CustomCursor getCustomCursor(@NotNull String cursorName) {
        if (!initialized) throw new RuntimeException("[FANCYMENU] CursorHandler accessed too early!");
        CustomCursor cursor = CUSTOM_CURSORS.get(Objects.requireNonNull(cursorName));
        return cursor != null && cursor.isUsable() ? cursor : null;
    }

    /**
     * Cursor gets reset every tick, so only set non-default cursors here.
     */
    public static void setClientTickCursor(long cursor) {
        if (!initialized) throw new RuntimeException("[FANCYMENU] CursorHandler accessed too early!");
        CLIENT_TICK_CURSOR.setRawCursor(cursor);
    }

    /**
     * Cursor gets reset every tick.
     */
    public static void setClientTickCursor(@NotNull String customCursorName) {
        if (!initialized) throw new RuntimeException("[FANCYMENU] CursorHandler accessed too early!");
        CustomCursor c = getCustomCursor(customCursorName);
        if (c != null) CLIENT_TICK_CURSOR.setCustomCursor(c, CustomCursor::isUsable);
    }

    private static void setCursor(long cursor) {
        if (!initialized) throw new RuntimeException("[FANCYMENU] CursorHandler accessed too early!");
        if (!SDLMouse.SDL_SetCursor(cursor == 0L ? SDLMouse.SDL_GetDefaultCursor() : cursor)) throw new IllegalStateException(SDLError.SDL_GetError());
    }

    @EventListener
    public void onClientTickPre(ClientTickEvent.Pre e) {
        long cursorToSet = CLIENT_TICK_CURSOR.takeCursorForTick(CURSOR_NORMAL, CustomCursor::isUsable, cursor -> cursor.id_long);
        if (cursorToSet != CursorTickSelection.NO_CURSOR_CHANGE) setCursor(cursorToSet);
    }

    /** Releases all FancyMenu-owned cursors while Minecraft's SDL window is still alive. */
    public static void shutdown() {
        LOGGER.info("[FANCYMENU] Releasing FancyMenu-owned SDL cursors during client shutdown..");
        CUSTOM_CURSORS.close();
        CLIENT_TICK_CURSOR.clear();
        CURSOR_HANDLE_LIFECYCLE.shutdown();
    }

    private static long createStandardCursor(int shape) {
        long cursor = SDLMouse.SDL_CreateSystemCursor(shape);
        SdlCursorTracker.onCreateSystemCursor(shape, cursor);
        CURSOR_HANDLE_LIFECYCLE.trackStandard(cursor);
        return cursor;
    }

    private static void destroyNativeCursor(long cursor) {
        // SDL cursors are process-wide. Detach an active cursor before releasing its native allocation.
        if (SDLMouse.SDL_GetCursor() == cursor && !SDLMouse.SDL_SetCursor(SDLMouse.SDL_GetDefaultCursor())) {
            throw new IllegalStateException("Could not detach active SDL cursor: " + SDLError.SDL_GetError());
        }
        SDLMouse.SDL_DestroyCursor(cursor);
        SdlCursorTracker.onDestroyCursor(cursor);
    }

    public static class CustomCursor {

        public final long id_long;
        public final int hotspotX;
        public final int hotspotY;
        private final CursorHandleLifecycle.Handle nativeHandle;
        @NotNull
        public final PngTexture texture;
        @NotNull
        public final String textureName;

        @SuppressWarnings("all")
        @Nullable
        public static CustomCursor create(@NotNull PngTexture texture, int hotspotX, int hotspotY, @NotNull String textureName) {
            CustomCursor customCursor = null;
            InputStream in = null;
            MemoryStack memStack = null;
            ByteBuffer texResourceBuffer = null;
            ByteBuffer stbBuffer = null;
            try {
                Objects.requireNonNull(texture);
                //Wait for the texture to load (Timeout = 5000ms)
                texture.waitForReady(5000);
                if (texture.isReady()) {
                    in = Objects.requireNonNull(texture.open());
                    texResourceBuffer = TextureUtil.readResource(in);
                    texResourceBuffer.rewind();
                    if (MemoryUtil.memAddress(texResourceBuffer) != 0L) {
                        memStack = MemoryStack.stackPush();
                        IntBuffer width = memStack.mallocInt(1);
                        IntBuffer height = memStack.mallocInt(1);
                        IntBuffer components = memStack.mallocInt(1);
                        stbBuffer = STBImage.stbi_load_from_memory(texResourceBuffer, width, height, components, 4);
                        if (stbBuffer != null) {
                            RenderSystem.assertOnRenderThread();
                            SDL_Surface surface = SDLSurface.SDL_CreateSurfaceFrom(width.get(0), height.get(0), SDLPixels.SDL_PIXELFORMAT_RGBA32, stbBuffer, width.get(0) * 4);
                            if (surface == null) throw new IOException("Could not create cursor surface: " + SDLError.SDL_GetError());
                            long lid;
                            try {
                                lid = SDLMouse.SDL_CreateColorCursor(surface, hotspotX, hotspotY);
                            } finally {
                                SDLSurface.SDL_DestroySurface(surface);
                            }
                            if (lid != 0L) {
                                customCursor = new CustomCursor(lid, hotspotX, hotspotY, texture, textureName);
                                if (!customCursor.isUsable()) customCursor = null;
                            } else {
                                throw new IllegalArgumentException("Failed to create custom cursor! Cursor handle was NULL!");
                            }
                        } else {
                            throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
                        }
                    } else {
                        throw new IllegalArgumentException("Invalid buffer! Memory address was NULL!");
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (texResourceBuffer != null) {
                try {
                    MemoryUtil.memFree(texResourceBuffer);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (stbBuffer != null) {
                try {
                    STBImage.stbi_image_free(stbBuffer);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            CloseableUtils.closeQuietly(in);
            CloseableUtils.closeQuietly(memStack);
            return customCursor;
        }

        protected CustomCursor(long id_long, int hotspotX, int hotspotY, @NotNull PngTexture texture, @NotNull String textureName) {
            this.id_long = id_long;
            this.hotspotX = hotspotX;
            this.hotspotY = hotspotY;
            this.texture = texture;
            this.textureName = textureName;
            this.nativeHandle = CURSOR_HANDLE_LIFECYCLE.trackCustom(id_long);
        }

        /** Idempotently unregisters and releases this cursor on Minecraft's SDL thread. */
        public void destroy() {
            CUSTOM_CURSORS.retire(this);
        }

        private boolean isUsable() {
            return this.nativeHandle.isLive();
        }

    }

    private static final class MinecraftSdlThreadExecutor implements CursorHandleLifecycle.ThreadExecutor {

        @Override
        public boolean isOnThread() {
            return RenderSystem.isOnRenderThread();
        }

        @Override
        public void execute(@NotNull Runnable task) {
            if (this.isOnThread()) {
                task.run();
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                minecraft.execute(task);
            } else {
                LOGGER.error("[FANCYMENU] Could not schedule SDL cursor cleanup because the Minecraft client is unavailable!");
            }
        }

    }

    private static final class CustomCursorRetirement implements CursorRegistry.Retirement<CustomCursor> {

        @Override
        public boolean markRetired(@NotNull CustomCursor cursor) {
            CLIENT_TICK_CURSOR.retireCustomCursor(cursor);
            return CURSOR_HANDLE_LIFECYCLE.requestDestruction(cursor.nativeHandle);
        }

        @Override
        public void executeRetirement(@NotNull CustomCursor cursor) {
            CURSOR_HANDLE_LIFECYCLE.executeDestruction(cursor.nativeHandle);
        }

    }

    private static final class SdlNativeOperations implements CursorHandleLifecycle.NativeOperations {

        @Override
        public void prepareForShutdown() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.getWindow() != null) SDLMouse.SDL_SetCursor(SDLMouse.SDL_GetDefaultCursor());
        }

        @Override
        public void destroyCursor(long nativeHandle) {
            destroyNativeCursor(nativeHandle);
        }

    }

}

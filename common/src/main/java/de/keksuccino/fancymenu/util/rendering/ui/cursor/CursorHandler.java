package de.keksuccino.fancymenu.util.rendering.ui.cursor;

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
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
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
    private static final CursorHandleLifecycle CURSOR_HANDLE_LIFECYCLE = new CursorHandleLifecycle(new MinecraftGlfwThreadExecutor(), new GlfwNativeOperations(), throwable -> LOGGER.error("[FANCYMENU] Failed to release a GLFW cursor!", throwable));
    private static final CursorRegistry<CustomCursor> CUSTOM_CURSORS = new CursorRegistry<>(new CustomCursorRetirement());
    private static final CursorTickSelection<CustomCursor> CLIENT_TICK_CURSOR = new CursorTickSelection<>();

    public static final long CURSOR_RESIZE_HORIZONTAL = createStandardCursor(GLFW.GLFW_RESIZE_EW_CURSOR);
    public static final long CURSOR_RESIZE_VERTICAL = createStandardCursor(GLFW.GLFW_RESIZE_NS_CURSOR);
    public static final long CURSOR_RESIZE_NWSE = createStandardCursor(GLFW.GLFW_RESIZE_NWSE_CURSOR);
    public static final long CURSOR_RESIZE_NESW = createStandardCursor(GLFW.GLFW_RESIZE_NESW_CURSOR);
    public static final long CURSOR_RESIZE_ALL = createStandardCursor(GLFW.GLFW_RESIZE_ALL_CURSOR);
    public static final long CURSOR_WRITING = createStandardCursor(GLFW.GLFW_IBEAM_CURSOR);
    public static final long CURSOR_POINTING_HAND = createStandardCursor(GLFW.GLFW_POINTING_HAND_CURSOR);
    public static final long CURSOR_NORMAL = createStandardCursor(GLFW.GLFW_ARROW_CURSOR);

    private static volatile boolean initialized = false;

    /**
     * Returns the currently active GLFW cursor handle on the Minecraft window, or {@code -1} if unknown/not yet tracked.
     * <p>
     * Note: GLFW does not expose a cursor getter, this relies on mixin hooks tracking calls to {@code GLFW.glfwSetCursor(...)}.
     */
    public static long getActiveCursor() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getWindow() == null) return -1L;
            return GlfwCursorTracker.getActiveCursor(de.keksuccino.fancymenu.util.window.WindowHandler.getWindowHandle());
        } catch (Exception ignored) {
            return -1L;
        }
    }

    /**
     * Returns the standard GLFW cursor shape id of the currently active cursor, or {@code -1} if unknown/not a standard cursor.
     */
    public static int getActiveStandardCursorShape() {
        long cursor = getActiveCursor();
        if (cursor == 0L) {
            return GLFW.GLFW_ARROW_CURSOR; // GLFW docs: NULL cursor switches back to default arrow cursor
        }
        if (cursor <= 0L) {
            return -1;
        }
        return GlfwCursorTracker.getStandardCursorShape(cursor);
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        EventHandler.INSTANCE.registerListenersOf(new CursorHandler());
    }

    public static void registerCustomCursor(@NotNull String uniqueCursorName, @NotNull CustomCursor cursor) {
        if (!initialized) throw new RuntimeException("[FANCYMENU] CursorHandler accessed too early!");
        LOGGER.info("[FANCYMENU] Registering GLFW custom cursor: NAME: " + uniqueCursorName + " | TEXTURE CONTEXT: " + cursor.textureName);
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
        GLFW.glfwSetCursor(de.keksuccino.fancymenu.util.window.WindowHandler.getWindowHandle(), cursor);
    }

    @EventListener
    public void onClientTickPre(ClientTickEvent.Pre e) {
        long cursorToSet = CLIENT_TICK_CURSOR.takeCursorForTick(CURSOR_NORMAL, CustomCursor::isUsable, cursor -> cursor.id_long);
        if (cursorToSet != CursorTickSelection.NO_CURSOR_CHANGE) setCursor(cursorToSet);
    }

    /** Releases all FancyMenu-owned cursors while Minecraft's GLFW window is still alive. */
    public static void shutdown() {
        LOGGER.info("[FANCYMENU] Releasing FancyMenu-owned GLFW cursors during client shutdown..");
        CUSTOM_CURSORS.close();
        CLIENT_TICK_CURSOR.clear();
        CURSOR_HANDLE_LIFECYCLE.shutdown();
    }

    private static long createStandardCursor(int shape) {
        long cursor = GLFW.glfwCreateStandardCursor(shape);
        CURSOR_HANDLE_LIFECYCLE.trackStandard(cursor);
        return cursor;
    }

    private static void destroyNativeCursor(long cursor) {
        boolean failedToSwitchWindow = false;
        for (long window : GlfwCursorTracker.getWindowsUsingCursor(cursor)) {
            try {
                GLFW.glfwSetCursor(window, 0L);
            } catch (Throwable throwable) {
                failedToSwitchWindow = true;
                LOGGER.error("[FANCYMENU] Failed to switch a window away from a GLFW cursor before destroying it!", throwable);
            }
        }
        if (failedToSwitchWindow) throw new IllegalStateException("Could not safely detach GLFW cursor " + cursor + " from every tracked window");
        GLFW.glfwDestroyCursor(cursor);
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
                        stbBuffer = STBImage.stbi_load_from_memory(texResourceBuffer, width, height, components, 0);
                        if (stbBuffer != null) {
                            GLFWImage image = GLFWImage.create();
                            image = image.set(texture.getWidth(), texture.getHeight(), stbBuffer);
                            RenderSystem.assertOnRenderThread();
                            long lid = GLFW.glfwCreateCursor(image, hotspotX, hotspotY);
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

        /** Idempotently unregisters and releases this cursor on Minecraft's GLFW thread. */
        public void destroy() {
            CUSTOM_CURSORS.retire(this);
        }

        private boolean isUsable() {
            return this.nativeHandle.isLive();
        }

    }

    private static final class MinecraftGlfwThreadExecutor implements CursorHandleLifecycle.ThreadExecutor {

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
                LOGGER.error("[FANCYMENU] Could not schedule GLFW cursor cleanup because the Minecraft client is unavailable!");
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

    private static final class GlfwNativeOperations implements CursorHandleLifecycle.NativeOperations {

        @Override
        public void prepareForShutdown() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.getWindow() != null) GLFW.glfwSetCursor(minecraft.getWindow().handle(), 0L);
        }

        @Override
        public void destroyCursor(long nativeHandle) {
            destroyNativeCursor(nativeHandle);
        }

    }

}

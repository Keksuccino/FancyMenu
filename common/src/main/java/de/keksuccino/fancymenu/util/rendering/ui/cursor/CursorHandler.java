package de.keksuccino.fancymenu.util.rendering.ui.cursor;

import com.mojang.blaze3d.platform.TextureUtil;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public class CursorHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final long CURSOR_RESIZE_HORIZONTAL = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_EW_CURSOR);
    public static final long CURSOR_RESIZE_VERTICAL = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NS_CURSOR);
    public static final long CURSOR_RESIZE_ALL = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_ALL_CURSOR);
    public static final long CURSOR_ROTATE = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_ALL_CURSOR); // Using resize_all as rotation cursor
    public static final long CURSOR_WRITING = GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR);
    public static final long CURSOR_POINTING_HAND = GLFW.glfwCreateStandardCursor(GLFW.GLFW_POINTING_HAND_CURSOR);
    private static final long CURSOR_NORMAL = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);

    private static final Map<String, CustomCursor> CUSTOM_CURSORS = new HashMap<>();

    private static long clientTickCursor = -2;
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;
        EventHandler.INSTANCE.registerListenersOf(new CursorHandler());
    }

    public static void registerCustomCursor(@NotNull String uniqueCursorName, @NotNull CustomCursor cursor) {
        if (!initialized) throw new RuntimeException("[FANCYMENU] CursorHandler accessed too early!");
        LOGGER.info("[FANCYMENU] Registering GLFW custom cursor: NAME: " + uniqueCursorName + " | TEXTURE CONTEXT: " + cursor.textureName);
        CUSTOM_CURSORS.put(Objects.requireNonNull(uniqueCursorName), Objects.requireNonNull(cursor));
    }

    public static void unregisterCustomCursor(@NotNull String cursorName) {
        if (!initialized) throw new RuntimeException("[FANCYMENU] CursorHandler accessed too early!");
        CustomCursor c = CUSTOM_CURSORS.get(cursorName);
        if (c != null) c.destroy();
        CUSTOM_CURSORS.remove(cursorName);
    }

    @Nullable
    public static CustomCursor getCustomCursor(@NotNull String cursorName) {
        if (!initialized) throw new RuntimeException("[FANCYMENU] CursorHandler accessed too early!");
        return CUSTOM_CURSORS.get(cursorName);
    }

    /**
     * Cursor gets reset every tick, so only set non-default cursors here.
     */
    public static void setClientTickCursor(long cursor) {
        if (!initialized) throw new RuntimeException("[FANCYMENU] CursorHandler accessed too early!");
        clientTickCursor = cursor;
    }

    /**
     * Cursor gets reset every tick.
     */
    public static void setClientTickCursor(@NotNull String customCursorName) {
        if (!initialized) throw new RuntimeException("[FANCYMENU] CursorHandler accessed too early!");
        CustomCursor c = getCustomCursor(customCursorName);
        if (c != null) setClientTickCursor(c.id_long);
    }

    private static void setCursor(long cursor) {
        if (!initialized) throw new RuntimeException("[FANCYMENU] CursorHandler accessed too early!");
        GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), cursor);
    }

    @EventListener
    public void onClientTickPre(ClientTickEvent.Pre e) {

        if ((clientTickCursor != -1) && (clientTickCursor != -2)) {
            //Set the non-default cursor set by the mod
            setCursor(clientTickCursor);
            clientTickCursor = -1;
        } else if (clientTickCursor == -1) {
            //Reset the cursor to default, if no custom cursor was set last tick
            setCursor(CURSOR_NORMAL);
            clientTickCursor = -2;
        }

    }

    public static class CustomCursor {

        public final long id_long;
        public final int hotspotX;
        public final int hotspotY;
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
                            long lid = GLFW.glfwCreateCursor(image, hotspotX, hotspotY);
                            if (lid != 0L) {
                                customCursor = new CustomCursor(lid, hotspotX, hotspotY, texture, textureName);
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
        }

        /** Does nothing. **/
        public void destroy() {
            //TODO unstable! fix this later!
//            try {
//                GLFW.glfwDestroyCursor(this.id_long);
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
        }

    }

}

package de.keksuccino.fancymenu.util.rendering.ui.cursor;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.resources.texture.LocalTexture;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public class CursorHandler {

    public static final long CURSOR_RESIZE_HORIZONTAL = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_EW_CURSOR);
    public static final long CURSOR_RESIZE_VERTICAL = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NS_CURSOR);
    public static final long CURSOR_RESIZE_ALL = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_ALL_CURSOR);
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
        unregisterCustomCursor(Objects.requireNonNull(uniqueCursorName));
        CUSTOM_CURSORS.put(uniqueCursorName, Objects.requireNonNull(cursor));
    }

    public static void unregisterCustomCursor(@NotNull String cursorName) {
        CustomCursor c = CUSTOM_CURSORS.get(cursorName);
        if (c != null) c.destroy();
        CUSTOM_CURSORS.remove(cursorName);
    }

    @Nullable
    public static CustomCursor getCustomCursor(@NotNull String cursorName) {
        return CUSTOM_CURSORS.get(cursorName);
    }

    /**
     * Cursor gets reset every tick, so only set non-default cursors here.
     */
    public static void setClientTickCursor(long cursor) {
        clientTickCursor = cursor;
    }

    /**
     * Cursor gets reset every tick.
     */
    public static void setClientTickCursor(@NotNull String customCursorName) {
        CustomCursor c = getCustomCursor(customCursorName);
        if (c != null) setClientTickCursor(c.id_long);
    }

    private static void setCursor(long cursor) {
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
        public final LocalTexture texture;

//        @Nullable
//        public static CustomCursor create(@NotNull LocalTexture texture, int hotspotX, int hotspotY) {
//            CustomCursor customCursor = null;
//            InputStream in = null;
//            GLFWImage image = null;
//            try {
//                ResourceLocation loc = Objects.requireNonNull(texture).getResourceLocation();
//                if (loc != null) {
//                    image = GLFWImage.create();
//                    in = Objects.requireNonNull(texture.tryOpen());
//                    byte[] byteArray = IOUtils.toByteArray(in);
//                    STBImage.load
//                    image.set(texture.getWidth(), texture.getHeight(), ByteBuffer.wrap(byteArray));
//                    long lid = GLFW.glfwCreateCursor(image, hotspotX, hotspotY);
//                    customCursor = new CustomCursor(lid, hotspotX, hotspotY, texture);
//                }
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//            IOUtils.closeQuietly(in);
//            try {
//                if (image != null) image.close();
//            } catch (Exception ignore) {}
//            return customCursor;
//        }

//        @Nullable
//        public static CustomCursor create(@NotNull LocalTexture texture, int hotspotX, int hotspotY) {
//            CustomCursor customCursor = null;
//            InputStream in = null;
//            GLFWImage image = null;
//            try {
//                if (Objects.requireNonNull(texture).getResourceLocation() != null) {
//                    image = GLFWImage.create();
//                    in = Objects.requireNonNull(texture.tryOpen());
//                    NativeImage nat = NativeImage.read(in);
//                    nat.asByteArray()
//                    ByteBuffer buffer = toByteBuffer(in);
//                    if (buffer != null) {
//                        IntBuffer width = BufferUtils.createIntBuffer(1);
//                        IntBuffer height = BufferUtils.createIntBuffer(1);
//                        IntBuffer components = BufferUtils.createIntBuffer(1);
//                        ByteBuffer img = STBImage.stbi_load_from_memory(buffer, width, height, components, 0);
//                        if (img != null) {
//                            image = GLFWImage.createSafe();
//                            image = image.set(texture.getWidth(), texture.getHeight(), );
//                            long lid = GLFW.glfwCreateCursor();
//                            customCursor = new CustomCursor(lid, hotspotX, hotspotY, texture);
//                            STBImage.stbi_image_free(img);
//                        }
//                    }
//                }
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//            IOUtils.closeQuietly(in);
////            try {
////                if (image != null) image.close();
////            } catch (Exception ignore) {}
//            return customCursor;
//        }

        @Nullable
        public static CustomCursor create(@NotNull LocalTexture texture, int hotspotX, int hotspotY) {
            CustomCursor customCursor = null;
            InputStream in = null;
            try {
                if (Objects.requireNonNull(texture).getResourceLocation() != null) {
                    in = Objects.requireNonNull(texture.tryOpen());
                    NativeImage nat = NativeImage.read(in);
                    GLFWImage image = GLFWImage.createSafe(nat.pixels);
                    if (image != null) {
                        long lid = GLFW.glfwCreateCursor(image, hotspotX, hotspotY);
                        customCursor = new CustomCursor(lid, hotspotX, hotspotY, texture);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            IOUtils.closeQuietly(in);
            return customCursor;
        }

        @Nullable
        private static ByteBuffer toByteBuffer(@NotNull InputStream in) {
            try{
                byte[] bytes = IOUtils.toByteArray(in);
                ByteBuffer bb = BufferUtils.createByteBuffer(bytes.length);
                bb.put(bytes).flip();
                return bb;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }

        protected CustomCursor(long id_long, int hotspotX, int hotspotY, @NotNull LocalTexture texture) {
            this.id_long = id_long;
            this.hotspotX = hotspotX;
            this.hotspotY = hotspotY;
            this.texture = texture;
        }

        public void destroy() {
            GLFW.glfwDestroyCursor(this.id_long);
        }

    }

}

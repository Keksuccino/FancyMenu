package de.keksuccino.fancymenu.util.file;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLProperties;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Owns the native data that SDL keeps alive while an asynchronous file dialog is open. */
public final class NativeFileDialog {

    private static final AtomicLong NEXT_ID = new AtomicLong();
    private static final Map<Long, Request> PENDING = new ConcurrentHashMap<>();
    private static final class CallbackHolder {

        // Keep the native callback alive until process exit, including while SDL returns through it.
        private static final SDL_DialogFileCallback CALLBACK = SDL_DialogFileCallback.create(NativeFileDialog::onSelection);

    }

    private NativeFileDialog() {
    }

    public static void open(@NotNull String title, @Nullable String description, @NotNull List<String> patterns, @NotNull Consumer<Result> consumer) {
        RenderSystem.assertOnRenderThread();
        long id = NEXT_ID.incrementAndGet();
        Request request = new Request(description, patterns, consumer);
        int properties = SDLProperties.SDL_CreateProperties();
        try {
            if (properties == 0) throw new IllegalStateException(SDLError.SDL_GetError());
            SDLProperties.SDL_SetStringProperty(properties, SDLDialog.SDL_PROP_FILE_DIALOG_TITLE_STRING, title);
            SDLProperties.SDL_SetPointerProperty(properties, SDLDialog.SDL_PROP_FILE_DIALOG_WINDOW_POINTER, WindowHandler.getWindowHandle());
            SDLProperties.SDL_SetPointerProperty(properties, SDLDialog.SDL_PROP_FILE_DIALOG_FILTERS_POINTER, request.filters.address());
            SDLProperties.SDL_SetNumberProperty(properties, SDLDialog.SDL_PROP_FILE_DIALOG_NFILTERS_NUMBER, 1);
            PENDING.put(id, request);
            SDLDialog.SDL_ShowFileDialogWithProperties(SDLDialog.SDL_FILEDIALOG_OPENFILE, CallbackHolder.CALLBACK, id, properties);
        } catch (RuntimeException ex) {
            PENDING.remove(id);
            request.close();
            consumer.accept(new Result(null, ex.getMessage()));
        } finally {
            if (properties != 0) SDLProperties.SDL_DestroyProperties(properties);
        }
    }

    private static void onSelection(long id, long fileList, int filter) {
        Request request = PENDING.remove(id);
        if (request == null) return;
        // Copy SDL-owned strings before returning; callbacks can run outside the Minecraft thread.
        String error = fileList == 0L ? SDLError.SDL_GetError() : null;
        long file = fileList == 0L ? 0L : MemoryUtil.memGetAddress(fileList);
        Result result = new Result(file == 0L ? null : MemoryUtil.memUTF8(file), error);
        request.close();
        MainThreadTaskExecutor.executeInMainThread(() -> request.consumer.accept(result), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
    }

    static String toSdlPattern(@NotNull List<String> patterns) {
        if (patterns.isEmpty()) return "*";
        // SDL accepts semicolon-separated extensions, not glob expressions. Unsupported globs must not hide files.
        List<String> extensions = patterns.stream().map(pattern -> pattern.startsWith("*.") ? pattern.substring(2) : pattern).toList();
        if (extensions.stream().anyMatch(extension -> extension.isBlank() || extension.contains("*") || extension.contains("?") || extension.contains("/"))) return "*";
        return String.join(";", extensions);
    }

    public record Result(@Nullable String path, @Nullable String error) {

    }

    private static final class Request implements AutoCloseable {

        private final Consumer<Result> consumer;
        private final ByteBuffer name;
        private final ByteBuffer pattern;
        private final SDL_DialogFileFilter.Buffer filters;

        private Request(@Nullable String description, @NotNull List<String> patterns, @NotNull Consumer<Result> consumer) {
            this.consumer = consumer;
            this.name = MemoryUtil.memUTF8(description == null ? "Files" : description);
            this.pattern = MemoryUtil.memUTF8(toSdlPattern(patterns));
            this.filters = SDL_DialogFileFilter.calloc(1);
            this.filters.get(0).name(this.name).pattern(this.pattern);
        }

        @Override
        public void close() {
            this.filters.free();
            MemoryUtil.memFree(this.pattern);
            MemoryUtil.memFree(this.name);
        }

    }

}

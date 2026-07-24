package de.keksuccino.fancymenu.util.rendering.ui.cursor;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Owns the lifetime of GLFW cursor allocations without depending on GLFW itself.
 *
 * <p>Every allocation has its own state token instead of using the numeric native handle as its identity. Native
 * libraries may reuse a handle value after destruction, so a delayed task must become a no-op based on the old
 * allocation token rather than accidentally destroying a newer allocation with the same numeric value.</p>
 */
final class CursorHandleLifecycle {

    private final ThreadExecutor threadExecutor;
    private final NativeOperations nativeOperations;
    private final Consumer<Throwable> errorHandler;
    private final List<Handle> customHandles = new ArrayList<>();
    private final List<Handle> standardHandles = new ArrayList<>();
    private boolean shutdown;

    CursorHandleLifecycle(@NotNull ThreadExecutor threadExecutor, @NotNull NativeOperations nativeOperations, @NotNull Consumer<Throwable> errorHandler) {
        this.threadExecutor = threadExecutor;
        this.nativeOperations = nativeOperations;
        this.errorHandler = errorHandler;
    }

    @NotNull
    Handle trackCustom(long nativeHandle) {
        Handle handle;
        synchronized (this) {
            boolean accepted = !this.shutdown && nativeHandle != 0L;
            handle = new Handle(nativeHandle, accepted);
            if (accepted) this.customHandles.add(handle);
        }
        if (!handle.wasAccepted() && nativeHandle != 0L) this.destroy(handle);
        return handle;
    }

    void trackStandard(long nativeHandle) {
        if (nativeHandle == 0L) return;
        Handle rejectedHandle = null;
        synchronized (this) {
            if (this.shutdown) {
                rejectedHandle = new Handle(nativeHandle, false);
            } else {
                this.standardHandles.add(new Handle(nativeHandle, true));
            }
        }
        if (rejectedHandle != null) this.destroy(rejectedHandle);
    }

    void destroy(@NotNull Handle handle) {
        if (this.requestDestruction(handle)) this.executeDestruction(handle);
    }

    boolean requestDestruction(@NotNull Handle handle) {
        return handle.requestDestruction();
    }

    void executeDestruction(@NotNull Handle handle) {
        this.threadExecutor.execute(() -> this.destroyOnGlfwThread(handle));
    }

    /**
     * Closes all allocations on the GLFW thread. The production shutdown hook invokes this from Minecraft's render
     * thread before its window and GLFW are torn down. An unexpected off-thread caller is queued without blocking;
     * blocking here could deadlock against a client thread that has already started stopping.
     */
    void shutdown() {
        List<Handle> customSnapshot;
        List<Handle> standardSnapshot;
        synchronized (this) {
            if (this.shutdown) return;
            this.shutdown = true;
            customSnapshot = new ArrayList<>(this.customHandles);
            standardSnapshot = new ArrayList<>(this.standardHandles);
        }
        this.threadExecutor.execute(() -> this.shutdownOnGlfwThread(customSnapshot, standardSnapshot));
    }

    private void shutdownOnGlfwThread(@NotNull List<Handle> customSnapshot, @NotNull List<Handle> standardSnapshot) {
        if (!this.threadExecutor.isOnThread()) {
            this.errorHandler.accept(new IllegalStateException("GLFW cursor shutdown executed outside the GLFW thread!"));
            return;
        }
        try {
            this.nativeOperations.prepareForShutdown();
        } catch (Throwable throwable) {
            this.errorHandler.accept(throwable);
        }
        // Custom cursors are detached first; standard cursors remain a valid FancyMenu fallback until this point.
        customSnapshot.forEach(this::destroyOnGlfwThread);
        standardSnapshot.forEach(this::destroyOnGlfwThread);
    }

    private void destroyOnGlfwThread(@NotNull Handle handle) {
        if (!this.threadExecutor.isOnThread()) {
            this.errorHandler.accept(new IllegalStateException("GLFW cursor destruction executed outside the GLFW thread!"));
            return;
        }
        if (handle.nativeHandle() == 0L || !handle.claimDestruction()) return;
        try {
            this.nativeOperations.destroyCursor(handle.nativeHandle());
            handle.completeDestruction();
            synchronized (this) {
                this.customHandles.remove(handle);
                this.standardHandles.remove(handle);
            }
        } catch (Throwable throwable) {
            // A failed native call stays retryable for a later explicit request or the final GLFW teardown.
            handle.failDestruction();
            this.errorHandler.accept(throwable);
        }
    }

    interface ThreadExecutor {

        boolean isOnThread();

        void execute(@NotNull Runnable task);

    }

    interface NativeOperations {

        void prepareForShutdown();

        void destroyCursor(long nativeHandle);

    }

    static final class Handle {

        private static final int LIVE = 0;
        private static final int DESTRUCTION_REQUESTED = 1;
        private static final int DESTROYING = 2;
        private static final int DESTROYED = 3;

        private final long nativeHandle;
        private final boolean accepted;
        private final AtomicInteger state = new AtomicInteger(LIVE);

        private Handle(long nativeHandle, boolean accepted) {
            this.nativeHandle = nativeHandle;
            this.accepted = accepted;
        }

        long nativeHandle() {
            return this.nativeHandle;
        }

        boolean wasAccepted() {
            return this.accepted;
        }

        boolean isLive() {
            return this.accepted && this.state.get() == LIVE;
        }

        private boolean requestDestruction() {
            return this.state.compareAndSet(LIVE, DESTRUCTION_REQUESTED);
        }

        private boolean claimDestruction() {
            while (true) {
                int currentState = this.state.get();
                if (currentState == DESTROYING || currentState == DESTROYED) return false;
                if (this.state.compareAndSet(currentState, DESTROYING)) return true;
            }
        }

        private void completeDestruction() {
            this.state.set(DESTROYED);
        }

        private void failDestruction() {
            this.state.compareAndSet(DESTROYING, LIVE);
        }

    }

}

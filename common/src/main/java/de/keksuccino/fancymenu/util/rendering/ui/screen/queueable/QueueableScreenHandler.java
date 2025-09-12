package de.keksuccino.fancymenu.util.rendering.ui.screen.queueable;

import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class QueueableScreenHandler {

    private static final Queue<QueueableScreen> SCREEN_QUEUE = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean IS_PROCESSING = new AtomicBoolean(false);

    private static volatile QueueableScreen currentScreen = null;

    /**
     * Adds a QueueableScreen to the queue and processes it.
     * This method is thread-safe and can be called from any thread.
     * 
     * @param screen The QueueableScreen to add to the queue
     */
    public static void addToQueue(@NotNull QueueableScreen screen) {
        if (screen == null) {
            throw new IllegalArgumentException("Screen cannot be null!");
        }
        
        // Set the close callback for the screen
        screen.setCloseCallback(QueueableScreenHandler::onScreenClosed);
        
        // Add screen to the queue
        SCREEN_QUEUE.offer(screen);
        
        // Try to process the queue
        processQueue();
    }

    /**
     * Called when a QueueableScreen is closed.
     * This will trigger processing of the next screen in the queue.
     * 
     * @param closedScreen The screen that was closed
     */
    private static void onScreenClosed(@NotNull QueueableScreen closedScreen) {
        // Clear current screen if it matches the closed one
        if (currentScreen == closedScreen) {
            currentScreen = null;
            IS_PROCESSING.set(false);
        }
        
        // Process next screen in queue
        processQueue();
    }

    /**
     * Processes the queue and opens the next screen if no screen is currently being displayed.
     * This method ensures thread-safety and prevents multiple screens from being opened simultaneously.
     */
    private static void processQueue() {
        // Only process if we're not already processing a screen
        if (IS_PROCESSING.compareAndSet(false, true)) {
            QueueableScreen nextScreen = SCREEN_QUEUE.poll();
            
            if (nextScreen != null) {
                currentScreen = nextScreen;
                
                // Open the screen on the main thread
                if (Minecraft.getInstance().isSameThread()) {
                    openScreen(nextScreen);
                } else {
                    MainThreadTaskExecutor.executeInMainThread(() -> openScreen(nextScreen), MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                }
            } else {
                // No screens to process, reset the flag
                IS_PROCESSING.set(false);
            }
        }
    }

    /**
     * Opens a screen on the main thread.
     * This method should only be called from the main Minecraft thread.
     * 
     * @param screen The screen to open
     */
    private static void openScreen(@NotNull QueueableScreen screen) {
        try {
            Minecraft.getInstance().setScreen(screen);
        } catch (Exception e) {
            // If opening fails, reset state and try next screen
            e.printStackTrace();
            currentScreen = null;
            IS_PROCESSING.set(false);
            processQueue();
        }
    }

    /**
     * Clears all screens from the queue.
     * This will not close the currently displayed screen.
     */
    public static void clearQueue() {
        SCREEN_QUEUE.clear();
    }

    /**
     * Gets the number of screens currently in the queue (not including the current screen).
     * 
     * @return The number of screens waiting in the queue
     */
    public static int getQueueSize() {
        return SCREEN_QUEUE.size();
    }

    /**
     * Checks if there is currently a QueueableScreen being displayed.
     * 
     * @return true if a screen is currently being displayed, false otherwise
     */
    public static boolean isScreenActive() {
        return currentScreen != null && IS_PROCESSING.get();
    }

    /**
     * Gets the currently displayed QueueableScreen.
     * 
     * @return The current screen, or null if no screen is being displayed
     */
    @Nullable
    public static QueueableScreen getCurrentScreen() {
        return currentScreen;
    }

}

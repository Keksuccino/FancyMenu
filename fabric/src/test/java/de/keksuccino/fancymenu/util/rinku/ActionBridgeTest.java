package de.keksuccino.fancymenu.util.rinku;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionRegistry;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.network.chat.Component;
import org.cef.callback.CefQueryCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionBridgeTest {

    @AfterEach
    void clearMainThreadQueue() {
        MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
    }

    @Test
    void browserActionExecutesOnClientThreadBeforeSuccessCallback() throws Exception {
        RecordingAction action = new RecordingAction("browser_action_bridge_thread_test", Thread.currentThread());
        ActionRegistry.register(action);
        RecordingCallback callback = new RecordingCallback(action);

        assertTrue(queryFromBrowserThread("{\"type\":\"fancymenu_action\",\"action\":\"" + action.getIdentifier() + "\"}", callback));
        assertFalse(action.executed);
        assertNull(callback.successResponse);
        assertNull(callback.failureCode);

        List<Runnable> tasks = MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        assertEquals(1, tasks.size());
        tasks.get(0).run();

        assertTrue(action.executed);
        assertSame(Thread.currentThread(), action.asyncCheckThread);
        assertSame(Thread.currentThread(), action.executionThread);
        assertNotNull(callback.successResponse);
        assertTrue(callback.successObservedAfterAction);
        assertNull(callback.failureCode);
    }

    @Test
    void unknownActionFailsOnlyAfterClientThreadDispatch() throws Exception {
        RecordingCallback callback = new RecordingCallback(null);

        assertTrue(queryFromBrowserThread("{\"type\":\"fancymenu_action\",\"action\":\"missing_browser_action\"}", callback));
        assertNull(callback.successResponse);
        assertNull(callback.failureCode);

        List<Runnable> tasks = MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        assertEquals(1, tasks.size());
        tasks.get(0).run();

        assertNull(callback.successResponse);
        assertEquals(500, callback.failureCode);
        assertEquals("Failed to execute action", callback.failureMessage);
    }

    @Test
    void malformedActionRequestFailsWithoutClientThreadDispatch() {
        RecordingCallback callback = new RecordingCallback(null);

        boolean handled = ActionBridge.createMessageHandler().onQuery(null, null, 1L, "{\"type\":\"fancymenu_action\",\"action\":\"\"}", false, callback);

        assertTrue(handled);
        assertNull(callback.successResponse);
        assertEquals(400, callback.failureCode);
        assertEquals("Invalid action format", callback.failureMessage);
        assertTrue(MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK).isEmpty());
    }

    private static boolean queryFromBrowserThread(@NotNull String request, @NotNull RecordingCallback callback) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "test-cef-ui-thread"));
        Future<Boolean> result = executor.submit(() -> ActionBridge.createMessageHandler().onQuery(null, null, 1L, request, false, callback));
        try {
            return result.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static final class RecordingAction extends Action {

        private final Thread expectedExecutionThread;
        private volatile boolean executed;
        private volatile Thread asyncCheckThread;
        private volatile Thread executionThread;

        private RecordingAction(@NotNull String identifier, @NotNull Thread expectedExecutionThread) {
            super(identifier);
            this.expectedExecutionThread = expectedExecutionThread;
        }

        @Override
        public boolean checkAsync() {
            this.asyncCheckThread = Thread.currentThread();
            return this.asyncCheckThread == this.expectedExecutionThread;
        }

        @Override
        public boolean hasValue() {
            return false;
        }

        @Override
        public void execute(@Nullable String value) {
            this.executionThread = Thread.currentThread();
            this.executed = true;
        }

        @Override
        public @NotNull Component getDisplayName() {
            return Component.empty();
        }

        @Override
        public @NotNull Component getDescription() {
            return Component.empty();
        }

        @Override
        public @Nullable Component getValueDisplayName() {
            return null;
        }

        @Override
        public @Nullable String getValuePreset() {
            return null;
        }

    }

    private static final class RecordingCallback implements CefQueryCallback {

        @Nullable
        private final RecordingAction action;
        private volatile String successResponse;
        private volatile Integer failureCode;
        private volatile String failureMessage;
        private volatile boolean successObservedAfterAction;

        private RecordingCallback(@Nullable RecordingAction action) {
            this.action = action;
        }

        @Override
        public void success(String response) {
            this.successResponse = response;
            this.successObservedAfterAction = (this.action != null) && this.action.executed;
        }

        @Override
        public void failure(int errorCode, String errorMessage) {
            this.failureCode = errorCode;
            this.failureMessage = errorMessage;
        }

    }

}

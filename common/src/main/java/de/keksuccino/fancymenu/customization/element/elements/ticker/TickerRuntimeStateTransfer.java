package de.keksuccino.fancymenu.customization.element.elements.ticker;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Carries ticker timing state only through a screen initialization caused synchronously by a ticker action.
 * Continuations are owned by the lexical action scope, so neither screens nor ticker state survive after the action
 * returns. The exact target identity still matters because equal screen instances are independent initialization runs.
 */
final class TickerRuntimeStateTransfer {

    private static final Object GENERATION_LOCK = new Object();
    private static final ThreadLocal<Deque<ExecutionContext>> ACTIVE_EXECUTIONS = new ThreadLocal<>();
    /** Invalidates action scopes that are active on other ticker threads when FancyMenu reloads. */
    private static long generation;

    private TickerRuntimeStateTransfer() {
    }

    @NotNull
    static ExecutionScope begin(@Nullable String sourceScreenIdentifier, @Nullable ScreenCustomizationLayer sourceLayer, @NotNull TickerElement executionSource) {
        return begin(new ExecutionContext(sourceScreenIdentifier, sourceLayer, null, executionSource, getGeneration()));
    }

    /** Test seam for exercising state matching without constructing and globally registering a customization layer. */
    @NotNull
    static ExecutionScope begin(@Nullable String sourceScreenIdentifier, @NotNull Collection<TickerElement> sourceTickers, @NotNull TickerElement executionSource) {
        return begin(new ExecutionContext(sourceScreenIdentifier, null, sourceTickers, executionSource, getGeneration()));
    }

    @NotNull
    private static ExecutionScope begin(@NotNull ExecutionContext context) {
        Deque<ExecutionContext> contexts = ACTIVE_EXECUTIONS.get();
        if (contexts == null) {
            contexts = new ArrayDeque<>();
            ACTIVE_EXECUTIONS.set(contexts);
        }
        contexts.push(context);
        return new ExecutionScope(context);
    }

    private static long getGeneration() {
        long contextGeneration;
        synchronized (GENERATION_LOCK) {
            contextGeneration = generation;
        }
        return contextGeneration;
    }

    static void bindTarget(@NotNull Object targetScreen, @Nullable String targetScreenIdentifier) {
        ExecutionContext context = getActiveContext();
        if ((context == null) || !Objects.equals(context.sourceScreenIdentifier, targetScreenIdentifier)) {
            return;
        }
        synchronized (GENERATION_LOCK) {
            if (context.generation != generation || context.targetStates.containsKey(targetScreen)) {
                return;
            }
            Map<RuntimeKey, RuntimeState> states = new LinkedHashMap<>();
            if (context.sourceLayer != null) {
                // A screen-changing action can run inside event dispatch, so use a stable snapshot of the old layer.
                for (AbstractElement element : context.sourceLayer.allElements.toArray(AbstractElement[]::new)) {
                    if (element instanceof TickerElement ticker) {
                        putState(states, ticker, ticker == context.executionSource);
                    }
                }
            } else if (context.sourceTickers != null) {
                for (TickerElement ticker : context.sourceTickers) {
                    putState(states, ticker, ticker == context.executionSource);
                }
            }
            // The source can already have been detached from the layer by an earlier nested initialization.
            putState(states, context.executionSource, true);
            context.targetStates.put(targetScreen, states);
            context.suspendCausalSource();
        }
    }

    @Nullable
    static RuntimeState take(@NotNull Object targetScreen, @NotNull RuntimeKey key) {
        ExecutionContext context = getActiveContext();
        if (context == null) {
            return null;
        }
        synchronized (GENERATION_LOCK) {
            if (context.generation != generation) {
                return null;
            }
            Map<RuntimeKey, RuntimeState> states = context.targetStates.get(targetScreen);
            if (states == null) {
                return null;
            }
            RuntimeState state = states.remove(key);
            if (states.isEmpty()) {
                context.targetStates.remove(targetScreen);
            }
            return state;
        }
    }

    static void finishInitialization(@NotNull Object targetScreen) {
        ExecutionContext context = getActiveContext();
        if (context == null) {
            return;
        }
        synchronized (GENERATION_LOCK) {
            if (context.generation == generation) {
                context.targetStates.remove(targetScreen);
            }
        }
    }

    static void clear() {
        synchronized (GENERATION_LOCK) {
            generation++;
        }
        ACTIVE_EXECUTIONS.remove();
    }

    static int pendingTargetCount() {
        ExecutionContext context = getActiveContext();
        if (context == null) {
            return 0;
        }
        synchronized (GENERATION_LOCK) {
            return (context.generation == generation) ? context.targetStates.size() : 0;
        }
    }

    private static void putState(@NotNull Map<RuntimeKey, RuntimeState> states, @NotNull TickerElement ticker, boolean executionSource) {
        RuntimeKey key = ticker.createRuntimeStateKey();
        if (key != null) {
            states.put(key, ticker.createRuntimeState(executionSource));
        }
    }

    private static void end(@NotNull ExecutionContext context) {
        Deque<ExecutionContext> contexts = ACTIVE_EXECUTIONS.get();
        if (contexts == null) {
            return;
        }
        if (contexts.peek() == context) {
            contexts.pop();
        } else {
            contexts.removeFirstOccurrence(context);
        }
        if (contexts.isEmpty()) {
            ACTIVE_EXECUTIONS.remove();
        }
    }

    @Nullable
    private static ExecutionContext getActiveContext() {
        Deque<ExecutionContext> contexts = ACTIVE_EXECUTIONS.get();
        return (contexts != null) ? contexts.peek() : null;
    }

    record RuntimeKey(@NotNull String layoutIdentifier, @NotNull String elementIdentifier, @NotNull String rawTickDelay, @NotNull String tickMode, boolean async, @NotNull String executableBlockIdentifier) {
    }

    record RuntimeState(boolean ticked, long lastTick, boolean suspended) {
    }

    private static final class ExecutionContext {

        @Nullable
        private final String sourceScreenIdentifier;
        @Nullable
        private final ScreenCustomizationLayer sourceLayer;
        @Nullable
        private final Collection<TickerElement> sourceTickers;
        @NotNull
        private final TickerElement executionSource;
        @NotNull
        private final IdentityHashMap<Object, Map<RuntimeKey, RuntimeState>> targetStates = new IdentityHashMap<>();
        private final long generation;

        private ExecutionContext(@Nullable String sourceScreenIdentifier, @Nullable ScreenCustomizationLayer sourceLayer, @Nullable Collection<TickerElement> sourceTickers, @NotNull TickerElement executionSource, long generation) {
            this.sourceScreenIdentifier = sourceScreenIdentifier;
            this.sourceLayer = sourceLayer;
            this.sourceTickers = sourceTickers;
            this.executionSource = executionSource;
            this.generation = generation;
        }

        private void suspendCausalSource() {
            if (this.executionSource.isImmediateNormalExecutionSource()) {
                this.executionSource.suspendAfterImmediateSameScreenReplacement();
            }
        }

    }

    static final class ExecutionScope implements AutoCloseable {

        private final ExecutionContext context;
        private boolean closed;

        private ExecutionScope(@NotNull ExecutionContext context) {
            this.context = context;
        }

        @Override
        public void close() {
            if (!this.closed) {
                this.closed = true;
                TickerRuntimeStateTransfer.end(this.context);
            }
        }

    }

}

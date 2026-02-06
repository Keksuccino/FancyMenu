package de.keksuccino.fancymenu.util.mcp;

import com.google.gson.JsonObject;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.ui.ActionScriptEditorWindowBody;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.listener.ListenerHandler;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import de.keksuccino.fancymenu.customization.scheduler.SchedulerHandler;
import de.keksuccino.fancymenu.customization.scheduler.SchedulerInstance;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

final class FancyMenuMcpActionEditorBridge {

    private static @Nullable PiPWindow activeWindow;
    private static @Nullable ActionScriptTarget activeTarget;

    private FancyMenuMcpActionEditorBridge() {
    }

    static synchronized boolean openForTarget(@NotNull ActionScriptTarget target) {
        GenericExecutableBlock script = getTargetScript(target);
        if (script == null) {
            return false;
        }
        closeActiveInternal();
        ActionScriptEditorWindowBody body = new ActionScriptEditorWindowBody(script, updated -> {
            if (updated != null) {
                setTargetScript(target, updated);
            }
        });
        PiPWindow window = ActionScriptEditorWindowBody.openInWindow(body);
        window.addCloseCallback(() -> {
            synchronized (FancyMenuMcpActionEditorBridge.class) {
                if (activeWindow == window) {
                    activeWindow = null;
                    activeTarget = null;
                }
            }
        });
        activeWindow = window;
        activeTarget = target;
        return true;
    }

    static synchronized boolean setScriptAndRefresh(@NotNull ActionScriptTarget target, @NotNull GenericExecutableBlock script, boolean keepEditorVisible) {
        if (!setTargetScript(target, script)) {
            return false;
        }
        if (keepEditorVisible) {
            return openForTarget(target);
        }
        return true;
    }

    static synchronized @Nullable GenericExecutableBlock getScript(@NotNull ActionScriptTarget target) {
        GenericExecutableBlock script = getTargetScript(target);
        return script != null ? script.copy(false) : null;
    }

    static synchronized void closeActive() {
        closeActiveInternal();
        activeWindow = null;
        activeTarget = null;
    }

    static synchronized @Nullable JsonObject getActiveEditorInfo() {
        if (activeTarget == null) {
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("target_type", activeTarget.type().name().toLowerCase());
        json.addProperty("target_id", activeTarget.targetIdentifier());
        json.addProperty("window_open", activeWindow != null);
        return json;
    }

    private static void closeActiveInternal() {
        if (activeWindow != null) {
            try {
                activeWindow.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static @Nullable GenericExecutableBlock getTargetScript(@NotNull ActionScriptTarget target) {
        return switch (target.type()) {
            case LAYOUT_OPEN_SCRIPT -> {
                Layout layout = resolveLayout(target.targetIdentifier());
                if (layout == null) {
                    yield null;
                }
                yield layout.openScreenExecutableBlocks.isEmpty() ? new GenericExecutableBlock() : layout.openScreenExecutableBlocks.getFirst().copy(false);
            }
            case LAYOUT_CLOSE_SCRIPT -> {
                Layout layout = resolveLayout(target.targetIdentifier());
                if (layout == null) {
                    yield null;
                }
                yield layout.closeScreenExecutableBlocks.isEmpty() ? new GenericExecutableBlock() : layout.closeScreenExecutableBlocks.getFirst().copy(false);
            }
            case LISTENER_SCRIPT -> {
                ListenerInstance instance = ListenerHandler.getInstance(target.targetIdentifier());
                yield (instance != null) ? instance.getActionScript().copy(false) : null;
            }
            case SCHEDULER_SCRIPT -> {
                SchedulerInstance instance = SchedulerHandler.getInstance(target.targetIdentifier());
                yield (instance != null) ? instance.getActionScript().copy(false) : null;
            }
        };
    }

    private static boolean setTargetScript(@NotNull ActionScriptTarget target, @NotNull GenericExecutableBlock script) {
        return switch (target.type()) {
            case LAYOUT_OPEN_SCRIPT -> {
                Layout layout = resolveLayout(target.targetIdentifier());
                if (layout == null) {
                    yield false;
                }
                layout.openScreenExecutableBlocks.clear();
                layout.openScreenExecutableBlocks.add(script.copy(false));
                layout.saveToFileIfPossible();
                LayoutHandler.reloadLayouts();
                yield true;
            }
            case LAYOUT_CLOSE_SCRIPT -> {
                Layout layout = resolveLayout(target.targetIdentifier());
                if (layout == null) {
                    yield false;
                }
                layout.closeScreenExecutableBlocks.clear();
                layout.closeScreenExecutableBlocks.add(script.copy(false));
                layout.saveToFileIfPossible();
                LayoutHandler.reloadLayouts();
                yield true;
            }
            case LISTENER_SCRIPT -> {
                ListenerInstance instance = ListenerHandler.getInstance(target.targetIdentifier());
                if (instance == null) {
                    yield false;
                }
                instance.setActionScript(script.copy(false));
                ListenerHandler.syncChanges();
                yield true;
            }
            case SCHEDULER_SCRIPT -> {
                SchedulerInstance instance = SchedulerHandler.getInstance(target.targetIdentifier());
                if (instance == null) {
                    yield false;
                }
                instance.setActionScript(script.copy(false));
                SchedulerHandler.syncChanges();
                yield true;
            }
        };
    }

    private static @Nullable Layout resolveLayout(@NotNull String identifier) {
        String normalizedIdentifier = identifier.replace("\\", "/");
        for (Layout layout : LayoutHandler.getAllLayouts()) {
            if (Objects.equals(layout.runtimeLayoutIdentifier, identifier)) {
                return layout;
            }
            if (layout.layoutFile != null && Objects.equals(layout.layoutFile.getAbsolutePath().replace("\\", "/"), normalizedIdentifier)) {
                return layout;
            }
            if (layout.getLayoutName().equalsIgnoreCase(identifier)) {
                return layout;
            }
        }
        return null;
    }

    record ActionScriptTarget(@NotNull Type type, @NotNull String targetIdentifier) {
        enum Type {
            LAYOUT_OPEN_SCRIPT,
            LAYOUT_CLOSE_SCRIPT,
            LISTENER_SCRIPT,
            SCHEDULER_SCRIPT
        }
    }
}

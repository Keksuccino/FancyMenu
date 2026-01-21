package de.keksuccino.fancymenu.customization.action;

import de.keksuccino.fancymenu.customization.action.ui.AsyncActionErrorScreen;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.screen.queueable.QueueableScreenHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Objects;


/**
 * An action to use with buttons, tickers, etc.<br>
 * Needs to get registered to the {@link ActionRegistry} at mod init.
 */
public abstract class Action {

    public static final Action EMPTY = new Action("empty") {@Override public boolean hasValue() {return false;}@Override public void execute(@Nullable String value) {}@Override public @NotNull Component getActionDisplayName() {return Component.empty();}@Override public @NotNull Component[] getActionDescription() {return new Component[0];}@Override public @Nullable Component getValueDisplayName() {return null;}@Override public @Nullable String getValueExample() {return null;}};

    private final String identifier;
    @Nullable
    protected ActionInstance currentInstance = null;
    protected volatile boolean asyncErrorShown = false;

    public Action(@NotNull String uniqueIdentifier) {
        this.identifier = Objects.requireNonNull(uniqueIdentifier);
    }

    public boolean isDeprecated() {
        return false;
    }

    /**
     * This lets you control if it should be possible to add a new instance of this action type to a layout.<br>
     * For example, by using this you can control if the action should only be available for specific types of {@link Screen}s.
     */
    public boolean shouldShowUpInEditorActionMenu(@NotNull LayoutEditorScreen editor) {
        return true;
    }

    /**
     * If this action has a value.
     */
    public abstract boolean hasValue();

    /**
     * @param value The value that was set to the action or NULL if this action has no value.
     */
    public abstract void execute(@Nullable String value);

    @NotNull
    public abstract Component getActionDisplayName();

    @NotNull
    public abstract Component[] getActionDescription();

    @Nullable
    public abstract Component getValueDisplayName();

    /**
     * An example of how the value of this action should look like.
     */
    @Nullable
    public abstract String getValueExample();

    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    @Nullable
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

    /**
     * Called when editing the value of an {@link ActionInstance}.<br>
     * The value of the given {@code instance} needs to get updated during the editing process.<br><br>
     *
     * This method should only open {@link PiPWindow}s and NEVER real {@link Screen}s!
     *
     * @param instance The {@link ActionInstance} to edit.
     * @param onEditingCompleted Always needs to get called when the editing was completed without canceling it. Should get called AFTER updating the instance's value.
     * @param onEditingCanceled Always needs to get called when the editing got canceled without completing it.
     */
    public void editValue(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        if (this.hasValue()) {
            Component title = (this.getValueDisplayName() != null) ? this.getValueDisplayName() : Component.empty();
            TextEditorWindowBody s = new TextEditorWindowBody(title, null, editedValue -> {
                if (editedValue != null) {
                    String old = instance.value;
                    instance.value = editedValue;
                    onEditingCompleted.accept(instance, old, editedValue);
                } else {
                    onEditingCanceled.accept(instance);
                }
            });
            List<TextEditorFormattingRule> formattingRules = this.getValueFormattingRules();
            if (formattingRules != null) s.formattingRules.addAll(formattingRules);
            s.setMultilineMode(false);
            if (instance.value != null) {
                s.setText(instance.value);
            } else {
                s.setText(this.getValueExample());
            }
            Dialogs.openGeneric(s, title, null, TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT);
        }
    }

    @ApiStatus.Internal
    public void editValueInternal(@NotNull ActionInstance instance, @NotNull Action.ActionEditingCompletedFeedback onEditingCompleted, @NotNull Action.ActionEditingCanceledFeedback onEditingCanceled) {
        ScreenUtils.blockSetScreenCalls(true);
        this.editValue(instance, onEditingCompleted, onEditingCanceled);
        ScreenUtils.blockSetScreenCalls(false);
    }

    public boolean canRunAsync() {
        return true;
    }

    public boolean checkAsync() {
        boolean sameThread = Minecraft.getInstance().isSameThread();
        if (!sameThread && !this.canRunAsync() && !this.asyncErrorShown) {
            this.asyncErrorShown = true;
            QueueableScreenHandler.addToQueue(new AsyncActionErrorScreen(this.getActionDisplayName()));
        }
        return this.canRunAsync() || sameThread; // should run action
    }

    @FunctionalInterface
    public interface ActionEditingCompletedFeedback {
        void accept(@NotNull ActionInstance instance, @Nullable String oldValue, @NotNull String newValue);
    }

    @FunctionalInterface
    public interface ActionEditingCanceledFeedback {
        void accept(@NotNull ActionInstance instance);
    }

}

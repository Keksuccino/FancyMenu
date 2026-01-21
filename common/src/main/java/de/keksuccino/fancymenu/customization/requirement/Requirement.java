package de.keksuccino.fancymenu.customization.requirement;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.customization.requirement.ui.AsyncRequirementErrorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.queueable.QueueableScreenHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringBuilderScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.konkrete.input.CharacterFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A LoadingRequirement.<br><br>
 *
 * Needs to be registered to the {@link RequirementRegistry} on mod init.
 */
public abstract class Requirement {

    protected final String identifier;
    private RequirementInstance currentInstance;
    protected volatile boolean asyncErrorShown = false;

    /**
     * The identifier needs to be unique! It is not possible to register two requirements with the same identifier.
     */
    public Requirement(@NotNull String uniqueRequirementIdentifier) {
        if (!CharacterFilter.getBasicFilenameCharacterFilter().isAllowed(uniqueRequirementIdentifier)) {
            throw new UnsupportedCharsetException("[FANCYMENU] Illegal characters in LoadingRequirement name: " + uniqueRequirementIdentifier);
        }
        this.identifier = Objects.requireNonNull(uniqueRequirementIdentifier);
    }

    /**
     * If the requirement has a value.<br><br>
     *
     * Keep in mind that the value string will be checked for placeholders, so don't mess with similar JSON-like parsing in it.
     */
    public abstract boolean hasValue();

    /**
     * The magic happens here. This is where you put the actual logic of the requirement.<br>
     * It checks if the requirement is met.<br><br>
     *
     * <b>For example:</b> If your requirement checks if the window is fullscreen, and it IS currently in fullscreen, then return TRUE here.<br><br>
     *
     * Keep in mind that placeholders get replaced in the value string, so don't mess with similar JSON-like parsing in it.<br>
     * Placeholders got replaced already at this point, so you get the final value string and don't need to care about raw placeholders here.
     *
     * @param value The value of the requirement, if it has one. Placeholders got replaced already. This is NULL if the requirement has no value!
     */
    public abstract boolean isRequirementMet(@Nullable String value);

    /**
     * The display name of the requirement.<br>
     * It is shown in the requirement options of the layout editor.
     */
    @NotNull
    public abstract Component getDisplayName();

    /**
     * The description of the requirement.<br>
     * It is shown in the requirement options of the layout editor.<br><br>
     *
     * Every entry in the returned list counts as a text line.
     */
    @Nullable
    public abstract Component getDescription();

    /**
     * The name of the category this requirement should be in.<br>
     * Requirements don't need to be in a category so if you don't want that, return NULL here.
     */
    @Nullable
    public abstract String getCategory();

    /**
     * The display name of the VALUE of the requirement, if it has one.<br>
     * It is shown in the requirement options of the layout editor.<br><br>
     *
     * Return NULL here if the requirement has no value.
     */
    @Nullable
    public abstract Component getValueDisplayName();

    /**
     * The preset/example of the value, if it has one.<br>
     * It is shown in the value input field of the requirement options of the layout editor.<br><br>
     *
     * Keep in mind that the value string will be checked for placeholders, so don't mess with similar JSON-like parsing here.<br><br>
     *
     * Return NULL here if the requirement has no value.
     */
    @Nullable
    public abstract String getValuePreset();

    /**
     * This returns a list with NEW instances of formatting rules used to format the value string in the {@link TextEditorWindowBody}.<br><br>
     *
     * Formatting rules are not mandatory, so if you don't want to use them, return NULL here.<br>
     * Same applies for when the requirement has no value.
     *
     * @return A list with formatting rules used for editing the requirement value in the {@link TextEditorWindowBody}.
     */
    @Nullable
    public abstract List<TextEditorFormattingRule> getValueFormattingRules();

    /**
     * Called when editing the value of an {@link RequirementInstance}.<br>
     * The value of the given {@code instance} needs to get updated during the editing process.<br><br>
     *
     * This method should only open {@link PiPWindow}s and NEVER real {@link Screen}s!
     *
     * @param instance The {@link RequirementInstance} to edit.
     * @param onEditingCompleted Always needs to get called when the editing was completed without canceling it. Should get called AFTER updating the instance's value.
     * @param onEditingCanceled Always needs to get called when the editing got canceled without completing it.
     */
    public void editValue(@NotNull RequirementInstance instance, @NotNull RequirementEditingCompletedFeedback onEditingCompleted, @NotNull RequirementEditingCanceledFeedback onEditingCanceled) {
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
                s.setText(this.getValuePreset());
            }
            Dialogs.openGeneric(s, title, null, TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT);
        }
    }

    @NotNull
    protected static Runnable openRequirementValueEditor(@NotNull Screen editorScreen, @NotNull Runnable onWindowClosedExternally) {
        PiPWindow window = new PiPWindow(editorScreen.getTitle())
                .setScreen(editorScreen)
                .setForceFancyMenuUiScale(true)
                .setAlwaysOnTop(true)
                .setBlockMinecraftScreenInputs(true)
                .setForceFocus(true)
                .setMinSize(TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT)
                .setSize(TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
        window.addCloseCallback(onWindowClosedExternally);
        return window::close;
    }

    public static abstract class RequirementValueEditScreen extends StringBuilderScreen {

        protected RequirementValueEditScreen(@NotNull Component title, @NotNull Consumer<String> callback) {
            super(title, callback);
        }

        @Override
        protected void renderTitle(@NotNull GuiGraphics graphics) {
            if (PiPWindowHandler.INSTANCE.isScreenRenderActive()) {
                return;
            }
            super.renderTitle(graphics);
        }

    }

    /**
     * The identifier of the requirement.
     */
    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    /**
     * This lets you control if it should be possible to add a new instance of this requirement type to a layout.<br>
     * For example, by using this you can control if the requirement should only be available for specific types of {@link Screen}s.
     */
    public boolean shouldShowUpInEditorRequirementMenu(@NotNull LayoutEditorScreen editor) {
        return true;
    }

    public void setCurrentInstance(@NotNull RequirementInstance instance) {
        this.currentInstance = instance;
    }

    @NotNull
    public RequirementInstance getCurrentInstance() {
        return this.currentInstance;
    }

    public boolean canRunAsync() {
        return true;
    }

    public boolean checkAsync() {
        boolean sameThread = Minecraft.getInstance().isSameThread();
        if (!sameThread && !this.canRunAsync() && !this.asyncErrorShown) {
            this.asyncErrorShown = true;
            QueueableScreenHandler.addToQueue(new AsyncRequirementErrorScreen(this.getDisplayName()));
        }
        return this.canRunAsync() || sameThread; // should check requirement
    }

    @FunctionalInterface
    public interface RequirementEditingCompletedFeedback {
        void accept(@NotNull RequirementInstance instance, @Nullable String oldValue, @NotNull String newValue);
    }

    @FunctionalInterface
    public interface RequirementEditingCanceledFeedback {
        void accept(@NotNull RequirementInstance instance);
    }

}

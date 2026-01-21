package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.ILocalizedValueCycle;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringBuilderScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class IsActiveCursorTypeRequirement extends Requirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsActiveCursorTypeRequirement() {
        super("is_active_cursor_type");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        try {
            if ((value == null) || value.trim().isEmpty()) return false;
            CursorType type = CursorType.getByKey(value.trim().toLowerCase());
            if (type == null) return false;
            int activeShape = CursorHandler.getActiveStandardCursorShape();
            if (type.shape == GLFW.GLFW_ARROW_CURSOR) {
                // Treat unknown cursor handle (0L) as arrow cursor by GLFW spec; CursorHandler already maps 0L -> ARROW
                return activeShape == GLFW.GLFW_ARROW_CURSOR;
            }
            return activeShape == type.shape;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle '" + this.getIdentifier() + "' loading requirement!", ex);
            return false;
        }
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.requirements.is_active_cursor_type");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.is_active_cursor_type.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.gui");
    }

    @Override
    public String getValueDisplayName() {
        return I18n.get("fancymenu.requirements.is_active_cursor_type.value_name");
    }

    @Override
    public String getValuePreset() {
        return CursorType.NORMAL.key;
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull RequirementInstance requirementInstance) {
        boolean[] handled = {false};
        final Runnable[] closeAction = new Runnable[] {() -> {}};
        IsActiveCursorTypeValueConfigScreen s = new IsActiveCursorTypeValueConfigScreen(
                Objects.requireNonNullElse(requirementInstance.value, this.getValuePreset()),
                callback -> {
                    if (handled[0]) {
                        return;
                    }
                    handled[0] = true;
                    if (callback != null) {
                        requirementInstance.value = callback;
                    }
                    closeAction[0].run();
                }
        );
        closeAction[0] = Requirement.openRequirementValueEditor(parentScreen, s, () -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
        });
    }

    public static class IsActiveCursorTypeValueConfigScreen extends StringBuilderScreen {

        @NotNull
        protected CursorType cursorType = CursorType.NORMAL;

        protected IsActiveCursorTypeValueConfigScreen(@Nullable String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.elements.requirements.edit_value"), callback);
            if (value != null) {
                CursorType t = CursorType.getByKey(value.trim().toLowerCase());
                if (t != null) {
                    this.cursorType = t;
                }
            }
        }

        @Override
        protected void initCells() {

            this.addSpacerCell(20);

            ILocalizedValueCycle<CursorType> cursorCycle = CommonCycles.cycleOrangeValue(
                    "fancymenu.requirements.is_active_cursor_type.value_name",
                    Arrays.asList(CursorType.values()),
                    this.cursorType
            ).setValueNameSupplier(type -> {
                if (type == CursorType.POINTING_HAND) return I18n.get("fancymenu.requirements.is_active_cursor_type.value.pointing_hand");
                if (type == CursorType.WRITING) return I18n.get("fancymenu.requirements.is_active_cursor_type.value.writing");
                if (type == CursorType.CROSSHAIR) return I18n.get("fancymenu.requirements.is_active_cursor_type.value.crosshair");
                if (type == CursorType.RESIZE_HORIZONTAL) return I18n.get("fancymenu.requirements.is_active_cursor_type.value.resize_horizontal");
                if (type == CursorType.RESIZE_VERTICAL) return I18n.get("fancymenu.requirements.is_active_cursor_type.value.resize_vertical");
                if (type == CursorType.RESIZE_NWSE) return I18n.get("fancymenu.requirements.is_active_cursor_type.value.resize_nwse");
                if (type == CursorType.RESIZE_NESW) return I18n.get("fancymenu.requirements.is_active_cursor_type.value.resize_nesw");
                if (type == CursorType.RESIZE_ALL) return I18n.get("fancymenu.requirements.is_active_cursor_type.value.resize_all");
                if (type == CursorType.NOT_ALLOWED) return I18n.get("fancymenu.requirements.is_active_cursor_type.value.not_allowed");
                return I18n.get("fancymenu.requirements.is_active_cursor_type.value.normal");
            });

            this.addCycleButtonCell(cursorCycle, true, (value, button) -> this.cursorType = value);

            this.addSpacerCell(20);

        }

        @Override
        public @NotNull String buildString() {
            return this.cursorType.key;
        }

    }

    public enum CursorType {

        NORMAL("normal", GLFW.GLFW_ARROW_CURSOR),
        WRITING("writing", GLFW.GLFW_IBEAM_CURSOR),
        CROSSHAIR("crosshair", GLFW.GLFW_CROSSHAIR_CURSOR),
        POINTING_HAND("pointing_hand", GLFW.GLFW_POINTING_HAND_CURSOR),
        RESIZE_HORIZONTAL("resize_horizontal", GLFW.GLFW_RESIZE_EW_CURSOR),
        RESIZE_VERTICAL("resize_vertical", GLFW.GLFW_RESIZE_NS_CURSOR),
        RESIZE_NWSE("resize_nwse", GLFW.GLFW_RESIZE_NWSE_CURSOR),
        RESIZE_NESW("resize_nesw", GLFW.GLFW_RESIZE_NESW_CURSOR),
        RESIZE_ALL("resize_all", GLFW.GLFW_RESIZE_ALL_CURSOR),
        NOT_ALLOWED("not_allowed", GLFW.GLFW_NOT_ALLOWED_CURSOR);

        public final String key;
        public final int shape;

        CursorType(@NotNull String key, int shape) {
            this.key = key;
            this.shape = shape;
        }

        @Nullable
        public static CursorType getByKey(@NotNull String key) {
            for (CursorType t : values()) {
                if (t.key.equals(key)) return t;
            }
            return null;
        }

    }

}

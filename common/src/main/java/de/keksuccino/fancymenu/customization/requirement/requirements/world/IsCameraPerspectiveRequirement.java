package de.keksuccino.fancymenu.customization.requirement.requirements.world;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.ILocalizedValueCycle;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class IsCameraPerspectiveRequirement extends Requirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsCameraPerspectiveRequirement() {
        super("is_camera_perspective");
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
            Perspective perspective = Perspective.getByKey(value.trim().toLowerCase());
            if (perspective == null) return false;
            return Minecraft.getInstance().options.getCameraType() == perspective.type;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle '" + this.getIdentifier() + "' loading requirement!", ex);
            return false;
        }
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.requirements.is_camera_perspective");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.is_camera_perspective.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.world");
    }

    @Override
    public String getValueDisplayName() {
        return I18n.get("fancymenu.requirements.is_camera_perspective.value_name");
    }

    @Override
    public String getValuePreset() {
        return Perspective.FIRST_PERSON.key;
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull RequirementInstance requirementInstance) {
        boolean[] handled = {false};
        final Runnable[] closeAction = new Runnable[] {() -> {}};
        IsCameraPerspectiveValueConfigScreen s = new IsCameraPerspectiveValueConfigScreen(Objects.requireNonNullElse(requirementInstance.value, this.getValuePreset()), callback -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            if (callback != null) {
                requirementInstance.value = callback;
            }
            closeAction[0].run();
        });
        closeAction[0] = Requirement.openRequirementValueEditor(parentScreen, s, () -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
        });
    }

    public static class IsCameraPerspectiveValueConfigScreen extends Requirement.RequirementValueEditScreen {

        @NotNull
        protected Perspective perspective = Perspective.FIRST_PERSON;

        protected IsCameraPerspectiveValueConfigScreen(@Nullable String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.elements.requirements.edit_value"), callback);
            if (value != null) {
                Perspective p = Perspective.getByKey(value.trim().toLowerCase());
                if (p != null) {
                    this.perspective = p;
                }
            }
        }

        @Override
        protected void initCells() {

            this.addSpacerCell(20);

            ILocalizedValueCycle<Perspective> perspectiveCycle = CommonCycles.cycleOrangeValue(
                    "fancymenu.requirements.is_camera_perspective.value_name",
                    Arrays.asList(Perspective.values()),
                    this.perspective
            ).setValueNameSupplier(perspective -> {
                if (perspective == Perspective.THIRD_PERSON_BACK) {
                    return I18n.get("fancymenu.requirements.is_camera_perspective.value.third_person_back");
                }
                if (perspective == Perspective.THIRD_PERSON_FRONT) {
                    return I18n.get("fancymenu.requirements.is_camera_perspective.value.third_person_front");
                }
                return I18n.get("fancymenu.requirements.is_camera_perspective.value.first_person");
            });

            this.addCycleButtonCell(perspectiveCycle, true, (value, button) -> this.perspective = value);

            this.addSpacerCell(20);

        }

        @Override
        public @NotNull String buildString() {
            return this.perspective.key;
        }

    }

    public enum Perspective {

        FIRST_PERSON("first_person", CameraType.FIRST_PERSON),
        THIRD_PERSON_BACK("third_person_back", CameraType.THIRD_PERSON_BACK),
        THIRD_PERSON_FRONT("third_person_front", CameraType.THIRD_PERSON_FRONT);

        public final String key;
        public final CameraType type;

        Perspective(@NotNull String key, @NotNull CameraType type) {
            this.key = key;
            this.type = type;
        }

        @Nullable
        public static Perspective getByKey(@NotNull String key) {
            for (Perspective p : values()) {
                if (p.key.equals(key)) return p;
            }
            return null;
        }

    }

}

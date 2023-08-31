package de.keksuccino.fancymenu.customization.loadingrequirement.requirements;

import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementInstance;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.ILocalizedValueCycle;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringBuilderScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class IsNumberRequirement extends LoadingRequirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsNumberRequirement() {
        super("fancymenu_visibility_requirement_is_number");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        //VALUE EXAMPLE:
        //["mode":"...","number":"...","compare_with":"..."]$,["mode":"...","number":"...","compare_with":"..."]$

        if (value != null) {
            List<String> secStrings = getSections(value);
            if (secStrings.isEmpty()) {
                return false;
            }
            boolean b = true;
            for (String s : secStrings) {
                if (!isSectionMet(parseSection(s))) {
                    b = false;
                }
            }
            return b;
        }

        return false;

    }

    private static boolean isSectionMet(List<String> section) {
        if (!section.isEmpty()) {
            String mode = section.get(0);
            String number = section.get(1);
            String compareWith = section.get(2);
            if (MathUtils.isDouble(number) && MathUtils.isDouble(compareWith)) {
                double num = Double.parseDouble(number);
                double comp = Double.parseDouble(compareWith);
                if (mode.equals("equals")) {
                    return (num == comp);
                } else if (mode.equals("bigger-than")) {
                    return (num > comp);
                } else if (mode.equals("smaller-than")) {
                    return (num < comp);
                } else if (mode.equals("bigger-than-or-equals")) {
                    return (num >= comp);
                } else if (mode.equals("smaller-than-or-equals")) {
                    return (num <= comp);
                }
            }
        }
        return false;
    }

    /**
     * index 0 = mode
     * index 1 = number
     * index 2 = compare_with
     */
    private static List<String> parseSection(String section) {
        List<String> l = new ArrayList<>();
        int currentIndex = 0;
        int currentStartIndex = 0;
        String mode = null;
        String number = null;
        String compareWith = null;
        for (char c : section.toCharArray()) {
            String s = String.valueOf(c);
            if (s.equals("\"")) {
                if ((currentIndex >= 7) && section.substring(currentIndex-7).startsWith("\"mode\":\"")) {
                    currentStartIndex = currentIndex+1;
                }
                if (section.substring(currentIndex).startsWith("\",\"number\":\"")) {
                    mode = section.substring(currentStartIndex, currentIndex);
                }
                if ((currentIndex >= 9) && section.substring(currentIndex-9).startsWith("\"number\":\"")) {
                    currentStartIndex = currentIndex+1;
                }
                if (section.substring(currentIndex).startsWith("\",\"compare_with\":\"")) {
                    number = section.substring(currentStartIndex, currentIndex);
                }
                if ((currentIndex >= 15) && section.substring(currentIndex-15).startsWith("\"compare_with\":\"")) {
                    currentStartIndex = currentIndex+1;
                }
                if (section.substring(currentIndex).startsWith("\"]$")) {
                    compareWith = section.substring(currentStartIndex, currentIndex);
                }
            }
            currentIndex++;
        }
        if ((mode != null) && (number != null) && (compareWith != null)) {
            l.add(mode);
            l.add(number);
            l.add(compareWith);
        }
        return l;
    }

    private static List<String> getSections(String value) {
        List<String> l = new ArrayList<>();
        int currentIndex = 0;
        int currentStartIndex = 0;
        for (char c : value.toCharArray()) {
            String s = String.valueOf(c);
            if (s.equals("[") && value.substring(currentIndex).startsWith("[\"mode\":\"")) {
                currentStartIndex = currentIndex;
            }
            if ((currentIndex >= 1) && s.equals("]") && value.substring(currentIndex-1).startsWith("\"]$")) {
                l.add(value.substring(currentStartIndex, currentIndex+2));
            }
            currentIndex++;
        }
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.is_number");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines(I18n.get("fancymenu.helper.editor.items.visibilityrequirements.is_number.desc")));
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getValueDisplayName() {
        return I18n.get("fancymenu.helper.editor.items.visibilityrequirements.is_number.valuename");
    }

    @Override
    public String getValuePreset() {
        return "[\"mode\":\"...\",\"number\":\"...\",\"compare_with\":\"...\"]$";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull LoadingRequirementInstance requirementInstance) {
        IsNumberValueConfigScreen s = new IsNumberValueConfigScreen(Objects.requireNonNullElse(requirementInstance.value, ""), callback -> {
            if (callback != null) {
                requirementInstance.value = callback;
            }
            Minecraft.getInstance().setScreen(parentScreen);
        });
        Minecraft.getInstance().setScreen(s);
    }

    public static class IsNumberValueConfigScreen extends StringBuilderScreen {

        @NotNull
        protected NumberCompareMode mode = NumberCompareMode.EQUALS;
        @NotNull
        protected String firstNumber = "";
        @NotNull
        protected String secondNumber = "";

        protected TextInputCell firstNumberCell;
        protected TextInputCell secondNumberCell;

        protected IsNumberValueConfigScreen(String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.helper.editor.items.visibilityrequirements.is_number.valuename"), callback);
            if (value == null) value = "";
            List<String> sections = getSections(value);
            if (!sections.isEmpty()) {
                List<String> deserialized = parseSection(sections.get(0));
                if (!deserialized.isEmpty()) {
                    NumberCompareMode m = NumberCompareMode.getByKey(deserialized.get(0));
                    if (m != null) mode = m;
                    firstNumber = deserialized.get(1);
                    secondNumber = deserialized.get(2);
                }
            }
        }

        @Override
        protected void initCells() {

            this.addSpacerCell(20);

            ILocalizedValueCycle<NumberCompareMode> modeCycle = CommonCycles.cycleOrangeValue("fancymenu.loading_requirements.is_number.compare_mode", Arrays.asList(NumberCompareMode.values()), this.mode)
                    .setValueNameSupplier(mode -> {
                        if (mode == NumberCompareMode.BIGGER_THAN) return I18n.get("fancymenu.loading_requirements.is_number.compare_mode.bigger_than");
                        if (mode == NumberCompareMode.SMALLER_THAN) return I18n.get("fancymenu.loading_requirements.is_number.compare_mode.smaller_than");
                        if (mode == NumberCompareMode.BIGGER_THAN_OR_EQUALS) return I18n.get("fancymenu.loading_requirements.is_number.compare_mode.bigger_than_or_equals");
                        if (mode == NumberCompareMode.SMALLER_THAN_OR_EQUALS) return I18n.get("fancymenu.loading_requirements.is_number.compare_mode.smaller_than_or_equals");
                        return I18n.get("fancymenu.loading_requirements.is_number.compare_mode.equals");
                    });
            this.addCycleButtonCell(modeCycle, true, (value, button) -> {
                this.mode = value;
            });

            String fNumber = this.getFirstNumberString();
            this.addLabelCell(Component.translatable("fancymenu.loading_requirements.is_number.compare_mode.first_number"));
            this.firstNumberCell = this.addTextInputCell(null, true, true).setText(fNumber);

            String sNumber = this.getSecondNumberString();
            this.addLabelCell(Component.translatable("fancymenu.loading_requirements.is_number.compare_mode.second_number"));
            this.secondNumberCell = this.addTextInputCell(null, true, true).setText(sNumber);

        }

        @Override
        public @NotNull String buildString() {
            return "[\"mode\":\"" + this.mode.key + "\",\"number\":\"" + this.getFirstNumberString() + "\",\"compare_with\":\"" + this.getSecondNumberString() + "\"]$";
        }

        @NotNull
        protected String getFirstNumberString() {
            if (this.firstNumberCell != null) {
                return this.firstNumberCell.getText();
            }
            return this.firstNumber;
        }

        @NotNull
        protected String getSecondNumberString() {
            if (this.secondNumberCell != null) {
                return this.secondNumberCell.getText();
            }
            return this.secondNumber;
        }

    }

    public enum NumberCompareMode {

        EQUALS("equals"),
        BIGGER_THAN("bigger-than"),
        SMALLER_THAN("smaller-than-or-equals"),
        BIGGER_THAN_OR_EQUALS("bigger-than-or-equals"),
        SMALLER_THAN_OR_EQUALS("smaller-than-or-equals");

        public final String key;

        NumberCompareMode(String key) {
            this.key = key;
        }

        @Nullable
        public static NumberCompareMode getByKey(@NotNull String key) {
            for (NumberCompareMode mode : NumberCompareMode.values()) {
                if (mode.key.equals(key)) return mode;
            }
            return null;
        }

    }

}

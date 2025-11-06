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

public class IsTextRequirement extends LoadingRequirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsTextRequirement() {
        super("fancymenu_visibility_requirement_is_text");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

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
            String text = section.get(1);
            String compareWith = section.get(2);
            if (mode.equals("equals")) {
                return text.equals(compareWith);
            } else if (mode.equals("contains")) {
                return text.contains(compareWith);
            } else if (mode.equals("starts-with")) {
                return text.startsWith(compareWith);
            } else if (mode.equals("ends-with")) {
                return text.endsWith(compareWith);
            }
        }
        return false;
    }

    /**
     * index 0 = mode
     * index 1 = text
     * index 2 = compare_with
     */
    private static List<String> parseSection(String section) {
        List<String> l = new ArrayList<>();
        int currentIndex = 0;
        int currentStartIndex = 0;
        String mode = null;
        String text = null;
        String compareWith = null;
        for (char c : section.toCharArray()) {
            String s = String.valueOf(c);
            if (s.equals("\"")) {
                if ((currentIndex >= 7) && section.substring(currentIndex-7).startsWith("\"mode\":\"")) {
                    currentStartIndex = currentIndex+1;
                }
                if (section.substring(currentIndex).startsWith("\",\"text\":\"")) {
                    mode = section.substring(currentStartIndex, currentIndex);
                }
                if ((currentIndex >= 7) && section.substring(currentIndex-7).startsWith("\"text\":\"")) {
                    currentStartIndex = currentIndex+1;
                }
                if (section.substring(currentIndex).startsWith("\",\"compare_with\":\"")) {
                    text = section.substring(currentStartIndex, currentIndex);
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
        if ((mode != null) && (text != null) && (compareWith != null)) {
            l.add(mode);
            l.add(text);
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
        return I18n.get("fancymenu.requirements.is_text");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.is_text.desc"));
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getValueDisplayName() {
        return "";
    }

    @Override
    public String getValuePreset() {
        return "[\"mode\":\"...\",\"text\":\"...\",\"compare_with\":\"...\"]$";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull LoadingRequirementInstance requirementInstance) {
        IsTextValueConfigScreen s = new IsTextValueConfigScreen(Objects.requireNonNullElse(requirementInstance.value, ""), callback -> {
            if (callback != null) {
                requirementInstance.value = callback;
            }
            Minecraft.getInstance().setScreen(parentScreen);
        });
        Minecraft.getInstance().setScreen(s);
    }

    public static class IsTextValueConfigScreen extends StringBuilderScreen {

        @NotNull
        protected TextCompareMode mode = TextCompareMode.EQUALS;
        @NotNull
        protected String firstText = "";
        @NotNull
        protected String secondText = "";

        protected TextInputCell firstTextCell;
        protected TextInputCell secondTextCell;

        protected IsTextValueConfigScreen(String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.requirements.is_text.value_name"), callback);
            if (value == null) value = "";
            List<String> sections = getSections(value);
            if (!sections.isEmpty()) {
                List<String> deserialized = parseSection(sections.get(0));
                if (!deserialized.isEmpty()) {
                    TextCompareMode m = TextCompareMode.getByKey(deserialized.get(0));
                    if (m != null) mode = m;
                    firstText = deserialized.get(1);
                    secondText = deserialized.get(2);
                }
            }
        }

        @Override
        protected void initCells() {

            this.addSpacerCell(20);

            ILocalizedValueCycle<TextCompareMode> modeCycle = CommonCycles.cycleOrangeValue("fancymenu.requirements.is_text.compare_mode", Arrays.asList(TextCompareMode.values()), this.mode)
                    .setValueNameSupplier(mode -> {
                        if (mode == TextCompareMode.CONTAINS) return I18n.get("fancymenu.requirements.is_text.compare_mode.contains");
                        if (mode == TextCompareMode.STARTS_WITH) return I18n.get("fancymenu.requirements.is_text.compare_mode.starts_with");
                        if (mode == TextCompareMode.ENDS_WITH) return I18n.get("fancymenu.requirements.is_text.compare_mode.ends_with");
                        return I18n.get("fancymenu.requirements.is_text.compare_mode.equals");
                    });
            this.addCycleButtonCell(modeCycle, true, (value, button) -> {
                this.mode = value;
            });

            this.addCellGroupEndSpacerCell();

            String fText = this.getFirstTextString();
            this.addLabelCell(Component.translatable("fancymenu.requirements.is_text.compare_mode.first_text"));
            this.firstTextCell = this.addTextInputCell(null, true, true).setText(fText);

            this.addCellGroupEndSpacerCell();

            String sText = this.getSecondTextString();
            this.addLabelCell(Component.translatable("fancymenu.requirements.is_text.compare_mode.second_text"));
            this.secondTextCell = this.addTextInputCell(null, true, true).setText(sText);

            this.addSpacerCell(20);

        }

        @Override
        public @NotNull String buildString() {
            return "[\"mode\":\"" + this.mode.key + "\",\"text\":\"" + this.getFirstTextString() + "\",\"compare_with\":\"" + this.getSecondTextString() + "\"]$";
        }

        @NotNull
        protected String getFirstTextString() {
            if (this.firstTextCell != null) {
                return this.firstTextCell.getText();
            }
            return this.firstText;
        }

        @NotNull
        protected String getSecondTextString() {
            if (this.secondTextCell != null) {
                return this.secondTextCell.getText();
            }
            return this.secondText;
        }

    }

    public enum TextCompareMode {

        EQUALS("equals"),
        CONTAINS("contains"),
        STARTS_WITH("starts-with"),
        ENDS_WITH("ends-with");

        public final String key;

        TextCompareMode(String key) {
            this.key = key;
        }

        @Nullable
        public static TextCompareMode getByKey(@NotNull String key) {
            for (TextCompareMode mode : TextCompareMode.values()) {
                if (mode.key.equals(key)) return mode;
            }
            return null;
        }

    }

}

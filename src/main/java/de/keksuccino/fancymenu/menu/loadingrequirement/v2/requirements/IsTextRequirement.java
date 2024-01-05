package de.keksuccino.fancymenu.menu.loadingrequirement.v2.requirements;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.menu.loadingrequirement.v2.LoadingRequirement;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        //VALUE EXAMPLE:
        //["mode":"...","text":"...","compare_with":"..."]$,["mode":"...","text":"...","compare_with":"..."]$

        if (value != null) {
            List<String> secStrings = getSections(value);
            if (secStrings.isEmpty()) {
                return false;
            }
            boolean b = true;
            for (String s : secStrings) {
//                LOGGER.info("###################### IS TEXT: SEC: " + s);
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
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.is_text");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.is_text.desc"), "%n%"));
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getValueDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.is_text.valuename");
    }

    @Override
    public String getValuePreset() {
        return "[\"mode\":\"...\",\"text\":\"...\",\"compare_with\":\"...\"]$";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}

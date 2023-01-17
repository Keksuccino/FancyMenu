
package de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements.requirements;

import de.keksuccino.fancymenu.api.visibilityrequirements.VisibilityRequirement;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IsNumberVisibilityRequirement extends VisibilityRequirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsNumberVisibilityRequirement() {
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
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.is_number");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.is_number.desc"), "%n%"));
    }

    @Override
    public String getValueDisplayName() {
        return Locals.localize("fancymenu.helper.editor.items.visibilityrequirements.is_number.valuename");
    }

    @Override
    public String getValuePreset() {
        return "[\"mode\":\"...\",\"number\":\"...\",\"compare_with\":\"...\"]$,[\"mode\":\"...\",\"number\":\"...\",\"compare_with\":\"...\"]$";
    }

    @Override
    public CharacterFilter getValueInputFieldFilter() {
        return null;
    }

}

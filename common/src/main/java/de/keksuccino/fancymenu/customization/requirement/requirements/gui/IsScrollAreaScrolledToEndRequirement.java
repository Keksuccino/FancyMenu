package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.elements.scrollarea.ScrollAreaElement;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Met while the content of a Scrollable Area element is scrolled to its end, in the direction the area mainly scrolls in.
 * An area with nothing to scroll shows all of its content, which counts as being at the end.
 */
public class IsScrollAreaScrolledToEndRequirement extends Requirement {

    @NotNull
    private final Function<String, AbstractElement> elementLookup;

    public IsScrollAreaScrolledToEndRequirement() {
        this(AbstractElement::getElementByInstanceIdentifier);
    }

    IsScrollAreaScrolledToEndRequirement(@NotNull Function<String, AbstractElement> elementLookup) {
        super("is_scroll_area_scrolled_to_end");
        this.elementLookup = Objects.requireNonNull(elementLookup);
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
        if ((value == null) || value.isBlank()) return false;
        if (!(this.elementLookup.apply(value.trim()) instanceof ScrollAreaElement area)) return false;
        return area.getScrolling().getMainAxis().isAtEnd();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.is_scroll_area_scrolled_to_end");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.is_scroll_area_scrolled_to_end.desc");
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.gui");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.requirements.is_scroll_area_scrolled_to_end.value_name");
    }

    @Override
    public String getValuePreset() {
        return "some.element.id";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}

package de.keksuccino.fancymenu.customization.requirement;

import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class TestRequirement extends Requirement {

    private final Predicate<String> evaluation;
    private final AtomicInteger evaluationCount = new AtomicInteger();

    public TestRequirement(@NotNull String identifier, @NotNull Predicate<String> evaluation) {
        super(identifier);
        this.evaluation = evaluation;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        this.evaluationCount.incrementAndGet();
        return this.evaluation.test(value);
    }

    public int getEvaluationCount() {
        return this.evaluationCount.get();
    }

    @Override
    public boolean checkAsync() {
        return true;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("Test requirement");
    }

    @Override
    public Component getDescription() {
        return null;
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public Component getValueDisplayName() {
        return null;
    }

    @Override
    public String getValuePreset() {
        return null;
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}

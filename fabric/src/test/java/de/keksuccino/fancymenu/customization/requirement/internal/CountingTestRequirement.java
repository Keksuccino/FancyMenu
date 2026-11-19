package de.keksuccino.fancymenu.customization.requirement.internal;

import de.keksuccino.fancymenu.customization.FreshValuesScope;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * A requirement whose result comes from a supplier and that counts how often it got checked.
 * It never gets registered, so every test can create its own.
 */
final class CountingTestRequirement extends Requirement {

    @NotNull
    private final BooleanSupplier result;
    private final AtomicInteger checks = new AtomicInteger();
    private final AtomicInteger checksOutsideFreshValuesScope = new AtomicInteger();

    CountingTestRequirement(@NotNull BooleanSupplier result) {
        super("fancymenu_test_counting_requirement");
        this.result = Objects.requireNonNull(result);
    }

    /** A new container that only contains this requirement. */
    @NotNull
    RequirementContainer newContainer() {
        RequirementContainer container = new RequirementContainer();
        container.addInstance(new RequirementInstance(this, null, RequirementInstance.RequirementMode.IF, container));
        return container;
    }

    int checks() {
        return this.checks.get();
    }

    int checksOutsideFreshValuesScope() {
        return this.checksOutsideFreshValuesScope.get();
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        this.checks.incrementAndGet();
        if (!FreshValuesScope.isActive()) this.checksOutsideFreshValuesScope.incrementAndGet();
        return this.result.getAsBoolean();
    }

    // The default check needs a running Minecraft client to compare threads.
    @Override
    public boolean checkAsync() {
        return true;
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

    @Override
    public @Nullable String getCategory() {
        return null;
    }

    @Override
    public @Nullable Component getValueDisplayName() {
        return null;
    }

    @Override
    public @Nullable String getValuePreset() {
        return null;
    }

    @Override
    public @Nullable List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}

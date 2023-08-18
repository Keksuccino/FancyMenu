package de.keksuccino.fancymenu.customization.action.statements;

import de.keksuccino.fancymenu.customization.action.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class IfExecutableBlock extends AbstractExecutableBlock {

    @NotNull
    public LoadingRequirementContainer body = new LoadingRequirementContainer().forceRequirementsMet(true);

    public IfExecutableBlock() {
    }

    public IfExecutableBlock(@NotNull LoadingRequirementContainer body) {
        this.body = Objects.requireNonNull(body);
    }

    @Override
    public void execute() {
        if (this.check()) super.execute();
    }

    public boolean check() {
        return this.body.requirementsMet();
    }

}

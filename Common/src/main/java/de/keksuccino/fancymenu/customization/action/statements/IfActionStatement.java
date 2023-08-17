package de.keksuccino.fancymenu.customization.action.statements;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class IfActionStatement implements ActionStatement {

    @NotNull
    public LoadingRequirementContainer body = new LoadingRequirementContainer().forceRequirementsMet(true);
    @NotNull
    public String identifier = ScreenCustomization.generateUniqueIdentifier();

    public IfActionStatement() {
    }

    public IfActionStatement(@NotNull LoadingRequirementContainer body) {
        this.body = Objects.requireNonNull(body);
    }

    public boolean check() {
        return this.body.requirementsMet();
    }

    public static IfActionStatement buildAlwaysTrue() {
        return new IfActionStatement();
    }

}

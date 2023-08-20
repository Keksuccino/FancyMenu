package de.keksuccino.fancymenu.customization.action.blocks.statements;

import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.Objects;

public class IfExecutableBlock extends AbstractExecutableBlock {

    @NotNull
    public LoadingRequirementContainer body = new LoadingRequirementContainer().forceRequirementsMet(true);
    @Nullable
    protected AbstractExecutableBlock child;

    public IfExecutableBlock() {
    }

    public IfExecutableBlock(@NotNull LoadingRequirementContainer body) {
        this.body = Objects.requireNonNull(body);
    }

    @Override
    public String getBlockType() {
        return "if";
    }

    @Override
    public void execute() {
        if (this.check()) {
            super.execute();
        } else if (this.child != null) {
            this.child.execute();
        }
    }

    @Override
    public void setChild(@Nullable AbstractExecutableBlock child) {
        this.child = child;
    }

    @Nullable
    @Override
    public AbstractExecutableBlock getChild() {
        return this.child;
    }

    public boolean check() {
        return this.body.requirementsMet();
    }

    @Override
    public @NotNull PropertyContainer serialize() {
        PropertyContainer container = super.serialize();
        String key = "[if_executable_block_body:" + this.getIdentifier() + "]";
        container.putProperty(key, this.body.identifier);
        this.body.serializeToExistingPropertyContainer(container);
        return container;
    }

    public static IfExecutableBlock deserializeEmptyWithIdentifier(@NotNull PropertyContainer serialized, @NotNull String identifier) {
        IfExecutableBlock b = new IfExecutableBlock();
        b.identifier = identifier;
        for (Map.Entry<String, String> m : serialized.getProperties().entrySet()) {
            if (m.getKey().equals("[if_executable_block_body:" + identifier + "]")) {
                LoadingRequirementContainer lrc = LoadingRequirementContainer.deserializeWithIdentifier(m.getValue(), serialized);
                if (lrc != null) {
                    b.body = lrc;
                }
                break;
            }
        }
        return b;
    }

}

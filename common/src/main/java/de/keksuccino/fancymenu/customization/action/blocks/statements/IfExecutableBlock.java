package de.keksuccino.fancymenu.customization.action.blocks.statements;

import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class IfExecutableBlock extends AbstractExecutableBlock {

    @NotNull
    public LoadingRequirementContainer condition = new LoadingRequirementContainer().forceRequirementsMet(true);
    @Nullable
    protected AbstractExecutableBlock child;
    private boolean collapsed = false;

    public IfExecutableBlock() {
    }

    public IfExecutableBlock(@NotNull LoadingRequirementContainer condition) {
        this.condition = Objects.requireNonNull(condition);
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
    public void setAppendedBlock(@Nullable AbstractExecutableBlock appended) {
        super.setAppendedBlock(appended);
        this.child = appended;
    }

    @Nullable
    @Override
    public AbstractExecutableBlock getAppendedBlock() {
        return this.child;
    }

    public boolean isCollapsed() {
        return this.collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    @Override
    public void addValuePlaceholder(@NotNull String placeholder, @NotNull Supplier<String> replaceWithSupplier) {
        super.addValuePlaceholder(placeholder, replaceWithSupplier);
        this.condition.addValuePlaceholder(placeholder, replaceWithSupplier);
    }

    @Override
    public @NotNull IfExecutableBlock copy(boolean unique) {
        IfExecutableBlock b = new IfExecutableBlock();
        if (!unique) b.identifier = this.identifier;
        if (this.getAppendedBlock() != null) b.setAppendedBlock((AbstractExecutableBlock)this.getAppendedBlock().copy(unique));
        for (Executable e : this.executables) {
            b.addExecutable(e.copy(unique));
        }
        b.condition = this.condition.copy(unique);
        b.valuePlaceholders.putAll(this.valuePlaceholders);
        b.collapsed = this.collapsed;
        return b;
    }

    public boolean check() {
        return this.condition.requirementsMet();
    }

    @Override
    public @NotNull PropertyContainer serialize() {
        PropertyContainer container = super.serialize();
        String key = "[if_executable_block_body:" + this.getIdentifier() + "]";
        container.putProperty(key, this.condition.identifier);
        this.condition.serializeToExistingPropertyContainer(container);
        container.putProperty("[if_executable_block_collapsed:" + this.getIdentifier() + "]", Boolean.toString(this.collapsed));
        return container;
    }

    public static IfExecutableBlock deserializeEmptyWithIdentifier(@NotNull PropertyContainer serialized, @NotNull String identifier) {
        IfExecutableBlock b = new IfExecutableBlock();
        b.identifier = identifier;
        for (Map.Entry<String, String> m : serialized.getProperties().entrySet()) {
            if (m.getKey().equals("[if_executable_block_body:" + identifier + "]")) {
                LoadingRequirementContainer lrc = LoadingRequirementContainer.deserializeWithIdentifier(m.getValue(), serialized);
                if (lrc != null) {
                    b.condition = lrc;
                }
                break;
            }
        }
        String collapsedKey = "[if_executable_block_collapsed:" + identifier + "]";
        if (serialized.hasProperty(collapsedKey)) {
            b.collapsed = Boolean.parseBoolean(serialized.getValue(collapsedKey));
        }
        return b;
    }

}

package de.keksuccino.fancymenu.customization.action.blocks;

import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;

public class FolderExecutableBlock extends AbstractExecutableBlock {

    public static final String DEFAULT_NAME = "New Folder";

    @NotNull
    private String name = DEFAULT_NAME;
    private boolean collapsed = false;

    @Override
    public String getBlockType() {
        return "folder";
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public void setName(@NotNull String name) {
        this.name = name.isEmpty() ? DEFAULT_NAME : name;
    }

    public boolean isCollapsed() {
        return this.collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    @Override
    public @NotNull FolderExecutableBlock copy(boolean unique) {
        FolderExecutableBlock b = new FolderExecutableBlock();
        if (!unique) b.identifier = this.identifier;
        if (this.getAppendedBlock() != null) b.setAppendedBlock((AbstractExecutableBlock)this.getAppendedBlock().copy(unique));
        for (Executable e : this.executables) {
            b.addExecutable(e.copy(unique));
        }
        b.valuePlaceholders.putAll(this.valuePlaceholders);
        b.name = this.name;
        b.collapsed = this.collapsed;
        return b;
    }

    @Override
    public @NotNull PropertyContainer serialize() {
        PropertyContainer container = super.serialize();
        container.putProperty("[folder_executable_block_name:" + this.identifier + "]", this.name);
        container.putProperty("[folder_executable_block_collapsed:" + this.identifier + "]", Boolean.toString(this.collapsed));
        return container;
    }

    public static FolderExecutableBlock deserializeEmptyWithIdentifier(@NotNull PropertyContainer serialized, @NotNull String identifier) {
        FolderExecutableBlock b = new FolderExecutableBlock();
        b.identifier = identifier;
        String nameKey = "[folder_executable_block_name:" + identifier + "]";
        String collapsedKey = "[folder_executable_block_collapsed:" + identifier + "]";
        if (serialized.hasProperty(nameKey)) {
            String storedName = serialized.getValue(nameKey);
            if (storedName != null && !storedName.isEmpty()) {
                b.name = storedName;
            }
        }
        if (serialized.hasProperty(collapsedKey)) {
            b.collapsed = Boolean.parseBoolean(serialized.getValue(collapsedKey));
        }
        return b;
    }

}

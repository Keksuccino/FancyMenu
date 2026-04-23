package de.keksuccino.fancymenu.customization.action.blocks;

import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;

public class CommentExecutableBlock extends AbstractExecutableBlock {

    public static final String DEFAULT_COMMENT = "New Comment";

    @NotNull
    private String comment = DEFAULT_COMMENT;

    @Override
    public String getBlockType() {
        return "comment";
    }

    @NotNull
    public String getComment() {
        return this.comment;
    }

    public void setComment(@NotNull String comment) {
        String normalized = comment.trim();
        this.comment = normalized.isEmpty() ? DEFAULT_COMMENT : normalized;
    }

    @Override
    public @NotNull CommentExecutableBlock copy(boolean unique) {
        CommentExecutableBlock b = new CommentExecutableBlock();
        if (!unique) b.identifier = this.identifier;
        if (this.getAppendedBlock() != null) b.setAppendedBlock((AbstractExecutableBlock)this.getAppendedBlock().copy(unique));
        for (Executable e : this.executables) {
            b.addExecutable(e.copy(unique));
        }
        b.valuePlaceholders.putAll(this.valuePlaceholders);
        b.comment = this.comment;
        return b;
    }

    @Override
    public @NotNull PropertyContainer serialize() {
        PropertyContainer container = super.serialize();
        container.putProperty("[comment_executable_block_value:" + this.identifier + "]", this.comment);
        return container;
    }

    public static CommentExecutableBlock deserializeEmptyWithIdentifier(@NotNull PropertyContainer serialized, @NotNull String identifier) {
        CommentExecutableBlock b = new CommentExecutableBlock();
        b.identifier = identifier;
        String valueKey = "[comment_executable_block_value:" + identifier + "]";
        if (serialized.hasProperty(valueKey)) {
            String storedComment = serialized.getValue(valueKey);
            if (storedComment != null && !storedComment.isEmpty()) {
                b.comment = storedComment;
            }
        }
        return b;
    }
}

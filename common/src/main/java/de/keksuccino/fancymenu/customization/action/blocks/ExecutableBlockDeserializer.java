package de.keksuccino.fancymenu.customization.action.blocks;

import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.ElseIfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.IfExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.statements.WhileExecutableBlock;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.konkrete.input.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class ExecutableBlockDeserializer {

    /**
     * This deserializes all known types of {@link AbstractExecutableBlock}s and its known content types, such as {@link ActionInstance}.
     */
    @NotNull
    public static List<AbstractExecutableBlock> deserializeAll(@NotNull PropertyContainer serialized) {

        Map<AbstractExecutableBlock, BlockMeta> executableBlocks = new LinkedHashMap<>();

        for (Map.Entry<String, String> m : serialized.getProperties().entrySet()) {
            if (m.getKey().startsWith("[executable_block:") && m.getKey().contains("]")) {
                String identifier = m.getKey().split("\\[executable_block:", 2)[1].split("]", 2)[0];
                if (m.getKey().contains("[type:")) {
                    String type = m.getKey().split("\\[type:", 2)[1];
                    if (type.contains("]")) {
                        type = type.split("]", 2)[0];
                        AbstractExecutableBlock b = deserializeEmptyWithTypeAndIdentifier(serialized, type, identifier);
                        if (b != null) {
                            String contentRaw = m.getValue();
                            if (contentRaw.contains("[executables:") && contentRaw.contains("]")) {
                                BlockMeta meta = new BlockMeta();
                                contentRaw = contentRaw.split("\\[executables:", 2)[1].split("]", 2)[0];
                                if (contentRaw.contains(";")) {
                                    meta.content = Arrays.asList(StringUtils.splitLines(contentRaw, ";"));
                                } else {
                                    meta.content.add(contentRaw);
                                }
                                if (m.getValue().contains("[appended:")) {
                                    String childIdentifier = m.getValue().split("\\[appended:", 2)[1];
                                    if (childIdentifier.contains("]")) {
                                        childIdentifier = childIdentifier.split("]", 2)[0];
                                        meta.childIdentifier = childIdentifier;
                                    }
                                }
                                executableBlocks.put(b, meta);
                            }
                        }
                    }
                }
            }
        }

        List<Executable> possibleContent = new ArrayList<>();
        possibleContent.addAll(executableBlocks.keySet());
        possibleContent.addAll(ActionInstance.deserializeAll(serialized));

        //Add contents and children to all blocks
        for (Map.Entry<AbstractExecutableBlock, BlockMeta> m : executableBlocks.entrySet()) {
            AbstractExecutableBlock b = m.getKey();
            List<String> content = m.getValue().content;
            for (String executableId : content) {
                for (Executable executable : possibleContent) {
                    if (executable.getIdentifier().equals(executableId)) {
                        b.addExecutable(executable);
                        break;
                    }
                }
            }
            if (m.getValue().childIdentifier != null) {
                for (Executable executable : possibleContent) {
                    if (executable.getIdentifier().equals(m.getValue().childIdentifier) && (executable instanceof AbstractExecutableBlock aeb)) {
                        b.setAppendedBlock(aeb);
                        break;
                    }
                }
            }
        }

        return new ArrayList<>(executableBlocks.keySet());

    }

    /**
     * This searches for a block with the given ID, will deserialize it if possible and will deserialize all its content and add it to the block.
     */
    @Nullable
    public static AbstractExecutableBlock deserializeWithIdentifier(@NotNull PropertyContainer serialized, @NotNull String identifier) {
        for (AbstractExecutableBlock b : deserializeAll(serialized)) {
            if (b.identifier.equals(identifier)) return b;
        }
        return null;
    }

    /**
     * This deserializes all known types of {@link AbstractExecutableBlock}s WITHOUT its content.
     */
    @Nullable
    public static AbstractExecutableBlock deserializeEmptyWithTypeAndIdentifier(@NotNull PropertyContainer serialized, @NotNull String type, @NotNull String identifier) {
        if (type.equals("generic")) return GenericExecutableBlock.deserializeEmptyWithIdentifier(serialized, identifier);
        if (type.equals("if")) return IfExecutableBlock.deserializeEmptyWithIdentifier(serialized, identifier);
        if (type.equals("else-if")) return ElseIfExecutableBlock.deserializeEmptyWithIdentifier(serialized, identifier);
        if (type.equals("else")) return ElseExecutableBlock.deserializeEmptyWithIdentifier(serialized, identifier);
        if (type.equals("while")) return WhileExecutableBlock.deserializeEmptyWithIdentifier(serialized, identifier);
        if (type.equals("folder")) return FolderExecutableBlock.deserializeEmptyWithIdentifier(serialized, identifier);
        return null;
    }

    protected static class BlockMeta {
        @NotNull
        protected List<String> content = new ArrayList<>();
        @Nullable
        protected String childIdentifier;
    }

}

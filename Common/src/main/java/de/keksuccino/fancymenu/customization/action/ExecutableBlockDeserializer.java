package de.keksuccino.fancymenu.customization.action;

import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.IfExecutableBlock;
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

        Map<AbstractExecutableBlock, List<String>> executableBlocks = new LinkedHashMap<>();

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
                                List<String> content = new ArrayList<>();
                                contentRaw = contentRaw.split("\\[executables:", 2)[1].split("]", 2)[0];
                                if (contentRaw.contains(";")) {
                                    content = Arrays.asList(StringUtils.splitLines(contentRaw, ";"));
                                } else {
                                    content.add(contentRaw);
                                }
                                executableBlocks.put(b, content);
                            }
                        }
                    }
                }
            }
        }

        List<Executable> possibleContent = new ArrayList<>();
        possibleContent.addAll(executableBlocks.keySet());
        possibleContent.addAll(ActionInstance.deserializeAll(serialized));

        //Add contents to all blocks
        for (Map.Entry<AbstractExecutableBlock, List<String>> m : executableBlocks.entrySet()) {
            AbstractExecutableBlock b = m.getKey();
            List<String> content = m.getValue();
            for (String executableId : content) {
                for (Executable executable : possibleContent) {
                    if (executable.getIdentifier().equals(executableId)) {
                        b.addExecutable(executable);
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
        return null;
    }

}

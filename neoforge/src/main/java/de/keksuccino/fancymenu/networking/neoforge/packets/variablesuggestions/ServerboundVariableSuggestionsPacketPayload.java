package de.keksuccino.fancymenu.networking.neoforge.packets.variablesuggestions;

import de.keksuccino.fancymenu.util.ObjectUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public record ServerboundVariableSuggestionsPacketPayload(@NotNull List<String> variableSuggestions) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation("fancymenu", "serverbound_variable_suggestions_payload");

    public ServerboundVariableSuggestionsPacketPayload(final FriendlyByteBuf buffer) {
        this(ObjectUtils.build(() -> {
            List<String> suggestions = new ArrayList<>();
            String suggestionsRaw = buffer.readUtf();
            if (suggestionsRaw.contains(";")) {
                for (String s : suggestionsRaw.split(";")) {
                    if (!s.isEmpty()) {
                        suggestions.add(s);
                    }
                }
            }
            return suggestions;
        }));
    }

    @Override
    public void write(@NotNull final FriendlyByteBuf buffer) {
        StringBuilder suggestionsRaw = new StringBuilder();
        for (String s : variableSuggestions()) {
            suggestionsRaw.append(s).append(";");
        }
        buffer.writeUtf(suggestionsRaw.toString());
    }

    @Override
    public @NotNull ResourceLocation id() {
        return ID;
    }

}

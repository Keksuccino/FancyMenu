package de.keksuccino.fancymenu.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.ISuggestionProvider;

import java.util.concurrent.CompletableFuture;

public class CommandUtils {

    public static CompletableFuture<Suggestions> getStringSuggestions(SuggestionsBuilder suggestionsBuilder, String... suggestions) {
        return ISuggestionProvider.suggest(suggestions, suggestionsBuilder);
    }

}

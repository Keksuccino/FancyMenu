package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public interface IMixinCommandSuggestions {

    @Accessor("allowSuggestions") boolean getAllowSuggestionsFancyMenu();

    @Accessor("keepSuggestions") boolean getKeepSuggestionsFancyMenu();

    @Nullable
    @Accessor("currentParse") ParseResults<ClientSuggestionProvider> getCurrentParseFancyMenu();
    @Accessor("currentParse") void setCurrentParseFancyMenu(ParseResults<ClientSuggestionProvider> currentParse);

    @Nullable
    @Accessor("pendingSuggestions") CompletableFuture<Suggestions> getPendingSuggestionsFancyMenu();
    @Accessor("pendingSuggestions") void setPendingSuggestionsFancyMenu(CompletableFuture<Suggestions> pendingSuggestions);

    @Accessor("commandUsage") List<FormattedCharSequence> getCommandUsageFancyMenu();

    @Nullable
    @Accessor("suggestions") CommandSuggestions.SuggestionsList getSuggestionsFancyMenu();
    @Accessor("suggestions") void setSuggestionsFancyMenu(CommandSuggestions.SuggestionsList suggestions);

    @Invoker("updateUsageInfo") void invokeUpdateUsageInfoFancyMenu();

    @Invoker("sortSuggestions") List<Suggestion> invokeSortSuggestionsFancyMenu(Suggestions suggestions);

}

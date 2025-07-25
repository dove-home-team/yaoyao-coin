package com.warmthdawn.mods.yaoyaocoin.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.warmthdawn.mods.yaoyaocoin.data.CoinManager;
import com.warmthdawn.mods.yaoyaocoin.data.CoinType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CoinNameArgument implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = Arrays.asList("copper", "gold");
    private static final DynamicCommandExceptionType ERROR_COIN_NOT_FOUND = new DynamicCommandExceptionType((p_112095_) -> Component.translatable("yaoyaocoin.coin.notFound", p_112095_));

    public static CoinNameArgument coinName() {
        return new CoinNameArgument();
    }

    public static CoinType getCoin(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String s = context.getArgument(name, String.class);
        CoinType type = CoinManager.getInstance().findCoinType(s);
        if (type == null) {
            throw ERROR_COIN_NOT_FOUND.create(s);
        } else {
            return type;
        }
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readUnquotedString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return context.getSource() instanceof SharedSuggestionProvider ? SharedSuggestionProvider.suggest(
                CoinManager.getInstance().getCoinTypes().stream().map(CoinType::name)
                , builder) : Suggestions.empty();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}

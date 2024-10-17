package slimeknights.mantle.command.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.SuggestionProviders;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.book.BookLoader;

import java.util.function.Consumer;

/**
 * Root command for all commands in mantle
 */
public class MantleClientCommand {
  /** Suggestion provider that lists registered book ids **/
  public static SuggestionProvider<CommandSource> REGISTERED_BOOKS;

  /** Registers all Mantle command related content */
  public static void init() {
    // register arguments
    REGISTERED_BOOKS = SuggestionProviders.register(Mantle.getResource("registered_books"), (context, builder) ->
      ISuggestionProvider.suggestIterable(BookLoader.getRegisteredBooks(), builder));

    // add command listener
    MinecraftForge.EVENT_BUS.addListener(MantleClientCommand::registerCommand);
  }

  /** Registers a sub command for the root Mantle client command */
  private static void register(LiteralArgumentBuilder<CommandSource> root, String name, Consumer<LiteralArgumentBuilder<CommandSource>> consumer) {
    LiteralArgumentBuilder<CommandSource> subCommand = Commands.literal(name);
    consumer.accept(subCommand);
    root.then(subCommand);
  }

  /** Event listener to register the Mantle client command */
  private static void registerCommand(RegisterCommandsEvent event) {
    LiteralArgumentBuilder<CommandSource> builder = Commands.literal("mantle");

    // sub commands
    register(builder, "book", BookCommand::register);
    register(builder, "clear_book_cache", ClearBookCacheCommand::register);

    // register final command
    event.getDispatcher().register(builder);
  }
}

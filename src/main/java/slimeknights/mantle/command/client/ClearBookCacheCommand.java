package slimeknights.mantle.command.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.ResourceLocationArgument;
import net.minecraft.util.ResourceLocation;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.data.BookData;

import javax.annotation.Nullable;

/** Command that clears the cache of a book or all books, faster than resource pack reloading for book writing */
public class ClearBookCacheCommand {
  /**
   * Registers this sub command with the root command
   * @param subCommand  Command builder
   */
  public static void register(LiteralArgumentBuilder<CommandSource> subCommand) {
    subCommand.requires(source -> source.getEntity() instanceof AbstractClientPlayerEntity)
              .then(Commands.argument("id", ResourceLocationArgument.resourceLocation()).suggests(MantleClientCommand.REGISTERED_BOOKS)
                            .executes(ClearBookCacheCommand::runBook))
              .executes(ClearBookCacheCommand::runAll);
  }

  /**
   * Runs the book-test command for specific book
   * @param context  Command context
   * @return  Integer return
   * @throws CommandSyntaxException if sender is not a player
   */
  private static int runBook(CommandContext<CommandSource> context) throws CommandSyntaxException {
    ResourceLocation book = ResourceLocationArgument.getResourceLocation(context, "id");
    clearBookCache(book);
    return 0;
  }

  /**
   * Runs the book-test command
   * @param context  Command context
   * @return  Integer return
   * @throws  CommandSyntaxException if sender is not a player
   */
  private static int runAll(CommandContext<CommandSource> context) throws CommandSyntaxException {
    clearBookCache(null);
    return 0;
  }

  private static void clearBookCache(@Nullable ResourceLocation book) {
    if (book != null) {
      BookData bookData = BookLoader.getBook(book);
      if (bookData != null) {
        bookData.reset();
      } else {
        BookCommand.bookNotFound(book);
      }
    } else {
      BookLoader.resetAllBooks();
    }
  }
}

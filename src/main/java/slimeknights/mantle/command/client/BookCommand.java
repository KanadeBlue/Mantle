package slimeknights.mantle.command.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.ResourceLocationArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import org.lwjgl.opengl.GL11;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.screen.book.BookScreen;
import slimeknights.mantle.command.MantleCommand;

import java.nio.file.Path;
import java.nio.file.Paths;

/** A command for different book operations, currently open and export_images */
public class BookCommand {
  private static final String BOOK_NOT_FOUND = "command.mantle.book_test.not_found";

  private static final String EXPORT_SUCCESS = "command.mantle.book.export.success";
  private static final String EXPORT_FAIL = "command.mantle.book.export.error_generic";
  private static final String EXPORT_FAIL_IO = "command.mantle.book.export.error_io";

  /**
   * Registers this sub command with the root command
   * @param subCommand  Command builder
   */
  public static void register(LiteralArgumentBuilder<CommandSource> subCommand) {
    subCommand.requires(source -> source.hasPermissionLevel(MantleCommand.PERMISSION_GAME_COMMANDS) && source.getEntity() instanceof PlayerEntity)
      .then(Commands.literal("open")
          .then(Commands.argument("id", ResourceLocationArgument.resourceLocation()).suggests(MantleClientCommand.REGISTERED_BOOKS)
            .executes(BookCommand::openBook)))
      .then(Commands.literal("export_images")
        .then(Commands.argument("id", ResourceLocationArgument.resourceLocation()).suggests(MantleClientCommand.REGISTERED_BOOKS)
          .then(Commands.argument("scale", IntegerArgumentType.integer(1, 16))
            .executes(BookCommand::exportImagesWithScale))
          .executes(BookCommand::exportImages)));
  }

  /**
   * Opens the specified book
   * @param context  Command context
   * @return  Integer return
   */
  private static int openBook(CommandContext<CommandSource> context) {
    ResourceLocation book = ResourceLocationArgument.getResourceLocation(context, "id");

    BookData bookData = BookLoader.getBook(book);
    if(bookData != null) {
      // Delay execution to ensure chat window is closed
      Minecraft.getInstance().deferTask(() ->
        bookData.openGui(new StringTextComponent("Book"), "", null, null)
      );
    } else {
      bookNotFound(book);
      return 1;
    }

    return 0;
  }

  /**
   * Renders all images in the book to files at specified scale
   * @param context  Command context
   * @return  Integer return
   */
  private static int exportImagesWithScale(CommandContext<CommandSource> context) {
    ResourceLocation book = ResourceLocationArgument.getResourceLocation(context, "id");
    int scale = context.getArgument("scale", Integer.class);

    return doExportImages(book, scale);
  }

  /**
   * Renders all images in the book to files
   * @param context  Command context
   * @return  Integer return
   */
  private static int exportImages(CommandContext<CommandSource> context) {
    ResourceLocation book = ResourceLocationArgument.getResourceLocation(context, "id");

    return doExportImages(book, 1);
  }

  /**
   * Renders all images in the book to files
   * @param book  Book to export
   * @param scale  Scale to export at
   * @return  Integer return
   */
  private static int doExportImages(ResourceLocation book, int scale) {
    BookData bookData = BookLoader.getBook(book);

    Path gameDirectory = Minecraft.getInstance().gameDir.toPath();
    Path screenshotDir = Paths.get(gameDirectory.toString(), "screenshots", "mantle_book", book.getNamespace(), book.getPath());

    if(bookData != null) {
      if(!screenshotDir.toFile().mkdirs() && !screenshotDir.toFile().exists()) {
        throw new CommandException(new TranslationTextComponent(EXPORT_FAIL_IO));
      }

      int width = BookScreen.PAGE_WIDTH_UNSCALED * 2 * scale;
      int height = BookScreen.PAGE_HEIGHT_UNSCALED * scale;
      float zFar = 1000.0F + 10000.0F * 3;

      bookData.load();
      BookScreen screen = new BookScreen(new StringTextComponent("Book"), bookData, "", null, null);
      screen.init(Minecraft.getInstance(), width / scale, height / scale);
      screen.drawArrows = false;
      screen.mouseInput = false;

      Matrix4f matrix = Matrix4f.orthographic(width, height, 1000.0F, zFar);
      RenderSystem.multMatrix(matrix);

//      MatrixStack stack = RenderSystem.getModelViewStack();
//      stack.pushPose();
      RenderSystem.pushMatrix();
//      stack.setIdentity();
      RenderSystem.loadIdentity();
//      stack.translate(0, 0, 1000F - zFar);
      RenderSystem.translatef(0, 0, 1000F - zFar);
//      stack.scale(scale, scale, 1);
      RenderSystem.scalef(scale, scale, 1);
//      RenderSystem.applyModelViewMatrix();
      RenderHelper.setupGui3DDiffuseLighting();

      Framebuffer target = new Framebuffer(width, height, true, Minecraft.IS_RUNNING_ON_MAC);
      target.enableStencil();

      try {
        target.bindFramebuffer(true);

        MatrixStack guiPose = new MatrixStack();

        do {
          RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.IS_RUNNING_ON_MAC);

          screen.tick();

          RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);

          guiPose.push();
          screen.render(guiPose, 0, 0, 0);
          guiPose.pop();

          try (NativeImage image = takeScreenshot(target)) {
            int page = screen.getPage_();
            String pageFormat = page < 0 ? "cover" : "page_" + page;
            Path path = Paths.get(screenshotDir.toString(), pageFormat + ".png");

            if (page == -1) { // the cover is half the width
              try (NativeImage scaled = new NativeImage(image.getFormat(), width / 2, height, false)) {
                copyRect(image, scaled, image.getWidth() / 2 - width / 4, 0, 0, 0,
                  width / 2, height);
                scaled.write(path);
              } catch (Exception e) {
                Mantle.logger.error("Failed to save screenshot", e);
                throw new CommandException(new TranslationTextComponent(EXPORT_FAIL));
              }
            } else {
              image.write(path);
            }
          } catch (Exception e) {
            Mantle.logger.error("Failed to save screenshot", e);
            throw new CommandException(new TranslationTextComponent(EXPORT_FAIL));
          }
        } while (screen.nextPage());
      } finally {
        RenderSystem.popMatrix();
//        stack.pop();
//        RenderSystem.applyModelViewMatrix();
        RenderSystem.defaultBlendFunc();
        target.unbindFramebuffer();
        target.deleteFramebuffer();
      }
    } else {
      bookNotFound(book);
      return 1;
    }

    PlayerEntity player = Minecraft.getInstance().player;
    if (player != null) {
      ITextComponent fileComponent = new StringTextComponent(screenshotDir.toString())
        .mergeStyle(TextFormatting.UNDERLINE)
        .modifyStyle(style -> style.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, screenshotDir.toAbsolutePath().toString())));
      player.sendStatusMessage(new TranslationTextComponent(EXPORT_SUCCESS, fileComponent), false);
    }
    return 0;
  }

  /**
   * Duplicate of {@link net.minecraft.util.ScreenShotHelper#createScreenshot(int, int, Framebuffer)}, but with transparency
   */
  private static NativeImage takeScreenshot(Framebuffer pFramebuffer) {
    int i = pFramebuffer.framebufferTextureWidth;
    int j = pFramebuffer.framebufferTextureHeight;
    NativeImage nativeimage = new NativeImage(i, j, false);
    RenderSystem.bindTexture(pFramebuffer.func_242996_f());
    nativeimage.downloadFromTexture(0, false);
    nativeimage.flip();
    return nativeimage;
  }

  /**
   * Minimalistic backport of NativeImage#copyRect
   */
  public static void copyRect(NativeImage src, NativeImage dst, int srcX, int srcY, int dstX, int dstY, int width, int height) {
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int color = src.getPixelRGBA(srcX + x, srcY + y);
        dst.setPixelRGBA(dstX + x, dstY + y, color);
      }
    }
  }

  public static void bookNotFound(ResourceLocation book) {
    PlayerEntity player = Minecraft.getInstance().player;
    if (player != null) {
      player.sendStatusMessage(new TranslationTextComponent(BOOK_NOT_FOUND, book).mergeStyle(TextFormatting.RED), false);
    }
  }
}

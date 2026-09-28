package slimeknights.mantle.command.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.command.SourcesCommand;

import java.util.ArrayList;
import java.util.List;

/** Command to list all sources for a file in a resource pack */
public class ClientSourcesCommand {
  /** List of subcommands to add */
  private static final List<ClientSourceFolder> FOLDERS = new ArrayList<>();

  /** Registers this command with the builder */
  public static void register(LiteralArgumentBuilder<FabricClientCommandSource> subCommand) {
    subCommand.then(ClientCommandManager.literal("path")
      .then(ClientCommandManager.argument("path", ResourceLocationArgument.id())
        // NOTE(porting): vanilla's ResourceLocationArgument#getId is typed to CommandSourceStack, which a Fabric
        //  client command source is not, so we read the argument generically instead.
        .executes(context -> run(context, context.getArgument("path", ResourceLocation.class)))));
    for (ClientSourceFolder source : FOLDERS) {
      subCommand.then(ClientCommandManager.literal(source.argument())
      .then(ClientCommandManager.argument("id", ResourceLocationArgument.id()).suggests(source.suggestionProvider())
          .executes(context -> run(context, source.folder(), context.getArgument("id", ResourceLocation.class), source.extension()))));
    }
  }

  /** Runs the command against the client's resource manager, reporting through the command source */
  private static int run(CommandContext<FabricClientCommandSource> context, ResourceLocation path) throws CommandSyntaxException {
    return SourcesCommand.run(Minecraft.getInstance().getResourceManager(), path, component -> context.getSource().sendFeedback(component));
  }

  /** Runs for the given folder and extension */
  private static int run(CommandContext<FabricClientCommandSource> context, String folder, ResourceLocation id, String extension) throws CommandSyntaxException {
    return run(context, id.withPath(folder + '/' + id.getPath() + extension));
  }


  /* Registering interesting folders */

  /** Folder to list sources for on the client */
  private record ClientSourceFolder(String argument, String folder, String extension, SuggestionProvider<FabricClientCommandSource> suggestionProvider) {}

  /** Suggests values using the passed suggestion provider */
  public static void register(String argument, String folder, String extension, SuggestionProvider<FabricClientCommandSource> suggestionProvider) {
    FOLDERS.add(new ClientSourceFolder(argument, folder, extension, suggestionProvider));
  }

  public static void registerMinecraft(String folder, SuggestionProvider<FabricClientCommandSource> suggestionProvider) {
    register(folder, folder, ".json", suggestionProvider);
  }
}

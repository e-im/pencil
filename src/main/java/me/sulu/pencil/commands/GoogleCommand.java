package me.sulu.pencil.commands;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.menu.EmbedPaginator;
import me.sulu.pencil.Pencil;
import me.sulu.pencil.util.customsearch.entity.Item;
import me.sulu.pencil.util.customsearch.entity.Result;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ExecutionException;


public class GoogleCommand extends SlashCommand {

  public GoogleCommand() {
    this.name = "google";
    this.help = "Searches the web";
    this.aliases = new String[]{"g", "ddg"};
    this.arguments = "<term>";
    this.options = Collections.singletonList(
      new OptionData(OptionType.STRING, "term", "Search term").setRequired(true)
    );
    this.guildOnly = true;
    this.botPermissions = new Permission[]{Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_MANAGE};
  }

  @Override
  protected void execute(SlashCommandEvent event) {
    String term = Objects.requireNonNull(event.getOption("term")).getAsString();
    try {
      InteractionHook interaction = event.replyEmbeds(new EmbedBuilder().setTitle("Searching...").build()).complete();
      handle(term, event.getUser()).display(interaction.retrieveOriginal().complete());
    } catch (IOException | InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void execute(CommandEvent event) {
    String term = event.getArgs();
    try {
      Message message = event.getMessage().replyEmbeds(new EmbedBuilder().setTitle("Searching...").build()).complete();
      handle(term, event.getAuthor()).paginate(message, 0);
    } catch (IOException | ExecutionException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  protected EmbedPaginator handle(String term, User user) throws IOException, ExecutionException, InterruptedException {
    Result result = Pencil.getCustomSearch().executeSearch(term);
    EmbedPaginator.Builder epb = new EmbedPaginator.Builder()
      .setEventWaiter(Pencil.getWaiter())
      .waitOnSinglePage(false)
      .setText("")
      .setFinalAction(m -> {
        try {
          m.clearReactions().queue();
        } catch (PermissionException | IllegalStateException ignored) {
        }
      });

    if (result.items() == null) {
      epb.addItems(
        new EmbedBuilder()
          .setColor(15167313)
          .setTitle(String.format("No Results Returned For %s", term))
          .setThumbnail("https://favicon.sulu.me/icon.png")
          .build()
      );
      return epb.build();
    }

    for (Item item : result.items()) {
      MessageEmbed embed = new EmbedBuilder()
        .setTitle(item.title(), item.link())
        .setColor(2508371)
        .setThumbnail("https://favicon.sulu.me/icon.png?url=" + item.displayLink())
        .setDescription(item.link() + "\n\n" + item.snippet().replace("\n", ""))
        .setFooter("Requested by " + user.getName() + " - About " + result.searchInformation().formattedTotalResults() + " results (" + result.searchInformation().formattedSearchTime() + " seconds)", user.getEffectiveAvatarUrl())
        .build();
      epb.addItems(embed);
    }
    return epb.build();
  }
}

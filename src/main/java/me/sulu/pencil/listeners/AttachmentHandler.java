package me.sulu.pencil.listeners;

import me.sulu.pencil.Pencil;
import me.sulu.pencil.util.Util;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// TODO: Upload .log.gz

public class AttachmentHandler extends ListenerAdapter {
  public static final Logger LOGGER = LogManager.getLogger();

  private static final List<String> allowedExtensions = Arrays.asList(
    "yml",
    "yaml",
    "properties",
    "json",
    "js",
    "java",
    "ts",
    "xml",
    "toml",
    "kt",
    "kts",
    "gradle",
    "sh",
    "bat",
    "patch",
    "diff",
    "log",
    "cpp",
    "md",
    "rst",
    "adoc",
    "asciidoc"
  );

  @Override
  public void onMessageReceived(MessageReceivedEvent event) {
    if (event.getMessage().getAttachments().size() >= 1) {
      for (Message.Attachment a : event.getMessage().getAttachments()) {
        if (a.getFileExtension() == null || a.getContentType() == null || !(allowedExtensions.contains(a.getFileExtension()) || a.getContentType().contains("text")))
          continue;

        try {
          String data = Pencil.getHTTP().sendAsync(
              HttpRequest.newBuilder(URI.create(a.getUrl())).build(),
              HttpResponse.BodyHandlers.ofString()
            )
            .thenApply(HttpResponse::body)
            .get(7, TimeUnit.SECONDS);

          try {
            event.getMessage().replyFormat("%s by %s: %s",
              a.getFileName(),
              event.getAuthor().getAsMention(),
              Util.pastebin(data, a.getContentType())).queue();
          } catch (Exception e) {
            LOGGER.warn("Failed to upload file!", e);
            this.fail(event);
          }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
          LOGGER.warn("Failed to download file!", e);
          this.fail(event);
        }
      }
    }
  }

  private void fail(MessageReceivedEvent event) {
    event.getMessage().reply(
      """
        **Please Use a Pastebin**
        Using a pastebin allows others to better provide you with support.
        Unfortunately, I was unable to automatically upload your file.
        """
    ).queue();
  }
}

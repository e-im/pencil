package me.sulu.pencil.listeners;

import com.github.mizosoft.methanol.MutableRequest;
import me.sulu.pencil.Pencil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// TODO: Upload .log.gz

public class AttachmentHandler extends ListenerAdapter {

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
            MutableRequest.GET(a.getUrl()),
            HttpResponse.BodyHandlers.ofString()
          )
            .thenApply(HttpResponse::body)
            .get(7, TimeUnit.SECONDS);

          String pasteUrl = pastebinFile(
            data,
            a.getFileName(),
            String.format("%s by %s (%s)", a.getFileName(), event.getAuthor().getAsTag(), event.getAuthor().getId()),
            String.format("Automatically generated paste from uploaded content sent by %s", event.getAuthor().getAsTag())
          );

          if (pasteUrl != null) {
            event.getMessage().reply(a.getFileName() + " by " + event.getAuthor().getAsMention() + ": " + pasteUrl).queue();
            return;
          }

          this.fail(event);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
          e.printStackTrace();
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

  private String pastebinFile(String data, String filename, String pasteName, String pasteDescription) {
    String pasteUrl = null;
    try {
      if (data.length() < 3500000) {
        try {
          pasteUrl = this.pasteGG(data, pasteName, pasteDescription, filename);
        } catch (Throwable t) {
          pasteUrl = bytebin(data);
        }
      } else {
        pasteUrl = bytebin(data);
      }
    } catch (Throwable ignored) {}
    return pasteUrl;
  }

  private String bytebin(String data) throws ExecutionException, InterruptedException, TimeoutException {
    return "https://p.sulu.me/" + Pencil.getHTTP().sendAsync(
      MutableRequest.POST(
        "https://p.sulu.me/post",
        HttpRequest.BodyPublishers.ofString(data)
      ),
      HttpResponse.BodyHandlers.ofString()
    )
      .thenApply(HttpResponse::body)
      .thenApply(JSONObject::new)
      .get(15, TimeUnit.SECONDS)
      .getString("key");
  }

  private String pasteGG(String data, String pasteName, String pasteDescription, String fileName) throws ExecutionException, InterruptedException, TimeoutException {
    return "https://paste.gg/" + Pencil.getHTTP().sendAsync(
      MutableRequest.POST(
        "https://api.paste.gg/v1/pastes",
        HttpRequest.BodyPublishers.ofString(
          new JSONObject()
            .put("name", pasteName)
            .put("description", pasteDescription)
            .put(
              "files",
              new JSONArray(
                new JSONObject[]{
                  new JSONObject()
                    .put("name", fileName)
                    .put(
                    "content",
                    new JSONObject()
                      .put("format", "text")
                      .put("value", data)
                  )
                }
              )
            )
            .toString()
        )
      )
        .header("Authorization", Pencil.getPasteggkey())
        .header("Content-Type", "application/json"),
      HttpResponse.BodyHandlers.ofString()
    )
      .thenApply(HttpResponse::body)
      .thenApply(JSONObject::new)
      .get(5, TimeUnit.SECONDS)
      .getJSONObject("result")
      .getString("id");
  }
}

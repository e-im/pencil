package me.sulu.pencil.listeners;

import me.sulu.pencil.util.Modmail;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;


public class DirectMessageHandler extends ListenerAdapter {

  @Override
  public void onMessageReceived(MessageReceivedEvent event) {
    if (event.getMessage().getAuthor().isBot()) return;
    if (event.isFromType(ChannelType.PRIVATE)) {
      StringBuilder message = new StringBuilder(event.getMessage().getContentDisplay());

      for (Message.Attachment a : event.getMessage().getAttachments()) {
        message.append(a.getUrl()).append("\n");
      }

      Modmail.send(message.toString(), event.getAuthor(), "Sent via direct message");
    }
  }
}

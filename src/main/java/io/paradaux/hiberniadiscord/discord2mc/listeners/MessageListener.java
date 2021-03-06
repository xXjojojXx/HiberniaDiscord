package io.paradaux.hiberniadiscord.discord2mc.listeners;

import io.paradaux.hiberniadiscord.HiberniaDiscord;
import io.paradaux.hiberniadiscord.discord2mc.api.Discord2McConfigurationCache;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class MessageListener extends ListenerAdapter {

    Discord2McConfigurationCache configurationCache;
    List<String> monitoredChannels;

    public MessageListener(Discord2McConfigurationCache configurationCache) {
        this.configurationCache = configurationCache;
        monitoredChannels = configurationCache.getMonitoredChannels();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Member member = event.getMember();

        if (!monitoredChannels.contains(event.getChannel().getId())) return; // If the message wasn't in a monitored channel, ignore it.
        if (event.getAuthor().isBot() && !configurationCache.isDoSendBotMessages()) return; // If ignore bots is enabled, respect it.

        HiberniaDiscord.newChain().async(() -> {
            String message = event.getMessage().getContentStripped(); // What the user sent
            if (member == null) return; // The message received event is triggered by the webhook, and thus has to ignore webhooks.

            // I've decided to implement commands in 3.1 rather than 3.0

            // if it is a command
//            if (message.startsWith(configurationCache.getCommandPrefix())) {
//
//                if (configurationCache.getCommandPrefix().equals("disabled")) return;
//                String[] args = message.split("\\s+");
//
//                switch (args[0]) {
//                    case "list": {
//                        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
//                        StringBuilder list = new StringBuilder();
//                        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
//
//                            if (list.length() > 0 ) {
//                                list.append(", ");
//                            }
//
//                            list.append(p.getDisplayName());
//                            event.getMessage().getChannel().sendMessage(list.toString()).queue();
//                        }
//
//
//
//                    }
//
//                    case "playercount": {
//                        event.getMessage().getChannel().sendMessage( Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers() + " players online").queue();
//                    }
//
//                    case "tps": {
//
//                    }
//
//                }
//
//            }

            // Getting the values of our built-in placeholders.
            String username = event.getAuthor().getName();
            String discriminator = event.getAuthor().getDiscriminator(); // The numbers after a user's username (after the #)
            String guildName = event.getGuild().getName();
            String channel = event.getMessage().getChannel().getName();
            String nickname = member.getNickname() != null ? member.getNickname() : ""; // If the user doesn't have a username, let it be empty string.

            // Get the user's highest role (i.e the one functionally in use)
            Role mainRole = getHighestFrom(member);
            String role = mainRole != null ? mainRole.getName() : "";

            // Set the chat message to the pre-formatted default message.
            String chatMessage = configurationCache.getMessageFormat();
            chatMessage = ChatColor.translateAlternateColorCodes('&', chatMessage);

            // Inject present placeholders.
            chatMessage = chatMessage.replace("%username%", username)
                                     .replace("%discriminator%", discriminator)
                                     .replace("%nickname%", nickname)
                                     .replace("%guildname%", guildName)
                                     .replace("%message%", message)
                                     .replace("%channel%", channel)
                                     .replace("%mainrole%", role);

            // Broadcast the message.
            Bukkit.broadcastMessage(chatMessage);
        }).execute();
    }

    @Nullable
    public Role getHighestFrom(Member member) {
        if (member == null) return null;
        List<Role> roles = member.getRoles();
        if (roles.isEmpty()) return null;

        return roles.stream().min((first, second) -> {
            if (first.getPosition() == second.getPosition()) return 0;
            return first.getPosition() > second.getPosition() ? -1 : 1;
        }).get();
    }


}

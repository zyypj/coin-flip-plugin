package gg.discord.mrkk.tadeu.coinflip.systems.response;

import gg.discord.mrkk.tadeu.coinflip.Main;
import gg.discord.mrkk.tadeu.coinflip.configuration.Configuration;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/*
Lightweight class to ask for a response for a player
@author: syncwrld
@website: https://github.com/syncwrld
 */
public class ResponseWaiter implements Listener {

    private final Main plugin;
    private final Configuration configuration;

    private final Map<Player, ResponseData> responses = new HashMap<>();

    public ResponseWaiter(Main plugin) {
        this.plugin = plugin;
        this.configuration = plugin.getConfiguration();
    }

    public void setup() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void ask(Player player, RequiredType type, Consumer<String> onComplete) {
        responses.put(player, new ResponseData(type, onComplete));
    }


    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!responses.containsKey(player)) return;

        event.setCancelled(true);
        ResponseData responseData = responses.get(player);
        String message = event.getMessage();

        if (message.equalsIgnoreCase("cancelar")) {
            responses.remove(player);
            player.sendMessage(configuration.getMessage("bet-chosen"));
            return;
        }

        if (!responseData.getType().isValid(message)) {
            player.sendMessage(responseData.getType().getErrorMessage());
            return;
        }

        responses.remove(player);
        responseData.getOnComplete().accept(message);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        responses.remove(event.getPlayer());
        plugin.getBetManager().removeBet(event.getPlayer().getName());
    }

    @Getter
    private static class ResponseData {
        private final RequiredType type;
        private final Consumer<String> onComplete;

        public ResponseData(RequiredType type, Consumer<String> onComplete) {
            this.type = type;
            this.onComplete = onComplete;
        }
    }

    @Getter
    public enum RequiredType {
        INTEGER("§cVocê deve digitar um número válido."),
        DOUBLE("§cVocê deve digitar um número decimal válido."),
        STRING("§cEntrada inválida."),
        CONFIRMATION("§cVocê deve digitar 'sim' para confirmar ou 'não' para cancelar.");

        private final String errorMessage;

        RequiredType(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public boolean isValid(String input) {
            try {
                switch (this) {
                    case INTEGER:
                        new BigInteger(input);
                        return true;
                    case DOUBLE:
                        Double.parseDouble(input);
                        return true;
                    case CONFIRMATION:
                        return input.equalsIgnoreCase("sim") || input.equalsIgnoreCase("não");
                    case STRING:
                    default:
                        return !input.trim().isEmpty();
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
}
package gg.discord.mrkk.tadeu.coinflip.systems.bet.inventories.listeners;

import gg.discord.mrkk.tadeu.coinflip.Main;
import gg.discord.mrkk.tadeu.coinflip.configuration.Configuration;
import gg.discord.mrkk.tadeu.coinflip.integrations.CashIntegration;
import gg.discord.mrkk.tadeu.coinflip.integrations.CoinsIntegration;
import gg.discord.mrkk.tadeu.coinflip.systems.bet.inventories.BetAnimationInventory;
import gg.discord.mrkk.tadeu.coinflip.systems.bet.inventories.BetInventory;
import gg.discord.mrkk.tadeu.coinflip.systems.bet.BetManager;
import gg.discord.mrkk.tadeu.coinflip.systems.response.ResponseHandler;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class InventoriesListener implements Listener {

    private final Main plugin;
    private final BetManager betManager;
    private final Configuration config;
    private final Map<UUID, BetInventory> inventoryMap = new HashMap<>();
    private final Map<UUID, BetAnimationInventory> protectedInventories = new HashMap<>();

    public InventoriesListener(Main plugin) {
        this.plugin = plugin;
        this.betManager = plugin.getBetManager();
        this.config = plugin.getConfiguration();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();

        if (protectedInventories.containsKey(player.getUniqueId())) {
            BetAnimationInventory inventory = protectedInventories.get(player.getUniqueId());
            inventory.openInventory(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        String inventoryTitle = config.getString("bet-menu.title");
        if (!inventory.getTitle().equalsIgnoreCase(inventoryTitle)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        BetInventory betInventory = inventoryMap.computeIfAbsent(player.getUniqueId(), uuid -> new BetInventory(plugin));

        ResponseHandler responseHandler = plugin.getResponseHandler();

        switch (slot) {
            case 36:
                betInventory.handleNavigationClick(player, inventory, 37);
                break;
            case 37:
                responseHandler.handleCoinsResponse(player);
                player.closeInventory();
                break;
            case 43:
                responseHandler.handleCashResponse(player);
                player.closeInventory();
                break;
            case 44:
                betInventory.handleNavigationClick(player, inventory, 43);
                break;
            case 40:
                betInventory.handleFilterClick(player, inventory);
                break;
            default:

                if (slot >= 10 && slot <= 24) {
                    if (clickedItem.getType() != Material.SKULL_ITEM) return;

                    processBetClick(player, clickedItem);
                    player.closeInventory();
                }
                break;
        }
    }

    private void processBetClick(Player challenger, ItemStack clickedItem) {
        String betItemNameTemplate = config.getString("bet-menu.item.name");
        String rawName = clickedItem.getItemMeta().getDisplayName();
        String creatorName = extractPlayerNameFromItem(rawName, betItemNameTemplate);

        if (creatorName == null) {
            challenger.sendMessage(config.getMessage("bet-not-found"));
            return;
        }

        Player creator = Bukkit.getPlayer(creatorName);
        BetManager.Bet bet = betManager.getBet(creatorName);

        if (bet != null) {
            double betValue = Double.parseDouble(bet.getValue());
            boolean isCash = bet.getType().equalsIgnoreCase("Cash");

            if (!validateBalances(challenger, creator, betValue, isCash)) {
                challenger.sendMessage(config.getMessage("insufficient-balance-challenger"));
                creator.sendMessage(config.getMessage("insufficient-balance-creator"));

                betManager.removeBet(creatorName);
                return;
            }

            if (betManager.processBet(challenger, bet, isCash)) {
                creator.sendMessage("§aA sua aposta foi aceita por " + challenger.getName() + ". Boa sorte!");
                challenger.sendMessage("§aA aposta foi aceita. Boa sorte!");
            }
        } else {
            challenger.sendMessage(config.getMessage("bet-not-found"));
            plugin.log("Aposta não encontrada para o criador: " + creatorName, true);
        }
    }

    private String extractPlayerNameFromItem(String rawName, String template) {
        String prefix = template.replace("{PLAYER}", "").replace("&", "§");
        if (rawName.startsWith(prefix)) {
            return rawName.replace(prefix, "").trim();
        }
        return null;
    }

    private boolean validateBalances(Player challenger, Player creator, double betValue, boolean isCash) {
        if (isCash) {
            CashIntegration cashIntegration = plugin.getCashIntegration();
            if (cashIntegration.getPpAPI().look(challenger.getUniqueId()) < betValue) {
                return false;
            }
            if (cashIntegration.getPpAPI().look(creator.getUniqueId()) < betValue) {
                return false;
            }
        } else {
            CoinsIntegration coinsIntegration = plugin.getCoinsIntegration();
            if (coinsIntegration.getEcon().getBalance(challenger) < betValue) {
                return false;
            }
            if (coinsIntegration.getEcon().getBalance(creator) < betValue) {
                return false;
            }
        }
        return true;
    }

    public void protectPlayer(Player player, BetAnimationInventory inventory) {
        protectedInventories.put(player.getUniqueId(), inventory);
    }

    public void unprotectPlayer(Player player) {
        protectedInventories.remove(player.getUniqueId());
    }
}
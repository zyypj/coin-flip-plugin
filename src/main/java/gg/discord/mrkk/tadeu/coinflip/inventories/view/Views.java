package gg.discord.mrkk.tadeu.coinflip.inventories.view;

import gg.discord.mrkk.tadeu.coinflip.Main;
import gg.discord.mrkk.tadeu.coinflip.inventories.BetInventory;
import lombok.Getter;

@Getter
public class Views {

    private BetInventory betInventory;

    public Views(Main plugin) {
        this.betInventory = new BetInventory(plugin);
    }
}

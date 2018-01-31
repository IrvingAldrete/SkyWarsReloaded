package com.walrusone.skywarsreloaded.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.database.DataStorage;
import com.walrusone.skywarsreloaded.objects.PlayerStat;
import com.walrusone.skywarsreloaded.utilities.Messaging;

public class ClearStatsCmd extends BaseCmd { 
	
	public ClearStatsCmd() {
		forcePlayer = true;
		cmdName = "clearstats";
		alias = new String[]{"cs", "cstats"};
		argLength = 2; //counting cmdName
	}

	@Override
	public boolean run() {
		Player bouncewarssPlayer = null;
		for (Player playerMatch: Bukkit.getOnlinePlayers()) {
			if (ChatColor.stripColor(playerMatch.getName()).equalsIgnoreCase(ChatColor.stripColor(args[1]))) {
				bouncewarssPlayer = playerMatch;
			}
		}
		
		if (bouncewarssPlayer != null) {
			PlayerStat pStat = PlayerStat.getPlayerStats(bouncewarssPlayer);
			pStat.clear();
			DataStorage.get().saveStats(pStat);
			player.sendMessage(new Messaging.MessageFormatter().setVariable("player", args[1]).format("command.stats-cleared"));
			return true;
		} else {
			new BukkitRunnable() {
				@Override
				public void run() {
					OfflinePlayer offlinePlayer = null;
					for (OfflinePlayer playerMatch: Bukkit.getOfflinePlayers()) {
						if (ChatColor.stripColor(playerMatch.getName()).equalsIgnoreCase(ChatColor.stripColor(args[1]))) {
							offlinePlayer = playerMatch;
						}
					}
					if (offlinePlayer != null) {
						final String uuid = offlinePlayer.getUniqueId().toString();
						new BukkitRunnable() {
							@Override
							public void run() {
									DataStorage.get().removePlayerData(uuid);
									DataStorage.get().updateTop();
									player.sendMessage(new Messaging.MessageFormatter().setVariable("player", args[1]).format("command.stats-cleared"));
							}
						}.runTask(SkyWarsReloaded.get());
					} else {
						new BukkitRunnable() {
							@Override
							public void run() {
								player.sendMessage(new Messaging.MessageFormatter().format("command.player-not-found"));
							}
						}.runTask(SkyWarsReloaded.get());
					}
				}
			}.runTaskAsynchronously(SkyWarsReloaded.get());
		}
		return true;
	}

}

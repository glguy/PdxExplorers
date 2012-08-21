package com.gmail.emertens.PdxExplorers;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.block.Sign;

public class PlayerListener implements Listener {

	PdxExplorers plugin;

	public PlayerListener(PdxExplorers p) {
		plugin = p;
	}

	@EventHandler(ignoreCancelled = true)
	public void onTeleport(PlayerTeleportEvent event) {
		if (event.getCause() != TeleportCause.UNKNOWN) {
			plugin.playerTeleported(event.getPlayer());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onDeath(PlayerDeathEvent event) {
		plugin.playerDied(event.getEntity());
	}

	@EventHandler(ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		Block block = event.getClickedBlock();
		BlockState state = block.getState();
		if (!(state instanceof Sign)) {
			return;
		}

		final Sign sign = (Sign)state;
		if (!PdxExplorers.isExplorerSign(sign.getLines())){
			return;
		}

		plugin.activateSign(event.getPlayer(), sign.getLine(1), sign.getLine(2));
	}

	@EventHandler(ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) {
		if (PdxExplorers.isExplorerSign(event.getLines())) {
			Player player = event.getPlayer();
			if (plugin.addExplorationSign(event.getLines(), player, event.getBlock().getLocation())) {
				player.sendMessage(ChatColor.RED + "Explorer sign created.");
			} else {
				event.setCancelled(true);
				player.sendMessage(ChatColor.RED + "Explorer sign creation is restricted.");
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlockAgainst();
		BlockState state = block.getState();
		if (state instanceof Sign) {
			Sign sign = (Sign)state;
			if (PdxExplorers.isExplorerSign(sign.getLines())) {
				event.setCancelled(true);
			}
		}
	}
}

package com.gmail.emertens.PdxExplorers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import org.bukkit.event.player.PlayerToggleFlightEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.block.Sign;

public class PlayerListener implements Listener {

	private final PdxExplorers plugin;

	public PlayerListener(PdxExplorers p) {
		plugin = p;
	}

	/**
	 * Block placements events are processed to allow a user
	 * to right-click on an explorer sign with a block in his
	 * hand without actually placing the block.
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		final Block block = event.getBlockAgainst();
		final BlockState state = block.getState();
		if (state instanceof Sign) {
			final Sign sign = (Sign)state;
			if (PdxExplorers.isExplorerSign(sign.lines())) {
				event.setCancelled(true);
			}
		}
	}

	/**
	 * Death events are processed to fail any progress a player
	 * has made when he dies and respawns at home.
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onDeath(PlayerDeathEvent event) {
		plugin.playerDied(event.getEntity());
	}
	
	/**
	 * Fly toggle events are processed to fail players who attempt
	 * to using flying to accelerate a route.
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onFly(PlayerToggleFlightEvent event) {
		if (event.isFlying()) {
			plugin.playerTeleported(event.getPlayer());
		}
	}
	
	/**
	 * This handler looks for command sign activation.
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		final Block block = event.getClickedBlock();
		final BlockState state = block.getState();
		if (!(state instanceof Sign)) {
			return;
		}

		final Sign sign = (Sign)state;
		final CommandSign cs = CommandSign.makeCommandSign(sign.lines());
		if (cs == null) return;

		try {
			plugin.activateSign(event.getPlayer(), cs);
		} catch (ExplorersException e) {
			event.getPlayer().sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
		}
	}
	
	/**
	 * This handler looks for uses of protected block and denies them
	 * when necessary.
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void protectButtons(PlayerInteractEvent event){
		final Block block = event.getClickedBlock();
		final Material m = block.getType();
		
		switch (m) {
		case STONE_BUTTON:
		case ACACIA_BUTTON:
		case BIRCH_BUTTON:
		case DARK_OAK_BUTTON:
		case OAK_BUTTON:
		case SPRUCE_BUTTON:
		case CRIMSON_BUTTON:
		case JUNGLE_BUTTON:
		case POLISHED_BLACKSTONE_BUTTON:
		case WARPED_BUTTON:
		case LEVER:
		case CHEST:
		case ENDER_CHEST:
			// These are the protected items
			break;
		default:
			return;
		}
		
		final Block above = block.getRelative(BlockFace.UP);
		if (above == null) return;
		
		final BlockState state = above.getState();
		if (!(state instanceof Sign)) return;

		final Sign sign = (Sign)state;
		final CommandSign cs = CommandSign.makeCommandSign(sign.lines());
		if (cs == null) return;
		
		final CommandSignType cst = cs.getSignType();
		if (cst != CommandSignType.LOCK_SIGN && cst != CommandSignType.ENROUTE_SIGN) return;
		
		try {
			plugin.allowUseLockedBlock(event.getPlayer(), cs.getRouteName(), cst);
		} catch (ExplorersException e) {
			event.getPlayer().sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
			event.setCancelled(true);
		}
	}

	/**
	 * This handler watches for creation of new signs in order to ensure
	 * that only a route owner or route admin is able to create new signs
	 * for any particular route. This protects against cheating.
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) {
		final Player player = event.getPlayer();
		try {
			CommandSign sign = CommandSign.makeCommandSign(event.lines());
			if (sign != null) {
				final Location signLocation = event.getBlock().getLocation();
				plugin.addExplorationSign(player, sign, signLocation);
			}
		} catch (ExplorersException e) {
			player.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
			event.setCancelled(true);
		}
	}

	/**
	 * This handler watches for teleportation and fails any route
	 * progress when teleportation occurs.
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onTeleport(PlayerTeleportEvent event) {
		if (event.getCause() != TeleportCause.UNKNOWN) {
			plugin.playerTeleported(event.getPlayer());
		}
	}
}

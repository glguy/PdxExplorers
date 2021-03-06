package com.gmail.emertens.PdxExplorers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * This class tracks the state of a route including winners and rewards. It
 * provides methods for formatting status messages for a route.
 * @author Eric Mertens
 *
 */
public class Route {
	private String owner;
	private Set<String> winners;
	private Map<Material,Integer> rewards;
	private int xpAward;

	public Route(final String owner) {
		this.owner = owner;
		winners = new HashSet<String>();
		rewards = Collections.synchronizedMap(new HashMap<Material, Integer>());
		xpAward = 0;
	}

	public Route() {
		owner = null;
		winners = new HashSet<String>();
		rewards = Collections.synchronizedMap(new HashMap<Material, Integer>());
		xpAward = 0;
	}

	@SuppressWarnings("unchecked")
	public Route(final Map<String, Object> map) {
		this.owner = (String) map.get("owner");

		winners = new HashSet<String>();

		ArrayList<Object> winnersArray = (ArrayList<Object>) map.get("winners");
		if (winnersArray != null) {
			for (Object s : winnersArray) {
				winners.add((String) s);
			}
		}

		rewards = (Map<Material,Integer>)map.get("rewards");
		if (rewards == null) {
			rewards = new HashMap<Material, Integer>();
		}
		rewards = Collections.synchronizedMap(rewards);
		
		Integer award = (Integer)map.get("xpaward");
		this.xpAward = award == null ? 0 : award;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		if (owner != null) { map.put("owner", owner); }
		if (!winners.isEmpty()) { map.put("winners", winners.toArray()); }
		if (!rewards.isEmpty()) { map.put("rewards", rewards); }
		if (xpAward != 0) { map.put("xpaward", xpAward); }
		return map;
	}

	public String getOwner(){
		return owner;
	}

	public boolean isOwner(final Player player) {
		return player != null && owner != null && owner.equalsIgnoreCase(player.getName());
	}

	private boolean hasWinners() {
		return !winners.isEmpty();
	}

	public void addWinner(final Player p) {
		// Record winner's name
		winners.add(p.getName());
		
		// Issue reward items
		for (Entry<Material, Integer> e : getRewards().entrySet()) {
			ItemStack stack = new ItemStack(e.getKey(), e.getValue());
			p.getInventory().addItem(stack);
			p.sendMessage("Exploration reward given: " + e.getValue() + " "
					+ e.getKey());
		}
		
		p.giveExp(xpAward);
	}

	public String pickWinner(int i) {
		if (hasWinners()) {
			String[] arr = winners.toArray(new String[]{});
			i %= arr.length + 1;
			if (i == arr.length) {
				return ChatColor.ITALIC + "Completed by";
			} else {
				return arr[i];
			}
		} else {
			return ChatColor.ITALIC + "No players";
		}
	}

	public String toChatString() {
		final String ownerString = owner == null ? ChatColor.ITALIC + "None" : owner;
		final String rewardsString = rewards.isEmpty() ? "" : 
				ChatColor.GRAY + ", " +
				ChatColor.RED + "Rewards: " +
				ChatColor.YELLOW + rewards.toString();
		final String xpString = xpAward == 0 ? "" :
			ChatColor.GRAY + ", " +
			ChatColor.RED + "Exp: " +
			ChatColor.YELLOW + Integer.toString(xpAward);
		
		return ChatColor.RED + "Owner: " + ChatColor.YELLOW + ownerString +
				ChatColor.GRAY + ", " +
				ChatColor.RED + "Winners: " + ChatColor.YELLOW + winners.size() +
				rewardsString +
				xpString;
	}

	public Map<Material,Integer> getRewards() {
		return rewards;
	}

	public void addReward(Material material, int amount) {
		synchronized (rewards) {
			Integer previous = rewards.get(material);
			if (previous == null) { previous = 0; }

			previous += amount;

			if (previous <= 0) {
				rewards.remove(material);
			} else {
				rewards.put(material , previous);
			}
		}
	}

	/**
	 * 
	 * @return Formatted list of players who have completed the route.
	 */
	public String toWinnersString() {
		if (winners.isEmpty()) {
			return ChatColor.RED + "None";
		} else {
			boolean first = true;
			StringBuilder builder = new StringBuilder();

			for (String p : winners) {
				if (!first) {
					builder.append(ChatColor.GRAY + ", ");
				} else {
					first = false;
				}

				builder.append(ChatColor.RESET + p);
			}
			return builder.toString();
		}
	}

	public void removeWinner(String playerName) {
		winners.remove(playerName);
	}

	public void setOwner(String name) {
		owner = name;
	}

	public boolean isWinner(Player player) {
		return winners.contains(player.getName());
	}

	public void setXpAward(int xp) {
		xpAward = xp;
	}
}

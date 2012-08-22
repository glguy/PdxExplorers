package com.gmail.emertens.PdxExplorers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public class Route {
	private String owner;
	private Set<String> winners;
	private Map<Material,Integer> rewards;

	public Route (final String owner) {
		this.owner = owner;
		winners = new HashSet<String>();
		rewards = Collections.synchronizedMap(new HashMap<Material, Integer>());
	}

	public Route () {
		owner = null;
		winners = new HashSet<String>();
		rewards = Collections.synchronizedMap(new HashMap<Material, Integer>());
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
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		if (owner != null) { map.put("owner", owner); }
		if (!winners.isEmpty()) { map.put("winners", winners.toArray()); }
		if (!rewards.isEmpty()) { map.put("rewards", rewards); }
		return map;
	}

	public String getOwner(){
		return owner;
	}

	public boolean isOwner(final String name) {
		return owner != null && owner.equalsIgnoreCase(name);
	}

	public Iterable<String> getWinners() {
		return winners;
	}

	public boolean hasWinners() {
		return !winners.isEmpty();
	}

	public void addWinner(String s) {
		winners.add(s);
	}

	public String pickWinner(int i) {
		if (hasWinners()) {
			String[] arr = winners.toArray(new String[]{});
			return arr[i % arr.length];
		} else {
			return null;
		}
	}

	public String toChatString() {
		final String ownerString = owner == null ? ChatColor.ITALIC + "None" : owner;
		final String rewardsString = rewards.isEmpty() ? ChatColor.ITALIC + "None" : rewards.toString();

		return ChatColor.RED + "Owner: " + ChatColor.YELLOW + ownerString +
				ChatColor.GRAY + ", " +
				ChatColor.RED + "Winners: " + ChatColor.YELLOW + winners.size() +
				ChatColor.GRAY + ", " +
				ChatColor.RED + "Rewards: " + ChatColor.YELLOW + rewardsString;
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
}

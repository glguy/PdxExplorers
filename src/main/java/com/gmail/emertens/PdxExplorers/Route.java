package com.gmail.emertens.PdxExplorers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * This class tracks the state of a route including winners and rewards. It
 * provides methods for formatting status messages for a route.
 * 
 * @author Eric Mertens
 *
 */
public class Route {
	private String owner;
	private Set<String> winners;
	private Map<Material, Integer> rewards;
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

		rewards = (Map<Material, Integer>) map.get("rewards");
		if (rewards == null) {
			rewards = new HashMap<Material, Integer>();
		}
		rewards = Collections.synchronizedMap(rewards);

		Integer award = (Integer) map.get("xpaward");
		this.xpAward = award == null ? 0 : award;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		if (owner != null) {
			map.put("owner", owner);
		}
		if (!winners.isEmpty()) {
			map.put("winners", winners.toArray());
		}
		if (!rewards.isEmpty()) {
			map.put("rewards", rewards);
		}
		if (xpAward != 0) {
			map.put("xpaward", xpAward);
		}
		return map;
	}

	public String getOwner() {
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

	public Component pickWinner(int i) {
		if (hasWinners()) {
			String[] arr = winners.toArray(new String[] {});
			i %= arr.length + 1;
			if (i == arr.length) {
				return Component.text("Completed by", Style.style(TextDecoration.ITALIC));
			} else {
				return Component.text(arr[i]);
			}
		} else {
			return Component.text("no players", Style.style(TextDecoration.ITALIC));
		}
	}

	public Component toChatString() {
		TextComponent.Builder builder = Component.text();

		builder.append(Component.text("Owner: ", NamedTextColor.RED));
		if (owner == null) {
			builder.append(Component.text("None", Style.style(TextDecoration.ITALIC)));
		} else {
			builder.append(Component.text(owner));
		}

		builder.append(Component.text(", ", NamedTextColor.GRAY));
		builder.append(Component.text("Winners: ", NamedTextColor.RED));
		builder.append(Component.text(winners.size(), NamedTextColor.YELLOW));

		if (!rewards.isEmpty()) {
			builder.append(Component.text(", ", NamedTextColor.GRAY));
			builder.append(Component.text("Rewards: ", NamedTextColor.RED));
			builder.append(Component.text(rewards.toString(), NamedTextColor.YELLOW));

		}
		if (xpAward != 0) {
			builder.append(Component.text(", ", NamedTextColor.GRAY));
			builder.append(Component.text("Exp: ", NamedTextColor.RED));
			builder.append(Component.text(Integer.toString(xpAward), NamedTextColor.YELLOW));
		}

		return builder.build();
	}

	public Map<Material, Integer> getRewards() {
		return rewards;
	}

	public void addReward(Material material, int amount) {
		synchronized (rewards) {
			Integer previous = rewards.get(material);
			if (previous == null) {
				previous = 0;
			}

			previous += amount;

			if (previous <= 0) {
				rewards.remove(material);
			} else {
				rewards.put(material, previous);
			}
		}
	}

	/**
	 * 
	 * @return Formatted list of players who have completed the route.
	 */
	public Component toWinnersString() {
		if (winners.isEmpty()) {
			return Component.text("None", NamedTextColor.RED);
		} else {
			boolean first = true;
			TextComponent.Builder builder = Component.text();

			for (String p : winners) {
				if (!first) {
					builder.append(Component.text(", ", NamedTextColor.GRAY));
				} else {
					first = false;
				}

				builder = builder.append(Component.text(p));
			}
			return builder.build();
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

package com.gmail.emertens.PdxExplorers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;

public class Route {
	private String owner;
	private Set<String> winners;
	
	public Route (final String owner) {
		this.owner = owner;
		winners = new HashSet<String>();
	}
	
	public Route () {
		owner = null;
		winners = new HashSet<String>();
	}
	
	@SuppressWarnings("unchecked")
	public Route(final Map<String, Object> map) {
		this.owner = (String) map.get("owner");
		
		winners = new HashSet<String>();

		for (Object s : ((ArrayList<Object>)map.get("winners"))) {
			winners.add((String)s);
		}
	}
	
	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("owner", owner);
		map.put("winners", winners.toArray());
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

		return ChatColor.RED + "Owner: " + ChatColor.YELLOW + ownerString +
				ChatColor.GRAY + ", " +
				ChatColor.RED + "Winners: " + ChatColor.YELLOW + winners.size();
	}
}
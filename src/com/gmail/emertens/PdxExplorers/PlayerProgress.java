package com.gmail.emertens.PdxExplorers;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;

public class PlayerProgress {
	
	private String token;
	private int waypoints;
	
	public PlayerProgress(String token) {
		this.token = token;
		waypoints = 0;
	}
	
	public String getToken() {
		return token;
	}
	
	public int getWaypoints() {
		return waypoints;
	}
	
	public void setWaypoints(final int waypoints) {
		this.waypoints = waypoints;
	}
	
	public Map<String,Object>toMap(){
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("token", token);
		map.put("waypoints", waypoints);
		return map;
	}

	public PlayerProgress(Map<String,Object> map) {
		token = (String)map.get("token");
		waypoints = (Integer)map.get("waypoints");
	}
	
	public String toChatString() {
		final String waypointText = waypoints == 0 ?
			 "" : ChatColor.GRAY + " [" + ChatColor.GOLD + waypoints + ChatColor.GRAY + "]";
		return ChatColor.GREEN + token + waypointText;
	}
}
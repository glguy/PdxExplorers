package com.gmail.emertens.PdxExplorers;

import java.util.HashMap;
import java.util.Map;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * This class tracks a player's progress along a route and
 * supports generating formatted messages.
 * @author Eric Mertens
 *
 */
public class PlayerProgress {

	private String token;
	private int waypoints;

	public PlayerProgress(String token) {
		this.token = token;
		waypoints = 0;
	}
	
	public PlayerProgress(String token, int waypoint) {
		this.token = token;
		this.waypoints = waypoint;
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

	public Component toChatString() {
		final Component waypointText = waypoints == 0
			? Component.text("")
			: Component.text(" [", NamedTextColor.GRAY)
			  .append(Component.text(waypoints, NamedTextColor.GOLD))
			  .append(Component.text("]", NamedTextColor.GRAY));
			
			return Component.text(token, NamedTextColor.GREEN).append(waypointText);
	}
}

package com.gmail.emertens.PdxExplorers;

public class CommandSign {
	
	private CommandSignType signType;
	private String routeName;
	private int waypointId;
	private static final String SIGN_HEADER = "[explorer]";
	
	public CommandSign(CommandSignType st, String n, int w) {
		signType = st;
		routeName = n;
		waypointId = w;
	}
	
	public static CommandSign makeCommandSign(String[] lines) {
		if (lines.length != 4) return null;
		if (!lines[0].equalsIgnoreCase(SIGN_HEADER)) return null;
		if (lines[2].isEmpty()) return null;
		
		int offset = lines[1].indexOf(": ");
		int w;
		String cmd;
		
		if (offset >= 0) {
			cmd = lines[1].substring(0, offset);
			try {
				w = Integer.parseInt(lines[1].substring(offset+2), 10);
			} catch (NumberFormatException e) {
				return null;
			}
		} else {
			cmd = lines[1];
			w = 1;
		}
		
		final CommandSignType st;
		
		if (cmd.equalsIgnoreCase("start")) {
			st = CommandSignType.START_SIGN;
		} else if (cmd.equalsIgnoreCase("view")) {
			st = CommandSignType.VIEW_SIGN;
		} else if (cmd.equalsIgnoreCase("finish")) {
			st = CommandSignType.FINISH_SIGN;
		} else if (cmd.equalsIgnoreCase("waypoint")) {
			st = CommandSignType.WAYPOINTS_SIGN;
		} else {
			return null;
		}
		
		return new CommandSign(st, lines[2], w);
	}

	public int getWaypoint() {
		return waypointId;
	}
	
	public CommandSignType getSignType() {
		return signType;
	}
	
	public String getRouteName() {
		return routeName;
	}
}

package com.gmail.emertens.PdxExplorers;

/**
 * This class represents one of the command signs used by players to
 * progress along a route.
 * @author Eric Mertens
 */
public class CommandSign {

	private final CommandSignType signType;
	private final String routeName;
	private int waypointId;
	
	// Sign labels
	private static final String SIGN_HEADER = "[explorer]";
	private static final String WAYPOINT = "waypoint";
	private static final String FINISH = "finish";
	private static final String VIEW = "view";
	private static final String START = "start";
	private static final String LOCK = "lock";
	private static final String ENROUTE = "enroute";
	
	public CommandSign(CommandSignType st, String n, int w) {
		signType = st;
		routeName = n;
		waypointId = w;
	}
	
	/**
	 * Parses the lines of a sign and returns a object if, and only if,
	 * the lines correspond to a command sign.
	 * 
	 * A valid sign has the following structure:
	 * <pre>
	 * "[explorer]"
	 * COMMAND[: NUMBER]
	 * ROUTE NAME
	 * </pre>
	 * 
	 * @param lines 4-element array of the lines on the sign
	 * @return sign object if, and only if, the lines correspond to a sign, null otherwise
	 */
	public static CommandSign makeCommandSign(String[] lines) {
		if (lines.length != 4) return null;
		if (!lines[0].equalsIgnoreCase(SIGN_HEADER)) return null;
		if (lines[2].isEmpty()) return null;
		
		final int offset = lines[1].indexOf(": ");
		final int w;
		final String cmd;
		
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
		
		if (cmd.equalsIgnoreCase(START)) {
			st = CommandSignType.START_SIGN;
		} else if (cmd.equalsIgnoreCase(VIEW)) {
			st = CommandSignType.VIEW_SIGN;
		} else if (cmd.equalsIgnoreCase(FINISH)) {
			st = CommandSignType.FINISH_SIGN;
		} else if (cmd.equalsIgnoreCase(WAYPOINT)) {
			st = CommandSignType.WAYPOINTS_SIGN;
		} else if (cmd.equalsIgnoreCase(LOCK)) {
			st = CommandSignType.LOCK_SIGN;
		} else if (cmd.equalsIgnoreCase(ENROUTE)) {
			st = CommandSignType.ENROUTE_SIGN;
		} else {
			return null;
		}
		
		return new CommandSign(st, lines[2], w);
	}

	/**
	 * Returns number of last way-point seen.
	 * @return number of last way-point seen
	 */
	public int getWaypoint() {
		return waypointId;
	}
	
	/**
	 * Returns sign type.
	 * @return sign type
	 */
	public CommandSignType getSignType() {
		return signType;
	}
	
	/**
	 * Returns route name.
	 * @return route name
	 */
	public String getRouteName() {
		return routeName.replaceAll(" ", "");
	}
}

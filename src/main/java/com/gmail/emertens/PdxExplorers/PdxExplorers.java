package com.gmail.emertens.PdxExplorers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * This class implements the core logic of the PdxExplorers plug-in. This
 * plug-in tracks in-game routes and allows players to progress through them.
 * 
 * @author Eric Mertens
 *
 */
public class PdxExplorers extends JavaPlugin {

	// Various messages
	private static final Component NO_CONSOLE_MSG = Component.text("Command not available to console",
			NamedTextColor.RED);
	private static final Component NO_PLAYER_MSG = Component.text("No such player.", NamedTextColor.RED);
	private static final Component SIGN_CREATED_MSG = Component.text("Explorer sign created.", NamedTextColor.GREEN);
	private static final Component NOT_STARTED_MSG = Component.text("You aren't on an exploration now.",
			NamedTextColor.RED);
	private static final Component ALREADY_EXPLORING_MSG = Component.text("You are already on this exploration.",
			NamedTextColor.YELLOW);
	private static final Component NO_FLYING_MSG = Component.text("You must land to start an exploration.",
			NamedTextColor.RED);
	private static final String EXPLORERS_COMMAND = "explorers";
	private static final Component EXPLORATION_FAILURE_MSG = Component.text("Exploration failed.", NamedTextColor.RED);
	private static final Component LOCK_OVERRIDE_MSG = Component.text("Lock ignored", NamedTextColor.YELLOW);

	// Permissions
	private static final String REVOKE_PERMISSION = "explorers.revoke";
	private static final String CREATE_PERMISSION = "explorers.create";
	private static final String REWARDS_PERMISSION = "explorers.rewards";
	private static final String DELETE_PERMISSION = "explorers.delete";
	private static final String GIVE_PERMISSION = "explorers.give";
	private static final String ASSIGN_PERMISSION = "explorers.assign";
	private static final String LOCK_OVERRIDE_PERMISSION = "explorers.lockexempt";

	private Map<String, PlayerProgress> explorers;
	private Map<String, Route> routes;
	private Set<Object[]> signs;

	// Common File objects
	private static final String SIGNS_DATA_FILE = "signs.yml";
	private static final String EXPLORERS_DATA_FILE = "explorers.yml";
	private static final String EXPLORATIONS_DATA_FILE = "explorations.yml";

	private YmlDataFile signsYml;
	private YmlDataFile explorersYml;
	private YmlDataFile explorationsYml;

	/**
	 * This counter increments on sign updates to track which name to show next.
	 */
	private int counter = 0;

	/**
	 * Set the player's active exploration to the given token.
	 * 
	 * @param name  Player's name
	 * @param token Exploration's token
	 */
	private void addPlayerToExploration(final Player player, String token) {

		Route route = getOrCreateRoute(token, null);
		route.addWinner(player);
		saveState();
	}

	/**
	 * This registers a new exploration sign upon creation by a player.
	 * 
	 * @param player   Player who created the sign
	 * @param sign     The sign just created
	 * @param location The location of the sign which was just created
	 * @throws ExplorersException
	 */
	public void addExplorationSign(Player player, CommandSign sign, Location location) throws ExplorersException {
		final String token = sign.getRouteName();
		final String name = player.getName();

		final Route r = getOrCreateRoute(token, name);
		if (!r.isOwner(player) && !player.hasPermission(CREATE_PERMISSION)) {
			throw new ExplorersPermissionException();
		}

		signs.add(locationToArray(location));
		player.sendMessage(SIGN_CREATED_MSG);
		saveState();
	}

	private Component explorationList(String token) throws ExplorersException {
		Route route = getExistingRoute(token);
		return Component.text(token, NamedTextColor.GREEN)
				.append(Component.text(": ", NamedTextColor.GRAY))
				.append(route.toWinnersString());
	}

	/**
	 * Predicate if these lines form a valid sign
	 * 
	 * @param sign An array of 4 signs on a sign
	 * @return True if these lines form a valid command sign
	 */
	public static boolean isExplorerSign(List<Component> lines) {
		return CommandSign.makeCommandSign(lines) != null;
	}

	private Component listExplorers() {
		TextComponent.Builder builder = Component.text();
		builder.append(Component.text("Explorers: ", NamedTextColor.YELLOW));

		boolean first = true;

		for (Entry<String, PlayerProgress> entry : explorers.entrySet()) {
			Player player = getServer().getPlayerExact(entry.getKey());
			if (player != null) {
				if (!first) {
					builder.append(Component.text(", ", NamedTextColor.GRAY));
				} else {
					first = false;
				}

				builder.append(player.displayName());
				builder.append(Component.text(": ", NamedTextColor.GRAY));
				builder.append(entry.getValue().toChatString());
			}
		}

		// After iterating through all the players it is possible that none were online
		if (first) {
			builder.append(Component.text("None", Style.style(TextDecoration.ITALIC)));
		}

		return builder.build();
	}

	@SuppressWarnings("unchecked")
	private void loadRoutes() {

		Map<String, Object> inputMap = new HashMap<String, Object>();

		inputMap = (Map<String, Object>) explorationsYml.load();
		routes = Collections
				.synchronizedMap(new HashMap<String, Route>());

		if (inputMap != null) {
			for (Entry<String, Object> e : inputMap.entrySet()) {
				Object v = e.getValue();
				String routeName = e.getKey();
				if (routeName.contains("�")) {
					continue;
				} // clean up old invalid data
				routes.put(routeName, new Route((Map<String, Object>) v));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void loadExplorers() {

		explorers = Collections.synchronizedMap(new HashMap<String, PlayerProgress>());

		Map<String, Object> inputMap = (Map<String, Object>) explorersYml.load();

		if (inputMap == null) {
			getLogger().warning("Using empty explorers list");
		} else {
			for (Entry<String, Object> e : inputMap.entrySet()) {
				Object v = e.getValue();
				explorers.put(e.getKey(), new PlayerProgress((Map<String, Object>) v));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void loadSigns() {
		ArrayList<Object> signsArray = (ArrayList<Object>) signsYml.load();
		signs = new HashSet<Object[]>();

		if (signsArray == null) {
			getLogger().warning("Using empty signs list");
		} else {
			for (Object a : signsArray) {
				signs.add(((ArrayList<Object>) a).toArray());
			}
		}
	}

	private void loadState() {
		loadSigns();
		loadExplorers();
		loadRoutes();
	}

	private static Object[] locationToArray(Location location) {
		return new Object[] { location.getWorld().getName(),
				location.getBlockX(), location.getBlockY(),
				location.getBlockZ() };
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {

		final Player player;
		if (sender instanceof Player) {
			player = (Player) sender;
		} else {
			player = null;
		}

		if (command.getName().equalsIgnoreCase(EXPLORERS_COMMAND)) {
			try {
				if (args.length == 0) {
					if (player == null) {
						sender.sendMessage(NO_CONSOLE_MSG);
					} else {
						playerStatusCommand(sender, player);
					}
				} else if (args[0].equalsIgnoreCase("players")) {
					sender.sendMessage(listExplorers());
				} else if (args[0].equalsIgnoreCase("routes")) {
					sender.sendMessage(routesList());
				} else if (args[0].equalsIgnoreCase("assign")) {
					switch (args.length) {
						case 2:
							assignCommand(sender, args[1], null, 0);
							break;
						case 3:
							assignCommand(sender, args[1], args[2], 0);
							break;
						case 4:
							assignCommand(sender, args[1], args[2], Integer.parseInt(args[3]));
							break;
						default:
							sender.sendMessage(
									Component.text("/explorers assign PLAYER [ROUTE [WAYPOINT]]", NamedTextColor.RED));
							break;
					}
				} else if (args[0].equalsIgnoreCase("route")) {
					if (args.length == 1) {
						sender.sendMessage(Component.text("Route requires additional command", NamedTextColor.RED));
					} else if (args[1].equalsIgnoreCase("show")) {
						if (args.length == 3) {
							Route r = getExistingRoute(args[2]);
							sender.sendMessage(r.toChatString());
						} else {
							sender.sendMessage(Component.text("/explorers route show ROUTE", NamedTextColor.RED));
						}
					} else if (args[1].equalsIgnoreCase("addreward")) {
						if (args.length == 5) {
							addRewardsCommand(sender, args[2], args[3], args[4]);
						} else {
							sender.sendMessage(Component.text("/explorers route addreward ROUTE MATERIAL QUANTITY",
									NamedTextColor.RED));
						}
					} else if (args[1].equalsIgnoreCase("setxp")) {
						if (args.length == 4) {
							setxpCommand(sender, args[2], Integer.parseInt(args[3]));
						} else {
							sender.sendMessage(
									Component.text("/explorers route setxp ROUTE QUANTITY", NamedTextColor.RED));
						}
					} else if (args[1].equalsIgnoreCase("delete")) {
						if (args.length == 3) {
							deleteRouteCommand(sender, player, args[2]);
						} else {
							sender.sendMessage(Component.text("/explorers route delete ROUTE", NamedTextColor.RED));
						}
					} else if (args[1].equalsIgnoreCase("revoke")) {
						if (args.length == 4) {
							revokeRouteCommand(sender, player, args[2], args[3]);
						} else {
							sender.sendMessage(
									Component.text("/explorers route revoke ROUTE PLAYER", NamedTextColor.RED));
						}
					} else if (args[1].equalsIgnoreCase("winners")) {
						if (args.length == 3) {
							listRouteWinnersCommand(sender, args[2]);
						} else {
							sender.sendMessage(Component.text("/explorers route winners ROUTE", NamedTextColor.RED));
						}
					} else if (args[1].equalsIgnoreCase("give")) {
						if (args.length == 4) {
							routeGiveCommand(sender, player, args[2], args[3]);
						} else {
							sender.sendMessage(
									Component.text("/explorers route give ROUTE PLAYER", NamedTextColor.RED));
						}
					} else {
						sender.sendMessage(Component.text("Unknown route command", NamedTextColor.RED));
					}
				} else if (args[0].equalsIgnoreCase("version")) {
					sender.sendMessage(getDescription().getFullName());
				} else {
					sender.sendMessage(Component.text("Bad command", NamedTextColor.RED));
				}
			} catch (ExplorersException e) {
				sender.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
			} catch (NumberFormatException e) {
				sender.sendMessage(Component.text("Failed to parse number", NamedTextColor.RED));
			}
			return true;
		}
		return false;
	}

	/**
	 * Change the experience reward for a route.
	 * 
	 * @param sender    Entity changing the reward
	 * @param routeName Name of route to change
	 * @param xp        New amount of experience awarded on completion
	 * @throws ExplorersException
	 */
	private void setxpCommand(CommandSender sender, String routeName,
			int xp) throws ExplorersException {
		if (!sender.hasPermission(REWARDS_PERMISSION)) {
			throw new ExplorersPermissionException();
		}

		Route r = getExistingRoute(routeName);
		r.setXpAward(xp);
		sender.sendMessage(Component.text("Success", NamedTextColor.GREEN));
	}

	private void assignCommand(CommandSender sender, String playerName,
			String routeName, int waypoint) throws ExplorersException {

		if (!sender.hasPermission(ASSIGN_PERMISSION)) {
			throw new ExplorersPermissionException();
		}

		Player p = getServer().getPlayer(playerName);
		if (p == null) {
			sender.sendMessage(NO_PLAYER_MSG);
		} else {

			if (routeName == null) {
				explorers.remove(playerName);
				p.sendMessage(Component.text("Exploration reset", NamedTextColor.RED));
			} else {
				getExistingRoute(routeName);
				explorers.put(p.getName(), new PlayerProgress(routeName, waypoint));

				if (waypoint == 0) {
					p.sendMessage(Component.text("Exploration route set to ", NamedTextColor.YELLOW)
							.append(Component.text(routeName, NamedTextColor.GREEN)));
				} else {
					p.sendMessage(
							Component.text("Exploration route set to ", NamedTextColor.YELLOW)
									.append(Component.text(routeName, NamedTextColor.GREEN))
									.append(Component.text(", waypoint ", NamedTextColor.YELLOW))
									.append(Component.text(waypoint, NamedTextColor.GREEN)));
				}
			}
			sender.sendMessage(Component.text("Success", NamedTextColor.GREEN));
		}

	}

	private void playerStatusCommand(CommandSender sender, final Player player) {
		final String name = player.getName();
		final PlayerProgress progress = explorers.get(name);
		final Component formattedToken = progress == null
				? Component.text("None", NamedTextColor.RED)
				: progress.toChatString();

		sender.sendMessage(Component.text("Exploration: ", NamedTextColor.YELLOW).append(formattedToken));
	}

	private void routeGiveCommand(CommandSender sender, Player player,
			String routeName, String targetName) throws ExplorersException {

		Route r = getExistingRoute(routeName);

		Player targetPlayer = getServer().getPlayer(targetName);
		if (targetPlayer == null) {
			sender.sendMessage(NO_PLAYER_MSG);
			return;
		}

		if (player != null && !r.isOwner(player) && !player.hasPermission(GIVE_PERMISSION)) {
			throw new ExplorersPermissionException();
		}

		r.setOwner(targetPlayer.getName());
		sender.sendMessage(
				Component.text("Gave " + routeName + " to ", NamedTextColor.GREEN)
						.append(targetPlayer.displayName()));
	}

	private void revokeRouteCommand(final CommandSender sender, final Player player,
			final String routeName, final String playerName) throws ExplorersException {

		Route r = getExistingRoute(routeName);

		if (player != null && !r.isOwner(player) && !player.hasPermission(REVOKE_PERMISSION)) {
			throw new ExplorersPermissionException();
		}
		r.removeWinner(playerName);
		sender.sendMessage(Component.text("Success", NamedTextColor.GREEN));
	}

	private void listRouteWinnersCommand(CommandSender sender, String routeName) throws ExplorersException {
		Route r = getExistingRoute(routeName);
		sender.sendMessage(r.toWinnersString());
	}

	private void deleteRouteCommand(final CommandSender sender, final Player player,
			final String routeName) throws ExplorersException {
		synchronized (routes) {
			Route r = getExistingRoute(routeName);

			if (player != null && !r.isOwner(player) && !player.hasPermission(DELETE_PERMISSION)) {
				throw new ExplorersPermissionException();
			}

			routes.remove(routeName);
			sender.sendMessage(Component.text("Success", NamedTextColor.GREEN));
		}
	}

	private static String canonicalRouteName(final String routeName) {
		return routeName.replaceAll(" ", "");
	}

	private Route getExistingRoute(String name) throws ExplorersException {
		Route r = routes.get(canonicalRouteName(name));
		if (r == null)
			throw new ExplorersNoRouteException();
		return r;
	}

	private Route getOrCreateRoute(String name, String newOwner) {
		synchronized (routes) {
			Route r = routes.get(canonicalRouteName(name));
			if (r == null) {
				r = new Route(newOwner);
				routes.put(name, r);
			}
			return r;
		}
	}

	private void addRewardsCommand(CommandSender sender, String route, String materialString, String amountString)
			throws NumberFormatException, ExplorersException {
		final Route r = getExistingRoute(route);

		if (!sender.hasPermission(REWARDS_PERMISSION)) {
			throw new ExplorersPermissionException();
		}

		final Material material = Material.matchMaterial(materialString);
		if (material == null) {
			sender.sendMessage(Component.text("Unable to parse material", NamedTextColor.RED));
			return;
		}

		final Integer amount;
		amount = Integer.parseInt(amountString, 10);

		r.addReward(material, amount);
		sender.sendMessage(Component.text("Reward updated", NamedTextColor.GREEN));
	}

	private TextComponent routesList() {
		TextComponent.Builder builder = Component.text();
		boolean first = true;
		builder.append(Component.text("Routes: "));

		for (String token : routes.keySet()) {
			if (!first)
				builder.append(Component.text(", ", NamedTextColor.GRAY));
			first = false;
			builder.append(Component.text(token, NamedTextColor.YELLOW));
		}

		return builder.build();
	}

	@Override
	public void onDisable() {
		saveState();
	}

	@Override
	public void onEnable() {

		// Configure data files
		final File dataFolder = getDataFolder();
		final File signsFile = new File(dataFolder, SIGNS_DATA_FILE);
		final File explorersFile = new File(dataFolder, EXPLORERS_DATA_FILE);
		final File explorationsFile = new File(dataFolder, EXPLORATIONS_DATA_FILE);
		signsYml = new YmlDataFile(signsFile);
		explorersYml = new YmlDataFile(explorersFile);
		explorationsYml = new YmlDataFile(explorationsFile);

		// Load saved state
		loadState();

		// Register listeners
		final PlayerListener listener = new PlayerListener(this);
		getServer().getPluginManager().registerEvents(listener, this);

		// Schedule update thread
		final Runnable signScroller = new Runnable() {
			public void run() {
				updateSigns();
			}
		};
		getServer().getScheduler().scheduleSyncRepeatingTask(this, signScroller, 25, 25);
	}

	/**
	 * This method fails the current exploration for a player and
	 * notifies that player of failure.
	 * 
	 * @param player The player whose progress will be reset
	 */
	private void playerFailed(Player player) {
		final String name = player.getName();
		boolean failure = false;
		synchronized (explorers) {
			if (explorers.containsKey(name)) {
				explorers.remove(name);
				failure = true;
			}
		}
		if (failure) {
			player.sendMessage(EXPLORATION_FAILURE_MSG);
		}
	}

	/**
	 * This method is called to update the game state when a player teleports.
	 * 
	 * @param player Player who teleported
	 */
	public void playerTeleported(Player player) {
		playerFailed(player);
	}

	/**
	 * This method is called to update the game state when a player dies.
	 * 
	 * @param player Player who died
	 */
	public void playerDied(Player player) {
		playerFailed(player);
	}

	/**
	 * Save route data to disk.
	 * 
	 * @throws IOException
	 */
	private void saveExplorations() throws IOException {
		final Map<String, Object> output = new HashMap<String, Object>();

		for (Entry<String, Route> e : routes.entrySet()) {
			output.put(e.getKey(), e.getValue().toMap());
		}
		explorationsYml.save(output);
	}

	/**
	 * Save player progress to disk.
	 * 
	 * @throws IOException
	 */
	private void saveExplorers() throws IOException {
		final Map<String, Object> output = new HashMap<String, Object>();

		for (Entry<String, PlayerProgress> e : explorers.entrySet()) {
			output.put(e.getKey(), e.getValue().toMap());
		}
		explorersYml.save(output);
	}

	/**
	 * Save sign locations to disk.
	 * 
	 * @throws IOException
	 */
	private void saveSigns() throws IOException {
		signsYml.save(signs.toArray());
	}

	/**
	 * Save all plug-in state to disk.
	 */
	public void saveState() {
		final File dataFolder = getDataFolder();

		if (!dataFolder.isDirectory()) {
			dataFolder.mkdirs();
		}

		try {
			saveExplorations();
		} catch (IOException e) {
			getLogger().warning("Unable to save explorations " + e.toString());
		}

		try {
			saveExplorers();
		} catch (IOException e) {
			getLogger().warning("Unable to save explorers " + e.toString());
		}

		try {
			saveSigns();
		} catch (IOException e) {
			getLogger().warning("Unable to save signs " + e.toString());
		}
	}

	/**
	 * Called when a player activates a command sign.
	 * 
	 * @param player   The player who activated the sign
	 * @param signType The command on the sign
	 * @param token    The route on the sign
	 * @throws ExplorersException
	 */
	public void activateSign(final Player player, final CommandSign sign) throws ExplorersException {
		switch (sign.getSignType()) {
			case FINISH_SIGN:
				activateFinishSign(player, sign.getRouteName(), sign.getWaypoint());
				break;
			case START_SIGN:
				activateStartSign(player, sign.getRouteName());
				break;
			case VIEW_SIGN:
				activateViewSign(player, sign.getRouteName());
				break;
			case WAYPOINTS_SIGN:
				activateWaypointSign(player, sign.getRouteName(), sign.getWaypoint());
				break;
			case LOCK_SIGN:
				activateLockSign(player, sign.getRouteName());
				break;
			case ENROUTE_SIGN:
				activateEnrouteSign(player, sign.getRouteName());
				break;
		}
	}

	/**
	 * Notify a player if they are on the current route and thus are able
	 * to use the protected block.
	 * 
	 * @param player    Player who is touching the sign
	 * @param routeName Route associated with the sign
	 * @throws ExplorersException
	 */
	private void activateEnrouteSign(Player player, String routeName) throws ExplorersException {
		PlayerProgress p = explorers.get(player.getName());

		if (p != null && p.getToken().equalsIgnoreCase(routeName)) {
			player.sendMessage(Component.text("Unlocked", NamedTextColor.GREEN));
		} else {
			player.sendMessage(Component.text("You are not on this route.", NamedTextColor.RED));
		}
	}

	/**
	 * Notify a player if they have completed this route and thus
	 * are able to use the protected block.
	 * 
	 * @param player    Player using the sign
	 * @param routeName Route associated with the sign
	 * @throws ExplorersException
	 */
	private void activateLockSign(Player player, String routeName) throws ExplorersException {

		Route r = getExistingRoute(routeName);

		if (r.isWinner(player)) {
			player.sendMessage(Component.text("Unlocked", NamedTextColor.GREEN));
		} else {
			player.sendMessage(Component.text("You have not completed this route.", NamedTextColor.RED));
		}
	}

	/**
	 * Attempt to progress the player along the current route.
	 * 
	 * @param player Player using the sign
	 * @param token  Route associated with the sign
	 * @param w      Way-point number associated with the sign
	 */
	private void activateWaypointSign(Player player, String token, int w) {
		PlayerProgress progress = explorers.get(player.getName());

		if (progress == null) {
			player.sendMessage(NOT_STARTED_MSG);
		} else if (!progress.getToken().equalsIgnoreCase(token)) {
			player.sendMessage(
					Component.text("You aren't on ", NamedTextColor.RED)
							.append(Component.text(token, NamedTextColor.GREEN))
							.append(Component.text(", you are on ", NamedTextColor.RED))
							.append(Component.text(progress.getToken(), NamedTextColor.GREEN)));
		} else {
			if (progress.getWaypoints() + 1 == w) {
				progress.setWaypoints(w);
				player.sendMessage(Component.text("Progress recorded!", NamedTextColor.GREEN));
			} else {
				player.sendMessage(
						Component.text("You need waypoint " + (progress.getWaypoints() + 1), NamedTextColor.RED));
			}
		}
	}

	/**
	 * Display a list of winners for a route
	 * 
	 * @param player Player using the view sign
	 * @param token  Route associated with the view sign
	 * @throws ExplorersException
	 */
	private void activateViewSign(final Player player, final String token) throws ExplorersException {
		player.sendMessage(explorationList(token));
	}

	/**
	 * Attempt to complete a route.
	 * 
	 * @param player Player using the finish sign
	 * @param token  Route associated with the finish sign
	 * @param w      Way-point number required to finish the route
	 */
	private void activateFinishSign(final Player player, final String token, final int w) {
		final String name = player.getName();
		final PlayerProgress progress = explorers.get(name);

		final Component message;
		boolean broadcast = false;

		if (progress == null) {
			message = NOT_STARTED_MSG;
		} else if (progress.getToken().equalsIgnoreCase(token)) {

			if (progress.getWaypoints() + 1 == w) {
				broadcast = true;
				message = player.displayName().append(
						Component.text(" completed the ", NamedTextColor.YELLOW))
						.append(
								Component.text(token, NamedTextColor.GREEN))
						.append(
								Component.text(" exploration!", NamedTextColor.YELLOW));

				explorers.remove(name);
				addPlayerToExploration(player, token);
				saveState();
			} else {
				message = Component.text("You need waypoint " + (progress.getWaypoints() + 1), NamedTextColor.RED);
			}
		} else {
			message = Component.text("You aren't on ", NamedTextColor.RED)
					.append(Component.text(token, NamedTextColor.GREEN))
					.append(Component.text(", you are on ", NamedTextColor.RED))
					.append(Component.text(progress.getToken(), NamedTextColor.GREEN));
		}

		if (broadcast) {
			getServer().broadcast(message);
		} else {
			player.sendMessage(message);
		}
	}

	/**
	 * Set up the player on a new route
	 * 
	 * @param player Player using the start sign
	 * @param token  Route associated with the start sign
	 */
	private void activateStartSign(final Player player, final String token) {
		Component message;
		final String name = player.getName();

		synchronized (explorers) {
			final PlayerProgress currentToken = explorers.get(name);

			if (currentToken != null && currentToken.getToken().equalsIgnoreCase(token)) {
				message = ALREADY_EXPLORING_MSG;
			} else if (player.isFlying()) {
				message = NO_FLYING_MSG;
			} else {
				if (currentToken == null) {
					message = Component.text("You have started exploring ", NamedTextColor.YELLOW)
							.append(Component.text(token, NamedTextColor.GREEN))
							.append(Component.text("!", NamedTextColor.YELLOW));
				} else {
					message = Component.text("You have switched to exploring ", NamedTextColor.YELLOW)
							.append(Component.text(token, NamedTextColor.GREEN))
							.append(Component.text("!", NamedTextColor.YELLOW));
				}
				explorers.put(name, new PlayerProgress(token));
			}
		}

		player.sendMessage(message);
		saveState();
	}

	/**
	 * Update the command signs to display the next player's name.
	 */
	private void updateSigns() {
		Set<Object[]> badSigns = new HashSet<Object[]>();

		for (Object[] location : signs) {
			final World world = getServer().getWorld((String) location[0]);
			final int x = (Integer) location[1];
			final int y = (Integer) location[2];
			final int z = (Integer) location[3];

			if (world == null)
				continue;
			if (world.isChunkLoaded(x / 16, z / 16)) {
				final Block block = world.getBlockAt(x, y, z);
				final BlockState state = block.getState();
				if (state instanceof Sign) {
					final Sign sign = (Sign) state;

					final Component token = sign.line(2);
					if (token instanceof TextComponent) {
						final TextComponent text = (TextComponent) token;
						final Route route = routes.get(text.content().replaceAll(" ", ""));

						if (route != null) {
							final Component nextName = route.pickWinner(counter);
							sign.line(3, nextName);
							sign.update();
						}

						continue; // Avoid adding this to badSigns

					}
				}
				badSigns.add(location);
			}
		}
		signs.removeAll(badSigns);
		counter++;
	}

	/**
	 * Perform checks when a player uses a route protected block.
	 * 
	 * @param player    Player using the block
	 * @param routeName Route protecting the block
	 * @param cst       Type of protection being used.
	 * @throws ExplorersException Exceptions will be generated when the route
	 *                            does not exist or when permission is denied.
	 */
	public void allowUseLockedBlock(Player player, String routeName, CommandSignType cst) throws ExplorersException {

		final boolean success;

		if (cst == CommandSignType.LOCK_SIGN) {
			final Route r = getExistingRoute(routeName);
			success = r.isWinner(player);
		} else {
			final PlayerProgress p = explorers.get(player.getName());
			success = p != null && p.getToken().equalsIgnoreCase(routeName);
		}

		if (success)
			return;
		if (player.hasPermission(LOCK_OVERRIDE_PERMISSION)) {
			player.sendMessage(LOCK_OVERRIDE_MSG);
			return;
		}
		throw new ExplorersPermissionException();
	}
}

package com.gmail.emertens.PdxExplorers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.ChatColor;
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

public class PdxExplorers extends JavaPlugin {

	private static final String BAD_FINISH_MSG = "You aren't on " + ChatColor.GREEN + "%s"
			+ ChatColor.RESET + ", you are on " + ChatColor.GREEN
			+ "%s";
	private static final String SUCCESS_MSG = ChatColor.YELLOW + "%s completed the "
			+ ChatColor.GREEN + "%s" + ChatColor.YELLOW
			+ " exploration!";
	private static final String NOT_STARTED_MSG = "You aren't on an exploration now.";
	private static final String ALREADY_EXPLORING_MSG = ChatColor.YELLOW
			+ "You are already on this exploration.";
	public static final String createPermission = "explorers.create";

	private Map<String, PlayerProgress> explorers;
	private Map<String, Route> routes;
	private Set<Object[]> signs;

	private static final String SIGNS_DATA_FILE = "signs.yml";
	private static final String EXPLORERS_DATA_FILE = "explorers.yml";
	private static final String EXPLORATIONS_DATA_FILE = "explorations.yml";

	
	private static final String EXPLORERS_COMMAND = "explorers";
	private static final String EXPLORATION_FAILURE_MSG = ChatColor.RED + "Exploration failed.";
	private static final String EXPLORATION_STARTED_MSG = ChatColor.YELLOW + "You have started exploring " + ChatColor.GREEN + "%s"
			+ ChatColor.YELLOW + "!";

	private static YmlDataFile signsYml;
	private static YmlDataFile explorersYml;
	private static YmlDataFile explorationsYml;


	/**
	 * This counter increments on sign updates to track which name to show next.
	 */
	private int counter = 0;

	private Runnable signScroller = new Runnable() {
		public void run() {
			updateSigns();
		}
	};

	/**
	 * Set the player's active exploration to the given token.
	 * @param name Player's name
	 * @param token Exploration's token
	 */
	private void addPlayerToExploration(final Player player, String token) {

		Route route = getOrCreateRoute(token, null);
		route.addWinner(player);
		saveState();
	}

	public void addExplorationSign(Player player, CommandSign sign, Location location) throws ExplorersException {
		final String token = sign.getRouteName();
		final String name = player.getName();

		Route r = getOrCreateRoute(token, name);
		if (!(r.isOwner(player) || player.hasPermission(PdxExplorers.createPermission))) {
			throw new ExplorersPermissionException();
		}
		
		signs.add(locationToArray(location));
		player.sendMessage(ChatColor.GREEN + "Explorer sign created.");
		saveState();
	}

	private String explorationList(String token) throws ExplorersException {
		Route route = getExistingRoute(token);
		return ChatColor.GREEN + token + ChatColor.GRAY + ": " + route.toWinnersString();
	}

	/**
	 * Predicate if these lines form a valid sign
	 * @param sign An array of 4 signs on a sign
	 * @return True if these lines form a valid command sign
	 */
	public static boolean isExplorerSign(final String[] lines) {
		return CommandSign.makeCommandSign(lines) != null;
	}

	private String listExplorers() {
		StringBuilder builder = new StringBuilder();
		boolean first = true;

		for (Entry<String, PlayerProgress> entry : explorers.entrySet()) {
			Player player = getServer().getPlayerExact(entry.getKey());
			if (player != null) {
				if (!first) {
					builder.append(ChatColor.GRAY + ", ");
				} else {
					first = false;
				}

				builder.append(ChatColor.RESET);
				builder.append(player.getDisplayName());
				builder.append(ChatColor.GRAY + ": ");
				builder.append(entry.getValue().toChatString());
			}
		}

		// After iterating through all the players it is possible that none were online
		if (first) {
			builder.append(ChatColor.ITALIC + "None");
		}

		return ChatColor.YELLOW + "Explorers: " + ChatColor.WHITE + builder;
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
				routes.put(routeName, new Route((Map<String,Object>)v));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void loadExplorers() {

		explorers = Collections.synchronizedMap(new HashMap<String, PlayerProgress>());

		Map<String, Object> inputMap = (Map<String, Object>)explorersYml.load();

		if (inputMap == null) {
			getLogger().warning("Using empty explorers list");
		} else {
			for (Entry<String,Object> e : inputMap.entrySet()) {
				Object v = e.getValue();
				explorers.put(e.getKey(), new  PlayerProgress((Map<String,Object>)v));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void loadSigns() {
		ArrayList<Object> signsArray = (ArrayList<Object>)signsYml.load();
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
				if (args.length == 0 && player != null) {
					final String name = player.getName();
					final PlayerProgress progress = explorers.get(name);
					final String formattedToken = progress == null ? ChatColor.RED
							+ "None"
							: progress.toChatString();

					sender.sendMessage(ChatColor.YELLOW + "Exploration: "
							+ formattedToken);
				} else if (args.length == 1
						&& args[0].equalsIgnoreCase("players")) {
					sender.sendMessage(listExplorers());
				} else if (args.length == 1
						&& args[0].equalsIgnoreCase("routes")) {
					sender.sendMessage(routesList());
				} else if (args.length == 3
						&& args[0].equalsIgnoreCase("route")
						&& args[1].equalsIgnoreCase("show")) {
					Route r = getExistingRoute(args[2]);
					sender.sendMessage(r.toChatString());
				} else if (args.length == 5
						&& args[0].equalsIgnoreCase("route")
						&& args[1].equalsIgnoreCase("addreward")) {
					addRewardsCommand(sender, args[2], args[3], args[4]);
				} else if (args.length == 3
						&& args[0].equalsIgnoreCase("route")
						&& args[1].equalsIgnoreCase("delete")) {
					deleteRouteCommand(sender, player, args[2]);
				} else if (args.length == 4
						&& args[0].equalsIgnoreCase("route")
						&& args[1].equalsIgnoreCase("revoke")) {
					revokeRouteCommand(sender, player, args[2], args[3]);
				} else if (args.length == 3
						&& args[0].equalsIgnoreCase("route")
						&& args[1].equalsIgnoreCase("winners")) {
					listRouteWinnersCommand(sender, args[2]);
				} else if (args.length == 4
						&& args[0].equalsIgnoreCase("route")
						&& args[1].equalsIgnoreCase("give")) {
					routeGiveCommand(sender, player, args[2], args[3]);
				} else if (args.length == 1
						&& args[0].equalsIgnoreCase("version")) {
					sender.sendMessage(getDescription().getVersion());
					
				// This has nothing to do with exploration
				} else if (player != null && args.length == 1
						&& args[0].equalsIgnoreCase("fly")) {
					player.sendMessage("Fly speed: " + player.getFlySpeed());
				} else if (player != null && args.length == 2
						&& args[0].equalsIgnoreCase("fly")
						&& player.hasPermission("explorers.setfly")) {
					Float speed = Float.parseFloat(args[1]);
					player.setFlySpeed(speed);
					player.sendMessage("Done");
				} else {
					sender.sendMessage(ChatColor.RED + "Bad command");
				}
			} catch (ExplorersException e) {
				sender.sendMessage(ChatColor.RED + e.getMessage());
			} catch (NumberFormatException e) {
				sender.sendMessage(ChatColor.RED + "Failed to parse number");

			}
			return true;
		}
		return false;
	}

	private void routeGiveCommand(CommandSender sender, Player player,
			String routeName, String targetName) throws ExplorersException {
		
		Route r = getExistingRoute(routeName);
		
		Player targetPlayer = getServer().getPlayer(targetName);
		if (targetPlayer == null) {
			sender.sendMessage(ChatColor.RED + "No such player.");
			return;
		}
		
		if (! ((player != null && r.isOwner(player)) || sender.hasPermission("explorers.give"))) {
			throw new ExplorersPermissionException();
		}
		
		r.setOwner(targetPlayer.getName());
		sender.sendMessage(ChatColor.GREEN + "Gave " + routeName + " to " + targetPlayer.getName());
	}

	private void revokeRouteCommand(CommandSender sender, Player player,
			String routeName, String playerName) throws ExplorersException {
		Route r = getExistingRoute(routeName);
		if ((player != null && r.isOwner(player))
				|| sender.hasPermission("explorers.revoke")) {
			r.removeWinner(playerName);
		}
		
	}

	private void listRouteWinnersCommand(CommandSender sender, String routeName) throws ExplorersException {
		Route r = getExistingRoute(routeName);
		sender.sendMessage(r.toWinnersString());
	}

	private void deleteRouteCommand(CommandSender sender, final Player player,
			String routeName) throws ExplorersException {
		synchronized (routes) {
			Route r = getExistingRoute(routeName);
			
			if (!(player != null && r.isOwner(player))
					&& !sender.hasPermission("explorers.delete")) {
				throw new ExplorersPermissionException();
			}
			routes.remove(routeName);
			sender.sendMessage(ChatColor.GREEN + "Success");
			
		}
	}
	
	private Route getExistingRoute(String name) throws ExplorersException {
		Route r = routes.get(name.replaceAll(" ", ""));
		if (r == null) throw new ExplorersNoRouteException();
		return r;
	}
	
	private Route getOrCreateRoute(String name, String newOwner) {
		synchronized (routes) {
			Route r = routes.get(name.replaceAll(" ", ""));
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

		if (!sender.hasPermission("explorers.rewards")) {
			throw new ExplorersPermissionException();
		}

		final Material material = Material.matchMaterial(materialString);
		if (material == null) {
			sender.sendMessage(ChatColor.RED + "Unable to parse material");
			return;
		}

		final Integer amount;
		amount = Integer.parseInt(amountString, 10);

		r.addReward(material, amount);
		sender.sendMessage(ChatColor.GREEN + "Reward updated");
	}

	private String routesList() {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		builder.append("Routes: ");

		for (String token : routes.keySet()) {
			if (!first) builder.append(ChatColor.GRAY + ", ");
			first = false;
			builder.append(ChatColor.YELLOW + token);
		}

		return builder.toString();
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

		// Schedule threads
		getServer().getScheduler().scheduleSyncRepeatingTask(this, signScroller, 25, 25);
	}

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
	 * @param player Account name of the player who teleported.
	 */
	public void playerTeleported(Player player) {
		playerFailed(player);
	}

	public void playerDied(Player player) {
		playerFailed(player);
	}

	public void removeExplorationSign(Location location) {
		signs.remove(locationToArray(location));
		saveState();
	}

	private void saveExplorations() throws IOException {
		final Map<String, Object> output = new HashMap<String, Object>();

		synchronized (routes) {
			for (Entry<String, Route> e : routes.entrySet()) {
				output.put(e.getKey(), e.getValue().toMap());
			}
		}
		explorationsYml.save(output);
	}
	private void saveExplorers() throws IOException {
		final Map<String, Object> output = new HashMap<String, Object>();
		synchronized (explorers) {
			for (Entry<String, PlayerProgress> e : explorers.entrySet()) {
				output.put(e.getKey(), e.getValue().toMap());
			}
		}
		explorersYml.save(output);
	}

	private void saveSigns() throws IOException {
		signsYml.save(signs.toArray());
	}

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
	 * @param player The player who activated the sign
	 * @param signType The command string on the sign
	 * @param token The route token on the sign
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
		}
	}

	private void activateWaypointSign(Player player, String token, int w) {
		PlayerProgress progress = explorers.get(player.getName());

		if (progress == null) {
			player.sendMessage(NOT_STARTED_MSG);
		} else if (!progress.getToken().equalsIgnoreCase(token)) {
			player.sendMessage(String.format(BAD_FINISH_MSG, token, progress.getToken()));
		} else {
			if (progress.getWaypoints() + 1 == w) {
				progress.setWaypoints(w);
				player.sendMessage(ChatColor.GREEN + "Progress recorded!");
			} else {
				player.sendMessage(ChatColor.RED + "You need waypoint " + (progress.getWaypoints()+1));
			}
		}
	}

	private void activateViewSign(final Player player, final String token) throws ExplorersException {
		player.sendMessage(explorationList(token));
	}

	private void activateFinishSign(final Player player, final String token, final int w) {
		final String name = player.getName();
		final PlayerProgress progress = explorers.get(name);

		final String message;
		boolean broadcast = false;

		if (progress == null) {
			message = NOT_STARTED_MSG;
		} else if (progress.getToken().equalsIgnoreCase(token)) {

			if (progress.getWaypoints() + 1 == w) {
				broadcast = true;
				message = String.format(SUCCESS_MSG, name, token);

				explorers.remove(name);
				addPlayerToExploration(player, token);
				saveState();
			} else {
				message = ChatColor.RED + "You need waypoint " + (progress.getWaypoints()+1);
			}
		} else {
			message = String.format(BAD_FINISH_MSG, token, progress.getToken());
		}

		if (broadcast) {
			getServer().broadcastMessage(message);
		} else {
			player.sendMessage(message);
		}
	}

	private void activateStartSign(final Player player, final String token) {
		String message;
		final String name = player.getName();

		synchronized (explorers) {
			final PlayerProgress currentToken = explorers.get(name);

			if (currentToken != null && currentToken.getToken().equalsIgnoreCase(token)) {
				message = ALREADY_EXPLORING_MSG;
			} else {
				if (currentToken == null) {
					message = String.format(EXPLORATION_STARTED_MSG, token);
				} else {
					message = ChatColor.YELLOW + "You have switched to exploring " + ChatColor.GREEN + token
							+ ChatColor.YELLOW + "!";;
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
			final World world = getServer().getWorld((String)location[0]);
			final int x = (Integer)location[1];
			final int y = (Integer)location[2];
			final int z = (Integer)location[3];

			if (world.isChunkLoaded(x/16, z/16)) {
				final Block block = world.getBlockAt(x, y, z);
				final BlockState state = block.getState();
				if (state instanceof Sign) {
					final Sign sign = (Sign) state;

					if (isExplorerSign(sign.getLines())) {
						String token = sign.getLine(2);
						final Route route = routes.get(token.replaceAll(" ", ""));

						if (route != null) {
							String nextName = route.pickWinner(counter);
							if (nextName == null) {
								nextName = ChatColor.ITALIC + "No one";
							}
							sign.setLine(3, nextName);
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
}

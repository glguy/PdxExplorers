package com.gmail.emertens.PdxExplorers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;

public class PdxExplorers extends JavaPlugin {

	private static final String NOT_STARTED_MSG = "You aren't on an exploration now.";
	private static final String ALREADY_EXPLORING_MSG = ChatColor.YELLOW
			+ "You are already on this exploration.";
	public static final String createPermission = "explorers.create";
	private Map<String, String> explorers;
	private Map<String, Set<String>> explorations;
	private Set<Object[]> signs;
	
	private static final String SIGN_HEADER = "[exporer]";
	private static final String START_SIGN_COMMAND = "start";
	private static final String VIEW_SIGN_COMMAND = "view";
	private static final String FINISH_SIGN_COMMAND = "finish";
	private static final String EXPLORERS_COMMAND = "explorers";
	private static final String EXPLORATION_FAILURE_MSG = ChatColor.RED + "Exploration failed.";
	private static final String EXPLORATION_STARTED_MSG = ChatColor.YELLOW + "You have started exploring " + ChatColor.GREEN + "%0"
			+ ChatColor.YELLOW + "!";
	
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
	private void addPlayerToExploration(String name, String token) {
		synchronized (explorations) {
			Set<String> players = explorations.get(token);

			if (players == null) {
				players = new HashSet<String>();
			}

			players.add(name);
			explorations.put(token, players);
		}
		saveState();
	}

	public void addExplorationSign(Location location) {
		signs.add(locationToArray(location));
		saveState();
	}

	private String explorationList(String token) {
		Set<String> players = explorations.get(token);
		if (players == null) {
			return ChatColor.YELLOW + "No one has finished this exploration.";
		} else {
			StringBuilder builder = new StringBuilder();

			builder.append(ChatColor.GREEN);
			builder.append(token);
			builder.append(ChatColor.GRAY);
			builder.append(": ");

			boolean first = true;
			for (String player : players) {
				if (!first)
					builder.append(ChatColor.GRAY + ", ");
				builder.append(ChatColor.RESET + player);
				first = false;
			}
			return builder.toString();
		}
	}
	
	/**
	 * Predicate if these lines form a valid sign
	 * @param sign An array of 4 signs on a sign
	 * @return True if these lines form a valid command sign
	 */
	public static boolean isExplorerSign(final String[] sign) {
		return sign[0].equalsIgnoreCase(SIGN_HEADER)
			&& (sign[1].equalsIgnoreCase(START_SIGN_COMMAND) ||
				sign[1].equalsIgnoreCase(FINISH_SIGN_COMMAND) ||
				sign[1].equalsIgnoreCase(VIEW_SIGN_COMMAND))
			&& !sign[2].isEmpty();
	}

	private String listExplorers() {
		StringBuilder builder = new StringBuilder();
		if (explorers.isEmpty()) {
			builder.append(ChatColor.ITALIC + "None");
		} else {
			boolean first = true;

			for (Entry<String, String> entry : explorers.entrySet()) {
				if (!first)
					builder.append(ChatColor.GRAY + ", ");
				String name = entry.getKey();
				String token = entry.getValue();
				builder.append(ChatColor.RESET + name + ChatColor.GRAY + ": "
						+ ChatColor.GREEN + token);
				first = false;
			}
		}
		return ChatColor.YELLOW + "Explorers: " + ChatColor.WHITE + builder;
	}
	
	@SuppressWarnings("unchecked")
	private void loadExplorations(File file) {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(FlowStyle.BLOCK);
		options.setAllowUnicode(true);
		Yaml yaml = new Yaml(options);

		Map<String, Object> inputMap = new HashMap<String, Object>();

		try {
			FileInputStream reader = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(reader, "UTF8");

			inputMap = yaml.loadAs(isr, Map.class);
			isr.close();
		} catch (IOException e) {
			getLogger().warning("Using empty signs list");
		}

		explorations = Collections.synchronizedMap(new HashMap<String, Set<String>>());
		for (Entry<String, Object> e : inputMap.entrySet()) {
			Set<String> set = new HashSet<String>();
			for (Object s : (ArrayList<Object>) e.getValue()) {
				set.add((String) s);
			}
			explorations.put(e.getKey(), set);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void loadExplorers(File file) {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(FlowStyle.BLOCK);
		options.setAllowUnicode(true);
		Yaml yaml = new Yaml(options);

		Map<String, String> inputMap = new HashMap<String, String>();

		try {
			FileInputStream reader = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(reader, "UTF8");

			inputMap = yaml.loadAs(isr, Map.class);
			isr.close();
		} catch (IOException e) {
			getLogger().warning("Using empty signs list");
		}

		explorers = Collections.synchronizedMap(inputMap);
	}
	
	@SuppressWarnings("unchecked")
	private void loadSigns(File file) {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(FlowStyle.BLOCK);
		options.setAllowUnicode(true);
		Yaml yaml = new Yaml(options);

		ArrayList<Object> signsArray = null;

		try {
			FileInputStream reader = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(reader, "UTF8");

			signsArray = (ArrayList<Object>) yaml.load(isr);
			isr.close();
		} catch (IOException e) {
			getLogger().warning("Using empty signs list");
		}

		signs = new HashSet<Object[]>();

		if (signsArray != null) {
			for (Object a : signsArray) {
				signs.add(((ArrayList<Object>)a).toArray());
			}
		}
	}
	
	private void loadState() {
		File dataFolder = getDataFolder();

		File signsFile = new File(dataFolder, "signs.yml");
		loadSigns(signsFile);
		
		File explorersFile = new File(dataFolder, "explorers.yml");
		loadExplorers(explorersFile);
		
		File explorationsFile = new File(dataFolder, "explorations.yml");
		loadExplorations(explorationsFile);
	}

	private static Object[] locationToArray(Location location) {
		return new Object[] {location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ()};
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {

		if (command.getName().equalsIgnoreCase(EXPLORERS_COMMAND)) {
			switch (args.length) {
			case 0:
				if (sender instanceof Player) {
					Player player = (Player) sender;
					String token = explorers.get(player.getName());
					if (token != null) {
						sender.sendMessage(ChatColor.YELLOW + "Exploration: "
								+ ChatColor.GREEN + token);
					} else {
						sender.sendMessage(ChatColor.YELLOW + "Exploration: "
								+ ChatColor.RED + "None");
					}
				}
				sender.sendMessage(listExplorers());
				break;
			case 1:
				if (sender.hasPermission("toggle")) {
					Player player = getServer().getPlayer(args[0]);
					if (player == null) {
						sender.sendMessage(ChatColor.RED + "No such player");
					} else {
						String name = player.getName();
						if (explorers.containsKey(name)) {
							explorers.remove(name);
							sender.sendMessage(ChatColor.YELLOW + name
									+ " is no longer an explorer.");
						} else {
							sender.sendMessage(ChatColor.YELLOW + name
									+ " is not an explorer.");
						}
					}
				} else {
					sender.sendMessage(ChatColor.RED
							+ "Explorer status change denied.");
				}
				break;
			case 2:
				if (sender.hasPermission("toggle")) {
					Player player = getServer().getPlayer(args[0]);
					if (player == null) {
						sender.sendMessage(ChatColor.RED + "No such player");
					} else {
						String name = player.getName();
						explorers.put(name, args[1]);
						sender.sendMessage(ChatColor.YELLOW + name
								+ " is now exploring " + args[1] + ".");
					}
				} else {
					sender.sendMessage(ChatColor.RED
							+ "Explorer status change denied.");
				}
				break;
			default:
				sender.sendMessage(ChatColor.RED + "Usage: /explorers");
				break;
			}

			return true;
		} else if (command.getName().equalsIgnoreCase("explored")) {
			return true;
		} else {
			return false;
		}

	}

	@Override
	public void onDisable() {
		saveState();
	}

	@Override
	public void onEnable() {
		loadState();

		PlayerListener listener = new PlayerListener(this);
		getServer().getPluginManager().registerEvents(listener, this);

		getServer().getScheduler().scheduleSyncRepeatingTask(this,
				signScroller, 25, 25);
	}

	private void playerFailed(Player player) {
		String name = player.getName();
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

	private void saveExplorations(File file) throws IOException {		
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(FlowStyle.BLOCK);
		options.setAllowUnicode(true);
		
		Map<String, Object[]> output = new HashMap<String, Object[]>();
		synchronized (explorations) {
			for (Entry<String, Set<String>> e : explorations.entrySet()) {
				output.put(e.getKey(), e.getValue().toArray());
			}
		}
		
		if (file.isFile()) {
			file.delete();
		}
		
		FileOutputStream writer = new FileOutputStream(file);
		 OutputStreamWriter osw = new OutputStreamWriter(writer, "UTF8");
		Yaml yaml = new Yaml(options);
		yaml.dump(output, osw);
		osw.close();
	}
	private void saveExplorers(File file) throws IOException {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(FlowStyle.BLOCK);
		options.setAllowUnicode(true);
		
		if (file.isFile()) {
			file.delete();
		}
		
		FileOutputStream writer = new FileOutputStream(file);
		 OutputStreamWriter osw = new OutputStreamWriter(writer, "UTF8");
		Yaml yaml = new Yaml(options);
		yaml.dump(explorers, osw);
		osw.close();
	}

	private void saveSigns(File file) throws IOException {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(FlowStyle.BLOCK);
		options.setAllowUnicode(true);
		
		if (file.isFile()) {
			file.delete();
		}
		
		FileOutputStream writer = new FileOutputStream(file);
		 OutputStreamWriter osw = new OutputStreamWriter(writer, "UTF8");

		Yaml yaml = new Yaml(options);
		yaml.dump(signs.toArray(), osw);
		osw.close();
	}

	public void saveState() {
		File dataFolder = getDataFolder();

		if (!dataFolder.isDirectory()) {
			dataFolder.mkdirs();
		}
		
		try {
		File explorationsFile = new File(dataFolder, "explorations.yml");
		saveExplorations(explorationsFile);
		} catch (IOException e) {
			getLogger().warning("Unable to save explorations " + e.toString());
		}
		
		try {
		File explorersFile = new File(dataFolder, "explorers.yml");
		saveExplorers(explorersFile);
		} catch (IOException e) {
			getLogger().warning("Unable to save explorers " + e.toString());
		}
		
		try {
			File signsFile = new File(dataFolder, "signs.yml");
			saveSigns(signsFile);
			} catch (IOException e) {
				getLogger().warning("Unable to save signs " + e.toString());
			}
	}

	/**
	 * Called when a player activates a command sign.
	 * @param player The player who activated the sign
	 * @param signType The command string on the sign
	 * @param token The route token on the sign
	 */
	public void activateSign(final Player player, final String signType, final String token) {
		if (signType.equalsIgnoreCase(START_SIGN_COMMAND)) {
			activateStartSign(player, token);
		} else if (signType.equalsIgnoreCase(FINISH_SIGN_COMMAND)) {
			activateFinishSign(player, token);
		} else if (signType.equalsIgnoreCase(VIEW_SIGN_COMMAND)) {
			activateViewSign(player, token);
		}
	}

	private void activateViewSign(final Player player, final String token) {
		player.sendMessage(explorationList(token));
	}

	private void activateFinishSign(final Player player, final String token) {
		final String name = player.getName();
		final String playersToken = explorers.get(name);
		String message;
		boolean broadcast = false;
		
		if (playersToken == null) {
			message = NOT_STARTED_MSG;
		} else if (playersToken.equalsIgnoreCase(token)) {
			broadcast = true;
			message =
					ChatColor.YELLOW + name + " completed the "
							+ ChatColor.GREEN + token + ChatColor.YELLOW
							+ " exploration!";
			explorers.remove(name);

			addPlayerToExploration(name, token);
			saveState();
		} else {
			message = "You aren't on " + ChatColor.GREEN + token
					+ ChatColor.RESET + ", you are on " + ChatColor.GREEN
					+ playersToken;
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
			final String currentToken = explorers.get(name);
			
			if (currentToken != null && currentToken.equalsIgnoreCase(token)) {
				message = ALREADY_EXPLORING_MSG;
			} else {
				if (currentToken == null) {
					message = String.format(EXPLORATION_STARTED_MSG, token);
				} else {
					message = ChatColor.YELLOW + "You have switched to exploring " + ChatColor.GREEN + token
							+ ChatColor.YELLOW + "!";;
				}
				explorers.put(name, token);
			}
		}
		
		player.sendMessage(message);
		saveState();
	}

	/**
	 * Update the command signs to display the next player's name.
	 */
	private void updateSigns() {
		for (Object[] location : signs) {
			final World world = getServer().getWorld((String)location[0]);
			final int x = (Integer)location[1];
			final int y = (Integer)location[2];
			final int z = (Integer)location[3];
			
			final Block block = world.getBlockAt(x, y, z);
			if (block.getChunk().isLoaded()) {
	
				final BlockState state = block.getState();
				if (state instanceof Sign) {
					final Sign sign = (Sign) state;
	
					if (isExplorerSign(sign.getLines())) {
						String token = sign.getLine(2);
						Set<String> players = explorations.get(token);
	
						if (players != null) {
							Object[] playerArray = players.toArray();
							final int offset = counter % playerArray.length;
							String nextName = (String) playerArray[offset];
							sign.setLine(3, nextName);
							sign.update();
						}
					}
				}
			}
		}
		counter++;
	}
}

package com.moneywasted.worldreboot;

import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class Main extends JavaPlugin {

	private static final String CONFIG_ENABLED = "Enabled";
	private static final String CONFIG_DISABLE_AFTER = "DisableAfterRegeneration";
	private static final String CONFIG_WORLDS = "WorldsToRegenerate";

	private static Logger logger;

	@Override
	public void onEnable() {
		new Metrics(this, 18036);

		saveDefaultConfig();
		FileConfiguration config = getConfig();

		logger = getLogger();

		if (!config.getBoolean(CONFIG_ENABLED)) {
			return;
		}

		List<String> worldsToRegenerate = config.getStringList(CONFIG_WORLDS);

		for (String world : worldsToRegenerate) {
			logger.warning(String.format("Regenerating World: %s", world));

			if (!deleteWorldFolderContents(world)) {
				logger.severe(String.format("Failed to Fully Regenerate World: %s", world));
			}
		}

		if (config.getBoolean(CONFIG_DISABLE_AFTER)) {
			logger.warning("DisableAfterRegeneration is TRUE, Disabling Plugin!");
			config.set(CONFIG_ENABLED, false);
			saveConfig();
		}
	}

	/**
	 * Deletes the contents of the world folder (but not the folder itself),
	 * since CraftBukkit may write uid files there on initialization.
	 *
	 * @param worldName the name of the world folder to clear
	 * @return {@code true} if all contents were deleted successfully, {@code false} otherwise
	 */
	private boolean deleteWorldFolderContents(String worldName) {
		Path worldFolder = getServer().getWorldContainer().toPath().resolve(worldName);

		if (!Files.isDirectory(worldFolder)) {
			return true; // Nothing to delete
		}

		boolean success = true;

		try (DirectoryStream<Path> entries = Files.newDirectoryStream(worldFolder)) {
			for (Path entry : entries) {
				if (!deleteRecursively(entry)) {
					success = false;
				}
			}
		} catch (IOException e) {
			logger.severe(String.format("Error Reading World Folder: %s", worldName));
			logger.severe(String.format("Error Message: %s", e.getMessage()));
			return false;
		}

		return success;
	}

	/**
	 * Recursively deletes a file or directory.
	 *
	 * @param path the path to delete
	 * @return {@code true} if deletion was fully successful, {@code false} otherwise
	 */
	private boolean deleteRecursively(Path path) {
		try (Stream<Path> walk = Files.walk(path)) {
			// Sort in reverse order so that files/children are deleted before their parent directories
			return walk.sorted(Comparator.reverseOrder())
				.map(p -> {
					try {
						Files.delete(p);
						return true;
					} catch (IOException e) {
						logger.severe(String.format("Failed to Delete: %s", p));
						logger.severe(String.format("Error Message: %s", e.getMessage()));
						return false;
					}
				})
				.reduce(true, Boolean::logicalAnd);
		} catch (IOException e) {
			logger.severe(String.format("Failed to Walk Path: %s", path));
			logger.severe(String.format("Error Message: %s", e.getMessage()));
			return false;
		}
	}
}
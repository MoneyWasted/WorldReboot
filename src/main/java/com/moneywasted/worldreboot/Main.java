package com.moneywasted.worldreboot;

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
	private static final List<String> CONFIG_DEFAULT_WORLDS = List.of("world", "world_nether", "world_the_end");

	@Override
	public void onEnable() {
		Logger pluginLogger = getLogger();
		FileConfiguration config = getConfig();

		config.addDefault(CONFIG_ENABLED, false);
		config.addDefault(CONFIG_DISABLE_AFTER, false);
		config.addDefault(CONFIG_WORLDS, CONFIG_DEFAULT_WORLDS);
		config.options().copyDefaults(true);
		saveConfig();

		if (!config.getBoolean(CONFIG_ENABLED)) {
			return;
		}

		List<String> worldsToRegenerate = config.getStringList(CONFIG_WORLDS);

		for (String world : worldsToRegenerate) {
			pluginLogger.warning(String.format("Regenerating World: %s", world));

			if (!deleteWorldFolderContents(world)) {
				pluginLogger.severe(String.format("Failed to Fully Regenerate World: %s", world));
			}
		}

		if (config.getBoolean(CONFIG_DISABLE_AFTER)) {
			pluginLogger.warning("DisableAfterRegeneration is TRUE, Disabling Plugin!");
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
			getLogger().severe(String.format("Error Reading World Folder: %s", worldName));
			getLogger().severe(String.format("Error Message: %s", e.getMessage()));
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
						getLogger().severe(String.format("Failed to Delete: %s", p));
						getLogger().severe(String.format("Error Message: %s", e.getMessage()));
						return false;
					}
				})
				.reduce(true, Boolean::logicalAnd);
		} catch (IOException e) {
			getLogger().severe(String.format("Failed to Walk Path: %s", path));
			getLogger().severe(String.format("Error Message: %s", e.getMessage()));
			return false;
		}
	}
}
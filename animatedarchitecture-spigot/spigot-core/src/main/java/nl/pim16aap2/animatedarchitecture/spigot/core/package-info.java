/**
 * The AnimatedArchitecture Spigot core module contains classes and interfaces specific to the Spigot platform.
 * <p>
 * The {@link nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin} class is the main
 * {@link org.bukkit.plugin.java.JavaPlugin} class that is instantiated on server startup. This class creates and
 * manages the {@link nl.pim16aap2.animatedarchitecture.spigot.core.IAnimatedArchitectureSpigotPlatform}.
 * <p>
 * Interaction with this plugin mostly happens through the
 * {@link nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform}. This can be obtained as follows:
 * <pre>{@code
 * private IAnimatedArchitecturePlatform getAnimatedArchitecturePlatform(JavaPlugin accessor)
 * {
 *     final @Nullable AnimatedArchitecturePlugin animatedArchitecturePlugin =
 *         (AnimatedArchitecturePlugin) Bukkit.getPluginManager().getPlugin("AnimatedArchitecture2");
 *     if (animatedArchitecturePlugin == null)
 *         // Handle this issue.
 *     return animatedArchitecturePlugin.getAnimatedArchitectureSpigotPlatform(accessor).orElseThrow();
 * }}</pre>
 * <p>
 * The 'accessor' parameter here is a reference to the plugin that is requesting access to the platform. This is used
 * for debugging purposes, as it allows AnimatedArchitecture to see which plugins have accessed the platform.
 * <p>
 * The platform will not be available before this plugin has been enabled or if some issue occurred while attempting to
 * enable it. How to deal with the AnimatedArchitecturePlugin being null or the platform not existing is up to the
 * plugin requesting access.
 */
@NonNullByDefault
package nl.pim16aap2.animatedarchitecture.spigot.core;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This package contains implementations for all event-related classes for the AnimatedArchitecture plugin for the
 * Spigot platform as well as utilities to call events.
 * <p>
 * Events are fired using Bukkit's {@link org.bukkit.event.Event} system. This means that creating listeners for these
 * events is done in the same way as for any other Bukkit event.
 *
 * <pre>{@code
 * @org.bukkit.event.EventHandler(ignoreCancelled = true)
 * public void onStructureCreation(StructurePrepareCreateEvent event)
 * {
 *     // Do something here when a structure is about to be created.
 * }}</pre>
 * <p>
 * Some events have two versions: One that has "prepare" in the name and one that does not. The "prepare" version is
 * fired before the action is performed and can be used to cancel the action. The non-"prepare" version is fired after
 * the action has been performed and cannot be cancelled.
 * <p>
 * This is done mostly for situations where even if the "prepare" event is not cancelled, the action may still fail. For
 * example, if a structure is about to be toggled, but some step in the process fails, the structure will not be
 * toggled. In this case, the "prepare" event will not be cancelled, the structure will not be toggled, and the
 * non-"prepare" event will not be fired.
 * <p>
 * Another example is situations where the action requires interaction with the database. The non-"prepare" event will
 * only be fired if both allowed by the "prepare" event and if the database interaction was successful.
 */
@NonNullByDefault
package nl.pim16aap2.animatedarchitecture.spigot.core.events;

import org.eclipse.jdt.annotation.NonNullByDefault;

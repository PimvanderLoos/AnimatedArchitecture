/**
 * This package contains classes responsible for providing compatibility with various third-party plugins.
 * <p>
 * The {@link nl.pim16aap2.bigdoors.spigot.core.compatiblity.FakePlayerCreator} can be used to create player objects
 * that appear online from offline players.
 * <p>
 * The {@link nl.pim16aap2.bigdoors.spigot.core.compatiblity.IProtectionCompat} interface is used to interact with
 * third-party plugins to determine if a structure can be animated or not based on whether a player is allowed to break
 * blocks in the old and the new location.
 */
@NonNullByDefault
package nl.pim16aap2.bigdoors.spigot.core.compatiblity;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This package contains command definitions and command factories used by this project, as well as various base command
 * classes and interfaces.
 * <p>
 * Construction of new commands is handled by the {@link nl.pim16aap2.bigdoors.core.commands.CommandFactory}, when can
 * be obtained using {@link nl.pim16aap2.bigdoors.core.api.IBigDoorsPlatform#getCommandFactory()}.
 * <p>
 * For situations where not all required data is available, the 'Delayed' version of the command can be used. For
 * example, when a player selects the option to change the 'blocksToMove' value of a portcullis in the GUI. In this
 * situation, you only know the door, but not yet the new 'blocksToMove' value. The
 * {@link nl.pim16aap2.bigdoors.core.commands.SetBlocksToMoveDelayed} command can then be used to request the player to
 * specify the new value.
 */
@NonNullByDefault
package nl.pim16aap2.bigdoors.core.commands;

import org.eclipse.jdt.annotation.NonNullByDefault;

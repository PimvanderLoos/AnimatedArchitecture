/**
 * This package contains command definitions and command factories used by this project, as well as various base command
 * classes and interfaces.
 * <p>
 * Construction of new commands is handled by the
 * {@link nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory}, when can be obtained using
 * {@link nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform#getCommandFactory()}.
 * <p>
 * A command is not executed directly when it is created. To execute a command, you must call
 * {@link nl.pim16aap2.animatedarchitecture.core.commands.BaseCommand#run()} on it. For example:
 * <pre>{@code
 * public void setBlocksToMove(
 *     CommandFactory commandFactory, ICommandSender commandSender, StructureRetriever retriever, int blocksToMove)
 * {
 *     commandFactory
 *         .newSetBlocksToMove(commandSender, retriever, blocksToMove)
 *         .run()
 *         .exceptionally(ex -> ...);
 * }}</pre>
 * <p>
 * For situations where not all required data is available, the 'Delayed' version of the command can be used. For
 * example, when a player selects the option to change the 'blocksToMove' value of a portcullis in the GUI. In this
 * situation, you only know the target structure, but not yet the new 'blocksToMove' value. The
 * {@link nl.pim16aap2.animatedarchitecture.core.commands.SetBlocksToMoveDelayed} command can then be used to request
 * the player to specify the new value.
 * <p>
 * For example:
 * <pre>{@code
 * public void setBlocksToMoveDelayed(
 *     CommandFactory commandFactory, ICommandSender commandSender, StructureRetriever retriever)
 * {
 *     commandFactory
 *         .getSetBlocksToMoveDelayed()
 *         .runDelayed(commandSender, retriever)
 *         .exceptionally(ex -> ...);
 * }</pre>
 */
@NonNullByDefault
package nl.pim16aap2.animatedarchitecture.core.commands;

import org.eclipse.jdt.annotation.NonNullByDefault;

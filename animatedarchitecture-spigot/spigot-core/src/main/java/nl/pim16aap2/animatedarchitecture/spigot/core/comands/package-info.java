/**
 * This package contains classes responsible for handling commands.
 * <p>
 * The {@link nl.pim16aap2.animatedarchitecture.spigot.core.comands.CommandManager} class is used to register commands
 * with the server. Commands can have multiple subcommands, each with their own set of required and optional arguments.
 * Argument parsers are used to parse user input into objects. Currently, parsers are provided for the following types:
 * Boolean, Direction, and StructureType in addition to the default parsers provided by <a
 * href="https://github.com/Incendo/cloud">Cloud</a>.
 * <p>
 * The execution of the commands is handled by the commands in the
 * {@link nl.pim16aap2.animatedarchitecture.core.commands} package.
 */
@NonNullByDefault
package nl.pim16aap2.animatedarchitecture.spigot.core.comands;

import org.eclipse.jdt.annotation.NonNullByDefault;

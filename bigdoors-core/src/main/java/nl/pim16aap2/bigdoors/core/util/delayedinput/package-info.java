/**
 * This package contains classes and interfaces related to handling and processing delayed input requests. The
 * {@link nl.pim16aap2.bigdoors.core.util.delayedinput.DelayedInputRequest} class represents a single delayed input
 * request, such as a request to a player to input a name or confirmation for a command.
 * <p>
 * The {@link nl.pim16aap2.bigdoors.core.util.delayedinput.DelayedStructureSpecificationInputRequest} class is a
 * subclass of {@link nl.pim16aap2.bigdoors.core.util.delayedinput.DelayedInputRequest} that is specifically designed
 * for handling requests to specify a structure to operate on when more than one structure fits a given identifier. For
 * example when a user tries to change a structure named "MyDoor", but there are 2 structures with that name.
 */
@NonNullByDefault
package nl.pim16aap2.bigdoors.core.util.delayedinput;

import org.eclipse.jdt.annotation.NonNullByDefault;

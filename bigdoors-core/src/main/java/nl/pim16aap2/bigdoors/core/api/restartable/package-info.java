/**
 * Contains classes and interfaces used to easily restart objects.
 * <p>
 * Any class that implements {@link nl.pim16aap2.bigdoors.core.api.restartable.IRestartable} or extends
 * {@link nl.pim16aap2.bigdoors.core.api.restartable.Restartable} can register itself with the
 * {@link nl.pim16aap2.bigdoors.core.api.restartable.RestartableHolder}.
 * <p>
 * When the restartable holder receives a 'initialize' instruction, all registered restartables will be initialized in
 * the order in which they were registered.
 * <p>
 * For a 'shutDown' instruction, all registered restartables will be shut down in reverse order.
 * <p>
 * A 'restart' instruction is a simple combination of both, where all registered restartables are first shut down, and
 * then initialized again.
 * <p>
 * Each class can define what is means to be restarted on its own.
 */
@NonNullByDefault
package nl.pim16aap2.bigdoors.core.api.restartable;

import org.eclipse.jdt.annotation.NonNullByDefault;

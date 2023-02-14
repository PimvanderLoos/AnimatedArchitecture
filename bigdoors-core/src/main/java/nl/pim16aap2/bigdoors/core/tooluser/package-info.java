/**
 * Contains the classes for the tool users system. This allows the user of the tool to create, edit, and inspect
 * structures.
 * <p>
 * Each {@link nl.pim16aap2.bigdoors.core.tooluser.ToolUser} has a {@link nl.pim16aap2.bigdoors.core.tooluser.Procedure}
 * that is executed every time the user uses the tool.
 * <p>
 * A {@link nl.pim16aap2.bigdoors.core.tooluser.Procedure} is a collection of one or more
 * {@link nl.pim16aap2.bigdoors.core.tooluser.Step}s. Each step has a
 * {@link nl.pim16aap2.bigdoors.core.tooluser.stepexecutor.StepExecutor} to process the user input and perform some kind
 * of action.
 */
@NonNullByDefault
package nl.pim16aap2.bigdoors.core.tooluser;

import org.eclipse.jdt.annotation.NonNullByDefault;

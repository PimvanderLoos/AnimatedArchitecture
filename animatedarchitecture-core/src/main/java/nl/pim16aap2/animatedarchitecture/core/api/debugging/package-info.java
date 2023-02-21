/**
 * This package contains classes and interfaces used for debugging.
 * <p>
 * The {@link nl.pim16aap2.animatedarchitecture.core.api.debugging.DebugReporter} can be used to generate a debug report. This
 * report is generated using all {@link nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable} subclasses that have been
 * registered with the {@link nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry}.
 * <p>
 * Each debuggable class can determine how to handle the generation of a debug report on its own.
 */
@NonNullByDefault
package nl.pim16aap2.animatedarchitecture.core.api.debugging;

import org.eclipse.jdt.annotation.NonNullByDefault;

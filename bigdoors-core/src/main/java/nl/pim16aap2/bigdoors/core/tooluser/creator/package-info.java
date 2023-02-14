/**
 * The creator package contains classes that are responsible for creating structures. These structures are created using
 * a {@link nl.pim16aap2.bigdoors.core.tooluser.Procedure} containing all the
 * {@link nl.pim16aap2.bigdoors.core.tooluser.Step}s required to provide the required data for a specific type of
 * structure.
 * <p>
 * The {@link nl.pim16aap2.bigdoors.core.tooluser.creator.Creator} base class contains a set of
 * {@link nl.pim16aap2.bigdoors.core.tooluser.Step.Factory} objects for common components of structures (e.g. for region
 * selection), but not a full procedure. Subclasses of the creator class will have to define that on their own.
 */
@NonNullByDefault
package nl.pim16aap2.bigdoors.core.tooluser.creator;

import org.eclipse.jdt.annotation.NonNullByDefault;

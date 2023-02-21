/**
 * Contains interfaces and classes for handling events related to AnimatedArchitecture structures. This includes events
 * for when a structure is created, opened, closed, or deleted. The events in this package are used by other packages to
 * trigger certain actions, such as playing audio.
 * <p>
 * To make it easy for external plugins to use these events as well, they should be fired using the native event handler
 * on the platform this project is running on.
 */
@NonNullByDefault
package nl.pim16aap2.animatedarchitecture.core.events;

import org.eclipse.jdt.annotation.NonNullByDefault;

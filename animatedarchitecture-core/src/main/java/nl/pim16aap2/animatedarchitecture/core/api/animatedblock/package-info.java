/**
 * This package contains various interfaces and classes used to define animated blocks.
 * <p>
 * An {@link nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock} represents a part of a structure
 * while it is being animated. Before and after the animation, these will be regular blocks.
 * <p>
 * The {@link nl.pim16aap2.animatedarchitecture.core.api.animatedblock.AnimatedHighlightedBlock} class represents a
 * specific type of animated block. These preview blocks are used to create an animation showing what the animation
 * would look like, without modifying the structure itself.
 * <p>
 * External plugins have two ways of interacting with the animations:
 * <ul>
 *     <li>
 *         {@link nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockHook}:
 * <p>
 *         The animated block hook allows hooking into each animated block in an animation.
 * <p>
 *         The hook interface allows overriding methods to react to events of the block, such as (re)spawning, moving,
 *         ticking, etc.
 *     </li>
 *     <li>
 *         {@link nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimationHook}:
 * <p>
 *         Animation hooks allow hooking into the animation itself.
 * <p>
 *         Like with the animated block hook, the animation hook interface allows overriding methods to react to events.
 *         Events for the animation hook include the start and end of the animation, every animation step, and more.
 *     </li>
 * </ul>
 */
@NonNullByDefault
package nl.pim16aap2.animatedarchitecture.core.api.animatedblock;

import org.eclipse.jdt.annotation.NonNullByDefault;

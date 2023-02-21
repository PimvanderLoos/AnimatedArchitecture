/**
 * This package contains the core logic for animating structures..
 * <p>
 * {@link nl.pim16aap2.animatedarchitecture.core.moveblocks.Animator} is responsible for actually moving the structure,
 * using a component implementing the {@link nl.pim16aap2.animatedarchitecture.core.moveblocks.IAnimationComponent}
 * interface to calculate the animation steps. Only a single component can be used per animation, which is used for
 * every step of the animation. Which specific component is used depends on the structure type being animated.
 * <p>
 * {@link nl.pim16aap2.animatedarchitecture.core.moveblocks.StructureActivityManager} keeps track of structures that are
 * being animated, or queued up to be animated.
 * <p>
 * External plugins can hook into the {@link nl.pim16aap2.animatedarchitecture.core.moveblocks.Animator} class using
 * either {@link nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimationHook}s or
 * {@link nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockHook}s. More information on that topic
 * can be found in {@link nl.pim16aap2.animatedarchitecture.core.api.animatedblock}.
 */
@NonNullByDefault
package nl.pim16aap2.animatedarchitecture.core.moveblocks;

import org.eclipse.jdt.annotation.NonNullByDefault;

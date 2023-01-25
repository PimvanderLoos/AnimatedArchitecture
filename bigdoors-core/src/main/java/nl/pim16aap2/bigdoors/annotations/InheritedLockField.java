package nl.pim16aap2.bigdoors.annotations;

import nl.pim16aap2.bigdoors.movable.AbstractMovable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Indicates that a {@link ReentrantReadWriteLock} field in an {@link AbstractMovable} subclass is inherited from its
 * super class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InheritedLockField
{
}

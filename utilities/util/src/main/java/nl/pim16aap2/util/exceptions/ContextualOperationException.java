package nl.pim16aap2.util.exceptions;

import lombok.experimental.StandardException;

/**
 * An exception that can be used to add context to exceptions.
 * <p>
 * This exception can be used to add context to exceptions. This is useful when you want to add context to exceptions
 * that are thrown in a chain of operations. For example, when you have a chain of operations that all throw exceptions,
 * you can use this exception to add context to the exceptions thrown by the operations.
 */
@StandardException
public final class ContextualOperationException extends RuntimeException
{
}

/**
 * Contains classes for loading structure types from external sources. This includes classes for initializing the
 * structure types and their components, as well as classes for managing the classpath and loading classes from external
 * sources.
 * <p>
 * Each structure is loaded from its own jar file. This jar file is supposed to have a manifest that contains the
 * following items:
 * <ul>
 *     <li>{@link java.util.jar.Attributes.Name#MAIN_CLASS} that refers to a subclass of
 *         {@link nl.pim16aap2.bigdoors.core.structures.StructureType}.
 * <p>
 *         For example: <pre>
 * Main-Class: nl.pim16aap2.bigdoors.structures.flag.StructureTypeFlag</pre>
 *     </li>
 *     <li>
 *         The "TypeName" parameter that defines the name of the structure type.
 * <p>
 *         For example: <pre>
 * Name: TypeName
 * TypeName: BigDoor</pre>
 *     </li>
 *     <li>
 *         The version of the structure type. This has to be an integer.
 * <p>
 *         For example: <pre>
 * Name: Version
 * Version: 1</pre>
 *     </li>
 *     <li>
 *         Optionally: A list of dependencies on other structure types. This has to be in the format of
 *         "{@code <typeName>(minVersion;maxVersion)}". Both the minimum and the maximum versions of the dependency are
 *         inclusive.
 * <p>
 *         For example:<pre>
 * Name: TypeDependencies
 * TypeDependencies: portcullis(1;5)</pre>
 *         This would require the "portcullis" type to be installed with {@code 1 <= version >= 5}.
 * <p>
 *         All dependencies of a class will be loaded before the class itself is loaded. Cyclic dependencies are not
 *         supported.
 *     </li>
 * </ul>
 */
@NonNullByDefault
package nl.pim16aap2.bigdoors.core.extensions;

import org.eclipse.jdt.annotation.NonNullByDefault;

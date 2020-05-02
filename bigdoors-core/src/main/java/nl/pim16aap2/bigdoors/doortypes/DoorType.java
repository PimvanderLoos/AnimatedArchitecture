package nl.pim16aap2.bigdoors.doortypes;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This class represents a type of Door. "Door" in this case, refers to any kind of animated object, so not necessarily
 * a door.
 *
 * @author Pim
 */
public class DoorType
{
    @NotNull
    protected final String pluginName;
    @NotNull
    protected final String typeName;
    protected final int typeVersion;
    @NotNull
    protected final List<Parameter> parameters;
    @NotNull
    protected final BiFunction<AbstractDoorBase.DoorData, Object[], Optional<AbstractDoorBase>> constructor;
    @NotNull
    protected final Function<AbstractDoorBase, Object[]> dataSupplier;

    /**
     * Constructs a new {@link DoorType}. Don't forget to register it using {@link DoorTypeManager#registerDoorType(DoorType)}.
     *
     * @param pluginName   The name of the plugin that owns this {@link DoorType}.
     * @param typeName     The name of this {@link DoorType}.
     * @param typeVersion  The version of this {@link DoorType}. Note that changing the version results in a completely
     *                     new {@link DoorType}, as far as the database is concerned. This fact can be used if the
     *                     parameters of the constructor for this type need to be changed.
     * @param parameters   List of {@link Parameter}s that describe which information is stored that is specific to this
     *                     {@link DoorType}. Do not include {@link AbstractDoorBase.DoorData}.
     * @param constructor  The factory method of the {@link DoorType}. This is what will be used to instantiate all
     *                     obects of this {@link DoorType}. Note that the order of the objects is defined by {@link
     *                     #parameters}.
     * @param dataSupplier The supplier that is used to retrieve all the data defined in {@link #parameters}. These
     *                     objects have to be in the same order!
     */
    protected DoorType(final @NotNull String pluginName, final @NotNull String typeName, final int typeVersion,
                       final @NotNull List<Parameter> parameters,
                       final @NotNull BiFunction<AbstractDoorBase.DoorData, Object[], Optional<AbstractDoorBase>> constructor,
                       final @NotNull Function<AbstractDoorBase, Object[]> dataSupplier)
    {
        this.pluginName = pluginName;
        this.typeName = typeName;
        this.typeVersion = typeVersion;
        this.parameters = parameters;
        this.constructor = constructor;
        this.dataSupplier = dataSupplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getPluginName() + ":" + getTypeName() + "-" + getVersion();
    }

    /**
     * Gets the name of the plugin that owns this {@link DoorType}.
     *
     * @return The name of the plugin that owns this {@link DoorType}.
     */
    @NotNull
    public final String getPluginName()
    {
        return pluginName;
    }

    /**
     * Gets the name of this {@link DoorType}.
     *
     * @return The name of this {@link DoorType}.
     */
    @NotNull
    public final String getTypeName()
    {
        return typeName;
    }

    /**
     * Gets the version of this {@link DoorType}. Note that changing the version creates a whole new {@link DoorType}
     * and you'll have to take care of the transition.
     *
     * @return The version of this {@link DoorType}.
     */
    public final int getVersion()
    {
        return typeVersion;
    }

    /**
     * Obtains all {@link Parameter}s used by this {@link DoorType}. Note that the order of the parameters must be the
     * same as the objects listed in {@link #constructor}.
     *
     * @return A list of all {@link Parameter}s used by this {@link DoorType}.
     */
    @NotNull
    public final List<Parameter> getParameters()
    {
        return parameters;
    }

    /**
     * Obtains the constructor for this {@link DoorType}. Note that the order of the array of objects must be the same
     * as {@link #parameters}.
     *
     * @return The constructor used to construct an object of this {@link DoorType}.
     */
    @NotNull
    public final BiFunction<AbstractDoorBase.DoorData, Object[], Optional<AbstractDoorBase>> getConstructor()
    {
        return constructor;
    }

    /**
     * Obtains the supplier of the data used by this {@link DoorType}.
     *
     * @return The data supplier of this {@link DoorType}.
     */
    @NotNull
    public final Function<AbstractDoorBase, Object[]> getDataSupplier()
    {
        return dataSupplier;
    }

    /**
     * Represents the various types of parameters accepted by the storage system.
     */
    public enum ParameterType
    {
        /**
         * Strings.
         */
        TEXT,

        /**
         * Signed integers.
         */
        INTEGER,

        /**
         * Floating point values.
         */
        REAL,

        /**
         * Binary blob of data.
         */
        BLOB,

//        // Supported types:
//        BOOLEAN,
//        SHORT,
//        INT,
//        FLOAT,
//        DOUBLE,
//        BIGDECIMAL,
//        STRING,
//        BYTES,
//        OBJECT,
//        TIME,
//        TIMESTAMP,
//        LONG,
//        DATE,
//        BYTES,

    }

    /**
     * Represents a parameter with a name, a type, and a value.
     */
    public static final class Parameter
    {
        @NotNull
        private final ParameterType parameterType;
        @NotNull
        private final String parameterName;

        public Parameter(final @NotNull ParameterType parameterType,
                         final @NotNull String parameterName)
        {
            this.parameterType = parameterType;
            this.parameterName = parameterName;
        }

        @NotNull
        public String getParameterName()
        {
            return parameterName;
        }

        @NotNull
        public ParameterType getParameterType()
        {
            return parameterType;
        }
    }
}

package nl.pim16aap2.bigdoors.doortypes;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * This class represents a type of Door. "Door" in this case, refers to any kind of animated object, so not necessarily
 * a door.
 *
 * @author Pim
 */
public abstract class DoorType
{
    @NotNull
    protected final String pluginName;
    @NotNull
    protected final String typeName;
    protected final int typeVersion;
    @NotNull
    protected final List<Parameter> parameters;
    @NotNull
    protected final String translationName;

    /**
     * Constructs a new {@link DoorType}. Don't forget to register it using {@link DoorTypeManager#registerDoorType(DoorType)}.
     *
     * @param pluginName  The name of the plugin that owns this {@link DoorType}.
     * @param typeName    The name of this {@link DoorType}.
     * @param typeVersion The version of this {@link DoorType}. Note that changing the version results in a completely
     *                    new {@link DoorType}, as far as the database is concerned. This fact can be used if the
     *                    parameters of the constructor for this type need to be changed.
     * @param parameters  List of {@link Parameter}s that describe which information is stored that is specific to this
     *                    {@link DoorType}. Do not include {@link AbstractDoorBase.DoorData}.
     */
    protected DoorType(final @NotNull String pluginName, final @NotNull String typeName, final int typeVersion,
                       final @NotNull List<Parameter> parameters)
    {
        this.pluginName = pluginName;
        this.typeName = typeName;
        this.typeVersion = typeVersion;
        this.parameters = parameters;
        translationName = "DoorType_" + toString();
    }

    /**
     * Instantiates a new {@link AbstractDoorBase} associated with this type.
     *
     * @param doorData The {@link AbstractDoorBase.DoorData} to instantiate the base door.
     * @param typeData The type-specific data for this {@link DoorType}. Must be in the order as defined by {@link
     *                 #getParameters()}.
     * @return A new {@link AbstractDoorBase} if one could be instantiated.
     *
     * @throws Exception
     */
    protected abstract Optional<AbstractDoorBase> instantiate(final @NotNull AbstractDoorBase.DoorData doorData,
                                                              final @NotNull Object... typeData)
        throws Exception;

    /**
     * Generates the type-specific data for this door type. Note that the data must be ordered in the same way as {@link
     * #getParameters()}.
     *
     * @param door The {@link AbstractDoorBase} to generate the data for.
     * @return An array of objects containing the type-specific data.
     *
     * @throws Exception
     */
    @NotNull
    protected abstract Object[] generateTypeData(final @NotNull AbstractDoorBase door)
        throws Exception;

    /**
     * Obtains the value of this type that represents the key in the translation system.
     *
     * @return The value of this type that represents the key in the translation system.
     */
    @NotNull
    public final String getTranslationName()
    {
        return translationName;
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public final String toString()
    {
        return getPluginName() + ":" + getTypeName() + ":" + getVersion();
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
     * same as the objects listed in {@link #instantiate(AbstractDoorBase.DoorData, Object...)}.
     *
     * @return A list of all {@link Parameter}s used by this {@link DoorType}.
     */
    @NotNull
    public final List<Parameter> getParameters()
    {
        return parameters;
    }

    /**
     * Gets the number of parameters used to instantiate a door of this type. See {@link #getParameters()}.
     *
     * @return The number of parameters used to instantiate a door of this type.
     */
    public final int getParameterCount()
    {
        return parameters.size();
    }

    /**
     * Attempts to instantiate an object of a {@link DoorType}.
     *
     * @param doorData The base data for the new door.
     * @param typeData The type-specific data for the door.
     * @return A new {@link AbstractDoorBase} if one was instantiated successfully.
     */
    public final Optional<AbstractDoorBase> constructDoor(final @NotNull AbstractDoorBase.DoorData doorData,
                                                          final @NotNull Object[] typeData)
    {
        if (typeData.length != getParameters().size())
        {
            PLogger.get().logException(new IllegalArgumentException(
                "DoorType " + toString() + " Expects " + getParameters().size() + " parameters but received: " +
                    typeData.length));
            return Optional.empty();
        }
        try
        {
            return instantiate(doorData, typeData);
        }
        catch (Exception e)
        {
            PLogger.get().logException(e);
        }
        return Optional.empty();
    }

    /**
     * Attempts to get all the type-specific data for a given {@link AbstractDoorBase}.
     *
     * @param door The {@link AbstractDoorBase} whose type-specific data to get.
     * @return An optional containing the type-specific data of the door, represented as an array of Objects.
     */
    public final Optional<Object[]> getTypeData(final @NotNull AbstractDoorBase door)
    {
        try
        {
            return Optional.of(generateTypeData(door));
        }
        catch (Exception e)
        {
            PLogger.get().logException(e);
        }
        return Optional.empty();
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

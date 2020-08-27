package nl.pim16aap2.bigdoors.doortypes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    /**
     * Gets the name of the plugin that owns this {@link DoorType}.
     *
     * @return The name of the plugin that owns this {@link DoorType}.
     */
    @Getter(onMethod = @__({@NotNull}))
    protected final String pluginName;

    /**
     * Gets the name of this {@link DoorType}. Note that this is always in lower case!
     *
     * @return The name of this {@link DoorType}.
     */
    @Getter(onMethod = @__({@NotNull}))
    protected final String simpleName;

    /**
     * Gets the version of this {@link DoorType}. Note that changing the version creates a whole new {@link DoorType}
     * and you'll have to take care of the transition.
     *
     * @return The version of this {@link DoorType}.
     */
    @Getter(onMethod = @__({@NotNull}))
    protected final int typeVersion;

    /**
     * Obtains all {@link Parameter}s used by this {@link DoorType}. Note that the order of the parameters must be the
     * same as the objects listed in {@link #instantiate(AbstractDoorBase.DoorData, Object...)}.
     *
     * @return A list of all {@link Parameter}s used by this {@link DoorType}.
     */
    @Getter(onMethod = @__({@NotNull}))
    protected final List<Parameter> parameters;

    /**
     * Obtains the value of this type that represents the key in the translation system.
     *
     * @return The value of this type that represents the key in the translation system.
     */
    @Getter(onMethod = @__({@NotNull}))
    protected final String translationName;

    /**
     * Gets a list of all theoretically valid {@link RotateDirection} for this given type. It does NOT take the physical
     * aspects of the {@link AbstractDoorBase} into consideration. Therefore, the actual list of valid {@link
     * RotateDirection}s is most likely going to be a subset of those returned by this method.
     *
     * @return A list of all valid {@link RotateDirection} for this given type.
     */
    @Getter(onMethod = @__({@NotNull}))
    private final List<RotateDirection> validOpenDirections;

    /**
     * Constructs a new {@link DoorType}. Don't forget to register it using {@link DoorTypeManager#registerDoorType(DoorType)}.
     *
     * @param pluginName  The name of the plugin that owns this {@link DoorType}.
     * @param simpleName  The 'simple' name of this {@link DoorType}. E.g. "Flag", or "Windmill".
     * @param typeVersion The version of this {@link DoorType}. Note that changing the version results in a completely
     *                    new {@link DoorType}, as far as the database is concerned. This fact can be used if the
     *                    parameters of the constructor for this type need to be changed.
     * @param parameters  List of {@link Parameter}s that describe which information is stored that is specific to this
     *                    {@link DoorType}. Do not include {@link AbstractDoorBase.DoorData}.
     */
    protected DoorType(final @NotNull String pluginName, final @NotNull String simpleName, final int typeVersion,
                       final @NotNull List<Parameter> parameters,
                       final @NotNull List<RotateDirection> validOpenDirections)
    {
        this.pluginName = pluginName;
        this.simpleName = simpleName.toLowerCase();
        this.typeVersion = typeVersion;
        this.parameters = parameters;
        this.validOpenDirections = validOpenDirections;
        translationName = "DOORTYPE_" + simpleName.toUpperCase();
    }

    /**
     * Checks if a given {@link RotateDirection} is valid for this type.
     *
     * @param rotateDirection The {@link RotateDirection} to check.
     * @return True if the provided {@link RotateDirection} is valid for this type, otherwise false.
     */
    public final boolean isValidOpenDirection(final @NotNull RotateDirection rotateDirection)
    {
        return validOpenDirections.contains(rotateDirection);
    }

    /**
     * Instantiates a new {@link AbstractDoorBase} associated with this type.
     *
     * @param doorData The {@link AbstractDoorBase.DoorData} to instantiate the base door.
     * @param typeData The type-specific data for this {@link DoorType}. Must be in the order as defined by {@link
     *                 #getParameters()}.
     * @return A new {@link AbstractDoorBase} if one could be instantiated.
     */
    @NotNull
    protected abstract Optional<AbstractDoorBase> instantiate(final @NotNull AbstractDoorBase.DoorData doorData,
                                                              final @NotNull Object... typeData);

    /**
     * Creates (and registers) a new {@link Creator} for this type.
     *
     * @param player The player who will own the {@link Creator}.
     * @return The newly created {@link Creator}.
     */
    @NotNull
    public abstract Creator getCreator(final @NotNull IPPlayer player);

    /**
     * Creates (and registers) a new {@link Creator} for this type.
     *
     * @param player The player who will own the {@link Creator}.
     * @param name   The name that will be given to the door.
     * @return The newly created {@link Creator}.
     */
    @NotNull
    public abstract Creator getCreator(final @NotNull IPPlayer player, final @Nullable String name);

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

    @NotNull
    @Override
    public final String toString()
    {
        return getPluginName() + ":" + getSimpleName() + ":" + getTypeVersion();
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
    @Value
    @AllArgsConstructor
    public static class Parameter
    {
        ParameterType parameterType;
        String parameterName;
    }
}

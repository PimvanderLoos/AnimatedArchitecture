package nl.pim16aap2.bigDoors.codegeneration;

import com.cryptomorin.xseries.XMaterial;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import nl.pim16aap2.bigDoors.NMS.NMSBlock;
import nl.pim16aap2.bigDoors.reflection.ReflectionBuilder;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Fence;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Set;

import static net.bytebuddy.implementation.MethodCall.construct;
import static net.bytebuddy.implementation.MethodCall.invoke;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static nl.pim16aap2.bigDoors.codegeneration.ReflectionRepository.*;

/**
 * Represents an implementation of a {@link ClassGenerator} to generate a subclass of {@link NMSBlock}.
 *
 * @author Pim
 */
final class NMSBlockClassGenerator extends ClassGenerator
{
    private static final @NotNull Class<?>[] CONSTRUCTOR_PARAMETER_TYPES =
        new Class<?>[]{World.class, int.class, int.class, int.class, classBlockBaseInfo,
                       asArrayType(classEnumDirectionAxis), asArrayType(classEnumBlockRotation)};

    public static final String FIELD_AXES_VALUES = "generated$axesValues";
    public static final String FIELD_ROTATION_VALUES = "generated$blockRotationValues";
    public static final String FIELD_CRAFT_BLOCK_DATA = "generated$craftBlockData";
    public static final String FIELD_BLOCK_DATA = "generated$blockData";

    /**
     * See {@link IUpdateMultipleFacing#intercept(IGeneratedNMSBlock, Object, Location, XMaterial)}
     */
    public static final String FIELD_LOCATION = "generated$loc";

    /**
     * See {@link IUpdateMultipleFacing#intercept(IGeneratedNMSBlock, Object, Location, XMaterial)}
     */
    public static final String FIELD_XMATERIAL = "generated$xMaterial";

    // Overrides
    public static final String METHOD_TO_STRING = "toString";
    public static final String METHOD_GET_ITEM = "getItem";

    /**
     * See {@link NMSBlock#rotateBlockUpDown(RotateDirection, DoorDirection)}
     */
    public static final String METHOD_ROTATION_UP_DOWN = "rotateBlockUpDown";

    /**
     * See {@link NMSBlock#canRotate()}
     */
    public static final String METHOD_CAN_ROTATE = "canRotate";

    /**
     * See {@link NMSBlock#deleteOriginalBlock()}
     */
    public static final String METHOD_DELETE_ORIGINAL_BLOCK = "deleteOriginalBlock";

    /**
     * See {@link NMSBlock#putBlock(Location)}
     */
    public static final String METHOD_PUT_BLOCK = "putBlock";

    public static final String METHOD_BLOCK_DATA_FROM_BUKKIT = "generated$constructBlockDataFromBukkit";
    public static final String METHOD_CHECK_WATERLOGGED = "generated$checkWaterLogged";
    public static final String METHOD_GET_MY_BLOCK_DATA = "generated$getMyBlockData";
    public static final String METHOD_UPDATE_MULTIPLE_FACING = "generated$updateCraftBlockDataMultipleFacing";
    public static final String METHOD_ROTATE_UP_DOWN = "generated$rotateBlockUpDown";
    public static final String METHOD_ROTATE = "generated$rotateBlockMethod";
    public static final String METHOD_ROTATE_CYLINDRICAL = "generated$rotateBlockCylindrical";

    public NMSBlockClassGenerator(@NotNull String mappingsVersion)
        throws Exception
    {
        super(mappingsVersion);
        generate();
    }

    @Override
    protected @NotNull Class<?>[] getConstructorArgumentTypes()
    {
        return CONSTRUCTOR_PARAMETER_TYPES;
    }

    @Override
    protected @NotNull String getBaseName()
    {
        return "NMSBlock";
    }

    @Override
    protected void generateImpl()
        throws Exception
    {
        DynamicType.Builder<?> builder = createBuilder(classBlockBase)
            .implement(org.bukkit.entity.FallingBlock.class, NMSBlock.class, IGeneratedNMSBlock.class);

        builder = addFields(builder);
        builder = addCTor(builder);
        builder = addBasicMethods(builder);
        builder = addPutBlockMethod(builder);
        builder = addRotateBlockMethod(builder);
        builder = addRotateBlockUpDownMethod(builder);
        builder = addUpdateMultipleFacingMethod(builder);
        builder = addRotateCylindricalMethod(builder);

        finishBuilder(builder);
    }

    private DynamicType.Builder<?> addCTor(DynamicType.Builder<?> builder)
    {
        final MethodCall getBlockAtLoc = invoke(methodGetBlockAtCoords).onArgument(0).withArgument(1, 2, 3);

        return builder
            .defineConstructor(Visibility.PUBLIC)
            .withParameters(getConstructorArgumentTypes())
            .intercept(invoke(ctorBlockBase).withArgument(4).andThen(

                construct(ctorLocation).withArgument(0, 1, 2, 3).setsField(named(FIELD_LOCATION))).andThen(

                FieldAccessor.ofField(FIELD_AXES_VALUES).setsArgumentAt(5)).andThen(

                FieldAccessor.ofField(FIELD_ROTATION_VALUES).setsArgumentAt(6)).andThen(

                invoke(methodGetBlockData)
                    .onMethodCall(getBlockAtLoc)
                    .setsField(named(FIELD_CRAFT_BLOCK_DATA))
                    .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC)).andThen(

                invoke(named(METHOD_CHECK_WATERLOGGED)).withField(FIELD_CRAFT_BLOCK_DATA)).andThen(

                invoke(named(METHOD_BLOCK_DATA_FROM_BUKKIT))).andThen(

                invoke(methodMatchXMaterial).withMethodCall(invoke(methodGetBlockMaterial).onMethodCall(getBlockAtLoc))
                                            .setsField(named(FIELD_XMATERIAL))));
    }

    private DynamicType.Builder<?> addFields(DynamicType.Builder<?> builder)
    {
        return builder
            .defineField(FIELD_BLOCK_DATA, classIBlockData, Visibility.PRIVATE)
            .defineField(FIELD_CRAFT_BLOCK_DATA, classCraftBlockData, Visibility.PRIVATE)
            .defineField(FIELD_XMATERIAL, XMaterial.class, Visibility.PRIVATE)
            .defineField(FIELD_LOCATION, Location.class, Visibility.PRIVATE)
            .defineField(FIELD_AXES_VALUES, asArrayType(classEnumDirectionAxis), Visibility.PRIVATE)
            .defineField(FIELD_ROTATION_VALUES, asArrayType(classEnumBlockRotation), Visibility.PRIVATE);
    }

    private DynamicType.Builder<?> addRotateBlockBaseMethod(DynamicType.Builder<?> builder, MethodDelegation delegation,
                                                            String baseName, String delegationName)
    {
        builder = builder
            .defineMethod(delegationName, classEnumBlockRotation, Visibility.PRIVATE)
            .withParameters(RotateDirection.class, asArrayType(classEnumBlockRotation))
            .intercept(delegation);

        builder = builder
            .defineMethod(baseName, void.class)
            .withParameters(RotateDirection.class)
            .intercept(invoke(methodRotateBlockData)
                           .onField(FIELD_BLOCK_DATA)
                           .withMethodCall(invoke(named(delegationName))
                                               .withArgument(0).withField(FIELD_ROTATION_VALUES))
                           .setsField(named(FIELD_BLOCK_DATA)));
        return builder;
    }

    private DynamicType.Builder<?> addRotateBlockMethod(DynamicType.Builder<?> builder)
    {
        final MethodDelegation findBlockRotation = MethodDelegation
            .to((IRotateBlock) (rotateDirection, values) ->
            {
                switch (rotateDirection)
                {
                    case CLOCKWISE:
                        return values[1];
                    case COUNTERCLOCKWISE:
                        return values[2];
                    default:
                        return values[0];
                }
            }, IRotateBlock.class);

        return addRotateBlockBaseMethod(builder, findBlockRotation, "rotateBlock", METHOD_ROTATE);
    }

    private DynamicType.Builder<?> addRotateCylindricalMethod(DynamicType.Builder<?> builder)
    {
        final MethodDelegation findBlockRotation = MethodDelegation
            .to((IRotateBlock) (rotateDirection, values) ->
            {
                if (rotateDirection.equals(RotateDirection.CLOCKWISE))
                    return values[1];
                else
                    return values[3];
            }, IRotateBlock.class);

        return addRotateBlockBaseMethod(builder, findBlockRotation, "rotateCylindrical", METHOD_ROTATE_CYLINDRICAL);
    }

    private DynamicType.Builder<?> addRotateBlockUpDownMethod(DynamicType.Builder<?> builder)
        throws IllegalAccessException
    {
        final Object blockRotatableAxis = fieldBlockRotatableAxis.get(null);

        final MethodCall getCurrentAxis = (MethodCall) invoke(methodEnumOrdinal)
            .onMethodCall((MethodCall) invoke(methodGetIBlockDataHolderState).onField(FIELD_BLOCK_DATA)
                                                                             .with(blockRotatableAxis)
                                                                             .withAssigner(Assigner.DEFAULT,
                                                                                           Assigner.Typing.DYNAMIC))
            .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

        final MethodCall getNewAxis = (MethodCall) invoke(named(METHOD_ROTATE_UP_DOWN))
            .withArgument(0).withMethodCall(getCurrentAxis).withField(FIELD_AXES_VALUES)
            .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

        final MethodCall setNewAxis = (MethodCall)
            invoke(methodSetIBlockDataHolderState).onField(FIELD_BLOCK_DATA).with(blockRotatableAxis)
                                                  .withMethodCall(getNewAxis)
                                                  .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

        builder = builder
            .defineMethod(METHOD_ROTATION_UP_DOWN, void.class).withParameters(boolean.class)
            .intercept(setNewAxis.setsField(named(FIELD_BLOCK_DATA)));

        builder = builder
            .defineMethod(METHOD_ROTATE_UP_DOWN, classEnumDirectionAxis, Visibility.PRIVATE)
            .withParameters(boolean.class, int.class, asArrayType(classEnumDirectionAxis))
            .intercept(MethodDelegation.to((IRotateBlockUpDown) (northSouthAligned, currentAxes, values) ->
            {
                int newIdx;
                switch (currentAxes)
                {
                    case 0:
                        newIdx = northSouthAligned ? 0 : 1;
                        break;
                    case 1:
                        newIdx = northSouthAligned ? 2 : 0;
                        break;
                    case 2:
                        newIdx = northSouthAligned ? 1 : 2;
                        break;
                    default:
                        throw new RuntimeException("Received unexpected direction " + currentAxes);
                }
                return values[newIdx];
            }, IRotateBlockUpDown.class));
        return builder;
    }

    private DynamicType.Builder<?> addBasicMethods(DynamicType.Builder<?> builder)
    {
        builder = builder
            .defineMethod(METHOD_BLOCK_DATA_FROM_BUKKIT, void.class, Visibility.PRIVATE)
            .intercept(invoke(methodGetCraftBlockDataState).onField(FIELD_CRAFT_BLOCK_DATA)
                                                           .setsField(named(FIELD_BLOCK_DATA)));

        builder = builder
            .defineMethod(METHOD_CAN_ROTATE, boolean.class)
            .intercept(invoke(methodIsAssignableFrom).on(MultipleFacing.class)
                                                     .withMethodCall(invoke(methodGetClass)
                                                                         .onField(FIELD_CRAFT_BLOCK_DATA)));

        builder = builder
            .defineMethod(METHOD_CHECK_WATERLOGGED, void.class, Visibility.PRIVATE)
            .withParameters(BlockData.class)
            .intercept(MethodDelegation.to((ICheckWaterLogged) blockData ->
            {
                if (blockData instanceof Waterlogged)
                    ((Waterlogged) blockData).setWaterlogged(false);
            }, ICheckWaterLogged.class));

        builder = builder.defineMethod(METHOD_GET_MY_BLOCK_DATA, classIBlockData)
                         .intercept(FieldAccessor.ofField(FIELD_BLOCK_DATA));

        builder = builder.defineMethod(METHOD_TO_STRING, String.class)
                         .intercept(invoke(named(METHOD_TO_STRING)).onField(FIELD_BLOCK_DATA));

        builder = builder.defineMethod(METHOD_GET_ITEM, classNMSItem).intercept(StubMethod.INSTANCE);

        final Method getBlock = ReflectionBuilder.findMethod().inClass(classBlockBase).withReturnType(classNMSBlock)
                                                 .withoutParameters().get();
        builder = builder.define(getBlock).intercept(StubMethod.INSTANCE);

        builder = builder
            .defineMethod(METHOD_DELETE_ORIGINAL_BLOCK, void.class)
            .intercept(invoke(methodSetBlockType)
                           .onMethodCall(invoke(methodLocationGetBlock).onField(FIELD_LOCATION))
                           .with(Material.AIR));

        return builder;
    }

    private DynamicType.Builder<?> addPutBlockMethod(DynamicType.Builder<?> builder)
    {
        final MethodCall worldCast = (MethodCall) invoke(methodGetNMSWorld)
            .onMethodCall(invoke(methodLocationGetWorld).onField(FIELD_LOCATION))
            .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

        return builder
            .defineMethod(METHOD_PUT_BLOCK, void.class)
            .withParameters(Location.class)
            .intercept(FieldAccessor.ofField(FIELD_LOCATION).setsArgumentAt(0).andThen(

                invoke(named(METHOD_UPDATE_MULTIPLE_FACING))
                    .withThis().withField(FIELD_CRAFT_BLOCK_DATA)
                    .withField(FIELD_LOCATION).withField(FIELD_XMATERIAL)).andThen(

                invoke(methodSetTypeAndData)
                    .onMethodCall(worldCast)
                    .withMethodCall(construct(cTorBlockPosition)
                                        .withMethodCall(invoke(methodLocationGetX).onField(FIELD_LOCATION))
                                        .withMethodCall(invoke(methodLocationGetY).onField(FIELD_LOCATION))
                                        .withMethodCall(invoke(methodLocationGetZ).onField(FIELD_LOCATION)))
                    .withField(FIELD_BLOCK_DATA)
                    .with(1)));
    }

    private DynamicType.Builder<?> addUpdateMultipleFacingMethod(DynamicType.Builder<?> builder)
    {
        return builder
            .defineMethod(METHOD_UPDATE_MULTIPLE_FACING, void.class)
            .withParameters(IGeneratedNMSBlock.class, Object.class, Location.class, XMaterial.class)
            .intercept(MethodDelegation.to((IUpdateMultipleFacing) (origin, craftBlockData, loc, xMat) ->
            {
                if (!(craftBlockData instanceof MultipleFacing))
                    return;

                final Set<BlockFace> allowedFaces = ((MultipleFacing) craftBlockData).getAllowedFaces();
                allowedFaces.forEach(
                    (blockFace) ->
                    {
                        Block otherBlock =
                            loc.clone().add(blockFace.getModX(), blockFace.getModY(), blockFace.getModZ()).getBlock();

                        if (blockFace.equals(BlockFace.UP))
                            ((MultipleFacing) craftBlockData).setFace(blockFace, true);
                        else if (otherBlock.getType().isSolid())
                        {
                            final Object otherData = origin.generated$retrieveBlockData(otherBlock);

                            ((MultipleFacing) craftBlockData).setFace(blockFace, true);

                            final boolean isOtherMultipleFacing = otherData instanceof MultipleFacing;
                            final boolean materialMatch = otherBlock.getType().equals(xMat.parseMaterial());
                            final boolean areBothFence = craftBlockData instanceof Fence && otherData instanceof Fence;

                            if (isOtherMultipleFacing && (materialMatch || areBothFence))
                            {
                                final Set<BlockFace> otherAllowedFaces = ((MultipleFacing) otherData).getAllowedFaces();
                                if (otherAllowedFaces.contains(blockFace.getOppositeFace()))
                                {
                                    ((MultipleFacing) otherData).setFace(blockFace.getOppositeFace(), true);
                                    origin.generated$updateBlockData(otherBlock, otherData);
                                }
                            }
                        }
                        else
                            ((MultipleFacing) craftBlockData).setFace(blockFace, false);
                    });
            }, IUpdateMultipleFacing.class).andThen(invoke(named(METHOD_BLOCK_DATA_FROM_BUKKIT))));
    }

    public interface IGeneratedNMSBlock
    {
        Object generated$retrieveBlockData(Block otherBlock);

        void generated$updateBlockData(Block otherBlock, Object newData);
    }

    public interface IRotateBlock
    {
        @RuntimeType
        Object intercept(RotateDirection rotateDirection, Object[] values);
    }

    public interface IRotateBlockUpDown
    {
        @RuntimeType
        Object intercept(boolean northSouthAligned, int currentAxes, Object[] values);
    }

    public interface ICheckWaterLogged
    {
        @RuntimeType
        void intercept(BlockData blockData);
    }

    public interface IUpdateMultipleFacing
    {
        /**
         * @param loc
         *     See {@link NMSBlockClassGenerator#FIELD_LOCATION}
         * @param xMat
         *     See {@link NMSBlockClassGenerator#FIELD_XMATERIAL}
         */
        @RuntimeType
        void intercept(IGeneratedNMSBlock origin, Object craftBlockData,
                       @FieldValue("generated$loc") Location loc, @FieldValue("generated$xMaterial") XMaterial xMat);
    }
}

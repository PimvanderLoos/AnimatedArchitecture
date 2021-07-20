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
import nl.pim16aap2.bigDoors.util.ReflectionUtils;
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

    public interface IGeneratedNMSBlock
    {
        Object generated$retrieveBlockData(Block otherBlock);

        void generated$updateBlockData(Block otherBlock, Object newData);
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

                construct(ctorLocation).withArgument(0, 1, 2, 3).setsField(named("loc"))).andThen(

                FieldAccessor.ofField("axesValues").setsArgumentAt(5)).andThen(

                FieldAccessor.ofField("blockRotationValues").setsArgumentAt(6)).andThen(

                invoke(named("getBlockData"))
                    .onMethodCall(getBlockAtLoc)
                    .setsField(named("craftBlockData"))
                    .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC)).andThen(

                invoke(named("checkWaterLogged")).withField("craftBlockData")).andThen(

                invoke(named("constructBlockDataFromBukkit"))).andThen(

                invoke(methodMatchXMaterial).withMethodCall(invoke(named("getType")).onMethodCall(getBlockAtLoc))));
    }

    private DynamicType.Builder<?> addFields(DynamicType.Builder<?> builder)
    {
        return builder
            .defineField("blockData", classIBlockData, Visibility.PRIVATE)
            .defineField("craftBlockData", classCraftBlockData, Visibility.PRIVATE)
            .defineField("xMat", XMaterial.class, Visibility.PRIVATE)
            .defineField("loc", Location.class, Visibility.PRIVATE)
            .defineField("axesValues", asArrayType(classEnumDirectionAxis), Visibility.PRIVATE)
            .defineField("blockRotationValues", asArrayType(classEnumBlockRotation), Visibility.PRIVATE);
    }

    public interface IRotateBlock
    {
        @RuntimeType
        Object intercept(RotateDirection rotateDirection, Object[] values);
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
                           .onField("blockData")
                           .withMethodCall(invoke(named(delegationName))
                                               .withArgument(0).withField("blockRotationValues"))
                           .setsField(named("blockData")));
        return builder;
    }

    private DynamicType.Builder<?> addRotateBlockMethod(DynamicType.Builder<?> builder)
    {
        final String rotateMethod = "generated$rotateBlockMethod";
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

        return addRotateBlockBaseMethod(builder, findBlockRotation, "rotateBlock", rotateMethod);
    }

    private DynamicType.Builder<?> addRotateCylindricalMethod(DynamicType.Builder<?> builder)
    {
        final String rotateMethod = "generated$rotateBlockCylindrical";
        final MethodDelegation findBlockRotation = MethodDelegation
            .to((IRotateBlock) (rotateDirection, values) ->
            {
                if (rotateDirection.equals(RotateDirection.CLOCKWISE))
                    return values[1];
                else
                    return values[3];
            }, IRotateBlock.class);

        return addRotateBlockBaseMethod(builder, findBlockRotation, "rotateCylindrical", rotateMethod);
    }

    public interface IRotateBlockUpDown
    {
        @RuntimeType
        Object intercept(boolean northSouthAligned, int currentAxes, Object[] values);
    }

    private DynamicType.Builder<?> addRotateBlockUpDownMethod(DynamicType.Builder<?> builder)
        throws IllegalAccessException
    {
        final String privateMethodName = "generated$rotateBlockUpDown";
        final Object blockRotatableAxis = fieldBlockRotatableAxis.get(null);

        final MethodCall getCurrentAxis = (MethodCall) invoke(methodEnumOrdinal)
            .onMethodCall((MethodCall) invoke(named("get")).onField("blockData").with(blockRotatableAxis)
                                                           .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
            .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

        final MethodCall getNewAxis = (MethodCall) invoke(named(privateMethodName))
            .withArgument(0).withMethodCall(getCurrentAxis).withField("axesValues")
            .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

        final MethodCall setNewAxis = (MethodCall)
            invoke(named("set")).onField("blockData").with(blockRotatableAxis).withMethodCall(getNewAxis)
                                .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

        builder = builder
            .defineMethod("rotateBlockUpDown", void.class).withParameters(boolean.class)
            .intercept(setNewAxis.setsField(named("blockData")));

        builder = builder
            .defineMethod(privateMethodName, classEnumDirectionAxis, Visibility.PRIVATE)
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

    public interface ICheckWaterLogged
    {
        @RuntimeType
        void intercept(BlockData blockData);
    }

    private DynamicType.Builder<?> addBasicMethods(DynamicType.Builder<?> builder)
    {
        builder = builder
            .defineMethod("constructBlockDataFromBukkit", void.class, Visibility.PRIVATE)
            .intercept(invoke(named("getState")).onField("craftBlockData").setsField(named("blockData")));

        builder = builder
            .defineMethod("canRotate", boolean.class)
            .intercept(invoke(methodIsAssignableFrom).on(MultipleFacing.class)
                                                     .withMethodCall(invoke(named("getClass"))
                                                                         .onField("craftBlockData")));

        builder = builder
            .defineMethod("checkWaterLogged", void.class, Visibility.PRIVATE)
            .withParameters(BlockData.class)
            .intercept(MethodDelegation.to((ICheckWaterLogged) blockData ->
            {
                if (blockData instanceof Waterlogged)
                    ((Waterlogged) blockData).setWaterlogged(false);
            }, ICheckWaterLogged.class));

        builder = builder.defineMethod("getMyBlockData", classIBlockData)
                         .intercept(FieldAccessor.ofField("blockData"));

        builder = builder.defineMethod("toString", String.class)
                         .intercept(invoke(named("toString")).onField("blockData"));

        builder = builder.defineMethod("getItem", classNMSItem).intercept(StubMethod.INSTANCE);

        final Method getBlock = ReflectionUtils.findMethodFromProfile(classBlockBase, classNMSBlock, null);
        builder = builder.define(getBlock).intercept(StubMethod.INSTANCE);

        builder = builder
            .defineMethod("deleteOriginalBlock", void.class)
            .intercept(invoke(methodSetBlockType)
                           .onMethodCall(invoke(named("getBlock")).onField("loc"))
                           .with(Material.AIR));

        return builder;
    }

    private DynamicType.Builder<?> addPutBlockMethod(DynamicType.Builder<?> builder)
    {
        final MethodCall worldCast = (MethodCall) invoke(methodGetNMSWorld)
            .onMethodCall(invoke(named("getWorld")).onField("loc"))
            .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

        return builder
            .defineMethod("putBlock", void.class)
            .withParameters(Location.class)
            .intercept(FieldAccessor.ofField("loc").setsArgumentAt(0).andThen(

                invoke(named("updateCraftBlockDataMultipleFacing"))
                    .withThis().withField("craftBlockData").withField("loc").withField("xMat")).andThen(

                invoke(named("setTypeAndData"))
                    .onMethodCall(worldCast)
                    .withMethodCall(construct(cTorBlockPosition)
                                        .withMethodCall(invoke(named("getBlockX")).onField("loc"))
                                        .withMethodCall(invoke(named("getBlockY")).onField("loc"))
                                        .withMethodCall(invoke(named("getBlockZ")).onField("loc")))
                    .withField("blockData")
                    .with(1)));
    }

    public interface IUpdateMultipleFacing
    {
        @RuntimeType
        void intercept(IGeneratedNMSBlock origin, Object craftBlockData,
                       @FieldValue("loc") Location loc, @FieldValue("xMat") XMaterial xMat);
    }

    private DynamicType.Builder<?> addUpdateMultipleFacingMethod(DynamicType.Builder<?> builder)
    {
        return builder
            .defineMethod("updateCraftBlockDataMultipleFacing", void.class)
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
            }, IUpdateMultipleFacing.class).andThen(invoke(named("constructBlockDataFromBukkit"))));
    }
}

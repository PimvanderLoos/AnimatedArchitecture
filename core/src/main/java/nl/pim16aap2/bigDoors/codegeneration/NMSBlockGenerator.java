package nl.pim16aap2.bigDoors.codegeneration;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import nl.pim16aap2.bigDoors.NMS.NMSBlock;
import nl.pim16aap2.bigDoors.reflection.ReflectionUtils;
import nl.pim16aap2.bigDoors.util.XMaterial;
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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Set;

import static net.bytebuddy.implementation.MethodCall.construct;
import static net.bytebuddy.implementation.MethodCall.invoke;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static nl.pim16aap2.bigDoors.codegeneration.ReflectionRepository.*;

final class NMSBlockGenerator extends Generator
{
    public NMSBlockGenerator(@NotNull String mappingsVersion)
    {
        super(mappingsVersion);
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
        DynamicType.Builder<?> builder = new ByteBuddy()
            .subclass(classBlockBase, ConstructorStrategy.Default.NO_CONSTRUCTORS)
            .implement(org.bukkit.entity.FallingBlock.class, NMSBlock.class, IGeneratedNMSBlock.class)
            // TODO: Use full name
//            .name("GeneratedNMSBlock_" + this.mappingsVersion);
            .name("GeneratedNMSBlock");

        builder = addFields(builder);
        builder = addCTor(builder);
        builder = addBasicMethods(builder);
        builder = addPutBlockMethod(builder);
        builder = addRotateBlockUpDownMethod(builder);
        builder = addUpdateMultipleFacingMethod(builder);

        finishBuilder(builder, World.class, int.class, int.class, int.class, classBlockBaseInfo,
                      asArrayType(classEnumDirectionEnumAxis));
    }

    private DynamicType.Builder<?> addCTor(DynamicType.Builder<?> builder)
    {
        final MethodCall getBlockAtLoc = invoke(methodGetBlockAtCoords).onArgument(0).withArgument(1, 2, 3);

        return builder
            .defineConstructor(Visibility.PUBLIC)
            .withParameters(World.class, int.class, int.class, int.class, classBlockBaseInfo,
                            asArrayType(classEnumDirectionEnumAxis))
            .intercept(invoke(ctorBlockBase).withArgument(4).andThen(

                construct(ctorLocation).withArgument(0, 1, 2, 3).setsField(named("loc"))).andThen(

                FieldAccessor.ofField("axesValues").setsArgumentAt(5)).andThen(

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
            .defineField("xmat", XMaterial.class, Visibility.PRIVATE)
            .defineField("loc", Location.class, Visibility.PRIVATE)
            .defineField("axesValues", asArrayType(classEnumDirectionEnumAxis), Visibility.PRIVATE)
            ;
    }

    public interface IRotateBlockUpDown
    {
        @RuntimeType
        int intercept(boolean northSouthAligned, int currentAxes);
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

        final MethodCall getNewAxisIdx = (MethodCall) invoke(named(privateMethodName))
            .withArgument(0).withMethodCall(getCurrentAxis)
            .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

        final MethodCall getNewAxis = (MethodCall)
            invoke(methodArrayGetIdx).withField("axesValues").withMethodCall(getNewAxisIdx)
                                .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

        final MethodCall setNewAxis = (MethodCall)
            invoke(named("set")).onField("blockData").with(blockRotatableAxis).withMethodCall(getNewAxis)
                                .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);

        builder = builder
            .defineMethod("rotateBlockUpDown", void.class).withParameters(boolean.class)
            .intercept(setNewAxis.setsField(named("blockData")));

        builder = builder
            .defineMethod(privateMethodName, int.class, Visibility.PRIVATE)
            .withParameters(boolean.class, int.class)
            .intercept(MethodDelegation.to((IRotateBlockUpDown) (northSouthAligned, currentAxes) ->
            {
                switch (currentAxes)
                {
                    case 0:
                        return northSouthAligned ? 0 : 1;
                    case 1:
                        return northSouthAligned ? 2 : 0;
                    case 2:
                        return northSouthAligned ? 1 : 2;
                    default:
                        throw new RuntimeException("Received unexpected direction " + currentAxes);
                }
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
                    .withThis().withField("craftBlockData").withField("loc").withField("xmat")).andThen(

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
                       @FieldValue("loc") Location loc, @FieldValue("xmat") XMaterial xmat);
    }

    private DynamicType.Builder<?> addUpdateMultipleFacingMethod(DynamicType.Builder<?> builder)
    {
        return builder
            .defineMethod("updateCraftBlockDataMultipleFacing", void.class)
            .withParameters(IGeneratedNMSBlock.class, Object.class, Location.class, XMaterial.class)
            .intercept(MethodDelegation.to((IUpdateMultipleFacing) (origin, craftBlockData, loc, xmat) ->
            {
                if (!(craftBlockData instanceof MultipleFacing))
                    return;

                Set<BlockFace> allowedFaces = ((MultipleFacing) craftBlockData).getAllowedFaces();
                allowedFaces.forEach(
                    (blockFace) ->
                    {
                        Block otherBlock =
                            loc.clone().add(blockFace.getModX(), blockFace.getModY(), blockFace.getModZ())
                               .getBlock();

                        Object otherData = origin.generated$retrieveBlockData(otherBlock);

                        if (blockFace.equals(BlockFace.UP))
                            ((MultipleFacing) craftBlockData).setFace(blockFace, true);
                        else if (otherBlock.getType().isSolid())
                        {
                            // TODO: Yikes
                            ((MultipleFacing) craftBlockData).setFace(blockFace, true);
                            if (otherData instanceof MultipleFacing &&
                                (otherBlock.getType().equals(xmat.parseMaterial()) ||
                                    (craftBlockData instanceof Fence &&
                                        otherData instanceof Fence)))
                                if (((MultipleFacing) otherData).getAllowedFaces()
                                                                .contains(blockFace.getOppositeFace()))
                                {
                                    ((MultipleFacing) otherData).setFace(blockFace.getOppositeFace(), true);
                                    origin.generated$updateBlockData(otherBlock, otherData);
                                }
                        }
                        else
                            ((MultipleFacing) craftBlockData).setFace(blockFace, false);
                    });
            }, IUpdateMultipleFacing.class).andThen(invoke(named("constructBlockDataFromBukkit"))));
    }
}

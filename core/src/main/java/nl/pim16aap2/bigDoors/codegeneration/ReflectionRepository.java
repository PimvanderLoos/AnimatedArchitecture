package nl.pim16aap2.bigDoors.codegeneration;

import com.cryptomorin.xseries.XMaterial;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.reflection.ReflectionBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

import static nl.pim16aap2.bigDoors.reflection.ReflectionBuilder.*;

final class ReflectionRepository
{
    public static final Class<?> classEntityFallingBlock;
    public static final Class<?> classBlock;
    public static final Class<?> classIBlockData;
    public static final Class<?> classIBlockState;
    public static final Class<?> classIBlockDataHolder;
    public static final Class<?> classBlockData;
    public static final Class<?> classNMSWorld;
    public static final Class<?> classNMSWorldServer;
    public static final Class<?> classNMSEntity;
    public static final Class<?> classNMSBlock;
    public static final Class<?> classNMSItem;
    public static final Class<?> classNMSDamageSource;
    public static final Class<?> classBlockRotatable;
    public static final Class<?> classBlockPosition;
    public static final Class<?> classVec3D;
    public static final Class<?> classNBTTagCompound;
    public static final Class<?> classNBTBase;
    public static final Class<?> classCrashReportSystemDetails;
    public static final Class<?> classGameProfileSerializer;
    public static final Class<?> classBlockBase;
    public static final Class<?> classBlockBaseInfo;
    public static final Class<?> classCraftWorld;
    public static final Class<?> classCraftEntity;
    public static final Class<?> classCraftServer;
    public static final Class<?> classCraftMagicNumbers;
    public static final Class<?> classCraftBlockData;
    public static final Class<?> classBlockStateEnum;

    public static final Class<?> classEnumBlockState;
    public static final Class<?> classEnumMoveType;
    public static final Class<?> classEnumDirectionAxis;
    public static final Class<?> classEnumBlockRotation;

    public static final Constructor<?> cTorNMSFallingBlockEntity;
    public static final Constructor<?> cTorBlockPosition;
    public static final Constructor<?> cTorVec3D;
    public static final Constructor<?> ctorCraftEntity;
    public static final Constructor<?> ctorBlockBase;
    public static final Constructor<?> ctorLocation;

    public static final Method methodGetBlockData;
    public static final Method methodGetBlockMaterial;
    public static final Method methodGetCraftBlockDataState;
    public static final Method methodLocationGetBlock;
    public static final Method methodLocationGetWorld;
    public static final Method methodLocationGetX;
    public static final Method methodLocationGetY;
    public static final Method methodLocationGetZ;
    public static final Method methodTick;
    public static final Method methodDie;
    public static final Method methodGetNMSWorld;
    public static final Method methodGetEntityHandle;
    public static final Method methodSetPosition;
    public static final Method methodSetNoGravity;
    public static final Method methodGetMot;
    public static final Method methodSetMotVec;
    public static final Method methodHurtEntities;
    public static final Method methodMove;
    public static final Method methodSaveData;
    public static final Method methodLoadData;
    public static final Method methodEntityFallingBlockGetBlock;
    public static final Method methodSetStartPos;
    public static final Method methodLocX;
    public static final Method methodLocY;
    public static final Method methodLocZ;
    public static final Method methodNMSAddEntity;
    public static final Method methodAppendEntityCrashReport;
    public static final Method methodCrashReportAppender;
    public static final Method methodNBTTagCompoundSet;
    public static final Method methodNBTTagCompoundSetInt;
    public static final Method methodNBTTagCompoundSetBoolean;
    public static final Method methodNBTTagCompoundSetFloat;
    public static final Method methodNBTTagCompoundHasKeyOfType;
    public static final Method methodNBTTagCompoundGetCompound;
    public static final Method methodNBTTagCompoundGetInt;
    public static final Method methodIBlockDataSerializer;
    public static final Method methodIBlockDataDeserializer;
    public static final Method methodFromData;
    public static final Method methodBlockBaseGetItem;
    public static final Method methodSetIBlockDataHolderState;
    public static final Method methodGetIBlockDataHolderState;
    public static final Method methodIsAir;
    public static final Method methodCraftMagicNumbersGetMaterial;
    public static final Method methodGetItemType;
    public static final Method methodCraftEntitySetTicksLived;
    public static final Method methodMatchXMaterial;
    public static final Method methodGetBlockAtCoords;
    public static final Method methodGetBlockAtLoc;
    public static final Method methodGetBlockFromBlockData;
    public static final Method methodRotateBlockData;
    public static final Method methodSetTypeAndData;
    public static final Method methodBlockInfoFromBlockBase;
    public static final Method methodGetTypeFromBlockPosition;
    public static final Method methodGetBukkitServer;
    public static final Method methodSetCraftEntityCustomName;
    public static final Method methodSetCraftEntityCustomNameVisible;
    public static final Method methodIsAssignableFrom;
    public static final Method methodSetBlockType;
    public static final Method methodEnumOrdinal;
    public static final Method methodArrayGetIdx;
    public static final Method methodGetClass;

    public static final Field fieldTileEntityData;
    public static final Field fieldTicksLived;
    public static final Field fieldNMSWorld;
    public static final Field fieldBlockRotatableAxis;

    public static final List<Field> fieldsVec3D;

    static
    {
        final String nmsBase = "net.minecraft.server." + BigDoors.get().getPackageVersion() + ".";
        final String craftBase = "org.bukkit.craftbukkit." + BigDoors.get().getPackageVersion() + ".";

        classEntityFallingBlock = findClass(nmsBase + "EntityFallingBlock",
                                            "net.minecraft.world.entity.item.EntityFallingBlock").get();
        classNBTTagCompound = findClass(nmsBase + "NBTTagCompound",
                                        "net.minecraft.nbt.NBTTagCompound").get();
        classNBTBase = findClass(nmsBase + "NBTBase", "net.minecraft.nbt.NBTBase").get();
        classBlockBase = findClass(nmsBase + "BlockBase", "net.minecraft.world.level.block.state.BlockBase").get();
        classBlockBaseInfo = findClass(classBlockBase.getName() + "$Info").get();
        classBlockData = findClass(classBlockBase.getName() + "$BlockData").get();
        classBlock = findClass(nmsBase + "Block", "net.minecraft.world.level.block.Block").get();
        classIBlockState = findClass(nmsBase + "IBlockState",
                                     "net.minecraft.world.level.block.state.properties.IBlockState").get();
        classIBlockData = findClass(nmsBase + "IBlockData",
                                    "net.minecraft.world.level.block.state.IBlockData").get();
        classIBlockDataHolder = findClass(nmsBase + "IBlockDataHolder",
                                    "net.minecraft.world.level.block.state.IBlockDataHolder").get();
        classCraftWorld = findClass(craftBase + "CraftWorld").get();
        classEnumMoveType = findClass(nmsBase + "EnumMoveType", "net.minecraft.world.entity.EnumMoveType").get();
        classVec3D = findClass(nmsBase + "Vec3D", "net.minecraft.world.phys.Vec3D").get();
        classNMSWorld = findClass(nmsBase + "World", "net.minecraft.world.level.World").get();
        classNMSWorldServer = findClass(nmsBase + "WorldServer", "net.minecraft.server.level.WorldServer").get();
        classNMSEntity = findClass(nmsBase + "Entity", "net.minecraft.world.entity.Entity").get();
        classBlockPosition = findClass(nmsBase + "BlockPosition", "net.minecraft.core.BlockPosition").get();
        classCrashReportSystemDetails = findClass(nmsBase + "CrashReportSystemDetails",
                                                  "net.minecraft.CrashReportSystemDetails").get();
        classGameProfileSerializer = findClass(nmsBase + "GameProfileSerializer",
                                               "net.minecraft.nbt.GameProfileSerializer").get();
        classCraftEntity = findClass(craftBase + "entity.CraftEntity").get();
        classCraftServer = findClass(craftBase + "CraftServer").get();
        classCraftMagicNumbers = findClass(craftBase + "util.CraftMagicNumbers").get();
        classCraftBlockData = findClass(craftBase + "block.data.CraftBlockData").get();
        classBlockStateEnum = findClass(nmsBase + "BlockStateEnum",
                                        "net.minecraft.world.level.block.state.properties.BlockStateEnum").get();
        classNMSBlock = findClass(nmsBase + "Block", "net.minecraft.world.level.block.Block").get();
        classNMSItem = findClass(nmsBase + "Item", "net.minecraft.world.item.Item").get();
        classNMSDamageSource = findClass(nmsBase + "DamageSource",
                                         "net.minecraft.world.damagesource.DamageSource").get();
        classEnumDirectionAxis = findClass(nmsBase + "EnumDirection$EnumAxis",
                                           "net.minecraft.core.EnumDirection$EnumAxis").get();
        classEnumBlockRotation = findClass(nmsBase + "EnumBlockRotation",
                                           "net.minecraft.world.level.block.EnumBlockRotation").get();
        classBlockRotatable = findClass(nmsBase + "BlockRotatable",
                                        "net.minecraft.world.level.block.BlockRotatable").get();
        classEnumBlockState = findClass(nmsBase + "BlockStateEnum",
                                        "net.minecraft.world.level.block.state.properties.BlockStateEnum").get();


        cTorNMSFallingBlockEntity = findConstructor().inClass(classEntityFallingBlock)
                                                     .withParameters(classNMSWorld, double.class,
                                                                     double.class, double.class, classIBlockData).get();
        cTorBlockPosition = findConstructor().inClass(classBlockPosition)
                                             .withParameters(double.class, double.class, double.class).get();
        cTorVec3D = findConstructor().inClass(classVec3D)
                                     .withParameters(double.class, double.class, double.class).get();
        ctorCraftEntity = findConstructor().inClass(classCraftEntity)
                                           .withParameters(classCraftServer, classNMSEntity).get();
        ctorBlockBase = findConstructor().inClass(classBlockBase)
                                         .withParameters(classBlockBaseInfo).get();
        ctorLocation = findConstructor().inClass(Location.class)
                                        .withParameters(World.class, double.class, double.class, double.class)
                                        .get();


        methodGetBlockData = findMethod().inClass(Block.class).withName("getBlockData").withoutParameters().get();
        methodGetBlockMaterial = findMethod().inClass(Block.class).withName("getType").withoutParameters().get();
        methodGetCraftBlockDataState = findMethod().inClass(classCraftBlockData).withName("getState")
                                                   .withoutParameters().get();
        methodLocationGetBlock = findMethod().inClass(Location.class).withName("getBlock").withoutParameters().get();
        methodLocationGetWorld = findMethod().inClass(Location.class).withName("getWorld").withoutParameters().get();
        methodLocationGetX = findMethod().inClass(Location.class).withName("getX").withoutParameters().get();
        methodLocationGetY = findMethod().inClass(Location.class).withName("getY").withoutParameters().get();
        methodLocationGetZ = findMethod().inClass(Location.class).withName("getZ").withoutParameters().get();
        methodGetNMSWorld = findMethod().inClass(classCraftWorld).withName("getHandle")
                                        .withoutParameters().withModifiers(Modifier.PUBLIC).get();
        methodGetEntityHandle = findMethod().inClass(classCraftEntity).withName("getHandle")
                                            .withoutParameters().withModifiers(Modifier.PUBLIC).get();
        methodTick = findMethod().inClass(classEntityFallingBlock).withReturnType(void.class)
                                 .withModifiers(Modifier.PUBLIC).withoutParameters().get();
        methodHurtEntities = findMethod().inClass(classEntityFallingBlock).withReturnType(boolean.class)
                                         .withParameters(parameterBuilder()
                                                             .withRequiredParameters(float.class, float.class)
                                                             .withOptionalParameters(classNMSDamageSource)).get();
        methodMove = findMethod().inClass(classNMSEntity).withReturnType(void.class)
                                 .withParameters(classEnumMoveType, classVec3D).get();
        methodEntityFallingBlockGetBlock = findMethod().inClass(classEntityFallingBlock).withReturnType(classIBlockData)
                                                       .withoutParameters().get();
        methodSetStartPos = findMethod().inClass(classEntityFallingBlock).withReturnType(void.class)
                                        .withModifiers(Modifier.PUBLIC).withParameters(classBlockPosition).get();
        methodAppendEntityCrashReport = findMethod().inClass(classEntityFallingBlock).withReturnType(void.class)
                                                    .withModifiers(Modifier.PUBLIC)
                                                    .withParameters(classCrashReportSystemDetails).get();
        methodCrashReportAppender = findMethod().inClass(classCrashReportSystemDetails)
                                                .withReturnType(classCrashReportSystemDetails)
                                                .withModifiers(Modifier.PUBLIC)
                                                .withParameters(String.class, Object.class).get();
        methodNBTTagCompoundSet = findMethod().inClass(classNBTTagCompound).withReturnType(classNBTBase)
                                              .withParameters(String.class, classNBTBase).get();
        methodNBTTagCompoundSetInt = findMethod().inClass(classNBTTagCompound).withReturnType(void.class)
                                                 .withParameters(String.class, int.class).get();
        methodNBTTagCompoundSetBoolean = findMethod().inClass(classNBTTagCompound).withReturnType(void.class)
                                                     .withParameters(String.class, boolean.class).get();
        methodNBTTagCompoundSetFloat = findMethod().inClass(classNBTTagCompound).withReturnType(void.class)
                                                   .withParameters(String.class, float.class).get();
        methodNBTTagCompoundGetCompound = findMethod().inClass(classNBTTagCompound).withReturnType(classNBTTagCompound)
                                                      .withParameters(String.class).get();
        methodNBTTagCompoundGetInt = findMethod().inClass(classNBTTagCompound).withReturnType(int.class)
                                                 .withParameters(String.class).get();
        methodNBTTagCompoundHasKeyOfType = findMethod().inClass(classNBTTagCompound).withReturnType(boolean.class)
                                                       .withParameters(String.class, int.class).get();
        methodIBlockDataSerializer = findMethod().inClass(classGameProfileSerializer)
                                                 .withReturnType(classNBTTagCompound)
                                                 .withModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                 .withParameters(classIBlockData).get();
        methodIBlockDataDeserializer = findMethod().inClass(classGameProfileSerializer)
                                                   .withReturnType(classIBlockData)
                                                   .withModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                   .withParameters(classNBTTagCompound).get();
        methodFromData = findMethod().inClass(classCraftBlockData).withName("fromData")
                                     .withParameters(classIBlockData).get();
        methodBlockBaseGetItem = findMethod().inClass(classBlockBase).withReturnType(classNMSItem)
                                             .withoutParameters()
                                             .withModifiers(Modifier.ABSTRACT, Modifier.PUBLIC).get();
        methodSetIBlockDataHolderState = ReflectionBuilder.findMethod().inClass(classIBlockDataHolder)
                                                          .withReturnType(Object.class)
                                                          .withParameters(classIBlockState, Comparable.class).get();
        methodCraftMagicNumbersGetMaterial = findMethod()
            .inClass(classCraftMagicNumbers).withName("getMaterial").withParameters(classIBlockData).get();
        methodGetItemType = findMethod().inClass(MaterialData.class).withName("getItemType").get();
        methodCraftEntitySetTicksLived = findMethod().inClass(classCraftEntity).withName("setTicksLived")
                                                     .withParameters(int.class).get();
        methodMatchXMaterial = findMethod().inClass(XMaterial.class).withName("matchXMaterial")
                                           .withParameters(Material.class).get();
        methodGetBlockAtCoords = findMethod().inClass(World.class).withName("getBlockAt")
                                             .withParameters(int.class, int.class, int.class).get();
        methodGetBlockAtLoc = findMethod().inClass(World.class).withName("getBlockAt").withParameters(Location.class)
                                          .get();
        methodGetBlockFromBlockData = findMethod().inClass(classBlockData).withReturnType(classBlock)
                                                  .withoutParameters().get();
        methodRotateBlockData = findMethod().inClass(classBlockData).withReturnType(classIBlockData)
                                            .withModifiers(Modifier.PUBLIC).withParameters(classEnumBlockRotation)
                                            .get();
        methodSetTypeAndData = findMethod().inClass(classNMSWorld).withReturnType(boolean.class)
                                           .withParameters(classBlockPosition, classIBlockData, int.class).get();
        methodBlockInfoFromBlockBase = findMethod().inClass(classBlockBaseInfo).withReturnType(classBlockBaseInfo)
                                                   .withModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                   .withParameters(classBlockBase).get();
        methodGetTypeFromBlockPosition = findMethod().inClass(classNMSWorld).withReturnType(classIBlockData)
                                                     .withParameters(classBlockPosition).get();
        methodGetBukkitServer = findMethod().inClass(Bukkit.class).withName("getServer").get();
        methodSetCraftEntityCustomName = findMethod().inClass(classCraftEntity).withName("setCustomName")
                                                     .withParameters(String.class).get();
        methodSetCraftEntityCustomNameVisible = findMethod()
            .inClass(classCraftEntity).withName("setCustomNameVisible").withParameters(boolean.class).get();
        methodIsAssignableFrom = findMethod().inClass(Class.class).withName("isAssignableFrom")
                                             .withParameters(Class.class).get();
        methodSetBlockType = findMethod().inClass(Block.class).withName("setType").withParameters(Material.class)
                                         .get();
        methodEnumOrdinal = findMethod().inClass(Enum.class).withName("ordinal").get();
        methodArrayGetIdx = findMethod().inClass(Array.class).withName("get")
                                        .withParameters(Object.class, int.class).get();
        methodGetClass = findMethod().inClass(Object.class).withName("getClass").get();
        methodGetIBlockDataHolderState = ReflectionASMAnalyzers
            .getGetIBlockDataHolderStateMethod(classCraftBlockData, classBlockStateEnum, classIBlockData,
                                               classIBlockState, classIBlockDataHolder);
        methodNMSAddEntity = ReflectionASMAnalyzers.getNMSAddEntityMethod(classNMSWorldServer, classNMSEntity);
        methodIsAir = ReflectionASMAnalyzers.getIsAirMethod(methodTick, classIBlockData, classBlockData);
        methodDie = ReflectionASMAnalyzers.getCraftEntityDelegationMethod(classCraftEntity, classNMSEntity);
        methodSetPosition = ReflectionASMAnalyzers.getSetPosition(classNMSEntity);
        methodSetNoGravity = ReflectionASMAnalyzers.getSetNoGravity(classCraftEntity, classNMSEntity);
        methodSetMotVec = ReflectionASMAnalyzers.getSetMotVecMethod(classCraftEntity, classNMSEntity, classVec3D);
        methodGetMot = ReflectionASMAnalyzers.getGetMotMethod(classCraftEntity, classNMSEntity, classVec3D);
        final Method[] methodsSaveLoadData = ReflectionASMAnalyzers.getSaveLoadDataMethods(
            classEntityFallingBlock, classNBTTagCompound, methodNBTTagCompoundSetInt);
        methodSaveData = methodsSaveLoadData[0];
        methodLoadData = methodsSaveLoadData[1];
        final Method[] locationMethods =
            ReflectionASMAnalyzers.getEntityLocationMethods(classCraftEntity, classNMSEntity);
        methodLocX = locationMethods[0];
        methodLocY = locationMethods[1];
        methodLocZ = locationMethods[2];


        fieldTileEntityData = findField().inClass(classEntityFallingBlock).ofType(classNBTTagCompound)
                                         .withModifiers(Modifier.PUBLIC).get();
        fieldTicksLived = findField().inClass(classEntityFallingBlock).ofType(int.class)
                                     .withModifiers(Modifier.PUBLIC).get();
        fieldNMSWorld = findField().inClass(classNMSEntity).ofType(classNMSWorld)
                                   .withModifiers(Modifier.PUBLIC).get();
        fieldBlockRotatableAxis = findField().inClass(classBlockRotatable).ofType(classEnumBlockState)
                                             .withModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC).get();


        fieldsVec3D = Collections.unmodifiableList(
            ReflectionBuilder.findField().inClass(classVec3D).allOfType(double.class)
                             .withModifiers(Modifier.PUBLIC, Modifier.FINAL)
                             .exactCount(3).get());
    }

    private ReflectionRepository()
    {
    }

    public static Class<?> asArrayType(Class<?> clz)
    {
        return Array.newInstance(clz, 0).getClass();
    }
}

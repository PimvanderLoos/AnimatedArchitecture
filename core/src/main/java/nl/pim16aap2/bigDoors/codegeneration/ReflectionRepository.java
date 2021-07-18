package nl.pim16aap2.bigDoors.codegeneration;

import nl.pim16aap2.bigDoors.reflection.ReflectionUtils;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.material.MaterialData;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static nl.pim16aap2.bigDoors.reflection.ReflectionUtils.*;

final class ReflectionRepository
{
    public static final Class<?> classEntityFallingBlock;
    public static final Class<?> classIBlockData;
    public static final Class<?> classCraftWorld;
    public static final Class<?> classNMSWorld;
    public static final Class<?> classNMSWorldServer;
    public static final Class<?> classBlockPosition;
    public static final Class<?> classNMSEntity;
    public static final Class<?> classVec3D;
    public static final Class<?> classEnumMoveType;
    public static final Class<?> classNBTTagCompound;
    public static final Class<?> classNBTBase;
    public static final Class<?> classCrashReportSystemDetails;
    public static final Class<?> classGameProfileSerializer;
    public static final Class<?> classCraftEntity;
    public static final Class<?> classCraftServer;
    public static final Class<?> classCraftMagicNumbers;

    public static final Constructor<?> cTorNMSFallingBlockEntity;
    public static final Constructor<?> cTorBlockPosition;
    public static final Constructor<?> cTorVec3D;
    public static final Constructor<?> ctorCraftEntity;

    public static final Method methodTick;
    public static final Method methodGetNMSWorld;
    public static final Method methodSetPosition;
    public static final Method methodSetNoGravity;
    public static final Method methodGetMot;
    public static final Method methodSetMot;
    public static final Method methodSetMotVec;
    public static final Method methodHurtEntities;
    public static final Method methodMove;
    public static final Method methodSaveData;
    public static final Method methodLoadData;
    public static final Method methodGetBlock;
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
    public static final Method methodIsAir;

    public static final Method methodCraftMagicNumbersGetMaterial;
    public static final Method methodGetItemType;
    public static final Method methodCraftEntitySetTicksLived;

    public static final Field fieldTileEntityData;
    public static final Field fieldTicksLived;
    public static final Field fieldNMSWorld;

    public static final List<Field> fieldsVec3D;

    public static final Object fieldEnumMoveTypeSelf;

    static
    {
        classEntityFallingBlock = findFirstClass(NMS_BASE + "EntityFallingBlock",
                                                 "net.minecraft.world.entity.item.EntityFallingBlock");
        classNBTTagCompound = findFirstClass(NMS_BASE + "NBTTagCompound",
                                             "net.minecraft.nbt.NBTTagCompound");
        classNBTBase = findFirstClass(NMS_BASE + "NBTBase", "net.minecraft.nbt.NBTBase");
        classIBlockData = findFirstClass(NMS_BASE + "IBlockData", "net.minecraft.world.level.block.state.IBlockData");
        classCraftWorld = findFirstClass(CRAFT_BASE + "CraftWorld");
        classEnumMoveType = findFirstClass(NMS_BASE + "EnumMoveType", "net.minecraft.world.entity.EnumMoveType");
        classVec3D = findFirstClass(NMS_BASE + "Vec3D", "net.minecraft.world.phys.Vec3D");
        classNMSWorld = findFirstClass(NMS_BASE + "World", "net.minecraft.world.level.World");
        classNMSWorldServer = findFirstClass(NMS_BASE + "WorldServer", "net.minecraft.server.level.WorldServer");
        classNMSEntity = findFirstClass(NMS_BASE + "Entity", "net.minecraft.world.entity.Entity");
        classBlockPosition = findFirstClass(NMS_BASE + "BlockPosition", "net.minecraft.core.BlockPosition");
        classCrashReportSystemDetails = findFirstClass(NMS_BASE + "CrashReportSystemDetails",
                                                       "net.minecraft.CrashReportSystemDetails");
        classGameProfileSerializer = findFirstClass(NMS_BASE + "GameProfileSerializer",
                                                    "net.minecraft.nbt.GameProfileSerializer");
        classCraftEntity = findClass(CRAFT_BASE + "entity.CraftEntity");
        classCraftServer = findClass(CRAFT_BASE + "CraftServer");
        classCraftMagicNumbers = findClass(CRAFT_BASE + "util.CraftMagicNumbers");


        cTorNMSFallingBlockEntity = ReflectionUtils.findCTor(classEntityFallingBlock, classNMSWorld, double.class,
                                                             double.class, double.class, classIBlockData);
        cTorBlockPosition = ReflectionUtils.findCTor(classBlockPosition, double.class, double.class, double.class);
        cTorVec3D = ReflectionUtils.findCTor(classVec3D, double.class, double.class, double.class);
        ctorCraftEntity = ReflectionUtils.findCTor(classCraftEntity, classCraftServer, classNMSEntity);


        methodGetNMSWorld = ReflectionUtils.getMethod(classCraftWorld, "getHandle");
        methodTick = ReflectionUtils.findMethodFromProfile(classEntityFallingBlock, void.class, Modifier.PUBLIC);
        methodSetPosition = ReflectionUtils.getMethod(classNMSEntity, "setPosition",
                                                      double.class, double.class, double.class);
        methodSetNoGravity = ReflectionUtils.getMethod(classNMSEntity, "setNoGravity", boolean.class);
        methodSetMot = ReflectionUtils.getMethod(classNMSEntity, "setMot", double.class, double.class, double.class);
        methodSetMotVec = ReflectionUtils.getMethod(classNMSEntity, "setMot", classVec3D);
        methodGetMot = ReflectionUtils.getMethod(classNMSEntity, "getMot");
        methodHurtEntities = ReflectionUtils.findMethodFromProfile(classEntityFallingBlock, boolean.class,
                                                                   Modifier.PUBLIC,
                                                                   float.class, float.class, null);
        methodMove = ReflectionUtils.getMethod(true, classNMSEntity, "move", classEnumMoveType, classVec3D);
        methodSaveData = ReflectionUtils.getMethod(classEntityFallingBlock, "saveData", classNBTTagCompound);
        methodLoadData = ReflectionUtils.getMethod(classEntityFallingBlock, "loadData", classNBTTagCompound);
        methodGetBlock = ReflectionUtils.getMethod(classEntityFallingBlock, "getBlock");
        methodSetStartPos = ReflectionUtils.findMethodFromProfile(classEntityFallingBlock, void.class,
                                                                  Modifier.PUBLIC, classBlockPosition);
        methodLocX = ReflectionUtils.getMethod(classNMSEntity, "locX");
        methodLocY = ReflectionUtils.getMethod(classNMSEntity, "locY");
        methodLocZ = ReflectionUtils.getMethod(classNMSEntity, "locZ");
        methodNMSAddEntity = ReflectionUtils.getMethod(classNMSWorldServer, "addEntity",
                                                       classNMSEntity, CreatureSpawnEvent.SpawnReason.class);
        methodAppendEntityCrashReport = ReflectionUtils
            .findMethodFromProfile(classEntityFallingBlock, void.class, Modifier.PUBLIC,
                                   classCrashReportSystemDetails);
        methodCrashReportAppender = ReflectionUtils.findMethodFromProfile(classCrashReportSystemDetails,
                                                                          classCrashReportSystemDetails,
                                                                          Modifier.PUBLIC,
                                                                          String.class, Object.class);
        methodIsAir = ReflectionUtils.getMethodFullInheritance(classIBlockData, "isAir");
        methodNBTTagCompoundSet = ReflectionUtils.getMethod(classNBTTagCompound, "set", String.class, classNBTBase);

        methodNBTTagCompoundSetInt = ReflectionUtils.getMethod(classNBTTagCompound, "setInt",
                                                               String.class, int.class);
        methodNBTTagCompoundSetBoolean = ReflectionUtils.getMethod(classNBTTagCompound, "setBoolean",
                                                                   String.class, boolean.class);
        methodNBTTagCompoundSetFloat = ReflectionUtils.getMethod(classNBTTagCompound, "setFloat",
                                                                 String.class, float.class);
        methodNBTTagCompoundGetCompound = ReflectionUtils.getMethod(classNBTTagCompound, "getCompound", String.class);
        methodNBTTagCompoundGetInt = ReflectionUtils.getMethod(classNBTTagCompound, "getInt", String.class);
        methodNBTTagCompoundHasKeyOfType = ReflectionUtils.getMethod(classNBTTagCompound, "hasKeyOfType",
                                                                     String.class, int.class);
        methodIBlockDataSerializer = ReflectionUtils
            .findMethodFromProfile(classGameProfileSerializer, classNBTTagCompound,
                                   ReflectionUtils.getModifiers(Modifier.PUBLIC, Modifier.STATIC), classIBlockData);
        methodIBlockDataDeserializer = ReflectionUtils
            .findMethodFromProfile(classGameProfileSerializer, classIBlockData,
                                   ReflectionUtils.getModifiers(Modifier.PUBLIC, Modifier.STATIC),
                                   classNBTTagCompound);
        methodCraftMagicNumbersGetMaterial = getMethod(classCraftMagicNumbers, "getMaterial", classIBlockData);
        methodGetItemType = getMethod(MaterialData.class, "getItemType");
        methodCraftEntitySetTicksLived = ReflectionUtils.getMethod(classCraftEntity, "setTicksLived", int.class);


        fieldTileEntityData = ReflectionUtils.getField(classEntityFallingBlock, Modifier.PUBLIC, classNBTTagCompound);
        fieldTicksLived = ReflectionUtils.getField(classEntityFallingBlock, Modifier.PUBLIC, int.class);
        fieldNMSWorld = ReflectionUtils.getField(classNMSEntity, Modifier.PUBLIC, classNMSWorld);
        fieldEnumMoveTypeSelf = ReflectionUtils.getEnumConstant(classEnumMoveType, 0);


        fieldsVec3D = ReflectionUtils
            .getFields(3, classVec3D, ReflectionUtils.getModifiers(Modifier.PUBLIC, Modifier.FINAL), double.class);
    }
}

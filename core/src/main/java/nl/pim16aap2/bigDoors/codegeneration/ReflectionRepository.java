package nl.pim16aap2.bigDoors.codegeneration;

import nl.pim16aap2.bigDoors.util.XMaterial;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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
    public static final Class<?> classNMSWorld;
    public static final Class<?> classNMSWorldServer;
    public static final Class<?> classNMSEntity;
    public static final Class<?> classNMSBlock;
    public static final Class<?> classNMSItem;
    public static final Class<?> classBlockPosition;
    public static final Class<?> classVec3D;
    public static final Class<?> classEnumMoveType;
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

    public static final Constructor<?> cTorNMSFallingBlockEntity;
    public static final Constructor<?> cTorBlockPosition;
    public static final Constructor<?> cTorVec3D;
    public static final Constructor<?> ctorCraftEntity;
    public static final Constructor<?> ctorBlockBase;
    public static final Constructor<?> ctorLocation;

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
    public static final Method methodMatchXMaterial;
    public static final Method methodGetBlockAtCoords;
    public static final Method methodGetBlockAtLoc;
    public static final Method methodIsAssignableFrom;
    public static final Method methodSetBlockType;

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
        classBlockBase = findFirstClass(NMS_BASE + "BlockBase", "net.minecraft.world.level.block.state.BlockBase");
        classBlockBaseInfo = findFirstClass(classBlockBase.getName() + "$Info");
        classCraftBlockData = findClass(CRAFT_BASE + "block.data.CraftBlockData");
        classNMSBlock = findFirstClass(NMS_BASE + "Block", "net.minecraft.world.level.block.Block");
        classNMSItem = findFirstClass(NMS_BASE + "Item", "net.minecraft.world.item.Item");


        cTorNMSFallingBlockEntity = findCTor(classEntityFallingBlock, classNMSWorld, double.class,
                                             double.class, double.class, classIBlockData);
        cTorBlockPosition = findCTor(classBlockPosition, double.class, double.class, double.class);
        cTorVec3D = findCTor(classVec3D, double.class, double.class, double.class);
        ctorCraftEntity = findCTor(classCraftEntity, classCraftServer, classNMSEntity);
        ctorBlockBase = findCTor(classBlockBase, classBlockBaseInfo);
        ctorLocation = findCTor(Location.class, World.class, double.class, double.class, double.class);


        methodGetNMSWorld = getMethod(classCraftWorld, "getHandle");
        methodTick = findMethodFromProfile(classEntityFallingBlock, void.class, Modifier.PUBLIC);
        methodSetPosition = getMethod(classNMSEntity, "setPosition", double.class, double.class, double.class);
        methodSetNoGravity = getMethod(classNMSEntity, "setNoGravity", boolean.class);
        methodSetMot = getMethod(classNMSEntity, "setMot", double.class, double.class, double.class);
        methodSetMotVec = getMethod(classNMSEntity, "setMot", classVec3D);
        methodGetMot = getMethod(classNMSEntity, "getMot");
        methodHurtEntities = findMethodFromProfile(classEntityFallingBlock, boolean.class,
                                                   Modifier.PUBLIC, float.class, float.class, null);
        methodMove = getMethod(true, classNMSEntity, "move", classEnumMoveType, classVec3D);
        methodSaveData = getMethod(classEntityFallingBlock, "saveData", classNBTTagCompound);
        methodLoadData = getMethod(classEntityFallingBlock, "loadData", classNBTTagCompound);
        methodGetBlock = getMethod(classEntityFallingBlock, "getBlock");
        methodSetStartPos = findMethodFromProfile(classEntityFallingBlock, void.class, Modifier.PUBLIC,
                                                  classBlockPosition);
        methodLocX = getMethod(classNMSEntity, "locX");
        methodLocY = getMethod(classNMSEntity, "locY");
        methodLocZ = getMethod(classNMSEntity, "locZ");
        methodNMSAddEntity = getMethod(classNMSWorldServer, "addEntity", classNMSEntity,
                                       CreatureSpawnEvent.SpawnReason.class);
        methodAppendEntityCrashReport = findMethodFromProfile(classEntityFallingBlock, void.class, Modifier.PUBLIC,
                                                              classCrashReportSystemDetails);
        methodCrashReportAppender = findMethodFromProfile(classCrashReportSystemDetails,
                                                          classCrashReportSystemDetails,
                                                          Modifier.PUBLIC, String.class, Object.class);
        methodIsAir = getMethodFullInheritance(classIBlockData, "isAir");
        methodNBTTagCompoundSet = getMethod(classNBTTagCompound, "set", String.class, classNBTBase);
        methodNBTTagCompoundSetInt = getMethod(classNBTTagCompound, "setInt", String.class, int.class);
        methodNBTTagCompoundSetBoolean = getMethod(classNBTTagCompound, "setBoolean", String.class, boolean.class);
        methodNBTTagCompoundSetFloat = getMethod(classNBTTagCompound, "setFloat", String.class, float.class);
        methodNBTTagCompoundGetCompound = getMethod(classNBTTagCompound, "getCompound", String.class);
        methodNBTTagCompoundGetInt = getMethod(classNBTTagCompound, "getInt", String.class);
        methodNBTTagCompoundHasKeyOfType = getMethod(classNBTTagCompound, "hasKeyOfType", String.class, int.class);
        methodIBlockDataSerializer = findMethodFromProfile(classGameProfileSerializer, classNBTTagCompound,
                                                           getModifiers(Modifier.PUBLIC, Modifier.STATIC),
                                                           classIBlockData);
        methodIBlockDataDeserializer = findMethodFromProfile(classGameProfileSerializer, classIBlockData,
                                                             getModifiers(Modifier.PUBLIC, Modifier.STATIC),
                                                             classNBTTagCompound);
        methodCraftMagicNumbersGetMaterial = getMethod(classCraftMagicNumbers, "getMaterial", classIBlockData);
        methodGetItemType = getMethod(MaterialData.class, "getItemType");
        methodCraftEntitySetTicksLived = getMethod(classCraftEntity, "setTicksLived", int.class);
        methodMatchXMaterial = getMethod(XMaterial.class, "matchXMaterial", Material.class);
        methodGetBlockAtCoords = getMethod(World.class, "getBlockAt", int.class, int.class, int.class);
        methodGetBlockAtLoc = getMethod(World.class, "getBlockAt", Location.class);
        methodIsAssignableFrom = getMethod(Class.class, "isAssignableFrom", Class.class);
        methodSetBlockType = getMethod(Block.class, "setType", Material.class);


        fieldTileEntityData = getField(classEntityFallingBlock, Modifier.PUBLIC, classNBTTagCompound);
        fieldTicksLived = getField(classEntityFallingBlock, Modifier.PUBLIC, int.class);
        fieldNMSWorld = getField(classNMSEntity, Modifier.PUBLIC, classNMSWorld);
        fieldEnumMoveTypeSelf = getEnumConstant(classEnumMoveType, 0);


        fieldsVec3D = getFields(3, classVec3D, getModifiers(Modifier.PUBLIC, Modifier.FINAL), double.class);
    }

    private ReflectionRepository()
    {
    }
}

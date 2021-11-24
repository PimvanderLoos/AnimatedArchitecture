package nl.pim16aap2.bigDoors.codegeneration;

import nl.pim16aap2.bigDoors.reflection.asm.ASMUtil;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;

import static nl.pim16aap2.bigDoors.reflection.ReflectionBuilder.findConstructor;
import static nl.pim16aap2.bigDoors.reflection.ReflectionBuilder.findMethod;

@SuppressWarnings("SameParameterValue") //
final class ReflectionASMAnalyzers
{
    private ReflectionASMAnalyzers()
    {
    }

    static Method[] getEntityLocationMethods(Class<?> classCraftEntity, Class<?> classNMSEntity)
    {
        final Method methodLocation = findMethod().inClass(classCraftEntity).withName("getLocation")
                                                  .withoutParameters().get();
        final String[] names = ASMUtil.getMethodNamesFromMethodCall(methodLocation, 3, classNMSEntity, double.class);

        final Method[] ret = new Method[names.length];
        for (int idx = 0; idx < names.length; ++idx)
        {
            final String name = Objects.requireNonNull(names[idx],
                                                       "Name at pos " + idx + " was null! This is not allowed!");
            ret[idx] = findMethod().inClass(classNMSEntity).withName(name).withoutParameters().get();
        }
        return ret;
    }

    static Method getSetNoGravity(Class<?> classCraftEntity, Class<?> classNMSEntity)
    {
        final Method craftMethod = findMethod().inClass(classCraftEntity).withName("setGravity")
                                               .withParameters(boolean.class).get();
        final String methodName = ASMUtil.getMethodNameFromMethodCall(craftMethod, classNMSEntity,
                                                                      void.class, boolean.class);
        return findMethod().inClass(classNMSEntity).withName(methodName).withParameters(boolean.class).get();
    }

    public static Method getSetPosition(Class<?> classNMSEntity)
    {
        final String nameSetPosition =
            ASMUtil.getMethodNameFromMethodCall(findConstructor().inClass(classNMSEntity).get(), classNMSEntity,
                                                void.class, double.class, double.class, double.class);
        return findMethod().inClass(classNMSEntity).withName(nameSetPosition)
                           .withParameters(double.class, double.class, double.class).get();
    }

    /**
     * Gets the saveData and loadData methods.
     *
     * @param classEntityFallingBlock
     *     The nms.EntityFallingBlock class.
     * @param classNBTTagCompound
     *     The nms.NBTTagCompound class.
     * @param methodNBTTagCompoundSetInt
     *     The setInt method of the nms.NBTTagCompound class.
     * @return An array containing the non-null saveData and loadData methods respectively.
     */
    public static Method[] getSaveLoadDataMethods(Class<?> classEntityFallingBlock,
                                                  Class<?> classNBTTagCompound,
                                                  Method methodNBTTagCompoundSetInt)
    {
        // The saveData and loadData methods have the same signature.
        // The difference is that one (saveData) has a bunch of nbtTagCompound.set___(data), while loadData
        // only uses nbtTagCompound's data.
        final List<Method> methodsSaveLoadData = findMethod()
            .inClass(classEntityFallingBlock).findMultiple().withReturnType(void.class)
            .withParameters(classNBTTagCompound).exactCount(2).get();
        final Method method0 = Objects.requireNonNull(methodsSaveLoadData.get(0), "Save/load object 0 cannot be null");
        final Method method1 = Objects.requireNonNull(methodsSaveLoadData.get(1), "Save/load object 1 cannot be null");

        // If method0 has the setInt method, it means that method0 is the saveData method
        // and method1 the loadData method. If it doesn't, it can only be the other way round.
        if (ASMUtil.executableContainsMethodCall(method0, methodNBTTagCompoundSetInt))
            return new Method[]{method0, method1};
        return new Method[]{method1, method0};
    }

    public static Method getSetMotVecMethod(Class<?> classCraftEntity, Class<?> classNMSEntity, Class<?> classVec3D)
    {
        final Method sourceMethod = findMethod().inClass(classCraftEntity).withName("setVelocity").get();
        final String methodName = ASMUtil.getMethodNameFromMethodCall(sourceMethod, classNMSEntity,
                                                                      void.class, classVec3D);
        return findMethod().inClass(classNMSEntity).withName(methodName).withParameters(classVec3D).get();
    }

    public static Method getGetMotMethod(Class<?> classCraftEntity, Class<?> classNMSEntity, Class<?> classVec3D)
    {
        final Method sourceMethod = findMethod().inClass(classCraftEntity).withName("getVelocity").get();
        final String methodName = ASMUtil.getMethodNameFromMethodCall(sourceMethod, classNMSEntity, classVec3D);
        return findMethod().inClass(classNMSEntity).withName(methodName).withoutParameters().get();
    }

    public static Method getIsAirMethod(Method methodTick, Class<?> classIBlockData, Class<?> classBlockData)
    {
        final String methodName = ASMUtil.getMethodNameFromMethodCall(methodTick, classIBlockData, boolean.class);
        return findMethod().inClass(classBlockData).withName(methodName).withoutParameters().get();
    }

    public static Method getCraftEntityDelegationMethod(Class<?> classCraftEntity, Class<?> classNMSEntity)
    {
        final Method sourceMethod = findMethod().inClass(classCraftEntity).withName("remove").withoutParameters().get();
        final String methodName = ASMUtil.getMethodNameFromMethodCall(sourceMethod, classNMSEntity, void.class);
        return findMethod().inClass(classNMSEntity).withName(methodName).withoutParameters().get();
    }

    public static Method getNMSAddEntityMethod(Class<?> classNMSWorldServer, Class<?> classNMSEntity)
    {
        final List<Method> candidates = findMethod()
            .inClass(classNMSWorldServer).findMultiple().withReturnType(boolean.class)
            .withParameters(classNMSEntity, CreatureSpawnEvent.SpawnReason.class).get();
        final Method privateMethod = findMethod().inClass(classNMSWorldServer).withReturnType(boolean.class)
                                                 .withParameters(classNMSEntity, CreatureSpawnEvent.SpawnReason.class)
                                                 .withModifiers(Modifier.PRIVATE).get();
        for (Method method : candidates)
            if (ASMUtil.executableContainsMethodCall(method, privateMethod))
                return method;
        throw new IllegalStateException("Could not find method with call to " + privateMethod.toGenericString()
                                            + " among candidates: " + candidates);
    }

    public static Method getGetIBlockDataHolderStateMethod(Class<?> classCraftBlockData, Class<?> classBlockStateEnum,
                                                           Class<?> classIBlockData, Class<?> classIBlockState,
                                                           Class<?> classIBlockDataHolder)
    {
        final Method sourceMethod = findMethod().inClass(classCraftBlockData).withName("get")
                                                .withParameters(classBlockStateEnum, Class.class).get();
        final String methodName = ASMUtil.getMethodNameFromMethodCall(sourceMethod, classIBlockData,
                                                                      Comparable.class, classIBlockState);
        return findMethod().inClass(classIBlockDataHolder).withName(methodName).withParameters(classIBlockState).get();
    }
}

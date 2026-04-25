package et.xiaomu.essentialsd.managers.inspect;

import cn.lunadeer.utils.XLogger;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

final class NbtBridge {
    private static final int TAG_COMPOUND = 10;

    private final Class<?> craftItemStackClass;
    private final Class<?> nmsItemStackClass;
    private final Class<?> compoundTagClass;
    private final Class<?> listTagClass;
    private final Class<?> tagClass;
    private final Class<?> providerClass;
    private final Object registryAccess;
    private final Constructor<?> compoundCtor;
    private final Constructor<?> listCtor;
    private final Method asBukkitCopyMethod;
    private final Method asNmsCopyMethod;
    private final Method readCompressedMethod;
    private final Method writeCompressedMethod;
    private final Method putMethod;
    private final Method putByteMethod;
    private final Method getListMethod;
    private final Method getByteMethod;
    private final Method getCompoundMethod;
    private final Method containsMethod;
    private final Class<?> codecClass;
    private final Class<?> dynamicOpsClass;
    private final Object serializationOps;
    private final Method codecParseMethod;
    private final Method codecEncodeStartMethod;
    private final Method dataResultMethod;
    private final Method dataErrorMethod;
    private final List<Field> itemCodecFields;
    private volatile Field selectedItemCodecField;
    private final byte getByteFallbackValue = 0;

    NbtBridge() {
        try {
            String craftBukkitPackage = Bukkit.getServer().getClass().getPackage().getName();
            this.craftItemStackClass = Class.forName(craftBukkitPackage + ".inventory.CraftItemStack");
            this.nmsItemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
            this.compoundTagClass = Class.forName("net.minecraft.nbt.CompoundTag");
            this.listTagClass = Class.forName("net.minecraft.nbt.ListTag");
            this.tagClass = Class.forName("net.minecraft.nbt.Tag");
            this.providerClass = Class.forName("net.minecraft.core.HolderLookup$Provider");
            this.compoundCtor = compoundTagClass.getConstructor();
            this.listCtor = listTagClass.getConstructor();
            this.asBukkitCopyMethod = craftItemStackClass.getMethod("asBukkitCopy", nmsItemStackClass);
            this.asNmsCopyMethod = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
            this.putMethod = findPutMethod(compoundTagClass, tagClass);
            this.putByteMethod = findPutByteMethod(compoundTagClass);
            this.getListMethod = findListGetter(compoundTagClass, listTagClass);
            this.getByteMethod = findByteGetter(compoundTagClass);
            this.getCompoundMethod = findCompoundGetter(compoundTagClass);
            this.containsMethod = findContainsMethod(compoundTagClass);
            Object serverHandle = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            this.registryAccess = findProvider(serverHandle);
            this.readCompressedMethod = findStaticMethod(
                    Class.forName("net.minecraft.nbt.NbtIo"),
                    compoundTagClass,
                    InputStream.class,
                    Class.forName("net.minecraft.nbt.NbtAccounter")
            );
            this.writeCompressedMethod = findStaticMethod(
                    Class.forName("net.minecraft.nbt.NbtIo"),
                    void.class,
                    compoundTagClass,
                    Path.class
            );
            this.codecClass = Class.forName("com.mojang.serialization.Codec");
            this.dynamicOpsClass = Class.forName("com.mojang.serialization.DynamicOps");
            this.serializationOps = createSerializationOps();
            this.codecParseMethod = codecClass.getMethod("parse", dynamicOpsClass, Object.class);
            this.codecEncodeStartMethod = codecClass.getMethod("encodeStart", dynamicOpsClass, Object.class);
            Class<?> dataResultClass = Class.forName("com.mojang.serialization.DataResult");
            this.dataResultMethod = dataResultClass.getMethod("result");
            this.dataErrorMethod = dataResultClass.getMethod("error");
            this.itemCodecFields = findItemCodecFields();
        } catch (Exception e) {
            throw new IllegalStateException("无法初始化离线背包 NBT 桥接: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
        }
    }

    Object read(Path path) throws Exception {
        try (InputStream input = Files.newInputStream(path)) {
            return readCompressedMethod.invoke(null, input, createUnlimitedAccounter());
        }
    }

    void write(Object rootTag, Path path) throws Exception {
        writeCompressedMethod.invoke(null, rootTag, path);
    }

    ItemStack[] readPlayerInventory(Object rootTag) throws Exception {
        ItemStack[] items = new ItemStack[InspectManager.PLAYER_SLOT_COUNT];
        Object list = getList(rootTag, "Inventory");
        for (Object entry : (Iterable<?>) list) {
            byte slot = getByte(entry, "Slot");
            int logical = nbtInventorySlotToLogical(slot);
            if (logical >= 0) {
                items[logical] = readItemSafely(entry, "背包槽位 " + logical);
            }
        }
        applyEquipment(rootTag, items);
        return items;
    }

    ItemStack[] readEnderChest(Object rootTag) throws Exception {
        ItemStack[] items = new ItemStack[InspectManager.ENDER_SLOT_COUNT];
        Object list = getList(rootTag, "EnderItems");
        for (Object entry : (Iterable<?>) list) {
            int slot = Byte.toUnsignedInt(getByte(entry, "Slot"));
            if (slot >= 0 && slot < InspectManager.ENDER_SLOT_COUNT) {
                items[slot] = readItemSafely(entry, "末影箱槽位 " + slot);
            }
        }
        return items;
    }

    void writePlayerInventory(Object rootTag, ItemStack[] items) throws Exception {
        Object list = listCtor.newInstance();
        @SuppressWarnings("unchecked")
        List<Object> entries = (List<Object>) list;
        for (int i = 0; i < items.length; i++) {
            ItemStack item = InspectManager.normalize(items[i]);
            if (item == null) {
                continue;
            }
            Object entry = toTag(item);
            putByteMethod.invoke(entry, "Slot", logicalInventorySlotToNbt(i));
            entries.add(entry);
        }
        putMethod.invoke(rootTag, "Inventory", list);

        Object equipment = compoundCtor.newInstance();
        writeEquipmentSlot(equipment, "feet", items, 36);
        writeEquipmentSlot(equipment, "legs", items, 37);
        writeEquipmentSlot(equipment, "chest", items, 38);
        writeEquipmentSlot(equipment, "head", items, 39);
        writeEquipmentSlot(equipment, "offhand", items, 40);
        putMethod.invoke(rootTag, "equipment", equipment);
    }

    void writeEnderChest(Object rootTag, ItemStack[] items) throws Exception {
        Object list = listCtor.newInstance();
        @SuppressWarnings("unchecked")
        List<Object> entries = (List<Object>) list;
        for (int i = 0; i < items.length; i++) {
            ItemStack item = InspectManager.normalize(items[i]);
            if (item == null) {
                continue;
            }
            Object entry = toTag(item);
            putByteMethod.invoke(entry, "Slot", (byte) i);
            entries.add(entry);
        }
        putMethod.invoke(rootTag, "EnderItems", list);
    }

    private ItemStack fromTag(Object tag) throws Exception {
        Object nmsItem = parseItem(tag);
        return InspectManager.normalize((ItemStack) asBukkitCopyMethod.invoke(null, nmsItem));
    }

    private Object toTag(ItemStack item) throws Exception {
        Object nmsItem = asNmsCopyMethod.invoke(null, item);
        return saveItem(nmsItem);
    }

    private static byte logicalInventorySlotToNbt(int slot) {
        if (slot >= 0 && slot <= 35) {
            return (byte) slot;
        }
        return switch (slot) {
            case 36 -> 100;
            case 37 -> 101;
            case 38 -> 102;
            case 39 -> 103;
            case 40 -> (byte) -106;
            default -> throw new IllegalArgumentException("非法的背包槽位: " + slot);
        };
    }

    private static int nbtInventorySlotToLogical(byte slot) {
        if (slot >= 0 && slot <= 35) {
            return slot;
        }
        return switch (slot) {
            case 100 -> 36;
            case 101 -> 37;
            case 102 -> 38;
            case 103 -> 39;
            case -106 -> 40;
            default -> -1;
        };
    }

    private static Method findByNameAndParams(Class<?> type, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && Arrays.equals(method.getParameterTypes(), parameterTypes)) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + "#" + name);
    }

    private static Method findStaticMethod(Class<?> type, Class<?> returnType, Class<?>... parameterTypes) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!Arrays.equals(method.getParameterTypes(), parameterTypes)) {
                continue;
            }
            if (!returnType.isAssignableFrom(method.getReturnType()) && method.getReturnType() != returnType) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        throw new NoSuchMethodException(type.getName());
    }

    private static Method findInstanceMethod(Class<?> type, Class<?> returnType, Class<?>... parameterTypes) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!Arrays.equals(method.getParameterTypes(), parameterTypes)) {
                continue;
            }
            if (!returnType.isAssignableFrom(method.getReturnType()) && method.getReturnType() != returnType) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        throw new NoSuchMethodException(type.getName());
    }

    private Object findProvider(Object serverHandle) throws Exception {
        for (Method method : serverHandle.getClass().getMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            if (!providerClass.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            method.setAccessible(true);
            return method.invoke(serverHandle);
        }
        throw new NoSuchMethodException("MinecraftServer registry access");
    }

    private static Object createUnlimitedAccounter() throws Exception {
        Class<?> accounterClass = Class.forName("net.minecraft.nbt.NbtAccounter");
        Method fallbackNoArgFactory = null;
        for (Method method : accounterClass.getMethods()) {
            if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getParameterCount() != 0) {
                continue;
            }
            if (!accounterClass.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            if (method.getName().toLowerCase(java.util.Locale.ROOT).contains("unlimited")) {
                method.setAccessible(true);
                return method.invoke(null);
            }
            if (fallbackNoArgFactory == null) {
                fallbackNoArgFactory = method;
            }
        }
        for (Method method : accounterClass.getMethods()) {
            if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                continue;
            }
            if (!accounterClass.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            Class<?> parameterType = method.getParameterTypes()[0];
            if (parameterType != long.class && parameterType != Long.class) {
                continue;
            }
            method.setAccessible(true);
            return method.invoke(null, Long.MAX_VALUE);
        }
        try {
            Constructor<?> constructor = accounterClass.getDeclaredConstructor(long.class);
            constructor.setAccessible(true);
            return constructor.newInstance(Long.MAX_VALUE);
        } catch (NoSuchMethodException ignored) {
        }
        if (fallbackNoArgFactory != null) {
            fallbackNoArgFactory.setAccessible(true);
            return fallbackNoArgFactory.invoke(null);
        }
        throw new NoSuchMethodException("NbtAccounter unlimited factory");
    }

    private static Method findListGetter(Class<?> compoundTagClass, Class<?> listTagClass) throws NoSuchMethodException {
        for (Method method : compoundTagClass.getMethods()) {
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!Arrays.equals(method.getParameterTypes(), new Class[]{String.class})) {
                continue;
            }
            if (listTagClass.isAssignableFrom(method.getReturnType())) {
                method.setAccessible(true);
                return method;
            }
        }
        for (Method method : compoundTagClass.getMethods()) {
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!Arrays.equals(method.getParameterTypes(), new Class[]{String.class})) {
                continue;
            }
            if (Optional.class.isAssignableFrom(method.getReturnType()) && optionalGenericMatches(method, listTagClass)) {
                method.setAccessible(true);
                return method;
            }
        }
        for (Method method : compoundTagClass.getMethods()) {
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!Arrays.equals(method.getParameterTypes(), new Class[]{String.class, int.class})) {
                continue;
            }
            if (listTagClass.isAssignableFrom(method.getReturnType())) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException("CompoundTag list getter");
    }

    private static Method findByteGetter(Class<?> compoundTagClass) throws NoSuchMethodException {
        for (Method method : compoundTagClass.getMethods()) {
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!Arrays.equals(method.getParameterTypes(), new Class[]{String.class, byte.class})) {
                continue;
            }
            if (method.getReturnType() == byte.class || method.getReturnType() == Byte.TYPE) {
                method.setAccessible(true);
                return method;
            }
        }
        for (Method method : compoundTagClass.getMethods()) {
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!Arrays.equals(method.getParameterTypes(), new Class[]{String.class})) {
                continue;
            }
            if (method.getReturnType() == byte.class || method.getReturnType() == Byte.TYPE) {
                method.setAccessible(true);
                return method;
            }
            if (Optional.class.isAssignableFrom(method.getReturnType()) && optionalGenericMatches(method, Byte.class)) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException("CompoundTag byte getter");
    }

    private static Method findCompoundGetter(Class<?> compoundTagClass) throws NoSuchMethodException {
        for (Method method : compoundTagClass.getMethods()) {
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!Arrays.equals(method.getParameterTypes(), new Class[]{String.class})) {
                continue;
            }
            if (compoundTagClass.isAssignableFrom(method.getReturnType())) {
                method.setAccessible(true);
                return method;
            }
            if (Optional.class.isAssignableFrom(method.getReturnType()) && optionalGenericMatches(method, compoundTagClass)) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException("CompoundTag compound getter");
    }

    private static Method findContainsMethod(Class<?> compoundTagClass) throws NoSuchMethodException {
        for (Method method : compoundTagClass.getMethods()) {
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!Arrays.equals(method.getParameterTypes(), new Class[]{String.class})) {
                continue;
            }
            if (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.TYPE) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException("CompoundTag contains");
    }

    private static Method findPutMethod(Class<?> compoundTagClass, Class<?> tagClass) throws NoSuchMethodException {
        for (Method method : compoundTagClass.getMethods()) {
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!Arrays.equals(method.getParameterTypes(), new Class[]{String.class, tagClass})) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        throw new NoSuchMethodException("CompoundTag put");
    }

    private static Method findPutByteMethod(Class<?> compoundTagClass) throws NoSuchMethodException {
        for (Method method : compoundTagClass.getMethods()) {
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (!Arrays.equals(method.getParameterTypes(), new Class[]{String.class, byte.class})) {
                continue;
            }
            method.setAccessible(true);
            return method;
        }
        throw new NoSuchMethodException("CompoundTag putByte");
    }

    private Object getList(Object rootTag, String key) throws Exception {
        if (getListMethod.getParameterCount() == 1) {
            Object value = getListMethod.invoke(rootTag, key);
            if (value instanceof Optional<?> optional) {
                if (optional.isPresent()) {
                    return optional.get();
                }
                return listCtor.newInstance();
            }
            if (value != null) {
                return value;
            }
        } else if (getListMethod.getParameterCount() == 2) {
            return getListMethod.invoke(rootTag, key, TAG_COMPOUND);
        }
        return listCtor.newInstance();
    }

    private Object getCompound(Object rootTag, String key) throws Exception {
        Object value = getCompoundMethod.invoke(rootTag, key);
        if (value instanceof Optional<?> optional) {
            return optional.orElse(null);
        }
        return value;
    }

    private boolean containsKey(Object rootTag, String key) throws Exception {
        Object value = containsMethod.invoke(rootTag, key);
        return Boolean.TRUE.equals(value);
    }

    private byte getByte(Object tag, String key) throws Exception {
        if (getByteMethod.getParameterCount() == 2) {
            Object value = getByteMethod.invoke(tag, key, getByteFallbackValue);
            return toByte(value);
        }
        Object value = getByteMethod.invoke(tag, key);
        if (value instanceof Optional<?> optional) {
            return optional.isPresent() ? toByte(optional.get()) : getByteFallbackValue;
        }
        return toByte(value);
    }

    private Object parseItem(Object tag) throws Exception {
        Field lastErrorCodec = null;
        Exception lastError = null;
        String lastErrorMessage = null;
        for (Field codecField : itemCodecFields) {
            try {
                Object codec = codecField.get(null);
                Object dataResult = codecParseMethod.invoke(codec, serializationOps, tag);
                Optional<?> optional = (Optional<?>) dataResultMethod.invoke(dataResult);
                if (optional.isPresent() && nmsItemStackClass.isInstance(optional.get())) {
                    selectedItemCodecField = codecField;
                    return optional.get();
                }
                Optional<?> error = (Optional<?>) dataErrorMethod.invoke(dataResult);
                if (error.isPresent()) {
                    lastErrorCodec = codecField;
                    lastErrorMessage = String.valueOf(error.get());
                }
            } catch (Exception e) {
                lastErrorCodec = codecField;
                lastError = e;
                lastErrorMessage = unwrapThrowable(e).getMessage();
            }
        }
        if (lastError != null) {
            throw new IllegalStateException("无法通过 ItemStack Codec 解析物品: " + lastErrorCodec.getName(), lastError);
        }
        if (lastErrorCodec != null) {
            throw new IllegalStateException("无法从 NBT 解析物品: " + lastErrorCodec.getName() +
                    (lastErrorMessage == null || lastErrorMessage.isBlank() ? "" : " - " + lastErrorMessage));
        }
        throw new IllegalStateException("无法从 NBT 解析物品");
    }

    private Object saveItem(Object nmsItem) throws Exception {
        List<Field> codecsToTry = new java.util.ArrayList<>();
        if (selectedItemCodecField != null) {
            codecsToTry.add(selectedItemCodecField);
        }
        for (Field codecField : itemCodecFields) {
            if (codecField != selectedItemCodecField) {
                codecsToTry.add(codecField);
            }
        }
        Exception lastError = null;
        for (Field codecField : codecsToTry) {
            try {
                Object codec = codecField.get(null);
                Object dataResult = codecEncodeStartMethod.invoke(codec, serializationOps, nmsItem);
                Optional<?> optional = (Optional<?>) dataResultMethod.invoke(dataResult);
                if (optional.isPresent() && compoundTagClass.isInstance(optional.get())) {
                    selectedItemCodecField = codecField;
                    return optional.get();
                }
            } catch (Exception e) {
                lastError = e;
            }
        }
        if (lastError != null) {
            throw new IllegalStateException("无法通过 ItemStack Codec 写出物品", lastError);
        }
        throw new IllegalStateException("无法将物品编码为 CompoundTag");
    }

    private static byte toByte(Object value) {
        if (value instanceof Number number) {
            return number.byteValue();
        }
        throw new IllegalStateException("无法将值转换为 byte: " + value);
    }

    private void applyEquipment(Object rootTag, ItemStack[] items) throws Exception {
        if (!containsKey(rootTag, "equipment")) {
            return;
        }
        Object equipment = getCompound(rootTag, "equipment");
        if (equipment == null) {
            return;
        }
        applyEquipmentSlot(equipment, "feet", items, 36);
        applyEquipmentSlot(equipment, "legs", items, 37);
        applyEquipmentSlot(equipment, "chest", items, 38);
        applyEquipmentSlot(equipment, "head", items, 39);
        applyEquipmentSlot(equipment, "offhand", items, 40);
    }

    private void applyEquipmentSlot(Object equipment, String key, ItemStack[] items, int slot) throws Exception {
        if (!containsKey(equipment, key)) {
            return;
        }
        Object itemTag = getCompound(equipment, key);
        if (itemTag == null) {
            return;
        }
        items[slot] = readItemSafely(itemTag, "装备槽 " + key);
    }

    private void writeEquipmentSlot(Object equipment, String key, ItemStack[] items, int slot) throws Exception {
        if (slot < 0 || slot >= items.length) {
            return;
        }
        ItemStack item = InspectManager.normalize(items[slot]);
        if (item == null) {
            return;
        }
        putMethod.invoke(equipment, key, toTag(item));
    }

    private ItemStack readItemSafely(Object tag, String slotName) {
        try {
            return fromTag(tag);
        } catch (Exception e) {
            Throwable root = unwrapThrowable(e);
            String message = root.getMessage();
            XLogger.warn("跳过无法解析的离线物品: {0} | {1}{2}",
                    slotName,
                    root.getClass().getSimpleName(),
                    message == null || message.isBlank() ? "" : " - " + message);
            return null;
        }
    }

    private static Throwable unwrapThrowable(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current != current.getCause()) {
            current = current.getCause();
        }
        return current;
    }

    private static boolean optionalGenericMatches(Method method, Class<?> targetType) {
        Type genericReturnType = method.getGenericReturnType();
        if (!(genericReturnType instanceof ParameterizedType parameterizedType)) {
            return false;
        }
        if (!(parameterizedType.getRawType() instanceof Class<?> rawType) || rawType != Optional.class) {
            return false;
        }
        Type[] arguments = parameterizedType.getActualTypeArguments();
        if (arguments.length != 1) {
            return false;
        }
        Type argument = arguments[0];
        if (argument instanceof Class<?> argumentClass) {
            return argumentClass == targetType || targetType.isAssignableFrom(argumentClass);
        }
        return false;
    }

    private Object createSerializationOps() throws Exception {
        Object nbtOps = findNbtOpsInstance();
        for (Method method : providerClass.getMethods()) {
            if (method.getParameterCount() != 1) {
                continue;
            }
            if (!dynamicOpsClass.isAssignableFrom(method.getParameterTypes()[0])) {
                continue;
            }
            if (!dynamicOpsClass.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            method.setAccessible(true);
            return method.invoke(registryAccess, nbtOps);
        }
        throw new NoSuchMethodException("HolderLookup.Provider#createSerializationContext");
    }

    private Object findNbtOpsInstance() throws Exception {
        Class<?> nbtOpsClass = Class.forName("net.minecraft.nbt.NbtOps");
        for (Field field : nbtOpsClass.getFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!nbtOpsClass.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            return field.get(null);
        }
        for (Method method : nbtOpsClass.getMethods()) {
            if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getParameterCount() != 0) {
                continue;
            }
            if (!nbtOpsClass.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            method.setAccessible(true);
            return method.invoke(null);
        }
        throw new NoSuchMethodException("NbtOps instance");
    }

    private List<Field> findItemCodecFields() throws NoSuchMethodException {
        List<Field> fields = new java.util.ArrayList<>();
        for (Field field : nmsItemStackClass.getFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!codecClass.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            fields.add(field);
        }
        if (fields.isEmpty()) {
            throw new NoSuchMethodException("ItemStack codec fields");
        }
        fields.sort(java.util.Comparator.comparing(Field::getName));
        return fields;
    }

}

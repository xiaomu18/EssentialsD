package et.xiaomu.essentialsd.managers.inspect;

import cn.lunadeer.utils.XLogger;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OfflinePlayerDataAccess {
    private static final Pattern PLAY_TIME_PATTERN = Pattern.compile("\"minecraft:(?:play_time|play_one_minute)\"\\s*:\\s*(\\d+)");
    private static volatile NbtBridge nbtBridge;
    private static volatile ReflectionAccess reflectionAccess;

    private OfflinePlayerDataAccess() {
    }

    public static @Nullable PlayerDataSnapshot loadSnapshot(OfflinePlayer player) {
        try {
            Path path = playerDataFile(player.getUniqueId());
            if (path == null || !Files.exists(path)) {
                return null;
            }

            Object rootTag = bridge().read(path);
            ReflectionAccess access = access(rootTag);
            Object abilities = access.getCompound(rootTag, "abilities");
            double[] pos = access.getDoubleList(rootTag, "Pos", 3);
            String worldName = resolveWorldName(access, rootTag);
            long lastSeen = player.getLastPlayed();
            if (lastSeen <= 0L) {
                lastSeen = Files.getLastModifiedTime(path).toMillis();
            }

            return new PlayerDataSnapshot(
                    worldName,
                    pos.length > 0 ? pos[0] : 0.0D,
                    pos.length > 1 ? pos[1] : 0.0D,
                    pos.length > 2 ? pos[2] : 0.0D,
                    gameModeFromId(access.getInt(rootTag, "playerGameType", 0)),
                    access.getBoolean(rootTag, "Invulnerable", false),
                    abilities != null && access.getBoolean(abilities, "mayfly", false),
                    abilities != null ? access.getFloat(abilities, "flySpeed", 0.1F) : 0.1F,
                    abilities != null ? access.getFloat(abilities, "walkSpeed", 0.2F) : 0.2F,
                    lastSeen,
                    readTotalPlayTime(player.getUniqueId())
            );
        } catch (Exception e) {
            XLogger.error("读取离线玩家信息失败: {0}", player.getUniqueId());
            XLogger.error(e);
            return null;
        }
    }

    public static boolean clear(OfflinePlayer player, InspectManager.Mode mode) {
        try {
            Path path = playerDataFile(player.getUniqueId());
            if (path == null || !Files.exists(path)) {
                return false;
            }

            NbtBridge nbt = bridge();
            Object rootTag = nbt.read(path);
            ItemStack[] empty = mode == InspectManager.Mode.ENDER_CHEST
                    ? new ItemStack[InspectManager.ENDER_SLOT_COUNT]
                    : new ItemStack[InspectManager.PLAYER_SLOT_COUNT];
            if (mode == InspectManager.Mode.ENDER_CHEST) {
                nbt.writeEnderChest(rootTag, empty);
            } else {
                nbt.writePlayerInventory(rootTag, empty);
            }
            nbt.write(rootTag, path);
            return true;
        } catch (Exception e) {
            XLogger.error("清空离线玩家数据失败: {0}", player.getUniqueId());
            XLogger.error(e);
            return false;
        }
    }

    private static String resolveWorldName(ReflectionAccess access, Object rootTag) throws Exception {
        String dimension = access.getString(rootTag, "Dimension", null);
        if (dimension != null && !dimension.isBlank()) {
            for (World world : Bukkit.getWorlds()) {
                if (world.getName().equalsIgnoreCase(dimension) || world.getKey().toString().equalsIgnoreCase(dimension)) {
                    return world.getName();
                }
            }
            return dimension;
        }

        long worldUuidMost = access.getLong(rootTag, "WorldUUIDMost", 0L);
        long worldUuidLeast = access.getLong(rootTag, "WorldUUIDLeast", 0L);
        if (worldUuidMost != 0L || worldUuidLeast != 0L) {
            UUID uuid = new UUID(worldUuidMost, worldUuidLeast);
            World world = Bukkit.getWorld(uuid);
            if (world != null) {
                return world.getName();
            }
        }
        return Bukkit.getWorlds().isEmpty() ? "unknown" : Bukkit.getWorlds().get(0).getName();
    }

    private static GameMode gameModeFromId(int id) {
        return switch (id) {
            case 1 -> GameMode.CREATIVE;
            case 2 -> GameMode.ADVENTURE;
            case 3 -> GameMode.SPECTATOR;
            default -> GameMode.SURVIVAL;
        };
    }

    private static @Nullable Long readTotalPlayTime(UUID uuid) {
        try {
            Path path = statsFile(uuid);
            if (path == null || !Files.exists(path)) {
                return null;
            }
            String content = Files.readString(path);
            Matcher matcher = PLAY_TIME_PATTERN.matcher(content);
            if (!matcher.find()) {
                return null;
            }
            long ticks = Long.parseLong(matcher.group(1));
            return ticks * 50L;
        } catch (Exception e) {
            XLogger.warn("读取离线统计失败: {0}", uuid);
            return null;
        }
    }

    private static NbtBridge bridge() {
        if (nbtBridge == null) {
            nbtBridge = new NbtBridge();
        }
        return nbtBridge;
    }

    private static ReflectionAccess access(Object rootTag) throws ClassNotFoundException, NoSuchMethodException {
        if (reflectionAccess == null) {
            reflectionAccess = new ReflectionAccess(rootTag.getClass(), Class.forName("net.minecraft.nbt.ListTag"));
        }
        return reflectionAccess;
    }

    static @Nullable Path playerDataFile(UUID uuid) {
        return resolveDataFile("playerdata", uuid + ".dat");
    }

    private static @Nullable Path statsFile(UUID uuid) {
        return resolveDataFile("stats", uuid + ".json");
    }

    private static @Nullable Path resolveDataFile(String directoryName, String fileName) {
        Path bestMatch = Bukkit.getWorlds().stream()
                .map(World::getWorldFolder)
                .map(java.io.File::toPath)
                .map(path -> path.resolve(directoryName).resolve(fileName))
                .filter(Files::exists)
                .max(Comparator.comparingLong(OfflinePlayerDataAccess::lastModified))
                .orElse(null);
        if (bestMatch != null) {
            return bestMatch;
        }

        Path worldContainer = Bukkit.getWorldContainer().toPath();
        try (java.util.stream.Stream<Path> stream = Files.find(worldContainer, 6, (path, attrs) ->
                attrs.isRegularFile()
                        && fileName.equals(path.getFileName().toString())
                        && path.getParent() != null
                        && directoryName.equals(path.getParent().getFileName().toString()))) {
            return stream.max(Comparator.comparingLong(OfflinePlayerDataAccess::lastModified)).orElse(null);
        } catch (Exception e) {
            XLogger.warn("搜索离线数据文件失败: {0}/{1} - {2}", directoryName, fileName, e.getMessage());
            return null;
        }
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception ignored) {
            return Long.MIN_VALUE;
        }
    }

    public record PlayerDataSnapshot(
            String worldName,
            double x,
            double y,
            double z,
            GameMode gameMode,
            boolean invulnerable,
            boolean allowFlight,
            float flySpeed,
            float walkSpeed,
            long lastSeenMillis,
            @Nullable Long totalPlayTimeMillis
    ) {
    }

    private static final class ReflectionAccess {
        private final Method getCompoundMethod;
        private final Method getListMethod;
        private final Method getStringMethod;
        private final Method getIntMethod;
        private final Method getLongMethod;
        private final Method getFloatMethod;
        private final Method getBooleanMethod;
        private final Method getByteMethod;
        private volatile Method numericTagDoubleMethod;

        private ReflectionAccess(Class<?> compoundTagClass, Class<?> listTagClass) throws NoSuchMethodException {
            this.getCompoundMethod = findCompoundGetter(compoundTagClass);
            this.getListMethod = findListGetter(compoundTagClass, listTagClass);
            this.getStringMethod = findValueGetter(compoundTagClass, String.class, String.class);
            this.getIntMethod = findNumericGetter(compoundTagClass, int.class, int.class);
            this.getLongMethod = findNumericGetter(compoundTagClass, long.class, long.class);
            this.getFloatMethod = findNumericGetter(compoundTagClass, float.class, float.class);
            this.getBooleanMethod = findBooleanGetter(compoundTagClass);
            this.getByteMethod = findByteGetter(compoundTagClass);
        }

        private Object getCompound(Object rootTag, String key) throws Exception {
            Object value = getCompoundMethod.invoke(rootTag, key);
            if (value instanceof Optional<?> optional) {
                return optional.orElse(null);
            }
            return value;
        }

        private Object getList(Object rootTag, String key) throws Exception {
            if (getListMethod.getParameterCount() == 1) {
                Object value = getListMethod.invoke(rootTag, key);
                if (value instanceof Optional<?> optional) {
                    return optional.orElse(null);
                }
                return value;
            }
            return getListMethod.invoke(rootTag, key, 6);
        }

        private String getString(Object rootTag, String key, String fallback) throws Exception {
            Object value = invokeValueGetter(getStringMethod, rootTag, key, fallback);
            if (value instanceof Optional<?> optional) {
                value = optional.orElse(null);
            }
            return value instanceof String string && !string.isBlank() ? string : fallback;
        }

        private int getInt(Object rootTag, String key, int fallback) throws Exception {
            Object value = invokeValueGetter(getIntMethod, rootTag, key, fallback);
            value = unwrapOptional(value);
            return value instanceof Number number ? number.intValue() : fallback;
        }

        private long getLong(Object rootTag, String key, long fallback) throws Exception {
            Object value = invokeValueGetter(getLongMethod, rootTag, key, fallback);
            value = unwrapOptional(value);
            return value instanceof Number number ? number.longValue() : fallback;
        }

        private float getFloat(Object rootTag, String key, float fallback) throws Exception {
            Object value = invokeValueGetter(getFloatMethod, rootTag, key, fallback);
            value = unwrapOptional(value);
            return value instanceof Number number ? number.floatValue() : fallback;
        }

        private boolean getBoolean(Object rootTag, String key, boolean fallback) throws Exception {
            if (getBooleanMethod != null) {
                Object value = invokeValueGetter(getBooleanMethod, rootTag, key, fallback);
                value = unwrapOptional(value);
                if (value instanceof Boolean bool) {
                    return bool;
                }
            }
            Object value = invokeValueGetter(getByteMethod, rootTag, key, (byte) (fallback ? 1 : 0));
            value = unwrapOptional(value);
            return value instanceof Number number ? number.byteValue() != 0 : fallback;
        }

        private double[] getDoubleList(Object rootTag, String key, int expectedLength) throws Exception {
            Object list = getList(rootTag, key);
            if (list == null) {
                return new double[0];
            }
            if (!(list instanceof List<?> values) || values.isEmpty()) {
                return new double[0];
            }
            int size = Math.min(expectedLength, values.size());
            double[] result = new double[size];
            for (int i = 0; i < size; i++) {
                result[i] = readDouble(values.get(i));
            }
            return result;
        }

        private double readDouble(Object value) throws Exception {
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value == null) {
                return 0.0D;
            }

            Method method = numericTagDoubleMethod;
            if (method == null || !method.getDeclaringClass().isInstance(value)) {
                method = findNumericValueMethod(value.getClass());
                numericTagDoubleMethod = method;
            }
            Object result = method.invoke(value);
            return result instanceof Number number ? number.doubleValue() : 0.0D;
        }

        private static Object invokeValueGetter(Method method, Object target, String key, Object fallback) throws Exception {
            if (method.getParameterCount() == 1) {
                return method.invoke(target, key);
            }
            return method.invoke(target, key, fallback);
        }

        private static Object unwrapOptional(Object value) {
            if (value instanceof Optional<?> optional) {
                return optional.orElse(null);
            }
            return value;
        }

        private static Method findCompoundGetter(Class<?> compoundTagClass) throws NoSuchMethodException {
            for (Method method : compoundTagClass.getMethods()) {
                if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != String.class) {
                    continue;
                }
                if (method.getReturnType() == compoundTagClass
                        || Optional.class.isAssignableFrom(method.getReturnType()) && optionalGenericMatches(method, compoundTagClass)) {
                    method.setAccessible(true);
                    return method;
                }
            }
            throw new NoSuchMethodException("CompoundTag compound getter");
        }

        private static Method findListGetter(Class<?> compoundTagClass, Class<?> listTagClass) throws NoSuchMethodException {
            for (Method method : compoundTagClass.getMethods()) {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class) {
                    if (listTagClass.isAssignableFrom(method.getReturnType())
                            || Optional.class.isAssignableFrom(method.getReturnType()) && optionalGenericMatches(method, listTagClass)) {
                        method.setAccessible(true);
                        return method;
                    }
                }
                if (method.getParameterCount() == 2 && method.getParameterTypes()[0] == String.class && method.getParameterTypes()[1] == int.class) {
                    if (listTagClass.isAssignableFrom(method.getReturnType())) {
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
            throw new NoSuchMethodException("CompoundTag list getter");
        }

        private static Method findValueGetter(Class<?> compoundTagClass, Class<?> returnType, Class<?>... parameterTypes) throws NoSuchMethodException {
            for (Method method : compoundTagClass.getMethods()) {
                if (method.getParameterCount() != parameterTypes.length) {
                    continue;
                }
                Class<?>[] methodParams = method.getParameterTypes();
                boolean matches = true;
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (methodParams[i] != parameterTypes[i]) {
                        matches = false;
                        break;
                    }
                }
                if (!matches) {
                    continue;
                }
                if (method.getReturnType() == returnType || Optional.class.isAssignableFrom(method.getReturnType()) && optionalGenericMatches(method, boxType(returnType))) {
                    method.setAccessible(true);
                    return method;
                }
            }
            throw new NoSuchMethodException("CompoundTag getter for " + returnType.getSimpleName());
        }

        private static Method findNumericGetter(Class<?> compoundTagClass, Class<?> primitiveType, Class<?> fallbackType) throws NoSuchMethodException {
            for (Method method : compoundTagClass.getMethods()) {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class) {
                    if (method.getReturnType() == primitiveType
                            || Optional.class.isAssignableFrom(method.getReturnType()) && optionalGenericMatches(method, boxType(primitiveType))) {
                        method.setAccessible(true);
                        return method;
                    }
                }
                if (method.getParameterCount() == 2 && method.getParameterTypes()[0] == String.class && method.getParameterTypes()[1] == fallbackType) {
                    if (method.getReturnType() == primitiveType) {
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
            throw new NoSuchMethodException("CompoundTag numeric getter for " + primitiveType.getSimpleName());
        }

        private static Method findBooleanGetter(Class<?> compoundTagClass) {
            for (Method method : compoundTagClass.getMethods()) {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class) {
                    if (method.getReturnType() == boolean.class
                            || Optional.class.isAssignableFrom(method.getReturnType()) && optionalGenericMatches(method, Boolean.class)) {
                        method.setAccessible(true);
                        return method;
                    }
                }
                if (method.getParameterCount() == 2 && method.getParameterTypes()[0] == String.class && method.getParameterTypes()[1] == boolean.class) {
                    if (method.getReturnType() == boolean.class) {
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
            return null;
        }

        private static Method findByteGetter(Class<?> compoundTagClass) throws NoSuchMethodException {
            for (Method method : compoundTagClass.getMethods()) {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class) {
                    if (method.getReturnType() == byte.class
                            || Optional.class.isAssignableFrom(method.getReturnType()) && optionalGenericMatches(method, Byte.class)) {
                        method.setAccessible(true);
                        return method;
                    }
                }
                if (method.getParameterCount() == 2 && method.getParameterTypes()[0] == String.class && method.getParameterTypes()[1] == byte.class) {
                    if (method.getReturnType() == byte.class) {
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
            throw new NoSuchMethodException("CompoundTag byte getter");
        }

        private static Method findNumericValueMethod(Class<?> tagClass) throws NoSuchMethodException {
            String[] preferredNames = {"getAsDouble", "doubleValue", "value"};
            for (String name : preferredNames) {
                for (Method method : tagClass.getMethods()) {
                    if (!method.getName().equals(name) || method.getParameterCount() != 0) {
                        continue;
                    }
                    if (method.getReturnType() == double.class
                            || method.getReturnType() == Double.class
                            || Number.class.isAssignableFrom(method.getReturnType())) {
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
            for (Method method : tagClass.getMethods()) {
                if (method.getParameterCount() != 0) {
                    continue;
                }
                if (method.getReturnType() == double.class
                        || method.getReturnType() == Double.class
                        || Number.class.isAssignableFrom(method.getReturnType())) {
                    method.setAccessible(true);
                    return method;
                }
            }
            throw new NoSuchMethodException("NumericTag double getter: " + tagClass.getName());
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

        private static Class<?> boxType(Class<?> type) {
            if (!type.isPrimitive()) {
                return type;
            }
            if (type == int.class) {
                return Integer.class;
            }
            if (type == long.class) {
                return Long.class;
            }
            if (type == float.class) {
                return Float.class;
            }
            if (type == double.class) {
                return Double.class;
            }
            if (type == boolean.class) {
                return Boolean.class;
            }
            if (type == byte.class) {
                return Byte.class;
            }
            return type;
        }
    }
}

package net.celestiald.cavesnotcliffs.world;

import net.minecraft.world.chunk.Chunk;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Optional bridge to integer biome storage supplied by RoughlyEnoughIDs. */
final class ExtendedBiomeStorageCompat {
    private static final String REID_API = "tff.reid.api.BiomeApi";
    private static final Adapter ADAPTER = loadAdapter();

    private ExtendedBiomeStorageCompat() {
    }

    /** Returns whether an extended-biome provider accepted the complete surface plane. */
    static boolean replaceSurfaceBiomes(Chunk chunk, int[] biomeIds) {
        if (chunk == null || biomeIds == null) {
            throw new NullPointerException("chunk and biomeIds are required");
        }
        return ADAPTER.replace(chunk, biomeIds);
    }

    private static Adapter loadAdapter() {
        Class<?> apiClass;
        try {
            apiClass = Class.forName(REID_API, true,
                    ExtendedBiomeStorageCompat.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
            return Adapter.NONE;
        } catch (LinkageError error) {
            throw incompatible(error);
        }

        try {
            Field instanceField = apiClass.getField("INSTANCE");
            Object instance = instanceField.get(null);
            Method replaceBiomes = apiClass.getMethod(
                    "replaceBiomes", Chunk.class, int[].class);
            return new ReidAdapter(instance, replaceBiomes);
        } catch (ReflectiveOperationException | LinkageError error) {
            throw incompatible(error);
        }
    }

    private static IllegalStateException incompatible(Throwable cause) {
        return new IllegalStateException("RoughlyEnoughIDs exposes an incompatible biome API", cause);
    }

    private interface Adapter {
        Adapter NONE = (chunk, biomeIds) -> false;

        boolean replace(Chunk chunk, int[] biomeIds);
    }

    private static final class ReidAdapter implements Adapter {
        private final Object instance;
        private final Method replaceBiomes;

        private ReidAdapter(Object instance, Method replaceBiomes) {
            this.instance = instance;
            this.replaceBiomes = replaceBiomes;
        }

        @Override
        public boolean replace(Chunk chunk, int[] biomeIds) {
            try {
                replaceBiomes.invoke(instance, chunk, biomeIds);
                return true;
            } catch (IllegalAccessException error) {
                throw incompatible(error);
            } catch (InvocationTargetException error) {
                Throwable cause = error.getCause();
                throw incompatible(cause == null ? error : cause);
            }
        }
    }
}

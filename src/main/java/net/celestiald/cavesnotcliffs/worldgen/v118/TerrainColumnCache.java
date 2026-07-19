package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thread-confined weighted access-order LRU for complete generated terrain columns.
 *
 * <p>The loader receives only the requested coordinates. It must derive all output from immutable
 * world-generation state and those coordinates so a column reloaded after eviction is independent
 * of request order. Loader recursion is rejected, as is every access from a thread other than the
 * constructing thread.</p>
 */
public final class TerrainColumnCache {
    public static final int DEFAULT_MAX_COLUMNS = 64;
    public static final long DEFAULT_MAX_WEIGHT_BYTES = 32L * 1024L * 1024L;

    private final Loader loader;
    private final int maxColumns;
    private final long maxWeightBytes;
    private final Thread ownerThread;
    private final LinkedHashMap<ColumnKey, TerrainColumn> columns =
        new LinkedHashMap<ColumnKey, TerrainColumn>(16, 0.75F, true);

    private long currentWeightBytes;
    private boolean loading;
    private int lastColumnX;
    private int lastColumnZ;
    private TerrainColumn lastColumn;

    public TerrainColumnCache(Loader loader) {
        this(loader, DEFAULT_MAX_COLUMNS, DEFAULT_MAX_WEIGHT_BYTES);
    }

    public TerrainColumnCache(Loader loader, int maxColumns, long maxWeightBytes) {
        if (loader == null) {
            throw new NullPointerException("loader");
        }
        if (maxColumns <= 0) {
            throw new IllegalArgumentException("maxColumns must be positive: " + maxColumns);
        }
        if (maxWeightBytes <= 0L) {
            throw new IllegalArgumentException("maxWeightBytes must be positive: "
                + maxWeightBytes);
        }
        this.loader = loader;
        this.maxColumns = maxColumns;
        this.maxWeightBytes = maxWeightBytes;
        ownerThread = Thread.currentThread();
    }

    public TerrainColumn get(int columnX, int columnZ) {
        checkOwnerThread();
        if (loading) {
            throw new IllegalStateException("Reentrant terrain-column generation is not allowed");
        }
        if (lastColumn != null && columnX == lastColumnX && columnZ == lastColumnZ) {
            return lastColumn;
        }

        ColumnKey key = new ColumnKey(columnX, columnZ);
        TerrainColumn cached = columns.get(key);
        if (cached != null) {
            remember(columnX, columnZ, cached);
            return cached;
        }

        TerrainColumn loaded;
        loading = true;
        try {
            loaded = loader.load(columnX, columnZ);
        } finally {
            loading = false;
        }
        if (loaded == null) {
            throw new IllegalStateException("Terrain-column loader returned null for " + key);
        }
        if (loaded.columnX() != columnX || loaded.columnZ() != columnZ) {
            throw new IllegalStateException("Terrain-column loader returned (" + loaded.columnX()
                + ", " + loaded.columnZ() + ") for request " + key);
        }

        if (insert(key, loaded)) {
            remember(columnX, columnZ, loaded);
        }
        return loaded;
    }

    /**
     * Inserts an externally generated column without invoking the loader. Used to hand off
     * asynchronously prestarted columns: the entry must be a pure function of the same immutable
     * state and coordinates the loader would have used. A column already cached at the
     * coordinates is kept and the offer is ignored, so a stale prestart can never replace a
     * fresher entry.
     */
    public void offer(int columnX, int columnZ, TerrainColumn column) {
        checkOwnerThread();
        if (loading) {
            throw new IllegalStateException("Reentrant terrain-column generation is not allowed");
        }
        if (column == null) {
            throw new NullPointerException("column");
        }
        if (column.columnX() != columnX || column.columnZ() != columnZ) {
            throw new IllegalStateException("Offered terrain column (" + column.columnX() + ", "
                + column.columnZ() + ") does not match request (" + columnX + ", " + columnZ
                + ")");
        }
        ColumnKey key = new ColumnKey(columnX, columnZ);
        if (columns.containsKey(key)) {
            return;
        }
        if (insert(key, column)) {
            remember(columnX, columnZ, column);
        }
    }

    public boolean isCached(int columnX, int columnZ) {
        checkOwnerThread();
        // containsKey intentionally does not change access order.
        return columns.containsKey(new ColumnKey(columnX, columnZ));
    }

    public void invalidate(int columnX, int columnZ) {
        checkOwnerThread();
        if (lastColumn != null && columnX == lastColumnX && columnZ == lastColumnZ) {
            lastColumn = null;
        }
        TerrainColumn removed = columns.remove(new ColumnKey(columnX, columnZ));
        if (removed != null) {
            currentWeightBytes -= removed.estimatedRetainedBytes();
        }
    }

    public void clear() {
        checkOwnerThread();
        columns.clear();
        currentWeightBytes = 0L;
        lastColumn = null;
    }

    public int size() {
        checkOwnerThread();
        return columns.size();
    }

    public long currentWeightBytes() {
        checkOwnerThread();
        return currentWeightBytes;
    }

    public int maxColumns() {
        checkOwnerThread();
        return maxColumns;
    }

    public long maxWeightBytes() {
        checkOwnerThread();
        return maxWeightBytes;
    }

    /**
     * Validates the column weight and inserts it under the dual limits, evicting by access order.
     * Returns false without caching when the column alone exceeds the byte limit.
     */
    private boolean insert(ColumnKey key, TerrainColumn column) {
        long weight = column.estimatedRetainedBytes();
        if (weight <= 0L) {
            throw new IllegalStateException("Terrain column has invalid cache weight: " + weight);
        }
        if (weight > maxWeightBytes) {
            return false;
        }

        // Evict before addition and compare using subtraction, avoiding overflow even when the
        // configured byte limit is Long.MAX_VALUE.
        while (!columns.isEmpty()
                && (columns.size() >= maxColumns
                    || currentWeightBytes > maxWeightBytes - weight)) {
            evictEldest();
        }
        columns.put(key, column);
        currentWeightBytes += weight;
        return true;
    }

    private void evictEldest() {
        Iterator<Map.Entry<ColumnKey, TerrainColumn>> iterator = columns.entrySet().iterator();
        Map.Entry<ColumnKey, TerrainColumn> eldest = iterator.next();
        currentWeightBytes -= eldest.getValue().estimatedRetainedBytes();
        iterator.remove();
    }

    private void remember(int columnX, int columnZ, TerrainColumn column) {
        lastColumnX = columnX;
        lastColumnZ = columnZ;
        lastColumn = column;
    }

    private void checkOwnerThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException("TerrainColumnCache is confined to thread '"
                + ownerThread.getName() + "' but was accessed from '"
                + Thread.currentThread().getName() + "'");
        }
    }

    public interface Loader {
        TerrainColumn load(int columnX, int columnZ);
    }

    private static final class ColumnKey {
        private final int x;
        private final int z;

        private ColumnKey(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof ColumnKey)) {
                return false;
            }
            ColumnKey other = (ColumnKey) object;
            return x == other.x && z == other.z;
        }

        @Override
        public int hashCode() {
            return 31 * x + z;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + z + ")";
        }
    }
}

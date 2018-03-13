package uk.org.wolfpuppy.minecraft.chunkchecker.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class LimitedMap<K, V> extends LinkedHashMap<K, V> {
    private int maxSize;

    public LimitedMap(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
        return size() > maxSize;
    }
}

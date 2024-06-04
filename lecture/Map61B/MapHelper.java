package Map61B;

import org.junit.Test;
import java.util.Set;

public class MapHelper {
    public static <K, V> void put(Map61B<K, V> map, K key, V value) {
        map.put(key, value);
    }

    public static <K, V> V get(Map61B<K, V> map, K key) {
        return map.get(key);
    }

    public static <K, V> boolean containsKey(Map61B<K, V> map, K key) {
        return map.containsKey(key);
    }

    public static <K, V> int size(Map61B<K, V> map) {
        return map.size();
    }

    public static <K> Set<K> keySet(Map61B<K, ?> map) {
        return map.keySet();
    }

    public static <K extends Comparable<K>, V> K getMaxKey(Map61B<K, V> map) {
        Set<K> keySet = keySet(map);
        K maxKey = null;
        for (K key : keySet) {
            if (maxKey == null || key.compareTo(maxKey) > 0) {
                maxKey = key;
            }
        }
        return maxKey;
    }

    @Test
    public void testGet() {
        Map61B<String, Integer> map = new ArrayMap<String, Integer>();
        put(map, "a", 1);
        put(map, "b", 2);
        put(map, "c", 3);
        put(map, "d", 4);
        put(map, "e", 5);
        assert get(map, "a") == 1;
        assert get(map, "b") == 2;
        assert get(map, "c") == 3;
        assert get(map, "d") == 4;
        assert get(map, "e") == 5;
    }

    @Test
    public void testContainsKey() {
        Map61B<String, Integer> map = new ArrayMap<String, Integer>();
        put(map, "a", 1);
        put(map, "b", 2);
        put(map, "c", 3);
        put(map, "d", 4);
        put(map, "e", 5);
        assert containsKey(map, "a");
        assert containsKey(map, "b");
        assert containsKey(map, "c");
        assert containsKey(map, "d");
        assert containsKey(map, "e");
    }

    @Test
    public void test() {
        Map61B<String, Integer> map = new ArrayMap<String, Integer>();
        put(map, "a", 1);
        put(map, "b", 2);
        put(map, "c", 3);
        put(map, "d", 4);
        put(map, "e", 5);
        assert size(map) == 5;
        assert getMaxKey(map).equals("e");
    }
}

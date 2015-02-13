package sim;

import java.util.Map;

/**
 * @author Roman Elizarov
 */
public class DistUtil {
    public static final int INF = Integer.MAX_VALUE;

    public static int get(Map<NID, Integer> map, NID k) {
        Integer val = map.get(k);
        return val == null ? INF : val;
    }

    public static void put(Map<NID, Integer> map, NID k, int d) {
        if (d == INF)
            map.remove(k);
        else
            map.put(k, d);
    }

    public static String d2s(int d) {
        return d == INF ? "INF" : "" + d;
    }

    public static int sumDist(int a, int b) {
        int c = a + b;
        return c < 0 ? INF : c;
    }
}

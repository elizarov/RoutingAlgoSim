package sim;

import java.util.Locale;

/**
 * @author Roman Elizarov
 */
public class AlgoFactory {
    public static AbstractAlgo createAlgo(String name) {
        try {
            Class<?> z = Class.forName("sim." + name.toLowerCase(Locale.US) + "." + name + "Algo");
            return (AbstractAlgo) z.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}

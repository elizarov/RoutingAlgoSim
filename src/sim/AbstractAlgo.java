package sim;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Elizarov
 */
public abstract class AbstractAlgo<M extends AbstractMsg, N extends AbstractNode<M>> {
    public abstract List<String> getAlgoDescription();

    public abstract N newNode(NID i);

    public Map<Integer,String> getLinkTypeLegend() {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(0, "Normal link");
        map.put(AbstractNode.LINK_BOLD + AbstractNode.LINK_ROUTE, "Route link");
        return map;
    }
}

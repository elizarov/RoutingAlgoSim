package sim.mdvam;

import sim.AbstractAlgo;
import sim.AbstractNode;
import sim.NID;
import sim.mdva.MDVAMsg;
import sim.mdva.MDVANode;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Elizarov
 */
public class MDVAmAlgo extends AbstractAlgo<MDVAmMsg, MDVAmNode> {
    @Override
    public List<String> getAlgoDescription() {
        return Arrays.asList(
            "MDVA: A Distance-Vector Multi-path Routing Protocol",
            "Algorithm from the paper is adapted to support one-way links",
            "and to maintain old (possibly cyclic) route even when MDVA DAG updates"
        );
    }

    @Override
    public MDVAmNode newNode(NID i) {
        return new MDVAmNode(i);
    }

    @Override
    public Map<Integer,String> getLinkTypeLegend() {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(0, "Normal link");
        map.put(AbstractNode.LINK_BOLD, "Link in MDVA DAG");
        map.put(AbstractNode.LINK_ROUTE, "Routing uses link");
        return map;
    }
}

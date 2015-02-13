package sim.mdva;

import sim.AbstractAlgo;
import sim.AbstractNode;
import sim.NID;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Elizarov
 */
public class MDVAAlgo extends AbstractAlgo<MDVAMsg, MDVANode> {
    @Override
    public List<String> getAlgoDescription() {
        return Arrays.asList(
            "MDVA: A Distance-Vector Multi-path Routing Protocol",
            "Algorithm from the paper is adapted to support one-way links"
        );
    }

    @Override
    public MDVANode newNode(NID i) {
        return new MDVANode(i);
    }
}

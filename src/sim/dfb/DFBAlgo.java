package sim.dfb;

import sim.AbstractAlgo;
import sim.NID;

import java.util.Arrays;
import java.util.List;

/**
 * Distributed Ford-Bellman algorithm: factory class.
 *
 * @author Roman Elizarov
 */
public class DFBAlgo extends AbstractAlgo<DFBMsg, DFBNode> {
    @Override
    public List<String> getAlgoDescription() {
        return Arrays.asList(
            "Distributed Ford-Bellman Algorithm",
            "Count-to-infinity is avoided by limiting distance to " + DFBNode.MAX_DIST
        );
    }

    @Override
    public DFBNode newNode(NID i) {
        return new DFBNode(i);
    }
}

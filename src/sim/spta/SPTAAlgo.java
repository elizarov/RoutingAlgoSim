package sim.spta;

import sim.AbstractAlgo;
import sim.AbstractMsg;
import sim.NID;
import sim.dfb.DFBMsg;
import sim.dfb.DFBNode;

import java.util.Arrays;
import java.util.List;

/**
 * Distributed Ford-Bellman algorithm: factory class.
 *
 * @author Roman Elizarov
 */
public class SPTAAlgo extends AbstractAlgo<AbstractMsg, SPTANode> {
    @Override
    public List<String> getAlgoDescription() {
        return Arrays.asList(
            "SPTA: Shortest Path Topology Algorithm",
            "from \"Broadcasting Topology Information in Computer Networks\""
        );
    }

    @Override
    public SPTANode newNode(NID i) {
        return new SPTANode(i);
    }
}

package sim.dpva;

import sim.AbstractAlgo;
import sim.NID;

import java.util.Arrays;
import java.util.List;

/**
 * Distance + Path Vector Algorithm: factory class.
 *
 * @author Roman Elizarov
 */
public class DPVAAlgo extends AbstractAlgo<DPVAMsg, DPVANode> {
    @Override
    public List<String> getAlgoDescription() {
        return Arrays.asList(
            "Distance + Path Vector Algorithm",
            "DFB distance + set of intermediate nodes"
        );
    }

    @Override
    public DPVANode newNode(NID i) {
        return new DPVANode(i);
    }
}

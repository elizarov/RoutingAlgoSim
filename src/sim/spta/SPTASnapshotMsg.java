package sim.spta;

import sim.AbstractMsg;
import sim.NID;

import java.util.*;

/**
 * @author Roman Elizarov
 */
public class SPTASnapshotMsg extends AbstractMsg {
    final Map<NID, Map<NID, Integer>> t;

    public SPTASnapshotMsg(NID from, NID to, Map<NID, Map<NID, Integer>> t) {
        super(from, to);
        this.t = SPTANode.deepCopyT(t);
    }

    @Override
    public String getDescription() {
        return "SNAPSHOT " + t;
    }
}

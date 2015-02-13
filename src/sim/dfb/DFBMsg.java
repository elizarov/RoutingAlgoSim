package sim.dfb;

import sim.AbstractMsg;
import sim.NID;
import sim.DistUtil;

/**
 * Distributed Ford-Bellman algorithm: message class.
 *
 * @author Roman Elizarov
 */
public class DFBMsg extends AbstractMsg {
    final int d; // distance

    public DFBMsg(NID from, NID to, int d) {
        super(from, to);
        this.d = d;
    }

    @Override
    public String getDescription() {
        return "UPDATE d=" + DistUtil.d2s(d);
    }
}

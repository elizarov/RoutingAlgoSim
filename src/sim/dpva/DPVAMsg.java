package sim.dpva;

import sim.AbstractMsg;
import sim.DistUtil;
import sim.NID;

import java.util.Set;

/**
 * Distance + Path Vector Algorithm: message class.
 *
 * @author Roman Elizarov
 */
public class DPVAMsg extends AbstractMsg {
    final int d; // distance
    final Set<NID> r; // route set -- intermediate nodes on a path

    public DPVAMsg(NID from, NID to, int d, Set<NID> r) {
        super(from, to);
        this.d = d;
        this.r = r;
    }

    @Override
    public String getDescription() {
        return "UPDATE d=" + DistUtil.d2s(d) + " r=" + r;
    }
}

package sim.mdvam;

import sim.AbstractMsg;
import sim.NID;
import sim.DistUtil;

/**
 * @author Roman Elizarov
 */
public class MDVAmMsg extends AbstractMsg {
    final MDVAmMsgType et; // message type
    final int d; // distance

    public MDVAmMsg(MDVAmMsgType et, NID from, NID to, int d) {
        super(from, to);
        this.et = et;
        this.d = d;
    }

    @Override
    public String getDescription() {
        return et + " d=" + DistUtil.d2s(d);
    }
}

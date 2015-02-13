package sim.mdva;

import sim.AbstractMsg;
import sim.NID;
import sim.DistUtil;

/**
 * @author Roman Elizarov
 */
public class MDVAMsg extends AbstractMsg {
    final MDVAMsgType et; // message type
    final int d; // distance

    public MDVAMsg(MDVAMsgType et, NID from, NID to, int d) {
        super(from, to);
        this.et = et;
        this.d = d;
    }

    @Override
    public String getDescription() {
        return et + " d=" + DistUtil.d2s(d);
    }
}

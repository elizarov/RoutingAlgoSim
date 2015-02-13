package sim.spta;

import sim.AbstractMsg;
import sim.DistUtil;
import sim.NID;

import java.util.List;

/**
 * @author Roman Elizarov
 */
public class SPTAUpdateMsg extends AbstractMsg {

    static class Link {
        final NID from;
        final NID to;
        final int d;

        public Link(NID from, NID to, int d) {
            this.from = from;
            this.to = to;
            this.d = d;
        }

        @Override
        public String toString() {
            return from + "->" + to + " d=" + DistUtil.d2s(d);
        }
    }

    final List<Link> links;

    public SPTAUpdateMsg(NID from, NID to, List<Link> links) {
        super(from, to);
        this.links = links;
    }

    @Override
    public String getDescription() {
        return "UPDATE " + links;
    }
}

package sim.dfb;

import sim.AbstractNode;
import sim.NID;
import sim.DistUtil;

import java.util.*;

/**
 * Distributed Ford-Bellman algorithm: logic at the node.
 *
 * @author Roman Elizarov
 */
public class DFBNode extends AbstractNode<DFBMsg> {
    public static final int MAX_DIST = 2000; // truncate to INF over this dist

    // --- algorithm state ---

    private Map<NID,Integer> dn = new HashMap<>(); // distance as reported by each neighbour (subset of ln nodes)
    private int rd; // last reported distance to neighbours

    public DFBNode(NID i) {
        super(i);
        rd = i.equals(NID.DEST) ? 0 : DistUtil.INF;
    }

    private int bestDist() {
        int best = bestDistOverSetViaMap(ln.keySet(), dn);
        return best > MAX_DIST ? DistUtil.INF : best;
    }

    @Override
    public List<DFBMsg> process(DFBMsg msg) {
        assert msg.to.equals(i);
        return process(msg.from, msg.d);
    }

    public List<DFBMsg> process(NID from, int d) {
        DistUtil.put(dn, from, d);
        return updates();
    }

    public List<DFBMsg> updates() {
        int best = bestDist();
        if (best == rd)
            return Collections.emptyList();
        List<DFBMsg> send = new ArrayList<>();
        for (NID j : in)
            send.add(new DFBMsg(i, j, best));
        rd = best;
        return send;
    }

    @Override
    public List<DFBMsg> updateOutgoingLink(NID m, int d) {
        DistUtil.put(ln, m, d);
        return updates();
    }

    @Override
    public List<DFBMsg> updateIncomingLink(NID m) {
        if (in.add(m) && rd != DistUtil.INF)
            return Collections.singletonList(new DFBMsg(i, m, rd));
        else
            return Collections.emptyList();
    }

    @Override
    public List<DFBMsg> removeLink(NID m) {
        ln.remove(m);
        in.remove(m);
        dn.remove(m);
        return updates();
    }

    @Override
    public int getLinkFlags(NID m) {
        int best = bestDist();
        return best != DistUtil.INF && best == distViaMap(m, dn) ? LINK_ROUTE + LINK_BOLD : 0;
    }

    @Override
    public String toString() {
        return "Node " + i +
                ": d=" + DistUtil.d2s(bestDist()) +
                " dn=" + dn +
                " ln=" + ln;
    }

    @Override
    public void addNodeDataTo(List<String> nodeStr) {
        nodeStr.add(i + "");
        nodeStr.add("d=" + DistUtil.d2s(bestDist()));
        nodeStr.add("dn=" + dn);
    }

    @Override
    public String verifyQuiescentDistance(int td) {
        int best = bestDist();
        if (best != td)
            return "Node " + i + " current distance " + DistUtil.d2s(best) + " != " + DistUtil.d2s(td) + " of true distance";
        return null;
    }
}

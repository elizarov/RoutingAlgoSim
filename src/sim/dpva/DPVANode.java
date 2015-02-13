package sim.dpva;

import sim.AbstractNode;
import sim.DistUtil;
import sim.NID;
import sim.dfb.DFBMsg;

import java.util.*;

/**
 * Distance + Path Vector Algorithm: logic at the node.
 *
 * @author Roman Elizarov
 */
public class DPVANode extends AbstractNode<DPVAMsg> {
    public static final int MAX_DIST = 2000; // truncate to INF over this dist

    // --- algorithm state ---

    private Map<NID,Integer> dn = new HashMap<>(); // distance as reported by each neighbour (subset of ln nodes)
    private Map<NID,Set<NID>> rn = new HashMap<>(); // route set as reported by each neighbour (subset of ln nodes)
    private Set<NID> rs = new HashSet<>();  // a set of neighbours that we sent non-INF (rd,rr) pair to (as opposed to sending them INF)

    private int rd;       // last reported distance to neighbours
    private Set<NID> rr;  // last reported route set to neighbours

    public DPVANode(NID i) {
        super(i);
        rd = i.equals(NID.DEST) ? 0 : DistUtil.INF;
        rr = Collections.emptySet(); // initially empty
    }

    private int bestDist() {
        int best = bestDistOverSetViaMap(ln.keySet(), dn);
        return best > MAX_DIST ? DistUtil.INF : best;
    }

    private Set<NID> bestRoute(int best) {
        if (best == DistUtil.INF)
            return Collections.emptySet();
        Set<NID> r = new HashSet<>();
        for (NID m : ln.keySet()) {
            if (distViaMap(m, dn) <= best) {
                // we don't need to keep final destination in a route path set
                if (!m.equals(NID.DEST))
                    r.add(m);
                Set<NID> mr = rn.get(m);
                if (mr != null)
                    r.addAll(mr);
            }
        }
        return r;
    }

    @Override
    public List<DPVAMsg> process(DPVAMsg msg) {
        assert msg.to.equals(i);
        return process(msg.from, msg.d, msg.r);
    }

    public List<DPVAMsg> process(NID from, int d, Set<NID> r) {
        DistUtil.put(dn, from, d);
        if (r.isEmpty())
            rn.remove(from);
        else
            rn.put(from, r);
        return updates();
    }

    public List<DPVAMsg> updates() {
        int best = bestDist();
        Set<NID> bestRoute = bestRoute(best);
        if (best == rd && bestRoute.equals(rr))
            return Collections.emptyList(); // nothing changes -- don't send any updates
        List<DPVAMsg> send = new ArrayList<>();
        for (NID j : in) {
            if (j.equals(NID.DEST))
                continue; // never need to send updates to the destination node
            // loop detection
            if (bestRoute.contains(j) || best == DistUtil.INF) {
                // loop detected or best route is INF.
                // Now send INF to neighbour only if previously sent non-INF to this node before
                if (rs.remove(j))
                    send.add(new DPVAMsg(i, j, DistUtil.INF, Collections.emptySet()));
            } else {
                // ok path to destination
                rs.add(j); // remember that we had sent non-INF path to this neighbour
                send.add(new DPVAMsg(i, j, best, bestRoute));
            }
        }
        rd = best;
        rr = bestRoute;
        return send;
    }

    @Override
    public List<DPVAMsg> updateOutgoingLink(NID m, int d) {
        DistUtil.put(ln, m, d);
        return updates();
    }

    @Override
    public List<DPVAMsg> updateIncomingLink(NID m) {
        // send updates on the incoming link if our known distance is non-INF
        if (in.add(m) && rd != DistUtil.INF) {
            rs.add(m);
            return Collections.singletonList(new DPVAMsg(i, m, rd, rr));
        } else
            return Collections.emptyList();
    }

    @Override
    public List<DPVAMsg> removeLink(NID m) {
        ln.remove(m);
        in.remove(m);
        dn.remove(m);
        rn.remove(m);
        rs.remove(m);
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
                ": d=" + DistUtil.d2s(bestDist()) + " r=" + bestRoute(bestDist()) +
                " dn=" + dn + " rn=" + rn +
                " ln=" + ln;
    }

    @Override
    public void addNodeDataTo(List<String> nodeStr) {
        nodeStr.add(i + "");
        nodeStr.add("d=" + DistUtil.d2s(bestDist()) + " r=" + bestRoute(bestDist()));
        nodeStr.add("dn=" + dn + " rn=" + rn);
    }

    @Override
    public String verifyQuiescentDistance(int td) {
        int best = bestDist();
        if (best != td)
            return "Node " + i + " current distance " + DistUtil.d2s(best) + " != " + DistUtil.d2s(td) + " of true distance";
        return null;
    }
}

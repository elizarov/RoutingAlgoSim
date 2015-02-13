package sim;

import java.util.*;

/**
 * @author Roman Elizarov
 */
public abstract class AbstractNode<M extends AbstractMsg> {

    public static int LINK_BOLD = 1;
    public static int LINK_ROUTE = 2;

    protected final NID i; // This node name

    // --- link state ---

    protected Set<NID> in = new LinkedHashSet<>(); // set of all incoming links
    protected Map<NID,Integer> ln = new HashMap<>(); // outgoing link distance to each neighbour k (l^i_k)

    // --- methods ---

    public AbstractNode(NID i) {
        this.i = i;
    }

    public NID getId() {
        return i;
    }

    public int getOutgoingLink(NID to) {
        return DistUtil.get(ln, to);
    }

    public Map<NID, Integer> getOutgoingLinks() {
        return ln;
    }

    public Set<NID> getIncomingLinks() {
        return in;
    }

    // --- abstract methods ---

    public abstract List<M> process(M msg);

    public abstract List<M> updateOutgoingLink(NID m, int d);

    public abstract List<M> updateIncomingLink(NID m);

    // remove node link (both incoming and outgoing)
    public abstract List<M> removeLink(NID m);

    public abstract int getLinkFlags(NID m);

    public abstract void addNodeDataTo(List<String> nodeStr);

    public abstract String verifyQuiescentDistance(int td);

    // ---------- utility methods ----------

    public int bestDistOverSetViaMap(Set<NID> set, Map<NID, Integer> map) {
        if (NID.DEST.equals(i))
            return 0; // we are the DEST and best distance is always zero
        int best = DistUtil.INF;
        for (NID k : set)
            best = Math.min(best, distViaMap(k, map));
        return best;
    }

    public int distViaMap(NID k, Map<NID, Integer> map) {
        return DistUtil.sumDist(DistUtil.get(map, k), DistUtil.get(ln, k));
    }

}

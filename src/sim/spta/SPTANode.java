package sim.spta;

import sim.AbstractMsg;
import sim.AbstractNode;
import sim.NID;
import sim.DistUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SPTA: Shortest Path Topology Algorithm from
 * "Broadcasting Topology Information in Computer Networks".
 *
 * @author Roman Elizarov
 */
public class SPTANode extends AbstractNode<AbstractMsg> {
    // --- algorithm state ---

    private Map<NID, Map<NID, Map<NID, Integer>>> tn = new HashMap<>(); // topology as reported by each neighbour (subset of ln nodes), tn[i][i] == ln always
    private Map<NID, Map<NID, Integer>> t = new TreeMap<>(); // main topology table
    private Map<NID, Map<NID, Integer>> rt = new TreeMap<>(); // recently sent topology table

    private int d; // best known distance
    private Set<NID> s = new HashSet<>(); // successor set for routing

    public SPTANode(NID i) {
        super(i);
        d = i.equals(NID.DEST) ? 0 : DistUtil.INF;
        tn.put(i, Collections.singletonMap(i, ln));
    }

    public static Map<NID, Map<NID, Integer>> deepCopyT(Map<NID, Map<NID, Integer>> t) {
        Map<NID, Map<NID, Integer>> tt = new TreeMap<>();
        t.forEach((a, map) -> tt.put(a, new TreeMap<>(map)));
        return tt;
    }

    public static int getTD(Map<NID, Map<NID, Integer>> t, NID from, NID to) {
        Map<NID, Integer> map = t.get(from);
        return map == null ? DistUtil.INF : DistUtil.get(map, to);
    }

    public static void putTD(Map<NID, Map<NID, Integer>> t, NID from, NID to, int d) {
        Map<NID, Integer> map = t.get(from);
        if (d < DistUtil.INF) {
            if (map == null)
                t.put(from, map = new TreeMap<>());
            map.put(to, d);
        } else {
            if (map != null) {
                map.remove(to);
                if (map.isEmpty())
                    t.remove(from);
            }
        }
    }

    @Override
    public List<AbstractMsg> process(AbstractMsg msg) {
        assert msg.to.equals(i);
        if (msg instanceof SPTASnapshotMsg) {
            SPTASnapshotMsg snapshot = (SPTASnapshotMsg) msg;
            tn.put(msg.from, snapshot.t);
        } else if (msg instanceof SPTAUpdateMsg) {
            SPTAUpdateMsg update = (SPTAUpdateMsg) msg;
            update.links.forEach(link -> putTD(tn.get(msg.from), link.from, link.to, link.d));
        }
        return updates();
    }

    private List<AbstractMsg> updates() {
        // rebuild main topology table by running Djikstra from us to all other reachable nodes
        // running Shortest Path Topology Algorithm
        t.clear();
        Set<NID> queue = new TreeSet<>(); // prioritize statically
        Map<NID, Integer> dist = new HashMap<>();
        Map<NID, NID> cn = new HashMap<>(); // closest neighbour (to use distance info from)
        queue.add(i);
        dist.put(i, 0);
        cn.put(i, i);
        while (!queue.isEmpty()) {
            NID i = bestDistInQueue(queue, dist);
            queue.remove(i);
            int di = dist.get(i);
            NID cni = cn.get(i);
            Map<NID, Map<NID, Integer>> ct = tn.get(cni); // correct topology (from closest neighbour)
            if (ct == null)
                continue;
            Map<NID, Integer> map = ct.get(i); // outgoing link map to use
            if (map == null)
                continue;
            map.forEach((j, d) -> {
                int oldD = DistUtil.get(dist, j);
                int newD = di + d;
                if (newD < oldD) {
                    queue.add(j);
                    dist.put(j, newD);
                    if (this.i.equals(i)) {
                        // first expansion from initial node -- initial node's neighbours
                        cn.put(j, j);
                    } else {
                        cn.put(j, cni);
                    }
                }
                putTD(t, i, j, d);
            });
        }
        // now run Djikstra again over computed topology t and compute successor set
        dist.clear();
        dist.put(NID.DEST, 0);
        queue.add(NID.DEST);
        while (!queue.isEmpty()) {
            NID i = bestDistInQueue(queue, dist);
            queue.remove(i);
            int di = dist.get(i);
            t.forEach((j, map) -> {
                int d = DistUtil.get(map, i);
                if (d != DistUtil.INF) {
                    int newD = di + d;
                    int oldD = DistUtil.get(dist, j);
                    if (newD < oldD) {
                        queue.add(j);
                        DistUtil.put(dist, j, newD);
                    }
                }
            });
        }
        d = DistUtil.get(dist, i); // distance to destination is computed
        s.clear(); // recompute successor set below
        if (d != DistUtil.INF)
            ln.forEach((j, jd) -> {
                Integer dj = DistUtil.get(dist, j);
                if (dj == d - jd)
                    s.add(j);
            });
        // compare with last reported and compute delta to send updates
        List<SPTAUpdateMsg.Link> changes = new ArrayList<>();
        rt.forEach((a, map) -> map.forEach((b, oldD) -> {
            int newD = getTD(t, a, b);
            if (oldD != newD)
                changes.add(new SPTAUpdateMsg.Link(a, b, newD));
        }));
        t.forEach((a, map) -> map.forEach((b, newD) -> {
            if (getTD(rt, a, b) == DistUtil.INF)
                changes.add(new SPTAUpdateMsg.Link(a, b, newD));
        }));
        rt = deepCopyT(t);
        // construct updates relevant to each node
        List<AbstractMsg> send = new ArrayList<>();
        for (NID to : in) {
            ArrayList<SPTAUpdateMsg.Link> links = changes.stream()
                    .filter(link -> !link.from.equals(to))
                    .collect(Collectors.toCollection(ArrayList::new));
            if (!links.isEmpty())
                send.add(new SPTAUpdateMsg(i, to, links));
        }
        return send;
    }

    private NID bestDistInQueue(Set<NID> queue, Map<NID, Integer> dist) {
        int best = DistUtil.INF;
        NID bestI = null;
        for (NID nid : queue) {
            if (dist.get(nid) < best) {
                best = dist.get(nid);
                bestI = nid;
            }
        }
        return bestI;
    }

    @Override
    public List<AbstractMsg> updateOutgoingLink(NID m, int d) {
        DistUtil.put(ln, m, d);
        return updates();
    }

    @Override
    public List<AbstractMsg> updateIncomingLink(NID m) {
        if (in.add(m))
            return Collections.singletonList(new SPTASnapshotMsg(i, m, t));
        else
            return Collections.emptyList();
    }

    @Override
    public List<AbstractMsg> removeLink(NID m) {
        ln.remove(m);
        in.remove(m);
        tn.remove(m);
        return updates();
    }

    @Override
    public int getLinkFlags(NID m) {
        return s.contains(m) ? LINK_ROUTE + LINK_BOLD : 0;
    }

    @Override
    public String toString() {
        return "Node " + i +
                ": d=" + DistUtil.d2s(d) +
                " t=" + t;
    }

    @Override
    public void addNodeDataTo(List<String> nodeStr) {
        nodeStr.add(i + "");
        nodeStr.add("d=" + DistUtil.d2s(d));
        nodeStr.add("t=" + t);
    }

    @Override
    public String verifyQuiescentDistance(int td) {
        if (d != td)
            return "Node " + i + " current distance " + DistUtil.d2s(d) + " != " + DistUtil.d2s(td) + " of true distance";
        return null;
    }
}

package sim;

import java.util.*;

/**
 * @author Roman Elizarov
 */
public class NetworkModel<M extends AbstractMsg, N extends AbstractNode<M>> {
    private final Map<NID, N> nodes = new TreeMap<>();
    private final List<M> msgs = new ArrayList<>();

    private final boolean log;
    private AbstractAlgo<M, N> algo;

    public static <M extends AbstractMsg, N extends AbstractNode<M>> NetworkModel<M, N> createNetworkModel(boolean log, AbstractAlgo<M, N> algo) {
        return new NetworkModel<>(log, algo);
    }

    private NetworkModel(boolean log, AbstractAlgo<M, N> algo) {
        this.log = log;
        this.algo = algo;
    }

    public AbstractAlgo getAlgo() {
        return algo;
    }

    public Set<NID> getNIDs() {
        return nodes.keySet();
    }

    public Collection<N> getNodes() {
        return nodes.values();
    }

    public N getNode(NID i) {
        N data = nodes.get(i);
        if (data == null)
            nodes.put(i, data = algo.newNode(i));
        return data;
    }

    public void removeNode(NID i) {
        removeNodeLinks(i);
        nodes.remove(i);
    }

    public void removeNodeLinks(NID i) {
        N node = getNode(i);
        Set<NID> links = new HashSet<>(node.getIncomingLinks());
        links.addAll(node.getOutgoingLinks().keySet());
        for (NID j : links)
            removeLink(i, j);
    }

    public int getLink(NID from, NID to) {
        return getNode(from).getOutgoingLink(to);
    }

    public void updateLink(NID from, NID to, int d) {
        assert d > 0;
        if (d == DistUtil.INF) {
            removeLink(from, to);
            return;
        }
        if (log)
            System.out.println("Updating link " + from + "->" + to + " d=" + DistUtil.d2s(d));
        send(getNode(from).updateOutgoingLink(to, d));
        send(getNode(to).updateIncomingLink(from));
    }

    // NOTE: Link is always removed in both directions (does not work otherwise)
    public void removeLink(NID from, NID to) {
        if (log)
            System.out.println("Removing link " + from + "<->" + to );
        send(getNode(from).removeLink(to));
        send(getNode(to).removeLink(from));
        // Drop pending messages over link
        for (Iterator<M> it = msgs.iterator(); it.hasNext();) {
            M msg = it.next();
            if (msg.isOverLink(from, to) || msg.isOverLink(to, from))
                it.remove(); // drop pending messages across dropped link
        }
        // recompute first over link
        recomputeFirstOverLink(from, to);
        recomputeFirstOverLink(to, from);
    }

    private void recomputeFirstOverLink(NID from, NID to) {
        boolean first = true;
        for (M msg : msgs) {
            if (msg.isOverLink(from, to)) {
                msg.firstOverLink = first;
                first = false;
            }
        }
    }

    public List<M> getMsgs() {
        return msgs;
    }

    public void processMessage(int i) {
        M msg = msgs.remove(i);
        assert msg.firstOverLink : "Cannot process non-first message " + msg;
        for (M o : msgs) {
            if (msg.isSameLink(o)) {
                o.firstOverLink = true;
                break;
            }
        }
        if (log)
            System.out.println("Processing message " + msg);
        send(getNode(msg.to).process(msg));
    }

    public void clear() {
        nodes.clear();
        msgs.clear();
    }

    private void send(List<M> send) {
        for (M msg : send) {
            msg.firstOverLink = true;
            for (M o : msgs) {
                if (msg.isSameLink(o)) {
                    msg.firstOverLink = false;
                    break;
                }
            }
            if (log)
                System.out.println("Sending message " + msg);
            msgs.add(msg);
        }
    }

    public String verifyInQuiescentState() {
        assert msgs.isEmpty() : "Must be called in quiescent state only";
        // now compute Dijkstra distances
        Map<NID,Integer> dist = new HashMap<>();
        Set<NID> queue = new HashSet<>();
        dist.put(NID.DEST, 0);
        queue.add(NID.DEST);
        while (!queue.isEmpty()) {
            NID cur = null;
            int best = DistUtil.INF;
            for (NID nid : queue) {
                if (dist.get(nid) < best) {
                    cur = nid;
                    best = dist.get(nid);
                }
            }
            queue.remove(cur);
            for (NID i : nodes.get(cur).getIncomingLinks()) {
                int d = nodes.get(i).getOutgoingLink(cur);
                if (!dist.containsKey(i) || best + d < dist.get(i)) {
                    dist.put(i, best + d);
                    queue.add(i);
                }
            }
        }
        // now verify distances in nodes
        for (N node : nodes.values()) {
            NID i = node.getId();
            int td = dist.containsKey(i) ? dist.get(i) : DistUtil.INF; // true Dijkstra distances
            String text = node.verifyQuiescentDistance(td);
            if (text != null)
                return text;
        }
        return null; // Ok
    }
}

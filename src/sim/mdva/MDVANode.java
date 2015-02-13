package sim.mdva;

import sim.AbstractNode;
import sim.NID;
import sim.DistUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Data that each node i stores for destination j in MDVA algorithm.
 * See "MDVA: A Distance-Vector Multi-path Routing Protocol" by Srinivas Vutukury, J.J. Garcia-Luna-Aceves.
 *
 * <p>The algorithm is modified for directed link graph. Links can change distance in each direction separately,
 * however when link it dropped, it must be dropped in both directions at the same time.
 *
 * <p>An additional feature of this implementation is that in addition to MDVA it maintains a routing graph that
 * does not temporarily breakdown on link disappearance (as does MDVA successor DAG), but choose to retain
 * routing information from last known distances. The trade off is that this routing graph can have
 * temporary routing loops.
 *
 * @author Roman Elizarov
 */
public class MDVANode extends AbstractNode<MDVAMsg> {

    // --- algorithm state ---

    private int fd; // feasible distance (FD^i_j)
    private int rd; // reported distance (RD^i_j)
    private Set<NID> wn = new LinkedHashSet<>(); // waiting neighbours in diffusing computation (WN^i_j) (subset of of ln nodes)
    private Map<NID,Integer> dn = new HashMap<>(); // distance as reported by each neighbour k (D^i_{jk}) (subset of ln nodes)

    // diffusing update is active (state == ACTIVE) if and only if !r.isEmpty()
    private Set<NID> r = new LinkedHashSet<>(); // waiting for replies from (subset of of in nodes)

    // The set of all neighbours (N) is a ln.keySet() union with in

    /*
     * LOOP-FREE INVARIANT CONDITIONS FOR EACH NODE i
     *      fd(i) <= dn(k)[i] for all k in set of neighbours               (1)
     *      s(i) = { k | d(i)[k] < fd(i) }                                 (2)
     */

    /*
     * Different from paper is in directed links
     *           ---------------> [ i ] ----------------------->  ... path to DEST
     *            in (incoming)         ln.keySet (outgoing)
     *
     *           UPDATE/QUERY messages are sent over incoming links ( <-- )   in reverse link direction
     *           REPLY messages are sent over outgoing links        ( --> )   in forward link direction
     */

    public MDVANode(NID i) {
        super(i);
        int d0 = NID.DEST.equals(i) ? 0 : DistUtil.INF;
        fd = d0;
        rd = d0;
    }

    public boolean isActive() {
        boolean active = !r.isEmpty();
        assert active || (fd == rd && fd == bestMDVADist(successorSet()));
        return active;
    }

    @Override
    public List<MDVAMsg> process(MDVAMsg msg) {
        assert msg.to.equals(i);
        return process(msg.et, msg.from, msg.d);
    }

    /**
     * Process incoming message per MDVA algorithm.
     *
     * @param et message type.
     * @param m neighbour that had sent the message.
     * @param d distance.
     * @return the list of messages to send.
     */
    private List<MDVAMsg> process(MDVAMsgType et, NID m, int d) {
        // a list of messages to send
        List<MDVAMsg> send = new ArrayList<>();
        //  variables
        Set<NID> s = successorSet(); // previous successor set -- compute before making updates
        if (ln.keySet().contains(m)) { // only remember reported distances if there is an outgoing link
/*04*/      DistUtil.put(dn, m, d); // update last reported distance
        }
/*05*/  int cd = bestMDVADist(); // current MDVA distance through all outgoing links (D^i_j)
/*06*/  int sd = bestMDVADist(s); // shortest MDVA distance through successor set (SD^i_j)
        // Note: it is always a case that cd <= sd (since cd is minimum over a larger set)
        assert cd <= sd;

        // process special message types logic and assertions upfront
        switch (et) {
            case QUERY:
                assert ln.containsKey(m) : "QUERY can arrive via outgoing link only";
                wn.add(m); // remember to reply
                break;
            case REPLY:
                assert r.contains(m) : "REPLY cannot be unsolicited by QUERY";
                r.remove(m); // update reply set of replies that are being waited for
        }

/*07*/  if (r.isEmpty()) {
            // we are PASSIVE or just received the last reply and is becoming passive
/*09*/      if (cd > rd) {
                // distance became worse -- activate diffusing computation
/*08*/          fd = rd; // feasible distance is the previously reported distance
/*14*/          rd = sd; // new reported distance is the distance though successor set
                // after these operations we have fd <= cd <= rd
                assert fd <= cd && cd <= rd;
                // now send queries with new (large) reported distance
                for (NID k : in) {
/*15*/              send.add(new MDVAMsg(MDVAMsgType.QUERY, getId(), k, rd));
                    r.add(k); // will wait for reply
                }
            }
            // because the algorithm now works for directed graph, it can be the case that no queries
            // were actually sent by the above code and we must return to PASSIVE state again,
            // or the above code was not executed, because
            // distance became better or stayed the same
            // Anyway, we must update distances and end ACTIVE phase (all replies received)
            if (r.isEmpty()) {
/*08*/          fd = cd; // feasible distance is set to be the same as the computed distance
                // sending pending replies
                for (NID k : wn) {
/*19*/              send.add(new MDVAMsg(MDVAMsgType.REPLY, getId(), k, cd));
                }
                // send updates the the rest of neighbour nodes if previously reported different distance
                for (NID k : in) {
                    // Original MDVA sends UPDATE when cd != rd, however here we send update if either
                    // cd != rd or cd != rdR, so and additional update is send when previous QUERY to a node was
                    // for a different distance
                    if (!wn.contains(k) && (cd != rd)) {
                        /* ERROR IN PAPER: send [UPDATE,j,RD] must be send [UPDATE,j,D] -- must send new distance */
/*20*/                  send.add(new MDVAMsg(MDVAMsgType.UPDATE, getId(), k, cd));
                    }
                }
/*23*/          rd = cd; // update reported distance -- all distances are the same the end of ACTIVE phase (cd == fd == rd)
/*24*/          wn.clear(); // clear set of pending replies
            }
        } else {
            // ACTIVE phase is in process
/*27*/      if (et == MDVAMsgType.QUERY) {
/*28*/          if (!s.contains(m) || sd <= rd) {
                    wn.remove(m); // REPLY immediately in this case, undo addition to the reply set
/*29*/              send.add(new MDVAMsg(MDVAMsgType.REPLY, getId(), m, rd));
                }
            }
        }
/*33*/  // we don't need to update s, because it is computed on the fly when needed
        return send;
    }

    // computes successor set
    public Set<NID> successorSet() {
        return ln.keySet().stream()
                .filter(k -> DistUtil.get(dn, k) < fd)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // best MDVA distance over all outgoing links
    public int bestMDVADist() {
        return bestMDVADist(ln.keySet());
    }

    // best MDVA distance over a given set
    public int bestMDVADist(Set<NID> s) {
        return bestDistOverSetViaMap(s, dn);
    }

    /**
     * Update outgoing link status.
     * @param m the node to which the link was established.
     * @param d distance of the link.
     * @return the list of messages to send.
     */
    @Override
    public List<MDVAMsg> updateOutgoingLink(NID m, int d) {
        assert d < DistUtil.INF;
        ln.put(m, d);
        // process update with last received distance
        return process(MDVAMsgType.UPDATE, m, DistUtil.get(dn, m));
    }

    /**
     * Update incoming link status.
     * @param m the node from which the link was established.
     * @return the list of messages to send.
     */
    @Override
    public List<MDVAMsg> updateIncomingLink(NID m) {
        boolean newLink = in.add(m);
        if (newLink && rd < DistUtil.INF) // advertise only on new links
            return Collections.singletonList(new MDVAMsg(MDVAMsgType.UPDATE, getId(), m, rd));
        else
            return Collections.emptyList();
    }

    // remove node link (both incoming and outgoing)
    @Override
    public List<MDVAMsg> removeLink(NID m) {
        // drop incoming link from the node first (don't send QUERY there anymore)
        in.remove(m); // remove it from incoming link tables
        // process distance update on outgoing link
        List<MDVAMsg> send;
        if (r.contains(m)) // was waiting to reply over this link -- process as if INF was received
            send = process(MDVAMsgType.REPLY, m, DistUtil.INF);
        else
            send = process(MDVAMsgType.UPDATE, m, DistUtil.INF);
        // clear all information about the link
        ln.remove(m); // remove outgoing link
        wn.remove(m); // no longer pending to send REPLY
        return send;
    }

    @Override
    public int getLinkFlags(NID m) {
        Set<NID> s = successorSet();
        int best = bestMDVADist(s);
        return (s.contains(m) ? LINK_BOLD : 0) +
                (best != DistUtil.INF && s.contains(m) && distViaMap(m, dn) == best ? LINK_ROUTE : 0);
    }

    @Override
    public String toString() {
        Set<NID> s = successorSet();
        int cd = bestMDVADist();
        int sd = bestMDVADist(s);
        String str = "Node " + i +
                ": cd=" + DistUtil.d2s(cd) + " sd=" + DistUtil.d2s(sd) + " s=" + s +
                " fd=" + DistUtil.d2s(fd) + " rd=" + DistUtil.d2s(rd) +
                " dn=" + dn +
                " ln=" + ln;
        if (!r.isEmpty())
            str += " r=" + r;
        if (!wn.isEmpty())
            str += " wn=" + wn;
        return str;
    }

    @Override
    public void addNodeDataTo(List<String> nodeStr) {
        Set<NID> s = successorSet();
        int cd = bestMDVADist();
        int sd = bestMDVADist(s);
        nodeStr.add(i + (r.isEmpty() ? "" : " ACTIVE"));
        nodeStr.add("cd=" + DistUtil.d2s(cd) + " sd=" + DistUtil.d2s(sd) + " s=" + s);
        nodeStr.add("fd=" + DistUtil.d2s(fd) + " rd=" + DistUtil.d2s(rd));
        nodeStr.add("dn=" + dn);
        if (!r.isEmpty())
            nodeStr.add("r=" + r);
        if (!wn.isEmpty())
            nodeStr.add("wn=" + wn);
    }

    @Override
    public String verifyQuiescentDistance(int td) {
        if (isActive())
            return "Node " + i + " is still active";
        int cd = bestMDVADist(); // current MDVA distance
        if (cd != td)
            return "Node " + i + " current MDVA distance " + DistUtil.d2s(cd) + " != " + DistUtil.d2s(td) + " of true distance";
        return null;
    }
}

package sim;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author Roman Elizarov
 */
public class NetworkUIModel  {
    private NetworkModel<?, ?> model;
    private final Map<NID,Point> points = new HashMap<>();
    private int nextNode;
    private NID lastNode;

    private List<Runnable> msgListeners = new ArrayList<>();
    private List<Runnable> nodeListeners = new ArrayList<>();

    public NetworkUIModel(AbstractAlgo algo) {
        model = NetworkModel.createNetworkModel(true, algo);
        init();
    }

    private void init() {
        newNode(new Point(NetworkCanvas.WIDTH / 2, NetworkCanvas.HEIGHT / 6));
    }

    public AbstractAlgo<?, ?> getAlgo() {
        return model.getAlgo();
    }

    public Set<NID> getNIDs() {
        return model.getNIDs();
    }

    public Collection<? extends AbstractNode<?>> getNodes() {
        return model.getNodes();
    }

    public AbstractNode getNode(NID i) {
        return model.getNode(i);
    }

    public int getLink(NID from, NID to) {
        return model.getLink(from, to);
    }

    public List<? extends AbstractMsg> getMsgs() {
        return model.getMsgs();
    }

    public Point getPoint(NID i) {
        return points.get(i);
    }

    public void setPoint(NID i, Point p) {
        points.put(i, p);
        fireNodeUpdateListeners();
    }

    public NID newNode(Point p) {
        NID i = NID.getNID(nextNode++);
        model.getNode(i); // create
        points.put(i, p);
        lastNode = i;
        fireNodeUpdateListeners();
        return i;
    }

    public void updateLink(NID from, NID to, int d) {
        model.updateLink(from, to, d);
        fireNodeUpdateListeners();
        fireMsgUpdateListeners();
    }

    public void removeNode(NID i) {
        points.remove(i);
        model.removeNode(i);
        if (i.equals(lastNode)) {
            lastNode = null;
            nextNode--;
        }
        fireNodeUpdateListeners();
        fireMsgUpdateListeners();
    }

    public void processMessage(int i) {
        model.processMessage(i);
        fireNodeUpdateListeners();
        fireMsgUpdateListeners();
    }

    public void updateAlgo(String name) {
        System.out.println("Updating algo to " + name);
        NetworkModel<?, ?> newModel = NetworkModel.createNetworkModel(true, AlgoFactory.createAlgo(name));
        for (NID i : model.getNIDs()) {
            newModel.getNode(i);// create node
            for (Map.Entry<NID, Integer> link : model.getNode(i).getOutgoingLinks().entrySet()) {
                newModel.updateLink(i, link.getKey(), link.getValue());
            }
        }
        model = newModel;
        fireNodeUpdateListeners();
        fireMsgUpdateListeners();
    }

    public void reset() {
        model.clear();
        points.clear();
        nextNode = 0;
        init();
        fireMsgUpdateListeners();
        fireNodeUpdateListeners();
    }

    public void addMsgsUpdateListener(Runnable r) {
        msgListeners.add(r);
    }

    public void addNodeUpdateListener(Runnable r) {
        nodeListeners.add(r);
    }

    protected void fireMsgUpdateListeners() {
        msgListeners.forEach(Runnable::run);
    }

    protected void fireNodeUpdateListeners() {
        nodeListeners.forEach(Runnable::run);
    }

}

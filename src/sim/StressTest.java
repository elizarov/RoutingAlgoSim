package sim;

import java.util.*;

/**
 * @author Roman Elizarov
 */
public class StressTest<M extends AbstractMsg, N extends AbstractNode<M>> {
    private static final boolean DEBUG = false;

    private static final int SEED = 1;

    private static final int MIN_NODES = 2;
    private static final int MAX_NODES = 20;
    private static final int MIN_UPDATES = 1;
    private static final int MAX_UPDATES = 20;
    private static final int MIN_D = 1;
    private static final int MAX_D = 100;
    private static final int BATCHES = 100_000;

    private static final double REMOVE_LINK_PR = 0.25;
    private static final double REMOVE_NODE_PR = 0.01;
    private static final double MORE_PR = 0.25;

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        new StressTest(AlgoFactory.createAlgo(args[0])).go();
    }

    private final NetworkModel<M, N> model;
    private final Random rnd = new Random(SEED);

    public StressTest(AbstractAlgo<M, N> algo) {
        model = NetworkModel.createNetworkModel(DEBUG, algo);
    }

    private void go() {
        System.out.println("Testing " + model.getAlgo().getClass().getName());
        int batchNo = 0;
        while (batchNo < BATCHES) {
            processBatch(batchNo);
            batchNo++;
            if (!DEBUG) {
                // only print dots
                if (batchNo % 100 == 0)
                    System.out.print('.');
                if (batchNo % 10000 == 0)
                    System.out.println();
            }
        }
        System.out.println("=== PASSED SUCCESSFULLY ===");
    }

    private void processBatch(int batchNo) {
        // report state before batch in debug mode
        if (DEBUG) {
            System.out.println("-----");
            report();
        }
        // make a batch of changes
        int nu = MIN_UPDATES + rnd.nextInt((MAX_UPDATES - MIN_UPDATES) * batchNo / BATCHES + 1);
        for (int i = 0; i < nu; i++)
            randomUpdate(batchNo);
        // process all messages
        while (!model.getMsgs().isEmpty()) {
            // more changes in the process with some probability
            while (rnd.nextDouble() < MORE_PR)
                randomUpdate(batchNo);
            if (model.getMsgs().isEmpty())
                break; // dropped some link and have no more messages to process.
            // ensure FIFO processing over each link
            int i;
            do {
                i = rnd.nextInt(model.getMsgs().size());
            } while (!model.getMsgs().get(i).firstOverLink);
            // process message
            model.processMessage(i);
        }
        // and verify
        String text = model.verifyInQuiescentState();
        if (text != null)
            fail(text);
    }

    private void randomUpdate(int batchNo) {
        int nn = MIN_NODES + (MAX_NODES - MIN_NODES) * batchNo / BATCHES;
        if (rnd.nextDouble() < REMOVE_NODE_PR) {
            // remove node
            model.removeNodeLinks(NID.getNID(rnd.nextInt(nn)));
            return;
        }
        // update/remove link
        NID from;
        NID to;
        do {
            from = NID.getNID(rnd.nextInt(nn));
            to = NID.getNID(rnd.nextInt(nn));
        } while (from.equals(to));
        if (rnd.nextDouble() < REMOVE_LINK_PR)
            model.removeLink(from, to);
        else
            model.updateLink(from, to, MIN_D + rnd.nextInt((MAX_D - MIN_D) * batchNo / BATCHES + 1));
    }

    private void fail(String s) {
        System.out.println();
        System.out.println("=== FAIL: " + s + " ===");
        report();
        System.exit(1);
    }

    private void report() {
        model.getNodes().forEach(System.out::println);
    }
}

package sim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Roman Elizarov
 */
public class CmdLineDemo<M extends AbstractMsg, N extends AbstractNode<M>> {
    private final NetworkModel<M, N> model;

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {
        new CmdLineDemo(AlgoFactory.createAlgo(args[0])).go();
    }

    public CmdLineDemo(AbstractAlgo<M, N> algo) {
        this.model = NetworkModel.createNetworkModel(true, algo);
    }

    public void go() throws IOException {
        help();
        model.getNode(NID.DEST);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (!Thread.currentThread().isInterrupted()) {
            String[] cmd = in.readLine().split("\\s+");
            if (cmd.length != 3) {
                error();
                continue;
            }
            NID from = NID.getNID(cmd[0]);
            NID to = NID.getNID(cmd[1]);
            int d;
            try {
                d = Integer.parseInt(cmd[2]);
            } catch (NumberFormatException e) {
                error();
                continue;
            }
            int d1 = d == 0 ? DistUtil.INF : d;
            model.updateLink(from, to, d1);
            processAll();
            report();
        }
    }

    public void processAll() {
        while (!model.getMsgs().isEmpty())
            model.processMessage(0);
    }

    private void report() {
        model.getNodes().forEach(System.out::println);
    }

    private void error() {
        System.out.println("Wrong command");
        help();
    }

    private void help() {
        System.out.println("Type: <from> <to> <dist> to add link");
        System.out.println("Use dist 0 to remove link");
        System.out.println("Node '" + NID.DEST + "' is a destination");
    }
}

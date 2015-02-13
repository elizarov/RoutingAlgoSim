package sim;

/**
 * @author Roman Elizarov
 */
public abstract class AbstractMsg {
    public final NID from; // neighbour that sends the message
    public final NID to; // to node
    public boolean firstOverLink; // true only for first message over link to ensure FIFO

    public AbstractMsg(NID from, NID to) {
        this.from = from;
        this.to = to;
    }

    public abstract String getDescription();

    public String toShortString() {
        return from + "->" + to + " " + getDescription();
    }

    public String toString() {
        return toShortString() + (firstOverLink ? "" : " !FIRST");
    }

    public boolean isOverLink(NID from, NID to) {
        return this.from.equals(from) && this.to.equals(to);
    }

    public boolean isSameLink(AbstractMsg o) {
        return o.from.equals(from) && o.to.equals(to);
    }
}

package sim;

/**
 * @author Roman Elizarov
 */
public class NID implements Comparable<NID> {
    public static final NID DEST = new NID("0"); // fixed destination node (j) to compute distance to
    private final String name;

    public static NID getNID(String name) {
        return new NID(name);
    }

    public static NID getNID(int i) {
        return getNID("" + i);
    }

    private NID(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && name.equals(((NID) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public int compareTo(NID o) {
        return name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return name;
    }
}

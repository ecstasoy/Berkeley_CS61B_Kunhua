public interface DisjointSet {
    /**
     * Connects two elements p and q.
     */
    void connect(int p, int q);

    /**
     * Returns true if p and q are connected.
     */
    boolean isConnected(int p, int q);
}

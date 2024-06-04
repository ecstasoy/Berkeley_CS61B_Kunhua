public class QuickUnionDS implements DisjointSet{
    private int[] parent;

    public QuickUnionDS(int N) {
        parent = new int[N];
        for (int i = 0; i < N; i++) {
            parent[i] = i;
        }
    }

    private int root(int p) {
        while (p != parent[p]) {
            p = parent[p];
        }
        return p;
    }

    @Override
    public boolean isConnected(int p, int q) {
        return root(p) == root(q);
    }

    @Override
    public void connect(int p, int q) {
        int rootP = root(p);
        int rootQ = root(q);
        parent[rootP] = rootQ;
    }

}

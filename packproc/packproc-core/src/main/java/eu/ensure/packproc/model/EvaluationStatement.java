package eu.ensure.packproc.model;

/**
 * User: Frode Randers
 * Date: 2012-03-07
 */
public class EvaluationStatement {
    public static final int POSITIVE = 1;
    public static final int NEUTRAL = 0;
    public static final int NEGATIVE = -1;

    private String path;
    private int factor;
    private String statement;

    public EvaluationStatement(String path, int factor, String statement) {
        this.path = path;
        this.factor = factor;
        this.statement = statement;
    }

    public String getPath() {
        return path;
    }

    public int getFactor() {
        return factor;
    }

    public String getStatement() {
        return statement;
    }
}

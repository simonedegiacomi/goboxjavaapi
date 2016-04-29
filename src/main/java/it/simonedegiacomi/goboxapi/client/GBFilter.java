package it.simonedegiacomi.goboxapi.client;

/**
 * Filter used to search files
 * Created on 26/03/16.
 * @author Degiacomi Simone
 */
public class GBFilter {

    /**
     * Number of result to find
     */
    public static final long DEFAULT_RESULT_SIZE = 50;

    /**
     * Offset of the result list
     */
    private long from;

    /**
     * Length of result list
     */
    private long size = DEFAULT_RESULT_SIZE;

    /**
     * Keyword of the query
     */
    private String keyword;

    /**
     * Kind of file
     */
    private String kind;

    public long getStart() {
        return from;
    }

    public void setStart(long start) {
        this.from = start;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

}
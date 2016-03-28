package it.simonedegiacomi.goboxapi.client;

/**
 * Created by simone on 26/03/16.
 */
public class GBFilter {

    public static final long DEFAULT_RESULT_SIZE = 50;

    private long from;
    private long size = DEFAULT_RESULT_SIZE;
    private String keyword;
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

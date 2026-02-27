package Table;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TableFormatter<E> {

    protected static final int headerReserveCap = 80;
    protected static final String defaultNullRepr = "null";
    protected static final int defaultDataMatIndentSize = 2;

    protected @Nullable ReadableTable<E> table;

    protected String nullRepr;
    protected int dataMatIndentSize;
    protected int @Nullable [] colWidths;
    protected int colWidthsLen;
    protected boolean colWidthsUseDefault;
    protected boolean colWidthsOutdated;

    protected TableFormatter(
            @Nullable ReadableTable<E> table,
            String nullRepr,
            int dataMatIndentSize,
            int @Nullable [] colWidths, int colWidthsLen, boolean colWidthsUseDefault, boolean colWidthsOutdated) {
        this.table = table;
        this.nullRepr = nullRepr;
        this.dataMatIndentSize = dataMatIndentSize;
        this.colWidths = colWidths;
        this.colWidthsLen = colWidthsLen;
        this.colWidthsUseDefault = colWidthsUseDefault;
        this.colWidthsOutdated = colWidthsOutdated;
    }

    public static <T> TableFormatter<T> createDefault() {
        return new TableFormatter<T>(
                null,
                defaultNullRepr,
                defaultDataMatIndentSize,
                null, 0, true, true);
    }
    public static <T> TableFormatter<T> createDefaultFrom(@NotNull ReadableTable<T> table) {
        return new TableFormatter<T>(
                table,
                defaultNullRepr,
                defaultDataMatIndentSize,
                new int [table.colCapacity], table.cols, true, true);
    }

    public String getTableRepr() {
        requireTableNonNull();
        doUpdateColWidths();
        return doGetTableRepr();
    }

    public void appendTableRepr(StringBuilder sb) {
        requireTableNonNull();
        doUpdateColWidths();
        doAppendTableRepr(sb);
    }

    public void appendHeader(StringBuilder sb) {
        requireTableNonNull();
        doUpdateColWidths();
        doAppendHeader(sb);
    }
    public void appendDataRow(StringBuilder sb, final int r) {
        requireTableNonNull();
        doUpdateColWidths();
        doAppendDataRow(sb, colWidths, r);
    }
    public void appendDataCell(StringBuilder sb, final int r, final int c) {
        requireTableNonNull();
        doUpdateColWidths(); assert colWidths != null; // assert is just for IDE
        doAppendDataCell(sb, colWidths, r, c);
    }

    public int [] calDataMatColWidths() {
        requireTableNonNull(); assert table != null; // assert is just for IDE

        int [] result = new int [table.cols];
        if (colWidthsUseDefault) {
            doUpdateColWidths(); assert colWidths != null; // assert is just for IDE
            System.arraycopy(colWidths, 0, result, 0, table.cols);
        } else {
            doCalDataMatColWidths(result);
        }
        return result;
    }

    protected void handleTableChange() {
        // mark all cache outdated
        colWidthsOutdated = true;
    }
    private void doUpdateColWidths() {
        assert table != null; // ensure by call site
        if (!colWidthsUseDefault || !colWidthsOutdated) {
            return;
        }
        if (colWidths == null || colWidths.length < table.cols) {
            colWidths = new int [table.colCapacity];
        }
        colWidthsLen = table.cols;
        doCalDataMatColWidths(colWidths);
        colWidthsOutdated = false;
        return;
    }

    private String doGetTableRepr() {
        StringBuilder sb = new StringBuilder( doCalTableReprReserveCap(colWidths) );
        doAppendTableRepr(sb);
        return sb.toString();
    }
    private void doAppendTableRepr(StringBuilder sb) {
        doAppendHeader(sb);
        sb.append(" [\n");
        doAppendDataMat(sb);
        sb.append("\n]");
    }
    private void doAppendDataMat(StringBuilder sb) {
        assert table != null; // ensure this at call site
        if (table.rows == 0) {
            sb.append("(empty)");
            return;
        }
        appendRepeatSpace(sb, dataMatIndentSize);
        doAppendDataRow(sb, colWidths, 0);
        for (int r = 1; r < table.rows; ++r) {
            sb.append(",\n");
            appendRepeatSpace(sb, dataMatIndentSize);
            doAppendDataRow(sb, colWidths, r);
        }
    }
    private void doAppendDataRow(StringBuilder sb, final int [] colWidths, final int r) {
        assert table != null; // please ensure this at call site
        if (table.cols == 0) {
            sb.append("[ (empty row) ]");
            return;
        }
        sb.append("[");
        doAppendDataCell(sb, colWidths, r, 0);
        for (int c = 1; c < table.cols; ++c) {
            sb.append(',');
            doAppendDataCell(sb, colWidths, r, c);
        }
        sb.append("]");
    }

    private void doAppendDataCell(StringBuilder sb, final int [] colWidths, final int r, final int c) {
        assert table != null; // please ensure this at call site
        E val = table.doGetElementCasted(r, c);
        String s = (val == null) ? nullRepr : val.toString();
        int len = s.length();
        // floor div + 1
        appendRepeatSpace(sb, (colWidths[c] - len) / 2 + 1);
        sb.append(s);
        // ceil div + 1, ceil: (n + d - 1) / d
        appendRepeatSpace(sb, (colWidths[c] - len + 2 - 1) / 2 + 1);
    }

    private void doAppendHeader(StringBuilder sb) {
        assert table != null; // ensure this at call site
        sb.append("Table<").append((table.elementType == null ? "Unknown" : table.elementType.getSimpleName()))
                .append(">: ").append(table.rows).append(" x ").append(table.cols)
                .append(" (capacity: ").append(table.rowCapacity).append(" x ").append(table.colCapacity)
                .append(")");
    }

    private int doCalTableReprReserveCap(final int [] colWidths) {
        return headerReserveCap + doCalDataMatReserveCap(colWidths) + 10;
    }

    private void doCalDataMatColWidths(int [] result) {
        assert table != null; // ensure this by call site
        assert result.length >= table.cols; // ensure this by call site
        // set all to 0
        for (int c = 0; c < table.cols; ++c) { result[c] = 0; }
        // core logic
        for (int r = 0; r < table.rows; ++r) {
            for (int c = 0; c < table.cols; ++c) {
                E val = table.doGetElementCasted(r, c);
                int widthRequire = (val == null) ? nullRepr.length() : val.toString().length();
                if (widthRequire > result[c]) {
                    result[c] = widthRequire;
                }
            }
        }
    }
    protected int calDataRowReserveCap(final int [] colWidths) {
        requireTableNonNull(); assert table != null;
        int capPerRow = dataMatIndentSize + 3 * table.cols + 3;
        for (int colCap : colWidths) { capPerRow += colCap; }
        return capPerRow;
    }
    protected int doCalDataMatReserveCap(final int [] colWidths) {
        requireTableNonNull(); assert table != null;
        return calDataRowReserveCap(colWidths) * table.rows + 10;
    }

    private static void appendRepeatSpace(StringBuilder sb, int numRepeat) {
        appendRepeatChar(sb, numRepeat, ' ');
    }
    private static void appendRepeatChar(StringBuilder sb, int numRepeat, char chr) {
        for (int i = 0; i < numRepeat; ++i) {
            sb.append(chr);
        }
    }

    protected void requireTableNonNull() throws IllegalStateException {
        if (table == null) {
            throw new IllegalStateException("Invalid state");
        }
    }

}

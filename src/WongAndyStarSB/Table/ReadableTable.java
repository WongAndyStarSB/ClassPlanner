package Table;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;

public class ReadableTable<E> {

    protected static int defaultRowCapacity = 5;
    protected static int defaultColCapacity = 5;

    public final Class<E> elementType;
    protected Object[] data;
    protected int rows = 0;
    protected int cols = 0;
    protected int rowCapacity = 0;
    protected int colCapacity = 0;
    protected TableFormatter<E> formatter = null;

    protected ReadableTable(final Class<E> elementType, final Object[] data, final int rows, final int cols, final int rowCap, final int colCap, final TableFormatter<E> formatter) {
        this.elementType = elementType;
        this.data = data;
        this.rows = rows;
        this.cols = cols;
        this.rowCapacity = rowCap;
        this.colCapacity = colCap;
        this.formatter = formatter;
    }

    protected static <T> @NotNull ReadableTable<T> doCreateWithSizeCapacity(final Class<T> elementType, final int rows, final int cols, final int rowCap, final int colCap) {
        // no check
        return new ReadableTable<T>(
                elementType,
                new Object[rowCap * colCap],
                rows, cols,
                rowCap, colCap,
                null
        );
    }

    public static <T> @NotNull ReadableTable<T> createEmpty(final Class<T> elementType) {
        return doCreateWithSizeCapacity(elementType, 0, 0, defaultRowCapacity, defaultColCapacity);
    }

    public static <T> @NotNull ReadableTable<T> createCopy(final ReadableTable<T> other) {
        ReadableTable<T> result = doCreateWithSizeCapacity(
                other.elementType,
                other.rows, other.cols,
                other.rowCapacity, other.colCapacity
        );
        System.arraycopy(other.data, 0, result.data, 0, other.rows * other.colCapacity);
        return result;
    }
    public static <T> @NotNull ReadableTable<T> createWithSizeCapacity(final Class<T> elementType, final int rows, final int cols, final int rowCap, final int colCap) {
        validateDimensions(rows, cols);
        validateDimensions(rowCap, colCap);
        return doCreateWithSizeCapacity(elementType, rows, cols, rowCap, colCap);
    }

    public static <T> @NotNull ReadableTable<T> createWithCapacity(final Class<T> elementType, final int rowCap, final int colCap) {
        validateDimensions(rowCap, colCap);
        Object[] data = new Object[rowCap * colCap];
        return new ReadableTable<T>(elementType, data, 0, 0, rowCap, colCap, null);
    }

    public static <T> @NotNull ReadableTable<T> createWithSize(final Class<T> elementType, final int rows, final int cols) {
        validateDimensions(rows, cols);
        final int rowCap = rows * 3 / 2;
        final int colCap = cols * 3 / 2;
        return doCreateWithSizeCapacity(elementType, rows, cols, rowCap, colCap);
    }
    public static <T> @NotNull ReadableTable<T> createWithSize(final Class<T> elementType, final int rows, final int cols, final T defaultVal) {
        ReadableTable<T> result = createWithSize(elementType, rows, cols);
        for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
                result.data[r * result.colCapacity + c] = defaultVal;
            }
        }
        return result;
    }

    public static <T> @NotNull ReadableTable<T> createFromArr(final Class<T> elementType, final T[][] arr2d) {
        int rows = arr2d.length;
        if (rows == 0) {
            return createEmpty(elementType);
        }
        int cols = arr2d[0].length;
        for (int r = 1; r < rows; ++r) {
            if (arr2d[r].length != cols) {
                throw new IllegalArgumentException(String.format(
                        "InconsistentColumnSize: at row %d: expected %d, but found %d",
                        r, cols, arr2d[r].length));
            }
        }
        ReadableTable<T> result = createWithSize(elementType, rows, cols, null);
        for (int r = 0; r < rows; ++r) {
            System.arraycopy(arr2d[r], 0, result.data, r * result.colCapacity, cols);
        }
        return result;
    }

    // getters

    public @NotNull Class<E> getElementType() {
        return elementType;
    }

    public int getNumRows() {
        return rows;
    }

    public int getNumCols() {
        return cols;
    }

    public int getRowCapacity() {
        return rowCapacity;
    }

    public int getColCapacity() {
        return colCapacity;
    }

    public @NotNull TableFormatter<E> getFormatter() {
        if (formatter == null) { formatter = TableFormatter.createDefaultFrom(this); }
        return formatter;
    }

    public E get(int rowIndex, int colIndex) {
        validateRowIndex(rowIndex);
        validateColIndex(colIndex);
        return doGetElementCasted(rowIndex, colIndex);
    }

    public Object getElementAsObject(int rowIndex, int colIndex) {
        validateRowIndex(rowIndex);
        validateColIndex(colIndex);
        return doGetElementAsObject(rowIndex, colIndex);
    }

    @SuppressWarnings("unchecked")
    public E[] getRowClone(final int rowIndex) {
        validateRowIndex(rowIndex);
        E[] result = (E[]) Array.newInstance(elementType, cols);
        System.arraycopy(data, rowIndex * colCapacity, result, 0, cols);
        return result;
    }

    @SuppressWarnings("unchecked")
    public E[] getColClone(final int colIndex) {
        validateColIndex(colIndex);
        E[] result = (E[]) Array.newInstance(elementType, rows);
        for (int r = 0; r < rows; ++r) {
            result[r] = (E) data[toFlatIndex(r, colIndex)];
        }
        return result;
    }

    public Object[] getUnderlyingArrayClone() {
        return data.clone();
    }

    // transpose

    public @NotNull ReadableTable<E> transpose() {
        ReadableTable<E> result = createWithSizeCapacity(
                elementType,
                rows, cols,
                rowCapacity, colCapacity
        );
        for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
                result.data[toFlatIndex(r, c)] = data[toFlatIndex(c, r)];
            }
        }
        return result;
    }

    // clone

//    @SuppressWarnings("unchecked")
//    public Table<E> clone() {
//        try {
//            Table<E> cloned = (Table<E>) super.clone();
//            cloned.data = this.data.clone();
//            return cloned;
//        } catch (CloneNotSupportedException e) {
//            throw new AssertionError();
//        }
//    }

    // copy

    public @NotNull ReadableTable<E> copy() {
        return createCopy(this);
    }
    public @NotNull ReadableTable<E> copyAndTrim() {
        ReadableTable<E> result = doCreateWithSizeCapacity(elementType, rows, cols, rows, cols);
        for (int r = 0; r < rows; ++r) {
            System.arraycopy(data, r * colCapacity, result.data, r * cols, cols);
        }
        return result;
    }

    // common functions

    @Override
    public @NotNull String toString() {
        if (formatter == null) {
            formatter = TableFormatter.createDefaultFrom(this);
        }
        return formatter.getTableRepr();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (!(obj instanceof ReadableTable<?> other)) { return false; }
        if ((this.rows != other.rows) || (this.cols != other.cols)) { return false; }
        for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
                if (!java.util.Objects.equals(this.get(r, c), other.get(r, c))) {
                    return false;
                }
            }
        }
        return true;
    }
    @Override
    public int hashCode() {
        int result = java.util.Objects.hash(rows, cols);
        for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
                result *= 31;
                result += java.util.Objects.hash(get(r, c));
            }
        }
        return result;
    }

    // helpers

    protected int toFlatIndex(final int rowIndex, final int colIndex) {
        return rowIndex * colCapacity + colIndex;
    }

    // helpers - getters

    @SuppressWarnings("unchecked")
    protected E doGetElementCasted(int rowIndex, int colIndex) {
        return (E) data[this.toFlatIndex(rowIndex, colIndex)];
    }

    protected Object doGetElementAsObject(int rowIndex, int colIndex) {
        return data[this.toFlatIndex(rowIndex, colIndex)];
    }

    @SuppressWarnings("unchecked")
    protected E doGetElementCasted(int flatIndex) {
        return (E) data[flatIndex];
    }

    protected Object doGetElementAsObject(int flatIndex) {
        return data[flatIndex];
    }






    public boolean isIndexValid(final int rowIndex, final int colIndex) {
        return (rowIndex >= 0 && rowIndex < rows && colIndex >= 0 && colIndex < cols);
    }

    protected void validateRowIndex(final int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rows) {
            throw new IllegalArgumentException(String.format(
                    "IllegalRowIndex: rowIndex(%d) out of range(0 to %d)",
                    rowIndex, rows-1));
        }
    }
    protected void validateColIndex(final int colIndex) {
        if (colIndex < 0 || colIndex >= cols) {
            throw new IllegalArgumentException(String.format(
                    "IllegalColIndex: colIndex(%d) out of range(0 to %d)",
                    colIndex, cols-1));
        }
    }
    protected void validateEndRowIndex(final int endRowIndex) {
        if (endRowIndex < 0 || endRowIndex > rows) {
            throw new IllegalArgumentException(String.format(
                    "IllegalEndRowIndex: endRowIndex(%d) out of range(0 to %d)",
                    endRowIndex, rows));
        }
    }
    protected void validateEndColIndex(final int endColIndex) {
        if (endColIndex < 0 || endColIndex > cols) {
            throw new IllegalArgumentException(String.format(
                    "IllegalEndColIndex: endColIndex(%d) out of range(0 to %d)",
                    endColIndex, cols));
        }
    }
    protected void validateRowBeginEnd(final int beginRowIndex, final int endRowIndex) {
        validateRowIndex(beginRowIndex);
        validateEndRowIndex(endRowIndex);
        if (beginRowIndex >= endRowIndex) {
            throw new IllegalArgumentException(String.format(
                    "EndBeforeBegin: endRowIndex(given: %d) has to be larger than beginRowIndex(given: %d)",
                    beginRowIndex, endRowIndex
            ));
        }
    }
    protected void validateColBeginEnd(final int beginColIndex, final int endColIndex) {
        validateColIndex(beginColIndex);
        validateEndColIndex(endColIndex);
        if (beginColIndex >= endColIndex) {
            throw new IllegalArgumentException(String.format(
                    "EndBeforeBegin: endColIndex(given: %d) has to be larger than beginColIndex(given: %d)",
                    beginColIndex, endColIndex
            ));
        }
    }
    protected void validateRowSize(final int newRowSize) {
        if (newRowSize < 0) {
            throw new IllegalArgumentException(String.format(
                    "IllegalNewRowSize: newRowSize(%d) cannot be smaller than 0",
                    newRowSize));
        }
    }
    protected void validateColSize(final int newColSize) {
        if (newColSize < 0) {
            throw new IllegalArgumentException(String.format(
                    "IllegalNewColSize: newColSize(%d) cannot be smaller than 0",
                    newColSize));
        }
    }
    protected void validateNonNeg(final int sizeRelated) {
        if (sizeRelated < 0) {
            throw new IllegalArgumentException(String.format(
                    "NegativeSizeRelatedValue: given value %d cannot be negative", sizeRelated
            ));
        }
    }
    protected void validateSizeNewRow(final E[] newRow) {
        if (newRow.length != cols) {
            throw new IllegalArgumentException(String.format(
                    "MismatchRowSize: expected %d but %d were given",
                    cols, newRow.length));
        }
    }
    protected void validateSizeNewCol(final E[] newCol) {
        if (newCol.length != rows) {
            throw new IllegalArgumentException(String.format(
                    "MismatchColSize: expected %d but %d were given",
                    rows, newCol.length));
        }
    }
    protected void validateRowCapacity(final int newRowCap) {
        if (newRowCap < rows) {
            throw new IllegalArgumentException(String.format(
                    "IllegalRowCapacity: new row capacity (%d) cannot be smaller than current logical row size (%d)",
                    newRowCap, rows));
        }
    }
    protected void validateColCapacity(final int newColCap) {
        if (newColCap < cols) {
            throw new IllegalArgumentException(String.format(
                    "IllegalColCapacity: new col capacity (%d) cannot be smaller than current logical col size (%d)",
                    newColCap, cols));
        }
    }
    protected static void validateDimensions(final int rows, final int cols) {
        if (rows < 0 || cols < 0) {
            throw new IllegalArgumentException(String.format(
                    "InvalidDimension: table dimensions must be non-negative: %d x %d",
                    rows, cols));
        }
    }
}

package Table;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Table<E> extends ReadableTable<E> {

    protected Table(final Class<E> elementType, final Object[] data, final int rows, final int cols, final int rowCap, final int colCap, final TableFormatter<E> formatter) {
        super(elementType, data, rows, cols, rowCap, colCap, formatter);
    }

    // factory methods

    protected static <T> @NotNull Table<T> doCreateWithSizeCapacity(final Class<T> elementType, final int rows, final int cols, final int rowCap, final int colCap) {
        // no check
        return new Table<T>(
                elementType,
                new Object[rowCap * colCap],
                rows, cols,
                rowCap, colCap,
                null
        );
    }

    public static <T> @NotNull Table<T> createEmpty(final Class<T> elementType) {
        return doCreateWithSizeCapacity(elementType, 0, 0, defaultRowCapacity, defaultColCapacity);
    }

    public static <T> @NotNull Table<T> createCopy(final Table<T> other) {
        Table<T> result = doCreateWithSizeCapacity(
                other.elementType,
                other.rows, other.cols,
                other.rowCapacity, other.colCapacity
        );
        System.arraycopy(other.data, 0, result.data, 0, other.rows * other.colCapacity);
        return result;
    }
    public static <T> @NotNull Table<T> createWithSizeCapacity(final Class<T> elementType, final int rows, final int cols, final int rowCap, final int colCap) {
        validateDimensions(rows, cols);
        validateDimensions(rowCap, colCap);
        return doCreateWithSizeCapacity(elementType, rows, cols, rowCap, colCap);
    }

    public static <T> @NotNull Table<T> createWithCapacity(final Class<T> elementType, final int rowCap, final int colCap) {
        validateDimensions(rowCap, colCap);
        Object[] data = new Object[rowCap * colCap];
        return new Table<T>(elementType, data, 0, 0, rowCap, colCap, null);
    }

    public static <T> @NotNull Table<T> createWithSize(final Class<T> elementType, final int rows, final int cols) {
        validateDimensions(rows, cols);
        final int rowCap = rows * 3 / 2;
        final int colCap = cols * 3 / 2;
        return doCreateWithSizeCapacity(elementType, rows, cols, rowCap, colCap);
    }
    public static <T> @NotNull Table<T> createWithSize(final Class<T> elementType, final int rows, final int cols, final T defaultVal) {
        Table<T> result = createWithSize(elementType, rows, cols);
        for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
                result.data[r * result.colCapacity + c] = defaultVal;
            }
        }
        return result;
    }

    public static <T> @NotNull Table<T> createFromArr(final Class<T> elementType, final T[][] arr2d) {
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
        Table<T> result = createWithSize(elementType, rows, cols, null);
        for (int r = 0; r < rows; ++r) {
            System.arraycopy(arr2d[r], 0, result.data, r * result.colCapacity, cols);
        }
        return result;
    }

    // getters (remain as super)

    // simple setters

    public Table<E> set(final int rowIndex, final int colIndex, @Nullable E val) {
        validateRowIndex(rowIndex);
        validateColIndex(colIndex);
        doSet(rowIndex, colIndex, val);

        return this;
    }

    public Table<E> setRow(final int rowIndex, final @Nullable E[] row) {
        validateRowIndex(rowIndex);
        validateSizeNewRow(row);
        System.arraycopy(row, 0, data, rowIndex * colCapacity, cols);
        return this;
    }

    public Table<E> setCol(final int colIndex, final @Nullable E[] col) {
        validateColIndex(colIndex);
        validateSizeNewCol(col);
        for (int r = 0; r < cols; ++r) {
            data[toFlatIndex(r, colIndex)] = col[r];
        }
        return this;
    }

    // resize/add/remove rows/cols

    public Table<E> resizeRows(int newRowSize) {
        validateRowSize(newRowSize);
        if (newRowSize < rows) {
            removeRows(newRowSize, rows);
        } else {
            addRows(newRowSize - rows);
        }
        return this;
    }

    public Table<E> removeRow(final int rowIndex) {
        validateRowIndex(rowIndex);
        for (int r = 0; r < rows-1; ++r) {
            System.arraycopy(data, (r+1) * colCapacity, data, r * colCapacity, cols);
        }
        for (int c = 0; c < cols; ++c) {
            data[toFlatIndex(rows-1, c)] = null;
        }
        rows -= 1;
        return this;
    }

    public Table<E> removeCol(final int colIndex) {
        validateColIndex(colIndex);
        for (int c = 0; c < cols-1; ++c) {
            for (int r = 0; r < rows; ++r) {
                data[toFlatIndex(r, c)] = doGetElementAsObject(r, c+1);
            }
        }
        for (int r = 0; r < rows; ++r) {
            data[toFlatIndex(r, cols-1)] = null;
        }
        cols -= 1;
        return this;
    }

    // [begin, end)
    public Table<E> removeRows(final int beginRowIdx, final int endRowIdx) {
        validateRowIndex(beginRowIdx);
        validateEndRowIndex(endRowIdx);
        validateRowBeginEnd(beginRowIdx, endRowIdx);
        for (int r = beginRowIdx; r < endRowIdx; ++r) {
            for (int c = 0; c < cols; ++c) {
                data[toFlatIndex(r, c)] = null;
            }
        }
        rows -= endRowIdx - beginRowIdx;
        return this;
    }

    public Table<E> addRow(final @Nullable E[] row) {
        validateSizeNewRow(row);
        doGrowRowCapIfNeeded(rows + 1);
        System.arraycopy(row, 0, data, rows * colCapacity, cols);
        rows += 1;
        return this;
    }

    public Table<E> addRows(int numRowsToAdd) {
        validateNonNeg(numRowsToAdd);
        if (numRowsToAdd == 0) {
            return this;
        }
        doGrowRowCapIfNeeded(rows + numRowsToAdd);
        rows += numRowsToAdd;
        return this;
    }

    public Table<E> addRows(E[] defaultVals) {
        int numRowsToAdd = defaultVals.length;
        if (numRowsToAdd == 0) {
            return this;
        }
        doGrowRowCapIfNeeded(cols + numRowsToAdd);
        for (int i = 0; i < numRowsToAdd; ++i) {
            for (int c = 0; c < cols; ++c) {
                data[toFlatIndex(rows + i, c)] = defaultVals[i];
            }
        }
        rows += numRowsToAdd;
        return this;
    }

    public Table<E> addCol(final @Nullable E[] col) {
        validateSizeNewCol(col);
        doGrowColCapIfNeeded(rows + 1);
        for (int r = 0; r < rows; ++r) {
            data[r * colCapacity + cols] = col[r];
        }
        cols += 1;
        return this;
    }

    public Table<E> addCols(int numColsToAdd) {
        validateNonNeg(numColsToAdd);
        if (numColsToAdd == 0) {
            return this;
        }
        doGrowColCapIfNeeded(cols + numColsToAdd);
        cols += numColsToAdd;
        return this;
    }

    public Table<E> addCols(E[] defaultVals) {
        int numColsToAdd = defaultVals.length;
        if (numColsToAdd == 0) {
            return this;
        }
        doGrowColCapIfNeeded(cols + numColsToAdd);
        for (int r = 0; r < rows; ++r) {
            for (int i = 0; i < numColsToAdd; ++i) {
                data[toFlatIndex(r, cols + i)] = defaultVals[i];
            }
        }
        cols += numColsToAdd;
        return this;
    }

    // transpose

    @Override
    public @NotNull Table<E> transpose() {
        Table<E> result = createWithSizeCapacity(
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

    public @NotNull Table<E> beTransposed() {
        for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
                Object tmp = doGetElementAsObject(r, c);
                data[toFlatIndex(r, c)] = data[toFlatIndex(c, r)];
                data[toFlatIndex(c, r)] = tmp;
            }
        }
        return this;
    }

    // Capacity/Reallocate

    public void setRowCapacity(final int newRowCap) {
        validateRowCapacity(newRowCap);
        doReallocRow(newRowCap);
    }
    public void setColCapacity(final int newColCap) {
        validateColCapacity(newColCap);
        doReallocCol(newColCap);
    }

    public void reallocate(final int newRowCap, final int newColCap) {
        if (newRowCap < rows || newColCap < cols) {
            throw new IllegalArgumentException("New capacity cannot be smaller than current logical size.");
        }
        if (newRowCap == rowCapacity && newColCap == colCapacity) {
            return; // don't do anything
        } else if (newRowCap == rowCapacity) {
            doReallocCol(newColCap);
        } else if (newColCap == colCapacity) {
            doReallocRow(newRowCap);
        } else {
            doRealloc(newRowCap, newColCap);
        }
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

    @Override
    public @NotNull Table<E> copy() {
        return createCopy(this);
    }
    @Override
    public @NotNull Table<E> copyAndTrim() {
        Table<E> result = doCreateWithSizeCapacity(elementType, rows, cols, rows, cols);
        for (int r = 0; r < rows; ++r) {
            System.arraycopy(data, r * colCapacity, result.data, r * cols, cols);
        }
        return result;
    }

    // common functions

    //  toString (remain same as super)
    //  equals (remain same as super)
    //  hashCode (remain same as super)

    // helpers

    //  toFlatIndex (remain same as super)

    // helpers - getters

    //  doGetElementCasted (remain same as super)
    //  doGetElementAsObject (remain same as super)
    //  doGetElementCasted (remain same as super)
    //  doGetElementAsObject (remain same as super)


    // helpers - setters

    protected void doSet(final int rowIndex, final int colIndex, E val) {
        data[toFlatIndex(rowIndex, colIndex)] = val;
    }

    protected void doSet(final int flatIndex, E val) {
        data[flatIndex] = val;
    }

    protected void doSetElementRaw(final int rowIndex, final int colIndex, Object rawVal) {
        data[toFlatIndex(rowIndex, colIndex)] = rawVal;
    }

    protected void doSetElementRaw(final int flatIndex, Object rawVal) {
        data[flatIndex] = rawVal;
    }

    // helpers - capacity/reallocate

    protected void doGrowCapIfNeeded(final int minNeededRowCap, final int minNeededColCap) {
        if (minNeededRowCap > rowCapacity && minNeededColCap > colCapacity) {
            doRealloc(minNeededRowCap * 3 / 2, minNeededColCap * 3 / 2);
            return;
        }
        if (minNeededRowCap > rowCapacity) {
            doReallocRow(minNeededRowCap * 3 / 2);
            return;
        }
        if (minNeededColCap > colCapacity) {
            doReallocCol(minNeededColCap * 3 / 2);
            return;
        }
        return;
    }
    protected void doGrowRowCapIfNeeded(final int minNeededRowCap) {
        if (minNeededRowCap > rowCapacity) {
            doReallocRow(minNeededRowCap * 3 / 2);
        }
    }
    protected void doGrowColCapIfNeeded(final int minNeededColCap) {
        if (minNeededColCap > colCapacity) {
            doReallocCol(minNeededColCap * 3 / 2);
        }
    }

    protected void doRealloc(final int newRowCap, final int newColCap) {
        // Note: no argument check
        // newRowCap should be >= rows and newColCap >= cols, this is ensured by caller
        Object[] result = new Object[newRowCap * newColCap];
        for (int r = 0; r < rows; ++r) {
            System.arraycopy(data, r * colCapacity, result, r * newColCap, cols);
        }
        rowCapacity = newRowCap;
        colCapacity = newColCap;
        data = result; // reference changed, the original Object[] is now unreachable
    }
    protected void doReallocRow(final int newRowCap) {
        // Note: no argument check
        // newRowCap should be >= rows, this is ensured by caller
        Object[] result = new Object[newRowCap * colCapacity];
        System.arraycopy(data, 0, result, 0, rows * colCapacity);
        rowCapacity = newRowCap;
        data = result; // reference changed, the original Object[] is now unreachable
    }
    protected void doReallocCol(final int newColCap) {
        // Note: no argument check
        // newRowCap should be >= cols, this is ensured by caller
        Object[] result = new Object[rowCapacity * newColCap];
        for (int r = 0; r < rows; ++r) {
            System.arraycopy(data, r * colCapacity, result, r * newColCap, cols);
        }
        colCapacity = newColCap;
        data = result; // reference changed, the original Object[] is now unreachable
    }


    // isIndexValid (remain same as super)

    private void updateFormatter() {
        if (formatter != null) {
            formatter.handleTableChange();
        }
    }

    // throw if condition methods (remain same as super)
}

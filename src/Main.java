import WongAndyStarSB.Table.ReadableTable;
import WongAndyStarSB.Table.Table;

public class Main {
    public static void main(String[] args) {
        Integer[][] arr2d = {
                {1, 2, 3, 4, 5},
                {2, 3, 4, 5, 9999},
                {3, 4, 555, 6, 7},
                {10, 11, 101, 22, 2}
        };
        Table<Integer> table = Table.createFromArr(Integer.class, arr2d);
        Table<Table> tt = Table.createWithSize(Table.class, 3, 3);
        System.out.print(table);
        System.out.print(tt);
        ReadableTable<Table> rtt = (ReadableTable<Table>) tt;
        System.out.print(rtt);
    }
}

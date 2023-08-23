package org.example.InputPoint.SQLdb;

import org.example.InputPoint.InputDataSource;
import org.example.InputPoint.SQLdb.SQLconnector.DatabaseConnector;
import tech.tablesaw.api.Table;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;

public class DBSchema extends InputDataSource implements Iterable<RTable> {
    private HashMap<String, RTable> rTables = new HashMap<>();
    private DatabaseConnector connector;
    private String schema;

    public DBSchema(){
        connector =  new DatabaseConnector();
        schema = connector.getSchemaName();
        retrieveSchema();
        connector.closeConnection();
    }

    /** For single table like files like CSV **/
    public DBSchema(String tableName, Table inputTable, String PKcol) {
        RTable table = new RTable();
        table.addPK(PKcol);
        for(String colName : inputTable.columnNames())  // table columns
            table.addColumn(colName, inputTable.column(colName).type().getPrinterFriendlyName());
        rTables.put(tableName, table);
    }

    public void retrieveSchema() {
        try {
            ResultSet tables = connector.retrieveTables();
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                RTable table = new RTable();

                // table columns
                ResultSet columns = connector.retrieveTableColumns(tableName);
                while (columns.next())
                    table.addColumn(
                            columns.getString("COLUMN_NAME"),
                            columns.getString("TYPE_NAME")
                    );
                columns.close();

                // PKs
                ResultSet PKs = connector.retrieveTablePKs(tableName);
                while (PKs.next())
                    table.addPK(PKs.getString("COLUMN_NAME"));
                PKs.close();

                // FKs
                ResultSet FKs = connector.retrieveTableFKs(tableName);
                while (FKs.next())
                    table.addFK(
                            FKs.getString("FKCOLUMN_NAME"),
                            FKs.getString("PKTABLE_NAME"),
                            FKs.getString("PKCOLUMN_NAME")
                    );
                FKs.close();

                table.setTableOperations();
                rTables.put(tableName, table);
            }
            tables.close();
        }catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println(rTables.toString());
    }

    public Iterator<RTable> iterator() {
        return rTables.values().iterator();
    }

    public HashMap<String, RTable> getrTables() {
        return rTables;
    }
    public RTable getTable(String tableName) {
        return rTables.get(tableName);
    }

    public boolean isPK(String tableName, String columnName){
        return rTables.get(tableName).isPK(columnName);
    }
    public String getSchemaName(){return schema;}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DBSchema{\n");
        rTables.forEach((tableName, rTable) -> {
            sb.append(tableName).append("\n").append(rTable.toString()).append("\n");
        });
        sb.append("}");
        return sb.toString();
    }
}

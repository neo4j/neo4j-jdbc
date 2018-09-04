package org.neo4j.jdbc.metadata;

import java.util.ArrayList;
import java.util.List;

public class Column {
	private static final List<String> COLUMN_NAMES;
	
	static {
		COLUMN_NAMES = new ArrayList<>();

		COLUMN_NAMES.add("TABLE_CAT");
		COLUMN_NAMES.add("TABLE_SCHEM"); 
		COLUMN_NAMES.add("TABLE_NAME");
		COLUMN_NAMES.add("COLUMN_NAME"); 
		COLUMN_NAMES.add("DATA_TYPE");
		COLUMN_NAMES.add("TYPE_NAME");
		COLUMN_NAMES.add("COLUMN_SIZE"); 
		COLUMN_NAMES.add("BUFFER_LENGTH"); 
		COLUMN_NAMES.add("DECIMAL_DIGITS");
		COLUMN_NAMES.add("NUM_PREC_RADIX"); 
		COLUMN_NAMES.add("NULLABLE"); 
		COLUMN_NAMES.add("REMARKS");
		COLUMN_NAMES.add("COLUMN_DEF"); 
		COLUMN_NAMES.add("SQL_DATA_TYPE"); 
		COLUMN_NAMES.add("SQL_DATETIME_SUB"); 
		COLUMN_NAMES.add("CHAR_OCTET_LENGTH");
		COLUMN_NAMES.add("ORDINAL_POSITION");
		COLUMN_NAMES.add("IS_NULLABLE");
		COLUMN_NAMES.add("SCOPE_CATALOG"); 
		COLUMN_NAMES.add("SCOPE_SCHEMA");
		COLUMN_NAMES.add("SCOPE_TABLE");
		COLUMN_NAMES.add("SOURCE_DATA_TYPE");
		COLUMN_NAMES.add("IS_AUTOINCREMENT"); 
		COLUMN_NAMES.add("IS_GENERATEDCOLUMN");
	}
	
	private String tableName;
	private String columnName;
	private int columnsPosition;
	
	public Column(String tableName, String columnName, int columnsPosition) {
		this.tableName = tableName;
		this.columnName = columnName;
		this.columnsPosition = columnsPosition;
	}

	public static List<String> getColumns() {
		return COLUMN_NAMES;
	}

	public String getTableCat() {
		return null;
	}

	public String getTableSchem() {
		return null;
	}
 
	public String getTableName() {
    return this.tableName;		
	}

	public String getColumnName() {
		return this.columnName;
	}
 
	public String getDataType() {
		return null;
	}

	public String getTypeName() {
		return "unknown";
	}

	public String getColumnSize() {
		return null;
	}
 
	public String getBufferLength() {
		return null;
	}
 
	public String getDecimalDigits() {
		return null;
	}

	public String getNumPrecRadix() {
		return null;
	}
 
	public String getNullable() {
		return "columnNoNulls";
	}
 
	public String getRemarks() {
		return this.columnName + " node property for label :" + this.tableName;
	}

	public String getColumnDef() {
		return null;
	}
 
	public String getSqlDataType() {
		return null;
	}
 
	public String getSqlDatetimeSub() {
		return null;
	}
 
	public String getCharOctetLength() {
		return "0";
	}

	public int getOrdinalPosition() {
		return this.columnsPosition;
	}

	public String getIsNullable() {
		return "NO";
	}

	public String getScopeCatalog() {
		return null;
	}
 
	public String getScopeSchema() {
		return null;
	}

	public String getScopeTable() {
		return null;
	}

	public String getSourceDataType() {
		return null;
	}

	public String getIsAutoincrement() {
		return "NO";
	}
 
	public String getIsGeneratedcolumn() {
		return "NO";
	}

	public List<Object> toResultSetRow() {
		List<Object> row = new ArrayList<>();
		row.add(this.getTableCat());
		row.add(this.getTableSchem());
		row.add(this.getTableName());
		row.add(this.getColumnName());
		row.add(this.getDataType());
		row.add(this.getTypeName());
		row.add(this.getColumnSize());
		row.add(this.getBufferLength());
		row.add(this.getDecimalDigits());
		row.add(this.getNumPrecRadix());
		row.add(this.getNullable());
		row.add(this.getRemarks());
		row.add(this.getColumnDef());
		row.add(this.getSqlDataType());
		row.add(this.getSqlDatetimeSub());
		row.add(this.getCharOctetLength());
		row.add(this.getOrdinalPosition());
		row.add(this.getIsNullable());
		row.add(this.getScopeCatalog());
		row.add(this.getScopeSchema());
		row.add(this.getScopeTable());
		row.add(this.getSourceDataType());
		row.add(this.getIsAutoincrement());
		row.add(this.getIsGeneratedcolumn());
    return row;
	}
}

package org.neo4j.jdbc.metadata;

import java.util.ArrayList;
import java.util.List;

public class Table {
	private static final List<String> COLUMN_NAMES;
	
	static {
		COLUMN_NAMES = new ArrayList<>();
		
		COLUMN_NAMES.add("TABLE_CAT");
		COLUMN_NAMES.add("TABLE_SCHEM");
		COLUMN_NAMES.add("TABLE_NAME");
		COLUMN_NAMES.add("TABLE_TYPE");
		COLUMN_NAMES.add("REMARKS");
		COLUMN_NAMES.add("TYPE_CAT");
		COLUMN_NAMES.add("TYPE_SCHEM");
		COLUMN_NAMES.add("TYPE_NAME");
		COLUMN_NAMES.add("SELF_REFERENCING_COL_NAME");
		COLUMN_NAMES.add("REF_GENERATION");
	}
	
	private String tableName;
	
	public Table(String tableName) {
		this.tableName = tableName;
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
	
	public String getTableType() {
		return "TABLE";
	}
	
	public String getRemarks() {
		return ":" + this.tableName + " node label";
	}
	
	public String getTypeCat() {
		return null;
	}
	
	public String getTypeSchem() {
		return null;
	}
	
	public String getTypeName() {
		return null;
	}
	
	public String getSelfReferencingColName() {
		return null;
	}
	
	public String getRefGeneration() {
		return null;
	}
	
	public List<Object> toResultSetRow() {
		List<Object> row = new ArrayList<>();
		row.add(this.getTableCat());
		row.add(this.getTableSchem());
		row.add(this.getTableName());
		row.add(this.getTableType());
		row.add(this.getRemarks());
		row.add(this.getTypeCat());
		row.add(this.getTypeSchem());
		row.add(this.getTypeName());
		row.add(this.getSelfReferencingColName());
		row.add(this.getRefGeneration());
		return row;
	}
}

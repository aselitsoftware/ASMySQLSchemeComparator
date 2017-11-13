package com.aselitsoftware;

public class IndexItem {

    private boolean unique;
    private String keyName;
    private int index;
    private String columnName;
    private String collation;
    private boolean nullable;
    private String indexType;
    
    public IndexItem(boolean unique, String keyName, int index,
		String columnName, String collation, boolean nullable,
		String indexType) {
		
    	this.unique = unique;
		this.keyName = (keyName.equals("primary") ? keyName.toUpperCase() : keyName);
		this.index = index;
		this.columnName = columnName;
		this.collation = collation;
		this.nullable = nullable;
		this.indexType = indexType;
	}

	public boolean getUnique() {
    	
    	return unique;
    }
    
    public String getKeyName() {
    	
    	return keyName;
    }
    
    public int getIndex() {
    	
    	return index;
    }
    
    public String getColumnName() {
    	
    	return columnName;
    }
    
    public String getCollation() {
    	
    	return collation;
    }
    
    public boolean getNullable() {
    	
    	return nullable;
    }
    
    public String getIndexType() {
    	
    	return indexType;
    }
}

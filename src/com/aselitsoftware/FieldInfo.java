package com.aselitsoftware;

public class FieldInfo {

	private String name;
	private String description = "";
	
	public FieldInfo(String name) {
		
		this.name = name;
	}
	
	public FieldInfo(String name, String description) {
		
		this.name = name;
		this.description = description;
	}
	
	public String getName() {
		
		return name;
	}
	
	public String getDescription() {
		
		return description;
	}
	
	public void setDescription(String type, boolean nullable, String def, String extra) {
		
        description = type;
        if (!nullable)
        	description = description.concat(" NOT NULL");
        if (!def.isEmpty())
        	description = description.concat(" DEFAULT \'").concat(def).concat("\'");
        else
        	if (nullable)
        		description = description.concat(" DEFAULT NULL");
        if (!extra.isEmpty())
        	description = description.concat(" ").concat(extra);
	}
}

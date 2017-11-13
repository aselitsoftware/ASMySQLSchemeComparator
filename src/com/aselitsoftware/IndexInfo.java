package com.aselitsoftware;
public class IndexInfo {

	private String name;
	private String description = "";
	
	public IndexInfo(String name) {
		
		this.name = name;
	}
	
	public String getName() {
		
		return name;
	}
	
	public void setDescription(String description) {
		
		this.description = description;
	}
	
	public String getDescription() {
		
		return description;
	}
}

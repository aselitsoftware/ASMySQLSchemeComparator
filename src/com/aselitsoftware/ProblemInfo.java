package com.aselitsoftware;

public class ProblemInfo {

	private String text = "";
	private String hint = "";
	private String sql = "";

	public ProblemInfo() {
		
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getHint() {
		return hint;
	}

	public void setHint(String hint) {
		this.hint = hint;
	}

	public String getSQL() {
		return sql;
	}

	public void setSQL(String sql) {
		this.sql = sql;
	}
}

package com.aselitsoftware;
import java.util.ArrayList;
import java.util.List;


public class CompareResult {

	public static enum CompareResultObjTypeEnum {
		
		crotScheme, crotTable;
	}
	
	private String objName = "";
	private String sql = "";
	private List<ProblemInfo> problems;
	private CompareResultObjTypeEnum objType;
	
	public CompareResult(CompareResultObjTypeEnum objType) {
	
		this.objType = objType;
		problems = new ArrayList<ProblemInfo>();
	}
	
	public String getObjName() {
		
		return objName;
	}
	
	public void setObjName(String objName) {
		
		this.objName = objName;
	}

	public String getSQL() {
		
		return sql;
	}

	public void setSQL(String sql) {

		this.sql = sql;
	}
	
	public void addProblem(ProblemInfo problem) {
		
		if (null == problem)
			return;
		problems.add(problem);
	}
	
	public int getProblemCount() {
		
		return problems.size();
	}
	
	public ProblemInfo getProblem(int index) throws IndexOutOfBoundsException {
		
		if ((index < 0) || (index >= problems.size()))
				throw new IndexOutOfBoundsException("");
		return problems.get(index);
	}
	
	public CompareResultObjTypeEnum getObjType() {
		
		return objType;
	}
}

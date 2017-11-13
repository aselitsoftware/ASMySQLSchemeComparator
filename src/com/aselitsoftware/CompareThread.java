package com.aselitsoftware;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.aselitsoftware.CompareResult.CompareResultObjTypeEnum;
import com.aselitsoftware.mysql.ConnectionInstaller;

public class CompareThread extends Thread {

//	interface for display thread state, error and result
	public interface CompareThreadInterface {

		public void showCompareThreadState(String value);
		public void showCompareThreadError(String value);
		public void showCompareThreadResults(CompareModeEnum mode, List<CompareResult> results,
				int totalTableCount, String dstConnectionParams);
	}
	
	public static enum CompareModeEnum {
		
		cmNone, cmCompareDatabases, cmCreateMasterDump, cmCreateSlaveDump, cmCompareMasterDump, cmCompareSlaveDump;
		
		public static CompareModeEnum fromInt(int value) {
			
			int i;
			
			CompareModeEnum []v = values();
			
			for (i = 0; i < v.length; i++)
				if (value == v[i].ordinal())
					return v[i];
			return cmNone;
		}
	};
	
	public static final String MASTER_DATABASE_NAME = "Master scheme";
	public static final String SLAVE_DATABASE_NAME = "Slave scheme";

	
	private CompareModeEnum mode;
	private ConnectionInstaller masterConnection;
	private ConnectionInstaller slaveConnection;
//	private String errorMessage = "";
	private CompareThreadInterface impl;
	
	private List<CompareResult> results;
	
	public CompareThread(ConnectionParams connectionParams, CompareModeEnum mode, CompareThreadInterface impl) {
	
		this.masterConnection = new ConnectionInstaller(connectionParams.getMasterDatabase());
		masterConnection.setName(MASTER_DATABASE_NAME);
		this.slaveConnection = new ConnectionInstaller(connectionParams.getSlaveDatabase());
		slaveConnection.setName(SLAVE_DATABASE_NAME);
		this.mode = mode;
		this.results = new ArrayList<CompareResult>();
		
		this.impl = impl;
	}
	
	/**
	 * 
	 * @param database Database name.
	 * @param table Table name.
	 * @param sql
	 * @return
	 */
	private String formatAlterTable(String database, String table, String sql) {
		
		return String.format("ALTER TABLE `%s`.`%s`\r\n%s", database, table, sql);
	}
	
	@Override
	public void run() {

		ConnectionInstaller srcConnection = null;
		ConnectionInstaller dstConnection = null;
		String []tableNames;
		int i, j, k;
		List<TableDescription> srcTables = null, dstTables;
		DumpFile dump;
		
		try {
			
			dump = new DumpFile();
			srcTables = new ArrayList<TableDescription>();
			dstTables = new ArrayList<TableDescription>();
			
			if ((mode.equals(CompareModeEnum.cmCompareMasterDump) || mode.equals(CompareModeEnum.cmCompareSlaveDump)) &&
				!dump.isExists())
				throw new Exception(String.format("Scheme dump file \"%s\" not found.", DumpFile.FILE_NAME));

			switch (mode) {
			case cmCompareDatabases:
//				check master connection parameters
				masterConnection.checkParams();
//				check slave connection parameters
				slaveConnection.checkParams();
				if (masterConnection.equals(slaveConnection))
					throw new Exception("Specified same databases.");
				srcConnection = masterConnection;
				srcConnection.setName(MASTER_DATABASE_NAME);
				dstConnection = slaveConnection;
				dstConnection.setName(SLAVE_DATABASE_NAME);
				break;
			case cmCreateMasterDump:
//				check master connection parameters
				masterConnection.checkParams();
				srcConnection = masterConnection;
				srcConnection.setName(MASTER_DATABASE_NAME);
				dstConnection = null;
				break;
			case cmCreateSlaveDump:
//				check slave connection parameters
				slaveConnection.checkParams();
				srcConnection = slaveConnection;
				srcConnection.setName(SLAVE_DATABASE_NAME);
				dstConnection = null;
				break;
			case cmCompareMasterDump:
//				check master connection parameters
				masterConnection.checkParams();
				srcConnection = null;
				dstConnection = masterConnection;
				dstConnection.setName(MASTER_DATABASE_NAME);
				break;
			case cmCompareSlaveDump:
//				check slave connection parameters
				slaveConnection.checkParams();
				srcConnection = null;
				dstConnection = slaveConnection;
				dstConnection.setName(SLAVE_DATABASE_NAME);
				break;
			default:
				break;
			}
			
			if (null != srcConnection) {
			
				impl.showCompareThreadState(String.format("Establish connection to \"%s\"...", srcConnection.getName()));
			
				if (!srcConnection.connect())
					throw new Exception(String.format("Error occur while connect to database \"%s\".", srcConnection.getName()));
				
				if (isInterrupted())
					throw new Exception("Process terminated by user.");
				
				impl.showCompareThreadState("Request tables list...");
				tableNames = srcConnection.RequestTables();
				for (i = 0; i < tableNames.length; i++) {
					
					TableDescription table = new TableDescription(srcConnection, tableNames[i]);
					srcTables.add(table);
				}
//				make dump file
				if (mode.equals(CompareModeEnum.cmCreateMasterDump) || mode.equals(CompareModeEnum.cmCreateSlaveDump)) {
					
					dump.save(srcTables);
				}
			} else {
				
				if (mode.equals(CompareModeEnum.cmCompareMasterDump) || mode.equals(CompareModeEnum.cmCompareSlaveDump)) {
				
					impl.showCompareThreadState(String.format("Load dump from file \"%s\".", DumpFile.FILE_NAME));
					dump.load(srcTables);
				}
			}
			
			
			if (null != dstConnection) {
				
				impl.showCompareThreadState(String.format("Establish connection to \"%s\"...", dstConnection.getName()));
				
				if (!dstConnection.connect())
					throw new Exception(String.format("Error occur while connect to database \"%s\".", dstConnection.getName()));
				
				if (isInterrupted())
					throw new Exception("Process terminated by user.");
				
				try {
					
					dstConnection.checkDatabase();
				} catch (Exception ex) {
					
					ProblemInfo problem = new ProblemInfo();
					problem.setText(String.format("Scheme \"%s\" does not exist.", dstConnection.getDatabase()));
					problem.setSQL(String.format("CREATE DATABASE %s", dstConnection.getDatabase()));
					
					CompareResult res = new CompareResult(CompareResultObjTypeEnum.crotScheme);
					res.setObjName(dstConnection.getDatabase());
					res.setSQL(problem.getSQL());
					res.addProblem(problem);
					results.add(res);
					
					throw new Exception(ex.getMessage());
				}
				
				impl.showCompareThreadState("Request tables list...");
				tableNames = dstConnection.RequestTables();
				
				for (i = 0; i < tableNames.length; i++) {
					
					TableDescription table = new TableDescription(dstConnection, tableNames[i]);
					dstTables.add(table);
				}
			}
			
			if (mode.equals(CompareModeEnum.cmCompareDatabases) ||
				mode.equals(CompareModeEnum.cmCompareMasterDump) ||
				mode.equals(CompareModeEnum.cmCompareSlaveDump)) {
					
				i = 0;
				for (Iterator<TableDescription> it = srcTables.iterator(); it.hasNext(); i++) {
					
					if (isInterrupted())
						throw new Exception("Process terminated by user.");
					
					TableDescription srcTable = (TableDescription) it.next();
					impl.showCompareThreadState(String.format("CompareTable \"%s\" (%d / %d)...", srcTable.getName(), i, srcTables.size()));
					
					j = -1;
					while (++j < dstTables.size()) {
						
						TableDescription dstTable = dstTables.get(j);
						if (srcTable.getName().equalsIgnoreCase(dstTable.getName()))
							break;
					}
					if (dstTables.size() == j) {
						
						ProblemInfo problem = new ProblemInfo();
						problem.setText("Table does not exist.");
						problem.setSQL(srcTable.getCreateSQL().replaceFirst(srcTable.getQuotedName(), String.format("`%s`.`%s`", dstConnection.getDatabase(), srcTable.getName())));
						
						CompareResult res = new CompareResult(CompareResultObjTypeEnum.crotTable);
						res.setObjName(srcTable.getName());
						res.setSQL(problem.getSQL());
						res.addProblem(problem);
						results.add(res);
					} else {
						
						CompareResult res = null;
						
						TableDescription dstTable = dstTables.get(j);
						List<String> sql = new ArrayList<String>();
						int modifyPos = 0;
						
						for (k = 0; k < srcTable.getFieldCount(); k++) {
							
							FieldInfo srcField = srcTable.getField(k);
//							try to search the field by name
							FieldInfo dstField = dstTable.getField(srcField.getName());
							ProblemInfo problem = null;
							
//							if field is not exist
							if (null == dstField) {
								
			                    String s = String.format("ADD COLUMN `%s` %s", srcField.getName(), srcField.getDescription());
		                    	if (0 == k)
		                    		s = s.concat(" FIRST");
		                    	else
		                    		s = s.concat(String.format(" AFTER `%s`", srcTable.getField(k - 1).getName()));
		                    	
		                    	problem = new ProblemInfo();
		                    	problem.setText(String.format("Field \"%s\" does not exist.", srcField.getName()));
		                    	problem.setSQL(formatAlterTable(dstConnection.getDatabase(), srcTable.getName(), s));
		                    	problem.setHint(String.format("%s %s", srcField.getName(), srcField.getDescription()));
		                    	
		                    	sql.add(s);
							} else {
								
								if (!srcField.getDescription().equals(dstField.getDescription())) {
									
									String s = String.format("MODIFY COLUMN `%s` %s", srcField.getName(), srcField.getDescription());
									
									problem = new ProblemInfo();
									problem.setText(String.format("Field \"%s\" have different description.", srcTable.getName()));
									problem.setSQL(formatAlterTable(dstConnection.getDatabase(), srcTable.getName(), s));
									problem.setHint(String.format("%s %s\r\n%s %s", srcField.getName(), srcField.getDescription(),
										dstField.getName(), dstField.getDescription()));
									
									sql.add(modifyPos++, s);
								}
							}
							if (null != problem) {
							
								if (null == res) {

									res = new CompareResult(CompareResultObjTypeEnum.crotTable);
									res.setObjName(srcTable.getName());
								}
								res.addProblem(problem);
							}
						}
						
						modifyPos = sql.size();
						
						for (k = 0; k < srcTable.getIndexCount(); k++) {
							
							IndexInfo srcIndex = srcTable.getIndex(k);
//							try to search the index by name
							IndexInfo dstIndex = dstTable.getIndex(srcIndex.getName());
							ProblemInfo problem = null;
							
							if (null == dstIndex) {
								
								String s = String.format("ADD %s", srcIndex.getDescription());
								
		                    	problem = new ProblemInfo();
		                    	problem.setText(String.format("Index \"%s\" does not exist.", srcIndex.getName()));
		                    	problem.setSQL(formatAlterTable(dstConnection.getDatabase(), srcTable.getName(), s));
		                    	problem.setHint(srcIndex.getDescription());
		                    	
		                    	sql.add(s);
							} else {
								
								if (!srcIndex.getDescription().equals(dstIndex.getDescription())) {
									
									String s = String.format("DROP INDEX `%s`\r\nADD %s", srcIndex.getName(), srcIndex.getDescription());
									
									problem = new ProblemInfo();
									problem.setText(String.format("Index \"%s\" have different description.", srcIndex.getName()));
									problem.setSQL(formatAlterTable(dstConnection.getDatabase(), srcTable.getName(), s));
									problem.setHint(String.format("%s\r\n%s", srcIndex.getDescription(), dstIndex.getDescription()));
									
									sql.add(modifyPos++, s);
								}
							}
							
							if (null != problem) {
								
								if (null == res) {

									res = new CompareResult(CompareResultObjTypeEnum.crotTable);
									res.setObjName(srcTable.getName());
								}
								res.addProblem(problem);
							}
						}
						
						if (null != res) {
							
							String s = "";
							for (String line : sql) {
								
								if (s.length() > 0)
									s = s.concat(",\r\n");
								s = s.concat(line);
							}
							res.setSQL(formatAlterTable(dstConnection.getDatabase(), res.getObjName(), s));
							results.add(res);
						}
					}
				}
			}
			
			if (null != srcConnection)
				srcConnection.close();
			if (null != dstConnection)
				dstConnection.close();
			
			impl.showCompareThreadState("Completed successfully.");
			
			impl.showCompareThreadResults(mode, results, (null != srcTables) ? srcTables.size() : 0,
					((null != dstConnection) ? dstConnection.getConnectionParams() : ((null != srcConnection) ? srcConnection.getConnectionParams() : "")));
//			interrupt();
			
		} catch (Exception ex) {
			
//			System.out.println(ex.getMessage());
			impl.showCompareThreadError(ex.getMessage());
//			interrupt();
		}
		
		/**
		do {
			
			if (!Thread.interrupted()) {
			
			
			} else {
				
				System.out.println("Compare thread was interrupted.");
				return;
			}

			try {
				
				Thread.sleep(1000);
			} catch(InterruptedException e) {
				
				return;
			}
		} while(true);
		*/
	}
}

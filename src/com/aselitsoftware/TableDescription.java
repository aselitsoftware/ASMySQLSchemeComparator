package com.aselitsoftware;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.aselitsoftware.mysql.ConnectionInstaller;


public class TableDescription {

	private String database = "";
	private String name;
	private String createSQL = "";
	private List<FieldInfo> fields;
	private List<IndexInfo> indexes;
	
	{
		this.fields = new ArrayList<FieldInfo>();
		this.indexes = new ArrayList<IndexInfo>();
	}
	
	/**
	 * Constructor retrieves the table description from the database. Need real connection.
	 * @param connection
	 * @param name
	 * @throws Exception
	 */
	public TableDescription(ConnectionInstaller connection, String name) throws Exception {
		
		if (null == connection)
			throw new Exception("Connection is null.");
		if (!connection.isConnected())
			throw new Exception("No connection.");
		this.name = name;
		this.database = connection.getDatabase();
		requestCreateSQL(connection);
		requestFields(connection);
		requestIndexes(connection);
	}
	
	/**
	 * Simple constructor
	 * @param name
	 * @param createSQL
	 * @throws Exception
	 */
	public TableDescription(String name, String createSQL) throws Exception {
		
		this.name = name;
		this.createSQL = createSQL;
	}

	public String getDatabase() {
		
		return database;
	}
	
	public String getName() {
		
		return name;
	}
	
	public String getQuotedName() {
		
		return String.format("`%s`", name);
	}

	public String getCreateSQL() {
		
		return createSQL;
	}
	
	public String getFullQuotedName() {
		
		return String.format("`%s`.`%s`", database, name); 
	}

	/**
	 * 
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
	private void requestCreateSQL(ConnectionInstaller connection) throws Exception {
		
		PreparedStatement st = null;
		ResultSet rs = null;
		String leftPart, rightPart;
		
		try {
			if (database.isEmpty())
				throw new Exception("Database name was not specified.");
			st = connection.prepareSQL(String.format("SHOW CREATE TABLE %s", getFullQuotedName()));
			rs = st.executeQuery();
			if (!rs.first())
				throw new Exception(String.format("Couldn't request \"CREATE\" query for table \"%s\".", name));
//			fix database name
//			createSQL = rs.getString(2).replaceFirst(getQuotedName(), getFullQuotedName());
			createSQL = rs.getString(2);
//			fix line terminator
//			createSQL = createSQL.replace(, "\n\r");
//			fix AUTOINCREMENT
			Pattern pattern = Pattern.compile("\\s*AUTO_INCREMENT=\\d+", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(createSQL);
			if (matcher.find()) {
			
				try {
					
					leftPart = createSQL.substring(0, matcher.start(0));
				} catch (IndexOutOfBoundsException ex) {
					
					leftPart = "";
				}
				
				try {
					
					rightPart = createSQL.substring(matcher.end(0) + 1, createSQL.length());
				} catch (IndexOutOfBoundsException ex) {
					
					rightPart = "";
				}
				createSQL = leftPart + " " + rightPart;
			}
		} finally {
			
			try {
			
				if (rs != null)
					rs.close();
				if (st != null)
					st.close();
			} catch (SQLException e) {
				
				System.out.println(e.getMessage());
			}
		}				
	}
	
	/**
	 * 
	 * @param connection
	 * @throws Exception
	 */
	private void requestFields(ConnectionInstaller connection) throws Exception {
		
		PreparedStatement st = null;
		ResultSet rs = null;
		
		try {
			if (database.isEmpty())
				throw new Exception("Database name was not specified.");
			st = connection.prepareSQL(String.format("SHOW COLUMNS FROM %s", getFullQuotedName()));
			rs = st.executeQuery();
			if (!rs.first())
				throw new Exception(String.format("Couldn't request fields list for table \"%s\".", name));
//			Field, Type, Null, Key, Default, Extra
			do {

//				Field
		        FieldInfo field = new FieldInfo(rs.getString(1).trim().toLowerCase());
//		        Type
		        String type = rs.getString(2).trim().toLowerCase();
//		        Null
		        boolean nullable = rs.getString(3).trim().equals("YES");
//		        Key
//		        String key = rs.getString(4).trim();
//		        Default
		        String def = (null != rs.getObject(5)) ? rs.getString(5).trim() : "";
//		        Extra
		        String extra = (null != rs.getObject(6)) ? rs.getString(6).trim() : "";
		        field.setDescription(type, nullable, def, extra);
		        fields.add(field);
			} while (rs.next());
		} finally {
			
			try {
			
				if (rs != null)
					rs.close();
				if (st != null)
					st.close();
			} catch (SQLException e) {
				
				System.out.println(e.getMessage());
			}
		}				
	}
	
	/**
	 * 
	 * @param data
	 */
	public void requestFields(String data) {
		
		Pattern pattern = Pattern.compile("^(.+?):(.+?)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(data);
		while (matcher.find()) {
		
			FieldInfo field = new FieldInfo(matcher.group(1), matcher.group(2));
			fields.add(field);
		}
	}
	
	/**
	 * 
	 * @param data
	 */
	public void requestIndexes(String data) {
		
		Pattern pattern = Pattern.compile("^(.+?):(.+?)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(data);
		while (matcher.find()) {
		
			IndexInfo index = new IndexInfo(matcher.group(1));
			index.setDescription(matcher.group(2));
			indexes.add(index);
		}
	}
	
	/**
	 * 
	 * @param connection
	 * @throws Exception
	 */
	private void requestIndexes(ConnectionInstaller connection) throws Exception {
		
		PreparedStatement st = null;
		ResultSet rs = null;
		List<IndexItem> items;
		String description;
		boolean flag;
		int no;
		
		items = new ArrayList<IndexItem>();
		
		try {
			if (database.isEmpty())
				throw new Exception("Database name was not specified.");
			st = connection.prepareSQL("SHOW INDEX FROM " + getFullQuotedName());
			rs = st.executeQuery();
			if (rs.first()) {
//				Table [1], Non_unique [2], Key_name [3], Seq_in_index [4], Column_name [5], Collation [6],
//				Cardinality [7], Sub_part [8], Packed [9], Null [10], Index_type [11], Comment [12]
				do {
	
					boolean unique = (0 == rs.getInt(2));
					String keyName = rs.getString(3).trim().toLowerCase();
					int index = rs.getInt(4);
					String columnName = rs.getString(5).trim().toLowerCase();
					String collation = (null != rs.getObject(6)) ? rs.getString(6) : "";
			        boolean nullable = rs.getString(10).trim().equals("YES");
			        String indexType = rs.getString(11).trim().toUpperCase();
			        
			        items.add(new IndexItem(unique, keyName, index, columnName, collation, nullable, indexType));
				} while (rs.next());
			}
		} finally {
			
			try {
			
				if (rs != null)
					rs.close();
				if (st != null)
					st.close();
			} catch (SQLException e) {
				
				System.out.println(e.getMessage());
			}
		}	
		
		for (ListIterator<IndexItem> it1 = items.listIterator(); it1.hasNext();) {
			
			IndexItem item1 = it1.next();
			if (1 == item1.getIndex()) {
				
				IndexInfo index = new IndexInfo(item1.getKeyName());
				if (item1.getKeyName().equals("PRIMARY"))
					description = "PRIMARY KEY";
				else {
					if (item1.getUnique())
						description = "UNIQUE INDEX";
					else
						description = "INDEX";
					description = description.concat(" `").concat(item1.getKeyName()).concat("`");
				}
//				add first field
				description = description.concat(" (`").concat(item1.getColumnName()).concat("`");
//				look for other fields...
				no = 1;
				do {
					no++;
					flag = false;
					for (ListIterator<IndexItem> it2 = items.listIterator(); it2.hasNext();) {
					
						IndexItem item2 = it2.next();
						if ((item2.getIndex() == no) && item1.getKeyName().equals(item2.getKeyName())) {
							
							flag = true;
							description = description.concat(",`").concat(item2.getColumnName()).concat("`");
							break;
						}
					}
				}  while (flag);
				description = description.concat(")");
				index.setDescription(description);
				
				indexes.add(index);
			}
		}
	}
	
	/**
	 * Return the size of the field list.
	 * @return
	 */
	public int getFieldCount() {
		
		return fields.size();
	}
	
	/**
	 * Return the field with specified index.
	 * @param index
	 * @return
	 * @throws IndexOutOfBoundsException
	 */
	public FieldInfo getField(int index) throws IndexOutOfBoundsException {
		
		if ((index < 0) || (index >= fields.size()))
			throw new IndexOutOfBoundsException();
		return fields.get(index);
	}
	
	public FieldInfo getField(String name) {
	
		for (Iterator<FieldInfo> it = fields.iterator(); it.hasNext();) {
			
			FieldInfo field = (FieldInfo) it.next();
			if (field.getName().equalsIgnoreCase(name))
				return field;
		}
		return null;
	}
			
	/**
	 * Return the size of the index list.
	 * @return
	 */
	public int getIndexCount() {
		
		return indexes.size();
	}
	
	/**
	 * Return the index with specified index.
	 * @param index
	 * @return
	 * @throws IndexOutOfBoundsException
	 */
	public IndexInfo getIndex(int index) throws IndexOutOfBoundsException {
		
		if ((index < 0) || (index >= indexes.size()))
			throw new IndexOutOfBoundsException();
		return indexes.get(index);
	}
	
	public IndexInfo getIndex(String name) {
		
		for (Iterator<IndexInfo> it = indexes.iterator(); it.hasNext();) {
			
			IndexInfo index = (IndexInfo) it.next();
			if (index.getName().equalsIgnoreCase(name))
				return index;
		}
		return null;
	}
}

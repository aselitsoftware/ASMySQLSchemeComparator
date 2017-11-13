package com.aselitsoftware;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DumpFile {

	public static final String FILE_NAME = "./dump.sch";
	private final String FIELDS_LIST_MARKER = "--- FIELDS ---";
    private final String INDEXES_LIST_MARKER = "--- INDEXES ---";
	
    private File file;
    
    public DumpFile() {
    	 
    	file = new File(FILE_NAME);
	}
	
    public boolean isExists() {
    	
    	return file.exists();
    }
    
	public void load(List<TableDescription> tables) throws Exception {
		
		FileReader fr = null;
		char []buffer;
		String data = "";
		
		try {
			
			if (!file.exists())
				throw new Exception("File \"".concat(FILE_NAME).concat("\" not found."));
			
			fr = new FileReader(file);
			buffer = new char[(int) file.length()];
			fr.read(buffer);
			
			data = new String(buffer);
			Pattern pattern = Pattern.compile("\\[(.+?)\\](.+?)".concat(FIELDS_LIST_MARKER).concat("(.*?)").concat(INDEXES_LIST_MARKER).concat("([^\\[]*)"), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
			Matcher matcher = pattern.matcher(data);
			while (matcher.find()) {
				
				TableDescription table = new TableDescription(matcher.group(1), matcher.group(2));
				table.requestFields(matcher.group(3));
				table.requestIndexes(matcher.group(4));
				tables.add(table);
			}
			
		} catch (IOException ex) {
			
			System.out.println(ex.getMessage());
		} finally {
			
			try {
				
				if (null != fr)
					fr.close();
			} catch (IOException ex) {
				
				System.out.println(ex.getMessage());
			}
		}
	}
	
	/**
	 * 
	 * @param tables
	 */
	public void save(List<TableDescription> tables) {
		
		FileWriter fr = null;
		String data = "";
		int i;
		
		try {
			
			for (ListIterator<TableDescription> it = tables.listIterator(); it.hasNext();) {
				
				TableDescription table = it.next();
				data = data.concat("[").concat(table.getName()).concat("]");
				data = data.concat(table.getCreateSQL());
				
				data = data.concat(FIELDS_LIST_MARKER);
				for (i = 0; i < table.getFieldCount(); i++) {
					
					FieldInfo field = table.getField(i);
					data = data.concat(field.getName().concat(":").concat(field.getDescription())).concat("\r\n");
				}
				
				data = data.concat(INDEXES_LIST_MARKER);
				for (i = 0; i < table.getIndexCount(); i++) {
					
					IndexInfo index = table.getIndex(i);
					data = data.concat(index.getName().concat(":").concat(index.getDescription())).concat("\r\n");
				}
			}
			
			fr = new FileWriter(file);
			fr.write(data);
		} catch (IOException ex) {
			
			System.out.println(ex.getMessage());
		} finally {
			
			try {
				
				if (null != fr)
					fr.close();
			} catch (IOException ex) {
				
				System.out.println(ex.getMessage());
			}
		}
		
	}

}

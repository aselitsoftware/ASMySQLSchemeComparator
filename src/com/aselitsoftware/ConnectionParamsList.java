package com.aselitsoftware;
import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class ConnectionParamsList {

	private final String FILE_NAME = "./settings.ini";
	private List<ConnectionParams> list;

	public ConnectionParamsList() {
		
		super();
		this.list = new ArrayList<ConnectionParams>();
		load();
	}
	
	/**
	 * Load available connections from "settings.ini" file.
	 */
	private void load() {
		
		Integer index;
		String nameProperty;
		Properties prop = new Properties();
		
		try {
			File file = new File(FILE_NAME);
			if (file.exists()) {
				
				InputStream stream = new FileInputStream(FILE_NAME);
				prop.load(stream);
				
				index = 0;
				while (true) {
				
					nameProperty = prop.getProperty("name" + index.toString());
					if (null == nameProperty)
						break;
					add(new ConnectionParams(nameProperty,
						prop.getProperty("masterDatabase" + index.toString()),
						prop.getProperty("slaveDatabase" + index.toString())));
					index++;
				}
				stream.close();
			}
		} catch (IOException e) {
		}
	}
	
	public void save() {
		
		Integer index;
		Properties prop = new Properties();
		try {
			OutputStream stream = new FileOutputStream(FILE_NAME);

			for (ListIterator<ConnectionParams> it = list.listIterator(); it.hasNext();) {
				
				index = it.nextIndex();
				ConnectionParams con = it.next();
				prop.setProperty("name" + index.toString(), con.getName());
				prop.setProperty("masterDatabase" + index.toString(), con.getMasterDatabase());
				prop.setProperty("slaveDatabase" + index.toString(), con.getSlaveDatabase());
			}
				
			prop.store(stream, "");
			stream.close();
		} catch (IOException e) {
		}
	}
	
	/**
	 * 
	 * @param index index of item
	 * @return
	 */
	public ConnectionParams get(int index) {
	
		try {
			
			return list.get(index);
		} catch (IndexOutOfBoundsException e) {
			
			return null;
		}
	}
	
	
	/**
	 * 
	 * @param name name of item
	 * @return
	 */
	public ConnectionParams get(String name) {
		
		
		for (ListIterator<ConnectionParams> it = list.listIterator(); it.hasNext();) {
			
			ConnectionParams con = it.next();
			if (!con.getName().equals(name))
				continue;
			return con;
		}
		return null;
	}
	
	/**
	 * 
	 * @param con
	 */
	public void add(ConnectionParams con) {
		
		list.add(con);
	}
	
	/**
	 * 
	 * @param index
	 */
	public boolean delete(int index) {
		
		try {
		
			list.remove(index);
			return true;
		} catch (IndexOutOfBoundsException e) {
			
			return false;
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public int size() {
		
		return list.size();
	}
}

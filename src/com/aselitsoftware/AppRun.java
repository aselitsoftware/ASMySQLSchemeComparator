package com.aselitsoftware;

import com.aselitsoftware.BaseAppRun;

public class AppRun extends BaseAppRun {

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		
		addJarToClasspath(getArchFileName("lib/swt"));
		
		try {
			MainWindow window = new MainWindow();
			window.open();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}
}

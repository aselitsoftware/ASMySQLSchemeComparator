package com.aselitsoftware;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.TableTreeItem;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.custom.TableTree;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import static com.aselitsoftware.CompareResult.CompareResultObjTypeEnum;
import static com.aselitsoftware.CompareThread.CompareModeEnum;

import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.PaintEvent;

import com.aselitsoftware.mysql.ConnectionInstaller;
import com.aselitsoftware.mysql.ConnectionSetupDialog;

public class MainWindow implements CompareThread.CompareThreadInterface {

	protected Shell shell;
	private ConnectionParamsList connections;
	private Combo comboCompareMode;
	private Text textMasterDatabase;
	private Text textSlaveDatabase;
	private Combo comboConnections;
	private Button btnSaveConnection;
	private Button btnDeleteConnection;
	private CLabel lblCompareThreadState;
	private CLabel lblDstConnectionParams;
	private TableTree tableResults;
	private Button btnCompare;
	private Button btnMasterDatabase;
	private Button btnSlaveDatabase;
	private Button btnSwap;
	private Button btnExpandAll;
	private Button btnCollapseAll;
	private Button btnExecute;
	private MainWindow appWindow;
	private Image imgOk = null;
	private TableColumn tblclmnExecute;
	private Text textSQL;
	
	private CompareThread compareThread = null;
	
	private static final String OK_FILE_NAME = "ok_16x16.png";
	private static final Logger log = LogManager.getLogger(MainWindow.class);

	private String getVersion() {
		
		String version = "?";
		try {
			URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			Enumeration<URL> resources;
			resources = urlClassLoader.getResources("META-INF/MANIFEST.MF");
			while (resources.hasMoreElements()) {
			
				URL element = resources.nextElement();
				String path = element.getPath();
				
//					check that this is my manifest
				if (!path.contains("MySQLSchemeComparator"))
					continue;
				
//					log.info(String.format("Path: %s", (null != path) ? path : "is null"));
				
				Manifest manifest = new Manifest(element.openStream());
				Attributes attr = manifest.getMainAttributes();
				version = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
				break;
			}
		} catch (IOException ex) {
			
			log.error(ex);
		}
		return version;
		
//			Manifest manifest = new Manifest();
			
			/* first example
			Thread th = Thread.currentThread();
			if (null == th)
				throw new Exception("Thread is null.");
			ClassLoader cl = th.getContextClassLoader();
			if (null == cl)
				throw new Exception("ClassLoader is null.");
			InputStream is = cl.getResourceAsStream("META-INF/MANIFEST.MF");
			*/
			
			/* second example
			URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			InputStream is = urlClassLoader.getResourceAsStream("META-INF/MANIFEST.MF");
			*/
			
			
			/* read manifest
			if (null == is)
				throw new Exception("InputStream is null.");
			manifest.read(is);
			*/
		    
			/* show all manifest attributes
			Attributes attr = manifest.getMainAttributes();
			
			log.info(String.format("Attr size: %d", attr.size()));
			for (Map.Entry<Object,Object> entry : attr.entrySet()) {
				
				log.info(String.format("key: %s, value: %s", String.valueOf(entry.getKey()),
					String.valueOf(entry.getValue())));
			}
			*/		    

//			return attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
	}
	
	/**
	 * Open the window.
	 */
	public void open() {
		
		appWindow = this;
		
		Display display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
//		shell.pack();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Update all controls (buttons, lists, etc) state.
	 */
	private void updateControls() {
		
		ConnectionParams con = connections.get(comboConnections.getSelectionIndex());
		CompareModeEnum mode = (-1 == comboCompareMode.getSelectionIndex()) ? CompareModeEnum.cmNone : CompareModeEnum.fromInt(comboCompareMode.getSelectionIndex() + 1);
		
		boolean flag1 = (null != con);
		boolean flag2 = (null == compareThread);
		boolean flag3 = flag1 && flag2;
		
		btnSaveConnection.setEnabled(flag2);
		btnDeleteConnection.setEnabled(flag3);
		btnSwap.setEnabled(flag3);
		
		btnMasterDatabase.setEnabled(flag3 && (mode.equals(CompareModeEnum.cmCompareDatabases) ||
			mode.equals(CompareModeEnum.cmCreateMasterDump) || mode.equals(CompareModeEnum.cmCompareMasterDump)));
		textMasterDatabase.setEnabled(btnMasterDatabase.getEnabled());
		
		btnSlaveDatabase.setEnabled(flag3 && (mode.equals(CompareModeEnum.cmCompareDatabases) ||
			mode.equals(CompareModeEnum.cmCreateSlaveDump) || mode.equals(CompareModeEnum.cmCompareSlaveDump)));
		textSlaveDatabase.setEnabled(btnSlaveDatabase.getEnabled());
		
		btnCompare.setEnabled(flag1 && (CompareModeEnum.cmNone != mode));
		btnCompare.setText((flag2) ? "Run" : "Stop");
		
		btnExpandAll.setEnabled(tableResults.getItemCount() > 0);
		btnCollapseAll.setEnabled(btnExpandAll.getEnabled());
		
		btnExecute.setEnabled(flag2 && !textSQL.getText().isEmpty());
	}
	
	/**
	 * 
	 */
	private void clearResults() {
		
		tableResults.getTable().removeAll();
		textSQL.setText("");
		lblDstConnectionParams.setText("");
	}
	
	/**
	 * Show selected connection parameters.
	 */
	private void showSelectedConnectionParams() {
	
		ConnectionParams con = connections.get(comboConnections.getSelectionIndex());
		textMasterDatabase.setText((null == con) ? "" : con.getMasterDatabase());
		textSlaveDatabase.setText((null == con) ? "" : con.getSlaveDatabase());
	}
		
	
	/**
	 * Create contents of the window.
	 * @wbp.parser.entryPoint
	 */
	protected void createContents() {
		
		FormLayout layout = new FormLayout();
		
		shell = new Shell();
		shell.setSize(680, 600);
//		shell.setMaximized(true);
		
		String caption = String.format("MySQL Scheme Comparator %s", getVersion());
		shell.setText(caption);
		
		shell.setLayout(layout);
				
		imgOk = SWTResourceManager.getImage(MainWindow.class, OK_FILE_NAME);
		
		
		comboConnections = new Combo(shell, SWT.NONE);
//		comboConnections.setFont(SWTResourceManager.getFont("Segoe UI", 10, SWT.NORMAL));
		comboConnections.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				showSelectedConnectionParams();
				updateControls();
			}
		});
		
		btnSaveConnection = new Button(shell, SWT.NONE);
//		btnSaveConnection.setFont(SWTResourceManager.getFont("Segoe UI", 10, SWT.NORMAL));
		btnSaveConnection.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				String conName = comboConnections.getText();
				if (conName.isEmpty())
					return;
				ConnectionParams con = connections.get(conName);
				if (null == con) {
					
					connections.add(new ConnectionParams(conName, textMasterDatabase.getText(), textSlaveDatabase.getText()));
					comboConnections.add(conName);
					comboConnections.select(comboConnections.getItemCount() - 1);
				} else {
					
					con.setMasterDatabase(textMasterDatabase.getText());
					con.setSlaveDatabase(textSlaveDatabase.getText());
				}
				connections.save();
			}
		});
		btnSaveConnection.setText("Save");
		
		btnDeleteConnection = new Button(shell, SWT.NONE);
//		btnDeleteConnection.setFont(SWTResourceManager.getFont("Segoe UI", 10, SWT.NORMAL));
		btnDeleteConnection.setToolTipText("Delete selected connection");
		btnDeleteConnection.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			
				int index = comboConnections.getSelectionIndex();
				if (-1 == index)
					return;
				
				MessageBox mb = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
				mb.setText(shell.getText());
				mb.setMessage(String.format("Do you want to delete connection \"%s\"?", comboConnections.getText()));
				if (SWT.YES != mb.open())
					return;
				
				if (connections.delete(index)) {
					
					connections.save();
					
					comboConnections.remove(index);
					if (index < comboConnections.getItemCount()) {
						
						comboConnections.select(index);
					} else
						if (comboConnections.getItemCount() > 0)
							comboConnections.select(comboConnections.getItemCount() - 1);
						else
							comboConnections.setText("");
					showSelectedConnectionParams();
					updateControls();
				}
			}
		});
		btnDeleteConnection.setText("Delete");
		
		textMasterDatabase = new Text(shell, SWT.BORDER);
//		textMasterDatabase.setFont(SWTResourceManager.getFont("Segoe UI", 10, SWT.NORMAL));
		
		btnMasterDatabase = new Button(shell, SWT.NONE);
//		btnMasterDatabase.setFont(SWTResourceManager.getFont("Segoe UI", 10, SWT.NORMAL));
		btnMasterDatabase.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			
				ConnectionParams con = connections.get(comboConnections.getSelectionIndex());
				if (null == con)
					return;
				String setupCaption = String.format("Setup %s for connection \"%s\"", CompareThread.MASTER_DATABASE_NAME, con.getName());
				ConnectionSetupDialog setup = new ConnectionSetupDialog(shell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL, setupCaption);
				String result = setup.open(textMasterDatabase.getText());
				if (null != result)
					textMasterDatabase.setText(result);
				setup = null;
			}
		});
		btnMasterDatabase.setText(CompareThread.MASTER_DATABASE_NAME);
		
		textSlaveDatabase = new Text(shell, SWT.BORDER);
//		textSlaveDatabase.setFont(SWTResourceManager.getFont("Segoe UI", 10, SWT.NORMAL));
		
		btnSlaveDatabase = new Button(shell, SWT.NONE);
//		btnSlaveDatabase.setFont(SWTResourceManager.getFont("Segoe UI", 10, SWT.NORMAL));
		btnSlaveDatabase.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				ConnectionParams con = connections.get(comboConnections.getSelectionIndex());
				if (null == con)
					return;
				String setupCaption = String.format("Setup %s for connection \"%s\"", CompareThread.SLAVE_DATABASE_NAME, con.getName());
				ConnectionSetupDialog setup = new ConnectionSetupDialog(shell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL, setupCaption);
				String result = setup.open(textSlaveDatabase.getText());
				if (null != result)
					textSlaveDatabase.setText(result);
				setup = null;
			}
		});
		btnSlaveDatabase.setText(CompareThread.SLAVE_DATABASE_NAME);
		
		btnSwap = new Button(shell, SWT.NONE);
//		btnSwap.setFont(SWTResourceManager.getFont("Segoe UI", 10, SWT.NORMAL));
		btnSwap.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				ConnectionParams con = connections.get(comboConnections.getSelectionIndex());
				if (null == con)
					return;
				String database = textMasterDatabase.getText();
				textMasterDatabase.setText(textSlaveDatabase.getText());
				textSlaveDatabase.setText(database);
			}
		});
		btnSwap.setToolTipText("Swap schemes");
		btnSwap.setText("Swap");
		
		comboCompareMode = new Combo(shell, SWT.DROP_DOWN | SWT.READ_ONLY);
//		comboCompareMode.setFont(SWTResourceManager.getFont("Segoe UI", 10, SWT.NORMAL));
		comboCompareMode.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			
				updateControls();
			}
		});
		
		comboCompareMode.add("Compare databases");
		comboCompareMode.add("Create dump of ".concat(CompareThread.MASTER_DATABASE_NAME));
		comboCompareMode.add("Create dump of ".concat(CompareThread.SLAVE_DATABASE_NAME));
		comboCompareMode.add("Compare ".concat(CompareThread.MASTER_DATABASE_NAME).concat(" with dump"));
		comboCompareMode.add("Compare ".concat(CompareThread.SLAVE_DATABASE_NAME).concat(" with dump"));
		
		btnCompare = new Button(shell, SWT.NONE);
//		btnCompare.setFont(SWTResourceManager.getFont("Segoe UI", 10, SWT.NORMAL));
		btnCompare.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			
				ConnectionParams con = connections.get(comboConnections.getSelectionIndex());
				if (null == con)
					return;
				if (-1 == comboCompareMode.getSelectionIndex())
					return;
				CompareModeEnum mode = CompareModeEnum.fromInt(comboCompareMode.getSelectionIndex() + 1);
				
				if (null == compareThread) {
				
					compareThread = new CompareThread(con, mode, appWindow);
					compareThread.start();
				} else
					compareThread.interrupt();
				
				updateControls();
			}
		});
		btnCompare.setText("Run");
		
		lblCompareThreadState = new CLabel(shell, SWT.NONE);
		lblCompareThreadState.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		lblCompareThreadState.setText("");
		
		lblDstConnectionParams = new CLabel(shell, SWT.NONE);
		lblDstConnectionParams.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		lblDstConnectionParams.setText("");
		
//		result table
		tableResults = new TableTree(shell, SWT.BORDER | SWT.FULL_SELECTION);
		tableResults.getTable().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			
				TableTreeItem[] items = tableResults.getSelection();
				if (items.length > 0) {
					
					Object data = items[0].getData("sql");
					textSQL.setText((null != data) ? (String) data : "");
				} else
					textSQL.setText("");
				updateControls();
			}
		});
		
		/**
		tableResults.getTable().addListener(SWT.MeasureItem, new Listener() {
			   public void handleEvent(Event event) {
			      // height cannot be per row so simply set
			      event.height = 28;
			   }
			});
		*/
		
		tableResults.getTable().setLinesVisible(true);
		
		tblclmnExecute = new TableColumn(tableResults.getTable(), SWT.NONE);
		tblclmnExecute.setWidth(45);
		
		TableColumn tblclmnData = new TableColumn(tableResults.getTable(), SWT.NONE);
		tblclmnData.setWidth(400);
		
		
		textSQL = new Text(shell, SWT.BORDER | SWT.READ_ONLY | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL);
		
		btnExecute = new Button(shell, SWT.NONE);
		btnExecute.addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("deprecation")
			@Override
			public void widgetSelected(SelectionEvent arg0) {
			
				ConnectionInstaller con = null;
				PreparedStatement st = null;
				
				try {
					try {
						con = new ConnectionInstaller((String) lblDstConnectionParams.getData("connectionParams"));
						
//						check query
						TableTreeItem[] items = tableResults.getSelection();
						Object data = (0 == items.length) ? null : items[0].getData("sql"); 
						if (null == data)
							return;
							
						MessageBox mb = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
						mb.setText(shell.getText());
						String mes = (null == items[0].getImage(1)) ? "Execute current SQL query?" : "Query for this table was executed already. Execute current SQL query anyway?"; 
						mb.setMessage(mes);
						if (SWT.YES != mb.open())
							return;
						
						if (!con.connect())
							throw new Exception("Error occurred while connect to database.");
						
						try {
							
							st = con.prepareSQL((String) data);
//							st.executeUpdate();
						} catch (SQLException ex) {
							
							log.error("Threw a SQLException in MainWindow::execute():", ex);
							throw new Exception("Error occurred while executing the query.");
						}
						
//						mark nodes as executed
						TableTreeItem parent = items[0].getParentItem();
						if (null == items[0].getImage(1))
							items[0].setImage(1, imgOk);
						if (null == parent) {
							
							items = items[0].getItems();
							for (TableTreeItem item : items) {
								
								if (null == item.getImage(1))
									item.setImage(1, imgOk);
							}
						} else {
							items = parent.getItems();
							int j = -1;
							while (++j < items.length) {
							
								if (null == items[j].getImage(1))
									break;
							}	
							if ((items.length == j) && (null == parent.getImage(1)))
								parent.setImage(1, imgOk);
						}
					} catch (Exception ex) {
					
						showCompareThreadError(ex.getMessage());
//						System.out.println(ex.getMessage());
					}
				} finally {
						try {
							if ((null != con) && con.isConnected())
								con.close();
							
							if (null != st)
								st.close();
						} catch (SQLException ex) {
						
							log.error("Threw a SQLException in MainWindow::execute():", ex);
						}
				}
			}
		});
		btnExecute.setText("Execute");
		
		btnExpandAll = new Button(shell, SWT.NONE);
		btnExpandAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			
				TableTreeItem[] items = tableResults.getItems();
				if (null == items)
					return;
				for (TableTreeItem item : items) {
					
					if (item.getExpanded())
						continue;
					item.setExpanded(true);
				}
			}
		});
		btnExpandAll.setText("Expand all");
		
		btnCollapseAll = new Button(shell, SWT.NONE);
		btnCollapseAll.addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("deprecation")
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				TableTreeItem[] items = tableResults.getItems();
				if (null == items)
					return;
				for (TableTreeItem item : items) {
					
					if (!item.getExpanded())
						continue;
					item.setExpanded(false);
				}
			}
		});
		btnCollapseAll.setText("Collapse all");
		
		FormData fd;
		
		fd = new FormData();
//		fd.height = 26;
		fd.width = 60;
		fd.right = new FormAttachment(100, -6);
		fd.top = new FormAttachment(0, 6);
		fd.bottom = new FormAttachment(comboConnections, 0, SWT.BOTTOM);
		btnDeleteConnection.setLayoutData(fd);
		
//		attach(btnDeleteConnection).withHeight(23).withWidth(46).atRight(6).atTop(6);
		
		fd = new FormData();
//		fd.height = 26;
		fd.width = 60;
//		fd.top = new FormAttachment(btnDeleteConnection, 0, SWT.TOP);
		fd.top = new FormAttachment(0, 6);
		fd.right = new FormAttachment(btnDeleteConnection, -6);
		fd.bottom = new FormAttachment(comboConnections, 0, SWT.BOTTOM);
		btnSaveConnection.setLayoutData(fd);

		fd = new FormData();
		fd.right = new FormAttachment(btnSaveConnection, -6);
//		fd.top = new FormAttachment(btnSaveConnection, 0, SWT.TOP);
		fd.top = new FormAttachment(0, 6);
		fd.left = new FormAttachment(0, 6);
		comboConnections.setLayoutData(fd);
		
		fd = new FormData();
		fd.width = 126;
//		fd.height = 26;
		fd.top = new FormAttachment(comboConnections, 6);
		fd.right = new FormAttachment(100, -6);
		fd.bottom = new FormAttachment(textMasterDatabase, 0, SWT.BOTTOM);
		btnMasterDatabase.setLayoutData(fd);
				
		fd = new FormData();
//		fd.height = 56;
		fd.width = 60;
		fd.right = new FormAttachment(btnMasterDatabase, -6);
		fd.top = new FormAttachment(btnMasterDatabase, 0, SWT.TOP);
		fd.bottom = new FormAttachment(textSlaveDatabase, 0, SWT.BOTTOM);
		btnSwap.setLayoutData(fd);
		
		fd = new FormData();
		fd.left = new FormAttachment(0, 6);
//		fd.top = new FormAttachment(btnMasterDatabase, 1, SWT.TOP);
		fd.top = new FormAttachment(comboConnections, 6);
		fd.right = new FormAttachment(btnSwap, -6);
		textMasterDatabase.setLayoutData(fd);
		
		fd = new FormData();
		fd.left = new FormAttachment(0, 6);
		fd.top = new FormAttachment(textMasterDatabase, 6);
		fd.right = new FormAttachment(btnSwap, -6);
		textSlaveDatabase.setLayoutData(fd);
		
		fd = new FormData();
		fd.width = 126;
//		fd.height = 26;
		fd.top = new FormAttachment(textMasterDatabase, 6);
		fd.right = new FormAttachment(100, -6);
		fd.bottom = new FormAttachment(textSlaveDatabase, 0, SWT.BOTTOM);
		btnSlaveDatabase.setLayoutData(fd);
		
		fd = new FormData();
		fd.width = 126;
//		fd.height = 26;
		fd.right = new FormAttachment(100, -6);
		fd.top = new FormAttachment(textSlaveDatabase, 6);
		fd.bottom = new FormAttachment(comboCompareMode, 0, SWT.BOTTOM);
		btnCompare.setLayoutData(fd);
		
		fd = new FormData();
		fd.left = new FormAttachment(0, 6);
		fd.right = new FormAttachment(btnCompare, -6);
		fd.top = new FormAttachment(textSlaveDatabase, 6);
		comboCompareMode.setLayoutData(fd);
		
		fd = new FormData();
		fd.left = new FormAttachment(0, 6);
		fd.right = new FormAttachment(100, -6);
		fd.top = new FormAttachment(comboCompareMode, 6);
		lblCompareThreadState.setLayoutData(fd);
		
		fd = new FormData();
		fd.width = 126;
		fd.height = 25;
		fd.right = new FormAttachment(100, -6);
		fd.top = new FormAttachment(lblCompareThreadState, 6);
		btnExpandAll.setLayoutData(fd);
		
		fd = new FormData();
		fd.width = 126;
		fd.height = 25;
		fd.right = new FormAttachment(100, -6);
		fd.top = new FormAttachment(btnExpandAll, 6);
		btnCollapseAll.setLayoutData(fd);
		
		fd = new FormData();
		fd.height = 270;
		fd.left = new FormAttachment(0, 6);
		fd.right = new FormAttachment(btnExpandAll, -6);
		fd.top = new FormAttachment(btnExpandAll, 0, SWT.TOP);
		tableResults.setLayoutData(fd);
		
		fd = new FormData();
		fd.width = 126;
		fd.height = 25;
		fd.right = new FormAttachment(100, -6);
		fd.top = new FormAttachment(tableResults, 6);
		btnExecute.setLayoutData(fd);
		
		fd = new FormData();
		fd.left = new FormAttachment(0, 6);
		fd.right = new FormAttachment(100, -6);
		fd.bottom = new FormAttachment(100, -6);
		lblDstConnectionParams.setLayoutData(fd);
		
		fd = new FormData();
		fd.left = new FormAttachment(0, 6);
		fd.right = new FormAttachment(btnExecute, -6);
		fd.top = new FormAttachment(btnExecute, 0, SWT.TOP);
		fd.bottom = new FormAttachment(lblDstConnectionParams, -6);
		textSQL.setLayoutData(fd);
		
		connections = new ConnectionParamsList();
		for (int i = 0; i < connections.size(); i++) {
			
			ConnectionParams con = connections.get(i);
			if (null == con)
				continue;
			comboConnections.add(con.getName());
		}
		
		updateControls();
	}

	@Override
	public void showCompareThreadState(String value) {
		
		Display.getDefault().asyncExec(new Runnable() {
		    
			public void run() {
				
				lblCompareThreadState.setText(value);
				lblCompareThreadState.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));				
		    }
		});
	}

	@Override
	public void showCompareThreadError(String value) {

		compareThread = null;
		
		Display.getDefault().asyncExec(new Runnable() {
		    
			public void run() {
				
				lblCompareThreadState.setText(value);
				lblCompareThreadState.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
				
				clearResults();
				
				updateControls();
		    }
		});
	}

	@Override
	public void showCompareThreadResults(CompareModeEnum mode, List<CompareResult> results, int totalTableCount,
		String dstConnectionParams) {

		compareThread = null;
		
		Display.getDefault().asyncExec(new Runnable() {
		    
			public void run() {
				
				String masterDBCaption;
				int totalDistinctions = 0;
				int schemeIssues = 0;
				int tablesWithDistinctions = 0;
				
				if (mode.equals(CompareModeEnum.cmCompareMasterDump) ||
					mode.equals(CompareModeEnum.cmCompareSlaveDump))
					masterDBCaption = "Dump";
				else
					if (mode.equals(CompareModeEnum.cmCompareDatabases) ||
						mode.equals(CompareModeEnum.cmCreateMasterDump))
						masterDBCaption = CompareThread.MASTER_DATABASE_NAME;
					else
						masterDBCaption = CompareThread.SLAVE_DATABASE_NAME;
			    	    
				clearResults();
				
				lblDstConnectionParams.setText((dstConnectionParams.length() > 0) ? "Destination database: ".concat(dstConnectionParams) : "");
				lblDstConnectionParams.setData("connectionParams", dstConnectionParams);
				
				if (mode.equals(CompareModeEnum.cmCompareDatabases) ||
					mode.equals(CompareModeEnum.cmCompareMasterDump) ||
					mode.equals(CompareModeEnum.cmCompareSlaveDump)) {
					
					for (Iterator iterator = results.iterator(); iterator.hasNext();) {
						
						CompareResult res = (CompareResult) iterator.next();
						if (res.getProblemCount() > 0) {
							
							TableTreeItem item0 = new TableTreeItem(tableResults, SWT.NONE);
							
							item0.setText(1, res.getObjName());
							item0.setData("sql", res.getSQL());
							
							FontData[] fd = item0.getFont().getFontData();
							fd[0].setStyle(SWT.BOLD);
							item0.setFont(new Font(Display.getDefault(), fd));
							
							for (int i = 0; i < res.getProblemCount(); i++) {
								
								ProblemInfo problem = res.getProblem(i);
								TableTreeItem item1 = new TableTreeItem(item0, SWT.NONE);
								item1.setText(1, problem.getText());
								item1.setData("sql", problem.getSQL());
								
								if (problem.getHint().length() > 0) {
									
									Pattern pattern = Pattern.compile("^(.+?)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
									Matcher matcher = pattern.matcher(problem.getHint());
									while (matcher.find()) {
									
										TableTreeItem item2 = new TableTreeItem(item1, SWT.NONE);
										item2.setText(1, matcher.group(1));
									}
								}
								item1.setExpanded(false);
							}
							item0.setExpanded(true);
							
							switch (res.getObjType()) {
							case crotScheme:
								schemeIssues++;
								break;
							case crotTable:
								tablesWithDistinctions++;
								break;
							}
							totalDistinctions += res.getProblemCount();
						}
					}
				}
				
				if (totalTableCount > 0) {
					
					String s = String.format("Compare done.\r\n- %d table%s at %s scheme;", totalTableCount,
						(totalTableCount > 1) ? "s" : "", masterDBCaption);
					if (0 == (schemeIssues + tablesWithDistinctions))
						s = s.concat("\r\n- no differents found.");
					else {
						if (schemeIssues > 0)
							s = s.concat(String.format("\r\n- %d scheme issue%s;", schemeIssues,
								(schemeIssues > 1) ? "s" : ""));
						if (tablesWithDistinctions > 0)
							s = s.concat(String.format("\r\n- %d table%s with distinctions;", tablesWithDistinctions,
								(tablesWithDistinctions > 1) ? "s" : ""));
						s = s.concat(String.format("\r\n- %d distinction%s in total.", totalDistinctions,
							(totalDistinctions > 1) ? "s" : ""));
					}
					MessageBox mb = new MessageBox(shell, SWT.ICON_INFORMATION);
					mb.setText(shell.getText());
					mb.setMessage(s);
					mb.open();
				}
				
				updateControls();
		    }
		});
	}
}

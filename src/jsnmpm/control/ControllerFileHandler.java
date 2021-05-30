package jsnmpm.control;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class ControllerFileHandler {

	
	// PUBLIC CONSTANTS
	public final String indentation = "                  ---->";
	// PRIVATE CONSTANTS
	private final static String SNMP_LOG_FILENAME = "snmp_log.txt"; 
	private final static String CTRL_LOG_FILENAME =  "ctrl_log.txt";
	
	private final static String SNMP_CONF_FILENAME = "jsnmp.conf";
	private final static String DEF_CONF_FILENAME = "defjsnmp.conf"; 

	
	
	private String execDir = null;
	private String ctrlLogFile = null;
	private String snmpLogFile = null;
	private String defConfFile = null;
	private String snmpConfFile = null;
	
	private Properties prop = null;

	
	
	public ControllerFileHandler() throws FileNotFoundException {
		
		this.execDir = FileSystems.getDefault().getPath("").toAbsolutePath().toString() + "\\";
		this.ctrlLogFile = this.execDir + CTRL_LOG_FILENAME;
		this.snmpLogFile = this.execDir + SNMP_LOG_FILENAME;
		this.snmpConfFile = this.execDir + SNMP_CONF_FILENAME;
		this.defConfFile = this.execDir + DEF_CONF_FILENAME;
	}
	
	// ········ GETTERS
	public Path getCtrlLogFilePath() {
		return Paths.get(this.ctrlLogFile);
	}
	
	public Path getSNMPLogFilePath() {
		return Paths.get(this.snmpLogFile);
	}
	
	public Path getConfFilePath() {
		return Paths.get(this.snmpConfFile);
	}
	
	public Path getDefConfFilePath() {
		return Paths.get(this.defConfFile);
	}
	
	
	// #################    C L A S S   I N I T I A L I Z E R   ####################
	/**
	 * Initialices this object for reading configuration files and writing to logfiles.
	 * Throws a FileNotFoundException if there is no configuration file available.
	 * 
	 * @throws FileNotFoundException
	 */
	protected void init() throws FileNotFoundException {
		// ············· CHECK IF FILES EXISTS
		// CTRL_LOG_FILE
		if(!this.fileExists(this.getCtrlLogFilePath()))
			this.createFile(this.getCtrlLogFilePath());
		
		try {
			Files.write(this.getCtrlLogFilePath(), 
					"###############################   LOGFILE FOR JSNMPMONITOR SNMP-CONTROLLER   ############################\n".getBytes(),
					StandardOpenOption.APPEND);
			this.writeEmptyLineToLogFile(this.getCtrlLogFilePath());
			
		} catch (IOException e) {
			//TODO
		}
		
		// SNMP_LOG_FILE
		if(!this.fileExists(this.getSNMPLogFilePath()))
			this.createFile(this.getSNMPLogFilePath());
		
		try {
			Files.write(this.getSNMPLogFilePath(), 
					"###############################   LOGFILE FOR JSNMPMONITOR SNMP-MANAGER   ############################\n".getBytes(),
					StandardOpenOption.APPEND);
			this.writeEmptyLineToLogFile(this.getSNMPLogFilePath());
			
		} catch (IOException e) {
			//TODO
		}
		
		// SNMP_CONF_FILE
		if(!this.fileExists(this.getConfFilePath())) {
			this.writeToLogFile(this.getCtrlLogFilePath(), "ERROR: Unable to find configuration file. Path: "+this.snmpConfFile);
			this.writeToLogFile(this.getCtrlLogFilePath(), "-----------> Starting with default configuration");
			throw new FileNotFoundException("ERROR: No available configuration filed");
			
				
		}else {
			this.prop = new Properties();
			try {
				this.prop.load(new FileInputStream(this.snmpConfFile));
			} catch (IOException e) {
				this.writeToLogFile(this.getCtrlLogFilePath(), "ERROR: Unable to read from configuration file. Path: "+this.snmpConfFile);
				throw new FileNotFoundException("ERROR: No available configuration filed");
			}
			
			this.writeToLogFile(this.getCtrlLogFilePath(), "Found configuration file. Path: "+this.snmpConfFile);
		}	
	}
	
	// ######################   L O G F I L E S    H A N D L I N G    M E T H O D S   ###############################
	private boolean fileExists(Path file) {
		return Files.exists(file);
	}
	
	private boolean createFile(Path file) {
		try {
			Files.createFile(file);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	public boolean writeToLogFile(Path file, String data) {
		byte[] entry = String.format("[%s]  %s\n", this.getCurrentTime(), data).getBytes();
		
		try {
			Files.write(file, entry, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	public void writeEmptyLineToLogFile(Path file) {
		try {
			Files.write(file, "\n".getBytes(), StandardOpenOption.WRITE, StandardOpenOption.APPEND);
			
		} catch (IOException e) {
			
		}
	}
	
	
	// ####################  R E A D   C O N F I G U R A T I O N   F I L E S   P R O P E R T I E S  #####################
	
	
	public String getConfProperty(String property) {
		if(this.prop != null) {
			return prop.getProperty(property);
		}else {
			this.writeToLogFile(this.getCtrlLogFilePath(), "ERROR: Cannot access property. No loaded file.");
			return null;
		}
	}
	
	
	
	// ####################   R A N D O M    U T I L I T I E S    ########################
	private String getCurrentTime() {
		return DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").format(LocalDateTime.now());
	}
	
	
	
}

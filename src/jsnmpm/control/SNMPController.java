package jsnmpm.control;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.net.UnknownHostException;
import java.sql.SQLException;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Flow.Processor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;


import org.snmp4j.PDU;

import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.VariableBinding;



import jsnmpm.control.process.SNMPProcess;
import jsnmpm.control.remoting.RemoteControl;
import jsnmpm.control.snmp.SNMPAgent;
import jsnmpm.control.snmp.SNMPManager;
import jsnmpm.control.utilities.Rule;
import jsnmpm.control.utilities.JSNMPUtil;
import jsnmpm.control.utilities.JSNMPUtil.INFO;
import jsnmpm.control.utilities.MailSender;
import jsnmpm.control.whisper.ProcessWhisper;
import jsnmpm.control.whisper.RequestWhisper;
import jsnmpm.control.whisper.TrapWhisper;
import jsnmpm.control.whisper.Whisper;
import jsnmpm.monitor.gui.AbstractMonitor;
import jsnmpm.monitor.terminal.TMonitor;

/**
 * Main class for controlling the program logic. Has method for handling both database and snmpmanager.<br>
 * Instanciates:<br/>
 * · SNMPManager <br/>
 * · DBController <br/>
 * 
 * @author MrStonedDog
 *
 */
public class SNMPController implements Processor<Whisper, Whisper>{
	
	// CONTROLER RESOURCES
	private SNMPManager snmpManager = null;
	private DBControl dbCtrl = null;
	private ControllerFileHandler ctrlFileHandler = null;
	
	// USER CONTROL / UI / GUI
	private AbstractMonitor monitor = null;
	private RemoteControl rCtrl = null;
	
	// CONTROLER VARIABLES
	private int agentIDCounter = 0;
	private boolean defConfiguration;
	
	// ############    CONSTRUCTOR    ###########
	/**
	 * Throws a FileNotFoundException if no configuration file is found.
	 * @param monitor
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	
	public SNMPController() {
		
		try {
			this.ctrlFileHandler = new ControllerFileHandler();
			this.ctrlFileHandler.init();
			this.defConfiguration = false;
		} catch (FileNotFoundException e) {
			this.defConfiguration = true;
			System.out.println("ERROR: No  configuration file found. Executing default system  configuration.\n"
					+ "Try to fix the configuration file!");
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e1) {
			}
		}
	}
	
	
	public void configSocket(int port, String ip) {
		
		this.rCtrl = new RemoteControl(ip,65432, this);
		new Thread(this.rCtrl).start();
	}
	
	public void configSocket() {
		this.rCtrl = new RemoteControl(this);
		new Thread(this.rCtrl).start();
	}
	
	public void configGUI(AbstractMonitor monitor) {
		this.monitor = monitor;
		new Thread(this.monitor).start();
	}
	
	public void configMonitor(AbstractMonitor monitor) {
			
		this.monitor = monitor;
		new Thread(this.monitor).start();
	}
	
	// ################  I N I T   P R O C E D U R E  ##################
		public void start() {
			
			this.ctrlFileHandler.writeEmptyLineToLogFile(this.ctrlFileHandler.getCtrlLogFilePath());
			// INITIALIZING SNMPMANAGER
			if(!this.defConfiguration) {
				try {
					String transports  = this.ctrlFileHandler.getConfProperty("listen");
					if(!(transports.isEmpty() || transports == null)) {
						String[]trans = transports.split(";");
						this.snmpManager = new SNMPManager(this, trans);
					}else {
						this.snmpManager = new SNMPManager(this, null);
					}
				
				} catch (IOException e) {
					this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "ERROR: Cannot load SNMPManager - Cause:" + e.getMessage());
					System.out.println("ERROR: \n Cause: " + e.getCause() + "\nMessage: " +  e.getMessage());
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e1) {}
					System.exit(1);
				}
			
			
				// LOAD DATABASE CONFIGURATION
				try {
				
					if(this.ctrlFileHandler.getConfProperty("setDatabase").equals("true")) {
		    	
							this.setDatabase(this.ctrlFileHandler.getConfProperty("dbname"),
							this.ctrlFileHandler.getConfProperty("host"),
							Integer.parseInt(this.ctrlFileHandler.getConfProperty("port")),
							this.ctrlFileHandler.getConfProperty("user"),
							this.ctrlFileHandler.getConfProperty("pass"));
			
							this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(),
									"INFO: Database configuration established successfully");
							this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(),
									(this.dbCtrl.checkConnection()) ? "INFO: Connection with database established!" : "WARNING: Cannot establish connection with database");
			
					}else {
						this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(),
								"WARNING! Database usage is disabled.");
						this.dbCtrl = null;
					}
			
				} catch(NumberFormatException nfe) {
					this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "ERROR: Wrong format por property 'PORT' in configuration file");
				
				}
					
				// LOADING AGENTS TO SNMPMANAGER
				try {
					if(this.getDataBaseStatus()) {
						this.loadAgents();
						this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(),
							"INFO: Success loading database agents to SNMPManager");
					}
				} catch (SQLException e) {
					this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "ERROR: Cannot load agents from database\n"+
													this.ctrlFileHandler.indentation + "Cause: "+ e.getMessage().split("\n")[0] + "\n" +
													this.ctrlFileHandler.indentation + "ErrorCode: " + e.getErrorCode());
				}
			
				// LOADING AGENTS TO SNMPMANAGER
				try {
					if(this.getDataBaseStatus()) {
						this.loadProcesses();
						this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(),
								"INFO: Success loading database processes to SNMPManager");
					}
				} catch (SQLException e) {
					this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "ERROR: Cannot load processes from database\n"+
															this.ctrlFileHandler.indentation + "Cause: "+ e.getMessage().split("\n")[0] + "\n" +
															this.ctrlFileHandler.indentation + "ErrorCode: " + e.getErrorCode());
				}
			
				// LOADING CONFIGURATION TO MONITOR
				this.ctrlFileHandler.writeEmptyLineToLogFile(this.ctrlFileHandler.getCtrlLogFilePath());
				this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "INFO: Display Information loaded successfully");
			
				if(this.ctrlFileHandler.getConfProperty("mail_enable").equals("true")) {
					//this.mailSender = new MailSender();
					if(this.snmpManager.setMailSenderForTrapHandler(this.ctrlFileHandler.getConfProperty("smtp_server"),
							this.ctrlFileHandler.getConfProperty("smtp_port"),
							this.ctrlFileHandler.getConfProperty("mail_account"),
							this.ctrlFileHandler.getConfProperty("mail_pass")))
						this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(),"INFO: Mailing enabled!");
					else
						this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(),"ERROR: Error loading mail. Check parameters are written correctly in configuration file.");
					
					/*this.mailSender.setSMTPHost(this.ctrlFileHandler.getConfProperty("smtp_server"));
					if(this.mailSender.getSMTPHost().isEmpty() || this.mailSender.getSMTPHost() == null)
						this.writeToCTRLLogFile("WARNING: SMTP Server address is empty or null");
					try {
						this.mailSender.setSMTPPort(Integer.parseInt(this.ctrlFileHandler.getConfProperty("smtp_port")));
					}catch(NumberFormatException nfe) {
						this.writeToCTRLLogFile("WARNING: SMTP Server port has wrong format. Configuring default 465");
						this.mailSender.setSMTPPort(465);
					}
					this.mailSender.setMailAccount(this.ctrlFileHandler.getConfProperty("mail_account"));
					if(this.mailSender.getMailAccount().isEmpty() || this.mailSender.getMailAccount() == null)
						this.writeToCTRLLogFile("WARNING: Mail Account is empty or null");
					
					this.mailSender.setMailPass(this.ctrlFileHandler.getConfProperty("mail_pass"));
					if(this.mailSender.getMailPass().isEmpty()|| this.mailSender.getMailPass() == null)
						this.writeToCTRLLogFile("WARNING: Mail Password is empty or null");
					
					this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(),"INFO: Mailing enabled!");*/
				}else {
					this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(),"INFO: Mailing is not enabled! Configuration wont be loaded.");
				}
				
				// LOADING TRAP HANDLER RULES
				this.loadRules();
				
				// TODO LOAD DEFAULT COLORS FOR TERMINAL???
			
				// TODO LOAD DEFAULT PARAMETERS FOR A GUI IMPLEMENTATION???
				
			}else {
				this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "WARNING: CONFIGURATION FILE NOT FOUND; EXECUTING WITH DEFAULT SYSTEM"
						+ "CONFIGURATION");
				try {
					this.snmpManager = new SNMPManager(this, null);
				} catch (IOException e) {
					this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "ERROR: Cannot load SNMPManager - Cause:" + e.getMessage());
					System.out.println("ERROR: \n Cause: " + e.getCause() + "\nMessage: " +  e.getMessage());
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e1) {}
					System.exit(1);
				}
				
			}
			this.chill();
		}
	
	private synchronized void chill() {
		while(true) {
			try {
				//System.out.println("WAITING");
				this.wait();
			} catch (InterruptedException e) {
				System.exit(0);
				break;
			}
		}
		
	}
	
	// GETTERS
	public Map<INFO, String> getFullCtrlInfo(){
		
		Map<INFO, String> ctrlInfo = new HashMap<INFO, String>();
		ctrlInfo.put(INFO.INTERFACES, this.snmpManager.getTransportMappingInfo());
		ctrlInfo.put(INFO.AGENTS, String.valueOf(this.snmpManager.getAgents().size()));
		ctrlInfo.put(INFO.DBACCESS,(this.dbCtrl != null && this.dbCtrl.hasConnection()) ? "True" : "False");
		ctrlInfo.put(INFO.TRAPS, String.valueOf(this.snmpManager.getTotalTraps()));
		ctrlInfo.put(INFO.PROCESSES, String.valueOf(this.snmpManager.getNumberProcesses(1)));
		return ctrlInfo;
	}
	
	public String getTransportMappingInfo(){
		return this.snmpManager.getTransportMappingInfo();
	}
	
	public int getAgentsNumber(){
		return this.snmpManager.getAgents().size();
	}
	
	public boolean getDataBaseStatus(){
		
		return (this.dbCtrl != null && this.dbCtrl.hasConnection());
	}
	
	public int getInMemoryTrapsNumber(){
		return this.snmpManager.getTotalTraps();
	}
	
	public int getTotalRunningProcesses(){
		return this.snmpManager.getNumberProcesses(1);
	}
	
	// SETTERS
	/**
	 * Sets database with given parameters. Returns an int depending on the result.<br/>
	 * @param dbName
	 * @param dbIP
	 * @param dbPort
	 * @param dbUser
	 * @param dbPass
	 * @return 0 = Good <br/> 1 = No Connection <br/> 2 = Cannot load driver </br> 3 = Unknown SQLException
	 */
	private boolean setDatabase(String dbName, String dbIP, int dbPort, String dbUser, String dbPass) {
		try {
			this.dbCtrl = new DBControl(dbName,dbIP,dbPort,dbUser,dbPass);
			
			return (this.dbCtrl.checkConnection());
		} catch (ClassNotFoundException e) {
			this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "ERROR: Unable to load Database handler");
			
		} catch (SQLException e) {
			this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "ERROR: Database query returned an unknown SQLException");
		}
		return false;
	}
	
	
	
	
	
	
	/**
	 * If database is set, retrieves all SNMPAgents from DB, and sets local @variable agentIDCounter to
	 * the max(agentID)+1.
	 * @throws SQLException
	 */
	public void loadAgents() throws SQLException {
		if(this.dbCtrl != null) {
			this.snmpManager.addAllAgents(this.dbCtrl.getSNMPAgents());
			this.agentIDCounter = this.snmpManager.getAgents().stream().mapToInt(SNMPAgent::getId).max().orElse(0);
			++this.agentIDCounter;
		}
	}
	
	public void loadProcesses() throws SQLException {
		if(this.dbCtrl != null) 
			this.snmpManager.addAllProcess(this.dbCtrl.getAllProcesses());
	}
	
	public void loadRules() {
		if(this.getDataBaseStatus()) {
			try {
				this.snmpManager.addAllTrapHandlerRules(this.dbCtrl.getTHRules());
				this.writeToCTRLLogFile("INFO: TrapHandler Rules loaded successufully!!");
			} catch (SQLException e) {
				this.writeToCTRLLogFile("ERROR: Could not load TrapHandler Rules!!");
			}
		}
	}
	
	// ############################  L O G   H A N D L I N G  ###########################
	public void writeToSNMPLogFile(String data) {
		this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getSNMPLogFilePath(),data);
	}
	
	public void writeEmptyLineToSNMPLogFile() {
		this.ctrlFileHandler.writeEmptyLineToLogFile(this.ctrlFileHandler.getSNMPLogFilePath());
	}
	
	public void writeToCTRLLogFile(String data) {
		this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(),data);
	}
	
	public void writeEmptyLineToCTRLLogFile() {
		this.ctrlFileHandler.writeEmptyLineToLogFile(this.ctrlFileHandler.getCtrlLogFilePath());
	}
	
	// ############################  H A N D L I N G   S N M P   A G E N T S  ###########################
	
	// ---> AFFECTS BOTH SNMPMANAGER AND DBCONTROLLER
	public synchronized int addNewAgent(String ip, int port, String name, String readCom) throws UnknownHostException, SQLException {
		SNMPAgent agent = new SNMPAgent(ip, port, name, readCom);
		agent.setId(this.agentIDCounter);
		this.snmpManager.addAgent(agent);
		
		if(this.dbCtrl != null && this.dbCtrl.hasConnection())
			dbCtrl.addSNMPAgent(agent);
		
		return this.agentIDCounter++;
	}
	
	public boolean updateAgent(int agentID, String name, String ip, int port, String readCom) {
		try {
			this.snmpManager.getAgent(agentID).setName(name);
			this.snmpManager.getAgent(agentID).setIP(ip);
			this.snmpManager.getAgent(agentID).setPort(port);
			this.snmpManager.getAgent(agentID).setReadCommunity(readCom);
			if(this.dbCtrl != null && this.dbCtrl.hasConnection())
				this.dbCtrl.updateAgent(agentID, name, ip, port, readCom);

			this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "INFO: Agent " + agentID + "was updated.");
		} catch (SQLException e) {
			this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "ERROR: Could not updated agent " + agentID);
			return false;
		}
		return true;
	}
	
	public boolean removeAgent(int agentID) {
		String ip = this.snmpManager.getAgent(agentID).getIp();
		if(this.snmpManager.deleteAgent(agentID)) {
			if(this.dbCtrl != null && this.dbCtrl.hasConnection()) {
				try {
					dbCtrl.removeSNMPAgent(agentID);
					this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "INFO: Agent '" + agentID + "' was deleted. HostIP: " +ip);
				} catch (SQLException e) {
					this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "ERROR: Could not delete agent '" + agentID +"'. HostIP: " + ip + 
							"\n-->SQL Error: " +e.getErrorCode() + 
							"\n-->SQL Message: " +e .getMessage());
				}
			}
			return true;
		}else {
			return false;
		}
	}
	
	// ###################################   M A N A G E R   M E T H O D S   ####################################
	public String managerGetTransportMappingInfo() {
		return this.snmpManager.getTransportMappingInfo();
	}
	
	public void managerAddTransport() {
		//TODO
	}
	
	/** Return an ArrayList with all the exitsting instances of SNMPAgent
	 * @return
	 */
	public ArrayList<SNMPAgent> managerGetAgents(){
		return this.snmpManager.getAgents();
	}
	
	/**
	 * Return the an SNMPAgent instance for the given agent_id or null if no SNMPAgent exists for given agent_id.
	 * @param agent_id
	 * @return
	 */
	public SNMPAgent managerGetAgent(int agent_id) {
		return this.snmpManager.getAgent(agent_id);
	}
	
	/**
	 * Sends a sync GET REQUEST to the given agent with the given OIDs.
	 * @param agent_id
	 * @param oid
	 * @return
	 */
	public ResponseEvent<Address> managerSendSyncGET(int agent_id, String...oid) {
		return this.snmpManager.sendSyncGET(agent_id, oid);
	}
	
	/** Sends an async REQUEST to the given agent_id, with the given oids and an object "handler" to 
	 * choose how to handle the request response.
	 * 
	 * @param agent_id
	 * @param handler
	 * @param oid
	 * @throws IOException
	 */
	public void managerSendAsync(int agent_id, Object handler, String...oid) throws IOException {
		this.snmpManager.sendAsync(JSNMPUtil.createPDU(oid), this.snmpManager.getAgent(agent_id).getCommunityTarget(), handler);
	}
	
	public void managerSendRequestX(int agentID, int requestType,  Object handler, String...oids) throws IOException{
		this.snmpManager.sendAsync(JSNMPUtil.createPDU(requestType, oids), this.snmpManager.getAgent(agentID).getCommunityTarget(), handler);
	}
	
	// TRAPHANDLER RULES
	public void addTrapHandlerRule(String name, String oid, int...actions) {
		Rule rule = this.snmpManager.addTrapHandlerRule(name, oid, actions);
		if(this.getDataBaseStatus() && rule != null) {
			String strActions = "";
			for(int action : rule.getActions()) {
				strActions += String.valueOf(action) + ",";
			}
			
			try {
				this.dbCtrl.addTHRule(rule.getName(), rule.getOID().toString(), strActions.substring(0,strActions.length()-1)
						, rule.getMailingData()[1], rule.getMailingData()[2]);
			} catch (SQLException e) {
				this.writeToCTRLLogFile("ERROR: Cannot ad THRule '" + name +"'"
						+ "\n-->SQLError: " + e.getErrorCode()
						+ "\n-->SQLMessage: " + e.getMessage());
			}
		}
	}
	
	public void addTrapHandlerRule(String name, String oid, String mailSubject, String mailtext, int...actions) {
		Rule rule = this.snmpManager.addTrapHandlerRule(name, oid, mailSubject, mailtext, actions);
		if(this.getDataBaseStatus() && rule != null) {
			String strActions = "";
			for(int action : rule.getActions()) {
				strActions += String.valueOf(action) + ",";
			}
			try {
				this.dbCtrl.addTHRule(rule.getName(), rule.getOID().toString(), strActions.substring(0,strActions.length()-1)
						, rule.getMailingData()[1], rule.getMailingData()[2]);
			} catch (SQLException e) {
				this.writeToCTRLLogFile("ERROR: Cannot ad THRule '" + name +"'"
						+ "\n-->SQLError: " + e.getErrorCode()
						+ "\n-->SQLMessage: " + e.getMessage());
			}
		}
	}
	
	public List<Rule> getTrapHandlerRules(){
		return this.snmpManager.getTrapHandlerRules();
	}
	

	// ##################################   D A T A B A S E    M E T H O D S   #######################################

	
	public List<SNMPAgent> dbGetSNMPAgents() throws SQLException{
		if(this.dbCtrl != null)
			return this.dbCtrl.getSNMPAgents();
		else
			return new ArrayList<SNMPAgent>();
	}
	
	public boolean dbAddPDU(PDU pdu, int agentID) {
		if(this.dbCtrl != null &&  this.dbCtrl.hasConnection()) {
			try {
				this.dbCtrl.addPDU(pdu, agentID);
				return true;
			} catch (SQLException e) {
				return false;
			}
		}
		return false;
	}
	
	public void dbAddProcess(String processID) {
		SNMPProcess ps = this.snmpManager.getProcess(processID);
		try {
			String oids = "";
			for(VariableBinding vs : ps.getVarbindings()) {
				oids += vs.getOid()+",";
			}
			oids = oids.substring(0, oids.length()-1);
			if(this.dbCtrl != null){
				this.dbCtrl.addProcess(ps.getProcessID(), ps.getName(), ps.getDescription(), ps.getSleepTime(), ps.getSaveInDB(), ps.getAgendID(), oids);
				this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "INFO: New SNMPProcess saved in DB. ProcessID = " + processID);
			}
		} catch (SQLException e) {
			this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "ERROR: Could NOT save SNMPProcess in database. ProcessID = " + processID
					+ "\nSQLError: " + e.getErrorCode()
					+ "\nSQLMessage: " + e.getMessage());
		}
	}

	
	
//  ############################   P R O C E S S O R    M E T H O D S   ########################
	// TODO HANDLE DATABASE FOR PROCESSES
	/**
	 * Creates a new Process. This process is accessible from the Map(snmpPSList). Creating a process does not start it.
	 * To do so, call @method startProcess(processID). To stop it, call stopProcess(processID). Once stopped, the Thread executing will be killed.
	 * To start a new Thread call @method startProcess(processID). One SNMPProcess must be executed only by one Thread at a time.
	 * 
	 * Returns the processID for this new SNMPProcess.
	 * @param agentI	 * @param sleepTime
	 * @param oids
	 * @return 
	 */
	public String createSNMPProcess(String name, String description, int agentID, long sleepTime, String...oids) {
		String processID = this.snmpManager.createSNMPProcess(name, description, agentID, sleepTime, this, oids);
		this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "INFO: New SNMPProcess created. ProcessID = " + processID);
		return processID;
		
	}
	
	public boolean updateProcess(String psID, String name, String descr, long sleepTime, byte saveData, byte show, byte running) {
		try {
			this.snmpManager.updateProcess(psID, name, descr, sleepTime, saveData, show, running);
			if(this.dbCtrl != null)
				this.dbCtrl.updateProcess(psID, name, descr, sleepTime, saveData);
			this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "INFO: SNMPProcess '" + psID + "' has updated!");
			return true;
		} catch (SQLException e) {
			this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "ERROR: SNMPProcess '" + psID + "' could not be updated in databases!"
					+ "\n--> SQLError: " + e.getErrorCode()
					+ "\n--> SQLMessage: " + e.getMessage());
			return false;
		}
	}
	
	/**
	 * Deletes a process. If the process is being executed when this method is called, pray to God he helps you.
	 * @param processID
	 */
	public void deleteSNMPProcess(String processID) {
		this.snmpManager.deleteSNMPProcess(processID);
		try {
			if(this.dbCtrl != null) {
				this.dbCtrl.deleteProcess(processID);
				this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "INFO: SNMPProcess '" + processID + "' has been deleted!");
			}
		} catch (SQLException e) {
			this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "ERROR: SNMPProcess '" + processID + "' could not be deleted from database");
		}
		
	}
	
	/**
	 * Starts a new Thread for the given process. If the process is being executed when this method is called, pray to God he helps you.
	 * @param processID
	 */
	public void startProcess(String processID) {
		this.snmpManager.startProcess(processID);
		 this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "INFO: Starting SNMPProcess '" + processID + "'");
	}
	
	/**
	 * Stops the Thread executed by a process. 
	 * @param processID
	 */
	public void stopProcess(String processID) {
		try {
			this.snmpManager.stopProcess(processID);
			 this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "INFO: SNMPProcess '" + processID + "' has been stopped");
		} catch (InterruptedException e) {
			

		}
	}
	
	public void setProcessSaveDataInDB(boolean status, String processID) {
		this.snmpManager.getProcess(processID).setSaveInDB(status);
	}
	
	public void setProcessVerbose(boolean status, String processID) {
		this.snmpManager.getProcess(processID).setShowResponse(status);
	}
	
	public void getProcessData(String processID) {
		
	}
	
	public SNMPProcess getProcess(String processID) {
		return this.snmpManager.getProcess(processID);
		
	}

	public SNMPProcess getProcessByName(String name) {
		for(SNMPProcess ps : this.snmpManager.getProcesses()) {
			if(ps.getName().equals(name))
				return ps;
		}
		return null;
	}
	
	public List<SNMPProcess> getAllSNMPProcesses(){
		return this.snmpManager.getProcesses();
	}
	public Set<String> getAllActiveProcessPID() {
		return this.snmpManager.getAllActiveProcessPID();	
	}
	
	public Set<String> getAllSNMPProcessPID() {
		return this.snmpManager.getAllSNMPProcessPID();
	}
	
	// ####################    S U B S C R I B E R   M E T H O D S     ######################
	public void onSubscribe(Subscription subscription) {

	}

	@Override
	public synchronized void onNext(Whisper item) {
		
		 // ··········· WHISPER IS TYPE SNMPROCESSWHISPER
		if(item instanceof ProcessWhisper) {
			
			ProcessWhisper psWhisper = (ProcessWhisper) item;
			if(psWhisper.getAction() == ProcessWhisper.SEND_SNMP) {
				try {
					
					this.snmpManager.sendAsync(psWhisper.getResponsePDU(),
							this.snmpManager.getAgent(psWhisper.getAgentID()).getCommunityTarget(),
							this.snmpManager.getProcess(psWhisper.getProcessID()));
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else if(psWhisper.getAction() == ProcessWhisper.RESPONSE) {
				
				try { //TODO IF DB IS NOT ACTIVE THIS BREAKS
					
					if(this.dbCtrl != null && this.snmpManager.getProcess(psWhisper.getProcessID()).getSaveInDB())
						if(((ProcessWhisper)item).getResponsePDU() != null)
							this.dbCtrl.addPDU(((ProcessWhisper)item).getResponsePDU(), psWhisper.getAgentID());
						else
							this.writeToCTRLLogFile("WARNING: Recieved null PDU from Process: " + psWhisper.getProcessID());
					
					if(this.snmpManager.getProcess(psWhisper.getProcessID()).getShowResponse())
						if(this.monitor != null)
							this.monitor.onNext(psWhisper);
				} catch (SQLException e) {
					this.writeToCTRLLogFile("ERROR: Cannot save process PDU in database."
							+ "\n--> SQLError: " + e.getErrorCode()
							+ "\n--> SQLMessage: " + e.getMessage());
				}
			}
		return;}
		
		 // ··········· REQUEST WHISPER
		if(item instanceof RequestWhisper) {
			if(this.monitor != null)
				this.monitor.onNext(item);
			return;
		}
		
		
		if(item instanceof TrapWhisper) {
			
			TrapWhisper trWhisper = (TrapWhisper)item;
			String data = "";
			for(VariableBinding var : trWhisper.getPDU().getVariableBindings()) {
				data += this.ctrlFileHandler.indentation + "OID: " + var.getOid() + " | Value: " +var.getVariable() + "\n";
			}
			// SAVING TRAP INFO IN LOG FILE
			String trapMessage = 	" - - - - - - - TRAP RECEIVED - - - - - - -   " + "\n" 
			        + this.ctrlFileHandler.indentation + "Address: " + trWhisper.getSourceAddress() + " | AgentID: " + trWhisper.getAgentID() + "\n"
					+ this.ctrlFileHandler.indentation + "RequestID: " + trWhisper.getPDU().getRequestID() + "\n"
					+ this.ctrlFileHandler.indentation + "DATA:\n" + data;
					
			this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getSNMPLogFilePath(), trapMessage);
				
			
			// SAVING TRAP INFO IN DB
			try {
				if(this.dbCtrl != null)
					this.dbCtrl.addPDU(trWhisper.getPDU(), agentIDCounter);
				else
					this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getSNMPLogFilePath(), "Warning: Cannot save TRAP in Database. Database not established");
			} catch (SQLException e) {
				if(this.dbCtrl != null && this.dbCtrl.hasConnection())
					this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getSNMPLogFilePath(), "Warning: There seems to be a problem saving TRAPS");
			}
			
			// PASS EVENT TO MONITOR
			//System.out.println("BEEPING?");
			if(this.monitor != null)
				this.monitor.onNext(item);

			return;
			
		}
		
		
		
	}

	/** Does nothing! */
	@Override
	public synchronized void onError(Throwable throwable) {}

	/** Does nothing! */
	@Override
	public synchronized void onComplete() {}

	/** Does nothing! */
	@Override
	public void subscribe(Subscriber<? super Whisper> subscriber) {}
	
	
}

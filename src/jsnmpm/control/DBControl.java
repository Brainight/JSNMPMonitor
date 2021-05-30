package jsnmpm.control;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.snmp4j.PDU;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import com.mysql.cj.protocol.Resultset;

import jsnmpm.control.process.SNMPProcess;
import jsnmpm.control.snmp.SNMPAgent;
import jsnmpm.control.utilities.Rule;


/**
 * 
 * @author MrStonedDog
 *
 */
public class DBControl {

	public static enum DB_ERR{
		OK, 
		NO_CONNECTION, 
		UNABLE_TO_LOAD_DRIVER,
		SQL_EXCEPTION;

	}
	// STATIC 
	private final String PREFIX_URL = "jdbc:mysql://";

	
	// INSTANCE
	/** IP from host executing MySQL **/
	private String host = null;
	
	private int port = 3306;
	private String dbName = null;
	private String user = null;
	private String password = null;
	private boolean hasConnection = false;
	
	public DBControl(String dbName, String host, int port, String user, String password) throws ClassNotFoundException, SQLException {
		//Class.forName("com.mysql.cj.jdbc.Driver");
        DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
		this.dbName = dbName;
		this.port = port;
		this.user = user;
		this.password = password;
		this.host = host;
	}
	
	public DBControl() {
		
	}
	
	
	// #########################     H E L P F U L    M E T H O D S      ##########################
	
	/**
	 * Return true if the program is able to connect to the database with the specified parameters. False otherwise.
	 * @return boolean
	 */
	public boolean checkConnection() {
		try {
			Connection con = this.getConnection();
			this.hasConnection = con.isValid(0);
			con.close();
		} catch (SQLException e) {
			this.hasConnection = false;
		}
		
		return this.hasConnection;
	}
	
	/** Returns the current database status. True if connection with DB can be established. False otherwise. 
	 *  This gets the value saved by  the last @method checkConnection call. To try and connect to the database again
	 *  use the mentioned method.
	 * @return
	 */
	protected boolean hasConnection() {
		return this.hasConnection;
	}
	
	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(this.PREFIX_URL+this.host+":"+this.port+"/"+this.dbName, this.user, this.password);
	}
	
	
	
	// ###########################     A G E N T S   H A N D L I N G      #############################
	//  --> GET / ADD / MODIFY / DEL
	
	/**
	 * Gets all SNMPAgents on the database.
	 * @return List<SNMPAgent>
	 * @throws SQLException 
	 */
	public List<SNMPAgent> getSNMPAgents() throws SQLException{ 
		// TODO DATABASE AGENT HAS MORE FIELDS THAN SNMPAGENT
		List<SNMPAgent> snmpAgents = new ArrayList<SNMPAgent>();
		Connection con = this.getConnection();
		Statement s = con.createStatement();
		ResultSet rs = s.executeQuery("SELECT * FROM snmpagent");
		while(rs.next()) {
			try {
				SNMPAgent agent = new SNMPAgent(rs.getString("ipv4"), rs.getInt("port"),
						rs.getString("alias"), rs.getString("read_com"));
				
				agent.setId(rs.getInt("agent_id"));
				snmpAgents.add(agent);
				
			} catch (UnknownHostException e) {
			}
		}
		con.close();
		return snmpAgents;
	}
	
	
	public void addSNMPAgent(SNMPAgent agent) throws SQLException {
		Connection con = this.getConnection();
		con.createStatement()
		.executeUpdate(String.format("INSERT INTO snmpagent VALUES (%d, '%s', '%s', '%s', '%s', '%d', '%s', '%s')",
				agent.getId(), null, agent.getName(), agent.getIp(), null, agent.getPort(), agent.getReadCommunity(), null));
		
		con.close();
		
	}
	
	/** Only adds those fiedls that aren`t null or -1 in case of integer.  **/
	public void updateAgent(int agentID, String name, String ip, int port, String readCom) throws SQLException {
		
		Connection con = this.getConnection();
		con.createStatement().executeUpdate("UPDATE snmpagent SET" +
		((name == null) ? "" : String.format(" alias='%s'", name)) + 
		((ip == null) ? "" : (name == null) ? String.format(" ip='%s'", ip) : String.format(", ip='%s'", ip)) + 
		((port == -1) ? "" : (name == null && ip == null) ? String.format(" port=%d ", port) : String.format(", port=%d", port)) + 
		((readCom == null) ? "" : (name == null && ip == null && port == -1) ? String.format(" read_com='%s'", readCom) : String.format(", read_com='%s'", readCom)) + 
		String.format("WHERE agent_id = %d", agentID ));
		
		con.close();
		
		
	}
	
	public void removeSNMPAgent(int agent_id) throws SQLException {
		Connection con = this.getConnection();
		con.createStatement().executeUpdate(String.format("DELETE FROM snmpagent WHERE agent_id = %d", agent_id));
		con.close();
	}
	
	// #######################################   P R O C E S S  H A N D L I N G   #################################
	
	public List<SNMPProcess> getAllProcesses() throws SQLException {
		List<SNMPProcess> processList = new ArrayList<SNMPProcess>();
		Connection con = this.getConnection();
		Statement s = con.createStatement();
		ResultSet rs = s.executeQuery("SELECT * FROM snmpprocess");
		while(rs.next()) {
			processList.add(new SNMPProcess(rs.getString("processID"), rs.getString("processName"), rs.getString("processDescription"),
					rs.getLong("processSleepTime"), rs.getBoolean("processSaveData"), rs.getInt("processAgentID"),
					rs.getString("processOID")));
		}
		
		con.close();
		return processList;
	}
	public void addProcess(String psID, String name, String descr, long sleeptime, boolean savePDUs, int agentID, String oids) throws SQLException {
		
		Connection con = this.getConnection();
		con.createStatement().executeUpdate(String.format("INSERT INTO snmpprocess VALUES ('%s','%s','%s',%d,%d,%d, '%s')", psID, name, descr, sleeptime, (savePDUs) ? 1 : 0, agentID, oids));
		con.close();
	}
	
	/** Only updates the values that are not null, (numeric) -1. Boolean must be passed as 0=false 1=true and -1 if "null" **/
	public void updateProcess(String processID, String name, String descr,  long sleepTime, byte saveData) throws SQLException {
		Connection con = this.getConnection();
		con.createStatement().executeUpdate("UPDATE snmpprocess SET" +
		((name == null) ? "" :  String.format(" processName = '%s'", name)) +
		((descr == null) ? "" : String.format(((name == null) ? " " : ", ") + " processDescription = '%s'" , descr)) +
		(( sleepTime == -1) ? "" :  String.format(((name == null && descr == null) ? " " : ", ") + " processSleepTime = %d", sleepTime)) +
		((saveData == -1) ? "" : String.format(((name == null && descr == null && sleepTime == -1) ? " " : ", ") + " processSaveData = %d" , saveData)) +
		String.format(" WHERE processID = '%s'", processID)
		);
		con.close();

	}
	
	public void deleteProcess(String psID) throws SQLException{
		
		Connection con = this.getConnection();
		con.createStatement().executeUpdate(String.format("DELETE FROM snmpprocess WHERE processID = %s", psID));
		con.close();
	}
	
	// #######################################    P D U   H A N D L I N G    #####################################
	
	// ················  PDU  ·····················
	public synchronized void addPDU(PDU pdu, int agent_id) throws SQLException {
		
		String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(LocalDateTime.now());
		Connection con = this.getConnection();
		con.createStatement().executeUpdate(String.format("INSERT INTO pdu VALUES (%d, %d, '%s',%d)", pdu.getRequestID().toInt(), pdu.getType(), date, agent_id));
		for(VariableBinding vb : pdu.getVariableBindings())
			this.addPDUVar(pdu.getRequestID().toInt(), date, vb.getOid().toDottedString(), vb.getVariable().toString());
		
		con.close();
	}
	
	// ··············  PDU VAR  ·····················
	private void addPDUVar(int pduID, String datetime, String oid, String variable) throws SQLException {
		Connection con = this.getConnection();
		con.createStatement().executeUpdate(String.format("INSERT INTO varbinding VALUES ('%s', '%s', %d, '%s')", oid, variable, pduID, datetime));
		con.close();
	}

	// ######################   T R A P  R U L E   ###########################
	// TODO THIS DOESNT IMPLEMENT SAVING MAIL_TO  FIELD IN RULE CLASS!!!
	public void addTHRule(String ruleName, String trapOID, String actions, String mailSubject, String mailText) throws SQLException {
		Connection con = this.getConnection();
		con.createStatement().executeUpdate(String.format("INSERT INTO traprule (ruleName, trapOID, actions, mailSubject, mailText) VALUES "
				+ "('%s', '%s', '%s', '%s', '%s')", ruleName, trapOID, actions, mailSubject, mailText));
		con.close();
	}
	
	public List<Rule> getTHRules() throws SQLException {
		List<Rule> ruleList = new ArrayList<Rule>();
		Connection con = this.getConnection();
		Statement s = con.createStatement();
		ResultSet rs = s.executeQuery("SELECT * FROM traprule");
		while(rs.next()) {
			Rule rule = new Rule();
			rule.setName(rs.getString("ruleName"));
			rule.setOID(new OID(rs.getString("trapOID")));
			rule.setMailSubject(rs.getString("mailSubject"));
			rule.setMailMessage("mailText");
			String actions = rs.getString("actions");
			if(actions.length() == 1) {
				try {
				rule.setAction(Integer.parseInt(actions));
				}catch(NumberFormatException nfe) {
					// TODO
					System.out.println("ERROR PARSING RULE");
				}
			}
			else {
				String[] strActions = actions.split(",");
				int[] arrActions = new int[strActions.length];
				for(int i = 0; i < arrActions.length; i++) {
					try {
						arrActions[i] = Integer.parseInt(strActions[i]);
						}catch(NumberFormatException nfe) {
							// TODO
							System.out.println("ERROR PARSING RULE");
						}
				}
				rule.setAction(arrActions);
			}	
			ruleList.add(rule);
		} // END WHILE
		
		con.close();
		return ruleList;
		
	}
	
	
	
}

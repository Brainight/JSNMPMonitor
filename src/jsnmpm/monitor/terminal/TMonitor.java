package jsnmpm.monitor.terminal;

import java.io.IOException;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.fusesource.jansi.Ansi;
import org.snmp4j.PDU;

import jsnmpm.control.SNMPController;
import jsnmpm.control.process.SNMPProcess;
import jsnmpm.control.snmp.SNMPAgent;
import jsnmpm.control.utilities.Rule;
import jsnmpm.control.utilities.JSNMPUtil;
import jsnmpm.control.utilities.JSNMPUtil.INFO;
import jsnmpm.control.whisper.ProcessWhisper;
import jsnmpm.control.whisper.RequestWhisper;
import jsnmpm.control.whisper.TrapWhisper;
import jsnmpm.monitor.gui.AbstractMonitor;
import jsnmpm.monitor.terminal.exceptions.TPVAuthenticationException;


/**
 * This class is the main class for JSNMP Monitor APP executed in Terminal Mode.
 * @author MrStonedDog
 *
 */
public class TMonitor extends AbstractMonitor{

	
	private SNMPController ctrl;
	private Terminal terminal;
	private TProcessViewerController tvc = null;
	volatile boolean running;
	
	private Map<INFO, String> sysStats;
	
	// TODO MAKE MAP OF INFO
	
	public TMonitor(SNMPController ctrl) throws IOException {
		this.ctrl = ctrl;
	}
	
	public void run() {
		startMonitor();
	}
	
	protected void startMonitor() {
		try {
			this.terminal = new Terminal();
		} catch (IOException e) {
			this.ctrl.writeToCTRLLogFile("ERROR: I/O Exception occurred in Terminal.");
		}
	
		
		try {
			this.tvc = new TProcessViewerController();
		} catch (IOException e) {
			this.ctrl.writeToCTRLLogFile("ERROR: I/O Exception occurred in ProcessViewerController.");
		}

		terminal.clearTerminal();
		terminal.writeBanner();
		this.sysStats = ctrl.getFullCtrlInfo();
		terminal.printMainInfo(this.sysStats);
		terminal.printMainMenu();
		terminal.setInitialCursor();
		
		running = true;
		while(running) {
			this.processInput(terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a((terminal.currentStatus == Terminal.Prompt.NORMAL) 
					? Ansi.ansi().a(terminal.optionPrompt)
					: Ansi.ansi().a(terminal.shellPrompt))));
		}
	}
	
	// ##############     BASIC INPUT PROCESSING     ############### 
	private void processInput(String input) {
		if(input != null) {
			if(terminal.currentStatus == Terminal.Prompt.NORMAL) {
				this.parseInput(input);
				
			}
			else 
				this.processCommand(input);
		}
	}
	
	private void parseInput(String input) {
		if(input.length() > 0 && input.trim().charAt(0) == '!') {
			processCommand(input.trim().substring(1));
		}else {
			processOption(input);
		}
	}
	
	
	private void processOption(String option) {
		switch(option) {
		case "1": //ADD AGENT
			this.addAgent();
			break;
			
		case "2": // SHOW AGETS
			this.showAgents();
			break;
		case "3":
			this.configureAgent();
			break;
		case "4":
			this.deleteAgent();
			break;
			
		case "5":
			
			if(ctrl.managerGetAgents().size() > 0)
				this.sendUniqueQuery();
			else
				terminal.coutWarning("Warning: You dont have any configured Agents!");
			break;
		
		case "6":
			this.sendSNMPNext_Bulk();
			break;
		case "7":
			break;
		case "8":
			this.configTraps();
			break;
			
		case "9":
			this.createSNMPProcess();
			break;
		
		case "10":
			this.showProcess();
			break;
			
		case "11":
			this.modProcess();
			break;
			
		case "12":
			this.deleteProcess();
			break;
			
		case "13":
			this.configureSNMPManager();
			break;
			
		case "14": //SHELL
			terminal.changePrompt();
			terminal.clearTerminal();
			terminal.setUpStartHeader(ctrl.getFullCtrlInfo());
			break;
			
		case "15": //EXIT
			try {
				synchronized(this.ctrl) {
					this.ctrl.notify();
				}
				
				Thread.currentThread().join();
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
			
		default:
			terminal.coutError("Error: Option not found!");
			break;
	}
	}
	private void processCommand(String command) {

		switch(command.split(" ")[0].toLowerCase()) {
		case "resize": // resize [width]
			try {
				this.terminal.resize(Integer.parseInt(command.split(" ")[1]), ctrl.getFullCtrlInfo());
				this.terminal.setTerminalWidth(Integer.parseInt(command.split(" ")[1]));
			} catch (NumberFormatException nfe){
				terminal.coutError("Error: Wrong argument [width]=\'"+command.split(" ")[1]+"\'. Expect type: int");
			} catch(IOException IOe) {
				terminal.coutError("Error: IO is broken");
			} catch(IndexOutOfBoundsException iobe) {
				terminal.coutError("Error: Missing argument [width]");
			}catch(Exception e) {
				terminal.coutError("Error: Unknown exception occurred");
			}
			break;
		case "exit":
			terminal.changePrompt();
			terminal.clearTerminal();
			terminal.setUpStartHeader(ctrl.getFullCtrlInfo());
			break;
			
		case "reset":
			terminal.clearTerminal();
			terminal.setUpStartHeader(ctrl.getFullCtrlInfo());
			break;
		default:
			terminal.coutError("Error: Unknown command \'" + command.split("  ")[0] + "\'");
			break;
		}
	}
	
	// #############    TERMINAL MENU OPTIONS HANDLING    #####################
	// ······· (1) ADD AGENT
	private String askIP() {
		String ip;
		Pattern ipPattern = Pattern.compile(JSNMPUtil.IP_PATTERN);
		boolean askAgain = false;
		do {
			ip = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("IPv4: "));
			if(ipPattern.matcher(ip).matches())
				askAgain = false;
			else {
				terminal.coutError("Error: \'" + ip + "\' is not a valida IP Address");
				askAgain = true;
			}
			}while(askAgain);
		return ip;
	}
	
	private int askPort() {
		int port = JSNMPUtil.DEFAULT_SNMP_PORT1;
		boolean askAgain = false;
		do {
			String sPort = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("Port (def=161):"));
			if(sPort.isEmpty() || sPort.isBlank())
				port = JSNMPUtil.DEFAULT_SNMP_PORT1;
			else {
				try {
					port = Integer.parseInt(sPort);
					askAgain = false;
					if(port != JSNMPUtil.DEFAULT_SNMP_PORT1)
						terminal.coutWarning("Warning: Port \'" + port + "\' is not SNMP default port.");
				}catch(NumberFormatException nfe) {
					terminal.coutError("Error: Wrong input \'" + sPort + " - Expected type: int");
					askAgain = true;
				}
			}
		}while(askAgain);
		return port;
	}
	
	private void addAgent() {
		
		String ip; String name; String readCommunity;
		int port = JSNMPUtil.DEFAULT_SNMP_PORT1;
		
		
		
		this.printMenuOptionHeader("ADD AGENT");
		// ASK IP
		ip = this.askIP();
		// ASK PORT
		port =  this.askPort();
		// ASK NAME
		name = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("Name: "));
		// ASK READCOMMUNITY
		readCommunity = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("Read Community: "));
		
		try {
			terminal.coutNewLine();
			int agentID = ctrl.addNewAgent(ip, port, name, readCommunity);
			// CHECK IF AGENT IS REACHABLE
			terminal.print(Ansi.ansi().cursor(terminal.currentPromptRow, terminal.currentPromptCol).a(terminal.ansiInfo2).a("Checking if agent is reachable..."));
			if(ctrl.managerGetAgent(agentID).isReachable()) {
				terminal.print(Ansi.ansi().a(terminal.ansiGood).a(" OK\n"));
				terminal.currentPromptRow++;
				terminal.coutNewLine();
			}
			else {
				terminal.print(Ansi.ansi().a(terminal.ansiInfo).a(" NO\n"));
				terminal.currentPromptRow++;
				terminal.coutNewLine();
				terminal.coutWarning("Warning: Cannot reach target (PING)/(TCP PORT 7). Make sure the machine is up.");
			}
			
			
			
			if(name.isEmpty() || name.isBlank())
				terminal.coutWarning("Warning: Agent has no name!");
			if(readCommunity.isBlank() || readCommunity.isEmpty())
				terminal.coutWarning("Warning: ReadCommunity is empty!");
			
			terminal.cout(Ansi.ansi().a(terminal.ansiInfo).a("--> Agent added successfully!"));
			this.sysStats.put(INFO.AGENTS, String.valueOf(this.ctrl.getAgentsNumber()));
			
		} catch (UnknownHostException e) {
			terminal.coutError("Error: Agent could not be created... Host is unknown");
		} catch (SQLException e) {
			terminal.coutError("Error: Agent could not be saved to database...");
			e.printStackTrace();
		}
		terminal.coutNewLine();
		this.pressEnterToContinue();
	}
	
	
	// ······ (2) SHOW AGENTS TODO FIIIX
	private void showAgents() {
		this.printMenuOptionHeader("SHOW AGENTS");
		
		// AGENTS WILL DE DISPLAYED ACCORDING TO THE GIVEN SPACE (X Lines / 40 Col (width)
		List<SNMPAgent> agentList = ctrl.managerGetAgents();
		if(agentList.size() == 0) {
			terminal.currentPromptCol = (terminal.getTerminalWidth() - "NO AGENTS ARE CONFIGURED".length()) / 2;
			terminal.coutWarning("NO AGENTS ARE CONFIGURED");
			terminal.coutNewLine();
			this.pressEnterToContinue();
			return;
		}
		
		terminal.currentPromptCol = terminal.startOptionPromptCol - 3;
		int agentXRow = terminal.getTerminalWidth()/40;
		boolean hasNextAgent = true;
		int agentDescriptionRows = 6; int jumpLines = 2;
		for(int i = 0;i<agentList.size(); i+=agentXRow) {
			Ansi headers = Ansi.ansi(); Ansi names = Ansi.ansi(); Ansi addresses = Ansi.ansi(); Ansi port = Ansi.ansi();
			Ansi states = Ansi.ansi(); Ansi readComms = Ansi.ansi();
			//  HEADERS
			try {
				for(int j = i, width=35, ag = 0; j < agentXRow + i; j++, ag++){
					headers.a(Ansi.ansi().cursor(terminal.currentPromptRow, terminal.currentPromptCol + width * ag)
							.a(terminal.ansiMenuOption).a(" - AGENT " + agentList.get(j).getId() + " - "));
				
					names.a(Ansi.ansi().cursor(terminal.currentPromptRow + 1, terminal.currentPromptCol + width * ag)
							.a(terminal.ansiPrompt).a("Name: ").a(terminal.ansiDefault).a(agentList.get(j).getName()));
				
					addresses.a(Ansi.ansi().cursor(terminal.currentPromptRow + 2, terminal.currentPromptCol + width * ag)
							.a(terminal.ansiPrompt).a("Address: ").a(terminal.ansiDefault).a(agentList.get(j).getCommunityTarget().getAddress()));
					
					port.a(Ansi.ansi().cursor(terminal.currentPromptRow + 3, terminal.currentPromptCol + width * ag)
							.a(terminal.ansiPrompt).a("Port: ").a(terminal.ansiDefault).a(agentList.get(j).getPort()));
				
					states.a(Ansi.ansi().cursor(terminal.currentPromptRow + 4, terminal.currentPromptCol + width * ag)
							.a(terminal.ansiPrompt).a("State: ").a(terminal.ansiDefault).a((agentList.get(j).getState()) ? "Reachable" : "Unreachable"));
				
					readComms.a(Ansi.ansi().cursor(terminal.currentPromptRow + 5, terminal.currentPromptCol + width * ag)
							.a(terminal.ansiPrompt).a("Read Comm: ").a(terminal.ansiDefault).a(agentList.get(j).getReadCommunity()));
				}
			}catch(IndexOutOfBoundsException exc) {hasNextAgent = false;}
			
			terminal.print(headers.a(terminal.ansiDefault).a("\n"));
			terminal.print(names.a("\n"));
			terminal.print(addresses.a("\n"));
			terminal.print(port.a("\n"));
			terminal.print(states.a("\n"));
			terminal.print(readComms.a("\n"));
			
			terminal.currentPromptRow += agentDescriptionRows;
			terminal.jumpLines(jumpLines);
			
			if(!hasNextAgent)
				break;
	
		}

		this.pressEnterToContinue();
	}
	
	// ······ (3) CONFIG AGENT
	private void configureAgent() {
		this.printMenuOptionHeader("CONFIGURE AGENT");
		
		int agentID = this.askForAgent("CONFIGURE AGENT");
		SNMPAgent agent = this.ctrl.managerGetAgent(agentID);
		String op = "", name = null, ip  = null, readCom = null;
		int port = -1;
		
		boolean quit = false;
		do{
		
		this.terminal.cout(Ansi.ansi().a(terminal.ansiGood + "(1)" + terminal.ansiInfo2 + " Name: " + terminal.ansiDefault + ((name == null) ? agent.getName() : name)));
		this.terminal.cout(Ansi.ansi().a(terminal.ansiGood + "(2)" + terminal.ansiInfo2 + " IPv4: " + terminal.ansiDefault + ((ip == null) ? agent.getIp() : ip)));
		this.terminal.cout(Ansi.ansi().a(terminal.ansiGood + "(3)" + terminal.ansiInfo2 + " Port: " + terminal.ansiDefault + ((port == -1) ? agent.getPort() : port)));
		this.terminal.cout(Ansi.ansi().a(terminal.ansiGood + "(4)" + terminal.ansiInfo2 + " Read Com: " + terminal.ansiDefault + ((readCom == null) ? agent.getReadCommunity() : readCom)));
		this.terminal.cout(Ansi.ansi().a(terminal.ansiPrompt + "---------------------------------"));
		this.terminal.cout(Ansi.ansi().a(terminal.ansiGood + "(5)" + terminal.ansiDefault + "Save and exit"));
		this.terminal.cout(Ansi.ansi().a(terminal.ansiGood + "(6)" + terminal.ansiDefault + "Exit"));
		
		op = this.terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt + "Configure: "));
		
		switch(op) {
		case "1":
			name = this.terminal.readInput(Ansi.ansi().a(this.terminal.ansiPrompt + "New Agent Name: "));
			break;
		case "2":
			ip =  this.askIP();
			break;
		case "3":
			port = this.askPort();
			break;
		case "4":
			readCom = this.terminal.readInput(Ansi.ansi().a(this.terminal.ansiPrompt + "New Read Community: "));
			break;
		case "5":
			this.terminal.cout(Ansi.ansi().a(terminal.ansiInfo2 + "Saving data..."));
			if(this.ctrl.updateAgent(agentID, (name == null) ? agent.getName() : name, (ip == null) ? agent.getIp() : ip,
					(port == -1) ? agent.getPort() : port, (readCom == null) ? agent.getReadCommunity() : readCom ))
				this.terminal.cout(Ansi.ansi().a(terminal.ansiGood + "Success!"));
			else
				this.terminal.coutError("Error: Unable to updated agent in DB. Check log.");
		case "6":
			this.terminal.cout(Ansi.ansi().a(terminal.ansiInfo2 + "Exiting..."));
			quit = true;
			break;
		default:
			this.terminal.coutError("Error: Unknown field");
				
		}
		}while(!quit);
		
		this.terminal.reset(ctrl.getFullCtrlInfo());
	}
	
	// ······ (4) DELETE AGENT
	private void deleteAgent() {
		this.printMenuOptionHeader("DELETE AGENT");
		
		int agentID = this.askForAgent("DELETE AGENT");
		
		if(agentID >= 0) {
			
				if(this.ctrl.removeAgent(agentID)) {
					terminal.cout(Ansi.ansi().a(terminal.ansiInfo2 + "Agent deleted succesfully"));
					this.sysStats.put(INFO.AGENTS, String.valueOf(this.ctrl.getAgentsNumber()));
				}
				else
					this.terminal.coutError("Error: A problem occured deleting agent");
				terminal.coutNewLine();
				this.pressEnterToContinue();
		}
	}
	
	// ······ (5) SEND REQUEST
	private  void sendUniqueQuery() {
		//String oid = null;
		String[] oids = null;
		int type; int[] agentIDs;
		boolean askAgain = false;
		
		this.printMenuOptionHeader("SEND REQUEST");

		// CHOOSE AGENT(S)
		terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a("Choose one ore more agents (separated by \",\")"));
		terminal.coutNewLine();
		
		terminal.print(Ansi.ansi().cursor(terminal.currentPromptRow, (terminal.getTerminalWidth() - "AGENTS".length()) / 2).a(terminal.ansiPrompt).a("AGENTS"));
		terminal.coutNewLine();
		
		for(SNMPAgent agent : ctrl.managerGetAgents()) 
			terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a("ID:" + agent.getId() + " - IP: " + agent.getIp() + " - NAME: "
					+agent.getName()+ " - State: " + agent.getState()));
		
		do {
			String[] strIDs = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("Agents ID: ")).split(",");
			agentIDs = new int[strIDs.length];
			for(int i = 0; i < strIDs.length; i++) {
				try {
					int agentID = Integer.parseInt(strIDs[i].trim());
					if(ctrl.managerGetAgent(agentID) == null) {
						terminal.coutError("Error: Cannot find SNMPAGent with ID=\'" + strIDs[i] + "\'.");
						askAgain = true;
						break;
					}else {
						agentIDs[i] = agentID;
						askAgain = false;
					}	
				
				}catch(NumberFormatException nfe) {
					terminal.coutError("Error: Expected type int for AgentID");
					askAgain = true;
					break;
				}
			}
		}while(askAgain);
		
		// SHOW TEST OIDS TODO HOW DO WE DO THIS FOR ALL OIDS?? XD
		oids = this.askOIDs();
	
		
		
		for(int agentID : agentIDs) {
			terminal.cout(Ansi.ansi().a(terminal.ansiPrompt).a("-".repeat(50)));
			terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a("Sending SNMP to Agent " + agentID + "..."));
			try {
				ctrl.managerSendAsync(agentID, new RequestWhisper(agentID, new PDU()), oids);
			} catch (IOException e1) {
				this.ctrl.writeToCTRLLogFile("ERROR: Cannot send SNMP Request\nError-Msg: " + e1.getMessage());
				terminal.cout(Ansi.ansi().a("Cannot send Request. Check log!"));
			}
			
			synchronized(this) {
				try {
					
					this.wait();

				} catch (InterruptedException e) {
					
				}
			}	
		}
		
		askAgain = true;
		
		terminal.coutNewLine();
		this.pressEnterToContinue();
	
	}
	
	// ······ (6) SEND NEXT/BULK //TODO
	private void sendSNMPNext_Bulk() {
		//String oid = null;
		String[] oids = null;
		int type; int[] agentIDs;
		boolean askAgain = false;
		
		this.printMenuOptionHeader("SEND REQUEST V2");

		// CHOOSE AGENT(S)
		terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a("Choose one ore more agents (separated by \",\")"));
		terminal.coutNewLine();
		
		terminal.print(Ansi.ansi().cursor(terminal.currentPromptRow, (terminal.getTerminalWidth() - "AGENTS".length()) / 2).a(terminal.ansiPrompt).a("AGENTS"));
		terminal.coutNewLine();
		
		for(SNMPAgent agent : ctrl.managerGetAgents()) 
			terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a("ID:" + agent.getId() + " - IP: " + agent.getIp() + " - NAME: "
					+agent.getName()+ " - State: " + agent.getState()));
		
		do {
			String[] strIDs = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("Agents ID: ")).split(",");
			agentIDs = new int[strIDs.length];
			for(int i = 0; i < strIDs.length; i++) {
				try {
					int agentID = Integer.parseInt(strIDs[i].trim());
					if(ctrl.managerGetAgent(agentID) == null) {
						terminal.coutError("Error: Cannot find SNMPAGent with ID=\'" + strIDs[i] + "\'.");
						askAgain = true;
						break;
					}else {
						agentIDs[i] = agentID;
						askAgain = false;
					}	
				
				}catch(NumberFormatException nfe) {
					terminal.coutError("Error: Expected type int for AgentID");
					askAgain = true;
					break;
				}
			}
		}while(askAgain);
		
		// SHOW TEST OIDS TODO HOW DO WE DO THIS FOR ALL OIDS?? XD
		//oids = this.askOIDs();
		oids = this.terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt + "Write OID(s) (oid1,oid2): ")).split(",");
		int requestType = 0;
		this.terminal.cout(Ansi.ansi().a(terminal.ansiPrompt + "Select request type: "));
		this.terminal.cout(Ansi.ansi().a(terminal.ansiInfo2 + "(1)GET"));
		this.terminal.cout(Ansi.ansi().a(terminal.ansiInfo2 + "(2)GETNEXT"));
		this.terminal.cout(Ansi.ansi().a(terminal.ansiInfo2 + "(3)GETBULK"));
		while(true) {
			String req = this.terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt + "Choice: "));
			if(req.equals("1")) {
				requestType = PDU.GET;
				break;
			}
			else if(req.equals("2")) {
				requestType = PDU.GETNEXT;
				break;
			}else if(req.equals("3")) {
				requestType = PDU.GETBULK;
				break;
			}else {
				this.terminal.coutError("Unknown option for request type. Try again!");
			}
		}
		
		for(int agentID : agentIDs) {
			terminal.cout(Ansi.ansi().a(terminal.ansiPrompt).a("-".repeat(50)));
			terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a("Sending SNMP to Agent " + agentID + "..."));
			try {
				ctrl.managerSendRequestX(agentID, requestType, new RequestWhisper(agentID, new PDU()), oids);
			} catch (IOException e1) {
				this.ctrl.writeToCTRLLogFile("ERROR: Cannot send SNMP Request\nError-Msg: " + e1.getMessage());
				terminal.cout(Ansi.ansi().a("Cannot send Request. Check log!"));
			}
			
			synchronized(this) {
				try {
					
					this.wait();

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}	
		}
		
		askAgain = true;
		
		terminal.coutNewLine();
		this.pressEnterToContinue();

	}
	// ······ (7) SEE TRAPS // TODO
	
	private void seeTraps() {
		
	}

	
	// ······ (8) TRAP CONFIG  //TODO
	
	private void configTraps() {
		int op = 0;
		this.printMenuOptionHeader("TRAP CONFIGURATION");
		this.terminal.cout(Ansi.ansi().a("1. New Rule"));
		this.terminal.cout(Ansi.ansi().a("2. Remove Rule"));
		this.terminal.cout(Ansi.ansi().a("3. See Rules"));
		this.terminal.cout(Ansi.ansi().a("0. Exit"));
		do {
			try {
				op = Integer.parseInt(this.terminal.readInput(Ansi.ansi().a("Option: ")));
				if(op > 3 || op < 0)
					this.terminal.coutError("ERROR: Wrong option ");
			}catch(NumberFormatException nfe) {
				this.terminal.coutError("ERROR: Wrong option ");
			}
		}while(op > 3 || op <  0);
		
		switch(op) {
		case 1:
			this.terminal.cout(Ansi.ansi().a(terminal.ansiMenuOption + "New Rule"));
			String ruleName = this.terminal.readInput(Ansi.ansi().a("Rule Name: "));
			
			String trapOID = null;
			// SET TRAP OID TO HANDLE
			do {
				trapOID = this.terminal.readInput(Ansi.ansi().a("Match Trap OID: "));
				if(!JSNMPUtil.isValidOID(trapOID)) {
					this.terminal.coutError("ERROR: Invalid OID");
					trapOID = null;
				}
			}while(trapOID == null || trapOID.isEmpty());
			
			this.terminal.coutNewLine();
			this.terminal.cout(Ansi.ansi().a(terminal.ansiMenuOption + "SET ACTION"));
			// SET ACTION
			int actions[] = null;
			boolean gtg = false;
			boolean sendMail = false;
			do {
				this.terminal.cout(Ansi.ansi().a("1. SEND MAIL"));
				this.terminal.cout(Ansi.ansi().a("2. EXTRA BEEP"));
				String[] actionsStr = this.terminal.readInput(Ansi.ansi().a("Option(s) (op1,op2...): ")).split(",");
				try {
					
					actions = new int[actionsStr.length];
					for(int i = 0; i  < actions.length; i++) {
						actions[i] = Integer.parseInt(actionsStr[i]);
						if(actions[i] == Rule.ACTION_SEND_MAIL)
							sendMail = true;
						if(actions[i] > 2 || actions[i] < 1) {
							gtg = false;
							sendMail = false;
							break;
						}
					}
					
					gtg = true;
					
				}catch(NumberFormatException nfe) {
					this.terminal.coutError("ERROR: Wrong option ");
					gtg = false;
					
				}
				
			}while(!gtg);
			
			// IF ACTION == SEND MAIL. SET SUBJECT AND MESSAGE
			
			if(sendMail) {
				this.terminal.cout(Ansi.ansi().a(terminal.ansiMenuOption + "MAILING OPTIONS"));
				
				String subject = this.terminal.readInput(Ansi.ansi().a("Subject: "));
				String message = this.terminal.readInput(Ansi.ansi().a("Message: "));
				
				this.ctrl.addTrapHandlerRule(ruleName, trapOID, subject, message, actions);
				break;
			}
			
			this.ctrl.addTrapHandlerRule(ruleName, trapOID, actions);
			
			
			break;
		case 2: //TODO
			break;
		case 3: //TODO
			
			List<Rule> rules = this.ctrl.getTrapHandlerRules();
			if(rules.size() == 0) {
				terminal.currentPromptCol = (terminal.getTerminalWidth() - "NO TRAP HANDLER RULES ARE CONFIGURED".length()) / 2;
				terminal.coutWarning("NO TRAP HANDLER RULES ARE CONFIGURED");
				terminal.coutNewLine();
				this.pressEnterToContinue();
				return;
			}
			terminal.currentPromptCol = terminal.startOptionPromptCol-10;
			int agentXRow = terminal.getTerminalWidth()/40;
			boolean hasNextAgent = true;
			int agentDescriptionRows = 5; int jumpLines = 2;
			for(int i = 0; i<rules.size(); i+=agentXRow) {
				Ansi headers = Ansi.ansi(); Ansi names = Ansi.ansi(); Ansi trapoid = Ansi.ansi();
				Ansi actionsAnsi = Ansi.ansi(); Ansi readComms = Ansi.ansi();
				//  HEADERS
				try {
					for(int j = i, width=35, ag = 0; j < agentXRow + i; j++, ag++){
						headers.a(Ansi.ansi().cursor(terminal.currentPromptRow, terminal.currentPromptCol + width * ag)
								.a(terminal.ansiMenuOption).a(" - RULE - "));
					
						names.a(Ansi.ansi().cursor(terminal.currentPromptRow + 1, terminal.currentPromptCol + width * ag)
								.a(terminal.ansiPrompt).a("Name: ").a(terminal.ansiDefault).a(rules.get(j).getName()));
					
						trapoid.a(Ansi.ansi().cursor(terminal.currentPromptRow + 2, terminal.currentPromptCol + width * ag)
								.a(terminal.ansiPrompt).a("OID: ").a(terminal.ansiDefault).a(rules.get(j).getOID().toString()));
					
						String ruleActions = "[";
						for(int action : rules.get(j).getActions()) {
							ruleActions += String.valueOf(action) + ",";
						}
						
						ruleActions = ruleActions.substring(0, ruleActions.length()-1);
						ruleActions += "]";
						
						actionsAnsi.a(Ansi.ansi().cursor(terminal.currentPromptRow + 3, terminal.currentPromptCol + width * ag)
								.a(terminal.ansiPrompt).a("Actions: ").a(terminal.ansiDefault).a(ruleActions));
					
					}
				}catch(IndexOutOfBoundsException exc) {hasNextAgent = false;}
				
				terminal.print(headers.a(terminal.ansiDefault).a("\n"));
				terminal.print(names.a("\n"));
				terminal.print(trapoid.a("\n"));
				terminal.print(actionsAnsi.a("\n"));
				
				terminal.currentPromptRow += agentDescriptionRows;
				terminal.jumpLines(jumpLines);
				
				if(!hasNextAgent)
					break;
		
			}

			this.pressEnterToContinue();
			break;
		default:
			break;
			
		}
		this.pressEnterToContinue();
		
	}
	
	// ······ (9) NEW PROCESS ·······
	private void createSNMPProcess() {
		this.printMenuOptionHeader("NEW PROCESS");
		long sleepTime = 0;
	
		String[] oids;
		String name, descr;
		boolean askAgain = true;
		
		int agentID = this.askForAgent("NEW PROCESS");
		
		name = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("Process Name (may be empty): "));
		descr = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("Description (may be empty): "));
		// ··· SET SLEEPTIME
		askAgain = true;
		do {
			try { //TODO IMPLEMENT DIFFERENT TIME MEASURES!
				sleepTime = Long.parseLong(terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("SleepTime > 1000 (ms) : ")));
				if(sleepTime > 1000) {
					askAgain = false;
				}else {
					terminal.coutError("Error: SleepTime needs to be higher than 1s (1000s)");
				}
			}catch(NumberFormatException nfe) {
				terminal.coutError("Error: Input must be numeric! (Remember -> milliseconds)");
			}
		}while(askAgain);
		
		// ··· SET OIDS
		oids = this.askOIDs();
		
		String processID;
		// ··· ASK IF CREATE
		if(this.askYesOrNo("Create Proccess(y/n): ")) {
			
			terminal.cout(Ansi.ansi().a(terminal.ansiPrompt).a("Creating new Process..."));
			processID = ctrl.createSNMPProcess(name, descr, agentID, sleepTime, oids);
			terminal.cout(Ansi.ansi().a(terminal.ansiPrompt).a("ProcessID: " + processID));
			terminal.coutNewLine();
			
		}else {
			terminal.cout(Ansi.ansi().a(terminal.ansiInfo).a("Process creation cancelled"));
			this.pressEnterToContinue();
			return;
		}
		
		// ··· ASK IF SAVE RESPONSES
		if(this.askYesOrNo("Save Response PDUs in DB(y/n)")) {
			this.ctrl.setProcessSaveDataInDB(true, processID);
		}else {
			this.ctrl.setProcessSaveDataInDB(false, processID);
			terminal.coutWarning("Warning: Process data will not be saved");
		}
		
		// ··· ASK IF VERBOSE
		this.ctrl.setProcessVerbose(this.askYesOrNo("Verbose Results (y/n)"), processID);
		
		// ··· ASK IF SAVE PROCESS
		if(this.askYesOrNo("Save Process In DB(y/n): ")) {
			terminal.cout(Ansi.ansi().a(terminal.ansiPrompt).a("Saving Process..."));
			ctrl.dbAddProcess(processID); //TODO
			
		}else {
			terminal.coutWarning("Warning: Process will be deleted after exiting the program");
		}
		
		// ··· ASK IF START
		if(this.askYesOrNo("Start Process? (y/n): ")) {
			this.terminal.cout(Ansi.ansi().a(terminal.ansiPrompt).a("Starting Process... (PID: " + processID + ")"));
			this.ctrl.startProcess(processID);
			this.sysStats.put(INFO.PROCESSES,String.valueOf(this.ctrl.getTotalRunningProcesses()));
			terminal.cout(Ansi.ansi().a(terminal.ansiPrompt).a("Done!"));
		}
		
		this.pressEnterToContinue();
		return;
	}
	
	
	// ······ (10) SHOW PROCESS
	private void showProcess() {
		this.printMenuOptionHeader("SHOW PROCESS");
		
		List<SNMPProcess> processList = (List<SNMPProcess>) this.ctrl.getAllSNMPProcesses();
		if(processList.size() == 0) {
			terminal.currentPromptCol = (terminal.getTerminalWidth() - "NO SNMP PROCESSES ARE CONFIGURED".length()) / 2;
			terminal.coutWarning("NO SNMP PROCESSES ARE CONFIGURED");
			terminal.coutNewLine();
			this.pressEnterToContinue();
			return;
		}
		terminal.currentPromptCol = terminal.startOptionPromptCol-10;
		int agentXRow = terminal.getTerminalWidth()/40;
		boolean hasNextAgent = true;
		int agentDescriptionRows = 5; int jumpLines = 2;
		for(int i = 0; i<processList.size(); i+=agentXRow) {
			Ansi headers = Ansi.ansi(); Ansi names = Ansi.ansi(); Ansi addresses = Ansi.ansi();
			Ansi states = Ansi.ansi(); Ansi readComms = Ansi.ansi();
			//  HEADERS
			try {
				for(int j = i, width=35, ag = 0; j < agentXRow + i; j++, ag++){
					headers.a(Ansi.ansi().cursor(terminal.currentPromptRow, terminal.currentPromptCol + width * ag)
							.a(terminal.ansiMenuOption).a(" - PROCESS " + processList.get(j).getProcessID() + " - "));
				
					names.a(Ansi.ansi().cursor(terminal.currentPromptRow + 1, terminal.currentPromptCol + width * ag)
							.a(terminal.ansiPrompt).a("Name: ").a(terminal.ansiDefault).a(processList.get(j).getName()));
				
					addresses.a(Ansi.ansi().cursor(terminal.currentPromptRow + 2, terminal.currentPromptCol + width * ag)
							.a(terminal.ansiPrompt).a("Interval: ").a(terminal.ansiDefault).a(processList.get(j).getSleepTime()));
				
					states.a(Ansi.ansi().cursor(terminal.currentPromptRow + 3, terminal.currentPromptCol + width * ag)
							.a(terminal.ansiPrompt).a("State: ").a(terminal.ansiDefault).a((processList.get(j).isRunning()) ? "Running" : "Stopped"));
				
					readComms.a(Ansi.ansi().cursor(terminal.currentPromptRow + 4, terminal.currentPromptCol + width * ag)
							.a(terminal.ansiPrompt).a("AgentID: ").a(terminal.ansiDefault).a(processList.get(j).getAgendID()));
				}
			}catch(IndexOutOfBoundsException exc) {hasNextAgent = false;}
			
			terminal.print(headers.a(terminal.ansiDefault).a("\n"));
			terminal.print(names.a("\n"));
			terminal.print(addresses.a("\n"));
			terminal.print(states.a("\n"));
			terminal.print(readComms.a("\n"));
			
			terminal.currentPromptRow += agentDescriptionRows;
			terminal.jumpLines(jumpLines);
			
			if(!hasNextAgent)
				break;
	
		}

		this.pressEnterToContinue();
	}
	// ······ (11) MOD PROCESSES
	
	// TODO 
	private void modProcess() {
		
		String op;
		String name = null;
		String description = null;
		long sleepTime = -1;
		byte saveData = -1, show = -1, running = -1;
		boolean quit = false;
		this.printMenuOptionHeader("MODIFY PROCESS");
		if(this.ctrl.getAllSNMPProcesses().size() == 0) {
			terminal.currentPromptCol = (terminal.getTerminalWidth() - "NO SNMP PROCESSES ARE CONFIGURED".length()) / 2;
			terminal.coutWarning("NO SNMP PROCESSES ARE CONFIGURED");
			terminal.coutNewLine();
			this.pressEnterToContinue();
			return;
		}
		SNMPProcess ps = this.askForProcess("MODIFY PROCESS");

		
		do{
			this.printMenuOptionHeader("PROCESS " + ps.getProcessID());
			this.terminal.cout(Ansi.ansi().a(terminal.ansiGood + "(1)" + terminal.ansiInfo2 + "Name: " + 
					terminal.ansiDefault + ((name == null) ? ps.getName() : name )));
			this.terminal.cout(Ansi.ansi().a(terminal.ansiGood + "(2)" + terminal.ansiInfo2 + "Description: " + 
					terminal.ansiDefault + ((description == null) ? ps.getDescription() : description )));
			this.terminal.cout(Ansi.ansi().a(terminal.ansiGood + "(3)" + terminal.ansiInfo2 +"SleepTime: " + 
					terminal.ansiDefault + ((sleepTime == -1) ? ps.getSleepTime() : sleepTime )));
			this.terminal.cout(Ansi.ansi().a(terminal.ansiGood + "(4)" + terminal.ansiInfo2 +"SaveData: " +
					terminal.ansiDefault + ((saveData == -1) ? ps.getSaveInDB() : (saveData == 1) ? true : false )));
			this.terminal.cout(Ansi.ansi().a(terminal.ansiGood + "(5)" + terminal.ansiInfo2 +"Show: " + 
					terminal.ansiDefault + ((show == -1) ? ps.getShowResponse() : (show == 1) ? true : false )));
			this.terminal.cout(Ansi.ansi().a(terminal.ansiGood + "(6)" + terminal.ansiInfo2 +"Executing: " + 
					terminal.ansiDefault + ((running == -1) ? ps.isRunning() : (running == 1) ? true : false )));
			this.terminal.cout(Ansi.ansi().a(terminal.ansiPrompt + "---------------------------------"));
			this.terminal.cout(Ansi.ansi().a(terminal.ansiGood + "(7)" + terminal.ansiDefault + "Save and exit"));
			this.terminal.cout(Ansi.ansi().a(terminal.ansiGood + "(8)" + terminal.ansiDefault + "Exit"));
		
			op = this.terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt + "Configure: "));
		
			switch(op) {
			case "1":
				name = this.terminal.readInput(Ansi.ansi().a(this.terminal.ansiPrompt + "New Process Name: "));
				break;
			case "2":
				description = this.terminal.readInput(Ansi.ansi().a(this.terminal.ansiPrompt + "New Description: "));
				break;
			case "3":
				String sT = this.terminal.readInput(Ansi.ansi().a(this.terminal.ansiPrompt + "New Sleep Time: "));
				try {
					sleepTime = Long.parseLong(sT);
				}catch(NumberFormatException nfe) {
					sleepTime = -1;
					terminal.coutError("SleepTime must be a number in miliseconds");
				}
				break;
			case "4":
				saveData = (byte) ((this.askYesOrNo("Save Data (y/n): ")) ? 1 : 0);
				break;
			case "5":
				show = (byte) ((this.askYesOrNo("Show process (y/n): ")) ? 1 : 0);
				break;
			case "6":
				running = (byte) ((this.askYesOrNo("Run process (y/n): ")) ? 1 : 0);
				break;
			case "7":
				this.terminal.cout(Ansi.ansi().a(terminal.ansiInfo2 + "Saving data..."));
				
				if(this.ctrl.updateProcess(ps.getProcessID(), (name == null) ? ps.getName() : name,
						(description == null) ? ps.getDescription() : description,
								(sleepTime == -1) ? ps.getSleepTime() : sleepTime,
										(byte)((saveData == -1) ? (ps.getSaveInDB()) ? 1 : 0 : saveData),
										(byte)((show == -1) ? (ps.getShowResponse()) ? 1 : 0 : show),
										(byte)((running == -1) ? (ps.isRunning()) ? 1 : 0 : running)))
					this.terminal.cout(Ansi.ansi().a(terminal.ansiGood + "Success!"));
				else
					this.terminal.coutError("Error: Unable to updated Process in DB. Check log.");
				quit = true;
				break;
			case "8":
				this.terminal.cout(Ansi.ansi().a(terminal.ansiInfo2 + "Exiting..."));
				quit = true;
				break;
			default:
				this.terminal.coutError("Error: Unknown field");
				
			}
		}while(!quit);
		
		this.terminal.reset(ctrl.getFullCtrlInfo());
		
		
	}
	// ······ (12) DEL PROCESS
	private void deleteProcess() {
		
		this.printMenuOptionHeader("DELETE PROCESS");
		SNMPProcess ps = this.askForProcess("DELETE PROCESS");
		if(this.askYesOrNo("You sure you want to delete selected process?")) {
			this.ctrl.deleteSNMPProcess(ps.getProcessID());
		}else
			this.terminal.cout(Ansi.ansi().a(terminal.ansiGood + "Canceling process delete"));
		
		this.pressEnterToContinue();
	}
	
	// ······ (13) SNMPMANAGER
	private void configureSNMPManager() {
		
	}
	
	// ······ (14) JSMP-SHELL --> NO NEED 4 EXTRA METHOD
	// ······ (15) EXIT --> NO NEED 4 EXTRA METHODD
	
	
	
	
	
	// #####################   H E L P F U L  M E T H O D S   #####################
	public int askForAgent(String returnHeader) {
		int agentID = -1;
		boolean askAgain = true;
		do {
			String sAgent = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("Select an agent  (ID / Name / ?): "));
			if(sAgent.equals("?")) {
				terminal.coutNewLine();
				this.showAgents();
				this.printMenuOptionHeader(returnHeader);
			}else {
				try {
					agentID = ctrl.managerGetAgent(Integer.parseInt(sAgent)).getId();
					askAgain = false;
				}catch(NumberFormatException nfe) {
					try {
					agentID = (int) ctrl.managerGetAgents().stream()
							.filter((SNMPAgent agent) -> agent.getName().equals(sAgent)).map(SNMPAgent::getId).toArray()[0];
					askAgain = false;
					}catch(NullPointerException | ArrayIndexOutOfBoundsException exc) {
						terminal.coutError("Error: Cannot find Agent with Name = \'" + sAgent + "\'");
					}catch(ClassCastException cce) {
						terminal.coutError("Error: Unknown Error ocurred");
						return -1;
					}
				}catch(NullPointerException npe) {
					terminal.coutError("Error: Cannot find Agent with ID = \'" + sAgent + "\'");
				}
			}
			}while(askAgain);
		return agentID;
	}
	
	public SNMPProcess askForProcess(String returnHeader) {
		String choice;
		while(true) {
			choice = terminal.readInput(Ansi.ansi().a("Select a process (ID / Name / ?): "));
			if(choice.equals("?")) {
				terminal.coutNewLine();
				this.showProcess();
				this.printMenuOptionHeader(returnHeader);
			}else {
				SNMPProcess process = this.ctrl.getProcess(choice);
				if(process != null) {
					return process;
				}else {
					process = this.ctrl.getProcessByName(choice);
					if(process != null) {
						return process;
					}else {
						terminal.coutError("Error: No such process id or name");
					}
				}
			}
		}
	}
	
	
	public void printMenuOptionHeader(String option) {
		String title = "              -  "+option+"  -              ";
		this.terminal.deleteLastLines(this.terminal.currentPromptRow - this.terminal.startOptionPromptRow);
		this.terminal.currentPromptCol = this.terminal.getTerminalWidth()/2 - title.length()/2;
		this.terminal.cout(Ansi.ansi().a(this.terminal.ansiMenuOption).a(title));
		this.terminal.currentPromptCol = this.terminal.startOptionPromptCol;
		this.terminal.coutNewLine();
	}
	
	public void pressEnterToContinue() {
		this.terminal.coutNewLine();
		this.terminal.currentPromptCol = (this.terminal.getTerminalWidth() - "  -- PRESS ENTER TO CONTINUE --  ".length()) / 2;
		this.terminal.readInput(Ansi.ansi().a(this.terminal.ansiMenuOption).a("  -- PRESS ENTER TO CONTINUE --  ").a(this.terminal.ansiDefault));
		this.terminal.reset(ctrl.getFullCtrlInfo());
	}
	
	/** Takes a String as argument, printing the string content and prompting for an answer. <br>
	 * If answer = 'yes' or 'y' returns true <br>
	 * If answer = 'no' or 'n' returns false <br>
	 * @param message
	 * @return boolean
	 */
	public boolean askYesOrNo(String message) {
		String ans;
		do {
			this.terminal.cout(Ansi.ansi().a(this.terminal.ansiPrompt).a(message));
			ans = this.terminal.readInput(Ansi.ansi().a(this.terminal.ansiPrompt).a(">"));
		
		if(ans.equals("y") || ans.equals("yes"))
			return true;
		
		if(ans.equals("n") || ans.equals("no"))
			return false;
		
		this.terminal.coutError("Error: Anser must be either \"yes\" or \"no\"");
		
		}while(true);
	}
	
	
	public String[] askOIDs() {

		boolean askAgain = true;
		String[] oids = null;
		
		JSNMPUtil.TEST_OIDS.forEach((key, value) -> {
			this.terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a("("+key+") - "+ value[0] + " - " + value[1]));
		});
		
		do {
			String sOID = this.terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("ID OIDs: "));
			try {
				oids = Arrays.asList(sOID.split(",")).stream().map(String::trim).map(Integer::parseInt).map((Integer index) -> {return JSNMPUtil.TEST_OIDS.get(index)[1];}).collect(Collectors.toList()).toArray(new String[0]);
				
				/*int ioid = Integer.parseInt(sOID);
				oid = JSNMPUtil.TEST_OIDS.get(ioid)[1];*/
				for(String oid : oids) {
					
					if(oid == null) {
						terminal.coutError("Error: Invalid OID ID");
						askAgain = true;
					}else
						askAgain = false;
				}
			}catch(NumberFormatException nfe) {
				this.terminal.coutError("Error: Expected type int for OID ID");
				askAgain = true;
			}catch(IndexOutOfBoundsException  iob) {
				this.terminal.coutError("Error: Inavlid OID ID");
				askAgain = true;
			}catch(NullPointerException npo) {
				this.terminal.coutError("Error: Cannot find select OID index");
				askAgain = true;
			}
		}while(askAgain);
		
		return oids;
	}



// ###########################     A B S T R A C T   M O N I T O R    M E T H O D S    ###########################

	@Override
	public void processProcessWhisper(ProcessWhisper processWhisper) {
		
		if(this.tvc.exists(processWhisper.getProcessID())){
			try {
				this.tvc.sendData(processWhisper.getProcessID(), processWhisper.getResponsePDU());
			} catch (IOException e) {
				terminal.coutError("Error: Cannot send data to display to TProcessViewer with ProcessID = " + processWhisper.getProcessID());
				this.ctrl.setProcessVerbose(false, processWhisper.getProcessID());
				this.tvc.removeHandler(processWhisper.getProcessID());
			}
		}else {
			String tvcToken = processWhisper.getProcessID() + Math.random()*9999+10000;
			
			try {
				terminal.coutNewLine();
				terminal.cout(Ansi.ansi().a(terminal.ansiPrompt + "Starting new ProcessViewer..."));
				tvc.acceptNewConnection(tvcToken, processWhisper);
			} catch (IOException e) {
				terminal.coutError("Error: IO Error in process verbose for ProcessID = " + processWhisper.getProcessID());
				this.ctrl.setProcessVerbose(false, processWhisper.getProcessID());
			} catch (TPVAuthenticationException e) {
				terminal.coutError("Error: TPVAuthentication error in process verbose for ProcessID = " + processWhisper.getProcessID());
				this.ctrl.setProcessVerbose(false, processWhisper.getProcessID());
			}
		}
		
	}

	@Override
	public void processRequestWhisper(RequestWhisper requestWhisper) {
		synchronized(this) {
		PDU response = requestWhisper.getResponsePDU();
		if(response != null) {
			if(response.getVariableBindings().size() > 0) {
				response.getVariableBindings().forEach(var -> {
					terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a("Response:"));
					terminal.cout(Ansi.ansi().a(terminal.ansiWarning + "OID: " + terminal.ansiDefault + var.getOid().toDottedString()
					+" ---> " + terminal.ansiDefault + var.getVariable().toString()));
		
				});
			}else {
				terminal.cout(Ansi.ansi().a(terminal.ansiError).a("Warning: Bulk Request return no variableBindings..."));
			}
			
			if(this.ctrl.getDataBaseStatus() && response.getType() == PDU.GET && this.askYesOrNo("Do you wish to save PDU in Database?")) {

				this.terminal.cout(Ansi.ansi().a((this.ctrl.dbAddPDU(response, requestWhisper.getAgentID()) ? this.terminal.ansiGood + "PDU saved!"  : "PDU couldnt be saved. Check log.")));			
			}
		}else {
			terminal.cout(Ansi.ansi().a(terminal.ansiError).a("Error: No response. Agent may be unreachable."));
			
		}
		terminal.coutNewLine();
		
		this.notify();
		}
	}

	@Override
	public void processTrapWhisper(TrapWhisper trapWhisper) {
		
		//System.out.println("GOT TRAP!!!");
		JSNMPUtil.makeSounds("C:\\Windows\\Media\\chord.wav");
		
		for(Rule rule : trapWhisper.getRules()) {
			if(trapWhisper.getPDU().getVariableBindings().get(2).getOid().equals(rule.getOID()))
				for(int action : rule.getActions()) {

					if(action == Rule.ACTION_EXTRA_BEEP) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
						}
						JSNMPUtil.makeSounds("C:\\Windows\\Media\\chord.wav");
						break;
					}
				}
		}
		
	
        
	}


	
	
	
	
	
	
}

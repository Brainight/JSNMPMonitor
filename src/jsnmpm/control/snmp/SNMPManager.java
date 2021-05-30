package jsnmpm.control.snmp;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.stream.Collectors;


import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.smi.Address;

import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import jsnmpm.control.process.SNMPProcess;
import jsnmpm.control.utilities.Rule;
import jsnmpm.control.utilities.JSNMPUtil;
import jsnmpm.control.whisper.ProcessWhisper;
import jsnmpm.control.whisper.RequestWhisper;
import jsnmpm.control.whisper.Whisper;

/**
 * @SNMPVersion SNMPv2c
 * @author MrStonedDog
 *
 */
public class SNMPManager implements ResponseListener, Publisher<Whisper>{
	
	//STATIC PARAMETERS
	
	// INSTANCE VARIABLES
	Map<Integer, SNMPAgent> agentsMap = null;
	private Snmp snmpHandler = null; 
	
	// TRAP HANDLER
	private SNMPTrapHandler trapHandler = null;
	
	/** Traps recieved still in "cache" **/
	private List<PDU> trapPDUList = null;
	
	/** <ProccessID, SNMPProccess> **/
	private Map<String,SNMPProcess> snmpProcessMap = null;
	
	/** Communication with main controller **/
	Subscriber<Whisper> control = null;
	
	
	public SNMPManager(Subscriber<Whisper> control, String[]transports) throws IOException {
		this.control = control;
		this.trapHandler = new SNMPTrapHandler(this);
		this.setConfiguration(transports);
		this.agentsMap = new HashMap<Integer, SNMPAgent>();
		this.snmpProcessMap = new HashMap<String, SNMPProcess>();
		this.trapPDUList = new ArrayList<PDU>();
		this.snmpHandler.listen();
		
	}
	
	private void setConfiguration(String...trans) throws IOException {
		
		// SETTING TRANSPORT MAPPINGS
		if(trans == null || trans[0] == null ||  trans[0].isEmpty()) {
			
			this.snmpHandler = new Snmp(new DefaultUdpTransportMapping(new UdpAddress("0.0.0.0/6666")));
			this.snmpHandler.addTransportMapping(new DefaultUdpTransportMapping(new UdpAddress("0.0.0.0/162")));
			
		}else {
			int i = 0;
			for(String transport : trans) {
				String[] tran = transport.split("/");
				
				if(JSNMPUtil.isValidIP(tran[0]) && JSNMPUtil.isValidPort(tran[1])) {
					if(i == 0)
						this.snmpHandler = new Snmp(new DefaultUdpTransportMapping(new UdpAddress(transport)));
					else
						this.snmpHandler.addTransportMapping(new DefaultUdpTransportMapping(new UdpAddress(transport)));
					i++;
				}
				else {}
					//TODO WRITE WRONG SETTING IN LISTEN FIELD IN CONF FILE
				
				
			}
			// CHECK FOR ERROR IN CONFIGURATION FILES
			if(i == 0) {
				this.snmpHandler = new Snmp(new DefaultUdpTransportMapping(new UdpAddress("0.0.0.0/6666")));
				this.snmpHandler.addTransportMapping(new DefaultUdpTransportMapping(new UdpAddress("0.0.0.0/162")));
			}
		}
		
		this.snmpHandler.addCommandResponder(trapHandler);
	}
	
	// ########################## PUBLIC METHODS ##########################
	
	// ·········   SNMP    ·········
	public int getTotalTraps() {
		return this.trapPDUList.size();
	}
	
	/** Returns the total number of processes. You may filter the processes by active, stopped, or all.
	 * Active = 1 <br/>
	 * Stopped = 2 <br/>
	 * All = 3 <br/>
	 * @return The number of processes.
	 */
	public int getNumberProcesses(int op) {
		
		switch(op) {
		case 1:
			return (int) this.snmpProcessMap.entrySet().stream().filter( e -> e.getValue().isRunning()).count();
		case 2:
			return (int) this.snmpProcessMap.entrySet().stream().filter( e -> !e.getValue().isRunning()).count();

		case 3:
			default:
			return this.snmpProcessMap.size();
		}
	}
	
	/**
	 * Return a collection with all available SNMPProcesses.
	 * @return
	 */
	public List<SNMPProcess> getProcesses() {
		return new ArrayList<SNMPProcess>(this.snmpProcessMap.values());
	}

	// ·········  NETWORK   ········
	/** Return a string with all transportmapping. The format for a transportmapping is "ip/port"
	 * If there is more than one, they will be separated by "," without spaces.
	 * @return
	 */
	public String getTransportMappingInfo() {
		
		String trans = "";
		for(TransportMapping<?> transport : this.snmpHandler.getMessageDispatcher().getTransportMappings()) {
			trans+=transport.getListenAddress().toString()+ " ";
		}
		return trans;
		
	}
	
	
	
	// ········ AGENT CONFIGURATION ·········
	
	/**
	 * Adds a SNMPAgent to the Manager Agent List.
	 * @param agent
	 */
	public synchronized void addAgent(SNMPAgent agent) {
		this.agentsMap.put(agent.getId(), agent);
	}
	
	public synchronized void addAllAgents(List<SNMPAgent> agents) {
		this.agentsMap.putAll(agents.stream().collect(Collectors.toMap(SNMPAgent::getId, SNMPAgent::getMyself)));
	}
	
	public boolean deleteAgent(int agent_id) {
		return (agentsMap.remove(agent_id) != null);
	}
	
	public synchronized void modifyAgent(int agent_id) {
		//TODO
	}
	
	// ········ AGENTS HANDLING ·············
	
	
	/**
	 * Returns all the SNMPAgent instances available in Agent Map
	 * @return
	 */
	public ArrayList<SNMPAgent> getAgents() {
		return new ArrayList<SNMPAgent>(agentsMap.values());
	}
	
	/**
	 * Returns the SNMPAgent instance with key = @param agent_id
	 * @param agent_id
	 * @return
	 */
	public SNMPAgent getAgent(int agent_id) {
		return agentsMap.get(agent_id);
	}
	
	
	// ········ SENDING AND RECIEVING
	/** Synchronized method to send a SNMP-REQUEST to a SNMPAgent with the given OIDs.
	 * 
	 * @param agent_id
	 * @param oid
	 * @return
	 */
	public synchronized ResponseEvent<Address> sendSyncGET(int agent_id, String...oid) {
	
		try {
			ResponseEvent<Address> response = this.snmpHandler.send(JSNMPUtil.createPDU(oid), this.agentsMap.get(agent_id).getCommunityTarget());
			this.agentsMap.get(agent_id).insertData(response.getResponse());
			return response;
	
		} catch (IOException e) {
			
			return null;
		}
	}
	
	
	/**
	 * Sends asynchronous SNMP-REQUEST.
	 * @param pdu
	 * @param target
	 * @param listener
	 * @throws IOException
	 */
	public void sendAsync(PDU pdu, CommunityTarget<Address> target, Object handler) throws IOException {
		this.snmpHandler.send(pdu, target, handler, this);
	}
	
	@Override
	public void subscribe(Subscriber<? super Whisper> subscriber) {}
	
	
	// ##########################    R E S P O N S E   L I S T E N E R     #################################
	@Override
	public synchronized <A extends Address> void onResponse(ResponseEvent<A> event) { //TODO
		
		((Snmp)event.getSource()).cancel(event.getRequest(), this); // CANCELING THE ASYNC RESPONSE WHEN RECIEVED
		  
		// ······  HANDLING PROCESS RESPONSE
		if(event.getUserObject() != null && event.getUserObject() instanceof SNMPProcess) { 
			  
			  this.control.onNext(new ProcessWhisper(((SNMPProcess)event.getUserObject()).getProcessID(),
					  ((SNMPProcess)event.getUserObject()).getAgendID(),
					  ProcessWhisper.RESPONSE, event.getResponse()));
			  
		// ······  HANDLING SIMPLE REQUEST RESPONSE
		}else {
			
			this.control.onNext(new RequestWhisper(((RequestWhisper)event.getUserObject()).getAgentID(),event.getResponse()));
			 
		}
	}
	
	// !!!!!!!!!!!!!! THIS IS NOW IMPLEMENTED WITH A NEW CLASS "SNMPTRAPHANDLER"  !!!!!!!!!!!!!!!!!!!!!!!1
	/*// ###########################    C O M M A N D    R E S P O N D E R    ########################################
	// Used for receiving TRAPS, NOTIFICATIONS and other type of PDU.
	@Override
	public <A extends Address> void processPdu(CommandResponderEvent<A> crevent) {
		
		PDU pdu = crevent.getPDU();
		if(pdu != null && pdu.getType() == PDU.TRAP || pdu.getType() == PDU.V1TRAP){
			this.trapPDUList.add(pdu);
			int agentID = -1;
			for(SNMPAgent agent : this.agentsMap.values()) {
				if(agent.getIp().equals(crevent.getPeerAddress().toString().split("/")[0])){
					agentID = agent.getId();
					break;
				}
			}
			
			this.control.onNext(new TrapWhisper(crevent.getPeerAddress().toString(), agentID, crevent.getPDU()));
		}
		
	}*/
	
// #############################    T R A P   H A N D L I N G     #########################
	
	public Rule addTrapHandlerRule(String name, String oid, int...actions) {
		return this.trapHandler.addTrapHandlerRule(name, oid, actions);
	}
	
	public Rule addTrapHandlerRule(String name, String oid, String mailSubject, String mailtext, int...actions) {
		return this.trapHandler.addTrapHandlerRule(name, oid, mailSubject, mailtext,  actions);
	}
	
	public List<Rule> getTrapHandlerRules(){
		return this.trapHandler.getRules();
	}
	
	public void addAllTrapHandlerRules(List<Rule> rules) {
		this.trapHandler.addAllTHRules(rules);
	}
	
	public boolean setMailSenderForTrapHandler(String smtpServer, String smtpPort, String mailAccount, String mailPass) {
		return this.trapHandler.setMailSender(smtpServer, smtpPort, mailAccount, mailPass);
	}
	
	
//  ############################   P R O C E S S O R    M E T H O D S   ########################
	
	/**
	 * Creates a new Process. This process is accessible from the Map(snmpPSList). Creating a process does not start it.
	 * To do so, call @method startProcess(processID). To stop it, call stopProcess(processID). Once stopped, the Thread executing will be killed.
	 * To start a new Thread call @method startProcess(processID). One SNMPProcess must be executed only by one Thread at a time.
	 * 
	 * Returns the processID for this new SNMPProcess.
	 * @param agentID
	 * @param sleepTime
	 * @param oids
	 * @return 
	 */
	public String createSNMPProcess(String name, String descr, int agentID, long sleepTime, Subscriber<Whisper> sub, String...oids) {
		String pid = DateTimeFormatter.ofPattern("yyMMddHHmmss").format(LocalDateTime.now());
		SNMPProcess pr = new SNMPProcess(pid,
				agentID, sleepTime, JSNMPUtil.createVariableBindingList(oids), sub);
		pr.setName(name);
		pr.setDescription(descr);
		
		this.snmpProcessMap.put(pr.getProcessID(), pr);
		return pr.getProcessID();
	}
	
	public void addAllProcess(List<SNMPProcess> processList) {
		for(SNMPProcess ps : processList) {
			ps.setSubscriber(this.control);
			this.snmpProcessMap.put(ps.getProcessID(), ps);
		}
	}
	
	public void updateProcess(String psID, String name, String descr, long sleepTime, byte saveData, byte show, byte running) {
		if(name != null)
			this.getProcess(psID).setName(name);
		
		if(descr != null)
			this.getProcess(psID).setDescription(descr);
		
		if(sleepTime > 1000)
			this.getProcess(psID).setSleepTime(sleepTime);
		
		if(saveData != -1) 
			this.getProcess(psID).setSaveInDB((saveData == 0) ? false : true);
		
		if(show != -1)
			this.getProcess(psID).setShowResponse((show == 0) ? false : true);
		
		if(running != -1) {
			this.getProcess(psID).setRunning((running == 0) ? false : true);
		}
	}
	
	/**
	 * Deletes a process. If the process is being executed when this method is called, pray to God he helps you.
	 * @param processID
	 */
	public void deleteSNMPProcess(String processID) {
		this.snmpProcessMap.remove(processID);
	}
	
	/**
	 * Starts a new Thread for the given process. If the process is being executed when this method is called, pray to God he helps you.
	 * @param processID
	 */
	public void startProcess(String processID) {
		this.snmpProcessMap.get(processID).start();
	}
	
	/**
	 * Stops the Thread executed by a process. 
	 * @param processID
	 * @throws InterruptedException 
	 */
	public void stopProcess(String processID) throws InterruptedException {
		
			this.snmpProcessMap.get(processID).stop();
	}
	
	public void getProcessData(String processID) {
		
	}
	
	public Set<String> getAllActiveProcessPID() {
		return this.snmpProcessMap.keySet().stream().filter((String key) -> this.snmpProcessMap.get(key).isRunning()).collect(Collectors.toSet());
	}
	
	public Set<String> getAllSNMPProcessPID() {
		return this.snmpProcessMap.keySet();
	}
	
	public SNMPProcess getProcess(String processID) {
		return this.snmpProcessMap.get(processID);
	}

	
}

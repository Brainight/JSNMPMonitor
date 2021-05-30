package jsnmpm.control.process;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.stream.Collectors;

import org.snmp4j.PDU;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

import jsnmpm.control.utilities.JSNMPUtil;
import jsnmpm.control.whisper.ProcessWhisper;
import jsnmpm.control.whisper.Whisper;

/**
 * This class is used for periodically monitoring a given SNMPAgent.
 * @author MrStonedDog
 */

public class SNMPProcess implements Runnable{
	
	private int type = PDU.GET;
	private String processID;
	private String name;
	private String description;
	private long sleepTime;
	private final int agentID;
	private Thread executer;
	
	private volatile boolean running = false;
	private List<VariableBinding> varBindings = null;
	private PDU pdu = null;
	private Map<OID, Variable> results = null;
	private Subscriber<Whisper> subscriber = null;
	private boolean saveDataInDB = true;
	private boolean showResponse = false;
	
	// #############   CONSTRUCTOR   ################
	public SNMPProcess(String processID, int agentID, long sleepTime, List<VariableBinding> varBindings, Subscriber<Whisper> sub) {
		this.processID = processID;
		this.agentID = agentID;
		this.sleepTime = sleepTime;
		this.varBindings = varBindings;
		this.results = this.varBindings.stream().collect(Collectors.toMap(VariableBinding::getOid, VariableBinding::getVariable));
		this.pdu = new PDU(this.type, varBindings);
		this.subscriber = sub;
	}
	
	public SNMPProcess(String psID, String psName, String psDescription, long sleepTime, boolean processSaveData, int psAgentID, String processOIDs) {
		this.processID = psID;
		this.name = psName;
		this.description = psDescription;
		this.sleepTime = sleepTime;
		this.saveDataInDB = processSaveData;
		this.agentID = psAgentID;
		this.varBindings = JSNMPUtil.createVariableBindingList(processOIDs.split(","));
		this.pdu = new PDU(this.type, varBindings);
	}
	
	// #########################    SETTERS AND GETTERS    #############################
	
	// ···· SETTERS
	public void setSubscriber(Subscriber<Whisper> subscriber) {
		this.subscriber = subscriber;
	}
	public void setSleepTime(long sleepTime2) {
		this.sleepTime = sleepTime2;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setDescription(String descr) {
		this.description = descr;
	}
	
	public void setVarBindings(List<VariableBinding> varBindings) {
		this.varBindings = varBindings;
		this.pdu = new PDU(this.type, this.varBindings);
	}
	
	public void setSaveInDB(boolean saveInDB) {
		this.saveDataInDB = saveInDB;
	}
	
	public void setShowResponse(boolean showResponse) {
		this.showResponse = showResponse;
	}
	
	public void setRunning(boolean running) {
		this.running = running;
		if(this.running) {
			this.start();
		}
	}
	
	// ···· GETTERS
	
	public String getProcessID() {
		return this.processID;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public long getSleepTime() {
		return this.sleepTime;
	}
	
	public int getAgendID() {
		return this.agentID;
	}
	
	public List<VariableBinding> getVarbindings() {
		return this.varBindings;
	}
	
	public boolean isRunning() {
		return this.running;
	}
	
	public boolean getSaveInDB() {
		return this.saveDataInDB;
	}
	
	public boolean getShowResponse() {
		return this.showResponse;
	}
	
	
	// #######################   P R O C E S S   M E T H O D S   ############################
	
	/**
	 * Creates a new Thread and runs this instance of SNMPProcess. 
	 */
	public void start() {
		this.executer = new Thread(this);
		this.executer.start();
	}
	
	/**
	 * Tries to kill the Thread that is being executed by this instance.
	 * @throws InterruptedException 
	 */
	public void stop() throws InterruptedException { 
		if(this.executer != null) {
			this.running = false;
			this.executer.join();
		}	
	}

	
	@Override
	public void run() {
		this.running = true;
		while(this.running) {
			
			try {
				this.subscriber.onNext(new ProcessWhisper(this.processID, this.agentID, ProcessWhisper.SEND_SNMP, this.pdu));
				Thread.sleep(this.sleepTime);
			} catch (InterruptedException e) {
				this.running = false;
			}
		}
	}
}

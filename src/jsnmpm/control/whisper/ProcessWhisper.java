package jsnmpm.control.whisper;

import org.snmp4j.PDU;
import org.snmp4j.event.ResponseListener;

public class ProcessWhisper implements Whisper{
	
	// TYPES OF DATA
	public final static byte SEND_SNMP = 1;
	public final static byte RESPONSE = 2;
	
	private int action = 0;
	private int agentID = 0;
	private String processID;;
	private PDU dataPDU = null;
	
	
	public ProcessWhisper(String processID, int agentID, int action, PDU data) {
		this.processID = processID;
		this.agentID = agentID;
		this.action = action;
		this.dataPDU = data;
	}
	
	public int getAction() {
		return this.action;
	}
	
	public int getAgentID() {
		return this.agentID;
	}
	
	public String getProcessID() {
		return this.processID;
	}
	
	public PDU getResponsePDU() {
		return this.dataPDU;
	}
	
}

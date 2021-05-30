package jsnmpm.control.whisper;

import org.snmp4j.PDU;

public class RequestWhisper implements Whisper{

	private int agentID;
	private PDU response = null;
	
	public RequestWhisper() {
		
	}
	
	public RequestWhisper(int agentID, PDU response) {
		this.agentID = agentID;
		this.response = response;
		
	}
	// ····· SETTERS
	
	public void setResponsePDU(PDU pdu) {
		this.response = pdu;
	}
	
	// ····· GETTERS
	public PDU getResponsePDU() {
		return this.response;
	}
	
	public int getAgentID() {
		return this.agentID;
	}
}

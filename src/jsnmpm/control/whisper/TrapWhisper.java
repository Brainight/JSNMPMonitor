package jsnmpm.control.whisper;


import java.util.List;


import org.snmp4j.PDU;

import jsnmpm.control.utilities.Rule;

public class TrapWhisper implements Whisper {

	private int agentID = -1;
	private String sourceAddress = null;
	private PDU trapPDU = null;
	private List<Rule> rules = null;
	
	public TrapWhisper(String sourceAddress, int agentID, PDU trapPdu,  List<Rule> rules) {
		
		this.sourceAddress = sourceAddress;
		this.agentID = agentID;
		this.trapPDU = trapPdu;
		this.rules = rules;
	}
	
	public int getAgentID() {
		return this.agentID;
	}
	
	public List<Rule> getRules() {
		return this.rules;
	}
	
	public String getSourceAddress() {
		return this.sourceAddress;
	}
	
	public PDU getPDU() {
		return this.trapPDU;
	}
}

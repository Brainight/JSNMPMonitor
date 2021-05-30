package jsnmpm.control.remoting.packets;

import java.io.Serializable;

import jsnmpm.control.snmp.SNMPAgent;

public class AgentData implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -848567005276937664L;
	private int agentID;
	private String name;
	private String ip;
	private int port;
	private String readCom;
	
	public AgentData() {
		
	}
	
	public AgentData(SNMPAgent agent) {
		this.agentID = agent.getId();
		this.name = agent.getName();
		this.ip = agent.getIp();
		this.port = agent.getPort();
		this.readCom = agent.getReadCommunity();
	}
	// SETTERS
	public void setAgentID(int id) {
		this.agentID = id;
	}
	
	public void setName(String name) {
		this.name  = name;
	}
	
	public void setAgentIP(String ip) {
		this.ip = ip;
	}
	
	public void setAgentPort(int port) {
		this.port = port;
	}
	
	public void setReadCom(String readCom) {
		this.readCom = readCom;
	}
	
	// GETTERS
	public int getAgentID() {
		return this.agentID;
	}
	
	public String getAgentName() {
		return this.name;
	}
	
	public String getAgentIP() {
		return this.ip;
	}
	
	public int getAgentPort() {
		return this.port;
	}
	
	public String getReadCom() {
		return this.readCom;
	}
}

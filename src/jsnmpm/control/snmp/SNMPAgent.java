package jsnmpm.control.snmp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow.Subscription;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;

import jsnmpm.control.utilities.JSNMPUtil;
import jsnmpm.control.whisper.ProcessWhisper;

/**
 * 
 * @author MrStonedDog
 *
 */
public class SNMPAgent {
	
	// ## STATIC ##
	public static final boolean UNREACHABLE = false;
	public static final boolean REACHABLE = true;
	
	// ## INSTANCE VARIABLES ##
	private InetAddress ip = null;
	private int port = 161;
	private boolean state = UNREACHABLE;
	private int id = -1;
	private String name = null;
	
	// ·· SNMP UTILS
	private Map<OID,PDU> pduList = null;  
	private CommunityTarget<Address> target = null;
	private OctetString readCommunity = null;
	
	
	// ##############  CONSTRUCTORS  ###############
	public SNMPAgent(String ip, int port, String name, String readCommunity) throws UnknownHostException {
		
		this.ip = InetAddress.getByName(ip);
		this.pduList = new HashMap<OID,PDU>();
		this.name = name;
		this.readCommunity = new OctetString(readCommunity);
		this.target = JSNMPUtil.createCommunityTarget(String.format("%s/%d", ip, port),this.readCommunity);
		this.target.setVersion(SnmpConstants.version2c);
		this.state = this.isReachable();
		
	}
	public SNMPAgent(String ip) throws UnknownHostException {
		
		this.ip = InetAddress.getByName(ip);
		this.pduList = new HashMap<OID,PDU>();
		this.readCommunity = new OctetString("");
		this.target = JSNMPUtil.createCommunityTarget(String.format("%s/%d", ip, this.port), this.readCommunity);
		this.target.setVersion(SnmpConstants.version2c);
		this.state = this.isReachable();
		
	}
	public SNMPAgent(String ip, int port) throws UnknownHostException {
		
		this.ip = InetAddress.getByName(ip);
		this.port = port;
		this.pduList = new HashMap<OID,PDU>();
		this.readCommunity = new OctetString("");
		this.target = JSNMPUtil.createCommunityTarget(String.format("%s/%d", ip, port), this.readCommunity);
		this.target.setVersion(SnmpConstants.version2c);
		this.state = this.isReachable();
	}
	
	public SNMPAgent(String ip, String readCommunity) throws UnknownHostException {
		this.ip = InetAddress.getByName(ip);
		this.pduList = new HashMap<OID,PDU>();
		this.readCommunity = new OctetString(readCommunity);
		this.target = JSNMPUtil.createCommunityTarget(String.format("%s/%d", ip, port), this.readCommunity);
		this.target.setVersion(SnmpConstants.version2c);
		this.state = this.isReachable();
	}
	
	public SNMPAgent(CommunityTarget<Address> target) throws UnknownHostException {
		
		String[] address = target.getAddress().toString().split("/");
		this.ip = InetAddress.getByName(address[0]);
		this.port = Integer.parseInt(address[1]);
		this.pduList =  new HashMap<OID,PDU>();
		this.target = target;
		this.readCommunity = target.getCommunity();
		this.state = this.isReachable();
	}
	
	// ################################# PRIVATE METHODS #######################################
	/**
	 * Appends data to @variable pduList. Data must be an instance of PDU.
	 * @param data
	 */
	protected void insertData(PDU pduData) {
			if(pduData != null)
				for(VariableBinding data : pduData.getVariableBindings()) {
					pduList.put(data.getOid(), pduData);
				}
	}
	// ################################## PUBLIC METHODS #######################################
	
	// ·················· HELPFUL METHODS
		
	public boolean isReachable() {
		try {
			return this.ip.isReachable(1000);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean isReachable(SNMPAgent agent) {
		try {
			return agent.ip.isReachable(1000);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	// ··· STATIC METHODS 

			
	// ··· INSTANCE METHODS
	
	// ········· SETTERS AND GETTERS ·············
	// Setters:
	public void setName(String name) {
		this.name = name;
	}
	
	public void setIP(String ip)  {
		InetAddress oldIP = this.ip;
		try {
		this.ip = InetAddress.getByName(ip);
		}catch(UnknownHostException uhe){
			this.ip = oldIP;
		}
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public void setReadCommunity(String community) {
		this.readCommunity = new OctetString(community);
		this.target = JSNMPUtil.createCommunityTarget(String.format("%s/%d", this.ip, this.port), this.readCommunity);
	}
	
	
	
	// Getters:
	public String getIp() {
		return this.ip.getHostAddress();
	}
	
	public int getPort() {
		return this.port;
	}
	
	public boolean getState() {
		return this.state;
	}
	
	public int getId() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public HashMap<OID,PDU> getData(){
		return (HashMap<OID, PDU>) this.pduList;
	}
	
	public String getReadCommunity() {
		return this.readCommunity.toString();
	}
	
	public CommunityTarget<Address> getCommunityTarget() {
		return this.target;
	}
	
	protected SNMPAgent getMyself() {
		return this;
	}
	


	
	
	
}

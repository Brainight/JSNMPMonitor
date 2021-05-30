package jsnmpm.control.utilities;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 * 
 * @author MrStonedDog
 *
 */
public class JSNMPUtil {
	
	// CONTROLER INFO
	public static enum INFO{
		
		DBACCESS,
		INTERFACES,
		AGENTS,
		TRAPS,
		PROCESSES;
	}
	
	/** Port: 161*/
	public final static int DEFAULT_SNMP_PORT1 = 161; 
	
	/** Port: 162 **/
	public final static int DEFAULT_SNMP_PORT2 = 162;
	
	public static Map<Integer, String[]> TEST_OIDS;

	public final static String RAM_OID = "1.3.6.1.2.1.25.2.2.0";
	public static String HOSTNAME_OID = "1.3.6.1.2.1.1.5.0";
    public static String RAM_USAGE = "1.3.6.1.2.1.25.2.3.1.4.1";
    public static String ifNumber = "1.3.6.1.4.1.311.1.1.3.1.1.1.1";
    public static String HEX_DATETIME = "1.3.6.1.2.1.25.1.2.0";
    public static String RND_CPU_USAGE = "1.3.6.1.2.1.25.3.3.1.2.6"; //1.3.6.1.2.1.25.3.3.1.2.x 
    public static String UBU_CPU_USAGE = "1.3.6.1.4.1.2021.10.1.3.1";
    public static String UBU_RAM_USED = "1.3.6.1.4.1.2021.4.6.0";
    public static String UBU_RAM_FREE = "1.3.6.1.4.1.2021.4.11.0";
    public static String UBU_TOTAL_RAM = "1.3.6.1.4.1.2021.4.3.0";
	static {
		TEST_OIDS = new HashMap<Integer, String[]>();
		TEST_OIDS.put(1, new String[] {"TOTAL_RAM",RAM_OID});
		TEST_OIDS.put(2, new String[] {"RAM_USAGE",RAM_USAGE});
		TEST_OIDS.put(3, new String[] {"HOSTNAME",HOSTNAME_OID});
		TEST_OIDS.put(4, new String[] {"IFNUMBER",ifNumber});
		TEST_OIDS.put(5, new String[] {"HEX_DATETIME",HEX_DATETIME});
		TEST_OIDS.put(6, new String[] {"RANDOM_CPU_USGAE",RND_CPU_USAGE});
		TEST_OIDS.put(7, new String[] {"UBU_RNDM_CPU_USAGE",UBU_CPU_USAGE});
		TEST_OIDS.put(8, new String[] {"UBU_RAM_USED",UBU_RAM_USED});
		TEST_OIDS.put(9, new String[] {"UBU_RAM_FREE",UBU_RAM_FREE});
		TEST_OIDS.put(10, new String[] {"UBU_TOTAL_RAM",UBU_TOTAL_RAM});
	}
	
	public final static String IP_PATTERN = "([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\."
			+ "([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\."
			+ "([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])";
	
	
	// ###########  METHODS FOR SNMP INSTACES  ############
	public static DefaultUdpTransportMapping createDefaultUDPTransport() {
		try {
			return new DefaultUdpTransportMapping();
		} catch (SocketException e) {
			return null;
		}
	}
	
	public static boolean isValidIP(String ip) {
			return Pattern.matches(IP_PATTERN, ip) ? true : false;
	}
	
	public static boolean isValidPort(String port) {
		try {
			
			if(Integer.parseInt(port) > 0 && Integer.parseInt(port) < 65535)
				return true;
			
		}catch(NumberFormatException nfe) {
			return false;
		}
		return false;
	}
	
	// ###########  METHDOS FOR OID's  ##############
	
	public static boolean isValidOID(String oid) {
		return new OID(oid).isValid();
	}
	
	public static List<VariableBinding> createVariableBindingList(String...oids){
		return Arrays.asList(oids).stream().map(OID::new).map(VariableBinding::new).collect(Collectors.toList());
	
	}
	
	
	// ###########  METHODS FOR PDU's  ##############
	/**
	 * Creates a PDU containing the given "oids". </br>
	 * Doesnt check if OID is valid or not.
	 * @param oids
	 * @return new PDU if oids.length >= 1 else null
	 */
	public static PDU createPDU(String ...oids) {
		if(oids.length == 1) {
			try {
				PDU pdu = new PDU();
				pdu.add(new VariableBinding(new OID(oids[0])));
				pdu.setType(PDU.GET);
				return pdu;
			}catch(Exception e) {
				return null;
			}
		}else if(oids.length > 1) {
			try {
				return new PDU(PDU.GET, createVariableBindingList(oids));
			}catch(Exception e) {
				return null;
			}
		}else
			return null;
	}
	
	/** Undone, dont use yet*/
	public static PDU createPDU(int type, String ...oids) {
			
		if(oids.length == 1) {
			try {
				PDU pdu = new PDU();
				pdu.add(new VariableBinding(new OID(oids[0])));
				pdu.setType(type);
				return pdu;
			}catch(Exception e) {
				return null;
			}
		}else if(oids.length > 1) {
			try {
				return new PDU(type, createVariableBindingList(oids));
			}catch(Exception e) {
				return null;
			}
		}else
			return null;
	}
	
	//
	// ############    METHODS FOR RESPONSEEVENTS    ###############
	/**
	 * Returns simplified PDU data recieved from as a GET-RESPONSE
	 * @param response
	 * @return
	 */
	public static List<String[]> getSimplifiedUniqueResponse(ResponseEvent<Address> response) { //TODO MAKE BETTER
		List<String[]> ans = new ArrayList<String[]>();
		for(VariableBinding var : response.getResponse().getVariableBindings())
			ans.add(new String[] {
					response.getPeerAddress().toString(),
					var.getOid().toDottedString(),
					var.getVariable().toString()});
		return ans;
	}
	
	
	
	// ############ METHODS FOR COMMUNITY_TARGETS  ##################
	/**
	 * Creates a new CommunityTarget.
	 * @param udpaddress
	 * @param community
	 * @param snmpVersion
	 * @return
	 */
	public static CommunityTarget<Address> createCommunityTarget(String udpaddress, OctetString community) {
			CommunityTarget<Address> target = new CommunityTarget<Address>();
		    target.setCommunity(new OctetString(community));
		    target.setAddress(GenericAddress.parse(udpaddress));
		    target.setVersion(SnmpConstants.version2c);
			return target;
	}
	
	
	// ·············································································································································
	
	/*
	 * Following methods are used for getting information about the system the SNMPManager will be executed on.
	 */
	
	private static Enumeration<NetworkInterface> getNetworkInterfaces() {
		try {
			return NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static void getAllInterfacesInfo() {
		String last_interface = null;
		for(NetworkInterface net_i: Collections.list(getNetworkInterfaces())) {
			for(InetAddress i_address : Collections.list(net_i.getInetAddresses()))
				
				if(i_address != null) {
					if(last_interface != net_i.getName()) {
						System.out.println("\nName: "+net_i.getName());
						System.out.println("Info: "+net_i.getDisplayName());
					}
					System.out.println(((i_address instanceof Inet4Address) ? "IPv4: " : "IPv6: ") + i_address.getHostAddress());
					last_interface = net_i.getName();
				}
		}
	}
	
	
	// #################  R A N D O M    U T I L I E S   ########################
	public static void makeSounds(String file) {
        Clip clip;
		try {
			clip = AudioSystem.getClip();
	        clip.open(AudioSystem.getAudioInputStream(new File(file)));
	        clip.start();
		} catch (LineUnavailableException e) {} catch (IOException e) {
			
		} catch (UnsupportedAudioFileException e) {
			
		}
	}
}

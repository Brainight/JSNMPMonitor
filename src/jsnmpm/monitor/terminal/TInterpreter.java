package jsnmpm.monitor.terminal;

import java.util.ArrayList;

import jsnmpm.control.SNMPController;

public class TInterpreter {

	// GENERIC
	public static final String SHUTDOWN = "shutdown";
	public static final String EXIT = "exit";
	
	// AGENT
	public static final String NEW_AGENT = "newagent";
	public static final String MOD_AGENT = "modagent";
	public static final String SHOW_AGENT = "showagent";
	public static final String DEL_AGENT = "delagent";
	
	//  SNMP
	public static final String SEND_GET = "send";
	public static final String SHOW_TRAP = "showtrap";
	public static final String CONF_TRAP = "conftrap";
	
	// PROCESSES
	public static final String ADD_PROCESS = "newprocess";
	public static final String MOD_PROCESS = "modprocess";
	public static final String SHOW_PROCESS = "showprocess";
	public static final String DEL_PROCESS = "delprocess";
	
	// UI
	public static final String UI_CONFIG = "configui";
	public static final String UPDATE = "update";
	public static final String RESIZE = "resize";
	
	// MANAGER
	public static final String SHOW_INTERFACE = "showinterface";
	
	public static void parse(String command, SNMPController ctrl) {
		String[] $ = command.split(" ");
		switch($[0]) {
		
		// GENERIC
		case SHUTDOWN:
			break;
		
		
		// AGENT
		case NEW_AGENT:
			break;
			
		case MOD_AGENT:
			break;
			
		case SHOW_AGENT:
			break;
			
		case DEL_AGENT:
			break;
			
			
			
		case EXIT:
			break;
		}
	}
	
	// ############################     F A K E   B I N A R I E S     #######################
	
	// GENERIC
	
	/**
	 * 
	 * @param $X
	 */
	private static void shutDown(String...$X) {
		
	}
	
	public static void update() {
		
	}
	/**
	 * 
	 * @param $X
	 */
	private static void resize(String...$X) {
		
	}
	
	
	/**
	 * 
	 * @param $X
	 */
	private static void exit() {
		
	}
	
	// AGENT
	
	/**
	 * 
	 * @param $X
	 */
	private static void newAgent(String...$X) {
		
	}
	
	/**
	 * 
	 * @param $X
	 */
	private static void showAgent(String...$X) {
		
	}
	
	/**
	 * 
	 * @param $X
	 */
	private static void modAgent(String...$X) {
		
	}
	
	/**
	 * 
	 * @param $X
	 */
	private static void delAgent(String...$X) {
		
	}
	
	//  SNMP
	
	/**
	 * 
	 * @param $X
	 */
	private static void send(String...$X) {
		
	}
	/**
	 * 
	 * @param $X
	 */
	private static void showTrap(String...$X) {
		
	}
	/**
	 * 
	 * @param $X
	 */
	private static void confTrap(String...$X) {
		
	}
	
	// PROCESSES
	/**
	 * 
	 * @param $X
	 */
	private static void newProcess(String...$X) {
		
	}
	/**
	 * 
	 * @param $X
	 */
	private static void showProcess(String...$X) {

	}
	/**
	 * 
	 * @param $X
	 */
	private static void modProcess(String...$X) {
		
	}
	/**
	 * 
	 * @param $X
	 */
	private static void delProcess(String...$X) {
		
	}
	
	// UI
	/**
	 * 
	 * @param $X
	 */
	private static void confiUI(String...$X) {
		
	}
	
	// MANAGER
	/**
	 * 
	 * @param $X
	 */
	private static void showInterface(String...$X) {
		
	}
	
	
	
}

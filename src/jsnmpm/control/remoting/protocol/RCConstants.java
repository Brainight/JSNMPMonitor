package jsnmpm.control.remoting.protocol;

public class RCConstants {

	// ··········  SERVER SIDE  ···········
	
	// CONTROL
	public static final byte ACTION_EXECUTE_COMMAND = 1;
	public static final byte ACTION_CTRL_OPEN_SESSION = 2;
	public static final byte ACTION_CTRL_CLOSE_SESSION = 3;
	public static final byte ACTION_CTRL_AUTH = 4;
	public static final byte ACTION_CTRL_GET_INFO = 5;
	
	// AGENTS
	public static final byte ACTION_AGENT_NEW = 10;
	public static final byte ACTION_AGENT_GET = 11;
	public static final byte ACTION_AGENT_MOD = 12;
	public static final byte ACTION_AGENT_DEL = 13;
	
	// SNMP
	public static final byte ACTION_SNMP_SEND_GET = 20;
	public static final byte ACTION_SNMP_SEND_GET_NEXT = 21;
	public static final byte ACTION_SNMP_SEND_GET_BULK = 22;
	public static final byte ACTION_SNMP_SHOW_TRAPS = 23;
	public static final byte ACTION_SNMP_CONF_TRAPS = 24;
	
	// PROCESS
	public static final byte ACTION_PS_NEW = 30;
	public static final byte ACTION_PS_GET = 31;
	public static final byte ACTION_PS_MOD = 32;
	public static final byte ACTION_PS_DEL = 33;
	
	// STUPID
	public static final byte ECHO = 99;
	
	
	// ···········  CLIENT SIDE ···············
	// ··· ACTIONS
	// BOOLEAN MSG
	public static final byte RESPONSE_SUCCESS = 0;
	public static final byte RESPONSE_ERROR = -1;
	
	public static final byte RESPONSE_CTRL_AUTH = -4;
	public static final byte RESPONSE_CTRL_GET_INFO = -5;
	
	// AGENTS
	public static final byte RESPONSE_AGENT_NEW = -10;
	public static final byte RESPONSE_AGENT_GET = -11;
	public static final byte RESPONSE_AGENT_MOD = -12;
	public static final byte RESPONSE_AGENT_DEL = -13;
		
	// SNMP
	public static final byte RESPONSE_SNMP_SEND_GET = -20;
	public static final byte RESPONSE_SNMP_SEND_GET_NEXT = -21;
	public static final byte RESPONSE_SNMP_SEND_GET_BULK = -22;
	public static final byte RESPONSE_SNMP_SHOW_TRAPS = -23;
	public static final byte RESPONSE_SNMP_CONF_TRAPS = -24;
		
	// PROCESS
	public static final byte RESPONSE_PS_NEW = -30;
	public static final byte RESPONSE_PS_GET = -31;
	public static final byte RESPONSE_PS_MOD = -32;
	public static final byte RESPONSE_PS_DEL = -33;

	// STUPID
	public static final byte ECHO_RESPONSE = -127;
	
	
	
	

}

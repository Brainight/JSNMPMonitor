package jsnmpm.control.utilities;

import java.util.HashSet;
import java.util.Set;

import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

public class Rule {
	
	public static final int ACTION_SEND_MAIL = 1;
	public static final int ACTION_EXTRA_BEEP = 2;
	//TODO Configure ID
	String ruleName = null;
	OID oid = null;
	Variable var = null;
	Set<Integer> actionsSet = null;
	String[] mailData = new String[]{"", "JSNMPMONITOR TRAP NOTIFY", "Manager received the following trap: "}; // MAIL_TO, SUBJECT, MESSAGE
	public Rule() {
		this.actionsSet = new HashSet<Integer>();
	}
	
	public Rule(OID oid, Variable var, int...actions) {
		
		this.actionsSet = new HashSet<Integer>();
		this.oid = oid;
		this.var = var;
		if(actions != null)
			for(int action : actions) {
				this.actionsSet.add(action);
			}
	}
	
	public void setName(String name) {
		this.ruleName = name;
	}
	
	public String getName() {
		return this.ruleName;
	}
	public void setAction(int...actions) {
		if(actions.length == 1)
			this.actionsSet.add(actions[0]);
		else
			for(int action : actions) {
				this.actionsSet.add(action);
			}
	}
	
	public void removAction(int...actions) {
		for(int action : actions) {
			this.actionsSet.remove(action);
		}
	}
	
	public void setMailSubject(String subject) {
		this.mailData[1] = subject;
	}
	
	public void setMailMessage(String message) {
		this.mailData[2] = message;
	}
		
	/** Returns an String array with content: <br>
	 * [0] = Receiver (if None, it is the same as sender in conf file) <br>
	 * [1] = Subject <br>
	 * [2] = Message <br>
	 * @return String Array.
	 */
	public String[] getMailingData() {
		return this.mailData;
	}
	
	public Set<Integer> getActions(){
		return this.actionsSet;
	}
	
	public void setOID(OID oid) {
		this.oid = oid;
	}
	
	public void setVariable(Variable var) {
		this.var = var;
	}
	
	public OID getOID() {
		return this.oid;
	}
	
	public Variable getVariable() {
		return this.var;
	}
}


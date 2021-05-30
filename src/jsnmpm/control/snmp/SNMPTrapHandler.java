package jsnmpm.control.snmp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

import jsnmpm.control.utilities.MailSender;
import jsnmpm.control.utilities.Rule;
import jsnmpm.control.whisper.TrapWhisper;

public class SNMPTrapHandler implements CommandResponder{
	
	List<Rule> rules = null;
	private SNMPManager mngr= null;
	private MailSender mailSender = null;
	
	public SNMPTrapHandler(SNMPManager manager) {
		this.mngr = manager;
		this.rules = new ArrayList<Rule>();
	}
	
	@Override
	public <A extends Address> void processPdu(CommandResponderEvent<A> crevent) {
		
		
		PDU pdu = crevent.getPDU();
		//System.out.println("RECEIVED PDU");
		if(pdu != null && pdu.getType() == PDU.TRAP || pdu.getType() == PDU.V1TRAP){
			
			int agentID = -1;
			for(SNMPAgent agent : this.mngr.agentsMap.values()) {
				if(agent.getIp().equals(crevent.getPeerAddress().toString().split("/")[0])){
					agentID = agent.getId();
					break;
				}
			}
			

			
			//System.out.print(pdu.getVariableBindings().get(2).getOid().toString());
			this.mngr.control.onNext(new TrapWhisper(crevent.getPeerAddress().toString(), agentID, crevent.getPDU(), this.getRules()));
			
			String data = "";
			for(VariableBinding var : crevent.getPDU().getVariableBindings()) {
				data += "---------->" + "OID: " + var.getOid() + " | Value: " +var.getVariable() + "\n";
			}
			
			
			String trapMessage = 	" - - - - - - - TRAP RECEIVED - - - - - - -   " + "\n" 
			        + "---------->" + "Address: " + crevent.getPeerAddress().toString() + " | AgentID: " + agentID + "\n"
					+ "---------->" + "RequestID: " + crevent.getPDU().getRequestID() + "\n"
					+ "---------->" + "DATA:\n" + data;
			
			for(Rule rule : this.getRules()) {
				if(rule.getOID().equals(crevent.getPDU().getVariableBindings().get(2).getOid())) {
					for(int action : rule.getActions()) {
						if(action == Rule.ACTION_SEND_MAIL) {
							if(this.mailSender != null)
								if(rule.getMailingData()[0].isEmpty() || rule.getMailingData()[0] == null) {
									this.mailSender.sendMail(rule.getMailingData()[1], rule.getMailingData()[2] + "\n\n" + trapMessage);
									//System.out.println("MAIL SENT1");
								}
								else {
									this.mailSender.sendMail(rule.getMailingData()[0], rule.getMailingData()[1], rule.getMailingData()[2] + "\n\n" + trapMessage);
									//System.out.println("MAIL SENT2");
								}
						}
					}
			
				}
			}
		}
	}
	
	public Rule addTrapHandlerRule(String name, String oid, int...actions) {
		Rule rule = new Rule(new OID(oid), null, actions);
		rule.setName(name);
		this.rules.add(rule);
		return rule;
	}
	
	public Rule addTrapHandlerRule(String name, String oid, String mailSubject, String mailtext, int...actions) {
		Rule rule = new Rule(new OID(oid), null, actions);
		rule.setName(name);
		rule.setMailSubject(mailSubject);
		rule.setMailMessage(mailtext);
		this.rules.add(rule);
		return rule;
	}
	
	public boolean setMailSender(String smtpServer, String smtpPort, String mailAccount, String mailPass) {
		try {
			int port = Integer.parseInt(smtpPort);
			this.mailSender = new MailSender(smtpServer, port, mailAccount, mailPass);
			return true;
		}catch(NumberFormatException nfe) {
			this.mailSender = null;
			return false;
		}
	}
	
	public void sendMail(String subject, String message) {
		if(this.mailSender != null) {
			this.mailSender.sendMail(subject, message);
			
		}
	}
	
	public void addAllTHRules(List<Rule> rules) {
		this.rules.addAll(rules);
	}

	
	public List<Rule> getRules(){
		return this.rules;
	}
	
	public List<Rule> getTrapActions(VariableBinding var) {
		List<Rule> rules = new ArrayList<Rule>();
		for(Rule rule : this.rules) {
			if(rule.getOID().equals(var.getOid())) {
				rules.add(rule);
				//System.out.println("\nADDING RULE TO TRAPWHISPER: " +  rule.getName());
			}
		}
		
		return rules;
		
		
	}
	
}

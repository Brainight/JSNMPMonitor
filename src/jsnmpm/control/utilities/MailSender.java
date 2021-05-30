package jsnmpm.control.utilities;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import jsnmpm.control.ControllerFileHandler;

public class MailSender {
 
	private String smptHost = null;
	private int smptPort = -1;
	private ControllerFileHandler logger = null;
	
	private String mailAccount = null;
	private String mailPass = null; // PLAIN PASSWORD, THANK GOD YOU WOKR IN CYBERSECURITY :')
	
	public MailSender() {
		
	}
	
	public MailSender(String host, int port, String account, String pass) {
		this.smptHost = host;
		this.smptPort = port;
		this.mailAccount = account;
		this.mailPass = pass;
	}
	
	public void setSMTPHost(String host) {
		this.smptHost = host;
	}
	
	public void setSMTPPort(int port) {
		this.smptPort = port;
	}
	
	public void setMailAccount(String account) {
		this.mailAccount = account;
	}
	
	public void setMailPass(String pass) {
		this.mailPass = pass;
	}
	
	public String getSMTPHost() {
		return this.smptHost;
	}
	
	public int getSMTPPort() {
		return this.smptPort;
	}
	
	public String getMailAccount() {
		return this.mailAccount;
	}
	
	public String getMailPass() {
		return this.mailPass;
	}
	
	public void sendMail(String subject, String message) {
		
		 Properties prop = new Properties();
		   prop.put("mail.smtp.auth", "true");
		   prop.put("mail.smtp.starttls.enable", "true");
		   prop.put("mail.smtps.host", this.smptHost);
		   prop.put("mail.smtp.port", this.smptPort);
		   
		   Session session = Session.getDefaultInstance(prop);
		   
		   try {
			   Message msg = new MimeMessage(session);
			   msg.setFrom(new InternetAddress(this.mailAccount));
			   msg.setRecipient(Message.RecipientType.TO, new InternetAddress(this.mailAccount));
			   msg.setSubject(subject);
			   msg.setText(message);
			   Transport tr = session.getTransport("smtps");
			   tr.connect(this.smptHost, this.mailAccount, this.mailPass);
		       tr.sendMessage(msg, new InternetAddress[] {new InternetAddress(this.mailAccount)});
		       tr.close();
		       this.logger.writeToLogFile(this.logger.getCtrlLogFilePath(), "INFO: Received trap, send mail!");
		   } catch (MessagingException e) {
			   this.logger.writeToLogFile(this.logger.getCtrlLogFilePath(), "ERROR: Could not send mail."
			   		+ "\n--> Reason: " + e.getMessage() 
			   		+ "\n--> MailSubject: " + subject
			   		+ "\n--> MailText: " + message
			   		+ "\n");
		   }  
		   
	}
	
	public void sendMail(String subject, String message, String mailTo) {
		
		 Properties prop = new Properties();
		   prop.put("mail.smtp.auth", "true");
		   prop.put("mail.smtp.starttls.enable", "true");
		   prop.put("mail.smtps.host", this.smptHost);
		   prop.put("mail.smtp.port", this.smptPort);
		   
		 Session session = Session.getDefaultInstance(prop);
		   
		   try {
			   Message msg = new MimeMessage(session);
			   msg.setFrom(new InternetAddress(this.smptHost));
			   msg.setRecipient(Message.RecipientType.TO, new InternetAddress(this.mailAccount));
			   msg.setSubject(subject);
			   msg.setText(message);
			   Transport tr = session.getTransport("smtps");
			   tr.connect(this.smptHost, this.mailAccount, this.mailPass);
		       tr.sendMessage(msg, new InternetAddress[] {new InternetAddress(this.mailAccount)});
		       tr.close();
			   System.out.println("Mail send");
		   } catch (MessagingException e) {
			   this.logger.writeToLogFile(this.logger.getCtrlLogFilePath(), "ERROR: Could not send mail."
				   		+ "\n--> Reason: " + e.getMessage() 
				   		+ "\n--> MailSubject: " + subject
				   		+ "\n--> MailText: " + message
				   		+ "\n");
			   
		   }  
	}

}


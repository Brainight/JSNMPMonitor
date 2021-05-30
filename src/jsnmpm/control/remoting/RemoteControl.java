package jsnmpm.control.remoting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jsnmpm.control.SNMPController;
import jsnmpm.control.remoting.packets.AgentData;
import jsnmpm.control.remoting.protocol.RCConstants;
import jsnmpm.control.remoting.protocol.RCPacket;
import jsnmpm.control.snmp.SNMPAgent;

public class RemoteControl implements Runnable{

	// ···NETWORKING
	private ServerSocket ss = null;
	private Socket clientSocket = null;
	private int concurrentConnections = 1;
	private String defaultIP = "127.0.0.1";
	private int defPort = 65432;
	
	// Client
	private boolean clientIsConnected = false;
	private String clientIP = null;
	
	// IO
	private ObjectInputStream in = null;
	private ObjectOutputStream out = null;
	
	// SNMP CONTROL
	SNMPController ctrl = null;
	
	
	// CLIENT
	private String adminPass= "67f067dcea6dc2ac91f3644a1cffedb3";
	
	public RemoteControl(String ip, int port, SNMPController ctrl)  {
		try {
			this.ss = new ServerSocket();
			SocketAddress sAddr = new InetSocketAddress(ip, port);
			this.ss.bind(sAddr, this.concurrentConnections);
			
			this.ctrl = ctrl;
		} catch (IOException e) {
			this.ctrl.writeToCTRLLogFile("ERROR: Socket Exception occurred. Cannot manage the application remotely.");
		}
	}
	
	public RemoteControl(SNMPController ctrl) {
		try {
			ss = new ServerSocket();
			SocketAddress sAddr = new InetSocketAddress(this.defaultIP, this.defPort);
			ss.bind(sAddr, this.concurrentConnections);
			this.ctrl = ctrl;
		} catch (IOException e) {
			this.ctrl.writeToCTRLLogFile("ERROR: Socket Exception occurred. Cannot manage the application remotely.");
			
		}
	}
	
	public RemoteControl(String ip, int port, String adminPass)  {
		try {
			ss = new ServerSocket();
			SocketAddress sAddr = new InetSocketAddress(ip, port);
			ss.bind(sAddr, this.concurrentConnections);
			
		} catch (IOException e) {
			
		}
	}
	
	public void run() {
		this.start();
	}
	
	private void start() {
		while(true) {
			try {
				this.clientSocket = ss.accept();
				this.out = new ObjectOutputStream(this.clientSocket.getOutputStream());
				this.in =  new ObjectInputStream(this.clientSocket.getInputStream());
				this.clientIP = this.clientSocket.getInetAddress().toString();
				this.ctrl.writeToCTRLLogFile("INFO: Client '" +  this.clientIP + "' connection attempts.");
				this.handleClient();
			} catch (IOException e) {
				this.clientSocket = null;
			}
		}
	}
	
	private void handleClient() {
		try {
			
			if(this.execAccessControl()) {
				this.ctrl.writeToCTRLLogFile("INFO: Client '" +  this.clientIP + "' authentication SUCCESSFULL");
				while(true) {
					RCPacket pkt = this.readClientData();
					switch(pkt.getAction()) {
					
					// GENERIC CTRL CASES
					case RCConstants.ACTION_EXECUTE_COMMAND:
						break;
						
					case RCConstants.ACTION_CTRL_OPEN_SESSION:
						break;
						
					case RCConstants.ACTION_CTRL_CLOSE_SESSION:
						break;
						
					case RCConstants.ACTION_CTRL_AUTH:
						break;
						
					// CASES FOR AGENTS
					case RCConstants.ACTION_AGENT_NEW:
						try {
							AgentData agent = (AgentData) pkt.getData();
							int agentID = this.ctrl.addNewAgent(agent.getAgentIP(), agent.getAgentPort(), agent.getAgentName(), agent.getReadCom());
							agent.setAgentID(agentID);
							this.sendDataToClient(new RCPacket(RCConstants.RESPONSE_AGENT_NEW, null, agent));
						}catch(ClassCastException cce) {
							
						} catch (SQLException e) {
							
						}
						break;
						
					case RCConstants.ACTION_AGENT_GET:
						
						List<AgentData> agents = new ArrayList<AgentData>();
						for(SNMPAgent agent : this.ctrl.managerGetAgents()) {
							agents.add(new AgentData(agent));
						}
						this.sendDataToClient(new RCPacket(RCConstants.RESPONSE_AGENT_GET, null, agents));
						break;
						
					case RCConstants.ACTION_AGENT_MOD:
						break;
						
					case RCConstants.ACTION_AGENT_DEL:
						try {
							AgentData agent = (AgentData) pkt.getData();
							if(this.ctrl.managerGetAgent(agent.getAgentID()) != null){
								if(this.ctrl.removeAgent(agent.getAgentID())){
									this.sendDataToClient(new RCPacket(RCConstants.RESPONSE_AGENT_DEL, "SUCCESS", null));
								}else {
									this.sendDataToClient(new RCPacket(RCConstants.RESPONSE_AGENT_DEL, "ERROR", "Cannot delete Agent with ID: " + agent.getAgentID()));
								}
							}else {
								this.sendDataToClient(new RCPacket(RCConstants.RESPONSE_AGENT_DEL, "ERROR", "No Agent with ID: " + agent.getAgentID()));
							}
							
						}catch(ClassCastException cce) {
							
						}						
						break;
						
					// CASES FOR SNMP
					case RCConstants.ACTION_SNMP_SEND_GET:
						break;
						
					case RCConstants.ACTION_SNMP_SEND_GET_NEXT:
						break;
						
					case RCConstants.ACTION_SNMP_SEND_GET_BULK:
						break;
						
					case RCConstants.ACTION_SNMP_SHOW_TRAPS:
						break;
						
					case RCConstants.ACTION_SNMP_CONF_TRAPS:
						break;
						
					// CASES FOR SNMP
					case RCConstants.ACTION_PS_NEW:
						break;
						
					case RCConstants.ACTION_PS_GET:
						break;
						
					case RCConstants.ACTION_PS_MOD:
						break;
						
					case RCConstants.ACTION_PS_DEL:
						break;
						
					// STUPID
					case RCConstants.ECHO:
						this.sendDataToClient(new RCPacket(RCConstants.ECHO_RESPONSE, null, pkt.getData()));
						break;
						
					default:
						break;
					}
					
				}
			}
			else {
				this.closeClientSocket();
				this.ctrl.writeToCTRLLogFile("INFO: Client '" +  this.clientIP + "' authentication FAILED.");
			}
		}catch(ClassNotFoundException cne) {
			this.closeClientSocket();
			this.ctrl.writeToCTRLLogFile("COMMUNICATION_PROTOCOL_ERROR: Client has interrupted the conection!");
		}catch(IOException ioe) {
			this.closeClientSocket();
			this.ctrl.writeToCTRLLogFile("CONNECTION_ERROR: Client has interrupted the conection!");
			System.out.println("CONNECTION_ERROR: Client has interrupted the conection!");
		}catch(NullPointerException npe) {
			this.closeClientSocket();
			this.ctrl.writeToCTRLLogFile("CONNECTION_ERROR: Client has interrupted the conection!");
			System.out.println("CONNECTION_ERROR: Client has interrupted the conection!");
		}
	}
	
	private boolean execAccessControl() throws ClassNotFoundException, IOException{
		try {
			System.out.println(Thread.currentThread().getName() + ": Starting Access Control");
			RCPacket pkt = (RCPacket) this.in.readObject();
			System.out.println("Credential recieved");
			if(pkt.getAction() == RCConstants.ACTION_CTRL_AUTH) {
				String pass = (String)pkt.getData();
				if(pass.equals(this.adminPass)) {
					this.out.writeObject(new RCPacket(RCConstants.RESPONSE_CTRL_AUTH, null, "Authentication succesfull"));
					return true;
				}else {
					return false;
				}
			}else
				System.out.println("BAD");
		}catch (ClassCastException cce) {
			return false;
		}
		return false;
	}
	
	
	// UTILS
	
	public RCPacket readClientData() {
		try {
			return (RCPacket) this.in.readObject();
		} catch (ClassNotFoundException | IOException e) {
			this.closeClientSocket();
		}
		return null;
	}
	
	public void sendDataToClient(RCPacket pkt) {
		try {
			this.out.writeObject(pkt);
		} catch (IOException e) {
			e.printStackTrace();
			this.closeClientSocket();
		}
	}
	public void closeClientSocket() {
		try {
			this.clientSocket.close();
		} catch (IOException | NullPointerException e) {
			this.ctrl.writeToCTRLLogFile("INFO: Client ");
		}
		this.clientSocket = null;
	}
	
	
	
	private void hashPass(String pass) {
		byte[] msg;
		try {
			msg = pass.getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(msg);
			String hash = new BigInteger(1, digest).toString(16);
			
			while(hash.length() < 32) {
				hash = "0" + hash;
			}
			this.adminPass = hash;
		} catch (UnsupportedEncodingException e) {
			
		} catch (NoSuchAlgorithmException e) {
		}
	}
	
	
}

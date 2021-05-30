package jsnmpm.monitor.terminal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

import org.snmp4j.PDU;
import org.snmp4j.smi.VariableBinding;

import jsnmpm.control.whisper.ProcessWhisper;
import jsnmpm.monitor.terminal.exceptions.TPVAuthenticationException;

public class TProcessViewerController{

	/** This Map saves a String corresponding to a given processID and a socket to send the process data **/
	HashMap<String, THandler> processes; 
	private ServerSocket ssc = null;
	private volatile boolean running = false;
	int totalViews = 0;
	
	public TProcessViewerController() throws IOException {
		try {
			ssc = new ServerSocket(16100);
			//ssc.bind(new InetSocketAddress("127.0.0.1",  16100));
		} catch (IOException e) {
			ssc.close();
			throw e;
		}
		
		processes = new HashMap<String, THandler>();
	}
	
	public void acceptNewConnection(String tpvToken, ProcessWhisper whisper) throws IOException, TPVAuthenticationException {
		//TODO COMMUNICATE WITH TMONITOR
		
		Process ps = new ProcessBuilder("cmd", "/c", "start","cmd.exe","/k","java","-jar","\"C:\\Users\\MrStonedDog\\Desktop\\JSNMP\\TViewer.jar\"",String.valueOf(tpvToken)).start();

		Socket sc = this.ssc.accept();

		DataInputStream in = new DataInputStream(sc.getInputStream());

		String[] auth = in.readUTF().split(":");
		
		if(auth[0].equals(tpvToken)) {
			processes.put(whisper.getProcessID(), new THandler(sc, whisper.getProcessID(), whisper.getAgentID()));
		}else {
			throw new TPVAuthenticationException("Authentication unsuccesful for a ProcessViewer Connection");
		}
	}
	
	public void sendData(String processID, PDU data) throws IOException {
		this.processes.get(processID).sendData(data);
	}
	
	public boolean exists(String processID) {
		return (this.processes.get(processID) != null) ? true : false;
	}
	
	public void removeHandler(String processID) {
		this.processes.remove(processID);
	}
	
	
	
// #################################    T H A N D L E R     I N N E R   C L A S S    ################################
	
	private class THandler extends Thread{
		
		private Socket sc;
		private DataOutputStream out;
		private DataInputStream in;
		private volatile boolean running = true;
		
		public THandler(Socket sc, String processID, int agentID) throws IOException {
			this.setName(String.format("%s%s%s", "THandler-", ++totalViews,"-Thread"));
			this.out = new DataOutputStream(sc.getOutputStream());
			this.out.writeUTF("###############   PID:" +processID + "-AGENT:" + agentID +" ###############");
			//this.start();
		}
		
		@Override
		public void run() {
			
			while(running) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					try {
						this.kill();
					} catch (InterruptedException | IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}
		
		public void kill() throws InterruptedException, IOException {
			this.running = false;
			sc.close();
			this.join();
		}
		
		public void sendData(PDU data) throws IOException {
			if(data == null) {
				this.out.writeUTF("Response is null");
			}else if(data.getErrorIndex() == 0) {
				for(VariableBinding var : data.getVariableBindings()) {
					this.out.writeUTF(DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now()) +
							" -- OID: "+ var.getOid().toDottedString() + " ---> " + var.getVariable());
				}
			}else {
				this.out.writeUTF("SNMPError: Status="+data.getErrorStatus() + "Index="+data.getErrorIndex() + "Data="+data.getErrorStatusText());
			}
			
		
		}
		
		
		
	}
	
}



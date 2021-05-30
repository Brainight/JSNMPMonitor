package jsnmpm.main;

import java.io.BufferedReader;

import java.io.IOException;

import java.io.InputStreamReader;

import javax.sound.sampled.LineUnavailableException;


import jsnmpm.control.SNMPController;
import jsnmpm.monitor.terminal.TMonitor;

public class JSNMPMain {

	
	public static void main(String[] args) {
		
		SNMPController snmpCtrl = new SNMPController();
		if(args.length == 0)
			try {
				snmpCtrl.configMonitor(new TMonitor(snmpCtrl));
			} catch (IOException e) {
				snmpCtrl.writeToCTRLLogFile("ERROR: Could not load Terminal");
			}
		else
		for(int i = 0; i < args.length; i++) {
			switch(args[i]) {
			
			case "-h":
			case "--help":
				printHelp();
				System.exit(0);
				break;
			case "-s":
			case "--socket":
				try {
					String[] params = args[i+1].split(":");
					int port = Integer.parseInt(params[1]);
					System.out.println("Configuring socket\nListening on: " + params[0]+"\nPort: " + port);
					
					snmpCtrl.configSocket(port, params[0]);
				}catch(NumberFormatException nfe) {
					System.out.println("SOCKET_CONFIG_ERROR: Wrong Port datatype");
					System.exit(1);
				}catch(ArrayIndexOutOfBoundsException ae) {
					System.out.println("Configuring default socket\nListening on: 127.0.0.1\nPort: 65432");
					snmpCtrl.configSocket();
				}
				break;
				
			case "-t":
			case "--terminal":
				try {
					snmpCtrl.configMonitor(new TMonitor(snmpCtrl));
				} catch (IOException e) {
					snmpCtrl.writeToCTRLLogFile("ERROR: Could not load Terminal");
				}
				break;
				
			case "-g":
			case "--gui":
				try {
					snmpCtrl.configGUI(new TMonitor(snmpCtrl)); // TODO Create a GUI Implementation. Not Terminal.
				} catch (IOException e) {
					snmpCtrl.writeToCTRLLogFile("ERROR: Could not load GUI implementation");
				}  
				break;	
			}
		}
		
		snmpCtrl.start();
		
	}
	
	private static void printHelp() {
		
		System.out.print(""
				+ "========================|    JSNMP MONITOR    |=========================\n"
				+ "\n Options:"
				+ "\n		-h | --help) "
				+ "\n			This option..."
				+ "\n		-s | --socket) ip:port"
				+ "\n			Start a server socket listening of the given IP's and port."
				+ "\n			You may specify all interfaces with 0.0.0.0:65432"	
				+ "\n			Example: 10.10.10.2:65432"
				+ "\n		-t | --terminal)"
				+ "\n			Executes the program and open a terminal to handle it."
				+ "\n			Warning: If terminal is closed, program ends too."
				+ "\n		-g | --gui)"
				+ "\n			Executes the program and opens a GUI to manage the program."
				+ "\n			Unlike -t, closing the window wont close the program"
				+ "\n\n\n Richard & L0rdSt0N3d\n\n"
				+ "								 -- ENTER --");


		try {
			new BufferedReader(new InputStreamReader(System.in)).readLine();
		} catch (IOException e) {
			
		}
		
	}
}

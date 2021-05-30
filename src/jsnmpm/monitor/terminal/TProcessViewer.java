package jsnmpm.monitor.terminal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.fusesource.jansi.Ansi;

/**
 * This class display the responses received from a SNMPProcess. It must  be executed in a different JVM in order to spawn a new
 * Terminal Window in which the responses will be displayed as output. 
 * The JSNMPMonitor process (JVM) will comunicate with this separated process (JVM) with Sockets.
 * JSNMPMonitor will open a specific port which needs to be passed to this process as a positonal parameter, so that it can establish a connection
 * with the Server. Only output methods are implemented, meaning this process is unable to send data to the server. 
 * 
 * @author MrStonedDog
 *
 */
public class  TProcessViewer {

	private static Terminal terminal;
	private static DataOutputStream out = null;
	private static DataInputStream in = null;
	private volatile static boolean running = false;
	private final static int SERVER_PORT = 16100;
	
	public static void main(String [] args) throws IOException {
		
		Socket sc = null;
		
		terminal = new Terminal();
		
		running = true;
		
		if(args.length == 1) {
			try {
				    terminal.setInitPromptCursor(0, 0);
					String tvcToken = args[0];
					sc = new Socket("127.0.0.1", SERVER_PORT);
					in = new DataInputStream(sc.getInputStream());
					out = new DataOutputStream(sc.getOutputStream());
					out.writeUTF(tvcToken);
					terminal.cout(Ansi.ansi().a(terminal.ansiPrompt + in.readUTF()));
					receiv();
				
				}catch(NumberFormatException nfe) {
					terminal.cout(Ansi.ansi().a(terminal.ansiPrompt + "Error: Cannot establish connection to given port"));
					running = false;
				} catch (IOException e) {
					terminal.cout(Ansi.ansi().a(terminal.ansiPrompt + "Error: I/O Exception occured"));
					running = false;
			}
		}else {
			System.out.println("Missing arguments: Authentication Token");
		}

	}
	
	private static void receiv() throws IOException {
		String message = "";
		String []msg;
		 terminal.setInitPromptCursor(2, 0);
		while(running) {
			
			message = in.readUTF();
			
			msg = message.split(" ");
			
			terminal.cout(Ansi.ansi().a(terminal.ansiPrompt + msg[0] + terminal.ansiDefault + message.substring(msg[0].length())));
		}
	}
	
}

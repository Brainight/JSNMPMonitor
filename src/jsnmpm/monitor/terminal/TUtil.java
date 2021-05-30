package jsnmpm.monitor.terminal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.fusesource.jansi.Ansi;

/**
 * 
 * @author MrStonedDog
 *
 */
public class TUtil {
	
	// ENUMS
	enum Encode{
		ASCII, BINARI
	}
	
	enum Display{
		 NEGATIVE, POSITIVE
	}
	
	enum Banner{
		BANNER1, BANNER2, BANNER3
	}
	
	// ############ CONSTANTS / VARIABLES / PARAMETERS / WHATEVER ... ##############################
	public static final String[] MENU_OP_HEADERS = new String[] {"     - AGENT -     ","    - MONITOR -    ", "   - PROCESSES -   ", "     - OTHER -     "};
	
	public static final String[] MENU_OP_AGENTS =  new String[]{"(1) ADD AGENTS","(2) SHOW AGENTS","(3) CONFIG AGENT","(4) DEL AGENT"};
	public static final String[] MENU_OP_MONITOR = new String[] {"(5) SEND REQUEST", "(6) SEND BULK/NEXT", "(7) SEE TRAPS ", "(8) TRAP CONFIG"};
	public static final String[] MENU_OP_PROCESS = new String[]{" (9) NEW PROCESS", "(10) SHOW PROCESS", "(11) MOD PROCESSES", "(12) DEL PROCCESS"};
	public static final String[] MENU_OP_OTHERS =  new String[]{"(13) SNMP-MANAGER", "(14) JSNMP-SHELL", "(15) EXIT"};
	
	public static final Map<String, String[]> MENU_OPTIONS;
	
	static {
		MENU_OPTIONS = new HashMap<String, String[]>();
		MENU_OPTIONS.put(MENU_OP_HEADERS[0], MENU_OP_AGENTS);
		MENU_OPTIONS.put(MENU_OP_HEADERS[1], MENU_OP_MONITOR);
		MENU_OPTIONS.put(MENU_OP_HEADERS[2], MENU_OP_PROCESS);
		MENU_OPTIONS.put(MENU_OP_HEADERS[3], MENU_OP_OTHERS);
	}
	
	// #############################  U S E F U L L   M E T H O D S ################################

	static String[] getAsLineArray(String text) {
		return text.split("\n");
	}
	
	
	// #####################################   B A N N E R    #####################################
	
	private static String banner1 = 
			                "\n############################################################################\n"
			                + "############################################################################\n"
						  +   "#######         ###          ###   #####   ###    ####     ###          ####\n"
					      +   "###########    ###   ##########     ###   ###      #      ###   ####   #####\n"
			              +   "##########    ###          ###   #  ##   ###   #     #   ###          ######\n"
			              +   "#####  ##    ##########   ###   ##      ###   ##   ##   ###   ##############\n"
			              +   "####        ###          ###   ####    ###   #######   ###   ###############\n"
			              +   "############################################################################\n"
			              +   "############################################################################";
	
	private static String banner2 = 
						    "\n###########################################################################\n"
						    + "######         ####        ####  #####  ####   ####   ####          #######\n"
						    + "#############  ####  ##########   ####  ####    ##    ####  ######  #######\n"
						    + "#############  ####  ##########    ###  ####          ####  ######  #######\n"
						    + "#############  ####        ####  #  ##  ####  ##  ##  ####          #######\n"
						    + "#############  ##########  ####  ##  #  ####  ######  ####  ###############\n"
						    + "#######   ##   ##########  ####  ####   ####  ######  ####  ###############\n"
						    + "########      #####        ####  #####  ####  ######  ####  ###############\n"
						    + "###########################################################################\n";
	
	private static String banner3 = 
						      "\n                                                                           \n"                                                                                                
			                + "    ##########   ###########   ###      ##   ###      ###   ###########    \n" 
			                + "            ##   ##            ####     ##   ####    ####   ##       ##    \n"
			                + "            ##   ##            ##  ##   ##   ## ##  ## ##   ##       ##    \n"
			                + "            ##   ###########   ##   ##  ##   ##  ####  ##   ###########    \n" 
			                + "            ##            ##   ##    ## ##   ##   ##   ##   ##             \n"
			                + "    ##      ##            ##   ##     ####   ##        ##   ##             \n"
			                + "     #########   ###########   ##      ###   ##        ##   ##             \n"
			                + "                                                                           ";
	
	public static Banner getRandomBanner() {
		switch((int)(Math.random()*3)) {
		case 0:
			return Banner.BANNER1;
		case 1:
			return Banner.BANNER2;
		case 2:
			return Banner.BANNER3;
		default:
			return Banner.BANNER3;
		}
	}
	
	static Encode getRandomEncode() {
		return (((int)(Math.random()*2) == 0) ? Encode.ASCII : Encode.BINARI);
		
	}
	
	static Display getRandomDispay() {
		return (((int)(Math.random()*2) == 0) ? Display.NEGATIVE : Display.POSITIVE);
	}
	
	static String createCustomBanner(Banner banner, int tabs, Encode character, Display type) {
		switch(banner) {
		case BANNER1:
			return ((character == Encode.ASCII) ? createASCIICustomBanner(banner1, tabs, type ) : createBINARICustomBanner(banner1, tabs, type));
		case BANNER2:
			return ((character == Encode.ASCII) ? createASCIICustomBanner(banner2, tabs, type ) : createBINARICustomBanner(banner2, tabs, type));
		case BANNER3:
			return ((character == Encode.ASCII) ? createASCIICustomBanner(banner3, tabs, type ) : createBINARICustomBanner(banner3, tabs, type));
		default:
			return ((character == Encode.ASCII) ? createASCIICustomBanner(banner3, tabs, type ) : createBINARICustomBanner(banner3, tabs, type));
		}
	}
	
	static Ansi createCustomAnsiBanner(Banner banner, int tabs, Encode character, Display type, Ansi write, Ansi hollow) {
		switch(banner) {
		case BANNER1:
			return ((character == Encode.ASCII) ? createAnsiASCIICustomBanner(banner1, tabs, type, write, hollow ) :
				createAnsiBINARICustomBanner(banner1, tabs, type, write, hollow));
		case BANNER2:
			return ((character == Encode.ASCII) ? createAnsiASCIICustomBanner(banner2, tabs, type, write, hollow ) :
				createAnsiBINARICustomBanner(banner2, tabs, type, write, hollow));
		case BANNER3:
			return ((character == Encode.ASCII) ? createAnsiASCIICustomBanner(banner3, tabs, type, write, hollow ) :
				createAnsiBINARICustomBanner(banner3, tabs, type, write, hollow));
		default:
			return ((character == Encode.ASCII) ? createAnsiASCIICustomBanner(banner1, tabs, type, write, hollow ) :
				createAnsiBINARICustomBanner(banner1, tabs, type, write, hollow));
		}
	}
	
	static String createASCIICustomBanner(String banner, int spc, Display type) {
		String oldbanner = banner;
		banner = "";
		for(char b : oldbanner.toCharArray()){
			if(b == '#')
				banner+= (type == Display.NEGATIVE) ? " " : String.valueOf((char)(int)(Math.random()*26+48));
			else if(b == '\n')
				banner+= '\n' + " ".repeat(spc);
			else {
				banner+= (type == Display.NEGATIVE) ? String.valueOf((char)(int)(Math.random()*26+48)) : String.valueOf(b);
			}
		}
		return banner;
	}
	
	static String createBINARICustomBanner(String banner, int spc, Display type) {
		String oldbanner = banner;
		banner = "";
		for(char b : oldbanner.toCharArray()){
			if(b == '#')
				banner+= (type == Display.NEGATIVE) ? " " : String.valueOf((int)(Math.random()*2));
			else if(b == '\n')
				banner+= '\n' + " ".repeat(spc);
			else {
				banner+= (type == Display.NEGATIVE) ? String.valueOf((int)(Math.random()*2)) : String.valueOf(b);
			}
		}
		return banner;
	}
	
	static Ansi createAnsiASCIICustomBanner(String banner, int spc, Display type, Ansi write, Ansi hollow) {
		Ansi ansiBanner = Ansi.ansi();
		for(char b : banner.toCharArray()){
			if(b == '#')
				ansiBanner.a((type == Display.NEGATIVE) ? hollow.a(" ") : write.a(String.valueOf((char)(int)(Math.random()*26+48))));
			else if(b == '\n')
				ansiBanner.a("\n" + " ".repeat(spc));
			else 
				ansiBanner.a(write.a((type == Display.NEGATIVE) ? String.valueOf((char)(int)(Math.random()*26+48)) : String.valueOf(b)));
		}
		return ansiBanner;
	}
	
	static Ansi createAnsiBINARICustomBanner(String banner, int spc, Display type, Ansi write, Ansi hollow) {
		Ansi ansiBanner = Ansi.ansi();
		for(char b : banner.toCharArray()){
			if(b == '#')
				ansiBanner.a((type == Display.NEGATIVE) ? hollow.a(" ") : write.a(String.valueOf((int)(Math.random()*2))));
			else if(b == '\n') 
				ansiBanner.a("\n" + " ".repeat(spc));
			else
				ansiBanner.a(write.a((type == Display.NEGATIVE) ? String.valueOf((int)(Math.random()*2)) : String.valueOf(b)));
		}
		return ansiBanner;
	}
	
	
	// ##################################  M E N U 1  ##################################
	
	
	
}

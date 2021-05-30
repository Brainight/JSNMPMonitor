package jsnmpm.monitor.terminal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import jsnmpm.control.SNMPController;
import jsnmpm.control.utilities.JSNMPUtil.INFO;
import jsnmpm.monitor.terminal.Terminal.Prompt;

public class Terminal {

	
	public static enum Prompt{
		NORMAL, SHELL
	}
	// ----------  PARAMETERS / VARIABLES  -----------
	// ·· SYSTEM INFO
	private String os = null;
	private int terminalWidth = 120;
	public Prompt currentStatus = Prompt.NORMAL;
	
	// ### I/O ###
	private BufferedReader br;
	
	// ### MOUSE POSITIONING ####
	// ·· PROMPTS
	// SHELL
	public String shellPrompt = "jsnmp> ";
	public final int startShellPromptRow = 30;
	public final int startShellPromptCol = 0;
	
	// OPTION
	public String optionPrompt = "Option: ";
	public final int startOptionPromptRow = 20;
	public final int startOptionPromptCol = this.getMenuStartCol();
	
	// PROMPT ROW/COL
	public int currentPromptRow = startOptionPromptRow;
	public int currentPromptCol = startOptionPromptRow;
	
	
	// ·· INFO
	public final int infoRow = 12;
	// ·· MENU
	public final int menuRow = 14;
	
	
	// DATA
	private final int BANNER_WIDTH = 76;
	public Ansi ansiUserinput = Ansi.ansi().fgDefault();
	public Ansi ansiPrompt = Ansi.ansi().fgBrightRed().bgDefault();
	public Ansi ansiMenuOption = Ansi.ansi().bgRed().fgBlack();
	public Ansi ansiMenuHeader = Ansi.ansi().bgRed().fgDefault();
	public Ansi ansiBanner = Ansi.ansi().fgBrightRed();
	
	public Ansi ansiInfo = Ansi.ansi().fgRed();
	public Ansi ansiInfo2 = Ansi.ansi().fgBrightBlue();
	public Ansi ansiDefault = Ansi.ansi().bgDefault().fgDefault();
	public Ansi ansiGood = Ansi.ansi().bgDefault().fgBrightGreen();
	public Ansi ansiError = Ansi.ansi().fgBrightMagenta();
	public Ansi ansiWarning = Ansi.ansi().fgYellow();
	public Ansi ansiNewLine = Ansi.ansi().a(ansiDefault).a("\n");

	
	// ######### 	CONSTRUCTOR 	###########
	/**
	 * This class implements all methods needed to print and read from the Terminal that is being used.
	 * 
	 * @throws IOException
	 */
	public Terminal() throws IOException {
		AnsiConsole.systemInstall();
		this.br = new BufferedReader(new InputStreamReader(System.in));
		this.os = this.getOS();

	}
	
	// ##################    GETTERS // SETTERS    #####################
	public int getTerminalWidth() {
		return this.terminalWidth;
	}
	
	public void setTerminalWidth(int width) {
		this.terminalWidth = width;
	}
	// ##################### 		TERMINAL I/O 		#########################
	
	/**
	 * Reads input from System.in . The given input must be of @class Ansi.
	 * @param text
	 * @return
	 */
	public String readInput(Ansi text) {
		try {
			System.out.print(Ansi.ansi().cursor(this.currentPromptRow++, this.currentPromptCol).a(text).a(this.ansiUserinput));
			String input = br.readLine();
			return input;
		} catch (IOException e) {
			return null;
		}
	}
	
	public String re4DInput(Ansi text) {
		try {
			System.out.print(Ansi.ansi().a(text).a(this.ansiUserinput));
			String input = br.readLine();
			return input;
		} catch (IOException e) {
			return null;
		}
	}
	
	/**
	 * Prints the given text with given Ansi configuration. After every letter @function Thread.sleep(millis)
	 * is called with millis = 3.
	 * @param ansi
	 * @param text
	 */
	private void write(Ansi ansi, String text) {
		for(int i = 0; i<text.length(); i++) {
			System.out.print(Ansi.ansi().a(ansi).a(text.charAt(i)));
			try {
				Thread.sleep(3);
			} catch (InterruptedException e) {
			}
		}
	}
	
	/**
	 * Used only for printing text with cursor positioning already specified.
	 * @param text
	 */
	public void print(Ansi text) {
		System.out.print(text);
	}
	
	/**
	 * Used only for printing text with cursor positioning already specified.
	 * @param text
	 */
	public void println(Ansi text) {
		System.out.println(text);
	}
	
	/**
	 * Used for printing text to System.out . The text must be of type @class Ansi.
	 * This method print a new line character at the end
	 * !! If you wanna print a text in more than one line you should call this function for every line.
	 * @param text
	 */
	public void cout(Ansi text) {
		
		System.out.print(Ansi.ansi().cursor(this.currentPromptRow++, this.currentPromptCol).a(text).a(this.ansiNewLine));

	}
	
	/**
	 * Prints out the given error text with the configuration of @param ansiError.
	 * Print a new line at the end. Uses function "cout"
	 * @param err
	 */
	public void coutError(String err) {
		this.cout(Ansi.ansi().a(this.ansiError).a(err));
	}
	
	/**
	 * Prints out the given warning text with the configuration of @param ansiWarning.
	 * Prints a new line at the end.  Uses function "cout"o
	 * @param warning
	 */
	public void coutWarning(String warning) {
		this.cout(Ansi.ansi().a(this.ansiWarning).a(warning));
	}
	
	public void coutNewLine() {
		this.cout(Ansi.ansi());
	}
	
	// ##################### 		SYSTEM DATA 		#################
	
	/** Tries to find out the operating system the program is running on. */
	private String getOS() {
		return System.getProperty("os.name");
	}
	
	// ##################### 		TERMINAL CONFIGURATION 		#####################
	/**
	 * Puts the cursor on the specified (row,column) in terminal. If the desired row or column is bigger
	 * than the maxRow / maxColumn visible in the prompt this method wont work. See @method jumpLines() for more help.
	 * @param row
	 * @param col
	 */
	public void setCursor(int row, int col) {
		print(Ansi.ansi().cursor(row, col));
	}
	
	public void setInitPromptCursor(int row, int col) {
		this.currentPromptRow = row;
		this.currentPromptCol = col;
	}
	
	public void jumpLines(int lines) {
		for(int i = 0; i < lines; i++)
			this.coutNewLine();
	}
	
	/**
	 * Sets initial cursor positiong depending on the Prompt status. By default if status == Prompt.NORMAL
	 * cursor will be placed at (currentOptionRow, currentOptionCol) -> (25, @return getBannerStartCol()) and if status == Prompt.SHELL it will
	 * be placed at (currentShellRow, currentShellCol) -> (30,0)
	 */
	public void setInitialCursor() {
		if(this.currentStatus == Prompt.NORMAL) {
			this.currentPromptRow = this.startOptionPromptRow;
			this.currentPromptCol = this.startOptionPromptCol;
		}else {
			this.currentPromptRow = this.startShellPromptRow;
			this.currentPromptCol = this.startShellPromptCol;
		}
	}
	
	/**
	 * Clears the terminal. It creates a process and inhertis current IO to execute the proper command
	 * for each OS. -> Windows: "cmd /c cls" </br> Linux: "clear"
	 */
	public void clearTerminal() {
		if(this.os.split(" ")[0].equals("Windows")) {
			try {
				new ProcessBuilder("cmd","/c","cls").inheritIO().start().waitFor();
			} catch (InterruptedException | IOException e) {
				coutError("Error: Cannot reset Window...");
			}
		}else { // PROBABLY A LINUX SYSTEM
			try {
				new ProcessBuilder("clear").inheritIO().start().waitFor();
			} catch (InterruptedException | IOException e) {
				coutError("Error: Cannot reset Window...");
			}
		}
	}
	
	/**
	 * Resets the terminal to its default state. Depending on the terminal status
	 * the prompt location may differ. 
	 * @param info
	 */
	public void reset(Map<INFO, String> ctrlInfo) {
		this.clearTerminal();
		//this.deleteLastLines(this.currentPromptRow);
		this.setUpStartHeader(ctrlInfo);
	}
	
	/**
	 * Changes between the two possible Prompt (Prompt.NORMAL -> "Option:" | Prompt.SHELL -> "jsnmp>")
	 */
	public void changePrompt() {
		if(this.currentStatus == Prompt.NORMAL) {
			this.currentStatus = Prompt.SHELL;
			this.currentPromptRow = this.startShellPromptRow;
			this.currentPromptCol = this.startShellPromptCol;
		}else {
			this.currentStatus = Prompt.NORMAL;
			this.currentPromptRow = this.startOptionPromptRow;
			this.currentPromptCol = this.getMenuStartCol();
		}
	}
	
	/** Deletes the last line.(Not the current) */
	public void deleteLastLine() {
		System.out.println(Ansi.ansi().cursor(--this.currentPromptRow, --this.currentPromptCol).eraseLine());
	}
	
	/** Deletes the n last lines. Calls @method deleteLastLine() for every line.*/
	public void deleteLastLines(int lines) {
		for(int i = 0;i<lines;i++) {
			this.deleteLastLine();
		}
	}
	
	/** Resize all graphics to given width. Does not resize terminal window.*/
	public void resize(int columns, Map<INFO, String> ctrlInfo) throws IOException {
		
		this.terminalWidth = columns;
		this.clearTerminal();
		this.setUpStartHeader(ctrlInfo);
	}
	
	// #######################      TERMINAL INTERFACE     #############################
	
	// ···· POSITIONING
	public int getBannerStartCol() {
		return Math.round(this.terminalWidth-this.BANNER_WIDTH)/2;
	}
	
	public int getMenuStartCol() {
		return getBannerStartCol();
	}
	
	// ···· PRINTING
	/** Sets the header. The header is made of a banner, the main info line and the menu "boxes".
	 * The @param info is the information to be displayed.
	 * @param info
	 */
	public  void setUpStartHeader(Map<INFO, String> ctrlInfo) {
		this.printBanner();
		this.printMainInfo(ctrlInfo);
		this.printMainMenu();
		this.setInitialCursor();
	
		
	}
	/** Prints the configured banner in custom/random mode"*/
	public void printBanner() {
		print(Ansi.ansi().a(this.ansiBanner).a(TUtil.createCustomBanner(TUtil.getRandomBanner(), this.getBannerStartCol(), TUtil.getRandomEncode(), TUtil.getRandomDispay())).a("\n\n"));
	}
	
	/** Prints the configured banner in custom/random mode" but calls @method write from this class. See "write" method 4 more info.*/
	public void writeBanner() {
		write(this.ansiBanner,TUtil.createCustomBanner(TUtil.getRandomBanner(), this.getBannerStartCol(), TUtil.getRandomEncode(), TUtil.getRandomDispay()));
	}
	
	/** Prints the main menu. */ // TODO MAY BE DONE MORE EFFICIENtLY???
	public void printMainMenu() {
		int col = getMenuStartCol()-7;
		int row = this.menuRow;
		for(String header : TUtil.MENU_OP_HEADERS) {
			print(Ansi.ansi().cursor(row++,col).a(this.ansiMenuHeader).bold().a(header).boldOff());
			for(String option : TUtil.MENU_OPTIONS.get(header)) {
				print(this.ansiMenuOption);
				print(Ansi.ansi().cursor(row++, col).append(String.format("%-"+header.length()+"s", option)).a(this.ansiDefault));
			}
			row = this.menuRow;
			col +=24;
			
		}
	}
	
	/** Prints the main info. */
	public void printMainInfo(Map<INFO, String> ctrlInfo) {
		int i = 0;
		Ansi info = Ansi.ansi();
		String strInfo = null;
		for(INFO entry : ctrlInfo.keySet()) {
			strInfo += entry +": " + ctrlInfo.get(entry) + "   ";
			info.a(this.ansiInfo + entry.toString() + ": " + this.ansiDefault + ctrlInfo.get(entry) + "   ");
		}

		print(Ansi.ansi().cursor(this.infoRow, 2+(this.terminalWidth - (strInfo.length() - "   ".length()))/2));
		print(Ansi.ansi().a(info).a(this.ansiDefault));
		

	}

}

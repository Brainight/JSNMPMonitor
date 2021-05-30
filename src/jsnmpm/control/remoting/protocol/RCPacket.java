package jsnmpm.control.remoting.protocol;

import java.io.Serializable;

public class RCPacket implements Serializable{
	
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6341472658411071319L;
	
	private byte action;
	private String options;
	private Object data;

	public RCPacket() {
		
	}
	
	public RCPacket(byte action, String options, Object data) {
		this.action = action;
		this.options = options;
		this.data = data;
	}
	
	public void setAction(byte action) {
		this.action = action;
	}
	
	public  void setOptions(String options) {
		this.options = options;
	}
	
	public void setData(Object data) {
		this.data = data;
	}
	
	public byte getAction() {
		return this.action;
	}
	
	public String getOptions() {
		return this.options;
	}
	
	public Object getData() {
		return this.data;
	}
}

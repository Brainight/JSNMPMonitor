package jsnmpm.monitor.gui;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import jsnmpm.control.whisper.ProcessWhisper;
import jsnmpm.control.whisper.RequestWhisper;
import jsnmpm.control.whisper.TrapWhisper;
import jsnmpm.control.whisper.Whisper;

/**
 * This class handles all the asynchronous communication between SNMPController and a given GUI/Terminal Monitor.
 * @author MrStonedDog
 *
 */
public abstract class AbstractMonitor implements Subscriber<Whisper>, Runnable{

	/** Does nothing **/
	@Override
	public void onSubscribe(Subscription subscription) {}

	@Override
	public void onNext(Whisper item) {
		if(item instanceof ProcessWhisper) 
			this.processProcessWhisper((ProcessWhisper)item);
	
		else if(item instanceof RequestWhisper)
			this.processRequestWhisper((RequestWhisper)item);
		
		else if(item instanceof TrapWhisper)
			this.processTrapWhisper((TrapWhisper)item);
		
	}
	
	/**
	 * Processes a ProcessWhisper message send by the SNMPController.<br>
	 * These whispers normally contain data about an executing SNMPProcess.
	 * @param item
	 */
	public abstract void processProcessWhisper(ProcessWhisper processWhisper);
	
	/**
	 * Processes a RequestWhisper message send by the SNMPController.<br>
	 * These whispers normally contain data from an asynchronous SNMP Request
	 * @param item
	 */
	public abstract void processRequestWhisper(RequestWhisper requestWhisper);
	
	/** Processes a TrapWhisper message send by the SNMPController <br>
	 * These whisper contain data from a TRAP, NOTIFICATION, or other type of PDU.
	 * @param trapWhisper
	 */
	public abstract void processTrapWhisper(TrapWhisper trapWhisper);
	
	public Subscriber<Whisper> getPipe() {
		return this;
	}
	
	protected abstract void startMonitor();
	
	
	/** Does nothing **/
	@Override
	public void onError(Throwable throwable) {}
	/** Does nothing **/
	@Override
	public void onComplete() {}
	
	
	



}

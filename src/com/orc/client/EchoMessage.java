package com.orc.client;

/**
 * This is an object version of the message sent by EchoClient
 * 
 * Each message contains a timestamp
 * A message number
 * and data (optional) up to 4k
 * 
 * Data is sent in plaintext, * seperated, and terminated with a newline
 * 
 * For example
 * 
 * 1210630906171*22*This is a message\n
 * 1210630906171*23*\n
 * 
 * Notice that the second message contained no data but still had a seperator
 * 
 * 
 * @author jeff
 * @see EchoClient
 */
public class EchoMessage {
	
	public static final int MAX_LEN = 4 * 1024;
	
	public long sentTimestamp;
	public String dataLine;
	public int msgNum;
	
	/**
	 * Resets the variables for reuse
	 */
	public void reset() {
		sentTimestamp = -1;
		dataLine = null;
		msgNum = -1;
	}
}

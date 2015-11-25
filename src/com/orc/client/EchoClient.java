package com.orc.client;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

import com.orc.io.MultiplexAdapter;
import com.orc.io.SimpleClientContext;
import com.orc.io.SimpleClientMultiplexer;
import com.orc.server.EchoServer;

/**
 * This client is designed to communicate with an {@link EchoServer}
 * 
 * It will open the specified file and send each line as an {@link EchoMessage}
 * 
 * The client will write data whenever the server's channel is writeable and
 * read data whenever the server responds.
 * 
 * In this program throughput is regarded as more important than latency so messages
 * are queued as they are created off the file rather than sending each message as it
 * is created.
 * 
 * @author jeff
 * @see EchoMessage
 * @see EchoServer
 *
 */
public class EchoClient extends MultiplexAdapter {

	/****Setup*/
	private 						String target;
	private 						int port;
	private 						String fileName;
	
	/****IO*/
	private SimpleClientMultiplexer selector;
	private ByteBuffer 				outBuff = ByteBuffer.allocateDirect(8 * 1024);//Buffer for writing to server
	private ByteBuffer 				inBuff = ByteBuffer.allocateDirect(8 * 1024);	//Buffer for reading from server
	private byte[] 					messageHolder = new byte[ EchoMessage.MAX_LEN ];
	private EchoMessage 			msg = new EchoMessage();

	/****File handling*/
	private BufferedReader 			data;
	private byte[] 					nextLine;
	private boolean 				fileDone = false;
	private int 					curMessage = 0;
	private int 					maxMessage = -1;
	
	/****Stats data*/
	private long 					startTime;
	private long 					processedCount;
	private long 					processedLag;
	private float 					aveLag;
	
	/**
	 * The constructor takes the required information neccessary to run the client
	 * 
	 *  After EchoClient is created connect() and openFile() should be called
	 * 
	 * @param target	The name/IP of the EchoServer
	 * @param port		The port of the EchoServer
	 * @param fileName	The location of the file to be converted into messages
	 */
	public EchoClient(final String target, final int port, final String fileName) {
		this.target = target;
		this.port = port;
		this.fileName = fileName;
	}
	
	/**
	 * Creates a {@link SimpleClientMultiplexer}, which will in turn
	 * attempt to connect to the server.
	 * 
	 * @throws IOException
	 */
	public void connect() throws IOException {
		this.selector = new SimpleClientMultiplexer(this, target, port);
		this.selector.connect();
	}
	
	/**
	 * Attempt to open the file indicated by the constructor.  
	 *	
	 * @throws FileNotFoundException
	 */
	public void openFile() throws FileNotFoundException {
		this.data = new BufferedReader(new FileReader(fileName));
	}
	
	/**
	 * Connection to server established, mark startTime to track throughput
	 */
	public void onConnect(SocketChannel channel, Object attachment) {
		startTime = System.currentTimeMillis();
	}
	
	/**
	 * Closes the channel connection to the server.
	 * 
	 * @param channel	The connection to the server
	 */
	public void onDisconnect(final Channel channel) {
		System.out.println("Disconnecting from server");
		try {
			channel.close();
		} catch (IOException e) {
			System.err.println("Error closing channel: " + e);
		}
	}

	/**
	 * On read is called when the app recieves data from the server
	 * 
	 * @param channel		The channel with data
	 * @param attachment	SimpelClientContext, not used in this instance
	 */
	public void onRead(ReadableByteChannel channel, Object attachment) {

		try {
			int bytesread = channel.read(inBuff);

			if (bytesread<0) onDisconnect(channel);
			else {
				inBuff.flip();
				
				if (inBuff.limit()>0)
					seperateMessages(inBuff);
			}
		} catch (IOException e) {
			System.err.println("IOException reading from channel: " + e);
			onDisconnect(channel);
		}
		
		if (msg!=null && msg.msgNum==maxMessage) {
			onDisconnect(channel);
			printStats(true);
		}
	}
	
	/**
	 * This method will print out some stats about how the application ran
	 * and exit the program.
	 * 
	 * A customer using this app probably wouldn't want it to exit
	 * 
	 * @param boolean True if you want the app to exit when this method completes
	 */
	public void printStats(boolean exit) {

		long finalTime = System.currentTimeMillis();
		long runTime = finalTime - startTime;
		System.out.println("Final Message Processed");
		System.out.println("-------------------------------");
		System.out.println("*************Stats*************");
		System.out.println("-------------------------------");
		System.out.println("Total Messages  :" + processedCount);
		System.out.println("Ave Lag         :" + aveLag + "ms" );
		System.out.println("Start Timestamp :" + startTime);
		System.out.println("Final Timestamp :" + finalTime);
		
		System.out.println("Runtime         :" + runTime + "ms");
		System.out.println("Throughput      :" + (float)processedCount/runTime + "msgs/ms");
		
		if (exit) System.exit(1);
	}
	
	/**
	 * This method looks for newlines and sets the buffer's possition and limit
	 * accordingly.  If a partial message is received it will hold it until more data
	 * arrives.
	 * 
	 * @param inBuff	A buffer containing data from the server
	 * @return			A count of how many messages were successful parsed
	 */
	private int seperateMessages(ByteBuffer inBuff) {
		int limit = inBuff.limit();
        inBuff.mark();
        //boolean nextMsg = false;
        
        int messageCount = 0;
        
        //Step through the buffer looking for newlines
        while (inBuff.hasRemaining()) {
            if (inBuff.get() == '\n') {
				
                int end = inBuff.position();

                inBuff.reset();
                inBuff.limit(end - 1);//drop the newline

                //inBuff now is set to 1 message
                if (onMessage(inBuff)) {
            		updateStats(true);
                	messageCount++;
                }

                inBuff.limit(limit);
                inBuff.position(end);
                inBuff.mark();
				
            }
        }
		
		inBuff.reset();
		
		//Adjust for any incomplete messages, or clear the buffer
        if (inBuff.hasRemaining() ) {
            inBuff.compact();
        } else {
            inBuff.clear();
        }
		
		return messageCount;
	}
	
	/**
	 * This method recieves the ByteBuffer with it's limit and possition set to 
	 * exactly 1 message (without the newline).  The message is put into the class
	 * EchoMessage - since this is a single-threaded model I don't have to worry about
	 * something else coming along and changing it.
	 * 
	 * Note: for simplicity's sake I convert the message to a string and parse it down
	 * This is inefficient, but writing a good set of ByteBuffer parsing utilities is
	 * beyond the scope of this exercise.
	 * 
	 * @param buff		The incoming data, set to exactly one message
	 * @return boolean 	True on success, False if parsing failed 
	 */
	private boolean onMessage(ByteBuffer buff) {
		String dataAsString = null;
		msg.reset();
		try {
			int len = buff.remaining();
			buff.get(messageHolder, 0, len );

			//Inefficient, but simple - you need some utils to do buffer manipulation
			dataAsString = new String(messageHolder, 0, len);
			String[] dataAsArray = dataAsString.split("\\*");

			msg.sentTimestamp = Long.parseLong(dataAsArray[0]);
			msg.msgNum = Integer.parseInt(dataAsArray[1]);
			if (dataAsArray.length ==3) //data can be null
				msg.dataLine = dataAsArray[2];
		} catch(Exception e) {
			e.printStackTrace();
			System.err.println("Error String:" + dataAsString + ":");
 			return false;
		}
		
		return true;
	}
	
	/**
	 * Updates message stats after a message has been processed
	 * 
	 * @param writeOut	True if you want message information writen to System.out 
	 * @return true
	 */
	private boolean updateStats(boolean writeOut) {
		long msgLag = (System.currentTimeMillis()-msg.sentTimestamp);
		processedCount++;
		processedLag+=msgLag;
		aveLag = (float)processedLag/(float)processedCount;
		float throughput = (float)processedCount/((System.currentTimeMillis()-startTime));
		
		if (writeOut)
			System.out.println("Lag Time: " + msgLag + "| Ave lag=" + aveLag + "| Throughput=" + throughput + "msg/ms | MsgNum: " + msg.msgNum + "| data:" + msg.dataLine);
		
		return true;
	}

	/**
	 * This method attempts to write to the server.  Any data that cannot be written 
	 * in one pass is retained for whenever the server's channel is not blocking.
	 * 
	 * This method starts by attempting to load more data from the file
	 */
	public void onWrite(WritableByteChannel channel, Object attachment) {
		
		SimpleClientContext client = (SimpleClientContext)attachment;
		//in case of missing/unopened file
		if(data!=null)
			loadBuffer();//add data to buffer
		outBuff.flip();
		if (outBuff.hasRemaining()) {
			try {
				client.socket.write(outBuff);//write to server

				if (outBuff.hasRemaining() ) {
		            outBuff.compact();
		        } else {
		            outBuff.clear();
		        }
			} catch (IOException e) {
				System.err.println("Error writing to client: " + e);
			}
		}
	}

	/**
	 * This method reads from a file and creates EchoMessages.
	 * 
	 * If the last line read won't fit in the outgoing buffer it is retained for the
	 * next time the method is called
	 * 
	 * @see EchoMessage
	 */
	private void loadBuffer() {
		
		if (fileDone && nextLine==null) {
			return;
		}
			
		//while there's room in the buffer
		do {
			try {//read from the file
				if (nextLine==null) {
					String next = data.readLine();
					
					if (next==null) {
						fileDone = true;
						data.close();
						maxMessage = curMessage;
						continue;
					} else {
						curMessage++;
						nextLine = next.getBytes();
					}
				}
			} catch (IOException e) {
				System.err.println("Error reading from file: " + e);
			}
			
			//append to the buffer
			byte[] header = (System.currentTimeMillis() + "*" + curMessage + "*").getBytes();
			if (nextLine!=null && outBuff.remaining()>nextLine.length + header.length) {
				outBuff.put(header);
				outBuff.put(nextLine);
				outBuff.put("\n".getBytes());
				nextLine=null;
			}
		} while (!fileDone && nextLine==null);
	}
	
	/**
	 * Calls the multiplexer's run method
	 * @see SimpleClientMultiplexer
	 */
	public void start() {
		selector.run();
	}
	
	/**
	 * Simple main, accepts arguments for server name/ip, port and file
	 * 
	 * If none are passed it will use defaults
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		int port = 8080;
		String address = "localhost";
		String fileName = "c:\\huckfin.txt";
		if (args.length==3) {
			try {
				port = Integer.parseInt(args[0]);
				address = args[1];
				fileName = args[2];
			} catch (NumberFormatException e) {
				System.err.println("Invalid port: " + args[0]);
			}
		}
		
		EchoClient client = new EchoClient(address, port, fileName);

		try {
			client.connect();
			client.openFile();
			client.start();
		} catch(FileNotFoundException f) {
			System.err.println("Error, file '" + fileName + "' not found");
		} catch(IOException e) {
			System.err.println("Error connecting");
			e.printStackTrace();
		} catch(Exception x) {
			System.err.println("Unexpected error: " + x);
			x.printStackTrace();
		}
		
	}

}

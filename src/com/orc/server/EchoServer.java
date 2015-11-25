package com.orc.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ServerSocketChannel;

import com.orc.io.MultiplexAdapter;
import com.orc.io.SimpleClientContext;
import com.orc.io.SimpleServerMultiplexer;

/**
 * A simple echo server
 * 
 * This program will listen on a specified port and echo back
 * all data sent to it.
 * 
 * The data will not be processed, but it will be writen to System.out for
 * monitoring
 * 
 * @author jeff
 *
 */
public class EchoServer extends MultiplexAdapter {

	private SimpleServerMultiplexer selector;
	private ByteBuffer 				buff = ByteBuffer.allocate(8 * 1024);
	private final int				port;

	/**
	 * The constructor will create a new multiplexer and bind to the specified port
	 * @param port The port the server will listen on
	 */
	public EchoServer(final int port) {
		this.port = port;
	}
	
	/**
	 * Create the multiplexer and connect it
	 * @throws IOException
	 */
	public void connect() throws IOException {
		this.selector = new SimpleServerMultiplexer(this, port);
		this.selector.connect();
	}
	
	/**
	 * Calls selector.run()
	 * 
	 * The multiplexer is the event driver in this program
	 * @see SimpleServerMultiplexer
	 */
	public void start() {
		selector.run();
	}
	
	/**
	 * New client has connected, spit out a notification to the screen
	 */
	public void onAccept(ServerSocketChannel channel, Object attachment) {
		System.out.println("Accepting new client");
	}

	/**
	 * Callback from the mulitplexer telling the server that there is data to be read
	 * 
	 * The server is set to read up to 8k of data and then spit it back.
	 * 
	 * If there is more than 8k of data the multiplexer will call onRead again once the
	 * method exits
	 * 
	 * @see SimpleServerMultiplexer
	 */
	public void onRead(ReadableByteChannel channel, Object attachment) {
		SimpleClientContext client = (SimpleClientContext)attachment;
		
		
		try {
			int bytesread = channel.read(buff);
			if (bytesread<0) onDisconnect(channel);
			else {
				buff.flip();
				byte[] b = new byte[buff.limit()];
				buff.get(b);
				System.out.println(new String(b));
				buff.flip();
			}
			sendEcho(buff, client);
		} catch (IOException e) {
			System.err.println("IOException reading from channel: " + e);
			onDisconnect(channel);
		}
		buff.clear();
	}

	/**
	 * Called when an error is thrown while reading or writing.
	 * 
	 * This method will close the channel which will also cancel
	 * all future callbacks for the client
	 * 
	 * @param channel The channel to be closed
	 */
	public void onDisconnect(Channel channel) {
		System.out.println("Client Disconnected");
		try {
			channel.close();
		} catch (IOException e) {
			System.err.println("Error closing channel: " + e);
		}
	}

	/**
	 * Attempts to write to the client
	 * 
	 * A more robust model would use the onWrite method and watch for the
	 * client to be open for writing.  This also involves keeping track of
	 * how much data was written to the client, etc.
	 * 
	 * For this simple program if the client can't keep up, the server drops
	 * the client
	 * 
	 * @see SimpleServerMultiplexer
	 * @see SimpleClientContext
	 * 
	 * @param buff		The data to be written to the client
	 * @param client	The client context which references the socket channel
	 */
	private void sendEcho(ByteBuffer buff, SimpleClientContext client) {
		try {
			client.socket.write(buff);
		} catch (IOException e) {
			System.err.println("Error writing to client: " + e);
			onDisconnect(client.socket);
		}
	}

	/**
	 * Standard main - creates an instance of {@link EchoServer} and starts it
	 * 
	 * @param args 	Optional - the port the server should run on
	 * 				Default 8080
	 */
	public static void main(String[] args) {
		int port = 8080;
		if (args.length>0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.err.println("Invalid port: " + args[0]);
			}
		}
		
		EchoServer server = new EchoServer(port);
		try {
			server.connect();
			server.start();
		} catch (IOException e) {
			System.err.println("IOException: " + e);
			e.printStackTrace();
		} catch (Exception f) {
			System.err.println("Unexpected exception");
			f.printStackTrace();
		}
	}

	
}

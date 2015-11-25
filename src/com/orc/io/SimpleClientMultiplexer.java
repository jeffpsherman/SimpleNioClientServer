package com.orc.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * This is a simple multiplexer set up to assume the duties of a 
 * client connecting to a server.
 * 
 * As such it will deal with onConnect, onRead and onWrite but not onAccept
 * 
 * @see SimpleServerMultiplexer for a Server based multiplexer
 * @author jeff
 *
 */
public class SimpleClientMultiplexer {

	private Selector selector;
	private SelectionKey connectKey;
	private final MultiplexListener client;
	private SocketChannel socketChannel;
	
	private String target;
	private int port;

	/**
	 * Pass in the neccessary info
	 * 
	 * @param client 	The MultiplexListener that the Selector will 
	 * 					call back when events occur
	 * @param target	The server's name or IP
	 * @param port		The port the multiplexor will listen on
	 * @throws IOException
	 */
	public SimpleClientMultiplexer(final MultiplexListener client, final String target, final int port) {
		this.client = client;
		this.target = target;
		this.port = port;
	}
	
	/**
	 * 
	 * The simple client will attempt to connect and register callbacks in the constructor
	 * In a more robust model you would want to be able to control this, 
	 * but for this I'm favoring simplicity
	 * 
	 * @throws IOException
	 */
	public void connect() throws IOException {
		selector = Selector.open();
		
	    socketChannel = SocketChannel.open();
	    socketChannel.configureBlocking(false);
	  
	    socketChannel.connect(new InetSocketAddress(target, port));

	    //Note that we are registering for a Connect, not an Accept
	    connectKey = socketChannel.register(selector, SelectionKey.OP_CONNECT);
	}
	
	/**
	 * This is the heart of the multiplexor.
	 * 
	 * When the program enters this method it will block until an event occurs on the
	 * socket at which point the {@link MultiplexListener} will be called back.
	 * 
	 * In this case a connect will be called when the client establishes a connection with
	 * the server.
	 * 
	 * Read will be called when the server writes data to the client
	 * Write will be called when the client is able to write to the server
	 */
	public void run() {

		while (true) {
			try {
				selector.select();//this blocks, use select(timestamp) to block for a small amount of time
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> i = keys.iterator();

				while (i.hasNext()) {
					SelectionKey curKey = i.next();
			        i.remove();

			        /*
			         * makes sure that the key hasn't become invalid between getting
			         * the iterator and getting to the key
			         */
			        if (!curKey.isValid()) {
			        	continue;
			        }

					if (connectKey==curKey && curKey.isConnectable()) {
						SocketChannel outConnection = (SocketChannel) curKey.channel();
						outConnection.finishConnect();
						
						SelectionKey clientKey;

						/**
						 * Set up callbacks for read/write operations based on the state of the channel connected
						 * to the server
						 */
						clientKey = outConnection.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

						SimpleClientContext clientContext = new SimpleClientContext();
						clientContext.socket = outConnection;
						clientContext.key = clientKey;
						clientKey.attach(clientContext);

						client.onConnect(outConnection, clientKey.attachment());

					} else if (curKey.isReadable()){
						client.onRead((ReadableByteChannel)curKey.channel(), curKey.attachment());
					} else if (curKey.isWritable()) {
						client.onWrite((WritableByteChannel)curKey.channel(), curKey.attachment());
					}
					
				}
			} catch (IOException e) {
				System.err.println(e);

			} catch (Exception e) {
				System.err.println(e);
				e.printStackTrace();
			}
		}
	}
	

}

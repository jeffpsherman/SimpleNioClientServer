package com.orc.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * This is a simple multiplexer set up to assume the duties of a 
 * server allowing multiple clients to connect.
 *
 * It will accept connections for a {@link MultiplexListener} and callback
 * onAccept and onRead.  
 * 
 * 
 * @author jeff
 *
 */
public class SimpleServerMultiplexer {

	private Selector selector;
	private ServerSocketChannel server;
	private SelectionKey acceptKey;
	private MultiplexListener client;
	
	private int port;
	

	/**
	 * @param client 	The MultiplexListener that the Selector will 
	 * 					call back when events occur
	 * @param port		The port the multiplexor will listen on
	 * @throws IOException
	 */
	public SimpleServerMultiplexer(final MultiplexListener client, final int port) {
		this.client = client;
		this.port = port;
	}

	/**
	 * multiplexer will bind to the given port and begin
	 * listening.
	 * 
	 * Data won't be processed until run() is called
	 * @throws IOException
	 */
	public void connect() throws IOException {
		selector = Selector.open();
		server = ServerSocketChannel.open();
		server.socket().bind(new InetSocketAddress(port));
		server.configureBlocking(false);
		acceptKey = server.register(selector, SelectionKey.OP_ACCEPT);
	}
	
	
	/**
	 * This is the heart of the multiplexor.
	 * 
	 * When the program enters this method it will block until an event occurs on the
	 * socket at which point the {@link MultiplexListener} will be called back.
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

					if (curKey == acceptKey && curKey.isAcceptable()) {
						SocketChannel incomming = server.accept();
						incomming.configureBlocking(false);

						SelectionKey clientKey = incomming.register(selector, SelectionKey.OP_READ);

						//Register the client context with the selection key
						SimpleClientContext clientContext = new SimpleClientContext();
						clientContext.socket = incomming;
						clientContext.key = clientKey;
						clientKey.attach(clientContext);

						client.onAccept((ServerSocketChannel)server, clientKey.attachment());
						
					} else if (curKey.isReadable()){//client has written data to the server
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

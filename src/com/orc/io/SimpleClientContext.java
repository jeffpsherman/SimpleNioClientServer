package com.orc.io;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * A client context keeps track of a client's socket's, callback keys
 * and other data.
 * 
 * This is a very simplified client context, a more robust version would have it's own
 * {@link ByteBuffer}'s for keeping track of reading and writing.
 * 
 * For the simplified version I'm just keeping track of the SocketChannel
 * and SelectionKey
 * 
 * @author jeff
 *
 */
public class SimpleClientContext {

	/**
	 * This is the channel used to communicate to the client/server
	 * You need it to write data to the socket
	 */
	public SocketChannel 		socket;
	
	/**
	 * This is the reference key used by NIO to keep track of callbacks
	 * 
	 */
	public SelectionKey 		key;
	
}

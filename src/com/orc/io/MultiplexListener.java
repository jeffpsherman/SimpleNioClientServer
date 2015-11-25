package com.orc.io;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Interface for multiplexer call backs
 * 
 * NIO channel has 4 callback operations: connect, accept, read, write
 * This class provides a standard interface a multiplexer can use for callbacks
 * 
 * @author jeff
 *
 */
public interface MultiplexListener {

	/**
	 * A callback indicating that data has been written to the channel is ready
	 * for reading into the program
	 * @param channel A readable byte channel
	 * @param attachment Any object attached to the SelectKey by the multiplexer
	 */
	public void onRead(ReadableByteChannel channel, Object attachment);
	
	/**
	 * A callback indicating that the channel can be written to by the program
	 * @param channel A writeable byte channel
	 * @param attachment Any object attached to the SelectKey by the multiplexer
	 */
	public void onWrite(WritableByteChannel channel, Object attachment);
	
	/**
	 * A callback indicating that an outgoing connection has connected to
	 * it's target
	 * 
	 * @param channel The channel that has been connected to the target
	 * @param attachment Any object attached to the SelectKey by the multiplexer
	 */
	public void onConnect(SocketChannel channel, Object attachment);
	
	/**
	 * A callback indicating that a connection is incoming and has
	 * been accepted by the host
	 *  
	 * @param channel The host's channel containing the incoming connection 
	 * @param attachment Any object attached to the SelectKey by the multiplexer
	 */
	public void onAccept(ServerSocketChannel channel, Object attachment);
	
}

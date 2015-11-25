package com.orc.io;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

/**
 * This class implements {@link MultiplexListener} and 
 * allows an extending class to completely ignore channel opperations
 * that are not relavent to the class
 *
 * It implements all the methods in MultiplexListener, but does nothing
 * on callback
 * 
 * @author jeff
 *
 */
public abstract class MultiplexAdapter implements MultiplexListener {

	public void onAccept(ServerSocketChannel channel, Object attachment) {}

	public void onConnect(SocketChannel channel, Object attachment) {}

	public void onRead(ReadableByteChannel channel, Object attachment) {}

	public void onWrite(WritableByteChannel channel, Object attachment) {}

	
}

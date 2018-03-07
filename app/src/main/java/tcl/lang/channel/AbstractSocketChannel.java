package tcl.lang.channel;

import java.net.InetAddress;

import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclInteger;
import tcl.lang.TclList;
import tcl.lang.TclObject;
import tcl.lang.TclString;

/**
 * This abstract class allows fconfigure to query information about a socket channel
 * 
 */
public abstract class AbstractSocketChannel extends Channel {
	
	/**
	 * This method returns a list of three elements; these are the address, the
	 * host name and the port to which the peer socket is connected or bound. If
	 * the host name cannot be computed, the second element of the list is
	 * identical to the address, its first element
	 * 
	 * @param interp current interpreter, for errors
	 * @return TclList containing address, host name and port
	 * @throws TclException
	 */
	public TclObject getPeerName(Interp interp) throws TclException {
		InetAddress address = getPeerAddress();
		if (address==null) throw new TclException(interp, "can't get peername: socket is not connected");
		TclObject rv = TclList.newInstance();
		TclList.append(interp, rv, TclString.newInstance(address.getHostAddress()));
		TclList.append(interp, rv, TclString.newInstance(address.getHostName()));
		TclList.append(interp, rv, TclInteger.newInstance(getPeerPort()));
		return rv;
	}
	
	/**
	 * This option returns a list of three elements, the address, the host name
	 * and the port number for the socket. If the host name cannot be computed,
	 * the second element is identical to the address, the first element of the
	 * list.
	 * 
	 * @param interp
	 *            current interpreter, for errors
	 * @return TclList containing address, host name and port
	 * @throws TclException
	 */
	public TclObject getSockName(Interp interp) throws TclException {
		InetAddress address = getLocalAddress();
		if (address==null) throw new TclException(interp, "can't get localname");
		TclObject rv = TclList.newInstance();
		TclList.append(interp, rv, TclString.newInstance(address.getHostAddress()));
		TclList.append(interp, rv, TclString.newInstance(address.getHostName()));
		TclList.append(interp, rv, TclInteger.newInstance(getLocalPort()));
		return rv;
	}
	
	@Override
	String getChanType() {
		return "tcp";
	}
	
	/**
	 * if output translation is set to AUTO, sockets are crlf
	 * regardless of platform
	  *
	 * @see tcl.lang.channel.Channel#setOutputTranslation(int)
	 */
	@Override
	public void setOutputTranslation(int translation) {
		if (translation == TclIO.TRANS_AUTO) translation = TclIO.TRANS_CRLF;
		super.setOutputTranslation(translation);
	}

	/**
	 * This option gets the current error status of the given socket. This is
	 * useful when you need to determine if an asynchronous connect operation
	 * succeeded. If there was an error, the error message is returned. If there
	 * was no error, an empty string is returned.
	 * 
	 * @param interp current interpreter, for errors
	 * @return Error message or empty string if no error
	 * @throws TclException
	 */
	public abstract TclObject getError(Interp interp) throws TclException;
	
	/**
	 * @return the InetAddress of the peer, or null if it is not connected
	 */
	abstract InetAddress getPeerAddress();
	
	/**
	 * @return the port number of the peer, or 0 if it is not connected
	 */
	abstract int getPeerPort();
	
	/**
	 * @return the local inet address of the socket
	 */
	abstract InetAddress getLocalAddress();
	
	/**
	 * @return the local port number of the socket
	 */
	abstract int getLocalPort();

}

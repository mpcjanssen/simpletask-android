/*
 * ResourceChannel.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ResourceChannel.java,v 1.1 2009/06/18 15:17:03 rszulgo Exp $
 *
 */

package tcl.lang.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclPosixException;
import tcl.lang.TclRuntimeError;

/**
 * Subclass of the abstract class Channel. It implements all of the methods to
 * perform read, write, open, close, etc on a file.
 */

public class ResourceChannel extends Channel {

	/**
	 * Resource files are read only.
	 */

	private InputStream file = null;

	/**
	 * Open a resource with the read/write permissions determined by modeFlags.
	 * This method must be called before any other methods will function
	 * properly.
	 * 
	 * @param interp
	 *            currrent interpreter.
	 * @param fileName
	 *            the absolute path of the resource to open
	 * @param modeFlags
	 *            modes used to open a file for reading, writing, etc
	 * @return the channelId of the file.
	 * @exception TclException
	 *                is thrown when the modeFlags is anything other than RDONLY
	 *                or the resource doesn't exists
	 * @exception IOException
	 *                is thrown when an IO error occurs that was not correctly
	 *                tested for. Most cases should be caught.
	 */

	public String open(Interp interp, String fileName, int modeFlags) throws IOException, TclException {

		mode = modeFlags;

		// disallow any mode except read

		if (modeFlags != TclIO.RDONLY) {
			throw new TclException(interp, "invalid mode(s), only RDONLY mode allowed for resource:");
		}

		try {
			file = interp.getClassLoader().getResourceAsStream(fileName);
		} catch (java.lang.NullPointerException npe) {
			throw new TclPosixException(interp, TclPosixException.ENOENT, true,
					"ResourceChannel.open: no file specified for \"resource:\" ");
		}

		if (file == null) {
			throw new TclPosixException(interp, TclPosixException.ENOENT, true,
					"ResourceChannel.open: cannot find \"resource:" + fileName + "\"");
		}

		// In standard Tcl fashion, set the channelId to be "resource" + the
		// value of the current FileDescriptor.

		String fName = TclIO.getNextDescriptor(interp, "resource");
		setChanName(fName);
		return fName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tcl.lang.channel.Channel#implClose()
	 */
	@Override
	void implClose() throws IOException {
		if (file == null) {
			throw new TclRuntimeError("ResourceChannel.close(): null file object");
		}
		file.close();
		file = null;
	}

	@Override
	String getChanType() {
		return "resource";
	}

	@Override
	protected InputStream getInputStream() throws IOException {
		return file;
	}

	@Override
	protected OutputStream getOutputStream() throws IOException {
		throw new IOException("ResourceChannel: output stream not available");
	}
}

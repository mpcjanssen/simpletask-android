/*
 * FileChannel.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: FileChannel.java,v 1.22 2006/07/11 09:10:44 mdejong Exp $
 *
 */

package tcl.lang.channel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.SyncFailedException;

import tcl.lang.FileUtil;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclPosixException;
import tcl.lang.TclRuntimeError;

/**
 * Subclass of the abstract class SeekableChannel. It implements all of the
 * methods to perform read, write, open, close, etc on a file.
 */
public class FileChannel extends SeekableChannel {

	/**
	 * The file needs to have a file pointer that can be moved randomly within
	 * the file. The RandomAccessFile is the only java.io class that allows this
	 * behavior.
	 */
	private RandomAccessFile file = null;

	/**
	 * Open a file with the read/write permissions determined by modeFlags. This
	 * method must be called before any other methods will function properly.
	 * 
	 * @param interp
	 *            currrent interpreter.
	 * @param fileName
	 *            the absolute path or name of file in the current directory to
	 *            open
	 * @param modeFlags
	 *            modes used to open a file for reading, writing, etc
	 * @return the channelId of the file.
	 * @exception TclException
	 *                is thrown when the modeFlags try to open a file it does
	 *                not have permission for or if the file dosent exist and
	 *                CREAT wasnt specified.
	 * @exception IOException
	 *                is thrown when an IO error occurs that was not correctly
	 *                tested for. Most cases should be caught.
	 */

	public String open(Interp interp, String fileName, int modeFlags) throws IOException, TclException {

		mode = modeFlags;
		File fileObj = FileUtil.getNewFileObj(interp, fileName);
		boolean appendModeCreate = ((modeFlags & TclIO.APPEND)!=0) && ! fileObj.exists();

		// Raise error if file exists and both CREAT and EXCL are set

		if (((modeFlags & TclIO.CREAT) != 0) && ((modeFlags & TclIO.EXCL) != 0) && fileObj.exists()) {
			throw new TclException(interp, "couldn't open \"" + fileName + "\": file exists");
		}

		if (((modeFlags & TclIO.CREAT) != 0) && !fileObj.exists()) {
			// Creates the file and closes it so it may be
			// reopened with the correct permissions. (w, w+, a+)

			file = new RandomAccessFile(fileObj, "rw");
			file.close();
		}

		if ((modeFlags & TclIO.RDWR) != 0) {
			// Opens file (r+), error if file does not exist unless opening in append mode
			if (! appendModeCreate) {
				checkFileExists(interp, fileObj);
				checkReadWritePerm(interp, fileObj, 0);
			}
			if (fileObj.isDirectory()) {
				throw new TclException(interp, "couldn't open \"" + fileName + "\": illegal operation on a directory");
			}

			file = new RandomAccessFile(fileObj, "rw");

		} else if ((modeFlags & TclIO.RDONLY) != 0) {
			// Opens file (r), error if file does not exist.

			checkFileExists(interp, fileObj);
			checkReadWritePerm(interp, fileObj, -1);

			if (fileObj.isDirectory()) {
				throw new TclException(interp, "couldn't open \"" + fileName + "\": illegal operation on a directory");
			}

			file = new RandomAccessFile(fileObj, "r");

		} else if ((modeFlags & TclIO.WRONLY) != 0) {
			// Opens file, error if doesn't exist, unless opening in append mode
			if (! appendModeCreate) {
				checkFileExists(interp, fileObj);
				checkReadWritePerm(interp, fileObj, 1);
			}
			if (fileObj.isDirectory()) {
				throw new TclException(interp, "couldn't open \"" + fileName + "\": illegal operation on a directory");
			}

			// Currently there is a limitation in the Java API.
			// A file can only be opened for read OR read-write.
			// Therefore if the file is write only, Java cannot
			// open the file. Throw an error indicating this
			// limitation.

			if (!appendModeCreate && !fileObj.canRead()) {
				throw new TclException(interp, "Java IO limitation: Cannot open a file "
						+ "that has only write permissions set.");
			}
			file = new RandomAccessFile(fileObj, "rw");

		} else {
			throw new TclRuntimeError("FileChannel.java: invalid mode value");
		}

		// If we are appending, move the file pointer to EOF.

		if ((modeFlags & TclIO.APPEND) != 0) {
			file.seek(file.length());
		}

		// Truncate file to zero length, this has to be done after
		// opening the file so that it will not fail even when another
		// handle to this same file is also open.

		if ((modeFlags & TclIO.TRUNC) != 0) {
			java.nio.channels.FileChannel chan = file.getChannel();
			chan.truncate(0);
		}

		// In standard Tcl fashion, set the channelId to be "file" + the
		// value of the current FileDescriptor.

		String fName = TclIO.getNextDescriptor(interp, "file");
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
			throw new TclRuntimeError("FileChannel.close(): null file object");
		}
		file.close();
		file = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tcl.lang.channel.Channel#getSync()
	 */
	@Override
	void sync() throws SyncFailedException, IOException {
		if (file != null)
			file.getChannel().force(closed);
	}

	/**
	 * Move the file pointer internal to the RandomAccessFile object. The file
	 * MUST be open or a TclRuntimeError is thrown.
	 * 
	 * @param offset
	 *            The number of bytes to move the file pointer
	 */

	@Override
	void implSeek(long offset) throws IOException {
		if (file == null) {
			throw new TclRuntimeError("FileChannel.seek(): null file object");
		}
		file.seek(offset);
	}

	/**
	 * Return the current offset of the file pointer in number of bytes from the
	 * beginning of the file. The file MUST be open or a TclRuntimeError is
	 * thrown.
	 * 
	 * @return The current value of the file pointer.
	 */
	@Override
	long implTell() throws IOException {
		if (file == null) {
			throw new TclRuntimeError("FileChannel.implTell(): null file object");
		}
		return file.getFilePointer();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tcl.lang.channel.SeekableChannel#maxSeek()
	 */
	@Override
	long getMaxSeek() throws IOException {
		if (file == null) {
			throw new TclRuntimeError("FileChannel.getMaxSeek(): null file object");
		}
		return file.length();
	}

	/**
	 * If the file dosent exist then a TclExcpetion is thrown.
	 * 
	 * @param interp
	 *            currrent interpreter.
	 * @param fileObj
	 *            a java.io.File object of the file for this channel.
	 */

	private void checkFileExists(Interp interp, File fileObj) throws TclException {
		if (!fileObj.exists()) {
			throw new TclPosixException(interp, TclPosixException.ENOENT, true, "couldn't open \"" + fileObj.getName()
					+ "\"");
		}
	}

	/**
	 * Checks the read/write permissions on the File object. If inmode is less
	 * than 0 it checks for read permissions, if mode greater than 0 it checks
	 * for write permissions, and if it equals 0 then it checks both.
	 * 
	 * @param interp
	 *            currrent interpreter.
	 * @param fileObj
	 *            a java.io.File object of the file for this channel.
	 * @param inmode
	 *            what permissions to check for.
	 */

	private void checkReadWritePerm(Interp interp, File fileObj, int inmode) throws TclException {
		boolean error = false;

		if (inmode <= 0) {
			if (!fileObj.canRead()) {
				error = true;
			}
		}
		if (inmode >= 0) {
			if (!fileObj.canWrite()) {
				error = true;
			}
		}
		if (error) {
			throw new TclPosixException(interp, TclPosixException.EACCES, true, "couldn't open \"" + fileObj.getName()
					+ "\"");
		}
	}

	@Override
	String getChanType() {
		return "file";
	}

	@Override
	protected InputStream getInputStream() throws IOException {
		if (file == null)
			throw new IOException("file has not been opened, or has been closed");
		return new FileInputStream(file.getFD());
	}

	@Override
	protected OutputStream getOutputStream() throws IOException {

		// wrap the RandomAccessFile object in an OutputStream, because
		// FileOutputStream(file.getFD()) doesn't appear writes after a seek
		// past EOF - it simply writes at the EOF. No implementation is provided
		// for close() or flush() because those are handled by FileChannel
		// directly.
		return new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				if (file != null)
					file.write(b);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see java.io.OutputStream#write(byte[], int, int)
			 */
			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				if (file != null)
					file.write(b, off, len);
			}

		};
	}
}

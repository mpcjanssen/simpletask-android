/*
 * Channel.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Channel.java,v 1.27 2006/07/07 23:36:00 mdejong Exp $
 */

package tcl.lang.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.SyncFailedException;
import java.io.Writer;

import tcl.lang.Interp;
import tcl.lang.TclByteArray;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclObject;
import tcl.lang.TclPosixException;
import tcl.lang.TclRuntimeError;
import tcl.lang.TclString;
import tcl.lang.cmd.EncodingCmd;

/**
 * The Channel class provides functionality that will be needed for any type of
 * Tcl channel. It performs generic reads, writes, without specifying how a
 * given channel is actually created. Each new channel type will need to extend
 * the abstract Channel class and override any methods it needs to provide a
 * specific implementation for.
 */

public abstract class Channel {

	/**
	 * The read, write, append and create flags are set here. The variables used
	 * to set the flags are found in the class TclIO.
	 */
	protected int mode;

	/**
	 * This is a unique name that sub-classes need to set via setChanName(). It
	 * is used as the key in the hashtable of registered channels (in interp).
	 */
	private String chanName;

	/**
	 * How many interpreters hold references to this IO channel?
	 */
	public int refCount = 0;

	/**
	 * Set to false when channel is in non-blocking mode.
	 */
	protected boolean blocking = true;

	/**
	 * The input buffer for this channel.
	 */
	protected InputBuffer inputBuffer = null;

	/**
	 * The underlying input stream offered by the implementation
	 */
	protected InputStream rawInputStream = null;
	/**
	 * The underlying output stream offered by the implementation
	 */
	protected OutputStream rawOutputStream = null;
	/**
	 * The EOL input filter for this channel.
	 */
	protected EolInputFilter eolInputFilter = null;

	/**
	 * The EOF input filter for this channel, which translates the EOF
	 * character.
	 */
	protected EofInputFilter eofInputFilter = null;

	/**
	 * The unicode decoder for reading characters on this channel
	 */
	protected UnicodeDecoder unicodeDecoder = null;

	/**
	 * This InputStream is the source of processed bytes for Channel.read()
	 */
	protected InputStream finalInputStream = null;
	/**
	 * Allows backtracking in the stream for this channel
	 */
	protected MarkableInputStream markableInputStream = null;

	/**
	 * This Reader is the source of processed characters for Channel.read()
	 */
	protected Reader finalReader = null;

	/**
	 * This Writer translates EOL characters on output, and performs BUFF_LINE
	 * flushes
	 */
	protected EolOutputFilter eolOutputFilter = null;

	/**
	 * This Writer translates Unicode to bytes for output
	 */
	protected UnicodeEncoder unicodeEncoder = null;

	/**
	 * This OutputStream buffers output data for the Channel
	 */
	protected OutputBuffer outputBuffer = null;

	/**
	 * This OutputStream writes the EOF character, and prevents this Chanel's
	 * getOutputStream() from being closed by the chain.
	 */
	protected EofOutputFilter eofOutputFilter = null;

	/**
	 * This OutputStream prevents outputs from blocking on a non-blocking
	 * channel
	 */
	protected NonBlockingOutputStream nonBlockingOutputStream = null;

	/**
	 * This Writer is the Channel's entry into the output chain
	 */
	protected Writer firstWriter = null;

	/**
	 * This OutputStream is the Channel's entry into the output chain for
	 * efficient byte writes
	 */
	protected OutputStream firstOutputStream = null;

	/**
	 * Buffering (full,line, or none)
	 */
	protected int buffering = TclIO.BUFF_FULL;

	/**
	 * Buffer size, in bytes, allocated for channel to store input or output
	 */
	protected int bufferSize = 4096;

	/**
	 * Name of Java encoding for this Channel. A null value means use no
	 * encoding (binary).
	 */
	protected String encoding;

	/**
	 * Input translation mode for end-of-line character
	 */
	protected int inputTranslation = TclIO.TRANS_AUTO;

	/**
	 * Output translation mode for end-of-line character
	 */
	protected int outputTranslation = TclIO.TRANS_PLATFORM;

	/**
	 * If nonzero, use this as a signal of EOF on input.
	 */
	protected char inputEofChar = 0;

	/**
	 * If nonzero, append this to a writeable channel on close.
	 */
	protected char outputEofChar = 0;

	/**
	 * Set to true when eof is seen
	 */
	boolean eofSeen = false;

	/**
	 * Thread id of reader owning thread, or -1 if no owner
	 */
	long readOwningThread = -1;

	/**
	 * Thread id of writer owning thread, or -1 if no owner
	 */
	long writeOwningThread = -1;
	
	/**
	 * Indicate that thread wants read ownership
	 */
	public static final int READ_OWNERSHIP = 1;
	
	/**
	 * Indicate tht thread wants write ownership
	 */
	public static final int WRITE_OWNERSHIP = 2;

	/**
	 * This object is notified when the channel is no longer owned
	 */
	Object ownershipNotifier = new Object();

	/**
	 * Set to true after close() is called
	 */
	volatile boolean closed = false;

	/**
	 * Set to true when the encoding changes between reads
	 */
	boolean encodingChangedSinceLastRead = false;

	Channel() {
		setEncoding(EncodingCmd.systemJavaEncoding);
	}

	/**
	 * Tcl_ReadChars -> read
	 * 
	 * Read data from this Channel into the given TclObject.
	 * 
	 * @param interp
	 *            is used for TclExceptions.
	 * @param tobj
	 *            the object that data will be added to.
	 * @param readType
	 *            specifies if the read should read the entire input
	 *            (TclIO.READ_ALL), the next line without the end-of-line
	 *            character (TclIO.READ_LINE), or a specified number of bytes
	 *            (TclIO.READ_N_BYTES).
	 * @param numBytes
	 *            the number of bytes/chars to read. Ignored unless readType is
	 *            TclIO.READ_N_BYTES.
	 * @return the number of bytes read. Returns -1 on EOF or on error.
	 * @exception TclException
	 *                is thrown if read occurs on WRONLY channel.
	 * @exception IOException
	 *                is thrown when an IO error occurs that was not correctly
	 *                tested for. Most cases should be caught.
	 */
	public int read(Interp interp, TclObject tobj, int readType, int numBytes) throws IOException, TclException {

		if (!setOwnership(true, READ_OWNERSHIP)) {
			throw new TclException(interp, "channel is busy");
		}
		try {
			checkRead(interp);
			initInput();

			encodingChangedSinceLastRead = false;

			if (eofSeen) {
				setOwnership(false, READ_OWNERSHIP);
				return -1;
			}

			/* do we read characters or bytes? Must read characters if encoding is not binary or a non-LF EOL char */
			boolean readChars = ! (encoding == null && (inputTranslation == TclIO.TRANS_BINARY || inputTranslation == TclIO.TRANS_LF));
			if (readChars) {
				TclString.empty(tobj);
			} else {
				TclByteArray.setLength(interp, tobj, 0);
			}

			switch (readType) {
			case TclIO.READ_ALL:
				/*
				 * Read the whole of the input (or at least Integer.MAX_VALUE
				 * bytes, which won't read large files)
				 */
				numBytes = Integer.MAX_VALUE;
				// and fall through to READ_N_BYTES
			case TclIO.READ_N_BYTES: {
				/*
				 * Read a specific number of bytes from the input
				 */
				int cnt = 0;
				int total = 0;
				char[] buf = null;
				int bufsize = numBytes < 8192 ? numBytes : 8192;
				if (readChars)
					buf = new char[bufsize];
				else
					TclByteArray.setLength(interp, tobj, 0);
				while (total < numBytes) {

					if (readChars)
						cnt = finalReader.read(buf, 0, Math.min(buf.length, numBytes - total));
					else {
						/* resize array */
						int curBufLen = TclByteArray.getLength(interp, tobj);
	                        
                        if (curBufLen < total + bufsize) {
                            TclByteArray.setLength(interp, tobj, curBufLen + bufsize);
                        }

						/*
						 * if we are reading unprocessed bytes, this is more
						 * efficient because it avoids UnicodeDecoder's byte ->
						 * char conversion
						 */
						cnt = finalInputStream.read(TclByteArray.getBytes(interp, tobj), total, Math.min(bufsize,
								numBytes - total));
					}
					if (cnt == -1) {
						eofSeen = true;
						break;
					}
					if (cnt == 0 && (!blocking)) {
						setEofSeenWithoutRead(); // perhaps it's an EOF that we
													// can flag, some tests in
													// io.test require that
													// eof() be detected
													// immediately in
													// non-blocking mode
						break;
					}
					if (readChars)
						TclString.append(tobj, buf, 0, cnt);
					total += cnt;
				}
				if (!readChars) {
					// trim the TclByteArray
					TclByteArray.setLength(interp, tobj, total);
				}
				if (eofSeen && total == 0) {
					setOwnership(false, READ_OWNERSHIP);
					return -1;
				}
				setOwnership(false, READ_OWNERSHIP);
				return total;
			}
			case TclIO.READ_LINE: {

				if (finalReader != eolInputFilter) {
					throw new TclRuntimeError("finalReader != eolInputFilter, programmer error!");
				}
				StringBuffer sb = new StringBuffer(64);
				int rv = eolInputFilter.readLine(sb, blocking);
				TclString.empty(tobj);
				setOwnership(false, READ_OWNERSHIP);

				switch (rv) {

				case EolInputFilter.COMPLETE_LINE:
					TclString.append(tobj, sb.toString());
					eofSeen = eolInputFilter.eofSeen();
					return sb.length();

				case EolInputFilter.EOF:
					eofSeen = true;
					return -1;

				case EolInputFilter.INCOMPLETE_LINE:
					setEofSeenWithoutRead(); // perhaps we can set eofSeen, some
												// tests in io.test require that
												// eof() be detected immediately
					return -1;
				}

			}
			default:
				throw new TclRuntimeError("Channel.read: Invalid read mode.");
			}
		} finally {
			setOwnership(false, READ_OWNERSHIP);
		}
	}

	/**
	 * Tcl_WriteObj -> write
	 * 
	 * Write data to the Channel
	 * 
	 * @param interp
	 *            is used for TclExceptions.
	 * @param outData
	 *            the TclObject that holds the data to write.
	 */

	public void write(Interp interp, TclObject outData) throws IOException, TclException {
		if (!setOwnership(true, WRITE_OWNERSHIP)) {
			throw new TclException(interp, "channel is busy");
		}
		try {
			checkWrite(interp);
			initOutput();

			if (outData.isByteArrayType() && encoding == null
					&& (outputTranslation == TclIO.TRANS_BINARY || outputTranslation == TclIO.TRANS_LF)) {
				/* Can write with the more efficient firstOutputStream */
				firstOutputStream.write(TclByteArray.getBytes(interp, outData), 0, TclByteArray.getLength(interp,
						outData));
				/*
				 * Step in to do line buffering, since we bypassed
				 * EolOutputFilter
				 */
				if (buffering == TclIO.BUFF_LINE) {
					byte[] bytes = TclByteArray.getBytes(interp, outData);
					for (byte b : bytes) {
						if (b == 0x0A) {
							firstOutputStream.flush();
							break;
						}
					}
				}
			} else {
				char[] cbuf;
				if (outData.isByteArrayType() && encoding != null) {
					/* Read the bytearray according to the system encoding */
					cbuf = TclByteArray.decodeToString(interp, outData, EncodingCmd.systemTclEncoding).toCharArray();
					if (cbuf.length == 0 && TclByteArray.getLength(interp, outData) > 0) {
						/*
						 * Must have had a bad encoding translation; throw an
						 * exception. This is based on io.test io-60.1
						 */
						throw new TclException(interp, "error writing \"" + getChanName() + "\": invalid argument");
					}
				} else {
					cbuf = outData.toString().toCharArray();
				}
				firstWriter.write(cbuf, 0, cbuf.length);
			}
		} finally {
			setOwnership(false, WRITE_OWNERSHIP);
		}
	}

	/**
	 * Tcl_WriteChars -> write
	 * 
	 * Write string data to the Channel.
	 * 
	 * @param interp
	 *            is used for TclExceptions.
	 * @param outStr
	 *            the String object to write.
	 */
	public void write(Interp interp, String outStr) throws IOException, TclException {
		write(interp, TclString.newInstance(outStr));
	}

	/**
	 * Ask for or give up ownership to channel
	 * 
	 * @param takeOwnership
	 *            if true, give Channel ownership to the current thread if no
	 *            other thread owns it; otherwise, give up ownership if the
	 *            current thread owns it
	 * @param type
	 * 			 	Either READ_OWNERSHIP or WRITE_OWNERSHIP          
	 * 
	 * @return If takeOwnership is true, returns true if the current thread
	 *         successfully received ownership. If takeOwnership is false,
	 *         returns true if the current thread was the owner of the channel.
	 */
	public synchronized boolean setOwnership(boolean takeOwnership, int type) {
		return setOwnership(takeOwnership, type, Thread.currentThread().getId());
	}

	/**
	 * Ask for or give up ownership to channel
	 * 
	 * @param takeOwnership
	 *            if true, give Channel ownership to the current thread if no
	 *            other thread owns it; otherwise, give up ownership if the
	 *            current thread owns it
	 * @param type
	 * 			   Either READ_OWNERSHIP or WRITE_OWNERSHIP
	 * @param threadId
	 *            Thread.getId() of thread that should own the channel
	 * 
	 * @return If takeOwnership is true, returns true if the current thread
	 *         successfully received ownership. If takeOwnership is false,
	 *         returns true if the current thread was the owner of the channel.
	 */
	public boolean setOwnership(boolean takeOwnership, int type, long threadId) {
		synchronized (ownershipNotifier) {
			long owningThread = (type == READ_OWNERSHIP ? readOwningThread : writeOwningThread);
			if (type != READ_OWNERSHIP && type != WRITE_OWNERSHIP) return false;
			if (owningThread < 0 || threadId == owningThread) {
				if (takeOwnership) {
					if (type==READ_OWNERSHIP) readOwningThread = threadId;
					if (type==WRITE_OWNERSHIP) writeOwningThread = threadId;
				} else {
					if (type==READ_OWNERSHIP) readOwningThread = -1;
					if (type==WRITE_OWNERSHIP) writeOwningThread = -1;
					ownershipNotifier.notifyAll();
				}
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Block until ownership to this channel is granted to the current thread
	 * 
	 * @param type  Either READ_OWNERSHIP or WRITE_OWNERSHIP
	 * 
	 * @throws InterruptedException
	 */
	public void waitForOwnership(int type) throws InterruptedException {
		long threadId = Thread.currentThread().getId();
		synchronized (ownershipNotifier) {
			while (!setOwnership(true, type, threadId)) {
				ownershipNotifier.wait();
			}
		}
	}

	/**
	 * Channels subclasses should override this to perform any specific close()
	 * operations, including closing of the getInputStream() and
	 * getOutputStream() streams if necessary. impClose() is called by
	 * NonBlockingOutputStream.Transaction.perform() after the input and output
	 * chain of readers, writers and streams are closed, or directly by
	 * Channel.close() if the channel is read-only.
	 * 
	 * @throws IOException
	 */
	abstract void implClose() throws IOException;

	/**
	 * Close the Channel. The channel is only closed, it is the responsibility
	 * of the "closer" to remove the channel from the channel table. Channel
	 * subclass specific closing is done is impClose(), which is called on
	 * behalf of this method by nonBlockingOutputStream for writable channels.
	 * Note that any thread can close the channel, even if it is not the owner.
	 */
	public synchronized void close() throws IOException {
		IOException ex = null;
		boolean implCloseCalled = false;

		closed = true;

		if (finalReader != null) {
			try {
				finalReader.close();
			} catch (IOException e) {
				ex = e;
			}

			finalReader = null;
			finalInputStream = null;
			eolInputFilter = null;
			unicodeDecoder = null;
			markableInputStream = null;
			inputBuffer = null;
			eofInputFilter = null;

		}

		if (firstWriter != null) {
			try {
				firstWriter.close();
				// nonBlockingOutputStream called implClose(), in a possibly
				// non-blocking
				implCloseCalled = true;
			} catch (IOException e) {
				ex = e;
			}
			firstWriter = null;
			firstOutputStream = null;
			eolOutputFilter = null;
			unicodeEncoder = null;
			outputBuffer = null;
			eofOutputFilter = null;
			nonBlockingOutputStream = null;
		}

		if (!implCloseCalled)
			implClose();

		if (ex != null)
			throw ex;
	}

	/**
	 * Flush the Channel.
	 * 
	 * @throws TclException
	 *             when attempting to flush a read only channel.
	 * @throws IOException
	 *             for all other flush errors.
	 */
	public void flush(Interp interp) throws IOException, TclException {

		checkWrite(interp);
		if (!setOwnership(true, WRITE_OWNERSHIP)) {
			throw new TclException(interp, "channel is busy");
		}
		try {
			if (firstWriter != null) {
				firstWriter.flush();
			}
		} finally {
			setOwnership(false, WRITE_OWNERSHIP);
		}
	}

	/**
	 * Channel subclasses should override this to flush any operating system
	 * buffers for this Channel out to the physical medium. The default
	 * implementation here does nothing. FileChannel and similar classes should
	 * override this to provide sync() capability.
	 */
	void sync() throws SyncFailedException, IOException {
	}

	/**
	 * Move the current file pointer. Channels that support seek and tell should
	 * use SeekableChannel.
	 * 
	 * @param interp
	 *            currrent interpreter.
	 * @param offset
	 *            The number of bytes to move the file pointer.
	 * @param mode
	 *            where to begin incrementing the file pointer; beginning,
	 *            current, end.
	 * @throws TclException
	 *             if channel does not support seek
	 * @throws IOException
	 */
	public void seek(Interp interp, long offset, int mode) throws IOException, TclException {
		throw new TclPosixException(interp, TclPosixException.EINVAL, true, "error during seek on \"" + getChanName()
				+ "\"");
	}

	/**
	 * @return the current file position. If tell is not supported on the given
	 *          channel then -1 will be returned. Subclasses should use the
	 *          SeekableChannel class to provide tell() capability.
	 */
	public long tell() throws IOException {
		return -1;
	}

	/**
	 * If we can detect EOF without another read, set eofSeen. Default
	 * implementation can't do much, but SeekableChannel can.
	 */
	void setEofSeenWithoutRead() {

	}

	/**
	 * Setup the input stream chain on the first read
	 */
	protected void initInput() throws IOException {
		if (finalReader != null)
			return;

		/*
		 * Set up the chain of Readers and InputStreams:
		 * 
		 * channel implementation's InputStream -> eofInputFilter -> inputBuffer
		 * -> MarkableInputStream -> unicodeEncoder -> eolInputFilter
		 */
		rawInputStream = getInputStream();
		eofInputFilter = new EofInputFilter(rawInputStream, (byte) (inputEofChar & 0xff));
		inputBuffer = new InputBuffer(eofInputFilter, bufferSize, buffering, blocking, this);
		markableInputStream = new MarkableInputStream(inputBuffer);
		unicodeDecoder = new UnicodeDecoder(markableInputStream, encoding);
		eolInputFilter = new EolInputFilter(unicodeDecoder, inputTranslation);

		/* read() gets characters from finalReader */
		finalReader = eolInputFilter;
		/* read() gets bytes from finalInputStream */
		finalInputStream = markableInputStream;

		encodingChangedSinceLastRead = false;

		closed = false;
	}

	/**
	 * Setup output stream chain on the first write
	 */
	protected void initOutput() throws IOException {
		if (firstWriter != null)
			return;

		/*
		 * Set up the chain of Writers and OutputStreams: EolOutputFilter ->
		 * UnicodeEncoder -> OutputBuffer -> EofOutputFilter
		 * NonBlockingOutputStream -> Channel's getOutputStream()
		 */
		rawOutputStream = getOutputStream();
		eofOutputFilter = new EofOutputFilter(rawOutputStream, (byte) (outputEofChar & 0xff));
		nonBlockingOutputStream = new NonBlockingOutputStream(eofOutputFilter, blocking, this);
		outputBuffer = new OutputBuffer(nonBlockingOutputStream, bufferSize, buffering);
		unicodeEncoder = new UnicodeEncoder(outputBuffer, encoding);
		eolOutputFilter = new EolOutputFilter(unicodeEncoder, outputTranslation);

		/* Characters are written to firstWriter */
		firstWriter = eolOutputFilter;

		/* Bytes are more efficiently written to firstOutputStream */
		firstOutputStream = outputBuffer;

		closed = false;
	}

	/**
	 * @return true if the channel is writable, according to the 'fileevent'
	 *         definition
	 */
	boolean isWritable() {
		if (outputBuffer == null && !closed)
			return true;
		if (closed)
			return false;
		if (outputBuffer.getBufferedByteCount() >= bufferSize)
			return false;
		return true;
	}

	/**
	 * @return true if the channel is readable, according to the 'fileevent'
	 *         definition
	 */
	boolean isReadable() {
		if (inputBuffer == null)
			return false;
		if (encodingChangedSinceLastRead)
			return true;
		if (inputBuffer.eof())
			return true;
		try {
			/* Line buffering may be waiting on a EOL.  test io-54.2 deadlocks
			 * if line buffered channel doesn't become readable
			 */
			if (buffering==TclIO.BUFF_LINE && ! inputBuffer.lastReadWouldHaveBlocked() &&  inputBuffer.isRefillInProgress())
				return true;
		} catch (IOException e1) { }
		try {
			return (inputBuffer.available() > 0);
		} catch (IOException e) {
			/*
			 * Don't consider 'stream closed' a readable trigger. This exception
			 * is probably due to a pipeline whose process terminated early
			 */
			if (e.getMessage().toLowerCase().contains("closed"))
				return false;
			else
				return true;
		}
	}

	/**
	 * Force a non-blocking refill of the InputBuffer
	 * 
	 */
	void fillInputBuffer() throws IOException {
		if (isReadOnly() || isReadWrite()) {
			if (!closed) {
				initInput();
				if (inputBuffer != null) {
					inputBuffer.requestRefill(false);
				}
			}
		}
	}

	/**
	 * Returns true if the last read reached the end of file.
	 */
	public boolean eof() {
		return eofSeen;
	}

	/**
	 * Returns true of close() has been called
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * This method should be overridden in the subclass to provide a channel
	 * specific InputStream object.
	 */
	protected abstract InputStream getInputStream() throws IOException;

	/**
	 * This method should be overridden in the subclass to provide a channel
	 * specific OutputStream object.
	 */
	protected abstract OutputStream getOutputStream() throws IOException;

	/**
	 * Get the channel name
	 * 
	 * @return the channel name that is the key for the chanTable hash table
	 */
	public String getChanName() {
		return chanName;
	}

	/**
	 * @return a string that describes the channel type.
	 * 
	 *         This is the equivilent of the Tcl_ChannelType->typeName field.
	 */
	abstract String getChanType();

	/**
	 * @return number of references to this Channel.
	 */
	int getRefCount() {
		return refCount;
	}

	/**
	 * Sets the channel name of this channel
	 * 
	 * @param chan
	 *            the unique channel name for the chanTable hash table
	 */
	void setChanName(String chan) {
		chanName = chan;
	}

	/**
	 * @return true if this Channel is read-only
	 */
	public boolean isReadOnly() {
		return ((mode & TclIO.RDONLY) != 0);
	}

	/**
	 * @return true if this channel is write-only
	 */
	public boolean isWriteOnly() {
		return ((mode & TclIO.WRONLY) != 0);
	}

	/**
	 * @return true if this channel is read and write accessible
	 */
	public boolean isReadWrite() {
		return ((mode & TclIO.RDWR) != 0);
	}

	/**
	 * Tests if this Channel was opened for reading
	 * 
	 * @param interp
	 *            the current interpreter
	 * @throws TclException
	 *             if this channel was not opened for reading
	 */
	protected void checkRead(Interp interp) throws TclException {
		if (!isReadOnly() && !isReadWrite()) {
			throw new TclException(interp, "channel \"" + getChanName() + "\" wasn't opened for reading");
		}
	}

	/**
	 * Tests if this Channel was opened for writing
	 * 
	 * @param interp
	 *            the current interpreter
	 * @throws TclException
	 *             if this Channel was not opened for writing
	 */
	protected void checkWrite(Interp interp) throws TclException {
		if (!isWriteOnly() && !isReadWrite()) {
			throw new TclException(interp, "channel \"" + getChanName() + "\" wasn't opened for writing");
		}
	}

	/**
	 * @return false if Channel is in non-blocking mode , true otherwise
	 */
	public boolean getBlocking() {
		return blocking;
	}

	/**
	 * Set blocking mode.
	 * 
	 * @param inBlocking
	 *            True for blocking mode, false for non-blocking mode.
	 * 
	 */
	public void setBlocking(boolean inBlocking) {
		blocking = inBlocking;
		if (inputBuffer != null) {
			inputBuffer.setBlockingMode(blocking);
		}
		if (nonBlockingOutputStream != null) {
			nonBlockingOutputStream.setBlocking(blocking);
		}
	}

	/**
	 * @return buffering mode - TclIO.BUFF_FULL, TclIO.BUFF_LINE or
	 *         TclIO.BUFF_NONE.
	 */
	public int getBuffering() {
		return buffering;
	}

	/**
	 * Set buffering mode
	 * 
	 * @param inBuffering
	 *            One of TclIO.BUFF_FULL, TclIO.BUFF_LINE, or TclIO.BUFF_NONE
	 */
	public void setBuffering(int inBuffering) {
		if (inBuffering < TclIO.BUFF_FULL || inBuffering > TclIO.BUFF_NONE)
			throw new TclRuntimeError("invalid buffering mode in Channel.setBuffering()");

		buffering = inBuffering;
		if (inputBuffer != null) {
			inputBuffer.setBuffering(inBuffering);
		}
		if (outputBuffer != null) {
			outputBuffer.setBuffering(inBuffering);
		}
	}

	/**
	 * @return the current buffer size
	 */
	public int getBufferSize() {
		return bufferSize;
	}

	/**
	 * Tcl_SetChannelBufferSize -> setBufferSize
	 * 
	 * Sets the buffer size
	 * 
	 * @param size
	 *            new buffer size. Resize request is ignored if size < 1 or > 1
	 *            Mbyte
	 */
	public void setBufferSize(int size) {

		// If the buffer size is smaller than 1 byte or larger than 1 Meg
		// do not accept the requested size and leave the current buffer size.

		if ((size < 1) || (size > (1024 * 1024))) {
			return;
		}

		bufferSize = size;
		if (inputBuffer != null) {
			inputBuffer.setBufferSize(size);
		}
		if (outputBuffer != null) {
			outputBuffer.setBufferSize(size);
		}
	}

	/**
	 * @return Number of bytes stored in the chain of readers and input streams
	 *         that have already been read from the Channel's underlying input
	 *         stream
	 */
	int getNumBufferedInputBytes() {
		if (inputBuffer != null) {
			try {
				/*
				 * Calculate the number of bytes stored in the chain; don't
				 * include anything below rawInputStream
				 */
				return unicodeDecoder.available() - rawInputStream.available();
			} catch (IOException e) {
				return 0;
			}
		} else {
			return 0;
		}
	}

	/**
	 * @return Number of bytes stored in the chain of Writers and OutputStreams
	 *         that have not yet been written to the Channel's underlying output
	 *         strem
	 */
	int getNumBufferedOutputBytes() {
		if (outputBuffer != null)
			return outputBuffer.getBufferedByteCount();
		else
			return 0;
	}

	/**
	 * Tests if last read was incomplete due to a blocked channel
	 * 
	 * @param interp
	 *            current interpreter
	 * @return true if input if the last read on this channel was incomplete
	 *          because input was blocked
	 * @throws TclException
	 *             if this channel is not opened for reading
	 */
	public boolean isBlocked(Interp interp) throws TclException {
		checkRead(interp);
		if (inputBuffer != null)
			return inputBuffer.lastReadWouldHaveBlocked() && !eof();
		else
			return false;
	}

	/**
	 * Query this Channel's encoding
	 * 
	 * @return Name of Channel's Java encoding (null if no encoding)
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * Set new Java encoding.
	 * 
	 * @param inEncoding
	 *            Java-style encoding string
	 */
	public void setEncoding(String inEncoding) {
		encoding = inEncoding;

		if (unicodeDecoder != null) {
			unicodeDecoder.setEncoding(encoding);
			encodingChangedSinceLastRead = true;
		}
		if (unicodeEncoder != null) {
			unicodeEncoder.setEncoding(encoding);
		}
	}

	/**
	 * @return input translation (TclIO.TRANS_*)
	 */
	public int getInputTranslation() {
		return inputTranslation;
	}

	/**
	 * Set new input translation if this channel is not write-only
	 * 
	 * @param translation
	 *            one of the TclIO.TRANS_* constants
	 */
	public void setInputTranslation(int translation) {
		if (!(isReadOnly() || isReadWrite()))
			return;
		inputTranslation = translation;
		if (eolInputFilter != null) {
			eolInputFilter.setTranslation(translation);
		}
	}

	/**
	 * @return output translation - one of the TclIO.TRANS_* constants
	 */
	public int getOutputTranslation() {
		return outputTranslation;
	}

	/**
	 * Set new output translation if this channel is writeable
	 * 
	 * @param translation
	 *            one of the TclIO.TRANS_* constants
	 */
	public void setOutputTranslation(int translation) {
		if (!(isWriteOnly() || isReadWrite()))
			return;
		if (translation == TclIO.TRANS_AUTO)
			outputTranslation = TclIO.TRANS_PLATFORM;
		else
			outputTranslation = translation;
		if (eolOutputFilter != null)
			eolOutputFilter.setTranslation(outputTranslation);
	}

	/**
	 * @return input eof character, or 0 if none defined
	 */
	public char getInputEofChar() {
		return inputEofChar;
	}

	/**
	 * Set new input eof character, if channel is readable
	 * 
	 * @param inEof
	 *            new EOF character, or 0 if none
	 */
	public void setInputEofChar(char inEof) {
		if (!(isReadOnly() || isReadWrite()))
			return;

		// Store as a byte, not a unicode character
		inEof = (char) (inEof & 0xFF);

		if (inEof == inputEofChar)
			return;

		inputEofChar = (char) (inEof & 0xFF);
		if (eofInputFilter != null) {
			eofInputFilter.setEofChar((byte) inputEofChar);
		}
		if (inputBuffer != null)
			inputBuffer.cancelEof();
		if (unicodeDecoder != null)
			unicodeDecoder.cancelEof();
		eofSeen = false;
	}

	/**
	 * @return output eof character, or 0 if none
	 */
	public char getOutputEofChar() {
		return outputEofChar;
	}

	/**
	 * Set new output eof character if this channel is writeable
	 * 
	 * @param outEof
	 *            new EOF character, or 0 if none
	 */
	public void setOutputEofChar(char outEof) {
		if (!(isWriteOnly() || isReadWrite()))
			return;

		// Store as a byte, not a unicode character
		outputEofChar = (char) (outEof & 0xFF);
		if (eofOutputFilter != null) {
			eofOutputFilter.setEofChar((byte) outputEofChar);
		}
	}

}

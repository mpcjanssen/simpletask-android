package tcl.lang.channel;

import java.io.IOException;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclEvent;
import tcl.lang.TclException;
import tcl.lang.TclIO;

/**
 * This class implements an efficient copy between Channels
 * 
 * @author Dan Bodoh
 * 
 */
public class Fcopy {
	/**
	 * Source channel, from which to copy
	 */
	Channel source;
	/**
	 * Destination channel, to which data is being copied
	 */
	Channel destination;
	/**
	 * Number of bytes to transfer, or entire source file if < 0
	 */
	long size;
	/**
	 * Number of bytes written to the destination
	 */
	long bytesWritten = 0;
	/**
	 * Callback Tcl command; bytesWritten and optionally an error message are
	 * appended as a list to this string. A non-null callback implies background
	 * fcopy
	 */
	String callback;
	/**
	 * Original value of source.getBlocking(), so it can be restored on fcopy
	 * completion
	 */
	boolean sourceBlocking;
	/**
	 * Original value of destination.getBlockign(), so it can be restored on
	 * fcopy completion
	 */
	boolean destinationBlocking;
	/**
	 * Original value of source.getBuffering(), so it can be restored on fcopy
	 * completion
	 */
	int sourceBuffering;
	/**
	 * Original value of destination.getBuffering(), so it can be restored on
	 * fcopy completion
	 */
	int destinationBuffering;
	/**
	 * Set to true if we can read/write bytes during fcopy; false if we must
	 * read/write chars
	 */
	boolean transferBytes;
	/**
	 * Set to true if input is encoded, but output is not; fcopy spec indicates
	 * output should be utf8 encoded in that case
	 */
	boolean doUtf8OutputEncoding = false;
	/**
	 * Set to true if output is encoded but input is not; fcopy spec indicates
	 * output should be assumed utf8
	 */
	boolean doUtf8InputEncoding = false;
	/**
	 * buffer used for transferring characters
	 */
	char[] cbuf = null;
	/**
	 * Buffer used for transferring bytes
	 */
	byte[] bbuf = null;
	/**
	 * If a background fcopy is taking place, this is the thread responsible
	 */
	Thread backgroundCopy = null;
	/**
	 * The current interpreter, for error reporting
	 */
	Interp interp;

	/**
	 * Create a new Fcopy object. This constructor does not start the copy
	 * process.
	 * 
	 * @param interp
	 *            The current interpreter
	 * @param source
	 *            the source Channel
	 * @param destination
	 *            the destination Channel
	 * @param size
	 *            number of bytes to write to the destination; if < 0 writing
	 *            takes place to EOF on source
	 * @param callback
	 *            if null, return when copy is complete; if non-null, perform
	 *            the copy in the background and call script in TclObject when
	 *            copy is complete
	 * @throws TclException
	 */
	public Fcopy(Interp interp, Channel source, Channel destination, long size, String callback) throws TclException {

		source.checkRead(interp);
		destination.checkWrite(interp);

		this.source = source;
		this.destination = destination;
		this.size = size;
		this.callback = callback;
		this.interp = interp;
	}

	/**
	 * @return the number of bytes written to the destination
	 */
	public long getWrittenByteCount() {
		return bytesWritten;
	}

	/**
	 * Start the copy process. If a callback was provided in the constructor,
	 * start() returns immediately. Otherwise, it waits for the copy to
	 * cmoplete.
	 * 
	 * @return number of bytes written to the destination, if a callback was not
	 *         provided.
	 * @throws TclException
	 * @throws IOException
	 */
	public long start() throws TclException {
		if (callback == null) {
			/* wait for copy to complete */
			getChannelOwnership(Thread.currentThread().getId());
			try {
				setup();
				doCopy();
			} catch (IOException e) {
				throw new TclException(interp, e.getMessage());
			} finally {
				tearDown();
			}
			return bytesWritten;
		} else {
			Runnable r = new Runnable() {
				public void run() {
					String errormsg = null;
					try {
						setup();
						doCopy();
					} catch (Exception e) {
						errormsg = e.getMessage();
					} finally {
						tearDown();
					}

					if (source.isClosed() || destination.isClosed()) {
						return; // don't call the callback if one of the
								// channels was closed
					}

					/* Call the callback using the TclEvent queue */

					final String callbackWithArgs = callback + " " + bytesWritten
							+ (errormsg == null ? "" : (" {" + errormsg + "}"));

					TclEvent event = new TclEvent() {
						@Override
						public int processEvent(int flags) {
							try {
								interp.eval(callbackWithArgs);
							} catch (TclException e) {
								interp.addErrorInfo("\n    (\"fcopy\" script)");
								interp.backgroundError();
							}
							return 1;
						}
					};
					interp.getNotifier().queueEvent(event, TCL.QUEUE_TAIL);
				}
			};
			backgroundCopy = new Thread(r);
			getChannelOwnership(backgroundCopy.getId());
			backgroundCopy.setDaemon(true);
			backgroundCopy.setName("Fcopy (" + interp.toString() + "): " + source.getChanName() + " -> " + destination.getChanName());
			backgroundCopy.start();
			return 0;
		}
	}

	/**
	 * Get channel ownership of source and destination, or throw TclException
	 * 
	 * @param id
	 *            Thread.getId() of thread that will own the channel
	 * @throws TclException
	 *             if one of the channels is busy
	 */
	private void getChannelOwnership(long id) throws TclException {
		if (!source.setOwnership(true, Channel.READ_OWNERSHIP, id)) {
			throw new TclException(interp, "channel \"" + source.getChanName() + "\" is busy");
		}
		if (!destination.setOwnership(true, Channel.WRITE_OWNERSHIP, id)) {
			source.setOwnership(false, Channel.READ_OWNERSHIP);
			throw new TclException(interp, "channel \"" + destination.getChanName() + "\" is busy");
		}
	}

	/**
	 * Save state of Channels, change channel buffering and blocking for
	 * efficient data transfer, and create Fcopy's internal buffer
	 * 
	 * @throws IOException
	 */
	private void setup() throws TclException, IOException {

		source.initInput();
		destination.initOutput();

		sourceBlocking = source.getBlocking();
		destinationBlocking = destination.getBlocking();

		/*
		 * Turn blocking on; background copy is done with a thread. Blocking is
		 * turned on to prevent extra buffer copying for background read/write
		 */
		source.setBlocking(true);
		destination.setBlocking(true);

		sourceBuffering = source.getBuffering();
		destinationBuffering = destination.getBuffering();

		/*
		 * Turn buffering off, so we don't do extra copies into buffers; but
		 * don't turn off line buffering; otherwise, reading from a terminal may
		 * block forever. Buffering is provided by this class.
		 */
		if (sourceBuffering == TclIO.BUFF_FULL)
			source.setBuffering(TclIO.BUFF_NONE);
		if (destinationBuffering == TclIO.BUFF_FULL)
			destination.setBuffering(TclIO.BUFF_NONE);

		/* Can we transfer bytes, which is more efficient than chars? */
		String srcEncoding = source.getEncoding() == null ? "binary" : source.getEncoding();
		String dstEncoding = destination.getEncoding() == null ? "binary" : destination.getEncoding();

		transferBytes = srcEncoding.equals(dstEncoding)
				&& (source.getInputTranslation() == destination.getOutputTranslation());

		/*
		 * If the source is encoded, but the destination is not, the fcopy spec
		 * says that the output should be utf-8 encoded; and vice versa.
		 */
		if ((source.getEncoding() != null) && (destination.getEncoding() == null)) {
			doUtf8OutputEncoding = true;
			destination.setEncoding("utf-8");
		} else if ((source.getEncoding() == null) && (destination.getEncoding() != null)) {
			doUtf8InputEncoding = true;
			source.setEncoding("utf-8");
		}
		int bufsize = source.getBufferSize();
		if (bufsize == 0 || sourceBuffering == TclIO.BUFF_NONE) {
			/* Need to request at least one byte/char */
			bufsize = 1;
		}

		if (transferBytes) {
			bbuf = new byte[bufsize];
		} else {
			cbuf = new char[bufsize];
		}
	}

	/**
	 * Restore the source and destination channels to original state
	 */
	private void tearDown() {
		source.setBlocking(sourceBlocking);
		destination.setBlocking(destinationBlocking);
		source.setBuffering(sourceBuffering);
		destination.setBuffering(destinationBuffering);
		source.setOwnership(false, Channel.READ_OWNERSHIP);
		destination.setOwnership(false, Channel.WRITE_OWNERSHIP);
		if (doUtf8OutputEncoding) {
			destination.setEncoding(null);
		}
		if (doUtf8InputEncoding) {
			source.setEncoding(null);
		}
	}

	/**
	 * Actually copy data from the source channel to the destination channel,
	 * until EOF or size bytes have been written to the destination.
	 * 
	 * @return number of bytes written to the destination
	 * @throws IOException
	 */
	private long doCopy() throws IOException {
		int cnt;
		long startCount = destination.outputBuffer.getReceivedByteCount();
		while (!source.eof()) {
			int transferSize = transferBytes ? bbuf.length : cbuf.length;
			if (size >= 0) {
				long remaining = size - bytesWritten;

				if (remaining <= 0)
					break;

				/*
				 * Throttle down near end of transfer if transferring
				 * characters, so we don't overshoot byte count
				 */
				if (!transferBytes && remaining <= 16) {
					remaining = 1;
				}
				transferSize = remaining < transferSize ? (int) remaining : transferSize;
			}
			if (source.isClosed())
				break;

			if (transferBytes) {
				cnt = source.finalInputStream.read(bbuf, 0, transferSize);
			} else {
				cnt = source.finalReader.read(cbuf, 0, transferSize);
			}

			if (cnt == -1) {
				source.eofSeen = true;
				break;
			}

			if (destination.isClosed())
				break;
			if (transferBytes) {
				destination.firstOutputStream.write(bbuf, 0, cnt);
			} else {
				destination.firstWriter.write(cbuf, 0, cnt);
			}

			bytesWritten = destination.outputBuffer.getReceivedByteCount() - startCount;
		}
		if (!destination.isClosed())
			destination.firstWriter.flush();
		return bytesWritten;
	}

}

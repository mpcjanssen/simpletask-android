package tcl.lang.channel;

import java.io.IOException;

import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclPosixException;
import tcl.lang.TclRuntimeError;

/**
 * Subclass of Channel that supports seeking
 * 
 * @author Dan Bodoh
 * 
 */
public abstract class SeekableChannel extends Channel {

	/**
	 * Seek to a specified absolute place in the file
	 * 
	 * @param offset
	 *            absolute offset into the file
	 * @throws IOException
	 */
	abstract void implSeek(long offset) throws IOException;

	/**
	 * @return the current byte offset in the file, relative to the beginning of
	 *         the file.
	 */
	abstract long implTell() throws IOException;

	/**
	 * @return maximum seek position, which is probably the end of the file
	 */
	abstract long getMaxSeek() throws IOException;

	/**
	 * Move the current file pointer.
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
	@Override
	public void seek(Interp interp, long offset, int mode) throws IOException, TclException {

		if (!setOwnership(true, READ_OWNERSHIP)) {
			throw new TclException(interp, "channel is busy");
		}
		if (!setOwnership(true, WRITE_OWNERSHIP)) {
			setOwnership(false, READ_OWNERSHIP);
			throw new TclException(interp, "channel is busy");
		}

		try {
			// Compute how much input and output is buffered. If both input and
			// output is buffered, cannot compute the current position.
			int inputBuffered = getNumBufferedInputBytes();
			int outputBuffered = getNumBufferedOutputBytes();

			if ((inputBuffered != 0) && (outputBuffered != 0)) {
				throw new TclPosixException(interp, TclPosixException.EFAULT, true, "error during seek on \""
						+ getChanName() + "\"");
			}

			// If we are seeking relative to the current position, compute the
			// corrected offset taking into account the amount of unread input.

			if (mode == TclIO.SEEK_CUR) {
				offset -= inputBuffered;
			}

			// The seekReset method will discard queued input and
			// reset flags like EOF and BLOCKED.
			seekReset();

			// If the channel is in asynchronous output mode, switch it back
			// to synchronous mode
			// scheduled. After the flush, the channel will be put back into
			// asynchronous output mode.

			boolean wasAsync = false;
			if (!getBlocking()) {
				wasAsync = true;
				setBlocking(true);
			}

			if (firstWriter != null)
				flush(interp);

			// Now seek to the new position in the channel as requested by the
			// caller.

			long actual_offset;

			switch (mode) {
			case TclIO.SEEK_SET: {
				actual_offset = offset;
				break;
			}
			case TclIO.SEEK_CUR: {
				actual_offset = implTell() + offset;
				break;
			}
			case TclIO.SEEK_END: {
				actual_offset = getMaxSeek() + offset;
				break;
			}
			default: {
				throw new TclRuntimeError("invalid seek mode");
			}
			}

			// A negative offset to seek() would raise an IOException, but
			// we want to raise an invalid argument error instead

			if (actual_offset < 0) {
				throw new TclPosixException(interp, TclPosixException.EINVAL, true, "error during seek on \""
						+ getChanName() + "\"");
			}

			implSeek(actual_offset);

			// Restore to nonblocking mode if that was the previous behavior.
			if (wasAsync) {
				setBlocking(false);
			}
		} finally {
			setOwnership(false, WRITE_OWNERSHIP);
			setOwnership(false, READ_OWNERSHIP);
		}
	}

	/**
	 * @return the current file position.
	 */
	@Override
	public long tell() throws IOException {
		long filepos = implTell();
		int inputBuffered = getNumBufferedInputBytes();
		int outputBuffered = getNumBufferedOutputBytes();

		if ((inputBuffered != 0) && (outputBuffered != 0)) {
			return -1;
		}
		if (filepos == -1) {
			return -1;
		}
		if (inputBuffered != 0) {
			return filepos - inputBuffered;
		}
		return filepos + outputBuffered;
	}

	/**
	 * Reset all internal state that is out-of-date after a seek()
	 */
	void seekReset() {
		if (inputBuffer != null)
			inputBuffer.seekReset();
		if (eolInputFilter != null)
			eolInputFilter.seekReset();
		if (eofInputFilter != null)
			eofInputFilter.seekReset();
		if (unicodeDecoder != null)
			unicodeDecoder.seekReset();
		if (markableInputStream != null) {
			markableInputStream.seekReset();
		}
		eofSeen = false;
	}

	/**
	 * Called just prior to each write to the getOutputStream() stream, if the
	 * channel is in TclIO.APPEND mode, to guarantee that write will append to
	 * the end of the output. There is no need to flush any of the channel
	 * buffers.
	 * 
	 * @throws IOException
	 */
	void prepareForAppendWrite() throws IOException {
		implSeek(getMaxSeek());
	}

	/**
	 * Set channel's eofSeen by testing current file position
	 * 
	 * @see tcl.lang.channel.Channel#setEofSeenWithoutRead()
	 */
	@Override
	void setEofSeenWithoutRead() {
		try {
			if (getNumBufferedInputBytes() == 0 && implTell() >= getMaxSeek())
				eofSeen = true;
		} catch (IOException e) {
			
		}
	}
	
}

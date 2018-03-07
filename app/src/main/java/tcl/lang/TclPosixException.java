/*
 * TclPosixException.java --
 *
 *	This file implements the TclPosixException class, used to report posix
 *	errors in Tcl scripts.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclPosixException.java,v 1.2 2001/11/22 00:08:36 mdejong Exp $
 *
 */

package tcl.lang;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/*
 * This class implements exceptions used to report posix errors in Tcl scripts.
 */

public class TclPosixException extends TclException {
	int errno = 0;
	public static final int EPERM = 1; /* Operation not permitted */
	public static final int ENOENT = 2; /* No such file or directory */
	public static final int ESRCH = 3; /* No such process */
	public static final int EINTR = 4; /* Interrupted system call */
	public static final int EIO = 5; /* Input/output error */
	public static final int ENXIO = 6; /* Device not configured */
	public static final int E2BIG = 7; /* Argument list too long */
	public static final int ENOEXEC = 8; /* Exec format error */
	public static final int EBADF = 9; /* Bad file descriptor */
	public static final int ECHILD = 10; /* No child processes */
	public static final int EDEADLK = 11; /* Resource deadlock avoided */
	/* 11 was EAGAIN */
	public static final int ENOMEM = 12; /* Cannot allocate memory */
	public static final int EACCES = 13; /* Permission denied */
	public static final int EFAULT = 14; /* Bad address */
	public static final int ENOTBLK = 15; /* Block device required */
	public static final int EBUSY = 16; /* Device busy */
	public static final int EEXIST = 17; /* File exists */
	public static final int EXDEV = 18; /* Cross-device link */
	public static final int ENODEV = 19; /* Operation not supported by device */
	public static final int ENOTDIR = 20; /* Not a directory */
	public static final int EISDIR = 21; /* Is a directory */
	public static final int EINVAL = 22; /* Invalid argument */
	public static final int ENFILE = 23; /* Too many open files in system */
	public static final int EMFILE = 24; /* Too many open files */
	public static final int ENOTTY = 25; /* Inappropriate ioctl for device */
	public static final int ETXTBSY = 26; /* Text file busy */
	public static final int EFBIG = 27; /* File too large */
	public static final int ENOSPC = 28; /* No space left on device */
	public static final int ESPIPE = 29; /* Illegal seek */
	public static final int EROFS = 30; /* Read-only file system */
	public static final int EMLINK = 31; /* Too many links */
	public static final int EPIPE = 32; /* Broken pipe */
	public static final int EDOM = 33; /* Numerical argument out of domain */
	public static final int ERANGE = 34; /* Result too large */
	public static final int EAGAIN = 35; /* Resource temporarily unavailable */
	public static final int EWOULDBLOCK = EAGAIN; /* Operation would block */
	public static final int EINPROGRESS = 36; /* Operation now in progress */
	public static final int EALREADY = 37; /* Operation already in progress */
	public static final int ENOTSOCK = 38; /* Socket operation on non-socket */
	public static final int EDESTADDRREQ = 39; /* Destination address required */
	public static final int EMSGSIZE = 40; /* Message too long */
	public static final int EPROTOTYPE = 41; /* Protocol wrong type for socket */
	public static final int ENOPROTOOPT = 42; /* Protocol not available */
	public static final int EPROTONOSUPPORT = 43; /* Protocol not supported */
	public static final int ESOCKTNOSUPPORT = 44; /* Socket type not supported */
	public static final int EOPNOTSUPP = 45; /* Operation not supported on socket */
	public static final int EPFNOSUPPORT = 46; /* Protocol family not supported */
	public static final int EAFNOSUPPORT = 47; /*
												 * Address family not supported
												 * by /* protocol family
												 */
	public static final int EADDRINUSE = 48; /* Address already in use */
	public static final int EADDRNOTAVAIL = 49; /*
												 * Can't assign requested /*
												 * address
												 */
	public static final int ENETDOWN = 50; /* Network is down */
	public static final int ENETUNREACH = 51; /* Network is unreachable */
	public static final int ENETRESET = 52; /*
											 * Network dropped connection on
											 * reset
											 */
	public static final int ECONNABORTED = 53; /*
												 * Software caused connection
												 * abort
												 */
	public static final int ECONNRESET = 54; /* Connection reset by peer */
	public static final int ENOBUFS = 55; /* No buffer space available */
	public static final int EISCONN = 56; /* Socket is already connected */
	public static final int ENOTCONN = 57; /* Socket is not connected */
	public static final int ESHUTDOWN = 58; /* Can't send after socket shutdown */
	public static final int ETOOMANYREFS = 59; /*
												 * Too many references: can't
												 * splice
												 */
	public static final int ETIMEDOUT = 60; /* Connection timed out */
	public static final int ECONNREFUSED = 61; /* Connection refused */
	public static final int ELOOP = 62; /* Too many levels of symbolic links */
	public static final int ENAMETOOLONG = 63; /* File name too long */
	public static final int EHOSTDOWN = 64; /* Host is down */
	public static final int EHOSTUNREACH = 65; /* No route to host */
	public static final int ENOTEMPTY = 66; /* Directory not empty */
	public static final int EPROCLIM = 67; /* Too many processes */
	public static final int EUSERS = 68; /* Too many users */
	public static final int EDQUOT = 69; /* Disc quota exceeded */
	public static final int ESTALE = 70; /* Stale NFS file handle */
	public static final int EREMOTE = 71; /* Too many levels of remote in path */
	public static final int EBADRPC = 72; /* RPC struct is bad */
	public static final int ERPCMISMATCH = 73; /* RPC version wrong */
	public static final int EPROGUNAVAIL = 74; /* RPC prog. not avail */
	public static final int EPROGMISMATCH = 75; /* Program version wrong */
	public static final int EPROCUNAVAIL = 76; /* Bad procedure for program */
	public static final int ENOLCK = 77; /* No locks available */
	public static final int ENOSYS = 78; /* Function not implemented */
	public static final int EFTYPE = 79; /* Inappropriate file type or format */

	
	/**
	 * Creates a TclException with the appropiate Tcl error message for posix
	 * error, and sets the interp's error code to a 3 element list (eg. {"posix"
	 * "eexist" {file already exists}}).  Does not append posix message to intepreter result.
	 * 
	 * @param interp Current interpretor, or null
	 * @param errno code of posix error
	 * @param errorMsg message to output to user
	 * @throws TclException
	 */
	public TclPosixException(Interp interp, // Interpreter for error report.
			int errno, // Code of posix error.
			String errorMsg) // Message to ouput to user.
			throws TclException {
		super(TCL.ERROR);

		init(interp, errno, false, errorMsg);
	}

	/**
	 * Creates a TclException with the appropiate Tcl error message for posix
	 * error, and sets the interp's error code to a 3 element list (eg. {"posix"
	 * "eexist" {file already exists}}).
	 * 
	 * @param interp Current interpretor, or null
	 * @param errno code of posix error
	 * @param appendPosixMsg if true, appends posix message to errorMsg in interpreter result
	 * @param errorMsg message to output to user
	 * @throws TclException
	 */
	public TclPosixException(Interp interp, // Interpreter for error report.
			int errno, // Code of posix error.
			boolean appendPosixMsg, // Tells whether to append posix
			// message to "errorMsg".
			String errorMsg) // Message to ouput to user.
			throws TclException {
		super(TCL.ERROR);

		init(interp, errno, appendPosixMsg, errorMsg);
	}
	
	/**
	 * Construct a TclPosixException from a Java IOException
	 * 
	 * @param interp current interpreter
	 * @param ioException The IOException that was thrown
	 * @param appendPosixMsg true if the Posix message should be appended to the errorMessage in the interpreter result
	 * @param errorMessage error message to return in interpreter result
	 * @throws TclException
	 */
	public TclPosixException(Interp interp, IOException ioException, boolean appendPosixMsg, String errorMessage)
			throws TclException {
		super(TCL.ERROR);
		String iomsg = ioException.getMessage();
		int errorAt = iomsg.indexOf("error=");
		int errno;
		if (errorAt == -1) {
			iomsg = iomsg.toLowerCase();
			if (iomsg.contains("no such file or directory")) {
				errno = ENOENT;
			} else if (iomsg.contains("broken pipe")) {
				errno = EPIPE;
			} else throw new TclException(interp,iomsg);

		} else {
			int start = errorAt+6;
			int end = start;
			while (end < iomsg.length() && Character.isDigit(iomsg.charAt(end))) end++;
			String errnoString = iomsg.substring(start,end);
			errno = Integer.parseInt(errnoString);
		}
		init(interp, errno, appendPosixMsg, errorMessage);
	}

	/**
	 * @return the error number for this TclPosixException
	 */
	public int getErrorNo() {
		return this.errno;
	}
	
	private void init(Interp interp, int errno, boolean appendPosixMsg, String errorMsg) throws TclException {
		String msg = getPosixMsg(errno);
		this.errno = errno;
		
		TclObject threeEltListObj = TclList.newInstance();
		TclList.append(interp, threeEltListObj, TclString.newInstance("POSIX"));
		TclList.append(interp, threeEltListObj, TclString
				.newInstance(getPosixId(errno)));
		TclList.append(interp, threeEltListObj, TclString.newInstance(msg));

		interp.setErrorCode(threeEltListObj);

		if (interp != null) {
			if (appendPosixMsg) {
				interp.setResult(errorMsg + ": " + msg);
			} else {
				interp.setResult(errorMsg);
			}
		}
	}

	/*
	 * ----------------------------------------------------------------------
	 * 
	 * getPosixId --
	 * 
	 * Return the string form of "errno".
	 * 
	 * Returns: An error name String.
	 * 
	 * Side effects: None.
	 * 
	 * ----------------------------------------------------------------------
	 */

	private static String getPosixId(int errno) // Code of posix error.
	{
		switch (errno) {
		case E2BIG:
			return "E2BIG";
		case EACCES:
			return "EACCES";
		case EADDRINUSE:
			return "EADDRINUSE";
		case EADDRNOTAVAIL:
			return "EADDRNOTAVAIL";
			// case EADV: return "EADV";
		case EAFNOSUPPORT:
			return "EAFNOSUPPORT";
		case EAGAIN:
			return "EAGAIN";
			// case EALIGN: return "EALIGN";
		case EALREADY:
			return "EALREADY";
			// case EBADE: return "EBADE";
		case EBADF:
			return "EBADF";
			// case EBADFD: return "EBADFD";
			// case EBADMSG: return "EBADMSG";
			// case EBADR: return "EBADR";
		case EBADRPC:
			return "EBADRPC";
			// case EBADRQC: return "EBADRQC";
			// case EBADSLT: return "EBADSLT";
			// case EBFONT: return "EBFONT";
		case EBUSY:
			return "EBUSY";
		case ECHILD:
			return "ECHILD";
			// case ECHRNG: return "ECHRNG";
			// case ECOMM: return "ECOMM";
		case ECONNABORTED:
			return "ECONNABORTED";
		case ECONNREFUSED:
			return "ECONNREFUSED";
		case ECONNRESET:
			return "ECONNRESET";
		case EDEADLK:
			return "EDEADLK";
			// case EDEADLOCK: return "EDEADLOCK";
		case EDESTADDRREQ:
			return "EDESTADDRREQ";
			// case EDIRTY: return "EDIRTY";
		case EDOM:
			return "EDOM";
			// case EDOTDOT: return "EDOTDOT";
		case EDQUOT:
			return "EDQUOT";
			// case EDUPPKG: return "EDUPPKG";
		case EEXIST:
			return "EEXIST";
		case EFAULT:
			return "EFAULT";
		case EFBIG:
			return "EFBIG";
		case EHOSTDOWN:
			return "EHOSTDOWN";
		case EHOSTUNREACH:
			return "EHOSTUNREACH";
			// case EIDRM: return "EIDRM";
			// case EINIT: return "EINIT";
		case EINPROGRESS:
			return "EINPROGRESS";
		case EINTR:
			return "EINTR";
		case EINVAL:
			return "EINVAL";
		case EIO:
			return "EIO";
		case EISCONN:
			return "EISCONN";
		case EISDIR:
			return "EISDIR";
			// case EISNAM: return "EISNAM";
			// case ELBIN: return "ELBIN";
			// case EL2HLT: return "EL2HLT";
			// case EL2NSYNC: return "EL2NSYNC";
			// case EL3HLT: return "EL3HLT";
			// case EL3RST: return "EL3RST";
			// case ELIBACC: return "ELIBACC";
			// case ELIBBAD: return "ELIBBAD";
			// case ELIBEXEC: return "ELIBEXEC";
			// case ELIBMAX: return "ELIBMAX";
			// case ELIBSCN: return "ELIBSCN";
			// case ELNRNG: return "ELNRNG";
		case ELOOP:
			return "ELOOP";
		case EMLINK:
			return "EMLINK";
		case EMSGSIZE:
			return "EMSGSIZE";
			// case EMULTIHOP: return "EMULTIHOP";
		case ENAMETOOLONG:
			return "ENAMETOOLONG";
			// case ENAVAIL: return "ENAVAIL";
			// case ENET: return "ENET";
		case ENETDOWN:
			return "ENETDOWN";
		case ENETRESET:
			return "ENETRESET";
		case ENETUNREACH:
			return "ENETUNREACH";
		case ENFILE:
			return "ENFILE";
			// case ENOANO: return "ENOANO";
		case ENOBUFS:
			return "ENOBUFS";
			// case ENOCSI: return "ENOCSI";
			// case ENODATA: return "ENODATA";
		case ENODEV:
			return "ENODEV";
		case ENOENT:
			return "ENOENT";
		case ENOEXEC:
			return "ENOEXEC";
		case ENOLCK:
			return "ENOLCK";
			// case ENOLINK: return "ENOLINK";
		case ENOMEM:
			return "ENOMEM";
			// case ENOMSG: return "ENOMSG";
			// case ENONET: return "ENONET";
			// case ENOPKG: return "ENOPKG";
		case ENOPROTOOPT:
			return "ENOPROTOOPT";
		case ENOSPC:
			return "ENOSPC";
			// case ENOSR: return "ENOSR";
			// case ENOSTR: return "ENOSTR";
			// case ENOSYM: return "ENOSYM";
		case ENOSYS:
			return "ENOSYS";
		case ENOTBLK:
			return "ENOTBLK";
		case ENOTCONN:
			return "ENOTCONN";
		case ENOTDIR:
			return "ENOTDIR";
		case ENOTEMPTY:
			return "ENOTEMPTY";
			// case ENOTNAM: return "ENOTNAM";
		case ENOTSOCK:
			return "ENOTSOCK";
			// case ENOTSUP: return "ENOTSUP";
		case ENOTTY:
			return "ENOTTY";
			// case ENOTUNIQ: return "ENOTUNIQ";
		case ENXIO:
			return "ENXIO";
		case EOPNOTSUPP:
			return "EOPNOTSUPP";
		case EPERM:
			return "EPERM";
		case EPFNOSUPPORT:
			return "EPFNOSUPPORT";
		case EPIPE:
			return "EPIPE";
		case EPROCLIM:
			return "EPROCLIM";
		case EPROCUNAVAIL:
			return "EPROCUNAVAIL";
		case EPROGMISMATCH:
			return "EPROGMISMATCH";
		case EPROGUNAVAIL:
			return "EPROGUNAVAIL";
			// case EPROTO: return "EPROTO";
		case EPROTONOSUPPORT:
			return "EPROTONOSUPPORT";
		case EPROTOTYPE:
			return "EPROTOTYPE";
		case ERANGE:
			return "ERANGE";
			// case EREFUSED: return "EREFUSED";
			// case EREMCHG: return "EREMCHG";
			// case EREMDEV: return "EREMDEV";
		case EREMOTE:
			return "EREMOTE";
			// case EREMOTEIO: return "EREMOTEIO";
			// case EREMOTERELEASE: return "EREMOTERELEASE";
		case EROFS:
			return "EROFS";
		case ERPCMISMATCH:
			return "ERPCMISMATCH";
			// case ERREMOTE: return "ERREMOTE";
		case ESHUTDOWN:
			return "ESHUTDOWN";
		case ESOCKTNOSUPPORT:
			return "ESOCKTNOSUPPORT";
		case ESPIPE:
			return "ESPIPE";
		case ESRCH:
			return "ESRCH";
			// case ESRMNT: return "ESRMNT";
		case ESTALE:
			return "ESTALE";
			// case ESUCCESS: return "ESUCCESS";
			// case ETIME: return "ETIME";
		case ETIMEDOUT:
			return "ETIMEDOUT";
		case ETOOMANYREFS:
			return "ETOOMANYREFS";
		case ETXTBSY:
			return "ETXTBSY";
			// case EUCLEAN: return "EUCLEAN";
			// case EUNATCH: return "EUNATCH";
		case EUSERS:
			return "EUSERS";
			// case EVERSION: return "EVERSION";
			// case EWOULDBLOCK: return "EWOULDBLOCK";
		case EXDEV:
			return "EXDEV";
			// case EXFULL: return "EXFULL";
		}
		return "unknown error";
	}

	/**
	 * Return the message string corresponding to "errno".
	 * 
	 * @param errno A POSIX error number
	 * @return An error message String.
	 */
	static String getPosixMsg(int errno) // Code of posix error.
	{
		switch (errno) {
		case E2BIG:
			return "argument list too long";
		case EACCES:
			return "permission denied";
		case EADDRINUSE:
			return "address already in use";
		case EADDRNOTAVAIL:
			return "can't assign requested address";
			// case EADV: return "advertise error";
		case EAFNOSUPPORT:
			return "address family not supported by protocol family";
		case EAGAIN:
			return "resource temporarily unavailable";
			// case EALIGN: return "EALIGN";
		case EALREADY:
			return "operation already in progress";
			// case EBADE: return "bad exchange descriptor";
		case EBADF:
			return "bad file number";
			// case EBADFD: return "file descriptor in bad state";
			// case EBADMSG: return "not a data message";
			// case EBADR: return "bad request descriptor";
		case EBADRPC:
			return "RPC structure is bad";
			// case EBADRQC: return "bad request code";
			// case EBADSLT: return "invalid slot";
			// case EBFONT: return "bad font file format";
		case EBUSY:
			return "file busy";
		case ECHILD:
			return "no children";
			// case ECHRNG: return "channel number out of range";
			// case ECOMM: return "communication error on send";
		case ECONNABORTED:
			return "software caused connection abort";
		case ECONNREFUSED:
			return "connection refused";
		case ECONNRESET:
			return "connection reset by peer";
		case EDEADLK:
			return "resource deadlock avoided";
			// case EDEADLOCK: return "resource deadlock avoided";
		case EDESTADDRREQ:
			return "destination address required";
			// case EDIRTY: return "mounting a dirty fs w/o force";
		case EDOM:
			return "math argument out of range";
			// case EDOTDOT: return "cross mount point";
		case EDQUOT:
			return "disk quota exceeded";
			// case EDUPPKG: return "duplicate package name";
		case EEXIST:
			return "file already exists";
		case EFAULT:
			return "bad address in system call argument";
		case EFBIG:
			return "file too large";
		case EHOSTDOWN:
			return "host is down";
		case EHOSTUNREACH:
			return "host is unreachable";
			// case EIDRM: return "identifier removed";
			// case EINIT: return "initialization error";
		case EINPROGRESS:
			return "operation now in progress";
		case EINTR:
			return "interrupted system call";
		case EINVAL:
			return "invalid argument";
		case EIO:
			return "I/O error";
		case EISCONN:
			return "socket is already connected";
		case EISDIR:
			return "illegal operation on a directory";
			// case EISNAM: return "is a name file";
			// case ELBIN: return "ELBIN";
			// case EL2HLT: return "level 2 halted";
			// case EL2NSYNC: return "level 2 not synchronized";
			// case EL3HLT: return "level 3 halted";
			// case EL3RST: return "level 3 reset";
			// case ELIBACC: return "can not access a needed shared library";
			// case ELIBBAD: return "accessing a corrupted shared library";
			// case ELIBEXEC: return "can not exec a shared library directly";
			// case ELIBMAX: return
			// "attempting to link in more shared libraries than system limit";
			// case ELIBSCN: return ".lib section in a.out corrupted";
			// case ELNRNG: return "link number out of range";
		case ELOOP:
			return "too many levels of symbolic links";
		case EMFILE:
			return "too many open files";
		case EMLINK:
			return "too many links";
		case EMSGSIZE:
			return "message too long";
			// case EMULTIHOP: return "multihop attempted";
		case ENAMETOOLONG:
			return "file name too long";
			// case ENAVAIL: return "not available";
			// case ENET: return "ENET";
		case ENETDOWN:
			return "network is down";
		case ENETRESET:
			return "network dropped connection on reset";
		case ENETUNREACH:
			return "network is unreachable";
		case ENFILE:
			return "file table overflow";
			// case ENOANO: return "anode table overflow";
		case ENOBUFS:
			return "no buffer space available";
			// case ENOCSI: return "no CSI structure available";
			// case ENODATA: return "no data available";
		case ENODEV:
			return "no such device";
		case ENOENT:
			return "no such file or directory";
		case ENOEXEC:
			return "exec format error";
		case ENOLCK:
			return "no locks available";
			// case ENOLINK: return "link has be severed";
		case ENOMEM:
			return "not enough memory";
			// case ENOMSG: return "no message of desired type";
			// case ENONET: return "machine is not on the network";
			// case ENOPKG: return "package not installed";
		case ENOPROTOOPT:
			return "bad proocol option";
		case ENOSPC:
			return "no space left on device";
			// case ENOSR: return "out of stream resources";
			// case ENOSTR: return "not a stream device";
			// case ENOSYM: return "unresolved symbol name";
		case ENOSYS:
			return "function not implemented";
		case ENOTBLK:
			return "block device required";
		case ENOTCONN:
			return "socket is not connected";
		case ENOTDIR:
			return "not a directory";
		case ENOTEMPTY:
			return "directory not empty";
			// case ENOTNAM: return "not a name file";
		case ENOTSOCK:
			return "socket operation on non-socket";
			// case ENOTSUP: return "operation not supported";
		case ENOTTY:
			return "inappropriate device for ioctl";
			// case ENOTUNIQ: return "name not unique on network";
		case ENXIO:
			return "no such device or address";
		case EOPNOTSUPP:
			return "operation not supported on socket";
		case EPERM:
			return "not owner";
		case EPFNOSUPPORT:
			return "protocol family not supported";
		case EPIPE:
			return "broken pipe";
		case EPROCLIM:
			return "too many processes";
		case EPROCUNAVAIL:
			return "bad procedure for program";
		case EPROGMISMATCH:
			return "program version wrong";
		case EPROGUNAVAIL:
			return "RPC program not available";
			// case EPROTO: return "protocol error";
		case EPROTONOSUPPORT:
			return "protocol not suppored";
		case EPROTOTYPE:
			return "protocol wrong type for socket";
		case ERANGE:
			return "math result unrepresentable";
			// case EREFUSED: return "EREFUSED";
			// case EREMCHG: return "remote address changed";
			// case EREMDEV: return "remote device";
		case EREMOTE:
			return "pathname hit remote file system";
			// case EREMOTEIO: return "remote i/o error";
			// case EREMOTERELEASE: return "EREMOTERELEASE";
		case EROFS:
			return "read-only file system";
		case ERPCMISMATCH:
			return "RPC version is wrong";
			// case ERREMOTE: return "object is remote";
		case ESHUTDOWN:
			return "can't send afer socket shutdown";
		case ESOCKTNOSUPPORT:
			return "socket type not supported";
		case ESPIPE:
			return "invalid seek";
		case ESRCH:
			return "no such process";
			// case ESRMNT: return "srmount error";
		case ESTALE:
			return "stale remote file handle";
			// case ESUCCESS: return "Error 0";
			// case ETIME: return "timer expired";
		case ETIMEDOUT:
			return "connection timed out";
		case ETOOMANYREFS:
			return "too many references: can't splice";
		case ETXTBSY:
			return "text file or pseudo-device busy";
			// case EUCLEAN: return "structure needs cleaning";
			// case EUNATCH: return "protocol driver not attached";
		case EUSERS:
			return "too many users";
			// case EVERSION: return "version mismatch";
			// case EWOULDBLOCK: return "operation would block";
		case EXDEV:
			return "cross-domain link";
			// case EXFULL: return "message tables full";
		default:
			return "unknown POSIX error";
		}
	}

} // end TclPosixException class


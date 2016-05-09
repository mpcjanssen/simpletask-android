/*
 * FileCmd.java --
 *
 *	This file contains the Jacl implementation of the built-in Tcl "file"
 *	command.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: FileCmd.java,v 1.14 2009/07/20 08:52:29 rszulgo Exp $
 *
 */

package tcl.lang.cmd;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import tcl.lang.Command;
import tcl.lang.FileUtil;
import tcl.lang.Interp;
import tcl.lang.JACL;
import tcl.lang.TclBoolean;
import tcl.lang.TclException;
import tcl.lang.TclIO;
import tcl.lang.TclIndex;
import tcl.lang.TclInteger;
import tcl.lang.TclList;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;
import tcl.lang.TclPosixException;
import tcl.lang.TclRuntimeError;
import tcl.lang.TclString;
import tcl.lang.Util;

/**
 * This class implements the built-in "file" command in Tcl.
 */

public class FileCmd implements Command {

	static final private String validCmds[] = { "atime", "attributes", "channels", "copy", "delete", "dirname",
			"executable", "exists", "extension", "isdirectory", "isfile", "join", "link", "lstat", "mtime", "mkdir",
			"nativename", "normalize", "owned", "pathtype", "readable", "readlink", "rename", "rootname", "separator",
			"size", "split", "stat", "system", "tail", "type", "volumes", "writable" };

	private static final int OPT_ATIME = 0;
	private static final int OPT_ATTRIBUTES = 1;
	private static final int OPT_CHANNELS = 2;
	private static final int OPT_COPY = 3;
	private static final int OPT_DELETE = 4;
	private static final int OPT_DIRNAME = 5;
	private static final int OPT_EXECUTABLE = 6;
	private static final int OPT_EXISTS = 7;
	private static final int OPT_EXTENSION = 8;
	private static final int OPT_ISDIRECTORY = 9;
	private static final int OPT_ISFILE = 10;
	private static final int OPT_JOIN = 11;
	private static final int OPT_LINK = 12;
	private static final int OPT_LSTAT = 13;
	private static final int OPT_MTIME = 14;
	private static final int OPT_MKDIR = 15;
	private static final int OPT_NATIVENAME = 16;
	private static final int OPT_NORMALIZE = 17;
	private static final int OPT_OWNED = 18;
	private static final int OPT_PATHTYPE = 19;
	private static final int OPT_READABLE = 20;
	private static final int OPT_READLINK = 21;
	private static final int OPT_RENAME = 22;
	private static final int OPT_ROOTNAME = 23;
	private static final int OPT_SEPARATOR = 24;
	private static final int OPT_SIZE = 25;
	private static final int OPT_SPLIT = 26;
	private static final int OPT_STAT = 27;
	private static final int OPT_SYSTEM = 28;
	private static final int OPT_TAIL = 29;
	private static final int OPT_TYPE = 30;
	private static final int OPT_VOLUMES = 31;
	private static final int OPT_WRITABLE = 32;

	private static final String validOptions[] = { "-force", "--" };

	private static final int OPT_FORCE = 0;
	private static final int OPT_LAST = 1;

	/**
	 * This procedure is invoked to process the "file" Tcl command. See the user
	 * documentation for details on what it does.
	 * 
	 * @see tcl.lang.Command#cmdProc(tcl.lang.Interp, tcl.lang.TclObject[])
	 */
	public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
		if (argv.length < 2) {
			throw new TclNumArgsException(interp, 1, argv, "option ?arg ...?");
		}

		int opt = TclIndex.get(interp, argv[1], validCmds, "option", 0);
		String path;
		File fileObj = null;

		switch (opt) {

		case OPT_ATIME:
			// FIXME: Java does not support retrieval of access time.
			if (argv.length < 3 || argv.length > 4) {
				throw new TclNumArgsException(interp, 2, argv, "name ?time?");
			}
			if (!FileUtil.getNewFileObj(interp, argv[2].toString()).exists() || argv[2].toString().length() == 0) {
				throw new TclPosixException(interp, TclPosixException.ENOENT, true, "could not read \"" + argv[2]
						+ "\"");
			}
			if (argv.length == 4) {
				try {
					int time = Integer.parseInt(argv[3].toString());
				} catch (NumberFormatException e) {
					throw new TclException(interp, "expected integer but got \"" + argv[3] + "\"");
				}
			}
			throw new TclException(interp, "sorry, \"file atime\" is not available due to JVM restrictions.");

		case OPT_ATTRIBUTES:
			// FIXME: Java does not support file attribute retrieval (1.7
			// might!)
			fileObj = null;
			if (argv.length >= 3) {
				String fn = argv[2].toString();
				fileObj = FileUtil.getNewFileObj(interp, fn);
				if (fn.length() == 0 || !fileObj.exists()) {
					throw new TclPosixException(interp, TclPosixException.ENOENT, true, "could not read \"" + fn + "\"");
				}
			}
			switch (argv.length) {
			case 3:
				interp.setResult("-JTCL_DOES_NOT_SUPPORT_ATTRIBUTES FILE_ATTRIBUTES_NOT_SUPPORTED");
				return;
			case 4:
			case 5:
				if (argv[3].toString().equals("-JTCL_DOES_NOT_SUPPORT_ATTRIBUTES")) {
					if (argv.length == 4)
						interp.setResult("FILE_ATTRIBUTES_NOT_SUPPORTED");
					return;
				}
				/* Else, fall through */
			default:
				throw new TclException(interp, "Sorry, \"file attributes\" is not available to due JVM restrictions");
			}

		case OPT_CHANNELS:
			if (argv.length > 3) {
				throw new TclNumArgsException(interp, 2, argv, "?pattern?");
			}
			try {
				TclIO.getChannelNames(interp, argv.length == 2 ? null : TclString.newInstance(argv[2]));
			} catch (TclException e) {
				throw new TclException(interp, "Could not get channel names.");
			}

			return;

		case OPT_COPY:
			fileCopyRename(interp, argv, true);
			return;

		case OPT_DELETE:
			fileDelete(interp, argv);
			return;

		case OPT_DIRNAME:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}
			path = argv[2].toString();

			// Return all but the last component. If there is only one
			// component, return it if the path was non-relative, otherwise
			// return the current directory.

			TclObject splitArrayObj[] = TclList.getElements(interp, FileUtil.splitAndTranslate(interp, path));

			if (splitArrayObj.length > 1) {
				interp.setResult(FileUtil.joinPath(interp, splitArrayObj, 0, splitArrayObj.length - 1));
			} else if ((splitArrayObj.length == 0) || (FileUtil.getPathType(path) == FileUtil.PATH_RELATIVE)) {
				if (JACL.PLATFORM == JACL.PLATFORM_MAC) {
					interp.setResult(":");
				} else {
					interp.setResult(".");
				}
			} else {
				interp.setResult(splitArrayObj[0].toString());
			}
			return;

		case OPT_EXECUTABLE:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}
			if (argv[2].toString().length() == 0) {
				interp.setResult(false);
			} else {
				interp.setResult(FileUtil.isExecutable(FileUtil.getNewFileObj(interp, argv[2].toString())));
			}
			return;

		case OPT_EXISTS:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}
			if (argv[2].toString().length() == 0) {
				interp.setResult(false);
			} else {
				try {
					fileObj = FileUtil.getNewFileObj(interp, argv[2].toString());
					interp.setResult(fileObj.exists());
				} catch (TclException e) {
					interp.setResult(false);
				}
			}
			return;

		case OPT_EXTENSION:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}
			interp.setResult(getExtension(argv[2].toString()));
			return;

		case OPT_ISDIRECTORY:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}
			if (argv[2].toString().length() == 0) {
				interp.setResult(false);
			} else {
				fileObj = FileUtil.getNewFileObj(interp, argv[2].toString());
				interp.setResult(fileObj.isDirectory());
			}
			return;

		case OPT_ISFILE:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}
			fileObj = FileUtil.getNewFileObj(interp, argv[2].toString());
			interp.setResult(fileObj.isFile());
			return;

		case OPT_JOIN:
			if (argv.length < 3) {
				throw new TclNumArgsException(interp, 2, argv, "name ?name ...?");
			}
			interp.setResult(FileUtil.joinPath(interp, argv, 2, argv.length));
			return;

		case OPT_LINK:
			// FIXME: Java doesn't support links, but we'll do what we can.
			if (argv.length < 3 || argv.length > 5) {
				throw new TclNumArgsException(interp, 2, argv, "?-linktype? linkname ?target?");
			}
			String linkName = null;
			;
			String targetName = null;
			if ("-symbolic".equals(argv[2].toString()) || "-hard".equals(argv[2].toString())) {
				linkName = argv[3].toString();
				targetName = argv.length == 5 ? argv[4].toString() : null;
				throw new TclException(interp,
						"sorry, creating links with \"file link\" is not available due to JVM restrictions.");
			} else if (argv.length == 5) {
				throw new TclException(interp, "bad switch \"" + argv[2] + "\": must be -symbolic or -hard");
			} else {
				linkName = argv[2].toString();
				targetName = argv.length == 4 ? argv[3].toString() : null;
			}
			File link = FileUtil.getNewFileObj(interp, linkName);

			if (targetName != null) {
				if (link.exists()) {
					throw new TclException(interp, "could not create new link \"" + linkName
							+ "\": that path already exists");
				}
				File target = FileUtil.getNewFileObj(interp, targetName);
				if (!target.exists()) {
					throw new TclException(interp, "could not create new link \"" + linkName + "\" since target \""
							+ targetName + "\" doesn't exist");
				}
				throw new TclException(interp,
						"sorry, creating a link with \"file link\" is not available due to JVM restrictions.");
			} else {
				/*
				 * With 3 args, test if the link is really a link. FIXME: This
				 * is an approximate test
				 */
				if (!link.exists() || linkName.length() == 0) {
					throw new TclPosixException(interp, TclPosixException.ENOENT, true, "could not read link \""
							+ linkName + "\"");
				}
				File targetOfLink = FileUtil.getLinkTarget(link);
				if (targetOfLink == null) {
					throw new TclException(interp, "could not read link \"" + linkName + "\": invalid argument");
				}
				interp.setResult(targetOfLink.getAbsolutePath());
				return;
			}

			/* case LSTAT: see below for case STAT: */

		case OPT_MTIME:
			if (argv.length < 3 || argv.length > 4) {
				throw new TclNumArgsException(interp, 2, argv, "name ?time?");
			}
			if (argv[2].toString().length() == 0) {
				throw new TclException(interp, "could not read \"\": no such file or directory");
			}
			fileObj = FileUtil.getNewFileObj(interp, argv[2].toString());
			if (argv.length == 4) {
				long newTime = TclInteger.getLong(interp, argv[3]) * 1000;
				fileObj.setLastModified(newTime);
			} else {
				interp.setResult(getMtime(interp, argv[2].toString(), fileObj));
			}
			return;

		case OPT_MKDIR:
			fileMakeDirs(interp, argv);
			return;

		case OPT_NATIVENAME:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}

			interp.setResult(FileUtil.translateFileName(interp, argv[2].toString()));
			return;

		case OPT_NORMALIZE:
			TclObject fName = null;

			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "filename");
			}
			if (argv[2].toString().length() == 0) {
				interp.setResult("");
			} else {
				fName = FileUtil.getNormalizedPath(interp, argv[2]);

				if (fName == null) {
					throw new TclException(interp, "Cannot normalize this path!");
				}

				interp.setResult(fName);
			}
			return;

		case OPT_OWNED:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}
			if (argv[2].toString().length() == 0) {
				interp.setResult(false);
			} else {
				fileObj = FileUtil.getNewFileObj(interp, argv[2].toString());
				interp.setResult(isOwner(interp, fileObj));
			}
			return;

		case OPT_PATHTYPE:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}
			switch (FileUtil.getPathType(argv[2].toString())) {
			case FileUtil.PATH_RELATIVE:
				interp.setResult("relative");
				return;
			case FileUtil.PATH_VOLUME_RELATIVE:
				interp.setResult("volumerelative");
				return;
			case FileUtil.PATH_ABSOLUTE:
				interp.setResult("absolute");
			}
			return;

		case OPT_READABLE:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}
			if (argv[2].toString().length() == 0) {
				interp.setResult(false);
			} else {
				fileObj = FileUtil.getNewFileObj(interp, argv[2].toString());
				interp.setResult(fileObj.canRead());
			}
			return;

		case OPT_READLINK:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}
			fileObj = FileUtil.getNewFileObj(interp, argv[2].toString());
			if (!fileObj.exists() || argv[2].toString().length() == 0) {
				throw new TclPosixException(interp, TclPosixException.ENOENT, true, "could not readlink \"" + argv[2]
						+ "\"");
			}
			File target = FileUtil.getLinkTarget(fileObj);
			if (target == null) {
				throw new TclException(interp, "could not readlink \"" + argv[2] + "\": invalid argument");
			}
			interp.setResult(target.getPath());
			return;

		case OPT_RENAME:
			fileCopyRename(interp, argv, false);
			return;

		case OPT_ROOTNAME:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}
			String fileName = argv[2].toString();
			String extension = getExtension(fileName);
			int diffLength = fileName.length() - extension.length();
			interp.setResult(fileName.substring(0, diffLength));
			return;

		case OPT_SEPARATOR:
			String arg;
			if (argv.length > 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}

			if (argv.length == 2) {
				arg = null;
			} else {
				arg = argv[2].toString();
				if (arg.length() == 0) {
					throw new TclException(interp, "Unrecognised path");
				}
			}

			String separator = FileUtil.getSeparators(arg);
			interp.setResult(separator);
			return;

		case OPT_SIZE:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}
			if (argv[2].toString().length() == 0) {
				throw new TclException(interp, "could not read \"\": no such file or directory");
			}
			fileObj = FileUtil.getNewFileObj(interp, argv[2].toString());
			if (!fileObj.exists()) {
				throw new TclPosixException(interp, TclPosixException.ENOENT, true, "could not read \""
						+ argv[2].toString() + "\"");
			}
			interp.setResult(fileObj.length());
			return;

		case OPT_SPLIT:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}
			interp.setResult(FileUtil.splitPath(interp, argv[2].toString()));
			return;

		case OPT_STAT:
		case OPT_LSTAT:
			if (argv.length != 4) {
				throw new TclNumArgsException(interp, 2, argv, "name varName");
			}
			if (argv[2].toString().length() == 0) {
				throw new TclException(interp, "could not read \"\": no such file or directory");
			}
			getAndStoreStatData(interp, argv[2].toString(), argv[3].toString(), opt == OPT_LSTAT);
			return;

		case OPT_SYSTEM:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}
			if (!FileUtil.getNewFileObj(interp, argv[2].toString()).exists() || argv[2].toString().length() == 0) {
				// this is the error message that test cases expect.
				throw new TclException(interp, "Unrecognised path");
			}
			interp.setResult("native");
			return;

		case OPT_TAIL:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}
			interp.setResult(getTail(interp, argv[2].toString()));
			return;

		case OPT_TYPE:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}
			if (argv[2].toString().length() == 0) {
				throw new TclException(interp, "could not read \"\": no such file or directory");
			}
			fileObj = FileUtil.getNewFileObj(interp, argv[2].toString());
			interp.setResult(getType(interp, argv[2].toString(), fileObj));
			return;

		case OPT_VOLUMES:
			if (argv.length != 2) {
				throw new TclNumArgsException(interp, 2, argv, null);
			}

			File[] roots = File.listRoots();
			if (roots != null) {
				TclObject list = TclList.newInstance();
				for (File root1 : roots) {
					String root = root1.getPath();
					TclList.append(interp, list, TclString.newInstance(root));
				}
				interp.setResult(list);
			}
			return;

		case OPT_WRITABLE:
			if (argv.length != 3) {
				throw new TclNumArgsException(interp, 2, argv, "name");
			}
			if (argv[2].toString().length() == 0) {
				interp.setResult(false);
			} else {
				fileObj = FileUtil.getNewFileObj(interp, argv[2].toString());
				interp.setResult(fileObj.canWrite());
			}
			return;
		default:
			throw new TclRuntimeError("file command with opt " + argv[1].toString() + " is not implemented");
		}
	}

	/**
	 * If "File" is owned by the uid associated with the program, return true.
	 * Otherwise, return false.
	 * 
	 * @param interp
	 * @param fileObj
	 * @return owner
	 * @throws TclException
	 */
	private static boolean isOwner(Interp interp, File fileObj) throws TclException {
		// If the file doesn't exist, return false;

		if (!fileObj.exists()) {
			return false;
		}
		boolean owner = true;

		// For Windows and Macintosh, there are no user ids
		// associated with a file, so we always return 1.

		if (Util.isUnix()) {
			// FIXME: Not yet implemented on Unix. Do no checking, for now.
			// Java does not support ownership checking.
		}
		return owner;
	}

	/**
	 * Finds the last modification of file in fileObj.
	 * 
	 * @param interp
	 * @param fileName
	 * @param fileObj
	 * @return int representation of modification time, in seconds.
	 * @throws TclException
	 */
	private static int getMtime(Interp interp, String fileName, File fileObj) throws TclException

	{
		if (!fileObj.exists()) {
			throw new TclPosixException(interp, TclPosixException.ENOENT, true, "could not read \"" + fileName + "\"");
		}
		// Divide to convert msecs to seconds
		return (int) (fileObj.lastModified() / 1000);
	}

	/**
	 * Finds the type of file in fileObj. WARNING: Only checks for file and
	 * directory status. If neither file or direcotry, return link. Java only
	 * supports file and directory checking.
	 * 
	 * @param interp
	 * @param fileName
	 * @param fileObj
	 * @return
	 * @throws TclException
	 */
	private static String getType(Interp interp, String fileName, File fileObj) throws TclException {
		if (!fileObj.exists()) {
			throw new TclPosixException(interp, TclPosixException.ENOENT, true, "could not read \"" + fileName + "\"");
		}

		if (FileUtil.getLinkTarget(fileObj) != null) {
			return "link";
		}
		if (fileObj.isFile()) {
			return "file";
		} else if (fileObj.isDirectory()) {
			return "directory";
		}
		return "link";
	}

	/**
	 * This is a utility procedure that breaks out the fields of a "stat"
	 * structure and stores them in textual form into the elements of an
	 * associative array. WARNING: skipping dev, gid, ino, mode, and nlink
	 * attributes. WARNING: ctime and atime are the same as mtime. Java does not
	 * support the above attributes.
	 * 
	 * Results: Returns a standard Tcl return value. If an error occurs then a
	 * message is left in interp->result.
	 * 
	 * Side effects: Elements of the associative array given by "varName" are
	 * modified.
	 * 
	 * @param interp
	 *            current interpreter
	 * @param fileName
	 *            file to stat
	 * @param varName
	 *            variable name into which to put stat results
	 * @param lstat
	 *            if true, perform lstat instead of stat
	 * @throws TclException
	 */
	private static void getAndStoreStatData(Interp interp, String fileName, String varName, boolean lstat)

	throws TclException {
		File fileObj = FileUtil.getNewFileObj(interp, fileName);
		if (lstat) {
			File target = FileUtil.getLinkTarget(fileObj);
			if (target != null)
				fileObj = target;
		}
		if (!fileObj.exists()) {
			throw new TclPosixException(interp, TclPosixException.ENOENT, true, "could not read \"" + fileName + "\"");
		}

		try {
			int mtime = getMtime(interp, fileName, fileObj);
			TclObject mtimeObj = TclInteger.newInstance(mtime);
			TclObject atimeObj = TclInteger.newInstance(mtime);
			TclObject ctimeObj = TclInteger.newInstance(mtime);
			// interp.setVar(varName, "atime", atimeObj, 0);
			interp.setVar(varName, "ctime", ctimeObj, 0);
			interp.setVar(varName, "mtime", mtimeObj, 0);
		} catch (SecurityException e) {
			throw new TclException(interp, e.getMessage());
		} catch (TclException e) {
			throw new TclException(interp, "can't set \"" + varName + "(dev)\": variable isn't array");
		}

		try {
			TclObject sizeObj = TclInteger.newInstance(fileObj.length());
			interp.setVar(varName, "size", sizeObj, 0);
		} catch (Exception e) {
			// Do nothing.
		}

		try {
			TclObject typeObj = TclString.newInstance(getType(interp, fileName, fileObj));
			interp.setVar(varName, "type", typeObj, 0);
		} catch (Exception e) {
		}

		try {
			TclObject uidObj = TclBoolean.newInstance(isOwner(interp, fileObj));
			interp.setVar(varName, "uid", uidObj, 0);
		} catch (TclException e) {
			// Do nothing.
		}
	}

	/**
	 * Return the substring of "path" which represents the file's extension. It
	 * is necessary to perform system specific operations because different
	 * systems have different separators.
	 * 
	 * @param path
	 * @return
	 */
	private static String getExtension(String path) {
		if (path.length() < 1) {
			return "";
		}

		// Set lastSepIndex to the first index in the last component of the
		// path.

		int lastSepIndex = -1;
		switch (JACL.PLATFORM) {
		case JACL.PLATFORM_WINDOWS:
			String tmpPath = path.replace('\\', '/').replace(':', '/');
			lastSepIndex = tmpPath.lastIndexOf('/');
			break;
		case JACL.PLATFORM_MAC:
			lastSepIndex = path.lastIndexOf(':');
			if (lastSepIndex == -1) {
				lastSepIndex = path.lastIndexOf('/');
			}
			break;
		default:
			lastSepIndex = path.lastIndexOf('/');
		}
		++lastSepIndex;

		// Return "" if the last character is a separator.

		if (lastSepIndex >= path.length()) {
			return ("");
		}

		// Find the last dot in the last component of the path.

		String lastSep = path.substring(lastSepIndex);
		int dotIndex = lastSep.lastIndexOf('.');

		// Return "" if no dot was found in the file's name.

		if (dotIndex == -1) {
			return "";
		}

		// In earlier versions, we used to back up to the first period in a
		// series
		// so that "foo..o" would be split into "foo" and "..o". This is a
		// confusing and usually incorrect behavior, so now we split at the last
		// period in the name.

		return (lastSep.substring(dotIndex));
	}

	/**
	 * Return the substring of "path" which represents the file's tail. It is
	 * necessary to perform system specific operations because different systems
	 * have different separators.
	 * 
	 * @param interp
	 * @param path
	 *            Path for which to find the tail.
	 * @return
	 * @throws TclException
	 *             Thrown if tilde subst, which may be called by
	 *             splitAndTranslate, fails.
	 */
	private static String getTail(Interp interp, String path) throws TclException {
		// Split the path and return the string form of the last component,
		// unless there is only one component which is the root or an absolute
		// path.

		TclObject splitResult = FileUtil.splitAndTranslate(interp, path);

		int last = TclList.getLength(interp, splitResult) - 1;

		if (last >= 0) {
			if ((last > 0) || (FileUtil.getPathType(path) == FileUtil.PATH_RELATIVE)) {
				TclObject tailObj = TclList.index(interp, splitResult, last);
				return tailObj.toString();
			}
		}
		return "";
	}

	/**
	 * This procedure implements the "mkdir" subcommand of the "file" command.
	 * Filename arguments need to be translated to native format before being
	 * passed to platform-specific code that implements mkdir functionality.
	 * WARNING: ignoring links because Java does not support them.
	 * 
	 * @param interp
	 *            current interpreter
	 * @param argv
	 *            arguments to the 'file command
	 * @throws TclException
	 */
	private static void fileMakeDirs(Interp interp, TclObject[] argv) throws TclException {
		boolean madeDir = false;

		for (int currentDir = 2; currentDir < argv.length; currentDir++) {

			String dirName = argv[currentDir].toString();
			if (dirName.length() == 0) {
				throw new TclPosixException(interp, TclPosixException.ENOENT, true, "can't create directory \"\"");
			}
			File dirObj = FileUtil.getNewFileObj(interp, dirName);
			if (dirObj.exists()) {
				// If the directory already exists, do nothing.

				if (dirObj.isDirectory()) {
					continue;
				}
				throw new TclPosixException(interp, TclPosixException.EEXIST, true, "can't create directory \""
						+ dirName + "\"");
			}
			try {
				madeDir = dirObj.mkdir();
				if (!madeDir) {
					madeDir = dirObj.mkdirs();
				}
			} catch (SecurityException e) {
				throw new TclException(interp, e.getMessage());
			}
			if (!madeDir) {
				throw new TclPosixException(interp, TclPosixException.EACCES, true, "can't create directory \""
						+ dirName + "\":  best guess at reason");
			}
		}
	}

	/**
	 * This procedure implements the "delete" subcommand of the "file" command.
	 * 
	 * @param interp
	 *            current interpreter
	 * @param argv
	 *            arguments to the 'file' command
	 * @throws TclException
	 */
	private static void fileDelete(Interp interp, TclObject[] argv) throws TclException {
		boolean force = false;
		int firstSource = 2;

		for (boolean last = false; (firstSource < argv.length) && (!last); firstSource++) {

			if (!argv[firstSource].toString().startsWith("-")) {
				break;
			}
			int opt;
			try {
				opt = TclIndex.get(interp, argv[firstSource], validOptions, "option", 1);
			} catch (TclException e) {
				// change error message to match tests
				throw new TclException(interp, e.getMessage().replace("must", "should"));
			}
			switch (opt) {
			case OPT_FORCE:
				force = true;
				break;
			case OPT_LAST:
				last = true;
				break;
			default:
				throw new TclRuntimeError("FileCmd.cmdProc: bad option " + opt + " index to validOptions");
			}
		}

		if (firstSource >= argv.length) {
			throw new TclNumArgsException(interp, 2, argv, "?options? file ?file ...?");
		}

		for (int i = firstSource; i < argv.length; i++) {
			if (argv[i].toString().length() > 0)
				deleteOneFile(interp, FileUtil.getNewFileObj(interp, argv[i].toString()), force);
		}
	}

	/**
	 * After performing error checking, deletes the specified file.
	 * 
	 * @param interp
	 *            the current interpreter
	 * @param fileObj
	 *            file object to delete
	 * @param force
	 *            if force, delete directories recursively
	 * @throws TclException
	 */
	private static void deleteOneFile(Interp interp, File fileObj, boolean force) throws TclException {
		boolean isDeleted = true;
		String fileName = fileObj.getPath();

		// Trying to delete a file that does not exist is not
		// considered an error, just a no-op

		if ((!fileObj.exists())) {
			return;
		}

		// If the file is a non-empty directory, recursively delete its children
		// if the -force option was chosen. Otherwise, throw an error.

		if (fileObj.isDirectory() && (fileObj.list().length > 0)) {
			if (force) {
				String fileList[] = fileObj.list();
				for (String aFileList : fileList) {
					deleteOneFile(interp, new File(fileObj, aFileList), force);
				}
			} else {
				throw new TclPosixException(interp, TclPosixException.ENOTEMPTY, "error deleting \"" + fileObj.getName()
						+ "\": directory not empty");
			}
		}
		try {
			isDeleted = fileObj.delete();
		} catch (SecurityException e) {
			throw new TclException(interp, e.getMessage());
		}
		if (!isDeleted) {
			throw new TclPosixException(interp, TclPosixException.EACCES, true, "error deleting \"" + fileName
					+ "\":  best guess at reason");
		}
	}

	/**
	 * This procedure implements the "copy" and "rename" subcommands of the
	 * "file" command. Filename arguments need to be translated to native format
	 * before being passed to platform-specific code that implements copy
	 * functionality.
	 * 
	 * @param interp
	 *            current interpreter
	 * @param argv
	 *            arguments to 'file' command
	 * @param copyFlag
	 *            if true, do a copy, else do a rename
	 * @throws TclException
	 */
	private static void fileCopyRename(Interp interp, TclObject[] argv, boolean copyFlag) throws TclException {
		int firstSource = 2;
		boolean force = false;

		for (boolean last = false; (firstSource < argv.length) && (!last); firstSource++) {

			if (!argv[firstSource].toString().startsWith("-")) {
				break;
			}
			int opt;
			try {
				opt = TclIndex.get(interp, argv[firstSource], validOptions, "option", 1);
			} catch (TclException e) {
				// change error message to match tests
				throw new TclException(interp, e.getMessage().replace("must", "should"));
			}
			switch (opt) {
			case OPT_FORCE:
				force = true;
				break;
			case OPT_LAST:
				last = true;
				break;
			default:
				throw new TclRuntimeError("FileCmd.cmdProc: bad option " + opt + " index to validOptions");
			}
		}

		if (firstSource >= (argv.length - 1)) {
			throw new TclNumArgsException(interp, firstSource, argv, "?options? source ?source ...? target");
		}

		// WARNING: ignoring links because Java does not support them.

		int target = argv.length - 1;
		String targetName = argv[target].toString();

		File targetObj = FileUtil.getNewFileObj(interp, targetName);
		if (targetObj.isDirectory()) {
			// If the target is a directory, move each source file into target
			// directory. Extract the tailname from each source, and append it
			// to
			// the end of the target path.

			for (int source = firstSource; source < target; source++) {

				String sourceName = argv[source].toString();

				if (targetName.length() == 0) {
					copyRenameOneFile(interp, sourceName, targetName, copyFlag, force);
				} else {
					String tailName = getTail(interp, sourceName);

					TclObject joinArrayObj[] = new TclObject[2];
					joinArrayObj[0] = TclString.newInstance(targetName);
					joinArrayObj[1] = TclString.newInstance(tailName);

					String fullTargetName = FileUtil.joinPath(interp, joinArrayObj, 0, 2);

					copyRenameOneFile(interp, sourceName, fullTargetName, copyFlag, force);
				}
			}
		} else {
			// If there is more than 1 source file and the target is not a
			// directory, then throw an exception.

			if (firstSource + 1 != target) {
				String action;
				if (copyFlag) {
					action = "copying";
				} else {
					action = "renaming";
				}
				throw new TclPosixException(interp, TclPosixException.ENOTDIR, "error " + action + ": target \""
						+ argv[target].toString() + "\" is not a directory");
			}
			String sourceName = argv[firstSource].toString();
			copyRenameOneFile(interp, sourceName, targetName, copyFlag, force);
		}
	}

	/**
	 * After performing error checking, performs the copy and rename commands.
	 * WARNING: ignoring links because Java does not support them.
	 * 
	 * @param interp
	 * @param sourceName
	 *            source file.
	 * @param targetName
	 *            target file.
	 * @param copyFlag
	 *            whether to copy or rename.
	 * @param force
	 *            whether to overwrite.
	 * @throws TclException
	 */
	private static void copyRenameOneFile(Interp interp, String sourceName, String targetName, boolean copyFlag,
			boolean force) throws TclException {

		// Copying or renaming a file onto itself is a no-op if force is chosen,
		// otherwise, it will be caught later as an EEXISTS error.

		if (force && sourceName.equals(targetName)) {
			return;
		}

		// Check that the source exists and that if -force was not specified,
		// the
		// target doesn't exist.
		//
		// Prevent copying/renaming a file onto a directory and
		// vice-versa. This is a policy decision based on the fact that
		// existing implementations of copy and rename on all platforms
		// also prevent this.

		String action;
		if (copyFlag) {
			action = "copying";
		} else {
			action = "renaming";
		}

		File sourceFileObj = FileUtil.getNewFileObj(interp, sourceName);
		if ((!sourceFileObj.exists()) || (sourceName.length() == 0)) {
			throw new TclPosixException(interp, TclPosixException.ENOENT, true, "error " + action + " \"" + sourceName
					+ "\"");
		}

		if (targetName.length() == 0) {
			throw new TclPosixException(interp, TclPosixException.ENOENT, true, "error " + action + " \"" + sourceName
					+ "\" to \"" + targetName + "\"");
		}
		File targetFileObj = FileUtil.getNewFileObj(interp, targetName);
		if (targetFileObj.exists() && !force) {
			throw new TclPosixException(interp, TclPosixException.EEXIST, true, "error " + action + " \"" + sourceName
					+ "\" to \"" + targetName + "\"");
		}

		if (sourceFileObj.isDirectory() && !targetFileObj.isDirectory() && targetFileObj.exists()) {
			throw new TclPosixException(interp, TclPosixException.EISDIR, "can't overwrite file \"" + targetName
					+ "\" with directory \"" + sourceName + "\"");
		}
		if (targetFileObj.isDirectory() && !sourceFileObj.isDirectory()) {
			throw new TclPosixException(interp, TclPosixException.EISDIR, "can't overwrite directory \"" + targetName
					+ "\" with file \"" + sourceName + "\"");
		}
		/* Does the target name a parent path? If so, that parent must exist */
		if (targetFileObj.getParent() != null) {
			if (!targetFileObj.getParentFile().exists()) {
				throw new TclPosixException(interp, TclPosixException.ENOENT, true, "error " + action + " \""
						+ sourceName + "\" to \"" + targetName + "\"");
			}
		}

		if (!copyFlag) {
			// Perform the rename procedure.

			if (!sourceFileObj.renameTo(targetFileObj)) {

				if (targetFileObj.isDirectory()) {
					throw new TclPosixException(interp, TclPosixException.EEXIST, true, "error renaming \""
							+ sourceName + "\" to \"" + targetName + "\"");
				}

				throw new TclPosixException(interp, TclPosixException.EACCES, true, "error renaming \"" + sourceName
						+ "\" to \"" + targetName + "\":  best guess at reason");
			}
		} else {
			// Perform the copy procedure.
			
			// Directories just get made
			if (sourceFileObj.isDirectory()) {
				if (! targetFileObj.mkdir()) {
					throw new TclPosixException(interp, TclPosixException.EACCES, true, "error copying \"" + sourceName
							+ "\" to \"" + targetName + "\":  best guess at reason");
				}
				/* Recursively copy all files in this directory */
				for (File f : sourceFileObj.listFiles()) {
					copyRenameOneFile(interp, f.getPath(), new File(targetFileObj,f.getName()).getPath(), true, force);
				}
			} else {	
				try {
					BufferedInputStream bin = new BufferedInputStream(new FileInputStream(sourceFileObj));
					BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(targetFileObj));
	
					final int bsize = 1024;
					byte[] buff = new byte[bsize];
					int numChars = bin.read(buff, 0, bsize);
					while (numChars != -1) {
						bout.write(buff, 0, numChars);
						numChars = bin.read(buff, 0, bsize);
					}
					bin.close();
					bout.close();
				} catch (IOException e) {
					throw new TclException(interp, "error copying: " + e.getMessage());
				}
			}
		}
	}

}

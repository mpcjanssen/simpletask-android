###############################################################################
# hyde
#
# dr. jacl and mr. hyde - split personality programming:  write java in tcl
#
# inspired by 'CriTcl', by Jean-Claude Wippler   http://wiki.tcl.tk/tcl/critcl
#
#
################################################################################

package provide hyde 1.7
package require java

namespace eval hyde {
    variable arraysHashcode {

	private static int hashCode(int[] array) {
		final int prime = 31;
		if (array == null)
			return 0;
		int result = 1;
		for (int index = 0; index < array.length; index++) {
			result = prime * result + array[index];
		}
		return result;
	}
	private static int hashCode(float[] array) {
		final int prime = 31;
		if (array == null)
			return 0;
		int result = 1;
		for (int index = 0; index < array.length; index++) {
			result = prime * result + Float.floatToIntBits(array[index]);
		}
		return result;
	}
	private static int hashCode(short[] array) {
		final int prime = 31;
		if (array == null)
			return 0;
		int result = 1;
		for (int index = 0; index < array.length; index++) {
			result = prime * result + array[index];
		}
		return result;
	}
	private static int hashCode(Object[] array) {
		final int prime = 31;
		if (array == null)
			return 0;
		int result = 1;
		for (int index = 0; index < array.length; index++) {
			result = prime * result
					+ (array[index] == null ? 0 : array[index].hashCode());
		}
		return result;
	}
	private static int hashCode(byte[] array) {
		final int prime = 31;
		if (array == null)
			return 0;
		int result = 1;
		for (int index = 0; index < array.length; index++) {
		}
		return result;
	}
	private static int hashCode(long[] array) {
		final int prime = 31;
		if (array == null)
			return 0;
		int result = 1;
		for (int index = 0; index < array.length; index++) {
			result = prime * result
					+ (int) (array[index] ^ (array[index] >>> 32));
		}
		return result;
	}
	private static int hashCode(char[] array) {
		final int prime = 31;
		if (array == null)
			return 0;
		int result = 1;
		for (int index = 0; index < array.length; index++) {
			result = prime * result + array[index];
		}
		return result;
	}
	private static int hashCode(boolean[] array) {
		final int prime = 31;
		if (array == null)
			return 0;
		int result = 1;
		for (int index = 0; index < array.length; index++) {
			result = prime * result + (array[index] ? 1231 : 1237);
		}
		return result;
	}
	private static int hashCode(double[] array) {
		final int prime = 31;
		if (array == null)
			return 0;
		int result = 1;
		for (int index = 0; index < array.length; index++) {
			long temp = Double.doubleToLongBits(array[index]);
			result = prime * result + (int) (temp ^ (temp >>> 32));
		}
		return result;
	}
  }
}

################################################################################
# startup processing
#
# check env array for hyde properties
# read cache if exists
#

namespace eval hyde {

    variable debug
    variable runtime
    variable compileDir
    variable cacheFile
    variable compiler
    variable writeCache
    variable janino_init  ""
    variable janinocp_init  ""
    variable janinocp_code  ""
    variable hydePath [file dirname [info script]]
    variable cacheCode
    variable cacheTime -1
    array set cacheCode {}

    # define configure to be use by both startup and runtime
    proc configure {{name ""} {value ""}} {
	variable debug
	variable runtime
	variable compileDir
	variable cacheFile
	variable writeCache
	variable compiler

	if {! [string length $name]} {
	    return [list [list -debug $debug] [list -runtime $runtime] \
		[list -cacheFile $cacheFile] [list -compiler $compiler] \
		[list -compiledir $compileDir] [list -writecache $writeCache]]
	}

	switch -- $name {

	    -debug {
		if {[string equal $value 1] || [string equal $value 0]} {
		    set debug $value
		    return $debug
		}
		error "invalid value \"$value\", should be 0 or 1"
	    }

	    -runtime {
		switch -- [string tolower $value] {
		    compile -
		    forcecompile -
		    runfromcache {set runtime [string tolower $value]; return $runtime}
		}
		error "invalid value \"$value\", should be compile, forcecompile, or runfromcache"
	    }

	    -writecache {
		if {[string is integer -strict $value]} {
		    set writeCache [expr {$value != 0}]
		    return $writeCache
		}
		error "invalid value \"$value\", should be 0 or 1"
	    }

	    -compiledir {
		if {! [string length $value]} {
		    set compileDir [mkTempDir hyde]
		    return $compileDir
		} else {
		    set compileDir $value
		    return $compileDir
		}
	    }

	    -cachefile {
		set cacheFile $value
		loadcache
		return $cacheFile
	    }

	    -compiler {
		switch -- [string tolower $value] {
		    pizza -
		    javac -
		    jikes -
		    janino -
		    janinocp -
		    gcj   { set compiler [string tolower $value]; return $compiler}
		}
		error "invalid value \"$value\", should be pizza, javac, jikes, janino, janinocp, or gcj"
	    }

	    error "invalid option \"$name\", should be -debug, -runtime, -compiledir, -cachefile, or -compiler"
	}
    }

    proc rmcompiledir {} {
	variable compileDir
	if {[file dirname $compileDir] eq $::env(java.io.tmpdir)} {
	    catch {file delete -force $compileDir}
	}
    }

    proc setFromList {obj propvalue} {
	if {[llength $propvalue] % 2 != 0} {
	    error "args must be a list of \"property-name value\" pairs"
	}
	foreach {prop value} $propvalue {
	    $obj set_$prop $value
	}
    }

    proc setFromArray {obj arrayName} {
	upvar $arrayName arr
	foreach {prop value} [array get arr] {
	    $obj set_$prop $value
	}
    }

    proc mkTempDir {{prefix "tmp"}} {
	variable debug
	set i [expr {int(rand() * 100000)}]
	while {1} {
	    set tmpDir ""
	    set dir [file join $::env(java.io.tmpdir) $prefix$i.d]
	    if {! [catch {file mkdir $dir}]} {
		if {$debug} {
		    puts "mkTempDir $dir"
		}
		return $dir
	    }
	    incr i
	}
    }

    proc chkCompileDir {} {
	variable compileDir
	variable debug
	if {! [file isdirectory $compileDir]} {
	    if {! [catch {file mkdir $compileDir}]} {
		if {$debug} {
		    puts "chkCompileDir: mkdir $compileDir"
		}
		return
	    } else {
		# create a temp directory
		configure -compiledir ""
	    }
	} else {
	    if {[file isdirectory $compileDir] && [file writable $compileDir]} {
		return
	    } else {
		# create temp directory
		configure -compiledir ""
	    }
	}
    }

    proc loadcache {} {
	variable cacheCode
	variable cacheFile
	variable cacheTime
	variable runtime
	variable debug
	if {[file isfile $cacheFile] && ![string equal $runtime forcecompile]} {
	    set cacheTime [file mtime $cacheFile]
	    if {$debug} {
		puts stderr \
		    "hyde: reading cachefile $cacheFile with time $cacheTime"
	    }
	    interp create -safe cacheReader
	    set fd [open $cacheFile]
	    set cacheData [read $fd]
	    close $fd
	    catch {interp eval cacheReader $cacheData}
	    catch {array set cacheCode [interp eval cacheReader \
		    array get cacheCode]}
	    interp delete cacheReader
	    if {$debug} {
		puts stderr \
		    "hyde: cachefile $cacheFile has: [lsort [array names cacheCode]]"
	    }
	}
    }

    # set defaults and check for optional property settings
    # properties, via env array:
    #    hyde.debug      hyde debugging switch
    #    hyde.runtime    hyde runtime compile/cache behavior
    #    hyde.compiledir which directory to use for compiling
    #    hyde.cachefile  which file to use for compile code cache, temp dir
    #    hyde.writecache write cache file after compile
    #    hyde.compiler   which compiler method to use

    # hyde.debug: values 0 (default), 1 debugging on
    # don't delete java and compiled classes
    # print cache loading and resolution
    variable debug 0
    if {[info exists ::env(hyde.debug)]} {
	configure -debug $::env(hyde.debug)
    }

    # hyde.runtime: compile, forcecompile, runfromcache,
    #    compile        run from cache if available and newer, otherwise compile
    #    forcecompile   always compile before running
    #    runfromcache   always run from cache,error if not exists,no write cache
    variable runtime compile
    if {[info exists ::env(hyde.runtime)]} {
	configure -runtime $::env(hyde.runtime)
    }

    # hyde.compileDir: compiled code tempdirectory
    # variable compiledir set during configure
    if {[info exists ::env(hyde.compiledir)]} {
	configure -compiledir $::env(hyde.compiledir)
    } else {
	if {[string first resource:/ $::argv0] == 0} {
	    # main program running from jar, use tmp dir
	    configure -compiledir {}
	} else {
	    configure -compiledir [file join [pwd] hyde]
	}
    }

    # hyde.writecache: write cache file after compiling
    # variable writecache set during configure
    if {[info exists ::env(hyde.writecache)]} {
	configure -writecache $::env(hyde.writecache)
    } else {
	if {[string first resource:/ $::argv0] == 0} {
	    # main program running from jar, don't writecache
	    configure -writecache 0
	} else {
	    configure -writecache 1
	}
    }

    # hyde.cachefile: compiled code cache file
    # variable cacheFile set during configure
    if {[info exists ::env(hyde.cachefile)]} {
	configure -cachefile $::env(hyde.cachefile)
    } else {
	configure -cachefile [file join [file dirname $::argv0] hydecache.tcl]
    }

    # hyde.compiler: java compiler method
    #   janino          janino compiler used via library jar
    #   janinocp        janinocp compiler used via library jar or classloader
    #   pizza           pizza compiler used via library jar
    #   javac           standard javac command line compiler
    #   jikes           ibm jikes command line compiler
    #   gcj             gnu gcj command line compiler
    #   see compileWith_* procs below for currently supported compilers
    variable compiler janino
    if {[info exists ::env(hyde.compiler)]} {
	configure -compiler $::env(hyde.compiler)
    }

}

################################################################################
# cache read/write
#
# obey "hyde.runtime" property rules:
#   write cache only if "compile" or "forcecompile"
#   read cache if "compile", cache exists, and cachefile is newer,  or
#   read cache if "runfromcache" but generate error if does not exists

proc hyde::writecache {} {
    variable debug
    variable runtime
    variable cacheCode
    variable cacheFile
    variable writeCache

    if {[string equal $runtime runfromcache] || ! $writeCache} {
	return
    }

    if {([file exists $cacheFile] && [file writable $cacheFile]) || \
	    (![file exists $cacheFile] && \
	    [file writable [file dirname $cacheFile]])} {
	if {$debug} {
	    puts stderr "hyde: writing bytecodes to $cacheFile"
	}
	set fd [open $cacheFile w]
	foreach {name bytecodeList} [array get cacheCode] {
	    set byteHex ""
	    puts -nonewline $fd "array set cacheCode \[list $name \[list "
	    foreach bytecode $bytecodeList {
		binary scan $bytecode H* byteHex
		puts -nonewline $fd "\[binary format H* $byteHex\] "
	    }
	    puts $fd "\]\]"
	}
	close $fd
    }
}

proc hyde::readcache {name {force 0}} {
    variable debug
    variable runtime
    variable cacheCode
    variable cacheFile
    variable cacheTime

    # force cache read, usually after a fresh compile
    if {$force} {
	if {![info exists cacheCode($name)]} {
	    error "hyde::readcache $name force, but does not exists"
	}
	return $cacheCode($name)
    }

    switch -- $runtime {
	compile {
	    if {![info exists cacheCode($name)]} {
		return ""
	    }
	    if {![string length [info script]]} {
		return ""
	    }
	    if {[set fileTime [file mtime [info script]]] < $cacheTime && \
		    $cacheTime > 0} {
		if {$debug} {
		    puts stderr \
			"hyde: [info script] $fileTime < cachefile $cacheTime"
		    puts stderr \
			"hyde: returning fresh cached bytecode for $name"
		}
		return $cacheCode($name)
	    }
	    return ""
	}
	forcecompile {
	    return ""
	}
	runfromcache {
	    if {![info exists cacheCode($name)]} {
		error "hyde: code for '$name' does not exist in cache"
	    } else {
		return $cacheCode($name)
	    }
	}
    }
    return ""
}

################################################################################
# compile_exec_compiler
#
# compile helper proc to exec a compiler as a separate process
# returns bytcodes on succesful compile, or generates Tcl error on failure

proc hyde::compile_exec_compiler {name codeStr cmdExec cmdArgs {keepClass 0}} {
    variable compileDir
    variable debug

    chkCompileDir
    set targetdir [file dirname $compileDir/$name]
    if {! [file isdirectory $targetdir]} {
	if {$debug} {
	    puts stderr "compile_exec_compiler: mkdir $targetdir"
	}
	file mkdir $targetdir
    }
    # delete previous class files, if any
    catch {eval file delete -force [glob $compileDir/$name.class $compileDir/$name\$*.class]}
    # write out java code to compile
    set fd [open $compileDir/$name.java w]
    puts $fd $codeStr
    close $fd
    if {$debug} {
	puts stderr "hyde: $cmdExec $cmdArgs $name.java"
    }
    set oldcd [pwd]
    cd $compileDir
    set rc [catch {eval exec $cmdExec $cmdArgs $name.java << {""}} result]
    cd $oldcd
    set byteCodeList [list]
    if {[file isfile $compileDir/$name.class]} {
	foreach classfile [lsort -dictionary [glob -nocomplain $compileDir/$name.class $compileDir/$name\$.class]] {
	    set fd [open $classfile]
	    fconfigure $fd -translation binary
	    set byteCode [read $fd]
	    lappend byteCodeList $byteCode
	    close $fd
	    if {! $debug} {
		file delete -force $classfile
	    }
	    # java class files produced by 'jclass' will likely need to stay around
	    # to be used by jproc or jcommand -import class
	    if {! $debug && !$keepClass} {
		file delete -force $classfile
	    }
	}
	return $byteCodeList
    } else {
	if {! $debug} {
	    catch {eval file delete -force [glob $compileDir/$name.class $compileDir/$name\$*.class]}
	}
	error $result
    }
}

################################################################################
# compileWith_*  methods
#
#
proc hyde::compileWith_javac {name codeStr {keepClass 0}} {
    variable compileDir
    chkCompileDir
    set classPath "-classpath $compileDir"
    if {[info exists ::env(CLASSPATH)] && [string length $::env(CLASSPATH)]} {
	append classPath $::env(path.separator) $::env(CLASSPATH)
    }
    if {[info exists ::env(TCL_CLASSPATH)] && \
	[string length $::env(TCL_CLASSPATH)]} {
	append classPath $::env(path.separator) [join $::env(TCL_CLASSPATH) $::env(path.separator)]
    }
    return [compile_exec_compiler $name $codeStr javac $classPath $keepClass]
}

proc hyde::compileWith_jikes {name codeStr {keepClass 0}} {
    variable compileDir
    chkCompileDir
    # jikes version 1.19 !!
    set ver 1.3
    catch {
	regexp {^([0-9]\.[0-9])} $::env(java.vm.version) match ver
    }
    set classPath "-target $ver -nowarn +Pno-naming-convention -d $compileDir -classpath $compileDir"
    if {[info exists ::env(CLASSPATH)] && [string length $::env(CLASSPATH)]} {
	append classPath $::env(path.separator) $::env(CLASSPATH)
    }
    if {[info exists ::env(TCL_CLASSPATH)] && \
	[string length $::env(TCL_CLASSPATH)]} {
	append classPath $::env(path.separator) [join $::env(TCL_CLASSPATH) $::env(path.separator)]
    }
    return [compile_exec_compiler $name $codeStr jikes $classPath $keepClass]
}

proc hyde::compileWith_gcj {name codeStr {keepClass 0}} {
    variable compileDir
    chkCompileDir
    set classPath "-C -classpath $compileDir"
    if {[info exists ::env(CLASSPATH)] && [string length $::env(CLASSPATH)]} {
	append classPath $::env(path.separator) $::env(CLASSPATH)
    }
    if {[info exists ::env(TCL_CLASSPATH)] && \
	[string length $::env(TCL_CLASSPATH)]} {
	append classPath $::env(path.separator) [join $::env(TCL_CLASSPATH) $::env(path.separator)]
    }
    return [compile_exec_compiler $name $codeStr gcj $classPath $keepClass]
}

proc hyde::compileWith_janino {name codeStr {keepClass 0}} {
    variable hydePath
    variable debug
    variable compileDir
    variable janino
    variable janino_init
    global env
    chkCompileDir
    set util  java.util
    set io    java.io
    set tcl   tcl.lang

    set classPath $compileDir
    if {[info exists env(CLASSPATH)] && [string length $env(CLASSPATH)]} {
	append classPath $env(path.separator) $env(CLASSPATH)
    }
    if {[info exists env(TCL_CLASSPATH)] && [string length $env(TCL_CLASSPATH)]} {
	append classPath $env(path.separator) [join  $env(TCL_CLASSPATH) $env(path.separator)]
    }

    set first_time 0
    if {[string length $janino_init] == 0} {
	# find the jtcl jar
	foreach path [split $env(CLASSPATH)  $env(path.separator)] {
            set last [file tail $path]
	    set tail [string tolower $last]
	    if {[string match jtcl*.jar $tail]} {
		set dir [file dirname $path]
		lappend env(TCL_CLASSPATH) [file join $dir $last]
		append classPath $env(path.separator) [file join $dir $last]
		set janino_init $classPath
		set first_time 1
		break
	    }
	}
    }

    if {$janino_init ne $classPath || $first_time} {

	set old_java_class_path [java::call System getProperty java.class.path]
	java::call System setProperty java.class.path $classPath

	set janino [java::new {org.codehaus.janino.SimpleCompiler boolean} 1]
	$janino setParentClassLoader [[java::getinterp] getClassLoader]

	java::call System setProperty java.class.path $old_java_class_path
    }

    if {[lsearch -exact $env(TCL_CLASSPATH) $compileDir] == -1} {
        lappend env(TCL_CLASSPATH) $compileDir
    }

    set err ""
    set bytecode_arr [java::null]
    if {[catch {set bytecode_arr [$janino compile $codeStr]} err]} {
	error $err
    } elseif {[java::isnull $bytecode_arr]} {
        error "janino compile failed: $err"
    }

    if {[$bytecode_arr length] < 1} {
	error "could not compile $name"
    }
    set byteCodeList [list]
    set numclassfiles [$bytecode_arr length]
    array set inner {}
    for {set i 0} {$i < $numclassfiles} {incr i} {
	set aclassfile [$bytecode_arr get $i]
	set class_name [$aclassfile getThisClassName]

	set rawByteCode [$aclassfile toByteArray]

	# since we get a raw byte array, turn it into a TclByteArray object...
	set tclobj [java::call $tcl.TclByteArray {newInstance byte[]} $rawByteCode]

	# ...and now make it into a Tcl variable
	[java::getinterp] {setVar java.lang.String tcl.lang.TclObject int} byteCode $tclobj 0

	if {[string first "\$" $class_name] == -1} {
	    set inner($class_name) $byteCode
	} else {
	    lappend byteCodeList $byteCode
	}

	# if debugging or keepClass is true, then write the bytecode back out
	# to a file, in case another class imports the one we just compiled
	# TODO: perhaps insert bytecodes into a hyde/hyde.jar file ??
	if {$debug || $keepClass} {
	    set class_pathname [string map {. /} $class_name]
	    set dir [file join $compileDir [file dirname $class_pathname]]
	    if {$debug} {
		puts stderr "compileWith_janino: mkdir $dir"
	    }
	    catch {file mkdir $dir}
	    set fd [open [file join $dir [file tail $class_pathname].class] w]
	    fconfigure $fd -translation binary
	    puts -nonewline $fd $byteCode
	    close $fd
	}

    }

    # append inner classes, if any

    foreach class_name [lsort -dictionary [array names inner]] {
	lappend byteCodeList $inner($class_name)
    }

    return $byteCodeList
}

proc hyde::compileWith_janinocp {name codeStr {keepClass 0}} {
    variable hydePath
    variable debug
    variable compileDir
    variable janinocp
    variable janinocp_init
    variable janinocp_code
    global env
    chkCompileDir
    set util  java.util
    set io    java.io
    set tcl   tcl.lang

    set classPath $compileDir
    if {[info exists env(CLASSPATH)] && [string length $env(CLASSPATH)]} {
	append classPath $env(path.separator) $env(CLASSPATH)
    }
    if {[info exists env(TCL_CLASSPATH)] && [string length $env(TCL_CLASSPATH)]} {
	append classPath $env(path.separator) [join  $env(TCL_CLASSPATH) $env(path.separator)]
    }

    set first_time 0
    if {[string length $janinocp_init] == 0} {
        # find the jtcl jar
        foreach path [split $env(CLASSPATH)  $env(path.separator)] {
    	set last [file tail $path]
    	set tail [string tolower $last]
    	if {[string match jtcl*.jar $tail]} {
    	    set dir [file dirname $path]
    	    lappend env(TCL_CLASSPATH) [file join $dir $last]
    	    append classPath $env(path.separator) [file join $dir $last]
    	    set janinocp_init $classPath
    	    set first_time 1
    	    break
    	}
        }
    }

    if {$janinocp_init ne $classPath || $first_time} {

	if {[string length $janinocp_code]} {
	    set defobj [java::defineclass $janinocp_code]
	    if {[java::isnull $defobj]} {
		error "hyde: jclass: defineclass failed for $classname"
	    }
	    set janinocp_code ""
	}

	set old_java_class_path [java::call System getProperty java.class.path]
	java::call System setProperty java.class.path $classPath

	set janinocp [java::new hydeinternal.JaninoCP [[[java::getinterp] getClassLoader] getParent ] ]

	java::call System setProperty java.class.path $old_java_class_path
    }

    if {[lsearch -exact $env(TCL_CLASSPATH) $compileDir] == -1} {
        lappend env(TCL_CLASSPATH) $compileDir
    }

    set err ""
    set bytecode_arr [java::null]
    if {[catch {set bytecode_arr [$janinocp compile $codeStr]} err]} {
	error $err
    } elseif {[java::isnull $bytecode_arr]} {
        error "janino compile failed: $err"
    }

    if {[$bytecode_arr length] < 1} {
	error "could not compile $name"
    }
    set byteCodeList [list]
    set numclassfiles [$bytecode_arr length]
    array set inner {}
    for {set i 0} {$i < $numclassfiles} {incr i} {
	set aclassfile [$bytecode_arr get $i]
	set class_name [$aclassfile getThisClassName]

	set rawByteCode [$aclassfile toByteArray]

	# since we get a raw byte array, turn it into a TclByteArray object...
	set tclobj [java::call $tcl.TclByteArray {newInstance byte[]} $rawByteCode]

	# ...and now make it into a Tcl variable
	[java::getinterp] {setVar java.lang.String tcl.lang.TclObject int} byteCode $tclobj 0

	if {[string first "\$" $class_name] == -1} {
	    set inner($class_name) $byteCode
	} else {
	    lappend byteCodeList $byteCode
	}

	# if debugging or keepClass is true, then write the bytecode back out
	# to a file, in case another class imports the one we just compiled
	# TODO: perhaps insert bytecodes into a hyde/hyde.jar file ??
	if {$debug || $keepClass} {
	    set class_pathname [string map {. /} $class_name]
	    set dir [file join $compileDir [file dirname $class_pathname]]
	    if {$debug} {
		puts stderr "compileWith_janinocp: mkdir $dir"
	    }
	    catch {file mkdir $dir}
	    set fd [open [file join $dir [file tail $class_pathname].class] w]
	    fconfigure $fd -translation binary
	    puts -nonewline $fd $byteCode
	    close $fd
	}

    }

    # append inner classes, if any

    foreach class_name [lsort -dictionary [array names inner]] {
	lappend byteCodeList $inner($class_name)
    }

    return $byteCodeList
}

proc hyde::compileWith_pizza {name codeStr {keepClass 0}} {
    variable hydePath
    variable pizzaPath
    variable debug
    variable compileDir
    global env
    chkCompileDir
    set pizzaVer 1.1

    set classPath $compileDir
    if {[info exists ::env(CLASSPATH)] && [string length $::env(CLASSPATH)]} {
	append classPath $::env(path.separator) $::env(CLASSPATH)
    }
    if {[info exists ::env(TCL_CLASSPATH)] && \
	[string length $::env(TCL_CLASSPATH)]} {
	append classPath $::env(path.separator) [join $::env(TCL_CLASSPATH) $::env(path.separator)]
    }

    if {$debug} {
	puts "classpath: $classPath"
	puts stderr "name: $name"
	puts stderr "codeStr:\n$codeStr"
    }

    # packages from which we import
    set pizza net.sf.pizzacompiler.compiler
    set util  java.util
    set io    java.io
    set tcl   tcl.lang

    set filename ${name}.java

    # find pizza jar
    if {! [info exists pizzaPath]} {
	set p [file join $hydePath pizza pizza-${pizzaVer}.jar]
	if {[file isfile $p]} {
	    set pizzaPath $p
	} else {
	    set pizzaPath [lindex [lsort -decreasing \
		[glob -nocomplain [file join $hydePath pizza pizza*.jar]]] 0]
	}
	if {! [info exists pizzaPath] || [string length $pizzaPath] == 0} {
	    error "can't find pizza*.jar"
	}

	set tcl_classpath ""
	catch {set tcl_classpath $env(TCL_CLASSPATH)}
	lappend env(TCL_CLASSPATH) $pizzaPath
	java::import  -package $pizza \
	    ByteArrayCompilerOutput ClassReader CompilerOutput Main \
	    MapSourceReader Report SourceReader
    }

    # set up compiler input, stuff source code into hash with filename as key
    set sourceHash [java::new $util.HashMap]
    $sourceHash put $filename $codeStr
    set compilerSource [java::new $pizza.MapSourceReader $sourceHash]

    # set up compiler bytecode output
    set compilerOutput [java::new $pizza.ByteArrayCompilerOutput]

    # set up compiler print output
    set print [java::new $io.PrintStream [java::new $io.ByteArrayOutputStream]]

    # invoke compiler
    java::call $pizza.Main init
    java::call $pizza.Main argument -pizza
    java::call $pizza.Main argument -nowarn
    java::call $pizza.Main setClassReader [java::new \
	$pizza.ClassReader $classPath [java::null]]
    set allFiles [java::new {String[]} 1 $filename]

    java::call $pizza.Main compile $allFiles \
	$compilerSource $compilerOutput $print

    # check compiler results for errors
    set errs [java::call $pizza.Report getErrorsAndWarnings]
    if {! [$errs isEmpty]} {
	set iter [$errs iterator]
	set msg ""
	while {[$iter hasNext]} {
	    lappend msg [[$iter next] toString]
	}
	error $msg
    }

    # get the bytecode from the compiler output object, an ArrayList
    set byteArrayList [java::cast $util.ArrayList [$compilerOutput getBytecode]]

    set byteCodeList [list]
    set inner [list]
    set numclassfiles [$byteArrayList size]
    for {set i 0} {$i < $numclassfiles} {incr i} {
	# get the first array index, from the first (and only) file compiled
	set rawByteCode [java::cast {byte[]}  [$byteArrayList get $i]]

	# since we get a raw byte array, turn it into a TclByteArray object...
	set tclobj [java::call $tcl.TclByteArray {newInstance byte[]} $rawByteCode]

	# ...and now make it into a Tcl variable
	[java::getinterp] setVar byteCode $tclobj 0

	lappend byteCodeList $byteCode

	# if debugging or keepClass is true, then write the bytecode back out
	# to a file, in case another class imports the one we just compiled
	# TODO: perhaps insert bytecodes into a hyde/hyde.jar file ??
	if {$debug || $keepClass} {
	    set dir [file join $compileDir [file dirname $name]]
	    if {$debug} {
		puts stderr "compileWith_pizza: mkdir $dir"
	    }
	    catch {file mkdir $dir}
	    set fd [open [file join $compileDir ${name}.class] w]
	    fconfigure $fd -translation binary
	    puts -nonewline $fd $byteCode
	    close $fd
	}
    }

    return $byteCodeList
}

###############################################################################
# generic compile proc

proc hyde::compile {name codeStr {keepClass 1}} {
    variable compiler
    variable cacheCode
    variable debug

    if {$debug} {
	puts "==============================================================="
	puts "compiling: $name"
	puts "---------------------------------------------------------------"
	puts $codeStr
	puts "==============================================================="
    }

    if {! [catch {compileWith_$compiler $name $codeStr $keepClass} result]} {
	array set cacheCode [list $name $result]
	writecache
    } else {
	error "hyde: compile failed for '$name' using '$compiler':\n$result"
    }
}

###############################################################################
# defineclass for a list of bytecodes

proc hyde::defineclass {byteCodeList} {
    set first 1
    set defobj [java::null]
    foreach byteCode $byteCodeList {
	if {$first} {
	    set defobj [java::defineclass $byteCode]
	    set first 0
	} else {
	    java::defineclass $byteCode
	}
    }
    return $defobj
}

###############################################################################
#
# jproc - compile a simple Java method
#

proc hyde::jproc {type name typedargs args} {

    set procname $name
    regsub -all :: $name {} name
    set methName $name
    append name Cmd

    set packageName hyde
    set className $packageName.$name
    set argList ""
    set procArgs ""
    set invokeArgs ""
    set comma ""
    foreach {argtype argname} $typedargs {
	append argList "$comma$argtype $argname"
	lappend procArgs   $argname
	lappend invokeArgs \$$argname
	set comma ", "
    }

    if {[llength $typedargs] % 2 != 0} {
	error "hyde: jproc: typedargs must be pairs of 'type argname .. ..'"
    }
    set packageName hyde
    set importList ""
    set exceptionList ""
    set extendsClass ""
    set implementsList ""
    set auxmethods ""
    set body ""
    while {[llength $args] > 1} {
	switch -- [lindex $args 0] {
	    -package {
		set packageName [lindex $args 1]
		set args [lrange $args 2 end]
	    }
	    -import {
		set importList [lindex $args 1]
		set args [lrange $args 2 end]
	    }
	    -extends {
		set extendsClass "extends [lindex $args 1]"
		set args [lrange $args 2 end]
	    }
	    -implements {
		set implementsList "implements [join [lindex $args 1] ", "]"
		set args [lrange $args 2 end]
	    }
	    -auxmethods {
		set auxmethods [lindex $args 1]
		set args [lrange $args 2 end]
	    }
	    -throws {
		set exceptionList "throws [join [lindex $args 1] ", "]"
		set args [lrange $args 2 end]
	    }
	    -body {
		set body [lindex $args 1]
		set args [lrange $args 2 end]
	    }
	    default {
		# bad argument
		error "jproc: unrecognized argument: '[lindex $args 0]'"
	    }
	}
    }

    set pathName $packageName
    regsub -all \\. $pathName / pathName

    # check if cached bytecode exists

    if {! [string length [set byteCodeList [readcache $pathName/$name]]]} {

	# bytecode not in cache: assemble code and compile
	if {![string length [append body [lindex $args 0]]]} {
	    error "hyde: jproc: no body defined"
	}

	# now build code for a static method call
	append codeStr ""
	if {[string length $packageName]} {
	    append codeStr "package $packageName;" \n
	    set className $packageName.$name
	} else {
	    error "hyde: jproc: '-package name' cannot be null"
	}
	foreach importPkg $importList {
	    append codeStr "import $importPkg;" \n
	}
	append codeStr "public class $name $extendsClass $implementsList {" \n
	append codeStr $auxmethods \n \n
	append codeStr "  public static $type" \n
	append codeStr "  $methName ($argList) $exceptionList {" \n \n
	append codeStr "  " $body \n \n
	append codeStr "  }" \n
	append codeStr "}" \n

	compile $pathName/$name $codeStr
	set byteCodeList [readcache $pathName/$name 1]

    }

    # send bytecode to jvm, define a helper proc to call static method
    set defobj [defineclass $byteCodeList]
    if {[java::isnull $defobj]} {
	error "hyde: jproc: defineclass failed for $packageName.$name $procname"
    }

    proc ::$procname $procArgs \
	"return \[java::call $packageName.$name $methName [join $invokeArgs]\]"

    return ""
}

###############################################################################
#
# jcommand - compile a Jacl command
#

proc hyde::jcommand {name args} {

    set procname $name
    regsub -all :: $name {} name
    append name Cmd

    # check if cached bytecode exists

    if {! [string length [set byteCodeList [readcache $name]]]} {

	# bytecode not in cache: assemble code and compile

	set importList ""
	set body ""
	set auxmethods ""
	set disposeimpl ""
	set disposebody ""
	while {[llength $args] > 1} {
	    switch -- [lindex $args 0] {
		-import {
		    set importList [lindex $args 1]
		    set args [lrange $args 2 end]
		}
		-dispose {
		    set disposeimpl ", CommandWithDispose"
		    set disposebody "  public void disposeCmd() { \n"
		    append disposebody	"    [lindex $args 1] \n  }"
		    set args [lrange $args 2 end]
		}
		-auxmethods {
		    set auxmethods [lindex $args 1]
		    set args [lrange $args 2 end]
		}
		-body {
		    set body [lindex $args 1]
		    set args [lrange $args 2 end]
		}
		default {
		    # bad argument
		    error "jcommand: unrecognized argument: '[lindex $args 0]'"
		}
	    }
	}
	if {![string length [append body [lindex $args 0]]]} {
	    error "hyde: jcommand: no body defined"
	}

	# now build code for a tcl command class
	append codeStr "package hyde;" \n
	append codeStr "import tcl.lang.*;" \n
	foreach importPkg $importList {
	    append codeStr "import $importPkg;" \n
	}
	append codeStr "public class $name " \n
	append codeStr "    implements Command $disposeimpl {" \n \n
	append codeStr $auxmethods \n \n
	append codeStr $disposebody \n \n
	append codeStr "  public void" \n
	append codeStr {  cmdProc (Interp interp, TclObject argv[])} \n
	append codeStr "    throws TclException {" \n \n
	append codeStr "  " $body \n \n
	append codeStr "  }" \n
	append codeStr "}" \n

	compile hyde/$name $codeStr
	set byteCodeList [readcache hyde/$name 1]
    }

    # send bytecode to jvm, create command in interpreter
    set defobj [defineclass $byteCodeList]
    if {[java::isnull $defobj]} {
	error "hyde: jcommand: defineclass failed for hyde.$name $procname"
    }
    if {[catch {[java::getinterp] createCommand ::$procname \
		    [java::new hyde.$name]}]} {
	error "jcommand: couldn't define new Tcl command $procname"
    }
    return ""
}

###############################################################################
#
# jclass - compile a Java class
#

proc hyde::jclass {name args} {
    variable compileDir
    variable debug
    variable arraysHashcode

    set className $name
    set cname $name

    set packageName "hyde"
    set accessType public
    set importList ""
    set includeStr ""
    set includeFile ""
    set extendsClass ""
    set implementsList ""
    set propertyList ""
    set hashEqualsList ""
    set toStringList ""
    set body ""
    while {[llength $args] > 1} {
	switch -- [lindex $args 0] {
	    -source {
		set includeStr [lindex $args 1]
		set args [lrange $args 2 end]
	    }
	    -include {
		set includeFile [lindex $args 1]
		set args [lrange $args 2 end]
	    }
	    -package {
		set packageName [lindex $args 1]
		set args [lrange $args 2 end]
	    }
	    -access {
		set accessType [lindex $args 1]
		set args [lrange $args 2 end]
	    }
	    -import {
		set importList [lindex $args 1]
		set args [lrange $args 2 end]
	    }
	    -extends {
		set extendsClass "extends [lindex $args 1]"
		set args [lrange $args 2 end]
	    }
	    -implements {
		set implementsList "implements [join [lindex $args 1] {, }]"
		set args [lrange $args 2 end]
	    }
	    -properties {
		set propertyList [lindex $args 1]
		set args [lrange $args 2 end]
	    }
	    -hashequals {
		set hashEqualsList [lindex $args 1]
		set args [lrange $args 2 end]
	    }
	    -tostring {
		set toStringList [lindex $args 1]
		set args [lrange $args 2 end]
	    }
	    -body {
		set body [lindex $args 1]
		set args [lrange $args 2 end]
	    }
	    default {
		# bad argument
		error "hyde: jclass: unrecognized argument: '[lindex $args 0]'"
	    }
	}
    }

    set types [list public protected private ""]
    if {[lsearch $types $accessType ] == -1} {
	error "hyde: jclass: '-access $accessType' invalid, must be one of: [join $types]"
    }

    if {[string length $includeStr]} {
        set body $includeStr
    } elseif {[string length $includeFile]} {
	if { [catch {set fd [open $includeFile]} ] } {
	    error "hyde: jclass: -include file '$includeFile' could not be opened for reading"
	} else {
	    set body [read $fd]
	    close $fd
	}
    } else {
	if {![string length [append body [lindex $args 0]]] && [llength $propertyList] == 0} {
	    error "hyde: jclass: must include one of '-source sourcetext', '-include file', '-properties proplist' or '-body body'"
	}
    }

    set codeStr ""
    if {[string length $packageName]} {
	append codeStr "package $packageName;" \n
	set name [string map {. /} $packageName/$name]
	set className $packageName.$className
	chkCompileDir
	if {! [file isdirectory [file join $compileDir [string map {. /} $packageName]]]} {
	    if {$debug} {
		puts stderr "jclass: mkdir [file join $compileDir [string map {. /} $packageName]]"
	    }
	    file mkdir [file join $compileDir [string map {. /} $packageName]]
	}
    }

    foreach importPkg $importList {
	append codeStr "import $importPkg;" \n\n
    }

    append codeStr "$accessType class $cname $extendsClass \n\t\
		$implementsList \{ " \n

    # generate properties, and associated getters/setters
    array set propType {}
    foreach typeProperty $propertyList {
	set len [llength $typeProperty]
	if {$len < 2 || $len > 3} {
	    error "hyde: jclass: property '$typeProperty' must be a list of 'type name ?initvalue?'"
	}
	set type [lindex $typeProperty 0]
	set prop [lindex $typeProperty 1]
	set init [lindex $typeProperty 2]
	set propType($prop) $type
	append codeStr \t "private $type $prop "
	if {$len == 3} {
	    append codeStr "= " $init
	}
	append codeStr ";\n"

	set propMeth [string toupper [string range $prop 0 0]]
	append propMeth [string range $prop 1 end]

	append codeStr \t "public $type get$propMeth () { " \n
	append codeStr \t\t "return this.$prop ; " \n "\t}\n\n"

	append codeStr \t "public $type get_$prop () { " \n
	append codeStr \t\t "return this.$prop ; " \n "\t}\n\n"

	append codeStr \t "public void set$propMeth ($type $prop) { " \n
	append codeStr \t\t "this.$prop = $prop ; " \n "\t}\n\n"

	append codeStr \t "public void set_$prop ($type $prop) { " \n
	append codeStr \t\t "this.$prop = $prop ; " \n "\t}\n\n"

    }

    # generate hashCode method
    set hashMethod {}
    if {$hashEqualsList eq "*"} {
	set hashEqualsList ""
	foreach typeProperty $propertyList {
	    lappend hashEqualsList [lindex $typeProperty 1]
	}
    }
    set hasArrays 0
    foreach prop $hashEqualsList {
	if {! [info exists propType($prop)]} {
	    error "-hashequals element \"$prop\" not defined as a -property"
	}
	switch $propType($prop) {
	    boolean {append hashMethod "\t\tresult = prime * result + (this.$prop ? 1231 : 1237);" \n}
	    byte    {append hashMethod "\t\tresult = prime * result + this.$prop;" \n}
	    int     {append hashMethod "\t\tresult = prime * result + this.$prop;" \n}
	    short   {append hashMethod "\t\tresult = prime * result + this.$prop;" \n}
	    long    {append hashMethod "\t\tresult = prime * result + (int) (this.$prop ^ (this.$prop >>> 32));" \n}
	    char    {append hashMethod "\t\tresult = prime * result + this.$prop;" \n}
	    float   {append hashMethod "\t\tresult = prime * result + Float.floatToIntBits(this.$prop);" \n}
	    double  {append hashMethod "\t\ttemp = Double.doubleToLongBits(this.$prop); result = prime * result + (int) (temp ^ (temp >>> 32));" \n}
	    default {
		if {[string first \[ $propType($prop)] != -1} {
		    append hashMethod "\t\tresult = prime * result + $cname.hashCode(this.$prop);" \n
		    set hasArrays 1
		} else {
		    append hashMethod "\t\tresult = prime * result + ((this.$prop == null) ? 0 : this.$prop.hashCode());" \n
		}
	    }
	}
    }
    if {[string length $hashMethod]} {
	append codeStr "\n\tpublic int hashCode() {\n"
	append codeStr "\t\tfinal int prime = 31;\n\t\tint result = 1;\n\t\tlong temp;\n"
	append codeStr "$hashMethod\n"
	append codeStr "\t\treturn result;\n"
	append codeStr "\t}\n\n"

	if {$hasArrays} {
	    append codeStr $arraysHashcode
	}
    }

    # generate equals method
    set eqMethod {}
    foreach prop $hashEqualsList {
	if {! [info exists propType($prop)]} {
	    error "-hashequals element \"$prop\" not defined as a -property"
	}
	switch $propType($prop) {
	    boolean {append eqMethod "\t\tif (this.$prop != other.$prop)\n\t\t\treturn false;" \n}
	    byte    {append eqMethod "\t\tif (this.$prop != other.$prop)\n\t\t\treturn false;" \n}
	    int     {append eqMethod "\t\tif (this.$prop != other.$prop)\n\t\t\treturn false;" \n}
	    short   {append eqMethod "\t\tif (this.$prop != other.$prop)\n\t\t\treturn false;" \n}
	    long    {append eqMethod "\t\tif (this.$prop != other.$prop)\n\t\t\treturn false;" \n}
	    char    {append eqMethod "\t\tif (this.$prop != other.$prop)\n\t\t\treturn false;" \n}
	    float   {append eqMethod "\t\tif (Float.floatToIntBits(this.$prop) != Float.floatToIntBits(other.$prop))\n\t\t\treturn false;" \n}
	    double  {append eqMethod "\t\tif (Double.doubleToLongBits(this.$prop) != Double.doubleToLongBits(other.$prop))\n\t\t\treturn false;" \n}
	    default {
		if {[string first \[ $propType($prop)] != -1} {
		    append eqMethod "\t\tif (!java.util.Arrays.equals(this.$prop, other.$prop))\n\t\t\treturn false;" \n
		} else {
		    append eqMethod "\t\tif (this.$prop == null) {\n\t\t\tif (other.$prop != null)\n\t\t\t\treturn false;\n\t\t} else if (!this.$prop.equals(other.$prop))\n\t\t\treturn false;" \n
		}
	    }
	}
    }
    if {[string length $eqMethod]} {
	append codeStr "\n\tpublic boolean equals (Object obj) {\n"
	append codeStr "\t\tif (this == obj)\n\t\t\treturn true;\n"
	append codeStr "\t\tif (obj == null)\n\t\t\treturn false;\n"
	append codeStr "\t\tif (getClass() != obj.getClass())\n\t\t\treturn false;\n"
	append codeStr "\t\tfinal $cname other = ($cname) obj;\n"
	append codeStr "$eqMethod\n"
	append codeStr "\t\treturn true;\n"
	append codeStr "\t}\n\n"
    }

    # generate toString
    set toStringMethod {}
    set toStringListMethod {}
    set toObjListMethod {}
    set toObjArrayMethod {}
    if {$toStringList eq "*"} {
	set toStringList ""
	foreach typeProperty $propertyList {
	    lappend toStringList [lindex $typeProperty 1]
	}
    }

    set primList [list boolean byte char short int long float double]
    set wrappedList [list Boolean Byte Character Short Integer Long Float Double String \
	java.lang.Boolean java.lang.Byte java.lang.Character java.lang.Short java.lang.Integer \
	java.lang.Long java.lang.Float java.lang.Double java.lang.String]
    set cont "   "
    set listcont "\""
    foreach prop $toStringList {
	append toStringMethod $cont "\"$prop=\" + " $prop
	set cont " + \", \" \n\t\t\t + "

	if {[lsearch $primList $propType($prop)] >= 0 } {
	    append toStringListMethod $listcont "$prop \" + quoteForTclBareWord(\"\"+$prop)"
	} else {
	    append toStringListMethod $listcont "$prop \" + quoteForTclBareWord($prop==null? \"java0x0\":\"\"+$prop)"
	}
	set listcont "   \n\t\t\t + \" "

	append toObjListMethod "\t\ttcl.lang.TclList.append(interp, list, tcl.lang.TclString.newInstance(\"$prop\"));\n"
	if {[lsearch $primList $propType($prop)] >= 0 } {
	    append toObjListMethod "\t\ttcl.lang.TclList.append(interp, list, tcl.lang.TclString.newInstance(\"\"+$prop));\n"
	} elseif {[lsearch $wrappedList $propType($prop)] >= 0 } {
	    append toObjListMethod "\t\tif ($prop == null) {\n"
	    append toObjListMethod "\t\t\ttcl.lang.TclList.append(interp, list, tcl.lang.TclString.newInstance(\"java0x0\"));\n"
	    append toObjListMethod "\t\t} else {\n"
	    append toObjListMethod "\t\t\ttcl.lang.TclList.append(interp, list, tcl.lang.TclString.newInstance(\"\"+$prop));\n"
	    append toObjListMethod "\t\t}\n"
	} else {
	    append toObjListMethod "\t\tif ($prop == null) {\n"
	    append toObjListMethod "\t\t\ttcl.lang.TclList.append(interp, list, tcl.lang.TclString.newInstance(\"java0x0\"));\n"
	    append toObjListMethod "\t\t} else {\n"
	    append toObjListMethod "\t\t\ttcl.lang.TclList.append(interp, list, tcl.pkg.java.ReflectObject.newInstance(interp, $prop.getClass(), $prop));\n"
	    append toObjListMethod "\t\t}\n"
	}

	if {[lsearch $primList $propType($prop)] >= 0 } {
	    append toObjArrayMethod "\t\tinterp.setVar(arrName, \"$prop\", tcl.lang.TclString.newInstance(\"\"+$prop), 0);\n"
	} elseif {[lsearch $wrappedList $propType($prop)] >= 0 } {
	    append toObjArrayMethod "\t\tif ($prop == null) {\n"
	    append toObjArrayMethod "\t\t\tinterp.setVar(arrName, \"$prop\", tcl.lang.TclString.newInstance(\"java0x0\"), 0);\n"
	    append toObjArrayMethod "\t\t} else {\n"
	    append toObjArrayMethod "\t\t\tinterp.setVar(arrName, \"$prop\", tcl.lang.TclString.newInstance(\"\"+$prop), 0);\n"
	    append toObjArrayMethod "\t\t}\n"
	} else {
	    append toObjArrayMethod "\t\tif ($prop == null) {\n"
	    append toObjArrayMethod "\t\t\tinterp.setVar(arrName, \"$prop\", tcl.lang.TclString.newInstance(\"java0x0\"), 0);\n"
	    append toObjArrayMethod "\t\t} else {\n"
	    append toObjArrayMethod "\t\t\tinterp.setVar(arrName, \"$prop\", tcl.pkg.java.ReflectObject.newInstance(interp, $prop.getClass(), $prop), 0);\n"
	    append toObjArrayMethod "\t\t}\n"
	}

    }

    if {[string length $toStringMethod]} {
	append codeStr "\n\n\tpublic String toString() {\n"
	append codeStr "\t\treturn\n\t\t\t $toStringMethod;\n"
	append codeStr "\t}\n\n"

	append codeStr "\n\n\tpublic String toStringList() {\n"
	append codeStr "\t\treturn\n\t\t\t $toStringListMethod;\n"
	append codeStr "\t}\n\n"

	append codeStr "\n\n\tpublic void toList(tcl.lang.Interp interp, String varName) throws tcl.lang.TclException {\n"
	append codeStr "\t\ttcl.lang.TclObject list = tcl.lang.TclList.newInstance();\n"
	append codeStr "\t\tlist.preserve();\n"
	append codeStr "$toObjListMethod\n"
	append codeStr "\t\tinterp.setVar(varName, list, 0);\n"
	append codeStr "\t}\n\n"

	append codeStr "\n\n\tpublic void toArray(tcl.lang.Interp interp, String arrName) throws tcl.lang.TclException {\n"
	append codeStr "$toObjArrayMethod\n"
	append codeStr "\t}\n\n"

	append codeStr {
        private String quoteForTclBareWord(final String s) {
                StringBuffer buf = new StringBuffer();
                char[] chars = s.toCharArray();
                for (int i = 0; i < chars.length; i++) {
                        char c = chars[i];
                        if (c == '{' || c == '}' || c == '"' || c == '[' || c == ']' || c == '\\' ||
                                        c == '$' || c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f') {
                                buf.append('\\');
                        }
                        buf.append(c);

                }
                return buf.toString();
        }
	    }
    }

    # if -source or -include a file, use it as codeStr
    if {[string length $includeStr]} {
	set codeStr $includeStr
    } elseif {[string length $includeFile]} {
	set codeStr $body
    } else {
	append codeStr \n$body\n\n \}
    }

    # check if cached bytecode exists

    if {! [string length [set byteCodeList [readcache $name]]]} {

	compile $name $codeStr 1
	set byteCodeList [readcache $name 1]

    }

    # send bytecode to jvm, and cause new class to be loaded by instantiating
    # a new object, then garbage collect the newly created object
    set defobj [defineclass $byteCodeList]
    if {[java::isnull $defobj]} {
	error "hyde: jclass: defineclass failed for $name"
    }

    return ""
}

################################################################################
#
# export public procs
#

namespace eval hyde {namespace export jproc jcommand jclass configure rmcompiledir setFromList setFromArray}

################################################################################
#
# janinocp - compile hydeinternal.JaninoCP when requested.
#

if {$argv eq "-compilejaninocp-"} {

    # find that janino jar, same location as tcljava.jar or jacl.jar
    foreach path [split $env(CLASSPATH)  $env(path.separator)] {
	set tail [file tail $path]
	if {[string match jtcl*.jar [string tolower $tail]]} {
	    set dir [file dirname $path]
	    lappend env(TCL_CLASSPATH) [file join $dir $tail]
	    append env(CLASSPATH) $env(path.separator) [file join $dir $tail] $env(path.separator)
	    break
	}
    }

    #hyde::configure -writecache 0
    hyde::configure -compiler javac
    hyde::configure -debug 1
    hyde::configure -cachefile janinocp_cache.tcl

    hyde::jclass JaninoCP -package hydeinternal -import {
        org.codehaus.janino.*
        org.codehaus.janino.util.*
        org.codehaus.janino.util.enumerator.*
        org.codehaus.janino.util.resource.*
        java.io.*
    } -body {

        private IClassLoader icloader = null;
        private String lastError;
    
        public JaninoCP(ClassLoader cpClassLoader) {
    
            String classPath = System.getProperty("java.class.path");
            icloader = createJavacLikePathIClassLoader(
                cpClassLoader,
                null, // optionalBootClassPath
                null, // optionalExtDirs
                PathResourceFinder.parsePath(classPath)
            );
        }
    
        public ClassFile[] compile(String javasrc) {
            lastError = "";
            try {
                StringReader sreader = new StringReader(javasrc);
                Scanner scanner = new Scanner(null, sreader);
                Parser parser = new Parser(scanner);
                Java.CompilationUnit cunit = parser.parseCompilationUnit();
                UnitCompiler ucompiler = new UnitCompiler(cunit, icloader);
                EnumeratorSet defaultDebug = DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION;
                ClassFile[] cfiles = ucompiler.compileUnit(defaultDebug);
                return cfiles;
            } catch (Exception e) {
                lastError = e.getMessage();
                return null;
            }
        }
    
        public String getError() {
            if (lastError == null) {
                return "compile() has not yet been run.";
            }
            return lastError;
        }
    
    
        private IClassLoader createJavacLikePathIClassLoader(
                    ClassLoader cpClassLoader,
                    final File[] optionalBootClassPath,
                    final File[] optionalExtDirs,
                    final File[] classPath) {
    
            IClassLoader icl = null;
    
            ResourceFinder bootClassPathResourceFinder = new PathResourceFinder(
                optionalBootClassPath == null ?
                PathResourceFinder.parsePath(System.getProperty("sun.boot.class.path")):
                optionalBootClassPath
            );
            ResourceFinder extensionDirectoriesResourceFinder = new JarDirectoriesResourceFinder(
                optionalExtDirs == null ?
                PathResourceFinder.parsePath(System.getProperty("java.ext.dirs")):
                optionalExtDirs
            );
            ResourceFinder classPathResourceFinder = new PathResourceFinder(classPath);
    
            if (cpClassLoader != null) {
                icl = new ClassLoaderIClassLoader(cpClassLoader);
            }
            icl = new ResourceFinderIClassLoader(bootClassPathResourceFinder, icl);
            icl = new ResourceFinderIClassLoader(extensionDirectoriesResourceFinder, icl);
            icl = new ResourceFinderIClassLoader(classPathResourceFinder, icl);
    
    	    return icl;
    
        }
    }
    # end of hyde::jclass

    if {! [ info exists ::hyde::cacheCode(hydeinternal/JaninoCP)  ]} {
	error "JaninoCP didn't compile"
	exit
    }

    ################################
    # insert the code into hyde.tcl
    namespace eval hyde {

	file copy hyde.tcl hyde.tcl.[clock seconds]
	set fd [open hyde.tcl]
	set hydetcl [read $fd]
	close $fd

	append startmark "#" "AUTOGENERATED" "#"
	append endmark   "#" "AUTOGENERATEDEND" "#"

	set fd [open janinocp_cache.tcl]
	set byteHex [read $fd]
	close $fd

	set binidx [string first \[binary $byteHex]
	set byteHex [string range $byteHex $binidx end]
	set binidx [string first \] $byteHex]
	set byteHex "[string range $byteHex 0 $binidx]"

	set code "\nset ::hyde::janinocp_code $byteHex\n\n"

	set startidx [string first $startmark $hydetcl]
	set endidx   [string first $endmark   $hydetcl]
	if {$startidx == -1 || $endidx == -1} {
	    error "couldn't find $startmark and/or $endmark in hyde.tcl"
	    exit
	}
	incr startidx [string length $startmark]
	set firstpart [string range $hydetcl 0 $startidx]
	set lastpart  [string range $hydetcl $endidx end]

	set hydetcl "$firstpart$code$lastpart"

	set fd [open hyde.tcl w]
	puts -nonewline $fd $hydetcl
	close $fd

    }
    #namespace eval hyde

}
# end of if -compilejaninocp-

# DO NOT MESS WITH THE FOLLOWING COMMENTS OR CODE:
#AUTOGENERATED#

set ::hyde::janinocp_code [binary format H* cafebabe0000002e00700a0022003309002100340800350a003600370a001700380a0021003908003a090021003b07003c0a0009003d07003e0a000b003f0700400a000d00410a000d00420700430a0010004409004500460a001000470700480a0014004908004a07004b08004c0a0017004d07004e08004f0a001a004d0700500a001d00510700520a001f005307005407005501000869636c6f616465720100224c6f72672f636f6465686175732f6a616e696e6f2f49436c6173734c6f616465723b0100096c6173744572726f720100124c6a6176612f6c616e672f537472696e673b0100063c696e69743e01001a284c6a6176612f6c616e672f436c6173734c6f616465723b2956010004436f646501000f4c696e654e756d6265725461626c65010007636f6d70696c65010039284c6a6176612f6c616e672f537472696e673b295b4c6f72672f636f6465686175732f6a616e696e6f2f7574696c2f436c61737346696c653b0100086765744572726f7201001428294c6a6176612f6c616e672f537472696e673b01001f6372656174654a617661634c696b655061746849436c6173734c6f61646572010068284c6a6176612f6c616e672f436c6173734c6f616465723b5b4c6a6176612f696f2f46696c653b5b4c6a6176612f696f2f46696c653b5b4c6a6176612f696f2f46696c653b294c6f72672f636f6465686175732f6a616e696e6f2f49436c6173734c6f616465723b01000a536f7572636546696c6501000d4a616e696e6f43502e6a6176610c002700560c0023002401000f6a6176612e636c6173732e706174680700570c005800590c005a005b0c002f00300100000c002500260100146a6176612f696f2f537472696e675265616465720c0027005c01001b6f72672f636f6465686175732f6a616e696e6f2f5363616e6e65720c0027005d01001a6f72672f636f6465686175732f6a616e696e6f2f5061727365720c0027005e0c005f00630100206f72672f636f6465686175732f6a616e696e6f2f556e6974436f6d70696c65720c002700640700650c006600670c006800690100136a6176612f6c616e672f457863657074696f6e0c006a002e01001f636f6d70696c65282920686173206e6f7420796574206265656e2072756e2e0100346f72672f636f6465686175732f6a616e696e6f2f7574696c2f7265736f757263652f506174685265736f7572636546696e64657201001373756e2e626f6f742e636c6173732e706174680c0027006b01003e6f72672f636f6465686175732f6a616e696e6f2f7574696c2f7265736f757263652f4a61724469726563746f726965735265736f7572636546696e64657201000d6a6176612e6578742e6469727301002b6f72672f636f6465686175732f6a616e696e6f2f436c6173734c6f6164657249436c6173734c6f616465720c0027002801002e6f72672f636f6465686175732f6a616e696e6f2f5265736f7572636546696e64657249436c6173734c6f616465720c0027006c01001568796465696e7465726e616c2f4a616e696e6f43500100106a6176612f6c616e672f4f626a6563740100032829560100106a6176612f6c616e672f53797374656d01000b67657450726f7065727479010026284c6a6176612f6c616e672f537472696e673b294c6a6176612f6c616e672f537472696e673b010009706172736550617468010023284c6a6176612f6c616e672f537472696e673b295b4c6a6176612f696f2f46696c653b010015284c6a6176612f6c616e672f537472696e673b2956010025284c6a6176612f6c616e672f537472696e673b4c6a6176612f696f2f5265616465723b2956010020284c6f72672f636f6465686175732f6a616e696e6f2f5363616e6e65723b29560100147061727365436f6d70696c6174696f6e556e697407006e01000f436f6d70696c6174696f6e556e697401000c496e6e6572436c617373657301002c28294c6f72672f636f6465686175732f6a616e696e6f2f4a61766124436f6d70696c6174696f6e556e69743b01004f284c6f72672f636f6465686175732f6a616e696e6f2f4a61766124436f6d70696c6174696f6e556e69743b4c6f72672f636f6465686175732f6a616e696e6f2f49436c6173734c6f616465723b29560100286f72672f636f6465686175732f6a616e696e6f2f446562756767696e67496e666f726d6174696f6e01001d44454641554c545f444542554747494e475f494e464f524d4154494f4e0100334c6f72672f636f6465686175732f6a616e696e6f2f7574696c2f656e756d657261746f722f456e756d657261746f725365743b01000b636f6d70696c65556e697401005a284c6f72672f636f6465686175732f6a616e696e6f2f7574696c2f656e756d657261746f722f456e756d657261746f725365743b295b4c6f72672f636f6465686175732f6a616e696e6f2f7574696c2f436c61737346696c653b01000a6765744d657373616765010012285b4c6a6176612f696f2f46696c653b2956010057284c6f72672f636f6465686175732f6a616e696e6f2f7574696c2f7265736f757263652f5265736f7572636546696e6465723b4c6f72672f636f6465686175732f6a616e696e6f2f49436c6173734c6f616465723b295607006f0100286f72672f636f6465686175732f6a616e696e6f2f4a61766124436f6d70696c6174696f6e556e69740100186f72672f636f6465686175732f6a616e696e6f2f4a617661002100210022000000020002002300240000000200250026000000040001002700280001002900000047000600030000001f2ab700012a01b500021203b800044d2a2a2b01012cb80005b70006b50002b100000001002a000000160005000000140004001100090016000f0017001e001d0001002b002c00010029000000a100040009000000552a1207b50008bb0009592bb7000a4dbb000b59012cb7000c4ebb000d592db7000e3a041904b6000f3a05bb00105919052ab40002b700113a06b200123a0719061907b600133a081908b04d2a2cb60015b5000801b0000100060049004a00140001002a00000032000c0000002000060022000f00230019002400230025002a002600390027003e002800470029004a002a004b002b0053002c0001002d002e000100290000002f000100010000000f2ab40008c700061216b02ab40008b000000001002a0000000e00030000003100070032000a00340002002f003000010029000000b40004000900000078013a05bb0017592cc7000e1218b80004b80005a700042cb700193a06bb001a592dc7000e121bb80004b80005a700042db7001c3a07bb0017591904b700193a082bc6000dbb001d592bb7001e3a05bb001f5919061905b700203a05bb001f5919071905b700203a05bb001f5919081905b700203a051905b000000001002a0000002a000a0000003e00030040001c00450035004a0040004c0044004d004e004f005b005000680051007500530002003100000002003200620000000a00010060006d00610019]

#AUTOGENERATEDEND#
# DO NOT MESS WITH THE ABOVE COMMENTS OR CODE:

# dummy command, so that if we source this file in an interactive interp,
# we won't get the bytecodes.
format ""

# vim: ts=4 sw=4

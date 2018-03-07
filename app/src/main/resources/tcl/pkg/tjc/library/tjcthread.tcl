#
#  Copyright (c) 2005 Mo DeJong
#
#  See the file "license.txt" for information on usage and
#  redistribution of this file, and for a DISCLAIMER OF ALL
#  WARRANTIES.

# These methods are used by the TJCThread runtime compiler
# implementation. They are invoked from a second thread
# that is only used to convert Tcl proc into Java source
# code and then to byte code.

package require TJC
package require java

set JAVA_INIT 0
set JAVA_CMD ""
if {![info exists JAVA_DRIVER]} {
    set JAVA_DRIVER ""
}
set JAVA_HOLD_REF ""

#puts "sourced tjcthread.tcl in second thread"

proc processJavaSource { filename javasrc } {
    global JAVA_INIT JAVA_CMD

#    puts "processJavaSource: $filename \"$javasrc\""

    if {!$JAVA_INIT} {
        initJavaCompiler
    }

    return [$JAVA_CMD $filename $javasrc]
}

proc initJavaCompiler {} {
    global _tjcthread
    global env
    global JAVA_INIT JAVA_CMD JAVA_DRIVER JAVA_HOLD_REF

    #puts "initJavaCompiler"

    set drivers {}
    set jar_file ""

    if {$JAVA_DRIVER == ""} {
        # Search through possible compiler implementations
        lappend drivers janino pizza
    } elseif {$JAVA_DRIVER == "pizza"} {
        lappend drivers pizza
    } elseif {$JAVA_DRIVER == "janino"} {
        lappend drivers janino
    }

    #puts "drivers is \{$drivers\}"

    # Look for compiler driver Jar in the same directory
    # where tcljava.jar lives.

    if {$::tcl_platform(host_platform) == "windows"} {
        set sep \;
    } else {
        set sep :
    }
    set jardir ""
    foreach path [split $::env(CLASSPATH) $sep] {
        if {$path == {}} {
            continue
        }
        if {[string match jtcl* [file tail $path]]} {
            set jardir [file dirname $path]
            set jar_file $path
         } elseif {[string match swank* [file tail $path]]} {
            set jardir [file dirname $path]
            set jar_file $path
        }
    }
    if {$jardir == ""} {
        error "could not locate jtcl/swank.jar on CLASSPATH \"$env(CLASSPATH)\""
    }

    foreach driver $drivers {
        if {$driver == "pizza"} {
            # Look for pizza compiler Jar
            set file [file join $jardir "pizza-1.1.jar"]
            if {[file exists $file]} {
                set jar_file $file
                set test_class net.sf.pizzacompiler.compiler.Main
                set test_ctor net.sf.pizzacompiler.compiler.Main
                set java_cmd pizzaCompile
                break
            }
        } elseif {$driver == "janino"} {
            # janino compiler builtin
                set test_class org.codehaus.janino.SimpleCompiler
                set test_ctor {org.codehaus.janino.SimpleCompiler boolean}
                set java_cmd janinoCompile
                break
        }
    }

    if {$jar_file == ""} {
        error "no working compiler driver found"
    }

    #puts "Using Compiler Jar file $jar_file"

    # Define runtime search path for TclClassLoader

    set env(TCL_CLASSPATH) $jar_file

    # Make sure the class was loaded from the jar file.

    if {[catch {java::field $test_class class} err]} {
        error "could not load compiler driver test class \"$test_class\", problem with TCL_CLASSPATH ?"
    }

    # hold a reflected Java object handle to avoid
    # having to reload classes because the garbage
    # collector unloaded compiler classes.

    if {$driver == "janino"} {
        # Hold ref to janino SimpleCompiler instance
        set JAVA_HOLD_REF [java::new $test_ctor 1]
    } else {
        # Hold ref to pizza class
        set JAVA_HOLD_REF $err
    }

    set JAVA_INIT 1
    set JAVA_CMD $java_cmd

    return
}

# Use the pizza compiler to convert Java source code to
# class files.

proc pizzaCompile { filename srccode } {
    global JAVA_INIT
    global env

    if {!$JAVA_INIT} {
        error "JAVA_INIT not set, initJavaCompiler must have been invoked"
    }

    if {$filename == ""} {
        error "empty string passed as filename"
    }

    set pizza net.sf.pizzacompiler.compiler

    set util  java.util
    set io    java.io
    set tcl   tcl.lang

    set sourceHash [java::new $util.HashMap]
    $sourceHash put $filename $srccode
    set compilerSource [java::new $pizza.MapSourceReader $sourceHash]

    # set up compiler bytecode output
    set compilerOutput [java::new $pizza.ByteArrayCompilerOutput]

    # set up compiler print output
    set print [java::new $io.PrintStream [java::new $io.ByteArrayOutputStream]]

# FIXME: We should not need to init the compiler every time, set an
# init flag to save these settings.
    # invoke compiler
    java::call $pizza.Main init
    java::call $pizza.Main argument -pizza
    java::call $pizza.Main argument -nowarn
    java::call $pizza.Main setClassReader [java::new \
        $pizza.ClassReader $env(CLASSPATH) [java::null]]
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

if {0} {
# Pizza 1.1 does not include the method
# ByteArrayCompilerOutput.getFilesToBytecode(), it is the
# only way to get the names of the compiled class files.
# Just pass the empty string as the class file name for now.

    # Get Map of class name to byte[]

    set map [$compilerOutput getFilesToBytecode]

    set iter [[$map entrySet] iterator]

    set results [list]
    set inner [list]

    for {set iter [[$map entrySet] iterator]} {[$iter hasNext]} {} {
        set entry [java::cast {java.util.Map.Entry} [$iter next]]
        set key [java::cast {String} [$entry getKey]]
        set value [java::cast {byte[]} [$entry getValue]]

        # Check for inner classes

        if {[string first "\$" $key] == -1} {
            lappend results $key $value
        } else {
            lappend inner $key $value
        }
    }

    if {$inner != {}} {
        # Sort inner classes by class name and append to results

        foreach {cname cdata} $inner {
            set cnames($cname) $cdata
        }
        set sorted_cnames [lsort -dictionary [array names cnames]]
        foreach cname $sorted_cnames {
            lappend results $cname $cnames($cname)
        }
    }

    return $results   
}

if {1} {
    # Get the bytecode from the compiler output object, an ArrayList.
    # The pizza compile does not know the class names here, it only
    # returns the byte arrays.
    set byteArrayList [java::cast $util.ArrayList [$compilerOutput getBytecode]]

    for {set i 0} {$i < [$byteArrayList size]} {incr i} {
        set class_name {}

        # If a single class was compiled, then get the fully
        # qualified class name from the file name. This file
        # name is fully qualified in terms of the Java package
        # that the class is in, but it not an absolute path
        # for a file on the filsystem.

        if {[$byteArrayList size] == 1} {
            set class_name [string map {.java "" / .} $filename]
        }

        set class_bytes [java::cast {byte[]} [$byteArrayList get $i]]

        lappend results $class_name $class_bytes
    }

    # return list of CLASSNAME CLASSDATA elements where CLASSDATA
    # if a reflected byte[] Java object.

    return $results
}

}

# The janinoCompile command will invoke internal janino compiler
# routines to generate class file data from Java source code
# contained in a String. This implementation will not load
# class data into the current interp, the class data is sent
# back to the original interp and is loaded there.

proc janinoCompile { filename srccode } {
    global JAVA_INIT
    global JAVA_HOLD_REF

    if {!$JAVA_INIT} {
        error "JAVA_INIT not set, initJavaCompiler must have been invoked"
    }

    if {$filename == ""} {
        error "empty string passed as filename"
    }

    # The JAVA_HOLD_REF ref is a SimpleCompiler instance when
    # compiling with Janino, invoke SimpleCompiler.compile(String)
    # to compile Java source code and get a ClassFile[] array result.

    set cfiles [$JAVA_HOLD_REF compile $srccode]

    # return list of CLASSNAME CLASSDATA elements where CLASSDATA
    # if a reflected byte[] Java object. Make sure that inner
    # classes are listed after the main class.

    set results [list]
    set inner [list]
    set len [$cfiles length]

    for {set i 0} {$i < $len} {incr i} {
        set cfile [$cfiles get $i]

        set class_name [$cfile getThisClassName]
        set class_bytes [$cfile toByteArray]

        # Check for inner classes

        if {[string first "\$" $class_name] == -1} {
            lappend results $class_name $class_bytes
        } else {
            lappend inner $class_name $class_bytes
        }
    }

    if {$inner != {}} {
        # Sort inner classes by class name and append to results

        foreach {cname cdata} $inner {
            set cnames($cname) $cdata
        }
        set sorted_cnames [lsort -dictionary [array names cnames]]
        foreach cname $sorted_cnames {
            lappend results $cname $cnames($cname)
        }
    }

    return $results
}

# Process Tcl source code and generate Java source
# code that implements the Tcl command. This method
# returns a list of {PROCNAME JAVASOURCE}.

proc processTclSource { java_filename proc_source } {
    global _tjc

    #puts "processTclSource $java_filename $proc_source"

    set len [llength $proc_source]
    if {$len != 4} {
        error "Tcl proc decl should have 4 arguments: got \{$proc_source\}"
    }
    set proc_name [lindex $proc_source 1]

    # Make sure TJC commands needed for the embedded version
    # of the compiler code have been loaded into this interp.

    if {![info exists _tjc(embedded)]} {
        # Load init code in the global namespace
        namespace eval :: {
           package require parser
           set _tjc(embedded) 1
           if {[catch {source resource:/tjc/library/reload.tcl}]} {
               source resource:/tcl/pkg/tjc/library/reload.tcl
           }

        }

        # Init the module code so that it knows
        # the Java package name and that it should
        # enable all optimizations. Don't validate
        # options since we only define the bare min
        # options needed to enable embedded compilation.

        module_parse {
PACKAGE tjcthread
OPTIONS +O
        }
    }

    # Pass name of Java file instead of Tcl file, this
    # would only appear in an error message.

    set proc_filename $java_filename

    set cname [file root [file tail $java_filename]]

    # Note that we don't try to check proc args, just
    # assume that the caller constructed valid args
    # from the proc declaration.

    set proc_decl_tuple $proc_source
    set proc_tuple [list $proc_name $cname $proc_decl_tuple]

    set tuple [compileproc_entry_point $proc_filename $proc_tuple]

    if {$tuple == "ERROR"} {
        # Error caught in compileproc_entry_point.
        error "compileproc_entry_point error"
    }

    # Write generated code for proc to Java file

    set java_source [lindex $tuple 3]

    return [list $proc_name $java_source]
}


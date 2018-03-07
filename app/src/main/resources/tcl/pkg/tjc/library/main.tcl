#
#  Copyright (c) 2005 Advanced Micro Devices, Inc.
#
#  See the file "license.amd" for information on usage and
#  redistribution of this file, and for a DISCLAIMER OF ALL
#   WARRANTIES.
#
#  RCS: @(#) $Id: main.tcl,v 1.5 2006/03/04 22:18:34 mdejong Exp $
#
#
proc checkStatus {tclFiles jarFile} {
    set maxTime 0
    foreach file $tclFiles {
        set mTime [file mtime $file]
        if {$mTime > $maxTime} {
            set maxTime $mTime
        }
    }
    set jarTime 0
    if {[file exists $jarFile]} {
        set jarTime [file mtime $jarFile]
    }
    set status 1
    if {$jarTime < $maxTime} {
        puts "Need to regenerate $jarFile"
        set status 0
    }
    return $status
}
# Process list of command line argments and assign
# values to the _cmdline array.

proc process_cmdline { argv } {
    global _cmdline

    set files [list]
    set options [list]

    foreach arg $argv {
        if {[string match *.tjc $arg]} {
            lappend files $arg
        } else {
            lappend options $arg
        }
    }

    set _cmdline(files) $files
    set _cmdline(options) $options

    return $options
}

# Validate command line options passed to tjc executable.

proc validate_options {} {
    global _tjc
    global _cmdline

    if {![info exists _tjc(debug)]} {
        set _tjc(debug) 0
    }
    if {![info exists _tjc(progress)]} {
        set _tjc(progress) 0
    }
    if {![info exists _tjc(compiler)]} {
        set _tjc(compiler) "javac"
    }

    foreach option $_cmdline(options) {
        # If option string does not start with a - character
        # then it is not a valid option.
        if {[string match *.tcl $option]} {
            error "Tcl source file $option is not a valid argument, pass TJC module file"
        } elseif {[string index $option 0] == "-"} {
            # Valid option
            switch -- $option {
                "-debug" {
                    # Print debug info for each method
                    set _tjc(debug) 1
                }
                "-nocompile" {
                    # Don't invoke javac, just emit code and exit
                    set _tjc(nocompile) 1
                }
                "-progress" {
                    # Don't invoke javac, just emit code and exit
                    set _tjc(progress) 1
                }
                "-javac" {
                    set _tjc(compiler) javac
                }
                "-pizza" {
                    set _tjc(compiler) pizza
                }
                "-janino" {
                    set _tjc(compiler) janino
                }
            }
        } else {
            error "option $option is invalid"
        }
    }
}

# Process jdk config values defined in jdk.cfg
# in root/jdk.cfg.

proc process_jdk_config {} {
    global _tjc env _jdk_config

    set debug 1
    if {$_tjc(debug)} {
        set debug 1
    }

    # Support for running tjc executable from build dir
    if {[info exists env(TJC_LIBRARY)] && \
            [info exists env(TJC_BUILD_DIR)]} {
        set jdk_cfg $env(TJC_BUILD_DIR)/jdk.cfg
    } else {
        set jdk_cfg [file join $_tjc(root) jdk.cfg]
    }
    set _jdk_config(CLASSPATH) $env(CLASSPATH)

    # java.home might be the jdk, or a jre inside the jdk, try both
    # fallback to those on the PATH
    set _jdk_config(JAVAC) javac
    set _jdk_config(JAR) jar
    foreach dir [list [file join $env(java.home) bin javac] [file join $env(java.home) .. bin javac]] {
	if {[file readable [file join $dir javac]]} {
            set _jdk_config(JAVAC)  [file join $dir javac]
	}
	if {[file readable [file join $dir jar]]} {
            set _jdk_config(JAR)  [file join $dir jar]
	}
    }

    if {[file exists $jdk_cfg]} {
        if {$debug || $_tjc(progress)} {
            puts "loading $jdk_cfg"
        }

        set res [jdk_config_parse_file $jdk_cfg]
        if {[lindex $res 0] == 0} {
            puts stderr "Error loading $jdk_cfg : [lindex $res 1]"
            return -1
        }
    }
    # JDK config values are now validated, they
    # can be quiried via jdk_config_var.
    # Set CLASSPATH now that it is validated
    set ::env(CLASSPATH) [jdk_config_var CLASSPATH]
    return 0
}

# Test out each jdk tool to make sure it actually works.

proc check_jdk_config {} {
    global _tjc

    set debug 0
    if {$_tjc(debug)} {
        set debug 1
    }

    if {$debug} {
        puts "testing jdk tools"
    }

    set fname [jdk_tool_javac_save Test {
public class Test {}
    }]
    set result [jdk_tool_javac [list $fname]]
    if {[lindex $result 0] == "ERROR"} {
        puts stderr "jdk tool check failed: JAVAC not working"
        return -1
    }

    set jarname [jdk_tool_jar test.jar]
    if {[lindex $result 0] == "ERROR"} {
        puts stderr "jdk tool check failed: JAR not working"
        return -1
    }

    # Compile test that imports tcl.lang.Interp to make
    # sure loading from CLASSPATH is working as expected.

    set fname [jdk_tool_javac_save TestInterp {
import tcl.lang.Interp;
public class TestInterp { Interp i; }
    }]
    set result [jdk_tool_javac [list $fname]]
    if {[lindex $result 0] == "ERROR"} {
        puts stderr "jdk tool check failed: Failed to find Interp on CLASSPATH"
        return -1
    }

    if {$debug} {
        puts "jdk tools working"
    }

    return 0
}

proc process_module_file { filename } {
    global _tjc

    set debug 0
    if {$_tjc(debug)} {
        set debug 1
    }

    if {$debug || $_tjc(progress)} {
        puts "processing module file $filename"
    }

    # Read in module file configuration
    if {![file exists $filename]} {
        puts stderr "module file \"$filename\" does not exists"
        return -1
    }

    if {[catch {module_parse_file $filename} err]} {
        puts stderr "module file $filename parse failure: $err"
        return -1
    }

    # Validate module commands parsed above
    if {[catch {module_parse_validate} err]} {
        puts stderr "module file $filename validation failure: $err"
        return -1
    }

    # Validate module options and proc options
    if {[catch {module_options_validate} err]} {
        puts stderr "module file $filename options validation failure: $err"
        return -1
    }

    # Query package name
    set pkg [module_query PACKAGE]

    # Query and possibly expand names of Tcl source files.
    set source_files [module_expand SOURCE]

    # Query and possibly expand names of Tcl source files to include as Tcl.
    set include_source_files [module_expand INCLUDE_SOURCE]

    set source_files [module_filter_include_source \
        $source_files $include_source_files]

    set tail [file tail $filename]
    set tailr [file rootname $tail]
    set jar_name "${tailr}.jar"
    if {[checkStatus $source_files $jar_name]} {
        return 0
    }

    # Parse source Tcl file, extract proc declarations, and save
    # the modified Tcl source into the build directory.

    set TJC [file join [pwd] [jdk_tjc_rootdir]]
    set TJC_BUILD [file join $TJC build]
    set TJC_BUILD_PACKAGE_LIBRARY [file join $TJC_BUILD [jdk_package_library $pkg 0]]

    file mkdir $TJC_BUILD_PACKAGE_LIBRARY

    set TJC_SOURCE [file join $TJC source]
    set TJC_SOURCE_PACKAGE_LIBRARY [file join $TJC_SOURCE [jdk_package_library $pkg 1]]

    file mkdir $TJC_SOURCE_PACKAGE_LIBRARY

    # Init Tcl proc name to Java class name module
    nameproc_init $pkg

    # file_and_procs is a list of {FILENAME PARSED_PROCS} for each parsed file
    set file_and_procs [list]

    set num_procs_parsed 0

    foreach source_file $source_files {
        if {$debug || $_tjc(progress)} {
            puts "scanning Tcl procs in $source_file"
        }

        set script [tjc_util_file_read $source_file]

        # Make a copy of input Tcl source
        file copy $source_file $TJC_SOURCE_PACKAGE_LIBRARY

        parseproc_init
        # Parse Tcl commands from file, if an error is generated
        # then print a diagnostic message and quit.
        if {[catch {
        set results [parseproc_start $script $source_file]
        } err]} {
            puts stderr "[module_get_filename]: Internal error while parsing Tcl file $source_file:"
            puts stderr "$err"
            return -1
        }

        set mod_script [lindex $results 0]
        set proc_tuples [lindex $results 1]
        incr num_procs_parsed [llength $proc_tuples]

        if {0 && $debug} {
        puts "got mod_script \"$mod_script\""
        puts "got proc_tuples \{$proc_tuples\}"
        }

        lappend file_and_procs [list $source_file $proc_tuples]

        # Write modified Tcl script to the build library directory
        set tail [file tail $source_file]
        set script_out [file join $TJC_BUILD_PACKAGE_LIBRARY $tail]

        tjc_util_file_saveas $script_out $mod_script

        if {$debug} {
        puts "wrote proc parsed script \"$script_out\""
        }
    }
    # release memory in case these vars are really large
    unset script mod_script

    # If no procs were parsed out of the Tcl files, generate
    # an error. It is unlikely that the user intended to
    # compile procs and none were found.

    if {$num_procs_parsed == 0} {
        puts stderr "no compilable procs found in SOURCE files"
        return -1
    }

    # Generate Java code for each proc discovered while scanning
    # Tcl files identified in the module file. Invoke the
    # compileproc_entry_point method to generate Java source.
    # The following tuple is returned by compileproc_entry_point.
    #
    # {TCL_FILENAME PROC_NAME JAVA_CLASSNAME JAVA_SOURCE}.
    #
    # Previously, all the procs generated from a file would be
    # passed to compileproc_entry_point, but that caused serious
    # problems with memory usage because a large amount of code
    # could be generated for all the procs in a file. Now, each
    # proc's code is generated and written to disk.

    set java_files [list]

    if {$debug} {
        puts "there are [llength $file_and_procs] file_and_procs pairs"
        puts "parsed a total of $num_procs_parsed Tcl procs"
    }

    set nocompile 0
    if {[info exists _tjc(nocompile)] && $_tjc(nocompile)} {
        set nocompile 1
    }

    foreach pair $file_and_procs {
        set proc_filename [lindex $pair 0]
        set proc_tuples [lindex $pair 1]

        if {$debug} {
            puts "processing proc_tuples for file $proc_filename"
            puts "there are [llength $proc_tuples] proc_tuples"
            puts "proc_tuples is [string length $proc_tuples] bytes long"
        }
        if {$_tjc(progress)} {
            puts "found [llength $proc_tuples] Tcl procs in $proc_filename"
        }

        foreach proc_tuple $proc_tuples {
            set tuple [compileproc_entry_point $proc_filename $proc_tuple]

            if {$tuple == "ERROR"} {
                # Error caught in compileproc_entry_point, diagnostic
                # message already printed so stop compilation now.
                return -1
            }

            # Write generated code for proc to Java file

            set tcl_filename [lindex $tuple 0]
            set proc_name [lindex $tuple 1]
            set java_class [lindex $tuple 2]
            set java_source [lindex $tuple 3]

            # Don't write Java source file when -nocompile is passed

            if {$nocompile} {
                set java_filename {}
                set java_filename [jdk_tool_javac_save $java_class $java_source]
            } else {
                set java_filename [jdk_tool_javac_save $java_class $java_source]
            }

            if {$debug || $_tjc(progress)} {
                puts "Tcl proc \"$proc_name\" defined in\
                    [file tail $tcl_filename] generated\
                    [string length $java_source] bytes of Java source"
            }

            lappend java_files $java_filename
        }
    }
    # release memory just in case the variable holds a lot of memory
    unset file_and_procs pair proc_tuples proc_tuple
    unset tuple java_source

    # Generate Java code for TJCExtension class for the package.
    # This code assumes that we have already checked that no two
    # files have the same filename and that all the files
    # that appear in INCLUDE_SOURCE appear in the SOURCE.
    # Also, we assume that INIT_SOURCE is a filename and not a pattern.

    set init_source [module_query INIT_SOURCE]
    set init_source [file tail $init_source]

    set source_tails [list]
    foreach file [concat $source_files $include_source_files] {
        set tail [file tail $file]
        lappend source_tails $tail
    }

    set tuple [compileproc_tjcextension $pkg $source_tails $init_source]
    set java_class [lindex $tuple 0]
    set java_source [lindex $tuple 1]
    set java_filename [jdk_tool_javac_save $java_class $java_source]
    lappend java_files $java_filename

    # Compile Java code

    if {[info exists _tjc(nocompile)] && $_tjc(nocompile)} {
        # Don't compile or create JAR file, just exit
        puts "Skipped JAVAC compile step since -nocompile flags was passed"
        return 0
    }

    if {$debug || $_tjc(progress)} {
        puts "Compiling [llength $java_files] files with javac"
    }

    set result [jdk_tool_javac $java_files]
    if {[lindex $result 0] == "ERROR"} {
        return -1
    }

    # Add INCLUDE_SOURCE Tcl files before creating jar files
    set include_source_files
    foreach include_source_file $include_source_files {
        file copy $include_source_file $TJC_SOURCE_PACKAGE_LIBRARY
        file copy $include_source_file $TJC_BUILD_PACKAGE_LIBRARY
    }

    # Create jar file for converted Tcl and class files

    set tail [file tail $filename]
    set tailr [file rootname $tail]
    set jar_name "${tailr}.jar"
    set jar_location [jdk_tool_jar $jar_name]

    # Create jar file for original Tcl source and generated Java source

    set src_jar_name "${tailr}src.jar"
    set src_jar_location [jdk_tool_jar $src_jar_name SOURCE]

    # Move generated jar files into working directory (above TJC)

    set jar_filename [file join [pwd] $jar_name]
    if {$debug} {
    puts "now to rename \"$jar_location\" to \"$jar_filename\""
    }
    if {[file exists $jar_filename]} {
        file delete $jar_filename
    }
    file rename $jar_location $jar_filename

    set src_jar_filename [file join [pwd] $src_jar_name]
    if {$debug} {
    puts "now to rename \"$src_jar_location\" to \"$src_jar_filename\""
    }
    if {[file exists $src_jar_filename]} {
        file delete $src_jar_filename
    }
    file rename $src_jar_location $src_jar_filename

    if {$debug || $_tjc(progress)} {
        puts "Created [file tail $jar_filename]"
        puts "Created [file tail $src_jar_filename]"
    }

    # Nuke TJC subdirectory
    jdk_tool_cleanup

    return 0
}

# entry point invoked when tjc is run normally.
# This method processes command line arguments
# in the argv list.

proc main { argv } {
    global argv0 _cmdline _tjc

    set debug 0

    if {$debug} {
    puts "tjc compiler loaded ..."
    puts "tjc_root is \"$_tjc(root)\""
    puts "cwd is [pwd]"
    puts "argv is \{$argv\}"
    puts "argv0 is \{$argv0\}"
    }

    process_cmdline $argv

    # Check that options (non TJC files) are valid
    if {[catch {validate_options} err]} {
        puts stderr $err
        return 1
    }

    # Anything that is not a .tjc file is filtered as an option
    if {!$debug && [llength $_cmdline(files)] == 0} {
        puts "usage: tjc module.tjc ..."
        return 1
    }

    set ret [process_jdk_config]
    if {$ret != 0} {
        return $ret
    }
    set ret [check_jdk_config]
    if {$ret != 0} {
        jdk_tool_cleanup
        return $ret
    }
    # Cleanup after check so build is clean
    # when we start processing module files.
    jdk_tool_cleanup

    foreach file $_cmdline(files) {
        set ret [process_module_file $file]
        if {$ret != 0} {
            return $ret
        }
    }

    jdk_tool_cleanup
    return 0
}


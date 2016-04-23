#
#  Copyright (c) 2005 Advanced Micro Devices, Inc.
#
#  See the file "license.amd" for information on usage and
#  redistribution of this file, and for a DISCLAIMER OF ALL
#   WARRANTIES.
#
#  RCS: @(#) $Id: tjc.tcl,v 1.2 2006/02/14 04:13:27 mdejong Exp $
#
#

# Run main if tjc.exe was run without -shell argument.
# If run with the shell argument, then just source
# tjc procedures and start the shell.

package require parser

set debug 0

set dir [file dirname $argv0]
set tail [file tail $argv0]
set froot [file root $tail]
set fext [file extension $tail]

if {$debug} {
    puts "argv0 is \"$argv0\""
    puts "argv is \"$argv\""
    
    puts "dir is \"$dir\""
    puts "tail is \"$tail\""
    puts "froot is \"$froot\""
    puts "fext is \"$fext\""
}

# Locate the tjc.jar file on the CLASSPATH, it should be in the
# lib/tcljava directory in the install tree. If running from the
# build directory, then env(TJC_LIBRARY) would be set.

set _tjc(root) [pwd]

if {$froot == "tjc" && ($fext == "" || $fext == ".exe")} {
    if {$debug} {
        puts "Running tjc shell"
    }
    source resource:/tcl/pkg/tjc/library/reload.tcl

    # If the argv contains "-file foo.tcl" then
    # source that file later on.
    set ind [lsearch -exact $argv "-file"]
    set file {}
    if {$ind != -1} {
        set file [lindex $argv [expr {$ind + 1}]]
    }

    # Nuke command line args since we don't want them
    # processed anyplace else (like in tcltest).
    set argc 0
    set argv {}

    if {$file != {}} {
        puts "source $file"
        source $file
    }
    unset dir tail froot fext file debug
} elseif {$tail == "tjc.tcl"} {
    # Run tjc program
    if {$debug} {
        puts "Running tjc program"
    }
    if {$argc != [llength $argv]} {
        error "argc is $argc but llength of argv is [llength $argv]"
    }
    # try to run compiled version first, otherwise we're building TJC 
    if {[catch {source resource:/tjc/library/reload.tcl}]} {
        source resource:/tcl/pkg/tjc/library/reload.tcl
    }

    unset dir tail froot fext debug

    exit [main $argv]
}


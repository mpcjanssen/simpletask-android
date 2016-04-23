#
#  Copyright (c) 2005 Advanced Micro Devices, Inc.
#
#  See the file "license.amd" for information on usage and
#  redistribution of this file, and for a DISCLAIMER OF ALL
#   WARRANTIES.
#
#  RCS: @(#) $Id: reload.tcl,v 1.3 2006/02/24 08:21:42 mdejong Exp $
#
#

proc reload {} {
    global _tjc

    set debug 0

    if {![info exists _tjc(tjc_load_dir)]} {
        set script [info script]
        set dir [file dirname $script]
        if {$debug} {
            puts "info script reports \"$script\""
            puts "script dir is \"$dir\""
        }
        set _tjc(tjc_load_dir) $dir
    } else {
        set dir $_tjc(tjc_load_dir)
        if {$debug} {
            puts "reloading from $dir"
        }
    }

    set regular_files [list \
        compileproc.tcl \
        descend.tcl \
        emitter.tcl \
        jdk.tcl \
        main.tcl \
        module.tcl \
        nameproc.tcl \
        parse.tcl \
        parseproc.tcl \
        util.tcl \
        ]

    # The embedded version of TJC is run in a separate
    # interp. It generates and compiles Java source
    # code directly, without writing to disk. For
    # that reason, the folllowing files are not needed.
    #
    # jdk.tcl
    # main.tcl
    # nameproc.tcl
    # parseproc.tcl
    # util.tcl

    set embedded_files [list \
        compileproc.tcl \
        descend.tcl \
        emitter.tcl \
        module.tcl \
        parse.tcl \
        ]

    if {[info exists _tjc(embedded)] && $_tjc(embedded)} {
        set files $embedded_files
    } else {
        set files $regular_files
    }

    foreach file $files {
        if {$debug} {
            puts "source $dir/$file"
        }
        uplevel #0 [list source $dir/$file]
    }
}

reload


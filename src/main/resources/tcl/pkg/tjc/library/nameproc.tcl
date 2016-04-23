#
#  Copyright (c) 2005 Advanced Micro Devices, Inc.
#
#  See the file "license.amd" for information on usage and
#  redistribution of this file, and for a DISCLAIMER OF ALL
#   WARRANTIES.
#
#  RCS: @(#) $Id: nameproc.tcl,v 1.1 2005/12/20 23:00:11 mdejong Exp $
#
#

# The nameproc module is used to generate a Java class
# name for a Tcl proc. The nameproc module is used by the
# parseproc module, but the lifetime of the names must
# cover multiple parseproc usages so that proc names
# don't conflict.

proc nameproc_init { package } {
    global _nameproc

    if {[info exists _nameproc]} {
        unset _nameproc
    }

    set _nameproc(package) $package
}

# Return a fully qualified class name for a given
# Tcl proc name. If the Tcl proc would generate a
# duplicate name, then generate a new unique one.

proc nameproc_generate { proc_name } {
    global _nameproc

    set cname [nameproc_class_name $proc_name]

    if {![info exists _nameproc(name,$cname)]} {
        # Unique class name
        set _nameproc(name,$cname) 1
    } else {
        # Not unique, generate a unique one
        incr _nameproc(name,$cname)
        set cname [nameproc_class_name $proc_name \
            $_nameproc(name,$cname)]
    }

    return [nameproc_full_class_name $_nameproc(package) $cname]
}


# Given a Tcl proc name, generate a Java class name
# that corresponds to the Tcl proc.

proc nameproc_class_name { proc_name {dup 1} } {
    set debug 0

    set cname ""
    set cap 1

    set len [string length $proc_name]
    for {set i 0} {$i < $len} {incr i} {
        set c [string index $proc_name $i]
        if {$debug} {
            puts "c for index $i is '$c'"
        }
        switch -regexp -- $c {
            {^[A-Z|a-z|0-9]$} {
                # Include as is, capitalize if needed
                if {$cap} {
                    set c [string toupper $c]
                    set cap 0
                }
                append cname $c
            }
            default {
                # Don't include unknown char, cap next known
                set cap 1
            }
        }
    }
    append cname "Cmd"
    if {$dup > 1} {
        append cname $dup
    }
    return $cname
}

# Given the name of a Java package and the name of a Java
# class for a given command, return the fully qualified
# class name.

proc nameproc_full_class_name { package class_name } {
    if {$package == "default"} {
        return $class_name
    } else {
        return "${package}.${class_name}"
    }
}


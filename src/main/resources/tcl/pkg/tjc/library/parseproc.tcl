#
#  Copyright (c) 2005 Advanced Micro Devices, Inc.
#
#  See the file "license.amd" for information on usage and
#  redistribution of this file, and for a DISCLAIMER OF ALL
#   WARRANTIES.
#
#  RCS: @(#) $Id: parseproc.tcl,v 1.2 2006/02/10 02:20:12 mdejong Exp $
#
#

# The parseproc module will parse proc definitions out
# of a Tcl file. Each proc body will be replaced by
# a command to load the compiled version of the proc.

# depends on descend module

proc parseproc_init {} {
    global _parseproc

    if {[info exists _parseproc]} {
        unset _parseproc
    }

    # Set in by parseproc_start and parseproc_command_callback
    set _parseproc(procs_parsed) 0
    set _parseproc(inscript_range) {0 0}
    set _parseproc(inscript) ""
    set _parseproc(outscript) ""
    set _parseproc(proclist) [list]
    set _parseproc(debug) 0
    set _parseproc(filename) ""

    descend_init
}

# Given an input Tcl script that contains proc definitions,
# parse the proc definitions and return a list of them
# along with the modified script. The return value is
# a pair containing {outscript proclist}. The outscript
# return value is a modified version of inscript that
# has proc body declarations replaced by a command
# to load a compiled version of the proc. The proclist
# is a list of proc body definitions that were parsed
# from the inscript.

proc parseproc_start { inscript {filename {}} } {
    global _parseproc

    set debug 0

    if {$debug} {
        puts "parseproc_start: filename is $filename"
        puts "script is\n->$script<-"
    }

    set _parseproc(filename) $filename
    set _parseproc(inscript) $inscript

    descend_report_command_callback parseproc_command_callback start
    descend_report_command_callback parseproc_command_finish_callback finish
    descend_start $inscript

    if {$debug} {
        puts "done descending in descend_start"
    }

    if {$_parseproc(procs_parsed) == 0} {
        # No procs parsed
        return [list $inscript {}]
    } else {
        # 1 or more procs parsed, append any remaining script text to outscript.

        set inscript_start [lindex $_parseproc(inscript_range) 0]
        set inscript_len [lindex $_parseproc(inscript_range) 1]

        set range [parse getrange $inscript]
        set range_start [lindex $range 0]
        set range_len [lindex $range 1]

        if {$inscript_start != $range_start} {
            error "expected inscript_start == range_start,\
                $inscript_start != $range_start"
        }

        if {$debug} {
            puts "comparing inscript_range \{$inscript_start $inscript_len\} to \{$range\}"
        }

        if {$inscript_len < $range_len} {
            set rest_start $inscript_len
            set rest_len [expr {$range_len - $inscript_len}]
            set rest [list $rest_start $rest_len]
            set rest_string [parse getstring $inscript $rest]

            if {$debug} {
                puts "inscript_len is $inscript_len, range_len is $range_len"
                puts "rest range is \{$rest\}"
                puts "appending rest_string \"$rest_string\" to outscript"
            }

            append _parseproc(outscript) $rest_string

            if {$debug} {
                puts "after rest_string append: outscript is \"$_parseproc(outscript)\""
            }
        }

        return [list $_parseproc(outscript) $_parseproc(proclist)]
    }
}

# Invoked each time a command is parsed by the descend module.
# If a command name can't be statically resolved then this
# procedure just ignores it. The command is also ignored
# if command arguments could not be determined.

proc parseproc_command_callback { key } {
    set debug 0

    if {$debug} {
        puts "parseproc_command_callback $key"
    }

    set result [descend_get_command_name $key]
    set undetermined [descend_arguments_undetermined $key]

    if {[lindex $result 0] && !$undetermined} {
        set cmdname [lindex $result 1]
        if {$cmdname == "proc"} {
            parseproc_command_proc $key $cmdname
        } elseif {$cmdname == "switch"} {
            # Make sure all code leading up to the start
            # of the switch command has been added to
            # the outscript.
            if {$debug} {
                puts "pre-process switch cmd: appending to outscript"
            }
            set cmd_range [descend_get_data $key command]
            _parseproc_append_outscript_up_to $cmd_range
            if {$debug} {
                puts "pre-process switch done"
            }
        }
    }
}

# Invoked each time a command and its contents are finished
# being parsed by the descend module.

proc parseproc_command_finish_callback { key } {

    set debug 0

    if {$debug} {
        puts "parseproc_command_finish_callback $key"
    }

    set result [descend_get_command_name $key]
    set undetermined [descend_arguments_undetermined $key]

    if {[lindex $result 0] && !$undetermined} {
        set cmdname [lindex $result 1]
        if {$cmdname == "switch"} {
            # Append all code in the switch command
            # range to the outscript. Finally,
            # reset the inscript_range using the
            # original script command range since
            # a rewritten script could have ranges
            # that differ from the original ones.

            if {$debug} {
            puts "finished parsing switch command in parseproc_command_finish_callback"
            }

            set script [descend_get_data $key script]
            set command [descend_get_data $key command]

            if {$debug} {
            puts "appending final text for possibly modified switch command"
            puts "script is \"$script\""
            puts "command range is \{$command\}"
            puts "command text is \"[parse getstring $script $command]\""
            }

            _parseproc_append_outscript_including $script $command

            if {$debug} {
            puts "done appending final text for possibly modified switch command"
            }

            set original_command [descend_get_data $key original_command]

            if {$debug} {
            puts "resetting processed script range with original command range \{$original_command\}"
            }
            set end [expr {[lindex $original_command 0] + [lindex $original_command 1]}]
            set ::_parseproc(inscript_range) [list 0 $end]
            if {$debug} {
            puts "resetting inscript_range is \{$::_parseproc(inscript_range)\}"
            }
        }
    }
}

# Process a proc command invocation discovered while parsing script

proc parseproc_command_proc { key cmdname } {
    global _parseproc

    set debug 0

    if {$debug} {
        puts "parseproc_command_proc $key $cmdname"
    }

    if {$cmdname == "proc"} {
        if {$debug} {
            puts "parseproc_command_proc found proc definiton"
        }

        set script [descend_get_data $key script]
        set command_range [descend_get_data $key command]
        set tree [descend_get_data $key tree]
        set nested [descend_get_data $key nested]

        # Number of args to proc command
        set num_args [llength $tree]
        if {$debug} {
            puts "parseproc_command_proc found $num_args args to proc command"
        }

        # Get name of proc
        set stree [lindex $tree 1]
        if {[parse_is_simple_text $stree]} {
            set name_result 1
            set name [parse_get_simple_text $script $stree "text"]
        } else {
            set name_result 0
            set name ""
        }

        if {$debug} {
            puts "name_result is $name_result, name is ->$name<-"
        }

        # Get proc arguments
        set stree [lindex $tree 2]
        if {[parse_is_constant $script $stree]} {
            set tuple [parse_get_constant_text $script $stree]
            set args [lindex $tuple 0]
            set args_text [lindex $tuple 2]

            if {![parseproc_args_ok $args]} {
                set args_result 0
            } else {
                set args_result 1
            }
        } else {
            set args_result 0
            set args ""
        }

        if {$debug} {
            puts "args_result is $args_result, args is ->$args<-"
        }

        # Get proc body
        set stree [lindex $tree 3]
        if {[parse_is_constant $script $stree]} {
            set tuple [parse_get_constant_text $script $stree]
            set body [lindex $tuple 0]
            set body_quotes [lindex $tuple 1]
            set body_text [lindex $tuple 2]

            # Ignore proc with a body block that is not braced
            if {$body_quotes == "\{\}"} {
                set body_result 1
            } else {
                set body_result 0
            }
        } else {
            set body_result 0
        }

        if {$debug} {
            puts "body_result is $body_result, body is ->$body<-"
        }

        # Get text range for body string. This is needed in case
        # something appears between the closing brace and a
        # line terminator like a semicolon.

        set closing_brace_range [lindex $tree 3 1]


        if {$num_args == 4 && $name_result && $args_result && $body_result} {
            # Report successful proc parse.
            if {$_parseproc(debug)} {
                if {$_parseproc(filename) != ""} {
                    set fsig "$_parseproc(filename): "
                } else {
                    set fsig ""
                }
                puts stderr "${fsig}parsing proc $name"
            }

            # Update the outscript to include code leading up to but
            # not including this proc def.
            set inscript_start [lindex $_parseproc(inscript_range) 0]
            if {$inscript_start != 0} {
                error "expected inscript_start to be 0, got $inscript_start"
            }
            set inscript_len [lindex $_parseproc(inscript_range) 1]
            set range_start [lindex $command_range 0]
            set range_len [lindex $command_range 1]

            # Number of characters from end of last command
            # to the start of this one.
            set before_len [expr {$range_start - $inscript_len}]
            if {$before_len > 0} {
                set range_before [list $inscript_len $before_len]
                set range_before_string [parse getstring $script $range_before]
                append _parseproc(outscript) $range_before_string

                if {$debug} {
                    puts "range_before is \{$range_before\}"
                    puts "range_before_string is \"$range_before_string\""
                }
            }

            set _parseproc(inscript_range) [list $inscript_start \
                [expr {$range_start + $range_len}]]

            if {$debug} {
                puts "prev inscript_range is \{$inscript_start $inscript_len\}"
                puts "command range is \{$range_start $range_len\}"
                puts "command string is \"[parse getstring $script [list $range_start $range_len]]\""
                puts "new outscript is \"$_parseproc(outscript)\""
                puts "new inscript_range is \{$_parseproc(inscript_range)\}"
            }

            # Append "TJC::command procname classname" to the script
            # in place of the proc and set any additional lines
            # to an empty comment to keep line numbers the same.
            # This command will be loaded into the set of commands
            # available at runtime when a tjc compiled package is loaded.

            set procname $name
            set classname [nameproc_generate $name]
            append _parseproc(outscript) [list TJC::command $procname $classname]

            if {$debug} {
                puts "post command create outscript is \"$_parseproc(outscript)\""
            }

            # Add any characters from just past the closing brace of
            # the proc body up to the terminating character.
            set last_char_index [expr {$range_start + $range_len - 1}]

            set after_closing_brace_index [expr \
                {[lindex $closing_brace_range 0] + [lindex $closing_brace_range 1]}]

            if {$debug} {
                puts "last_char_index is $last_char_index"
                puts "after_closing_brace_index is $after_closing_brace_index"
            }

            set post_str ""
            if {$after_closing_brace_index <= $last_char_index} {
                set len [expr {$last_char_index + 1 - $after_closing_brace_index}]
                set post_range [list $after_closing_brace_index $len]
                set post_str [parse getstring $script $post_range]

                if {$debug} {
                    puts "post_range is \{$post_range\}"
                    puts "post_str is \"$post_str\""
                }
            }

            # Get the last character in the command.
            # Included in command range: NEWLINE, SEMICOLON
            # Not included in command range: CLOSE_BRACKET

            set last_char_range [list $last_char_index 1]
            set last_char_string [parse getstring $script $last_char_range]
            if {$last_char_string == "\n"} {
                set last_char_is_newline 1
            } else {
                set last_char_is_newline 0
            }

            if {$last_char_string == "\;"} {
                set last_char_is_semicolon 1
            } else {
                set last_char_is_semicolon 0
            }

            # Scan the characters from the start of the 'p' in proc
            # to the closing brace of the body. to find out how
            # many newlines are contained within.

            set r [list $range_start 1]
            if {[parse getstring $script $r] != "p"} {
                error "expected proc declaration $key to begin at range \{$r\}"
            }
            set r [list [expr {$after_closing_brace_index - 1}] 1]
            if {[parse getstring $script $r] != "\}"} {
                error "expected proc declaration $key to end at range \{$r\}"
            }
            set decl_range [list $range_start [expr {$after_closing_brace_index - $range_start}]]
            set decl_newlines [parse countnewline $script $decl_range]

            if {$debug} {
                puts "command string is \"[parse getstring $script $command_range]\""
                puts "proc string is \"[parse getstring $script $decl_range]\""
                puts "decl_newlines is $decl_newlines"
                puts "last_char_is_newline is $last_char_is_newline"
                puts "last_char_is_semicolon is $last_char_is_semicolon"
                puts "nested is $nested"
            }

            if {$decl_newlines > 0} {
                if {$nested} {
                    # Continue empty lines when nested
                    for {set i 0} {$i < $decl_newlines} {incr i} {
                        append _parseproc(outscript) "\\\n"
                    }
                } else {
                    # Add empty comment lines when not nested
                    for {set i 0} {$i < $decl_newlines} {incr i} {
                        append _parseproc(outscript) "\n#"
                    }
                }
            }

            # Append any characters that appear after the closing body brace
            if {$post_str != ""} {
                append _parseproc(outscript) $post_str

                if {$debug} {
                    puts "post_str is \"$post_str\""
                    puts "post post_range outscript is \"$_parseproc(outscript)\""
                }
            }

            if {$debug} {
                puts "new outscript is \"$_parseproc(outscript)\""
            }

            # Append proc info tuple: {PROC_NAME CLASS_NAME PROC_LIST}
            # Note that the args element does not contain enclosing
            # braces so that it can be treated like a Tcl list.

            set tuple [list $name $classname [list proc $name $args_text $body_text]]
            lappend _parseproc(proclist) $tuple

            incr _parseproc(procs_parsed)

            if {$_parseproc(debug)} {
                if {$_parseproc(filename) != ""} {
                    set fsig "$_parseproc(filename): "
                } else {
                    set fsig ""
                }
                puts stderr "${fsig}parsed proc $name"
            }
        } else {
            # Report unsuccessful proc parse
            if {$_parseproc(debug)} {
                if {$_parseproc(filename) != ""} {
                    set fsig "$_parseproc(filename): "
                } else {
                    set fsig ""
                }
                puts stderr "${fsig}could not parse $name"
            }
        }
    }
}

# Append data to the outscript starting from the end of last
# processed command and leading up to (but not including)
# the passed in range. This function assumes the original
# script and ranges into the original script.

proc _parseproc_append_outscript_up_to { range } {
    global _parseproc

    set debug 0

    if {$debug} {
        puts "_parseproc_append_outscript_to \{$range\}"
    }

    set inscript $_parseproc(inscript)

    set inscript_start [lindex $_parseproc(inscript_range) 0]
    if {$inscript_start != 0} {
        error "expected inscript_start to be 0, got $inscript_start"
    }
    set inscript_len [lindex $_parseproc(inscript_range) 1]
    set range_start [lindex $range 0]
    set range_len [lindex $range 1]

    # Number of characters from end of last command
    # to the start of this one.
    set before_len [expr {$range_start - $inscript_len}]
    if {$before_len > 0} {
        set range_before [list $inscript_len $before_len]
        set range_before_string [parse getstring $_parseproc(inscript) $range_before]
        append _parseproc(outscript) $range_before_string

        if {$debug} {
            puts "range_before is \{$range_before\}"
            puts "range_before_string is \"$range_before_string\""
        }
    }

    set _parseproc(inscript_range) [list $inscript_start $range_start]

    if {$debug} {
        puts "prev inscript_range is \{$inscript_start $inscript_len\}"
        puts "command range is \{$range_start $range_len\}"
        puts "command string is \"[parse getstring $_parseproc(inscript) [list $range_start $range_len]]\""
        puts "new outscript is \"$_parseproc(outscript)\""
        puts "new inscript_range is \{$_parseproc(inscript_range)\}"
    }

    return
}


# Append data to the outscript starting from the end of last
# processed command and leading up to and including passed
# in range. This function accepts a script argument that
# is used to pass in a rewritten script.

proc _parseproc_append_outscript_including { script range } {
    global _parseproc

    set debug 0

    if {$debug} {
        puts "_parseproc_append_outscript_including \{$range\}"
    }

    set inscript $script

    set inscript_start [lindex $_parseproc(inscript_range) 0]
    if {$inscript_start != 0} {
        error "expected inscript_start to be 0, got $inscript_start"
    }
    set inscript_len [lindex $_parseproc(inscript_range) 1]
    set range_start [lindex $range 0]
    set range_len [lindex $range 1]

    # Number of characters from end of last command to the
    # end of this range.
    set before_len [expr {$range_start + $range_len - $inscript_len}]
    if {$before_len > 0} {
        set range_before [list $inscript_len $before_len]
        set range_before_string [parse getstring $inscript $range_before]
        append _parseproc(outscript) $range_before_string

        if {$debug} {
            puts "range_before is \{$range_before\}"
            puts "range_before_string is \"$range_before_string\""
        }
    }

    set _parseproc(inscript_range) [list $inscript_start \
                [expr {$range_start + $range_len}]]

    if {$debug} {
        puts "prev inscript_range is \{$inscript_start $inscript_len\}"
        puts "command range is \{$range_start $range_len\}"
        puts "command string is \"[parse getstring $inscript [list $range_start $range_len]]\""
        puts "new outscript is \"$_parseproc(outscript)\""
        puts "new inscript_range is \{$_parseproc(inscript_range)\}"
    }

    return
}



# Return 1 if the proc args contained in argstr adhere to
# Tcl's rules. If they don't, then we will not know how
# to compile the proc args.

proc parseproc_args_ok { argstr } {
    # args can be a plain string with no quotes or braces.
    # args can be a brace enclosed list
    # args can be a quoted string (a list)

    set first [string index $argstr 0]
    set last [string index $argstr end]

    if {($first == "\"" && $last != "\"") || \
        ($last == "\"" && $first != "\"")} {
        return 0
    }
    if {($first == "\{" && $last != "\}") || \
        ($last == "\}" && $first != "\{")} {
        return 0
    }

    if {$first != "\"" && $first != "\{"} {
        # An unquoted string
        if {[string first " " $argstr] != -1} {
            # unquoted string with spaces in it ???
            return 0
        }
        set naked $argstr
    } else {
        set naked [string range $argstr 1 end-1]
    }

    # If there is a dollar sign character anywhere in the args
    # string then don't accept it.

    if {[string first "\$" $naked] != -1} {
        return 0
    }

    # If there is a bracket character anywhere in the args
    # string then don't accept it.

    if {[string first "\[" $naked] != -1} {
        return 0
    }
    if {[string first "\]" $naked] != -1} {
        return 0
    }

    # Convert string to list and iterate over args
    set arg_list $naked
    set len [llength $arg_list]
    set i 0
    set found_default 0
    
    foreach arg $arg_list {
        if {[llength $arg] > 2} {
            return 0
        }
        if {[llength $arg] == 1} {
            # no default argument
            if {$arg == "args" && $i != ($len - 1)} {
                return 0
            }

            if {$arg != "args" && $found_default} {
                return 0
            }
        } else {
            # a default argument
            set name [lindex $arg 0]
            set def [lindex $arg 1]

            if {$name == "args"} {
                return 0
            }

            # must not have a default argument
            # in front of a regular argument
            set found_default 1
        }

        incr i
    }

    return 1
}


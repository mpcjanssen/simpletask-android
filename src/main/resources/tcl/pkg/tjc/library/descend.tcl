#
#  Copyright (c) 2005 Advanced Micro Devices, Inc.
#
#  See the file "license.amd" for information on usage and
#  redistribution of this file, and for a DISCLAIMER OF ALL
#   WARRANTIES.
#
#  RCS: @(#) $Id: descend.tcl,v 1.4 2006/05/29 21:56:14 mdejong Exp $
#
#

# The descend module will parse a Tcl script and invoke
# built-in Tcl command specific methods that will
# descend into the structure of commands.

# Init descend module

proc descend_init {} {
    global _descend _descend_callbacks

    if {[info exists _descend]} {
        unset _descend
    }


    set _descend(key) 0
    set _descend(container_stack) [list]
    set _descend(commands_stack) [list]

    if {[info exists _descend_callbacks]} {
        unset _descend_callbacks
    }
}

# Return the next parse key that is available.
# This module passes a parse key around so that
# functions can access parse information found
# when a specific command was parsed from a
# script.

proc descend_next_key {} {
    global _descend
    set key "dkey$_descend(key)"
    incr _descend(key)
    return $key
}

# Start parsing the given script, this method will
# return a list of keys that were parsed as the
# script was descended into.

proc descend_start { script {range {0 end}} {nested 0} {chknested 1} {undetermined 0} {outermost 1} } {
    global _descend

#    set debug 0

#    if {$debug} {
#        puts "descend_start \"$script\"\nrange \{$range\}, nested $nested, chknested $chknested"
#    }

    set start_key_id $_descend(key)
    if {$outermost} {
        _descend_commands_push
    }

    while {1} {
        # Reset script since a reparsing operation could have changed it
#        if {$debug} {
#            puts "Invoking descend_next_command with script \"$script\" and range \{$range\}"
#        }
        set key [descend_next_command $script $range $nested $undetermined]

        if {$key == ""} {
            # Done parsing commands from script
#            if {$debug} {
#                puts "got empty key from descend_next_command, done parsing"
#            }
            break
        } elseif {[llength $key] == 2 && [lindex $key 0] == "continue"} {
            # Empty command found, keep parsing the rest of the script
            set range [lindex $key 1]
#            if {$debug} {
#                puts "got continue key from descend_next_command, parsing from $range"
#            }
            continue
        }

        # Add this key to the list of command keys for this level
        _descend_commands_add $key

        # Report that a given command was parsed
#        if {$debug} {
#            puts "now to report command key $key"
#        }
        descend_report_command $key

        # Check command and arguments for nested commands.
        # Don't check for nested command if the script
        # is an unquoted argument, since nested command
        # would have already been checked for.
        if {$chknested} {
#            if {$debug} {
#                puts "checking for nested commands for key $key"
#            }
            descend_check_nested_commands $key
        } else {
#            if {$debug} {
#                puts "skipping nested commands check for key $key"
#            }
        }

        # Descend into body blocks of a known container.
#        if {$debug} {
#            puts "checking container commands for key $key"
#        }
        descend_check_container_command $key

        # Report that we are done processing the given command
        # and any commands contained within it.
#        if {$debug} {
#            puts "now to report command key $key as finished"
#        }
        descend_report_command_finish $key

        # Set range to the range of the rest of the code.
        # Note that when a switch script reparses its code
        # it keeps the original script's rest range.
        set range [descend_get_data $key rest]
    }

    # Report all keys created during descent
    set end_key_id $_descend(key)
    set keys [list]

    for {set key_id $start_key_id} {$key_id < $end_key_id} {incr key_id} {
        lappend keys "dkey$key_id"
    }

    if {$outermost} {
        set level_keys [_descend_commands_pop]
        # Stack should be empty again
        if {[llength $_descend(commands_stack)] != 0} {
            error "level stack depth is [llength $_descend(commands_stack)], expected depth of 0"
        }
        set _descend(commands,outermost) $level_keys
    }

    return $keys
}

# Parse the next command from the given script
# and create a new key for the parse result.

proc descend_next_command { script range nested undetermined } {
    global _descend

#    set debug 0

#    if {0 || $debug} {
#        set rest [parse getstring $script $range]
#        if {[string length $rest] > 2000} {
#            set rest [string range $rest 0 2000]
#        }
#        puts "descend_next_command: from range \{$range\} : rest of script is\
#            \n->$rest<-"
#    }

    set charlength [parse charlength $script $range]

    if {$charlength == 0} {
        # No more chars to parse from script
#        if {$debug} {
#            puts "descend_next_command: no more chars to parse"
#        }
        return ""
    }

#    if {$debug} {
#        puts "descend_next_command: $charlength chars left in script"
#    }

    # Clear parse error before parse operation
    global _tjc
    set _tjc(parse_error) ""

    if {[catch {parse command $script $range} err]} {
        # Error while parsing script. Check for
        # some known errors and generate additional
        # information that will be useful when
        # printing an error message to the user.
        set handled 0
        set bracket [string match "*missing close-bracket*" $err]
        if {$bracket} {
            set _tjc(parse_error) "missing close-bracket"
            set handled 1
        }
        if {$handled} {
            set _tjc(parse_script) [parse getstring $script $range]
        }
        # Rethrow error
        return -code error -errorcode $::errorCode $err
    } else {
        set result $err
    }
    set comment [lindex $result 0]
    set command [lindex $result 1]
    set rest [lindex $result 2]
    set tree [lindex $result 3]

    # If we parsed an empty command, then return [continue $rest]
    # so that the empty command will be skipped and parsing
    # will continue.

    if {[lindex $command 1] == 0 || \
            [string trim [parse getstring $script $command]] == "\;"} {
#        if {$debug} {
#            puts "descend_next_command: empty command parsed, returning continue $rest"
#        }
        return [list continue $rest]
    }

    set key [descend_next_key]

    set _descend($key,script) $script

    # If range has the word "end" in the second position, save the actual length
    if {[lindex $range 1] == "end"} {
        set whole_range [parse getrange $script]
#        if {$debug} {
#            puts "whole_range is \{$whole_range\}"
#            puts "range is \{$range\}"
#        }
        set whole_range_len [lindex $whole_range 1]
        set range_start [lindex $range 0]
        set range_len [lindex $range 1]
        set range [list $range_start [expr {$whole_range_len - $range_start}]]
#        if {$debug} {
#            puts "updated range from \{$range_start $range_len\} to \{$range\}"
#        }
    }
    set _descend($key,range) $range
    set _descend($key,comment) $comment
    set _descend($key,command) $command
    set _descend($key,rest) $rest
    set _descend($key,tree) $tree
    set _descend($key,nested) $nested
    set _descend($key,undetermined) $undetermined
    set _descend($key,usage) 0 ; # assume no usage error by default

    # Init commands with an empty list for each argument by default
    set commands [list]
    set len [llength $tree]
    for {set i 0} {$i < $len} {incr i} {
        lappend commands {}
    }
    set _descend($key,commands) $commands

    set _descend($key,container_commands) [list]
    set _descend($key,validated) {}
    set _descend($key,static_container) 0

#    if {$debug} {
#        puts "comment text ->[parse getstring $script $comment]<-"
#        puts "command text ->[parse getstring $script $command]<-"
#        puts "rest text ->[parse getstring $script $rest]<-"
#        puts "tree is \{$tree\}"
#        puts "nested is $nested"
#        puts "undetermined is $undetermined"
#    }

    return $key
}

# Invoked each time a command is parsed from
# a script passed to descend_start. This
# method will invoke a user defined callback
# so that user code can record that a specific
# command was discovered.

proc descend_report_command { key } {
    global _descend_callbacks

#    set debug 0

#    if {$debug} {
#        puts "descend_report_command $key"
#    }

    if {[info exists _descend_callbacks(report_command)]} {
        set cmd $_descend_callbacks(report_command)
#        if {$debug} {
#            puts "descend_report_command callback : $cmd $key"
#        }
        namespace eval :: [list $cmd $key]
    }
}

# Like descend_report_command except that this callback is
# invoked after a command and any contained commands have
# been processed.

proc descend_report_command_finish { key } {
    global _descend_callbacks

#    set debug 0

#    if {$debug} {
#        puts "descend_report_command_finish $key"
#    }

    if {[info exists _descend_callbacks(report_command_finish)]} {
        set cmd $_descend_callbacks(report_command_finish)
        namespace eval :: [list $cmd $key]
    }
}


# Define a callback command that will be invoked
# each time a command is parsed from the script.
# The callback should accept one argument, the
# parse key for this specific command.

proc descend_report_command_callback { cmd {when start} } {
    global _descend_callbacks
    if {$when == "start"} {
        set _descend_callbacks(report_command) $cmd
    } elseif {$when == "finish"} {
        set _descend_callbacks(report_command_finish) $cmd
    } else {
        error "unknow when value \"$when\", must be start or finish"
    }
}

# Report command keys as they appear in the body of the script
# passed to descend_start. When no key is given, the keys
# detected in the topmost layer of the script passed to descend_start
# are returned. Otherwise, a list of keys that corresponds to
# the command arguments for a given key are returned. The default
# type argument will return "nested" type command invocations. If
# the "container" type is passed, container commands will be
# returned instead.

proc descend_commands { {key {}} {type nested} } {
    global _descend

    if {$type == "nested"} {
        set tkey commands
    } elseif {$type == "container"} {
        set tkey container_commands
    } else {
        error "unnown type \"$type\""
    }

    if {$key == {}} {
        return $_descend(commands,outermost)
    } else {
        # A valid key, lookup command body info
        return [descend_get_data $key $tkey]
    }
}

# Push a new empty list of commands onto the body stack. This
# list is filled as nested commands are discovered. 

proc _descend_commands_push {} {
    global _descend
    set l [list]
    set _descend(commands_stack) [linsert \
        $_descend(commands_stack) 0 $l]
    return
}

# Pop the current list of commands off the body stack. This
# is used to report the commands that have been found at
# a given level while descending into a script.

proc _descend_commands_pop {} {
    global _descend
    set l [lindex $_descend(commands_stack) 0]
    set _descend(commands_stack) [lrange $_descend(commands_stack) 1 end]
    return $l
}

# Add a parsed command key to the list of commands at
# the top of the body stack.

proc _descend_commands_add { key } {
    global _descend
    set l [lindex $_descend(commands_stack) 0]
    lappend l $key
    set _descend(commands_stack) [lreplace $_descend(commands_stack) 0 0 $l]
    return $l
}

# Invoked when a usage error is found while
# descending into a command.

proc descend_report_usage { key info } {
    global _descend_callbacks

#    set debug 0

#    if {$debug} {
#        puts "descend_report_usage $key \{$info\}"
#    }

    if {[info exists _descend_callbacks(report_usage)]} {
        set cmd $_descend_callbacks(report_usage)
        namespace eval :: [list $cmd $key $info]
    }
}

# Define a callback command that will be invoked
# each time a usage error is found while descending
# into a script.

proc descend_report_usage_callback { cmd } {
    global _descend_callbacks
    set _descend_callbacks(report_usage) $cmd
}

# Get data element for the named key. This command
# is invoked frequently, so it is as optimized.

proc descend_get_data { key dname } {
    if {[catch {
        set result $::_descend($key,$dname)
    } err]} {
        # If the key way not found, generate an
        # error to indicate why.

        if {![info exists ::_descend($key,script)]} {
            error "key \"$key\" does not exists"
        }
        if {![info exists ::_descend($key,$dname)]} {
            error "data \"$dname\" does not exists for key \"$key\""
        }
        error $err
    }
    return $result
}

# Return true if a script argument to a container command
# is statically defined. For example the command [if {1} {cmd1}]
# has a statically defined script argument at index 2. The
# same would also apply to [if {1} cmd1], while [if {1} $script]
# and [if {1} [command]] would not be considered static.

proc descend_container_argument_body_is_static { key argindex } {
    set tree [descend_get_data $key tree]
    set subtree [lindex $tree $argindex]
    if {[parse_is_simple_text $subtree]} {
        return 1
    } else {
        return 0
    }
}

# Return true if arguments for the given command could not
# be determined. There are cases where a command would be
# invoked at runtime, but it is not possible to tell what
# the arguments to the command would be.

proc descend_arguments_undetermined { key } {
    set undetermined [descend_get_data $key undetermined]
    return $undetermined
}

# Return a pair {result cmdname}. If the command name
# can not be determined statically, then {0 {}} will
# be returned. Otherwise, {1 cmdname} will be returned.

proc descend_get_command_name { key } {
#    set debug 0

#    if {$debug} {
#        puts "descend_get_command_name : $key"
#    }

    # Query parse tree and see if the command
    # name is a simple name that can be returned

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set result [list]

    if {[parse_is_simple_text [lindex $tree 0]]} {
        lappend result 1 \
            [parse_get_simple_text $script [lindex $tree 0]]
    } else {
        lappend result 0 {}
    }

    return $result
}

# Return a list consisting of the command and its arguments.
# If the arguments could not be determined, then only the
# command name is returned.

proc descend_get_command { key } {
#    set debug 0

#    if {$debug} {
#        puts "descend_get_command : $key"
#    }

    # Query parse tree and see if the command
    # name is a simple name that can be returned

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    # If the arguments can't be determined, then
    # just pass a tree for the command name.

    if {[descend_arguments_undetermined $key]} {
#        if {$debug} {
#            puts "descend_get_command: arguments undetermined"
#        }
        set tree [list [lindex $tree 0]]
    }

    return [descend_tree_get_command $script $tree]
}

proc descend_tree_get_command { script tree } {
    set cmd [list]

    set len [llength $tree]
    for {set i 0} {$i < $len} {incr i} {
        set result [descend_tree_get $script $tree $i]
        if {[lindex $result 0] == 1} {
            lappend cmd [lindex $result 1]
        } else {
            # Could not get a command description for this elem
            lappend cmd UNKNOWN
        }
    }

    return $cmd
}

# Return a command element as a list element. This
# can include the command name or one of its arguments.

proc descend_tree_get { script tree index } {
    set stree [lindex $tree $index]

    if {[parse_is_simple_text $stree]} {
        return [list 1 [parse_get_simple_text $script $stree]]
    }

    # Word type, could be a simple command, a simple variable,
    # or a mixture of the commands, variables, and text.

    if {[parse_is_word $stree]} {
        if {[parse_is_word_command $stree]} {
            return [list 1 [parse_get_word_command $script $stree]]
        }

        if {[parse_is_word_variable $stree]} {
            return [list 1 [parse_get_word_variable $script $stree]]
        }

        # FIXME: Multiple command, variable get as list needed

        # Return straight word text
        set range [lindex $stree 1]
        return [list 1 [parse getstring $script $range]]
    }

    # Not found
    return {0 {}}
}

# This command will loop over the command word and
# any arguments checking for a nested command invocations.

proc descend_check_nested_commands { key } {
    global _descend

#    set debug 0

#    if {$debug} {
#        puts "descend_check_nested_commands $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    set argument_keys [list]

    for {set i 0} {$i < $num_args} {incr i} {
        _descend_commands_push
        set stree [lindex $tree $i]
        if {[parse_is_word_command $stree]} {
            # Parse nested command as argument
            descend_nested_subtree $script $stree
        } elseif {[parse_is_word_variable $stree]} {
            set var_subtree [lindex $stree 2 0]
            descend_check_variable_for_nested_commands $script $var_subtree
        } elseif {[parse_is_word $stree]} {
            # A word element is made up of variable, command, and text elements.
            # A simple/text type is not a word.
            descend_check_word_for_nested_commands $script $stree
        }
        lappend argument_keys [_descend_commands_pop]
    }

    # Define list of keys for each argument that is a command
    set _descend($key,commands) $argument_keys
#    if {$debug} {
#        puts "descend_check_nested_commands $key: set nested commands list to \{$argument_keys\}"
#    }

    return
}

# Invoked when a variable subtree should be checked to see if it contains
# command invocations.

proc descend_check_variable_for_nested_commands { script stree } {
#    set debug 0
#    if {$debug} {
#        puts "descend_check_variable_for_nested_commands: \"$script\" \{$stree\}"
#    }
    
    if {![parse_is_variable $stree]} {
        error "expected variable stree but got \{$stree\}"
    }

    if {[parse_is_scalar_variable $stree]} {
        # A scalar can't contain nested commands
        return
    }

    parse_variable_iterate $script $stree \
        descend_check_variable_for_nested_commands_iterator
}

# Callback invoked from descend_check_nested_commands while
# descending into the elements in a variable looking for
# command elements.

proc descend_check_variable_for_nested_commands_iterator { script stree type values ranges } {
#    set debug 0

#    if {$debug} {
#        puts "descend_check_variable_for_nested_commands_iterator : \"$script\" : \{$type\} \{$values\} \{$ranges\}"
#    }

    # Use array type info from parse layer to decide how to
    # handle the variable.

    switch -exact -- $type {
        {scalar} -
        {array text} -
        {array scalar} -
        {array word} -
        {word begin} -
        {word end} -
        {word text} -
        {word scalar} -
        {word array text} -
        {word array scalar} -
        {word array word} {
            # No-op
        }
        {array command} -
        {word command} -
        {word array command} {
            # Array with a command key: $a([cmd]) (values {ARRAY_NAME cmd})
            # or an array with a word element that is a command (values {cmd})
            # or an array with a word element that is an array with a command key (values {ARRAY_NAME cmd})

#            if {$debug} {
#                puts "descend_check_variable_for_nested_commands_iterator : found nested command in array variable word"
#            }
            if {$type == {array command} || $type == {word array command}} {
                set cmd_stree [lindex $stree 2 1]
            } elseif {$type == {word command}} {
                set cmd_stree $stree
            } else {
                error "unknown type \"$type\""
            }

            _descend_commands_push
            descend_nested_subtree $script $cmd_stree
            _descend_commands_add [_descend_commands_pop]
        }
        default {
            error "unknown type \{$type\}"
        }
    }
}

# Invoked when a word subtree should be checked to see if any
# of the word elements are command invocations.

proc descend_check_word_for_nested_commands { script stree } {
#    set debug 0

#    if {$debug} {
#        puts "descend_check_word_for_nested_commands: \"$script\" \{$stree\}"
#    }

    if {![parse_is_word $stree]} {
        error "expected word type stree: got type [lindex $stree 0] from \{$stree\}"
    }

    parse_word_iterate $script $stree \
        descend_check_word_for_nested_commands_iterator
}

# Callback invoked from descend_check_word_for_nested_commands while
# descending into the elements in a word looking for
# command elements.

proc descend_check_word_for_nested_commands_iterator { script stree type values ranges } {
#    set debug 0

#    if {$debug} {
#        puts "descend_check_word_for_nested_commands_iterator : \"$script\" \{$stree\} : \{$type\} \{$values\} \{$ranges\}"
#    }

    switch -exact -- $type {
        {backslash} {
            # No-op
        }
        {command} {
            # Nested command as word element
            _descend_commands_push
            descend_nested_subtree $script $stree
            _descend_commands_add [_descend_commands_pop]
        }
        {text} {
            # No-op
        }
        {variable} {
            # Check variable in case array key word contains nested commands
            descend_check_variable_for_nested_commands $script $stree
        }
        {word begin} -
        {word end} -
        {word} {
            # No-op
        }
        default {
            error "unknown type \"$type\""
        }
    }
}

# Check the given parsed command to see if it is a "container"
# command that contains other code to evaulate. For example,
# the if command is a container command.

proc descend_check_container_command { key } {
    global _descend

#    set debug 0

    # If the command name is not a simple/text type, then
    # we can't determine what it is statically.

    set result [descend_get_command_name $key]

    if {[lindex $result 0] == 0} {
#        if {$debug} {
#            puts "descend_check_container_command: returning since command name is not static"
#        }
        return
    }

    # No point in trying to descend into a command that we
    # can't determine arguments for.

    if {[descend_arguments_undetermined $key]} {
#        if {$debug} {
#            puts "descend_check_container_command: returning since command arguments can't be determined"
#        }
        return
    }

    set cmdname [lindex $result 1]

    switch -exact -- $cmdname {
        "::after" -
        "after" {
            set descend_cmd descend_container_after
        }
        "::catch" -
        "catch" {
            set descend_cmd descend_container_catch
        }
        "::expr" -
        "expr" {
            set descend_cmd descend_container_expr
        }
        "::for" -
        "for" {
            set descend_cmd descend_container_for
        }
        "::foreach" -
        "foreach" {
            set descend_cmd descend_container_foreach
        }
        "::if" -
        "if" {
            set descend_cmd descend_container_if
        }
        "::lsort" -
        "lsort" {
            set descend_cmd descend_container_lsort
        }
        "::switch" -
        "switch" {
            set descend_cmd descend_container_switch
        }
        "::while" -
        "while" {
            set descend_cmd descend_container_while
        }
        default {
            return
        }
    }

#    if {$debug} {
#        puts "descend_check_container_command: found descend_cmd $descend_cmd"
#    }

    # Push command onto container stack
    set _descend(container_stack) [linsert $_descend(container_stack) 0 $cmdname]

    # Invoke descend handler for cmdname
    namespace eval :: [list $descend_cmd $key]

    # Pop command off container stack
    set _descend(container_stack) [lreplace $_descend(container_stack) 0 0]

    return
}

# Return "container" stack, this is a stack of command names that contain
# other commands. For example, an if command contains code in a true block.
# The commands inside the true block are considered to be in the "if" container.

proc descend_get_container_stack {} {
    global _descend
    return $_descend(container_stack)
}

# Debug command that will print the contents of a parse tree.

proc descend_tree_print { key } {
    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]
    _descend_tree_print $script $tree
}

proc _descend_tree_print { script tree } {
    if {$tree == {}} {
        return
    }

    foreach tuple $tree {
        set type [lindex $tuple 0]
        set range [lindex $tuple 1]
        set subtree [lindex $tuple 2]
        puts "\"[parse getstring $script $range]\" : type $type : range $range"
        _descend_tree_print $script $subtree
    }
}

# Return the range of the unquoted body given a body
# argument that may or may not be quoted. If it is
# not quoted, then range return is the one passed in.

proc descend_range_quoted { script subtree range } {
    set range_text [parse getstring $script $range]
    set first [string index $range_text 0]
    set last [string index $range_text end]

    set trim 0
    set err 0
    set isbraced 0

    if {$first == "\{"} {
        if {$last == "\}"} {
            set trim 1
            set isbraced 1
        } else {
            set err 1
        }
    } elseif {$last == "\}"} {
        set err 1
    }

    if {$first == "\""} {
        if {$last == "\""} {
            set trim 1
        } else {
            set err 1
        }
    } elseif {$last == "\""} {
        set err 1
    }

    if {$err} {
        error "mismatched quoting for quoted text \"$range_text\""
    }

    # Not quoted
    if {!$trim} {
        return [list $range 0]
    }

    # Determine range without the quotes or braces
    set range_start [lindex $range 0]
    set range_len [lindex $range 1]

    return [list [list [expr {$range_start + 1}] [expr {$range_len - 2}]] $isbraced]
}

# Like descend_range_quoted except for a bracketed nested command

proc descend_range_bracked { script subtree range } {
    return [parse_range_without_brackets $script $range]
}

# Descend into a script containing commands that are passed as
# a body argument. For example an if command like "if {1} { cmd1 }"
# has a container body argument "{ cmd 1 }". This procedure returns
# a list of keys parsed.

proc descend_container_body_argument { key script tree body_index {undetermined 0} } {
#    set debug 0

    set nested [descend_get_data $key nested]

    set body_subtree [lindex $tree $body_index]
    set body_range [lindex $body_subtree 1]
    set body_text [parse getstring $script $body_range]
    
    set result [descend_range_quoted $script $body_subtree $body_range]
    set unquoted_body_range [lindex $result 0]
    set isbraced [lindex $result 1]
    set chknested $isbraced
    set outermost 0

#    if {$debug} {
#        puts "descend_container_body_argument in container command $key"
#        puts "script is \"$script\""
#        puts "range is \"$unquoted_body_range\""
#        puts "range_text is \"[parse getstring $script $unquoted_body_range]\""
#        puts "isbraced is $isbraced"
#        puts "chknested is $chknested"
#        puts "undetermined is $undetermined"
#    }

    return [descend_start $script $unquoted_body_range $nested $chknested $undetermined $outermost]
}

# Descend into a script that is made up of multiple arguments
# concatenated together. For example an after command like
# after 50 cmd {arg1 arg2} arg3 should descend into the equivalent
# of {cmd arg1 arg2 arg3}. The most common case is
# after 50 "cmd arg1 arg2 arg3" which should be descended
# into as [cmd arg1 arg2 arg3]. This procedure returns
# a list of keys parsed.

proc descend_container_concat_body_arguments { key script tree body_indexes {undetermined 0} } {
#    set debug 0

    set nested [descend_get_data $key nested]

    set outermost 0

    set ranges [list]

    set prev_body_index -1

    set command ""

    if {[llength $body_indexes] == 0} {
        error "body_indexes can't be {}"
    }

    foreach body_index $body_indexes {
        if {$prev_body_index == -1} {
            set prev_body_index $body_index
        } elseif {$body_index != ($prev_body_index + 1)} {
            error "prev_body_index was $prev_body_index,\
                body_index was $body_index, expected body index to be prev + 1"
        } else {
            set prev_body_index $body_index
        }

        set body_subtree [lindex $tree $body_index]
        if {![parse_is_simple_text $body_subtree]} {
            error "body subtree at index $body_index should be simple/text"
        }
        set text [parse_get_simple_text $script $body_subtree "text"]
        # append each element to generated script
        foreach elem $text {
#            if {$debug} {
#                puts "appended simple/text element \"$elem\""
#            }
            append command $elem
            append command " "
        }
    }

    # Indicate that the range is unbraced by passing 0 for chknested.
    # This keeps a nested command argument from being descended into twice.
    # This should not actually matter here since we only deal with
    # simple/text entries.
    set chknested 0

#    if {$debug} {
#        puts "descend_container_concat_body_arguments in container command $key"
#        puts "command is \"$command\""
#        puts "chknested is $chknested"
#    }

    # FIXME: problem with command here, the script argument does not match
    # the original. Need to check for this special case somehow in caller code.

    return [descend_start $command {0 end} $nested $chknested $undetermined $outermost]
}

# Descend into a nested command enclosed in brackets.
# For example a command like "set var [one]" would
# descend into the "[one]" arguments minus the brackets.
# This procedure returns a list of keys parsed.

proc descend_nested_subtree { script subtree {undetermined 0} } {
#    set debug 0

    set nested 1
    set chknested 1
    set outermost 0

    set range [lindex $subtree 1]
    set unbracked_range [descend_range_bracked $script $subtree $range]

#    if {$debug} {
#        puts "descending into nested command in subtree \{$subtree\}"
#        puts "script is \"$script\""
#        puts "range is \"$unbracked_range\""
#        puts "range_text is \"[parse getstring $script $unbracked_range]\""
#    }

    set keys [descend_start $script $unbracked_range $nested $chknested $undetermined $outermost]
    # An empty nested command would report no keys. Create a special empty
    # one and report it now.
    #if {[llength $keys] == 0} {
    #    set keys [descend_empty_nested_command $script $unbracked_range]
    #}
    return $keys
}

# Generate an empty nested command key and
# report it in the normal way.

proc descend_empty_nested_command { script range } {
    global _descend
    set key [descend_next_key]

    set erange [list [lindex $range 0] 0]
    set _descend($key,script) $script
    set _descend($key,range) $range
    set _descend($key,comment) $erange
    set _descend($key,command) $erange
    set _descend($key,rest) $erange
    set _descend($key,tree) {}
    set _descend($key,nested) 1
    set _descend($key,undetermined) 0
    set _descend($key,usage) 0

    descend_report_command $key
    descend_report_command_finish $key

    return $key
}

# Invoked when an if command is discovered while descending into a
# Tcl script.

proc descend_container_if { key } {
    global _descend

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    # Validate arguments passed to if command.
    # If the command is not valid, just ignore it.
    set result [descend_container_if_validate $key]
    if {$result != "OK"} {
        # FIXME: Log errors somehow?

        if {[descend_get_data $key usage]} {
            descend_report_usage $key $result
        }

        return
    }

    set container_commands [list]

    # Check expr argument and true body block: if {EXPR} {BODY}
    set pair [descend_container_if_expr_body $key]
    set expr_index [lindex $pair 0]
    set body_index [lindex $pair 1]

    # descend into expr argument looking for commands
    _descend_commands_push
    descend_container_expr_argument $key $expr_index
    lappend container_commands [_descend_commands_pop]

    if {[descend_container_argument_body_is_static $key $body_index]} {
        _descend_commands_push
        descend_container_body_argument $key $script $tree $body_index
        lappend container_commands [_descend_commands_pop]
    } else {
        lappend container_commands {}
    }

    # Descend into elseif arguments if they exist

    if {[descend_container_if_has_elseif_body $key]} {
        foreach {expr_index body_index} [descend_container_if_iterate_elseif_body $key] {
            # descend into expr argument looking for commands
            _descend_commands_push
            descend_container_expr_argument $key $expr_index
            lappend container_commands [_descend_commands_pop]

            if {[descend_container_argument_body_is_static $key $body_index]} {
                _descend_commands_push
                descend_container_body_argument $key $script $tree $body_index
                lappend container_commands [_descend_commands_pop]
            } else {
                lappend container_commands {}
            }
        }
    }

    # Descend into else body if there is one

    if {[descend_container_if_has_else_body $key]} {
        set body_index [descend_container_if_else_body $key]

        if {[descend_container_argument_body_is_static $key $body_index]} {
            _descend_commands_push
            descend_container_body_argument $key $script $tree $body_index
            lappend container_commands [_descend_commands_pop]
        } else {
            lappend container_commands {}
        }
    } else {
        lappend container_commands {}
    }
    set _descend($key,container_commands) $container_commands
}

# If the "if" command indicated by "key" has valid arguments,
# then return "OK". If any of the arguments are invalid
# then return an error message indicating the problem.

proc descend_container_if_validate { key } {
    global _descend

#    set debug 0

#    if {$debug} {
#        puts "descend_container_if_validate $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    # Validated if command indexes in a list of length 3.
    # The first list element is a pair of indexes indicating
    # where the first expr and body are located. The
    # second list element is a list of indexes indicating
    # the indexes of each elseif expr and body block.
    # the final list element is the index of the else body.
    # {{expr_ind body_ind} {expr_ind body_ind ...} {body_ind}}

    set indexes_expr_body [list]
    set indexes_elseif_body [list]
    set indexes_else_body [list]
    set _descend($key,validated) {}

    set i 1
    set argc [llength $tree]

    set clause "if"
    set keywordNeeded 0
    set static 1
    while {1} {
        # At this point in the loop, lindex i refers to an expression
        # to test, either for the main expression or an expression
        # following an "elseif".  The arguments after the expression must
        # be "then" (optional) and a script to execute.

        if {$i >= $argc} {
            set word [lindex [lindex $tree [expr {$i - 1}]] 1]
            set end [expr {[lindex $word 0] + [lindex $word 1]}]
            set _descend($key,usage) 1
            return [list noExpr [list $end 1] $clause]
        }
        # expr argument at index $i
        if {![descend_container_expr_argument_validate $key $i]} {
            set _descend($key,usage) 1
            return [list invalidArg "if" $i {} $clause "expr argument"]
        }
        # determine if body code is static
        if {![descend_container_argument_body_is_static $key $i]} {
            set static 0
        }
        if {$clause == "if"} {
            lappend indexes_expr_body $i
        } else {
            lappend indexes_elseif_body $i
        }
        incr i
        # skip optional "then" argument if found
        if {$i < $argc} {
            set subtree [lindex $tree $i]
            if {[parse_is_simple_text $subtree]} {
                set text [parse_get_simple_text $script $subtree "text"]
                if {$text == "then"} {
                    incr i
                }
            }
        }
        if {$i >= $argc} {
            set word [lindex [lindex $tree [expr {$i - 1}]] 1]
            set end [expr {[lindex $word 0] + [lindex $word 1]}]
            set _descend($key,usage) 1
            return [list noScript [list $end 1] $clause]
        }
        # body argument at index $i
        if {![descend_container_argument_body_is_static $key $i]} {
            set static 0
        }
        if {$clause == "if"} {
            lappend indexes_expr_body $i
        } else {
            lappend indexes_elseif_body $i
        }
        set keywordNeeded 1
        incr i

        if {$i >= $argc} {
            # completed successfully
            set _descend($key,static_container) $static
            set _descend($key,validated) \
                [list $indexes_expr_body $indexes_elseif_body $indexes_else_body]
            return "OK"
        }

        # check for "elseif" and continue looping if found
        set subtree [lindex $tree $i]
        if {[parse_is_simple_text $subtree]} {
            set text [parse_get_simple_text $script $subtree "text"]
            if {$text == "elseif"} {
                set keywordNeeded 0
                set clause "elseif"
                incr i
                continue
            }
        }
        break
    }

    # Now we check for an else clause
    set subtree [lindex $tree $i]
    if {[parse_is_simple_text $subtree] && \
            [parse_get_simple_text $script $subtree "text"] == "else"} {
        set clause "else"
        set keywordNeeded 0
        incr i

        if {$i >= $argc} {
            set word [lindex [lindex $tree [expr {$i - 1}]] 1]
            set end [expr {[lindex $word 0] + [lindex $word 1]}]
            set _descend($key,usage) 1
            return [list noScript [list $end 1] $clause]
        }

        # body argument at index $i
        if {![descend_container_argument_body_is_static $key $i]} {
            set static 0
        }
        lappend indexes_else_body $i
    }

    # Final argument is not marked with else keyword, this is
    # a deprecated but legal way to indicate an else body.

    if {($keywordNeeded) || (($i+1) != $argc)} {
        if {![descend_container_argument_body_is_static $key $i]} {
            set static 0
        }
        lappend indexes_else_body $i
    }

    # Return "OK" when the arguments to "if" are considered valid
    set _descend($key,static_container) $static
    set _descend($key,validated) \
        [list $indexes_expr_body $indexes_elseif_body $indexes_else_body]
    return "OK"
}

# The container functions below (like descend_container_if_expr)
# assume that the arguments to the function have already been
# validated (with descend_container_if_validate for example).

# Return {EXPR_INDEX BODY_INDEX} for if command.

proc descend_container_if_expr_body { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    return [lindex $validated 0]
}

# Return true if there are one of more "elseif EXPR BODY" arguments
# for this if command.

proc descend_container_if_has_elseif_body { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    return [llength [lindex $validated 1]]
}

# Return a list of {EXPR_INDEX BODY_INDEX} elements
# for each elseif block.

proc descend_container_if_iterate_elseif_body { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    set elems [lindex $validated 1]
    if {[llength $elems] == 0} {
        error "no elseif body elements"
    }
    return $elems
}

# Return true if this "if" command has an "else BODY" block.

proc descend_container_if_has_else_body { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    return [llength [lindex $validated 2]]
}

# Return the BODY_INDEX for the "else BODY" block of this if command.

proc descend_container_if_else_body { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    set body [lindex $validated 2]
    if {[llength $body] == 0} {
        error "no else body"
    }
    if {[llength $body] > 1} {
        error "too many else body elements"
    }
    return $body
}

# Invoked when a catch command is discovered while descending into a
# Tcl script.

proc descend_container_catch { key } {
    global _descend

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    # Validate arguments, ignore if invalid.
    set result [descend_container_catch_validate $key]
    if {$result != "OK"} {
        # FIXME: Log errors somehow?

        if {[descend_get_data $key usage]} {
            descend_report_usage $key $result
        }

        return
    }

    # Descend into script argument if it is statically defined.
    set body_index [descend_container_catch_body $key]
    if {[descend_container_argument_body_is_static $key $body_index]} {
        _descend_commands_push
        descend_container_body_argument $key $script $tree $body_index
        set _descend($key,container_commands) [_descend_commands_pop]
    }
}

# If the "catch" command indicated by "key" has valid arguments,
# then return "OK". If any of the arguments are invalid
# then return an error message indicating the problem.

proc descend_container_catch_validate { key } {
    global _descend

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set num_args [llength $tree]

    # Usage: catch script ?varName?

    if {$num_args != 2 && $num_args != 3} {
        set _descend($key,usage) 1
        return [list numArgs "catch" $num_args "2 or 3"]
    }

    # The script argument must be a static string,
    # but varname can be evaluated at runtime if needed.

    if {[descend_container_argument_body_is_static $key 1]} {
        set _descend($key,static_container) 1
    }

    # Validated catch command is a list of length 2.

    if {$num_args == 3} {
        set _descend($key,validated) [list 1 2]
    } else {
        set _descend($key,validated) [list 1 {}]
    }

    return "OK"
}

# Return BODY_INDEX for catch command.

proc descend_container_catch_body { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    return [lindex $validated 0]
}

# Return true if there is a variable name argument
# for the catch command.

proc descend_container_catch_has_variable { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    set index [lindex $validated 1]
    if {$index == {}} {
        return 0
    } else {
        return 1
    }
}

# If the catch variable is statically defined,
# return {1 VARNAME} indicating the name. Otherwise
# return {0 {}} to indicate that the variable
# name needs to be evaluated at runtime.

proc descend_container_catch_variable { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    set index [lindex $validated 1]
    if {$index == {}} {
        error "no variable argument"
    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]
    set subtree [lindex $tree $index]
    if {[parse_is_simple_text $subtree]} {
        return [list 1 [parse_get_simple_text $script $subtree "text"]]
    } else {
        return {0 {}}
    }
}

# Invoked when a while command is discovered while descending into a
# Tcl script.

proc descend_container_while { key } {
    global _descend

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    # Validate arguments, ignore if invalid.
    set result [descend_container_while_validate $key]
    if {$result != "OK"} {
        # FIXME: Log errors somehow?

        if {[descend_get_data $key usage]} {
            descend_report_usage $key $result
        }

        return
    }

    set container_commands [list]
    _descend_commands_push

    # descend into expr argument looking for commands
    set expr_index [descend_container_while_expr $key]
    descend_container_expr_argument $key $expr_index

    lappend container_commands [_descend_commands_pop]

    # Descend into body argument.
    set body_index [descend_container_while_body $key]

    if {[descend_container_argument_body_is_static $key $body_index]} {
        _descend_commands_push
        descend_container_body_argument $key $script $tree $body_index
        lappend container_commands [_descend_commands_pop]
    } else {
        lappend container_commands {}
    }
    set _descend($key,container_commands) $container_commands
}

# If the "while" command indicated by "key" has valid arguments,
# then return "OK". If any of the arguments are invalid
# then return an error message indicating the problem.

proc descend_container_while_validate { key } {
    global _descend

#    set debug 0

#    if {$debug} {
#        puts "descend_container_while_validate $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set num_args [llength $tree]

    # Usage: while test body

    if {$num_args != 3} {
        set _descend($key,usage) 1
        return [list numArgs "while" $num_args 3]
    }

    # Validate expr argument
    if {![descend_container_expr_argument_validate $key 1]} {
        set _descend($key,usage) 1
        return [list invalidArg 1 "expr" "braced expr argument required"]
    }

    # Determine if the body code is statically defined
    if {[descend_container_argument_body_is_static $key 2]} {
        set _descend($key,static_container) 1
    }

    # Validated while command is a list of length 2.
    set _descend($key,validated) [list 1 2]
    return "OK"
}

# Return EXPR_INDEX for while command.

proc descend_container_while_expr { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    return [lindex $validated 0]
}

# Return BODY_INDEX for while command

proc descend_container_while_body { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    return [lindex $validated 1]
}

# Invoked when a for command is discovered while descending into a
# Tcl script.

proc descend_container_for { key } {
    global _descend

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    # Validate arguments, ignore if invalid.
    set result [descend_container_for_validate $key]
    if {$result != "OK"} {
        # FIXME: Log errors somehow?

        if {[descend_get_data $key usage]} {
            descend_report_usage $key $result
        }

        return
    }

    set container_commands [list]

    # Descend into start argument
    set body_index [descend_container_for_start $key]
    if {[descend_container_argument_body_is_static $key $body_index]} {
        _descend_commands_push
        descend_container_body_argument $key $script $tree $body_index
        lappend container_commands [_descend_commands_pop]
    } else {
        lappend container_commands {}
    }

    # Descend into expr argument
    _descend_commands_push
    set expr_index [descend_container_for_expr $key]
    descend_container_expr_argument $key $expr_index
    lappend container_commands [_descend_commands_pop]

    # Descend into next argument
    set body_index [descend_container_for_next $key]
    if {[descend_container_argument_body_is_static $key $body_index]} {
        _descend_commands_push
        descend_container_body_argument $key $script $tree $body_index
        lappend container_commands [_descend_commands_pop]
    } else {
        lappend container_commands {}
    }

    # Descend into body argument.
    set body_index [descend_container_for_body $key]
    if {[descend_container_argument_body_is_static $key $body_index]} {
        _descend_commands_push
        descend_container_body_argument $key $script $tree $body_index
        lappend container_commands [_descend_commands_pop]
    }

    set _descend($key,container_commands) $container_commands
}

# If the "for" command indicated by "key" has valid arguments,
# then return "OK". If any of the arguments are invalid
# then return an error message indicating the problem.

proc descend_container_for_validate { key } {
    global _descend

#    set debug 0

#    if {$debug} {
#        puts "descend_container_for_validate $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set num_args [llength $tree]

    # Usage: for start test next body

    if {$num_args != 5} {
        set _descend($key,usage) 1
        return [list numArgs "for" $num_args 5]
    }

    # Validate expr argument
    if {![descend_container_expr_argument_validate $key 2]} {
        set _descend($key,usage) 1
        return [list invalidArg 2 "expr" "braced expr argument required"]
    }

    # Determine if the each body is statically defined
    set static 1
    foreach i {1 3 4} {
        if {![descend_container_argument_body_is_static $key $i]} {
            set static 0
        }
    }
    if {$static} {
        set _descend($key,static_container) 1
    }

    # Validated for command is a list of length 4
    set _descend($key,validated) [list 1 2 3 4]
    return "OK"
}

# Return BODY_INDEX for the start script argument.

proc descend_container_for_start { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    return [lindex $validated 0]
}

# Return EXPR_INDEX for the test expr argument.

proc descend_container_for_expr { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    return [lindex $validated 1]
}

# Return BODY_INDEX for the next script argument.

proc descend_container_for_next { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    return [lindex $validated 2]
}

# Return BODY_INDEX for the body script argument.

proc descend_container_for_body { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    return [lindex $validated 3]
}


# Invoked when a foreach command is discovered while descending into a
# Tcl script.

proc descend_container_foreach { key } {
    global _descend

#    set debug 0

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    # Validate arguments, ignore if invalid.
    set result [descend_container_foreach_validate $key]
    if {$result != "OK"} {
        # FIXME: Log errors somehow?

        if {[descend_get_data $key usage]} {
            descend_report_usage $key $result
        }

        return
    }

    # If the foreach command is iterating over a single
    # list, handle that case.

    if {[descend_container_foreach_has_single_list $key]} {
        # Handle the case of a single variable name.

        if {[descend_container_foreach_has_single_variable $key]} {
#            if {$debug} {
#                puts "foreach with single variable and single list"
#            }
        } else {
#            if {$debug} {
#                puts "foreach with multiple variables and single list"
#            }
        }
    } else {
        # Iterating over multiple lists
#        if {$debug} {
#            puts "foreach with single or multiple variables and multiple lists"
#        }
    }

    # Descend into body argument.
    set body_index [descend_container_foreach_body $key]
    if {[descend_container_argument_body_is_static $key $body_index]} {
        _descend_commands_push
        descend_container_body_argument $key $script $tree $body_index
        set _descend($key,container_commands) [_descend_commands_pop]
    } else {
        set _descend($key,container_commands) {}
    }
}

# If the "foreach" command indicated by "key" has valid arguments,
# then return "OK". If any of the arguments are invalid
# then return an error message indicating the problem.

proc descend_container_foreach_validate { key } {
    global _descend

#    set debug 0

#    if {$debug} {
#        puts "descend_container_foreach_validate $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set num_args [llength $tree]

    # Usage: foreach varname list body
    # Usage: foreach varlist1 list1 ?varlist2 list2 ...? body

    if {$num_args == 4} {
        # No-op
    } elseif {$num_args > 4 && (($num_args - 4) % 2) == 0} {
        # No-op
    } else {
        set _descend($key,usage) 1
        return [list numArgs foreach $num_args "4, 6, 8, ..."]
    }

    # Validated foreach command is a list of length 3
    # {TYPE {VARNAME_INDEX LIST_INDEX ...} {BODY_INDEX}}

    set is_static 1

    if {$num_args == 4} {
        # Check argument 1, a single variable name needs
        # to be marked differently than a list of variables.
        set varlist [descend_container_foreach_varlist $key 1]

        if {$varlist == "NOT_STATIC"} {
            # Var list needs to be evaluated at runtime
            set is_static 0
            set type "unknown"
        } elseif {[llength $varlist] == 0} {
            # Empty variable list, mark as usage error
            set _descend($key,usage) 1
            return [list listArg foreach "empty list arg"]
        } elseif {[llength $varlist] == 1} {
            set type "single"
        } else {
            set type "list"
        }
        set _descend($key,validated) [list $type {1 2} 3]
    } else {
        set varname_listname_list [list]
        set type "multi"
        set last_list_index [expr {$num_args - 1 - 1}]
        for {set i 1} {$i < $last_list_index} {incr i 2} {
            set var_list_index $i
            set var_values_index [expr {$i + 1}]
            # Check for empty variable list usage error
            set varlist [descend_container_foreach_varlist $key $var_list_index]
            if {$varlist == "NOT_STATIC"} {
                # Var list needs to be evaluated at runtime
                set is_static 0
            } elseif {[llength $varlist] == 0} {
                set _descend($key,usage) 1
                return [list listArg foreach "empty list arg"]
            }
            lappend varname_listname_list $var_list_index $var_values_index
        }
        set _descend($key,validated) [list \
            $type \
            $varname_listname_list \
            [expr {$last_list_index + 1}] \
            ]
    }
    # Determine if the body code is statically defined.
    if {$is_static} {
        if {![descend_container_argument_body_is_static $key \
                [lindex $_descend($key,validated) end]]} {
#            if {$debug} {
#                puts "body at index [lindex $_descend($key,validated) end] is NOT static"
#            }
            set is_static 0
        }
    }
    if {$is_static} {
        set _descend($key,static_container) 1
    }
    return "OK"
}

# Return true if the foreach command iterates over
# a single list.

proc descend_container_foreach_has_single_list { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    set type [lindex $validated 0]
    if {$type == "single" || $type == "list"} {
        return 1
    } else {
        return 0
    }
}

# Return true if the foreach command iterates over
# a single list with a single variable.

proc descend_container_foreach_has_single_variable { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    set type [lindex $validated 0]
    if {$type == "single"} {
        return 1
    } else {
        return 0
    }
}

# Return a list of variable names that were passed
# as a varlist option to the foreach command.
# If only a single variable name was given then
# it is returned.

proc descend_container_foreach_varlist { key varindex } {
#    set debug 0

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    # Parse argument at varindex into a list
    set subtree [lindex $tree $varindex]
    if {![parse_is_simple_text $subtree]} {
        return "NOT_STATIC"
    }
    set text_range [lindex $subtree 2 0 1]

#    if {$debug} {
#        puts "text range: \{$text_range\}"    
#        puts "parsing list from text range: \"[parse getstring $script $text_range]\""    
#    }

    if {[catch {parse list $script $text_range} list_ranges]} {
        # Mark command a non-static when parse error is found
        return {}
    }
    set var_names [list]
    foreach range $list_ranges {
        lappend var_names [parse getstring $script $range]
    }
    return $var_names
}

# Return list of {VARLIST_INDEX VARVALUES_INDEX} arguments.

proc descend_container_foreach_varlistvalues { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    return [lindex $validated 1]
}

# Return BODY_INDEX for the body script argument

proc descend_container_foreach_body { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    return [lindex $validated 2]
}

# Invoked when a switch command is discovered while descending into a
# Tcl script.

proc descend_container_switch { key } {
    global _descend

#    set debug 0

    # Validate arguments, ignore if invalid.
    set result [descend_container_switch_validate $key]
    if {$result != "OK"} {
        # FIXME: Log errors somehow?

        if {[descend_get_data $key usage]} {
            descend_report_usage $key $result
        }

        return
    }

    # Query the script and tree *after* validation since
    # a switch command can rewrite its script and reparse
    # its command arguments.

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set string_index [descend_container_switch_string $key]
#    if {$debug} {
#        puts "string_index is $string_index"
#    }

    set container_commands [list]

    # Descend into each switch body
    foreach {pat_index body_index} [descend_container_switch_patbody_indexes $key] {
#        if {$debug} {
#            puts "descending into switch body argument at index $body_index"
#        }

        # If the body command is the fallthrough token "-" then skip
        # descending into the body.

        if {[descend_container_switch_is_fallthrough $key $body_index]} {
            lappend container_commands {}
            continue
        }

        # If the body argument is static, then descend into it

        if {[descend_container_argument_body_is_static $key $body_index]} {
            _descend_commands_push
            descend_container_body_argument $key $script $tree $body_index
            lappend container_commands [_descend_commands_pop]
        } else {
            lappend container_commands {}
        }
    }

    set _descend($key,container_commands) $container_commands
}

# Return "" if the string argument is not one of the
# mode arguments (-exact -regexp or -glob). Return
# the full mode string if it is.

proc descend_container_switch_is_switch_mode { option_str } {
    set option ""
    switch -exact -- $option_str {
        "-e" -
        "-ex" -
        "-exa" -
        "-exac" -
        "-exact" {
            set option "exact"
        }
        "-g" -
        "-gl" -
        "-glo" -
        "-glob" {
            set option "glob"
        }
        "-r" -
        "-re" -
        "-reg" -
        "-rege" -
        "-regex" -
        "-regexp" {
            set option "regexp"
        }
    }
    return $option
}

# If the "switch" command indicated by "key" has valid arguments,
# then return "OK". If any of the arguments are invalid
# then return an error message indicating the problem.
# This validate command can also rewrite the switch script and
# reparse it, this is the only command that does this.

proc descend_container_switch_validate { key } {
    global _descend

#    set debug 0

#    if {$debug} {
#        puts "descend_container_switch_validate $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]
    set nested [descend_get_data $key nested]
    set parse_range [descend_get_data $key range]
    set undetermined 0 ; # assume reparse has known args
    set original_command_range [descend_get_data $key command]
    set original_rest_range [descend_get_data $key rest]

    set num_args [llength $tree]
    set original_num_args $num_args
    set original_commands [descend_get_data $key commands]

    # Usage: switch ?options? string pattern body ?pattern body ...?
    # Usage: switch ?options? string {pattern body ?pattern body ...?}

    if {$num_args < 3} {
        set _descend($key,usage) 1
        return [list numArgs switch $num_args]
    }

    set is_static 1

    # Check for -exact, -glob, -regexp, or --
    set option "default"
    set option_str ""
    set found_option_terminator 0
    set string_argument_index -1
    set patlist [list]
    set tIndex "" ; # Index of "--" end of option terminator
    set sIndex "" ; # Index of string argument
    set arg1_undetermined 0
    set arg2_undetermined 0

    set subtree [lindex $tree 1]
    if {[parse_is_simple_text $subtree]} {
        set option_str [parse_get_simple_text $script $subtree "text"]
        set mode_str [descend_container_switch_is_switch_mode $option_str]
        if {$mode_str != ""} {
            # mode_str is a valid mode option.
            set option $mode_str
        } else {
            # Not a mode option. Could be "--" or some invalid option.
            if {$option_str == "--"} {
                # option remains "default"
                set found_option_terminator 1
                set tIndex 1
            } elseif {[string index $option_str 0] == "-"} {
                # Start of some invalid option, fail validation
                # and raise the error in the switch command
                # at runtime.
                set _descend($key,usage) 1
                return [list badOption switch 1 $option_str]
            }
        }
    } else {
        # First argument could be a variable or a command
        set arg1_undetermined 1
    }

    # Examine second argument
    set subtree [lindex $tree 2]
    if {[parse_is_simple_text $subtree]} {
        set option_str [parse_get_simple_text $script $subtree "text"]
        if {$option_str == "--"} {
            # It is not actualy a usage error for "--" to appear after
            # an earlier "--", but we can't compile this usage so
            # just skip it.
            if {$found_option_terminator} {
                set _descend($key,usage) 1
                return [list badOption switch 2 $option_str]
            }
            set found_option_terminator 1
            set tIndex 2
        } elseif {!$found_option_terminator && \
                [descend_container_switch_is_switch_mode $option_str] != ""} {
            # A second valid mode option, fail validation
            # and raise the error in the switch command.
            set _descend($key,usage) 1
            return [list badOption switch 2 $option_str]
        }
    } else {
        # Second argument could be a variable or a command
        set arg2_undetermined 1
    }

    if {$num_args == 3} {
        # Could be "switch STRING PATLIST"

        if {$option != "default" || $found_option_terminator} {
            # If first argument was not the string, then
            # the second argument can't be the PATLIST.
            set _descend($key,usage) 1
            return [list badOption switch 2]
        }

        if {$arg2_undetermined} {
            # Can't descend into PATLIST unless it is a
            # brace quoted list.

            return [list nonStatic switch 2]
        }

        # Note: It is still possible that the string could
        # start with a "-" character. This would need to
        # be flagged as an error at runtime.

        set sIndex 1
        lappend patlist 2
    } elseif {$num_args == 4} {
        # Could be: "switch -exact STRING PATLIST"
        # Could be: "switch -- STRING PATLIST"
        # Could be: "switch STRING PAT BODY"

        if {$option == "default" && $found_option_terminator && $tIndex == 1} {
            # "switch -- STRING PATLIST"
            set sIndex 2
            lappend patlist 3
        } elseif {$option != "default" && !$found_option_terminator} {
            # "switch -exact STRING PATLIST"
            set sIndex 2
            lappend patlist 3
        } elseif {$option == "default" && !$found_option_terminator \
                && !$arg1_undetermined} {
            # "switch CONSTATNT_STRING PAT BODY"

            set sIndex 1
            lappend patlist 2 3
        } else {
            # Can't statically determine command usage.
            return [list nonStatic switch]
        }
    } elseif {$num_args == 5} {
        # Could be: "switch -exact -- STRING PATLIST"
        # Could be: "switch -- STRING PAT BODY"
        # Could be: "switch -exact STRING PAT BODY"

        # Check type of 3rd argument so we can tell
        # the difference between the following cases:
        # switch -exact $term $str PATLIST
        # and
        # switch -exact $str $pat PATBODY
        set subtree [lindex $tree 3]
        if {[parse_is_simple_text $subtree]} {
            set arg3_undetermined 0
        } else {
            set arg3_undetermined 1
        }

        if {$option != "default" && $found_option_terminator && $tIndex == 2} {
            # "switch -exact -- STRING PATLIST"
            set sIndex 3
            lappend patlist 4
        } elseif {$option != "default" && !$found_option_terminator && \
                !$arg3_undetermined} {
            # "switch -exact STRING PAT BODY"
            set sIndex 2
            lappend patlist 3 4
        } elseif {$option == "default" && $found_option_terminator && $tIndex == 1} {
            # "switch -- STRING PAT BODY"
            set sIndex 2
            lappend patlist 3 4
        } else {
            # Can't statically determine command usage.
            return [list nonStatic switch]
        }
    } elseif {$num_args >= 6} {
        # Could be: "switch -exact -- STRING PAT BODY ..."
        # Could be: "switch STRING PAT BODY PAT BODY ..."

        if {($option == "default" && \
                (($found_option_terminator && $arg1_undetermined) || \
                ($arg1_undetermined && $arg2_undetermined))) || \
                ($option != "default" && $arg2_undetermined)} {
            # "switch $opt -- STRING PAT BODY ..."
            # or
            # "switch $opt $term STRING PAT BODY ..."
            # or
            # "switch -exact $term STRING PAT BODY ..."

            # Can't statically determine command usage.
            return [list nonStatic switch]
        } elseif {$option != "default" && $found_option_terminator} {
            # "switch -exact -- STRING PAT BODY ..."
            set sIndex 3

            if {$num_args == 6} {
                lappend patlist 4 5
            } else {
                # More than 6 arguments
                if {(($num_args - 6) % 2) != 0} {
                    set _descend($key,usage) 1
                    return [list numArgs switch $num_args]
                } else {
                    lappend patlist 4 5
                    for {set i 6} {$i < $num_args} {incr i 2} {
                        lappend patlist $i [expr {$i + 1}]
                    }
                }
            }
        } elseif {$option == "default" && !$found_option_terminator} {
            # "switch STRING PAT BODY PAT BODY ..."
            set sIndex 1

            if {$num_args == 6} {
                lappend patlist 2 3 4 5
            } else {
                # More than 6 arguments
                if {(($num_args - 6) % 2) != 0} {
                    set _descend($key,usage) 1
                    return [list numArgs switch $num_args]
                } else {
                    lappend patlist 2 3 4 5
                    for {set i 6} {$i < $num_args} {incr i 2} {
                        lappend patlist $i [expr {$i + 1}]
                    }
                }
            }
        }
    }

    if {[llength $patlist] == 0} {
        error "patlist length can't be zero"
    } elseif {[llength $patlist] == 1} {
        # When a switch body is a list of multiple pattern/body
        # pairs, remove the braces and reparse the whole command
        # so that the pattern/body arguments become regular
        # command arguments.

        set pindex [lindex $patlist 0]
        set subtree [lindex $tree $pindex]
        # pattern/body list must be a simple/text string
        if {![parse_is_simple_text $subtree]} {
            return [list nonStatic switch $pindex]
        }
        # pattern/body must be enclosed in braces
        set braced_range [lindex $subtree 1]
        set braced_range_string [parse getstring $script $braced_range]
        if {[string index $braced_range_string 0] != "\{" ||
                [string index $braced_range_string end] != "\}"} {
            return [list nonStatic switch $pindex]
        }

        set text_range [lindex $subtree 2 0 1]

        # Check for non-zero even num elements in pattern/body list.
        if {[catch {parse list $script $text_range} result]} {
            set _descend($key,usage) 1
            return [list invalidBody switch $pindex "invalid pat/body argument"]
        }
        set list_ranges $result
        if {[llength $list_ranges] == 0} {
            set _descend($key,usage) 1
            return [list invalidBody switch $pindex "invalid pat/body argument"]
        }
        if {([llength $list_ranges] % 2) != 0} {
            set _descend($key,usage) 1
            return [list invalidBody switch $pindex "uneven pat/body args"]
        }

        # Check for comment inside pattern/body list. This
        # error might have been caught above, but a pattern
        # that starts with # could get through.

        set is_pattern 1
        foreach list_range $list_ranges {
            set str [parse getstring $script $list_range]
            if {[string index $str 0] == "#"} {
                set _descend($key,usage) 1
                return [list invalidBody switch $pindex "comment in pat/body argument"]
            }
            set is_pattern [expr {!$is_pattern}]
        }
        if {!$is_pattern} {error "expected to end on a body element"}

        # Last body can't be a continuation
        if {$str == "-"} {
            set _descend($key,usage) 1
            return [list invalidBody switch $pindex "no body for last pattern"]
        }

        # Recreate the script without the enclosing braces around the pat/body pairs.
        set unbraced_script [descend_container_switch_script_recreate \
            $script $text_range]

        # Unset any _descend array variables created by the
        # previous parse operation.
        foreach akey [array names _descend ${key},*] {
            unset _descend($akey)
        }
        unset akey

        # Save current key counter and reset it to the value it would
        # have been just before the switch was parsed. This is needed
        # so that the same key is created.
        set prev_key $key
        set saved_key $_descend(key)
        if {![regexp {dkey([0-9]+)} $key whole sub1]} {
            error "key \"$key\" does not match regexp pattern"
        }
        set _descend(key) $sub1

        # Adjust parse_range to account for change in the
        # unbraced_script length as compared to script.
        set original_range [parse getrange $script]
        set unbraced_range [parse getrange $unbraced_script]
#        if {$debug} {
#            puts "parse_range is \{$parse_range\}"
#            puts "original_range is \{$original_range\}"
#            puts "unbraced_range is \{$unbraced_range\}"            
#        }

        set original_range_len [lindex $original_range 1]
        set mod_range_len [lindex $unbraced_range 1]
        set delta_len [expr {($original_range_len - $mod_range_len) * -1}]
#        if {$debug} {
#            puts "delta_len is $delta_len"
#        }

        set unbraced_parse_range [list \
            [lindex $parse_range 0] \
            [expr {[lindex $parse_range 1] + $delta_len}] \
            ]

        # Now reparse the modified script so that the switch body
        # elements are parsed as braced arguments.

#        if {$debug} {
#            puts "reparsing switch script"
#            puts "original script \{$script\} with range \{$parse_range\}"
#            puts "unbraced script \{$unbraced_script\} with range \{$unbraced_parse_range\}"
#        }

        set key [descend_next_command $unbraced_script $unbraced_parse_range $nested $undetermined]

        if {$key == ""} {
            # Empty command should not be reported.
            error "descend_next_command reported empty commmand"
        } elseif {[llength $key] == 2 && [lindex $key 0] == "continue"} {
            # Empty command should not be reported.
            error "descend_next_command reported empty/continue commmand"
        } elseif {$key != $prev_key} {
            error "prev_key \"$prev_key\" does not match new key \"$key\""
        }

        # Reset the saved key id after regenerating the key
        set _descend(key) $saved_key

#        if {$debug} {
#            puts "reparsed key $key"
#            puts "original command range was \{$original_command_range\}"
#            puts "reparsed command range is \{[descend_get_data $key command]\}"
#            puts "reparsed command text is \{[parse getstring [descend_get_data $key script] [descend_get_data $key command]]\}"
#        }

        # Reset rest range, this needs to be in terms of the original
        # script so that parsing the next script works as expected.
        set _descend($key,rest) $original_rest_range

        # Update locals to new parse results
        #set script [descend_get_data $key script]
        set tree [descend_get_data $key tree]
        #set nested [descend_get_data $key nested]
        #set parse_range [descend_get_data $key range]
        set num_args [llength $tree]

        # Set validated info before the reparse
        set _descend($key,pre_validated) [list $option $tIndex $sIndex $patlist]

        # Set new validated with indexes of arguments that were just parsed
        #FIXME: "set patlist [list]" here? Or should we remove the last pattern?
        for {set i [expr {$pindex + 1}]} {$i < $num_args} {incr i} {
            lappend patlist $i
        }

        # Recalculate the "commands" data element, this is a listing of
        # the nested commands for each element. A reparse will never
        # add new nested commands. If any previously discovered nested
        # command exist, then add them to the list.
        set commands [list]
        for {set i 0} {$i < $num_args} {incr i} {
            lappend commands {}
        }
        for {set i 0} {$i < [llength $original_commands]} {incr i} {
            set oc [lindex $original_commands $i]
            if {[llength $oc] > 0} {
                set commands [lreplace $commands $i $i $oc]
            }
        }
        set _descend($key,commands) $commands

        # Set validated info after the reparse
        set _descend($key,validated) [list $option $tIndex $sIndex $patlist]
    } else {
        # Last body can't be a continuation
        set last_index [lindex $patlist end]

        if {[descend_container_argument_body_is_static $key $last_index]} {
            set subtree [lindex $tree $last_index]
            set str [parse_get_simple_text $script $subtree "text"]
            if {$str == "-"} {
                set _descend($key,usage) 1
                return [list invalidBody switch $last_index "no body for last pattern"]
            }
        }

        set _descend($key,validated) [list $option $tIndex $sIndex $patlist]
        set _descend($key,pre_validated) $_descend($key,validated)
    }

    # Loop over each pattern/body argument looking for non-static
    # body blocks. Each of the pattern blocks is always a static
    # string even if it looked like a non-static one originally.

    foreach {pIndex bIndex} $patlist {
        # Note: body blocks reparsed into arguments are always
        # going to be static strings.
        if {![descend_container_argument_body_is_static $key $bIndex]} {
#            if {$debug} {
#                puts "non-static body argument at $bIndex"
#            }
            set is_static 0
        }
    }
    if {$is_static} {
        set _descend($key,static_container) $is_static
    }

    # Save command range in original script
    set _descend($key,original_command) $original_command_range

    # Validated switch command is a list of length 3
    # {OPTION STRING_INDEX {{PATTERN_INDEX BODY_INDEX} ...}}

    return "OK"
}

# Recreate the script containing a switch command
# with a list of pattern/body elements. Elements
# that contain backslashed characters will be
# double quoted. Elements that contains no backslash
# characters will be brace quoted. The generated
# script is returned.

proc descend_container_switch_script_recreate { script range } {
#    set debug 0

#    if {$debug} {
#        puts "descend_container_switch_script_recreate \"$script\" \{$range\}"
#    }

    set range_start [lindex $range 0]
    set range_len [lindex $range 1]

    # Double check that the range passed in corresponds to a braced string
    set braced_range [list [expr {$range_start - 1}] [expr {$range_len + 2}]]
    set braced_range_string [parse getstring $script $braced_range]
    if {[string index $braced_range_string 0] != "\{" ||
            [string index $braced_range_string end] != "\}"} {
        error "range \{$braced_range\} is not a braced string"
    }

    set unbraced_script [parse getstring $script \
        [list 0 [expr {$range_start - 1}]]]

#    if {$debug} {
#        puts "unbraced_script(1) is \"$unbraced_script\""
#    }

    set extra_space 0
    set newlines_added 0

    # Get the list that contains the pattern/body elements.
    # If backslash newline sequences exist, then we need
    # to remove them before parsing as a list.

    set patbody_str [parse getstring $script $range]
    set no_bs_patbody_str [string map {"\\\n" ""} $patbody_str]
#    if {$debug} {
#        puts "patbody_str is\t\t\"$patbody_str\""
#        puts "no_bs_patbody_str is\t\"$no_bs_patbody_str\""
#    }
    if {[string length $no_bs_patbody_str] < [string length $patbody_str]} {
        set patbody_str $no_bs_patbody_str
    }

    # Parse elements out of patbody list. The list structure
    # would have already been verified at this point.

    set list_ranges [parse list $patbody_str {0 end}]
    set len [llength $patbody_str]
    if {[llength $list_ranges] != $len} {
        error "num list_ranges ([llength $list_ranges]]) != list length $len"
    }

    if {$len == 0} {
        error "no pattern/body elements found"
    }
    if {($len % 2) != 0} {
        error "expected even length for patbody list, got $len"
    }

    set is_pattern 1
    for {set i 0} {$i < $len} {incr i} {
        set list_range [lindex $list_ranges $i]
        set str [parse getstring $patbody_str $list_range]
        set lstr [lindex $patbody_str $i]

#        if {$debug} {
#        puts "processing patbody string ->$str-> ->$lstr<-, is_pattern is $is_pattern"
#        }

        # Convert the element to either a double quoted or
        # a brace quoted string.

        set first [string index $str 0]
        set last [string index $str end]

        if {($is_pattern && $str == "default") ||
                (!$is_pattern && $str == "-")} {
            # Use known switch arguments as-is.
            set unquoted_str $str
            set quoted_str $str
        } elseif {$first == "\"" && $last == "\""} {
            # Double quoted element
            set unquoted_str [string range $str 1 end-1]
            set quoted_str $str
        } elseif {$first == "\{" && $last == "\}"} {
            # Brace quoted element
            set unquoted_str [string range $str 1 end-1]
            set quoted_str $str
        } else {
            # Otherwise, treat as if double quoted
            set unquoted_str $str
            set quoted_str "\"$str\""
        }

#        if {$debug} {
#            puts "got unquoted_str ->$unquoted_str<-"
#            puts "got quoted_str ->$quoted_str<-"
#        }

        # Determine if a double quoted element contains
        # backslash characters. If it does not, then
        # convert it to a brace quoted constant string.

        set first [string index $quoted_str 0]
        set last [string index $quoted_str end]

        if {$first == "\"" && $last == "\""} {
            set bs_subst_str [subst -nocommands -novariables $unquoted_str]
            if {[string equal $unquoted_str $bs_subst_str]} {
                # No backslash subst
                set quoted_str "\{$unquoted_str\}"
#                if {$debug} {
#                    puts "no backslash subst, converted to brace quoted constant ->$quoted_str<-"
#                }
            } else {
                # There were backslash subst characters. If
                # the backslash subst does not match the
                # element returned by lindex, then something
                # is very wrong.
                if {![string equal $bs_subst_str $lstr]} {
                    error "bs_subst_str (1) ->$bs_subst_str<- != list index ->$lstr<-"
                }
                # Backslash '[', ']' and '$' characters in the unquoted_str
                # so that no command and variable subst are found when reparsing.
                set bs_subst_only ""
                set last_bs 0
                set unquoted_str_len [string length $unquoted_str]
                for {set ind 0} {$ind < $unquoted_str_len} {incr ind} {
                    set c [string index $unquoted_str $ind]
                    switch -exact -- $c {
                        {$} -
                        {[} -
                        {]} {
                            if {$last_bs} {
                                append bs_subst_only $c
                            } else {
                                append bs_subst_only "\\" $c
                            }
                            set last_bs 0
                        }
                        "\\" {
                            append bs_subst_only $c
                            if {$last_bs} {
                                # Double backslash
                                set last_bs 0
                            } else {
                                set last_bs 1
                            }
                        }
                        default {
                            append bs_subst_only $c
                            set last_bs 0
                        }
                    }
                }
                # Double check the generated string. There
                # should be no variable or command substs
                # in the string. It is not safe to run the
                # subst command since $bs_subst_only might
                # call methods in the compiler interp. Need to
                # parse the string as a word and see if
                # it is a word that contains backslash
                # substs only.
                if {[catch {parse command "\"$bs_subst_only\"" {0 end}} err]} {
                    error "parse error during generated word parse check : \"$bs_subst_only\" : $err"
                }
                set results $err
                set tree [lindex $results 3]
                if {[llength $tree] != 1} {
                    error "expected 1 tree element, got [llength $tree]"
                }
                set word [lindex $tree 0]
                set word_elems [lindex $word 2]
                foreach elem $word_elems {
                    set type [lindex $elem 0]
                    if {$type != "backslash" && $type != "text"} {
                        error "unexpected element type \"$type\", word_elems was \{$word_elems\}"
                    }
                }
                # If we only found text and backslash types in the word
                # the we are confident that there are no command or
                # variable substs in the generated string.
                set bs_subst_str [subst -nocommands -novariables $bs_subst_only]
                if {![string equal $bs_subst_str $lstr]} {
                    error "bs_subst_str (2) ->$bs_subst_str<- != list index ->$lstr<-"
                }
                set quoted_str "\"$bs_subst_only\""
            }
        }

#        if {$debug} {
#            puts "appending quoted_str >$quoted_str<-"
#        }

        append unbraced_script $quoted_str
        append unbraced_script " "
        set extra_space 1

        set newlines [parse countnewline $str {0 end}]
        if {$newlines > 0} {
#            if {$debug} {
#                puts "added $newlines newlines to unbraced_script"
#            }
            incr newlines_added $newlines
        }

        set is_pattern [expr {!$is_pattern}]
    }
    if {!$is_pattern} {
        error "expected to process a body block last"
    }

    # Trim last space off unbraced_script
    if {$extra_space} {
        set unbraced_script [string range $unbraced_script 0 end-1]
    }

#    if {$debug} {
#        puts "unbraced_script(2) is \"$unbraced_script\""
#    }

    # Fill with continued lines
    set numnewlines [parse countnewline $script $range]
    set newlines_needed [expr {$numnewlines - $newlines_added}]

#    if {$debug} {
#        puts "numnewlines is $numnewlines"
#        puts "newlines_added is $newlines_added"
#        puts "newlines_needed is $newlines_needed"
#    }

    for {set i 0} {$i < $newlines_needed} {incr i} {
        append unbraced_script "\\\n"
    }

    # Append any characters after the last brace in the original script
    set last_range [list [expr {$range_start + $range_len + 1}] end]
    set last_string [parse getstring $script $last_range]
#    if {$debug} {
#        puts "last_string is \"$last_string\""
#    }
    if {[string length $last_string] > 0} {
        append unbraced_script $last_string
    }

#    if {$debug} {
#    puts "script is\t\t\"$script\""
#    puts "unbraced_script is\t\"$unbraced_script\""
#    }

    # Check that number of lines in old script and new script are equal

    return $unbraced_script
}

# Return the mode of the switch command. If no option argument
# was passed to the switch command, then "default" will be
# returned. Otherwise, "exact", "glob", or "regexp" will be
# returned.

proc descend_container_switch_mode { key } {
#    set debug 0

#    if {$debug} {
#        puts "descend_container_switch_mode $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    return [lindex $validated 0]
}

# Return true if the switch command has a "--" token
# that indicates the last argument was given appears
# just before the string argument.

proc descend_container_switch_has_last { key } {
#    set debug 0

#    if {$debug} {
#        puts "descend_container_switch_has_last $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    if {[lindex $validated 1] == ""} {
        return 0
    } else {
        return 1
    }
}

# Return the index of the string argument to the switch command

proc descend_container_switch_string { key } {
#    set debug 0

#    if {$debug} {
#        puts "descend_container_switch_string $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    return [lindex $validated 2]
}

# Return list of {PATTERN_INDEX BODY_INDEX} argument indexes.

proc descend_container_switch_patbody_indexes { key } {
#    set debug 0

#    if {$debug} {
#        puts "descend_container_switch_patbody_indexes $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    return [lindex $validated 3]
}

# Return true if the switch has a pattern/body list
# before it was reparsed.

proc descend_container_switch_has_patbody_list { key } {
#    set debug 0

#    if {$debug} {
#        puts "descend_container_switch_has_patbody_list $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set validated [descend_get_data $key pre_validated]
    if {$validated == {}} {error "not validated"}
    set inds [lindex $validated 3]
    if {[llength $inds] == 1} {
        return 1
    } else {
        return 0
    }
}

# Return true if the body of a pattern/body in a switch command
# is the fall through token "-". This indicates that the
# body from the next switch pattern/body pair should be
# evaluated.

proc descend_container_switch_is_fallthrough { key bodyindex } {
#    set debug 0

#    if {$debug} {
#        puts "descend_container_switch_is_fallthrough $key $bodyindex"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}

    set subtree [lindex $tree $bodyindex]
    if {[parse_is_simple_text $subtree]} {
        set text [parse_get_simple_text $script $subtree text]
        if {$text == "-"} {
            return 1
        }
    }
    return 0
}



# Invoked when an after command is discovered while descending into a
# Tcl script.

proc descend_container_after { key } {
    global _descend

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    # Validate arguments, ignore if invalid.
    set result [descend_container_after_validate $key]
    if {$result != "OK"} {
        # FIXME: Log errors somehow?

        if {[descend_get_data $key usage]} {
            descend_report_usage $key $result
        }
        return
    }

    # If after command is a "ms" or "idle" subcommand
    # type, then descend into the concatenated arguments.

    if {[descend_container_after_has_command_indexes $key]} {
        # If some of the command arguments can't be statically
        # determined, then just try to reparse the command
        # name with undetermined arguments.
        _descend_commands_push

        if {[descend_container_is_static $key]} {
            descend_container_concat_body_arguments $key $script $tree \
                [descend_container_after_command_indexes $key]
        } else {
            set undetermined 1
            descend_container_body_argument $key $script $tree \
                [lindex [descend_container_after_command_indexes $key] 0] $undetermined
        }
        set _descend($key,container_commands) [_descend_commands_pop]
    }
}

# If the "after" command indicated by "key" has valid arguments,
# then return "OK". If any of the arguments are invalid
# then return an error message indicating the problem.

proc descend_container_after_validate { key } {
    global _descend

#    set debug 0

#    if {$debug} {
#        puts "descend_container_after_validate $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    # Usage: after ms
    # Usage: after ms ?script ...?
    # Usage: after cancel id
    # Usage: after cancel script ?script ...?
    # Usage: after idle script ?script ...?
    # Usage: after info ?id?

    if {$num_args < 2} {
        set _descend($key,usage) 1
        return [list numArgs "after" $num_args "2 or more"]
    }

    # Find subcommand type
    set type "unknown"
    set type_args {}
    set cmd_indexes {}

    set subtree [lindex $tree 1]
    if {[parse_is_simple_text $subtree]} {
        set text [parse_get_simple_text $script $subtree text]
#        if {$debug} {
#            puts "subcommand text is \"$text\""
#        }
        switch -regexp -- $text {
            "^[0-9]+$" {
                if {$num_args == 2} {
                    set type "ms_sleep"
                    set type_args $text
                } else {
                    set type "ms"
                    set type_args $text
                    for {set i 2} {$i < $num_args} {incr i} {
                        lappend cmd_indexes $i
                    }
                }
            }
            "^idle$" {
                set type "idle"
                if {$num_args == 2} {
                    set _descend($key,usage) 1
                    return [list numArgs "after" $num_args "3 or more"]
                }
                for {set i 2} {$i < $num_args} {incr i} {
                    lappend cmd_indexes $i
                }
            }
            "^cancel$" {
                set type $text
                if {$num_args == 2} {
                    set _descend($key,usage) 1
                    return [list numArgs "after" $num_args "3 or more"]
                }
                # When num_args is 3, can't determine if this is
                # an "after cancel id" or "after cancel cmd".
                # The command indexes are currently ignored so
                # ignore this issue for now.
            }
            "^info$" {
                set type $text
                if {$num_args > 3} {
                    set _descend($key,usage) 1
                    return [list numArgs "after" $num_args "2 or 3"]
                }
            }
            default {
                # Unknown after subcommand
                set _descend($key,usage) 1
                return [list badOption "after" 1 $text]
            }
        }
    } else {
        # Can't determine after subcommand
        return [list nonStatic "after" 1]
    }

    # Check each script argument to see if the command
    # is statically defined.

    if {$type == "ms" || $type == "idle"} {
        set static 1
        foreach index $cmd_indexes {
            set subtree [lindex $tree $index]
            if {![parse_is_simple_text $subtree]} {
                set static 0
            }
        }
        if {$static} {
            set _descend($key,static_container) 1
        }
    }

    # Validated after command is a list of length 3.
    # {type {type_args} {CMD_INDEX ...}}
    set _descend($key,validated) [list $type $type_args $cmd_indexes]
    return "OK"
}

# Return true for an after command invocation where a command
# to be evaluated later is passed into the after command.

proc descend_container_after_has_command_indexes { key } {
#    set debug 0

#    if {$debug} {
#        puts "descend_container_after_has_command_indexes $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}

    set type [lindex $validated 0]
    if {$type == "ms" || $type == "idle"} {
        return 1
    } else {
        return 0
    }
}

# Get the command argument indexes for an after command where
# descend_container_after_has_command_indexes return true.
# The first index is the command, each additional index
# corresponds to arguments to the command.

proc descend_container_after_command_indexes { key } {
#    set debug 0

#    if {$debug} {
#        puts "descend_container_after_command_indexes $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    return [lindex $validated 2]
}



# Invoked when an lsort command is discovered while descending into a
# Tcl script.

proc descend_container_lsort { key } {
    global _descend

    # Validate arguments, ignore if invalid.
    set result [descend_container_lsort_validate $key]
    if {$result != "OK"} {
        # FIXME: Log errors somehow?

        if {[descend_get_data $key usage]} {
            descend_report_usage $key $result
        }

        return
    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    # If lsort command has a -command argument, then
    # descend into it.

    if {[descend_container_lsort_has_command $key] && [descend_container_is_static $key]} {
        set command_index [descend_container_lsort_command $key]

        if {[descend_container_argument_body_is_static $key $command_index]} {
            # arguments for -command can't be determined statically, so indicate
            # this by passing the undetermined flag for the next parse.
            _descend_commands_push
            set undetermined 1
            descend_container_body_argument $key $script $tree $command_index $undetermined
            set _descend($key,container_commands) [_descend_commands_pop]
        }
    }
}

# If the "lsort" command indicated by "key" has valid arguments,
# then return "OK". If any of the arguments are invalid
# then return an error message indicating the problem.

proc descend_container_lsort_validate { key } {
    global _descend

#    set debug 0

#    if {$debug} {
#        puts "descend_container_after_validate $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    # Usage: lsort ?options? list

    if {$num_args < 2} {
        set _descend($key,usage) 1
        return [list numArgs "lsort" $num_args "2 or more"]
    }

    # If options includes "-command cmd" then save info.
    # Otherwise, ignore the arguments.
    set found_command_index -1
    set command_argument_index -1

    for {set i 1} {$i < $num_args} {incr i} {
        set subtree [lindex $tree $i]
        if {[parse_is_simple_text $subtree]} {
            set text [parse_get_simple_text $script $subtree text]
            switch -exact -- $text {
                "-c" -
                "-co" -
                "-com" -
                "-comm" -
                "-comma" -
                "-comman" -
                "-command" {
                    if {$found_command_index != -1} {
                        set _descend($key,usage) 1
                        return [list badOption "lsort" 1 $text]
                    }
                    set found_command_index $i
                }
            }
            # Last argument was -command
            if {$found_command_index != -1 && $found_command_index == ($i - 1)} {
                set command_argument_index $i
            }
        }
    }

    # Check the command to make sure it is statically defined

    if {$command_argument_index != -1} {
        set subtree [lindex $tree $command_argument_index]
        if {[parse_is_simple_text $subtree]} {
            set static 1
        } else {
            set static 0
        }
        set _descend($key,static_container) $static
        set _descend($key,validated) [list $command_argument_index]
    } else {
        set _descend($key,static_container) 0
        set _descend($key,validated) [list {}]
    }

    return "OK"
}

proc descend_container_lsort_has_command { key } {
#    set debug 0

#    if {$debug} {
#        puts "descend_container_lsort_has_command $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}

    set index [lindex $validated 0]

    if {$index == {}} {
        return 0
    } else {
        return 1
    }
}

# Return index of -commmand argument for lsort command

proc descend_container_lsort_command { key } {
#    set debug 0

#    if {$debug} {
#        puts "descend_container_lsort_has_command $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}

    set index [lindex $validated 0]
    return $index
}


# Descend into expr command looking for commands that should be run.

proc descend_container_expr { key } {
    global _descend

#    set debug 0

    # Validate arguments passed to expr command.
    # If the command is not valid, just ignore it.

    set result [descend_container_expr_validate $key]

    if {$result != "OK"} {
        # FIXME: Log errors somehow?

        if {[descend_get_data $key usage]} {
            descend_report_usage $key $result
        }

        return
    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    # Examine expr argument looking for commands
    # to descend into. This is implemented via
    # the expr iterator functionality in the parse
    # module.

    set arg [descend_container_expr_arg $key]
    _descend_commands_push
    descend_container_expr_argument $key $arg
    set _descend($key,container_commands) [_descend_commands_pop]

    return
}

# Descend into an expr argument to a command. This method assumes
# that the expr argument has already been validated.

proc descend_container_expr_argument { key argindex } {
#    set debug 0

#    if {$debug} {
#        puts "descend_container_expr_argument: $key $argindex"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set stree [lindex $tree $argindex]
    set range [parse_get_simple_text_range $stree text]
#    if {$debug} {
#        set range_script [parse_get_simple_text $script $stree text]
#        puts "parse expr \{$range_script\} in range \{$range\} at argument $argindex"
#    }
    set etree [parse expr $script $range]
    parse_expr_iterate $script $etree descend_container_expr_argument_iterate_callback
}

# Invoked once for each subexpression inside the expr argument tree.

proc descend_container_expr_argument_iterate_callback { script etree type values ranges } {
#    set debug 0

#    if {$debug} {
#        puts "descend_container_expr_iterate_callback:\
#            \"$script\" \{$etree\} \{$type\} \{$values\} \{$ranges\}"
#    }

    switch -exact -- $type {
        {literal operand} {
            # No-op
        }
        {command operand} {
            # Descend into command
            set cmd_script $script
            set cmd_range [lindex $ranges 0]

#            if {$debug} {
#                puts "descending into expr nested command \[[parse getstring $script $cmd_range]\]"
#            }

            _descend_commands_push
            descend_nested_subtree $script $etree
            _descend_commands_add [_descend_commands_pop]
        }
        {variable operand} {
            # Check variable elements for nested commands
            descend_check_variable_for_nested_commands $script $etree
        }
        {word operand} {
            # Check word elements for nested commands
            descend_check_word_for_nested_commands $script $etree
        }
        {unary operator} {
            # No-op
        }
        {binary operator} {
            # No-op
        }
        {ternary operator} {
            # No-op
        }
        {math function} {
            # No-op
        }
        {subexpression} {
            # No-op
        }
        {value} {
            # No-op
        }
        default {
            error "unknown type \"$type\""
        }
    }
}

# If the "expr" command indicated by "key" has valid arguments,
# then return "OK". If any of the arguments are invalid
# then return an error message indicating the problem.

proc descend_container_expr_validate { key } {
    global _descend

#    set debug 0

#    if {$debug} {
#        puts "descend_container_expr_validate $key"
#    }

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set num_args [llength $tree]

    if {$num_args != 2} {
        set _descend($key,usage) 1
        return [list numArgs "expr" $num_args "1 braced argument"]
    }

    set arg 1
    set valid [descend_container_expr_argument_validate $key $arg]
    if {!$valid} {
        set _descend($key,usage) 1
        return [list invalidArg $arg "expr" "braced expr argument required"]
    }

    # If expr was validated. then it must be a braced static expression.
    set _descend($key,static_container) 1

    # Validated expr command is a single index of an argument.
    # Don't bother trying to support funky unbraced expr
    # arguments or expr command invocations that have more
    # than one argument. The only exception to this is if
    # the unbraced literal string "1" or "0" is passed.

    set _descend($key,validated) {1}
    return "OK"
}

# Return true if the expr argument at the given index is valid.

proc descend_container_expr_argument_validate { key argindex } {
    global _descend
#    set debug 0

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]

    set stree [lindex $tree $argindex]
    if {![parse_is_simple_text $stree]} {
        return 0
    }
    set range [parse_get_simple_text_range $stree simple]
    if {![parse_is_braced $script $range]} {
        # Allow "1", "0", "true", and "false", reject everything else
        set simple [parse_get_simple_text $script $stree]
        switch -exact -- $simple {
            "1" -
            "0" -
            "true" -
            "false" {
                return 1
            }
            default {
                return 0
            }
        }
    }

    # If the expr can't be parsed, then don't try to descend into.
    set range [parse_get_simple_text_range $stree text]
#    if {$debug} {
#        set range_script [parse_get_simple_text $script $stree text]
#        puts "checking expr \{$range_script\} in range \{$range\} at argument $argindex"
#    }
    # FIXME: Need to output expr parse error. The user would want to know
    # about a real problem before running the code and we don't want to
    # just revert to simpler code if there is actually a bug in the
    # expr parser layer. This current code just covers up the problem.
    if {[catch {parse expr $script $range} err]} {
        # Error raise in parse expr, symtax error or some other problem
        return 0
    }
    return 1
}

# Return EXPR_INDEX for the braced argument passed to
# the expr command.

proc descend_container_expr_arg { key } {
    set validated [descend_get_data $key validated]
    if {$validated == {}} {error "not validated"}
    return [lindex $validated 0]
}

# Return true if the container command indicated by
# key passed the command validation tests. This
# command works for any of the constainer commands.

proc descend_container_is_valid { key } {
    if {[descend_get_data $key validated] == {}} {
        return 0
    } else {
        return 1
    }
}

# Return true if the container command identified by
# key is statically defined. This means that each
# body block is a list of commands that does not
# change at runtime. If the command contains
# expr bodies, each must be brace quoted.

proc descend_container_is_static { key } {
    global _descend
    return $_descend($key,static_container)
}

# Parse a simple variable name into a two part
# name. The variable name "a" would be parsed
# into {scalar a} while the variable name
# "a(b)" would be parsed into {array a b}.

proc descend_simple_variable { varname } {
    if {[regexp {^([^\(]+)\((.*)\)$} $varname whole part1 part2]} {
        # Array variable
        return [list "array" $part1 $part2]
    } else {
        return [list "scalar" $varname]
    }
}

# Tcl commands that act as containers for more code
# this means that they eval a brace quoted string
# of code passed to them.
#
# after (delay)         DONE (needs work)
# case                  UNSUPPORTED
# catch                 DONE
# eval
# expr                  DONE
# fileevent (delay)
# for                   DONE
# foreach               DONE
# if                    DONE
# interp eval
# lsort -command        DONE (needs work)
# namespace eval
# package ifneeded      UNSUPPORTED
# proc                  NOT-CONTAINER
# switch                DONE
# uplevel
# while                 DONE


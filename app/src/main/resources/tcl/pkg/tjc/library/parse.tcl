#
#  Copyright (c) 2005 Advanced Micro Devices, Inc.
#
#  See the file "license.amd" for information on usage and
#  redistribution of this file, and for a DISCLAIMER OF ALL
#   WARRANTIES.
#
#  RCS: @(#) $Id: parse.tcl,v 1.4 2006/05/29 21:56:14 mdejong Exp $
#
#

# This module contains functions that access the contents
# of a parse result. Most of the time, this will involve
# decoding a parse subtree to see what it contains.

# Return true if the subtree is a simple/text type.
# This method is called frequently by all the
# modules, so it is optimized so as to make it
# execute as quickly as possible.

proc parse_is_simple_text { stree } {
    if {[lindex $stree 0] ne "simple"} {
        return 0
    }

    # save simple range

    set range [lindex $stree 1]

    set stree [lindex $stree 2]

    if {[llength $stree] != 1} {
        return 0
    }

    # examine text subtree

    set stree [lindex $stree 0]

    if {[lindex $stree 0] ne "text"} {
        return 0
    }

    # If the simple_range matches the text_range, then
    # the text is unquoted. Otherwise, check that the
    # text_rage is a subrange of the simple_range
    # which can happen with quoted strings.

    if {$range ne [lindex $stree 1]} {
        if {![parse_is_subrange $range [lindex $stree 1]]} {
            return 0
        }
    }

    if {[lindex $stree 2] ne {}} {
        return 0
    }

    return 1
}

# Return a simple text string given a simple/text subtree
# and the script it was generated from. This function
# assumes that the parse_is_simple_text has already
# been invoked and has returned true for this subtree.
# The default rtype is "simple" meaning return the
# range defined in the simple tuple. If the rtype "text"
# is passed then the range defined by the text subtree
# element is defined. The text range can be a subrange
# of the simple range in the case of a quoted string.

proc parse_get_simple_text { script stree {rtype simple} } {
#    set debug 0

#    if {![parse_is_simple_text $stree]} {
#        error "not a simple text type : \{$stree\}"
#    }

    if {$rtype == "simple"} {
        set range [lindex $stree 1]
    } elseif  {$rtype == "text"} {
        set range [lindex $stree 2 0 1]
    } else {
        error "unknown rtype \"$rtype\""
    }

#    if {$debug} {
#        puts "range is \{$range\}"
#    }

    return [parse getstring $script $range]
}

# Like above, but return the range

proc parse_get_simple_text_range { stree {rtype simple} } {
#    if {![parse_is_simple_text $stree]} {
#        error "not a simple text type : \{$stree\}"
#    }

    if {$rtype == "simple"} {
        set range [lindex $stree 1]
    } elseif  {$rtype == "text"} {
        set range [lindex $stree 2 0 1]
    } else {
        error "unknown rtype \"$rtype\""
    }

    return $range
}

# Return 1 if the inner range is contained inside the
# outer range. For example, a quoted string has
# an outer range that includes the quotes and
# an inner range that does not. This method returns
# 1 if the ranges are identical.

proc parse_is_subrange { outer inner } {
    if {$outer == $inner} {
        return 1
    }

    set outer_start [lindex $outer 0]
    set outer_len [lindex $outer 1]
    set outer_end [expr {$outer_start + $outer_len}]

    set inner_start [lindex $inner 0]
    set inner_len [lindex $inner 1]
    set inner_end [expr {$inner_start + $inner_len}]

    if {($inner_start < $outer_start) ||
            ($inner_end > $outer_end)} {
        return 0
    }
    return 1
}

# Return 1 if this is a "word" type. A word type can contain
# nested commands and variables.

proc parse_is_word { stree } {
    if {![string equal "word" [lindex $stree 0]]} {
#        set word [lindex $stree 0]
#        puts "parse_is_word: type was \"$word\", expected \"word\""
        return 0
    }
    return 1
}

# Return 1 if this is a word/command type. This is the type used
# for a nested command as a command argument.

proc parse_is_word_command { stree } {
#    set debug 0

    set word [lindex $stree 0]
    if {![string equal "word" $word]} {
#        if {$debug} {
#            puts "parse_is_word_command: type was \"$word\", expected \"word\""
#        }
        return 0
    }

    set word_range [lindex $stree 1]

    set subtree [lindex $stree 2]
    if {[llength $subtree] != 1} {
#        if {$debug} {
#            puts "parse_is_word_command: subtree length was [llength $subtree], expected 1"
#        }
        return 0
    }

    set command_tuple [lindex $subtree 0]
    set command [lindex $command_tuple 0]
    set command_range [lindex $command_tuple 1]
    set command_subtree [lindex $command_tuple 2]

    if {![string equal "command" $command]} {
#        if {$debug} {
#            puts "parse_is_word_command: command was \"$command\", expected \"command\""
#        }
        return 0
    }

    # Assume that the word_range is the same as the command_range

    if {$word_range != $command_range} {
#        if {$debug} {
#            puts "parse_is_word_command: word_range \{$word_range\} not equal to command_range \{$command_range\}"
#        }
        return 0
    }

    if {[llength $command_subtree] != 0} {
#        if {$debug} {
#            puts "parse_is_word_command: command_subtree was \{$command_subtree\}, expected \{\}"
#        }
        return 0
    }

    return 1
}

# Return the nested command text for a word/command type
# given a subtree and the script it was generated from.
# This function assumes that parse_is_word_command
# has already been invoked and returned true for this subtree.

proc parse_get_word_command { script stree } {
#    if {![parse_is_word_command $stree]} {
#        error "not a word command type : \{$stree\}"
#    }

    set range [lindex $stree 1]
    return [parse getstring $script $range]
}

# Return 1 if this is a word/variable type. This is the type used
# for a command argument that is a variable.

proc parse_is_word_variable { stree } {
#    set debug 0

    set word [lindex $stree 0]
    if {![string equal "word" $word]} {
#        if {$debug} {
#            puts "parse_is_word_variable: type was \"$word\", expected \"word\""
#        }
        return 0
    }

    set word_range [lindex $stree 1]

    set subtree [lindex $stree 2]
    if {[llength $subtree] != 1} {
#        if {$debug} {
#            puts "parse_is_word_variable: subtree length was [llength $subtree], expected 1"
#        }
        return 0
    }

    set variable_tuple [lindex $subtree 0]

    # Validate variable type but don't attempt to validate
    # the subtree elements.
    set is_variable [parse_is_variable $variable_tuple]
    if {!$is_variable} {
#        if {$debug} {
#            set variable [lindex $variable_tuple 0]
#            puts "parse_is_word_variable: variable was \"$variable\", expected \"variable\""
#        }
        return 0
    }

    set variable_range [lindex $variable_tuple 1]

    # Assume that the word_range is the same as the variable_range

    if {$word_range != $variable_range} {
#        if {$debug} {
#            puts "parse_is_word_variable: word_range \{$word_range\} not equal to variable_range \{$variable_range\}"
#        }
        return 0
    }

    return 1
}

# Return word variable text string given a word/variable subtree
# and the script it was generated from. This function
# assumes that the parse_is_word_variable has already
# been invoked and has returned true for this subtree.
# The default rtype is "variable" meaning return the
# range defined in the variable tuple which includes the
# dollar sign. If the "text" rtype is passed, then only
# the variable name after the dollar sign is returned.

proc parse_get_word_variable { script stree {rtype variable} } {
#    if {![parse_is_word_variable $stree]} {
#        error "not a word variable type : \{$stree\}"
#    }

    set variable_subtree [lindex $stree 2 0]
    return [parse_get_variable $script $variable_subtree $rtype]
}

# Return variable text string given a variable subtree
# and the script it was generated from. This function
# assumes that the parse_is_variable has already
# been invoked and has returned true for this subtree.
# The default rtype is "variable" meaning return the
# range defined in the variable tuple which includes the
# dollar sign. If the "text" rtype is passes then only
# the variable name after the dollar sign is returned.

proc parse_get_variable { script stree {rtype variable} } {
#    set debug 0

#    if {![parse_is_variable $stree]} {
#        error "not a variable type : \{$stree\}"
#    }

#    if {$debug} {
#        puts "parse_get_variable: \"$script\" \{$stree\} and rtype $rtype"
#    }

    if {$rtype == "variable"} {
        set range [lindex $stree 1]
    } elseif  {$rtype == "text"} {
        set variable_subtree [lindex $stree 2]
#        if {$debug} {
#            puts "variable_subtree is \{$variable_subtree\}"
#        }
        if {[llength $variable_subtree] == 1} {
            # Scalar variable
            set range [lindex $variable_subtree 0 1]
        } else {
            # Array variable
            set r1 [lindex $variable_subtree 0 1]
            set r2 [lindex $variable_subtree end 1]
            set range [list]
            lappend range [lindex $r1 0]
            set r2_start [lindex $r2 0]
            set r2_len [lindex $r2 1]
            lappend range [expr {($r2_start + $r2_len + 1) - [lindex $r1 0]}]
        }
    } else {
        error "unknown rtype \"$rtype\""
    }

#    if {$debug} {
#        puts "range is \{$range\}"
#    }

    return [parse getstring $script $range]
}

# Return true if the subtree is a text type.

proc parse_is_text { stree } {
    return [expr {[lindex $stree 0] == "text"}]
}

# Return the text string indicated by a text subtree
# given the subtree and the script it was generated from.

proc parse_get_text { script stree } {
#    set debug 0

    if {![parse_is_text $stree]} {
        error "not a text type stree : \{$stree\}"
    }

    set range [lindex $stree 1]

#    if {$debug} {
#        puts "range is \{$range\}"
#        puts "will return range string \"[parse getstring $script $range]\""
#    }

    return [parse getstring $script $range]
}

# Given a nested command invocation like "set i [foo]",
# return the range for "foo" (inside the brackets).

proc parse_range_without_brackets { script range } {
    set range_text [parse getstring $script $range]
    set first [string index $range_text 0]
    set last [string index $range_text end]

    if {$first == "\[" && $last == "\]"} {
        # Looks ok
    } else {
        error "parse_range_without_brackets: range_text \"$range_text\" should be enclosed in brackets"
    }

    set range_start [lindex $range 0]
    set range_len [lindex $range 1]

    return [list [expr {$range_start + 1}] [expr {$range_len - 2}]]
}

# Return true if the text denoted by range is enclosed in braces.

proc parse_is_braced { script range } {
    set range_text [parse getstring $script $range]
    set first [string index $range_text 0]
    set last [string index $range_text end]

    if {$first == "\{"} {
        if {$last == "\}"} {
            return 1
        } else {
            return 0
        }
    }
    return 0
}

# Given a range for a braced string like "{foo}",
# return the range for "foo" (inside the braces).

proc parse_range_without_braces { script range } {
    set range_text [parse getstring $script $range]
    set first [string index $range_text 0]
    set last [string index $range_text end]

    if {$first == "\{" && $last == "\}"} {
        # Looks ok
    } else {
        error "parse_range_without_braces: range_text \"$range_text\" should be enclosed in braces"
    }

    set range_start [lindex $range 0]
    set range_len [lindex $range 1]

    return [list [expr {$range_start + 1}] [expr {$range_len - 2}]]
}

# Return true if the given stree indicates a constant value
# text element. For example, a simple/text element is a
# constant word. A word element that is braces would be
# treated as a simple/text except for the special case of
# a backslash newline subst. If a backslash newline subst
# if found, then treat it as a constant. An unbraced string
# that contains regular backslash elements like \t would
# not be considered constant here.

proc parse_is_constant { script stree } {
#    set debug 0

#    if {$debug} {
#        puts "parse_is_constant \{$stree\}"
#    }

    if {[parse_is_simple_text $stree]} {
#        if {$debug} {
#            puts "parse_is_constant returning 1 since type is simple/text"
#        }
        return 1
    }
    if {![parse_is_word $stree]} {
#        if {$debug} {
#            puts "parse_is_constant returning 0 since type is not a word"
#        }
        return 0
    }
    #set word_stree [lindex $stree 2]
#    if {$debug} {
#        puts "parse_is_constant will iterate over word"
#    }
    set result [parse_word_iterate \
        $script $stree _parse_is_constant_word_iterate]
#    if {$debug} {
#        puts "parse_is_constant done with word iteration"
#    }

    if {$result == "NONCONSTANT"} {
        # Not a constant word
#        if {$debug} {
#            puts "parse_word_iterate indicates failing \"$result\" result"
#        }
        return 0
    } else {
        # Returned a constant word
#        if {$debug} {
#            puts "parse_word_iterate indicates successful \"$result\" result"
#        }
        return 1
    }
}

# Invoked while iterating over the elements of a word
# during parse_is_constant. This method checks that
# a word is made up of either constant elements
# or the special backslash newline element.

proc _parse_is_constant_word_iterate { script stree type values ranges } {
#    set debug 0

#    if {$debug} {
#        puts "_parse_is_constant_word_iterate : $type \{$values\} \{$ranges\}"
#    }

    # Ignore rest of elements if iteration indicated a non-constant element.
    set word [parse_word_iterate_word_value]
    if {$word == "NONCONSTANT"} {
        return
    }

    switch -exact -- $type {
        {backslash} {
            # A backslash is only constant if it is a backslash newline
            set elem [lindex $values 0]
            if {[regexp "^\n\[ |\t\]*\$" $elem whole]} {
#                if {$debug} {
#                    puts "found backslash newline pattern match \"$whole\""
#                }
                set elem " "
                set word [parse_word_iterate_word_value]
#                if {$debug} {
#                    puts "appending text \"$elem\" to existing word \"$word\""
#                }
                append word $elem
                parse_word_iterate_word_value $word
            } else {
                # Any other backslash element is considered non-constant
                parse_word_iterate_word_value "NONCONSTANT"
            }
        }
        {command} {
            # A constant word can't contain a nested command.
            parse_word_iterate_word_value "NONCONSTANT"
        }
        {text} {
            set elem [lindex $values 0]
            set word [parse_word_iterate_word_value]
#            if {$debug} {
#                puts "appending text \"$elem\" to existing word \"$word\""
#            }
            append word $elem
            parse_word_iterate_word_value $word
        }
        {variable} {
            # A constant word can't contain a variable.
            parse_word_iterate_word_value "NONCONSTANT"
        }
        {word begin} {
            # No-op
        }
        {word end} {
            # No-op
        }
        {word} {
            # No-op
        }
        default {
            error "unknown type \"$type\""
        }
    }

#    if {$debug} {
#        puts "_parse_is_constant_word_iterate returning"
#    }
}

# This method assumes that parse_is_constant has already
# returned true for this stree. The method will return
# a list of elements. The first is the constant string.
# The second is the quote characters, either "", "{}",
# or "\"\"". The third element is the string without
# the surrounding quote characters.

proc parse_get_constant_text { script stree } {
#    set debug 0

#    if {$debug} {
#        puts "parse_get_constant_text"
#    }

    if {[parse_is_simple_text $stree]} {
#        if {$debug} {
#            puts "returning simple/text type info"
#        }
        set quoted [parse_get_simple_text $script $stree]

        set first [string index $quoted 0]
        set last [string index $quoted end]
        if {$first == "\{" && $last == "\}"} {
            set qchars "\{\}"
        } elseif {$first == "\"" && $last == "\""} {
            set qchars "\"\""
        } else {
            set qchars ""
        }

        set unquoted [parse_get_simple_text $script $stree "text"]
    } elseif {[parse_is_word $stree]} {
        set result [parse_word_iterate \
            $script $stree _parse_is_constant_word_iterate]

        if {$result == "NONCONSTANT"} {
            # Not a constant word
            error "expected to iterate over a constant word"
        }

        # Returned a constant word
#        if {$debug} {
#            puts "parse_word_iterate indicates successful \"$result\" result"
#        }

        # Recover quote element from word range.
        set unquoted $result
        set word_range [lindex $stree 1]
        set str [parse getstring $script $word_range]

        set first [string index $str 0]
        set last [string index $str end]

        if {$first == "\{" && $last == "\}"} {
            set qchars "\{\}"
            set quoted "\{$result\}"
        } elseif {$first == "\"" && $last == "\""} {
            set qchars "\"\""
            set quoted "\"$result\""
        } else {
            set qchars ""
            set quoted $result
        }
    } else {
        error "not a word or simple/text type \{$stree\}"
    }

#    if {$debug} {
#        puts "parse_get_constant_text returning \{$quoted $qchars $unquoted\}"
#    }

    return [list $quoted $qchars $unquoted]
}

# Combine range of each tree element to get the range of the
# whole tree.

proc parse_range_of_tree { stree {first 0} {last end} } {
    set rstart [lindex $stree $first 1]
    set rend [lindex $stree $last 1]

    set range [list]
    set rstart_start [lindex $rstart 0]
    lappend range $rstart_start

    set rend_start [lindex $rend 0]
    set rend_len [lindex $rend 1]
    # FIXME: parse_get_variable required adding one to the
    # len while this does not. Why is that?
    lappend range [expr {($rend_start + $rend_len) - $rstart_start}]
    return $range
}

# Return true if the subtree is a variable type

proc parse_is_variable { stree } {
#    puts "parse_is_variable : \{$stree\}"

    if {[lindex $stree 0] ne "variable"} {
        return 0
    }

    set range [lindex $stree 1]
    if {[llength $range] != 2} {
        error "unexpected range \{$range\} for variable subtree \{$stree\}"
    }    
    if {[lindex $stree 2] == {}} {
        error "unexpected empty inner_tree for variable subtree \{$stree\}"
    }

    # Don't try to examine the contents of the subtree since
    # that would require recursion into a word key and this
    # method needs to be useful while recursing elsewhere.

    return 1
}

# Return true if a variable subtree is for a scalar
# variable.

proc parse_is_scalar_variable { stree } {
    if {![parse_is_variable $stree]} {
        return 0
    }
    set variable_tree [lindex $stree 2]
    if {[llength $variable_tree] == 1 \
            && [lindex $variable_tree 0 0] == "text"} {
        return 1
    }
    return 0
}

# Return name of scalar variable in stree. This
# is much faster than setting up a whole variable
# scan for a simple scalar variable.

proc parse_get_scalar_variable { script stree } {
#    if {![parse_is_scalar_variable $stree]} {
#        error "not a scalar variable"
#    }
    set variable_tree [lindex $stree 2]
    set range [lindex $variable_tree 0 1]
    return [parse getstring $script $range]
}

# Given a subtree for a variable type, return a
# list describing the variable and any additional
# elements contained inside the word key.

proc parse_get_variable_list { script stree } {
    global _parse
    set _parse(get_variable_list_results) [list]
    set _parse(get_variable_list_word) [list]

    parse_variable_iterate $script $stree \
        _parse_get_variable_list_callback

    return $_parse(get_variable_list_results)
}

# Callback invoked as a result of parsing a variable.

proc _parse_get_variable_list_callback { script stree type values ranges } {
    global _parse
    set result [list "variable"]

    # Use array type info from parse layer to decide how to
    # handle the variable.

    switch -exact -- $type {
        {scalar} {
            # Scalar variable: {v}
            lappend result [lindex $values 0]
        }
        {array text} {
            # Array with a text string key: {a k}
            lappend result $values
        }
        {array scalar} {
            # Array with a scalar variable key: {a {variable k}}
            lappend result [list \
                [lindex $values 0] \
                [list "variable" [lindex $values 1]] \
                ]
        }
        {array command} {
            # Array with a command key: {a {command cmd}}
            lappend result [list \
                [lindex $values 0] \
                [list "command" [lindex $values 1]] \
                ]
        }
        {array word} {
            # complex array key case, either a word made
            # up of text, command, and variable elements
            # or an array key that is itself an array.
            lappend result [concat \
                [lindex $values 0] \
                $_parse(get_variable_list_word) \
                ]
        }
        {word begin} {
            # Begin processing word elements for complex
            # word as array key
             set _parse(get_variable_list_word) [list]
        }
        {word end} {
            # End processing of word elements for complex
            # word as array key
        }
        {word text} {
            # word element that is a text string
            lappend _parse(get_variable_list_word) \
                [list "text" [lindex $values 0]]
        }
        {word scalar} {
            # word element that is a scalar variable
            lappend _parse(get_variable_list_word) \
                [list "variable" [lindex $values 0]]
        }
        {word command} {
            # word element that is a command
            lappend _parse(get_variable_list_word) \
                [list "command" [lindex $values 0]]
        }
        {word array text} {
            # word element that is an array variable with a text key
            lappend _parse(get_variable_list_word) \
                [list "variable" $values]
        }
        {word array scalar} {
            # word element that is an array variable with a scalar key
            lappend _parse(get_variable_list_word) \
                [list "variable" [lindex $values 0] \
                    [list "variable" [lindex $values 1]]]
        }
        {word array command} {
            # word element that is an array variable with a command key
            lappend _parse(get_variable_list_word) \
                [list "variable" [lindex $values 0] \
                    [list "command" [lindex $values 1]]]
        }
        {word array word} {
            # word element that is an array with a word key
            lappend result \
                [concat \
                    [lindex $values 0] \
                    $_parse(get_variable_list_word) \
                ]
        }
        default {
            error "unknown type \{$type\}"
        }
    }

    if {[llength $result] > 1} {
        foreach elem $result {
            lappend _parse(get_variable_list_results) $elem
        }
    }
}

# Convert a variable inner tree to a list of types contained inside
# the inner tree.

proc _parse_variable_subtree_types { stree } {
#    set debug 0

    set inner_type_list [list]

    set len [llength $stree]
    for {set i 0} {$i < $len} {incr i} {
        set inner_type [lindex $stree $i 0]
        set inner_stree [lindex $stree $i 2]

        set entry [list]
        lappend entry $inner_type

        set inner_len [llength $inner_stree]
        for {set j 0} {$j < $inner_len} {incr j} {
            set inner_type [lindex $inner_stree $j 0]
            lappend entry $inner_type
        }

        if {[llength $entry] == 1} {
            lappend inner_type_list [lindex $entry 0]
        } else {
            lappend inner_type_list $entry
        }
    }

#    if {$debug} {
#        puts "returning inner types list \{$inner_type_list\} for \{$stree\}"
#    }

    return $inner_type_list
}


# Return true if the subtree is a command type.

proc parse_is_command { stree } {
    return [expr {[lindex $stree 0] == "command"}]
}

# Return the command text inside the brackets for a command type.

proc parse_get_command { script stree } {
#    set debug 0

    if {![parse_is_command $stree]} {
        error "not a command type stree : \{$stree\}"
    }

    set range [parse_range_without_brackets $script [lindex $stree 1]]

#    if {$debug} {
#        puts "range is \{$range\}"
#        puts "will return range string \"[parse getstring $script $range]\""
#    }

    return [parse getstring $script $range]
}

# Iterate over the contents of a variable. Pass
# the variable parse subtree and the script it
# was generated from along with the name of
# a callback command to invoke.

proc parse_variable_iterate { script stree cmd } {
    global _parse

#    set debug 0

#    if {$debug} {
#        puts "parse_variable_iterate:"
#    }

    # Save current word value and reset it when returning
    # in case parse_variable_iterate is invoked recursively
    # while building a word value.
    set saved_word_value [parse_variable_iterate_word_value]

    # Stack will contain keys that identify the variable
    # that holds command info for a specific array.
    foreach key [array names _parse key*] {
        unset _parse($key)
    }
    set key key0
    set _parse($key) [list]
    set _parse(parse_variable_iterate_stack) [list $key]
    set _parse(parse_variable_iterate_key) 1

    _parse_variable_iterate $script $stree $cmd [list 0 $key]

    # Loop over keys to create cmdstack.
    set cmdstack [list]
    foreach key $_parse(parse_variable_iterate_stack) {
        set cmdlist $_parse($key)
#        if {$debug} {
#            puts "parse_variable_iterate: key $key cmdlist is \{$cmdlist\}"
#        }
        lappend cmdstack $cmdlist
    }

    # Make sure old results don't get reused somehow

    foreach key [array names _parse key*] {
        unset _parse($key)
    }
    set _parse(parse_variable_iterate_stack) [list]
    set _parse(parse_variable_iterate_key) 0

#    if {$debug} {
#        puts "parse_variable_iterate: cmdstack length is [llength $cmdstack]"
#    }

    # eval each callback in the cmdstack list, it is important
    # that this command callback iteration be done after all
    # the globals used during the iteration have been dealt
    # with so that recursive calls to parse_variable_iterate
    # work correctly.

    foreach cmdlist $cmdstack {
#        if {$debug} {
#            puts "new cmdlist level --- num commands is [llength $cmdlist]"
#        }
        foreach cmd $cmdlist {
            # Command lists contain the following elements:
            # CMD SCRIPT STREE TYPE VALUES RANGES

#            if {$debug} {
#                puts "parse_variable_iterate: cmd is \{$cmd\}"
#            }

            # Verify that the command has the correct number of
            # arguments. We get a really confusing error message
            # during the eval step later on if the wrong number
            # of arguments is passed.

            if {[llength $cmd] != 6} {
                error "expected 6 arguments, callback format is\
                    CMD SCRIPT STREE TYPE VALUES RANGES\
                    but cmd was \{$cmd\}"
            }

            # When invoking callback, reset the word value if a
            # callback that will pass {word begin} is found.
            # When {word end} will be passed, save the current
            # word value so that it can be used to substitute
            # for the __WORD_VALUE__ placeholder. The VALUES
            # argument is the only element that can contain
            # a __WORD_VALUE__ placeholder.

            set atype [lindex $cmd 3]
            set avalues [lindex $cmd 4]
            if {$atype == {word begin}} {
                parse_variable_iterate_word_value {}
            } elseif {$atype == {word end}} {
                set word_value [parse_variable_iterate_word_value]
            } elseif {[string first __WORD_VALUE__ $avalues] != -1} {
                if {![info exists word_value]} {
                    error "__WORD_VALUE__ placeholder found with no previous {word begin/end}"
                }
                if {$word_value == {}} {
                    set new_avalues [string map {__WORD_VALUE__ {{}}} $avalues]
                } else {
                    set word_value [list $word_value]
                    set new_avalues [string map [list __WORD_VALUE__ $word_value] $avalues]
                }
                set new_cmd [lrange $cmd 0 3]
                lappend new_cmd $new_avalues
                lappend new_cmd [lindex $cmd 5]
                set cmd $new_cmd
#                if {$debug} {
#                    puts "parse_variable_iterate: replaced __WORD_VALUE__ with \"$word_value\""
#                    puts "parse_variable_iterate: word replaced cmd is \{$cmd\}"
#                }
            }

            # Evaluate specific command and take extra care to ensure
            # that it is passed as a pure list to avoid a costly reparse.

            namespace eval :: [lrange $cmd 0 end]
        }
    }

    # FIXME: Seems to be a problem with parse_variable_iterate_word_value
    # here. How would the caller for a recursive iteration find out
    # that the result word value would be since we reset the saved value
    # here. If the value were queried in the {word end}, that could
    # work, but it would likely be better to return the value here.
    # Figure out how to test this and make this iterator work more
    # like the word iterator.

    parse_variable_iterate_word_value $saved_word_value
}

proc _parse_variable_iterate { script stree cmd private } {
    global _parse

#    set debug 0

    set iterating_word [lindex $private 0]
    set key [lindex $private 1]

#    if {$debug} {
#        puts "_parse_variable_iterate:"
#        puts "script \"$script\""
#        puts "stree \{$stree\}"
#        puts "cmd \{$cmd\}"
#        puts "iterating_word is $iterating_word"
#        puts "key is $key"
#    }

    set cmdlist $_parse($key)
    set cmdindex 0

#    if {$debug} {
#        puts "_parse_variable_iterate: cmdlist is"
#        if {[llength $cmdlist] == 0} {
#            puts "{}"
#        } else {
#            set i 0
#            foreach elem $cmdlist {
#                puts "cmd ($i) is \{$elem\}"
#                incr i
#            }
#        }
#    }

    set type [lindex $stree 0]
    if {$type != "variable"} {
        error "expected \"variable\" type, got \"$type\""
    }

    set range [lindex $stree 1]
    if {[llength $range] != 2} {
        error "expected range of length 2, got \{$range\}"
    }
#    if {$debug} {
#        puts "variable range is \{$range\}"
#        puts "variable range text inside script is \"[parse getstring $script $range]\""
#    }

    set inner_tree [lindex $stree 2]
    set inner_tree_types [_parse_variable_subtree_types $inner_tree]

#    if {$debug} {
#        puts "inner_tree is \{inner_tree\}"
#        puts "inner_tree_types is \{inner_tree_types\}"
#    }

    if {$inner_tree_types == {}} {
        error "empty inner tree types for stree \{$stree\}"
    } elseif {$inner_tree_types == {text}} {
        # scalar variable: either scalar or scalar word element

        set inner_range [lindex $inner_tree 0 1]
        set varname [parse getstring $script $inner_range]

        if {!$iterating_word} {
            set atype {scalar}
        } else {
            set atype {word scalar}
        }
        set avalues [list $varname]
        set aranges [list $inner_range]

        lappend cmdlist [list $cmd $script $stree $atype $avalues $aranges]
    } elseif {$inner_tree_types == {text text}} {
        # array with text key: either array or array word element

        set arrayname_range [lindex $inner_tree 0 1]
        set arrayname [parse getstring $script $arrayname_range]

        set key_range [lindex $inner_tree 1 1]
        set key_string [parse getstring $script $key_range]

        if {!$iterating_word} {
            set atype {array text}
        } else {
            set atype {word array text}
        }
        set avalues [list $arrayname $key_string]
        set aranges [list $arrayname_range $key_range]

        lappend cmdlist [list $cmd $script $stree $atype $avalues $aranges]
    } elseif {$inner_tree_types == {text {variable text}}} {
        # array with scalar key: either array or array word element

        set arrayname_range [lindex $inner_tree 0 1]
        set arrayname [parse getstring $script $arrayname_range]

        set varname_range [lindex $inner_tree 1 2 0 1]
        set varname [parse getstring $script $varname_range]

        if {!$iterating_word} {
            set atype {array scalar}
        } else {
            set atype {word array scalar}
        }
        set avalues [list $arrayname $varname]
        set aranges [list $arrayname_range $varname_range]

        lappend cmdlist [list $cmd $script $stree $atype $avalues $aranges]
    } elseif {$inner_tree_types == {text command}} {
        # array with command key: either array or array word element

        set arrayname_range [lindex $inner_tree 0 1]
        if {$arrayname_range == {}} {error "empty range at index 0 1 in inner_tree \{$inner_tree\}"}
        set arrayname [parse getstring $script $arrayname_range]

        set command_range [lindex $inner_tree 1 1]
        if {$command_range == {}} {error "empty range at index 1 1 in inner_tree \{$inner_tree\}"}
        set commandtext_range [parse_range_without_brackets\
            $script $command_range]
        set commandtext [parse getstring $script $commandtext_range]

        if {!$iterating_word} {
            set atype {array command}
        } else {
            set atype {word array command}
        }
        set avalues [list $arrayname $commandtext]
        set aranges [list $arrayname_range $commandtext_range]

        lappend cmdlist [list $cmd $script $stree $atype $avalues $aranges]
    } elseif {[lindex $inner_tree_types 0] == "text"} {
        # Invoke callback for each element found inside
        # a word key.

        # Create new cmdlist that is enclosed by {word begin} and {word end}.
        set inner_cmdlist [list]
        set inner_key "key$_parse(parse_variable_iterate_key)"
        set _parse($inner_key) $inner_cmdlist
        incr _parse(parse_variable_iterate_key)

        # Push inner_key onto stack before processing word elements
        # since they could also push a key value onto the stack.

        set _parse(parse_variable_iterate_stack) \
            [linsert $_parse(parse_variable_iterate_stack) 0 $inner_key]

        set atype {word begin}
        set avalues {}
        set aranges {}

        lappend inner_cmdlist [list $cmd $script $stree $atype $avalues $aranges]

        for {set i 1} {$i < [llength $inner_tree]} {incr i} {
            set itl_type [lindex $inner_tree_types $i 0]
            set inner_stree [lindex $inner_tree $i]
#            if {$debug} {
#                puts "word key loop on index $i: type is $itl_type"
#                puts "inner_stree \{$inner_stree\}"
#            }

            switch -exact -- $itl_type {
                "command" {
                    # Invoke callback for command word element
                    set atype {word command}

                    set command_range [lindex $inner_stree 1]
                    set commandtext_range [parse_range_without_brackets\
                        $script $command_range]
                    set commandtext [parse getstring $script $commandtext_range]

                    set avalues [list $commandtext]
                    set aranges [list $commandtext_range]

                    lappend inner_cmdlist [list $cmd $script $inner_stree $atype $avalues $aranges]
                }
                "text" {
                    # Invoke callback for text word element
                    set atype {word text}

                    set text_range [lindex $inner_stree 1]
                    set text [parse getstring $script $text_range]

                    set avalues [list $text]
                    set aranges [list $text_range]

                    lappend inner_cmdlist [list $cmd $script $inner_stree $atype $avalues $aranges]
                }
                "variable" {
                    # Save current inner_cmdlist in key variable

                    set _parse($inner_key) $inner_cmdlist

                    set private [list 1 $inner_key]
                    _parse_variable_iterate $script $inner_stree $cmd $private

                    # Reset inner_cmdlist to the key value since it was just changed
                    # by the call to _parse_variable_iterate.

                    set prev_inner_cmdlist $inner_cmdlist
                    set inner_cmdlist $_parse($inner_key)

#                    if {$debug} {
#                        puts "inner_cmdlist changed from/to\n\{$prev_inner_cmdlist\}\n\
#                            \{$inner_cmdlist\}\nin call to _parse_variable_iterate"
#                    }
                    unset prev_inner_cmdlist
                }
                default {
                    error "unknown inner type \{$itl_type\} for stree \{$stree\}"
                }
            }
        }

        # Add closing command and save the commands for the word key.

        set atype {word end}
        set avalues {}
        set aranges {}
        lappend inner_cmdlist [list $cmd $script $stree $atype $avalues $aranges]

        set _parse($inner_key) $inner_cmdlist

        # Process the array with a word key
        set arrayname_range [lindex $inner_tree 0 1]
        set arrayname [parse getstring $script $arrayname_range]

        # Range covers text inside the parens, value is
        # the value set via parse_variable_iterate_word_value
        # if there was one.
        set word_range [parse_range_of_tree $inner_tree 1]
        set word_value $_parse(parse_variable_iterate_word_value)

        if {!$iterating_word} {
            set atype {array word}
        } else {
            set atype {word array word}
        }
        # An array with a word value is tricky because we will
        # not know what the word value is until after the
        # word elements have been evaluated. Create the avalues
        # pair with this special placeholder __WORD_VALUE__
        # and replace it with the actual value before the
        # command is called.
        set avalues [list $arrayname __WORD_VALUE__]
        set aranges [list $arrayname_range $word_range]
        lappend cmdlist [list $cmd $script $stree $atype $avalues $aranges]
    } else {
        error "unmatched inner_tree_types \{$inner_tree_types\}"
    }

#    if {$debug} {
#         puts "saving cmdlist \{$cmdlist\} back into key $key"
#    }

    # Save modified cmdlist back into key variable
    set _parse($key) $cmdlist

    return
}

# Called to query the current value of a array variable
# word key or to set a new value. This method only has meaning
# after a {word begin} but before a {word end} iteration
# pair. If a value for the word key is not set it will be {}.

proc parse_variable_iterate_word_value { {value pviwv_QUERY} } {
    global _parse
    if {$value == "pviwv_QUERY"} {
        if {![info exists _parse(parse_variable_iterate_word_value)]} {
            set _parse(parse_variable_iterate_word_value) {}
        }
        return $_parse(parse_variable_iterate_word_value)
    }
    set _parse(parse_variable_iterate_word_value) $value
    return
}

# Iterate over the contents of a word. A word can be made
# up of text, variable, and command elements. Pass
# the word parse subtree and the script it was generated
# from along with the name of a callback command to invoke.

proc parse_word_iterate { script stree cmd } {
#    set debug 0

#    if {$debug} {
#        puts "parse_word_iterate : \"$script\" \{$stree\} $cmd"
#    }

    set saved_word_value [parse_word_iterate_word_value]

    set cmdlist [list]

    if {![parse_is_word $stree]} {
        error "expected word stree but got \{$stree\}"
    }

    set elements_stree [lindex $stree 2]

#    if {$debug} {
#        puts "elements_stree is \{$elements_stree\}"
#    }

    set atype {word begin}
    set avalues {}
    set aranges {}
    lappend cmdlist [list $cmd $script $stree $atype $avalues $aranges]

    foreach elements_tuple $elements_stree {
#        if {$debug} {
#            puts "elements_tuple \{$elements_tuple\}"
#        }

        set type [lindex $elements_tuple 0]

        switch -exact -- $type {
            "backslash" {
                set bs_stree $elements_tuple
                set bs_range [lindex $elements_tuple 1]
                set bs_start [lindex $bs_range 0]
                set bs_len [lindex $bs_range 1]
                incr bs_start 1
                incr bs_len -1
                set bs_range [list $bs_start $bs_len]
                set bs_text [parse getstring $script $bs_range]

                set atype "backslash"
                set avalues [list $bs_text]
                set aranges [list $bs_range]
                lappend cmdlist [list $cmd $script $bs_stree $atype $avalues $aranges]
            }
            "command" {
                set command_stree $elements_tuple
                set command_range [lindex $elements_tuple 1]
                set commandtext_range [parse_range_without_brackets\
                    $script $command_range]
                set commandtext [parse getstring $script $commandtext_range]

                set atype "command"
                set avalues [list $commandtext]
                set aranges [list $commandtext_range]
                lappend cmdlist [list $cmd $script $command_stree $atype $avalues $aranges]
            }
            "text" {
                set text_stree $elements_tuple
                set text_range [lindex $elements_tuple 1]
                set text [parse getstring $script $text_range]

                set atype "text"
                set avalues [list $text]
                set aranges [list $text_range]
                lappend cmdlist [list $cmd $script $text_stree $atype $avalues $aranges]
            }
            "variable" {
                # Pass variable value as the whole variable range including
                # the dollar sign. The caller will need to descend into the
                # passed stree to figure out what kind of variable this is.
                set variable_stree $elements_tuple
                set variable_range [lindex $elements_tuple 1]
                set variable [parse getstring $script $variable_range]

                set atype "variable"
                set avalues [list $variable]
                set aranges [list $variable_range]
                lappend cmdlist [list $cmd $script $variable_stree $atype $avalues $aranges]
            }
            default {
                error "unsupported word element type \"$type\" in elements_tuple \{$elements_tuple\}"
            }
        }

#        if {$debug} {
#            puts "type $atype: appended cmd \{[lindex $cmdlist end]\}"
#        }
    }

    if {[llength $cmdlist] == 0} {
        error "cmdlist length should not be zero"
    }

    set atype {word end}
    set avalues {}
    set aranges {}
    lappend cmdlist [list $cmd $script $stree $atype $avalues $aranges]

    set atype {word}
    set avalues __WORD_VALUE__
    set aranges {}
    lappend cmdlist [list $cmd $script $stree $atype $avalues $aranges]

#    if {$debug} {
#        puts "parse_word_iterate: will iterate over cmdlist"
#    }

    # Loop over each command in cmdlist and invoke callback
    # for each word element.

    foreach cmd $cmdlist {
        # Command lists contain the following elements:
        # CMD SCRIPT STREE TYPE VALUES RANGES

#        if {$debug} {
#            puts "parse_word_iterate: cmd is \{$cmd\}"
#        }

        # Verify that the command has the correct number of
        # arguments. We get a really confusing error message
        # during the eval step later on if the wrong number
        # of arguments is passed.

        if {[llength $cmd] != 6} {
            error "expected 6 arguments, callback format is\
                CMD SCRIPT STREE TYPE VALUES RANGES\
                but cmd was \{$cmd\}"
        }

        # When invoking callback, reset the word value if a
        # callback that will pass {word begin} is found.
        # When {word end} will be passed, save the current
        # word value so that it can be used to substitute
        # for the __WORD_VALUE__ placeholder. The VALUES
        # argument will be set to the value __WORD_VALUE__
        # if a substitution is needed.

        set atype [lindex $cmd 3]
        set avalues [lindex $cmd 4]
        if {$atype == {word begin}} {
            parse_word_iterate_word_value {}
        } elseif {$atype == {word end}} {
            set word_value [parse_word_iterate_word_value]
        } elseif {$avalues == "__WORD_VALUE__"} {
            if {![info exists word_value]} {
                error "__WORD_VALUE__ placeholder found with no previous {word begin/end}"
            }
            set new_avalues $word_value

            set new_cmd [lrange $cmd 0 3]
            lappend new_cmd $new_avalues
            lappend new_cmd [lindex $cmd 5]
            set cmd $new_cmd

#            if {$debug} {
#                puts "parse_word_iterate: replaced __WORD_VALUE__ with \"$word_value\""
#                puts "parse_word_iterate: word replaced cmd is \{$cmd\}"
#            }
        }

        # Evaluate specific command and take extra care to ensure
        # that it is passed as a pure list to avoid a costly reparse.

        namespace eval :: [lrange $cmd 0 end]
    }

    # Return word value calculated during iteration,
    # restore word value at function invocation time.
    set r_word [parse_word_iterate_word_value]
    parse_word_iterate_word_value $saved_word_value
    return $r_word
}

# Called to query the current value of a word value
# as its elements are being iterated over or to set
# a new value. This method should only be used during
# a word iteration callback. The result word value will
# be returned by parse_word_iterate.

proc parse_word_iterate_word_value { {value pwiwv_QUERY} } {
    global _parse
    if {$value == "pwiwv_QUERY"} {
        if {![info exists _parse(parse_word_iterate_word_value)]} {
            set _parse(parse_word_iterate_word_value) {}
        }
        return $_parse(parse_word_iterate_word_value)
    }
    set _parse(parse_word_iterate_word_value) $value
    return
}






# Iterate over expr subexpressions starting from the inner
# most one and progressing outward. Expressions can invoke
# the in_cmd callback to evaluate operands and expressions
# and so on. The in_cmd callback should invoke
# parse_expr_iterate_type_value to set the type and
# result for an operand or expression evaluation.
# The result value will be passed as a value type and will
# also be returned by this function. If values are not set
# then the value returning code will not be used. This
# procedure is recursion safe since the entire expr is descended
# into before any callbacks that might invoke parse_expr_iterate
# again are invoked.
#
# The in_cmd callback should accept 5 arguments as follows:
# { script etree type values ranges }

proc parse_expr_iterate { in_script in_etree in_cmd } {
    global _parse

#    set debug 0

    set _parse(parse_expr_iterate_result) [list]
    set _parse(parse_expr_iterate_stack) [list]

    _parse_expr_iterate_descend $in_script $in_etree $in_cmd

    set cmdlist $_parse(parse_expr_iterate_result)

    set _parse(parse_expr_iterate_result) [list]
    set _parse(parse_expr_iterate_stack) [list]

#    if {$debug} {
#        # FIXME: Print cmdstack as stack elements
#        puts "\n\n-----------------"
#        puts "parse_expr_iterate: will iterate over cmdlist:"
#        set i 0
#        foreach elem $cmdlist {
#            puts "$i: $elem"
#            incr i
#        }
#        puts "-----------------"
#    }

    # Loop over each command in cmdlist and invoke in_cmd
    # for each expr element.
    
    if {[llength $cmdlist] == 0} {
        error "empty cmdlist"
    }

#    if {$debug} {
#        set max 1000
#    }

    set subexpr_stack [list]

    for {set i 0 ; set len [llength $cmdlist]} {$i < $len} {} {
#        if {$debug} {
#            incr max -1
#            if {$max < 0} {error "max exceeded, infinite loop ?"}
#        }

        set cmdlist_elem [lindex $cmdlist $i]
        set elem_type [lindex $cmdlist_elem 0]

#        if {$debug} {
#            puts "parse_expr_iterate ($i): elem_type is \"$elem_type\""
#            if {$elem_type == "operator"} {
#                puts "operator is [lindex $cmdlist_elem 4]"
#                puts "subexpr_stack: depth [llength $subexpr_stack]"
#                foreach elem $subexpr_stack {
#                    if {$elem == {}} {
#                        puts "\{\}"
#                    } else {
#                        puts $elem
#                    }
#                }
#            }
#            #set index 0
#            #foreach elem $cmdlist_elem {
#            #    puts "\[lindex \$cmdlist_elem $index\] is $elem"
#            #    incr index
#            #}
#        }

        switch -exact -- $elem_type {
            "operator" {
                set script [lindex $cmdlist_elem 1]
                set etree [lindex $cmdlist_elem 2]
                set num_operands [lindex $cmdlist_elem 3]
                set operator [lindex $cmdlist_elem 4]
                set ranges [lindex $cmdlist_elem 5]

                # Evaluate value operands
                set operands [list]
                set subexpr_found 0

                # Count number of subexpressions so we know how many
                # to pop off the stack. The order of the subexpression
                # values on that stack is the reverse of the way they
                # appear in the operands.
                set jstart [expr {$i + 1}]
                set jend [expr {$i + 1 + $num_operands}]
                for {set j $jstart} {$j < $jend} {incr j} {
                    set operand_cmdlist_elem [lindex $cmdlist $j]
                    set operand_cmdlist_elem_type [lindex $operand_cmdlist_elem 0]
                    if {$operand_cmdlist_elem_type == "subexpr"} {
                        incr subexpr_found
                        if {$subexpr_found > 3} {
                            error "max of 3 subexpressions supported, subexpr_found\
                                is $subexpr_found"
                        }
                    }
                }
#                if {$debug} {
#                    puts "will pop $subexpr_found subexpr {} elements"
#                }
                set stack_size [llength $subexpr_stack]
                if {$subexpr_found > $stack_size} {
                    error "attempt to pop $subexpr_found expressions off\
                        stack of size $stack_size"
                }
                set subexpr_list [list]
                for {set j 0} {$j < $subexpr_found} {incr j} {
                    set subexpr_list [linsert $subexpr_list 0 \
                        [lindex $subexpr_stack 0]]
                    set subexpr_stack [lrange $subexpr_stack 1 end]
                }
                set subexpr_found 0
                for {set j $jstart} {$j < $jend} {incr j} {
                    set operand_cmdlist_elem [lindex $cmdlist $j]
                    set operand_cmdlist_elem_type [lindex $operand_cmdlist_elem 0]
                    if {$operand_cmdlist_elem_type == "subexpr"} {
                        set type_val [lindex $subexpr_list $subexpr_found]
                        _parse_expr_iterate_subexpr $operand_cmdlist_elem $in_cmd
                        incr subexpr_found
                        lappend operands $type_val
                    } elseif {$operand_cmdlist_elem_type == "value"} {
                        lappend operands [_parse_expr_iterate_evaluate_value $operand_cmdlist_elem]
                    } else {
                        error "unknown type \"$operand_cmdlist_elem_type\" in \{$operand_cmdlist_elem\} at index $j"
                    }
                }

                # Evaluate operator
                if {[_parse_expr_iterate_is_math_function $operator]} {
                    set type {math function}
                } elseif {$num_operands == 1} {
                    set type {unary operator}
                } elseif {$num_operands == 2} {
                    set type {binary operator}
                } elseif {$num_operands == 3} {
                    set type {ternary operator}
                } else {
                    error "unsupported num_operands $num_operands in \{$cmdlist_elem\}"
                }

                # Values list for an operator: {OPERATOR {OPERAND OPERAND ...}}
                # Each OPERAND is a {type value} list for a value that was evaulated.
                set values [list $operator $operands]

                set _parse(parse_expr_iterate_type_value) {}

                # CALLBACK ARGS: script etree type values ranges
                set cmd [list $in_cmd $script $etree $type $values $ranges]
                namespace eval :: $cmd

                set type_val $_parse(parse_expr_iterate_type_value)
#                if {$debug} {
#                    puts "operator evaluated to \{$type_val\}"
#                }

                # Push result of operator evaluation onto stack
                set subexpr_stack [linsert $subexpr_stack 0 $type_val]

                incr i $num_operands
                incr i
            }
            "value" {
                _parse_expr_iterate_evaluate_value $cmdlist_elem
                incr i
            }
            default {
                error "unknown elem type \"$elem_type\""
            }
        }
    }

    # Whole expression has been processed, invoke callback one
    # more time to indicate that the expression value is available.
    set type {value}
    set values [list $_parse(parse_expr_iterate_type_value)]
    set ranges {}
    set cmd [list $in_cmd $in_script $in_etree $type $values $ranges]
    namespace eval :: $cmd

    # Return {type value} for whole expression
#    if {$debug} {
#        puts "returning {type value} \{$_parse(parse_expr_iterate_type_value)\}"
#    }
    return $_parse(parse_expr_iterate_type_value)
}

# Invoked when a "value" element is found while iterating over an expr. A value
# argument may be evaluated by the callback.

proc _parse_expr_iterate_evaluate_value { cmd_elem } {
    global _parse

#    set debug 0

    set type [lindex $cmd_elem 0]

#    if {$type != "value"} {
#        error "expected \"value\" operand but got \"$type\" from \{$cmd_elem\}"
#    }

    set _parse(parse_expr_iterate_type_value) {}
    set cmd [lindex $cmd_elem 1]
    # Make sure script is a pure list to avoid expensive reparse
    namespace eval :: [lrange $cmd 0 end]

    # A {literal operand} need not be evaluated by the callback
    if {$_parse(parse_expr_iterate_type_value) == {} && \
            [lindex $cmd 3] == {literal operand}} {
        set _parse(parse_expr_iterate_type_value) [lindex $cmd 4 0]
    }
    set type_val $_parse(parse_expr_iterate_type_value)
#    if {$debug} {
#        puts "value evaluated to \{$type_val\}"
#    }
    return $type_val
}

# Invoked when a "subexpr" element is found while iterating over an expr.

proc _parse_expr_iterate_subexpr { cmd_elem in_cmd } {
    global _parse

    set type [lindex $cmd_elem 0]

    if {$type != "subexpr"} {
        error "expected \"subexpr\" operand but got \"$type\" from \{$cmd_elem\}"
    }

    # FIXME: Get script and etree from cmd_elem ???

    set script {}
    set etree {}
    set type {subexpression}
    set values {}
    set ranges {}

    set cmd [list $in_cmd $script $etree $type $values $ranges]
    namespace eval :: $cmd

    return
}

# Return true if the given operator is a math function defined in Tcl.

proc _parse_expr_iterate_is_math_function { operator } {
    switch -exact -- $operator {
        abs -
        acos -
        asin -
        atan -
        atan2 -
        ceil -
        cos -
        cosh -
        double -
        exp -
        floor -
        fmod -
        hypot -
        int -
        log -
        log10 -
        pow -
        rand -
        round -
        sin -
        sinh -
        sqrt -
        srand -
        tan -
        tanh -
        wide {
            return 1
        }
        default {
            return 0
        }
    }
}

# Descend into expression tree and create a result that starts with
# the inner most expression and ends with the outer most one.

proc _parse_expr_iterate_descend { script etree cmd {private {}} } {
    global _parse

#    set debug 0

#    if {$debug} {
#        puts "_parse_expr_iterate_descend: \"$script\" \{$etree\} $cmd"
#    }

    set type [lindex $etree 0]
    set range [lindex $etree 1]
    set setree [lindex $etree 2]

    if {$type == "subexpr"} {
        # descend into subexpression tree.

        # Check for special case of math function with no arguments like rand().
        # This needs to be handled as subexpression instead of as a value elem.

        set no_arg_mfunc 0
        if {[llength $setree] == 1} {
            set value [lindex $setree 0]
            if {[lindex $value 0] == "operator" && [lindex $value 2] == {}} {
                set no_arg_mfunc 1
            }
        }

        if {[llength $setree] >= 2 || $no_arg_mfunc} {
            # A subexpression with an operator and operands
            set op [lindex $setree 0]
            # Determine how many operands this operator takes
            set num_operands [expr {[llength $setree] - 1}]
            if {!$no_arg_mfunc && ($num_operands < 1 || $num_operands > 3)} {
                error "invalid num_operands $num_operands"
            }
            set operands [list]
            for {set i 1} {$i < ($num_operands + 1)} {incr i} {
                lappend operands [lindex $setree $i]
            }

#            if {$debug} {
#                puts "found operator subexpression : [parse getstring $script [lindex $op 1]]"
#                puts "op is \{$op\}"
#                puts "num_operands is $num_operands"
#                foreach operand $operands {
#                    puts "operand is \{$operand\}"
#                }
#            }

            # Push new subexpression onto the expression stack
            set expr_list [lindex $_parse(parse_expr_iterate_stack) 0]
            lappend expr_list [list subexpr {}]
            set _parse(parse_expr_iterate_stack) \
                [lreplace $_parse(parse_expr_iterate_stack) 0 0 $expr_list]
            set _parse(parse_expr_iterate_stack) \
                [linsert $_parse(parse_expr_iterate_stack) 0 {}]

            _parse_expr_iterate_descend $script $op $cmd $num_operands

            foreach operand $operands {
                _parse_expr_iterate_descend $script $operand $cmd
            }

            # Pop completed subexpression off the stack
            # and append to result list.

            set expr_list [lindex $_parse(parse_expr_iterate_stack) 0]
            set _parse(parse_expr_iterate_stack) \
                [lreplace $_parse(parse_expr_iterate_stack) 0 0]

#            if {$debug} {
#                puts "inserting \{$expr_list\} at front of expression result"
#            }

            foreach elem $expr_list {
                lappend _parse(parse_expr_iterate_result) $elem
            }

#            if {$debug} {
#                puts "new expression result is \{$_parse(parse_expr_iterate_result)\}"
#                puts "new expression stack is \{$_parse(parse_expr_iterate_stack)\}"
#            }
        } else {
            # A subexpression with a single value
            set value [lindex $setree 0]

#            if {$debug} {
#                puts "found value subexpression"
#                puts "value is \{$value\}"
#            }

            # Pass num_operands 0 in case this is a rand() operator
            _parse_expr_iterate_descend $script $value $cmd 0

            # If this is the sole value, return it
            if {[llength $_parse(parse_expr_iterate_stack)] == 1} {
                set expr_list [lindex $_parse(parse_expr_iterate_stack) 0]
                foreach elem $expr_list {
                    lappend _parse(parse_expr_iterate_result) $elem
                }
            }
        }
        return
    }

    switch -exact -- $type {
        "command" {}
        "operator" {}
        "text" {}
        "variable" {}
        "word" {}
        "backslash" {
           # The Tcl expr parser should see a single backslash
           # string like "\n" as a word element that contains
           # a backslash. Fixup the info so that this is
           # seen as a word that contains a backslash character.
#           if {$debug} {
#               puts "backslash (before): $etree"
#           }
           # Range should include the enclosing double quotes.
           set range_before [expr {[lindex $range 0] - 1}]
           set before [parse getstring $script [list $range_before 1]]
           if {$before != "\""} {
               error "expected double quote before backslash character, got '$before'"
           }
           set range_after [expr {[lindex $range 0] + [lindex $range 1]}]
           set after [parse getstring $script [list $range_after 1]]
           if {$after != "\""} {
               error "expected double quote after backslash character, got '$after'"
           }
           set range_with_quotes [list $range_before \
               [expr {[lindex $range 1] + 2}]]

           set bstree [list "backslash" $range {}]
           set type {word}
           set setree [list $bstree]

           set range $range_with_quotes
           set etree [list $type $range_with_quotes $setree]
#           if {$debug} {
#               puts "backslash (after): $etree"
#           }
        }
        default {
            # Unknown type
            error "Unknown expr type \"$type\""
            return
        }
    }

    set type_str [parse getstring $script $range]

#    if {$debug} {
#        puts "got $type \"$type_str\" from range $range"
#    }

    # Get current command list for expr at top of stack.
    set expr_list [lindex $_parse(parse_expr_iterate_stack) 0]

    switch -exact -- $type {
        "command" {
            if {$setree != {}} {
                error "expected empty subtree, got \{$setree\}"
            }

            # Get command text without brackets
            set range [parse_range_without_brackets $script $range]
            set type_str [parse getstring $script $range]

            set atype {command operand}
            set avalues [list $type_str]
            set aranges [list $range]
            set cmdlist [list $cmd $script $etree $atype $avalues $aranges]
            lappend expr_list [list value $cmdlist]
        }
        "operator" {
            if {$setree != {}} {
                error "expected empty subtree, got \{$setree\}"
            }
            # private argument is set to num_operands in caller for op type
            set num_operands $private
            lappend expr_list [list operator $script $etree $num_operands $type_str [list $range]]
        }
        "text" {
            if {$setree != {}} {
                error "expected empty subtree, got \{$setree\}"
            }
            set atype {literal operand}

            # Tricky bit: Need to detect a string operand like "1" or {1} and treat
            # it differently that the plain text 1. Do this by checking around
            # the range to see if it is double quoted.

            set range_start [lindex $range 0]
            set range_length [lindex $range 1]
            set char_before_range_index [expr {$range_start - 1}]
            if {$char_before_range_index < 0} {
                set char_before ""
            } else {
                set char_before [parse getstring $script [list $char_before_range_index 1]]
            }

            set max_range [parse getrange $script]
            set max_index [expr {[lindex $max_range 0] + [lindex $max_range 1] - 1}]

            set char_after_range_index [expr {$range_start + $range_length}]
            if {$char_after_range_index > $max_index} {
#                if {$debug} {
#                    puts "char_after_range_index $char_after_range_index >= $max_index"
#                }
                set char_after ""
            } else {
                set char_after [parse getstring $script [list $char_after_range_index 1]]
            }
#            if {$debug} {
#                puts "found char before \'$char_before\' and char after \'$char_after\'"
#                puts "quoted text is $char_before$type_str$char_after"
#            }

            if {$char_before == "\"" && $char_after == "\""} {
                set avalues [list [list "string" $type_str]]
            } elseif {$char_before == "\{" && $char_after == "\}"} {
                set avalues [list [list "braced string" $type_str]]
            } else {
                set avalues [list [list "text" $type_str]]
            }
            set aranges [list $range]
            set cmdlist [list $cmd $script $etree $atype $avalues $aranges]
            lappend expr_list [list value $cmdlist]
        }
        "variable" {
            set atype {variable operand}
            set avalues [list $type_str]
            set aranges [list $range]
            set cmdlist [list $cmd $script $etree $atype $avalues $aranges]
            lappend expr_list [list value $cmdlist]
        }
        "word" {
            if {$setree == {}} {
                error "expected non-empty subtree, got \{$setree\}"
            }
            set atype {word operand}
            set avalues [list $type_str]
            set aranges [list $range]
            set cmdlist [list $cmd $script $etree $atype $avalues $aranges]
            lappend expr_list [list value $cmdlist]
        }
    }

    # Pop/Push modified expr back to top of stack
    set _parse(parse_expr_iterate_stack) [lreplace $_parse(parse_expr_iterate_stack) 0 0 $expr_list]
    return
}

# Invoked to indicate that a type and value pair for
# an operand or a subexpression is available. The value
# for an operand will be passed to an operator. The
# value for a subexpession will be passed to the enclosing
# subexpression. If no value is set a placeholder will
# be passed for the value.

proc parse_expr_iterate_type_value { type value } {
    set ::_parse(parse_expr_iterate_type_value) [list $type $value]
    return
}


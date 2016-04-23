#
#  Copyright (c) 2005 Advanced Micro Devices, Inc.
#
#  See the file "license.amd" for information on usage and
#  redistribution of this file, and for a DISCLAIMER OF ALL
#   WARRANTIES.
#
#  RCS: @(#) $Id: compileproc.tcl,v 1.34 2006/08/21 21:41:13 mdejong Exp $
#
#

# The compileproc module will parse and compile the contents of a proc.

# Convert a proc declaration to a list of proc arguments.
# This method assumes that the script string is already
# a valid Tcl list.

proc compileproc_script_to_proc_list { script } {
    set len [llength $script]
    if {$len != 4} {
        error "expected proc name args body: got $len args"
    }
    if {[lindex $script 0] != "proc"} {
        error "expected proc at index 0, got \"[lindex $script 0]\""
    }
    return [lrange $script 1 end]
}

# Split arguments to a proc up into three types.
# The return value is a list of length 3 consisting
# of {NON_DEFAULT_ARGS DEFAULT_ARGS ARGS}. The
# first list is made up of those argumens that
# have no default value. The second list is made
# up of those arguments that have a default value.
# The third list is a single boolean element, it is
# true if the special "args" argument was found as
# the last element.

proc compileproc_args_split { proc_args } {
#    set debug 0

#    if {$debug} {
#        puts "compileproc_args_split : \{$proc_args\}"
#    }

    set non_default_args [list]
    set default_args [list]
    set args [list false]
    set min_num_args 0
    set max_num_args {}

    foreach proc_arg $proc_args {
        if {$proc_arg == "args"} {
            set args true
        } elseif {[llength $proc_arg] == 1} {
            lappend non_default_args $proc_arg
            incr min_num_args
        } else {
            lappend default_args $proc_arg
        }
    }

    # Calculate max number of arguments as long as "args" was not found

    if {!$args} {
        set max_num_args [expr {$min_num_args + [llength $default_args]}]
    }

#    if {$debug} {
#        puts "returning \{$non_default_args\} \{$default_args\} $args : $min_num_args $max_num_args"
#    }

    return [list $non_default_args $default_args $args $min_num_args $max_num_args]
}


# Process proc arguments and emit code to set local variables
# named in the proc argument to the values in the passed in objv.

proc compileproc_args_assign { proc_args } {
    global _compileproc_key_info

    set cmd_needs_init $_compileproc_key_info(cmd_needs_init)
    set constants_found $_compileproc_key_info(constants_found)

    if {[llength $proc_args] == 0} {
        # No arguments to proc
        return [emitter_empty_args]
    }

    set buffer ""
    set result [compileproc_args_split $proc_args]
    set non_default_args [lindex $result 0]
    set default_args [lindex $result 1]
    set has_args [lindex $result 2]
    set min_num_args [lindex $result 3]
    set max_num_args [lindex $result 4]

    if {[llength $non_default_args] == 0 && [llength $default_args] == 0 && !$has_args} {
        error "no arguments found"
    } elseif {!$has_args && [llength $default_args] == 0} {
        # Non-default arguments only
        if {$min_num_args != $max_num_args} {
            error "expected min to match max num args: $min_num_args $max_num_args"
        }
        set args_str [join $non_default_args " "]
        append buffer [emitter_num_args $max_num_args $args_str]

        set index 1
        foreach arg $non_default_args {
            append buffer [compileproc_assign_arg $arg $index]
            incr index
        }
    } elseif {!$has_args && [llength $non_default_args] == 0} {
        # Default arguments only
        if {$min_num_args != 0} {
            error "expected min num arguments to be zero, got $min_num_args"
        }
        set arg_names [list]
        foreach arg $default_args {
            set name [lindex $arg 0]
            set default [lindex $arg 1]
            lappend arg_names "?${name}?"
        }
        set args_str [join $arg_names " "]

        append buffer [emitter_num_default_args $max_num_args $args_str]

        set index 1
        foreach arg $default_args {
            set name [lindex $arg 0]
            set default [lindex $arg 1]
            compileproc_constant_cache_add $default
            set constants_found 1
            set default_sym [compileproc_constant_cache_get $default]

            append buffer [compileproc_assign_default_arg $name $index $default_sym]
            incr index
        }
    } elseif {$has_args && \
            [llength $non_default_args] == 0 && \
            [llength $default_args] == 0} {
        # args argument only
        append buffer [compileproc_assign_args_arg 1]
    } elseif {$has_args && \
            [llength $non_default_args] > 0 && \
            [llength $default_args] == 0} {
        # Non-default arguments and args
        if {$max_num_args != {}} {
            error "expected empty max num args, got $max_num_args"
        }
        set all_args $non_default_args
        lappend all_args args

        set args_str [join $all_args " "]
        append buffer [emitter_num_min_args $min_num_args $args_str]

        set index 1
        foreach arg $non_default_args {
            append buffer [compileproc_assign_arg $arg $index]
            incr index
        }

        append buffer [compileproc_assign_args_arg $index]
    } elseif {$has_args && \
            [llength $non_default_args] == 0 && \
            [llength $default_args] > 0} {
        # Default arguments and args
        if {$max_num_args != {}} {
            error "expected empty max num args, got $max_num_args"
        }
        if {$min_num_args != 0} {
            error "expected zero min num args, got $min_num_args"
        }
        set arg_names [list]
        foreach arg $default_args {
            set name [lindex $arg 0]
            set default [lindex $arg 1]
            lappend arg_names "?${name}?"
        }
        lappend arg_names "args"
        set args_str [join $arg_names " "]

        # No num args check

        set index 1
        foreach arg $default_args {
            set name [lindex $arg 0]
            set default [lindex $arg 1]
            compileproc_constant_cache_add $default
            set constants_found 1
            set default_sym [compileproc_constant_cache_get $default]

            append buffer [compileproc_assign_default_arg $name $index $default_sym]
            incr index
        }

        append buffer [compileproc_assign_args_arg $index]
    } elseif {!$has_args && \
            [llength $non_default_args] > 0 && \
            [llength $default_args] > 0} {
        # Non-default args and default args

        set arg_names $non_default_args
        foreach arg $default_args {
            set name [lindex $arg 0]
            set default [lindex $arg 1]
            lappend arg_names "?${name}?"
        }
        set args_str [join $arg_names " "]

        append buffer [emitter_num_range_args $min_num_args $max_num_args $args_str]

        set index 1
        foreach arg $non_default_args {
            set name [lindex $arg 0]

            append buffer [compileproc_assign_arg $name $index]
            incr index
        }

        foreach arg $default_args {
            set name [lindex $arg 0]
            set default [lindex $arg 1]
            compileproc_constant_cache_add $default
            set constants_found 1
            set default_sym [compileproc_constant_cache_get $default]

            append buffer [compileproc_assign_default_arg $name $index $default_sym]
            incr index
        }
    } elseif {$has_args && \
            [llength $non_default_args] > 0 && \
            [llength $default_args] > 0} {
        # Non-default args, default args, and args

        set arg_names $non_default_args
        foreach arg $default_args {
            set name [lindex $arg 0]
            set default [lindex $arg 1]
            lappend arg_names "?${name}?"
        }
        lappend arg_names "args"
        set args_str [join $arg_names " "]

        append buffer [emitter_num_min_args $min_num_args $args_str]

        set index 1
        foreach arg $non_default_args {
            set name [lindex $arg 0]

            append buffer [compileproc_assign_arg $name $index]
            incr index
        }

        foreach arg $default_args {
            set name [lindex $arg 0]
            set default [lindex $arg 1]
            compileproc_constant_cache_add $default
            set constants_found 1
            set default_sym [compileproc_constant_cache_get $default]

            append buffer [compileproc_assign_default_arg $name $index $default_sym]
            incr index
        }

        append buffer [compileproc_assign_args_arg $index]
    } else {
        error "unhandled args case: non_default_args \{$non_default_args\}, default_args \{$default_args\}, has_args $has_args"
    }

    # Set constant flags if needed

    if {$constants_found} {
        set cmd_needs_init 1
    }

    set _compileproc_key_info(cmd_needs_init) $cmd_needs_init
    set _compileproc_key_info(constants_found) $constants_found

    return $buffer
}

# Emit code to assign a procedure argument to the local variable
# table. Name is the name of the argument to the procedure.
# Index is the integer index from the passed in objv array where
# the value of the argument will be found.

proc compileproc_assign_arg { name index } {
    if {![string is integer $index]} {
        error "index $index is not an integer"
    }
    if {$index < 1} {
        error "index $index must be greater than 0"
    }
    set value "objv\[$index\]"
    return [compileproc_set_variable {} $name true $value false]
}

# Emit code to assign a procedure argument with a default value
# to the local variable table. Name is the name of the argument
# to the procedure.
# Index is the integer index from the passed in objv array where
# the value of the argument will be found.

proc compileproc_assign_default_arg { name index default_symbol } {
    if {![string is integer $index]} {
        error "index $index is not an integer"
    }
    if {$index < 1} {
        error "index $index must be greater than 0"
    }

    set buffer ""

    emitter_indent_level +1
    set sp "\n[emitter_indent]"
    emitter_indent_level -1

    set value "${sp}((objv.length <= $index) ? $default_symbol : objv\[$index\])"
    append buffer [compileproc_set_variable {} $name true $value false]

    return $buffer
}

# Emit code to assign procedure arguments to a local named "args"
# starting from the given index.

proc compileproc_assign_args_arg { index } {
    if {![string is integer $index]} {
        error "index $index is not an integer"
    }
    if {$index < 1} {
        error "index $index must be greater than 0"
    }

    set buffer ""
    append buffer [emitter_indent] \
    "if ( objv.length <= " $index " ) \{\n"
    emitter_indent_level +1
    set value ""
    append buffer [compileproc_set_variable {} "args" true $value true]
    emitter_indent_level -1
    append buffer [emitter_indent] \
    "\} else \{\n"
    emitter_indent_level +1
    append buffer [emitter_indent] \
    "TclObject argl = TclList.newInstance()\;\n"
    append buffer [emitter_indent] \
    "for (int i = " $index "\; i < objv.length\; i++) \{\n"
    emitter_indent_level +1
    append buffer [emitter_indent] \
    "TclList.append(interp, argl, objv\[i\])\;\n"
    emitter_indent_level -1
    append buffer [emitter_indent] \
    "\}\n"

    set value argl
    append buffer [compileproc_set_variable {} "args" true $value false]

    emitter_indent_level -1
    append buffer [emitter_indent] \
    "\}\n"

    return $buffer
}

# Process proc that has the -compile option set. This method will
# generate a Java class that will just eval the proc body string much
# like the Tcl proc command would.

proc compileproc_nocompile { proc_list class_name } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_nocompile [lindex $proc_list 0] : \{$proc_list\} $class_name"
#    }

    if {[llength $proc_list] != 3} {
        error "expected \{PROC_NAME PROC_ARGS PROC_BODY\} : passed [llength $proc_list] args"
    }

    compileproc_init

    set name [lindex $proc_list 0]
    set args [lindex $proc_list 1]
    set body [lindex $proc_list 2]

    set pair [compileproc_split_classname $class_name]
    set _compileproc(package) [lindex $pair 0]
    set _compileproc(classname) [lindex $pair 1]

    set buffer ""

    set buffer ""
    # class comment
    append buffer [emitter_class_comment $name]
    # package statement
    append buffer [emitter_package_name $_compileproc(package)]
    # import statement
    append buffer [emitter_import_tcl]
    # class declaration
    append buffer "\n" [emitter_class_start $_compileproc(classname)]
    # cmdProc declaration
    append buffer [emitter_cmd_proc_start]

    # Emit command initilization
    append buffer [emitter_init_cmd_check]

    # Setup local variable table. The wcmd identifier here
    # is inherited from TJC.CompiledCommand.
    append buffer [emitter_callframe_push wcmd.ns]
    append buffer [emitter_callframe_try]

    # Process proc args
    append buffer [compileproc_args_assign $args]

    # Invoke interp.eval() for proc body
    append buffer [emitter_eval_proc_body $body]

    # end callframe block
    append buffer [emitter_callframe_pop $name]

    # end cmdProc declaration
    append buffer [emitter_cmd_proc_end]

    # Emit class constants
    set cdata [compileproc_constant_cache_generate]
    if {$cdata != {}} {
        append buffer "\n" $cdata
    }

    # Variable cache not supported in -compile mode

    # end class declaration
    append buffer [emitter_class_end $_compileproc(classname)]

    set _compileproc(class) $buffer
    return $_compileproc(class)
}

# Split class name like foo.bar.OneCmd into package and class
# name parts.

proc compileproc_split_classname { class_name } {
    set elems [split $class_name .]
    if {[llength $elems] == 1} {
        return [list default $class_name]
    } else {
        return [list [join [lrange $elems 0 end-1] .] [lindex $elems end]]
    }
}

# Invoked by main module to compile a proc into
# Java source code. This method should catch
# errors raised during compilation and print a
# diagnostic "interal error" type of message to
# indicate where something went wrong.
#
# The filename argument is the name of the Tcl
# file that the proc was defined in. It is used
# in error reporting and is returned in the
# result tuple. Can be "".
#
# The proc_tuple argument is a tuple of:
# {PROC_NAME PROC_JAVA_CLASS_NAME PROC_LIST}
#
# PROC_NAME is the plain name of the Tcl proc
# PROC_JAVA_CLASS_NAME is the short name
#     of the Java class.
# PROC_LIST is a list containing the proc declaration.
#     The list length is 4: like {proc p {} {}}

proc compileproc_entry_point { filename proc_tuple } {
#    set debug 0

    set proc_name [lindex $proc_tuple 0]

#    if {$debug} {
#        puts "compileproc_entry_point $filename $proc_name"
#        puts "proc_tuple is \{$proc_tuple\}"
#    }

    set package [module_query PACKAGE]
    set compile_option [module_option_value compile]

#    if {$debug} {
#        puts "compile options is $compile_option"
#    }

    set class_name [lindex $proc_tuple 1]

    set proc [lindex $proc_tuple 2]
    set proc_list [lrange $proc 1 end]

    # If -compile or +compile is not set for specific proc, use module setting
    set proc_compile_option [module_option_value compile $proc_name]
    if {$proc_compile_option == {}} {
        set proc_compile_option $compile_option
    }

    if {$proc_compile_option} {
        if {[catch {
            set class_data [compileproc_compile $proc_list $class_name \
                compileproc_query_module_flags]
        } err]} {
            global _tjc

            if {![info exists _tjc(parse_error)] || $_tjc(parse_error) == ""} {
                # Not a handled parse error. Print lots of info.

                puts stderr "Interal error while compiling proc \"$proc_name\" in file $filename"
                #puts stderr "$err"
                # Print stack trace until the call to compileproc_compile is found.
                set lines [split $::errorInfo "\n"]
                foreach line $lines {
                    puts stderr $line
                    if {[string first {compileproc_compile} $line] != -1} {
                        break
                    }
                }
            } else {
                # A parse error that was caught at the source. Print a
                # error that a user might find helpful.
                puts stderr "Parse error while compiling proc \"$proc_name\" in file $filename"
                puts stderr $_tjc(parse_error)
                puts stderr "While parsing script text:"
                puts stderr $_tjc(parse_script)
            }

            return "ERROR"
        }

#        if {$debug} {
#            puts "generated $class_name data from proc \"$proc_name\""
#            puts "class data is:\n$class_data"
#        }

        return [list $filename $proc_name $class_name $class_data]
    } else {
        if {[catch {
            set class_data [compileproc_nocompile $proc_list $class_name]
        } err]} {
            puts stderr "Interal error while generating proc \"$proc_name\" in file $filename"
            puts stderr "$err"
            return "ERROR"
        }

#        if {$debug} {
#            puts "generated $class_name data from proc \"$proc_name\""
#            puts "class data is:\n$class_data"
#        }

        return [list $filename $proc_name $class_name $class_data]
    }
}


# Generate TJCExtension class that will be included in the JAR.
# The init method of the TJCExtension class will be invoked
# as a result of running the TJC::package command to load
# a TJC compiled package.

proc compileproc_tjcextension { package tcl_files init_file } {
    if {[llength $tcl_files] == 0} {
        error "empty tcl_files argument to compileproc_tjcextension"
    }
    if {$init_file == ""} {
        error "empty string init_file argument to compileproc_tjcextension"
    }

    set buffer ""
    # package statement
    append buffer [emitter_package_name $package]
    # import statement
    append buffer [emitter_import_tcl]

    if {$package == "default"} {
        set prefix "/library/"
    } else {
        set prefix "/"
        foreach elem [split $package .] {
            append prefix $elem "/"
        }
        append prefix "library/"
    }

    append buffer "
public class TJCExtension extends Extension \{
    public void init(Interp interp)
            throws TclException
    \{
        String init_file = \"$init_file\";
        String\[\] files = \{
"

    for {set len [llength $tcl_files] ; set i 0} {$i < $len} {incr i} {
        set fname [lindex $tcl_files $i]
        append buffer \
            "            " \
            "\"" $fname "\""
        if {$i == ($len - 1)} {
            # No-op
        } else {
            append buffer ","
        }
        append buffer "\n"
    }

    append buffer "        \}\;
        String prefix = \"$prefix\"\;

        TJC.sourceInitFile(interp, init_file, files, prefix)\;
    \}
\}
"

    if {$package == "default"} {
        set full_classname "TJCExtension"
    } else {
        set full_classname "${package}.TJCExtension"
    }

    return [list $full_classname $buffer]
}


# The functions below are used when compiling a proc body into
# a set of commands, words, and inlined methods.

proc compileproc_init {} {
    global _compileproc _compileproc_ckeys _compileproc_key_info
    global _compileproc_command_cache _compileproc_variable_cache
    global _compileproc_expr_value_stack

    if {[info exists _compileproc]} {
        unset _compileproc
    }
    if {[info exists _compileproc_ckeys]} {
        unset _compileproc_ckeys
    }
    if {[info exists _compileproc_key_info]} {
        unset _compileproc_key_info
    }
    if {[info exists _compileproc_command_cache]} {
        unset _compileproc_command_cache
    }
    if {[info exists _compileproc_variable_cache]} {
        unset _compileproc_variable_cache
    }
    if {[info exists _compileproc_expr_value_stack]} {
        unset _compileproc_expr_value_stack
    }

    # Counter for local variable names inside cmdProc scope
    compileproc_tmpvar_reset

    descend_init
    compileproc_constant_cache_init
    emitter_indent_level zero

    # Init variables needed for recursive var and word iteration
    set _compileproc(var_scan_key) {}
    set _compileproc(var_scan_results) {}

    set _compileproc(word_scan_key) {}
    set _compileproc(word_scan_results) {}

    set _compileproc(expr_eval_key) {}
    set _compileproc(expr_eval_buffer) {}
    set _compileproc(expr_eval_expressions) 0

    # Init OPTIONS settings

    # Set to {} if containers should not be inlined.
    # Set to all or a list of containers that should
    # be inlined for fine tuned control while testing.
    set _compileproc(options,inline_containers) {}

    # Is set to 1 if break/continue can be inlined
    # inside loops and if return can be inlined
    # in the method body. The stack records
    # this info for each control scope.
    set _compileproc(options,inline_controls) 0
    set _compileproc(options,controls_stack) {}
    compileproc_push_controls_context proc 0 0 1

    # Is set to 1 if the commands invoked inside
    # a compiled proc are resolved to Command
    # references and cached the first time
    # the containing command is invoked.
    set _compileproc(options,cache_commands) 0

    # Is set to 1 when preserve() and release()
    # should be skipped for constant value arguments.
    set _compileproc(options,skip_constant_increment) 0

    # Is set to 1 if variable access inside a proc
    # makes use of cached Var refrences.
    set _compileproc(options,cache_variables) 0

    # Is set to 1 if built-in Tcl command can
    # be replaced by inline code.
    set _compileproc(options,inline_commands) 0

    # Is set to 1 if inlined Tcl commands can
    # avoid emitting a setResult() or
    # resetResult() operation because the
    # result of the command is never used.
    set _compileproc(options,omit_results) 0

    # These options are enabled via +inline-expr.
    # The basic expr features enabled when
    # containers are inlined are improved
    # upon significantly by these optimizations.
    set _compileproc(options,expr_inline_operators) 0
    set _compileproc(options,expr_value_stack) 0
    set _compileproc(options,expr_value_stack_null) 0
    set _compileproc(options,expr_inline_set_result) 0

    # Init flags in key info array
    set _compileproc_key_info(cmd_needs_init) 0
    set _compileproc_key_info(constants_found) 0
    set _compileproc_key_info(generated) 0
}

proc compileproc_start { proc_list } {
    global _compileproc

#    set debug 0

    if {[llength $proc_list] != 3} {
        error "expected \{PROC_NAME PROC_ARGS PROC_BODY\} : passed [llength $proc_list] args"
    }

    set name [lindex $proc_list 0]
    set args [lindex $proc_list 1]
    set body [lindex $proc_list 2]

    set _compileproc(name) $name
    set _compileproc(args) $args
    set _compileproc(body) $body
    # Keys for commands that appear directly inside the proc body.
    # Commands that are contained inside other commands or are
    # nested arguments do not appear in this list.
    set _compileproc(keys) [list]

    descend_report_command_callback compileproc_command_start_callback start
    descend_report_command_callback compileproc_command_finish_callback finish
    return [descend_start $body]
}

# Return descend command keys for those commands in the first
# level of the proc body. Nested commands or commands inside
# containers are not included in this list.

proc compileproc_keys {} {
    return [descend_commands]
}

# Return info tuple for each key parsed while processing the keys.
# The order of the key info is the parse order.

proc compileproc_keys_info {} {
    global _compileproc_ckeys
    return $_compileproc_ckeys(info_keys)
}

# Return a list of command keys that are children of
# the passed in parent key. If no children exist
# then {} is returned.

proc compileproc_key_children { pkey } {
    global _compileproc _compileproc_ckeys

    if {![info exists _compileproc_ckeys($pkey)]} {
        return {}
    } else {
        return $_compileproc_ckeys($pkey)
    }
}

# Invoked when a new command is being processed. This method
# will determine if the command is at the toplevel of the
# proc or if it is an embedded command or a contained command
# and save the results accordingly.

proc compileproc_command_start_callback { key } {
    global _compileproc _compileproc_ckeys

#    set debug 0

#    if {$debug} {
#        puts "compileproc_command_start_callback $key"
#    }

    set result [descend_get_command_name $key]
    #set container_stack [descend_get_container_stack]

    if {[descend_arguments_undetermined $key]} {
        # Command known to be invoked but arguments not
        # known at compile time. Just ignore.
        return
    } elseif {[lindex $result 0]} {
        # Command name could be determined statically
        set cmdname [lindex $result 1]
#        if {$debug} {
#            puts "parsed command \"$cmdname\""
#        }
    } else {
        set cmdname _UNKNOWN
    }

    set info_token [list]
    lappend info_token $key $cmdname

    lappend _compileproc_ckeys(info_keys) $info_token
    set _compileproc_ckeys($key,info_key) $info_token
}

# Invoked when a command is no longer being processed.

proc compileproc_command_finish_callback { key } {
#    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_command_finish_callback $key"
#    }
}

# Entry point for all compiled proc variations. This method
# is invoked by compileproc_entry_point and returns a buffer
# containing the generated Java source code.

proc compileproc_compile { proc_list class_name {config_init {}} } {
    global _compileproc
    global _compileproc_key_info

#    set debug 0

#    if {$debug} {
#        puts "compileproc_compile [lindex $proc_list 0] : \{$proc_list\} $class_name"
#    }

    if {[llength $proc_list] != 3} {
        error "expected \{PROC_NAME PROC_ARGS PROC_BODY\} : passed [llength $proc_list] args"
    }

    set name [lindex $proc_list 0]
    set args [lindex $proc_list 1]
    set body [lindex $proc_list 2]

    compileproc_init

    # Invoke module flag query callback

    if {$config_init != {}} {
        namespace eval :: [list $config_init $name]
    }

    compileproc_start $proc_list
    compileproc_scan_keys [compileproc_keys]

    # Process proc args, this needs to be done before
    # emitting the command body so that an argument
    # that makes use of a constant will be handled.

    emitter_indent_level +2
    set args_buffer [compileproc_args_assign $args]
    emitter_indent_level -2

    # Generate Java source.

    set pair [compileproc_split_classname $class_name]
    set _compileproc(package) [lindex $pair 0]
    set _compileproc(classname) [lindex $pair 1]

    set buffer ""
    # class comment
    append buffer [emitter_class_comment $name]
    # package statement
    append buffer [emitter_package_name $_compileproc(package)]
    # import statement
    append buffer [emitter_import_tcl]
    # class declaration
    append buffer "\n" [emitter_class_start $_compileproc(classname)]

    # cmdProc declaration
    append buffer [emitter_cmd_proc_start]

    # If the command needs to be initialized, then do that
    # the first time the cmdProc is invoked. There may
    # be cases where we emit this check but the command
    # does not actually init any constants or commands.
    if {$_compileproc_key_info(cmd_needs_init)} {
        if {$_compileproc(options,inline_commands)} {
            lappend flags {inlineCmds}
        } else {
            set flags {}
        }
        append buffer [emitter_init_cmd_check $flags]
    }

    # Setup local variable table. The wcmd identifier here
    # is inherited from TJC.CompiledCommand.
    append buffer [emitter_callframe_push wcmd.ns]

    set body_bufer ""

    # Start try block and process proc args
    append body_buffer \
        [emitter_callframe_try] \
        $args_buffer

    # Walk over commands at the toplevel and emit invocations
    # for each toplevel command.

    foreach key [compileproc_keys] {
        append body_buffer [compileproc_emit_invoke $key]
    }

    # If cached variables were used, then emit a local variable
    # named compiledLocals just after pushing the call frame.

    if {[compileproc_variable_cache_is_used]} {
        append buffer \
            [emitter_callframe_init_compiledlocals \
                [compileproc_variable_cache_count]]
    }

    # If expr evaluations are going to grab values
    # and save them on the stack, then do that now.

    if {[compileproc_expr_value_stack_is_used]} {
        append buffer \
            [compileproc_expr_value_stack_generate]
    }

    # Append code for each toplevel command
    append buffer $body_buffer

    # End callframe block and cmdProc declaration,
    # expr value cleanup may be needed in the
    # finally block.

    if {[compileproc_expr_value_stack_is_used]} {
        emitter_indent_level +1
        set expr_value_buffer \
            [compileproc_expr_value_stack_release_generate]
        emitter_indent_level -1

        append buffer \
            [emitter_callframe_pop $name $expr_value_buffer]
    } else {
        append buffer \
            [emitter_callframe_pop $name]
    }

    append buffer \
            [emitter_cmd_proc_end]

    # Emit constant TclObject values and an initConstants() method.
    # It is possible that constant were found while scanning but
    # none were actually used, so this needs to appear after
    # the cmdProc() method has been emitted.

    if {$_compileproc_key_info(constants_found)} {
        set cdata [compileproc_constant_cache_generate]
        if {$cdata != {}} {
            append buffer "\n" $cdata
        }
    }

    # Emit code needed to cache command refrences.

    if {$_compileproc(options,cache_commands)} {
        set cdata [compileproc_command_cache_init_generate]
        if {$cdata != ""} {
            append buffer "\n" $cdata
        }
    }

    # Emit compiled local variable name array

    if {[compileproc_variable_cache_is_used]} {
        append buffer "\n" \
            [compileproc_variable_cache_names_array]
    }

    # end class declaration
    append buffer [emitter_class_end $_compileproc(classname)]

    # Top of controls context stack should be proc context
    compileproc_pop_controls_context proc

    set _compileproc(class) $buffer
    return $_compileproc(class)
}

# Reset the compiled in constant cache for a given proc.

proc compileproc_constant_cache_init {} {
    global _compileproc_constant_cache

    if {[info exists _compileproc_constant_cache]} {
        unset _compileproc_constant_cache
    }

    set _compileproc_constant_cache(ordered_keys) {}
}

# Add a constant Tcl string value to the constant cache.

proc compileproc_constant_cache_add { tstr } {
    global _compileproc_constant_cache

    set key const,$tstr

    if {![info exists _compileproc_constant_cache($key)]} {
        set ident {}
        set type [compileproc_constant_cache_type $tstr]
        set tuple [list $ident $type $tstr]
        set _compileproc_constant_cache($key) $tuple
    }
    return
}

# Determine the type for a constant TclObject based on
# what type the string looks like. Note that this
# implementation will determine the type based on
# integer and double ranges that are valid in Java.

proc compileproc_constant_cache_type { tstr } {
#    set debug 0

#    if {$debug} {
#        puts "compileproc_constant_cache_type \"$tstr\""
#    }

    if {$tstr == ""} {
        return STRING
    } elseif {$tstr == "false" || $tstr == "true"} {
#        if {$debug} {
#            puts "string \"$tstr\" looks like an BOOLEAN"
#        }
        return BOOLEAN
    } elseif {[compileproc_string_is_java_integer $tstr]} {
#        if {$debug} {
#            puts "string \"$tstr\" looks like an INTEGER"
#        }
        return INTEGER
    } elseif {[compileproc_string_is_java_double $tstr]} {
#        if {$debug} {
#            puts "string \"$tstr\" looks like a DOUBLE"
#        }
        return DOUBLE
    } else {
#        if {$debug} {
#            puts "string \"$tstr\" must be a STRING"
#        }
        return STRING
    }
}

# Return a class instance scoped reference for the
# given constant Tcl string. Note that a constant
# added to the pool will not actually appear in
# the Java file unless this method is invoked
# for that constant.

# FIXME: Write some tests for string that are the
# same after any backslash and output subst done
# in the emitter layer. Should not have duplicated
# constant strings in the cache.

proc compileproc_constant_cache_get { tstr } {
    global _compileproc_constant_cache

    set key const,$tstr

    if {![info exists _compileproc_constant_cache($key)]} {
        error "constant cache variable not found for\
            \"$tstr\", should have been setup via compileproc_constant_cache_add"
    }

    set tuple $_compileproc_constant_cache($key)
    set ident [lindex $tuple 0]
    if {$ident == {}} {
        # Generate name for instance variable and
        # save it so that this constant will appear
        # in the output pool.

        set type [lindex $tuple 1]
        set tstr [lindex $tuple 2]

        if {![info exists _compileproc_constant_cache(const_id)]} {
            set _compileproc_constant_cache(const_id) 0
        } else {
            incr _compileproc_constant_cache(const_id)
        }
        set ident "const$_compileproc_constant_cache(const_id)"
        set tuple [list $ident $type $tstr]
        set _compileproc_constant_cache($key) $tuple
        lappend _compileproc_constant_cache(ordered_keys) $key
    }
    return $ident
}

# Generate code to setup constant TclObject instance
# variables. These are used when a constant word
# value in a Tcl proc is used over and over again.

proc compileproc_constant_cache_generate {} {
    global _compileproc_constant_cache

#    set debug 0

    set tlist [list]

    foreach key $_compileproc_constant_cache(ordered_keys) {
        set tuple $_compileproc_constant_cache($key)

#        if {$debug} {
#            puts "processing key $key, tuple is \{$tuple\}"
#        }

        set ident [lindex $tuple 0]
        if {$ident == {}} {
            # Skip unused constant
            continue
        }

        lappend tlist $tuple
    }

    # If no constants were actually used, nothing to generate
    if {$tlist == {}} {
        return {}
    }

    return [emitter_init_constants $tlist]
}

# The list of all commands that could be invoked during
# this method is iterated here to create a large switch
# method to update the command cache. Command names
# are resolved into command refrences that are checked
# on a per-instance basis.

proc compileproc_command_cache_init_generate {} {
    global _compileproc
    global _compileproc_command_cache

#    set debug 0

#    if {$debug} {
#        puts "compileproc_command_cache_init_generate"
#    }

    if {![info exists _compileproc_command_cache(ordered_cmds)]} {
        return ""
    }

    if {[emitter_indent_level] != 0} {
        error "expected enter indent level of 0, got [emitter_indent_level]"
    }

    set buffer ""

    emitter_indent_level +1

    # Emit cmdEpoch for this command
    append buffer \
        [emitter_statement "int wcmd_cmdEpoch = 0"]

    # Emit instance scoped variables that hold a Command reference.
    set symbol_ids [list]

    foreach cmdname $_compileproc_command_cache(ordered_cmds) \
            key $_compileproc_command_cache(ordered_keys) {
        set symbol $_compileproc_command_cache($key)

        set cacheId [compileproc_get_cache_id_from_symbol $symbol]

        lappend symbol_ids $cacheId

        append buffer \
            [emitter_statement "WrappedCommand $symbol"] \
            [emitter_statement "int ${symbol}_cmdEpoch"]
    }

    append buffer \
        "\n" \
        [emitter_indent] \
        "void updateCmdCache(Interp interp, int cacheId) throws TclException \{\n"

    emitter_indent_level +1

    append buffer \
        [emitter_statement "String cmdName"]

    # Emit switch on cacheId to determine command name

    append buffer [emitter_indent] \
        "switch ( cacheId ) \{\n"

    emitter_indent_level +1

    # Special case for id 0, it will update all the commands
    # and reset wcmd_cmdEpoch.
    set cacheId 0

    append buffer [emitter_indent] \
        "case $cacheId: \{\n"

    emitter_indent_level +1

    foreach cmdname $_compileproc_command_cache(ordered_cmds) \
            key $_compileproc_command_cache(ordered_keys) \
            cacheId $symbol_ids {
        set symbol $_compileproc_command_cache($key)

        append buffer \
            [emitter_statement "$symbol = TJC.INVALID_COMMAND_CACHE"] \
            [emitter_statement "${symbol}_cmdEpoch = 0"]
    }

    append buffer \
        [emitter_statement "wcmd_cmdEpoch = wcmd.cmdEpoch"] \
        [emitter_statement "return"]

    emitter_indent_level -1

    # End switch case
    append buffer [emitter_indent] \
        "\}\n"

    # Resolve command names in namespace the command is defined in.

    foreach cmdname $_compileproc_command_cache(ordered_cmds) \
            key $_compileproc_command_cache(ordered_keys) \
            cacheId $symbol_ids {
        set symbol $_compileproc_command_cache($key)

        append buffer [emitter_indent] \
            "case " $cacheId ": \{\n"

        emitter_indent_level +1

        set jsym [emitter_double_quote_tcl_string $cmdname]

        append buffer \
            [emitter_statement "cmdName = $jsym"] \
            [emitter_statement "break"]

        emitter_indent_level -1

        # End switch case
        append buffer [emitter_indent] \
            "\}\n"
    }

    # Emit default block, this branch would never be taken.

    append buffer [emitter_indent] \
        "default: \{\n"

    emitter_indent_level +1

    append buffer \
        [emitter_statement \
        "throw new TclRuntimeError(\"default: cacheId \" + cacheId)"]

    emitter_indent_level -1

    # End default switch case
    append buffer [emitter_indent] \
        "\}\n"

    emitter_indent_level -1

    # End switch block

    append buffer [emitter_indent] \
        "\}\n"

    # Allocate locals to hold command ref and epoch

    append buffer \
        [emitter_statement \
            "WrappedCommand lwcmd = TJC.resolveCmd(interp, cmdName)"] \
        [emitter_statement "int cmdEpoch"] \
        [emitter_container_if_start "lwcmd == null"] \
        [emitter_statement "lwcmd = TJC.INVALID_COMMAND_CACHE"] \
        [emitter_statement "cmdEpoch = 0"] \
        [emitter_container_if_else] \
        [emitter_statement "cmdEpoch = lwcmd.cmdEpoch"] \
        [emitter_container_if_end]

    # Emit switch on cacheId to assign cache variable

    append buffer [emitter_indent] \
        "switch ( cacheId ) \{\n"

    emitter_indent_level +1

    foreach cmdname $_compileproc_command_cache(ordered_cmds) \
            key $_compileproc_command_cache(ordered_keys) \
            cacheId $symbol_ids {
        set symbol $_compileproc_command_cache($key)

        append buffer [emitter_indent] \
            "case $cacheId: \{\n"

        emitter_indent_level +1

        append buffer \
            [emitter_statement "$symbol = lwcmd"] \
            [emitter_statement "${symbol}_cmdEpoch = cmdEpoch"] \
            [emitter_statement "break"]

        emitter_indent_level -1

        # End switch case
        append buffer [emitter_indent] \
            "\}\n"
    }

    emitter_indent_level -1

    # End switch block

    append buffer [emitter_indent] \
        "\}\n"

    emitter_indent_level -1

    # End updateCmdCache

    append buffer [emitter_indent] \
        "\}\n"

    emitter_indent_level -1

    if {[emitter_indent_level] != 0} {
        error "expected exit indent level of 0, got [emitter_indent_level]"
    }

    return $buffer
}

# Return a buffer that checks to see if a WrappedCommand ref
# is still valid and returns a Command value (or null).

proc compileproc_command_cache_epoch_check { symbol } {
    set buffer ""

    append buffer \
        "((" $symbol "_cmdEpoch == " $symbol ".cmdEpoch)\n"

    emitter_indent_level +1

    append buffer [emitter_indent] \
        "? " $symbol ".cmd : null)"

    emitter_indent_level -1

    return $buffer
}

# If a cached command is no longer valid, try to update
# the cached value by looking the command up again.
# This method will also check to see if the containing
# command's cmdEpoch was changed and flush all the
# cached symbols in that case.

proc compileproc_command_cache_update { symbol } {
    set buffer ""

    set if_cond "${symbol}_cmdEpoch != ${symbol}.cmdEpoch"

    append buffer [emitter_container_if_start $if_cond]

    set cacheId [compileproc_get_cache_id_from_symbol $symbol]

    append buffer \
        [emitter_statement "updateCmdCache(interp, $cacheId)"] \
        [emitter_container_if_end]

    return $buffer
}

# Given "cmdcache1" return integer cache id "1".

proc compileproc_get_cache_id_from_symbol { symbol } {
    # Get cache id number from symbol
    if {![regexp {^[a-z]+cache([0-9]+)$} $symbol whole cacheId]} {
        error "could not match cache id in \"$symbol\""
    }
    return $cacheId
}

# Emit code to check the command epoch
# for the "this" command that contains other
# cached commands. The this epoch could
# be changed when a command is renamed
# or moved into another namespace. When
# the this command epoch is changed, all
# cached commands inside this command
# should be flushed. This check needs to
# be done before a specific cached command's
# epoch is checked.

proc compileproc_command_cache_this_check {} {
    set cond {wcmd_cmdEpoch != wcmd.cmdEpoch}

    set buffer ""

    append buffer \
        [emitter_container_if_start $cond] \
        [emitter_statement "updateCmdCache(interp, 0)"] \
        [emitter_container_if_end]

    return $buffer
}

# Lookup a command cache symbol given a command key.
# The order which the commands appear in the proc
# define what order the commands are initialized in.

proc compileproc_command_cache_lookup { dkey } {
    global _compileproc_command_cache
    global _compileproc_ckeys

    if {![info exists _compileproc_command_cache(counter)]} {
        set _compileproc_command_cache(counter) 1
    }

    # Determine the name of the command, if the command
    # name can't be determined statically just return
    # {} so that no cache will be used.

    #set tuple [compileproc_argument_printable $dkey 0]
    #set type [lindex $tuple 0]
    #if {$type != "constant"} {
    #    return {}
    #}

    # Optimized type check for performance reasons
    if {[lindex $::_compileproc_key_info($dkey,types) 0] != "constant"} {
        return
    }

    # The command name is a constant string. Find
    # the actual name of the command. There is no
    # way to know how the runtime will resolve
    # different commands with namespace qualifiers
    # and so on, so a command string must match
    # exactly to use the same cache value.

    set cmdname [lindex $_compileproc_ckeys($dkey,info_key) 1]

    set key "symbol,$cmdname"

    if {![info exists _compileproc_command_cache($key)]} {
        # Create cache symbol for new command
        set symbol "cmdcache$_compileproc_command_cache(counter)"
        incr _compileproc_command_cache(counter)
        set _compileproc_command_cache($key) $symbol
        lappend _compileproc_command_cache(ordered_keys) $key
        lappend _compileproc_command_cache(ordered_cmds) $cmdname
    } else {
        # Return existing symbol for this command
        set symbol $_compileproc_command_cache($key)
    }

    return $symbol
}

# Lookup a cached scalar variable by name. This
# method is used to get a token for a scalar
# variable read or write operation. This method
# assumes that the passed in vname is a constant str.

proc compileproc_variable_cache_lookup { vname } {
    global _compileproc_variable_cache

#    puts "compileproc_variable_cache_lookup $vname"

    if {![info exists _compileproc_variable_cache(counter)]} {
        set _compileproc_variable_cache(counter) 0
    }

    set key "symbol,$vname"

#    puts "looking for key \"$key\" in _compileproc_variable_cache():"
#    parray _compileproc_variable_cache

    if {![info exists _compileproc_variable_cache($key)]} {
        # Create cache symbol for new compiled local variable
        set symbol "compiledLocals\[$_compileproc_variable_cache(counter)\]"
        incr _compileproc_variable_cache(counter)
        set _compileproc_variable_cache($key) $symbol
        lappend _compileproc_variable_cache(ordered_keys) $key
        lappend _compileproc_variable_cache(ordered_vars) $vname
    } else {
        # Return existing symbol for this variable name
        set symbol $_compileproc_variable_cache($key)
    }

#    puts "returning symbol $symbol"

    return $symbol
}

proc compileproc_get_variable_cache_id_from_symbol { symbol } {
    # Get cache id number from symbol
    if {![regexp {^compiledLocals\[([0-9]+)\]$} $symbol whole cacheId]} {
        error "could not match cache id in \"$symbol\""
    }
    return $cacheId
}

# Return 1 if there are cached variables, this method is used
# to detect when cached variable support should be enabled
# in the generated code.

proc compileproc_variable_cache_is_used {} {
    global _compileproc_variable_cache

    if {![info exists _compileproc_variable_cache(ordered_keys)]} {
        return 0
    }

    return 1
}

# Return the number of compiled local cache vars used in the method.
# This is only valid when compileproc_variable_cache_is_used return 1.

proc compileproc_variable_cache_count {} {
    return [llength $::_compileproc_variable_cache(ordered_keys)]
}

# Return a buffer that declares an array containing
# the name of each compiled local.

proc compileproc_variable_cache_names_array {} {
    global _compileproc_variable_cache

    set buffer ""

    emitter_indent_level +1

    append buffer \
        [emitter_indent] \
        "String\[\] compiledLocalsNames = \{\n"

    emitter_indent_level +1

    set max [llength $_compileproc_variable_cache(ordered_vars)]

    for {set i 0} {$i < $max} {incr i} {
        set varname [lindex $_compileproc_variable_cache(ordered_vars) $i]
        append buffer [emitter_indent] \
            [emitter_double_quote_tcl_string $varname]
        if {$i == ($max - 1)} {
            append buffer "\n"
        } else {
            append buffer ",\n"
        }
    }

    emitter_indent_level -1

    append buffer \
        [emitter_indent] \
        "\}\;\n"

    emitter_indent_level -1

    return $buffer
}

# Return 1 if the expr value stack option is enabled
# and expr values will be grabbed and stored on the
# stack instead of being grabbed and released before
# each use.

proc compileproc_expr_value_stack_is_used {} {
    global _compileproc_expr_value_stack

    if {![info exists _compileproc_expr_value_stack(ordered_symbols)]} {
        return 0
    }

    return 1
}

# Get an ExprValue ref from the stack. This
# method is invoked during code generation
# when an ExprValue ref is needed. The returned
# ref must be released.

proc compileproc_expr_value_stack_get {} {
    global _compileproc_expr_value_stack

#    set debug 0

    if {!$::_compileproc(options,expr_value_stack)} {
        error "compileproc_expr_value_stack_get invoked but expr_value_stack flag is off"
    }

    if {![info exists _compileproc_expr_value_stack(ordered_symbols)]} {
        set ordered_symbols [list]
        set used_stack [list]
    } else {
        set ordered_symbols $_compileproc_expr_value_stack(ordered_symbols)
        set used_stack $_compileproc_expr_value_stack(used_stack)
    }

#    if {$debug} {
#        puts "compileproc_expr_value_stack_get"
#        puts "ordered_symbols is \{$ordered_symbols\}"
#        puts "used_stack is \{$used_stack\}"
#    }

    set symbols_size [llength $ordered_symbols]
    set stack_size [llength $used_stack]

    if {$stack_size > $symbols_size} {
       error "stack \{$used_stack\} size is larger than symbols \{$ordered_symbols\}"
    } elseif {$stack_size == $symbols_size} {
        # Another symbol is needed for this expr evaluation
        set symbol "evs${symbols_size}"
        lappend ordered_symbols $symbol
    } else {
        # Reuse existing symbol
        set symbol [lindex $ordered_symbols $stack_size]
        if {$symbol == ""} {
           error "empty symbol grabbed from ordered_symbols index $stack_size : \{$ordered_symbols\}"
        }
    }
    lappend used_stack $symbol

    set _compileproc_expr_value_stack(ordered_symbols) $ordered_symbols
    set _compileproc_expr_value_stack(used_stack) $used_stack

#    if {$debug} {
#        puts "post compileproc_expr_value_stack_get: $symbol"
#        puts "ordered_symbols is \{$_compileproc_expr_value_stack(ordered_symbols)\}"
#        puts "used_stack is \{$_compileproc_expr_value_stack(used_stack)\}"
#    }

    return $symbol
}

proc compileproc_expr_value_stack_release { symbol } {
    global _compileproc_expr_value_stack

#    set debug 0

#    if {$debug} {
#        puts "compileproc_expr_value_stack_release: $symbol"
#        puts "ordered_symbols is \{$_compileproc_expr_value_stack(ordered_symbols)\}"
#        puts "used_stack is \{$_compileproc_expr_value_stack(used_stack)\}"
#    }

    if {[info exists _compileproc_expr_value_stack(alias,$symbol)]} {
#        if {$debug} {
#            puts "$symbol is an alias for $_compileproc_expr_value_stack(alias,$symbol)"
#        }
        set top $_compileproc_expr_value_stack(alias,$symbol)
        unset _compileproc_expr_value_stack(alias,$symbol)
        set symbol $top
    }

    # Released symbol must be on top of the used stack

    set used_stack $_compileproc_expr_value_stack(used_stack)

    set top [lindex $used_stack end]
    if {$symbol ne $top} {
        error "released symbol $symbol is not at the top of the stack \{$used_stack\}"
    }

    # Pop symbol off used stack and reset it
    set used_stack [lreplace $used_stack end end]
    set _compileproc_expr_value_stack(used_stack) $used_stack

#    if {$debug} {
#        puts "post compileproc_expr_value_stack_release: $symbol"
#        puts "ordered_symbols is \{$_compileproc_expr_value_stack(ordered_symbols)\}"
#        puts "used_stack is \{$_compileproc_expr_value_stack(used_stack)\}"
#    }

    return
}

# Create alias from a local to an expr value local.
# This tricky little bit of code is needed so that
# an alias symbol can be released instread of
# the actual expr value symbol on the stack.

proc compileproc_expr_value_stack_alias { alias symbol } {
    global _compileproc_expr_value_stack
    set _compileproc_expr_value_stack(alias,$alias) $symbol
    return
}

proc compileproc_expr_value_stack_lookup_alias { alias } {
    return $::_compileproc_expr_value_stack(alias,$alias)
}

# Return a buffer that declares local ExprValue variables
# and grabs values from the runtime pool.

proc compileproc_expr_value_stack_generate {} {
    global _compileproc_expr_value_stack

    set buffer ""

    foreach symbol $_compileproc_expr_value_stack(ordered_symbols) {
        append buffer [emitter_indent] \
            "ExprValue $symbol = TJC.exprGetValue(interp)\;\n"
    }

    return $buffer
}

# Return a buffer that releases a local ExprValue
# back into the runtime pool of objects.

proc compileproc_expr_value_stack_release_generate {} {
    global _compileproc_expr_value_stack

    set buffer ""

    # Release most recently grabbed values first
    # so that cache is refreshed with newer
    # generation objects.

    set ordered_symbols $_compileproc_expr_value_stack(ordered_symbols)
    set i [llength $ordered_symbols]
    incr i -1

    set expr_value_stack_null $::_compileproc(options,expr_value_stack_null)

    for {} {$i >= 0} {incr i -1} {
        set symbol [lindex $ordered_symbols $i]

        # If expr value stack null is enabled, an
        # error could cause execution to jump
        # to the finally block without resetting
        # the ExprValue on the stack. Just ignore
        # a null value in this case.

        if {$expr_value_stack_null} {
            append buffer \
                [emitter_indent] \
                "if ( $symbol != null ) \{ TJC.exprReleaseValue(interp, $symbol)\; \}\n"
        } else {
            append buffer \
                [emitter_indent] \
                "TJC.exprReleaseValue(interp, $symbol)\;\n"
        }
    }

    return $buffer
}

# Loop over parsed command keys and determine information
# about each command and its arguments. Scanning the commands
# generates meta-data about the parse trees that is then
# used to generate code with specific optimizations enabled.
# The scan starts with the toplevel keys for a specific proc
# and descends into all the keys that are children of these
# toplevel keys. The scan will also descend into container
# commands if that option is enabled. The keys list indicates
# a "block" of commands, starting with the toplevel keys
# in a proc.

proc compileproc_scan_keys { keys } {
    global _compileproc _compileproc_key_info

#    set debug 0

#    if {$debug} {
#        puts "compileproc_scan_keys: \{$keys\}"
#    }

    set cmd_needs_init $_compileproc_key_info(cmd_needs_init)
    set constants_found $_compileproc_key_info(constants_found)

    # Create lookup table for command arguments. Figure out
    # types and values for each word as we iterate over the
    # command keys. The first time compileproc_scan_keys
    # in invoked, the toplevel keys in the proc are passed.

    set last_key [lindex $keys end]

    foreach key $keys {
        # If the command name is a simple string, then get
        # the command name String.
        if {[descend_arguments_undetermined $key]} {
            error "unexpected undetermined arguments command key \"$key\""
        }
        set script [descend_get_data $key script]
        set tree [descend_get_data $key tree]
        set num_args [llength $tree]
        if {$num_args < 1} {error "num args ($num_args) must be positive"}

#        if {$debug} {
#            puts "key is $key"
#            puts "script is ->$script<-"
#            puts "tree is \{$tree\}"
#        }

        set types [list] ; # type of value for argument
        set values [list] ; # value depends on type
        set cmaps [list] ; # map (orignal -> values) chars per character
        set instrs [list] ; # original Tcl string for argument

        # Get list of the keys for nested commands in each argument
        # to this command.
        set argument_commands [descend_commands $key]
        if {[llength $argument_commands] != $num_args} {
            # A command can't have zero arguments, so {} should not be returned by
            # descend_commands here. The descend module inits the commands flag
            # to a list of empty lists based on the number of arguments to the command.
            error "mismatched num_args ($num_args) and num argument_commands\
                [llength $argument_commands] for key $key, argument_commands
                is \{$argument_commands\}"
        }

        # Walk over each argument to the command looking for constant
        # TclObject values and register any that are found.
        set i 0
        foreach telem $tree {
            # See if this argument is a constant (simple/text) type
            # and create a class constant if it is one.
            if {[parse_is_simple_text $telem]} {
                set qtext [parse_get_simple_text $script $telem]
                if {[string index $qtext 0] == "\{" &&
                    [string index $qtext end] == "\}"} {
                    set brace_quoted 1
                } else {
                    set brace_quoted 0
                }
#                if {$debug} {
#                    puts "found simple/text ->$qtext<- at argument index $i"
#                    if {$brace_quoted} {
#                        puts "argument is a brace quoted"
#                    } else {
#                        puts "argument is not a brace quoted"
#                    }
#                }
                set uqtext [parse_get_simple_text $script $telem "text"]
#                if {$debug} {
#                    puts "found unquoted simple/text ->$uqtext<- at argument index $i"
#                }

                # A brace quoted argument like {foo\nbar} must be written
                # as the Java String "foo\\nbar". Find all backslash
                # chars and double backslash them. Also, save a map
                # of original Tcl characters to backslashed characters.

                if {$brace_quoted && [string first "\\" $uqtext] != -1} {
                    set cmap {}
                    set bs_uqtext ""

                    set len [string length $uqtext]
                    for {set i 0} {$i < $len} {incr i} {
                        set c [string index $uqtext $i]
                        if {$c == "\\"} {
                            append bs_uqtext "\\\\"
                            lappend cmap 2
                        } else {
                            append bs_uqtext $c
                            lappend cmap 1
                        }
                    }

#                    if {$debug} {
#                        puts "doubled up escapes in brace quoted string"
#                        puts "uqtext    ->$uqtext<-"
#                        puts "bs_uqtext ->$bs_uqtext<-"
#                        puts "cmap is \{$cmap\}"
#                    }

                    set uqtext $bs_uqtext
                } else {
                    set cmap {}
                }

                set constants_found 1
                lappend types constant
                lappend values $uqtext
                lappend instrs $qtext
                lappend cmaps $cmap
                compileproc_constant_cache_add $uqtext
            } elseif {[parse_is_word_variable $telem]} {
                # A word that contains a single variable can
                # have multiple types. The most simple is
                # a scalar. More complex types like arrays
                # will require a full evaluation.

#                if {$debug} {
#                    puts "found word/variable type at argument index $i"
#                }

                set commands [lindex $argument_commands $i]
                if {$commands != {}} {
                    # An array key contains commands
                    compileproc_childkey_reset $key $commands
                }

                set vstree [lindex $telem 2 0]
                set vinfo [compileproc_scan_variable $key $script $vstree]
                if {$commands != {}} {
                    compileproc_childkey_validate $key
                }

                lappend types variable
                lappend values $vinfo
                lappend instrs [parse_get_word_variable $script $telem]
                lappend cmaps {}
            } elseif {[parse_is_word_command $telem]} {
                # A word element that contains 0 to N nested
                # commands. Loop over each of the keys for
                # each nested command.

#                if {$debug} {
#                    puts "found word/command type: [parse_get_word_command $script $telem] at argument index $i"
#                }
                set commands [lindex $argument_commands $i]
                if {[compileproc_is_empty_command $commands]} {
                    # An empty command has no key. The result of an empty
                    # command is the empty list, so just pretend this is
                    # a constant ref to {}.
                    set uqtext {}
                    set constants_found 1
                    lappend types constant
                    lappend values $uqtext
                    lappend instrs {[]}
                    lappend cmaps {}
                    compileproc_constant_cache_add $uqtext
#                    if {$debug} {
#                        puts "found empty word/command, subst {} at argument index $i"
#                    }
                } else {
#                    if {$debug} {
#                        puts "found word/command keys: $commands at argument index $i"
#                    }

                    lappend types command
                    lappend values $commands
                    lappend instrs [parse_get_word_command $script $telem]
                    lappend cmaps {}
#                    if {$debug} {
#                    puts "scanning commands \{$commands\} (child of $key)"
#                    }
                    compileproc_scan_keys $commands
#                    if {$debug} {
#                    puts "done scanning commands \{$commands\} (child of $key)"
#                    }
                }
            } elseif {[parse_is_word $telem]} {
                # A word element made up of text, variables, and or commands

#                if {$debug} {
#                    puts "found word element type at index $i"
#                }

                set wrange [lindex $telem 1]
                set qtext [parse getstring $script $wrange]

                set commands [lindex $argument_commands $i]
                if {$commands != {}} {
                    # An array key that contains commands
                    compileproc_childkey_reset $key $commands
                }

                set wstree $telem
                set type_winfo [compileproc_scan_word $key $script $wstree]
                if {$commands != {}} {
                    compileproc_childkey_validate $key
                }
                set type [lindex $type_winfo 0]
                set value [lindex $type_winfo 1]
                set cmap [lindex $type_winfo 2]

                if {$type == "constant"} {
                    set constants_found 1
                    compileproc_constant_cache_add $value
                }
                lappend types $type
                lappend values $value
                lappend instrs $qtext
                lappend cmaps $cmap
            } else {
                error "unsupported type at argument index $i, telem is: \{$telem\}"
            }

            incr i
        }

        if {[llength $types] != [llength $values]} {
            error "num types vs values mismatch, [llength $types] != [llength $values]"
        }
        if {[llength $types] != $num_args} {
            error "num types vs num_args mismatch, [llength $types] != $num_args"
        }

        set _compileproc_key_info($key,types) $types
        set _compileproc_key_info($key,values) $values
        set _compileproc_key_info($key,instrs) $instrs
        set _compileproc_key_info($key,num_args) $num_args
        set _compileproc_key_info($key,cmaps) $cmaps

        if {$_compileproc(options,omit_results)} {
            # The last command in a given block could become
            # the result for the block. If this is not
            # the last command in the block then there
            # is no need to save the result. A nested
            # command invocation is considered a block.

#            if {$debug} {
#                puts "omit_results test: key is $key"
#            }

            set is_nested [descend_get_data $key nested]

#            if {$debug} {
#                puts "is_nested is $is_nested"
#            }

            set is_last_key [expr {$key == $last_key}]

#            if {$debug} {
#                puts "is_last_key is $is_last_key"
#            }

            if {$is_last_key} {
                if {[info exists _compileproc_key_info($key,container)]} {
                    set container_key $_compileproc_key_info($key,container)
                    set container_result $_compileproc_key_info($container_key,result)
                } else {
                    set container_key ""
                    set container_result 1
                }

#                if {$debug} {
#                    puts "container_key is $container_key"
#                    puts "container_result is $container_result"
#                }

                # Some container commands like "while" always reset
                # the interp result after executing commands. In
                # these cases, there is no need to actually set the
                # interp result for commands in the body block.

                if {$container_result && \
                        $container_key != "" && \
                        [compileproc_is_result_reset_container_command $container_key]} {
                    set container_result 0
                }

                # The "catch" command is a tricky special case. If a
                # variable is passed to the catch command then the
                # last command is the catch block will need to set
                # the result so that it can be saved into the var.
                # Note that this not depend on the result flag for
                # the container command.

                if {$container_key != ""} {
                    set tuple [compileproc_container_catch_with_varname $container_key]
                    if {[lindex $tuple 0]} {
                        set has_varname [lindex $tuple 1]

                        # set container_result to either true or false
                        # depending in the has_varname value.

                        set container_result $has_varname
                    }
                }
            } ; # end if {$is_last_key}

            if {$is_last_key && ($container_result || $is_nested)} {
                # Last command in block or contained command
                # is a nested command.
                set _compileproc_key_info($key,result) 1
            } else {
                # This command is not the last one in the block, or
                # it is inside a container and the result of the
                # container is not used.
                set _compileproc_key_info($key,result) 0
            }

#            if {$debug} {
#                puts "result used is $_compileproc_key_info($key,result)"
#            }
        }

        # If inlining of containers is enabled and this
        # container can be inlined, then scan the
        # commands contained inside it.

        if {[compileproc_is_container_command $key] &&
                [compileproc_can_inline_container $key]} {
            compileproc_scan_keys_in_container $key
        }
    } ; # end foreach key $keys loop

    if {$constants_found} {
        set cmd_needs_init 1
#        if {$debug} {
#            puts "compileproc_scan_keys: key_info printout"
#            parray _compileproc_key_info
#        }
    }

    set _compileproc_key_info(cmd_needs_init) $cmd_needs_init
    set _compileproc_key_info(constants_found) $constants_found
    return
}

# Scan each command key inside the container command indicated
# by the passed in key. This method is able to handle
# container commands like "while" that use a flat list of keys
# as well as complex commands like "if" that use lists
# containing sublists for each block.

proc compileproc_scan_keys_in_container { key } {
#    set debug 0

#    if {$debug} {
#        puts "compileproc_scan_keys_in_container: $key"
#    }

    set ccmds [descend_commands $key container]

#    if {$debug} {
#        puts "container keys are \{$ccmds\}"
#    }

    if {$ccmds == {}} {
        return
    }

    # If this is a "catch" or "foreach" command, need to
    # handle flat container command list so that all
    # keys in the block are handled correctly.

    set is_flat_list [compileproc_is_container_command_keys_flat $key]

    if {$is_flat_list} {
#        if {$debug} {
#        puts "container commands is a flat list"
#        }

        compileproc_scan_keys_in_container_sublist $key [list $ccmds]
    } else {
#        if {$debug} {
#        puts "container commands is not a flat list"
#        }

        compileproc_scan_keys_in_container_sublist $key $ccmds
    }
    return
}

# Scan input klists list, each element is a list of keys.
# This method is tricky because a list element can itself
# be a list of keys and this method will need to be
# invoked recursively in that case.

proc compileproc_scan_keys_in_container_sublist { key klists } {
    global _compileproc_key_info

#    set debug 0

#    if {$debug} {
#        puts "compileproc_scan_keys_in_container_sublist: $klists"
#    }

    # Iterate over each sublist passed in via klists

    foreach klist $klists {
        if {$klist == {}} {
            # Skip empty sublist
            continue
        }

        # Scan the elements of klist looking for an element
        # that is actually a sublist that indicates a nested
        # command. Scan these nested command keys with a
        # recursive invocation and then remove the sublist.

        set trimmed_klist [list]
        foreach ckey $klist {
            set len [llength $ckey]
            if {$len == 0} {
                # Ignore empty sublist
            } elseif {$len > 1} {
                compileproc_scan_keys_in_container_sublist $key [list $ckey]
            } else {
                lappend trimmed_klist $ckey
            }
        }
#        if {$debug} {
#            if {$trimmed_klist == $klist} {
#                puts "no sublist elements found in \{$klist\}"
#            } else {
#                puts "sublist elements trimmed out of klist, now \{$klist\}"
#            }
#        }
        set klist $trimmed_klist

        if {$klist == {}} {
            continue
        }

#        if {$debug} {
#        puts "scanning container commands \{$klist\} (child of $key)"
#        }

        # Define a "container" field for the keys that are
        # about to be scanned so that the contained command
        # key can be used to lookup the container key.

        foreach ckey $klist {
            set _compileproc_key_info($ckey,container) $key
        }

        compileproc_scan_keys $klist

#        if {$debug} {
#            puts "done scanning container commands \{$klist\} (child of $key)"
#        }
    } ; # end foreach klist in $klists

    return
}

# Returns true if the given dkey is an empty command, meaning
# it has no child keys.

proc compileproc_is_empty_command { keys } {
    #if {$keys == {} || $keys == {{}}}
    if {$keys == {}} {
        return 1
    } else {
        return 0
    }
}

# Reset child key counter and list of child keys.
# This list is used when iterating over a variable
# or a word that is an argument to a the command
# denoted by the key argument.

proc compileproc_childkey_reset { key ckeys } {
    global _compileproc _compileproc_ckeys
    set _compileproc_ckeys($key) $ckeys
    set _compileproc($key,childkey_counter) 0
    return
}

# Return the next child key for the given key and
# increment the child key counter.

proc compileproc_childkey_next { key } {
    global _compileproc
    set children [compileproc_key_children $key]
    set len [llength $children]
    if {$_compileproc($key,childkey_counter) >= $len} {
        error "no more children for $key, children are \{$children\}, index is\
            $_compileproc($key,childkey_counter)"
    }
    set ckey [lindex $children $_compileproc($key,childkey_counter)]
    incr _compileproc($key,childkey_counter)
    return $ckey
}

proc compileproc_childkey_validate { key } {
    global _compileproc
    set children [compileproc_key_children $key]
    set expected_children [llength $children]
    set num_processed $_compileproc($key,childkey_counter)

    if {$_compileproc($key,childkey_counter) != $expected_children} {
        error "expected $expected_children children, but processed\
            $num_processed for key $key, children are \{$children\}"
    }
}

# Given a variable subtree and the script it was generated from,
# scan the contents of the variable and return a list that
# describes how to evaluate the variable starting from the
# inner most value and progressing to the outer most one.

proc compileproc_scan_variable { key script vstree } {
    global _compileproc
#    set debug 0

    if {[parse_is_scalar_variable $vstree]} {
        set vname [parse_get_scalar_variable $script $vstree]
        set vinfo [list scalar $vname]

#        if {$debug} {
#            puts "found scalar variable type"
#        }
    } else {
        # Parse array variable into a vinfo list that is
        # evaluated by compileproc_emit_variable.

        set saved_var_scan_key $_compileproc(var_scan_key)
        set saved_var_scan_results $_compileproc(var_scan_results)
        set _compileproc(var_scan_key) $key
        set _compileproc(var_scan_results) {}

        parse_variable_iterate $script $vstree \
            _compileproc_scan_variable_iterator

        set vinfo $_compileproc(var_scan_results)
        set _compileproc(var_scan_key) $saved_var_scan_key
        set _compileproc(var_scan_results) $saved_var_scan_results

#        if {$debug} {
#            puts "found array variable type"
#        }
    }

    return $vinfo
}

# Callback invoked while variable elements are being scanned. Variables
# are fully scanned before any code is generated.

proc _compileproc_scan_variable_iterator { script stree type values ranges } {
    global _compileproc
    upvar #0 _compileproc(var_scan_results) results

#    set debug 0

#    if {$debug} {
#        puts "_compileproc_scan_variable_iterator : \{$type\} \{$values\} \{$ranges\}"
#    }

    # Use array type info from parse layer to decide how to
    # handle the variable.

    switch -exact -- $type {
        {scalar} {
            # Scalar variable: $v
            lappend results $type [lindex $values 0]
        }
        {array text} {
            # Array with a text string key: $a(k)
            lappend results $type $values
        }
        {array scalar} {
            # Array with a scalar variable key: $a($k)
            lappend results $type $values
        }
        {array command} {
            # Array with a command key: $a([cmd])
            set key $_compileproc(var_scan_key)
            set ckeys [compileproc_childkey_next $key]
            if {[compileproc_is_empty_command $ckeys]} {
                # An empty command evaluates to the empty list,
                # in an array variable word this is a key
                # that is the empty string.
                set ckeys {}
            } else {
#                if {$debug} {
#                puts "scanning child keys \{$ckeys\} (child of $key)"
#                }
                # There does not appear to be a way for an {array command}
                # to have anything other than a flat list of keys, so
                # no need to loop over keys looking for {}.
                compileproc_scan_keys $ckeys
#                if {$debug} {
#                puts "done scanning child keys \{$ckeys\} (child of $key)"
#                }
            }
            lappend results $type [list [lindex $values 0] $ckeys]
        }
        {array word} {
            # complex array key case, either a word made
            # up of text, command, and variable elements
            # or an array key that is itself an array.
            lappend results $type $values
        }
        {word begin} {
            # Begin processing word elements for complex
            # word as array key
        }
        {word end} {
            # End processing of word elements for complex
            # word as array key
        }
        {word text} {
            # word element that is a text string

            set word [parse_variable_iterate_word_value]
            lappend word [list "text" [lindex $values 0]]
            parse_variable_iterate_word_value $word
        }
        {word scalar} {
            # word element that is a scalar variable

            set word [parse_variable_iterate_word_value]
            lappend word [list scalar [lindex $values 0]]
            parse_variable_iterate_word_value $word
        }
        {word command} {
            # word element that is a command
            set key $_compileproc(var_scan_key)
            set ckeys [compileproc_childkey_next $key]
            if {[compileproc_is_empty_command $ckeys]} {
                # An empty command evaluates to the empty list,
                # in an array variable word this is a key
                # that is the empty string.
                set ckeys {}
                compileproc_constant_cache_add {}
            } else {
#                if {$debug} {
#                puts "scanning child keys \{$ckeys\} (child of $key)"
#                }
                compileproc_scan_keys $ckeys
#                if {$debug} {
#                puts "done scanning child keys \{$ckeys\} (child of $key)"
#                }
            }

            set word [parse_variable_iterate_word_value]
            lappend word [list $type $ckeys]
            parse_variable_iterate_word_value $word
        }
        {word array text} {
            # word element that is an array variable with a text key

            set word [parse_variable_iterate_word_value]
            lappend word [list {array text} $values]
            parse_variable_iterate_word_value $word
        }
        {word array scalar} {
            # word element that is an array variable with a scalar key

            set word [parse_variable_iterate_word_value]
            lappend word [list {array scalar} $values]
            parse_variable_iterate_word_value $word
        }
        {word array command} {
            # word element that is an array variable with a command key
            set key $_compileproc(var_scan_key)
            set ckeys [compileproc_childkey_next $key]
            if {[compileproc_is_empty_command $ckeys]} {
                # An empty command evaluates to the empty list,
                # in an array variable word this is a key
                # that is the empty string.
                set ckeys {}
                compileproc_constant_cache_add {}
            } else {
#                if {$debug} {
#                puts "scanning child key \{$ckeys\} (child of $key)"
#                }
                compileproc_scan_keys $ckeys
#                if {$debug} {
#                puts "done scanning child key \{$ckeys\} (child of $key)"
#                }
            }

            set word [parse_variable_iterate_word_value]
            lappend word [list {array command} \
                [list [lindex $values 0] $ckeys]]
            parse_variable_iterate_word_value $word
        }
        {word array word} {
            # word element that is an array with a word key

            set word [parse_variable_iterate_word_value]
            lappend word [list {array word} $values]
            parse_variable_iterate_word_value $word
        }
        default {
            error "unknown variable type \{$type\}"
        }
    }
}

# Scan contents of a word subtree and return a formatted
# description of the word elements.

proc compileproc_scan_word { key script wstree } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_scan_word $key \{$script\} \{$wstree\}"
#    }

    # Parse word elements into a list that describes
    # each word element.

    set saved_word_scan_key $_compileproc(word_scan_key)
    set saved_word_scan_results $_compileproc(word_scan_results)
    set _compileproc(word_scan_key) $key
    set _compileproc(word_scan_results) {}

    parse_word_iterate $script $wstree _compileproc_scan_word_iterate

    set winfo $_compileproc(word_scan_results)
    set _compileproc(word_scan_key) $saved_word_scan_key
    set _compileproc(word_scan_results) $saved_word_scan_results

#    if {$debug} {
#        puts "checking returned word info \{$winfo\}"
#    }

    # If each element is a {text} element, then combine them
    # all into a single constant string and return a constant
    # string type. This can happen when a backslash is found
    # inside an otherwise constant string.
    # Return a word type if a non-text element is found.

    set all_text 1
    foreach wi $winfo {
        set type [lindex $wi 0]
        if {$type != "text"} {
            set all_text 0
            break
        }
    }
    if {$all_text} {
#        if {$debug} {
#            puts "all word types were {text}, returning constant string"
#        }
        set all_str ""
        set has_escapes 0
        set cmap [list]
        foreach wi $winfo {
            set str [lindex $wi 1]
            append all_str $str

            if {[string index $str 0] == "\\"} {
                set has_escapes 1
                lappend cmap [string length $str]
            } else {
                # Not an escape string, add a map
                # entry for each plain character.
                set len [string length $str]
                for {set i 0} {$i < $len} {incr i} {
                    lappend cmap 1
                }
            }
        }
        # Don't bother with map for string with no escapes.
        if {!$has_escapes} {
            set cmap {}
        }
        return [list constant $all_str $cmap]
    } else {
        return [list word $winfo {}]
    }
}

proc _compileproc_scan_word_iterate { script stree type values ranges } {
    global _compileproc

#    set debug 0

    upvar #0 _compileproc(word_scan_results) results
    set key $_compileproc(word_scan_key)

#    if {$debug} {
#        puts "_compileproc_scan_word_iterate : \"$script\" \{$stree\} $type \{$values\} \{$ranges\}"
#    }

    switch -exact -- $type {
        {backslash} {
            # If backslashed element can be represented as an identical
            # backslash element in Java source code, then use it as is.
            # Otherwise, convert it to a representation that is valid.
            set elem "\\[lindex $values 0]"
            set jelem [emitter_backslash_tcl_elem $elem]

            set word [parse_word_iterate_word_value]
            lappend word [list {text} $jelem]
            parse_word_iterate_word_value $word
        }
        {command} {
            set ckeys [compileproc_childkey_next $key]
            if {[compileproc_is_empty_command $ckeys]} {
                # An empty command evaluates to the empty list,
                # in an array variable word this is a key
                # that is the empty string.
                set ckeys {}
                compileproc_constant_cache_add {}
            } else {
#                if {$debug} {
#                puts "scanning child key \{$ckeys\} (child of $key)"
#                }
                compileproc_scan_keys $ckeys
#                if {$debug} {
#                puts "done scanning child key \{$ckeys\} (child of $key)"
#                }
            }

            set word [parse_word_iterate_word_value]
            lappend word [list $type $ckeys]
            parse_word_iterate_word_value $word
        }
        {text} {
            set word [parse_word_iterate_word_value]
            lappend word [list $type [lindex $values 0]]
            parse_word_iterate_word_value $word
        }
        {variable} {
            # Create list that describes this variable, it
            # will be used to emit code to query the variable
            # value.

            set vstree $stree
            set vinfo [compileproc_scan_variable $key $script $vstree]

            set word [parse_word_iterate_word_value]
            lappend word [list $type $vinfo]
            parse_word_iterate_word_value $word
        }
        {word begin} {
            # No-op
        }
        {word end} {
            # No-op
        }
        {word} {
            # Word result available
            set results $values
        }
        default {
            error "unknown type \"$type\""
        }
    }
}

# Return a tuple of {TYPE PSTR} for an argument.
# The pstr value is a printable string that
# describes the argument. This is commonly used
# to print an argument description inside a comment.

proc compileproc_argument_printable { key i } {
    # Return string the describes the argument.
    # Use "..." if the string would not print
    # as simple text.

    set type [lindex $::_compileproc_key_info($key,types) $i]
    if {$type == {}} {
        if {$i < 0 || \
                $i >= $::_compileproc_key_info($key,num_args)} {
            error "index $i out of argument range"
        }
    }

    set print 1
    switch -exact -- $type {
        "constant" {
            set str [lindex $::_compileproc_key_info($key,instrs) $i]
            if {[string length $str] > 20} {
                set print 0
            }
        }
        "variable" {
            set str [lindex $::_compileproc_key_info($key,instrs) $i]
            if {[string length $str] > 20} {
                set str "\$..."
            }
        }
        "command" {
            # No need to print nested command text here since
            # the command name will be printed in the invocation.
            set str "\[...\]"
        }
        "word" {
            set str [lindex $::_compileproc_key_info($key,instrs) $i]
            if {[string length $str] > 20} {
                set str "\"...\""
            }
        }
        default {
            error "unknown type \"$type\" at index $i"
        }
    }
    # Don't print argument that contains a funky string like a newline.
    if {$print} {
        if {\
                [string first "\a" $str] != -1 ||
                [string first "\b" $str] != -1 ||
                [string first "\f" $str] != -1 ||
                [string first "\n" $str] != -1 ||
                [string first "\r" $str] != -1 ||
                [string first "\t" $str] != -1 ||
                [string first "\v" $str] != -1 ||
                [string first "\\" $str] != -1} {
            set print 0
        }
    }

    if {!$print} {
        set str "..."
    }
    return [list $type $str]
}

# Emit code that will invoke a Tcl method. The descend key for
# the method invocation to be written is passed.

proc compileproc_emit_invoke { key } {
    global _compileproc_key_info

#    set debug 0

#    if {$debug} {
#        puts "compileproc_emit_invoke $key"
#    }

    set num_args $_compileproc_key_info($key,num_args)

    # Create string the describes the command
    # and all the arguments to the command.

    set cmdstr ""
    set cmdname "..."
    for {set i 0} {$i < $num_args} {incr i} {
        set tuple [compileproc_argument_printable $key $i]
        set type [lindex $tuple 0]
        set str [lindex $tuple 1]
        if {$i == 0 && $type == "constant"} {
            set cmdname $str
        }

        if {$i < ($num_args - 1)} {
            append cmdstr $str " "
        } else {
            append cmdstr $str
        }
    }

    # Open method invocation block
    append buffer [emitter_invoke_start $cmdstr]

    # If the command can be inlined, then do that now.
    # Otherwise, emit a regular invoke() call.

    if {[compileproc_is_container_command $key] &&
            [compileproc_can_inline_container $key]} {
        append buffer [compileproc_emit_container $key]
    } elseif {[compileproc_can_inline_control $key]} {
        append buffer [compileproc_emit_control $key]
    } elseif {[compileproc_can_inline_command $key]} {
        append buffer [compileproc_emit_inline_command $key]
    } else {
        append buffer [compileproc_emit_invoke_call $key]
    }

    # Close method invocation block
    append buffer [emitter_invoke_end $cmdname]

    return $buffer
}

# Emit call to TJC.invoke() to directly invoke a Tcl
# command via its Command.cmdProc() implementation.
# Pass the descend key for the command to invoke.

proc compileproc_emit_invoke_call { key } {
    return [compileproc_emit_objv_assignment \
        $key \
        0 end \
        {} 0 \
        compileproc_emit_invoke_call_impl \
        {} \
        ]
}

# Emit code to allocate an array of TclObjects and
# assign command arguments to the array. This
# command will invoke a specific callback to
# emit code that appears after the array has
# been allocated and populated.
#
# key : dkey for command
# starti : integer index of first argument to assign
# endi : integer index of last argument to assign
# tmpsymbol : pass already declared TclObject, {} if none
# force_decl_tmpsymbol : pass true to force declaration of
#     TclObject temp symbol before the try loop. This
#     flag is only used if tmpsymbol is {}.
# callback : command to invoke after array has been populated.
# userdata : user supplied data to pass into callback command

proc compileproc_emit_objv_assignment { key starti endi \
        tmpsymbol force_decl_tmpsymbol \
        callback userdata } {
    global _compileproc
    global _compileproc_key_info

    set num_args $_compileproc_key_info($key,num_args)

    if {$endi == "end"} {
        set endi $num_args
    }

    # Init buffer with array symbol declaration
    # and an optional tmp symbol. Declare the
    # tmp symbol before the try block when
    # the use_tmp_local is set to true because
    # a non-constant argument was passed or
    # because the force_decl_tmpsymbol flag
    # was true. This maintains compatibility
    # with older test output.

    set buffer ""
    set arraysym [compileproc_tmpvar_next objv]
    set objv_size [expr {$num_args - $starti}]
    append buffer [emitter_invoke_command_start $arraysym $objv_size]

    # If all the arguments are constant values and
    # the emitted code should skip constant increments,
    # then there is no need to declare a tmp local.

    if {$_compileproc(options,skip_constant_increment)} {
        set use_tmp_local 0

        for {set i $starti} {$i < $endi} {incr i} {
            set tuple [compileproc_get_argument_tuple $key $i]
            set type [lindex $tuple 0]
            if {$type != "constant"} {
                set use_tmp_local 1
            }
        }
    } else {
        set use_tmp_local 1
    }

    if {!$use_tmp_local && $force_decl_tmpsymbol} {
        set use_tmp_local 1
    }

    # Declare tmp symbol before the try block only
    # when skipping const increment.

    if {!$_compileproc(options,skip_constant_increment)} {
        append buffer [emitter_container_try_start]
    }

    if {$tmpsymbol != {}} {
        # Use passed in TclObject tmpsymbol
    } elseif {$use_tmp_local} {
        set tmpsymbol [compileproc_tmpvar_next]
        append buffer [emitter_indent] "TclObject " $tmpsymbol "\;\n"
    }

    # Declare tmp symbol before the try block only
    # when skipping const increment.

    if {$_compileproc(options,skip_constant_increment)} {
        append buffer [emitter_container_try_start]
    }

    # Evaluate each argument to the command, increment the
    # ref count if needed, and save the TclObject into
    # the argument array.

    set const_unincremented_indexes [list]

    for {set i $starti} {$i < $endi} {incr i} {
        set tuple [compileproc_emit_argument $key $i 0 $tmpsymbol]
        set type [lindex $tuple 0]
        set symbol [lindex $tuple 1]
        set symbol_buffer [lindex $tuple 2]

        # Generate a string description of the argument before
        # evaluating the value. This makes code easier to debug
        # since constant values appear in the comments.

        set print_tuple [compileproc_argument_printable $key $i]
        set print_type [lindex $print_tuple 0]
        set print_str [lindex $print_tuple 1]

        append buffer \
            [emitter_comment "Arg $i $print_type: $print_str"]

        set array_i [expr {$i - $starti}]

        if {$_compileproc(options,skip_constant_increment) \
                && $type == "constant"} {
            append buffer [emitter_array_assign $arraysym $array_i $symbol]

            lappend const_unincremented_indexes $array_i
        } else {
            # emitter_invoke_command_assign will notice when the
            # tmpsymbol is being assigned to itself and skip it.

            append buffer $symbol_buffer \
                [emitter_invoke_command_assign $arraysym \
                    $array_i $tmpsymbol $symbol]
        }
    }

    # Invoke user supplied callback to emit code that appears
    # after the array has been populated.

    append buffer [$callback $key $arraysym $tmpsymbol $userdata]

    # Close try block and emit finally block

    append buffer [emitter_invoke_command_finally]

    # Special code for case where constant
    # increment is skipped and one or more arguments
    # is a constant. If all the arguments are constants
    # then just release the array. Otherwise, unroll
    # the non-constant release() loop and release the array.

    set release_none_flag false

    if {$const_unincremented_indexes != {}} {
        if {[llength $const_unincremented_indexes] == $objv_size} {
            # Special case, all the arguments are constants so
            # there is no need to release any of them.

            set release_none_flag 1
        } else {
            # No need to release the constant elements, but
            # invoke TclObject.release() for each non-constant.
            # Each statement must appear in an if block in case
            # the statements above exited the try early.
            # This unrolls the release loop in TJC.releaseObjvElems(),
            # testing shows this is faster than invoking the method.
            # Leave the old set array to null code in place if there
            # are more than 4 inlined release() calls.

            if {($objv_size - [llength $const_unincremented_indexes]) <= 4} {
                if {$tmpsymbol == ""} {
                    error "tmp local should have been declared"
                }
                set tmpsymbol_if_block ""
                append tmpsymbol_if_block \
                    [emitter_container_if_start "$tmpsymbol != null"] \
                    [emitter_tclobject_release $tmpsymbol] \
                    [emitter_container_if_end]

                for {set i 0} {$i < $objv_size} {incr i} {
                    set is_non_constant 1
                    foreach cui $const_unincremented_indexes {
                        if {$i == $cui} {
                            set is_non_constant 0
                        }
                    }
                    if {$is_non_constant} {
                        append buffer [emitter_indent] \
                            $tmpsymbol " = " $arraysym "\[" $i "\]" "\;\n" \
                            $tmpsymbol_if_block
                    }
                }
                set release_none_flag 1
            } else {
                foreach i $const_unincremented_indexes {
                    append buffer [emitter_array_assign $arraysym $i null]
                }
            }
        }
    }

    append buffer [emitter_invoke_command_end $arraysym $objv_size $release_none_flag]

    return $buffer
}

# Emit TJC.invoke() for either the runtime lookup case or the
# cached command case. The arraysym is the symbol declared
# as a TclObject[]. The tmpsymbol is a symbol declared as
# as TclObject used to store a tmp result inside the try block,
# it will be {} if no tmpsymbol was declared.

proc compileproc_emit_invoke_call_impl { key arraysym tmpsymbol userdata } {
    global _compileproc

    set buffer ""

    # Check to see if cached command pointer should be passed.

    if {$_compileproc(options,cache_commands)} {
        set cmdsym [compileproc_command_cache_lookup $key]

        if {$cmdsym == {}} {
            set cmdsym_buffer {}
        } else {
            # Emit code to check that cached command is valid
            set cmdsym_buffer [compileproc_command_cache_epoch_check $cmdsym]
        }

        # Check for change in containing commands's cmdEpoch
        if {$cmdsym != {}} {
            append buffer [compileproc_command_cache_this_check]
        }

        # Emit invoke()
        append buffer [emitter_invoke_command_call $arraysym $cmdsym_buffer 0]

        # Emit command cache update check and function call
        if {$cmdsym != {}} {
            append buffer [compileproc_command_cache_update $cmdsym]
        }
    } else {
        # Emit invoke()
        append buffer [emitter_invoke_command_call $arraysym {} 0]
    }

    return $buffer
}

# Emit code to evaluate the value of a Tcl command
# argument. This method will return a tuple of
# {TYPE SYMBOL BUFFER}. A constant argument will be
# added to the constant pool by this method. By
# default a TclObject will be declared to contain
# the result. If declare_flag is false then the variable
# declaration will be left up to the caller.

proc compileproc_emit_argument { key i {declare_flag 1} {symbol_name {}} } {
    global _compileproc_key_info

#    set debug 0

#    if {$debug} {
#        puts "compileproc_emit_argument $key $i $declare_flag \{$symbol_name\}"
#    }

    if {$declare_flag} {
        set declare "TclObject "
    } else {
        set declare ""
    }

    set types $_compileproc_key_info($key,types)
    set values $_compileproc_key_info($key,values)
    set num_args $_compileproc_key_info($key,num_args)

    set buffer ""
    set symbol ""

    if {$i < 0 || $i >= $num_args} {
        error "command argument index $i is out of range : num_args is $num_args"
    } else {
        set type [lindex $types $i]
        switch -exact -- $type {
            "constant" {
                set str [lindex $values $i]
                set ident [compileproc_constant_cache_get $str]
                set symbol $ident
            }
            "variable" {
                # Evaluate variable, save result into a tmp symbol,
                # then add the tmp to the argument array.
                set vinfo [lindex $values $i]

                if {$symbol_name == {}} {
                    set tmpsymbol [compileproc_tmpvar_next]
                } else {
                    set tmpsymbol $symbol_name
                }
                set symbol $tmpsymbol

                append buffer \
                    [compileproc_emit_variable $tmpsymbol $vinfo $declare_flag]
            }
            "command" {
                set ckeys [lindex $values $i]
                # Empty command replaced with ref to {} in key scan
                if {[compileproc_is_empty_command $ckeys]} {
                    error "unexpected empty nested command for key $key"
                }

                if {$symbol_name == {}} {
                    set tmpsymbol [compileproc_tmpvar_next]
                } else {
                    set tmpsymbol $symbol_name
                }
                set symbol $tmpsymbol

                # Emit 1 to N invocations
                foreach ckey $ckeys {
                    append buffer [compileproc_emit_invoke $ckey]
                }

                append buffer [emitter_indent] \
                    $declare $tmpsymbol " = interp.getResult()\;\n"
            }
            "word" {
                # Concatenate word elements together and save the
                # result into a tmp symbol.
                set winfo [lindex $values $i]

                if {$symbol_name == {}} {
                    set tmpsymbol [compileproc_tmpvar_next]
                } else {
                    set tmpsymbol $symbol_name
                }
                set symbol $tmpsymbol

                append buffer \
                    [compileproc_emit_word $tmpsymbol $winfo $declare_flag]
            }
            default {
                error "unknown type \"$type\""
            }
        }
    }

    if {$symbol == ""} {error "empty symbol"}

    return [list $type $symbol $buffer]
}

# Return a tuple {TYPE INFO INSTR CMAP} for the argument
# at the given index.

proc compileproc_get_argument_tuple { key i } {
    global _compileproc_key_info

#    set debug 0

#    if {$debug} {
#        puts "compileproc_get_argument_type $key $i"
#    }

    set types $_compileproc_key_info($key,types)
    set values $_compileproc_key_info($key,values)
    set num_args $_compileproc_key_info($key,num_args)
    set instrs $_compileproc_key_info($key,instrs)
    set cmaps $_compileproc_key_info($key,cmaps)

    if {$i < 0 || $i >= $num_args} {
        error "command argument index $i is out of range : num_args is $num_args"
    } else {
        set type [lindex $types $i]
        switch -exact -- $type {
            "constant" -
            "variable" -
            "command" -
            "word" {
                return [list $type \
                    [lindex $values $i] \
                    [lindex $instrs $i] \
                    [lindex $cmaps $i] \
                    ]
            }
            default {
                error "unknown type \"$type\""
            }
        }
    }
}

# Generate code to determine a variable value at runtime
# and assign the value the the given tmpsymbol. This
# method is the primary entry point for variable evaluation.

proc compileproc_emit_variable { tmpsymbol vinfo {declare_flag 1} } {
    global _compileproc

#    set debug 0

    set buffer ""
    if {$declare_flag} {
        set declare "TclObject "
    } else {
        set declare ""
    }
    set vtype [lindex $vinfo 0]

    switch -exact -- $vtype {
        {scalar} {
            set vname [lindex $vinfo 1]
            append buffer \
                [compileproc_emit_scalar_variable_get $declare$tmpsymbol $vname]
        }
        {array text} {
            set avname [lindex $vinfo 1 0]
            if {$avname == ""} {error "empty array name in \{$vinfo\}"}
            set kname [lindex $vinfo 1 1]
            # Note: array key can be "" here
            #if {$kname == ""} {error "empty array key name in \{$vinfo\}"}
            append buffer \
                [compileproc_emit_array_variable_get $declare$tmpsymbol $avname $kname true]
        }
        {array scalar} {
            set avname [lindex $vinfo 1 0]
            if {$avname == ""} {error "empty array name in \{$vinfo\}"}
            set kvname [lindex $vinfo 1 1]
            if {$kvname == ""} {error "empty array key variable name in \{$vinfo\}"}
            append buffer \
                [compileproc_emit_scalar_variable_get $declare$tmpsymbol $kvname] \
                [compileproc_emit_array_variable_get $tmpsymbol $avname \
                    $tmpsymbol.toString() false]
        }
        {array command} {
            set avname [lindex $vinfo 1 0]
            if {$avname == ""} {error "empty array name in \{$vinfo\}"}
            set ckeys [lindex $vinfo 1 1]
            if {[compileproc_is_empty_command $ckeys]} {
                # Empty command
                append buffer \
                    [compileproc_emit_array_variable_get $declare$tmpsymbol $avname \
                        "" true]
            } else {
                # Emit 1 to N invocations, then query results
                foreach ckey $ckeys {
                    append buffer [compileproc_emit_invoke $ckey]
                }
                append buffer \
                    [emitter_indent] \
                        $declare $tmpsymbol " = interp.getResult()" \
                        "\;\n" \
                    [compileproc_emit_array_variable_get $tmpsymbol $avname \
                        $tmpsymbol.toString() false]
            }
        }
        {word command} {
            set ckeys [lindex $vinfo 1 0]
            if {[compileproc_is_empty_command $ckeys]} {
                # Empty command, emit ref to constant empty Tcl string.
                set esym [compileproc_constant_cache_get {}]
                append buffer \
                    [emitter_indent] \
                    $declare $tmpsymbol " = " $esym "\;\n"
            } else {
                # Emit 1 to N invocations, then query results
                foreach ckey $ckeys {
                    append buffer [compileproc_emit_invoke $ckey]
                }
                append buffer \
                    [emitter_indent] \
                        $declare $tmpsymbol " = interp.getResult()" \
                        "\;\n"
            }
        }
        {array word} {
            set avname [lindex $vinfo 1 0]
            if {$avname == ""} {error "empty array name in \{$vinfo\}"}
            set values [lindex $vinfo 1 1]
            if {[llength $values] == 0} {error "empty array values in \{$vinfo\}"}
            # Declare tmp variable unless it was already defined earlier.
            if {$declare_flag} {
                append buffer [emitter_indent] \
                    $declare $tmpsymbol "\;\n"
            }
            if {[llength $values] > 1} {
                # Multiple values to concatenate into a single word
                set sbtmp [compileproc_tmpvar_next sbtmp]
                append buffer \
                    [emitter_statement \
                    "StringBuffer $sbtmp = new StringBuffer(64)"]
                foreach value $values {
                    set type [lindex $value 0]
                    if {$type == "text"} {
                        set str [emitter_backslash_tcl_string [lindex $value 1]]
                        # FIXME: A constant String of length 1 takes up space
                        # in the string table for each command. If there is
                        # only 1 character, then use StringBuffer.append(int 'C')
                        # where C is the character in question.

                        # A constant string, just append it to the StringBuffer
                        append buffer [emitter_indent] \
                            $sbtmp ".append(\"" $str "\")\;\n"
                    } else {
                        # A variable or command that must be evaluated then appended
                        append buffer \
                            [compileproc_emit_variable $tmpsymbol $value 0] \
                            [emitter_statement "$sbtmp.append($tmpsymbol.toString())"]
                    }
                }
                set result $sbtmp.toString()
            } else {
                # A single word value, no need to concat results together.
                set value [lindex $values 0]
                set type [lindex $value 0]
                if {$type == "text"} {
                    # A complex word with a single value would not be a constant
                    error "unexpected single constant value for word"
                }
                append buffer [compileproc_emit_variable $tmpsymbol $value 0]
                set result $tmpsymbol.toString()
            }
            # Finally, evaluate the array with the word value as the key
            append buffer \
                [compileproc_emit_array_variable_get $tmpsymbol $avname $result false]
        }
        default {
            error "unhandled non-scalar type \"$vtype\" in vinfo \{$vinfo\}"
        }
    }

    return $buffer
}

# Emit code to get the value of a scalar variable and assign
# it to a tmpsymbol of type TclObject. This method
# is used by compileproc_emit_variable for scalars. A local
# variable scalar or a fully qualified global namespace
# qualifier like $::myglobal are both supported. A namespace
# relative scoped qualifier like $child::var can't be
# cached and is not added to the compiled local table.

proc compileproc_emit_scalar_variable_get { tmpsymbol vname } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_emit_scalar_variable_get $tmpsymbol $vname"
#    }

    if {([string first "::" $vname] == -1 || \
                [string range $vname 0 1] == "::") && \
            [info exists _compileproc(options,cache_variables)] && \
            $_compileproc(options,cache_variables)} {
        set symbol [compileproc_variable_cache_lookup $vname]
        set cacheId [compileproc_get_variable_cache_id_from_symbol $symbol]

        set buffer ""

        # Need to emit special init call for scoped vars to init link
        if {[string range $vname 0 1] == "::"} {
            append buffer \
                [emitter_init_compiled_local_scoped_var $vname \
                    "compiledLocals" $cacheId]
        }

        append buffer [emitter_get_compiled_local_scalar_var \
            $tmpsymbol $vname "compiledLocals" $cacheId]
    } else {
        set buffer [emitter_get_var $tmpsymbol $vname true null false 0]
    }
    return $buffer
}

# Emit code to set the value of a scalar variable. This method
# assigns a new value to a scalar variable and returns an
# assignable value of type TclObject. This method is used
# throughout this module to set a scalar variable value. A namespace
# relative scoped qualifier like $child::var can't be
# cached and is not added to the compiled local table.

proc compileproc_emit_scalar_variable_set { tmpsymbol vname value } {
    global _compileproc

#    puts "compileproc_emit_scalar_variable_set $tmpsymbol $vname $value"

    if {([string first "::" $vname] == -1 || \
                [string range $vname 0 1] == "::") && \
            [info exists _compileproc(options,cache_variables)] && \
            $_compileproc(options,cache_variables)} {
        set symbol [compileproc_variable_cache_lookup $vname]
        set cacheId [compileproc_get_variable_cache_id_from_symbol $symbol]

        set buffer ""

        # Need to emit special init call for scoped vars to init link

        if {[string range $vname 0 1] == "::"} {
            append buffer \
                [emitter_init_compiled_local_scoped_var $vname \
                    "compiledLocals" $cacheId]
        }

        append buffer [emitter_set_compiled_local_scalar_var \
            $tmpsymbol $vname $value "compiledLocals" $cacheId]
    } else {
        set buffer [emitter_set_var $tmpsymbol $vname true null false $value 0]
    }

    return $buffer
}

# Emit code to get the value of an array variable and assign
# it to a tmpsymbol of type TclObject. This method is
# used by compileproc_emit_variable for arrays. A local
# variable array or a fully qualified global namespace
# qualifier like $::myglobal(elem) are both supported.
# A namespace relative scoped qualifier like $child::var(elem)
# can't be cached and is not added to the compiled local table.
#
# tmpsymbol : TclObject symbol to assign get result to
# vname : name of variable
# key : array element key
# key_is_string : true if key is a constant literal String.

proc compileproc_emit_array_variable_get { tmpsymbol vname key key_is_string } {
    global _compileproc

#    puts "compileproc_emit_array_variable_get $tmpsymbol $vname $key $key_is_string"

    if {([string first "::" $vname] == -1 ||
                [string range $vname 0 1] == "::") && \
            [info exists _compileproc(options,cache_variables)] && \
            $_compileproc(options,cache_variables)} {
        set symbol [compileproc_variable_cache_lookup $vname]
        set cacheId [compileproc_get_variable_cache_id_from_symbol $symbol]

        set buffer ""

        # Need to emit special init call for scoped vars to init link

        if {[string range $vname 0 1] == "::"} {
            append buffer \
                [emitter_init_compiled_local_scoped_var $vname \
                    "compiledLocals" $cacheId]
        }

        append buffer [emitter_get_compiled_local_array_var \
            $tmpsymbol $vname $key $key_is_string "compiledLocals" $cacheId]
    } else {
        set buffer [emitter_get_var $tmpsymbol $vname true $key $key_is_string 0]
    }
    return $buffer
}

# Emit code to set the value of an array variable and assign
# the result to a tmpsymbol of type TclObject. This method
# will assign a new value to an array element. This method is used
# throughout this module to set an array variable value.
# A absolute namespace scope array name like "::arr" is supported.
# A namespace relative scoped qualifier like $child::var can't be
# cached and is not added to the compiled local table.
# The key argument is the array key to be looked up, if
# key_is_string is true it will be a literal string.

proc compileproc_emit_array_variable_set { tmpsymbol vname key key_is_string value } {
    global _compileproc

#    puts "compileproc_emit_array_variable_set $tmpsymbol $vname $value $key $key_is_string ..."

    if {([string first "::" $vname] == -1 || \
                [string range $vname 0 1] == "::") && \
            [info exists _compileproc(options,cache_variables)] && \
            $_compileproc(options,cache_variables)} {
        set symbol [compileproc_variable_cache_lookup $vname]
        set cacheId [compileproc_get_variable_cache_id_from_symbol $symbol]

        set buffer ""

        # Need to emit special init call for scoped vars to init link

        if {[string range $vname 0 1] == "::"} {
            append buffer \
                [emitter_init_compiled_local_scoped_var $vname \
                    "compiledLocals" $cacheId]
        }

        append buffer [emitter_set_compiled_local_array_var \
            $tmpsymbol $vname $key $key_is_string $value "compiledLocals" $cacheId]
    } else {
        set buffer [emitter_set_var $tmpsymbol $vname true $key $key_is_string $value 0]
    }

    return $buffer
}

# Determine a word value at runtime and emit code
# to assign the value of a TclObject result to
# a local indicated by tmpsymbol. The tmpsymbol
# can't be {} but it can be declared by this
# method if declare_flag is true.
# In the special case where the caller wants
# the evaluation result as a String, the
# string_symbol can be passed.

proc compileproc_emit_word { tmpsymbol winfo {declare_flag true} {string_symbol {}} } {
#    set debug 0
#
#    if {$debug} {
#        puts "compileproc_emit_word $tmpsymbol $winfo $declare_flag $string_symbol"
#    }

    set buffer ""

    if {$declare_flag} {
        set declare "TclObject "
    } else {
        set declare ""
    }

    set len [llength $winfo]
    if {$len == 0} {
        error "empty winfo"
    } elseif {$len == 1} {
        # A word that contains a single element, either a variable
        # or a command.
        set wi [lindex $winfo 0]
        append buffer [compileproc_emit_word_element $wi $tmpsymbol false {} $declare_flag]
        if {$string_symbol != {}} {
            append buffer [emitter_indent] \
                "String " $string_symbol " = " $tmpsymbol ".toString();\n"
        }
    } else {
        # A word that contains multiple elements that should be concatenated together
        set sbtmp [compileproc_tmpvar_next sbtmp]

        if {$declare_flag} {
            append buffer [emitter_indent] \
                $declare $tmpsymbol "\;\n"
        }

        if {$string_symbol != {}} {
            append buffer [emitter_indent] \
                "String " $string_symbol "\;\n"
        }

        append buffer [emitter_indent] \
            "StringBuffer " $sbtmp " = new StringBuffer(64)\;\n"

        foreach wi $winfo {
            append buffer [compileproc_emit_word_element $wi $tmpsymbol true $sbtmp $declare_flag]
        }

        if {$string_symbol != {}} {
            # Assign String value instead of creating TclObject
            append buffer [emitter_indent] \
                $string_symbol " = " $sbtmp ".toString();\n"
        } else {
            # Create new TclObject that contains the new StringBuffer
            append buffer [emitter_indent] \
                $tmpsymbol " = TclString.newInstance(" $sbtmp ")\;\n"
        }
    }

    return $buffer
}

# Emit code to either assign a value to or append to tmpsymbol.
# This code will append to a StringBuffer tmpsymbol if the
# append flag is true. By default, a TclObject local variable
# will be declared. If declare_flag is false, then the variable
# will be assigned but it is up to the caller to declare it
# before hand.

proc compileproc_emit_word_element { winfo_element tmpsymbol append sbtmp {declare_flag 1} } {
#    set debug 0

#    if {$debug} {
#        puts "compileproc_emit_word_element \{$winfo_element\} $tmpsymbol $append $sbtmp $declare_flag"
#    }

    set buffer ""

    if {$declare_flag} {
        set declare "TclObject "
    } else {
        set declare ""
    }

    set type [lindex $winfo_element 0]

    if {$type == "text"} {
        # Emit code to append constant string to word value
        set str [emitter_backslash_tcl_string [lindex $winfo_element 1]]
        if {$append} {
            append buffer [emitter_indent] \
                $sbtmp ".append(\"" $str "\")\;\n"
        } else {
            error "constant text can't be assigned"
        }
    } elseif {$type == "variable"} {
        # Emit variable query code
        set vinfo [lindex $winfo_element 1]
        set decl $declare_flag
        if {$append} {
            set decl 0
        }
        append buffer [compileproc_emit_variable $tmpsymbol $vinfo $decl]
        if {$append} {
            append buffer [emitter_indent] \
                $sbtmp ".append(" $tmpsymbol ".toString())" "\;\n"
        }
    } elseif {$type == "command"} {
        # Emit command evaluation code
        set ckeys [lindex $winfo_element 1]

        if {[compileproc_is_empty_command $ckeys]} {
            # Empty command, emit ref to constant empty Tcl string.
            set esym [compileproc_constant_cache_get {}]
            if {$append} {
                append buffer [emitter_indent] \
                    $sbtmp ".append(" $esym ".toString())" "\;\n"
            } else {
                append buffer [emitter_indent] \
                    $declare $tmpsymbol " = $esym\;\n"
            }
        } else {
            # 1 or more command keys, emit a invocation for
            # each non-empty key.
            foreach ckey $ckeys {
                if {$ckey == {}} {continue} ; # Skip empty command
                append buffer [compileproc_emit_invoke $ckey]
            }
            if {$append} {
                append buffer \
                    [emitter_indent] \
                    $tmpsymbol " = interp.getResult()" "\;\n" \
                    [emitter_indent] \
                    $sbtmp ".append(" $tmpsymbol ".toString())" "\;\n"
            } else {
                append buffer \
                    [emitter_indent] \
                    $declare $tmpsymbol " = interp.getResult()" \
                    "\;\n"
            }
        }
    } else {
        error "unknown word info element type \"$type\" in winfo_element \{$winfo_element\}"
    }

    return $buffer
}

# Reset the counter used to generate temp variable names in the scope
# of a command. We don't want to have to deal with any issues related to
# reuse of variable names in different scoped blocks, so number all tmp
# variables in the cmdProc scope.

proc compileproc_tmpvar_reset {} {
    global _compileproc
    set _compileproc(local_counter) 0
}

# Return the next temp var name

proc compileproc_tmpvar_next { {prefix "tmp"} } {
    global _compileproc
    set vname "${prefix}$_compileproc(local_counter)"
    incr _compileproc(local_counter)
    return $vname
}

# Return true if the key corresponds to a container command.
# This type of command contains other commands and can be
# inlined so that logic tests and looping can be done in Java.

proc compileproc_is_container_command { key } {
    global _compileproc_ckeys
    if {![info exists _compileproc_ckeys($key,info_key)]} {
        return 0
    }
    set cmdname [lindex $_compileproc_ckeys($key,info_key) 1]
    if {$cmdname == "_UNKNOWN"} {
        return 0
    }
    switch -exact -- $cmdname {
        "::catch" -
        "catch" -
        "::expr" -
        "expr" -
        "::for" -
        "for" -
        "::foreach" -
        "foreach" -
        "::if" -
        "if" -
        "::switch" -
        "switch" -
        "::while" -
        "while" {
            return 1
        }
        default {
            return 0
        }
    }
}

# Return true if the key indicates a container command
# that explicitly resets the interp result after running
# contained commands. The compiler can generate optimized
# code in some cases when it is known that the commands
# inside a block do not need to set the interp result
# because the command will always explicitly reset
# the interp result after running commands in the block.

proc compileproc_is_result_reset_container_command { key } {
    global _compileproc_ckeys
    if {![info exists _compileproc_ckeys($key,info_key)]} {
        return 0
    }
    set cmdname [lindex $_compileproc_ckeys($key,info_key) 1]
    if {$cmdname == "_UNKNOWN"} {
        return 0
    }

    switch -exact -- $cmdname {
        "::catch" -
        "catch" {
            # Catch will reset the interp result after
            # invoking the body block, but it will also
            # set the result to a condition code. Return
            # false here since the interp result is not
            # set to null after catch is finidhied. See
            # compileproc_container_catch_with_varname
            # for more about tricky special case handling
            # related to the catch command.

            return 0
        }
        "::expr" -
        "expr" {
            return 0
        }
        "::for" -
        "for" {
            return 1
        }
        "::foreach" -
        "foreach" {
            return 1
        }
        "::if" -
        "if" {
            return 0
        }
        "::switch" -
        "switch" {
            return 0
        }
        "::while" -
        "while" {
            return 1
        }
        default {
            return 0
        }
    }
}

# Return true if the container keys list for a
# given container command is a flat list. Only
# the "catch" and "foreach" commands make use
# of a flat list of container commands.

proc compileproc_is_container_command_keys_flat { key } {
    global _compileproc_ckeys
    if {![info exists _compileproc_ckeys($key,info_key)]} {
        return 0
    }
    set cmdname [lindex $_compileproc_ckeys($key,info_key) 1]
    if {$cmdname == "_UNKNOWN"} {
        return 0
    }

    switch -exact -- $cmdname {
        "::catch" -
        "catch" -
        "::foreach" -
        "foreach" {
            return 1
        }
        default {
            return 0
        }
    }
}

# Return true if this container command can be inlined.
# Typically, a container command that is staticly
# defined and has the correct number of arguments
# can be inlined. For example, a while loop where
# the body code is contained in a braced string and
# the expr test is in a braced string can be inlined.

proc compileproc_can_inline_container { key } {
    global _compileproc _compileproc_ckeys
#    set debug 0

#    if {$debug} {
#        puts "compileproc_can_inline_container $key"
#    }

    if {$_compileproc(options,inline_containers) == {}} {
#        if {$debug} {
#            puts "inline_containers is {}, returning 0"
#        }
        return 0
    }

    # Determine the command name
    if {![info exists _compileproc_ckeys($key,info_key)]} {
#        if {$debug} {
#            puts "no info_key for key $key"
#        }
        return 0
    }
    set qcmdname [lindex $_compileproc_ckeys($key,info_key) 1]
    if {$qcmdname == "_UNKNOWN"} {
        error "container should not have non-constant name"
    }
    set cmdname [namespace tail $qcmdname]
#    if {$debug} {
#        puts "container command name \"$cmdname\" for key $key"
#    }

    # Determine if command can be inlined based on module config.
    set inline 0

    if {$_compileproc(options,inline_containers) == "all"} {
        set inline 1
    }

    if {$inline == 0} {
        set ind [lsearch -exact $_compileproc(options,inline_containers) $cmdname]
        if {$ind == -1} {
#            if {$debug} {
#                puts "container \"$cmdname\" not found in inline_containers list\
#                    \{$_compileproc(options,inline_containers)\}"
#            }
            return 0
        } else {
            set inline 1
        }
    }

    if {$inline == 0} {
        error "expected inline to be 1, not \"$inline\""
    }

    # The command container could be inlined if it passed
    # the container validation test and all the body
    # blocks are static.

    if {[descend_container_is_valid $key] && \
            [descend_container_is_static $key]} {

#        if {$debug} {
#            puts "container is valid and static, cmdname is \"$cmdname\""
#        }

        #if {$cmdname == "foreach"} {
        #    # Don't try to inline foreach with multiple lists
        #    if {![descend_container_foreach_has_single_list $key]} {
        #        return 0
        #    }
        #    # Don't try to inline foreach with multiple vars in varlist
        #    if {![descend_container_foreach_has_single_variable $key]} {
        #        return 0
        #    }
        #}

        # Compile switch command with a string and a patbody list

        #if {$cmdname == "switch"} {
        #    if {![descend_container_switch_has_patbody_list $key]} {
        #        return 0
        #    }
        #}

        return 1
    } else {
#        if {$debug} {
#            puts "container command \"$cmdname\" is not valid or not static"
#            puts "is_valid [descend_container_is_valid $key]"
#            puts "is_static [descend_container_is_static $key]"
#        }
        return 0
    }
}

# Return true if the command identified by key is
# a break, continue, or return command that can
# be inlined. The break and continue commands
# can be inlined inside a loop context. A return
# command can be inlined in a procedure unless
# it is contained inside a catch block. This
# method is invoked as code is emitted.

proc compileproc_can_inline_control { key } {
    global _compileproc
    global _compileproc_ckeys

#    set debug 0

#    if {$debug} {
#        puts "compileproc_can_inline_control $key"
#    }

    if {!$_compileproc(options,inline_controls)} {
#        if {$debug} {
#            puts "inline_controls option is not enabled"
#        }
        return 0
    }
    if {![info exists _compileproc_ckeys($key,info_key)]} {
        return 0
    }
    set cmdname [lindex $_compileproc_ckeys($key,info_key) 1]
    if {$cmdname == "_UNKNOWN"} {
        return 0
    }
    switch -exact -- $cmdname {
        "::break" -
        "break" {
            set is_break 1
            set is_continue 0
            set is_return 0
        }
        "::continue" -
        "continue" {
            set is_break 0
            set is_continue 1
            set is_return 0
        }
        "::return" -
        "return" {
            set is_break 0
            set is_continue 0
            set is_return 1
        }
        default {
#            if {$debug} {
#                puts "command \"$cmdname\" is not a break, continue, or return command"
#            }
            return 0
        }
    }

    # A break or continue command must have a single argument.
    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$is_break || $is_continue} {
        if {$num_args != 1} {
#            if {$debug} {
#                puts "invalid num_args $num_args to break or continue command"
#            }
            return 0
        }
    } elseif {$is_return} {
        # Let runtime handle a return with special options.
        if {$num_args != 1 && $num_args != 2} {
#            if {$debug} {
#                puts "unhandled return with $num_args arguments"
#            }
            return 0
        }
    }

    # The command is break, continue, or return and the
    # inline controls flag is enabled. Check the current
    # controls stack tuple to determine if the command
    # can be inlined in this control context.

    set stack $_compileproc(options,controls_stack)
    if {[llength $stack] == 0} {
#        if {$debug} {
#            puts "empty controls_stack, can't inline break/continue"
#        }
        return 0
    }
    set tuple [lindex $stack 0]
    if {[llength $tuple] != 7 \
            || [lindex $tuple 1] != "break" \
            || [lindex $tuple 3] != "continue" \
            || [lindex $tuple 5] != "return"} {
        error "expected controls_stack top tuple\
            {type break 0|1 continue 0|1 return 0|1} but got \{$tuple\}"
    }
#    if {$debug} {
#        puts "checking command $cmdname : controls context is \{$tuple\}"
#    }
    array set stack_map [lrange $tuple 1 end]
    if {$is_break && $stack_map(break)} {
        # break can be inlined
#        if {$debug} {
#            puts "can inline break command"
#        }
        return 1
    }
    if {$is_continue && $stack_map(continue)} {
        # continue can be inlined
#        if {$debug} {
#            puts "can inline continue command"
#        }
        return 1
    }
    if {$is_return && $stack_map(return)} {
        # return can be inlined
#        if {$debug} {
#            puts "can inline return command"
#        }
        return 1
    }

#    if {$debug} {
#        puts "can't inline break/continue/return command, not valid in context [lindex $tuple 0]"
#    }
    return 0
}

# Return true if the key corresponds to a built-in Tcl command
# that can be inlined. Container commands are handled elsewhere,
# inlined commands are single commands like "set" that can
# be replaced with optimized code.

proc compileproc_can_inline_command { key } {
    global _compileproc
    global _compileproc_ckeys

#    set debug 0

#    if {$debug} {
#        puts "compileproc_can_inline_command $key"
#    }

    if {!$_compileproc(options,inline_commands)} {
        return 0
    }

    if {![info exists _compileproc_ckeys($key,info_key)]} {
        return 0
    }
    set cmdname [lindex $_compileproc_ckeys($key,info_key) 1]
    if {$cmdname == "_UNKNOWN"} {
        return 0
    }

    # See if commands is one of the built-in Tcl
    # commands that we know how to inline. Note
    # that we will not support an inline of the
    # following commands:
    #
    # variable, upvar, uplevel

    switch -exact -- $cmdname {
        "::append" -
        "append" {
            return [compileproc_can_inline_command_append $key]
        }
        "::global" -
        "global" {
            return [compileproc_can_inline_command_global $key]
        }
        "::incr" -
        "incr" {
            return [compileproc_can_inline_command_incr $key]
        }
        "::lappend" -
        "lappend" {
            return [compileproc_can_inline_command_lappend $key]
        }
        "::lindex" -
        "lindex" {
            return [compileproc_can_inline_command_lindex $key]
        }
        "::list" -
        "list" {
            return [compileproc_can_inline_command_list $key]
        }
        "::llength" -
        "llength" {
            return [compileproc_can_inline_command_llength $key]
        }
        "::set" -
        "set" {
            return [compileproc_can_inline_command_set $key]
        }
        "::string" -
        "string" {
            return [compileproc_can_inline_command_string $key]
        }
        default {
            return 0
        }
    }
}

# Return code to inline a specific built-in Tcl command.

proc compileproc_emit_inline_command { key } {
    global _compileproc
    global _compileproc_ckeys

    if {!$_compileproc(options,inline_commands)} {
        error "compileproc_emit_inline_command invoked when inline_commands option is false"
    }
    set cmdname [lindex $_compileproc_ckeys($key,info_key) 1]
    # convert input like "::set" to "set" to simplify switch
    set cmdname [namespace tail $cmdname]

    # invoke specific method to emit command code

    switch -exact -- $cmdname {
        "append" {
            return [compileproc_emit_inline_command_append $key]
        }
        "global" {
            return [compileproc_emit_inline_command_global $key]
        }
        "incr" {
            return [compileproc_emit_inline_command_incr $key]
        }
        "lappend" {
            return [compileproc_emit_inline_command_lappend $key]
        }
        "lindex" {
            return [compileproc_emit_inline_command_lindex $key]
        }
        "list" {
            return [compileproc_emit_inline_command_list $key]
        }
        "llength" {
            return [compileproc_emit_inline_command_llength $key]
        }
        "set" {
            return [compileproc_emit_inline_command_set $key]
        }
        "string" {
            return [compileproc_emit_inline_command_string $key]
        }
        default {
            error "unsupported inlined command \"$cmdname\""
        }
    }
}

# Invoke to emit code for a specific type of container
# command.

proc compileproc_emit_container { key } {
    global _compileproc_ckeys

    if {![info exists _compileproc_ckeys($key,info_key)]} {
        error "expected _compileproc_ckeys entry for container $key"
    }
    set cmdname [lindex $_compileproc_ckeys($key,info_key) 1]
    if {$cmdname == "_UNKNOWN"} {
        error "expected command name for $key"
    }

    # If command name has a namespace qualifier, just use
    # the tail part.
    set tail [namespace tail $cmdname]

    switch -exact -- $tail {
        "catch" {
            return [compileproc_emit_container_catch $key]
        }
        "expr" {
            return [compileproc_emit_container_expr $key]
        }
        "for" {
            return [compileproc_emit_container_for $key]
        }
        "foreach" {
            return [compileproc_emit_container_foreach $key]
        }
        "if" {
            return [compileproc_emit_container_if $key]
        }
        "switch" {
            return [compileproc_emit_container_switch $key]
        }
        "while" {
            return [compileproc_emit_container_while $key]
        }
        default {
            error "expected a supported container command, got \"$tail\""
        }
    }
}

# Emit code for an inlined if container.

proc compileproc_emit_container_if { key } {
    set buffer ""

    if {![descend_container_is_valid $key] || \
            ![descend_container_is_static $key]} {
        error "NOT valid or NOT static for key $key"
    }

    set ccmds [descend_commands $key container]
    if {[llength $ccmds] < 3} {
        error "bad num container commands \{$ccmds\}"
    }

    set expr_keys [lindex $ccmds 0]
    set true_keys [lindex $ccmds 1]

    if {[llength $ccmds] > 3} {
        set elseif_keys [lrange $ccmds 2 end-1]
        if {([llength $elseif_keys] % 2) != 0} {
            error "expected even number of elseif keys, got \{$elseif_keys\}"
        }
    } else {
        set elseif_keys {}
    }
    set else_keys [lindex $ccmds end]

    # Evaluate if {expr} expression
    set expr_index [lindex [descend_container_if_expr_body $key] 0]

    set tuple [compileproc_expr_evaluate_boolean_emit $key $expr_index]
    set tmpsymbol [lindex $tuple 0]
    append buffer [lindex $tuple 1]
    set is_constant [lindex $tuple 2]
    append buffer [emitter_container_if_start $tmpsymbol]

    set omit_result [compileproc_omit_set_result $key]

    # true block
    if {$true_keys == {}} {
        # If result of if is used, reset interp result
        if {!$omit_result} {
            append buffer [emitter_reset_result]
        }
    } else {
        foreach true_key $true_keys {
            append buffer [compileproc_emit_invoke $true_key]
        }
    }

    # elseif blocks
    set elseif_depth 0
    if {$elseif_keys != {}} {
        foreach {elseif_expr_keys elseif_body_keys} $elseif_keys \
                {expr_index body_index} [descend_container_if_iterate_elseif_body $key] {

            set tuple [compileproc_expr_evaluate_boolean_emit $key $expr_index]
            set tmpsymbol [lindex $tuple 0]
            set is_constant [lindex $tuple 2]
            if {$is_constant} {
                # A constant has no evaluation code, so it can be
                # placed directly in an if/elseif expression.
                append buffer \
                    [lindex $tuple 1] \
                    [emitter_container_if_else_if $tmpsymbol]
            } else {
                # The expression needs to be evaluated with multiple
                # statements. Use multiple if/else blocks to simulate
                # the if/elseif/else Tcl command.

                append buffer \
                    [emitter_container_if_else] \
                    [lindex $tuple 1] \
                    [emitter_container_if_start $tmpsymbol]
                incr elseif_depth
            }

            if {$elseif_body_keys == {}} {
                # If result of if is used, reset interp result
                if {!$omit_result} {
                    append buffer [emitter_reset_result]
                }
            } else {
                foreach elseif_body_key $elseif_body_keys {
                    append buffer [compileproc_emit_invoke $elseif_body_key]
                }
            }
        }
    }

    # else block will need to reset the interp result
    # when no else commands are found so that the
    # result is set properly when no if block is run.
    # There is no need to worry about this if the result
    # of this command is not used and we are omitting results.

    if {$else_keys == {}} {
        # If result of if is used, reset interp result in else block
        if {!$omit_result} {
            append buffer [emitter_container_if_else]
            append buffer [emitter_reset_result]
        }
    } else {
        append buffer [emitter_container_if_else]

        foreach else_key $else_keys {
            append buffer [compileproc_emit_invoke $else_key]
        }
    }

    # Finish up if/else blocks
    for {set i 0} {$i < $elseif_depth} {incr i} {
        append buffer [emitter_container_if_end]
    }

    append buffer [emitter_container_if_end]

    return $buffer
}

# This method is invoked when a container command that
# has a boolean expr block wants to evaluate the expr
# as a boolean value. A tuple of {tmpsymbol buffer is_constant}
# is returned by this method.

proc compileproc_expr_evaluate_boolean_emit { key expr_index } {
    global _compileproc_key_info
    global _compileproc

#    set debug 0

    set buffer ""

    set expr_result [compileproc_expr_evaluate $key $expr_index]
    set type [lindex $expr_result 0]

    # Determine if the expression has a constant boolean value
    switch -exact -- $type {
        {constant} -
        {constant boolean} -
        {constant string} {
            set value [lindex $expr_result 1]
#            if {$debug} {
#                puts "got constant value \"$value\""
#            }
            # Convert constant value to a constant boolean.
            if {$type == {constant string}} {
                set ovalue "\"$value\""
            } else {
                set ovalue $value
            }
            if {[catch {expr "!!$ovalue"} result]} {
                # Not a constant boolean expression
#                if {$debug} {
#                    puts "expr \{!!$ovalue\} returned: $err"
#                }
            } else {
                if {$result} {
                    set simple_bool_value true
                } else {
                    set simple_bool_value false
                }
                # Constant value expression
                return [list $simple_bool_value $buffer 1]
            }
        }
    }

    # Determine if the expression is a plain scalar variable
    # that can be handled with optimized code.

    if {$type == {variable scalar}} {
        set sname [lindex $expr_result 1]
        set vinfo [list scalar $sname]
        set tmpsymbol1 [compileproc_tmpvar_next]
        append buffer \
            [compileproc_emit_variable $tmpsymbol1 $vinfo]
        set tmpsymbol2 [compileproc_tmpvar_next]
        append buffer [emitter_indent] \
            "boolean " $tmpsymbol2 " = " \
            [emitter_tclobject_to_boolean $tmpsymbol1] \
            "\;\n"
        set retsymbol $tmpsymbol2
        return [list $tmpsymbol2 $buffer 0]
    }

    # Otherwise, the expression is non-trivial so handle
    # it with the expr command. In some testing usage,
    # we want to disable expr inlining when compiling
    # commands like "if". In the normal case, a expr
    # inside a command is inlined.

    set inline 0
    if {$_compileproc(options,inline_containers) == "all"} {
        set inline 1
    }
    if {$inline == 0} {
        set ind [lsearch -exact $_compileproc(options,inline_containers) \
            "expr"]
        if {$ind != -1} {
            set inline 1
        }
    }
    #set inline 0

    if {!$inline} {
        # Invoke expr command with a constant string
        # as the expression.
        set values $_compileproc_key_info($key,values)
        set expr_str [lindex $values $expr_index]

        # Generate a fake key that invokes the
        # expr method with the passed in argument
        # string. This only works for constant
        # strings right now.

        set gkey [compileproc_generate_key expr \
            [list $expr_str] [list "\{$expr_str\}"]]
        append buffer [compileproc_emit_invoke $gkey]
        set tmpsymbol1 [compileproc_tmpvar_next]
        append buffer [emitter_indent] \
            "TclObject " $tmpsymbol1 " = interp.getResult()" "\;\n"

        set tmpsymbol2 [compileproc_tmpvar_next]
        append buffer [emitter_indent] \
            "boolean " $tmpsymbol2 " = " \
        [emitter_tclobject_to_boolean $tmpsymbol1] "\;\n"

        return [list $tmpsymbol2 $buffer 0]
    } else {
        # Generate expr evaluation buffer and then
        # determine a boolean value for the expression.

        set eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
            $expr_result]
        set infostr [lindex $eval_tuple 0]
        set ev [lindex $eval_tuple 1]
        append buffer [lindex $eval_tuple 2]
        set ev_types [lindex $eval_tuple 3 3]

        set tmpsymbol [compileproc_tmpvar_next]
        append buffer \
            [emitter_indent] \
            "boolean " $tmpsymbol " = "

        # If ExprValue result is known at compile time,
        # then invoke optimized logic to convert this
        # type to a boolean. This optimization avoids
        # a method invocation and a switch, it can
        # make the code execute 4x faster in some cases.

        if {$_compileproc(options,expr_inline_set_result)} {
            switch -- $ev_types {
                {boolean} -
                {int} {
                    append buffer \
                        "( " $ev ".getIntValue() != 0 )"
                }
                {double} {
                    append buffer \
                        "( " $ev ".getDoubleValue() != 0.0 )"
                }
                default {
                    append buffer \
                        $ev ".getBooleanValue(interp)"
                }
            }
        } else {
            append buffer \
                $ev ".getBooleanValue(interp)"
        }

        append buffer \
            "\;\n" \
            [compileproc_emit_exprvalue_release $ev]

        return [list $tmpsymbol $buffer 0]
    }
}

# Evaluate an expression and return the parse tree
# for the expression. This method is used by a
# compile expr command and container commands
# like if and while.

proc compileproc_expr_evaluate { key expr_index } {
    global _compileproc _compileproc_key_info

#    set debug 0

#    if {$debug} {
#        puts "compileproc_expr_evaluate $key $expr_index"
#    }

    set types $_compileproc_key_info($key,types)
    set values $_compileproc_key_info($key,values)

    if {[lindex $types $expr_index] != "constant"} {
        error "expected constant argument type at expr index $expr_index of \{$types\}"
    }

    set expr_str [lindex $values $expr_index]
    set simple_bool 0
    # Use string compare here so the "+1" is not
    # seen as a constant boolean.
    if {$expr_str eq "1" || $expr_str eq "true"} {
        set simple_bool 1
        set simple_bool_value "true"
    } elseif {$expr_str eq "0" || $expr_str eq "false"} {
        set simple_bool 1
        set simple_bool_value "false"
    }

    if {$simple_bool} {
#        if {$debug} {
#            puts "compileproc_expr_evaluate returning constant bool $simple_bool_value"
#        }
        return [list {constant boolean} $simple_bool_value $expr_str]
    }

#    if {$debug} {
#        puts "compileproc_expr_evaluate: a non-constant-boolean expr will be iterated"
#    }

    # FIXME: Is there any way we could save the etree created during the descend
    # parse so we don't need to do this all over again. Also, if we parsed the
    # expr once in the descend module and it turned out to be a simple type
    # like a scalar variable, could we save that info an use it here?

    set script [descend_get_data $key script]
    set tree [descend_get_data $key tree]
    set stree [lindex $tree $expr_index]
    set range [parse_get_simple_text_range $stree text]
    set etree [parse expr $script $range]

#    if {$debug} {
#        puts "parsed etree: $etree"
#    }

    # When iterating over an expr command or a command
    # that contains an implicit expr, we need to map
    # command operators to command keys. This is
    # implemented using the childkey API. An expr
    # that is compiled will never have regular
    # nested commands, so there is no danger of
    # the childkey API being used for different
    # things here. Note that we need to treat
    # the "expr" command itself with special care
    # since its container commands data applies to
    # the whole expression.
    set ccmds [descend_commands $key container]
#    if {$debug} {
#        puts "container commands for key $key are \{$ccmds\}"
#    }
    set tuple [compileproc_expr_container_index $key $expr_index]
    if {[lindex $tuple 0] == "expr"} {
        set argument_ccmds $ccmds
#        if {$debug} {
#            puts "expr command found, argument_ccmds is \{$argument_ccmds\}"
#        }
    } else {
        set container_index [lindex $tuple 1]
        set argument_ccmds [lindex $ccmds $container_index]
#        if {$debug} {
#            puts "[lindex $tuple 0] command with expr found"
#            puts "argument $expr_index maps to container index $container_index"
#            puts "argument_ccmds is \{$argument_ccmds\}"
#        }
    }
    compileproc_childkey_reset $key $argument_ccmds

    # Save values (needed for recursive safety), then
    # iterate and restore saved values.

    set saved_expr_eval_key $_compileproc(expr_eval_key)
    set saved_expr_eval_buffer $_compileproc(expr_eval_buffer)
    set saved_expr_eval_expressions $_compileproc(expr_eval_expressions)

    set _compileproc(expr_eval_key) $key
    set _compileproc(expr_eval_buffer) {}
    set _compileproc(expr_eval_expressions) 0

    set type_value [parse_expr_iterate $script $etree \
        compileproc_expr_evaluate_callback]

    set expr_eval_buffer $_compileproc(expr_eval_buffer)
    set expr_eval_expressions $_compileproc(expr_eval_expressions)

    set _compileproc(expr_eval_key) $saved_expr_eval_key
    set _compileproc(expr_eval_buffer) $saved_expr_eval_buffer
    set _compileproc(expr_eval_expressions) $saved_expr_eval_expressions

    # Check that each container command was processed.
#    if {$debug} {
#        puts "calling compileproc_childkey_validate for $key with argument_ccmds \{$argument_ccmds\}"
#    }
    compileproc_childkey_validate $key

    # Examine results of expr iteration

    set type [lindex $type_value 0]
    set value [lindex $type_value 1]

#    if {$debug} {
#        puts "type value is: $type_value"
#        puts "num expressions is $_compileproc(expr_eval_expressions)"
#        if {$_compileproc(expr_eval_buffer) != {}} {
#        puts "eval buffer is:\n$_compileproc(expr_eval_buffer)"
#        }
#    }

    return $type_value
}

# Map expr argument index to an index into the
# container command list. Every container command
# except the "if" command has a trivial mapping.
# This method returns the name of the command and
# the index into the container command list that
# the expr index would be found at. For the "expr"
# command, the index -1 is returned since there
# is no mapping.

proc compileproc_expr_container_index { key in_expr_index } {
    global _compileproc_ckeys

#    set debug 0

#    if {$debug} {
#    puts "compileproc_expr_container_index $key $in_expr_index"
#    }

    if {$in_expr_index == {}} {
        error "empty in_expr_index argument"
    }

    set cmdname [lindex $_compileproc_ckeys($key,info_key) 1]

    if {$cmdname == "if"} {
        set expr_indexes [list]
        # if expr body block
        set tuple [descend_container_if_expr_body $key]
        set expr_index [lindex $tuple 0]
        lappend expr_indexes $expr_index {}
        # else/if blocks
        if {[descend_container_if_has_elseif_body $key]} {
            foreach {expr_index body_index} \
                    [descend_container_if_iterate_elseif_body $key] {
                lappend expr_indexes $expr_index {}
            }
        }
        # else block -> No-op

        # Find index in expr_indexes for in_expr_index
#        if {$debug} {
#            puts "searching list \{$expr_indexes\} for $in_expr_index"
#        }
        set ind [lsearch -exact $expr_indexes $in_expr_index]
        if {$ind == -1} {
            error "if: failed to find expr index $in_expr_index\
                in \{$expr_indexes\}"
        }
        return [list $cmdname $ind]
    } elseif {$cmdname == "expr"} {
        return [list $cmdname -1]
    }

    # Command other than if have a trivial mapping
    return [list $cmdname [expr {$in_expr_index - 1}]]
}

# Invoked as expr is iterated over.

proc compileproc_expr_evaluate_callback { script etree type values ranges } {
    global _compileproc

#    set debug 0

    # Init
    #set _compileproc(expr_eval_key) $key
    #set _compileproc(expr_eval_buffer) {}
    #set _compileproc(expr_eval_expressions) 0

#    if {$debug} {
#        puts "compileproc_expr_evaluate_callback:\
#            \{$type\} \{$values\} \{$ranges\}"
#    }

    switch -exact -- $type {
        {literal operand} {
            set type [lindex $values 0 0]
            set literal [lindex $values 0 1]

            # The literals 1 and 1.0 are handled differently
            # than the literal strings "1" and {1}.
            if {$type == {text}} {
                parse_expr_iterate_type_value constant $literal
            } elseif {$type == {string}} {
                parse_expr_iterate_type_value {constant string} $literal
            } elseif {$type == {braced string}} {
                # A brace quoted operand like {foo\nbar} must be written
                # as the Java String "foo\\nbar". Find all backslash
                # chars and double backslash them.
                if {[string first "\\" $literal] != -1} {
                    set dbs_str ""
                    set len [string length $literal]
                    for {set i 0} {$i < $len} {incr i} {
                        set c [string index $literal $i]
                        if {$c == "\\"} {
                            append dbs_str "\\\\"
                        } else {
                            append dbs_str $c
                        }
                    }
                    set literal $dbs_str
                }
                parse_expr_iterate_type_value {constant braced string}\
                    $literal
            } else {
                error "unhandled literal operand type \"$type\""
            }
        }
        {command operand} {
            # Descend into container commands inside the expr.
            set key $_compileproc(expr_eval_key)
            set ccmds [compileproc_childkey_next $key]
            parse_expr_iterate_type_value {command operand} $ccmds
        }
        {variable operand} {
            # Check variable element. Treat a scalar variable as a special
            # case since it can be looked up as a value.

            if {[parse_is_scalar_variable $etree]} {
                set scalar [parse_get_scalar_variable $script $etree]
                parse_expr_iterate_type_value {variable scalar} $scalar
            } else {
                set key $_compileproc(expr_eval_key)
                set script [descend_get_data $key script]
                set vinfo [compileproc_scan_variable $key $script $etree]
                parse_expr_iterate_type_value {variable array} $vinfo
            }
        }
        {word operand} {
            # Scan word elements looking for nested commands,
            # variables, and backslash escapes. If the word
            # contained only backslashes, then convert to
            # a constant double quoted string operand.

            set key $_compileproc(expr_eval_key)

            set wstree $etree
            set type_winfo [compileproc_scan_word $key $script $wstree]

            set type [lindex $type_winfo 0]
            set value [lindex $type_winfo 1]
            set cmap [lindex $type_winfo 2]

#            if {$debug} {
#            puts "compileproc_scan_word returned type/value/cmap: $type \{$value\} \{$cmap\}"
#            }

            if {$type == "constant"} {
                parse_expr_iterate_type_value {constant string} $value
            } else {
                # Don't have a types/values/cmap record for expr
                # operands like we do for command arguments, so
                # just pass them along as the value list.
                set value [list $type $values $cmap]
                parse_expr_iterate_type_value {word operand} $type_winfo
            }
        }
        {unary operator} {
            incr _compileproc(expr_eval_expressions)
            parse_expr_iterate_type_value {unary operator} $values
        }
        {binary operator} {
            incr _compileproc(expr_eval_expressions)
            parse_expr_iterate_type_value {binary operator} $values
        }
        {ternary operator} {
            incr _compileproc(expr_eval_expressions)
            parse_expr_iterate_type_value {ternary operator} $values
        }
        {math function} {
            incr _compileproc(expr_eval_expressions)
            parse_expr_iterate_type_value {math function} $values
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

# Invoked to query module file flags and set flags in the compileproc module
# that depend on these flags. This method is invoked dynamically inside
# compileproc_compile after compileproc_init has been invoked so that
# the compileproc module can be tested without depending on other modules.

proc compileproc_query_module_flags { proc_name } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_query_module_flags $proc_name"
#    }

    # If +inline-containers if set for this proc of for the whole module, then
    # enable the flag in this module.
    set inline_containers_option [module_option_value inline-containers $proc_name]
    if {$inline_containers_option == {}} {
        set inline_containers_option [module_option_value inline-containers]
    }
    if {$inline_containers_option} {
        set _compileproc(options,inline_containers) all
    } else {
        # Expect option to be set to {}
        if {$_compileproc(options,inline_containers) != {}} {
            error "inline_containers option set to \
                \"$_compileproc(options,inline_containers)\", expected empty string"
        }
    }

    set inline_controls_option \
        [module_option_value inline-controls $proc_name]
    if {$inline_controls_option == {}} {
        set inline_controls_option \
            [module_option_value inline-controls]
    }
    if {$inline_controls_option} {
        set _compileproc(options,inline_controls) 1
    }

    set cache_commands_option \
        [module_option_value cache-commands $proc_name]
    if {$cache_commands_option == {}} {
        set cache_commands_option \
            [module_option_value cache-commands]
    }
    if {$cache_commands_option} {
        set _compileproc(options,cache_commands) 1
    }

    set constant_increment_option \
        [module_option_value constant-increment $proc_name]
    if {$constant_increment_option == {}} {
        set constant_increment_option \
            [module_option_value constant-increment]
    }
    if {$constant_increment_option == 0} {
        set _compileproc(options,skip_constant_increment) 1
    }

    set cache_variables_option \
        [module_option_value cache-variables $proc_name]
    if {$cache_variables_option == {}} {
        set cache_variables_option \
            [module_option_value cache-variables]
    }
    if {$cache_variables_option} {
        set _compileproc(options,cache_variables) 1
    }

    set inline_commands_option \
        [module_option_value inline-commands $proc_name]
    if {$inline_commands_option == {}} {
        set inline_commands_option \
            [module_option_value inline-commands]
    }
    if {$inline_commands_option} {
        set _compileproc(options,inline_commands) 1
    }

    set omit_results_option \
        [module_option_value omit-results $proc_name]
    if {$omit_results_option == {}} {
        set omit_results_option \
            [module_option_value omit-results]
    }
    if {$omit_results_option} {
        set _compileproc(options,omit_results) 1
    }

    set inline_expr_option \
        [module_option_value inline-expr $proc_name]
    if {$inline_expr_option == {}} {
        set inline_expr_option \
            [module_option_value inline-expr]
    }
    if {$inline_expr_option} {
        set _compileproc(options,expr_inline_operators) 1

        # The -inline-expr-value-stack and
        # +inline-expr-value-stack-null options
        # can be used to disable the stack expr
        # value feature and enable debug nulling
        # of stack values.

        set inline_expr_value_stack_option \
            [module_option_value inline-expr-value-stack $proc_name]
        if {$inline_expr_value_stack_option == {}} {
            set inline_expr_value_stack_option \
                [module_option_value inline-expr-value-stack]
        }
        set _compileproc(options,expr_value_stack) $inline_expr_value_stack_option

        set inline_expr_value_stack_null_option \
            [module_option_value inline-expr-value-stack-null $proc_name]
        if {$inline_expr_value_stack_null_option == {}} {
            set inline_expr_value_stack_null_option \
                [module_option_value inline-expr-value-stack-null]
        }
        set _compileproc(options,expr_value_stack_null) $inline_expr_value_stack_null_option

        set _compileproc(options,expr_inline_set_result) 1
    }

#    if {$debug} {
#        puts "compileproc module options set to:"
#        puts "inline_containers = $_compileproc(options,inline_containers)"
#        puts "inline_controls = $_compileproc(options,inline_controls)"
#        puts "cache_commands = $_compileproc(options,cache_commands)"
#        puts "skip_constant_increment = $_compileproc(options,skip_constant_increment)"
#        puts "cache_variables = $_compileproc(options,cache_variables)"
#        puts "inline_commands = $_compileproc(options,inline_commands)"
#        puts "omit_results = $_compileproc(options,omit_results)"

#        puts "expr_inline_operators = $_compileproc(options,expr_inline_operators)"
#        puts "expr_value_stack = $_compileproc(options,expr_value_stack)"
#        puts "expr_value_stack_null = $_compileproc(options,expr_value_stack_null)"
#        puts "expr_inline_set_result = $_compileproc(options,expr_inline_set_result)"
#    }
}

# Generate fake key that we can pass to compileproc_emit_invoke
# so that a named method with a constant argument type will
# be emitted. The argl list is a list of constant strings.
# The inargl list is a list of instrs (original quoted arguments)
# for the key

proc compileproc_generate_key { cmdname argl inargl } {
    global _compileproc_key_info

    set num_args [llength $argl]
    incr num_args 1

    if {![info exists _compileproc_key_info(generated)]} {
        set _compileproc_key_info(generated) 0
    }
    set key "gkey$_compileproc_key_info(generated)"
    incr _compileproc_key_info(generated)

    set types [list]
    for {set i 0} {$i < $num_args} {incr i} {
        lappend types constant
    }
    set values [list]
    set instrs [list]

    lappend values $cmdname
    lappend instrs $cmdname
    for {set i 0} {$i < ($num_args - 1)} {incr i} {
        lappend values [lindex $argl $i]
        lappend instrs [lindex $inargl $i]
    }

    foreach str $values {
        compileproc_constant_cache_add $str
    }

    set _compileproc_key_info($key,types) $types
    set _compileproc_key_info($key,values) $values
    set _compileproc_key_info($key,instrs) $instrs
    set _compileproc_key_info($key,num_args) $num_args

    return $key
}

# Emit code for an inlined while container.

proc compileproc_emit_container_while { key } {
    global _compileproc

    set buffer ""

    if {![descend_container_is_valid $key] || \
            ![descend_container_is_static $key]} {
        error "NOT valid or NOT static for key $key"
    }

    # Container commands for a while block is
    # a list of expr keys and a list of body keys.

    set ccmds [descend_commands $key container]
    if {[llength $ccmds] != 2} {
        error "bad num container commands \{$ccmds\} for key $key"
    }

    set expr_keys [lindex $ccmds 0]
    set body_keys [lindex $ccmds 1]

    # Evaluate while {expr} expression
    set expr_index [descend_container_while_expr $key]

    # Evaluate expression: Note that an inlined expr
    # could contain break or continue commands that
    # should not be inlined.

    # Push loop break/continue context
    if {$_compileproc(options,inline_controls)} {
        compileproc_push_controls_context while 0 0
    }

    emitter_indent_level +1 ; # Indent expr evaluation code properly
    set tuple [compileproc_expr_evaluate_boolean_emit $key $expr_index]
    emitter_indent_level -1

    # Pop loop break/continue context
    if {$_compileproc(options,inline_controls)} {
        compileproc_pop_controls_context while
    }

    set tmpsymbol [lindex $tuple 0]
    set tmpbuffer [lindex $tuple 1]
    set is_constant [lindex $tuple 2]

    set is_constant_loopvar 1
    if {!$is_constant} {
        # A non-constant expression is evaluated inside the loop
        set is_constant_loopvar 1
    } else {
        if {$tmpsymbol == "false"} {
            # The constant expression "false" must have a non-constant
            # loop expression to avoid a compile time error.
            set is_constant_loopvar 0
        } elseif {$tmpsymbol == "true" && $body_keys == {}} {
            # The constant expression "true" must have a non-constant
            # value when there are no commands in the loop so as to
            # avoid a compile time error.
            set is_constant_loopvar 0
        } elseif {$tmpsymbol == "true" && $body_keys != {}} {
            set is_constant_loopvar 1
        }
    }

    if {$is_constant_loopvar} {
        append buffer [emitter_container_for_start {} true]
        if {!$is_constant} {
            # evaluate expr, break out of loop if false
            append buffer \
                $tmpbuffer \
                [emitter_container_for_expr $tmpsymbol]
        }
    } else {
        set bval $tmpsymbol
        set tmpsymbol [compileproc_tmpvar_next]
        set init_buffer "boolean $tmpsymbol = $bval"
        append buffer [emitter_container_for_start $init_buffer $tmpsymbol]
    }

    # body block
    if {$body_keys != {}} {
        if {!$is_constant} {
            append buffer "\n"
        }

        # Setup try/catch for break/continue/return
        append buffer \
            [emitter_container_for_try_start] \
            [emitter_container_fake_tclexception]

        # Push loop break/continue/return context
        if {$_compileproc(options,inline_controls)} {
            compileproc_push_controls_context while 1 1
        }

        foreach body_key $body_keys {
            append buffer [compileproc_emit_invoke $body_key]
        }

        # Pop loop break/continue/return context
        if {$_compileproc(options,inline_controls)} {
            compileproc_pop_controls_context while
        }

        # Finish try/catch for break and continue exceptions
        append buffer [emitter_container_for_try_end]
    }

    # Close loop block and call interp.resetResult()
    # if the result of this command is used.

    append buffer \
        [emitter_container_for_end [compileproc_omit_set_result $key]]

    return $buffer
}

# Emit code for an inlined for container.

proc compileproc_emit_container_for { key } {
    global _compileproc

    set buffer ""

    if {![descend_container_is_valid $key] || \
            ![descend_container_is_static $key]} {
        error "NOT valid or NOT static for key $key"
    }

    set ccmds [descend_commands $key container]
    if {[llength $ccmds] != 4} {
        error "bad num container commands \{$ccmds\} for key $key"
    }

    set start_keys [lindex $ccmds 0]
    set expr_keys [lindex $ccmds 1]
    set next_keys [lindex $ccmds 2]
    set body_keys [lindex $ccmds 3]

    set start_index [descend_container_for_start $key]
    set expr_index [descend_container_for_expr $key]
    set next_index [descend_container_for_next $key]
    set body_index [descend_container_for_body $key]

    # start block executes before the loop.

    if {$start_keys != {}} {
        foreach start_key $start_keys {
            append buffer [compileproc_emit_invoke $start_key]
        }
    }

    # Evaluate expression: Note that an inlined expr
    # could contain break or continue commands that
    # should not be inlined.

    # Push loop break context
    if {$_compileproc(options,inline_controls)} {
        compileproc_push_controls_context for 0 0
    }

    emitter_indent_level +1 ; # Indent expr evaluation code properly
    set tuple [compileproc_expr_evaluate_boolean_emit $key $expr_index]
    emitter_indent_level -1

    # Pop loop break context
    if {$_compileproc(options,inline_controls)} {
        compileproc_pop_controls_context for
    }

    set tmpsymbol [lindex $tuple 0]
    set tmpbuffer [lindex $tuple 1]
    set is_constant [lindex $tuple 2]

    set is_constant_loopvar 1
    if {!$is_constant} {
        # A non-constant expression is evaluated inside the loop
        set is_constant_loopvar 1
    } else {
        if {$tmpsymbol == "false"} {
            # The constant expression "false" must have a non-constant
            # loop expression to avoid a compile time error.
            set is_constant_loopvar 0
        } elseif {$tmpsymbol == "true" && $body_keys == {} && $next_keys == {}} {
            # The constant expression "true" must have a non-constant
            # value when there are no commands in the loop so as to
            # avoid a compile time error.
            set is_constant_loopvar 0
        } elseif {$tmpsymbol == "true" && ($body_keys != {} || $next_keys != {})} {
            set is_constant_loopvar 1
        }
    }

    # next block may require a special skip flag local
    if {$next_keys != {}} {
        set skip_tmpsymbol [compileproc_tmpvar_next skip]
        set skip_init "$skip_tmpsymbol = true"
        set skip_decl "boolean $skip_init"
    } else {
        set skip_tmpsymbol ""
    }

    if {$is_constant_loopvar} {
        set init_buffer ""
        if {$skip_tmpsymbol != ""} {
            set init_buffer ${skip_decl}
        }
        append buffer [emitter_container_for_start $init_buffer true]
    } else {
        # Non-constant loopvar
        set bval $tmpsymbol
        set tmpsymbol [compileproc_tmpvar_next]
        set init_buffer "boolean $tmpsymbol = $bval"

        if {$skip_tmpsymbol != ""} {
            append init_buffer ", $skip_init"
        }
        append buffer [emitter_container_for_start $init_buffer $tmpsymbol]
    }

    # next block executes before the body code inside the loop and
    # is skipped in the first iteration.

    # next block
    if {$next_keys != {}} {
        # check skip local
        append buffer [emitter_container_for_skip_start $skip_tmpsymbol]

        # Setup try/catch for break exception
        append buffer \
            [emitter_container_for_try_start] \
            [emitter_container_fake_tclexception]

        # Push loop break context
        if {$_compileproc(options,inline_controls)} {
            compileproc_push_controls_context for 1 0
        }

        foreach next_key $next_keys {
            append buffer [compileproc_emit_invoke $next_key]
        }

        # Pop loop break context
        if {$_compileproc(options,inline_controls)} {
            compileproc_pop_controls_context for
        }

        # Finish try/catch for break
        append buffer \
            [emitter_container_for_try_end 0] \
            [emitter_container_for_skip_end] \
            "\n"
    }

    if {!$is_constant} {
        # evaluate expr, break out of loop if false
        append buffer \
            $tmpbuffer \
            [emitter_container_for_expr $tmpsymbol]
    }

    # body block
    if {$body_keys != {}} {
        if {!$is_constant} {
            append buffer "\n"
        }

        # Setup try/catch for break and continue exceptions
        append buffer \
            [emitter_container_for_try_start] \
            [emitter_container_fake_tclexception]

        # Push loop break/continue/return context
        if {$_compileproc(options,inline_controls)} {
            compileproc_push_controls_context for 1 1
        }

        foreach body_key $body_keys {
            append buffer [compileproc_emit_invoke $body_key]
        }

        # Pop loop break/continue/return context
        if {$_compileproc(options,inline_controls)} {
            compileproc_pop_controls_context for
        }

        # Finish try/catch for break and continue exceptions
        append buffer [emitter_container_for_try_end]
    }

    # Close loop block and call interp.resetResult()
    # if the result of this command is used.

    append buffer \
        [emitter_container_for_end [compileproc_omit_set_result $key]]

    return $buffer
}


# Emit code for an inlined catch container.

proc compileproc_emit_container_catch { key } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_emit_container_catch $key"
#    }

    set buffer ""

    if {![descend_container_is_valid $key] || \
            ![descend_container_is_static $key]} {
        error "NOT valid or NOT static for key $key"
    }

    # Container commands is 0 to N keys for a catch
    set body_keys [descend_commands $key container]
#    if {$debug} {
#        puts "body_keys is \{$body_keys\}"
#        puts "descend_container_catch_has_variable is [descend_container_catch_has_variable $key]"
#    }

    # Determine if the catch command has a non-static varname
    # argument. If it does, then evaluate it before the
    # catch loop.

    if {[descend_container_catch_has_variable $key]} {
        set has_variable 1
        set has_static_varname 0
        set has_static_array_varname 0

        set vinfo [descend_container_catch_variable $key]

        if {[lindex $vinfo 0]} {
            set has_static_varname 1
            set static_varname [lindex $vinfo 1]
        } elseif {[compileproc_can_inline_variable_access $key 2]} {
            # The catch variable is not static, but it is
            # a static array name with a non-static key.

            set has_static_array_varname 1
        } else {
            # Evaluate varname argument and save the
            # variable name in a local of type String.

            set tuple [compileproc_emit_argument $key 2 true]
            set tclobject_symbol [lindex $tuple 1]
            append buffer [lindex $tuple 2]
            set varsymbol [compileproc_tmpvar_next]
            append buffer [emitter_statement \
                "String $varsymbol = ${tclobject_symbol}.toString()"]
        }
    } else {
        set has_variable 0
    }

    if {$body_keys == {}} {
        # No body commands to executed, just reset
        # the interp result and set a variable if
        # catch has three arguments.
        if {$has_variable} {
            if {$has_static_varname} {
                # simple static varname
                set set_var_buffer ""
                emitter_indent_level +1
                append set_var_buffer \
                    [compileproc_set_variable {} $static_varname true "" true]
                emitter_indent_level -1
                append buffer [compileproc_container_catch_handler_var \
                    {} true $set_var_buffer]
            } elseif {$has_static_array_varname} {
                # static array name with non-static key
                set gs_buffers \
                    [compileproc_get_set_nonconstant_array_variable $key 2 \
                        set {} false {""}]
                append buffer [lindex $gs_buffers 0]
                # No body code to append
                append buffer [compileproc_container_catch_handler_var \
                    {} true [lindex $gs_buffers 1]]
            } else {
                # non-static varname evaluated at runtime
                set set_var_buffer ""
                emitter_indent_level +1
                append set_var_buffer \
                    [compileproc_set_variable {} $varsymbol false "" true]
                emitter_indent_level -1
                append buffer [compileproc_container_catch_handler_var \
                    {} true $set_var_buffer]
            }
        } else {
            append buffer [compileproc_container_catch_handler_novar {} true]
        }
    } else {
        set catch_body_buffer ""

        set code_tmpsymbol [compileproc_tmpvar_next code]
        append catch_body_buffer \
            [emitter_container_catch_try_start $code_tmpsymbol] \
            [emitter_container_fake_tclexception]

        # Push loop break/continue context
        if {$_compileproc(options,inline_controls)} {
            compileproc_push_controls_context catch 0 0 0
        }

        foreach body_key $body_keys {
            append catch_body_buffer [compileproc_emit_invoke $body_key]
        }

        # Pop loop break/continue context
        if {$_compileproc(options,inline_controls)} {
            compileproc_pop_controls_context catch
        }

        append catch_body_buffer [emitter_container_catch_try_end $code_tmpsymbol]

        # Set variable if there was one

        if {$has_variable} {
            if {$has_static_varname} {
                # simple static varname
                append buffer $catch_body_buffer
                set set_var_buffer ""
                emitter_indent_level +1
                append set_var_buffer \
                    [compileproc_set_variable {} $static_varname true "result" false]
                emitter_indent_level -1
                append buffer [compileproc_container_catch_handler_var \
                    $code_tmpsymbol false $set_var_buffer]
            } elseif {$has_static_array_varname} {
                # static array name with non-static key
                set gs_buffers \
                    [compileproc_get_set_nonconstant_array_variable $key 2 \
                        set {} false "result"]
                append buffer [lindex $gs_buffers 0]
                append buffer $catch_body_buffer
                append buffer [compileproc_container_catch_handler_var \
                    $code_tmpsymbol false [lindex $gs_buffers 1]]
            } else {
                # non-static varname evaluated at runtime
                append buffer $catch_body_buffer
                set set_var_buffer ""
                emitter_indent_level +1
                append set_var_buffer \
                    [compileproc_set_variable {} $varsymbol false "result" false]
                emitter_indent_level -1
                append buffer [compileproc_container_catch_handler_var \
                    $code_tmpsymbol false $set_var_buffer]
            }
        } else {
            append buffer $catch_body_buffer
            append buffer [compileproc_container_catch_handler_novar $code_tmpsymbol false]
        }
    }
    return $buffer
}

# The next two methods are used to close a catch block.
# The first is invoked when no variable is passed to
# the catch command.

proc compileproc_container_catch_handler_novar { tmpsymbol is_empty_body } {
    set buffer ""

    append buffer [emitter_reset_result]
    if {$is_empty_body} {
        append buffer [emitter_indent] \
            "interp.setResult(TCL.OK)\;\n"
    } else {
        append buffer [emitter_indent] \
            "interp.setResult(" $tmpsymbol ")\;\n"
    }

    return $buffer
}

proc compileproc_container_catch_handler_var { tmpsymbol is_empty_body setvar_buffer } {
    set buffer ""

    if {!$is_empty_body} {
        append buffer [emitter_indent] \
            "TclObject result = interp.getResult()\;\n"
    }
    append buffer [emitter_indent] \
        "try \{\n"
    emitter_indent_level +1

    # Emit variable assignment

    if {$setvar_buffer == ""} {
        error "setvar buffer should not be empty"
    }
    append buffer $setvar_buffer

    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\} catch (TclException ex) \{\n"
    emitter_indent_level +1
    append buffer [emitter_indent] \
        "TJC.catchVarErr(interp)\;\n"
    emitter_indent_level -1
    append buffer [emitter_indent] \
        "\}\n"

    append buffer [emitter_reset_result]
    if {$is_empty_body} {
        append buffer [emitter_indent] \
            "interp.setResult(TCL.OK)\;\n"
    } else {
        append buffer [emitter_indent] \
            "interp.setResult(" $tmpsymbol ")\;\n"
    }

    return $buffer
}

# Return tuple if {IS_CATCH IS_RESULT_USED} for the
# given command key. The IS_CATCH element will be
# true if the command is "catch". The IS_RESULT_USED
# element will be true when a variable name argument
# is passed to the catch command and false otherwise.

proc compileproc_container_catch_with_varname { key } {
    global _compileproc_ckeys
    if {![info exists _compileproc_ckeys($key,info_key)]} {
        return {0 0}
    }
    set cmdname [lindex $_compileproc_ckeys($key,info_key) 1]
    if {$cmdname == "_UNKNOWN"} {
        return {0 0}
    }
    switch -exact -- $cmdname {
        "::catch" -
        "catch" {
            return [list 1 \
                [descend_container_catch_has_variable $key]]
        }
        default {
            return {0 0}
        }
    }
}

# Emit code for an inlined foreach container.

proc compileproc_emit_container_foreach { key } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_emit_container_foreach $key"
#    }

    set buffer ""

    if {![descend_container_is_valid $key] || \
            ![descend_container_is_static $key]} {
        error "NOT valid or NOT static for key $key"
    }

    # container commands is 0 to N keys for the body block
    set body_keys [descend_commands $key container]
#    if {$debug} {
#        puts "body_keys is \{$body_keys\}"
#    }

    # The varlist needs to be constant, but the values
    # lists to be iterated over can be a constant, a
    # variable, a command, or a word.

    set varlistvalues [descend_container_foreach_varlistvalues $key]
    if {([llength $varlistvalues] % 2) != 0} {
        error "expected even number of varnames and varlists, got \{$varlistvalues\}"
    }
    set varnamelist_indexes [list]
    set valuelist_indexes [list]

    foreach {i1 i2} $varlistvalues {
        lappend varnamelist_indexes $i1
        lappend valuelist_indexes $i2
    }

    set varnamelists [list]
    set list_symbols [list]

    # Figure out how many list objects will be looped over

    foreach index $varnamelist_indexes {
        set varlist [descend_container_foreach_varlist $key $index]
        lappend varnamelists $varlist
    }

#    if {$debug} {
#        puts "varnamelists is \{$varnamelists\}"
#        puts "varnamelist_indexes is \{$varnamelist_indexes\}"
#        puts "valuelist_indexes is \{$valuelist_indexes\}"
#    }

    # Declare a local variable to hold a ref to each list value.

    set num_locals [llength $valuelist_indexes]
    for {set i 0} {$i < $num_locals} {incr i} {
        set tmpsymbol [compileproc_tmpvar_next]
        append buffer [emitter_statement "TclObject $tmpsymbol = null"]
        lappend list_symbols $tmpsymbol
    }

    # Open try block after declaring locals that hold the list refs

    append buffer [emitter_container_foreach_try_finally_start]

    # Generate code to evaluate argument into a TclObject symbol

    foreach index $valuelist_indexes tmpsymbol $list_symbols {
        set tuple [compileproc_emit_argument $key $index 0 $tmpsymbol]
        set list_type [lindex $tuple 0]
        set list_symbol [lindex $tuple 1]
        set list_buffer [lindex $tuple 2]

        append buffer $list_buffer
        if {$list_type == "constant"} {
            append buffer [emitter_indent] \
                $tmpsymbol " = " $list_symbol "\;\n"
        }

        # Invoke TclObject.preserve() to hold a ref. Don't worry
        # about the skip constant incr flag for this module since
        # a foreach is almost always use with a non-constant list
        # and the complexity of not emitting the enclosing try
        # block is not worth it.

        append buffer [emitter_container_foreach_list_preserve $tmpsymbol]
    }

    append buffer [compileproc_container_foreach_loop_start $varnamelists $list_symbols]

    # Emit body commands
    if {$body_keys != {}} {
        append buffer "\n"

        # Setup try/catch for break/continue/return
        append buffer \
            [emitter_container_for_try_start] \
            [emitter_container_fake_tclexception]

        # Push loop break/continue context
        if {$_compileproc(options,inline_controls)} {
            compileproc_push_controls_context foreach 1 1
        }

        foreach body_key $body_keys {
            append buffer [compileproc_emit_invoke $body_key]
        }

        # Pop loop break/continue/return context
        if {$_compileproc(options,inline_controls)} {
            compileproc_pop_controls_context foreach
        }

        append buffer [emitter_container_for_try_end]
    }

    append buffer [compileproc_container_foreach_loop_end $key $list_symbols]

    return $buffer
}

# Start foreach loop. Pass a list of the varlist arguments
# to the foreach command and a list of the evaluated symbols
# that contain list values.

proc compileproc_container_foreach_loop_start { varlists list_symbols } {
    set buffer ""
#    set debug 0

#    if {$debug} {
#        puts "compileproc_container_foreach_loop_start: varlists \{$varlists\} : list_symbols \{$list_symbols\}"
#    }

    if {[llength $varlists] == 0} {
        error "varlists can't be {}"
    }
    if {[llength $list_symbols] == 0} {
        error "list_symbols can't be {}"
    }

    set multilists [expr {[llength $list_symbols] > 1}]
    if {$multilists} {
        set multivars 1
    } else {
        set varlist [lindex $varlists 0]
        set multivars [expr {[llength $varlist] > 1}]
    }

    set tmpsymbols [list]
    set list_symbol_lengths [list]

    foreach list_symbol $list_symbols {
        append buffer [emitter_container_foreach_list_length $list_symbol]
        lappend list_symbol_lengths "${list_symbol}_length"
    }
    if {$multilists} {
        set num_loops_tmpsymbol [compileproc_tmpvar_next num_loops]
        set max_loops_tmpsymbol [compileproc_tmpvar_next max_loops]
        append buffer [emitter_indent] \
            "int " $num_loops_tmpsymbol ", " $max_loops_tmpsymbol " = 0\;\n"
        foreach varlist $varlists list_symbol $list_symbols {
            set num_variables [llength $varlist]
            if {$num_variables > 1} {
                append buffer [emitter_indent] \
                    $num_loops_tmpsymbol " = (" \
                    ${list_symbol} "_length + " $num_variables \
                    " - 1) / " $num_variables "\;\n"
            } else {
                append buffer [emitter_indent] \
                    $num_loops_tmpsymbol " = " $list_symbol "_length\;\n"
            }
            append buffer [emitter_indent] \
                "if ( " $num_loops_tmpsymbol " > " $max_loops_tmpsymbol " ) \{\n"
            emitter_indent_level +1
            append buffer [emitter_indent] \
                $max_loops_tmpsymbol " = " $num_loops_tmpsymbol "\;\n"
            emitter_indent_level -1
            append buffer [emitter_indent] \
                "\}\n"
        }
    }

    # Newline before for loop
    append buffer "\n"

    # Create for loop
    set tmpi [compileproc_tmpvar_next index]
    set init_buffer "int $tmpi = 0"

    if {!$multilists} {
        # Iterating over a single list
        #set list_symbol [lindex $list_symbols 0]
        set list_symbol_length [lindex $list_symbol_lengths 0]
        set test_buffer "$tmpi < $list_symbol_length"
    } else {
        # Iterating over multiple lists
        set test_buffer "$tmpi < $max_loops_tmpsymbol"
    }

    if {$multilists || !$multivars} {
        set incr_buffer "${tmpi}++"
    } else {
        set varlist [lindex $varlists 0]
        set incr_buffer "$tmpi += [llength $varlist]"
    }
    append buffer \
        [emitter_container_for_start $init_buffer $test_buffer $incr_buffer]

    # Map tmpsymbol to tuple {varname list_symbol list_symbol_length mult offset index}

    if {!$multilists} {
        set list_symbol [lindex $list_symbols 0]
        set list_symbol_length [lindex $list_symbol_lengths 0]
        set varlist [lindex $varlists 0]
        for {set i 0} {$i < [llength $varlist]} {incr i} {
            set varname [lindex $varlist $i]
            set tmpsymbol [compileproc_tmpvar_next]
            lappend tmpsymbols $tmpsymbol
            set tuple [list $varname $list_symbol $list_symbol_length 1 $i $tmpi]
            set tmpsymbols_map($tmpsymbol) $tuple
        }
    } else {
        foreach varlist $varlists \
                list_symbol $list_symbols \
                list_symbol_length $list_symbol_lengths {
            set mult [llength $varlist]
            for {set i 0} {$i < [llength $varlist]} {incr i} {
                set varname [lindex $varlist $i]
                set tmpsymbol [compileproc_tmpvar_next]
                lappend tmpsymbols $tmpsymbol
                if {$mult > 1} {
                    set isym "${list_symbol}_index"
                } else {
                    set isym $tmpi
                }
                set tuple [list $varname $list_symbol $list_symbol_length \
                    $mult $i $isym]
                set tmpsymbols_map($tmpsymbol) $tuple
            }
        }
    }

#    if {$debug} {
#        parray tmpsymbols_map
#    }

    # Determine indexes for each list if needed

    if {$multilists} {
        foreach tmpsymbol $tmpsymbols {
            set tuple $tmpsymbols_map($tmpsymbol)
            foreach {varname list_symbol list_symbol_length mult offset ivar} \
                $tuple break

            if {$mult > 1} {
                if {[info exists declared_ivar($ivar)]} {
                    continue
                }
                append buffer [emitter_indent] \
                    "int " $ivar " = " $tmpi " * " $mult "\;\n"
                set declared_ivar($ivar) 1
            }
        }
    }

    # Query TclObject value at list index.

    foreach tmpsymbol $tmpsymbols {
        set tuple $tmpsymbols_map($tmpsymbol)
        foreach {varname list_symbol list_symbol_length mult offset ivar} \
            $tuple break

        if {!$multivars && !$multilists} {
            set index $tmpi
        } elseif {$multivars || $multilists} {
            if {$offset == 0} {
                set index "$ivar"
            } else {
                set index "$ivar + $offset"
            }
        }

        if {!$multivars && !$multilists} {
            append buffer [emitter_indent] \
                "TclObject " $tmpsymbol " = TclList.index(interp, " \
                $list_symbol ", " $index ")\;\n"
        } elseif {$multilists || $multivars} {
            append buffer \
                [emitter_indent] \
                    "TclObject " $tmpsymbol " = null" "\;\n" \
                [emitter_indent] \
                "if ( " $index " < " $list_symbol_length " ) \{\n"
            emitter_indent_level +1
            append buffer [emitter_indent] \
                $tmpsymbol " = TclList.index(interp, " $list_symbol ", " $index ")\;\n"
            emitter_indent_level -1
            append buffer [emitter_indent] \
                "\}\n"
        }
    }

    # Set variables

    foreach tmpsymbol $tmpsymbols {
        set tuple $tmpsymbols_map($tmpsymbol)
        foreach {varname list_symbol list_symbol_length mult offset ivar} \
            $tuple break

        append buffer [emitter_container_foreach_var_try_start]
        if {!$multivars && !$multilists} {
            append buffer \
                [compileproc_set_variable {} $varname true $tmpsymbol false]
        } elseif {$multilists || $multivars} {
            append buffer [emitter_indent] \
                "if ( " $tmpsymbol " == null ) \{\n"
            emitter_indent_level +1
            append buffer \
                [compileproc_set_variable {} $varname true "" true]
            emitter_indent_level -1
            append buffer [emitter_indent] \
                "\} else \{\n"
            emitter_indent_level +1
            append buffer \
                [compileproc_set_variable {} $varname true $tmpsymbol false]
            emitter_indent_level -1
            append buffer [emitter_indent] \
                "\}\n"
        }
        append buffer [emitter_container_foreach_var_try_end $varname]
    }

    return $buffer
}

# End foreach loop end logic

proc compileproc_container_foreach_loop_end { key list_symbols } {
    set buffer ""

    # Close loop block and call interp.resetResult()
    # if the result of this command is used.

    append buffer \
        [emitter_container_for_end [compileproc_omit_set_result $key]]

    # Add finally block to decrement ref count for lists
    append buffer [emitter_container_foreach_try_finally]

    foreach list_symbol $list_symbols {
        append buffer [emitter_container_foreach_list_release $list_symbol]
    }

    # Close try/finally block around loop
    append buffer [emitter_container_foreach_try_finally_end]

    return $buffer
}

# Emit code for an inlined switch container. This container
# has two flavors. In the general case, use the switch
# command at runtime to search for a body to execute.
# If the mode is exact matching and each of the patterns
# is a constant string then an optimized search is used.

proc compileproc_emit_container_switch { key } {
#    set debug 0

    set buffer ""

    if {![descend_container_is_valid $key] || \
            ![descend_container_is_static $key]} {
        error "NOT valid or NOT static for key $key"
    }

    # Check flag that disables the special constant string
    # code for a switch command.
    set inline_constant_strings 1

    global _compileproc
    set ind [lsearch -exact $_compileproc(options,inline_containers) \
        switch_no_constant_strings]
    if {$ind != -1} {
        set inline_constant_strings 0
    }

    # No special inline for -glob and -regexp modes.

    set switch_mode [descend_container_switch_mode $key]
    if {$switch_mode != "default" && $switch_mode != "exact"} {
        set inline_constant_strings 0
#        if {$debug} {
#            puts "inline_constant_strings set to 0 because mode is $switch_mode"
#        }
    }

    # container commands is 0 to N lists of keys for each body block
    set body_keys [descend_commands $key container]
#    if {$debug} {
#        puts "body_keys is \{$body_keys\}"
#    }

    set pbIndexes [descend_container_switch_patbody_indexes $key]
    set pbIndex [lindex $pbIndexes 0]

    # If one of the patterns is a variable or a command, then
    # can't use special inline code.

    if {$inline_constant_strings} {
        # Check that each pattern argument is a constant string.
        foreach {pIndex bIndex} $pbIndexes {
            set tuple [compileproc_get_argument_tuple $key $pIndex]
            set type [lindex $tuple 0]
            if {$type != "constant"} {
                set inline_constant_strings 0
#                if {$debug} {
#                    puts "inline_constant_strings set to 0 because pattern index $pIndex is type $type"
#                }
            }
        }
    }

    set sIndex [descend_container_switch_string $key]

    # Get string argument as TclObject
    set tuple [compileproc_emit_argument $key $sIndex]
    set str_type [lindex $tuple 0]
    set str_symbol [lindex $tuple 1]
    set str_buffer [lindex $tuple 2]
    append buffer $str_buffer
    # Get string argument as a String
    set string_tmpsymbol [compileproc_tmpvar_next]
    append buffer [emitter_indent] \
        "String " $string_tmpsymbol " = " $str_symbol ".toString()\;\n"

    # If no -- appears before the string argument
    # then the string can't start with a "-" character.

    if {![descend_container_switch_has_last $key]} {
        append buffer [emitter_indent] \
            "TJC.switchStringIsNotOption(interp, " $string_tmpsymbol ")\;\n"
    }

    if {$inline_constant_strings} {
        append buffer \
            [compileproc_emit_container_switch_constant $key $string_tmpsymbol]
        return $buffer
    }

    # Not the optimized version for constant strings, invoke
    # the runtime implementation for the switch command that
    # will locate the switch block to execute. Don't worry
    # about the skip constant incrment switch here.

    # Declare match offset variable
    set offset_tmpsymbol [compileproc_tmpvar_next]
    append buffer [emitter_indent] \
        "int " $offset_tmpsymbol "\;\n"

    # Allocate array of TclObject and open try block
    set array_tmpsymbol [compileproc_tmpvar_next objv]
    set size [llength $pbIndexes]
    append buffer \
        [emitter_container_switch_start $array_tmpsymbol $size] \
        [emitter_container_try_start]

    # Assign values to the proper indexes in the array.

    set tmpsymbol [compileproc_tmpvar_next]
    append buffer [emitter_indent] \
        "TclObject " $tmpsymbol "\;\n"

    set pattern_comments [list]
    set i 0
    foreach {patIndex bodyIndex} $pbIndexes {
        # Pattern description
        set tuple [compileproc_argument_printable $key $patIndex]
        set str [lindex $tuple 1]
        set comment "Pattern $str"
        lappend pattern_comments $comment
        append buffer [emitter_comment $comment]

        # Evaluate argument code
        set tuple [compileproc_emit_argument $key $patIndex 0 $tmpsymbol]
        set pat_type [lindex $tuple 0]
        set pat_symbol [lindex $tuple 1]
        set pat_buffer [lindex $tuple 2]
        append buffer \
            $pat_buffer \
            [emitter_container_switch_assign $array_tmpsymbol $i \
            $tmpsymbol $pat_symbol]
        incr i 1

        # If the fallthrough "-" body was given, then pass a constant
        # string to indicate that. Otherwise, this method would assume
        # a body block was compiled if a null TclObject body is passed.

        if {[descend_container_switch_is_fallthrough $key $bodyIndex]} {
            # Body description
            append buffer [emitter_comment "- fallthrough"]

            set tuple [compileproc_emit_argument $key $bodyIndex]
            set body_type [lindex $tuple 0]
            if {$body_type != "constant"} {
                error "expected body to be constant type, got $body_type"
            }
            set body_symbol [lindex $tuple 1]
            set body_buffer [lindex $tuple 2]
            append buffer \
                $body_buffer \
                [emitter_container_switch_assign $array_tmpsymbol $i \
                    $tmpsymbol $body_symbol]
        }
        incr i 1
    }

    # call invokeSwitch(), close try block, and releaseObjvElems() in finally block

    append buffer [emitter_container_switch_invoke \
        $offset_tmpsymbol \
        $array_tmpsymbol 0 $size \
        $string_tmpsymbol \
        $switch_mode \
        ]

    # If blocks for body blocks based on body offset.
    set offsets [list]
    foreach {patIndex bodyIndex} $pbIndexes {
        if {[descend_container_switch_is_fallthrough $key $bodyIndex]} {
            lappend offsets ""
        } else {
            set offset [expr {$bodyIndex - $pbIndex}]
            lappend offsets $offset
        }
    }
    if {[llength $offsets] == 0} {error "empty offsets list"}
    append buffer \
        [emitter_reset_result] \
        [emitter_container_if_start "$offset_tmpsymbol == -1"] \
        [emitter_indent] \
        "// No match\n"
    for {set i 0} {$i < [llength $offsets]} {incr i} {
        set offset [lindex $offsets $i]
        if {$offset == ""} {
            # Fall through body block
            continue
        }
        append buffer [emitter_container_if_else_if "$offset_tmpsymbol == $offset"]

        set pattern_comment [lindex $pattern_comments $i]
        if {$pattern_comment != ""} {
            append buffer [emitter_comment $pattern_comment]
        }

        foreach body_key [lindex $body_keys $i] {
            append buffer [compileproc_emit_invoke $body_key]
        }
    }
    append buffer \
        [emitter_container_if_else] \
        [emitter_indent] \
        "throw new TclRuntimeError(\"bad switch body offset \" +\n"
    emitter_indent_level +1
    append buffer [emitter_statement "String.valueOf($offset_tmpsymbol))"]
    emitter_indent_level -1
    append buffer [emitter_container_if_end]

    return $buffer
}

# Emit inlined constant string switch code. This is a faster
# version that is only used when the mode is exact and the
# patterns are all constant strings.

proc compileproc_emit_container_switch_constant { key string_tmpsymbol } {
#    set debug 0

#    if {$debug} {
#        puts "compileproc_emit_container_switch_constant $key $string_tmpsymbol"
#    }

    set buffer ""

    set body_keys [descend_commands $key container]
    set pbIndexes [descend_container_switch_patbody_indexes $key]

    set length "${string_tmpsymbol}_length"
    set first "${string_tmpsymbol}_first"

    append buffer [emitter_indent] \
        "int " $length " = " $string_tmpsymbol ".length()\;\n" \
        [emitter_indent] \
        "char " $first " = '\\n'\;\n" \
        [emitter_container_if_start "$length > 0"] \
        [emitter_indent] \
        $first " = " $string_tmpsymbol ".charAt(0)\;\n" \
        [emitter_container_if_end]

    # Don't bother invoking resetResult() when we know the
    # result of this switch command is not used.

    if {![compileproc_omit_set_result $key]} {
        append buffer [emitter_reset_result]
    }

    emitter_indent_level +2
    set spacer2 [emitter_indent]
    emitter_indent_level -1
    set spacer1 [emitter_indent]
    emitter_indent_level -1

    set i 0
    set last_pattern [lindex $pbIndexes end-1]
    set ifnum 0
    set fallthrough_expression ""

    foreach {patIndex bodyIndex} $pbIndexes {
        # Inline pattern strings as Java String objects.

        set tuple [compileproc_get_argument_tuple $key $patIndex]
        set pattern [lindex $tuple 1]
        set cmap [lindex $tuple 3]
#        if {$debug} {
#            puts "pattern ->$pattern<- at index $patIndex"
#            puts "pattern cmap is \{$cmap\}"
#        }

        if {$cmap == {}} {
            # Grab the first character out of a pattern
            # that contains no backslash elements.
            set pattern_first [string index $pattern 0]
            set pattern_len [string length $pattern]
        } else {
            # The constant string pattern contains backslashes.
            # Extract from 1 to N characters from the pattern
            # that correspond to 1 character from the original
            # Tcl string.
            set first_num_characters [lindex $cmap 0]
            set first_end_index [expr {$first_num_characters - 1}]
            set pattern_first [string range $pattern 0 $first_end_index]

            # Pattern length is length of Tcl string, not the
            # length of the escaped string.
            set pattern_len [llength $cmap]
        }
#        if {$debug} {
#            puts "pattern_first is ->$pattern_first<-"
#            puts "pattern_len is $pattern_len"
#        }

        set pattern_jstr [emitter_backslash_tcl_string $pattern]
        set pattern_first_jstr [emitter_backslash_tcl_string $pattern_first]
#        if {$debug} {
#            puts "pattern_jstr is ->$pattern_jstr<-"
#            puts "pattern_first_jstr is '$pattern_first_jstr'"
#        }

        set expression ""
        if {$pattern_len > 0} {
            append expression \
                $length " == " $pattern_len " && " \
                $first " == '" $pattern_first_jstr "'"
            if {$pattern_len > 1} {
                # Note: Invoke "String.compareTo(String) == 0" here since
                # testing shows that it is a bit faster than "String.equals(String)"
                append expression "\n" $spacer2 \
                    "&& " $string_tmpsymbol ".compareTo(\"" \
                    $pattern_jstr "\") == 0"
            }
        } else {
            append expression \
                $length " == 0"
        }

        if {[descend_container_switch_is_fallthrough $key $bodyIndex]} {
            # Double check fallthrough container commands
            if {[lindex $body_keys $i] != {}} {
                error "expected empty body keys for index $i, got \{[lindex $body_keys $i]\}"
            }
        
            if {$fallthrough_expression != ""} {
                append fallthrough_expression $spacer1
            }
            append fallthrough_expression "( " $expression " ) ||\n"
            incr i
            continue
        }
        if {$fallthrough_expression != ""} {
            append fallthrough_expression \
                $spacer1 \
                "( " $expression " )"
            set expression $fallthrough_expression
            set fallthrough_expression ""
        }

        if {($patIndex == $last_pattern) && ($pattern == "default")} {
            if {$ifnum == 0} {
                append buffer [emitter_container_if_start true]
            } else {
                append buffer [emitter_container_if_else]
            }
        } elseif {$ifnum == 0} {
            append buffer [emitter_container_if_start $expression]
        } else {
            append buffer [emitter_container_if_else_if $expression]
        }
        incr ifnum

        # Argument description
        set tuple [compileproc_argument_printable $key $patIndex]
        set astr [lindex $tuple 1]
        append buffer [emitter_indent] \
            "// Pattern " $astr "\n"

        # Emit commands

        foreach body_key [lindex $body_keys $i] {
            append buffer [compileproc_emit_invoke $body_key]
        }

        incr i
    }
    if {$fallthrough_expression != ""} {
        error "should not have fallen through past last body"
    }

    append buffer [emitter_container_if_end]

    return $buffer
}

# Generate code to set a variable to a value and assign
# the result to a tmpsymbol of type TclObject. If no
# result assignment is needed, pass {} as tmpsymbol.
# This method assumes that a variable name is statically
# defined. This method will emit different code for scalar
# vs array variables.

proc compileproc_set_variable { tmpsymbol varname varname_is_string value value_is_string } {
    if {$value_is_string} {
        # FIXME: Would be better to do this in the emitter layer, would
        # require adding is_value_string argument to emitter_set_var + scalar func.
        set value [emitter_double_quote_tcl_string $value]
    }

    if {$varname_is_string} {
        # static variable name, emit either array or scalar assignment
        set vinfo [descend_simple_variable $varname]
        if {[lindex $vinfo 0] == "array"} {
            set p1 [lindex $vinfo 1]
            set p2 [lindex $vinfo 2]
            return [compileproc_emit_array_variable_set $tmpsymbol $p1 $p2 true $value]
        } elseif {[lindex $vinfo 0] == "scalar"} {
            return [compileproc_emit_scalar_variable_set $tmpsymbol $varname $value]
        } else {
            error "unexpected result \{$vinfo\} from descend_simple_variable"
        }
    } else {
        # Non-static variable name, can't use cache so handle with
        # interp.setVar()
        return [emitter_set_var $tmpsymbol $varname false null false $value 0]
    }
}

# Generate code to get a variable value and assign
# the result to the passed in tmpsymbol. This
# method assumes that a variable name is static.
# This method will emit different code for scalar vs
# array variables.

proc compileproc_get_variable { tmpsymbol varname varname_is_string } {
    if {$varname_is_string} {
        # static variable name, emit either array or scalar assignment
        set vinfo [descend_simple_variable $varname]
        if {[lindex $vinfo 0] == "array"} {
            set p1 [lindex $vinfo 1]
            set p2 [lindex $vinfo 2]
            return [compileproc_emit_array_variable_get $tmpsymbol $p1 $p2 true]
        } elseif {[lindex $vinfo 0] == "scalar"} {
            return [compileproc_emit_scalar_variable_get $tmpsymbol $varname]
        } else {
            error "unexpected result \{$vinfo\} from descend_simple_variable"
        }
    } else {
        # Non-static variable name, can't use cache so handle with
        # interp.getVar() which works with either scalar or
        # array variable names.
        return [emitter_get_var $tmpsymbol $varname false null false 0]
    }
}

# Push a controls context. The default controls context is the whole
# procedure. A new context is pushed when a loop is entered or
# a catch command is encountered. The controls context is used
# to determine when a control command like break, continue, and
# return can be inlined. The error command is a control command
# but it is never inlined.

proc compileproc_push_controls_context { type can_break can_continue {can_return keep} } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_push_controls_context $type $can_break\
#            $can_continue $can_return"
#    }

    # The default can_return means that the new context should retain the
    # can_return value from the current controls context. This is so that
    # a return in a loop in a proc can be inlined while a return in a catch
    # block is not.

    if {$can_return == "keep"} {
        set tuple [lindex $_compileproc(options,controls_stack) 0]
        set can_return [lindex $tuple 6]
    }

    set tuple [list $type "break" $can_break "continue" $can_continue \
        "return" $can_return]
#    if {$debug} {
#        puts "push controls stack tuple \{$tuple\}"
#    }

    set _compileproc(options,controls_stack) [linsert \
        $_compileproc(options,controls_stack) 0 $tuple]

#    if {$debug} {
#        puts "controls_stack:"
#        foreach tuple $_compileproc(options,controls_stack) {
#            puts $tuple
#        }
#    }
}

# Pop a controls context off the stack.

proc compileproc_pop_controls_context { type } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_pop_controls_context $type"
#    }

    set stack $_compileproc(options,controls_stack)
    if {[llength $stack] == 0} {
        error "attempt to pop off empty controls stack : type was $type"
    }
    set tuple [lindex $stack 0]
    set stack [lrange $stack 1 end]

    # Double check that the type matches, this might
    # catch a problem with mismatched push/pops.

    if {$type != [lindex $tuple 0]} {
        error "found controls type [lindex $tuple 0] but expected $type\
            : controls_stack \{$_compileproc(options,controls_stack)\}"
    } elseif {"proc" == [lindex $tuple 0] && $type != "proc"} {
        error "popped proc control context off of controls stack"
    }

#    if {$debug} {
#        puts "popped controls stack tuple \{$tuple\}"
#        puts "controls_stack:"
#        foreach tuple $_compileproc(options,controls_stack) {
#            puts $tuple
#        }
#    }

    set _compileproc(options,controls_stack) $stack
}

# Emit an inlined control statement. The control commands
# are break, continue, and return. The error command is
# always raised via a normal command invocation.

proc compileproc_emit_control { key } {
    global _compileproc
    global _compileproc_ckeys

#    set debug 0

#    if {$debug} {
#        puts "compileproc_emit_control $key"
#    }

    set cmdname [lindex $_compileproc_ckeys($key,info_key) 1]
    switch -exact -- $cmdname {
        "::break" -
        "break" {
            set statement "break"
        }
        "::continue" -
        "continue" {
            set statement "continue"
        }
        "::return" -
        "return" {
           set statement "return"
        }
        default {
            error "should have been break or continue command, got $cmdname"
        }
    }

    if {$statement == "return"} {
        return [compileproc_emit_control_return $key]
    } else {
        return [emitter_container_loop_break_continue $statement]
    }
}

# Emit an inlined return command. This is used for
# a return command that has either 0 or 1 arguments.
# A return command that appears inside a catch
# block is not inlined.

proc compileproc_emit_control_return { key } {
    global _compileproc
    global _compileproc_ckeys

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    set buffer ""

    if {$num_args == 1} {
        append buffer \
            [emitter_reset_result] \
            [emitter_control_return]
        return $buffer
    } elseif {$num_args == 2} {
        # No-op
    } else {
        error "expected return with 1 or 2 arguments, got num_args $num_args"
    }

    # Set interp result to TclObject argument. Handle the
    # case of a nested command with optimized code.

    set buffer ""

    set tuple [compileproc_get_argument_tuple $key 1]
    set type [lindex $tuple 0]
    set ckeys [lindex $tuple 1]

    if {$type == "command"} {
        # Invoke nested command(s) and leave interp result as-is.
        foreach ckey $ckeys {
            append buffer [compileproc_emit_invoke $ckey]
        }
        append buffer [emitter_control_return]
    } else {
        set tuple [compileproc_emit_argument $key 1]
        set obj_type [lindex $tuple 0]
        set obj_symbol [lindex $tuple 1]
        set obj_buffer [lindex $tuple 2]
        append buffer \
            $obj_buffer \
            [emitter_control_return_argument $obj_symbol]
    }

    # FIXME: It is not really clear that a resetResult() invocation
    # is needed before the interp result is set. We know that resetResult
    # is always invoked before cmdProc() entry. Is there any way that
    # an error can be raised and caught during normal execution in
    # a way the leaves the errorInfo vars in the interp set? I think
    # catch would clear these but more research is needed.

    return $buffer
}

# Emit a container expr command. Note that other container
# commands that have an expr block will use the command
# compileproc_expr_evaluate_boolean_emit to evaluate an
# expression as a boolean.

proc compileproc_emit_container_expr { key } {
#    set debug 0

#    if {$debug} {
#        puts "compileproc_emit_container_expr $key"
#    }

    set buffer ""

    if {![descend_container_is_valid $key] || \
            ![descend_container_is_static $key]} {
        error "NOT valid or NOT static for key $key"
    }

    append buffer [compileproc_expr_evaluate_result_emit $key]
    return $buffer
}

# This method is invoked when an expr command wants
# to evaluate an expression string and set the interp
# result to the value of the expression. A buffer
# containing the code to set the interp result is
# returned by this method.

proc compileproc_expr_evaluate_result_emit { key } {
    global _compileproc
    global _compileproc_key_info

#    set debug 0

#    if {$debug} {
#        puts "compileproc_expr_evaluate_result_emit $key"
#    }

    set buffer ""

    set expr_index 1
    set expr_result [compileproc_expr_evaluate $key $expr_index]
#    if {$debug} {
#        puts "expr_result is \{$expr_result\}"
#    }
    set eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
        $expr_result]
    set infostr [lindex $eval_tuple 0]
    set ev [lindex $eval_tuple 1]
    append buffer [lindex $eval_tuple 2]

    set ev_types [lindex $eval_tuple 3 3]
#    puts "ev_types \{$ev_types\}"

    if {$_compileproc(options,expr_inline_set_result)} {
        # Invoke specific Interp.setResult() method
        # based on compile time info about the
        # return value inside the ExprValue.

        if {$ev_types == {boolean}} {
            append buffer [emitter_indent] \
                "interp.setResult( " $ev ".getIntValue() != 0 )\;\n"
        } elseif {$ev_types == {int}} {
            append buffer [emitter_indent] \
                "interp.setResult( " $ev ".getIntValue() )\;\n"
        } elseif {$ev_types == {double}} {
            append buffer [emitter_indent] \
                "interp.setResult( " $ev ".getDoubleValue() )\;\n"
        } elseif {$ev_types == {String}} {
            append buffer [emitter_indent] \
                "interp.setResult( " $ev ".getStringValue() )\;\n"
        } else {
            append buffer [emitter_indent] \
                "TJC.exprSetResult(interp, " $ev ")\;\n"
        }
    } else {
        # Invoke exprSetResult() to set interp result
        # based on runtime type info.

        append buffer [emitter_indent] \
            "TJC.exprSetResult(interp, " $ev ")\;\n"
    }

    append buffer \
        [compileproc_emit_exprvalue_release $ev]

    return $buffer
}

# Emit a unary operator after evaluating a value.
# Return a tuple of {EXPRVALUE BUFFER EV_TYPES}
#
# EXPRVALUE : symbol of type ExprValue
# BUFFER : Buffer that will evaluate EXPRVALUE
# EV_TYPES : List of possible result types
#     Can be {} if types are not known at compile time
#     Can be 1 or more of {int double String}
#     Can be {boolean} for logical unary not operator

proc compileproc_expr_evaluate_emit_unary_operator { op_tuple } {
#    set debug 0

#    if {$debug} {
#        puts "compileproc_expr_evaluate_emit_unary_operator \{$op_tuple\}"
#    }

    set buffer ""
    set op_buffer ""
    set operand_ev_types {}

    set type [lindex $op_tuple 0]
    if {$type != {unary operator}} {
        error "expected \{unary operator\} but got type \{$type\}"
    }

    set vtuple [lindex $op_tuple 1]
    set op [lindex $vtuple 0]
    set values [lindex $vtuple 1]

    if {[llength $values] != 1} {
        error "values length should be 1,\
            not [llength $values],\
            values_list was \{$values\}"
    }

    set tuple [lindex $values 0]

    # Peek at the contents of the operand without
    # emitting code for it yet. In some cases, a
    # literal int or double value can be evaluated
    # at compile time.

    set peek_tuple [compileproc_expr_peek_operand $tuple]
    set value_info_type [lindex $peek_tuple 0]
    set value_info_value [lindex $peek_tuple 1]
    set value_info_has_null_string [lindex $peek_tuple 2]
    set infostr [lindex $peek_tuple 3]

    set is_numeric_literal 0
    set is_numeric_literal_int 0
    set is_numeric_literal_double 0

    if {$value_info_type == {int literal} ||
            $value_info_type == {double literal}} {
        set is_numeric_literal 1
        set numeric_literal $value_info_value
        set is_numeric_literal_int [string equal \
            $value_info_type {int literal}]
        set is_numeric_literal_double [string equal \
            $value_info_type {double literal}]
    }

#    if {$debug} {
#        puts "op is $op"
#        puts "passed tuple \{$tuple\} to compileproc_expr_peek_operand"
#
#        puts "value_info_type is $value_info_type"
#        puts "value_info_value is $value_info_value"
#        puts "value_info_has_null_string is $value_info_has_null_string"
#        puts "infostr is ->$infostr<-"
#    }

    set skip_unary_op_call 0
    set plus_number 0
    set negate_number 0
    set bitwise_negate_number 0

    # Check for the special case of the smallest
    # possible integer -0x80000000. This number
    # needs to be negated otherwise it will
    # not be a valid 32 bit integer. Regen literal
    # with a negative sign added to the front.

    if {$op == "-" && $value_info_type == "String"} {
#        if {$debug} {
#            puts "possible smallest int negation: op is $op,\
#                \{$value_info_type $value_info_value\}"
#        }
        set numeric_literal [string range \
            $value_info_value 1 end-1]
        set numeric_literal [string trim $numeric_literal]
        set first [string index $numeric_literal 0]
        if {$first == "-"} {
            set is_already_negative 1
        } else {
            set is_already_negative 0
        }
        set min "-0x80000000"
        set wneg "-${numeric_literal}"
        set is_jint [compileproc_string_is_java_integer $wneg]
#        if {$debug} {
#            puts "wneg is $wneg"
#            puts "min is $min"
#            puts "is_already_negative is $is_already_negative"
#            puts "is java integer is $is_jint"
#            puts "expr == compare is [expr {$wneg == $min}]"
#        }
        if {!$is_already_negative && \
                $is_jint && \
                ($wneg == $min)} {
            set negate_number 1
        } else {
            set negate_number 0
        }
    } elseif {$op == "-" && $is_numeric_literal && \
            ($numeric_literal > 0)} {
        set negate_number 1
    } elseif {$op == "+" && $is_numeric_literal} {
        # A unary plus operator checks that an
        # operand is a number and tosses out
        # the string rep. If a literal is known
        # to be a number, then there is no point
        # in invoking the unary operator at
        # runtime.

        set plus_number 1
    } elseif {$op == "~" &&
            $is_numeric_literal && $is_numeric_literal_int} {
        # A unary bitwise not operator only
        # works with int operands. If a literal
        # integer is found, then it can be inlined
        # without invoking the unary operator.

        set bitwise_negate_number 1
    }

    if {$negate_number || \
            $bitwise_negate_number || \
            $plus_number} {
        # Negate numeric literal and generate
        # eval buffer with a constant value.

        if {$bitwise_negate_number} {
            set tuple [list {constant int} "~${numeric_literal}"]
            set operand_ev_types int
        } elseif {$plus_number} {
            if {$is_numeric_literal_int} {
                set tuple [list {constant int} $numeric_literal]
                set operand_ev_types int
            } elseif {$is_numeric_literal_double} {
                set tuple [list {constant double} $numeric_literal]
                set operand_ev_types double
            } else {
                error "unknown literal type"
            }
        } elseif {$negate_number} {
            set tuple [list [lindex $tuple 0] "-${numeric_literal}"]
            if {$is_numeric_literal_int} {
                set operand_ev_types int
            } elseif {$is_numeric_literal_double} {
                set operand_ev_types double
            }
        } else {
            error "unknown operation"
        }

        set eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
            $tuple]

        set ev [lindex $eval_tuple 1]
        set op_buffer [lindex $eval_tuple 2]
        set skip_unary_op_call 1
    }

    set inline_op 0

    switch -exact -- $op {
        "+" {
            set opval TJC.EXPR_OP_UNARY_PLUS
        }
        "-" {
            set opval TJC.EXPR_OP_UNARY_MINUS
        }
        "!" {
            set opval TJC.EXPR_OP_UNARY_NOT

            if {$::_compileproc(options,expr_inline_operators)} {
                set inline_op 1
            }
        }
        "~" {
            set opval TJC.EXPR_OP_UNARY_BIT_NOT
        }
        default {
            error "unsupported unary operator \"$op\""
        }
    }

    if {$skip_unary_op_call} {
        # Don't emit operator method when a unary operator
        # has been applied to a constant value.
    } elseif {$inline_op} {
        # The operator and operand could not be evaluated
        # at compile time. If this operator has an inlined
        # implementation then generate that now.

        switch -exact -- $op {
            "+" {
                error "operator + should not be matched"
            }
            "-" {
                error "operator - should not be matched"
            }
            "!" {
                set op_tuple [\
                    compileproc_expr_evaluate_emit_inlined_unary_not_operator \
                    $tuple \
                    $value_info_type \
                    $value_info_value \
                    $value_info_has_null_string \
                    ]
            }
            "~" {
                error "operator ! should not be matched"
            }
        }

        set ev [lindex $op_tuple 0]
        set op_buffer [lindex $op_tuple 1]
        set operand_ev_types [lindex $op_tuple 2]
    } else {
        # Operator could not be evaluated at compile time
        # and no special inline logic was generated.
        # Generate code to create an ExprValue and
        # pass it to the TJC.exprUnaryOperator() API.

        set eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
            $tuple]
        set ev [lindex $eval_tuple 1]

        set operand_ev_types [lindex $eval_tuple 3 3]

        append op_buffer \
            [lindex $eval_tuple 2] \
            [emitter_indent] \
            "TJC.exprUnaryOperator(interp, " $opval ", " $ev ")\;\n"
    }

    # Emit printable info that describes the
    # operator and the operand:

    append buffer \
        [emitter_indent] \
        "// Unary operator: " $op " " $infostr "\n" \
        $op_buffer \
        [emitter_indent] \
        "// End Unary operator: " $op "\n"

    # Determine the operator result type. Some operators
    # have only one possible type while others
    # depend on the type of the operand.

    if {$op == "!"} {
        set ev_types boolean
    } elseif {$op == "~"} {
        set ev_types int
    } elseif {$op == "+" || $op == "-"} {
        if {$operand_ev_types == {int}} {
            set ev_types int
        } elseif {$operand_ev_types == {double}} {
            set ev_types double
        } else {
            set ev_types {int double}
        }
    }

    return [list $ev $buffer $ev_types]
}

# Emit an inlined unary not operator. This method
# is invoked only when inlined expr operators
# are enabled and the operand has not been
# evaluated as a compile time constant.
# This method will generate an inlined operator
# implementation that depends on the type
# op the operand and will return a tuple of:
#
# {EXPRVALUE BUFFER EV_TYPES}
#
# EXPRVALUE : symbol of type ExprValue
# BUFFER : Buffer that will evaluate EXPRVALUE
# EV_TYPES : Type of operand to unary operator

proc compileproc_expr_evaluate_emit_inlined_unary_not_operator {
        value_tuple value_type value_symbol value_has_null_string } {

#    set debug 0

#    if {$debug} {
#        puts "compileproc_expr_evaluate_emit_inlined_unary_not_operator\
#            \{$value_tuple\} \"$value_type\" $value_symbol $value_has_null_string"
#    }

    set buffer ""

    if {$value_type == "TclObject"} {
        # Generate buffer that evaluates to a TclObject ref.

        set eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
            $value_tuple {rtype TclObject}]
        set tclobject_sym [lindex $eval_tuple 1]
        append buffer [lindex $eval_tuple 2]

        set ev [compileproc_tmpvar_next]
        append buffer [compileproc_emit_exprvalue_get $ev "" ""]

        # Note that TJC.exprUnaryNotOperatorKnownInt() will read
        # the int value from a TclObject and set a new int
        # value with a null string value. A double value
        # will be read directly by TJC.exprUnaryNotOperator()
        # and the string value will also be null.

        append buffer \
            [emitter_container_if_start "$tclobject_sym.isIntType()"] \
            [emitter_statement \
                "TJC.exprUnaryNotOperatorKnownInt($ev, $tclobject_sym)"] \
            [emitter_container_if_else] \
            [emitter_statement \
                "TJC.exprUnaryNotOperator(interp, $ev, $tclobject_sym)"] \
            [emitter_container_if_end]

        # Type of TclObject operand not known at compile time
        set ev_types {}
    } else {
        # Generate buffer that evaluates to an ExprValue.
        # Pass {nostr 1} flag to indicate that a literal should
        # be generated with a null string value. Certain
        # literals like "0xFF" would have a string value
        # but it would get thrown away by this operator.

        set eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
            $value_tuple {nostr 1}]

        set ev [lindex $eval_tuple 1]
        append buffer [lindex $eval_tuple 2]
        set result_info [lindex $eval_tuple 3]

        # Types for ExprValue may be known at compile time
        set ev_types [lindex $result_info 3]

        # If we know that the ExprValue has no string value, then
        # use an optimized implementation that avoids nulling
        # out the string value.

        if {!$value_has_null_string} {
            set value_has_null_string [lindex $result_info 2]
        }

        # If operand type is known to be {int}, {boolean}, or {double},
        # then skip emitting of conditional logic that depends on
        # operand type.

        if {$ev_types == {int} || $ev_types == {boolean}} {
            if {$value_has_null_string} {
                append buffer [emitter_statement "$ev.optIntUnaryNotNstr()"]
            } else {
                append buffer [emitter_statement "$ev.optIntUnaryNot()"]
            }
        } elseif {$ev_types == {double}} {
            # The exprUnaryNotOperator() contains an optimized
            # special case for double arguments.
            append buffer \
                [emitter_statement "TJC.exprUnaryNotOperator(interp, $ev)"]
        } else {
            # Type of operand not known at compile time.
            # Emit generic code that will work with
            # ExprValue of any type.

            append buffer \
                [emitter_container_if_start "$ev.isIntType()"]

            if {$value_has_null_string} {
                append buffer [emitter_statement "$ev.optIntUnaryNotNstr()"]
            } else {
                append buffer [emitter_statement "$ev.optIntUnaryNot()"]
            }

            append buffer \
                [emitter_container_if_else] \
                [emitter_statement "TJC.exprUnaryNotOperator(interp, $ev)"] \
                [emitter_container_if_end]
        }
    }

    # result tuple {EXPRVALUE BUFFER EV_TYPES}

    return [list $ev $buffer $ev_types]
}

# Emit a binary operator after evaluating a left
# and right value.
# Return a tuple of {EXPRVALUE BUFFER EV_TYPES}
#
# EXPRVALUE : symbol of type ExprValue
# BUFFER : Buffer that will evaluate EXPRVALUE
# EV_TYPES : List of possible result types
#     Can be {} if types are not known at compile time
#     Can be 1 or more of {int double String}
#     Can be {boolean} for logical operators

proc compileproc_expr_evaluate_emit_binary_operator { op_tuple } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_expr_evaluate_emit_binary_operator \{$op_tuple\}"
#    }

    set buffer ""
    set op_buffer ""

    set type [lindex $op_tuple 0]
    if {$type != {binary operator}} {
        error "expected \{binary operator\} but got type \{$type\}"
    }

    set vtuple [lindex $op_tuple 1]
    set op [lindex $vtuple 0]
    set values [lindex $vtuple 1]

    if {[llength $values] != 2} {
        error "values length should be 2,\
            not [llength $values],\
            values_list was \{$values\}"
    }

    # Figure out which operator this is

    set logic_op 0 ; # true if && or || operator
    set equals_op 0 ; # true if == != eq or ne operator
    set bcompare_op 0 ; # true for any boolean compare op
    set math_op 0 ; # true for + - / * and %
    set bitwise_op 0 ; # true for << >> & | and ^

    switch -exact -- $op {
        "*" {
            set opval TJC.EXPR_OP_MULT
            set math_op 1
        }
        "/" {
            set opval TJC.EXPR_OP_DIVIDE
            set math_op 1
        }
        "%" {
            set opval TJC.EXPR_OP_MOD
            set math_op 1
        }
        "+" {
            set opval TJC.EXPR_OP_PLUS
            set math_op 1
        }
        "-" {
            set opval TJC.EXPR_OP_MINUS
            set math_op 1
        }
        "<<" {
            set opval TJC.EXPR_OP_LEFT_SHIFT
            set bitwise_op 1
        }
        ">>" {
            set opval TJC.EXPR_OP_RIGHT_SHIFT
            set bitwise_op 1
        }
        "<" {
            set opval TJC.EXPR_OP_LESS
            set bcompare_op 1
        }
        ">" {
            set opval TJC.EXPR_OP_GREATER
            set bcompare_op 1
        }
        "<=" {
            set opval TJC.EXPR_OP_LEQ
            set bcompare_op 1
        }
        ">=" {
            set opval TJC.EXPR_OP_GEQ
            set bcompare_op 1
        }
        "==" {
            set opval TJC.EXPR_OP_EQUAL
            set equals_op 1
            set bcompare_op 1
        }
        "!=" {
            set opval TJC.EXPR_OP_NEQ
            set equals_op 1
            set bcompare_op 1
        }
        "&" {
            set opval TJC.EXPR_OP_BIT_AND
            set bitwise_op 1
        }
        "^" {
            set opval TJC.EXPR_OP_BIT_XOR
            set bitwise_op 1
        }
        "|" {
            set opval TJC.EXPR_OP_BIT_OR
            set bitwise_op 1
        }
        "eq" {
            set opval TJC.EXPR_OP_STREQ
            set equals_op 1
            set bcompare_op 1
        }
        "ne" {
            set opval TJC.EXPR_OP_STRNEQ
            set equals_op 1
            set bcompare_op 1
        }
        "&&" -
        "||" {
            # These do not invoke a binary operator method.
            set logic_op 1
        }
        default {
            error "unsupported binary operator \"$op\""
        }
    }

    set left_tuple [lindex $values 0]
    set right_tuple [lindex $values 1]

#    if {$debug} {
#        puts "left_tuple is \{$left_tuple\}"
#        puts "right_tuple is \{$right_tuple\}"
#    }

    set left_peek_tuple [compileproc_expr_peek_operand $left_tuple]
    set right_peek_tuple [compileproc_expr_peek_operand $right_tuple]

#    if {$debug} {
#        puts "left_peek_tuple is \{$left_peek_tuple\}"
#        puts "right_peek_tuple is \{$right_peek_tuple\}"
#    }

    set left_infostr [lindex $left_peek_tuple 3]
    set right_infostr [lindex $right_peek_tuple 3]

    # Generate code to evaluate left and right operands
    # for logic and generic operator cases.

    if {$equals_op} {
        set op_tuple [compileproc_expr_evaluate_emit_binary_equals_operator \
            $op $left_tuple $left_peek_tuple $right_tuple $right_peek_tuple]
    } elseif {$logic_op} {
        set op_tuple [compileproc_expr_evaluate_emit_binary_logic_operator \
            $op $left_tuple $left_peek_tuple $right_tuple $right_peek_tuple]
    } else {
        set op_tuple {}
    }

    if {$op_tuple != {}} {
        set ev1 [lindex $op_tuple 0]
        set op_buffer [lindex $op_tuple 1]
        # operand types will be {} if inlined operator
        # function returns just a symbols and a buffer.
        set left_operand_ev_types [lindex $op_tuple 2]
        set right_operand_ev_types [lindex $op_tuple 3]
    } else {
        # Append code to evaluate left and right values and
        # invoke the binary operator evaluation method.

        set left_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
            $left_tuple]
        set right_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
            $right_tuple]

        set ev1 [lindex $left_eval_tuple 1]
        set ev2 [lindex $right_eval_tuple 1]

        append op_buffer \
            [lindex $left_eval_tuple 2] \
            [lindex $right_eval_tuple 2]

        set left_operand_ev_types [lindex $left_eval_tuple 3 3]
        set right_operand_ev_types [lindex $right_eval_tuple 3 3]

        # Emit TJC binary operator invocation
        append op_buffer [emitter_indent] \
            "TJC.exprBinaryOperator(interp, " $opval ", " $ev1 ", " $ev2 ")\;\n" \
            [compileproc_emit_exprvalue_release $ev2]
    }

    # Printable info describing this operator and
    # the left and right operands:

    append buffer [emitter_indent] \
        "// Binary operator: " $left_infostr " " $op " " $right_infostr "\n" \
        $op_buffer \
        [emitter_indent] \
        "// End Binary operator: " $op "\n"

    # Determine the operator result type.
    # Some operators have only one possible type
    # while others depend on the type of the operand.

    if {$bcompare_op || $logic_op} {
        set ev_types boolean
    } elseif {$math_op} {
        if {$op == "%"} {
            set ev_types int
        } else {
            # Result type for math operators depends
            # on operand types. If either operand
            # is known to be a double then the result
            # is a double. If both operands are known
            # to be of type int, then the result is int.
            # Otherwise, result could be int or double.

            if {$left_operand_ev_types == {int} && \
                $right_operand_ev_types == {int}} {
                set ev_types int
            } elseif {$left_operand_ev_types == {double} || \
                $right_operand_ev_types == {double}} {
                set ev_types double
            } else {
                set ev_types {int double}
            }
        }
    } elseif {$bitwise_op} {
        set ev_types int
    } else {
        set ev_types {int double}
    }

    return [list $ev1 $buffer $ev_types]
}

# FIXME: Cleanup binary operator function by moving logic
# related to emitting of special cases into operator
# specific function. Then, these operator specific
# functions can be invoked on a per-operator basis.

if {0} {

MATH (* / + -) (no strings allowed)

MATH (%) (only int)

BIT (<< >> & ^ |) (only int)

COMPARE (< > <= >=) (any, operands converted)

EQUALS (== != eq ne) (any, eq ne not converted)

LOGIC (&& ||) (no strings, no conversion)

}

# Emit code for the ==, !=, eq, and ne operators.
# These equals operators accept any type.
# The == and != operators will convert operands
# so that the types match. 
# Return a tuple of {EXPRVALUE BUFFER}
# or {} if the default binary operator method
# should be invoked.
#
# EXPRVALUE : symbol of type ExprValue
# BUFFER : Buffer that will evaluate EXPRVALUE

proc compileproc_expr_evaluate_emit_binary_equals_operator {
        op left_tuple left_peek_tuple right_tuple right_peek_tuple } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_expr_evaluate_emit_binary_equals_operator $op \
#            \{$left_tuple\} \{$left_peek_tuple\} \{$right_tuple\} \{$right_peek_tuple\}"
#    }

    if {$op != "==" && $op != "!=" && $op != "eq" && $op != "ne"} {
        error "operator \"$op\" is not an equals operator"
    }

    set op_buffer ""

    # Check for the common special case of comparing a
    # TclObject to the empty string. This check will
    # work for a variable that has been resolved into
    # a TclObject or a command that has been resolved
    # into a TclObject.

    set left_value_info_type [lindex $left_peek_tuple 0]
    set left_value_info_value [lindex $left_peek_tuple 1]

    set right_value_info_type [lindex $right_peek_tuple 0]
    set right_value_info_value [lindex $right_peek_tuple 1]

# FIXME: Currently, the IS_STRING_VALUE_NULL value is ignored
# in the right_peek_tuple here??

    set is_tclobject_string_compare 0

    # These flags are set to 1 to indicate that a specific
    # optimized case has been found.
    set opt_tclobject_empty_string_compare 0
    set opt_tclobject_string_compare 0
    set opt_tclobject_to_tclobject_string_compare 0
    set opt_tclobject_to_exprvalue_string_compare 0

    # Set only when one operand is a TclObject and the other is not
    set is_left_operand_tclobject 0

    # Set to 1 when any of the special case optimizations
    # is detected.

    set is_any_optimized_op 0

#    if {$debug} {
#        puts "pre empty_string_compare check: op is $op"
#        puts "left_value_info_type $left_value_info_type"
#        puts "left_value_info_value ->$left_value_info_value<-"
#        puts "right_value_info_type $right_value_info_type"
#        puts "right_value_info_value ->$right_value_info_value<-"
#    }

    # Check for a string compare like {$v == "foo"} or {"foo" == $v}
 
    if {($left_value_info_type == "TclObject" && \
            $right_value_info_type == "String") || \
            ($left_value_info_type == "String" && \
                $right_value_info_type == "TclObject")} {
        set is_tclobject_string_compare 1
        set is_left_operand_tclobject [expr {$left_value_info_type == "TclObject"}]
    }

    if {$is_tclobject_string_compare} {
        if {$is_left_operand_tclobject} {
            set str $right_value_info_value
        } else {
            set str $left_value_info_value
        }
        # Remove double quotes and test for empty string compare
        set string_literal [string range $str 1 end-1]
        if {$string_literal eq ""} {
            set is_any_optimized_op 1
            set opt_tclobject_empty_string_compare 1
        } else {
            # If constant string is not empty, use
            # optimization for a non-empty string compare.
            set is_any_optimized_op 1
            set opt_tclobject_string_compare 1
        }
    }

    # Check for inlined 'eq' and 'ne' logic when +inline-expr
    # is enabled and no other optimized case is found.

    if {!$is_any_optimized_op && \
            $_compileproc(options,expr_inline_operators) && \
            ($op == "eq" || $op == "ne")} {

        if {$left_value_info_type == "TclObject" && \
                $right_value_info_type == "TclObject"} {
            # Invocation like [expr {$v1 eq $v2}]
            set is_any_optimized_op 1
            set opt_tclobject_to_tclobject_string_compare 1
        } elseif {$left_value_info_type == "TclObject" || \
                $right_value_info_type == "TclObject"} {
            # Invocation like [expr {$v eq 1}] where one operand
            # is a TclObject and the other is a ExprValue.
            set is_any_optimized_op 1
            set opt_tclobject_to_exprvalue_string_compare 1
            set is_left_operand_tclobject [expr {$left_value_info_type == "TclObject"}]
        } else {
            # Invocation like [expr {1 eq (1 + 0)}] where
            # both operands are ExprValue types. This is handled
            # by the default invocation of the binary operator.
        }
    }

#    if {$debug} {
#        puts "post empty_string_compare check: op is $op"
#        puts "is_tclobject_string_compare $is_tclobject_string_compare"
#        puts "is_left_operand_tclobject $is_left_operand_tclobject"

#        puts "is_any_optimized_op $is_any_optimized_op"
#        puts "opt_tclobject_empty_string_compare $opt_tclobject_empty_string_compare"
#        puts "opt_tclobject_string_compare $opt_tclobject_string_compare"
#        puts "opt_tclobject_to_tclobject_string_compare $opt_tclobject_to_tclobject_string_compare"
#        puts "opt_tclobject_to_exprvalue_string_compare $opt_tclobject_to_exprvalue_string_compare"
#    }

    # Check for special flag used only during code generation testing
    if {[info exists _compileproc(options,expr_no_string_compare_optimizations)]} {
        set opt_tclobject_string_compare 0
        set opt_tclobject_empty_string_compare 0
        set opt_tclobject_to_tclobject_string_compare 0
        set opt_tclobject_to_exprvalue_string_compare 0
        set is_any_optimized_op 0
#        if {$debug} {
#            puts "expr_no_string_compare_optimizations flag set"
#        }
    }

    # Generate code for specific optimized logic, or
    # use the default operator invocation.

    if {$opt_tclobject_empty_string_compare} {
        # Special case for: expr {$obj == ""}. Generate
        # an eval buffer with a TclObject result for
        # the object operand.

        if {$is_left_operand_tclobject} {
            set left_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
                $left_tuple {rtype TclObject}]
            set tclobject_sym [lindex $left_eval_tuple 1]
            append op_buffer [lindex $left_eval_tuple 2]
        } else {
            set right_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
                $right_tuple {rtype TclObject}]
            set tclobject_sym [lindex $right_eval_tuple 1]
            append op_buffer [lindex $right_eval_tuple 2]
        }
        set tmpsymbol [compileproc_tmpvar_next]
        if {$op == "!=" || $op == "ne"} {
            # Negate equality test
            set negate true
        } else {
            set negate false
        }
        append op_buffer [compileproc_emit_exprvalue_get $tmpsymbol "" ""] \
            [emitter_indent] \
            "TJC.exprEqualsEmptyString(" \
                $tmpsymbol ", " $tclobject_sym ", " $negate \
            ")" "\;\n"
        set ev1 $tmpsymbol
    } elseif {$opt_tclobject_string_compare} {
        # Special case for: expr {$obj == "foo"}. The
        # string that will be compared is non-empty.
        # Generate an eval buffer for the object
        # operand with the TclObject type. The other
        # operand is a String.

        # Use string literal from first left/right type test, it has
        # already been escaped by the emitter layer.
        set jstr "\"$string_literal\""

        if {$is_left_operand_tclobject} {
            set left_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
                $left_tuple {rtype TclObject}]
            set tclobject_sym [lindex $left_eval_tuple 1]
            append op_buffer [lindex $left_eval_tuple 2]
        } else {
            set right_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
                $right_tuple {rtype TclObject}]
            set tclobject_sym [lindex $right_eval_tuple 1]
            append op_buffer [lindex $right_eval_tuple 2]
        }

        set boolean_tmpsymbol [compileproc_tmpvar_next]
        if {$op == "!=" || $op == "ne"} {
            # Negate equality test
            set not "! "
        } else {
            set not ""
        }
        # Emit a call to String.equals(Object) which will do a pointer
        # compare before trying to do a character by character compare.
        append op_buffer [emitter_indent] \
            "boolean " $boolean_tmpsymbol \
            " = " $not $tclobject_sym ".toString().equals(" $jstr ")\;\n"
        set tmpsymbol [compileproc_tmpvar_next]
        append op_buffer [compileproc_emit_exprvalue_get $tmpsymbol \
            boolean $boolean_tmpsymbol]
        set ev1 $tmpsymbol
    } elseif {$opt_tclobject_to_tclobject_string_compare} {
        # Special case for: [expr {$v1 eq $v2}].
        # No need to parse TclObject as a number or
        # invoke the binary operator method.
        # Generate eval buffers for the left and
        # right operands with a TclObject result type.

        set left_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
            $left_tuple {rtype TclObject}]
        set left_tclobject_sym [lindex $left_eval_tuple 1]
        append op_buffer [lindex $left_eval_tuple 2]

        # Save string rep of left operand, this TclObject could
        # be modified when the right operand is evaluated so
        # it is critical that we grab the String rep now.

        set left_str_sym [compileproc_tmpvar_next]
        append op_buffer [emitter_statement \
            "String $left_str_sym = $left_tclobject_sym.toString()"]

        set right_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
            $right_tuple {rtype TclObject}]
        set right_tclobject_sym [lindex $right_eval_tuple 1]
        append op_buffer [lindex $right_eval_tuple 2]

        # Save string rep of right operand

        set right_str_sym [compileproc_tmpvar_next]
        append op_buffer [emitter_statement \
            "String $right_str_sym = $right_tclobject_sym.toString()"]

        set boolean_tmpsymbol [compileproc_tmpvar_next]
        if {$op == "ne"} {
            # Negate equality test
            set not "! "
        } else {
            set not ""
        }
        # Emit a call to String.equals(Object) which will do a pointer
        # compare before trying to do a character by character compare.
        append op_buffer [emitter_indent] \
            "boolean " $boolean_tmpsymbol \
            " = " $not $left_str_sym ".equals(" $right_str_sym ")\;\n"
        set tmpsymbol [compileproc_tmpvar_next]
        append op_buffer [compileproc_emit_exprvalue_get $tmpsymbol \
            boolean $boolean_tmpsymbol]
        set ev1 $tmpsymbol
    } elseif {$opt_tclobject_to_exprvalue_string_compare} {
        # Special case for: [expr {$v1 eq 1}].
        # No need to parse TclObject as a number or
        # invoke the binary operator method.
        # Generate the object operand with a TclObject
        # type and compare it to the string value
        # of the ExprValue operand.

        if {$is_left_operand_tclobject} {
            # Generate left hand operand as a TclObject

            set left_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
                $left_tuple {rtype TclObject}]
            set left_tclobject_sym [lindex $left_eval_tuple 1]
            append op_buffer [lindex $left_eval_tuple 2]

            # Save string rep of left operand, the TclObject could
            # be modified when the right operand is evaluated.

            set left_str_sym [compileproc_tmpvar_next]
            append op_buffer [emitter_statement \
                "String $left_str_sym = $left_tclobject_sym.toString()"]

            # Generate right hand operand as an ExprValue

            set right_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
                $right_tuple]
            set ev2 [lindex $right_eval_tuple 1]
            append op_buffer [lindex $right_eval_tuple 2]

            set right_str_sym "$ev2.getStringValue()"

            set ev $ev2
        } else {
            # Right operand must be the TclObject

            # Generate left hand operand as an ExprValue

            set left_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
                $left_tuple]
            set ev1 [lindex $left_eval_tuple 1]
            append op_buffer [lindex $left_eval_tuple 2]

            # Generate right hand operand as a TclObject

            set right_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
                $right_tuple {rtype TclObject}]
            set right_tclobject_sym [lindex $right_eval_tuple 1]
            append op_buffer [lindex $right_eval_tuple 2]

            # The right operand does not need to be saved as
            # a String tmp since no further variable evaluaion
            # will be done before the compare operation.

            set left_str_sym "$ev1.getStringValue()"
            set right_str_sym "$right_tclobject_sym.toString()"

            set ev $ev1
        }

        set boolean_tmpsymbol [compileproc_tmpvar_next]
        if {$op == "ne"} {
            # Negate equality test
            set not "! "
        } else {
            set not ""
        }
        # Emit a call to String.equals(Object) which will do a pointer
        # compare before trying to do a character by character compare.
        append op_buffer [emitter_indent] \
            "boolean " $boolean_tmpsymbol \
            " = " $not $left_str_sym ".equals(" $right_str_sym ")\;\n"

        # Reuse the ExprValue for the result, this will also
        # release the ExprValue.

        append op_buffer [emitter_indent] \
            $ev ".setIntValue(" $boolean_tmpsymbol ")\;\n"

        set ev1 $ev
    } else {
        return {}
    }

    return [list $ev1 $op_buffer]
}

# Emit code for the || and && operators.
# These operators evaluate operands as
# booleans and can be used for short
# circut logic.
# Return a tuple of {EXPRVALUE BUFFER}
# or {} if the default binary operator method
# should be invoked.
#
# EXPRVALUE : symbol of type ExprValue
# BUFFER : Buffer that will evaluate EXPRVALUE

proc compileproc_expr_evaluate_emit_binary_logic_operator {
        op left_tuple left_peek_tuple right_tuple right_peek_tuple } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_expr_evaluate_emit_binary_logic_operator $op \
#            \{$left_tuple\} \{$left_peek_tuple\} \{$right_tuple\} \{$right_peek_tuple\}"
#    }

    if {$op != "||" && $op != "&&"} {
        error "operator \"$op\" is not a logic operator"
    }

    set left_infostr [lindex $left_peek_tuple 3]
#    set right_infostr [lindex $right_peek_tuple 3]

    set left_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
        $left_tuple]
    set right_eval_tuple [compileproc_expr_evaluate_emit_exprvalue \
        $right_tuple]

    set ev1 [lindex $left_eval_tuple 1]
    set ev2 [lindex $right_eval_tuple 1]

    append op_buffer [lindex $left_eval_tuple 2]
    if {$op == "&&"} {
        set not ""
    } elseif {$op == "||"} {
        set not "!"
    } else {
        error "unmatched logic_op \"$op\""
    }

# FIXME: Should be able to evaluate this logic using only
# a single ExprValue and a single boolean condition that
# lives on the stack. Only implement when inline-operators
# flags is true so that old code is still emitted.

    append op_buffer \
        [emitter_indent] \
        "if (" $not $ev1 ".getBooleanValue(interp)) \{\n" \
        [lindex $right_eval_tuple 2] \
        [emitter_indent] \
        $ev1 ".setIntValue(" $ev2 ".getBooleanValue(interp))\;\n" \
        [compileproc_emit_exprvalue_release $ev2] \
        [emitter_indent] \
        "\} else \{\n"

    set else_value [expr {($not == "") ? 0 : 1}]
    append op_buffer \
        [emitter_indent] \
        $ev1 ".setIntValue(" $else_value ")\;\n" \
        [emitter_indent] \
        "\} // End if: " $not $left_infostr "\n"

    return [list $ev1 $op_buffer]
}

# Emit a ternary operator like ($b ? 1 : 0)
# Return a tuple of {EXPRVALUE BUFFER EV_TYPES}
#
# EXPRVALUE : symbol of type ExprValue
# BUFFER : Buffer that will evaluate EXPRVALUE
# EV_TYPES : List of possible result types
#     Can be {} if types are not known at compile time
#     Can be 1 or more of {int double String}
#     Can be {boolean} if both values are boolean values

proc compileproc_expr_evaluate_emit_ternary_operator { op_tuple } {
    global _compileproc

    #puts "compileproc_expr_evaluate_emit_ternary_operator \{$op_tuple\}"

    set buffer ""
    set op_buffer ""

    set vtuple [lindex $op_tuple 1]
    set op [lindex $vtuple 0]
    set values [lindex $vtuple 1]

    if {[llength $values] != 3} {
        error "values length should be 3,\
            not [llength $values],\
            values_list was \{$values\}"
    }

    # Emit code to evaluate condition value

    set cond_tuple [lindex $values 0]
    set cond_eval_tuple [compileproc_expr_evaluate_emit_exprvalue $cond_tuple]
    set cond_infostr [lindex $cond_eval_tuple 0]
    set ev1 [lindex $cond_eval_tuple 1]
    set cond_ev_types [lindex $cond_eval_tuple 3 3]

#    puts "cond_ev_types \{$cond_ev_types\}"

    append op_buffer [lindex $cond_eval_tuple 2]

    # Emit code to evaluate true value

    set true_tuple [lindex $values 1]
    set true_eval_tuple [compileproc_expr_evaluate_emit_exprvalue $true_tuple]
    set true_infostr [lindex $true_eval_tuple 0]
    set ev2 [lindex $true_eval_tuple 1]
    set true_ev_types [lindex $true_eval_tuple 3 3]

#    puts "true_ev_types \{$true_ev_types\}"

    # FIXME: If ev1 is known to be of type boolean or int at compile
    # time, then an optimized call can be used instead of
    # getBooleanValue() here. Invoke inlined getIntValue()
    # and then do a (val ? true : false) branch to set
    # a local boolean value. This is also done in the setResult logic!

    # if {$true_ev_types = {int}} ...

    append op_buffer \
        [emitter_indent] \
        "if (" $ev1 ".getBooleanValue(interp)) \{\n" \
        [lindex $true_eval_tuple 2]

    if {$_compileproc(options,expr_value_stack)} {
        # Copy ev2 into ev1, then release ev2
        append op_buffer \
            [emitter_indent] \
            $ev1 ".setValue(" $ev2 ")" "\;\n" \
            [compileproc_emit_exprvalue_release $ev2]
    } else {
        # Release ev1, then assign ev2 to ev1
        append op_buffer \
            [compileproc_emit_exprvalue_release $ev1] \
            [emitter_indent] \
            $ev1 " = " $ev2 "\;\n"
    }

    append op_buffer \
        [emitter_indent] \
        "\} else \{\n"

    # Emit code to evaluate false value

    set false_tuple [lindex $values 2]
    set false_eval_tuple [compileproc_expr_evaluate_emit_exprvalue $false_tuple]
    set false_infostr [lindex $false_eval_tuple 0]
    set ev3 [lindex $false_eval_tuple 1]
    set false_ev_types [lindex $false_eval_tuple 3 3]

#    puts "false_ev_types \{$false_ev_types\}"

    append op_buffer \
        [lindex $false_eval_tuple 2]

    if {$_compileproc(options,expr_value_stack)} {
        # Copy ev3 into ev1, then release ev3
        append op_buffer \
            [emitter_indent] \
            $ev1 ".setValue(" $ev3 ")" "\;\n" \
            [compileproc_emit_exprvalue_release $ev3]
    } else {
        # Release ev1, then assign ev3 to ev1
        append op_buffer \
            [compileproc_emit_exprvalue_release $ev1] \
            [emitter_indent] \
            $ev1 " = " $ev3 "\;\n"
    }

    append op_buffer \
        [emitter_indent] \
        "\}\n"

# FIXME: When stack values is enabled, is it possible to process
# all of these ExprValues using just 1 symbol from the stack?
# It should be if the boolean condition is saved in a boolean
# on the stack instead of in an ExprValue.

    append buffer \
        [emitter_indent] \
        "// Ternary operator: " $cond_infostr " ? " $true_infostr " : " $false_infostr "\n" \
        $op_buffer \
        [emitter_indent] \
        "// End Ternary operator: ?\n"

    # If types for both true and false values are known
    # at compile time, then the type for the whole
    # expression is known at compile time. This
    # works for int, double, String, and boolean.
    # If {int double} and a specific type then
    # return {int double}.

    set true_size [llength $true_ev_types]
    set false_size [llength $false_ev_types]

    set true_is_int [expr {$true_size == 1 && $true_ev_types == {int}}]
    set true_is_double [expr {$true_size == 1 && $true_ev_types == {double}}]
    set true_is_int_double [expr {$true_ev_types == {int double}}]

    set false_is_int [expr {$false_size == 1 && $false_ev_types == {int}}]
    set false_is_double [expr {$false_size == 1 && $false_ev_types == {double}}]
    set false_is_int_double [expr {$false_ev_types == {int double}}]

    if {$true_size == 1 && $false_size == 1 && \
        ($true_ev_types eq $false_ev_types)} {
        # ( ... ? 1 : 0 )
        # ( ... ? 1.0 : 0.0 )
        # ( ... ? true : false )
        # ( ... ? "a" : "b" )
        set ev_types $true_ev_types
    } elseif {($true_is_int && $false_is_double) || \
        ($true_is_double && $false_is_int)} {
        # ( ... ? 0 : 0.0 )
        # ( ... ? 0.0 : 1 )
        set ev_types {int double}
    } elseif {($true_is_int_double && \
            ($false_is_int || $false_is_double)) || \
        ($false_is_int_double && \
            ($true_is_int || $true_is_double)) || \
        ($true_is_int_double && $false_is_int_double)} {
        # ( ... ? 1 : abs($v) )
        # ( ... ? 1.0 : abs($v) )
        # ( ... ? abs($v) : 0 )
        # ( ... ? abs($v) : 1.0 )
        # ( ... ? abs($v) : abs($v) )
        set ev_types {int double}
    } else {
        # Specific type not known at compile time
        set ev_types {}
    }

    return [list $ev1 $buffer $ev_types]
}

# Emit a math function like pow(2,2).
# Return a tuple of {EXPRVALUE BUFFER EV_TYPES}
#
# EXPRVALUE : symbol of type ExprValue
# BUFFER : Buffer that will evaluate EXPRVALUE
# EV_TYPES : List of possible result types {int double String} or {boolean}

proc compileproc_expr_evaluate_emit_math_function { op_tuple } {
#    set debug 0

#    if {$debug} {
#        puts "compileproc_expr_evaluate_emit_math_function \{$op_tuple\}"
#    }

    set buffer ""
    set ev_types {}

    # There is no checking of the number of arguments to
    # a math function. We just pass in however many
    # the user passed and expect that an exception will
    # be raised if the wrong number of args were passed.

    set vtuple [lindex $op_tuple 1]
    set funcname [lindex $vtuple 0]
    set values [lindex $vtuple 1]

    set infostrs [list]
    set evsyms [list]
    set buffers [list]
    set rtuples [list]

    # Emit ExprValue for each argument to the math function
    set len [llength $values]
    for {set i 0} {$i < $len} {incr i} {
        set tuple [lindex $values $i]
        set eval_tuple [compileproc_expr_evaluate_emit_exprvalue $tuple]
        foreach {infostr ev eval_buffer rtuple} $eval_tuple {break}

        lappend infostrs $infostr
        lappend evsyms $ev
        lappend buffers $eval_buffer
        lappend rtuples $rtuple
    }

    append buffer [emitter_indent] \
        "// Math function: " $funcname
    foreach infostr $infostrs {
        append buffer " " $infostr
    }
    append buffer "\n"

    # Append code to create ExprValue objects and assign to values array

    for {set i 0} {$i < $len} {incr i} {
        set eval_buffer [lindex $buffers $i]
        append buffer $eval_buffer
    }

    # Invoke runtime access to math function. Use an
    # optimized runtime method for int() and double().

    if {$len == 1 && ($funcname == "int" || $funcname == "double")} {
        set ev [lindex $evsyms 0]
        if {$funcname == "int"} {
            set tjc_func "TJC.exprIntMathFunction"
            set ev_types int
        } else {
            set tjc_func "TJC.exprDoubleMathFunction"
            set ev_types double
        }
        append buffer [emitter_indent] \
            ${tjc_func} "(interp, " $ev ")\;\n"
        set result_tmpsymbol $ev
    } else {
        if {$len > 0} {
            set values_tmpsymbol [compileproc_tmpvar_next]
            append buffer [emitter_indent] \
                "ExprValue\[\] " $values_tmpsymbol " = new ExprValue\[" $len "\]\;\n"

            for {set i 0} {$i < $len} {incr i} {
                set ev [lindex $evsyms $i]
                append buffer [emitter_indent] \
                    $values_tmpsymbol "\[" $i "\] = " $ev "\;\n"
            }
        } else {
            set values_tmpsymbol null
        }

        set expr_value_stack $::_compileproc(options,expr_value_stack)

        # When expr values are on the stack, optimize
        # by passing null as the result argument and
        # using values[0] directly as the result.
        # The only exception is when invoking rand()
        # which takes no arguments.

        if {$expr_value_stack && ($len > 0)} {
            set result_tmpsymbol null
        } else {
            set result_tmpsymbol [compileproc_tmpvar_next]
            append buffer [compileproc_emit_exprvalue_get $result_tmpsymbol "" ""]
        }

        set jstr [emitter_backslash_tcl_string $funcname]

        append buffer [emitter_indent] \
            "TJC.exprMathFunction(interp, \"" \
            $jstr "\", " \
            $values_tmpsymbol ", " \
            $result_tmpsymbol \
            ")" "\;\n"

        # Release expr values in reverse allocation order

        if {$len > 0} {
            set i $len
            incr i -1
            for {} {$i >= 0} {incr i -1} {
                set ev [lindex $evsyms $i]

                # Use values[0] if it contains the result

                if {$i == 0 && $result_tmpsymbol == "null"} {
                    set result_tmpsymbol $ev
                    break
                }

                append buffer \
                    [compileproc_emit_exprvalue_release $ev]
            }
        }

        # Determine return ev_types, int and double are handled above

        switch -- $funcname {
            "abs" {
                # Result type of abs() should match the
                # input type, assuming the input type
                # is known at compile time.

                if {$len == 1} {
                    set rtuple [lindex $rtuples 0]
                    set result_type [lindex $rtuple 0]
                    set result_ev_types [lindex $rtuple 3]
                    
                    if {$result_type == "int literal" || \
                        ($result_type == "ExprValue" && \
                            $result_ev_types == "int")} {
                        set ev_types int
                    } elseif {$result_type == "double literal" || \
                        ($result_type == "ExprValue" && \
                            $result_ev_types == "double")} {
                        set ev_types double
                    } else {
                        # Argument type not known at compile time
                        set ev_types {int double}
                    }
                }
            }
            "round" {
                set ev_types int
            }
            "acos" -
            "asin" -
            "atan" -
            "atan2" -
            "ceil" -
            "cos" -
            "cosh" -
            "exp" -
            "floor" -
            "fmod" -
            "hypot" -
            "log" -
            "log10" -
            "pow" -
            "rand" -
            "sin" -
            "sinh" -
            "sqrt" -
            "srand" -
            "tan" -
            "tanh" {
                # All these math functions return double
                set ev_types double
            }
            default {
                # No-op for unknown math function
            }
        }
    }

    append buffer [emitter_indent] \
        "// End Math function: " $funcname "\n"

    return [list $result_tmpsymbol $buffer $ev_types]
}

# Determine information about an expr operand by peeking
# at the toplevel of a exprvalue tuple. For example,
# the utility can be used to determine if an operand
# is a constant int literal before emitting code.

proc compileproc_expr_peek_operand { tuple } {
    set debug 0

#    if {$debug} {
#        puts "compileproc_expr_peek_operand \{$tuple\}"
#    }

    set peek_tuple [compileproc_expr_evaluate_emit_exprvalue $tuple {peek 1}]

#    if {$debug} {
#        puts "peek tuple \{$peek_tuple\}"
#    }

    # peek_tuple is {INFOSTR SYMBOL BUFFER RESULT_TUPLE}
    #
    # RESULT_TUPLE is {TYPE SYMBOL IS_STRING_VALUE_NULL}

    set infostr [lindex $peek_tuple 0]
    set buffer [lindex $peek_tuple 2]
    if {$buffer != ""} {
        error "expected empty eval buffer but got : \"$buffer\""
    }
    set result_info [lindex $peek_tuple 3]
    # Replace EV_TYPES with INFOSTR. EV_TYPES is always
    # going to be {} in peek mode.
    set result_tuple [lrange $result_info 0 2]

    # result_info tuple {TYPE SYMBOL IS_STRING_VALUE_NULL EV_TYPES}
    #
    # return tuple {TYPE SYMBOL IS_STRING_VALUE_NULL INFOSTR}

    lappend result_tuple $infostr
    return $result_tuple
}

# This method is invoked to emit code that will evaluate
# an expr value. This evaluation could result in a
# a simple value, evaluation of a subexpression,
# evaluation of nested commands, and so on. Typically,
# the caller will invoke this method and then operate
# on the result symbol of type ExprValue. It is also
# possible to indicate the preferred return value
# by passing flags in the genflags tuple.

# tuple : An expr value tuple
#
# genflags : A tuple containing {KEY VALUE} pairs
#     that control how this method functions and
#     what the evaluation buffer returns. The
#     possible flag values are:
#
#     peek : A boolean value that indicates a peek operation.
#            A peek operation does not generate code,
#            it examines the tuple and returns
#            information about the operand.
#
#     rtype : Indicates the preferred result type. Type can
#             be either ExprValue (the default) or TclObject.
#
#     nostr : Indicates that string value for an ExprValue should
#             not be generated. For example, an int literal 0x1
#             would normally have a value of 1 and a string
#             value of "0x1". If nostr is set then the string
#             value will be null.
#
# Return tuple {INFOSTR SYMBOL BUFFER RESULT_INFO}

proc compileproc_expr_evaluate_emit_exprvalue { tuple {genflags {}} } {
#    set debug 0

#    if {$debug} {
#        puts "compileproc_expr_evaluate_emit_exprvalue \{$tuple\} \{$genflags\}"
#    }

    # Examine genflags

    set no_exprvalue_for_tclobject 0
    set no_str_for_exprvalue 0
    set gencode 1

    if {([llength $genflags] % 2) != 0} {
        error "genflags must have even number of flag value elements : \{$genflags\}"
    }
    foreach {flag value} $genflags {
        switch -- $flag {
            "rtype" {
                if {$value == "ExprValue"} {
                    # No-op
                } elseif {$value == "TclObject"} {
                    set no_exprvalue_for_tclobject 1
                } else {
                    error "unsupported rtype \"$value\""
                }
            }
            "nostr" {
                if {$value} {
                    set no_str_for_exprvalue 1
                }
            }
            "peek" {
                if {$value} {
                    set gencode 0
                }
            }
            default {
                error "unsupported flag \"$flag\""
            }
        }
    }

    set buffer ""
    set infostr ""

    set result_type ""
    set result_symbol ""
    set value_symbol ""
    set srep ""

    set type [lindex $tuple 0]
    set value [lindex $tuple 1]

    # Could be 1 or more of {int double String}
    # or {boolean}. An empty ev_types is
    # the same as {int double String}.
    set ev_types {}

    switch -exact -- $type {
        {constant int} -
        {constant double} {
            # Compiler knows this tuple represents a literal
            # int or double value with no string rep. The compiler
            # uses this logic to implement compiled constants
            # for unary operator results like "-1" or "~0".

            if {$type eq {constant int}} {
                set result_type "int literal"
                lappend ev_types int
            } else {
                set result_type "double literal"
                lappend ev_types double
            }
            set result_symbol $value
            set infostr $value
            set srep null
        }
        {constant boolean} -
        {constant string} -
        {constant braced string} -
        {constant} {
            if {$type == {constant boolean}} {
                # Don't need shortcut for constant boolean value
                set value [lindex $tuple 2]
            }
            # Generate Java string before possible backslash subst
            set jvalue [emitter_backslash_tcl_string $value]

            # A double quoted constant string could contain
            # backslashes like \n or \t at this point.
            # Subst them while testing if number is a
            # integer or double.
            if {$type == {constant string}} {
                set value [subst -nocommands -novariables \
                    $value]
            }
            set is_integer [compileproc_string_is_java_integer $value]
            set is_double [compileproc_string_is_java_double $value]
            if {$is_integer || $is_double} {
                set tuple [compileproc_parse_value $value]
                set stringrep_matches [lindex $tuple 0]
                set parsed_number [lindex $tuple 1]
                set printed_number [lindex $tuple 2]

                if {$stringrep_matches} {
                    set srep null
                } else {
                    set srep "\"$jvalue\""
                }
                if {$is_integer} {
                    set result_type "int literal"
                } else {
                    set result_type "double literal"
                }
                set result_symbol $printed_number
                set infostr $printed_number
            } else {
                set result_type "String"
                set result_symbol "\"$jvalue\""
                set infostr $result_symbol

                # FIXME: Create a better printable string
                # scanning function that can determine which
                # characters in a Tcl string can't appear in
                # a Java comment and does not print those.
                # This method will print "\n" right now,
                # which is likely ok but banned by the
                # earlier printing checks.
            }
        }
        {variable scalar} -
        {variable array} {
            # Evaluate variable
            if {$type == {variable scalar}} {
                set varname $value
                set vinfo [list scalar $value]
            } else {
                set varname "[lindex $value 1 0](...)"
                set vinfo $value
            }

            set result_type "TclObject"
            set infostr "\$$varname"

            if {$gencode} {
                set tmpsymbol [compileproc_tmpvar_next]
                append buffer \
                    [compileproc_emit_variable $tmpsymbol $vinfo]
                set result_symbol $tmpsymbol
            }
        }
        {unary operator} {
            set infostr "()"
            set result_type "ExprValue"
            # All unary operators null the string value
            set srep "null"

            if {$gencode} {
                set eval_tuple [compileproc_expr_evaluate_emit_unary_operator \
                    $tuple]
                set result_symbol [lindex $eval_tuple 0]
                append buffer [lindex $eval_tuple 1]
                set ev_types [lindex $eval_tuple 2]
            }
        }
        {binary operator} {
            set infostr "()"
            set result_type "ExprValue"

            if {$gencode} {
                set eval_tuple [compileproc_expr_evaluate_emit_binary_operator \
                    $tuple]
                set result_symbol [lindex $eval_tuple 0]
                append buffer [lindex $eval_tuple 1]
                set ev_types [lindex $eval_tuple 2]
            }
        }
        {ternary operator} {
            set infostr "(?:)"
            set result_type "ExprValue"

            if {$gencode} {
                set eval_tuple [compileproc_expr_evaluate_emit_ternary_operator \
                    $tuple]
                set result_symbol [lindex $eval_tuple 0]
                append buffer [lindex $eval_tuple 1]
                set ev_types [lindex $eval_tuple 2]
            }
        }
        {math function} {
            set infostr "math()"
            set result_type "ExprValue"

            if {$gencode} {
                set eval_tuple [compileproc_expr_evaluate_emit_math_function \
                    $tuple]
                set result_symbol [lindex $eval_tuple 0]
                append buffer [lindex $eval_tuple 1]
                set ev_types [lindex $eval_tuple 2]
            }
        }
        {command operand} {
            set ccmds $value
#            if {$debug} {
#                puts "container commands for command operand are \{$ccmds\}"
#            }
            if {$ccmds == {}} {
                set infostr "\[\]"
                set result_type "String"
                set result_symbol "\"\"" ; # empty Java String
            } else {
                set infostr "\[...\]"
                set result_type "TclObject"

                if {$gencode} {
                    foreach ckey $ccmds {
                        append buffer [compileproc_emit_invoke $ckey]
                    }
                    set tmpsymbol [compileproc_tmpvar_next]
                    append buffer [emitter_indent] \
                        "TclObject " $tmpsymbol " = interp.getResult()\;\n"
                    set result_symbol $tmpsymbol
                }
            }
        }
        {word operand} {
            # A word is made up of command, string, and variable elements.
            set types [lindex $value 0]
            set values [lindex $value 1]
            set cmap [lindex $value 2]
#            if {$debug} {
#                puts "expr value is a {word operand}:"
#                puts "types is \{$types\}"
#                puts "values is \{$values\}"
#                puts "cmap is \{$cmap\}"
#            }

            set winfo $values

            set result_type "TclObject"
            set infostr "\"...\""

            if {$gencode} {
                set tmpsymbol [compileproc_tmpvar_next]
                append buffer \
                    [compileproc_emit_word $tmpsymbol $winfo true]
                set result_symbol $tmpsymbol
            }
        }
        default {
            error "unhandled type \"$type\""
        }
    }

#    if {$debug} {
#        puts "result_type is \"$result_type\""
#        puts "result_symbol is ->$result_symbol<-"
#        puts "srep is \"$srep\""
#    }

    # Invoke method to get an ExprValue and initialize
    # it to an int, double, String, TclObject value.

    if {$no_exprvalue_for_tclobject && $result_type == "TclObject"} {
        # Return TclObject symbol instead of creating an ExprValue
        set value_symbol $result_symbol
    } elseif {$result_type != "ExprValue"} {
        if {$gencode} {
            set tmpsymbol [compileproc_tmpvar_next]
            set value_symbol $tmpsymbol
        }
    } else {
        set value_symbol $result_symbol
    }

    if {$no_exprvalue_for_tclobject && $result_type == "TclObject"} {
       # No-op
    } elseif {$result_type == "int literal" || $result_type == "double literal"} {
        set type "int"
        if {$result_type == "double literal"} {
            set type "double"
        }
        lappend ev_types $type
        if {$no_str_for_exprvalue} {
            set srep "null"
        }
        if {$gencode} {
            append buffer \
                [compileproc_emit_exprvalue_get $tmpsymbol $type $result_symbol $srep]
        }
    } elseif {$result_type == "String" || $result_type == "TclObject"} {
        if {$result_type == "String"} {
            lappend ev_types "String"
        }
        if {$gencode} {
            append buffer \
                [compileproc_emit_exprvalue_get $tmpsymbol $result_type $result_symbol]
        }
    } elseif {$result_type == "ExprValue"} {
        # No-op
    } else {
        error "unknown result type \"$result_type\""
    }

    # Tuple of {TYPE SYMBOL IS_STRING_VALUE_NULL EV_TYPES}
    set result_info [list $result_type $result_symbol \
        [expr {$srep == "null"}] \
        $ev_types \
        ]

    # Return tuple {INFOSTR SYMBOL BUFFER RESULT_INFO}

    return [list $infostr $value_symbol $buffer $result_info]
}

# This method will return a buffer that declares a symbol
# of type ExprValue and allocates an ExprValue object
# initialized to the passed in value. Pass the empty string
# in the value argument to get an uninitialized ExprValue.

# symbol: The ExprValue symbol to be declared
# value_type: Type of next argument, either int, double,
#     boolean, TclObject, String, or "" (for no init)
# value:  A value of type indicated by value_type, or ""
# srep:   String rep of value (can be null), or "".

proc compileproc_emit_exprvalue_get { symbol value_type value {srep __TJC_NONE} } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_emit_exprvalue_get $symbol $value_type ->$value<- \"$srep\""
#    }

    # Validate value_type argument
    switch -exact -- $value_type {
        "int" -
        "double" -
        "boolean" -
        "TclObject" -
        "String" -
        "" {
            # No-op
        }
        default {
            error "unsupported value_type \"$value_type\""
        }
    }

    set buffer [emitter_indent]
    append buffer "ExprValue " $symbol " = "

    if {! $_compileproc(options,expr_value_stack)} {
        # Invoke overloaded TJC.exprGetValue() method

        if {$value_type == "" && $value == ""} {
            append buffer "TJC.exprGetValue(interp)" "\;\n"
        } elseif {$srep == "__TJC_NONE"} {
            append buffer "TJC.exprGetValue(interp, " $value ")" "\;\n"
        } else {
            append buffer "TJC.exprGetValue(interp, " $value ", " $srep ")" "\;\n"
        }
    } else {
        # Get ExprValue symbol from the stack

        set stack_sym [compileproc_expr_value_stack_get]
        compileproc_expr_value_stack_alias $symbol $stack_sym

        append buffer $stack_sym "\;\n"

        # If special debug flag is set, then null the
        # expr value initialized at the start of the
        # method. This will generate a NPE if some
        # generation logic caused the same slot to
        # be used more than once.

        if {$_compileproc(options,expr_value_stack_null)} {
            append buffer \
                [emitter_indent] \
                $stack_sym " = null" \
                "\;\n"
        }

        if {$value_type == "" && $value == ""} {
            # Get ExprValue but don't initialize it
        } else {
            # Invoke type specific initilization method

            if {$srep == "__TJC_NONE" || $srep == "null"} {
                set args $value
            } else {
                set args "$value, $srep"
            }

            append buffer [emitter_indent]
 
            switch -exact -- $value_type {
                "int" {
                    append buffer \
                        $symbol ".setIntValue(" $args ")"
                }
                "double" {
                    append buffer \
                        $symbol ".setDoubleValue(" $args ")"
                }
                "boolean" {
                    append buffer \
                        $symbol ".setIntValue(" $args ")"
                }
                "TclObject" {
                    # Parse TclObject value into ExprValue
                    if {$args ne $value} {
                        error "unexpected string rep \"$srep\" for TclObject type"
                    }
                    append buffer \
                        "TJC.exprInitValue(interp, $symbol, $value)"
                }
                "String" {
                    # String value never has a string rep
                    if {$args ne $value} {
                        error "unexpected string rep \"$srep\" for String type"
                    }
                    append buffer \
                        $symbol ".setStringValue(" $value ")"
                }
                "" {
                    error "unexpected empty string as value_type"
                }
                default {
                    error "unmatched value_type \"$value_type\""
                }
            }

            append buffer "\;\n"
        }
    }

    return $buffer
}

# This method is invoked to generate code that should
# be evaluated when the expr logic is done with an
# ExprValue. If the expr values are being grabbed
# from the stack then the values don't need to be
# released and this method return the empty string.

# symbol: ExprValue symbol that may be released

proc compileproc_emit_exprvalue_release { symbol } {
    global _compileproc

    if {$_compileproc(options,expr_value_stack)} {
        set buffer ""

        # If special flag to enable nulling of grabbed
        # ExprValue was on, then reset the alias value.

        if {$_compileproc(options,expr_value_stack_null)} {
            set ev [compileproc_expr_value_stack_lookup_alias $symbol]

            append buffer \
                [emitter_indent] \
                $ev " = " $symbol \
                "\;\n"
        }

        # Release the ExprValue symbol (it is actualy an alias)
        compileproc_expr_value_stack_release $symbol

        return $buffer
    }

    set buffer ""
    append buffer \
        [emitter_indent] \
        "TJC.exprReleaseValue(interp, " $symbol ")" \
        "\;\n"
    return $buffer
}

# Return true if the given string can be
# represented as a Java integer type.
# This means it fits into a 32bit int
# type.

proc compileproc_string_is_java_integer { tstr } {
#    set debug 0

#    if {$debug} {
#        puts "compileproc_string_is_java_integer \"$tstr\""
#    }

    # A Java int type must be in the range -2147483648 to 2147483647.
    # A Tcl implementation might support only 32 bit integer operations,
    # or it might promote integers to wide integers if numbers get too
    # large. Check that the value can be represented as some kind of integer
    # and then check the range.

    if {$tstr == "" || ![string is integer $tstr]} {
#        if {$debug} {
#            puts "string \"$tstr\" is not an integer as defined by string is integer"
#        }
        return 0
    }

    # Check that the integer value is within the range of valid
    # integers. Do this by comparing floating point values.
    set min -2147483648.0
    set max  2147483647.0

    # For integers that consist of only decimal digits, it is
    # possible to parse the integer as a double. For example,
    # 2147483648 can't be represented as an integer but it
    # could be represented as a 2147483648.0 double value.
    # If the integer looks like a decimal integer, then
    # append a decimal point, otherwise convert it to a
    # double after parsing as an integer.

    set tstr [string trim $tstr]
    if {[regexp {^(\-|\+)?(0x|0X)?0+$} $tstr]} {
        set fnum "0.0"
    } elseif {[regexp {^(\-|\+)?[1-9][0-9]*$} $tstr]} {
#        if {$debug} {
#            puts "tstr looks like a decimal integer, will parse it as a double"
#        }
        set fnum "${tstr}.0"
    } else {
#        if {$debug} {
#            puts "tstr does not look like a decimal integer, parsing as int"
#        }

        # An integer in a non-decimal base needs to be checked for
        # overflow by parsing as an unsigned number and checking
        # to see if the integer became negative.

        set is_neg 0
        set c [string index ${tstr} 0]
        if {$c == "+" || $c == "-"} {
            if {$c == "-"} {
                set is_neg 1
            }
            set ptstr [string range $tstr 1 end]
        } else {
            set ptstr $tstr
        }
        set fnum [expr {int($ptstr)}]
        if {$is_neg && $fnum == $min} {
            # Special case for -0x80000000, this is a valid java integer
            set fnum $min
        } elseif {$fnum <= 0} {
            # Unsigned number must have overflowed, or been chopped to zero
#            if {$debug} {
#                puts "unsigned integer \"$ptstr\" causes 32bit overflow, not a java integer"
#            }
            return 0
        } else {
            if {$is_neg} {
                set fnum [expr {-1.0 * $fnum}]
            }
        }
    }
    set fnum [expr {double($fnum)}]
#    if {$debug} {
#        puts "tstr is $tstr"
#        puts "fnum is $fnum"
#    }

    if {($fnum > 0.0 && $fnum > $max) ||
            ($fnum < 0.0 && $fnum < $min)} {
#        if {$debug} {
#            puts "string \"$tstr\" is outside the java integer range"
#        }
        return 0
    }
    return 1
}

# Return true if the given string can be
# represented as a Java double type. If
# the string could be an integer then
# this method will return 0.

proc compileproc_string_is_java_double { tstr } {
#    set debug 0

#    if {$debug} {
#        puts "compileproc_string_is_java_double \"$tstr\""
#    }

    if {$tstr == "" || ![string is double $tstr]} {
        return 0
    }
    # String like "1.0" and "1e6" are valid doubles,
    # but "9999999999999999" is not.
    set has_decimal [expr {[string first "." $tstr] != -1}]
    set has_exp [expr {[string first "e" $tstr] != -1}]

    if {$has_decimal || (!$has_decimal && $has_exp)} {
        # Looks like a valid floating point
    } else {
        # Not a valid floating point
        return 0
    }

    # If this number is smaller than the smallest Java
    # double or larger than the largest double, then
    # reject it and let the runtime worry about parsing
    # it from a string.

    if {0} {
    # FIXME: These tests don't work at all. Not clear
    # how we can tell if a floating point number is
    # not going to be a valid Java constant.

    set dmin1 -4.9e-324
    set dmin2 4.9e-324
    if {$tstr < $dmin1} {
        puts "$tstr is smaller than $dmin1"
        return 0
    }
    if {$tstr < $dmin2} {
        puts "$tstr is smaller than $dmin2"
        return 0
    }

    set dmax1 -1.7976931348623157e+308
    set dmax2 1.7976931348623157e+308
    if {$tstr > $dmax1} {
        puts "$tstr is larger than $dmax1"
        return 0
    }
    if {$tstr > $dmax2} {
        puts "$tstr is larger than $dmax2"
        return 0
    }

    }

    return 1
}

# Return a tuple of {MATCHES PARSED PRINTED} that indicates if a
# parsed value exactly matches the string rep of the passed in
# value. The PARSED value is the format of the number used by Tcl.
# The PRINTED value is the preferred output format that a Java
# literal would be printed as. For example, the integer literal
# "100" would exactly match the string rep of "100" so there is
# no reason to save the string rep. The integer literal "0xFF"
# would have a parsed value of "255" so it would not exactly match
# the string rep. In integer cases, the PARSED and PRINTED values
# will be the same. For floating point numbers like "1.0e16", the
# PARSED value would be "1e+016" and the PRINTED value would be "1e16".

proc compileproc_parse_value { value } {
#    set debug 0

#    if {$debug} {
#        puts "compileproc_parse_value \"$value\""
#    }

    set is_integer [compileproc_string_is_java_integer $value]
    if {!$is_integer} {
        set is_double [compileproc_string_is_java_double $value]
    } else {
        set is_double 0
    }

#    if {$debug} {
#        puts "is_integer $is_integer"
#        puts "is_double $is_double"
#    }

    if {!$is_integer && !$is_double} {
        error "value \"$value\" is not a valid Java integer or double"
    }

    set parsed [expr {$value}]
    set matching_strrep [expr {$parsed eq $value}]
    set compare [string compare $parsed $value]
    # Double check results
    if {$matching_strrep && ($compare != 0)} {
        error "matching_strrep is true but compare is $compare"
    }
    if {!$matching_strrep && ($compare == 0)} {
        error "matching_strrep is false but compare is $compare"
    }
#    if {$debug} {
#        puts "value  is \"$value\""
#        puts "parsed is \"$parsed\""
#        puts "\$parsed eq \$value is $matching_strrep"
#    }

    set printed $parsed

    # Attempt to simplify a double like "1e+016" so that
    # it prints like "1e16".

    if {$is_double} {
        set estr [format %g $value]
#        if {$debug} {
#            puts "exponent string is \"$estr\""
#        }

        if {[regexp {^([0-9|.]+)e([\+|\-][0-9][0-9][0-9])$} \
                $estr whole npart epart]} {
#            if {$debug} {
#                puts "number part is \"$npart\""
#                puts "exponent part is \"$epart\""
#            }
            set printed $npart
            append printed "e"

            # Skip + if it appears at front of exponent
            set sign [string index $epart 0]
            if {$sign == "-"} {
                append printed "-"
            }
            # Skip leading zeros
            set d1 [string index $epart 1]
            if {$d1 != 0} {
                append printed $d1
            }
            set d2 [string index $epart 2]
            if {$d2 != 0} {
                append printed $d2
            }
            set d3 [string index $epart 3]
            append printed $d3
        }
    }

#    if {$debug} {
#        puts "printed is \"$printed\""
#    }

    return [list $matching_strrep $parsed $printed]
}

# Return 1 if append command can be inlined.

proc compileproc_can_inline_command_append { key } {
    global _compileproc

    # The append command accepts 2, 3, or more than 3 arguments.
    # Pass to runtime impl if there are less than 3 arguments.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args < 3} {
        return 0
    }

    # 2nd argument to inlined append command is typically a
    # static scalar or array variable name. It could
    # also be a static array name with a non-static key.

    if {![compileproc_can_inline_variable_access $key 1]} {
        return 0
    }

    return 1
}

# Emit code to implement inlined append command. The
# inlined append command would have already been validated
# to have 3 or more arguments and a constant variable
# name or a constant array name with a non-constant key.
# The inlined append command is a bit more tricky than
# other commands because it must emit special code when
# cache variables is enabled.

proc compileproc_emit_inline_command_append { key } {
    set buffer ""
    set tmpsymbol {}

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    # Determine constant variable name as a String.

    set tuple [compileproc_get_argument_tuple $key 1]
    set type [lindex $tuple 0]

    if {$type == "constant"} {
        set varname [lindex $tuple 1]
        set userdata [list 1 $varname]
        set force_decl_tmpsymbol 1
    } else {
        # Evaluate word key in argument 1 before
        # evaluating arguments in the try block.
        # This works with cache vars on or off.

        set tmpsymbol [compileproc_tmpvar_next]
        append buffer [emitter_statement "TclObject $tmpsymbol"]

        set tuple [compileproc_setup_nonconstant_array_variable \
            $key 1 $tmpsymbol]
        set varname [lindex $tuple 0]
        set keysym [lindex $tuple 1]
        set word_buffer [lindex $tuple 2]

        append buffer $word_buffer

        set userdata [list 0 $varname $keysym]

        set force_decl_tmpsymbol 0
    }

    # Emit code to allocate an array of TclObject and
    # assign command argument values to the array
    # before including append specific inline code.

    append buffer [compileproc_emit_objv_assignment \
        $key \
        2 end \
        $tmpsymbol $force_decl_tmpsymbol \
        compileproc_emit_append_call_impl \
        $userdata \
        ]

    return $buffer
}

# Invoked to emit code for inlined TJC.appendVar() call
# inside try block.

proc compileproc_emit_append_call_impl { key arraysym tmpsymbol userdata } {
    global _compileproc

    if {$_compileproc(options,cache_variables)} {
        set cache_variables 1
    } else {
        set cache_variables 0
    }

    set buffer ""

    set is_static_varname [lindex $userdata 0]
    if {$is_static_varname} {
        set varname [lindex $userdata 1]
    } else {
        set varname [lindex $userdata 1]
        set keysym [lindex $userdata 2]
    }

    # Reset interp result before the append
    # method is invoked. If the variable
    # contains the same TclObject saved
    # in the interp result then it would
    # be considered shared and we want
    # to avoid a pointless duplication of
    # the TclObject.

    append buffer [emitter_reset_result]

    set omit_result [compileproc_omit_set_result $key]
    
    if {$omit_result} {
        set assign ""
    } else {
        set assign "$tmpsymbol = "
    }

    if {!$cache_variables} {
        # Note that TJC.appendVar() will concat the second varname
        # if non-null, so passing whole name in first arg is better.

        set qvarname [emitter_double_quote_tcl_string $varname]
        if {$is_static_varname} {
            append buffer \
                [emitter_statement \
                    "${assign}TJC.appendVar(interp, $qvarname, null, $arraysym)"]
        } else {
            # array variable with word key, invoke runtime append.
            append buffer \
                [emitter_statement \
                    "${assign}TJC.appendVar(interp, $qvarname, $keysym, $arraysym)"]
        }
    } elseif {$is_static_varname} {
        # Both array and scalar vars can be cached.

        set vinfo [descend_simple_variable $varname]
        if {[lindex $vinfo 0] == "array"} {
            set p1 [lindex $vinfo 1]
            set p2 [lindex $vinfo 2]

            set cache_symbol [compileproc_variable_cache_lookup $p1]
            set cacheId [compileproc_get_variable_cache_id_from_symbol $cache_symbol]

            set qp1 [emitter_double_quote_tcl_string $p1]
            set qp2 [emitter_double_quote_tcl_string $p2]

            append buffer \
                [emitter_statement \
                    "${assign}appendVarArray(interp, $qp1, $qp2,\
                        $arraysym, compiledLocals, $cacheId)"]
        } elseif {[lindex $vinfo 0] == "scalar"} {
            set cache_symbol [compileproc_variable_cache_lookup $varname]
            set cacheId [compileproc_get_variable_cache_id_from_symbol $cache_symbol]

            set qvarname [emitter_double_quote_tcl_string $varname]
            append buffer \
                [emitter_statement \
                    "${assign}appendVarScalar(interp, $qvarname,\
                        $arraysym, compiledLocals, $cacheId)"]
        } else {
            error "unexpected result \{$vinfo\} from descend_simple_variable"
        }
    } else {
        # array variable with word key, cache vars enabled.

        set cache_symbol [compileproc_variable_cache_lookup $varname]
        set cacheId [compileproc_get_variable_cache_id_from_symbol $cache_symbol]

        set qvarname [emitter_double_quote_tcl_string $varname]

        append buffer \
            [emitter_statement \
                "${assign}appendVarArray(interp, $qvarname, $keysym,\
                    $arraysym, compiledLocals, $cacheId)"]
    }

    # If result of append is not used, then don't bother setting it.

    if {!$omit_result} {
        append buffer [emitter_set_result $tmpsymbol false]
    }

    return $buffer
}

# Return 1 if global command can be inlined.

proc compileproc_can_inline_command_global { key } {
    global _compileproc

#    set debug 0
#
#    if {$debug} {
#        puts "compileproc_can_inline_command_global $key"
#    }

    # The global command accepts 1 to N args, If no arguments
    # are given just pass to runtime global command impl to
    # raise error message.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args < 2} {
#        if {$debug} {
#            puts "returning false since there are $num_args args"
#        }
        return 0
    }

    # Each argument variable name must be a constant string
    # otherwise pass the arguments to the runtime global command.

    for {set i 1} {$i < $num_args} {incr i} {
        set tuple [compileproc_get_argument_tuple $key $i]
        set type [lindex $tuple 0]

        if {$type != "constant"} {
#            if {$debug} {
#                puts "returning false since argument $i is non-constant type $type"
#            }
            return 0
        }

        # Global accepts what seem to be array variable names like
        # ARR(elem), but it does not seem to be possible to access
        # them. If an argument looks like an array then don't
        # compile the global command in order to keep things simple.

        set varname [lindex $tuple 1]
        set tail [namespace tail $varname]
        set vinfo [descend_simple_variable $tail]
        if {[lindex $vinfo 0] != "scalar"} {
#            if {$debug} {
#                puts "returning false since argument $i is not a scalar"
#            }
            return 0
        }
    }

    return 1
}

# Emit code to implement inlined global command invocation.

proc compileproc_emit_inline_command_global { key } {
    global _compileproc

    set buffer ""

    # Reset interp result if the result of the global
    # command is used.

    if {![compileproc_omit_set_result $key]} {
        append buffer [emitter_reset_result]
    }

    # Emit global function invocation for each argument

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    # Pass -1 when cache variables is disabled, otherwise
    # pass a cache variable id that will be used for this
    # variable.

    if {$_compileproc(options,cache_variables)} {
        set cache_variables 1
    } else {
        set cache_variables 0
    }

    for {set i 1} {$i < $num_args} {incr i} {
        set tuple [compileproc_get_argument_tuple $key $i]
        set varname [lindex $tuple 1]
        set tail [namespace tail $varname]

        if {$cache_variables} {
            # Create a compiled local variable for this global variable
            set cache_symbol [compileproc_variable_cache_lookup $tail]
            set localIndex [compileproc_get_variable_cache_id_from_symbol $cache_symbol]
        } else {
            set localIndex -1
        }
        append buffer [emitter_make_global_link_var $varname $tail $localIndex]
    }

    return $buffer
}

# Return 1 if incr command can be inlined.

proc compileproc_can_inline_command_incr { key } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_can_inline_command_incr $key"
#    }

    # The incr command accepts 2 or 3 arguments. If wrong number
    # of args given just pass to runtime incr command impl to
    # raise error message.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args != 2 && $num_args != 3} {
#        if {$debug} {
#            puts "returning false since there are $num_args args"
#        }
        return 0
    }

    # 2nd argument to inlined incr command is typically a
    # static scalar or array variable name. It could
    # also be a static array name with a non-static key.

    if {![compileproc_can_inline_variable_access $key 1]} {
        return 0
    }

    # 3rd argument can be a constant or a runtime evaluation result

    return 1
}

# Emit code to implement inlined incr command invocation. The
# inlined incr command would have already been validated
# to have a constant variable name and 2 or 3 arguments
# when this method is invoked. The inlined incr command
# is a bit more tricky that then other commands because
# it must emit special code when cache variables is
# enabled.

proc compileproc_emit_inline_command_incr { key } {
    global _compileproc

    if {$_compileproc(options,cache_variables)} {
        set cache_variables 1
    } else {
        set cache_variables 0
    }

    set buffer ""
    set incr_value_buffer ""
    set incr_buffer ""

    # Determine constant variable name as a String.

    set tuple [compileproc_get_argument_tuple $key 1]
    set type [lindex $tuple 0]
    if {$type == "constant"} {
        set constant_varname 1
        set varname [lindex $tuple 1]
    } else {
        set constant_varname 0
    }

    # Determine the incr amount, if there are 2
    # arguments then the incr amount is 1,
    # otherwise it could be a constant value
    # or a value to be evaluated at runtime.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args == 2} {
        # incr by one
        set incr_symbol 1
    } elseif {$num_args == 3} {
        # determine incr amount

        set constant_integer_increment 0

        set tuple [compileproc_get_argument_tuple $key 2]
        set type [lindex $tuple 0]
        set value [lindex $tuple 1]

        if {$type == "constant"} {
            set is_integer [compileproc_string_is_java_integer $value]
            if {$is_integer} {
                set tuple [compileproc_parse_value $value]
                set stringrep_matches [lindex $tuple 0]
                set parsed_number [lindex $tuple 1]

                if {$stringrep_matches} {
                    set constant_integer_increment 1
                    set incr_symbol $parsed_number
                }
            }
        }

        if {!$constant_integer_increment} {
            # Evaluate value argument
            set tuple [compileproc_emit_argument $key 2 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]
            append incr_value_buffer $value_buffer

            set tmpsymbol [compileproc_tmpvar_next]
            append incr_value_buffer [emitter_statement \
                "int $tmpsymbol = TclInteger.get(interp, $value_symbol)"]

            set incr_symbol $tmpsymbol
        }
    } else {
        error "expected 2 or 3 arguments to incr"
    }

    # Reset the interp result in case it holds a ref
    # to the TclObject inside the variable.

    append incr_buffer [emitter_reset_result]

    # Emit incr statement, incr_symbol is an int value
    # to add to the current value in the variable.

    set omit_result 0

    if {!$cache_variables} {
        # Not in cache variable mode, emit generic TJC.incrVar()
        # that works for either scalars or arrays. Note that
        # omit result logic is ignored here.

        set result_tmpsymbol [compileproc_tmpvar_next]

        if {$constant_varname} {
            set qvarname [emitter_double_quote_tcl_string $varname]
            append buffer \
                $incr_value_buffer \
                $incr_buffer \
                [emitter_indent] "TclObject $result_tmpsymbol = " \
                "TJC.incrVar(interp, " $qvarname ", null" ", " $incr_symbol ")\;\n"
        } else {
            # Evaluate non-constant array varname as a word value.

            append buffer \
                [emitter_indent] "TclObject $result_tmpsymbol\;\n"

            set tuple [compileproc_emit_argument $key 1 false $result_tmpsymbol]
            append buffer [lindex $tuple 2]

            # Save result of word evaluation in a String in case the value
            # evaluation changes the interp result.

            set avname_tmpsymbol [compileproc_tmpvar_next]
            append buffer [emitter_statement \
                "String $avname_tmpsymbol = ${result_tmpsymbol}.toString()"]

            # Add argument 2 evaluation code, then call incr method

            append buffer \
                $incr_value_buffer \
                $incr_buffer \
                [emitter_indent] "$result_tmpsymbol = " \
                "TJC.incrVar(interp, " $avname_tmpsymbol \
                ", null" \
                ", " $incr_symbol ")\;\n"
        }
    } elseif {$constant_varname} {
        # cache variable mode with constant var name, emit either
        # scalar or variable invocation.

        set omit_result [compileproc_omit_set_result $key]

        append buffer \
            $incr_value_buffer \
            $incr_buffer \
            [emitter_indent]

        if {!$omit_result} {
            set result_tmpsymbol [compileproc_tmpvar_next]
            append buffer \
                "TclObject $result_tmpsymbol = "
        }

        set vinfo [descend_simple_variable $varname]
        if {[lindex $vinfo 0] == "array"} {
            set p1 [lindex $vinfo 1]
            set p2 [lindex $vinfo 2]
            set cache_symbol [compileproc_variable_cache_lookup $p1]
            set cacheId [compileproc_get_variable_cache_id_from_symbol $cache_symbol]

            set qp1 [emitter_double_quote_tcl_string $p1]
            set qp2 [emitter_double_quote_tcl_string $p2]

            append buffer \
                "incrVarArray(interp, " $qp1 ", " $qp2 ", " $incr_symbol ", " \
                "compiledLocals" ", " $cacheId ")\;\n"
        } elseif {[lindex $vinfo 0] == "scalar"} {
            set qvarname [emitter_double_quote_tcl_string $varname]

            set cache_symbol [compileproc_variable_cache_lookup $varname]
            set cacheId [compileproc_get_variable_cache_id_from_symbol $cache_symbol]

            append buffer \
                "incrVarScalar(interp, " $qvarname ", " $incr_symbol ", " \
                "compiledLocals" ", " $cacheId ")\;\n"
        } else {
            error "unexpected result \{$vinfo\} from descend_simple_variable"
        }
    } else {
        # Evaluate constant array varname with a non-constant key.

        set omit_result [compileproc_omit_set_result $key]

        set result_tmpsymbol [compileproc_tmpvar_next]
        append buffer \
            [emitter_indent] \
            "TclObject $result_tmpsymbol\;\n"

        # Evaluate array variable key in argument 1 before argument 2

        set tuple [compileproc_setup_nonconstant_array_variable \
            $key 1 $result_tmpsymbol]
        set varname [lindex $tuple 0]
        set keysym [lindex $tuple 1]
        set word_buffer [lindex $tuple 2]

        set qvarname [emitter_double_quote_tcl_string $varname]

        set cache_symbol [compileproc_variable_cache_lookup $varname]
        set cacheId [compileproc_get_variable_cache_id_from_symbol $cache_symbol]

        append buffer \
            $word_buffer \
            $incr_value_buffer \
            $incr_buffer \
            [emitter_indent]

        if {!$omit_result} {
            append buffer \
                "$result_tmpsymbol = "
        }

        append buffer \
            "incrVarArray(interp, " $qvarname ", " $keysym ", " $incr_symbol ", " \
            "compiledLocals" ", " $cacheId ")\;\n"
    }

    # Set interp result to the returned value.

    if {!$omit_result} {
        append buffer [emitter_set_result $result_tmpsymbol false]
    }

    return $buffer
}

# Return 1 if lappend command can be inlined.

proc compileproc_can_inline_command_lappend { key } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_can_inline_command_lappend $key"
#    }

    # The lappend command accepts 2, 3, or more than 3 arguments.
    # Pass to runtime impl if there are less than 3 arguments.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args < 3} {
#        if {$debug} {
#            puts "returning false since there are $num_args args"
#        }
        return 0
    }

    # 2nd argument to inlined lappend command is typically a
    # static scalar or array variable name. It could
    # also be a static array name with a non-static key.

    if {![compileproc_can_inline_variable_access $key 1]} {
        return 0
    }

    return 1
}

# Emit code to implement inlined lappend command. The
# inlined lappend command would have already been validated
# to have 3 arguments and a constant variable name or
# a constant array name with a non-constant key. The inlined
# lappend command is tricky as compared to other commands
# because it needs to emit special code when cached variables
# are enabled.

proc compileproc_emit_inline_command_lappend { key } {
    set buffer ""
    set tmpsymbol {}

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    # Determine constant variable name as a String.

    set tuple [compileproc_get_argument_tuple $key 1]
    set type [lindex $tuple 0]
    if {$type == "constant"} {
        set varname [lindex $tuple 1]
        set userdata [list 1 $varname]
        set force_decl_tmpsymbol 1
    } else {
        # Evaluate word key in argument 1 before
        # evaluating arguments in the try block.
        # This works with cache vars on or off.

        set tmpsymbol [compileproc_tmpvar_next]
        append buffer [emitter_statement "TclObject $tmpsymbol"]

        set tuple [compileproc_setup_nonconstant_array_variable \
            $key 1 $tmpsymbol]
        set varname [lindex $tuple 0]
        set keysym [lindex $tuple 1]
        set word_buffer [lindex $tuple 2]

        append buffer $word_buffer

        set userdata [list 0 $varname $keysym]

        set force_decl_tmpsymbol 0
    }

    # Emit code to allocate an array of TclObject and
    # assign command argument values to the array
    # before including lappend specific inline code.

    append buffer [compileproc_emit_objv_assignment \
        $key \
        2 end \
        $tmpsymbol $force_decl_tmpsymbol \
        compileproc_emit_lappend_call_impl \
        $userdata \
        ]

    return $buffer
}

# Invoked to emit code for inlined TJC.lappendVar() call
# inside try block. This method is called after all
# of the arguments have been evaluated and have
# been assigned to the array.
#
# key : dkey for command
# arraysym : symbol for TclObject[] array
# tmpsymbol : symbol of type TclObject, can be used inside try/finally blocks
# userdata : user defined data passed to compileproc_emit_objv_assignment

proc compileproc_emit_lappend_call_impl { key arraysym tmpsymbol userdata } {
    global _compileproc

    if {$_compileproc(options,cache_variables)} {
        set cache_variables 1
    } else {
        set cache_variables 0
    }

    set buffer ""

    set is_static_varname [lindex $userdata 0]
    if {$is_static_varname} {
        set varname [lindex $userdata 1]
    } else {
        set varname [lindex $userdata 1]
        set keysym [lindex $userdata 2]
    }

    # Reset interp result before the lappend
    # method is invoked. If the variable
    # contains the same TclObject saved
    # in the interp result then it would
    # be considered shared and we want
    # to avoid a pointless duplication of
    # the TclObject.

    append buffer [emitter_reset_result]

    set omit_result [compileproc_omit_set_result $key]

    if {$omit_result} {
        set assign ""
    } else {
        set assign "$tmpsymbol = "
    }

    if {!$cache_variables} {
        set qvarname [emitter_double_quote_tcl_string $varname]
        if {$is_static_varname} {
            append buffer \
                [emitter_statement \
                    "${assign}TJC.lappendVar(interp, $qvarname, null, $arraysym)"]
        } else {
            # array variable with word key, invoke runtime lappend.
            append buffer \
                [emitter_statement \
                    "${assign}TJC.lappendVar(interp, $qvarname, $keysym, $arraysym)"]
        }
    } elseif {$is_static_varname} {
        # static variable name, cache vars enabled, emit either
        # scalar or array variable access.

        set vinfo [descend_simple_variable $varname]
        if {[lindex $vinfo 0] == "array"} {
            set p1 [lindex $vinfo 1]
            set p2 [lindex $vinfo 2]

            set cache_symbol [compileproc_variable_cache_lookup $p1]
            set cacheId [compileproc_get_variable_cache_id_from_symbol $cache_symbol]

            set qp1 [emitter_double_quote_tcl_string $p1]
            set qp2 [emitter_double_quote_tcl_string $p2]

            append buffer \
                [emitter_statement \
                    "${assign}lappendVarArray(interp, $qp1, $qp2,\
                        $arraysym, compiledLocals, $cacheId)"]
        } elseif {[lindex $vinfo 0] == "scalar"} {
            set qvarname [emitter_double_quote_tcl_string $varname]
            set cache_symbol [compileproc_variable_cache_lookup $varname]
            set cacheId [compileproc_get_variable_cache_id_from_symbol $cache_symbol]

            append buffer \
                [emitter_statement \
                    "${assign}lappendVarScalar(interp, $qvarname,\
                        $arraysym, compiledLocals, $cacheId)"]
        } else {
            error "unexpected result \{$vinfo\} from descend_simple_variable"
        }
    } else {
        # array variable with word key, cache vars enabled.

        set cache_symbol [compileproc_variable_cache_lookup $varname]
        set cacheId [compileproc_get_variable_cache_id_from_symbol $cache_symbol]

        set qvarname [emitter_double_quote_tcl_string $varname]

        append buffer \
            [emitter_statement \
                "${assign}lappendVarArray(interp, $qvarname, $keysym,\
                    $arraysym, compiledLocals, $cacheId)"]
    }

    # If result of lappend is not used, then don't bother setting it.

    if {!$omit_result} {
        append buffer [emitter_set_result $tmpsymbol false]
    }

    return $buffer
}

# Return 1 if lindex command can be inlined.

proc compileproc_can_inline_command_lindex { key } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_can_inline_command_lindex $key"
#    }

    # lindex accepts 2 to N argument, but we only
    # compile a lindex command that has 3 arguments.
    # Pass to runtime impl for other num args.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args != 3} {
#        if {$debug} {
#            puts "returning false since there are $num_args args"
#        }
        return 0
    }

    return 1
}

# Emit code to implement inlined lindex command invocation
# that has 3 arguments.

proc compileproc_emit_inline_command_lindex { key } {
    global _compileproc

    set buffer ""

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    # Other that 3 argument is an error.

    if {$num_args != 3} {
        error "expected 3 arguments"
    }

    # Check for a constant integer literal as the
    # index argument. For example: [lindex $list 0]

    set constant_integer_index 0

    set tuple [compileproc_get_argument_tuple $key 2]
    set type [lindex $tuple 0]
    set value [lindex $tuple 1]

    if {$type == "constant"} {
        set is_integer [compileproc_string_is_java_integer $value]
        if {$is_integer} {
            set tuple [compileproc_parse_value $value]
            set stringrep_matches [lindex $tuple 0]
            set parsed_number [lindex $tuple 1]

            if {$stringrep_matches} {
                set constant_integer_index 1
                set index_symbol $parsed_number
            }
        }
    }

    if {$constant_integer_index} {
        # Emit optimized code when a constant integer
        # argument is passed to lindex. There is no
        # need to preserve and release the list
        # since the index is not evaluated.

        # Evaluate value argument
        set tuple [compileproc_emit_argument $key 1 true {}]
        set value_type [lindex $tuple 0]
        set value_symbol [lindex $tuple 1]
        set value_buffer [lindex $tuple 2]

        if {$value_type == "constant"} {
            set result_symbol [compileproc_tmpvar_next]
            set declare_result_symbol 1
        } else {
            append buffer $value_buffer
            set result_symbol $value_symbol
            set declare_result_symbol 0
        }

        if {$declare_result_symbol} {
            append buffer [emitter_indent] \
                "TclObject " $result_symbol " = "
        } else {
            append buffer [emitter_indent] \
                $result_symbol " = "
        }

        append buffer \
            "TclList.index(interp, " $value_symbol ", " $index_symbol ")\;\n" \
            [emitter_container_if_start "$result_symbol == null"] \
            [emitter_reset_result] \
            [emitter_container_if_else] \
            [emitter_set_result $result_symbol false] \
            [emitter_container_if_end]

        return $buffer
    }

    # Not a constant integer index argument.
    # Evaluate the list and index arguments
    # and invoke TJC.lindexNonconst().

    set list_tmpsymbol [compileproc_tmpvar_next]
    set index_tmpsymbol [compileproc_tmpvar_next]

    append buffer \
        [emitter_statement "TclObject $list_tmpsymbol = null"] \
        [emitter_statement "TclObject $index_tmpsymbol"]

    # open try block

    append buffer [emitter_container_try_start]

    # Evaluate list argument
    set i 1
    set tuple [compileproc_emit_argument $key $i false $list_tmpsymbol]
    set value_type [lindex $tuple 0]
    set value_symbol [lindex $tuple 1]
    set value_buffer [lindex $tuple 2]
    if {$value_type == "constant"} {
        append buffer [emitter_statement \
            "$list_tmpsymbol = $value_symbol"]
    } else {
        append buffer $value_buffer
    }
    append buffer [emitter_tclobject_preserve $list_tmpsymbol]

    # Evaluate index argument
    set i 2
    set tuple [compileproc_emit_argument $key $i false $index_tmpsymbol]
    set value_type [lindex $tuple 0]
    set value_symbol [lindex $tuple 1]
    set value_buffer [lindex $tuple 2]
    if {$value_type == "constant"} {
        append buffer [emitter_statement \
            "$index_tmpsymbol = $value_symbol"]
    } else {
        append buffer $value_buffer
    }

    # Call TJC.lindexNonconst()
    append buffer [emitter_statement \
        "TJC.lindexNonconst(interp, $list_tmpsymbol, $index_tmpsymbol)"]

    # finally block

    append buffer \
        [emitter_container_try_finally] \
        [emitter_container_if_start "$list_tmpsymbol != null"] \
        [emitter_tclobject_release $list_tmpsymbol] \
        [emitter_container_if_end] \
        [emitter_container_try_end]

    return $buffer
}

# Return 1 if list command can be inlined.
# The list command accepts 0 to N arguments
# of any type, so it can always be inlined.

proc compileproc_can_inline_command_list { key } {
    return 1
}

# Emit code to implement inlined list command invocation.
# This code generates a "pure" Tcl list, that is a list
# without a string rep.

proc compileproc_emit_inline_command_list { key } {
    global _compileproc

    set buffer ""

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    #puts "compileproc_emit_inline_command_list $key, num_args is $num_args"

    set tmpsymbol [compileproc_tmpvar_next]

    append buffer [emitter_statement \
        "TclObject $tmpsymbol = TclList.newInstance()"]

    # Set a special flag if all arguments are constant
    # strings.

    set constant_args 1
    for {set i 1} {$i < $num_args} {incr i} {
        set tuple [compileproc_get_argument_tuple $key $i]
        set type [lindex $tuple 0]

        if {$type != "constant"} {
            set constant_args 0
        }
    }

    # Declare a second symbol to hold evaluated list values
    # in the case where 1 or more arguments are non-constant.

    if {$constant_args} {
        set value_tmpsymbol {}
    } else {
        set value_tmpsymbol [compileproc_tmpvar_next]
        append buffer [emitter_statement "TclObject $value_tmpsymbol"]
    }

    if {$num_args == 1} {
        # No-op
    } elseif {$num_args == 2} {
        # Special case of list of length 1 shows up quite
        # a bit so create a special case without the
        # try block. Since only one element is added to
        # the list, there is no need to worry about
        # the preserve() and release() logic.

        append buffer \
            [compileproc_emit_inline_command_list_argument $key 1 \
                $tmpsymbol $value_tmpsymbol]
    } else {
        # Setup try block to release list object in case of an exception
        append buffer [emitter_indent] "try \{\n"
        emitter_indent_level +1

        for {set i 1} {$i < $num_args} {incr i} {
            append buffer \
                [compileproc_emit_inline_command_list_argument $key $i \
                    $tmpsymbol $value_tmpsymbol]
        }

        emitter_indent_level -1
        append buffer [emitter_indent] "\} catch (TclException ex) \{\n"
        emitter_indent_level +1
        append buffer \
            [emitter_tclobject_release $tmpsymbol] \
            [emitter_statement "throw ex"]
        emitter_indent_level -1
        append buffer [emitter_indent] "\}\n"
    }

    # Set interp result to the returned value.
    # This code always calls setResult(), so there
    # is no need to worry about calling resetResult()
    # before the inlined set impl begins.

    append buffer [emitter_set_result $tmpsymbol false]

    return $buffer
}

# Evaluate argument append statement and return buffer 

proc compileproc_emit_inline_command_list_argument { key i listsymbol valuesymbol } {
    set buffer ""

    # Evaluate value argument
    set tuple [compileproc_emit_argument $key $i false $valuesymbol]
    set value_type [lindex $tuple 0]
    set value_symbol [lindex $tuple 1]
    set value_buffer [lindex $tuple 2]

    # check for constant symbol special case, no
    # need to eval anything for a constant. Note
    # that we don't bother to preserve() and
    # release() the TclObject since
    # TclList.append() calls preserve().

    if {$value_type == "constant"} {
        set value $value_symbol
    } else {
        append buffer $value_buffer
        set value $value_symbol
    }

    # FIXME: This generated code invokes append()
    # over and over, an optimized approach that
    # invoked a TJC method optmized to know that
    # the TclObject is already of type list
    # could be even more efficient. A list append
    # is done often, so speed is important here.

    append buffer [emitter_statement \
        "TclList.append(interp, $listsymbol, $value)"]

    return $buffer
}

# Return 1 if llength command can be inlined.

proc compileproc_can_inline_command_llength { key } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_can_inline_command_llength $key"
#    }

    # The llength command accepts 1 argument. Pass to runtime
    # llength command impl to raise error message otherwise.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args != 2} {
#        if {$debug} {
#            puts "returning false since there are $num_args args"
#        }
        return 0
    }

    return 1
}

# Emit code to implement inlined llength command invocation.

proc compileproc_emit_inline_command_llength { key } {
    global _compileproc

    set buffer ""

    # Evaluate value argument
    set tuple [compileproc_emit_argument $key 1 true {}]
    set value_type [lindex $tuple 0]
    set value_symbol [lindex $tuple 1]
    set value_buffer [lindex $tuple 2]

    append buffer $value_buffer

    # Get list length and set interp result. We don't
    # need to worry about the ref count here since
    # there is only one value and we are finished
    # with it by the time setResult() is invoked.

    set tmpsymbol [compileproc_tmpvar_next]

    append buffer \
        [emitter_statement \
            "int $tmpsymbol = TclList.getLength(interp, $value_symbol)"] \
        [emitter_set_result $tmpsymbol false]

    return $buffer
}

# Return 1 if set command can be inlined.

proc compileproc_can_inline_command_set { key } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_can_inline_command_set $key"
#    }

    # The set command accepts 2 or 3 arguments. If wrong number
    # of args given just pass to runtime set command impl to
    # raise error message.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args != 2 && $num_args != 3} {
#        if {$debug} {
#            puts "returning false since there are $num_args args"
#        }
        return 0
    }

    # 2nd argument to inlined set command is typically a
    # static scalar or array variable name. It could
    # also be a static array name with a non-static key.

    if {![compileproc_can_inline_variable_access $key 1]} {
        return 0
    }

    # 3rd argument can be a constant or a runtime evaluation result

    return 1
}

# Emit code to implement inlined set command invocation. The
# inlined set command would have already been validated
# to have a constant variable name and 2 or 3 arguments
# when this method is invoked.

proc compileproc_emit_inline_command_set { key } {
    global _compileproc

    set buffer ""

    # Determine constant variable name as a String.

    set tuple [compileproc_get_argument_tuple $key 1]
    set type [lindex $tuple 0]
    if {$type == "constant"} {
        set constant_varname 1
        set varname [lindex $tuple 1]
    } else {
        set constant_varname 0
    }

    # Determine if this is a get or set operation

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    set omit_result 0

    if {$num_args == 2} {
        # get variable value
        set tmpsymbol [compileproc_tmpvar_next]
        if {$constant_varname} {
            append buffer \
                [compileproc_get_variable "TclObject $tmpsymbol" \
                    $varname true]
        } else {
            append buffer [emitter_statement \
                "TclObject $tmpsymbol"]
            foreach gs_buffer \
                    [compileproc_get_set_nonconstant_array_variable $key 1 \
                        get $tmpsymbol] {
                append buffer $gs_buffer
            }
        }
    } elseif {$num_args == 3} {
        # Determine if command can avoid emitting a setResult() call.

        set omit_result [compileproc_omit_set_result $key]

#        puts "omit_result for $key is $omit_result"

        if {$omit_result} {
            # If the result is not set and the value
            # is a constant, then there is no need
            # to declare a tmpsymbol.

            set tuple [compileproc_get_argument_tuple $key 2]
            set type [lindex $tuple 0]
            
            if {$type == "constant"} {
                set tmpsymbol {}
            } else {
                set tmpsymbol [compileproc_tmpvar_next]
            }
        } else {
            set tmpsymbol [compileproc_tmpvar_next]
        }

        if {$tmpsymbol != {}} {
            append buffer [emitter_statement \
                "TclObject $tmpsymbol"]
        }

        # Evaluate value argument
        set tuple [compileproc_emit_argument $key 2 false $tmpsymbol]
        set value_type [lindex $tuple 0]
        set value_symbol [lindex $tuple 1]
        set value_buffer [lindex $tuple 2]

        # check for constant symbol special case, no
        # need to eval anything for a constant. Note
        # that we don't bother to preserve() and
        # release() the TclObject since we are only
        # dealing with one TclObject and we are passing
        # it to setVar() which will take care of
        # preserving the value.

        if {$value_type == "constant"} {
            set value $value_symbol
        } else {
            set value $tmpsymbol
        }

        # set variable value, save result in tmp

        if {$constant_varname} {
            append buffer $value_buffer
            if {$omit_result} {
                set tmpsymbol {}
            }
            append buffer [compileproc_set_variable $tmpsymbol $varname true $value 0]
        } else {
            set gs_buffers \
                [compileproc_get_set_nonconstant_array_variable $key 1 \
                    set $tmpsymbol true $value]
            append buffer \
                [lindex $gs_buffers 0] \
                $value_buffer \
                [lindex $gs_buffers 1]
        }
    } else {
        error "expected 2 or 3 arguments to set"
    }

    # Set interp result to the returned value.
    # This code always calls setResult(), so there
    # is no need to worry about calling resetResult()
    # before the inlined set impl begins. In the case
    # where the result is not used, don't set it.

    if {!$omit_result} {
        append buffer [emitter_set_result $tmpsymbol false]
    }

    return $buffer
}

# Return 1 if string command can be inlined.

proc compileproc_can_inline_command_string { key } {
    global _compileproc

#    set debug 0

#    if {$debug} {
#        puts "compileproc_can_inline_command_string $key"
#    }

    # Some subcommands of the string command can be inlined
    # while others must be processed at runtime. The string
    # command accepts 3 or more arguments.

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    if {$num_args < 3} {
#        if {$debug} {
#            puts "returning false since there are $num_args args"
#        }
        return 0
    }

    # 2nd argument to inlined string must be a constant string
    # that indicates the subcommand name.

    set tuple [compileproc_get_argument_tuple $key 1]
    set type [lindex $tuple 0]
    set subcmdname [lindex $tuple 1]

    if {$type != "constant"} {
#        if {$debug} {
#            puts "returning false since argument 1 is non-constant type $type"
#        }

        return 0
    }

    # Check for supported string subcommand names. This code
    # will validate the expected number of args for each
    # string subcommand.

    switch -exact -- $subcmdname {
        "compare" -
        "equal" {
            # usage: string compare string1 string2
            # usage: string equal string1 string2

            # FIXME: could add -nocase support

            if {$num_args != 4} {
#                if {$debug} {
#                    puts "returning false since string $subcmdname\
#                        requires 4 arguments, there were $num_args args"
#                }
                return 0
            }
        }
        "first" -
        "last" {
            # usage: string first subString string ?startIndex?
            # usage: string last subString string ?lastIndex?

            if {$num_args != 4 && $num_args != 5} {
#                if {$debug} {
#                    puts "returning false since string $subcmdname\
#                        requires 4 or 5 arguments, there were $num_args args"
#                }
                return 0
            }
        }
        "index" {
            # usage: string index string charIndex

            if {$num_args != 4} {
#                if {$debug} {
#                    puts "returning false since string length\
#                        requires 4 arguments, there were $num_args args"
#                }
                return 0
            }
        }
        "length" {
            # usage: string length string

            if {$num_args != 3} {
#                if {$debug} {
#                    puts "returning false since string length\
#                        requires 3 arguments, there were $num_args args"
#                }
                return 0
            }
        }
        "range" {
            # usage: string range string first last

            if {$num_args != 5} {
#                if {$debug} {
#                    puts "returning false since string range\
#                        requires 5 arguments, there were $num_args args"
#                }
                return 0
            }
        }
        default {
#            if {$debug} {
#                puts "returning false since argument 1 is\
#                    unsupported subcommand $subcmdname"
#            }
            return 0
        }
    }

    return 1
}

# Emit code to implement inlined string command invocation.
# The inlined string command would have already been validated
# at this point.

proc compileproc_emit_inline_command_string { key } {
    set buffer ""

    # Emit code for specific inlined subcommand

    set tree [descend_get_data $key tree]
    set num_args [llength $tree]

    set tuple [compileproc_get_argument_tuple $key 1]
    set subcmdname [lindex $tuple 1]

    switch -exact -- $subcmdname {
        "compare" -
        "equal" {
            # usage: string compare string1 string2
            # usage: string equal string1 string2

            # Evaluate string1 argument, save in String tmp local
            set tuple [compileproc_emit_argument $key 2 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set string1_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement "String $string1_tmpsymbol = $value_symbol.toString()"]

            # Evaluate string2 argument, save in String tmp local
            set tuple [compileproc_emit_argument $key 3 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set string2_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement "String $string2_tmpsymbol = $value_symbol.toString()"]

            set result_tmpsymbol [compileproc_tmpvar_next]

            # Emit either "compare" or "equal" operation
            if {$subcmdname == "compare"} {
                append buffer \
                    [emitter_statement \
                        "int $result_tmpsymbol =\
                        $string1_tmpsymbol.compareTo($string2_tmpsymbol)"] \
                    [emitter_statement \
                        "$result_tmpsymbol = (($result_tmpsymbol > 0) ? 1 :\
                        ($result_tmpsymbol < 0) ? -1 : 0)"] \
                    [emitter_set_result $result_tmpsymbol false]
            } elseif {$subcmdname == "equal"} {
                append buffer \
                    [emitter_statement \
                        "boolean $result_tmpsymbol =\
                        $string1_tmpsymbol.equals($string2_tmpsymbol)"] \
                    [emitter_set_result $result_tmpsymbol false]
            } else {
                error "unknown subcommand \"$subcmdname\""
            }
        }
        "first" {
            # usage: string first subString string ?startIndex?

            # The string first command is often used to search for
            # a single character in a string, like so:
            #
            # [string first "\n" $str]
            #
            # Optimize this case by inlining a call to Java's
            # String.indexOf() method when the subString
            # is a single character or a String.

            # Evaluate subString argument
            set tuple [compileproc_get_argument_tuple $key 2]
            set value_type [lindex $tuple 0]

            if {$value_type == "constant"} {
                # Inline constant subString instead of adding
                # it to the constant table.

                set substr_is_constant 1
                set substr_constant_string [lindex $tuple 1]
                set cmap [lindex $tuple 3]

                if {$cmap == {}} {
                    # Grab the first character out of a pattern
                    # that contains no backslash elements.

                    set substr_first [string index $substr_constant_string 0]
                    set substr_len [string length $substr_constant_string]
                } else {
                    # The constant string substr contains backslashes.
                    # Extract from 1 to N characters from the substr_constant_string
                    # that correspond to 1 character from the original

                    set first_num_characters [lindex $cmap 0]
                    set first_end_index [expr {$first_num_characters - 1}]
                    set substr_first [string range $substr_constant_string 0 $first_end_index]

                    # The subString length is length of Tcl string, not the
                    # length of the escaped string.
                    set substr_len [llength $cmap]
                }

                # Emit either a constant String or a constant char decl.
                # When a start index is passed, just declare a String.

                set substr_tmpsymbol [compileproc_tmpvar_next]

                if {$substr_len == 1 && ($num_args == 4)} {
                    set jstr [emitter_backslash_tcl_string \
                        $substr_constant_string]
                    append buffer \
                        [emitter_statement "char $substr_tmpsymbol = '$jstr'"]
                } else {
                    # Invoke TJC method for empty string.
                    if {$substr_len == 0} {
                        set substr_is_constant 0
                    }

                    set jstr [emitter_backslash_tcl_string \
                        $substr_constant_string]
                    append buffer \
                        [emitter_statement "String $substr_tmpsymbol = \"$jstr\""]
                }
            } else {
                # Emit non-constant argument

                set substr_is_constant 0

                set tuple [compileproc_emit_argument $key 2 true {}]
                set value_type [lindex $tuple 0]
                set value_symbol [lindex $tuple 1]
                set value_buffer [lindex $tuple 2]

                set substr_tmpsymbol [compileproc_tmpvar_next]

                append buffer \
                    $value_buffer \
                    [emitter_statement "String $substr_tmpsymbol = $value_symbol.toString()"]
            }

            # Evaluate string argument, save in String tmp local
            set tuple [compileproc_emit_argument $key 3 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set string_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement "String $string_tmpsymbol = $value_symbol.toString()"]

            # See if optional startIndex is given, pass TclObject if it was found

            if {$num_args == 5} {
                set tuple [compileproc_emit_argument $key 4 true {}]
                set value_type [lindex $tuple 0]
                set value_symbol [lindex $tuple 1]
                set value_buffer [lindex $tuple 2]

                append buffer \
                    $value_buffer
                set start_symbol $value_symbol
            } else {
                set start_symbol null
            }

            # Inline direct call to String.indexOf(char)
            # or String.indexOf(String) when the substr
            # is a compile time constant and no startIndex
            # is passed.

            set result_tmpsymbol [compileproc_tmpvar_next]

            if {$substr_is_constant && ($num_args == 4)} {
                append buffer \
                    [emitter_statement "int $result_tmpsymbol =\
                        $string_tmpsymbol.indexOf($substr_tmpsymbol)"]
            } else {
                append buffer \
                    [emitter_statement "TclObject $result_tmpsymbol =\
                        TJC.stringFirst(interp, $substr_tmpsymbol,\
                        $string_tmpsymbol, $start_symbol)"]
            }
            append buffer [emitter_set_result $result_tmpsymbol false]
        }
        "index" {
            # usage: string index string charIndex

            # Evaluate string argument, save in String tmp local
            set tuple [compileproc_emit_argument $key 2 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set string_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement "String $string_tmpsymbol = $value_symbol.toString()"]

            # Evaluate charIndex argument
            set tuple [compileproc_emit_argument $key 3 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set result_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement \
                    "TclObject $result_tmpsymbol =\
                    TJC.stringIndex(interp, $string_tmpsymbol, $value_symbol)"] \
                [emitter_set_result $result_tmpsymbol false]
        }
        "last" {
            # usage: string last subString string ?lastIndex?

            # string last is basically the same as string first, but
            # this implementation does not try to declare the subString
            # as a Java literal or invoke String.lastIndexOf(). Use
            # of string last is less common than string first.

            # Evaluate subString argument, save in String tmp local
            set tuple [compileproc_get_argument_tuple $key 2]
            set tuple [compileproc_emit_argument $key 2 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set substr_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement "String $substr_tmpsymbol = $value_symbol.toString()"]

            # Evaluate string argument, save in String tmp local
            set tuple [compileproc_emit_argument $key 3 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set string_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement "String $string_tmpsymbol = $value_symbol.toString()"]

            # See if optional lastIndex is given, pass TclObject if it was found

            if {$num_args == 5} {
                set tuple [compileproc_emit_argument $key 4 true {}]
                set value_type [lindex $tuple 0]
                set value_symbol [lindex $tuple 1]
                set value_buffer [lindex $tuple 2]

                append buffer \
                    $value_buffer
                set last_symbol $value_symbol
            } else {
                set last_symbol null
            }

            # Invoke runtime method and set interp result

            set result_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                [emitter_statement "TclObject $result_tmpsymbol =\
                    TJC.stringLast(interp, $substr_tmpsymbol,\
                    $string_tmpsymbol, $last_symbol)"] \
                [emitter_set_result $result_tmpsymbol false]
        }
        "length" {
            # usage: string length string

            # Evaluate string argument
            set tuple [compileproc_emit_argument $key 2 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set int_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement "int $int_tmpsymbol = $value_symbol.toString().length()"] \
                [emitter_set_result $int_tmpsymbol false]
        }
        "range" {
            # usage: string range string first last

            # Evaluate string argument, save in String tmp local
            set tuple [compileproc_emit_argument $key 2 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            set string_tmpsymbol [compileproc_tmpvar_next]

            append buffer \
                $value_buffer \
                [emitter_statement "String $string_tmpsymbol = $value_symbol.toString()"]

            # Evaluate first argument
            set tuple [compileproc_emit_argument $key 3 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            if {$value_type == "constant"} {
                set first_tmpsymbol [compileproc_tmpvar_next]
                append buffer \
                    [emitter_statement \
                        "TclObject $first_tmpsymbol = $value_symbol"]
            } else {
                set first_tmpsymbol $value_symbol
                append buffer $value_buffer
            }

            append buffer \
                [emitter_tclobject_preserve $first_tmpsymbol]

            # Evaluate last argument
            set tuple [compileproc_emit_argument $key 4 true {}]
            set value_type [lindex $tuple 0]
            set value_symbol [lindex $tuple 1]
            set value_buffer [lindex $tuple 2]

            if {$value_type == "constant"} {
                set last_tmpsymbol [compileproc_tmpvar_next]
                append buffer \
                    [emitter_statement \
                        "TclObject $last_tmpsymbol = $value_symbol"]
            } else {
                set last_tmpsymbol $value_symbol
                append buffer $value_buffer
            }

            # Invoke method and set interp result
            set result_tmpsymbol [compileproc_tmpvar_next]
            append buffer \
                [emitter_statement \
                    "TclObject $result_tmpsymbol =\
                        TJC.stringRange(interp, $string_tmpsymbol,\
                            $first_tmpsymbol, $last_tmpsymbol)"] \
                [emitter_set_result $result_tmpsymbol false]
        }
        default {
            # subcommand name should have already been validated
            error "unsupported subcommand $subcmdname"
        }
    }

    return $buffer
}

# Return true if the command argument at the given index
# is a statically defined variable name that can be
# set with inlined code. For example, a scalar varname
# like "s" or an array varname like "a(k)". The tricky
# bit is that this method will also return true for
# a static array name with a non-static array key
# like "a($k)".

proc compileproc_can_inline_variable_access { key index } {
    set tuple [compileproc_get_argument_tuple $key $index]
    set type [lindex $tuple 0]

    switch -exact -- $type {
        "word" {
            # No-op
        }
        "constant" {
            # A static variable name like a(b)(c) will
            # not be inlined since it is not clear what
            # this does.
            set str [lindex $tuple 1]
            set ind [string first "(" $str]
            if {$ind != -1 &&
                    ($ind != [string last "(" $str])} {
                return 0
            }
            set ind [string last ")" $str]
            if {$ind != -1 &&
                    ($ind != [string first ")" $str])} {
                return 0
            }
            return 1
        }
        "command" -
        "variable" -
        default {
            return 0
        }
    }

    # A word element, check for "arr($key)" case. The
    # first word element would be "arr(" and the
    # last would be ")" in this case.

    set elems [lindex $tuple 1]
    set first [lindex $elems 0]
    set last [lindex $elems end]

    if {[lindex $first 0] != "text" ||
            [lindex $last 0] != "text"} {
        return 0
    }

    # Last element must be "...)", punt if more than 1 ")"
    # or if we find a "(" in the string.

    set last [lindex $last 1]
    set ind [string last ")" $last]
    if {$ind == -1 ||
            $ind != ([string length $last] - 1) ||
            $ind != [string first ")" $last] ||
            -1 != [string first "(" $last]} {
        return 0
    }

    # First element must be "...(...", punt if more than 1 "("
    # or if we find a ")" in the string. Note that varname
    # can't be "(...".

    set first [lindex $first 1]
    set ind [string first "(" $first]
    if {$ind == -1 || $ind == 0 ||
            $ind != [string first "(" $first] ||
            -1 != [string first ")" $first]} {
        return 0
    }

    # Get varname leading up to the "("
    incr ind -1
    set varname [string range $first 0 $ind]

    # varname can include "::" namesapce scope
    # qualifiers.

    return 1
}

# Emit code to get or set the value of a non-constant array variable
# that has a statically defined array name. The command
# compileproc_can_inline_variable_access returns true for
# these array variables. This methods should not be invoked
# for constant variable names or for non-constant array names.
# This method returns two buffers, the first will get the
# value of the array key. The second method will set the
# value of the array object using the passed in tmpsymbol
# as the array key. The second buffer will result in an
# assignable value of type TclObject.
#
# key : descend key for the command
# index : array variable argument index
# getset : either "get" or "set"
# tmpsymbol : symbol of type TclObject that a get or set result
#     can be assigned to.
# assign_tmpsymbol : defaults to true for a get operation. For
#     a set operation, will assign result of array set to tmpsymbol.
# value : (optional) set variable to value (can be TclObject or String)

proc compileproc_get_set_nonconstant_array_variable { key index \
        getset tmpsymbol {assign_tmpsymbol true} {value {}} } {

    set tuple [compileproc_setup_nonconstant_array_variable \
        $key $index $tmpsymbol]
    set varname [lindex $tuple 0]
    set keysym [lindex $tuple 1]
    set word_buffer [lindex $tuple 2]

    if {$getset == "get"} {
        set get_set_buffer \
            [compileproc_emit_array_variable_get $tmpsymbol $varname $keysym false]
    } elseif {$getset == "set"} {
        if {$assign_tmpsymbol} {
            set set_tmpsymbol $tmpsymbol
        } else {
            set set_tmpsymbol {}
        }

        set get_set_buffer \
            [compileproc_emit_array_variable_set $set_tmpsymbol \
                $varname $keysym false $value]
    } else {
        error "bad getset argument must be \"get\" or \"set\""
    }

    return [list $word_buffer $get_set_buffer]
}

# Util method used to evaluate the word elements in a
# non-constant array variable name. This method
# returns a tuple of:
#
# {ARRAYNAME KEYSYM WORD_BUFFER}
#
# ARRAYNAME : array variable name as constant String
# KEYSM : array element key as String symbol
# WORD_BUFFER : code to evaluate KEYSYM

proc compileproc_setup_nonconstant_array_variable { key index tmpsymbol } {
    # Non-constant array variable name already validated by
    # compileproc_can_inline_variable_access, so just grab the
    # variable name here.

    # Get the static array variable name

    set tuple [compileproc_get_argument_tuple $key $index]
    if {[lindex $tuple 0] != "word"} {
        error "expected word argument"
    }
    set elems [lindex $tuple 1]
    set first [lindex $elems 0 1]
    set ind [string first "(" $first]
    incr ind -1
    set varname [string range $first 0 $ind]
    set prefix [string range $first [expr {$ind + 2}] end]

    set last [lindex $elems end 1]
    set suffix [string range $last 0 end-1]

    # Evaluate the array key, it is composed of word elements.
    # Start with the text just after the "...(" and include
    # all elements up to the ")" character.

    set key_elems [lrange $elems 1 end-1]
    if {$prefix != ""} {
        set key_elems [linsert $key_elems 0 [list text $prefix]]
    }
    if {$suffix != ""} {
        lappend key_elems [list text $suffix]
    }

    # Support passing in {} as tmpsymbol
    set decl_flag 0
    if {$tmpsymbol == {}} {
        set tmpsymbol [compileproc_tmpvar_next]
        set decl_flag 1
    }
    # Evaluate the word element in the first buffer
    # as save the results in a String local. This
    # always needs to be done since the result
    # object could be an interp result.

    set keysym [compileproc_tmpvar_next]
    append word_buffer \
        [compileproc_emit_word $tmpsymbol $key_elems $decl_flag $keysym]

    return [list $varname $keysym $word_buffer]
}

# Return true when a setResult() operation can be
# omitted for the given command key.

proc compileproc_omit_set_result { key } {
    global _compileproc _compileproc_key_info

    return [expr {$_compileproc(options,omit_results) && \
        !$_compileproc_key_info($key,result)}]
}


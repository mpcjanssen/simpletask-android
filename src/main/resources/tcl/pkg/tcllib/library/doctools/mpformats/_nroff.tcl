# -*- tcl -*-
#
# -- nroff commands
#
# Copyright (c) 2003-2005 Andreas Kupries <andreas_kupries@sourceforge.net>


################################################################
# nroff specific commands
#
# All dot-commands (f.e. .PP) are returned with a leading \n,
# enforcing that they are on a new line. Any empty line created
# because of this is filtered out in the post-processing step.


proc nr_lp      {}          {return \n.LP}
proc nr_ta      {{text {}}} {return ".ta$text"}
proc nr_bld     {}          {return \1\\fB}
proc nr_ul      {}          {return \1\\fI}
proc nr_rst     {}          {return \1\\fR}
proc nr_p       {}          {return \n.PP\n}
proc nr_comment {text}      {return "\1'\1\\\" [join [split $text \n] "\n\1'\1\\\" "]"} ; # "
proc nr_enum    {num}       {nr_item " \[$num\]"}
proc nr_item    {{text {}}} {return "\n.IP$text"}
proc nr_vspace  {}          {return \n.sp\n}
proc nr_blt     {text}      {return "\n.TP\n$text"}
proc nr_bltn    {n text}    {return "\n.TP $n\n$text"}
proc nr_in      {}          {return \n.RS}
proc nr_out     {}          {return \n.RE}
proc nr_nofill  {}          {return \n.nf}
proc nr_fill    {}          {return \n.fi}
proc nr_title   {text}      {return "\n.TH $text"}
proc nr_include {file}      {return "\n.so $file"}
proc nr_bolds   {}          {return \n.BS}
proc nr_bolde   {}          {return \n.BE}
proc nr_read    {fn}        {return [nroffMarkup [dt_read $fn]]}
proc nr_cs      {}          {return \n.CS}
proc nr_ce      {}          {return \n.CE}

proc nr_section {name} {
    if {![regexp {[ 	]} $name]} {
	return "\n.SH [string toupper $name]"
    }
    return "\n.SH \"[string toupper $name]\""
}
proc nr_subsection {name}   {
    if {![regexp {[ 	]} $name]} {
	return "\n.SS [string toupper $name]"
    }
    return "\n.SS \"[string toupper $name]\""
}


################################################################

# Handling of nroff special characters in content:
#
# Plain text is initially passed through unescaped;
# internally-generated markup is protected by preceding it with \1.
# The final PostProcess step strips the escape character from
# real markup and replaces unadorned special characters in content
# with proper escapes.
#

global   markupMap
set      markupMap [list \
	"\\"   "\1\\" \
	"'"    "\1'" \
	"\\\\" "\\"]
global   finalMap
set      finalMap [list \
	"\1\\" "\\" \
	"\1'"  "'" \
	"\\"   "\\\\"]
global   textMap
set      textMap [list "\\" "\\\\"]


proc nroffEscape {text} {
    global textMap
    return [string map $textMap $text]
}

# markup text --
#	Protect markup characters in $text.
#	These will be stripped out in PostProcess.
#
proc nroffMarkup {text} {
    global markupMap
    return [string map $markupMap $text]
}

proc nroff_postprocess {nroff} {
    global finalMap

    # Postprocessing final nroff text.
    # - Strip empty lines out of the text
    # - Remove leading and trailing whitespace from lines.
    # - Exceptions to the above: Keep empty lines and leading
    #   whitespace when in verbatim sections (no-fill-mode)

    set nfMode   [list .nf .CS]	; # commands which start no-fill mode
    set fiMode   [list .fi .CE]	; # commands which terminate no-fill mode
    set lines    [list]         ; # Result buffer
    set verbatim 0              ; # Automaton mode/state

    foreach line [split $nroff "\n"] {
	if {!$verbatim} {
	    # Normal lines, not in no-fill mode.

	    if {[lsearch -exact $nfMode [split $line]] >= 0} {
		# no-fill mode starts after this line.
		set verbatim 1
	    }

	    # Ensure that empty lines are not added.
	    # This also removes leading and trailing whitespace.

	    if {![string length $line]} {continue}
	    set line [string trim $line]
	    if {![string length $line]} {continue}

	    if {[regexp {^\x1\\f[BI]\.} $line]} {
		# We found confusing formatting at the beginning of
		# the current line. We lift this line up and attach it
		# at the end of the last line to remove this
		# irregularity. Note that the regexp has to look for
		# the special 0x01 character as well to be sure that
		# the sequence in question truly is formatting.

		set last  [lindex   $lines end]
		set lines [lreplace $lines end end]
		set line "$last $line"
	    } elseif {[string match '* $line]} {
		# Apostrophes at the beginning of a line have to
		# quoted to prevent misinterpretation as comments.
		# The apostrophes for true comments are quoted with \1
		# already and will therefore not detected by the code
		# here.

		set line \1\\$line
	    }
	} else {
	    # No-fill mode. We remove trailing whitespace, but keep
	    # leading whitespace and empty lines.

	    if {[lsearch -exact $fiMode [split $line]] >= 0} {
		# Normal mode resumes after this line.
		set verbatim 0
	    }
	    set line [string trimright $line]
	}
	lappend lines $line
    }
    # Return the modified result buffer
    return [string map $finalMap [join $lines "\n"]]
}


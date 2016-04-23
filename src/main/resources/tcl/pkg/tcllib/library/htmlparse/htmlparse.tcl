# htmlparse.tcl --
#
#	This file implements a simple HTML parsing library in Tcl.
#	It may take advantage of parsers coded in C in the future.
#
#	The functionality here is a subset of the
#
#		Simple HTML display library by Stephen Uhler (stephen.uhler@sun.com)
#		Copyright (c) 1995 by Sun Microsystems
#		Version 0.3 Fri Sep  1 10:47:17 PDT 1995
#
#	The main restriction is that all Tk-related code in the above
#	was left out of the code here. It is expected that this code
#	will go into a 'tklib' in the future.
#
# Copyright (c) 2001 by ActiveState Tool Corp.
# See the file license.terms.

package require Tcl       8.2
package require struct::stack
package require cmdline   1.1

namespace eval ::htmlparse {
    namespace export		\
	    parse		\
	    debugCallback	\
	    mapEscapes		\
	    2tree		\
	    removeVisualFluff	\
	    removeFormDefs

    # Table of escape characters. Maps from their names to the actual
    # character.  See http://htmlhelp.org/reference/html40/entities/

    variable namedEntities

    # I. Latin-1 Entities (HTML 4.01)
    array set namedEntities {
	nbsp \xa0 iexcl \xa1 cent \xa2 pound \xa3 curren \xa4
	yen \xa5 brvbar \xa6 sect \xa7 uml \xa8 copy \xa9
	ordf \xaa laquo \xab not \xac shy \xad reg \xae
	macr \xaf deg \xb0 plusmn \xb1 sup2 \xb2 sup3 \xb3
	acute \xb4 micro \xb5 para \xb6 middot \xb7 cedil \xb8
	sup1 \xb9 ordm \xba raquo \xbb frac14 \xbc frac12 \xbd
	frac34 \xbe iquest \xbf Agrave \xc0 Aacute \xc1 Acirc \xc2
	Atilde \xc3 Auml \xc4 Aring \xc5 AElig \xc6 Ccedil \xc7
	Egrave \xc8 Eacute \xc9 Ecirc \xca Euml \xcb Igrave \xcc
	Iacute \xcd Icirc \xce Iuml \xcf ETH \xd0 Ntilde \xd1
	Ograve \xd2 Oacute \xd3 Ocirc \xd4 Otilde \xd5 Ouml \xd6
	times \xd7 Oslash \xd8 Ugrave \xd9 Uacute \xda Ucirc \xdb
	Uuml \xdc Yacute \xdd THORN \xde szlig \xdf agrave \xe0
	aacute \xe1 acirc \xe2 atilde \xe3 auml \xe4 aring \xe5
	aelig \xe6 ccedil \xe7 egrave \xe8 eacute \xe9 ecirc \xea
	euml \xeb igrave \xec iacute \xed icirc \xee iuml \xef
	eth \xf0 ntilde \xf1 ograve \xf2 oacute \xf3 ocirc \xf4
	otilde \xf5 ouml \xf6 divide \xf7 oslash \xf8 ugrave \xf9
	uacute \xfa ucirc \xfb uuml \xfc yacute \xfd thorn \xfe
	yuml \xff
    }

    # II. Entities for Symbols and Greek Letters (HTML 4.01)
    array set namedEntities {
	fnof \u192 Alpha \u391 Beta \u392 Gamma \u393 Delta \u394
	Epsilon \u395 Zeta \u396 Eta \u397 Theta \u398 Iota \u399
	Kappa \u39A Lambda \u39B Mu \u39C Nu \u39D Xi \u39E
	Omicron \u39F Pi \u3A0 Rho \u3A1 Sigma \u3A3 Tau \u3A4
	Upsilon \u3A5 Phi \u3A6 Chi \u3A7 Psi \u3A8 Omega \u3A9
	alpha \u3B1 beta \u3B2 gamma \u3B3 delta \u3B4 epsilon \u3B5
	zeta \u3B6 eta \u3B7 theta \u3B8 iota \u3B9 kappa \u3BA
	lambda \u3BB mu \u3BC nu \u3BD xi \u3BE omicron \u3BF
	pi \u3C0 rho \u3C1 sigmaf \u3C2 sigma \u3C3 tau \u3C4
	upsilon \u3C5 phi \u3C6 chi \u3C7 psi \u3C8 omega \u3C9
	thetasym \u3D1 upsih \u3D2 piv \u3D6 bull \u2022
	hellip \u2026 prime \u2032 Prime \u2033 oline \u203E
	frasl \u2044 weierp \u2118 image \u2111 real \u211C
	trade \u2122 alefsym \u2135 larr \u2190 uarr \u2191
	rarr \u2192 darr \u2193 harr \u2194 crarr \u21B5
	lArr \u21D0 uArr \u21D1 rArr \u21D2 dArr \u21D3 hArr \u21D4
	forall \u2200 part \u2202 exist \u2203 empty \u2205
	nabla \u2207 isin \u2208 notin \u2209 ni \u220B prod \u220F
	sum \u2211 minus \u2212 lowast \u2217 radic \u221A
	prop \u221D infin \u221E ang \u2220 and \u2227 or \u2228
	cap \u2229 cup \u222A int \u222B there4 \u2234 sim \u223C
	cong \u2245 asymp \u2248 ne \u2260 equiv \u2261 le \u2264
	ge \u2265 sub \u2282 sup \u2283 nsub \u2284 sube \u2286
	supe \u2287 oplus \u2295 otimes \u2297 perp \u22A5
	sdot \u22C5 lceil \u2308 rceil \u2309 lfloor \u230A
	rfloor \u230B lang \u2329 rang \u232A loz \u25CA
	spades \u2660 clubs \u2663 hearts \u2665 diams \u2666
    }

    # III. Special Entities (HTML 4.01)
    array set namedEntities {
	quot \x22 amp \x26 lt \x3C gt \x3E OElig \u152 oelig \u153
	Scaron \u160 scaron \u161 Yuml \u178 circ \u2C6
	tilde \u2DC ensp \u2002 emsp \u2003 thinsp \u2009
	zwnj \u200C zwj \u200D lrm \u200E rlm \u200F ndash \u2013
	mdash \u2014 lsquo \u2018 rsquo \u2019 sbquo \u201A
	ldquo \u201C rdquo \u201D bdquo \u201E dagger \u2020
	Dagger \u2021 permil \u2030 lsaquo \u2039 rsaquo \u203A
	euro \u20AC
    }

    # IV. Special Entities (XHTML, XML)
    array set namedEntities {
	apos \u0027
    }

    # Internal cache for the foreach variable-lists and the
    # substitution strings used to split a HTML string into
    # incrementally handleable scripts. This should reduce the
    # time compute this information for repeated calls with the same
    # split-factor. The array is indexed by a combination of the
    # numerical split factor and the length of the command prefix and
    # maps this to a 2-element list containing variable- and
    # subst-string.

    variable  splitdata
    array set splitdata {}

}

# htmlparse::parse --
#
#	This command is the basic parser for HTML. It takes a HTML
#	string, parses it and invokes a command prefix for every tag
#	encountered. It is not necessary for the HTML to be valid for
#	this parser to function. It is the responsibility of the
#	command invoked for every tag to check this. Another
#	responsibility of the invoked command is the handling of tag
#	attributes and character entities (escaped characters). The
#	parser provides the un-interpreted tag attributes to the
#	invoked command to aid in the former, and the package at large
#	provides a helper command, '::htmlparse::mapEscapes', to aid
#	in the handling of the latter. The parser *does* ignore
#	leading DOCTYPE declarations and all valid HTML comments it
#	encounters.
#
#	All information beyond the HTML string itself is specified via
#	options, these are explained below.
#
#	To help understanding the options some more background
#	information about the parser.
#
#	It is capable to detect incomplete tags in the HTML string
#	given to it. Under normal circumstances this will cause the
#	parser to throw an error, but if the option '-incvar' is used
#	to specify a global (or namespace) variable the parser will
#	store the incomplete part of the input into this variable
#	instead. This will aid greatly in the handling of
#	incrementally arriving HTML as the parser will handle whatever
#	he can and defer the handling of the incomplete part until
#	more data has arrived.
#
#	Another feature of the parser are its two possible modes of
#	operation. The normal mode is activated if the option '-queue'
#	is not present on the command line invoking the parser. If it
#	is present the parser will go into the incremental mode instead.
#
#	The main difference is that a parser in normal mode will
#	immediately invoke the command prefix for each tag it
#	encounters. In incremental mode however the parser will
#	generate a number of scripts which invoke the command prefix
#	for groups of tags in the HTML string and then store these
#	scripts in the specified queue. It is then the responsibility
#	of the caller of the parser to ensure the execution of the
#	scripts in the queue.
#
#	Note: The queue objecct given to the parser has to provide the
#	same interface as the queue defined in tcllib -> struct. This
#	does for example mean that all queues created via that part of
#	tcllib can be immediately used here. Still, the queue doesn't
#	have to come from tcllib -> struct as long as the same
#	interface is provided.
#
#	In both modes the parser will return an empty string to the
#	caller.
#
#	To a parser in incremental mode the option '-split' can be
#	given and will specify the size of the groups he creates. In
#	other words, -split 5 means that each of the generated scripts
#	will invoke the command prefix for 5 consecutive tags in the
#	HTML string. A parser in normal mode will ignore this option
#	and its value.
#
#	The option '-vroot' specifies a virtual root tag. A parser in
#	normal mode will invoke the command prefix for it immediately
#	before and after he processes the tags in the HTML, thus
#	simulating that the HTML string is enclosed in a <vroot>
#	</vroot> combination. In incremental mode however the parser
#	is unable to provide the closing virtual root as he never
#	knows when the input is complete. In this case the first
#	script generated by each invocation of the parser will contain
#	an invocation of the command prefix for the virtual root as
#	its first command.
#
#	Interface to the command prefix:
#
#	In normal mode the parser will invoke the command prefix with
#	for arguments appended. See '::htmlparse::debugCallback' for a
#	description. In incremental mode however the generated scripts
#	will invoke the command prefix with five arguments
#	appended. The last four of these are the same which were
#	mentioned above. The first however is a placeholder string
#	(\win\) for a clientdata value to be supplied later during the
#	actual execution of the generated scripts. This could be a tk
#	window path, for example. This allows the user of this package
#	to preprocess HTML strings without commiting them to a
#	specific window, object, whatever during parsing. This
#	connection can be made later. This also means that it is
#	possible to cache preprocessed HTML. Of course, nothing
#	prevents the user of the parser to replace the placeholder
#	with an empty string.
#
# Arguments:
#	args	An option/value-list followed by the string to
#		parse. Available options are:
#
#		-cmd	The command prefix to invoke for every tag in
#			the HTML string. Defaults to
#			'::htmlparse::debugCallback'.
#
#		-vroot	The virtual root tag to add around the HTML in
#			normal mode. In incremental mode it is the
#			first tag in each chunk processed by the
#			parser, but there will be no closing tags.
#			Defaults to 'hmstart'.
#
#		-split	The size of the groups produced by an
#			incremental mode parser. Ignored when in
#			normal mode. Defaults to 10. Values <= 0 are
#			not allowed.
#
#		-incvar	The name of the variable where to store any
#			incomplete HTML into. Optional.
#
#		-queue
#			The handle/name of the queue objecct to store
#			the generated scripts into. Activates
#			incremental mode. Normal mode is used if this
#			option is not present.
#
#		After the options the command expects a single argument
#		containing the HTML string to parse.
#
# Side Effects:
#	In normal mode as of the invoked command. Else none.
#
# Results:
#	None.

proc ::htmlparse::parse {args} {
    # Convert the HTML string into a evaluable command sequence.

    variable splitdata

    # Option processing, start with the defaults, then run through the
    # list of arguments.

    set cmd    ::htmlparse::debugCallback
    set vroot  hmstart
    set incvar ""
    set split  10
    set queue  ""

    while {[set err [cmdline::getopt args {cmd.arg vroot.arg incvar.arg split.arg queue.arg} opt arg]]} {
	if {$err < 0} {
	    return -code error "::htmlparse::parse : $arg"
	}
	switch -exact -- $opt {
	    cmd    -
	    vroot  -
	    incvar -
	    queue  {
		if {[string length $arg] == 0} {
		    return -code error "::htmlparse::parse : -$opt illegal argument (empty)"
		}
		# Each option has an variable with the same name associated with it.
		# FRINK: nocheck
		set $opt $arg
	    }
	    split  {
		if {$arg <= 0} {
		    return -code error "::htmlparse::parse : -split illegal argument (<= 0)"
		}
		set split $arg
	    }
	    default {# Can't happen}
	}
    }

    if {[llength $args] > 1} {
	return -code error "::htmlparse::parse : to many arguments behind the options, expected one"
    }
    if {[llength $args] < 1} {
	return -code error "::htmlparse::parse : html string missing"
    }

    set html [PrepareHtml [lindex $args 0]]

    # Look for incomplete HTML from the last iteration and prepend it
    # to the input we just got.

    if {$incvar != {}} {
	upvar $incvar incomplete
    } else {
	set incomplete ""
    }

    if {[catch {set new $incomplete$html}]} {set new $html}
    set html $new

    # Handle incomplete HTML (Recognize incomplete tag at end, buffer
    # it up for the next call).

    set end [lindex \{$html\} end]
    if {[set idx [string last < $end]] > [string last > $end]} {

	if {$incvar == {}} {
	    return -code error "::htmlparse::parse : HTML is incomplete, option -incvar is missing"
	}

	#  upvar $incvar incomplete -- Already done, s.a.
	set incomplete [string range $end $idx end]
	incr idx -1
	set html       [string range $end 0 $idx]
	
    } else {
	set incomplete ""
    }

    # Convert the HTML string into a script.

    set sub "\}\n$cmd {\\1} {} {\\2} \{\}\n$cmd {\\1} {/} {} \{"
    regsub -all -- {<([^\s>]+)\s*([^>]*)/>} $html $sub html

    set sub "\}\n$cmd {\\2} {\\1} {\\3} \{"
    regsub -all -- {<(/?)([^\s>]+)\s*([^>]*)>} $html $sub html

    # The value of queue now determines wether we process the HTML by
    # ourselves (queue is empty) or if we generate a list of  scripts
    # each of which processes n tags, n the argument to -split.

    if {$queue == {}} {
	# And evaluate it. This is the main parsing step.

	eval "$cmd {$vroot} {} {} \{$html\}"
	eval "$cmd {$vroot} /  {} {}"
    } else {
	# queue defined, generate list of scripts doing small chunks of tags.

	set lcmd [llength $cmd]
	set key  $split,$lcmd

	if {![info exists splitdata($key)]} {
	    for {set i 0; set group {}} {$i < $split} {incr i} {
		# Use the length of the command prefix to generate
		# additional variables before the main variable after
		# which the placeholder will be inserted.

		for {set j 1} {$j < $lcmd} {incr j} {
		    append group "b${j}_$i "
		}

		append group "a$i c$i d$i e$i f$i\n"
	    }
	    regsub -all -- {(a[0-9]+)}          $group    {{$\1} @win@} subgroup
	    regsub -all -- {([b-z_0-9]+[0-9]+)} $subgroup {{$\1}}       subgroup

	    set splitdata($key) [list $group $subgroup]
	}

	foreach {group subgroup} $splitdata($key) break ; # lassign
	foreach $group "$cmd {$vroot} {} {} \{$html\}" {
	    $queue put [string trimright [subst $subgroup]]
	}
    }
    return
}

# htmlparse::PrepareHtml --
#
#	Internal helper command of '::htmlparse::parse'. Removes
#	leading DOCTYPE declarations and comments, protects the
#	special characters of tcl from evaluation.
#
# Arguments:
#	html	The HTML string to prepare
#
# Side Effects:
#	None.
#
# Results:
#	The provided HTML string with the described modifications
#	applied to it.

proc ::htmlparse::PrepareHtml {html} {
    # Remove the following items from the text:
    # - A leading	<!DOCTYPE...> declaration.
    # - All comments	<!-- ... -->
    #
    # Also normalize the line endings (\r -> \n).

    # Tcllib SF Bug 861287 - Processing of comments.
    # Recognize EOC by RE, instead of fixed string.

    set html [string map [list \r \n] $html]

    regsub -- "^.*<!DOCTYPE\[^>\]*>"    $html {}     html
    regsub -all -- "--(\[ \t\n\]*)>"      $html "\001\\1\002" html

    # Recognize borken beginnings of a comment and convert them to PCDATA.
    regsub -all -- "<--(\[^\001\]*)\001(\[^\002\]*)\002" $html {\&lt;--\1--\2\&gt;} html

    # And now recognize true comments, remove them.
    regsub -all -- "<!--\[^\001\]*\001(\[^\002\]*)\002"  $html {}                   html

    # Protect characters special to tcl (braces, slashes) by
    # converting them to their escape sequences.

    return [string map [list \
		    "\{" "&#123;" \
		    "\}" "&#125;" \
		    "\\" "&#92;"] $html]
}



# htmlparse::debugCallback --
#
#	The standard callback used by the parser in
#	'::htmlparse::parse' if none was specified by the user. Simply
#	dumps its arguments to stdout.  This callback can be used for
#	both normal and incremental mode of the calling parser. In
#	other words, it accepts four or five arguments. The last four
#	arguments are described below. The optional fifth argument
#	contains the clientdata value given to the callback by a
#	parser in incremental mode. All callbacks have to follow the
#	signature of this command in the last four arguments, and
#	callbacks used in incremental parsing have to follow this
#	signature in the last five arguments.
#
# Arguments:
#	tag			The name of the tag currently
#				processed by the parser.
#
#	slash			Either empty or a slash. Allows us to
#				distinguish between opening (slash is
#				empty) and closing tags (slash is
#				equal to a '/').
#
#	param			The un-interpreted list of parameters
#				to the tag.
#
#	textBehindTheTag	The text found by the parser behind
#				the tag named in 'tag'.
#
# Side Effects:
#	None.
#
# Results:
#	None.

proc ::htmlparse::debugCallback {args} {
    # args = ?clientData? tag slash param textBehindTheTag
    puts "==> $args"
    return
}

# htmlparse::mapEscapes --
#
#	Takes a HTML string, substitutes all escape sequences with
#	their actual characters and returns the resulting string.
#	HTML not containing escape sequences or invalid escape
#	sequences is returned unchanged.
#
# Arguments:
#	html	The string to modify
#
# Side Effects:
#	None.
#
# Results:
#	The argument string with all escape sequences replaced with
#	their actual characters.

proc ::htmlparse::mapEscapes {html} {
    # Find HTML escape characters of the form &xxx(;|EOW)

    # Quote special Tcl chars so they pass through [subst] unharmed.
    set new [string map [list \] \\\] \[ \\\[ \$ \\\$ \\ \\\\] $html]
    regsub -all -- {&([[:alnum:]]{2,7})(;|\M)} $new {[DoNamedMap \1 {\2}]} new
    regsub -all -- {&#([[:digit:]]{1,5})(;|\M)} $new {[DoDecMap \1 {\2}]} new
    regsub -all -- {&#x([[:xdigit:]]{1,4})(;|\M)} $new {[DoHexMap \1 {\2}]} new
    return [subst $new]
}

proc ::htmlparse::DoNamedMap {name endOf} {
    variable namedEntities
    if {[info exist namedEntities($name)]} {
	return $namedEntities($name)
    } else {
	# Put it back..
	return "&$name$endOf"
    }
}

proc ::htmlparse::DoDecMap {dec endOf} {
    scan $dec %d dec
    if {$dec <= 0xFFFD} {
	return [format %c $dec]
    } else {
	# Put it back..
	return "&#$dec$endOf"
    }
}

proc ::htmlparse::DoHexMap {hex endOf} {
    scan $hex %x value
    if {$value <= 0xFFFD} {
	return [format %c $value]
    } else {
	# Put it back..
	return "&#x$hex$endOf"
    }
}

# htmlparse::2tree --
#
#	This command is a wrapper around '::htmlparse::parse' which
#	takes a HTML string and converts it into a tree containing the
#	logical structure of the parsed document. The tree object has
#	to be created by the caller. It is also expected that the tree
#	object provides the same interface as the tree object from
#	tcllib -> struct. It doesn't have to come from that module
#	though. The internal callback does some basic checking of HTML
#	validity and tries to recover from the most basic errors.
#
# Arguments:
#	html	The HTML string to parse and convert.
#	tree	The name of the tree to fill.
#
# Side Effects:
#	Creates a tree object (see tcllib -> struct)
#	and modifies it.
#
# Results:
#	The contents of 'tree'.

proc ::htmlparse::2tree {html tree} {

    # One internal datastructure is required, a stack of open
    # tags. This stack is also provided by the 'struct' module of
    # tcllib. As the operation of this command is synchronuous we
    # don't have to take care against multiple running copies at the
    # same times (Such are possible, but will be in different
    # interpreters and true concurrency is possible only if they are
    # in different threads too). IOW, no need for tricks to make the
    # internal datastructure unique.

    catch {::htmlparse::tags destroy}

    ::struct::stack ::htmlparse::tags
    ::htmlparse::tags push root
    $tree set root type root

    parse -cmd [list ::htmlparse::2treeCallback $tree] $html

    # A bit hackish, correct the ordering of nodes for the optional
    # tag types, over a larger area when was seen by the parser itself.

    $tree walk root -order post n {
	::htmlparse::Reorder $tree $n
    }

    ::htmlparse::tags destroy
    return $tree
}

# htmlparse::2treeCallback --
#
#	Internal helper command. A special callback to
#	'::htmlparse::parse' used by '::htmlparse::2tree' which takes
#	the incoming stream of tags and converts them into a tree
#	representing the inner structure of the parsed HTML
#	document. Recovers from simple HTML errors like missing
#	opening tags, missing closing tags and overlapping tags.
#
# Arguments:
#	tree			The name of the tree to manipulate.
#	tag			See '::htmlparse::debugCallback'.
#	slash			See '::htmlparse::debugCallback'.
#	param			See '::htmlparse::debugCallback'.
#	textBehindTheTag	See '::htmlparse::debugCallback'.
#
# Side Effects:
#	Manipulates the tree object whose name was given as the first
#	argument.
#
# Results:
#	None.

proc ::htmlparse::2treeCallback {tree tag slash param textBehindTheTag} {
    # This could be table-driven I think but for now the switches
    # should work fine.

    # Normalize tag information for later comparisons. Also remove
    # superfluous whitespace. Don't forget to decode the standard
    # entities.

    set  tag  [string tolower $tag]
    set  textBehindTheTag [string trim $textBehindTheTag]
    if {$textBehindTheTag != {}} {
	set text [mapEscapes $textBehindTheTag]
    }

    if {"$slash" == "/"} {
	# Handle closing tags. Standard operation is to pop the tag
	# from the stack of open tags. We don't do this for </p> and
	# </li>. As they were optional they were never pushed onto the
	# stack (Well, actually they are just popped immediately after
	# they were pusheed, see below).

	switch -exact -- $tag {
	    base - option - meta - li - p {
		# Ignore, nothing to do.		
	    }
	    default {
		# The moment we get a closing tag which does not match
		# the tag on the stack we have two possibilities on how
		# this came into existence to choose from:
		#
		# a) A tag is now closed but was never opened.
		# b) A tag requiring an end tag was opened but the end
		#    tag was omitted and we now are at a tag which was
		#    opened before the one with the omitted end tag.

		# NOTE:
		# Pages delivered from the amazon.uk site contain both
		# cases: </a> without opening, <b> & <font> without
		# closing. Another error: <a><b></a></b>, i.e. overlapping
		# tags. Fortunately this can be handled by the algorithm
		# below, in two cycles, one of which is case (b), followed
		# by case (a). It seems as if Amazon/UK believes that visual
		# markup like <b> and <font> is an option (switch-on) instead
		# of a region.

		# Algorithm used here to deal with these:
		# 1) Search whole stack for the matching opening tag.
		#    If there is one assume case (b) and pop everything
		#    until and including this opening tag.
		# 2) If no matching opening tag was found assume case
		#    (a) and ignore the tag.
		#
		# Part (1) also subsumes the normal case, i.e. the
		# matching tag is at the top of the stack.

		set nodes [::htmlparse::tags peek [::htmlparse::tags size]]
		# Note: First item is top of stack, last item is bottom of stack !
		# (This behaviour of tcllib stacks is not documented
		# -> we should update the manpage).

		#foreach n $nodes {lappend tstring [p get $n -key type]}
		#puts stderr --[join $tstring]--

		set level 1
		set found 0
		foreach n $nodes {
		    set type [$tree get $n type]
		    if {0 == [string compare $tag $type]} {
			# Found an earlier open tag -> (b).
			set found 1
			break
		    }
		    incr level
		}
		if {$found} {
		    ::htmlparse::tags pop $level
		    if {$level > 1} {
			#foreach n $nodes {lappend tstring [$tree get $n type]}
			#puts stderr "\tdesync at <$tag> ($tstring) => pop $level"
		    }
		} else {
		    #foreach n $nodes {lappend tstring [$tree get $n type]}
		    #puts stderr "\tdesync at <$tag> ($tstring) => ignore"
		}
	    }
	}

	# If there is text behind a closing tag X it belongs to the
	# parent tag of X.

	if {$textBehindTheTag != {}} {
	    # Attach the text behind the closing tag to the reopened
	    # context.

	    set        pcd  [$tree insert [::htmlparse::tags peek] end]
	    $tree set $pcd  type PCDATA
	    $tree set $pcd  data $textBehindTheTag
	}

    } else {
	# Handle opening tags. The standard operation for most is to
	# push them onto the stack and thus open a nested context.
	# This does not happen for both the optional tags (p, li) and
	# the ones which don't have closing tags (meta, br, option,
	# input, area, img).
	#
	# The text coming with the tag will be added after the tag if
	# it is a tag without a matching close, else it will be added
	# as a node below the tag (as it is the region between the
	# opening and closing tag and thus nested inside). Empty text
	# is ignored under all circcumstances.

	set        node [$tree insert [::htmlparse::tags peek] end]
	$tree set $node type $tag
	$tree set $node data $param

	if {$textBehindTheTag != {}} {
	    switch -exact -- $tag {
		input -	area - img - br {
		    set pcd  [$tree insert [::htmlparse::tags peek] end]
		}
		default {
		    set pcd  [$tree insert $node end]
		}
	    }
	    $tree set $pcd type PCDATA
	    $tree set $pcd data $textBehindTheTag
	}

	::htmlparse::tags push $node

	# Special handling: <p>, <li> may have no closing tag => pop
	#                 : them immediately.
	#
	# Special handling: <meta>, <br>, <option>, <input>, <area>,
	#                 : <img>: no closing tags for these.

	switch -exact -- $tag {
	    hr - base - meta - li - br - option - input - area - img - p - h1 - h2 - h3 - h4 - h5 - h6 {
		::htmlparse::tags pop
	    }
	    default {}
	}
    }
}

# htmlparse::removeVisualFluff --
#
#	This command walks a tree as generated by '::htmlparse::2tree'
#	and removes all the nodes which represent visual tags and not
#	structural ones. The purpose of the command is to make the
#	tree easier to navigate without getting bogged down in visual
#	information not relevant to the search.
#
# Arguments:
#	tree	The name of the tree to cut down.
#
# Side Effects:
#	Modifies the specified tree.
#
# Results:
#	None.

proc ::htmlparse::removeVisualFluff {tree} {
    $tree walk root -order post n {
	::htmlparse::RemoveVisualFluff $tree $n
    }
    return
}

# htmlparse::removeFormDefs --
#
#	Like '::htmlparse::removeVisualFluff' this command is here to
#	cut down on the size of the tree as generated by
#	'::htmlparse::2tree'. It removes all nodes representing forms
#	and form elements.
#
# Arguments:
#	tree	The name of the tree to cut down.
#
# Side Effects:
#	Modifies the specified tree.
#
# Results:
#	None.

proc ::htmlparse::removeFormDefs {tree} {
    $tree walk root -order post n {
	::htmlparse::RemoveFormDefs $tree $n
    }
    return
}

# htmlparse::RemoveVisualFluff --
#
#	Internal helper command to
#	'::htmlparse::removeVisualFluff'. Does the actual work.
#
# Arguments:
#	tree	The name of the tree currently processed
#	node	The name of the node to look at.
#
# Side Effects:
#	Modifies the specified tree.
#
# Results:
#	None.

proc ::htmlparse::RemoveVisualFluff {tree node} {
    switch -exact -- [$tree get $node type] {
	hmstart - html - font - center - div - sup - b - i {
	    # Removes the node, but does not affect the nodes below
	    # it. These are just made into chiildren of the parent of
	    # this node, in its place.

	    $tree cut $node
	}
	script - option - select - meta - map - img {
	    # Removes this node and everything below it.
	    $tree delete $node
	}
	default {
	    # Ignore tag
	}
    }
}

# htmlparse::RemoveFormDefs --
#
#	Internal helper command to
#	'::htmlparse::removeFormDefs'. Does the actual work.
#
# Arguments:
#	tree	The name of the tree currently processed
#	node	The name of the node to look at.
#
# Side Effects:
#	Modifies the specified tree.
#
# Results:
#	None.

proc ::htmlparse::RemoveFormDefs {tree node} {
    switch -exact -- [$tree get $node type] {
	form {
	    $tree delete $node
	}
	default {
	    # Ignore tag
	}
    }
}

# htmlparse::Reorder --

#	Internal helper command to '::htmlparse::2tree'. Moves the
#	nodes between p/p, li/li and h<i> sequences below the
#	paragraphs and items. IOW, corrects misconstructions for
#	the optional node types.
#
# Arguments:
#	tree	The name of the tree currently processed
#	node	The name of the node to look at.
#
# Side Effects:
#	Modifies the specified tree.
#
# Results:
#	None.

proc ::htmlparse::Reorder {tree node} {
    switch -exact -- [set tp [$tree get $node type]] {
	h1 - h2 - h3 - h4 - h5 - h6 - p - li {
	    # Look for right siblings until the next node with a
	    # similar type (or end of level) and move these below this
	    # node.

	    while {1} {
		set sibling [$tree next $node]
		if {
		    ($sibling == {}) ||
		    ([lsearch -exact {h1 h2 h3 h4 h5 h6 p li} [$tree get $sibling type]] != -1)
		} {
		    break
		}
		$tree move $node end $sibling
	    }
	}
	default {
	    # Ignore tag
	}
    }
}

# ### ######### ###########################

package provide htmlparse 1.2

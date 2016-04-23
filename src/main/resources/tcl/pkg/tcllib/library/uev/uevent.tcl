# -*- tcl -*-
# ### ### ### ######### ######### #########
## UEvent - User Event Service - Tcl-level general Event Handling

# ### ### ### ######### ######### #########
## Requirements

package require Tcl 8.4
package require logger

namespace eval ::uevent {}

# ### ### ### ######### ######### #########
## API: bind, unbind, generate

proc ::uevent::bind {tag event command} {
    # Register command (prefix!) as observer for events on the tag.
    # Command will take 3 arguments: tag, event, and dictionary of
    # detail information. Result is token by which the observer can
    # be removed.

    variable db
    variable tk
    variable ex
    variable tcounter

    log::debug [::list bind: $tag $event -> $command]

    set tec [::list $tag $event $command]

    # Same combination as before, same token
    if {[info exists ex($tec)]} {
	log::debug [::list known! $ex($tec)]
	return $ex($tec)
    }

    # New token, and enter everything ...

    set te [::list $tag $event]
    set t uev[incr tcounter]

    set     tk($t) $tec
    set     ex($tec) $t
    lappend db($te)  $t

    log::debug [::list new! $t]
    return $t
}

proc ::uevent::unbind {token} {
    # Removes the event binding represented by the token.

    variable db
    variable tk
    variable ex

    log::debug [::list unbind: $token]

    if {![info exists tk($token)]} return

    set tec $tk($token)
    set te [lrange $tec 0 1]

    log::debug [linsert [linsert $tec 0 =] end-1 ->]

    unset ex($tec)
    unset tk($token)

    set pos [lsearch -exact $db($te) $token]
    if {$pos < 0} return

    if {[llength $db($te)] == 1} {
	# Last observer for this tag,event combination is gone.
	log::debug [linsert $te 0 last!]
	unset db($te)
    } else {
	# Shrink list of observers
	log::debug [linsert [linsert $te 0 shrink!] end @ $pos]
	set db($te) [lreplace $db($te) $pos $pos]
    }
    return
}

proc ::uevent::generate {tag event {details {}}} {
    # Generates the event on the tag, with detail information (a
    # dictionary). This notifies all registered observers.  The
    # notifications are put into the Tcl event queue via 'after 0'
    # events, decoupling them in time from the issueing code.

    variable db
    variable tk

    log::debug [::list generate: $tag $event $details]

    set key [::list $tag $event]
    if {![info exists db($key)]} return

    foreach t $db($key) {
	set cmd [lindex $tk($t) 2]
	log::debug [::list trigger! $t = $cmd]
	after 0 [linsert $cmd end $tag $event $details]
    }

    return
}

proc ::uevent::list {args} {
    # list           - Return all known tags
    # list tag       - Return all events bound to the tag
    # list tag event - Return commands bound to event in tag

    switch -- [llength $args] {
	0 {
	    variable db
	    # Return all known tags.
	    set res {}
	    foreach te [array names db] {
		lappend res [lindex $te 0]
	    }
	    return [lsort -uniq $res]
	}
	1 {
	    variable db
	    # Return all known events for a specific tag
	    set res {}
	    set tag [lindex $args 0]
	    foreach te [array names db [::list $tag *]] {
		lappend res [lindex $te 1]
	    }
	    if {![llength $res]} {
		return -code error "Tag \"$tag\" is not known"
	    }
	    return $res
	}
	2 {
	    variable db
	    variable tk
	    # Return all commands bound to a tag/event combination
	    if {![info exists db($args)]} {
		foreach {tag event} $args break
		return -code error "Tag/Event \"$tag\"/\"$event\" is not known"
	    }
	    set res {}
	    foreach t $db($args) {
		lappend res [lindex $tk($t) 2]
	    }
	    return $res
	}
	default {
	    return -code error "wrong#args: expected ?tag? ?event?"
	}
    }
}

# ### ### ### ######### ######### #########
## Initialization - Tracing, System state

logger::initNamespace ::uevent
namespace eval        ::uevent {

    # Information needed:
    # (1) Per <tag,event> the commands bound to it.
    # (2) Per <tag,event,command> a token representing it.
    # (3) For all <tag,event,command> a quick way to check their existence

    # (Ad 1) db : array (list (tag, event) -> list (token))
    # (Ad 2) tk : array (token -> list (tag, event, command))
    # (Ad 3) ex : array (list (tag, event, command) -> token)

    variable db ; array set db {}
    variable tk ; array set tk {}
    variable ex ; array set ex {}

    variable tcounter 0

    namespace export bind unbind generate list
}

# ### ### ### ######### ######### #########
## Ready

package provide uevent 0.2

##
# ### ### ### ######### ######### #########

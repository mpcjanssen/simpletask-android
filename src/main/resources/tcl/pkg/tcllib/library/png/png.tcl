# png.tcl --
#
#       Querying and modifying PNG image files.
#
# Copyright (c) 2004    Aaron Faupell <afaupell@users.sourceforge.net>
#
# See the file "license.terms" for information on usage and redistribution
# of this file, and for a DISCLAIMER OF ALL WARRANTIES.
# 
# RCS: @(#) $Id: png.tcl,v 1.10 2007/08/20 22:06:58 andreas_kupries Exp $

package provide png 0.1.2

namespace eval ::png {}

proc ::png::_openPNG {file {mode r}} {
    set fh [open $file $mode]
    fconfigure $fh -encoding binary -translation binary -eofchar {}
    if {[read $fh 8] != "\x89PNG\r\n\x1a\n"} { close $fh; return -code error "not a png file" }
    return $fh
}

proc ::png::isPNG {file} {
    if {[catch {_openPNG $file} fh]} { return 0 }
    close $fh
    return 1
}

proc ::png::validate {file} {
    package require crc32
    if {[catch {_openPNG $file} fh]} { return SIG }
    set num 0
    set idat 0
    set last {}

    while {[set r [read $fh 8]] != ""} {
        binary scan $r Ia4 len type
        if {$len < 0} { close $fh; return BADLEN }
        set r [read $fh $len]
        binary scan [read $fh 4] I crc
	if {$crc < 0} {set crc [format %u [expr {$crc & 0xffffffff}]]}
        if {[eof $fh]} { close $fh; return EOF }
        if {($num == 0) && ($type != "IHDR")} { close $fh; return NOHDR }
        if {$type == "IDAT"} { set idat 1 }
        if {[::crc::crc32 $type$r] != $crc} { close $fh; return CKSUM }
        set last $type
        incr num
    }
    close $fh
    if {!$idat} { return NODATA }
    if {$last != "IEND"} { return NOEND }
    return OK
}

proc ::png::imageInfo {file} {
    set fh [_openPNG $file]
    binary scan [read $fh 8] Ia4 len type
    set r [read $fh $len]
    if {![eof $fh] && $type == "IHDR"} {
        binary scan $r IIccccc width height depth color compression filter interlace
	binary scan [read $fh 4] I check
	if {$check < 0} {set check [format %u [expr {$check & 0xffffffff}]]}
	if {[::crc::crc32 IHDR$r] != $check} {
	    return -code error "header checksum failed"
	}
        close $fh
        return [list width $width height $height depth $depth color $color \
		compression $compression filter $filter interlace $interlace]
    }
    close $fh
    return
}

proc ::png::getTimestamp {file} {
    set fh [_openPNG $file]

    while {[set r [read $fh 8]] != ""} {
        binary scan $r Ia4 len type
        if {$type == "tIME"} {
            set r [read $fh [expr {$len + 4}]]
            binary scan $r Sccccc year month day hour minute second
            close $fh
            return [clock scan "$month/$day/$year $hour:$minute:$second" -gmt 1]
        }
        seek $fh [expr {$len + 4}] current
    }
    close $fh
    return
}

proc ::png::setTimestamp {file time} {
    set fh [_openPNG $file r+]
    
    set time [eval binary format Sccccc [string map {" 0" " "} [clock format $time -format "%Y %m %d %H %M %S" -gmt 1]]]
    if {![catch {package present crc32}]} {
        append time [binary format I [::crc::crc32 tIME$time]]
    } else {
        append time [binary format I 0]
    }

    while {[set r [read $fh 8]] != ""} {
        binary scan $r Ia4 len type
        if {[eof $fh]} { close $fh; return }
        if {$type == "tIME"} {
            seek $fh 0 current
            puts -nonewline $fh $time
            close $fh
            return
        }
        if {$type == "IDAT" && ![info exists idat]} { set idat [expr {[tell $fh] - 8}] }
        seek $fh [expr {$len + 4}] current
    }
    if {![info exists idat]} { close $fh; return -code error "no timestamp or data chunk found" }
    seek $fh $idat start
    set data [read $fh]
    seek $fh $idat start
    puts -nonewline $fh [binary format I 7]tIME$time$data
    close $fh
    return
}

proc ::png::getComments {file} {
    set fh [_openPNG $file]
    set text {}

    while {[set r [read $fh 8]] != ""} {
        binary scan $r Ia4 len type
        set pos [tell $fh]
        if {$type == "tEXt"} {
            set r [read $fh $len]
            lappend text [split $r \x00]
        } elseif {$type == "iTXt"} {
            set r [read $fh $len]
            set keyword [lindex [split $r \x00] 0]
            set r [string range $r [expr {[string length $keyword] + 1}] end]
            binary scan $r cc comp method
            if {$comp == 0} {
                lappend text [linsert [split [string range $r 2 end] \x00] 0 $keyword]
            }
        }
        seek $fh [expr {$pos + $len + 4}] start
    }
    close $fh
    return $text
}

proc ::png::removeComments {file} {
    set fh [_openPNG $file r+]
    set data "\x89PNG\r\n\x1a\n"
    while {[set r [read $fh 8]] != ""} {
        binary scan $r Ia4 len type
        if {$type == "zTXt" || $type == "iTXt" || $type == "tEXt"} {
            seek $fh [expr {$len + 4}] current
        } else {
            seek $fh -8 current
            append data [read $fh [expr {$len + 12}]]
        }
    }
    close $fh
    set fh [open $file w]
    fconfigure $fh -encoding binary -translation binary -eofchar {}
    puts -nonewline $fh $data
    close $fh
}

proc ::png::addComment {file keyword arg1 args} {
    if {[llength $args] > 0 && [llength $args] != 2} { close $fh; return -code error "wrong number of arguments" }
    set fh [_openPNG $file r+]

    if {[llength $args] > 0} {
        set comment "iTXt$keyword\x00\x00\x00$arg1\x00[encoding convertto utf-8 [lindex $args 0]]\x00[encoding convertto utf-8 [lindex $args 1]]"
    } else {
        set comment "tEXt$keyword\x00$arg1"
    }
    
    if {![catch {package present crc32}]} {
        append comment [binary format I [::crc::crc32 $comment]]
    } else {
        append comment [binary format I 0]
    }

    while {[set r [read $fh 8]] != ""} {
        binary scan $r Ia4 len type
        if {$type ==  "IDAT"} {
            seek $fh -8 current
            set pos [tell $fh]
            set data [read $fh]
            seek $fh $pos start
            set 1 [tell $fh]
            puts -nonewline $fh $comment
            set clen [binary format I [expr {[tell $fh] - $1 - 8}]]
            seek $fh $pos start
            puts -nonewline $fh $clen$comment$data
            close $fh
            return
        }
        seek $fh [expr {$len + 4}] current
    }
    close $fh
    return -code error "no data chunk found"
}


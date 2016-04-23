# pkgIndex.tcl - 
#
# uuid package index file
#
# $Id: pkgIndex.tcl,v 1.2 2005/09/30 05:36:39 andreas_kupries Exp $

if {![package vsatisfies [package provide Tcl] 8.2]} {return}
package ifneeded uuid 1.0.1 [list source [file join $dir uuid.tcl]]

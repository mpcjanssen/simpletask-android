## -*- tcl -*-
# ### ### ### ######### ######### #########

## Common information for slippy based maps. I.e. tile size,
## relationship between zoom level and map size, etc.

## See http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Pseudo-Code
## for the coordinate conversions and other information.

# ### ### ### ######### ######### #########
## Requisites

package require Tcl 8.4
package require snit
package require math::constants

# ### ### ### ######### ######### #########
## Implementation

namespace eval ::map::slippy {
    math::constants::constants pi radtodeg degtorad
}

snit::type map::slippy {
    # ### ### ### ######### ######### #########
    ## API

    typemethod length {level} {
	return [expr {$ourtilesize * [tiles $level]}]
    }

    typemethod tiles {level} {
	return [tiles $level]
    }

    typemethod {tile size} {} {
	return $ourtilesize
    }

    typemethod {tile valid} {tile levels {msgv {}}} {
	if {$msgv ne ""} { upvar 1 $msgv msg }

	# Bad syntax.

	if {[llength $tile] != 3} {
	    set msg "Bad tile <[join $tile ,]>, expected 3 elements (zoom, row, col)"
	    return 0
	}

	foreach {z r c} $tile break

	# Requests outside of the valid ranges are rejected
	# immediately, without even going to the filesystem or
	# provider.

	if {($z < 0) || ($z >= $levels)} {
	    set msg "Bad zoom level '$z' (max: $levels)"
	    return 0
	}

	set tiles [tiles $z]
	if {($r < 0) || ($r >= $tiles) ||
	    ($c < 0) || ($c >= $tiles)
	} {
	    set msg "Bad cell '$r $c' (max: $tiles)"
	    return 0
	}

	return 1
    }

    # Coordinate conversions.
    # geo   = zoom, latitude, longitude
    # tile  = zoom, row,      column
    # point = zoom, y,        x

    typemethod {geo 2tile} {geo} {
	::variable degtorad
	::variable pi
	foreach {zoom lat lon} $geo break 
	# lat, lon are in degrees.
	# The missing sec() function is computed using the 1/cos equivalency.
	set tiles  [tiles $zoom]
	set latrad [expr {$degtorad * $lat}]
	set row    [expr {int((1 - (log(tan($latrad) + 1.0/cos($latrad)) / $pi)) / 2 * $tiles)}]
	set col    [expr {int((($lon + 180.0) / 360.0) * $tiles)}]
	return [list $zoom $row $col]
    }

    typemethod {geo 2point} {geo} {
	::variable degtorad
	::variable pi
	foreach {zoom lat lon} $geo break 
	# Essence: [geo 2tile $geo] * $ourtilesize, with 'geo 2tile' inlined.
	set tiles  [tiles $zoom]
	set latrad [expr {$degtorad * $lat}]
	set y      [expr {$ourtilesize * ((1 - (log(tan($latrad) + 1.0/cos($latrad)) / $pi)) / 2 * $tiles)}]
	set x      [expr {$ourtilesize * ((($lon + 180.0) / 360.0) * $tiles)}]
	return [list $zoom $y $x]
    }

    typemethod {tile 2geo} {tile} {
	::variable radtodeg
	::variable pi
	foreach {zoom row col} $tile break
	# Note: For integer row/col the geo location is for the upper
	#       left corner of the tile. To get the geo location of
	#       the center simply add 0.5 to the row/col values.
	set tiles [tiles $zoom]
	set lat   [expr {$radtodeg * (atan(sinh($pi * (1 - 2 * $row / double($tiles)))))}]
	set lon   [expr {$col / double($tiles) * 360.0 - 180.0}]
	return [list $zoom $lat $lon]
    }

    typemethod {tile 2point} {tile} {
	foreach {zoom row col} $tile break
	# Note: For integer row/col the pixel location is for the
	#       upper left corner of the tile. To get the pixel
	#       location of the center simply add 0.5 to the row/col
	#       values.
	set tiles [tiles $zoom]
	set y     [expr {$tiles * $row}]
	set x     [expr {$tiles * $col}]
	return [list $zoom $y $x]
    }

    typemethod {point 2geo} {point} {
	::variable radtodeg
	::variable pi
	foreach {zoom y x} $point break
	set length [expr {$ourtilesize * [tiles $zoom]}]
	set lat    [expr {$radtodeg * (atan(sinh($pi * (1 - 2 * $y / $length))))}]
	set lon    [expr {$x / $length * 360.0 - 180.0}]
	return [list $zoom $lat $lon]
    }

    typemethod {point 2tile} {point} {
	foreach {zoom y x} $point break
	set tiles [tiles $zoom]
	set row   [expr {$y / $tiles}]
	set col   [expr {$x / $tiles}]
	return [list $zoom $y $x]
    }

    proc tiles {level} {
	return [expr {1 << $level}]
    }

    # ### ### ### ######### ######### #########
    ## Internal commands

    # ### ### ### ######### ######### #########
    ## State

    typevariable ourtilesize 256 ; # Size of slippy tiles <pixels>

    # ### ### ### ######### ######### #########
}

# ### ### ### ######### ######### #########
## Ready

package provide map::slippy 0.3

# special.tcl --
#    Provide well-known special mathematical functions
#
# This file contains a collection of tests for one or more of the Tcllib
# procedures.  Sourcing this file into Tcl runs the tests and
# generates output for errors.  No output means no errors were found.
#
# Copyright (c) 2004 by Arjen Markus. All rights reserved.
#
# RCS: @(#) $Id: special.tcl,v 1.13 2008/08/13 07:28:47 arjenmarkus Exp $
#
package require math
package require math::constants
package require math::statistics

# namespace special
#    Create a convenient namespace for the "special" mathematical functions
#
namespace eval ::math::special {
    #
    # Define a number of common mathematical constants
    #
    ::math::constants::constants pi
    variable halfpi [expr {$pi/2.0}]

    #
    # Functions defined in other math submodules
    #
    if { [info commands Beta] == {} } {
       namespace import ::math::Beta
       namespace import ::math::ln_Gamma
    }

    #
    # Export the various functions
    #
    namespace export Beta ln_Gamma Gamma erf erfc fresnel_C fresnel_S sinc
}

# Gamma --
#    The Gamma function - synonym for "factorial"
#
proc ::math::special::Gamma {x} {
    if { [catch { expr {exp( [ln_Gamma $x] )} } result] } {
        return -code error -errorcode $::errorCode $result
    }
    return $result
}

# erf --
#    The error function
# Arguments:
#    x          The value for which the function must be evaluated
# Result:
#    erf(x)
# Note:
#    The algoritm used is due to George Marsaglia
#    See: http://www.velocityreviews.com/forums/t317358-erf-function-in-c.html
#    I did not want to copy and convert the even more accurate but
#    rather lengthy algorithm used by lcc-win32/Sun
#
proc ::math::special::erf {x} {
    set x    [expr {$x*sqrt(2.0)}]

    if { $x >  10.0 } { return  1.0 }
    if { $x < -10.0 } { return -1.0 }

    set a    1.2533141373155
    set b   -1.0
    set pwr  1.0
    set t    0.0
    set z    0.0

    set s [expr {$a+$b*$x}]

    set i 2
    while { $s != $t } {
        set a   [expr {($a+$z*$b)/double($i)}]
        set b   [expr {($b+$z*$a)/double($i+1)}]
        set pwr [expr {$pwr*$x*$x}]
        set t   $s
        set s   [expr {$s+$pwr*($a+$x*$b)}]

        incr i 2
   }

   return [expr {1.0-2.0*$s*exp(-0.5*$x*$x-0.9189385332046727418)}]
}



# erfc --
#    The complement of the error function
# Arguments:
#    x          The value for which the function must be evaluated
# Result:
#    erfc(x) = 1.0-erf(x)
#
proc ::math::special::erfc {x} {
    set x    [expr {$x*sqrt(2.0)}]

    if { $x >  10.0 } { return  0.0 }
    if { $x < -10.0 } { return  0.0 }

    set a    1.2533141373155
    set b   -1.0
    set pwr  1.0
    set t    0.0
    set z    0.0

    set s [expr {$a+$b*$x}]

    set i 2
    while { $s != $t } {
        set a   [expr {($a+$z*$b)/double($i)}]
        set b   [expr {($b+$z*$a)/double($i+1)}]
        set pwr [expr {$pwr*$x*$x}]
        set t   $s
        set s   [expr {$s+$pwr*($a+$x*$b)}]

        incr i 2
   }

   return [expr {2.0*$s*exp(-0.5*$x*$x-0.9189385332046727418)}]
}


# ComputeFG --
#    Compute the auxiliary functions f and g
#
# Arguments:
#    x            Parameter of the integral (x>=0)
# Result:
#    Approximate values for f and g
# Note:
#    See Abramowitz and Stegun. The accuracy is 2.0e-3.
#
proc ::math::special::ComputeFG {x} {
    list [expr {(1.0+0.926*$x)/(2.0+1.792*$x+3.104*$x*$x)}] \
        [expr {1.0/(2.0+4.142*$x+3.492*$x*$x+6.670*$x*$x*$x)}]
}

# fresnel_C --
#    Compute the Fresnel cosine integral
#
# Arguments:
#    x            Parameter of the integral (x>=0)
# Result:
#    Value of C(x) = integral from 0 to x of cos(0.5*pi*x^2)
# Note:
#    This relies on a rational approximation of the two auxiliary functions f and g
#
proc ::math::special::fresnel_C {x} {
    variable halfpi
    if { $x < 0.0 } {
        error "Domain error: x must be non-negative"
    }

    if { $x == 0.0 } {
        return 0.0
    }

    foreach {f g} [ComputeFG $x] {break}

    set xarg [expr {$halfpi*$x*$x}]

    return [expr {0.5+$f*sin($xarg)-$g*cos($xarg)}]
}

# fresnel_S --
#    Compute the Fresnel sine integral
#
# Arguments:
#    x            Parameter of the integral (x>=0)
# Result:
#    Value of S(x) = integral from 0 to x of sin(0.5*pi*x^2)
# Note:
#    This relies on a rational approximation of the two auxiliary functions f and g
#
proc ::math::special::fresnel_S {x} {
    variable halfpi
    if { $x < 0.0 } {
        error "Domain error: x must be non-negative"
    }

    if { $x == 0.0 } {
        return 0.0
    }

    foreach {f g} [ComputeFG $x] {break}

    set xarg [expr {$halfpi*$x*$x}]

    return [expr {0.5-$f*cos($xarg)-$g*sin($xarg)}]
}

# sinc --
#    Compute the sinc function
# Arguments:
#    x       Value of the argument
# Result:
#    sin(x)/x
#
proc ::math::special::sinc {x} {
    if { $x == 0.0 } {
        return 1.0
    } else {
        return [expr {sin($x)/$x}]
    }
}

# Bessel functions and elliptic integrals --
#
source [file join [file dirname [info script]] "bessel.tcl"]
source [file join [file dirname [info script]] "classic_polyns.tcl"]
source [file join [file dirname [info script]] "elliptic.tcl"]
source [file join [file dirname [info script]] "exponential.tcl"]

package provide math::special 0.2.2

if {![package vsatisfies [package provide Tcl] 8.2]} {return}
package ifneeded doctools            1.4.11 [list source [file join $dir doctools.tcl]]
package ifneeded doctools::toc       1.1.3  [list source [file join $dir doctoc.tcl]]
package ifneeded doctools::idx       1.0.4  [list source [file join $dir docidx.tcl]]
package ifneeded doctools::cvs       1      [list source [file join $dir cvs.tcl]]
package ifneeded doctools::changelog 1      [list source [file join $dir changelog.tcl]]

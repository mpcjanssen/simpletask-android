if {![package vsatisfies [package provide Tcl] 8.4]} {return}
package ifneeded map::slippy             0.3 [list source [file join $dir map_slippy.tcl]]
package ifneeded map::slippy::fetcher    0.2 [list source [file join $dir map_slippy_fetcher.tcl]]
package ifneeded map::slippy::cache      0.2 [list source [file join $dir map_slippy_cache.tcl]]


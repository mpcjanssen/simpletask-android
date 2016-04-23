if {![package vsatisfies [package provide Tcl] 8.2]} {
    # PRAGMA: returnok
    return
}
package ifneeded tar 0.7 [list source [file join $dir tar.tcl]]

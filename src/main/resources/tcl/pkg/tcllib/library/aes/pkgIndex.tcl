if {![package vsatisfies [package provide Tcl] 8.2]} {
    # PRAGMA: returnok
    return
}
package ifneeded aes 1.0.2 [list source [file join $dir aes.tcl]]

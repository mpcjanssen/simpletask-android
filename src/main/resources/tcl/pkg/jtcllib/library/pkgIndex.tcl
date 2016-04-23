# Tcl package index file, version 1.1
# jtcllib version 1.1

# All jtcllib packages need Tcl 8 (use [namespace])
if {![package vsatisfies [package provide Tcl] 8]} {return}

# Extend the auto_path to make jtcllib packages available
if {[lsearch -exact $::auto_path $dir] == -1} {
    lappend ::auto_path $dir
}

set maindir $dir
set dir [file join $maindir hyde] ;	 source [file join $dir pkgIndex.tcl]
set dir [file join $maindir ziplib] ;	 source [file join $dir pkgIndex.tcl]
set dir [file join $maindir fleet] ;	 source [file join $dir pkgIndex.tcl]
unset maindir


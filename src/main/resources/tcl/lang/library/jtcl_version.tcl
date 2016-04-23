# fake package that provides JTcl version,
# project.version substituted during build


if { [ catch {
    if {[regexp {^[2-9]} "${project.version}"]} {
        package provide JTcl "${project.version}"	
    } else {
        package provide JTcl 0.0	
    }
  }]} {
    # default if project.version not substituted
    package provide JTcl 0.0	
}



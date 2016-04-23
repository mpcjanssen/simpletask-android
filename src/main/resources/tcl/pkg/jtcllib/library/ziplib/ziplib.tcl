package provide ziplib 1.0
package require java

namespace eval ::ziplib { }

# open a zip file for reading, returns java.util.zip.ZipInputStream 
proc ::ziplib::openInputZip {fileName} {
    java::try {
        set fd [java::new java.io.BufferedInputStream [java::new java.io.FileInputStream $fileName]]
        set zip [java::new java.util.zip.ZipInputStream $fd]
    } catch {Exception err} {
        error "could not open file \"$fileName\": [$err getMessage]"
    }
    set ent [java::null]
    java::try {
        set ent [$zip getNextEntry]
    } catch {Exception err} {
        close $fd
        error "could not open file \"$fileName\": [$err getMessage]"
    }
    $fd close
    if {[java::isnull $ent]} {
        error "could not open file \"$fileName\": file is not a zip file"
    }
    set fd [java::new java.io.BufferedInputStream [java::new java.io.FileInputStream $fileName]]
    set zip [java::new java.util.zip.ZipInputStream $fd]
    return $zip
}

# open a new zip file for writing, returns java.util.zip.ZipOutputStream 
proc ::ziplib::openOutputZip {fileName} {
    java::try {
        set fd [java::new java.io.BufferedOutputStream [java::new java.io.FileOutputStream $fileName]]
    } catch {Exception err} {
        error "could not open file \"$fileName\": [$err getMessage]"
    }
    set zip [java::new java.util.zip.ZipOutputStream $fd]
    return $zip
}

# copies an open input zip to an open output zip, with option list of regex to exclude
proc ::ziplib::copyZip {zipin zipout {excludeReList {}}} {
    while {! [java::isnull [set entry [$zipin getNextEntry]]]} {
        set fileName [$entry getName]
        set exclude 0
        foreach excludeRe $excludeReList  {
            if {[string length $excludeRe] && [regexp -- $excludeRe $fileName]} {
                set exclude 1
                break
            }
        }
        if {! $exclude} {
            $zipout putNextEntry $entry
            copyStream $zipin $zipout
            $zipout closeEntry
        }
    }
}

# copy an onput input stream to an output stream
proc ::ziplib::copyStream {in out} {
    set buff [java::new {byte[]} 32768]
    while {[set len [$in read $buff]] != -1} {
        $out write $buff 0 $len
    }
}

# copy an open input zip to an output directory
proc ::ziplib::unzipToDir {zipin dir} {
    if {[file exists $dir]} {
        if {! [file isdirectory $dir]} {
            error "directory \"$dir\" is not a directory"
        }
    } else {
        file mkdir $dir
    }
    set fileDirObj [java::new java.io.File $dir]
    while {! [java::isnull [set entry [$zipin getNextEntry]]]} {
        set fileName [$entry getName]
        set fdObj [java::new java.io.File $fileDirObj $fileName]
        if {[$entry isDirectory]} {
            if {[$fdObj exists]} {
                if {! [$fdObj isDirectory]} {
                   error "\"[$fdObj toString]\" is not a directory"
                }
            } else {
                $fdObj mkdirs
            }
        } else {
            # make sure all directories exists in path
            set parentDir [$fdObj getParentFile]
            if {[$parentDir exists]} {
                if {! [$parentDir isDirectory]} {
                    error "\"[$parentDir toString]\" is not a directory"
                } else {
                    $parentDir mkdirs
                }
            } else {
                $parentDir mkdirs
            }
            set fdout [java::new {java.io.FileOutputStream java.io.File} $fdObj]
            copyStream $zipin $fdout
            $fdout close
        }
    }
}

# make a new zip directory in an open output zip
proc ::ziplib::mkZipDir {zipout zipdir} {
    set zipdir [string trim $zipdir /]
    if {! [string length $zipdir]} {
        return
    }
    set entry [java::new java.util.zip.ZipEntry $zipdir/]
    $entry setSize 0
    $entry setCompressedSize 0
    $entry setMethod [java::field java.util.zip.ZipEntry STORED]
    $entry setCrc 0
    $zipout putNextEntry $entry
}

# make a new zip file entry
proc ::ziplib::mkZipFile {zipout fileName} {
    set entry [java::new java.util.zip.ZipEntry $fileName]
    $zipout putNextEntry $entry
}

# copy the contents of a directory into an open output zip
proc ::ziplib::zipFromDir {zipout dir {zipTopLevel {}}} {

    if {[string length $dir] && ! [file isdirectory $dir]} {
        error "\$dir"\" is not a directory"
    }
    if {[string range $zipTopLevel 0 2] eq "../"} {
        error "invalid ziptoplevel directory \"$zipTopLevel\""
    }
    set zipTopLevel [string trim $zipTopLevel /]
    if {$zipTopLevel eq "."} {
        set zipTopLevel ""
    }
    if {[string length $zipTopLevel]} {
        mkZipDir $zipout $zipTopLevel
    }

    set files [lsort [concat [glob [file join $dir .*]] [glob [file join $dir *]]]]

    set dirs [list]
    foreach fileName $files {
        set entryName [file tail $fileName]
        if {$entryName eq "." || $entryName eq ".."} {
            continue
        }
        if {[file isdirectory $fileName]} {
            lappend dirs $entryName
            continue
        }
        set fd [java::new java.io.BufferedInputStream [java::new java.io.FileInputStream $fileName]]
        set entry [java::new java.util.zip.ZipEntry [file join $zipTopLevel $entryName]]
        $zipout putNextEntry $entry
        copyStream $fd $zipout
        $fd close
    }

    foreach fileName $dirs {
        zipFromDir $zipout [file join $dir $fileName] [file join $zipTopLevel $fileName]
    }
}

# find the location of a java class, return a URL formatted string
# or just the path name if the location is a jar file or directory
proc ::ziplib::getClassLocation {class} {
    set clazz [$class  getClass]
    set pd [$clazz getProtectionDomain]
    set cs [$pd getCodeSource]
    set loc [[$cs getLocation] toString]
    if {[string range $loc 0 4] eq "file:"} {
        set loc [string range $loc 5 end]
    }
    return $loc
}

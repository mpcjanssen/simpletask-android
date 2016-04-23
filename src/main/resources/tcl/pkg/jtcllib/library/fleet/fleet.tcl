package provide fleet 0.1
package require java
java::load tcl.pkg.fleet.FleetExt

namespace eval ::fleet {}

proc ::fleet::initResults {reply} {
   set status [dict get $reply status]
   set value [dict get $reply value]
}

proc ::fleet::processReply {reply} {
   set status [dict get $reply status]
   set value [dict get $reply value]
   if {$status eq "FAIL"} {
        puts $value
   } else {
       set fleet [dict get $reply fleet]
       upvar #0 ::fleet::${fleet}::pars pars
       set memberName [dict get $reply member]
       set value [dict get $reply value]
       $pars(calcProc) $value
       incr pars(nResults) 
       if {$pars(nResults) == $pars(nMessages)} {
            $pars(doneProc)
       } else {
           incr pars($memberName,nResults)
           set count [expr {$pars($memberName,nSent)-$pars($memberName,nResults)}]
           if {$count < $pars(lowWater)} {
               set newCount [expr {$pars(highWater)-$count}]
               #sendMessagesToMember $fleet $memberName $newCount
               sendMessages $fleet
           }
       }
   }
}
proc ::fleet::sendMessages {fleet} {
    upvar #0 ::fleet::${fleet}::pars pars
    if {$pars(messageNum) >= $pars(nMessages)} {
         return 1
    }
    for {set i 0} {$i < $pars(nMembers)} {incr i} {
        set member $pars(members,$i)
        set count [expr {$pars($member,nSent)-$pars($member,nResults)}]
        if {$count < $pars(lowWater)} {
            set newCount [expr {$pars(highWater)-$count}]
            sendMessagesToMember $fleet $member $newCount
        }
    }
}

proc ::fleet::sendMessagesToMember {fleet member newCount} {
    upvar #0 ::fleet::${fleet}::pars pars
    for {set j 0} {$j < $newCount} {incr j} {
        if {$pars(messageNum) >= $pars(nMessages)} {
             return 1
        }
        incr pars($member,nSent)
        $pars(messageProc) $pars(fleet) $member $pars(messageNum)
        incr pars(messageNum)
    }
}

proc ::fleet::initFleet {nMembers {script {}} } {
   set fleet [fleet create]
   upvar #0 ::fleet::${fleet}::pars pars
   array set pars {
       nMembers 2
       lowWater 50
       highWater 100
       nMessages 10000
       updateAt 500
       nResults 0
       messageNum 0
       messageProc messageProc
       calcProc calcProc
       doneProc reportProc
   }
   set pars(nMembers) $nMembers
   set pars(fleet) $fleet

   for {set i 0} {$i < $pars(nMembers)} {incr i} {
       set member [$pars(fleet) member]
       set pars(members,$i) $member
       set pars($member,nSent) 0
       set pars($member,nResults) 0
       if {$script ne {}} {
           $fleet tell $pars(members,$i) $script -reply ::fleet::initResults
       }
   }
   return $fleet
}

proc ::fleet::reset {fleet} {
   upvar #0 ::fleet::${fleet}::pars pars
   for {set i 0} {$i < $pars(nMembers)} {incr i} {
       set member $pars(members,$i)
       set pars($member,nSent) 0
       set pars($member,nResults) 0
   }
   set pars(nResults) 0
   set pars(messageNum) 0
}

proc ::fleet::jproc {fleet args} {
    eval ::hyde::jproc $args
    set procName [lindex $args 1]
    set procArgs [info args $procName]
    set procBody [info body $procName]
    set bytes $::hyde::cacheCode(hyde/${procName}Cmd)
    $fleet tell * "package require java"
    $fleet tell * "java::defineclass $bytes"
    $fleet tell * [list proc $procName $procArgs $procBody]
}

proc ::fleet::configure {fleet args} {
   upvar #0 ::fleet::${fleet}::pars pars
   foreach "name value" $args {
       set name [string trimleft $name "-"]
       set pars($name) $value
   } 
}

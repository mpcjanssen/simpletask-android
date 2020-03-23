package com.thegrizzlylabs.sardineandroid.model;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root
public class CurrentUserPrivilegeSet {

    @ElementList(inline = true)
    public List<Privilege> privileges;
}

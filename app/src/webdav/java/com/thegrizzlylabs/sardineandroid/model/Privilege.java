package com.thegrizzlylabs.sardineandroid.model;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementListUnion;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root(strict = false)
@Namespace(prefix = "D", reference = "DAV:")
public class Privilege {

	@ElementListUnion({
			@ElementList(entry="read", inline=true, type=Read.class),
			@ElementList(entry="write", inline=true, type=Write.class),
			@ElementList(entry="write-properties", inline=true, type=WriteProperties.class),
			@ElementList(entry="write-content", inline=true, type=WriteContent.class),
			@ElementList(entry="unlock", inline=true, type=Unlock.class),
			@ElementList(entry="read-acl", inline=true, type=ReadAcl.class),
			@ElementList(entry="write-acl", inline=true, type=WriteAcl.class),
			@ElementList(entry="bind", inline=true, type=Bind.class),
			@ElementList(entry="unbind", inline=true, type=UnBind.class),
			@ElementList(entry="read-current-user-privilege-set", inline=true, type=ReadCurrentUserPrivilegeSet.class),
			@ElementList(entry="all", inline=true, type=All.class),

			// Added to support Jianguoyun WebDAV server
			@ElementList(entry="write_acl", inline=true, type=WriteAcl.class),
			@ElementList(entry="read_acl", inline=true, type=ReadAcl.class),
	})
	private List<SimplePrivilege> content;

	public List<SimplePrivilege> getContent() {
		if (content == null) {
			content = new ArrayList<>();
		}
		return content;
	}

	public void setContent(List<SimplePrivilege> content) {
		this.content = content;
	}

}

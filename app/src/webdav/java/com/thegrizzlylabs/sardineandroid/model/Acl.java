package com.thegrizzlylabs.sardineandroid.model;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

import java.util.List;


/**
 * <p>Java class for anonymous complex type.</p>
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>
 *   &lt;D:owner&gt; 
 *        &lt;D:href&gt;http://www.example.com/acl/users/gstein&lt;/D:href&gt;
 *      &lt;/D:owner&gt;
 * </pre>
 * 
 * 
 * 
 */
@Root
@Namespace(prefix = "D", reference = "DAV:")
public class Acl {

	@ElementList(inline = true, required = false)
	private List<Ace> ace;

	public List<Ace> getAce() {
		return ace;
	}

	public void setAce(List<Ace> ace) {
		this.ace = ace;
	}
}

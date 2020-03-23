package com.thegrizzlylabs.sardineandroid.model;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

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
public class Ace {

	@Element(required = false)
	private Principal principal;

	@Element(required = false)
	private Grant grant;

	@Element(required = false)
	private Deny deny;

	@Element(required = false)
	private Inherited inherited;

    @Element(name = "protected", required = false)
	private Protected protected1;
    
	public Principal getPrincipal() {
		return principal;
	}
	public void setPrincipal(Principal principal) {
		this.principal = principal;
	}
	public Grant getGrant() {
		return grant;
	}
	public void setGrant(Grant grant) {
		this.grant = grant;
	}
	public Deny getDeny() {
		return deny;
	}
	public void setDeny(Deny deny) {
		this.deny = deny;
	}
	public Inherited getInherited() {
		return inherited;
	}
	public void setInherited(Inherited inherited) {
		this.inherited = inherited;
	}
	public Protected getProtected() {
		return protected1;
	}
	public void setProtected(Protected protected1) {
		this.protected1 = protected1;
	}
	
	
}

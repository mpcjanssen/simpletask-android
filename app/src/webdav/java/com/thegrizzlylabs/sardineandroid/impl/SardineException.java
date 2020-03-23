/*
 * Copyright 2009-2011 Jon Stevens et al.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thegrizzlylabs.sardineandroid.impl;

import java.io.IOException;
import java.util.Locale;

/**
 * Specialized type of exception for Sardine so
 * that it is easy to get the error information from it.
 *
 * @author jonstevens
 */
public class SardineException extends IOException
{
	private static final long serialVersionUID = -3900043433469104564L;

	private final int statusCode;
	private String responsePhrase;

	/**
	 * @param msg            Custom description of failure
	 * @param statusCode     Error code returned by server
	 * @param responsePhrase Response phrase following the error code
	 */
	public SardineException(String msg, int statusCode, String responsePhrase) {
		super(msg);
		this.statusCode = statusCode;
		this.responsePhrase = responsePhrase;
	}

	/**
	 * The response phrase returned by the server.
	 *
	 * @return Null if not known.
	 */
	public String getResponsePhrase()
	{
		return this.responsePhrase;
	}

	public int getStatusCode() {
		return this.statusCode;
	}

	@Override
	public String getMessage() {
		return String.format(Locale.US, "%s (%d %s)", super.getMessage(), this.getStatusCode(), this.getResponsePhrase());
	}
}
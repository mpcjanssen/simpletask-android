package com.thegrizzlylabs.sardineandroid.report;

import com.thegrizzlylabs.sardineandroid.model.Multistatus;
import com.thegrizzlylabs.sardineandroid.util.SardineUtil;

import java.io.IOException;

public abstract class SardineReport<T>
{
	public String toXml() throws IOException
	{
		return SardineUtil.toXml(toJaxb());
	}

	public abstract Object toJaxb();

	public abstract T fromMultistatus(Multistatus multistatus);
}

/*
 * Copyright 2018 datagear.tech
 *
 * Licensed under the LGPLv3 license:
 * http://www.gnu.org/licenses/lgpl-3.0.html
 */

package org.datagear.persistence.support.dialect;

import java.sql.Connection;

import org.datagear.connection.URLSensor;
import org.datagear.connection.support.OracleURLSensor;
import org.datagear.persistence.Dialect;
import org.datagear.persistence.DialectBuilder;
import org.datagear.persistence.support.AbstractURLSensedDialectBuilder;

/**
 * Oracle的{@linkplain DialectBuilder}。
 * 
 * @author datagear@163.com
 *
 */
public class OracleDialectBuilder extends AbstractURLSensedDialectBuilder
{
	public OracleDialectBuilder()
	{
		super(new OracleURLSensor());
	}

	@Override
	public void setUrlSensor(URLSensor urlSensor)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Dialect build(Connection cn)
	{
		return new OracleDialect(getIdentifierQuote(cn));
	}
}

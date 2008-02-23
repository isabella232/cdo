/***************************************************************************
 * Copyright (c) 2004 - 2008 Martin Taal
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Martin Taal - copied from CDORevisionPropertyHandler and adapted
 **************************************************************************/
package org.eclipse.emf.cdo.server.internal.hibernate.tuplizer;

import org.eclipse.emf.cdo.protocol.id.CDOID;
import org.eclipse.emf.cdo.server.internal.hibernate.CDOHibernateUtil;

import org.hibernate.HibernateException;

/**
 * @author Martin Taal
 */
public class CDOReferenceGetter extends CDOPropertyGetter
{
  private static final long serialVersionUID = 1L;

  public CDOReferenceGetter(CDORevisionTuplizer tuplizer, String propertyName)
  {
    super(tuplizer, propertyName);
  }

  @Override
  public Object get(Object target) throws HibernateException
  {
    final Object o = super.get(target);
    if (o instanceof CDOID)
    {
      return CDOHibernateUtil.getInstance().getCDORevision((CDOID)o);
    }
    return o;
  }
}

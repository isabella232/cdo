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

import org.eclipse.emf.cdo.internal.protocol.revision.InternalCDORevision;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * @author Martin Taal
 */
public class CDOVersionPropertySetter extends CDOPropertySetter
{
  private static final long serialVersionUID = 1L;

  public CDOVersionPropertySetter(CDORevisionTuplizer tuplizer, String propertyName)
  {
    super(tuplizer, propertyName);
  }

  @Override
  public void set(Object target, Object value, SessionFactoryImplementor factory) throws HibernateException
  {
    final InternalCDORevision revision = (InternalCDORevision)target;
    revision.setVersion(((Number)value).intValue());
    if (!isVirtualProperty())
    {
      super.set(target, value, factory);
    }
  }

  @Override
  protected boolean isVirtualPropertyAllowed()
  {
    return true;
  }
}

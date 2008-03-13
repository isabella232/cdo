/***************************************************************************
 * Copyright (c) 2004 - 2008 Martin Taal and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Martin Taal - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.cdo.server.hibernate;

import org.hibernate.cfg.Configuration;

/**
 * A mappingprovider adds a hibernate mapping to a hibernate configuration object.
 * 
 * @author Martin Taal
 */
public interface IHibernateMappingProvider
{

  /** Adds a mapping to a configuration object */
  public void addMapping(Configuration configuration);

  /** Sets the Store in the mapping provider, is called before addMapping. */
  public void setHibernateStore(IHibernateStore hibernateStore);
}

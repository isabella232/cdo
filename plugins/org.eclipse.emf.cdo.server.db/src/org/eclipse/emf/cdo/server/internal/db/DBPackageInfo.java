/***************************************************************************
 * Copyright (c) 2004 - 2007 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.cdo.server.internal.db;

import org.eclipse.net4j.db.IDBSchema;

/**
 * @author Eike Stepper
 */
public final class DBPackageInfo extends DBInfo
{
  private IDBSchema schema;

  public DBPackageInfo(int id)
  {
    super(id);
  }

  public IDBSchema getSchema()
  {
    return schema;
  }

  public void setSchema(IDBSchema schema)
  {
    if (this.schema != schema)
    {
      if (this.schema != null)
      {
        throw new IllegalStateException("Schema " + schema + "is already set");
      }

      this.schema = schema;
    }
  }
}

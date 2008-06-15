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
package org.eclipse.emf.cdo.server.internal.hibernate;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.server.IStoreWriter.CommitContext;

import java.util.HashMap;

/**
 * A HibernateCommitContext contains the commitcontext as well as support for direct (hashmap) based search for a new or
 * changed object using the id.
 * 
 * @author Martin Taal
 */
public class HibernateCommitContext
{
  private CommitContext commitContext;

  private HashMap<CDOID, CDORevision> dirtyObjects = null;

  private HashMap<CDOID, CDORevision> newObjects = null;

  public CommitContext getCommitContext()
  {
    return commitContext;
  }

  public void setCommitContext(CommitContext commitContext)
  {
    this.commitContext = commitContext;
  }

  protected void initialize()
  {

    if (dirtyObjects != null)
    {
      return;
    }

    dirtyObjects = new HashMap<CDOID, CDORevision>();
    for (CDORevision cdoRevision : commitContext.getDirtyObjects())
    {
      dirtyObjects.put(cdoRevision.getID(), cdoRevision);
    }

    newObjects = new HashMap<CDOID, CDORevision>();

    for (CDORevision cdoRevision : commitContext.getNewObjects())
    {
      newObjects.put(cdoRevision.getID(), cdoRevision);
    }

  }

  public CDORevision getDirtyObject(CDOID id)
  {
    initialize();
    return dirtyObjects.get(id);
  }

  public CDORevision getNewObject(CDOID id)
  {
    initialize();
    return newObjects.get(id);
  }

  public void setNewID(CDOID oldId, CDOID newId)
  {
    initialize();
    CDORevision cdoRevision;
    if ((cdoRevision = dirtyObjects.get(oldId)) != null)
    {
      dirtyObjects.remove(oldId);
      dirtyObjects.put(newId, cdoRevision);
      return;
    }

    if ((cdoRevision = newObjects.get(oldId)) != null)
    {
      newObjects.remove(oldId);
      newObjects.put(newId, cdoRevision);
      return;
    }
  }
}

/*
 * Copyright (c) 2009-2015 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.explorer.ui.repositories;

import org.eclipse.emf.cdo.explorer.repositories.CDORepository;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Eike Stepper
 */
public class RepositoryDeleteHandler extends RepositoryHandler
{
  public RepositoryDeleteHandler()
  {
    super(null, null);
  }

  @Override
  protected void doExecute(IProgressMonitor progressMonitor) throws Exception
  {
    for (CDORepository repository : elements)
    {
      repository.delete();
    }
  }
}

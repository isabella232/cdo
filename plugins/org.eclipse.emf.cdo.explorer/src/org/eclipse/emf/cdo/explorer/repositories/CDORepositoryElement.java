/*
 * Copyright (c) 2010-2012 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.explorer.repositories;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.util.CDOTimeProvider;

/**
 * An element of a CDO repository.
 *
 * @author Eike Stepper
 * @since 4.4
 * @apiviz.landmark
 */
public interface CDORepositoryElement extends CDOTimeProvider
{
  public CDORepository getRepository();

  public int getBranchID();

  public long getTimeStamp();

  public CDOID getObjectID();
}

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
package org.eclipse.net4j.internal.util.om.monitor;

import org.eclipse.net4j.util.om.monitor.OMSubMonitor;

/**
 * @author Eike Stepper
 */
public class NullMonitor implements InternalOMMonitor
{
  public NullMonitor()
  {
  }

  public void fork(Runnable runnable)
  {
  }

  public void fork(int workFromParent, Runnable runnable)
  {
  }

  public void fork(Runnable runnable, String msg)
  {
  }

  public void fork(int workFromParent, Runnable runnable, String msg)
  {
  }

  public OMSubMonitor fork()
  {
    return this;
  }

  public OMSubMonitor fork(int workFromParent)
  {
    return this;
  }

  public String getTask()
  {
    return null;
  }

  public void setTask(String task)
  {
  }

  public void worked()
  {
  }

  public void worked(int work)
  {
  }

  public void worked(String msg)
  {
  }

  public void worked(int work, String msg)
  {
  }

  public void join()
  {
  }

  public void join(String msg)
  {
  }

  public int getTotalWork()
  {
    return 0;
  }

  public boolean hasBegun()
  {
    return true;
  }

  public void message(String msg)
  {
  }
}

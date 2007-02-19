/***************************************************************************
 * Copyright (c) 2004-2007 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.net4j.util.lifecycle;

import org.eclipse.net4j.util.ReflectUtil;
import org.eclipse.net4j.util.om.trace.ContextTracer;

import org.eclipse.internal.net4j.bundle.Net4j;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Eike Stepper
 */
public class LifecycleImpl implements Lifecycle, LifecycleNotifier
{
  public static boolean USE_LABEL = true;

  private static final ContextTracer TRACER = new ContextTracer(Net4j.DEBUG_LIFECYCLE, LifecycleImpl.class);

  private static final ContextTracer DUMPER = new ContextTracer(Net4j.DEBUG_LIFECYCLE_DUMP, LifecycleImpl.class);

  private boolean active;

  /**
   * Don't initialize lazily to circumvent synchronization!
   */
  private Queue<LifecycleListener> listeners = new ConcurrentLinkedQueue();

  protected LifecycleImpl()
  {
  }

  public final void addLifecycleListener(LifecycleListener listener)
  {
    listeners.add(listener);
  }

  public final void removeLifecycleListener(LifecycleListener listener)
  {
    listeners.remove(listener);
  }

  public final synchronized void activate() throws LifecycleException
  {
    if (!active)
    {
      if (TRACER.isEnabled())
      {
        TRACER.trace("Activating " + this);//$NON-NLS-1$
      }

      try
      {
        onAboutToActivate();
      }
      catch (RuntimeException ex)
      {
        throw ex;
      }
      catch (Exception ex)
      {
        throw new LifecycleException(ex);
      }

      fireLifecycleAboutToActivate();
      if (DUMPER.isEnabled())
      {
        DUMPER.trace("DUMP" + ReflectUtil.toString(this)); //$NON-NLS-1$
      }

      try
      {
        onActivate();
      }
      catch (RuntimeException ex)
      {
        throw ex;
      }
      catch (Exception ex)
      {
        throw new LifecycleException(ex);
      }
      
      active = true;
      fireLifecycleActivated();
    }
  }

  public final synchronized Exception deactivate()
  {
    if (active)
    {
      if (TRACER.isEnabled())
      {
        TRACER.trace("Deactivating " + this);//$NON-NLS-1$
      }

      fireLifecycleDeactivating();

      try
      {
        onDeactivate();
      }
      catch (Exception ex)
      {
        if (TRACER.isEnabled())
        {
          TRACER.trace(ex);
        }

        return ex;
      }
      finally
      {
        active = false;
      }
    }

    return null;
  }

  public final boolean isActive()
  {
    return active;
  }

  @Override
  public String toString()
  {
    if (USE_LABEL)
    {
      return ReflectUtil.getLabel(this);
    }
    else
    {
      return super.toString();
    }
  }

  protected void fireLifecycleAboutToActivate()
  {
    for (LifecycleListener listener : listeners)
    {
      try
      {
        listener.notifyLifecycleAboutToActivate(this);
      }
      catch (Exception ex)
      {
        if (TRACER.isEnabled())
        {
          TRACER.trace(ex);
        }
      }
    }
  }

  protected void fireLifecycleActivated()
  {
    for (LifecycleListener listener : listeners)
    {
      try
      {
        listener.notifyLifecycleActivated(this);
      }
      catch (Exception ex)
      {
        if (TRACER.isEnabled())
        {
          TRACER.trace(ex);
        }
      }
    }
  }

  protected void fireLifecycleDeactivating()
  {
    for (LifecycleListener listener : listeners)
    {
      try
      {
        listener.notifyLifecycleDeactivating(this);
      }
      catch (Exception ex)
      {
        if (TRACER.isEnabled())
        {
          TRACER.trace(ex);
        }
      }
    }
  }

  protected void onAboutToActivate() throws Exception
  {
  }

  protected void onActivate() throws Exception
  {
  }

  protected void onDeactivate() throws Exception
  {
  }
}

/***************************************************************************
 * Copyright (c) 2004, 2005, 2006 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.net4j.signal;

/**
 * @author Eike Stepper
 */
public abstract class SignalReactor extends Signal
{
  protected SignalReactor()
  {
  }

  @Override
  public String toString()
  {
    return "SignalReactor[" + getSignalID() + ", " + getProtocol() + ", correlation=" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        + getCorrelationID() + "]"; //$NON-NLS-1$
  }
}

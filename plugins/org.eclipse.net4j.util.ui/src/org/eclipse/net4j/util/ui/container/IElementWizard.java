/**
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.net4j.util.ui.container;

import org.eclipse.swt.widgets.Composite;

/**
 * @author Eike Stepper
 * @since 3.1
 */
public interface IElementWizard
{
  public String getResultDescription();

  public void create(Composite parent, String factoryType, String defaultDescription);
}

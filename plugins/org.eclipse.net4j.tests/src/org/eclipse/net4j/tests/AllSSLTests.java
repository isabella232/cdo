/*
 * Copyright (c) 2011, 2012, 2015 Eike Stepper (Loehne, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Teerawat Chaiyakijpichet (No Magic Asia Ltd.) - initial API and implementation
 */
package org.eclipse.net4j.tests;

import org.eclipse.net4j.tests.config.Net4jTestSuite;
import org.eclipse.net4j.tests.config.TestConfig.SSL;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Teerawat Chaiyakijpichet (No Magic Asia Ltd.)
 */
public class AllSSLTests
{
  public static Test suite()
  {
    @SuppressWarnings("unchecked")
    TestSuite suite = new Net4jTestSuite(AllTests.class.getName(), SSL.class);
    AllTests.populateSuite(suite);
    return suite;
  }
}

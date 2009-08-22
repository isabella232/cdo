/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *
 * $Id: GenRefMultiContained.java,v 1.2 2009-08-22 09:34:58 estepper Exp $
 */
package org.eclipse.emf.cdo.tests.legacy.model4;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc --> A representation of the model object '<em><b>Gen Ref Multi Contained</b></em>'. <!--
 * end-user-doc -->
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.emf.cdo.tests.legacy.model4.GenRefMultiContained#getElements <em>Elements</em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.emf.cdo.tests.legacy.model4.model4Package#getGenRefMultiContained()
 * @model
 * @generated
 */
public interface GenRefMultiContained extends EObject
{
  /**
   * Returns the value of the '<em><b>Elements</b></em>' containment reference list. The list contents are of type
   * {@link org.eclipse.emf.ecore.EObject}. <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Elements</em>' containment reference list isn't clear, there really should be more of a
   * description here...
   * </p>
   * <!-- end-user-doc -->
   * 
   * @return the value of the '<em>Elements</em>' containment reference list.
   * @see org.eclipse.emf.cdo.tests.legacy.model4.model4Package#getGenRefMultiContained_Elements()
   * @model containment="true"
   * @generated
   */
  EList<EObject> getElements();

} // GenRefMultiContained

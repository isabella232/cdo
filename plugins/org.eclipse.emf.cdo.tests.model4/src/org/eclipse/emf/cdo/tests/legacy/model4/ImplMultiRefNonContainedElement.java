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
 * $Id: ImplMultiRefNonContainedElement.java,v 1.2 2009-08-22 09:34:58 estepper Exp $
 */
package org.eclipse.emf.cdo.tests.legacy.model4;

import org.eclipse.emf.cdo.tests.legacy.model4interfaces.IMultiRefNonContainedElement;

/**
 * <!-- begin-user-doc --> A representation of the model object '<em><b>Impl Multi Ref Non Contained Element</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.eclipse.emf.cdo.tests.legacy.model4.ImplMultiRefNonContainedElement#getName <em>Name</em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.eclipse.emf.cdo.tests.legacy.model4.model4Package#getImplMultiRefNonContainedElement()
 * @model
 * @generated
 */
public interface ImplMultiRefNonContainedElement extends IMultiRefNonContainedElement
{
  /**
   * Returns the value of the '<em><b>Name</b></em>' attribute. <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Name</em>' attribute isn't clear, there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * 
   * @return the value of the '<em>Name</em>' attribute.
   * @see #setName(String)
   * @see org.eclipse.emf.cdo.tests.legacy.model4.model4Package#getImplMultiRefNonContainedElement_Name()
   * @model
   * @generated
   */
  String getName();

  /**
   * Sets the value of the '{@link org.eclipse.emf.cdo.tests.legacy.model4.ImplMultiRefNonContainedElement#getName
   * <em>Name</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @param value
   *          the new value of the '<em>Name</em>' attribute.
   * @see #getName()
   * @generated
   */
  void setName(String value);

} // ImplMultiRefNonContainedElement

/**
 * <copyright>
 * </copyright>
 *
 * $Id: CDOResourceNodeImpl.java,v 1.1.2.4 2008-10-18 06:31:09 estepper Exp $
 */
package org.eclipse.emf.cdo.eresource.impl;

import org.eclipse.emf.cdo.common.util.CDOException;
import org.eclipse.emf.cdo.eresource.CDOResourceFolder;
import org.eclipse.emf.cdo.eresource.CDOResourceNode;
import org.eclipse.emf.cdo.eresource.EresourcePackage;
import org.eclipse.emf.cdo.util.CDOURIUtil;

import org.eclipse.emf.internal.cdo.CDOObjectImpl;
import org.eclipse.emf.internal.cdo.CDOTransactionImpl;

import org.eclipse.net4j.util.ObjectUtil;

import org.eclipse.emf.ecore.EClass;

import java.util.List;

/**
 * <!-- begin-user-doc --> An implementation of the model object '<em><b>CDO Resource Node</b></em>'. <!-- end-user-doc
 * -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link org.eclipse.emf.cdo.eresource.impl.CDOResourceNodeImpl#getFolder <em>Folder</em>}</li>
 * <li>{@link org.eclipse.emf.cdo.eresource.impl.CDOResourceNodeImpl#getName <em>Name</em>}</li>
 * <li>{@link org.eclipse.emf.cdo.eresource.impl.CDOResourceNodeImpl#getPath <em>Path</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 * @since 2.0
 */
public abstract class CDOResourceNodeImpl extends CDOObjectImpl implements CDOResourceNode
{
  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected CDOResourceNodeImpl()
  {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected EClass eStaticClass()
  {
    return EresourcePackage.Literals.CDO_RESOURCE_NODE;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected int eStaticFeatureCount()
  {
    return 0;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public CDOResourceFolder getFolder()
  {
    return (CDOResourceFolder)eGet(EresourcePackage.Literals.CDO_RESOURCE_NODE__FOLDER, true);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setFolderGen(CDOResourceFolder newFolder)
  {
    eSet(EresourcePackage.Literals.CDO_RESOURCE_NODE__FOLDER, newFolder);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated NOT
   */
  public void setFolder(CDOResourceFolder newFolder)
  {
    String oldPath = getPath();
    basicSetFolder(newFolder, true);
    adjustContainedResources(oldPath);
  }

  /**
   * @ADDED
   */
  public void basicSetFolder(CDOResourceFolder newFolder, boolean checkDuplicates)
  {
    CDOResourceFolder oldFolder = getFolder();
    if (!ObjectUtil.equals(oldFolder, newFolder))
    {
      if (checkDuplicates)
      {
        String newPath = (newFolder == null ? "" : newFolder.getPath()) + CDOURIUtil.SEGMENT_SEPARATOR + getName();
        checkDuplicates(newPath);
      }

      setFolderGen(newFolder);
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public String getName()
  {
    return (String)eGet(EresourcePackage.Literals.CDO_RESOURCE_NODE__NAME, true);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setNameGen(String newName)
  {
    eSet(EresourcePackage.Literals.CDO_RESOURCE_NODE__NAME, newName);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated NOT
   */
  public void setName(String newName)
  {
    String oldPath = getPath();
    basicSetName(newName, true);
    adjustContainedResources(oldPath);
  }

  /**
   * @ADDED
   */
  public void basicSetName(String newName, boolean checkDuplicates)
  {
    String oldName = getName();
    if (!ObjectUtil.equals(oldName, newName))
    {
      if (checkDuplicates)
      {
        CDOResourceFolder parent = getFolder();
        String newPath = (parent == null ? "" : parent.getPath()) + CDOURIUtil.SEGMENT_SEPARATOR + getName();
        checkDuplicates(newPath);
      }

      setNameGen(newName);
    }
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated NOT
   */
  public String getPath()
  {
    CDOResourceFolder folder = getFolder();
    if (folder == null)
    {
      return "/" + getName();
    }

    return folder.getPath() + "/" + getName();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated NOT
   */
  public void setPath(String newPath)
  {
    CDOTransactionImpl transaction = cdoView().toTransaction();
    if (newPath == null)
    {
      throw new CDOException("Null path is not allowed");
    }

    String oldPath = getPath();
    if (!ObjectUtil.equals(oldPath, newPath))
    {
      // TODO check for duplicates
      List<String> names = CDOURIUtil.analyzePath(newPath);
      if (names.isEmpty())
      {
        throw new CDOException("Root path is not allowed");
      }

      String newName = names.remove(names.size() - 1);
      CDOResourceFolder newFolder = transaction.getOrCreateResourceFolder(names);
      if (newFolder == null)
      {
        transaction.getRootResource().getContents().add(this);
      }

      basicSetFolder(newFolder, false);
      basicSetName(newName, false);
      adjustContainedResources(oldPath);
    }
  }

  /**
   * @ADDED
   */
  private void adjustContainedResources(String oldPath)
  {
    // ResourceSet resourceSet = cdoView().getViewSet().getResourceSet();
    // EList<Resource> resources = resourceSet.getResources();
    // for (Resource resource : resources.toArray(new Resource[resources.size()]))
    // {
    // if (resource instanceof CDOResource)
    // {
    // CDOResource cdoResource = (CDOResource)resource;
    // String path = cdoResource.getPath();
    // // if (ObjectUtil.equals(path, oldPath))
    // // {
    // // // Don't handle *this* node
    // // continue;
    // // }
    //
    // if (path.startsWith(oldPath))
    // {
    // }
    // }
    // }
  }

  /**
   * @ADDED
   */
  private void checkDuplicates(String newPath)
  {
    try
    {
      cdoView().getResourceID(newPath);
    }
    catch (Exception ex)
    {
      throw new CDOException("Duplicate path: " + newPath);
    }
  }
} // CDOResourceNodeImpl

/*
 * Copyright (c) 2004-2014 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.internal.explorer;

import org.eclipse.emf.cdo.explorer.CDOExplorerElement;
import org.eclipse.emf.cdo.internal.explorer.bundle.OM;

import org.eclipse.net4j.util.AdapterUtil;
import org.eclipse.net4j.util.ObjectUtil;
import org.eclipse.net4j.util.StringUtil;
import org.eclipse.net4j.util.event.Notifier;
import org.eclipse.net4j.util.io.IOUtil;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * @author Eike Stepper
 */
public abstract class AbstractElement extends Notifier implements CDOExplorerElement, Adapter.Internal
{
  public static final String PROP_TYPE = "type";

  public static final String PROP_LABEL = "label";

  public static final String PROP_SERVER_BROWSER_PORT = "serverBrowserPort";

  private org.eclipse.emf.common.notify.Notifier target;

  private File folder;

  private String id;

  private String type;

  private String label;

  private int serverBrowserPort;

  public AbstractElement()
  {
  }

  public abstract AbstractManager<?> getManager();

  public final File getFolder()
  {
    return folder;
  }

  public final String getID()
  {
    return id;
  }

  public final String getType()
  {
    return type;
  }

  public final String getLabel()
  {
    return label;
  }

  public final void setLabel(String label)
  {
    if (!ObjectUtil.equals(this.label, label))
    {
      this.label = label;
      save();

      fireElementChangedEvent(true);
    }
  }

  public final int getServerBrowserPort()
  {
    return serverBrowserPort;
  }

  public final void setServerBrowserPort(int serverBrowserPort)
  {
    if (this.serverBrowserPort != serverBrowserPort)
    {
      this.serverBrowserPort = serverBrowserPort;
      save();
    }
  }

  protected final void fireElementChangedEvent(boolean impactsParent)
  {
    AbstractManager<?> manager = getManager();
    if (manager != null)
    {
      manager.fireElementChangedEvent(impactsParent, this);
    }
  }

  public String validateLabel(String label)
  {
    if (StringUtil.isEmpty(label.trim()))
    {
      return "Label is empty.";
    }

    if (ObjectUtil.equals(label, getLabel()))
    {
      return null;
    }

    AbstractManager<?> manager = getManager();
    if (manager != null)
    {
      for (CDOExplorerElement element : manager.getElements())
      {
        if (ObjectUtil.equals(element.getLabel(), label))
        {
          return "Label is not unique.";
        }
      }
    }

    return null;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Object getAdapter(Class adapter)
  {
    return AdapterUtil.adapt(this, adapter, false);
  }

  public void notifyChanged(Notification notification)
  {
  }

  public org.eclipse.emf.common.notify.Notifier getTarget()
  {
    return target;
  }

  public void setTarget(org.eclipse.emf.common.notify.Notifier newTarget)
  {
    target = newTarget;
  }

  public void unsetTarget(org.eclipse.emf.common.notify.Notifier oldTarget)
  {
    if (target == oldTarget)
    {
      setTarget(null);
    }
  }

  public boolean isAdapterForType(Object type)
  {
    return false;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == this)
    {
      return true;
    }

    if (obj == null)
    {
      return false;
    }

    if (obj.getClass() == getClass())
    {
      AbstractElement that = (AbstractElement)obj;
      return id.equals(that.getID());
    }

    return false;
  }

  @Override
  public int hashCode()
  {
    return getClass().hashCode() ^ id.hashCode();
  }

  public int compareTo(CDOExplorerElement o)
  {
    String label1 = StringUtil.safe(label);
    String label2 = StringUtil.safe(o.getLabel());
    return label1.compareTo(label2);
  }

  public void delete(boolean deleteContents)
  {
    if (deleteContents)
    {
      IOUtil.delete(folder);
    }
    else
    {
      String propertiesFileName = getManager().getPropertiesFileName();

      File from = new File(folder, propertiesFileName);
      File dest = new File(from.getParentFile(), from.getName() + ".removed");
      from.renameTo(dest);
    }
  }

  public void save()
  {
    Properties properties = new Properties();
    collectProperties(properties);

    String propertiesFileName = getManager().getPropertiesFileName();
    saveProperties(propertiesFileName, properties);
  }

  protected final void saveProperties(String fileName, Properties properties)
  {
    OutputStream out = null;

    try
    {
      folder.mkdirs();

      File file = new File(folder, fileName);
      out = new FileOutputStream(file);

      properties.store(out, getClass().getSimpleName() + fileName);
    }
    catch (IOException ex)
    {
      OM.LOG.error(ex);
    }
    finally
    {
      IOUtil.close(out);
    }
  }

  protected void init(File folder, String type, Properties properties)
  {
    this.folder = folder;
    id = folder.getName();

    this.type = type;
    label = properties.getProperty(PROP_LABEL);

    String property = properties.getProperty(PROP_SERVER_BROWSER_PORT);
    if (property != null)
    {
      serverBrowserPort = Integer.parseInt(property);
    }
  }

  protected void collectProperties(Properties properties)
  {
    properties.put(PROP_TYPE, type);
    properties.put(PROP_LABEL, label);

    if (serverBrowserPort != 0)
    {
      properties.put(PROP_SERVER_BROWSER_PORT, Integer.toString(serverBrowserPort));
    }
  }

  public static AbstractElement[] collect(Collection<?> c)
  {
    List<AbstractElement> result = new ArrayList<AbstractElement>();
    for (Object object : c)
    {
      if (object instanceof AbstractElement)
      {
        AbstractElement element = (AbstractElement)object;
        result.add(element);
      }
    }

    return result.toArray(new AbstractElement[result.size()]);
  }
}

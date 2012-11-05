/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    PluginManager.java
 *    Copyright (C) 2011-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class that manages a global map of plugins. The knowledge flow uses this to
 * manage plugins other than step components and perspectives. Is general
 * purpose, so can be used by other Weka components. Provides static methods for
 * registering and instantiating plugins.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class PluginManager {

  /**
   * Global map that is keyed by plugin base class/interface type. The inner Map
   * then stores individual plugin instances of the interface type, keyed by
   * plugin name/short title with values the actual fully qualified class name
   */
  protected static Map<String, Map<String, String>> PLUGINS = new HashMap<String, Map<String, String>>();

  /**
   * Add all key value pairs from the supplied property file
   * 
   * @param propsFile the properties file to add
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(File propsFile)
      throws Exception {
    BufferedInputStream bi = new BufferedInputStream(new FileInputStream(
        propsFile));
    addFromProperties(bi);
  }

  /**
   * Add all key value pairs from the supplied properties stream
   * 
   * @param propsStream an input stream to a properties file
   * @throws Exception if a problem occurs
   */
  public static synchronized void addFromProperties(InputStream propsStream)
      throws Exception {
    Properties expProps = new Properties();

    expProps.load(propsStream);
    propsStream.close();
    propsStream = null;
    Set keys = expProps.keySet();
    Iterator keysI = keys.iterator();
    while (keysI.hasNext()) {
      String baseType = (String) keysI.next();
      String implementations = expProps.getProperty(baseType);
      if (implementations != null && implementations.length() > 0) {
        String[] parts = implementations.split(",");
        for (String impl : parts) {
          PluginManager.addPlugin(baseType, impl.trim(), impl.trim());
        }
      }
    }
  }

  /**
   * Get a set of names of plugins that implement the supplied interface.
   * 
   * @param interfaceName the fully qualified name of the interface to list
   *          plugins for
   * 
   * @return a set of names of plugins
   */
  public static Set<String> getPluginNamesOfType(String interfaceName) {
    if (PLUGINS.get(interfaceName) != null) {
      return PLUGINS.get(interfaceName).keySet();
    }

    return null;
  }

  /**
   * Add a plugin.
   * 
   * @param interfaceName the fully qualified interface name that the plugin
   *          implements
   * 
   * @param name the name/short description of the plugin
   * @param concreteType the fully qualified class name of the actual concrete
   *          implementation
   */
  public static void addPlugin(String interfaceName, String name,
      String concreteType) {
    if (PLUGINS.get(interfaceName) == null) {
      Map<String, String> pluginsOfInterfaceType = new TreeMap<String, String>();
      pluginsOfInterfaceType.put(name, concreteType);
      PLUGINS.put(interfaceName, pluginsOfInterfaceType);
    } else {
      PLUGINS.get(interfaceName).put(name, concreteType);
    }
  }

  /**
   * Get an instance of a concrete implementation of a plugin type
   * 
   * @param interfaceType the fully qualified interface name of the plugin type
   * @param name the name/short description of the plugin to get
   * @return the concrete plugin
   * @throws Exception if the plugin can't be found or instantiated
   */
  public static Object getPluginInstance(String interfaceType, String name)
      throws Exception {
    if (PLUGINS.get(interfaceType) == null
        || PLUGINS.get(interfaceType).size() == 0) {
      throw new Exception("No plugins of interface type: " + interfaceType
          + " available!!");
    }

    Map<String, String> pluginsOfInterfaceType = PLUGINS.get(interfaceType);
    if (pluginsOfInterfaceType.get(name) == null) {
      throw new Exception("Can't find named plugin '" + name + "' of type '"
          + interfaceType + "'!");
    }

    String concreteImpl = pluginsOfInterfaceType.get(name);
    Object plugin = Class.forName(concreteImpl).newInstance();

    return plugin;
  }
}

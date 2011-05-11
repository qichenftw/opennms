//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2006 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//
package org.opennms.netmgt.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeMap;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.utils.IPLike;
import org.opennms.core.utils.InetAddressComparator;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.core.xml.CastorUtils;
import org.opennms.netmgt.ConfigFileConstants;
import org.opennms.netmgt.config.wmi.Definition;
import org.opennms.netmgt.config.wmi.Range;
import org.opennms.netmgt.config.wmi.WmiConfig;
import org.opennms.protocols.wmi.WmiAgentConfig;
import org.springframework.core.io.FileSystemResource;

/**
 * This class is the main repository for WMI configuration information used by
 * the capabilities daemon. When this class is loaded it reads the WMI
 * configuration into memory, and uses the configuration to find the
 * {@link org.opennms.protocols.wmi.WmiAgentConfig WmiAgentConfig} objects for specific
 * addresses. If an address cannot be located in the configuration then a
 * default peer instance is returned to the caller.
 *
 * <strong>Note: </strong>Users of this class should make sure the
 * <em>init()</em> is called before calling any other method to ensure the
 * config is loaded before accessing other convenience methods.
 *
 * @author <a href="mailto:david@opennms.org">David Hustace </a>
 * @author <a href="mailto:weave@oculan.com">Weave </a>
 * @author <a href="mailto:gturner@newedgenetworks.com">Gerald Turner </a>
 * @author <a href="mailto:matt.raykowski@gmail.com">Matt Raykowski</a>
 */
public class WmiPeerFactory extends PeerFactory {
    /**
     * The singleton instance of this factory
     */
    private static WmiPeerFactory m_singleton = null;

    /**
     * The config class loaded from the config file
     */
    private static WmiConfig m_config;

    /**
     * This member is set to true if the configuration file has been loaded.
     */
    private static boolean m_loaded = false;

    /**
     * Private constructor
     * 
     * @exception java.io.IOException
     *                Thrown if the specified config file cannot be read
     * @exception org.exolab.castor.xml.MarshalException
     *                Thrown if the file does not conform to the schema.
     * @exception org.exolab.castor.xml.ValidationException
     *                Thrown if the contents do not match the required schema.
     *
     * @param configFile the path to the config file to load in.
     */
    private WmiPeerFactory(String configFile) throws IOException, MarshalException, ValidationException {
        m_config = CastorUtils.unmarshal(WmiConfig.class, new FileSystemResource(configFile));
    }

    /**
     * <p>Constructor for WmiPeerFactory.</p>
     *
     * @param stream a {@link java.io.InputStream} object.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws org.exolab.castor.xml.ValidationException if any.
     */
    public WmiPeerFactory(InputStream stream) throws MarshalException, ValidationException {
        m_config = CastorUtils.unmarshal(WmiConfig.class, stream);
    }

    /**
     * <p>Constructor for WmiPeerFactory.</p>
     *
     * @param rdr a {@link java.io.Reader} object.
     * @throws java.io.IOException if any.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws org.exolab.castor.xml.ValidationException if any.
     */
    @Deprecated
    public WmiPeerFactory(Reader rdr) throws IOException, MarshalException, ValidationException {
        m_config = CastorUtils.unmarshal(WmiConfig.class, rdr);
    }

    /**
     * Load the config from the default config file and create the singleton
     * instance of this factory.
     *
     * @exception java.io.IOException
     *                Thrown if the specified config file cannot be read
     * @exception org.exolab.castor.xml.MarshalException
     *                Thrown if the file does not conform to the schema.
     * @exception org.exolab.castor.xml.ValidationException
     *                Thrown if the contents do not match the required schema.
     * @throws java.io.IOException if any.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws org.exolab.castor.xml.ValidationException if any.
     */
    public static synchronized void init() throws IOException, MarshalException, ValidationException {
        if (m_loaded) {
            // init already called - return
            // to reload, reload() will need to be called
            return;
        }

        File cfgFile = ConfigFileConstants.getFile(ConfigFileConstants.WMI_CONFIG_FILE_NAME);

        log().debug("init: config file path: " + cfgFile.getPath());

        m_singleton = new WmiPeerFactory(cfgFile.getPath());

        m_loaded = true;
    }

    private static ThreadCategory log() {
        return ThreadCategory.getInstance(WmiPeerFactory.class);
    }

    /**
     * Reload the config from the default config file
     *
     * @exception java.io.IOException
     *                Thrown if the specified config file cannot be read/loaded
     * @exception org.exolab.castor.xml.MarshalException
     *                Thrown if the file does not conform to the schema.
     * @exception org.exolab.castor.xml.ValidationException
     *                Thrown if the contents do not match the required schema.
     * @throws java.io.IOException if any.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws org.exolab.castor.xml.ValidationException if any.
     */
    public static synchronized void reload() throws IOException, MarshalException, ValidationException {
        m_singleton = null;
        m_loaded = false;

        init();
    }

    /**
     * Package-private access. Should only be used for unit testing.
     */
    WmiConfig getConfig() {
        return m_config;
    }

    /**
     * Saves the current settings to disk
     *
     * @throws java.lang.Exception if saving settings to disk fails.
     */
    public static synchronized void saveCurrent() throws Exception {
        optimize();

        // Marshal to a string first, then write the string to the file. This
        // way the original config
        // isn't lost if the XML from the marshal is hosed.
        StringWriter stringWriter = new StringWriter();
        Marshaller.marshal(m_config, stringWriter);
        if (stringWriter.toString() != null) {
            Writer fileWriter = new OutputStreamWriter(new FileOutputStream(ConfigFileConstants.getFile(ConfigFileConstants.WMI_CONFIG_FILE_NAME)), "UTF-8");
            fileWriter.write(stringWriter.toString());
            fileWriter.flush();
            fileWriter.close();
        }

        reload();
    }

    /**
     * Combine specific and range elements so that WMIPeerFactory has to spend
     * less time iterating all these elements.
     * TODO This really should be pulled up into PeerFactory somehow, but I'm not sure how (given that "Definition" is different for both
     * Snmp and WMI.  Maybe some sort of visitor methodology would work.  The basic logic should be fine as it's all IP address manipulation
     *
     * @throws UnknownHostException
     */
    static void optimize() throws UnknownHostException {
        ThreadCategory log = log();

        // First pass: Remove empty definition elements
        for (Iterator<Definition> definitionsIterator = m_config.getDefinitionCollection().iterator();
        definitionsIterator.hasNext();) {
            Definition definition = definitionsIterator.next();
            if (definition.getSpecificCount() == 0 && definition.getRangeCount() == 0) {
                if (log.isDebugEnabled())
                    log.debug("optimize: Removing empty definition element");
                definitionsIterator.remove();
            }
        }

        // Second pass: Replace single IP range elements with specific elements
        for (Definition definition : m_config.getDefinitionCollection()) {
            synchronized(definition) {
                for (Iterator<Range> rangesIterator = definition.getRangeCollection().iterator(); rangesIterator.hasNext();) {
                    Range range = rangesIterator.next();
                    if (range.getBegin().equals(range.getEnd())) {
                        definition.addSpecific(range.getBegin());
                        rangesIterator.remove();
                    }
                }
            }
        }

        // Third pass: Sort specific and range elements for improved XML
        // readability and then combine them into fewer elements where possible
        for (Iterator<Definition> defIterator = m_config.getDefinitionCollection().iterator(); defIterator.hasNext(); ) {
            Definition definition = defIterator.next();

            // Sort specifics
            final TreeMap<InetAddress,String> specificsMap = new TreeMap<InetAddress,String>(new InetAddressComparator());
            for (String specific : definition.getSpecificCollection()) {
                specificsMap.put(InetAddressUtils.getInetAddress(specific), specific.trim());
            }

            // Sort ranges
            final TreeMap<InetAddress,Range> rangesMap = new TreeMap<InetAddress,Range>(new InetAddressComparator());
            for (Range range : definition.getRangeCollection()) {
                rangesMap.put(InetAddressUtils.getInetAddress(range.getBegin()), range);
            }

            // Combine consecutive specifics into ranges
            InetAddress priorSpecific = null;
            Range addedRange = null;
            for (final InetAddress specific : specificsMap.keySet()) {
                if (priorSpecific == null) {
                    priorSpecific = specific;
                    continue;
                }

                if (BigInteger.ONE.equals(InetAddressUtils.difference(specific, priorSpecific)) &&
                        InetAddressUtils.inSameScope(specific, priorSpecific)) {
                    if (addedRange == null) {
                        addedRange = new Range();
                        addedRange.setBegin(InetAddressUtils.toIpAddrString(priorSpecific));
                        rangesMap.put(priorSpecific, addedRange);
                        specificsMap.remove(priorSpecific);
                    }

                    addedRange.setEnd(InetAddressUtils.toIpAddrString(specific));
                    specificsMap.remove(specific);
                }
                else {
                    addedRange = null;
                }

                priorSpecific = specific;
            }

            // Move specifics to ranges
            for (final InetAddress specific : new ArrayList<InetAddress>(specificsMap.keySet())) {
                for (final InetAddress begin : new ArrayList<InetAddress>(rangesMap.keySet())) {
                    if (!InetAddressUtils.inSameScope(begin, specific)) {
                        continue;
                    }

                    if (InetAddressUtils.toInteger(begin).subtract(BigInteger.ONE).compareTo(InetAddressUtils.toInteger(specific)) > 0) {
                        continue;
                    }

                    Range range = rangesMap.get(begin);

                    final InetAddress end = InetAddressUtils.getInetAddress(range.getEnd());

                    if (InetAddressUtils.toInteger(end).add(BigInteger.ONE).compareTo(InetAddressUtils.toInteger(specific)) < 0) {
                        continue;
                    }

                    if (
                            InetAddressUtils.toInteger(specific).compareTo(InetAddressUtils.toInteger(begin)) >= 0 &&
                            InetAddressUtils.toInteger(specific).compareTo(InetAddressUtils.toInteger(end)) <= 0
                    ) {
                        specificsMap.remove(specific);
                        break;
                    }

                    if (InetAddressUtils.toInteger(begin).subtract(BigInteger.ONE).equals(InetAddressUtils.toInteger(specific))) {
                        rangesMap.remove(begin);
                        rangesMap.put(specific, range);
                        range.setBegin(InetAddressUtils.toIpAddrString(specific));
                        specificsMap.remove(specific);
                        break;
                    }

                    if (InetAddressUtils.toInteger(end).add(BigInteger.ONE).equals(InetAddressUtils.toInteger(specific))) {
                        range.setEnd(InetAddressUtils.toIpAddrString(specific));
                        specificsMap.remove(specific);
                        break;
                    }
                }
            }

            // Combine consecutive ranges
            Range priorRange = null;
            InetAddress priorBegin = null;
            InetAddress priorEnd = null;
            for (final Iterator<InetAddress> rangesIterator = rangesMap.keySet().iterator(); rangesIterator.hasNext();) {
                final InetAddress beginAddress = rangesIterator.next();
                final Range range = rangesMap.get(beginAddress);
                final InetAddress endAddress = InetAddressUtils.getInetAddress(range.getEnd());

                if (priorRange != null) {
                    if (InetAddressUtils.inSameScope(beginAddress, priorEnd) && InetAddressUtils.difference(beginAddress, priorEnd).compareTo(BigInteger.ONE) <= 0) {
                        priorBegin = new InetAddressComparator().compare(priorBegin, beginAddress) < 0 ? priorBegin : beginAddress;
                        priorRange.setBegin(InetAddressUtils.toIpAddrString(priorBegin));
                        priorEnd = new InetAddressComparator().compare(priorEnd, endAddress) > 0 ? priorEnd : endAddress;
                        priorRange.setEnd(InetAddressUtils.toIpAddrString(priorEnd));

                        rangesIterator.remove();
                        continue;
                    }
                }

                priorRange = range;
                priorBegin = beginAddress;
                priorEnd = endAddress;
            }

            // Update changes made to sorted maps
            definition.setSpecific(specificsMap.values().toArray(new String[0]));
            definition.setRange(rangesMap.values().toArray(new Range[0]));
        }
    }

    /**
     * Return the singleton instance of this factory.
     *
     * @return The current factory instance.
     * @throws java.lang.IllegalStateException
     *             Thrown if the factory has not yet been initialized.
     */
    public static synchronized WmiPeerFactory getInstance() {
        if (!m_loaded)
            throw new IllegalStateException("The WmiPeerFactory has not been initialized");

        return m_singleton;
    }

    /**
     * <p>setInstance</p>
     *
     * @param singleton a {@link org.opennms.netmgt.config.WmiPeerFactory} object.
     */
    public static synchronized void setInstance(WmiPeerFactory singleton) {
        m_singleton = singleton;
        m_loaded = true;
    }

    /**
     * <p>getAgentConfig</p>
     *
     * @param agentInetAddress a {@link java.net.InetAddress} object.
     * @return a {@link org.opennms.protocols.wmi.WmiAgentConfig} object.
     */
    public synchronized WmiAgentConfig getAgentConfig(InetAddress agentInetAddress) {

        if (m_config == null) {
            return new WmiAgentConfig(agentInetAddress);
        }

        WmiAgentConfig agentConfig = new WmiAgentConfig(agentInetAddress);

        //Now set the defaults from the m_config
        setWmiAgentConfig(agentConfig, new Definition());

        // Attempt to locate the node
        //
        Enumeration<Definition> edef = m_config.enumerateDefinition();
        DEFLOOP: while (edef.hasMoreElements()) {
            Definition def = edef.nextElement();

            // check the specifics first
            for (String saddr : def.getSpecificCollection()) {
                InetAddress addr = InetAddressUtils.addr(saddr);
                if (addr.equals(agentConfig.getAddress())) {
                    setWmiAgentConfig(agentConfig, def);
                    break DEFLOOP;
                }
            }

            // check the ranges
            for (Range rng : def.getRangeCollection()) {
                if (InetAddressUtils.isInetAddressInRange(InetAddressUtils.str(agentConfig.getAddress()), rng.getBegin(), rng.getEnd())) {
                    setWmiAgentConfig(agentConfig, def );
                    break DEFLOOP;
                }
            }

            // check the matching IP expressions
            //
            for (String ipMatch : def.getIpMatchCollection()) {
                if (IPLike.matches(InetAddressUtils.str(agentInetAddress), ipMatch)) {
                    setWmiAgentConfig(agentConfig, def);
                    break DEFLOOP;
                }
            }

        } // end DEFLOOP

        if (agentConfig == null) {

            Definition def = new Definition();
            setWmiAgentConfig(agentConfig, def);
        }

        return agentConfig;

    }

    private void setWmiAgentConfig(WmiAgentConfig agentConfig, Definition def) {
        setCommonAttributes(agentConfig, def);
        agentConfig.setPassword(determinePassword(def));       
    }

    /**
     * This is a helper method to set all the common attributes in the agentConfig.
     * 
     * @param agentConfig
     * @param def
     */
    private void setCommonAttributes(WmiAgentConfig agentConfig, Definition def) {
        agentConfig.setRetries(determineRetries(def));
        agentConfig.setTimeout((int)determineTimeout(def));
        agentConfig.setUsername(determineUsername(def));
        agentConfig.setPassword(determinePassword(def));
        agentConfig.setDomain(determineDomain(def));
    }

    /**
     * Helper method to search the wmi-config for the appropriate username
     * @param def
     * @return a string containing the username. will return the default if none is set.
     */
    private String determineUsername(Definition def) {
        return (def.getPassword() == null ? (m_config.getUsername() == null ? WmiAgentConfig.DEFAULT_USERNAME :m_config.getUsername()) : def.getUsername());
    }

    /**
     * Helper method to search the wmi-config for the appropriate domain/workgroup.
     * @param def
     * @return a string containing the domain. will return the default if none is set.
     */
    private String determineDomain(Definition def) {
        return (def.getDomain() == null ? (m_config.getDomain() == null ? WmiAgentConfig.DEFAULT_DOMAIN :m_config.getDomain()) : def.getDomain());
    }

    /**
     * Helper method to search the wmi-config for the appropriate password
     * @param def
     * @return a string containing the password. will return the default if none is set.
     */
    private String determinePassword(Definition def) {
        return (def.getPassword() == null ? (m_config.getPassword() == null ? WmiAgentConfig.DEFAULT_PASSWORD :m_config.getPassword()) : def.getPassword());
    }

    /**
     * Helper method to search the wmi-config 
     * @param def
     * @return a long containing the timeout, WmiAgentConfig.DEFAULT_TIMEOUT if not specified.
     */
    private long determineTimeout(Definition def) {
        long timeout = WmiAgentConfig.DEFAULT_TIMEOUT;
        return (long)(def.getTimeout() == 0 ? (m_config.getTimeout() == 0 ? timeout : m_config.getTimeout()) : def.getTimeout());
    }

    private int determineRetries(Definition def) {        
        int retries = WmiAgentConfig.DEFAULT_RETRIES;
        return (def.getRetry() == 0 ? (m_config.getRetry() == 0 ? retries : m_config.getRetry()) : def.getRetry());
    }

    /**
     * <p>getWmiConfig</p>
     *
     * @return a {@link org.opennms.netmgt.config.wmi.WmiConfig} object.
     */
    public static WmiConfig getWmiConfig() {
        return m_config;
    }

    /**
     * <p>setWmiConfig</p>
     *
     * @param m_config a {@link org.opennms.netmgt.config.wmi.WmiConfig} object.
     */
    public static synchronized void setWmiConfig(WmiConfig m_config) {
        WmiPeerFactory.m_config = m_config;
    }
}

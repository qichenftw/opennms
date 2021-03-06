/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.features.vaadin.mibcompiler;

import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsmiparser.parser.SmiDefaultParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.features.vaadin.mibcompiler.api.MibParser;
import org.opennms.features.vaadin.mibcompiler.services.JsmiMibParser;
import org.opennms.features.vaadin.mibcompiler.services.OnmsProblemEventHandler;
import org.opennms.features.vaadin.mibcompiler.services.PrefabGraphDumper;
import org.opennms.netmgt.config.datacollection.DatacollectionGroup;
import org.opennms.netmgt.config.datacollection.Group;
import org.opennms.netmgt.config.datacollection.MibObj;
import org.opennms.netmgt.model.PrefabGraph;
import org.opennms.netmgt.xml.eventconf.Event;
import org.opennms.netmgt.xml.eventconf.Events;
import org.opennms.netmgt.xml.eventconf.Maskelement;
import org.opennms.netmgt.xml.eventconf.Varbindsdecode;

/**
 * The Test Class for JsmiMibParser.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a> 
 */
public class JsmiMibParserTest {

    /** The Constant MIB_DIR. */
    protected static final File MIB_DIR = new File("src/test/resources");

    /** The parser. */
    protected MibParser parser;

    /**
     * Sets the up.
     */
    @Before
    public void setUp() {
        parser = new JsmiMibParser();
        parser.setMibDirectory(MIB_DIR);
    }

    /**
     * Test good MIB.
     *
     * @throws Exception the exception
     */
    @Test
    public void testGoodMib() throws Exception {
        if (parser.parseMib(new File(MIB_DIR, "IF-MIB.txt"))) {
            Assert.assertTrue(parser.getMissingDependencies().isEmpty());
            Assert.assertNull(parser.getFormattedErrors());
        } else {
            Assert.fail("The IF-MIB.txt file couldn't be parsed successfully.");
        }
    }

    /**
     * Test a MIB with missing dependencies.
     *
     * @throws Exception the exception
     */
    @Test
    public void testMissingDependencies() throws Exception {
        if (parser.parseMib(new File(MIB_DIR, "SONUS-COMMON-MIB.txt"))) {
            Assert.fail("The SONUS-COMMON-MIB.txt file contains unsatisfied dependencies, so the MIB parser must generate errors.");
        } else {
            List<String> dependencies = parser.getMissingDependencies();
            Assert.assertEquals(2, dependencies.size());
            Assert.assertNotNull(parser.getFormattedErrors());
            Assert.assertEquals("[SONUS-SMI, SONUS-TC]", dependencies.toString());
        }
    }

    /**
     * Test a MIB with internal errors.
     *
     * @throws Exception the exception
     */
    @Test
    public void testMibWithErrors() throws Exception {
        if (parser.parseMib(new File(MIB_DIR, "NET-SNMP-MIB.txt"))) {
            Assert.fail("The NET-SNMP-MIB.txt file contains errors, so the MIB parser must generate errors.");
        } else {
            Assert.assertTrue(parser.getMissingDependencies().isEmpty());
            String errors = parser.getFormattedErrors();
            Assert.assertNotNull(errors);
            System.err.println(errors);
        }
    }

    /**
     * Test generate events from notifications.
     *
     * @throws Exception the exception
     */
    @Test
    public void testNotifications() throws Exception {
        if (parser.parseMib(new File(MIB_DIR, "IF-MIB.txt"))) {
            Events events = parser.getEvents("uei.opennms.org/traps/ifmib");
            Assert.assertNotNull(events);
            System.out.println(JaxbUtils.marshal(events));
            Assert.assertEquals(2, events.getEventCount());
            Event event  = null;
            for (Event e : events.getEventCollection()) {
                if (e.getUei().contains("linkDown"))
                    event = e;
            }
            Assert.assertNotNull(event);
            Assert.assertNotNull(event.getDescr()); // TODO Must be more specific
            Assert.assertNotNull(event.getLogmsg());
            Assert.assertNotNull(event.getLogmsg().getContent()); // TODO Must be more specific
            Assert.assertEquals("Indeterminate", event.getSeverity());
            Assert.assertEquals("IF-MIB defined trap event: linkDown", event.getEventLabel());
            Assert.assertNotNull(event.getMask());
            for (Maskelement me : event.getMask().getMaskelementCollection()) {
                if (me.getMename().equals("id"))
                    Assert.assertEquals(".1.3.6.1.6.3.1.1.5", me.getMevalueCollection().get(0));
                if (me.getMename().equals("generic"))
                    Assert.assertEquals("6", me.getMevalueCollection().get(0));
                if (me.getMename().equals("specific"))
                    Assert.assertEquals("3", me.getMevalueCollection().get(0));
            }
            Assert.assertEquals(2, event.getVarbindsdecodeCount());
            for (Varbindsdecode vb : event.getVarbindsdecodeCollection()) {
                if (vb.getParmid().equals("parm[#2]"))
                    Assert.assertEquals(3, vb.getDecodeCount());
                if (vb.getParmid().equals("parm[#3]"))
                    Assert.assertEquals(7, vb.getDecodeCount());
            }
        } else {
            Assert.fail("The IF-MIB.txt file couldn't be parsed successfully.");
        }
    }

    /**
     * Test generate events from traps.
     */
    @Test
    public void testTraps() {
        if (parser.parseMib(new File(MIB_DIR, "RFC1269-MIB.txt"))) {
            Assert.assertEquals("RFC1269-MIB", parser.getMibName());
            Events events = parser.getEvents("uei.opennms.org/traps/RFC1269");
            Assert.assertNotNull(events);
            Assert.assertEquals(2, events.getEventCount());
            System.out.println(JaxbUtils.marshal(events));
            Assert.assertEquals(2, events.getEventCount());
            Event event  = null;
            for (Event e : events.getEventCollection()) {
                if (e.getUei().contains("bgpBackwardTransition"))
                    event = e;
            }
            Assert.assertNotNull(event);
            Assert.assertNotNull(event.getDescr()); // TODO Must be more specific
            Assert.assertNotNull(event.getLogmsg());
            Assert.assertNotNull(event.getLogmsg().getContent()); // TODO Must be more specific
            Assert.assertEquals("Indeterminate", event.getSeverity());
            Assert.assertEquals("RFC1269-MIB defined trap event: bgpBackwardTransition", event.getEventLabel());
            Assert.assertNotNull(event.getMask());
            for (Maskelement me : event.getMask().getMaskelementCollection()) {
                if (me.getMename().equals("id"))
                    Assert.assertEquals(".1.3.6.1.2.1.15", me.getMevalueCollection().get(0));
                if (me.getMename().equals("generic"))
                    Assert.assertEquals("6", me.getMevalueCollection().get(0));
                if (me.getMename().equals("specific"))
                    Assert.assertEquals("2", me.getMevalueCollection().get(0));
            }
            Assert.assertEquals(1, event.getVarbindsdecodeCount());
            for (Varbindsdecode vb : event.getVarbindsdecodeCollection()) {
                if (vb.getParmid().equals("parm[#3]"))
                    Assert.assertEquals(6, vb.getDecodeCount());
            }
        } else {
            Assert.fail("The RFC1269-MIB.txt file couldn't be parsed successfully.");
        }
    }

    /**
     * Test generate data collection.
     *
     * @throws Exception the exception
     */
    @Test
    public void testGenerateDataCollection() throws Exception {
        if (parser.parseMib(new File(MIB_DIR, "IF-MIB.txt"))) {
            DatacollectionGroup dcGroup = parser.getDataCollection();
            Assert.assertNotNull(dcGroup);
            System.out.println(JaxbUtils.marshal(dcGroup));
            Assert.assertEquals(5, dcGroup.getResourceTypeCount());
            Assert.assertEquals(7, dcGroup.getGroupCount());
            Group mibGroup = null;
            for (Group g : dcGroup.getGroupCollection()) {
                if (g.getName().equals("ifTable"))
                    mibGroup = g;
            }
            Assert.assertNotNull(mibGroup);
            Assert.assertEquals(22, mibGroup.getMibObjCount());
            for (MibObj mo : mibGroup.getMibObjCollection()) {
                Assert.assertEquals("ifEntry", mo.getInstance());
                Assert.assertTrue(mo.getOid().startsWith(".1.3.6.1.2.1.2.2.1"));
                Assert.assertTrue(mo.getType().matches("^(?i)(counter|gauge|timeticks|integer|octetstring|string)?\\d*$"));
            }
        } else {
            Assert.fail("The IF-MIB.txt file couldn't be parsed successfully.");
        }
    }

    /**
     * Test name cutter
     *
     * @throws Exception the exception
     */
    @Test
    public void testNameCutter() throws Exception {
        if (parser.parseMib(new File(MIB_DIR, "Clavister-MIB.mib"))) {
            DatacollectionGroup dcGroup = parser.getDataCollection();
            Assert.assertNotNull(dcGroup);
            System.out.println(JaxbUtils.marshal(dcGroup));
            int count = 0;
            for (Group group : dcGroup.getGroupCollection()) {
                for (MibObj mo : group.getMibObjCollection()) {
                    if (mo.getAlias().length() > 19) { // Character restriction.
                        count++;
                    }
                }
            }
            // Without the name-cutter the number will be 80.
            Assert.assertEquals(0, count);
        } else {
            Assert.fail("The Clavister-MIB.mib file couldn't be parsed successfully.");
        }
    }

    /**
     * Test generate graph templates.
     *
     * @throws Exception the exception
     */
    @Test
    public void testGenerateGraphTemplates() throws Exception {
        if (parser.parseMib(new File(MIB_DIR, "Clavister-MIB.mib"))) {
            List<PrefabGraph> graphs = parser.getPrefabGraphs();
            StringWriter writer = new StringWriter();
            PrefabGraphDumper dumper = new PrefabGraphDumper();
            dumper.dump(graphs, writer);
            System.out.println(writer.getBuffer().toString());
            Assert.assertEquals(100074, writer.getBuffer().toString().length());
        } else {
            Assert.fail("The Clavister-MIB.mib file couldn't be parsed successfully.");
        }
    }

    /**
     * Test a MIB with internal syntax errors (or invalid content).
     *
     * @throws Exception the exception
     */
    @Test
    public void testMibWithInvalidContent() throws Exception {
        SmiDefaultParser parser = new SmiDefaultParser();
        OnmsProblemEventHandler errorHandler = new OnmsProblemEventHandler(parser);
        List<URL> inputUrls = new ArrayList<URL>();
        try {
            inputUrls.add(new File(MIB_DIR, "SNMPv2-SMI.txt").toURI().toURL());
            inputUrls.add(new File(MIB_DIR, "NET-SNMP-MIB.txt").toURI().toURL());
        } catch (Exception e) {
            Assert.fail();
        }
        parser.getFileParserPhase().setInputUrls(inputUrls);
        parser.parse();
        if (parser.getProblemEventHandler().isNotOk()) {
            Assert.assertEquals(6, parser.getProblemEventHandler().getTotalCount());
            Assert.assertTrue(errorHandler.getDependencies().isEmpty());
            Assert.assertNotNull(errorHandler.getMessages());
            System.err.println(errorHandler.getMessages());
        } else {
            Assert.fail("The NET-SNMP-MIB.txt file contains errors, so the MIB parser must generate errors.");
        }
    }

    /**
     * Test a MIB with internal syntax errors (or invalid content).
     *
     * @throws Exception the exception
     */
    @Test
    public void testBadIfMib() throws Exception {
        if (parser.parseMib(new File(MIB_DIR, "IF-MIB-BAD.txt"))) {
            Assert.fail();
        } else {
            Assert.assertEquals(0, parser.getMissingDependencies().size());
            String errors = parser.getFormattedErrors();
            Assert.assertNotNull(errors);
            System.err.println(errors);
        }
    }

}

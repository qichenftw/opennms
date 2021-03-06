/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2008-2012 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.poller.monitors;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.utils.ParameterMap;
import org.opennms.core.xml.CastorUtils;
import org.opennms.netmgt.config.mailtransporttest.MailTransportTest;
import org.opennms.netmgt.config.mailtransporttest.ReadmailHost;
import org.opennms.netmgt.config.mailtransporttest.ReadmailTest;
import org.opennms.netmgt.config.mailtransporttest.SendmailTest;

/**
 * This is a wrapper class for handling JavaMail configurations.
 *
 * @author <a href="mailto:david@opennms.org">David Hustace</a>
 * @version $Id: $
 */
public class MailTransportParameters {
    
    /** Constant <code>KEY="MailTransportParameters.class.getName()"</code> */
    public static final String KEY = MailTransportParameters.class.getName();
	private static final int DEFAULT_RETRY = 1;
	private static final int DEFAULT_TIMEOUT = 3000;
    private Map<String,Object> m_parameterMap;
    private MailTransportTest m_transportTest;
	private String m_testSubjectSuffix;
    private boolean m_end2EndTestInProgress = false;
    private Properties m_javamailProperties = new Properties();

    MailTransportParameters(Map<String,Object> parameterMap) {
        m_parameterMap = parameterMap;
        String test = getStringParm("mail-transport-test", null);
        if (test == null) {
            throw new IllegalArgumentException("mail-transport-test must be set in monitor parameters");
        }
        m_transportTest = parseMailTransportTest(test);
    }
    
    static synchronized MailTransportParameters get(Map<String,Object> parameterMap) {
        MailTransportParameters parms = (MailTransportParameters)parameterMap.get(KEY);
        if (parms == null) {
            parms = new MailTransportParameters(parameterMap);
            parameterMap.put(KEY, parms);
        }
        return parms;
    }
            
    Map<String,Object> getParameterMap() {
        return Collections.unmodifiableMap(m_parameterMap);
    }

    MailTransportTest getTransportTest() {
        return m_transportTest;
    }

    MailTransportTest parseMailTransportTest(String test) {
        try {
            return CastorUtils.unmarshal(MailTransportTest.class, new ByteArrayInputStream(test.getBytes("UTF-8")));
        } catch (MarshalException e) {
            throw new IllegalArgumentException("Unable to parse mail-test-sequence for MailTransportMonitor: "+test, e);
        } catch (ValidationException e) {
            throw new IllegalArgumentException("Unable to parse mail-test-sequence for MailTransportMonitor: "+test, e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Unable to parse mail-test-sequence for MailTransportMonitor: "+test, e);
        }
    
    }

    private String getStringParm(String key, String deflt) {
        return ParameterMap.getKeyedString(this.getParameterMap(), key, deflt);
    }
    
    private int getIntParm(String key, int defValue) {
        return ParameterMap.getKeyedInteger(getParameterMap()  , key, defValue);
    }

	/**
	 * <p>getRetries</p>
	 *
	 * @return a int.
	 */
	public int getRetries() {
		return getIntParm("retry", MailTransportParameters.DEFAULT_RETRY);
	}

	/**
	 * <p>getTimeout</p>
	 *
	 * @return a int.
	 */
	public int getTimeout() {
		return getIntParm("timeout", MailTransportParameters.DEFAULT_TIMEOUT);
	}
	
    /**
     * <p>getReadTestPassword</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getReadTestPassword() {
        return getReadTest().getUserAuth().getPassword();
    }

    ReadmailTest getReadTest() {
        return getTransportTest().getMailTest().getReadmailTest();
    }

	/**
	 * <p>getTestSubjectSuffix</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	public String getTestSubjectSuffix() {
		return m_testSubjectSuffix;
	}

	/**
	 * <p>setTestSubjectSuffix</p>
	 *
	 * @param suffix a {@link java.lang.String} object.
	 */
	public void setTestSubjectSuffix(final String suffix) {
		m_testSubjectSuffix = suffix;
	}

	/**
	 * <p>getComputedTestSubject</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	public String getComputedTestSubject() {
	    try {
	        final String subject = getSendTestSubject();
	        final String suffix = getTestSubjectSuffix();
			if (subject != null) {
	            return new StringBuilder(subject).append(':').append(suffix == null? "" : suffix).toString();
	        } else {
	            return null;
	        }
	    } catch (final IllegalStateException e) {
	        return null;
	    }
	}
	
    String getSendTestFrom() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().getSendmailMessage().getFrom();
    }

    /**
     * <p>isSendTestUseAuth</p>
     *
     * @return a boolean.
     */
    public boolean isSendTestUseAuth() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().isUseAuthentication();
    }

    /**
     * <p>getSendTestCharSet</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSendTestCharSet() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().getSendmailProtocol().getCharSet();        }

    /**
     * <p>getSendTestMessageContentType</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSendTestMessageContentType() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().getSendmailProtocol().getMessageContentType();
    }

    /**
     * <p>isSendTestDebug</p>
     *
     * @return a boolean.
     */
    public boolean isSendTestDebug() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().isDebug();
    }

    /**
     * <p>getSendTestMessageEncoding</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSendTestMessageEncoding() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().getSendmailProtocol().getMessageEncoding();
    }

    /**
     * <p>getSendTestMailer</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSendTestMailer() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().getSendmailProtocol().getMailer();
    }

    /**
     * <p>getSendTestHost</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSendTestHost() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().getSendmailHost().getHost();        }

    /**
     * <p>getSendTestMessageBody</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSendTestMessageBody() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().getSendmailMessage().getBody();
    }

    /**
     * <p>getSendTestPassword</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSendTestPassword() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().getUserAuth().getPassword();
    }

    /**
     * <p>isSendTestIsQuitWait</p>
     *
     * @return a boolean.
     */
    public boolean isSendTestIsQuitWait() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().getSendmailProtocol().isQuitWait();
    }

    /**
     * <p>getSendTestPort</p>
     *
     * @return a int.
     */
    public int getSendTestPort() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return (int)getSendTest().getSendmailHost().getPort();
    }

    /**
     * <p>isSendTestIsSslEnable</p>
     *
     * @return a boolean.
     */
    public boolean isSendTestIsSslEnable() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().getSendmailProtocol().isSslEnable();
    }

    /**
     * <p>isSendTestStartTls</p>
     *
     * @return a boolean.
     */
    public boolean isSendTestStartTls() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().getSendmailProtocol().isStartTls();
    }

    /**
     * <p>getSendTestSubject</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSendTestSubject() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().getSendmailMessage().getSubject();
    }

    /**
     * <p>getSendTestRecipeint</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSendTestRecipeint() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().getSendmailMessage().getTo();
    }

    /**
     * <p>getSendTestTransport</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSendTestTransport() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().getSendmailProtocol().getTransport();
    }

    /**
     * <p>isSendTestUseJmta</p>
     *
     * @return a boolean.
     */
    public boolean isSendTestUseJmta() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().isUseJmta();
    }

    /**
     * <p>getSendTestUserName</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSendTestUserName() {
        if (getSendTest() == null) {
            throw new IllegalStateException("Request for send mailparmaters invalid due to no sendmail specification in config");
        }
        return getSendTest().getUserAuth().getUserName();
    }

    SendmailTest getSendTest() {
        return getTransportTest().getMailTest().getSendmailTest();
    }

    /**
     * <p>getReadTestHost</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getReadTestHost() {
        final ReadmailTest readTest = getReadTest();
        if (readTest != null) {
        	final ReadmailHost readmailHost = readTest.getReadmailHost();
        	if (readmailHost != null) return readmailHost.getHost();
        }
        return null;
    }

    /**
     * <p>getReadTestPort</p>
     *
     * @return a int.
     */
    public int getReadTestPort() {
        return (int)getReadTest().getReadmailHost().getPort();
    }

    /**
     * <p>getReadTestUserName</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getReadTestUserName() {
        return getReadTest().getUserAuth().getUserName();
    }

    /**
     * <p>getReadTestFolder</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getReadTestFolder() {
        return getReadTest().getMailFolder();
    }

    /**
     * <p>getReadTestProtocol</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getReadTestProtocol() {
        return getReadTest().getReadmailHost().getReadmailProtocol().getTransport();
    }

    /**
     * <p>isReadTestStartTlsEnabled</p>
     *
     * @return a boolean.
     */
    public boolean isReadTestStartTlsEnabled() {
        return getReadTest().getReadmailHost().getReadmailProtocol().isStartTls();
    }
    
    /**
     * <p>isReadTestSslEnabled</p>
     *
     * @return a boolean.
     */
    public boolean isReadTestSslEnabled() {
        return getReadTest().getReadmailHost().getReadmailProtocol().isSslEnable();
    }

    /**
     * <p>setEnd2EndTestInProgress</p>
     *
     * @param b a boolean.
     */
    public void setEnd2EndTestInProgress(boolean b) {
        m_end2EndTestInProgress  = b;
    }
    
    /**
     * <p>isEnd2EndTestInProgress</p>
     *
     * @return a boolean.
     */
    public boolean isEnd2EndTestInProgress() {
        return m_end2EndTestInProgress;
    }
    
    /**
     * <p>getReadTestAttemptInterval</p>
     *
     * @return a long.
     */
    public long getReadTestAttemptInterval() {
        return getReadTest().getAttemptInterval();
    }
    
    /**
     * <p>getSendTestAttemptInterval</p>
     *
     * @return a long.
     */
    public long getSendTestAttemptInterval() {
        return getSendTest().getAttemptInterval();
    }

    /**
     * <p>getJavamailProperties</p>
     *
     * @return a {@link java.util.Properties} object.
     */
    public Properties getJavamailProperties() {
        return m_javamailProperties;
    }

    /**
     * <p>setJavamailProperties</p>
     *
     * @param props a {@link java.util.Properties} object.
     */
    public void setJavamailProperties(Properties props) {
        m_javamailProperties = props;
    }

    /**
     * <p>setReadTestHost</p>
     *
     * @param host a {@link java.lang.String} object.
     */
    public void setReadTestHost(String host) {
        getReadTest().getReadmailHost().setHost(host);
    }

    /**
     * <p>setSendTestHost</p>
     *
     * @param host a {@link java.lang.String} object.
     */
    public void setSendTestHost(String host) {
        getSendTest().getSendmailHost().setHost(host);
    }
}

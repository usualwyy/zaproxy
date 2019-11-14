/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2013 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.authentication;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.apache.commons.httpclient.URI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.network.HttpRequestHeader;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationMethodIndicatorsUnitTest {

    private static final String LOGGED_OUT_COMPLEX_INDICATOR = "User [^\\s]* logged out";
    private static final String LOGGED_OUT_COMPLEX_BODY =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                    + "Pellentesque auctor nulla id turpis placerat vulputate. User Test logged out. "
                    + " Proin tempor bibendum eros rutrum. ";
    private static final String LOGGED_IN_INDICATOR = "logged in";
    private static final String LOGGED_OUT_INDICATOR = "logged out";
    private static final String LOGGED_IN_BODY =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                    + "Pellentesque auctor nulla id turpis placerat vulputate."
                    + LOGGED_IN_INDICATOR
                    + " Proin tempor bibendum eros rutrum. ";
    private static final String LOGGED_OUT_BODY =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
                    + "Pellentesque auctor nulla id turpis placerat vulputate."
                    + LOGGED_OUT_INDICATOR
                    + " Proin tempor bibendum eros rutrum. ";

    private HttpMessage loginMessage;
    private AuthenticationMethod method;

    @Before
    public void setUp() throws Exception {
        loginMessage = new HttpMessage();
        HttpRequestHeader header = new HttpRequestHeader();
        header.setURI(new URI("http://www.example.com", true));
        loginMessage.setRequestHeader(header);
        method = Mockito.mock(AuthenticationMethod.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void shouldStoreSetLoggedInIndicator() {
        // Given
        method.setLoggedInIndicatorPattern(LOGGED_IN_INDICATOR);

        // When/Then
        assertEquals(LOGGED_IN_INDICATOR, method.getLoggedInIndicatorPattern().pattern());
    }

    @Test
    public void shouldStoreSetLoggedOutIndicator() {
        // Given
        method.setLoggedOutIndicatorPattern(LOGGED_OUT_INDICATOR);

        // When/Then
        assertEquals(LOGGED_OUT_INDICATOR, method.getLoggedOutIndicatorPattern().pattern());
    }

    @Test
    public void shouldNotStoreNullOrEmptyLoggedInIndicator() {
        // Given
        method.setLoggedInIndicatorPattern(null);

        // When/Then
        assertNull(method.getLoggedInIndicatorPattern());

        // Given
        method.setLoggedInIndicatorPattern("  ");

        // When/Then
        assertNull(method.getLoggedInIndicatorPattern());
    }

    @Test
    public void shouldNotStoreNullOrEmptyLoggedOutIndicator() {
        // Given
        method.setLoggedOutIndicatorPattern(null);

        // When/Then
        assertNull(method.getLoggedOutIndicatorPattern());

        // Given
        method.setLoggedOutIndicatorPattern("  ");

        // When/Then
        assertNull(method.getLoggedOutIndicatorPattern());
    }

    @Test
    public void shouldIdentifyLoggedInResponseBodyWhenLoggedInIndicatorIsSet() {
        // Given
        method.setLoggedInIndicatorPattern(LOGGED_IN_INDICATOR);
        loginMessage.setResponseBody(LOGGED_IN_BODY);

        // When/Then
        assertThat(method.isAuthenticated(loginMessage), is(true));
    }

    @Test
    public void shouldIdentifyLoggedOutResponseBodyWhenLoggedInIndicatorIsSet() {
        // Given
        method.setLoggedInIndicatorPattern(LOGGED_IN_INDICATOR);
        loginMessage.setResponseBody(LOGGED_OUT_BODY);

        // When/Then
        assertThat(method.isAuthenticated(loginMessage), is(false));
    }

    @Test
    public void shouldIdentifyLoggedInResponseHeaderWhenLoggedInIndicatorIsSet() {
        // Given
        method.setLoggedInIndicatorPattern(LOGGED_IN_INDICATOR);
        loginMessage.getResponseHeader().addHeader("test", LOGGED_IN_INDICATOR);

        // When/Then
        assertThat(method.isAuthenticated(loginMessage), is(true));
    }

    @Test
    public void shouldIdentifyLoggedOutResponseHeaderWhenLoggedInIndicatorIsSet() {
        // Given
        method.setLoggedInIndicatorPattern(LOGGED_IN_INDICATOR);
        loginMessage.getResponseHeader().addHeader("test", LOGGED_OUT_INDICATOR);

        // When/Then
        assertThat(method.isAuthenticated(loginMessage), is(false));
    }

    @Test
    public void shouldIdentifyLoggedOutResponseBodyWhenLoggedOutIndicatorIsSet() {
        // Given
        method.setLoggedOutIndicatorPattern(LOGGED_OUT_INDICATOR);
        loginMessage.setResponseBody(LOGGED_OUT_BODY);

        // When/Then
        assertThat(method.isAuthenticated(loginMessage), is(false));
    }

    @Test
    public void shouldIdentifyLoggedInResponseBodyWhenLoggedOutIndicatorIsSet() {
        // Given
        method.setLoggedOutIndicatorPattern(LOGGED_OUT_INDICATOR);
        loginMessage.setResponseBody(LOGGED_IN_BODY);

        // When/Then
        assertThat(method.isAuthenticated(loginMessage), is(true));
    }

    @Test
    public void shouldIdentifyLoggedOutResponseHeaderWhenLoggedOutIndicatorIsSet() {
        // Given
        method.setLoggedOutIndicatorPattern(LOGGED_OUT_INDICATOR);
        loginMessage.getResponseHeader().addHeader("test", LOGGED_OUT_INDICATOR);

        // When/Then
        assertThat(method.isAuthenticated(loginMessage), is(false));
    }

    @Test
    public void shouldIdentifyLoggedInResponseHeaderWhenLoggedOutIndicatorIsSet() {
        // Given
        method.setLoggedOutIndicatorPattern(LOGGED_OUT_INDICATOR);
        loginMessage.getResponseHeader().addHeader("test", LOGGED_IN_INDICATOR);

        // When/Then
        assertThat(method.isAuthenticated(loginMessage), is(true));
    }

    @Test
    public void shouldIdentifyLoggedOutResponseWithComplexRegex() {
        // Given
        method.setLoggedOutIndicatorPattern(LOGGED_OUT_COMPLEX_INDICATOR);
        loginMessage.setResponseBody(LOGGED_OUT_COMPLEX_BODY);

        // When/Then
        assertThat(method.isAuthenticated(loginMessage), is(false));
    }

    @Test
    public void shouldIdentifyLoggedInResponseWithComplexRegex() {
        // Given
        method.setLoggedOutIndicatorPattern(LOGGED_OUT_COMPLEX_INDICATOR);
        loginMessage.setResponseBody(LOGGED_OUT_BODY);

        // When/Then
        assertThat(method.isAuthenticated(loginMessage), is(true));
    }

    @Test
    public void shouldIdentifyResponseAsLoggedInWhenNoIndicatorIsSet() {
        // Given
        loginMessage.setResponseBody(LOGGED_OUT_BODY);

        // When/Then
        assertThat(method.isAuthenticated(loginMessage), is(true));
    }
}

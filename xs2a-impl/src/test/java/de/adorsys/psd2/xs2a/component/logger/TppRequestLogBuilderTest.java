/*
 * Copyright 2018-2019 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.psd2.xs2a.component.logger;

import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TppRequestLogBuilderTest {
    private static final String X_REQUEST_ID_HEADER = "x-request-id";

    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private TppInfo tppInfo;

    @InjectMocks
    private TppRequestLogBuilder tppRequestLogBuilder;

    @Test
    public void withTpp_shouldAddTppIdAndIp() {
        // When
        tppRequestLogBuilder.withTpp(tppInfo);

        // Then
        verify(tppInfo).getAuthorisationNumber();
        verify(httpServletRequest).getRemoteAddr();
        verifyNoMoreInteractions(tppInfo);
        verifyNoMoreInteractions(httpServletRequest);
    }

    @Test
    public void withRequestUri_shouldAddRequestUri() {
        // When
        tppRequestLogBuilder.withRequestUri();

        // Then
        verify(httpServletRequest).getRequestURI();
        verifyNoMoreInteractions(httpServletRequest);
    }

    @Test
    public void withXRequestId_shouldAddXRequestId() {
        // When
        tppRequestLogBuilder.withXRequestId();

        // Then
        verify(httpServletRequest).getHeader(eq(X_REQUEST_ID_HEADER));
        verifyNoMoreInteractions(httpServletRequest);
    }
}

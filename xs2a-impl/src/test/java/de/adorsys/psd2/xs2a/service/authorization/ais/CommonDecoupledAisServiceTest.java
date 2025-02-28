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

package de.adorsys.psd2.xs2a.service.authorization.ais;

import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.consent.UpdateConsentPsuDataReq;
import de.adorsys.psd2.xs2a.domain.consent.UpdateConsentPsuDataResponse;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aAuthenticationObject;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiAccountConsent;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorisationDecoupledScaResponse;
import de.adorsys.psd2.xs2a.spi.domain.psu.SpiPsuData;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponseStatus;
import de.adorsys.psd2.xs2a.spi.service.AisConsentSpi;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CommonDecoupledAisServiceTest {
    private static final String CONSENT_ID = "Test consentId";
    private static final String PSU_ID = "Test psuId";
    private static final String AUTHORISATION_ID = "Test authorisationId";
    private static final String PSU_SUCCESS_MESSAGE = "Test psuSuccessMessage";
    private static final SpiResponseStatus RESPONSE_STATUS = SpiResponseStatus.LOGICAL_FAILURE;
    private static final ScaStatus FAILED_SCA_STATUS = ScaStatus.FAILED;
    private static final SpiPsuData SPI_PSU_DATA = new SpiPsuData(PSU_ID, null, null, null);
    private static final PsuIdData PSU_ID_DATA = new PsuIdData(PSU_ID, null, null, null);
    private static final SpiContextData SPI_CONTEXT_DATA = new SpiContextData(SPI_PSU_DATA, new TppInfo(), UUID.randomUUID());
    private static final String PSU_ERROR_MESSAGE = "Test psuErrorMessage";
    private static final MessageErrorCode FORMAT_ERROR_CODE = MessageErrorCode.FORMAT_ERROR;
    private static final ScaStatus METHOD_SELECTED_SCA_STATUS = ScaStatus.SCAMETHODSELECTED;
    private static final String AUTHENTICATION_METHOD_ID = "Test authentication method id";
    private static final UpdateConsentPsuDataResponse UPDATE_CONSENT_PSU_DATA_RESPONSE = buildUpdateConsentPsuDataResponse();

    @InjectMocks
    private CommonDecoupledAisService commonDecoupledAisService;
    @Mock
    private AisConsentSpi aisConsentSpi;
    @Mock
    private RequestProviderService requestProviderService;
    @Mock
    private SpiErrorMapper spiErrorMapper;
    @Mock
    private SpiContextDataProvider spiContextDataProvider;
    @Mock
    private SpiAccountConsent spiAccountConsent;
    @Mock
    private UpdateConsentPsuDataReq request;
    @Mock
    private SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory;
    @Mock
    private SpiAspspConsentDataProvider spiAspspConsentDataProvider;

    @Before
    public void setUp() {
        when(spiContextDataProvider.provideWithPsuIdData(PSU_ID_DATA))
            .thenReturn(SPI_CONTEXT_DATA);

        when(request.getAuthorizationId())
            .thenReturn(AUTHORISATION_ID);

        when(request.getConsentId())
            .thenReturn(CONSENT_ID);

        when(requestProviderService.getRequestId()).thenReturn(UUID.randomUUID());
        when(aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)).thenReturn(spiAspspConsentDataProvider);

    }

    @Test
    public void proceedDecoupledApproach_by_request_spiAccountConsent_psuData_success() {
        // Given
        when(aisConsentSpi.startScaDecoupled(SPI_CONTEXT_DATA, AUTHORISATION_ID, null, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(new SpiAuthorisationDecoupledScaResponse(PSU_SUCCESS_MESSAGE)));

        // When
        UpdateConsentPsuDataResponse actualResponse = commonDecoupledAisService.proceedDecoupledApproach(request, spiAccountConsent, PSU_ID_DATA);

        // Then
        assertThat(actualResponse).isEqualTo(UPDATE_CONSENT_PSU_DATA_RESPONSE);
    }

    @Test
    public void proceedDecoupledApproach_Success() {
        // Given
        when(aisConsentSpi.startScaDecoupled(SPI_CONTEXT_DATA, AUTHORISATION_ID, AUTHENTICATION_METHOD_ID, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(new SpiAuthorisationDecoupledScaResponse(PSU_SUCCESS_MESSAGE)));

        // When
        UpdateConsentPsuDataResponse actualResponse = commonDecoupledAisService.proceedDecoupledApproach(request, spiAccountConsent, AUTHENTICATION_METHOD_ID, PSU_ID_DATA);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getPsuMessage()).isEqualTo(PSU_SUCCESS_MESSAGE);
        assertThat(actualResponse.getScaStatus()).isEqualTo(METHOD_SELECTED_SCA_STATUS);
    }

    @Test
    public void proceedDecoupledApproach_Failure_StartScaDecoupledHasError() {
        // Given
        when(aisConsentSpi.startScaDecoupled(SPI_CONTEXT_DATA, AUTHORISATION_ID, AUTHENTICATION_METHOD_ID, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildErrorSpiResponse(new SpiAuthorisationDecoupledScaResponse(PSU_ERROR_MESSAGE)));
        when(spiErrorMapper.mapToErrorHolder(buildErrorSpiResponse(new SpiAuthorisationDecoupledScaResponse(PSU_ERROR_MESSAGE)), ServiceType.AIS))
            .thenReturn(ErrorHolder.builder(FORMAT_ERROR_CODE).errorType(ErrorType.AIS_400).build());

        // When
        UpdateConsentPsuDataResponse actualResponse = commonDecoupledAisService.proceedDecoupledApproach(request, spiAccountConsent, AUTHENTICATION_METHOD_ID, PSU_ID_DATA);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getScaStatus()).isEqualTo(FAILED_SCA_STATUS);
        assertThat(actualResponse.getMessageError().getErrorType()).isEqualTo(ErrorType.AIS_400);
    }

    @Test
    public void proceedDecoupledApproach_ShouldContainMethodId() {
        // Given
        when(aisConsentSpi.startScaDecoupled(SPI_CONTEXT_DATA, AUTHORISATION_ID, AUTHENTICATION_METHOD_ID, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(new SpiAuthorisationDecoupledScaResponse(PSU_SUCCESS_MESSAGE)));

        // When
        UpdateConsentPsuDataResponse actualResponse = commonDecoupledAisService.proceedDecoupledApproach(request, spiAccountConsent, AUTHENTICATION_METHOD_ID, PSU_ID_DATA);

        // Then
        String actualMethodId = actualResponse.getChosenScaMethod().getAuthenticationMethodId();
        assertThat(actualMethodId).isEqualTo(AUTHENTICATION_METHOD_ID);
        assertThat(actualResponse.getChosenScaMethodForPsd2Response()).isNull();
    }

    // Needed because SpiResponse is final, so it's impossible to mock it
    private <T> SpiResponse<T> buildSuccessSpiResponse(T payload) {
        return SpiResponse.<T>builder()
                   .payload(payload)
                   .build();
    }

    // Needed because SpiResponse is final, so it's impossible to mock it
    private <T> SpiResponse<T> buildErrorSpiResponse(T payload) {
        return SpiResponse.<T>builder()
                   .payload(payload)
                   .fail(RESPONSE_STATUS);
    }

    private static UpdateConsentPsuDataResponse buildUpdateConsentPsuDataResponse() {
        UpdateConsentPsuDataResponse response = new UpdateConsentPsuDataResponse(METHOD_SELECTED_SCA_STATUS, CONSENT_ID, AUTHORISATION_ID);
        response.setPsuMessage(PSU_SUCCESS_MESSAGE);
        response.setChosenScaMethod(new Xs2aAuthenticationObject());
        return response;
    }
}

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


package de.adorsys.psd2.xs2a.service.authorization.ais.stage.embedded;


import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.consent.AccountConsent;
import de.adorsys.psd2.xs2a.domain.consent.UpdateConsentPsuDataReq;
import de.adorsys.psd2.xs2a.domain.consent.UpdateConsentPsuDataResponse;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aAuthenticationObject;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.ScaApproachResolver;
import de.adorsys.psd2.xs2a.service.authorization.ais.CommonDecoupledAisService;
import de.adorsys.psd2.xs2a.service.consent.Xs2aAisConsentService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.consent.Xs2aAisConsentMapper;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiToXs2aAuthenticationObjectMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiPsuDataMapper;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiAccountConsent;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthenticationObject;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorizationCodeResult;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AisScaMethodSelectedStageTest {
    private static final String CONSENT_ID = "Test consentId";
    private static final String WRONG_CONSENT_ID = "wrong consent id";
    private static final String AUTHORISATION_ID = "Test authorisation id";
    private static final String TEST_AUTHENTICATION_METHOD_ID = "sms";
    private static final ScaStatus METHOD_SELECTED_SCA_STATUS = ScaStatus.SCAMETHODSELECTED;
    private static final SpiResponseStatus RESPONSE_STATUS = SpiResponseStatus.LOGICAL_FAILURE;
    private static final MessageErrorCode ERROR_CODE = MessageErrorCode.FORMAT_ERROR;
    private static final PsuIdData PSU_DATA = new PsuIdData("some psuId", null, null, null);
    private static final SpiPsuData SPI_PSU_DATA = new SpiPsuData(null, null, null, null);
    private static final SpiContextData SPI_CONTEXT_DATA = new SpiContextData(SPI_PSU_DATA, new TppInfo(), UUID.randomUUID());
    private static final ScaStatus FAILED_SCA_STATUS = ScaStatus.FAILED;
    private static final String PSU_SUCCESS_MESSAGE = "Test psuSuccessMessage";
    private static final String AUTHENTICATION_METHOD_ID = "sms";

    @InjectMocks
    private AisScaMethodSelectedStage scaMethodSelectedStage;

    @Mock
    private Xs2aAisConsentService aisConsentService;
    @Mock
    private AisConsentSpi aisConsentSpi;
    @Mock
    private Xs2aAisConsentMapper aisConsentMapper;
    @Mock
    private Xs2aToSpiPsuDataMapper psuDataMapper;
    @Mock
    private UpdateConsentPsuDataReq request;
    @Mock
    private SpiAccountConsent spiAccountConsent;
    @Mock
    private AccountConsent accountConsent;
    @Mock
    private SpiToXs2aAuthenticationObjectMapper spiToXs2aAuthenticationObjectMapper;
    @Mock
    private SpiContextDataProvider spiContextDataProvider;
    @Mock
    private SpiErrorMapper spiErrorMapper;
    @Mock
    private ScaApproachResolver scaApproachResolver;
    @Mock
    private CommonDecoupledAisService commonDecoupledAisService;
    @Mock
    private RequestProviderService requestProviderService;
    @Mock
    private SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory;
    @Mock
    private SpiAspspConsentDataProvider spiAspspConsentDataProvider;

    @Before
    public void setUp() {
        when(request.getConsentId())
            .thenReturn(CONSENT_ID);

        when(aisConsentService.getAccountConsentById(WRONG_CONSENT_ID))
            .thenReturn(Optional.empty());

        when(request.getAuthenticationMethodId())
            .thenReturn(TEST_AUTHENTICATION_METHOD_ID);

        when(request.getAuthorizationId())
            .thenReturn(AUTHORISATION_ID);

        when(request.getPsuData())
            .thenReturn(PSU_DATA);

        when(aisConsentService.getAccountConsentById(CONSENT_ID))
            .thenReturn(Optional.of(accountConsent));

        when(aisConsentMapper.mapToSpiAccountConsent(accountConsent))
            .thenReturn(spiAccountConsent);

        when(spiToXs2aAuthenticationObjectMapper.mapToXs2aAuthenticationObject(buildSpiAuthenticationObject()))
            .thenReturn(buildXs2aAuthenticationObject());

        when(spiContextDataProvider.provideWithPsuIdData(any()))
            .thenReturn(SPI_CONTEXT_DATA);
        when(requestProviderService.getRequestId()).thenReturn(UUID.randomUUID());
        when(aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)).thenReturn(spiAspspConsentDataProvider);

    }

    @Test
    public void apply_Success() {
        when(aisConsentSpi.requestAuthorisationCode(SPI_CONTEXT_DATA, TEST_AUTHENTICATION_METHOD_ID, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildSuccessSpiResponse(buildSpiAuthorizationCodeResult()));

        UpdateConsentPsuDataResponse actualResponse = scaMethodSelectedStage.apply(request);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getChosenScaMethod()).isEqualTo(buildXs2aAuthenticationObject());
        assertThat(actualResponse.getScaStatus()).isEqualTo(METHOD_SELECTED_SCA_STATUS);
    }

    @Test
    public void apply_Success_DecoupledApproach() {
        when(aisConsentService.isAuthenticationMethodDecoupled(anyString(), anyString()))
            .thenReturn(true);
        when(commonDecoupledAisService.proceedDecoupledApproach(any(), any(), eq(AUTHENTICATION_METHOD_ID), any()))
            .thenReturn(buildUpdateConsentPsuDataResponse());

        UpdateConsentPsuDataResponse actualResponse = scaMethodSelectedStage.apply(request);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getPsuMessage()).isEqualTo(PSU_SUCCESS_MESSAGE);
        assertThat(actualResponse.getScaStatus()).isEqualTo(METHOD_SELECTED_SCA_STATUS);
        verify(commonDecoupledAisService).proceedDecoupledApproach(eq(request), eq(spiAccountConsent), eq(AUTHENTICATION_METHOD_ID), any());
    }

    @Test
    public void apply_DecoupledApproach_ShouldChangeScaApproach() {
        when(aisConsentService.isAuthenticationMethodDecoupled(anyString(), anyString())).thenReturn(true);

        scaMethodSelectedStage.apply(request);

        verify(scaApproachResolver, times(1)).forceDecoupledScaApproach();
    }

    @Test
    public void apply_Failure_SpiResponseWithError() {
        when(aisConsentSpi.requestAuthorisationCode(SPI_CONTEXT_DATA, TEST_AUTHENTICATION_METHOD_ID, spiAccountConsent, spiAspspConsentDataProvider))
            .thenReturn(buildErrorSpiResponse());

        when(spiErrorMapper.mapToErrorHolder(buildErrorSpiResponse(), ServiceType.AIS))
            .thenReturn(ErrorHolder.builder(ERROR_CODE).errorType(ErrorType.AIS_400).build());

        UpdateConsentPsuDataResponse actualResponse = scaMethodSelectedStage.apply(request);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getScaStatus()).isEqualTo(FAILED_SCA_STATUS);
        assertThat(actualResponse.getMessageError().getErrorType()).isEqualTo(ErrorType.AIS_400);
    }

    @Test
    public void apply_Identification_wrongId_Failure() {
        //Given
        when(request.getConsentId()).thenReturn(WRONG_CONSENT_ID);

        //When
        UpdateConsentPsuDataResponse actualResponse = scaMethodSelectedStage.apply(request);

        //Then
        assertThat(actualResponse.getScaStatus()).isEqualTo(ScaStatus.FAILED);
        assertThat(actualResponse.getMessageError().getErrorType()).isEqualTo(ErrorType.AIS_400);
        assertThat(actualResponse.getMessageError().getTppMessage().getMessageErrorCode()).isEqualTo(MessageErrorCode.CONSENT_UNKNOWN_400);
    }

    private SpiAuthenticationObject buildSpiAuthenticationObject() {
        SpiAuthenticationObject spiAuthenticationObject = new SpiAuthenticationObject();
        spiAuthenticationObject.setAuthenticationMethodId(AUTHENTICATION_METHOD_ID);
        spiAuthenticationObject.setAuthenticationType("SMS_OTP");
        return spiAuthenticationObject;
    }

    private Xs2aAuthenticationObject buildXs2aAuthenticationObject() {
        Xs2aAuthenticationObject xs2aAuthenticationObject = new Xs2aAuthenticationObject();
        xs2aAuthenticationObject.setAuthenticationMethodId(AUTHENTICATION_METHOD_ID);
        xs2aAuthenticationObject.setAuthenticationType("SMS_OTP");
        return xs2aAuthenticationObject;
    }

    // Needed because SpiResponse is final, so it's impossible to mock it
    private <T> SpiResponse<T> buildSuccessSpiResponse(T payload) {
        return SpiResponse.<T>builder()
                   .payload(payload)
                   .build();
    }

    // Needed because SpiResponse is final, so it's impossible to mock it
    private SpiResponse<SpiAuthorizationCodeResult> buildErrorSpiResponse() {
        return SpiResponse.<SpiAuthorizationCodeResult>builder()
                   .fail(RESPONSE_STATUS);
    }

    private SpiAuthorizationCodeResult buildSpiAuthorizationCodeResult() {
        SpiAuthorizationCodeResult codeResult = new SpiAuthorizationCodeResult();
        codeResult.setSelectedScaMethod(buildSpiAuthenticationObject());
        return codeResult;
    }

    private UpdateConsentPsuDataResponse buildUpdateConsentPsuDataResponse() {
        UpdateConsentPsuDataResponse response = new UpdateConsentPsuDataResponse(ScaStatus.SCAMETHODSELECTED, CONSENT_ID, AUTHORISATION_ID);
        response.setPsuMessage(PSU_SUCCESS_MESSAGE);
        return response;
    }
}


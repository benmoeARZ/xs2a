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

package de.adorsys.psd2.xs2a.integration;


import de.adorsys.psd2.aspsp.profile.domain.AspspSettings;
import de.adorsys.psd2.aspsp.profile.service.AspspProfileService;
import de.adorsys.psd2.consent.api.AspspDataService;
import de.adorsys.psd2.consent.api.CmsAuthorisationType;
import de.adorsys.psd2.consent.api.pis.authorisation.CreatePisAuthorisationResponse;
import de.adorsys.psd2.consent.api.service.PisCommonPaymentServiceEncrypted;
import de.adorsys.psd2.consent.api.service.TppStopListService;
import de.adorsys.psd2.event.service.Xs2aEventServiceEncrypted;
import de.adorsys.psd2.event.service.model.EventBO;
import de.adorsys.psd2.starter.Xs2aStandaloneStarter;
import de.adorsys.psd2.xs2a.config.*;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.profile.ScaRedirectFlow;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.AuthorisationScaApproachResponse;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.integration.builder.AspspSettingsBuilder;
import de.adorsys.psd2.xs2a.integration.builder.TppInfoBuilder;
import de.adorsys.psd2.xs2a.integration.builder.UrlBuilder;
import de.adorsys.psd2.xs2a.integration.builder.payment.PisCommonPaymentResponseBuilder;
import de.adorsys.psd2.xs2a.service.TppService;
import de.adorsys.psd2.xs2a.spi.service.SinglePaymentSpi;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"integration-test", "mock-qwac"})
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(
    classes = Xs2aStandaloneStarter.class)
@ContextConfiguration(classes = {
    CorsConfigurationProperties.class,
    ObjectMapperConfig.class,
    WebConfig.class,
    Xs2aEndpointPathConstant.class,
    Xs2aInterfaceConfig.class
})
public class PaymentControllerTest {
    private static final Charset UTF_8 = Charset.forName("utf-8");
    private static final PaymentType SINGLE_PAYMENT_TYPE = PaymentType.SINGLE;
    private static final ScaStatus SCA_STATUS = ScaStatus.RECEIVED;
    private static final String SEPA_PAYMENT_PRODUCT = "sepa-credit-transfers";
    private static final String ENCRYPT_PAYMENT_ID = "DfLtDOgo1tTK6WQlHlb-TMPL2pkxRlhZ4feMa5F4tOWwNN45XLNAVfWwoZUKlQwb_=_bS6p6XvTWI";
    private static final String AUTHORISATION_ID = "e8356ea7-8e3e-474f-b5ea-2b89346cb2dc";
    private static final String CANCELLATION_ID = "cancellationId";
    private static final TppInfo TPP_INFO = TppInfoBuilder.buildTppInfo();
    private static final ScaApproach SCA_APPROACH = ScaApproach.REDIRECT;
    private HttpHeaders httpHeadersExplicit = new HttpHeaders();

    private static final String CANCELLATION_AUTHORISATIONS_RESP = "/json/payment/res/explicit/SinglePaymentCancellationAuth_response.json";
    private static final String CANCELLATION_AUTHORISATIONS_REDIRECT_OAUTH_RESP = "/json/payment/res/explicit/SinglePaymentCancellationAuth_Redirect_OAuth_response.json";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AspspProfileService aspspProfileService;
    @MockBean
    private TppService tppService;
    @MockBean
    private TppStopListService tppStopListService;
    @MockBean
    private Xs2aEventServiceEncrypted eventServiceEncrypted;
    @MockBean
    private PisCommonPaymentServiceEncrypted pisCommonPaymentServiceEncrypted;
    @MockBean
    private SinglePaymentSpi singlePaymentSpi;
    @MockBean
    private AspspDataService aspspDataService;
    @MockBean
    @Qualifier("consentRestTemplate")
    private RestTemplate consentRestTemplate;

    @Before
    public void init() {
        // common actions for all tests
        given(aspspProfileService.getAspspSettings())
            .willReturn(AspspSettingsBuilder.buildAspspSettings());
        given(tppService.getTppInfo()).willReturn(TPP_INFO);
        given(tppService.getTppId()).willReturn(TPP_INFO.getAuthorisationNumber());
        given(tppStopListService.checkIfTppBlocked(TppInfoBuilder.getTppInfo()))
            .willReturn(false);
        given(eventServiceEncrypted.recordEvent(any(EventBO.class)))
            .willReturn(true);

        given(pisCommonPaymentServiceEncrypted.getPsuDataListByPaymentId(any()))
            .willReturn(Optional.of(Collections.singletonList(getPsuIdData())));

        given(pisCommonPaymentServiceEncrypted.createAuthorizationCancellation(any(), any()))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(CANCELLATION_ID, SCA_STATUS)));
        given(consentRestTemplate.postForEntity(anyString(), any(EventBO.class), eq(Boolean.class)))
            .willReturn(new ResponseEntity<>(true, HttpStatus.OK));

        httpHeadersExplicit.add("Content-Type", "application/json");
        httpHeadersExplicit.add("tpp-qwac-certificate", "qwac certificate");
        httpHeadersExplicit.add("X-Request-ID", "2f77a125-aa7a-45c0-b414-cea25a116035");
        httpHeadersExplicit.add("PSU-ID", "PSU-123");
        httpHeadersExplicit.add("PSU-ID-Type", "Some type");
        httpHeadersExplicit.add("PSU-Corporate-ID", "Some corporate id");
        httpHeadersExplicit.add("PSU-Corporate-ID-Type", "Some corporate id type");
        httpHeadersExplicit.add("PSU-IP-Address", "1.1.1.1");

        // when we use Explicit auth mode we need to set 'true' and value 'signingBasketSupported' in profile also should be 'true'
        httpHeadersExplicit.add("TPP-Explicit-Authorisation-Preferred", "true");
    }

    @Test
    public void getPaymentInitiationScaStatus_successful() throws Exception {
        // Given
        given(aspspProfileService.getScaApproaches()).willReturn(Collections.singletonList(SCA_APPROACH));
        given(pisCommonPaymentServiceEncrypted.getAuthorisationScaStatus(ENCRYPT_PAYMENT_ID, AUTHORISATION_ID, CmsAuthorisationType.CREATED))
            .willReturn(Optional.of(ScaStatus.RECEIVED));
        given(pisCommonPaymentServiceEncrypted.getCommonPaymentById(ENCRYPT_PAYMENT_ID))
            .willReturn(Optional.of(PisCommonPaymentResponseBuilder.buildPisCommonPaymentResponse()));
        given(pisCommonPaymentServiceEncrypted.getAuthorisationScaApproach(AUTHORISATION_ID, CmsAuthorisationType.CREATED))
            .willReturn(Optional.of(new AuthorisationScaApproachResponse(SCA_APPROACH)));

        MockHttpServletRequestBuilder requestBuilder = get(UrlBuilder.buildGetPaymentInitiationScaStatusUrl(SINGLE_PAYMENT_TYPE.getValue(), SEPA_PAYMENT_PRODUCT, ENCRYPT_PAYMENT_ID, AUTHORISATION_ID));
        requestBuilder.headers(httpHeadersExplicit);

        // When
        ResultActions resultActions = mockMvc.perform(requestBuilder);

        //Then
        resultActions.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(content().json("{\"scaStatus\":\"received\"}"));
    }

    @Test
    public void cancelPaymentAuthorisation_successful() throws Exception {
        // Given
        given(aspspProfileService.getScaApproaches()).willReturn(Collections.singletonList(SCA_APPROACH));
        given(pisCommonPaymentServiceEncrypted.getAuthorisationScaStatus(ENCRYPT_PAYMENT_ID, AUTHORISATION_ID, CmsAuthorisationType.CREATED))
            .willReturn(Optional.of(ScaStatus.RECEIVED));
        given(pisCommonPaymentServiceEncrypted.getCommonPaymentById(ENCRYPT_PAYMENT_ID))
            .willReturn(Optional.of(PisCommonPaymentResponseBuilder.buildPisCommonPaymentResponse()));
        given(pisCommonPaymentServiceEncrypted.getAuthorisationScaApproach(CANCELLATION_ID, CmsAuthorisationType.CANCELLED))
            .willReturn(Optional.of(new AuthorisationScaApproachResponse(SCA_APPROACH)));

        MockHttpServletRequestBuilder requestBuilder = post(UrlBuilder.buildGetPaymentCancellationAuthorisationUrl(SINGLE_PAYMENT_TYPE.getValue(), SEPA_PAYMENT_PRODUCT, ENCRYPT_PAYMENT_ID));
        requestBuilder.headers(httpHeadersExplicit);

        // When
        ResultActions resultActions = mockMvc.perform(requestBuilder);

        //Then
        resultActions.andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(content().json(IOUtils.resourceToString(CANCELLATION_AUTHORISATIONS_RESP, UTF_8)));
    }

    @Test
    public void cancelPaymentAuthorisation_Redirect_OAuth_successful() throws Exception {
        // Given
        AspspSettings aspspSettings = AspspSettingsBuilder.buildAspspSettings();
        aspspSettings.setScaRedirectFlow(ScaRedirectFlow.OAUTH);
        given(aspspProfileService.getScaApproaches()).willReturn(Collections.singletonList(SCA_APPROACH));
        given(aspspProfileService.getAspspSettings())
            .willReturn(aspspSettings);
        given(pisCommonPaymentServiceEncrypted.getAuthorisationScaStatus(ENCRYPT_PAYMENT_ID, AUTHORISATION_ID, CmsAuthorisationType.CREATED))
            .willReturn(Optional.of(ScaStatus.RECEIVED));
        given(pisCommonPaymentServiceEncrypted.getCommonPaymentById(ENCRYPT_PAYMENT_ID))
            .willReturn(Optional.of(PisCommonPaymentResponseBuilder.buildPisCommonPaymentResponse()));
        given(pisCommonPaymentServiceEncrypted.getAuthorisationScaApproach(any(String.class), eq(CmsAuthorisationType.CANCELLED)))
            .willReturn(Optional.of(new AuthorisationScaApproachResponse(SCA_APPROACH)));

        MockHttpServletRequestBuilder requestBuilder = post(UrlBuilder.buildGetPaymentCancellationAuthorisationUrl(SINGLE_PAYMENT_TYPE.getValue(), SEPA_PAYMENT_PRODUCT, ENCRYPT_PAYMENT_ID));
        requestBuilder.headers(httpHeadersExplicit);

        // When
        ResultActions resultActions = mockMvc.perform(requestBuilder);

        //Then
        resultActions.andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(content().json(IOUtils.resourceToString(CANCELLATION_AUTHORISATIONS_REDIRECT_OAUTH_RESP, UTF_8)));
    }

    @Test
    public void getPaymentInitiationCancellationAuthorisationInformation() throws Exception {
        // Given
        given(aspspProfileService.getScaApproaches()).willReturn(Collections.singletonList(SCA_APPROACH));
        given(pisCommonPaymentServiceEncrypted.getCommonPaymentById(ENCRYPT_PAYMENT_ID))
            .willReturn(Optional.of(PisCommonPaymentResponseBuilder.buildPisCommonPaymentResponse()));

        List<String> cancellationIds = Arrays.asList("c0121ca2-ab3a-4564-b915-6e40e8b40f50", "743d0a45-7233-4fbf-9799-c657f327836c");
        given(pisCommonPaymentServiceEncrypted.getAuthorisationsByPaymentId(ENCRYPT_PAYMENT_ID, CmsAuthorisationType.CANCELLED))
            .willReturn(Optional.of(cancellationIds));

        MockHttpServletRequestBuilder requestBuilder = get(UrlBuilder.buildGetPaymentInitiationCancellationAuthorisationInformationUrl(SINGLE_PAYMENT_TYPE.getValue(), SEPA_PAYMENT_PRODUCT, ENCRYPT_PAYMENT_ID));
        requestBuilder.headers(httpHeadersExplicit);

        // When
        ResultActions resultActions = mockMvc.perform(requestBuilder);

        //Then
        resultActions.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(content().json(IOUtils.resourceToString("/json/payment/res/Cancellations.json", UTF_8)));
    }

    private PsuIdData getPsuIdData() {
        return new PsuIdData("PSU-123", "Some type", "Some corporate id", "Some corporate id type");
    }
}

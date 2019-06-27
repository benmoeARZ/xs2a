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


import de.adorsys.aspsp.xs2a.spi.ASPSPXs2aApplication;
import de.adorsys.psd2.aspsp.profile.service.AspspProfileService;
import de.adorsys.psd2.consent.api.CmsAuthorisationType;
import de.adorsys.psd2.consent.api.pis.CreatePisCommonPaymentResponse;
import de.adorsys.psd2.consent.api.pis.authorisation.CreatePisAuthorisationRequest;
import de.adorsys.psd2.consent.api.pis.authorisation.CreatePisAuthorisationResponse;
import de.adorsys.psd2.consent.api.pis.proto.PisPaymentInfo;
import de.adorsys.psd2.consent.api.service.EventServiceEncrypted;
import de.adorsys.psd2.consent.api.service.PisCommonPaymentServiceEncrypted;
import de.adorsys.psd2.consent.api.service.TppStopListService;
import de.adorsys.psd2.xs2a.config.*;
import de.adorsys.psd2.xs2a.core.consent.AspspConsentData;
import de.adorsys.psd2.xs2a.core.event.Event;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.sca.AuthorisationScaApproachResponse;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.integration.builder.AspspSettingsBuilder;
import de.adorsys.psd2.xs2a.integration.builder.PsuIdDataBuilder;
import de.adorsys.psd2.xs2a.integration.builder.TppInfoBuilder;
import de.adorsys.psd2.xs2a.integration.builder.UrlBuilder;
import de.adorsys.psd2.xs2a.integration.builder.payment.SpiPaymentInitiationResponseBuilder;
import de.adorsys.psd2.xs2a.service.TppService;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiBulkPayment;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiPeriodicPayment;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiSinglePayment;
import de.adorsys.psd2.xs2a.spi.service.BulkPaymentSpi;
import de.adorsys.psd2.xs2a.spi.service.PeriodicPaymentSpi;
import de.adorsys.psd2.xs2a.spi.service.SinglePaymentSpi;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("integration-test")
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(
    classes = ASPSPXs2aApplication.class)
@ContextConfiguration(classes = {
    CorsConfigurationProperties.class,
    ObjectMapperConfig.class,
    WebConfig.class,
    Xs2aEndpointPathConstant.class,
    Xs2aInterfaceConfig.class
})
public class InitiatePayments_successfulTest {
    private static final Charset UTF_8 = Charset.forName("utf-8");
    private static final String SINGLE_PAYMENT_REQUEST_JSON_PATH = "/json/payment/req/SinglePaymentInitiate_request.json";
    private static final String PERIODIC_PAYMENT_REQUEST_JSON_PATH = "/json/payment/req/PeriodicPaymentInitiate_request.json";
    private static final String BULK_PAYMENT_REQUEST_JSON_PATH = "/json/payment/req/BulkPaymentInitiate_request.json";

    private static final PaymentType SINGLE_PAYMENT_TYPE = PaymentType.SINGLE;
    private static final PaymentType PERIODIC_PAYMENT_TYPE = PaymentType.PERIODIC;
    private static final PaymentType BULK_PAYMENT_TYPE = PaymentType.BULK;

    private static final String SEPA_PAYMENT_PRODUCT = "sepa-credit-transfers";
    private static final String ENCRYPT_PAYMENT_ID = "DfLtDOgo1tTK6WQlHlb-TMPL2pkxRlhZ4feMa5F4tOWwNN45XLNAVfWwoZUKlQwb_=_bS6p6XvTWI";
    private static final String AUTHORISATION_ID = "e8356ea7-8e3e-474f-b5ea-2b89346cb2dc";
    private static final TppInfo TPP_INFO = TppInfoBuilder.buildTppInfo();

    private static final ScaStatus SCA_STATUS = ScaStatus.RECEIVED;

    private HttpHeaders httpHeadersImplicit = new HttpHeaders();
    private HttpHeaders httpHeadersImplicitNoPsuData = new HttpHeaders();
    private HttpHeaders httpHeadersExplicit = new HttpHeaders();
    private HttpHeaders httpHeadersExplicitNoPsuData = new HttpHeaders();
    private MultiKeyMap responseMap = new MultiKeyMap();
    private MultiKeyMap responseMapSigningBasketMode = new MultiKeyMap();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AspspProfileService aspspProfileService;
    @MockBean
    private TppService tppService;
    @MockBean
    private TppStopListService tppStopListService;
    @MockBean
    private EventServiceEncrypted eventServiceEncrypted;
    @MockBean
    private PisCommonPaymentServiceEncrypted pisCommonPaymentServiceEncrypted;
    @MockBean
    private SinglePaymentSpi singlePaymentSpi;
    @MockBean
    private PeriodicPaymentSpi periodicPaymentSpi;
    @MockBean
    private BulkPaymentSpi bulkPaymentSpi;

    @MockBean
    @Qualifier("consentRestTemplate")
    private RestTemplate consentRestTemplate;

    @Before
    public void init() {
        HashMap<String, String> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "application/json");
        headerMap.put("tpp-qwac-certificate", "qwac certificate");
        headerMap.put("X-Request-ID", "2f77a125-aa7a-45c0-b414-cea25a116035");
        headerMap.put("PSU-ID", "PSU-123");
        headerMap.put("PSU-ID-Type", "Some type");
        headerMap.put("PSU-Corporate-ID", "Some corporate id");
        headerMap.put("PSU-Corporate-ID-Type", "Some corporate id type");
        headerMap.put("PSU-IP-Address", "1.1.1.1");

        httpHeadersImplicit.setAll(headerMap);
        // when Implicit auth mode we need to set 'false'
        httpHeadersImplicit.add("TPP-Implicit-Authorisation-Preferred", "false");

        httpHeadersImplicitNoPsuData.putAll(httpHeadersImplicit);
        httpHeadersImplicitNoPsuData.remove("PSU-ID");
        httpHeadersImplicitNoPsuData.remove("PSU-ID-Type");
        httpHeadersImplicitNoPsuData.remove("PSU-Corporate-ID");
        httpHeadersImplicitNoPsuData.remove("PSU-Corporate-ID-Type");

        httpHeadersExplicit.setAll(headerMap);
        // when we use Explicit auth mode we need to set 'true' and value 'signingBasketSupported' in profile also should be 'true'
        httpHeadersExplicit.add("TPP-Explicit-Authorisation-Preferred", "true");

        httpHeadersExplicitNoPsuData.putAll(httpHeadersExplicit);
        httpHeadersExplicitNoPsuData.remove("PSU-ID");
        httpHeadersExplicitNoPsuData.remove("PSU-ID-Type");
        httpHeadersExplicitNoPsuData.remove("PSU-Corporate-ID");
        httpHeadersExplicitNoPsuData.remove("PSU-Corporate-ID-Type");

        responseMap.put(false, PaymentType.SINGLE, ScaApproach.REDIRECT, "", "", "/json/payment/res/implicit/SinglePaymentInitiate_redirect_implicit_response.json");
        responseMap.put(false, PaymentType.SINGLE, ScaApproach.REDIRECT, "", "psuIdDataIsEmpty", "/json/payment/res/implicit/SinglePaymentInitiate_redirect_implicit_psuIdDataIsEmpty_response.json");
        responseMap.put(false, PaymentType.SINGLE, ScaApproach.REDIRECT, "multilevelSca", "", "/json/payment/res/implicit/SinglePaymentInitiate_redirect_implicit_multilevelSca_response.json");
        responseMap.put(false, PaymentType.SINGLE, ScaApproach.REDIRECT, "multilevelSca", "psuIdDataIsEmpty", "/json/payment/res/implicit/SinglePaymentInitiate_redirect_implicit_multilevelSca_psuIdDataIsEmpty_response.json");
        responseMap.put(false, PaymentType.SINGLE, ScaApproach.EMBEDDED, "", "", "/json/payment/res/implicit/SinglePaymentInitiate_embedded_implicit_response.json");
        responseMap.put(false, PaymentType.SINGLE, ScaApproach.EMBEDDED, "", "psuIdDataIsEmpty", "/json/payment/res/implicit/SinglePaymentInitiate_embedded_implicit_psuIdDataIsEmpty_response.json");
        responseMap.put(false, PaymentType.SINGLE, ScaApproach.EMBEDDED, "multilevelSca", "", "/json/payment/res/implicit/SinglePaymentInitiate_embedded_implicit_multilevelSca_response.json");
        responseMap.put(false, PaymentType.SINGLE, ScaApproach.EMBEDDED, "multilevelSca", "psuIdDataIsEmpty", "/json/payment/res/implicit/SinglePaymentInitiate_embedded_implicit_multilevelSca_psuIdDataIsEmpty_response.json");
        responseMap.put(false, PaymentType.PERIODIC, ScaApproach.REDIRECT, "/json/payment/res/implicit/PeriodicPaymentInitiate_redirect_implicit_response.json");
        responseMap.put(false, PaymentType.PERIODIC, ScaApproach.EMBEDDED, "/json/payment/res/implicit/PeriodicPaymentInitiate_embedded_implicit_response.json");
        responseMap.put(false, PaymentType.BULK, ScaApproach.REDIRECT, "/json/payment/res/implicit/BulkPaymentInitiate_redirect_implicit_response.json");
        responseMap.put(false, PaymentType.BULK, ScaApproach.EMBEDDED, "/json/payment/res/implicit/BulkPaymentInitiate_embedded_implicit_response.json");

        responseMap.put(true, PaymentType.SINGLE, ScaApproach.REDIRECT, "", "", "/json/payment/res/explicit/SinglePaymentInitiate_redirect_explicit_response.json");
        responseMap.put(true, PaymentType.SINGLE, ScaApproach.REDIRECT, "", "psuIdDataIsEmpty", "/json/payment/res/explicit/SinglePaymentInitiate_redirect_explicit_psuIdDataIsEmpty_response.json");
        responseMap.put(true, PaymentType.SINGLE, ScaApproach.REDIRECT, "multilevelSca", "", "/json/payment/res/explicit/SinglePaymentInitiate_redirect_explicit_multilevelSca_response.json");
        responseMap.put(true, PaymentType.SINGLE, ScaApproach.REDIRECT, "multilevelSca", "psuIdDataIsEmpty", "/json/payment/res/explicit/SinglePaymentInitiate_redirect_explicit_multilevelSca_psuIdDataIsEmpty_response.json");
        responseMap.put(true, PaymentType.SINGLE, ScaApproach.EMBEDDED, "", "", "/json/payment/res/explicit/SinglePaymentInitiate_redirect_explicit_response.json");
        responseMap.put(true, PaymentType.SINGLE, ScaApproach.EMBEDDED, "", "psuIdDataIsEmpty", "/json/payment/res/explicit/SinglePaymentInitiate_redirect_explicit_psuIdDataIsEmpty_response.json");
        responseMap.put(true, PaymentType.SINGLE, ScaApproach.EMBEDDED, "multilevelSca", "", "/json/payment/res/explicit/SinglePaymentInitiate_embedded_explicit_multilevelSca_response.json");
        responseMap.put(true, PaymentType.SINGLE, ScaApproach.EMBEDDED, "multilevelSca", "psuIdDataIsEmpty", "/json/payment/res/explicit/SinglePaymentInitiate_embedded_explicit_multilevelSca_psuIdDataIsEmpty_response.json");
        responseMap.put(true, PaymentType.PERIODIC, ScaApproach.REDIRECT, "/json/payment/res/explicit/PeriodicPaymentInitiate_redirect_explicit_response.json");
        responseMap.put(true, PaymentType.PERIODIC, ScaApproach.EMBEDDED, "/json/payment/res/explicit/PeriodicPaymentInitiate_redirect_explicit_response.json");
        responseMap.put(true, PaymentType.BULK, ScaApproach.REDIRECT, "/json/payment/res/explicit/BulkPaymentInitiate_redirect_explicit_response.json");
        responseMap.put(true, PaymentType.BULK, ScaApproach.EMBEDDED, "/json/payment/res/explicit/BulkPaymentInitiate_redirect_explicit_response.json");

        responseMapSigningBasketMode.put(true, PaymentType.SINGLE, ScaApproach.EMBEDDED, "multilevelSca", "psuIdDataIsEmpty", "/json/payment/res/explicit/SinglePaymentInitiate_embedded_explicit_multilevelSca_psuIdDataIsEmpty_signingBasketActive_response.json");
        responseMapSigningBasketMode.put(true, PaymentType.SINGLE, ScaApproach.EMBEDDED, "multilevelSca", "", "/json/payment/res/explicit/SinglePaymentInitiate_embedded_explicit_multilevelSca_signingBasketActive_response.json");

        given(aspspProfileService.getAspspSettings())
            .willReturn(AspspSettingsBuilder.buildAspspSettings());
        given(tppService.getTppInfo())
            .willReturn(TPP_INFO);
        given(tppService.getTppId())
            .willReturn(TPP_INFO.getAuthorisationNumber());
        given(tppStopListService.checkIfTppBlocked(TppInfoBuilder.buildTppUniqueParamsHolder()))
            .willReturn(false);
        given(eventServiceEncrypted.recordEvent(any(Event.class)))
            .willReturn(true);

        given(pisCommonPaymentServiceEncrypted.createCommonPayment(any(PisPaymentInfo.class)))
            .willReturn(Optional.of(new CreatePisCommonPaymentResponse(ENCRYPT_PAYMENT_ID)));
    }

    // =============== IMPLICIT MODE
    //
    @Test
    public void initiateSinglePayment_implicit_embedded_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.EMBEDDED)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersImplicit, ScaApproach.EMBEDDED, false, false);
    }

    @Test
    public void initiateSinglePayment_implicit_embedded_psuIdDataIsEmpty_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequestWithEmptyPsuIdData(ScaApproach.EMBEDDED)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersImplicitNoPsuData, ScaApproach.EMBEDDED, false, true);
    }

    @Test
    public void initiateSinglePayment_implicit_embedded_multilevelSca_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.EMBEDDED)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersImplicit, ScaApproach.EMBEDDED, true, false);
    }

    @Test
    public void initiateSinglePayment_implicit_embedded_multilevelSca_psuIdDataIsEmpty_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.EMBEDDED)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersImplicitNoPsuData, ScaApproach.EMBEDDED, true, true);
    }

    @Test
    public void initiateSinglePayment_implicit_redirect_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.REDIRECT)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersImplicit, ScaApproach.REDIRECT, false, false);
    }

    @Test
    public void initiateSinglePayment_implicit_redirect_psuIdDataIsEmpty_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequestWithEmptyPsuIdData(ScaApproach.REDIRECT)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersImplicitNoPsuData, ScaApproach.REDIRECT, false, true);
    }

    @Test
    public void initiateSinglePayment_implicit_redirect_multilevelSca_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.REDIRECT)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersImplicit, ScaApproach.REDIRECT, true, false);
    }

    @Test
    public void initiateSinglePayment_implicit_redirect_multilevelSca_psuIdDataIsEmpty_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequestWithEmptyPsuIdData(ScaApproach.REDIRECT)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersImplicitNoPsuData, ScaApproach.REDIRECT, true, true);
    }

    @Test
    public void initiatePeriodicPayment_implicit_embedded_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.EMBEDDED)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiatePeriodicPayment_successful(httpHeadersImplicit, ScaApproach.EMBEDDED);
    }

    @Test
    public void initiatePeriodicPayment_implicit_redirect_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.REDIRECT)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiatePeriodicPayment_successful(httpHeadersImplicit, ScaApproach.REDIRECT);
    }

    @Test
    public void initiateBulkPayment_implicit_embedded_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.EMBEDDED)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateBulkPayment_successful(httpHeadersImplicit, ScaApproach.EMBEDDED);
    }

    @Test
    public void initiateBulkPayment_implicit_redirect_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.REDIRECT)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateBulkPayment_successful(httpHeadersImplicit, ScaApproach.REDIRECT);
    }

    // =============== EXPLICIT MODE
    //

    @Test
    public void initiateSinglePayment_explicit_embedded_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.EMBEDDED)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersExplicit, ScaApproach.EMBEDDED, false, false);
    }

    @Test
    public void initiateSinglePayment_explicit_embedded_psuIdDataIsEmpty_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequestWithEmptyPsuIdData(ScaApproach.EMBEDDED)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersExplicitNoPsuData, ScaApproach.EMBEDDED, false, true);
    }

    @Test
    public void initiateSinglePayment_explicit_embedded_multilevelSca_successful() throws Exception {
        aspspProfileService.getAspspSettings().setSigningBasketSupported(false);
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.EMBEDDED)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersExplicit, ScaApproach.EMBEDDED, true, false);
    }

    @Test
    public void initiateSinglePayment_explicit_embedded_multilevelSca_psuIdDataIsEmpty_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequestWithEmptyPsuIdData(ScaApproach.EMBEDDED)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersExplicitNoPsuData, ScaApproach.EMBEDDED, true, true);
    }

    @Test
    public void initiateSinglePayment_explicit_embedded_multilevelSca_psuIdDataIsEmpty_signingBasketActive_successful() throws Exception {
        aspspProfileService.getAspspSettings().setSigningBasketSupported(true);
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequestWithEmptyPsuIdData(ScaApproach.EMBEDDED)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersExplicitNoPsuData, ScaApproach.EMBEDDED, true, true);
    }

    @Test
    public void initiateSinglePayment_explicit_embedded_multilevelSca_signingBasketActive_successful() throws Exception {
        aspspProfileService.getAspspSettings().setSigningBasketSupported(true);
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.EMBEDDED)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersExplicit, ScaApproach.EMBEDDED, true, false);
    }

    @Test
    public void initiateSinglePayment_explicit_redirect_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.REDIRECT)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersExplicit, ScaApproach.REDIRECT, false, false);
    }

    @Test
    public void initiateSinglePayment_explicit_redirect_psuIdDataIsEmpty_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequestWithEmptyPsuIdData(ScaApproach.REDIRECT)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersExplicitNoPsuData, ScaApproach.REDIRECT, false, true);
    }

    @Test
    public void initiateSinglePayment_explicit_redirect_multilevelSca_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.REDIRECT)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersExplicit, ScaApproach.REDIRECT, true, false);
    }

    @Test
    public void initiateSinglePayment_explicit_redirect_multilevelSca_psuIdDataIsEmpty_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequestWithEmptyPsuIdData(ScaApproach.REDIRECT)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateSinglePayment_successful(httpHeadersExplicitNoPsuData, ScaApproach.REDIRECT, true, true);
    }

    @Test
    public void initiatePeriodicPayment_explicit_embedded_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.EMBEDDED)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiatePeriodicPayment_successful(httpHeadersExplicit, ScaApproach.EMBEDDED);
    }

    @Test
    public void initiatePeriodicPayment_explicit_redirect_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.REDIRECT)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiatePeriodicPayment_successful(httpHeadersExplicit, ScaApproach.REDIRECT);
    }

    @Test
    public void initiateBulkPayment_explicit_embedded_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.EMBEDDED)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateBulkPayment_successful(httpHeadersExplicit, ScaApproach.EMBEDDED);
    }

    @Test
    public void initiateBulkPayment_explicit_redirect_successful() throws Exception {
        given(pisCommonPaymentServiceEncrypted.createAuthorization(ENCRYPT_PAYMENT_ID, getPisAuthorisationRequest(ScaApproach.REDIRECT)))
            .willReturn(Optional.of(new CreatePisAuthorisationResponse(AUTHORISATION_ID, SCA_STATUS)));
        initiateBulkPayment_successful(httpHeadersExplicit, ScaApproach.REDIRECT);
    }

    private CreatePisAuthorisationRequest getPisAuthorisationRequest(ScaApproach scaApproach) {
        return new CreatePisAuthorisationRequest(CmsAuthorisationType.CREATED, PsuIdDataBuilder.buildPsuIdData(), scaApproach);
    }

    private CreatePisAuthorisationRequest getPisAuthorisationRequestWithEmptyPsuIdData(ScaApproach scaApproach) {
        return new CreatePisAuthorisationRequest(CmsAuthorisationType.CREATED, PsuIdDataBuilder.buildEmptyPsuIdData(), scaApproach);
    }

    private void initiateSinglePayment_successful(HttpHeaders headers, ScaApproach scaApproach, boolean multilevelSca, boolean isPsuIdDataEmpty) throws Exception {
        // Given
        given(aspspProfileService.getScaApproaches()).willReturn(Collections.singletonList(scaApproach));
        given(singlePaymentSpi.initiatePayment(any(SpiContextData.class), any(SpiSinglePayment.class), any(AspspConsentData.class)))
            .willReturn(SpiPaymentInitiationResponseBuilder.buildSinglePaymentResponse(multilevelSca));

        given(consentRestTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class), any(String.class)))
            .willReturn(ResponseEntity.ok(Void.class));
        given(pisCommonPaymentServiceEncrypted.getAuthorisationScaApproach(AUTHORISATION_ID, CmsAuthorisationType.CREATED))
            .willReturn(Optional.of(new AuthorisationScaApproachResponse(scaApproach)));

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post(UrlBuilder.buildInitiatePaymentUrl(SINGLE_PAYMENT_TYPE.getValue(), SEPA_PAYMENT_PRODUCT));
        requestBuilder.headers(headers);
        requestBuilder.content(IOUtils.resourceToString(SINGLE_PAYMENT_REQUEST_JSON_PATH, UTF_8));

        // When
        ResultActions resultActions = mockMvc.perform(requestBuilder);

        String filePath = isSigningBasketModeActive(headers)
                              ? (String) responseMapSigningBasketMode.get(isExplicitMethod(headers, multilevelSca), PaymentType.SINGLE, scaApproach, multilevelScaKey(multilevelSca), psuIdDataEmptyKey(isPsuIdDataEmpty))
                              : (String) responseMap.get(isExplicitMethod(headers, multilevelSca), PaymentType.SINGLE, scaApproach, multilevelScaKey(multilevelSca), psuIdDataEmptyKey(isPsuIdDataEmpty));

        //Then
        resultActions.andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(content().json(IOUtils.resourceToString(filePath, UTF_8)));
    }

    private void initiatePeriodicPayment_successful(HttpHeaders headers, ScaApproach scaApproach) throws Exception {
        // Given
        given(aspspProfileService.getScaApproaches()).willReturn(Collections.singletonList(scaApproach));
        given(periodicPaymentSpi.initiatePayment(any(SpiContextData.class), any(SpiPeriodicPayment.class), any(AspspConsentData.class)))
            .willReturn(SpiPaymentInitiationResponseBuilder.buildPeriodicPaymentResponse());

        given(consentRestTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class), any(String.class)))
            .willReturn(ResponseEntity.ok(Void.class));
        given(pisCommonPaymentServiceEncrypted.getAuthorisationScaApproach(AUTHORISATION_ID, CmsAuthorisationType.CREATED))
            .willReturn(Optional.of(new AuthorisationScaApproachResponse(scaApproach)));

        MockHttpServletRequestBuilder requestBuilder = post(UrlBuilder.buildInitiatePaymentUrl(PERIODIC_PAYMENT_TYPE.getValue(), SEPA_PAYMENT_PRODUCT));
        requestBuilder.headers(headers);
        requestBuilder.content(IOUtils.resourceToString(PERIODIC_PAYMENT_REQUEST_JSON_PATH, UTF_8));

        // When
        ResultActions resultActions = mockMvc.perform(requestBuilder);

        //Then
        resultActions.andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(content().json(IOUtils.resourceToString((String) responseMap.get(isExplicitMethod(headers, false), PaymentType.PERIODIC, scaApproach), UTF_8)));
    }

    private void initiateBulkPayment_successful(HttpHeaders headers, ScaApproach scaApproach) throws Exception {
        // Given
        HashMap<String, BigDecimal> amountMap = new HashMap<>();
        amountMap.put("DE21500105176194357737", new BigDecimal("666"));
        amountMap.put("DE54500105173424724776", new BigDecimal("888"));
        given(aspspProfileService.getScaApproaches()).willReturn(Collections.singletonList(scaApproach));

        given(bulkPaymentSpi.initiatePayment(any(SpiContextData.class), any(SpiBulkPayment.class), any(AspspConsentData.class)))
            .willReturn(SpiPaymentInitiationResponseBuilder.buildBulkPaymentResponse());

        given(consentRestTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), any(Class.class), any(String.class)))
            .willReturn(ResponseEntity.ok(Void.class));
        given(pisCommonPaymentServiceEncrypted.getAuthorisationScaApproach(AUTHORISATION_ID, CmsAuthorisationType.CREATED))
            .willReturn(Optional.of(new AuthorisationScaApproachResponse(scaApproach)));

        MockHttpServletRequestBuilder requestBuilder = post(UrlBuilder.buildInitiatePaymentUrl(BULK_PAYMENT_TYPE.getValue(), SEPA_PAYMENT_PRODUCT));
        requestBuilder.headers(headers);
        requestBuilder.content(IOUtils.resourceToString(BULK_PAYMENT_REQUEST_JSON_PATH, UTF_8));

        // When
        ResultActions resultActions = mockMvc.perform(requestBuilder);

        //Then
        resultActions.andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(content().json(IOUtils.resourceToString((String) responseMap.get(isExplicitMethod(headers, false), PaymentType.BULK, scaApproach), UTF_8)));
    }

    private String multilevelScaKey(boolean multilevelSca) {
        return multilevelSca ? "multilevelSca" : "";
    }

    private String psuIdDataEmptyKey(boolean isPsuIdDataEmpty) {
        return isPsuIdDataEmpty ? "psuIdDataIsEmpty" : "";
    }

    public boolean isExplicitMethod(HttpHeaders headers, boolean multilevelScaRequired) {
        return multilevelScaRequired || isSigningBasketModeActive(headers);
    }

    public boolean isSigningBasketModeActive(HttpHeaders headers) {
        boolean tppExplicitAuthorisationPreferred = Boolean.valueOf(headers.toSingleValueMap().get("TPP-Explicit-Authorisation-Preferred"));
        return tppExplicitAuthorisationPreferred && aspspProfileService.getAspspSettings().isSigningBasketSupported();
    }
}

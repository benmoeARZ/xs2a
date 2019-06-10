/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
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

package de.adorsys.psd2.aspsp.profile.web;

import de.adorsys.psd2.aspsp.profile.domain.AspspSettings;
import de.adorsys.psd2.aspsp.profile.domain.MulticurrencyAccountLevel;
import de.adorsys.psd2.aspsp.profile.domain.SupportedAccountReferenceField;
import de.adorsys.psd2.aspsp.profile.service.AspspProfileService;
import de.adorsys.psd2.xs2a.core.ais.BookingStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.profile.ScaRedirectFlow;
import de.adorsys.psd2.xs2a.core.profile.StartAuthorisationMode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static de.adorsys.psd2.aspsp.profile.domain.SupportedAccountReferenceField.IBAN;
import static de.adorsys.psd2.xs2a.core.ais.BookingStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AspspProfileControllerTest {
    private static final int FREQUENCY_PER_DAY = 5;
    private static final boolean COMBINED_SERVICE_INDICATOR = false;
    private static final boolean TPP_SIGNATURE_REQUIRED = false;
    private static final String PIS_REDIRECT_LINK = "https://localhost/payment/confirmation/";
    private static final String AIS_REDIRECT_LINK = "https://localhost/view/account/";
    private static final MulticurrencyAccountLevel MULTICURRENCY_ACCOUNT_LEVEL = MulticurrencyAccountLevel.SUBACCOUNT;
    private static final List<BookingStatus> AVAILABLE_BOOKING_STATUSES = getBookingStatuses();
    private static final List<SupportedAccountReferenceField> SUPPORTED_ACCOUNT_REFERENCE_FIELDS = getSupportedAccountReferenceFields();
    private static final int CONSENT_LIFETIME = 0;
    private static final int TRANSACTION_LIFETIME = 0;
    private static final boolean ALL_PSD_2_SUPPORT = false;
    private static final boolean BANK_OFFERED_CONSENT_SUPPORT = false;
    private static final boolean TRANSACTIONS_WITHOUT_BALANCES_SUPPORTED = false;
    private static final boolean SIGNING_BASKET_SUPPORTED = true;
    private static final boolean PAYMENT_CANCELLATION_AUTHORIZATION_MANDATED = false;
    private static final boolean PIIS_CONSENT_SUPPORTED = false;
    private static final boolean DELTA_LIST_SUPPORTED = false;
    private static final long REDIRECT_URL_EXPIRATION_TIME_MS = 600000;
    private static final long NOT_CONFIRMED_CONSENT_EXPIRATION_PERIOD_MS = 86400000;
    private static final long NOT_CONFIRMED_PAYMENT_EXPIRATION_PERIOD_MS = 86400000;
    private static final String PIS_PAYMENT_CANCELLATION_REDIRECT_URL_TO_ASPSP = "https://localhost/payment/cancellation/";
    private static Map<PaymentType, Set<String>> SUPPORTED_PAYMENT_TYPE_AND_PRODUCT_MATRIX = buildSupportedPaymentTypeAndProductMatrix();
    private static final long PAYMENT_CANCELLATION_REDIRECT_URL_EXPIRATION_TIME_MS = 600000;
    private static final boolean AVAILABLE_ACCOUNTS_CONSENT_SUPPORTED = false;
    private static final boolean SCA_BY_ONE_TIME_AVAILABLE_ACCOUNTS_CONSENT_REQUIRED = false;
    private static final boolean PSU_IN_INITIAL_REQUEST_MANDATED = false;
    private static final boolean FORCE_XS2A_BASE_URL = false;
    private static final String XS2A_BASEURL = "http://myhost.com/";
    private static final ScaRedirectFlow SCA_REDIRECT_FLOW = ScaRedirectFlow.REDIRECT;
    private static final List<String> SUPPORTED_TRANSACTION_APPLICATION_TYPES = Arrays.asList("JSON", "XML");
    private static final boolean ENTRY_REFERENCE_FROM_SUPPORTED = true;
    private static final StartAuthorisationMode START_AUTHORISATION_MODE = StartAuthorisationMode.AUTO;

    @InjectMocks
    private AspspProfileController aspspProfileController;

    @Mock
    private AspspProfileService aspspProfileService;

    @Before
    public void setUpAccountServiceMock() {
        when(aspspProfileService.getAspspSettings())
            .thenReturn(buildAspspSettings());
        when(aspspProfileService.getScaApproaches())
            .thenReturn(Collections.singletonList(ScaApproach.REDIRECT));
    }

    @Test
    public void getAspspSettings() {
        //Given:
        HttpStatus expectedStatusCode = HttpStatus.OK;

        //When:
        ResponseEntity<AspspSettings> actualResponse = aspspProfileController.getAspspSettings();

        //Then:
        assertThat(actualResponse.getStatusCode()).isEqualTo(expectedStatusCode);
        assertThat(actualResponse.getBody()).isEqualTo(buildAspspSettings());
    }

    @Test
    public void getScaApproach() {
        //Given:
        HttpStatus expectedStatusCode = HttpStatus.OK;

        //When:
        ResponseEntity<List<ScaApproach>> actualResponse = aspspProfileController.getScaApproaches();

        //Then:
        assertThat(actualResponse.getStatusCode()).isEqualTo(expectedStatusCode);
        assertThat(actualResponse.getBody()).isEqualTo(Collections.singletonList(ScaApproach.REDIRECT));
    }

    private static AspspSettings buildAspspSettings() {
        return new AspspSettings(
            FREQUENCY_PER_DAY,
            COMBINED_SERVICE_INDICATOR,
            TPP_SIGNATURE_REQUIRED,
            PIS_REDIRECT_LINK,
            AIS_REDIRECT_LINK,
            MULTICURRENCY_ACCOUNT_LEVEL,
            BANK_OFFERED_CONSENT_SUPPORT,
            AVAILABLE_BOOKING_STATUSES,
            SUPPORTED_ACCOUNT_REFERENCE_FIELDS,
            CONSENT_LIFETIME,
            TRANSACTION_LIFETIME,
            ALL_PSD_2_SUPPORT,
            TRANSACTIONS_WITHOUT_BALANCES_SUPPORTED,
            SIGNING_BASKET_SUPPORTED,
            PAYMENT_CANCELLATION_AUTHORIZATION_MANDATED,
            PIIS_CONSENT_SUPPORTED,
            REDIRECT_URL_EXPIRATION_TIME_MS,
            PIS_PAYMENT_CANCELLATION_REDIRECT_URL_TO_ASPSP,
            NOT_CONFIRMED_CONSENT_EXPIRATION_PERIOD_MS,
            NOT_CONFIRMED_PAYMENT_EXPIRATION_PERIOD_MS,
            SUPPORTED_PAYMENT_TYPE_AND_PRODUCT_MATRIX,
            PAYMENT_CANCELLATION_REDIRECT_URL_EXPIRATION_TIME_MS,
            AVAILABLE_ACCOUNTS_CONSENT_SUPPORTED,
            SCA_BY_ONE_TIME_AVAILABLE_ACCOUNTS_CONSENT_REQUIRED,
            PSU_IN_INITIAL_REQUEST_MANDATED,
            FORCE_XS2A_BASE_URL,
            XS2A_BASEURL,
            SCA_REDIRECT_FLOW,
            DELTA_LIST_SUPPORTED,
            ENTRY_REFERENCE_FROM_SUPPORTED,
            SUPPORTED_TRANSACTION_APPLICATION_TYPES,
            START_AUTHORISATION_MODE);
    }

    private static List<SupportedAccountReferenceField> getSupportedAccountReferenceFields() {
        return Collections.singletonList(IBAN);
    }

    private static List<BookingStatus> getBookingStatuses() {
        return Arrays.asList(
            BOOKED,
            PENDING,
            BOTH
        );
    }

    private static Map<PaymentType, Set<String>> buildSupportedPaymentTypeAndProductMatrix() {
        Map<PaymentType, Set<String>> matrix = new HashMap<>();
        Set<String> availablePaymentProducts = Collections.singleton( "sepa-credit-transfers");
        matrix.put(PaymentType.SINGLE, availablePaymentProducts);
        return matrix;
    }
}

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

package de.adorsys.psd2.aspsp.profile.domain;

import de.adorsys.psd2.xs2a.core.ais.BookingStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
public class AspspSettings {
    private int frequencyPerDay;
    private boolean combinedServiceIndicator;
    private boolean tppSignatureRequired;
    private String pisRedirectUrlToAspsp;
    private String aisRedirectUrlToAspsp;
    private MulticurrencyAccountLevel multicurrencyAccountLevel;
    private boolean bankOfferedConsentSupport;
    private List<BookingStatus> availableBookingStatuses;
    private List<SupportedAccountReferenceField> supportedAccountReferenceFields;
    private int consentLifetime;
    private int transactionLifetime;
    private boolean allPsd2Support;
    private boolean transactionsWithoutBalancesSupported;
    private boolean signingBasketSupported;
    private boolean paymentCancellationAuthorizationMandated;
    private boolean piisConsentSupported;
    private long redirectUrlExpirationTimeMs;
    private String pisPaymentCancellationRedirectUrlToAspsp;
    private long notConfirmedConsentExpirationPeriodMs;
    private long notConfirmedPaymentExpirationPeriodMs;
    private Map<PaymentType, Set<String>> supportedPaymentTypeAndProductMatrix;
    private long paymentCancellationRedirectUrlExpirationTimeMs;
    private boolean availableAccountsConsentSupported;
    private boolean scaByOneTimeAvailableAccountsConsentRequired;
    private boolean psuInInitialRequestMandated;
    private boolean forceXs2aBaseUrl;
    private String xs2aBaseUrl;
    private boolean deltaListSupported;
    private boolean entryReferenceFromSupported;
    private List<String> supportedTransactionApplicationTypes;
}

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

package de.adorsys.psd2.aspsp.profile.service;

import de.adorsys.psd2.aspsp.profile.config.BankProfileSetting;
import de.adorsys.psd2.aspsp.profile.config.ProfileConfiguration;
import de.adorsys.psd2.aspsp.profile.domain.AspspSettings;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.profile.StartAuthorisationMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AspspProfileServiceImpl implements AspspProfileService {
    private final ProfileConfiguration profileConfiguration;

    @Override
    public AspspSettings getAspspSettings() {
        BankProfileSetting setting = profileConfiguration.getSetting();
        return new AspspSettings(
            setting.getFrequencyPerDay(),
            setting.isCombinedServiceIndicator(),
            setting.isTppSignatureRequired(),
            setting.getPisRedirectUrlToAspsp(),
            setting.getAisRedirectUrlToAspsp(),
            setting.getMulticurrencyAccountLevel(),
            setting.isBankOfferedConsentSupport(),
            setting.getAvailableBookingStatuses(),
            setting.getSupportedAccountReferenceFields(),
            setting.getConsentLifetime(),
            setting.getTransactionLifetime(),
            setting.isAllPsd2Support(),
            setting.isTransactionsWithoutBalancesSupported(),
            setting.isSigningBasketSupported(),
            setting.isPaymentCancellationAuthorizationMandated(),
            setting.isPiisConsentSupported(),
            setting.getRedirectUrlExpirationTimeMs(),
            setting.getAuthorisationExpirationTimeMs(),
            setting.getPisPaymentCancellationRedirectUrlToAspsp(),
            setting.getNotConfirmedConsentExpirationPeriodMs(),
            setting.getNotConfirmedPaymentExpirationPeriodMs(),
            setting.getSupportedPaymentTypeAndProductMatrix(),
            setting.getPaymentCancellationRedirectUrlExpirationTimeMs(),
            setting.isAvailableAccountsConsentSupported(),
            setting.isScaByOneTimeAvailableAccountsConsentRequired(),
            setting.isPsuInInitialRequestMandated(),
            setting.isForceXs2aBaseUrl(),
            setting.getXs2aBaseUrl(),
            setting.getScaRedirectFlow(),
            setting.isDeltaListSupported(),
            setting.isEntryReferenceFromSupported(),
            setting.getSupportedTransactionApplicationTypes(),
            StartAuthorisationMode.getByValue(setting.getStartAuthorisationMode())
        );
    }

    @Override
    public List<ScaApproach> getScaApproaches() {
        return profileConfiguration.getSetting()
                   .getScaApproaches();
    }
}

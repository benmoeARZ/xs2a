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
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AspspProfileUpdateServiceImpl implements AspspProfileUpdateService {

    private final ProfileConfiguration profileConfiguration;

    /**
     * Update sca approach
     *
     * @param scaApproaches the new value of scaApproach list
     */
    @Override
    public void updateScaApproaches(List<ScaApproach> scaApproaches) {
        profileConfiguration.getSetting()
            .setScaApproaches(scaApproaches);
    }

    /**
     * Update all aspsp settings (frequency per day, combined service indicator, available payment products, available payment types,
     * is tpp signature required, PIS redirect URL, AIS redirect URL, multicurrency account level, is bank offered consent supported,
     * available booking statuses, supported account reference fields, consent lifetime, transaction lifetime, allPsd2 support,
     * transactions without balances support, signing basket support, is payment cancellation authorisation mandated, piis consent support,
     * delta report support, redirect url expiration time, type of authorisation start, etc.) except SCA approach
     *
     * @param aspspSettings new aspsp specific settings which to be stored in profile
     */
    @Override
    public void updateAspspSettings(@NotNull AspspSettings aspspSettings) {
        BankProfileSetting setting = profileConfiguration.getSetting();
        setting.setFrequencyPerDay(aspspSettings.getFrequencyPerDay());
        setting.setCombinedServiceIndicator(aspspSettings.isCombinedServiceIndicator());
        setting.setTppSignatureRequired(aspspSettings.isTppSignatureRequired());
        setting.setPisRedirectUrlToAspsp(aspspSettings.getPisRedirectUrlToAspsp());
        setting.setAisRedirectUrlToAspsp(aspspSettings.getAisRedirectUrlToAspsp());
        setting.setMulticurrencyAccountLevel(aspspSettings.getMulticurrencyAccountLevel());
        setting.setBankOfferedConsentSupport(aspspSettings.isBankOfferedConsentSupport());
        setting.setAvailableBookingStatuses(aspspSettings.getAvailableBookingStatuses());
        setting.setSupportedAccountReferenceFields(aspspSettings.getSupportedAccountReferenceFields());
        setting.setConsentLifetime(aspspSettings.getConsentLifetime());
        setting.setTransactionLifetime(aspspSettings.getTransactionLifetime());
        setting.setAllPsd2Support(aspspSettings.isAllPsd2Support());
        setting.setTransactionsWithoutBalancesSupported(aspspSettings.isTransactionsWithoutBalancesSupported());
        setting.setSigningBasketSupported(aspspSettings.isSigningBasketSupported());
        setting.setPaymentCancellationAuthorizationMandated(aspspSettings.isPaymentCancellationAuthorizationMandated());
        setting.setPiisConsentSupported(aspspSettings.isPiisConsentSupported());
        setting.setDeltaListSupported(aspspSettings.isDeltaListSupported());
        setting.setRedirectUrlExpirationTimeMs(aspspSettings.getRedirectUrlExpirationTimeMs());
        setting.setAuthorisationExpirationTimeMs(aspspSettings.getAuthorisationExpirationTimeMs());
        setting.setPisPaymentCancellationRedirectUrlToAspsp(aspspSettings.getPisPaymentCancellationRedirectUrlToAspsp());
        setting.setNotConfirmedConsentExpirationPeriodMs(aspspSettings.getNotConfirmedConsentExpirationPeriodMs());
        setting.setNotConfirmedPaymentExpirationPeriodMs(aspspSettings.getNotConfirmedPaymentExpirationPeriodMs());
        setting.setSupportedPaymentTypeAndProductMatrix(aspspSettings.getSupportedPaymentTypeAndProductMatrix());
        setting.setPaymentCancellationRedirectUrlExpirationTimeMs(aspspSettings.getPaymentCancellationRedirectUrlExpirationTimeMs());
        setting.setAvailableAccountsConsentSupported(aspspSettings.isAvailableAccountsConsentSupported());
        setting.setScaByOneTimeAvailableAccountsConsentRequired(aspspSettings.isScaByOneTimeAvailableAccountsConsentRequired());
        setting.setPsuInInitialRequestMandated(aspspSettings.isPsuInInitialRequestMandated());
        setting.setForceXs2aBaseUrl(aspspSettings.isForceXs2aBaseUrl());
        setting.setXs2aBaseUrl(aspspSettings.getXs2aBaseUrl());
        setting.setScaRedirectFlow(aspspSettings.getScaRedirectFlow());
        setting.setEntryReferenceFromSupported(aspspSettings.isEntryReferenceFromSupported());
        setting.setSupportedTransactionApplicationTypes(aspspSettings.getSupportedTransactionApplicationTypes());
        setting.setStartAuthorisationMode(aspspSettings.getStartAuthorisationMode() == null ? StartAuthorisationMode.AUTO.getValue() : aspspSettings.getStartAuthorisationMode().getValue());
    }
}

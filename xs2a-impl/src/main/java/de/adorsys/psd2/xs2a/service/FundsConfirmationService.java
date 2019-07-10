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

package de.adorsys.psd2.xs2a.service;

import de.adorsys.psd2.consent.api.service.PiisConsentService;
import de.adorsys.psd2.xs2a.core.event.EventType;
import de.adorsys.psd2.xs2a.core.piis.PiisConsent;
import de.adorsys.psd2.xs2a.core.profile.AccountReference;
import de.adorsys.psd2.xs2a.core.profile.AccountReferenceSelector;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.ResponseObject;
import de.adorsys.psd2.xs2a.domain.fund.FundsConfirmationRequest;
import de.adorsys.psd2.xs2a.domain.fund.FundsConfirmationResponse;
import de.adorsys.psd2.xs2a.domain.fund.PiisConsentValidationResult;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.event.Xs2aEventService;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiToXs2aFundsConfirmationMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiFundsConfirmationRequestMapper;
import de.adorsys.psd2.xs2a.service.profile.AspspProfileServiceWrapper;
import de.adorsys.psd2.xs2a.service.spi.InitialSpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.service.validator.piis.PiisConsentValidation;
import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.fund.SpiFundsConfirmationRequest;
import de.adorsys.psd2.xs2a.spi.domain.fund.SpiFundsConfirmationResponse;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.FundsConfirmationSpi;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.FORMAT_ERROR;
import static de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType.PIIS_400;

@Slf4j
@Service
@AllArgsConstructor
public class FundsConfirmationService {
    private final AspspProfileServiceWrapper profileService;
    private final FundsConfirmationSpi fundsConfirmationSpi;
    private final SpiContextDataProvider spiContextDataProvider;
    private final Xs2aToSpiFundsConfirmationRequestMapper xs2aToSpiFundsConfirmationRequestMapper;
    private final SpiToXs2aFundsConfirmationMapper spiToXs2aFundsConfirmationMapper;
    private final PiisConsentValidation piisConsentValidation;
    private final PiisConsentService piisConsentService;
    private final Xs2aEventService xs2aEventService;
    private final SpiErrorMapper spiErrorMapper;
    private final RequestProviderService requestProviderService;
    private final SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory;

    /**
     * Checks if the account balance is sufficient for requested operation
     *
     * @param request Contains the requested balanceAmount in order to compare with the available balanceAmount in the account
     * @return Response with the result 'true' if there are enough funds in the account, 'false' otherwise
     */
    public ResponseObject<FundsConfirmationResponse> fundsConfirmation(FundsConfirmationRequest request) {
        xs2aEventService.recordTppRequest(EventType.FUNDS_CONFIRMATION_REQUEST_RECEIVED, request);

        PiisConsent consent = null;
        if (profileService.isPiisConsentSupported()) {
            AccountReference accountReference = request.getPsuAccount();
            PiisConsentValidationResult validationResult = validateAccountReference(accountReference);

            if (validationResult.hasError()) {
                ErrorHolder errorHolder = validationResult.getErrorHolder();
                log.info("X-Request-ID: [{}]. Check availability of funds validation failed: {}",
                         requestProviderService.getRequestId(), errorHolder);
                return ResponseObject.<FundsConfirmationResponse>builder()
                           .fail(new MessageError(errorHolder))
                           .build();
            }

            consent = validationResult.getConsent();
        }

        PsuIdData psuIdData = getPsuIdData(consent);

        InitialSpiAspspConsentDataProvider aspspConsentDataProvider = null;

        // We don't transfer provider to the SPI level if there is no PIIS consent. Both PIIS consent and the provider
        // parameters are marked as @Nullable in SPI.
        if (consent != null) {
            aspspConsentDataProvider = aspspConsentDataProviderFactory.getInitialAspspConsentDataProvider();
            aspspConsentDataProvider.saveWith(consent.getId());
        }

        FundsConfirmationResponse response = executeRequest(psuIdData, consent, request, aspspConsentDataProvider);

        if (response.hasError()) {
            ErrorHolder errorHolder = response.getErrorHolder();
            return ResponseObject.<FundsConfirmationResponse>builder()
                       .fail(new MessageError(errorHolder))
                       .build();
        }

        return ResponseObject.<FundsConfirmationResponse>builder()
                   .body(response)
                   .build();
    }

    private PiisConsentValidationResult validateAccountReference(AccountReference accountReference) {
        AccountReferenceSelector selector = accountReference.getUsedAccountReferenceSelector();

        if (selector == null) {
            log.info("X-Request-ID: [{}]. Check availability of funds failed, because while validate account reference no account identifier found in the request [{}].",
                     requestProviderService.getRequestId(), accountReference);
            return new PiisConsentValidationResult(ErrorHolder.builder(FORMAT_ERROR).errorType(PIIS_400).build());
        }

        List<PiisConsent> response = piisConsentService.getPiisConsentListByAccountIdentifier(accountReference.getCurrency(),
                                                                                              selector);

        return piisConsentValidation.validatePiisConsentData(response);
    }

    private FundsConfirmationResponse executeRequest(@NotNull PsuIdData psuIdData,
                                                     @Nullable PiisConsent consent,
                                                     @NotNull FundsConfirmationRequest request,
                                                     @Nullable SpiAspspConsentDataProvider aspspConsentDataProvider) {
        SpiFundsConfirmationRequest spiRequest = xs2aToSpiFundsConfirmationRequestMapper.mapToSpiFundsConfirmationRequest(request);
        SpiContextData spiContextData = spiContextDataProvider.provideWithPsuIdData(psuIdData);

        SpiResponse<SpiFundsConfirmationResponse> fundsSufficientCheck = fundsConfirmationSpi.performFundsSufficientCheck(
            spiContextData,
            consent,
            spiRequest,
            aspspConsentDataProvider
        );

        if (fundsSufficientCheck.hasError()) {
            ErrorHolder error = spiErrorMapper.mapToErrorHolder(fundsSufficientCheck, ServiceType.PIIS);
            log.info("X-Request-ID: [{}]. Check availability of funds failed, because perform funds sufficient check failed. Msg error: {}",
                     requestProviderService.getRequestId(), error);
                return new FundsConfirmationResponse(error);
        }

        return spiToXs2aFundsConfirmationMapper.mapToFundsConfirmationResponse(fundsSufficientCheck.getPayload());
    }

    private @NotNull PsuIdData getPsuIdData(@Nullable PiisConsent consent) {
        // TODO Extract PSU Data from request if it's possible https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/525
        PsuIdData emptyPsuIdData = new PsuIdData(null, null, null, null);
        if (consent == null) {
            return emptyPsuIdData;
        }

        return Optional.ofNullable(consent.getPsuData())
                   .orElse(emptyPsuIdData);
    }
}

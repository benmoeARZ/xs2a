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

package de.adorsys.psd2.xs2a.service.authorization.pis.stage.cancellation;


import de.adorsys.psd2.consent.api.pis.authorisation.GetPisAuthorisationResponse;
import de.adorsys.psd2.consent.api.service.PisCommonPaymentServiceEncrypted;
import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ChallengeData;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.ScaApproachResolver;
import de.adorsys.psd2.xs2a.service.authorization.pis.PisCommonDecoupledService;
import de.adorsys.psd2.xs2a.service.authorization.pis.stage.PisScaStage;
import de.adorsys.psd2.xs2a.service.consent.Xs2aPisCommonPaymentService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.consent.CmsToXs2aPaymentMapper;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.*;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthenticationObject;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorizationCodeResult;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.PaymentCancellationSpi;
import de.adorsys.psd2.xs2a.spi.service.SpiPayment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import static de.adorsys.psd2.xs2a.core.sca.ScaStatus.SCAMETHODSELECTED;

@Slf4j
@Service("PIS_CANCELLATION_EMBEDDED_PSUAUTHENTICATED")
public class PisCancellationScaAuthenticatedStage extends PisScaStage<Xs2aUpdatePisCommonPaymentPsuDataRequest, GetPisAuthorisationResponse, Xs2aUpdatePisCommonPaymentPsuDataResponse> {
    private final PaymentCancellationSpi paymentCancellationSpi;
    private final Xs2aPisCommonPaymentService xs2aPisCommonPaymentService;
    private final PisCommonDecoupledService pisCommonDecoupledService;
    private final SpiContextDataProvider spiContextDataProvider;
    private final ScaApproachResolver scaApproachResolver;
    private final SpiErrorMapper spiErrorMapper;
    private final SpiToXs2aAuthenticationObjectMapper spiToXs2aAuthenticationObjectMapper;
    private final RequestProviderService requestProviderService;
    private final SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory;

    public PisCancellationScaAuthenticatedStage(CmsToXs2aPaymentMapper cmsToXs2aPaymentMapper, Xs2aToSpiPeriodicPaymentMapper xs2aToSpiPeriodicPaymentMapper, Xs2aToSpiSinglePaymentMapper xs2aToSpiSinglePaymentMapper, Xs2aToSpiBulkPaymentMapper xs2aToSpiBulkPaymentMapper, PisCommonPaymentServiceEncrypted pisCommonPaymentServiceEncrypted, ApplicationContext applicationContext, PaymentCancellationSpi paymentCancellationSpi, Xs2aPisCommonPaymentService xs2aPisCommonPaymentService, PisCommonDecoupledService pisCommonDecoupledService, SpiContextDataProvider spiContextDataProvider, ScaApproachResolver scaApproachResolver, SpiErrorMapper spiErrorMapper, SpiToXs2aAuthenticationObjectMapper spiToXs2aAuthenticationObjectMapper, Xs2aToSpiPsuDataMapper xs2aToSpiPsuDataMapper, RequestProviderService requestProviderService, SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory) {
        super(cmsToXs2aPaymentMapper, xs2aToSpiPeriodicPaymentMapper, xs2aToSpiSinglePaymentMapper, xs2aToSpiBulkPaymentMapper, pisCommonPaymentServiceEncrypted, applicationContext, xs2aToSpiPsuDataMapper);
        this.paymentCancellationSpi = paymentCancellationSpi;
        this.xs2aPisCommonPaymentService = xs2aPisCommonPaymentService;
        this.pisCommonDecoupledService = pisCommonDecoupledService;
        this.spiContextDataProvider = spiContextDataProvider;
        this.scaApproachResolver = scaApproachResolver;
        this.spiErrorMapper = spiErrorMapper;
        this.spiToXs2aAuthenticationObjectMapper = spiToXs2aAuthenticationObjectMapper;
        this.requestProviderService = requestProviderService;
        this.aspspConsentDataProviderFactory = aspspConsentDataProviderFactory;
    }

    @Override
    public Xs2aUpdatePisCommonPaymentPsuDataResponse apply(Xs2aUpdatePisCommonPaymentPsuDataRequest request, GetPisAuthorisationResponse pisAuthorisationResponse) {
        PaymentType paymentType = pisAuthorisationResponse.getPaymentType();
        String paymentProduct = pisAuthorisationResponse.getPaymentProduct();
        SpiPayment payment = mapToSpiPayment(pisAuthorisationResponse, paymentType, paymentProduct);

        if (isDecoupledApproach(request.getAuthorisationId(), request.getAuthenticationMethodId())) {
            scaApproachResolver.forceDecoupledScaApproach();
            xs2aPisCommonPaymentService.updateScaApproach(request.getAuthorisationId(), ScaApproach.DECOUPLED);
            return pisCommonDecoupledService.proceedDecoupledCancellation(request, payment, request.getAuthenticationMethodId());
        }

        return proceedEmbeddedApproach(request, payment);
    }

    private Xs2aUpdatePisCommonPaymentPsuDataResponse proceedEmbeddedApproach(Xs2aUpdatePisCommonPaymentPsuDataRequest request, SpiPayment payment) {
        String authenticationMethodId = request.getAuthenticationMethodId();
        String authenticationId = request.getAuthorisationId();
        String paymentId = request.getPaymentId();
        PsuIdData psuData = extractPsuIdData(request, true);
        SpiContextData spiContextData = spiContextDataProvider.provideWithPsuIdData(psuData);

        SpiAspspConsentDataProvider spiAspspConsentDataProvider = aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(request.getPaymentId());

        SpiResponse<SpiAuthorizationCodeResult> spiResponse = paymentCancellationSpi.requestAuthorisationCode(spiContextData, authenticationMethodId, payment, spiAspspConsentDataProvider);

        if (spiResponse.hasError()) {
            ErrorHolder errorHolder = spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.PIS);
            log.warn("InR-ID: [{}], X-Request-ID: [{}], Payment-ID [{}], Authorisation-ID [{}], PSU-ID [{}]. PIS_CANCELLATION_EMBEDDED_PSUAUTHENTICATED stage. Proceed embedded approach when performs authorisation depending on selected SCA method has failed. Error msg: {}.",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), paymentId, authenticationId, psuData.getPsuId(), errorHolder);
            return new Xs2aUpdatePisCommonPaymentPsuDataResponse(errorHolder, paymentId, authenticationId, psuData);
        }

        SpiAuthorizationCodeResult authorizationCodeResult = spiResponse.getPayload();

        if (authorizationCodeResult.isEmpty()) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Payment-ID [{}], Authorisation-ID [{}], PSU-ID [{}]. PIS_CANCELLATION_EMBEDDED_PSUAUTHENTICATED stage. Proceed embedded approach when update payment PSU data has failed. SCA_METHOD_UNKNOWN",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), paymentId, authenticationId, psuData.getPsuId());
            ErrorHolder errorHolder = ErrorHolder.builder(MessageErrorCode.SCA_METHOD_UNKNOWN)
                                          .build();
            return new Xs2aUpdatePisCommonPaymentPsuDataResponse(errorHolder, request.getPaymentId(), request.getAuthorisationId(), psuData);
        }

        SpiAuthenticationObject spiAuthenticationObject = authorizationCodeResult.getSelectedScaMethod();
        ChallengeData challengeData = mapToChallengeData(authorizationCodeResult);

        Xs2aUpdatePisCommonPaymentPsuDataResponse response = new Xs2aUpdatePisCommonPaymentPsuDataResponse(SCAMETHODSELECTED, request.getPaymentId(), request.getAuthorisationId(), psuData);
        response.setChosenScaMethod(spiToXs2aAuthenticationObjectMapper.mapToXs2aAuthenticationObject(spiAuthenticationObject));
        response.setChallengeData(challengeData);
        response.setPsuData(psuData);
        return response;
    }

    private ChallengeData mapToChallengeData(SpiAuthorizationCodeResult authorizationCodeResult) {
        if (authorizationCodeResult != null && !authorizationCodeResult.isEmpty()) {
            return authorizationCodeResult.getChallengeData();
        }
        return null;
    }

    private boolean isDecoupledApproach(String authorisationId, String authenticationMethodId) {
        return xs2aPisCommonPaymentService.isAuthenticationMethodDecoupled(authorisationId, authenticationMethodId);
    }
}

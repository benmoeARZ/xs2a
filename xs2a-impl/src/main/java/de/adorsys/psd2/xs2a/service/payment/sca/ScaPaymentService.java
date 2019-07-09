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

package de.adorsys.psd2.xs2a.service.payment.sca;

import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.pis.*;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.authorization.ScaApproachServiceTypeProvider;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.*;
import de.adorsys.psd2.xs2a.service.spi.InitialSpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.payment.response.SpiBulkPaymentInitiationResponse;
import de.adorsys.psd2.xs2a.spi.domain.payment.response.SpiPaymentInitiationResponse;
import de.adorsys.psd2.xs2a.spi.domain.payment.response.SpiPeriodicPaymentInitiationResponse;
import de.adorsys.psd2.xs2a.spi.domain.payment.response.SpiSinglePaymentInitiationResponse;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.BulkPaymentSpi;
import de.adorsys.psd2.xs2a.spi.service.CommonPaymentSpi;
import de.adorsys.psd2.xs2a.spi.service.PeriodicPaymentSpi;
import de.adorsys.psd2.xs2a.spi.service.SinglePaymentSpi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public abstract class ScaPaymentService implements ScaApproachServiceTypeProvider {
    private final SinglePaymentSpi singlePaymentSpi;
    private final PeriodicPaymentSpi periodicPaymentSpi;
    private final BulkPaymentSpi bulkPaymentSpi;
    private final CommonPaymentSpi commonPaymentSpi;
    private final Xs2aToSpiSinglePaymentMapper xs2AToSpiSinglePaymentMapper;
    private final Xs2aToSpiPeriodicPaymentMapper xs2aToSpiPeriodicPaymentMapper;
    private final Xs2aToSpiBulkPaymentMapper xs2aToSpiBulkPaymentMapper;
    private final Xs2aToSpiPaymentInfo xs2aToSpiPaymentInfo;
    private final SpiToXs2aPaymentMapper spiToXs2aPaymentMapper;
    private final SpiContextDataProvider spiContextDataProvider;
    private final SpiErrorMapper spiErrorMapper;
    private final SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory;
    private final RequestProviderService requestProviderService;

    public SinglePaymentInitiationResponse createSinglePayment(SinglePayment payment, TppInfo tppInfo, String paymentProduct, PsuIdData psuIdData) {
        SpiContextData spiContextData = spiContextDataProvider.provideWithPsuIdData(psuIdData);
        InitialSpiAspspConsentDataProvider aspspConsentDataProvider =
            aspspConsentDataProviderFactory.getInitialAspspConsentDataProvider();

        SpiResponse<SpiSinglePaymentInitiationResponse> spiResponse = singlePaymentSpi.initiatePayment(spiContextData, xs2AToSpiSinglePaymentMapper.mapToSpiSinglePayment(payment, paymentProduct), aspspConsentDataProvider);

        if (spiResponse.hasError()) {
            ErrorHolder errorHolder = spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.PIS);
            log.info("X-Request-ID: [{}], Payment-ID [{}]. CREATE SINGLE Payment failed. Can't initiate Payment at SPI-level. Error msg: {}.",
                     requestProviderService.getRequestId(), payment.getPaymentId(), errorHolder);
            return new SinglePaymentInitiationResponse(errorHolder);
        }

        return spiToXs2aPaymentMapper.mapToPaymentInitiateResponse(spiResponse.getPayload(), aspspConsentDataProvider);
    }

    public PeriodicPaymentInitiationResponse createPeriodicPayment(PeriodicPayment payment, TppInfo tppInfo, String paymentProduct, PsuIdData psuIdData) {
        SpiContextData spiContextData = spiContextDataProvider.provideWithPsuIdData(psuIdData);
        InitialSpiAspspConsentDataProvider aspspConsentDataProvider =
            aspspConsentDataProviderFactory.getInitialAspspConsentDataProvider();

        SpiResponse<SpiPeriodicPaymentInitiationResponse> spiResponse = periodicPaymentSpi.initiatePayment(spiContextData, xs2aToSpiPeriodicPaymentMapper.mapToSpiPeriodicPayment(payment, paymentProduct), aspspConsentDataProvider);

        if (spiResponse.hasError()) {
            ErrorHolder errorHolder = spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.PIS);
            log.info("X-Request-ID: [{}], Payment-ID [{}]. CREATE PERIODIC Payment failed. Can't initiate Payment at SPI-level. Error msg: {}.",
                     requestProviderService.getRequestId(), payment.getPaymentId(), errorHolder);
            return new PeriodicPaymentInitiationResponse(errorHolder);
        }

        return spiToXs2aPaymentMapper.mapToPaymentInitiateResponse(spiResponse.getPayload(), aspspConsentDataProvider);
    }

    public BulkPaymentInitiationResponse createBulkPayment(BulkPayment bulkPayment, TppInfo tppInfo, String paymentProduct, PsuIdData psuIdData) {
        SpiContextData spiContextData = spiContextDataProvider.provideWithPsuIdData(psuIdData);
        InitialSpiAspspConsentDataProvider aspspConsentDataProvider =
            aspspConsentDataProviderFactory.getInitialAspspConsentDataProvider();

        SpiResponse<SpiBulkPaymentInitiationResponse> spiResponse = bulkPaymentSpi.initiatePayment(spiContextData, xs2aToSpiBulkPaymentMapper.mapToSpiBulkPayment(bulkPayment, paymentProduct), aspspConsentDataProvider);

        if (spiResponse.hasError()) {
            ErrorHolder errorHolder = spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.PIS);
            log.info("X-Request-ID: [{}], Payment-ID [{}]. CREATE BULK Payment failed. Can't initiate Payment at SPI-level. Error msg: {}.",
                     requestProviderService.getRequestId(), bulkPayment.getPaymentId(), errorHolder);
            return new BulkPaymentInitiationResponse(errorHolder);
        }

        return spiToXs2aPaymentMapper.mapToPaymentInitiateResponse(spiResponse.getPayload(), aspspConsentDataProvider);
    }

    public CommonPaymentInitiationResponse createCommonPayment(CommonPayment payment, TppInfo tppInfo, String paymentProduct, PsuIdData psuIdData) {
        SpiContextData spiContextData = spiContextDataProvider.provide(psuIdData, tppInfo);
        InitialSpiAspspConsentDataProvider aspspConsentDataProvider =
            aspspConsentDataProviderFactory.getInitialAspspConsentDataProvider();

        SpiResponse<SpiPaymentInitiationResponse> spiResponse = commonPaymentSpi.initiatePayment(spiContextData, xs2aToSpiPaymentInfo.mapToSpiPaymentRequest(payment, paymentProduct), aspspConsentDataProvider);

        if (spiResponse.hasError()) {
            ErrorHolder errorHolder = spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.PIS);
            log.info("X-Request-ID: [{}], Payment-ID [{}]. CREATE COMMON Payment failed. Can't initiate Payment at SPI-level. Error msg: {}.",
                     requestProviderService.getRequestId(), payment.getPaymentId(), errorHolder);
            return new CommonPaymentInitiationResponse(errorHolder);
        }

        return spiToXs2aPaymentMapper.mapToCommonPaymentInitiateResponse(spiResponse.getPayload(), payment.getPaymentType(), aspspConsentDataProvider);
    }
}

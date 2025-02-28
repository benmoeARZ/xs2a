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

package de.adorsys.psd2.xs2a.service.payment;

import de.adorsys.psd2.consent.api.pis.PisPayment;
import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.pis.BulkPayment;
import de.adorsys.psd2.xs2a.domain.pis.PaymentInformationResponse;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiToXs2aBulkPaymentMapper;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiBulkPayment;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.BulkPaymentSpi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service("bulk-payments")
@RequiredArgsConstructor
public class ReadBulkPaymentService extends ReadPaymentService<PaymentInformationResponse<BulkPayment>> {
    private final SpiContextDataProvider spiContextDataProvider;
    private final Xs2aUpdatePaymentAfterSpiService updatePaymentStatusAfterSpiService;
    private final BulkPaymentSpi bulkPaymentSpi;
    private final SpiToXs2aBulkPaymentMapper spiToXs2aBulkPaymentMapper;
    private final SpiErrorMapper spiErrorMapper;
    private final SpiPaymentFactory spiPaymentFactory;
    private final RequestProviderService requestProviderService;
    private final SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory;

    @Override
    public PaymentInformationResponse<BulkPayment> getPayment(List<PisPayment> pisPayments, String paymentProduct, PsuIdData psuData, @NotNull String encryptedPaymentId) {
        Optional<SpiBulkPayment> spiPaymentOptional = spiPaymentFactory.createSpiBulkPayment(pisPayments, paymentProduct);

        if (!spiPaymentOptional.isPresent()) {
            return new PaymentInformationResponse<>(
                ErrorHolder.builder(MessageErrorCode.RESOURCE_UNKNOWN_404)
                    .messages(Collections.singletonList("Payment not found"))
                    .build()
            );
        }

        SpiAspspConsentDataProvider aspspConsentDataProvider =
            aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(encryptedPaymentId);

        SpiResponse<SpiBulkPayment> spiResponse = bulkPaymentSpi.getPaymentById(spiContextDataProvider.provideWithPsuIdData(psuData), spiPaymentOptional.get(), aspspConsentDataProvider);

        if (spiResponse.hasError()) {
            ErrorHolder errorHolder = spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.PIS);
            log.info("InR-ID: [{}], X-Request-ID: [{}], Payment-ID [{}]. READ BULK Payment failed. Can't get Payment by id at SPI-level. Error msg: [{}]",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), spiPaymentOptional.get().getPaymentId(), errorHolder);
            return new PaymentInformationResponse<>(errorHolder);
        }

        SpiBulkPayment spiResponsePayment = spiResponse.getPayload();
        BulkPayment xs2aBulkPayment = spiToXs2aBulkPaymentMapper.mapToXs2aBulkPayment(spiResponsePayment);

        TransactionStatus paymentStatus = xs2aBulkPayment.getTransactionStatus();
        if (!updatePaymentStatusAfterSpiService.updatePaymentStatus(encryptedPaymentId, paymentStatus)) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Internal payment ID: [{}], Transaction status: [{}]. Update of a payment status in the CMS has failed.",
                     requestProviderService.getInternalRequestId(), requestProviderService.getRequestId(), xs2aBulkPayment.getPaymentId(), paymentStatus);
        }

        return new PaymentInformationResponse<>(xs2aBulkPayment);
    }
}

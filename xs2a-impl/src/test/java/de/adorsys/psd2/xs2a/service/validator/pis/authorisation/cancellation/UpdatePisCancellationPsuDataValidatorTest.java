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

package de.adorsys.psd2.xs2a.service.validator.pis.authorisation.cancellation;

import de.adorsys.psd2.consent.api.pis.proto.PisCommonPaymentResponse;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.domain.pis.PaymentAuthorisationType;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.validator.PisEndpointAccessCheckerService;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import de.adorsys.psd2.xs2a.service.validator.pis.PaymentTypeAndProductValidator;
import de.adorsys.psd2.xs2a.service.validator.tpp.PisTppInfoValidator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.*;
import java.util.UUID;


import static de.adorsys.psd2.xs2a.domain.TppMessageInformation.of;
import static de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType.PIS_403;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UpdatePisCancellationPsuDataValidatorTest {
    private static final TppInfo TPP_INFO = buildTppInfo("authorisation number");
    private static final TppInfo INVALID_TPP_INFO = buildTppInfo("invalid authorisation number");
    private static final String AUTHORISATION_ID = "authorisation id";
    private static final String INVALID_AUTHORISATION_ID = "invalid authorisation id";

    private static final MessageError TPP_VALIDATION_ERROR =
        new MessageError(ErrorType.PIS_401, TppMessageInformation.of(UNAUTHORIZED, "Invalid TPP"));
    private static final MessageError BLOCKED_ENDPOINT_ERROR = new MessageError(PIS_403, of(SERVICE_BLOCKED));

    private static final MessageError PAYMENT_PRODUCT_VALIDATION_ERROR =
        new MessageError(ErrorType.PIS_404, TppMessageInformation.of(PRODUCT_UNKNOWN));

    private static final String CORRECT_PAYMENT_PRODUCT = "sepa-credit-transfers";
    private static final String WRONG_PAYMENT_PRODUCT = "sepa-credit-transfers111";

    @Mock
    private PisTppInfoValidator pisTppInfoValidator;
    @Mock
    private PisEndpointAccessCheckerService pisEndpointAccessCheckerService;
    @Mock
    PaymentTypeAndProductValidator paymentProductAndTypeValidator;
    @Mock
    private RequestProviderService requestProviderService;

    @InjectMocks
    private UpdatePisCancellationPsuDataValidator updatePisCancellationPsuDataValidator;

    @Before
    public void setUp() {
        // Inject pisTppInfoValidator via setter
        updatePisCancellationPsuDataValidator.setPisValidators(pisTppInfoValidator, paymentProductAndTypeValidator);

        when(pisTppInfoValidator.validateTpp(TPP_INFO))
            .thenReturn(ValidationResult.valid());
        when(pisTppInfoValidator.validateTpp(INVALID_TPP_INFO))
            .thenReturn(ValidationResult.invalid(TPP_VALIDATION_ERROR));

        when(pisEndpointAccessCheckerService.isEndpointAccessible(AUTHORISATION_ID, PaymentAuthorisationType.CANCELLATION))
            .thenReturn(true);
        when(pisEndpointAccessCheckerService.isEndpointAccessible(INVALID_AUTHORISATION_ID, PaymentAuthorisationType.CANCELLATION))
            .thenReturn(false);
        when(paymentProductAndTypeValidator.validateTypeAndProduct(PaymentType.SINGLE, CORRECT_PAYMENT_PRODUCT))
            .thenReturn(ValidationResult.valid());
        when(paymentProductAndTypeValidator.validateTypeAndProduct(PaymentType.SINGLE, WRONG_PAYMENT_PRODUCT))
            .thenReturn(ValidationResult.invalid(PAYMENT_PRODUCT_VALIDATION_ERROR));
        when(requestProviderService.getRequestId()).thenReturn(UUID.randomUUID());
    }

    @Test
    public void validate_withValidPaymentObject_shouldReturnValid() {
        // Given
        PisCommonPaymentResponse commonPaymentResponse = buildPisCommonPaymentResponse(TPP_INFO);

        // When
        ValidationResult validationResult = updatePisCancellationPsuDataValidator.validate(new UpdatePisCancellationPsuDataPO(commonPaymentResponse, AUTHORISATION_ID));

        // Then
        verify(pisTppInfoValidator).validateTpp(commonPaymentResponse.getTppInfo());

        assertNotNull(validationResult);
        assertTrue(validationResult.isValid());
        assertNull(validationResult.getMessageError());
    }

    @Test
    public void validate_withInvalidPaymentProduct_shouldReturnPaymentProductValidationError() {
        // Given
        PisCommonPaymentResponse commonPaymentResponse = buildPisCommonPaymentResponse(TPP_INFO);
        commonPaymentResponse.setPaymentProduct(WRONG_PAYMENT_PRODUCT);
        // When
        ValidationResult validationResult = updatePisCancellationPsuDataValidator.validate(new UpdatePisCancellationPsuDataPO(commonPaymentResponse, AUTHORISATION_ID));

        // Then
        assertNotNull(validationResult);
        assertTrue(validationResult.isNotValid());
        assertEquals(PAYMENT_PRODUCT_VALIDATION_ERROR, validationResult.getMessageError());
    }

    @Test
    public void validate_withInvalidTppInPayment_shouldReturnTppValidationError() {
        // Given
        PisCommonPaymentResponse commonPaymentResponse = buildPisCommonPaymentResponse(INVALID_TPP_INFO);

        // When
        ValidationResult validationResult = updatePisCancellationPsuDataValidator.validate(new UpdatePisCancellationPsuDataPO(commonPaymentResponse, AUTHORISATION_ID));

        // Then
        verify(pisTppInfoValidator).validateTpp(commonPaymentResponse.getTppInfo());

        assertNotNull(validationResult);
        assertTrue(validationResult.isNotValid());
        assertEquals(TPP_VALIDATION_ERROR, validationResult.getMessageError());
    }

    @Test
    public void validate_withInvalidAuthorisation_shouldReturnValidationError() {
        // Given
        PisCommonPaymentResponse commonPaymentResponse = buildPisCommonPaymentResponse(TPP_INFO);

        // When
        ValidationResult validationResult = updatePisCancellationPsuDataValidator.validate(new UpdatePisCancellationPsuDataPO(commonPaymentResponse, INVALID_AUTHORISATION_ID));

        // Then
        verify(pisTppInfoValidator).validateTpp(commonPaymentResponse.getTppInfo());

        assertNotNull(validationResult);
        assertTrue(validationResult.isNotValid());
        assertEquals(BLOCKED_ENDPOINT_ERROR, validationResult.getMessageError());
    }

    @Test
    public void validate_withInvalidTppAndInvalidAuthorisation_shouldReturnTppValidationErrorFirst() {
        // Given
        PisCommonPaymentResponse commonPaymentResponse = buildPisCommonPaymentResponse(INVALID_TPP_INFO);

        // When
        ValidationResult validationResult = updatePisCancellationPsuDataValidator.validate(new UpdatePisCancellationPsuDataPO(commonPaymentResponse, INVALID_AUTHORISATION_ID));

        // Then
        verify(pisTppInfoValidator).validateTpp(commonPaymentResponse.getTppInfo());

        assertNotNull(validationResult);
        assertTrue(validationResult.isNotValid());
        assertEquals(TPP_VALIDATION_ERROR, validationResult.getMessageError());
    }

    private static TppInfo buildTppInfo(String authorisationNumber) {
        TppInfo tppInfo = new TppInfo();
        tppInfo.setAuthorisationNumber(authorisationNumber);
        return tppInfo;
    }

    private PisCommonPaymentResponse buildPisCommonPaymentResponse(TppInfo tppInfo) {
        PisCommonPaymentResponse pisCommonPaymentResponse = new PisCommonPaymentResponse();
        pisCommonPaymentResponse.setTppInfo(tppInfo);
        pisCommonPaymentResponse.setPaymentProduct(CORRECT_PAYMENT_PRODUCT);
        pisCommonPaymentResponse.setPaymentType(PaymentType.SINGLE);
        return pisCommonPaymentResponse;
    }
}

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

package de.adorsys.psd2.xs2a.service.validator.ais.account;

import de.adorsys.psd2.xs2a.core.profile.AccountReference;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.domain.consent.AccountConsent;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aAccountAccess;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import de.adorsys.psd2.xs2a.service.validator.ais.account.common.AccountAccessValidator;
import de.adorsys.psd2.xs2a.service.validator.ais.account.common.AccountConsentValidator;
import de.adorsys.psd2.xs2a.service.validator.ais.account.common.AccountReferenceAccessValidator;
import de.adorsys.psd2.xs2a.service.validator.ais.account.common.PermittedAccountReferenceValidator;
import de.adorsys.psd2.xs2a.service.validator.ais.account.dto.CommonAccountRequestObject;
import de.adorsys.psd2.xs2a.service.validator.tpp.AisTppInfoValidator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.CONSENT_INVALID;
import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.UNAUTHORIZED;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GetAccountDetailsValidatorTest {
    private static final TppInfo TPP_INFO = buildTppInfo("authorisation number");
    private static final TppInfo INVALID_TPP_INFO = buildTppInfo("invalid authorisation number");
    private static final String ACCOUNT_ID = "account id";
    private static final String REQUEST_URI = "/accounts";
    private static final boolean WITH_BALANCE = false;

    private static final MessageError TPP_VALIDATION_ERROR =
        new MessageError(ErrorType.AIS_401, TppMessageInformation.of(UNAUTHORIZED));

    private static final MessageError PERMITTED_ACCOUNT_REFERENCE_VALIDATION_ERROR =
        new MessageError(ErrorType.AIS_401, TppMessageInformation.of(CONSENT_INVALID));

    @InjectMocks
    private GetAccountDetailsValidator getAccountDetailsValidator;

    @Mock
    private AccountConsentValidator accountConsentValidator;
    @Mock
    private AisTppInfoValidator aisTppInfoValidator;
    @Mock
    private AccountReferenceAccessValidator accountReferenceAccessValidator;
    @Mock
    private PermittedAccountReferenceValidator permittedAccountReferenceValidator;
    @Mock
    private AccountAccessValidator accountAccessValidator;
    @Mock
    private AccountReference accountReference;

    @Before
    public void setUp() {
        // Inject pisTppInfoValidator via setter
        getAccountDetailsValidator.setPisTppInfoValidator(aisTppInfoValidator);

        when(aisTppInfoValidator.validateTpp(TPP_INFO))
            .thenReturn(ValidationResult.valid());
        when(aisTppInfoValidator.validateTpp(INVALID_TPP_INFO))
            .thenReturn(ValidationResult.invalid(TPP_VALIDATION_ERROR));
    }

    @Test
    public void validate_withInvalidAccountReference_shouldReturnInvalid() {
        // Given
        AccountConsent accountConsent = buildAccountConsent(TPP_INFO);
        when(accountReferenceAccessValidator.validate(accountConsent.getAccess(), accountConsent.getAccess().getAccounts(), ACCOUNT_ID))
            .thenReturn(ValidationResult.valid());
        when(permittedAccountReferenceValidator.validate(accountConsent, ACCOUNT_ID, WITH_BALANCE))
            .thenReturn(ValidationResult.invalid(PERMITTED_ACCOUNT_REFERENCE_VALIDATION_ERROR));

        // When
        ValidationResult validationResult = getAccountDetailsValidator.validate(new CommonAccountRequestObject(accountConsent, ACCOUNT_ID, WITH_BALANCE, REQUEST_URI));

        // Then
        verify(permittedAccountReferenceValidator).validate(accountConsent, ACCOUNT_ID, WITH_BALANCE);

        assertNotNull(validationResult);
        assertTrue(validationResult.isNotValid());
        assertEquals(PERMITTED_ACCOUNT_REFERENCE_VALIDATION_ERROR, validationResult.getMessageError());
    }

    @Test
    public void validate_withValidConsentObject_shouldReturnValid() {
        // Given
        AccountConsent accountConsent = buildAccountConsent(TPP_INFO);
        when(accountReferenceAccessValidator.validate(accountConsent.getAccess(), accountConsent.getAccess().getAccounts(), ACCOUNT_ID))
            .thenReturn(ValidationResult.valid());
        when(permittedAccountReferenceValidator.validate(accountConsent, ACCOUNT_ID, WITH_BALANCE))
            .thenReturn(ValidationResult.valid());
        when(accountAccessValidator.validate(accountConsent, accountConsent.isWithBalance()))
            .thenReturn(ValidationResult.valid());
        when(accountConsentValidator.validate(accountConsent, REQUEST_URI))
            .thenReturn(ValidationResult.valid());

        // When
        ValidationResult validationResult = getAccountDetailsValidator.validate(new CommonAccountRequestObject(accountConsent, ACCOUNT_ID, WITH_BALANCE, REQUEST_URI));

        // Then
        verify(aisTppInfoValidator).validateTpp(accountConsent.getTppInfo());

        assertNotNull(validationResult);
        assertTrue(validationResult.isValid());
        assertNull(validationResult.getMessageError());
    }

    @Test
    public void validate_withInvalidAccountReferenceAccess_error() {
        // Given
        AccountConsent accountConsent = buildAccountConsent(TPP_INFO);
        when(accountReferenceAccessValidator.validate(accountConsent.getAccess(), accountConsent.getAccess().getAccounts(), ACCOUNT_ID))
            .thenReturn(ValidationResult.invalid(ErrorType.AIS_401, TppMessageInformation.of(CONSENT_INVALID)));

        // When
        ValidationResult validationResult = getAccountDetailsValidator.validate(new CommonAccountRequestObject(accountConsent, ACCOUNT_ID, WITH_BALANCE, REQUEST_URI));

        // Then
        verify(aisTppInfoValidator).validateTpp(accountConsent.getTppInfo());

        assertNotNull(validationResult);
        assertFalse(validationResult.isValid());

        verify(permittedAccountReferenceValidator, never()).validate(any(AccountConsent.class), anyString(), anyBoolean());
        verify(accountAccessValidator, never()).validate(any(AccountConsent.class), anyBoolean());
        verify(accountConsentValidator, never()).validate(any(AccountConsent.class), anyString());
    }

    @Test
    public void validate_withInvalidTppInConsent_shouldReturnTppValidationError() {
        // Given
        AccountConsent accountConsent = buildAccountConsent(INVALID_TPP_INFO);

        // When
        ValidationResult validationResult = getAccountDetailsValidator.validate(new CommonAccountRequestObject(accountConsent, ACCOUNT_ID, WITH_BALANCE, REQUEST_URI));

        // Then
        verify(aisTppInfoValidator).validateTpp(accountConsent.getTppInfo());

        assertNotNull(validationResult);
        assertTrue(validationResult.isNotValid());
        assertEquals(TPP_VALIDATION_ERROR, validationResult.getMessageError());
    }

    private static TppInfo buildTppInfo(String authorisationNumber) {
        TppInfo tppInfo = new TppInfo();
        tppInfo.setAuthorisationNumber(authorisationNumber);
        return tppInfo;
    }

    private AccountConsent buildAccountConsent(TppInfo tppInfo) {
        return new AccountConsent("id", buildXs2aAccountAccess(), false, null, 0,
                                  null, null, false, false,
                                  Collections.emptyList(), tppInfo, null, false,
                                  Collections.emptyList(), null, Collections.emptyMap());
    }

    private Xs2aAccountAccess buildXs2aAccountAccess() {
        return new Xs2aAccountAccess(Collections.singletonList(accountReference), Collections.emptyList(), Collections.emptyList(), null, null, null);
    }
}

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

package de.adorsys.psd2.xs2a.service.validator.ais.account.common;

import de.adorsys.psd2.xs2a.core.ais.AccountAccessType;
import de.adorsys.psd2.xs2a.core.profile.AccountReference;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aAccountAccess;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.CONSENT_INVALID;

@Component
@RequiredArgsConstructor
public class AccountReferenceAccessValidator {

    public ValidationResult validate(Xs2aAccountAccess accountAccess, List<AccountReference> references, String accountId) {
        if (accountAccess.getAllPsd2() != null) {
            return ValidationResult.valid();
        }

        if (isConsentForAllAvailableAccounts(accountAccess) || !isValidAccountByAccess(accountId, references)) {
            return ValidationResult.invalid(ErrorType.AIS_401, TppMessageInformation.of(CONSENT_INVALID));
        }

        return ValidationResult.valid();
    }

    private boolean isValidAccountByAccess(String accountId, List<AccountReference> allowedAccountData) {
        return CollectionUtils.isNotEmpty(allowedAccountData)
                   && allowedAccountData.stream()
                          .anyMatch(a -> a.getResourceId().equals(accountId));
    }

    private boolean isConsentForAllAvailableAccounts(Xs2aAccountAccess accountAccess) {
        return Arrays.asList(accountAccess.getAvailableAccounts(), accountAccess.getAvailableAccountsWithBalances())
                   .contains(AccountAccessType.ALL_ACCOUNTS);
    }
}

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

package de.adorsys.psd2.consent.api.ais;

import de.adorsys.psd2.xs2a.core.profile.AccountReference;
import lombok.Value;

import java.util.List;

@Value
public class AisAccountAccess {
    private List<AccountReference> accounts;
    private List<AccountReference> balances;
    private List<AccountReference> transactions;

    // TODO: connected with the ticket for removing duplicated enums: https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/671
    private String availableAccounts;
    private String allPsd2;
    private String availableAccountsWithBalances;

}

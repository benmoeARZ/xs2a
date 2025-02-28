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

package de.adorsys.psd2.xs2a.spi.domain.consent;

import de.adorsys.psd2.xs2a.core.ais.AccountAccessType;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiAccountReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpiAccountAccess {
    private List<SpiAccountReference> accounts;
    private List<SpiAccountReference> balances;
    private List<SpiAccountReference> transactions;
    private AccountAccessType availableAccounts;
    private AccountAccessType allPsd2;
    private AccountAccessType availableAccountsWithBalances;

    public boolean isEmpty() {
        return CollectionUtils.isEmpty(this.accounts)
                   && CollectionUtils.isEmpty(this.balances)
                   && CollectionUtils.isEmpty(this.transactions)
                   && this.allPsd2 == null
                   && this.availableAccounts == null
                   && this.availableAccountsWithBalances == null;
    }
}

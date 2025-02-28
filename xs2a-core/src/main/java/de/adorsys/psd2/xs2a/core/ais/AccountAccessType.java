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

package de.adorsys.psd2.xs2a.core.ais;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum AccountAccessType {
    ALL_ACCOUNTS("allAccounts"),
    @Deprecated //since 2.8 TODO remove deprecated enum in 2.10 https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/887
    ALL_ACCOUNTS_WITH_BALANCES("allAccountsWithBalances");

    private static Map<String, AccountAccessType> container = new HashMap<>();

    static {
        Arrays.stream(values())
            .forEach(aat -> container.put(aat.getDescription(), aat));
    }

    private String description;

    AccountAccessType(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static Optional<AccountAccessType> getByDescription(String description) {
        return Optional.ofNullable(container.get(description));
    }
}

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

package de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers;

import de.adorsys.psd2.xs2a.domain.consent.Xs2aAccountAccess;
import de.adorsys.psd2.xs2a.spi.domain.consent.SpiAccountAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SpiToXs2aAccountAccessMapper {
    private final SpiToXs2aAccountReferenceMapper spiToXs2aAccountReferenceMapper;

    public Optional<Xs2aAccountAccess> mapToAccountAccess(SpiAccountAccess access) {
        return Optional.ofNullable(access)
                   .map(aa ->
                            new Xs2aAccountAccess(
                                spiToXs2aAccountReferenceMapper.mapToXs2aAccountReferences(aa.getAccounts()),
                                spiToXs2aAccountReferenceMapper.mapToXs2aAccountReferences(aa.getBalances()),
                                spiToXs2aAccountReferenceMapper.mapToXs2aAccountReferences(aa.getTransactions()),
                                aa.getAvailableAccounts(),
                                aa.getAllPsd2(),
                                aa.getAvailableAccountsWithBalances()));
    }
}

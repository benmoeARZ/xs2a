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

package de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers;

import de.adorsys.psd2.xs2a.domain.address.Xs2aAddress;
import de.adorsys.psd2.xs2a.domain.address.Xs2aCountryCode;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;

import javax.validation.constraints.NotNull;

@Mapper(componentModel = "spring", imports = {Xs2aCountryCode.class},
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface SpiToXs2aAddressMapper {

    @Mapping(target = "country", source = "address", qualifiedByName = "mapToXs2aCountryCode")
    Xs2aAddress mapToAddress(@NotNull SpiAddress address);

    default Xs2aCountryCode mapToXs2aCountryCode(SpiAddress address) {
        return new Xs2aCountryCode(address.getCountry());
    }
}

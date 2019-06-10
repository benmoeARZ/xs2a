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

import de.adorsys.psd2.xs2a.domain.Xs2aExchangeRate;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiExchangeRate;
import de.adorsys.psd2.xs2a.util.reader.JsonReader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {SpiToXs2aExchangeRateMapperImpl.class})
public class SpiToXs2aExchangeRateMapperTest {

    @Autowired
    private SpiToXs2aExchangeRateMapper mapper;

    private JsonReader jsonReader = new JsonReader();
    private SpiExchangeRate spiExchangeRate;
    private Xs2aExchangeRate expectedXs2aExchangeRate;

    @Before
    public void setUp() {
        spiExchangeRate = jsonReader.getObjectFromFile("json/service/mapper/spi_xs2a_mappers/spi-exchange-rate.json",
                                                       SpiExchangeRate.class);
        expectedXs2aExchangeRate = jsonReader.getObjectFromFile("json/service/mapper/spi_xs2a_mappers/xs2a-exchange-rate.json",
                                                                Xs2aExchangeRate.class);
    }

    @Test
    public void mapToExchangeRate() {
        Xs2aExchangeRate xs2aExchangeRate = mapper.mapToExchangeRate(spiExchangeRate);
        assertEquals(expectedXs2aExchangeRate, xs2aExchangeRate);
    }

    @Test
    public void mapToExchangeRate_nullValue() {
        assertNull(mapper.mapToExchangeRate(null));
    }

    @Test
    public void mapToExchangeRateList() {
        List<Xs2aExchangeRate> xs2aExchangeRateList = mapper.mapToExchangeRateList(Collections.singletonList(spiExchangeRate));

        assertEquals(1, xs2aExchangeRateList.size());
        assertEquals(expectedXs2aExchangeRate, xs2aExchangeRateList.get(0));
    }
}

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

import de.adorsys.psd2.xs2a.domain.account.Xs2aBalancesReport;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiAccountBalance;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiAccountReference;
import de.adorsys.psd2.xs2a.util.reader.JsonReader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {SpiToXs2aBalanceReportMapperImpl.class, SpiToXs2aBalanceMapperImpl.class,
    SpiToXs2aAccountReferenceMapperImpl.class, SpiToXs2aAmountMapperImpl.class})
public class SpiToXs2aBalanceReportMapperTest {

    @Autowired
    private SpiToXs2aBalanceReportMapper mapper;

    private JsonReader jsonReader = new JsonReader();

    @Test
    public void mapToXs2aBalancesReport() {
        SpiAccountBalance spiAccountBalance = jsonReader.getObjectFromFile("json/service/mapper/spi_xs2a_mappers/spi-account-balance.json",
                                                                           SpiAccountBalance.class);
        SpiAccountReference spiAccountReference = jsonReader.getObjectFromFile("json/service/mapper/spi_xs2a_mappers/spi-account-reference.json",
                                                                               SpiAccountReference.class);

        Xs2aBalancesReport xs2aBalancesReport = mapper.mapToXs2aBalancesReport(spiAccountReference, Collections.singletonList(spiAccountBalance));

        Xs2aBalancesReport expectedXs2aBalancesReport = jsonReader.getObjectFromFile("json/service/mapper/spi_xs2a_mappers/xs2a-balances-report.json",
                                                                                     Xs2aBalancesReport.class);
        assertEquals(expectedXs2aBalancesReport, xs2aBalancesReport);
    }

    @Test
    public void mapToXs2aBalancesReport_nullValue() {
        Xs2aBalancesReport xs2aBalancesReport = mapper.mapToXs2aBalancesReport(null, null);
        assertNull(xs2aBalancesReport);
    }
}

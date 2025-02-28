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

import de.adorsys.psd2.consent.api.pis.proto.PisPaymentInfo;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiPaymentInfo;
import de.adorsys.psd2.xs2a.spi.domain.psu.SpiPsuData;
import de.adorsys.psd2.xs2a.util.reader.JsonReader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.OffsetDateTime;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {SpiToXs2aPaymentInfoMapperImpl.class})
public class SpiToXs2aPaymentInfoMapperTest {
    private static final String PAYMENT_ID = "2Cixxv85Or_qoBBh_d7VTZC0M8PwzR5IGzsJuT-jYHNOMR1D7n69vIF46RgFd7Zn_=_bS6p6XvTWI";
    private static final String PAYMENT_PRODUCT = "sepa-credit-transfers";
    private static final OffsetDateTime OFFSET_DATE_TIME = OffsetDateTime.now();
    private static final byte[] PAYMENT_DATA = "data".getBytes();

    @Autowired
    private SpiToXs2aPaymentInfoMapper mapper;

    private JsonReader jsonReader = new JsonReader();

    @Test
    public void mapToXs2aPaymentInfo() {
        SpiPaymentInfo spiPaymentInfo = new SpiPaymentInfo(PAYMENT_PRODUCT);
        spiPaymentInfo.setPaymentId(PAYMENT_ID);
        spiPaymentInfo.setPaymentType(PaymentType.SINGLE);
        spiPaymentInfo.setStatus(TransactionStatus.ACSP);
        spiPaymentInfo.setStatusChangeTimestamp(OFFSET_DATE_TIME);
        spiPaymentInfo.setPsuDataList(Collections.singletonList(new SpiPsuData("psuId", "", "", "")));
        spiPaymentInfo.setPaymentData(PAYMENT_DATA);

        PisPaymentInfo pisPaymentInfo = mapper.mapToXs2aPaymentInfo(spiPaymentInfo);

        PisPaymentInfo expectedPisPaymentInfo = jsonReader.getObjectFromFile("json/service/mapper/spi_xs2a_mappers/xs2a-payment-info.json", PisPaymentInfo.class);
        expectedPisPaymentInfo.setStatusChangeTimestamp(OFFSET_DATE_TIME);
        assertEquals(expectedPisPaymentInfo, pisPaymentInfo);
    }

    @Test
    public void mapToXs2aPaymentInfo_nullValue() {
        PisPaymentInfo pisPaymentInfo = mapper.mapToXs2aPaymentInfo(null);
        assertNull(pisPaymentInfo);
    }
}

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

package de.adorsys.psd2.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class DownloadTransactionsListResponse200JsonTest {
    private final Class clazz = DownloadTransactionsListResponse200Json.class;

    @Test
    public void checkFieldForExistence() {
        try {
            CommonModelTest.сheckOutputStreamField(clazz);
        } catch (NoSuchFieldException e) {
            fail();
        }
    }

}

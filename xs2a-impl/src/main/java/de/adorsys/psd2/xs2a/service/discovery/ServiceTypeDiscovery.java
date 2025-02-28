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

package de.adorsys.psd2.xs2a.service.discovery;

import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static de.adorsys.psd2.xs2a.config.Xs2aEndpointPathConstant.*;
import static de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType.*;

@Slf4j
class ServiceTypeDiscovery {
    private static final AntPathMatcher matcher = new AntPathMatcher();
    private static final Map<String, ServiceType> pathToServiceType;

    static {
        pathToServiceType = new HashMap<>();
        pathToServiceType.put(ACCOUNTS_PATH, AIS);
        pathToServiceType.put(CONSENTS_PATH, AIS);
        pathToServiceType.put(SINGLE_PAYMENTS_PATH, PIS);
        pathToServiceType.put(PERIODIC_PAYMENTS_PATH, PIS);
        pathToServiceType.put(BULK_PAYMENTS_PATH, PIS);
        pathToServiceType.put(FUNDS_CONFIRMATION_PATH, PIIS);
        pathToServiceType.put(SIGNING_BASKETS_PATH, SB);
    }

    /**
     * Returns service type by checking incoming path on existing paths patterns matching (each pattern is associated with corresponding service type).
     *
     * @param targetPath        target path to be checked on pattern matching
     * @param internalRequestId internal id of the request
     * @param requestId         request id provided by the TPP
     * @return Service Type value
     */
    static ServiceType getServiceType(String targetPath, UUID internalRequestId, UUID requestId) {
        for (Map.Entry<String, ServiceType> entry : pathToServiceType.entrySet()) {
            String pattern = entry.getKey();

            if (matcher.match(pattern, targetPath)) {
                return entry.getValue();
            }
        }

        log.warn("InR-ID: [{}], X-Request-ID: [{}]. Can't get ServiceType because illegal path: [{}]",
                 internalRequestId, requestId, targetPath);
        throw new IllegalArgumentException("Illegal path: " + targetPath);
    }
}

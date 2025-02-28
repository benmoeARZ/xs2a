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

package de.adorsys.psd2.xs2a.service.mapper.psd2;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum ErrorType {
    PIS_400(ServiceType.PIS, 400),
    PIS_401(ServiceType.PIS, 401),
    PIS_403(ServiceType.PIS, 403),
    PIS_404(ServiceType.PIS, 404),
    PIS_405(ServiceType.PIS, 405),
    PIS_409(ServiceType.PIS, 409),
    PIS_415(ServiceType.PIS, 415),
    PIS_500(ServiceType.PIS, 500),
    PIS_CANC_405(ServiceType.PIS, 405),

    PIIS_400(ServiceType.PIIS, 400),
    PIIS_401(ServiceType.PIIS, 401),
    PIIS_403(ServiceType.PIIS, 403),
    PIIS_404(ServiceType.PIIS, 404),
    PIIS_405(ServiceType.PIIS, 405),
    PIIS_409(ServiceType.PIIS, 409),
    PIIS_415(ServiceType.PIIS, 415),
    PIIS_429(ServiceType.PIIS, 429),
    PIIS_500(ServiceType.PIIS, 500),

    AIS_400(ServiceType.AIS, 400),
    AIS_401(ServiceType.AIS, 401),
    AIS_403(ServiceType.AIS, 403),
    AIS_404(ServiceType.AIS, 404),
    AIS_405(ServiceType.AIS, 405),
    AIS_406(ServiceType.AIS, 406),
    AIS_415(ServiceType.AIS, 415),
    AIS_429(ServiceType.AIS, 429),
    AIS_500(ServiceType.AIS, 500),

    SB_400(ServiceType.SB, 400),
    SB_401(ServiceType.SB, 401),
    SB_403(ServiceType.SB, 403),
    SB_404(ServiceType.SB, 404),
    SB_405(ServiceType.SB, 405),
    SB_409(ServiceType.SB, 409),
    SB_415(ServiceType.SB, 415),
    SB_500(ServiceType.SB, 500);

    @Getter
    private ServiceType serviceType;
    @Getter
    private int errorCode;

    ErrorType(ServiceType serviceType, int errorCode) {
        this.serviceType = serviceType;
        this.errorCode = errorCode;
    }

    public static Optional<ErrorType> getByServiceTypeAndErrorCode(ServiceType serviceType, int errorCode) {
        return Arrays.stream(ErrorType.values())
                   .filter(et -> et.getServiceType().equals(serviceType))
                   .filter(et -> et.getErrorCode() == errorCode)
                   .findFirst();
    }
}

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

package de.adorsys.psd2.xs2a.spi.service;

import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthenticationObject;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorisationDecoupledScaResponse;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorisationStatus;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorizationCodeResult;
import de.adorsys.psd2.xs2a.spi.domain.psu.SpiPsuData;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Interface, that contains the method for the authorisation flow.
 * To be used in SPI interfaces, that need authorisation functionality.
 *
 * @param <T> business object to be provided during the implementation
 */
interface AuthorisationSpi<T> {

    /**
     * Authorises psu and returns current authorisation status. Used only with embedded SCA Approach.
     *
     * @param contextData              holder of call's context data (e.g. about PSU and TPP)
     * @param psuLoginData             ASPSP identifier(s) of the psu, provided by TPP within this request.
     * @param password                 Psu's password
     * @param businessObject           generic consent/payment object
     * @param aspspConsentDataProvider Provides access to read/write encrypted data to be stored in the consent management system.
     * @return success or failure authorization status
     */
    SpiResponse<SpiAuthorisationStatus> authorisePsu(@NotNull SpiContextData contextData, @NotNull SpiPsuData psuLoginData, String password, T businessObject, @NotNull SpiAspspConsentDataProvider aspspConsentDataProvider);

    /**
     * Returns a list of SCA methods for PSU by its login. Used only with embedded SCA Approach.
     *
     * @param contextData              holder of call's context data (e.g. about PSU and TPP)
     * @param businessObject           generic consent/payment object
     * @param aspspConsentDataProvider Provides access to read/write encrypted data to be stored in the consent management system.
     * @return a list of SCA methods applicable for specified PSU
     */
    SpiResponse<List<SpiAuthenticationObject>> requestAvailableScaMethods(@NotNull SpiContextData contextData, T businessObject, @NotNull SpiAspspConsentDataProvider aspspConsentDataProvider);

    /**
     * Performs strong customer authorisation depending on selected SCA method. Used only with embedded SCA Approach.
     *
     * @param contextData              holder of call's context data (e.g. about PSU and TPP)
     * @param authenticationMethodId   Id of a chosen sca method
     * @param businessObject           generic consent/payment object
     * @param aspspConsentDataProvider Provides access to read/write encrypted data to be stored in the consent management system.
     */
    @NotNull
    SpiResponse<SpiAuthorizationCodeResult> requestAuthorisationCode(@NotNull SpiContextData contextData, @NotNull String authenticationMethodId, @NotNull T businessObject, @NotNull SpiAspspConsentDataProvider aspspConsentDataProvider);

    /**
     * Notifies a decoupled app about starting SCA. AuthorisationId is provided to allow the app to access CMS(the same way like redirectId is used in Redirect Approach). Used only with decoupled SCA Approach.
     *
     * @param contextData              holder of call's context data (e.g. about PSU and TPP)
     * @param authorisationId          a unique identifier of authorisation process
     * @param authenticationMethodId   Id of a chosen sca method(for a decoupled SCA method within embedded approach)
     * @param businessObject           generic consent/payment object
     * @param aspspConsentDataProvider Provides access to read/write encrypted data to be stored in the consent management system.
     * @return Return a response object, containing a message from ASPSP to PSU, giving him instructions regarding decoupled SCA starting.
     */
    @NotNull
    default SpiResponse<SpiAuthorisationDecoupledScaResponse> startScaDecoupled(@NotNull SpiContextData contextData, @NotNull String authorisationId, @Nullable String authenticationMethodId, @NotNull T businessObject, @NotNull SpiAspspConsentDataProvider aspspConsentDataProvider) {
        return SpiResponse.<SpiAuthorisationDecoupledScaResponse>builder().fail(SpiResponseStatus.NOT_SUPPORTED);
    }
}

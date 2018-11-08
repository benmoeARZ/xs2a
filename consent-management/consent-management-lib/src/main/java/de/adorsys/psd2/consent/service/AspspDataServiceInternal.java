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

package de.adorsys.psd2.consent.service;

import de.adorsys.psd2.consent.api.AspspDataService;
import de.adorsys.psd2.consent.domain.AspspConsentDataEntity;
import de.adorsys.psd2.consent.repository.AspspConsentDataRepository;
import de.adorsys.psd2.consent.service.security.EncryptedData;
import de.adorsys.psd2.consent.service.security.SecurityDataService;
import de.adorsys.psd2.xs2a.core.consent.AspspConsentData;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AspspDataServiceInternal implements AspspDataService {
    private final SecurityDataService securityDataService;
    private final AspspConsentDataRepository aspspConsentDataRepository;

    @Override
    public @NotNull Optional<AspspConsentData> readAspspConsentData(@NotNull String encryptedConsentId) {
        return getAspspConsentDataEntity(encryptedConsentId)
                   .map(AspspConsentDataEntity::getData)
                   .flatMap(data -> securityDataService.decryptConsentData(encryptedConsentId, data))
                   .map(decryptedData -> new AspspConsentData(decryptedData.getData(), encryptedConsentId));
    }

    @Override
    @Transactional
    public boolean updateAspspConsentData(@NotNull AspspConsentData aspspConsentData) {
        Optional<String> aspspConsentDataBase64 = Optional.ofNullable(aspspConsentData.getAspspConsentData())
                                                      .map(Base64.getEncoder()::encodeToString);

        if (aspspConsentDataBase64.isPresent()) {
            return encryptAndUpdateAspspConsentDataEntity(aspspConsentData.getConsentId(), aspspConsentDataBase64.get());
        }
        return false;
    }

    private Optional<AspspConsentDataEntity> getAspspConsentDataEntity(String encryptedConsentId) {
        return securityDataService.decryptId(encryptedConsentId)
                   .flatMap(aspspConsentDataRepository::findByConsentId);
    }

    private boolean encryptAndUpdateAspspConsentDataEntity(String encryptedConsentId, String aspspConsentDataBase64) {
        Optional<String> consentId = securityDataService.decryptId(encryptedConsentId);
        if (consentId.isPresent()) {
            Optional<EncryptedData> encryptedData = securityDataService.encryptConsentData(encryptedConsentId, aspspConsentDataBase64);

            if (encryptedData.isPresent()) {
                return updateAndSaveAspspConsentData(consentId.get(), encryptedData.get().getData());
            }
        }

        return false;
    }

    private boolean updateAndSaveAspspConsentData(String consentId, byte[] encryptConsentData) {
        AspspConsentDataEntity aspspConsentDataEntity = aspspConsentDataRepository
                                                            .findByConsentId(consentId)
                                                            .orElseGet(() -> new AspspConsentDataEntity(consentId));
        aspspConsentDataEntity.setData(encryptConsentData);

        return aspspConsentDataRepository.save(aspspConsentDataEntity) != null;
    }
}

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

package de.adorsys.psd2.consent.repository;

import de.adorsys.psd2.consent.domain.TppStopListEntity;
import de.adorsys.psd2.xs2a.core.tpp.TppStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TppStopListRepository extends CrudRepository<TppStopListEntity, Long> {

    Optional<TppStopListEntity> findByTppAuthorisationNumberAndNationalAuthorityIdAndInstanceId(@NotNull String tppAuthorisationNumber, @NotNull String nationalAuthorityId, @NotNull String instanceId);

    Optional<TppStopListEntity> findByTppAuthorisationNumberAndInstanceId(@NotNull String tppAuthorisationNumber, @NotNull String instanceId);

    List<TppStopListEntity> findAllByStatusAndBlockingExpirationTimestampLessThanEqual(@NotNull TppStatus tppStatus, @NotNull OffsetDateTime dateTimeToCompare);
}

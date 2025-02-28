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

package de.adorsys.psd2.consent.domain.account;

import de.adorsys.psd2.consent.domain.InstanceDependableEntity;
import de.adorsys.psd2.consent.domain.PsuData;
import de.adorsys.psd2.consent.domain.ScaMethod;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import lombok.Data;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity(name = "ais_consent_authorization")
public class AisConsentAuthorization extends InstanceDependableEntity {
    @Id
    @Column(name = "authorization_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ais_consent_authorization_generator")
    @SequenceGenerator(name = "ais_consent_authorization_generator", sequenceName = "ais_consent_auth_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "psu_id")
    private PsuData psuData;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "consent_id", nullable = false)
    private AisConsent consent;

    @Column(name = "sca_status", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private ScaStatus scaStatus;

    @Column(name = "authentication_method_id")
    private String authenticationMethodId;

    @Column(name = "sca_authentication_data")
    private String scaAuthenticationData;

    @Column(name = "redirect_expiration_timestamp")
    private OffsetDateTime redirectUrlExpirationTimestamp;

    @Column(name = "expiration_timestamp")
    private OffsetDateTime authorisationExpirationTimestamp;

    @ElementCollection
    @CollectionTable(name = "ais_available_sca_method", joinColumns = @JoinColumn(name = "authorisation_id"))
    private List<ScaMethod> availableScaMethods = new ArrayList<>();

    @Column(name = "sca_approach", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private ScaApproach scaApproach;

    public boolean isRedirectUrlNotExpired() {
        return redirectUrlExpirationTimestamp.isAfter(OffsetDateTime.now());
    }

    public boolean isAuthorisationNotExpired() {
        return authorisationExpirationTimestamp.isAfter(OffsetDateTime.now());
    }
}

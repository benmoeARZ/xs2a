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

package de.adorsys.psd2.consent.service.psu;

import de.adorsys.psd2.consent.api.pis.CmsPayment;
import de.adorsys.psd2.consent.api.pis.CmsPaymentResponse;
import de.adorsys.psd2.consent.api.service.PisCommonPaymentService;
import de.adorsys.psd2.consent.domain.PsuData;
import de.adorsys.psd2.consent.domain.TppInfoEntity;
import de.adorsys.psd2.consent.domain.payment.PisAuthorization;
import de.adorsys.psd2.consent.domain.payment.PisCommonPaymentData;
import de.adorsys.psd2.consent.domain.payment.PisPaymentData;
import de.adorsys.psd2.consent.psu.api.CmsPsuAuthorisation;
import de.adorsys.psd2.consent.psu.api.CmsPsuPisService;
import de.adorsys.psd2.consent.psu.api.pis.CmsPisPsuDataAuthorisation;
import de.adorsys.psd2.consent.repository.PisAuthorisationRepository;
import de.adorsys.psd2.consent.repository.PisPaymentDataRepository;
import de.adorsys.psd2.consent.repository.specification.PisAuthorisationSpecification;
import de.adorsys.psd2.consent.repository.specification.PisPaymentDataSpecification;
import de.adorsys.psd2.consent.service.CommonPaymentDataService;
import de.adorsys.psd2.consent.service.mapper.CmsPsuAuthorisationMapper;
import de.adorsys.psd2.consent.service.mapper.CmsPsuPisMapper;
import de.adorsys.psd2.consent.service.mapper.PsuDataMapper;
import de.adorsys.psd2.xs2a.core.exception.AuthorisationIsExpiredException;
import de.adorsys.psd2.xs2a.core.exception.RedirectUrlIsExpiredException;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CmsPsuPisServiceInternal implements CmsPsuPisService {
    private final PisPaymentDataRepository pisPaymentDataRepository;
    private final PisAuthorisationRepository pisAuthorisationRepository;
    private final CmsPsuPisMapper cmsPsuPisMapper;
    private final PisCommonPaymentService pisCommonPaymentService;
    private final CommonPaymentDataService commonPaymentDataService;
    private final PsuDataMapper psuDataMapper;
    private final PisAuthorisationSpecification pisAuthorisationSpecification;
    private final PisPaymentDataSpecification pisPaymentDataSpecification;
    private final CmsPsuService cmsPsuService;
    private final CmsPsuAuthorisationMapper cmsPsuPisAuthorisationMapper;

    @Override
    @Transactional
    public boolean updatePsuInPayment(@NotNull PsuIdData psuIdData, @NotNull String authorisationId, @NotNull String instanceId) throws AuthorisationIsExpiredException {
        return getAuthorisationByExternalId(authorisationId, instanceId)
                   .map(auth -> updatePsuData(auth, psuIdData))
                   .orElseGet(() -> {
                       log.info("Authorisation ID [{}], Instance ID: [{}]. Update PSU  in Payment failed, because authorisation not found",
                                authorisationId, instanceId);
                       return false;
                   });
    }

    @Override
    @Transactional
    public @NotNull Optional<CmsPaymentResponse> checkRedirectAndGetPayment(@NotNull String redirectId,
                                                                            @NotNull String instanceId)
        throws RedirectUrlIsExpiredException {

        Optional<PisAuthorization> optionalAuthorisation = pisAuthorisationRepository
                                                               .findOne(pisAuthorisationSpecification.byExternalIdAndInstanceId(redirectId, instanceId));

        if (optionalAuthorisation.isPresent()) {
            PisAuthorization authorisation = optionalAuthorisation.get();

            if (!authorisation.isRedirectUrlNotExpired()) {
                log.info("Authorisation ID [{}], Instance ID: [{}]. Check redirect URL and get payment failed, because redirect URL is expired",
                         authorisation.getId(), instanceId);
                changeAuthorisationStatusToFailed(authorisation);

                String tppNokRedirectUri = authorisation.getPaymentData().getTppInfo().getNokRedirectUri();
                throw new RedirectUrlIsExpiredException(tppNokRedirectUri);
            }
            return Optional.of(buildCmsPaymentResponse(authorisation, false));
        }

        log.info("Authorisation ID [{}], Instance ID: [{}]. Check redirect URL and get payment failed, because authorisation not found or has finalised status",
                 redirectId, instanceId);
        return Optional.empty();
    }

    @Transactional
    @Override
    public @NotNull Optional<CmsPayment> getPayment(@NotNull PsuIdData psuIdData, @NotNull String paymentId, @NotNull String instanceId) {
        if (isPsuDataEquals(paymentId, psuIdData)) {
            List<PisPaymentData> list = pisPaymentDataRepository.findAll(pisPaymentDataSpecification.byPaymentIdAndInstanceId(paymentId, instanceId));

            // todo implementation should be changed https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/534
            if (!list.isEmpty()) {
                return Optional.of(cmsPsuPisMapper.mapToCmsPayment(list));
            } else {
                return commonPaymentDataService.getPisCommonPaymentData(paymentId, instanceId)
                           .map(cmsPsuPisMapper::mapToCmsPayment);
            }
        }
        log.info("Payment ID: [{}], Instance ID: [{}]. Get payment failed, because given PSU data and PSU data stored in payment are not equal",
                 paymentId, instanceId);
        return Optional.empty();
    }

    @Override
    @Transactional
    public @NotNull Optional<CmsPaymentResponse> checkRedirectAndGetPaymentForCancellation(@NotNull String redirectId,
                                                                                           @NotNull String instanceId)
        throws RedirectUrlIsExpiredException {

        Optional<PisAuthorization> optionalAuthorisation = pisAuthorisationRepository
                                                               .findOne(pisAuthorisationSpecification.byExternalIdAndInstanceId(redirectId, instanceId))
                                                               .filter(a -> !a.getScaStatus().isFinalisedStatus());

        if (optionalAuthorisation.isPresent()) {
            PisAuthorization authorisation = optionalAuthorisation.get();

            if (!authorisation.isRedirectUrlNotExpired()) {
                log.info("Authorisation ID [{}], Instance ID: [{}]. Check redirect URL and get payment cancellation failed, because authorisation not found or has finalised status",
                         redirectId, instanceId);
                changeAuthorisationStatusToFailed(authorisation);

                String tppNokRedirectUri = authorisation.getPaymentData().getTppInfo().getNokRedirectUri();
                throw new RedirectUrlIsExpiredException(tppNokRedirectUri);
            }
            return Optional.of(buildCmsPaymentResponse(authorisation, true));
        }
        log.info("Authorisation ID [{}], Instance ID: [{}]. Check redirect URL and get payment cancellation failed, because authorisation not found or has finalised status",
                 redirectId, instanceId);
        return Optional.empty();
    }

    @Override
    public @NotNull Optional<CmsPsuAuthorisation> getAuthorisationByAuthorisationId(@NotNull String authorisationId, @NotNull String instanceId) {
        Optional<PisAuthorization> optionalAuthorisation = pisAuthorisationRepository
                                                               .findOne(pisAuthorisationSpecification.byExternalIdAndInstanceId(authorisationId, instanceId));

        if (optionalAuthorisation.isPresent()) {
            PisAuthorization authorisation = optionalAuthorisation.get();
            return Optional.of(cmsPsuPisAuthorisationMapper.mapToCmsPsuAuthorisationPis(authorisation));
        }

        log.info("Authorisation ID: [{}], Instance ID: [{}]. Get authorisation failed, because authorisation not found",
                 authorisationId, instanceId);

        return Optional.empty();
    }

    @Override
    @Transactional
    public boolean updateAuthorisationStatus(@NotNull PsuIdData psuIdData, @NotNull String paymentId,
                                             @NotNull String authorisationId, @NotNull ScaStatus status, @NotNull String instanceId) throws AuthorisationIsExpiredException {
        Optional<PisAuthorization> pisAuthorisation = getAuthorisationByExternalId(authorisationId, instanceId);

        if (!pisAuthorisation.isPresent()) {
            log.info("Authorisation ID [{}], Instance ID: [{}]. Update authorisation status failed, because authorisation not found.",
                     authorisationId, instanceId);
            return false;
        }

        boolean isValid = pisAuthorisation
                              .map(auth -> auth.getPaymentData().getPaymentId())
                              .map(id -> validateGivenData(id, paymentId, psuIdData))
                              .orElse(false);

        if (!isValid) {
            log.info("Authorisation ID [{}], Instance ID: [{}]. Update authorisation status failed, because request data is not valid",
                     authorisationId, instanceId);
            return false;
        }

        return updateAuthorisationStatusAndSaveAuthorisation(pisAuthorisation.get(), status);
    }

    @Override
    @Transactional
    public boolean updatePaymentStatus(@NotNull String paymentId, @NotNull TransactionStatus status, @NotNull String instanceId) {
        Optional<PisCommonPaymentData> paymentDataOptional = commonPaymentDataService.getPisCommonPaymentData(paymentId, instanceId);

        return paymentDataOptional
                   .filter(p -> p.getTransactionStatus().isNotFinalisedStatus())
                   .map(pd -> commonPaymentDataService.updateStatusInPaymentData(pd, status))
                   .orElseGet(() -> {
                       log.info("Payment ID [{}], Instance ID: [{}]. Update payment status failed, because PIS common payment data not found",
                                paymentId, instanceId);
                       return false;
                   });
    }

    @Override
    public Optional<List<CmsPisPsuDataAuthorisation>> getPsuDataAuthorisations(@NotNull String paymentId, @NotNull String instanceId) {
        return commonPaymentDataService.getPisCommonPaymentData(paymentId, instanceId)
                   .map(PisCommonPaymentData::getAuthorizations)
                   .map(this::getPsuDataAuthorisations);
    }

    @NotNull
    private List<CmsPisPsuDataAuthorisation> getPsuDataAuthorisations(List<PisAuthorization> authorisations) {
        return authorisations.stream()
                   .filter(auth -> Objects.nonNull(auth.getPsuData()))
                   .map(auth -> new CmsPisPsuDataAuthorisation(psuDataMapper.mapToPsuIdData(auth.getPsuData()),
                                                               auth.getExternalId(),
                                                               auth.getScaStatus(),
                                                               auth.getAuthorizationType()))
                   .collect(Collectors.toList());
    }

    private boolean updatePsuData(PisAuthorization authorisation, PsuIdData psuIdData) {
        PsuData newPsuData = psuDataMapper.mapToPsuData(psuIdData);
        if (newPsuData == null || StringUtils.isBlank(newPsuData.getPsuId())) {
            log.info("Authorisation ID [{}]. Update PSU in payment failed in updatePsuData method, because newPsuData or psuId in newPsuData is empty or null",
                     authorisation.getId());
            return false;
        }

        Optional<PsuData> optionalPsuData = Optional.ofNullable(authorisation.getPsuData());
        if (optionalPsuData.isPresent()) {
            newPsuData.setId(optionalPsuData.get().getId());
        } else {
            List<PsuData> paymentPsuList = authorisation.getPaymentData().getPsuDataList();
            Optional<PsuData> psuDataOptional = cmsPsuService.definePsuDataForAuthorisation(newPsuData, paymentPsuList);

            if (psuDataOptional.isPresent()) {
                newPsuData = psuDataOptional.get();
                authorisation.getPaymentData().setPsuDataList(cmsPsuService.enrichPsuData(newPsuData, paymentPsuList));
            }
            log.info("Authorisation ID [{}]. Update PSU in payment failed in updatePsuData method because authorisation contains no PSU data.", authorisation.getId());
        }

        authorisation.setPsuData(newPsuData);
        pisAuthorisationRepository.save(authorisation);
        return true;
    }

    private boolean validateGivenData(String realPaymentId, String givenPaymentId, PsuIdData psuIdData) {
        return Optional.of(givenPaymentId)
                   .filter(p -> isPsuDataEquals(p, psuIdData))
                   .map(id -> StringUtils.equals(realPaymentId, id))
                   .orElseGet(() -> {
                       log.info("Cannot validate given PSU data, because given payment ID is null");
                       return false;
                   });
    }

    private boolean updateAuthorisationStatusAndSaveAuthorisation(PisAuthorization pisAuthorisation, ScaStatus status) {
        if (pisAuthorisation.getScaStatus().isFinalisedStatus()) {
            log.info("Authorisation ID [{}], SCA status: [{}]. Update authorisation status failed in updateAuthorisationStatusAndSaveAuthorisation method, " +
                         "because authorisation has finalised status", pisAuthorisation.getExternalId(), pisAuthorisation.getScaStatus().getValue());
            return false;
        }
        pisAuthorisation.setScaStatus(status);
        return Optional.ofNullable(pisAuthorisationRepository.save(pisAuthorisation))
                   .isPresent();
    }

    private boolean isPsuDataEquals(String paymentId, PsuIdData psuIdData) {
        return pisCommonPaymentService.getPsuDataListByPaymentId(paymentId)
                   .map(lst -> lst.stream()
                                   .anyMatch(psu -> psu.contentEquals(psuIdData)))
                   .orElseGet(() -> {
                       log.info("Payment ID: [{}]. Cannot equal PSU data with payment ID, because PSU data list not found by ID", paymentId);
                       return false;
                   });
    }

    private CmsPaymentResponse buildCmsPaymentResponse(PisAuthorization authorisation, boolean isPaymentCancellation) {
        PisCommonPaymentData commonPayment = authorisation.getPaymentData();
        CmsPayment payment = cmsPsuPisMapper.mapPaymentDataToCmsPayment(commonPayment);
        TppInfoEntity tppInfo = commonPayment.getTppInfo();

        return new CmsPaymentResponse(
            payment,
            authorisation.getExternalId(),
            isPaymentCancellation ? tppInfo.getCancelRedirectUri() : tppInfo.getRedirectUri(),
            isPaymentCancellation ? tppInfo.getCancelNokRedirectUri() : tppInfo.getNokRedirectUri());
    }

    private void changeAuthorisationStatusToFailed(PisAuthorization authorisation) {
        authorisation.setScaStatus(ScaStatus.FAILED);
        pisAuthorisationRepository.save(authorisation);
    }

    private Optional<PisAuthorization> getAuthorisationByExternalId(@NotNull String authorisationId, @NotNull String instanceId) throws AuthorisationIsExpiredException {
        Optional<PisAuthorization> authorization = pisAuthorisationRepository.findOne(pisAuthorisationSpecification.byExternalIdAndInstanceId(authorisationId, instanceId));

        if (authorization.isPresent() && !authorization.get().isAuthorisationNotExpired()) {
            log.info("Authorisation ID [{}], Instance ID: [{}]. Authorisation is expired", authorisationId, instanceId);
            throw new AuthorisationIsExpiredException(authorization.get().getPaymentData().getTppInfo().getNokRedirectUri());
        }
        return authorization;
    }
}

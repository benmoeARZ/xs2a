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


package de.adorsys.psd2.consent.service;

import de.adorsys.psd2.consent.api.CmsAuthorisationType;
import de.adorsys.psd2.consent.api.pis.CmsPayment;
import de.adorsys.psd2.consent.api.pis.CmsPaymentResponse;
import de.adorsys.psd2.consent.api.pis.CmsSinglePayment;
import de.adorsys.psd2.consent.api.service.PisCommonPaymentService;
import de.adorsys.psd2.consent.domain.AccountReferenceEntity;
import de.adorsys.psd2.consent.domain.PsuData;
import de.adorsys.psd2.consent.domain.TppInfoEntity;
import de.adorsys.psd2.consent.domain.payment.PisAuthorization;
import de.adorsys.psd2.consent.domain.payment.PisCommonPaymentData;
import de.adorsys.psd2.consent.domain.payment.PisPaymentData;
import de.adorsys.psd2.consent.psu.api.pis.CmsPisPsuDataAuthorisation;
import de.adorsys.psd2.consent.repository.PisAuthorisationRepository;
import de.adorsys.psd2.consent.repository.PisPaymentDataRepository;
import de.adorsys.psd2.consent.repository.specification.PisAuthorisationSpecification;
import de.adorsys.psd2.consent.repository.specification.PisPaymentDataSpecification;
import de.adorsys.psd2.consent.service.mapper.CmsPsuPisMapper;
import de.adorsys.psd2.consent.service.mapper.PsuDataMapper;
import de.adorsys.psd2.consent.service.psu.CmsPsuPisServiceInternal;
import de.adorsys.psd2.xs2a.core.exception.AuthorisationIsExpiredException;
import de.adorsys.psd2.xs2a.core.exception.RedirectUrlIsExpiredException;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CmsPsuPisServiceInternalTest {
    private static final String WRONG_PAYMENT_ID = "wrong payment id";
    private static final String AUTHORISATION_ID = "authorisation id";
    private static final String WRONG_AUTHORISATION_ID = "wrong authorisation id";
    private static final String PAYMENT_PRODUCT = "sepa-credit-transfers";
    private static final String FINALISED_PAYMENT_ID = "finalised payment id";
    private static final String FINALISED_AUTHORISATION_ID = "finalised authorisation id";
    private static final String EXPIRED_AUTHORISATION_ID = "expired authorisation id";
    private static final String TPP_NOK_REDIRECT_URI = "tpp nok redirect uri";
    private final PsuIdData WRONG_PSU_ID_DATA = buildWrongPsuIdData();
    private final PsuIdData PSU_ID_DATA = buildPsuIdData();
    private static final String PAYMENT_ID = "payment id";
    private static final String DEFAULT_SERVICE_INSTANCE_ID = "UNDEFINED";
    private static final CmsAuthorisationType AUTHORISATION_TYPE_CREATED = CmsAuthorisationType.CREATED;

    @InjectMocks
    private CmsPsuPisServiceInternal cmsPsuPisServiceInternal;

    @Mock
    private PisPaymentDataRepository pisPaymentDataRepository;
    @Mock
    private PisAuthorisationRepository pisAuthorisationRepository;
    @Mock
    private CmsPsuPisMapper cmsPsuPisMapper;
    @Mock
    private PisCommonPaymentService pisCommonPaymentService;
    @Mock
    private PsuDataMapper psuDataMapper;
    @Mock
    private CommonPaymentDataService commonPaymentDataService;
    @Mock
    private PisAuthorisationSpecification pisAuthorisationSpecification;
    @Mock
    private PisPaymentDataSpecification pisPaymentDataSpecification;

    private CmsPayment cmsPayment;

    @Before
    public void setUp() {
        PsuData psuData = buildPsuData();
        PsuIdData psuIdData = buildPsuIdData();
        cmsPayment = buildCmsPayment();

        when(cmsPsuPisMapper.mapToCmsPayment(buildPisPaymentDataList()))
            .thenReturn(cmsPayment);

        when(pisCommonPaymentService.getPsuDataListByPaymentId(PAYMENT_ID))
            .thenReturn(Optional.of(Collections.singletonList(psuIdData)));
        when(pisCommonPaymentService.getPsuDataListByPaymentId(WRONG_PAYMENT_ID))
            .thenReturn(Optional.empty());

        when(psuDataMapper.mapToPsuData(psuIdData))
            .thenReturn(psuData);
        when(psuDataMapper.mapToPsuIdData(any(PsuData.class)))
            .thenReturn(psuIdData);
    }

    @Test
    public void updatePsuInPayment_Success() throws AuthorisationIsExpiredException {
        // Given
        when(pisAuthorisationSpecification.byExternalIdAndInstanceId(AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID))
            .thenReturn((root, criteriaQuery, criteriaBuilder) -> null);
        //noinspection unchecked
        when(pisAuthorisationRepository.findOne(any(Specification.class))).thenReturn(Optional.ofNullable(buildPisAuthorisation()));

        // When
        boolean actualResult = cmsPsuPisServiceInternal.updatePsuInPayment(PSU_ID_DATA, AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertTrue(actualResult);
        verify(pisAuthorisationSpecification, times(1))
            .byExternalIdAndInstanceId(AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);
    }

    @Test(expected = AuthorisationIsExpiredException.class)
    public void updatePsuInPayment_athorisationIsExpired() throws AuthorisationIsExpiredException {
        // Given
        when(pisAuthorisationSpecification.byExternalIdAndInstanceId(AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID))
            .thenReturn((root, criteriaQuery, criteriaBuilder) -> null);
        PisAuthorization pisAuthorization = buildPisAuthorisation();
        pisAuthorization.setAuthorisationExpirationTimestamp(OffsetDateTime.now().minusDays(1));
        //noinspection unchecked
        when(pisAuthorisationRepository.findOne(any(Specification.class))).thenReturn(Optional.of(pisAuthorization));

        // When
        boolean actualResult = cmsPsuPisServiceInternal.updatePsuInPayment(PSU_ID_DATA, AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertTrue(actualResult);
        verify(pisAuthorisationSpecification, times(1))
            .byExternalIdAndInstanceId(AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);
    }

    @Test
    public void updatePsuInPayment_Fail_WrongPaymentId() throws AuthorisationIsExpiredException {
        // Given

        // When
        boolean actualResult = cmsPsuPisServiceInternal.updatePsuInPayment(PSU_ID_DATA, WRONG_AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertFalse(actualResult);
        verify(pisAuthorisationSpecification, times(1))
            .byExternalIdAndInstanceId(WRONG_AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);
    }

    @Test
    public void getPayment_Success() {
        // Given
        when(pisPaymentDataSpecification.byPaymentIdAndInstanceId(PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID))
            .thenReturn((root, criteriaQuery, criteriaBuilder) -> null);
        //noinspection unchecked
        when(pisPaymentDataRepository.findAll(any(Specification.class))).thenReturn(buildPisPaymentDataList());

        // When
        Optional<CmsPayment> actualResult = cmsPsuPisServiceInternal.getPayment(PSU_ID_DATA, PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertTrue(actualResult.isPresent());
        assertThat(actualResult.get().getPaymentId()).isEqualTo(PAYMENT_ID);
        verify(pisPaymentDataSpecification, times(1))
            .byPaymentIdAndInstanceId(PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID);
        verify(commonPaymentDataService, never()).getPisCommonPaymentData(any(), any());
    }

    @Test
    public void getPayment_emptyList_Success() {
        // Given
        when(pisPaymentDataSpecification.byPaymentIdAndInstanceId(PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID))
            .thenReturn((root, criteriaQuery, criteriaBuilder) -> null);
        //noinspection unchecked
        when(pisPaymentDataRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());
        PisCommonPaymentData pisCommonPaymentData = new PisCommonPaymentData();
        when(commonPaymentDataService.getPisCommonPaymentData(PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID)).thenReturn(Optional.of(pisCommonPaymentData));
        when(cmsPsuPisMapper.mapToCmsPayment(pisCommonPaymentData)).thenReturn(cmsPayment);

        // When
        Optional<CmsPayment> actualResult = cmsPsuPisServiceInternal.getPayment(PSU_ID_DATA, PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertTrue(actualResult.isPresent());
        assertThat(actualResult.get().getPaymentId()).isEqualTo(PAYMENT_ID);
        verify(pisPaymentDataSpecification, times(1))
            .byPaymentIdAndInstanceId(PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID);
        verify(commonPaymentDataService, times(1))
            .getPisCommonPaymentData(PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID);
    }

    @Test
    public void getPayment_Fail_WrongPaymentId() {
        // Given

        // When
        Optional<CmsPayment> actualResult = cmsPsuPisServiceInternal.getPayment(PSU_ID_DATA, WRONG_PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertFalse(actualResult.isPresent());
        verify(pisCommonPaymentService, times(1))
            .getPsuDataListByPaymentId(WRONG_PAYMENT_ID);
        verify(commonPaymentDataService, never()).getPisCommonPaymentData(any(), any());
    }

    @Test
    public void getPayment_Fail_WrongPsuIdData() {
        // Given

        // When
        Optional<CmsPayment> actualResult = cmsPsuPisServiceInternal.getPayment(WRONG_PSU_ID_DATA, PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertFalse(actualResult.isPresent());
        verify(pisPaymentDataSpecification, never())
            .byPaymentIdAndInstanceId(PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID);
        verify(commonPaymentDataService, never()).getPisCommonPaymentData(any(), any());
    }

    @Test
    public void updateAuthorisationStatus_Success() throws AuthorisationIsExpiredException {
        // Given
        when(pisAuthorisationSpecification.byExternalIdAndInstanceId(AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID))
            .thenReturn((root, criteriaQuery, criteriaBuilder) -> null);
        PisAuthorization pisAuthorization = buildPisAuthorisation();
        //noinspection unchecked
        when(pisAuthorisationRepository.findOne(any(Specification.class))).thenReturn(Optional.ofNullable(pisAuthorization));
        when(pisAuthorisationRepository.save(pisAuthorization)).thenReturn(pisAuthorization);

        // When
        boolean actualResult = cmsPsuPisServiceInternal.updateAuthorisationStatus(PSU_ID_DATA, PAYMENT_ID, AUTHORISATION_ID, ScaStatus.FAILED, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertTrue(actualResult);
        verify(pisAuthorisationSpecification, times(1))
            .byExternalIdAndInstanceId(AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);
        verify(pisAuthorisationRepository, times(1)).save(any(PisAuthorization.class));
    }

    @Test
    public void updateAuthorisationStatus_Fail_InvalidRequestData() throws AuthorisationIsExpiredException {
        // Given
        when(pisAuthorisationSpecification.byExternalIdAndInstanceId(AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID))
            .thenReturn((root, criteriaQuery, criteriaBuilder) -> null);
        PisAuthorization pisAuthorization = buildPisAuthorisation();
        //noinspection unchecked
        when(pisAuthorisationRepository.findOne(any(Specification.class))).thenReturn(Optional.ofNullable(pisAuthorization));

        // When
        boolean actualResult = cmsPsuPisServiceInternal.updateAuthorisationStatus(PSU_ID_DATA, WRONG_PAYMENT_ID, AUTHORISATION_ID, ScaStatus.FAILED, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertFalse(actualResult);
        verify(pisAuthorisationSpecification, times(1))
            .byExternalIdAndInstanceId(AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);
    }

    @Test
    public void updateAuthorisationStatus_WrongPaymentId() throws AuthorisationIsExpiredException {
        // Given

        // When
        boolean actualResult = cmsPsuPisServiceInternal.updateAuthorisationStatus(PSU_ID_DATA, WRONG_PAYMENT_ID, AUTHORISATION_ID, ScaStatus.FAILED, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertFalse(actualResult);
        verify(pisAuthorisationSpecification, times(1))
            .byExternalIdAndInstanceId(AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);
    }

    @Test
    public void updateAuthorisationStatus_WrongPsuIdData() throws AuthorisationIsExpiredException {
        // Given

        // When
        boolean actualResult = cmsPsuPisServiceInternal.updateAuthorisationStatus(WRONG_PSU_ID_DATA, PAYMENT_ID, AUTHORISATION_ID, ScaStatus.FAILED, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertFalse(actualResult);
        verify(pisAuthorisationSpecification, times(1))
            .byExternalIdAndInstanceId(AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);
    }

    @Test
    public void updateAuthorisationStatus_WrongAuthorisationId() throws AuthorisationIsExpiredException {
        // Given

        // When
        boolean actualResult = cmsPsuPisServiceInternal.updateAuthorisationStatus(PSU_ID_DATA, PAYMENT_ID, WRONG_AUTHORISATION_ID, ScaStatus.FAILED, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertFalse(actualResult);
        verify(pisAuthorisationSpecification, times(1))
            .byExternalIdAndInstanceId(WRONG_AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);
    }

    @Test
    public void updatePaymentStatus_Success() {
        // Given
        when(commonPaymentDataService.getPisCommonPaymentData(PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID))
            .thenReturn(Optional.of(buildPisCommonPaymentData()));
        when(commonPaymentDataService.updateStatusInPaymentData(buildPisCommonPaymentData(), TransactionStatus.RCVD)).thenReturn(true);

        // When
        boolean actualResult = cmsPsuPisServiceInternal.updatePaymentStatus(PAYMENT_ID, TransactionStatus.RCVD, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertTrue(actualResult);
    }

    @Test
    public void updatePaymentStatus_Fail_WrongPaymentId() {
        // Given
        when(commonPaymentDataService.getPisCommonPaymentData(WRONG_PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID))
            .thenReturn(Optional.empty());

        // When
        boolean actualResult = cmsPsuPisServiceInternal.updatePaymentStatus(WRONG_PAYMENT_ID, TransactionStatus.CANC, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertFalse(actualResult);
    }

    @Test
    public void getPsuDataAuthorisations_Success() {
        // Given
        when(commonPaymentDataService.getPisCommonPaymentData(PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID))
            .thenReturn(Optional.of(buildPisCommonPaymentDataWithAuthorisation()));

        // When
        Optional<List<CmsPisPsuDataAuthorisation>> actualResult = cmsPsuPisServiceInternal.getPsuDataAuthorisations(PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertTrue(actualResult.isPresent());
        assertThat(actualResult.get().size()).isEqualTo(1);
        assertThat(actualResult.get().get(0).getAuthorisationType()).isEqualTo(AUTHORISATION_TYPE_CREATED);
    }

    @Test
    public void getPsuDataAuthorisationsEmptyPsuData_Success() {
        // Given
        when(commonPaymentDataService.getPisCommonPaymentData(PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID))
            .thenReturn(Optional.of(buildPisCommonPaymentDataWithAuthorisationEmptyPsuData()));

        // When
        Optional<List<CmsPisPsuDataAuthorisation>> actualResult = cmsPsuPisServiceInternal.getPsuDataAuthorisations(PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertTrue(actualResult.isPresent());
        assertTrue(actualResult.get().isEmpty());
    }

    @Test
    public void updateAuthorisationStatus_Fail_FinalisedStatus() throws AuthorisationIsExpiredException {
        //Given

        // When
        boolean actualResult = cmsPsuPisServiceInternal.updateAuthorisationStatus(PSU_ID_DATA, PAYMENT_ID, FINALISED_AUTHORISATION_ID, ScaStatus.SCAMETHODSELECTED, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertFalse(actualResult);
        verify(pisAuthorisationSpecification, times(1))
            .byExternalIdAndInstanceId(FINALISED_AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);
    }

    @Test
    public void updatePaymentStatus_Fail_FinalisedStatus() {
        //Given
        when(commonPaymentDataService.getPisCommonPaymentData(FINALISED_PAYMENT_ID, DEFAULT_SERVICE_INSTANCE_ID))
            .thenReturn(Optional.of(buildFinalisedPisCommonPaymentData()));

        // When
        boolean actualResult = cmsPsuPisServiceInternal.updatePaymentStatus(FINALISED_PAYMENT_ID, TransactionStatus.CANC, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertFalse(actualResult);
    }

    @Test
    public void getPaymentByAuthorisationId_Success() throws RedirectUrlIsExpiredException {
        //Given
        when(pisAuthorisationSpecification.byExternalIdAndInstanceId(AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID))
            .thenReturn((root, criteriaQuery, criteriaBuilder) -> null);
        PisAuthorization expectedAuthorisation = buildPisAuthorisation();
        //noinspection unchecked
        when(pisAuthorisationRepository.findOne(any(Specification.class))).thenReturn(Optional.ofNullable(expectedAuthorisation));

        // When
        Optional<CmsPaymentResponse> actualResult = cmsPsuPisServiceInternal.checkRedirectAndGetPayment(AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertTrue(actualResult.isPresent());
        assertThat(actualResult.get().getAuthorisationId()).isEqualTo(AUTHORISATION_ID);
        verify(pisAuthorisationSpecification, times(1))
            .byExternalIdAndInstanceId(AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);
    }

    @Test(expected = RedirectUrlIsExpiredException.class)
    public void getPaymentByAuthorisationId_Fail_ExpiredRedirectUrl() throws RedirectUrlIsExpiredException {
        //Given
        when(pisAuthorisationSpecification.byExternalIdAndInstanceId(EXPIRED_AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID))
            .thenReturn((root, criteriaQuery, criteriaBuilder) -> null);
        PisAuthorization expectedAuthorisation = buildExpiredAuthorisation();
        //noinspection unchecked
        when(pisAuthorisationRepository.findOne(any(Specification.class))).thenReturn(Optional.ofNullable(expectedAuthorisation));

        // When
        Optional<CmsPaymentResponse> actualResult = cmsPsuPisServiceInternal.checkRedirectAndGetPayment(EXPIRED_AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertThat(actualResult).isEqualTo(Optional.of(new CmsPaymentResponse(TPP_NOK_REDIRECT_URI)));
        verify(pisAuthorisationSpecification, times(1))
            .byExternalIdAndInstanceId(EXPIRED_AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);
    }

    @Test
    public void getPaymentByAuthorisationId_Fail_WrongId() throws RedirectUrlIsExpiredException {
        // Given

        // When
        Optional<CmsPaymentResponse> actualResult = cmsPsuPisServiceInternal.checkRedirectAndGetPayment(WRONG_AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertThat(actualResult).isEqualTo(Optional.empty());
        verify(pisAuthorisationSpecification, times(1))
            .byExternalIdAndInstanceId(WRONG_AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);
    }

    @Test
    public void checkRedirectAndGetPaymentForCancellation_Success() throws RedirectUrlIsExpiredException {
        //Given
        when(pisAuthorisationSpecification.byExternalIdAndInstanceId(AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID))
            .thenReturn((root, criteriaQuery, criteriaBuilder) -> null);
        PisAuthorization expectedAuthorisation = buildPisAuthorisation();
        //noinspection unchecked
        when(pisAuthorisationRepository.findOne(any(Specification.class))).thenReturn(Optional.ofNullable(expectedAuthorisation));

        // When
        Optional<CmsPaymentResponse> actualResult = cmsPsuPisServiceInternal.checkRedirectAndGetPaymentForCancellation(AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertTrue(actualResult.isPresent());
        assertThat(actualResult.get().getAuthorisationId()).isEqualTo(AUTHORISATION_ID);
        verify(pisAuthorisationSpecification, times(1))
            .byExternalIdAndInstanceId(AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);
    }

    @Test(expected = RedirectUrlIsExpiredException.class)
    public void checkRedirectAndGetPaymentForCancellation_Fail_ExpiredRedirectUrl() throws RedirectUrlIsExpiredException {
        //Given
        when(pisAuthorisationSpecification.byExternalIdAndInstanceId(EXPIRED_AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID))
            .thenReturn((root, criteriaQuery, criteriaBuilder) -> null);
        PisAuthorization expectedAuthorisation = buildExpiredAuthorisation();
        //noinspection unchecked
        when(pisAuthorisationRepository.findOne(any(Specification.class))).thenReturn(Optional.ofNullable(expectedAuthorisation));

        // When
        Optional<CmsPaymentResponse> actualResult = cmsPsuPisServiceInternal.checkRedirectAndGetPaymentForCancellation(EXPIRED_AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertThat(actualResult).isEqualTo(Optional.of(new CmsPaymentResponse(TPP_NOK_REDIRECT_URI)));
        verify(pisAuthorisationSpecification, times(1))
            .byExternalIdAndInstanceId(EXPIRED_AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);
    }

    @Test
    public void checkRedirectAndGetPaymentForCancellation_Fail_WrongId() throws RedirectUrlIsExpiredException {
        // Given

        // When
        Optional<CmsPaymentResponse> actualResult = cmsPsuPisServiceInternal.checkRedirectAndGetPaymentForCancellation(WRONG_AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);

        // Then
        assertThat(actualResult).isEqualTo(Optional.empty());
        verify(pisAuthorisationSpecification, times(1))
            .byExternalIdAndInstanceId(WRONG_AUTHORISATION_ID, DEFAULT_SERVICE_INSTANCE_ID);
    }

    private PsuIdData buildPsuIdData() {
        return new PsuIdData(
            "psuId",
            "psuIdType",
            "psuCorporateId",
            "psuCorporateIdType"
        );
    }

    private PsuIdData buildWrongPsuIdData() {
        return new PsuIdData(
            "wrong psuId",
            "psuIdType",
            "wrong psuCorporateId",
            "psuCorporateIdType"
        );
    }

    private PisAuthorization buildPisAuthorisation() {
        PisAuthorization pisAuthorisation = new PisAuthorization();
        pisAuthorisation.setScaStatus(ScaStatus.PSUAUTHENTICATED);
        pisAuthorisation.setAuthorizationType(CmsAuthorisationType.CREATED);
        pisAuthorisation.setPaymentData(buildPisCommonPaymentData());
        pisAuthorisation.setExternalId(AUTHORISATION_ID);
        pisAuthorisation.setPsuData(buildPsuData());
        pisAuthorisation.setRedirectUrlExpirationTimestamp(OffsetDateTime.now().plusHours(1));
        pisAuthorisation.setAuthorisationExpirationTimestamp(OffsetDateTime.now().plusHours(1));
        pisAuthorisation.setPaymentData(buildPisCommonPaymentData());

        return pisAuthorisation;
    }

    private PisCommonPaymentData buildPisCommonPaymentData() {
        PisCommonPaymentData pisCommonPaymentData = new PisCommonPaymentData();
        pisCommonPaymentData.setTransactionStatus(TransactionStatus.RCVD);
        pisCommonPaymentData.setPsuDataList(Collections.singletonList(buildPsuData()));
        pisCommonPaymentData.setPaymentType(PaymentType.SINGLE);
        pisCommonPaymentData.setPaymentProduct(PAYMENT_PRODUCT);
        pisCommonPaymentData.setPayments(buildPisPaymentDataListForCommonData());
        pisCommonPaymentData.setTppInfo(buildTppInfo());
        pisCommonPaymentData.setPaymentId(PAYMENT_ID);
        pisCommonPaymentData.setCreationTimestamp(OffsetDateTime.of(2018, 10, 10, 10, 10, 10, 10, ZoneOffset.UTC));
        return pisCommonPaymentData;
    }

    private PisCommonPaymentData buildPisCommonPaymentDataWithAuthorisation() {
        PisCommonPaymentData pisCommonPaymentData = buildPisCommonPaymentData();
        pisCommonPaymentData.setAuthorizations(Collections.singletonList(buildFinalisedAuthorisation()));
        return pisCommonPaymentData;
    }

    private PisCommonPaymentData buildPisCommonPaymentDataWithAuthorisationEmptyPsuData() {
        PisCommonPaymentData pisCommonPaymentData = new PisCommonPaymentData();
        pisCommonPaymentData.setTransactionStatus(TransactionStatus.RCVD);
        pisCommonPaymentData.setPaymentType(PaymentType.SINGLE);
        pisCommonPaymentData.setPaymentProduct(PAYMENT_PRODUCT);
        pisCommonPaymentData.setPayments(buildPisPaymentDataListForCommonData());
        pisCommonPaymentData.setTppInfo(buildTppInfo());
        pisCommonPaymentData.setPaymentId(PAYMENT_ID);
        pisCommonPaymentData.setCreationTimestamp(OffsetDateTime.of(2018, 10, 10, 10, 10, 10, 10, ZoneOffset.UTC));
        pisCommonPaymentData.setAuthorizations(Collections.singletonList(buildFinalisedAuthorisationNoPsuData()));
        return pisCommonPaymentData;
    }

    private PisCommonPaymentData buildFinalisedPisCommonPaymentData() {
        PisCommonPaymentData pisCommonPaymentData = new PisCommonPaymentData();
        pisCommonPaymentData.setTransactionStatus(TransactionStatus.RJCT);
        pisCommonPaymentData.setPsuDataList(Collections.singletonList(buildPsuData()));
        pisCommonPaymentData.setPaymentType(PaymentType.SINGLE);
        pisCommonPaymentData.setPaymentProduct(PAYMENT_PRODUCT);
        pisCommonPaymentData.setPayments(buildPisPaymentDataListForCommonData());
        pisCommonPaymentData.setTppInfo(buildTppInfo());
        pisCommonPaymentData.setPaymentId(PAYMENT_ID);
        pisCommonPaymentData.setCreationTimestamp(OffsetDateTime.of(2018, 10, 10, 10, 10, 10, 10, ZoneOffset.UTC));
        return pisCommonPaymentData;
    }

    private TppInfoEntity buildTppInfo() {
        TppInfoEntity tppInfoEntity = new TppInfoEntity();
        tppInfoEntity.setNokRedirectUri("tpp nok redirect uri");
        tppInfoEntity.setRedirectUri("tpp ok redirect uri");

        return tppInfoEntity;
    }

    private PsuData buildPsuData() {
        PsuIdData psuIdData = buildPsuIdData();
        PsuData psuData = new PsuData(
            psuIdData.getPsuId(),
            psuIdData.getPsuIdType(),
            psuIdData.getPsuCorporateId(),
            psuIdData.getPsuCorporateIdType()
        );
        psuData.setId(1L);

        return psuData;
    }

    private List<PisPaymentData> buildPisPaymentDataList() {
        PisPaymentData pisPaymentData = new PisPaymentData();
        pisPaymentData.setPaymentId(PAYMENT_ID);
        pisPaymentData.setPaymentData(buildPisCommonPaymentData());
        pisPaymentData.setDebtorAccount(buildAccountReference());
        pisPaymentData.setCreditorAccount(buildAccountReference());
        pisPaymentData.setAmount(new BigDecimal("1000"));
        pisPaymentData.setCurrency(Currency.getInstance("EUR"));

        return Collections.singletonList(pisPaymentData);
    }

    private List<PisPaymentData> buildPisPaymentDataListForCommonData() {
        PisPaymentData pisPaymentData = new PisPaymentData();
        pisPaymentData.setPaymentId(PAYMENT_ID);
        pisPaymentData.setDebtorAccount(buildAccountReference());
        pisPaymentData.setCreditorAccount(buildAccountReference());
        pisPaymentData.setAmount(new BigDecimal("1000"));
        pisPaymentData.setCurrency(Currency.getInstance("EUR"));

        return Collections.singletonList(pisPaymentData);
    }

    private AccountReferenceEntity buildAccountReference() {
        AccountReferenceEntity pisAccountReference = new AccountReferenceEntity();
        pisAccountReference.setIban("iban");
        pisAccountReference.setCurrency(Currency.getInstance("EUR"));

        return pisAccountReference;
    }

    private CmsPayment buildCmsPayment() {
        CmsSinglePayment cmsPayment = new CmsSinglePayment(PAYMENT_PRODUCT);
        cmsPayment.setPaymentId(PAYMENT_ID);

        return cmsPayment;
    }

    private PisAuthorization buildFinalisedAuthorisation() {
        PisAuthorization pisAuthorisation = buildFinalisedAuthorisationNoPsuData();
        pisAuthorisation.setPsuData(buildPsuData());

        return pisAuthorisation;
    }

    private PisAuthorization buildFinalisedAuthorisationNoPsuData() {
        PisAuthorization pisAuthorisation = new PisAuthorization();
        pisAuthorisation.setScaStatus(ScaStatus.FINALISED);
        pisAuthorisation.setAuthorizationType(CmsAuthorisationType.CREATED);
        pisAuthorisation.setPaymentData(buildPisCommonPaymentData());
        pisAuthorisation.setExternalId(AUTHORISATION_ID);

        return pisAuthorisation;
    }

    private PisAuthorization buildExpiredAuthorisation() {
        PisAuthorization pisAuthorisation = new PisAuthorization();
        pisAuthorisation.setScaStatus(ScaStatus.RECEIVED);
        pisAuthorisation.setAuthorizationType(CmsAuthorisationType.CREATED);
        pisAuthorisation.setPaymentData(buildPisCommonPaymentData());
        pisAuthorisation.setExternalId(EXPIRED_AUTHORISATION_ID);
        pisAuthorisation.setPsuData(buildPsuData());
        pisAuthorisation.setRedirectUrlExpirationTimestamp(OffsetDateTime.now().minusDays(1));
        pisAuthorisation.setAuthorisationExpirationTimestamp(OffsetDateTime.now().plusHours(1));

        return pisAuthorisation;
    }
}


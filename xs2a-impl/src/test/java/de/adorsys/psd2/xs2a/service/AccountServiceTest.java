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

package de.adorsys.psd2.xs2a.service;

import de.adorsys.psd2.consent.api.ActionStatus;
import de.adorsys.psd2.xs2a.core.ais.BookingStatus;
import de.adorsys.psd2.xs2a.core.consent.AisConsentRequestType;
import de.adorsys.psd2.xs2a.core.consent.ConsentStatus;
import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.event.EventType;
import de.adorsys.psd2.xs2a.core.profile.AccountReference;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.ResponseObject;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.domain.Transactions;
import de.adorsys.psd2.xs2a.domain.account.*;
import de.adorsys.psd2.xs2a.domain.consent.AccountConsent;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aAccountAccess;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.consent.AccountReferenceInConsentUpdater;
import de.adorsys.psd2.xs2a.service.consent.Xs2aAisConsentService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.event.Xs2aEventService;
import de.adorsys.psd2.xs2a.service.mapper.consent.Xs2aAisConsentMapper;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.*;
import de.adorsys.psd2.xs2a.service.profile.AspspProfileServiceWrapper;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import de.adorsys.psd2.xs2a.service.validator.ValueValidatorService;
import de.adorsys.psd2.xs2a.service.validator.ais.account.*;
import de.adorsys.psd2.xs2a.service.validator.ais.account.dto.*;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.account.*;
import de.adorsys.psd2.xs2a.spi.domain.psu.SpiPsuData;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponseStatus;
import de.adorsys.psd2.xs2a.spi.service.AccountSpi;
import de.adorsys.psd2.xs2a.util.reader.JsonReader;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static de.adorsys.psd2.xs2a.domain.TppMessageInformation.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AccountServiceTest {
    private static final String ASPSP_ACCOUNT_ID = "3278921mxl-n2131-13nw";
    private static final boolean WITH_BALANCE = false;
    private static final String CONSENT_ID = "Test consentId";
    private static final String ACCOUNT_ID = UUID.randomUUID().toString();
    private static final String TRANSACTION_ID = "Test transactionId";
    private static final String IBAN = "Test IBAN";
    private static final String BBAN = "Test BBAN";
    private static final String PAN = "Test PAN";
    private static final String MASKED_PAN = "Test MASKED_PAN";
    private static final String MSISDN = "Test MSISDN";
    private static final String REQUEST_URI = "request/uri";
    private static final Currency EUR_CURRENCY = Currency.getInstance("EUR");
    private static final LocalDate DATE_FROM = LocalDate.of(2018, 1, 1);
    private static final LocalDate DATE_TO = LocalDate.now();
    private static final SpiResponseStatus LOGICAL_FAILURE_RESPONSE_STATUS = SpiResponseStatus.LOGICAL_FAILURE;
    private static final MessageErrorCode FORMAT_ERROR_CODE = MessageErrorCode.FORMAT_ERROR;
    private static final MessageErrorCode CONSENT_INVALID_MESSAGE_ERROR_CODE = MessageErrorCode.CONSENT_INVALID;
    private static final MessageError CONSENT_INVALID_MESSAGE_ERROR = new MessageError(ErrorType.AIS_401, of(CONSENT_INVALID_MESSAGE_ERROR_CODE));
    private static final SpiAccountConsent SPI_ACCOUNT_CONSENT = new SpiAccountConsent();
    private static final List<SpiAccountDetails> EMPTY_ACCOUNT_DETAILS_LIST = Collections.emptyList();
    private static final SpiAccountReference SPI_ACCOUNT_REFERENCE = buildSpiAccountReference();
    private static final SpiAccountReference SPI_ACCOUNT_REFERENCE_GLOBAL = buildSpiAccountReferenceGlobal();
    private static final AccountReference XS2A_ACCOUNT_REFERENCE = buildXs2aAccountReference();
    private static final AccountReference XS2A_ACCOUNT_REFERENCE_WITHOUT_ASPSP_IDS = buildXs2aAccountReferenceWithoutAspspIds();
    private static final SpiTransactionReport SPI_TRANSACTION_REPORT = buildSpiTransactionReport();
    private static final SpiContextData SPI_CONTEXT_DATA = new SpiContextData(new SpiPsuData(null, null, null, null), new TppInfo(), UUID.randomUUID());
    private static final BookingStatus BOOKING_STATUS = BookingStatus.BOTH;
    private static final MessageError VALIDATION_ERROR =
        new MessageError(ErrorType.AIS_401, TppMessageInformation.of(MessageErrorCode.CONSENT_INVALID));
    private static final String ENTRY_REFERENCE_FROM = "777";
    private static final Boolean DELTA_LIST = Boolean.FALSE;
    private static final Xs2aTransactionsReportByPeriodRequest XS2A_TRANSACTIONS_REPORT_BY_PERIOD_REQUEST = new Xs2aTransactionsReportByPeriodRequest(CONSENT_ID, ACCOUNT_ID, MediaType.APPLICATION_JSON_VALUE, WITH_BALANCE, DATE_FROM, DATE_TO, BOOKING_STATUS, REQUEST_URI, ENTRY_REFERENCE_FROM, DELTA_LIST);
    private static final JsonReader jsonReader = new JsonReader();

    @InjectMocks
    private AccountService accountService;

    @Mock
    private AccountSpi accountSpi;
    @Mock
    private SpiToXs2aAccountDetailsMapper accountDetailsMapper;
    @Mock
    private SpiToXs2aBalanceMapper balanceMapper;
    @Mock
    private SpiToXs2aBalanceReportMapper balanceReportMapper;
    @Mock
    private SpiToXs2aAccountReferenceMapper referenceMapper;
    @Mock
    private SpiTransactionListToXs2aAccountReportMapper transactionsToAccountReportMapper;
    @Mock
    private ValueValidatorService validatorService;
    @Mock
    private Xs2aAisConsentService aisConsentService;
    @Mock
    private Xs2aAisConsentMapper consentMapper;
    @Mock
    private TppService tppService;
    @Mock
    private AspspProfileServiceWrapper aspspProfileService;
    @Mock
    private SpiAccountDetails spiAccountDetails;
    @Mock
    private Xs2aAccountDetails xs2aAccountDetails;
    @Mock
    private Xs2aBalancesReport xs2aBalancesReport;
    @Mock
    private SpiTransaction spiTransaction;
    @Mock
    private Xs2aToSpiAccountReferenceMapper xs2aToSpiAccountReferenceMapper;
    @Mock
    private Xs2aEventService xs2aEventService;
    @Mock
    private SpiContextDataProvider spiContextDataProvider;
    @Mock
    private AccountReferenceInConsentUpdater accountReferenceUpdater;
    @Mock
    private SpiToXs2aTransactionMapper spiToXs2aTransactionMapper;
    @Mock
    private Transactions transactions;
    @Mock
    private SpiErrorMapper spiErrorMapper;
    @Mock
    private GetAccountListValidator getAccountListValidator;
    @Mock
    private GetAccountDetailsValidator getAccountDetailsValidator;
    @Mock
    private GetBalancesReportValidator getBalancesReportValidator;
    @Mock
    private GetTransactionsReportValidator getTransactionsReportValidator;
    @Mock
    private GetTransactionDetailsValidator getTransactionDetailsValidator;
    @Mock
    private RequestProviderService requestProviderService;
    @Mock
    private SpiAspspConsentDataProviderFactory spiAspspConsentDataProviderFactory;

    private AccountConsent accountConsent;

    @Before
    public void setUp() {
        accountConsent = createConsent(CONSENT_ID, createAccountAccess(XS2A_ACCOUNT_REFERENCE));

        when(spiContextDataProvider.provideWithPsuIdData(any()))
            .thenReturn(SPI_CONTEXT_DATA);
        when(getAccountListValidator.validate(any(GetAccountListConsentObject.class)))
            .thenReturn(ValidationResult.valid());
        when(getAccountDetailsValidator.validate(any(CommonAccountRequestObject.class)))
            .thenReturn(ValidationResult.valid());
        when(getBalancesReportValidator.validate(any(CommonAccountBalanceRequestObject.class)))
            .thenReturn(ValidationResult.valid());
        when(getTransactionsReportValidator.validate(any(TransactionsReportByPeriodObject.class)))
            .thenReturn(ValidationResult.valid());
        when(getTransactionDetailsValidator.validate(any(CommonAccountTransactionsRequestObject.class)))
            .thenReturn(ValidationResult.valid());
        when(requestProviderService.getRequestId()).thenReturn(UUID.randomUUID());
        when(aisConsentService.getAccountConsentById(CONSENT_ID))
            .thenReturn(Optional.of(accountConsent));
    }

    @Test
    public void getAccountDetailsList_Failure_AllowedAccountDataHasError() {
        when(getAccountListValidator.validate(new GetAccountListConsentObject(accountConsent, WITH_BALANCE, REQUEST_URI)))
            .thenReturn(ValidationResult.invalid(CONSENT_INVALID_MESSAGE_ERROR));

        ResponseObject<Xs2aAccountListHolder> actualResponse = accountService.getAccountList(CONSENT_ID, WITH_BALANCE, REQUEST_URI);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getError()).isEqualTo(CONSENT_INVALID_MESSAGE_ERROR);
    }

    @Test
    public void getAccountDetailsList_Failure_SpiResponseHasError() {
        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);

        when(accountSpi.requestAccountList(SPI_CONTEXT_DATA, WITH_BALANCE, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildErrorSpiResponse(EMPTY_ACCOUNT_DETAILS_LIST));

        when(spiErrorMapper.mapToErrorHolder(buildErrorSpiResponse(EMPTY_ACCOUNT_DETAILS_LIST), ServiceType.AIS))
            .thenReturn(ErrorHolder.builder(FORMAT_ERROR_CODE).errorType(ErrorType.AIS_400).build());

        ResponseObject<Xs2aAccountListHolder> actualResponse = accountService.getAccountList(CONSENT_ID, WITH_BALANCE, REQUEST_URI);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();

        TppMessageInformation tppMessage = actualResponse.getError().getTppMessage();

        assertThat(tppMessage).isNotNull();
        assertThat(tppMessage.getMessageErrorCode()).isEqualTo(FORMAT_ERROR_CODE);
    }

    @Test
    public void getAccountDetailsList_Success() {
        // Given
        List<SpiAccountDetails> spiAccountDetailsList = Collections.singletonList(spiAccountDetails);

        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);

        when(accountSpi.requestAccountList(SPI_CONTEXT_DATA, WITH_BALANCE, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildSuccessSpiResponse(spiAccountDetailsList));

        List<Xs2aAccountDetails> xs2aAccountDetailsList = Collections.singletonList(xs2aAccountDetails);

        when(accountDetailsMapper.mapToXs2aAccountDetailsList(spiAccountDetailsList))
            .thenReturn(xs2aAccountDetailsList);

        when(accountReferenceUpdater.updateAccountReferences(eq(CONSENT_ID), any(), anyList())).thenReturn(Optional.of(accountConsent));

        // When
        ResponseObject<Xs2aAccountListHolder> actualResponse = accountService.getAccountList(CONSENT_ID, WITH_BALANCE, REQUEST_URI);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isFalse();

        Xs2aAccountListHolder body = actualResponse.getBody();

        assertThat(CollectionUtils.isNotEmpty(body.getAccountDetails())).isTrue();

        List<Xs2aAccountDetails> accountDetailsList = body.getAccountDetails();

        assertThat(CollectionUtils.isNotEmpty(accountDetailsList)).isTrue();
        assertThat(CollectionUtils.isEqualCollection(accountDetailsList, xs2aAccountDetailsList)).isTrue();
    }

    @Test
    public void getAccountDetailsList_shouldUpdateAccountReferences() {
        // Given
        AccountConsent accountConsent = createConsent(CONSENT_ID, createAccountAccess(XS2A_ACCOUNT_REFERENCE_WITHOUT_ASPSP_IDS));

        when(aisConsentService.getAccountConsentById(CONSENT_ID))
            .thenReturn(Optional.of(accountConsent));

        List<SpiAccountDetails> spiAccountDetailsList = Collections.singletonList(spiAccountDetails);

        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);

        when(accountSpi.requestAccountList(SPI_CONTEXT_DATA, WITH_BALANCE, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildSuccessSpiResponse(spiAccountDetailsList));

        List<Xs2aAccountDetails> xs2aAccountDetailsList = Collections.singletonList(xs2aAccountDetails);

        when(accountDetailsMapper.mapToXs2aAccountDetailsList(spiAccountDetailsList))
            .thenReturn(xs2aAccountDetailsList);

        AccountConsent updatedAccountConsent = createConsent(CONSENT_ID, createAccountAccess(XS2A_ACCOUNT_REFERENCE));
        when(accountReferenceUpdater.updateAccountReferences(CONSENT_ID, accountConsent.getAccess(), xs2aAccountDetailsList))
            .thenReturn(Optional.of(updatedAccountConsent));

        // When
        ResponseObject<Xs2aAccountListHolder> actualResponse = accountService.getAccountList(CONSENT_ID, WITH_BALANCE, REQUEST_URI);

        // Then
        Xs2aAccountListHolder responseBody = actualResponse.getBody();
        assertThat(responseBody.getAccountDetails()).isEqualTo(xs2aAccountDetailsList);

        verify(accountReferenceUpdater).updateAccountReferences(CONSENT_ID, accountConsent.getAccess(), xs2aAccountDetailsList);
        assertThat(responseBody.getAccountConsent()).isEqualTo(updatedAccountConsent);
    }

    @Test
    public void getAccountList_Success_ShouldRecordEvent() {
        // Given
        List<SpiAccountDetails> spiAccountDetailsList = Collections.singletonList(spiAccountDetails);
        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);
        when(accountSpi.requestAccountList(SPI_CONTEXT_DATA, WITH_BALANCE, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildSuccessSpiResponse(spiAccountDetailsList));
        List<Xs2aAccountDetails> xs2aAccountDetailsList = Collections.singletonList(xs2aAccountDetails);
        when(accountDetailsMapper.mapToXs2aAccountDetailsList(spiAccountDetailsList))
            .thenReturn(xs2aAccountDetailsList);
        when(accountReferenceUpdater.updateAccountReferences(eq(CONSENT_ID), any(), anyList()))
            .thenReturn(Optional.of(accountConsent));
        ArgumentCaptor<EventType> argumentCaptor = ArgumentCaptor.forClass(EventType.class);

        // When
        accountService.getAccountList(CONSENT_ID, WITH_BALANCE, REQUEST_URI);

        // Then
        verify(xs2aEventService, times(1)).recordAisTppRequest(eq(CONSENT_ID), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo(EventType.READ_ACCOUNT_LIST_REQUEST_RECEIVED);
    }

    @Test
    public void getAccountList_withInvalidConsent_shouldReturnValidationError() {
        // Given
        when(getAccountListValidator.validate(any(GetAccountListConsentObject.class)))
            .thenReturn(ValidationResult.invalid(VALIDATION_ERROR));

        // When
        ResponseObject<Xs2aAccountListHolder> actualResponse = accountService.getAccountList(CONSENT_ID, WITH_BALANCE, REQUEST_URI);

        // Then
        verify(getAccountListValidator).validate(new GetAccountListConsentObject(accountConsent, WITH_BALANCE, REQUEST_URI));
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getError()).isEqualTo(VALIDATION_ERROR);
    }

    @Test
    public void getAccountDetails_Failure_AllowedAccountDataHasError() {
        // Given
        when(getAccountDetailsValidator.validate(new CommonAccountRequestObject(accountConsent, ACCOUNT_ID, WITH_BALANCE, REQUEST_URI)))
            .thenReturn(ValidationResult.invalid(CONSENT_INVALID_MESSAGE_ERROR));

        // When
        ResponseObject<Xs2aAccountDetailsHolder> actualResponse = accountService.getAccountDetails(CONSENT_ID, ACCOUNT_ID, WITH_BALANCE, REQUEST_URI);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getError()).isEqualTo(CONSENT_INVALID_MESSAGE_ERROR);
    }

    @Test
    public void getAccountDetails_Failure_SpiResponseHasError() {
        // Given
        when(accountSpi.requestAccountDetailForAccount(SPI_CONTEXT_DATA, WITH_BALANCE, SPI_ACCOUNT_REFERENCE, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildErrorSpiResponse(spiAccountDetails));

        when(xs2aToSpiAccountReferenceMapper.mapToSpiAccountReference(XS2A_ACCOUNT_REFERENCE))
            .thenReturn(SPI_ACCOUNT_REFERENCE);

        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);

        when(spiErrorMapper.mapToErrorHolder(buildErrorSpiResponse(spiAccountDetails), ServiceType.AIS))
            .thenReturn(ErrorHolder.builder(FORMAT_ERROR_CODE).errorType(ErrorType.AIS_400).build());

        // When
        ResponseObject<Xs2aAccountDetailsHolder> actualResponse = accountService.getAccountDetails(CONSENT_ID, ACCOUNT_ID, WITH_BALANCE, REQUEST_URI);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();

        TppMessageInformation tppMessage = actualResponse.getError().getTppMessage();

        assertThat(tppMessage).isNotNull();
        assertThat(tppMessage.getMessageErrorCode()).isEqualTo(FORMAT_ERROR_CODE);
    }

    @Test
    public void getAccountDetails_failure_accountReferenceNotFoundInAccountAccess() {
        // Given
        when(getAccountDetailsValidator.validate(new CommonAccountRequestObject(accountConsent, ACCOUNT_ID, WITH_BALANCE, REQUEST_URI)))
            .thenReturn(ValidationResult.invalid(CONSENT_INVALID_MESSAGE_ERROR));

        // When
        ResponseObject<Xs2aAccountDetailsHolder> actualResponse = accountService.getAccountDetails(CONSENT_ID, ACCOUNT_ID, WITH_BALANCE, REQUEST_URI);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getError()).isEqualTo(CONSENT_INVALID_MESSAGE_ERROR);
    }

    @Test
    public void getAccountDetails_Success() {
        // Given
        when(accountSpi.requestAccountDetailForAccount(SPI_CONTEXT_DATA, WITH_BALANCE, SPI_ACCOUNT_REFERENCE, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildSuccessSpiResponse(spiAccountDetails));

        when(xs2aToSpiAccountReferenceMapper.mapToSpiAccountReference(XS2A_ACCOUNT_REFERENCE))
            .thenReturn(SPI_ACCOUNT_REFERENCE);

        when(accountDetailsMapper.mapToXs2aAccountDetails(spiAccountDetails))
            .thenReturn(xs2aAccountDetails);

        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);

        // When
        ResponseObject<Xs2aAccountDetailsHolder> actualResponse = accountService.getAccountDetails(CONSENT_ID, ACCOUNT_ID, WITH_BALANCE, REQUEST_URI);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isFalse();

        Xs2aAccountDetails body = actualResponse.getBody().getAccountDetails();

        assertThat(body).isNotNull();
        assertThat(body).isEqualTo(xs2aAccountDetails);
    }

    @Test
    public void getAccountDetails_Success_ShouldRecordEvent() {
        // Given
        when(accountSpi.requestAccountDetailForAccount(SPI_CONTEXT_DATA, WITH_BALANCE, SPI_ACCOUNT_REFERENCE, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildSuccessSpiResponse(spiAccountDetails));
        when(xs2aToSpiAccountReferenceMapper.mapToSpiAccountReference(XS2A_ACCOUNT_REFERENCE))
            .thenReturn(SPI_ACCOUNT_REFERENCE);
        when(accountDetailsMapper.mapToXs2aAccountDetails(spiAccountDetails))
            .thenReturn(xs2aAccountDetails);
        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);
        ArgumentCaptor<EventType> argumentCaptor = ArgumentCaptor.forClass(EventType.class);

        // When
        accountService.getAccountDetails(CONSENT_ID, ACCOUNT_ID, WITH_BALANCE, REQUEST_URI);

        // Then
        verify(xs2aEventService, times(1)).recordAisTppRequest(eq(CONSENT_ID), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo(EventType.READ_ACCOUNT_DETAILS_REQUEST_RECEIVED);
    }

    @Test
    public void getAccountDetails_withInvalidConsent_shouldReturnValidationError() {
        // Given
        when(getAccountDetailsValidator.validate(any(CommonAccountRequestObject.class)))
            .thenReturn(ValidationResult.invalid(VALIDATION_ERROR));

        // When
        ResponseObject<Xs2aAccountDetailsHolder> actualResponse = accountService.getAccountDetails(CONSENT_ID, ACCOUNT_ID, WITH_BALANCE, REQUEST_URI);

        // Then
        verify(getAccountDetailsValidator).validate(new CommonAccountRequestObject(accountConsent, ACCOUNT_ID, WITH_BALANCE, REQUEST_URI));
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getError()).isEqualTo(VALIDATION_ERROR);
    }

    @Test
    public void getBalancesReport_Failure_AllowedAccountDataHasError() {
        // Given
        when(getBalancesReportValidator.validate(new CommonAccountBalanceRequestObject(accountConsent, ACCOUNT_ID, REQUEST_URI)))
            .thenReturn(ValidationResult.invalid(CONSENT_INVALID_MESSAGE_ERROR));

        // When
        ResponseObject<Xs2aBalancesReport> actualResponse = accountService.getBalancesReport(CONSENT_ID, ACCOUNT_ID, REQUEST_URI);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getError()).isEqualTo(CONSENT_INVALID_MESSAGE_ERROR);
    }

    @Test
    public void getBalancesReport_Failure_SpiResponseHasError() {
        // Given
        when(xs2aToSpiAccountReferenceMapper.mapToSpiAccountReference(XS2A_ACCOUNT_REFERENCE))
            .thenReturn(SPI_ACCOUNT_REFERENCE);

        when(accountSpi.requestBalancesForAccount(SPI_CONTEXT_DATA, SPI_ACCOUNT_REFERENCE, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildErrorSpiResponse(Collections.emptyList()));

        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);

        when(spiErrorMapper.mapToErrorHolder(buildErrorSpiResponse(Collections.EMPTY_LIST), ServiceType.AIS))
            .thenReturn(ErrorHolder.builder(FORMAT_ERROR_CODE).errorType(ErrorType.AIS_400).build());

        // When
        ResponseObject<Xs2aBalancesReport> actualResponse = accountService.getBalancesReport(CONSENT_ID, ACCOUNT_ID, REQUEST_URI);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();

        TppMessageInformation tppMessage = actualResponse.getError().getTppMessage();

        assertThat(tppMessage).isNotNull();
        assertThat(tppMessage.getMessageErrorCode()).isEqualTo(FORMAT_ERROR_CODE);
    }

    @Test
    public void getBalancesReport_Failure_ConsentNotContainsAccountReference() {
        // Given
        when(getBalancesReportValidator.validate(new CommonAccountBalanceRequestObject(accountConsent, ACCOUNT_ID, REQUEST_URI)))
            .thenReturn(ValidationResult.invalid(CONSENT_INVALID_MESSAGE_ERROR));

        // When
        ResponseObject<Xs2aBalancesReport> actualResponse = accountService.getBalancesReport(CONSENT_ID, ACCOUNT_ID, REQUEST_URI);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getError()).isEqualTo(CONSENT_INVALID_MESSAGE_ERROR);
    }

    @Test
    public void getBalancesReport_Success() {
        // Given
        when(accountSpi.requestBalancesForAccount(SPI_CONTEXT_DATA, SPI_ACCOUNT_REFERENCE, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildSuccessSpiResponse(Collections.emptyList()));

        when(balanceReportMapper.mapToXs2aBalancesReport(SPI_ACCOUNT_REFERENCE, Collections.emptyList()))
            .thenReturn(xs2aBalancesReport);

        when(xs2aBalancesReport.getXs2aAccountReference())
            .thenReturn(XS2A_ACCOUNT_REFERENCE);

        when(xs2aToSpiAccountReferenceMapper.mapToSpiAccountReference(XS2A_ACCOUNT_REFERENCE))
            .thenReturn(SPI_ACCOUNT_REFERENCE);

        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);

        ResponseObject<Xs2aBalancesReport> actualResponse = accountService.getBalancesReport(CONSENT_ID, ACCOUNT_ID, REQUEST_URI);

        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isFalse();

        Xs2aBalancesReport body = actualResponse.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getXs2aAccountReference()).isEqualTo(XS2A_ACCOUNT_REFERENCE);
    }

    @Test
    public void getBalancesReport_Success_ShouldRecordEvent() {
        // Given
        when(accountSpi.requestBalancesForAccount(SPI_CONTEXT_DATA, SPI_ACCOUNT_REFERENCE, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildSuccessSpiResponse(Collections.emptyList()));
        when(balanceReportMapper.mapToXs2aBalancesReport(SPI_ACCOUNT_REFERENCE, Collections.emptyList()))
            .thenReturn(xs2aBalancesReport);
        when(xs2aToSpiAccountReferenceMapper.mapToSpiAccountReference(XS2A_ACCOUNT_REFERENCE))
            .thenReturn(SPI_ACCOUNT_REFERENCE);
        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);
        ArgumentCaptor<EventType> argumentCaptor = ArgumentCaptor.forClass(EventType.class);

        // When
        accountService.getBalancesReport(CONSENT_ID, ACCOUNT_ID, REQUEST_URI);

        // Then
        verify(xs2aEventService, times(1)).recordAisTppRequest(eq(CONSENT_ID), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo(EventType.READ_BALANCE_REQUEST_RECEIVED);
    }

    @Test
    public void getBalancesReport_withInvalidConsent_shouldReturnValidationError() {
        // Given
        when(getBalancesReportValidator.validate(any(CommonAccountBalanceRequestObject.class)))
            .thenReturn(ValidationResult.invalid(VALIDATION_ERROR));

        // When
        ResponseObject<Xs2aBalancesReport> actualResponse = accountService.getBalancesReport(CONSENT_ID, ACCOUNT_ID, REQUEST_URI);

        // Then
        verify(getBalancesReportValidator).validate(new CommonAccountBalanceRequestObject(accountConsent, ACCOUNT_ID, REQUEST_URI));
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getError()).isEqualTo(VALIDATION_ERROR);
    }

    @Test
    public void getTransactionsReportByPeriod_Failure_AllowedAccountDataHasError() {
        // Given
        when(getTransactionsReportValidator.validate(new TransactionsReportByPeriodObject(accountConsent, ACCOUNT_ID, WITH_BALANCE, REQUEST_URI, ENTRY_REFERENCE_FROM, DELTA_LIST, MediaType.APPLICATION_JSON_VALUE, BOOKING_STATUS)))
            .thenReturn(ValidationResult.invalid(CONSENT_INVALID_MESSAGE_ERROR));

        // When
        ResponseObject<Xs2aTransactionsReport> actualResponse = accountService.getTransactionsReportByPeriod(XS2A_TRANSACTIONS_REPORT_BY_PERIOD_REQUEST);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getError()).isEqualTo(CONSENT_INVALID_MESSAGE_ERROR);
    }

    @Test
    public void getTransactionsReportByPeriod_Failure_SpiResponseHasError() {
        // Given
        doNothing().when(validatorService).validateAccountIdPeriod(ACCOUNT_ID, DATE_FROM, DATE_TO);

        when(aspspProfileService.isTransactionsWithoutBalancesSupported())
            .thenReturn(true);

        when(xs2aToSpiAccountReferenceMapper.mapToSpiAccountReference(XS2A_ACCOUNT_REFERENCE))
            .thenReturn(SPI_ACCOUNT_REFERENCE);

        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);

        when(accountSpi.requestTransactionsForAccount(SPI_CONTEXT_DATA, MediaType.APPLICATION_JSON_VALUE, WITH_BALANCE, DATE_FROM, DATE_TO, BOOKING_STATUS, SPI_ACCOUNT_REFERENCE, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildErrorSpiResponse(SPI_TRANSACTION_REPORT));

        when(spiErrorMapper.mapToErrorHolder(buildErrorSpiResponse(SPI_TRANSACTION_REPORT), ServiceType.AIS))
            .thenReturn(ErrorHolder.builder(FORMAT_ERROR_CODE).errorType(ErrorType.AIS_400).build());

        // When
        ResponseObject<Xs2aTransactionsReport> actualResponse = accountService.getTransactionsReportByPeriod(XS2A_TRANSACTIONS_REPORT_BY_PERIOD_REQUEST);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();

        TppMessageInformation tppMessage = actualResponse.getError().getTppMessage();

        assertThat(tppMessage).isNotNull();
        assertThat(tppMessage.getMessageErrorCode()).isEqualTo(FORMAT_ERROR_CODE);
    }

    @Test
    public void getTransactionsReportByPeriod_failure_accountReferenceNotFoundInAccountAccess() {
        // Given
        when(getTransactionsReportValidator.validate(new TransactionsReportByPeriodObject(accountConsent, ACCOUNT_ID, WITH_BALANCE, REQUEST_URI, ENTRY_REFERENCE_FROM, DELTA_LIST, MediaType.APPLICATION_JSON_VALUE, BOOKING_STATUS)))
            .thenReturn(ValidationResult.invalid(CONSENT_INVALID_MESSAGE_ERROR));

        // When
        ResponseObject<Xs2aTransactionsReport> actualResponse = accountService.getTransactionsReportByPeriod(XS2A_TRANSACTIONS_REPORT_BY_PERIOD_REQUEST);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getError()).isEqualTo(CONSENT_INVALID_MESSAGE_ERROR);
    }

    @Test
    public void getTransactionsReportByPeriod_Success() {
        // Given
        doNothing().when(validatorService).validateAccountIdPeriod(ACCOUNT_ID, DATE_FROM, DATE_TO);

        when(aspspProfileService.isTransactionsWithoutBalancesSupported())
            .thenReturn(true);

        when(accountSpi.requestTransactionsForAccount(SPI_CONTEXT_DATA, MediaType.APPLICATION_JSON_VALUE, WITH_BALANCE, DATE_FROM, DATE_TO, BOOKING_STATUS, SPI_ACCOUNT_REFERENCE, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildSuccessSpiResponse(SPI_TRANSACTION_REPORT));

        Xs2aAccountReport xs2aAccountReport = new Xs2aAccountReport(Collections.emptyList(), Collections.emptyList(), null);

        when(transactionsToAccountReportMapper.mapToXs2aAccountReport(Collections.emptyList(), null))
            .thenReturn(Optional.of(xs2aAccountReport));

        when(referenceMapper.mapToXs2aAccountReference(SPI_ACCOUNT_REFERENCE))
            .thenReturn(XS2A_ACCOUNT_REFERENCE);

        when(xs2aToSpiAccountReferenceMapper.mapToSpiAccountReference(XS2A_ACCOUNT_REFERENCE))
            .thenReturn(SPI_ACCOUNT_REFERENCE);

        when(balanceMapper.mapToXs2aBalanceList(Collections.emptyList()))
            .thenReturn(Collections.emptyList());

        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);

        // When
        ResponseObject<Xs2aTransactionsReport> actualResponse = accountService.getTransactionsReportByPeriod(XS2A_TRANSACTIONS_REPORT_BY_PERIOD_REQUEST);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isFalse();

        Xs2aTransactionsReport body = actualResponse.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getAccountReport()).isEqualTo(xs2aAccountReport);
        assertThat(body.getAccountReference()).isEqualTo(XS2A_ACCOUNT_REFERENCE);
        assertThat(CollectionUtils.isEqualCollection(body.getBalances(), Collections.emptyList())).isTrue();
    }

    @Test
    public void getTransactionsReportByPeriod_WhenConsentIsGlobal_Success() {
        // Given
        Xs2aAccountAccess xs2aAccountAccess = jsonReader.getObjectFromFile("json/service/validator/ais/account/xs2a-account-access-global.json", Xs2aAccountAccess.class);

        AccountConsent accountConsent = createConsent(CONSENT_ID, xs2aAccountAccess);

        when(aisConsentService.getAccountConsentById(CONSENT_ID))
            .thenReturn(Optional.of(accountConsent));

        doNothing()
            .when(validatorService).validateAccountIdPeriod(ACCOUNT_ID, DATE_FROM, DATE_TO);

        when(aspspProfileService.isTransactionsWithoutBalancesSupported())
            .thenReturn(true);

        when(accountSpi.requestTransactionsForAccount(SPI_CONTEXT_DATA, MediaType.APPLICATION_JSON_VALUE, WITH_BALANCE, DATE_FROM, DATE_TO, BOOKING_STATUS, SPI_ACCOUNT_REFERENCE_GLOBAL, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildSuccessSpiResponse(SPI_TRANSACTION_REPORT));

        Xs2aAccountReport xs2aAccountReport = new Xs2aAccountReport(Collections.emptyList(), Collections.emptyList(), null);

        when(transactionsToAccountReportMapper.mapToXs2aAccountReport(Collections.emptyList(), null))
            .thenReturn(Optional.of(xs2aAccountReport));

        when(referenceMapper.mapToXs2aAccountReference(SPI_ACCOUNT_REFERENCE_GLOBAL))
            .thenReturn(XS2A_ACCOUNT_REFERENCE);

        when(balanceMapper.mapToXs2aBalanceList(Collections.emptyList()))
            .thenReturn(Collections.emptyList());

        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);

        // When
        ResponseObject<Xs2aTransactionsReport> actualResponse = accountService.getTransactionsReportByPeriod(XS2A_TRANSACTIONS_REPORT_BY_PERIOD_REQUEST);

        // Then
        assertNotNull(actualResponse);
        assertThat(actualResponse.hasError()).isFalse();

        Xs2aTransactionsReport body = actualResponse.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getAccountReport()).isEqualTo(xs2aAccountReport);
        assertThat(body.getAccountReference()).isEqualTo(XS2A_ACCOUNT_REFERENCE);
        assertTrue(CollectionUtils.isEqualCollection(body.getBalances(), Collections.emptyList()));
    }

    @Test
    public void getTransactionsReportByPeriod_Success_ShouldRecordEvent() {
        // Given
        doNothing().when(validatorService).validateAccountIdPeriod(ACCOUNT_ID, DATE_FROM, DATE_TO);

        when(aspspProfileService.isTransactionsWithoutBalancesSupported())
            .thenReturn(true);
        when(accountSpi.requestTransactionsForAccount(SPI_CONTEXT_DATA, MediaType.APPLICATION_JSON_VALUE, WITH_BALANCE, DATE_FROM, DATE_TO, BOOKING_STATUS, SPI_ACCOUNT_REFERENCE, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildSuccessSpiResponse(SPI_TRANSACTION_REPORT));
        Xs2aAccountReport xs2aAccountReport = new Xs2aAccountReport(Collections.emptyList(), Collections.emptyList(), null);
        when(transactionsToAccountReportMapper.mapToXs2aAccountReport(Collections.emptyList(), null))
            .thenReturn(Optional.of(xs2aAccountReport));
        when(referenceMapper.mapToXs2aAccountReference(SPI_ACCOUNT_REFERENCE))
            .thenReturn(XS2A_ACCOUNT_REFERENCE);
        when(xs2aToSpiAccountReferenceMapper.mapToSpiAccountReference(XS2A_ACCOUNT_REFERENCE))
            .thenReturn(SPI_ACCOUNT_REFERENCE);
        when(balanceMapper.mapToXs2aBalanceList(Collections.emptyList()))
            .thenReturn(Collections.emptyList());
        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);
        ArgumentCaptor<EventType> argumentCaptor = ArgumentCaptor.forClass(EventType.class);

        // When
        accountService.getTransactionsReportByPeriod(XS2A_TRANSACTIONS_REPORT_BY_PERIOD_REQUEST);

        // Then
        verify(xs2aEventService, times(1)).recordAisTppRequest(eq(CONSENT_ID), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo(EventType.READ_TRANSACTION_LIST_REQUEST_RECEIVED);
    }

    @Test
    public void getTransactionsReportByPeriod_withInvalidConsent_shouldReturnValidationError() {
        // Given
        when(getTransactionsReportValidator.validate(any(TransactionsReportByPeriodObject.class)))
            .thenReturn(ValidationResult.invalid(VALIDATION_ERROR));

        // When
        ResponseObject<Xs2aTransactionsReport> actualResponse = accountService.getTransactionsReportByPeriod(XS2A_TRANSACTIONS_REPORT_BY_PERIOD_REQUEST);

        // Then
        verify(getTransactionsReportValidator).validate(new TransactionsReportByPeriodObject(accountConsent, ACCOUNT_ID, WITH_BALANCE, REQUEST_URI, ENTRY_REFERENCE_FROM, DELTA_LIST, MediaType.APPLICATION_JSON_VALUE, BOOKING_STATUS));
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getError()).isEqualTo(VALIDATION_ERROR);
    }

    @Test
    public void getTransactionDetails_Failure_AllowedAccountDataHasError() {
        // Given
        when(getTransactionDetailsValidator.validate(new CommonAccountTransactionsRequestObject(accountConsent, ACCOUNT_ID, REQUEST_URI)))
            .thenReturn(ValidationResult.invalid(CONSENT_INVALID_MESSAGE_ERROR));

        // When
        ResponseObject<Transactions> actualResponse = accountService.getTransactionDetails(CONSENT_ID, ACCOUNT_ID, TRANSACTION_ID, REQUEST_URI);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getError()).isEqualTo(CONSENT_INVALID_MESSAGE_ERROR);
    }

    @Test
    public void getTransactionDetails_Failure_SpiResponseHasError() {
        // Given
        doNothing().when(validatorService).validateAccountIdTransactionId(ACCOUNT_ID, TRANSACTION_ID);

        when(xs2aToSpiAccountReferenceMapper.mapToSpiAccountReference(XS2A_ACCOUNT_REFERENCE))
            .thenReturn(SPI_ACCOUNT_REFERENCE);

        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);

        when(accountSpi.requestTransactionForAccountByTransactionId(SPI_CONTEXT_DATA, TRANSACTION_ID, SPI_ACCOUNT_REFERENCE, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildErrorSpiResponse(spiTransaction));

        when(spiErrorMapper.mapToErrorHolder(buildErrorSpiResponse(spiTransaction), ServiceType.AIS))
            .thenReturn(ErrorHolder.builder(FORMAT_ERROR_CODE).errorType(ErrorType.AIS_400).build());

        // When
        ResponseObject<Transactions> actualResponse = accountService.getTransactionDetails(CONSENT_ID, ACCOUNT_ID, TRANSACTION_ID, REQUEST_URI);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();

        TppMessageInformation tppMessage = actualResponse.getError().getTppMessage();

        assertThat(tppMessage).isNotNull();
        assertThat(tppMessage.getMessageErrorCode()).isEqualTo(FORMAT_ERROR_CODE);
    }

    @Test
    public void getTransactionDetails_failure_accountReferenceNotFoundInAccountAccess() {
        // Given
        when(getTransactionDetailsValidator.validate(new CommonAccountTransactionsRequestObject(accountConsent, ACCOUNT_ID, REQUEST_URI)))
            .thenReturn(ValidationResult.invalid(CONSENT_INVALID_MESSAGE_ERROR));

        // When
        ResponseObject<Transactions> actualResponse = accountService.getTransactionDetails(CONSENT_ID, ACCOUNT_ID, TRANSACTION_ID, REQUEST_URI);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getError()).isEqualTo(CONSENT_INVALID_MESSAGE_ERROR);
    }

    @Test
    public void getTransactionDetails_Success() {
        // Пшмут
        doNothing()
            .when(validatorService).validateAccountIdTransactionId(ACCOUNT_ID, TRANSACTION_ID);

        when(xs2aToSpiAccountReferenceMapper.mapToSpiAccountReference(XS2A_ACCOUNT_REFERENCE))
            .thenReturn(SPI_ACCOUNT_REFERENCE);

        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);

        when(accountSpi.requestTransactionForAccountByTransactionId(SPI_CONTEXT_DATA, TRANSACTION_ID, SPI_ACCOUNT_REFERENCE, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildSuccessSpiResponse(spiTransaction));

        when(spiToXs2aTransactionMapper.mapToXs2aTransaction(spiTransaction))
            .thenReturn(transactions);

        // When
        ResponseObject<Transactions> actualResponse = accountService.getTransactionDetails(CONSENT_ID, ACCOUNT_ID, TRANSACTION_ID, REQUEST_URI);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isFalse();

        Transactions body = actualResponse.getBody();

        assertThat(body).isEqualTo(transactions);
    }

    @Test
    public void getTransactionDetails_Success_ShouldRecordEvent() {
        // Given
        doNothing().when(validatorService).validateAccountIdTransactionId(ACCOUNT_ID, TRANSACTION_ID);
        when(xs2aToSpiAccountReferenceMapper.mapToSpiAccountReference(XS2A_ACCOUNT_REFERENCE))
            .thenReturn(SPI_ACCOUNT_REFERENCE);
        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);
        when(accountSpi.requestTransactionForAccountByTransactionId(SPI_CONTEXT_DATA, TRANSACTION_ID, SPI_ACCOUNT_REFERENCE, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildSuccessSpiResponse(spiTransaction));
        when(spiToXs2aTransactionMapper.mapToXs2aTransaction(spiTransaction))
            .thenReturn(transactions);
        ArgumentCaptor<EventType> argumentCaptor = ArgumentCaptor.forClass(EventType.class);

        // When
        accountService.getTransactionDetails(CONSENT_ID, ACCOUNT_ID, TRANSACTION_ID, REQUEST_URI);

        // Then
        verify(xs2aEventService, times(1)).recordAisTppRequest(eq(CONSENT_ID), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo(EventType.READ_TRANSACTION_DETAILS_REQUEST_RECEIVED);
    }

    @Test
    public void getTransactionDetails_withInvalidConsent_shouldReturnValidationError() {
        // Given
        when(getTransactionDetailsValidator.validate(any(CommonAccountTransactionsRequestObject.class)))
            .thenReturn(ValidationResult.invalid(VALIDATION_ERROR));

        // When
        ResponseObject<Transactions> actualResponse = accountService.getTransactionDetails(CONSENT_ID, ACCOUNT_ID, TRANSACTION_ID, REQUEST_URI);

        // Then
        verify(getTransactionDetailsValidator).validate(new CommonAccountTransactionsRequestObject(accountConsent, ACCOUNT_ID, REQUEST_URI));
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.hasError()).isTrue();
        assertThat(actualResponse.getError()).isEqualTo(VALIDATION_ERROR);
    }

    @Test
    public void consentActionLog_recurringConsentWithIpAddress_needsToUpdateUsageFalse() {
        // Given
        AccountConsent accountConsent = createConsent(true);
        prepationForGetAccountListRequest(accountConsent);
        when(requestProviderService.isRequestFromTPP()).thenReturn(false);

        // When
        accountService.getAccountList(CONSENT_ID, WITH_BALANCE, REQUEST_URI);

        // Then
        verify(aisConsentService, atLeastOnce()).consentActionLog(null, CONSENT_ID, ActionStatus.SUCCESS, REQUEST_URI, false);
    }

    @Test
    public void consentActionLog_recurringConsentWithoutIpAddress_needsToUpdateUsageTrue() {
        // Given
        AccountConsent accountConsent = createConsent(true);
        prepationForGetAccountListRequest(accountConsent);
        when(requestProviderService.isRequestFromTPP()).thenReturn(true);

        // When
        accountService.getAccountList(CONSENT_ID, WITH_BALANCE, REQUEST_URI);

        // Then
        verify(aisConsentService, atLeastOnce()).consentActionLog(null, CONSENT_ID, ActionStatus.SUCCESS, REQUEST_URI, true);
    }

    @Test
    public void consentActionLog_oneOffConsentWithIpAddress_needsToUpdateUsageTrue() {
        // Given
        AccountConsent accountConsent = createConsent(false);
        prepationForGetAccountListRequest(accountConsent);

        // When
        accountService.getAccountList(CONSENT_ID, WITH_BALANCE, REQUEST_URI);

        // Then
        verify(aisConsentService, atLeastOnce()).consentActionLog(null, CONSENT_ID, ActionStatus.SUCCESS, REQUEST_URI, true);
    }

    @Test
    public void consentActionLog_oneOffConsentWithoutIpAddress_needsToUpdateUsageTrue() {
        // Given
        AccountConsent accountConsent = createConsent(false);
        prepationForGetAccountListRequest(accountConsent);

        // When
        accountService.getAccountList(CONSENT_ID, WITH_BALANCE, REQUEST_URI);

        // Then
        verify(aisConsentService, atLeastOnce()).consentActionLog(null, CONSENT_ID, ActionStatus.SUCCESS, REQUEST_URI, true);
    }

    private void prepationForGetAccountListRequest(AccountConsent accountConsent) {
        List<SpiAccountDetails> spiAccountDetailsList = Collections.singletonList(spiAccountDetails);
        when(consentMapper.mapToSpiAccountConsent(any()))
            .thenReturn(SPI_ACCOUNT_CONSENT);
        when(accountSpi.requestAccountList(SPI_CONTEXT_DATA, WITH_BALANCE, SPI_ACCOUNT_CONSENT, spiAspspConsentDataProviderFactory.getSpiAspspDataProviderFor(CONSENT_ID)))
            .thenReturn(buildSuccessSpiResponse(spiAccountDetailsList));
        List<Xs2aAccountDetails> xs2aAccountDetailsList = Collections.singletonList(xs2aAccountDetails);
        when(accountDetailsMapper.mapToXs2aAccountDetailsList(spiAccountDetailsList))
            .thenReturn(xs2aAccountDetailsList);
        when(accountReferenceUpdater.updateAccountReferences(eq(CONSENT_ID), any(), anyList())).thenReturn(Optional.of(accountConsent));
    }

    // Needed because SpiResponse is final, so it's impossible to mock it
    private <T> SpiResponse<T> buildSuccessSpiResponse(T payload) {
        return SpiResponse.<T>builder()
                   .payload(payload)
                   .build();
    }

    // Needed because SpiResponse is final, so it's impossible to mock it
    private <T> SpiResponse<T> buildErrorSpiResponse(T payload) {
        return SpiResponse.<T>builder()
                   .payload(payload)
                   .fail(LOGICAL_FAILURE_RESPONSE_STATUS);
    }

    // Needed because SpiAccountReference is final, so it's impossible to mock it
    private static SpiAccountReference buildSpiAccountReference() {
        return new SpiAccountReference(ASPSP_ACCOUNT_ID, ACCOUNT_ID, IBAN, BBAN, PAN, MASKED_PAN, MSISDN, EUR_CURRENCY);
    }

    private static SpiAccountReference buildSpiAccountReferenceGlobal() {
        return new SpiAccountReference(ACCOUNT_ID, null, null, null, null, null, null);
    }

    private static AccountReference buildXs2aAccountReference() {
        return new AccountReference(ASPSP_ACCOUNT_ID, ACCOUNT_ID, IBAN, BBAN, PAN, MASKED_PAN, MSISDN, EUR_CURRENCY);
    }

    private static AccountReference buildXs2aAccountReferenceWithoutAspspIds() {
        return new AccountReference(null, null, IBAN, BBAN, PAN, MASKED_PAN, MSISDN, EUR_CURRENCY);
    }

    // Needed because SpiTransactionReport is final, so it's impossible to mock it
    private static SpiTransactionReport buildSpiTransactionReport() {
        return new SpiTransactionReport(Collections.emptyList(), Collections.emptyList(), SpiTransactionReport.RESPONSE_TYPE_JSON, null);
    }

    private static AccountConsent createConsent(String id, Xs2aAccountAccess access) {
        return new AccountConsent(id, access, false, LocalDate.now(), 4, null, ConsentStatus.VALID, false, false, null, createTppInfo(), AisConsentRequestType.GLOBAL, false, Collections.emptyList(), OffsetDateTime.now(), Collections.emptyMap());
    }

    private static AccountConsent createConsent(boolean recurringIndicator) {
        String fileName = recurringIndicator
                              ? "json/AccountConsentRecurringIndicatorTrue.json"
                              : "json/AccountConsentRecurringIndicatorFalse.json";
        return jsonReader.getObjectFromFile(fileName, AccountConsent.class);
    }

    private static TppInfo createTppInfo() {
        TppInfo tppInfo = new TppInfo();
        tppInfo.setAuthorisationNumber(UUID.randomUUID().toString());
        return tppInfo;
    }

    private static Xs2aAccountAccess createAccountAccess(AccountReference accountReference) {
        return new Xs2aAccountAccess(Collections.singletonList(accountReference), Collections.singletonList(accountReference), Collections.singletonList(accountReference), null, null, null);
    }

}

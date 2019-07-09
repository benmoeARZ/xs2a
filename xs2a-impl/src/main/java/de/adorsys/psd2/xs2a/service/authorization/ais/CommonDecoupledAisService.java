package de.adorsys.psd2.xs2a.service.authorization.ais;

import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.domain.consent.DecoupledUpdateConsentPsuDataResponse;
import de.adorsys.psd2.xs2a.domain.consent.UpdateConsentPsuDataReq;
import de.adorsys.psd2.xs2a.domain.consent.UpdateConsentPsuDataResponse;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aAuthenticationObject;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiAccountConsent;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorisationDecoupledScaResponse;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.AisConsentSpi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommonDecoupledAisService {
    private final AisConsentSpi aisConsentSpi;
    private final RequestProviderService requestProviderService;
    private final SpiErrorMapper spiErrorMapper;
    private final SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory;
    private final SpiContextDataProvider spiContextDataProvider;

    public UpdateConsentPsuDataResponse proceedDecoupledApproach(UpdateConsentPsuDataReq request, SpiAccountConsent spiAccountConsent, PsuIdData psuData) {
        return proceedDecoupledApproach(request, spiAccountConsent, null, psuData);
    }

    public UpdateConsentPsuDataResponse proceedDecoupledApproach(UpdateConsentPsuDataReq request, SpiAccountConsent spiAccountConsent, String authenticationMethodId, PsuIdData psuData) {
        String consentId = request.getConsentId();
        String authorisationId = request.getAuthorizationId();
        SpiResponse<SpiAuthorisationDecoupledScaResponse> spiResponse = aisConsentSpi.startScaDecoupled(spiContextDataProvider.provideWithPsuIdData(psuData), authorisationId, authenticationMethodId, spiAccountConsent, aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(consentId));

        if (spiResponse.hasError()) {
            MessageError messageError = new MessageError(spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.AIS));
            log.warn("X-Request-ID: [{}], Consent-ID [{}], Authorisation-ID [{}], PSU-ID [{}], Authentication-Method-ID [{}]. Notifies a decoupled app about starting SCA when proceed decoupled approach has failed. Error msg: {}.",
                     requestProviderService.getRequestId(), request.getConsentId(), request.getAuthorizationId(), psuData.getPsuId(), authenticationMethodId, messageError);
            return createFailedResponse(messageError, spiResponse.getMessages(), request);
        }

        UpdateConsentPsuDataResponse response = new DecoupledUpdateConsentPsuDataResponse(ScaStatus.SCAMETHODSELECTED, consentId, authorisationId);
        response.setPsuMessage(spiResponse.getPayload().getPsuMessage());
        response.setChosenScaMethod(buildXs2aAuthenticationObjectForDecoupledApproach(authenticationMethodId));
        return response;
    }

    // Should ONLY be used for switching from Embedded to Decoupled approach during SCA method selection
    private Xs2aAuthenticationObject buildXs2aAuthenticationObjectForDecoupledApproach(String authenticationMethodId) {
        Xs2aAuthenticationObject xs2aAuthenticationObject = new Xs2aAuthenticationObject();
        xs2aAuthenticationObject.setAuthenticationMethodId(authenticationMethodId);
        return xs2aAuthenticationObject;
    }

    private UpdateConsentPsuDataResponse createFailedResponse(MessageError messageError,
                                                              List<String> messages,
                                                              UpdateConsentPsuDataReq updateConsentPsuDataReq) {
        UpdateConsentPsuDataResponse response = new UpdateConsentPsuDataResponse(ScaStatus.FAILED,
                                                                                 updateConsentPsuDataReq.getConsentId(),
                                                                                 updateConsentPsuDataReq.getAuthorizationId());

        response.setMessageError(messageError);
        response.setPsuMessage(buildPsuMessage(messages));
        return response;
    }

    private String buildPsuMessage(List<String> messages) {
        return String.join(", ", messages);
    }

}

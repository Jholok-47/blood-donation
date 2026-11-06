package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.DTO.MlCandidateFeatures;
import com.lifelink.blood_donation.DTO.MlPredictionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GOAL: the ONLY class that talks to the Python model service. Best-effort by design (same policy as
 * best-effort emails): short timeouts, every failure is caught and logged, callers get Optional.empty()
 * and fall back to the rule-based ranking. Never throws into a calling transaction/request.
 */

@Slf4j
@Service
public class MlRankingClient {

    private final RestClient restClient;
    private final boolean enabled;

    public MlRankingClient(
            @Value("${lifelink.ml.enabled:true}") boolean enabled,
            @Value("${lifelink.ml.base-url:http://localhost:8001}") String baseUrl,
            @Value("${lifelink.ml.connect-timeout-ms:500}") int connectTimeoutMs,
            @Value("${lifelink.ml.read-timeout-ms:2000}") int readTimeoutMs) {
        this.enabled = enabled;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public boolean isEnabled() {
        return enabled;
    }

/** One batched call for all candidates; probabilities come back in the same order. */

    public Optional<MlPredictionResponse> predict(List<MlCandidateFeatures> candidates) {
        if (!enabled || candidates.isEmpty()) {
            return Optional.empty();
        }
        try {
            MlPredictionResponse response = restClient.post()
                    .uri("/predict")
                    .body(Map.of("candidates", candidates))
                    .retrieve()
                    .body(MlPredictionResponse.class);
            if (response == null || response.getProbabilities() == null
                    || response.getProbabilities().size() != candidates.size()) {
                log.warn("ML service returned an unusable response; using rule-based ranking");
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (RuntimeException e) {
            log.warn("ML service unavailable ({}); using rule-based ranking", e.getMessage());
            return Optional.empty();
        }
    }

/** Raw /health payload, or empty if the service cannot be reached. */

    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> health() {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(restClient.get().uri("/health").retrieve().body(Map.class));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}

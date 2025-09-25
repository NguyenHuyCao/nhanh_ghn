package com.app84soft.check_in.other_service.nhanh;

import com.app84soft.check_in.dto.nhanh.NhanhProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhAuthService {
    private final NhanhProperties props;
    private volatile String cachedToken;
    private volatile Instant expireAt = Instant.EPOCH;

    public String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(expireAt)) return cachedToken;
        if (props.getStaticToken()==null || props.getStaticToken().isBlank())
            throw new IllegalStateException("Nhanh staticToken is not configured");
        cachedToken = props.getStaticToken();
        expireAt = Instant.now().plusSeconds(props.getTokenCacheSeconds()>0 ? props.getTokenCacheSeconds() : 1800);
        return cachedToken;
    }
}


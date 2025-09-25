package com.app84soft.check_in.other_service.nhanh;

import com.app84soft.check_in.dto.nhanh.NhanhProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhClient {

    private final RestTemplate restTemplate;
    private final NhanhProperties prop;
    private final NhanhAuthService auth; // <-- dùng service lấy token

    private HttpHeaders authHeaders() {
        String token = auth.getAccessToken();
        HttpHeaders h = new HttpHeaders();
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        // Nhanh có nơi nhận "Token", có nơi nhận Bearer – đặt cả 2 cho chắc:
        h.set("Token", token);
        h.setBearerAuth(token);
        h.set("x-nhanh-business-id", String.valueOf(prop.getBusinessId()));
        return h;
    }

    private String url(String path) {
        String b = prop.getBaseUrl();
        if (b.endsWith("/"))
            b = b.substring(0, b.length() - 1);
        if (!path.startsWith("/"))
            path = "/" + path;
        return b + path;
    }

    // ví dụ triển khai chuẩn cho NhanhClient.listOrdersIndex
    public Map<String, Object> listOrdersIndex(Map<String, String> q) throws JsonProcessingException {
        String url = trimSlash(prop.getBaseUrl()) + "/api/order/index";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fromDate", q.get("fromDate"));
        data.put("toDate", q.get("toDate"));
        data.put("page", Integer.parseInt(q.getOrDefault("page", "1")));
        data.put("limit", Integer.parseInt(q.getOrDefault("limit", "20")));
        if (q.containsKey("keyword"))
            data.put("keyword", q.get("keyword"));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("version", "2.0");
        form.add("appId", prop.getAppId());
        form.add("businessId", prop.getBusinessId());
        form.add("accessToken", auth.getAccessToken());
        form.add("data", new ObjectMapper().writeValueAsString(data));

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<Map> resp =
                restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(form, h), Map.class);
        return resp.getBody();
    }
    private static String trimSlash(String u){
        if (u == null) return "";
        return u.endsWith("/") ? u.substring(0, u.length()-1) : u;
    }

}

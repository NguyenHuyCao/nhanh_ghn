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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhanhClient {

    private final RestTemplate restTemplate;
    private final NhanhProperties prop;
    private final NhanhAuthService auth;

    private String url(String path) {
        String b = prop.getBaseUrl();
        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (!path.startsWith("/")) path = "/" + path;
        return b + path;
    }

    public Map<String, Object> listOrdersIndex(Map<String, String> q) throws JsonProcessingException {
        long t0 = System.nanoTime();
        String u = url("/api/order/index");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fromDate", q.get("fromDate"));
        data.put("toDate",   q.get("toDate"));
        data.put("page",     Integer.parseInt(q.getOrDefault("page", "1")));
        data.put("limit",    Integer.parseInt(q.getOrDefault("limit", "20")));
        if (q.containsKey("keyword")) data.put("keyword", q.get("keyword"));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("version", "2.0");
        form.add("appId", prop.getAppId());
        form.add("businessId", prop.getBusinessId());
        form.add("accessToken", auth.getAccessToken());
        form.add("data", new ObjectMapper().writeValueAsString(data));

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));

        log.info("Nhanh.index CALL fromDate={} toDate={} page={} limit={}",
                data.get("fromDate"), data.get("toDate"), data.get("page"), data.get("limit"));

        ResponseEntity<Map> resp =
                restTemplate.exchange(u, HttpMethod.POST, new HttpEntity<>(form, h), Map.class);

        Map<String, Object> body = resp.getBody();
        int size = 0, total = 0, totalPages = 0;
        try {
            Map<String,Object> d = (Map<String,Object>) body.get("data");
            Object ordersObj = d.get("orders") != null ? d.get("orders") : d.get("items");
            if (ordersObj instanceof List<?> l) size = l.size();
            else if (ordersObj instanceof Map<?,?> m) size = m.size();
            total = asInt(d.get("totalRecords"), 0);
            totalPages = asInt(d.get("totalPages"), 0);
        } catch (Exception ignore) {}

        log.info("Nhanh.index DONE in {}ms size={} total={} totalPages={}", ms(t0), size, total, totalPages);
        return body;
    }

    private static int asInt(Object v, int def) {
        try { return v == null ? def : Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return def; }
    }
    private static long ms(long t0){ return Math.round((System.nanoTime()-t0)/1_000_000.0); }
}

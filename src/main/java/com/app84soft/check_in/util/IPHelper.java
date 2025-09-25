package com.app84soft.check_in.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;


@Log4j2
public final class IPHelper {
    public static String getIp(final HttpServletRequest request) {
        try {
            String ip = request.getHeader("X-Forwarded-For");
            if (StringUtils.isNotBlank(ip)) {
                String[] ips = ip.split(",");
                if (ips.length > 0) {
                    ip = ips[0].trim();
                }
            }
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip.trim())) {
                ip = request.getHeader("X-Real-IP");
            }

            //
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip.trim())) {
                ip = request.getHeader("Proxy-Client-IP");
            }

            //
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip.trim())) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }

            //
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip.trim())) {
                ip = request.getRemoteAddr();
            }

            //
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip.trim())) {
                final String id = request.getSession().getId();
            }
            return ip;
        } catch (Exception e) {
            final String id = request.getSession().getId();
            return "";
        }
    }

    public static String getURLBase(final HttpServletRequest request) throws MalformedURLException {
        URL requestURL = new URL(request.getRequestURL().toString());
        String port = requestURL.getPort() == -1 ? "" : ":" + requestURL.getPort();
        return requestURL.getProtocol() + "://" + requestURL.getHost() + port + "/";
    }

}

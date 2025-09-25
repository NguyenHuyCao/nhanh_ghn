package com.app84soft.check_in.util;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public final class Util {
    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    public static boolean validateEmail(String emailStr) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
        return matcher.find();
    }

    private static final Pattern[] inputRegexes = new Pattern[4];

    static {
        inputRegexes[0] = Pattern.compile(".*[A-Z].*");
        inputRegexes[1] = Pattern.compile(".*[a-z].*");
        inputRegexes[2] = Pattern.compile(".*\\d.*");
        inputRegexes[3] = Pattern.compile(".*[`~!@#$%^&*()\\-_=+\\\\|\\[{\\]};:'\",<.>/?].*");
    }

    public static Map<String, String> getUrlInfo(String url) {
        Map<String, String> map = new HashMap<>();
        try {
            String query = new URL(url).getQuery();
            String[] params = query.split("&");
            for (String param : params) {
                String name = param.split("=")[0];
                String value = param.split("=")[1];
                map.put(name, value);
            }
            return map;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static String genUUID() {
        return UUID.randomUUID().toString();
    }

    public static String formatNumber(double d) {
        DecimalFormat formatter = new DecimalFormat("###,###,###,###.###");
        try {
            return formatter.format(d);
        } catch (Exception e) {
            return "0";
        }
    }

    public static String convertDateToString(final Date date, String pattern) {
        return convertDateToString(date, pattern, null);
    }

    public static Date getTopDay(Date date) {
        if (date == null) {
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+7"));
        return calendar.getTime();
    }

    public static Date getBotDay(Date date) {
        if (date == null) {
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+7"));
        return calendar.getTime();
    }

    public static String convertDateToString(final Date date, String pattern, String timezoneId) {
        if (date == null || StringUtils.isBlank(pattern)) {
            return null;
        }
        if (timezoneId == null) {
            timezoneId = "GMT+7";
        } else {
            timezoneId = "GMT" + timezoneId;
        }
        DateFormat df = new SimpleDateFormat(pattern);
        df.setTimeZone(TimeZone.getTimeZone(timezoneId));
        return df.format(date);
    }

    public static Date convertStringToDate(String dateString, String pattern, String timezoneId) {
        if (StringUtils.isBlank(dateString) || StringUtils.isBlank(pattern)) {
            return null;
        }
        DateFormat df = new SimpleDateFormat(pattern);
        if (timezoneId == null) {
            timezoneId = "GMT+7";
        } else {
            timezoneId = "GMT" + timezoneId;
        }
        df.setTimeZone(TimeZone.getTimeZone(timezoneId));
        try {
            return df.parse(dateString);
        } catch (Exception e) {
            return null;
        }
    }

    public static Date convertStringToDate(String dateString, String pattern) {
        return convertStringToDate(dateString, pattern, null);
    }

    public static boolean isMatchingRegex(String input) {
        boolean inputMatches = true;
        for (Pattern inputRegex : inputRegexes) {
            if (!inputRegex.matcher(input).matches()) {
                inputMatches = false;
            }
        }
        return inputMatches;
    }

    public static long getDuration(Date date1, Date date2, TimeUnit unit) {
        long diffInMillies = Math.abs(date1.getTime() - date2.getTime());
        return unit.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }

    //Bỏ dấu tiếng việt
    public static String removeCharacterVn(String input) {
        if (StringUtils.isBlank(input)) {
            return null;
        }
        final Pattern NONLATIN = Pattern.compile("[^\\w.-]");
        final Pattern WHITESPACE = Pattern.compile("[\\s]");
        String nowhitespace = WHITESPACE.matcher(input.replaceAll("đ", "d")).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized)
                .replaceAll("")
                .replaceAll("\"", "");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    public static String objectToString(Object data) {
        if (data == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        try {
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Map<String, Object> objectToMap(Object data) {
        if (data == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        return mapper.convertValue(data, Map.class);
    }

    public static <T> T stringToObject(Class<? extends T> type, String data) {
        if (data == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        try {
            return mapper.readValue(data, type);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> T stringToArray(TypeReference<T> type, String data) {
        if (data == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        try {
            return mapper.readValue(data, type);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String convertStringArrayToString(String[] strArr, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (String str : strArr)
            sb.append(str).append(delimiter);
        return sb.substring(0, sb.length() - 1);
    }

    public static double rounding(double value) {
        DecimalFormat df = new DecimalFormat("#.##");
        return Double.parseDouble(df.format(value));
    }

    public static String randomString(int count, String characters) {
        return RandomStringUtils.random(count, characters);
    }

    public static String randomString(int count) {
        return randomString(count, ALPHA_NUMERIC_STRING);
    }

    public static <T> T convertMapToObject(Map<String, Object> map, Class<T> clazz) {
        try {
            T object = clazz.getDeclaredConstructor().newInstance();  // Tạo đối tượng mới từ class
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true); // Đảm bảo có thể truy cập các trường private
                // Lấy tên trường từ annotation @JsonProperty (nếu có)
                String jsonKey = getJsonPropertyName(field);
                if (map.containsKey(jsonKey)) {
                    Object value = map.get(jsonKey);
                    // Nếu giá trị là một Map và kiểu của trường là một class, chuyển đổi đệ quy
                    if (value instanceof Map && !field.getType().equals(Object.class)) {
                        value = convertMapToObject((Map<String, Object>) value, field.getType());
                    }
                    try {
                        field.set(object, value); // Gán giá trị vào trường
                    } catch (Exception e) {
                        log.info(e.getMessage());
                    }
                }
            }
            return object;
        } catch (Exception e) {
            log.info(e.getMessage());
            return null;
        }
    }

    private static String getJsonPropertyName(Field field) {
        JsonProperty annotation = field.getAnnotation(JsonProperty.class);
        if (annotation != null && !annotation.value().isEmpty()) {
            return annotation.value(); // Trả về giá trị từ @JsonProperty
        }
        return field.getName(); // Trả về tên trường nếu không có annotation
    }

    // Tạo checksum
    public static String hmacUtils(String message, String CHECKSUM_SECRET) {
        final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
        try {
            SecretKeySpec signingKey = new SecretKeySpec(CHECKSUM_SECRET.getBytes(), HMAC_SHA256_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(message.getBytes());
            return new String(Base64.getEncoder().encode(rawHmac));

        } catch (Exception e) {
            return "";
        }
    }
}

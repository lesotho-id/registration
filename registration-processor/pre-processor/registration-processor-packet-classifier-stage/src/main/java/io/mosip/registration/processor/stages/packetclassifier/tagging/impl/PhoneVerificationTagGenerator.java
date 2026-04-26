
package io.mosip.registration.processor.stages.packetclassifier.tagging.impl;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.stages.packetclassifier.dto.FieldDTO;
import io.mosip.registration.processor.stages.packetclassifier.tagging.TagGenerator;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
@Component
public class PhoneVerificationTagGenerator implements TagGenerator {

    private static final Logger LOGGER =
            RegProcessorLogger.getLogger(PhoneVerificationTagGenerator.class);

    @Autowired
    private RegistrationProcessorRestClientService<Object> restClientService;

    @Override
    public Map<String, String> generateTags(String workflowInstanceId,
                                            String registrationId,
                                            String process,
                                            Map<String, FieldDTO> idObjectFieldDTOMap,
                                            Map<String, String> metaInfoMap,
                                            int iteration) {

        Map<String, String> tags = new HashMap<>();

        String idNumber = getValue(idObjectFieldDTOMap, "idNumber");
        String phone = getValue(idObjectFieldDTOMap, "phone");
        String residence = getValue(idObjectFieldDTOMap, "residenceStatus");

        boolean isCitizen = "Citizen".equalsIgnoreCase(residence);

        if (idNumber != null && phone != null) {

            boolean verified = callVerifyApi(idNumber, phone, isCitizen);

            tags.put("is_phone_verified", String.valueOf(verified));
        }

        return tags;
    }

    @Override
    public List<String> getRequiredIdObjectFieldNames() {
        return Arrays.asList("idNumber", "phone", "residenceStatus");
    }
    private boolean callVerifyApi(String idNumber, String phone, boolean isCitizen) {
        try {
            List<String> paramNames = Arrays.asList("idNumber", "phone", "isCitizen");
            List<Object> paramValues = Arrays.asList(idNumber, phone, isCitizen);

            Object response = restClientService.getApi(ApiName.MOBILE_VERIFIER_API, null,paramNames,
                    paramValues,
                    String.class
            );

            LOGGER.info("Verify API response: {}", response);

            if (response == null) {
                throw new RuntimeException("Null response from Mobile Verify API");
            }

            String body = response.toString();

            if (body.contains("\"status\":\"SUCCESS\"")) {
                return true;
            }
            if (body.contains("\"status\":\"FAILURE\"")) {
                return false;
            }
            throw new RuntimeException("Unexpected API response: " + body);

        } catch (Exception e) {
            LOGGER.error("Mobile Verify API failed", e);
            throw new RuntimeException("Mobile verification API failure", e);
        }
    }
    private String getValue(Map<String, FieldDTO> map, String key) {

        if (!map.containsKey(key) || map.get(key) == null) {
            return null;
        }
        String raw = map.get(key).getValue();
        try {
            org.json.JSONArray arr = new org.json.JSONArray(raw);
            return arr.getJSONObject(0).getString("value");
        } catch (Exception e) {
            return raw;
        }
    }
}

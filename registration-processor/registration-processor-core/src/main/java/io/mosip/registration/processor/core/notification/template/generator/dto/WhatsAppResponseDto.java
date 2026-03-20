package io.mosip.registration.processor.core.notification.template.generator.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class WhatsAppResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * The status.
     */
    private String status;

    /**
     * The message.
     */
    private String message;
}
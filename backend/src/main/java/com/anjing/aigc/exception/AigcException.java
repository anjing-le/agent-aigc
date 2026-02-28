package com.anjing.aigc.exception;

import lombok.Getter;

/**
 * AIGC 模块业务异常
 * 
 * @author AIGC Team
 */
@Getter
public class AigcException extends RuntimeException {
    
    private final String errorCode;
    private final String errorMessage;
    
    public AigcException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    public AigcException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}


package com.anjing.aigc.exception;

import com.anjing.model.errorcode.AigcErrorCode;
import com.anjing.model.exception.BizException;

/**
 * AIGC 模块业务异常
 * 
 * @author AIGC Team
 */
public class AigcException extends BizException {

    public AigcException(AigcErrorCode errorCode) {
        super(errorCode);
    }

    public AigcException(AigcErrorCode errorCode, String message) {
        super(message, errorCode);
    }

    public AigcException(AigcErrorCode errorCode, String message, Throwable cause) {
        super(message, cause, errorCode);
    }
}

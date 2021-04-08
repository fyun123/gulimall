package com.whut.gulimall.member.exception;

import com.whut.common.exception.BizCodeEnume;

public class PhoneExistException extends RuntimeException {
    public PhoneExistException() {
        super(BizCodeEnume.PHONE_EXIST_EXCEPTION.getMsg());
    }
}

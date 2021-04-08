package com.whut.gulimall.member.exception;

import com.whut.common.exception.BizCodeEnume;

public class UsernameExistException extends RuntimeException {
    public UsernameExistException() {
        super(BizCodeEnume.USER_EXIST_EXCEPTION.getMsg());
    }
}

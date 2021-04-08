package com.whut.common.to;

import lombok.Data;

@Data
public class SocialUserTo {


    private String accessToken;

    private long expiresIn;

    private String refreshToken;

    private String uid;

    private String remindIn;


}

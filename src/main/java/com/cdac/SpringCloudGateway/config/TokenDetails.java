package com.cdac.SpringCloudGateway.config;

import java.util.List;

import lombok.Data;

@Data
public class TokenDetails {

    private String userId;

    private List<String> roles;

}

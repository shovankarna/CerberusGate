package com.cdac.SpringCloudGateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cdac.SpringCloudGateway.config.TokenDetails;
import com.cdac.SpringCloudGateway.config.TokenExtract;
import com.cdac.SpringCloudGateway.services.NonceService;

@RestController
@CrossOrigin("*")
@RequestMapping(value = "/nonce")
public class NonceController {

    @Autowired
    private NonceService nonceService;

    @GetMapping("/fetch")
    public String getNonce(@RequestHeader("Authorization") String headerAuth) throws Exception {
        try {
            System.out.println("INSIDE NONCE");
            TokenDetails td = TokenExtract.getTokenDetails(headerAuth);
            return nonceService.generateNonce(td.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
package com.cdac.SpringCloudGateway.services;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

@Service
public class NonceService {

    private final ConcurrentMap<String, String> nonceStore = new ConcurrentHashMap<>();

    public String generateNonce(String userId) {
        if (hasNonce(userId)) {
            nonceStore.remove(userId);
        }

        String nonce = UUID.randomUUID().toString();
        nonceStore.put(userId, nonce);
        //// System.out.println("NEW NONCE USERID:" + userId);
        //// System.out.println("NEW NONCE: " + nonce);
        return nonce;
    }

    public boolean validateNonce(String userId, String modifiedNonce) {

        try {

            //// System.out.println("modifiedNonce length:" + modifiedNonce.length());
            if (modifiedNonce == null || modifiedNonce.length() != 36 + 20) { // 36 is UUID length, 16 is added random
                                                                              // chars
                return false;
            }

            // Extract the actual nonce by removing the inserted random characters
            String actualNonce = modifiedNonce.substring(4, 10) +
                    modifiedNonce.substring(14, 22) +
                    modifiedNonce.substring(26, 30) +
                    modifiedNonce.substring(34, 39) +
                    modifiedNonce.substring(43);

            //// System.out.println("actualNonce: " + actualNonce);
            //// System.out.println("storedNonce userId --> " + userId);

            String storedNonce = nonceStore.get(userId);

           // // System.out.println("storedNonce --> " + storedNonce);
            //// System.out.println("modifiedNonce --> " + modifiedNonce);

            if (storedNonce != null && storedNonce.equals(actualNonce)) {
                nonceStore.remove(userId); // Remove nonce after validation to prevent reuse
                return true;
            }
            return false;

        } catch (Exception e) {
            // TODO: handle exception
            //System.err.println("Error validating nonce for userId: " + userId);
            e.printStackTrace();
            return false; // Return false in case of error
        }
    }

    public boolean hasNonce(String userId) {
        return nonceStore.containsKey(userId);
    }
}

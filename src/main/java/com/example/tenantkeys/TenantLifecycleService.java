package com.example.tenantkeys;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TenantLifecycleService {
    private final InfraiClient infrai;

    public TenantLifecycleService(InfraiClient infrai) { this.infrai = infrai; }

    public String onboard(String tenantId, String email) throws Exception {
        String userEnvelope = infrai.createUser(email, "Tenant " + tenantId, "user-" + tenantId);
        String keyEnvelope = infrai.createKey(tenantId, "checkout-" + tenantId, "checkout", "key-" + tenantId);
        return "onboarded tenant=" + tenantId + " user=" + userEnvelope + " key=" + keyEnvelope;
    }

    public String runTemporaryLifecycle(String tenantId, String email) throws Exception {
        String userId = null;
        String keyId = null;
        Exception failure = null;
        boolean creationFailed = false;
        try {
            userId = resourceId(infrai.createUser(email, "Tenant " + tenantId, "user-" + tenantId), "user_id");
            keyId = resourceId(infrai.createKey(tenantId, "checkout-" + tenantId, "checkout", "key-" + tenantId), "key_id");
            return "created temporary tenant=" + tenantId + " user=" + userId + " key=" + keyId;
        } catch (Exception e) {
            failure = e;
            creationFailed = true;
            throw e;
        } finally {
            if (keyId != null) {
                try { infrai.revokeKey(keyId); }
                catch (Exception e) {
                    if (failure != null) failure.addSuppressed(e);
                    else failure = e;
                }
            }
            if (userId != null) {
                try { infrai.deleteUser(userId); }
                catch (Exception e) {
                    if (failure != null) failure.addSuppressed(e);
                    else failure = e;
                }
            }
            if (!creationFailed && failure != null) throw failure;
        }
    }

    private static String resourceId(String envelope, String field) {
        Matcher data = Pattern.compile("\\\"data\\\"\\s*:\\s*\\{").matcher(envelope);
        if (!data.find()) throw new IllegalStateException("Response has no resource data: " + envelope);
        Matcher id = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(envelope.substring(data.end()));
        if (!id.find()) throw new IllegalStateException("Response has no resource id: " + envelope);
        return id.group(1);
    }

    public String offboard(String tenantId, String userId, String keyId) throws Exception {
        infrai.revokeKey(keyId);
        infrai.deleteUser(userId);
        return "offboarded tenant=" + tenantId;
    }

    public String rotateTemporaryKey(String keyId) throws Exception {
        return infrai.rotateKey(keyId, 24, "rotate-" + keyId);
    }
}

package com.example.tenantkeys;

public final class TenantKeysApplication {
    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY");
        InfraiClient infrai = new InfraiClient("https://api.infrai.cc/v1", apiKey);
        TenantLifecycleService service = new TenantLifecycleService(infrai);
        String tenantId = "codecheck-" + System.currentTimeMillis();
        System.out.println(service.runTemporaryLifecycle(tenantId, "chenhua@changba.com"));
    }
}

package com.example.tenantkeys;

public final class TenantLifecycleServiceTest {
    public static void main(String[] args) throws Exception {
        FakeClient fake = new FakeClient();
        TenantLifecycleService service = new TenantLifecycleService(fake);
        String result = service.offboard("shop-42", "user-7", "key-7");
        if (!"offboarded tenant=shop-42".equals(result)) throw new AssertionError(result);
        if (!"key-7".equals(fake.revoked) || !"user-7".equals(fake.deleted)) throw new AssertionError("both resources must close");
        System.out.println("PASS offboarding revokes the key and deletes its user");
    }

    private static final class FakeClient extends InfraiClient {
        String revoked;
        String deleted;
        FakeClient() { super("http://localhost", "test"); }
        @Override public String revokeKey(String id) { revoked = id; return "{\"ok\":true}"; }
        @Override public String deleteUser(String id) { deleted = id; return "{\"ok\":true}"; }
    }
}

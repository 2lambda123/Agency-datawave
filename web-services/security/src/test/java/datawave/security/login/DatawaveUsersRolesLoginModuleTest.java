package datawave.security.login;

import datawave.security.auth.DatawaveCredential;
import datawave.security.authorization.DatawavePrincipal;
import org.jboss.security.SimplePrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.picketbox.plugins.PicketBoxCallbackHandler;

import javax.security.auth.Subject;
import javax.security.auth.login.FailedLoginException;
import java.lang.reflect.Field;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatawaveUsersRolesLoginModuleTest {
    private static final String NORMALIZED_SUBJECT_DN = "cn=testuser, ou=my department, o=my company, st=some-state, c=us";
    private static final String NORMALIZED_SUBJECT_DN_WITH_ISSUER_DN = "cn=testuser, ou=my department, o=my company, st=some-state, c=us<cn=test ca, ou=my department, o=my company, st=some-state, c=us>";
    private static final String SUBJECT_DN_WITH_CN_FIRST = "CN=testUser, OU=My Department, O=My Company, ST=Some-State, C=US";
    private static final String SUBJECT_DN_WITH_CN_LAST = "C=US, ST=Some-State, O=My Company, OU=My Department, CN=testUser";
    private static final String ISSUER_DN_WITH_CN_FIRST = "CN=TEST CA, OU=My Department, O=My Company, ST=Some-State, C=US";
    private static final String ISSUER_DN_WITH_CN_LAST = "C=US, ST=Some-State, O=My Company, OU=My Department, CN=TEST CA";
    
    private DatawaveUsersRolesLoginModule loginModule;
    private PicketBoxCallbackHandler callbackHandler;
    
    private X509Certificate testUserCert;
    
    @BeforeEach
    public void setUp() throws Exception {
        callbackHandler = new PicketBoxCallbackHandler();
        
        HashMap<String,String> sharedState = new HashMap<>();
        HashMap<String,String> options = new HashMap<>();
        options.put("usersProperties", "users.properties");
        options.put("rolesProperties", "roles.properties");
        options.put("principalClass", "datawave.security.authorization.DatawavePrincipal");
        
        loginModule = new DatawaveUsersRolesLoginModule();
        loginModule.initialize(new Subject(), callbackHandler, sharedState, options);
        
        KeyStore truststore = KeyStore.getInstance("PKCS12");
        truststore.load(getClass().getResourceAsStream("/ca.pkcs12"), "secret".toCharArray());
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(getClass().getResourceAsStream("/testUser.pkcs12"), "secret".toCharArray());
        testUserCert = (X509Certificate) keystore.getCertificate("testuser");
    }
    
    @Disabled
    @Test
    public void testSuccessfulLogin() throws Exception {
        String name = testUserCert.getSubjectDN().getName() + "<" + testUserCert.getIssuerDN().getName() + ">";
        callbackHandler.setSecurityInfo(new SimplePrincipal(name), new DatawaveCredential(testUserCert.getSubjectDN().getName(), testUserCert.getIssuerDN()
                        .getName(), null, null).toString());
        
        boolean success = loginModule.login();
        assertTrue(success, "Login didn't succeed for alias in users/roles.properties");
        Field f = loginModule.getClass().getField("identity");
        f.setAccessible(true);
        DatawavePrincipal principal = (DatawavePrincipal) f.get(loginModule);
        assertEquals(NORMALIZED_SUBJECT_DN_WITH_ISSUER_DN, principal.getName());
    }
    
    @Disabled
    @Test
    public void testReverseDnSuccessfulLogin() throws Exception {
        String name = SUBJECT_DN_WITH_CN_LAST + "<" + ISSUER_DN_WITH_CN_LAST + ">";
        callbackHandler.setSecurityInfo(new SimplePrincipal(name),
                        new DatawaveCredential(SUBJECT_DN_WITH_CN_LAST, ISSUER_DN_WITH_CN_LAST, null, null).toString());
        
        boolean success = loginModule.login();
        assertTrue(success, "Login didn't succeed for alias in users/roles.properties");
        Field f = loginModule.getClass().getField("identity");
        f.setAccessible(true);
        DatawavePrincipal principal = (DatawavePrincipal) f.get(loginModule);
        assertEquals(NORMALIZED_SUBJECT_DN_WITH_ISSUER_DN, principal.getName());
    }
    
    @Disabled
    @Test
    public void testFailedLoginBadPassword() throws Exception {
        FailedLoginException e = assertThrows(FailedLoginException.class, () -> callbackHandler.setSecurityInfo(new SimplePrincipal("testUser<testIssuer>"),
                        new DatawaveCredential("testUser", "testIssuer", null, null).toString()));
        assertEquals("Password invalid/Password required", e.getMessage());
        
        boolean success = loginModule.login();
        assertFalse(success, "Login succeed for alias in users.properties with bad password");
    }
    
    @Test
    public void normalizeDnWithCnLast() {
        assertEquals(NORMALIZED_SUBJECT_DN, DatawaveUsersRolesLoginModule.normalizeUsername(SUBJECT_DN_WITH_CN_LAST));
    }
    
    @Test
    public void normalizeDnWithCnFirst() {
        assertEquals(NORMALIZED_SUBJECT_DN, DatawaveUsersRolesLoginModule.normalizeUsername(SUBJECT_DN_WITH_CN_FIRST));
    }
    
    @Test
    public void normalizeSubjectIssuerCombinations() {
        assertEquals(NORMALIZED_SUBJECT_DN_WITH_ISSUER_DN,
                        DatawaveUsersRolesLoginModule.normalizeUsername(SUBJECT_DN_WITH_CN_FIRST + "<" + ISSUER_DN_WITH_CN_FIRST + ">"));
        assertEquals(NORMALIZED_SUBJECT_DN_WITH_ISSUER_DN,
                        DatawaveUsersRolesLoginModule.normalizeUsername(SUBJECT_DN_WITH_CN_FIRST + "<" + ISSUER_DN_WITH_CN_LAST + ">"));
        assertEquals(NORMALIZED_SUBJECT_DN_WITH_ISSUER_DN,
                        DatawaveUsersRolesLoginModule.normalizeUsername(SUBJECT_DN_WITH_CN_LAST + "<" + ISSUER_DN_WITH_CN_FIRST + ">"));
        assertEquals(NORMALIZED_SUBJECT_DN_WITH_ISSUER_DN,
                        DatawaveUsersRolesLoginModule.normalizeUsername(SUBJECT_DN_WITH_CN_LAST + "<" + ISSUER_DN_WITH_CN_LAST + ">"));
    }
}

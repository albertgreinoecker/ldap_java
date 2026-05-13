package at.ac.htlinn.ldap;

import io.github.cdimascio.dotenv.Dotenv;

import javax.naming.NamingException;

public class LDAPMAin {
    public static void main(String[] args) throws NamingException {
        Dotenv dotenv = Dotenv.load();

        String url = dotenv.get("LDAP_URL");
        String bindOn = dotenv.get("LDAP_BIND_ON");
        String password = dotenv.get("LDAP_PASSWORD");

        LDAPWrapper ldapWrapper = new LDAPWrapper(url, bindOn,password);
        ldapWrapper.searchByAccountName("a.greinoecker");

        boolean ok = ldapWrapper.checkAuthenticate("a.greinoecker", "test");
        System.out.println("LOGIN-OK" + ok);
        ldapWrapper.close();
      }
}

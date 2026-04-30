package at.ac.htlinn.ldap;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.directory.*;
import java.util.Hashtable;


public class LDAPWrapper {
    String ldapUrl = "";
    String bindDn = "";
    String password = "";
    DirContext context = null;

    public LDAPWrapper(String ldapUrl, String bindDn, String password) throws NamingException {
        this.ldapUrl = ldapUrl;
        this.bindDn = bindDn;
        this.password = password;
        connect();
    }

    public void connect() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();

        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");

        env.put(Context.PROVIDER_URL, ldapUrl);

        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, bindDn);
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.REFERRAL, "ignore");
        context = new InitialDirContext(env);
        System.out.println("LDAP-Verbindung erfolgreich.");
    }

   public void searchByAccountName(String name) throws NamingException {
       String searchFilter = String.format(
               "(&(sAMAccountName=%s)(objectClass=person)(memberOf=CN=All-Staff,OU=ADMIN,DC=SYNCHTLINN,DC=local))",
               name
       );
       search("DC=SYNCHTLINN,DC=local", searchFilter);

   }

   public void search(String searchBase , String searchFilter) throws NamingException {

       SearchControls controls = new SearchControls();
       controls.setSearchScope(SearchControls.SUBTREE_SCOPE);

       NamingEnumeration<SearchResult> results =
               context.search(searchBase, searchFilter, controls);

       try {
           while (results.hasMore()) {
               SearchResult result = results.next();
               Attributes attrs = result.getAttributes();

               System.out.println("DN: " + result.getNameInNamespace());

               NamingEnumeration<? extends Attribute> allAttrs = attrs.getAll();

               while (allAttrs.hasMore()) {
                   Attribute attr = allAttrs.next();

                   String attrName = attr.getID();

                   NamingEnumeration<?> values = attr.getAll();
                   while (values.hasMore()) {
                       Object value = values.next();
                       System.out.println(attrName + ": " + value);
                   }
               }

               System.out.println("----------------------");
           }
       } catch (PartialResultException e) {
           // AD returns referrals after real results — all data already processed
       }
       context.close();
   }

   boolean  checkAuthenticate(String username, String password) {
        Hashtable<String, String> env = new Hashtable<>();

        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");

        env.put(Context.PROVIDER_URL, ldapUrl);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");

        env.put(Context.SECURITY_PRINCIPAL,
                username + "@SYNCHTLINN.local");

        env.put(Context.SECURITY_CREDENTIALS, password);

        try {
            new InitialDirContext(env).close();
            return true; // ✅ Login erfolgreich
        } catch (NamingException e) {
            return false; // Login fehlgeschlagen
        }
    }
    void close() throws NamingException {
        context.close();
    }
}

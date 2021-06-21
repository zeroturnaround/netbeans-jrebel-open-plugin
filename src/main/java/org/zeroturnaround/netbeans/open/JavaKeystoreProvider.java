package org.zeroturnaround.netbeans.open;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.spi.autoupdate.KeyStoreProvider;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service=KeyStoreProvider.class)
public class JavaKeystoreProvider implements KeyStoreProvider {

    private static final Logger logger = Logger.getLogger(JavaKeystoreProvider.class.getName());

    private final KeyStore keystore;
    
    public JavaKeystoreProvider() {
        
        KeyStore ks = null;
        
        try {
            logger.info("Loading Java truststore...");
            
            String relativeCacertsPath = "/lib/security/cacerts".replace("/", File.separator);
            String filename = System.getProperty("java.home") + relativeCacertsPath;
            FileInputStream is = new FileInputStream(filename);

            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            String password = "changeit";
            ks.load(is, password.toCharArray());

            logger.info("Java truststore loaded.");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Keystore loading failed", ex);
            
            try {
                ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(null, null);
            } catch (Exception ex2) {
            }
        }
        
        keystore = ks;
    }
    
    @Override
    public KeyStore getKeyStore() {
        return keystore;
    }
  
}

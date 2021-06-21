/*
 * The MIT License
 *
 * Copyright 2021 Perforce Software AS.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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

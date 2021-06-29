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

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.netbeans.api.autoupdate.InstallSupport;
import org.netbeans.api.autoupdate.OperationContainer;
import org.netbeans.api.autoupdate.OperationException;
import org.netbeans.api.autoupdate.OperationSupport;
import org.netbeans.api.autoupdate.UpdateElement;
import org.netbeans.api.autoupdate.UpdateUnit;
import org.netbeans.api.autoupdate.UpdateUnitProvider;
import org.netbeans.api.autoupdate.UpdateUnitProviderFactory;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.modules.ModuleInstall;
import org.openide.util.Exceptions;

public class Installer extends ModuleInstall {

    private static final Logger logger = Logger.getLogger(Installer.class.getName());

    private static final String ZT_PROVIDER_URL = "http://localhost:8000/updates.xml";

    @Override
    public void restored() {
        logger.info("=== STARTING! ===");

        try {

            UpdateUnitProvider ztProvider = UpdateUnitProviderFactory.getDefault().getUpdateUnitProviders(false).stream()
                    .filter(provider -> ZT_PROVIDER_URL.equals(provider.getProviderURL()))
                    .findAny()
                    .orElse(createZTProvider());

            try (ProgressHandle handle = ProgressHandle.createHandle("Refreshing updates information...")) {
                handle.start();
                handle.switchToIndeterminate();
                
                ztProvider.refresh(handle, true);
                
                handle.finish();
            }

            List<UpdateUnit> updateUnits = ztProvider.getUpdateUnits();
            updateUnits.stream().forEach(uu -> logger.info("Update Unit: " + uu));
            Optional<UpdateUnit> maybeModuleUpdateUnit = updateUnits.stream().filter(uu -> "org.zeroturnaround.jrebel.netbeans".equals(uu.getCodeName())).findAny();

            UpdateUnit jrebelUnit = null;
            UpdateElement jrebelUpdate = null;

            if (maybeModuleUpdateUnit.isPresent()) {
                jrebelUnit = maybeModuleUpdateUnit.get();
                logger.info("JRebel update unit: " + jrebelUnit);

                List<UpdateElement> updateElements = jrebelUnit.getAvailableUpdates();
                if (!updateElements.isEmpty()) {
                    jrebelUpdate = updateElements.get(updateElements.size() - 1);
                    logger.info("JRebel update: " + jrebelUpdate);
                } else {
                    logger.info("JRebel updates NOT FOUND");
                    return;
                }
            } else {
                logger.info("JRebel update unit NOT FOUND");
                return;
            }

            OperationContainer<InstallSupport> operationContainer = OperationContainer.createForInstall();
            if (!operationContainer.canBeAdded(jrebelUnit, jrebelUpdate)) {
                logger.warning("Cannot install module update: " + jrebelUpdate);
                return;
            }
            operationContainer.add(Collections.singleton(jrebelUpdate));

            InstallSupport support = operationContainer.getSupport();

            InstallSupport.Validator v;
            InstallSupport.Installer i;
            OperationSupport.Restarter r;

            logger.info("Downloading " + jrebelUpdate);

            try ( ProgressHandle downloadHandle = ProgressHandle.createHandle("Downloading...")) {
                v = support.doDownload(downloadHandle, true, true);
            }
            logger.info("Validating " + jrebelUpdate);
            try ( ProgressHandle validateHandle = ProgressHandle.createHandle("Validating...")) {
                i = support.doValidate(v, validateHandle);
            }
            logger.info("Installing " + jrebelUpdate);
            try ( ProgressHandle installHandle = ProgressHandle.createHandle("Installing...")) {
                r = support.doInstall(i, installHandle);
            }
            logger.info("Installed! " + jrebelUpdate);

            if (r != null) {

                showMessage("JRebel for NetBeans has been successfully installed.");
                
                logger.info("Restart now");
                SwingUtilities.invokeLater(() -> {
                    try {
                        support.doRestart(r, null);
                    } catch (OperationException ex) {
                        logger.log(Level.SEVERE, "Restart failed", ex);
                    }
                });
            }

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unexpected failure", ex);
        } finally {
            logger.info("=== FINISHED! ===");
        }
    }

    private void showMessage(final String message) {
        if (SwingUtilities.isEventDispatchThread()) {
            showMessageInEventDispatcherThread(message);
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> showMessageInEventDispatcherThread(message));
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Failed to show the message '" + message + "' in UI", ex);
            }
        }
    }
    
    private void showMessageInEventDispatcherThread(String message) {
        JOptionPane.showMessageDialog(null, message, "JRebel for NetBeans Installer", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private static UpdateUnitProvider createZTProvider() throws MalformedURLException {
        return UpdateUnitProviderFactory.getDefault().create("ZT Provider TEST", "Zeroturnaround Provider TEST ", new URL(ZT_PROVIDER_URL));
    }

}

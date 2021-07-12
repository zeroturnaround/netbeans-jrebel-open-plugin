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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import org.netbeans.api.autoupdate.InstallSupport;
import org.netbeans.api.autoupdate.OperationContainer;
import org.netbeans.api.autoupdate.UpdateElement;
import org.netbeans.api.autoupdate.UpdateUnit;
import org.netbeans.api.autoupdate.UpdateUnitProvider;
import org.netbeans.api.autoupdate.UpdateUnitProviderFactory;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.modules.autoupdate.ui.api.PluginManager;
import org.openide.modules.ModuleInstall;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

public class Installer extends ModuleInstall {

  private static final Logger logger = Logger.getLogger(Installer.class.getName());

  private static final String JREBEL_PROVIDER_URL = "https://dl.zeroturnaround.com/jrebel/netbeans/updates-nightly.xml";
  private static final String JREBEL_MODULE_CODE_NAME = "org.zeroturnaround.jrebel.netbeans";
  private static final String JREBEL_INSTALLED = "jrebel.installed";

  private static ResourceBundle bundle = NbBundle.getBundle(Installer.class);

  @Override
  public void restored() {
    logger.log(Level.INFO, "=== STARTING! [{0}] ===", Thread.currentThread().getName());

    try {

      UpdateUnitProvider jrebelProvider = UpdateUnitProviderFactory.getDefault().getUpdateUnitProviders(false).stream()
              .filter(provider -> JREBEL_PROVIDER_URL.equals(provider.getProviderURL()))
              .findAny()
              .or(() -> Optional.of(createJRebelProvider()))
              .get();

      try ( ProgressHandle handle = ProgressHandle.createHandle(bundle.getString("Installer.refreshing.updates.info"))) {
        handle.start();
        handle.switchToIndeterminate();

        jrebelProvider.refresh(handle, true);

        handle.finish();
      }

      List<UpdateUnit> updateUnits = jrebelProvider.getUpdateUnits();
      Optional<UpdateUnit> maybeModuleUpdateUnit = updateUnits.stream().filter(uu -> JREBEL_MODULE_CODE_NAME.equals(uu.getCodeName())).findAny();

      if (!maybeModuleUpdateUnit.isPresent()) {
        logger.info("JRebel update unit NOT FOUND");
        return;
      }

      if (isJRebelInstalled()) {
        logger.info("JRebel has been marked installed - LEAVING");
        return;
      }

      setJRebelInstalled(true); //either already installed or tentative state (immediate restart from the wizard won't give a chance to update the preference)

      UpdateUnit jrebelUnit = maybeModuleUpdateUnit.get();
      logger.log(Level.INFO, "JRebel update unit: {0}", jrebelUnit);

      List<UpdateElement> updateElements = jrebelUnit.getAvailableUpdates();

      if (updateElements.isEmpty()) {
        logger.info("NO JRebel updates FOUND");
        return;
      }

      UpdateElement jrebelUpdate = updateElements.get(updateElements.size() - 1);
      logger.log(Level.INFO, "JRebel update: {0}", jrebelUpdate);

      OperationContainer<InstallSupport> operationContainer = OperationContainer.createForInstall();
      if (!operationContainer.canBeAdded(jrebelUnit, jrebelUpdate)) {
        logger.log(Level.WARNING, "Cannot install module update: {0}", jrebelUpdate);
        return;
      }
      operationContainer.add(Collections.singleton(jrebelUpdate));

      if (SwingUtilities.isEventDispatchThread()) {
        openJRebelInstallWizard(operationContainer);
      } else {
        SwingUtilities.invokeLater(() -> {
          openJRebelInstallWizard(operationContainer);
        });
      }

    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Unexpected failure", ex);
    } finally {
      logger.info("=== FINISHED! ===");
    }
  }

  private void openJRebelInstallWizard(OperationContainer<InstallSupport> operationContainer) {
    logger.info("JRebel installation - open wizard");
    boolean success = PluginManager.openInstallWizard(operationContainer);
    logger.log(Level.INFO, "JRebel installation result: {0}", success);
    setJRebelInstalled(success);
  }

  private static UpdateUnitProvider createJRebelProvider() {
    try {
      UpdateUnitProvider provider = UpdateUnitProviderFactory.getDefault().create(bundle.getString("Installer.update.unit.provider.name"), bundle.getString("Installer.update.unit.provider.displayname"), new URL(JREBEL_PROVIDER_URL));
      provider.setEnable(true);
      return provider;
    } catch (MalformedURLException ex) {
      throw new IllegalStateException("Could not create JRebel update unit provider", ex);
    }
  }

  private Preferences getPreferences() {
    return NbPreferences.forModule(Installer.class);
  }

  private void setJRebelInstalled(boolean installed) {
    try {
      getPreferences().putBoolean(JREBEL_INSTALLED, installed);
      getPreferences().flush();
    } catch (BackingStoreException ex) {
      throw new IllegalStateException("Could not update preference value", ex);
    }
  }

  private boolean isJRebelInstalled() {
    return getPreferences().getBoolean(JREBEL_INSTALLED, false);
  }
}

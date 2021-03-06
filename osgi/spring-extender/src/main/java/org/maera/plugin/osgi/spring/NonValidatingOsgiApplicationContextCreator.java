package org.maera.plugin.osgi.spring;

import org.eclipse.gemini.blueprint.context.DelegatedExecutionOsgiBundleApplicationContext;
import org.eclipse.gemini.blueprint.extender.OsgiApplicationContextCreator;
import org.eclipse.gemini.blueprint.extender.support.ApplicationContextConfiguration;
import org.eclipse.gemini.blueprint.extender.support.DefaultOsgiApplicationContextCreator;
import org.eclipse.gemini.blueprint.extender.support.scanning.ConfigurationScanner;
import org.eclipse.gemini.blueprint.extender.support.scanning.DefaultConfigurationScanner;
import org.eclipse.gemini.blueprint.util.OsgiStringUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.maera.NonValidatingOsgiBundleXmlApplicationContext;
import org.springframework.util.ObjectUtils;

/**
 * Application context creator that will use a special application context that disables XML Schema validation
 *
 * @since 0.1
 */
public class NonValidatingOsgiApplicationContextCreator implements OsgiApplicationContextCreator {

    private static final Logger log = LoggerFactory.getLogger(DefaultOsgiApplicationContextCreator.class);

    private ConfigurationScanner configurationScanner = new DefaultConfigurationScanner();

    /**
     * Creates an application context that disables validation.  Most of this code is copy/pasted from
     * {@link DefaultOsgiApplicationContextCreator}
     *
     * @param bundleContext The bundle context for the application context
     * @return The new application context
     * @throws Exception If anything goes wrong
     */
    public DelegatedExecutionOsgiBundleApplicationContext createApplicationContext(BundleContext bundleContext)
            throws Exception {

        Bundle bundle = bundleContext.getBundle();
        ApplicationContextConfiguration config = new ApplicationContextConfiguration(bundle, configurationScanner);

        if (log.isTraceEnabled()) {
            log.trace("Created configuration " + config + " for bundle "
                    + OsgiStringUtils.nullSafeNameAndSymName(bundle));
        }

        // it's not a spring bundle, ignore it
        if (!config.isSpringPoweredBundle()) {
            return null;
        }

        if ( log.isInfoEnabled() ) {
        log.info("Discovered configurations " + ObjectUtils.nullSafeToString(config.getConfigurationLocations())
                + " in bundle [" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "]");
        }

        // This is the one new line, which uses our application context and not the other one
        DelegatedExecutionOsgiBundleApplicationContext sdoac =
                new NonValidatingOsgiBundleXmlApplicationContext(config.getConfigurationLocations());

        sdoac.setBundleContext(bundleContext);
        sdoac.setPublishContextAsService(config.isPublishContextAsService());

        return sdoac;
    }
}

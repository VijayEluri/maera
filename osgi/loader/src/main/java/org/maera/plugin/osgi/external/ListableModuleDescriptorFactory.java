package org.maera.plugin.osgi.external;

import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.ModuleDescriptorFactory;

import java.util.Set;

/**
 * A module descriptor factory that can list its supported module descriptors.
 *
 * @since 0.1
 */
public interface ListableModuleDescriptorFactory extends ModuleDescriptorFactory {
    
    Set<Class<ModuleDescriptor<?>>> getModuleDescriptorClasses();
}

package org.maera.plugin.loaders;

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.maera.plugin.*;
import org.maera.plugin.descriptors.UnrecognisedModuleDescriptor;
import org.maera.plugin.elements.ResourceLocation;
import org.maera.plugin.hostcontainer.DefaultHostContainer;
import org.maera.plugin.impl.UnloadablePlugin;
import org.maera.plugin.mock.*;
import org.maera.plugin.util.ClassLoaderUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

public class SinglePluginLoaderTest {

    @Test
    public void testAtlassianPlugin() throws Exception {
        final SinglePluginLoader loader = new SinglePluginLoader("test-maera-plugin.xml");
        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("vegetable", MockVegetableModuleDescriptor.class);
        final Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        assertEquals(1, plugins.size());

        // test the plugin information
        final Plugin plugin = (Plugin) plugins.iterator().next();
        enableModules(plugin);
        assertEquals("Test Plugin", plugin.getName());
        assertEquals("test.maera.plugin", plugin.getKey());
        assertNotNull(plugin.getPluginInformation());
        assertEquals("1.0", plugin.getPluginInformation().getVersion());
        assertEquals("test.maera.plugin.i18n", plugin.getI18nNameKey());
        assertEquals("test.maera.plugin.desc.i18n", plugin.getPluginInformation().getDescriptionKey());
        assertEquals("This plugin descriptor is just used for test purposes!", plugin.getPluginInformation().getDescription());
        assertEquals("Atlassian Software Systems Pty Ltd", plugin.getPluginInformation().getVendorName());
        assertEquals("http://www.atlassian.com", plugin.getPluginInformation().getVendorUrl());
        assertEquals(3f, plugin.getPluginInformation().getMinVersion(), 0);
        assertEquals(3.1f, plugin.getPluginInformation().getMaxVersion(), 0);
        assertEquals(4, plugin.getModuleDescriptors().size());

        final ModuleDescriptor bearDescriptor = plugin.getModuleDescriptor("bear");
        assertEquals("test.maera.plugin:bear", bearDescriptor.getCompleteKey());
        assertEquals("bear", bearDescriptor.getKey());
        assertEquals("Bear Animal", bearDescriptor.getName());
        assertEquals(MockBear.class, bearDescriptor.getModuleClass());
        assertEquals("A plugin module to describe a bear", bearDescriptor.getDescription());
        assertTrue(bearDescriptor.isEnabledByDefault());
        assertEquals("test.maera.module.bear.name", bearDescriptor.getI18nNameKey());
        assertEquals("test.maera.module.bear.description", bearDescriptor.getDescriptionKey());

        final Iterable resources = bearDescriptor.getResourceDescriptors();
        assertEquals(3, Iterables.size(resources));

        assertEquals("20", bearDescriptor.getParams().get("height"));
        assertEquals("brown", bearDescriptor.getParams().get("colour"));

        final List goldDescriptors = plugin.getModuleDescriptorsByModuleClass(MockGold.class);
        assertEquals(1, goldDescriptors.size());
        final ModuleDescriptor goldDescriptor = (ModuleDescriptor) goldDescriptors.get(0);
        assertEquals("test.maera.plugin:gold", goldDescriptor.getCompleteKey());
        assertEquals(new MockGold(20), goldDescriptor.getModule());
        assertEquals(goldDescriptors, plugin.getModuleDescriptorsByModuleClass(MockMineral.class));

        assertEquals(1, Iterables.size(plugin.getResourceDescriptors()));
        final ResourceLocation pluginResource = plugin.getResourceLocation("download", "icon.gif");
        assertEquals("/icon.gif", pluginResource.getLocation());
    }

    @Test
    public void testBadPluginKey() throws PluginParseException {
        final SinglePluginLoader loader = new SinglePluginLoader("test-bad-plugin-key-plugin.xml");
        final Collection<Plugin> plugins = loader.loadAllPlugins(null);
        assertEquals(1, plugins.size());
        final Plugin plugin = plugins.iterator().next();
        assertTrue(plugin instanceof UnloadablePlugin);
        assertEquals("test-bad-plugin-key-plugin.xml", plugin.getKey());
        assertTrue(((UnloadablePlugin) plugin).getErrorText().endsWith("Plugin keys cannot contain ':'. Key is 'test:bad'"));
    }

    @Test
    public void testBadResource() {
        final List<Plugin> plugins = new ArrayList<Plugin>(new SinglePluginLoader("foo").loadAllPlugins(null));
        assertEquals(1, plugins.size());
        assertTrue(plugins.get(0) instanceof UnloadablePlugin);
        assertEquals("foo", plugins.get(0).getKey());
    }

    @Test
    public void testDisabledPlugin() throws PluginParseException {
        final SinglePluginLoader loader = new SinglePluginLoader("test-disabled-plugin.xml");
        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        final Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);
        assertEquals(1, plugins.size());
        final Plugin plugin = (Plugin) plugins.iterator().next();
        assertFalse(plugin.isEnabledByDefault());

        assertEquals(1, plugin.getModuleDescriptors().size());
        final ModuleDescriptor module = plugin.getModuleDescriptor("gold");
        assertFalse(module.isEnabledByDefault());
    }

    @Test
    public void testNonUniqueKeysWithinAPlugin() throws PluginParseException {
        final SinglePluginLoader loader = new SinglePluginLoader("test-bad-non-unique-keys-plugin.xml");
        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        final List<Plugin> plugins = new ArrayList<Plugin>(loader.loadAllPlugins(moduleDescriptorFactory));
        assertEquals(1, plugins.size());
        final Plugin plugin = plugins.get(0);
        assertTrue(plugin instanceof UnloadablePlugin);
        assertTrue(((UnloadablePlugin) plugin).getErrorText().endsWith("Found duplicate key 'bear' within plugin 'test.bad.plugin'"));
    }

    /**
     * @deprecated testing deprecated behaviour
     */
    @Deprecated
    @Test
    public void testPluginByInputStream() {
        final SinglePluginLoader loader = new SinglePluginLoader(ClassLoaderUtils.getResourceAsStream("test-disabled-plugin.xml", SinglePluginLoader.class));
        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        final Collection<Plugin> plugins = loader.loadAllPlugins(moduleDescriptorFactory);
        assertEquals(1, plugins.size());
        assertFalse((plugins.iterator().next()).isEnabledByDefault());
    }

    /**
     * @deprecated testing deprecated behaviour
     */
    @Deprecated
    @Test(expected = IllegalStateException.class)
    public void testPluginByInputStreamNotReentrant() {
        final SinglePluginLoader loader = new SinglePluginLoader(ClassLoaderUtils.getResourceAsStream("test-disabled-plugin.xml", SinglePluginLoader.class));
        loader.getSource();
        loader.getSource();
    }

    @Test
    public void testPluginByUrl() throws PluginParseException {
        final SinglePluginLoader loader = new SinglePluginLoader(ClassLoaderUtils.getResource("test-disabled-plugin.xml", SinglePluginLoader.class));
        // URL created should be reentrant and create a different stream each
        // time
        assertNotSame(loader.getSource(), loader.getSource());
        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        final Collection<Plugin> plugins = loader.loadAllPlugins(moduleDescriptorFactory);
        assertEquals(1, plugins.size());
        assertFalse((plugins.iterator().next()).isEnabledByDefault());
    }

    @Test
    public void testPluginWithOnlyPermittedModules() throws PluginParseException {
        final SinglePluginLoader loader = new SinglePluginLoader("test-maera-plugin.xml");

        // Define the module descriptor factory
        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        // Exclude mineral
        final List<String> permittedList = new ArrayList<String>();
        permittedList.add("animal");
        moduleDescriptorFactory.setPermittedModuleKeys(permittedList);

        final Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        // 1 plugin returned
        assertEquals(1, plugins.size());

        final Plugin plugin = (Plugin) plugins.iterator().next();

        // Only one descriptor, animal
        assertEquals(1, plugin.getModuleDescriptors().size());
        assertNotNull(plugin.getModuleDescriptor("bear"));
        assertNull(plugin.getModuleDescriptor("gold"));
    }

    @Test
    public void testPluginWithOnlyPermittedModulesAndMissingModuleDescriptor() throws PluginParseException {
        final SinglePluginLoader loader = new SinglePluginLoader("test-maera-plugin.xml");

        // Define the module descriptor factory
        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);

        // Exclude mineral
        final List<String> permittedList = new ArrayList<String>();
        permittedList.add("animal");
        moduleDescriptorFactory.setPermittedModuleKeys(permittedList);

        final Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        // 1 plugin returned
        assertEquals(1, plugins.size());

        final Plugin plugin = (Plugin) plugins.iterator().next();

        // Only one descriptor, animal
        assertEquals(1, plugin.getModuleDescriptors().size());
        assertNotNull(plugin.getModuleDescriptor("bear"));
        assertNull(plugin.getModuleDescriptor("gold"));
    }

    @Test
    public void testPluginsInOrder() throws PluginParseException {
        final SinglePluginLoader loader = new SinglePluginLoader(ClassLoaderUtils.getResource("test-ordered-pluginmodules.xml", SinglePluginLoader.class));
        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        final Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);
        final Plugin plugin = (Plugin) plugins.iterator().next();
        final Collection modules = plugin.getModuleDescriptors();
        assertEquals(3, modules.size());
        final Iterator iterator = modules.iterator();
        assertEquals("yogi1", ((MockAnimalModuleDescriptor) iterator.next()).getKey());
        assertEquals("yogi2", ((MockAnimalModuleDescriptor) iterator.next()).getKey());
        assertEquals("yogi3", ((MockAnimalModuleDescriptor) iterator.next()).getKey());
    }

    @Test
    public void testRejectOsgiPlugin() throws Exception {
        final SinglePluginLoader loader = new SinglePluginLoader("test-osgi-plugin.xml");
        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        final Collection<Plugin> plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        assertEquals(1, plugins.size());

        // test the plugin information
        final Plugin plugin = plugins.iterator().next();
        assertTrue(plugin instanceof UnloadablePlugin);
        assertEquals("test.maera.plugin", plugin.getKey());
    }

    @Test
    public void testSinglePluginLoader() throws Exception {
        final SinglePluginLoader loader = new SinglePluginLoader("test-system-plugin.xml");
        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        final Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        assertEquals(1, plugins.size());

        // test the plugin information
        final Plugin plugin = (Plugin) plugins.iterator().next();
        assertTrue(plugin.isSystemPlugin());
    }

    @Test
    public void testUnknownPluginModule() throws PluginParseException {
        final SinglePluginLoader loader = new SinglePluginLoader("test-bad-plugin.xml");
        final Collection<Plugin> plugins = loader.loadAllPlugins(new DefaultModuleDescriptorFactory(new DefaultHostContainer()));
        final List<Plugin> pluginsList = new ArrayList<Plugin>(plugins);

        assertEquals(1, pluginsList.size());

        final Plugin plugin = plugins.iterator().next();
        final List<ModuleDescriptor<?>> moduleList = new ArrayList<ModuleDescriptor<?>>(plugin.getModuleDescriptors());

        // The module that had the problem should be an
        // UnrecognisedModuleDescriptor
        assertEquals(UnrecognisedModuleDescriptor.class, moduleList.get(0).getClass());
    }

    private void enableModules(final Plugin plugin) {
        for (final ModuleDescriptor descriptor : plugin.getModuleDescriptors()) {
            if (descriptor instanceof StateAware) {
                ((StateAware) descriptor).enabled();
            }
        }
    }
}

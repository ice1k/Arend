package org.arend.library;

import org.arend.ext.ArendExtension;
import org.arend.ext.DefaultArendExtension;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.ModulePath;
import org.arend.ext.typechecking.DefinitionListener;
import org.arend.ext.typechecking.ListDefinitionListener;
import org.arend.ext.ui.ArendUI;
import org.arend.extImpl.*;
import org.arend.library.classLoader.MultiClassLoader;
import org.arend.library.error.LibraryError;
import org.arend.module.error.ExceptionError;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.module.scopeprovider.SimpleModuleScopeProvider;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.naming.scope.Scope;
import org.arend.prelude.Prelude;
import org.arend.source.BinarySource;
import org.arend.source.PersistableBinarySource;
import org.arend.source.Source;
import org.arend.source.SourceLoader;
import org.arend.source.error.PersistingError;
import org.arend.term.group.ChildGroup;
import org.arend.typechecking.order.dependency.DependencyListener;
import org.arend.typechecking.order.dependency.DummyDependencyListener;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a library which can load modules in the binary format (see {@link #getBinarySource})
 * as well as ordinary modules (see {@link #getRawSource}).
 */
public abstract class SourceLibrary extends BaseLibrary {
  public enum Flag { RECOMPILE }
  private final EnumSet<Flag> myFlags = EnumSet.noneOf(Flag.class);
  private final SimpleModuleScopeProvider myAdditionalModuleScopeProvider = new SimpleModuleScopeProvider();
  private ArendExtension myExtension;

  /**
   * Adds a flag.
   */
  public void addFlag(Flag flag) {
    myFlags.add(flag);
  }

  /**
   * Removes a flag.
   */
  public void removeFlag(Flag flag) {
    myFlags.remove(flag);
  }

  /**
   * Gets the raw source (that is, the source containing not typechecked data) for a given module path.
   *
   * @param modulePath  a path to the source.
   *
   * @return the raw source corresponding to the given path or null if the source is not found.
   */
  @Nullable
  public abstract Source getRawSource(ModulePath modulePath);

  /**
   * Gets the test source for a given module path.
   *
   * @param modulePath  a path to the source.
   *
   * @return the test source corresponding to the given path or null if the source is not found.
   */
  @Nullable
  public Source getTestSource(ModulePath modulePath) {
    return null;
  }

  /**
   * Gets the binary source (that is, the source containing typechecked data) for a given module path.
   *
   * @param modulePath  a path to the source.
   *
   * @return the binary source corresponding to the given path or null if the source is not found.
   */
  @Nullable
  public BinarySource getBinarySource(ModulePath modulePath) {
    return getPersistableBinarySource(modulePath);
  }

  @Nullable
  public abstract PersistableBinarySource getPersistableBinarySource(ModulePath modulePath);

  /**
   * Gets the collection of modules generated by the language extension.
   *
   * @return the collection of additional modules.
   */
  public @NotNull Collection<? extends Map.Entry<ModulePath, Scope>> getAdditionalModules() {
    return myAdditionalModuleScopeProvider.getRegisteredEntries();
  }

  @Override
  public final @NotNull ModuleScopeProvider getModuleScopeProvider() {
    return module -> {
      Scope scope = myAdditionalModuleScopeProvider.forModule(module);
      return scope != null ? scope : getDeclaredModuleScopeProvider().forModule(module);
    };
  }

  @NotNull
  @Override
  public ArendExtension getArendExtension() {
    return myExtension != null ? myExtension : super.getArendExtension();
  }

  public void setArendExtension(ArendExtension extension) {
    myExtension = extension;
  }

  @Nullable
  public ArendUI getUI() {
    return null;
  }

  /**
   * Loads the header of this library.
   *
   * @param errorReporter a reporter for all errors that occur during the loading process.
   *
   * @return loaded library header, or null if some error occurred.
   */
  @Nullable
  protected abstract LibraryHeader loadHeader(ErrorReporter errorReporter);

  /**
   * Invoked by a source after it loads the group of a module.
   *
   * @param modulePath  the path to the loaded module.
   * @param group       the group of the loaded module or null if the group was not loaded.
   * @param isRaw       true if the module was loaded from a raw source, false otherwise.
   */
  public void groupLoaded(ModulePath modulePath, @Nullable ChildGroup group, boolean isRaw, boolean inTests) {

  }

  /**
   * Invoked by a binary source after it is loaded.
   *
   * @param modulePath  the path to the loaded module.
   * @param isComplete  true if the module was loaded completely, false otherwise.
   */
  public void binaryLoaded(ModulePath modulePath, boolean isComplete) {

  }

  /**
   * Checks if this library has any raw sources.
   * Note that currently libraries without raw sources do not work properly with class synonyms.
   *
   * @return true if the library has raw sources, false otherwise.
   */
  public boolean hasRawSources() {
    return true;
  }

  /**
   * Gets a referable converter which is used during loading of binary sources without raw counterparts.
   *
   * @return a referable converter or null if the library does not have raw sources.
   */
  @Nullable
  public ReferableConverter getReferableConverter() {
    return IdReferableConverter.INSTANCE;
  }

  /**
   * Gets a dependency listener for definitions loaded from binary sources.
   *
   * @return a dependency listener.
   */
  @NotNull
  public DependencyListener getDependencyListener() {
    return DummyDependencyListener.INSTANCE;
  }

  /**
   * Indicates whether the library should be loaded if some errors occur.
   *
   * @return true if the library should be loaded despite errors, false otherwise.
   */
  protected boolean mustBeLoaded() {
    return false;
  }

  protected void loadGeneratedModules() {}

  @Override
  public boolean load(LibraryManager libraryManager, TypecheckingOrderingListener typechecking) {
    if (isLoaded()) {
      return true;
    }

    LibraryHeader header = loadHeader(libraryManager.getLibraryErrorReporter());
    if (header == null) {
      return false;
    }
    if (!header.languageVersionRange.inRange(Prelude.VERSION)) {
      libraryManager.showIncorrectLanguageVersionError(getName(), header.languageVersionRange);
      if (!mustBeLoaded()) {
        return false;
      }
    }

    MultiClassLoader<Library> classLoader = libraryManager.getClassLoader(isExternal());
    if (header.classLoaderDelegate != null && header.extMainClass != null) {
      classLoader.addDelegate(this, header.classLoaderDelegate);
    }

    Map<String, ArendExtension> dependenciesExtensions = new LinkedHashMap<>();
    for (LibraryDependency dependency : header.dependencies) {
      Library loadedDependency = libraryManager.loadDependency(this, dependency.name, typechecking);
      if (loadedDependency == null && !mustBeLoaded()) {
        classLoader.removeDelegate(this);
        return false;
      }

      if (loadedDependency != null) {
        libraryManager.registerDependency(this, loadedDependency);
        dependenciesExtensions.put(dependency.name, loadedDependency.getArendExtension());
      }
    }

    libraryManager.beforeLibraryLoading(this);

    try {
      Class<?> extMainClass = null;
      if (header.classLoaderDelegate != null && header.extMainClass != null) {
        extMainClass = classLoader.loadClass(header.extMainClass);
        if (!ArendExtension.class.isAssignableFrom(extMainClass)) {
          libraryManager.getLibraryErrorReporter().report(LibraryError.incorrectExtensionClass(getName()));
          extMainClass = null;
        }
      }

      if (extMainClass != null) {
        myExtension = (ArendExtension) extMainClass.getDeclaredConstructor().newInstance();
      }
    } catch (Exception e) {
      classLoader.removeDelegate(this);
      libraryManager.getLibraryErrorReporter().report(new ExceptionError(e, "loading of library " + getName()));
    }

    SerializableKeyRegistryImpl keyRegistry = new SerializableKeyRegistryImpl();
    if (myExtension == null) {
      myExtension = new DefaultArendExtension();
    }

    myExtension.registerKeys(keyRegistry);
    myExtension.setDependencies(dependenciesExtensions);
    myExtension.setPrelude(new Prelude());
    myExtension.setConcreteFactory(new ConcreteFactoryImpl(null));
    myExtension.setVariableRenamerFactory(VariableRenamerFactoryImpl.INSTANCE);
    ArendUI ui = getUI();
    if (ui != null) {
      myExtension.setUI(ui);
    }

    DefinitionContributorImpl contributor = new DefinitionContributorImpl(this, libraryManager.getLibraryErrorReporter(), myAdditionalModuleScopeProvider);
    try {
      myExtension.declareDefinitions(contributor);
    } finally {
      contributor.disable();
    }
    loadGeneratedModules();

    try {
      SourceLoader sourceLoader = new SourceLoader(this, libraryManager, true);
      if (hasRawSources()) {
        for (ModulePath module : header.modules) {
          sourceLoader.preloadRaw(module, false);
        }
        sourceLoader.loadRawSources();
      }

      if (!myFlags.contains(Flag.RECOMPILE) || isExternal()) {
        DefinitionListener definitionListener = ListDefinitionListener.join(libraryManager.getDefinitionListener(), myExtension.getDefinitionListener());

        for (ModulePath module : header.modules)
          sourceLoader.preloadBinary(module, keyRegistry, definitionListener);

        SourceLoader newSourceLoader = new SourceLoader(this, libraryManager, false);
        newSourceLoader.initalizeLoader(sourceLoader);
        sourceLoader = newSourceLoader;

        for (ModulePath module : header.modules) {
          if (!sourceLoader.loadBinary(module, keyRegistry, definitionListener) && isExternal()) {
            libraryManager.getLibraryErrorReporter().report(LibraryError.moduleLoading(module, getName()));
            if (!mustBeLoaded()) {
              libraryManager.afterLibraryLoading(this, false);
              return false;
            }
          }
        }
      }
    } catch (Throwable e) {
      libraryManager.afterLibraryLoading(this, false);
      throw e;
    }

    if (myExtension != null) {
      myExtension.setDefinitionProvider(DefinitionProviderImpl.INSTANCE);
      ArendDependencyProviderImpl provider = new ArendDependencyProviderImpl(typechecking, libraryManager.getAvailableModuleScopeProvider(this), libraryManager.getDefinitionRequester(), this);
      try {
        myExtension.load(provider);
      } finally {
        provider.disable();
      }
    }

    libraryManager.afterLibraryLoading(this, true);

    return super.load(libraryManager, typechecking);
  }

  public boolean loadTests(LibraryManager libraryManager, Collection<? extends ModulePath> modules) {
    if (modules.isEmpty()) {
      return true;
    }

    SourceLoader sourceLoader = new SourceLoader(this, libraryManager, false);
    for (ModulePath module : getLoadedModules()) {
      sourceLoader.setModuleLoaded(module);
    }
    for (ModulePath module : modules) {
      sourceLoader.preloadRaw(module, true);
    }
    sourceLoader.loadRawSources();

    return true;
  }

  @Override
  public boolean loadTests(LibraryManager libraryManager) {
    return loadTests(libraryManager, getTestModules());
  }

  @Override
  public boolean unload() {
    myAdditionalModuleScopeProvider.clear();
    return super.unload();
  }

  @Override
  public boolean containsModule(ModulePath modulePath) {
    Source source = getRawSource(modulePath);
    if (source != null && source.isAvailable()) {
      return true;
    }
    source = getBinarySource(modulePath);
    return source != null && source.isAvailable();
  }

  public boolean supportsPersisting() {
    return !isExternal();
  }

  public boolean persistModule(ModulePath modulePath, ReferableConverter referableConverter, ErrorReporter errorReporter) {
    PersistableBinarySource source = getPersistableBinarySource(modulePath);
    if (source == null) {
      errorReporter.report(new PersistingError(modulePath));
      return false;
    } else {
      return source.persist(this, referableConverter, errorReporter);
    }
  }

  public boolean persistUpdatedModules(ErrorReporter errorReporter) {
    boolean ok = true;
    for (ModulePath module : getUpdatedModules()) {
      if (getModuleGroup(module, false) != null && !persistModule(module, IdReferableConverter.INSTANCE, errorReporter)) {
        ok = false;
      }
    }
    return ok;
  }

  public boolean deleteModule(ModulePath modulePath) {
    PersistableBinarySource source = getPersistableBinarySource(modulePath);
    return source != null && source.delete(this);
  }
}

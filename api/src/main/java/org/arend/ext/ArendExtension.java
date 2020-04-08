package org.arend.ext;

import org.arend.ext.concrete.ConcreteFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * The main class of the extension should implement this interface.
 */
public interface ArendExtension {
  /**
   * Declares meta definitions defined in this extension.
   * This method is invoked first, so it does not have access to the library itself.
   * All definitions must be declared in this method, that is {@code contributor} cannot be stored and invoked later.
   */
  default void declareDefinitions(@NotNull DefinitionContributor contributor) {}

  /**
   * Can be used to get access to extensions of libraries on which this library depends.
   *
   * @param dependencies    a map from names of libraries to corresponding extensions
   */
  default void setDependencies(@NotNull Map<String, ArendExtension> dependencies) {}

  /**
   * Can be used to get access to the prelude.
   */
  default void setPrelude(@NotNull ArendPrelude prelude) {}

  /**
   * Can be used to get access to a {@link ConcreteFactory}.
   */
  default void setConcreteFactory(@NotNull ConcreteFactory factory) {}

  /**
   * Can be used to get access to definitions.
   */
  default void setDefinitionProvider(@NotNull DefinitionProvider definitionProvider) {}

  /**
   * This method is invoked last and can be used to initialize the extension.
   * It should store all the definition that will be used in the extension.
   *
   * @param definitionProvider  provides the access to definitions defined in the library; can be used only inside this method.
   */
  default void load(@NotNull ArendDefinitionProvider definitionProvider) {}
}

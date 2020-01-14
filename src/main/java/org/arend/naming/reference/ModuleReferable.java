package org.arend.naming.reference;

import org.arend.ext.module.ModulePath;
import org.arend.ext.reference.Precedence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class ModuleReferable implements LocatedReferable {
  public final ModulePath path;

  public ModuleReferable(ModulePath path) {
    this.path = path;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    return path.toString();
  }

  @Nonnull
  @Override
  public Precedence getPrecedence() {
    return Precedence.DEFAULT;
  }

  @Nullable
  @Override
  public ModulePath getLocation() {
    return path;
  }

  @Nonnull
  @Override
  public Kind getKind() {
    return Kind.OTHER;
  }

  @Nullable
  @Override
  public LocatedReferable getLocatedReferableParent() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ModuleReferable that = (ModuleReferable) o;
    return Objects.equals(path, that.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path);
  }

  @Override
  public String toString() {
    return path.toString();
  }
}

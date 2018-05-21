package com.jetbrains.jetpad.vclang.naming.reference;

import com.jetbrains.jetpad.vclang.naming.scope.ClassFieldImplScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LongUnresolvedReference implements UnresolvedReference {
  private final Object myData;
  private final List<String> myPath;
  private Referable resolved;

  public LongUnresolvedReference(Object data, @Nonnull List<String> path) {
    assert !path.isEmpty();
    myData = data;
    myPath = path;
  }

  public LongUnresolvedReference(Object data, @Nonnull List<String> path, @Nonnull String name) {
    myData = data;
    if (path.isEmpty()) {
      myPath = Collections.singletonList(name);
    } else {
      myPath = new ArrayList<>(path.size() + 1);
      myPath.addAll(path);
      myPath.add(name);
    }
  }

  public LongUnresolvedReference(Object data, @Nonnull String name, @Nonnull List<String> path) {
    myData = data;
    if (path.isEmpty()) {
      myPath = Collections.singletonList(name);
    } else {
      myPath = new ArrayList<>(path.size() + 1);
      myPath.add(name);
      myPath.addAll(path);
    }
  }

  public List<String> getPath() {
    return myPath;
  }

  @Nullable
  @Override
  public Object getData() {
    return myData;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (String name : myPath) {
      if (first) {
        first = false;
      } else {
        builder.append(".");
      }
      builder.append(name);
    }
    return builder.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    LongUnresolvedReference that = (LongUnresolvedReference) o;

    return myPath.equals(that.myPath);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myPath.hashCode();
    return result;
  }

  @Nonnull
  @Override
  public Referable resolve(Scope scope) {
    if (resolved != null) {
      return resolved;
    }

    for (int i = 0; i < myPath.size() - 1; i++) {
      scope = scope.resolveNamespace(myPath.get(i));
      if (scope == null) {
        Object data = getData();
        resolved = new ErrorReference(data, i == 0 ? null : new LongUnresolvedReference(data, myPath.subList(0, i)), myPath.get(i));
        return resolved;
      }
    }

    String name = myPath.get(myPath.size() - 1);
    resolved = scope.resolveName(name);
    if (resolved == null) {
      Object data = getData();
      resolved = new ErrorReference(data, myPath.size() == 1 ? null : new LongUnresolvedReference(data, myPath.subList(0, myPath.size() - 1)), name);
    }

    return resolved;
  }

  @Nullable
  @Override
  public Concrete.Expression resolveArgument(Scope scope) {
    if (resolved != null) {
      return null;
    }

    Scope prevScope = scope;
    for (int i = 0; i < myPath.size() - 1; i++) {
      Scope nextScope = scope.resolveNamespace(myPath.get(i));
      if (nextScope == null) {
        return resolveField(prevScope, i == 0 ? 0 : i - 1);
      }
      prevScope = scope;
      scope = nextScope;
    }

    String name = myPath.get(myPath.size() - 1);
    resolved = scope.resolveName(name);
    if (resolved == null) {
      if (myPath.size() == 1) {
        resolved = new ErrorReference(getData(), null, name);
      } else {
        return resolveField(prevScope, myPath.size() - 2);
      }
    }

    return null;
  }

  private Concrete.Expression resolveField(Scope scope, int i) {
    resolved = scope.resolveName(myPath.get(i));
    ClassReferable classRef = resolved instanceof GlobalReferable ? ((GlobalReferable) resolved).getTypeClassReference() : null;
    if (classRef == null) {
      Object data = getData();
      resolved = new ErrorReference(data, i == 0 ? null : new LongUnresolvedReference(data, myPath.subList(0, i)), myPath.get(i));
      return null;
    }

    Object data = getData();
    Concrete.Expression result = new Concrete.ReferenceExpression(data, getReferable());
    for (i++; i < myPath.size(); i++) {
      resolved = new ClassFieldImplScope(classRef).resolveName(myPath.get(i));
      if (resolved == null) {
        resolved = new ErrorReference(data, classRef, myPath.get(i));
        return null;
      }
      if (i == myPath.size() - 1) {
        return result;
      }
      result = new Concrete.AppExpression(data, new Concrete.ReferenceExpression(data, getReferable()), new Concrete.Argument(result, false));

      classRef = resolved instanceof GlobalReferable ? ((GlobalReferable) resolved).getTypeClassReference() : null;
      if (classRef == null) {
        resolved = new ErrorReference(data, new LongUnresolvedReference(data, myPath.subList(0, i)), myPath.get(i));
        return null;
      }
    }

    return result;
  }

  private Referable getReferable() {
    Referable ref = resolved;
    while (ref instanceof RedirectingReferable) {
      ref = ((RedirectingReferable) ref).getOriginalReferable();
    }
    return ref;
  }

  public Scope resolveNamespace(Scope scope) {
    if (resolved instanceof ErrorReference) {
      return null;
    }

    for (int i = 0; i < myPath.size(); i++) {
      scope = scope.resolveNamespace(myPath.get(i));
      if (scope == null) {
        Object data = getData();
        resolved = new ErrorReference(data, i == 0 ? null : new LongUnresolvedReference(data, myPath.subList(0, i)), myPath.get(i));
        return null;
      }
    }

    return scope;
  }

  public ErrorReference getErrorReference() {
    return resolved instanceof ErrorReference ? (ErrorReference) resolved : null;
  }
}

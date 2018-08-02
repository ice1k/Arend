package com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider;

import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Reference;

import javax.annotation.Nullable;

public interface PartialConcreteProvider {
  @Nullable Reference getInstanceTypeReference(GlobalReferable instance);
  boolean isRecord(ClassReferable classRef);
  boolean isInstance(GlobalReferable ref);
  boolean isCoerce(GlobalReferable ref);
  boolean isData(GlobalReferable ref);
}

package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.error.doc.LineDoc;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;

import java.util.Collection;
import java.util.stream.Collectors;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.*;

public class FieldsImplementationError extends TypecheckingError {
  public boolean alreadyImplemented;
  public Collection<? extends GlobalReferable> fields;

  public FieldsImplementationError(boolean alreadyImplemented, Collection<? extends GlobalReferable> fields, Concrete.SourceNode cause) {
    super("The following fields are " + (alreadyImplemented ? "already" : "not") + " implemented: ", cause);
    this.alreadyImplemented = alreadyImplemented;
    this.fields = fields;
  }

  @Override
  public LineDoc getHeaderDoc(PrettyPrinterConfig src) {
    return hList(super.getHeaderDoc(src), hSep(text(", "), fields.stream().map(f -> f == null ? text("parent") : refDoc(f) /* TODO[classes] */).collect(Collectors.toList())));
  }
}
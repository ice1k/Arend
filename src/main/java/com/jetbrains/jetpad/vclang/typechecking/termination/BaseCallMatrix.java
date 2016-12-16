package com.jetbrains.jetpad.vclang.typechecking.termination;

/*Generated by MPS */

import java.util.HashMap;

public abstract class BaseCallMatrix<T> {
  public enum R {
    Unknown(),
    Equal(),
    LessThan()
  }

  private static BaseCallMatrix.R rmul(BaseCallMatrix.R a, BaseCallMatrix.R b) {
    switch (a) {
      case Equal:
        switch (b) {
          case Equal:
            return BaseCallMatrix.R.Equal;
          case LessThan:
            return BaseCallMatrix.R.LessThan;
          default:
            return BaseCallMatrix.R.Unknown;
        }
      case LessThan:
        switch (b) {
          case Equal:
          case LessThan:
            return BaseCallMatrix.R.LessThan;
          default:
            return BaseCallMatrix.R.Unknown;
        }
      default:
        return BaseCallMatrix.R.Unknown;
    }
  }

  static boolean rleq(BaseCallMatrix.R a, BaseCallMatrix.R b) {
    switch (a) {
      case LessThan:
        return (b == BaseCallMatrix.R.LessThan);
      case Equal:
        return (b == BaseCallMatrix.R.LessThan || b == BaseCallMatrix.R.Equal);
      default:
        return true;
    }
  }

  private HashMap<Integer, BaseCallMatrix.CallMatrixEntry> myMatrixMap = new HashMap<>();
  private int myWidth;
  private int myHeight;

  BaseCallMatrix(int width, int height) {
    myWidth = width;
    myHeight = height;
  }

  BaseCallMatrix(BaseCallMatrix<T> m) {
    // copy constructor 
    myWidth = m.myWidth;
    myHeight = m.myHeight;
    for (Integer j : m.myMatrixMap.keySet()) {
      BaseCallMatrix.CallMatrixEntry cme = m.myMatrixMap.get(j);
      myMatrixMap.put(j, new BaseCallMatrix.CallMatrixEntry(cme.myIndex, cme.myRel));
    }
  }

  BaseCallMatrix(BaseCallMatrix<T> m1, BaseCallMatrix<T> m2) {
    // multiplication constructor 
    if (m1.myWidth != m2.myHeight) {
      throw new IllegalArgumentException();
    }
    myHeight = m1.myHeight;
    myWidth = m2.myWidth;

    for (int j = 0; j < myWidth; j++) {
      BaseCallMatrix.CallMatrixEntry cme2 = m2.myMatrixMap.get(j);
      if (cme2 != null) {
        BaseCallMatrix.CallMatrixEntry cme = m1.myMatrixMap.get(cme2.myIndex);
        if (cme != null) {
          myMatrixMap.put(j, new BaseCallMatrix.CallMatrixEntry(cme.myIndex, rmul(cme.myRel, cme2.myRel)));
        }
      }
    }

  }

  int getHeight() {
    return myHeight;
  }

  int getWidth() {
    return myWidth;
  }

  public abstract T getCodomain();

  public abstract T getDomain();

  public abstract int getCompositeLength();

  public void set(int i, int j, BaseCallMatrix.R v) {
    if (v != BaseCallMatrix.R.Unknown) {
      BaseCallMatrix.CallMatrixEntry cme = new BaseCallMatrix.CallMatrixEntry(i, v);
      if (myMatrixMap.get(j) != null && !((cme.equals(myMatrixMap.get(j))))) {
        throw new IllegalStateException("Call matrix is assumed to be monomial");
      }
      myMatrixMap.put(j, cme);
    }
  }

  public BaseCallMatrix.R getValue(int i, int j) {
    BaseCallMatrix.CallMatrixEntry cme = myMatrixMap.get(j);
    BaseCallMatrix.R result2 = BaseCallMatrix.R.Unknown;
    if (cme != null && cme.myIndex == i) {
      result2 = cme.myRel;
    }
    return result2;
  }

  public final boolean leq(BaseCallMatrix<T> cm) {
    if (getCodomain() != cm.getCodomain() || getDomain() != cm.getDomain()) {
      return false;
    }
    for (int k : myMatrixMap.keySet()) {
      BaseCallMatrix.CallMatrixEntry cme = cm.myMatrixMap.get(k);
      BaseCallMatrix.CallMatrixEntry mme = myMatrixMap.get(k);
      if (cme == null || cme.myIndex != mme.myIndex || !(BaseCallMatrix.rleq(mme.myRel, cme.myRel))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public final boolean equals(Object object) {
    if (object instanceof BaseCallMatrix) {
      BaseCallMatrix cm = (BaseCallMatrix) object;
      return !(getCodomain() != cm.getCodomain() || getDomain() != cm.getDomain()) && myMatrixMap.equals(cm.myMatrixMap);
    } else {
      return false;
    }
  }

  @Override
  public final int hashCode() {
    int result = getCodomain().hashCode() * 31 + getDomain().hashCode();
    return result * 31 + myMatrixMap.hashCode();
  }

  protected String[] getColumnLabels() {
    String[] result = new String[myWidth];
    for (int i = 0; i < myWidth; i++) {
      result[i] = "?";
    }
    return result;
  }

  protected String[] getRowLabels() {
    String[] result = new String[myHeight];
    for (int i = 0; i < myHeight; i++) {
      result[i] = "?";
    }
    return result;
  }

  public String getMatrixLabel() {
    return "";
  }

  static char rToChar(R r) {
    switch (r) {
      case Equal:
        return '=';
      case LessThan:
        return '<';
      default:
        return '?';
    }
  }

  @Override
  public String toString() {
    String result = getMatrixLabel() + "\n";
    String[] columnLabels = getColumnLabels();
    String[] rowLabels = getRowLabels();
    int max = 0;
    for (String label : rowLabels) {
      if (max < label.length()) {
        max = label.length();
      }
    }
    max++;

    result += String.format("%" + (max + 1) + "s", "");
    for (int j = 0; j < myWidth; j++) {
      result += columnLabels[j] + " ";
    }
    result += "\n";
    for (int i = 0; i < myHeight; i++) {
      result += String.format("%" + max + "s", rowLabels[i]);
      for (int j = 0; j < myWidth; j++)
        result += String.format("%" + (columnLabels[j].length() + 1) + "s", String.valueOf(rToChar(getValue(i, j))));
      result += "\n";
    }

    return result;
  }

  private static class CallMatrixEntry {
    int myIndex;
    BaseCallMatrix.R myRel;

    CallMatrixEntry(int index, BaseCallMatrix.R rel) {
      myIndex = index;
      myRel = rel;
    }

    @Override
    public int hashCode() {
      return myIndex * 31 + myRel.hashCode();
    }

    @Override
    public boolean equals(Object object) {
      if (object instanceof BaseCallMatrix.CallMatrixEntry) {
        BaseCallMatrix.CallMatrixEntry cm = (BaseCallMatrix.CallMatrixEntry) object;
        return cm.myIndex == myIndex && cm.myRel == myRel;
      }
      return false;
    }
  }
}

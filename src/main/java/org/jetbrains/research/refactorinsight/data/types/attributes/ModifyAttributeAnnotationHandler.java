package org.jetbrains.research.refactorinsight.data.types.attributes;

import gr.uom.java.xmi.diff.ModifyAttributeAnnotationRefactoring;
import org.jetbrains.research.refactorinsight.adapters.CodeRange;
import org.jetbrains.research.refactorinsight.adapters.LocationInfo;
import org.jetbrains.research.refactorinsight.data.Group;
import org.jetbrains.research.refactorinsight.data.RefactoringInfo;
import org.jetbrains.research.refactorinsight.data.RefactoringLine;
import org.jetbrains.research.refactorinsight.data.types.Handler;
import org.refactoringminer.api.Refactoring;

public class ModifyAttributeAnnotationHandler extends Handler {
  @Override
  public RefactoringInfo specify(Refactoring refactoring, RefactoringInfo info) {
    ModifyAttributeAnnotationRefactoring ref = (ModifyAttributeAnnotationRefactoring) refactoring;

    String classNameBefore = ref.getAttributeBefore().getClassName();
    String classNameAfter = ref.getAttributeAfter().getClassName();

    return info.setGroup(Group.ATTRIBUTE)
        .setDetailsBefore(classNameBefore)
        .setDetailsAfter(classNameAfter)
        .setNameBefore(ref.getAttributeBefore().getVariableDeclaration().toQualifiedString())
        .setNameAfter(ref.getAttributeAfter().getVariableDeclaration().toQualifiedString())
        .setElementBefore(ref.getAnnotationBefore().toString())
        .setElementAfter(ref.getAnnotationAfter().toString())
        .addMarking(new CodeRange(ref.getAnnotationBefore().codeRange()),
            new CodeRange(ref.getAnnotationAfter().codeRange()),
            line -> line.addOffset(new LocationInfo(ref.getAnnotationBefore().getLocationInfo()),
                new LocationInfo(ref.getAnnotationAfter().getLocationInfo())),
            RefactoringLine.MarkingOption.NONE, true);
  }


  @Override
  public RefactoringInfo specify(org.jetbrains.research.kotlinrminer.api.Refactoring refactoring,
                                 RefactoringInfo info) {
    //This kind of refactoring is not supported by kotlinRMiner yet.
    return null;
  }
}

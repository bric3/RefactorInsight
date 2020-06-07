package data.types.attributes;

import com.intellij.openapi.project.Project;
import data.Group;
import data.RefactoringInfo;
import data.types.Handler;
import gr.uom.java.xmi.diff.ExtractAttributeRefactoring;
import org.refactoringminer.api.Refactoring;
import utils.Utils;

public class ExtractAttributeHandler extends Handler {

  @Override
  public RefactoringInfo specify(Refactoring refactoring, RefactoringInfo info, Project project) {
    ExtractAttributeRefactoring ref = (ExtractAttributeRefactoring) refactoring;

    String classNameBefore = ref.getOriginalClass().getName();

    info.setGroup(Group.ATTRIBUTE)
        .setNameBefore(classNameBefore)
        .setNameAfter(classNameBefore)
        .setElementBefore(ref.getVariableDeclaration().getVariableDeclaration().toQualifiedString())
        .setElementAfter(null)
        .addMarking(ref.getExtractedVariableDeclarationCodeRange().getStartLine(),
            ref.getExtractedVariableDeclarationCodeRange().getStartLine() - 1,
            ref.getExtractedVariableDeclarationCodeRange().getStartLine(),
            ref.getExtractedVariableDeclarationCodeRange().getEndLine(),
            ref.getOriginalClass().getLocationInfo().getFilePath(),
            ref.getNextClass().getLocationInfo().getFilePath());
    ref.leftSide().forEach(extraction ->
        info.addMarking(extraction.getStartLine(), extraction.getEndLine(),
            ref.getExtractedVariableDeclarationCodeRange().getStartLine(),
            ref.getExtractedVariableDeclarationCodeRange().getEndLine(),
            extraction.getFilePath(),
            ref.getExtractedVariableDeclarationCodeRange().getFilePath()));
    return info;
  }
}

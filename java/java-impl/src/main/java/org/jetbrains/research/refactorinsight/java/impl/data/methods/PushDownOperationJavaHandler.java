package org.jetbrains.research.refactorinsight.java.impl.data.methods;

import gr.uom.java.xmi.decomposition.AbstractStatement;
import gr.uom.java.xmi.diff.PushDownOperationRefactoring;
import org.jetbrains.research.refactorinsight.common.data.Group;
import org.jetbrains.research.refactorinsight.common.data.RefactoringInfo;
import org.jetbrains.research.refactorinsight.common.data.RefactoringLine;
import org.jetbrains.research.refactorinsight.java.impl.data.JavaRefactoringHandler;
import org.refactoringminer.api.Refactoring;

import java.util.List;

import static org.jetbrains.research.refactorinsight.java.impl.data.util.JavaUtils.*;

public class PushDownOperationJavaHandler extends JavaRefactoringHandler {

    @Override
    public RefactoringInfo specify(Refactoring refactoring, RefactoringInfo info) {
        PushDownOperationRefactoring ref = (PushDownOperationRefactoring) refactoring;

        List<AbstractStatement> statementsBefore =
                ref.getOriginalOperation().getBody().getCompositeStatement().getStatements();
        List<AbstractStatement> statementsAfter =
                ref.getMovedOperation().getBody().getCompositeStatement().getStatements();
        info.setChanged(!isStatementsEqualJava(statementsBefore, statementsAfter));

        String classBefore = ref.getOriginalOperation().getClassName();
        String classAfter = ref.getMovedOperation().getClassName();

        return info.setGroup(Group.METHOD)
                .setDetailsBefore(classBefore)
                .setDetailsAfter(classAfter)
                .addMarking(
                        createCodeRangeFromJava(ref.getOriginalOperation().codeRange()),
                        createCodeRangeFromJava(ref.getMovedOperation().codeRange()),
                        refactoringLine -> refactoringLine.setWord(new String[]{
                                ref.getOriginalOperation().getName(),
                                null,
                                ref.getMovedOperation().getName()
                        }),
                        RefactoringLine.MarkingOption.COLLAPSE,
                        true)
                .setNameBefore(calculateSignatureForJavaMethod(ref.getOriginalOperation()))
                .setNameAfter(calculateSignatureForJavaMethod(ref.getMovedOperation()));
    }

}

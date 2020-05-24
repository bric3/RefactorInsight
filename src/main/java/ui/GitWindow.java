package ui;

import com.intellij.diff.DiffContentFactoryEx;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.fragments.LineFragment;
import com.intellij.diff.fragments.LineFragmentImpl;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.util.DiffUserDataKeysEx;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ui.ChangesTree;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBViewport;
import com.intellij.vcs.log.ui.MainVcsLogUi;
import com.intellij.vcs.log.ui.VcsLogInternalDataKeys;
import com.intellij.vcs.log.ui.frame.VcsLogChangesBrowser;
import com.intellij.vcs.log.ui.table.VcsLogGraphTable;
import data.RefactoringEntry;
import data.RefactoringInfo;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jetbrains.annotations.NotNull;
import services.MiningService;
import services.RefactoringsBundle;


public class GitWindow extends ToggleAction {

  Project project;
  AnActionEvent event;
  DiffContentFactoryEx myDiffContentFactory;
  private ChangesTree changesTree;
  private JBViewport viewport;
  private boolean selected = false;
  private VcsLogGraphTable table;
  private JBScrollPane scrollPane;
  private MiningService miningService;

  private void setUp(@NotNull AnActionEvent e) {
    VcsLogChangesBrowser changesBrowser =
        (VcsLogChangesBrowser) e.getData(VcsLogChangesBrowser.DATA_KEY);
    changesTree = changesBrowser.getViewer();
    MainVcsLogUi logUI = e.getData(VcsLogInternalDataKeys.MAIN_UI);

    project = e.getProject();
    miningService = project.getService(MiningService.class);

    table = logUI.getTable();
    table.getSelectionModel().addListSelectionListener(new CommitSelectionListener());

    event = e;
    myDiffContentFactory = DiffContentFactoryEx.getInstanceEx();
    viewport = (JBViewport) changesTree.getParent();
    scrollPane = new JBScrollPane(new JBLabel(RefactoringsBundle.message("not.selected")));
  }

  private void toRefactoringView(@NotNull AnActionEvent e) {
    while (miningService.isMining()) {

    }
    int index = table.getSelectionModel().getAnchorSelectionIndex();
    if (index != -1) {
      buildComponent(index);
    }
    viewport.setView(scrollPane);
  }

  private void buildComponent(int index) {
    String commitId = table.getModel().getCommitId(index).getHash().asString();
    String refactorings = miningService.getRefactorings(commitId);
    if (!refactorings.equals("")) {
      scrollPane.getViewport().setView(buildList(index, refactorings));
    } else {
      JBSplitter splitter = new JBSplitter(true, (float) 0.1);
      JBLabel label = new JBLabel(RefactoringsBundle.message("not.mined"));
      JBPanel panel = new JBPanel();
      splitter.setFirstComponent(label);

      JButton button = new JButton("Mine this commit");
      GitWindow gitWindow = this;
      button.addMouseListener(
          new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              miningService
                  .mineAtCommit(table.getModel().getCommitMetadata(index), project, gitWindow);
            }
          });

      panel.add(button);
      splitter.setSecondComponent(panel);
      scrollPane.getViewport().setView(splitter);
    }
  }

  /**
   * Method called after a single commit is mined.
   * Updates the view with the refactorings found.
   * @param commitId to refresh the view at.
   */
  public void refresh(String commitId) {
    int index = table.getSelectionModel().getAnchorSelectionIndex();
    if (table.getModel().getCommitId(index).getHash().asString().equals(commitId)) {
      buildComponent(index);
    }
  }

  private void toChangesView(@NotNull AnActionEvent e) {
    viewport.setView(changesTree);
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    return selected;
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    if (changesTree == null) {
      setUp(e);
    }
    if (state) {
      toRefactoringView(e);
    } else {
      toChangesView(e);
    }
    selected = state;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setVisible(true);
    super.update(e);
  }

  private JBList buildList(int index, String refactorings) {
    List<RefactoringInfo> refs = RefactoringEntry.fromString(refactorings).getRefactorings();
    String[] names = refs.stream().map(r -> r.getName()).toArray(String[]::new);
    JBList<String> list = new JBList<>(names);

    MouseAdapter mouseListener = new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          showDiff(index, refs.get(list.locationToIndex(e.getPoint())));
        }
      }
    };
    list.addMouseListener(mouseListener);
    return list;
  }

  private void showDiff(int index, RefactoringInfo info) {
    try {
      Collection<Change> changes = table.getModel().getFullDetails(index).getChanges();

      String contentBefore = "";
      String contentAfter = "";
      for (Change change : changes) {
        if (change.getBeforeRevision() != null
            && (project.getBasePath() + "/" + info.getBeforePath())
            .equals(change.getBeforeRevision().getFile().getPath())) {
          contentBefore = change.getBeforeRevision().getContent();
        }
        if (change.getAfterRevision() != null
            && (project.getBasePath() + "/" + info.getAfterPath())
            .equals(change.getAfterRevision().getFile().getPath())) {
          contentAfter = change.getAfterRevision().getContent();
        }
      }

      DiffContent diffContentBefore = myDiffContentFactory.create(project, contentBefore,
          JavaClassFileType.INSTANCE);
      DiffContent diffContentAfter = myDiffContentFactory.create(project, contentAfter,
          JavaClassFileType.INSTANCE);

      SimpleDiffRequest request = new SimpleDiffRequest(info.getName(),
          diffContentBefore, diffContentAfter, info.getBeforePath(), info.getAfterPath());

      List<LineFragment> fragments = filterWrongCodeRanges(info, contentBefore, contentAfter);
      request.putUserData(DiffUserDataKeysEx.CUSTOM_DIFF_COMPUTER,
          (text1, text2, policy, innerChanges, indicator) -> fragments);

      DiffManager.getInstance().showDiff(project, request);
    } catch (VcsException e) {
      e.printStackTrace();
    }
  }
  //TODO: deal with -1 code range somewhere else
  private List<LineFragment> filterWrongCodeRanges(RefactoringInfo info, String contentBefore, String contentAfter){
    int beforeLinecount = (int) contentBefore.chars().filter(c -> c == '\n').count() + 1;
    int afterLinecount = (int) contentAfter.chars().filter(c -> c == '\n').count() + 1;
    return info.getLineMarkings().stream().map(o ->
        new LineFragmentImpl(o.getStartLine1(),
            o.getEndLine1() > 0 ? o.getEndLine1() : beforeLinecount,
            o.getStartLine2(),
            o.getEndLine2() > 0 ? o.getEndLine2() : afterLinecount,
            0, 0, 0, 0)
    ).collect(Collectors.toList());
  }

  class CommitSelectionListener implements ListSelectionListener {
    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
      if (listSelectionEvent.getValueIsAdjusting()) {
        return;
      }
      DefaultListSelectionModel selectionModel =
          (DefaultListSelectionModel) listSelectionEvent.getSource();

      int beginIndex = selectionModel.getMinSelectionIndex();
      int endIndex = selectionModel.getMaxSelectionIndex();

      if (beginIndex != -1 || endIndex != -1) {
        if (!miningService.isMining()) {
          buildComponent(beginIndex);
        }
      }
    }
  }
}
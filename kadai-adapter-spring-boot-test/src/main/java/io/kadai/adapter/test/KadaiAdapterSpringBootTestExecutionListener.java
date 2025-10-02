package io.kadai.adapter.test;

import io.kadai.classification.api.ClassificationQueryColumnName;
import io.kadai.classification.api.ClassificationService;
import io.kadai.classification.internal.ClassificationMapper;
import io.kadai.common.api.BaseQuery.SortDirection;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.internal.InternalKadaiEngine;
import io.kadai.task.api.TaskQueryColumnName;
import io.kadai.task.api.TaskService;
import io.kadai.task.internal.AttachmentMapper;
import io.kadai.task.internal.ObjectReferenceMapper;
import io.kadai.task.internal.TaskMapper;
import io.kadai.testapi.KadaiEngineProxy;
import io.kadai.workbasket.api.WorkbasketQueryColumnName;
import io.kadai.workbasket.api.WorkbasketService;
import io.kadai.workbasket.internal.DistributionTargetMapper;
import io.kadai.workbasket.internal.WorkbasketAccessMapper;
import io.kadai.workbasket.internal.WorkbasketMapper;
import java.util.List;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

/**
 * {@link TestExecutionListener} clearing the Kadai-Database before every Test-Run.
 *
 * <p>This listener is <b>not</b> part of {@link
 * KadaiAdapterSpringBootTest @KadaiAdapterSpringBootTest} because it would restrict registration of
 * other listeners.
 *
 * <p>You can however build your own custom meta-annotations on top of it and there register this
 * listener.
 */
public class KadaiAdapterSpringBootTestExecutionListener implements TestExecutionListener, Ordered {

  private static InternalKadaiEngine internalKadaiEngine;
  private static TaskService taskService;
  private static WorkbasketService workbasketService;
  private static ClassificationService classificationService;
  private static AttachmentMapper attachmentMapper;
  private static TaskMapper taskMapper;
  private static WorkbasketMapper workbasketMapper;
  private static WorkbasketAccessMapper workbasketAccessMapper;
  private static DistributionTargetMapper distributionTargetMapper;
  private static ClassificationMapper classificationMapper;
  private static ObjectReferenceMapper objectReferenceMapper;

  @Override
  public void beforeTestClass(@NonNull TestContext testContext) throws Exception {
    ApplicationContext context = testContext.getApplicationContext();
    internalKadaiEngine = new KadaiEngineProxy(context.getBean(KadaiEngine.class)).getEngine();
    taskService = internalKadaiEngine.getEngine().getTaskService();
    workbasketService = internalKadaiEngine.getEngine().getWorkbasketService();
    classificationService = internalKadaiEngine.getEngine().getClassificationService();
    attachmentMapper = internalKadaiEngine.getSqlSession().getMapper(AttachmentMapper.class);
    taskMapper = internalKadaiEngine.getSqlSession().getMapper(TaskMapper.class);
    workbasketMapper = internalKadaiEngine.getSqlSession().getMapper(WorkbasketMapper.class);
    workbasketAccessMapper =
        internalKadaiEngine.getSqlSession().getMapper(WorkbasketAccessMapper.class);
    distributionTargetMapper =
        internalKadaiEngine.getSqlSession().getMapper(DistributionTargetMapper.class);
    classificationMapper =
        internalKadaiEngine.getSqlSession().getMapper(ClassificationMapper.class);
    objectReferenceMapper =
        internalKadaiEngine.getSqlSession().getMapper(ObjectReferenceMapper.class);
  }

  @Override
  public void beforeTestMethod(@NonNull TestContext testContext) {
    internalKadaiEngine.getEngine().runAsAdmin(this::clearKadaiDb);
  }

  @Override
  public int getOrder() {
    return Integer.MAX_VALUE;
  }

  private void clearKadaiDb() {
    final List<String> taskIds =
        taskService.createTaskQuery().listValues(TaskQueryColumnName.ID, SortDirection.ASCENDING);
    attachmentMapper.deleteMultipleByTaskIds(taskIds);
    taskIds.forEach(taskMapper::delete);

    final List<String> workbasketIds =
        workbasketService
            .createWorkbasketQuery()
            .listValues(WorkbasketQueryColumnName.ID, SortDirection.ASCENDING);
    workbasketIds.forEach(workbasketAccessMapper::deleteAllAccessItemsForWorkbasketId);
    workbasketIds.forEach(workbasketMapper::delete);
    workbasketIds.forEach(distributionTargetMapper::deleteAllDistributionTargetsBySourceId);
    workbasketIds.forEach(distributionTargetMapper::deleteAllDistributionTargetsByTargetId);

    classificationService
        .createClassificationQuery()
        .listValues(ClassificationQueryColumnName.ID, SortDirection.ASCENDING)
        .forEach(classificationMapper::deleteClassification);

    objectReferenceMapper.deleteMultipleByTaskIds(taskIds);
  }
}

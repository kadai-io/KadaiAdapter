package io.kadai.adapter.test;

import io.kadai.classification.api.ClassificationService;
import io.kadai.classification.api.models.Classification;
import io.kadai.common.api.KadaiEngine;
import io.kadai.workbasket.api.WorkbasketPermission;
import io.kadai.workbasket.api.WorkbasketService;
import io.kadai.workbasket.api.WorkbasketType;
import io.kadai.workbasket.api.models.Workbasket;
import io.kadai.workbasket.api.models.WorkbasketAccessItem;

/**
 * Instantiated Util-Class providing helpers for writing integration-tests with Kadai and
 * KadaiAdapter.
 */
public class KadaiAdapterTestUtil {

  private final KadaiEngine kadaiEngine;

  public KadaiAdapterTestUtil(KadaiEngine kadaiEngine) {
    this.kadaiEngine = kadaiEngine;
  }

  public void createWorkbasket(String workbasketKey, String domain) throws Exception {
    WorkbasketService workbasketService = this.kadaiEngine.getWorkbasketService();
    Workbasket wb = workbasketService.newWorkbasket(workbasketKey, domain);
    wb.setName(workbasketKey);
    wb.setOwner("teamlead_1");
    wb.setType(WorkbasketType.PERSONAL);
    wb = workbasketService.createWorkbasket(wb);
    createWorkbasketAccessList(wb);
  }

  public void createWorkbasketAccessList(Workbasket wb) throws Exception {
    WorkbasketService workbasketService = this.kadaiEngine.getWorkbasketService();
    WorkbasketAccessItem workbasketAccessItem =
        workbasketService.newWorkbasketAccessItem(wb.getId(), wb.getOwner());
    workbasketAccessItem.setAccessName(wb.getOwner());
    workbasketAccessItem.setPermission(WorkbasketPermission.APPEND, true);
    workbasketAccessItem.setPermission(WorkbasketPermission.TRANSFER, true);
    workbasketAccessItem.setPermission(WorkbasketPermission.READ, true);
    workbasketAccessItem.setPermission(WorkbasketPermission.OPEN, true);
    workbasketAccessItem.setPermission(WorkbasketPermission.DISTRIBUTE, true);
    workbasketService.createWorkbasketAccessItem(workbasketAccessItem);
  }

  public void createClassification(String classificationKey, String domain) throws Exception {
    ClassificationService myClassificationService = this.kadaiEngine.getClassificationService();
    Classification classification =
        myClassificationService.newClassification(classificationKey, domain, "TASK");
    classification.setServiceLevel("P1D");
    myClassificationService.createClassification(classification);
  }
}
